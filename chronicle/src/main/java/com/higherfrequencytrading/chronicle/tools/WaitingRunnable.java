package com.higherfrequencytrading.chronicle.tools;

/**
 * @author peter.lawrey
 */
public interface WaitingRunnable {
    /**
     * @return did the task do something
     * @throws IllegalStateException when this runnable is finished
     */
    public boolean run() throws IllegalStateException;
}
