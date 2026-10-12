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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NumbersRoundingTest extends NumbersTestSupport {

    private static final RoundingMode[] MODES = RoundingMode.values();

    private static final double[] DOUBLES = { 0.0, -0.0, 1.5, -1.5, 2.5, -2.5, 0.5, -0.5, 123.456, -123.456, 0.1, 0.3, 1.005, 2.675, 1e-5, 1e10, 1e22, 1e23,
            1e300, 12345.0, 9007199254740993.0, 1e-320, -1e-320, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE, -Double.MAX_VALUE };

    private static final float[] FLOATS = { 0.0f, -0.0f, 1.5f, -1.5f, 2.5f, -2.5f, 0.5f, -0.5f, 123.456f, -123.456f, 0.1f, 1.005f, 1e-5f, 1e10f, 1e30f,
            12345.0f, 1.0E-40f, -1.0E-40f, Float.MIN_VALUE, -Float.MIN_VALUE, Float.MIN_NORMAL, Float.MAX_VALUE, -Float.MAX_VALUE };

    private static void assertSameFloat(final float expected, final float actual, final String what) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual), what + ": expected " + expected + " but was " + actual);
    }

    /** The pre-fix algorithm, exact for |scale| small enough to be affordable; the reference for the parity sweeps. */
    private static double referenceRound(final double x, final int scale, final RoundingMode mode) {
        final double rounded = BigDecimal.valueOf(x).setScale(scale, mode).doubleValue();
        return rounded == 0.0d ? Math.copySign(0.0d, x) : rounded;
    }

    private static float referenceRound(final float x, final int scale, final RoundingMode mode) {
        final float rounded = new BigDecimal(Float.toString(x)).setScale(scale, mode).floatValue();
        return rounded == 0.0f ? Math.copySign(0.0f, x) : rounded;
    }

    @Test
    @DisplayName("guard: the helper reference implementation agrees with the documented examples (so the parity sweep tests something real)")
    public void test_referenceImplementationSanity() {
        assertEquals(1.01, referenceRound(1.005, 2, RoundingMode.HALF_UP));
        assertSameDouble(-0.0, referenceRound(-0.004, 2, RoundingMode.HALF_UP), "-0.004");
        assertEquals(1.01f, referenceRound(1.005f, 2, RoundingMode.HALF_UP));

        try {
            referenceRound(3.14159, 2, RoundingMode.UNNECESSARY);
            fail("expected ArithmeticException");
        } catch (final ArithmeticException expected) {
            // ok
        }
    }

    // round

    @Test
    @DisplayName("C-013: the documented examples of the four round overloads still hold")
    public void test_C013_documentedExamplesStillHold() {
        assertEquals(3.14, Numbers.round(3.14159, 2));
        assertEquals(123.5, Numbers.round(123.456, 1));
        assertEquals(3.0, Numbers.round(2.5, 0));
        assertEquals(-3.0, Numbers.round(-2.5, 0));
        assertEquals(1.01, Numbers.round(1.005, 2));
        assertEquals(12300.0, Numbers.round(12345.0, -2));
        assertSameDouble(-0.0, Numbers.round(-0.004, 2), "-0.004");
        assertEquals(2.0, Numbers.round(2.5, 0, RoundingMode.HALF_DOWN));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.round(Double.MAX_VALUE, -308));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.round(Double.MAX_VALUE, -308, RoundingMode.UP));
        assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159, 2, RoundingMode.UNNECESSARY));
        assertEquals(3.14f, Numbers.round(3.14159f, 2));
        assertEquals(1.01f, Numbers.round(1.005f, 2));
        assertEquals(12300.0f, Numbers.round(12345.0f, -2));
        assertSameFloat(-0.0f, Numbers.round(-0.004f, 2), "-0.004f");
        assertEquals(Float.POSITIVE_INFINITY, Numbers.round(Float.MAX_VALUE, -35));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.round(Float.MAX_VALUE, -38, RoundingMode.UP));
        assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159f, 2, RoundingMode.UNNECESSARY));
        assertTrue(Float.isNaN(Numbers.round(Float.NaN, 2)));
        assertTrue(Double.isNaN(Numbers.round(Double.NaN, 2, RoundingMode.HALF_UP)));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.round(Double.NEGATIVE_INFINITY, 2));
    }

    @Test
    public void c156_halfDownAndHalfEvenAreZeroWhenTheUnitEqualsTwiceTheValue() {
        // unit 10 == 2|x| for x = 5: HALF_UP rounds away; HALF_DOWN and HALF_EVEN are already a signed zero
        assertEquals(10.0, Numbers.round(5.0, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -1, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -1, RoundingMode.HALF_EVEN), 0.0);
        assertEquals(-10.0, Numbers.round(-5.0, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.round(-5.0, -1, RoundingMode.HALF_DOWN)));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.round(-5.0, -1, RoundingMode.HALF_EVEN)));

        // unit 1 == 2|x| for x = 0.5
        assertEquals(1.0, Numbers.round(0.5, 0, RoundingMode.HALF_UP), 0.0);
        assertEquals(0.0, Numbers.round(0.5, 0, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(0.0, Numbers.round(0.5, 0, RoundingMode.HALF_EVEN), 0.0);

        // above the tie every HALF_* mode is still non-zero; once the unit exceeds 2|x| all of them are zero
        assertEquals(10.0, Numbers.round(5.1, -1, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(10.0, Numbers.round(5.1, -1, RoundingMode.HALF_EVEN), 0.0);
        assertEquals(0.0, Numbers.round(4.9, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -2, RoundingMode.HALF_UP), 0.0);

        // float likewise
        assertEquals(10.0f, Numbers.round(5.0f, -1, RoundingMode.HALF_UP), 0.0f);
        assertEquals(0.0f, Numbers.round(5.0f, -1, RoundingMode.HALF_DOWN), 0.0f);
        assertEquals(0.0f, Numbers.round(5.0f, -1, RoundingMode.HALF_EVEN), 0.0f);
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(Numbers.round(-5.0f, -1, RoundingMode.HALF_EVEN)));
        assertEquals(1.0f, Numbers.round(0.5f, 0, RoundingMode.HALF_UP), 0.0f);
        assertEquals(0.0f, Numbers.round(0.5f, 0, RoundingMode.HALF_EVEN), 0.0f);
    }

    @Test
    @DisplayName("C-013: inside the affordable range nothing changed -- bit-for-bit parity with the plain BigDecimal algorithm, exceptions included")
    public void test_C013_parityWithThePlainAlgorithmInTheOrdinaryRange() {
        final Random random = new Random(20260902);
        int compared = 0;

        for (int i = 0; i < 20_000; i++) {
            final double x = i < DOUBLES.length ? DOUBLES[i] : Double.longBitsToDouble(random.nextLong());

            if (!Double.isFinite(x)) {
                continue;
            }

            final int scale = random.nextInt(901) - 450;
            final RoundingMode mode = MODES[random.nextInt(MODES.length)];
            Double expected = null;

            try {
                expected = referenceRound(x, scale, mode);
            } catch (final ArithmeticException e) {
                assertEquals(RoundingMode.UNNECESSARY, mode);
            }

            if (expected == null) {
                assertThrows(ArithmeticException.class, () -> Numbers.round(x, scale, mode), "round(" + x + ", " + scale + ", " + mode + ")");
            } else {
                assertSameDouble(expected, Numbers.round(x, scale, mode), "round(" + x + ", " + scale + ", " + mode + ")");
            }

            compared++;
        }

        for (int i = 0; i < 20_000; i++) {
            final float x = i < FLOATS.length ? FLOATS[i] : Float.intBitsToFloat(random.nextInt());

            if (!Float.isFinite(x)) {
                continue;
            }

            final int scale = random.nextInt(901) - 450;
            final RoundingMode mode = MODES[random.nextInt(MODES.length)];
            Float expected = null;

            try {
                expected = referenceRound(x, scale, mode);
            } catch (final ArithmeticException e) {
                assertEquals(RoundingMode.UNNECESSARY, mode);
            }

            if (expected == null) {
                assertThrows(ArithmeticException.class, () -> Numbers.round(x, scale, mode), "round(" + x + "f, " + scale + ", " + mode + ")");
            } else {
                assertSameFloat(expected, Numbers.round(x, scale, mode), "round(" + x + "f, " + scale + ", " + mode + ")");
            }

            compared++;
        }

        assertTrue(compared > 39_000, "compared " + compared);
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
    public void testRound_SignOfZero() {
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-0.4, 0, RoundingMode.DOWN)));
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-0.3, 0, RoundingMode.HALF_UP)));
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.round(0.4, 0, RoundingMode.DOWN)));
        assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-0.3f, 0, RoundingMode.HALF_UP)));
        assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-0.0f, 2, RoundingMode.HALF_UP)));
        assertEquals(0, Float.floatToRawIntBits(Numbers.round(0.3f, 0, RoundingMode.HALF_UP)));
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
    public void c038_roundScaleRuleDouble() {
        // away-from-zero modes: every non-zero x is +-Infinity once scale <= -309
        for (final double x : new double[] { Double.MIN_VALUE, 1.0, 123.456, 1e300, Double.MAX_VALUE }) {
            assertEquals(Double.POSITIVE_INFINITY, Numbers.round(x, -309, RoundingMode.UP), 0.0, "x=" + x);
            assertEquals(Double.POSITIVE_INFINITY, Numbers.round(x, -309, RoundingMode.CEILING), 0.0, "x=" + x);
            assertEquals(Double.NEGATIVE_INFINITY, Numbers.round(-x, -309, RoundingMode.UP), 0.0, "x=" + x);
            assertEquals(Double.NEGATIVE_INFINITY, Numbers.round(-x, -309, RoundingMode.FLOOR), 0.0, "x=" + x);
            assertEquals(Double.POSITIVE_INFINITY, Numbers.round(x, Integer.MIN_VALUE, RoundingMode.UP), 0.0, "x=" + x);

            // toward-zero results and HALF_* are a signed zero there
            for (final RoundingMode mode : new RoundingMode[] { RoundingMode.DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN }) {
                assertEquals(0L, Double.doubleToRawLongBits(Numbers.round(x, -309, mode)), "x=" + x + " " + mode);
                assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-x, -309, mode)), "-x=" + -x + " " + mode);
            }

            assertEquals(0L, Double.doubleToRawLongBits(Numbers.round(x, -309, RoundingMode.FLOOR)), "x=" + x);
            assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-x, -309, RoundingMode.CEILING)), "x=" + x);
        }

        // -308 is not yet universal
        assertEquals(1.0E308, Numbers.round(1.0, -308, RoundingMode.UP), 0.0);
        assertEquals(1.0E308, Numbers.round(Double.MIN_VALUE, -308, RoundingMode.UP), 0.0);

        // small-scale thresholds: unit > |x| (DOWN etc.), unit > 2|x| (HALF_UP)
        assertEquals(0.0, Numbers.round(9.9, -1, RoundingMode.DOWN), 0.0);
        assertEquals(10.0, Numbers.round(10.0, -1, RoundingMode.DOWN), 0.0);
        assertEquals(0.0, Numbers.round(4.9, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(10.0, Numbers.round(5.0, -1, RoundingMode.HALF_UP), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -1, RoundingMode.HALF_DOWN), 0.0);
        assertEquals(0.0, Numbers.round(5.0, -1, RoundingMode.HALF_EVEN), 0.0);
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-9.9, -1, RoundingMode.CEILING)));

        // 2-arg (HALF_UP): far enough is always a signed zero; MAX_VALUE hits Infinity on the way
        assertEquals(0.0, Numbers.round(1.0, -1), 0.0);
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-1.0, -1)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.round(Double.MAX_VALUE, -308), 0.0);
        assertEquals(0.0, Numbers.round(Double.MAX_VALUE, -309), 0.0);
        assertEquals(0.0, Numbers.round(Double.MAX_VALUE, Integer.MIN_VALUE), 0.0);
    }

    @Test
    public void c038_roundScaleRuleFloat() {
        for (final float x : new float[] { Float.MIN_VALUE, 1.0f, 123.456f, 1e30f, Float.MAX_VALUE }) {
            assertEquals(Float.POSITIVE_INFINITY, Numbers.round(x, -39, RoundingMode.UP), 0.0f, "x=" + x);
            assertEquals(Float.POSITIVE_INFINITY, Numbers.round(x, -39, RoundingMode.CEILING), 0.0f, "x=" + x);
            assertEquals(Float.NEGATIVE_INFINITY, Numbers.round(-x, -39, RoundingMode.UP), 0.0f, "x=" + x);
            assertEquals(Float.NEGATIVE_INFINITY, Numbers.round(-x, -39, RoundingMode.FLOOR), 0.0f, "x=" + x);

            for (final RoundingMode mode : new RoundingMode[] { RoundingMode.DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN }) {
                assertEquals(0, Float.floatToRawIntBits(Numbers.round(x, -39, mode)), "x=" + x + " " + mode);
                assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-x, -39, mode)), "-x=" + -x + " " + mode);
            }

            assertEquals(0, Float.floatToRawIntBits(Numbers.round(x, -39, RoundingMode.FLOOR)), "x=" + x);
            assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-x, -39, RoundingMode.CEILING)), "x=" + x);
        }

        assertEquals(1.0E38f, Numbers.round(1.0f, -38, RoundingMode.UP), 0.0f);
        assertEquals(1.0E38f, Numbers.round(Float.MIN_VALUE, -38, RoundingMode.UP), 0.0f);
        assertEquals(0.0f, Numbers.round(1.0f, -1), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, Numbers.round(Float.MAX_VALUE, -35), 0.0f);
        assertEquals(3.0E38f, Numbers.round(Float.MAX_VALUE, -38), 0.0f);
        assertEquals(0.0f, Numbers.round(Float.MAX_VALUE, -39), 0.0f);
    }

    @Test
    public void c091_roundUnnecessaryKeepsTheBigDecimalExceptionAsCause() {
        final ArithmeticException d = assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159, 2, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, d.getMessage());
        assertInstanceOf(ArithmeticException.class, d.getCause());
        assertNotNull(d.getCause().getMessage());

        final ArithmeticException f = assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159f, 2, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, f.getMessage());
        assertInstanceOf(ArithmeticException.class, f.getCause());

        final ArithmeticException clamped = assertThrows(ArithmeticException.class, () -> Numbers.round(1.0, Integer.MIN_VALUE, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, clamped.getMessage());
        assertInstanceOf(ArithmeticException.class, clamped.getCause());

        // the failures detected by this class itself stay cause-less, with the same message
        final ArithmeticException divided = assertThrows(ArithmeticException.class, () -> Numbers.divide(7, 2, RoundingMode.UNNECESSARY));
        assertEquals(UNNECESSARY_MESSAGE, divided.getMessage());
        assertNull(divided.getCause());

        // exact values and the other modes are unaffected
        assertEquals(3.14, Numbers.round(3.14, 2, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(3.14, Numbers.round(3.14159, 2, RoundingMode.HALF_UP), 0.0);
    }

    @Test
    public void c068_roundUnnecessaryMessage() {
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159, 2, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(3.14159f, 2, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(1.5, 0, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(-0.5f, 0, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE, assertThrows(ArithmeticException.class, () -> Numbers.round(1e300, -301, RoundingMode.UNNECESSARY)).getMessage());
        assertEquals(UNNECESSARY_MESSAGE,
                assertThrows(ArithmeticException.class, () -> Numbers.round(1.0, Integer.MIN_VALUE, RoundingMode.UNNECESSARY)).getMessage());

        // exact at the requested scale: no throw, value unchanged, signed zero kept
        assertEquals(3.14, Numbers.round(3.14, 2, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(3.14f, Numbers.round(3.14f, 2, RoundingMode.UNNECESSARY), 0.0f);
        assertEquals(2.0, Numbers.round(2.0, 0, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(1.5, Numbers.round(1.5, 1, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(1e300, Numbers.round(1e300, -300, RoundingMode.UNNECESSARY), 0.0);
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.round(-0.0, 0, RoundingMode.UNNECESSARY)));
        assertEquals(Integer.MIN_VALUE, Float.floatToRawIntBits(Numbers.round(-0.0f, 3, RoundingMode.UNNECESSARY)));
        assertTrue(Double.isNaN(Numbers.round(Double.NaN, 0, RoundingMode.UNNECESSARY)));

        // the other modes are unaffected
        assertEquals(3.14, Numbers.round(3.14159, 2, RoundingMode.HALF_UP), 0.0);
        assertEquals(3.15f, Numbers.round(3.14159f, 2, RoundingMode.CEILING), 0.0f);
    }

    @Test
    @DisplayName("C-013: every scale at or below -400 behaves exactly as -400 does: zero with x's sign, +-Infinity, or the UNNECESSARY throw")
    public void test_C013_scaleBelowMinusFourHundredIsSaturated() {
        // Before the fix: round(1.5, -10_000_000) took over a second and round(1.5, Integer.MIN_VALUE)
        // threw ArithmeticException("Underflow").
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            final int[] scales = { -401, -1000, -100_000_000, Integer.MIN_VALUE };

            for (final double x : DOUBLES) {
                for (final RoundingMode mode : MODES) {
                    final String what = "round(" + x + ", s, " + mode + ")";
                    Double expected = null;

                    try {
                        expected = referenceRound(x, -400, mode);
                    } catch (final ArithmeticException e) {
                        assertEquals(RoundingMode.UNNECESSARY, mode, what);
                        assertTrue(x != 0.0d, what);
                    }

                    for (final int scale : scales) {
                        if (expected == null) {
                            assertThrows(ArithmeticException.class, () -> Numbers.round(x, scale, mode), what + " at " + scale);
                        } else {
                            assertSameDouble(expected, Numbers.round(x, scale, mode), what + " at " + scale);
                        }
                    }
                }
            }

            for (final float x : FLOATS) {
                for (final RoundingMode mode : MODES) {
                    final String what = "round(" + x + "f, s, " + mode + ")";
                    Float expected = null;

                    try {
                        expected = referenceRound(x, -400, mode);
                    } catch (final ArithmeticException e) {
                        assertEquals(RoundingMode.UNNECESSARY, mode, what);
                        assertTrue(x != 0.0f, what);
                    }

                    for (final int scale : scales) {
                        if (expected == null) {
                            assertThrows(ArithmeticException.class, () -> Numbers.round(x, scale, mode), what + " at " + scale);
                        } else {
                            assertSameFloat(expected, Numbers.round(x, scale, mode), what + " at " + scale);
                        }
                    }
                }
            }

            // The concrete outcomes the Javadoc now promises.
            assertSameDouble(0.0d, Numbers.round(1.5, -100_000_000), "1.5 to the nearest 10^100000000");
            assertSameDouble(-0.0d, Numbers.round(-1.5, Integer.MIN_VALUE), "-1.5 keeps its sign");
            assertSameDouble(Double.POSITIVE_INFINITY, Numbers.round(1.5, -100_000_000, RoundingMode.UP), "away from zero overflows");
            assertSameDouble(Double.NEGATIVE_INFINITY, Numbers.round(-1.5, Integer.MIN_VALUE, RoundingMode.FLOOR), "toward -Infinity overflows");
            assertSameDouble(Double.POSITIVE_INFINITY, Numbers.round(Double.MAX_VALUE, -400, RoundingMode.CEILING), "MAX_VALUE up");
            assertSameDouble(0.0d, Numbers.round(Double.MAX_VALUE, -400, RoundingMode.HALF_UP), "MAX_VALUE is below half of 10^400");
            assertSameFloat(0.0f, Numbers.round(1.5f, Integer.MIN_VALUE), "1.5f");
            assertSameFloat(Float.NEGATIVE_INFINITY, Numbers.round(-1.5f, Integer.MIN_VALUE, RoundingMode.FLOOR), "-1.5f toward -Infinity");
            assertThrows(ArithmeticException.class, () -> Numbers.round(1.5, Integer.MIN_VALUE, RoundingMode.UNNECESSARY));
            assertThrows(ArithmeticException.class, () -> Numbers.round(1.5f, Integer.MIN_VALUE, RoundingMode.UNNECESSARY));
            assertSameDouble(0.0d, Numbers.round(0.0d, Integer.MIN_VALUE, RoundingMode.UNNECESSARY), "zero never needs rounding");
            assertSameDouble(-0.0d, Numbers.round(-0.0d, Integer.MIN_VALUE, RoundingMode.UNNECESSARY), "negative zero never needs rounding");
            assertSameFloat(-0.0f, Numbers.round(-0.0f, Integer.MIN_VALUE, RoundingMode.UNNECESSARY), "negative zero never needs rounding (float)");
        });
    }

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
    public void testRoundToInt_OutOfRangeMessageNamesInputRoundedValueAndMode() {
        final ArithmeticException ex = assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(2147483647.5, RoundingMode.UP));
        assertEquals("not in range: 2.1474836475E9 rounded to 2.147483648E9 with rounding mode UP", ex.getMessage());

        // The very same input is in range under DOWN, so naming only the rounded value would be misleading.
        assertEquals(2147483647, Numbers.roundToInt(2147483647.5, RoundingMode.DOWN));

        final ArithmeticException negative = assertThrows(ArithmeticException.class, () -> Numbers.roundToInt(-2147483648.5, RoundingMode.UP));
        assertEquals("not in range: -2.1474836485E9 rounded to -2.147483649E9 with rounding mode UP", negative.getMessage());
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
    public void testRoundToLong_OutOfRangeMessageNamesInputRoundedValueAndMode() {
        final ArithmeticException ex = assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(9.223372036854776E18, RoundingMode.DOWN));
        assertEquals("not in range: 9.223372036854776E18 rounded to 9.223372036854776E18 with rounding mode DOWN", ex.getMessage());

        final ArithmeticException negative = assertThrows(ArithmeticException.class, () -> Numbers.roundToLong(-9.3E18, RoundingMode.FLOOR));
        assertEquals("not in range: -9.3E18 rounded to -9.3E18 with rounding mode FLOOR", negative.getMessage());
    }

    // C-013: a scale at or above the value's own scale is a no-op and must cost nothing
    @Test
    @DisplayName("C-013: a scale at or above the value's own decimal scale returns x itself, at every scale up to Integer.MAX_VALUE, in every mode")
    public void test_C013_scaleAtOrAboveTheValuesScaleIsANoOp() {
        // Before the fix: round(1.5, 10_000_000) took seconds and round(1.5, Integer.MAX_VALUE) threw
        // ArithmeticException("BigInteger would overflow supported range"). The whole loop below is now
        // a few milliseconds; the timeout turns a regression into a failure instead of a hang.
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            for (final double x : DOUBLES) {
                final int own = BigDecimal.valueOf(x).scale();
                final int[] scales = { own, own + 1, 400, 1100, 100_000_000, Integer.MAX_VALUE };

                for (final int scale : scales) {
                    for (final RoundingMode mode : MODES) {
                        // UNNECESSARY included: padding zeros never needs rounding.
                        assertSameDouble(x, Numbers.round(x, scale, mode), "round(" + x + ", " + scale + ", " + mode + ")");
                    }

                    assertSameDouble(x, Numbers.round(x, scale), "round(" + x + ", " + scale + ")");
                }
            }

            for (final float x : FLOATS) {
                final int own = new BigDecimal(Float.toString(x)).scale();
                final int[] scales = { own, own + 1, 400, 1100, 100_000_000, Integer.MAX_VALUE };

                for (final int scale : scales) {
                    for (final RoundingMode mode : MODES) {
                        assertSameFloat(x, Numbers.round(x, scale, mode), "round(" + x + "f, " + scale + ", " + mode + ")");
                    }

                    assertSameFloat(x, Numbers.round(x, scale), "round(" + x + "f, " + scale + ")");
                }
            }
        });
    }
}
