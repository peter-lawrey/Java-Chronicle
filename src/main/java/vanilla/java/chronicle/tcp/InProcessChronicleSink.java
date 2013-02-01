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
package vanilla.java.chronicle.tcp;

import vanilla.java.chronicle.Chronicle;
import vanilla.java.chronicle.EnumeratedMarshaller;
import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.WrappedExcerpt;

import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This listens to a ChronicleSource and copies new entries. This SInk can be any number of excerpt behind the source and can be restart many times without losing data.
 * <p/>
 * Can be used as a component with lower over head than ChronicleSink
 *
 * @author plawrey
 */
public class InProcessChronicleSink<C extends Chronicle> implements Chronicle {
    private final C chronicle;
    private final SocketAddress address;
    private final Excerpt excerpt;
    private final String name;
    private final Logger logger;
    private volatile boolean closed = false;

    public InProcessChronicleSink(C chronicle, String hostname, int port) {
        this.chronicle = chronicle;
        this.address = new InetSocketAddress(hostname, port);
        name = chronicle.name() + '@' + hostname + ':' + port;
        logger = Logger.getLogger(getClass().getName() + '.' + chronicle);
        excerpt = chronicle.createExcerpt();
        readBuffer = TcpUtil.createBuffer(256 * 1024, chronicle);
    }

    @Override
    public String name() {
        return chronicle.name();
    }

    @Override
    public Excerpt createExcerpt() {
        return new SinkExcerpt();
    }

    @Override
    public long size() {
        return chronicle.size();
    }

    @Override
    public long sizeInBytes() {
        return chronicle.sizeInBytes();
    }

    @Override
    public ByteOrder byteOrder() {
        return chronicle.byteOrder();
    }

    @Override
    public <E> void setEnumeratedMarshaller(EnumeratedMarshaller<E> marshaller) {
        chronicle.setEnumeratedMarshaller(marshaller);
    }

    private class SinkExcerpt extends WrappedExcerpt {
        public SinkExcerpt() {
            super(chronicle.createExcerpt());
        }

        @Override
        public boolean nextIndex() {
            if (super.nextIndex()) return true;
            readNext();
            return super.nextIndex();
        }

        @Override
        public boolean index(long index) throws IndexOutOfBoundsException {
            if (super.index(index)) return true;
            readNext();
            return index(index);
        }
    }

    private SocketChannel sc = null;
    private long scIndex = -1;
    private boolean scFirst = true;

    void readNext() {
        if (sc == null || !sc.isOpen()) {
            sc = createConnection();
            scFirst = true;
        }
        if (sc != null)
            readNextExcerpt(sc);
    }

    private SocketChannel createConnection() {
        while (!closed) {
            try {
                readBuffer.clear();
                readBuffer.limit(0);

                SocketChannel sc = SocketChannel.open(address);
                sc.socket().setReceiveBufferSize(256 * 1024);
                logger.info("Connected to " + address);
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
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private final ByteBuffer readBuffer; // minimum size

    private void readNextExcerpt(SocketChannel sc) {
        try {
            if (closed) return;

            if (readBuffer.remaining() < TcpUtil.HEADER_SIZE) {
                if (readBuffer.remaining() == 0)
                    readBuffer.clear();
                else
                    readBuffer.compact();
                if (sc.read(readBuffer) < 0) {
                    sc.close();
                    return;
                }
                readBuffer.flip();
            }

            if (scFirst) {
                scIndex = readBuffer.getLong();
//                    System.out.println("ri " + scIndex);
                scFirst = false;
            }
            long size = readBuffer.getInt();
//                System.out.println("size="+size);
            if (scIndex != chronicle.size())
                throw new StreamCorruptedException("Expected index " + chronicle.size() + " but got " + scIndex);
            if (size > Integer.MAX_VALUE || size < 0)
                throw new StreamCorruptedException("size was " + size);

            excerpt.startExcerpt((int) size);
            // perform a progressive copy of data.
            long remaining = size;
            int limit = readBuffer.limit();

            int size2 = (int) Math.min(readBuffer.remaining(), remaining);
            remaining -= size2;
            readBuffer.limit(readBuffer.position() + size2);
            excerpt.write(readBuffer);
            // reset the limit;
            readBuffer.limit(limit);

            // needs more than one read.
            while (remaining > 0) {
//                System.out.println("++ read");
                readBuffer.clear();
                int size3 = (int) Math.min(readBuffer.capacity(), remaining);
                readBuffer.limit(size3);
//                    System.out.println("... reading");
                if (sc.read(readBuffer) < 0) throw new EOFException();
                readBuffer.flip();
//                    System.out.println("r " + ChronicleTest.asString(bb));
                remaining -= readBuffer.remaining();
                excerpt.write(readBuffer);
            }

            excerpt.finish();
            scIndex++;
        } catch (IOException e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Lost connection to " + address + " retrying", e);
            else if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Lost connection to " + address + " retrying " + e);
        }
    }

    private void readHeader(SocketChannel sc, ByteBuffer bb, boolean first) throws IOException {
        bb.position(0);
        bb.limit(first ? TcpUtil.HEADER_SIZE : 4);
        while (bb.remaining() > 0 && sc.read(bb) > 0) ;
        if (bb.remaining() > 0) throw new EOFException();
        bb.flip();
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
    public void close() {
        closed = true;
        closeSocket(sc);
        chronicle.close();
    }
}
