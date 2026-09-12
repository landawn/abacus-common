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

import java.math.BigInteger;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

public class NumbersDivideTest extends NumbersTestSupport {

    // ===== divide(int, int, RoundingMode) =====

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

    // ===== divide(long, long, RoundingMode) =====

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

    // ===== divide(BigInteger, BigInteger, RoundingMode) =====

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
}
