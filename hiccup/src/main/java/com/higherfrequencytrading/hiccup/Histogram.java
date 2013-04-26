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

import java.util.Arrays;

/**
 * @author peter.lawrey
 */
public class Histogram {
    private final int buckets;
    private final int resolution;
    private final int ordersOfMagnitude;
    private final int count[][];
    private int underflow = 0, overflow = 0;
    private long totalCount = 0;
    private long maximum = 0;

    public Histogram(int buckets, int resolution, int ordersOfMagnitude) {
        this.buckets = buckets;
        this.resolution = resolution;
        this.ordersOfMagnitude = ordersOfMagnitude;
        count = new int[ordersOfMagnitude][buckets];
    }

    public static Histogram add(Histogram... histograms) {
        int buckets = histograms[0].buckets;
        int resolution = histograms[0].resolution;
        int ordersOfMagnitude = histograms[0].ordersOfMagnitude;
        for (Histogram histogram : histograms) {
            assert buckets == histogram.buckets;
            assert resolution == histogram.resolution;
            assert ordersOfMagnitude == histogram.ordersOfMagnitude;
        }
        Histogram ret = new Histogram(buckets, resolution, ordersOfMagnitude);
        for (Histogram histogram : histograms) {
            for (int i = 0; i < ordersOfMagnitude; i++) {
                for (int j = 0; j < buckets; j++)
                    ret.count[i][j] += histogram.count[i][j];
            }
            ret.overflow += histogram.overflow;
            ret.underflow += histogram.underflow;
            ret.totalCount += histogram.totalCount;
        }
        long total2 = ret.underflow + ret.overflow;
        for (int i = 0; i < ordersOfMagnitude; i++)
            for (int j = 0; j < buckets; j++)
                total2 += ret.count[i][j];
        if (ret.totalCount != total2)
            throw new AssertionError(ret.totalCount + " != " + total2);
        return ret;
    }

    public boolean sample(long sample) {
        totalCount++;
        if (sample < 0) {
            underflow++;
            return false;
        }
        if (sample > maximum)
            maximum = sample;
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
        return true;
    }

    public long percentile(double percentile) {
        long searchCount = (long) (totalCount * percentile / 100);
        searchCount = totalCount - searchCount;
        if (percentile == 100)
            searchCount = 1;
        searchCount -= overflow;
        if (searchCount <= 0)
            return maximum;

        int order;
        for (order = ordersOfMagnitude - 1; order >= 0; order--) {
            for (int i = buckets - 1; i >= 0; i--) {
                searchCount -= count[order][i];
                if (searchCount <= 0) {
                    long l = (long) i * resolution;
                    for (int j = 0; j < order; j++)
                        l *= 10;
                    return l;
                }
            }
        }
        return Long.MIN_VALUE;
    }

    public long count() {
        return totalCount;
    }

    public void clear() {
        totalCount = underflow = overflow = 0;
        for (int[] ints : count)
            Arrays.fill(ints, 0);
    }
}