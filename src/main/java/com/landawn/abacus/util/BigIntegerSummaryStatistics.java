/*
 * Copyright (C) 2021 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

import com.landawn.abacus.util.function.Consumer;

/**
 * A state object for collecting statistics such as count, min, max, sum, and average
 * for a collection of BigInteger values.
 *
 * <p>This class is designed to work with Java 8 streams and can be used as a
 * reduction target for a stream of BigInteger values. It maintains arbitrary
 * precision for all calculations by using BigInteger for sum, min, and max values.</p>
 *
 * <p>This implementation is not thread-safe. External synchronization is required
 * if instances are accessed from multiple threads.</p>
 *
 * <p><b>Usage Examples with streams:</b></p>
 * <pre>{@code
 * List<BigInteger> numbers = Arrays.asList(
 *     new BigInteger("1000000000000000000"),
 *     new BigInteger("2000000000000000000"),
 *     new BigInteger("3000000000000000000")
 * );
 *
 * BigIntegerSummaryStatistics stats = numbers.stream()
 *     .collect(BigIntegerSummaryStatistics::new,
 *              BigIntegerSummaryStatistics::accept,
 *              BigIntegerSummaryStatistics::combine);
 *
 * System.out.println("Count: " + stats.getCount());
 * System.out.println("Sum: " + stats.getSum());
 * System.out.println("Average: " + stats.getAverage());
 * }</pre>
 *
 * @see BigDecimalSummaryStatistics
 * @see java.util.IntSummaryStatistics
 * @see java.util.LongSummaryStatistics
 */
public class BigIntegerSummaryStatistics implements Consumer<BigInteger> {

    private long count;

    private BigInteger sum = BigInteger.ZERO;

    private BigInteger min = null;

    private BigInteger max = null;

    /**
     * Constructs an empty instance with zero count, zero sum,
     * and undefined min/max values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
     * stats.accept(new BigInteger("12345678901234567890"));
     * }</pre>
     */
    public BigIntegerSummaryStatistics() {
    }

    /**
     * Constructs an instance with the specified count, min, max, and sum.
     *
     * <p>This constructor is useful for creating a summary statistics object
     * from pre-calculated values, such as when deserializing or combining
     * results from multiple sources.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics(
     *     3L,
     *     new BigInteger("10"),
     *     new BigInteger("30"),
     *     new BigInteger("60")
     * );
     * }</pre>
     *
     * @param count the count of values; must be non-negative
     * @param min the minimum value, may be {@code null} if no values were recorded
     * @param max the maximum value, may be {@code null} if no values were recorded
     * @param sum the sum of values; if {@code null}, it is treated as {@link BigInteger#ZERO}
     * @throws IllegalArgumentException if {@code count} is negative, or if {@code min} is greater than {@code max}
     */
    public BigIntegerSummaryStatistics(final long count, final BigInteger min, final BigInteger max, final BigInteger sum) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        if (N.compare(min, max) > 0) {
            throw new IllegalArgumentException("minimum is greater than maximum");
        }

