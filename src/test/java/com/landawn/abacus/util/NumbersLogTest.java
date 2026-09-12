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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class NumbersLogTest extends NumbersTestSupport {

    @Test
    public void testLog() {
        assertEquals(0.0, Numbers.log(1.0), DELTA);
        assertEquals(Math.log(Math.E), Numbers.log(Math.E), DELTA);
        assertEquals(Math.log(10.0), Numbers.log(10.0), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.log(0.0));
        assertTrue(Double.isNaN(Numbers.log(-1.0)));
    }

    @Test
    public void testLog2_int() {
        assertEquals(0, Numbers.log2(1, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log2(2, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(8, RoundingMode.UNNECESSARY));
        for (final RoundingMode mode : RoundingMode.values()) {
            assertEquals(3, Numbers.log2(8, mode));
        }
        assertEquals(3, Numbers.log2(10, RoundingMode.DOWN));
        assertEquals(3, Numbers.log2(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10, RoundingMode.UP));
        assertEquals(4, Numbers.log2(10, RoundingMode.CEILING));
        assertEquals(3, Numbers.log2(11, RoundingMode.HALF_DOWN));
        assertEquals(4, Numbers.log2(13, RoundingMode.HALF_DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log2(10, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testLog2_long() {
        assertEquals(0, Numbers.log2(1L, RoundingMode.UNNECESSARY));
        assertEquals(10, Numbers.log2(1024L, RoundingMode.UNNECESSARY));
        assertEquals(9, Numbers.log2(1000L, RoundingMode.FLOOR));
        assertEquals(60, Numbers.log2(1L << 60, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1L, RoundingMode.FLOOR));
    }

    @Test
    public void testLog2_double() {
        assertEquals(0.0, Numbers.log2(1.0), DELTA);
        assertEquals(1.0, Numbers.log2(2.0), DELTA);
        assertEquals(3.0, Numbers.log2(8.0), DELTA);
        assertEquals(-1.0, Numbers.log2(0.5), 0.000001);
        assertEquals(-2.0, Numbers.log2(0.25), 0.000001);
        assertEquals(10.0, Numbers.log2(1024.0), DELTA);
        assertTrue(Double.isNaN(Numbers.log2(-1.0)));
        assertTrue(Double.isNaN(Numbers.log2(Double.NaN)));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.log2(0.0), 0);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.log2(Double.POSITIVE_INFINITY), 0);
    }

    @Test
    public void testLog2_double_RoundingMode() {
        assertEquals(3, Numbers.log2(8.0, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(10.0, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(10.0, RoundingMode.CEILING));
        assertTrue(Numbers.log2(Double.MIN_VALUE, RoundingMode.FLOOR) < 0);
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(-1.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(Double.NaN, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log2(10.0, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testLog2_BigInteger() {
        assertEquals(0, Numbers.log2(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log2(BigInteger.TWO, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(BigInteger.valueOf(8), RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(BigInteger.valueOf(10), RoundingMode.DOWN));
        assertEquals(4, Numbers.log2(BigInteger.valueOf(10), RoundingMode.UP));
        assertEquals(4, Numbers.log2(BigInteger.valueOf(12), RoundingMode.HALF_EVEN));
        assertEquals(9, Numbers.log2(new BigInteger("1000"), RoundingMode.FLOOR));
        assertEquals(10, Numbers.log2(new BigInteger("1000"), RoundingMode.CEILING));

        final BigInteger pow2_100 = BigInteger.TWO.pow(100);
        assertEquals(100, Numbers.log2(pow2_100, RoundingMode.HALF_UP));
        assertEquals(100, Numbers.log2(pow2_100.add(BigInteger.ONE), RoundingMode.DOWN));
        assertEquals(101, Numbers.log2(pow2_100.add(BigInteger.ONE), RoundingMode.UP));
        assertEquals(300, Numbers.log2(BigInteger.TWO.pow(300), RoundingMode.HALF_EVEN));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(BigInteger.ZERO, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2(BigInteger.valueOf(-1), RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log2((BigInteger) null, RoundingMode.DOWN));
    }

    @Test
    public void testLog10_int() {
        assertEquals(0, Numbers.log10(1, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(100, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(100, RoundingMode.DOWN));
        assertEquals(2, Numbers.log10(200, RoundingMode.DOWN));
        assertEquals(3, Numbers.log10(200, RoundingMode.UP));
        assertEquals(1, Numbers.log10(99, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(101, RoundingMode.CEILING));
        assertEquals(2, Numbers.log10(123, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(-1, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(200, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testLog10_long() {
        assertEquals(0, Numbers.log10(1L, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log10(1000L, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log10(1234L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0L, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(-1L, RoundingMode.FLOOR));
    }

    @Test
    public void testLog10_double() {
        assertEquals(0.0, Numbers.log10(1.0), DELTA);
        assertEquals(1.0, Numbers.log10(10.0), DELTA);
        assertEquals(2.0, Numbers.log10(100.0), DELTA);
        assertEquals(-1.0, Numbers.log10(0.1), 0.0001);
        assertTrue(Double.isInfinite(Numbers.log10(0.0)));
        assertTrue(Double.isNaN(Numbers.log10(-1.0)));
    }

    @Test
    public void testLog10_double_RoundingMode() {
        for (final RoundingMode mode : RoundingMode.values()) {
            assertEquals(2, Numbers.log10(100.0, mode));
        }
        assertEquals(2, Numbers.log10(200.0, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(200.0, RoundingMode.CEILING));
        assertEquals(2, Numbers.log10(200.0, RoundingMode.DOWN));
        assertEquals(3, Numbers.log10(200.0, RoundingMode.UP));
        assertEquals(-1, Numbers.log10(0.2, RoundingMode.FLOOR));
        assertEquals(0, Numbers.log10(0.2, RoundingMode.CEILING));
        assertEquals(-1, Numbers.log10(0.1, RoundingMode.FLOOR));
        assertEquals(0, Numbers.log10(0.1, RoundingMode.CEILING));
        assertEquals(-324, Numbers.log10(Double.MIN_VALUE, RoundingMode.FLOOR));
        assertEquals(308, Numbers.log10(Double.MAX_VALUE, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(0.1, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(Double.NaN, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(1.0, null));
    }

    @Test
    public void testLog10_double_MatchesLegacy() {
        final List<Double> samples = new ArrayList<>();
        for (int k = 0; k <= 22; k++) {
            final double power = Math.pow(10, k);
            samples.add(power);
            samples.add(Math.nextUp(power));
            samples.add(Math.nextDown(power));
        }
        samples.add(0.1);
        samples.add(Double.MIN_VALUE);
        samples.add(Double.MAX_VALUE);
        final Random random = new Random(0x10610_6L);
        while (samples.size() < 20_000) {
            final double d = Double.longBitsToDouble(random.nextLong());
            if (d > 0.0 && Double.isFinite(d)) {
                samples.add(d);
            }
        }
        for (final double x : samples) {
            for (final RoundingMode mode : RoundingMode.values()) {
                final int expected;
                try {
                    expected = legacyLog10Rounded(x, mode);
                } catch (final ArithmeticException e) {
                    assertThrows(ArithmeticException.class, () -> Numbers.log10(x, mode), "x=" + x + ", mode=" + mode);
                    continue;
                }
                assertEquals(expected, Numbers.log10(x, mode), "x=" + x + ", mode=" + mode);
            }
        }
    }

    @Test
    public void testLog10_BigInteger() {
        assertEquals(0, Numbers.log10(BigInteger.ONE, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log10(BigInteger.TEN, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(BigInteger.valueOf(100), RoundingMode.UNNECESSARY));

        final BigInteger bigVal = BigInteger.TEN.pow(20);
        assertEquals(20, Numbers.log10(bigVal, RoundingMode.DOWN));
        assertEquals(20, Numbers.log10(bigVal.add(BigInteger.ONE), RoundingMode.DOWN));
        assertEquals(21, Numbers.log10(bigVal.add(BigInteger.ONE), RoundingMode.CEILING));

        final BigInteger val2 = BigInteger.TEN.pow(25).subtract(BigInteger.ONE);
        assertEquals(24, Numbers.log10(val2, RoundingMode.DOWN));
        assertEquals(25, Numbers.log10(val2, RoundingMode.CEILING));

        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(BigInteger.ZERO, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10((BigInteger) null, RoundingMode.DOWN));
    }
}
