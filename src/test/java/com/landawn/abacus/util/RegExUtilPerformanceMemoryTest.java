package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for RegExUtil. */
@Tag("slow-test")
public class RegExUtilPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G058 end ----
    // ---- perf review 2026-10-08 G059 begin ----
    @Nested
    class G059 {

        private final String eAcute = String.valueOf((char) 0xE9);
        private final String cjk = new String(new char[] { (char) 0x4E16, (char) 0x754C });
        private final String smiley = new String(Character.toChars(0x1F600));

        private final String[] regexes = { "\\w+", "\\d", "a*", "", "\\b", "(?=a)", "x", "^", "$", "a|b", "\\s+", "(a)(b)?", "\\R", "\\p{L}+", "[^a-z]",
                "(?<=a)", "\\$", "\\\\", "." };

        private String[] sources() {
            return new String[] { "", "a", "aaa", "baaac", "hello world", "  lead and trail  ", "a1b2c3", "price $5 and C:\\dir\\", "abab",
                    "caf" + eAcute + " " + cjk + " end", smiley + "x" + smiley + "a", "line1\r\nline2\nline3\r", "$$\\\\$1", "xxxx" };
        }

        private List<java.util.function.Function<String, String>> functions() {
            final List<java.util.function.Function<String, String>> result = new ArrayList<>();
            result.add(String::toUpperCase);
            result.add(match -> null);
            result.add(match -> "");
            result.add(match -> "$" + match + "\\");
            result.add(match -> "[" + match + "]");
            result.add(match -> "$1");
            result.add(match -> cjk + match);
            result.add(match -> match + match);
            return result;
        }

        private List<com.landawn.abacus.util.function.IntBiFunction<String>> indexFunctions() {
            final List<com.landawn.abacus.util.function.IntBiFunction<String>> result = new ArrayList<>();
            result.add((start, end) -> "[" + start + "-" + end + "]");
            result.add((start, end) -> null);
            result.add((start, end) -> "");
            result.add((start, end) -> "$" + start + "\\");
            result.add((start, end) -> start == end ? smiley : eAcute);
            return result;
        }

        /**
         * The pre-change implementation of replaceAll(String, Pattern, Function): Matcher.replaceAll with the function
         * result escaped by Matcher.quoteReplacement.
         */
        private String jdkReplaceAll(final String source, final java.util.regex.Pattern pattern, final java.util.function.Function<String, String> replacer) {
            return pattern.matcher(source)
                    .replaceAll(matcher -> java.util.regex.Matcher
                            .quoteReplacement(Strings.nullToEmpty(replacer.apply(source.substring(matcher.start(), matcher.end())))));
        }

        /**
         * The pre-change implementation of replaceAll(String, Pattern, IntBiFunction).
         */
        private String jdkReplaceAll(final String source, final java.util.regex.Pattern pattern,
                final com.landawn.abacus.util.function.IntBiFunction<String> replacer) {
            return pattern.matcher(source)
                    .replaceAll(matcher -> java.util.regex.Matcher.quoteReplacement(Strings.nullToEmpty(replacer.apply(matcher.start(), matcher.end()))));
        }

        private void assertSameAsJdk(final String source, final java.util.regex.Pattern pattern, final java.util.function.Function<String, String> function) {
            final List<String> expectedCalls = new ArrayList<>();
            final String expected = jdkReplaceAll(source, pattern, match -> {
                expectedCalls.add(match);
                return function.apply(match);
            });

            final List<String> actualCalls = new ArrayList<>();
            final String actual = RegExUtil.replaceAll(source, pattern, match -> {
                actualCalls.add(match);
                return function.apply(match);
            });

            final String context = "source=" + source + ", pattern=" + pattern.pattern();
            assertEquals(expected, actual, context);
            assertEquals(expectedCalls, actualCalls, context);

            if (expectedCalls.isEmpty()) {
                assertSame(source, actual, context);
            }
        }

        private void assertSameAsJdk(final String source, final java.util.regex.Pattern pattern,
                final com.landawn.abacus.util.function.IntBiFunction<String> function) {
            final List<String> expectedCalls = new ArrayList<>();
            final String expected = jdkReplaceAll(source, pattern, (start, end) -> {
                expectedCalls.add(start + ":" + end);
                return function.apply(start, end);
            });

            final List<String> actualCalls = new ArrayList<>();
            final String actual = RegExUtil.replaceAll(source, pattern, (start, end) -> {
                actualCalls.add(start + ":" + end);
                return function.apply(start, end);
            });

            final String context = "source=" + source + ", pattern=" + pattern.pattern();
            assertEquals(expected, actual, context);
            assertEquals(expectedCalls, actualCalls, context);

            if (expectedCalls.isEmpty()) {
                assertSame(source, actual, context);
            }
        }

        // G059-01: replaceAll(String, Pattern, Function) appends each result literally instead of quoting and re-parsing it
        @Test
        public void testReplaceAllFunction_sameResultAndCallbacksAsQuotedMatcherReplaceAll() {
            for (final String regex : regexes) {
                final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);

                for (final String source : sources()) {
                    for (final java.util.function.Function<String, String> function : functions()) {
                        assertSameAsJdk(source, pattern, function);
                    }
                }
            }

            assertEquals("Hello World", RegExUtil.replaceAll("hello world", java.util.regex.Pattern.compile("\\b\\w"), (String match) -> match.toUpperCase()));
            assertEquals("X", RegExUtil.replaceAll("", java.util.regex.Pattern.compile("a*"), match -> "X"));
            assertEquals("bXc", RegExUtil.replaceAll("baaac", java.util.regex.Pattern.compile("a+"), match -> "X"));
            assertEquals("bc", RegExUtil.replaceAll("baaac", java.util.regex.Pattern.compile("a+"), match -> ""));
            assertEquals("baaac".replaceAll("a*", "X"), RegExUtil.replaceAll("baaac", java.util.regex.Pattern.compile("a*"), match -> "X"));
            assertEquals("a$1\\b", RegExUtil.replaceAll("a.b", java.util.regex.Pattern.compile("\\."), match -> "$1\\"));
            assertEquals("ab", RegExUtil.replaceAll("a1b2", java.util.regex.Pattern.compile("\\d"), match -> null));
            assertEquals("", RegExUtil.replaceAll(null, java.util.regex.Pattern.compile("a*"), match -> "X"));
        }

        // G059-01: replaceAll(String, Pattern, IntBiFunction) appends each result literally instead of quoting and re-parsing it
        @Test
        public void testReplaceAllIntBiFunction_sameResultAndCallbacksAsQuotedMatcherReplaceAll() {
            for (final String regex : regexes) {
                final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);

                for (final String source : sources()) {
                    for (final com.landawn.abacus.util.function.IntBiFunction<String> function : indexFunctions()) {
                        assertSameAsJdk(source, pattern, function);
                    }
                }
            }

            assertEquals("abc[3-6]def",
                    RegExUtil.replaceAll("abc123def", java.util.regex.Pattern.compile("\\d+"), (start, end) -> "[" + start + "-" + end + "]"));
            assertEquals("<0>", RegExUtil.replaceAll("", java.util.regex.Pattern.compile("a*"), (start, end) -> "<" + start + ">"));
            assertEquals("a$c", RegExUtil.replaceAll("abc", java.util.regex.Pattern.compile("b"), (start, end) -> "$"));
            assertEquals("baaac".replaceAll("a*", "X"), RegExUtil.replaceAll("baaac", java.util.regex.Pattern.compile("a*"), (start, end) -> "X"));
            assertEquals("", RegExUtil.replaceAll(null, java.util.regex.Pattern.compile("a*"), (start, end) -> "X"));
        }

        // G059-01: random sources/patterns/results (fixed seed) agree with the quoted Matcher.replaceAll, incl. the callback sequence
        @Test
        public void testReplaceAll_randomInputsSameAsQuotedMatcherReplaceAll() {
            final Random random = new Random(20261008L);
            final String[] pieces = { "a", "b", "1", " ", "$", "\\", "\n", eAcute, cjk, smiley, "ab", "  " };
            final String[] results = { "", "X", "$", "\\", "$0", "\\1", eAcute, smiley, null };

            for (int i = 0; i < 3000; i++) {
                final StringBuilder sb = new StringBuilder();
                final int length = random.nextInt(12);

                for (int j = 0; j < length; j++) {
                    sb.append(pieces[random.nextInt(pieces.length)]);
                }

                final String source = sb.toString();
                final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regexes[random.nextInt(regexes.length)]);
                final String fixed = results[random.nextInt(results.length)];
                final boolean echo = random.nextBoolean();

                assertSameAsJdk(source, pattern, match -> echo ? match + fixed : fixed);
                assertSameAsJdk(source, pattern, (start, end) -> echo ? start + "" + fixed : fixed);
            }
        }

        // G059-01: a throwing replacer propagates its own exception after the same number of calls, and argument checks are unchanged
        @Test
        public void testReplaceAll_throwingReplacerAndArgumentChecks() {
            final java.util.regex.Pattern digit = java.util.regex.Pattern.compile("\\d");
            final AtomicInteger calls = new AtomicInteger();

            final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> RegExUtil.replaceAll("a1b2c3", digit, match -> {
                if (calls.incrementAndGet() == 2) {
                    throw new IllegalStateException("boom " + match);
                }

                return "x";
            }));
            assertEquals("boom 2", ex.getMessage());
            assertEquals(2, calls.get());

            final List<String> seen = new ArrayList<>();
            final IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1b2c3", digit, (start, end) -> {
                seen.add(start + ":" + end);

                if (seen.size() == 3) {
                    throw new IllegalArgumentException("stop " + start);
                }

                return "$";
            }));
            assertEquals("stop 5", ex2.getMessage());
            assertEquals(Arrays.asList("1:2", "3:4", "5:6"), seen);

            final AtomicInteger notCalled = new AtomicInteger();
            assertEquals("", RegExUtil.replaceAll(null, digit, match -> String.valueOf(notCalled.incrementAndGet())));
            assertEquals("", RegExUtil.replaceAll(null, digit, (start, end) -> String.valueOf(notCalled.incrementAndGet())));
            assertEquals(0, notCalled.get());

            assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1", (java.util.regex.Pattern) null, match -> "x"));
            assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1", (java.util.regex.Pattern) null, (start, end) -> "x"));
            assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1", digit, (java.util.function.Function<String, String>) null));
            assertThrows(IllegalArgumentException.class,
                    () -> RegExUtil.replaceAll("a1", digit, (com.landawn.abacus.util.function.IntBiFunction<String>) null));
            assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll(null, (java.util.regex.Pattern) null, match -> "x"));
            assertThrows(java.util.regex.PatternSyntaxException.class, () -> RegExUtil.replaceAll("a1", "(", match -> "x"));
            assertThrows(java.util.regex.PatternSyntaxException.class, () -> RegExUtil.replaceAll(null, "(", (start, end) -> "x"));
        }

        // G059-01: no match returns the source instance itself (as Matcher.replaceAll did); a match yields a new string
        @Test
        public void testReplaceAll_noMatchReturnsSourceInstance() {
            final String source = new String("no digits here");
            assertSame(source, RegExUtil.replaceAll(source, java.util.regex.Pattern.compile("\\d"), match -> "x"));
            assertSame(source, RegExUtil.replaceAll(source, java.util.regex.Pattern.compile("\\d"), (start, end) -> "x"));
            assertSame(source, RegExUtil.replaceAll(source, "\\d", match -> "x"));
            assertSame(source, RegExUtil.replaceAll(source, "\\d", (start, end) -> "x"));

            final String result = RegExUtil.replaceAll(source, java.util.regex.Pattern.compile("o"), match -> "o");
            assertEquals(source, result);
            assertNotSame(source, result);
        }
    }
}
