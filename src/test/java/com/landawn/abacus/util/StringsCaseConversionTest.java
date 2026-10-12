package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.base64EncodeString;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.replaceFirstDouble;
import static com.landawn.abacus.util.Strings.reverse;
import static com.landawn.abacus.util.Strings.reverseDelimited;
import static com.landawn.abacus.util.Strings.rotate;
import static com.landawn.abacus.util.Strings.shuffle;
import static com.landawn.abacus.util.Strings.sort;
import static com.landawn.abacus.util.Strings.substring;
import static com.landawn.abacus.util.Strings.substringAfter;
import static com.landawn.abacus.util.Strings.substringAfterAny;
import static com.landawn.abacus.util.Strings.substringAfterLast;
import static com.landawn.abacus.util.Strings.substringBefore;
import static com.landawn.abacus.util.Strings.substringBeforeAny;
import static com.landawn.abacus.util.Strings.substringBeforeLast;
import static com.landawn.abacus.util.Strings.substringBetween;
import static com.landawn.abacus.util.Strings.substringBetweenIgnoreCase;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.u.Optional;

/** Strings caseconversion contracts, including regression cases. */
public class StringsCaseConversionTest extends StringsTestSupport {



    /** Verbatim transcription of the pre-change {@code capitalizeWords(String, String)} implementation. */
    private static String refCapitalizeWords(final String str, final String delimiter) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final String[] words = Strings.splitPreserveAllTokens(str, delimiter);

        for (int i = 0; i < words.length; i++) {
            words[i] = Strings.capitalize(words[i]);
        }

