package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.substring;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/** Strings padding contracts, including regression cases. */
public class StringsPaddingTest extends StringsTestSupport {

    // O-1 : repeat(String, int, String, String, String)

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

    protected static String padStartReference(final String str, final int minimumLength, final String padString) {
        final String value = str == null ? "" : str;
        if (value.length() >= minimumLength) {
            return value;
        }

        return paddingReference(padString, minimumLength - value.length()) + value;
    }

    protected static String padEndReference(final String str, final int minimumLength, final String padString) {
        final String value = str == null ? "" : str;
        if (value.length() >= minimumLength) {
            return value;
        }

        return value + paddingReference(padString, minimumLength - value.length());
    }

    protected static String paddingReference(final String padString, final int length) {
        final String actualPadString = padString == null || padString.isEmpty() ? " " : padString;
        final StringBuilder result = new StringBuilder(length + 1);
        int remaining = length;

        while (remaining >= actualPadString.length()) {
            result.append(actualPadString);
            remaining -= actualPadString.length();
        }

        if (remaining > 0) {
            int endIndex = remaining;

            if (Character.isHighSurrogate(actualPadString.charAt(remaining - 1)) && remaining < actualPadString.length()
                    && Character.isLowSurrogate(actualPadString.charAt(remaining))) {
                endIndex++;
            }

            result.append(actualPadString, 0, endIndex);
        }

        return result.toString();
    }

    protected static String repeatReference(String str, final int repeatCount, String delimiter, String prefix, String suffix) {
        str = str == null ? "" : str;
        delimiter = delimiter == null ? "" : delimiter;
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;

        final StringBuilder result = new StringBuilder(prefix);

        for (int i = 0; i < repeatCount; i++) {
            if (i > 0) {
                result.append(delimiter);
            }

            result.append(str);
        }

        return result.append(suffix).toString();
    }

    // abbreviate

    @Test
    public void testAbbreviate() {
        assertEquals("abc", Strings.abbreviate("abc", 5));
        assertEquals("ab...", Strings.abbreviate("abcdefg", 5));
        assertNull(Strings.abbreviate(null, 5));
        assertEquals("", Strings.abbreviate("", 5));
    }

