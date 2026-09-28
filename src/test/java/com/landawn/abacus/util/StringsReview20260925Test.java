package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Doc-trace tests for the 2026-09-25 fix pass on {@link Strings} (findings U20-01, U20-03, U20-04, U20-05, U20-08 of the
 * line-by-line review of the 2026-09-24 changes). Every example that the corrected javadoc and class-level tables state is
 * executed here; for a cross-library claim both sides are asserted, so a Commons Lang upgrade that removes a divergence
 * shows up as a stale table cell.
 */
public class StringsReview20260925Test extends TestBase {

    private static final String EMOJI = "\uD83D\uDE00";

    private static boolean hasLoneSurrogate(final String s) {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);

            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= s.length() || !Character.isLowSurrogate(s.charAt(i + 1))) {
                    return true;
                }

                i++;
            } else if (Character.isLowSurrogate(c)) {
                return true;
            }
        }

        return false;
    }

    // ---------------------------------------------------------------- U20-01 (L12/C-027): Commons divergences in the class tables

    @Test
    public void testU2001_indexOfDifferenceNullVsEmptyDivergesFromCommons() {
        assertEquals(-1, Strings.indexOfDifference((String) null, ""));
        assertEquals(-1, Strings.indexOfDifference("", (String) null));
        assertEquals(-1, Strings.indexOfDifference((String) null, (String) null));
        assertEquals(-1, Strings.indexOfDifference("", ""));
        assertEquals(0, Strings.indexOfDifference((String) null, "a"));
        assertEquals(0, Strings.indexOfDifference("a", ""));

        // the table's warning describes Commons Lang: 0 when exactly one side is null, -1 only for two nulls
        assertEquals(0, StringUtils.indexOfDifference(null, ""));
        assertEquals(0, StringUtils.indexOfDifference("", null));
        assertEquals(-1, StringUtils.indexOfDifference(null, null));
        assertEquals(-1, StringUtils.indexOfDifference("", ""));
    }

    @Test
    public void testU2001_startsWithAnyAndEndsWithAnyOnEmptyHaystackDivergeFromCommons() {
        assertTrue(Strings.startsWithAny("", ""));
        assertTrue(Strings.endsWithAny("", ""));
        assertTrue(Strings.startsWithAny("", "x", ""));
        assertTrue(Strings.endsWithAny("", "x", ""));
        assertFalse(Strings.startsWithAny("", "x"));
        assertFalse(Strings.endsWithAny("", "x"));
        assertFalse(Strings.startsWithAny(null, ""));
        assertFalse(Strings.endsWithAny(null, ""));

        // Commons Lang returns false for an empty haystack even when a search string is ""
        assertFalse(StringUtils.startsWithAny("", ""));
        assertFalse(StringUtils.endsWithAny("", ""));
        assertFalse(StringUtils.startsWithAny("", "x", ""));

        // the single-value forms agree on both sides
        assertTrue(Strings.startsWith("", ""));
        assertTrue(Strings.endsWith("", ""));
        assertTrue(StringUtils.startsWith("", ""));
        assertTrue(StringUtils.endsWith("", ""));
        assertTrue(Strings.startsWithAny("a", ""));
        assertTrue(StringUtils.startsWithAny("a", ""));
    }

    @Test
    public void testU2001_containsOnlyEmptyStringWithNullSetDivergesFromCommons() {
        assertTrue(Strings.containsOnly("", (char[]) null));
        assertTrue(Strings.containsOnly("", new char[0]));
        assertTrue(Strings.containsOnly("", 'a'));
        assertFalse(Strings.containsOnly(null, 'a'));
        assertFalse(Strings.containsOnly("a", (char[]) null));

        // Commons Lang: a null set is false before the empty-input rule is applied
        assertFalse(StringUtils.containsOnly("", (char[]) null));
        assertTrue(StringUtils.containsOnly("", new char[0]));
        assertTrue(StringUtils.containsOnly("", 'a'));
        assertFalse(StringUtils.containsOnly(null, 'a'));
    }

    @Test
    public void testU2001_substringOutOfRangeBeginDivergesFromCommons() {
        assertNull(Strings.substring("Hello", 10));
        assertNull(Strings.substring("", 1));
        assertNull(Strings.substring("Hello", 3, 2));
        assertNull(Strings.substring("Hello", 6, 9));
        assertEquals("", Strings.substring("Hello", 5));
        assertEquals("", Strings.substring("", 0));
        assertEquals("llo", Strings.substring("Hello", 2, 9));

        // Commons Lang returns "" instead of null for begin > length and for begin > end
        assertEquals("", StringUtils.substring("Hello", 10));
        assertEquals("", StringUtils.substring("", 1));
        assertEquals("", StringUtils.substring("Hello", 3, 2));
        assertEquals("", StringUtils.substring("Hello", 6, 9));
        assertEquals("llo", StringUtils.substring("Hello", 2, 9));
    }

    // ---------------------------------------------------------------- U20-03 (L12/C-011): abbreviateMiddle slack rule

    @Test
    public void testU2003_abbreviateMiddleDocumentedSurrogateExamples() {
        // the head cut moves before the pair; the tail takes the freed unit
        assertEquals("a.cd", Strings.abbreviateMiddle("a" + EMOJI + "bcd", ".", 4));
        // the tail cut moves past the pair; the head takes the freed unit
        assertEquals("ab.", Strings.abbreviateMiddle("abcd" + EMOJI, ".", 3));
        // BMP inputs are unaffected
        assertEquals("ab.f", Strings.abbreviateMiddle("abcdef", ".", 4));
        assertEquals("abc...xyz", Strings.abbreviateMiddle("abcdefghijklmnopqrstuvwxyz", "...", 9));
    }

    @Test
    public void testU2003_abbreviateMiddleIsAtMostOneUnitShortAndNeverSplitsAPair() {
        final String s = "ab" + EMOJI + "cd" + EMOJI + "ef" + EMOJI + "gh";

        for (int maxLength = 3; maxLength < s.length(); maxLength++) {
            final String r = Strings.abbreviateMiddle(s, ".", maxLength);

            assertTrue(r.length() <= maxLength, maxLength + " -> " + r);
            assertTrue(r.length() >= maxLength - 1, maxLength + " -> " + r);
            assertFalse(hasLoneSurrogate(r), maxLength + " -> " + r);
            assertTrue(s.startsWith(r.substring(0, r.indexOf('.'))), maxLength + " -> " + r);
            assertTrue(s.endsWith(r.substring(r.indexOf('.') + 1)), maxLength + " -> " + r);
        }

        assertSame(s, Strings.abbreviateMiddle(s, ".", s.length()));
    }

    // ---------------------------------------------------------------- U20-04 (L12/C-005): abbreviate offset sentence, surrogate corners

    @Test
    public void testU2004_abbreviateOffsetSurrogateCorners() {
        final String s = "abcde" + EMOJI + "fghij";

        // the pair at the effective offset does not fit in the single code unit between the markers
        assertEquals("......", Strings.abbreviate(s, 5, 7));
        assertEquals("......", Strings.abbreviate(s, "...", 5, 7));
        // it appears once both of its code units fit
        assertEquals("..." + EMOJI + "...", Strings.abbreviate(s, 5, 8));
        assertEquals("..." + EMOJI + "...", Strings.abbreviate(s, "...", 5, 8));
        // an offset on the low surrogate is moved past the pair
        assertEquals("...f...", Strings.abbreviate(s, 6, 7));
        assertEquals("...f...", Strings.abbreviate(s, "...", 6, 7));

        // the BMP rule of the same sentence is unchanged
        assertEquals("...fghi...", Strings.abbreviate("abcdefghijklmno", 5, 10));
        assertEquals("abc...", Strings.abbreviate("abcdefghijklmno", 4, 6));
        assertEquals("...ijklmno", Strings.abbreviate("abcdefghijklmno", 12, 10));
    }

    // ---------------------------------------------------------------- U20-05 (L12/C-012): @param mapper of the whitespace overload

    @Test
    public void testU2005_mapWordsWhitespaceOverloadRejectsNullMapperResult() {
        final NullPointerException e = assertThrows(NullPointerException.class, () -> Strings.mapWords("a b", w -> null));
        assertEquals("mapper returned null", e.getMessage());

        assertEquals("A B", Strings.mapWords("a b", String::toUpperCase));
        assertNull(Strings.mapWords(null, w -> null));
        assertEquals("", Strings.mapWords("", w -> null));
    }

    // ---------------------------------------------------------------- U20-08 (L12/C-020): split example bound to the String overload

    @Test
    public void testU2008_splitStringDelimiterDocumentedRemainderExample() {
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ":", 2));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ":", 2, true));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ":", 2, false));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ':', 2));
        assertArrayEquals(new String[] { "a", "b::" }, Strings.split("a::b::", "::", 2));
    }
}
