package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Numeric and resource-retention contracts for the October 2026 performance changes. */
public class NumbersOptimizationRegressionTest {

    @Test
    public void packedRadicesPreserveMagnitudeSignAndLeadingZeros() {
        final Random random = new Random(20261019);

        for (final int bits : new int[] { 63, 64, 65, 66, 67, 68, 127, 128, 129, 255, 256, 257, 1024, 32768 }) {
            for (int sample = 0; sample < 8; sample++) {
                final BigInteger magnitude = new BigInteger(bits, random).setBit(bits - 1);

                for (final String prefix : new String[] { "0x", "0X", "#", "0" }) {
                    final int radix = prefix.equals("0") ? 8 : 16;
                    final String token = prefix + "000" + magnitude.toString(radix);
                    assertEquals(magnitude, Numbers.decodeBigInteger(token));
                    assertEquals(magnitude, Numbers.decodeBigInteger("+" + token));
                    assertEquals(magnitude.negate(), Numbers.decodeBigInteger("-" + token));
                    assertEquals(magnitude, Numbers.convert(Numbers.createNumber(token), BigInteger.class));
                }
            }
        }

        assertEquals(BigInteger.ZERO, Numbers.decodeBigInteger("-0x" + "0".repeat(4096)));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("0x" + "f".repeat(4096) + "z"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("0" + "7".repeat(4096) + "8"));
    }

    @Test
    public void roundedLog2KeepsTheExactFallbackAtTheSquareRootBoundary() {
        for (final int floor : new int[] { 255, 256, 257, 511, 1024, 4096 }) {
            final BigInteger thresholdSquared = BigInteger.ONE.shiftLeft(2 * floor + 1);
            final BigInteger thresholdFloor = thresholdSquared.sqrt();

            for (int offset = -2; offset <= 2; offset++) {
                final BigInteger value = thresholdFloor.add(BigInteger.valueOf(offset));
                final int expected = value.multiply(value).compareTo(thresholdSquared) < 0 ? floor : floor + 1;

                for (final RoundingMode mode : new RoundingMode[] { RoundingMode.HALF_DOWN, RoundingMode.HALF_UP, RoundingMode.HALF_EVEN }) {
                    assertEquals(expected, Numbers.log2(value, mode), floor + "/" + offset + "/" + mode);
                }
            }
        }
    }

    private static int exactLog10(final double value, final RoundingMode mode) {
        final BigDecimal exact = new BigDecimal(value);
        final int floor = exact.precision() - exact.scale() - 1;
        final boolean power = exact.compareTo(BigDecimal.ONE.scaleByPowerOfTen(floor)) == 0;

        return switch (mode) {
            case FLOOR -> floor;
            case CEILING -> power ? floor : floor + 1;
            case DOWN -> floor < 0 && !power ? floor + 1 : floor;
            case UP -> floor >= 0 && !power ? floor + 1 : floor;
            case UNNECESSARY -> {
                if (!power) {
                    throw new ArithmeticException();
                }
                yield floor;
            }
            default -> exact.multiply(exact).compareTo(BigDecimal.ONE.scaleByPowerOfTen(2 * floor + 1)) < 0 ? floor : floor + 1;
        };
    }

    private static void checkLog10(final double value) {
        if (!(value > 0) || !Double.isFinite(value)) {
            return;
        }

        for (final RoundingMode mode : RoundingMode.values()) {
            final int expected;
            try {
                expected = exactLog10(value, mode);
            } catch (final ArithmeticException e) {
                assertThrows(ArithmeticException.class, () -> Numbers.log10(value, mode));
                continue;
            }
            assertEquals(expected, Numbers.log10(value, mode), value + "/" + mode);
        }
    }

    @Test
    public void roundedLog10MatchesExactArithmeticNearEveryDecimalBoundary() {
        for (int exponent = -324; exponent <= 308; exponent++) {
            for (final double boundary : new double[] { Math.pow(10, exponent), Math.pow(10, exponent + 0.5) }) {
                double low = boundary;
                double high = boundary;
                checkLog10(boundary);

                for (int neighbor = 0; neighbor < 8; neighbor++) {
                    low = Math.nextDown(low);
                    high = Math.nextUp(high);
                    checkLog10(low);
                    checkLog10(high);
                }
            }
        }

        final Random random = new Random(61019);
        for (int i = 0; i < 2000; i++) {
            checkLog10(Double.longBitsToDouble(random.nextLong() & Long.MAX_VALUE));
        }
    }