        return Strings.join(words, delimiter);
    }



    /** Verbatim transcription of the pre-change {@code capitalizeWordsFully(String, String, Collection)}. */
    private static String refCapitalizeWordsFully(final String str, final String delimiter, final Collection<String> excludedWords) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final Set<String> normalizedExcludedWords;

        if (CommonUtil.isEmpty(excludedWords)) {
            normalizedExcludedWords = null;
        } else {
            normalizedExcludedWords = new HashSet<>();

            for (final String excludedWord : excludedWords) {
                if (excludedWord != null) {
                    normalizedExcludedWords.add(excludedWord.toLowerCase(Locale.ROOT));
                }
            }
        }

        final String[] words = Strings.splitPreserveAllTokens(str, delimiter);

        for (int i = 0, len = words.length; i < len; i++) {
            final String normalizedWord = words[i].toLowerCase(Locale.ROOT);
            words[i] = i != 0 && normalizedExcludedWords != null && normalizedExcludedWords.contains(normalizedWord) ? normalizedWord
                    : Strings.capitalize(normalizedWord);
        }

        return Strings.join(words, delimiter);
    }

    private static void assertThrowsIae(final Runnable r) {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, r::run);
    }



    // B1 : camel case capitalizes the first letter OR cased code point
    private static final String CIRCLED_A_LOWER = "ⓐ";



 // ⓐ CIRCLED LATIN SMALL LETTER A (So, Other_Lowercase)
    private static final String CIRCLED_A_UPPER = "Ⓐ";



 // Ⓐ CIRCLED LATIN CAPITAL LETTER A (So, Other_Uppercase)
    private static final String ROMAN_EIGHT_LOWER = "ⅷ";



 // ⅷ SMALL ROMAN NUMERAL EIGHT (Nl, Other_Lowercase)
    private static final String ROMAN_EIGHT_UPPER = "Ⅷ";



 // Ⅷ ROMAN NUMERAL EIGHT (Nl, Other_Uppercase)
    private static final String SQUARED_A = new String(Character.toChars(0x1F130));



    // Greek: capital omicron, capital sigma, small alpha, small final sigma, small omicron
    private static final String OMICRON_SIGMA = "\u039F\u03A3";

    private static final String ALPHA = "\u03B1";

    private static final String FINAL_SIGMA = "\u03C2";

    private static final String SMALL_OMICRON = "\u03BF";

    private static final String CAPITAL_SIGMA = "\u03A3";

    private static final String SMALL_SIGMA = "\u03C3";

    protected static int indexOfIgnoreCaseReference(final String str, final String target, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);

        if (target.isEmpty()) {
            return Math.min(fromIndex, str.length());
        }

        for (int i = fromIndex, end = str.length() - target.length(); i <= end; i++) {
            if (str.regionMatches(true, i, target, 0, target.length())) {
                return i;
            }
        }

        return -1;
    }

    protected static int lastIndexOfIgnoreCaseReference(final String str, final String target, final int startIndexFromBack) {
        if (startIndexFromBack < 0 || target.length() > str.length()) {
            return -1;
        }

        if (target.isEmpty()) {
            return Math.min(startIndexFromBack, str.length());
        }

        for (int i = Math.min(startIndexFromBack, str.length() - target.length()); i >= 0; i--) {
            if (str.regionMatches(true, i, target, 0, target.length())) {
                return i;
            }
        }

        return -1;
    }

    @SuppressWarnings("deprecation")
    protected static void assertCaseFormatConvertersRejectSurrogate(final char splitChar) {
        final String value = "a\uD83D\uDE00b";
        final String expectedMessage = "splitChar must not be a UTF-16 surrogate code unit: U+" + Integer.toHexString(splitChar).toUpperCase(Locale.ROOT);

        assertEquals(expectedMessage, assertThrows(IllegalArgumentException.class, () -> Strings.toCamelCase(value, splitChar)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Strings.toUpperCamelCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toPascalCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toSnakeCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toScreamingSnakeCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toKebabCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toCamelCase(null, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toKebabCase("", splitChar));
    }

    protected static String camelCaseReference(final String str, final boolean upperCamelCase) {
        final StringBuilder result = new StringBuilder(str.length());
        final StringBuilder word = new StringBuilder();
        boolean capitalizeWord = upperCamelCase;

        for (int i = 0, len = str.length(); i < len;) {
            final int codePoint = str.codePointAt(i);
            final int charCount = Character.charCount(codePoint);

            if (codePoint == '_' || codePoint == '-' || Character.isWhitespace(codePoint)) {
                if (!word.isEmpty()) {
                    appendCamelCaseWordReference(result, word, capitalizeWord);
                    word.setLength(0);
                }

                if (!result.isEmpty()) {
                    capitalizeWord = true;
                }
            } else {
                final boolean atCaseBoundary = !word.isEmpty() && (Character.isUpperCase(codePoint) || Character.isTitleCase(codePoint))
                        && (Character.isLowerCase(str.codePointBefore(i)) || (i + charCount < len && Character.isLowerCase(str.codePointAt(i + charCount))));

                if (atCaseBoundary) {
                    appendCamelCaseWordReference(result, word, capitalizeWord);
                    word.setLength(0);
                    capitalizeWord = true;
                }

                word.appendCodePoint(codePoint);
            }

            i += charCount;
        }

        if (!word.isEmpty()) {
            appendCamelCaseWordReference(result, word, capitalizeWord);
        }

        return result.toString();
    }

    protected static void appendCamelCaseWordReference(final StringBuilder output, final CharSequence word, final boolean capitalize) {
        final String lowercaseWord = word.toString().toLowerCase(Locale.ROOT);

        if (!capitalize) {
            output.append(lowercaseWord);
            return;
        }

        for (int i = 0, len = lowercaseWord.length(); i < len;) {
            final int codePoint = lowercaseWord.codePointAt(i);
            final int charCount = Character.charCount(codePoint);

            if (Character.isLetter(codePoint)) {
                output.append(lowercaseWord, 0, i);
                output.append(lowercaseWord.substring(i, i + charCount).toUpperCase(Locale.ROOT));
                output.append(lowercaseWord, i + charCount, len);
                return;
            }

            i += charCount;
        }

        output.append(lowercaseWord);
    }

    protected static String delimitedCaseReference(final String str, final char separator, final boolean uppercase) {
        final StringBuilder result = new StringBuilder(str.length() + 16);

        for (int i = 0, len = str.length(); i < len;) {
            final int codePoint = str.codePointAt(i);
            final int charCount = Character.charCount(codePoint);

            if (codePoint == '_' || codePoint == '-' || Character.isWhitespace(codePoint)) {
                final boolean atStart = result.isEmpty();

                while (i < len) {
                    final int separatorCodePoint = str.codePointAt(i);

                    if (separatorCodePoint != '_' && separatorCodePoint != '-' && !Character.isWhitespace(separatorCodePoint)) {
                        break;
                    }

                    i += Character.charCount(separatorCodePoint);
                }

                if (!atStart && i < len) {
                    result.append(separator);
                }

                continue;
            }

            if ((Character.isUpperCase(codePoint) || Character.isTitleCase(codePoint)) && i > 0
                    && (Character.isLowerCase(str.codePointBefore(i)) || (i + charCount < len && Character.isLowerCase(str.codePointAt(i + charCount))))
                    && !result.isEmpty() && result.charAt(result.length() - 1) != separator) {
                result.append(separator);
            }

            result.appendCodePoint(codePoint);
            i += charCount;
        }

        return uppercase ? result.toString().toUpperCase(Locale.ROOT) : result.toString().toLowerCase(Locale.ROOT);
    }

    protected static String capitalizeWhitespaceWordsReference(final String str, final boolean lowercaseRemainder) {
        final StringBuilder result = new StringBuilder(str.length());

        for (int i = 0, len = str.length(); i < len;) {
            final int start = i;
            final boolean whitespace = Character.isWhitespace(str.codePointAt(i));

            do {
                i += Character.charCount(str.codePointAt(i));
            } while (i < len && Character.isWhitespace(str.codePointAt(i)) == whitespace);

            if (whitespace) {
                result.append(str, start, i);
            } else {
                final String word = str.substring(start, i);
                result.append(Strings.capitalize(lowercaseRemainder ? word.toLowerCase(Locale.ROOT) : word));
            }
        }

        return result.toString();
    }

    protected static String swapAsciiCaseReference(final String str) {
        final char[] result = str.toCharArray();

        for (int i = 0; i < result.length; i++) {
            if (result[i] >= 'A' && result[i] <= 'Z') {
                result[i] += 'a' - 'A';
            } else if (result[i] >= 'a' && result[i] <= 'z') {
                result[i] -= 'a' - 'A';
            }
        }

        return new String(result);
    }

    protected static String caseVariant(final String str) {
        final char[] result = str.toCharArray();

        for (int i = 0; i < result.length; i++) {
            if (Character.isUpperCase(result[i]) || Character.isTitleCase(result[i])) {
                result[i] = Character.toLowerCase(result[i]);
            } else if (Character.isLowerCase(result[i])) {
                result[i] = Character.toUpperCase(result[i]);
            }
        }

        return new String(result);
    }

    protected static int countMatchesIgnoreCaseReference(final String str, final String target) {
        if (str == null || str.isEmpty() || target == null || target.isEmpty()) {
            return 0;
        }

        return indicesOfIgnoreCaseReference(str, target, 0).length;
    }

    protected static int[] indicesOfIgnoreCaseReference(final String str, final String target, final int fromIndex) {
        if (str == null || target == null || target.length() > str.length()) {
            return new int[0];
        }

        final List<Integer> indices = new ArrayList<>();
        final int targetLength = target.length();
        int nextIndex = Math.max(0, fromIndex);

        if (targetLength == 0) {
            for (int i = nextIndex; i < str.length(); i++) {
                indices.add(i);
            }
        } else {
            while (nextIndex <= str.length() - targetLength) {
                final int index = indexOfIgnoreCaseReference(str, target, nextIndex);

                if (index < 0) {
                    break;
                }

                indices.add(index);
                nextIndex = index + targetLength;
            }
        }

        final int[] result = new int[indices.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = indices.get(i);
        }
        return result;
    }

    protected static String replaceIgnoreCaseReference(final String str, int fromIndex, final String target, String replacement, int max) {
        if (str == null || str.isEmpty() || target == null || target.isEmpty() || max == 0 || fromIndex >= str.length()) {
            return str;
        }

        fromIndex = Math.max(0, fromIndex);
        replacement = replacement == null ? "" : replacement;
        int end = indexOfIgnoreCaseReference(str, target, fromIndex);

        if (end < 0) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length());
        int start = fromIndex;
        result.append(str, 0, fromIndex);

        while (end >= 0) {
            result.append(str, start, end).append(replacement);
            start = end + target.length();

            if (--max == 0) {
                break;
            }

            end = indexOfIgnoreCaseReference(str, target, start);
        }

        return result.append(str, start, str.length()).toString();
    }

    @Test
    public void testAppendIfMissingIgnoreCase() {
        assertEquals("abc.txt", Strings.appendIfMissingIgnoreCase("abc", ".txt"));
        assertEquals("abc.TXT", Strings.appendIfMissingIgnoreCase("abc.TXT", ".txt"));
        assertNull(Strings.appendIfMissingIgnoreCase(null, ".txt"));
    }

    @Test
    public void testAppendPrependCaseSensitivity() {
        Assertions.assertEquals("File.TXT", Strings.appendIfMissingIgnoreCase("File", ".TXT"));
        Assertions.assertEquals("File.txt", Strings.appendIfMissingIgnoreCase("File.txt", ".TXT"));
        Assertions.assertEquals("File.TxT", Strings.appendIfMissingIgnoreCase("File.TxT", ".txt"));

        Assertions.assertEquals("HTTP://site.com", Strings.prependIfMissingIgnoreCase("site.com", "HTTP://"));
        Assertions.assertEquals("HtTp://site.com", Strings.prependIfMissingIgnoreCase("HtTp://site.com", "http://"));
    }

    @Test
    public void testCamelCaseConvertersAreNotIdempotent() {
        // Adjacent single-letter words become an acronym-shaped run that a second pass re-splits.
        assertEquals("aCA", Strings.toCamelCase("a-c a"));
        assertEquals("aCa", Strings.toCamelCase("aCA"));
        assertNotEquals(Strings.toCamelCase("a-c a"), Strings.toCamelCase(Strings.toCamelCase("a-c a")));

        assertEquals("CD", Strings.toUpperCamelCase("c-d "));
        assertEquals("Cd", Strings.toUpperCamelCase("CD"));
        assertNotEquals(Strings.toUpperCamelCase("c-d "), Strings.toUpperCamelCase(Strings.toUpperCamelCase("c-d ")));

        assertEquals("ABBaAaA", Strings.toUpperCamelCase("aBBaAaA"));
        assertEquals("AbBaAaA", Strings.toUpperCamelCase("ABBaAaA"));

        // A third application is stable: only the first re-split changes anything.
        assertEquals("aCa", Strings.toCamelCase("aCa"));
        assertEquals("Cd", Strings.toUpperCamelCase("Cd"));
    }

    @Test
    public void testCamelCaseConvertersNullEmptyAndUnicode() {
        assertEquals(null, Strings.toCamelCase(null));
        assertEquals(null, Strings.toUpperCamelCase(null));
        assertEquals("", Strings.toCamelCase(""));
        assertEquals("", Strings.toUpperCamelCase(""));
        // a supplementary code point is not a letter here, so it is carried through unchanged
        String grin = "" + HIGH_EMOJI + (char) 0xDE00;
        assertEquals(grin, Strings.toCamelCase(grin));
        assertEquals(grin, Strings.toUpperCamelCase(grin));
    }

    // capitalize

    @Test
    public void testCapitalize() {
        assertEquals("Abc", Strings.capitalize("abc"));
        assertEquals("Abc", Strings.capitalize("Abc"));
        assertEquals("ABC", Strings.capitalize("ABC"));
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
    }

    @Test
    public void testCapitalize_Edge() {
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
        assertEquals("A", Strings.capitalize("a"));
        assertEquals("Cat", Strings.capitalize("cat"));
        assertEquals("Cat", Strings.capitalize("Cat"));
    }

    @Test
    public void testCapitalize_EdgeCases() {
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
        assertEquals("A", Strings.capitalize("a"));
        assertEquals("Hello", Strings.capitalize("hello"));
        assertEquals("Hello", Strings.capitalize("Hello"));
        assertEquals("123", Strings.capitalize("123"));
        assertEquals(" abc", Strings.capitalize(" abc"));
    }

    @Test
    public void testCapitalize_uppercaseDigraphMapsToTitlecase() {
        assertEquals(String.valueOf((char) 0x01C5) + "en", Strings.capitalize(String.valueOf((char) 0x01C4) + "en"));
        assertEquals(String.valueOf((char) 0x01C8), Strings.capitalize(String.valueOf((char) 0x01C7)));
        assertEquals(String.valueOf((char) 0x01CB), Strings.capitalize(String.valueOf((char) 0x01CA)));
        assertEquals(String.valueOf((char) 0x01F2), Strings.capitalize(String.valueOf((char) 0x01F1)));
        final String upper = "Cat";
        assertSame(upper, Strings.capitalize(upper));
    }

    @Test
    public void testToCamelCase_AgreesWithCapitalizeForCasedSymbols() {
        for (final String word : new String[] { CIRCLED_A_LOWER + "bc", ROMAN_EIGHT_LOWER, ROMAN_EIGHT_UPPER, "ⅰⅱ", "ⓩ" }) {
            assertEquals(Strings.capitalize(word.toLowerCase(java.util.Locale.ROOT)), Strings.toUpperCamelCase(word), word);
            assertEquals(Strings.capitalizeWordsFully(word), Strings.toUpperCamelCase(word), word);
        }
    }

    // capitalizeWords

    @Test
    public void testCapitalizeWords() {
        assertEquals("APPLE ORange         Water", Strings.capitalizeWords("APPLE ORange         Water"));
        assertEquals("APPLE", Strings.capitalizeWords("aPPLE"));
        assertEquals("HELLO WORLD", Strings.capitalizeWords("hELLO wORLD"));
        assertEquals("Hello  World", Strings.capitalizeWords("hello  world"));
        assertEquals("Hello\tWorld", Strings.capitalizeWords("hello\tworld"));
        assertEquals("Hello\nWorld", Strings.capitalizeWords("hello\nworld"));
        assertNull(Strings.capitalizeWords(null));
        assertEquals("", Strings.capitalizeWords(""));
    }



    // D-1: capitalizeWords / capitalizeWordsFully delimiter fast path
    @Test
    public void testD1_capitalizeWords_delimiter_basics() {
        assertEquals("Hello World", Strings.capitalizeWords("hello world", " "));
        assertEquals("Hello-World", Strings.capitalizeWords("hello-world", "-"));
        assertEquals("Hello, World", Strings.capitalizeWords("hello, world", ", "));
        assertEquals("A  B", Strings.capitalizeWords("a  b", " "));
        assertEquals("HELLO WORLD", Strings.capitalizeWords("HELLO WORLD", " "));
        assertEquals("Hello World", Strings.capitalizeWordsFully("HELLO WORLD", " "));
        assertEquals("Hello World", Strings.capitalizeWordsFully("hELLo WoRLD", " "));
    }



    /**
     * The word-splitting parameter of capitalizeWords/capitalizeWordsFully is named {@code delimiter} (was
     * {@code separator}) so its validation message matches split/mapWords/reverseDelimited.
     */
    @Test
    public void testCapitalizeWords_delimiterValidationMessage() {
        String expected = assertThrows(IllegalArgumentException.class, () -> Strings.split("x", "")).getMessage();

        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords("x", "")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWordsFully("x", "")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords("x", "", Set.of("a"))).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWordsFully("x", "", Set.of("a"))).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("x", "", Fn.identity())).getMessage());
    }



    /**
     * The {@code String...} overloads of capitalizeWords/capitalizeWordsFully were removed because they were
     * ambiguous with the {@code Collection} overloads for an untyped {@code null} argument. This pins the
     * replacement call shapes (including a bare {@code null}, which must now resolve without a cast).
     */
    @Test
    public void testCapitalizeWords_excludedWordsIsCollectionOnly() {
        assertEquals("Hello World", Strings.capitalizeWords("hello world", " ", null));
        assertEquals("Hello World", Strings.capitalizeWordsFully("hELLO wORLD", " ", null));
        assertEquals("The End the", Strings.capitalizeWords("the end the", " ", Set.of("the")));
        assertEquals("End the End", Strings.capitalizeWordsFully("END THE END", " ", Set.of("the")));

        // The Collection overload accepts any Collection, not just Set.
        assertEquals("The End the", Strings.capitalizeWords("the end the", " ", List.of("the")));
        assertEquals("The End the", Strings.capitalizeWords("the end the", " ", new HashSet<>(List.of("the"))));

        assertEquals(1, N.filter(Strings.class.getMethods(), m -> "capitalizeWords".equals(m.getName()) && m.getParameterCount() == 3).size());
        assertEquals(1, N.filter(Strings.class.getMethods(), m -> "capitalizeWordsFully".equals(m.getName()) && m.getParameterCount() == 3).size());
    }

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

    @Test
    public void testCapitalizeWordsWithExcludedWords() {
        assertEquals("The Quick Brown Fox", Strings.capitalizeWords("the quick brown fox", " ", Set.of("the")));
        assertEquals("Hello and Goodbye", Strings.capitalizeWords("hello and goodbye", " ", Set.of("and")));
        assertEquals("APPLE ORange    of     WaTer Of", Strings.capitalizeWords("APPLE oRange    of     WaTer Of", " ", Set.of("of")));
        assertEquals("The First Name of the Person", Strings.capitalizeWords("the first name of the person", " ", Set.of("the", "of")));
        assertEquals("Hello World", Strings.capitalizeWords("hello world", " ", null));
        assertEquals("Hello World", Strings.capitalizeWords("hello world", " ", Set.of()));

        List<String> excludedWords = Arrays.asList("the", "and", "or");
        assertEquals("The Quick Brown Fox", Strings.capitalizeWords("the quick brown fox", " ", excludedWords));
        assertNull(Strings.capitalizeWords(null, " ", excludedWords));
    }

    @Test
    public void testCapitalizeWordsWithSeparator() {
        assertEquals("Abc-Def", Strings.capitalizeWords("abc-def", "-"));
        assertEquals("ABC_DEF", Strings.capitalizeWords("ABC_DEF", "_"));
        assertNull(Strings.capitalizeWords(null, "-"));
        assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords("abc", ""));
    }

    @Test
    public void testD1_delimiterContainingACasedLetterIsLocatedInTheOriginalString() {
        // Delimiter positions must be probed against the original input, not the partially case-folded
        // buffer, otherwise capitalizing a word start could create or destroy a delimiter match.
        assertEquals("AAA", Strings.capitalizeWords("aAa", "A"));
        assertEquals("aaa", Strings.capitalizeWords("aaa", "a"));
        // "baBab" splits on "aB" into ["b", "ab"] (the only "aB" is at index 1; the trailing "ab" is not a
        // match), which capitalizes to ["B", "Ab"] and rejoins as "B" + "aB" + "Ab". Capitalizing the leading
        // 'b' must not make the scan see a delimiter that the original string did not contain.
        assertEquals("BaBAb", Strings.capitalizeWords("baBab", "aB"));
    }

    @Test
    public void testD1_emptyWordsBetweenAdjacentDelimitersArePreserved() {
        // capitalize("") is "", so an empty word contributes nothing and the delimiters round-trip exactly.
        assertEquals(",,", Strings.capitalizeWords(",,", ","));
        assertEquals("A,,B", Strings.capitalizeWords("a,,b", ","));
        assertEquals(",A", Strings.capitalizeWords(",a", ","));
        assertEquals("A,", Strings.capitalizeWords("a,", ","));
        assertEquals(",,", Strings.capitalizeWordsFully(",,", ","));
        assertEquals("A,,B", Strings.capitalizeWordsFully("a,,B", ","));
    }

    @Test
    public void testD1_exclusionOverloadsStillUseThePerWordPath() {
        assertEquals("The End", Strings.capitalizeWords("the end", " ", Set.of("the")));
        assertEquals("The End the", Strings.capitalizeWords("the end the", " ", Set.of("the")));
        assertEquals("A  the B", Strings.capitalizeWords("a  the b", " ", Set.of("the")));
        assertEquals("The End", Strings.capitalizeWordsFully("THE END", " ", Set.of("the")));
        assertEquals("End the End", Strings.capitalizeWordsFully("END THE END", " ", Set.of("the")));
        // An empty exclusion collection must fall through to the fast path with identical output.
        assertEquals("The End", Strings.capitalizeWords("the end", " ", new ArrayList<>()));
        assertEquals("The End", Strings.capitalizeWordsFully("THE END", " ", new ArrayList<>()));
    }

    @Test
    public void testD1_fastPathMatchesTheSplitJoinReferenceExhaustively() {
        // Exhaustive over every string of length 0..5 from an alphabet that mixes a lower-case letter, an
        // upper-case letter, a neutral char and a delimiter char, against every delimiter shape that matters.
        final char[] alphabet = { 'a', 'B', 'x', ',' };
        final String[] delimiters = { ",", "x", "a", "B", ",,", "ax", "xa", "aB", ",x,", "z" };

        for (int len = 0; len <= 5; len++) {
            final int total = (int) Math.pow(alphabet.length, len);

            for (int n = 0; n < total; n++) {
                final String str = decode(n, len, alphabet);

                for (final String delimiter : delimiters) {
                    assertEquals(refCapitalizeWords(str, delimiter), Strings.capitalizeWords(str, delimiter),
                            "capitalizeWords(" + str + ", " + delimiter + ")");
                    assertEquals(refCapitalizeWordsFully(str, delimiter, null), Strings.capitalizeWordsFully(str, delimiter),
                            "capitalizeWordsFully(" + str + ", " + delimiter + ")");
                }
            }
        }
    }

    @Test
    public void testD1_fastPathMatchesTheSplitJoinReferenceOnRandomAsciiInput() {
        final char[] pool = { 'a', 'z', 'A', 'Z', '0', '9', '_', '-', ' ', '.', ',', '\t', '~' };
        final String[] delimiters = { " ", ", ", "-", "_", ".", "--", " - ", "a", "A", "0", "\t", "ab", "aa" };
        final Random rnd = new Random(20260830L);

        for (int len = 0; len <= 30; len++) {
            for (int t = 0; t < 300; t++) {
                final StringBuilder sb = new StringBuilder();

                for (int i = 0; i < len; i++) {
                    sb.append(pool[rnd.nextInt(pool.length)]);
                }

                final String str = sb.toString();
                final String delimiter = delimiters[rnd.nextInt(delimiters.length)];

                assertEquals(refCapitalizeWords(str, delimiter), Strings.capitalizeWords(str, delimiter));
                assertEquals(refCapitalizeWordsFully(str, delimiter, null), Strings.capitalizeWordsFully(str, delimiter));
            }
        }
    }

    @Test
    public void testD1_multiCharAndOverlappingDelimiters() {
        assertEquals("AXXBXXC", Strings.capitalizeWords("aXXbXXc", "XX"));
        // Non-overlapping left-to-right scan: "aba" consumes [0,3), leaving "ba" as the second word.
        assertEquals("abaBa", Strings.capitalizeWords("ababa", "aba"));
        assertEquals("XXAXX", Strings.capitalizeWords("XXaXX", "XX"));
        // A delimiter longer than the input never matches: the whole string is one word.
        assertEquals("Abc", Strings.capitalizeWords("abc", "abcd"));
    }

    @Test
    public void testD1_nonAsciiInputStillTakesTheGeneralPath() {
        // The fast path is ASCII-only because the Unicode 17 titlecase mapping is not length-preserving.
        assertEquals("Café Au Lait", Strings.capitalizeWords("café au lait", " "));
        assertEquals("Straße Test", Strings.capitalizeWords("straße test", " "));
        assertEquals("Aé,Bé", Strings.capitalizeWords("aé,bé", ","));
        // U+01F0 (small j with caron) titlecases to two code units, so it cannot be folded in place.
        assertEquals("J̌x Y", Strings.capitalizeWords("ǰx y", " "));
    }

    @Test
    public void testD1_nullAndEmptyContractUnchanged() {
        assertEquals(null, Strings.capitalizeWords(null, " "));
        assertEquals("", Strings.capitalizeWords("", " "));
        assertEquals(null, Strings.capitalizeWordsFully(null, " "));
        assertEquals("", Strings.capitalizeWordsFully("", " "));
        assertThrowsIae(() -> Strings.capitalizeWords("a b", ""));
        assertThrowsIae(() -> Strings.capitalizeWords("a b", null));
        assertThrowsIae(() -> Strings.capitalizeWordsFully("a b", ""));
        assertThrowsIae(() -> Strings.capitalizeWordsFully("a b", null));
    }

    @Test
    public void testD1_wordStartIsTheFirstCodePointNotTheFirstLetter() {
        // capitalize() titlecases only the first code point; a non-letter word start is left alone.
        assertEquals("1ab 2cd", Strings.capitalizeWords("1ab 2cd", " "));
        assertEquals("_ab", Strings.capitalizeWords("_ab", " "));
        assertEquals("1ab 2cd", Strings.capitalizeWordsFully("1AB 2CD", " "));
    }

    @Test
    public void testCapitalizeWordsFully() {
        assertEquals("Hello World", Strings.capitalizeWordsFully("hELLO wORLD"));
        assertEquals("Hello World", Strings.capitalizeWordsFully("HELLO WORLD"));
        assertEquals("Hello  World", Strings.capitalizeWordsFully("hELLO  wORLD"));
        assertEquals("Hello\tWorld", Strings.capitalizeWordsFully("hELLO\twORLD"));
        assertEquals("Hello-World", Strings.capitalizeWordsFully("hELLO-wORLD", "-"));
        assertEquals("Hello_World", Strings.capitalizeWordsFully("HELLO_WORLD", "_"));
        assertNull(Strings.capitalizeWordsFully(null));
        assertEquals("", Strings.capitalizeWordsFully(""));
        assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWordsFully("abc", ""));
    }

    @Test
    public void testCapitalizeWordsFullyWithExcludedWords() {
        assertEquals("The Lord of the Rings", Strings.capitalizeWordsFully("tHE lORD oF tHE rINGS", " ", Set.of("OF", "THE")));
        assertEquals("Of Mice and Men", Strings.capitalizeWordsFully("OF MICE AND MEN", " ", Set.of("of", "and")));
        assertEquals("Hello World", Strings.capitalizeWordsFully("hELLO wORLD", " ", null));

        List<String> excludedWords = Arrays.asList("the", "of", "and");
        assertEquals("The Lord of the Rings", Strings.capitalizeWordsFully("THE LORD OF THE RINGS", " ", excludedWords));
        assertNull(Strings.capitalizeWordsFully(null, " ", excludedWords));
    }

    @Test
    public void testCaseCodePoint_bmp() {
        assertEquals(0x0061, Strings.toLowerCase(0x0041));
        assertEquals(0x0041, Strings.toUpperCase(0x0061));

        assertEquals(0x007A, Strings.toLowerCase(0x005A));
        assertEquals(0x005A, Strings.toUpperCase(0x007A));

        // already in the target case
        assertEquals(0x0061, Strings.toLowerCase(0x0061));
        assertEquals(0x0041, Strings.toUpperCase(0x0041));

        // no case mapping at all
        assertEquals(0x0031, Strings.toLowerCase(0x0031));
        assertEquals(0x0031, Strings.toUpperCase(0x0031));
        assertEquals(0x0020, Strings.toLowerCase(0x0020));
        assertEquals(0x0020, Strings.toUpperCase(0x0020));

        // Greek
        assertEquals(0x03C9, Strings.toLowerCase(0x03A9));
        assertEquals(0x03A9, Strings.toUpperCase(0x03C9));

        // title case Dz: lowercases to dz, uppercases to DZ
        assertEquals(0x01C6, Strings.toLowerCase(0x01C5));
        assertEquals(0x01C4, Strings.toUpperCase(0x01C5));

        // Turkish dotless i uppercases to plain I
        assertEquals(0x0049, Strings.toUpperCase(0x0131));

        // Cherokee: the lowercase letters live far from the capitals
        assertEquals(0xAB70, Strings.toLowerCase(0x13A0));
    }

    @Test
    public void testCaseCodePoint_charAndIntOverloadsAgree() {
        // Character.toLowerCase(char) is a narrowing cast of Character.toLowerCase(int), so the two would part
        // company only if some BMP code point mapped to a supplementary one - none does, and this pins that.
        for (int cp = 0; cp <= Character.MAX_VALUE; cp++) {
            assertEquals(Strings.toLowerCase((char) cp), Strings.toLowerCase(cp), "U+" + Integer.toHexString(cp));
            assertEquals(Strings.toUpperCase((char) cp), Strings.toUpperCase(cp), "U+" + Integer.toHexString(cp));
        }
    }

    @Test
    public void testCaseCodePoint_charOverloadStillWins() {
        // These assignments compile only while a char argument still binds toLowerCase(char)/toUpperCase(char):
        // the int overload returns int, which does not narrow to char without a cast.
        final char lower = Strings.toLowerCase('A');
        final char upper = Strings.toUpperCase('a');
        assertEquals('a', lower);
        assertEquals('A', upper);

        // A boxed Character unboxes to char, and char is more specific than int, so it binds the char overload too.
        final Character boxed = 'A';
        final char fromBoxed = Strings.toLowerCase(boxed);
        assertEquals('a', fromBoxed);

        // A byte/short widens to int, which only the new overload accepts.
        final byte b = 'A';
        assertEquals(0x61, Strings.toLowerCase(b));
    }

    @Test
    public void testCaseCodePoint_invalid() {
        for (final int invalid : new int[] { -1, Integer.MIN_VALUE, Character.MAX_CODE_POINT + 1, Integer.MAX_VALUE }) {
            assertThrows(IllegalArgumentException.class, () -> Strings.toLowerCase(invalid));
            assertThrows(IllegalArgumentException.class, () -> Strings.toUpperCase(invalid));
        }
    }

    @Test
    public void testCaseCodePoint_matchesCharacterEverywhere() {
        for (int cp = Character.MIN_CODE_POINT; cp <= Character.MAX_CODE_POINT; cp++) {
            assertEquals(Character.toLowerCase(cp), Strings.toLowerCase(cp), "U+" + Integer.toHexString(cp));
            assertEquals(Character.toUpperCase(cp), Strings.toUpperCase(cp), "U+" + Integer.toHexString(cp));
        }
    }

    @Test
    public void testCaseCodePoint_simpleMappingDivergesFromString() {
        // U+0130 LATIN CAPITAL LETTER I WITH DOT ABOVE: the full mapping expands to "i" + U+0307.
        final String dottedCapitalI = String.valueOf((char) 0x0130);
        assertEquals(0x0069, Strings.toLowerCase(0x0130));
        assertEquals(2, Strings.toLowerCase(dottedCapitalI).length());
        assertEquals(0x0069, Strings.toLowerCase(dottedCapitalI).charAt(0));
        assertEquals(0x0307, Strings.toLowerCase(dottedCapitalI).charAt(1));

        // U+00DF LATIN SMALL LETTER SHARP S: the full mapping expands to "SS"; the simple one is a no-op.
        assertEquals(0x00DF, Strings.toUpperCase(0x00DF));
        assertEquals("SS", Strings.toUpperCase(String.valueOf((char) 0x00DF)));

        // U+FB01 LATIN SMALL LIGATURE FI: the full mapping expands to "FI"; the simple one is a no-op.
        assertEquals(0xFB01, Strings.toUpperCase(0xFB01));
        assertEquals("FI", Strings.toUpperCase(String.valueOf((char) 0xFB01)));
    }

    @Test
    public void testCaseCodePoint_supplementary() {
        // Deseret
        assertEquals(0x10428, Strings.toLowerCase(0x10400));
        assertEquals(0x10400, Strings.toUpperCase(0x10428));

        // Adlam
        assertEquals(0x1E922, Strings.toLowerCase(0x1E900));
        assertEquals(0x1E900, Strings.toUpperCase(0x1E922));

        // Warang Citi
        assertEquals(0x118C0, Strings.toLowerCase(0x118A0));
        assertEquals(0x118A0, Strings.toUpperCase(0x118C0));

        // The char overloads see only the surrogate halves of these, and surrogates have no case mapping.
        final char high = Character.highSurrogate(0x10400);
        final char low = Character.lowSurrogate(0x10400);
        assertEquals(high, Strings.toLowerCase(high));
        assertEquals(low, Strings.toLowerCase(low));
    }

    @Test
    public void testCaseCodePoint_surrogatesAndBoundaries() {
        for (int cp = Character.MIN_SURROGATE; cp <= Character.MAX_SURROGATE; cp++) {
            assertEquals(cp, Strings.toLowerCase(cp), "U+" + Integer.toHexString(cp));
            assertEquals(cp, Strings.toUpperCase(cp), "U+" + Integer.toHexString(cp));
        }

        assertEquals(Character.MIN_CODE_POINT, Strings.toLowerCase(Character.MIN_CODE_POINT));
        assertEquals(Character.MIN_CODE_POINT, Strings.toUpperCase(Character.MIN_CODE_POINT));
        assertEquals(Character.MAX_CODE_POINT, Strings.toLowerCase(Character.MAX_CODE_POINT));
        assertEquals(Character.MAX_CODE_POINT, Strings.toUpperCase(Character.MAX_CODE_POINT));
    }

    @Test
    public void testCaseConverters_fullUnicodeMappings() {
        assertEquals("STRASSE", Strings.toScreamingSnakeCase("stra\u00DFe"));
        assertEquals("i\u0307", Strings.toSnakeCase("\u0130"));
        assertEquals("\u03BF\u03C2", Strings.toSnakeCase("\u039F\u03A3"));
        assertEquals("\u03BF\u03C2Value", Strings.toCamelCase("\u039F\u03A3_VALUE"));
        // A capitalized word's first letter uses the Unicode 17 titlecase mapping ("Ss"), as capitalize() does,
        // not String.toUpperCase ("SS"). Changed in the 2026-09-02 review; see StringsReviewFixes20260902Test.
        assertEquals("SsetaName", Strings.toUpperCamelCase("\u00DFeta_name"));
    }

    @Test
    public void testCaseConverters_splitOnCaseBoundaries() {
        // Case-boundary splitting is shared with toSnakeCase; invertibility is not.
        assertEquals("xmlParser", Strings.toCamelCase("XMLParser"));
        assertEquals("XmlParser", Strings.toUpperCamelCase("XMLParser"));
        assertEquals("helloWorldApi", Strings.toCamelCase("helloWorldAPI"));
        assertEquals("fooBarBaz", Strings.toCamelCase("foo_barBaz"));
        // An all-caps single word: the first letter is cased per camel/upper-camel, the rest lowercased.
        assertEquals("user", Strings.toCamelCase("USER"));
        assertEquals("User", Strings.toUpperCamelCase("USER"));
    }

    @Test
    public void testCaseConverters_SupplementaryUnicode() {
        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));

        assertEquals(deseretSmall + "Value", Strings.toCamelCase(deseretCapital + "Value"));
        assertEquals(deseretCapital + "Value", Strings.toUpperCamelCase(deseretSmall + "Value"));
        assertEquals(deseretSmall + "_value", Strings.toSnakeCase(deseretSmall + "Value"));
        assertEquals(deseretCapital + "_VALUE", Strings.toScreamingSnakeCase(deseretSmall + "Value"));
        assertEquals(deseretSmall + "-value", Strings.toKebabCase(deseretSmall + "Value"));
    }

    @Test
    public void testCaseConverters_TitlecaseBoundary() {
        // U+01C5 is a Unicode titlecase letter; its uppercase/lowercase forms are U+01C4/U+01C6.
        final String input = "a\u01C5b";

        // The camel-case forms capitalize the second word with the titlecase mapping (U+01C5), as capitalize()
        // does, not the uppercase mapping (U+01C4). Changed in the 2026-09-02 review; see StringsReviewFixes20260902Test.
        assertEquals("a\u01C5b", Strings.toCamelCase(input));
        assertEquals("A\u01C5b", Strings.toUpperCamelCase(input));
        assertEquals("a_\u01C6b", Strings.toSnakeCase(input));
        assertEquals("A_\u01C4B", Strings.toScreamingSnakeCase(input));
        assertEquals("a-\u01C6b", Strings.toKebabCase(input));
    }

    @Test
    public void testCaseConverters_WhitespaceIsDelimiter() {
        // camel/upperCamel already split on whitespace (via regex); now snake/kebab/screaming do too
        assertEquals("firstName", Strings.toCamelCase("first name"));
        assertEquals("FirstName", Strings.toUpperCamelCase("first name"));
        assertEquals("first_name", Strings.toSnakeCase("first name"));
        assertEquals("first-name", Strings.toKebabCase("first name"));
        assertEquals("FIRST_NAME", Strings.toScreamingSnakeCase("first name"));

        // capitalized words separated by a space
        assertEquals("hello_world", Strings.toSnakeCase("Hello World"));
        assertEquals("hello-world", Strings.toKebabCase("Hello World"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("Hello World"));

        // consecutive whitespace collapses to a single separator (no duplicate separators)
        assertEquals("hello_world", Strings.toSnakeCase("hello  world"));
        assertEquals("hello-world", Strings.toKebabCase("hello  world"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("hello  world"));

        // tab counts as whitespace
        assertEquals("a_b", Strings.toSnakeCase("a\tb"));
        assertEquals("a-b", Strings.toKebabCase("a\tb"));

        // leading/trailing separator runs are dropped
        assertEquals("hello", Strings.toSnakeCase(" hello"));
        assertEquals("hello", Strings.toKebabCase(" hello"));
        assertEquals("hello", Strings.toSnakeCase("hello "));
        assertEquals("hello", Strings.toKebabCase("hello "));
        assertEquals("HELLO", Strings.toScreamingSnakeCase("hello "));
        assertEquals("hello", Strings.toSnakeCase("hello"));
        assertEquals("hello", Strings.toKebabCase("hello"));

        // mixed whitespace + existing delimiter, still single separator
        assertEquals("a_b_c", Strings.toSnakeCase("a-b c"));
        assertEquals("a-b-c", Strings.toKebabCase("a_b c"));

        // char overloads also handle whitespace (via delegation to the no-arg form)
        assertEquals("a_b_c", Strings.toSnakeCase("a.b c", '.'));
        assertEquals("a-b-c", Strings.toKebabCase("a.b c", '.'));
        assertEquals("A_B_C", Strings.toScreamingSnakeCase("a.b c", '.'));
    }

    @Test
    public void testCaseFormatSplitChar_rejectsSurrogateCodeUnits() {
        assertCaseFormatConvertersRejectSurrogate('\uD83D');
        assertCaseFormatConvertersRejectSurrogate('\uDE00');
    }



    /**
     * Characterization tests for Unicode case mapping and exclusion matching.
     */
    @Test
    public void testCaseMappingAndExclusionMatching() {
        // toUpperCamelCase, capitalize and capitalizeWords all capitalize with the Unicode 17 TITLECASE mapping
        // (U+01C5). Before the 2026-09-02 review toUpperCamelCase used the UPPERCASE mapping (U+01C4) and this
        // test pinned that difference; see StringsReviewFixes20260902Test.
        assertEquals("ǅen", Strings.toUpperCamelCase("ǆen"));
        assertEquals("ǅen", Strings.capitalize("ǆen"));
        assertEquals("ǅen", Strings.capitalizeWords("ǆen"));
        assertEquals("ǅen", Strings.capitalizeWordsFully("ǄEN"));

        // excludedWords matching is case-sensitive regardless of a caller-supplied Set's comparator.
        Set<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("the");

        assertEquals("A THE B", Strings.mapWords("a The b", " ", caseInsensitive, String::toUpperCase));
        assertEquals("A THE B", Strings.mapWords("a The b", " ", new HashSet<>(List.of("the")), String::toUpperCase));
        assertEquals("A THE B", Strings.capitalizeWords("a tHE b", " ", caseInsensitive));
    }

    @Test
    public void testCharCaseOverloadsUseSimpleMapping() {
        // Documented divergence: a char cannot hold a length-changing mapping, so the char overloads use
        // Character.toLowerCase/toUpperCase while the String overloads apply the full Locale.ROOT mapping.
        assertEquals('i', Strings.toLowerCase((char) 0x0130)); // simple: one char
        assertEquals("i̇", Strings.toLowerCase(Character.toString(0x0130))); // full: expands to two

        assertEquals((char) 0x00DF, Strings.toUpperCase((char) 0x00DF)); // simple: unchanged
        assertEquals("SS", Strings.toUpperCase(Character.toString(0x00DF))); // full: expands to two

        // Where no expansion applies the two agree.
        for (final char ch : new char[] { 'a', 'Z', '1', ' ', (char) 0x03A9, (char) 0x03C9 }) {
            assertEquals(String.valueOf(Strings.toLowerCase(ch)), Strings.toLowerCase(String.valueOf(ch)));
            assertEquals(String.valueOf(Strings.toUpperCase(ch)), Strings.toUpperCase(String.valueOf(ch)));
        }
    }

    @Test
    public void testCollaboratorNullValidationOrder() {
        // Primary-first: null/empty primary bypasses locale
        assertNull(Strings.toLowerCase(null, null));
        assertEquals("", Strings.toLowerCase("", null));
        assertThrows(NullPointerException.class, () -> Strings.toLowerCase("A", null));
        assertNull(Strings.toUpperCase(null, null));
        assertEquals("", Strings.toUpperCase("", null));
        assertThrows(NullPointerException.class, () -> Strings.toUpperCase("a", null));

        // The random generator is validated before no-op returns.
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle("a", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle("ab", null));

        // Primary-first Base64 charset collaborators
        assertEquals("", Strings.base64EncodeString(null, null));
        assertEquals("", Strings.base64EncodeString("", null));
        assertThrows(NullPointerException.class, () -> Strings.base64EncodeString("a", null));
        assertEquals("", Strings.base64DecodeToString(null, null));
        assertEquals("", Strings.base64DecodeToString("", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64DecodeToString("!!!!", null));
        assertThrows(NullPointerException.class, () -> Strings.base64DecodeToString("YQ==", null));
        assertEquals("", Strings.base64UrlDecodeToString(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlDecodeToString("!!!!", null));

        // Optional charset (null → UTF-8) for URL query helpers
        assertEquals("", Strings.encodeUrlQuery(null, null));
        assertNotNull(Strings.parseUrlQuery("a=b", (java.nio.charset.Charset) null));
        assertTrue(Strings.parseUrlQuery("", (java.nio.charset.Charset) null).isEmpty());

        // Collaborator-first: target type always required
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("a=b", (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("", (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("", null, null));

        // Collaborator-first: mapper always required
        assertThrows(IllegalArgumentException.class, () -> Strings.mapWords(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("a b", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.mapWords(null, "", String::toUpperCase));
        assertNull(Strings.mapWords(null, " ", String::toUpperCase));
        assertEquals("", Strings.mapWords("", " ", String::toUpperCase));

        final java.util.function.Function<String, String> nullMapper = null;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("a", nullMapper)).getMessage().contains("mapper"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("a", " ", nullMapper)).getMessage().contains("mapper"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("a", " ", Collections.emptyList(), nullMapper)).getMessage()
                .contains("mapper"));

        // Collaborator-first: suppliers always required
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("x", (java.util.function.Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("x", (java.util.function.Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("x", (java.util.function.Supplier<String>) null));

        // Collaborator-first: IntUnaryOperator always required
        assertThrows(IllegalArgumentException.class, () -> Strings.substring(null, 0, (java.util.function.IntUnaryOperator) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween(null, 0, (java.util.function.IntUnaryOperator) null));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, Strings.compareIgnoreCase(null, null));
        assertEquals(0, Strings.compareIgnoreCase("", ""));
        assertEquals(0, Strings.compareIgnoreCase("test", "TEST"));
        assertEquals(0, Strings.compareIgnoreCase("Hello", "hello"));
        assertEquals(0, Strings.compareIgnoreCase("MiXeD", "mixed"));
        assertEquals(0, Strings.compareIgnoreCase("hello", "HELLO"));
        assertEquals(0, Strings.compareIgnoreCase("abc", "ABC"));

        assertTrue(Strings.compareIgnoreCase(null, "test") < 0);
        assertTrue(Strings.compareIgnoreCase("test", null) > 0);
        assertTrue(Strings.compareIgnoreCase(null, "abc") < 0);
        assertTrue(Strings.compareIgnoreCase("abc", null) > 0);
        assertTrue(Strings.compareIgnoreCase(null, "a") < 0);
        assertTrue(Strings.compareIgnoreCase("a", null) > 0);

        assertTrue(Strings.compareIgnoreCase("apple", "BANANA") < 0);
        assertTrue(Strings.compareIgnoreCase("a", "B") < 0);
        assertTrue(Strings.compareIgnoreCase("BANANA", "apple") > 0);
        assertTrue(Strings.compareIgnoreCase("B", "a") > 0);
        assertTrue(Strings.compareIgnoreCase("a", "b") < 0);
        assertTrue(Strings.compareIgnoreCase("b", "a") > 0);
        assertTrue(Strings.compareIgnoreCase("abc", "def") < 0);
        assertTrue(Strings.compareIgnoreCase("def", "abc") > 0);

        assertTrue(Strings.compareIgnoreCase("", "test") < 0);
        assertTrue(Strings.compareIgnoreCase("test", "") > 0);
        assertTrue(Strings.compareIgnoreCase("abc", "ABCD") < 0);
        assertTrue(Strings.compareIgnoreCase("ABCD", "abc") > 0);
    }

    @Test
    public void testContainsAllIgnoreCase() {
        assertTrue(Strings.containsAllIgnoreCase("AbCdEfG", "abc", "def"));
        assertFalse(Strings.containsAllIgnoreCase("abcdef", "abc", "xyz"));
        assertFalse(Strings.containsAllIgnoreCase(null, "abc"));
    }

    @Test
    public void testContainsAnyIgnoreCase() {
        assertTrue(Strings.containsAnyIgnoreCase("AbCdEf", "abc", "xyz"));
        assertFalse(Strings.containsAnyIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.containsAnyIgnoreCase(null, "abc"));
    }

    @Test
    public void testContainsAnyIgnoreCase_MultipleValues() {
        assertTrue(Strings.containsAnyIgnoreCase("Hello World", "WORLD", "foo"));
        assertTrue(Strings.containsAnyIgnoreCase("Hello World", "hello"));
        assertFalse(Strings.containsAnyIgnoreCase("Hello World", "xyz", "abc", "def"));
        assertFalse(Strings.containsAnyIgnoreCase(null, "hello"));
        assertFalse(Strings.containsAnyIgnoreCase("hello"));
        // two-argument variant
        assertTrue(Strings.containsAnyIgnoreCase("Hello", "HELLO", "WORLD"));
        assertFalse(Strings.containsAnyIgnoreCase("Hello", "XYZ", "ABC"));
        // three or more values (uses toLowerCase path)
        assertTrue(Strings.containsAnyIgnoreCase("Hello World", "WORLD", "FOO", "BAR"));
        assertFalse(Strings.containsAnyIgnoreCase("Hello World", "XXX", "YYY", "ZZZ"));
    }

    @Test
    public void testContainsIgnoreCase() {
        assertTrue(Strings.containsIgnoreCase("AbCdEf", "abc"));
        assertTrue(Strings.containsIgnoreCase("abcdef", "DEF"));
        assertFalse(Strings.containsIgnoreCase("abc", "xyz"));
        assertFalse(Strings.containsIgnoreCase(null, "abc"));
    }

    @Test
    public void testContainsIgnoreCase_EdgeCases() {
        assertTrue(Strings.containsIgnoreCase("ABC", "abc"));
        assertTrue(Strings.containsIgnoreCase("abc", "BC"));
        assertFalse(Strings.containsIgnoreCase("abc", "xy"));
        assertFalse(Strings.containsIgnoreCase(null, "a"));
    }

    @Test
    public void testContainsIgnoreCase_WithDelimiter() {
        assertTrue(StrUtil.containsTokenIgnoreCase("a,B,c", "b", ","));
        assertFalse(StrUtil.containsTokenIgnoreCase("a,b,c", "d", ","));
        assertFalse(StrUtil.containsTokenIgnoreCase(null, "b", ","));
    }

    @Test
    public void testContainsIgnoreCaseWithDelimiter() {
        Assertions.assertTrue(StrUtil.containsTokenIgnoreCase("Apple,Banana,Orange", "banana", ","));
        Assertions.assertTrue(StrUtil.containsTokenIgnoreCase("ONE TWO THREE", "two", " "));
        Assertions.assertTrue(StrUtil.containsTokenIgnoreCase("One-Two-Three", "TWO", "-"));

        Assertions.assertFalse(StrUtil.containsTokenIgnoreCase("Apple,Banana,Orange", "BAN", ","));

        Assertions.assertFalse(StrUtil.containsTokenIgnoreCase(null, "test", ","));
        Assertions.assertFalse(StrUtil.containsTokenIgnoreCase("test", null, ","));
    }

    @Test
    public void testContainsNoneIgnoreCase() {
        assertTrue(Strings.containsNoneIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.containsNoneIgnoreCase("AbCdEf", "abc", "xyz"));
        assertTrue(Strings.containsNoneIgnoreCase(null, "abc"));
        assertFalse(Strings.containsNoneIgnoreCase("", ""));
        assertTrue(Strings.containsAnyIgnoreCase("", ""));
        assertTrue(Strings.containsNoneIgnoreCase("", "a"));
        assertFalse(Strings.containsNoneIgnoreCase("", "a", ""));
        assertFalse(Strings.containsNoneIgnoreCase("x", ""));
        assertEquals(!Strings.containsAnyIgnoreCase("", ""), Strings.containsNoneIgnoreCase("", ""));
        assertEquals(!Strings.containsAnyIgnoreCase("x", ""), Strings.containsNoneIgnoreCase("x", ""));
        assertTrue(Strings.containsNoneIgnoreCase(null, ""));
        assertFalse(Strings.containsAnyIgnoreCase(null, ""));
        assertEquals(!Strings.containsAnyIgnoreCase(null, ""), Strings.containsNoneIgnoreCase(null, ""));
        assertEquals(!Strings.containsAnyIgnoreCase(null, "abc"), Strings.containsNoneIgnoreCase(null, "abc"));
    }

    @Test
    public void testConvertWords() {
        assertEquals("HELLO WORLD", Strings.mapWords("hello world", String::toUpperCase));
        assertEquals("abc def", Strings.mapWords("ABC DEF", String::toLowerCase));
        assertEquals("hello!\tworld!", Strings.mapWords("hello\tworld", s -> s + "!"));
        assertNull(Strings.mapWords(null, String::toUpperCase));
    }

    @Test
    public void testConvertWords_EdgeCases() {
        assertNull(Strings.mapWords(null, String::toUpperCase));
        assertEquals("", Strings.mapWords("", String::toUpperCase));
        assertEquals("HELLO WORLD", Strings.mapWords("hello world", String::toUpperCase));
        assertEquals("HELLO-WORLD", Strings.mapWords("hello-world", "-", String::toUpperCase));
    }

    @Test
    public void testConvertWords_WithDelimiter() {
        assertEquals("HELLO-WORLD", Strings.mapWords("hello-world", "-", String::toUpperCase));
        assertNull(Strings.mapWords(null, "-", String::toUpperCase));
    }

    @Test
    public void testConvertWords_WithDelimiterAndExcludedWords() {
        String result = Strings.mapWords("hello world", " ", Arrays.asList("world"), s -> s.toUpperCase());
        assertNotNull(result);
        assertTrue(result.contains("HELLO"));
        assertTrue(result.contains("world"));
    }

    @Test
    public void testConvertWords_WithExcludedWords() {
        List<String> excluded = Arrays.asList("the", "and");
        assertEquals("HELLO the WORLD", Strings.mapWords("hello the world", " ", excluded, String::toUpperCase));
    }

    @Test
    public void testConvertWords_WithExcludedWords_FullCoverage() {
        // with excluded words
        List<String> excluded = Arrays.asList("of", "the");
        assertEquals("TOWER_of_LONDON_the_CITY", Strings.mapWords("tower_of_london_the_city", "_", excluded, String::toUpperCase));

        // null/empty string
        assertNull(Strings.mapWords(null, "_", excluded, String::toUpperCase));
        assertEquals("", Strings.mapWords("", "_", excluded, String::toUpperCase));

        // no excluded words
        assertEquals("HELLO_WORLD", Strings.mapWords("hello_world", "_", null, String::toUpperCase));

        // empty excluded words
        assertEquals("HELLO_WORLD", Strings.mapWords("hello_world", "_", Collections.emptyList(), String::toUpperCase));
    }

    @Test
    public void testConvertWordsEdgeCases() {
        Assertions.assertEquals("HELLO", Strings.mapWords("hello", " ", null, String::toUpperCase));

        Function<String, String> reverser = s -> new StringBuilder(s).reverse().toString();
        Assertions.assertEquals("olleh dlrow", Strings.mapWords("hello world", " ", null, reverser));

        Set<String> allExcluded = new HashSet<>(Arrays.asList("hello", "world"));
        Assertions.assertEquals("hello world", Strings.mapWords("hello world", " ", allExcluded, String::toUpperCase));
    }

    @Test
    public void testCountMatchesIgnoreCase() {
        // normal (same-case)
        assertEquals(3, Strings.countMatchesIgnoreCase("abcabcabc", "abc"));
        // case-insensitive match
        assertEquals(3, Strings.countMatchesIgnoreCase("abcABCabc", "abc"));
        assertEquals(2, Strings.countMatchesIgnoreCase("Hello HELLO", "hello"));
        // non-overlapping
        assertEquals(2, Strings.countMatchesIgnoreCase("aAaA", "aa"));
        // not found
        assertEquals(0, Strings.countMatchesIgnoreCase("test", "xyz"));
        // null/empty
        assertEquals(0, Strings.countMatchesIgnoreCase(null, "test"));
        assertEquals(0, Strings.countMatchesIgnoreCase("test", null));
        assertEquals(0, Strings.countMatchesIgnoreCase("", "a"));
        assertEquals(0, Strings.countMatchesIgnoreCase("test", ""));
    }

    @Test
    public void testDelimitedCaseConvertersAreIdempotent() {
        for (final String input : new String[] { "a-c a", "c-d ", "aBBaAaA", "helloWorldAPI", "  x__y  ", "a", "", "" + HIGH_EMOJI + LOW_MAN + "aB" }) {
            assertEquals(Strings.toSnakeCase(input), Strings.toSnakeCase(Strings.toSnakeCase(input)), "toSnakeCase: " + input);
            assertEquals(Strings.toKebabCase(input), Strings.toKebabCase(Strings.toKebabCase(input)), "toKebabCase: " + input);
            assertEquals(Strings.toScreamingSnakeCase(input), Strings.toScreamingSnakeCase(Strings.toScreamingSnakeCase(input)),
                    "toScreamingSnakeCase: " + input);
        }
    }

    @Test
    public void testDelimitedCaseConvertersNotIdempotentForUnpairedCasedCodePoints() {
        // U+211D DOUBLE-STRUCK CAPITAL R is uppercase but has no lowercase mapping
        final String capitalR = String.valueOf((char) 0x211D);
        assertEquals("x" + capitalR, Strings.toSnakeCase("X" + capitalR));
        assertEquals("x_" + capitalR, Strings.toSnakeCase("x" + capitalR));
        assertEquals("x" + capitalR, Strings.toKebabCase("X" + capitalR));
        assertEquals("x-" + capitalR, Strings.toKebabCase("x" + capitalR));

        // U+0138 LATIN SMALL LETTER KRA is lowercase but has no uppercase mapping
        final String kra = String.valueOf((char) 0x0138);
        assertEquals("AX" + kra, Strings.toScreamingSnakeCase("ax" + kra));
        assertEquals("A_X" + kra, Strings.toScreamingSnakeCase("AX" + kra));
    }

    @Test
    public void testEdgeCases() {
        assertFalse(Character.isLetter('\u24D0'));
        assertFalse(Character.isLetter('\u24B6'));
        assertTrue(Strings.isAllLowerCase("a\u24D0"));
        assertFalse(Strings.isAllUpperCase("a\u24D0"));
        assertTrue(Strings.isAllUpperCase("A\u24B6"));
        assertFalse(Strings.isAllLowerCase("A\u24B6"));

        String longString = "a".repeat(10000);
        assertEquals(10000, reverse(longString).length());
        assertEquals(10000, sort(longString).length());

        String unicode = "Hello 世界 🌍";
        assertNotNull(reverse(unicode));
        assertNotNull(sort(unicode));

        String special = "!@#$%^&*()_+-=[] {}|;:'\",.<>?/\\`~";
        assertNotNull(reverse(special));
        assertNotNull(sort(special));
    }

    @Test
    public void testEndsWithAnyIgnoreCase() {
        assertTrue(Strings.endsWithAnyIgnoreCase("AbCdEf", "def", "xyz"));
        assertTrue(Strings.endsWithAnyIgnoreCase("Hello", "LLO", "test"));
        assertTrue(Strings.endsWithAnyIgnoreCase("TEST", "xyz", "test"));
        assertTrue(Strings.endsWithAnyIgnoreCase("MiXeD", "abc", "XED"));
        assertTrue(Strings.endsWithAnyIgnoreCase("ABC", "xy", "bc"));

        assertFalse(Strings.endsWithAnyIgnoreCase("abcdef", "xyz", "123"));
        assertFalse(Strings.endsWithAnyIgnoreCase("abc", "xy", "cd"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "def"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "a", "b"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "bc"));
    }

    @Test
    public void testEndsWithIgnoreCase() {
        assertTrue(Strings.endsWithIgnoreCase("AbCdEf", "def"));
        assertTrue(Strings.endsWithIgnoreCase("Hello", "LLO"));
        assertTrue(Strings.endsWithIgnoreCase("TEST", "test"));
        assertTrue(Strings.endsWithIgnoreCase("MiXeD", "XED"));
        assertTrue(Strings.endsWithIgnoreCase("abc", "ABC"));
        assertTrue(Strings.endsWithIgnoreCase("ABC", "bc"));
        assertTrue(Strings.endsWithIgnoreCase("abc", "BC"));

        assertFalse(Strings.endsWithIgnoreCase("abcdef", "xyz"));
        assertFalse(Strings.endsWithIgnoreCase("abc", "ab"));
        assertFalse(Strings.endsWithIgnoreCase(null, "suffix"));
        assertFalse(Strings.endsWithIgnoreCase("test", null));
        assertFalse(Strings.endsWithIgnoreCase(null, null));
        assertFalse(Strings.endsWithIgnoreCase(null, "def"));
    }

    @Test
    public void testContainsAnyEqualsAnyIgnoreCase_consistentAcrossArity() {
        // regression: the >= 3 element paths used whole-string toLowerCase + skip-empty, disagreeing
        // with the 1/2-element fast paths that use containsIgnoreCase/equalsIgnoreCase
        assertTrue(Strings.containsAnyIgnoreCase("test", "", "zz"));
        assertTrue(Strings.containsAnyIgnoreCase("test", "", "zz", "qq")); // was false
        assertFalse(Strings.containsAnyIgnoreCase("test", "x", "y", "z"));
        assertTrue(Strings.containsAnyIgnoreCase("test", "x", "y", "ES"));

        assertTrue(Strings.equalsAnyIgnoreCase("ſ", "x", "S"));
        assertTrue(Strings.equalsAnyIgnoreCase("ſ", "x", "y", "S")); // was false
        assertTrue(Strings.equalsAnyIgnoreCase(null, "x", "y", null));
        assertFalse(Strings.equalsAnyIgnoreCase(null, "x", "y", "z"));
    }

    @Test
    public void testEqualsAnyIgnoreCase() {
        assertTrue(Strings.equalsAnyIgnoreCase("abc", "ABC", "xyz"));
        assertTrue(Strings.equalsAnyIgnoreCase("test", "HELLO", "TEST", "world"));
        assertTrue(Strings.equalsAnyIgnoreCase("ABC", "xyz", "abc"));
        assertTrue(Strings.equalsAnyIgnoreCase("Hello", "HELLO", "world"));
        assertTrue(Strings.equalsAnyIgnoreCase("test", "TEST"));
        assertTrue(Strings.equalsAnyIgnoreCase("test", "hello", "TEST"));
        assertTrue(Strings.equalsAnyIgnoreCase("abc", "XYZ", "ABC", "DEF"));
        assertTrue(Strings.equalsAnyIgnoreCase("hello", "HELLO", "world", "foo"));
        assertTrue(Strings.equalsAnyIgnoreCase(null, "a", null, "b"));
        assertTrue(Strings.equalsAnyIgnoreCase("hello", "HELLO", "WORLD"));
        assertTrue(Strings.equalsAnyIgnoreCase("hello", "HELLO"));

        assertFalse(Strings.equalsAnyIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.equalsAnyIgnoreCase(null, "abc"));
        assertFalse(Strings.equalsAnyIgnoreCase("test"));
        assertFalse(Strings.equalsAnyIgnoreCase("test", (String[]) null));
        assertFalse(Strings.equalsAnyIgnoreCase("test", "HELLO", "WORLD", "ABC"));
        assertFalse(Strings.equalsAnyIgnoreCase("xyz", "A", "B", "C"));
        assertFalse(Strings.equalsAnyIgnoreCase("abc", "xyz", "def"));
        assertFalse(Strings.equalsAnyIgnoreCase("hello", "world", "foo", "bar"));
        assertFalse(Strings.equalsAnyIgnoreCase(null, "a", "b", "c"));
        assertFalse(Strings.equalsAnyIgnoreCase("hello", "WORLD", "FOO"));
        assertFalse(Strings.equalsAnyIgnoreCase("hello"));
    }

    @Test
    public void testEqualsIgnoreCase() {
        assertTrue(Strings.equalsIgnoreCase("abc", "ABC"));
        assertTrue(Strings.equalsIgnoreCase(null, null));
        assertTrue(Strings.equalsIgnoreCase("test", "TEST"));
        assertTrue(Strings.equalsIgnoreCase("Hello", "hello"));
        assertTrue(Strings.equalsIgnoreCase("MiXeD", "mixed"));
        assertTrue(Strings.equalsIgnoreCase("", ""));

        assertFalse(Strings.equalsIgnoreCase("abc", "xyz"));
        assertFalse(Strings.equalsIgnoreCase("abc", null));
        assertFalse(Strings.equalsIgnoreCase(null, "test"));
        assertFalse(Strings.equalsIgnoreCase("test", null));
        assertFalse(Strings.equalsIgnoreCase("hello", "world"));
        assertFalse(Strings.equalsIgnoreCase("abc", "def"));
        assertFalse(Strings.equalsIgnoreCase(null, "abc"));
    }

    @Test
    public void test_format_propName() {
        String str = Strings.toSnakeCase("ME_#A3C_AAA_1A2");
        assertEquals("me_#a3c_aaa_1a2", str);

        str = Strings.toScreamingSnakeCase("me_#a3c_aaa_1a2");
        assertEquals("ME_#A3C_AAA_1A2", str);

        str = Strings.toCamelCase("me_#a3c_aaa_1a2");
        assertEquals("me#A3cAaa1A2", str);

        str = Strings.toCamelCase("ME_#A3C_AAA_1A2");
        assertEquals("me#A3cAaa1A2", str);

        assertEquals("xml_parser", Strings.toSnakeCase("XmlParser"));
        assertEquals("xml_parser", Strings.toSnakeCase("xmlPARSER"));
        assertEquals("io_error", Strings.toSnakeCase("IOError"));
        assertEquals("io_error", Strings.toSnakeCase("ioERROR"));
        assertEquals("hello_world_api", Strings.toSnakeCase("helloWorldAPI"));

        assertEquals("XML_PARSER", Strings.toScreamingSnakeCase("XmlParser"));
        assertEquals("XML_PARSER", Strings.toScreamingSnakeCase("xmlPARSER"));
        assertEquals("IO_ERROR", Strings.toScreamingSnakeCase("IOError"));
        assertEquals("IO_ERROR", Strings.toScreamingSnakeCase("ioERROR"));
        assertEquals("HELLO_WORLD_API", Strings.toScreamingSnakeCase("helloWorldAPI"));
    }

    @Test
    public void testIgnoreCaseKmp_ExactCrossoverAndBmpFoldEquivalence() throws ReflectiveOperationException {
        final Method crossover = Strings.class.getDeclaredMethod("shouldUseIgnoreCaseKmp", int.class, int.class);
        crossover.setAccessible(true);
        assertFalse((boolean) crossover.invoke(null, 255, 16));
        assertTrue((boolean) crossover.invoke(null, 256, 16));
        assertFalse((boolean) crossover.invoke(null, 4096, 15));

        final String target = "a".repeat(15) + "b";
        final String belowCrossover = "a".repeat(254) + "B";
        final String atCrossover = "x" + belowCrossover;

        for (final String input : new String[] { belowCrossover, atCrossover }) {
            assertEquals(indexOfIgnoreCaseReference(input, target, 0), Strings.indexOfIgnoreCase(input, target));
            assertEquals(lastIndexOfIgnoreCaseReference(input, target, input.length()), Strings.lastIndexOfIgnoreCase(input, target));
            assertEquals(countMatchesIgnoreCaseReference(input, target), Strings.countMatchesIgnoreCase(input, target));
            assertArrayEquals(indicesOfIgnoreCaseReference(input, target, 0), Strings.indicesOfIgnoreCase(input, target).toArray());
            assertEquals(replaceIgnoreCaseReference(input, 0, target, "<match>", -1), Strings.replaceAllIgnoreCase(input, target, "<match>"));
        }

        final String filler = "\0".repeat(240);

        for (int value = Character.MIN_VALUE; value <= Character.MAX_VALUE; value++) {
            final char ch = (char) value;
            final char folded = Character.toLowerCase(Character.toUpperCase(ch));
            final String input = filler + String.valueOf(ch).repeat(16);
            final String foldedTarget = String.valueOf(folded).repeat(16);
            assertEquals(indexOfIgnoreCaseReference(input, foldedTarget, 0), Strings.indexOfIgnoreCase(input, foldedTarget), "U+" + Integer.toHexString(value));
        }

        final String loneHighSurrogateTarget = ("\uD800a").repeat(8);
        final String malformedInput = "x".repeat(240) + ("\uD800A").repeat(8) + "\uDC00";
        assertEquals(indexOfIgnoreCaseReference(malformedInput, loneHighSurrogateTarget, 0),
                Strings.indexOfIgnoreCase(malformedInput, loneHighSurrogateTarget));
        assertEquals(lastIndexOfIgnoreCaseReference(malformedInput, loneHighSurrogateTarget, malformedInput.length()),
                Strings.lastIndexOfIgnoreCase(malformedInput, loneHighSurrogateTarget));
    }

    @Test
    public void testIgnoreCaseKmpPipeline_RandomizedDifferential() {
        final char[] alphabet = { 'a', 'A', 'b', 'B', 'c', 'C', 'I', 'i', '\u0130', '\u0131', '\u03A3', '\u03C3', '\u03C2', '\u017F', 'S', 's', '\u212A', 'K',
                'k', '0' };
        final int[] maximumReplacements = { -1, 0, 1, 2, 5 };
        final Random random = new Random(20260826L);

        for (int iteration = 0; iteration < 180; iteration++) {
            final String target = randomString(random, alphabet, 16 + random.nextInt(33));
            final StringBuilder inputBuilder = new StringBuilder(randomString(random, alphabet, 256 + random.nextInt(257)));

            if ((iteration & 1) == 0) {
                final int matchIndex = random.nextInt(inputBuilder.length() - target.length() + 1);
                inputBuilder.replace(matchIndex, matchIndex + target.length(), caseVariant(target));
            }

            final String input = inputBuilder.toString();
            final int fromIndex = random.nextInt(input.length() + 41) - 20;
            final int startIndexFromBack = random.nextInt(input.length() + 41) - 20;
            final int max = maximumReplacements[random.nextInt(maximumReplacements.length)];
            final String replacement = iteration % 7 == 0 ? null : "<" + iteration + ">";

            assertEquals(indexOfIgnoreCaseReference(input, target, fromIndex), Strings.indexOfIgnoreCase(input, target, fromIndex), "iteration " + iteration);
            assertEquals(lastIndexOfIgnoreCaseReference(input, target, startIndexFromBack), Strings.lastIndexOfIgnoreCase(input, target, startIndexFromBack),
                    "iteration " + iteration);
            assertEquals(countMatchesIgnoreCaseReference(input, target), Strings.countMatchesIgnoreCase(input, target), "iteration " + iteration);
            assertArrayEquals(indicesOfIgnoreCaseReference(input, target, fromIndex), Strings.indicesOfIgnoreCase(input, target, fromIndex).toArray(),
                    "iteration " + iteration);
            assertEquals(replaceIgnoreCaseReference(input, fromIndex, target, replacement, max),
                    Strings.replaceIgnoreCase(input, fromIndex, target, replacement, max), "iteration " + iteration);
        }

        final String targetLength15 = "a".repeat(14) + "b";
        final String targetLength16 = "a".repeat(15) + "b";
        final String adversarial = "a".repeat(300) + "B";
        assertEquals(indexOfIgnoreCaseReference(adversarial, targetLength15, 0), Strings.indexOfIgnoreCase(adversarial, targetLength15));
        assertEquals(indexOfIgnoreCaseReference(adversarial, targetLength16, 0), Strings.indexOfIgnoreCase(adversarial, targetLength16));
        assertEquals(lastIndexOfIgnoreCaseReference(adversarial, targetLength16, adversarial.length()),
                Strings.lastIndexOfIgnoreCase(adversarial, targetLength16));

        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));
        final String supplementaryInput = ("x" + deseretCapital).repeat(200);
        final String supplementaryTarget = ("X" + deseretSmall).repeat(20);
        assertEquals(countMatchesIgnoreCaseReference(supplementaryInput, supplementaryTarget),
                Strings.countMatchesIgnoreCase(supplementaryInput, supplementaryTarget));
        assertEquals(replaceIgnoreCaseReference(supplementaryInput, 0, supplementaryTarget, "z", -1),
                Strings.replaceAllIgnoreCase(supplementaryInput, supplementaryTarget, "z"));
    }

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

    // indexOfIgnoreCase



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
    public void testIndexOfIgnoreCase() {
        assertEquals(0, Strings.indexOfIgnoreCase("AbCdEf", "abc"));
        assertEquals(3, Strings.indexOfIgnoreCase("abcDEF", "def"));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "xyz"));
        assertEquals(-1, Strings.indexOfIgnoreCase(null, "abc"));
    }

    @Test
    public void testIndexOfIgnoreCase_LargeKmpPathMatchesRegionMatches() {
        final String adversarialTarget = "a".repeat(256) + "b";
        final String adversarialInput = "a".repeat(5000) + "B";
        assertEquals(4744, Strings.indexOfIgnoreCase(adversarialInput, adversarialTarget));
        assertEquals(4744, Strings.lastIndexOfIgnoreCase(adversarialInput, adversarialTarget));
        assertEquals(-1, Strings.indexOfIgnoreCase(adversarialInput, "a".repeat(256) + "c"));

        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));
        final String supplementaryInput = "x".repeat(300) + deseretCapital.repeat(20);
        final String supplementaryTarget = deseretSmall.repeat(20);
        assertEquals(indexOfIgnoreCaseReference(supplementaryInput, supplementaryTarget, 0),
                Strings.indexOfIgnoreCase(supplementaryInput, supplementaryTarget));
        assertEquals(lastIndexOfIgnoreCaseReference(supplementaryInput, supplementaryTarget, supplementaryInput.length()),
                Strings.lastIndexOfIgnoreCase(supplementaryInput, supplementaryTarget));

        final char[] alphabet = { 'I', 'i', '\u0130', '\u0131', '\u03A3', '\u03C3', '\u03C2', '\u212A', 'K', 'k', 'x', '\uD800', '\uDC00' };
        final Random random = new Random(20260821L);

        for (int iteration = 0; iteration < 100; iteration++) {
            final StringBuilder input = new StringBuilder(320);
            final StringBuilder target = new StringBuilder(20);

            for (int i = 0; i < 320; i++) {
                input.append(alphabet[random.nextInt(alphabet.length)]);
            }

            for (int i = 0; i < 20; i++) {
                target.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final int fromIndex = random.nextInt(340) - 10;
            final int startIndexFromBack = random.nextInt(340) - 10;
            assertEquals(indexOfIgnoreCaseReference(input.toString(), target.toString(), fromIndex),
                    Strings.indexOfIgnoreCase(input.toString(), target.toString(), fromIndex));
            assertEquals(lastIndexOfIgnoreCaseReference(input.toString(), target.toString(), startIndexFromBack),
                    Strings.lastIndexOfIgnoreCase(input.toString(), target.toString(), startIndexFromBack));
        }
    }

    @Test
    public void testIndexOfIgnoreCase_sameLengthCompareNotCaseFold() {
        // regionMatches(true) is a same-length UTF-16 compare, not Unicode case folding.
        assertEquals(-1, Strings.indexOfIgnoreCase("stra\u00dfe", "SS"));
        assertEquals(-1, Strings.indexOfIgnoreCase("SS", "\u00df"));
        assertFalse(Strings.containsIgnoreCase("stra\u00dfe", "SS"));
        assertEquals(0, Strings.countMatchesIgnoreCase("stra\u00dfe", "SS"));
        assertEquals("stra\u00dfe", Strings.replaceIgnoreCase("stra\u00dfe", 0, "SS", "ss", -1));
        // ASCII same-length ignore-case still matches.
        assertEquals(0, Strings.indexOfIgnoreCase("SS", "ss"));
    }

    @Test
    public void testIndexOfIgnoreCase_WithFromIndex() {
        assertEquals(3, Strings.indexOfIgnoreCase("AbCaBc", "abc", 1));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "abc", 5));
    }

    @Test
    public void testIndexOfIgnoreCase_WithFromIndex_FullCoverage() {
        // basic found
        assertEquals(6, Strings.indexOfIgnoreCase("hello WORLD", "world", 0));
        assertEquals(6, Strings.indexOfIgnoreCase("hello WORLD", "WORLD", 3));

        // null inputs
        assertEquals(-1, Strings.indexOfIgnoreCase(null, "world", 0));
        assertEquals(-1, Strings.indexOfIgnoreCase("hello", null, 0));

        // fromIndex beyond length
        assertEquals(-1, Strings.indexOfIgnoreCase("hello", "ell", 3));

        // negative fromIndex treated as 0
        assertEquals(1, Strings.indexOfIgnoreCase("hello", "ELL", -5));

        // short string optimization path (<= 3 chars, case-insensitive same)
        assertEquals(1, Strings.indexOfIgnoreCase("hello", "ell", 0));
    }

    @Test
    public void testIndexOfIgnoreCase_WithDelimiter() {
        int result = StrUtil.indexOfTokenIgnoreCase("a,B,c", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase(null, "b", ","));
    }

    @Test
    public void testIndexOfIgnoreCase_WithDelimiterAndFromIndex() {
        int result = StrUtil.indexOfTokenIgnoreCase("a,B,c,b", "b", ",", 3);
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase(null, "b", ",", 0));
    }

    @Test
    public void testIndexOfIgnoreCase_WithDelimiterAndFromIndex_FullCoverage() {
        // C-538: a null delimiter is rejected
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "APPLE", (String) null, 0));

        // C-538: an empty delimiter is rejected
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "APPLE", "", 0));

        // with delimiter, found at start
        assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ",", 0));

        // with delimiter, found at end
        assertEquals(10, StrUtil.indexOfTokenIgnoreCase("pineapple,apple", "apple", ",", 0));

        // null source
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase(null, "apple", ",", 0));

        // null valueToFind
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("apple", null, ",", 0));

        // not found
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("hello world", "test", ",", 0));

        // with fromIndex
        assertEquals(6, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ",", 3));

        // targetLen < substrLen
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("ab", "longer-string", ",", 0));
    }

    @Test
    public void testIndexOfIgnoreCaseComplexScenarios() {
        Assertions.assertEquals(7, StrUtil.indexOfTokenIgnoreCase("apple, APPLE, banana", "apple", ", ", 1));

        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("test,value", "st,va", ","));

        Assertions.assertEquals(1, StrUtil.indexOfTokenIgnoreCase(",apple,", "apple", ","));
    }

    @Test
    public void testIndexOfIgnoreCaseWithDelimiter() {
        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ","));
        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "APPLE", ","));
        Assertions.assertEquals(10, StrUtil.indexOfTokenIgnoreCase("pineapple,apple", "apple", ","));
        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("test", "test", ","));

        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase(null, "test", ","));
        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("test", null, ","));

        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfTokenIgnoreCase("test", "test", "")); // C-538

        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("apple banana", "apple", ","));
    }

    @Test
    public void testIndexOfIgnoreCaseWithDelimiterAndFromIndex() {
        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ",", 0));
        Assertions.assertEquals(6, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ",", 5));
        Assertions.assertEquals(10, StrUtil.indexOfTokenIgnoreCase("pineapple,apple", "apple", ",", 0));
        Assertions.assertEquals(11, StrUtil.indexOfTokenIgnoreCase("test value test", "test", " ", 5));

        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,banana", "apple", ",", -5));

        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("apple", "apple", ",", 10));
    }

    @Test
    public void testIndicesOfIgnoreCase() {
        {
            int[] indices = Strings.indicesOfIgnoreCase("AbCaBc", "abc").toArray();
            assertArrayEquals(new int[] { 0, 3 }, indices);
            int[] indices2 = Strings.indicesOfIgnoreCase("abc", "xyz").toArray();
            assertArrayEquals(new int[0], indices2);

            int[] indices3 = Strings.indicesOfIgnoreCase("abc", "").toArray();
            assertArrayEquals(new int[] { 0, 1, 2 }, indices3);

            int[] indices4 = Strings.indicesOfIgnoreCase("abc", null).toArray();
            assertEquals(0, indices4.length);
        }

        {
            int[] indices = Strings.indicesOfIgnoreCase("AbCaBc", "abc", 1).toArray();
            assertArrayEquals(new int[] { 3 }, indices);
            int[] indices2 = Strings.indicesOfIgnoreCase("abc", "xyz", 1).toArray();
            assertArrayEquals(new int[0], indices2);

            int[] indices3 = Strings.indicesOfIgnoreCase("abc", "", 1).toArray();
            assertArrayEquals(new int[] { 1, 2 }, indices3);

            int[] indices4 = Strings.indicesOfIgnoreCase("abc", null, 1).toArray();
            assertEquals(0, indices4.length);
        }
    }

    @Test
    public void testIndicesOfIgnoreCase_WithFromIndex() {
        int[] indices = Strings.indicesOfIgnoreCase("aAbAa", "a", 2).toArray();
        assertNotNull(indices);
        assertTrue(indices.length >= 1);
    }

    @Test
    public void testIsAllLowerCase() {
        assertTrue(Strings.isAllLowerCase("abc"));
        assertTrue(Strings.isAllLowerCase("a"));
        assertFalse(Strings.isAllLowerCase("Abc"));
        assertFalse(Strings.isAllLowerCase("ABC"));
        assertFalse(Strings.isAllLowerCase("abc123"));
        assertFalse(Strings.isAllLowerCase(null));
    }

    @Test
    public void testIsAllLowerCase_empty_isVacuouslyTrue() {
        // An empty CharSequence has no code point that is not lowercase, so the universal
        // quantifier is vacuously satisfied. null is a missing input, not an empty one.
        assertTrue(Strings.isAllLowerCase(""));
        assertTrue(Strings.isAllLowerCase(new StringBuilder()));
        assertTrue(Strings.isAllLowerCase(new StringBuilder("ab").subSequence(1, 1)));
        assertFalse(Strings.isAllLowerCase(null));

        // ... and it is simultaneously vacuously all-uppercase, which is only consistent for empty input.
        assertTrue(Strings.isAllUpperCase(""));
        assertFalse(Strings.isMixedCase(""));

        // The sibling character-class predicates deliberately keep returning false for empty input.
        assertFalse(Strings.isAlpha(""));
        assertFalse(Strings.isNumeric(""));
        assertFalse(Strings.isAsciiPrintable(""));
        assertFalse(Strings.isWhitespace(""));
    }

    @Test
    public void testIsAllUpperCase() {
        assertTrue(Strings.isAllUpperCase("ABC"));
        assertTrue(Strings.isAllUpperCase("A"));
        assertFalse(Strings.isAllUpperCase("Abc"));
        assertFalse(Strings.isAllUpperCase("abc"));
        assertFalse(Strings.isAllUpperCase("ABC123"));
        assertFalse(Strings.isAllUpperCase(null));
    }

    @Test
    public void testIsAllUpperCase_empty_isVacuouslyTrue() {
        assertTrue(Strings.isAllUpperCase(""));
        assertTrue(Strings.isAllUpperCase(new StringBuilder()));
        assertTrue(Strings.isAllUpperCase(new StringBuilder("AB").subSequence(2, 2)));
        assertFalse(Strings.isAllUpperCase(null));
    }

    @Test
    public void testIsAsciiLowerCase() {
        assertTrue(Strings.isAsciiLowerCase('a'));
        assertTrue(Strings.isAsciiLowerCase('z'));
        assertFalse(Strings.isAsciiLowerCase('A'));
        assertFalse(Strings.isAsciiLowerCase('1'));
    }

    @Test
    public void testIsAsciiUpperCase() {
        assertTrue(Strings.isAsciiUpperCase('A'));
        assertTrue(Strings.isAsciiUpperCase('Z'));
        assertFalse(Strings.isAsciiUpperCase('a'));
        assertFalse(Strings.isAsciiUpperCase('1'));
    }

    @Test
    public void testIsJavaKeyword() {
        assertTrue(Strings.isJavaKeyword("class"));
        assertTrue(Strings.isJavaKeyword("public"));
        assertTrue(Strings.isJavaKeyword("if"));
        assertTrue(Strings.isJavaKeyword("return"));
        assertTrue(Strings.isJavaKeyword("void"));
        assertTrue(Strings.isJavaKeyword("abstract"));
        assertTrue(Strings.isJavaKeyword("int"));
        assertTrue(Strings.isJavaKeyword("for"));

        assertFalse(Strings.isJavaKeyword("Class"));
        assertFalse(Strings.isJavaKeyword("hello"));
        assertFalse(Strings.isJavaKeyword("myVar"));
        assertFalse(Strings.isJavaKeyword(null));
        assertFalse(Strings.isJavaKeyword(""));
    }

    @Test
    public void testIsJavaKeyword_RestrictedIdentifiers() {
        assertFalse(Strings.isJavaKeyword("var"));
        assertFalse(Strings.isJavaKeyword("yield"));
        assertFalse(Strings.isJavaKeyword("record"));
        assertFalse(Strings.isJavaKeyword("when"));
        assertEquals(javax.lang.model.SourceVersion.isKeyword("when"), Strings.isJavaKeyword("when"));
    }

    @Test
    public void testIsLowerCase() {
        assertTrue(Strings.isLowerCase('a'));
        assertTrue(Strings.isLowerCase('z'));
        assertFalse(Strings.isLowerCase('A'));
        assertFalse(Strings.isLowerCase('1'));
    }

    @Test
    public void testIsMixedCase() {
        assertTrue(Strings.isMixedCase("Abc"));
        assertTrue(Strings.isMixedCase("aBc"));
        assertTrue(Strings.isMixedCase("aB"));
        assertFalse(Strings.isMixedCase("abc"));
        assertFalse(Strings.isMixedCase("ABC"));
        assertFalse(Strings.isMixedCase("123"));
        assertFalse(Strings.isMixedCase("a"));
        assertFalse(Strings.isMixedCase("B"));
        assertFalse(Strings.isMixedCase("a黎"));
        assertFalse(Strings.isMixedCase("黎B"));
        assertFalse(Strings.isMixedCase(null));
        assertFalse(Strings.isMixedCase(""));
    }

    @Test
    public void testIsMixedCase_TitleCase() {
        final String titleCaseDz = "ǅ";
        assertFalse(Character.isUpperCase(titleCaseDz.codePointAt(0)));
        assertTrue(Character.isTitleCase(titleCaseDz.codePointAt(0)));

        assertTrue(Strings.isMixedCase(titleCaseDz + "a"));
        assertTrue(Strings.isMixedCase("a" + titleCaseDz));
        assertTrue(Strings.isMixedCase("x" + titleCaseDz + "1"));
        assertFalse(Strings.isMixedCase(titleCaseDz));
        assertFalse(Strings.isMixedCase(titleCaseDz + titleCaseDz));
        assertFalse(Strings.isMixedCase(titleCaseDz + "A"));
        assertFalse(Strings.isAllUpperCase(titleCaseDz));
        assertFalse(Strings.isAllLowerCase(titleCaseDz));

        final String supplementaryLower = new String(Character.toChars(0x10597));
        assertTrue(Strings.isMixedCase(titleCaseDz + supplementaryLower));
    }

    @Test
    public void testIsUpperCase() {
        assertTrue(Strings.isUpperCase('A'));
        assertTrue(Strings.isUpperCase('Z'));
        assertFalse(Strings.isUpperCase('a'));
        assertFalse(Strings.isUpperCase('1'));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        assertEquals(3, Strings.lastIndexOfIgnoreCase("AbCaBc", "abc"));
        assertEquals(0, Strings.lastIndexOfIgnoreCase("ABC", "abc"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase(null, "abc"));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithStartIndex() {
        assertEquals(0, Strings.lastIndexOfIgnoreCase("AbCaBc", "abc", 2));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("abc", "xyz", 5));
    }

    @Test
    public void testLastIndexOfIgnoreCase_DelimiterSkipsEmbeddedToken() {
        assertEquals(10, StrUtil.lastIndexOfTokenIgnoreCase("pineapple,APPLE", "apple", ","));
        assertEquals(0, StrUtil.lastIndexOfTokenIgnoreCase("apple,pineapple", "apple", ",", 8));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("pineapple", "apple", ","));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiter() {
        int result = StrUtil.lastIndexOfTokenIgnoreCase("a,B,c,b", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase(null, "b", ","));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiter_respectsStartIndexBound() {
        // Regression: the VALUE's position (not the leading delimiter's) must not exceed startIndexFromBack,
        // matching the case-sensitive lastIndexOf(String,String,String,int) twin. The IgnoreCase variant
        // previously omitted the "- delimiterLen" start-bound adjustment and wrongly returned a positive index.
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase(",a", "a", ",", 0)); // "a" is at index 1, beyond bound 0
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("xy,z", "z", ",", 2)); // "z" is at index 3, beyond bound 2

        // Parity with the case-sensitive twin for the same inputs.
        assertEquals(StrUtil.lastIndexOfToken(",a", "a", ",", 0), StrUtil.lastIndexOfTokenIgnoreCase(",a", "a", ",", 0));
        assertEquals(StrUtil.lastIndexOfToken("xy,z", "z", ",", 2), StrUtil.lastIndexOfTokenIgnoreCase("xy,z", "z", ",", 2));

        // When the bound includes the value's position, the (case-insensitive) match is found.
        assertEquals(1, StrUtil.lastIndexOfTokenIgnoreCase(",A", "a", ",", 1));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiterAndFromBack() {
        int result = StrUtil.lastIndexOfTokenIgnoreCase("a,B,c,b", "b", ",", 10);
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase(null, "b", ",", 0));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiterAndStartIndex() {
        // C-538: an empty delimiter is rejected (a plain search is Strings.lastIndexOfIgnoreCase)
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.lastIndexOfTokenIgnoreCase("hello world", "WORLD", "", 20));

        // null str returns -1
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase(null, "world", ",", 20));

        // null valueToFind returns -1
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("hello world", null, ",", 20));

        // negative startIndex returns -1
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("hello world", "world", ",", -1));

        // value found at end boundary (no trailing delimiter)
        assertEquals(6, StrUtil.lastIndexOfTokenIgnoreCase("hello,world", "WORLD", ",", 20));

        // value not present
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("hello,world", "xyz", ",", 20));
    }

    @Test
    public void testLocaleSensitive() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            // No-arg toLowerCase uses Locale.ROOT, not the JVM default locale.
            assertEquals("i", Strings.toLowerCase("I"));
            assertEquals("i", Strings.toLowerCase("I", Locale.ENGLISH));
            assertEquals("ı", Strings.toLowerCase("I", new Locale("tr", "TR")));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    // mapWords

    @Test
    public void testC503_mapWordsDelimiterOverloadsRejectNullMapperResult() {
        final NullPointerException e1 = assertThrows(NullPointerException.class, () -> Strings.mapWords("a-b", "-", w -> null));
        assertEquals("mapper returned null", e1.getMessage());

        final NullPointerException e2 = assertThrows(NullPointerException.class, () -> Strings.mapWords("a-b", "-", List.of("zz"), w -> null));
        assertEquals("mapper returned null", e2.getMessage());

        final NullPointerException e3 = assertThrows(NullPointerException.class, () -> Strings.mapWords("a b", w -> null));
        assertEquals("mapper returned null", e3.getMessage());

        // a mapper that returns null only for a word that is excluded is never called for it
        assertEquals("A-b", Strings.mapWords("a-b", "-", List.of("b"), w -> "b".equals(w) ? null : w.toUpperCase()));
        assertEquals("A-B", Strings.mapWords("a-b", "-", Strings::toUpperCase));
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

    @Test
    public void testU2005_mapWordsWhitespaceOverloadRejectsNullMapperResult() {
        final NullPointerException e = assertThrows(NullPointerException.class, () -> Strings.mapWords("a b", w -> null));
        assertEquals("mapper returned null", e.getMessage());

        assertEquals("A B", Strings.mapWords("a b", String::toUpperCase));
        assertNull(Strings.mapWords(null, w -> null));
        assertEquals("", Strings.mapWords("", w -> null));
    }

    @Test
    public void testOptimizedAsciiCasePaths_ExhaustiveShortInputs() {
        final List<String> inputs = shortStrings(new char[] { 'a', 'A', 'b', 'B', '0', '_', '-', ' ', '\t', '.' }, 4);

        for (final String input : inputs) {
            assertEquals(camelCaseReference(input, false), Strings.toCamelCase(input), input);
            assertEquals(camelCaseReference(input, true), Strings.toUpperCamelCase(input), input);
            assertEquals(delimitedCaseReference(input, '_', false), Strings.toSnakeCase(input), input);
            assertEquals(delimitedCaseReference(input, '_', true), Strings.toScreamingSnakeCase(input), input);
            assertEquals(delimitedCaseReference(input, '-', false), Strings.toKebabCase(input), input);
            assertEquals(capitalizeWhitespaceWordsReference(input, false), Strings.capitalizeWords(input), input);
            assertEquals(capitalizeWhitespaceWordsReference(input, true), Strings.capitalizeWordsFully(input), input);
            assertEquals(swapAsciiCaseReference(input), Strings.swapCase(input), input);

            final String customDelimiterInput = input.replace('.', '-');
            assertEquals(camelCaseReference(customDelimiterInput, false), Strings.toCamelCase(input, '.'), input);
            assertEquals(camelCaseReference(customDelimiterInput, true), Strings.toUpperCamelCase(input, '.'), input);
        }

        // capitalizeWords capitalizes the first code point, not the first later letter.
        assertEquals("2bETA !cat ABC", Strings.capitalizeWords("2bETA !cat aBC"));
        assertEquals("2beta !cat Abc", Strings.capitalizeWordsFully("2bETA !cat aBC"));
        // Camel case intentionally uses the first letter after a separator.
        assertEquals("2Beta", Strings.toCamelCase("2_beta"));
        assertEquals("2Beta", Strings.toUpperCamelCase("2_beta"));
    }

    @Test
    public void testOptimizedAsciiCasePaths_MatchUnicodeReference() {
        final char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_- \t\n\r\f.!@#$%^&*()".toCharArray();
        final Random random = new Random(20260822L);

        for (int iteration = 0; iteration < 750; iteration++) {
            final StringBuilder value = new StringBuilder();
            final int length = random.nextInt(97);

            for (int i = 0; i < length; i++) {
                value.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final String input = value.toString();
            assertEquals(camelCaseReference(input, false), Strings.toCamelCase(input), input);
            assertEquals(camelCaseReference(input, true), Strings.toUpperCamelCase(input), input);
            assertEquals(delimitedCaseReference(input, '_', false), Strings.toSnakeCase(input), input);
            assertEquals(delimitedCaseReference(input, '_', true), Strings.toScreamingSnakeCase(input), input);
            assertEquals(delimitedCaseReference(input, '-', false), Strings.toKebabCase(input), input);
            assertEquals(capitalizeWhitespaceWordsReference(input, false), Strings.capitalizeWords(input), input);
            assertEquals(capitalizeWhitespaceWordsReference(input, true), Strings.capitalizeWordsFully(input), input);
            assertEquals(swapAsciiCaseReference(input), Strings.swapCase(input), input);
        }

        final String punctuationOnly = "0123_- !";
        assertTrue(punctuationOnly == Strings.swapCase(punctuationOnly));

        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));
        assertEquals(deseretSmall + " VALUE", Strings.swapCase(deseretCapital + " value"));
        assertEquals(deseretSmall + "Value", Strings.toCamelCase(deseretCapital + "Value"));
    }

    @Test
    public void testPerformanceOptimized() {
        String str = "abc";

        assertEquals(0, Strings.indexOfIgnoreCase("ABC", "abc"));
        assertEquals(0, Strings.indexOfIgnoreCase("ABC", "ABC"));
    }

    @Test
    public void testPrependIfMissingIgnoreCase() {
        assertEquals("http://abc", Strings.prependIfMissingIgnoreCase("abc", "http://"));
        assertEquals("HTTP://abc", Strings.prependIfMissingIgnoreCase("HTTP://abc", "http://"));
        assertNull(Strings.prependIfMissingIgnoreCase(null, "http://"));
    }

    @Test
    public void testC023_randomizedIgnoreCaseAgainstRegionMatches() {
        final Random rnd = new Random(23);
        final String[] pieces = { "a", "B", "s", "S", "\u017F", "K", "\u212A", "i", "I", "\u0130", "\u0131", "\u00DF", "\u03C3", "\u03C2", "\u03A3", EMOJI,
                "\uD801\uDC00", "\uD801\uDC28", "\uD800", "\uDC00" };

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

    @Test
    public void testRemoveAllIgnoreCase() {
        // normal / case-insensitive
        assertEquals("qd", Strings.removeAllIgnoreCase("queued", "UE"));
        assertEquals("bB", Strings.removeAllIgnoreCase("aAbBaA", "a"));
        assertEquals("", Strings.removeAllIgnoreCase("abcABC", "abc"));
        // not found
        assertEquals("queued", Strings.removeAllIgnoreCase("queued", "zz"));
        // null/empty
        assertNull(Strings.removeAllIgnoreCase(null, "*"));
        assertEquals("", Strings.removeAllIgnoreCase("", "*"));
        assertEquals("abc", Strings.removeAllIgnoreCase("abc", null));
        assertEquals("abc", Strings.removeAllIgnoreCase("abc", ""));
    }

    @Test
    public void testRemoveAllIgnoreCase_FromIndex() {
        // normal / case-insensitive with fromIndex
        assertEquals("", Strings.removeAllIgnoreCase("ABCabcABC", 0, "abc"));
        assertEquals("ABC", Strings.removeAllIgnoreCase("ABCabcABC", 3, "abc"));
        assertEquals("hell WRLD", Strings.removeAllIgnoreCase("hello WORLD", 0, "O"));
        // no match at or after index
        assertEquals("test", Strings.removeAllIgnoreCase("test", 4, "es"));
        // negative fromIndex treated as 0
        assertEquals("bB", Strings.removeAllIgnoreCase("aAbBaA", -5, "a"));
        // null/empty
        assertNull(Strings.removeAllIgnoreCase(null, 0, "abc"));
        assertEquals("", Strings.removeAllIgnoreCase("", 0, "abc"));
        assertEquals("abc", Strings.removeAllIgnoreCase("abc", 0, null));
    }

    @Test
    public void testRemoveEndIgnoreCase() {
        assertEquals("ab", Strings.removeEndIgnoreCase("abc", "C"));
        assertEquals("ab", Strings.removeEndIgnoreCase("abC", "c"));
        assertNull(Strings.removeEndIgnoreCase(null, "c"));
    }

    @Test
    public void testRemoveEndIgnoreCase_EdgeCases() {
        assertNull(Strings.removeEndIgnoreCase(null, "def"));
        assertEquals("abc", Strings.removeEndIgnoreCase("abcDEF", "def"));
        assertEquals("abcdef", Strings.removeEndIgnoreCase("abcdef", "xyz"));
    }

    // removeStartIgnoreCase

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

    @Test
    public void testRemoveStartIgnoreCase() {
        assertEquals("bc", Strings.removeStartIgnoreCase("abc", "A"));
        assertEquals("bc", Strings.removeStartIgnoreCase("Abc", "a"));
        assertNull(Strings.removeStartIgnoreCase(null, "a"));
    }

    @Test
    public void testRemoveStartIgnoreCase_EdgeCases() {
        assertNull(Strings.removeStartIgnoreCase(null, "abc"));
        assertEquals("def", Strings.removeStartIgnoreCase("ABCdef", "abc"));
        assertEquals("abcdef", Strings.removeStartIgnoreCase("abcdef", "xyz"));
    }

    @Test
    public void testReplaceAllIgnoreCase() {
        assertEquals("xbcxbc", Strings.replaceAllIgnoreCase("AbcAbc", "a", "x"));
        assertEquals("xbcxbc", Strings.replaceAllIgnoreCase("abcabc", "A", "x"));
        assertNull(Strings.replaceAllIgnoreCase(null, "a", "x"));
    }

    @Test
    public void testReplaceAllIgnoreCase_WithFromIndex() {
        assertEquals("hello XX XX", Strings.replaceAllIgnoreCase("hello ab AB", 0, "ab", "XX"));
        assertNull(Strings.replaceAllIgnoreCase(null, 0, "a", "b"));
    }

    @Test
    public void testReplaceFirstDouble_EdgeCases() {
        assertEquals("abcXYZdef", replaceFirstDouble("abc12.34def", "XYZ"));
        assertEquals("X", replaceFirstDouble(".5", "X"));
        assertEquals("x=X", replaceFirstDouble("x=-.5", "X"));
        assertEquals("X", replaceFirstDouble(".5e2", "X", true));
        assertEquals("x=X", replaceFirstDouble("x=-.5e2", "X", true));
        assertEquals("abc", replaceFirstDouble("abc", "XYZ"));
        assertNull(replaceFirstDouble(null, "XYZ"));
    }

    @Test
    public void testReplaceFirstIgnoreCase() {
        assertEquals("xbcAbc", Strings.replaceFirstIgnoreCase("AbcAbc", "a", "x"));
        assertEquals("xbcabc", Strings.replaceFirstIgnoreCase("abcabc", "A", "x"));
        assertNull(Strings.replaceFirstIgnoreCase(null, "a", "x"));
    }

    @Test
    public void testReplaceFirstIgnoreCase_WithFromIndex() {
        assertEquals("hello WORLD xyz", Strings.replaceFirstIgnoreCase("hello world xyz", 0, "WORLD", "WORLD"));
        assertEquals("hello WORLD xyz", Strings.replaceFirstIgnoreCase("hello world xyz", 0, "world", "WORLD"));
        assertNull(Strings.replaceFirstIgnoreCase(null, 0, "a", "b"));
    }

    @Test
    public void testReplaceIgnoreCase() {
        assertEquals("xyzxyz", Strings.replaceIgnoreCase("AbCABC", 0, "abc", "xyz", 2));
        assertNull(Strings.replaceIgnoreCase(null, 0, "abc", "xyz", 1));
    }

    @Test
    public void testReplaceIgnoreCase_WithMax() {
        assertEquals("XX ab AB", Strings.replaceIgnoreCase("ab ab AB", 0, "ab", "XX", 1));
        assertEquals("XX XX AB", Strings.replaceIgnoreCase("ab ab AB", 0, "ab", "XX", 2));
        assertNull(Strings.replaceIgnoreCase(null, 0, "a", "b", 1));
    }

    @Test
    public void testReplaceIgnoreCase_lengthChangingCaseMapping() {
        // regression: lowercasing the whole string shifted match indices for U+0130 (which lowercases to
        // "i" + combining dot), causing IndexOutOfBoundsException or replacement at the wrong position
        assertEquals("İc", Strings.replaceAllIgnoreCase("İb", "b", "c"));
        assertEquals("İXa", Strings.replaceAllIgnoreCase("İaba", "ab", "X"));
        assertEquals("İstanbul X", Strings.replaceAllIgnoreCase("İstanbul city", "CITY", "X"));
        // unchanged for plain text
        assertEquals("xYx", Strings.replaceAllIgnoreCase("aYa", "A", "x"));
        assertEquals("xYa", Strings.replaceFirstIgnoreCase("aYa", "A", "x"));
    }

    @Test
    public void testReplaceLastIgnoreCase() {
        // normal / case-insensitive
        assertEquals("aBz", Strings.replaceLastIgnoreCase("aBa", "A", "z"));
        assertEquals("Hello Hi", Strings.replaceLastIgnoreCase("Hello HELLO", "hello", "Hi"));
        assertEquals("abz", Strings.replaceLastIgnoreCase("aba", "a", "z"));
        // not found
        assertEquals("any", Strings.replaceLastIgnoreCase("any", "xyz", "z"));
        // null/empty
        assertNull(Strings.replaceLastIgnoreCase(null, "a", "z"));
        assertEquals("", Strings.replaceLastIgnoreCase("", "a", "z"));
        assertEquals("any", Strings.replaceLastIgnoreCase("any", null, "z"));
        assertEquals("any", Strings.replaceLastIgnoreCase("any", "", "z"));
    }

    @Test
    public void testReplaceLastIgnoreCase_StartIndex() {
        // normal / case-insensitive with startIndexFromBack
        assertEquals("aBaCaDzE", Strings.replaceLastIgnoreCase("aBaCaDaE", 8, "a", "z"));
        assertEquals("aBaCzDaE", Strings.replaceLastIgnoreCase("aBaCaDaE", 5, "a", "z"));
        assertEquals("Hello HELLO Hi", Strings.replaceLastIgnoreCase("Hello HELLO hello", 16, "hello", "Hi"));
        // negative index -> unchanged
        assertEquals("aBaCaDaE", Strings.replaceLastIgnoreCase("aBaCaDaE", -1, "a", "z"));
        // null/empty
        assertNull(Strings.replaceLastIgnoreCase(null, 5, "a", "z"));
        assertEquals("", Strings.replaceLastIgnoreCase("", 0, "a", "z"));
        assertEquals("any", Strings.replaceLastIgnoreCase("any", 3, null, "z"));
    }

    @Test
    public void testReverse_EdgeCases() {
        assertNull(reverse(null));
        assertEquals("", reverse(""));
        assertEquals("cba", reverse("abc"));
        assertEquals("a", reverse("a"));
    }

    @Test
    public void testReverseDelimited_CharEdgeCases() {
        assertNull(reverseDelimited(null, ','));
        assertEquals("", reverseDelimited("", ','));
        assertEquals("c,b,a", reverseDelimited("a,b,c", ','));
        assertEquals("abc", reverseDelimited("abc", ','));
    }

    @Test
    public void testReverseDelimited_StringEdgeCases() {
        assertNull(reverseDelimited(null, "::"));
        assertEquals("", reverseDelimited("", "::"));
        assertEquals("c::b::a", reverseDelimited("a::b::c", "::"));
    }

    @Test
    public void testRotate_EdgeCases() {
        assertNull(rotate(null, 1));
        assertEquals("", rotate("", 1));
        assertEquals("oHell", rotate("Hello", 1));
        assertEquals("elloH", rotate("Hello", -1));
        assertEquals("Hello", rotate("Hello", 0));
        assertEquals("Hello", rotate("Hello", 5));
    }

    @Test
    public void testScreamingSnakeCase() {
        assertNull(Strings.toScreamingSnakeCase(null));
        assertEquals("", Strings.toScreamingSnakeCase(""));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("helloWorld"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("HelloWorld"));
    }

    @Test
    public void testShuffle_EdgeCases() {
        assertNull(shuffle(null));
        assertEquals("", shuffle(""));
        assertEquals(1, shuffle("a").length());
        String original = "abcdefghij";
        String shuffled = shuffle(original);
        assertEquals(original.length(), shuffled.length());
        // All chars should be present
        char[] origChars = original.toCharArray();
        char[] shuffChars = shuffled.toCharArray();
        java.util.Arrays.sort(origChars);
        java.util.Arrays.sort(shuffChars);
        assertArrayEquals(origChars, shuffChars);
    }

    @Test
    public void testSort_EdgeCases() {
        assertNull(sort(null));
        assertEquals("", sort(""));
        assertEquals("abcde", sort("edcba"));
        assertEquals("aabbc", sort("abcba"));
        assertEquals("a", sort("a"));
    }

    @Test
    public void testStartsWithAnyIgnoreCase() {
        assertTrue(Strings.startsWithAnyIgnoreCase("AbCdEf", "abc", "xyz"));
        assertTrue(Strings.startsWithAnyIgnoreCase("Hello", "HELLO", "test"));
        assertTrue(Strings.startsWithAnyIgnoreCase("TEST", "xyz", "test"));
        assertTrue(Strings.startsWithAnyIgnoreCase("MiXeD", "abc", "MIXED"));
        assertTrue(Strings.startsWithAnyIgnoreCase("ABC", "xy", "ab"));

        assertFalse(Strings.startsWithAnyIgnoreCase("abcdef", "xyz", "123"));
        assertFalse(Strings.startsWithAnyIgnoreCase("abc", "xy", "cd"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "abc"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "a", "b"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "ab"));
    }

    @Test
    public void testStartsWithIgnoreCase() {
        assertTrue(Strings.startsWithIgnoreCase("AbCdEf", "abc"));
        assertTrue(Strings.startsWithIgnoreCase("Hello", "hello"));
        assertTrue(Strings.startsWithIgnoreCase("TEST", "test"));
        assertTrue(Strings.startsWithIgnoreCase("MiXeD", "mixed"));
        assertTrue(Strings.startsWithIgnoreCase("ABC", "abc"));
        assertTrue(Strings.startsWithIgnoreCase("hello world", "HELLO"));
        assertTrue(Strings.startsWithIgnoreCase("JavaScript", "java"));
        assertTrue(Strings.startsWithIgnoreCase("ABC", "ab"));
        assertTrue(Strings.startsWithIgnoreCase("abc", "AB"));

        assertFalse(Strings.startsWithIgnoreCase("abcdef", "xyz"));
        assertFalse(Strings.startsWithIgnoreCase("hello", "world"));
        assertFalse(Strings.startsWithIgnoreCase("abc", "bc"));
        assertFalse(Strings.startsWithIgnoreCase(null, "prefix"));
        assertFalse(Strings.startsWithIgnoreCase("test", null));
        assertFalse(Strings.startsWithIgnoreCase(null, null));
        assertFalse(Strings.startsWithIgnoreCase(null, "abc"));
        assertFalse(Strings.startsWithIgnoreCase(null, "ab"));
    }

    @Test
    public void testStringCaseTransformsUseFullUnicodeMappings() {
        assertEquals('ß', Strings.swapCase('ß')); // char overload is necessarily a simple one-to-one mapping
        assertEquals("SS", Strings.swapCase("ß"));
        assertEquals("i\u0307", Strings.swapCase("\u0130"));

        assertEquals("Sseta", Strings.capitalize("ßeta"));
        assertEquals("\u01C5en", Strings.capitalize("\u01C6en"));
        assertEquals("\u02BCN", Strings.capitalize("\u0149"));
        assertEquals("Ffile", Strings.capitalize("\uFB03le"));

        final String vithkuqiSmallA = new String(Character.toChars(0x10597));
        final String vithkuqiCapitalA = new String(Character.toChars(0x10570));
        assertEquals(vithkuqiCapitalA + "bc", Strings.capitalize(vithkuqiSmallA + "bc"));

        assertEquals("i\u0307stanbul", Strings.uncapitalize("\u0130stanbul"));
        assertEquals("\u01C5en Sseta", Strings.capitalizeWords("\u01C6en ßeta"));
        assertEquals("\u01C5en::Sseta", Strings.capitalizeWords("\u01C6en::ßeta", "::"));
        assertEquals("\u01C5en Sseta", Strings.capitalizeWordsFully("\u01C4EN ßETA"));
    }

    @Test
    public void testStrUtilIgnoreCaseOptionalWrappers() {
        assertEquals("WORLD", StrUtil.substringAfterIgnoreCase("Hello WORLD", "hello ").get());
        assertFalse(StrUtil.substringAfterIgnoreCase("NoMatch", "xyz").isPresent());
        assertFalse(StrUtil.substringAfterIgnoreCase(null, "x").isPresent());

        assertEquals("Test", StrUtil.substringAfterLastIgnoreCase("com.Example.Test", ".example.").get());
        assertFalse(StrUtil.substringAfterLastIgnoreCase("hello", ".").isPresent());

        assertEquals("User", StrUtil.substringBeforeIgnoreCase("User@Example.com", "@EXAMPLE").get());
        assertFalse(StrUtil.substringBeforeIgnoreCase("no-match", "@").isPresent());

        assertEquals("Com.Example", StrUtil.substringBeforeLastIgnoreCase("Com.Example.TEST", ".test").get());
        assertFalse(StrUtil.substringBeforeLastIgnoreCase("no-match", "@").isPresent());

        assertEquals("CONTENT", StrUtil.substringBetweenIgnoreCase("#CONTENT#", "#").get());
        assertFalse(StrUtil.substringBetweenIgnoreCase("[[only-one", "[[").isPresent());

        assertEquals("content", StrUtil.substringBetweenIgnoreCase("<TAG>content</TAG>", "<tag>", "</tag>").get());
        assertFalse(StrUtil.substringBetweenIgnoreCase("[[no-end", "[[", "]]").isPresent());

        assertEquals("text2", StrUtil.substringBetweenIgnoreCase("<A>text1</A><A>text2</A>", 5, "<a>", "</a>").get());
        assertFalse(StrUtil.substringBetweenIgnoreCase("test", 10, "t", "t").isPresent());

        // each wrapper must mirror the Strings method it delegates to
        assertEquals(Strings.substringAfterIgnoreCase("Hello WORLD", "hello "), StrUtil.substringAfterIgnoreCase("Hello WORLD", "hello ").orElseNull());
        assertNull(StrUtil.substringBeforeIgnoreCase("no-match", "@").orElseNull());
    }

    @Test
    public void testSubstring_EdgeCases() {
        assertEquals("bc", substring("abc", 1));
        assertEquals("b", substring("abc", 1, 2));
        assertNull(substring(null, 1));
        assertEquals("", substring("abc", 3));
    }

    @Test
    public void testSubstringAfter_EdgeCases() {
        assertEquals("def", substringAfter("abc.def", "."));
        assertNull(substringAfter(null, "."));
        assertNull(substringAfter("abc", null));
        assertNull(substringAfter("abc", "xyz"));
    }

    @Test
    public void testSubstringAfterAny_EdgeCases() {
        assertEquals("def,ghi", substringAfterAny("abc.def,ghi", "."));
        assertNull(substringAfterAny(null, "."));
    }

    @Test
    public void testSubstringAfterIgnoreCase() {
        assertEquals("WORLD", Strings.substringAfterIgnoreCase("hello WORLD", "hello "));
        assertEquals("world", Strings.substringAfterIgnoreCase("Hello world", "HELLO "));
        assertNull(Strings.substringAfterIgnoreCase(null, "hello"));
    }

    @Test
    public void testSubstringAfterIgnoreCase_StringDelimiter() {
        // normal / case-insensitive
        assertEquals("Example.com", Strings.substringAfterIgnoreCase("www.Example.com", "WWW."));
        assertEquals("World", Strings.substringAfterIgnoreCase("Hello::World", "::"));
        // not found
        assertNull(Strings.substringAfterIgnoreCase("NoDelimiter", "xyz"));
        // empty delimiter -> whole string
        assertEquals("test", Strings.substringAfterIgnoreCase("test", ""));
        // null
        assertNull(Strings.substringAfterIgnoreCase(null, "test"));
        assertNull(Strings.substringAfterIgnoreCase("test", null));
    }

    @Test
    public void testSubstringAfterLast_EdgeCases() {
        assertEquals("ghi", substringAfterLast("abc.def.ghi", "."));
        assertNull(substringAfterLast(null, "."));
        assertNull(substringAfterLast("abc", "xyz"));
    }

    @Test
    public void testSubstringAfterLastIgnoreCase() {
        assertEquals("C", Strings.substringAfterLastIgnoreCase("a:b:C", ":"));
        assertNull(Strings.substringAfterLastIgnoreCase(null, ":"));
    }

    @Test
    public void testSubstringBefore_EdgeCases() {
        assertEquals("abc", substringBefore("abc.def", "."));
        assertNull(substringBefore(null, "."));
        assertNull(substringBefore("abc", null));
        assertNull(substringBefore("abc", "xyz"));
    }

    @Test
    public void testSubstringBeforeAny_EdgeCases() {
        assertEquals("abc", substringBeforeAny("abc.def,ghi", ".", ","));
        assertNull(substringBeforeAny(null, "."));
    }

    @Test
    public void testSubstringBeforeIgnoreCase() {
        assertEquals("abc", Strings.substringBeforeIgnoreCase("abc:DEF", ":"));
        assertNull(Strings.substringBeforeIgnoreCase(null, ":"));
    }

    @Test
    public void testSubstringBeforeIgnoreCase_StringDelimiter() {
        // normal / case-insensitive
        assertEquals("user", Strings.substringBeforeIgnoreCase("user@Example.com", "@"));
        assertEquals("Hello", Strings.substringBeforeIgnoreCase("Hello World Java", " world"));
        // not found
        assertNull(Strings.substringBeforeIgnoreCase("no-delimiter", "@"));
        // leading delimiter -> empty
        assertEquals("", Strings.substringBeforeIgnoreCase("@leading", "@"));
        // empty delimiter -> empty
        assertEquals("", Strings.substringBeforeIgnoreCase("test", ""));
        // null
        assertNull(Strings.substringBeforeIgnoreCase(null, "@"));
        assertNull(Strings.substringBeforeIgnoreCase("test", null));
    }

    @Test
    public void testSubstringBeforeLast_EdgeCases() {
        assertEquals("abc.def", substringBeforeLast("abc.def.ghi", "."));
        assertNull(substringBeforeLast(null, "."));
        assertNull(substringBeforeLast("abc", "xyz"));
    }

    @Test
    public void testSubstringBeforeLastIgnoreCase() {
        assertEquals("a:b", Strings.substringBeforeLastIgnoreCase("a:b:C", ":"));
        assertNull(Strings.substringBeforeLastIgnoreCase(null, ":"));
    }

    @Test
    public void testSubstringBetween_EdgeCases() {
        assertEquals("def", substringBetween("abc[def]ghi", "[", "]"));
        assertNull(substringBetween(null, "[", "]"));
        assertEquals("", substringBetween("ab", "a", "b"));
    }

    @Test
    public void testSubstringBetweenIgnoreCase() {
        assertEquals("B", substringBetweenIgnoreCase("aBc", "A", "C"));
        assertEquals("tagged", substringBetweenIgnoreCase("<TAG>tagged</TAG>", "<tag>", "</tag>"));
        assertEquals("ABC", substringBetweenIgnoreCase("xABCx", "X"));
        assertNull(substringBetweenIgnoreCase("abc", "x", "c"));
        assertNull(substringBetweenIgnoreCase(null, "a", "c"));
    }

    // substringBetweenIgnoreCase



    /**
     * C-019: an empty delimiter matches twice at index zero, so {@code substringBetweenIgnoreCase} answers
     * {@code ""} for every non-{@code null} string - the one input for which it never returns {@code null}.
     * Its javadoc listed only the {@code null} outcomes; the non-ignore-case twin already documented this.
     */
    @Test
    public void test20260906_substringBetweenIgnoreCaseWithAnEmptyDelimiter() {
        assertEquals("", Strings.substringBetweenIgnoreCase("test", ""));
        assertEquals("", Strings.substringBetweenIgnoreCase("", ""));
        assertEquals(Strings.substringBetween("test", ""), Strings.substringBetweenIgnoreCase("test", ""));
        assertEquals(Strings.substringBetween("", ""), Strings.substringBetweenIgnoreCase("", ""));

        // the documented null outcomes are unchanged
        assertNull(Strings.substringBetweenIgnoreCase("test", null));
        assertNull(Strings.substringBetweenIgnoreCase(null, ""));
        assertNull(Strings.substringBetweenIgnoreCase("no-match", "[["));
    }

    @Test
    public void testsubstringBetweenIgnoreCase() {
        assertEquals("content", Strings.substringBetweenIgnoreCase("<TAG>content</tag>", "<tag>", "</TAG>"));
        assertNull(Strings.substringBetweenIgnoreCase(null, "<", ">"));
    }

    @Test
    public void testSubstringBetweenIgnoreCase_WithFromIndex() {
        String result = Strings.substringBetweenIgnoreCase("aXbYcXdY", 3, "x", "y");
        assertEquals("d", result);
    }

    @Test
    public void testSubstringBetweenIgnoreCase_EdgeCases() {
        assertEquals("def", substringBetweenIgnoreCase("ABC[def]GHI", "[", "]"));
        assertNull(substringBetweenIgnoreCase(null, "a", "b"));
    }

    // swapCase

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



    // G072-04: swapCase non-ASCII path; the per-code-point substring is only built for cased code points
    @Test
    public void testSwapCase_unicodeUncasedAndCasedCodePoints() {
        assertEquals("\u4E2D\u6587 1-2 \uFF01", Strings.swapCase("\u4E2D\u6587 1-2 \uFF01"));
        assertEquals("\u4E2DAbC\u00C9", Strings.swapCase("\u4E2DaBc\u00E9"));
        assertEquals("SS\u00E0", Strings.swapCase("\u00DF\u00C0"));
        assertEquals("i\u0307x", Strings.swapCase("\u0130X"));
        assertEquals("\u01C6", Strings.swapCase("\u01C5"));
        assertEquals("\uD801\uDC28\uD801\uDC00", Strings.swapCase("\uD801\uDC00\uD801\uDC28"));
        assertEquals("\uD83D\uDE00A\ud800b\udc00", Strings.swapCase("\uD83D\uDE00a\ud800B\udc00"));
        assertEquals("o\u03C3\u03C2 \u03C3", Strings.swapCase("O\u03A3\u03A3 \u03A3"));
        assertEquals("\u00E9\u4E2D", Strings.swapCase("\u00C9\u4E2D"));

        final int[] pool = { 'a', 'Z', '5', ' ', 0x00e9, 0x00c9, 0x00df, 0x0130, 0x01c5, 0x4e2d, 0x3000, 0x10400, 0x10428, 0x1f600, 0xd800, 0xdc00, 0xfb00,
                0x0149, 0x0390 };
        final Random random = new Random(20260926L);

        for (int round = 0; round < 2000; round++) {
            final StringBuilder input = new StringBuilder();
            final StringBuilder expected = new StringBuilder();

            for (int i = random.nextInt(12); i >= 0; i--) {
                final int codePoint = pool[random.nextInt(pool.length)];
                final String single = new String(Character.toChars(codePoint));
                input.append(single);

                if (Character.isUpperCase(codePoint) || Character.isTitleCase(codePoint)) {
                    expected.append(single.toLowerCase(Locale.ROOT));
                } else if (Character.isLowerCase(codePoint)) {
                    expected.append(single.toUpperCase(Locale.ROOT));
                } else {
                    expected.append(single);
                }
            }

            final String text = input.toString();

            if (text.indexOf("\uD800\uDC00") >= 0) {
                continue;
            }

            assertEquals(expected.toString(), Strings.swapCase(text), text);
        }
    }

    @Test
    public void testSwapCaseChar() {
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('a', Strings.swapCase('A'));
        assertEquals('1', Strings.swapCase('1'));
        assertEquals(' ', Strings.swapCase(' '));
    }



    // C-014: swapCase applies the Unicode Final_Sigma rule, toLowerCase follows the JDK approximation
    @Test
    public void testC014_swapCaseFinalSigmaDiffersFromToLowerCase() {
        assertEquals("ος-α", Strings.swapCase("ΟΣ-Α"));
        assertEquals("οσ-α", Strings.toLowerCase("ΟΣ-Α"));
    }

    @Test
    public void testSwapCaseString() {
        assertNull(Strings.swapCase(null));
        assertEquals("", Strings.swapCase(""));
        assertEquals("ABC", Strings.swapCase("abc"));
        assertEquals("abc", Strings.swapCase("ABC"));
        assertEquals("AbC", Strings.swapCase("aBc"));
        assertEquals("tHE dOG hAS a bONE", Strings.swapCase("The Dog Has A Bone"));
        assertEquals("hELLO", Strings.swapCase("Hello"));
        assertEquals("tHE DOG HAS A bone", Strings.swapCase("The dog has a BONE"));
        assertEquals("abc123XYZ", Strings.swapCase("ABC123xyz"));
        assertEquals("hELLO wORLD", Strings.swapCase("Hello World"));
        assertEquals("123", Strings.swapCase("123"));

        assertEquals("\u03BF\u03C2", Strings.swapCase("\u039F\u03A3"));
        assertEquals("A\u03C2", Strings.swapCase("a\u03A3"));
        assertEquals("\u03BF\u03C3\u03B1", Strings.swapCase("\u039F\u03A3\u0391"));
        assertEquals("a\u03C3B", Strings.swapCase("A\u03A3b"));
        assertEquals("a'\u03C2", Strings.swapCase("A'\u03A3"));
        assertEquals("a\u03C3'B", Strings.swapCase("A\u03A3'b"));
        assertEquals("a\u03C2\u0301 ", Strings.swapCase("A\u03A3\u0301 "));
        assertEquals("a\u03C3\u0301B", Strings.swapCase("A\u03A3\u0301b"));
        assertEquals("\u03C3", Strings.swapCase("\u03A3"));
    }

    // toCamelCase

    @Test
    public void test_toCamelCase() {
        {
            assertEquals("a", Strings.toCamelCase("a"));
            assertEquals("accountContact", Strings.toCamelCase("account_contact"));
            assertEquals("aBCD", Strings.toCamelCase("a_B_c_D"));
            assertEquals("bBCD", Strings.toCamelCase("B_B_c_d"));
        }
    }

    @Test
    public void testToCamelCase() {
        assertEquals("helloWorld", Strings.toCamelCase("hello_world"));
        assertEquals("helloWorld", Strings.toCamelCase("HELLO_WORLD"));
        assertEquals("helloWorld", Strings.toCamelCase("hello-world"));
        assertNull(Strings.toCamelCase(null));
        assertEquals("", Strings.toCamelCase(""));
    }

    @Test
    public void testToCamelCase_AgreesWithCapitalize() {
        // Every capitalized camel-case word that starts with a letter must be byte-identical to what
        // capitalize()/capitalizeWordsFully() produce for the same word: the Lt digraphs, the one-to-many
        // titlecase mappings, and ordinary letters. (Words starting with a digit differ by design: camel case
        // capitalizes the first LETTER, "2beta" -> "2Beta", while capitalize() maps the first code point and
        // leaves "2beta" alone; that case is covered by camelCase_capitalizesWithTitlecaseNotUppercase.)
        final String[] words = { "ǆen", "ǄEN", "ǅen", "ǉax", "ǌax", "ǳord", "ßeta", "ŉame", "ᾀx", "ևx", "ﬁle", "İstanbul", "ΣΣ", "été", "straße", "𐐨eseret",
                "hello", "WORLD", "x" };

        for (final String w : words) {
            final String expected = Strings.capitalize(w.toLowerCase(java.util.Locale.ROOT));
            assertEquals(expected, Strings.toUpperCamelCase(w), w);
            assertEquals(Strings.capitalizeWordsFully(w), Strings.toUpperCamelCase(w), w);
            assertEquals("a" + expected, Strings.toCamelCase("a_" + w), w);
            assertEquals("a" + expected, Strings.toCamelCase("a " + w), w);
        }
    }

    @Test
    public void testToCamelCase_AllCapsAndEmpty() {
        assertNull(Strings.toCamelCase(null));
        assertEquals("", Strings.toCamelCase(""));
        assertEquals("helloWorld", Strings.toCamelCase("hello_world"));
        assertEquals("helloWorld", Strings.toCamelCase("HELLO_WORLD"));
        assertEquals("helloWorld", Strings.toCamelCase("Hello_World"));
    }

    @Test
    public void testToCamelCase_AsciiAndUnicodePathsAgree() {
        // Forcing the Unicode path with one non-ASCII character must not change how ASCII words are capitalized.
        assertEquals("vérsion2Beta", Strings.toCamelCase("vérsion_2beta"));
        assertEquals("É2Beta", Strings.toUpperCamelCase("é_2beta"));
        assertEquals("helloWörld", Strings.toCamelCase("hello_wörld"));
        assertEquals("HelloWörld", Strings.toUpperCamelCase("HELLO_WÖRLD"));
        assertEquals("xmlParseré", Strings.toCamelCase("XMLParseré"));
        // and the ASCII fast path itself is untouched
        assertEquals("firstName", Strings.toCamelCase("FIRST_NAME"));
        assertEquals("FirstName", Strings.toUpperCamelCase("first name"));
        assertEquals("version2Beta", Strings.toCamelCase("version_2beta"));
        assertEquals("XmlParser", Strings.toUpperCamelCase("XMLParser"));
    }

    @Test
    public void testToCamelCase_CaseBoundaryAtCasedSymbol() {
        // Ⅷ is an uppercase boundary for the splitter; the re-capitalized word must keep it uppercase.
        assertEquals("x" + ROMAN_EIGHT_UPPER, Strings.toCamelCase("x" + ROMAN_EIGHT_UPPER));
        assertEquals("x" + ROMAN_EIGHT_UPPER + "y", Strings.toCamelCase("x" + ROMAN_EIGHT_UPPER + "y"));
        assertEquals("X" + ROMAN_EIGHT_UPPER + "y", Strings.toUpperCamelCase("x" + ROMAN_EIGHT_UPPER + "y"));
        // Both directions of the round trip through the delimited forms now agree on the capital.
        assertEquals("x_" + ROMAN_EIGHT_LOWER, Strings.toSnakeCase("x" + ROMAN_EIGHT_UPPER));
        assertEquals("x" + ROMAN_EIGHT_UPPER, Strings.toCamelCase(Strings.toSnakeCase("x" + ROMAN_EIGHT_UPPER)));
    }



 // 🄰 SQUARED LATIN CAPITAL LETTER A (no lowercase mapping)
    @Test
    public void testToCamelCase_CasedNonLetterSymbols() {
        // The JDK classifies these as cased but not as letters; that combination is what the old predicate missed.
        assertFalse(Character.isLetter(0x24D0));
        assertTrue(Character.isLowerCase(0x24D0));
        assertFalse(Character.isLetter(0x2167));
        assertTrue(Character.isUpperCase(0x2167));

        assertEquals(CIRCLED_A_UPPER + "bc", Strings.toUpperCamelCase(CIRCLED_A_LOWER + "bc"));
        assertEquals(CIRCLED_A_UPPER + "Bc", Strings.toUpperCamelCase(CIRCLED_A_LOWER + "_bc")); // two words: "ⓐ" and "bc"
        assertEquals(CIRCLED_A_UPPER + "bc", Strings.toUpperCamelCase(CIRCLED_A_UPPER + "BC"));
        assertEquals(ROMAN_EIGHT_UPPER, Strings.toUpperCamelCase(ROMAN_EIGHT_LOWER));
        assertEquals(ROMAN_EIGHT_UPPER, Strings.toUpperCamelCase(ROMAN_EIGHT_UPPER));
        assertEquals("x" + ROMAN_EIGHT_UPPER, Strings.toCamelCase("x_" + ROMAN_EIGHT_LOWER));
        assertEquals("x" + CIRCLED_A_UPPER + "bc", Strings.toCamelCase("x " + CIRCLED_A_LOWER + "bc"));
        assertEquals("x" + CIRCLED_A_UPPER + "bc", Strings.toCamelCase("x." + CIRCLED_A_LOWER + "bc", '.'));
    }

    @Test
    public void testToCamelCase_CasedSymbolWithoutLowercaseMapping() {
        // U+1F130 is Other_Uppercase with no lowercase (or titlecase) mapping: it is the capitalizable code point,
        // it stays as it is, and the following ASCII letter is no longer promoted in its place.
        assertEquals(SQUARED_A + "x", Strings.toUpperCamelCase(SQUARED_A + "x"));
        assertEquals("a" + SQUARED_A + "x", Strings.toCamelCase("a_" + SQUARED_A + "x"));
        assertEquals(Strings.capitalizeWordsFully(SQUARED_A + "x"), Strings.toUpperCamelCase(SQUARED_A + "x"));
    }

    @Test
    public void testToCamelCase_FirstLetterRuleForDigitsAndPunctuation() {
        // Digits and punctuation are neither letters nor cased, so the search still moves past them (test-locked
        // elsewhere too); this is exactly where camel case and capitalize() are documented to differ.
        assertEquals("2Beta", Strings.toUpperCamelCase("2beta"));
        assertEquals("version2Beta", Strings.toCamelCase("version_2beta"));
        assertEquals("'Cat'", Strings.toUpperCamelCase("'cat'"));
        assertEquals("2beta", Strings.capitalizeWordsFully("2beta"));
        assertEquals("'cat'", Strings.capitalizeWordsFully("'cat'"));
        assertEquals("2beta", Strings.capitalize("2beta"));
        // A word with nothing capitalizable at all is emitted lowercased, unchanged.
        assertEquals("a123", Strings.toCamelCase("a_123"));
        assertEquals("a'!'", Strings.toCamelCase("a_'!'"));
    }



    /**
     * Contract pin for the {@code toCamelCase(String)} javadoc: the "2beta" -> "2Beta" titlecase rule applies to a
     * capitalized WORD, never to the first word of a camelCase result, so on a whole string {@code toCamelCase} and
     * {@code capitalize} agree.
     */
    @Test
    public void testToCamelCase_FirstWordIsLowercasedNotTitlecased() {
        assertEquals("2beta", Strings.toCamelCase("2beta"));
        assertEquals("2beta", Strings.capitalize("2beta"));

        // the titlecase rule is observable only on a non-first word
        assertEquals("version2Beta", Strings.toCamelCase("version_2beta"));
        assertEquals("x2Beta", Strings.toCamelCase("x_2beta"));

        // toUpperCamelCase capitalizes the first word too, so there the whole-string form does differ
        assertEquals("2Beta", Strings.toUpperCamelCase("2beta"));
    }

    @Test
    public void testToCamelCase_notInverseOfToSnakeCase() {
        // Separators are removed and collapsed; leading/trailing separators disappear.
        assertEquals("helloWorld", Strings.toCamelCase("_helloWorld"));
        assertEquals("helloWorld", Strings.toCamelCase("helloWorld_"));
        assertEquals("helloWorld", Strings.toCamelCase("_hello_world"));
        assertEquals("aB", Strings.toCamelCase("a__b"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("_helloWorld"));

        // Snake collapses separator runs to one delimiter; camel removes them, so the pair is not invertible.
        assertEquals("a_b", Strings.toSnakeCase("a__b"));
        assertNotEquals("a__b", Strings.toCamelCase(Strings.toSnakeCase("a__b")));
        assertNotEquals("_helloWorld", Strings.toCamelCase(Strings.toSnakeCase("_helloWorld")));
        assertNotEquals("_hello_world", Strings.toSnakeCase(Strings.toCamelCase("_hello_world")));
        assertEquals("version2Beta", Strings.toCamelCase("version_2beta"));
        assertEquals("2Beta", Strings.toUpperCamelCase("2beta"));
        assertEquals("", Strings.toCamelCase("___"));
    }



    /**
     * Regression tests for the bug where toCamelCase / toUpperCamelCase used
     * {@code sb.length() - substr.length()} to locate the start of the most
     * recently appended token. That formula is only correct when
     * {@code substr.toLowerCase(Locale.ROOT)} produces the same number of
     * characters as {@code substr}. The fix captures {@code startPos = sb.length()}
     * BEFORE the append so the index is always exact.
     *
     * <p>The most visible symptom with common inputs is that the first character
     * of every non-first token must be uppercased (lowerCamelCase) or every token
     * must be uppercased (UpperCamelCase). Any off-by-one in the index would leave
     * the wrong character capitalised.</p>
     */
    @Test
    public void testToCamelCase_startPositionIndexBugFix() {
        // Multi-token path (firstSplitStrategyIndex >= 0): the fixed code records
        // startPos before appending toLowerCase'd token, then uses startPos
        // for setCharAt.  Verify correct capitalisation for every non-first token.
        assertEquals("helloWorld", Strings.toCamelCase("hello_world"));
        assertEquals("helloWorldFoo", Strings.toCamelCase("hello_world_foo"));
        assertEquals("helloWorld", Strings.toCamelCase("HELLO_WORLD"));
        assertEquals("helloWorldFoo", Strings.toCamelCase("HELLO_WORLD_FOO"));
        assertEquals("helloWorld", Strings.toCamelCase("hello-world"));
        assertEquals("firstName", Strings.toCamelCase("first_name"));
        assertEquals("firstName", Strings.toCamelCase("FIRST_NAME"));

        // Three tokens: only the second and third should be capitalised
        assertEquals("oneTwoThree", Strings.toCamelCase("one_two_three"));
        assertEquals("oneTwoThree", Strings.toCamelCase("ONE_TWO_THREE"));

        // Single-char tokens: each non-first single-char token becomes uppercase
        assertEquals("aBC", Strings.toCamelCase("a_b_c"));

        // First token empty (leading separator) — empty tokens are skipped, so the
        // next non-empty token becomes the first (lowercase) word
        assertEquals("world", Strings.toCamelCase("_world"));
    }



    // D1 : camel case capitalizes with the titlecase mapping, like capitalize()
    @Test
    public void testToCamelCase_TitlecaseNotUppercase() {
        // U+01C6 dž -> U+01C5 Dž (titlecase), not U+01C4 DŽ (uppercase)
        assertEquals("ǅen", Strings.toUpperCamelCase("ǆen"));
        assertEquals("ǅen", Strings.toUpperCamelCase("ǄEN")); // lowercased first, then titlecased
        assertEquals("ǅen", Strings.toUpperCamelCase("ǅen"));
        assertEquals("ǅ", Strings.toUpperCamelCase("Ǆ"));
        assertEquals("xǅen", Strings.toCamelCase("x_ǆen"));
        assertEquals("xǅen", Strings.toCamelCase("x ǄEN"));
        assertEquals("xǅen", Strings.toCamelCase("x-ǆen", '-'));
        // The first word of camelCase is lowercased, never capitalized, so it is unaffected.
        assertEquals("ǆen", Strings.toCamelCase("ǄEN"));
        assertEquals("ǆenX", Strings.toCamelCase("ǄEN_x"));

        // ß -> "Ss" (titlecase), not "SS" (uppercase); the result grows by one code unit either way.
        assertEquals("Sseta", Strings.toUpperCamelCase("ßeta"));
        assertEquals("SsetaName", Strings.toUpperCamelCase("ßeta_name"));
        assertEquals("xSseta", Strings.toCamelCase("x_ßeta"));
        assertEquals("ßetaName", Strings.toCamelCase("ßeta_name"));
        // The first LETTER is capitalized even when it is not the first code point.
        assertEquals("2Ssx", Strings.toUpperCamelCase("2ßx"));
        assertEquals("a2Ssx", Strings.toCamelCase("a_2ßx"));
    }

    @Test
    public void testToCamelCase_TitlecaseTableAndAsciiPath() {
        assertEquals("ǅen", Strings.toUpperCamelCase("ǆen"));
        assertEquals("SsetaName", Strings.toUpperCamelCase("ßeta_name"));
        assertEquals("firstName", Strings.toCamelCase("first_name"));
        assertEquals("FirstName", Strings.toUpperCamelCase("FIRST_NAME"));
        assertEquals("xmlParser", Strings.toCamelCase("XMLParser"));
        assertNull(Strings.toCamelCase(null));
        assertEquals("", Strings.toUpperCamelCase(""));
    }

    @Test
    public void testToCamelCase_WithSplitChar() {
        assertEquals("helloWorld", Strings.toCamelCase("hello_world", '_'));
        assertEquals("helloWorld", Strings.toCamelCase("hello-world", '-'));
        assertNull(Strings.toCamelCase(null, '_'));
    }

    @Test
    public void testToCamelCase_WithStringsArray() {
        // single element array (no split char found) - uppercase input
        assertEquals("firstName", Strings.toCamelCase("FirstName"));
        assertEquals("xmlParser", Strings.toCamelCase("XMLParser"));

        // multi-word input via split
        assertEquals("firstName", Strings.toCamelCase("first_name"));
        assertEquals("helloWorld", Strings.toCamelCase("hello_world"));

        // null/empty
        assertNull(Strings.toCamelCase((String) null));
        assertEquals("", Strings.toCamelCase(""));
    }

    @Test
    public void testToCamelCaseSplitChar_stripsLeadingTrailingDelimiters() {
        // regression: the single-token branch ran on the original string (delimiters still attached)
        // instead of the split token
        assertEquals("name", Strings.toCamelCase("-name", '-'));
        assertEquals("Name", Strings.toUpperCamelCase("-name", '-'));
        assertEquals("name", Strings.toCamelCase("-NAME", '-'));
        assertEquals("firstName", Strings.toCamelCase(".firstName", '.'));

        // unchanged behavior
        assertEquals("firstName", Strings.toCamelCase("firstName", '.'));
        assertEquals("FirstName", Strings.toUpperCamelCase("first.name", '.'));
    }

    // toKebabCase

    @Test
    public void testToKebabCase() {
        assertNull(Strings.toKebabCase(null));
        assertEquals("", Strings.toKebabCase(""));
        assertEquals("hello-world", Strings.toKebabCase("helloWorld"));
        assertEquals("hello-world", Strings.toKebabCase("HelloWorld"));
        assertEquals("hello-world-api", Strings.toKebabCase("helloWorldAPI"));
        assertEquals("abc", Strings.toKebabCase("abc"));
        assertEquals("io-error", Strings.toKebabCase("IOError"));
        assertEquals("hello", Strings.toKebabCase("hello"));
    }

    @Test
    public void testToKebabCase_NormalizesUnderscore() {
        // underscore normalized to hyphen (behavior change)
        assertEquals("a-b", Strings.toKebabCase("a_b"));
        assertEquals("first-name", Strings.toKebabCase("first_name"));
        // existing hyphens pass through unchanged
        assertEquals("first-name", Strings.toKebabCase("first-name"));
        // case boundaries still convert
        assertEquals("hello-world", Strings.toKebabCase("helloWorld"));
        // no duplicate hyphens when underscore sits at a case boundary
        assertEquals("a-b", Strings.toKebabCase("a_B"));
        // normalized separators at the edges are discarded; explicit hyphens are retained
        assertEquals("a", Strings.toKebabCase(" _a_ "));
        assertEquals("a", Strings.toKebabCase("-a-"));
        assertEquals("a", Strings.toKebabCase("-a--"));
        assertEquals("first-name", Strings.toKebabCase("_first__name_"));
        assertEquals("hello", Strings.toKebabCase("hello_"));
        assertEquals("hello", Strings.toKebabCase("_hello"));
        assertEquals("hello", Strings.toKebabCase(" hello "));
    }

    @Test
    public void testToKebabCase_SplitChar() {
        // custom split char normalized to hyphen
        assertEquals("a-b", Strings.toKebabCase("a.b", '.'));
        assertEquals("first-name", Strings.toKebabCase("first.name", '.'));
        assertEquals("first-name", Strings.toKebabCase("firstName", '.'));
        // '_' split char also works (base method normalizes underscores)
        assertEquals("a-b", Strings.toKebabCase("a_b", '_'));
        // null/empty
        assertNull(Strings.toKebabCase(null, '.'));
        assertEquals("", Strings.toKebabCase("", '.'));
    }

    // toLowerCase

    @Test
    public void testToLowerCase() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals("hello", Strings.toLowerCase("HELLO"));
        assertEquals("", Strings.toLowerCase(""));
        assertNull(Strings.toLowerCase(null));
    }

    @Test
    public void testToLowerCase_Char() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals('z', Strings.toLowerCase('Z'));
        assertEquals('a', Strings.toLowerCase('a'));
        assertEquals('1', Strings.toLowerCase('1'));
    }

    @Test
    public void testToLowerCase_String() {
        assertEquals("abc", Strings.toLowerCase("ABC"));
        assertEquals("abc", Strings.toLowerCase("abc"));
        assertEquals("abc123", Strings.toLowerCase("ABC123"));
        assertNull(Strings.toLowerCase(null));
        assertEquals("", Strings.toLowerCase(""));
    }

    @Test
    public void testToLowerCase_WithLocale() {
        assertEquals("abc", Strings.toLowerCase("ABC", Locale.ENGLISH));
        assertNull(Strings.toLowerCase(null, Locale.ENGLISH));
        assertNull(Strings.toLowerCase(null, null));
        assertEquals("", Strings.toLowerCase("", null));
        assertThrows(NullPointerException.class, () -> Strings.toLowerCase("A", null));
    }

    @Test
    public void testToLowerCaseChar() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals('a', Strings.toLowerCase('a'));
    }

    @Test
    public void testToLowerCaseString() {
        assertNull(Strings.toLowerCase(null));
        assertEquals("", Strings.toLowerCase(""));
        assertEquals("abc", Strings.toLowerCase("aBc"));
    }

    @Test
    public void testToLowerCaseStringLocale() {
        assertEquals("ı", Strings.toLowerCase("I", new Locale("tr")));
    }

    // toPascalCase

    @Test
    public void testToPascalCase() {
        // toPascalCase is a supported alias for toUpperCamelCase
        assertNull(Strings.toPascalCase(null));
        assertEquals("", Strings.toPascalCase(""));
        assertEquals("HelloWorld", Strings.toPascalCase("hello_world"));
    }

    @Test
    public void testToPascalCase_EdgeCases() {
        assertNull(Strings.toPascalCase(null));
        assertEquals("", Strings.toPascalCase(""));
        assertEquals("HelloWorld", Strings.toPascalCase("hello_world"));
        assertEquals("HelloWorld", Strings.toPascalCase("HELLO_WORLD"));
    }

    @Test
    public void testToPascalCase_WithSplitChar() {
        assertEquals("FirstName", Strings.toPascalCase("first.name", '.'));
        assertNull(Strings.toPascalCase(null, '.'));
    }

    @Test
    public void testToScreamingSnakeCase() {
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("HelloWorld"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("helloWorld"));
        assertEquals("ABC", Strings.toScreamingSnakeCase("ABC"));
        assertNull(Strings.toScreamingSnakeCase(null));

        // NF-1: an existing hyphen is normalized to an underscore (mirrors toSnakeCase)
        assertEquals("FIRST_NAME", Strings.toScreamingSnakeCase("first-name"));
        assertEquals("A_B", Strings.toScreamingSnakeCase("a-b"));

        // NF-1: (String, char) split overload, consistent with toCamelCase(String, char)
        assertEquals("FIRST_NAME", Strings.toScreamingSnakeCase("first.name", '.'));
        assertEquals("A_B_C", Strings.toScreamingSnakeCase("a#b#c", '#'));
        assertNull(Strings.toScreamingSnakeCase(null, '.'));
        assertEquals("", Strings.toScreamingSnakeCase("", '.'));
    }

    @Test
    public void testToScreamingSnakeCase_FullCoverage() {
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("helloWorld"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("HelloWorld"));
        assertEquals("HELLO_WORLD_API", Strings.toScreamingSnakeCase("helloWorldAPI"));
        assertEquals("IO_ERROR", Strings.toScreamingSnakeCase("IOError"));

        // null/empty
        assertNull(Strings.toScreamingSnakeCase(null));
        assertEquals("", Strings.toScreamingSnakeCase(""));
        assertEquals("HELLO", Strings.toScreamingSnakeCase("hello"));
    }

    // toSnakeCase

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



    // C-010 (documented, unchanged): a digit does not start a new word
    @Test
    public void testC010_digitDoesNotStartAWord() {
        assertEquals("base64url", Strings.toSnakeCase("base64URL"));
        assertEquals("base_url", Strings.toSnakeCase("baseURL"));
        assertEquals("vector3d", Strings.toSnakeCase("vector3D"));
        assertEquals("aB2c", Strings.toCamelCase("aB2C"));
    }

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

    @Test
    public void testToSnakeCase() {
        assertEquals("hello_world", Strings.toSnakeCase("HelloWorld"));
        assertEquals("hello_world", Strings.toSnakeCase("helloWorld"));
        assertEquals("abc", Strings.toSnakeCase("abc"));
        assertEquals("a_b", Strings.toSnakeCase("a-_b"));
        assertEquals("a_b", Strings.toSnakeCase("a_-b"));
        assertEquals("a_b", Strings.toSnakeCase("a__b"));
        assertEquals("a_b", Strings.toSnakeCase("a--b"));
        assertEquals("a_b", Strings.toSnakeCase("a__-b"));
        assertEquals("a_b", Strings.toSnakeCase("a -_ b"));
        assertEquals("a-b", Strings.toKebabCase("a-_b"));
        assertEquals("a-b", Strings.toKebabCase("a_-b"));
        assertEquals("a-b", Strings.toKebabCase("a--b"));
        assertEquals("a-b", Strings.toKebabCase("a__-b"));
        assertEquals("STRASSE", Strings.toScreamingSnakeCase("straße"));
        assertNull(Strings.toSnakeCase(null));
    }

    @Test
    public void testToSnakeCase_FullCoverage() {
        assertEquals("hello_world", Strings.toSnakeCase("helloWorld"));
        assertEquals("hello_world", Strings.toSnakeCase("HelloWorld"));
        assertEquals("hello_world_api", Strings.toSnakeCase("helloWorldAPI"));
        assertEquals("io_error", Strings.toSnakeCase("IOError"));
        assertEquals("xml_parser", Strings.toSnakeCase("XMLParser"));

        // null/empty
        assertNull(Strings.toSnakeCase(null));
        assertEquals("", Strings.toSnakeCase(""));
        assertEquals("hello", Strings.toSnakeCase("hello"));
        assertEquals("abc", Strings.toSnakeCase("abc"));
    }

    @Test
    public void testToSnakeCase_NormalizesHyphen() {
        // hyphen normalized to underscore (behavior change)
        assertEquals("a_b", Strings.toSnakeCase("a-b"));
        assertEquals("first_name", Strings.toSnakeCase("first-name"));
        // existing underscores pass through unchanged
        assertEquals("first_name", Strings.toSnakeCase("first_name"));
        // case boundaries still convert
        assertEquals("hello_world", Strings.toSnakeCase("helloWorld"));
        // no duplicate underscores when hyphen sits at a case boundary
        assertEquals("a_b", Strings.toSnakeCase("a-B"));
        // normalized separators at the edges are discarded; explicit underscores are retained
        assertEquals("a", Strings.toSnakeCase(" -a- "));
        assertEquals("a", Strings.toSnakeCase("_a__"));
        assertEquals("a", Strings.toSnakeCase("-a-"));
        assertEquals("first_name", Strings.toSnakeCase("_first__name_"));
        assertEquals("A", Strings.toScreamingSnakeCase(" -a- "));
    }

    @Test
    public void testToSnakeCase_SplitChar() {
        // custom split char normalized to underscore
        assertEquals("a_b", Strings.toSnakeCase("a.b", '.'));
        assertEquals("first_name", Strings.toSnakeCase("first.name", '.'));
        assertEquals("first_name", Strings.toSnakeCase("firstName", '.'));
        // '-' split char also works (base method normalizes hyphens)
        assertEquals("a_b", Strings.toSnakeCase("a-b", '-'));
        // null/empty
        assertNull(Strings.toSnakeCase(null, '.'));
        assertEquals("", Strings.toSnakeCase("", '.'));
    }

    // toUpperCamelCase

    @Test
    public void testToUpperCamelCase() {
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello_world"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("HELLO_WORLD"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello-world"));
        assertNull(Strings.toUpperCamelCase(null));
        assertEquals("", Strings.toUpperCamelCase(""));
    }

    @Test
    public void testToUpperCamelCase_startPositionIndexBugFix() {
        // The fix in toUpperCamelCase mirrors the one in toCamelCase.
        // Every token (including the first) must have its first letter uppercased.
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello_world"));
        assertEquals("HelloWorldFoo", Strings.toUpperCamelCase("hello_world_foo"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("HELLO_WORLD"));
        assertEquals("HelloWorldFoo", Strings.toUpperCamelCase("HELLO_WORLD_FOO"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello-world"));
        assertEquals("FirstName", Strings.toUpperCamelCase("first_name"));
        assertEquals("FirstName", Strings.toUpperCamelCase("FIRST_NAME"));

        // Three tokens
        assertEquals("OneTwoThree", Strings.toUpperCamelCase("one_two_three"));
        assertEquals("OneTwoThree", Strings.toUpperCamelCase("ONE_TWO_THREE"));

        // Single-char tokens
        assertEquals("ABC", Strings.toUpperCamelCase("a_b_c"));
    }

    @Test
    public void testToUpperCamelCase_WithSplitChar() {
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello_world", '_'));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello-world", '-'));
        assertNull(Strings.toUpperCamelCase(null, '_'));
    }

    @Test
    public void testToUpperCamelCase_WithStringsArray() {
        // via char split
        assertEquals("FirstName", Strings.toUpperCamelCase("first_name"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello_world"));

        // single word (no split)
        assertEquals("Hello", Strings.toUpperCamelCase("hello"));
        assertEquals("Hello", Strings.toUpperCamelCase("Hello")); // already capitalized

        // null/empty
        assertNull(Strings.toUpperCamelCase((String) null));
        assertEquals("", Strings.toUpperCamelCase(""));
    }

    // toUpperCase

    @Test
    public void testToUpperCase() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals("HELLO", Strings.toUpperCase("hello"));
        assertEquals("", Strings.toUpperCase(""));
        assertNull(Strings.toUpperCase(null));
    }

    @Test
    public void testToUpperCase_Char() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals('Z', Strings.toUpperCase('z'));
        assertEquals('A', Strings.toUpperCase('A'));
        assertEquals('1', Strings.toUpperCase('1'));
    }

    @Test
    public void testToUpperCase_String() {
        assertEquals("ABC", Strings.toUpperCase("abc"));
        assertEquals("ABC", Strings.toUpperCase("ABC"));
        assertEquals("ABC123", Strings.toUpperCase("abc123"));
        assertNull(Strings.toUpperCase(null));
        assertEquals("", Strings.toUpperCase(""));
    }

    @Test
    public void testToUpperCase_WithLocale() {
        assertEquals("ABC", Strings.toUpperCase("abc", Locale.ENGLISH));
        assertNull(Strings.toUpperCase(null, Locale.ENGLISH));
        assertNull(Strings.toUpperCase(null, null));
        assertEquals("", Strings.toUpperCase("", null));
        assertThrows(NullPointerException.class, () -> Strings.toUpperCase("a", null));
    }

    @Test
    public void testToUpperCaseChar() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals('A', Strings.toUpperCase('A'));
    }

    @Test
    public void testToUpperCaseString() {
        assertNull(Strings.toUpperCase(null));
        assertEquals("", Strings.toUpperCase(""));
        assertEquals("ABC", Strings.toUpperCase("aBc"));
    }

    @Test
    public void testUncapitalize() {
        assertEquals("abc", Strings.uncapitalize("Abc"));
        assertEquals("abc", Strings.uncapitalize("abc"));
        assertEquals("aBC", Strings.uncapitalize("ABC"));
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
    }

    @Test
    public void testUncapitalize_EdgeCases() {
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
        assertEquals("a", Strings.uncapitalize("A"));
        assertEquals("hello", Strings.uncapitalize("Hello"));
        assertEquals("hello", Strings.uncapitalize("hello"));
        assertEquals("123", Strings.uncapitalize("123"));
    }

    @Test
    public void testUnicodePredicates_SupplementaryCodePoints() {
        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));
        final String osmanyaDigit = new String(Character.toChars(0x104A0));

        assertTrue(Strings.isAllUpperCase(deseretCapital));
        assertTrue(Strings.isAllLowerCase(deseretSmall));
        assertTrue(Strings.isMixedCase(deseretCapital + deseretSmall));
        assertTrue(Strings.isAlpha(deseretCapital + deseretSmall));
        assertTrue(Strings.isAlphaSpace(deseretCapital + " " + deseretSmall));
        assertTrue(Strings.isAlphanumeric(deseretCapital + osmanyaDigit));
        assertTrue(Strings.isAlphanumericSpace(deseretCapital + " " + osmanyaDigit));
        assertTrue(Strings.isNumeric(osmanyaDigit));
        assertTrue(Strings.isNumericSpace(osmanyaDigit + " " + osmanyaDigit));
    }

}
