package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringsStripTest extends StringsTestSupport {
    @Test
    public void testStripEach() {
        String[] array = { "  a  ", " b ", "c" };
        Strings.stripEach(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testStripWithChars() {
        assertEquals("abc", Strings.strip("xyabcxyz", "xyz"));
    }

    @Test
    public void testStripEach_AdditionalValues() {
        String[] strs = { " a ", "  b  ", " c" };
        Strings.stripEach(strs);
        assertArrayEquals(new String[] { "a", "b", "c" }, strs);
    }

    @Test
    public void testStripEach_WithChars() {
        String[] strs = { "xxhelloxx", "yworldy" };
        Strings.stripEach(strs, "xy");
        assertEquals("hello", strs[0]);
        assertEquals("world", strs[1]);
    }

    @Test
    public void testStrip() {
        assertEquals("abc", Strings.strip("  abc  "));
        assertEquals("abc", Strings.strip("abc"));
        assertEquals("", Strings.strip("   "));
        assertNull(Strings.strip(null));
    }

    @Test
    public void testStrip_WithChars() {
        assertEquals("abc", Strings.strip("xxabcxx", "x"));
        assertEquals("abc", Strings.strip("--abc--", "-"));
        assertNull(Strings.strip(null, "x"));
    }

    @Test
    public void testStrip_EdgeCases() {
        assertNull(Strings.strip(null));
        assertEquals("", Strings.strip(""));
        assertEquals("", Strings.strip("   "));
        assertEquals("abc", Strings.strip("  abc  "));
    }

    @Test
    public void testStrip_WithCharsEdgeCases() {
        assertEquals("abc", Strings.strip("xxabcxx", "x"));
        assertEquals("abc", Strings.strip("xyabcyx", "xy"));
        assertNull(Strings.strip(null, "x"));
    }

    @Test
    public void testStripEach_EdgeCases() {
        String[] array = { "  abc  ", " xyz " };
        Strings.stripEach(array);
        assertEquals("abc", array[0]);
        assertEquals("xyz", array[1]);
    }

    @Test
    public void testStripEach_WithCharsEdgeCases() {
        String[] array = { "xxabcxx", "yydefyy" };
        Strings.stripEach(array, "xy");
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
    }

    @Test
    public void testStripToNull() {
        assertEquals("abc", Strings.stripToNull("  abc  "));
        assertNull(Strings.stripToNull("   "));
        assertNull(Strings.stripToNull(null));
    }

    @Test
    public void testStripToNullEach() {
        String[] strs = { "  hello  ", "   " };
        Strings.stripToNullEach(strs);
        assertEquals("hello", strs[0]);
        assertNull(strs[1]);
    }

    @Test
    public void testStripToNull_EdgeCases() {
        assertNull(Strings.stripToNull(null));
        assertNull(Strings.stripToNull(""));
        assertNull(Strings.stripToNull("   "));
        assertEquals("abc", Strings.stripToNull("  abc  "));
    }

    @Test
    public void testStripToNullEach_EdgeCases() {
        String[] array = { "  abc  ", "   " };
        Strings.stripToNullEach(array);
        assertEquals("abc", array[0]);
        assertNull(array[1]);
    }

    @Test
    public void testStripToEmpty() {
        assertEquals("abc", Strings.stripToEmpty("  abc  "));
        assertEquals("", Strings.stripToEmpty("   "));
        assertEquals("", Strings.stripToEmpty(null));
    }

    @Test
    public void testStripToEmptyEach() {
        String[] strs = { "  hello  ", null };
        Strings.stripToEmptyEach(strs);
        assertEquals("hello", strs[0]);
        assertEquals("", strs[1]);
    }

    @Test
    public void testStripToEmpty_EdgeCases() {
        assertEquals("", Strings.stripToEmpty(null));
        assertEquals("", Strings.stripToEmpty(""));
        assertEquals("", Strings.stripToEmpty("   "));
        assertEquals("abc", Strings.stripToEmpty("  abc  "));
    }

    @Test
    public void testStripToEmptyEach_EdgeCases() {
        String[] array = { "  abc  ", null };
        Strings.stripToEmptyEach(array);
        assertEquals("abc", array[0]);
        assertEquals("", array[1]);
    }

    @Test
    public void testStripStartEach_WithChars() {
        String[] strs = { "xxhello", "yworld" };
        Strings.stripStartEach(strs, "xy");
        assertEquals("hello", strs[0]);
        assertEquals("world", strs[1]);
    }

    @Test
    public void testStripStart() {
        assertEquals("abc  ", Strings.stripStart("  abc  "));
        assertEquals("abc", Strings.stripStart("abc"));
        assertNull(Strings.stripStart(null));
    }

    @Test
    public void testStripStartEach() {
        String[] arr = { "xxabc", "xydef", "xyz", null };
        Strings.stripStartEach(arr, "xyz");
        Assertions.assertArrayEquals(new String[] { "abc", "def", "", null }, arr);

        String[] arr2 = { "  test", null, " \tabc" };
        Strings.stripStartEach(arr2, null);
        Assertions.assertArrayEquals(new String[] { "test", null, "abc" }, arr2);

        Strings.stripStartEach(null, "xyz");

        Strings.stripStartEach(new String[0], "xyz");
    }

    @Test
    public void testStripStart_NoArgs() {
        assertEquals("hello  ", Strings.stripStart("  hello  "));
        assertNull(Strings.stripStart(null));
    }

    @Test
    public void testStripStart_EdgeCases() {
        assertNull(Strings.stripStart(null));
        assertEquals("", Strings.stripStart(""));
        assertEquals("abc  ", Strings.stripStart("  abc  "));
        assertEquals("abc", Strings.stripStart("abc"));
    }

    @Test
    public void testStripStart_WithCharsEdgeCases() {
        assertEquals("abcxx", Strings.stripStart("xxabcxx", "x"));
        assertNull(Strings.stripStart(null, "x"));
    }

    @Test
    public void testStripStartEach_EdgeCases() {
        String[] array = { "xxabc", "yydef" };
        Strings.stripStartEach(array, "xy");
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
    }

    @Test
    public void testStripEndEach_WithChars() {
        String[] strs = { "helloxx", "worldy" };
        Strings.stripEndEach(strs, "xy");
        assertEquals("hello", strs[0]);
        assertEquals("world", strs[1]);
    }

    @Test
    public void testStripEnd() {
        assertEquals("  abc", Strings.stripEnd("  abc  "));
        assertEquals("abc", Strings.stripEnd("abc"));
        assertNull(Strings.stripEnd(null));
    }

    @Test
    public void testStripEnd_WithChars() {
        assertEquals("xxabc", Strings.stripEnd("xxabcxx", "x"));
        assertNull(Strings.stripEnd(null, "x"));
    }

    @Test
    public void testStripEndEach() {
        String[] arr = { "abcxx", "defxy", "xyz", null };
        Strings.stripEndEach(arr, "xyz");
        Assertions.assertArrayEquals(new String[] { "abc", "def", "", null }, arr);

        String[] arr2 = { "test  ", null, "abc \t" };
        Strings.stripEndEach(arr2, null);
        Assertions.assertArrayEquals(new String[] { "test", null, "abc" }, arr2);

        Strings.stripEndEach(null, "xyz");

        Strings.stripEndEach(new String[0], "xyz");
    }

    @Test
    public void testStripEnd_NoArgs() {
        assertEquals("  hello", Strings.stripEnd("  hello  "));
        assertNull(Strings.stripEnd(null));
    }

    @Test
    public void testStripEnd_EdgeCases() {
        assertNull(Strings.stripEnd(null));
        assertEquals("", Strings.stripEnd(""));
        assertEquals("  abc", Strings.stripEnd("  abc  "));
        assertEquals("abc", Strings.stripEnd("abc"));
    }

    @Test
    public void testStripEndEach_EdgeCases() {
        String[] array = { "abcxx", "defyy" };
        Strings.stripEndEach(array, "xy");
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
    }

    @Test
    public void testStripEnd_FullCoverage() {
        // null stripChars -> strip whitespace
        assertEquals("abc", Strings.stripEnd("abc   ", null));
        assertEquals("", Strings.stripEnd("   ", null));

        // specific chars
        assertEquals("abc", Strings.stripEnd("abcxyz", "xyz"));
        assertEquals("12", Strings.stripEnd("120.00", ".0"));

        // null/empty str
        assertNull(Strings.stripEnd(null, "xyz"));
        assertEquals("", Strings.stripEnd("", "xyz"));

        // empty stripChars
        assertEquals("abc  ", Strings.stripEnd("abc  ", ""));

        // no stripping needed
        assertEquals("abc", Strings.stripEnd("abc", "xyz"));
    }

    @Test
    public void testStripAccentsEach() {
        String[] array = { "\u00e0bc", "xyz" };
        Strings.stripAccentsEach(array);
        assertArrayEquals(new String[] { "abc", "xyz" }, array);
    }

    @Test
    public void testStripAccents() {
        assertEquals("aeiou", Strings.stripAccents("\u00e0\u00e9\u00ed\u00f3\u00fa"));
        assertNull(Strings.stripAccents(null));
        assertEquals("", Strings.stripAccents(""));
        assertEquals("abc", Strings.stripAccents("abc"));
        assertEquals("eclair", Strings.stripAccents("\u00E9clair"));
        assertEquals("uber", Strings.stripAccents("\u00FCber"));
    }

    @Test
    public void testStripAccents_ExtendedCombiningMarks() {
        assertEquals("a", Strings.stripAccents("a\u1AB0")); // Combining Diacritical Marks Extended
        assertEquals("b", Strings.stripAccents("b\u1DC0")); // Combining Diacritical Marks Supplement
        assertEquals("c", Strings.stripAccents("c\uFE20")); // Combining Half Marks

        assertEquals("a\u034F", Strings.stripAccents("a\u034F")); // Combining Grapheme Joiner is a normalization control
        assertEquals("1\uFE0F\u20E3", Strings.stripAccents("1\uFE0F\u20E3")); // emoji keycap sequence is structural
        assertEquals("x\u20D0", Strings.stripAccents("x\u20D0")); // symbol modifier block is intentionally preserved
        assertEquals("a\u05B4", Strings.stripAccents("a\u05B4")); // Hebrew point is outside the selected blocks
        assertEquals("\u2764\uFE0F", Strings.stripAccents("\u2764\uFE0F")); // preserve emoji variation selector
    }

    @Test
    public void testStripAccents_PreservesNfdUnstableWithoutAccents() {
        assertEquals("한", Strings.stripAccents("한"));
        assertEquals("한국어", Strings.stripAccents("한국어"));
        assertEquals("한cafe", Strings.stripAccents("한caf\u00e9"));
        assertEquals("Lodz", Strings.stripAccents("\u0141\u00f3d\u017a"));
        assertEquals("\u2126", Strings.stripAccents("\u2126")); // Ohm sign must not become Greek Omega
        assertEquals("\u2126e", Strings.stripAccents("\u2126\u00e9")); // Preserve Ohm sign when another code point loses an accent
        assertEquals("e\u2126", Strings.stripAccents("\u00e9\u2126"));
        assertEquals("\u212Ae", Strings.stripAccents("\u212A\u00e9")); // Preserve Kelvin sign, not ASCII K
        assertEquals("\uF900e", Strings.stripAccents("\uF900\u00e9")); // Preserve CJK compatibility ideograph
    }

    @Test
    public void testStripAccentsEach_EdgeCases() {
        String[] array = { "\u00E9clair", "caf\u00E9" };
        Strings.stripAccentsEach(array);
        assertEquals("eclair", array[0]);
        assertEquals("cafe", array[1]);
    }

    @Test
    public void testStripAccents_AllOptimizationBranchesMatchOriginalSemantics() {
        final String ascii = "plain ASCII 123";
        assertTrue(ascii == Strings.stripAccents(ascii));

        final String[] deterministic = { "", "\u00E9", "\u00E9clair", "\u00E9".repeat(32), "A\u0301B\u1AB0C\u1DC0D\uFE20", "\u0141\u00F3d\u017A",
                "\uD55C\u2126\u212A\uFA10", "\uD800\u00E9\uDC00", "x\u034Fy", "x\u20E3y", "x\uFE0Fy" };

        for (final String input : deterministic) {
            assertEquals(stripAccentsReference(input), Strings.stripAccents(input), input);
        }

        final StringBuilder manyDistinct = new StringBuilder();
        for (int codePoint = 0x0100; codePoint < 0x0150; codePoint++) {
            manyDistinct.appendCodePoint(codePoint);
        }
        assertEquals(stripAccentsReference(manyDistinct.toString()), Strings.stripAccents(manyDistinct.toString()));

        final StringBuilder everyBmpPrivateUseCharacter = new StringBuilder(0x1902);
        everyBmpPrivateUseCharacter.append('\u00E9');
        for (char ch = '\uE000'; ch < '\uF8FF'; ch++) {
            everyBmpPrivateUseCharacter.append(ch);
        }
        everyBmpPrivateUseCharacter.append('\uF8FF').append('\u0142');
        assertEquals(stripAccentsReference(everyBmpPrivateUseCharacter.toString()), Strings.stripAccents(everyBmpPrivateUseCharacter.toString()));

        final int[] alphabet = { 'a', 'Z', 0x00C0, 0x00E9, 0x0141, 0x0142, 0x0301, 0x034F, 0x1AB0, 0x1DC0, 0x20E3, 0xFE0F, 0xFE20, 0xAC00, 0x2126, 0x212A,
                0xFA10, 0x1F600, 0xD800, 0xDC00, 0xE000 };
        final Random random = new Random(20260824L);

        for (int iteration = 0; iteration < 250; iteration++) {
            final StringBuilder input = new StringBuilder();
            final int length = random.nextInt(121);

            for (int i = 0; i < length; i++) {
                input.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
            }

            assertEquals(stripAccentsReference(input.toString()), Strings.stripAccents(input.toString()), "iteration " + iteration);
        }
    }

    @Test
    public void testStripAccents_SelectedRangesAndCacheCrossover() {
        final int[][] ranges = { { 0x0300, 0x036F }, { 0x1AB0, 0x1AFF }, { 0x1DC0, 0x1DFF }, { 0xFE20, 0xFE2F } };

        for (final int[] range : ranges) {
            for (int codePoint = range[0]; codePoint <= range[1]; codePoint++) {
                final String mark = new String(Character.toChars(codePoint));
                final String input = "x" + mark + "y";
                assertEquals(stripAccentsReference(input), Strings.stripAccents(input), "U+" + Integer.toHexString(codePoint));
            }
        }

        for (final int distinctCount : new int[] { 63, 64, 65, 66 }) {
            final StringBuilder input = new StringBuilder();

            for (int i = 0; i < distinctCount; i++) {
                input.appendCodePoint(0x0100 + i);
            }

            input.append("-\u00E9-\u0142-\uD800-\uDC00");
            assertEquals(stripAccentsReference(input.toString()), Strings.stripAccents(input.toString()), "distinctCount=" + distinctCount);
        }

        final String boundarySensitive = "\u00E9\u0323\u2126\u034F\u0301\uD55C\uFE20";
        assertEquals(stripAccentsReference(boundarySensitive), Strings.stripAccents(boundarySensitive));
    }
}
