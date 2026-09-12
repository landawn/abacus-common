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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;

import org.junit.jupiter.api.Test;

public class NumbersIsTest extends NumbersTestSupport {

    // ===== isParsable =====

    @Test
    public void testIsParsable() {
        assertTrue(Numbers.isParsable("123"));
        assertTrue(Numbers.isParsable("-123"));
        assertTrue(Numbers.isParsable("123.45"));
        assertTrue(Numbers.isParsable(".5"));
        assertTrue(Numbers.isParsable("0.5"));
        assertTrue(Numbers.isParsable("1.2e3"));
        assertTrue(Numbers.isParsable("123."));
        assertTrue(Numbers.isParsable("0."));
        assertTrue(Numbers.isParsable("1.5f"));
        assertTrue(Numbers.isParsable("NaN"));
        assertTrue(Numbers.isParsable("Infinity"));
        assertTrue(Numbers.isParsable("0x1.0p2"));
        assertTrue(Numbers.isParsable(" 123.45 "));

        assertFalse(Numbers.isParsable("0xFF"));
        assertFalse(Numbers.isParsable("123L"));
        assertFalse(Numbers.isParsable("abc"));
        assertFalse(Numbers.isParsable(""));
        assertFalse(Numbers.isParsable(null));
        assertFalse(Numbers.isParsable("1.2.3"));
        assertFalse(Numbers.isParsable("+"));
        assertFalse(Numbers.isParsable("-"));
        assertFalse(Numbers.isParsable("."));
        assertFalse(Numbers.isParsable("   "));
    }

