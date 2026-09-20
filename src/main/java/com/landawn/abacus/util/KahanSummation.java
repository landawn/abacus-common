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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

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
 * Instances are mutable and are not safe for concurrent updates without external synchronization.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * KahanSummation sum = new KahanSummation();
 * sum.add(0.1);
 * sum.add(0.2);
 * sum.add(0.3);
 * double result = sum.sum();   // more accurate than simple addition
 *
 * // Or use the static factory method:
 * KahanSummation sum2 = KahanSummation.of(0.1, 0.2, 0.3);
 * }</pre>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Kahan_summation_algorithm">Kahan summation algorithm</a>
 */
public final class KahanSummation { // NOSONAR

    // This precision covers the exact decimal expansion of a binary64 value plus the extra
    // quotient bits from any positive long count, including rounding decisions at subnormal ties.
    // The context is used only after the ordinary finite summation has overflowed.
    private static final MathContext OVERFLOW_AVERAGE_CONTEXT = new MathContext(1200, RoundingMode.HALF_EVEN);

    /**
     * Constructs a new KahanSummation with initial values of zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = new KahanSummation();
     * sum.add(0.1);
     * sum.add(0.2);
     * double result = sum.sum();   // 0.30000000000000004; compensation does not make every sum exact
     * }</pre>
     *
     */
    public KahanSummation() {
    }

    private long count;

    private double correction;

    private double sum;

    private double simpleSum;

    // Activated only after a finite simple sum overflows. The compensated prefix and all
    // subsequent finite additions are retained at higher precision, so later cancellation
    // does not amplify rounding performed by a single-double running mean.
    private BigDecimal overflowSafeSum;

    // False after a non-finite value, or an aggregate with unknown finite provenance, is seen.
    private boolean finiteValuesOnly = true;

    /**
     * Creates a new KahanSummation instance initialized with the provided values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = KahanSummation.of(1.0, 2.0, 3.0);
     * System.out.println(sum.sum());   // prints 6.0
     * }</pre>
     *
     * @param a the array of double values to sum; may be empty but not {@code null}
     * @return a new KahanSummation instance containing the sum of the provided values
     * @throws IllegalArgumentException if {@code a} is {@code null}
     * @see #addAll(double[])
     */
    public static KahanSummation of(final double... a) throws IllegalArgumentException {
        N.checkArgNotNull(a, cs.a);

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
     * for (int i = 0; i < 10; i++) {
     *     sum.add(0.1);
     * }
     * // sum.sum() returns 1.0 (correct), whereas naive summation yields 0.9999999999999999
     * }</pre>
     *
     * @param value the value to add to the summation
     * @throws ArithmeticException if the observation count would overflow; this summation is unchanged
     */
    public void add(final double value) throws ArithmeticException {
        final long previousCount = count;
        final double previousSimpleSum = simpleSum;
        count = Math.addExact(count, 1L);
        simpleSum += value;

        if (finiteValuesOnly) {
            if (overflowSafeSum == null) {
                if (!Double.isFinite(simpleSum)) {
                    if (Double.isFinite(value)) {
                        activateOverflowSafeSum(previousCount, previousSimpleSum, exactBigDecimal(value));
                    } else {
                        invalidateOverflowSafeSum();
                    }
                }
            } else if (Double.isFinite(value)) {
                overflowSafeSum = overflowSafeSum.add(exactBigDecimal(value));
            } else {
                invalidateOverflowSafeSum();
            }
        }

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
     * @param values the array of values to add to the summation; must not be {@code null}
     * @throws IllegalArgumentException if {@code values} is {@code null}
     * @throws ArithmeticException if the observation count would overflow; no values are added
     */
    public void addAll(final double[] values) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(values, cs.values);

        // Preflight the whole batch so count overflow cannot leave a partially added prefix.
        Math.addExact(count, values.length);

        for (final double value : values) {
            add(value);
        }
    }