    private static void checkFloatingToken(final String text) {
        boolean expected = false;
        double parsed = 0;
        if (text != null && text.length() <= Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH) {
            try {
                parsed = Double.parseDouble(text);
                expected = true;
            } catch (final NumberFormatException ignored) {
                // The JDK parser is the grammar oracle.
            }
        }

        assertEquals(expected, Numbers.isParsable(text), text);
        assertEquals(expected, Numbers.tryParseDouble(text).isPresent(), text);
        assertEquals(expected, Numbers.tryParseFloat(text).isPresent(), text);
        if (expected) {
            assertEquals(Double.doubleToLongBits(parsed), Double.doubleToLongBits(Numbers.tryParseDouble(text).get()), text);
            assertEquals(Float.floatToIntBits(Float.parseFloat(text)), Float.floatToIntBits(Numbers.tryParseFloat(text).get()), text);
        }
    }

    @Test
    public void lexicalScannerMatchesJdkIncludingHexExponentsAndSuffixes() {
        for (final String text : new String[] { null, "", " ", "+", ".", "1.2.3", "1e+", "1e-", "0x", "0x.p1", "0x1", "0x1f", "0x1p", "0x1p+", "0x1p-0f",
                "0x.8P+2D", "0Xf.p99999999999999999999", "-0x0p0", "1f", "1.d", ".5E2F", "NaN", "+NaN", "-Infinity", "NaNf", "infinity", "1_0", "\u0661",
                "\u0000\t-0.0\u001f", "\u00a01.0", "1e" + "9".repeat(4094), "1".repeat(4097) }) {
            checkFloatingToken(text);
        }

        final Random random = new Random(101926);
        final String alphabet = "0123456789abcdefABCDEFxXpPeE+-.fFdD _\t\n\u0661";
        for (int i = 0; i < 5000; i++) {
            final String token = (i % 2 == 0 ? Double.toString(Double.longBitsToDouble(random.nextLong()))
                    : Double.toHexString(Double.longBitsToDouble(random.nextLong()))) + (i % 3 == 0 ? "F" : "");
            checkFloatingToken(token);
            final int index = random.nextInt(token.length() + 1);
            checkFloatingToken(token.substring(0, index) + alphabet.charAt(random.nextInt(alphabet.length())) + token.substring(index));
        }
    }

    private static final class TextNumber extends Number {
        private static final long serialVersionUID = 1L;
        final String text;
        int textCalls;
        int doubleCalls;

        TextNumber(final String text) {
            this.text = text;
        }

        @Override
        public int intValue() {
            return 37;
        }

        @Override
        public long longValue() {
            return 37;
        }

        @Override
        public float floatValue() {
            return 37;
        }

        @Override
        public double doubleValue() {
            doubleCalls++;
            return 37;
        }

        @Override
        public String toString() {
            textCalls++;
            return text;
        }
    }

    @Test
    public void fractionalTextShortcutPreservesSingleRenderingAndFallback() {
        for (final String text : new String[] { "0", "-0.0", "+.123", "0." + "1".repeat(4000), "0.1e-2", "0e2147483647", "0.0e2147483648", "0e-2147483648",
                "0e9999999999", "0.1e-2147483647", "0.1e-", "0.1x", "0.1f", " 0.1", "\u0660.1", null }) {
            final TextNumber source = new TextNumber(text);
            BigInteger expected = BigInteger.valueOf(37);
            boolean usable = false;
            if (text != null && !text.contains("\u0660")) {
                try {
                    expected = new BigDecimal(text).toBigInteger();
                    usable = true;
                } catch (final NumberFormatException ignored) {
                    // Invalid own text must still use doubleValue().
                }
            }
            assertEquals(expected, Numbers.convert(source, BigInteger.class), String.valueOf(text));
            assertEquals(1, source.textCalls, String.valueOf(text));
            assertEquals(usable ? 0 : 1, source.doubleCalls, String.valueOf(text));
        }
    }

