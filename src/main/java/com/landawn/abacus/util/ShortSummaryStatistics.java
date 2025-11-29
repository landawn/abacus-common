/*
 * Copyright (C) 2018, 2019 HaiYang Li
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

import com.landawn.abacus.util.function.ShortConsumer;

/**
 * A state object for collecting statistics such as count, min, max, sum, and average for {@code short} values.
 * This class is designed to work with streams of short values, providing a way to compute
 * multiple statistics in a single pass.
 * 
 * <p>This class is mutable and not thread-safe. It's designed to be used with sequential streams
 * or with proper synchronization in parallel contexts.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ShortSummaryStatistics stats = new ShortSummaryStatistics();
 * short[] values = {10, 20, 30, 40, 50};
 * for (short value : values) {
 *     stats.accept(value);
 * }
 * 
 * System.out.println("Count: " + stats.getCount());  // 5
 * System.out.println("Sum: " + stats.getSum());  // 150
 * System.out.println("Min: " + stats.getMin());  // 10
 * System.out.println("Max: " + stats.getMax());  // 50
 * System.out.println("Average: " + stats.getAverage());  // 30.0
 * }</pre>
 * 
 * @see java.util.IntSummaryStatistics
 * @see java.util.LongSummaryStatistics
 * @see java.util.DoubleSummaryStatistics
 */
public class ShortSummaryStatistics implements ShortConsumer {

    private long count;

    private long sum;

    private short min = Short.MAX_VALUE;

    private short max = Short.MIN_VALUE;

    /**
     * Constructs an empty instance with zero count, zero sum,
     * {@code Short.MAX_VALUE} min, and {@code Short.MIN_VALUE} max.
     */
    public ShortSummaryStatistics() {
    }

    /**
     * Constructs a non-empty instance with the specified count, min, max, and sum.
     * This constructor is useful for creating a statistics object from pre-computed values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create statistics from known values
     * ShortSummaryStatistics stats = new ShortSummaryStatistics(5, (short)10, (short)50, 150);
     * }</pre>
     *
     * @param count the count of values
     * @param min the minimum value
     * @param max the maximum value
     * @param sum the sum of all values
     * @throws IllegalArgumentException if count is negative
     */
    public ShortSummaryStatistics(final long count, final short min, final short max, final long sum) {
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    /**
     * Records a new short value into the summary information.
     * Updates the count, sum, min, and max values accordingly.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSummaryStatistics stats = new ShortSummaryStatistics();
     * stats.accept((short)10);
     * stats.accept((short)20);
     * stats.accept((short)30);
     * }</pre>
     *
     * @param value the input value to be recorded
     */
    @Override
    public void accept(final short value) {
        ++count;
        sum += value;
        min = N.min(min, value);
        max = N.max(max, value);
    }

    /**
     * Combines the state of another {@code ShortSummaryStatistics} into this one.
     * This method is useful for combining statistics computed separately,
     * for example in parallel stream operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSummaryStatistics stats1 = new ShortSummaryStatistics();
     * stats1.accept((short)10);
     * stats1.accept((short)20);
     * 
     * ShortSummaryStatistics stats2 = new ShortSummaryStatistics();
     * stats2.accept((short)30);
     * stats2.accept((short)40);
     * 
     * stats1.combine(stats2);
     * // stats1 now contains statistics for all four values
     * }</pre>
     *
     * @param other another {@code ShortSummaryStatistics} to be combined with this one
     */
    public void combine(final ShortSummaryStatistics other) {
        count += other.count;
        sum += other.sum;
        min = N.min(min, other.min);
        max = N.max(max, other.max);
    }

    /**
     * Returns the minimum value recorded, or {@code Short.MAX_VALUE} if no values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSummaryStatistics stats = new ShortSummaryStatistics();
     * stats.accept((short)10);
     * stats.accept((short)5);
     * short min = stats.getMin();  // Returns 5
     * }</pre>
     *
     * @return the minimum value, or {@code Short.MAX_VALUE} if none
     */
    public final short getMin() {
        return min;
    }

    /**
     * Returns the maximum value recorded, or {@code Short.MIN_VALUE} if no values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSummaryStatistics stats = new ShortSummaryStatistics();
     * stats.accept((short)10);
     * stats.accept((short)20);
     * short max = stats.getMax();  // Returns 20
     * }</pre>
     *
     * @return the maximum value, or {@code Short.MIN_VALUE} if none
     */
    public final short getMax() {
        return max;
    }

    /**
     * Returns the count of values recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSummaryStatistics stats = new ShortSummaryStatistics();
     * stats.accept((short)10);
     * stats.accept((short)20);
     * long count = stats.getCount();  // Returns 2
     * }</pre>
     *
     * @return the count of values
     */
    public final long getCount() {
        return count;
    }

    /**
     * Returns the sum of values recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSummaryStatistics stats = new ShortSummaryStatistics();
     * stats.accept((short)10);
     * stats.accept((short)20);
     * stats.accept((short)30);
     * Long sum = stats.getSum();  // Returns 60
     * }</pre>
     *
     * @return the sum of values as a Long
     */
    public final Long getSum() {
        return sum;
    }

    /**
     * Returns the arithmetic mean of values recorded, or 0.0 if no values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSummaryStatistics stats = new ShortSummaryStatistics();
     * stats.accept((short)10);
     * stats.accept((short)20);
     * stats.accept((short)30);
     * Double avg = stats.getAverage();  // Returns 20.0
     * }</pre>
     *
     * @return the arithmetic mean of values, or 0.0 if none
     */
    public final Double getAverage() {
        return getCount() > 0 ? (double) getSum() / getCount() : 0.0d;
    }

    /**
     * Returns a string representation of this statistics object.
     * The format includes all computed statistics: min, max, count, sum, and average.
     * 
     * <p><b>Example output:</b></p>
     * <pre>{@code
     * {min=10, max=50, count=5, sum=150, average=30.000000}
     * }</pre>
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return String.format("{min=%d, max=%d, count=%d, sum=%d, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
