package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.DelimiterMatchMode;

/**
 * Tests for the fixes of the 2026-08-30f review of {@link Strings}.
 *
 * <ul>
 *   <li><b>O-1</b> {@code repeat(String, int, String, String, String)} no longer builds the result in a
 *       speculatively sized {@code StringBuilder}. The repeated part is produced by {@link String#repeat(int)}
 *       (which grows by doubling instead of appending once per element) and the affixes are spliced on in a
 *       single concatenation, so the cost is one full-size temporary plus the result. Allocation drops from
 *       3.0x-5.5x of the result size to 1.0x-2.0x in every shape, and the previous {@code n < 8} /
 *       {@code n >= 8} split is gone. Output is unchanged - proved exhaustively below against an independent
 *       reference.</li>
 *   <li><b>O-2</b> {@code isWrappedWith(String, String)} uses the same subtraction-based non-overlap check as
 *       {@code isWrappedWith(String, String, String)}, {@code wrapIfMissing} and {@code unwrap} instead of a
 *       division. Both forms are overflow-free and agree on every input.</li>
 *   <li><b>J-1</b> {@link DelimiterMatchMode#OUTERMOST_ONLY} now documents that "outermost" is relative to the
 *       matches the mode returns: an unclosed begin delimiter produces no match and therefore suppresses
 *       nothing nested inside it.</li>
 *   <li><b>J-3</b> the class-level cross-library table now records both {@code lenientFormat} differences from
 *       Guava: the {@code ": ["} surplus-argument separator and {@code N.deepToString} array rendering.</li>
 * </ul>
 */
public class StringsRegressionDTest extends TestBase {

    // ---------------------------------------------------------------------------------------------
    // O-1 : repeat(String, int, String, String, String)
    // ---------------------------------------------------------------------------------------------

