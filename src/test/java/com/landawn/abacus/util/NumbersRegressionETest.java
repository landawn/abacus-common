package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Locks the behaviour changed by the 2026-09-01 review pass (fixes applied 2026-09-02):
 * <ul>
 *   <li><b>B1</b> {@code Double -> Float} is plain IEEE-754 narrowing, no longer a {@code toString} round trip;</li>
 *   <li><b>D1</b> the {@code create} family no longer routes a token by length, the value alone selects the type;</li>
 *   <li><b>D2</b> the per-thread {@code DecimalFormat} cache keeps only JDK types in the thread (class-loader leak);</li>
 *   <li><b>B2</b> a {@code null} value bypasses target validation in {@code convert}, as the Javadoc now says.</li>
 * </ul>
 */
public class NumbersRegressionETest extends TestBase {

    // ==================================================================================================
    // B1: Double -> Float is the IEEE-754 narrowing cast (ties to even)
    // ==================================================================================================

    /** The exact midpoint between {@code f} and the next float up, as a double (always exactly representable). */
    private static double midpointAbove(final float f) {
        return ((double) f + (double) Math.nextUp(f)) / 2.0d;
    }

    @Test
    @DisplayName("B1: 1.21d narrows to 1.21f, exactly as the cast does - the old rationale never needed a round trip")
    public void test_B1_ordinaryDoublesNarrowLikeTheCast() {
        assertEquals(1.21f, (float) 1.21d, 0.0f, "precondition: the cast alone already gives 1.21f");

        for (final double d : new double[] { 1.21d, 1.1d, 123.45d, 0.1d, -2.5d, 3.141592653589793d, 1e-45d, 1e38d, 1e39d, 1e300d, -1e300d, Double.MIN_VALUE,
                Double.MAX_VALUE, 0.0d, -0.0d }) {
            final float expected = (float) d;
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.toFloat(d)), "toFloat(Object) " + d);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.toFloat(d, 7f)), "toFloat(Object, default) " + d);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.convert(d, Float.class)), "convert(Float.class) " + d);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.convert(d, float.class)), "convert(float.class) " + d);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.convert(d, Type.of(Float.class))), "convert(Type) " + d);
        }
    }

    @Test
    @DisplayName("B1: at an exact float midpoint the result is the even significand, not whichever side the decimal spelling fell on")
    public void test_B1_midpointsRoundHalfToEven() {
        // 1 + 2^-24 lies exactly between 1.0f and 1.0000001f. Its shortest decimal spelling is slightly
        // above the midpoint, so the old Float.parseFloat(Double.toString(d)) route answered 1.0000001f
        // (odd significand); IEEE-754 ties-to-even answers 1.0f.
        final double m = midpointAbove(1.0f);
        assertEquals(1.0d + 0x1p-24, m, 0.0d);
        assertEquals(1.0000001f, Float.parseFloat(Double.toString(m)), 0.0f, "precondition: the decimal route breaks the tie upward");

        assertEquals(1.0f, Numbers.toFloat(m), 0.0f);
        assertEquals(1.0f, Numbers.convert(m, Float.class), 0.0f);
        assertEquals(1.0f, Numbers.convert(m, float.class), 0.0f);

        // Every midpoint, in both directions: the even significand wins, whatever the spelling does.
        final Random rnd = new Random(20260902L);
        int checked = 0, spellingDisagreed = 0;

        while (checked < 20_000) {
            final float f = Float.intBitsToFloat(rnd.nextInt());

            if (!Float.isFinite(f) || Math.abs(f) > 1e30f || Math.abs(f) < 1e-30f) {
                continue;
            }

            final double mid = midpointAbove(f);
            final float expected = (float) mid; // ties to even by JLS 5.1.3
            assertEquals(0, Float.floatToIntBits(expected) & 1, "the cast picks the even significand");
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.toFloat(mid)), "toFloat " + mid);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.convert(mid, Float.class)), "convert " + mid);

            if (Float.floatToIntBits(expected) != Float.floatToIntBits(Float.parseFloat(Double.toString(mid)))) {
                spellingDisagreed++;
            }

            checked++;
        }

        assertTrue(spellingDisagreed > 1000, "the decimal route must actually disagree on a large share of midpoints, saw " + spellingDisagreed);
    }

    @Test
    @DisplayName("B1: NaN, infinities and float-range overflow are preserved by the narrowing, as before")
    public void test_B1_nonFiniteAndSaturation() {
        assertTrue(Float.isNaN(Numbers.toFloat(Double.NaN)));
        assertTrue(Float.isNaN(Numbers.convert(Double.NaN, Float.class)));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.toFloat(Double.POSITIVE_INFINITY), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, Numbers.convert(Double.NEGATIVE_INFINITY, Float.class), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, Numbers.toFloat(1e300d), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, Numbers.convert(-1e300d, Float.class), 0.0f);
        assertEquals(0.0f, Numbers.toFloat(1e-50d), 0.0f);
        assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(Numbers.convert(-1e-50d, Float.class)));
    }

    @Test
    @DisplayName("B1: the Float -> Double direction is unchanged and still follows the decimal spelling")
    public void test_B1_floatToDoubleStillUsesTheDecimalSpelling() {
        assertEquals(1.21d, Numbers.toDouble(1.21f), 0.0d);
        assertEquals(1.21d, Numbers.convert(1.21f, Double.class), 0.0d);
        assertNotEquals(1.21d, 1.21f, "precondition: widening does not give 1.21");
        assertEquals(new BigDecimal("1.21"), Numbers.convert(1.21f, BigDecimal.class));
    }

    @Test
    @DisplayName("B1: a random sweep - toFloat(Object Double) and convert agree with the cast bit for bit")
    public void test_B1_randomSweepMatchesTheCast() {
        final Random rnd = new Random(1L);

        for (int i = 0; i < 200_000; i++) {
            final double d = Double.longBitsToDouble(rnd.nextLong());
            final int expected = Float.floatToIntBits((float) d);
            assertEquals(expected, Float.floatToIntBits(Numbers.toFloat(d)));
            assertEquals(expected, Float.floatToIntBits(Numbers.convert(d, Float.class)));
        }
    }

    // ==================================================================================================
    // D1: the create family has no length limit; the value alone selects the type
    // ==================================================================================================

    @Test
    @DisplayName("D1: a literal equal to 1.0 is a Double at 4096, 4097 and 40000 characters alike")
    public void test_D1_lengthNeverFlipsTheType() {
        for (final int zeros : new int[] { 10, 4093, 4094, 4095, 4096, 5000, 40_000 }) {
            final String s = "1." + "0".repeat(zeros);
            assertEquals(Double.valueOf(1.0d), Numbers.createNumber(s), "zeros=" + zeros);
            assertEquals(Double.valueOf(1.0d), Numbers.createNumber(s + "d"), "zeros=" + zeros);
            assertEquals(Double.valueOf(1.0d), Numbers.createNumber(s + "D"), "zeros=" + zeros);
            assertEquals(Float.valueOf(1.0f), Numbers.createNumber(s + "f"), "zeros=" + zeros);
            assertEquals(Float.valueOf(1.0f), Numbers.createNumber(s + "F"), "zeros=" + zeros);
            assertEquals(Double.valueOf(-1.0d), Numbers.createNumber("-" + s), "zeros=" + zeros);
            assertEquals(Double.valueOf(1.0d), Numbers.tryCreateNumber(s).orElseThrow(), "zeros=" + zeros);
            assertTrue(Numbers.isCreatable(s), "zeros=" + zeros);
        }

        // The digits past double precision are folded into the rounding, exactly as for a short token.
        final String third = "1." + "3".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 100);
        assertEquals(Double.valueOf(1.3333333333333333d), Numbers.createNumber(third));
        assertEquals(Float.valueOf(1.3333334f), Numbers.createNumber(third + "f"));
        assertEquals(Double.valueOf(Double.parseDouble("1." + "3".repeat(50))), Numbers.createNumber(third));
    }

    @Test
    @DisplayName("D1: over-long tokens still escalate to BigDecimal when, and only when, the value requires it")
    public void test_D1_valueStillSelectsBigDecimal() {
        final int limit = Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH;

        // Underflow to zero with non-zero digits present.
        final String tiny = "0." + "0".repeat(limit + 5) + "1";
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(tiny));
        assertEquals(0, new BigDecimal(tiny).compareTo((BigDecimal) Numbers.createNumber(tiny)));
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(tiny + "f"));
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(tiny + "d"));

        // Overflow to infinity.
        final String huge = "1" + "0".repeat(limit + 5) + ".5";
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(huge));
        assertEquals(0, new BigDecimal(huge).compareTo((BigDecimal) Numbers.createNumber(huge)));
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(huge + "d"));

        // A value that fits a double but not a float honours the f suffix by escalating one step.
        final String floatOverflow = "1" + "0".repeat(limit + 5) + "e-" + (limit + 5 - 39);
        assertEquals(1e39d, Numbers.createNumber(floatOverflow + "f").doubleValue(), 0.0d);
        assertInstanceOf(Double.class, Numbers.createNumber(floatOverflow + "f"));

        // An integral token of any length is a BigInteger.
        final String integral = "7" + "0".repeat(limit + 5);
        assertEquals(new BigInteger(integral), Numbers.createNumber(integral));
    }

    @Test
    @DisplayName("D1: a lexical zero of any length is the correctly signed binary zero, with or without a suffix")
    public void test_D1_overLongZeroKeepsSignAndType() {
        final String zero = "0." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 10);
        assertEquals(Double.valueOf(0.0d), Numbers.createNumber(zero));
        assertEquals(Double.valueOf(-0.0d), Numbers.createNumber("-" + zero));
        assertEquals(Float.valueOf(0.0f), Numbers.createNumber(zero + "f"));
        assertEquals(Float.valueOf(-0.0f), Numbers.createNumber("-" + zero + "f"));
        assertEquals(Double.valueOf(-0.0d), Numbers.createNumber("-" + zero + "d"));

        // A zero significand with an exponent too large for any BigDecimal is still a plain zero.
        final String hugeExponentZero = "0e" + "9".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 10);
        assertTrue(Numbers.isCreatable(hugeExponentZero));
        assertEquals(Double.valueOf(0.0d), Numbers.createNumber(hugeExponentZero));
        assertEquals(Double.valueOf(-0.0d), Numbers.createNumber("-" + hugeExponentZero));
        assertEquals(Float.valueOf(-0.0f), Numbers.createNumber("-" + hugeExponentZero + "f"));
        assertEquals(Double.valueOf(0.0d), Numbers.tryCreateNumber(hugeExponentZero).orElseThrow());
    }

    @Test
    @DisplayName("D1: the bounded parsers keep their limit; only the create family ignores it")
    public void test_D1_boundedParsersStillRejectOverLongInput() {
        final String overLimit = "1." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 1);
        assertEquals(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 1, overLimit.length());

        assertFalse(Numbers.isParsable(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.toFloat(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.toDouble(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.parseDouble(overLimit));
        assertTrue(Numbers.tryParseFloat(overLimit).isEmpty());
        assertTrue(Numbers.tryParseDouble(overLimit).isEmpty());
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDouble("x=" + overLimit));

        assertEquals(Double.valueOf(1.0d), Numbers.createNumber(overLimit));
        assertEquals(Double.valueOf(1.0d), Numbers.tryCreateNumber(overLimit).orElseThrow());
        assertEquals(0, BigDecimal.ONE.compareTo(Numbers.parseBigDecimal(overLimit)));
    }

    @Test
    @DisplayName("D1: createNumber(s) and createNumber(s + 'd') agree on the type for every floating shape, at every length")
    public void test_D1_suffixNeverChangesTheTypeOfAFloatingToken() {
        final List<String> shapes = new ArrayList<>();

        for (final int n : new int[] { 5, 308, 309, 4095, 4096, 4097, 8000 }) {
            shapes.add("1" + "0".repeat(n - 1) + "e-400");
            shapes.add("1" + "0".repeat(n - 1) + "e-1");
            shapes.add("1." + "0".repeat(n - 2));
            shapes.add("0." + "0".repeat(n - 2) + "1");
            shapes.add("0".repeat(n) + "1.5");
            shapes.add("1" + "0".repeat(n - 1) + ".5");
        }

        for (final String s : shapes) {
            assertSame(Numbers.createNumber(s).getClass(), Numbers.createNumber(s + "d").getClass(), "len=" + s.length() + " head=" + s.substring(0, 5));
            assertEquals(Numbers.createNumber(s), Numbers.tryCreateNumber(s).orElseThrow());
        }
    }

    // ==================================================================================================
    // D2: the DecimalFormat cache keeps working exactly as before, with a JDK-only thread-local value
    // ==================================================================================================

    @Test
    @DisplayName("D2: a FORMAT locale change is picked up on the next call and the old symbols do not linger")
    public void test_D2_localeChangeIsHonoured() {
        final Locale original = Locale.getDefault(Locale.Category.FORMAT);

        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            assertEquals("1,234.50", Numbers.format(1234.5d, "#,##0.00"));
            assertEquals("1,234.50", Numbers.format(1234.5d, "#,##0.00")); // cached instance

            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            assertEquals("1.234,50", Numbers.format(1234.5d, "#,##0.00"));
            assertEquals("1.234,00", Numbers.format(1234L, "#,##0.00"));

            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            assertEquals("1,234.50", Numbers.format(1234.5d, "#,##0.00"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
    }

    @Test
    @DisplayName("D2: rotating through more patterns than the cache holds keeps every answer correct")
    public void test_D2_evictionKeepsAnswersCorrect() {
        final Locale original = Locale.getDefault(Locale.Category.FORMAT);

        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);

            for (int round = 0; round < 3; round++) {
                for (int i = 0; i < 40; i++) {
                    final String pattern = "0." + "0".repeat(i % 12 + 1) + "'#" + i + "'";
                    final String expected = new java.text.DecimalFormat(pattern, java.text.DecimalFormatSymbols.getInstance(Locale.US)).format(1.5d);
                    assertEquals(expected, Numbers.format(1.5d, pattern), pattern);
                }

                // A pattern used on every iteration survives the burst (access order), and stays right.
                assertEquals("1.50", Numbers.format(1.5d, "0.00"));
            }
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
    }

    @Test
    @DisplayName("D2: the value a thread retains is made of JDK types only")
    public void test_D2_threadLocalValueIsJdkOnly() throws Exception {
        final java.lang.reflect.Field field = Numbers.class.getDeclaredField("THREAD_LOCAL_DECIMAL_FORMATS");
        field.setAccessible(true);
        final ThreadLocal<?> threadLocal = (ThreadLocal<?>) field.get(null);

        Numbers.format(1.5d, "0.00");
        final Object value = threadLocal.get();

        assertTrue(value instanceof Object[], "value class: " + value.getClass());
        final Object[] slots = (Object[]) value;
        assertEquals(2, slots.length);
        assertTrue(slots[0] == null || slots[0] instanceof Locale);
        assertSame(java.util.LinkedHashMap.class, slots[1].getClass(), "no library-defined subclass may be retained by the thread");

        for (final Object entry : ((java.util.Map<?, ?>) slots[1]).values()) {
            assertSame(java.text.DecimalFormat.class, entry.getClass());
        }
    }

    @Test
    @DisplayName("D2: a malformed pattern is still rejected before anything is cached")
    public void test_D2_malformedPatternIsNotCached() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(1, "'unterminated"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(1, "'unterminated"));
        assertEquals("1", Numbers.format(1, "0"));
    }

    // ==================================================================================================
    // B2: a null value takes the default without the target being validated
    // ==================================================================================================

    @Test
    @DisplayName("B2: convert(null, unusableTarget) is the documented null, convert(1, unusableTarget) still throws")
    public void test_B2_nullValueSkipsTargetValidation() {
        assertNull(Numbers.convert(null, Number.class));
        assertNull(Numbers.convert(null, AtomicInteger.class));
        assertNull(Numbers.convert(null, Type.of(Number.class)));
        assertEquals(Integer.valueOf(-1), Numbers.convert(null, Integer.class, -1));

        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(1, Number.class));
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(null, (Class<Integer>) null));
    }
}
