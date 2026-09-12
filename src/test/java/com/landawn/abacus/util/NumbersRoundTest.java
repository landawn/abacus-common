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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class NumbersRoundTest extends NumbersTestSupport {

    @Test
    public void testRoundIntermediate() {
        assertEquals(2.7, Numbers.roundIntermediate(2.7, RoundingMode.FLOOR), 0.0);
        assertEquals(-3.0, Numbers.roundIntermediate(-2.3, RoundingMode.FLOOR), 0.0);
        assertEquals(-2.3, Numbers.roundIntermediate(-2.3, RoundingMode.CEILING), 0.0);
        assertEquals(3.0, Numbers.roundIntermediate(2.3, RoundingMode.CEILING), 0.0);
        assertEquals(2.7, Numbers.roundIntermediate(2.7, RoundingMode.DOWN), 0.0);
        assertEquals(3.0, Numbers.roundIntermediate(2.3, RoundingMode.UP), 0.0);
        assertEquals(-3.0, Numbers.roundIntermediate(-2.3, RoundingMode.UP), 0.0);
        assertEquals(2.0, Numbers.roundIntermediate(2.3, RoundingMode.HALF_EVEN), 0.0);
        assertEquals(3.0, Numbers.roundIntermediate(2.5, RoundingMode.HALF_UP), 0.0);
        assertEquals(2.5, Numbers.roundIntermediate(2.5, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(3.0, Numbers.roundIntermediate(3.0, RoundingMode.UNNECESSARY), 0.0);
        assertThrows(ArithmeticException.class, () -> Numbers.roundIntermediate(2.5, RoundingMode.UNNECESSARY));
        assertThrows(ArithmeticException.class, () -> Numbers.roundIntermediate(Double.POSITIVE_INFINITY, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.roundIntermediate(Double.NaN, RoundingMode.DOWN));
    }

    @Test
    public void testRound_float() {
        assertEquals(12.11f, Numbers.round(12.105f, 2), 0.001f);
        assertEquals(12.1f, Numbers.round(12.105f, 1), 0.001f);
        assertEquals(12.0f, Numbers.round(12.105f, 0), 0.001f);
        assertEquals(10.0f, Numbers.round(12.105f, -1), 0.001f);
        assertEquals(1.24f, Numbers.round(1.235f, 2), FLOAT_DELTA);
        assertEquals(-1.24f, Numbers.round(-1.235f, 2), FLOAT_DELTA);
        assertEquals(1.3f, Numbers.round(1.25f, 1), 0.0001f);
        assertEquals(8.24f, Numbers.round(8.235f, 2), 0.0f);
        assertEquals(1.05f, Numbers.round(1.045f, 2), 0.0f);
        assertEquals(1e20f, Numbers.round(1e20f, 0), 0.0f);
        assertEquals(-1e20f, Numbers.round(-1e20f, 0), 0.0f);
        assertEquals(0x1p63f, Numbers.round(0x1p63f, 0), 0.0f);
        assertEquals(1e18f, Numbers.round(1e18f, 6), 1e12f);
    }

    @Test
    public void testRound_double() {
        assertEquals(12.11, Numbers.round(12.105, 2), 0.001);
        assertEquals(12.0, Numbers.round(12.105, 0), 0.001);
        assertEquals(10.0, Numbers.round(12.105, -1), 0.001);
        assertEquals(12.35, Numbers.round(12.3456, 2), 1e-10);
        assertEquals(1.01, Numbers.round(1.005, 2), 0.0);
        assertEquals(1.02, Numbers.round(1.015, 2), 0.0);
        assertEquals(1.3, Numbers.round(1.25, 1), 0.0001);
        assertEquals(1e20, Numbers.round(1e20, 0), 0.0);
        assertEquals(-1e20, Numbers.round(-1e20, 0), 0.0);
        assertEquals(0x1p63, Numbers.round(0x1p63, 0), 0.0);
        assertEquals(1e17, Numbers.round(1e17, 2), 1e6);
        assertEquals(9.3e12, Numbers.round(9.3e12, 6), 1.0);
        assertEquals(4.0, Numbers.round(3.5, 0), 0.0);
        assertEquals(-3.0, Numbers.round(-3.4, 0), 0.0);
    }

    @Test
    public void testRound_scaleMatchesHalfUp() {
        for (final double x : new double[] { 1.005, 1.015, 2.675, 8.235, 12.3456, -1.005, 0.0, 123456.789 }) {
            for (int scale = -2; scale <= 6; scale++) {
                assertEquals(Numbers.round(x, scale, RoundingMode.HALF_UP), Numbers.round(x, scale), 0.0);
            }
        }
        for (final float x : new float[] { 1.005f, 8.235f, 1.045f, 12.345f, -8.235f, 0.0f }) {
            for (int scale = -2; scale <= 6; scale++) {
                assertEquals(Numbers.round(x, scale, RoundingMode.HALF_UP), Numbers.round(x, scale), 0.0f);
            }
        }
    }

    @Test
    public void testRound_RoundingMode() {
        assertEquals(12.11f, Numbers.round(12.105f, 2, RoundingMode.HALF_UP), 0.001f);
        assertEquals(12.10f, Numbers.round(12.105f, 2, RoundingMode.DOWN), 0.001f);
        assertEquals(12.11f, Numbers.round(12.105f, 2, RoundingMode.UP), 0.001f);
        assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.DOWN), 0.0001f);
        assertEquals(1.3f, Numbers.round(1.25f, 1, RoundingMode.UP), 0.0001f);
        assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.HALF_DOWN), 0.0001f);
        assertEquals(1.3f, Numbers.round(1.25f, 1, RoundingMode.HALF_UP), 0.0001f);
        assertEquals(1.2f, Numbers.round(1.25f, 1, RoundingMode.HALF_EVEN), 0.0001f);
        assertThrows(IllegalArgumentException.class, () -> Numbers.round(1.25f, 1, null));

        assertEquals(12.11, Numbers.round(12.105, 2, RoundingMode.HALF_UP), 0.001);
        assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.DOWN), 0.0001);
        assertEquals(1.3, Numbers.round(1.25, 1, RoundingMode.UP), 0.0001);
        assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.HALF_DOWN), 0.0001);
        assertEquals(1.3, Numbers.round(1.25, 1, RoundingMode.HALF_UP), 0.0001);
        assertEquals(1.2, Numbers.round(1.25, 1, RoundingMode.HALF_EVEN), 0.0001);
        assertEquals(12.34, Numbers.round(12.345, 2, RoundingMode.HALF_DOWN), 1e-10);
        assertEquals(12.35, Numbers.round(12.345, 2, RoundingMode.HALF_UP), 1e-10);
        assertEquals(3.14f, Numbers.round(3.145f, 2, RoundingMode.HALF_EVEN), FLOAT_DELTA);
        assertEquals(3.15f, Numbers.round(3.141f, 2, RoundingMode.CEILING), FLOAT_DELTA);
        assertEquals(3.14f, Numbers.round(3.149f, 2, RoundingMode.FLOOR), FLOAT_DELTA);
        assertEquals(-3.0, Numbers.round(-2.5, 0, RoundingMode.HALF_UP), 0.0);
        assertEquals(3.0, Numbers.round(2.5, 0, RoundingMode.HALF_UP), 0.0);
        assertEquals(2.0d, Numbers.round(2.5d, 0, RoundingMode.HALF_EVEN), 0.0d);
        assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159d, 2, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testRound_NaNAndInfinity() {
        for (final int scale : new int[] { -2, 0, 2, 6, 7, 8 }) {
            assertTrue(Float.isNaN(Numbers.round(Float.NaN, scale)));
            assertTrue(Double.isNaN(Numbers.round(Double.NaN, scale)));
            assertEquals(Float.POSITIVE_INFINITY, Numbers.round(Float.POSITIVE_INFINITY, scale), 0.0f);
            assertEquals(Float.NEGATIVE_INFINITY, Numbers.round(Float.NEGATIVE_INFINITY, scale), 0.0f);
            assertEquals(Double.POSITIVE_INFINITY, Numbers.round(Double.POSITIVE_INFINITY, scale), 0.0d);
            assertEquals(Double.NEGATIVE_INFINITY, Numbers.round(Double.NEGATIVE_INFINITY, scale), 0.0d);
            assertTrue(Float.isNaN(Numbers.round(Float.NaN, scale, RoundingMode.HALF_UP)));
            assertTrue(Double.isNaN(Numbers.round(Double.NaN, scale, RoundingMode.HALF_EVEN)));
            assertEquals(Float.POSITIVE_INFINITY, Numbers.round(Float.POSITIVE_INFINITY, scale, RoundingMode.DOWN), 0.0f);
            assertEquals(Double.NEGATIVE_INFINITY, Numbers.round(Double.NEGATIVE_INFINITY, scale, RoundingMode.FLOOR), 0.0d);
        }
    }

    @Test
    public void testRound_SignOfZero() {
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-0.4, 0, RoundingMode.DOWN)));
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-0.3, 0, RoundingMode.HALF_UP)));
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.round(0.4, 0, RoundingMode.DOWN)));
        assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-0.3f, 0, RoundingMode.HALF_UP)));
        assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-0.0f, 2, RoundingMode.HALF_UP)));
        assertEquals(0, Float.floatToRawIntBits(Numbers.round(0.3f, 0, RoundingMode.HALF_UP)));
    }

    @Test
    public void testRound_ExtremeScale() {
        assertEquals(3.0e38f, Numbers.round(Float.MAX_VALUE, -38), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, Numbers.round(Float.MAX_VALUE, -35), 0.0f);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.round(Double.MAX_VALUE, -308, RoundingMode.UP), 0.0d);
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.round(-Double.MAX_VALUE, -308, RoundingMode.UP), 0.0d);
        assertEquals(0.0d, Numbers.round(1.0d, Integer.MIN_VALUE, RoundingMode.HALF_UP), 0.0d);
        assertEquals(1.0d, Numbers.round(1.0d, Integer.MAX_VALUE), 0.0d);
        assertEquals(0.0d, Numbers.round(1.0d, Integer.MIN_VALUE), 0.0d);
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-1.0d, Integer.MIN_VALUE)));
        assertDoesNotThrow(() -> Numbers.round(1.0d, Integer.MAX_VALUE));
        assertDoesNotThrow(() -> Numbers.round(Double.NaN, Integer.MIN_VALUE));
    }

    @Test
    public void testRound_vsFormat() {
        final Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            assertEquals(12.11f, Numbers.round(12.105f, 2), 0.0f);
            assertEquals("12.10", Numbers.format(12.105f, "0.00"));
            assertEquals(1.01f, Numbers.round(1.005f, 2), 0.0f);
            assertEquals("1.00", Numbers.format(1.005f, "0.00"));
            assertEquals(3.0d, Numbers.round(2.5d, 0), 0.0d);
            assertEquals("2", Numbers.format(2.5d, "0"));
            assertEquals(0.13d, Numbers.round(0.125d, 2), 0.0d);
            assertEquals("0.12", Numbers.format(0.125d, "0.00"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }

    @Test
    public void testRoundToInt() {
        assertEquals(13, Numbers.roundToInt(12.5, RoundingMode.HALF_UP));
        assertEquals(13, Numbers.roundToInt(12.5, RoundingMode.CEILING));
        assertEquals(12, Numbers.roundToInt(12.5, RoundingMode.FLOOR));
        assertEquals(5, Numbers.roundToInt(4.5, RoundingMode.HALF_UP));
        assertEquals(4, Numbers.roundToInt(4.5, RoundingMode.HALF_DOWN));
        assertEquals(Integer.MAX_VALUE, Numbers.roundToInt(2147483647.0, RoundingMode.UNNECESSARY));
        assertEquals(Integer.MIN_VALUE, Numbers.roundToInt(-2147483648.0, RoundingMode.UNNECESSARY));
        assertEquals(Integer.MAX_VALUE, Numbers.roundToInt(2147483647.9, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(Double.POSITIVE_INFINITY, RoundingMode.HALF_UP));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(Double.NaN, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(Integer.MAX_VALUE + 1.0, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(2147483648.0, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(Double.MAX_VALUE, RoundingMode.DOWN));
    }

    @Test
    public void testRoundToLong() {
        assertEquals(13L, Numbers.roundToLong(12.5, RoundingMode.HALF_UP));
        assertEquals(6L, Numbers.roundToLong(5.5, RoundingMode.UP));
        assertEquals(5L, Numbers.roundToLong(5.5, RoundingMode.DOWN));
        assertEquals(6L, Numbers.roundToLong(5.5, RoundingMode.HALF_UP));
        assertEquals(5L, Numbers.roundToLong(5.0, RoundingMode.UNNECESSARY));
        assertEquals(-6L, Numbers.roundToLong(-5.5, RoundingMode.UP));
        assertEquals(9223372036854774784L, Numbers.roundToLong(9223372036854774784.0, RoundingMode.DOWN));
        assertEquals(Long.MIN_VALUE, Numbers.roundToLong(-9.223372036854776E18, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Double.POSITIVE_INFINITY, RoundingMode.HALF_UP));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Double.NaN, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(Long.MAX_VALUE + 100.0, RoundingMode.FLOOR));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(9.223372036854776E18, RoundingMode.DOWN));
    }

    @Test
    public void testRoundToBigInteger() {
        assertEquals(BigInteger.valueOf(13), Numbers.roundToBigInteger(12.5, RoundingMode.HALF_UP));
        assertEquals(BigInteger.valueOf(6), Numbers.roundToBigInteger(5.5, RoundingMode.UP));
        assertEquals(BigInteger.valueOf(5), Numbers.roundToBigInteger(5.5, RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(-6), Numbers.roundToBigInteger(-5.5, RoundingMode.UP));
        assertTrue(Numbers.roundToBigInteger(1e20, RoundingMode.DOWN).compareTo(BigInteger.ZERO) > 0);
        assertThrows(ArithmeticException.class, () -> Numbers.roundToBigInteger(Double.POSITIVE_INFINITY, RoundingMode.HALF_UP));
        assertThrows(ArithmeticException.class, () -> Numbers.roundToBigInteger(Double.NaN, RoundingMode.FLOOR));
    }

    @Test
    public void testRoundToInt_OutOfRangeMessageNamesInputRoundedValueAndMode() {
        final ArithmeticException ex = assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(2147483647.5, RoundingMode.UP));
        assertEquals("not in range: 2.1474836475E9 rounded to 2.147483648E9 with rounding mode UP", ex.getMessage());

        // The very same input is in range under DOWN, so naming only the rounded value would be misleading.
        assertEquals(2147483647, Numbers.roundToInt(2147483647.5, RoundingMode.DOWN));

        final ArithmeticException negative = assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(-2147483648.5, RoundingMode.UP));
        assertEquals("not in range: -2.1474836485E9 rounded to -2.147483649E9 with rounding mode UP", negative.getMessage());
    }

    @Test
    public void testRoundToLong_OutOfRangeMessageNamesInputRoundedValueAndMode() {
        final ArithmeticException ex = assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(9.223372036854776E18, RoundingMode.DOWN));
        assertEquals("not in range: 9.223372036854776E18 rounded to 9.223372036854776E18 with rounding mode DOWN", ex.getMessage());

        final ArithmeticException negative = assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(-9.3E18, RoundingMode.FLOOR));
        assertEquals("not in range: -9.3E18 rounded to -9.3E18 with rounding mode FLOOR", negative.getMessage());
    }
}
