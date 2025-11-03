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

import com.landawn.abacus.util.function.CharConsumer;

/**
 * A state object for collecting statistics such as count, min, max, sum, and average
 * for a stream of char values.
 * 
 * <p>This class is designed to work with char streams and can be used as a 
 * reduction target for stream operations. It maintains running statistics that
 * can be queried at any time.</p>
 * 
 * <p>This implementation is not thread-safe. If used in parallel stream operations,
 * proper synchronization or thread-safe alternatives should be used.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * CharSummaryStatistics stats = new CharSummaryStatistics();
 * stats.accept('A');
 * stats.accept('B');
 * stats.accept('C');
 * System.out.println("Count: " + stats.getCount()); // Count: 3
 * System.out.println("Min: " + stats.getMin()); // Min: A
 * System.out.println("Max: " + stats.getMax()); // Max: C
 * }</pre>
 * 
 * @see CharConsumer
 */
public class CharSummaryStatistics implements CharConsumer {

    private long count;

    private long sum;

    private char min = Character.MAX_VALUE;

    private char max = Character.MIN_VALUE;

    /**
     * Constructs an empty instance with zero count, zero sum,
     * {@code Character.MAX_VALUE} min, and {@code Character.MIN_VALUE} max.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics();
     * }</pre>
     */
    public CharSummaryStatistics() {
    }

    /**
     * Constructs an instance with the specified initial values.
     * 
     * <p>This constructor is useful when creating a summary from pre-calculated
     * statistics or when merging multiple summaries.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics(5, 'A', 'E', 335);
     * // Creates statistics for 5 chars ranging from 'A' to 'E' with sum of 335
     * }</pre>
     *
     * @param count the count of values
     * @param min the minimum value
     * @param max the maximum value
     * @param sum the sum of all values
     */
    public CharSummaryStatistics(final long count, final char min, final char max, final long sum) {
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    /**
     * Records a new char value into the summary statistics.
     * 
     * <p>This method updates all statistics (count, sum, min, max) to include
     * the new value. It implements the {@link CharConsumer} interface, making
     * this class suitable for use with stream reduction operations.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics();
     * stats.accept('X');
     * stats.accept('Y');
     * System.out.println(stats.getCount()); // 2
     * }</pre>
     *
     * @param value the char value to record
     */
    @Override
    public void accept(final char value) {
        ++count;
        sum += value;
        min = N.min(min, value);
        max = N.max(max, value);
    }

    /**
     * Combines the state of another {@code CharSummaryStatistics} into this one.
     * 
     * <p>This method is useful when parallelizing statistics collection or when
     * merging statistics from multiple sources. After combining, this instance
     * will reflect statistics for all values from both instances.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats1 = new CharSummaryStatistics();
     * stats1.accept('A');
     * stats1.accept('B');
     * 
     * CharSummaryStatistics stats2 = new CharSummaryStatistics();
     * stats2.accept('Y');
     * stats2.accept('Z');
     * 
     * stats1.combine(stats2);
     * System.out.println(stats1.getCount()); // 4
     * System.out.println(stats1.getMin()); // A
     * System.out.println(stats1.getMax()); // Z
     * }</pre>
     *
     * @param other another {@code CharSummaryStatistics} to combine with this one
     */
    public void combine(final CharSummaryStatistics other) {
        count += other.count;
        sum += other.sum;
        min = N.min(min, other.min);
        max = N.max(max, other.max);
    }

    /**
     * Returns the minimum char value recorded, or {@code Character.MAX_VALUE} if no
     * values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics();
     * stats.accept('M');
     * stats.accept('A');
     * stats.accept('Z');
     * System.out.println(stats.getMin()); // A
     * }</pre>
     *
     * @return the minimum char value, or {@code Character.MAX_VALUE} if none
     */
    public final char getMin() {
        return min;
    }

    /**
     * Returns the maximum char value recorded, or {@code Character.MIN_VALUE} if no
     * values have been recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics();
     * stats.accept('M');
     * stats.accept('A');
     * stats.accept('Z');
     * System.out.println(stats.getMax()); // Z
     * }</pre>
     *
     * @return the maximum char value, or {@code Character.MIN_VALUE} if none
     */
    public final char getMax() {
        return max;
    }

    /**
     * Returns the count of values recorded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics();
     * stats.accept('X');
     * stats.accept('Y');
     * stats.accept('Z');
     * System.out.println(stats.getCount()); // 3
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
     * <p>Note that the sum is maintained as a {@code long} to avoid overflow.
     * The sum represents the total of the numeric values of all recorded chars.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics();
     * stats.accept('A'); // ASCII value 65
     * stats.accept('B'); // ASCII value 66
     * stats.accept('C'); // ASCII value 67
     * System.out.println(stats.getSum()); // 198
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
     * <p>The average is calculated as a {@code double} to preserve precision.
     * The average represents the mean of the numeric values of all recorded chars.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics();
     * stats.accept('A'); // ASCII value 65
     * stats.accept('B'); // ASCII value 66
     * stats.accept('C'); // ASCII value 67
     * System.out.println(stats.getAverage()); // 66.0
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
     * readable format. The min and max values are displayed as characters.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = new CharSummaryStatistics();
     * stats.accept('A');
     * stats.accept('B');
     * System.out.println(stats.toString());
     * // {min=A, max=B, count=2, sum=131, average=65.500000}
     * }</pre>
     * 
     * @return a string representation of this summary
     */
    @Override
    public String toString() {
        return String.format("{min=%c, max=%c, count=%d, sum=%d, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
