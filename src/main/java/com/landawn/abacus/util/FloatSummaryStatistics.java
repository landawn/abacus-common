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
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * FloatSummaryStatistics stats = new FloatSummaryStatistics();
 * floatStream.forEach(stats::accept);
 *
 * System.out.println("Count: " + stats.getCount());
 * System.out.println("Sum: " + stats.getSum());
 * System.out.println("Min: " + stats.getMin());
 * System.out.println("Max: " + stats.getMax());
 * System.out.println("Average: " + stats.getAverage());
 * }</pre>
 *
 * @see KahanSummation
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
     * {@code Float.POSITIVE_INFINITY} min, and {@code Float.NEGATIVE_INFINITY} max.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * }</pre>
     *
     */
    public FloatSummaryStatistics() {
    }

    /**
     * Constructs an instance with the specified count, min, max, and sum.
     * This constructor is useful for creating a summary from pre-calculated values
     * or for combining multiple summaries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create statistics from known values
     * FloatSummaryStatistics stats = new FloatSummaryStatistics(5, 1.0f, 10.0f, 30.0);
     * }</pre>
     *
     * @param count the count of values; must be non-negative
     * @param min the minimum {@code float} value
     * @param max the maximum {@code float} value
     * @param sum the sum of all values as a {@code double}
     * @throws IllegalArgumentException if {@code count} is negative, the empty state is not canonical,
     *         {@code min} is greater than {@code max}, or the NaN states of {@code min}, {@code max},
     *         and {@code sum} are inconsistent
     */
    public FloatSummaryStatistics(final long count, final float min, final float max, final double sum) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        if (count == 0) {
            if (min != Float.POSITIVE_INFINITY || max != Float.NEGATIVE_INFINITY || sum != 0D) {
                throw new IllegalArgumentException("Invalid empty state: min, max, and sum must be canonical");
            }
        } else if (N.compare(min, max) > 0 || Float.isNaN(min) != Float.isNaN(max) || Float.isNaN(min) != Double.isNaN(sum)) {
            throw new IllegalArgumentException("minimum, maximum, and sum are inconsistent");
        }

        summation.combine(count, sum);

        this.min = min;
        this.max = max;
    }

    /**
     * Records a new float value into the summary information.
     * Updates the count, sum, min, and max accordingly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(3.14f);
     * stats.accept(2.71f);
     * stats.accept(1.41f);
     * }</pre>
     *
     * @param value the input value to be recorded
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats1 = new FloatSummaryStatistics();
     * FloatSummaryStatistics stats2 = new FloatSummaryStatistics();
     *
     * // Process different parts of data
     * part1.forEach(stats1::accept);
     * part2.forEach(stats2::accept);
     *
     * // Combine results
     * stats1.combine(stats2);
     * }</pre>
     *
     * @param other another {@code FloatSummaryStatistics} to be combined with this one
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(5.0f);
     * stats.accept(2.0f);
     * float min = stats.getMin();   // returns 2.0f
     * }</pre>
     *
     * @return the minimum value, or {@code Float.POSITIVE_INFINITY} if none
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(5.0f);
     * stats.accept(8.0f);
     * float max = stats.getMax();   // returns 8.0f
     * }</pre>
     *
     * @return the maximum value, or {@code Float.NEGATIVE_INFINITY} if none
     */
    public final float getMax() {
        return max;
    }

    /**
     * Returns the count of values recorded.
     * This includes values added via {@link #accept(float)}, values supplied through the
     * {@link #FloatSummaryStatistics(long, float, float, double)} constructor, and counts
     * merged in via {@link #combine(FloatSummaryStatistics)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(1.0f);
     * stats.accept(2.0f);
     * stats.accept(3.0f);
     * long count = stats.getCount();   // returns 3
     * }</pre>
     *
     * @return the count of values
     */
    public final long getCount() {
        return summation.count();
    }

    /**
     * Returns the sum of values recorded.
     * The sum is calculated using Kahan summation algorithm to minimize
     * floating-point rounding errors.
     *
     * <p>Note: The sum is returned as a {@code double} to maintain precision.
     * If no values have been recorded, returns 0.0.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(1.1f);
     * stats.accept(2.2f);
     * stats.accept(3.3f);
     * double sum = stats.getSum();   // returns 6.6 (approximately)
     * }</pre>
     *
     * @return the sum of values as a {@code double}, or {@code 0.0} if none
     */
    public final double getSum() {
        return summation.sum();
    }

    /**
     * Returns the arithmetic mean of values recorded, or {@code 0.0} if no values
     * have been recorded. The average is calculated as sum / count, where the sum
     * is accumulated with Kahan compensated summation to reduce floating-point error.
     *
     * <p>Note: If any recorded value was NaN or the sum is infinite, the result
     * may be NaN or infinite per standard floating-point arithmetic rules.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(2.0f);
     * stats.accept(4.0f);
     * stats.accept(6.0f);
     * double avg = stats.getAverage();   // returns 4.0
     * }</pre>
     *
     * @return the arithmetic mean of values as a {@code double}, or {@code 0.0} if none
     */
    public final double getAverage() {
        return summation.average().orElse(0d);
    }

    /**
     * Returns a string representation of this statistics object.
     * The string representation consists of the minimum, maximum, count, sum,
     * and average values in a readable format.
     *
     * <p>Format: {@code {min=<min>, max=<max>, count=<count>, sum=<sum>, average=<average>}}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = new FloatSummaryStatistics();
     * stats.accept(1.0f);
     * stats.accept(2.0f);
     * System.out.println(stats);
     * // Output: {min=1.000000, max=2.000000, count=2, sum=3.000000, average=1.500000}
     * }</pre>
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return String.format("{min=%f, max=%f, count=%d, sum=%f, average=%f}", getMin(), getMax(), getCount(), getSum(), getAverage());
    }
}
