package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class StringsToTest extends StringsTestSupport {
    @Test
    public void testToCharArray() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Strings.toCharArray("abc"));
        assertArrayEquals(new char[0], Strings.toCharArray(""));
        // Empty input returns the shared N.EMPTY_CHAR_ARRAY constant by design.
        assertSame(Strings.toCharArray(""), Strings.toCharArray(""));
        assertNull(Strings.toCharArray(null));
    }

    @Test
    public void testToCharArray_NonStringCharSequence() {
        // CharSequence that is not a String triggers the manual copy path
        StringBuilder sb = new StringBuilder("abc");
        char[] result = Strings.toCharArray(sb);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        // null input
        assertNull(Strings.toCharArray((CharSequence) null));

        // empty CharSequence
        assertArrayEquals(new char[0], Strings.toCharArray(new StringBuilder()));

        // String path (fast path)
        assertArrayEquals(new char[] { 'h', 'i' }, Strings.toCharArray("hi"));
    }

    @Test
    public void testToCodePoints() {
        int[] codePoints = Strings.toCodePoints("abc");
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, codePoints);
        assertArrayEquals(new int[0], Strings.toCodePoints(""));
        // Empty input returns the shared N.EMPTY_INT_ARRAY constant by design.
        assertSame(Strings.toCodePoints(""), Strings.toCodePoints(""));
        assertNull(Strings.toCodePoints(null));
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

    @Test
    public void testToLowerCase() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals("hello", Strings.toLowerCase("HELLO"));
        assertEquals("", Strings.toLowerCase(""));
        assertNull(Strings.toLowerCase(null));
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
    public void testToUpperCase() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals("HELLO", Strings.toUpperCase("hello"));
        assertEquals("", Strings.toUpperCase(""));
        assertNull(Strings.toUpperCase(null));
    }

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
    public void testToUpperCamelCase() {
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello_world"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("HELLO_WORLD"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello-world"));
        assertNull(Strings.toUpperCamelCase(null));
        assertEquals("", Strings.toUpperCamelCase(""));
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
    public void testToPascalCase() {
        // toPascalCase is a supported alias for toUpperCamelCase
        assertNull(Strings.toPascalCase(null));
        assertEquals("", Strings.toPascalCase(""));
        assertEquals("HelloWorld", Strings.toPascalCase("hello_world"));
    }

    @Test
    public void testToPascalCase_WithSplitChar() {
        assertEquals("FirstName", Strings.toPascalCase("first.name", '.'));
        assertNull(Strings.toPascalCase(null, '.'));
    }

    @Test
    public void testToPascalCase_EdgeCases() {
        assertNull(Strings.toPascalCase(null));
        assertEquals("", Strings.toPascalCase(""));
        assertEquals("HelloWorld", Strings.toPascalCase("hello_world"));
        assertEquals("HelloWorld", Strings.toPascalCase("HELLO_WORLD"));
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
    public void testToUnicodeEscape_CodePoint() {
        assertEquals(Strings.toUnicodeEscape('A'), Strings.toUnicodeEscape(0x41));
        assertEquals("\\u20ac", Strings.toUnicodeEscape(0x20AC));
        assertEquals("\\ud83d\\ude00", Strings.toUnicodeEscape(0x1F600));
        assertEquals(Strings.toUnicodeEscape('\uD800'), Strings.toUnicodeEscape(0xD800));
        assertThrows(IllegalArgumentException.class, () -> Strings.toUnicodeEscape(-1));
        assertThrows(IllegalArgumentException.class, () -> Strings.toUnicodeEscape(Character.MAX_CODE_POINT + 1));
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
}
