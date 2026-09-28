/*
 * Copyright (C) 2026 HaiYang Li
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Pins for the 2026-09-24 Numbers review fixes (Dates/Numbers ledger, base r9618): C-019, C-020, C-021, C-022,
 * C-032, C-034, C-035, C-037, C-038, C-055, C-059, C-060, C-061, C-065, C-068; cycle 2: C-086, C-087, C-088,
 * C-091, C-115, C-116; cycle 3: C-147, C-148, C-149, C-150, C-151, C-154, C-156.
 */
public class NumbersReviewFixes20260924Test extends TestBase {

    private static final String UNNECESSARY_MESSAGE = "mode was UNNECESSARY, but rounding was necessary";

    // ===== C-019: powExact overflow message no longer depends on which multiplication detects it =====

    @Test
    public void c019_powExactIntOverflowMessageIsUniform() {
        assertEquals("powExact(10, 10) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(10, 10)).getMessage());
        assertEquals("powExact(3, 20) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(3, 20)).getMessage());
        assertEquals("powExact(46340, 3) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(46340, 3)).getMessage());
        assertEquals("powExact(46341, 2) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(46341, 2)).getMessage());
        assertEquals("powExact(-3, 20) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(-3, 20)).getMessage());
        assertEquals("powExact(2, 31) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(2, 31)).getMessage());
        assertEquals("powExact(-2147483648, 2) overflow",
                assertThrows(ArithmeticException.class, () -> Numbers.powExact(Integer.MIN_VALUE, 2)).getMessage());

        // non-overflowing neighbours
        assertEquals(1000000000, Numbers.powExact(10, 9));
        assertEquals(1162261467, Numbers.powExact(3, 19));
        assertEquals(-1162261467, Numbers.powExact(-3, 19));
        assertEquals(2147395600, Numbers.powExact(46340, 2));
        assertEquals(2147395600, Numbers.powExact(-46340, 2));
        assertEquals(1073741824, Numbers.powExact(2, 30));
        assertEquals(Integer.MIN_VALUE, Numbers.powExact(-2, 31));
        assertEquals(Integer.MIN_VALUE, Numbers.powExact(Integer.MIN_VALUE, 1));
        assertEquals(1, Numbers.powExact(Integer.MIN_VALUE, 0));
    }

    @Test
    public void c019_powExactLongOverflowMessageIsUniform() {
        assertEquals("powExact(10, 19) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(10L, 19)).getMessage());
        assertEquals("powExact(3, 40) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(3L, 40)).getMessage());
        assertEquals("powExact(-3, 40) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(-3L, 40)).getMessage());
        assertEquals("powExact(3037000500, 2) overflow",
                assertThrows(ArithmeticException.class, () -> Numbers.powExact(3037000500L, 2)).getMessage());
        assertEquals("powExact(-3037000499, 3) overflow",
                assertThrows(ArithmeticException.class, () -> Numbers.powExact(-3037000499L, 3)).getMessage());
        assertEquals("powExact(2, 63) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(2L, 63)).getMessage());
        assertEquals("powExact(-9223372036854775808, 2) overflow",
                assertThrows(ArithmeticException.class, () -> Numbers.powExact(Long.MIN_VALUE, 2)).getMessage());

        // non-overflowing neighbours
        assertEquals(1000000000000000000L, Numbers.powExact(10L, 18));
        assertEquals(4052555153018976267L, Numbers.powExact(3L, 39));
        assertEquals(-4052555153018976267L, Numbers.powExact(-3L, 39));
        assertEquals(9223372030926249001L, Numbers.powExact(3037000499L, 2));
        assertEquals(9223372030926249001L, Numbers.powExact(-3037000499L, 2));
        assertEquals(Long.MIN_VALUE, Numbers.powExact(-2L, 63));
        assertEquals(Long.MIN_VALUE, Numbers.powExact(Long.MIN_VALUE, 1));
        assertEquals(1L, Numbers.powExact(Long.MIN_VALUE, 0));
    }

    @Test
    public void c019_powExactMatchesBigIntegerOrThrowsDescriptively() {
        for (int b = -100; b <= 100; b++) {
            for (int k = 0; k <= 70; k++) {
                final BigInteger exact = BigInteger.valueOf(b).pow(k);
                final int bb = b;
                final int kk = k;

                if (exact.bitLength() < Integer.SIZE) {
                    assertEquals(exact.intValueExact(), Numbers.powExact(bb, kk), "int " + b + "^" + k);
                } else {
                    assertEquals("powExact(" + b + ", " + k + ") overflow",
                            assertThrows(ArithmeticException.class, () -> Numbers.powExact(bb, kk)).getMessage(), "int " + b + "^" + k);
                }

                if (exact.bitLength() < Long.SIZE) {
                    assertEquals(exact.longValueExact(), Numbers.powExact((long) bb, kk), "long " + b + "^" + k);
                } else {
                    assertEquals("powExact(" + b + ", " + k + ") overflow",
                            assertThrows(ArithmeticException.class, () -> Numbers.powExact((long) bb, kk)).getMessage(), "long " + b + "^" + k);
                }
            }
        }
    }

    // ===== C-020 / C-066: divide(BigInteger, BigInteger, mode) messages =====

    @Test
    public void c020_divideBigIntegerZeroDivisorMessageIsSizeIndependent() {
        final BigInteger huge = BigInteger.TEN.pow(30);

        assertEquals("/ by zero",
                assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.valueOf(7), BigInteger.ZERO, RoundingMode.DOWN)).getMessage());
        assertEquals("/ by zero", assertThrows(ArithmeticException.class, () -> Numbers.divide(huge, BigInteger.ZERO, RoundingMode.DOWN)).getMessage());
        assertEquals("/ by zero",
                assertThrows(ArithmeticException.class, () -> Numbers.divide(huge.negate(), BigInteger.ZERO, RoundingMode.HALF_EVEN)).getMessage());
        assertEquals("/ by zero",
                assertThrows(ArithmeticException.class, () -> Numbers.divide(huge, BigInteger.ZERO, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals("/ by zero",
                assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.ZERO, BigInteger.ZERO, RoundingMode.DOWN)).getMessage());
    }

    @Test
    public void c020_divideBigIntegerUnnecessaryMessageMatchesPrimitiveOverloads() {
        final BigInteger huge = BigInteger.TEN.pow(30);
        final BigInteger three = BigInteger.valueOf(3);

        assertEquals(UNNECESSARY_MESSAGE,
                assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.valueOf(7), three, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE,
                assertThrows(ArithmeticException.class, () -> Numbers.divide(huge.add(BigInteger.ONE), three, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE,
                assertThrows(ArithmeticException.class, () -> Numbers.divide(huge.negate(), BigInteger.valueOf(7), RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(assertThrows(ArithmeticException.class, () -> Numbers.divide(7, 3, RoundingMode.UNNECESSARY)).getMessage(),
                assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.valueOf(7), three, RoundingMode.UNNECESSARY)).getMessage());

        // exact divisions still succeed, compact and huge
        assertEquals(three, Numbers.divide(BigInteger.valueOf(9), three, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.TEN.pow(15), Numbers.divide(huge, BigInteger.TEN.pow(15), RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.TEN.pow(15).negate(), Numbers.divide(huge.negate(), BigInteger.TEN.pow(15), RoundingMode.UNNECESSARY));
        assertEquals(huge.add(BigInteger.ONE).divide(three), Numbers.divide(huge.add(BigInteger.ONE), three, RoundingMode.DOWN));
        assertEquals(huge.add(BigInteger.ONE).divide(three).add(BigInteger.ONE), Numbers.divide(huge.add(BigInteger.ONE), three, RoundingMode.UP));
    }

    // ===== C-021: private UnsignedLongs replaced by Long.remainderUnsigned in the Miller-Rabin tester =====

    @Test
    public void c021_isPrimeLargeValuesAfterUnsignedLongsRemoval() {
        assertFalse(Numbers.isPrime(Long.MAX_VALUE)); // 2^63 - 1 = 7^2 * 73 * 127 * 337 * 92737 * 649657
        assertTrue(Numbers.isPrime(9223372036854775783L)); // largest prime below 2^63
        assertTrue(Numbers.isPrime(Long.MAX_VALUE - 24));
        assertFalse(Numbers.isPrime(Long.MAX_VALUE - 25));
        assertFalse(Numbers.isPrime(3825123056546413051L)); // strong pseudoprime to the first nine prime bases
        assertTrue(Numbers.isPrime(3037000493L)); // largest prime not above FLOOR_SQRT_MAX_LONG (SMALL tester)
        assertTrue(Numbers.isPrime(3037000507L)); // first prime above FLOOR_SQRT_MAX_LONG (LARGE tester)

        final Random random = new Random(20260924L);

        for (int i = 0; i < 3000; i++) {
            final long n = (random.nextLong() >>> 1) | (1L << 62) | 1L; // odd, in [2^62, 2^63)
            assertEquals(BigInteger.valueOf(n).isProbablePrime(100), Numbers.isPrime(n), "n=" + n);
        }

        for (int i = 0; i < 3000; i++) {
            final long n = 3037000499L + 1 + (random.nextLong() >>> 30); // just above the SMALL/LARGE threshold
            assertEquals(BigInteger.valueOf(n).isProbablePrime(100), Numbers.isPrime(n), "n=" + n);
        }
    }

    // ===== C-022 / C-060: index-based zero-significand check in createNumber (behaviour-neutral) =====

    @Test
    public void c022_createNumberZeroSignificandDetection() {
        // Float results
        assertCreated(Float.valueOf(0.0f), "0.0e-99999999999f");
        assertCreated(Float.valueOf(0.0f), "0e400f");
        assertCreated(Float.valueOf(-0.0f), "-0.0f");
        assertCreated(Float.valueOf(0.5f), ".5f");
        assertCreated(Float.valueOf(1.0f), "1.f");
        assertCreated(Float.valueOf(1.0f), "1.0f");
        assertCreated(Float.valueOf(1.0f), "1.0F");
        assertCreated(Float.valueOf(1e-40f), "1e-40f"); // subnormal float, not zero

        // Float underflow/overflow with a non-zero significand falls through to Double
        assertCreated(Double.valueOf(1.0E-50), "1e-50f");
        assertCreated(Double.valueOf(-1.0E-50), "-1e-50f");
        assertCreated(Double.valueOf(1.0E40), "1e40f");

        // Double results
        assertCreated(Double.valueOf(123.0), "123d");
        assertCreated(Double.valueOf(1.0), "1.0D");
        assertCreated(Double.valueOf(100000.0), "1e5D");
        assertCreated(Double.valueOf(0.0), "0.000");
        assertCreated(Double.valueOf(-0.0), "-0.0");
        assertCreated(Double.valueOf(-0.0), "-0e5");
        assertCreated(Double.valueOf(-0.0), "-.0e-3");
        assertCreated(Double.valueOf(0.0), "00.0e1");
        assertCreated(Double.valueOf(0.0), "0.0");
        assertCreated(Double.valueOf(0.0), ".0");
        assertCreated(Double.valueOf(0.0), "0.");
        assertCreated(Double.valueOf(0.0), "0E0");
        assertCreated(Double.valueOf(0.0), "0e-2147483647");
        assertCreated(Double.valueOf(0.0), "0e2147483648");
        assertCreated(Double.valueOf(-0.0), "-0e-99999999999");
        assertCreated(Double.valueOf(1500.0), "1.5e3");
        assertCreated(Double.valueOf(100000.0), "1E5");
        assertCreated(Double.valueOf(4.9E-324), "4.9e-324");
        assertCreated(Double.valueOf(4.9E-324), "3e-324"); // rounds up to Double.MIN_VALUE, not zero
        assertCreated(Double.valueOf(1.7976931348623157E308), "1.7976931348623157e308");

        // Double underflow/overflow with a non-zero significand becomes BigDecimal
        assertCreated(new BigDecimal("1e400"), "1e400f");
        assertCreated(new BigDecimal("1e400"), "1e400d");
        assertCreated(new BigDecimal("1e-400"), "1e-400d");
        assertCreated(new BigDecimal("1e309"), "1e309");
        assertCreated(new BigDecimal("1e-330"), "1e-330");
        assertCreated(new BigDecimal("1e-324"), "1e-324");
        assertCreated(new BigDecimal("0.0000000001e-400"), "0.0000000001e-400");

        // integral tokens are untouched, including hexadecimal ones that happen to contain an 'E'
        assertCreated(Integer.valueOf(123), "123");
        assertCreated(Long.valueOf(123), "123L");
        assertCreated(Long.valueOf(83), "0123L");
        assertCreated(Long.valueOf(1234567890123L), "1234567890123");
        assertCreated(new BigInteger("12345678901234567890"), "12345678901234567890");
        assertCreated(Integer.valueOf(0x1E5), "0x1E5");
        assertCreated(Integer.valueOf(-0x1e5), "-0x1e5");

        // a non-zero significand with an unrepresentable scale is still rejected
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e-99999999999"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e99999999999"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e+"));
        assertTrue(Numbers.tryCreateNumber("1e99999999999").isEmpty());
        assertEquals(Double.valueOf(0.0), Numbers.tryCreateNumber("0e99999999999").get());
    }

    private static void assertCreated(final Number expected, final String token) {
        final Number actual = Numbers.createNumber(token);
        assertEquals(expected.getClass(), actual.getClass(), token);
        assertEquals(expected, actual, token); // Float/Double equals distinguishes -0.0 from 0.0; BigDecimal equals includes scale
        assertTrue(Numbers.isCreatable(token), token);
        assertEquals(expected, Numbers.tryCreateNumber(token).get(), token);
    }

    // ===== C-032: integer to*(String) parsers do not trim whitespace, unlike toFloat/toDouble =====

    @Test
    public void c032_integerToParsersRejectSurroundingWhitespace() {
        assertThrows(NumberFormatException.class, () -> Numbers.toByte(" 12"));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte("12 ", (byte) 0));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort(" 12"));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort(" 12 ", (short) 0));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt(" 12"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("\t12", 0));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong(" 12"));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong("12\n", 0L));

        assertEquals(12.0f, Numbers.toFloat(" 12 "), 0.0f);
        assertEquals(12.0f, Numbers.toFloat(" 12 ", 0.0f), 0.0f);
        assertEquals(12.0d, Numbers.toDouble(" 12 "), 0.0d);
        assertEquals(12.0d, Numbers.toDouble("\t12\n", 0.0d), 0.0d);
    }

    // ===== C-034: decodeInteger/decodeLong report an out-of-range value as NumberFormatException =====

    @Test
    public void c034_decodeOutOfRangeIsNumberFormatException() {
        final NumberFormatException intNfe = assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("2147483648"));
        assertEquals("2147483648 is not a valid Integer.", intNfe.getMessage());
        assertEquals("Integer value is out of range", intNfe.getCause().getMessage());
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("-2147483649"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("0x80000000"));

        final NumberFormatException longNfe = assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9223372036854775808"));
        assertEquals("9223372036854775808 is not a valid Long.", longNfe.getMessage());
        assertEquals("Long value is out of range", longNfe.getCause().getMessage());
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("0x8000000000000000"));

        // the to* family reports the same values as ArithmeticException; decodeBigInteger has no range
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("2147483648"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("9223372036854775808"));
        assertEquals(new BigInteger("9223372036854775808"), Numbers.decodeBigInteger("9223372036854775808"));
    }

    // ===== C-035: log2(double) is inexact at some powers of two; log2(double, mode) is exact at all of them =====

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

    // ===== C-037: binomialToDouble is the correctly rounded double of the exact coefficient =====

    @Test
    public void c037_binomialToDoubleIsCorrectlyRoundedExactValue() {
        for (int n = 0; n <= 200; n++) {
            for (int k = 0; k <= n; k++) {
                final double expected = Numbers.binomialToBigInteger(n, k).doubleValue();
                assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(Numbers.binomialToDouble(n, k)), "C(" + n + "," + k + ")");
            }
        }

        for (final int n : new int[] { 1024, 1028, 1029, 1030, 1031, 1100, 2000 }) {
            for (int k = 0; k <= n; k += 7) {
                final BigInteger exact = Numbers.binomialToBigInteger(n, k);
                final double actual = Numbers.binomialToDouble(n, k);

                if (new BigDecimal(exact).compareTo(new BigDecimal(Double.MAX_VALUE)) > 0) {
                    assertEquals(Double.POSITIVE_INFINITY, actual, 0.0, "C(" + n + "," + k + ")");
                } else {
                    assertEquals(Double.doubleToRawLongBits(exact.doubleValue()), Double.doubleToRawLongBits(actual), "C(" + n + "," + k + ")");
                }
            }
        }

        assertEquals(1.008913445455642E29, Numbers.binomialToDouble(100, 50), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.binomialToDouble(2000, 1000), 0.0);
        assertFalse(Double.isInfinite(Numbers.binomialToDouble(1029, 514)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.binomialToDouble(1030, 515), 0.0);
    }

    // ===== C-038: round scale rule as documented =====

    @Test
    public void c038_roundScaleRuleDouble() {
        // away-from-zero modes: every non-zero x is +-Infinity once scale <= -309
        for (final double x : new double[] { Double.MIN_VALUE, 1.0, 123.456, 1e300, Double.MAX_VALUE }) {
            assertEquals(Double.POSITIVE_INFINITY, Numbers.round(x, -309, RoundingMode.UP), 0.0, "x=" + x);
            assertEquals(Double.POSITIVE_INFINITY, Numbers.round(x, -309, RoundingMode.CEILING), 0.0, "x=" + x);
            assertEquals(Double.NEGATIVE_INFINITY, Numbers.round(-x, -309, RoundingMode.UP), 0.0, "x=" + x);
            assertEquals(Double.NEGATIVE_INFINITY, Numbers.round(-x, -309, RoundingMode.FLOOR), 0.0, "x=" + x);
            assertEquals(Double.POSITIVE_INFINITY, Numbers.round(x, Integer.MIN_VALUE, RoundingMode.UP), 0.0, "x=" + x);

            // toward-zero results and HALF_* are a signed zero there
            for (final RoundingMode mode : new RoundingMode[] { RoundingMode.DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN }) {
                assertEquals(0L, Double.doubleToRawLongBits(Numbers.round(x, -309, mode)), "x=" + x + " " + mode);
                assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-x, -309, mode)), "-x=" + -x + " " + mode);
            }

            assertEquals(0L, Double.doubleToRawLongBits(Numbers.round(x, -309, RoundingMode.FLOOR)), "x=" + x);
            assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-x, -309, RoundingMode.CEILING)), "x=" + x);
        }

        // -308 is not yet universal
        assertEquals(1.0E308, Numbers.round(1.0, -308, RoundingMode.UP), 0.0);
        assertEquals(1.0E308, Numbers.round(Double.MIN_VALUE, -308, RoundingMode.UP), 0.0);

        // small-scale thresholds: unit > |x| (DOWN etc.), unit > 2|x| (HALF_UP)
        assertEquals(0.0, Numbers.round(9.9, -1, RoundingMode.DOWN), 0.0);
        assertEquals(10.0, Numbers.round(10.0, -1, RoundingMode.DOWN), 0.0);
        assertEquals(0.0, Numbers.round(4.9, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(10.0, Numbers.round(5.0, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -1, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -1, RoundingMode.HALF_EVEN), 0.0);
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-9.9, -1, RoundingMode.CEILING)));

        // 2-arg (HALF_UP): far enough is always a signed zero; MAX_VALUE hits Infinity on the way
        assertEquals(0.0, Numbers.round(1.0, -1), 0.0);
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-1.0, -1)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.round(Double.MAX_VALUE, -308), 0.0);
        assertEquals(0.0, Numbers.round(Double.MAX_VALUE, -309), 0.0);
        assertEquals(0.0, Numbers.round(Double.MAX_VALUE, Integer.MIN_VALUE), 0.0);
    }

    @Test
    public void c038_roundScaleRuleFloat() {
        for (final float x : new float[] { Float.MIN_VALUE, 1.0f, 123.456f, 1e30f, Float.MAX_VALUE }) {
            assertEquals(Float.POSITIVE_INFINITY, Numbers.round(x, -39, RoundingMode.UP), 0.0f, "x=" + x);
            assertEquals(Float.POSITIVE_INFINITY, Numbers.round(x, -39, RoundingMode.CEILING), 0.0f, "x=" + x);
            assertEquals(Float.NEGATIVE_INFINITY, Numbers.round(-x, -39, RoundingMode.UP), 0.0f, "x=" + x);
            assertEquals(Float.NEGATIVE_INFINITY, Numbers.round(-x, -39, RoundingMode.FLOOR), 0.0f, "x=" + x);

            for (final RoundingMode mode : new RoundingMode[] { RoundingMode.DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN }) {
                assertEquals(0, Float.floatToRawIntBits(Numbers.round(x, -39, mode)), "x=" + x + " " + mode);
                assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-x, -39, mode)), "-x=" + -x + " " + mode);
            }

            assertEquals(0, Float.floatToRawIntBits(Numbers.round(x, -39, RoundingMode.FLOOR)), "x=" + x);
            assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-x, -39, RoundingMode.CEILING)), "x=" + x);
        }

        assertEquals(1.0E38f, Numbers.round(1.0f, -38, RoundingMode.UP), 0.0f);
        assertEquals(1.0E38f, Numbers.round(Float.MIN_VALUE, -38, RoundingMode.UP), 0.0f);
        assertEquals(0.0f, Numbers.round(1.0f, -1), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, Numbers.round(Float.MAX_VALUE, -35), 0.0f);
        assertEquals(3.0E38f, Numbers.round(Float.MAX_VALUE, -38), 0.0f);
        assertEquals(0.0f, Numbers.round(Float.MAX_VALUE, -39), 0.0f);
    }

    // ===== C-055: notAValidNumber quotes input that starts or ends with whitespace =====

    @Test
    public void c055_notAValidNumberQuotesWhitespaceEdges() {
        assertEquals("\" 1 \" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(" 1 ")).getMessage());
        assertEquals("\"1 \" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1 ")).getMessage());
        assertEquals("\" 1\" is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.toLong(" 1")).getMessage());
        assertEquals("\"\u00A01\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u00A01")).getMessage());
        assertEquals("\"1\u00A0\" is not a valid Short.", assertThrows(NumberFormatException.class, () -> Numbers.toShort("1\u00A0")).getMessage());
        assertEquals("\" \" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(" ")).getMessage());
        assertEquals("\"\\u00091\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\t1")).getMessage());
        assertEquals("\" 1\" is not a valid BigDecimal.", assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal(" 1")).getMessage());
        assertEquals("\" 1\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(" 1")).getMessage());

        // interior whitespace and plain junk stay unquoted (pins NumbersToTest / NumbersTest)
        assertEquals("abc is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc")).getMessage());
        assertEquals("abc is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("abc")).getMessage());
        assertEquals("1 2 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1 2")).getMessage());
        assertEquals("1\u00A02 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u00A02")).getMessage());
        assertEquals("12x is not a valid Byte.", assertThrows(NumberFormatException.class, () -> Numbers.toByte("12x")).getMessage());
    }

    // ===== C-059: float-target BigInteger/BigDecimal converters are Number::floatValue =====

    @Test
    public void c059_floatTargetConvertersFromBigNumbers() {
        assertEquals(Float.valueOf(1.2345679E19f), Numbers.convert(new BigInteger("12345678901234567890"), Float.class));
        assertEquals(Float.valueOf(1.5f), Numbers.convert(new BigDecimal("1.5"), Float.class));
        assertEquals(Float.valueOf(1.5f), Numbers.convert(new BigDecimal("1.5"), float.class));
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), Numbers.convert(new BigDecimal("1e50"), Float.class));
        assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), Numbers.convert(BigInteger.TEN.pow(50).negate(), Float.class));
        assertEquals(Float.valueOf(0.0f), Numbers.convert(new BigDecimal("1e-50"), Float.class));
        assertInstanceOf(Float.class, Numbers.convert(BigInteger.ONE, Float.class));
        assertInstanceOf(Float.class, Numbers.convert(BigDecimal.ONE, float.class));
    }

    // ===== C-061: parseBigDecimal never attaches a message-less cause =====

    @Test
    public void c061_parseBigDecimalDanglingExponentCauseHasMessage() {
        for (final String token : new String[] { "1e", "1e+", "1e-", "1E", "0.5e+", "1.e" }) {
            final NumberFormatException nfe = assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal(token), token);
            assertEquals(token + " is not a valid BigDecimal.", nfe.getMessage(), token);
            assertNotNull(nfe.getCause(), token);
            assertInstanceOf(NumberFormatException.class, nfe.getCause(), token);
            assertEquals("no exponent digits in " + token, nfe.getCause().getMessage(), token);

            for (Throwable t = nfe; t != null; t = t.getCause()) {
                assertNotNull(t.getMessage(), token + " -> " + t.getClass().getName());
            }
        }

        // a JDK cause that does carry a message is kept as is
        final NumberFormatException dot = assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("."));
        assertEquals(". is not a valid BigDecimal.", dot.getMessage());
        assertEquals("No digits found.", dot.getCause().getMessage());
        final NumberFormatException twoPoints = assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1.2.3"));
        assertEquals("Character array contains more than one decimal point.", twoPoints.getCause().getMessage());

        // the quick pre-check path attaches no cause at all
        final NumberFormatException abc = assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("abc"));
        assertEquals("abc is not a valid BigDecimal.", abc.getMessage());
        assertNull(abc.getCause());

        assertEquals(new BigDecimal("1e5"), Numbers.parseBigDecimal("1e5"));
        assertEquals(new BigDecimal("1e+5"), Numbers.parseBigDecimal("1e+5"));
    }

    // ===== C-065: saturatedPow(long, int) limit expression (parenthesised; behaviour-neutral) =====

    @Test
    public void c065_saturatedPowLongLimitSign() {
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(3L, 40));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(-3L, 40));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedPow(-3L, 41));
        assertEquals(-4052555153018976267L, Numbers.saturatedPow(-3L, 39));
        assertEquals(4052555153018976267L, Numbers.saturatedPow(3L, 39));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedPow(-3037000500L, 3));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(-3037000500L, 2));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(Long.MIN_VALUE, 2));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedPow(Long.MIN_VALUE, 3));
    }

    // ===== C-068: round(x, scale, UNNECESSARY) and fuzzyEquals/fuzzyCompare messages =====

    @Test
    public void c068_roundUnnecessaryMessage() {
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159, 2, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159f, 2, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(1.5, 0, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(-0.5f, 0, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(1e300, -301, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(1.0, Integer.MIN_VALUE, RoundingMode.UNNECESSARY)).getMessage());

        // exact at the requested scale: no throw, value unchanged, signed zero kept
        assertEquals(3.14, Numbers.round(3.14, 2, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(3.14f, Numbers.round(3.14f, 2, RoundingMode.UNNECESSARY), 0.0f);
        assertEquals(2.0, Numbers.round(2.0, 0, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(1.5, Numbers.round(1.5, 1, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(1e300, Numbers.round(1e300, -300, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-0.0, 0, RoundingMode.UNNECESSARY)));
        assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-0.0f, 3, RoundingMode.UNNECESSARY)));
        assertTrue(Double.isNaN(Numbers.round(Double.NaN, 0, RoundingMode.UNNECESSARY)));

        // the other modes are unaffected
        assertEquals(3.14, Numbers.round(3.14159, 2, RoundingMode.HALF_UP), 0.0);
        assertEquals(3.15f, Numbers.round(3.14159f, 2, RoundingMode.CEILING), 0.0f);
    }

    @Test
    public void c068_fuzzyToleranceMessageIsTheSharedOne() {
        // The message is deliberately the one Builder's eager tolerance check duplicates (BuilderRegressionTest
        // pins the two equal), so it stays value-free; the round(UNNECESSARY) message above is the class idiom.
        final String shared = "tolerance must be non-negative and not NaN";
        assertEquals(shared, assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyEquals(1.0, 1.0, -0.1)).getMessage());
        assertEquals(shared, assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyEquals(1.0f, 1.0f, -0.1f)).getMessage());
        assertEquals(shared, assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyEquals(1.0, 1.0, Double.NaN)).getMessage());
        assertEquals(shared, assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyEquals(1.0f, 1.0f, Float.NaN)).getMessage());
        assertEquals(shared, assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyEquals(1.0, 1.0, Double.NEGATIVE_INFINITY)).getMessage());
        assertEquals(shared, assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyCompare(1.0, 2.0, -0.1)).getMessage());
        assertEquals(shared, assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyCompare(1.0f, 2.0f, -2.5f)).getMessage());

        // valid tolerances, including the boundaries
        assertTrue(Numbers.fuzzyEquals(1.0, 1.0, 0.0));
        assertTrue(Numbers.fuzzyEquals(1.0, -1.0, Double.POSITIVE_INFINITY));
        assertTrue(Numbers.fuzzyEquals(-0.0f, 0.0f, 0.0f));
        assertEquals(0, Numbers.fuzzyCompare(1.0001, 1.0002, 0.001));
        assertEquals(-1, Numbers.fuzzyCompare(1.0f, 2.0f, 0.5f));
    }

    // ===== C-086: double-target BigInteger/BigDecimal converters are Number::doubleValue =====

    @Test
    public void c086_doubleTargetConvertersFromBigNumbers() {
        assertEquals(Double.valueOf(0x1p70), Numbers.convert(BigInteger.TWO.pow(70), Double.class));
        assertEquals(Double.valueOf(1.5), Numbers.convert(new BigDecimal("1.5"), Double.class));
        assertEquals(Double.valueOf(123.0), Numbers.convert(BigInteger.valueOf(123), double.class));
        assertEquals(Double.valueOf(-0.1), Numbers.convert(new BigDecimal("-0.1"), double.class));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Numbers.convert(new BigDecimal("1e400"), Double.class));
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Numbers.convert(new BigDecimal("-1e400"), double.class));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Numbers.convert(BigInteger.TWO.pow(2000), Double.class));
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Numbers.convert(BigInteger.TWO.pow(2000).negate(), double.class));
        assertEquals(Double.valueOf(0.0), Numbers.convert(new BigDecimal("1e-400"), Double.class));
        assertInstanceOf(Double.class, Numbers.convert(BigInteger.ONE, Double.class));
        assertInstanceOf(Double.class, Numbers.convert(BigDecimal.ONE, double.class));
    }

    // ===== C-087: an unknown Number's BigInteger overflow is reported from the source's own text =====

    private static Number customNumber(final String text, final double d) {
        return new Number() {
            @Override
            public int intValue() {
                return (int) d;
            }

            @Override
            public long longValue() {
                return (long) d;
            }

            @Override
            public float floatValue() {
                return (float) d;
            }

            @Override
            public double doubleValue() {
                return d;
            }

            @Override
            public String toString() {
                return text;
            }
        };
    }

    @Test
    public void c087_unknownNumberBigIntegerOverflowReportsTheSourceText() {
        final Number unknown = customNumber("1e2147483648", Double.POSITIVE_INFINITY);
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(unknown, BigInteger.class));
        assertEquals("BigInteger overflow: 1e2147483648", e.getMessage());
        assertNotNull(e.getCause());
        assertEquals("BigInteger overflow: 1E+2147483648", e.getCause().getMessage());
        assertNotNull(e.getCause().getCause()); // the JDK's own exception stays at the root

        // the same source to an integral target already reported its own text
        assertEquals("byte overflow: 1e2147483648", assertThrows(ArithmeticException.class, () -> Numbers.convert(unknown, byte.class)).getMessage());
        assertEquals("long overflow: 1e2147483648", assertThrows(ArithmeticException.class, () -> Numbers.toLong(unknown)).getMessage());

        // a BigDecimal source is unchanged: it is its own text
        final ArithmeticException direct = assertThrows(ArithmeticException.class,
                () -> Numbers.convert(new BigDecimal("1e2147483648"), BigInteger.class));
        assertEquals("BigInteger overflow: 1E+2147483648", direct.getMessage());
        assertNotNull(direct.getCause());
        assertNull(direct.getCause().getCause());

        // in-range unknown sources still convert through the same path
        assertEquals(BigInteger.valueOf(12), Numbers.convert(customNumber("12.9", 12.9), BigInteger.class));
        assertEquals(BigInteger.TEN.pow(30), Numbers.convert(customNumber("1e30", 1e30), BigInteger.class));
        assertEquals(BigInteger.ZERO, Numbers.convert(customNumber("1e-2147483647", 0.0), BigInteger.class));
        assertEquals(BigInteger.valueOf(-7), Numbers.convert(customNumber("seven", -7.5), BigInteger.class));
    }

    // ===== C-088: toShort(String, short) needs no range check on a cache hit =====

    @Test
    public void c088_toShortCacheHitNeedsNoRangeCheck() {
        assertEquals((short) 9999, Numbers.toShort("9999", (short) 0));
        assertEquals((short) -999, Numbers.toShort("-999", (short) 0));
        assertEquals((short) 0, Numbers.toShort("0", (short) 7));
        assertEquals((short) -1001, Numbers.toShort("-1001", (short) 0)); // five characters: the scan path
        assertEquals((short) 10000, Numbers.toShort("10000", (short) 0));
        assertEquals((short) 32767, Numbers.toShort("32767", (short) 0));
        assertEquals((short) -32768, Numbers.toShort("-32768", (short) 0));
        assertEquals((short) 7, Numbers.toShort("", (short) 7));
        assertEquals((short) 7, Numbers.toShort(null, (short) 7));
        assertEquals("short overflow: 32768", assertThrows(ArithmeticException.class, () -> Numbers.toShort("32768", (short) 0)).getMessage());
        assertEquals("short overflow: -32769", assertThrows(ArithmeticException.class, () -> Numbers.toShort("-32769", (short) 0)).getMessage());
        assertThrows(NumberFormatException.class, () -> Numbers.toShort("12x", (short) 0));

        // toByte keeps its live check on a cache hit
        assertEquals((byte) 127, Numbers.toByte("127", (byte) 0));
        assertEquals("byte overflow: 128", assertThrows(ArithmeticException.class, () -> Numbers.toByte("128", (byte) 0)).getMessage());
        assertEquals("byte overflow: -129", assertThrows(ArithmeticException.class, () -> Numbers.toByte("-129", (byte) 0)).getMessage());
    }

    // ===== C-091: round(x, scale, UNNECESSARY) keeps BigDecimal's exception as the cause =====

    @Test
    public void c091_roundUnnecessaryKeepsTheBigDecimalExceptionAsCause() {
        final ArithmeticException d = assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159, 2, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, d.getMessage());
        assertInstanceOf(ArithmeticException.class, d.getCause());
        assertNotNull(d.getCause().getMessage());

        final ArithmeticException f = assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159f, 2, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, f.getMessage());
        assertInstanceOf(ArithmeticException.class, f.getCause());

        final ArithmeticException clamped = assertThrows(ArithmeticException.class, () -> Numbers.round(1.0, Integer.MIN_VALUE, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, clamped.getMessage());
        assertInstanceOf(ArithmeticException.class, clamped.getCause());

        // the failures detected by this class itself stay cause-less, with the same message
        final ArithmeticException divided = assertThrows(ArithmeticException.class, () -> Numbers.divide(7, 2, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, divided.getMessage());
        assertNull(divided.getCause());

        // exact values and the other modes are unaffected
        assertEquals(3.14, Numbers.round(3.14, 2, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(3.14, Numbers.round(3.14159, 2, RoundingMode.HALF_UP), 0.0);
    }

    // ===== C-115: Unicode format characters are escaped, and quoted at an edge =====

    @Test
    public void c115_formatCharactersAreEscapedAndQuotedAtTheEdges() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uFEFF123"));
        assertEquals("\"\\uFEFF123\" is not a valid Integer.", e.getMessage());
        assertEquals("invalid character '\\uFEFF' at index 0 of \\uFEFF123", e.getCause().getMessage());

        e = assertThrows(NumberFormatException.class, () -> Numbers.parseDouble("\uFEFF123"));
        assertEquals("\"\\uFEFF123\" is not a valid Double.", e.getMessage());
        assertEquals("For input string: \"\\uFEFF123\"", e.getCause().getMessage());

        e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u202E2"));
        assertEquals("1\\u202E2 is not a valid Integer.", e.getMessage());
        assertEquals("invalid character '\\u202E' at index 1 of 1\\u202E2", e.getCause().getMessage());

        assertEquals("\"123\\uFEFF\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("123\uFEFF")).getMessage());
        assertEquals("\"\\u200B1\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u200B1")).getMessage());
        assertEquals("\"1\\u00AD\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u00AD")).getMessage());
        assertEquals("1\\u200B2 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u200B2")).getMessage());
        assertEquals("\"\\uFEFF1\" is not a valid BigDecimal.", assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("\uFEFF1")).getMessage());
        assertEquals("\"\\uFEFF1\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("\uFEFF1")).getMessage());
        assertEquals("\"\\uFEFF1\" is not a valid Number.", assertThrows(NumberFormatException.class, () -> Numbers.createNumber("\uFEFF1")).getMessage());
        assertEquals("\"\\u20661\\u2069\" is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.toLong("\u20661\u2069")).getMessage());

        // a supplementary format character (a tag) is escaped as its two units; an emoji still passes through raw
        assertEquals("1\\uDB40\\uDC011 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\uDB40\uDC011")).getMessage());
        assertEquals("1\uD83D\uDE002 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\uD83D\uDE002")).getMessage());

        // a non-breaking space (a space separator, not a format character) and plain whitespace are unchanged
        assertEquals("\"\u00A01\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u00A01")).getMessage());
        assertEquals("\" 1\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(" 1")).getMessage());
        assertEquals("1\u00A02 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u00A02")).getMessage());
        assertEquals("\"\\u00091\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\t1")).getMessage());

        // no message in the retained chain carries a raw format character
        for (final String token : new String[] { "\uFEFF123", "1\u202E2", "\u200B", "\u00AD1", "12\u2069", "1.5e\u200D3" }) {
            for (Throwable t = assertThrows(NumberFormatException.class, () -> Numbers.createNumber(token), token); t != null; t = t.getCause()) {
                final String message = t.getMessage();

                for (int i = 0; i < message.length(); i++) {
                    assertTrue(Character.getType(message.charAt(i)) != Character.FORMAT, token + ": " + message);
                }
            }
        }
    }

    // ===== C-116: an "invalid character" cause names the whole code point =====

    @Test
    public void c116_invalidCharacterCauseNamesTheWholeCodePoint() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.createNumber("\uD83D\uDE001"));
        assertEquals("\uD83D\uDE001 is not a valid Number.", e.getMessage());
        assertEquals("invalid character '\uD83D\uDE00' at index 0 of \uD83D\uDE001", e.getCause().getMessage());

        e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uD83D\uDE001"));
        assertEquals("\uD83D\uDE001 is not a valid Integer.", e.getMessage());
        assertEquals("invalid character '\uD83D\uDE00' at index 0 of \uD83D\uDE001", e.getCause().getMessage());

        e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\uD83D\uDE00"));
        assertEquals("invalid character '\uD83D\uDE00' at index 1 of 1\uD83D\uDE00", e.getCause().getMessage());
        assertEquals("invalid character '\uD83D\uDE00' at index 0 of \uD83D\uDE00",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("\uD83D\uDE00")).getCause().getMessage());
        assertEquals("invalid character '\uD83D\uDE00' at index 2 of 12\uD83D\uDE00",
                assertThrows(NumberFormatException.class, () -> Numbers.createNumber("12\uD83D\uDE00")).getCause().getMessage());

        // a lone surrogate is still escaped
        e = assertThrows(NumberFormatException.class, () -> Numbers.createNumber("\uD83D1"));
        assertEquals("\\uD83D1 is not a valid Number.", e.getMessage());
        assertEquals("invalid character '\\uD83D' at index 0 of \\uD83D1", e.getCause().getMessage());
        e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\uDE00"));
        assertEquals("invalid character '\\uDE00' at index 1 of 1\\uDE00", e.getCause().getMessage());

        // an ordinary invalid character is unchanged
        assertEquals("invalid character 'x' at index 2 of 12x", assertThrows(NumberFormatException.class, () -> Numbers.toByte("12x")).getCause().getMessage());
        assertEquals("invalid character 'x' at index 2 of 12x", assertThrows(NumberFormatException.class, () -> Numbers.createNumber("12x")).getCause().getMessage());
    }

    // ===== C-147: a supplementary format character at an edge is quoted like a BMP one (code-point edge test) =====

    @Test
    public void c147_supplementaryFormatCharacterAtAnEdgeIsQuoted() {
        final String tag = "\uDB40\uDC01"; // U+E0001 LANGUAGE TAG: a format character outside the BMP
        final String beam = "\uD834\uDD73"; // U+1D173 MUSICAL SYMBOL BEGIN BEAM: likewise

        final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.toInt(tag + "5"));
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Integer.", e.getMessage());
        assertEquals("invalid character '\\uDB40\\uDC01' at index 0 of \\uDB40\\uDC015", e.getCause().getMessage());
        assertEquals("\"5\\uDB40\\uDC01\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5" + tag)).getMessage());
        assertEquals("\"\\uDB40\\uDC01\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(tag)).getMessage());
        assertEquals("\"\\uDB40\\uDC015\\uDB40\\uDC01\" is not a valid Integer.",
                assertThrows(NumberFormatException.class, () -> Numbers.toInt(tag + "5" + tag)).getMessage());
        assertEquals("\"\\uD834\\uDD735\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(beam + "5")).getMessage());
        assertEquals("\"5\\uD834\\uDD73\" is not a valid Short.", assertThrows(NumberFormatException.class, () -> Numbers.toShort("5" + beam)).getMessage());
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.toLong(tag + "5")).getMessage());
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Integer.",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(tag + "5")).getMessage());
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Number.", assertThrows(NumberFormatException.class, () -> Numbers.createNumber(tag + "5")).getMessage());
        assertEquals("\"\\uDB40\\uDC015\" is not a valid BigDecimal.",
                assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal(tag + "5")).getMessage());
        final NumberFormatException f = assertThrows(NumberFormatException.class, () -> Numbers.toFloat(tag + "5"));
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Float.", f.getMessage());
        assertEquals("For input string: \"\\uDB40\\uDC015\"", f.getCause().getMessage());
        assertEquals("\"5\\uDB40\\uDC01\" is not a valid Double.", assertThrows(NumberFormatException.class, () -> Numbers.toDouble("5" + tag)).getMessage());

        // exactly as a BMP format character at an edge (C-115), which is unchanged
        assertEquals("\"\\uFEFF5\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uFEFF5")).getMessage());
        assertEquals("\"5\\u200B\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\u200B")).getMessage());

        // inside the token the character is escaped but nothing is quoted (C-115 pins the same for the tag)
        assertEquals("1\\uDB40\\uDC012 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1" + tag + "2")).getMessage());
        assertEquals("1\\uD834\\uDD732 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1" + beam + "2")).getMessage());

        // a printable supplementary character at an edge (an emoji) is neither escaped nor quoted
        assertEquals("\uD83D\uDE005 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uD83D\uDE005")).getMessage());
        assertEquals("5\uD83D\uDE00 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\uD83D\uDE00")).getMessage());

        // an unpaired surrogate at an edge is escaped but not quoted: it is not a format character
        assertEquals("\\uD83D5 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uD83D5")).getMessage());
        assertEquals("5\\uDE00 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\uDE00")).getMessage());
        assertEquals("\\uDC005 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uDC005")).getMessage());
        assertEquals("5\\uD83D is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\uD83D")).getMessage());

        // invisible characters that are not format characters pass through raw and unquoted, as documented
        assertEquals("\u31645 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u31645")).getMessage()); // Hangul filler, Lo
        assertEquals("5\uFE0F is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\uFE0F")).getMessage()); // VS16, Mn
        assertEquals("\u034F5 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u034F5")).getMessage()); // CGJ, Mn
        assertEquals("\uDB40\uDD005 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uDB40\uDD005")).getMessage()); // U+E0100, Mn

        // whitespace edges (C-055) are unchanged
        assertEquals("\" 5\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(" 5")).getMessage());
        assertEquals("\"\\u00095\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\t5")).getMessage());
    }

    // ===== C-148: a string-routed conversion previews stringFormForTargetParser's never-null text =====

    private static Number renderedAs(final String text, final double value) {
        return new Number() {
            private static final long serialVersionUID = 1L;

            @Override
            public int intValue() {
                return (int) value;
            }

            @Override
            public long longValue() {
                return (long) value;
            }

            @Override
            public float floatValue() {
                return (float) value;
            }

            @Override
            public double doubleValue() {
                return value;
            }

            @Override
            public String toString() {
                return text;
            }
        };
    }

    @Test
    public void c148_stringRoutedConversionPreviewsTheRenderingHandedToTheParser() {
        // A source with no string form (null or empty toString) is rendered as its double value; that rendering,
        // never null, is what the failing target parser's message previews, on both overloads.
        for (final String rendering : new String[] { null, "" }) {
            NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.convert(renderedAs(rendering, 1.5), AtomicInteger.class));
            assertEquals("1.5 is not a valid AtomicInteger.", e.getMessage());
            assertEquals("1.5 is not a valid Integer.", e.getCause().getMessage());

            e = assertThrows(NumberFormatException.class, () -> Numbers.convert(renderedAs(rendering, 1.5), N.typeOf(AtomicInteger.class)));
            assertEquals("1.5 is not a valid AtomicInteger.", e.getMessage());
            assertEquals("1.5 is not a valid Integer.", e.getCause().getMessage());
        }

        // an ordinary rendering is previewed as is, and a parseable one converts
        assertEquals("2.5 is not a valid AtomicInteger.",
                assertThrows(NumberFormatException.class, () -> Numbers.convert(renderedAs("2.5", 99), AtomicInteger.class)).getMessage());
        assertEquals(7, Numbers.convert(renderedAs("7", 0), AtomicInteger.class).get());
        assertEquals(7, Numbers.convert(renderedAs("7", 0), N.typeOf(AtomicInteger.class)).get());
    }

    // ===== C-149: the slice-based over-length failure uses the one notAValidNumber message template =====

    @Test
    public void c149_overLengthTokenMessageIsIdenticalOnTheWholeStringAndSlicePaths() {
        final int limit = Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH;
        final String token = "1".repeat(limit + 1);
        final NumberFormatException whole = assertThrows(NumberFormatException.class, () -> Numbers.toDouble(token));
        final NumberFormatException sliced = assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDouble("id=" + token + ";"));
        final NumberFormatException slicedOrElse = assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDoubleOrElse("id=" + token + ";", -1.0));

        assertEquals("1".repeat(64) + "...[" + (limit + 1) + " chars] is not a valid Double.", whole.getMessage());
        assertEquals(whole.getMessage(), sliced.getMessage());
        assertEquals(whole.getMessage(), slicedOrElse.getMessage());
        assertEquals("Double input length " + (limit + 1) + " exceeds limit " + limit, whole.getCause().getMessage());
        assertEquals(whole.getCause().getMessage(), sliced.getCause().getMessage());
        assertEquals(whole.getCause().getMessage(), slicedOrElse.getCause().getMessage());

        // the slice previews the token only, never its surroundings
        final String signed = "-1." + "3".repeat(5000);
        assertEquals("-1." + "3".repeat(61) + "...[5003 chars] is not a valid Double.",
                assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDouble("x " + signed + " tail")).getMessage());

        // a token at the limit parses on both paths
        final String atLimit = "1".repeat(limit);
        assertEquals(Numbers.toDouble(atLimit), Numbers.extractFirstDouble("id=" + atLimit).getAsDouble(), 0.0);
    }

    // ===== C-150: primitive and wrapper targets share one converter entry (the aliasing map is Map.of) =====

    private static void assertSameConverter(final Class<? extends Number> primitive, final Class<? extends Number> wrapper) {
        final Number[] sources = { (byte) 7, (short) 7, 7, 7L, 7.0f, 7.0, BigInteger.valueOf(7), new BigDecimal("7") };

        for (final Number source : sources) {
            final Number viaPrimitive = Numbers.convert(source, primitive);
            final Number viaWrapper = Numbers.convert(source, wrapper);
            assertEquals(wrapper, viaPrimitive.getClass(), primitive + " from " + source.getClass().getSimpleName());
            assertEquals(viaWrapper, viaPrimitive, primitive + " from " + source.getClass().getSimpleName());
            assertEquals(viaWrapper, Numbers.convert(source, primitive, null), primitive + " (default overload)");
        }
    }

    @Test
    public void c150_primitiveAndWrapperTargetsShareOneConverterEntry() {
        assertSameConverter(byte.class, Byte.class);
        assertSameConverter(short.class, Short.class);
        assertSameConverter(int.class, Integer.class);
        assertSameConverter(long.class, Long.class);
        assertSameConverter(float.class, Float.class);
        assertSameConverter(double.class, Double.class);

        // the aliasing covers the overflow check on both spellings of the target
        assertThrows(ArithmeticException.class, () -> Numbers.convert(1000, byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(1000, Byte.class));
        assertEquals(Byte.valueOf((byte) 7), Numbers.convert(7.9, byte.class));
    }

    // ===== C-151: every integer-token scanner reads the sign and radix prefix through one helper =====

    /**
     * Independent spelling of the sign + radix-prefix grammar: an optional sign, then {@code 0x}/{@code 0X}/{@code #}
     * for radix 16, a leading zero followed by another character for radix 8 when {@code octal} is set, ASCII
     * digits of that radix, and, when {@code stripLongSuffix} is set, an optional trailing {@code L}/{@code l}.
     * Returns {@code null} for a malformed token.
     */
    private static BigInteger referenceIntegerToken(final String s, final boolean octal, final boolean stripLongSuffix) {
        int end = s.length();

        if (stripLongSuffix && end > 1 && (s.charAt(end - 1) == 'L' || s.charAt(end - 1) == 'l')) {
            end--;
        }

        if (end == 0) {
            return null;
        }

        int pos = 0;
        boolean negate = false;

        if (s.charAt(0) == '-') {
            negate = true;
            pos++;
        } else if (s.charAt(0) == '+') {
            pos++;
        }

        int radix = 10;

        if (pos + 1 < end && s.charAt(pos) == '0' && (s.charAt(pos + 1) == 'x' || s.charAt(pos + 1) == 'X')) {
            radix = 16;
            pos += 2;
        } else if (pos < end && s.charAt(pos) == '#') {
            radix = 16;
            pos++;
        } else if (octal && pos + 1 < end && s.charAt(pos) == '0') {
            radix = 8;
            pos++;
        }

        if (pos >= end) {
            return null;
        }

        for (int i = pos; i < end; i++) {
            final char ch = s.charAt(i);

            if (ch > 127 || Character.digit(ch, radix) < 0) {
                return null;
            }
        }

        final BigInteger magnitude = new BigInteger(s.substring(pos, end), radix);
        return negate ? magnitude.negate() : magnitude;
    }

    private static void assertScannersAgree(final String token) {
        final BigInteger decode = referenceIntegerToken(token, true, false); // decodeInteger, decodeBigInteger
        final BigInteger decodeWithSuffix = referenceIntegerToken(token, true, true); // decodeLong
        final BigInteger decimalFirst = referenceIntegerToken(token, false, true); // toInt, toLong, tryParseInt, tryParseLong

        if (decode == null) {
            assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger(token), token);
            assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(token), token);
        } else {
            assertEquals(decode, Numbers.decodeBigInteger(token), token);

            if (decode.bitLength() > 31) {
                assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(token), token);
            } else {
                assertEquals(decode.intValue(), Numbers.decodeInteger(token).intValue(), token);
            }
        }

        if (decodeWithSuffix == null || decodeWithSuffix.bitLength() > 63) {
            assertThrows(NumberFormatException.class, () -> Numbers.decodeLong(token), token);
        } else {
            assertEquals(decodeWithSuffix.longValue(), Numbers.decodeLong(token).longValue(), token);
        }

        if (decimalFirst == null) {
            assertThrows(NumberFormatException.class, () -> Numbers.toInt(token), token);
            assertThrows(NumberFormatException.class, () -> Numbers.toLong(token), token);
            assertFalse(Numbers.tryParseInt(token).isPresent(), token);
            assertFalse(Numbers.tryParseLong(token).isPresent(), token);
        } else {
            if (decimalFirst.bitLength() > 31) {
                assertThrows(ArithmeticException.class, () -> Numbers.toInt(token), token);
                assertFalse(Numbers.tryParseInt(token).isPresent(), token);
            } else {
                assertEquals(decimalFirst.intValue(), Numbers.toInt(token), token);
                assertEquals(decimalFirst.intValue(), Numbers.tryParseInt(token).getAsInt(), token);
            }

            if (decimalFirst.bitLength() > 63) {
                assertThrows(ArithmeticException.class, () -> Numbers.toLong(token), token);
                assertFalse(Numbers.tryParseLong(token).isPresent(), token);
            } else {
                assertEquals(decimalFirst.longValue(), Numbers.toLong(token), token);
                assertEquals(decimalFirst.longValue(), Numbers.tryParseLong(token).getAsLong(), token);
            }
        }
    }

    @Test
    public void c151_everyScannerAgreesWithTheReferenceGrammarOverTheSignPrefixMatrix() {
        final String[] signs = { "", "+", "-" };
        final String[] prefixes = { "", "0x", "0X", "#", "0", "00", "0x0", "-", "+" };
        final String[] zeros = { "", "0", "000" };
        final String[] magnitudes = { "", "1", "7", "8", "9", "a", "f", "F", "g", "x", "10", "77", "80", "ff", "127", "128", "255", "32767", "32768", "7fff",
                "8000", "2147483647", "2147483648", "7fffffff", "80000000", "17777777777", "20000000000", "9223372036854775807", "9223372036854775808",
                "9223372036854775809", "7fffffffffffffff", "8000000000000000", "8000000000000001", "777777777777777777777", "1000000000000000000000",
                "1000000000000000000001", "99999999999999999999", "1-", "1+1" };
        final String[] suffixes = { "", "L", "l", "x", " " };
        int checked = 0;

        for (final String sign : signs) {
            for (final String prefix : prefixes) {
                for (final String zero : zeros) {
                    for (final String magnitude : magnitudes) {
                        for (final String suffix : suffixes) {
                            final String token = sign + prefix + zero + magnitude + suffix;

                            if (!token.isEmpty()) {
                                assertScannersAgree(token);
                                checked++;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(checked > 15_000, "tokens checked: " + checked);

        // the one lexical collision, Long.MIN_VALUE, in every radix and spelling
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-9223372036854775808"));
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-0x8000000000000000"));
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-#0000008000000000000000L"));
        assertEquals(Long.MIN_VALUE, Numbers.decodeLong("-01000000000000000000000"));
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE), Numbers.decodeBigInteger("-0X8000000000000000"));
        assertEquals(Long.MIN_VALUE, Numbers.tryParseLong("-9223372036854775808").getAsLong());
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("-01000000000000000000000")); // decimal-first: -10^21
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("+9223372036854775808"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9223372036854775808"));
    }

    // ===== C-154: divide(BigInteger, BigInteger, UNNECESSARY) keeps BigDecimal's exception as the cause =====

    @Test
    public void c154_divideBigIntegerUnnecessaryKeepsTheBigDecimalExceptionAsCause() {
        final BigInteger three = BigInteger.valueOf(3);
        final ArithmeticException small = assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.valueOf(7), three, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, small.getMessage());
        assertInstanceOf(ArithmeticException.class, small.getCause());
        assertEquals("Rounding necessary", small.getCause().getMessage());

        final ArithmeticException huge = assertThrows(ArithmeticException.class,
                () -> Numbers.divide(BigInteger.TEN.pow(30).add(BigInteger.ONE), three, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, huge.getMessage());
        assertEquals("Rounding necessary", huge.getCause().getMessage());

        final ArithmeticException negative = assertThrows(ArithmeticException.class,
                () -> Numbers.divide(BigInteger.valueOf(-7), BigInteger.TWO, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, negative.getMessage());
        // the same cause shape round(x, scale, UNNECESSARY) carries (C-091)
        assertEquals(assertThrows(ArithmeticException.class, () -> Numbers.round(1.25, 1, RoundingMode.UNNECESSARY)).getCause().getMessage(),
                negative.getCause().getMessage());

        // the primitive overloads detect the inexact quotient themselves and stay cause-less (C-091)
        assertNull(assertThrows(ArithmeticException.class, () -> Numbers.divide(7, 3, RoundingMode.UNNECESSARY)).getCause());
        assertNull(assertThrows(ArithmeticException.class, () -> Numbers.divide(7L, 3L, RoundingMode.UNNECESSARY)).getCause());

        // a zero divisor, exact quotients and the other modes are unchanged
        assertEquals("/ by zero",
                assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.ONE, BigInteger.ZERO, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(BigInteger.valueOf(-3), Numbers.divide(BigInteger.valueOf(-9), three, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.TEN.pow(15), Numbers.divide(BigInteger.TEN.pow(30), BigInteger.TEN.pow(15), RoundingMode.UNNECESSARY));
        assertEquals(three, Numbers.divide(BigInteger.valueOf(8), three, RoundingMode.HALF_UP));
        assertEquals(BigInteger.TWO, Numbers.divide(BigInteger.valueOf(8), three, RoundingMode.DOWN));
    }

    // ===== C-156: HALF_DOWN/HALF_EVEN are already zero when the rounding unit equals 2|x| (doc wording pin) =====

    @Test
    public void c156_halfDownAndHalfEvenAreZeroWhenTheUnitEqualsTwiceTheValue() {
        // unit 10 == 2|x| for x = 5: HALF_UP rounds away; HALF_DOWN and HALF_EVEN are already a signed zero
        assertEquals(10.0, Numbers.round(5.0, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -1, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -1, RoundingMode.HALF_EVEN), 0.0);
        assertEquals(-10.0, Numbers.round(-5.0, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.round(-5.0, -1, RoundingMode.HALF_DOWN)));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.round(-5.0, -1, RoundingMode.HALF_EVEN)));

        // unit 1 == 2|x| for x = 0.5
        assertEquals(1.0, Numbers.round(0.5, 0, RoundingMode.HALF_UP), 0.0);
        assertEquals(0.0, Numbers.round(0.5, 0, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(0.0, Numbers.round(0.5, 0, RoundingMode.HALF_EVEN), 0.0);

        // above the tie every HALF_* mode is still non-zero; once the unit exceeds 2|x| all of them are zero
        assertEquals(10.0, Numbers.round(5.1, -1, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(10.0, Numbers.round(5.1, -1, RoundingMode.HALF_EVEN), 0.0);
        assertEquals(0.0, Numbers.round(4.9, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -2, RoundingMode.HALF_UP), 0.0);

        // float likewise
        assertEquals(10.0f, Numbers.round(5.0f, -1, RoundingMode.HALF_UP), 0.0f);
        assertEquals(0.0f, Numbers.round(5.0f, -1, RoundingMode.HALF_DOWN), 0.0f);
        assertEquals(0.0f, Numbers.round(5.0f, -1, RoundingMode.HALF_EVEN), 0.0f);
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(Numbers.round(-5.0f, -1, RoundingMode.HALF_EVEN)));
        assertEquals(1.0f, Numbers.round(0.5f, 0, RoundingMode.HALF_UP), 0.0f);
        assertEquals(0.0f, Numbers.round(0.5f, 0, RoundingMode.HALF_EVEN), 0.0f);
    }
}
