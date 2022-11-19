/*
 * Copyright (C) 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util;

import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.LongConsumer;

/**
 *
 */
public class LongSummaryStatistics implements LongConsumer, IntConsumer {

    private long count;

    private long sum;

    private long min = Long.MAX_VALUE;

    private long max = Long.MIN_VALUE;

    public LongSummaryStatistics() {
    }

    public LongSummaryStatistics(long count, long min, long max, long sum) {
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    /**
     *
     * @param value
     */
    @Override
    public void accept(int value) {
        accept((long) value);
    }

    /**
     *
     * @param value
     */
    @Override
    public void accept(long value) {
        ++count;
        sum += value;
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    /**
     *
     * @param other
     */
    public void combine(LongSummaryStatistics other) {
        count += other.count;
        sum += other.sum;
        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
    }

    /**
     * Gets the min.
     *
     * @return
     */
    public final long getMin() {
        return min;
    }

    /**
     * Gets the max.
     *
     * @return
     */
    public final long getMax() {
        return max;
    }

    /**
     * Gets the count.
     *
     * @return
     */
    public final long getCount() {
        return count;
    }

    /**
     * Gets the sum.
     *
     * @return
     */
    public final Long getSum() {
        return sum;
    }

    /**
     * Gets the average.
     *
     * @return
     */
    public final Double getAverage() {
        return getCount() > 0 ? (double) getSum() / getCount() : 0.0d;
    }

    //    @Deprecated
    //    @Beta
    //    public final long sum() {
    //        return sum;
    //    }
    //
    //    @Deprecated
    //    @Beta
    //    public final OptionalDouble average() {
    //        if (count == 0) {
    //            return OptionalDouble.empty();
    //        }
    //
    //        return OptionalDouble.of(getAverage());
    //    }

    @Override
    public String toString() {
        return String.format("{min=%d, max=%d, count=%d, sum=%d, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