    @Test
    public void binaryFractionBoundPreservesValuesAroundOneAndScaleCutoffs() {
        for (final int scale : new int[] { 18, 19, 20, 64, 1024, 9999, 10000, 10001 }) {
            final BigInteger power = BigInteger.TEN.pow(scale);
            for (final BigInteger unscaled : new BigInteger[] { BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.shiftLeft(scale * 3),
                    power.subtract(BigInteger.ONE), power, power.add(BigInteger.ONE) }) {
                for (final int sign : new int[] { -1, 1 }) {
                    final BigDecimal value = new BigDecimal(sign < 0 ? unscaled.negate() : unscaled, scale);
                    assertEquals(value.toBigInteger(), Numbers.convert(value, BigInteger.class));
                }
            }
        }
        assertEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal(BigInteger.ONE.negate(), Integer.MAX_VALUE), BigInteger.class));
    }

    @Test
    public void shortcutsPreserveSubclassConversionAndResultTypes() {
        final BigDecimal custom = new BigDecimal(BigInteger.ONE, 1024) {
            private static final long serialVersionUID = 1L;

            @Override
            public BigInteger toBigInteger() {
                return BigInteger.TEN;
            }
        };
        assertSame(BigInteger.TEN, Numbers.convert(custom, BigInteger.class));

        final BigInteger subclass = new BigInteger("18446744073709551616") {
            private static final long serialVersionUID = 1L;
        };
        final BigInteger ceiling = Numbers.ceilingPowerOfTwo(subclass);
        assertEquals(subclass, ceiling);
        assertSame(BigInteger.class, ceiling.getClass());
    }

    @Test
    public void noOpRoundingPreservesBitsAndStillValidatesMode() {
        for (final RoundingMode mode : RoundingMode.values()) {
            for (final double value : new double[] { 0.0, -0.0, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MAX_VALUE, Double.POSITIVE_INFINITY,
                    Double.longBitsToDouble(0x7ff8000000000123L) }) {
                for (final int scale : new int[] { 400, 1000, Integer.MAX_VALUE }) {
                    assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(Numbers.round(value, scale, mode)));
                }
            }
            for (final float value : new float[] { 0.0f, -0.0f, Float.MIN_VALUE, -Float.MIN_VALUE, Float.MAX_VALUE, Float.NEGATIVE_INFINITY,
                    Float.intBitsToFloat(0x7fc00123) }) {
                for (final int scale : new int[] { 46, 400, Integer.MAX_VALUE }) {
                    assertEquals(Float.floatToRawIntBits(value), Float.floatToRawIntBits(Numbers.round(value, scale, mode)));
                }
            }
            assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(Numbers.round(-0.0, Integer.MIN_VALUE, mode)));
            assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(Numbers.round(-0.0f, Integer.MIN_VALUE, mode)));
        }
        assertThrows(IllegalArgumentException.class, () -> Numbers.round(0.0, 1000, null));
        assertThrows(IllegalArgumentException.class, () -> Numbers.round(0.0f, 1000, null));
    }

    @Test
    public void ceilingPowerReusesExactInputsAndPreservesAdjacentResults() {
        for (final int exponent : new int[] { 0, 1, 63, 64, 255, 4096, 65536 }) {
            final BigInteger power = BigInteger.ONE.shiftLeft(exponent);
            assertSame(power, Numbers.ceilingPowerOfTwo(power));
            assertEquals(power.shiftLeft(1), Numbers.ceilingPowerOfTwo(power.add(BigInteger.ONE)));
            if (exponent > 1) {
                assertEquals(power, Numbers.ceilingPowerOfTwo(power.subtract(BigInteger.ONE)));
            }
        }
    }

    @Test
    public void unusuallyLongPatternsStillFormatButAreNotRetained() throws Exception {
        final var field = Numbers.class.getDeclaredField("THREAD_LOCAL_DECIMAL_FORMATS");
        field.setAccessible(true);
        final ThreadLocal<?> local = (ThreadLocal<?>) field.get(null);
        final Locale saved = Locale.getDefault(Locale.Category.FORMAT);
        local.remove();
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            final String cacheable = "#".repeat(1023) + "0";
            Numbers.format(12.5, cacheable);
            final Map<?, ?> map = (Map<?, ?>) ((Object[]) local.get())[1];
            assertTrue(map.containsKey(cacheable));

            for (final String pattern : new String[] { "#".repeat(1024) + "0", "#".repeat(65536) + "0", "'" + "x".repeat(2048) + "'0.00" }) {
                final DecimalFormat expected = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
                expected.setRoundingMode(RoundingMode.HALF_EVEN);
                assertEquals(expected.format(12.5), Numbers.format(12.5, pattern));
                assertFalse(map.containsKey(pattern));
            }
            assertEquals(1, map.size());
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            assertEquals("12,50", Numbers.format(12.5, "#".repeat(1025) + "0.00"));
            assertTrue(map.isEmpty(), "an uncached call must also invalidate the old locale's cache");
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, saved);
            local.remove();
        }
    }

    @Test
    public void invalidLargeBigIntegersHaveBoundedMathDiagnostics() {
        final BigInteger large = BigInteger.ONE.shiftLeft(32768).negate();
        final String positive = assertThrows(IllegalArgumentException.class, () -> Numbers.log2(large, RoundingMode.FLOOR)).getMessage();
        final String nonNegative = assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(large, RoundingMode.FLOOR)).getMessage();
        assertTrue(positive.contains("BigInteger[signum=-1, bitLength=32768]"));
        assertTrue(nonNegative.contains("BigInteger[signum=-1, bitLength=32768]"));
        assertTrue(positive.length() < 100);
        assertTrue(nonNegative.length() < 100);
        assertEquals("x (-1) must be > 0",
                assertThrows(IllegalArgumentException.class, () -> Numbers.log2(BigInteger.ONE.negate(), RoundingMode.FLOOR)).getMessage());
        assertEquals("x (-1) must be >= 0",
                assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(BigInteger.ONE.negate(), RoundingMode.FLOOR)).getMessage());
    }
}
