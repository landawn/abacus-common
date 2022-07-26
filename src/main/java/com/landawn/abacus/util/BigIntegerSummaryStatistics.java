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
import java.math.BigInteger;
import java.math.MathContext;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Consumer;

/**
 *
 */
public class BigIntegerSummaryStatistics implements Consumer<BigInteger> {

    private long count;

    private BigInteger sum = BigInteger.ZERO;

    private BigInteger min = null;

    private BigInteger max = null;

    public BigIntegerSummaryStatistics() {
    }

    public BigIntegerSummaryStatistics(long count, BigInteger min, BigInteger max, BigInteger sum) {
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
    public void accept(BigInteger value) {
        ++count;
        sum = sum.add(value);
        min = min == null ? value : min.compareTo(value) > 0 ? value : min;
        max = max == null ? value : max.compareTo(value) < 0 ? value : max;
    }

    /**
     *
     * @param other
     */
    public void combine(BigIntegerSummaryStatistics other) {
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
    public final BigInteger getMin() {
        return min;
    }

    /**
     * Gets the max.
     *
     * @return
     */
    public final BigInteger getMax() {
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
    public final BigInteger getSum() {
        return sum;
    }

    /**
     * Gets the average.
     *
     * @return
     */
    public final BigDecimal getAverage() {
        return count == 0L ? BigDecimal.ZERO : new BigDecimal(getSum()).divide(BigDecimal.valueOf(this.count), MathContext.DECIMAL128);
    }

    @Deprecated
    @Beta
    public final BigInteger sum() {
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

    @Override
    public String toString() {
        return String.format("{min=%d, max=%d, count=%d, sum=%d, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
