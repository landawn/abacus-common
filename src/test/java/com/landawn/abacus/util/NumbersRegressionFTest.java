package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Locks the behaviour changed by the 2026-09-02 review pass (ninth pass over {@code Numbers}):
 * <ul>
 *   <li><b>B1</b> a {@code Number} subtype whose {@code toString()} is not usable text is converted to
 *       {@code BigInteger} by truncating the <em>exact</em> {@code doubleValue()}, exactly as the {@code Long}
 *       target and a {@code Double} source are &mdash; no longer by truncating the shortest-decimal rendering,
 *       which above 2<sup>53</sup> is not the value and made {@code convert(x, BigInteger.class)} disagree with
 *       {@code convert(x, Long.class)} for 73.7% of the doubles in (2<sup>53</sup>, 2<sup>63</sup>);</li>
 *   <li><b>D1/D2</b> the {@code Float -> Double} decimal-spelling widening and the {@code double -> BigInteger}
 *       exact truncation each live in one helper shared by {@code toDouble(Object)}/the {@code convert} table
 *       and by the table/the unknown-source fallback respectively, so this class pins that the two call sites
 *       of each agree on every special value;</li>
 *   <li><b>D4</b> {@code tryParseFloat}/{@code tryParseDouble}/{@code tryCreateNumber} use the same emptiness
 *       test as every other entry point.</li>
 * </ul>
 */
public class NumbersRegressionFTest extends TestBase {

    /**
     * A {@code Number} this class does not recognize, whose {@code toString()} is never usable as a decimal
     * token, so every integral and arbitrary-precision target must fall back to {@code doubleValue()}.
     */
    private static final class OpaqueNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final double value;
        private final String text;

        OpaqueNumber(final double value, final String text) {
            this.value = value;
            this.text = text;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return (long) value;
        }

