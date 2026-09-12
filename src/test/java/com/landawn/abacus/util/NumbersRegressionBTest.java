/*
 * Copyright (C) 2026 HaiYang Li
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-30h review of {@link Numbers}.
 *
 * <p>Covers, in order: the quadratic arbitrary-precision detour on the {@code Number}-object integral path
 * (B1), the {@code NullPointerException} raised for a subtype whose {@code toString()} returns {@code null}
 * (B2), and the behaviour pinned by the corrected Javadoc (D1, J1, J2, J3, J4) plus the unreachability
 * invariant the new {@code AssertionError} guards rely on (D3).</p>
 */
public class NumbersRegressionBTest extends TestBase {

    // =============================================================================================
    // Number subtypes outside the eight this class converts directly.
    // =============================================================================================

    /** {@code toString()} is exact decimal text, as an application fixed-point type's would be. */
    private static final class DecimalTextNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final BigDecimal value;

        DecimalTextNumber(final String text) {
            value = new BigDecimal(text);
        }

        @Override
        public int intValue() {
            return value.intValue();
        }

        @Override
        public long longValue() {
            return value.longValue();
        }

        @Override
        public float floatValue() {
            return value.floatValue();
        }

        @Override
        public double doubleValue() {
            return value.doubleValue();
        }

