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
import com.higherfrequencytrading.chronicle.impl.GlobalSettings;
import com.higherfrequencytrading.chronicle.impl.IntIndexedChronicle;

import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class PackedHashedTable {
    private final Chronicle hash;
    private final Chronicle records;
    private final HashRecord hashRecord;
    private final Excerpt recordsExcerpt;
    private final int maxRecordSize;
    private final String basePath;

    public PackedHashedTable(String basePath, int hashBucketBits, int hashCapacityBits, int maxRecordSize) throws IOException {
        this.basePath = basePath;
        this.maxRecordSize = maxRecordSize;
        hash = new IntIndexedChronicle(basePath + ".hash", Math.min(24, hashCapacityBits - 4));
        hashRecord = new HashRecord(hash.createExcerpt(), hashBucketBits, hashCapacityBits);

        while (hash.size() < 1 << hashBucketBits)
            hashRecord.addEntry();

        records = new IntIndexedChronicle(basePath + ".record", Math.min(26, hashCapacityBits - 2));
        recordsExcerpt = records.createExcerpt();
    }

    public Excerpt startRecord() {
        recordsExcerpt.startExcerpt(maxRecordSize);
        return recordsExcerpt;
    }

    public void endRecord(int hashCode) {
        int index = (int) recordsExcerpt.index();
        recordsExcerpt.finish();

        hashRecord.addRecord(hashCode, index);
    }

    public void deleteOnExit() {
        GlobalSettings.deleteOnExit(basePath + ".hash");
        GlobalSettings.deleteOnExit(basePath + ".record");
    }

    public interface HashRecordIterator {
        /**
         * @param recordExcerpt which might be a match
         * @return true to stop or false to continue
         */
        public boolean onExcerpt(Excerpt recordExcerpt);
    }

    public void lookup(int hashCode, HashRecordIterator iterator) {
        hashRecord.lookup(hashCode, recordsExcerpt, iterator);
    }

    public void close() {
        records.close();
        hash.close();
    }

    static class HashRecord {
        private static final int HEADER_SIZE = 4;
        private static final int PER_ENTRY_SIZE = 8; // hashCode and record index.
        private final Excerpt excerpt;
        private final int hashBucketMask;
        private final int sizeORecordBytes;

        HashRecord(Excerpt excerpt, int hashBucketBits, int hashCapacityBits) {
            this.excerpt = excerpt;
            this.hashBucketMask = (1 << hashBucketBits) - 1;
//            this.hashCapacityBits = hashCapacityBits;

            int hashBucketSizeBits = hashCapacityBits - hashBucketBits;
            sizeORecordBytes = HEADER_SIZE + (PER_ENTRY_SIZE << hashBucketSizeBits);
        }

        void addEntry() {
            excerpt.startExcerpt(sizeORecordBytes);
            excerpt.position(sizeORecordBytes);
            excerpt.finish();
        }

        public void addRecord(int hashCode, int index) {
            int bucket = bucket(hashCode);
            excerpt.index(bucket);
            int size = size();
            excerpt.position(size * PER_ENTRY_SIZE + HEADER_SIZE);
            excerpt.writeInt(hashCode);
            excerpt.writeInt(index);
            excerpt.writeInt(0, size + 1);
        }

        private int bucket(int hashCode) {
//            return ((hashCode >> 18) ^ (hashCode >> 9) ^ hashCode) & hashBucketMask;
            return hashCode & hashBucketMask;
        }

        private int size() {
            return excerpt.readInt(0);
        }

        public void lookup(int hashCode, Excerpt recordsExcerpt, HashRecordIterator iterator) {
            int bucket = bucket(hashCode);
            excerpt.index(bucket);
            for (int i = 0, size = size(); i < size; i++) {
                excerpt.position(i * PER_ENTRY_SIZE + HEADER_SIZE);
                int hashCode2 = excerpt.readInt();
                if (hashCode != hashCode2)
                    continue;
                int record = excerpt.readInt();
                recordsExcerpt.index(record);
                if (iterator.onExcerpt(recordsExcerpt))
                    break;
            }
        }
    }
}
