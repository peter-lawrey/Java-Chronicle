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

package vanilla.java.stages.api;

import com.higherfrequencytrading.chronicle.Excerpt;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:34
 */
public class EventsReader {
    private final Excerpt excerpt;
    private final Events events;
    private final MetaData metaData;
    private final Update update = new Update();

    public EventsReader(Excerpt excerpt, Events events, TimingStage readStage, TimingStage writeStage) {
        this.excerpt = excerpt;
        this.events = events;
        metaData = new MetaData(readStage, writeStage);
    }

    public boolean read() {
        if (!excerpt.nextIndex())
            return false;
        MessageType mt = excerpt.readEnum(MessageType.class);
        metaData.readMarshallable(excerpt);
        switch (mt) {
            case update: {
                update.readMarshallable(excerpt);
                events.onMarketData(metaData, update);
                break;
            }
        }
        return true;
    }
}
