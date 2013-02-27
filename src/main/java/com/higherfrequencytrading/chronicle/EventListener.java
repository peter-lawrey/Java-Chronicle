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

    /**
     * An arbitary event was published for listeners by Wrapper.publishEvent()
     *
     * @param object published.
     */
    public void onEvent(Object object);
}