    /**
     * abbreviate's length errors report the offending maxLength (not just the minimum), the marker's length
     * rather than its contents (so a huge marker cannot produce a huge message), and — for the offset form —
     * the offset the CALLER passed, not the internally clamped/shifted one.
     */
    @Test
    public void testAbbreviate_lengthErrorMessagesIncludeMaxLength() {
        assertEquals("maxLength (3) must be at least 4 for an abbreviationMarker of length 3",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", 3)).getMessage());
        assertEquals("maxLength (2) must be at least 3 for an abbreviationMarker of length 2",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "..", 2)).getMessage());
        assertEquals("maxLength (0) must be at least 1 when abbreviationMarker is empty",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "", 0)).getMessage());
        assertEquals("maxLength (6) must be at least 7 for an abbreviationMarker of length 3 at offset 5",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefghij", "...", 5, 6)).getMessage());

        // offset is clamped to str.length() then shifted left internally; the message must still echo 100.
        assertEquals("maxLength (6) must be at least 7 for an abbreviationMarker of length 3 at offset 100",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefghij", "...", 100, 6)).getMessage());

        // The message must not embed the marker itself, so its size stays bounded.
        String hugeMarker = Strings.repeat('x', 10_000);
        String message = assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefghij", hugeMarker, 0, 2)).getMessage();
        assertTrue(message.length() < 100, message);
        assertFalse(message.contains(hugeMarker));
    }

    @Test
    public void testAbbreviate_OffsetBeyondLengthAndEmptyMarker() {
        assertEquals("abcd", Strings.abbreviate("abcdef", "", 100, 4));
        assertEquals("a", Strings.abbreviate("abcdef", "", 100, 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdef", "", 100, 0));
        assertEquals("...ghij", Strings.abbreviate("abcdefghij", "...", 100, 7));
        assertEquals("abcdefg...", Strings.abbreviate("abcdefghijklmno", "...", -1, 10));
        assertEquals("abcdefg...", Strings.abbreviate("abcdefghijklmno", "...", Integer.MIN_VALUE, 10));
    }

    @Test
    public void testAbbreviate_OffsetMinLengthException() {
        // marker="..." (len=3), minAbbrevLengthOffset=7, offset=6 > 4
        // maxLength=6 < 7 => IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("Hello World!!", "...", 6, 6));
    }

    @Test
    public void testAbbreviate_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals("...", Strings.abbreviate(emoji + "abc", 4));
        assertEquals("", Strings.abbreviate(emoji + "abc", "", 1));
        assertEquals("...efgh", Strings.abbreviate("abcd" + emoji + "efgh", "...", 5, 7));
    }

    @Test
    public void testAbbreviate_TooSmallLengthThrows() {
        // maxLength must be at least abbreviationMarker.length + 1.
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "...", 3));
        // OK boundaries.
        assertEquals("a...", Strings.abbreviate("abcdefg", 4));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 7));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 8));
        assertNull(Strings.abbreviate(null, 4));
    }

    @Test
    public void testAbbreviate_WithMarker() {
        assertEquals("abc", Strings.abbreviate("abc", "...", 5));
        assertEquals("ab...", Strings.abbreviate("abcdefg", "...", 5));
        assertEquals("abcd*", Strings.abbreviate("abcdefg", "*", 5));
        assertNull(Strings.abbreviate(null, "...", 5));
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

    @Test
    public void testAbbreviateMarkerMaxLength() {
        assertNull(Strings.abbreviate(null, "...", 10));
        assertEquals("", Strings.abbreviate("", "...", 10));
        assertNull(Strings.abbreviate(null, "...", 3));
        assertEquals("", Strings.abbreviate("", "...", 3));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", null, 10));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", null, 0));
        assertEquals("a", Strings.abbreviate("abcdefg", "", 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "", 0));
        assertEquals("abc..", Strings.abbreviate("abcdefg", "..", 5));
        assertEquals("a..", Strings.abbreviate("abcdefg", "..", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abc", "..", 2));
    }

    @Test
    public void testAbbreviateMaxLength() {
        assertNull(Strings.abbreviate(null, 10));
        assertEquals("", Strings.abbreviate("", 10));
        // null/empty input must not throw on too-small maxLength (null-in/null-out, empty-in/empty-out)
        assertNull(Strings.abbreviate(null, 3));
        assertEquals("", Strings.abbreviate("", 3));
        assertEquals("abc...", Strings.abbreviate("abcdefg", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 7));
        assertEquals("a...", Strings.abbreviate("abcdefg", 4));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abc", 3));
    }

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

    @Test
    public void testAbbreviateWithMarker() {
        assertEquals("abc***", Strings.abbreviate("abcdefg", "***", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", "***", 7));
        assertNull(Strings.abbreviate(null, "***", 4));
    }

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

    @Test
    public void testAbbreviate_OffsetOverloadsArePublic() throws NoSuchMethodException {
        assertTrue(Modifier.isPublic(Strings.class.getDeclaredMethod("abbreviate", String.class, int.class, int.class).getModifiers()));
        assertTrue(Modifier.isPublic(Strings.class.getDeclaredMethod("abbreviate", String.class, String.class, int.class, int.class).getModifiers()));
    }

    // abbreviateMiddle

    @Test
    public void testAbbreviateMiddle() {
        assertEquals("abc...xyz", Strings.abbreviateMiddle("abcdefghijklmnopqrstuvwxyz", "...", 9));
        assertEquals("abc", Strings.abbreviateMiddle("abc", "...", 10));
        assertNull(Strings.abbreviateMiddle(null, "...", 5));
    }

    @Test
    public void testAbbreviateMiddle_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals("a.cd", Strings.abbreviateMiddle("a" + emoji + "bcd", ".", 4));
        assertEquals("ab.", Strings.abbreviateMiddle("abcd" + emoji, ".", 3));
    }

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

    @Test
    public void testC505_abbreviateMiddleStaysWithinDocumentedWindow() {
        final String str = "bc\uD801\uDC00d\uD801\uDC00\uD83D" + EMOJI + "cb";
        final String result = Strings.abbreviateMiddle(str, ".", 6);
        assertEquals("bc.cb", result);
        assertTrue(result.length() >= 5 && result.length() <= 6);
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

    @Test
    public void testEmptyStringEdgeCases() {
        assertEquals("", Strings.repeat("", 5));
        assertEquals(0, Strings.countMatches("", ""));
        assertTrue(Strings.contains("abc", ""));
        assertEquals("abc", Strings.removeAll("abc", ""));
    }

    @Test
    public void testLongStrings() {
        String longStr = Strings.repeat('a', 10000);
        assertEquals(10000, longStr.length());
        assertTrue(Strings.isAllLowerCase(longStr));
        assertEquals(1, Strings.countMatches(longStr + "b" + longStr, "b"));
    }

    // padEnd

    @Test
    public void testPadEnd() {
        assertEquals("abc  ", Strings.padEnd("abc", 5));
        assertEquals("abc", Strings.padEnd("abc", 3));
        assertEquals("abc", Strings.padEnd("abc", 2));
        assertEquals("     ", Strings.padEnd(null, 5));
    }

    @Test
    public void testPadEnd_NullOrEmptyPadStr() {
        assertEquals("abc  ", Strings.padEnd("abc", 5, null));
        assertEquals("abc  ", Strings.padEnd("abc", 5, ""));
    }

    @Test
    public void testPadEnd_NullStr() {
        assertEquals("*****", Strings.padEnd(null, 5, "*"));
        assertEquals("**", Strings.padEnd(null, 2, "*"));
        assertEquals("", Strings.padEnd(null, 0, "*"));
    }

    @Test
    public void testPadEnd_RightPadding() {
        assertEquals("a    ", Strings.padEnd("a", 5));
        assertEquals("a0000", Strings.padEnd("a", 5, '0'));
        assertEquals("a", Strings.padEnd("a", 1));
        assertEquals("---", Strings.padEnd(null, 3, '-'));
        assertEquals("", Strings.padEnd(null, 0));
    }

    @Test
    public void testPadEnd_WithChar() {
        assertEquals("abc00", Strings.padEnd("abc", 5, '0'));
        assertEquals("abc", Strings.padEnd("abc", 3, '0'));
    }

    @Test
    public void testPadEnd_WithString() {
        assertEquals("abc--", Strings.padEnd("abc", 5, "--"));
        assertEquals("abc", Strings.padEnd("abc", 3, "-"));
    }

    @Test
    public void testPadEnd_WithStringPad() {
        assertEquals("abc--", Strings.padEnd("abc", 5, "--"));
        assertEquals("abc", Strings.padEnd("abc", 3, "--"));
    }

    // padStart

    @Test
    public void testPadStart() {
        assertEquals("  abc", Strings.padStart("abc", 5));
        assertEquals("abc", Strings.padStart("abc", 3));
        assertEquals("abc", Strings.padStart("abc", 2));
        assertEquals("     ", Strings.padStart(null, 5));
    }

    @Test
    public void testPadStart_LeftPadding() {
        // padStart adds padding on the LEFT.
        assertEquals("    a", Strings.padStart("a", 5));
        assertEquals("0000a", Strings.padStart("a", 5, '0'));
        assertEquals("a", Strings.padStart("a", 1));
        // null becomes empty, then padded.
        assertEquals("---", Strings.padStart(null, 3, '-'));
        assertEquals("", Strings.padStart(null, 0));
    }

    @Test
    public void testPadStart_WithChar() {
        assertEquals("00abc", Strings.padStart("abc", 5, '0'));
        assertEquals("abc", Strings.padStart("abc", 3, '0'));
    }

    @Test
    public void testPadStart_WithString() {
        assertEquals("--abc", Strings.padStart("abc", 5, "--"));
        assertEquals("abc", Strings.padStart("abc", 3, "-"));
    }

    @Test
    public void testPadStart_WithString_FullCoverage() {
        // basic padding
        assertEquals("00abc", Strings.padStart("abc", 5, "0"));
        assertEquals("xyzxabc", Strings.padStart("abc", 7, "xyz"));

        // no padding needed
        assertEquals("abcde", Strings.padStart("abcde", 3, "0"));
        assertEquals("abc", Strings.padStart("abc", 3, "0"));

        // null input is treated as empty, then padded
        assertEquals("00", Strings.padStart(null, 2, "0"));

        // padding where padStr is longer than remaining
        assertEquals("ababc", Strings.padStart("c", 5, "ab"));
    }

    @Test
    public void testPadStart_WithStringPad() {
        assertEquals("--abc", Strings.padStart("abc", 5, "--"));
        assertEquals("abc", Strings.padStart("abc", 3, "--"));
        assertEquals("--abc", Strings.padStart("abc", 5, "--"));
    }

    @Test
    public void testParameterValidation() {
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("test", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.center("test", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("test", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ",", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.appendIfMissing("test", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.prependIfMissing("test", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.wrapIfMissing("test", ""));
    }

    // repeat

    @Test
    public void test_repeat() {
        String sql = Strings.repeat("?", 0, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN ()", sql);

        sql = Strings.repeat("?", 1, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?)", sql);

        sql = Strings.repeat("?", 2, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?)", sql);

        sql = Strings.repeat("?", 3, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?)", sql);

        sql = Strings.repeat("?", 4, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?, ?)", sql);

        sql = Strings.repeat("?", 5, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?, ?, ?)", sql);

        for (int n = 0; n < 10; n++) {
            assertEquals(new String(Array.repeat('a', n)), Strings.repeat('a', n));
            assertEquals(Strings.join(Array.repeat('a', n), ","), Strings.repeat('a', n, ','));
            assertEquals("ab".repeat(n), Strings.repeat("ab", n));
            assertEquals(Strings.join(Array.repeat("ab", n), ", "), Strings.repeat("ab", n, ", "));
        }
    }

    @Test
    public void testRepeat() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertEquals("", Strings.repeat("hello", 0));
        assertEquals("hellohello", Strings.repeat("hello", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
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
    public void test_repeat_2() {
        for (int n = 0; n < 10; n++) {
            final String repeated = Strings.repeat("ab", n, ", ");
            assertEquals(repeated + ")", Strings.repeat("ab", n, ", ", null, ")"));
            assertEquals("(" + repeated, Strings.repeat("ab", n, ", ", "(", null));
            assertEquals("(" + repeated + ")", Strings.repeat("ab", n, ", ", "(", ")"));
        }
    }

    @Test
    public void testRepeat_Char() {
        assertEquals("aaa", Strings.repeat('a', 3));
        assertEquals("", Strings.repeat('a', 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    public void testRepeat_CharWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("a", Strings.repeat('a', 1, ','));
        assertEquals("", Strings.repeat('a', 0, ','));
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
    public void testRepeat_EdgeCases() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("a", Strings.repeat('a', 1));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("a", -1));
        assertEquals("", Strings.repeat((String) null, 5));
        assertEquals("", Strings.repeat("", 5));
        // delimiter overload
        assertEquals("a-a-a", Strings.repeat("a", 3, "-"));
        assertEquals("", Strings.repeat("a", 0, "-"));
        // prefix/suffix
        assertEquals("[a,a,a]", Strings.repeat("a", 3, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("a", 0, ",", "[", "]"));
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

    @Test
    public void testRepeat_resultLengthGuardStillFires() {
        // checkRepeatResultLength must still reject before any allocation is attempted
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("ab", Integer.MAX_VALUE, "-"));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("ab", Integer.MAX_VALUE, "-", "[", "]"));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat(null, Integer.MAX_VALUE, "-"));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat('a', Integer.MAX_VALUE, '-'));
    }

    // Cross-check: the rewritten repeat is still the exact inverse of split for the shapes that round-trip
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
    public void testRepeat_String() {
        assertEquals("abcabcabc", Strings.repeat("abc", 3));
        assertEquals("", Strings.repeat("abc", 0));
        assertEquals("", Strings.repeat(null, 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("abc", -1));
    }

    @Test
    public void testRepeat_StringWithDelimiter() {
        assertEquals("abc,abc,abc", Strings.repeat("abc", 3, ","));
        assertEquals("abc", Strings.repeat("abc", 1, ","));
        assertEquals("", Strings.repeat("abc", 0, ","));
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

    @Test
    public void testRepeat_WithDelimiterBulkPathMatchesReference() {
        final String[] values = { null, "", "x", "ab", "\uD83D\uDE00" };
        final String[] delimiters = { null, "", ",", "::", "\uD83D\uDE00" };
        final String[][] affixes = { { null, null }, { "", "" }, { "[", "]" }, { "prefix", "suffix" } };
        final int[] repeatCounts = { 0, 1, 2, 3, 7, 8, 16, 257 };

        for (final String value : values) {
            for (final String delimiter : delimiters) {
                for (final String[] affix : affixes) {
                    for (final int repeatCount : repeatCounts) {
                        assertEquals(repeatReference(value, repeatCount, delimiter, affix[0], affix[1]),
                                Strings.repeat(value, repeatCount, delimiter, affix[0], affix[1]));
                    }
                }
            }
        }
    }

    @Test
    public void testRepeat_WithPrefixSuffix() {
        assertEquals("[abc,abc,abc]", Strings.repeat("abc", 3, ",", "[", "]"));
        assertEquals("[abc]", Strings.repeat("abc", 1, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("abc", 0, ",", "[", "]"));
        assertEquals("[,,]", Strings.repeat("", 3, ",", "[", "]"));
        assertEquals("xxx", Strings.repeat("x", 3, null, null, null));
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
    public void testRepeatChar() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("a", Strings.repeat('a', 1));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    public void testRepeatString() {
        assertEquals("ababab", Strings.repeat("ab", 3));
        assertEquals("", Strings.repeat("ab", 0));
        assertEquals("", Strings.repeat(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("ab", -1));
    }

    @Test
    public void testRepeatStringWithDelimiter() {
        assertEquals("ab,ab,ab", Strings.repeat("ab", 3, ","));
        assertEquals("[ab,ab]", Strings.repeat("ab", 2, ",", "[", "]"));
        assertEquals("[ab]", Strings.repeat("ab", 1, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("ab", 0, ",", "[", "]"));
        assertEquals("[,]", Strings.repeat("", 2, ",", "[", "]"));
    }

    @Test
    public void testC508_repeatWithAffixesRestatesZeroAndEmptyRules() {
        assertEquals("[]", Strings.repeat("ab", 0, ",", "[", "]"));
        assertEquals("[,,]", Strings.repeat(null, 3, ",", "[", "]"));
        assertEquals("[,,]", Strings.repeat("", 3, ",", "[", "]"));
        assertEquals("[ab,ab,ab]", Strings.repeat("ab", 3, ",", "[", "]"));
        assertEquals(",,", Strings.repeat(null, 3, ","));
    }

    @Test
    public void testRepeatWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("hello,hello", Strings.repeat("hello", 2, ","));
        assertEquals("", Strings.repeat("hello", 0, ","));
        assertEquals(",,", Strings.repeat(null, 3, ","));
        assertEquals(",,", Strings.repeat(null, 3, ','));
    }

    @Test
    public void testRepeatWithDelimiterRejectsImpossibleLengthBeforeAppending() {
        final OutOfMemoryError charError = assertThrows(OutOfMemoryError.class, () -> Strings.repeat('a', Integer.MAX_VALUE, ','));
        assertTrue(charError.getMessage().contains("Required string length"));

        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("", Integer.MAX_VALUE, ","));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("a", Integer.MAX_VALUE, ",", "[", "]"));
    }

    @Test
    public void testRepeatWithPrefixSuffix() {
        assertEquals("[a,a,a]", Strings.repeat("a", 3, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("a", 0, ",", "[", "]"));
    }

    @Test
    public void testStringPadding_BulkPathMatchesReference() {
        final String[] values = { null, "", "x", "xy", "\uD83D\uDE00x" };
        final String[] padStrings = { null, "", "0", "ab", "abc", "\uD83D\uDE00", "x\uD83D\uDE00", "x\uD83D" };
        final int[] minimumLengths = { 0, 1, 2, 3, 4, 5, 16, 31, 32, 33, 256, 257 };

        for (final String value : values) {
            for (final String padString : padStrings) {
                for (final int minimumLength : minimumLengths) {
                    assertEquals(padStartReference(value, minimumLength, padString), Strings.padStart(value, minimumLength, padString));
                    assertEquals(padEndReference(value, minimumLength, padString), Strings.padEnd(value, minimumLength, padString));
                }
            }
        }

        assertEquals(padStartReference("", 256, "ab"), Strings.padStart(null, 256, "ab"));
        assertEquals(padEndReference("", 256, "ab"), Strings.padEnd(null, 256, "ab"));
    }

    @Test
    public void testStringPadding_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals(emoji + "x", Strings.padStart("x", 2, emoji));
        assertEquals("x" + emoji, Strings.padEnd("x", 2, emoji));
        assertEquals(emoji + "x", Strings.center("x", 3, emoji));
    }

}
