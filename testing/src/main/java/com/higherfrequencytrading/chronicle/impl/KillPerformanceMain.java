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
package com.higherfrequencytrading.chronicle.impl;

/**
 * Used to see what the impact of a busy machine has on performance.
 * <p/>
 * e.g.
 * Running KillPerformanceMain and BaseIndexedChronicleLatencyMain
 * <p/>
 * The average RTT latency was 189 ns. The 50/99 / 99.9/99.99%tile latencies were 180/220 / 2,820/4,280. There were 2 delays over 100 μs - without this running
 * The average RTT latency was 211 ns. The 50/99 / 99.9/99.99%tile latencies were 185/223 / 4,359/9,122. There were 214 delays over 100 μs - with this running
 * <p/>
 * Running KillPerformanceMain and IndexedChronicleLatencyMain (which uses thread affinity)
 * The average RTT latency was 189 ns. The 50/99 / 99.9/99.99%tile latencies were 177/200 / 3,137/4,943. There were 2 delays over 100 μs - without this running.
 * The average RTT latency was 192 ns. The 50/99 / 99.9/99.99%tile latencies were 180/210 / 3,136/4,845. There were 3 delays over 100 μs - with this running.
 *
 * @author peter.lawrey
 */
public class KillPerformanceMain {
    static volatile Long number;

    public static void main(String... args) {
        while (number >= 0)
            number++;
    }
}
