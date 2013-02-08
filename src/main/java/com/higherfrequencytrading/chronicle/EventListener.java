package com.higherfrequencytrading.chronicle;

/**
 * @author peter.lawrey
 */
public interface EventListener {
    /**
     * @param eventId the index in the chronicle for correlation and restart purposes
     * @param name    the name of the collection updated
     */
    public void eventStart(long eventId, String name);

    /**
     * @param lastEvent false if there is definitely another event to process, true if it is not known if there is a waiting event.
     */
    public void eventEnd(boolean lastEvent);
}
