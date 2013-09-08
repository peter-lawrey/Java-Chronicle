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
import com.higherfrequencytrading.chronicle.ExcerptMarshallable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * User: peter Date: 05/08/13 Time: 17:39
 */
public class Update implements ExcerptMarshallable {
    private String instrument;
    private final List<UpdateLevel> levels = new ArrayList<UpdateLevel>();
    private final List<UpdateLevel> levelPool = new ArrayList<UpdateLevel>();

    @Override
    public void readMarshallable(@NotNull Excerpt in) throws IllegalStateException {
        resetLevels(in.readEnum(String.class));
        int len = (int) in.readStopBit();
        for (int i = 0; i < len; i++)
            acquireLevel().readMarshallable(in);
    }

    public void resetLevels(String instrument) {
        this.instrument = instrument;
        levels.clear();
    }

    public UpdateLevel acquireLevel() {
        if (levels.size() == levelPool.size())
            levelPool.add(new UpdateLevel());
        UpdateLevel level = levelPool.get(levels.size());
        levels.add(level);
        return level;
    }

    @Override
    public void writeMarshallable(@NotNull Excerpt out) {
        out.writeEnum(instrument);
        out.writeStopBit(levels.size());
        for (int i = 0, len = levels.size(); i < len; i++)
            levels.get(i).writeMarshallable(out);
    }
}
