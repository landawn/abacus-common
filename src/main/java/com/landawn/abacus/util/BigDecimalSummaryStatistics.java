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
import java.math.MathContext;
import java.text.DecimalFormat;

import com.landawn.abacus.util.function.Consumer;

/**
 * A state object for collecting statistics such as count, min, max, sum, and average
 * for a collection of BigDecimal values.
 * 
 * <p>This class is designed to work with Java 8 streams and can be used as a
 * reduction target for a stream of BigDecimal values. It maintains precision
 * by using BigDecimal for all calculations.</p>
 * 
 * <p>This implementation is not thread-safe. However, it is safe to use
 * a BigDecimalSummaryStatistics instance concurrently from multiple threads
 * if it is not being modified.</p>
 * 
 * <p><b>Usage Examples with streams:</b></p>
 * <pre>{@code
 * List<BigDecimal> amounts = Arrays.asList(
 *     new BigDecimal("10.50"),
 *     new BigDecimal("20.75"),
 *     new BigDecimal("15.25")
 * );
 * 
 * BigDecimalSummaryStatistics stats = amounts.stream()
 *     .collect(BigDecimalSummaryStatistics::new,
 *              BigDecimalSummaryStatistics::accept,
 *              BigDecimalSummaryStatistics::combine);
 * 
 * System.out.println("Count: " + stats.getCount());
 * System.out.println("Sum: " + stats.getSum());
 * System.out.println("Average: " + stats.getAverage());
 * }</pre>
 * 
 * @see java.util.DoubleSummaryStatistics
 * @see java.util.IntSummaryStatistics
 */
public class BigDecimalSummaryStatistics implements Consumer<BigDecimal> {

    private long count;

    private BigDecimal sum = BigDecimal.ZERO;

    private BigDecimal min = null;

    private BigDecimal max = null;

    /**
     * Constructs an empty instance with zero count, zero sum,
     * and undefined min/max values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
     * stats.accept(new BigDecimal("100.00"));
     * }</pre>
     */
    public BigDecimalSummaryStatistics() {
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
     * BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics(
     *     3L,
     *     new BigDecimal("10.00"),
     *     new BigDecimal("30.00"),
     *     new BigDecimal("60.00")
     * );
     * }</pre>
     *
     * @param count the count of values
     * @param min the minimum value
     * @param max the maximum value
     * @param sum the sum of values
     */
    public BigDecimalSummaryStatistics(final long count, final BigDecimal min, final BigDecimal max, final BigDecimal sum) {
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    /**
     * Records a new BigDecimal value into the summary information.
     * 
     * <p>This method updates the count, sum, min, and max values based on
     * the provided value. Null values should not be passed to this method.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
     * stats.accept(new BigDecimal("25.50"));
     * stats.accept(new BigDecimal("30.75"));
     * }</pre>
     *
     * @param value the input value to be recorded
     */
    @Override
    public void accept(final BigDecimal value) {
        ++count;
        sum = sum.add(value);
        min = min == null ? value : min.compareTo(value) > 0 ? value : min;
        max = max == null ? value : max.compareTo(value) < 0 ? value : max;
    }

    /**
     * Combines the state of another BigDecimalSummaryStatistics into this one.
     * 
     * <p>This method is useful for parallel processing where multiple
     * summary statistics objects are created for different partitions
     * and need to be combined into a single result.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigDecimalSummaryStatistics stats1 = new BigDecimalSummaryStatistics();
     * stats1.accept(new BigDecimal("10"));
     * stats1.accept(new BigDecimal("20"));
     * 
     * BigDecimalSummaryStatistics stats2 = new BigDecimalSummaryStatistics();
     * stats2.accept(new BigDecimal("30"));
     * stats2.accept(new BigDecimal("40"));
     * 
     * stats1.combine(stats2);
     * // stats1 now contains combined statistics of all four values
     * }</pre>
     *
     * @param other another BigDecimalSummaryStatistics to be combined with this one
     */
    public void combine(final BigDecimalSummaryStatistics other) {
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
     * BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
     * stats.accept(new BigDecimal("10.50"));
     * stats.accept(new BigDecimal("5.25"));
     * BigDecimal min = stats.getMin();   // Returns 5.25
     * }</pre>
     *
     * @return the minimum value, or {@code null} if none
     */
    public final BigDecimal getMin() {
        return min;
    }

    /**
     * Returns the maximum value recorded, or {@code null} if no values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
     * stats.accept(new BigDecimal("10.50"));
     * stats.accept(new BigDecimal("25.75"));
     * BigDecimal max = stats.getMax();   // Returns 25.75
     * }</pre>
     *
     * @return the maximum value, or {@code null} if none
     */
    public final BigDecimal getMax() {
        return max;
    }

    /**
     * Returns the count of values recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
     * stats.accept(new BigDecimal("10"));
     * stats.accept(new BigDecimal("20"));
     * long count = stats.getCount();   // Returns 2
     * }</pre>
     *
     * @return the count of values
     */
    public final long getCount() {
        return count;
    }

    /**
     * Returns the sum of values recorded, or zero if no values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
     * stats.accept(new BigDecimal("10.50"));
     * stats.accept(new BigDecimal("20.50"));
     * BigDecimal sum = stats.getSum();   // Returns 31.00
     * }</pre>
     *
     * @return the sum of values, or zero if none
     */
    public final BigDecimal getSum() {
        return sum;
    }

    /**
     * Returns the arithmetic mean of values recorded, or zero if no values have been recorded.
     * 
     * <p>The average is calculated using MathContext.DECIMAL128 for precision.
     * If no values have been recorded, this method returns BigDecimal.ZERO.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
     * stats.accept(new BigDecimal("10.00"));
     * stats.accept(new BigDecimal("20.00"));
     * stats.accept(new BigDecimal("30.00"));
     * BigDecimal avg = stats.getAverage();   // Returns 20.00
     * }</pre>
     *
     * @return the arithmetic mean of values, or zero if none
     */
    public final BigDecimal getAverage() {
        return count == 0L ? BigDecimal.ZERO : getSum().divide(BigDecimal.valueOf(count), MathContext.DECIMAL128);
    }

    static final DecimalFormat df = new DecimalFormat("#,###.000000");

    /**
     * Returns a string representation of this summary statistics object.
     * 
     * <p>The string representation includes min, max, count, sum, and average
     * values formatted with appropriate decimal places. Values are formatted
     * using a DecimalFormat with pattern "#,###.000000".</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * {min=5.250000, max=25.750000, count=3, sum=46.500000, average=15.500000}
     * }</pre>
     *
     * @return a string representation of this summary statistics
     */
    @Override
    public String toString() {
        return Strings.concat("{min=", min == null ? "null" : df.format(min), ", max=", max == null ? "null" : df.format(max), ", count=",
                String.valueOf(count), ", sum=", df.format(getSum()), ", average=", df.format(getAverage()), "}");
    }
}
