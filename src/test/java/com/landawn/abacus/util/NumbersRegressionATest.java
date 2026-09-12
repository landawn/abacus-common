package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the fourth independent review pass over {@code Numbers} (2026-08-30g).
 * Ledger: {@code scripts/cross_review/Numbers_ledger_2026-08-30g.md}.
 */
public class NumbersRegressionATest extends TestBase {

    /** A {@code Number} outside the built-in eight whose {@code toString()} is caller-controlled. */
    private static final class LoudNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final String text;
        private final double value;

        LoudNumber(final String text, final double value) {
            this.text = text;
            this.value = value;
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

    private static void assertBoundedAndEscaped(final Throwable t) {
        for (Throwable cur = t; cur != null && cur != cur.getCause(); cur = cur.getCause()) {
            final String message = cur.getMessage();

            if (message == null) {
                continue;
            }

            assertTrue(message.length() < 256, "message is " + message.length() + " chars: " + message.substring(0, Math.min(60, message.length())));

            for (int i = 0; i < message.length(); i++) {
                assertTrue(!Character.isISOControl(message.charAt(i)),
                        "raw control U+" + Integer.toHexString(message.charAt(i)) + " at " + i + " of " + cur.getClass().getSimpleName());
            }
        }
    }

    // ==================================================================================================
    // C-001: an unknown Number's toString() must be bounded and escaped in the overflow message
    // ==================================================================================================

    @Test
    public void test_overflowMessage_isBoundedForANumberWithAHugeToString() {
        // A subtype whose toString() is NOT a numeric token: this is the path that reached the raw
        // toString(). A numeric one is re-parsed as a BigDecimal and summarized by the bit-length guard
        // instead (pinned by the next test), so it never exercised the leak.
        final LoudNumber huge = new LoudNumber("Q".repeat(100_000), Double.NaN);

        for (final Executable call : new Executable[] { () -> Numbers.toByte(huge), () -> Numbers.toShort(huge), () -> Numbers.toInt(huge),
                () -> Numbers.toLong(huge), () -> Numbers.convert(huge, Integer.class), () -> Numbers.convert(huge, Long.class),
                () -> Numbers.convert(huge, BigInteger.class), () -> Numbers.convert(huge, BigDecimal.class) }) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, call::run);
            assertBoundedAndEscaped(e);
            assertTrue(e.getMessage().contains("...[100000 chars]"), e.getMessage());
        }
    }

    @Test
    public void test_overflowMessage_aNumericToStringIsPreviewedRatherThanSummarized() {
        // Superseded expectation. When this test was written the object path recovered a huge numeric
        // toString() by building a BigDecimal, so the message came out of describeNumberForError's
        // BigDecimal branch ("BigDecimal[signum=1, unscaledBitLength=332193, scale=0]"). The 2026-08-30h
        // pass began reporting the raw token instead, to match what toInt(String) prints, and 2026-08-30i
        // removed the BigDecimal from this path altogether. The subtype is not a BigDecimal, so the message
        // now takes the ordinary preview branch -- still bounded and escaped, which is what C-001 was about.
        final LoudNumber numeric = new LoudNumber("9".repeat(100_000), Double.NaN);
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toInt(numeric));

        assertBoundedAndEscaped(e);
        assertEquals("int overflow: " + "9".repeat(96) + "...[100000 chars]", e.getMessage());

        // A real BigDecimal argument still takes the bit-length summary, so the two branches remain distinct.
        assertEquals("int overflow: BigDecimal[signum=1, unscaledBitLength=332193, scale=0]",
                assertThrows(ArithmeticException.class, () -> Numbers.toInt(new BigDecimal("9".repeat(100_000)))).getMessage());
    }

    @Test
    public void test_overflowMessage_escapesAControlCharacterInANumbersToString() {
        final LoudNumber withBreak = new LoudNumber("1" + (char) 0x0A + "2", Double.NaN);

        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toLong(withBreak));
        assertBoundedAndEscaped(e);
        assertEquals("long overflow: 1\\u000A2", e.getMessage());

        // an astral pair is printable and must survive intact
        final LoudNumber emoji = new LoudNumber("1" + (char) 0xD83D + (char) 0xDE00 + "2", Double.NaN);
        assertEquals("int overflow: 1" + (char) 0xD83D + (char) 0xDE00 + "2", assertThrows(ArithmeticException.class, () -> Numbers.toInt(emoji)).getMessage());

        // an unpaired surrogate is escaped rather than emitted as broken UTF-16
        final LoudNumber lone = new LoudNumber("1" + (char) 0xD800 + "2", Double.NaN);
        assertEquals("int overflow: 1\\uD8002", assertThrows(ArithmeticException.class, () -> Numbers.toInt(lone)).getMessage());
    }

    @Test
    public void test_overflowMessage_spellingIsUnchangedForEveryTypeThatRenderedInFullBefore() {
        assertEquals("byte overflow: 1000", assertThrows(ArithmeticException.class, () -> Numbers.toByte(1000)).getMessage());
        assertEquals("byte overflow: 1234", assertThrows(ArithmeticException.class, () -> Numbers.toByte(new AtomicInteger(1234))).getMessage());
        assertEquals("int overflow: NaN", assertThrows(ArithmeticException.class, () -> Numbers.toInt(Double.NaN)).getMessage());
        assertEquals("long overflow: 1.0E300", assertThrows(ArithmeticException.class, () -> Numbers.toLong(1e300d)).getMessage());

        // The widest BigDecimal the 192-bit guard admits is 72 characters and must still render in full.
        final BigDecimal widest = new BigDecimal(BigInteger.ONE.shiftLeft(192).subtract(BigInteger.ONE).negate(), Integer.MIN_VALUE + 58);

        // 72 is the number MAX_NUMBER_ERROR_TEXT_LENGTH is derived from; pin it so that a JDK change to
        // BigDecimal.toString() cannot silently invalidate the cap's rationale.
        assertEquals(72, widest.toString().length(), widest.toString());

        final String rendered = assertThrows(ArithmeticException.class, () -> Numbers.toInt(widest)).getMessage();
        assertEquals("int overflow: " + widest, rendered);
        assertTrue(rendered.length() < 256, rendered);

        // Beyond the guard, the existing summary form is unchanged.
        assertTrue(assertThrows(ArithmeticException.class, () -> Numbers.toInt(BigInteger.ONE.shiftLeft(2000))).getMessage()
                .startsWith("int overflow: BigInteger[signum=1, bitLength=2001]"));
    }

    // ==================================================================================================
    // C-003: an illegal DecimalFormat pattern must not put the whole pattern in the message
    // ==================================================================================================

    @Test
    public void test_format_illegalPatternMessageIsBoundedAndEscaped() {
        final String hugePattern = "'" + "x".repeat(100_000);
        final IllegalArgumentException huge = assertThrows(IllegalArgumentException.class, () -> Numbers.format(1, hugePattern));
        assertBoundedAndEscaped(huge);
        assertTrue(huge.getMessage().contains("...[100001 chars]"), huge.getMessage());
        assertNotNull(huge.getCause());

        final IllegalArgumentException withBreak = assertThrows(IllegalArgumentException.class, () -> Numbers.format(1, "'1" + (char) 0x0A + "2"));
        assertBoundedAndEscaped(withBreak);
        assertTrue(withBreak.getMessage().contains("\\u000A"), withBreak.getMessage());

        // A null pattern is still rejected before any DecimalFormat is built, and valid patterns still work.
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(1, (String) null));
        assertEquals("1.00", Numbers.format(1, "0.00"));
        assertEquals("1.00", Numbers.format(1, "0.00"));
    }

    // ==================================================================================================
    // C-004: decodeLong's cross-scanner invariant is checked without relying on -ea
    // ==================================================================================================

    @Test
    public void test_decodeLong_stillHandlesTheLongMinValueTokenAndMalformedInput() {
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-9223372036854775808"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-0x8000000000000000"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-01000000000000000000000"));
        assertEquals(Long.valueOf(255L), Numbers.decodeLong("0xFFL"));

        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9223372036854775808"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("-9223372036854775809"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("12x3"));
        assertBoundedAndEscaped(assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9".repeat(100_000))));
    }

    // ==================================================================================================
    // C-002: every built-in numeric target obeys the built-in rule, whatever the source's exact class
    // ==================================================================================================

    @Test
    public void test_convert_bigTargetsNarrowAnUnknownNumberByTheDocumentedRule() {
        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9d);
        final DoubleAdder negative = new DoubleAdder();
        negative.add(-12.9d);

        // "an integral target (byte, short, int, long, or BigInteger) truncates toward zero"
        assertEquals(BigInteger.valueOf(12), Numbers.convert(fractional, BigInteger.class));
        assertEquals(BigInteger.valueOf(-12), Numbers.convert(negative, BigInteger.class));
        assertEquals(0, new BigDecimal("12.9").compareTo(Numbers.convert(fractional, BigDecimal.class)));

        // ... exactly as a directly supported source of the same value does
        assertEquals(Numbers.convert(12.9d, BigInteger.class), Numbers.convert(fractional, BigInteger.class));
        assertEquals(0, Numbers.convert(12.9d, BigDecimal.class).compareTo(Numbers.convert(fractional, BigDecimal.class)));

        // an integer-valued unknown source is unaffected
        assertEquals(BigInteger.valueOf(1234), Numbers.convert(new AtomicInteger(1234), BigInteger.class));
        assertEquals(0, BigDecimal.valueOf(1234).compareTo(Numbers.convert(new AtomicInteger(1234), BigDecimal.class)));
    }

    @Test
    public void test_convert_bigTargetsRejectANonFiniteUnknownNumberWithArithmeticException() {
        for (final double nonFinite : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            final DoubleAdder adder = new DoubleAdder();
            adder.add(nonFinite);

            assertThrows(ArithmeticException.class, () -> Numbers.convert(adder, BigInteger.class), String.valueOf(nonFinite));
            assertThrows(ArithmeticException.class, () -> Numbers.convert(adder, BigDecimal.class), String.valueOf(nonFinite));

            // the same answer the directly supported Double source gives
            assertThrows(ArithmeticException.class, () -> Numbers.convert(nonFinite, BigInteger.class), String.valueOf(nonFinite));
        }
    }

    @Test
    public void test_convert_floatAndDoubleTargetsAgreeWithToFloatAndToDouble() {
        // A subtype whose toString() is formatted text: the string route cannot parse it, but the
        // documented contract says convert is consistent with toFloat(Object)/toDouble(Object).
        final LoudNumber formatted = new LoudNumber("12.9 ms", 12.9d);

        assertEquals(Numbers.toFloat(formatted), Numbers.convert(formatted, Float.class), 0.0f);
        assertEquals(Numbers.toDouble(formatted), Numbers.convert(formatted, Double.class), 0.0d);
        assertEquals(12.9f, Numbers.convert(formatted, Float.class), 0.0f);
        assertEquals(12.9d, Numbers.convert(formatted, Double.class), 0.0d);

        // integral targets already used doubleValue() as their last resort; they still do
        assertEquals(Integer.valueOf(12), Numbers.convert(formatted, Integer.class));
        assertEquals(BigInteger.valueOf(12), Numbers.convert(formatted, BigInteger.class));

        // NaN/Infinity are preserved by a float/double target
        final LoudNumber nan = new LoudNumber("not a number", Double.NaN);
        assertTrue(Float.isNaN(Numbers.convert(nan, Float.class)));
        assertTrue(Double.isNaN(Numbers.convert(nan, Double.class)));
    }

    @Test
    public void test_convert_agreesWithTheToStarFamilyForEveryBuiltInTarget() {
        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9d);
        final Number[] sources = { Byte.valueOf((byte) 12), Short.valueOf((short) 12), Integer.valueOf(12), Long.valueOf(12L), Float.valueOf(12.9f),
                Double.valueOf(12.9d), BigInteger.valueOf(12), new BigDecimal("12.9"), new AtomicInteger(12), fractional, new LoudNumber("12.9 ms", 12.9d) };

        for (final Number source : sources) {
            final String label = source.getClass().getSimpleName() + "(" + source + ")";
            assertEquals(Numbers.toByte(source), Numbers.convert(source, Byte.class).byteValue(), label);
            assertEquals(Numbers.toShort(source), Numbers.convert(source, Short.class).shortValue(), label);
            assertEquals(Numbers.toInt(source), Numbers.convert(source, Integer.class).intValue(), label);
            assertEquals(Numbers.toLong(source), Numbers.convert(source, Long.class).longValue(), label);
            assertEquals(Numbers.toFloat(source), Numbers.convert(source, Float.class).floatValue(), 0.0f, label);
            assertEquals(Numbers.toDouble(source), Numbers.convert(source, Double.class).doubleValue(), 0.0d, label);
        }
    }

    @Test
    public void test_convert_aTargetOutsideTheBuiltInSetStillUsesItsStringParser() {
        // The string route is still what an unknown TARGET gets, and an unusable one is still an IAE.
        assertEquals(5, Numbers.convert(Integer.valueOf(5), AtomicInteger.class).intValue());
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(Integer.valueOf(5), Number.class));
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(Integer.valueOf(5), (Class<Integer>) null));
    }

    /** Minimal throwing-runnable so a loop can drive several call shapes through one assertion. */
    private interface Executable {
        public void run();
    }

    @Test
    public void test_convert_nullAndDefaultsAreUnchanged() {
        assertNull(Numbers.convert(null, Integer.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(null, int.class));
        assertEquals(Integer.valueOf(-1), Numbers.convert(null, Integer.class, -1));
        assertSame(BigInteger.TEN, Numbers.convert(null, BigInteger.class, BigInteger.TEN));
    }
}
