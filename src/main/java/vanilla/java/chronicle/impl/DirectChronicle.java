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

package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.Chronicle;

import java.nio.ByteBuffer;

/**
 * All Chronicle must actually implement this interface, however these method are intended for internal use only.
 *
 * @author peter.lawrey
 */
public interface DirectChronicle extends Chronicle {

    public long getIndexData(long indexId);

    ByteBuffer acquireDataBuffer(long startPosition);

    int positionInBuffer(long startPosition);

    void setIndexData(long indexId, long indexData);

    long startExcerpt(int capacity);

    void incrSize();
}
