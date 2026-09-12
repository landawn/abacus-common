package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.StrUtil;

public class StringsContainsTest extends StringsTestSupport {
    @Test
    public void testContains_Char() {
        assertTrue(Strings.contains("abc", 'a'));
        assertTrue(Strings.contains("abc", 'c'));
        assertFalse(Strings.contains("abc", 'x'));
        assertFalse(Strings.contains(null, 'a'));
    }

    @Test
    public void testContains_String() {
        assertTrue(Strings.contains("abcdef", "abc"));
        assertTrue(Strings.contains("abcdef", "def"));
        assertFalse(Strings.contains("abcdef", "xyz"));
        assertFalse(Strings.contains(null, "abc"));
    }

    @Test
    public void testContains() {
        assertTrue(Strings.contains("hello", 'e'));
        assertFalse(Strings.contains("hello", 'x'));
        assertFalse(Strings.contains(null, 'e'));

        assertTrue(Strings.contains("hello", "ell"));
        assertFalse(Strings.contains("hello", "xyz"));
        assertFalse(Strings.contains(null, "ell"));
    }

    @Test
    public void testContainsChar() {
        assertFalse(Strings.contains(null, 'a'));
        assertFalse(Strings.contains("", 'a'));
        assertTrue(Strings.contains("abc", 'a'));
        assertFalse(Strings.contains("abc", 'd'));
    }

    @Test
    public void testContainsString() {
        assertFalse(Strings.contains(null, "a"));
        assertFalse(Strings.contains("", "a"));
        assertTrue(Strings.contains("abc", "a"));
        assertTrue(Strings.contains("abc", "ab"));
        assertFalse(Strings.contains("abc", "d"));
    }

    @Test
    public void testContains_WithDelimiter() {
        assertTrue(StrUtil.containsToken("a,b,c", "b", ","));
        assertFalse(StrUtil.containsToken("a,b,c", "d", ","));
        assertFalse(StrUtil.containsToken(null, "b", ","));
    }

    @Test
    public void testContains_CharEdgeCases() {
        assertTrue(Strings.contains("abc", 'b'));
        assertFalse(Strings.contains("abc", 'x'));
        assertFalse(Strings.contains(null, 'a'));
        assertFalse(Strings.contains("", 'a'));
    }

    @Test
    public void testContains_StringEdgeCases() {
        assertTrue(Strings.contains("abc", "bc"));
        assertFalse(Strings.contains("abc", "xy"));
        assertFalse(Strings.contains(null, "a"));
        assertFalse(Strings.contains("abc", null));
    }

