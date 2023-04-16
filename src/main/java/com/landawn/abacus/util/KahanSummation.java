/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import com.landawn.abacus.util.u.OptionalDouble;

public class KahanSummation {

    private long count;

    private double correction;

    private double sum;

    private double simpleSum;

    /**
     *
     * @param a
     * @return
     */
    public static KahanSummation of(double... a) {
        final KahanSummation summation = new KahanSummation();

        for (double e : a) {
            summation.add(e);
        }

        return summation;
    }

    /**
     *
     * @param value
     */
    public void add(final double value) {
        count++;
        simpleSum += value;

        kahanSum(value);
    }

    /**
     * Adds the all.
     *
     * @param values
     */
    public void addAll(final double[] values) {
        for (double value : values) {
            add(value);
        }
    }

    /**
     *
     * @param countA
     * @param sumA
     */
    public void combine(final long countA, final double sumA) {
        this.count += countA;
        this.simpleSum += sumA;

        kahanSum(sumA);
    }

    /**
     *
     * @param other
     */
    public void combine(final KahanSummation other) {
        this.count += other.count;
        this.simpleSum += other.simpleSum;
        kahanSum(other.sum);
        kahanSum(other.correction);
    }

    /**
     * 
     *
     * @return 
     */
    public long count() {
        return count;
    }

    /**
     * 
     *
     * @return 
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
     * 
     *
     * @return 
     */
    public OptionalDouble average() {
        return count == 0 ? OptionalDouble.empty() : OptionalDouble.of(sum() / count());
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public String toString() {
        return String.format("{count=%d, sum=%f, average=%f}", count(), sum(), average().orElseZero());
    }

    // https://en.wikipedia.org/wiki/Kahan_summation_algorithm
    /**
     *
     * @param value
     */
    /*
    function KahanSum(input)
    var sum = input[1]
    var c = 0.0                 // A running compensation for lost low-order bits.
    for i = 2 to input.length do
        var y = input[i] - c    // So far, so good: c is zero at first.
        var t = sum + y         // Alas, sum is big, y small, so low-order digits of y are lost.
        c = (t - sum) - y       // (t - sum) cancels the high-order part of y; subtracting y recovers negative (low part of y)
        sum = t                 // Algebraically, c should always be zero. Beware overly-aggressive optimizing compilers!
    next i                      // Next time around, the lost low part will be added to y in a fresh attempt.
    return sum
    */
    private void kahanSum(final double value) {
        double y = value - correction;
        double t = sum + y;

        this.correction = (t - sum) - y;
        this.sum = t;
    }
}
