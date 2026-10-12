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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NumbersFormattingTest extends NumbersTestSupport {

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

    // format

    // J4 - the boxed format overloads call the overload their @see names.
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
    public void testFormat_CacheEviction() {
        final Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            final List<String> patterns = new ArrayList<>();
            for (int i = 1; i <= 40; i++) {
                patterns.add("#,##0." + "0".repeat(1 + i % 6) + (i % 2 == 0 ? "" : ";(#)"));
            }
            for (int pass = 0; pass < 3; pass++) {
                for (final String pattern : patterns) {
                    final DecimalFormat reference = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
                    reference.setRoundingMode(RoundingMode.HALF_EVEN);
                    assertEquals(reference.format(1234.56789d), Numbers.format(1234.56789d, pattern), pattern);
                    assertEquals(reference.format(-1234.56789d), Numbers.format(-1234.56789d, pattern), pattern);
                }
            }
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }

    @Test
    public void testFormat_double() {
        final double val = 12.1050f;
        assertEquals("12", Numbers.format(val, "0"));
        assertEquals("12.1", Numbers.format(val, "0.0"));
        assertEquals("12.10", Numbers.format(val, "0.00"));
        assertEquals("12.105", Numbers.format(val, "#.#####"));
        assertEquals("12.11", Numbers.format(12.105, "0.00"));
        assertEquals("12.16%", Numbers.format(0.12156, "#.##%"));
        assertEquals("12.10%", Numbers.format(0.121, "0.00%"));
        assertEquals("2", Numbers.format(2.5, "0"));
        assertEquals("4", Numbers.format(3.5, "0"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(12.1, (String) null));
    }

    @Test
    public void testFormat_Double() {
        assertEquals("3.14159", Numbers.format(3.14159265359, "0.00000"));
        assertEquals("2.71828", Numbers.format(Double.valueOf(2.718281828), "0.00000"));
        assertEquals("12.16%", Numbers.format(Double.valueOf(0.12156), "#.##%"));
        assertNotNull(Numbers.format(Double.valueOf(123.4), "0.00E0"));
        assertNull(Numbers.format((Double) null, "0.00"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(Double.valueOf(12.1), null));
    }

    @Test
    public void testFormat_float() {
        assertEquals("12.10", Numbers.format(12.105f, "0.00"));
        assertEquals("12.1", Numbers.format(12.105f, "#.##"));
        assertEquals("3.14", Numbers.format(3.14f, "0.00"));
        assertEquals("12.3%", Numbers.format(0.123f, "0.0%"));
        assertEquals("12.1%", Numbers.format(0.121f, "#.##%"));
        assertEquals("2", Numbers.format(2.5f, "0"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(12.1f, null));
    }

    @Test
    public void testFormat_Float() {
        assertEquals("2.72", Numbers.format(Float.valueOf(2.71828f), "0.00"));
        assertNotNull(Numbers.format(Float.valueOf(123.4f), "0.00E0"));
        assertNull(Numbers.format((Float) null, "0.00"));
        assertNull(Numbers.format((Float) null, "0.00E0"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(Float.valueOf(12.1f), null));
    }

    // C-003: an illegal DecimalFormat pattern must not put the whole pattern in the message
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

    @Test
    public void testFormat_int() {
        assertEquals("123", Numbers.format(123, "0"));
        assertEquals("123", Numbers.format(123, "#"));
        assertEquals("123.00", Numbers.format(123, "0.00"));
        assertEquals("1,234", Numbers.format(1234, "#,###"));
        assertEquals("12300%", Numbers.format(123, "0%"));
        assertEquals("12,345.000", Numbers.format(12345, "#,##0.000"));
        assertEquals("hello1", Numbers.format(1, "hello"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(123, null));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(1, "'unterminated"));
    }

    @Test
    public void testFormat_Integer() {
        assertEquals("456", Numbers.format(Integer.valueOf(456), "#"));
        assertEquals("123.00", Numbers.format(Integer.valueOf(123), "0.00"));
        assertEquals("1,234", Numbers.format(Integer.valueOf(1234), "#,000"));
        assertNull(Numbers.format((Integer) null, "0"));
        assertNull(Numbers.format((Integer) null, "#,000"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(Integer.valueOf(123), null));
    }

    @Test
    public void testFormat_LocaleAndCache() {
        final Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertEquals("1,234.5", Numbers.format(1234.5, "#,##0.0"));
            assertEquals("200.00", Numbers.format(200, "0.00"));
            assertEquals("12,345.000", Numbers.format(12345, "#,##0.000"));

            Locale.setDefault(Locale.GERMANY);
            assertEquals("1.234,5", Numbers.format(1234.5, "#,##0.0"));
            assertEquals("1.234,50", Numbers.format(1234.5, "#,##0.00"));
            assertEquals("1234,50", Numbers.format(1234.5, "0.00"));

            Locale.setDefault(Locale.US);
            assertEquals("1,234.5", Numbers.format(1234.5, "#,##0.0"));
            assertEquals("1234.50", Numbers.format(1234.5, "0.00"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void testFormat_long() {
        assertEquals("789", Numbers.format(789L, "#"));
        assertEquals("789.00", Numbers.format(789L, "0.00"));
        assertEquals("1,234,567,890", Numbers.format(1234567890L, "#,###"));
        assertTrue(Numbers.format(9876543210L, "#,##0.00").contains("9,876,543,210"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(123L, null));
    }

    @Test
    public void testFormat_Long() {
        assertEquals("101112", Numbers.format(Long.valueOf(101112L), "#"));
        assertEquals("1,234", Numbers.format(Long.valueOf(1234L), "#,000"));
        assertNull(Numbers.format((Long) null, "0"));
        assertNull(Numbers.format((Long) null, "#,000"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(Long.valueOf(123), null));
    }

    @Test
    public void testFormat_NotSharedBetweenThreads() throws Exception {
        final Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            final String[] patterns = { "#,##0.00", "#,##0.000", "0.0####", "#.##%", "#,##0.0;(#)" };
            final String[] expected = new String[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                expected[i] = Numbers.format(1234.56789d, patterns[i]);
            }

            final List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
            final Thread[] threads = new Thread[4];
            for (int t = 0; t < threads.length; t++) {
                threads[t] = new Thread(() -> {
                    try {
                        for (int i = 0; i < 5_000; i++) {
                            final int p = i % patterns.length;
                            assertEquals(expected[p], Numbers.format(1234.56789d, patterns[p]));
                        }
                    } catch (final Throwable e) {
                        failures.add(e);
                    }
                });
                threads[t].start();
            }
            for (final Thread thread : threads) {
                thread.join();
            }
            assertTrue(failures.isEmpty(), () -> "concurrent formatting failed: " + failures);
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }

    @Test
    public void testFormat_PooledGroupingPatterns() {
        final Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            assertEquals("1,234,567.89", Numbers.format(1234567.891d, "#,##0.00"));
            assertEquals("1,234,567.9", Numbers.format(1234567.891d, "#,##0.0"));
            assertEquals("1,234,567", Numbers.format(1234567, "#,##0"));
            assertEquals("0", Numbers.format(0, "#,##0"));
            assertEquals("-1,234,567.89", Numbers.format(-1234567.891d, "#,##0.00"));
            assertEquals("$2,000.00", Numbers.format(2000, "$#,###.00"));

            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            assertEquals("1.234,50", Numbers.format(1234.5d, "#,##0.00"));
            assertEquals("1.234,500", Numbers.format(1234.5d, "#,##0.000"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }

    // C-014: the format family's signed-zero and infinity rendering, now documented
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

    // D2: the DecimalFormat cache keeps working exactly as before, with a JDK-only thread-local value
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
    @DisplayName("D2: a malformed pattern is still rejected before anything is cached")
    public void test_D2_malformedPatternIsNotCached() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(1, "'unterminated"));
        assertThrows(IllegalArgumentException.class, () -> Numbers.format(1, "'unterminated"));
        assertEquals("1", Numbers.format(1, "0"));
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
}
