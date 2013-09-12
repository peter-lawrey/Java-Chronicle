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

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: peter Date: 05/08/13 Time: 17:34
 */
public class EventsWriter implements Events {
    private final Excerpt excerpt;
    @Nullable
    private final MetaData metaData = new MetaData(null, TimingStage.SourceWrite);

    public EventsWriter(@NotNull Chronicle chronicle) {
        excerpt = chronicle.createExcerpt();
    }

    @Override
    public void onMarketData(@Nullable MetaData metaData, @NotNull Update update) {
        if (metaData == null) {
            metaData = this.metaData;
            metaData.startTiming();
        }
        excerpt.startExcerpt(1024); // a guess
        excerpt.writeEnum(MessageType.update);
        metaData.writeMarshallable(excerpt);
        update.writeMarshallable(excerpt);
        excerpt.finish();
    }
}