    @Test
    public void testIsParsable_matchesParseUnion() {
        assertTrue(Numbers.isParsable("1.5f"));
        assertNotNull(Numbers.parseFloat("1.5f"));
        assertNotNull(Numbers.parseDouble("1.5f"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1.5f"));

        assertTrue(Numbers.isParsable("NaN"));
        assertTrue(Numbers.parseFloat("NaN").isNaN());
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("NaN"));

        assertTrue(Numbers.isParsable("0x1.0p2"));
        assertEquals(4.0f, Numbers.parseFloat("0x1.0p2"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("0x1.0p2"));

        assertTrue(Numbers.isParsable("Infinity"));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.parseFloat("Infinity"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("Infinity"));

        assertTrue(Numbers.isParsable(" 123.45 "));
        assertEquals(Float.valueOf(123.45f), Numbers.parseFloat(" 123.45 "));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal(" 123.45 "));

        assertTrue(Numbers.isParsable("1e2147483649"));
        assertTrue(Numbers.parseFloat("1e2147483649").isInfinite());
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1e2147483649"));

        assertFalse(Numbers.isParsable("0xFF"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("0xFF"));
        assertFalse(Numbers.isParsable("123L"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("123L"));

        assertNull(Numbers.parseFloat(null));
        assertNull(Numbers.parseDouble(null));
        assertNull(Numbers.parseBigDecimal(null));
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("   "));
    }

    @Test
    public void testIsParsable_AsciiDigitsOnly() {
        assertTrue(Numbers.isParsable("123"));
        assertTrue(Numbers.isParsable("123.45"));
        assertFalse(Numbers.isParsable("١٢٣"));
        assertFalse(Numbers.isParsable("١.٥"));
        assertFalse(Numbers.isParsable("१२३"));
        assertFalse(Numbers.isParsable("１２３"));
        assertThrows(NumberFormatException.class, () -> Float.parseFloat("١٢٣"));

        for (final String value : new String[] { "1e39", "1e-50", "3.4028236e38f", "0x1.fffffep127", "NaN", "-Infinity" }) {
            assertDoesNotThrow(() -> Numbers.parseFloat(value), value);
            assertTrue(Numbers.isParsable(value), value);
        }
        for (final String value : new String[] { "123", "+.5", "123.", "1e309", "1e2147483648" }) {
            assertDoesNotThrow(() -> Numbers.parseBigDecimal(value), value);
            assertTrue(Numbers.isParsable(value), value);
        }
    }

    // ===== isCreatable =====

    @Test
    public void testIsCreatable() {
        assertTrue(Numbers.isCreatable("123"));
        assertTrue(Numbers.isCreatable("-123"));
        assertTrue(Numbers.isCreatable("+123"));
        assertTrue(Numbers.isCreatable("12.3"));
        assertTrue(Numbers.isCreatable("1.23e5"));
        assertTrue(Numbers.isCreatable("1.5e-3"));
        assertTrue(Numbers.isCreatable("123L"));
        assertTrue(Numbers.isCreatable("1.0f"));
        assertTrue(Numbers.isCreatable("1.0d"));
        assertTrue(Numbers.isCreatable("0x1A"));
        assertTrue(Numbers.isCreatable("0x00FF"));
        assertTrue(Numbers.isCreatable("-0xABCDEF"));
        assertTrue(Numbers.isCreatable("077"));
        assertTrue(Numbers.isCreatable("0.9"));
        assertTrue(Numbers.isCreatable("#FF"));
        assertTrue(Numbers.isCreatable("-#FF"));
        assertTrue(Numbers.isCreatable("+#1a2b"));

        assertFalse(Numbers.isCreatable(null));
        assertFalse(Numbers.isCreatable(""));
        assertFalse(Numbers.isCreatable("   "));
        assertFalse(Numbers.isCreatable("abc"));
        assertFalse(Numbers.isCreatable("123.45.67"));
        assertFalse(Numbers.isCreatable("123e"));
        assertFalse(Numbers.isCreatable("++123"));
        assertFalse(Numbers.isCreatable("+"));
        assertFalse(Numbers.isCreatable("0x"));
        assertFalse(Numbers.isCreatable("0xG"));
        assertFalse(Numbers.isCreatable("08"));
        assertFalse(Numbers.isCreatable("09"));
        assertFalse(Numbers.isCreatable("0789"));
        assertFalse(Numbers.isCreatable("#"));
        assertFalse(Numbers.isCreatable("#FG"));
        assertFalse(Numbers.isCreatable(" "));
        assertFalse(Numbers.isCreatable("  123  "));
    }

    @Test
    public void testIsCreatable_matchesCreateNumber() {
        final String[] valid = { "0", "-123", "+.5", "123.", "0xFF", "-0x80000000", "#fffffffffffffffffff", "010", "01f", "09f", "0123L", "01e1", "1.5e3D",
                "1e2147483648", "0e999999999999999999999", "#FF", "+#1a2b" };
        for (final String value : valid) {
            assertNotNull(Numbers.createNumber(value), value);
            assertTrue(Numbers.isCreatable(value), value);
        }
        assertEquals(Float.valueOf(1.0f), Numbers.createNumber("01f"));
        assertEquals(Long.valueOf(83L), Numbers.createNumber("0123L"));
        assertEquals(Double.valueOf(10.0d), Numbers.createNumber("01e1"));

        final String[] invalid = { " ", "\n", "abc", "09", "0789", "0x", "0xFFL", "#", "1e", "1.2.3", "NaN", "Infinity", "0x1.0p2", "1e2147483649",
                "1.0e-2147483647", "١٢٣" };
        for (final String value : invalid) {
            assertThrows(NumberFormatException.class, () -> Numbers.createNumber(value), value);
            assertFalse(Numbers.isCreatable(value), value);
        }
        assertNull(Numbers.createNumber(null));
        assertNull(Numbers.createNumber(""));
    }

    @Test
    public void testIsCreatable_ExponentScale() {
        assertTrue(Numbers.isCreatable("1e2147483648"));
        assertTrue(Numbers.isCreatable("1.0e2147483649D"));
        final Number exponentOnlyBoundary = Numbers.createNumber("1e2147483648");
        assertEquals(BigDecimal.class, exponentOnlyBoundary.getClass());
        assertEquals(Integer.MIN_VALUE, ((BigDecimal) exponentOnlyBoundary).scale());

        assertFalse(Numbers.isCreatable("1e2147483649"));
        assertFalse(Numbers.isCreatable("1e-2147483648"));
        assertTrue(Numbers.isCreatable("1e2147483647"));
        assertTrue(Numbers.isCreatable("1e-2147483647"));
        assertFalse(Numbers.isCreatable("1.0e-2147483647"));
        assertTrue(Numbers.isCreatable("1.0e-2147483646"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e2147483649"));
    }

    @Test
    public void testIsCreatable_ZeroSignificand() {
        assertTrue(Numbers.isCreatable("0e9223372036854775808"));
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.createNumber("0e9223372036854775808").doubleValue()));
        assertTrue(Numbers.isCreatable("-0.000e-999999999999999999999D"));
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.createNumber("-0.000e-999999999999999999999D").doubleValue()));
        assertFalse(Numbers.isCreatable("1e9223372036854775808"));
        assertFalse(Numbers.isCreatable("0e"));
        assertFalse(Numbers.isCreatable("0e+"));
    }

    @Test
    public void testIsCreatable_doesNotMaterializeLargeInteger() {
        assertTimeout(Duration.ofSeconds(2), () -> assertTrue(Numbers.isCreatable("9".repeat(1_000_000))));
    }

    // ===== isPrime =====

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

    // ===== isPerfectSquare =====

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

    // ===== isPowerOfTwo =====

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

    // ===== isMathematicalInteger =====

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
}
