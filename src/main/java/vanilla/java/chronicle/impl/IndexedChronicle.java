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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * The fastest and most extensible Chronicle.
 *
 * @author peter.lawrey
 */
public class IndexedChronicle extends AbstractChronicle {
    public static final long MAX_VIRTUAL_ADDRESS = 1L << 48;
    private final List<MappedByteBuffer> indexBuffers = new ArrayList<MappedByteBuffer>();
    private final List<MappedByteBuffer> dataBuffers = new ArrayList<MappedByteBuffer>();
    private final int indexBitSize;
    protected final int indexLowMask;
    private final int dataBitSize;
    private final int dataLowMask;
    private final FileChannel indexChannel;
    private final FileChannel dataChannel;
    private boolean useUnsafe = false;
    private final ByteOrder byteOrder;

    public IndexedChronicle(String basePath, int dataBitSizeHint) throws IOException {
        this(basePath, dataBitSizeHint, ByteOrder.nativeOrder());
    }

    public IndexedChronicle(String basePath, int dataBitSizeHint, ByteOrder byteOrder) throws IOException {
        super(extractName(basePath));

        this.byteOrder = byteOrder;
        indexBitSize = Math.min(30, Math.max(12, dataBitSizeHint - 4));
        dataBitSize = Math.min(30, Math.max(12, dataBitSizeHint));
        indexLowMask = (1 << indexBitSize) - 1;
        dataLowMask = (1 << dataBitSize) - 1;

        File parentFile = new File(basePath).getParentFile();
        if (parentFile != null)
            parentFile.mkdirs();
        indexChannel = new RandomAccessFile(basePath + ".index", "rw").getChannel();
        dataChannel = new RandomAccessFile(basePath + ".data", "rw").getChannel();

        // find the last record.
        long indexSize = indexChannel.size() >>> indexBitSize();
        if (indexSize > 0) {
            while (--indexSize > 0 && getIndexData(indexSize) == 0) ;
            System.out.println(basePath + ", size=" + indexSize);
            size = indexSize;
        } else {
            System.out.println(basePath + " created.");
        }
    }

    private static String extractName(String basePath) {
        File file = new File(basePath);
        String name = file.getName();
        if (name != null && name.length() > 0)
            return name;
        file = file.getParentFile();
        if (file == null) return "chronicle";
        name = file.getName();
        if (name != null && name.length() > 0)
            return name;
        return "chronicle";
    }

    protected int indexBitSize() {
        return 3;
    }

    public void useUnsafe(boolean useUnsafe) {
        this.useUnsafe = useUnsafe && byteOrder == ByteOrder.nativeOrder();
    }

    public boolean useUnsafe() {
        return useUnsafe;
    }

    public ByteOrder byteOrder() {
        return byteOrder;
    }

    @Override
    public Excerpt<IndexedChronicle> createExcerpt() {
        return useUnsafe ? new UnsafeExcerpt<IndexedChronicle>(this) : new ByteBufferExcerpt<IndexedChronicle>(this);
    }

    @Override
    public long getIndexData(long indexId) {
        long indexOffset = indexId << indexBitSize();
        ByteBuffer indexBuffer = acquireIndexBuffer(indexOffset);
        return indexBuffer.getLong((int) (indexOffset & indexLowMask));
    }

    protected ByteBuffer acquireIndexBuffer(long startPosition) {
        if (startPosition >= MAX_VIRTUAL_ADDRESS)
            throw new IllegalStateException("ByteOrder is incorrect.");
        int indexBufferId = (int) (startPosition >> indexBitSize);
        while (indexBuffers.size() <= indexBufferId) indexBuffers.add(null);
        ByteBuffer buffer = indexBuffers.get(indexBufferId);
        if (buffer != null)
            return buffer;
        try {
//            long start = System.nanoTime();
            MappedByteBuffer mbb = indexChannel.map(FileChannel.MapMode.READ_WRITE, startPosition & ~indexLowMask, 1 << indexBitSize);
//            long time = System.nanoTime() - start;
//            System.out.println(Thread.currentThread().getName()+": map "+time);
            mbb.order(byteOrder);
            indexBuffers.set(indexBufferId, mbb);
            return mbb;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ByteBuffer acquireDataBuffer(long startPosition) {
        if (startPosition >= MAX_VIRTUAL_ADDRESS)
            throw new IllegalStateException("ByteOrder is incorrect.");
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
        long indexOffset = indexId << indexBitSize();
        ByteBuffer indexBuffer = acquireIndexBuffer(indexOffset);
        indexBuffer.putLong((int) (indexOffset & indexLowMask), indexData);
    }

    @Override
    public long startExcerpt(int capacity) {
        long startPosition = getIndexData(size);
        assert size == 0 || startPosition != 0;
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
        try {
            clearAll(indexChannel, indexBuffers);
        } finally {
            clearAll(dataChannel, dataBuffers);
        }
    }

    private void clearAll(FileChannel channel, List<MappedByteBuffer> buffers) {
        try {
            for (MappedByteBuffer buffer : buffers) {
                if (buffer != null) {
                    buffer.force();
                }
            }
        } finally {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
            for (MappedByteBuffer buffer : buffers) {
                if (buffer instanceof DirectBuffer)
                    ((DirectBuffer) buffer).cleaner().clean();
            }
        }
        buffers.clear();
    }
}
