/*
 * Copyright (C) 2021 HaiYang Li
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

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Consumer;

/** 
 * 
 */
public class BigDecimalSummaryStatistics implements Consumer<BigDecimal> {

    private long count;

    private BigDecimal sum = BigDecimal.ZERO;

    private BigDecimal min = null;

    private BigDecimal max = null;

    public BigDecimalSummaryStatistics() {
    }

    public BigDecimalSummaryStatistics(long count, BigDecimal sum, BigDecimal min, BigDecimal max) {
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
    public void accept(BigDecimal value) {
        ++count;
        sum = sum.add(value);
        min = min == null ? value : min.compareTo(value) > 0 ? value : min;
        max = max == null ? value : max.compareTo(value) < 0 ? value : max;
    }

    /**
     *
     * @param other
     */
    public void combine(BigDecimalSummaryStatistics other) {
        count += other.count;
        sum = sum.add(other.sum);
        min = min == null ? other.min : min.compareTo(other.min) > 0 ? other.min : min;
        max = max == null ? other.max : max.compareTo(other.max) < 0 ? other.max : max;
    }

    /**
     * Gets the min.
     *
     * @return
     */
    public final BigDecimal getMin() {
        return min;
    }

    /**
     * Gets the max.
     *
     * @return
     */
    public final BigDecimal getMax() {
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
    public final BigDecimal getSum() {
        return sum;
    }

    /**
     * Gets the average.
     *
     * @return
     */
    public final BigDecimal getAverage() {
        return count == 0L ? BigDecimal.ZERO : getSum().divide(BigDecimal.valueOf(this.count), MathContext.DECIMAL128);
    }

    @Deprecated
    @Beta
    public final BigDecimal sum() {
        return sum;
    }

    @Deprecated
    @Beta
    public final Optional<BigDecimal> average() {
        if (count == 0) {
            return Optional.<BigDecimal> empty();
        }

        return Optional.of(getAverage());
    }

    static final DecimalFormat df = new DecimalFormat("#,###.000000");

    @Override
    public String toString() {
        return StringUtil.concat("{min=", min == null ? "null" : df.format(min), ", max=", max == null ? "null" : df.format(max), ", count=",
                String.valueOf(count), ", sum=", df.format(getSum()), ", average=", df.format(getAverage()), "}");
    }
}
