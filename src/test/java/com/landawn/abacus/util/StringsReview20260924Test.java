package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.DelimiterMatchMode;
import com.landawn.abacus.util.Strings.StrUtil;

/**
 * Regression tests for the 2026-09-24 twelve-class review (ledger
 * {@code scripts/cross_review/Strings_IOUtil_CommonUtil_N_Array_Iterables_Iterators_Maps_Beans_Files_Multiset_Multimap_ledger_2026-09-24.md}).
 */
public class StringsReview20260924Test extends TestBase {

    private static final String EMOJI = "😀";

    // C-004: the supplier-returned-null message names the supplier, not a non-existent 'defaultValue' parameter
    @Test
    public void testC004_defaultIfSupplierMessagesNameTheSupplier() {
        final Supplier<String> nullSupplier = () -> null;

        final NullPointerException npe = assertThrows(NullPointerException.class, () -> Strings.defaultIfNull((String) null, nullSupplier));
        assertEquals("defaultValueSupplier returned null", npe.getMessage());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("", (Supplier<String>) () -> ""));
        assertTrue(e.getMessage().contains("defaultValueSupplier"), e.getMessage());
        assertFalse(e.getMessage().contains("'defaultValue'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank(" ", (Supplier<String>) () -> "  "));
        assertTrue(e.getMessage().contains("defaultValueSupplier"), e.getMessage());

        // the supplier is not consulted for a present value, and a good supplier still works
        assertEquals("x", Strings.defaultIfNull("x", nullSupplier));
        assertEquals("d", Strings.defaultIfBlank("\t", (Supplier<String>) () -> "d"));
    }

    // C-005 / C-008: the documented abbreviate offset and surrogate examples
    @Test
    public void testC005_C008_abbreviateDocumentedExamples() {
        assertEquals("abc...", Strings.abbreviate("abcdefghijklmno", 4, 6));
        assertEquals("abc...", Strings.abbreviate("abcdefghijklmno", "...", 4, 6));
        assertEquals("abcdefghijk...", Strings.abbreviate("abcdefghijklmno", 11, 14));
        // an effective offset greater than marker length + 1 keeps the offset character
        assertTrue(Strings.abbreviate("abcdefghijklmno", 6, 10).contains("g"));
        // the only unit that fits is the first half of a pair: marker alone
        assertEquals("...", Strings.abbreviate(EMOJI + "abc", 4));
        assertEquals("...", Strings.abbreviate(EMOJI + EMOJI + "a", 4));
    }

    // C-007: the rewritten varargs examples really exercise the varargs overloads
    @Test
    public void testC007_varargsIsAllIsAnyExamples() {
        assertTrue(Strings.isAllEmpty(null, "", null, ""));
        assertFalse(Strings.isAllEmpty("", "", "", "foo"));
        assertFalse(Strings.isAllEmpty(" ", "", null, ""));
        assertTrue(Strings.isAllEmpty((String[]) null));
        assertTrue(Strings.isAllEmpty(new String[] {}));
        assertTrue(Strings.isAllEmpty(new String[] { "", null }));

        assertTrue(Strings.isAllBlank(null, "", " ", "  "));
        assertFalse(Strings.isAllBlank(" ", "", null, "bar"));
        assertTrue(Strings.isAllBlank((String[]) null));
        assertTrue(Strings.isAllBlank(new String[] {}));
        assertTrue(Strings.isAllBlank(new String[] { " ", null }));

        assertTrue(Strings.isAnyEmpty("a", "b", "c", ""));
        assertTrue(Strings.isAnyEmpty("a", null, "c", "d"));
        assertFalse(Strings.isAnyEmpty("a", " ", "c", "d"));
        assertFalse(Strings.isAnyEmpty("a", "b", "c", "d"));
        assertTrue(Strings.isAnyEmpty((String) null));
        assertFalse(Strings.isAnyEmpty((String[]) null));
        assertFalse(Strings.isAnyEmpty(new String[] {}));
        assertTrue(Strings.isAnyEmpty(new String[] { "" }));

        assertTrue(Strings.isAnyBlank("a", "b", "c", " "));
        assertTrue(Strings.isAnyBlank("a", null, "c", "d"));
        assertFalse(Strings.isAnyBlank("a", "b", "c", "d"));
        assertTrue(Strings.isAnyBlank((String) null));
        assertFalse(Strings.isAnyBlank((String[]) null));
        assertFalse(Strings.isAnyBlank(new String[] {}));
        assertTrue(Strings.isAnyBlank(new String[] { "" }));
    }

    // C-009: documented host rules of isValidUrl / isValidHttpUrl
    @Test
    public void testC009_isValidUrlHostRules() {
        assertFalse(Strings.isValidUrl("https://例え.jp/"));
        assertTrue(Strings.isValidUrl("https://xn--r8jz45g.jp/"));
        assertTrue(Strings.isValidUrl("http://example.com/é"));
        assertTrue(Strings.isValidUrl("http://example.com:0/"));
        assertTrue(Strings.isValidHttpUrl("http://xn--r8jz45g.jp/"));
        assertFalse(Strings.isValidHttpUrl("http://例え.jp/"));
    }

    // C-011: abbreviateMiddle hands the budget lost to surrogate rounding to the other side
    @Test
    public void testC011_abbreviateMiddleUsesTheWholeBudgetAroundSurrogates() {
        assertEquals("a." + EMOJI, Strings.abbreviateMiddle("a" + EMOJI + EMOJI, ".", 4));
        assertEquals("a.cd", Strings.abbreviateMiddle("a" + EMOJI + "bcd", ".", 4));
        assertEquals("ab.", Strings.abbreviateMiddle("abcd" + EMOJI, ".", 3));
        // BMP input unchanged (same as Commons Lang)
        assertEquals("ab.f", Strings.abbreviateMiddle("abcdef", ".", 4));
        assertEquals("abc", Strings.abbreviateMiddle("abc", ".", 5));
        assertNull(Strings.abbreviateMiddle(null, ".", 5));
        assertEquals("", Strings.abbreviateMiddle("", ".", 5));

        final Random rnd = new Random(20260924);
        final String[] alphabet = { "a", "b", "c", EMOJI, "𐐀" };

        for (int round = 0; round < 20_000; round++) {
            final StringBuilder sb = new StringBuilder();
            final int n = 2 + rnd.nextInt(8);
            for (int k = 0; k < n; k++) {
                sb.append(alphabet[rnd.nextInt(alphabet.length)]);
            }
            final String str = sb.toString();
            final String middle = rnd.nextBoolean() ? "." : "..";
            final int maxLength = rnd.nextInt(str.length() + 2);
            final String result = Strings.abbreviateMiddle(str, middle, maxLength);

            if (maxLength >= str.length() || maxLength < middle.length() + 2) {
                assertEquals(str, result);
                continue;
            }

            assertTrue(result.length() <= maxLength, str + " / " + maxLength + " -> " + result);
            // never splits a pair
            for (int k = 0; k < result.length(); k++) {
                final char ch = result.charAt(k);
                if (Character.isHighSurrogate(ch)) {
                    assertTrue(k + 1 < result.length() && Character.isLowSurrogate(result.charAt(k + 1)), result);
                } else if (Character.isLowSurrogate(ch)) {
                    assertTrue(k > 0 && Character.isHighSurrogate(result.charAt(k - 1)), result);
                }
            }
            // prefix + middle + suffix of the input, wasting at most one unit per side
            final int m = result.indexOf(middle);
            assertTrue(m >= 0, result);
            assertTrue(str.startsWith(result.substring(0, m)), result);
            assertTrue(str.endsWith(result.substring(m + middle.length())), result);
            assertTrue(result.length() >= maxLength - 1, str + " / " + maxLength + " -> " + result);
        }
    }

    // C-012: a mapper returning null is rejected instead of writing the text "null"
    @Test
    public void testC012_mapWordsRejectsNullMapperResult() {
        assertThrows(NullPointerException.class, () -> Strings.mapWords("a b", w -> null));
        assertThrows(NullPointerException.class, () -> Strings.mapWords("a-b", "-", w -> null));
        assertThrows(NullPointerException.class, () -> Strings.mapWords("a-b", "-", Arrays.asList("a"), w -> null));
        // excluded words are never passed to the mapper
        assertEquals("a-B", Strings.mapWords("a-b", "-", Arrays.asList("a"), w -> "a".equals(w) ? null : w.toUpperCase()));
        // null / empty input: nothing is mapped
        assertNull(Strings.mapWords(null, w -> null));
        assertEquals("", Strings.mapWords("", w -> null));
        assertEquals("", Strings.mapWords("", "-", w -> null));
        assertEquals("HELLO\tWÖRLD", Strings.mapWords("hello\twörld", String::toUpperCase));
    }

    // C-014: swapCase applies the Unicode Final_Sigma rule, toLowerCase follows the JDK approximation
    @Test
    public void testC014_swapCaseFinalSigmaDiffersFromToLowerCase() {
        assertEquals("ος-α", Strings.swapCase("ΟΣ-Α"));
        assertEquals("οσ-α", Strings.toLowerCase("ΟΣ-Α"));
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

    // C-019 / C-020: the documented trailing-terminator and trailing-delimiter rules
    @Test
    public void testC019_C020_documentedTrailingRules() {
        assertArrayEquals(new String[] { "a", "" }, Strings.splitToLines("a\n"));
        assertArrayEquals(new String[] { "a", "" }, Strings.splitToLines("a\n", false, false));
        assertArrayEquals(new String[] { "a" }, Strings.splitToLines("a\n", false, true));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ':', 2));
        assertArrayEquals(new String[] { "a", "b::" }, Strings.split("a::b::", "::", 2));
    }

