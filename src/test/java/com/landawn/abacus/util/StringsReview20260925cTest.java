package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.DisplayWidthPolicy;
import com.landawn.abacus.util.Strings.StrUtil;

/**
 * Tests for the Strings rows C-546..C-573 (cycle 2) of the 2026-09-25 Strings/IOUtil review (ledger
 * {@code scripts/cross_review/Strings_IOUtil_ledger_2026-09-25.md}). One method (or a few) per finding; doc-only
 * findings are covered where the documented claim can be asserted cheaply, and every cross-library claim asserts both
 * sides so a Commons Lang upgrade that removes a divergence shows up as a stale sentence.
 */
public class StringsReview20260925cTest extends TestBase {

    // U+1F600 GRINNING FACE, U+1F601 GRINNING FACE WITH SMILING EYES (same high surrogate U+D83D)
    private static final String SMILE = "\uD83D\uDE00";
    private static final String GRIN = "\uD83D\uDE01";
    // U+1F300 CYCLONE and U+1F700 ALCHEMICAL SYMBOL FOR QUINTESSENCE share the low surrogate U+DF00
    private static final String CYCLONE = "\uD83C\uDF00";
    private static final String QUINTESSENCE = "\uD83D\uDF00";

    private static final String CAPITAL_SIGMA = "\u03A3";
    private static final String SMALL_SIGMA = "\u03C3";
    private static final String FINAL_SIGMA = "\u03C2";

    // ---------------------------------------------------------------- C-546: abbreviate empty marker vs Commons

