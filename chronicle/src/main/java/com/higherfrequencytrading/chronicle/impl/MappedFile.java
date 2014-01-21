/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.higherfrequencytrading.chronicle.impl;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This manages the full life cycle of a file and its mappings.
 */
public class MappedFile {
    private final FileChannel fileChannel;
    private final String basePath;
    private final long blockSize;
    private final long overlapSize;
    private final List<MappedMemory> maps = new ArrayList<MappedMemory>();
    // short list of the last two mappings.
    private volatile MappedMemory map0, map1;

    public MappedFile(String basePath, long blockSize) throws FileNotFoundException {
        this(basePath, blockSize, 0L);
    }

    public MappedFile(String basePath, long blockSize, long overlapSize) throws FileNotFoundException {
        this.basePath = basePath;
        this.blockSize = blockSize;
        this.overlapSize = overlapSize;
        fileChannel = new RandomAccessFile(basePath, "rw").getChannel();
    }

    public static MappedByteBuffer getMap(@NotNull FileChannel fileChannel, long start, int size) throws IOException {
        for (int i = 1; ; i++) {
            try {
//                long startTime = System.nanoTime();
                @SuppressWarnings("UnnecessaryLocalVariable")
                MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, start, size);
                map.order(ByteOrder.nativeOrder());
//                long time = System.nanoTime() - startTime;
//                System.out.printf("Took %,d us to map %,d MB%n", time / 1000, size / 1024 / 1024);
//                System.out.println("Map size: "+size);
                return map;
            } catch (IOException e) {
                if (e.getMessage() == null || !e.getMessage().endsWith("user-mapped section open")) {
                    throw e;
                }
                if (i < 10)
                    //noinspection CallToThreadYield
                    Thread.yield();
                else
                    try {
                        //noinspection BusyWait
                        Thread.sleep(1);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
            }
        }
    }

    public MappedMemory acquire(long index) throws IOException {
        return acquire(index, false);
    }

    public MappedMemory acquire(long index, boolean prefetch) throws IOException {
        MappedMemory map0 = this.map0, map1 = this.map1;
        if (map0 != null && map0.index() == index) {
            map0.reserve();
            return map0;
        }
        if (map1 != null && map1.index() == index) {
            map1.reserve();
            return map1;
        }
        return acquire0(index, prefetch);
    }

    private synchronized MappedMemory acquire0(long index, boolean prefetch) throws IOException {
        if (map1 != null)
            map1.release();
        map1 = map0;
        map0 = new MappedMemory(fileChannel.map(FileChannel.MapMode.READ_WRITE, index * blockSize, blockSize + overlapSize), index);
        map0.reserve();
        maps.add(map0);
        // clean up duds.
        for (int i = maps.size() - 1; i >= 0; i--) {
            if (maps.get(i).refCount() <= 0)
                maps.remove(i);
        }
        return map0;
    }

    public synchronized void close() throws IOException {
        if (map1 != null) {
            map1.release();
            map1 = null;
        }
        if (map0 != null) {
            map0.release();
            map0 = null;
        }
        // clean up errant maps.
        int count = 0;
        for (int i = maps.size() - 1; i >= 0; i--) {
            if (maps.get(i).refCount() <= 0) {
                maps.get(i).close();
                count++;
            }
        }
        if (count > 1)
            Logger.getLogger(MappedFile.class.getName()).info(basePath + " memory mappings left unreleased, num= " + count);
        maps.clear();
        fileChannel.close();
    }

    public long size() {
        try {
            return fileChannel.size();
        } catch (IOException e) {
            return 0;
        }
    }
}