    // C-021: documented surrogate / overlap rules that had no test
    @Test
    public void testC021_documentedSurrogateAndOverlapRules() {
        assertEquals("", Strings.truncate(EMOJI + "x", 1));
        assertEquals("x", Strings.truncate(EMOJI + "x", 1, 2));
        assertEquals("", Strings.truncate(EMOJI + "x", 0, 1));
        assertEquals(EMOJI, Strings.truncate(EMOJI + EMOJI, 1, 2));
        assertEquals("a", Strings.chop("a" + EMOJI));
        assertEquals("", Strings.chop(EMOJI));

        assertEquals("aaaaa", Strings.wrapIfMissing("a", "aa", "aa"));
        assertEquals("aaaa", Strings.wrapIfMissing("aa", "aa", "aa"));
        assertEquals("aaaaa", Strings.wrapIfMissing("aaa", "aa", "aa"));
        assertEquals("aaaa", Strings.wrapIfMissing("aaaa", "aa", "aa"));
        assertEquals("a", Strings.unwrap("aaaaa", "aa", "aa"));
        assertEquals("aa", Strings.unwrap("aa", "aa", "aa"));
        assertEquals("aaa", Strings.unwrap("aaa", "aa", "aa"));
        assertEquals("", Strings.unwrap("aaaa", "aa", "aa"));

        assertEquals(Strings.indexOf("abcabc", 'b', 0), Strings.indexOf("abcabc", 'b', -5));
        assertEquals(1, Strings.indexOf("abcabc", 'b', Integer.MIN_VALUE));
        assertEquals(-1, Strings.indexOf("abcabc", 'b', 6));
    }

