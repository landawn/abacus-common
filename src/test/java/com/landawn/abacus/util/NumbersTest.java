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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.type.Type;

public class NumbersTest extends NumbersTestSupport {

    @Test
    public void testLcmSignIndependentAcrossNumericTypes() {
        for (final int signA : new int[] { -1, 1 }) {
            for (final int signB : new int[] { -1, 1 }) {
                assertEquals(12, Numbers.lcm(signA * 4, signB * 6));
                assertEquals(12L, Numbers.lcm(signA * 4L, signB * 6L));
                assertEquals(BigInteger.valueOf(12), Numbers.lcm(BigInteger.valueOf(signA * 4), BigInteger.valueOf(signB * 6)));
            }
        }

        assertEquals(0, Numbers.lcm(Integer.MIN_VALUE, 0));
        assertEquals(0, Numbers.lcm(0, Integer.MIN_VALUE));
        assertEquals(0L, Numbers.lcm(Long.MIN_VALUE, 0L));
        assertEquals(0L, Numbers.lcm(0L, Long.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Integer.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Long.MIN_VALUE, 1L));
        assertEquals(BigInteger.ONE.shiftLeft(63), Numbers.lcm(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.ONE));
        assertEquals(BigInteger.ZERO, Numbers.lcm(BigInteger.ZERO, BigInteger.valueOf(Long.MIN_VALUE)));
    }

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

    // ===== decode / parse / tryParse =====

    @Test
    public void testDecodeInteger() {
        assertEquals(Integer.valueOf(123), Numbers.decodeInteger("123"));
        assertEquals(Integer.valueOf(255), Numbers.decodeInteger("0xFF"));
        assertEquals(Integer.valueOf(8), Numbers.decodeInteger("010"));
        assertNull(Numbers.decodeInteger(null));
        assertNull(Numbers.decodeInteger(""));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("08"));
        assertEquals("abc is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("abc")).getMessage());
        for (final String token : new String[] { "0", "00", "+00", "010", "+010", "-010", "0x7fffffff", "-0x80000000", "#7f" }) {
            assertEquals(Integer.decode(token), Numbers.decodeInteger(token), token);
        }
    }

