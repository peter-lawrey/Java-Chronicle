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

package com.higherfrequencytrading.chronicle;

/**
 * @author peter.lawrey
 */
public interface ByteStringAppender extends Appendable {
    int length();

    int capacity();

    ByteStringAppender append(CharSequence s);

    ByteStringAppender append(CharSequence s, int start, int end);

    ByteStringAppender append(byte[] str);

    ByteStringAppender append(byte[] str, int offset, int len);

    ByteStringAppender append(boolean b);

    ByteStringAppender append(char c);

    ByteStringAppender append(Enum value);

    ByteStringAppender append(int i);

    ByteStringAppender append(long l);

    ByteStringAppender appendTime(long timeInMS);

// TODO
//   ByteStringAppender append(float f);

// TODO
//    ByteStringAppender append(float f, int precision);

    ByteStringAppender append(double d);

    ByteStringAppender append(double d, int precision);
}
