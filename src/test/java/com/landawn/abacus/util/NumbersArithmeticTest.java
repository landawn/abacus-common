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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

public class NumbersArithmeticTest extends NumbersTestSupport {

    @Test
    public void testAddExact() {
        assertEquals(5, Numbers.addExact(2, 3));
        assertEquals(5L, Numbers.addExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Integer.MAX_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.addExact(Long.MAX_VALUE, 1L));
    }

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

    // divide

    @Test
    public void testDivide_BigInteger() {
        assertEquals(BigInteger.valueOf(5), Numbers.divide(BigInteger.TEN, BigInteger.TWO, RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(2), Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(5), RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(3), Numbers.divide(BigInteger.TEN, BigInteger.valueOf(3), RoundingMode.FLOOR));
        assertEquals(BigInteger.valueOf(4), Numbers.divide(BigInteger.TEN, BigInteger.valueOf(3), RoundingMode.CEILING));
        assertEquals(BigInteger.valueOf(3), Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(4), Numbers.divide(BigInteger.valueOf(10), BigInteger.valueOf(3), RoundingMode.UP));
        assertEquals(BigInteger.valueOf(3), Numbers.divide(BigInteger.valueOf(7), BigInteger.valueOf(2), RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(4), Numbers.divide(BigInteger.valueOf(7), BigInteger.valueOf(2), RoundingMode.UP));
        assertEquals(BigInteger.valueOf(3), Numbers.divide(BigInteger.valueOf(7), BigInteger.valueOf(2), RoundingMode.FLOOR));
        assertEquals(BigInteger.valueOf(4), Numbers.divide(BigInteger.valueOf(7), BigInteger.valueOf(2), RoundingMode.CEILING));

        assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.TEN, BigInteger.ZERO, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.divide(null, BigInteger.ONE, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.divide(BigInteger.ONE, null, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.divide(BigInteger.ONE, BigInteger.ONE, null));
    }

    // D3: divide(long, long, mode) states its divide-by-zero guard explicitly, as the int overload does.
    @Test
    public void test_divide_byZeroReportsTheSameWayForIntAndLong() {
        for (final RoundingMode mode : RoundingMode.values()) {
            final ArithmeticException longEx = assertThrows(ArithmeticException.class, () -> Numbers.divide(1L, 0L, mode));
            final ArithmeticException intEx = assertThrows(ArithmeticException.class, () -> Numbers.divide(1, 0, mode));
            assertEquals("/ by zero", longEx.getMessage(), mode.name());
            assertEquals(intEx.getMessage(), longEx.getMessage(), mode.name());
        }

        // the guard runs before the MIN_VALUE/-1 overflow check, so a zero divisor wins over either
        assertEquals("/ by zero", assertThrows(ArithmeticException.class, () -> Numbers.divide(Long.MIN_VALUE, 0L, RoundingMode.DOWN)).getMessage());
        assertEquals("/ by zero", assertThrows(ArithmeticException.class, () -> Numbers.divide(Integer.MIN_VALUE, 0, RoundingMode.DOWN)).getMessage());

        // ... and a null mode is still rejected first, before either arithmetic guard
        assertThrows(IllegalArgumentException.class, () -> Numbers.divide(1L, 0L, null));
        assertThrows(IllegalArgumentException.class, () -> Numbers.divide(1, 0, null));

        // the MIN_VALUE/-1 overflow guard is unchanged
        assertEquals("long overflow", assertThrows(ArithmeticException.class, () -> Numbers.divide(Long.MIN_VALUE, -1L, RoundingMode.DOWN)).getMessage());
    }

    @Test
    public void testDivide_int() {
        assertEquals(5, Numbers.divide(10, 2, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.divide(10, 5, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.divide(9, 3, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.divide(6, 3, RoundingMode.UNNECESSARY));
        assertEquals(-2, Numbers.divide(-6, 3, RoundingMode.UNNECESSARY));

        assertEquals(3, Numbers.divide(10, 3, RoundingMode.DOWN));
        assertEquals(4, Numbers.divide(10, 3, RoundingMode.UP));
        assertEquals(3, Numbers.divide(10, 3, RoundingMode.FLOOR));
        assertEquals(4, Numbers.divide(10, 3, RoundingMode.CEILING));
        assertEquals(3, Numbers.divide(10, 3, RoundingMode.HALF_UP));

        assertEquals(-3, Numbers.divide(-10, 3, RoundingMode.DOWN));
        assertEquals(-4, Numbers.divide(-10, 3, RoundingMode.UP));
        assertEquals(-4, Numbers.divide(-10, 3, RoundingMode.FLOOR));
        assertEquals(-3, Numbers.divide(-10, 3, RoundingMode.CEILING));
        assertEquals(-2, Numbers.divide(-7, 3, RoundingMode.CEILING));
        assertEquals(-3, Numbers.divide(-7, 3, RoundingMode.FLOOR));

        assertEquals(2, Numbers.divide(7, 3, RoundingMode.DOWN));
        assertEquals(3, Numbers.divide(7, 3, RoundingMode.UP));
        assertEquals(2, Numbers.divide(7, 3, RoundingMode.FLOOR));
        assertEquals(3, Numbers.divide(7, 3, RoundingMode.CEILING));
        assertEquals(2, Numbers.divide(7, 3, RoundingMode.HALF_DOWN));
        assertEquals(2, Numbers.divide(7, 3, RoundingMode.HALF_UP));
        assertEquals(2, Numbers.divide(7, 3, RoundingMode.HALF_EVEN));

        assertEquals(3, Numbers.divide(5, 2, RoundingMode.HALF_UP));
        assertEquals(2, Numbers.divide(5, 2, RoundingMode.HALF_DOWN));
        assertEquals(2, Numbers.divide(5, 2, RoundingMode.HALF_EVEN));
        assertEquals(4, Numbers.divide(7, 2, RoundingMode.HALF_UP));
        assertEquals(3, Numbers.divide(7, 2, RoundingMode.HALF_DOWN));
        assertEquals(4, Numbers.divide(7, 2, RoundingMode.HALF_EVEN));
        assertEquals(4, Numbers.divide(7, 2, RoundingMode.UP));
        assertEquals(3, Numbers.divide(7, 2, RoundingMode.DOWN));
        assertEquals(3, Numbers.divide(7, 2, RoundingMode.FLOOR));
        assertEquals(4, Numbers.divide(7, 2, RoundingMode.CEILING));

        assertEquals(2, Numbers.divide(4, 2, RoundingMode.HALF_EVEN));
        assertEquals(2, Numbers.divide(7, 4, RoundingMode.HALF_DOWN));
        assertEquals(1, Numbers.divide(3, 4, RoundingMode.HALF_UP));

        assertThrows(ArithmeticException.class, () -> Numbers.divide(10, 0, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.divide(10, 3, RoundingMode.UNNECESSARY));
        assertThrows(ArithmeticException.class, () -> Numbers.divide(Integer.MIN_VALUE, -1, RoundingMode.HALF_UP));
    }

    @Test
    public void testDivide_long() {
        assertEquals(5L, Numbers.divide(10L, 2L, RoundingMode.UNNECESSARY));
        assertEquals(2L, Numbers.divide(10L, 5L, RoundingMode.UNNECESSARY));
        assertEquals(1000000L, Numbers.divide(1000000000000L, 1000000L, RoundingMode.UNNECESSARY));
        assertEquals(Long.MAX_VALUE, Numbers.divide(Long.MAX_VALUE, 1L, RoundingMode.UNNECESSARY));
        assertEquals(-Long.MAX_VALUE, Numbers.divide(Long.MIN_VALUE + 1, 1L, RoundingMode.UNNECESSARY));

        assertEquals(3L, Numbers.divide(10L, 3L, RoundingMode.DOWN));
        assertEquals(4L, Numbers.divide(10L, 3L, RoundingMode.UP));
        assertEquals(3L, Numbers.divide(10L, 3L, RoundingMode.FLOOR));
        assertEquals(4L, Numbers.divide(10L, 3L, RoundingMode.CEILING));

        assertEquals(2L, Numbers.divide(7L, 3L, RoundingMode.DOWN));
        assertEquals(3L, Numbers.divide(7L, 3L, RoundingMode.UP));
        assertEquals(2L, Numbers.divide(7L, 3L, RoundingMode.FLOOR));
        assertEquals(3L, Numbers.divide(7L, 3L, RoundingMode.CEILING));
        assertEquals(2L, Numbers.divide(7L, 3L, RoundingMode.HALF_DOWN));
        assertEquals(2L, Numbers.divide(7L, 3L, RoundingMode.HALF_UP));
        assertEquals(2L, Numbers.divide(7L, 3L, RoundingMode.HALF_EVEN));
        assertEquals(-3L, Numbers.divide(-7L, 3L, RoundingMode.FLOOR));
        assertEquals(-2L, Numbers.divide(-7L, 3L, RoundingMode.CEILING));

        assertEquals(2L, Numbers.divide(5L, 2L, RoundingMode.HALF_EVEN));
        assertEquals(4L, Numbers.divide(7L, 2L, RoundingMode.HALF_EVEN));
        assertEquals(4L, Numbers.divide(7L, 2L, RoundingMode.HALF_UP));
        assertEquals(3L, Numbers.divide(7L, 2L, RoundingMode.HALF_DOWN));
        assertEquals(-2L, Numbers.divide(-5L, 2L, RoundingMode.HALF_DOWN));
        assertEquals(-3L, Numbers.divide(-5L, 2L, RoundingMode.HALF_UP));

        assertThrows(ArithmeticException.class, () -> Numbers.divide(10L, 0L, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.divide(7L, 3L, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testDivide_long_MinValueOverflow() {
        for (final RoundingMode mode : RoundingMode.values()) {
            assertThrows(ArithmeticException.class, () -> Numbers.divide(Long.MIN_VALUE, -1L, mode),
                    "divide(Long.MIN_VALUE, -1L, " + mode + ") should overflow");
        }
    }

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

    @Test
    public void c020_divideBigIntegerZeroDivisorMessageIsSizeIndependent() {
        final BigInteger huge = BigInteger.TEN.pow(30);

        assertEquals("/ by zero",
                assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.valueOf(7), BigInteger.ZERO, RoundingMode.DOWN)).getMessage());
        assertEquals("/ by zero", assertThrows(ArithmeticException.class, () -> Numbers.divide(huge, BigInteger.ZERO, RoundingMode.DOWN)).getMessage());
        assertEquals("/ by zero",
                assertThrows(ArithmeticException.class, () -> Numbers.divide(huge.negate(), BigInteger.ZERO, RoundingMode.HALF_EVEN)).getMessage());
        assertEquals("/ by zero", assertThrows(ArithmeticException.class, () -> Numbers.divide(huge, BigInteger.ZERO, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals("/ by zero",
                assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.ZERO, BigInteger.ZERO, RoundingMode.DOWN)).getMessage());
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
    public void testMod() {
        assertEquals(1, Numbers.mod(7, 3));
        assertEquals(2, Numbers.mod(-7, 3));
        assertEquals(1, Numbers.mod(7L, 3));
        assertEquals(1L, Numbers.mod(7L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.mod(7, 0));
        assertThrows(ArithmeticException.class, () -> Numbers.mod(7L, 0L));
    }

    @Test
    public void testMultiplyExact() {
        assertEquals(6, Numbers.multiplyExact(2, 3));
        assertEquals(6L, Numbers.multiplyExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Integer.MAX_VALUE, 2));
        assertThrows(ArithmeticException.class, () -> Numbers.multiplyExact(Long.MAX_VALUE, 2L));
    }

    // powExact

    @Test
    public void testPowExact_int() {
        assertEquals(1, Numbers.powExact(2, 0));
        assertEquals(2, Numbers.powExact(2, 1));
        assertEquals(4, Numbers.powExact(2, 2));
        assertEquals(8, Numbers.powExact(2, 3));
        assertEquals(16, Numbers.powExact(2, 4));
        assertEquals(1024, Numbers.powExact(2, 10));
        assertEquals(1000, Numbers.powExact(10, 3));
        assertEquals(81, Numbers.powExact(3, 4));
        assertEquals(243, Numbers.powExact(3, 5));
        assertEquals(1000000000, Numbers.powExact(10, 9));

        assertEquals(1, Numbers.powExact(0, 0));
        assertEquals(0, Numbers.powExact(0, 5));
        assertEquals(1, Numbers.powExact(1, 100));
        assertEquals(1, Numbers.powExact(-1, 0));
        assertEquals(-1, Numbers.powExact(-1, 1));
        assertEquals(1, Numbers.powExact(-1, 2));
        assertEquals(-1, Numbers.powExact(-1, 3));
        assertEquals(1, Numbers.powExact(-1, 4));
        assertEquals(4, Numbers.powExact(-2, 2));
        assertEquals(-8, Numbers.powExact(-2, 3));
        assertEquals(16, Numbers.powExact(-2, 4));

        assertThrows(IllegalArgumentException.class, () -> Numbers.powExact(2, -1));
    }

    @Test
    public void testPowExact_int_Overflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(2, 31));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(2, 32));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(-2, 32));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(-2, 33));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(3, 20));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(10, 10));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(Integer.MAX_VALUE, 2));

        assertEquals("powExact(2, 32) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(2, 32)).getMessage());
        assertEquals("powExact(-2, 32) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(-2, 32)).getMessage());
        assertEquals("powExact(2147483647, 2) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(Integer.MAX_VALUE, 2)).getMessage());
    }

    @Test
    public void testPowExact_long() {
        assertEquals(1L, Numbers.powExact(2L, 0));
        assertEquals(2L, Numbers.powExact(2L, 1));
        assertEquals(8L, Numbers.powExact(2L, 3));
        assertEquals(1024L, Numbers.powExact(2L, 10));
        assertEquals(1L << 30, Numbers.powExact(2L, 30));
        assertEquals(1L << 60, Numbers.powExact(2L, 60));
        assertEquals(1000000L, Numbers.powExact(10L, 6));
        assertEquals(27L, Numbers.powExact(3L, 3));
        assertEquals(81L, Numbers.powExact(3L, 4));
        assertEquals(243L, Numbers.powExact(3L, 5));
        assertEquals(1000000000000000000L, Numbers.powExact(10L, 18));

        assertEquals(1L, Numbers.powExact(0L, 0));
        assertEquals(0L, Numbers.powExact(0L, 5));
        assertEquals(1L, Numbers.powExact(1L, 100));
        assertEquals(1L, Numbers.powExact(-1L, 4));
        assertEquals(-1L, Numbers.powExact(-1L, 1));
        assertEquals(-1L, Numbers.powExact(-1L, 5));
        assertEquals(4L, Numbers.powExact(-2L, 2));
        assertEquals(-8L, Numbers.powExact(-2L, 3));

        assertThrows(IllegalArgumentException.class, () -> Numbers.powExact(2L, -1));
    }

    @Test
    public void testPowExact_long_Overflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(2L, 63));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(2L, 64));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(-2L, 64));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(10L, 19));
        assertThrows(ArithmeticException.class, () -> Numbers.powExact(Long.MAX_VALUE, 2));

        assertEquals("powExact(2, 64) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(2L, 64)).getMessage());
        assertEquals("powExact(-2, 64) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(-2L, 64)).getMessage());
        assertEquals("powExact(9223372036854775807, 2) overflow",
                assertThrows(ArithmeticException.class, () -> Numbers.powExact(Long.MAX_VALUE, 2)).getMessage());
    }

    @Test
    public void c019_powExactIntOverflowMessageIsUniform() {
        assertEquals("powExact(10, 10) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(10, 10)).getMessage());
        assertEquals("powExact(3, 20) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(3, 20)).getMessage());
        assertEquals("powExact(46340, 3) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(46340, 3)).getMessage());
        assertEquals("powExact(46341, 2) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(46341, 2)).getMessage());
        assertEquals("powExact(-3, 20) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(-3, 20)).getMessage());
        assertEquals("powExact(2, 31) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(2, 31)).getMessage());
        assertEquals("powExact(-2147483648, 2) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(Integer.MIN_VALUE, 2)).getMessage());

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
        assertEquals("powExact(3037000500, 2) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(3037000500L, 2)).getMessage());
        assertEquals("powExact(-3037000499, 3) overflow", assertThrows(ArithmeticException.class, () -> Numbers.powExact(-3037000499L, 3)).getMessage());
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

    @Test
    public void testSaturatedAdd() {
        assertEquals(5, Numbers.saturatedAdd(2, 3));
        assertEquals(-5, Numbers.saturatedAdd(-2, -3));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, 1));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedAdd(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, -1));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedAdd(Integer.MIN_VALUE, Integer.MIN_VALUE));

        assertEquals(5L, Numbers.saturatedAdd(2L, 3L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedAdd(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedAdd(Long.MIN_VALUE, -1L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedAdd(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void testSaturatedCastToInt() {
        assertEquals(0, Numbers.saturatedCastToInt(0L));
        assertEquals(42, Numbers.saturatedCastToInt(42L));
        assertEquals(-42, Numbers.saturatedCastToInt(-42L));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedCastToInt(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedCastToInt(Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedCastToInt((long) Integer.MAX_VALUE + 1));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedCastToInt((long) Integer.MIN_VALUE - 1));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedCastToInt(Long.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedCastToInt(Long.MIN_VALUE));
    }

    @Test
    public void testSaturatedMultiply() {
        assertEquals(6, Numbers.saturatedMultiply(2, 3));
        assertEquals(0, Numbers.saturatedMultiply(0, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedMultiply(Integer.MAX_VALUE, -2));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedMultiply(Integer.MIN_VALUE, 2));

        assertEquals(6L, Numbers.saturatedMultiply(2L, 3L));
        assertEquals(20000L, Numbers.saturatedMultiply(100L, 200L));
        assertEquals(0L, Numbers.saturatedMultiply(0L, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedMultiply(Long.MAX_VALUE, 2L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedMultiply(10000000000L, 10000000000L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedMultiply(Long.MIN_VALUE, 2L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedMultiply(-10000000000L, 10000000000L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedMultiply(-1L, Long.MIN_VALUE));
        assertEquals((Long.MAX_VALUE / 4) * 2L, Numbers.saturatedMultiply(Long.MAX_VALUE / 4, 2L));
    }

    @Test
    public void testSaturatedPow() {
        assertEquals(1, Numbers.saturatedPow(2, 0));
        assertEquals(8, Numbers.saturatedPow(2, 3));
        assertEquals(1024, Numbers.saturatedPow(2, 10));
        assertEquals(1, Numbers.saturatedPow(0, 0));
        assertEquals(0, Numbers.saturatedPow(0, 5));
        assertEquals(1, Numbers.saturatedPow(1, 100));
        assertEquals(1, Numbers.saturatedPow(-1, 0));
        assertEquals(-1, Numbers.saturatedPow(-1, 1));
        assertEquals(1, Numbers.saturatedPow(-1, 4));
        assertEquals(-1, Numbers.saturatedPow(-1, 5));
        assertEquals(4, Numbers.saturatedPow(-2, 2));
        assertEquals(-8, Numbers.saturatedPow(-2, 3));
        assertEquals(1000000000, Numbers.saturatedPow(10, 9));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(2, 31));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(2, 100));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(10, 10));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedPow(-2, 32));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedPow(-2, 31));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedPow(-100, 9));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedPow(2, -1));

        assertEquals(1L, Numbers.saturatedPow(2L, 0));
        assertEquals(8L, Numbers.saturatedPow(2L, 3));
        assertEquals(1L << 40, Numbers.saturatedPow(2L, 40));
        assertEquals(1L, Numbers.saturatedPow(0L, 0));
        assertEquals(0L, Numbers.saturatedPow(0L, 5));
        assertEquals(1L, Numbers.saturatedPow(1L, 100));
        assertEquals(1L, Numbers.saturatedPow(-1L, 4));
        assertEquals(-1L, Numbers.saturatedPow(-1L, 5));
        assertEquals(4L, Numbers.saturatedPow(-2L, 2));
        assertEquals(-8L, Numbers.saturatedPow(-2L, 3));
        assertEquals(1000000000000000000L, Numbers.saturatedPow(10L, 18));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(2L, 63));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(-2L, 64));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(10L, 19));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedPow(Long.MAX_VALUE, 2));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedPow(-10L, 19));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedPow(-1000L, 19));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedPow(2L, -1));
    }

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

    @Test
    public void testSaturatedSubtract() {
        assertEquals(-1, Numbers.saturatedSubtract(2, 3));
        assertEquals(0, Numbers.saturatedSubtract(5, 5));
        assertEquals(Integer.MIN_VALUE, Numbers.saturatedSubtract(Integer.MIN_VALUE, 1));
        assertEquals(Integer.MAX_VALUE, Numbers.saturatedSubtract(Integer.MAX_VALUE, -1));

        assertEquals(-1L, Numbers.saturatedSubtract(2L, 3L));
        assertEquals(0L, Numbers.saturatedSubtract(5L, 5L));
        assertEquals(Long.MIN_VALUE, Numbers.saturatedSubtract(Long.MIN_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, Numbers.saturatedSubtract(Long.MAX_VALUE, -1L));
    }

    @Test
    public void testSubtractExact() {
        assertEquals(-1, Numbers.subtractExact(2, 3));
        assertEquals(-1L, Numbers.subtractExact(2L, 3L));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Integer.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Numbers.subtractExact(Long.MIN_VALUE, 1L));
    }
}