        @Override
        public String toString() {
            return value.toPlainString();
        }
    }

    /** {@code toString()} is a well-formed integer token of arbitrary length; {@code doubleValue()} overflows. */
    private static final class HugeIntegralTextNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final String text;

        HugeIntegralTextNumber(final int digits) {
            text = "9".repeat(digits);
        }

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
            return Float.POSITIVE_INFINITY;
        }

        @Override
        public double doubleValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** {@code toString()} is formatted text, so no numeric token can be recovered from it. */
    private static final class FormattedNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final double value;

        FormattedNumber(final double value) {
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
            return value + " ms";
        }
    }

    /** {@code toString()} returns {@code null}, which {@link Object#toString()} overrides are free to do. */
    private static final class NullTextNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final double value;

        NullTextNumber(final double value) {
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
            return null;
        }
    }

    // =============================================================================================
    // B1 - an out-of-range integer token must not be routed through BigDecimal.
    // =============================================================================================

    @Test
    @DisplayName("B1: an out-of-range integral toString() is rejected without an arbitrary-precision parse")
    public void test_B1_outOfRangeIntegerTokenDoesNotBuildABigDecimal() {
        final java.lang.management.ThreadMXBean threadBean = java.lang.management.ManagementFactory.getThreadMXBean();
        final com.sun.management.ThreadMXBean allocationBean = threadBean instanceof com.sun.management.ThreadMXBean
                ? (com.sun.management.ThreadMXBean) threadBean
                : null;

        if (allocationBean == null || !allocationBean.isThreadAllocatedMemorySupported()) {
            return;
        }

        if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
            allocationBean.setThreadAllocatedMemoryEnabled(true);
        }

        final HugeIntegralTextNumber huge = new HugeIntegralTextNumber(200_000);

        // Warm the path so class loading is not billed to the measured call.
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(huge));

        final long threadId = Thread.currentThread().getId();
        final long before = allocationBean.getThreadAllocatedBytes(threadId);
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(huge));
        final long allocated = allocationBean.getThreadAllocatedBytes(threadId) - before;

        // The scan is in place, so the rejection costs a constant amount of memory regardless of the token
        // length. Before the fix this fell through to new BigDecimal(text): 488 KB for a 100,000-digit token
        // and 967 KB for a 200,000-digit one, growing super-linearly. Measured after the fix: 1,224 bytes.
        Assertions.assertTrue(allocated < 64 * 1024, "rejecting a 200,000-digit integer token allocated " + allocated + " bytes");
    }

    @Test
    @DisplayName("B1: the rejection is still ArithmeticException, with a bounded message naming the value")
    public void test_B1_outOfRangeIntegerTokenStillReportsBoundedArithmeticException() {
        final HugeIntegralTextNumber huge = new HugeIntegralTextNumber(10_000);

        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toInt(huge));
        assertTrue(e.getMessage().startsWith("int overflow: 99999999"), e.getMessage());
        assertTrue(e.getMessage().endsWith("...[10000 chars]"), e.getMessage());
        assertTrue(e.getMessage().length() < 200, e.getMessage());

        // Every integral target reports the same way, through both entry points.
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(huge));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort(huge));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(huge));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Short.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Integer.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Long.class));
    }

    @Test
    @DisplayName("B1: a small out-of-range integer token keeps its historical message")
    public void test_B1_smallOutOfRangeTokenMessageIsUnchanged() {
        assertEquals("byte overflow: 1234", assertThrows(ArithmeticException.class, () -> Numbers.toByte(new AtomicInteger(1234))).getMessage());
        assertEquals("byte overflow: 1234", assertThrows(ArithmeticException.class, () -> Numbers.convert(new AtomicInteger(1234), Byte.class)).getMessage());
        assertEquals("short overflow: 40000", assertThrows(ArithmeticException.class, () -> Numbers.toShort(new AtomicInteger(40000))).getMessage());

        final LongAdder adder = new LongAdder();
        adder.add(3_000_000_000L);
        assertEquals("int overflow: 3000000000", assertThrows(ArithmeticException.class, () -> Numbers.toInt(adder)).getMessage());
    }

    @Test
    @DisplayName("B1: the decimal and doubleValue() steps are still reached for text that is not an integer token")
    public void test_B1_nonIntegerTextStillTakesTheLaterSteps() {
        // Fractional text -> the BigDecimal step, truncating toward zero.
        assertEquals(12, Numbers.toInt(new DecimalTextNumber("12.9")));
        assertEquals(-12, Numbers.toInt(new DecimalTextNumber("-12.9")));
        assertEquals((byte) 127, Numbers.toByte(new DecimalTextNumber("127.9")));

        // The boundary the BigDecimal step exists for: the integer part is exactly Long.MAX_VALUE, but
        // doubleValue() rounds the whole value up to 2^63 and would wrongly reject it.
        final DecimalTextNumber edge = new DecimalTextNumber("9223372036854775807.9");
        assertEquals(9.223372036854776E18, edge.doubleValue(), "precondition: doubleValue rounds up to 2^63");
        assertEquals(Long.MAX_VALUE, Numbers.toLong(edge));
        assertEquals(Long.MIN_VALUE, Numbers.toLong(new DecimalTextNumber("-9223372036854775808.9")));

        // Non-canonical text -> the doubleValue() step, not a parse failure.
        assertEquals(12, Numbers.toInt(new FormattedNumber(12.9)));
        assertEquals(12L, Numbers.toLong(new FormattedNumber(12.9)));

        // A fractional value out of range is still an overflow, decided from the exact decimal.
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(new DecimalTextNumber("128.9")));
    }

    @Test
    @DisplayName("B1: an out-of-range integer token now reports overflow exactly as the String parser does")
    public void test_B1_objectPathAgreesWithTheStringParserOnOverflow() {
        // Before the fix the object path could not tell "out of range" from "malformed", so an L-suffixed
        // token -- which BigDecimal cannot parse -- fell all the way through to doubleValue() and silently
        // returned a value unrelated to the text. toByte("999L") threw while toByte((Object) x) did not.
        final Number lSuffixed = new Number() {
            private static final long serialVersionUID = 1L;

            @Override
            public int intValue() {
                return 5;
            }

            @Override
            public long longValue() {
                return 5L;
            }

            @Override
            public float floatValue() {
                return 5f;
            }

            @Override
            public double doubleValue() {
                return 5d;
            }

            @Override
            public String toString() {
                return "999L";
            }
        };

        // Superseded by 2026-08-30i for the L-suffixed spelling only. This pass proved that accepting the
        // to*(String) text grammar here made the integral targets answer from the text while the
        // BigInteger/BigDecimal targets answered from doubleValue(), because no BigDecimal can be built from
        // "999L". The recovery grammar is now decimal-only, so an L-suffixed toString() is "non-canonical
        // text" like any other formatted rendering and takes the documented doubleValue() route.
        assertEquals(5, Numbers.toByte(lSuffixed));
        assertEquals(5, Numbers.toInt(lSuffixed));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("999L"));

        // What B1 was actually about is unchanged: an out-of-range *canonical* integer token reports overflow
        // from the object path with exactly the message the String parser produces, and does not fall through
        // to doubleValue().
        final String viaString = assertThrows(ArithmeticException.class, () -> Numbers.toByte("999")).getMessage();
        final String viaObject = assertThrows(ArithmeticException.class, () -> Numbers.toByte(new DecimalTextNumber("999"))).getMessage();
        assertEquals(viaString, viaObject);
        assertEquals("byte overflow: 999", viaObject);

        // In-range canonical values are still read from the text.
        assertEquals(999, Numbers.toInt(new DecimalTextNumber("999")));

        // Text that is a decimal but not an integer token is still truncated toward zero on both routes.
        assertEquals(12, Numbers.toInt(new DecimalTextNumber("12.9")));
    }

    @Test
    @DisplayName("B1: an in-range integer token is unaffected, including the Long.MIN_VALUE collision")
    public void test_B1_inRangeTokensAreUnaffected() {
        assertEquals(1234, Numbers.toInt(new AtomicInteger(1234)));
        assertEquals(-5L, Numbers.toLong(new AtomicLong(-5L)));
        assertEquals(Long.MIN_VALUE, Numbers.toLong(new DecimalTextNumber("-9223372036854775808")));
        assertEquals(Long.MAX_VALUE, Numbers.toLong(new DecimalTextNumber("9223372036854775807")));
        assertEquals(0, Numbers.toInt(new DecimalTextNumber("0")));
        assertEquals(1, Numbers.toInt(new DecimalTextNumber("000000000000000000000000001")));
    }

    // =============================================================================================
    // B2 - a Number whose toString() returns null.
    // =============================================================================================

    @Test
    @DisplayName("B2: a null toString() falls back to doubleValue() instead of raising NullPointerException")
    public void test_B2_nullToStringCoercesViaDoubleValue() {
        final NullTextNumber value = new NullTextNumber(12.9);

        assertEquals((byte) 12, Numbers.toByte(value));
        assertEquals((short) 12, Numbers.toShort(value));
        assertEquals(12, Numbers.toInt(value));
        assertEquals(12L, Numbers.toLong(value));
        assertEquals(12.9f, Numbers.toFloat(value));
        assertEquals(12.9d, Numbers.toDouble(value));

        assertEquals(Byte.valueOf((byte) 12), Numbers.convert(value, Byte.class));
        assertEquals(Integer.valueOf(12), Numbers.convert(value, Integer.class));
        assertEquals(Long.valueOf(12L), Numbers.convert(value, Long.class));
        assertEquals(Float.valueOf(12.9f), Numbers.convert(value, Float.class));
        assertEquals(Double.valueOf(12.9d), Numbers.convert(value, Double.class));
        assertEquals(BigInteger.valueOf(12), Numbers.convert(value, BigInteger.class));
        assertEquals(BigDecimal.valueOf(12.9d), Numbers.convert(value, BigDecimal.class));
    }

    @Test
    @DisplayName("B2: a null toString() on an unrepresentable value reports overflow, naming the type")
    public void test_B2_nullToStringOverflowNamesTheTypeInsteadOfThrowingNpe() {
        final NullTextNumber notFinite = new NullTextNumber(Double.NaN);
        final NullTextNumber outOfRange = new NullTextNumber(1e30d);

        for (final Number source : new Number[] { notFinite, outOfRange }) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toInt(source));
            assertTrue(e.getMessage().startsWith("int overflow: "), e.getMessage());
            assertTrue(e.getMessage().contains(NullTextNumber.class.getName()), e.getMessage());
            assertTrue(e.getMessage().endsWith("(toString() returned null)"), e.getMessage());
        }

        // The arbitrary-precision targets reject a non-finite value the same way.
        final ArithmeticException big = assertThrows(ArithmeticException.class, () -> Numbers.convert(notFinite, BigDecimal.class));
        assertTrue(big.getMessage().startsWith("BigDecimal overflow: "), big.getMessage());
        assertThrows(ArithmeticException.class, () -> Numbers.convert(notFinite, BigInteger.class));

        // ... and a finite out-of-range one still converts where the target is wide enough. With no text to
        // recover, the value comes from doubleValue(), and each target then applies the rule it applies to a
        // Double source: the BigDecimal target keeps the canonical decimal spelling, the BigInteger target
        // truncates the EXACT value (2026-09-02: it used to truncate the spelling, 10^30, which is not the
        // value of the double 1e30 and differed from what convert(1e30d, BigInteger.class) answers).
        assertEquals(1e30d, Numbers.convert(outOfRange, Double.class));
        assertEquals(BigDecimal.valueOf(1e30d), Numbers.convert(outOfRange, BigDecimal.class));
        assertEquals(new BigDecimal(1e30d).toBigInteger(), Numbers.convert(outOfRange, BigInteger.class));
        assertEquals(new BigInteger("1000000000000000019884624838656"), Numbers.convert(outOfRange, BigInteger.class));
        assertEquals(Numbers.convert(1e30d, BigInteger.class), Numbers.convert(outOfRange, BigInteger.class));
    }

    // =============================================================================================
    // D1 - the extract family saturates where the integer finders throw.
    // =============================================================================================

    @Test
    @DisplayName("D1: extractFirstDouble saturates to Infinity where extractFirstInt throws")
    public void test_D1_extractFirstDoubleSaturatesInsteadOfThrowing() {
        final String tooBig = "9".repeat(400);

        assertEquals(u.OptionalDouble.of(Double.POSITIVE_INFINITY), Numbers.extractFirstDouble(tooBig));
        assertEquals(u.OptionalDouble.of(Double.POSITIVE_INFINITY), Numbers.extractFirstDouble(tooBig, true));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.extractFirstDoubleOrElse(tooBig, 0d));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.extractFirstDoubleOrElse(tooBig, 0d, true));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.extractFirstDoubleOrElse("x=-" + tooBig, 0d));

        // A token that underflows becomes a signed zero rather than an absence.
        assertEquals(0d, Numbers.extractFirstDoubleOrElse("0." + "0".repeat(400) + "1", 7d));
        assertEquals("-0.0", Double.toString(Numbers.extractFirstDoubleOrElse("-1e-400", -7d, true)));

        // The integer finders keep the opposite policy: out of range is an error, not a value.
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstInt(tooBig));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong(tooBig));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstIntOrElse(tooBig, 0));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLongOrElse(tooBig, 0L));

        // An over-long token is still an error for the double family, which is the case that does throw.
        final String overLong = "1".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 1);
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDouble(overLong));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDoubleOrElse(overLong, 0d));
    }

    // =============================================================================================
    // J1/J2/J3 - behaviour pinned by the corrected Javadoc.
    // =============================================================================================

    @Test
    @DisplayName("J1: createNumber picks the type from the value, never from the length of the literal")
    public void test_J1_createNumberTypeFollowsTheValueNotTheLiteralLength() {
        // The Javadoc used to claim this returned a BigDecimal via a "longer than 308 characters" shortcut.
        // That shortcut no longer exists: the value is an ordinary double, so the result is a Double.
        assertEquals(Double.valueOf(1.0d), Numbers.createNumber("1" + "0".repeat(310) + "e-310"));
        assertEquals(Double.valueOf(1.5d), Numbers.createNumber("0".repeat(400) + "1.5"));
        assertEquals(Double.valueOf(1.0d), Numbers.createNumber("1." + "0".repeat(400) + "5"));

        // A suffix never changes the chosen type for the same numeric payload.
        for (final String literal : new String[] { "1" + "0".repeat(310) + "e-310", "0".repeat(400) + "1.5", "1." + "0".repeat(400) + "5" }) {
            assertSame(Numbers.createNumber(literal).getClass(), Numbers.createNumber(literal + "d").getClass(), literal);
        }

        // A value that genuinely does not fit a double still escalates, at any length.
        assertEquals(BigDecimal.class, Numbers.createNumber("1e400").getClass());
        assertEquals(BigDecimal.class, Numbers.createNumber("0." + "0".repeat(400) + "1").getClass());

        // The create family applies no length limit at all (2026-09-02): a payload past
        // MAX_FLOATING_POINT_TOKEN_LENGTH is parsed like any other and the value selects the type.
        final String overLongPayload = "1." + "3".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH);
        assertEquals(Double.valueOf(1.3333333333333333d), Numbers.createNumber(overLongPayload));
        assertEquals(Double.class, Numbers.createNumber("1." + "3".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 2)).getClass());
    }

    @Test
    @DisplayName("J2: toLong(Object) recovers a non-canonical Number subtype exactly like its byte/short/int siblings")
    public void test_J2_toLongMatchesItsSiblingsForANonCanonicalSubtype() {
        final FormattedNumber formatted = new FormattedNumber(12.9);
        assertEquals("12.9 ms", formatted.toString(), "precondition: toString() is not a numeric token");

        assertEquals(12L, Numbers.toLong(formatted));
        assertEquals(12L, Numbers.toLong(formatted, 0L));
        assertEquals((byte) 12, Numbers.toByte(formatted));
        assertEquals((short) 12, Numbers.toShort(formatted));
        assertEquals(12, Numbers.toInt(formatted));
        assertEquals(Long.valueOf(12L), Numbers.convert(formatted, Long.class));

        // The whole family agrees, which is what the shared wording now promises.
        assertEquals(Numbers.toInt(formatted), (int) Numbers.toLong(formatted));

        // A DoubleAdder is the JDK's own instance of the same shape.
        final DoubleAdder adder = new DoubleAdder();
        adder.add(12.9);
        assertEquals(12L, Numbers.toLong(adder));
        assertEquals(12, Numbers.toInt(adder));
    }

    @Test
    @DisplayName("J3: the token length limit bounds the matched token, not the argument it was found in")
    public void test_J3_extractFirstDoubleLimitsTheTokenNotTheArgument() {
        final String haystack = "x".repeat(50_000) + "1.5" + "y".repeat(50_000);
        assertEquals(100_003, haystack.length());
        assertEquals(u.OptionalDouble.of(1.5d), Numbers.extractFirstDouble(haystack));
        assertEquals(1.5d, Numbers.extractFirstDoubleOrElse(haystack, 0d));

        // Whereas the bounded parsers do measure the raw argument.
        assertFalse(Numbers.isParsable(" ".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH) + "1.5"));
        assertThrows(NumberFormatException.class, () -> Numbers.toDouble(" ".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH) + "1.5"));
    }

    // =============================================================================================
    // J4 - the boxed format overloads call the overload their @see names.
    // =============================================================================================

    @Test
    @DisplayName("J4: every boxed format overload renders exactly like its primitive counterpart")
    public void test_J4_boxedFormatOverloadsMatchThePrimitiveOnes() {
        final Locale saved = Locale.getDefault(Locale.Category.FORMAT);

        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);

            final String[] patterns = { "#,###", "0.00", "$#,##0.00", "#.##%", "hello", "#,##0.000" };

            for (final String pattern : patterns) {
                for (final int i : new int[] { 0, 1, -1234, 1234, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
                    assertEquals(Numbers.format(i, pattern), Numbers.format(Integer.valueOf(i), pattern), pattern + " / " + i);
                }

                for (final long l : new long[] { 0L, -123456789L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE }) {
                    assertEquals(Numbers.format(l, pattern), Numbers.format(Long.valueOf(l), pattern), pattern + " / " + l);
                }

                for (final float f : new float[] { 0f, -12.105f, 12.105f, 0.121f, Float.NaN, Float.POSITIVE_INFINITY }) {
                    assertEquals(Numbers.format(f, pattern), Numbers.format(Float.valueOf(f), pattern), pattern + " / " + f);
                }

                for (final double d : new double[] { 0d, -12.105d, 12.105d, 0.12156d, Double.NaN, Double.NEGATIVE_INFINITY }) {
                    assertEquals(Numbers.format(d, pattern), Numbers.format(Double.valueOf(d), pattern), pattern + " / " + d);
                }
            }

            // The documented spellings are unchanged.
            assertEquals("1,234", Numbers.format(Integer.valueOf(1234), "#,###"));
            assertEquals("123,456,789", Numbers.format(Long.valueOf(123456789L), "#,###"));
            assertEquals("12.10", Numbers.format(Float.valueOf(12.105f), "0.00"));
            assertEquals("12.11", Numbers.format(Double.valueOf(12.105d), "0.00"));

            // null is still the null result, and a null pattern is still rejected first.
            assertNull(Numbers.format((Integer) null, "#,###"));
            assertNull(Numbers.format((Long) null, "#,###"));
            assertNull(Numbers.format((Float) null, "#,###"));
            assertNull(Numbers.format((Double) null, "#,###"));
            assertThrows(IllegalArgumentException.class, () -> Numbers.format(Integer.valueOf(1), (String) null));
            assertThrows(IllegalArgumentException.class, () -> Numbers.format((Integer) null, (String) null));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, saved);
        }
    }

    // =============================================================================================
    // D3 - the invariant behind the AssertionError guards.
    // =============================================================================================

    @Test
    @DisplayName("D3: the two integer scanners agree, so no input reaches an AssertionError guard")
    public void test_D3_scannerGuardsAreUnreachableForEveryToken() {
        // parseLongInRange and decodeInteger now fail loudly if the range-limited scan and the diagnostic
        // scan ever disagree, matching decodeLong. Sweep the grammar to show that they do not: any
        // AssertionError escaping here would be that disagreement.
        sweepScannerAgreement("0123456789.eE+-xX#lLfFdD", 3);
        sweepScannerAgreement("0123456789+-xX#lL", 4);

        // Plus the boundary tokens most likely to expose a sentinel collision.
        final String[] boundaries = { "-9223372036854775808", "9223372036854775807", "-9223372036854775809", "9223372036854775808", "-2147483648", "2147483647",
                "-2147483649", "2147483648", "0x8000000000000000", "-0x8000000000000000", "#8000000000000000", "-#8000000000000000", "-01000000000000000000000",
                "-9223372036854775808L", "0000000000000009223372036854775807", "-0000000000000009223372036854775808", "0x7FFFFFFFFFFFFFFF", "-0x1" };

        for (final String token : boundaries) {
            exerciseScanners(token);
        }

        // The one documented sentinel collision still round-trips.
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-9223372036854775808"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-9223372036854775808"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-0x8000000000000000"));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), Numbers.decodeInteger("-2147483648"));
        assertEquals(Integer.MIN_VALUE, Numbers.toInt("-2147483648"));
    }

    private static void sweepScannerAgreement(final String alphabet, final int maxLen) {
        final char[] buf = new char[maxLen];

        for (int len = 1; len <= maxLen; len++) {
            int total = 1;

            for (int i = 0; i < len; i++) {
                total *= alphabet.length();
            }

            for (int code = 0; code < total; code++) {
                int c = code;

                for (int i = 0; i < len; i++) {
                    buf[i] = alphabet.charAt(c % alphabet.length());
                    c /= alphabet.length();
                }

                exerciseScanners(new String(buf, 0, len));
            }
        }
    }

    /** Runs every entry point that carries an {@code AssertionError} guard. Only that error escapes. */
    private static void exerciseScanners(final String token) {
        try {
            Numbers.toByte(token);
        } catch (final NumberFormatException | ArithmeticException expected) {
            // Either outcome is fine; only an AssertionError is a failure.
        }

        try {
            Numbers.toShort(token);
        } catch (final NumberFormatException | ArithmeticException expected) {
            // Either outcome is fine.
        }

        try {
            Numbers.toInt(token);
        } catch (final NumberFormatException | ArithmeticException expected) {
            // Either outcome is fine.
        }

        try {
            Numbers.toLong(token);
        } catch (final NumberFormatException | ArithmeticException expected) {
            // Either outcome is fine.
        }

        try {
            Numbers.decodeInteger(token);
        } catch (final NumberFormatException expected) {
            // Either outcome is fine.
        }

        try {
            Numbers.decodeLong(token);
        } catch (final NumberFormatException expected) {
            // Either outcome is fine.
        }
    }
}