    // C-025 / C-028: indexOfDifference results (the removed tail was unreachable)
    @Test
    public void testC025_indexOfDifference() {
        assertEquals(7, Strings.indexOfDifference("i am a machine", "i am a robot"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(2, Strings.indexOfDifference("ab", "abxyz"));
        assertEquals(2, Strings.indexOfDifference("abxyz", "ab"));
        assertEquals(0, Strings.indexOfDifference(null, "abc"));
        assertEquals(-1, Strings.indexOfDifference(null, ""));
        // a code-unit index that can fall inside a surrogate pair, unlike lengthOfCommonPrefix
        assertEquals(1, Strings.indexOfDifference("😀", "😁"));
        assertEquals(0, Strings.lengthOfCommonPrefix("😀", "😁"));
    }

    // C-030: joinEntries sizing change keeps the output
    @Test
    public void testC030_joinEntriesIterable() {
        final List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        entries.add(new java.util.AbstractMap.SimpleEntry<>("a", 1));
        entries.add(new java.util.AbstractMap.SimpleEntry<>("b", 2));
        final Iterable<Map.Entry<String, Integer>> iterable = entries::iterator;

        assertEquals("[a=1, b=2]", Strings.joinEntries(iterable, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[a=1, b=2]", Strings.joinEntries(entries, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[]", Strings.joinEntries(Collections.<Map.Entry<String, Integer>> emptyList(), ", ", "=", "[", "]", false, Map.Entry::getKey,
                Map.Entry::getValue));
    }

    // C-036: the three StrUtil.substringBetweenFirstAndLast wrappers agree with the Strings originals
    @Test
    public void testC036_strUtilSubstringBetweenFirstAndLast() {
        final String[] inputs = { null, "", "hello", "[a][b]", "x[a]y[b]z", "##", "#a#b#", "😀#x#😀" };
        final String[] delimiters = { "", "#", "[", "]", "ab" };

        for (final String str : inputs) {
            for (final String d : delimiters) {
                assertEquals(u.Optional.ofNullable(Strings.substringBetweenFirstAndLast(str, d)), StrUtil.substringBetweenFirstAndLast(str, d));

                for (final String e : delimiters) {
                    assertEquals(u.Optional.ofNullable(Strings.substringBetweenFirstAndLast(str, d, e)), StrUtil.substringBetweenFirstAndLast(str, d, e));

                    for (int from = -1; from <= 6; from++) {
                        assertEquals(u.Optional.ofNullable(Strings.substringBetweenFirstAndLast(str, from, d, e)),
                                StrUtil.substringBetweenFirstAndLast(str, from, d, e));
                    }
                }
            }
        }

        assertEquals(u.Optional.of("hello"), StrUtil.substringBetweenFirstAndLast("hello", ""));
        assertFalse(StrUtil.substringBetweenFirstAndLast(null, "#").isPresent());
    }

    // C-431: the nested-mode engine no longer rescans the tail after every end delimiter; results are unchanged
    @Test
    public void testC431_nestedSubstringIndicesBetweenMatchesPreviousEngine() {
        final Random rnd = new Random(431);
        final String[][] delimiterPairs = { { "[", "]" }, { "ab", "bc" }, { "[[", "]]" }, { "a", "a" }, { "(", ")" }, { "aa", "a" } };
        final String alphabet = "[]()abcx";
        final DelimiterMatchMode[] modes = { DelimiterMatchMode.ALL_LEVELS, DelimiterMatchMode.OUTERMOST_ONLY, DelimiterMatchMode.SEQUENTIAL };

        for (int round = 0; round < 60_000; round++) {
            final StringBuilder sb = new StringBuilder();
            final int n = rnd.nextInt(24);
            for (int k = 0; k < n; k++) {
                sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            }
            final String str = sb.toString();
            final String[] pair = delimiterPairs[rnd.nextInt(delimiterPairs.length)];
            final int from = str.isEmpty() ? 0 : rnd.nextInt(str.length() + 1);
            final int to = from + rnd.nextInt(str.length() - from + 1);
            final DelimiterMatchMode mode = modes[rnd.nextInt(modes.length)];
            final int maxCount = rnd.nextBoolean() ? Integer.MAX_VALUE : rnd.nextInt(4);

            assertEquals(previousEngine(str, from, to, pair[0], pair[1], mode, maxCount),
                    Strings.substringIndicesBetween(str, from, to, pair[0], pair[1], mode, maxCount),
                    () -> str + " [" + from + "," + to + ") " + Arrays.toString(pair) + " " + mode + " " + maxCount);
        }
    }

    @Test
    public void testC431_deepNestBeforeLongTailIsLinear() {
        final int depth = 3000;
        final String str = "{".repeat(depth) + "}".repeat(depth) + "x".repeat(2_000_000) + "{z}";

        final List<IndexRange> all = Strings.substringIndicesBetween(str, 0, str.length(), "{", "}", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE);
        assertEquals(depth + 1, all.size());
        assertEquals(IndexRange.of(depth, depth), all.get(0));

        final List<IndexRange> outer = Strings.substringIndicesBetween(str, 0, str.length(), "{", "}", DelimiterMatchMode.OUTERMOST_ONLY, Integer.MAX_VALUE);
        assertEquals(2, outer.size());
        assertEquals(IndexRange.of(1, 2 * depth - 1), outer.get(0));
        assertEquals("z", str.substring(outer.get(1).start(), outer.get(1).end()));
    }

    // C-023 / C-033: a surrogate in the haystack no longer disables the folded search, and results are unchanged
    @Test
    public void testC023_ignoreCaseSearchWithSurrogatesInHaystack() {
        final String hay = EMOJI + "x".repeat(300) + "ABCDEFGHIJKLMNOPQRST";
        final String needle = "abcdefghijklmnopqrst";
        assertEquals(302, Strings.indexOfIgnoreCase(hay, needle));
        assertEquals(302, Strings.lastIndexOfIgnoreCase(hay, needle));
        assertEquals(1, Strings.countMatchesIgnoreCase(hay, needle));
        assertArrayEquals(new int[] { 302 }, Strings.indicesOfIgnoreCase(hay, needle).toArray());
        assertEquals(EMOJI + "x".repeat(300) + "Z", Strings.replaceAllIgnoreCase(hay, needle, "Z"));

        // lone surrogate + Kelvin sign (folds to k)
        assertEquals(301, Strings.indexOfIgnoreCase("\uD801" + "y".repeat(300) + "\u212AELVIN_SIGN_TEST_x", "kelvin_sign_test_x"));
        // Deseret needle (stays on the regionMatches path): capital U+10400 matches small U+10428
        assertEquals(300, Strings.indexOfIgnoreCase("x".repeat(300) + "\uD801\uDC00abcdefghijklmnop", "\uD801\uDC28abcdefghijklmnop"));
        // StrUtil token search through the same engine
        final String tokens = EMOJI + ",ABCDEFGHIJKLMNOPQ," + "x".repeat(300);
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase(tokens, "abcdefghijklmnopq", ","));
        assertEquals(3, StrUtil.lastIndexOfTokenIgnoreCase(tokens, "abcdefghijklmnopq", ","));
    }

    @Test
    public void testC023_randomizedIgnoreCaseAgainstRegionMatches() {
        final Random rnd = new Random(23);
        final String[] pieces = { "a", "B", "s", "S", "\u017F", "K", "\u212A", "i", "I", "\u0130", "\u0131", "\u00DF", "\u03C3", "\u03C2", "\u03A3",
                EMOJI, "\uD801\uDC00", "\uD801\uDC28", "\uD800", "\uDC00" };

        for (int round = 0; round < 3000; round++) {
            final StringBuilder sb = new StringBuilder();
            final int n = 250 + rnd.nextInt(100);
            for (int k = 0; k < n; k++) {
                sb.append(pieces[rnd.nextInt(pieces.length)]);
            }
            final String hay = sb.toString();
            final int from = rnd.nextInt(hay.length() - 20);
            final String raw = hay.substring(from, from + 16 + rnd.nextInt(4));
            final String needle = rnd.nextBoolean() ? raw.toUpperCase(java.util.Locale.ROOT) : raw;

            int expected = -1;
            for (int i = 0; i + needle.length() <= hay.length(); i++) {
                if (hay.regionMatches(true, i, needle, 0, needle.length())) {
                    expected = i;
                    break;
                }
            }
            assertEquals(expected, Strings.indexOfIgnoreCase(hay, needle), () -> hay + " / " + needle);
        }
    }

    // C-010 (documented, unchanged): a digit does not start a new word
    @Test
    public void testC010_digitDoesNotStartAWord() {
        assertEquals("base64url", Strings.toSnakeCase("base64URL"));
        assertEquals("base_url", Strings.toSnakeCase("baseURL"));
        assertEquals("vector3d", Strings.toSnakeCase("vector3D"));
        assertEquals("aB2c", Strings.toCamelCase("aB2C"));
    }

    private static <T> T onSmallStack(final java.util.concurrent.Callable<T> task) throws Exception {
        final Object[] result = new Object[1];
        final Throwable[] failure = new Throwable[1];
        final Thread thread = new Thread(null, () -> {
            try {
                result[0] = task.call();
            } catch (final Throwable e) {
                failure[0] = e;
            }
        }, "small-stack", 256 * 1024);
        thread.start();
        thread.join();
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
        @SuppressWarnings("unchecked")
        final T t = (T) result[0];
        return t;
    }

    // C-001: long dotted / escaped inputs no longer overflow the stack
    @Test
    public void testC001_emailValidationAndFindingDoNotOverflowTheStack() throws Exception {
        assertTrue(onSmallStack(() -> Strings.isValidEmailAddress("user@" + "a.".repeat(5000) + "com")));
        assertTrue(onSmallStack(() -> Strings.isValidEmailAddress("a.".repeat(5000) + "a@x.y")));
        assertTrue(onSmallStack(() -> Strings.isValidEmailAddress("\"" + "\\a".repeat(5000) + "\"@x.y")));
        assertTrue(onSmallStack(() -> Strings.isValidEmailAddress("user@[x:" + "a".repeat(5000) + "]")));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("see " + "a.".repeat(5000) + " end")));
        final String longDomain = "user@" + "a.".repeat(5000) + "com";
        assertEquals(longDomain, onSmallStack(() -> Strings.findFirstEmailAddress("mail " + longDomain + " now")));
        assertEquals("user@a.b.c", Strings.findFirstEmailAddress("user@a.b.c."));
        assertEquals("user@example.com", Strings.findFirstEmailAddress("Email me at user@example.com."));
        // "!x@y.z" directly follows a '.', so it is a continuation of the previous token, not a separate address (C-436)
        assertEquals(Arrays.asList("a@b.c"), Strings.findAllEmailAddresses("a@b.c.!x@y.z"));
    }

