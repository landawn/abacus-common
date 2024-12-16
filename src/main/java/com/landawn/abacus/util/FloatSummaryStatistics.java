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

import com.landawn.abacus.util.function.FloatConsumer;

public class FloatSummaryStatistics implements FloatConsumer {

    private final KahanSummation summation = new KahanSummation();

    private float min = Float.POSITIVE_INFINITY;

    private float max = Float.NEGATIVE_INFINITY;

    public FloatSummaryStatistics() {
    }

    /**
     *
     * @param count
     * @param min
     * @param max
     * @param sum
     */
    public FloatSummaryStatistics(final long count, final float min, final float max, final double sum) {
        summation.combine(count, sum);

        this.min = min;
        this.max = max;
    }

    /**
     *
     * @param value
     */
    @Override
    public void accept(final float value) {
        summation.add(value);

        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    /**
     *
     * @param other
     */
    public void combine(final FloatSummaryStatistics other) {
        summation.combine(other.summation);

        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
    }

    /**
     * Gets the min.
     *
     * @return
     */
    public final float getMin() {
        return min;
    }

    /**
     * Gets the max.
     *
     * @return
     */
    public final float getMax() {
        return max;
    }

    /**
     * Gets the count.
     *
     * @return
     */
    public final long getCount() {
        return summation.count();
    }

    /**
     * Gets the sum.
     *
     * @return
     */
    public final Double getSum() {
        return summation.sum();
    }

    /**
     * Gets the average.
     *
     * @return
     */
    public final Double getAverage() {
        return summation.average().orElse(0d);
    }

    //    @Deprecated
    //    @Beta
    //    public final float sum() {
    //        return (float) summation.sum();
    //    }
    //
    //    @Deprecated
    //    @Beta
    //    public final OptionalDouble average() {
    //        return summation.average();
    //    }

    @Override
    public String toString() {
        return String.format("{min=%f, max=%f, count=%d, sum=%f, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