    @Test
    public void testC546_emptyMarkerNonPositiveMaxLengthThrowsWhereCommonsReturnsInput() {
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdef", "", 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdef", "", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdef", "", 100, 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdef", "", 0, Integer.MIN_VALUE));

        // the other side of the documented divergence
        assertEquals("abcdef", StringUtils.abbreviate("abcdef", "", 0));
        assertEquals("abcdef", StringUtils.abbreviate("abcdef", "", 100, 0));

        // boundary: maxLength 1 is accepted by both and truncates
        assertEquals("a", Strings.abbreviate("abcdef", "", 1));
        assertEquals("a", StringUtils.abbreviate("abcdef", "", 1));

        // null / empty str are never validated
        assertNull(Strings.abbreviate(null, "", 0));
        assertEquals("", Strings.abbreviate("", "", 0));
    }

    // ---------------------------------------------------------------- C-548: word-initial sigma keeps the non-final form

    @Test
    public void testC548_sigmaStartingItsWordStaysNonFinal() {
        assertEquals("a_" + SMALL_SIGMA, Strings.toSnakeCase("a" + CAPITAL_SIGMA));
        assertEquals("a-" + SMALL_SIGMA, Strings.toKebabCase("a" + CAPITAL_SIGMA));
        assertEquals(SMALL_SIGMA, Strings.toSnakeCase(CAPITAL_SIGMA));
        assertEquals(SMALL_SIGMA + "_x", Strings.toSnakeCase(CAPITAL_SIGMA + "_x"));

        // a sigma that ends a word after a cased letter becomes final (the documented examples)
        assertEquals("\u03BF" + FINAL_SIGMA + "_\u03B1", Strings.toSnakeCase("\u039F" + CAPITAL_SIGMA + "_\u03B1"));
        assertEquals("\u03BF" + FINAL_SIGMA + "-\u03B1", Strings.toKebabCase("\u039F" + CAPITAL_SIGMA + "_\u03B1"));
    }

    // ---------------------------------------------------------------- C-550: capitalizeWords* delimiter is collaborator-first

    @Test
    public void testC550_capitalizeWordsDelimiterValidatedBeforeNullOrEmptyStr() {
        final List<String> excluded = Arrays.asList("of");

        for (final String str : new String[] { null, "" }) {
            for (final String delimiter : new String[] { null, "" }) {
                assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords(str, delimiter));
                assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords(str, delimiter, excluded));
                assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWordsFully(str, delimiter));
                assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWordsFully(str, delimiter, excluded));
            }
        }

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords(null, null)).getMessage().contains("delimiter"));

        // with a valid delimiter the null / empty short-circuit still applies
        assertNull(Strings.capitalizeWords(null, ","));
        assertEquals("", Strings.capitalizeWords("", ",", excluded));
        assertNull(Strings.capitalizeWordsFully(null, ","));
        assertEquals("", Strings.capitalizeWordsFully("", ",", excluded));
        assertEquals("Man,of,War", Strings.capitalizeWords("man,of,war", ",", excluded));
    }

    // ---------------------------------------------------------------- C-551: validation lives in the public methods only

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

    private static String[] nullToEmpty(final String[] array) {
        return array == null ? new String[0] : array;
    }

    @Test
    public void testC551_removeAllDelegatesGuardsToTheReplaceEngine() {
        assertEquals("acac", Strings.removeAll("abcabc", -5, "b"));
        assertEquals("acac", Strings.removeAll("abcabc", Integer.MIN_VALUE, "b"));
        assertEquals("abcac", Strings.removeAll("abcabc", 2, "b"));
        assertEquals("ACAC", Strings.removeAllIgnoreCase("ABCABC", -1, "b"));
        assertEquals("aBcAc", Strings.removeAllIgnoreCase("aBcABc", 3, "b"));

        final String str = "abc";
        assertSame(str, Strings.removeAll(str, 3, "c"));
        assertSame(str, Strings.removeAll(str, Integer.MAX_VALUE, "a"));
        assertSame(str, Strings.removeAll(str, 0, ""));
        assertSame(str, Strings.removeAll(str, 0, null));
        assertSame(str, Strings.removeAll(str, 0, "x"));
        assertSame(str, Strings.removeAllIgnoreCase(str, 3, "C"));
        assertSame(str, Strings.removeAllIgnoreCase(str, 0, null));
        assertNull(Strings.removeAll(null, 0, "a"));
        assertNull(Strings.removeAllIgnoreCase(null, -1, "a"));
        assertEquals("", Strings.removeAll("", 5, "x"));
        assertEquals("", Strings.removeAllIgnoreCase("", -5, "x"));

        // Unicode: a supplementary removeStr is removed as a whole pair
        assertEquals("ab", Strings.removeAll("a" + SMILE + "b" + SMILE, 0, SMILE));
    }

    // ---------------------------------------------------------------- C-552: splitOnWhitespace history sentence dropped

    @Test
    public void testC552_splitOnWhitespaceIsTheExplicitApiAndSplitRejectsNullOrEmptyDelimiter() {
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitOnWhitespace(" a \t b\n"));
        assertArrayEquals(new String[] {}, Strings.splitOnWhitespace(null));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b", ""));
    }

    // ---------------------------------------------------------------- C-553: removeStart/EndIgnoreCase same-length rule

    @Test
    public void testC553_removeStartEndIgnoreCaseUseSameLengthRegionMatches() {
        assertEquals("SSa", Strings.removeStartIgnoreCase("SSa", "\u00DF"));
        assertEquals("aSS", Strings.removeEndIgnoreCase("aSS", "\u00DF"));
        assertFalse(Strings.startsWithIgnoreCase("SSa", "\u00DF"));
        assertFalse(Strings.endsWithIgnoreCase("aSS", "\u00DF"));

        // single-char case pairs do match, and exactly removeStr.length() code units are removed
        assertEquals("a", Strings.removeStartIgnoreCase("\u00DFa", "\u1E9E"));
        assertEquals("domain.com", Strings.removeStartIgnoreCase("WwW.domain.com", "www."));
        assertEquals("www.domain", Strings.removeEndIgnoreCase("www.domain.CoM", ".com"));
        assertEquals("x", Strings.removeStartIgnoreCase("\u0130x", "i"));
        assertEquals("", Strings.removeEndIgnoreCase("abc", "ABC"));
        assertNull(Strings.removeStartIgnoreCase(null, "a"));
        assertEquals("", Strings.removeEndIgnoreCase("", "a"));
    }

    // ---------------------------------------------------------------- C-554: splitToLines non-\n/\r terminators

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

    private static String escape(final String str) {
        final StringBuilder sb = new StringBuilder();

        for (final char c : str.toCharArray()) {
            sb.append(String.format("\\u%04X", (int) c));
        }

        return sb.toString();
    }

    // ---------------------------------------------------------------- C-555: strip matches a surrogate stripChars by code point

    @Test
    public void testC555_supplementaryStripCharsNeverSplitsADifferentPair() {
        assertEquals(GRIN, Strings.strip(GRIN, SMILE));
        assertEquals(GRIN + " text " + GRIN, Strings.strip(GRIN + " text " + GRIN, SMILE));
        assertEquals(GRIN, Strings.stripStart(GRIN, SMILE));
        assertEquals(GRIN, Strings.stripEnd(GRIN, SMILE));
        assertEquals("x" + CYCLONE, Strings.stripEnd("x" + CYCLONE, QUINTESSENCE));
        assertEquals(CYCLONE + "x", Strings.stripStart(CYCLONE + "x", QUINTESSENCE));

        // the same code point is still stripped, from either end and repeatedly
        assertEquals("x", Strings.strip(SMILE + "x" + SMILE, SMILE));
        assertEquals("x", Strings.strip(SMILE + SMILE + "x" + GRIN + SMILE, SMILE + GRIN));
        assertEquals("x" + SMILE, Strings.stripStart(SMILE + "x" + SMILE, SMILE));
        assertEquals(SMILE + "x", Strings.stripEnd(SMILE + "x" + SMILE, SMILE));
        assertEquals("", Strings.strip(SMILE + SMILE, SMILE));
        assertEquals("", Strings.stripStart(SMILE, SMILE));
        assertEquals("", Strings.stripEnd(SMILE, SMILE));

        // mixed BMP + supplementary stripChars
        assertEquals("abc", Strings.strip(" " + SMILE + "abc" + SMILE + "x", "x " + SMILE));
        assertEquals(GRIN + "abc", Strings.strip(" " + GRIN + "abc x", "x " + SMILE));

        // Each forms go through the same code
        final String[] array = { GRIN, SMILE + "a" + SMILE, null, "" };
        Strings.stripEach(array, SMILE);
        assertArrayEquals(new String[] { GRIN, "a", null, "" }, array);
        final String[] startArray = { GRIN + "a", SMILE + "a" };
        Strings.stripStartEach(startArray, SMILE);
        assertArrayEquals(new String[] { GRIN + "a", "a" }, startArray);
        final String[] endArray = { "a" + GRIN, "a" + SMILE };
        Strings.stripEndEach(endArray, SMILE);
        assertArrayEquals(new String[] { "a" + GRIN, "a" }, endArray);

        // the documented Commons Lang divergence: StringUtils matches per code unit and splits the pair
        assertEquals("\uDE01", StringUtils.strip(GRIN, SMILE));
        assertEquals("x\uD83C", StringUtils.stripEnd("x" + CYCLONE, QUINTESSENCE));
    }

    @Test
    public void testC555_loneSurrogatesAndUnchangedBmpRules() {
        // a lone surrogate in str is a code unit of its own: a supplementary stripChars does not strip it ...
        assertEquals("\uD83Dx", Strings.strip("\uD83Dx", SMILE));
        assertEquals("x\uDE00", Strings.strip("x\uDE00", SMILE));
        assertEquals("\uDE00x", Strings.stripStart("\uDE00x", SMILE));
        assertEquals("x\uD83D", Strings.stripEnd("x\uD83D", SMILE));
        // ... only the same unpaired surrogate in stripChars does
        assertEquals("x", Strings.strip("\uD83Dx\uD83D", "\uD83D"));
        assertEquals("x", Strings.strip("\uDE00x\uD83D", "\uD83D\uDE01\uDE00a\uD83D"));
        // a lone high surrogate before a pair: the lone one is stripped as a code point of its own, and the pair after it
        // is then read whole, so a stray high surrogate in stripChars does not reach into the pair
        assertEquals(SMILE, Strings.stripStart("\uD83D" + SMILE, "\uD83D"));
        assertEquals(SMILE, Strings.strip("\uD83D" + SMILE + "\uD83D", "\uD83D"));
        // a stripChars holding a pair AND the lone high surrogate strips both
        assertEquals("x", Strings.strip("\uD83D" + SMILE + "x", SMILE + "\uDE01\uD83D"));

        // BMP stripChars: unchanged code-unit behaviour (the documented examples)
        assertEquals("  abc", Strings.strip("  abcyx", "xyz"));
        assertEquals("abc", Strings.strip("yxabcxyz", "xyz"));
        assertEquals("12", Strings.stripEnd("120.00", ".0"));
        assertEquals("abc  ", Strings.stripStart("yxabc  ", "xyz"));

        // null / empty rules unchanged
        assertNull(Strings.strip(null, SMILE));
        assertEquals("", Strings.strip("", SMILE));
        final String str = " " + SMILE + " ";
        assertSame(str, Strings.strip(str, ""));
        assertSame(str, Strings.stripStart(str, ""));
        assertSame(str, Strings.stripEnd(str, ""));
        assertEquals(SMILE, Strings.strip(str, null));
        assertEquals(SMILE + " ", Strings.stripStart(str, null));
        assertEquals(" " + SMILE, Strings.stripEnd(str, null));
        final String unchanged = GRIN + "a";
        assertSame(unchanged, Strings.strip(unchanged, SMILE));
    }

    @Test
    public void testC555_fuzzAgainstCodePointReference() {
        final String[] units = { "a", " ", "\uD83D", "\uDE00", "\uDE01", "\uD83C", SMILE, GRIN, CYCLONE, "\u00E9" };
        final String[] stripSets = { SMILE, GRIN, SMILE + " ", "\uD83D", "\uDE00", "a\uD83D\uDE00\uDE01", CYCLONE + QUINTESSENCE, "a ", " \u00E9",
                "\uD83D" + SMILE };
        final Random random = new Random(555L);

        for (int iteration = 0; iteration < 30_000; iteration++) {
            final StringBuilder sb = new StringBuilder();
            final int count = random.nextInt(7);

            for (int k = 0; k < count; k++) {
                sb.append(units[random.nextInt(units.length)]);
            }

            final String str = sb.toString();
            final String stripChars = stripSets[random.nextInt(stripSets.length)];

            assertEquals(referenceStrip(str, stripChars, true, true), Strings.strip(str, stripChars), escape(str) + " / " + escape(stripChars));
            assertEquals(referenceStrip(str, stripChars, true, false), Strings.stripStart(str, stripChars), escape(str) + " / " + escape(stripChars));
            assertEquals(referenceStrip(str, stripChars, false, true), Strings.stripEnd(str, stripChars), escape(str) + " / " + escape(stripChars));
        }
    }

    /** Documented semantics: code-unit matching for a surrogate-free stripChars, whole-code-point matching otherwise. */
    private static String referenceStrip(final String str, final String stripChars, final boolean fromStart, final boolean fromEnd) {
        boolean hasSurrogate = false;

        for (int i = 0; i < stripChars.length(); i++) {
            hasSurrogate |= Character.isSurrogate(stripChars.charAt(i));
        }

        final int[] strUnits = hasSurrogate ? str.codePoints().toArray() : str.chars().toArray();
        final List<Integer> set = new ArrayList<>();

        for (final int value : hasSurrogate ? stripChars.codePoints().toArray() : stripChars.chars().toArray()) {
            set.add(value);
        }

        int begin = 0;
        int end = strUnits.length;

        while (fromStart && begin < end && set.contains(strUnits[begin])) {
            begin++;
        }

        while (fromEnd && end > begin && set.contains(strUnits[end - 1])) {
            end--;
        }

        final StringBuilder sb = new StringBuilder();

        for (int i = begin; i < end; i++) {
            sb.appendCodePoint(strUnits[i]);
        }

        return sb.toString();
    }

    // ---------------------------------------------------------------- C-556: chop of a single supplementary code point

    @Test
    public void testC556_chopOfASingleCodePointIsEmpty() {
        assertEquals("", Strings.chop(SMILE));
        assertEquals("", Strings.chop("a"));
        assertEquals("", Strings.chop("\r\n"));
        assertEquals("", Strings.chop(""));
        assertNull(Strings.chop(null));
        assertEquals(SMILE, Strings.chop(SMILE + GRIN));
        assertEquals("\r", Strings.chop("\r\r\n"));
    }

    // ---------------------------------------------------------------- C-557: literal characters in the stripAccents docs

    @Test
    public void testC557_stripAccentsDocumentedExamplesStillHold() {
        assertEquals("a \u2260 b", Strings.stripAccents("a \u2260 b"));
        assertEquals("Lodz", Strings.stripAccents("\u0141\u00F3d\u017A"));
        assertEquals("\u0110\u0111\u0126\u0127\u0166\u0167\u00D8\u00F8", Strings.stripAccents("\u0110\u0111\u0126\u0127\u0166\u0167\u00D8\u00F8"));
        assertTrue(Strings.isMixedCase("\u01C5a"));
        assertFalse(StringUtils.isMixedCase("\u01C5a"));
    }

    // ---------------------------------------------------------------- C-558: past-the-end start, forward vs backward families

    @Test
    public void testC558_pastTheEndStartForwardIsMinusOneBackwardClamps() {
        assertEquals(-1, Strings.indexOfAny("abc", 4, ""));
        assertEquals(-1, Strings.minIndexOfAll("abc", 4, ""));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 4, ""));
        assertEquals(-1, Strings.indexOfAny("abc", Integer.MAX_VALUE, "", "a"));

        assertEquals(3, Strings.lastIndexOfAny("abc", 100, ""));
        assertEquals(3, Strings.minLastIndexOfAll("abc", 100, ""));
        assertEquals(3, Strings.maxLastIndexOfAll("abc", 100, ""));

        // the single-needle families clamp in both directions
        assertEquals(3, Strings.indexOf("abc", "", 4));
        assertEquals(3, Strings.indexOfIgnoreCase("abc", "", 4));
        assertEquals(3, Strings.lastIndexOf("abc", "", 100));
        assertEquals(3, Strings.lastIndexOfIgnoreCase("abc", "", 100));

        // boundary: a start exactly at the length is in range for both
        assertEquals(3, Strings.indexOfAny("abc", 3, ""));
        assertEquals(3, Strings.lastIndexOfAny("abc", 3, ""));
        // empty haystack
        assertEquals(0, Strings.indexOfAny("", 0, ""));
        assertEquals(-1, Strings.indexOfAny("", 1, ""));
        assertEquals(0, Strings.lastIndexOfAny("", 5, ""));
    }

    // ---------------------------------------------------------------- C-559: containsNone/containsOnly surrogate divergence

    @Test
    public void testC559_containsNoneAndContainsOnlyDivergeFromCommonsOnSurrogates() {
        assertFalse(Strings.containsNone("\uD83D", '\uD83D', 'x'));
        assertTrue(StringUtils.containsNone("\uD83D", '\uD83D', 'x'));

        assertTrue(Strings.containsOnly("\uD83D", '\uD83D', '\uDE00', 'x'));
        assertFalse(StringUtils.containsOnly("\uD83D", '\uD83D', '\uDE00', 'x'));

        // no divergence without surrogates
        assertEquals(StringUtils.containsNone("hello", 'x', 'y'), Strings.containsNone("hello", 'x', 'y'));
        // "can differ": Commons reports the lone high surrogate when it is the LAST candidate, as this method does
        assertFalse(Strings.containsNone("\uD83D", 'x', '\uD83D'));
        assertFalse(StringUtils.containsNone("\uD83D", 'x', '\uD83D'));
        assertEquals(StringUtils.containsOnly("abab", 'a', 'b'), Strings.containsOnly("abab", 'a', 'b'));
        // a full pair supplied as both surrogates
        assertTrue(Strings.containsOnly(SMILE, '\uD83D', '\uDE00'));
        assertFalse(Strings.containsNone(SMILE, '\uDE00'));
    }

    // ---------------------------------------------------------------- C-560: substringBefore(String, char) on ""

    @Test
    public void testC560_substringBeforeCharEmptyInputIsNull() {
        assertNull(Strings.substringBefore("", '@'));
        assertNull(Strings.substringBefore(null, '@'));
        assertEquals("", Strings.substringBefore("@leading", '@'));
        assertEquals("user", Strings.substringBefore("user@example.com", '@'));
        // the three sibling char overloads agree on ""
        assertNull(Strings.substringAfter("", '@'));
        assertNull(Strings.substringAfterLast("", '@'));
        assertNull(Strings.substringBeforeLast("", '@'));
    }

    // ---------------------------------------------------------------- C-561: index/operator substringBetween on ""

    @Test
    public void testC561_substringBetweenIndexAndOperatorFormsOnEmptyInput() {
        assertEquals("", Strings.substringBetween("", -1, 0));
        assertEquals("", Strings.substringBetween("", -1, i -> 0));
        assertEquals("", Strings.substringBetween("", i -> -1, 0));
        assertEquals("", Strings.substringBetween("", "", ""));
        assertNull(Strings.substringBetween("", "[", "]"));
        // a non-negative end is capped at the length, so any end >= 0 behaves like 0 on ""
        assertEquals("", Strings.substringBetween("", -1, 5));
        assertEquals("", Strings.substringBetween("", -1, i -> 1));
        // a begin index of 0 is already past the end of ""; a negative end is invalid
        assertNull(Strings.substringBetween("", 0, 0));
        assertNull(Strings.substringBetween("", i -> 0, 0));
        assertNull(Strings.substringBetween("", -1, -1));
        assertNull(Strings.substringBetween(null, -1, 0));
    }

    // ---------------------------------------------------------------- C-562: null mode checked for null / "" input

    @Test
    public void testC562_nullDelimiterMatchModeRejectedForNullAndEmptyInput() {
        for (final String str : new String[] { null, "" }) {
            assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween(str, '[', ']', null));
            assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween(str, "[", "]", null));
            assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween(str, '[', ']', null));
            assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween(str, "[", "]", null));
        }

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween(null, "[", "]", null)).getMessage()
                .contains("delimiterMatchMode"));
    }

    // ---------------------------------------------------------------- C-564: joinEntries 5-arg forwarder

    @Test
    public void testC564_joinEntriesForwarderStillValidatesExtractorsCollaboratorFirst() {
        final List<Map.Entry<String, Integer>> entries = Arrays.asList(Map.entry("a", 1), Map.entry("b", 2));
        final List<Map.Entry<String, Integer>> nullEntries = null;
        final List<Map.Entry<String, Integer>> noEntries = Collections.emptyList();
        final Function<Map.Entry<String, Integer>, String> key = Map.Entry::getKey;
        final Function<Map.Entry<String, Integer>, Integer> value = Map.Entry::getValue;

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.joinEntries(nullEntries, ",", "=", null, value)).getMessage()
                .contains("keyExtractor"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.joinEntries(noEntries, ",", "=", key, null)).getMessage()
                .contains("valueExtractor"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.joinEntries(entries, ",", "=", null, null)).getMessage()
                .contains("keyExtractor"));
        assertEquals("a=1, b=2", Strings.joinEntries(entries, ", ", "=", key, value));
        assertEquals("", Strings.joinEntries(nullEntries, ", ", "=", key, value));
        assertEquals("", Strings.joinEntries(noEntries, ", ", "=", key, value));
        assertEquals("a1b2", Strings.joinEntries(entries, null, "", key, value));
    }

    // ---------------------------------------------------------------- C-566: concat contract (null renders as "null")

    @Test
    public void testC566_concatContract() {
        assertEquals("nullnull", Strings.concat((Object) null, (Object) null));
        assertEquals("[1, 2]x", Strings.concat(new int[] { 1, 2 }, "x"));
        assertEquals("b", Strings.concatNullToEmpty(null, "b"));
        assertEquals("", Strings.concatNullToEmpty(null, null));
        assertEquals("ab", Strings.concatNullToEmpty("a", "b"));
    }

    // ---------------------------------------------------------------- C-567: non-List range join + lenientFormat recovery

    @Test
    public void testC567_joinCollectionRangeOnNonListCollections() {
        final LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        assertEquals("b,c", Strings.join(set, 1, 3, ","));
        assertEquals("d", Strings.join(set, 3, 4, ","));
        assertEquals("", Strings.join(set, 2, 2, ","));
        assertEquals("[b, c, d]", Strings.join(set, 1, 4, ", ", "[", "]"));
        assertEquals("b|c", Strings.join(new LinkedHashSet<>(Arrays.asList(" a ", " b ", " c ")), 1, 3, "|", true));

        final ArrayDeque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals("3-4-5", Strings.join(deque, 2, 5, "-"));
        assertEquals("1", Strings.join(deque, 0, 1, "-"));

        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(set, 3, 5, ","));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(set, -1, 2, ","));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(set, 3, 2, ","));
    }

    @Test
    public void testC567_lenientFormatRecoversFromAThrowingToString() {
        final Object throwing = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        };

        final String result = Strings.lenientFormat("v=%s!", throwing);
        final String expectedPrefix = "v=<" + throwing.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(throwing));

        assertTrue(result.startsWith(expectedPrefix), result);
        assertTrue(result.endsWith(" threw java.lang.IllegalStateException>!"), result);

        // a surplus throwing argument is recovered the same way
        final String surplus = Strings.lenientFormat("x", throwing);
        assertTrue(surplus.startsWith("x: [<"), surplus);
        assertTrue(surplus.endsWith(" threw java.lang.IllegalStateException>]"), surplus);
    }

    // ---------------------------------------------------------------- C-569: email finder absorbs label characters

    @Test
    public void testC569_emailFinderExtendsOverLabelCharactersAndSkipsOthers() {
        assertEquals("a@b.com-x", Strings.findFirstEmailAddress("a@b.com-x"));
        assertEquals("a@b.com9", Strings.findFirstEmailAddress("a@b.com9"));
        assertEquals(Arrays.asList("a@b.com-x", "a@b.com9"), Strings.findAllEmailAddresses("a@b.com-x and a@b.com9"));

        assertNull(Strings.findFirstEmailAddress("a@b.com_x"));
        assertNull(Strings.findFirstEmailAddress("a@b.com-"));
        assertNull(Strings.findFirstEmailAddress("a@b.com\u0301"));
        assertTrue(Strings.findAllEmailAddresses("x a@b.com- y").isEmpty());
        assertNull(Strings.findFirstEmailAddress("m\u00FCller@firma.de"));
        assertEquals("a@b.com", Strings.findFirstEmailAddress("mail a@b.com, thanks"));

        // a letter or digit that cannot extend the last label (non-ASCII, or after a bracketed literal) is skipped
        assertNull(Strings.findFirstEmailAddress("a@b.com\u00E9"));
        assertNull(Strings.findFirstEmailAddress("a@[1.2.3.4]x"));
        assertTrue(Strings.findAllEmailAddresses("x a@b.com\u0661 y").isEmpty());
        assertEquals("a@[1.2.3.4]", Strings.findFirstEmailAddress("a@[1.2.3.4] x"));
    }

    // ---------------------------------------------------------------- C-571: base64UrlEncodeString uses UTF-8

    @Test
    public void testC571_base64UrlEncodeStringUsesUtf8Unpadded() {
        final String input = "\u00E9?>" + SMILE;
        assertEquals(Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8)), Strings.base64UrlEncodeString(input));
        assertEquals("", Strings.base64UrlEncodeString(""));
        assertEquals("", Strings.base64UrlEncodeString(null));
    }

    // ---------------------------------------------------------------- C-572: containsToken is a one-line delegate

    @Test
    public void testC572_containsTokenDelegatesValidationAndNullRules() {
        for (final String delimiter : new String[] { null, "" }) {
            for (final String str : new String[] { null, "", "a,b" }) {
                for (final String token : new String[] { null, "", "b" }) {
                    assertEquals("'delimiter' cannot be null or empty",
                            assertThrows(IllegalArgumentException.class, () -> StrUtil.containsToken(str, token, delimiter)).getMessage());
                    assertEquals("'delimiter' cannot be null or empty",
                            assertThrows(IllegalArgumentException.class, () -> StrUtil.containsTokenIgnoreCase(str, token, delimiter)).getMessage());
                }
            }
        }

        assertFalse(StrUtil.containsToken(null, "a", ","));
        assertFalse(StrUtil.containsToken("a", null, ","));
        assertFalse(StrUtil.containsTokenIgnoreCase(null, "a", ","));
        assertFalse(StrUtil.containsTokenIgnoreCase("a", null, ","));
        assertTrue(StrUtil.containsToken("apple,banana", "banana", ","));
        assertFalse(StrUtil.containsToken("apple,banana", "BANANA", ","));
        assertTrue(StrUtil.containsTokenIgnoreCase("apple,banana", "BANANA", ","));
        assertTrue(StrUtil.containsTokenIgnoreCase("xANDy", "y", "and"));
        assertTrue(StrUtil.containsToken("", "", ","));
        assertTrue(StrUtil.containsToken(",a", "", ","));
        assertFalse(StrUtil.containsToken("a,b", "", ","));
        assertTrue(StrUtil.containsToken(SMILE + "," + GRIN, GRIN, ","));
    }

    // ---------------------------------------------------------------- C-547: quoted local part whitespace must be a quoted pair

    @Test
    public void testC547_quotedLocalPartWhitespaceMustBeAQuotedPair() {
        assertTrue(Strings.isValidEmailAddress("\"a\\ b\"@x.y"));
        assertTrue(Strings.isValidEmailAddress("\"a\\\tb\"@x.y"));
        assertFalse(Strings.isValidEmailAddress("\"a b\"@x.y"));
        assertFalse(Strings.isValidEmailAddress("\"a\tb\"@x.y"));
        assertFalse(Strings.isValidEmailAddress("\" \"@example.org"));
        assertTrue(Strings.isValidEmailAddress("\"quoted.user\"@example.com"));
        assertTrue(Strings.isValidEmailAddress("\"a@b\"@x.y"));
    }

    // ---------------------------------------------------------------- C-549: swapCase vs the case converters

    @Test
    public void testC549_caseConvertersAgreeWithSwapCaseAtSeparatorsButNotInsideAWord() {
        final String omicronSigma = "\u039F" + CAPITAL_SIGMA;

        // the documented example: toLowerCase differs at a hyphen
        assertEquals("\u03BF" + FINAL_SIGMA + "-\u03B1", Strings.swapCase(omicronSigma + "-\u0391"));
        assertEquals("\u03BF" + SMALL_SIGMA + "-\u03B1", Strings.toLowerCase(omicronSigma + "-\u0391"));

        // the case converters split first and agree with swapCase at '_', '-' and whitespace
        assertEquals("\u03BF" + FINAL_SIGMA + "_\u03B1", Strings.toSnakeCase(omicronSigma + "-\u0391"));
        assertEquals("\u03BF" + FINAL_SIGMA + "-\u03B1", Strings.toKebabCase(omicronSigma + "-\u0391"));
        assertEquals("\u03BF" + FINAL_SIGMA + "\u0391", Strings.toCamelCase(omicronSigma + "-\u0391"));
        assertEquals("\u03BF" + FINAL_SIGMA + "_\u03B1", Strings.swapCase(omicronSigma + "_\u0391"));
        assertEquals("\u03BF" + FINAL_SIGMA + "_\u03B1", Strings.toSnakeCase(omicronSigma + " \u0391"));

        // ... and differ inside a word (a digit is neither cased nor case-ignorable)
        assertEquals("\u03BF" + FINAL_SIGMA + "2\u0391", Strings.swapCase(omicronSigma + "2\u03B1"));
        assertEquals("\u03BF" + SMALL_SIGMA + "2\u03B1", Strings.toSnakeCase(omicronSigma + "2\u03B1"));
        assertEquals("\u03BF" + SMALL_SIGMA + "2\u03B1", Strings.toCamelCase(omicronSigma + "2\u03B1"));
    }

    // ---------------------------------------------------------------- C-568: spacing components take their own cell

    @Test
    public void testC568_spacingComponentsAddACell() {
        for (final DisplayWidthPolicy policy : new DisplayWidthPolicy[] { DisplayWidthPolicy.DEFAULT, DisplayWidthPolicy.CJK }) {
            // halfwidth katakana + voiced / semi-voiced sound marks
            assertEquals(2, Strings.displayWidth("\uFF76\uFF9E", policy));
            assertEquals(2, Strings.displayWidth("\uFF8A\uFF9F", policy));
            assertEquals(4, Strings.displayWidth("\uFF76\uFF9E\uFF77\uFF9E", policy));
            assertEquals(3, Strings.displayWidth("\uFF76\uFF9E\uFF9E", policy));
            assertEquals(1, Strings.displayWidth("\uFF9E", policy));
            assertEquals(2, Strings.displayWidth(" \uFF9E", policy));

            // spacing vowel signs (Mc, ccc 0) and Thai / Lao AM after a visible base
            assertEquals(2, Strings.displayWidth("\u0E01\u0E33", policy)); // Thai KO KAI + SARA AM
            assertEquals(2, Strings.displayWidth("\u0E01\u0E48\u0E33", policy));
            assertEquals(2, Strings.displayWidth("\u0E81\u0EB3", policy)); // Lao KO + AM
            assertEquals(2, Strings.displayWidth("\u0915\u093E", policy)); // Devanagari KA + AA
            assertEquals(2, Strings.displayWidth("\u0915\u093F", policy)); // KA + I (drawn on the left, still a cell)
            assertEquals(2, Strings.displayWidth("\u0915\u093E\u0903", policy)); // at most one extra cell per cluster
            assertEquals(2, Strings.displayWidth("\u1780\u17B6", policy)); // Khmer KA + AA
            assertEquals(2, Strings.displayWidth("\u1000\u102C", policy)); // Myanmar KA + AA (two clusters by UAX #29)
            assertEquals(4, Strings.displayWidth("\u0915\u093E\u0916\u093F", policy));

            // never a third cell after a wide base
            assertEquals(2, Strings.displayWidth("\u4E2D\u093E", policy));
            assertEquals(2, Strings.displayWidth("\uAC00\u093E", policy));

            // unchanged: no visible base, Prepend, conjuncts, non-spacing marks
            assertEquals(1, Strings.displayWidth("\u093E", policy));
            assertEquals(1, Strings.displayWidth("\u0E33", policy));
            assertEquals(1, Strings.displayWidth("\u0D4Ea", policy)); // MALAYALAM LETTER DOT REPH (Prepend) + a
            assertEquals(1, Strings.displayWidth("\u0600" + "1", policy)); // Cf Prepend
            assertEquals(1, Strings.displayWidth("\u0915\u094D\u0937", policy)); // conjunct KSSA stays its base width
            assertEquals(2, Strings.displayWidth("\u0915\u094D\u0937\u093E", policy));
            assertEquals(1, Strings.displayWidth("e\u0301", policy));
            assertEquals(1, Strings.displayWidth("A\u3099", policy));
            assertEquals(1, Strings.displayWidth("\u1B13\u1B44", policy)); // spacing virama (ccc 9) adds no cell
            assertEquals(2, Strings.displayWidth("\uAC00\u302E", policy)); // Hangul tone mark (Mc, ccc 224, wide)

            // canonically fused spacing marks fold into their letter
            assertEquals(1, Strings.displayWidth("\u1B06", policy));
            assertEquals(1, Strings.displayWidth("\u1B05\u1B35", policy));
            // after a non-spacing vowel sign the same mark composes into a spacing vowel sign and adds the cell
            assertEquals(2, Strings.displayWidth("k\u1B3B", policy));
            assertEquals(2, Strings.displayWidth("k\u1B3A\u1B35", policy));
            assertEquals(2, Strings.displayWidth("\u0B95\u0BD7", policy)); // U+0BD7 after a consonant it does not fuse with
            assertEquals(1, Strings.displayWidth("\u0B94", policy));
            assertEquals(1, Strings.displayWidth("\u0B92\u0BD7", policy));
            assertEquals(1, Strings.displayWidth("\u0BCC", policy));
            assertEquals(1, Strings.displayWidth("\u0BC6\u0BD7", policy));
            assertEquals(2, Strings.displayWidth("\u0B95\u0BCC", policy));
            assertEquals(2, Strings.displayWidth("\u0B95\u0BC6\u0BD7", policy));
            assertEquals(2, Strings.displayWidth("\u0995\u09CB", policy));
            assertEquals(2, Strings.displayWidth("\u0995\u09C7\u09BE", policy));
            assertEquals(Strings.displayWidth(cp(0x1138E), policy), Strings.displayWidth(cp(0x1138B) + cp(0x113C2), policy));
            assertEquals(Strings.displayWidth(cp(0x1D15F), policy), Strings.displayWidth(cp(0x1D158) + cp(0x1D165), policy));

            // emoji / flag / Hangul clusters unchanged
            assertEquals(2, Strings.displayWidth("\uD83D\uDC69\u200D\uD83D\uDCBB", policy));
            assertEquals(2, Strings.displayWidth("\uD83C\uDDFA\uD83C\uDDF8", policy));
            assertEquals(2, Strings.displayWidth("1\uFE0F\u20E3", policy));
            assertEquals(2, Strings.displayWidth("\uD83D\uDC4D\uD83C\uDFFD", policy));
            assertEquals(2, Strings.displayWidth("\uD55C", policy));
            assertEquals(2, Strings.displayWidth("\u1112\u1161\u11AB", policy));
            assertEquals(2, Strings.displayWidth("\u1100\uAC00", policy));
        }

        // the pad helpers now see the right width
        assertEquals("\uFF76\uFF9E  ", Strings.padEndToDisplayWidth("\uFF76\uFF9E", 4));
        assertEquals("  \uFF76\uFF9E", Strings.padStartToDisplayWidth("\uFF76\uFF9E", 4));
        assertEquals("\u0E01\u0E33  ", Strings.padEndToDisplayWidth("\u0E01\u0E33", 4));
        assertEquals("\u0915\u093E", Strings.padEndToDisplayWidth("\u0915\u093E", 2));
        assertEquals(4, Strings.displayWidth(Strings.padStartToDisplayWidth("\u093E", 4)));
    }

    @Test
    public void testC568_canonicallyEquivalentSpellingsMeasureTheSame() {
        final String[] prefixes = { "", "k", "\u0915", "\u0B95" };
        int checked = 0;

        for (int codePoint = 0; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (Character.getType(codePoint) == Character.SURROGATE || !Character.isDefined(codePoint)) {
                continue;
            }

            final String str = cp(codePoint);
            final String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);

            if (nfd.equals(str)) {
                continue;
            }

            final String nfc = Normalizer.normalize(str, Normalizer.Form.NFC);

            // DEFAULT policy only: under CJK an East Asian Ambiguous precomposed letter (U+00E0 = 2) already differs from
            // its narrow base + zero-width mark decomposition (1), independently of the C-568 cluster rule.
            for (final String prefix : prefixes) {
                assertEquals(Strings.displayWidth(prefix + nfc), Strings.displayWidth(prefix + nfd),
                        () -> String.format("U+%04X with prefix %s", codePoint0(nfc), escape(prefix)));
            }

            checked++;
        }

        assertTrue(checked > 2000, "decomposable code points checked: " + checked);

        // random NFC / NFD pairs over Indic, Thai, kana and Hangul material
        final String[] pool = { "\u0915", "\u093E", "\u094D", "\u0937", "\u0995", "\u09CB", "\u09CC", "\u0B92", "\u0B94", "\u0BCA", "\u0BCC", "\u0C95",
                "\u0CC7", "\u0CCB", "\u0D15", "\u0D4A", "\u0D4C", "\u0DDA", "\u0DDD", "\u0E01", "\u0E33", "\u1B05", "\u1B06", "\u1B3B", "\uD55C", "\u304B",
                "\u3099", "\u304C", "\uFF76", "\uFF9E", "a", "\u00E9", "\u0301", cp(0x1138E), cp(0x113C5), cp(0x11347), cp(0x1134B), "\u1B3A", "\u1B35", "\u1B3B", "\u1B11", "\u0BD7", "k" };
        final Random random = new Random(568L);

        for (int iteration = 0; iteration < 50_000; iteration++) {
            final StringBuilder sb = new StringBuilder();
            final int count = 1 + random.nextInt(5);

            for (int k = 0; k < count; k++) {
                sb.append(pool[random.nextInt(pool.length)]);
            }

            final String nfc = Normalizer.normalize(sb, Normalizer.Form.NFC);
            final String nfd = Normalizer.normalize(sb, Normalizer.Form.NFD);
            assertEquals(Strings.displayWidth(nfc), Strings.displayWidth(nfd), () -> escape(nfc) + " vs " + escape(nfd));
        }
    }

    @Test
    public void testC568_clusterWidthDoesNotDependOnPartOrderAndPrependIsNeverTheBase() {
        // R4-01: a base-less spacing mark and a halfwidth sound mark both count, in either order
        assertEquals(1, Strings.displayWidth("\u093E"));
        assertEquals(2, Strings.displayWidth("\u093E\uFF9E"));
        assertEquals(2, Strings.displayWidth("\uFF9E\u093E"));
        assertEquals(2, Strings.displayWidth("\uFF9E\uFF9E"));
        assertEquals(3, Strings.displayWidth("\u093E\uFF9E\uFF9F"));
        assertEquals(1, Strings.displayWidth("\uFF9E"));

        // R4-02: a visible Prepend is drawn on the consonant that follows - never the base, nothing attaches to it
        assertEquals(1, Strings.displayWidth("\u0D4E\u093E"));
        assertEquals(1, Strings.displayWidth("\u0D4E\uFF9E"));
        assertEquals(1, Strings.displayWidth("\u0D4E"));
        assertEquals(1, Strings.displayWidth("\u0D4Ea"));
        assertEquals(2, Strings.displayWidth("\u0D4E\u0D15\u0D3E"));
        assertEquals(1, Strings.displayWidth("\u0600\u093E"));

        // the non-zero-class and canonically fused spacing marks add no cell (R4-03 javadoc)
        assertEquals(1, Strings.displayWidth("\u1B24\u1B44"));
        assertEquals(1, Strings.displayWidth("\u0B92\u0BD7"));
        assertEquals(Strings.displayWidth(cp(0x11383)), Strings.displayWidth(cp(0x11382) + cp(0x113C9)));
        assertEquals(Strings.displayWidth(cp(0x11391)), Strings.displayWidth(cp(0x11390) + cp(0x113C9)));
        assertEquals(1, Strings.displayWidth(cp(0x11382) + cp(0x113C9)));

        // an attached sound mark after a wide emoji follows the emoji policy of its base
        final String grinningSoundMark = SMILE + "\uFF9E";
        assertEquals(3, Strings.displayWidth(grinningSoundMark));
        assertEquals(2, Strings.displayWidth(grinningSoundMark, new DisplayWidthPolicy(1, 1)));
    }

    @Test
    public void testC568_padToDisplayWidthIsExactAroundSpacingComponents() {
        for (int width = 0; width <= 5; width++) {
            for (final String str : new String[] { "\u093E\uFF9E", "\uFF9E", "\u093E", "\u0D4E", "\u0D4E\u093E", "\u0E33", "\u302E", "\uD83C\uDFFB",
                    "\u1B35\uFF9E\uAC00", "k\u093E" }) {
                final int expected = Math.max(width, Strings.displayWidth(str));
                assertEquals(expected, Strings.displayWidth(Strings.padStartToDisplayWidth(str, width)), escape(str) + " start " + width);
                assertEquals(expected, Strings.displayWidth(Strings.padEndToDisplayWidth(str, width)), escape(str) + " end " + width);
            }
        }

        // seeded fuzz over the code points whose clusters can absorb or add a column: the padded result is always
        // exactly the requested width (or the input's own width when that is larger)
        final int[] pool = { 'a', ' ', 0x0915, 0x093E, 0x094D, 0x0937, 0x0E01, 0x0E33, 0x0EB3, 0x0B92, 0x0BD7, 0x1B05, 0x1B35, 0x1B44, 0x302E, 0x0D4E,
                0x0600, 0xFF76, 0xFF9E, 0xFF9F, 0x0301, 0x200D, 0xFE0F, 0x2764, 0x1F600, 0x1F3FB, 0x4E2D, 0x1100, 0x1161, 0x11A8, 0x16FF0 };
        final Random random = new Random(5680L);

        for (int iteration = 0; iteration < 20_000; iteration++) {
            final StringBuilder sb = new StringBuilder();
            final int count = 1 + random.nextInt(5);

            for (int k = 0; k < count; k++) {
                sb.appendCodePoint(pool[random.nextInt(pool.length)]);
            }

            final String str = sb.toString();
            final int width = random.nextInt(9);
            final int expected = Math.max(width, Strings.displayWidth(str));
            assertEquals(expected, Strings.displayWidth(Strings.padStartToDisplayWidth(str, width)), escape(str) + " start " + width);
            assertEquals(expected, Strings.displayWidth(Strings.padEndToDisplayWidth(str, width)), escape(str) + " end " + width);
        }
    }

    private static String cp(final int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    private static int codePoint0(final String str) {
        return str.codePointAt(0);
    }

    // ---------------------------------------------------------------- C-573: token spanning the delimiter

    @Test
    public void testC573_tokenContainingTheDelimiterNeverMatches() {
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfToken("a,b", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("x,A,B,y", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("A,B", "a,b", ","));
        assertEquals(2, StrUtil.indexOfToken("x,a,b,y", "a", ","));
    }
}