    /**
     * Combines this summation with a pre-computed count and sum.
     * The provided {@code sumA} is incorporated via the Kahan algorithm to maintain precision.
     * This method is useful when merging externally computed partial sums.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum1 = new KahanSummation();
     * sum1.add(1.0);
     *
     * // Combine with pre-computed values
     * sum1.combine(5, 10.0);   // Merges 5 values whose total is 10.0
     * }</pre>
     *
     * @param countA the number of values represented by {@code sumA}
     * @param sumA the pre-computed sum of those values
     * @throws IllegalArgumentException if {@code countA} is negative.
     * @throws ArithmeticException if the combined observation count would overflow; this summation is unchanged
     */
    public void combine(final long countA, final double sumA) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNegative(countA, cs.countA);

        final long previousCount = count;
        final double previousSimpleSum = simpleSum;
        count = Math.addExact(count, countA);
        simpleSum += sumA;

        if (finiteValuesOnly) {
            if (countA > 0 && Double.isFinite(sumA)) {
                final BigDecimal addedSum = exactBigDecimal(sumA);

                if (overflowSafeSum != null) {
                    overflowSafeSum = overflowSafeSum.add(addedSum);
                } else if (!Double.isFinite(simpleSum)) {
                    activateOverflowSafeSum(previousCount, previousSimpleSum, addedSum);
                }
            } else if (countA > 0 || sumA != 0d) {
                // A non-finite aggregate sum may have come from finite values that
                // overflowed, but combine(count, sum) does not retain enough
                // information to reconstruct their average. Likewise, a non-zero
                // sum representing zero observations has no meaningful average.
                invalidateOverflowSafeSum();
            }
        }