        @Override
        public float floatValue() {
            return (float) value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** Every text shape that the recovery must reject as "not a decimal token". */
    private static final String[] UNUSABLE_TEXTS = { null, "", "N/A", "1,000,000", " 12", "12 ms", "0x10", "1e", "--1", "1.2.3" };

    // ==================================================================================================
    // B1: unknown-Number fallback, BigInteger target == Long target == Double source, above 2^53
    // ==================================================================================================

    @Test
    @DisplayName("B1: above 2^53 the BigInteger target truncates the exact doubleValue(), as the Long target does")
    public void test_B1_unusableText_bigIntegerTargetTruncatesTheExactDouble() {
        // Each of these is exactly representable as a double and is NOT its own shortest decimal:
        // 1.0000000000000001E18 renders as ...0100, the value is ...0128.
        final double[] values = { 1000000000000000128.0d, 4611686018427388928.0d, -4611686018427388928.0d, 9007199254740994.0d, -1000000000000000128.0d,
                Math.nextDown(0x1p63), -0x1p63 };

        for (final double d : values) {
            for (final String text : UNUSABLE_TEXTS) {
                final OpaqueNumber source = new OpaqueNumber(d, text);
                final String why = d + " / " + text;

                final long asLong = Numbers.convert(source, Long.class);
                final BigInteger asBig = Numbers.convert(source, BigInteger.class);

                assertEquals((long) d, asLong, why);
                assertEquals(BigInteger.valueOf(asLong), asBig, why);
                assertEquals(new BigDecimal(d).toBigInteger(), asBig, why);
                // ... and exactly what a Double source of the same value gives.
                assertEquals(Numbers.convert(Double.valueOf(d), BigInteger.class), asBig, why);
                assertEquals(Numbers.convert(Double.valueOf(d), Long.class), asLong, why);
                assertEquals(Numbers.toLong(source), asLong, why);
            }
        }
    }

    @Test
    @DisplayName("B1: the concrete pre-fix failure -- a BigInteger larger than the source it was truncated from")
    public void test_B1_theOldAnswerExceededTheSource() {
        final OpaqueNumber source = new OpaqueNumber(4611686018427388928.0d, "n/a");

        assertEquals(new BigInteger("4611686018427388928"), Numbers.convert(source, BigInteger.class));
        // The value the old spelling route produced; larger than the source, so not a truncation at all.
        assertFalse(new BigInteger("4611686018427389000").equals(Numbers.convert(source, BigInteger.class)));
    }

    @Test
    @DisplayName("B1: random sweep over (2^53, 2^63), both signs -- Long, BigInteger and Double-source agree")
    public void test_B1_randomSweepAgreesAcrossTargets() {
        final Random random = new Random(20260902L);

        for (int i = 0; i < 20_000; i++) {
            double d = Math.scalb(1.0 + random.nextDouble(), 53 + random.nextInt(10));
            if (random.nextBoolean()) {
                d = -d;
            }

            final OpaqueNumber source = new OpaqueNumber(d, "n/a");
            final BigInteger asBig = Numbers.convert(source, BigInteger.class);

            assertEquals(BigInteger.valueOf(Numbers.convert(source, Long.class)), asBig, String.valueOf(d));
            assertEquals(Numbers.convert(Double.valueOf(d), BigInteger.class), asBig, String.valueOf(d));
        }
    }

    @Test
    @DisplayName("B1: beyond the long range the BigInteger target is still the exact double, not its spelling")
    public void test_B1_beyondLongRangeIsExact() {
        for (final double d : new double[] { 1e30d, -1e30d, 0x1p63, 1.7976931348623157E308, 1e19d }) {
            for (final String text : UNUSABLE_TEXTS) {
                final OpaqueNumber source = new OpaqueNumber(d, text);
                final String why = d + " / " + text;

                assertEquals(new BigDecimal(d).toBigInteger(), Numbers.convert(source, BigInteger.class), why);
                assertEquals(Numbers.convert(Double.valueOf(d), BigInteger.class), Numbers.convert(source, BigInteger.class), why);
                assertThrows(ArithmeticException.class, () -> Numbers.convert(source, Long.class), why);
            }
        }

        // Named: the double 1e30 is not 10^30.
        assertEquals(new BigInteger("1000000000000000019884624838656"), Numbers.convert(new OpaqueNumber(1e30d, "n/a"), BigInteger.class));
    }

    @Test
    @DisplayName("B1: the BigDecimal target keeps its own rule -- the canonical decimal spelling of the fallback double")
    public void test_B1_bigDecimalTargetStillUsesTheCanonicalSpelling() {
        for (final double d : new double[] { 1000000000000000128.0d, 4611686018427388928.0d, 12.9d, 1e30d, 0.1d, -0.0d }) {
            for (final String text : UNUSABLE_TEXTS) {
                final OpaqueNumber source = new OpaqueNumber(d, text);
                final String why = d + " / " + text;

                assertEquals(BigDecimal.valueOf(d), Numbers.convert(source, BigDecimal.class), why);
                assertEquals(Numbers.convert(Double.valueOf(d), BigDecimal.class), Numbers.convert(source, BigDecimal.class), why);
            }
        }

        // The split is deliberate and documented: above 2^53, spelling.toBigInteger() != exact truncation.
        final OpaqueNumber source = new OpaqueNumber(4611686018427388928.0d, "n/a");
        assertFalse(Numbers.convert(source, BigDecimal.class).toBigInteger().equals(Numbers.convert(source, BigInteger.class)));
    }

    @Test
    @DisplayName("B1: below one the fallback truncates to ZERO without building the exact expansion; -0.0 included")
    public void test_B1_magnitudeBelowOneIsZero() {
        for (final double d : new double[] { 0.0d, -0.0d, 0.5d, -0.999999d, Double.MIN_VALUE, -Double.MIN_VALUE, Math.nextDown(1.0d), -Math.nextDown(1.0d) }) {
            for (final String text : UNUSABLE_TEXTS) {
                final OpaqueNumber source = new OpaqueNumber(d, text);
                assertSame(BigInteger.ZERO, Numbers.convert(source, BigInteger.class), d + " / " + text);
                assertEquals(0L, Numbers.convert(source, Long.class), d + " / " + text);
            }
        }

        // Exactly one is not below one.
        assertEquals(BigInteger.ONE, Numbers.convert(new OpaqueNumber(1.0d, "n/a"), BigInteger.class));
        assertEquals(BigInteger.ONE.negate(), Numbers.convert(new OpaqueNumber(-1.0d, "n/a"), BigInteger.class));
    }

    @Test
    @DisplayName("B1: a non-finite fallback is rejected as BigInteger overflow, with the bounded description")
    public void test_B1_nonFiniteFallbackIsOverflow() {
        for (final double d : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            final ArithmeticException named = assertThrows(ArithmeticException.class, () -> Numbers.convert(new OpaqueNumber(d, "n/a"), BigInteger.class));
            assertEquals("BigInteger overflow: n/a", named.getMessage());

            final ArithmeticException nullText = assertThrows(ArithmeticException.class, () -> Numbers.convert(new OpaqueNumber(d, null), BigInteger.class));
            assertTrue(nullText.getMessage().startsWith("BigInteger overflow: "), nullText.getMessage());
            assertTrue(nullText.getMessage().endsWith("(toString() returned null)"), nullText.getMessage());

            // A huge toString() is previewed, never embedded (the message stays bounded and the text is read once).
            final ArithmeticException huge = assertThrows(ArithmeticException.class,
                    () -> Numbers.convert(new OpaqueNumber(d, "Q".repeat(100_000)), BigInteger.class));
            assertTrue(huge.getMessage().length() < 300, String.valueOf(huge.getMessage().length()));
            assertTrue(huge.getMessage().contains("...[100000 chars]"), huge.getMessage());
        }
    }

    @Test
    @DisplayName("B1: a USABLE text is still exact and still wins over doubleValue(), on both arbitrary-precision targets")
    public void test_B1_usableTextIsUnaffected() {
        // doubleValue() deliberately disagrees with the text so the route taken is observable.
        final OpaqueNumber fractional = new OpaqueNumber(-7654321.0d, "12.9");
        assertEquals(BigInteger.valueOf(12), Numbers.convert(fractional, BigInteger.class));
        assertEquals(new BigDecimal("12.9"), Numbers.convert(fractional, BigDecimal.class));
        assertEquals(12L, Numbers.convert(fractional, Long.class));

        final OpaqueNumber huge = new OpaqueNumber(-7654321.0d, "123456789012345678901234567890.5");
        assertEquals(new BigInteger("123456789012345678901234567890"), Numbers.convert(huge, BigInteger.class));
        assertEquals(new BigDecimal("123456789012345678901234567890.5"), Numbers.convert(huge, BigDecimal.class));

        final OpaqueNumber exponent = new OpaqueNumber(-7654321.0d, "1e-100000000");
        assertSame(BigInteger.ZERO, Numbers.convert(exponent, BigInteger.class));
    }

    // ==================================================================================================
    // D1: the table's Float/Double -> BigInteger entries share the helper (regression coverage)
    // ==================================================================================================

    @Test
    @DisplayName("D1: Float and Double sources still convert to BigInteger by exact truncation")
    public void test_D1_builtInSourcesStillExact() {
        assertEquals(new BigInteger("2147483648"), Numbers.convert((float) Integer.MAX_VALUE, BigInteger.class));
        assertEquals(new BigInteger("33554448"), Numbers.convert(33554448f, BigInteger.class));
        assertEquals(new BigInteger("-33554448"), Numbers.convert(-33554448f, BigInteger.class));
        assertEquals(new BigInteger("1000000000000000128"), Numbers.convert(1000000000000000128.0d, BigInteger.class));
        assertEquals(new BigInteger("-9223372036854775808"), Numbers.convert(-0x1p63, BigInteger.class));

        for (final Number belowOne : new Number[] { 0.5f, -0.5f, -0.0f, 0.0f, Float.MIN_VALUE, 0.5d, -0.999d, -0.0d, Double.MIN_VALUE }) {
            assertSame(BigInteger.ZERO, Numbers.convert(belowOne, BigInteger.class), String.valueOf(belowOne));
        }

        for (final Number nonFinite : new Number[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY }) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(nonFinite, BigInteger.class),
                    String.valueOf(nonFinite));
            assertEquals("BigInteger overflow: " + nonFinite, e.getMessage());
        }
    }

    // ==================================================================================================
    // D2: Float -> Double via the decimal spelling, one helper behind toDouble(Object) and the table
    // ==================================================================================================

    @Test
    @DisplayName("D2: toDouble(Object) and convert(Float, Double.class) agree on every special float")
    public void test_D2_floatToDoubleSitesAgree() {
        final float[] specials = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f, 1.21f, Float.MIN_VALUE, -Float.MIN_VALUE,
                Float.MAX_VALUE, -Float.MAX_VALUE, 3.4E38f, 1e10f, 0.1f };

        for (final float f : specials) {
            final double viaTable = Numbers.convert(Float.valueOf(f), Double.class);
            final double viaToDouble = Numbers.toDouble(Float.valueOf(f));
            final double expected = Double.parseDouble(Float.toString(f));

            assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(viaTable), String.valueOf(f));
            assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(viaToDouble), String.valueOf(f));
            assertEquals(Double.doubleToRawLongBits(viaTable), Double.doubleToRawLongBits(Numbers.toDouble(Float.valueOf(f), 99.0d)), String.valueOf(f));
        }

        // The documented spelling semantics, and the special values by name.
        assertEquals(1.21d, Numbers.convert(Float.valueOf(1.21f), Double.class));
        assertTrue(Double.isNaN(Numbers.convert(Float.valueOf(Float.NaN), Double.class)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.convert(Float.valueOf(Float.POSITIVE_INFINITY), Double.class));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.convert(Float.valueOf(Float.NEGATIVE_INFINITY), Double.class));
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(Numbers.convert(Float.valueOf(-0.0f), Double.class)));
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(Numbers.convert(Float.valueOf(-0.0f), double.class)));
    }

    // ==================================================================================================
    // D4: emptiness test unified
    // ==================================================================================================

    @Test
    @DisplayName("D4: the try* entry points treat null and empty exactly as the throwing ones do")
    public void test_D4_tryEntryPointsEmptiness() {
        assertTrue(Numbers.tryParseFloat(null).isEmpty());
        assertTrue(Numbers.tryParseFloat("").isEmpty());
        assertTrue(Numbers.tryParseDouble(null).isEmpty());
        assertTrue(Numbers.tryParseDouble("").isEmpty());
        assertTrue(Numbers.tryCreateNumber(null).isEmpty());
        assertTrue(Numbers.tryCreateNumber("").isEmpty());

        assertEquals(1.5f, Numbers.tryParseFloat("1.5").get());
        assertEquals(1.5d, Numbers.tryParseDouble("1.5").get());
        assertEquals(Integer.valueOf(15), Numbers.tryCreateNumber("15").get());
    }
}
