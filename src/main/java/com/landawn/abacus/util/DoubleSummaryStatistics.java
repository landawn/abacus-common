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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleConsumer;

/**
 *
 */
public class DoubleSummaryStatistics implements DoubleConsumer {

    private final KahanSummation summation = new KahanSummation();

    private double min = Double.POSITIVE_INFINITY;

    private double max = Double.NEGATIVE_INFINITY;

    public DoubleSummaryStatistics() {
    }

    public DoubleSummaryStatistics(long count, double sum, double min, double max) {
        summation.combine(count, sum);

        this.min = min;
        this.max = max;
    }

    /**
     *
     * @param value
     */
    public void accept(float value) {
        summation.add(value);

        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    /**
     *
     * @param value
     */
    @Override
    public void accept(double value) {
        summation.add(value);

        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    /**
     *
     * @param other
     */
    public void combine(DoubleSummaryStatistics other) {
        summation.combine(other.summation);

        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
    }

    /**
     * Gets the min.
     *
     * @return
     */
    public final double getMin() {
        return min;
    }

    /**
     * Gets the max.
     *
     * @return
     */
    public final double getMax() {
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

    @Deprecated
    @Beta
    public final double sum() {
        return summation.sum();
    }

    @Deprecated
    @Beta
    public final OptionalDouble average() {
        return summation.average();
    }

    @Override
    public String toString() {
        return String.format("{min=%d, max=%d, count=%d, sum=%d, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
