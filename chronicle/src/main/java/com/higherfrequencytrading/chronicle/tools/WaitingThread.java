package com.higherfrequencytrading.chronicle.tools;

import java.io.Closeable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author peter.lawrey
 */
public class WaitingThread implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(WaitingThread.class.getName());
    private final int delayMS;
    private final Set<WaitingRunnable> runnables = new LinkedHashSet<WaitingRunnable>();
    private volatile WaitingRunnable[] runnableArray = {};
    private final ExecutorService service;
    private volatile boolean closed = false;

    public WaitingThread(int delayMS, final String name, final boolean daemon) {
        this.delayMS = delayMS;
        service = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, name);
                t.setDaemon(daemon);
                return t;
            }
        });
        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    WaitingRunnable[] ra = runnableArray;
                    boolean busy = false;
                    for (WaitingRunnable waitingRunnable : ra) {
                        try {
                            busy |= waitingRunnable.run();
                        } catch (IllegalStateException e) {
                            remove(waitingRunnable);
                        } catch (Throwable t) {
                            LOGGER.log(Level.WARNING, "Task " + waitingRunnable + " failed, removing", t);
                        }
                    }
                    if (!busy)
                        pause();
                } catch (Throwable t) {
                    if (!closed)
                        LOGGER.log(Level.SEVERE, "WaitingThread died unexpectedly", t);
                }
            }
        });
    }

    protected void pause() throws InterruptedException {
        if (delayMS < 0) return;
        if (delayMS == 0)
            Thread.yield();
        else
            Thread.sleep(delayMS);
    }

    public void add(WaitingRunnable waitingRunnable) {
        synchronized (runnables) {
            if (runnables.add(waitingRunnable))
                runnableArray = runnables.toArray(new WaitingRunnable[runnables.size()]);
        }
    }

    public void remove(WaitingRunnable waitingRunnable) {
        synchronized (runnables) {
            if (runnables.remove(waitingRunnable))
                runnableArray = runnables.toArray(new WaitingRunnable[runnables.size()]);
        }
    }

    @Override
    public void close() {
        closed = true;
        service.shutdown();
    }
}
