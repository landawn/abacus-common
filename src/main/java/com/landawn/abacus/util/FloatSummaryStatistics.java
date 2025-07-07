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

import com.landawn.abacus.util.function.FloatConsumer;

/**
 * A state object for collecting statistics about float values.
 * This class is designed to work with streams of float values, providing
 * a way to calculate count, sum, min, max, and average in a single pass.
 * 
 * <p>This class uses Kahan summation algorithm internally to reduce numerical
 * errors when computing the sum of float values, providing better accuracy
 * than naive summation.</p>
 * 
 * <p>This class is mutable and not thread-safe. It's designed to be used
 * as a reduction target in stream operations.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * FloatSummaryStatistics stats = new FloatSummaryStatistics();
 * floatStream.forEach(stats::accept);
 * 
 * System.out.println("Count: " + stats.getCount());
 * System.out.println("Sum: " + stats.getSum());
 * System.out.println("Min: " + stats.getMin());
 * System.out.println("Max: " + stats.getMax());
 * System.out.println("Average: " + stats.getAverage());
 * </pre>
 * 
 * @see java.util.DoubleSummaryStatistics
 * @see java.util.IntSummaryStatistics
 * @see java.util.LongSummaryStatistics
 */
public class FloatSummaryStatistics implements FloatConsumer {

    private final KahanSummation summation = new KahanSummation();

    private float min = Float.POSITIVE_INFINITY;

    private float max = Float.NEGATIVE_INFINITY;

    /**
     * Constructs an empty instance with zero count, zero sum,
     * {@code Float.POSITIVE_INFINITY} min, {@code Float.NEGATIVE_INFINITY} max,
     * and zero average.
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * </pre>
     */
    public FloatSummaryStatistics() {
    }

    /**
     * Constructs a non-empty instance with the specified count, min, max, and sum.
     * This constructor is useful for creating a summary from pre-calculated values
     * or for combining multiple summaries.
     *
     * @param count the count of values (must be non-negative)
     * @param min the minimum value
     * @param max the maximum value
     * @param sum the sum of all values
     * @throws IllegalArgumentException if count is negative
     * 
     * <p>Example:</p>
     * <pre>
     * // Create statistics from known values
     * FloatSummaryStatistics stats = new FloatSummaryStatistics(5, 1.0f, 10.0f, 30.0);
     * </pre>
     */
    public FloatSummaryStatistics(final long count, final float min, final float max, final double sum) {
        summation.combine(count, sum);

        this.min = min;
        this.max = max;
    }

    /**
     * Records a new float value into the summary information.
     * Updates the count, sum, min, and max accordingly.
     *
     * @param value the input value to be recorded
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(3.14f);
     * stats.accept(2.71f);
     * stats.accept(1.41f);
     * </pre>
     */
    @Override
    public void accept(final float value) {
        summation.add(value);

        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    /**
     * Combines the state of another {@code FloatSummaryStatistics} into this one.
     * This method is useful for parallel processing where multiple statistics
     * objects are computed independently and then combined.
     *
     * @param other another {@code FloatSummaryStatistics} to be combined with this one
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats1 = new FloatSummaryStatistics();
     * FloatSummaryStatistics stats2 = new FloatSummaryStatistics();
     * 
     * // Process different parts of data
     * part1.forEach(stats1::accept);
     * part2.forEach(stats2::accept);
     * 
     * // Combine results
     * stats1.combine(stats2);
     * </pre>
     */
    public void combine(final FloatSummaryStatistics other) {
        summation.combine(other.summation);

        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
    }

    /**
     * Returns the minimum value recorded, or {@code Float.POSITIVE_INFINITY}
     * if no values have been recorded.
     * 
     * <p>Note: If any recorded value was NaN, then the result will be NaN.</p>
     *
     * @return the minimum value, or {@code Float.POSITIVE_INFINITY} if none
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(5.0f);
     * stats.accept(2.0f);
     * float min = stats.getMin(); // Returns 2.0f
     * </pre>
     */
    public final float getMin() {
        return min;
    }

    /**
     * Returns the maximum value recorded, or {@code Float.NEGATIVE_INFINITY}
     * if no values have been recorded.
     * 
     * <p>Note: If any recorded value was NaN, then the result will be NaN.</p>
     *
     * @return the maximum value, or {@code Float.NEGATIVE_INFINITY} if none
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(5.0f);
     * stats.accept(8.0f);
     * float max = stats.getMax(); // Returns 8.0f
     * </pre>
     */
    public final float getMax() {
        return max;
    }

    /**
     * Returns the count of values recorded.
     * This represents the number of times {@link #accept(float)} has been called.
     *
     * @return the count of values
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(1.0f);
     * stats.accept(2.0f);
     * stats.accept(3.0f);
     * long count = stats.getCount(); // Returns 3
     * </pre>
     */
    public final long getCount() {
        return summation.count();
    }

    /**
     * Returns the sum of values recorded.
     * The sum is calculated using Kahan summation algorithm to minimize
     * floating-point rounding errors.
     * 
     * <p>Note: The sum is returned as a Double to maintain precision.
     * If no values have been recorded, returns 0.0.</p>
     *
     * @return the sum of values, or zero if none
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(1.1f);
     * stats.accept(2.2f);
     * stats.accept(3.3f);
     * Double sum = stats.getSum(); // Returns 6.6 (approximately)
     * </pre>
     */
    public final Double getSum() {
        return summation.sum();
    }

    /**
     * Returns the arithmetic mean of values recorded, or zero if no values have been recorded.
     * The average is calculated as sum / count using high-precision arithmetic.
     * 
     * <p>Note: If the sum is NaN or infinite, or if the count is zero,
     * special rules apply as per floating-point arithmetic.</p>
     *
     * @return the arithmetic mean of values, or zero if none
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(2.0f);
     * stats.accept(4.0f);
     * stats.accept(6.0f);
     * Double avg = stats.getAverage(); // Returns 4.0
     * </pre>
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

    /**
     * Returns a string representation of this statistics object.
     * The string representation consists of the minimum, maximum, count, sum, 
     * and average values in a readable format.
     * 
     * <p>Format: {@code {min=<min>, max=<max>, count=<count>, sum=<sum>, average=<average>}}</p>
     *
     * @return a string representation of this object
     * 
     * <p>Example:</p>
     * <pre>
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(1.0f);
     * stats.accept(2.0f);
     * System.out.println(stats);
     * // Output: {min=1.000000, max=2.000000, count=2, sum=3.000000, average=1.500000}
     * </pre>
     */
    @Override
    public String toString() {
        return String.format("{min=%f, max=%f, count=%d, sum=%f, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}