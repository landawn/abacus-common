package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.u.Optional;

/**
 * Tests for the fixes of the second 2026-09-02 review of {@link Strings}.
 *
 * <ul>
 *   <li><b>B1</b> the camel-case capitalizer looked for the word's first <i>letter</i> ({@code Character.isLetter})
 *       and so skipped cased code points that are not letters: the circled letters (U+24B6..U+24E9, {@code So}),
 *       the Roman numerals (U+2160..U+217F, {@code Nl}) and the squared capitals (U+1F130.., {@code So}). The
 *       case-boundary rule and every sibling ({@code capitalize}, {@code swapCase}, {@code isMixedCase},
 *       {@code toSnakeCase}) already treat them as cased, so {@code toUpperCamelCase("ⓐbc")} capitalized the
 *       {@code 'b'} ({@code "ⓐBc"}) and {@code toCamelCase("xⅧ")} split at Ⅷ, lowercased it and lost the capital
 *       ({@code "xⅷ"}). The capitalizer now targets the first letter <i>or cased</i> code point.</li>
 *   <li><b>D1</b> {@code substring(String, IntUnaryOperator, int)} handed the operator the raw end index while
 *       {@code substringBetween(String, IntUnaryOperator, int)} (and the fixed-index twin
 *       {@code substring(String, int, int)}) clamp the end to the string length, so
 *       {@code substring("Hello", i -> i - 3, 100)} was {@code null} where the sibling returned {@code "lo"}. The
 *       operator now receives the clamped end. Only calls whose end exceeds the length change.</li>
 *   <li><b>J1</b> {@code toUpperCamelCase} no longer claims to produce "exactly" what {@code capitalizeWordsFully}
 *       produces; the two differ on digit- and punctuation-led words (pinned here).</li>
 * </ul>
 */
public class StringsRegressionFTest extends TestBase {

    // ---------------------------------------------------------------------------------------------
    // B1 : camel case capitalizes the first letter OR cased code point
    // ---------------------------------------------------------------------------------------------

    private static final String CIRCLED_A_LOWER = "ⓐ"; // ⓐ CIRCLED LATIN SMALL LETTER A (So, Other_Lowercase)
    private static final String CIRCLED_A_UPPER = "Ⓐ"; // Ⓐ CIRCLED LATIN CAPITAL LETTER A (So, Other_Uppercase)
    private static final String ROMAN_EIGHT_LOWER = "ⅷ"; // ⅷ SMALL ROMAN NUMERAL EIGHT (Nl, Other_Lowercase)
    private static final String ROMAN_EIGHT_UPPER = "Ⅷ"; // Ⅷ ROMAN NUMERAL EIGHT (Nl, Other_Uppercase)
    private static final String SQUARED_A = new String(Character.toChars(0x1F130)); // 🄰 SQUARED LATIN CAPITAL LETTER A (no lowercase mapping)

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
    public void testToCamelCase_CaseBoundaryAtCasedSymbol() {
        // Ⅷ is an uppercase boundary for the splitter; the re-capitalized word must keep it uppercase.
        assertEquals("x" + ROMAN_EIGHT_UPPER, Strings.toCamelCase("x" + ROMAN_EIGHT_UPPER));
        assertEquals("x" + ROMAN_EIGHT_UPPER + "y", Strings.toCamelCase("x" + ROMAN_EIGHT_UPPER + "y"));
        assertEquals("X" + ROMAN_EIGHT_UPPER + "y", Strings.toUpperCamelCase("x" + ROMAN_EIGHT_UPPER + "y"));
        // Both directions of the round trip through the delimited forms now agree on the capital.
        assertEquals("x_" + ROMAN_EIGHT_LOWER, Strings.toSnakeCase("x" + ROMAN_EIGHT_UPPER));
        assertEquals("x" + ROMAN_EIGHT_UPPER, Strings.toCamelCase(Strings.toSnakeCase("x" + ROMAN_EIGHT_UPPER)));
    }

