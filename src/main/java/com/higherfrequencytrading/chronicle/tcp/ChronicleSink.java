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
package com.higherfrequencytrading.chronicle.tcp;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This listens to a ChronicleSource and copies new entries. This SInk can be any number of excerpt behind the source and can be restart many times without losing data.
 * <p/>
 * Can be used as a component or run as a stand alone service.
 *
 * @author peter.lawrey
 */
public class ChronicleSink<C extends Chronicle> implements Closeable {
    private final C chronicle;
    private final SocketAddress address;
    private final ExcerptListener<C> listener;

    private final ExecutorService service;
    private final String name;
    private final Logger logger;
    private volatile boolean closed = false;

    public ChronicleSink(C chronicle, String hostname, int port) {
        this(chronicle, hostname, port, NullExcerptListener.INSTANCE);
    }

    public ChronicleSink(C chronicle, String hostname, int port, ExcerptListener<C> listener) {
        this.chronicle = chronicle;
        this.listener = listener;
        this.address = new InetSocketAddress(hostname, port);
        name = chronicle.name() + '@' + hostname + ':' + port;
        logger = Logger.getLogger(getClass().getName() + '.' + chronicle);
        service = Executors.newSingleThreadExecutor(new NamedThreadFactory(name));
        service.execute(new Sink());
    }

    public static void main(String... args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: java " + ChronicleSink.class.getName() + " {chronicle-base-path} {hostname} {port}");
            System.exit(-1);
        }
        int dataBitsHintSize = Integer.getInteger("dataBitsHintSize", 27);
        String def = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? "Big" : "Little";
        ByteOrder byteOrder = System.getProperty("byteOrder", def).equalsIgnoreCase("Big") ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        String basePath = args[0];
        String hostname = args[1];
        int port = Integer.parseInt(args[2]);
        IndexedChronicle ic = new IndexedChronicle(basePath, dataBitsHintSize, byteOrder);
        ChronicleSink cs = new ChronicleSink(ic, hostname, port);
    }

    class Sink implements Runnable {
        Excerpt excerpt = chronicle.createExcerpt();

        @Override
        public void run() {
            SocketChannel sc = null;
            try {
                while (!closed) {
                    if (sc == null || !sc.isOpen())
                        sc = createConnection();
                    else
                        readNextExcerpt(sc);
                }
            } finally {
                closeSocket(sc);
            }
        }

        private SocketChannel createConnection() {
            while (!closed) {
                try {
                    SocketChannel sc = SocketChannel.open(address);
                    ByteBuffer bb = ByteBuffer.allocate(8);
                    bb.putLong(0, chronicle.size());
                    while (bb.remaining() > 0 && sc.write(bb) > 0) ;
                    if (bb.remaining() > 0) throw new EOFException();
                    return sc;

                } catch (IOException e) {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Failed to connect to " + address + " retrying", e);
                    else if (logger.isLoggable(Level.INFO))
                        logger.log(Level.INFO, "Failed to connect to " + address + " retrying " + e);
                }
            }
            return null;
        }

        private void readNextExcerpt(SocketChannel sc) {
            ByteBuffer bb = TcpUtil.createBuffer(1, chronicle); // minimum size
            try {
                while (!closed) {
                    readHeader(sc, bb);
                    long index = bb.getLong(0);
                    long size = bb.getInt(8);
                    if (index != chronicle.size())
                        throw new StreamCorruptedException("Expected index " + chronicle.size() + " but got " + index);
                    if (size > Integer.MAX_VALUE || size < 0)
                        throw new StreamCorruptedException("size was " + size);

                    excerpt.startExcerpt((int) size);
                    // perform a progressive copy of data.
                    long remaining = size;
                    bb.position(0);
                    while (remaining > 0) {
                        int size2 = (int) Math.min(bb.capacity(), remaining);
                        bb.limit(size2);
                        if (sc.read(bb) < 0) throw new EOFException();
                        bb.flip();
                        remaining -= bb.remaining();
                        excerpt.write(bb);
                    }
                    excerpt.finish();

                    excerpt.index(index);
                    listener.onExcerpt(excerpt);
                }
            } catch (IOException e) {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Lost connection to " + address + " retrying", e);
                else if (logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO, "Lost connection to " + address + " retrying " + e);
            }
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Disconnected from " + address);
        }

        private void readHeader(SocketChannel sc, ByteBuffer bb) throws IOException {
            bb.position(0);
            bb.limit(TcpUtil.HEADER_SIZE);
            while (bb.remaining() > 0 && sc.read(bb) > 0) ;
            if (bb.remaining() > 0) throw new EOFException();
        }
    }

    void closeSocket(SocketChannel sc) {
        if (sc != null)
            try {
                sc.close();
            } catch (IOException e) {
                logger.warning("Error closing socket " + e);
            }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        service.shutdownNow();
        chronicle.close();
    }
}
