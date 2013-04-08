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

package com.higherfrequencytrading.hiccup;

/**
 * @author peter.lawrey
 */
public class Histogram {
    private final int buckets, resolution;
    private final int count[];
    private int underflow = 0, overflow = 0;
    private long totalCount = 0;

    public Histogram(int buckets, int resolution) {
        this.buckets = buckets;
        this.resolution = resolution;
        count = new int[buckets];
    }

    public boolean sample(long sample) {
        totalCount++;
        long bucket = ((sample + resolution / 2) / resolution);
        if (sample < 0) {
            underflow++;
            return false;
        } else if (bucket >= buckets) {
            overflow++;
            return false;
        }
        count[((int) bucket)]++;
        return true;
    }

    public long percentile(double percentile) {
        long searchCount = (long) (totalCount * percentile / 100);
        // forward search is faster
        if (searchCount < totalCount * 3 / 4) {
            searchCount -= underflow;
            if (searchCount <= 0)
                return Long.MIN_VALUE;
            for (int i = 0; i < buckets; i++) {
                searchCount -= count[i];
                if (searchCount <= 0)
                    return (long) i * resolution;
            }
            return Long.MAX_VALUE;
        } else {
            searchCount = totalCount - searchCount;
            searchCount -= overflow;
            if (searchCount <= 0)
                return Long.MAX_VALUE;
            for (int i = buckets - 1; i >= 0; i--) {
                searchCount -= count[i];
                if (searchCount <= 0)
                    return (long) i * resolution;
            }
            return Long.MIN_VALUE;
        }
    }
}
