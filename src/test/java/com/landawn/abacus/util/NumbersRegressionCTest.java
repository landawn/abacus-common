package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the {@code Numbers.java} review of 2026-08-30i.
 *
 * <ul>
 *   <li><b>B1</b> &mdash; the unrecognized-{@code Number} recovery no longer builds a {@code BigDecimal} for an
 *       integral target, so a decimal {@code toString()} of any length is linear and allocation-free instead of
 *       quadratic and unbounded.</li>
 *   <li><b>B2</b> &mdash; the integral targets and the {@code BigInteger}/{@code BigDecimal} targets recover a
 *       value from exactly the same set of texts, so they can no longer answer differently for one source.</li>
 *   <li><b>J1/J2</b> &mdash; the {@code createNumber} payload-length routing boundaries the javadoc now
 *       states.</li>
 * </ul>
 */
public class NumbersRegressionCTest extends TestBase {

    // ==================================================================================================
    // Test doubles
    // ==================================================================================================

    /**
     * A {@code Number} outside the eight built-in types whose {@code toString()} and {@code doubleValue()}
     * deliberately disagree, so the recovery route actually taken is observable in the result.
     */
    private static final class SplitNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final String text;
        private final double value;

        SplitNumber(final String text, final double value) {
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

    /** The sentinel {@code doubleValue()} used throughout: a value no test text denotes. */
    private static final double SENTINEL = -7654321.0d;

    private static SplitNumber n(final String text) {
        return new SplitNumber(text, SENTINEL);
    }

    // ==================================================================================================
    // B2: one recovery grammar for every target
    // ==================================================================================================

    @Test
    @DisplayName("B2: a hex or L-suffixed toString() is not a Number's canonical text and takes doubleValue()")
    public void test_B2_hexAndLSuffixedTextAreNotCanonicalNumberText() {
        // toInt(String) accepts these spellings because it is a text parse. Object conversion is a numeric
        // coercion, and no Number renders itself in hex or with a Java long-literal suffix, so the text is
        // treated as non-canonical and the documented doubleValue() fallback applies.
        for (final String text : new String[] { "0xFF", "0XFF", "#FF", "-0xFF", "999L", "999l", "-999L", "007L", "0x10" }) {
            final SplitNumber value = n(text);

            assertEquals((int) SENTINEL, Numbers.toInt(value), text);
            assertEquals((long) SENTINEL, Numbers.toLong(value), text);
            assertEquals(Integer.valueOf((int) SENTINEL), Numbers.convert(value, Integer.class), text);
            assertEquals(BigInteger.valueOf((long) SENTINEL), Numbers.convert(value, BigInteger.class), text);
        }

        // The String parsers are unchanged: they still read the same spellings as text.
        assertEquals(255, Numbers.toInt("0xFF"));
        assertEquals(255, Numbers.toInt("#FF"));
        assertEquals(999, Numbers.toInt("999L"));
    }

    @Test
    @DisplayName("B2: every built-in target recovers the same value from the same source")
    public void test_B2_allTargetsAgreeOnTheSameSource() {
        for (final String text : new String[] { "42", "-42", "+42", "010", "12.9", "-12.9", "0.0", "-0.0", "1e3", "1.5e3", "1e-3", ".5", "42.", "0", "00000042",
                "9223372036854775807", "-9223372036854775808", "0xFF", "#FF", "999L", "not a number", "", "1,234", " 42 " }) {
            final SplitNumber value = n(text);
            final Long asLong;

            try {
                asLong = Numbers.convert(value, Long.class);
            } catch (final ArithmeticException e) {
                continue; // out of long range for both; nothing to compare
            }

            // BigInteger is the same truncation as Long, just unbounded, so whenever Long succeeds the two
            // must agree. Before this fix they disagreed for every hex or L-suffixed spelling.
            assertEquals(BigInteger.valueOf(asLong), Numbers.convert(value, BigInteger.class), text);
            assertEquals(asLong.longValue(), Numbers.toLong(value), text);

            if (asLong.longValue() >= Integer.MIN_VALUE && asLong.longValue() <= Integer.MAX_VALUE) {
                assertEquals(asLong.intValue(), Numbers.convert(value, Integer.class).intValue(), text);
            } else {
                assertThrows(ArithmeticException.class, () -> Numbers.convert(value, Integer.class), text);
            }
        }
    }

    @Test
    @DisplayName("B2: fuzz -- convert(x, Long) and convert(x, BigInteger) never disagree")
    public void test_B2_fuzzTargetsNeverDisagree() {
        final Random rnd = new Random(20260830L);
        final char[] alphabet = "0123456789+-.#xXlLeEabcdefABCDEF".toCharArray();
        final StringBuilder sb = new StringBuilder();
        int compared = 0;

        for (int i = 0; i < 200_000; i++) {
            sb.setLength(0);
            final int len = 1 + rnd.nextInt(8);

            for (int j = 0; j < len; j++) {
                sb.append(alphabet[rnd.nextInt(alphabet.length)]);
            }

            final SplitNumber value = n(sb.toString());
            final Long asLong;

            try {
                asLong = Numbers.convert(value, Long.class);
            } catch (final ArithmeticException e) {
                continue;
            }

            compared++;
            assertEquals(BigInteger.valueOf(asLong), Numbers.convert(value, BigInteger.class), sb.toString());
        }

        assertTrue(compared > 100_000, "expected the fuzz to actually compare something, got " + compared);
    }

    @Test
    @DisplayName("B2: the recognized subtypes and canonical decimal text are unaffected")
    public void test_B2_recognizedSubtypesAreUnaffected() {
        assertEquals(12, Numbers.toInt(new AtomicInteger(12)));
        assertEquals(12L, Numbers.toLong(new AtomicLong(12L)));

        final LongAdder adder = new LongAdder();
        adder.add(1234);
        assertEquals(1234, Numbers.toInt(adder));
        assertEquals(BigInteger.valueOf(1234), Numbers.convert(adder, BigInteger.class));

        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9d);
        assertEquals(12, Numbers.toInt(fractional));
        assertEquals(BigInteger.valueOf(12), Numbers.convert(fractional, BigInteger.class));
        assertEquals(0, new BigDecimal("12.9").compareTo(Numbers.convert(fractional, BigDecimal.class)));
    }

