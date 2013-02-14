package com.higherfrequencytrading.chronicle.tcp.gw;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.WaitingRunnable;
import com.higherfrequencytrading.chronicle.tools.WaitingThread;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author peter.lawrey
 */
public class SocketGateway implements WaitingRunnable, Closeable {
    /*
        struct GatewayEntry {
            long writeTimeMS, writeTimeNS, readTimeNS;
            byte[] bytes;
        }
    */
    private final InetSocketAddress address;
    private final Chronicle outbound;
    private final Chronicle inbound;

    private final GatewayEntryReader outboundReader;
    private final GatewayEntryWriter inboundWriter;
    private SocketChannel socket;

    private volatile boolean closed = false;

    public SocketGateway(final InetSocketAddress address, Chronicle outbound, Chronicle inbound, WaitingThread waitingThread) {
        this.address = address;
        this.outbound = outbound;
        this.inbound = inbound;

        Excerpt out = outbound.createExcerpt();
        out.index(out.size());
        outboundReader = new GatewayEntryReader(out, true) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 << 20);

            @Override
            protected void onEntry(long writeTimeNS, long writeTimeMS, long readTimeMS, int length, char type, Excerpt excerpt) {
                if (type == 'X') return;

                byteBuffer.position(0);
                byteBuffer.limit(length);
                excerpt.read(byteBuffer);
                byteBuffer.flip();
                try {
                    while (socket.write(byteBuffer) > 0 && byteBuffer.remaining() > 0) ;
                } catch (IOException e) {
                    inboundWriter.onException("Failed to write", e);
                }
            }
        };
        Excerpt in = inbound.createExcerpt();
        in.index(in.size());
        inboundWriter = new GatewayEntryWriter(in);

        waitingThread.add(this);
    }

    enum State {
        PAUSING, CONNECTING, WAITING_FOR_CONNECTION, PROCESSING
    }

    private volatile State state = State.PAUSING;
    private long pauseTimeout = System.currentTimeMillis() + 100;
    private final ByteBuffer bb = ByteBuffer.allocateDirect(1 << 20);

    @Override
    public boolean run() throws IllegalStateException {
        if (closed) {
            closeAll();
            throw new IllegalStateException("closed");
        }
        try {
            switch (state) {
                case PAUSING:
                    if (System.currentTimeMillis() <= pauseTimeout)
                        state = State.CONNECTING;
                    break;

                case CONNECTING:
                    socket = SocketChannel.open();
                    socket.configureBlocking(false);
                    socket.socket().connect(address);
                    state = State.WAITING_FOR_CONNECTION;
                    break;

                case WAITING_FOR_CONNECTION:
                    if (socket.finishConnect())
                        state = State.PROCESSING;
                    break;

                case PROCESSING:
                    bb.clear();
                    if (socket.read(bb) < 0)
                        return outboundReader.readEntry();
                    bb.flip();
                    Excerpt excerpt = inboundWriter.startExceprt(bb.remaining(), 'I');
                    excerpt.write(bb);
                    excerpt.finish();
                    break;
            }
        } catch (IOException e) {
            inboundWriter.onException("Failed while " + state, e);
            state = State.PAUSING;
            pauseTimeout = System.currentTimeMillis() + 5000;
        }
        return false;
    }

    void closeAll() {
        closeSocket();
        outbound.close();
        inbound.close();
    }

    private void closeSocket() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        socket = null;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    public static void main(String... args) throws IOException {
        String outboundPath = args[0];
        String inboundPath = args[1];
        String hostname = args[2];
        int port = Integer.parseInt(args[3]);
        WaitingThread thread = new WaitingThread(1, "SocketGateway " + hostname + ":" + port, false);
        new SocketGateway(new InetSocketAddress(hostname, port),
                new IndexedChronicle(outboundPath), new IndexedChronicle(inboundPath), thread);
    }
}
