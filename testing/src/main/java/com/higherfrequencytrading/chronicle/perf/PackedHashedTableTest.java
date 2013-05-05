/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.higherfrequencytrading.chronicle.perf;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IntIndexedChronicle;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.higherfrequencytrading.chronicle.impl.GlobalSettings.deleteOnExit;
import static org.junit.Assert.assertTrue;

/**
 * This test is based on http://mysqlha.blogspot.co.uk/2010/09/mysql-versus-mongodb-yet-another-silly.html
 * <p/>
 * In the original test the peak QPS was around 105 K/s at 128 threads. This test achieves around 600 K/s with one thread.
 * <p/>
 *
 * @author peter.lawrey
 */
public class PackedHashedTableTest {
    final String baseDir = new File("/dev/shm/").exists() ? "/dev/shm/" : System.getProperty("java.io.tmpdir");

    @Test
    public void testLookup() throws Exception {
        PackedHashedTable pht = new PackedHashedTable(baseDir + "/deleteme", 2, 4, 64);
        pht.deleteOnExit();

        Excerpt record = pht.startRecord();
        record.writeLong(1234567890L);
        pht.endRecord(1111);

        final boolean[] found = {false};
        pht.lookup(1111, new PackedHashedTable.HashRecordIterator() {
            @Override
            public boolean onExcerpt(Excerpt recordExcerpt) {
                found[0] |= recordExcerpt.readLong() == 1234567890L;
                return false;
            }
        });
        pht.close();
        assertTrue(found[0]);
    }

    /*
    * hash based random access.
    * Average time to add 1.4 us, to lookup 1.6 us
     */
    @Test
    public void testPerf() throws IOException {
/* def setup_db(host, port, dbname, rows):
      conn = pymongo.Connection(host, port)
      conn.drop_database(dbname)
      db = conn[dbname]
      for x in xrange(0, rows):
        sx = str(x)
        lsx = len(sx)
        db.c.save({'_id':x, 'k':x, 'c':sx+'x'*(120 - lsx), 'pad':sx+'y'*(120 - lsx)})
        if x % 1000 == 0:
          print '... row %d' % x
 */

        PackedHashedTable pht = new PackedHashedTable(baseDir + "/deleteme.h", 14, 24, 256);
        pht.deleteOnExit();

        final int warmup = 20000;
        final int rows = 600000;

        byte[] xs = new byte[120];
        Arrays.fill(xs, (byte) 'x');
        byte[] ys = new byte[120];
        Arrays.fill(ys, (byte) 'y');

        long start = 0;
        for (int i = -warmup; i < rows; i++) {
            if (i == 0)
                start = System.nanoTime();
            Excerpt record = pht.startRecord();
            record.writeInt(i);
            int pos = record.position();
            // field "c"
            record.append(i); // as text.
            record.write(xs, 0, pos + 120 - record.position());

            // field "pad"
            int pos2 = record.position();
            record.append(i); // as text.
            record.write(ys, 0, pos2 + 120 - record.position());
            pht.endRecord(i);
        }
        long mid = System.nanoTime();
/*
    def query_process(host, port, pipe_to_parent, requests_per, dbname, rows, check, id):
      conn = pymongo.Connection(host, port)
      db = conn[dbname]
      gets = 0
      while True:
        for loop in xrange(0, requests_per):
          target = random.randrange(0, rows)
          o = db.c.find_one({'_id': target})
          assert o['_id'] == target
          if check:
            assert o['k'] == target
            sx = str(o['id'])
            lsx = len(sx)
            assert o['c'] == sx+'x'*(120-lsx)
            assert o['pad'] == sx+'y'*(120-lsx)

          gets += 1
*/
        int ptr = 0;
        final MyHashRecordIterator iterator = new MyHashRecordIterator();

        for (int i = 0; i < rows; i++) {
            // pseudo random walk
            ptr += 1019;
            ptr %= rows;

            iterator.found = false;
            iterator.ptr = ptr;
            pht.lookup(ptr, iterator);
            //noinspection ConstantConditions
            if (!iterator.found)
                assertTrue("Failed to find " + ptr, iterator.found);
        }
        long end = System.nanoTime();
        System.out.printf("Average time to add %.1f us, to lookup %.1f us%n",
                (mid - start) / rows / 1e3, (end - mid) / rows / 1e3);
    }

