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

import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.ShortConsumer;

/** 
 * 
 */
public class ShortSummaryStatistics implements ShortConsumer {

    /** The count. */
    private long count;

    /** The sum. */
    private long sum;

    /** The min. */
    private short min = Short.MAX_VALUE;

    /** The max. */
    private short max = Short.MIN_VALUE;

    /**
     * Instantiates a new short summary statistics.
     */
    public ShortSummaryStatistics() {
    }

    /**
     * Instantiates a new short summary statistics.
     *
     * @param count
     * @param sum
     * @param min
     * @param max
     */
    public ShortSummaryStatistics(long count, long sum, short min, short max) {
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
    public void accept(short value) {
        ++count;
        sum += value;
        min = N.min(min, value);
        max = N.max(max, value);
    }

    /**
     *
     * @param other
     */
    public void combine(ShortSummaryStatistics other) {
        count += other.count;
        sum += other.sum;
        min = N.min(min, other.min);
        max = N.max(max, other.max);
    }

    /**
     * Gets the min.
     *
     * @return
     */
    public final short getMin() {
        return min;
    }

    /**
     * Gets the max.
     *
     * @return
     */
    public final short getMax() {
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

    /**
     *
     * @return
     */
    public final int sum() {
        return N.toIntExact(sum);
    }

    /**
     *
     * @return
     */
    public final OptionalDouble average() {
        if (count == 0) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(getAverage());
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return String.format("{min=%d, max=%d, count=%d, sum=%d, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
