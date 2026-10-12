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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NumbersMathTest extends NumbersTestSupport {

    // A 60-digit reference for the hyperbolic inverses, independent of java.lang.Math.
    private static final MathContext MC = new MathContext(60, RoundingMode.HALF_EVEN);

    private static final BigDecimal EPS = new BigDecimal("1e-72");

    private static final BigDecimal LN2 = lnNearOne(TWO);

    /** {@code atanh(z) = z + z^3/3 + z^5/5 + ...}; used for {@code |z| <= 0.6}. */
    private static BigDecimal atanhSeries(final BigDecimal z) {
        final BigDecimal z2 = z.multiply(z, MC);
        BigDecimal term = z;
        BigDecimal sum = z;

        for (int n = 3; n < 100_000; n += 2) {
            term = term.multiply(z2, MC);
            final BigDecimal add = term.divide(BigDecimal.valueOf(n), MC);

            if (add.abs().compareTo(EPS) < 0) {
                break;
            }

            sum = sum.add(add, MC);
        }

        return sum;
    }

    /** {@code ln(y) = 2 atanh((y - 1) / (y + 1))} for {@code y} in {@code [0.5, 4]}. */
    private static BigDecimal lnNearOne(final BigDecimal y) {
        return atanhSeries(y.subtract(BigDecimal.ONE).divide(y.add(BigDecimal.ONE), MC)).multiply(TWO, MC);
    }

    /** {@code ln(y)} for any positive {@code y}, reduced by powers of two into {@code [1, 2)}. */
    private static BigDecimal ln(BigDecimal y) {
        int k = 0;

        while (y.compareTo(TWO) >= 0) {
            y = y.divide(TWO, MC);
            k++;
        }

        while (y.compareTo(BigDecimal.ONE) < 0) {
            y = y.multiply(TWO, MC);
            k--;
        }

        return lnNearOne(y).add(LN2.multiply(BigDecimal.valueOf(k), MC), MC);
    }

    private static BigDecimal asinhRef(final double x) {
        final BigDecimal bx = new BigDecimal(x);
        return ln(bx.add(bx.multiply(bx, MC).add(BigDecimal.ONE, MC).sqrt(MC), MC));
    }

    private static BigDecimal atanhRef(final double x) {
        final BigDecimal bx = new BigDecimal(x);
        return ln(BigDecimal.ONE.add(bx, MC).divide(BigDecimal.ONE.subtract(bx, MC), MC)).divide(TWO, MC);
    }

    private static double ulps(final double got, final BigDecimal ref) {
        return Math.abs(new BigDecimal(got).subtract(ref, MC).doubleValue()) / Math.ulp(ref.doubleValue());
    }

    @Test
    public void testAcosh() {
        assertEquals(0.0, Numbers.acosh(1.0), 1e-12);
        assertTrue(Double.isFinite(Numbers.acosh(Double.MAX_VALUE)));
        assertTrue(Double.isNaN(Numbers.acosh(0.5)));
    }

    // asinh

    @Test
    public void testAsinh() {
        assertEquals(0.0, Numbers.asinh(0.0), DELTA);
        assertEquals(-Numbers.asinh(0.5), Numbers.asinh(-0.5), 1e-12);
        assertTrue(Double.isFinite(Numbers.asinh(Double.MAX_VALUE)));
        final double expected = Math.log(Double.MAX_VALUE) + Math.log(2.0);
        assertEquals(expected, Numbers.asinh(Double.MAX_VALUE), Math.ulp(expected));
        assertEquals(-expected, Numbers.asinh(-Double.MAX_VALUE), Math.ulp(expected));
        for (final double x : new double[] { 0.5, 2.0, 10.0 }) {
            assertEquals(Math.log(x + Math.sqrt(x * x + 1)), Numbers.asinh(x), 1e-12, "x=" + x);
        }
    }

    // C-011: accuracy
    @Test
    @DisplayName("C-011: asinh is within 2 ulp of a 60-digit reference across the finite range")
    public void test_C011_asinhWithinTwoUlp() {
        final Random random = new Random(20260902L);
        double worst = 0;

        for (int i = 0; i < 3_000; i++) {
            // magnitudes from 2^-30 up to 2^300, both signs
            final double x = Math.copySign(Math.scalb(1 + random.nextDouble(), random.nextInt(330) - 30), random.nextBoolean() ? 1 : -1);
            final double u = ulps(Math.abs(Numbers.asinh(x)), asinhRef(Math.abs(x)));
            worst = Math.max(worst, u);
            assertTrue(u <= 2.0, "asinh(" + x + ") is " + u + " ulp off");
        }

        // The values where the previous implementation was worst (8.0 ulp and 1.6 ulp).
        for (final double x : new double[] { 0.24778358001241585, 1.1182790946252728, 0.2, 0.167, 0.1670001, 0.097, 0.036, 0.0036, 0.5, 0.9, 1.0, 2.0 }) {
            assertTrue(ulps(Numbers.asinh(x), asinhRef(x)) <= 2.0, String.valueOf(x));
        }

        assertTrue(worst <= 2.0, String.valueOf(worst));
    }

    @Test
    @DisplayName("C-011: the documented example values still hold")
    public void test_C011_documentedExamples() {
        assertEquals(0.88137, Numbers.asinh(1.0), 1e-5);
        assertEquals(-0.88137, Numbers.asinh(-1.0), 1e-5);
        assertEquals(2.99822, Numbers.asinh(10.0), 1e-5);
        assertEquals(0.54931, Numbers.atanh(0.5), 1e-5);
        assertEquals(-0.54931, Numbers.atanh(-0.5), 1e-5);
        assertEquals(1.47222, Numbers.atanh(0.9), 1e-5);
        // ... and within the documented 2 ulp of the reference at every example.
        for (final double x : new double[] { 1.0, 10.0 }) {
            assertTrue(ulps(Numbers.asinh(x), asinhRef(x)) <= 2.0, String.valueOf(x));
        }
        for (final double x : new double[] { 0.5, 0.9 }) {
            assertTrue(ulps(Numbers.atanh(x), atanhRef(x)) <= 2.0, String.valueOf(x));
        }
    }

    // atanh

    @Test
    public void testAtanh() {
        assertEquals(0.0, Numbers.atanh(0.0), DELTA);
        assertEquals(-Numbers.atanh(0.5), Numbers.atanh(-0.5), 1e-12);
        assertTrue(Double.isInfinite(Numbers.atanh(1.0)));
        assertTrue(Double.isNaN(Numbers.atanh(1.5)));
        assertTrue(Double.isNaN(Numbers.atanh(Double.NaN)));
        for (final double x : new double[] { 0.001, 0.003, 0.031, 0.087, 0.15, 0.5 }) {
            assertEquals(0.5d * Math.log((1 + x) / (1 - x)), Numbers.atanh(x), 1E-12, "x=" + x);
        }
    }

    @Test
    @DisplayName("C-011: atanh is within 2 ulp of a 60-digit reference across the open domain")
    public void test_C011_atanhWithinTwoUlp() {
        final Random random = new Random(20260902L);

        for (int i = 0; i < 3_000; i++) {
            final double x = switch (i % 3) {
                case 0 -> random.nextDouble() * 0.15 + 1e-12; // the old series range
                case 1 -> 0.15 + random.nextDouble() * 0.849; // the old log range
                default -> 1 - Math.scalb(1 + random.nextDouble(), -10 - random.nextInt(30)); // hugging 1
            };
            final double signed = random.nextBoolean() ? x : -x;
            final double u = ulps(Math.abs(Numbers.atanh(signed)), atanhRef(x));
            assertTrue(u <= 2.0, "atanh(" + signed + ") is " + u + " ulp off");
        }

        // The values where the previous implementation was worst (3.8 ulp), and the old branch boundaries.
        for (final double x : new double[] { 0.1596407553086861, 0.1837689664409789, 0.3, 0.15, 0.1500001, 0.087, 0.031, 0.003, 0.5, 0.9 }) {
            assertTrue(ulps(Numbers.atanh(x), atanhRef(x)) <= 2.0, String.valueOf(x));
        }
    }

    @Test
    @DisplayName("C-011: special values and signed zero are unchanged by the reformulation")
    public void test_C011_specialValues() {
        // asinh: every real is in the domain, nothing throws
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.asinh(0.0)));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.asinh(-0.0)));
        assertTrue(Double.isNaN(Numbers.asinh(Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.asinh(Double.POSITIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.asinh(Double.NEGATIVE_INFINITY));
        assertEquals(Math.log(Double.MAX_VALUE) + Math.log(2), Numbers.asinh(Double.MAX_VALUE), 1e-12);
        assertEquals(-Numbers.asinh(Double.MAX_VALUE), Numbers.asinh(-Double.MAX_VALUE));
        // asinh(a) == a exactly for tiny and subnormal a (the series used to produce this; log1p does too),
        // and within an ulp where the a^3/6 term is just below the last bit.
        for (final double tiny : new double[] { Double.MIN_VALUE, -Double.MIN_VALUE, 1e-320, 0x1p-60, -0x1p-60 }) {
            assertEquals(tiny, Numbers.asinh(tiny), String.valueOf(tiny));
        }
        assertEquals(0x1p-30, Numbers.asinh(0x1p-30), Math.ulp(0x1p-30));
        // odd function
        for (final double x : new double[] { 0.001, 0.05, 0.13, 0.2, 1.5, 100.0, 1e10, 1e100 }) {
            assertEquals(-Numbers.asinh(x), Numbers.asinh(-x), String.valueOf(x));
        }

        // atanh: open domain (-1, 1); +-1 are +-Infinity; beyond is NaN; nothing throws
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.atanh(0.0)));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.atanh(-0.0)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.atanh(1.0));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.atanh(-1.0));
        assertTrue(Double.isNaN(Numbers.atanh(1.5)));
        assertTrue(Double.isNaN(Numbers.atanh(-1.5)));
        assertTrue(Double.isNaN(Numbers.atanh(Math.nextUp(1.0))));
        assertTrue(Double.isNaN(Numbers.atanh(Double.POSITIVE_INFINITY)));
        assertTrue(Double.isNaN(Numbers.atanh(Double.NEGATIVE_INFINITY)));
        assertTrue(Double.isNaN(Numbers.atanh(Double.NaN)));
        assertTrue(Double.isFinite(Numbers.atanh(Math.nextDown(1.0))));
        assertTrue(Numbers.atanh(Math.nextDown(1.0)) > 18.0);
        for (final double tiny : new double[] { Double.MIN_VALUE, -Double.MIN_VALUE, 1e-320, 0x1p-60, -0x1p-60 }) {
            assertEquals(tiny, Numbers.atanh(tiny), String.valueOf(tiny));
        }
        assertEquals(0x1p-30, Numbers.atanh(0x1p-30), Math.ulp(0x1p-30));
        for (final double x : new double[] { 0.001, 0.05, 0.13, 0.2, 0.5, 0.9, 0.999999 }) {
            assertEquals(-Numbers.atanh(x), Numbers.atanh(-x), String.valueOf(x));
        }
    }

    @Test
    public void testLog() {
        assertEquals(0.0, Numbers.log(1.0), DELTA);
        assertEquals(Math.log(Math.E), Numbers.log(Math.E), DELTA);
        assertEquals(Math.log(10.0), Numbers.log(10.0), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.log(0.0));
        assertTrue(Double.isNaN(Numbers.log(-1.0)));
    }

    // log2

    @Test
    public void testLog2_BigInteger() {
        assertEquals(0, Numbers.log2(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log2(BigInteger.TWO, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(BigInteger.valueOf(10), RoundingMode.DOWN));
        assertEquals(4, Numbers.log2(BigInteger.valueOf(10), RoundingMode.UP));
        assertEquals(4, Numbers.log2(BigInteger.valueOf(12), RoundingMode.HALF_EVEN));
        assertEquals(9, Numbers.log2(new BigInteger("1000"), RoundingMode.FLOOR));
        assertEquals(10, Numbers.log2(new BigInteger("1000"), RoundingMode.CEILING));

        final BigInteger pow2_100 = BigInteger.TWO.pow(100);
        assertEquals(100, Numbers.log2(pow2_100, RoundingMode.HALF_UP));
        assertEquals(100, Numbers.log2(pow2_100.add(BigInteger.ONE), RoundingMode.DOWN));
        assertEquals(101, Numbers.log2(pow2_100.add(BigInteger.ONE), RoundingMode.UP));
        assertEquals(300, Numbers.log2(BigInteger.TWO.pow(300), RoundingMode.HALF_EVEN));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(BigInteger.ZERO, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(BigInteger.valueOf(-1), RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2((BigInteger) null, RoundingMode.DOWN));
    }

    @Test
    public void testLog2_double() {
        assertEquals(0.0, Numbers.log2(1.0), DELTA);
        assertEquals(1.0, Numbers.log2(2.0), DELTA);
        assertEquals(3.0, Numbers.log2(8.0), DELTA);
        assertEquals(-1.0, Numbers.log2(0.5), 0.000001);
        assertEquals(-2.0, Numbers.log2(0.25), 0.000001);
        assertEquals(10.0, Numbers.log2(1024.0), DELTA);
        assertTrue(Double.isNaN(Numbers.log2(-1.0)));
        assertTrue(Double.isNaN(Numbers.log2(Double.NaN)));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.log2(0.0), 0);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.log2(Double.POSITIVE_INFINITY), 0);
    }

    @Test
    public void testLog2_double_RoundingMode() {
        assertEquals(3, Numbers.log2(8.0, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(10.0, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10.0, RoundingMode.CEILING));
        assertTrue(Numbers.log2(Double.MIN_VALUE, RoundingMode.FLOOR) < 0);
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(Double.NaN, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log2(10.0, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testLog2_int() {
        assertEquals(0, Numbers.log2(1, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log2(2, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(8, RoundingMode.UNNECESSARY));
        for (final RoundingMode mode : RoundingMode.values()) {
            assertEquals(3, Numbers.log2(8, mode));
        }
        assertEquals(3, Numbers.log2(10, RoundingMode.DOWN));
        assertEquals(3, Numbers.log2(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10, RoundingMode.UP));
        assertEquals(4, Numbers.log2(10, RoundingMode.CEILING));
        assertEquals(3, Numbers.log2(11, RoundingMode.HALF_DOWN));
        assertEquals(4, Numbers.log2(13, RoundingMode.HALF_DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log2(10, RoundingMode.UNNECESSARY));
    }

    @Test
    public void c035_log2DoubleInexactAtPowersOfTwoButRoundingOverloadIsExact() {
        assertEquals(29.000000000000004, Numbers.log2(0x1p29), 0.0);
        assertEquals(-1066.0000000000002, Numbers.log2(0x1p-1066), 0.0);
        assertEquals(30.0, Math.ceil(Numbers.log2(0x1p29)), 0.0);
        assertEquals(-1067.0, Math.floor(Numbers.log2(0x1p-1066)), 0.0);

        int inexact = 0;

        for (int e = Double.MIN_EXPONENT - 52; e <= Double.MAX_EXPONENT; e++) {
            final double x = Math.scalb(1.0, e);

            if (Numbers.log2(x) != e) {
                inexact++;
            }

            assertEquals(e, Numbers.log2(x, RoundingMode.FLOOR), "floor 2^" + e);
            assertEquals(e, Numbers.log2(x, RoundingMode.CEILING), "ceiling 2^" + e);
            assertEquals(e, Numbers.log2(x, RoundingMode.UNNECESSARY), "unnecessary 2^" + e);
            assertEquals(e, Numbers.log2(x, RoundingMode.HALF_EVEN), "half_even 2^" + e);
        }

        assertTrue(inexact > 0, "log2(double) is exact at every power of two on this JDK; revisit the javadoc");
        assertEquals(3.0, Numbers.log2(8.0), 0.0);
        assertEquals(-1.0, Numbers.log2(0.5), 0.0);
    }

    @Test
    public void testLog2_long() {
        assertEquals(0, Numbers.log2(1L, RoundingMode.UNNECESSARY));
        assertEquals(10, Numbers.log2(1024L, RoundingMode.UNNECESSARY));
        assertEquals(9, Numbers.log2(1000L, RoundingMode.FLOOR));
        assertEquals(60, Numbers.log2(1L << 60, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1L, RoundingMode.FLOOR));
    }

    // log10

    @Test
    public void testLog10_BigInteger() {
        assertEquals(0, Numbers.log10(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log10(BigInteger.TEN, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(BigInteger.valueOf(100), RoundingMode.UNNECESSARY));

        final BigInteger bigVal = BigInteger.TEN.pow(20);
        assertEquals(20, Numbers.log10(bigVal, RoundingMode.DOWN));
        assertEquals(20, Numbers.log10(bigVal.add(BigInteger.ONE), RoundingMode.DOWN));
        assertEquals(21, Numbers.log10(bigVal.add(BigInteger.ONE), RoundingMode.CEILING));

        final BigInteger val2 = BigInteger.TEN.pow(25).subtract(BigInteger.ONE);
        assertEquals(24, Numbers.log10(val2, RoundingMode.DOWN));
        assertEquals(25, Numbers.log10(val2, RoundingMode.CEILING));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(BigInteger.ZERO, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10((BigInteger) null, RoundingMode.DOWN));
    }

    @Test
    public void testLog10_double() {
        assertEquals(0.0, Numbers.log10(1.0), DELTA);
        assertEquals(1.0, Numbers.log10(10.0), DELTA);
        assertEquals(2.0, Numbers.log10(100.0), DELTA);
        assertEquals(-1.0, Numbers.log10(0.1), 0.0001);
        assertTrue(Double.isInfinite(Numbers.log10(0.0)));
        assertTrue(Double.isNaN(Numbers.log10(-1.0)));
    }

    @Test
    public void testLog10_double_MatchesLegacy() {
        final List<Double> samples = new ArrayList<>();
        for (int k = 0; k <= 22; k++) {
            final double power = Math.pow(10, k);
            samples.add(power);
            samples.add(Math.nextUp(power));
            samples.add(Math.nextDown(power));
        }
        samples.add(0.1);
        samples.add(Double.MIN_VALUE);
        samples.add(Double.MAX_VALUE);
        final Random random = new Random(0x10610_6L);
        while (samples.size() < 20_000) {
            final double d = Double.longBitsToDouble(random.nextLong());
            if (d > 0.0 && Double.isFinite(d)) {
                samples.add(d);
            }
        }
        for (final double x : samples) {
            for (final RoundingMode mode : RoundingMode.values()) {
                final int expected;
                try {
                    expected = legacyLog10Rounded(x, mode);
                } catch (final ArithmeticException e) {
                    assertThrows(ArithmeticException.class, () -> Numbers.log10(x, mode), "x=" + x + ", mode=" + mode);
                    continue;
                }
                assertEquals(expected, Numbers.log10(x, mode), "x=" + x + ", mode=" + mode);
            }
        }
    }

    @Test
    public void testLog10_double_RoundingMode() {
        for (final RoundingMode mode : RoundingMode.values()) {
            assertEquals(2, Numbers.log10(100.0, mode));
        }
        assertEquals(2, Numbers.log10(200.0, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(200.0, RoundingMode.CEILING));
        assertEquals(2, Numbers.log10(200.0, RoundingMode.DOWN));
        assertEquals(3, Numbers.log10(200.0, RoundingMode.UP));
        assertEquals(-1, Numbers.log10(0.2, RoundingMode.FLOOR));
        assertEquals(0, Numbers.log10(0.2, RoundingMode.CEILING));
        assertEquals(-1, Numbers.log10(0.1, RoundingMode.FLOOR));
        assertEquals(0, Numbers.log10(0.1, RoundingMode.CEILING));
        assertEquals(-324, Numbers.log10(Double.MIN_VALUE, RoundingMode.FLOOR));
        assertEquals(308, Numbers.log10(Double.MAX_VALUE, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(0.1, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(Double.NaN, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(1.0, null));
    }

    @Test
    public void testLog10_int() {
        assertEquals(0, Numbers.log10(1, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(100, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(100, RoundingMode.DOWN));
        assertEquals(2, Numbers.log10(200, RoundingMode.DOWN));
        assertEquals(3, Numbers.log10(200, RoundingMode.UP));
        assertEquals(1, Numbers.log10(99, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(101, RoundingMode.CEILING));
        assertEquals(2, Numbers.log10(123, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(-1, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(200, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testLog10_long() {
        assertEquals(0, Numbers.log10(1L, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log10(1000L, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log10(1234L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(-1L, RoundingMode.FLOOR));
    }

    // mean

    @Test
    public void testMean_double() {
        assertEquals(3.0, Numbers.mean(1.0, 2.0, 3.0, 4.0, 5.0), DELTA);
        assertEquals(3.5, Numbers.mean(3.0, 4.0), 0.001);
        assertEquals(2.5, Numbers.mean(1.0, 2.0, 3.0, 4.0), 1e-10);
        assertEquals(0.0, Numbers.mean(-1.0, 1.0), 0.0001);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new double[0]));
    }

    @Test
    public void testMean_int() {
        assertEquals(3.0, Numbers.mean(1, 2, 3, 4, 5), DELTA);
        assertEquals(5.0, Numbers.mean(3, 5, 7), 0.001);
        assertEquals(10.0, Numbers.mean(10), 0.001);
        assertEquals(0.0, Numbers.mean(0), DELTA);
        assertEquals(-1.0, Numbers.mean(-2, 0, -1), DELTA);
        assertEquals(0.0, Numbers.mean(-5, 0, 5), 0.0001);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new int[0]));
    }

    @Test
    public void testMean_long() {
        assertEquals(3.0, Numbers.mean(1L, 2L, 3L, 4L, 5L), DELTA);
        assertEquals(5.0, Numbers.mean(3L, 5L, 7L), 0.001);
        assertEquals(Long.MAX_VALUE, Numbers.mean(Long.MAX_VALUE), 1e-10);
        assertEquals(1000000.0, Numbers.mean(999999L, 1000000L, 1000001L), 0.0001);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new long[0]));
    }

    @Test
    public void testMean_NaNAndInfinity() {
        final IllegalArgumentException nan = assertThrows(IllegalArgumentException.class, () -> Numbers.mean(1.0, Double.NaN));
        assertTrue(nan.getMessage().contains("finite"), nan.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.NaN, 1.0, 2.0));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.POSITIVE_INFINITY, 1.0));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    public void testMean_Overflow() {
        assertEquals(Double.MAX_VALUE, Numbers.mean(Double.MAX_VALUE, Double.MAX_VALUE), 0.0);
        assertTrue(Double.isFinite(Numbers.mean(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)));
        assertEquals(-Double.MAX_VALUE, Numbers.mean(-Double.MAX_VALUE, -Double.MAX_VALUE), 0.0);
        assertEquals(0.0, Numbers.mean(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE), 0.0);
        assertEquals(0.2, Numbers.mean(Double.MAX_VALUE, 1.0, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE), 0.0);
        assertEquals(0.0d, Numbers.mean(1e308d, 1e308d, -1e308d, -1e308d));

        final double mean = Numbers.mean(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE);
        assertTrue(Double.isFinite(mean));
        assertEquals(Double.MAX_VALUE / 3.0d, mean, Math.ulp(Double.MAX_VALUE / 3.0d) * 4);
    }

    // sqrt

    @Test
    public void testSqrt2PrecomputedBits() {
        assertEquals(256, Numbers.SQRT2_PRECOMPUTE_THRESHOLD);
        assertEquals(Numbers.SQRT2_PRECOMPUTE_THRESHOLD + 1, Numbers.SQRT2_PRECOMPUTED_BITS.bitLength());
        assertEquals(BigInteger.ONE.shiftLeft(2 * Numbers.SQRT2_PRECOMPUTE_THRESHOLD + 1).sqrt(), Numbers.SQRT2_PRECOMPUTED_BITS);

        for (int k = 0; k < Numbers.SQRT2_PRECOMPUTE_THRESHOLD; k++) {
            assertEquals(BigInteger.ONE.shiftLeft(2 * k + 1).sqrt(), Numbers.SQRT2_PRECOMPUTED_BITS.shiftRight(Numbers.SQRT2_PRECOMPUTE_THRESHOLD - k),
                    "k=" + k);
        }

        for (int k = 1; k < 62; k++) {
            for (final long delta : new long[] { -1, 0, 1 }) {
                final long x = (1L << k) + delta;
                if (x <= 0) {
                    continue;
                }
                for (final RoundingMode m : new RoundingMode[] { RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN }) {
                    assertEquals(Numbers.log2(x, m), Numbers.log2(BigInteger.valueOf(x), m), "x=" + x + " " + m);
                }
            }
        }
    }

    @Test
    public void testSqrt_BigInteger() {
        assertEquals(BigInteger.ZERO, Numbers.sqrt(BigInteger.ZERO, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.ONE, Numbers.sqrt(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(9), RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.TEN, Numbers.sqrt(BigInteger.valueOf(100), RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(100), Numbers.sqrt(BigInteger.valueOf(10000), RoundingMode.UNNECESSARY));
        assertEquals(new BigInteger("1000000000"), Numbers.sqrt(new BigInteger("1000000000000000000"), RoundingMode.FLOOR));

        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(4), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.UP));
        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(10), RoundingMode.HALF_DOWN));
        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(9), RoundingMode.HALF_UP));

        final BigInteger large = new BigInteger("1000000000000000000000000");
        final BigInteger largeSq = large.multiply(large);
        assertEquals(large, Numbers.sqrt(largeSq, RoundingMode.UNNECESSARY));

        final BigInteger next = large.add(BigInteger.ONE);
        final BigInteger lowerHalf = largeSq.add(large);
        final BigInteger upperHalf = lowerHalf.add(BigInteger.ONE);
        assertEquals(large, Numbers.sqrt(upperHalf, RoundingMode.FLOOR));
        assertEquals(large, Numbers.sqrt(upperHalf, RoundingMode.DOWN));
        assertEquals(next, Numbers.sqrt(upperHalf, RoundingMode.CEILING));
        assertEquals(next, Numbers.sqrt(upperHalf, RoundingMode.UP));
        assertEquals(large, Numbers.sqrt(lowerHalf, RoundingMode.HALF_DOWN));
        assertEquals(large, Numbers.sqrt(lowerHalf, RoundingMode.HALF_UP));
        assertEquals(large, Numbers.sqrt(lowerHalf, RoundingMode.HALF_EVEN));
        assertEquals(next, Numbers.sqrt(upperHalf, RoundingMode.HALF_DOWN));
        assertEquals(next, Numbers.sqrt(upperHalf, RoundingMode.HALF_UP));
        assertEquals(next, Numbers.sqrt(upperHalf, RoundingMode.HALF_EVEN));
        assertThrows(ArithmeticException.class, () -> Numbers.sqrt(upperHalf, RoundingMode.UNNECESSARY));

        final BigInteger huge = new BigInteger("123456789012345678901234567890");
        final BigInteger sqrtFloor = Numbers.sqrt(huge, RoundingMode.FLOOR);
        final BigInteger sqrtCeil = Numbers.sqrt(huge, RoundingMode.CEILING);
        assertTrue(sqrtFloor.pow(2).compareTo(huge) <= 0);
        assertTrue(sqrtCeil.pow(2).compareTo(huge) >= 0);

        final BigInteger perfect = BigInteger.valueOf(100).pow(10);
        assertEquals(BigInteger.valueOf(10).pow(10), Numbers.sqrt(perfect, RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(10).pow(10), Numbers.sqrt(perfect, RoundingMode.UNNECESSARY));

        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(BigInteger.valueOf(-1), RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt((BigInteger) null, RoundingMode.DOWN));
    }

    @Test
    public void testSqrt_int() {
        assertEquals(0, Numbers.sqrt(0, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.sqrt(1, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.sqrt(4, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.sqrt(9, RoundingMode.UNNECESSARY));

        assertEquals(1, Numbers.sqrt(2, RoundingMode.DOWN));
        assertEquals(2, Numbers.sqrt(2, RoundingMode.UP));
        assertEquals(1, Numbers.sqrt(2, RoundingMode.FLOOR));
        assertEquals(2, Numbers.sqrt(2, RoundingMode.CEILING));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.DOWN));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.sqrt(10, RoundingMode.UP));
        assertEquals(4, Numbers.sqrt(10, RoundingMode.CEILING));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.HALF_DOWN));
        assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_DOWN));
        assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_UP));
        assertEquals(2, Numbers.sqrt(6, RoundingMode.HALF_EVEN));

        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(-1, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.sqrt(10, RoundingMode.UNNECESSARY));
        assertThrows(ArithmeticException.class, () -> Numbers.sqrt(2, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testSqrt_long() {
        assertEquals(0L, Numbers.sqrt(0L, RoundingMode.UNNECESSARY));
        assertEquals(1L, Numbers.sqrt(1L, RoundingMode.UNNECESSARY));
        assertEquals(3L, Numbers.sqrt(9L, RoundingMode.UNNECESSARY));
        assertEquals(10L, Numbers.sqrt(100L, RoundingMode.UNNECESSARY));
        assertEquals(100000L, Numbers.sqrt(10000000000L, RoundingMode.UNNECESSARY));

        assertEquals(3L, Numbers.sqrt(10L, RoundingMode.DOWN));
        assertEquals(4L, Numbers.sqrt(10L, RoundingMode.UP));
        assertEquals(31L, Numbers.sqrt(1000L, RoundingMode.FLOOR));
        assertEquals(32L, Numbers.sqrt(1000L, RoundingMode.CEILING));
        assertEquals(3L, Numbers.sqrt(10L, RoundingMode.HALF_UP));
        assertEquals(3L, Numbers.sqrt(10L, RoundingMode.HALF_DOWN));
        assertEquals(3L, Numbers.sqrt(10L, RoundingMode.HALF_EVEN));
        assertEquals(7L, Numbers.sqrt(50L, RoundingMode.HALF_DOWN));
        assertEquals(7L, Numbers.sqrt(50L, RoundingMode.HALF_UP));

        assertEquals(316227L, Numbers.sqrt(100000000000L, RoundingMode.DOWN));
        assertEquals(316228L, Numbers.sqrt(100000000000L, RoundingMode.UP));
        assertEquals(316227L, Numbers.sqrt(100000000000L, RoundingMode.FLOOR));
        assertEquals(316228L, Numbers.sqrt(100000000000L, RoundingMode.CEILING));

        final long x = 100000000001L;
        final long sqrtVal = Numbers.sqrt(x, RoundingMode.FLOOR);
        assertTrue(sqrtVal * sqrtVal <= x);

        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(-1L, RoundingMode.FLOOR));
    }
}
