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

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

/**
 * The fastest and most extensible Chronicle.
 *
 * @author peter.lawrey
 */
public class IndexedChronicle extends AbstractChronicle {
    public static final long MAX_VIRTUAL_ADDRESS = 1L << 48;
    public static final int DEFAULT_DATA_BITS_SIZE = 27; // 1 << 27 or 128 MB.
    public static final int DEFAULT_DATA_BITS_SIZE32 = 22; // 1 << 22 or 4 MB.
    private static final Logger logger = Logger.getLogger(IndexedChronicle.class.getName());
    protected final int indexLowMask;

    private final int indexBitSize;
    private final int dataBitSize;
    private final int dataLowMask;
    private final MappedFile indexCache;
    private final MappedFile dataCache;
    private final ByteOrder byteOrder;
    private final boolean synchronousMode;

    private boolean useUnsafe = false;
    private AbstractExcerpt lastAppender;
    private Thread appendingThread;

    public IndexedChronicle(String basePath) throws IOException {
        this(basePath, ChronicleTools.is64Bit() ? DEFAULT_DATA_BITS_SIZE : DEFAULT_DATA_BITS_SIZE32);
    }

    public IndexedChronicle(String basePath, int dataBitSizeHint) throws IOException {
        this(basePath, dataBitSizeHint, ByteOrder.nativeOrder());
    }

    public IndexedChronicle(String basePath, int dataBitSizeHint, ByteOrder byteOrder) throws IOException {
        this(basePath, dataBitSizeHint, byteOrder, !ChronicleTools.is64Bit());
    }

    public IndexedChronicle(String basePath, int dataBitSizeHint, ByteOrder byteOrder, boolean minimiseByteBuffers) throws IOException {
        this(basePath, dataBitSizeHint, byteOrder, minimiseByteBuffers, false);
    }

    public IndexedChronicle(String basePath, int dataBitSizeHint, ByteOrder byteOrder, boolean minimiseByteBuffers, boolean synchronousMode) throws IOException {
        super(extractName(basePath));

        this.byteOrder = byteOrder;
        this.synchronousMode = synchronousMode;
        indexBitSize = Math.min(30, Math.max(12, dataBitSizeHint - 3));
        dataBitSize = Math.min(30, Math.max(12, dataBitSizeHint));
        indexLowMask = (1 << indexBitSize) - 1;
        dataLowMask = (1 << dataBitSize) - 1;

        File parentFile = new File(basePath).getParentFile();
        if (parentFile != null)
            //noinspection ResultOfMethodCallIgnored
            parentFile.mkdirs();
        indexCache = new MappedFile(basePath + ".index", 1L << indexBitSize);
        dataCache = new MappedFile(basePath + ".data", 1L << dataBitSize);

        // find the last record.
        long indexSize = indexCache.size() >>> indexBitSize();
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
    public long getIndexData(long indexId) {
        long indexOffset = indexId << indexBitSize();
        MappedMemory mappedMemory = acquireIndexBuffer(indexOffset);
        ByteBuffer indexBuffer = mappedMemory.buffer();
        long num = indexBuffer.getLong((int) (indexOffset & indexLowMask));
        mappedMemory.release();
        return num;
    }

    @NotNull
    protected MappedMemory acquireIndexBuffer(long startPosition) {
        if (startPosition >= MAX_VIRTUAL_ADDRESS)
            throwByteOrderIsIncorrect();
        try {
//            long start = System.nanoTime();
            MappedMemory mbb = indexCache.acquire(startPosition >>> indexBitSize);

//            long time = System.nanoTime() - start;
//            System.out.println(Thread.currentThread().getName()+": map "+time);
            mbb.buffer().order(byteOrder);
            return mbb;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    private MappedMemory throwByteOrderIsIncorrect() {
        throw new IllegalStateException("ByteOrder is incorrect.");
    }

    protected int indexBitSize() {
        return 3;
    }

    @Override
    public long sizeInBytes() {
        return indexCache.size() + dataCache.size();
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

    @NotNull
    @Override
    public Excerpt createExcerpt() {
        return useUnsafe ? new UnsafeExcerpt(this) : new ByteBufferExcerpt(this);
    }

    @Nullable
    @Override
    public MappedMemory acquireDataBuffer(long startPosition) {
        if (startPosition >= MAX_VIRTUAL_ADDRESS)
            return throwByteOrderIsIncorrect();
        try {
            MappedMemory mbb = dataCache.acquire(startPosition >>> dataBitSize);

            mbb.buffer().order(ByteOrder.nativeOrder());
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
    public long startExcerpt(AbstractExcerpt appender, int capacity) {
        boolean debug = false;
        assert debug = true;
        if (debug) {
            assert lastAppender == null || lastAppender == appender : "Chronicle cannot safely have more than one appender ";
            assert appendingThread == null : "Chronicle is already being appended to in " + appendingThread;
            lastAppender = appender;
            appendingThread = Thread.currentThread();
        }
        final long size = this.size;
        long startPosition = getIndexData(size);
        assert size == 0 || startPosition != 0 : "size: " + size + " startPosition: " + startPosition + " is the chronicle corrupted?";
        // does it overlap a ByteBuffer barrier.
        if ((startPosition & ~dataLowMask) != ((startPosition + capacity) & ~dataLowMask)) {
            // resize the previous entry.
            startPosition = (startPosition + dataLowMask) & ~dataLowMask;
            setIndexData(size, startPosition);
        }
        return startPosition;
    }

    @Override
    public void incrementSize(long expected) {
        if (size + 1 != expected)
            throw new ConcurrentModificationException("size: " + (size + 1) + ", expected: " + expected + ", Have you updated the chronicle without thread safety?");
        assert size == 0 || getIndexData(size) > 0 : "Failed to set the index at " + size + " was 0.";

        size++;
        appendingThread = null;
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

    @Override
    public void setIndexData(long indexId, long indexData) {
        long indexOffset = indexId << indexBitSize();
        MappedMemory indexBuffer = acquireIndexBuffer(indexOffset);
        indexBuffer.buffer().putLong((int) (indexOffset & indexLowMask), indexData);
        if (synchronousMode())
            indexBuffer.force();
        indexBuffer.release();
    }

    @Override
    public boolean synchronousMode() {
        return synchronousMode;
    }

    public void close() {
        try {
            indexCache.close();
            dataCache.close();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
