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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class NumbersTest extends NumbersTestSupport {

    @Test
    public void testConstants() {
        assertEquals((byte) 0, Numbers.BYTE_ZERO.byteValue());
        assertEquals((byte) 1, Numbers.BYTE_ONE.byteValue());
        assertEquals((byte) -1, Numbers.BYTE_MINUS_ONE.byteValue());
        assertEquals((short) 0, Numbers.SHORT_ZERO.shortValue());
        assertEquals((short) 1, Numbers.SHORT_ONE.shortValue());
        assertEquals((short) -1, Numbers.SHORT_MINUS_ONE.shortValue());
        assertEquals(0, Numbers.INTEGER_ZERO.intValue());
        assertEquals(1, Numbers.INTEGER_ONE.intValue());
        assertEquals(2, Numbers.INTEGER_TWO.intValue());
        assertEquals(-1, Numbers.INTEGER_MINUS_ONE.intValue());
        assertEquals(0L, Numbers.LONG_ZERO.longValue());
        assertEquals(1L, Numbers.LONG_ONE.longValue());
        assertEquals(-1L, Numbers.LONG_MINUS_ONE.longValue());
        assertEquals(0.0f, Numbers.FLOAT_ZERO, 0.0f);
        assertEquals(1.0f, Numbers.FLOAT_ONE, 0.0f);
        assertEquals(-1.0f, Numbers.FLOAT_MINUS_ONE, 0.0f);
        assertEquals(0.0d, Numbers.DOUBLE_ZERO, 0.0d);
        assertEquals(1.0d, Numbers.DOUBLE_ONE, 0.0d);
        assertEquals(-1.0d, Numbers.DOUBLE_MINUS_ONE, 0.0d);
    }

    @Test
    public void testClamp() {
        assertEquals(5, Numbers.clamp(5, 1, 10));
        assertEquals(1, Numbers.clamp(-3, 1, 10));
        assertEquals(10, Numbers.clamp(42, 1, 10));
        assertEquals(5L, Numbers.clamp(5L, 1L, 10L));
        assertEquals(5.0, Numbers.clamp(5.0, 1.0, 10.0), 0.0);
        assertTrue(Double.isNaN(Numbers.clamp(Double.NaN, 1.0, 10.0)));
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.clamp(-0.0, 0.0, 1.0)));
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.clamp(0.0, -1.0, -0.0)));
        assertThrows(IllegalArgumentException.class, () -> Numbers.clamp(5, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.clamp(5.0, Double.NaN, 10.0));
        assertThrows(IllegalArgumentException.class, () -> Numbers.clamp(0.0, 0.0, -0.0));
    }

    @Test
    public void testFitsInLong() {
        assertTrue(Numbers.fitsInLong(BigInteger.ZERO));
        assertTrue(Numbers.fitsInLong(BigInteger.valueOf(Long.MAX_VALUE)));
        assertTrue(Numbers.fitsInLong(BigInteger.valueOf(Long.MIN_VALUE)));
        assertFalse(Numbers.fitsInLong(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)));
        assertFalse(Numbers.fitsInLong(new BigInteger("123456789012345678901234567890")));
    }

    @Test
    public void testFuzzyCompare() {
        assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.0001f, 0.001f));
        assertTrue(Numbers.fuzzyCompare(1.0f, 2.0f, 0.001f) < 0);
        assertEquals(0, Numbers.fuzzyCompare(1.0, 1.0 + 1e-16, 1e-15));
        assertTrue(Numbers.fuzzyCompare(1.0, 2.0, 0.001) < 0);
    }

    @Test
    public void testFuzzyEquals() {
        assertTrue(Numbers.fuzzyEquals(1.0f, 1.0f, 0.0f));
        assertTrue(Numbers.fuzzyEquals(1.0f, 1.0001f, 0.001f));
        assertFalse(Numbers.fuzzyEquals(1.0f, 2.0f, 0.001f));
        assertTrue(Numbers.fuzzyEquals(Float.NaN, Float.NaN, 0.1f));
        assertTrue(Numbers.fuzzyEquals(1.0, 1.0 + 1e-16, 1e-15));
        assertFalse(Numbers.fuzzyEquals(1.0, 1.0 + 1e-14, 1e-15));
        assertTrue(Numbers.fuzzyEquals(Double.NaN, Double.NaN, 0.1));
        assertTrue(Numbers.fuzzyEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.1));
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

    @Test
    public void testIsMathematicalInteger() {
        assertTrue(Numbers.isMathematicalInteger(0.0));
        assertTrue(Numbers.isMathematicalInteger(-0.0));
        assertTrue(Numbers.isMathematicalInteger(1.0));
        assertTrue(Numbers.isMathematicalInteger(-5.0));
        assertTrue(Numbers.isMathematicalInteger(1e10));
        assertFalse(Numbers.isMathematicalInteger(1.5));
        assertFalse(Numbers.isMathematicalInteger(0.5));
        assertFalse(Numbers.isMathematicalInteger(Math.PI));
        assertFalse(Numbers.isMathematicalInteger(Double.NaN));
        assertFalse(Numbers.isMathematicalInteger(Double.POSITIVE_INFINITY));
        assertFalse(Numbers.isMathematicalInteger(Double.NEGATIVE_INFINITY));

        final double[] samples = { 0.0d, -0.0d, 1.0d, -1.0d, 0.5d, 1e300d, Double.MIN_VALUE, Double.MAX_VALUE, Math.scalb(1.0d, 52),
                Math.scalb(1.0d, 52) + 0.5d, Math.scalb(1.0d, 53), Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
        for (final double x : samples) {
            assertEquals(!Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x), Numbers.isMathematicalInteger(x), Double.toString(x));
        }
    }

    @Test
    public void testIsNormal() {
        assertTrue(Numbers.isNormal(Double.MIN_NORMAL));
        assertTrue(Numbers.isNormal(1.0d));
        assertTrue(Numbers.isNormal(-Double.MAX_VALUE));
        assertFalse(Numbers.isNormal(0.0d));
        assertFalse(Numbers.isNormal(-0.0d));
        assertFalse(Numbers.isNormal(Double.MIN_VALUE));
        assertFalse(Numbers.isNormal(Math.nextDown(Double.MIN_NORMAL)));
        assertFalse(Numbers.isNormal(Double.POSITIVE_INFINITY));
        assertFalse(Numbers.isNormal(Double.NaN));
    }

    // isPerfectSquare

    @Test
    public void testIsPerfectSquare() {
        assertTrue(Numbers.isPerfectSquare(0));
        assertTrue(Numbers.isPerfectSquare(1));
        assertTrue(Numbers.isPerfectSquare(4));
        assertTrue(Numbers.isPerfectSquare(100));
        assertFalse(Numbers.isPerfectSquare(2));
        assertFalse(Numbers.isPerfectSquare(-4));
        assertFalse(Numbers.isPerfectSquare(Integer.MAX_VALUE));

        assertTrue(Numbers.isPerfectSquare(0L));
        assertTrue(Numbers.isPerfectSquare(100000000L * 100000000L));
        assertFalse(Numbers.isPerfectSquare(Long.MAX_VALUE));
        assertFalse(Numbers.isPerfectSquare(Long.MIN_VALUE));

        final long maxSquare = 3037000499L * 3037000499L;
        assertTrue(Numbers.isPerfectSquare(maxSquare));
        assertFalse(Numbers.isPerfectSquare(maxSquare - 1));
    }

    @Test
    public void testIsPerfectSquare_BigInteger() {
        assertTrue(Numbers.isPerfectSquare(BigInteger.ZERO));
        assertTrue(Numbers.isPerfectSquare(BigInteger.ONE));
        assertTrue(Numbers.isPerfectSquare(BigInteger.valueOf(16)));
        assertFalse(Numbers.isPerfectSquare(BigInteger.valueOf(17)));
        assertFalse(Numbers.isPerfectSquare(BigInteger.valueOf(-4)));
        assertThrows(IllegalArgumentException.class, () -> Numbers.isPerfectSquare((BigInteger) null));

        for (long n = 0; n < 2_000; n++) {
            assertEquals(Numbers.isPerfectSquare(n), Numbers.isPerfectSquare(BigInteger.valueOf(n)), Long.toString(n));
        }
        for (long root = 3_037_000_400L; root <= 3_037_000_499L; root++) {
            final BigInteger square = BigInteger.valueOf(root).pow(2);
            assertTrue(Numbers.isPerfectSquare(square), Long.toString(root));
            assertTrue(Numbers.isPerfectSquare(square.longValueExact()), Long.toString(root));
            assertFalse(Numbers.isPerfectSquare(square.add(BigInteger.ONE)));
        }
        final BigInteger root = BigInteger.TEN.pow(50).add(BigInteger.valueOf(7));
        assertTrue(Numbers.isPerfectSquare(root.multiply(root)));
        assertFalse(Numbers.isPerfectSquare(root.multiply(root).add(BigInteger.ONE)));
        assertTrue(Numbers.isPerfectSquare(BigInteger.TEN.pow(100)));
        assertTrue(Numbers.isPerfectSquare(BigInteger.TWO.pow(1000)));
        assertFalse(Numbers.isPerfectSquare(BigInteger.TWO.pow(1001)));
    }

    // G061-01: the mod-16 residue pre-filter must agree with the exact root-and-square test at every magnitude,
    // including multi-word values whose low 32 bits are what the filter reads.
    @Test
    public void testIsPerfectSquare_BigInteger_residueFilterMatchesExactCheck() {
        final java.util.Random random = new java.util.Random(20260926L);
        for (final int bits : new int[] { 1, 31, 32, 33, 63, 64, 65, 127, 200, 1000 }) {
            for (int trial = 0; trial < 20; trial++) {
                final BigInteger root = new BigInteger(bits, random);
                final BigInteger square = root.multiply(root);
                for (int delta = -40; delta <= 40; delta++) {
                    final BigInteger n = square.add(BigInteger.valueOf(delta));
                    final boolean expected = n.signum() >= 0 && n.sqrt().pow(2).equals(n);
                    assertEquals(expected, Numbers.isPerfectSquare(n), n.toString());
                }
            }
        }
        // Low 32 bits all zero / all ones above a large high part.
        assertTrue(Numbers.isPerfectSquare(BigInteger.ONE.shiftLeft(64)));
        assertFalse(Numbers.isPerfectSquare(BigInteger.ONE.shiftLeft(65)));
        assertFalse(Numbers.isPerfectSquare(BigInteger.ONE.shiftLeft(96).subtract(BigInteger.ONE)));
        assertTrue(Numbers.isPerfectSquare(BigInteger.ONE.shiftLeft(48).subtract(BigInteger.ONE).pow(2)));
        assertFalse(Numbers.isPerfectSquare(BigInteger.valueOf(Long.MAX_VALUE).pow(2).negate()));
    }

    @Test
    public void testIsPowerOfTwo() {
        assertTrue(Numbers.isPowerOfTwo(1));
        assertTrue(Numbers.isPowerOfTwo(2));
        assertTrue(Numbers.isPowerOfTwo(1024));
        assertFalse(Numbers.isPowerOfTwo(0));
        assertFalse(Numbers.isPowerOfTwo(3));
        assertFalse(Numbers.isPowerOfTwo(-2));
        assertFalse(Numbers.isPowerOfTwo(Integer.MIN_VALUE));

        assertTrue(Numbers.isPowerOfTwo(1L));
        assertTrue(Numbers.isPowerOfTwo(1L << 30));
        assertTrue(Numbers.isPowerOfTwo(1099511627776L));
        assertFalse(Numbers.isPowerOfTwo(0L));
        assertFalse(Numbers.isPowerOfTwo(Long.MIN_VALUE));

        assertTrue(Numbers.isPowerOfTwo(1.0));
        assertTrue(Numbers.isPowerOfTwo(0.5));
        assertTrue(Numbers.isPowerOfTwo(0.25));
        assertFalse(Numbers.isPowerOfTwo(0.0));
        assertFalse(Numbers.isPowerOfTwo(3.0));
        assertFalse(Numbers.isPowerOfTwo(Double.NaN));
        assertFalse(Numbers.isPowerOfTwo(Double.POSITIVE_INFINITY));

        assertTrue(Numbers.isPowerOfTwo(BigInteger.ONE));
        assertTrue(Numbers.isPowerOfTwo(BigInteger.valueOf(1024)));
        assertFalse(Numbers.isPowerOfTwo(BigInteger.ZERO));
        assertFalse(Numbers.isPowerOfTwo(new BigInteger("3")));
        assertFalse(Numbers.isPowerOfTwo(new BigInteger("-2")));
        assertThrows(IllegalArgumentException.class, () -> Numbers.isPowerOfTwo((BigInteger) null));
    }

    // isPrime

    @Test
    public void testIsPrime() {
        assertFalse(Numbers.isPrime(0));
        assertFalse(Numbers.isPrime(1));
        assertTrue(Numbers.isPrime(2));
        assertTrue(Numbers.isPrime(3));
        assertFalse(Numbers.isPrime(4));
        assertTrue(Numbers.isPrime(97));
        assertTrue(Numbers.isPrime(7919));
        assertFalse(Numbers.isPrime(100));
        assertThrows(IllegalArgumentException.class, () -> Numbers.isPrime(-1));

        for (int i = 0; i <= 1000; i++) {
            assertEquals(Numbers.isPrime((long) i), Numbers.isPrime(i));
        }
        for (int n = 0; n < 50_000; n++) {
            assertEquals(BigInteger.valueOf(n).isProbablePrime(100), Numbers.isPrime(n), "n=" + n);
        }
    }

    @Test
    public void testIsPrime_long() {
        assertFalse(Numbers.isPrime(0L));
        assertFalse(Numbers.isPrime(1L));
        assertTrue(Numbers.isPrime(2L));
        assertTrue(Numbers.isPrime(13L));
        assertFalse(Numbers.isPrime(14L));
        assertFalse(Numbers.isPrime(289L));
        assertTrue(Numbers.isPrime(9007199254740881L));
        assertFalse(Numbers.isPrime(9007199254740880L));
        assertTrue(Numbers.isPrime(3215031767L));
        assertFalse(Numbers.isPrime(3215031751L));
        assertTrue(Numbers.isPrime(999999999999999877L));
        assertFalse(Numbers.isPrime(561));
        assertFalse(Numbers.isPrime(1105));
        assertFalse(Numbers.isPrime(1729));
        assertFalse(Numbers.isPrime(41041));
        assertFalse(Numbers.isPrime(2047));
        for (final long n : new long[] { 2047, 3277, 4033, 8321, 3215031751L, 3825123056546413051L }) {
            assertFalse(Numbers.isPrime(n), "strong pseudoprime must be rejected: " + n);
        }
    }

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

    @Test
    public void testConvertNonFiniteFloatToBigDecimalOverflows() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.NaN, BigDecimal.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.POSITIVE_INFINITY, BigDecimal.class));
    }

    @Test
    public void testToPrimitiveWithDefaultForNull() {
        assertEquals(5L, Numbers.toLong((Object) null, 5L));
        assertEquals(1.5f, Numbers.toFloat((Object) null, 1.5f));
        assertEquals(2.5d, Numbers.toDouble((Object) null, 2.5d));
    }

    @Test
    public void testIsCreatableEdgeTokens() {
        assertFalse(Numbers.isCreatable("1ef"));
        assertFalse(Numbers.isCreatable("1e99999999999f"));
        assertFalse(Numbers.isCreatable(".L"));
        assertFalse(Numbers.isCreatable("."));
        assertTrue(Numbers.isCreatable("1e05"));
        assertEquals(100000.0d, Numbers.createNumber("1e05"));
    }

    @Test
    public void testParseBigDecimalSignOnly() {
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("+"));
    }

    @Test
    public void testErrorMessagePreviewKeepsSurrogatePairs() {
        final String emoji = "😀";
        final String longInput = "x".repeat(63) + emoji + "yyyy";
        final NumberFormatException ex = assertThrows(NumberFormatException.class, () -> Numbers.createNumber(longInput));
        assertTrue(ex.getMessage().contains("x".repeat(63) + "...["), ex.getMessage());
        assertFalse(ex.getMessage().contains("\uD83D..."), ex.getMessage());

        final NumberFormatException ex2 = assertThrows(NumberFormatException.class, () -> Numbers.createNumber("\u0001" + emoji));
        assertTrue(ex2.getMessage().contains("\\u0001" + emoji), ex2.getMessage());
    }

    @Test
    public void testLog2CeilingAndLog10BigIntegerFloor() {
        assertEquals(3, Numbers.log2(5L, RoundingMode.CEILING));
        assertEquals(3, Numbers.log2(5L, RoundingMode.UP));

        for (int k = 19; k <= 120; k++) {
            final BigInteger x = BigInteger.TEN.pow(k).subtract(BigInteger.ONE);
            assertEquals(k - 1, Numbers.log10(x, RoundingMode.FLOOR), "k=" + k);
            assertEquals(k, Numbers.log10(BigInteger.TEN.pow(k), RoundingMode.FLOOR), "k=" + k);
        }
    }

    @Test
    public void testCeilingPowerOfTwoOverflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.ceilingPowerOfTwo((1 << 30) + 1));
        assertEquals(1 << 30, Numbers.ceilingPowerOfTwo(1 << 30));
    }

    @Test
    public void testSqrtLongRoundingCorrections() {
        final long justBelow = (1L << 62) - 1;
        assertEquals((1L << 31) - 1, Numbers.sqrt(justBelow, RoundingMode.FLOOR));
        assertEquals(1L << 31, Numbers.sqrt(justBelow, RoundingMode.CEILING));
        assertEquals(1L << 20, Numbers.sqrt(1L << 40, RoundingMode.CEILING));
        assertEquals(1L << 20, Numbers.sqrt(1L << 40, RoundingMode.UP));
    }

    @Test
    public void testSqrtBigIntegerCeilingOfPerfectSquare() {
        assertEquals(BigInteger.ONE.shiftLeft(50), Numbers.sqrt(BigInteger.ONE.shiftLeft(100), RoundingMode.CEILING));
        assertEquals(BigInteger.ONE.shiftLeft(50).add(BigInteger.ONE), Numbers.sqrt(BigInteger.ONE.shiftLeft(100).add(BigInteger.ONE), RoundingMode.CEILING));
    }

    @Test
    public void testGcdWithMinValueAndZero() {
        assertEquals(4, Numbers.gcd(4, Integer.MIN_VALUE));
        assertEquals(5, Numbers.gcd(5, 0));
        assertEquals(4L, Numbers.gcd(4L, Long.MIN_VALUE));
        assertEquals(6L, Numbers.gcd(0L, 6L));
        assertEquals(6L, Numbers.gcd(6L, 0L));
    }

    @Test
    public void testLcmOverflowFromGcdFailure() {
        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void testSaturatedPowZeroExponent() {
        assertEquals(1, Numbers.saturatedPow(3, 0));
        assertEquals(1L, Numbers.saturatedPow(3L, 0));
    }

    @Test
    public void testClampFloatRejectsNaNBounds() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.clamp(1f, Float.NaN, 2f));
        assertThrows(IllegalArgumentException.class, () -> Numbers.clamp(1f, 0f, Float.NaN));
    }

    @Test
    public void testFactorialToDoubleBeyondMax() {
        assertEquals(Double.POSITIVE_INFINITY, Numbers.factorialToDouble(1000));
    }

    @Test
    public void testRoundIntermediateIntegerAndHalfModes() {
        assertEquals(2.0, Numbers.roundIntermediate(2.0, RoundingMode.UP));
        assertEquals(2.0, Numbers.roundIntermediate(2.3, RoundingMode.HALF_UP));
        assertEquals(3.0, Numbers.roundIntermediate(2.7, RoundingMode.HALF_DOWN));
    }

    @Test
    public void testFuzzyCompareGreaterAndNaN() {
        assertEquals(1, Numbers.fuzzyCompare(2f, 1f, 0.1f));
        assertEquals(1, Numbers.fuzzyCompare(Float.NaN, 1f, 0.1f));
        assertEquals(-1, Numbers.fuzzyCompare(1f, Float.NaN, 0.1f));
        assertEquals(1, Numbers.fuzzyCompare(2.0, 1.0, 0.1));
    }

    @Test
    public void testIsPrimeWhenWitnessBaseIsMultipleOfN() {
        // 188533673 is a prime factor of the Miller-Rabin base 725270293939359937.
        assertTrue(Numbers.isPrime(188533673L));
        assertFalse(Numbers.isPrime(188533673L * 3));
    }
}
