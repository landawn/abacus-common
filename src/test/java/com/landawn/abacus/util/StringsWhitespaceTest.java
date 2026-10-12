package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.substring;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Strings whitespace contracts, including regression cases. */
public class StringsWhitespaceTest extends StringsTestSupport {

    private static final String CTRL = String.valueOf((char) 0x0001);



 // START OF HEADING (not Java whitespace)
    private static final String NEL = String.valueOf((char) 0x0085);



 // NEXT LINE (not Java whitespace)
    private static final String NBSP = String.valueOf((char) 0x00A0);



 // NO-BREAK SPACE (isSpaceChar only)
    private static final String OGHAM = String.valueOf((char) 0x1680);



 // OGHAM SPACE MARK (Java whitespace)
    private static final String EN_QUAD = String.valueOf((char) 0x2000);



 // EN QUAD (Java whitespace)
    private static final String FIGURE_SP = String.valueOf((char) 0x2007);



 // FIGURE SPACE (isSpaceChar only)
    private static final String NNBSP = String.valueOf((char) 0x202F);



 // NARROW NO-BREAK SPACE (isSpaceChar only)
    private static final String IDEO_SP = String.valueOf((char) 0x3000);



    // U+1F300 CYCLONE and U+1F700 ALCHEMICAL SYMBOL FOR QUINTESSENCE share the low surrogate U+DF00
    private static final String CYCLONE = "\uD83C\uDF00";

    private static final String QUINTESSENCE = "\uD83D\uDF00";



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

    protected static String normalizeSpaceReference(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length());
        boolean lastWasWhitespace = true;
        boolean sawNonWhitespace = false;

        for (int i = 0, len = str.length(); i < len;) {
            final int codePoint = str.codePointAt(i);

            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                if (!lastWasWhitespace) {
                    result.append(' ');
                    lastWasWhitespace = true;
                }
            } else {
                result.appendCodePoint(codePoint);
                lastWasWhitespace = false;
                sawNonWhitespace = true;
            }