    @Test
    public void testContainsIgnoreCase() {
        assertTrue(Strings.containsIgnoreCase("AbCdEf", "abc"));
        assertTrue(Strings.containsIgnoreCase("abcdef", "DEF"));
        assertFalse(Strings.containsIgnoreCase("abc", "xyz"));
        assertFalse(Strings.containsIgnoreCase(null, "abc"));
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
    public void testContainsIgnoreCase_WithDelimiter() {
        assertTrue(StrUtil.containsTokenIgnoreCase("a,B,c", "b", ","));
        assertFalse(StrUtil.containsTokenIgnoreCase("a,b,c", "d", ","));
        assertFalse(StrUtil.containsTokenIgnoreCase(null, "b", ","));
    }

    @Test
    public void testContainsIgnoreCase_EdgeCases() {
        assertTrue(Strings.containsIgnoreCase("ABC", "abc"));
        assertTrue(Strings.containsIgnoreCase("abc", "BC"));
        assertFalse(Strings.containsIgnoreCase("abc", "xy"));
        assertFalse(Strings.containsIgnoreCase(null, "a"));
    }

    @Test
    public void testContainsAll() {
        assertTrue(Strings.containsAll("hello world", 'h', 'e', 'l'));
        assertFalse(Strings.containsAll("hello", 'h', 'x'));
        assertTrue(Strings.containsAll("", new char[0]));

        assertTrue(Strings.containsAll("hello world", "hello", "world"));
        assertFalse(Strings.containsAll("hello", "hello", "xyz"));
    }

    @Test
    public void test_containsAll() {
        final String str = "abcD12";
        assertTrue(Strings.containsAll(str, 'a', 'D', '2'));
        assertTrue(Strings.containsAllIgnoreCase(str, "A", "d", "2"));
        assertFalse(Strings.containsAll(str, "a", "d", "2"));
    }

    @Test
    public void testContainsAll_StringArray() {
        assertTrue(Strings.containsAll("abcdefg", "abc", "def"));
        assertFalse(Strings.containsAll("abcdef", "abc", "xyz"));
        assertFalse(Strings.containsAll(null, "abc", "def"));
    }

    @Test
    public void testContainsAllChars() {
        assertTrue(Strings.containsAll("abcd", 'a', 'b'));
        assertFalse(Strings.containsAll("abcd", 'a', 'e'));
        assertTrue(Strings.containsAll("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertFalse(Strings.containsAll(null, 'a'));
    }

    @Test
    public void testContainsAllStrings() {
        assertTrue(Strings.containsAll("abcdef", "ab", "cd"));
        assertFalse(Strings.containsAll("abcdef", "ab", "gh"));
        assertTrue(Strings.containsAll("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertFalse(Strings.containsAll(null, "a"));
        assertTrue(Strings.containsAll("", ""));
        assertTrue(Strings.containsAll("x", ""));
        assertFalse(Strings.containsAll("", "a"));
        assertEquals(Strings.contains("", ""), Strings.containsAll("", ""));
    }

    @Test
    public void testContainsAll_CharArray() {
        assertTrue(Strings.containsAll("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsAll("abc", 'a', 'x'));
        assertTrue(Strings.containsAll("abc", (char[]) null));
        assertFalse(Strings.containsAll(null, 'a', 'b'));
        // Lone surrogate code units are treated as ordinary UTF-16 code units (no validation, no exception).
        assertFalse(Strings.containsAll("abc", '\uDC00', 'a')); // '\uDC00' is not present in "abc"
        assertFalse(Strings.containsAll("abc", '\uDFFF', 'a')); // '\uDFFF' is not present in "abc"
    }

    @Test
    public void testContainsAllIgnoreCase() {
        assertTrue(Strings.containsAllIgnoreCase("AbCdEfG", "abc", "def"));
        assertFalse(Strings.containsAllIgnoreCase("abcdef", "abc", "xyz"));
        assertFalse(Strings.containsAllIgnoreCase(null, "abc"));
    }

    @Test
    public void testContainsAny() {
        assertTrue(Strings.containsAny("hello", 'h', 'x'));
        assertFalse(Strings.containsAny("hello", 'x', 'y'));
        assertFalse(Strings.containsAny("", 'x'));

        assertTrue(Strings.containsAny("hello", "world", "ell"));
        assertFalse(Strings.containsAny("hello", "xyz", "abc"));
    }

    @Test
    public void testContainsAny_CharArray() {
        assertTrue(Strings.containsAny("abc", 'a', 'x'));
        assertTrue(Strings.containsAny("abc", 'c', 'y'));
        assertFalse(Strings.containsAny("abc", 'x', 'y'));
        assertFalse(Strings.containsAny(null, 'a', 'b'));
    }

    @Test
    public void testContainsAny_StringArray() {
        assertTrue(Strings.containsAny("abcdef", "abc", "xyz"));
        assertTrue(Strings.containsAny("abcdef", "xyz", "def"));
        assertFalse(Strings.containsAny("abc", "xyz", "123"));
        assertFalse(Strings.containsAny(null, "abc", "def"));
    }

    @Test
    public void testContainsAnyChars() {
        assertTrue(Strings.containsAny("abcd", 'a', 'e'));
        assertFalse(Strings.containsAny("abcd", 'e', 'f'));
        assertFalse(Strings.containsAny("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertFalse(Strings.containsAny(null, 'a'));
    }

    @Test
    public void testContainsAnyStrings() {
        assertTrue(Strings.containsAny("abcdef", "ab", "gh"));
        assertFalse(Strings.containsAny("abcdef", "gh", "ij"));
        assertFalse(Strings.containsAny("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertFalse(Strings.containsAny(null, "a"));
        assertTrue(Strings.containsAny("", ""));
        assertTrue(Strings.containsAny("x", ""));
        assertFalse(Strings.containsAny("", "a"));
        assertEquals(Strings.contains("", ""), Strings.containsAny("", ""));
        assertTrue(Strings.containsAllIgnoreCase("", ""));
        assertTrue(Strings.containsAnyIgnoreCase("", ""));
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
    public void testContainsNone_CharArray() {
        assertTrue(Strings.containsNone("abc", 'x', 'y'));
        assertTrue(Strings.containsNone("", 'a'));
        assertTrue(Strings.containsNone(null, 'a'));
        assertFalse(Strings.containsNone("abc", 'a', 'x'));
    }

    @Test
    public void testContainsNone_StringArray() {
        assertTrue(Strings.containsNone("abc", "xyz", "123"));
        assertFalse(Strings.containsNone("abcdef", "abc", "xyz"));
        assertTrue(Strings.containsNone(null, "abc"));
    }

    @Test
    public void testContainsNone() {
        assertTrue(Strings.containsNone("hello", 'x', 'y'));
        assertFalse(Strings.containsNone("hello", 'h', 'x'));
        assertTrue(Strings.containsNone("", 'x'));
        assertTrue(Strings.containsNone(null, 'x'));

        assertTrue(Strings.containsNone("hello", "xyz", "abc"));
        assertFalse(Strings.containsNone("hello", "world", "ell"));
    }

    @Test
    public void testContainsNoneChars() {
        assertTrue(Strings.containsNone("abcd", 'e', 'f'));
        assertFalse(Strings.containsNone("abcd", 'a', 'e'));
        assertTrue(Strings.containsNone("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertTrue(Strings.containsNone(null, 'a'));
        assertEquals(!Strings.containsAny(null, 'a'), Strings.containsNone(null, 'a'));
    }

    @Test
    public void testContainsNoneStrings() {
        assertTrue(Strings.containsNone("abcdef", "gh", "ij"));
        assertFalse(Strings.containsNone("abcdef", "ab", "gh"));
        assertTrue(Strings.containsNone("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertTrue(Strings.containsNone(null, "a"));
        assertFalse(Strings.containsNone("", ""));
        assertTrue(Strings.containsAny("", ""));
        assertTrue(Strings.containsNone("", "a"));
        assertFalse(Strings.containsNone("", "a", ""));
        assertFalse(Strings.containsNone("x", ""));
        assertTrue(Strings.containsNone("", (String[]) null));
        assertEquals(!Strings.containsAny("", ""), Strings.containsNone("", ""));
        assertEquals(!Strings.containsAny("x", ""), Strings.containsNone("x", ""));
        assertTrue(Strings.containsNone(null, ""));
        assertFalse(Strings.containsAny(null, ""));
        assertTrue(Strings.containsNone(null, (String[]) null));
        assertEquals(!Strings.containsAny(null, ""), Strings.containsNone(null, ""));
        assertEquals(!Strings.containsAny(null, "a"), Strings.containsNone(null, "a"));
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
    public void testContainsOnly_StringContainsOnlySpecifiedChars() {
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertTrue(Strings.containsOnly("abcabc", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("cba", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("a", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("ab", 'a', 'b', 'c'));
    }

    @Test
    public void testContainsOnly_StringContainsOtherChars() {
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("axb", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("xyz", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("ab c", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("123", 'a', 'b', 'c'));
    }

    @Test
    public void testContainsOnly_SpecialCharacters() {
        assertTrue(Strings.containsOnly(" ", ' '));
        assertTrue(Strings.containsOnly("\t\n", '\t', '\n'));
        assertTrue(Strings.containsOnly("!@#", '!', '@', '#'));
        assertTrue(Strings.containsOnly("123", '1', '2', '3'));

        assertFalse(Strings.containsOnly("Hello World", 'H', 'e', 'l', 'o'));
        assertTrue(Strings.containsOnly("Hello", 'H', 'e', 'l', 'o'));
    }

    @Test
    public void testContainsOnly_UnicodeCharacters() {
        assertTrue(Strings.containsOnly("αβγ", 'α', 'β', 'γ'));
        assertFalse(Strings.containsOnly("αβγδ", 'α', 'β', 'γ'));
    }

    @Test
    public void testContainsOnly_RepeatedValuesToFind() {
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c', 'a', 'b'));
        assertTrue(Strings.containsOnly("aaa", 'a', 'a', 'a'));
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c', 'a', 'b'));
    }

    @Test
    public void testContainsOnly_LongStrings() {
        String longString = "a".repeat(1000);
        assertTrue(Strings.containsOnly(longString, 'a'));
        assertFalse(Strings.containsOnly(longString + "b", 'a'));

        String mixedLongString = "abcabc".repeat(100);
        assertTrue(Strings.containsOnly(mixedLongString, 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly(mixedLongString + "d", 'a', 'b', 'c'));
    }

    @Test
    public void testContainsOnly_Whitespace() {
        assertTrue(Strings.containsOnly("   ", ' '));
        assertTrue(Strings.containsOnly("\t\t\t", '\t'));
        assertTrue(Strings.containsOnly("\n\n", '\n'));
        assertTrue(Strings.containsOnly(" \t\n", ' ', '\t', '\n'));

        assertFalse(Strings.containsOnly("a b", 'a', 'b'));
        assertTrue(Strings.containsOnly("a b", 'a', 'b', ' '));
    }

    @Test
    public void testContainsOnly_Numeric() {
        assertTrue(Strings.containsOnly("123", '1', '2', '3'));
        assertTrue(Strings.containsOnly("000", '0'));
        assertTrue(Strings.containsOnly("1234567890", '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));

        assertFalse(Strings.containsOnly("123a", '1', '2', '3'));
        assertFalse(Strings.containsOnly("12.3", '1', '2', '3'));
    }

    @Test
    public void testContainsOnly() {
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abc", 'a', 'b'));
        assertFalse(Strings.containsOnly(null, 'a'));
    }

    @Test
    public void testContainsOnly_NullString() {
        assertFalse(Strings.containsOnly(null, 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly(null, 'x'));
        assertFalse(Strings.containsOnly(null));
    }

    @Test
    public void testContainsOnly_EmptyString() {
        assertTrue(Strings.containsOnly("", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("", 'x'));
        assertTrue(Strings.containsOnly("", new char[0]));
    }

    @Test
    public void testContainsOnly_EmptyValuesToFind() {
        assertFalse(Strings.containsOnly("abc", new char[0]));
        assertFalse(Strings.containsOnly("a", new char[0]));
        assertFalse(Strings.containsOnly(" ", new char[0]));

        assertFalse(Strings.containsOnly("abc", (char[]) null));
    }

    @Test
    public void testContainsOnly_SingleCharacter() {
        assertTrue(Strings.containsOnly("a", 'a'));
        assertTrue(Strings.containsOnly("a", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("x", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("a", 'b', 'c'));
    }

    @Test
    public void testContainsOnly_CaseSensitive() {
        assertFalse(Strings.containsOnly("ABC", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abc", 'A', 'B', 'C'));
        assertTrue(Strings.containsOnly("ABC", 'A', 'B', 'C'));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
    }

    @Test
    public void testContainsOnly_EdgeCases() {
        assertTrue(Strings.containsOnly("a", 'a'));

        assertTrue(Strings.containsOnly("aaabbbccc", 'a', 'b', 'c'));

        assertTrue(Strings.containsOnly("ab", 'a', 'b', 'c', 'd', 'e'));

        assertTrue(Strings.containsOnly("aaaa", 'a'));

        assertTrue(Strings.containsOnly("abcd", 'a', 'b', 'c', 'd'));
    }

    @Test
    public void testContainsOnly_TreatsSurrogateAsCodeUnit() {
        char highSurrogate = '\uD800';
        char lowSurrogate = '\uDC00';

        // No validation/exception: "test" contains characters outside the allowed set, so containsOnly is false.
        assertFalse(Strings.containsOnly("test", highSurrogate));
        assertFalse(Strings.containsOnly("test", lowSurrogate));
        assertFalse(Strings.containsOnly("test", 'a', highSurrogate, 'b'));

        // A string made up only of the given (surrogate) code units is matched as code units.
        assertTrue(Strings.containsOnly("\uD800\uD800", highSurrogate));
    }

    @Test
    public void testContainsWhitespace_NoWhitespace() {
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace("123"));
        assertFalse(Strings.containsWhitespace("!@#$%"));
        assertFalse(Strings.containsWhitespace("αβγ"));
    }

    @Test
    public void testContainsWhitespace_WithSpace() {
        assertTrue(Strings.containsWhitespace(" "));
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace(" abc"));
        assertTrue(Strings.containsWhitespace("abc "));
        assertTrue(Strings.containsWhitespace("a b c"));
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
    public void testContainsWhitespace() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("a\tb"));
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    public void testContainsWhitespace_NullString() {
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    public void testContainsWhitespace_EmptyString() {
        assertFalse(Strings.containsWhitespace(""));
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
}
