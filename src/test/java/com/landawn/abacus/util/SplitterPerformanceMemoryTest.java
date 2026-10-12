package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Splitter. */
@Tag("slow-test")
public class SplitterPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G067 end ----
    // ---- perf review 2026-10-08 G068 begin ----
    @Nested
    class G068 {

        private final String vt = String.valueOf((char) 0x0B);
        private final String nel = String.valueOf((char) 0x85);
        private final String ls = String.valueOf((char) 0x2028);
        private final String ps = String.valueOf((char) 0x2029);
        private final String high = String.valueOf((char) 0xD83D);
        private final String low = String.valueOf((char) 0xDE00);

        private Splitter configure(final Splitter splitter, final boolean omit, final int mode, final int limit) {
            if (omit) {
                splitter.omitEmptyStrings();
            }

            if (mode == 1) {
                splitter.trimResults();
            } else if (mode == 2) {
                splitter.stripResults();
            }

            if (limit > 0) {
                splitter.limit(limit);
            }

            return splitter;
        }

        private String randomLineSource(final Random rnd, final String[] alphabet, final int maxLength) {
            final int length = rnd.nextInt(maxLength + 1);
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < length; i++) {
                sb.append(alphabet[rnd.nextInt(alphabet.length)]);
            }

            return sb.toString();
        }

        // G068-01: forLines() scans a String source by char; it must split exactly where the pattern engine for \R does.
        @Test
        public void testForLines_stringSourceMatchesPatternEngine() {
            final String[] alphabet = { "a", "b", " ", "\t", "\n", "\r", "\r\n", vt, "\f", nel, ls, ps, high, low, high + low, "\u001C",
                    String.valueOf((char) 0xA0), String.valueOf((char) 0x3000), "\r\r\n", "\n\r", "xyz" };
            final Random rnd = new Random(68);
            final List<String> sources = new ArrayList<>(Arrays.asList("", "\n", "\r", "\r\n", "\n\r", "\r\r", "\r\n\r\n", "a", "a\n", "\na", "a\rb", "a\r\nb",
                    " \n ", vt + "\f" + nel + ls + ps, "x\r", "\r\nx"));

            for (int i = 0; i < 1500; i++) {
                sources.add(randomLineSource(rnd, alphabet, i % 10 == 0 ? 50 : 10));
            }

            final int[] limits = { 0, 1, 2, 3, 5 };

            for (final String source : sources) {
                for (int omit = 0; omit < 2; omit++) {
                    for (int mode = 0; mode < 3; mode++) {
                        for (final int limit : limits) {
                            final Splitter lines = configure(Splitter.forLines(), omit == 1, mode, limit);
                            final Splitter reference = configure(Splitter.with(RegExUtil.LINE_SEPARATOR), omit == 1, mode, limit);
                            final List<String> expected = reference.split(source);
                            final String context = "source=" + Arrays.toString(source.toCharArray()) + " omit=" + omit + " mode=" + mode + " limit=" + limit;

                            assertEquals(expected, lines.split(source), context);
                            assertEquals(expected, lines.splitToStream(source).toList(), context);
                            assertArrayEquals(expected.toArray(new String[0]), lines.splitToArray(source), context);
                        }
                    }
                }
            }
        }

        // G068-01: explicit separator set of \R, incl. "\r\n" as one separator and characters that must not split.
        @Test
        public void testForLines_explicitSeparatorsAndNonSeparators() {
            final Splitter lines = Splitter.forLines();

            assertEquals(Arrays.asList("a", "b"), lines.split("a\r\nb"));
            assertEquals(Arrays.asList("a", "", "b"), lines.split("a\r\rb"));
            assertEquals(Arrays.asList("a", "", "b"), lines.split("a\n\rb"));
            assertEquals(Arrays.asList("a", "", "b"), lines.split("a\r\n\nb"));
            assertEquals(Arrays.asList("", ""), lines.split("\r"));
            assertEquals(Arrays.asList("", ""), lines.split("\r\n"));
            assertEquals(Arrays.asList("a", ""), lines.split("a\r"));
            assertEquals(Arrays.asList(""), lines.split(""));
            assertEquals(Collections.emptyList(), lines.split((CharSequence) null));

            for (final String separator : new String[] { "\n", "\r", "\r\n", vt, "\f", nel, ls, ps }) {
                assertEquals(Arrays.asList("x", "y", ""), lines.split("x" + separator + "y" + separator), Arrays.toString(separator.toCharArray()));
            }

            for (final String other : new String[] { "\t", " ", "\u001C", "\u001D", "\u001E", "\u001F", String.valueOf((char) 0x0E),
                    String.valueOf((char) 0x09), String.valueOf((char) 0x84), String.valueOf((char) 0x86), String.valueOf((char) 0x2027),
                    String.valueOf((char) 0x202A), String.valueOf((char) 0xA0), high, low }) {
                assertEquals(Arrays.asList("x" + other + "y"), lines.split("x" + other + "y"), Arrays.toString(other.toCharArray()));
            }

            assertEquals(Arrays.asList(high + low, "a" + high, low), lines.split(high + low + "\na" + high + "\r\n" + low));
            assertEquals(Arrays.asList("a", "b\r\nc\n"), Splitter.forLines().limit(2).split("a\r\nb\r\nc\n"));
            assertEquals(Arrays.asList("a", "b"), Splitter.forLines().stripResults().omitEmptyStrings().limit(2).split(" a \r\n\r\n\t b \n"));
            assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(Strings.splitToLines("a\nb\r\nc")));
        }

        // G068-01: non-String sources keep the pattern engine; the iterator protocol is unchanged.
        @Test
        public void testForLines_nonStringSourcesAndIteratorProtocol() {
            final String text = " a\r\n\r\nb " + nel + "c" + ls + ps + "\r";
            final List<String> expected = Splitter.with(RegExUtil.LINE_SEPARATOR).split(text);

            assertEquals(Arrays.asList(" a", "", "b ", "c", "", "", ""), expected);
            assertEquals(expected, Splitter.forLines().split(text));
            assertEquals(expected, Splitter.forLines().split(new StringBuilder(text)));
            assertEquals(expected, Splitter.forLines().split(java.nio.CharBuffer.wrap(text)));

            final Iterator<String> iter = Splitter.forLines().omitEmptyStrings().splitToStream("x\n\ny").iterator();

            assertTrue(iter.hasNext());
            assertTrue(iter.hasNext());
            assertEquals("x", iter.next());
            assertEquals("y", iter.next());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::next);

            final Splitter shared = Splitter.forLines();
            final String[] first = shared.splitToArray("1\n2");
            final String[] second = shared.splitToArray("1\n2");

            assertArrayEquals(new String[] { "1", "2" }, first);
            assertNotSame(first, second);
        }

        // G068-02: splitToArray to a primitive array stores each converted token directly instead of via reflective Array.set.
        @Test
        public void testSplitToArray_primitiveComponentTypesMatchBoxedConversion() {
            final Object[][] cases = { { int[].class, int.class, "1,-2,,2147483647,-2147483648,0,007,+5" },
                    { long[].class, long.class, "1,-2,,9223372036854775807,-9223372036854775808,0" },
                    { double[].class, double.class, "1.5,-0.0,,NaN,Infinity,-Infinity,4.9e-324,1e308,0" },
                    { float[].class, float.class, "1.5,-0.0,,NaN,Infinity,-Infinity,3.4028235e38,1e-45" },
                    { boolean[].class, boolean.class, "true,false,,1,0,-1,y,Y,n,N,TRUE,False" },
                    { char[].class, char.class, "a,,Z,0,9, ," + high + "," + low + "," + String.valueOf((char) 0xE9) },
                    { byte[].class, byte.class, "1,-2,,127,-128,0" }, { short[].class, short.class, "1,-2,,32767,-32768,0" } };

            for (final Object[] testCase : cases) {
                final Class<?> arrayType = (Class<?>) testCase[0];
                final Class<?> componentType = (Class<?>) testCase[1];
                final String source = (String) testCase[2];
                final com.landawn.abacus.type.Type<Object> type = com.landawn.abacus.type.Type.of(componentType);

                for (final Splitter splitter : new Splitter[] { Splitter.with(','), Splitter.with(',').omitEmptyStrings(), Splitter.with(',').trimResults() }) {
                    final List<Object> expected = splitter.split(source, type);
                    final Object actual = splitter.splitToArray(source, arrayType);
                    final Object again = splitter.splitToArray(source, arrayType);

                    assertSame(arrayType, actual.getClass());
                    assertNotSame(actual, again);
                    assertEquals(expected.size(), java.lang.reflect.Array.getLength(actual), source);

                    for (int i = 0; i < expected.size(); i++) {
                        final Object element = java.lang.reflect.Array.get(actual, i);

                        if (element instanceof Double d) {
                            assertEquals(Double.doubleToRawLongBits((Double) expected.get(i)), Double.doubleToRawLongBits(d), source + " @" + i);
                        } else if (element instanceof Float f) {
                            assertEquals(Float.floatToRawIntBits((Float) expected.get(i)), Float.floatToRawIntBits(f), source + " @" + i);
                        } else {
                            assertEquals(expected.get(i), element, source + " @" + i);
                        }
                    }
                }

                assertEquals(0, java.lang.reflect.Array.getLength(Splitter.with(',').splitToArray(null, arrayType)));
                assertEquals(1, java.lang.reflect.Array.getLength(Splitter.with(',').splitToArray("", arrayType)));
            }

            final Random rnd = new Random(1068);
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < 3000; i++) {
                sb.append(i == 0 ? "" : ",").append(rnd.nextInt());
            }

            final int[] ints = Splitter.with(',').splitToArray(sb.toString(), int[].class);
            final String[] tokens = sb.toString().split(",");

            assertEquals(tokens.length, ints.length);

            for (int i = 0; i < tokens.length; i++) {
                assertEquals(Integer.parseInt(tokens[i]), ints[i]);
            }
        }

        // G068-02: conversion failures still surface from the element type's valueOf, with the same type and message.
        @Test
        public void testSplitToArray_primitiveConversionFailuresUnchanged() {
            final Object[][] cases = { { int[].class, int.class, "1,abc,3", "abc" }, { int[].class, int.class, "1,2147483648", "2147483648" },
                    { int[].class, int.class, "1, 2", " 2" }, { long[].class, long.class, "9223372036854775808", "9223372036854775808" },
                    { double[].class, double.class, "1,x", "x" }, { boolean[].class, boolean.class, "true,maybe", "maybe" },
                    { byte[].class, byte.class, "1,128", "128" }, { short[].class, short.class, "-32769", "-32769" },
                    { char[].class, char.class, "a,ab", "ab" } };

            for (final Object[] testCase : cases) {
                final Class<?> arrayType = (Class<?>) testCase[0];
                final com.landawn.abacus.type.Type<Object> type = com.landawn.abacus.type.Type.of((Class<?>) testCase[1]);
                final String badToken = (String) testCase[3];
                RuntimeException expected = null;

                try {
                    type.valueOf(badToken);
                } catch (final RuntimeException e) {
                    expected = e;
                }

                if (expected == null) {
                    // The token converts after all: the array call must succeed too.
                    assertNotNull(Splitter.with(',').splitToArray((String) testCase[2], arrayType));
                    continue;
                }

                final RuntimeException actual = assertThrows(RuntimeException.class, () -> Splitter.with(',').splitToArray((String) testCase[2], arrayType));

                assertSame(expected.getClass(), actual.getClass(), String.valueOf(testCase[2]));
                assertEquals(expected.getMessage(), actual.getMessage(), String.valueOf(testCase[2]));
            }

            assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').splitToArray("1", int.class));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').splitToArray("1", (Class<int[]>) null));
        }
    }
}