            i += Character.charCount(codePoint);
        }

        if (!sawNonWhitespace) {
            return "";
        }

        if (lastWasWhitespace) {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    protected static String removeWhitespaceReference(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                result.append(str.charAt(i));
            }
        }

        return result.toString();
    }

    protected static String escapeQuotesReference(final String str, final char quoteChar, final boolean escapeBothQuotes) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length() + 16);

        if (!escapeBothQuotes && quoteChar == '\\') {
            for (int i = 0, len = str.length(); i < len;) {
                if (str.charAt(i) != '\\') {
                    result.append(str.charAt(i++));
                    continue;
                }

                final int runStart = i;
                while (i < len && str.charAt(i) == '\\') {
                    i++;
                }

                result.append(str, runStart, i);

                if (((i - runStart) & 1) != 0) {
                    result.append('\\');
                }
            }

            return result.toString();
        }

        for (int i = 0, len = str.length(); i < len; i++) {
            final char ch = str.charAt(i);

            if (ch == '\\' && i < len - 1) {
                result.append(ch).append(str.charAt(++i));
            } else {
                if (escapeBothQuotes ? ch == '\'' || ch == '"' : ch == quoteChar) {
                    result.append('\\');
                }

                result.append(ch);
            }
        }

        return result.toString();
    }

    protected static String stripAccentsReference(final String str) {
        if (str == null) {
            return null;
        }

        StringBuilder result = null;

        for (int fromIndex = 0, len = str.length(); fromIndex < len;) {
            final int codePoint = str.codePointAt(fromIndex);
            final int toIndex = fromIndex + Character.charCount(codePoint);

            if (codePoint < 0x80) {
                if (result != null) {
                    result.append((char) codePoint);
                }

                fromIndex = toIndex;
                continue;
            }

            final String originalCodePoint = str.substring(fromIndex, toIndex);
            final String codePointNfd = java.text.Normalizer.normalize(originalCodePoint, java.text.Normalizer.Form.NFD);
            final StringBuilder decomposed = new StringBuilder(codePointNfd);

            for (int i = 0; i < decomposed.length(); i++) {
                if (decomposed.charAt(i) == '\u0141') {
                    decomposed.setCharAt(i, 'L');
                } else if (decomposed.charAt(i) == '\u0142') {
                    decomposed.setCharAt(i, 'l');
                }
            }

            final String stripped = removeAccentMarksReference(decomposed);

            if (stripped.equals(codePointNfd)) {
                if (result != null) {
                    result.append(originalCodePoint);
                }
            } else {
                if (result == null) {
                    result = new StringBuilder(str.length());
                    result.append(str, 0, fromIndex);
                }

                result.append(java.text.Normalizer.normalize(stripped, java.text.Normalizer.Form.NFC));
            }

            fromIndex = toIndex;
        }

        return result == null ? str : result.toString();
    }

    protected static String removeAccentMarksReference(final CharSequence decomposed) {
        StringBuilder result = null;

        for (int fromIndex = 0, len = decomposed.length(); fromIndex < len;) {
            final int codePoint = Character.codePointAt(decomposed, fromIndex);
            final int toIndex = fromIndex + Character.charCount(codePoint);

            if (isRemovableAccentMarkReference(codePoint)) {
                if (result == null) {
                    result = new StringBuilder(len);
                    result.append(decomposed, 0, fromIndex);
                }
            } else if (result != null) {
                result.append(decomposed, fromIndex, toIndex);
            }

            fromIndex = toIndex;
        }

        return result == null ? decomposed.toString() : result.toString();
    }

    protected static boolean isRemovableAccentMarkReference(final int codePoint) {
        // C-513: the negation overlay U+0338 is preserved like U+034F
        if (!Unicode17Data.isMark(codePoint) || codePoint == 0x034F || codePoint == 0x0338) {
            return false;
        }

        return codePoint >= 0x0300 && codePoint <= 0x036F || codePoint >= 0x1AB0 && codePoint <= 0x1AFF || codePoint >= 0x1DC0 && codePoint <= 0x1DFF
                || codePoint >= 0xFE20 && codePoint <= 0xFE2F;
    }

    // blankElementsToEmpty

    @Test
    public void testBlankElementsToEmpty_Array() {
        String[] array = { null, "abc", "  ", "xyz" };
        Strings.blankElementsToEmpty(array);
        assertArrayEquals(new String[] { "", "abc", "", "xyz" }, array);
    }

    @Test
    public void testBlankElementsToEmpty_NullOrEmptyInput() {
        assertDoesNotThrow(() -> Strings.blankElementsToEmpty((String[]) null));
        assertDoesNotThrow(() -> Strings.blankElementsToEmpty(new String[0]));
    }

    @Test
    public void testBlankElementsToEmptyArray() {
        String[] arr = { null, "a", " ", "\t" };
        Strings.blankElementsToEmpty(arr);
        assertArrayEquals(new String[] { "", "a", "", "" }, arr);
    }

    // blankElementsToNull

    @Test
    public void testBlankElementsToNull_Array() {
        String[] array = { "  ", "abc", "", "xyz" };
        Strings.blankElementsToNull(array);
        assertArrayEquals(new String[] { null, "abc", null, "xyz" }, array);
    }

    @Test
    public void testBlankElementsToNull_NullOrEmptyInput() {
        assertDoesNotThrow(() -> Strings.blankElementsToNull((String[]) null));
        assertDoesNotThrow(() -> Strings.blankElementsToNull(new String[0]));
    }

    @Test
    public void testBlankElementsToNullArray() {
        String[] arr = { null, "a", " ", "\t" };
        Strings.blankElementsToNull(arr);
        assertArrayEquals(new String[] { null, "a", null, null }, arr);
    }

    @Test
    public void testBlankToEmpty() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("   "));
        assertEquals("abc", Strings.blankToEmpty("abc"));
        assertEquals("  abc  ", Strings.blankToEmpty("  abc  "));
    }

    @Test
    public void testBlankToEmptyString() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("   "));
        assertEquals("abc", Strings.blankToEmpty("abc"));
    }

    @Test
    public void testBlankToNull() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("   "));
        assertEquals("abc", Strings.blankToNull("abc"));
        assertEquals("  abc  ", Strings.blankToNull("  abc  "));
    }

    @Test
    public void testBlankToNullString() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("   "));
        assertEquals("abc", Strings.blankToNull("abc"));
    }

    // containsWhitespace

    @Test
    public void testContainsWhitespace() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("a\tb"));
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    public void testContainsWhitespace_EdgeCases() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("abc\t"));
        assertTrue(Strings.containsWhitespace("abc\n"));
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace(""));
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    public void testContainsWhitespace_EmptyString() {
        assertFalse(Strings.containsWhitespace(""));
    }

    @Test
    public void testContainsWhitespace_NoWhitespace() {
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace("123"));
        assertFalse(Strings.containsWhitespace("!@#$%"));
        assertFalse(Strings.containsWhitespace("αβγ"));
    }

    @Test
    public void testContainsWhitespace_NullString() {
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    public void testContainsWhitespace_VariousWhitespace() {
        assertTrue(Strings.containsWhitespace("\t"));
        assertTrue(Strings.containsWhitespace("\n"));
        assertTrue(Strings.containsWhitespace("\r"));
        assertTrue(Strings.containsWhitespace("\f"));
        assertTrue(Strings.containsWhitespace("a\tb"));
        assertTrue(Strings.containsWhitespace("line1\nline2"));
        assertTrue(Strings.containsWhitespace("text\rtext"));
    }

    @Test
    public void testContainsWhitespace_WithSpace() {
        assertTrue(Strings.containsWhitespace(" "));
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace(" abc"));
        assertTrue(Strings.containsWhitespace("abc "));
        assertTrue(Strings.containsWhitespace("a b c"));
    }



    /**
     * The class-level whitespace-predicate index lists {@code containsWhitespace} in the code-unit half and the
     * case-conversion family, {@code capitalizeWords}/{@code capitalizeWordsFully} and {@code mapWords} in the
     * code-point half. Both halves resolve to Java whitespace, so a non-breaking separator is neither a
     * {@code containsWhitespace} hit nor a word separator, while U+3000 is both.
     */
    @Test
    public void testWhitespacePredicates_containsWhitespaceAndCaseConversionFamily() {
        assertFalse(Strings.containsWhitespace("a" + NBSP + "b"));
        assertFalse(Strings.containsWhitespace("a" + FIGURE_SP + "b"));
        assertFalse(Strings.containsWhitespace("a" + NNBSP + "b"));
        assertFalse(Strings.containsWhitespace("a" + NEL + "b"));
        assertFalse(Strings.containsWhitespace("a" + CTRL + "b"));
        assertTrue(Strings.containsWhitespace("a" + OGHAM + "b"));
        assertTrue(Strings.containsWhitespace("a" + IDEO_SP + "b"));

        // the case-conversion family splits on a Java-whitespace run ...
        assertEquals("a_b", Strings.toSnakeCase("a" + IDEO_SP + "b"));
        assertEquals("a-b", Strings.toKebabCase("a" + IDEO_SP + "b"));
        assertEquals("A_B", Strings.toScreamingSnakeCase("a" + IDEO_SP + "b"));
        assertEquals("aB", Strings.toCamelCase("a" + IDEO_SP + "b"));
        assertEquals("AB", Strings.toUpperCamelCase("a" + IDEO_SP + "b"));
        assertEquals("AB", Strings.toPascalCase("a" + IDEO_SP + "b"));
        assertEquals("a_b", Strings.toSnakeCase("a" + OGHAM + "b"));

        // ... and not on a non-breaking separator, which is not Java whitespace
        assertEquals("a" + NBSP + "b", Strings.toSnakeCase("a" + NBSP + "b"));
        assertEquals("a" + NBSP + "b", Strings.toKebabCase("a" + NBSP + "b"));
        assertEquals("A" + NBSP + "B", Strings.toScreamingSnakeCase("a" + NBSP + "b"));
        assertEquals("a" + NBSP + "b", Strings.toCamelCase("a" + NBSP + "b"));
        assertEquals("A" + NBSP + "b", Strings.toUpperCamelCase("a" + NBSP + "b"));
        assertEquals("a" + FIGURE_SP + "b", Strings.toSnakeCase("a" + FIGURE_SP + "b"));

        // capitalizeWords, capitalizeWordsFully and mapWords use the same predicate
        assertEquals("Ab" + IDEO_SP + "Cd", Strings.capitalizeWords("ab" + IDEO_SP + "cd"));
        assertEquals("Ab" + NBSP + "cd", Strings.capitalizeWords("ab" + NBSP + "cd"));
        assertEquals("Ab" + IDEO_SP + "Cd", Strings.capitalizeWordsFully("AB" + IDEO_SP + "CD"));
        assertEquals("Ab" + NBSP + "cd", Strings.capitalizeWordsFully("AB" + NBSP + "CD"));
        assertEquals("<ab>" + IDEO_SP + "<cd>", Strings.mapWords("ab" + IDEO_SP + "cd", w -> "<" + w + ">"));
        assertEquals("<ab" + NBSP + "cd>", Strings.mapWords("ab" + NBSP + "cd", w -> "<" + w + ">"));

        // the code-unit half of the index
        final String[] tokens = Strings.splitOnWhitespacePreserveAllTokens("a" + IDEO_SP + IDEO_SP + "b");
        assertEquals(3, tokens.length);
        assertEquals("a", tokens[0]);
        assertEquals("", tokens[1]);
        assertEquals("b", tokens[2]);
        assertEquals(1, Strings.splitOnWhitespacePreserveAllTokens("a" + NBSP + NBSP + "b").length);
        assertEquals("a", Strings.stripStart(IDEO_SP + "a", null));
        assertEquals("a", Strings.stripEnd("a" + IDEO_SP, null));
        assertEquals(null, Strings.stripToNull(IDEO_SP));
        assertEquals(NBSP, Strings.stripToEmpty(NBSP));
    }

    @Test
    public void testCopyThenStrip() {
        String[] result = Strings.copyThenStrip(new String[] { "  a  ", " b " });
        assertArrayEquals(new String[] { "a", "b" }, result);
        assertNull(Strings.copyThenStrip((String[]) null));
    }

    @Test
    public void testCopyThenStrip_EdgeCases() {
        String[] strs = { "  abc  ", " xyz ", null };
        String[] result = Strings.copyThenStrip(strs);
        assertEquals("abc", result[0]);
        assertEquals("xyz", result[1]);
        assertNull(result[2]);
        // Ensure original is not modified
        assertEquals("  abc  ", strs[0]);
        assertArrayEquals(new String[] { "\u0001x\u0001", "x", "\u00A0x\u00A0" },
                Strings.copyThenStrip(new String[] { "\u0001x\u0001", "\u2000x\u2000", "\u00A0x\u00A0" }));
    }

    @Test
    public void testCopyThenTrim() {
        String[] result = Strings.copyThenTrim(new String[] { "  a  ", " b " });
        assertArrayEquals(new String[] { "a", "b" }, result);
        assertNull(Strings.copyThenTrim((String[]) null));
    }

    @Test
    public void testCopyThenTrim_EdgeCases() {
        String[] strs = { "  abc  ", " xyz ", null };
        String[] result = Strings.copyThenTrim(strs);
        assertEquals("abc", result[0]);
        assertEquals("xyz", result[1]);
        assertNull(result[2]);
        // Ensure original is not modified
        assertEquals("  abc  ", strs[0]);
        assertArrayEquals(new String[] { "x", "\u2000x\u2000", "\u00A0x\u00A0" },
                Strings.copyThenTrim(new String[] { "\u0001x\u001F", "\u2000x\u2000", "\u00A0x\u00A0" }));
    }

    // defaultIfBlank

    @Test
    public void testDefaultIfBlank() {
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
        assertEquals("default", Strings.defaultIfBlank("", "default"));
        assertEquals("default", Strings.defaultIfBlank(" ", "default"));
        assertEquals("abc", Strings.defaultIfBlank("abc", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("abc", " "));
    }

    @Test
    public void testDefaultIfBlank_Blank() {
        assertEquals("default", Strings.defaultIfBlank("   ", "default"));
        assertEquals("default", Strings.defaultIfBlank("", "default"));
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
    }

    @Test
    public void testDefaultIfBlank_BlankDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, "  "));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, (String) null));
    }

    @Test
    public void testDefaultIfBlank_NotBlank() {
        assertEquals("hello", Strings.defaultIfBlank("hello", "default"));
        assertEquals("  abc  ", Strings.defaultIfBlank("  abc  ", "default"));
    }

    @Test
    public void testDefaultIfBlankSupplier() {
        Supplier<String> supplier = () -> "supplied";
        assertEquals("supplied", Strings.defaultIfBlank(" ", supplier));
        assertEquals("abc", Strings.defaultIfBlank("abc", supplier));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank(" ", Fn.s(() -> " ")));
    }

    @Test
    public void testDefaultIfBlank_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfBlank("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfBlank("   ", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfBlank(null, () -> "default"));
    }

    @Test
    public void testDeleteWhitespace() {
        assertEquals("abc", Strings.removeWhitespace("a b c"));
        assertEquals("abc", Strings.removeWhitespace("  a  b  c  "));
        assertEquals("abc", Strings.removeWhitespace("abc"));
        assertNull(Strings.removeWhitespace(null));
    }

    @Test
    public void testEachMethodsWithSingleElement() {
        String[] single = { "  test  " };
        Strings.trimToNullEach(single);
        Assertions.assertEquals("test", single[0]);

        String[] single2 = { "  " };
        Strings.trimToNullEach(single2);
        Assertions.assertNull(single2[0]);

        String[] single3 = { "xyztest" };
        Strings.stripStartEach(single3, "xyz");
        Assertions.assertEquals("test", single3[0]);
    }

    // firstNonBlank

    @Test
    public void testFirstNonBlank() {
        assertEquals("test", Strings.firstNonBlank("", " ", "test"));
        assertEquals("first", Strings.firstNonBlank("first", "second"));
        assertNull(Strings.firstNonBlank("", " ", null));
        assertNull(Strings.firstNonBlank());
    }

    @Test
    public void testFirstNonBlank_charSequence() {
        final StringBuilder blank = new StringBuilder("  \t");
        final StringBuilder hello = new StringBuilder("hello");
        final StringBuilder world = new StringBuilder("world");

        assertSame(hello, Strings.firstNonBlank(hello, world));
        assertSame(world, Strings.firstNonBlank(blank, world));
        assertSame(world, Strings.firstNonBlank(null, world));
        assertNull(Strings.firstNonBlank(blank, blank));
        assertNull(Strings.firstNonBlank((StringBuilder) null, (StringBuilder) null));

        assertSame(hello, Strings.firstNonBlank(hello, world, blank));
        assertSame(world, Strings.firstNonBlank(blank, world, hello));
        assertSame(hello, Strings.firstNonBlank(blank, null, hello));
        assertNull(Strings.firstNonBlank(blank, null, blank));

        assertSame(world, Strings.firstNonBlank(new StringBuilder[] { blank, null, world, hello }));
        assertNull(Strings.firstNonBlank(new StringBuilder[] { blank, null }));
        assertNull(Strings.firstNonBlank((StringBuilder[]) null));

        assertSame(world, Strings.firstNonBlank(Arrays.asList(blank, null, world, hello)));
        assertNull(Strings.firstNonBlank(Arrays.asList(blank, null)));
        assertNull(Strings.firstNonBlank((Iterable<StringBuilder>) null));
        assertNull(Strings.firstNonBlank(new ArrayList<StringBuilder>()));
    }

    @Test
    public void testFirstNonBlank_Iterable() {
        assertNull(Strings.firstNonBlank((Iterable<String>) null));
        assertNull(Strings.firstNonBlank(new ArrayList<>()));
        assertEquals("hello", Strings.firstNonBlank(Arrays.asList("   ", null, "hello")));
        assertNull(Strings.firstNonBlank(Arrays.asList("  ", null, "")));
    }

    @Test
    public void testFirstNonBlank_ThreeArgs() {
        assertEquals("hello", Strings.firstNonBlank("hello", "world", "!"));
        assertEquals("world", Strings.firstNonBlank("   ", "world", "!"));
        assertEquals("!", Strings.firstNonBlank("  ", "", "!"));
        assertEquals("!", Strings.firstNonBlank(null, null, "!"));
        assertNull(Strings.firstNonBlank("", "", ""));
    }

    @Test
    public void testFirstNonBlank_TwoArgs() {
        assertEquals("hello", Strings.firstNonBlank("hello", "world"));
        assertEquals("world", Strings.firstNonBlank("   ", "world"));
        assertEquals("world", Strings.firstNonBlank(null, "world"));
        assertNull(Strings.firstNonBlank("", ""));
        assertNull(Strings.firstNonBlank(null, null));
        assertNull(Strings.firstNonBlank("  ", "  "));
    }

    @Test
    public void testFirstNonBlank_VarArgs() {
        assertNull(Strings.firstNonBlank());
        assertNull(Strings.firstNonBlank((String[]) null));
        assertEquals("abc", Strings.firstNonBlank(null, "  ", "abc"));
        assertNull(Strings.firstNonBlank(null, "", "   "));
    }

    @Test
    public void testFirstNonBlank_VarargsAllBlank() {
        assertNull(Strings.firstNonBlank(null, "  ", "  ", null));
        assertNull(Strings.firstNonBlank("", " ", "\t", null));
        assertEquals("x", Strings.firstNonBlank(null, "  ", "x", "y"));
    }

    @Test
    public void testFirstNonBlankIterable() {
        assertEquals("a", Strings.firstNonBlank(list(null, " ", "\t", "a", "b")));
        assertNull(Strings.firstNonBlank(list(null, " ", "\t")));
        assertNull(Strings.firstNonBlank(new ArrayList<>()));
        assertNull(Strings.firstNonBlank((Iterable<String>) null));
    }

    @Test
    public void testFirstNonBlankVarArgs() {
        assertEquals("a", Strings.firstNonBlank(null, " ", "\t", "a", "b"));
        assertNull(Strings.firstNonBlank(null, " ", "\t"));
        assertNull(Strings.firstNonBlank());
    }

    // isAllBlank

    @Test
    public void testIsAllBlank() {
        assertTrue(Strings.isAllBlank(null, null));
        assertTrue(Strings.isAllBlank("", ""));
        assertTrue(Strings.isAllBlank("   ", "\t\n"));
        assertTrue(Strings.isAllBlank(null, "   "));
        assertFalse(Strings.isAllBlank("abc", "   "));
        assertFalse(Strings.isAllBlank("   ", "xyz"));
        assertFalse(Strings.isAllBlank("", "test"));

        assertTrue(Strings.isAllBlank(null, null, null));
        assertTrue(Strings.isAllBlank("", "", ""));
        assertTrue(Strings.isAllBlank("   ", "\t", "\n"));
        assertTrue(Strings.isAllBlank("", " ", "\t"));
        assertFalse(Strings.isAllBlank("abc", "   ", ""));
        assertFalse(Strings.isAllBlank("", "xyz", "   "));
        assertFalse(Strings.isAllBlank("", " ", "test"));
    }

    @Test
    public void testIsAllBlank_Iterable() {
        assertTrue(Strings.isAllBlank((Iterable<CharSequence>) null));
        assertTrue(Strings.isAllBlank(new ArrayList<>()));
        assertTrue(Strings.isAllBlank(Arrays.asList(null, "", "   ")));
        assertTrue(Strings.isAllBlank(list(null, " ", "\n")));
        assertFalse(Strings.isAllBlank(Arrays.asList("   ", "abc", "")));
        assertFalse(Strings.isAllBlank(list(null, "a", " ")));
    }

    @Test
    public void testIsAllBlank_Varargs() {
        assertTrue(Strings.isAllBlank());
        assertTrue(Strings.isAllBlank((CharSequence[]) null));
        assertTrue(Strings.isAllBlank((CharSequence) null));
        assertTrue(Strings.isAllBlank(null, "", "  "));
        assertTrue(Strings.isAllBlank(null, null, " ", "\t"));
        assertTrue(Strings.isAllBlank("  ", null, "", "  "));
        assertFalse(Strings.isAllBlank(null, "foo", "  "));
        assertFalse(Strings.isAllBlank("  ", "bar", null));
        assertFalse(Strings.isAllBlank("a", "b", "c", "d"));
        assertFalse(Strings.isAllBlank("  ", null, "x", "  "));
    }

    // isAnyBlank

    @Test
    public void testIsAnyBlank() {
        assertTrue(Strings.isAnyBlank(null, null));
        assertTrue(Strings.isAnyBlank("", ""));
        assertTrue(Strings.isAnyBlank("   ", "xyz"));
        assertTrue(Strings.isAnyBlank("abc", "\t\n"));
        assertTrue(Strings.isAnyBlank(null, "xyz"));
        assertTrue(Strings.isAnyBlank(" ", "test"));
        assertFalse(Strings.isAnyBlank("abc", "xyz"));
        assertFalse(Strings.isAnyBlank("hello", "world"));

        assertTrue(Strings.isAnyBlank(null, null, null));
        assertTrue(Strings.isAnyBlank("abc", "   ", "xyz"));
        assertTrue(Strings.isAnyBlank("", "def", "xyz"));
        assertTrue(Strings.isAnyBlank("abc", "def", null));
        assertTrue(Strings.isAnyBlank("hello", " ", "world"));
        assertFalse(Strings.isAnyBlank("abc", "def", "xyz"));
    }

    @Test
    public void testIsAnyBlank_Iterable() {
        assertFalse(Strings.isAnyBlank((Iterable<CharSequence>) null));
        assertFalse(Strings.isAnyBlank(new ArrayList<>()));
        assertTrue(Strings.isAnyBlank(Arrays.asList("abc", "   ", "xyz")));
        assertTrue(Strings.isAnyBlank(list("a", " ", "b")));
        assertFalse(Strings.isAnyBlank(Arrays.asList("abc", "def", "xyz")));
        assertFalse(Strings.isAnyBlank(list("a", "b")));
    }

    @Test
    public void testIsAnyBlank_Varargs() {
        assertFalse(Strings.isAnyBlank());
        assertFalse(Strings.isAnyBlank((CharSequence[]) null));
        assertTrue(Strings.isAnyBlank(null, "foo"));
        assertTrue(Strings.isAnyBlank("", "bar"));
        assertTrue(Strings.isAnyBlank("  bob  ", null));
        assertTrue(Strings.isAnyBlank(" ", "bar"));
        assertTrue(Strings.isAnyBlank("a", "b", "\t", "d"));
        assertTrue(Strings.isAnyBlank("a", "b", null, "d"));
        assertFalse(Strings.isAnyBlank("foo", "bar"));
        assertFalse(Strings.isAnyBlank("a", "b", "c", "d"));
    }

    @Test
    public void testIsBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertTrue(Strings.isBlank("   \t\n\r  "));
        assertTrue(Strings.isBlank("\t"));
        assertTrue(Strings.isBlank("\n"));
        assertTrue(Strings.isBlank(" \t\n\r\f"));
        assertFalse(Strings.isBlank("abc"));
        assertFalse(Strings.isBlank("  abc  "));
        assertFalse(Strings.isBlank("\u00A0"));
    }

    @Test
    public void testIsBlank_NonStringCharSequence() {
        assertTrue(Strings.isBlank(new StringBuilder("   ")));
        assertTrue(Strings.isBlank(new StringBuilder("\t\n\r")));
        assertTrue(Strings.isBlank(new StringBuilder(" \t\n\r\f")));
        assertFalse(Strings.isBlank(new StringBuilder("abc")));
        assertFalse(Strings.isBlank(new StringBuilder("\u00A0")));

        assertEquals(Strings.isBlank("   "), Strings.isBlank(new StringBuilder("   ")));
        assertEquals(Strings.isBlank("\t\n\r"), Strings.isBlank(new StringBuilder("\t\n\r")));
        assertEquals(Strings.isBlank("abc"), Strings.isBlank(new StringBuilder("abc")));
        assertEquals(Strings.isBlank("\u00A0"), Strings.isBlank(new StringBuilder("\u00A0")));

        final String grinningFace = "\uD83D\uDE00";
        assertFalse(Strings.isBlank(grinningFace));
        assertFalse(Strings.isBlank(new StringBuilder(grinningFace)));
        assertEquals(Strings.isBlank(grinningFace), Strings.isBlank(new StringBuilder(grinningFace)));
    }

    @Test
    public void testIsNotBlank() {
        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank(""));
        assertFalse(Strings.isNotBlank(" "));
        assertFalse(Strings.isNotBlank("   \t\n\r  "));
        assertTrue(Strings.isNotBlank("abc"));
        assertTrue(Strings.isNotBlank("  abc  "));
    }

    @Test
    public void testIsWhitespace() {
        assertTrue(Strings.isWhitespace("   "));
        assertTrue(Strings.isWhitespace("\t\n\r"));
        assertTrue(Strings.isWhitespace(" "));
        assertFalse(Strings.isWhitespace("abc"));
        assertFalse(Strings.isWhitespace(" a "));
        assertFalse(Strings.isWhitespace(" abc "));
        assertFalse(Strings.isWhitespace(null));
        assertFalse(Strings.isWhitespace(""));
    }

    // normalizeSpace

    @Test
    public void testNormalizeSpace() {
        assertEquals("a b c", Strings.normalizeSpace("a  b   c"));
        assertEquals("abc", Strings.normalizeSpace("abc"));
        assertEquals("a b", Strings.normalizeSpace("  a  b  "));
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
    }

    @Test
    public void testNormalizeSpace_EdgeCases() {
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
        assertEquals("", Strings.normalizeSpace("   "));
        assertEquals("abc", Strings.normalizeSpace("abc"));
        assertEquals("abc", Strings.normalizeSpace("  abc  "));
        assertEquals("abc def", Strings.normalizeSpace("  abc    def  "));
        assertEquals("abc def", Strings.normalizeSpace("abc\n\tdef"));
        assertEquals("a b c", Strings.normalizeSpace("  a \t b \n c  "));
    }

    @Test
    public void testNormalizeSpace_UnicodeWhitespace() {
        final String nbsp = "\u00A0";
        assertEquals("a b", Strings.normalizeSpace(nbsp + nbsp + "a" + nbsp + nbsp + "b" + nbsp));
        assertEquals("", Strings.normalizeSpace(nbsp + nbsp));
        assertEquals("foo bar", Strings.normalizeSpace(nbsp + "foo" + nbsp + "bar" + nbsp));
        // Em space (U+2003) is both whitespace and a space separator
        assertEquals("a b", Strings.normalizeSpace("a\u2003\u2003b"));
        // Ordinary single spaces still collapse runs and trim ends
        assertEquals("x y", Strings.normalizeSpace("  x   y  "));
    }

    @Test
    public void testWhitespacePredicates_normalizeSpaceAlsoUsesIsSpaceChar() {
        // normalizeSpace uses the strictly wider "isWhitespace || isSpaceChar" set, so it collapses the
        // non-breaking separators that strip/removeWhitespace/isBlank all leave alone.
        assertEquals("x", Strings.normalizeSpace(NBSP + "x" + NBSP));
        assertEquals("x", Strings.normalizeSpace(FIGURE_SP + "x" + FIGURE_SP));
        assertEquals("x", Strings.normalizeSpace(NNBSP + "x" + NNBSP));
        assertEquals("a b", Strings.normalizeSpace("a" + NBSP + FIGURE_SP + NNBSP + "b"));
        // ... and it still leaves the low control characters alone (they are neither).
        assertEquals(CTRL + "x" + CTRL, Strings.normalizeSpace(CTRL + "x" + CTRL));
        assertEquals(NEL + "x" + NEL, Strings.normalizeSpace(NEL + "x" + NEL));
    }

    @Test
    public void testOptimizedWhitespaceAndQuotePaths_ExhaustiveShortInputsAndPredicates() {
        final List<String> inputs = shortStrings(new char[] { 'a', ' ', '\t', '\u00A0', '\'', '"', '\\', '\uD800' }, 4);
        final char[] quoteChars = { '\'', '"', '\\', 'a', '\uD800' };

        for (final String input : inputs) {
            assertEquals(normalizeSpaceReference(input), Strings.normalizeSpace(input), input);
            assertEquals(removeWhitespaceReference(input), Strings.removeWhitespace(input), input);
            assertEquals(escapeQuotesReference(input, (char) 0, true), Strings.escapeQuotes(input), input);

            for (final char quoteChar : quoteChars) {
                assertEquals(escapeQuotesReference(input, quoteChar, false), Strings.escapeQuotes(input, quoteChar), input);
            }
        }

        for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                final String whitespace = new String(Character.toChars(codePoint));
                assertEquals("a b", Strings.normalizeSpace("a" + whitespace + "b"), "U+" + Integer.toHexString(codePoint));
                assertEquals("", Strings.normalizeSpace(whitespace), "U+" + Integer.toHexString(codePoint));
            }
        }

        for (int value = Character.MIN_VALUE; value <= Character.MAX_VALUE; value++) {
            final char ch = (char) value;

            if (Character.isWhitespace(ch)) {
                assertEquals("ab", Strings.removeWhitespace("a" + ch + "b"), "U+" + Integer.toHexString(value));
            }
        }

        for (int runLength = 0; runLength <= 20; runLength++) {
            final String input = "x" + "\\".repeat(runLength) + "y";
            assertEquals(escapeQuotesReference(input, '\\', false), Strings.escapeQuotes(input, '\\'), "runLength=" + runLength);
        }
    }

    @Test
    public void testRemoveWhitespace() {
        assertNull(Strings.removeWhitespace(null));
        assertEquals("", Strings.removeWhitespace(""));
        assertEquals("abc", Strings.removeWhitespace("abc"));
        assertEquals("abc", Strings.removeWhitespace("a b c"));
        assertEquals("abc", Strings.removeWhitespace("  a  b  c  "));
        assertEquals("abc", Strings.removeWhitespace("a\tb\nc"));
        assertEquals("abc", Strings.removeWhitespace("a b\tc\r\n"));
        assertEquals("abc", Strings.removeWhitespace("\t\n\r abc \t\n\r"));
    }

    @Test
    public void testWhitespacePredicates_isBlankAndRemoveWhitespaceKeepNonBreakingSeparators() {
        assertFalse(Strings.isBlank(NBSP));
        assertFalse(Strings.isBlank(FIGURE_SP));
        assertTrue(Strings.isBlank(OGHAM));
        assertTrue(Strings.isBlank(IDEO_SP));

        assertEquals(NBSP + "x" + NBSP, Strings.removeWhitespace(NBSP + "x" + NBSP));
        assertEquals(FIGURE_SP + "x", Strings.removeWhitespace(FIGURE_SP + "x" + IDEO_SP));

        // splitOnWhitespace does not treat U+00A0 as a boundary either.
        assertEquals(1, Strings.splitOnWhitespace("a" + NBSP + "b").length);
        assertEquals(2, Strings.splitOnWhitespace("a" + IDEO_SP + "b").length);
    }

    @Test
    public void testRemoveWhitespaceEach_Basic() {
        String[] array = { "a b", " x y z " };
        Strings.removeWhitespaceEach(array);
        assertArrayEquals(new String[] { "ab", "xyz" }, array);
    }

    @Test
    public void testRemoveWhitespaceEach_NullAndMixedWhitespace() {
        // null array - no exception
        Strings.removeWhitespaceEach(null);

        String[] strs = { "  hello  ", "world\t123", " h i " };
        Strings.removeWhitespaceEach(strs);
        assertEquals("hello", strs[0]);
        assertEquals("world123", strs[1]);
        assertEquals("hi", strs[2]);

        String[] withNulls = { null, "a b" };
        Strings.removeWhitespaceEach(withNulls);
        assertNull(withNulls[0]);
        assertEquals("ab", withNulls[1]);
    }

    // splitOnWhitespace

    @Test
    public void testSplitOnWhitespace() {
        assertArrayEquals(new String[] { "hello", "world", "test" }, Strings.splitOnWhitespace("hello   world\ttest"));
        assertArrayEquals(new String[] { "hello", "world" }, Strings.splitOnWhitespace("  hello   world  "));
        assertArrayEquals(new String[] { "hello", "world" }, Strings.splitOnWhitespace("  hello   world  ", true));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitOnWhitespace("a  b\tc"));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitOnWhitespace(" a  b  c ", true));
        assertArrayEquals(new String[] { "a", "b c" }, Strings.splitOnWhitespace("a b c", 2));
        assertArrayEquals(new String[] { "a", "b c" }, Strings.splitOnWhitespace("a b c", 2, true));
        assertArrayEquals(new String[] { "a b" }, Strings.splitOnWhitespace("  a b", 1));
        assertArrayEquals(new String[] { "a b" }, Strings.splitOnWhitespace("  a b", 1, true));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespace(null));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespace(""));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespace("   ", 1));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespace("   ", 1, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitOnWhitespace("a b", 0));
    }

    @Test
    public void testSplitOnWhitespace_maxKeepsWhitespaceAfterLastToken() {
        assertArrayEquals(new String[] { "a", "b " }, Strings.splitOnWhitespace("a b ", 2));
        assertArrayEquals(new String[] { "a " }, Strings.splitOnWhitespace("a ", 1));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitOnWhitespace("a b", 2));
        assertArrayEquals(new String[] { "a", "b:" }, Strings.split("a:b:", ':', 2));
    }

    @Test
    public void testC552_splitOnWhitespaceIsTheExplicitApiAndSplitRejectsNullOrEmptyDelimiter() {
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitOnWhitespace(" a \t b\n"));
        assertArrayEquals(new String[] {}, Strings.splitOnWhitespace(null));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b", ""));
    }

    @Test
    public void testSplitOnWhitespacePreserveAllTokens() {
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitOnWhitespacePreserveAllTokens("a  b"));
        assertArrayEquals(new String[] { "abc", "def" }, Strings.splitOnWhitespacePreserveAllTokens("abc def"));
        assertArrayEquals(new String[] { "" }, Strings.splitOnWhitespacePreserveAllTokens(""));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespacePreserveAllTokens(null));
        assertArrayEquals(new String[] { "a", " b c" }, Strings.splitOnWhitespacePreserveAllTokens("a  b c", 2));
        assertArrayEquals(new String[] { "", "a", "", "b", "" }, Strings.splitOnWhitespacePreserveAllTokens(" a  b ", false));
        assertArrayEquals(new String[] { "  a b" }, Strings.splitOnWhitespacePreserveAllTokens("  a b", 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitOnWhitespacePreserveAllTokens("a b", 0));
    }

    // strip

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
    public void testOptimizedWhitespaceStripAndEscapePaths_MatchReference() {
        final char[] alphabet = { 'a', 'B', '0', ' ', '\t', '\n', '\r', '\f', '\u000B', '\u001C', '\u00A0', '\u2003', '\'', '"', '\\', '_', '-', '\uD83D',
                '\uDE00' };
        final char[] quoteChars = { '\'', '"', '\\', 'a', '\uD800' };
        final String[] stripSets = { null, "", "ab", " \t", "'\"\\", "\uD83D\uDE00" };
        final Random random = new Random(20260823L);

        for (int iteration = 0; iteration < 600; iteration++) {
            final StringBuilder value = new StringBuilder();
            final int length = random.nextInt(81);

            for (int i = 0; i < length; i++) {
                value.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final String input = value.toString();
            assertEquals(normalizeSpaceReference(input), Strings.normalizeSpace(input), input);
            assertEquals(removeWhitespaceReference(input), Strings.removeWhitespace(input), input);
            assertEquals(escapeQuotesReference(input, (char) 0, true), Strings.escapeQuotes(input), input);

            for (final char quoteChar : quoteChars) {
                assertEquals(escapeQuotesReference(input, quoteChar, false), Strings.escapeQuotes(input, quoteChar), input);
            }

            for (final String stripSet : stripSets) {
                assertEquals(Strings.stripEnd(Strings.stripStart(input, stripSet), stripSet), Strings.strip(input, stripSet), input);
            }
        }

        final String normalized = "already normalized";
        final String withoutWhitespace = "alreadyNormalized";
        final String withoutQuotes = "no quote characters";
        final String withoutStripChars = "middle";
        assertTrue(normalized == Strings.normalizeSpace(normalized));
        assertTrue(withoutWhitespace == Strings.removeWhitespace(withoutWhitespace));
        assertTrue(withoutQuotes == Strings.escapeQuotes(withoutQuotes));
        assertTrue(withoutStripChars == Strings.strip(withoutStripChars, "xyz"));
        assertEquals("\\\\", Strings.escapeQuotes("\\\\", '\\'));
        assertEquals("\\\\", Strings.escapeQuotes("\\", '\\'));
        assertEquals("a b", Strings.normalizeSpace("\u00A0a\u2003\t b\u00A0"));
    }

    @Test
    public void testStrip() {
        assertEquals("abc", Strings.strip("  abc  "));
        assertEquals("abc", Strings.strip("abc"));
        assertEquals("", Strings.strip("   "));
        assertNull(Strings.strip(null));
    }

    @Test
    public void testStrip_EdgeCases() {
        assertNull(Strings.strip(null));
        assertEquals("", Strings.strip(""));
        assertEquals("", Strings.strip("   "));
        assertEquals("abc", Strings.strip("  abc  "));
    }

    @Test
    public void testStrip_WithChars() {
        assertEquals("abc", Strings.strip("xxabcxx", "x"));
        assertEquals("abc", Strings.strip("--abc--", "-"));
        assertNull(Strings.strip(null, "x"));
    }

    @Test
    public void testStrip_WithCharsEdgeCases() {
        assertEquals("abc", Strings.strip("xxabcxx", "x"));
        assertEquals("abc", Strings.strip("xyabcyx", "xy"));
        assertNull(Strings.strip(null, "x"));
    }

    @Test
    public void testStripWithChars() {
        assertEquals("abc", Strings.strip("xyabcxyz", "xyz"));
    }

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
    public void testWhitespacePredicates_stripIsCharacterIsWhitespace() {
        // strip() uses Character.isWhitespace: the mirror image of trim() at both ends of the range.
        assertEquals(CTRL + "x" + CTRL, Strings.strip(CTRL + "x" + CTRL));
        assertEquals("x", Strings.strip(OGHAM + "x" + OGHAM));
        assertEquals("x", Strings.strip(EN_QUAD + "x" + EN_QUAD));
        assertEquals("x", Strings.strip(IDEO_SP + "x" + IDEO_SP));
        // U+0085, U+00A0 and U+2007 are not Java whitespace, so strip keeps them too.
        assertEquals(NEL + "x" + NEL, Strings.strip(NEL + "x" + NEL));
        assertEquals(NBSP + "x" + NBSP, Strings.strip(NBSP + "x" + NBSP));
        assertEquals(FIGURE_SP + "x" + FIGURE_SP, Strings.strip(FIGURE_SP + "x" + FIGURE_SP));
        assertEquals(NNBSP + "x" + NNBSP, Strings.strip(NNBSP + "x" + NNBSP));
        // U+2007 is the one member of U+2000..U+200A that strip keeps, because it is non-breaking.
        for (int cp = 0x2000; cp <= 0x200A; cp++) {
            final String sp = String.valueOf((char) cp);
            assertEquals(cp == 0x2007 ? sp + "x" + sp : "x", Strings.strip(sp + "x" + sp), "U+" + Integer.toHexString(cp));
        }
    }

    // stripAccents

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

    @Test
    public void testC557_stripAccentsDocumentedExamplesStillHold() {
        assertEquals("a \u2260 b", Strings.stripAccents("a \u2260 b"));
        assertEquals("Lodz", Strings.stripAccents("\u0141\u00F3d\u017A"));
        assertEquals("\u0110\u0111\u0126\u0127\u0166\u0167\u00D8\u00F8", Strings.stripAccents("\u0110\u0111\u0126\u0127\u0166\u0167\u00D8\u00F8"));
        assertTrue(Strings.isMixedCase("\u01C5a"));
        assertFalse(StringUtils.isMixedCase("\u01C5a"));
    }

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

    @Test
    public void testStripAccentsEach() {
        String[] array = { "\u00e0bc", "xyz" };
        Strings.stripAccentsEach(array);
        assertArrayEquals(new String[] { "abc", "xyz" }, array);
    }

    @Test
    public void testStripAccentsEach_EdgeCases() {
        String[] array = { "\u00E9clair", "caf\u00E9" };
        Strings.stripAccentsEach(array);
        assertEquals("eclair", array[0]);
        assertEquals("cafe", array[1]);
    }

    // stripEach

    @Test
    public void testStripEach() {
        String[] array = { "  a  ", " b ", "c" };
        Strings.stripEach(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testStripEach_AdditionalValues() {
        String[] strs = { " a ", "  b  ", " c" };
        Strings.stripEach(strs);
        assertArrayEquals(new String[] { "a", "b", "c" }, strs);
    }

    @Test
    public void testStripEach_EdgeCases() {
        String[] array = { "  abc  ", " xyz " };
        Strings.stripEach(array);
        assertEquals("abc", array[0]);
        assertEquals("xyz", array[1]);
    }

    @Test
    public void testStripEach_WithChars() {
        String[] strs = { "xxhelloxx", "yworldy" };
        Strings.stripEach(strs, "xy");
        assertEquals("hello", strs[0]);
        assertEquals("world", strs[1]);
    }

    @Test
    public void testStripEach_WithCharsEdgeCases() {
        String[] array = { "xxabcxx", "yydefyy" };
        Strings.stripEach(array, "xy");
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
    }

    // stripEnd

    @Test
    public void testStripEnd() {
        assertEquals("  abc", Strings.stripEnd("  abc  "));
        assertEquals("abc", Strings.stripEnd("abc"));
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
    public void testStripEnd_NoArgs() {
        assertEquals("  hello", Strings.stripEnd("  hello  "));
        assertNull(Strings.stripEnd(null));
    }

    @Test
    public void testStripEnd_WithChars() {
        assertEquals("xxabc", Strings.stripEnd("xxabcxx", "x"));
        assertNull(Strings.stripEnd(null, "x"));
    }

    // stripEndEach

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
    public void testStripEndEach_EdgeCases() {
        String[] array = { "abcxx", "defyy" };
        Strings.stripEndEach(array, "xy");
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

    // stripStart

    @Test
    public void testStripStart() {
        assertEquals("abc  ", Strings.stripStart("  abc  "));
        assertEquals("abc", Strings.stripStart("abc"));
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
    public void testStripStart_NoArgs() {
        assertEquals("hello  ", Strings.stripStart("  hello  "));
        assertNull(Strings.stripStart(null));
    }

    @Test
    public void testStripStart_WithCharsEdgeCases() {
        assertEquals("abcxx", Strings.stripStart("xxabcxx", "x"));
        assertNull(Strings.stripStart(null, "x"));
    }

    // stripStartEach

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
    public void testStripStartEach_EdgeCases() {
        String[] array = { "xxabc", "yydef" };
        Strings.stripStartEach(array, "xy");
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
    }

    @Test
    public void testStripStartEach_WithChars() {
        String[] strs = { "xxhello", "yworld" };
        Strings.stripStartEach(strs, "xy");
        assertEquals("hello", strs[0]);
        assertEquals("world", strs[1]);
    }

    @Test
    public void testStripToEmpty() {
        assertEquals("abc", Strings.stripToEmpty("  abc  "));
        assertEquals("", Strings.stripToEmpty("   "));
        assertEquals("", Strings.stripToEmpty(null));
    }

    @Test
    public void testStripToEmpty_EdgeCases() {
        assertEquals("", Strings.stripToEmpty(null));
        assertEquals("", Strings.stripToEmpty(""));
        assertEquals("", Strings.stripToEmpty("   "));
        assertEquals("abc", Strings.stripToEmpty("  abc  "));
    }

    @Test
    public void testStripToEmptyEach() {
        String[] strs = { "  hello  ", null };
        Strings.stripToEmptyEach(strs);
        assertEquals("hello", strs[0]);
        assertEquals("", strs[1]);
    }

    @Test
    public void testStripToEmptyEach_EdgeCases() {
        String[] array = { "  abc  ", null };
        Strings.stripToEmptyEach(array);
        assertEquals("abc", array[0]);
        assertEquals("", array[1]);
    }

    @Test
    public void testStripToNull() {
        assertEquals("abc", Strings.stripToNull("  abc  "));
        assertNull(Strings.stripToNull("   "));
        assertNull(Strings.stripToNull(null));
    }

    @Test
    public void testStripToNull_EdgeCases() {
        assertNull(Strings.stripToNull(null));
        assertNull(Strings.stripToNull(""));
        assertNull(Strings.stripToNull("   "));
        assertEquals("abc", Strings.stripToNull("  abc  "));
    }

    @Test
    public void testStripToNullEach() {
        String[] strs = { "  hello  ", "   " };
        Strings.stripToNullEach(strs);
        assertEquals("hello", strs[0]);
        assertNull(strs[1]);
    }

    @Test
    public void testStripToNullEach_EdgeCases() {
        String[] array = { "  abc  ", "   " };
        Strings.stripToNullEach(array);
        assertEquals("abc", array[0]);
        assertNull(array[1]);
    }

    // trim

    @Test
    public void testTrim() {
        assertEquals("abc", Strings.trim("  abc  "));
        assertEquals("abc", Strings.trim("abc"));
        assertEquals("", Strings.trim("   "));
        assertNull(Strings.trim(null));
    }

    @Test
    public void testTrim_EdgeCases() {
        assertNull(Strings.trim(null));
        assertEquals("", Strings.trim(""));
        assertEquals("", Strings.trim("   "));
        assertEquals("abc", Strings.trim("  abc  "));
        assertEquals("abc", Strings.trim("abc"));
    }



    /**
     * Regression: {@link Strings#trim(String)} previously short-circuited via
     * {@link Character#isWhitespace(char)}, which is NOT equivalent to the
     * predicate used by {@link String#trim()} (chars &lt;= 0x20). Low control
     * characters such as U+0001 (SOH) and U+0005 (ENQ) are NOT considered whitespace by
     * Character#isWhitespace, but ARE removed by String#trim. The bug caused
     * Strings.trim to return the input unchanged in those cases.
     */
    @Test
    public void testTrim_LowControlCharsRegression() {
        // U+0005 ENQ is trimmed by String#trim, and is not a Java whitespace char.
        assertEquals("abc", Strings.trim("\u0005abc"));
        assertEquals("abc", Strings.trim("abc\u0005"));
        assertEquals("abc", Strings.trim("\u0001abc\u0002"));
        assertEquals("", Strings.trim("\u0001\u0002\u0003"));
        // Non-breaking space (U+00A0) is > 0x20 and NOT trimmed by String#trim.
        assertEquals("\u00A0abc\u00A0", Strings.trim("\u00A0abc\u00A0"));
    }

    @Test
    public void testWhitespacePredicates_trimIsCharLessThanOrEqual32() {
        // trim() uses String#trim's "char <= 32" rule: it removes low control characters ...
        assertEquals("x", Strings.trim(CTRL + "x" + CTRL));
        // ... but nothing above U+0020, not even characters that ARE Java whitespace.
        assertEquals(OGHAM + "x" + OGHAM, Strings.trim(OGHAM + "x" + OGHAM));
        assertEquals(EN_QUAD + "x" + EN_QUAD, Strings.trim(EN_QUAD + "x" + EN_QUAD));
        assertEquals(IDEO_SP + "x" + IDEO_SP, Strings.trim(IDEO_SP + "x" + IDEO_SP));
        assertEquals(NEL + "x" + NEL, Strings.trim(NEL + "x" + NEL));
        assertEquals(NBSP + "x" + NBSP, Strings.trim(NBSP + "x" + NBSP));
        assertEquals(FIGURE_SP + "x" + FIGURE_SP, Strings.trim(FIGURE_SP + "x" + FIGURE_SP));
    }

    // trimEach

    @Test
    public void testTrimEach() {
        String[] array = { "  a  ", " b ", "c" };
        Strings.trimEach(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testTrimEach_EdgeCases() {
        String[] array = { "  abc  ", " xyz ", null };
        Strings.trimEach(array);
        assertEquals("abc", array[0]);
        assertEquals("xyz", array[1]);
        assertNull(array[2]);
    }

    @Test
    public void testTrimEach_WithNullElement() {
        String[] arr = { "  a  ", null, "b" };
        Strings.trimEach(arr);
        assertArrayEquals(new String[] { "a", null, "b" }, arr);
    }

    @Test
    public void testTrimToEmpty() {
        assertEquals("abc", Strings.trimToEmpty("  abc  "));
        assertEquals("", Strings.trimToEmpty("   "));
        assertEquals("", Strings.trimToEmpty(null));
    }

    @Test
    public void testTrimToEmpty_EdgeCases() {
        assertEquals("", Strings.trimToEmpty(null));
        assertEquals("", Strings.trimToEmpty(""));
        assertEquals("", Strings.trimToEmpty("   "));
        assertEquals("abc", Strings.trimToEmpty("  abc  "));
    }

    @Test
    public void testTrimToEmptyEach() {
        String[] array = { "  a  ", "   ", null };
        Strings.trimToEmptyEach(array);
        assertArrayEquals(new String[] { "a", "", "" }, array);
    }

    @Test
    public void testTrimToEmptyEach_EdgeCases() {
        String[] array = { "  abc  ", null, "   " };
        Strings.trimToEmptyEach(array);
        assertEquals("abc", array[0]);
        assertEquals("", array[1]);
        assertEquals("", array[2]);
    }

    @Test
    public void testTrimToNull() {
        assertEquals("abc", Strings.trimToNull("  abc  "));
        assertNull(Strings.trimToNull("   "));
        assertNull(Strings.trimToNull(null));
    }

    @Test
    public void testTrimToNull_EdgeCases() {
        assertNull(Strings.trimToNull(null));
        assertNull(Strings.trimToNull(""));
        assertNull(Strings.trimToNull("   "));
        assertEquals("abc", Strings.trimToNull("  abc  "));
    }

    @Test
    public void testTrimToNullEach() {
        String[] array = { "  a  ", "   ", "c" };
        Strings.trimToNullEach(array);
        assertArrayEquals(new String[] { "a", null, "c" }, array);
    }

    @Test
    public void testTrimToNullEach_EdgeCases() {
        String[] array = { "  abc  ", "   ", null };
        Strings.trimToNullEach(array);
        assertEquals("abc", array[0]);
        assertNull(array[1]);
        assertNull(array[2]);
    }

    @Test
    public void test_whilespace() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("abc\n"));
        assertTrue(Strings.containsWhitespace("abc\t"));
    }

    @Test
    public void testWhitespacePredicateOverloadsAgree() {
        // Pins the class-level "Whitespace predicate" note: strip/splitOnWhitespace/removeWhitespace scan UTF-16
        // code units while isBlank/isWhitespace/mapWords scan code points, and the two select the same characters.
        for (int cp = Character.MIN_SUPPLEMENTARY_CODE_POINT; cp <= Character.MAX_CODE_POINT; cp++) {
            assertFalse(Character.isWhitespace(cp), "U+" + Integer.toHexString(cp));
        }

        for (int cp = 0; cp <= Character.MAX_VALUE; cp++) {
            assertEquals(Character.isWhitespace((char) cp), Character.isWhitespace(cp), "U+" + Integer.toHexString(cp));
        }
    }

    @Test
    public void testWhitespacePredicates_nullAndEmptyInputs() {
        assertEquals(null, Strings.trim(null));
        assertEquals(null, Strings.strip(null));
        assertEquals(null, Strings.normalizeSpace(null));
        assertEquals(null, Strings.removeWhitespace(null));
        assertEquals("", Strings.trim(""));
        assertEquals("", Strings.strip(""));
        assertEquals("", Strings.normalizeSpace(""));
        assertEquals("", Strings.removeWhitespace(""));
        assertEquals("", Strings.normalizeSpace(NBSP + FIGURE_SP));
    }

}