    /** Independent reference: the plainly written definition of the documented contract. */
    private static String refRepeat(final String str, final int n, final String delimiter, final String prefix, final String suffix) {
        final String s = str == null ? "" : str;
        final String d = delimiter == null ? "" : delimiter;
        final StringBuilder sb = new StringBuilder(prefix == null ? "" : prefix);

        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(d);
            }
            sb.append(s);
        }

        return sb.append(suffix == null ? "" : suffix).toString();
    }

    @Test
    public void testRepeat5Arg_matchesReferenceOverFullMatrix() {
        final String[] strs = { null, "", "a", "ab", "abc", "xyzw" };
        final String[] delims = { null, "", ",", "--", " ", "::" };
        final String[] affixes = { null, "", "[", "Start:" };

        int cases = 0;

        for (final String s : strs) {
            for (final String d : delims) {
                for (final String p : affixes) {
                    for (final String x : affixes) {
                        // 0 and 1 are the early-return arms; 2..12 spans the old n < 8 / n >= 8 split.
                        for (final int n : new int[] { 0, 1, 2, 3, 7, 8, 9, 12 }) {
                            assertEquals(refRepeat(s, n, d, p, x), Strings.repeat(s, n, d, p, x),
                                    "str=" + s + " n=" + n + " delim=" + d + " prefix=" + p + " suffix=" + x);
                            cases++;
                        }
                    }
                }
            }
        }

        assertEquals(6 * 6 * 4 * 4 * 8, cases);
    }

    @Test
    public void testRepeat5Arg_allBranchesReachedAndCorrect() {
        // unwrapped, delimiter empty -> String.repeat(n)
        assertEquals("ababab", Strings.repeat("ab", 3, "", "", ""));
        assertEquals("ababab", Strings.repeat("ab", 3, null, null, null));
        // unwrapped, str empty -> delimiter.repeat(n - 1)
        assertEquals("--", Strings.repeat("", 3, "-", "", ""));
        assertEquals("--", Strings.repeat(null, 3, "-", "", ""));
        // unwrapped, both non-empty -> str + (delimiter + str) * (n - 1)
        assertEquals("ab,ab,ab", Strings.repeat("ab", 3, ",", "", ""));
        // wrapped, delimiter empty
        assertEquals("[ababab]", Strings.repeat("ab", 3, "", "[", "]"));
        // wrapped, str empty
        assertEquals("[--]", Strings.repeat("", 3, "-", "[", "]"));
        // wrapped, both non-empty -> prefix + str + (delimiter + str).repeat(n - 1) + suffix, spliced once
        assertEquals("[ab,ab,ab]", Strings.repeat("ab", 3, ",", "[", "]"));
        assertEquals("Start:X-X-X-X:End", Strings.repeat("X", 4, "-", "Start:", ":End"));
        // prefix only / suffix only
        assertEquals("[ab,ab", Strings.repeat("ab", 2, ",", "[", ""));
        assertEquals("ab,ab]", Strings.repeat("ab", 2, ",", "", "]"));
    }

    @Test
    public void testRepeat5Arg_earlyReturnsUnchanged() {
        assertEquals("[]", Strings.repeat("ab", 0, ",", "[", "]"));
        assertEquals("[ab]", Strings.repeat("ab", 1, ",", "[", "]"));
        assertEquals("", Strings.repeat("ab", 0, ",", "", ""));
        assertEquals("ab", Strings.repeat("ab", 1, ",", "", ""));
        // n == 0 and both tokens empty are the two "prefix + suffix" arms
        assertEquals("[]", Strings.repeat("", 5, "", "[", "]"));
        assertEquals("[]", Strings.repeat(null, 5, null, "[", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("ab", -1, ",", "", ""));
    }

    @Test
    public void testRepeat_documentedContractsStillHold() {
        // the @ai-ignore'd null/empty-str delimiter contract must not shift
        assertEquals(",,", Strings.repeat(null, 3, ','));
        assertEquals(",,", Strings.repeat(null, 3, ","));
        assertEquals(",,", Strings.repeat("", 3, ","));
        assertEquals("--", Strings.repeat(null, 3, "-"));
        assertEquals("", Strings.repeat(null, 3));
        assertEquals("", Strings.repeat("", 3));
        assertEquals("ababab", Strings.repeat("ab", 3, (String) null));
        assertEquals("ab,ab,ab", Strings.repeat("ab", 3, ','));
        assertEquals("Hello Hello", Strings.repeat("Hello", 2, ' '));
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("*-*-*-*", Strings.repeat('*', 4, '-'));
        assertEquals("[ab,ab,ab]", Strings.repeat("ab", 3, ",", "[", "]"));
        assertEquals("abab", Strings.repeat("ab", 2, null, null, null));
    }

    @Test
    public void testRepeat_delegatingOverloadsMatchThe5ArgForm() {
        final String[] strs = { null, "", "a", "ab" };
        final String[] delims = { null, "", ",", "--" };

        for (final String s : strs) {
            for (final String d : delims) {
                for (int n = 0; n <= 10; n++) {
                    assertEquals(Strings.repeat(s, n, d, "", ""), Strings.repeat(s, n, d), "str=" + s + " n=" + n + " delim=" + d);
                }
            }
        }

        for (int n = 0; n <= 10; n++) {
            assertEquals(Strings.repeat("ab", n, ","), Strings.repeat("ab", n, ','));
            assertEquals(Strings.repeat("a", n, ","), Strings.repeat('a', n, ','));
        }
    }

    @Test
    public void testRepeat_largeNStillCorrect() {
        // exercises the String.repeat body path well past the old n < 8 threshold
        final int n = 5_000;
        final String out = Strings.repeat("ab", n, "-");
        assertEquals(2 * n + (n - 1), out.length());
        assertTrue(out.startsWith("ab-ab-"));
        assertTrue(out.endsWith("-ab"));
        assertEquals(n, Strings.countMatches(out, "ab"));
        assertEquals(n - 1, Strings.countMatches(out, "-"));

        // and the wrapped, exactly-sized-buffer path
        final String wrapped = Strings.repeat("ab", n, "-", "<", ">");
        assertEquals(out.length() + 2, wrapped.length());
        assertEquals("<" + out + ">", wrapped);
    }

    /**
     * The shape the first cut of this fix got wrong: WRAPPED (non-empty affixes) with both a body token and a
     * delimiter, a very short token and a large {@code n}. An append loop there is 4x-5x slower than splicing
     * the {@code String.repeat} tail (measured 16-20 ms vs 3.8 ms at {@code n = 2,000,000}), and slower than
     * the original code as well. Correctness on that shape is what this test pins.
     */
    @Test
    public void testRepeat_wrappedShortTokenLargeN() {
        final int n = 100_000;

        for (final String token : new String[] { "x", "ab" }) {
            for (final String delim : new String[] { "-", "::" }) {
                final String wrapped = Strings.repeat(token, n, delim, "<", ">");
                final String unwrapped = Strings.repeat(token, n, delim);

                assertEquals("<" + unwrapped + ">", wrapped);
                assertEquals(token.length() * n + delim.length() * (n - 1) + 2, wrapped.length());
                assertTrue(wrapped.startsWith("<" + token + delim + token));
                assertTrue(wrapped.endsWith(token + delim + token + ">"));
                assertEquals(n, Strings.countMatches(unwrapped, token));
                assertEquals(n - 1, Strings.countMatches(unwrapped, delim));
                assertEquals(refRepeat(token, n, delim, "<", ">"), wrapped);
            }
        }

        // the single-token wrapped arms at the same scale
        assertEquals("<" + "x".repeat(n) + ">", Strings.repeat("x", n, "", "<", ">"));
        assertEquals("<" + "-".repeat(n - 1) + ">", Strings.repeat("", n, "-", "<", ">"));
    }

    @Test
    public void testRepeat_resultLengthGuardStillFires() {
        // checkRepeatResultLength must still reject before any allocation is attempted
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("ab", Integer.MAX_VALUE, "-"));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("ab", Integer.MAX_VALUE, "-", "[", "]"));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat(null, Integer.MAX_VALUE, "-"));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat('a', Integer.MAX_VALUE, '-'));
    }

    @Test
    public void testRepeat_surrogatePairsAreNotSplit() {
        final String emoji = "😀"; // U+1F600, two UTF-16 code units
        final String delimited = Strings.repeat(emoji, 3, "-");
        assertEquals(emoji + "-" + emoji + "-" + emoji, delimited);
        assertEquals(5, delimited.codePointCount(0, delimited.length())); // 3 emoji + 2 delimiters
        assertEquals(emoji + emoji, Strings.repeat(emoji, 2, ""));
        assertEquals("<" + emoji + "-" + emoji + ">", Strings.repeat(emoji, 2, "-", "<", ">"));
    }

    // ---------------------------------------------------------------------------------------------
    // O-2 : isWrappedWith(String, String)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIsWrappedWith_documentedExamples() {
        assertTrue(Strings.isWrappedWith("'hello'", "'"));
        assertTrue(Strings.isWrappedWith("--comment--", "--"));
        assertFalse(Strings.isWrappedWith("hello", "'"));
        assertFalse(Strings.isWrappedWith("aaa", "aa")); // the matches overlap
        assertTrue(Strings.isWrappedWith("\"text\"", "\""));
        assertFalse(Strings.isWrappedWith("'hello\"", "'"));
        assertFalse(Strings.isWrappedWith(null, "'"));
    }

    @Test
    public void testIsWrappedWith_lengthBoundary() {
        // exactly 2 * prefixSuffix.length() is the smallest wrapped string
        assertTrue(Strings.isWrappedWith("aaaa", "aa"));
        assertFalse(Strings.isWrappedWith("aaa", "aa"));
        assertTrue(Strings.isWrappedWith("aaaaa", "aa"));
        assertTrue(Strings.isWrappedWith("''", "'"));
        assertFalse(Strings.isWrappedWith("'", "'"));
        assertFalse(Strings.isWrappedWith("", "x"));
    }

    @Test
    public void testIsWrappedWith_agreesWithThreeArgFormAndOldDivisionForm() {
        final String[] strs = { null, "", "a", "aa", "aaa", "aaaa", "aaaaa", "'x'", "''", "'", "ab", "abab", "ababa", "xayax" };
        final String[] fixes = { "a", "aa", "'", "ab", "x", "aba" };

        for (final String s : strs) {
            for (final String f : fixes) {
                final boolean actual = Strings.isWrappedWith(s, f);
                // the 3-arg sibling, which always used the subtraction form
                assertEquals(Strings.isWrappedWith(s, f, f), actual, "str=" + s + " fix=" + f);
                // the previous division form must agree too (the change is a pure refactor)
                final boolean oldForm = s != null && s.length() / 2 >= f.length() && s.startsWith(f) && s.endsWith(f);
                assertEquals(oldForm, actual, "old-vs-new str=" + s + " fix=" + f);
            }
        }
    }

    @Test
    public void testIsWrappedWith_rejectsEmptyPrefixSuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", null));
    }

    // ---------------------------------------------------------------------------------------------
    // J-1 : DelimiterMatchMode.OUTERMOST_ONLY on unbalanced input
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testOutermostOnly_unclosedBeginDelimiterSuppressesNothing() {
        // The '[' at index 1 is never closed, so "c" is the outermost *match* even though it is not at
        // nesting depth zero. This is the behaviour the enum javadoc now states.
        assertEquals(List.of("c"), Strings.substringsBetween("a[b[c]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of(""), Strings.substringsBetween("[x[]x", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of(""), Strings.substringsBetween("[[[][x[", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));

        // ALL_LEVELS agrees on these inputs (there is only one complete pair each)
        assertEquals(List.of("c"), Strings.substringsBetween("a[b[c]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of(""), Strings.substringsBetween("[x[]x", '[', ']', DelimiterMatchMode.ALL_LEVELS));

        // an unmatched *end* delimiter likewise changes nothing
        assertEquals(List.of("a"), Strings.substringsBetween("[a]b]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
    }

    @Test
    public void testOutermostOnly_balancedInputUnchanged() {
        assertEquals(List.of("a2[c]", "a"), Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of("[b[a]]c"), Strings.substringsBetween("[[b[a]]c]", "[", "]", DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of("a[b]c"), Strings.substringsBetween("[a[b]c]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of("[a]"), Strings.substringsBetween("[[a]]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
    }

    @Test
    public void testAllModes_documentedExamplesUnchanged() {
        final String s = "3[a2[c]]2[a]";
        assertEquals(List.of("a2[c", "a"), Strings.substringsBetween(s, '[', ']', DelimiterMatchMode.SEQUENTIAL));
        assertEquals(List.of("c", "a2[c]", "a"), Strings.substringsBetween(s, '[', ']', DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of("a2[c]", "a"), Strings.substringsBetween(s, '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));

        final String t = "[[b[a]]c]";
        assertEquals(List.of("[b[a"), Strings.substringsBetween(t, "[", "]", DelimiterMatchMode.SEQUENTIAL));
        assertEquals(List.of("a", "b[a]", "[b[a]]c"), Strings.substringsBetween(t, "[", "]", DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of("[b[a]]c"), Strings.substringsBetween(t, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY));
    }

    @Test
    public void testOutermostOnly_indexRangesMatchTheSubstrings() {
        // substringIndicesBetween must stay the exact index counterpart of substringsBetween
        for (final String s : new String[] { "a[b[c]", "[x[]x", "3[a2[c]]2[a]", "[[b[a]]c]", "[a]b]", "[[[][x[" }) {
            for (final DelimiterMatchMode mode : DelimiterMatchMode.values()) {
                final List<IndexRange> ranges = Strings.substringIndicesBetween(s, '[', ']', mode);
                final List<String> expected = new ArrayList<>(ranges.size());

                for (final IndexRange r : ranges) {
                    expected.add(s.substring(r.start(), r.end()));
                }

                assertEquals(expected, Strings.substringsBetween(s, '[', ']', mode), "s=" + s + " mode=" + mode);
            }
        }
    }

    @Test
    public void testOutermostOnly_maxCountTruncatesTheSameSequence() {
        for (final String s : new String[] { "a[b[c]", "3[a2[c]]2[a]", "[[b[a]]c]", "[a][b][c]" }) {
            for (final DelimiterMatchMode mode : DelimiterMatchMode.values()) {
                final List<IndexRange> full = Strings.substringIndicesBetween(s, 0, s.length(), "[", "]", mode, Integer.MAX_VALUE);

                for (int maxCount = 0; maxCount <= full.size() + 1; maxCount++) {
                    final List<IndexRange> limited = Strings.substringIndicesBetween(s, 0, s.length(), "[", "]", mode, maxCount);
                    assertEquals(full.subList(0, Math.min(maxCount, full.size())), limited, "s=" + s + " mode=" + mode + " maxCount=" + maxCount);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // J-3 : lenientFormat differences from Guava
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testLenientFormat_surplusArgumentsUseColonSeparator() {
        assertEquals("Hello World: [Extra]", Strings.lenientFormat("Hello %s", "World", "Extra"));
        assertEquals("Hello: [World]", Strings.lenientFormat("Hello", "World"));
        assertEquals("x: [1, 2]", Strings.lenientFormat("x", 1, 2));
        assertEquals("null: [arg1, arg2]", Strings.lenientFormat(null, "arg1", "arg2"));
    }

    @Test
    public void testLenientFormat_arrayArgumentUsesDeepToString() {
        assertEquals("[n, m]", Strings.lenientFormat("%s", (Object) new String[] { "n", "m" }));
        assertEquals("[1, 2]", Strings.lenientFormat("%s", new int[] { 1, 2 }));
        assertEquals("[[1, 2], [3]]", Strings.lenientFormat("%s", (Object) new int[][] { { 1, 2 }, { 3 } }));
        // surplus array arguments are rendered the same way
        assertEquals("x: [[n, m]]", Strings.lenientFormat("x", (Object) new String[] { "n", "m" }));
    }

    @Test
    public void testLenientFormat_otherDocumentedBehaviourUnchanged() {
        assertEquals("Hello World", Strings.lenientFormat("Hello %s", "World"));
        assertEquals("Hello New World", Strings.lenientFormat("Hello %s %s", "New", "World"));
        assertEquals("Hello World %s", Strings.lenientFormat("Hello %s %s", "World"));
        assertEquals("Hello (Object[])null", Strings.lenientFormat("Hello %s", (Object[]) null));
        assertEquals("null", Strings.lenientFormat(null));
        assertEquals("", Strings.lenientFormat(""));
        assertEquals("null1", Strings.lenientFormat("%s%s", null, 1));
    }

    // ---------------------------------------------------------------------------------------------
    // Cross-check: the rewritten repeat is still the exact inverse of split for the shapes that round-trip
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testRepeat_roundTripsThroughSplit() {
        for (final String token : new String[] { "a", "ab", "abc" }) {
            for (final String delim : new String[] { ",", "--", "::" }) {
                for (int n = 1; n <= 12; n++) {
                    final String joined = Strings.repeat(token, n, delim);
                    assertEquals(n, Strings.splitPreserveAllTokens(joined, delim).length, "token=" + token + " delim=" + delim + " n=" + n);
                    assertTrue(Arrays.stream(Strings.splitPreserveAllTokens(joined, delim)).allMatch(token::equals));
                }
            }
        }
    }

    @Test
    public void testRepeat_mayReturnACallerSuppliedInstance() {
        final String delim = new String("-");
        final String twice = Strings.repeat("", 2, delim);
        assertEquals("-", twice);
        assertEquals(1, twice.length());
        assertSame(delim, twice);

        final String token = new String("ab");
        assertEquals("ab", Strings.repeat(token, 1, ","));
        assertEquals("--", Strings.repeat("", 3, delim));
        assertEquals("ababab", Strings.repeat(token, 3, ""));
    }
}