        kahanSum(sumA);
    }

    /**
     * Combines this summation with another KahanSummation instance.
     * <p>
     * This method properly combines both the sum and the correction term from the other instance.
     * The operation is alias-safe: passing {@code this} doubles the represented observations in
     * the same way as combining two independently accumulated instances with identical state.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum1 = KahanSummation.of(0.1, 0.2);
     * KahanSummation sum2 = KahanSummation.of(0.3, 0.4);
     * sum1.combine(sum2);
     * // sum1 now contains the combined sum with proper error compensation
     * }</pre>
     *
     * @param other the other KahanSummation to combine with this one; may be this instance
     * @throws IllegalArgumentException if {@code other} is {@code null}
     * @throws ArithmeticException if the combined observation count would overflow; this summation is unchanged
     */
    public void combine(final KahanSummation other) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(other, cs.other);

        // Snapshot all source fields before mutating this instance. This is required when
        // other == this because kahanSum changes both sum and correction.
        final long otherCount = other.count;
        final double otherSimpleSum = other.simpleSum;
        final double otherSum = other.sum;
        final double otherCorrection = other.correction;
        final BigDecimal otherOverflowSafeSum = other.overflowSafeSum;
        final boolean otherFiniteValuesOnly = other.finiteValuesOnly;
        final long previousCount = count;
        final double previousSimpleSum = simpleSum;

        count = Math.addExact(count, otherCount);
        simpleSum += otherSimpleSum;

        if (finiteValuesOnly) {
            if (otherCount > 0 && otherFiniteValuesOnly) {
                final BigDecimal otherAggregate = otherOverflowSafeSum != null ? otherOverflowSafeSum
                        : exactCompensatedSum(otherSum, otherCorrection, otherSimpleSum);

                if (overflowSafeSum != null) {
                    overflowSafeSum = overflowSafeSum.add(otherAggregate);
                } else if (otherOverflowSafeSum != null || !Double.isFinite(simpleSum)) {
                    activateOverflowSafeSum(previousCount, previousSimpleSum, otherAggregate);
                }
            } else if (otherCount > 0 || otherSimpleSum != 0d) {
                invalidateOverflowSafeSum();
            }
        }

        kahanSum(otherSum);
        // Subtract the compensation bits: the running invariant is
        // (true sum) ~= sum - correction, so the other's correction must be
        // incorporated with a negated sign (see JDK-8214761 for the identical
        // fix in java.util.DoubleSummaryStatistics).
        kahanSum(-otherCorrection);
    }

    /**
     * Returns the count of values added to this summation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = new KahanSummation();
     * sum.add(1.0);
     * sum.add(2.0);
     * System.out.println(sum.count());   // prints 2
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
     * double result = sum.sum();   // more accurate than simple addition
     * }</pre>
     *
     * @return the sum with Kahan error compensation applied
     */
    public double sum() {
        // The running invariant is (true sum) ~= sum - correction, because
        // kahanSum stores correction = (t - sum) - y, i.e. the negated lost
        // low-order bits (see JDK-8214761 for the identical fix in
        // java.util.DoubleSummaryStatistics.getSum()).
        final double tmp = sum - correction;

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
     * System.out.println(avg.orElse(0));   // prints 2.5
     * }</pre>
     *
     * <p>If all recorded values are finite but their intermediate sum overflows, this method uses its
     * higher-precision overflow-safe total instead of returning a spurious infinity. If any recorded value
     * is non-finite, standard IEEE 754 propagation applies.</p>
     *
     * @return an {@link OptionalDouble} containing the average, or an empty {@link OptionalDouble} if no values have been added
     * @see #sum()
     * @see #count()
     */
    public OptionalDouble average() {
        if (count == 0) {
            return OptionalDouble.empty();
        }

        final double result = sum();

        if (!Double.isFinite(result)) {
            if (overflowSafeSum != null) {
                return OptionalDouble.of(overflowSafeAverage());
            } else if (finiteValuesOnly && Double.isFinite(simpleSum)) {
                // A compensated result can cross the finite boundary while the
                // simple sum remains finite. It is still a safe fallback mean.
                return OptionalDouble.of(simpleSum / count);
            }
        }

        return OptionalDouble.of(result / count);
    }

    /**
     * Returns a string representation of this KahanSummation.
     * <p>
     * The format is: {@code {count=<count>, sum=<sum>, average=<average>}}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KahanSummation sum = KahanSummation.of(1.0, 2.0, 3.0);
     * System.out.println(sum);   // prints {count=3, sum=6.000000, average=2.000000}
     * }</pre>
     *
     * @return a string representation containing count, sum, and average
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "{count=%d, sum=%f, average=%f}", count(), sum(), average().orElseZero());
    }

    // https://en.wikipedia.org/wiki/Kahan_summation_algorithm

    /**
     * Core Kahan summation algorithm implementation.
     *
     * <p>This method updates the running {@code sum} and {@code correction} fields
     * using the classic compensated summation technique to reduce floating-point error.</p>
     *
     * @param value the value to add using Kahan summation
     */
    private void kahanSum(final double value) {
        final double y = value - correction;
        final double t = sum + y;

        correction = (t - sum) - y;
        sum = t;
    }

    /** Activates the higher-precision fallback only after a finite aggregate crosses the representable range. */
    private void activateOverflowSafeSum(final long previousCount, final double previousSimpleSum, final BigDecimal addedSum) {
        overflowSafeSum = previousCount == 0 ? addedSum : exactCompensatedSum(sum, correction, previousSimpleSum).add(addedSum);
    }

    private void invalidateOverflowSafeSum() {
        finiteValuesOnly = false;
        overflowSafeSum = null;
    }

    private double overflowSafeAverage() {
        final double result = overflowSafeSum.divide(BigDecimal.valueOf(count), OVERFLOW_AVERAGE_CONTEXT).doubleValue();

        // The mathematical mean of finite values is bounded by those values. Guard against a
        // higher-precision compensated prefix rounding just beyond the binary64 finite boundary.
        return Double.isInfinite(result) ? Math.copySign(Double.MAX_VALUE, result) : result;
    }

    /** Reconstructs the represented compensated total without rounding sum - correction back to binary64. */
    private static BigDecimal exactCompensatedSum(final double sum, final double correction, final double simpleSum) {
        if (Double.isFinite(sum) && Double.isFinite(correction)) {
            return exactBigDecimal(sum).subtract(exactBigDecimal(correction));
        }

        return exactBigDecimal(simpleSum);
    }

    /** BigDecimal.valueOf deliberately uses a decimal surrogate; this constructor retains the exact binary64 value. */
    private static BigDecimal exactBigDecimal(final double value) {
        return new BigDecimal(value); // NOSONAR - exact binary64 reconstruction is required here.
    }
}
