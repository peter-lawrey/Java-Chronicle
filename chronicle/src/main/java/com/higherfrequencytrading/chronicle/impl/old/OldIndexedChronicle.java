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

package com.higherfrequencytrading.chronicle.impl.old;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.AbstractChronicle;
import com.higherfrequencytrading.chronicle.impl.ByteBufferExcerpt;
import com.higherfrequencytrading.chronicle.impl.UnsafeExcerpt;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * The fastest and most extensible Chronicle.
 *
 * @author peter.lawrey
 */
public class OldIndexedChronicle extends AbstractChronicle {
    public static final long MAX_VIRTUAL_ADDRESS = 1L << 48;
    public static final int DEFAULT_DATA_BITS_SIZE = 27; // 1 << 27 or 128 MB.
    public static final int DEFAULT_DATA_BITS_SIZE32 = 22; // 1 << 22 or 4 MB.
    private static final Logger logger = Logger.getLogger(OldIndexedChronicle.class.getName());

    // used if minimiseByteBuffers is false.  This is faster but uses much more virtual memory.
    private final List<MappedByteBuffer> indexBuffers = new ArrayList<MappedByteBuffer>();
    private final List<MappedByteBuffer> dataBuffers = new ArrayList<MappedByteBuffer>();
    // used if minimiseByteBuffers is true;
    private int lastIndexId = -1;
    private MappedByteBuffer lastIndexBuffer = null;
    private int lastDataId = -1;
    private MappedByteBuffer lastDataBuffer = null;
    // end of used.
    private final int indexBitSize;
    protected final int indexLowMask;
    private final int dataBitSize;
    private final int dataLowMask;
    private final FileChannel indexChannel;
    private final FileChannel dataChannel;
    private boolean useUnsafe = false;
    private final ByteOrder byteOrder;
    private final boolean minimiseByteBuffers;
    private final boolean synchronousMode;

    public OldIndexedChronicle(String basePath) throws IOException {
        this(basePath, ChronicleTools.is64Bit() ? DEFAULT_DATA_BITS_SIZE : DEFAULT_DATA_BITS_SIZE32);
    }

    public OldIndexedChronicle(String basePath, int dataBitSizeHint) throws IOException {
        this(basePath, dataBitSizeHint, ByteOrder.nativeOrder());
    }

    public OldIndexedChronicle(String basePath, int dataBitSizeHint, ByteOrder byteOrder) throws IOException {
        this(basePath, dataBitSizeHint, byteOrder, !ChronicleTools.is64Bit());
    }

    public OldIndexedChronicle(String basePath, int dataBitSizeHint, ByteOrder byteOrder, boolean minimiseByteBuffers) throws IOException {
        this(basePath, dataBitSizeHint, byteOrder, minimiseByteBuffers, false);
    }

