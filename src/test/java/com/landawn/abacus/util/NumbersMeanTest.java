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

import org.junit.jupiter.api.Test;

public class NumbersMeanTest extends NumbersTestSupport {

    // ===== mean(int...) =====

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

    // ===== mean(long...) =====

    @Test
    public void testMean_long() {
        assertEquals(3.0, Numbers.mean(1L, 2L, 3L, 4L, 5L), DELTA);
        assertEquals(5.0, Numbers.mean(3L, 5L, 7L), 0.001);
        assertEquals(Long.MAX_VALUE, Numbers.mean(Long.MAX_VALUE), 1e-10);
        assertEquals(1000000.0, Numbers.mean(999999L, 1000000L, 1000001L), 0.0001);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new long[0]));
    }

    // ===== mean(double...) =====

    @Test
    public void testMean_double() {
        assertEquals(3.0, Numbers.mean(1.0, 2.0, 3.0, 4.0, 5.0), DELTA);
        assertEquals(3.5, Numbers.mean(3.0, 4.0), 0.001);
        assertEquals(2.5, Numbers.mean(1.0, 2.0, 3.0, 4.0), 1e-10);
        assertEquals(0.0, Numbers.mean(-1.0, 1.0), 0.0001);
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new double[0]));
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
}
