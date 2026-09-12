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

import java.math.BigInteger;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

public class NumbersSqrtTest extends NumbersTestSupport {

    // ===== sqrt(int, RoundingMode) =====

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

    // ===== sqrt(long, RoundingMode) =====

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

    // ===== sqrt(BigInteger, RoundingMode) =====

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
}
