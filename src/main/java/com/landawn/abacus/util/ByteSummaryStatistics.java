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

import com.landawn.abacus.util.function.ByteConsumer;

/**
 * A state object for collecting statistics such as count, min, max, sum, and average
 * for a stream of byte values.
 * 
 * <p>This class is designed to work with byte streams and can be used as a 
 * reduction target for stream operations. It maintains running statistics that
 * can be queried at any time.</p>
 * 
 * <p>This implementation is not thread-safe. If used in parallel stream operations,
 * proper synchronization or thread-safe alternatives should be used.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ByteSummaryStatistics stats = new ByteSummaryStatistics();
 * stats.accept((byte) 10);
 * stats.accept((byte) 20);
 * stats.accept((byte) 30);
 * System.out.println("Count: " + stats.getCount());   // Count: 3
 * System.out.println("Average: " + stats.getAverage());   // Average: 20.0
 * }</pre>
 * 
 * @see ByteConsumer
 */
public class ByteSummaryStatistics implements ByteConsumer {

    private long count;

    private long sum;

    private byte min = Byte.MAX_VALUE;

    private byte max = Byte.MIN_VALUE;

    /**
     * Constructs an empty instance with zero count, zero sum,
     * {@code Byte.MAX_VALUE} min, and {@code Byte.MIN_VALUE} max.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics();
     * }</pre>
     */
    public ByteSummaryStatistics() {
    }

    /**
     * Constructs an instance with the specified initial values.
     * 
     * <p>This constructor is useful when creating a summary from pre-calculated
     * statistics or when merging multiple summaries.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics(5, (byte)10, (byte)50, 150);
     * // Creates statistics for 5 values ranging from 10 to 50 with sum of 150
     * }</pre>
     *
     * @param count the count of values
     * @param min the minimum value
     * @param max the maximum value
     * @param sum the sum of all values
     */
    public ByteSummaryStatistics(final long count, final byte min, final byte max, final long sum) {
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    /**
     * Records a new byte value into the summary statistics.
     * 
     * <p>This method updates all statistics (count, sum, min, max) to include
     * the new value. It implements the {@link ByteConsumer} interface, making
     * this class suitable for use with stream reduction operations.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics();
     * stats.accept((byte) 42);
     * stats.accept((byte) 17);
     * System.out.println(stats.getCount());   // 2
     * }</pre>
     *
     * @param value the byte value to record
     */
    @Override
    public void accept(final byte value) {
        ++count;
        sum += value;
        min = N.min(min, value);
        max = N.max(max, value);
    }

    /**
     * Combines the state of another {@code ByteSummaryStatistics} into this one.
     * 
     * <p>This method is useful when parallelizing statistics collection or when
     * merging statistics from multiple sources. After combining, this instance
     * will reflect statistics for all values from both instances.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats1 = new ByteSummaryStatistics();
     * stats1.accept((byte) 10);
     * stats1.accept((byte) 20);
     * 
     * ByteSummaryStatistics stats2 = new ByteSummaryStatistics();
     * stats2.accept((byte) 30);
     * stats2.accept((byte) 40);
     * 
     * stats1.combine(stats2);
     * System.out.println(stats1.getCount());   // 4
     * System.out.println(stats1.getSum());     // 100
     * }</pre>
     *
     * @param other another {@code ByteSummaryStatistics} to combine with this one
     */
    public void combine(final ByteSummaryStatistics other) {
        count += other.count;
        sum += other.sum;
        min = N.min(min, other.min);
        max = N.max(max, other.max);
    }

    /**
     * Returns the minimum value recorded, or {@code Byte.MAX_VALUE} if no
     * values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics();
     * stats.accept((byte) 50);
     * stats.accept((byte) 10);
     * stats.accept((byte) 30);
     * System.out.println(stats.getMin());   // 10
     * }</pre>
     *
     * @return the minimum value, or {@code Byte.MAX_VALUE} if none
     */
    public final byte getMin() {
        return min;
    }

    /**
     * Returns the maximum value recorded, or {@code Byte.MIN_VALUE} if no
     * values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics();
     * stats.accept((byte) 50);
     * stats.accept((byte) 10);
     * stats.accept((byte) 30);
     * System.out.println(stats.getMax());   // 50
     * }</pre>
     *
     * @return the maximum value, or {@code Byte.MIN_VALUE} if none
     */
    public final byte getMax() {
        return max;
    }

    /**
     * Returns the count of values recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics();
     * stats.accept((byte) 1);
     * stats.accept((byte) 2);
     * stats.accept((byte) 3);
     * System.out.println(stats.getCount());   // 3
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
     * <p>Note that the sum is maintained as a {@code long} to avoid overflow
     * for large numbers of byte values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics();
     * stats.accept((byte) 10);
     * stats.accept((byte) 20);
     * stats.accept((byte) 30);
     * System.out.println(stats.getSum());   // 60
     * }</pre>
     *
     * @return the sum of values, as a {@code Long}
     */
    public final Long getSum() {
        return sum;
    }

    /**
     * Returns the arithmetic mean of values recorded, or zero if no values
     * have been recorded.
     * 
     * <p>The average is calculated as a {@code double} to preserve precision.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics();
     * stats.accept((byte) 10);
     * stats.accept((byte) 20);
     * stats.accept((byte) 30);
     * System.out.println(stats.getAverage());   // 20.0
     * }</pre>
     *
     * @return the arithmetic mean of values, or zero if none
     */
    public final Double getAverage() {
        return getCount() > 0 ? (double) getSum() / getCount() : 0.0d;
    }

    /**
     * Returns a string representation of this summary, including all statistics.
     * 
     * <p>The format includes min, max, count, sum, and average values in a
     * readable format.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSummaryStatistics stats = new ByteSummaryStatistics();
     * stats.accept((byte) 10);
     * stats.accept((byte) 20);
     * System.out.println(stats.toString());
     * // {min=10, max=20, count=2, sum=30, average=15.000000}
     * }</pre>
     * 
     * @return a string representation of this summary
     */
    @Override
    public String toString() {
        return String.format("{min=%d, max=%d, count=%d, sum=%d, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
