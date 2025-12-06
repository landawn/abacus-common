/*
 * Copyright (C) 2019 HaiYang Li
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

import com.landawn.abacus.util.u.OptionalDouble;

/**
 * Implementation of Kahan summation algorithm for improved numerical precision.
 * <p>
 * The Kahan summation algorithm is a compensated summation technique that significantly
 * reduces the numerical error in the total obtained by adding a sequence of finite-precision
 * floating-point numbers, compared to the obvious approach.
 * <p>
 * This is particularly useful when summing many numbers where intermediate results may
 * lose precision due to floating-point arithmetic limitations.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * KahanSummation sum = new KahanSummation();
 * sum.add(0.1);
 * sum.add(0.2);
 * sum.add(0.3);
 * double result = sum.sum();   // More accurate than simple addition
 * 
 * // Or use the static factory method:
 * KahanSummation sum2 = KahanSummation.of(0.1, 0.2, 0.3);
 * }</pre>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Kahan_summation_algorithm">Kahan summation algorithm</a>
 */
public final class KahanSummation { // NOSONAR

    /**
     * Constructs a new KahanSummation with initial values of zero.
     */
    public KahanSummation() {
    }

    private long count;

    private double correction;

    private double sum;

    private double simpleSum;

    /**
     * Creates a static KahanSummation instance initialized with the provided values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = KahanSummation.of(1.0, 2.0, 3.0);
     * System.out.println(sum.sum());   // 6.0
     * }</pre>
     *
     * @param a the array of double values to sum
     * @return a new KahanSummation instance containing the sum of the provided values
     */
    public static KahanSummation of(final double... a) {
        final KahanSummation summation = new KahanSummation();

        for (final double e : a) {
            summation.add(e);
        }

        return summation;
    }

    /**
     * Adds a single value to the summation using the Kahan algorithm.
     * <p>
     * The algorithm maintains a running compensation (correction) for lost low-order bits.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = new KahanSummation();
     * sum.add(1e20);
     * sum.add(1.0);
     * sum.add(-1e20);
     * // Result will be 1.0 (correct), not 0.0 (which simple summation might give)
     * }</pre>
     *
     * @param value the value to add to the summation
     */
    public void add(final double value) {
        count++;
        simpleSum += value;

        kahanSum(value);
    }

    /**
     * Adds all values from the provided array to the summation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = new KahanSummation();
     * double[] values = {0.1, 0.2, 0.3, 0.4, 0.5};
     * sum.addAll(values);
     * }</pre>
     *
     * @param values the array of values to add to the summation
     */
    public void addAll(final double[] values) {
        for (final double value : values) {
            add(value);
        }
    }

    /**
     * Combines this summation with values from simple summation (count and sum).
     * <p>
     * This method is useful when combining pre-computed sums.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum1 = new KahanSummation();
     * sum1.add(1.0);
     * 
     * // Combine with pre-computed values
     * sum1.combine(5, 10.0);   // Add 5 values with sum 10.0
     * }</pre>
     *
     * @param countA the count of values to combine
     * @param sumA the sum of values to combine
     */
    public void combine(final long countA, final double sumA) {
        count += countA;
        simpleSum += sumA;

        kahanSum(sumA);
    }

    /**
     * Combines this summation with another KahanSummation instance.
     * <p>
     * This method properly combines both the sum and the correction term from the other instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum1 = KahanSummation.of(0.1, 0.2);
     * KahanSummation sum2 = KahanSummation.of(0.3, 0.4);
     * sum1.combine(sum2);
     * // sum1 now contains the combined sum with proper error compensation
     * }</pre>
     *
     * @param other the other KahanSummation to combine with this one
     */
    public void combine(final KahanSummation other) {
        count += other.count;
        simpleSum += other.simpleSum;
        kahanSum(other.sum);
        kahanSum(other.correction);
    }

    /**
     * Returns the count of values added to this summation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = new KahanSummation();
     * sum.add(1.0);
     * sum.add(2.0);
     * System.out.println(sum.count());   // 2
     * }</pre>
     *
     * @return the number of values that have been added
     */
    public long count() {
        return count;
    }

    /**
     * Returns the compensated sum of all added values.
     * <p>
     * If the result is NaN and the simple sum is infinite, returns the simple sum instead.
     * This handles edge cases where the compensation might produce NaN.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = new KahanSummation();
     * for (int i = 0; i < 1000000; i++) {
     *     sum.add(0.01);
     * }
     * double result = sum.sum();   // More accurate than simple addition
     * }</pre>
     *
     * @return the sum with Kahan error compensation applied
     */
    public double sum() {
        final double tmp = sum + correction;

        if (Double.isNaN(tmp) && Double.isInfinite(simpleSum)) {
            return simpleSum;
        } else {
            return tmp;
        }
    }

    /**
     * Calculates and returns the average of all added values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = KahanSummation.of(1.0, 2.0, 3.0, 4.0);
     * OptionalDouble avg = sum.average();
     * System.out.println(avg.orElse(0));   // 2.5
     * }</pre>
     *
     * @return an OptionalDouble containing the average, or empty if no values have been added
     */
    public OptionalDouble average() {
        return count == 0 ? OptionalDouble.empty() : OptionalDouble.of(sum() / count());
    }

    /**
     * Returns a string representation of this KahanSummation.
     * <p>
     * The format is: {@code {count=<count>, sum=<sum>, average=<average>}}
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = KahanSummation.of(1.0, 2.0, 3.0);
     * System.out.println(sum);   // {count=3, sum=6.000000, average=2.000000}
     * }</pre>
     *
     * @return a string representation containing count, sum, and average
     */
    @Override
    public String toString() {
        return String.format("{count=%d, sum=%f, average=%f}", count(), sum(), average().orElseZero());
    }

    // https://en.wikipedia.org/wiki/Kahan_summation_algorithm

    /**
     * Core Kahan summation algorithm implementation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * function KahanSum(input)
     * var sum = input[1]
     * var c = 0.0                 // A running compensation for lost low-order bits.
     * for i = 2 to input.length do
     *     var y = input[i] - c    // So far, so good: c is zero at first.
     *     var t = sum + y         // Alas, sum is big, y small, so low-order digits of y are lost.
     *     c = (t - sum) - y       // (t - sum) cancels the high-order part of y; subtracting y recovers negative (low part of y)
     *     sum = t                 // Algebraically, c should always be zero. Beware overly aggressive optimizing compilers!
     * next i                      // Next time around, the lost low part will be added to y in a fresh attempt.
     * return sum
     * }</pre>
     *
     * @param value the value to add using Kahan summation
     */
    private void kahanSum(final double value) {
        final double y = value - correction;
        final double t = sum + y;

        correction = (t - sum) - y;
        sum = t;
    }
}
