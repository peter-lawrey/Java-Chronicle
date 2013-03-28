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

package com.higherfrequencytrading.chronicle.tcp;

import com.higherfrequencytrading.chronicle.Chronicle;

import java.nio.ByteBuffer;

/**
 * @author peter.lawrey
 */
enum TcpUtil {
    ;
    static final int HEADER_SIZE = 12;
    static final int INITIAL_BUFFER_SIZE = 64 * 1024;

    public static ByteBuffer createBuffer(int minSize, Chronicle chronicle) {
        int newSize = (minSize + INITIAL_BUFFER_SIZE - 1) / INITIAL_BUFFER_SIZE * INITIAL_BUFFER_SIZE;
        return ByteBuffer.allocateDirect(newSize).order(chronicle.byteOrder());
    }
}
