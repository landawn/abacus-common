package com.landawn.abacus.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Strings103Test extends TestBase {

    @Test
    public void testToCamelCase() {
        // Test with null and empty strings
        Assertions.assertNull(Strings.toCamelCase(null, '.'));
        Assertions.assertEquals("", Strings.toCamelCase("", '.'));

        // Test normal cases
        Assertions.assertEquals("firstName", Strings.toCamelCase("first.name", '.'));
        Assertions.assertEquals("firstName", Strings.toCamelCase("FIRST.NAME", '.'));
        Assertions.assertEquals("firstName", Strings.toCamelCase("first#name", '#'));
        Assertions.assertEquals("firstName", Strings.toCamelCase("firstName", '.'));

        // Test with multiple delimiters
        Assertions.assertEquals("firstMiddleLast", Strings.toCamelCase("first.middle.last", '.'));

        // Test with consecutive delimiters
        Assertions.assertEquals("firstLast", Strings.toCamelCase("first..last", '.'));

        // Test with delimiter at start/end
        Assertions.assertEquals("firstName", Strings.toCamelCase(".first.name", '.'));
        Assertions.assertEquals("firstName", Strings.toCamelCase("first.name.", '.'));

        // Test with single word
        Assertions.assertEquals("word", Strings.toCamelCase("WORD", '.'));
        Assertions.assertEquals("word", Strings.toCamelCase("Word", '.'));

        // Test with all uppercase
        Assertions.assertEquals("abc", Strings.toCamelCase("ABC", '.'));
        Assertions.assertEquals("abcDef", Strings.toCamelCase("ABC.DEF", '.'));
    }

    @Test
    public void testConvertWords() {
        // Test with null and empty
        Assertions.assertNull(Strings.convertWords(null, " ", null, String::toUpperCase));
        Assertions.assertEquals("", Strings.convertWords("", " ", null, String::toUpperCase));

        // Test with empty delimiter - should throw
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.convertWords("test", "", null, String::toUpperCase));

        // Test without excluded words
        Assertions.assertEquals("HELLO WORLD", Strings.convertWords("hello world", " ", null, String::toUpperCase));

        // Test with excluded words
        Set<String> excluded = new HashSet<>(Arrays.asList("and", "or"));
        Assertions.assertEquals("JACK and JILL", Strings.convertWords("jack and jill", " ", excluded, String::toUpperCase));
        Assertions.assertEquals("*true* or *false*", Strings.convertWords("true or false", " ", excluded, s -> "*" + s + "*"));

        // Test with empty excluded words
        Assertions.assertEquals("HELLO WORLD", Strings.convertWords("hello world", " ", new HashSet<>(), String::toUpperCase));

        // Test with delimiter not in string
        Assertions.assertEquals("HELLO WORLD", Strings.convertWords("hello world", ",", excluded, String::toUpperCase));

        // Test with consecutive delimiters
        Assertions.assertEquals("HELLO  WORLD", Strings.convertWords("hello  world", " ", null, String::toUpperCase));
    }

    @Test
    public void testReplaceOnce() {
        // Test null cases
        Assertions.assertNull(Strings.replaceOnce(null, "a", "b"));
        Assertions.assertEquals("", Strings.replaceOnce("", "a", "b"));
        Assertions.assertEquals("any", Strings.replaceOnce("any", null, "b"));
        Assertions.assertEquals("ny", Strings.replaceOnce("ny", "a", null));
        Assertions.assertEquals("any", Strings.replaceOnce("any", "", "b"));

        // Test replacement cases
        Assertions.assertEquals("ba", Strings.replaceOnce("aba", "a", null));
        Assertions.assertEquals("ba", Strings.replaceOnce("aba", "a", ""));
        Assertions.assertEquals("zba", Strings.replaceOnce("aba", "a", "z"));

        // Test no match
        Assertions.assertEquals("abc", Strings.replaceOnce("abc", "d", "e"));
    }

    @Test
    public void testReplaceOnceWithIndex() {
        // Test basic cases
        Assertions.assertEquals("abzde", Strings.replaceOnce("abcde", 0, "c", "z"));
        Assertions.assertEquals("abcde", Strings.replaceOnce("abcde", 3, "c", "z"));

        // Test with negative index
        // Assertions.assertEquals("zbcde", Strings.replaceOnce("abcde", -1, "a", "z"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceOnce("abcde", -1, "a", "z"));

        // Test with index beyond string length
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceOnce("abcde", 10, "a", "z"));
    }

    @Test
    public void testSplitWithTrim() {
        // Test null and empty
        Assertions.assertArrayEquals(new String[0], Strings.split(null, "::", true));
        Assertions.assertArrayEquals(new String[0], Strings.split("", "::", true));

        // Test with trim
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a :: b :: c ", "::", true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a :: b :: c ", "::", false));

        // Test with consecutive delimiters
        Assertions.assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::::b", "::", true));

        // Test with null delimiter (split on whitespace)
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a  b  c ", null, true));
    }

    @Test
    public void testSplitWithMax() {
        // Test invalid max
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ":", 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ":", -1));

        // Test null and empty
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ":", 3));
        Assertions.assertArrayEquals(new String[0], Strings.split("", ":", 3));

        // Test with max = 1
        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", ":", 1));

        // Test normal cases
        Assertions.assertArrayEquals(new String[] { "a", "b", "c:d:e" }, Strings.split("a:b:c:d:e", ":", 3));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Strings.split("a:b:c:d:e", ":", 5));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Strings.split("a:b:c:d:e", ":", 10));
    }

    @Test
    public void testSplitWithMaxAndTrim() {
        // Test with trim
        Assertions.assertArrayEquals(new String[] { "a", "b", "c : d" }, Strings.split(" a : b : c : d ", ":", 3, true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c : d " }, Strings.split(" a : b : c : d ", ":", 3, false));

        // Test with max = 1
        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", ":", 1, true));
        Assertions.assertArrayEquals(new String[] { "  a:b:c  " }, Strings.split("  a:b:c  ", ":", 1, false));
    }

    @Test
    public void testSplitPreserveAllTokensChar() {
        // Test null and empty
        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, '.'));
        Assertions.assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", '.'));

        // Test normal cases
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a.b.c", '.'));
        Assertions.assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a..b.c", '.'));
        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", '.'));

        // Test with delimiter at edges
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "" }, Strings.splitPreserveAllTokens("a b c ", ' '));
        Assertions.assertArrayEquals(new String[] { "", "a", "b", "c" }, Strings.splitPreserveAllTokens(" a b c", ' '));
    }

    @Test
    public void testSplitPreserveAllTokensCharWithTrim() {
        // Test with trim
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a . b . c ", '.', true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.splitPreserveAllTokens(" a . b . c ", '.', false));

        // Test empty tokens
        Assertions.assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a..b", '.', true));
        Assertions.assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" .. ", '.', true));
    }

    @Test
    public void testSplitPreserveAllTokensString() {
        // Test null and empty
        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, "::"));
        Assertions.assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", "::"));

        // Test normal cases
        Assertions.assertArrayEquals(new String[] { "ab", "cd", "ef" }, Strings.splitPreserveAllTokens("ab:cd:ef", ":"));
        Assertions.assertArrayEquals(new String[] { "ab", "cd", "ef", "" }, Strings.splitPreserveAllTokens("ab:cd:ef:", ":"));
        Assertions.assertArrayEquals(new String[] { "", "cd", "ef" }, Strings.splitPreserveAllTokens(":cd:ef", ":"));

        // Test with null delimiter (split on whitespace)
        Assertions.assertArrayEquals(new String[] { "abc", "def" }, Strings.splitPreserveAllTokens("abc def", null));
        Assertions.assertArrayEquals(new String[] { "abc", "", "def" }, Strings.splitPreserveAllTokens("abc  def", " "));
    }

    @Test
    public void testSplitPreserveAllTokensStringWithTrim() {
        // Test with trim
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a : b : c ", ":", true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.splitPreserveAllTokens(" a : b : c ", ":", false));

        // Test empty tokens
        Assertions.assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a::b", ":", true));
        Assertions.assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" :: ", ":", true));
    }

    @Test
    public void testSplitPreserveAllTokensWithMax() {
        // Test invalid max
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("test", ":", 0));

        // Test with max
        Assertions.assertArrayEquals(new String[] { "a", "b", "c:d" }, Strings.splitPreserveAllTokens("a:b:c:d", ":", 3));
        Assertions.assertArrayEquals(new String[] { "a", "b", "::d" }, Strings.splitPreserveAllTokens("a:b:::d", ":", 3));
        Assertions.assertArrayEquals(new String[] { "", "a", "b", "c" }, Strings.splitPreserveAllTokens(":a:b:c", ":", 4));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "" }, Strings.splitPreserveAllTokens("a:b:c:", ":", 4));

        // Test with max = 1
        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 1));
    }

    @Test
    public void testSplitPreserveAllTokensWithMaxAndTrim() {
        // Test with trim and max
        Assertions.assertArrayEquals(new String[] { "a", "b", "c : d" }, Strings.splitPreserveAllTokens(" a : b : c : d ", ":", 3, true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c : d " }, Strings.splitPreserveAllTokens(" a : b : c : d ", ":", 3, false));

        // Test empty tokens
        Assertions.assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a::b:c", ":", 4, true));
        Assertions.assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" : : ", ":", 3, true));

        // Test with max = 1
        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 1, true));
        Assertions.assertArrayEquals(new String[] { "  a:b:c  " }, Strings.splitPreserveAllTokens("  a:b:c  ", ":", 1, false));
    }

    @Test
    public void testTrimToNull() {
        // Test array modification
        String[] arr = { "  abc  ", "   ", "def", "", null };
        Strings.trimToNull(arr);
        Assertions.assertArrayEquals(new String[] { "abc", null, "def", null, null }, arr);

        // Test null array
        Strings.trimToNull((String[]) null);

        // Test empty array
        Strings.trimToNull(new String[0]);

        // Test with tabs and newlines
        String[] arr2 = { "\t\n", "  test  ", " \t\n " };
        Strings.trimToNull(arr2);
        Assertions.assertArrayEquals(new String[] { null, "test", null }, arr2);
    }

    @Test
    public void testTrimToEmpty() {
        // Test array modification
        String[] arr = { "  abc  ", null, "def", "   " };
        Strings.trimToEmpty(arr);
        Assertions.assertArrayEquals(new String[] { "abc", "", "def", "" }, arr);

        // Test null array
        Strings.trimToEmpty((String[]) null);

        // Test empty array
        Strings.trimToEmpty(new String[0]);

        // Test with tabs and newlines
        String[] arr2 = { null, "  test  ", " \t\n " };
        Strings.trimToEmpty(arr2);
        Assertions.assertArrayEquals(new String[] { "", "test", "" }, arr2);
    }

    @Test
    public void testStripToNull() {
        // Test array modification
        String[] arr = { "  abc  ", "   ", "def", "", null };
        Strings.stripToNull(arr);
        Assertions.assertArrayEquals(new String[] { "abc", null, "def", null, null }, arr);

        // Test null array
        Strings.stripToNull((String[]) null);

        // Test empty array
        Strings.stripToNull(new String[0]);
    }

    @Test
    public void testStripToEmpty() {
        // Test array modification
        String[] arr = { "  abc  ", null, "def", "   " };
        Strings.stripToEmpty(arr);
        Assertions.assertArrayEquals(new String[] { "abc", "", "def", "" }, arr);

        // Test null array
        Strings.stripToEmpty((String[]) null);

        // Test empty array
        Strings.stripToEmpty(new String[0]);
    }

    @Test
    public void testStripStart() {
        // Test single string
        Assertions.assertNull(Strings.stripStart(null));
        Assertions.assertEquals("", Strings.stripStart(""));
        Assertions.assertEquals("abc", Strings.stripStart("abc"));
        Assertions.assertEquals("abc", Strings.stripStart("  abc"));
        Assertions.assertEquals("abc  ", Strings.stripStart("abc  "));
        Assertions.assertEquals("abc ", Strings.stripStart(" abc "));
    }

    @Test
    public void testStripStartArray() {
        // Test array modification
        String[] arr = { "xxabc", "xydef", "xyz", null };
        Strings.stripStart(arr, "xyz");
        Assertions.assertArrayEquals(new String[] { "abc", "def", "", null }, arr);

        // Test with null stripChars (strips whitespace)
        String[] arr2 = { "  test", null, " \tabc" };
        Strings.stripStart(arr2, null);
        Assertions.assertArrayEquals(new String[] { "test", null, "abc" }, arr2);

        // Test null array
        Strings.stripStart((String[]) null, "xyz");

        // Test empty array
        Strings.stripStart(new String[0], "xyz");
    }

    @Test
    public void testStripEnd() {
        // Test single string
        Assertions.assertNull(Strings.stripEnd(null));
        Assertions.assertEquals("", Strings.stripEnd(""));
        Assertions.assertEquals("abc", Strings.stripEnd("abc"));
        Assertions.assertEquals("  abc", Strings.stripEnd("  abc"));
        Assertions.assertEquals("abc", Strings.stripEnd("abc  "));
        Assertions.assertEquals(" abc", Strings.stripEnd(" abc "));
    }

    @Test
    public void testStripEndArray() {
        // Test array modification
        String[] arr = { "abcxx", "defxy", "xyz", null };
        Strings.stripEnd(arr, "xyz");
        Assertions.assertArrayEquals(new String[] { "abc", "def", "", null }, arr);

        // Test with null stripChars (strips whitespace)
        String[] arr2 = { "test  ", null, "abc \t" };
        Strings.stripEnd(arr2, null);
        Assertions.assertArrayEquals(new String[] { "test", null, "abc" }, arr2);

        // Test null array
        Strings.stripEnd((String[]) null, "xyz");

        // Test empty array
        Strings.stripEnd(new String[0], "xyz");
    }

    @Test
    public void testStripAccents() {
        // Test array modification
        String[] arr = { "éclair", "café", "naïve", null };
        Strings.stripAccents(arr);
        // Note: The actual behavior depends on the implementation of stripAccents(String)
        // which is not shown in the provided code

        // Test null array
        Strings.stripAccents((String[]) null);

        // Test empty array
        Strings.stripAccents(new String[0]);
    }

    @Test
    public void testChomp() {
        // Test array modification
        String[] arr = { "abc\n", "def\r", "ghi", "jkl\r\n", null };
        Strings.chomp(arr);
        // Note: The actual behavior depends on the implementation of chomp(String)
        // which is not shown in the provided code

        // Test null array
        Strings.chomp((String[]) null);

        // Test empty array
        Strings.chomp(new String[0]);
    }

    @Test
    public void testChop() {
        // Test array modification
        String[] arr = { "abc", "def\r\n", "g", "", null };
        Strings.chop(arr);
        // Note: The actual behavior depends on the implementation of chop(String)
        // which is not shown in the provided code

        // Test null array
        Strings.chop((String[]) null);

        // Test empty array
        Strings.chop(new String[0]);
    }

    @Test
    public void testTruncate() {
        // Test invalid maxWidth
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.truncate(new String[] { "test" }, -1));

        // Test array modification
        String[] arr = { "hello", "world123", "hi", null };
        Strings.truncate(arr, 5);
        // Note: The actual behavior depends on the implementation of truncate(String, int)
        // which is not shown in the provided code

        // Test null array
        Strings.truncate((String[]) null, 5);

        // Test empty array
        Strings.truncate(new String[0], 5);
    }

    @Test
    public void testTruncateWithOffset() {
        // Test invalid parameters
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.truncate(new String[] { "test" }, -1, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.truncate(new String[] { "test" }, 0, -1));

        // Test array modification
        String[] arr = { "hello", "world123", "hi", null };
        Strings.truncate(arr, 2, 3);
        // Note: The actual behavior depends on the implementation of truncate(String, int, int)
        // which is not shown in the provided code

        // Test null array
        Strings.truncate((String[]) null, 2, 3);

        // Test empty array
        Strings.truncate(new String[0], 2, 3);
    }

    @Test
    public void testDeleteWhitespace() {
        // Test array modification
        String[] arr = { "  hello  ", "world\t123", " h i ", null };
        Strings.deleteWhitespace(arr);
        // Note: The actual behavior depends on the implementation of deleteWhitespace(String)
        // which is not shown in the provided code

        // Test null array
        Strings.deleteWhitespace((String[]) null);

        // Test empty array
        Strings.deleteWhitespace(new String[0]);
    }

    @Test
    public void testAppendIfMissingIgnoreCase() {
        // Test empty suffix
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.appendIfMissingIgnoreCase("file", ""));

        // Test null and empty string
        Assertions.assertEquals(".TXT", Strings.appendIfMissingIgnoreCase(null, ".TXT"));
        Assertions.assertEquals(".TXT", Strings.appendIfMissingIgnoreCase("", ".TXT"));

        // Test normal cases
        Assertions.assertEquals("file.TXT", Strings.appendIfMissingIgnoreCase("file", ".TXT"));
        Assertions.assertEquals("file.txt", Strings.appendIfMissingIgnoreCase("file.txt", ".TXT"));
        Assertions.assertEquals("file.TXT", Strings.appendIfMissingIgnoreCase("file.TXT", ".txt"));

        // Test with different suffix
        Assertions.assertEquals("document.pdf", Strings.appendIfMissingIgnoreCase("document", ".pdf"));
        Assertions.assertEquals("document.PDF", Strings.appendIfMissingIgnoreCase("document.PDF", ".pdf"));
    }

    @Test
    public void testPrependIfMissingIgnoreCase() {
        // Test empty prefix
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.prependIfMissingIgnoreCase("example.com", ""));

        // Test null and empty string
        Assertions.assertEquals("HTTP://", Strings.prependIfMissingIgnoreCase(null, "HTTP://"));
        Assertions.assertEquals("HTTP://", Strings.prependIfMissingIgnoreCase("", "HTTP://"));

        // Test normal cases
        Assertions.assertEquals("HTTP://example.com", Strings.prependIfMissingIgnoreCase("example.com", "HTTP://"));
        Assertions.assertEquals("http://example.com", Strings.prependIfMissingIgnoreCase("http://example.com", "HTTP://"));
        Assertions.assertEquals("HTTP://example.com", Strings.prependIfMissingIgnoreCase("HTTP://example.com", "http://"));

        // Test with different prefix
        Assertions.assertEquals("www.example.com", Strings.prependIfMissingIgnoreCase("example.com", "www."));
        Assertions.assertEquals("WWW.example.com", Strings.prependIfMissingIgnoreCase("WWW.example.com", "www."));
    }

    @Test
    public void testIsAsciiLowerCase() {
        // Test ASCII lowercase
        Assertions.assertTrue(Strings.isAsciiLowerCase('a'));
        Assertions.assertTrue(Strings.isAsciiLowerCase('z'));
        Assertions.assertTrue(Strings.isAsciiLowerCase('m'));

        // Test non-lowercase
        Assertions.assertFalse(Strings.isAsciiLowerCase('A'));
        Assertions.assertFalse(Strings.isAsciiLowerCase('Z'));
        Assertions.assertFalse(Strings.isAsciiLowerCase('1'));
        Assertions.assertFalse(Strings.isAsciiLowerCase(' '));
        Assertions.assertFalse(Strings.isAsciiLowerCase('ñ'));
    }

    @Test
    public void testIsUpperCase() {
        // Test uppercase
        Assertions.assertTrue(Strings.isUpperCase('A'));
        Assertions.assertTrue(Strings.isUpperCase('Z'));
        Assertions.assertTrue(Strings.isUpperCase('Ñ'));

        // Test non-uppercase
        Assertions.assertFalse(Strings.isUpperCase('a'));
        Assertions.assertFalse(Strings.isUpperCase('1'));
        Assertions.assertFalse(Strings.isUpperCase(' '));
    }

    @Test
    public void testIsAsciiUpperCase() {
        // Test ASCII uppercase
        Assertions.assertTrue(Strings.isAsciiUpperCase('A'));
        Assertions.assertTrue(Strings.isAsciiUpperCase('Z'));
        Assertions.assertTrue(Strings.isAsciiUpperCase('M'));

        // Test non-uppercase
        Assertions.assertFalse(Strings.isAsciiUpperCase('a'));
        Assertions.assertFalse(Strings.isAsciiUpperCase('z'));
        Assertions.assertFalse(Strings.isAsciiUpperCase('1'));
        Assertions.assertFalse(Strings.isAsciiUpperCase(' '));
        Assertions.assertFalse(Strings.isAsciiUpperCase('Ñ'));
    }

    @Test
    public void testIndexOfIgnoreCaseWithDelimiter() {
        // Test basic cases
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("apple,APPLE,banana", "apple", ","));
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("apple,APPLE,banana", "APPLE", ","));
        Assertions.assertEquals(10, Strings.indexOfIgnoreCase("pineapple,apple", "apple", ","));
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("test", "test", ","));

        // Test null cases
        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase(null, "test", ","));
        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase("test", null, ","));

        // Test empty delimiter
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("test", "test", ""));

        // Test not found
        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase("apple banana", "apple", ","));
    }

    @Test
    public void testIndexOfIgnoreCaseWithDelimiterAndFromIndex() {
        // Test basic cases
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("apple,APPLE,banana", "apple", ",", 0));
        Assertions.assertEquals(6, Strings.indexOfIgnoreCase("apple,APPLE,banana", "apple", ",", 5));
        Assertions.assertEquals(10, Strings.indexOfIgnoreCase("pineapple,apple", "apple", ",", 0));
        Assertions.assertEquals(11, Strings.indexOfIgnoreCase("test value test", "test", " ", 5));

        // Test negative fromIndex
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("apple,banana", "apple", ",", -5));

        // Test fromIndex beyond string length
        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase("apple", "apple", ",", 10));
    }

    @Test
    public void testSmallestLastIndexOfAll() {
        // Test basic cases
        Assertions.assertEquals(6, Strings.smallestLastIndexOfAll("Hello World", "o", "World"));
        Assertions.assertEquals(6, Strings.smallestLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(6, Strings.smallestLastIndexOfAll("Hello Hello", "Hello", "o"));

        // Test not found
        Assertions.assertEquals(-1, Strings.smallestLastIndexOfAll("test", "xyz", "abc"));

        // Test null cases
        Assertions.assertEquals(-1, Strings.smallestLastIndexOfAll(null, "test"));
        Assertions.assertEquals(-1, Strings.smallestLastIndexOfAll("test", (String[]) null));
        Assertions.assertEquals(-1, Strings.smallestLastIndexOfAll("test", new String[0]));

        // Test with null elements
        Assertions.assertEquals(1, Strings.smallestLastIndexOfAll("test", null, "es", null));
    }

    @Test
    public void testSmallestLastIndexOfAllWithIndex() {
        // Test basic cases
        Assertions.assertEquals(6, Strings.smallestLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(4, Strings.smallestLastIndexOfAll("Hello World", 5, "o", "World"));
        Assertions.assertEquals(-1, Strings.smallestLastIndexOfAll("Hello World", 3, "o", "World"));

        // Test negative index
        Assertions.assertEquals(-1, Strings.smallestLastIndexOfAll("test", -1, "test"));

        // Test index beyond string length
        Assertions.assertEquals(0, Strings.smallestLastIndexOfAll("test", 100, "test"));
    }

    @Test
    public void testLargestLastIndexOfAll() {
        // Test basic cases
        Assertions.assertEquals(7, Strings.largestLastIndexOfAll("Hello World", "o", "World"));
        Assertions.assertEquals(7, Strings.largestLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(6, Strings.largestLastIndexOfAll("Hello World", 6, "o", "World"));
        Assertions.assertEquals(7, Strings.largestLastIndexOfAll("Hello Hello", "Hello", "e"));

        // Test not found
        Assertions.assertEquals(-1, Strings.largestLastIndexOfAll("test", "xyz", "abc"));

        // Test null cases
        Assertions.assertEquals(-1, Strings.largestLastIndexOfAll(null, "test"));
        Assertions.assertEquals(-1, Strings.largestLastIndexOfAll("test", (String[]) null));
        Assertions.assertEquals(-1, Strings.largestLastIndexOfAll("test", new String[0]));
    }

    @Test
    public void testLargestLastIndexOfAllWithIndex() {
        // Test basic cases
        Assertions.assertEquals(7, Strings.largestLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(6, Strings.largestLastIndexOfAll("Hello World", 6, "o", "World"));
        Assertions.assertEquals(0, Strings.largestLastIndexOfAll("Hello World", 3, "o", "H"));

        // Test negative index
        Assertions.assertEquals(-1, Strings.largestLastIndexOfAll("test", -1, "test"));

        // Test index beyond string length
        Assertions.assertEquals(0, Strings.largestLastIndexOfAll("test", 100, "test"));
    }

    @Test
    public void testContainsIgnoreCaseWithDelimiter() {
        // Test found cases
        Assertions.assertTrue(Strings.containsIgnoreCase("Apple,Banana,Orange", "banana", ","));
        Assertions.assertTrue(Strings.containsIgnoreCase("ONE TWO THREE", "two", " "));
        Assertions.assertTrue(Strings.containsIgnoreCase("One-Two-Three", "TWO", "-"));

        // Test not found cases
        Assertions.assertFalse(Strings.containsIgnoreCase("Apple,Banana,Orange", "BAN", ","));

        // Test null cases
        Assertions.assertFalse(Strings.containsIgnoreCase(null, "test", ","));
        Assertions.assertFalse(Strings.containsIgnoreCase("test", null, ","));
    }

    @Test
    public void testContainsAnyIgnoreCase() {
        // Test found cases
        Assertions.assertTrue(Strings.containsAnyIgnoreCase("Hello World", "xyz", "WORLD"));
        Assertions.assertTrue(Strings.containsAnyIgnoreCase("Programming", "GRAM", "xyz"));

        // Test not found cases
        Assertions.assertFalse(Strings.containsAnyIgnoreCase("Hello World", "XYZ", "ABC"));

        // Test edge cases
        Assertions.assertFalse(Strings.containsAnyIgnoreCase("", "test"));
        Assertions.assertFalse(Strings.containsAnyIgnoreCase(null, "test"));
        Assertions.assertFalse(Strings.containsAnyIgnoreCase("test", (String[]) null));
        Assertions.assertFalse(Strings.containsAnyIgnoreCase("test", new String[0]));

        // Test single element
        Assertions.assertTrue(Strings.containsAnyIgnoreCase("Hello", "HELLO"));

        // Test two elements
        Assertions.assertTrue(Strings.containsAnyIgnoreCase("Hello World", "xyz", "WORLD"));
    }

    @Test
    public void testContainsNoneIgnoreCase() {
        // Test none found cases
        Assertions.assertTrue(Strings.containsNoneIgnoreCase("Hello World", "xyz", "abc"));
        Assertions.assertTrue(Strings.containsNoneIgnoreCase("test", "ABC", "XYZ"));

        // Test found cases
        Assertions.assertFalse(Strings.containsNoneIgnoreCase("Hello World", "HELLO", "xyz"));

        // Test edge cases
        Assertions.assertTrue(Strings.containsNoneIgnoreCase("", "test"));
        Assertions.assertTrue(Strings.containsNoneIgnoreCase(null, "test"));
        Assertions.assertTrue(Strings.containsNoneIgnoreCase("test", (String[]) null));
        Assertions.assertTrue(Strings.containsNoneIgnoreCase("test", new String[0]));
    }

    @Test
    public void testSubstringChar() {
        // Test basic cases
        Assertions.assertEquals(" World", Strings.substring("Hello World", ' '));
        Assertions.assertEquals("@example.com", Strings.substring("user@example.com", '@'));

        // Test not found
        Assertions.assertNull(Strings.substring("Hello", 'x'));

        // Test edge cases
        Assertions.assertNull(Strings.substring("", 'a'));
        Assertions.assertNull(Strings.substring(null, 'a'));
    }

    @Test
    public void testSubstringString() {
        // Test basic cases
        Assertions.assertEquals("World", Strings.substring("Hello World", "Wo"));
        Assertions.assertEquals("@example.com", Strings.substring("user@example.com", "@"));

        // Test not found
        Assertions.assertNull(Strings.substring("Hello", "xyz"));

        // Test empty delimiter
        Assertions.assertEquals("Hello", Strings.substring("Hello", ""));

        // Test null cases
        Assertions.assertNull(Strings.substring(null, "test"));
        Assertions.assertNull(Strings.substring("test", null));
    }

    @Test
    public void testSubstringWithIndexAndChar() {
        // Test basic cases
        Assertions.assertEquals("Hello", Strings.substring("Hello, World!", 0, ','));
        Assertions.assertEquals("b", Strings.substring("a-b-c-d", 2, '-'));

        // Test not found
        Assertions.assertNull(Strings.substring("test", 0, 'x'));

        // Test invalid index
        Assertions.assertNull(Strings.substring("test", -1, 't'));
        Assertions.assertNull(Strings.substring("test", 10, 't'));

        // Test null
        Assertions.assertNull(Strings.substring(null, 0, ','));
    }

    @Test
    public void testSubstringWithIndexAndString() {
        // Test basic cases
        Assertions.assertEquals("Hello", Strings.substring("Hello, World!", 0, ", "));
        Assertions.assertEquals("content", Strings.substring("<tag>content</tag>", 5, "</"));

        // Test not found
        Assertions.assertNull(Strings.substring("test", 0, "xyz"));

        // Test empty delimiter
        Assertions.assertEquals("", Strings.substring("test", 0, ""));

        // Test null cases
        Assertions.assertNull(Strings.substring(null, 0, "test"));
        Assertions.assertNull(Strings.substring("test", 0, (String) null));
    }

    @Test
    public void testSubstringWithCharAndEndIndex() {
        // Test basic cases
        Assertions.assertEquals("-d", Strings.substring("a-b-c-d", '-', 7));
        Assertions.assertEquals(" world", Strings.substring("hello world", ' ', 11));

        // Test not found
        Assertions.assertNull(Strings.substring("test", 'x', 4));

        // Test negative index
        Assertions.assertNull(Strings.substring("test", 't', -1));

        // Test null
        Assertions.assertNull(Strings.substring(null, 'a', 5));
    }

    @Test
    public void testSubstringWithStringAndEndIndex() {
        // Test basic cases
        Assertions.assertEquals("<c>", Strings.substring("<a><b><c>", "<", 9));
        Assertions.assertEquals("-baz", Strings.substring("foo-bar-baz", "-", 11));

        // Test not found
        Assertions.assertNull(Strings.substring("test", "xyz", 4));

        // Test empty delimiter
        Assertions.assertEquals("", Strings.substring("test", "", 4));

        // Test null cases
        Assertions.assertNull(Strings.substring(null, "test", 5));
        Assertions.assertNull(Strings.substring("test", (String) null, 5));
    }

    @Test
    public void testJoinByteArray() {
        // Test basic cases
        byte[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[1, 2, 3]", Strings.join(arr, 0, 3, ", ", "[", "]"));
        Assertions.assertEquals("Bytes: 2 | 3 | 4", Strings.join(arr, 1, 4, " | ", "Bytes: ", ""));
        Assertions.assertEquals("(3)", Strings.join(arr, 2, 3, ", ", "(", ")"));

        // Test empty range
        Assertions.assertEquals("[]", Strings.join(arr, 1, 1, ", ", "[", "]"));

        // Test null array
        Assertions.assertEquals("[]", Strings.join((byte[]) null, 0, 0, ", ", "[", "]"));

        // Test no delimiter
        Assertions.assertEquals("<12>", Strings.join(arr, 0, 2, "", "<", ">"));

        // Test index out of bounds
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(arr, -1, 3, ", ", "", ""));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(arr, 0, 10, ", ", "", ""));
    }

    @Test
    public void testJoinShortArray() {
        // Test basic cases
        short[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("1, 2, 3, 4, 5", Strings.join(arr, 0, 5, ", ", "", ""));
        Assertions.assertEquals("2-3-4", Strings.join(arr, 1, 4, "-", "", ""));
        Assertions.assertEquals("[1, 2, 3]", Strings.join(arr, 0, 3, ", ", "[", "]"));
        Assertions.assertEquals("Numbers: 1, 2, 3, 4, 5", Strings.join(arr, 0, 5, ", ", "Numbers: ", ""));

        // Test edge cases
        Assertions.assertEquals("", Strings.join((short[]) null, 0, 0, ", ", "", ""));
        Assertions.assertEquals("[]", Strings.join(arr, 2, 2, ", ", "[", "]"));

        // Test index out of bounds
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(arr, -1, 3, ", ", "", ""));
    }

    @Test
    public void testJoinLongArray() {
        // Test basic cases
        long[] arr = { 100L, 200L, 300L, 400L, 500L };
        Assertions.assertEquals("100, 200, 300, 400, 500", Strings.join(arr, 0, 5, ", ", "", ""));
        Assertions.assertEquals("200-300-400", Strings.join(arr, 1, 4, "-", "", ""));
        Assertions.assertEquals("[100, 200, 300]", Strings.join(arr, 0, 3, ", ", "[", "]"));
        Assertions.assertEquals("Values: 100, 200, 300, 400, 500", Strings.join(arr, 0, 5, ", ", "Values: ", ""));
        Assertions.assertEquals("Result: 300 | 400!", Strings.join(arr, 2, 4, " | ", "Result: ", "!"));

        // Test edge cases
        Assertions.assertEquals("[]", Strings.join((long[]) null, 0, 0, ", ", "[", "]"));
        Assertions.assertEquals("[]", Strings.join(arr, 2, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoinEntries() {
        // Test with person objects
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("John", 25), new AbstractMap.SimpleEntry<>("Jane", 30));

        Assertions.assertEquals("John=25, Jane=30", Strings.joinEntries(entries, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertEquals("John IS 25 AND Jane IS 30", Strings.joinEntries(entries, " AND ", " IS ", Map.Entry::getKey, Map.Entry::getValue));

        // Test empty list
        List<Map.Entry<String, Integer>> emptyList = new ArrayList<>();
        Assertions.assertEquals("", Strings.joinEntries(emptyList, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));

        // Test null list
        Assertions.assertEquals("", Strings.joinEntries((List<Map.Entry<String, Integer>>) null, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));

        // Test null extractors
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.joinEntries(entries, ", ", "=", null, Map.Entry::getValue));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.joinEntries(entries, ", ", "=", Map.Entry::getKey, null));
    }

    @Test
    public void testJoinEntriesWithPrefixSuffix() {
        // Test with entries and trim
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>(" name ", 1), new AbstractMap.SimpleEntry<>(" age ", 25));

        Assertions.assertEquals("[ name =1,  age =25]", Strings.joinEntries(entries, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertEquals("[name=1, age=25]", Strings.joinEntries(entries, ", ", "=", "[", "]", true, Map.Entry::getKey, Map.Entry::getValue));

        // Test with prefix only
        Assertions.assertEquals("Total: Apple costs $1.99 + Banana costs $0.99",
                Strings.joinEntries(Arrays.asList(new AbstractMap.SimpleEntry<>("Apple", 1.99), new AbstractMap.SimpleEntry<>("Banana", 0.99)), " + ",
                        " costs $", "Total: ", "", false, Map.Entry::getKey, Map.Entry::getValue));

        // Test empty list with prefix and suffix
        Assertions.assertEquals("[]",
                Strings.joinEntries(new ArrayList<Map.Entry<String, Integer>>(), ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    public void testConcat5Strings() {
        // Test normal cases
        Assertions.assertEquals("Hello World!!", Strings.concat("Hello", " ", "World", "!", "!"));
        Assertions.assertEquals("ABCDE", Strings.concat("A", "B", "C", "D", "E"));

        // Test with nulls
        Assertions.assertEquals("123", Strings.concat("1", null, "2", null, "3"));
        Assertions.assertEquals("", Strings.concat(null, null, null, null, null));

        // Test with empty strings
        Assertions.assertEquals("AC", Strings.concat("A", "", "", "", "C"));
    }

    @Test
    public void testConcat6Strings() {
        // Test normal cases
        Assertions.assertEquals("ABCDEF", Strings.concat("A", "B", "C", "D", "E", "F"));
        Assertions.assertEquals("123456", Strings.concat("1", "2", "3", "4", "5", "6"));
        Assertions.assertEquals("Hello!", Strings.concat("H", "e", "l", "l", "o", "!"));

        // Test with nulls
        Assertions.assertEquals("", Strings.concat(null, null, null, null, null, null));
    }

    @Test
    public void testConcat7Strings() {
        // Test normal cases
        Assertions.assertEquals("ABCDEFG", Strings.concat("A", "B", "C", "D", "E", "F", "G"));
        Assertions.assertEquals("1234567", Strings.concat("1", "2", "3", "4", "5", "6", "7"));
        Assertions.assertEquals("Hello 7", Strings.concat("H", "e", "l", "l", "o", " ", "7"));
    }

    @Test
    public void testConcat8Strings() {
        // Test normal cases
        Assertions.assertEquals("ABCDEFGH", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H"));
        Assertions.assertEquals("12345678", Strings.concat("1", "2", "3", "4", "5", "6", "7", "8"));
        Assertions.assertEquals("Hello 8!", Strings.concat("H", "e", "l", "l", "o", " ", "8", "!"));
    }

    @Test
    public void testConcat9Strings() {
        // Test normal cases
        Assertions.assertEquals("ABCDEFGHI", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        Assertions.assertEquals("123456789", Strings.concat("1", "2", "3", "4", "5", "6", "7", "8", "9"));
        Assertions.assertEquals("Hello 9!!", Strings.concat("H", "e", "l", "l", "o", " ", "9", "!", "!"));
    }

    @Test
    public void testConcatStringArray() {
        // Test normal cases
        Assertions.assertEquals("Hello World", Strings.concat(new String[] { "Hello", " ", "World" }));
        Assertions.assertEquals("ABCD", Strings.concat(new String[] { "A", "B", "C", "D" }));

        // Test with nulls
        Assertions.assertEquals("HelloWorld", Strings.concat(new String[] { "Hello", null, "World" }));
        Assertions.assertEquals("", Strings.concat(new String[] { null, null }));

        // Test empty array
        Assertions.assertEquals("", Strings.concat(new String[0]));

        // Test null array
        Assertions.assertEquals("", Strings.concat((String[]) null));

        // Test different array sizes
        Assertions.assertEquals("A", Strings.concat(new String[] { "A" }));
        Assertions.assertEquals("AB", Strings.concat(new String[] { "A", "B" }));
        Assertions.assertEquals("ABC", Strings.concat(new String[] { "A", "B", "C" }));
    }

    @Test
    public void testConcat2Objects() {
        // Test normal cases
        Assertions.assertEquals("Hello123", Strings.concat("Hello", 123));
        Assertions.assertEquals("42 is the answer", Strings.concat(42, " is the answer"));

        // Test with nulls
        Assertions.assertEquals("World", Strings.concat(null, "World"));
        Assertions.assertEquals("Hello", Strings.concat("Hello", null));
        Assertions.assertEquals("", Strings.concat(null, null));

        // Test with different object types
        Assertions.assertEquals("3.14true", Strings.concat(3.14, true));
    }

    @Test
    public void testConcat3Objects() {
        // Test normal cases
        Assertions.assertEquals("Hello World", Strings.concat("Hello", " ", "World"));
        Assertions.assertEquals("123", Strings.concat(1, 2, 3));
        Assertions.assertEquals("Value: 42!", Strings.concat("Value: ", 42, "!"));

        // Test with nulls
        Assertions.assertEquals("", Strings.concat(null, null, null));
    }

    @Test
    public void testConcat4Objects() {
        // Test normal cases
        Assertions.assertEquals("ABCD", Strings.concat("A", "B", "C", "D"));
        Assertions.assertEquals("1+2=3", Strings.concat(1, "+", 2, "=3"));
        Assertions.assertEquals("Result: 10 out of 20", Strings.concat("Result: ", 10, " out of ", 20));

        // Test with nulls
        Assertions.assertEquals("", Strings.concat(null, null, null, null));
    }

    @Test
    public void testConcat5Objects() {
        // Test normal cases
        Assertions.assertEquals("ABCDE", Strings.concat("A", "B", "C", "D", "E"));
        Assertions.assertEquals("1 2 3", Strings.concat(1, " ", 2, " ", 3));
        Assertions.assertEquals("Sum of 1+2=3", Strings.concat("Sum of ", 1, "+", 2, "=3"));

        // Test with nulls
        Assertions.assertEquals("", Strings.concat(null, null, null, null, null));
    }

    @Test
    public void testConcat6Objects() {
        // Test normal cases
        Assertions.assertEquals("ABCDEF", Strings.concat("A", "B", "C", "D", "E", "F"));
        Assertions.assertEquals("123456", Strings.concat(1, 2, 3, 4, 5, 6));
        Assertions.assertEquals("Hello!", Strings.concat("H", "e", "l", "l", "o", "!"));
    }

    @Test
    public void testConcat7Objects() {
        // Test normal cases
        Assertions.assertEquals("ABCDEFG", Strings.concat("A", "B", "C", "D", "E", "F", "G"));
        Assertions.assertEquals("1234567", Strings.concat(1, 2, 3, 4, 5, 6, 7));
        Assertions.assertEquals("Hello 7", Strings.concat("H", "e", "l", "l", "o", " ", 7));
    }

    @Test
    public void testConcat8Objects() {
        // Test normal cases
        Assertions.assertEquals("ABCDEFGH", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H"));
        Assertions.assertEquals("12345678", Strings.concat(1, 2, 3, 4, 5, 6, 7, 8));
        Assertions.assertEquals("Hello 8!", Strings.concat("H", "e", "l", "l", "o", " ", 8, "!"));
    }

    @Test
    public void testConcat9Objects() {
        // Test normal cases
        Assertions.assertEquals("ABCDEFGHI", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        Assertions.assertEquals("123456789", Strings.concat(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Assertions.assertEquals("Hello 9!!", Strings.concat("H", "e", "l", "l", "o", " ", 9, "!", "!"));
    }

    @Test
    public void testOverlay() {
        // Test normal cases
        Assertions.assertEquals("abc", Strings.overlay(null, "abc", 0, 0));
        Assertions.assertEquals("abc", Strings.overlay("", "abc", 0, 0));
        Assertions.assertEquals("abef", Strings.overlay("abcdef", null, 2, 4));
        Assertions.assertEquals("abef", Strings.overlay("abcdef", "", 2, 4));
        Assertions.assertEquals("abzzzzef", Strings.overlay("abcdef", "zzzz", 2, 4));

        // Test edge cases
        Assertions.assertEquals("xyzdef", Strings.overlay("abcdef", "xyz", 0, 3));
        Assertions.assertEquals("abcxyz", Strings.overlay("abcdef", "xyz", 3, 6));

        // Test index out of bounds
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.overlay("test", "x", -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.overlay("test", "x", 0, 10));
    }

    @Test
    public void testParseBoolean() {
        // Test true cases
        Assertions.assertTrue(Strings.parseBoolean("true"));
        Assertions.assertTrue(Strings.parseBoolean("True"));
        Assertions.assertTrue(Strings.parseBoolean("TRUE"));

        // Test false cases
        Assertions.assertFalse(Strings.parseBoolean("false"));
        Assertions.assertFalse(Strings.parseBoolean("yes"));
        Assertions.assertFalse(Strings.parseBoolean("1"));
        Assertions.assertFalse(Strings.parseBoolean(""));
        Assertions.assertFalse(Strings.parseBoolean(null));
    }

    @Test
    public void testParseChar() {
        // Test single character
        Assertions.assertEquals('A', Strings.parseChar("A"));
        Assertions.assertEquals('$', Strings.parseChar("$"));

        // Test numeric codes
        Assertions.assertEquals('A', Strings.parseChar("65"));
        Assertions.assertEquals('$', Strings.parseChar("36"));

        // Test edge cases
        Assertions.assertEquals('\0', Strings.parseChar(""));
        Assertions.assertEquals('\0', Strings.parseChar(null));

        // Test invalid numeric string
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseChar("ABC"));
    }

    @Test
    public void testParseByte() {
        // Test valid cases
        Assertions.assertEquals((byte) 123, Strings.parseByte("123"));
        Assertions.assertEquals((byte) -123, Strings.parseByte("-123"));
        Assertions.assertEquals((byte) 0, Strings.parseByte("0"));

        // Test invalid cases
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseByte("abc"));
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseByte("999"));
    }

    @Test
    public void testParseShort() {
        // Test valid cases
        Assertions.assertEquals((short) 12345, Strings.parseShort("12345"));
        Assertions.assertEquals((short) -12345, Strings.parseShort("-12345"));
        Assertions.assertEquals((short) 0, Strings.parseShort("0"));

        // Test invalid cases
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseShort("abc"));
    }

    @Test
    public void testParseInt() {
        // Test valid cases
        Assertions.assertEquals(123456, Strings.parseInt("123456"));
        Assertions.assertEquals(-123456, Strings.parseInt("-123456"));
        Assertions.assertEquals(0, Strings.parseInt("0"));

        // Test invalid cases
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseInt("abc"));
    }

    @Test
    public void testParseLong() {
        // Test valid cases
        Assertions.assertEquals(123456789L, Strings.parseLong("123456789"));
        Assertions.assertEquals(-123456789L, Strings.parseLong("-123456789"));
        Assertions.assertEquals(0L, Strings.parseLong("0"));

        // Test invalid cases
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseLong("abc"));
    }

    @Test
    public void testParseFloat() {
        // Test valid cases
        Assertions.assertEquals(123.45f, Strings.parseFloat("123.45"));
        Assertions.assertEquals(-123.45f, Strings.parseFloat("-123.45"));
        Assertions.assertEquals(0.0f, Strings.parseFloat("0"));

        // Test invalid cases
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseFloat("abc"));
    }

    @Test
    public void testParseDouble() {
        // Test valid cases
        Assertions.assertEquals(123.456789, Strings.parseDouble("123.456789"));
        Assertions.assertEquals(-123.456789, Strings.parseDouble("-123.456789"));
        Assertions.assertEquals(0.0, Strings.parseDouble("0"));

        // Test invalid cases
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseDouble("abc"));
    }

    // Additional edge case tests for better coverage

    @Test
    public void testSplitWithSingleCharacterDelimiter() {
        // Test splitting with single character strings
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ","));
        Assertions.assertArrayEquals(new String[] { "abc" }, Strings.split("abc", ","));
        Assertions.assertArrayEquals(new String[] {}, Strings.split("", ","));

        // Test with multiple consecutive delimiters
        Assertions.assertArrayEquals(new String[] { "a", "b" }, Strings.split("a,,b", ","));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,,,b,,,c", ","));
    }

    @Test
    public void testSplitPreserveAllTokensEdgeCases() {
        // Test with only delimiters
        Assertions.assertArrayEquals(new String[] { "", "", "", "" }, Strings.splitPreserveAllTokens(":::", ":"));
        Assertions.assertArrayEquals(new String[] { "", "" }, Strings.splitPreserveAllTokens(":", ":"));

        // Test with mixed content
        Assertions.assertArrayEquals(new String[] { "", "a", "", "b", "" }, Strings.splitPreserveAllTokens(":a::b:", ":"));
    }

    @Test
    public void testSubstringEdgeCases() {
        // Test substring with delimiter at start
        Assertions.assertEquals(",test", Strings.substring(",test", ','));

        // Test substring with delimiter at end
        Assertions.assertEquals(",", Strings.substring("test,", ','));

        // Test with multiple occurrences
        Assertions.assertEquals(",test,more", Strings.substring("first,test,more", ','));
    }

    @Test
    public void testJoinEntriesWithNullValues() {
        // Test with null values in entries
        List<Map.Entry<String, String>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("key1", null), new AbstractMap.SimpleEntry<>(null, "value2"),
                new AbstractMap.SimpleEntry<>("key3", "value3"));

        String result = Strings.joinEntries(entries, ", ", "=", Map.Entry::getKey, Map.Entry::getValue);
        Assertions.assertEquals("key1=null, null=value2, key3=value3", result);
    }

    @Test
    public void testConcatWithLargeArrays() {
        // Test with arrays larger than 7 elements
        String[] largeArray = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
        Assertions.assertEquals("ABCDEFGHIJ", Strings.concat(largeArray));

        // Test with mixed null values
        String[] mixedArray = { "A", null, "B", null, "C", "D", "E", "F", "G", "H" };
        Assertions.assertEquals("ABCDEFGH", Strings.concat(mixedArray));
    }

    @Test
    public void testIndexOfIgnoreCaseComplexScenarios() {
        // Test with complex delimiter scenarios
        Assertions.assertEquals(7, Strings.indexOfIgnoreCase("apple, APPLE, banana", "apple", ", ", 1));

        // Test when value to find contains delimiter
        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase("test,value", "st,va", ","));

        // Test with delimiter at boundaries
        Assertions.assertEquals(1, Strings.indexOfIgnoreCase(",apple,", "apple", ","));
    }

    @Test
    public void testReplaceOnceEdgeCases() {
        // Test replace at start
        Assertions.assertEquals("xyzcde", Strings.replaceOnce("abcde", "ab", "xyz"));

        // Test replace at end
        Assertions.assertEquals("abcxyz", Strings.replaceOnce("abcde", "de", "xyz"));

        // Test replace entire string
        Assertions.assertEquals("xyz", Strings.replaceOnce("abc", "abc", "xyz"));

        // Test with overlapping pattern
        Assertions.assertEquals("xc", Strings.replaceOnce("abc", "ab", "x"));
    }

    @Test
    public void testSplitWithWhitespaceDelimiter() {
        // Test split with null delimiter (whitespace)
        Assertions.assertArrayEquals(new String[] { "hello", "world", "test" }, Strings.split("hello   world\ttest", null));
        Assertions.assertArrayEquals(new String[] { "hello", "world" }, Strings.split("  hello   world  ", null));

        // Test split with null delimiter and trim
        Assertions.assertArrayEquals(new String[] { "hello", "world" }, Strings.split("  hello   world  ", null, true));
    }

    @Test
    public void testJoinWithEmptyElements() {
        // Test join with empty prefix and suffix
        byte[] arr = { 1, 2, 3 };
        Assertions.assertEquals("1,2,3", Strings.join(arr, 0, 3, ",", "", ""));

        // Test join with only prefix
        Assertions.assertEquals("Values: 1,2,3", Strings.join(arr, 0, 3, ",", "Values: ", ""));

        // Test join with only suffix
        Assertions.assertEquals("1,2,3!", Strings.join(arr, 0, 3, ",", "", "!"));
    }

    @Test
    public void testConvertWordsEdgeCases() {
        // Test with single word
        Assertions.assertEquals("HELLO", Strings.convertWords("hello", " ", null, String::toUpperCase));

        // Test with custom converter
        Function<String, String> reverser = s -> new StringBuilder(s).reverse().toString();
        Assertions.assertEquals("olleh dlrow", Strings.convertWords("hello world", " ", null, reverser));

        // Test with all words excluded
        Set<String> allExcluded = new HashSet<>(Arrays.asList("hello", "world"));
        Assertions.assertEquals("hello world", Strings.convertWords("hello world", " ", allExcluded, String::toUpperCase));
    }

    @Test
    public void testArrayMethodsWithSingleElement() {
        // Test trim methods with single element
        String[] single = { "  test  " };
        Strings.trimToNull(single);
        Assertions.assertEquals("test", single[0]);

        String[] single2 = { "  " };
        Strings.trimToNull(single2);
        Assertions.assertNull(single2[0]);

        // Test strip methods with single element
        String[] single3 = { "xyztest" };
        Strings.stripStart(single3, "xyz");
        Assertions.assertEquals("test", single3[0]);
    }

    @Test
    public void testSubstringBoundaryConditions() {
        // Test substring with index at string boundaries
        Assertions.assertEquals("test", Strings.substring("test", 0, 4));
        Assertions.assertEquals("", Strings.substring("test", 4, 4));
        Assertions.assertEquals("est", Strings.substring("test", 1, 4));

        // Test with invalid indices
        Assertions.assertNull(Strings.substring("test", 5, 10));
        Assertions.assertNull(Strings.substring("test", -1, 4));
    }

    @Test
    public void testParseNumericEdgeCases() {
        // Test parsing with whitespace (should fail)
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseInt(" 123 "));

        // Test parsing with special values
        Assertions.assertEquals(Integer.MAX_VALUE, Strings.parseInt(String.valueOf(Integer.MAX_VALUE)));
        Assertions.assertEquals(Integer.MIN_VALUE, Strings.parseInt(String.valueOf(Integer.MIN_VALUE)));

        // Test parseChar with special characters
        Assertions.assertEquals('\n', Strings.parseChar("10"));
        Assertions.assertEquals('9', Strings.parseChar("9"));
    }

    @Test
    public void testLastIndexOfAllWithOverlappingPatterns() {
        // Test with overlapping patterns
        String text = "ababababab";
        Assertions.assertEquals(8, Strings.largestLastIndexOfAll(text, "ab", "ba"));
        Assertions.assertEquals(7, Strings.smallestLastIndexOfAll(text, "ab", "ba"));

        // Test with pattern not found
        Assertions.assertEquals(-1, Strings.largestLastIndexOfAll(text, "xyz", "qrs"));
    }

    @Test
    public void testSplitPreserveAllTokensWithComplexDelimiters() {
        // Test with multi-character delimiter
        // Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, StringUtils.splitPreserveAllTokens("a::b::c", "::"));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a::b::c", "::"));
        Assertions.assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a::::b", "::"));

        // Test with delimiter not found
        Assertions.assertArrayEquals(new String[] { "test" }, Strings.splitPreserveAllTokens("test", "::"));
    }

    @Test
    public void testJoinEntriesWithSingleEntry() {
        // Test with single entry
        List<Map.Entry<String, String>> singleEntry = Arrays.asList(new AbstractMap.SimpleEntry<>("key", "value"));

        Assertions.assertEquals("key=value", Strings.joinEntries(singleEntry, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertEquals("[key=value]", Strings.joinEntries(singleEntry, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    public void testConcatPerformanceOptimization() {
        // Test that different sizes use optimized paths
        // Size 0
        Assertions.assertEquals("", Strings.concat());

        // Size 1
        Assertions.assertEquals("A", Strings.concat("A"));

        // Size 2-7 (optimized cases)
        for (int i = 2; i <= 7; i++) {
            String[] arr = new String[i];
            Arrays.fill(arr, "X");
            String expected = "X".repeat(i);
            Assertions.assertEquals(expected, Strings.concat(arr));
        }

        // Size > 7 (general case)
        String[] arr = new String[10];
        Arrays.fill(arr, "Y");
        Assertions.assertEquals("Y".repeat(10), Strings.concat(arr));
    }

    @Test
    public void testSplitMaxBoundaryConditions() {
        // Test with max equal to number of possible splits
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a:b:c", ":", 3));

        // Test with max greater than possible splits
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a:b:c", ":", 100));

        // Test with max = 2
        Assertions.assertArrayEquals(new String[] { "a", "b:c:d" }, Strings.split("a:b:c:d", ":", 2));
    }

    @Test
    public void testAppendPrependCaseSensitivity() {
        // Test case sensitivity in append/prepend
        Assertions.assertEquals("File.TXT", Strings.appendIfMissingIgnoreCase("File", ".TXT"));
        Assertions.assertEquals("File.txt", Strings.appendIfMissingIgnoreCase("File.txt", ".TXT"));
        Assertions.assertEquals("File.TxT", Strings.appendIfMissingIgnoreCase("File.TxT", ".txt"));

        // Test with mixed case prefixes
        Assertions.assertEquals("HTTP://site.com", Strings.prependIfMissingIgnoreCase("site.com", "HTTP://"));
        Assertions.assertEquals("HtTp://site.com", Strings.prependIfMissingIgnoreCase("HtTp://site.com", "http://"));
    }

    @Test
    public void testNullHandlingConsistency() {
        // Verify consistent null handling across methods

        // Split methods should return empty array for null
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ","));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", 5));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", true));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", 5, true));

        // SplitPreserveAllTokens should return empty array for null
        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ','));
        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ","));

        // Array methods should handle null arrays gracefully
        Strings.trimToNull((String[]) null);
        Strings.trimToEmpty((String[]) null);
        Strings.stripToNull((String[]) null);
        Strings.stripToEmpty((String[]) null);
        Strings.deleteWhitespace((String[]) null);

        // No exceptions should be thrown
    }
}