    @Test
    public void testDecodeLong() {
        assertEquals(Long.valueOf(123L), Numbers.decodeLong("123"));
        assertEquals(Long.valueOf(255L), Numbers.decodeLong("0xFFL"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-9223372036854775808"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-0x8000000000000000"));
        assertNull(Numbers.decodeLong(null));
        assertNull(Numbers.decodeLong(""));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9223372036854775808"));
        assertEquals("abc is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("abc")).getMessage());
        for (final String token : new String[] { "0", "010", "0x7fffffffffffffff", "-0x8000000000000000" }) {
            assertEquals(Long.decode(token), Numbers.decodeLong(token), token);
        }
    }

    @Test
    public void testDecodeBigInteger() {
        assertEquals(new BigInteger("12345678901234567890"), Numbers.decodeBigInteger("12345678901234567890"));
        assertEquals(new BigInteger("FF", 16), Numbers.decodeBigInteger("0xFF"));
        assertEquals(new BigInteger("77", 8), Numbers.decodeBigInteger("077"));
        assertEquals(BigInteger.valueOf(-1), Numbers.decodeBigInteger("-1"));
        assertNull(Numbers.decodeBigInteger(null));
        assertNull(Numbers.decodeBigInteger(""));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("--1"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("+-1"));
    }

    @Test
    public void testParseFloat() {
        assertEquals(Float.valueOf(1.5f), Numbers.parseFloat("1.5"));
        assertEquals(Float.valueOf(0.0f), Numbers.parseFloat("0"));
        assertNull(Numbers.parseFloat(null));
        assertNull(Numbers.parseFloat(""));
        assertTrue(Numbers.parseFloat("NaN").isNaN());
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("abc"));
        assertEquals("abc is not a valid Float.", assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("abc")).getMessage());
        assertEquals("\"   \" is not a valid Float.", assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("   ")).getMessage());
    }

    @Test
    public void testParseDouble() {
        assertEquals(Double.valueOf(1.5d), Numbers.parseDouble("1.5"));
        assertEquals(Double.valueOf(-1.5), Numbers.parseDouble("-1.5"));
        assertNull(Numbers.parseDouble(null));
        assertNull(Numbers.parseDouble(""));
        assertThrows(NumberFormatException.class, () -> Numbers.parseDouble("abc"));
        assertEquals("abc is not a valid Double.", assertThrows(NumberFormatException.class, () -> Numbers.parseDouble("abc")).getMessage());
    }

    @Test
    public void testParseBigDecimal() {
        assertEquals(new BigDecimal("123.456789"), Numbers.parseBigDecimal("123.456789"));
        assertEquals(BigDecimal.ZERO, Numbers.parseBigDecimal("0"));
        assertNull(Numbers.parseBigDecimal(null));
        assertNull(Numbers.parseBigDecimal(""));
        assertEquals(0, Numbers.parseBigDecimal("123.").scale());
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("١٢٣"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("0x1.0p2"));
        assertEquals("1.2.3 is not a valid BigDecimal.", assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1.2.3")).getMessage());
        final String overLimit = "1" + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH);
        assertEquals(overLimit.length(), Numbers.parseBigDecimal(overLimit).precision());
    }

    @Test
    public void testTryParseInt() {
        assertEquals(123, Numbers.tryParseInt("123").getAsInt());
        assertEquals(10, Numbers.tryParseInt("010").getAsInt());
        assertEquals(16, Numbers.tryParseInt("0x10").getAsInt());
        assertEquals(123, Numbers.tryParseInt("123L").getAsInt());
        assertTrue(Numbers.tryParseInt("2147483648").isEmpty());
        assertTrue(Numbers.tryParseInt("abc").isEmpty());
        assertTrue(Numbers.tryParseInt("").isEmpty());
        assertTrue(Numbers.tryParseInt(null).isEmpty());
        assertTrue(Numbers.tryParseInt("١٢٣").isEmpty());
    }

    @Test
    public void testTryParseLong() {
        assertEquals(123L, Numbers.tryParseLong("123").getAsLong());
        assertEquals(255L, Numbers.tryParseLong("0xFFL").getAsLong());
        assertEquals(Long.MIN_VALUE, Numbers.tryParseLong("-9223372036854775808").getAsLong());
        assertEquals(Long.MIN_VALUE, Numbers.tryParseLong("-0x8000000000000000").getAsLong());
        assertTrue(Numbers.tryParseLong("9223372036854775808").isEmpty());
        assertTrue(Numbers.tryParseLong("abc").isEmpty());
        assertTrue(Numbers.tryParseLong(null).isEmpty());
    }

    @Test
    public void testTryParseFloat() {
        for (final String value : new String[] { "0", "123.45", "NaN", "-Infinity", "0x1.0p2", "1e39" }) {
            final u.OptionalFloat result = assertDoesNotThrow(() -> Numbers.tryParseFloat(value), value);
            assertTrue(result.isPresent(), value);
            assertEquals(Float.parseFloat(value), result.getAsFloat(), value);
        }
        for (final String value : new String[] { null, "", " ", "abc", "123L", "0xFF", "١٢٣" }) {
            assertTrue(Numbers.tryParseFloat(value).isEmpty(), String.valueOf(value));
        }
    }

    @Test
    public void testTryParseDouble() {
        for (final String value : new String[] { "0", "123.45", "NaN", "-Infinity", "0x1.0p2", "1e309" }) {
            final u.OptionalDouble result = assertDoesNotThrow(() -> Numbers.tryParseDouble(value), value);
            assertTrue(result.isPresent(), value);
            assertEquals(Double.parseDouble(value), result.getAsDouble(), value);
        }
        for (final String value : new String[] { null, "", "abc", "123L", "0xFF" }) {
            assertTrue(Numbers.tryParseDouble(value).isEmpty(), String.valueOf(value));
        }
    }

    @Test
    public void testNullableCreationTreatsOnlyEmptyAsNull() {
        assertNull(Numbers.decodeInteger(null));
        assertNull(Numbers.parseFloat(""));
        assertNull(Numbers.createNumber(""));
        for (final String whitespace : new String[] { " ", "\n" }) {
            assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(whitespace));
            assertThrows(NumberFormatException.class, () -> Numbers.parseFloat(whitespace));
            assertThrows(NumberFormatException.class, () -> Numbers.createNumber(whitespace));
            assertTrue(Numbers.tryParseInt(whitespace).isEmpty());
            assertTrue(Numbers.tryCreateNumber(whitespace).isEmpty());
        }
    }

    @Test
    public void testFloatingPointTokenLengthLimit() {
        final String atLimit = "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH);
        final String overLimit = atLimit + "0";
        assertTrue(Numbers.isParsable(atLimit));
        assertEquals(0.0d, Numbers.toDouble(atLimit), 0.0d);
        assertFalse(Numbers.isParsable(overLimit));
        assertTrue(Numbers.tryParseDouble(overLimit).isEmpty());
        final NumberFormatException nfe = assertThrows(NumberFormatException.class, () -> Numbers.parseDouble(overLimit));
        assertTrue(nfe.getMessage().length() < 128, nfe.getMessage());
        assertNotNull(nfe.getCause());
        assertTrue(nfe.getCause().getMessage().contains("exceeds limit"));
    }

    @Test
    public void testErrorMessages_BoundedAndEscaped() {
        final NumberFormatException ctrl = assertThrows(NumberFormatException.class, () -> Numbers.toLong("12\n34"));
        assertTrue(ctrl.getMessage().contains("\\u000A"), ctrl.getMessage());
        assertFalse(ctrl.getMessage().contains("\n"), ctrl.getMessage());
        final NumberFormatException decode = assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9".repeat(100_000)));
        assertTrue(decode.getMessage().length() < 128, decode.getMessage());
        final String hugeDigits = "9".repeat(100_000);
        final ArithmeticException overflow = assertThrows(ArithmeticException.class, () -> Numbers.toLong(hugeDigits));
        assertTrue(overflow.getMessage().startsWith("long overflow: 99999999"), overflow.getMessage());
        assertTrue(overflow.getMessage().length() < 128, overflow.getMessage());
        assertEquals(1L, Numbers.toLong("0".repeat(100_000) + "1"));
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-9223372036854775808"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("١٢٣"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("１２３"));
    }

    @Test
    public void testBoundedConversion_HugeBigDecimal() {
        final BigDecimal huge = new BigDecimal("1e2147483647");
        assertTimeout(java.time.Duration.ofSeconds(2), () -> {
            assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, int.class));
            assertThrows(ArithmeticException.class, () -> Numbers.toLong(huge, 0L));
        });
        assertEquals("byte overflow: " + huge, assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, byte.class)).getMessage());
        assertEquals(0L, Numbers.toLong(new BigDecimal("1e-1000000"), 0L));
    }

    @Test
    public void testScannerFastPaths_RandomizedRoundTrips() {
        final Random random = new Random(0x5EED_C0DE_1234_5678L);
        for (int i = 0; i < 4096; i++) {
            final int intValue = random.nextInt();
            final BigInteger expectedInt = BigInteger.valueOf(intValue);
            final boolean explicitPlus = (i & 1) == 0;
            final String hexPrefix = (i % 3 == 0) ? "0x" : (i % 3 == 1 ? "0X" : "#");
            final String intDecimal = signedRadixToken(expectedInt, 10, "", explicitPlus);
            final String intHex = signedRadixToken(expectedInt, 16, hexPrefix, explicitPlus);
            assertEquals(intValue, Numbers.toInt(intDecimal), intDecimal);
            assertEquals(Integer.valueOf(intValue), Numbers.decodeInteger(intHex), intHex);

            final long longValue = random.nextLong();
            final BigInteger expectedLong = BigInteger.valueOf(longValue);
            final String longDecimal = signedRadixToken(expectedLong, 10, "", explicitPlus);
            assertEquals(longValue, Numbers.toLong(longDecimal), longDecimal);
            assertEquals(Long.valueOf(longValue), Numbers.decodeLong(longDecimal + "L"), longDecimal);
        }
    }

    // ===== ceilingPowerOfTwo / floorPowerOfTwo =====

    @Test
    public void testCeilingPowerOfTwo() {
        assertEquals(1, Numbers.ceilingPowerOfTwo(1));
        assertEquals(4, Numbers.ceilingPowerOfTwo(3));
        assertEquals(1L, Numbers.ceilingPowerOfTwo(1L));
        assertEquals(4L, Numbers.ceilingPowerOfTwo(3L));
        assertEquals(1024L, Numbers.ceilingPowerOfTwo(1000L));
        assertEquals(1L << 62, Numbers.ceilingPowerOfTwo((1L << 62) - 1));
        assertEquals(BigInteger.valueOf(4), Numbers.ceilingPowerOfTwo(BigInteger.valueOf(3)));
        assertEquals(BigInteger.ONE.shiftLeft(100), Numbers.ceilingPowerOfTwo(BigInteger.ONE.shiftLeft(100)));
        assertThrows(IllegalArgumentException.class, () -> Numbers.ceilingPowerOfTwo(0L));
        assertThrows(ArithmeticException.class, () -> Numbers.ceilingPowerOfTwo((1L << 62) + 1));
        assertThrows(IllegalArgumentException.class, () -> Numbers.ceilingPowerOfTwo(BigInteger.ZERO));
    }

    @Test
    public void testFloorPowerOfTwo() {
        assertEquals(1, Numbers.floorPowerOfTwo(1));
        assertEquals(2, Numbers.floorPowerOfTwo(3));
        assertEquals(1L, Numbers.floorPowerOfTwo(1L));
        assertEquals(8L, Numbers.floorPowerOfTwo(15L));
        assertEquals(BigInteger.ONE, Numbers.floorPowerOfTwo(BigInteger.ONE));
        assertEquals(BigInteger.valueOf(8), Numbers.floorPowerOfTwo(BigInteger.valueOf(15)));
        assertThrows(IllegalArgumentException.class, () -> Numbers.floorPowerOfTwo(0L));
        assertThrows(IllegalArgumentException.class, () -> Numbers.floorPowerOfTwo(BigInteger.ZERO));
    }

    // ===== mod / gcd / lcm =====

    @Test
    public void testMod() {
        assertEquals(1, Numbers.mod(7, 3));
        assertEquals(2, Numbers.mod(-7, 3));
        assertEquals(1, Numbers.mod(7L, 3));
        assertEquals(1L, Numbers.mod(7L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.mod(7, 0));
        assertThrows(ArithmeticException.class, () -> Numbers.mod(7L, 0L));
    }

    @Test
    public void testGcd() {
        assertEquals(4, Numbers.gcd(12, 8));
        assertEquals(6, Numbers.gcd(54, 24));
        assertEquals(1, Numbers.gcd(17, 13));
        assertEquals(5, Numbers.gcd(0, 5));
        assertEquals(4L, Numbers.gcd(12L, 8L));
        assertEquals(BigInteger.valueOf(4), Numbers.gcd(BigInteger.valueOf(12), BigInteger.valueOf(8)));
        assertEquals(BigInteger.ZERO, Numbers.gcd(BigInteger.ZERO, BigInteger.ZERO));
        assertThrows(ArithmeticException.class, () -> Numbers.gcd(0, Integer.MIN_VALUE));
        assertEquals("gcd would be 2^31, not representable as int",
                assertThrows(ArithmeticException.class, () -> Numbers.gcd(0, Integer.MIN_VALUE)).getMessage());
        assertEquals("gcd would be 2^63, not representable as long",
                assertThrows(ArithmeticException.class, () -> Numbers.gcd(0L, Long.MIN_VALUE)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Numbers.gcd(null, BigInteger.ONE));
    }

    @Test
    public void testLcm() {
        assertEquals(12, Numbers.lcm(4, 6));
        assertEquals(36, Numbers.lcm(12, 18));
        assertEquals(0, Numbers.lcm(0, 5));
        assertEquals(12L, Numbers.lcm(4L, 6L));
        assertEquals(BigInteger.valueOf(12), Numbers.lcm(BigInteger.valueOf(4), BigInteger.valueOf(6)));
        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Integer.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.lcm(Long.MIN_VALUE, 1L));
        assertThrows(IllegalArgumentException.class, () -> Numbers.lcm(null, BigInteger.ONE));
    }

    // ===== exact arithmetic =====

    @Test
    public void testAddExact() {
        assertEquals(5, Numbers.addExact(2, 3));
        assertEquals(5L, Numbers.addExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Integer.MAX_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Long.MAX_VALUE, 1L));
    }

    @Test
    public void testSubtractExact() {
        assertEquals(-1, Numbers.subtractExact(2, 3));
        assertEquals(-1L, Numbers.subtractExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Integer.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Long.MIN_VALUE, 1L));
    }

    @Test
    public void testMultiplyExact() {
        assertEquals(6, Numbers.multiplyExact(2, 3));
        assertEquals(6L, Numbers.multiplyExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Integer.MAX_VALUE, 2));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Long.MAX_VALUE, 2L));
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

    // ===== factorial =====

    @Test
    public void testFactorial() {
        assertEquals(1, Numbers.factorialExact(0));
        assertEquals(120, Numbers.factorialExact(5));
        assertEquals(479001600, Numbers.factorialExact(12));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedFactorial(13));
        assertEquals("factorialExact(13) overflow", assertThrows(ArithmeticException.class, () -> Numbers.factorialExact(13)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Numbers.factorialExact(-1));

        assertEquals(1L, Numbers.factorialExactToLong(0));
        assertEquals(2432902008176640000L, Numbers.factorialExactToLong(20));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedFactorialToLong(21));
        assertEquals("factorialExactToLong(21) overflow", assertThrows(ArithmeticException.class, () -> Numbers.factorialExactToLong(21)).getMessage());

        assertEquals(1.0, Numbers.factorialToDouble(0), 0.0);
        assertEquals(120.0, Numbers.factorialToDouble(5), 0.0);
        assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(0));
        assertEquals(BigInteger.valueOf(120), Numbers.factorialToBigInteger(5));
        assertTrue(Numbers.factorialToBigInteger(30).compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);

        assertEquals(BigInteger.ONE, Numbers.listProduct(new ArrayList<>()));
        assertEquals(BigInteger.valueOf(6), Numbers.listProduct(List.of(BigInteger.TWO, BigInteger.valueOf(3))));
    }

    @Test
    public void testFitsInLong() {
        assertTrue(Numbers.fitsInLong(BigInteger.ZERO));
        assertTrue(Numbers.fitsInLong(BigInteger.valueOf(Long.MAX_VALUE)));
        assertTrue(Numbers.fitsInLong(BigInteger.valueOf(Long.MIN_VALUE)));
        assertFalse(Numbers.fitsInLong(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)));
        assertFalse(Numbers.fitsInLong(new BigInteger("123456789012345678901234567890")));
    }

    // ===== fuzzyEquals / fuzzyCompare =====

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
    public void testFuzzyCompare() {
        assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.0001f, 0.001f));
        assertTrue(Numbers.fuzzyCompare(1.0f, 2.0f, 0.001f) < 0);
        assertEquals(0, Numbers.fuzzyCompare(1.0, 1.0 + 1e-16, 1e-15));
        assertTrue(Numbers.fuzzyCompare(1.0, 2.0, 0.001) < 0);
    }

    // ===== asinh / acosh / atanh =====

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

    @Test
    public void testAcosh() {
        assertEquals(0.0, Numbers.acosh(1.0), 1e-12);
        assertTrue(Double.isFinite(Numbers.acosh(Double.MAX_VALUE)));
        assertTrue(Double.isNaN(Numbers.acosh(0.5)));
    }

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
    public void testUnknownNumberRenderedOnce() {
        final CountingNumber20260906 value = new CountingNumber20260906();
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(value));
        assertEquals(1, value.toStringCalls);

        final Number blank = new BlankTextNumber20260906(42.0d);
        assertThrows(NumberFormatException.class, () -> Numbers.convert(blank, AtomicLong.class));
        assertEquals(Integer.valueOf(42), Numbers.convert(blank, Integer.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, Type.of(BigDecimal.class)));
        assertTrue(Double.isNaN(Numbers.convert(Double.NaN, Type.of(Double.class))));
    }
}
