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
    private final int buckets;
    private final int resolution;
    private final int ordersOfMagnitude;
    private final int count[][];
    private final int orderCount[];
    private int underflow = 0, overflow = 0;
    private long totalCount = 0;

    public Histogram(int buckets, int resolution, int ordersOfMagnitude) {
        this.buckets = buckets;
        this.resolution = resolution;
        this.ordersOfMagnitude = ordersOfMagnitude;
        count = new int[ordersOfMagnitude][buckets];
        orderCount = new int[ordersOfMagnitude];
    }

    public boolean sample(long sample) {
        totalCount++;
        if (sample < 0) {
            underflow++;
            return false;
        }
        long bucket = ((sample + resolution / 2) / resolution);
        int order = 0;
        while (bucket >= buckets && order < ordersOfMagnitude - 1) {
            bucket /= 10;
            order++;
        }
        if (bucket >= buckets) {
            overflow++;
            return false;
        }
        count[order][((int) bucket)]++;
        orderCount[order]++;
        return true;
    }

    public long percentile(double percentile) {
        long searchCount = (long) (totalCount * percentile / 100);
        searchCount = totalCount - searchCount;
        searchCount -= overflow;

        int order;
        for (order = ordersOfMagnitude - 1; order >= 0; order--) {
            if (searchCount < orderCount[order]) {
                break;
            }
            searchCount -= orderCount[order];
        }
        if (searchCount <= 0)
            return Long.MAX_VALUE;
        for (int i = buckets - 1; i >= 0; i--) {
            searchCount -= count[order][i];
            if (searchCount <= 0) {
                long l = (long) i * resolution;
                for (int j = 0; j < order; j++)
                    l *= 10;
                return l;
            }
        }
        return Long.MIN_VALUE;
    }
}
