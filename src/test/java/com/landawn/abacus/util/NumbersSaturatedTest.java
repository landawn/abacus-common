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

import org.junit.jupiter.api.Test;

public class NumbersSaturatedTest extends NumbersTestSupport {

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
    public void testSaturatedFactorial() {
        assertEquals(1, Numbers.saturatedFactorial(0));
        assertEquals(1, Numbers.saturatedFactorial(1));
        assertEquals(2, Numbers.saturatedFactorial(2));
        assertEquals(6, Numbers.saturatedFactorial(3));
        assertEquals(24, Numbers.saturatedFactorial(4));
        assertEquals(120, Numbers.saturatedFactorial(5));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedFactorial(-1));

        assertEquals(1L, Numbers.saturatedFactorialToLong(0));
        assertEquals(2L, Numbers.saturatedFactorialToLong(2));
        assertEquals(3628800L, Numbers.saturatedFactorialToLong(10));
        assertThrows(IllegalArgumentException.class, () -> Numbers.saturatedFactorialToLong(-1));
    }
}
