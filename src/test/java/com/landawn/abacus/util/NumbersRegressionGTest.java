package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Locks the behaviour changed by cycle 2 of the 2026-09-02 iterative review of {@code Numbers}:
 * <ul>
 *   <li><b>C-011</b> {@code asinh}/{@code atanh} are computed with one {@code log1p} formulation each and are
 *       within 2 ulp of the exact value over their whole domain; the previous Taylor-series / {@code log}
 *       branches measured up to 8 ulp ({@code asinh(0.2478)}) and 3.8 ulp ({@code atanh(0.16)}) off;</li>
 *   <li><b>C-012</b> a {@code NumberFormatException} raised by an unsupported target type's own parser inside
 *       {@code convert} carries the bounded, escaped preview every other parse failure of this class carries,
 *       not the parser's whole-text message.</li>
 * </ul>
 */
public class NumbersRegressionGTest extends TestBase {

    // ==================================================================================================
    // A 60-digit reference for the hyperbolic inverses, independent of java.lang.Math.
    // ==================================================================================================

    private static final MathContext MC = new MathContext(60, RoundingMode.HALF_EVEN);
    private static final BigDecimal EPS = new BigDecimal("1e-72");
    /** {@code BigDecimal.TWO} is JDK 19+; the build targets 17. */
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal LN2 = lnNearOne(TWO);

    /** {@code atanh(z) = z + z^3/3 + z^5/5 + ...}; used for {@code |z| <= 0.6}. */
    private static BigDecimal atanhSeries(final BigDecimal z) {
        final BigDecimal z2 = z.multiply(z, MC);
        BigDecimal term = z;
        BigDecimal sum = z;

        for (int n = 3; n < 100_000; n += 2) {
            term = term.multiply(z2, MC);
            final BigDecimal add = term.divide(BigDecimal.valueOf(n), MC);

            if (add.abs().compareTo(EPS) < 0) {
                break;
            }

            sum = sum.add(add, MC);
        }

        return sum;
    }

    /** {@code ln(y) = 2 atanh((y - 1) / (y + 1))} for {@code y} in {@code [0.5, 4]}. */
    private static BigDecimal lnNearOne(final BigDecimal y) {
        return atanhSeries(y.subtract(BigDecimal.ONE).divide(y.add(BigDecimal.ONE), MC)).multiply(TWO, MC);
    }

    /** {@code ln(y)} for any positive {@code y}, reduced by powers of two into {@code [1, 2)}. */
    private static BigDecimal ln(BigDecimal y) {
        int k = 0;

        while (y.compareTo(TWO) >= 0) {
            y = y.divide(TWO, MC);
            k++;
        }

        while (y.compareTo(BigDecimal.ONE) < 0) {
            y = y.multiply(TWO, MC);
            k--;
        }

        return lnNearOne(y).add(LN2.multiply(BigDecimal.valueOf(k), MC), MC);
    }

    private static BigDecimal asinhRef(final double x) {
        final BigDecimal bx = new BigDecimal(x);
        return ln(bx.add(bx.multiply(bx, MC).add(BigDecimal.ONE, MC).sqrt(MC), MC));
    }

    private static BigDecimal atanhRef(final double x) {
        final BigDecimal bx = new BigDecimal(x);
        return ln(BigDecimal.ONE.add(bx, MC).divide(BigDecimal.ONE.subtract(bx, MC), MC)).divide(TWO, MC);
    }

    private static double ulps(final double got, final BigDecimal ref) {
        return Math.abs(new BigDecimal(got).subtract(ref, MC).doubleValue()) / Math.ulp(ref.doubleValue());
    }

    // ==================================================================================================
    // C-011: accuracy
    // ==================================================================================================

    @Test
    @DisplayName("C-011: asinh is within 2 ulp of a 60-digit reference across the finite range")
    public void test_C011_asinhWithinTwoUlp() {
        final Random random = new Random(20260902L);
        double worst = 0;

        for (int i = 0; i < 3_000; i++) {
            // magnitudes from 2^-30 up to 2^300, both signs
            final double x = Math.copySign(Math.scalb(1 + random.nextDouble(), random.nextInt(330) - 30), random.nextBoolean() ? 1 : -1);
            final double u = ulps(Math.abs(Numbers.asinh(x)), asinhRef(Math.abs(x)));
            worst = Math.max(worst, u);
            assertTrue(u <= 2.0, "asinh(" + x + ") is " + u + " ulp off");
        }

        // The values where the previous implementation was worst (8.0 ulp and 1.6 ulp).
        for (final double x : new double[] { 0.24778358001241585, 1.1182790946252728, 0.2, 0.167, 0.1670001, 0.097, 0.036, 0.0036, 0.5, 0.9, 1.0, 2.0 }) {
            assertTrue(ulps(Numbers.asinh(x), asinhRef(x)) <= 2.0, String.valueOf(x));
        }

        assertTrue(worst <= 2.0, String.valueOf(worst));
    }

