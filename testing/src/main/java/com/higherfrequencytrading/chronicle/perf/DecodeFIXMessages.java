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

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTesters;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;

import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class DecodeFIXMessages {
    static final String TMP = System.getProperty("java.io.tmpdir");

    public static void main(String... args) throws IOException {
        String msg0 = "8=FIX.4.2 | 9=178 | 35=8 | 49=PHLX | 56=PERS | 52=20071123-05:30:00.000 | 11=ATOMNOCCC9990900 | 20=3 | 150=E | 39=E | 55=MSFT | 167=CS | 54=1 | 38=15 | 40=2 | 44=15 | 58=PHLX EQUITY TESTING | 59=0 | 47=C | 32=0 | 31=0 | 151=15 | 14=0 | 6=0 | 10=128 | ";
        String msg = msg0.replace(" | ", "" + (char) 1);

        String basePath = TMP + "/fix";
        ChronicleTools.deleteOnExit(basePath);
        IndexedChronicle chronicle = new IndexedChronicle(basePath);
        chronicle.useUnsafe(true);
        Excerpt excerpt = chronicle.createExcerpt();
        byte[] bytes = msg.getBytes();
        int runs = 1000000;
        for (int i = 0; i < runs; i++) {
            excerpt.startExcerpt(bytes.length);
            excerpt.write(bytes);
            excerpt.finish();
        }
        excerpt.index(-1);

        StringBuilder date = new StringBuilder();
        long start = System.nanoTime();
        while (excerpt.nextIndex()) {
            long l = excerpt.parseLong();
            assert l == 8;
            String s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("FIX.4.2");
            l = excerpt.parseLong();
            assert l == 9;
            l = excerpt.parseLong();

            l = excerpt.parseLong();
            assert l == 35;
            l = excerpt.parseLong();
            assert l == 8;

            l = excerpt.parseLong();
            assert l == 49;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("PHLX");

            l = excerpt.parseLong();
            assert l == 56;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("PERS");

            l = excerpt.parseLong();
            assert l == 52;
            date.setLength(0);
            excerpt.parseUTF(date, StopCharTesters.CONTROL_STOP);
            assert date.toString().equals("20071123-05:30:00.000");

            l = excerpt.parseLong();
            assert l == 11;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("ATOMNOCCC9990900");

            l = excerpt.parseLong();
            assert l == 20;
            l = excerpt.parseLong();
            assert l == 3;

            l = excerpt.parseLong();
            assert l == 150;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("E");

            l = excerpt.parseLong();
            assert l == 39;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("E");

            l = excerpt.parseLong();
            assert l == 55;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("MSFT");

            l = excerpt.parseLong();
            assert l == 167;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("CS");

            l = excerpt.parseLong();
            assert l == 54;
            l = excerpt.parseLong();
            assert l == 1;

            l = excerpt.parseLong();
            assert l == 38;
            l = excerpt.parseLong();
            assert l == 15;

            l = excerpt.parseLong();
            assert l == 40;
            l = excerpt.parseLong();
            assert l == 2;

            l = excerpt.parseLong();
            assert l == 44;
            l = excerpt.parseLong();
            assert l == 15;

            l = excerpt.parseLong();
            assert l == 58;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("PHLX EQUITY TESTING");

            l = excerpt.parseLong();
            assert l == 59;
            l = excerpt.parseLong();
            assert l == 0;

            l = excerpt.parseLong();
            assert l == 47;
            s = excerpt.parseEnum(String.class, StopCharTesters.CONTROL_STOP);
            assert s.equals("C");

            l = excerpt.parseLong();
            assert l == 32;
            l = excerpt.parseLong();
            assert l == 0;

            l = excerpt.parseLong();
            assert l == 31;
            l = excerpt.parseLong();
            assert l == 0;

            l = excerpt.parseLong();
            assert l == 151;
            l = excerpt.parseLong();
            assert l == 15;

            l = excerpt.parseLong();
            assert l == 14;
            l = excerpt.parseLong();
            assert l == 0;

            l = excerpt.parseLong();
            assert l == 6;
            l = excerpt.parseLong();
            assert l == 0;

            l = excerpt.parseLong();
            assert l == 10;
            l = excerpt.parseLong();
            assert l == 128;

            excerpt.finish();
        }
        long time = System.nanoTime() - start;
        System.out.printf("The average decode time was %,d ns%n", time / runs);
        chronicle.close();
    }
}
