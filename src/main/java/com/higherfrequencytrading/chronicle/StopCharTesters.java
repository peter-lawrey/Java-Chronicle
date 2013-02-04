/*
 * Copyright 2011 Peter Lawrey
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
package com.higherfrequencytrading.chronicle;

/**
 * @author plawrey
 */
public enum StopCharTesters {
    ;

    public static StopCharTester forChars(CharSequence sequence) {
        if (sequence.length() == 1)
            return forChar(sequence.charAt(0));
        return new CSCSTester(sequence);
    }

    public static StopCharTester forChar(char ch) {
        return new CharCSTester(ch);
    }

    static class CSCSTester implements StopCharTester {
        private final String seperators;

        public CSCSTester(CharSequence cs) {
            seperators = cs.toString();
        }

        @Override
        public boolean isStopChar(int ch) {
            return seperators.indexOf(ch) >= 0;
        }
    }

    static class CharCSTester implements StopCharTester {
        private final char ch;

        public CharCSTester(char ch) {
            this.ch = ch;
        }

        @Override
        public boolean isStopChar(int ch) {
            return this.ch == ch;
        }
    }
}