    @Test
    @DisplayName("C-011: atanh is within 2 ulp of a 60-digit reference across the open domain")
    public void test_C011_atanhWithinTwoUlp() {
        final Random random = new Random(20260902L);

        for (int i = 0; i < 3_000; i++) {
            final double x = switch (i % 3) {
                case 0 -> random.nextDouble() * 0.15 + 1e-12; // the old series range
                case 1 -> 0.15 + random.nextDouble() * 0.849; // the old log range
                default -> 1 - Math.scalb(1 + random.nextDouble(), -10 - random.nextInt(30)); // hugging 1
            };
            final double signed = random.nextBoolean() ? x : -x;
            final double u = ulps(Math.abs(Numbers.atanh(signed)), atanhRef(x));
            assertTrue(u <= 2.0, "atanh(" + signed + ") is " + u + " ulp off");
        }

        // The values where the previous implementation was worst (3.8 ulp), and the old branch boundaries.
        for (final double x : new double[] { 0.1596407553086861, 0.1837689664409789, 0.3, 0.15, 0.1500001, 0.087, 0.031, 0.003, 0.5, 0.9 }) {
            assertTrue(ulps(Numbers.atanh(x), atanhRef(x)) <= 2.0, String.valueOf(x));
        }
    }

    @Test
    @DisplayName("C-011: the documented example values still hold")
    public void test_C011_documentedExamples() {
        assertEquals(0.88137, Numbers.asinh(1.0), 1e-5);
        assertEquals(-0.88137, Numbers.asinh(-1.0), 1e-5);
        assertEquals(2.99822, Numbers.asinh(10.0), 1e-5);
        assertEquals(0.54931, Numbers.atanh(0.5), 1e-5);
        assertEquals(-0.54931, Numbers.atanh(-0.5), 1e-5);
        assertEquals(1.47222, Numbers.atanh(0.9), 1e-5);
        // ... and within the documented 2 ulp of the reference at every example.
        for (final double x : new double[] { 1.0, 10.0 }) {
            assertTrue(ulps(Numbers.asinh(x), asinhRef(x)) <= 2.0, String.valueOf(x));
        }
        for (final double x : new double[] { 0.5, 0.9 }) {
            assertTrue(ulps(Numbers.atanh(x), atanhRef(x)) <= 2.0, String.valueOf(x));
        }
    }

