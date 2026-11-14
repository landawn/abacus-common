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
 * Implementation of compensated (Kahan) summation for improved numerical precision.
 * <p>
 * Compensated summation significantly reduces the numerical error in the total obtained by adding
 * a sequence of finite-precision floating-point numbers, compared to the obvious approach. This
 * class uses Neumaier's variant (the Kahan-Babuska algorithm), which captures the rounding error
 * of every addition exactly, including an addition whose operand is larger in magnitude than the
 * running sum, where classic Kahan summation loses the smaller term.
 * <p>
 * Results are overflow-aware. Once any finite value, or the running total, reaches {@code 2^960} in
 * magnitude, well before the {@code double} range can be exceeded, the values are also accumulated
 * exactly from then on, and the total is rounded once, so
 * {@code KahanSummation.of(1e308, 1e308, -1e308, -1e308, 5).sum()} is {@code 5.0}. If any value is
 * infinite or {@code NaN}, the non-finite values alone decide the result, because finite values
 * cannot change an infinite or {@code NaN} total: {@code (MAX_VALUE, MAX_VALUE, -Infinity)} gives
 * {@code -Infinity} in any order, while {@code (Infinity, -Infinity)} gives {@code NaN}.
 * <p>
 * The exact accumulation uses {@code BigDecimal} arithmetic and is roughly a hundred times slower per value
 * than the compensated {@code double} path, but it runs only when a value or the running total reaches
 * {@code 2^960} (about {@code 9.75e288}). Real data essentially never does, apart from a
 * {@code Double.MAX_VALUE} sentinel (used as "infinity" or "not set") leaking into a sum; if it does,
 * the result is still correct, just slower. Ordinary sums pay only a magnitude check per value.
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
    // The context is used only once the exact total has started (see EXACT_TOTAL_THRESHOLD).
    private static final MathContext OVERFLOW_AVERAGE_CONTEXT = new MathContext(1200, RoundingMode.HALF_EVEN);

    // The exact total starts once any finite value, or the running total, reaches this magnitude. Below it the double
    // state cannot overflow: each step's rounding error is at most ulp(2^960) / 2 = 2^907, so even 2^63 steps keep
    // |correction| below 2^970. Only inputs this large can overflow, or drop a small term that later cancellation of
    // huge values exposes, so starting the exact total here, well before the overflow threshold, keeps every term from
    // that point on and rounds the total once. N.sum(double[]) and N.average(double[]) re-scan exactly at the same
    // point; keep the two in step. Ordinary data never gets this large and pays only for the comparison.
    static final double EXACT_TOTAL_THRESHOLD = 0x1p960;

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

    // Activated once a finite value or the running total reaches EXACT_TOTAL_THRESHOLD (2^960), well before the total
    // can overflow. The compensated prefix and all subsequent finite additions are then retained exactly, so a total
    // near or beyond the overflow threshold is rounded once, and later cancellation of huge values cannot expose terms
    // that the double state dropped.
    private BigDecimal overflowSafeSum;

    // False after a non-finite value, or an aggregate with unknown finite provenance, is seen.
    private boolean finiteValuesOnly = true;

    // IEEE 754 total of the non-finite values (and non-finite combined aggregates) seen so far: 0.0 until the
    // first one, then +/-Infinity or NaN for good. Finite values cannot change an infinite or NaN total, so
    // sum() and average() use this alone once it is non-finite. That keeps a finite prefix which overflows
    // toward the opposite sign from turning a single infinity into NaN: (MAX, MAX, -Infinity) -> -Infinity.
    private double nonFiniteSum;

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
     * Adds a single value to the summation using compensated (Kahan-Babuska) summation.
     * <p>
     * The algorithm maintains a running compensation (correction) for the low-order bits that each addition loses.
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
        final double previousSum = sum;
        final double previousCorrection = correction;
        count = Math.addExact(count, 1L);
        simpleSum += value;
        compensatedAdd(value);

        if (!Double.isFinite(value)) {
            nonFiniteSum += value;
            invalidateOverflowSafeSum();
        } else if (finiteValuesOnly) {
            if (overflowSafeSum != null) {
                if (value != 0d) { // An exact zero (either sign) cannot change the exact total: skip the BigDecimal work.
                    overflowSafeSum = overflowSafeSum.add(exactBigDecimal(value));
                }
            } else if (exactTotalNeeded(value)) {
                activateOverflowSafeSum(previousCount, previousSum, previousCorrection, previousSimpleSum, exactBigDecimal(value));
            }
        }
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
     * The provided {@code sumA} is incorporated via compensated summation to maintain precision.
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
        final double previousSum = sum;
        final double previousCorrection = correction;
        count = Math.addExact(count, countA);
        simpleSum += sumA;
        compensatedAdd(sumA);

        if (!Double.isFinite(sumA)) {
            // A non-finite aggregate sum may have come from finite values that
            // overflowed, but combine(count, sum) does not retain enough
            // information to reconstruct them, so it is treated like a non-finite value.
            nonFiniteSum += sumA;
            invalidateOverflowSafeSum();
        } else if (finiteValuesOnly) {
            if (countA > 0) {
                // Exact decimal state is needed only once a value or the running total reaches EXACT_TOTAL_THRESHOLD.
                if (overflowSafeSum != null) {
                    if (sumA != 0d) { // As in add(double): a zero aggregate cannot change the exact total.
                        overflowSafeSum = overflowSafeSum.add(exactBigDecimal(sumA));
                    }
                } else if (exactTotalNeeded(sumA)) {
                    activateOverflowSafeSum(previousCount, previousSum, previousCorrection, previousSimpleSum, exactBigDecimal(sumA));
                }
            } else if (sumA != 0d) {
                // A non-zero sum representing zero observations has no meaningful average.
                invalidateOverflowSafeSum();
            }
        }
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
        // other == this because compensatedAdd changes both sum and correction.
        final long otherCount = other.count;
        final double otherSimpleSum = other.simpleSum;
        final double otherSum = other.sum;
        final double otherCorrection = other.correction;
        final BigDecimal otherOverflowSafeSum = other.overflowSafeSum;
        final boolean otherFiniteValuesOnly = other.finiteValuesOnly;
        final double otherNonFiniteSum = other.nonFiniteSum;
        final long previousCount = count;
        final double previousSimpleSum = simpleSum;
        final double previousSum = sum;
        final double previousCorrection = correction;

        count = Math.addExact(count, otherCount);
        simpleSum += otherSimpleSum;

        compensatedAdd(otherSum);
        // Subtract the compensation bits: the running invariant is
        // (true sum) ~= sum - correction, so the other's correction must be
        // incorporated with a negated sign (see JDK-8214761 for the identical
        // fix in java.util.DoubleSummaryStatistics).
        compensatedAdd(-otherCorrection);

        // 0.0 unless the other summation has seen a non-finite value.
        nonFiniteSum += otherNonFiniteSum;

        if (finiteValuesOnly) {
            if (otherCount > 0 && otherFiniteValuesOnly) {
                // Avoid constructing exact terms on the normal double-only path.
                if (overflowSafeSum != null) {
                    final BigDecimal otherAggregate = otherOverflowSafeSum != null ? otherOverflowSafeSum
                            : exactCompensatedSum(otherSum, otherCorrection, otherSimpleSum);
                    overflowSafeSum = overflowSafeSum.add(otherAggregate);
                } else if (otherOverflowSafeSum != null || exactTotalNeeded(otherSum)) {
                    final BigDecimal otherAggregate = otherOverflowSafeSum != null ? otherOverflowSafeSum
                            : exactCompensatedSum(otherSum, otherCorrection, otherSimpleSum);
                    activateOverflowSafeSum(previousCount, previousSum, previousCorrection, previousSimpleSum, otherAggregate);
                }
            } else if (otherCount > 0 || otherSimpleSum != 0d) {
                invalidateOverflowSafeSum();
            }
        }
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
     *
     * <p>If the running total of finite values overflows, the higher-precision overflow-safe total is
     * rounded once instead, so the result is infinite only when the total is outside the {@code double}
     * range, and later cancellation is not lost:
     * {@code KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE).sum()} returns
     * {@code Double.MAX_VALUE}, the same as the order {@code (MAX, -MAX, MAX)}, and
     * {@code KahanSummation.of(Double.MAX_VALUE, 0x1p969, 0x1p969).sum()} returns {@code Infinity}, the
     * correctly rounded value of {@code MAX_VALUE + 2^970}. ({@link java.util.DoubleSummaryStatistics#getSum()}
     * returns {@code Infinity} and {@code NaN} for these two inputs.) The overflow-safe total keeps deciding the
     * result after later values bring the running total back into range, so
     * {@code KahanSummation.of(Double.MAX_VALUE, 0x1p969, 0x1p969, 1, -Double.MAX_VALUE, -0x1p969, -0x1p969).sum()}
     * returns {@code 1.0}. If any added value is infinite or
     * {@code NaN}, the non-finite values alone decide the result:
     * {@code KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY).sum()} returns
     * {@code -Infinity} rather than the {@code NaN} that sequential addition produces.</p>
     *
     * <p>The exact total starts once any value, or the running total, reaches {@code 2^960} in magnitude,
     * well before the overflow threshold, so the total of every input that comes near overflowing is
     * correctly rounded, including small values next to huge ones that later cancel:
     * {@code (1e-300, MAX_VALUE, -3.9e-7, -2^969, 3.0, 2^1023, 2^969, -MAX_VALUE, -2^1023)} returns
     * {@code 3.0 - 3.9e-7}. Values added before that point count as far as the compensated summation
     * retained them, which matters only for extreme cancellation among values below {@code 2^960}. A sum built
     * through {@link #combine(long, double)} from a non-finite {@code sumA} treats that value as an
     * infinite input, because the finite values behind it are unknown.</p>
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
        if (!Double.isFinite(nonFiniteSum)) {
            // Finite values cannot change an infinite or NaN total: (MAX, MAX, -Infinity) -> -Infinity, not NaN.
            return nonFiniteSum;
        }

        if (overflowSafeSum != null) {
            // Once started (a value or the running total reached EXACT_TOTAL_THRESHOLD), this is the authoritative total,
            // whatever the binary64 state shows: the double correction may have dropped terms that later cancellation
            // exposes, so (MAX, 2^969, 2^969, 1, -MAX, -2^969, -2^969) is 1, not 0, and a total near the overflow
            // threshold is rounded once: MAX_VALUE followed by two 2^969 values is MAX_VALUE + 2^970, so Infinity.
            return overflowSafeSum.doubleValue();
        }

        final double result = compensatedTotal();

        if (Double.isFinite(result) || !finiteValuesOnly) {
            return result;
        } else {
            // Defensive only: any non-finite total of finite values activates overflowSafeSum.
            return Double.isFinite(simpleSum) ? simpleSum : result;
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
     * higher-precision overflow-safe total instead of returning a spurious infinity, and keeps using it after
     * later values bring the running total back into range; {@link #sum()} describes when that total starts.
     * If any recorded value
     * is non-finite, the result is decided by the non-finite values alone, because finite values cannot change
     * an infinite or {@code NaN} total: {@code KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE,
     * Double.NEGATIVE_INFINITY).average()} is {@code -Infinity} rather than the {@code NaN} that sequential
     * addition produces, while {@code (Infinity, -Infinity)} still gives {@code NaN}.</p>
     *
     * @return an {@link OptionalDouble} containing the average, or an empty {@link OptionalDouble} if no values have been added
     * @see #sum()
     * @see #count()
     */
    public OptionalDouble average() {
        if (count == 0) {
            return OptionalDouble.empty();
        }

        if (!Double.isFinite(nonFiniteSum)) {
            return OptionalDouble.of(nonFiniteSum / count);
        }

        if (overflowSafeSum != null) {
            // Authoritative once started, whatever the binary64 state shows (see sum()).
            return OptionalDouble.of(overflowSafeAverage());
        }

        final double result = compensatedTotal();

        if (!Double.isFinite(result)) {
            if (finiteValuesOnly && Double.isFinite(simpleSum)) {
                // Defensive only: any non-finite total of finite values activates overflowSafeSum.
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

    // https://en.wikipedia.org/wiki/Kahan_summation_algorithm#Further_enhancements

    /**
     * Core compensated summation step (Neumaier's variant of Kahan summation).
     *
     * <p>This method updates the running {@code sum} and {@code correction} fields. The rounding error of
     * {@code sum + value} is recovered exactly whichever operand is larger in magnitude, so a small value
     * added before a large one is not lost, as it is in classic Kahan summation:
     * {@code (2^969, MAX_VALUE, 2^969)} keeps both {@code 2^969} terms. The invariant is
     * {@code (true sum) ~= sum - correction}, so the error is subtracted from {@code correction}.
     * {@link N} repeats this loop for arrays and must stay in step with it.</p>
     *
     * @param value the value to add
     */
    private void compensatedAdd(final double value) {
        final double t = sum + value;

        if (Math.abs(sum) >= Math.abs(value)) {
            correction -= (sum - t) + value;
        } else {
            correction -= (value - t) + sum;
        }

        sum = t;
    }

    /**
     * The compensated total {@code sum - correction} in binary64, without the overflow-safe or non-finite handling.
     * If that is {@code NaN} while the simple sum is infinite, the simple sum is returned instead.
     */
    private double compensatedTotal() {
        final double tmp = sum - correction;

        return Double.isNaN(tmp) && Double.isInfinite(simpleSum) ? simpleSum : tmp;
    }

    /**
     * Whether the exact total should start with the finite {@code added} value or aggregate, which has already been
     * applied to the running state: it or the running total has reached {@link #EXACT_TOTAL_THRESHOLD}.
     *
     * <p>No overflow check is needed: this runs after every finite step, and while every value and running total
     * stays below 2^960, each step's rounding error is at most about 2^908, so {@code simpleSum} and
     * {@code sum - correction} stay far below the overflow threshold. This runs on every {@code add}, so it is kept
     * to two comparisons.</p>
     */
    private boolean exactTotalNeeded(final double added) {
        return Math.abs(added) >= EXACT_TOTAL_THRESHOLD || Math.abs(sum) >= EXACT_TOTAL_THRESHOLD;
    }

    /**
     * Starts the exact total when a finite value or aggregate, or the running total, reaches {@link #EXACT_TOTAL_THRESHOLD}.
     * The previous* arguments are this summation's state before that addition.
     */
    private void activateOverflowSafeSum(final long previousCount, final double previousSum, final double previousCorrection, final double previousSimpleSum,
            final BigDecimal addedSum) {
        overflowSafeSum = previousCount == 0 ? addedSum : exactCompensatedSum(previousSum, previousCorrection, previousSimpleSum).add(addedSum);
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
