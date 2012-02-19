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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Fast Chronicle with a compact index when you don't need more the 4 GB of data.
 *
 * @author peter.lawrey
 */
public class IntIndexedChronicle extends IndexedChronicle {
    private static final long LONG_MASK = -1L >>> -32;

    public IntIndexedChronicle(String basePath, int dataBitSizeHint) throws IOException {
        super(basePath, dataBitSizeHint);
    }

    public IntIndexedChronicle(String basePath, int dataBitSizeHint, ByteOrder byteOrder) throws IOException {
        super(basePath, dataBitSizeHint, byteOrder);
    }

    @Override
    protected int indexBitSize() {
        return 2;
    }

    @Override
    public long getIndexData(long indexId) {
        long indexOffset = indexId << indexBitSize();
        ByteBuffer indexBuffer = acquireIndexBuffer(indexOffset);
        return indexBuffer.getInt((int) (indexOffset & indexLowMask)) & LONG_MASK;
    }

    @Override
    public void setIndexData(long indexId, long indexData) {
        long indexOffset = indexId << indexBitSize();
        ByteBuffer indexBuffer = acquireIndexBuffer(indexOffset);
        assert indexData <= LONG_MASK;
        indexBuffer.putInt((int) (indexOffset & indexLowMask), (int) indexData);
    }
}
