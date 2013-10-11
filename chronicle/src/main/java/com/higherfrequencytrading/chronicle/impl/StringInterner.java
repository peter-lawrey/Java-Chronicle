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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter.lawrey
 */
public class StringInterner {
    @NotNull
    private final String[] interner;
    private final int mask;

    public StringInterner(int capacity) {
        int n = nextPower2(capacity, 128);
        interner = new String[n];
        mask = n - 1;
    }

    private static boolean isEqual(@Nullable CharSequence s, @NotNull CharSequence cs) {
        if (s == null) return false;
        if (s.length() != cs.length()) return false;
        for (int i = 0; i < cs.length(); i++)
            if (s.charAt(i) != cs.charAt(i))
                return false;
        return true;
    }

    @NotNull
    public String intern(@NotNull CharSequence cs) {
        int h = (int) (hashOf(cs) & mask);
        String s = interner[h];
        if (isEqual(s, cs))
            return s;
        String s2 = cs.toString();
        return interner[h] = s2;
    }

    public static int nextPower2(int n, int min) {
        if (n < min) return min;
        if ((n & (n - 1)) == 0) return n;
        int i = min;
        while (i < n) {
            i *= 2;
            if (i <= 0) return 1 << 30;
        }
        return i;
    }

    public static long hashOf(CharSequence s) {
        long hash = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            hash = 57 * hash + s.charAt(i);
        }
        return hash;
    }
}
