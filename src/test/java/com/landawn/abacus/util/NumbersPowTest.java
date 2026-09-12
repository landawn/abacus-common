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

public class NumbersPowTest extends NumbersTestSupport {

    // ===== powExact(int, int) =====

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

    // ===== powExact(long, int) =====

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
}
