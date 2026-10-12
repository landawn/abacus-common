package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.join;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Strings split contracts, including regression cases. */
public class StringsSplitTest extends StringsTestSupport {

    private static void assertMaxRejected(final org.junit.jupiter.api.function.Executable call, final int max) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertEquals("'max' cannot be zero or negative: " + max, e.getMessage());
    }

    private static void assertDelimiterRejected(final org.junit.jupiter.api.function.Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertEquals("'delimiter' cannot be null or empty", e.getMessage());
    }



    /** Validate the delimiter before max, including when the source would otherwise return early. */
    @Test
    public void testDelimiterIsValidatedBeforeMax() {
        for (final String str : new String[] { null, "", "a,b" }) {
            for (final int max : new int[] { 0, -1 }) {
                for (final String delimiter : new String[] { null, "" }) {
                    assertDelimiterRejected(() -> Strings.split(str, delimiter, max));
                    assertDelimiterRejected(() -> Strings.split(str, delimiter, max, true));
                    assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter, max));
                    assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter, max, true));
                }
                // With a valid delimiter, max is still validated before an empty-source return.
                assertMaxRejected(() -> Strings.split(str, ",", max), max);
                assertMaxRejected(() -> Strings.split(str, ",", max, true), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ",", max), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ",", max, true), max);
            }
        }
    }

    @Test
    public void testNullHandlingConsistency() {

        Assertions.assertArrayEquals(new String[0], Strings.split(null, ","));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", 5));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", true));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", 5, true));

        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ','));
        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ","));

        Strings.trimToNullEach(null);
        Strings.trimToEmptyEach(null);
        Strings.stripToNullEach(null);
        Strings.stripToEmptyEach(null);
        Strings.removeWhitespaceEach(null);

    }

    // split

    @Test
    public void testEveryMaxTakingSplitOverloadValidatesMaxBeforeNullOrEmptyStr() {
        for (final String str : new String[] { null, "" }) {
            for (final int max : new int[] { 0, -1 }) {
                assertMaxRejected(() -> Strings.split(str, ',', max), max);
                assertMaxRejected(() -> Strings.split(str, ',', max, true), max);
                assertMaxRejected(() -> Strings.split(str, ",", max), max);
                assertMaxRejected(() -> Strings.split(str, ",", max, true), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ',', max), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ',', max, true), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ",", max), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ",", max, true), max);
                assertMaxRejected(() -> Strings.splitOnWhitespace(str, max), max);
                assertMaxRejected(() -> Strings.splitOnWhitespace(str, max, true), max);
                assertMaxRejected(() -> Strings.splitOnWhitespacePreserveAllTokens(str, max), max);
                assertMaxRejected(() -> Strings.splitOnWhitespacePreserveAllTokens(str, max, true), max);
            }
        }
    }



    // C-017: the simplified post-loop of the multi-char splitWorker keeps its results
    @Test
    public void testC017_multiCharSplitJoinBackInvariant() {
        final Random rnd = new Random(17);
        final String[] delimiters = { "::", "ab", "aa", "xyz" };

        for (int round = 0; round < 20_000; round++) {
            final String delimiter = delimiters[rnd.nextInt(delimiters.length)];
            final StringBuilder sb = new StringBuilder();
            final int n = 1 + rnd.nextInt(12);
            for (int k = 0; k < n; k++) {
                sb.append("ab:xyz".charAt(rnd.nextInt(6)));
            }
            final String str = sb.toString();
            final String[] tokens = Strings.splitPreserveAllTokens(str, delimiter);
            assertEquals(str, String.join(delimiter, tokens), str + " / " + delimiter);
        }

        assertArrayEquals(new String[] { "a", "b", "" }, Strings.splitPreserveAllTokens("a::b::", "::"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::b::", "::"));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", "::"));
    }

    @Test
    public void testSplit() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ','));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a.b.c", '.'));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a..b.c", '.'));
        assertArrayEquals(new String[] { "abc" }, Strings.split("abc", ','));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split(null, ','));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("", ','));

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a::b::c", "::"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a,,b", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,,,b,,,c", ","));
        assertArrayEquals(new String[] { "abc" }, Strings.split("abc", "::"));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split(null, ","));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("", ","));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::b::", "::"));
    }

    @Test
    public void testSplit_delimiterValidatedBeforeMax() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split("a", "", 0)).getMessage().contains("delimiter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split("a", null, 0, true)).getMessage().contains("delimiter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a", "", 0)).getMessage().contains("delimiter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a", null, 0, false)).getMessage().contains("delimiter"));
    }

    @Test
    public void testSplit_Max() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ",", 3));
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a,b,c" }, Strings.split("a,b,c", ",", 1));
        assertArrayEquals(new String[] { "a", "b.c" }, Strings.split("a.b.c", ".", 2));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ",", 5));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a:b:c", ":", 3));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a:b:c", ":", 100));
        assertArrayEquals(new String[] { "a", "b:c:d" }, Strings.split("a:b:c:d", ":", 2));
        assertArrayEquals(new String[] { "a", "b::c" }, Strings.split("a::::b::c", "::", 2));
        assertArrayEquals(new String[] { "a" }, Strings.split("a::::", "::", 2));
        assertArrayEquals(new String[] { "a::b" }, Strings.split("::a::b", "::", 1));
        assertArrayEquals(new String[] { "a,b" }, Strings.split(",,a,b", ",", 1));
        assertArrayEquals(new String[] { "a,b" }, Strings.split(",,a,b", ',', 1));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("::::", "::", 1));
        assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", ":", 1));
        assertArrayEquals(new String[] { "a", "b::c" }, Strings.split("a::b::c", "::", 2));
        assertArrayEquals(new String[] { "a", "b", "c:d:e" }, Strings.split("a:b:c:d:e", ":", 3));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Strings.split("a:b:c:d:e", ":", 5));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Strings.split("a:b:c:d:e", ":", 10));
        assertArrayEquals(new String[0], Strings.split(null, ":", 3));
        assertArrayEquals(new String[0], Strings.split("", ":", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ":", 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ":", -1));
    }

    @Test
    public void testSplit_MaxAndTrim() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split(" a , b,c ", ",", 2, true));
        assertArrayEquals(new String[] { " a ", " b,c " }, Strings.split(" a , b,c ", ",", 2, false));
        assertArrayEquals(new String[] { "a", "b", "c : d" }, Strings.split(" a : b : c : d ", ":", 3, true));
        assertArrayEquals(new String[] { " a ", " b ", " c : d " }, Strings.split(" a : b : c : d ", ":", 3, false));
        assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", ":", 1, true));
        assertArrayEquals(new String[] { "  a:b:c  " }, Strings.split("  a:b:c  ", ":", 1, false));
        assertArrayEquals(new String[] { "a ::b" }, Strings.split(":: a ::b", "::", 1, true));
        assertArrayEquals(new String[] { "a,b" }, Strings.split(",,a,b", ',', 1, true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a::   ::b", "::", 3, true));
        assertArrayEquals(new String[] { "a::b::c" }, Strings.split("a::b::c", "::", 1, true));
    }

    @Test
    public void testSplit_rejectsNullOrEmptyDelimiter() {
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", null, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", "", true));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", "", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", null, 2, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", "", true));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", "", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", null, 2, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.split(null, ""));
    }

    @Test
    public void testSplit_Trim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ',', true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a , b , c ", ',', false));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a . b . c ", '.', true));
        assertEquals(0, Strings.split((String) null, ',', true).length);
        assertEquals(0, Strings.split("", ',', true).length);
        assertEquals(0, Strings.split((String) null, ',', false).length);

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ",", true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a , b , c ", ",", false));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a :: b :: c ", "::", true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a :: b :: c ", "::", false));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::::b", "::", true));
        assertArrayEquals(new String[0], Strings.split(null, "::", true));
        assertArrayEquals(new String[0], Strings.split("", "::", true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a,   ,b", ',', true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a::   ::b", "::", true));
    }

    @Test
    public void testC551_splitResultsUnchangedForMaxOneTwoAndUnlimited() {
        // pinned results (identical before and after dropping the worker re-checks)
        assertArrayEquals(new String[] { "a::b::c" }, Strings.split("a::b::c", "::", 1));
        assertArrayEquals(new String[] { "a", "b::c" }, Strings.split("a::b::c", "::", 2));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a::b::c", "::", Integer.MAX_VALUE));
        assertArrayEquals(new String[] { "a::b" }, Strings.split("::a::b", ':', 1));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ':', 2));
        assertArrayEquals(new String[] { "a:" }, Strings.split("a:", ':', 1));
        assertArrayEquals(new String[] { "", "a::b" }, Strings.splitPreserveAllTokens("::a::b", "::", 2));
        assertArrayEquals(new String[] { "a", "b : c" }, Strings.split(" a : b : c ", ':', 2, true));
        assertArrayEquals(new String[] {}, Strings.split("", "::", 1));
        assertArrayEquals(new String[] {}, Strings.split(null, ':', 2));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", "::", 1));

        // differential: the char worker (reached directly and through a one-char String delimiter) equals Commons Lang,
        // whose splitWorker this engine mirrors, for max in {1, 2, 3, MAX}
        final Random random = new Random(20260925L);
        final char[] alphabet = { 'a', 'b', ':', ':', ' ' };
        final int[] maxes = { 1, 2, 3, Integer.MAX_VALUE };

        for (int iteration = 0; iteration < 20_000; iteration++) {
            final int length = random.nextInt(9);
            final StringBuilder sb = new StringBuilder(length);

            for (int k = 0; k < length; k++) {
                sb.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final String str = sb.toString();

            for (final int max : maxes) {
                final int commonsMax = max == Integer.MAX_VALUE ? -1 : max;
                final String[] expected = nullToEmpty(StringUtils.split(str, ":", commonsMax));
                final String[] expectedPreserve = nullToEmpty(StringUtils.splitPreserveAllTokens(str, ":", commonsMax));

                assertArrayEquals(expected, Strings.split(str, ':', max), str + " / " + max);
                assertArrayEquals(expected, Strings.split(str, ":", max), str + " / " + max);

                if (!str.isEmpty()) {
                    assertArrayEquals(expectedPreserve, Strings.splitPreserveAllTokens(str, ':', max, false), str + " / " + max);
                    assertArrayEquals(expectedPreserve, Strings.splitPreserveAllTokens(str, ":", max), str + " / " + max);
                }
            }
        }
    }

    @Test
    public void testU2008_splitStringDelimiterDocumentedRemainderExample() {
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ":", 2));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ":", 2, true));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ":", 2, false));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ':', 2));
        assertArrayEquals(new String[] { "a", "b::" }, Strings.split("a::b::", "::", 2));
    }

    @Test
    public void testC551_splitValidationOrderDelimiterThenMaxThenStr() {
        // delimiter first, even when max is also invalid and str is null
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split(null, (String) null, 0)).getMessage().contains("delimiter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split("a", "", 0, true)).getMessage().contains("delimiter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens(null, "", -1)).getMessage().contains("delimiter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens(null, (String) null, 0, false)).getMessage()
                .contains("delimiter"));

        // then max, before a null / empty str short-circuit
        for (final String str : new String[] { null, "", "a:b" }) {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split(str, ":", 0)).getMessage().contains("max"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split(str, "::", -1, true)).getMessage().contains("max"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split(str, ':', 0)).getMessage().contains("max"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split(str, ':', Integer.MIN_VALUE, false)).getMessage().contains("max"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens(str, ":", 0)).getMessage().contains("max"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens(str, ':', 0, true)).getMessage().contains("max"));
        }

        // no-limit overloads still reject a null / empty String delimiter
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a b", "", true));
    }

    @Test
    public void testTrimVersusEmptyOmissionOrderAcrossSplitFamilies() {
        // The split family decides empty-token omission on the RAW token, so a whitespace-only token
        // survives (as "") once trimming runs.
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a,   ,b", ',', true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a::   ::b", "::", true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a,   ,b", ',', 5, true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a::   ::b", "::", 5, true));

        // splitToLines decides omission AFTER trimming, so the same whitespace-only element is dropped.
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\n   \nb", true, true));
        // ... and is kept as "" when omitEmptyLines is false.
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitToLines("a\n   \nb", true, false));

        // Without trimming the two families agree: a whitespace-only element is non-empty and is kept.
        assertArrayEquals(new String[] { "a", "   ", "b" }, Strings.split("a,   ,b", ',', false));
        assertArrayEquals(new String[] { "a", "   ", "b" }, Strings.splitToLines("a\n   \nb", false, true));
    }

    // splitPreserveAllTokens

    @Test
    public void testC510_maxOneReturnsTheWholeInputForEveryPreserveOverload() {
        for (final String s : new String[] { "a:b:", ":a", "a", " a b ", "::", "a::b", "\t x\ty " }) {
            assertArrayEquals(new String[] { s }, Strings.splitPreserveAllTokens(s, ":", 1));
            assertArrayEquals(new String[] { s.trim() }, Strings.splitPreserveAllTokens(s, ":", 1, true));
            assertArrayEquals(new String[] { s }, Strings.splitPreserveAllTokens(s, ":", 1, false));
            assertArrayEquals(new String[] { s }, Strings.splitOnWhitespacePreserveAllTokens(s, 1));
            assertArrayEquals(new String[] { s.trim() }, Strings.splitOnWhitespacePreserveAllTokens(s, 1, true));
            assertArrayEquals(new String[] { s }, Strings.splitPreserveAllTokens(s, ':', 1));
        }

        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ":", 1));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ":", 1));
        assertArrayEquals(new String[] { "" }, Strings.splitOnWhitespacePreserveAllTokens("", 1, true));
        assertArrayEquals(new String[] { "a", "b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 2));
    }

    @Test
    public void testSplitPreserveAllTokens() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a,b,c", ','));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ','));
        assertArrayEquals(new String[] { "", "a" }, Strings.splitPreserveAllTokens(",a", ','));
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a..b.c", '.'));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", '.'));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, '.'));
        assertArrayEquals(new String[] { "", "a", "b", "" }, Strings.splitPreserveAllTokens(".a.b.", '.'));
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a,,b,c", ','));

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a , b , c ", ',', true));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ',', true));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a . b . c ", '.', true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.splitPreserveAllTokens(" a . b . c ", '.', false));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a..b", '.', true));
        assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" .. ", '.', true));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens((String) null, ',', true));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ',', true));
        assertArrayEquals(new String[] { " a ", " b " }, Strings.splitPreserveAllTokens(" a , b ", ',', false));

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a,b,c", ","));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a::b::c", "::"));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a::::b", "::"));
        assertArrayEquals(new String[] { "test" }, Strings.splitPreserveAllTokens("test", "::"));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, "::"));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", "::"));
        assertArrayEquals(new String[] { "ab", "cd", "ef" }, Strings.splitPreserveAllTokens("ab:cd:ef", ":"));
        assertArrayEquals(new String[] { "ab", "cd", "ef", "" }, Strings.splitPreserveAllTokens("ab:cd:ef:", ":"));
        assertArrayEquals(new String[] { "", "cd", "ef" }, Strings.splitPreserveAllTokens(":cd:ef", ":"));
        assertArrayEquals(new String[] { "abc", "", "def" }, Strings.splitPreserveAllTokens("abc  def", " "));
        assertArrayEquals(new String[] { "", "", "", "" }, Strings.splitPreserveAllTokens(":::", ":"));
        assertArrayEquals(new String[] { "", "" }, Strings.splitPreserveAllTokens(":", ":"));
        assertArrayEquals(new String[] { "", "a", "", "b", "" }, Strings.splitPreserveAllTokens(":a::b:", ":"));
        assertArrayEquals(new String[] { "a", "", "", "b" }, Strings.splitPreserveAllTokens("a::::::b", "::"));

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a : b : c ", ":", true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.splitPreserveAllTokens(" a : b : c ", ":", false));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a::b", ":", true));
        assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" :: ", ":", true));
        assertEquals(0, Strings.splitPreserveAllTokens((String) null, ",", true).length);
        assertEquals(1, Strings.splitPreserveAllTokens("", ",", true).length);
        assertEquals("", Strings.splitPreserveAllTokens("", ",", true)[0]);
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens(" a , , b ", ",", true));
    }

    @Test
    public void testSplitPreserveAllTokens_Max() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.splitPreserveAllTokens("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a", ",c" }, Strings.splitPreserveAllTokens("a,,c", ",", 2));
        assertArrayEquals(new String[] { "a", "b.c" }, Strings.splitPreserveAllTokens("a ..b.c ", "..", 3, true));
        assertArrayEquals(new String[] { "a", "b", "c : d" }, Strings.splitPreserveAllTokens(" a : b : c : d ", ":", 3, true));
        assertArrayEquals(new String[] { " a ", " b ", " c : d " }, Strings.splitPreserveAllTokens(" a : b : c : d ", ":", 3, false));
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a::b:c", ":", 4, true));
        assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" : : ", ":", 3, true));
        assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 1, true));
        assertArrayEquals(new String[] { "  a:b:c  " }, Strings.splitPreserveAllTokens("  a:b:c  ", ":", 1, false));
        assertArrayEquals(new String[] { "a,b" }, Strings.splitPreserveAllTokens("  a,b  ", ",", 1, true));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens((String) null, ":", 5));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ":", 5));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 5));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a::c", ":", 5));
        assertEquals(0, Strings.splitPreserveAllTokens((String) null, ",", 3, true).length);
        assertEquals(1, Strings.splitPreserveAllTokens("", ",", 3, true).length);
        assertEquals(3, Strings.splitPreserveAllTokens(" a , , b , c ", ",", 3, true).length);
        assertArrayEquals(new String[] { "a", "b", "c:d" }, Strings.splitPreserveAllTokens("a:b:c:d", ":", 3));
        assertArrayEquals(new String[] { "a", "b", "::d" }, Strings.splitPreserveAllTokens("a:b:::d", ":", 3));
        assertArrayEquals(new String[] { "", "a", "b", "c" }, Strings.splitPreserveAllTokens(":a:b:c", ":", 4));
        assertArrayEquals(new String[] { "a", "b", "c", "" }, Strings.splitPreserveAllTokens("a:b:c:", ":", 4));
        assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 1));
        assertArrayEquals(new String[] { ",,a,b" }, Strings.splitPreserveAllTokens(",,a,b", ',', 1));
        assertArrayEquals(new String[] { ",,a,b" }, Strings.splitPreserveAllTokens(",,a,b", ',', 1, true));
        assertArrayEquals(new String[] { "::a::b" }, Strings.splitPreserveAllTokens("::a::b", "::", 1));
        assertArrayEquals(new String[] { "a", "::b" }, Strings.splitPreserveAllTokens("a::::b", "::", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("test", ":", 0));
    }

    @Test
    public void testSplitPreserveAllTokens_MultiCharDelimiter_MaxNoSpuriousTrailingEmpty() {
        assertArrayEquals(new String[] { "a", "b..c" }, Strings.splitPreserveAllTokens("a..b..c", "..", 2));
        assertArrayEquals(new String[] { "a", "b::c::d" }, Strings.splitPreserveAllTokens("a::b::c::d", "::", 2));
        assertArrayEquals(new String[] { "", "b..c" }, Strings.splitPreserveAllTokens("..b..c", "..", 2));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitPreserveAllTokens("a..b", "..", 2));
        assertArrayEquals(new String[] { "a", "b", "c..d" }, Strings.splitPreserveAllTokens("a..b..c..d", "..", 3));
        assertArrayEquals(new String[] { "a", "b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 2));
        assertArrayEquals(new String[] { "a", "b..c" }, Strings.splitPreserveAllTokens(" a .. b..c ", "..", 2, true));
    }

    @Test
    public void testSplitPreserveAllTokens_MultiCharDelimiter_TrailingEmptyToken() {
        assertArrayEquals(new String[] { "a", "b", "" }, Strings.splitPreserveAllTokens("a:b:", ':'));
        assertArrayEquals(new String[] { "a", "" }, Strings.splitPreserveAllTokens("a:", ':'));
        assertArrayEquals(new String[] { "a", "b", "" }, Strings.splitPreserveAllTokens("a::b::", "::"));
        assertArrayEquals(new String[] { "a", "" }, Strings.splitPreserveAllTokens("a::", "::"));
        assertArrayEquals(new String[] { "a", "b", "", "" }, Strings.splitPreserveAllTokens("a::b::::", "::"));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a::b::c", "::"));
        assertArrayEquals(new String[] { "", "a", "b" }, Strings.splitPreserveAllTokens("::a::b", "::"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::b::", "::"));
    }



    // C-018: the char/max overloads of splitPreserveAllTokens behave exactly like the one-char String overloads
    @Test
    public void testC018_splitPreserveAllTokensCharMaxMatchesStringForm() {
        final Random rnd = new Random(18);

        for (int round = 0; round < 20_000; round++) {
            final StringBuilder sb = new StringBuilder();
            final int n = rnd.nextInt(10);
            for (int k = 0; k < n; k++) {
                sb.append(" a:b".charAt(rnd.nextInt(4)));
            }
            final String str = rnd.nextInt(20) == 0 ? null : sb.toString();
            final int max = 1 + rnd.nextInt(5);
            final boolean trim = rnd.nextBoolean();
            assertArrayEquals(Strings.splitPreserveAllTokens(str, ":", max, trim), Strings.splitPreserveAllTokens(str, ':', max, trim));
            assertArrayEquals(Strings.splitPreserveAllTokens(str, ":", max), Strings.splitPreserveAllTokens(str, ':', max));
        }

        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a", ':', 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens(null, ':', -1, true));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ':', 2));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ':', 2));
        assertArrayEquals(new String[] { "a: b" }, Strings.splitPreserveAllTokens(" a: b ", ':', 1, true));
    }

    @Test
    public void testSplitPreserveAllTokensValidatesDelimiterBeforeNullOrEmptyStr() {
        for (final String str : new String[] { null, "" }) {
            for (final String delimiter : new String[] { null, "" }) {
                assertDelimiterRejected(() -> Strings.split(str, delimiter));
                assertDelimiterRejected(() -> Strings.split(str, delimiter, true));
                assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter));
                assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter, true));
                // String-delimiter overloads validate the delimiter before max, in signature order.
                assertDelimiterRejected(() -> Strings.split(str, delimiter, 3));
                assertDelimiterRejected(() -> Strings.split(str, delimiter, 3, true));
                assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter, 3));
                assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter, 3, true));
            }
        }
    }

    // splitToLines



    // C-019 / C-020: the documented trailing-terminator and trailing-delimiter rules
    @Test
    public void testC019_C020_documentedTrailingRules() {
        assertArrayEquals(new String[] { "a", "" }, Strings.splitToLines("a\n"));
        assertArrayEquals(new String[] { "a", "" }, Strings.splitToLines("a\n", false, false));
        assertArrayEquals(new String[] { "a" }, Strings.splitToLines("a\n", false, true));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ':', 2));
        assertArrayEquals(new String[] { "a", "b::" }, Strings.split("a::b::", "::", 2));
    }

    @Test
    public void testSplitToLines() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines("a\nb\nc"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\r\nb"));
        assertArrayEquals(new String[] { "" }, Strings.splitToLines(""));
        assertArrayEquals(new String[] {}, Strings.splitToLines(null));

        String[] substrs = Strings.splitToLines("aa\naa\r\n");
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);
        substrs = Strings.splitToLines("aa\r\naa\n");
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);
    }

    @Test
    public void testSplitToLines_WithOptions() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines("a\n b \n\n c \r\n", true, true));
        assertArrayEquals(new String[] { "a", "b", "", "c", "" }, Strings.splitToLines("a\nb\n\nc\n", false, false));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines(" a \n b \n c ", true, false));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\n\nb", false, true));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines(" a \n\n b ", true, true));
        assertArrayEquals(new String[] { "a", "b", "\u2003" }, Strings.splitToLines("\ta\t\n\u0001b\u001F\n\u2003", true, false));
        assertArrayEquals(new String[] { "a" }, Strings.splitToLines("\t\n a \n\u0001\u001F", true, true));

        String[] substrs = Strings.splitToLines("aa\r\n  aa\n", true, false);
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);
        substrs = Strings.splitToLines("aa\r\n  aa\n", true, true);
        assertEquals(2, substrs.length);
        assertEquals(substrs[0], substrs[1]);
        substrs = Strings.splitToLines("aa\r\n  aa\n", false, true);
        assertEquals(2, substrs.length);
        assertNotEquals(substrs[0], substrs[1]);
        assertEquals(0, Strings.splitToLines("", false, true).length);
        assertEquals(0, Strings.splitToLines(null, false, true).length);
    }

    @Test
    public void testC554_splitToLinesRecognisesAllEightTerminators() {
        final String[] terminators = { "\n", "\r", "\r\n", "\u000B", "\u000C", "\u0085", "\u2028", "\u2029" };

        for (final String terminator : terminators) {
            assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a" + terminator + "b"), escape(terminator));
            assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitToLines("a" + terminator + terminator + "b"), escape(terminator));
            assertArrayEquals(new String[] { "", "" }, Strings.splitToLines(terminator), escape(terminator));
            assertArrayEquals(new String[] { "a", "" }, Strings.splitToLines("a" + terminator), escape(terminator));
            assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines(" a " + terminator + terminator + " b ", true, true), escape(terminator));
            assertArrayEquals(new String[] { " a ", "", " b " }, Strings.splitToLines(" a " + terminator + terminator + " b ", false, false),
                    escape(terminator));
            assertArrayEquals(new String[] {}, Strings.splitToLines(terminator, false, true), escape(terminator));
        }

        // CR LF is one terminator; LF CR is two
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\r\nb"));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitToLines("a\n\rb"));
        // mixed terminators
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, Strings.splitToLines("a\u0085b\u2028c\u000Cd"));
    }



    /** A valid max still short-circuits on a null/empty input exactly as documented. */
    @Test
    public void testValidMaxStillShortCircuitsOnNullOrEmptyStr() {
        assertArrayEquals(new String[0], Strings.split(null, ',', 3));
        assertArrayEquals(new String[0], Strings.split("", ',', 3));
        assertArrayEquals(new String[0], Strings.splitOnWhitespace(null, 3));
        assertArrayEquals(new String[0], Strings.splitOnWhitespace("", 3, true));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ',', 3));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ',', 3));
        assertArrayEquals(new String[0], Strings.splitOnWhitespacePreserveAllTokens(null, 3));
        assertArrayEquals(new String[] { "" }, Strings.splitOnWhitespacePreserveAllTokens("", 3, true));
    }

}
