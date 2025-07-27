package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class RegExUtil100Test extends TestBase {

    @Test
    public void testFindWithString() {
        // Test basic find
        Assertions.assertTrue(RegExUtil.find("Hello World", "World"));
        Assertions.assertFalse(RegExUtil.find("Hello World", "world")); // case sensitive
        Assertions.assertTrue(RegExUtil.find("Hello World", "\\w+"));
        Assertions.assertFalse(RegExUtil.find("Hello World", "\\d+"));

        // Test with email pattern
        Assertions.assertTrue(RegExUtil.find("Contact: john@example.com", "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}"));

        // Test empty/null cases
        Assertions.assertFalse(RegExUtil.find("", "test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", ""));

        // Test with special regex characters
        Assertions.assertTrue(RegExUtil.find("Price: $99.99", "\\$\\d+\\.\\d+"));
    }

    @Test
    public void testFindWithPattern() {
        Pattern emailPattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}");

        Assertions.assertTrue(RegExUtil.find("Contact: john@example.com", emailPattern));
        Assertions.assertFalse(RegExUtil.find("No email here", emailPattern));

        // Test with pre-compiled patterns from RegExUtil
        Assertions.assertTrue(RegExUtil.find("abc123def456", RegExUtil.INTEGER_FINDER));
        Assertions.assertTrue(RegExUtil.find("Price: $29.99", RegExUtil.NUMBER_FINDER));
    }

    @Test
    public void testFindInvalidInput() {
        // Test null/empty regex
        Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", (String) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", ""));

        // Test null pattern
        Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", (Pattern) null));
    }

    @Test
    public void testMatchesWithString() {
        // Test exact matches
        Assertions.assertTrue(RegExUtil.matches("123", "\\d+"));
        Assertions.assertFalse(RegExUtil.matches("abc123", "\\d+"));
        Assertions.assertTrue(RegExUtil.matches("hello@example.com", "[a-z]+@[a-z]+\\.[a-z]+"));

        // Test empty cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches("", ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches("test", ""));
    }

    @Test
    public void testMatchesWithPattern() {
        Pattern datePattern = RegExUtil.DATE_MATCHER;

        Assertions.assertTrue(RegExUtil.matches("2023-12-25", datePattern));
        Assertions.assertFalse(RegExUtil.matches("Not a date", datePattern));

        // Test with other patterns
        Assertions.assertTrue(RegExUtil.matches("123", RegExUtil.INTEGER_MATCHER));
        Assertions.assertTrue(RegExUtil.matches("-456", RegExUtil.NEGATIVE_INTEGER_MATCHER));
    }

    @Test
    public void testRemoveAll() {
        // Test basic removal
        Assertions.assertEquals("HelloWorld", RegExUtil.removeAll("Hello123World456", "\\d+"));
        Assertions.assertEquals("", RegExUtil.removeAll("12345", "\\d+"));
        Assertions.assertEquals("test", RegExUtil.removeAll("test", "\\d+"));

        // Test with whitespace
        Assertions.assertEquals("HelloWorld", RegExUtil.removeAll("Hello   World", "\\s+"));

        // Test null input
        Assertions.assertEquals("", RegExUtil.removeAll(null, "\\d+"));
    }

    @Test
    public void testRemoveAllWithPattern() {
        Assertions.assertEquals("HelloWorld", RegExUtil.removeAll("Hello   World", RegExUtil.WHITESPACE_FINDER));
        Assertions.assertEquals("  ", RegExUtil.removeAll("123 456 789", RegExUtil.INTEGER_FINDER));

        // Test null input
        Assertions.assertEquals("", RegExUtil.removeAll(null, RegExUtil.NUMBER_FINDER));
    }

    @Test
    public void testRemoveFirst() {
        // Test basic removal
        Assertions.assertEquals("HelloWorld456", RegExUtil.removeFirst("Hello123World456", "\\d+"));
        Assertions.assertEquals("Hello   World", RegExUtil.removeFirst("Hello   World   !", "\\s+!"));

        // Test when pattern not found
        Assertions.assertEquals("Hello", RegExUtil.removeFirst("Hello", "\\d+"));

        // Test null input
        Assertions.assertEquals("", RegExUtil.removeFirst(null, "\\d+"));
    }

    @Test
    public void testRemoveFirstWithPattern() {
        Assertions.assertEquals("HelloWorld   !", RegExUtil.removeFirst("Hello   World   !", RegExUtil.WHITESPACE_FINDER));
        Assertions.assertEquals("abcdef456", RegExUtil.removeFirst("abc123def456", RegExUtil.INTEGER_FINDER));

        // Test null input
        Assertions.assertEquals("", RegExUtil.removeFirst(null, RegExUtil.NUMBER_FINDER));
    }

    @Test
    public void testRemoveLast() {
        // Test basic removal
        Assertions.assertEquals("Hello123World", RegExUtil.removeLast("Hello123World456", "\\d+"));
        Assertions.assertEquals("Hello   World", RegExUtil.removeLast("Hello   World   !", "\\s+!"));

        // Test when pattern not found
        Assertions.assertEquals("Hello", RegExUtil.removeLast("Hello", "\\d+"));

        // Test null input
        Assertions.assertEquals("", RegExUtil.removeLast(null, "\\d+"));
    }

    @Test
    public void testRemoveLastWithPattern() {
        Assertions.assertEquals("Hello   World!", RegExUtil.removeLast("Hello   World   !", RegExUtil.WHITESPACE_FINDER));
        Assertions.assertEquals("abc123def", RegExUtil.removeLast("abc123def456", RegExUtil.INTEGER_FINDER));

        // Test null input
        Assertions.assertEquals("", RegExUtil.removeLast(null, RegExUtil.NUMBER_FINDER));
    }

    @Test
    public void testReplaceAll() {
        // Test basic replacement
        Assertions.assertEquals("Hello World", RegExUtil.replaceAll("Hello   World", "\\s+", " "));
        Assertions.assertEquals("X-X-X", RegExUtil.replaceAll("123-456-789", "\\d+", "X"));

        // Test with null replacement
        Assertions.assertEquals("HelloWorld", RegExUtil.replaceAll("Hello123World", "\\d+", (String) null));

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceAll(null, "\\d+", "X"));
    }

    @Test
    public void testReplaceAllWithFunction() {
        // Test with function replacer
        String result = RegExUtil.replaceAll("hello world", "\\b\\w", match -> match.toUpperCase());
        Assertions.assertEquals("Hello World", result);

        // Test with number doubling
        result = RegExUtil.replaceAll("1 2 3", "\\d", match -> String.valueOf(Integer.parseInt(match) * 2));
        Assertions.assertEquals("2 4 6", result);

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceAll(null, "\\d+", match -> "X"));
    }

    @Test
    public void testReplaceAllWithIntBiFunction() {
        // Test with index-based replacer
        String result = RegExUtil.replaceAll("abc123def", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
        Assertions.assertEquals("abc[3-6]def", result);

        // Test multiple matches
        result = RegExUtil.replaceAll("a1b2c3", "\\d", (start, end) -> "{" + start + "}");
        Assertions.assertEquals("a{1}b{3}c{5}", result);

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceAll(null, "\\d+", (start, end) -> "X"));
    }

    @Test
    public void testReplaceAllWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");

        Assertions.assertEquals("Hello World", RegExUtil.replaceAll("Hello   World", pattern, " "));
        Assertions.assertEquals("HelloWorld", RegExUtil.replaceAll("Hello   World", pattern, ""));

        // Test with pre-defined patterns
        Assertions.assertEquals("abc def ghi", RegExUtil.replaceAll("abc123def456ghi", RegExUtil.INTEGER_FINDER, " "));

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceAll(null, pattern, "X"));
    }

    @Test
    public void testReplaceFirst() {
        // Test basic replacement
        Assertions.assertEquals("HelloXXXWorld456", RegExUtil.replaceFirst("Hello123World456", "\\d+", "XXX"));
        Assertions.assertEquals("Hello_World", RegExUtil.replaceFirst("Hello   World", "\\s+", "_"));

        // Test when pattern not found
        Assertions.assertEquals("Hello", RegExUtil.replaceFirst("Hello", "\\d+", "X"));

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceFirst(null, "\\d+", "X"));
    }

    @Test
    public void testReplaceFirstWithFunction() {
        // Test with function replacer
        String result = RegExUtil.replaceFirst("hello world", "\\b\\w", match -> match.toUpperCase());
        Assertions.assertEquals("Hello world", result);

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceFirst(null, "\\d+", match -> "X"));
    }

    @Test
    public void testReplaceFirstWithIntBiFunction() {
        // Test with index-based replacer
        String result = RegExUtil.replaceFirst("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
        Assertions.assertEquals("abc[3-6]def456", result);

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceFirst(null, "\\d+", (start, end) -> "X"));
    }

    @Test
    public void testReplaceFirstWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");

        Assertions.assertEquals("HelloXXXWorld456", RegExUtil.replaceFirst("Hello123World456", pattern, "XXX"));

        // Test with pre-defined patterns
        Assertions.assertEquals("Hello World   !", RegExUtil.replaceFirst("Hello   World   !", RegExUtil.WHITESPACE_FINDER, " "));

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceFirst(null, pattern, "X"));
    }

    @Test
    public void testReplaceLast() {
        // Test basic replacement
        Assertions.assertEquals("Hello123WorldXXX", RegExUtil.replaceLast("Hello123World456", "\\d+", "XXX"));
        Assertions.assertEquals("Hello   World_!", RegExUtil.replaceLast("Hello   World   !", "\\s+", "_"));

        // Test when pattern not found
        Assertions.assertEquals("Hello", RegExUtil.replaceLast("Hello", "\\d+", "X"));

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceLast(null, "\\d+", "X"));
    }

    @Test
    public void testReplaceLastWithFunction() {
        // Test with function replacer
        String result = RegExUtil.replaceLast("hello world hello", "hello", match -> match.toUpperCase());
        Assertions.assertEquals("hello world HELLO", result);

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceLast(null, "\\d+", match -> "X"));
    }

    @Test
    public void testReplaceLastWithIntBiFunction() {
        // Test with index-based replacer
        String result = RegExUtil.replaceLast("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
        Assertions.assertEquals("abc123def[9-12]", result);

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceLast(null, "\\d+", (start, end) -> "X"));
    }

    @Test
    public void testReplaceLastWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");

        Assertions.assertEquals("Hello123WorldXXX", RegExUtil.replaceLast("Hello123World456", pattern, "XXX"));

        // Test with pre-defined patterns
        Assertions.assertEquals("Hello   World!", RegExUtil.replaceLast("Hello   World   !", RegExUtil.WHITESPACE_FINDER, ""));

        // Test null input
        Assertions.assertEquals("", RegExUtil.replaceLast(null, pattern, "X"));
    }

    @Test
    public void testCountMatches() {
        // Test basic counting
        Assertions.assertEquals(3, RegExUtil.countMatches("Hello World", "l"));
        Assertions.assertEquals(2, RegExUtil.countMatches("abc123def456", "\\d+"));
        Assertions.assertEquals(0, RegExUtil.countMatches("Hello", "\\d"));

        // Test empty/null cases
        Assertions.assertEquals(0, RegExUtil.countMatches("", "test"));
        Assertions.assertEquals(0, RegExUtil.countMatches(null, "test"));
    }

    @Test
    public void testCountMatchesWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");

        Assertions.assertEquals(3, RegExUtil.countMatches("abc123def456ghi789", pattern));
        Assertions.assertEquals(0, RegExUtil.countMatches("no numbers here", pattern));

        // Test with pre-defined patterns
        Assertions.assertEquals(3, RegExUtil.countMatches("Hello   World   How   Are", RegExUtil.WHITESPACE_FINDER));

        // Test empty/null cases
        Assertions.assertEquals(0, RegExUtil.countMatches("", pattern));
        Assertions.assertEquals(0, RegExUtil.countMatches(null, pattern));
    }

    @Test
    public void testMatchResults() {
        // Test basic match results
        Stream<MatchResult> matches = RegExUtil.matchResults("abc123def456", "\\d+");
        List<String> results = matches.map(MatchResult::group).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("123", "456"), results);

        // Test empty results
        matches = RegExUtil.matchResults("no numbers", "\\d+");
        Assertions.assertEquals(0, matches.count());

        // Test null/empty input
        matches = RegExUtil.matchResults(null, "\\d+");
        Assertions.assertEquals(0, matches.count());

        matches = RegExUtil.matchResults("", "\\d+");
        Assertions.assertEquals(0, matches.count());
    }

    @Test
    public void testMatchResultsWithPattern() {
        Pattern pattern = Pattern.compile("\\b\\w+@\\w+\\.\\w+\\b");
        Stream<MatchResult> matches = RegExUtil.matchResults("Contact: john@example.com, jane@test.org", pattern);
        List<String> emails = matches.map(MatchResult::group).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("john@example.com", "jane@test.org"), emails);

        // Test with pre-defined patterns
        matches = RegExUtil.matchResults("2023-12-25 and 2024-01-01", RegExUtil.DATE_FINDER);
        List<String> dates = matches.map(MatchResult::group).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("2023-12-25", "2024-01-01"), dates);
    }

    @Test
    public void testMatchIndices() {
        // Test basic indices
        IntStream indices = RegExUtil.matchIndices("Hello World", "l");
        int[] indicesArray = indices.toArray();
        Assertions.assertArrayEquals(new int[] { 2, 3, 9 }, indicesArray);

        // Test with regex pattern
        indices = RegExUtil.matchIndices("abc123def456ghi", "\\d+");
        indicesArray = indices.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 9 }, indicesArray);

        // Test empty results
        indices = RegExUtil.matchIndices("no matches", "\\d+");
        Assertions.assertEquals(0, indices.count());
    }

    @Test
    public void testMatchIndicesWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        IntStream indices = RegExUtil.matchIndices("abc123def456ghi", pattern);
        int[] indicesArray = indices.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 9 }, indicesArray);

        // Test with pre-defined patterns
        indices = RegExUtil.matchIndices("Hello   World   !", RegExUtil.WHITESPACE_FINDER);
        indicesArray = indices.toArray();
        Assertions.assertArrayEquals(new int[] { 5, 13 }, indicesArray);
    }

    @Test
    public void testSplit() {
        // Test basic split
        String[] parts = RegExUtil.split("one,two,three", ",");
        Assertions.assertArrayEquals(new String[] { "one", "two", "three" }, parts);

        // Test with whitespace
        parts = RegExUtil.split("Hello   World", "\\s+");
        Assertions.assertArrayEquals(new String[] { "Hello", "World" }, parts);

        // Test null input
        parts = RegExUtil.split(null, ",");
        Assertions.assertArrayEquals(new String[0], parts);

        // Test empty input
        parts = RegExUtil.split("", ",");
        Assertions.assertArrayEquals(new String[] { "" }, parts);
    }

    @Test
    public void testSplitWithLimit() {
        // Test with limit
        String[] parts = RegExUtil.split("one,two,three,four", ",", 3);
        Assertions.assertArrayEquals(new String[] { "one", "two", "three,four" }, parts);

        // Test with no limit
        parts = RegExUtil.split("a:b:c:d", ":", -1);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "d" }, parts);

        // Test null input
        parts = RegExUtil.split(null, ",", 2);
        Assertions.assertArrayEquals(new String[0], parts);
    }

    @Test
    public void testSplitWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        String[] words = RegExUtil.split("Hello   World   Java", pattern);
        Assertions.assertArrayEquals(new String[] { "Hello", "World", "Java" }, words);

        // Test with pre-defined patterns
        String[] parts = RegExUtil.split("abc123def456ghi", RegExUtil.INTEGER_FINDER);
        Assertions.assertArrayEquals(new String[] { "abc", "def", "ghi" }, parts);

        // Test null input
        parts = RegExUtil.split(null, pattern);
        Assertions.assertArrayEquals(new String[0], parts);
    }

    @Test
    public void testSplitWithPatternAndLimit() {
        Pattern pattern = Pattern.compile(",");
        String[] parts = RegExUtil.split("a,b,c,d", pattern, 3);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c,d" }, parts);

        // Test null input
        parts = RegExUtil.split(null, pattern, 2);
        Assertions.assertArrayEquals(new String[0], parts);
    }

    @Test
    public void testSplitToLines() {
        // Test various line separators
        String text = "Line 1\nLine 2\r\nLine 3\rLine 4";
        String[] lines = RegExUtil.splitToLines(text);
        Assertions.assertArrayEquals(new String[] { "Line 1", "Line 2", "Line 3", "Line 4" }, lines);

        // Test null input
        lines = RegExUtil.splitToLines(null);
        Assertions.assertArrayEquals(new String[0], lines);

        // Test empty input
        lines = RegExUtil.splitToLines("");
        Assertions.assertArrayEquals(new String[] { "" }, lines);
    }

    @Test
    public void testSplitToLinesWithLimit() {
        String text = "Line 1\nLine 2\nLine 3\nLine 4";
        String[] lines = RegExUtil.splitToLines(text, 3);
        Assertions.assertArrayEquals(new String[] { "Line 1", "Line 2", "Line 3\nLine 4" }, lines);

        // Test with no limit
        lines = RegExUtil.splitToLines(text, -1);
        Assertions.assertArrayEquals(new String[] { "Line 1", "Line 2", "Line 3", "Line 4" }, lines);
    }

    @Test
    public void testPredefinedPatterns() {
        // Test JAVA_IDENTIFIER_FINDER
        Assertions.assertTrue(RegExUtil.find("class MyClass", RegExUtil.JAVA_IDENTIFIER_FINDER));
        Assertions.assertTrue(RegExUtil.find("$var_123", RegExUtil.JAVA_IDENTIFIER_FINDER));

        // Test NUMBER patterns
        Assertions.assertTrue(RegExUtil.find("Price: $99.99", RegExUtil.NUMBER_FINDER));
        Assertions.assertTrue(RegExUtil.find("Temperature: -5.5", RegExUtil.NEGATIVE_NUMBER_FINDER));
        Assertions.assertTrue(RegExUtil.find("Count: 42", RegExUtil.POSITIVE_INTEGER_FINDER));

        // Test DATE/TIME patterns
        Assertions.assertTrue(RegExUtil.matches("2023-12-25", RegExUtil.DATE_MATCHER));
        Assertions.assertTrue(RegExUtil.matches("14:30:45", RegExUtil.TIME_MATCHER));
        Assertions.assertTrue(RegExUtil.matches("2023-12-25 14:30:45", RegExUtil.DATE_TIME_MATCHER));

        // Test EMAIL pattern
        Assertions.assertTrue(RegExUtil.matches("user@example.com", RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER));

        // Test URL patterns
        Assertions.assertTrue(RegExUtil.find("Visit https://example.com", RegExUtil.HTTP_URL_FINDER));
        Assertions.assertTrue(RegExUtil.find("Protocol: ftp://files.com", RegExUtil.URL_FINDER));
    }
}