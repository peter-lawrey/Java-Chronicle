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
import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Chronicle as a service to be replicated to any number of clients.  Clients can restart from where ever they are up to.
 * <p/>
 * Can be used ad a component or run as a stand alone service.
 *
 * @author peter.lawrey
 */
public class ChronicleSource<C extends Chronicle> implements Closeable {

    private final C chronicle;
    private final ServerSocketChannel server;
    private final int delayNS;

    private final String name;
    private final ExecutorService service;
    private final Logger logger;

    private volatile boolean closed = false;

    public ChronicleSource(C chronicle, int port, int delayNS) throws IOException {
        this.chronicle = chronicle;
        this.delayNS = delayNS;
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(port));
        name = chronicle.name() + "@" + port;
        logger = Logger.getLogger(getClass().getName() + "." + name);
        service = Executors.newCachedThreadPool(new NamedThreadFactory(name));
        service.execute(new Acceptor());
    }

    public static void main(String... args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java " + ChronicleSource.class.getName() + " {chronicle-base-path} {port} [delayNS]");
            System.exit(-1);
        }
        int dataBitsHintSize = Integer.getInteger("dataBitsHintSize", 24);
        String def = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? "Big" : "Little";
        ByteOrder byteOrder = System.getProperty("byteOrder", def).equalsIgnoreCase("Big") ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        String basePath = args[0];
        int port = Integer.parseInt(args[1]);
        int delayNS = 5 * 1000 * 1000;
        if (args.length > 2)
            delayNS = Integer.parseInt(args[2]);
        IndexedChronicle ic = new IndexedChronicle(basePath, dataBitsHintSize, byteOrder);
        ChronicleSource cs = new ChronicleSource(ic, port, delayNS);
    }

    class Acceptor implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName(name + "-acceptor");
            try {
                while (!closed) {
                    SocketChannel socket = server.accept();
                    service.execute(new Handler(socket));
                }
            } catch (IOException e) {
                if (!closed)
                    logger.log(Level.SEVERE, "Acceptor dying", e);
            }
        }
    }

    class Handler implements Runnable {
        private final SocketChannel socket;

        public Handler(SocketChannel socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                long index = readIndex(socket);
                Excerpt excerpt = chronicle.createExcerpt();
                ByteBuffer bb = TcpUtil.createBuffer(1, chronicle);
                while (!closed) {
                    while (!excerpt.index(index))
                        pause(delayNS);
                    int size = excerpt.capacity();
                    int capacity = size + TcpUtil.HEADER_SIZE;
                    if (capacity > bb.capacity())
                        bb = TcpUtil.createBuffer(capacity, chronicle);

                    bb.clear();
                    bb.putLong(index);
                    bb.putLong(size);
                    excerpt.read(bb);
                    bb.flip();
                    while (bb.remaining() > 0 && socket.write(bb) > 0) ;
                    if (bb.remaining() > 0) throw new EOFException("Failed to send index=" + index);
                }
            } catch (IOException e) {
                if (!closed)
                    logger.log(Level.INFO, "Connect " + socket + " died", e);
            }
        }

        private long readIndex(SocketChannel socket) throws IOException {
            ByteBuffer bb = ByteBuffer.allocate(8);
            while (bb.remaining() > 0 && socket.read(bb) > 0) ;
            if (bb.remaining() > 0) throw new EOFException();
            return bb.getLong(0);
        }
    }

    protected void pause(int delayNS) {
        if (delayNS < 1) return;
        if (delayNS < 20000)
            Thread.yield();
        else
            LockSupport.parkNanos(delayNS);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        service.shutdown();
        try {
            service.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        chronicle.close();
    }

}