    /**
     * Use int which is [0, rows) as key
     * <p/>
     * Average time to add 1.2 us, to lookup 1.0 us
     */
    @Test
    public void testIndexedPerf() throws IOException {
/* def setup_db(host, port, dbname, rows):
      conn = pymongo.Connection(host, port)
      conn.drop_database(dbname)
      db = conn[dbname]
      for x in xrange(0, rows):
        sx = str(x)
        lsx = len(sx)
        db.c.save({'_id':x, 'k':x, 'c':sx+'x'*(120 - lsx), 'pad':sx+'y'*(120 - lsx)})
        if x % 1000 == 0:
          print '... row %d' % x
 */

        String basePath = baseDir + "/deleteme.int";
        Chronicle chronicle = new IntIndexedChronicle(basePath);
        deleteOnExit(basePath);

        Excerpt record = chronicle.createExcerpt();
        final int warmup = 20000;
        final int rows = 1000000;

        byte[] xs = new byte[120];
        Arrays.fill(xs, (byte) 'x');
        byte[] ys = new byte[120];
        Arrays.fill(ys, (byte) 'y');

        long start = 0;
        for (int i = -warmup; i < rows; i++) {
            if (i == 0)
                start = System.nanoTime();
            record.startExcerpt(256);
            record.writeInt(i + warmup);
            int pos = record.position();
            // field "c"
            record.append(i + warmup); // as text.
            record.write(xs, 0, pos + 120 - record.position());

            // field "pad"
            int pos2 = record.position();
            record.append(i + warmup); // as text.
            record.write(ys, 0, pos2 + 120 - record.position());
            record.finish();
        }
        long mid = System.nanoTime();
/*
    def query_process(host, port, pipe_to_parent, requests_per, dbname, rows, check, id):
      conn = pymongo.Connection(host, port)
      db = conn[dbname]
      gets = 0
      while True:
        for loop in xrange(0, requests_per):
          target = random.randrange(0, rows)
          o = db.c.find_one({'_id': target})
          assert o['_id'] == target
          if check:
            assert o['k'] == target
            sx = str(o['id'])
            lsx = len(sx)
            assert o['c'] == sx+'x'*(120-lsx)
            assert o['pad'] == sx+'y'*(120-lsx)

          gets += 1
*/
        int ptr = -1;
        final MyHashRecordIterator iterator = new MyHashRecordIterator();

        for (int i = 0; i < rows; i++) {
            // pseudo random walk
            ptr += 1019;
            ptr %= rows;

            assertTrue(record.index(ptr));
            iterator.found = false;
            iterator.ptr = ptr;
            iterator.onExcerpt(record);

            //noinspection ConstantConditions
            if (!iterator.found)
                assertTrue("Failed to find " + ptr, iterator.found);
        }
        long end = System.nanoTime();
        System.out.printf("Average time to add %.1f us, to lookup %.1f us%n",
                (mid - start) / rows / 1e3, (end - mid) / rows / 1e3);
    }

    static class MyHashRecordIterator implements PackedHashedTable.HashRecordIterator {
        final byte[] string1 = new byte[120];
        final byte[] string2 = new byte[120];
        public boolean found = false;
        public int ptr;

        @Override
        public boolean onExcerpt(Excerpt recordExcerpt) {
            int ptr0 = recordExcerpt.readInt();
            if (ptr0 == ptr) {
                recordExcerpt.readFully(string1);
                recordExcerpt.readFully(string2);
                return found = true;
            }
            return false;
        }
    }
}
