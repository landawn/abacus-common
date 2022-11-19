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

/**
 *
 */
public class IntSummaryStatistics implements IntConsumer {

    private long count;

    private long sum;

    private int min = Integer.MAX_VALUE;

    private int max = Integer.MIN_VALUE;

    public IntSummaryStatistics() {
    }

    public IntSummaryStatistics(long count, int min, int max, long sum) {
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
        ++count;
        sum += value;
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    /**
     *
     * @param other
     */
    public void combine(IntSummaryStatistics other) {
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
    public final int getMin() {
        return min;
    }

    /**
     * Gets the max.
     *
     * @return
     */
    public final int getMax() {
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
    //    public final int sum() {
    //        return Numbers.toIntExact(sum);
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
