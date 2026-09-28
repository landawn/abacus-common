package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.DelimiterMatchMode;
import com.landawn.abacus.util.Strings.DisplayWidthPolicy;
import com.landawn.abacus.util.Strings.StrUtil;

/**
 * Tests for the Strings rows C-501..C-545 of the 2026-09-25 Strings/IOUtil review (ledger
 * {@code scripts/cross_review/Strings_IOUtil_ledger_2026-09-25.md}). One method (or a few) per finding; doc-only
 * findings are covered where the documented claim can be asserted cheaply, and every cross-library claim asserts both
 * sides so a Commons Lang upgrade that removes a divergence shows up as a stale sentence.
 */
public class StringsReview20260925bTest extends TestBase {

    private static final String EMOJI = "\uD83D\uDE00";

    // Greek: capital omicron, capital sigma, small alpha, small final sigma, small omicron
    private static final String OMICRON_SIGMA = "\u039F\u03A3";
    private static final String ALPHA = "\u03B1";
    private static final String FINAL_SIGMA = "\u03C2";
    private static final String SMALL_OMICRON = "\u03BF";

    // ---------------------------------------------------------------- C-501: abbreviationMarker in the IAE messages

    @Test
    public void testC501_abbreviateMessagesNameAbbreviationMarker() {
        assertEquals("maxLength (3) must be at least 4 for an abbreviationMarker of length 3",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", 3)).getMessage());
        assertEquals("maxLength (0) must be at least 1 when abbreviationMarker is empty",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "", 0)).getMessage());
        assertEquals("maxLength (6) must be at least 7 for an abbreviationMarker of length 3 at offset 5",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefghij", "...", 5, 6)).getMessage());
        assertEquals("abcd...", Strings.abbreviate("abcdefghij", 7));
    }

    // ---------------------------------------------------------------- C-502: Supplier overloads are collaborator-first