        this.count = count;
        this.sum = sum == null ? BigInteger.ZERO : sum;
        this.min = min;
        this.max = max;
    }

    /**
     * Records a new BigInteger value into the summary information.
     *
     * <p>This method updates the count, sum, min, and max values based on
     * the provided value. Null values should not be passed to this method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
     * stats.accept(new BigInteger("100"));
     * stats.accept(new BigInteger("200"));
     * stats.accept(new BigInteger("300"));
     * }</pre>
     *
     * @param value the input value to be recorded, must not be {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public void accept(final BigInteger value) {
        ++count;
        sum = sum.add(value);
        min = min == null ? value : min.compareTo(value) > 0 ? value : min;
        max = max == null ? value : max.compareTo(value) < 0 ? value : max;
    }

    /**
     * Combines the state of another BigIntegerSummaryStatistics into this one.
     *
     * <p>This method is useful for parallel processing where multiple
     * summary statistics objects are created for different partitions
     * and need to be combined into a single result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats1 = new BigIntegerSummaryStatistics();
     * stats1.accept(new BigInteger("1000"));
     * stats1.accept(new BigInteger("2000"));
     *
     * BigIntegerSummaryStatistics stats2 = new BigIntegerSummaryStatistics();
     * stats2.accept(new BigInteger("3000"));
     * stats2.accept(new BigInteger("4000"));
     *
     * stats1.combine(stats2);
     * // stats1 now contains combined statistics of all four values
     * }</pre>
     *
     * @param other another BigIntegerSummaryStatistics to be combined with this one
     */
    public void combine(final BigIntegerSummaryStatistics other) {
        count += other.count;
        sum = sum.add(other.sum);
        min = N.min(min, other.min);
        max = N.max(max, other.max);
    }

    /**
     * Returns the minimum value recorded, or {@code null} if no values have been recorded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
     * stats.accept(new BigInteger("100"));
     * stats.accept(new BigInteger("50"));
     * BigInteger min = stats.getMin();   // Returns 50
     * }</pre>
     *
     * @return the minimum value, or {@code null} if none
     */
    public final BigInteger getMin() {
        return min;
    }

    /**
     * Returns the maximum value recorded, or {@code null} if no values have been recorded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
     * stats.accept(new BigInteger("100"));
     * stats.accept(new BigInteger("500"));
     * BigInteger max = stats.getMax();   // Returns 500
     * }</pre>
     *
     * @return the maximum value, or {@code null} if none
     */
    public final BigInteger getMax() {
        return max;
    }

    /**
     * Returns the count of values recorded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
     * stats.accept(new BigInteger("10"));
     * stats.accept(new BigInteger("20"));
     * long count = stats.getCount();   // Returns 2
     * }</pre>
     *
     * @return the count of values
     */
    public final long getCount() {
        return count;
    }

    /**
     * Returns the sum of values recorded, or {@link BigInteger#ZERO} if no values have been recorded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
     * stats.accept(new BigInteger("1000"));
     * stats.accept(new BigInteger("2000"));
     * BigInteger sum = stats.getSum();   // Returns 3000
     * }</pre>
     *
     * @return the sum of values, or {@link BigInteger#ZERO} if none
     */
    public final BigInteger getSum() {
        return sum;
    }

    /**
     * Returns the arithmetic mean of values recorded as a {@link BigDecimal},
     * or {@link BigDecimal#ZERO} if no values have been recorded.
     *
     * <p>The average is calculated by converting the {@link BigInteger} sum to a
     * {@link BigDecimal} and dividing by the count using {@link java.math.MathContext#DECIMAL128}
     * for 34-digit decimal precision.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
     * stats.accept(new BigInteger("10"));
     * stats.accept(new BigInteger("20"));
     * stats.accept(new BigInteger("30"));
     * BigDecimal avg = stats.getAverage();   // Returns 2E+1 (i.e., 20 with DECIMAL128 precision)
     * }</pre>
     *
     * @return the arithmetic mean of values as a {@link BigDecimal} with {@link java.math.MathContext#DECIMAL128} precision,
     *         or {@link BigDecimal#ZERO} if no values have been recorded
     */
    public final BigDecimal getAverage() {
        return count == 0L ? BigDecimal.ZERO : new BigDecimal(getSum()).divide(BigDecimal.valueOf(count), MathContext.DECIMAL128);
    }

    /**
     * Returns a string representation of this summary statistics object.
     *
     * <p>The representation uses the format
     * {@code {min=<min>, max=<max>, count=<count>, sum=<sum>, average=<average>}},
     * where each field is formatted via its {@code toString()} method.
     * The average is a {@link BigDecimal} computed with {@link java.math.MathContext#DECIMAL128}
     * precision and may therefore contain many decimal digits.</p>
     *
     * <p>Example output:</p>
     * <pre>{@code
     * {min=10, max=30, count=3, sum=60, average=20.00000000000000000000000000000000}
     * }</pre>
     *
     * @return a string representation of this summary statistics
     */
    @Override
    public String toString() {
        return String.format("{min=%s, max=%s, count=%d, sum=%s, average=%s}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
