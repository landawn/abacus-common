package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Cycle-4 fixes of the 2026-09-02 iterative Numbers review (ledger
 * {@code scripts/cross_review/Numbers_ledger_2026-09-02.md}): C-013 ({@code round(x, scale)} did work
 * proportional to {@code 10^|scale|} for scales the value cannot use, and threw for extreme ones) and C-014
 * (the documented signed-zero and infinity rendering of the {@code format} family).
 */
public class NumbersRegressionHTest extends TestBase {

    private static final RoundingMode[] MODES = RoundingMode.values();

    private static final double[] DOUBLES = { 0.0, -0.0, 1.5, -1.5, 2.5, -2.5, 0.5, -0.5, 123.456, -123.456, 0.1, 0.3, 1.005, 2.675, 1e-5, 1e10, 1e22, 1e23,
            1e300, 12345.0, 9007199254740993.0, 1e-320, -1e-320, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE, -Double.MAX_VALUE };

    private static final float[] FLOATS = { 0.0f, -0.0f, 1.5f, -1.5f, 2.5f, -2.5f, 0.5f, -0.5f, 123.456f, -123.456f, 0.1f, 1.005f, 1e-5f, 1e10f, 1e30f,
            12345.0f, 1.0E-40f, -1.0E-40f, Float.MIN_VALUE, -Float.MIN_VALUE, Float.MIN_NORMAL, Float.MAX_VALUE, -Float.MAX_VALUE };

    private static void assertSameDouble(final double expected, final double actual, final String what) {
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual), what + ": expected " + expected + " but was " + actual);
    }

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

    // ==================================================================================================
    // C-013: a scale at or above the value's own scale is a no-op and must cost nothing
    // ==================================================================================================

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

    // ==================================================================================================
    // C-014: the format family's signed-zero and infinity rendering, now documented
    // ==================================================================================================

    @Test
    @DisplayName("C-014: a negative value that rounds to zero keeps its sign, and infinities render as the locale's infinity symbol")
    public void test_C014_formatSignedZeroAndInfinityAsDocumented() {
        final Locale original = Locale.getDefault(Locale.Category.FORMAT);

        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);

            assertEquals("-0.00", Numbers.format(-0.001, "0.00"));
            assertEquals("-0.00", Numbers.format(-0.001f, "0.00"));
            assertEquals("-0.00", Numbers.format(Double.valueOf(-0.001), "0.00"));
            assertEquals("-0.00", Numbers.format(Float.valueOf(-0.001f), "0.00"));
            assertEquals("-0", Numbers.format(-0.4, "0"));
            assertEquals("-0.00", Numbers.format(-0.0, "0.00"));
            assertEquals("-0.00", Numbers.format(-0.0f, "0.00"));
            assertEquals("0.00", Numbers.format(0.0, "0.00"));
            assertEquals("0.00", Numbers.format(0.001, "0.00"));

            // The documented recipe for a bare "0.00": round to the pattern's scale, then add 0.0.
            assertSameDouble(-0.0, Numbers.round(-0.001, 2), "round(-0.001, 2)");
            assertEquals("0.00", Numbers.format(Numbers.round(-0.001, 2) + 0.0, "0.00"));

            final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
            assertEquals("∞", symbols.getInfinity());
            assertEquals("NaN", symbols.getNaN());
            assertEquals("∞", Numbers.format(Double.POSITIVE_INFINITY, "0.00"));
            assertEquals("-∞", Numbers.format(Double.NEGATIVE_INFINITY, "0.00"));
            assertEquals("-∞", Numbers.format(Float.NEGATIVE_INFINITY, "0.00"));
            assertEquals("∞", Numbers.format(Float.valueOf(Float.POSITIVE_INFINITY), "#,##0.00"));
            assertEquals("NaN", Numbers.format(Double.NaN, "0.00"));

            // The documented asymmetry: an infinity still takes the pattern's prefix and suffix, a NaN takes
            // neither. (This is what the first draft of the policy bullet got wrong: it said only that the
            // digits are not applied, which reads as if the affixes were dropped too.)
            assertEquals("∞%", Numbers.format(Double.valueOf(Double.POSITIVE_INFINITY), "0.00%"));
            assertEquals("-∞%", Numbers.format(Double.NEGATIVE_INFINITY, "0.00%"));
            assertEquals("$∞", Numbers.format(Double.POSITIVE_INFINITY, "$#,##0.00"));
            assertEquals("-$∞", Numbers.format(Double.NEGATIVE_INFINITY, "$#,##0.00"));
            assertEquals("∞kg", Numbers.format(Float.POSITIVE_INFINITY, "0.00'kg'"));
            assertEquals("NaN", Numbers.format(Float.NaN, "0.00%"));
            assertEquals("NaN", Numbers.format(Double.NaN, "$#,##0.00"));
            assertEquals("NaN", Numbers.format(Float.NaN, "0.00'kg'"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
    }

    @Test
    @DisplayName("C-014: the class-level policy examples for signed zero hold in a locale with different symbols too")
    public void test_C014_signedZeroInAnotherLocale() {
        final Locale original = Locale.getDefault(Locale.Category.FORMAT);

        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            assertEquals("-0,00", Numbers.format(-0.001, "0.00"));
            assertEquals("-0", Numbers.format(-0.4, "0"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
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
}
