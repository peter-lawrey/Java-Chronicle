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

package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.chronicle.EnumeratedMarshaller;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTester;

import java.util.Date;

/**
 * @author peter.lawrey
 */
public class DateMarshaller implements EnumeratedMarshaller<Date> {
    private final Date[] interner;

    public DateMarshaller(int size) {
        int size2 = 128;
        while (size2 < size && size2 < (1 << 20)) size2 <<= 1;
        interner = new Date[size2];
    }

    @Override
    public Class<Date> classMarshaled() {
        return Date.class;
    }

    @Override
    public void write(Excerpt excerpt, Date date) {
        excerpt.writeLong(date.getTime());
    }

    @Override
    public Date read(Excerpt excerpt) {
        return lookupDate(excerpt.readLong());
    }

    @Override
    public Date parse(Excerpt excerpt, StopCharTester tester) {
        return lookupDate(excerpt.readLong());
    }

    private Date lookupDate(long time) {
        int idx = hashFor(time);
        Date date = interner[idx];
        if (date != null && date.getTime() == time)
            return date;
        return interner[idx] = new Date(time);
    }

    private int hashFor(long time) {
        long h = time;
        h ^= (h >>> 41) ^ (h >>> 21);
        h ^= (h >>> 14) ^ (h >>> 7);
        return (int) (h & (interner.length - 1));
    }
}