    // ==================================================================================================
    // B1: the integral recovery is lexical, so it is linear and allocation-free at any length
    // ==================================================================================================

    @Test
    @DisplayName("B1: a huge decimal toString() no longer builds a BigDecimal")
    public void test_B1_hugeDecimalTextIsBoundedInTimeAndAllocation() {
        final String text = "1." + "9".repeat(200_000);
        final SplitNumber value = n(text);

        // warm up so the measurement is not dominated by first-call class loading
        for (int i = 0; i < 2_000; i++) {
            Numbers.toInt(n("1.5"));
        }

        assertEquals(1, Numbers.toInt(value));

        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        if (bean instanceof com.sun.management.ThreadMXBean) {
            final com.sun.management.ThreadMXBean sun = (com.sun.management.ThreadMXBean) bean;
            final long id = Thread.currentThread().getId();
            final long before = sun.getThreadAllocatedBytes(id);
            Numbers.toInt(value);
            final long allocated = sun.getThreadAllocatedBytes(id) - before;

            // The BigDecimal this replaced allocated ~13.6 MB for this input. Nothing input-sized is created
            // now: only the DecimalTextScan holder and the exception-free scan.
            assertTrue(allocated < 8_192, "toInt((Object) hugeDecimalText) allocated " + allocated + " bytes");
        }

        // Doubling the digit count must not quadruple the cost. Measured before the fix: 431 ms -> 2128 ms for
        // 200k -> 400k digits; after: about 1 ms at either size. The bound is generous so the assertion is
        // about the complexity class, not the machine.
        final SplitNumber twiceAsLong = n("1." + "9".repeat(400_000));
        final long start = System.nanoTime();

        for (int i = 0; i < 50; i++) {
            Numbers.toInt(twiceAsLong);
        }

        final long millis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(millis < 2_000, "50 calls on a 400k-digit decimal toString() took " + millis + " ms");
    }