    @Test
    @DisplayName("C-011: special values and signed zero are unchanged by the reformulation")
    public void test_C011_specialValues() {
        // asinh: every real is in the domain, nothing throws
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.asinh(0.0)));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.asinh(-0.0)));
        assertTrue(Double.isNaN(Numbers.asinh(Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.asinh(Double.POSITIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.asinh(Double.NEGATIVE_INFINITY));
        assertEquals(Math.log(Double.MAX_VALUE) + Math.log(2), Numbers.asinh(Double.MAX_VALUE), 1e-12);
        assertEquals(-Numbers.asinh(Double.MAX_VALUE), Numbers.asinh(-Double.MAX_VALUE));
        // asinh(a) == a exactly for tiny and subnormal a (the series used to produce this; log1p does too),
        // and within an ulp where the a^3/6 term is just below the last bit.
        for (final double tiny : new double[] { Double.MIN_VALUE, -Double.MIN_VALUE, 1e-320, 0x1p-60, -0x1p-60 }) {
            assertEquals(tiny, Numbers.asinh(tiny), String.valueOf(tiny));
        }
        assertEquals(0x1p-30, Numbers.asinh(0x1p-30), Math.ulp(0x1p-30));
        // odd function
        for (final double x : new double[] { 0.001, 0.05, 0.13, 0.2, 1.5, 100.0, 1e10, 1e100 }) {
            assertEquals(-Numbers.asinh(x), Numbers.asinh(-x), String.valueOf(x));
        }

        // atanh: open domain (-1, 1); +-1 are +-Infinity; beyond is NaN; nothing throws
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.atanh(0.0)));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.atanh(-0.0)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.atanh(1.0));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.atanh(-1.0));
        assertTrue(Double.isNaN(Numbers.atanh(1.5)));
        assertTrue(Double.isNaN(Numbers.atanh(-1.5)));
        assertTrue(Double.isNaN(Numbers.atanh(Math.nextUp(1.0))));
        assertTrue(Double.isNaN(Numbers.atanh(Double.POSITIVE_INFINITY)));
        assertTrue(Double.isNaN(Numbers.atanh(Double.NEGATIVE_INFINITY)));
        assertTrue(Double.isNaN(Numbers.atanh(Double.NaN)));
        assertTrue(Double.isFinite(Numbers.atanh(Math.nextDown(1.0))));
        assertTrue(Numbers.atanh(Math.nextDown(1.0)) > 18.0);
        for (final double tiny : new double[] { Double.MIN_VALUE, -Double.MIN_VALUE, 1e-320, 0x1p-60, -0x1p-60 }) {
            assertEquals(tiny, Numbers.atanh(tiny), String.valueOf(tiny));
        }
        assertEquals(0x1p-30, Numbers.atanh(0x1p-30), Math.ulp(0x1p-30));
        for (final double x : new double[] { 0.001, 0.05, 0.13, 0.2, 0.5, 0.9, 0.999999 }) {
            assertEquals(-Numbers.atanh(x), Numbers.atanh(-x), String.valueOf(x));
        }
    }

    // ==================================================================================================
    // C-012: bounded message from an unsupported target's own parser
    // ==================================================================================================

    /** A Number whose text is huge and carries a line break, so an unbounded message would be visible. */
    private static final class LoudNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final String text;

        LoudNumber(final String text) {
            this.text = text;
        }

        @Override
        public int intValue() {
            return 1;
        }

        @Override
        public long longValue() {
            return 1;
        }

        @Override
        public float floatValue() {
            return 1;
        }

        @Override
        public double doubleValue() {
            return 1;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    @Test
    @DisplayName("C-012: a parseable-but-unsupported target still converts, and a fractional source still fails as NumberFormatException")
    public void test_C012_unsupportedTargetBehaviourUnchanged() {
        assertEquals(12, Numbers.convert(12, AtomicInteger.class).get());
        assertEquals(7L, Numbers.convert(7, AtomicLong.class).get());
        assertEquals(12, Numbers.convert(12, Type.of(AtomicInteger.class)).get());

        assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, AtomicInteger.class));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, Type.of(AtomicInteger.class)));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, AtomicInteger.class, new AtomicInteger()));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, Type.of(AtomicInteger.class), new AtomicInteger()));
        // An abstract target is still IllegalArgumentException, not NumberFormatException.
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(12.9, Number.class));
    }

    @Test
    @DisplayName("C-012: the message names the target and previews the text, on both convert entry points")
    public void test_C012_messageIsBoundedAndNamesTheTarget() {
        final NumberFormatException byClass = assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, AtomicInteger.class));
        assertEquals("12.9 is not a valid AtomicInteger.", byClass.getMessage());
        assertTrue(byClass.getCause() instanceof NumberFormatException, String.valueOf(byClass.getCause()));
        assertTrue(byClass.getCause().getMessage().contains("12.9"), byClass.getCause().getMessage());

        final NumberFormatException byType = assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, Type.of(AtomicInteger.class)));
        assertEquals("12.9 is not a valid AtomicInteger.", byType.getMessage());

        // A 100 KB text with an embedded line break: previewed, escaped, never echoed.
        final LoudNumber loud = new LoudNumber("1.\n" + "5".repeat(100_000));
        final NumberFormatException huge = assertThrows(NumberFormatException.class, () -> Numbers.convert(loud, AtomicInteger.class));
        assertTrue(huge.getMessage().length() < 300, String.valueOf(huge.getMessage().length()));
        assertTrue(huge.getMessage().contains("...[100003 chars]"), huge.getMessage());
        assertTrue(huge.getMessage().contains("\\u000A"), huge.getMessage());
        assertFalse(huge.getMessage().contains("\n"), "raw line break in message");
        assertTrue(huge.getMessage().endsWith(" is not a valid AtomicInteger."), huge.getMessage());
        // The retained cause is bounded too (the JDK's own message embedded the whole text).
        assertTrue(huge.getCause().getMessage().length() < 300, String.valueOf(huge.getCause().getMessage().length()));
        assertFalse(huge.getCause().getMessage().contains("\n"), "raw line break in cause");
    }
}