    public OldIndexedChronicle(String basePath, int dataBitSizeHint, ByteOrder byteOrder, boolean minimiseByteBuffers, boolean synchronousMode) throws IOException {
        super(extractName(basePath));

        this.byteOrder = byteOrder;
        this.minimiseByteBuffers = minimiseByteBuffers;
        this.synchronousMode = synchronousMode;
        indexBitSize = Math.min(30, Math.max(12, dataBitSizeHint - 3));
        dataBitSize = Math.min(30, Math.max(12, dataBitSizeHint));
        indexLowMask = (1 << indexBitSize) - 1;
        dataLowMask = (1 << dataBitSize) - 1;

        File parentFile = new File(basePath).getParentFile();
        if (parentFile != null)
            //noinspection ResultOfMethodCallIgnored
            parentFile.mkdirs();
        indexChannel = new RandomAccessFile(basePath + ".index", synchronousMode ? "rwd" : "rw").getChannel();
        dataChannel = new RandomAccessFile(basePath + ".data", synchronousMode ? "rwd" : "rw").getChannel();

        // find the last record.
        long indexSize = indexChannel.size() >>> indexBitSize();
        if (indexSize > 0) {
            indexSize--;
            while (indexSize > 0 && getIndexData(indexSize) == 0)
                indexSize--;
            logger.info(basePath + ", size=" + indexSize);
            size = indexSize;
        } else {
            logger.info(basePath + " created.");
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

    @Override
    public long sizeInBytes() {
        try {
            return indexChannel.size() + dataChannel.size();
        } catch (IOException ignored) {
            return -1;
        }
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
    public boolean synchronousMode() {
        return synchronousMode;
    }

    @Override
    public Excerpt createExcerpt() {
        return useUnsafe ? new OldUnsafeExcerpt(this) : new OldByteBufferExcerpt(this);
    }

    @Override
    public long getIndexData(long indexId) {
        long indexOffset = indexId << indexBitSize();
        ByteBuffer indexBuffer = acquireIndexBuffer(indexOffset);
        return indexBuffer.getLong((int) (indexOffset & indexLowMask));
    }

    protected MappedByteBuffer acquireIndexBuffer(long startPosition) {
        if (startPosition >= MAX_VIRTUAL_ADDRESS)
            throwByteOrderIsIncorrect();
        int indexBufferId = (int) (startPosition >> indexBitSize);
        if (minimiseByteBuffers) {
            if (lastIndexId == indexBufferId)
                return lastIndexBuffer;

        } else {
            while (indexBuffers.size() <= indexBufferId) indexBuffers.add(null);
            MappedByteBuffer buffer = indexBuffers.get(indexBufferId);
            if (buffer != null)
                return buffer;
        }
        try {
//            long start = System.nanoTime();
            MappedByteBuffer mbb;
            try {
                mbb = indexChannel.map(FileChannel.MapMode.READ_WRITE, startPosition & ~indexLowMask, 1 << indexBitSize);
            } catch (OutOfMemoryError e) {
                System.gc();
                mbb = indexChannel.map(FileChannel.MapMode.READ_WRITE, startPosition & ~indexLowMask, 1 << indexBitSize);
            }
//            long time = System.nanoTime() - start;
//            System.out.println(Thread.currentThread().getName()+": map "+time);
            mbb.order(byteOrder);
            if (minimiseByteBuffers) {
                lastIndexBuffer = mbb;
                lastIndexId = indexBufferId;
            } else {
                indexBuffers.set(indexBufferId, mbb);
            }
            return mbb;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public MappedByteBuffer acquireDataBuffer(long startPosition) {
        if (startPosition >= MAX_VIRTUAL_ADDRESS)
            return throwByteOrderIsIncorrect();
        int dataBufferId = (int) (startPosition >> dataBitSize);
        if (minimiseByteBuffers) {
            if (lastDataId == dataBufferId) {
                return lastDataBuffer;
            }
        } else {
            while (dataBuffers.size() <= dataBufferId) dataBuffers.add(null);
            MappedByteBuffer buffer = dataBuffers.get(dataBufferId);
            if (buffer != null)
                return buffer;
        }
        try {
            MappedByteBuffer mbb;
            try {
                mbb = dataChannel.map(FileChannel.MapMode.READ_WRITE, startPosition & ~dataLowMask, 1 << dataBitSize);
            } catch (OutOfMemoryError e) {
                System.gc();
                mbb = dataChannel.map(FileChannel.MapMode.READ_WRITE, startPosition & ~dataLowMask, 1 << dataBitSize);
            }
            mbb.order(ByteOrder.nativeOrder());
            if (minimiseByteBuffers) {
                lastDataBuffer = mbb;
                lastDataId = dataBufferId;
            } else {
                dataBuffers.set(dataBufferId, mbb);
            }
            return mbb;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private MappedByteBuffer throwByteOrderIsIncorrect() {
        throw new IllegalStateException("ByteOrder is incorrect.");
    }

    @Override
    public int positionInBuffer(long startPosition) {
        return (int) (startPosition & dataLowMask);
    }

    @Override
    public void setIndexData(long indexId, long indexData) {
        long indexOffset = indexId << indexBitSize();
        MappedByteBuffer indexBuffer = acquireIndexBuffer(indexOffset);
        indexBuffer.putLong((int) (indexOffset & indexLowMask), indexData);
        if (synchronousMode())
            indexBuffer.force();
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
    public void incrementSize() {
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