    // C-002: the general address literal is a top-level alternative of the bracketed domain
    @Test
    public void testC002_generalAddressLiteral() {
        assertTrue(Strings.isValidEmailAddress("user@[IPv6:2001:db8::1]"));
        assertTrue(Strings.isValidEmailAddress("a@[x:abc]"));
        assertTrue(Strings.isValidEmailAddress("user@[127.0.0.1]"));
        assertFalse(Strings.isValidEmailAddress("user@[1.2.3.x:abc]"));
        assertFalse(Strings.isValidEmailAddress("user@[1.2.3.4:x]"));
        assertFalse(Strings.isValidEmailAddress("user@[256.1.1.1]"));
        assertFalse(Strings.isValidEmailAddress("user@[x:]"));
        assertFalse(Strings.isValidEmailAddress("a@[x:a]b]"));
    }

    // C-435: finding is linear on long runs without '@'
    @Test
    public void testC435_emailFindingIsLinear() throws Exception {
        final String base64 = Strings.base64Encode(new byte[750_000]).replace("=", "");
        final long start = System.nanoTime();
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("a".repeat(1_000_000))));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("a.".repeat(500_000))));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses(base64)));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("\"" + "\\\"".repeat(500_000))));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("x@" + "a-".repeat(500_000))));
        // generous bound: quadratic behaviour took minutes on these inputs
        assertTrue(System.nanoTime() - start < 60_000_000_000L);
        assertEquals(100_000, Strings.findAllEmailAddresses("a@b.cc ".repeat(100_000)).size());
    }

    // C-436: no fragment of a different address is returned
    @Test
    public void testC436_emailFindingRespectsBoundaries() {
        assertNull(Strings.findFirstEmailAddress("Kontakt: m\u00FCller@firma.de"));
        assertNull(Strings.findFirstEmailAddress("j\u00F6rg@example.com"));
        assertNull(Strings.findFirstEmailAddress("user@mail.ex\u00E4mple.com"));
        assertNull(Strings.findFirstEmailAddress("a..b@c.com"));
        assertNull(Strings.findFirstEmailAddress("foo@bar@baz.com"));
        assertNull(Strings.findFirstEmailAddress("mu\u0308ller@x.com"));
        assertNull(Strings.findFirstEmailAddress("\uD801\uDC00249@a-b.com"));
        assertNull(Strings.findFirstEmailAddress("john@example.com-"));

        assertEquals("o'brien@example.com", Strings.findFirstEmailAddress("mail o'brien@example.com"));
        assertEquals("john@x.com", Strings.findFirstEmailAddress("{\"email\":\"john@x.com\"}"));
        assertEquals("john@x.com", Strings.findFirstEmailAddress("<john@x.com>"));
        assertEquals("john@x.com", Strings.findFirstEmailAddress("mailto:john@x.com"));
        assertEquals("bob@corp.io", Strings.findFirstEmailAddress("C:\\dir\\bob@corp.io"));
        assertEquals(Arrays.asList("test@gmail.orgg", "test2@gmail.cn"), Strings.findAllEmailAddresses("*** test@gmail.orgg&&^ test2@gmail.cn ((& "));
        assertEquals(EMOJI + " a@b.cc", EMOJI + " " + Strings.findFirstEmailAddress(EMOJI + " a@b.cc"));
    }

    /**
     * Verbatim copy of the r9620 nested engine of
     * {@link Strings#substringIndicesBetween(String, int, int, String, String, DelimiterMatchMode, int)} (before C-431),
     * used as the differential reference.
     */
    private static List<IndexRange> previousEngine(final String str, final int fromIndex, final int toIndex, final String begin, final String end,
            final DelimiterMatchMode delimiterMatchMode, final int maxCount) {
        if (str == null || Strings.isEmpty(begin) || Strings.isEmpty(end) || maxCount == 0) {
            return new ArrayList<>();
        }

        int idx = str.indexOf(begin, fromIndex);

        if (idx < 0 || idx > toIndex - begin.length()) {
            return new ArrayList<>();
        }

        final List<IndexRange> res = new ArrayList<>();
        final int bl = begin.length();
        final int el = end.length();

        idx += bl;

        if (delimiterMatchMode == DelimiterMatchMode.SEQUENTIAL) {
            int endIndex = -1;

            do {
                endIndex = str.indexOf(end, idx);

                if (endIndex < 0 || endIndex > toIndex - el) {
                    break;
                }

                res.add(new IndexRange(idx, endIndex));

                if (res.size() >= maxCount) {
                    break;
                }

                idx = str.indexOf(begin, endIndex + el);

                if (idx < 0) {
                    break;
                }

                idx += bl;
            } while (idx < toIndex);
        } else {
            final Deque<Integer> queue = new ArrayDeque<>();

            queue.add(idx);
            int next = -1;

            for (int i = idx; i < toIndex;) {
                if (queue.isEmpty()) {
                    idx = next >= i ? next : str.indexOf(begin, i);

                    if (idx < 0) {
                        break;
                    } else {
                        idx += bl;
                        queue.add(idx);
                        i = idx;
                    }
                }

                idx = str.indexOf(end, i);

                if (idx < 0 || idx > toIndex - el) {
                    break;
                } else {
                    final int endIndex = idx;
                    idx = res.size() > 0 ? Math.max(res.get(res.size() - 1).end() + el, queue.peekLast()) : queue.peekLast();

                    while ((idx = str.indexOf(begin, idx)) >= 0 && idx + bl <= endIndex) {
                        idx += bl;
                        queue.push(idx);
                    }

                    if (idx >= 0) {
                        next = idx;
                    }

                    final int startIndex = queue.pop();

                    if (delimiterMatchMode == DelimiterMatchMode.OUTERMOST_ONLY && res.size() > 0 && startIndex < res.get(res.size() - 1).start()) {
                        while (res.size() > 0 && startIndex < res.get(res.size() - 1).start()) {
                            res.remove(res.size() - 1);
                        }
                    }

                    res.add(new IndexRange(startIndex, endIndex));

                    if (res.size() >= maxCount && (delimiterMatchMode != DelimiterMatchMode.OUTERMOST_ONLY || queue.isEmpty())) {
                        break;
                    }

                    i = endIndex + el;
                }
            }

            if (delimiterMatchMode == DelimiterMatchMode.OUTERMOST_ONLY) {
                while (res.size() > maxCount) {
                    res.remove(res.size() - 1);
                }
            }
        }

        return res;
    }
}