    @Test
    public void testC502_supplierOverloadsAreCollaboratorFirst() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("hello", (Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("hello", (Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("hello", (Supplier<String>) null));
        assertEquals("hello", Strings.defaultIfNull("hello", (Supplier<String>) () -> "x"));
        assertEquals("x", Strings.defaultIfBlank("  ", (Supplier<String>) () -> "x"));
    }

    // ---------------------------------------------------------------- C-503: mapWords delimiter overloads reject a null mapper result

    @Test
    public void testC503_mapWordsDelimiterOverloadsRejectNullMapperResult() {
        final NullPointerException e1 = assertThrows(NullPointerException.class, () -> Strings.mapWords("a-b", "-", w -> null));
        assertEquals("mapper returned null", e1.getMessage());

        final NullPointerException e2 = assertThrows(NullPointerException.class,
                () -> Strings.mapWords("a-b", "-", List.of("zz"), w -> null));
        assertEquals("mapper returned null", e2.getMessage());

        final NullPointerException e3 = assertThrows(NullPointerException.class, () -> Strings.mapWords("a b", w -> null));
        assertEquals("mapper returned null", e3.getMessage());

        // a mapper that returns null only for a word that is excluded is never called for it
        assertEquals("A-b", Strings.mapWords("a-b", "-", List.of("b"), w -> "b".equals(w) ? null : w.toUpperCase()));
        assertEquals("A-B", Strings.mapWords("a-b", "-", Strings::toUpperCase));
    }

    // ---------------------------------------------------------------- C-504: per-word lowercasing in toSnakeCase/toKebabCase

    @Test
    public void testC504_delimitedCaseAppliesFinalSigmaPerWordLikeCamelCase() {
        final String input = OMICRON_SIGMA + "_" + ALPHA;

        // before the fix toSnakeCase gave a non-final sigma here while toCamelCase gave the final form
        assertEquals(SMALL_OMICRON + FINAL_SIGMA + "\u0391", Strings.toCamelCase(input));
        assertEquals(SMALL_OMICRON + FINAL_SIGMA + "_" + ALPHA, Strings.toSnakeCase(input));
        assertEquals(SMALL_OMICRON + FINAL_SIGMA + "-" + ALPHA, Strings.toKebabCase(input));
        assertEquals(SMALL_OMICRON + FINAL_SIGMA + "_" + ALPHA, Strings.toSnakeCase(OMICRON_SIGMA + " " + ALPHA));
        // a lone capital sigma is its own word (case boundary after "a"); a word without a preceding cased letter is
        // not word-final in the Unicode sense, so it lowercases to the non-final form - what the JDK does for the word alone
        assertEquals("a_" + "\u03A3".toLowerCase(java.util.Locale.ROOT), Strings.toSnakeCase("a\u03A3"));
        assertEquals("a\u03A3", Strings.toCamelCase("a\u03A3"));
        assertEquals(OMICRON_SIGMA + "_\u0391", Strings.toScreamingSnakeCase(input));

        // the lowercase forms are the per-word JDK mapping
        assertEquals(OMICRON_SIGMA.toLowerCase(java.util.Locale.ROOT) + "_" + ALPHA, Strings.toSnakeCase(input));
    }

    @Test
    public void testC504_delimitedCaseUnchangedForAsciiAndOtherUnicode() {
        assertEquals("hello_world_api", Strings.toSnakeCase("helloWorldAPI"));
        assertEquals("io_error", Strings.toSnakeCase("IOError"));
        assertEquals("first_name", Strings.toSnakeCase("_first__name_"));
        assertEquals("a_b", Strings.toSnakeCase("a -_ b"));
        assertEquals("a", Strings.toSnakeCase("-a-"));
        assertEquals("hello-world-api", Strings.toKebabCase("helloWorldAPI"));
        assertEquals("STRASSE", Strings.toScreamingSnakeCase("stra\u00DFe"));
        assertEquals("x\u00DFeta", Strings.toSnakeCase("X\u00DFeta"));
        assertEquals("x_\u00DFeta", Strings.toSnakeCase("x_\u00DFeta"));
        assertEquals("xSseta", Strings.toCamelCase("x_\u00DFeta"));
        assertEquals("base64url", Strings.toSnakeCase("base64URL"));
        assertEquals("", Strings.toSnakeCase(""));
        assertNull(Strings.toSnakeCase(null));

        // the Unicode engine and the ASCII fast path agree on ASCII forced through the Unicode path (non-ASCII tail)
        final String tail = "\u00E9";
        assertEquals("hello_world" + tail, Strings.toSnakeCase("helloWorld" + tail));
        assertEquals("HELLO_WORLD\u00C9", Strings.toScreamingSnakeCase("helloWorld" + tail));
        assertEquals("hello_world_" + tail, Strings.toSnakeCase("helloWorld_" + tail));
        // an emoji is neither lowercase nor uppercase, so "Y" after it is a boundary only when a lowercase letter follows
        assertEquals("x" + EMOJI + "y", Strings.toSnakeCase("x" + EMOJI + "Y"));
        assertEquals("x" + EMOJI + "_ya", Strings.toSnakeCase("x" + EMOJI + "Ya"));
    }

    // ---------------------------------------------------------------- C-505: abbreviateMiddle best-effort slack (comment only)

    @Test
    public void testC505_abbreviateMiddleStaysWithinDocumentedWindow() {
        final String str = "bc\uD801\uDC00d\uD801\uDC00\uD83D" + EMOJI + "cb";
        final String result = Strings.abbreviateMiddle(str, ".", 6);
        assertEquals("bc.cb", result);
        assertTrue(result.length() >= 5 && result.length() <= 6);
    }

    // ---------------------------------------------------------------- C-507: pad/center declare OutOfMemoryError

    @Test
    public void testC507_padAndCenterSurfaceOutOfMemoryErrorLikeRepeat() {
        assertThrows(OutOfMemoryError.class, () -> Strings.padStart("a", Integer.MAX_VALUE, "xy"));
        assertThrows(OutOfMemoryError.class, () -> Strings.padEnd("a", Integer.MAX_VALUE, "xy"));
        assertThrows(OutOfMemoryError.class, () -> Strings.center("a", Integer.MAX_VALUE, "xy"));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("xy", Integer.MAX_VALUE));
    }

    // ---------------------------------------------------------------- C-508: 5-arg repeat @return rules

    @Test
    public void testC508_repeatWithAffixesRestatesZeroAndEmptyRules() {
        assertEquals("[]", Strings.repeat("ab", 0, ",", "[", "]"));
        assertEquals("[,,]", Strings.repeat(null, 3, ",", "[", "]"));
        assertEquals("[,,]", Strings.repeat("", 3, ",", "[", "]"));
        assertEquals("[ab,ab,ab]", Strings.repeat("ab", 3, ",", "[", "]"));
        assertEquals(",,", Strings.repeat(null, 3, ","));
    }

    // ---------------------------------------------------------------- C-510: max == 1 preserve forms equal the worker

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

    // ---------------------------------------------------------------- C-512: negative-index rule of the case-sensitive forms

    @Test
    public void testC512_negativeIndexRulesOfCaseSensitiveRemoveAndReplace() {
        assertEquals("bcbc", Strings.removeAll("abcabc", -1, 'a'));
        assertEquals("bcbc", Strings.removeAll("abcabc", -1, "a"));
        assertEquals("zbz", Strings.replaceAll("aba", -1, "a", "z"));
        assertEquals("abcabc", Strings.replaceLast("abcabc", -1, "a", "z"));
        assertEquals("abczbc", Strings.replaceLast("abcabc", 3, "a", "z"));
    }

    // ---------------------------------------------------------------- C-513: U+0338 negation overlay is preserved

    @Test
    public void testC513_stripAccentsKeepsTheNegationOverlay() {
        assertEquals("\u2260", Strings.stripAccents("\u2260"));
        assertEquals("a \u2260 b", Strings.stripAccents("a \u2260 b"));
        assertEquals("\u2209", Strings.stripAccents("\u2209"));
        assertEquals("\u226E", Strings.stripAccents("\u226E"));
        assertEquals("a\u0338", Strings.stripAccents("a\u0338"));
        assertEquals("\u0338", Strings.stripAccents("\u0338"));
        assertEquals("=\u0338", Strings.stripAccents("=\u0338"));

        // accents next to a preserved overlay are still stripped; the overlay keeps its symbol
        assertEquals("e \u2260 e", Strings.stripAccents("\u00E9 \u2260 \u00E8"));
        assertEquals("eclair", Strings.stripAccents("\u00E9clair"));
        assertEquals("Lodz", Strings.stripAccents("\u0141\u00F3d\u017A"));
        assertEquals("", Strings.stripAccents(""));
        assertNull(Strings.stripAccents(null));

        // the documented Commons Lang divergence
        assertEquals("=", StringUtils.stripAccents("\u2260"));
    }

    // ---------------------------------------------------------------- C-514: only L-stroke is special-cased

    @Test
    public void testC514_onlyLWithStrokeIsMappedSeparately() {
        assertEquals("Ll", Strings.stripAccents("\u0141\u0142"));
        assertEquals("\u0110\u0111", Strings.stripAccents("\u0110\u0111"));
        assertEquals("\u0126\u0127", Strings.stripAccents("\u0126\u0127"));
        assertEquals("\u0166\u0167", Strings.stripAccents("\u0166\u0167"));
        assertEquals("\u00D8\u00F8", Strings.stripAccents("\u00D8\u00F8"));

        // Commons maps a few more stroked letters (the javadoc names D-stroke and T-stroke)
        assertEquals("Dd", StringUtils.stripAccents("\u0110\u0111"));
        assertEquals("Tt", StringUtils.stripAccents("\u0166\u0167"));
    }

    // ---------------------------------------------------------------- C-515: five Commons divergences now flagged

    @Test
    public void testC515_flaggedCommonsDivergences() {
        assertEquals("xx", Strings.wrap("", "x"));
        assertEquals("xx", Strings.wrapIfMissing("", "x"));
        assertEquals("[]", Strings.wrapIfMissing("", "[", "]"));
        assertEquals("", StringUtils.wrap("", "x"));
        assertEquals("", StringUtils.wrapIfMissing("", "x"));

        assertEquals("aaaa", Strings.wrapIfMissing("aa", "aa"));
        assertEquals("\"\"", Strings.wrapIfMissing("\"", "\""));
        assertEquals("aaaa", Strings.wrapIfMissing("aa", "aa", "aa"));
        assertEquals("aa", StringUtils.wrapIfMissing("aa", "aa"));
        assertEquals("\"", StringUtils.wrapIfMissing("\"", "\""));

        assertEquals("a", Strings.chop("a" + EMOJI));
        assertEquals("a\uD83D", StringUtils.chop("a" + EMOJI));

        assertTrue(Strings.isMixedCase("\u01C5a"));
        assertFalse(StringUtils.isMixedCase("\u01C5a"));

        assertEquals("\u0132", Strings.stripAccents("\u0132"));
        assertEquals("IJ", StringUtils.stripAccents("\u0132"));
    }

    // ---------------------------------------------------------------- C-516: offset non-negative; empty stripChars strips nothing

    @Test
    public void testC516_truncateOffsetAndStripCharsContracts() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", -1, 2)).getMessage().contains("offset"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.truncateEach(new String[] { "abc" }, -1, 2)).getMessage()
                .contains("offset"));
        assertEquals("abc", Strings.stripStart("abc", ""));
        assertEquals("abc", Strings.stripEnd("abc", ""));
        assertEquals(" abc ", Strings.stripStart(" abc ", ""));
        assertEquals("abc ", Strings.stripStart(" abc ", null));
        assertEquals(" abc", Strings.stripEnd(" abc ", null));
    }

    // ---------------------------------------------------------------- C-518: empty exclusion set vs Commons

    @Test
    public void testC518_indexOfAnyButEmptyExclusionSetDivergesFromCommons() {
        assertEquals(0, Strings.indexOfAnyBut("bad"));
        assertEquals(0, Strings.indexOfAnyBut("bad", (char[]) null));
        assertEquals(1, Strings.indexOfAnyBut("bad", 1));
        assertEquals(1, Strings.indexOfAnyBut("bad", 1, (char[]) null));
        assertEquals(-1, Strings.indexOfAnyBut("bad", 3));
        assertEquals(-1, Strings.indexOfAnyBut(""));
        assertEquals(-1, Strings.indexOfAnyBut((String) null));
        assertEquals(-1, StringUtils.indexOfAnyBut("bad"));
        assertEquals(-1, StringUtils.indexOfAnyBut("bad", (char[]) null));
    }

    // ---------------------------------------------------------------- C-519: indexOf example wording

    @Test
    public void testC519_indexOfAtTheTerminalBoundary() {
        assertEquals(-1, Strings.indexOf("hello", "o", 5));
        assertEquals(5, Strings.indexOf("hello", "", 5));
        assertEquals(5, Strings.indexOf("hello", "", 99));
        assertEquals(-1, Strings.indexOf("hello", "o", 6));
    }

    // ---------------------------------------------------------------- C-520: char min/maxLastIndexOfAll startIndexFromBack rules

    @Test
    public void testC520_charLastIndexOfAllStartIndexRules() {
        assertEquals(-1, Strings.minLastIndexOfAll("hello", -1, 'l'));
        assertEquals(-1, Strings.maxLastIndexOfAll("hello", -1, 'l'));
        assertEquals(3, Strings.minLastIndexOfAll("hello", 100, 'l', 'o'));
        assertEquals(4, Strings.maxLastIndexOfAll("hello", 100, 'l', 'o'));
        assertEquals(3, Strings.minLastIndexOfAll("hello", 5, 'l', 'o'));
        assertEquals(4, Strings.maxLastIndexOfAll("hello", 4, 'l', 'o'));
        assertEquals(2, Strings.maxLastIndexOfAll("hello", 2, 'l', 'o'));
    }

    // ---------------------------------------------------------------- C-521: table rows for the char and String overloads

    @Test
    public void testC521_indexOfAllFamilyEmptyHaystackRowsByOverload() {
        assertEquals(-1, Strings.minIndexOfAll("", new char[] { 'a' }));
        assertEquals(-1, Strings.maxIndexOfAll("", new char[] { 'a' }));
        assertEquals(-1, Strings.minLastIndexOfAll("", new char[] { 'a' }));
        assertEquals(-1, Strings.maxLastIndexOfAll("", new char[] { 'a' }));
        assertEquals(-1, Strings.indexOfAny("", new char[] { 'a' }));
        assertEquals(-1, Strings.lastIndexOfAny("", new char[] { 'a' }));
        assertEquals(-1, Strings.indexOfAnyBut("", new char[] { 'a' }));

        assertEquals(0, Strings.minIndexOfAll("", ""));
        assertEquals(0, Strings.maxIndexOfAll("", ""));
        assertEquals(0, Strings.minLastIndexOfAll("", ""));
        assertEquals(0, Strings.maxLastIndexOfAll("", ""));
        assertEquals(0, Strings.indexOfAny("", ""));
        assertEquals(0, Strings.lastIndexOfAny("", ""));
        assertEquals(-1, Strings.minIndexOfAll("", "a"));
        assertEquals(-1, Strings.minIndexOfAll((String) null, ""));
    }

    // ---------------------------------------------------------------- C-523: negative start short-circuit; reassigned fromIndex

    @Test
    public void testC523_lastIndexOfAnyNegativeStartAndMaxIndexOfAllFromIndex() {
        assertEquals(-1, Strings.lastIndexOfAny("abc", -1, "a"));
        assertEquals(-1, Strings.lastIndexOfAny("abc", -1, ""));
        assertEquals(-1, Strings.lastIndexOfAny("abc", Integer.MIN_VALUE, "a", "b"));
        assertEquals(0, Strings.lastIndexOfAny("abc", 0, "a", "b"));
        assertEquals(3, Strings.lastIndexOfAny("abc", 100, ""));

        assertEquals(2, Strings.maxIndexOfAll("abc", -5, "c", "a"));
        assertEquals(2, Strings.maxIndexOfAll("abc", 0, "c", "a"));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 4, "a"));
        assertEquals(3, Strings.maxIndexOfAll("abc", 3, ""));
        assertEquals(3, Strings.maxIndexOfAll("abcabc", 1, "a", "c"));
    }

    // ---------------------------------------------------------------- C-524: substringBefore/Last dead branches and char twin guard

    @Test
    public void testC524_substringBeforeAtLengthAndEmptyInputs() {
        assertNull(Strings.substringBefore("test", 4, "@"));
        assertEquals("", Strings.substringBefore("test", 4, ""));
        assertNull(Strings.substringBefore("test", 5, "@"));
        assertEquals("", Strings.substringBefore("test@end", 4, "@"));
        assertNull(Strings.substringBefore("", 0, "@"));
        assertEquals("", Strings.substringBefore("", 0, ""));

        assertNull(Strings.substringBeforeLast("a.b", 3, "."));
        assertEquals("", Strings.substringBeforeLast("a.b", 3, ""));
        assertEquals("b.c", Strings.substringBeforeLast("a.b.c.d", 2, "."));
        assertNull(Strings.substringBeforeLast("", 0, "."));

        assertNull(Strings.substringBefore("", '@'));
        assertNull(Strings.substringBefore((String) null, '@'));
        assertEquals("", Strings.substringBefore("@x", '@'));
        assertEquals("user", Strings.substringBefore("user@example.com", '@'));
        assertNull(Strings.substringAfter("", '@'));
        assertNull(Strings.substringBeforeLast("", '@'));
    }

    // ---------------------------------------------------------------- C-527: join/joinEntries/concat are not cycle-safe

    @Test
    public void testC527_selfContainingCollectionOverflowsTheStack() {
        final List<Object> self = new ArrayList<>();
        self.add("x");
        self.add(self);

        assertThrows(StackOverflowError.class, () -> Strings.join(self));
        assertThrows(StackOverflowError.class, () -> Strings.join(new Object[] { self }, ", "));
        assertThrows(StackOverflowError.class, () -> Strings.join(self.iterator(), ", "));

        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", self);
        assertThrows(StackOverflowError.class, () -> Strings.joinEntries(map));
        assertThrows(StackOverflowError.class, () -> Strings.concat(self, "x"));

        // lenientFormat goes through N.deepToString and survives
        assertEquals("[x, (this Collection)]", Strings.lenientFormat("%s", self));
        assertEquals("[x, (this Collection)]", self.toString());
    }

    // ---------------------------------------------------------------- C-528: joinEntries(Map range) after the dead sub-condition removal

    @Test
    public void testC528_joinEntriesRangeUnchanged() {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        m.put("d", 4);

        assertEquals("b=2, c=3", Strings.joinEntries(m, 1, 3, ", ", "=", "", "", false));
        assertEquals("<b=2, c=3>", Strings.joinEntries(m, 1, 3, ", ", "=", "<", ">", false));
        assertEquals("a=1", Strings.joinEntries(m, 0, 1, ", ", "=", "", "", false));
        assertEquals("d=4", Strings.joinEntries(m, 3, 4, ", ", "=", "", "", false));
        assertEquals("", Strings.joinEntries(m, 2, 2, ", ", "=", "", "", false));
        assertEquals("a=1, b=2, c=3, d=4", Strings.joinEntries(m, 0, 4, ", ", "=", "", "", false));
        assertEquals("b=2, c=3", Strings.joinEntries(m, 1, 3, ", "));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.joinEntries(m, 1, 5, ", ", "=", "", "", false));
    }

    // ---------------------------------------------------------------- C-530: reverse and ill-formed UTF-16

    @Test
    public void testC530_reverseCanFuseLoneSurrogates() {
        final String x = "a\uDE00\uD83Db";
        final String reversed = Strings.reverse(x);
        assertEquals("b\uD83D\uDE00a", reversed);
        assertEquals(4, x.codePointCount(0, x.length()));
        assertEquals(3, reversed.codePointCount(0, reversed.length()));
        assertNotEquals(x, Strings.reverse(reversed));
        assertEquals("a\uD83D\uDE00b", Strings.reverse(reversed));

        // well-formed input round-trips by code point
        assertEquals("b" + EMOJI + "a", Strings.reverse("a" + EMOJI + "b"));
        assertEquals("a" + EMOJI + "b", Strings.reverse(Strings.reverse("a" + EMOJI + "b")));
    }

    // ---------------------------------------------------------------- C-531: reverseDelimited(String, String) is not an involution

    @Test
    public void testC531_reverseDelimitedSelfOverlappingDelimiter() {
        assertEquals(" :::", Strings.reverseDelimited(":: :", "::"));
        assertEquals("::: ", Strings.reverseDelimited(" :::", "::"));
        assertNotEquals(":: :", Strings.reverseDelimited(Strings.reverseDelimited(":: :", "::"), "::"));

        // the char overload is an involution
        assertEquals(":: :", Strings.reverseDelimited(Strings.reverseDelimited(":: :", ':'), ':'));
        assertEquals("::a::::b::", Strings.reverseDelimited(Strings.reverseDelimited("::a::::b::", "::"), "::"));
    }

    // ---------------------------------------------------------------- C-532: Hangul syllable blocks are one cell pair

    @Test
    public void testC532_hangulSyllableBlocksMeasureAsOneCluster() {
        final String ll = "\u1100\u1100";
        final String lSyllable = "\u1100\uAC00";
        final String nfd = Normalizer.normalize(lSyllable, Normalizer.Form.NFD);
        assertEquals("\u1100\u1100\u1161", nfd);

        assertEquals(2, Strings.displayWidth(ll));
        assertEquals(2, Strings.displayWidth(lSyllable));
        assertEquals(2, Strings.displayWidth(nfd));
        assertEquals(Strings.displayWidth(lSyllable), Strings.displayWidth(nfd));
        assertEquals(2, Strings.displayWidth("\u1107\uC0C1"));
        assertEquals(4, Strings.displayWidth("\uAC00\uAC00"));
        assertEquals(4, Strings.displayWidth("a" + ll + "b"));

        assertEquals(2, Strings.displayWidth(ll, DisplayWidthPolicy.CJK));
        assertEquals(2, Strings.displayWidth(lSyllable, DisplayWidthPolicy.CJK));
        assertEquals(2, Strings.displayWidth("\u1107\uC0C1", DisplayWidthPolicy.CJK));
        assertEquals(4, Strings.displayWidth("\uAC00\uAC00", DisplayWidthPolicy.CJK));
        assertEquals(4, Strings.displayWidth("a" + ll + "b", DisplayWidthPolicy.CJK));

        assertEquals(ll + "  ", Strings.padEndToDisplayWidth(ll, 4));
        assertEquals("  " + ll, Strings.padStartToDisplayWidth(ll, 4));

        // a per-code-point sum would say 4 for the two leading jamo
        assertEquals(2, Strings.codePointDisplayWidth(0x1100));
        assertEquals(0, Strings.codePointDisplayWidth(0x1161));
    }

    // ---------------------------------------------------------------- C-534: encodeUrlQuery(Object) uses UTF-8

    @Test
    public void testC534_encodeUrlQueryUsesUtf8() {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("a", "\u4E2D");
        assertEquals("a=%E4%B8%AD", Strings.encodeUrlQuery(m));
        assertEquals("a=%E4%B8%AD", Strings.encodeUrlQuery(m, (Charset) null));
        assertEquals(Strings.encodeUrlQuery(m, Charsets.UTF_8), Strings.encodeUrlQuery(m));
        assertEquals("", Strings.encodeUrlQuery(null));
    }

    // ---------------------------------------------------------------- C-535: U+FE0E is zero width and never demotes

    @Test
    public void testC535_textPresentationSelectorNeverNarrows() {
        assertEquals(0, Strings.codePointDisplayWidth(0xFE0E));
        assertEquals(0, Strings.codePointDisplayWidth(0xFE0F));
        assertEquals(2, Strings.displayWidth("\u231A\uFE0E"));
        assertEquals(2, Strings.displayWidth("\u231A"));
        assertEquals(1, Strings.displayWidth("\u2764\uFE0E"));
        assertEquals(1, Strings.displayWidth("\u2764"));
        assertEquals(2, Strings.displayWidth("\u2764\uFE0F"));
        assertEquals(2, Strings.displayWidth(EMOJI + "\uFE0E"));
        assertEquals(1, Strings.displayWidth("a\uFE0E"));
    }

    // ---------------------------------------------------------------- C-536: ASCII digits only

    @Test
    public void testC536_numberFindersMatchAsciiDigitsOnly() {
        final String arabicIndic = "\u0663\u0664";
        assertTrue(Character.isDigit('\u0663'));
        assertNull(Strings.findFirstInteger(arabicIndic));
        assertNull(Strings.findFirstDouble("\u0663.\u0664"));
        assertNull(Strings.findFirstDouble("\u0663.\u0664", true));
        assertSame(arabicIndic, Strings.replaceFirstInteger(arabicIndic, "X"));
        assertSame(arabicIndic, Strings.replaceFirstDouble(arabicIndic, "X"));
        assertSame(arabicIndic, Strings.replaceFirstDouble(arabicIndic, "X", true));
        assertEquals("34", Strings.findFirstInteger("x\u0663" + "34"));
        assertEquals("12.5", Strings.findFirstDouble("\u0663 12.5"));
    }

    // ---------------------------------------------------------------- C-537: the three parseUrlQueryLenient delegates

    @Test
    public void testC537_parseUrlQueryLenientDelegates() {
        assertEquals(Map.of("a", "%zz"), Strings.parseUrlQueryLenient("a=%zz", (Charset) null));
        assertEquals(Map.of("a", "%zz"), Strings.parseUrlQueryLenient("a=%zz", Charsets.UTF_8));
        assertEquals(Map.of("a", "%zz"), Strings.parseUrlQueryLenient("a=%zz", Map.class));
        assertEquals(Map.of("a", "%zz"), Strings.parseUrlQueryLenient("a=%zz", (Charset) null, Map.class));
        assertEquals(Map.of("a", "b c"), Strings.parseUrlQueryLenient("a=b+c", Charsets.UTF_8, Map.class));
        assertTrue(Strings.parseUrlQueryLenient("", Map.class).isEmpty());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQueryLenient("", (Class<?>) null)).getMessage()
                .contains("targetType"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQueryLenient("a=1", Charsets.UTF_8, (Class<?>) null))
                .getMessage().contains("targetType"));
    }

    // ---------------------------------------------------------------- C-538: token family rejects a null/empty delimiter

    @Test
    public void testC538_tokenFamilyRejectsNullOrEmptyDelimiter() {
        for (final String delimiter : new String[] { null, "" }) {
            assertIaeNamingDelimiter(() -> StrUtil.indexOfToken("a,b", "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfToken("a,b", "b", delimiter, 0));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfTokenIgnoreCase("a,b", "B", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfTokenIgnoreCase("a,b", "B", delimiter, 0));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfToken("a,b", "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfToken("a,b", "b", delimiter, 3));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfTokenIgnoreCase("a,b", "B", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfTokenIgnoreCase("a,b", "B", delimiter, 3));
            assertIaeNamingDelimiter(() -> StrUtil.containsToken("a,b", "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.containsTokenIgnoreCase("a,b", "B", delimiter));

            // collaborator-first: even a null str or token does not short-circuit before the delimiter check
            assertIaeNamingDelimiter(() -> StrUtil.indexOfToken(null, "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfToken("a,b", null, delimiter, 0));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfTokenIgnoreCase(null, null, delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfTokenIgnoreCase(null, "b", delimiter, -1));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfToken(null, "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfToken(null, "b", delimiter, -1));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfTokenIgnoreCase(null, "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfTokenIgnoreCase("a,b", null, delimiter, 0));
            assertIaeNamingDelimiter(() -> StrUtil.containsToken(null, "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.containsTokenIgnoreCase(null, null, delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.containsToken("", "", delimiter));
        }

        // the applesauce misuse from the review now fails loudly; the plain search is one call away
        assertThrows(IllegalArgumentException.class, () -> StrUtil.containsToken("applesauce", "apple", ""));
        assertTrue(Strings.contains("applesauce", "apple"));

        // a non-empty delimiter behaves as before, including null str/token and the empty-token field rule
        assertEquals(2, StrUtil.indexOfToken("a,b", "b", ","));
        assertEquals(6, StrUtil.indexOfToken("apple,banana,cherry", "banana", ",", 0));
        assertEquals(-1, StrUtil.indexOfToken(null, "b", ","));
        assertEquals(-1, StrUtil.indexOfToken("a,b", null, ","));
        assertEquals(13, StrUtil.lastIndexOfToken("apple,banana,apple", "apple", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("apple", "apple", ",", -1));
        assertEquals(4, StrUtil.lastIndexOfToken("a,b,", "", ","));
        assertEquals(3, StrUtil.indexOfToken("ab,", "", ",", 10));
        assertEquals(-1, StrUtil.indexOfToken("ab", "", ",", 10));
        assertTrue(StrUtil.containsToken(",a", "", ","));
        assertFalse(StrUtil.containsToken("a,b", "", ","));
        assertFalse(StrUtil.containsToken(null, "a", ","));
        assertFalse(StrUtil.containsTokenIgnoreCase("a,b", null, ","));
        assertTrue(StrUtil.containsTokenIgnoreCase("Apple,Banana,Orange", "banana", ","));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("BaBaBaB", "A", "BAB"));
        assertEquals(0, StrUtil.indexOfToken("aaa", "a", "aa"));
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(3, StrUtil.indexOfToken(EMOJI + "," + "b", "b", ","));
        assertEquals(0, StrUtil.indexOfToken("a" + EMOJI + "b", "a", EMOJI));
    }

    private static void assertIaeNamingDelimiter(final org.junit.jupiter.api.function.Executable executable) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertTrue(e.getMessage().contains("delimiter"), e.getMessage());
    }

    // ---------------------------------------------------------------- C-540: the delimiter boundary is matched ignoring case

    @Test
    public void testC540_ignoreCaseTokenSearchMatchesTheDelimiterIgnoringCase() {
        assertEquals(6, StrUtil.indexOfTokenIgnoreCase("a AND b", "b", "and "));
        assertEquals(-1, StrUtil.indexOfToken("a AND b", "b", "and "));
        assertEquals(6, StrUtil.indexOfTokenIgnoreCase("a AND b", "b", "and ", 0));
        assertEquals(-1, StrUtil.indexOfToken("a AND b", "b", "and ", 0));
        assertEquals(6, StrUtil.lastIndexOfTokenIgnoreCase("a AND b", "b", "and "));
        assertEquals(-1, StrUtil.lastIndexOfToken("a AND b", "b", "and "));
        assertEquals(6, StrUtil.lastIndexOfTokenIgnoreCase("a AND b", "b", "and ", 6));
        assertEquals(-1, StrUtil.lastIndexOfToken("a AND b", "b", "and ", 6));
        assertTrue(StrUtil.containsTokenIgnoreCase("xANDy", "y", "and"));
        assertFalse(StrUtil.containsToken("xANDy", "y", "and"));
        assertTrue(StrUtil.containsTokenIgnoreCase("One-Two-Three", "TWO", "-"));

        // the containment pre-check is also case-insensitive (documented before this cycle)
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND"));
        assertEquals(4, StrUtil.indexOfToken("pANDxandyANDq", "xandy", "AND"));
    }

    // ---------------------------------------------------------------- C-542: OUTERMOST_ONLY + maxCount no longer stores every later match

    @Test
    public void testC542_outermostOnlyWithSmallMaxCountResultUnchanged() {
        final String str = "[" + "[a]".repeat(2000);
        assertEquals(List.of(new IndexRange(2, 3)), Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of("a", "a"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));
        assertEquals(List.of(new IndexRange(2, 3), new IndexRange(5, 6)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));

        // the pinned shape from StringsSubstringTest: an enclosing match still replaces a stored nested one
        assertEquals(List.of("a2[c]"), Strings.substringsBetween("3[a2[c]]2[a]", 0, 12, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of("a2[c]", "a"), Strings.substringsBetween("3[a2[c]]2[a]", 0, 12, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));
        assertEquals(List.of("[b[a]]c"), Strings.substringsBetween("[[b[a]]c]", 0, 9, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of("c"), Strings.substringsBetween("a[b[c]", 0, 6, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of(), Strings.substringsBetween("3[a2[c]]2[a]", 0, 12, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 0));
    }

    @Test
    public void testC542_maxCountIsAlwaysAPrefixOfTheUnlimitedResult() {
        final Random random = new Random(20260925L);
        final String[][] delimiterPairs = { { "[", "]" }, { "ab", "bc" }, { "aa", "a" }, { "a", "ab" }, { "x", "xx" }, { "[]", "[" } };
        final char[] alphabet = { 'a', 'b', 'c', 'x', '[', ']' };
        int checks = 0;

        for (int round = 0; round < 20000; round++) {
            final int len = random.nextInt(24);
            final StringBuilder sb = new StringBuilder(len);

            for (int i = 0; i < len; i++) {
                sb.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final String str = sb.toString();
            final String[] pair = delimiterPairs[random.nextInt(delimiterPairs.length)];
            final int from = random.nextInt(len + 1);
            final int to = from + random.nextInt(len - from + 1);

            for (final DelimiterMatchMode mode : DelimiterMatchMode.values()) {
                final List<IndexRange> unlimited = Strings.substringIndicesBetween(str, from, to, pair[0], pair[1], mode, Integer.MAX_VALUE);

                for (int maxCount = 0; maxCount <= 4; maxCount++) {
                    final List<IndexRange> limited = Strings.substringIndicesBetween(str, from, to, pair[0], pair[1], mode, maxCount);
                    assertEquals(unlimited.subList(0, Math.min(maxCount, unlimited.size())), limited,
                            () -> "str=" + str + " from=" + from + " to=" + to + " pair=" + Arrays.toString(pair) + " mode=" + mode);
                    checks++;
                }
            }
        }

        assertTrue(checks > 0);
    }

    // ---------------------------------------------------------------- C-543: int/long join docs restate the null/empty rules

    @Test
    public void testC543_intAndLongJoinNullAndEmptyRules() {
        assertEquals("", Strings.join((int[]) null, ", "));
        assertEquals("", Strings.join(new int[] {}, ", "));
        assertEquals("12", Strings.join(new int[] { 1, 2 }, null));
        assertEquals("12", Strings.join(new int[] { 1, 2 }, ""));
        assertEquals("", Strings.join((int[]) null, 0, 0, ", "));
        assertEquals("", Strings.join(new int[] { 1, 2 }, 1, 1, ", "));
        assertEquals("[]", Strings.join((int[]) null, 0, 0, ", ", "[", "]"));

        assertEquals("", Strings.join((long[]) null, ", "));
        assertEquals("", Strings.join(new long[] {}, ", "));
        assertEquals("12", Strings.join(new long[] { 1L, 2L }, null));
        assertEquals("", Strings.join((long[]) null, 0, 0, ", "));
        assertEquals("", Strings.join(new long[] { 1L, 2L }, 1, 1, ", "));
        assertEquals("[]", Strings.join((long[]) null, 0, 0, ", ", "[", "]"));
        assertEquals("[2]", Strings.join(new long[] { 1L, 2L }, 1, 2, ", ", "[", "]"));
    }

    // ---------------------------------------------------------------- C-544: operator substringBetween @param delimiter may be null

    @Test
    public void testC544_operatorSubstringBetweenNullDelimiter() {
        assertNull(Strings.substringBetween("Hello", (String) null, i -> 5));
        assertNull(Strings.substringBetween("Hello", i -> 0, (String) null));
        assertNull(Strings.substringBetween("Hello", "HelloWorld", i -> 5));
        assertNull(Strings.substringBetween("Hello", "x", i -> 5));
        assertEquals("World", Strings.substringBetween("Hello:World", ":", i -> i + 5));
        assertEquals("ello", Strings.substringBetween("Hello:World", i -> i - 5, ":"));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween(null, (String) null, (java.util.function.IntUnaryOperator) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween(null, (java.util.function.IntUnaryOperator) null, (String) null));
    }

    // ---------------------------------------------------------------- C-545: substringsBetween maxCount counting rule

    @Test
    public void testC545_substringsBetweenMaxCountCountsPerMode() {
        final String str = "3[a2[c]]2[a]";
        assertEquals(List.of("a2[c]"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of("a2[c"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.SEQUENTIAL, 1));
        assertEquals(List.of("c"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.ALL_LEVELS, 1));
        assertEquals(List.of("c", "a2[c]"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.ALL_LEVELS, 2));

        // substrings == mapped indices for the same arguments
        final List<IndexRange> ranges = Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1);
        assertEquals(1, ranges.size());
        assertEquals("a2[c]", str.substring(ranges.get(0).start(), ranges.get(0).end()));
    }
}
