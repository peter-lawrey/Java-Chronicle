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

import sun.nio.ch.DirectBuffer;
import vanilla.java.chronicle.Excerpt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter.lawrey
 */
public class TimeSeriesChronicle extends AbstractChronicle {
    private final List<MappedByteBuffer> indexBuffers = new ArrayList<MappedByteBuffer>();
    private final List<MappedByteBuffer> dataBuffers = new ArrayList<MappedByteBuffer>();
    private final int indexBitSize;
    private final int indexLowMask;
    private final int dataBitSize;
    private final int dataLowMask;
    private final FileChannel indexChannel;
    private final FileChannel dataChannel;

    public TimeSeriesChronicle(String basePath, int dataBitSizeHint) throws FileNotFoundException {
        indexBitSize = Math.min(30, Math.max(12, dataBitSizeHint - 4));
        dataBitSize = Math.min(30, Math.max(12, dataBitSizeHint));
        indexLowMask = (1 << indexBitSize) - 1;
        dataLowMask = (1 << dataBitSize) - 1;

        File parentFile = new File(basePath).getParentFile();
        if (parentFile != null)
            parentFile.mkdirs();
        indexChannel = new RandomAccessFile(basePath + ".index", "rw").getChannel();
        dataChannel = new RandomAccessFile(basePath + ".data", "rw").getChannel();
    }

    @Override
    public Excerpt createExcerpt() {
        return new ByteBufferExcerpt(this);
    }

    @Override
    public long getIndexData(long indexId) {
        long indexOffset = indexId << 3;
        ByteBuffer indexBuffer = acquireIndexBuffer(indexOffset);
        return indexBuffer.getLong((int) (indexOffset & indexLowMask));
    }

    private ByteBuffer acquireIndexBuffer(long startPosition) {
        int indexBufferId = (int) (startPosition >> indexBitSize);
        while (indexBuffers.size() <= indexBufferId) indexBuffers.add(null);
        ByteBuffer buffer = indexBuffers.get(indexBufferId);
        if (buffer != null)
            return buffer;
        try {
            MappedByteBuffer mbb = indexChannel.map(FileChannel.MapMode.READ_WRITE, startPosition & ~indexLowMask, 1 << indexBitSize);
            mbb.order(ByteOrder.nativeOrder());
            indexBuffers.set(indexBufferId, mbb);
            return mbb;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ByteBuffer acquireDataBuffer(long startPosition) {
        int dataBufferId = (int) (startPosition >> dataBitSize);
        while (dataBuffers.size() <= dataBufferId) dataBuffers.add(null);
        ByteBuffer buffer = dataBuffers.get(dataBufferId);
        if (buffer != null)
            return buffer;
        try {
            MappedByteBuffer mbb = dataChannel.map(FileChannel.MapMode.READ_WRITE, startPosition & ~dataLowMask, 1 << dataBitSize);
            mbb.order(ByteOrder.nativeOrder());
            dataBuffers.set(dataBufferId, mbb);
            return mbb;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int positionInBuffer(long startPosition) {
        return (int) (startPosition & dataLowMask);
    }

    @Override
    public void setIndexData(long indexId, long indexData) {
        long indexOffset = indexId << 3;
        ByteBuffer indexBuffer = acquireIndexBuffer(indexOffset);
        indexBuffer.putLong((int) (indexOffset & indexLowMask), indexData);
    }

    @Override
    public long startExcerpt(int capacity) {
        long startPosition = getIndexData(size);
        assert size > 0 || startPosition != 0;
        // does it overlap a ByteBuffer barrier.
        if ((startPosition & ~dataLowMask) != ((startPosition + capacity) & ~dataLowMask)) {
            // resize the previous entry.
            startPosition = (startPosition + dataLowMask) & ~dataLowMask;
            setIndexData(size, startPosition);
        }
        return startPosition;
    }

    @Override
    public void incrSize() {
        size++;
    }

    /**
     * Clear any previous data in the Chronicle.
     * <p/>
     * Added for testing purposes.
     */
    public void clear() {
        size = 0;
        setIndexData(1, 0);
    }

    public void close() {
        clearAll(indexChannel, indexBuffers);
        clearAll(dataChannel, dataBuffers);
    }

    private void clearAll(FileChannel channel, List<MappedByteBuffer> buffers) {
        try {
            indexChannel.close();
        } catch (IOException ignored) {
        }
        for (MappedByteBuffer buffer : buffers) {
            if (buffer instanceof DirectBuffer)
                ((DirectBuffer) buffer).cleaner().clean();
        }
        buffers.clear();
    }
}