    @Test
    public void testToCamelCase_AgreesWithCapitalizeForCasedSymbols() {
        for (final String word : new String[] { CIRCLED_A_LOWER + "bc", ROMAN_EIGHT_LOWER, ROMAN_EIGHT_UPPER, "ⅰⅱ", "ⓩ" }) {
            assertEquals(Strings.capitalize(word.toLowerCase(java.util.Locale.ROOT)), Strings.toUpperCamelCase(word), word);
            assertEquals(Strings.capitalizeWordsFully(word), Strings.toUpperCamelCase(word), word);
        }
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

    // ---------------------------------------------------------------------------------------------
    // D1 : substring(String, IntUnaryOperator, int) clamps the end before the operator sees it
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testSubstring_OperatorReceivesClampedEnd() {
        final int[] seen = { -1 };
        final IntUnaryOperator recording = end -> {
            seen[0] = end;
            return end - 2;
        };

        assertEquals("lo", Strings.substring("Hello", recording, 100));
        assertEquals(5, seen[0]);

        assertEquals("lo", Strings.substring("Hello", i -> i - 2, Integer.MAX_VALUE));
        assertEquals("Hello", Strings.substring("Hello", i -> 0, 100));
        assertEquals("", Strings.substring("Hello", i -> i, 100));
        assertNull(Strings.substring("Hello", i -> i + 1, 100)); // begin > clamped end
        assertNull(Strings.substring("Hello", i -> -1, 100)); // negative begin
    }

    @Test
    public void testSubstring_OperatorAgreesWithSubstringBetween() {
        final String str = "Hello World";

        for (final int end : new int[] { 0, 1, 5, 11, 12, 50, Integer.MAX_VALUE }) {
            for (final int back : new int[] { 0, 1, 3, 11, 12 }) {
                final String expected = Strings.substring(str, Math.max(0, Math.min(end, str.length()) - back), end);
                assertEquals(expected, Strings.substring(str, e -> Math.max(0, e - back), end), "end=" + end + " back=" + back);
                // substringBetween's begin is exclusive, so begin - 1 reproduces the same range.
                assertEquals(expected, Strings.substringBetween(str, e -> Math.max(0, e - back) - 1, end), "end=" + end + " back=" + back);
            }
        }
    }

    @Test
    public void testSubstring_OperatorInRangeUnchanged() {
        assertEquals("Wor", Strings.substring("Hello World", i -> i - 3, 9));
        assertEquals("Hello", Strings.substring("Hello World", i -> i - 5, 5));
        assertEquals("Hel", Strings.substring("Hello", i -> 0, 3));
        assertEquals("", Strings.substring("Hello", i -> i, 3));
        assertNull(Strings.substring("Hello", i -> i - 3, -1));
        assertNull(Strings.substring(null, i -> 0, 5));
        assertNull(Strings.substring("Hello", i -> 5, 3));
        assertEquals("", Strings.substring("", i -> 0, 0));
        assertEquals("", Strings.substring("", i -> 0, 7));
    }

    @Test
    public void testSubstring_OperatorNotInvokedWhenShortCircuit() {
        final int[] calls = { 0 };
        final IntUnaryOperator counting = end -> {
            calls[0]++;
            return 0;
        };

        assertNull(Strings.substring(null, counting, 5));
        assertNull(Strings.substring("Hello", counting, -1));
        assertEquals(0, calls[0]);
    }

    @Test
    public void testStrUtil_OperatorMirrorsClamp() {
        assertEquals(Optional.of("lo"), StrUtil.substring("hello", end -> end - 2, 99));
        assertEquals("lo", StrUtil.substringOrElse("hello", end -> end - 2, 99, "default"));
        assertEquals("lo", StrUtil.substringOrElseItself("hello", end -> end - 2, 99));
        assertEquals("default", StrUtil.substringOrElse("hello", end -> end + 1, 99, "default"));
        assertSame("hello", StrUtil.substringOrElseItself("hello", end -> end + 1, 99));
        assertEquals(Optional.empty(), StrUtil.substring("hello", end -> end + 1, 99));
    }
}