    @Test
    @DisplayName("B1: an out-of-range decimal toString() reports overflow without arbitrary precision")
    public void test_B1_hugeOutOfRangeDecimalTextReportsOverflow() {
        final SplitNumber huge = n("9".repeat(100_000) + ".5");
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toInt(huge));

        assertTrue(e.getMessage().startsWith("int overflow: "), e.getMessage());
        assertTrue(e.getMessage().contains("...[100002 chars]"), e.getMessage());
        assertTrue(e.getMessage().length() < 256, "message length " + e.getMessage().length());
    }

    @Test
    @DisplayName("B1: truncation toward zero matches a BigDecimal reference for every shape")
    public void test_B1_truncationMatchesBigDecimal() {
        final String[] texts = { "0", "-0", "0.0", "-0.0", "1", "-1", "+1", "1.9", "-1.9", "0.9", "-0.9", ".9", "-.9", "1.", "-1.", "010", "-010",
                "00000000000000000000001", "1e0", "1e1", "1e2", "1.5e1", "1.5e3", "1.55e1", "-1.55e1", "1e-1", "1.9e-1", "12345.6789e2", "12345.6789e-2",
                "9223372036854775807", "-9223372036854775808", "9223372036854775807.9", "-9223372036854775808.9", "922337203685477580.7e1",
                "0.000000000000000000009223372036854775807e21", "1e18", "-1e18", "9.223372036854775807e18", "-9.223372036854775808e18", "0.0000000001",
                "-0.0000000001", "1e-999999999", "0e999999999", "-0e999999999", "0.0e5" };

        for (final String text : texts) {
            final BigDecimal reference = new BigDecimal(text);
            final SplitNumber value = n(text);

            // long
            if (reference.compareTo(new BigDecimal(BigInteger.valueOf(Long.MIN_VALUE)).subtract(BigDecimal.ONE)) > 0
                    && reference.compareTo(new BigDecimal(BigInteger.valueOf(Long.MAX_VALUE)).add(BigDecimal.ONE)) < 0) {
                assertEquals(reference.longValue(), Numbers.toLong(value), text);
            } else {
                assertThrows(ArithmeticException.class, () -> Numbers.toLong(value), text);
            }

            // int, and the BigInteger target's truncation. Only computed for a scale toBigInteger() can
            // materialize: "0e999999999" scales up to a billion digits, and "1e-999999999" makes setScale(0)
            // divide by 10^999999999. Those extremes are pinned explicitly below instead.
            if (Math.abs((long) reference.scale()) <= 1_000 && reference.precision() - reference.scale() < 40) {
                final BigInteger truncated = reference.toBigInteger();

                if (truncated.bitLength() < 32) {
                    assertEquals(truncated.intValue(), Numbers.toInt(value), text);
                } else {
                    assertThrows(ArithmeticException.class, () -> Numbers.toInt(value), text);
                }

                assertEquals(truncated, Numbers.convert(value, BigInteger.class), text);
            }
        }

        // The extremes the reference cannot be asked about: an exponent far below one truncates to zero, and a
        // zero significand is zero at any exponent. Both are decided lexically, with no BigDecimal built.
        for (final String text : new String[] { "1e-999999999", "-1e-999999999", "0e999999999", "-0e999999999", "0.0e-999999999" }) {
            assertEquals(0, Numbers.toInt(n(text)), text);
            assertEquals(0L, Numbers.toLong(n(text)), text);
            assertEquals((byte) 0, Numbers.toByte(n(text)), text);
        }

        // ... and the mirror image: a magnitude far above every long is an overflow, not a wrap.
        for (final String text : new String[] { "1e999999999", "-1e999999999", "9.9e19", "1" + "0".repeat(30) }) {
            assertThrows(ArithmeticException.class, () -> Numbers.toLong(n(text)), text);
            assertThrows(ArithmeticException.class, () -> Numbers.toInt(n(text)), text);
        }
    }

    @Test
    @DisplayName("B1: the lexical recovery accepts exactly the BigDecimal grammar, restricted to ASCII digits")
    public void test_B1_grammarMatchesBigDecimalExceptForNonAsciiDigits() {
        // Accepted by both: the BigDecimal target reproduces the text exactly, so it came from the text and
        // not from doubleValue(). Compared as BigDecimal rather than BigInteger because a legal token such as
        // "0e999999999" has a billion-digit integer form.
        for (final String text : new String[] { "1", "+1", "-1", "1.", "+1.", ".5", "1.5", "1.e5", ".5e5", "1e5", "1E5", "1e+5", "1e-5", "010", "00",
                "1e0000000005", "1e999999999", "0e999999999", "1e2147483648" }) {
            assertEquals(new BigDecimal(text), Numbers.convert(n(text), BigDecimal.class), text);
        }

        // Rejected by both: the sentinel doubleValue() is used instead.
        final BigDecimal sentinel = BigDecimal.valueOf(SENTINEL);

        for (final String text : new String[] { ".", "1e", "1e+", "e5", "", " 1", "1 ", "1_0", "1.2.3", "--1", "1-", "1+1", "1e5e5", "1.5f", "NaN", "Infinity",
                "0x10", "#10", "1L", "١٢٣", "1۲1" }) {
            assertEquals(sentinel, Numbers.convert(n(text), BigDecimal.class), text);
            assertEquals((long) SENTINEL, Numbers.toLong(n(text)), text);
        }

        // An exponent BigDecimal itself refuses is rejected here too, so the two stay in step.
        for (final String text : new String[] { "1e-2147483648", "1e-2147483649", "0e-2147483648", "1e9999999999", "1e10000000000",
                "1e99999999999999999999" }) {
            assertThrows(NumberFormatException.class, () -> new BigDecimal(text), text);
            assertEquals(sentinel, Numbers.convert(n(text), BigDecimal.class), text);
            assertEquals((long) SENTINEL, Numbers.toLong(n(text)), text);
        }
    }

    @Test
    @DisplayName("B1: a null, empty or non-numeric toString() still falls back to doubleValue()")
    public void test_B1_nonCanonicalTextStillFallsBack() {
        assertEquals((int) SENTINEL, Numbers.toInt(new SplitNumber(null, SENTINEL)));
        assertEquals((int) SENTINEL, Numbers.toInt(n("")));
        assertEquals((int) SENTINEL, Numbers.toInt(n("42 ms")));
        assertEquals((int) SENTINEL, Numbers.toInt(n("1,234")));
        assertEquals((int) SENTINEL, Numbers.toInt(n(" 42 ")));

        // Non-finite doubleValue() on a non-canonical text is still an overflow, not an NPE or a wrap.
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(new SplitNumber("frob", Double.NaN)));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(new SplitNumber("frob", Double.POSITIVE_INFINITY)));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new SplitNumber("frob", Double.NaN), BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new SplitNumber(null, Double.NaN), BigDecimal.class));
    }

    @Test
    @DisplayName("B1: the byte/short ranges narrow by the same lexical rule")
    public void test_B1_narrowRangesUseTheSameRule() {
        assertEquals((byte) 127, Numbers.toByte(n("127.9")));
        assertEquals((byte) -128, Numbers.toByte(n("-128.9")));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(n("128.0")));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(n("-128.0000000001e1")));
        assertEquals((short) 32767, Numbers.toShort(n("32767.999")));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort(n("32768")));
        assertEquals((byte) 0, Numbers.toByte(n("0.9")));
        assertEquals((byte) 0, Numbers.toByte(n("-0.9")));
    }

    @Test
    @DisplayName("B1: the overflow message reuses the toString() the recovery already read")
    public void test_B1_overflowMessageDoesNotCallToStringTwice() {
        final int[] calls = { 0 };
        final Number counting = new Number() {
            private static final long serialVersionUID = 1L;

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0L;
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }

            @Override
            public String toString() {
                calls[0]++;
                return "99999999999999999999999";
            }
        };

        assertThrows(ArithmeticException.class, () -> Numbers.toInt(counting));
        assertEquals(1, calls[0], "toString() should be read once, not once for the value and once for the message");
    }

    // ==================================================================================================
    // J1 / J2: createNumber selects the type from the value alone, at any length. (The payload-length
    // routing these tests once pinned was removed on 2026-09-02: it flipped the type of a literal equal
    // to 1.0 at an arbitrary character count, and the JDK parser is linear in the token length.)
    // ==================================================================================================

    @Test
    @DisplayName("J1: the value selects the type on both sides of MAX_FLOATING_POINT_TOKEN_LENGTH")
    public void test_J1_lengthNeverSelectsTheType() {
        // The value here is exactly 1.0 at every length, so the type must not change with the length.
        assertSame(Double.class, Numbers.createNumber("1." + "0".repeat(4094)).getClass());
        assertSame(Double.class, Numbers.createNumber("1." + "0".repeat(4095)).getClass());
        assertEquals(Double.valueOf(1.0d), Numbers.createNumber("1." + "0".repeat(4095)));

        // Nor do leading zeros or the exponent.
        assertEquals(Double.valueOf(1.0d), Numbers.createNumber("1" + "0".repeat(310) + "e-310"));
        assertSame(Double.class, Numbers.createNumber("0".repeat(400) + "1.5").getClass());
    }

    @Test
    @DisplayName("J2: an f/d suffix is honoured at any length")
    public void test_J2_typeSuffixIsHonouredAtAnyLength() {
        assertSame(Float.class, Numbers.createNumber("1." + "0".repeat(4094) + "f").getClass());
        assertSame(Float.class, Numbers.createNumber("1." + "0".repeat(4095) + "f").getClass());
        assertSame(Double.class, Numbers.createNumber("1." + "0".repeat(4094) + "d").getClass());
        assertSame(Double.class, Numbers.createNumber("1." + "0".repeat(4095) + "d").getClass());

        // A lexical zero is still built as the correctly signed binary zero, suffix and all.
        assertEquals(Float.valueOf(0.0f), Numbers.createNumber("0." + "0".repeat(4095) + "f"));
        assertEquals(Float.valueOf(-0.0f), Numbers.createNumber("-0." + "0".repeat(4095) + "f"));

        // createNumber(s) and createNumber(s + "d") still agree on the result type at every length.
        for (final int zeros : new int[] { 10, 4093, 4094, 4095, 4096, 5000 }) {
            final String s = "1." + "0".repeat(zeros);
            assertSame(Numbers.createNumber(s).getClass(), Numbers.createNumber(s + "d").getClass(), "zeros=" + zeros);
        }
    }

    // ==================================================================================================
    // D4: the character table still works from its new declaration site
    // ==================================================================================================

    @Test
    @DisplayName("D4: moving the alphanumerics table above the methods changed nothing")
    public void test_D4_characterTableStillFilters() {
        assertEquals(new BigDecimal("1.5"), Numbers.parseBigDecimal("1.5"));
        assertEquals(BigInteger.valueOf(255), Numbers.decodeBigInteger("0xFF"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1۲1"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("1۲1"));
        assertNull(Numbers.parseBigDecimal(""));
        assertNotNull(Numbers.parseBigDecimal("1e2147483648"));
    }
}
