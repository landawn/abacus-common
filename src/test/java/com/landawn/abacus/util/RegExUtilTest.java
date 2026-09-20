package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.function.IntBiFunction;

public class RegExUtilTest extends AbstractTest {

    @Test
    @DisplayName("Test JAVA_IDENTIFIER_FINDER pattern")
    public void testJavaIdentifierFinder() {
        assertTrue(RegExUtil.JAVA_IDENTIFIER_FINDER.matcher("myVariable").find());
        assertTrue(RegExUtil.JAVA_IDENTIFIER_FINDER.matcher("_test").find());
        assertTrue(RegExUtil.JAVA_IDENTIFIER_FINDER.matcher("$var").find());
        assertTrue(RegExUtil.JAVA_IDENTIFIER_FINDER.matcher("var123").find());
        assertTrue(RegExUtil.JAVA_IDENTIFIER_FINDER.matcher("123var").find());
        assertTrue(RegExUtil.JAVA_IDENTIFIER_FINDER.matcher("123var").find());
        assertFalse(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("123var").find());
        assertFalse(RegExUtil.JAVA_IDENTIFIER_FINDER.matcher("").find());
    }

    @Test
    @DisplayName("Test INTEGER_FINDER pattern")
    public void testIntegerFinder() {
        assertTrue(RegExUtil.INTEGER_FINDER.matcher("123").find());
        assertTrue(RegExUtil.INTEGER_FINDER.matcher("-456").find());
        assertTrue(RegExUtil.INTEGER_FINDER.matcher("+789").find());
        assertTrue(RegExUtil.INTEGER_FINDER.matcher("abc123def").find());
        assertFalse(RegExUtil.INTEGER_FINDER.matcher("abc").find());
    }

    @Test
    @DisplayName("Test NUMBER_FINDER pattern")
    public void testNumberFinder() {
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("123").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("123.45").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("-123.45").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("+0.99").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("100.").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher(".25").find());
        assertFalse(RegExUtil.NUMBER_FINDER.matcher("abc").find());

        Matcher leading = RegExUtil.NUMBER_FINDER.matcher(".25");
        assertTrue(leading.find());
        assertEquals(".25", leading.group(1));

        Matcher signedLeading = RegExUtil.NUMBER_FINDER.matcher("-.5");
        assertTrue(signedLeading.find());
        assertEquals("-.5", signedLeading.group(1));

        Matcher trailing = RegExUtil.NUMBER_FINDER.matcher("100.");
        assertTrue(trailing.find());
        assertEquals("100.", trailing.group(1));
    }

    @Test
    @DisplayName("Test SCIENTIFIC_NUMBER_FINDER pattern")
    public void testScientificNumberFinder() {
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("1.5e10").find());
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("-2E-5").find());
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("+6.022e23").find());
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("42").find());
        assertFalse(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("abc").find());

        Matcher leadingSci = RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher(".5e2");
        assertTrue(leadingSci.find());
        assertEquals(".5e2", leadingSci.group(1));

        Matcher signedLeadingSci = RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("-.5e2");
        assertTrue(signedLeadingSci.find());
        assertEquals("-.5e2", signedLeadingSci.group(1));
    }

    @Test
    @DisplayName("Test PHONE_NUMBER_FINDER pattern")
    public void testPhoneNumberFinder() {
        assertTrue(RegExUtil.PHONE_NUMBER_FINDER.matcher("123 456 7890").find());
        assertTrue(RegExUtil.PHONE_NUMBER_FINDER.matcher("+1 234 567 8900").find());
        assertTrue(RegExUtil.PHONE_NUMBER_FINDER.matcher("555").find());
        assertFalse(RegExUtil.PHONE_NUMBER_FINDER.matcher("ab").find());
        assertFalse(RegExUtil.PHONE_NUMBER_FINDER.matcher("   ").find());
        assertFalse(RegExUtil.PHONE_NUMBER_MATCHER.matcher("  1 ").matches());
        assertTrue(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("+1 234 567 8900").matches());
        assertFalse(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("               ").matches());
    }

    @Test
    @DisplayName("Test DATE_FINDER pattern")
    public void testDateFinder() {
        assertTrue(RegExUtil.DATE_FINDER.matcher("2023-12-25").find());
        assertTrue(RegExUtil.DATE_FINDER.matcher("2023/12/25").find());
        assertTrue(RegExUtil.DATE_FINDER.matcher("2023.12.25").find());
        assertTrue(RegExUtil.DATE_FINDER.matcher("2023 12 25").find());
        assertFalse(RegExUtil.DATE_FINDER.matcher("2023-13-25").find());
        assertFalse(RegExUtil.DATE_FINDER.matcher("2023-12-32").find());
    }

    @Test
    @DisplayName("Test TIME_FINDER pattern")
    public void testTimeFinder() {
        assertTrue(RegExUtil.TIME_FINDER.matcher("12:30:45").find());
        assertTrue(RegExUtil.TIME_FINDER.matcher("00:00:00").find());
        assertTrue(RegExUtil.TIME_FINDER.matcher("23:59:59").find());
        assertFalse(RegExUtil.TIME_FINDER.matcher("24:00:00").find());
        assertFalse(RegExUtil.TIME_FINDER.matcher("12:60:00").find());
        assertFalse(RegExUtil.TIME_FINDER.matcher("9:15:30").find());
    }

    @Test
    @DisplayName("Test DATE_TIME_FINDER pattern")
    public void testDateTimeFinder() {
        assertTrue(RegExUtil.DATE_TIME_FINDER.matcher("2023-12-25 12:30:45").find());
        assertTrue(RegExUtil.DATE_TIME_FINDER.matcher("2023/12/25 23:59:59").find());
        assertFalse(RegExUtil.DATE_TIME_FINDER.matcher("2023-12-25").find());
    }

    @Test
    @DisplayName("Test BANK_CARD_NUMBER_FINDER pattern")
    public void testBankCardNumberFinder() {
        assertTrue(RegExUtil.BANK_CARD_NUMBER_FINDER.matcher("1234 5678 9012 3456").find());
        assertTrue(RegExUtil.BANK_CARD_NUMBER_FINDER.matcher("1234-5678-9012-3456").find());
        assertTrue(RegExUtil.BANK_CARD_NUMBER_FINDER.matcher("1234567890123456").find());
        assertFalse(RegExUtil.BANK_CARD_NUMBER_FINDER.matcher("123").find());
    }

    @Test
    @DisplayName("Test URL_FINDER pattern")
    public void testUrlFinder() {
        assertTrue(RegExUtil.URL_FINDER.matcher("https://www.example.com").find());
        assertTrue(RegExUtil.URL_FINDER.matcher("ftp://ftp.example.com/file").find());
        assertTrue(RegExUtil.URL_FINDER.matcher("http://localhost:8080/api").find());
        assertTrue(RegExUtil.URL_FINDER.matcher("HTTPS://EXAMPLE.COM/path").find());
        assertEquals("http://a", RegExUtil.findFirst("http://a trailing", RegExUtil.URL_FINDER));
        assertFalse(RegExUtil.URL_FINDER.matcher("not a url").find());
    }

    @Test
    @DisplayName("Test HTTP_URL_FINDER pattern")
    public void testHttpUrlFinder() {
        assertTrue(RegExUtil.HTTP_URL_FINDER.matcher("http://www.example.com").find());
        assertTrue(RegExUtil.HTTP_URL_FINDER.matcher("https://api.example.com:8443/v1/users?id=123").find());
        assertTrue(RegExUtil.HTTP_URL_MATCHER.matcher("https://example.com?view=compact#summary").matches());
        assertTrue(RegExUtil.HTTP_URL_MATCHER.matcher("https://example.com#summary").matches());
        assertTrue(RegExUtil.HTTP_URL_FINDER.matcher("HTTP://EXAMPLE.COM").find());
        assertTrue(RegExUtil.HTTP_URL_MATCHER.matcher("https://example.com/foo-bar").matches());
        assertTrue(RegExUtil.HTTP_URL_MATCHER.matcher("https://example.com/search?q=hello+world").matches());
        assertTrue(RegExUtil.HTTP_URL_MATCHER.matcher("https://example.com/a%20b").matches());
        assertTrue(RegExUtil.HTTP_URL_MATCHER.matcher("https://example.com/~user").matches());
        assertEquals("https://example.com/foo-bar", RegExUtil.findFirst("Visit https://example.com/foo-bar today", RegExUtil.HTTP_URL_FINDER));
        assertFalse(RegExUtil.HTTP_URL_MATCHER.matcher("http://.").matches());
        assertFalse(RegExUtil.HTTP_URL_FINDER.matcher("ftp://example.com").find());
    }

    @Test
    @DisplayName("Test ALPHANUMERIC_FINDER pattern")
    public void testAlphanumericFinder() {
        assertTrue(RegExUtil.ALPHANUMERIC_FINDER.matcher("ABC123").find());
        assertTrue(RegExUtil.ALPHANUMERIC_FINDER.matcher("test456").find());
        assertFalse(RegExUtil.ALPHANUMERIC_FINDER.matcher("@#$").find());
    }

    @Test
    @DisplayName("Test ALPHANUMERIC_SPACE_FINDER pattern")
    public void testAlphanumericSpaceFinder() {
        assertTrue(RegExUtil.ALPHANUMERIC_SPACE_FINDER.matcher("ABC 123").find());
        assertTrue(RegExUtil.ALPHANUMERIC_SPACE_FINDER.matcher("Hello World 123").find());
        assertFalse(RegExUtil.ALPHANUMERIC_SPACE_FINDER.matcher("@#$").find());
    }

    @Test
    @DisplayName("Test DUPLICATES_FINDER pattern")
    public void testDuplicatesFinder() {
        assertTrue(RegExUtil.DUPLICATES_FINDER.matcher("the quick brown fox jumps over the lazy dog").find());
        assertFalse(RegExUtil.DUPLICATES_FINDER.matcher("all words are unique").find());
    }

    @Test
    @DisplayName("Test WHITESPACE_FINDER pattern")
    public void testWhitespaceFinder() {
        assertTrue(RegExUtil.WHITESPACE_FINDER.matcher("   ").find());
        assertTrue(RegExUtil.WHITESPACE_FINDER.matcher("\t\n").find());
        assertFalse(RegExUtil.WHITESPACE_FINDER.matcher("abc").find());
    }

    @Test
    @DisplayName("Test LINE_SEPARATOR pattern")
    public void testLineSeparator() {
        assertTrue(RegExUtil.LINE_SEPARATOR.matcher("\n").find());
        assertTrue(RegExUtil.LINE_SEPARATOR.matcher("\r\n").find());
        assertTrue(RegExUtil.LINE_SEPARATOR.matcher("\r").find());
        assertFalse(RegExUtil.LINE_SEPARATOR.matcher("abc").find());
    }

    @Test
    @DisplayName("Test word boundaries")
    public void testWordBoundaries() {
        assertTrue(RegExUtil.find("the cat", "\\bcat\\b"));
        assertFalse(RegExUtil.find("scatter", "\\bcat\\b"));
    }

    @Test
    @DisplayName("Test backreferences")
    public void testBackreferences() {
        assertTrue(RegExUtil.find("the the", "(\\w+)\\s+\\1"));
        assertFalse(RegExUtil.find("the cat", "(\\w+)\\s+\\1"));
    }

    @Test
    @DisplayName("Test lookahead and lookbehind")
    public void testLookaheadLookbehind() {
        assertTrue(RegExUtil.find("test123", "\\d+(?=\\b)"));
        assertTrue(RegExUtil.find("$100", "(?<=\\$)\\d+"));
    }

    @Test
    @DisplayName("Test Unicode characters")
    public void testUnicodeCharacters() {
        assertTrue(RegExUtil.find("Hello 世界", "世界"));
        assertTrue(RegExUtil.find("Café", "Café"));
    }

    @Test
    @DisplayName("Test very long strings")
    public void testVeryLongStrings() {
        String longString = "a".repeat(10000) + "123" + "b".repeat(10000);
        assertTrue(RegExUtil.find(longString, "\\d+"));
        assertEquals(1, RegExUtil.countMatches(longString, "\\d+"));
    }

    @Test
    @DisplayName("Test precompiled pattern reuse")
    public void testPrecompiledPatternReuse() {
        Pattern pattern = Pattern.compile("\\d+");
        assertTrue(RegExUtil.find("123", pattern));
        assertTrue(RegExUtil.find("456", pattern));
        assertTrue(RegExUtil.find("789", pattern));
    }

    @Test
    @DisplayName("Test POSITIVE_INTEGER_FINDER pattern")
    public void testPositiveIntegerFinder() {
        assertTrue(RegExUtil.POSITIVE_INTEGER_FINDER.matcher("123").find());
        assertTrue(RegExUtil.POSITIVE_INTEGER_FINDER.matcher("0").find());
        assertFalse(RegExUtil.POSITIVE_INTEGER_FINDER.matcher("abc").find());
    }

    @Test
    @DisplayName("Test NEGATIVE_INTEGER_FINDER pattern")
    public void testNegativeIntegerFinder() {
        assertTrue(RegExUtil.NEGATIVE_INTEGER_FINDER.matcher("-123").find());
        assertFalse(RegExUtil.NEGATIVE_INTEGER_FINDER.matcher("123").find());
        assertFalse(RegExUtil.NEGATIVE_INTEGER_FINDER.matcher("abc").find());
    }

    @Test
    @DisplayName("Test POSITIVE_NUMBER_FINDER pattern")
    public void testPositiveNumberFinder() {
        assertTrue(RegExUtil.POSITIVE_NUMBER_FINDER.matcher("123").find());
        assertTrue(RegExUtil.POSITIVE_NUMBER_FINDER.matcher("123.45").find());
        assertTrue(RegExUtil.POSITIVE_NUMBER_FINDER.matcher(".25").find());
        assertTrue(RegExUtil.POSITIVE_NUMBER_FINDER.matcher("100.").find());
        assertFalse(RegExUtil.POSITIVE_NUMBER_FINDER.matcher("abc").find());
    }

    @Test
    @DisplayName("Test NEGATIVE_NUMBER_FINDER pattern")
    public void testNegativeNumberFinder() {
        assertTrue(RegExUtil.NEGATIVE_NUMBER_FINDER.matcher("-123").find());
        assertTrue(RegExUtil.NEGATIVE_NUMBER_FINDER.matcher("-123.45").find());
        assertTrue(RegExUtil.NEGATIVE_NUMBER_FINDER.matcher("-.25").find());
        assertFalse(RegExUtil.NEGATIVE_NUMBER_FINDER.matcher("123").find());
    }

    @Test
    @DisplayName("Test find(String, String) with valid regex")
    public void testFindWithRegex() {
        Pattern digits = Pattern.compile("\\d+");
        assertTrue(RegExUtil.find("Hello123World", "\\d+"));
        assertTrue(RegExUtil.find("Hello123World", digits));
        assertTrue(RegExUtil.find("test@example.com", "\\w+@\\w+\\.\\w+"));
        assertFalse(RegExUtil.find("Hello World", "\\d+"));
        assertFalse(RegExUtil.find("Hello World", digits));
        assertFalse(RegExUtil.find(null, "\\d+"));
        assertFalse(RegExUtil.find(null, digits));
        assertFalse(RegExUtil.find("", "\\d+"));
        assertFalse(RegExUtil.find("", digits));
        assertTrue(RegExUtil.find("Hello World", "World"));
        assertTrue(RegExUtil.find("Price: $99.99", "\\$\\d+\\.\\d+"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test case sensitivity")
    public void testCaseSensitivity() {
        assertTrue(RegExUtil.find("Hello World", "Hello"));
        assertFalse(RegExUtil.find("Hello World", "hello"));
        assertTrue(RegExUtil.find("Hello World", "(?i)hello"));
    }

    @Test
    public void testPredefinedPatterns() {
        Assertions.assertTrue(RegExUtil.find("class MyClass", RegExUtil.JAVA_IDENTIFIER_FINDER));
        Assertions.assertTrue(RegExUtil.find("$var_123", RegExUtil.JAVA_IDENTIFIER_FINDER));

        Assertions.assertTrue(RegExUtil.find("Price: $99.99", RegExUtil.NUMBER_FINDER));
        Assertions.assertTrue(RegExUtil.find("Temperature: -5.5", RegExUtil.NEGATIVE_NUMBER_FINDER));
        Assertions.assertTrue(RegExUtil.find("Count: 42", RegExUtil.POSITIVE_INTEGER_FINDER));

        Assertions.assertTrue(RegExUtil.matches("2023-12-25", RegExUtil.DATE_MATCHER));
        Assertions.assertTrue(RegExUtil.matches("14:30:45", RegExUtil.TIME_MATCHER));
        Assertions.assertTrue(RegExUtil.matches("2023-12-25 14:30:45", RegExUtil.DATE_TIME_MATCHER));

        Assertions.assertTrue(RegExUtil.matches("user@example.com", RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER));

        Assertions.assertTrue(RegExUtil.find("Visit https://example.com", RegExUtil.HTTP_URL_FINDER));
        Assertions.assertTrue(RegExUtil.find("Protocol: ftp://files.com", RegExUtil.URL_FINDER));
    }

    @Test
    @DisplayName("Test EMAIL_ADDRESS_RFC_5322_FINDER pattern")
    public void testEmailAddressFinder() {
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("test@example.com").find());
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("user.name@example.co.uk").find());
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("user+tag@example.com").find());
        assertFalse(RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("invalid@").find());
    }

    @Test
    @DisplayName("Test INTEGER_MATCHER pattern")
    public void testIntegerMatcher() {
        assertTrue(RegExUtil.INTEGER_MATCHER.matcher("123").matches());
        assertTrue(RegExUtil.INTEGER_MATCHER.matcher("-456").matches());
        assertFalse(RegExUtil.INTEGER_MATCHER.matcher("abc123").matches());
        assertFalse(RegExUtil.INTEGER_MATCHER.matcher("123.45").matches());
    }

    @Test
    @DisplayName("Test NUMBER_MATCHER pattern")
    public void testNumberMatcher() {
        assertTrue(RegExUtil.NUMBER_MATCHER.matcher("123").matches());
        assertTrue(RegExUtil.NUMBER_MATCHER.matcher("123.45").matches());
        assertTrue(RegExUtil.NUMBER_MATCHER.matcher(".5").matches());
        assertTrue(RegExUtil.NUMBER_MATCHER.matcher("-.5").matches());
        assertTrue(RegExUtil.NUMBER_MATCHER.matcher("1.").matches());
        assertFalse(RegExUtil.NUMBER_MATCHER.matcher("abc123").matches());
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_MATCHER.matcher(".5e2").matches());
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_MATCHER.matcher("-.5e2").matches());
    }

    @Test
    @DisplayName("Test EMAIL_ADDRESS_RFC_5322_MATCHER pattern")
    public void testEmailMatcher() {
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher("test@example.com").matches());
        assertFalse(RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher("not an email").matches());
    }

    @Test
    @DisplayName("Test anchors")
    public void testAnchors() {
        assertTrue(RegExUtil.matches("Hello", "^Hello$"));
        assertFalse(RegExUtil.matches("Hello World", "^Hello$"));
        assertTrue(RegExUtil.find("Hello World", "^Hello"));
        assertTrue(RegExUtil.find("Hello World", "World$"));
    }

    @Test
    @DisplayName("Test matches(String, String) with valid regex")
    public void testMatchesWithRegex() {
        Pattern digits = Pattern.compile("\\d+");
        assertTrue(RegExUtil.matches("123", "\\d+"));
        assertTrue(RegExUtil.matches("123", digits));
        assertTrue(RegExUtil.matches("abc", "[a-z]+"));
        assertFalse(RegExUtil.matches("abc123", "\\d+"));
        assertFalse(RegExUtil.matches("abc123", digits));
        assertFalse(RegExUtil.matches(null, "\\d+"));
        assertFalse(RegExUtil.matches(null, digits));
        assertFalse(RegExUtil.matches("", "\\d+"));
        assertTrue(RegExUtil.matches("hello@example.com", "[a-z]+@[a-z]+\\.[a-z]+"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test findFirst with special regex patterns")
    public void testFindFirstWithSpecialPatterns() {
        // Test with word boundaries
        Pattern wordPattern = Pattern.compile("\\bcat\\b");
        assertEquals("cat", RegExUtil.findFirst("The cat and the caterpillar", wordPattern));

        // Test with lookahead
        Pattern lookaheadPattern = Pattern.compile("\\d+(?=\\$)");
        assertEquals("100", RegExUtil.findFirst("Price: 100$ and 200€", lookaheadPattern));

        // Test with lookbehind
        Pattern lookbehindPattern = Pattern.compile("(?<=\\$)\\d+");
        assertEquals("99", RegExUtil.findFirst("Total: $99 and €50", lookbehindPattern));

        // Test with groups
        Pattern groupPattern = Pattern.compile("(\\w+)@(\\w+\\.\\w+)");
        assertEquals("user@domain.com", RegExUtil.findFirst("Email: user@domain.com", groupPattern));
    }

    @Test
    @DisplayName("Test findFirst with greedy vs non-greedy")
    public void testFindFirstGreedyVsNonGreedy() {
        // Greedy pattern - will match from first < to last > in the string
        Pattern greedyPattern = Pattern.compile("<.*>");
        assertEquals("<tag>content</tag> and <other>text</other>", RegExUtil.findFirst("<tag>content</tag> and <other>text</other>", greedyPattern));

        // Non-greedy pattern
        Pattern nonGreedyPattern = Pattern.compile("<.*?>");
        assertEquals("<tag>", RegExUtil.findFirst("<tag>content</tag> and <other>text</other>", nonGreedyPattern));
    }

    @Test
    @DisplayName("Test findFirst with Unicode and special characters")
    public void testFindFirstWithUnicode() {
        Pattern unicodePattern = Pattern.compile("[\\u4e00-\\u9fa5]+");
        assertEquals("你好", RegExUtil.findFirst("Hello 你好 World 世界", unicodePattern));

        Pattern emojiPattern = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+");
        assertEquals("😀", RegExUtil.findFirst("Hello 😀 World 🌍", emojiPattern));
    }

    @Test
    @DisplayName("Test findFirst and findLast with multiline text")
    public void testFindFirstAndLastMultiline() {
        String multilineText = "Line 1: 100\nLine 2: 200\nLine 3: 300";

        Pattern numberPattern = Pattern.compile("\\d+");
        assertEquals("1", RegExUtil.findFirst(multilineText, numberPattern)); // Finds "1" in "Line 1"
        assertEquals("300", RegExUtil.findLast(multilineText, numberPattern));

        Pattern lineStartPattern = Pattern.compile("^Line \\d+", Pattern.MULTILINE);
        assertEquals("Line 1", RegExUtil.findFirst(multilineText, lineStartPattern));
        assertEquals("Line 3", RegExUtil.findLast(multilineText, lineStartPattern));
    }

    @Test
    @DisplayName("Test findFirst and findLast with complex patterns")
    public void testFindFirstAndLastComplexPatterns() {
        // URL pattern - simplified to avoid regex syntax issues
        Pattern urlPattern = Pattern.compile("https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w\\-._~:/?#@!$&'()*+,;=]*");
        String textWithUrls = "Visit https://example.com and https://test.org for more info";
        assertEquals("https://example.com", RegExUtil.findFirst(textWithUrls, urlPattern));
        assertEquals("https://test.org", RegExUtil.findLast(textWithUrls, urlPattern));

        // Hex color pattern
        Pattern hexPattern = Pattern.compile("#[0-9A-Fa-f]{6}\\b");
        String cssText = "color: #FF5733; background: #00AA00; border: #123456;";
        assertEquals("#FF5733", RegExUtil.findFirst(cssText, hexPattern));
        assertEquals("#123456", RegExUtil.findLast(cssText, hexPattern));

        // IP address pattern
        Pattern ipPattern = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");
        String networkText = "Server IPs: 192.168.1.1, 10.0.0.1, 172.16.0.1";
        assertEquals("192.168.1.1", RegExUtil.findFirst(networkText, ipPattern));
        assertEquals("172.16.0.1", RegExUtil.findLast(networkText, ipPattern));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with greedy vs non-greedy")
    public void testFindFirstStringRegexGreedyVsNonGreedy() {
        // Greedy - matches from first < to last >
        assertEquals("<tag>content</tag>", RegExUtil.findFirst("<tag>content</tag> and <other>text</other>", "<([a-zA-Z0-9]+)>.*?</\\1>"));
        assertEquals("<other>text</other>", RegExUtil.findLast("<tag>content</tag> and <other>text</other>", "<([a-zA-Z0-9]+)>.*?</\\1>"));

        // Non-greedy - matches shortest possible
        assertEquals("<tag>", RegExUtil.findFirst("<tag>content</tag> and <other>text</other>", "<.*?>"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with Unicode and special characters")
    public void testFindFirstStringRegexWithUnicode() {
        assertEquals("你好", RegExUtil.findFirst("Hello 你好 World 世界", "[\\u4e00-\\u9fa5]+"));
        assertEquals("Café", RegExUtil.findFirst("I love Café and Naïve", "Café"));
        assertEquals("😀", RegExUtil.findFirst("Hello 😀 World 🌍", "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with multiline text")
    public void testFindFirstStringRegexMultiline() {
        String multilineText = "Line 1: 100\nLine 2: 200\nLine 3: 300";
        assertEquals("1", RegExUtil.findFirst(multilineText, "\\d+"));
        assertEquals("Line 1", RegExUtil.findFirst(multilineText, "^Line \\d+"));
    }

    @Test
    @DisplayName("Test findFirst and findLast(String, String) with many matches")
    public void testFindFirstAndLastStringRegexWithManyMatches() {
        // Create a string with many matches
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("item").append(i).append(" ");
        }
        String text = sb.toString();

        // findFirst should find the first occurrence
        assertEquals("item0", RegExUtil.findFirst(text, "item\\d+"));

        // findLast should find the last occurrence
        assertEquals("item99", RegExUtil.findLast(text, "item\\d+"));
    }

    @Test
    @DisplayName("Test findFirst and findLast(String, String) with complex patterns")
    public void testFindFirstAndLastStringRegexComplexPatterns() {
        // URL pattern
        String textWithUrls = "Visit https://example.com and https://test.org for more info";
        assertEquals("https://example.com", RegExUtil.findFirst(textWithUrls, "https?://[\\w.-]+(?:\\.[\\w.-]+)*"));
        assertEquals("https://test.org", RegExUtil.findLast(textWithUrls, "https?://[\\w.-]+(?:\\.[\\w.-]+)*"));

        // Hex color pattern
        String cssText = "color: #FF5733; background: #00AA00; border: #123456;";
        assertEquals("#FF5733", RegExUtil.findFirst(cssText, "#[0-9A-Fa-f]{6}\\b"));
        assertEquals("#123456", RegExUtil.findLast(cssText, "#[0-9A-Fa-f]{6}\\b"));

        // IP address pattern
        String networkText = "Server IPs: 192.168.1.1, 10.0.0.1, 172.16.0.1";
        assertEquals("192.168.1.1", RegExUtil.findFirst(networkText, "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"));
        assertEquals("172.16.0.1", RegExUtil.findLast(networkText, "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"));
    }

    @Test
    @DisplayName("Test findFirst and findLast(String, String) with whitespace patterns")
    public void testFindFirstAndLastStringRegexWithWhitespace() {
        String text = "Hello   World  Test   End";

        // Find first whitespace sequence
        assertEquals("   ", RegExUtil.findFirst(text, "\\s+"));

        // Find last whitespace sequence
        assertEquals("   ", RegExUtil.findLast(text, "\\s+"));

        // Find first word
        assertEquals("Hello", RegExUtil.findFirst(text, "\\w+"));

        // Find last word
        assertEquals("End", RegExUtil.findLast(text, "\\w+"));
    }

    @Test
    @DisplayName("Test findFirst and findLast(String, String) with digit patterns")
    public void testFindFirstAndLastStringRegexWithDigits() {
        String text = "Price: $10.99, Tax: $2.15, Total: $13.14";

        // Integer part only
        assertEquals("10", RegExUtil.findFirst(text, "\\d+"));
        assertEquals("14", RegExUtil.findLast(text, "\\d+"));

        // Decimal numbers
        assertEquals("10.99", RegExUtil.findFirst(text, "\\d+\\.\\d+"));
        assertEquals("13.14", RegExUtil.findLast(text, "\\d+\\.\\d+"));
    }

    @Test
    @DisplayName("Test findFirst and findLast(String, String) with alternation")
    public void testFindFirstAndLastStringRegexWithAlternation() {
        String text = "I like cats and dogs and birds and cats";

        assertEquals("cats", RegExUtil.findFirst(text, "cats|dogs|birds"));
        assertEquals("cats", RegExUtil.findLast(text, "cats|dogs|birds"));

        String text2 = "red green blue yellow red";
        assertEquals("red", RegExUtil.findFirst(text2, "red|blue|yellow"));
        assertEquals("red", RegExUtil.findLast(text2, "red|blue|yellow"));
    }

    @Test
    @DisplayName("Test findFirst and findLast(String, String) with quantifiers")
    public void testFindFirstAndLastStringRegexWithQuantifiers() {
        String text = "a aa aaa aaaa aaaaa";

        // Exactly 2 'a's
        assertEquals("aa", RegExUtil.findFirst(text, "a{2}"));
        assertEquals("aa", RegExUtil.findLast(text, "a{2}"));

        // At least 3 'a's (greedy)
        assertEquals("aaa", RegExUtil.findFirst(text, "a{3,}"));
        assertEquals("aaaaa", RegExUtil.findLast(text, "a{3,}"));

        // Between 2 and 4 'a's
        assertEquals("aa", RegExUtil.findFirst(text, "a{2,4}"));
        assertEquals("aaaa", RegExUtil.findLast(text, "a{2,4}"));
    }

    // ===========================================
    // Tests for findFirst and findLast methods
    // ===========================================

    @Test
    @DisplayName("Test findFirst with single match")
    public void testFindFirstWithSingleMatch() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("123", RegExUtil.findFirst("abc123def", pattern));

        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        assertEquals("test@example.com", RegExUtil.findFirst("Contact: test@example.com for info", emailPattern));
    }

    @Test
    @DisplayName("Test findFirst with multiple matches returns first")
    public void testFindFirstWithMultipleMatches() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("123", RegExUtil.findFirst("abc123def456ghi789", pattern));

        Pattern wordPattern = Pattern.compile("\\b\\w{4}\\b");
        assertEquals("This", RegExUtil.findFirst("This is a test with many four letter words", wordPattern));
    }

    @Test
    @DisplayName("Test findFirst with no match returns null")
    public void testFindFirstWithNoMatch() {
        Pattern pattern = Pattern.compile("\\d+");
        assertNull(RegExUtil.findFirst("abc def ghi", pattern));

        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        assertNull(RegExUtil.findFirst("No email here", emailPattern));
    }

    @Test
    @DisplayName("Test findFirst with null source returns null")
    public void testFindFirstWithNullSource() {
        Pattern pattern = Pattern.compile("\\d+");
        assertNull(RegExUtil.findFirst(null, pattern));

        // A null source is NOT normalized to "": it is never handed to a matcher, so even a pattern that can
        // match the empty string finds nothing in it - while an empty source is matched normally.
        final Pattern zeroWidth = Pattern.compile("\\d*");
        assertNull(RegExUtil.findFirst(null, zeroWidth));
        assertEquals("", RegExUtil.findFirst("", zeroWidth));
    }

    @Test
    @DisplayName("Test findFirst with empty source returns null")
    public void testFindFirstWithEmptySource() {
        Pattern pattern = Pattern.compile("\\d+");
        assertNull(RegExUtil.findFirst("", pattern));

        Pattern anyPattern = Pattern.compile(".*");
        assertEquals("", RegExUtil.findFirst("", anyPattern)); // Empty match
    }

    @Test
    @DisplayName("Test findFirst with predefined patterns")
    public void testFindFirstWithPredefinedPatterns() {
        // Test with INTEGER_FINDER
        assertEquals("123", RegExUtil.findFirst("abc123def456", RegExUtil.INTEGER_FINDER));
        assertEquals("-456", RegExUtil.findFirst("abc-456def+789", RegExUtil.INTEGER_FINDER));

        // Test with NUMBER_FINDER
        assertEquals("3.14", RegExUtil.findFirst("Pi is 3.14 and e is 2.71", RegExUtil.NUMBER_FINDER));

        // Test with EMAIL_ADDRESS_RFC_5322_FINDER
        assertEquals("first@example.com", RegExUtil.findFirst("Emails: first@example.com, second@test.org", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER));

        // Test with DATE_FINDER
        assertEquals("2023-12-25", RegExUtil.findFirst("Born on 2023-12-25, died on 2024-01-15", RegExUtil.DATE_FINDER));

        // Test with PHONE_NUMBER_FINDER
        String phoneResult = RegExUtil.findFirst("Call 123 456 7890 or 098 765 4321", RegExUtil.PHONE_NUMBER_FINDER);
        assertNotNull(phoneResult); // Phone pattern finds something, exact match depends on pattern
    }

    @Test
    @DisplayName("Test findFirst at beginning of string")
    public void testFindFirstAtBeginning() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("123", RegExUtil.findFirst("123abc456", pattern));

        Pattern anchorPattern = Pattern.compile("^\\d+");
        assertEquals("123", RegExUtil.findFirst("123abc456", anchorPattern));
        assertNull(RegExUtil.findFirst("abc123def", anchorPattern));
    }

    @Test
    @DisplayName("Test findFirst with case sensitivity")
    public void testFindFirstCaseSensitivity() {
        Pattern caseSensitive = Pattern.compile("Hello");
        assertEquals("Hello", RegExUtil.findFirst("Say Hello World", caseSensitive));
        assertNull(RegExUtil.findFirst("Say hello World", caseSensitive));

        Pattern caseInsensitive = Pattern.compile("(?i)Hello");
        assertEquals("hello", RegExUtil.findFirst("Say hello World", caseInsensitive));
        assertEquals("HELLO", RegExUtil.findFirst("Say HELLO World", caseInsensitive));
    }

    @Test
    @DisplayName("Test findFirst and findLast consistency")
    public void testFindFirstAndLastConsistency() {
        // With single match, both should return the same
        Pattern singleMatchPattern = Pattern.compile("unique");
        String singleMatchText = "This is a unique test";
        assertEquals(RegExUtil.findFirst(singleMatchText, singleMatchPattern), RegExUtil.findLast(singleMatchText, singleMatchPattern));

        // With no match, both should return null
        Pattern noMatchPattern = Pattern.compile("\\d+");
        String noMatchText = "No numbers here";
        assertNull(RegExUtil.findFirst(noMatchText, noMatchPattern));
        assertNull(RegExUtil.findLast(noMatchText, noMatchPattern));

        // With empty source, both should behave the same
        Pattern anyPattern = Pattern.compile("\\w+");
        assertNull(RegExUtil.findFirst("", anyPattern));
        assertNull(RegExUtil.findLast("", anyPattern));

        // With null source, both should behave the same
        assertNull(RegExUtil.findFirst(null, anyPattern));
        assertNull(RegExUtil.findLast(null, anyPattern));
    }

    @Test
    @DisplayName("Test findFirst and findLast with empty matches")
    public void testFindFirstAndLastWithEmptyMatches() {
        // Pattern that can match empty string
        Pattern emptyPattern = Pattern.compile("\\b");

        String text = "Hello World";
        // Both should find word boundaries
        assertNotNull(RegExUtil.findFirst(text, emptyPattern));
        assertNotNull(RegExUtil.findLast(text, emptyPattern));

        // Zero-width assertion
        Pattern zeroWidthPattern = Pattern.compile("(?=\\w)");
        assertNotNull(RegExUtil.findFirst("abc", zeroWidthPattern));
        assertNotNull(RegExUtil.findLast("abc", zeroWidthPattern));
    }

    @Test
    @DisplayName("Test findFirst and findLast edge cases")
    public void testFindFirstAndLastEdgeCases() {
        // Very long string
        String longString = "a".repeat(10000) + "TARGET" + "b".repeat(10000);
        Pattern targetPattern = Pattern.compile("TARGET");
        assertEquals("TARGET", RegExUtil.findFirst(longString, targetPattern));
        assertEquals("TARGET", RegExUtil.findLast(longString, targetPattern));

        // String with special characters
        String specialChars = "!@#$%^&*()_+-=[] {}|;':\",./<>?";
        Pattern specialPattern = Pattern.compile("[!@#$%^&*()]+");
        assertEquals("!@#$%^&*()", RegExUtil.findFirst(specialChars, specialPattern));

        // Pattern matching entire string
        Pattern fullMatchPattern = Pattern.compile(".*");
        assertEquals("Hello World", RegExUtil.findFirst("Hello World", fullMatchPattern));
        assertEquals("", RegExUtil.findLast("Hello World", fullMatchPattern)); // Last match might be empty at the end

        // Backreference pattern
        Pattern backrefPattern = Pattern.compile("(\\w+)\\s+\\1");
        assertEquals("the the", RegExUtil.findFirst("the the cat cat dog dog", backrefPattern));
        assertEquals("dog dog", RegExUtil.findLast("the the cat cat dog dog", backrefPattern));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with common patterns")
    public void testFindFirstStringRegexWithCommonPatterns() {
        // Email pattern
        assertEquals("user@example.com", RegExUtil.findFirst("Contact: user@example.com or admin@test.org", "\\w+@\\w+\\.\\w+"));

        // URL pattern
        assertEquals("https://www.example.com", RegExUtil.findFirst("Visit https://www.example.com for info", "https?://[\\w.]+"));

        // Phone number pattern
        assertEquals("123-456-7890", RegExUtil.findFirst("Call 123-456-7890 or 098-765-4321", "\\d{3}-\\d{3}-\\d{4}"));

        // Date pattern
        assertEquals("2023-12-25", RegExUtil.findFirst("Born on 2023-12-25, died on 2024-01-15", "\\d{4}-\\d{2}-\\d{2}"));

        // Hex color
        assertEquals("#FF5733", RegExUtil.findFirst("color: #FF5733; background: #00AA00", "#[0-9A-Fa-f]{6}"));

        // IP address
        assertEquals("192.168.1.1", RegExUtil.findFirst("Server: 192.168.1.1 and 10.0.0.1", "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with special regex features")
    public void testFindFirstStringRegexWithSpecialFeatures() {
        // Case insensitive
        assertEquals("Hello", RegExUtil.findFirst("Say Hello World", "(?i)hello"));
        assertEquals("HELLO", RegExUtil.findFirst("Say HELLO World", "(?i)hello"));

        // Word boundaries
        assertEquals("cat", RegExUtil.findFirst("The cat and caterpillar", "\\bcat\\b"));

        // Lookahead
        assertEquals("100", RegExUtil.findFirst("Price: 100$ and 200€", "\\d+(?=\\$)"));

        // Lookbehind
        assertEquals("99", RegExUtil.findFirst("Total: $99 and €50", "(?<=\\$)\\d+"));

        // Groups
        assertEquals("user@domain.com", RegExUtil.findFirst("Email: user@domain.com", "(\\w+)@(\\w+\\.\\w+)"));

        // Backreferences
        assertEquals("the the", RegExUtil.findFirst("the the cat dog", "(\\w+)\\s+\\1"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) at beginning of string")
    public void testFindFirstStringRegexAtBeginning() {
        assertEquals("123", RegExUtil.findFirst("123abc456", "\\d+"));
        assertEquals("123", RegExUtil.findFirst("123abc456", "^\\d+"));
        assertNull(RegExUtil.findFirst("abc123def", "^\\d+"));
    }

    @Test
    @DisplayName("Test findFirst and findLast(String, String) consistency")
    public void testFindFirstAndLastStringRegexConsistency() {
        // With single match, both should return the same
        String singleMatchText = "This is a unique test";
        assertEquals(RegExUtil.findFirst(singleMatchText, "unique"), RegExUtil.findLast(singleMatchText, "unique"));

        // With no match, both should return null
        String noMatchText = "No numbers here";
        assertNull(RegExUtil.findFirst(noMatchText, "\\d+"));
        assertNull(RegExUtil.findLast(noMatchText, "\\d+"));

        // With empty source, both should behave the same
        assertNull(RegExUtil.findFirst("", "\\w+"));
        assertNull(RegExUtil.findLast("", "\\w+"));

        // With null source, both should behave the same
        assertNull(RegExUtil.findFirst(null, "\\w+"));
        assertNull(RegExUtil.findLast(null, "\\w+"));
    }

    @Test
    @DisplayName("Test findFirst and findLast(String, String) with edge cases")
    public void testFindFirstAndLastStringRegexEdgeCases() {
        // Very long string
        String longString = "a".repeat(10000) + "TARGET" + "b".repeat(10000);
        assertEquals("TARGET", RegExUtil.findFirst(longString, "TARGET"));
        assertEquals("TARGET", RegExUtil.findLast(longString, "TARGET"));

        // String with special characters
        String specialChars = "!@#$%^&*()_+-=[] {}|;':\",./<>?";
        assertEquals("!@#$%^&*()", RegExUtil.findFirst(specialChars, "[!@#$%^&*()]+"));

        // Consecutive matches
        assertEquals("123", RegExUtil.findFirst("123456789", "\\d{3}"));
        assertEquals("789", RegExUtil.findLast("123456789", "\\d{3}"));

        // Overlapping pattern possibilities
        assertEquals("12", RegExUtil.findFirst("12345678901234567890", "\\d{2}"));
        assertEquals("90", RegExUtil.findLast("12345678901234567890", "\\d{2}"));
    }

    @Test
    @DisplayName("Test findFirst with null pattern throws exception")
    public void testFindFirstWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst("test", (Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst(null, (Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test findLast with greedy vs non-greedy")
    public void testFindLastGreedyVsNonGreedy() {
        // Greedy pattern - matches the entire string from first < to last >
        Pattern greedyPattern = Pattern.compile("<.*>");
        assertEquals("<tag>content</tag> and <other>text</other>", RegExUtil.findLast("<tag>content</tag> and <other>text</other> extra", greedyPattern));

        // Non-greedy pattern - matches individual tags
        Pattern nonGreedyPattern = Pattern.compile("<.*?>");
        assertEquals("</other>", RegExUtil.findLast("<tag>content</tag> and <other>text</other>", nonGreedyPattern));
    }

    @Test
    @DisplayName("Test findLast with Unicode and special characters")
    public void testFindLastWithUnicode() {
        Pattern unicodePattern = Pattern.compile("[\\u4e00-\\u9fa5]+");
        assertEquals("世界", RegExUtil.findLast("Hello 你好 World 世界", unicodePattern));

        Pattern emojiPattern = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+");
        assertEquals("🌍", RegExUtil.findLast("Hello 😀 World 🌍", emojiPattern));
    }

    @Test
    @DisplayName("Test findLast performance with many matches")
    public void testFindLastPerformanceWithManyMatches() {
        // Create a string with many matches
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("match").append(i).append(" ");
        }
        String text = sb.toString();

        Pattern pattern = Pattern.compile("match\\d+");

        // findLast should find the last occurrence
        assertEquals("match999", RegExUtil.findLast(text, pattern));

        // findFirst should find the first occurrence
        assertEquals("match0", RegExUtil.findFirst(text, pattern));
    }

    @Test
    @DisplayName("Test findLast(String, String) with greedy vs non-greedy")
    public void testFindLastStringRegexGreedyVsNonGreedy() {
        // Greedy - matches from first < to last >
        assertEquals("<tag>content</tag> and <other>text</other>", RegExUtil.findLast("<tag>content</tag> and <other>text</other> extra", "<.*>"));

        // Non-greedy - matches individual tags
        assertEquals("</other>", RegExUtil.findLast("<tag>content</tag> and <other>text</other>", "<.*?>"));
    }

    @Test
    @DisplayName("Test findLast(String, String) with Unicode and special characters")
    public void testFindLastStringRegexWithUnicode() {
        assertEquals("世界", RegExUtil.findLast("Hello 你好 World 世界", "[\\u4e00-\\u9fa5]+"));
        assertEquals("Naïve", RegExUtil.findLast("I love Café and Naïve", "Naïve"));
        assertEquals("🌍", RegExUtil.findLast("Hello 😀 World 🌍", "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"));
    }

    @Test
    @DisplayName("Test findLast(String, String) with multiline text")
    public void testFindLastStringRegexMultiline() {
        String multilineText = "Line 1: 100\nLine 2: 200\nLine 3: 300";
        assertEquals("300", RegExUtil.findLast(multilineText, "\\d+"));
        assertEquals("Line 3", RegExUtil.findLast(multilineText, "Line \\d+"));
    }

    @Test
    @DisplayName("Test findLast with single match")
    public void testFindLastWithSingleMatch() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("123", RegExUtil.findLast("abc123def", pattern));

        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        assertEquals("test@example.com", RegExUtil.findLast("Contact: test@example.com for info", emailPattern));
    }

    @Test
    @DisplayName("Test findLast with multiple matches returns last")
    public void testFindLastWithMultipleMatches() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("789", RegExUtil.findLast("abc123def456ghi789", pattern));

        Pattern wordPattern = Pattern.compile("\\b\\w{4}\\b");
        assertEquals("char", RegExUtil.findLast("This is a test with many four char items", wordPattern)); // "char" is the last 4-letter word

        // Test with overlapping possibilities
        Pattern overlapPattern = Pattern.compile("\\d{2}");
        assertEquals("90", RegExUtil.findLast("12345678901234567890", overlapPattern)); // Last two digits are "90"
    }

    @Test
    @DisplayName("Test findLast with no match returns null")
    public void testFindLastWithNoMatch() {
        Pattern pattern = Pattern.compile("\\d+");
        assertNull(RegExUtil.findLast("abc def ghi", pattern));

        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        assertNull(RegExUtil.findLast("No email here", emailPattern));
    }

    @Test
    @DisplayName("Test findLast with null source returns null")
    public void testFindLastWithNullSource() {
        Pattern pattern = Pattern.compile("\\d+");
        assertNull(RegExUtil.findLast(null, pattern));

        // A null source is NOT normalized to "": it is never handed to a matcher, so even a pattern that can
        // match the empty string finds nothing in it - while an empty source is matched normally.
        final Pattern zeroWidth = Pattern.compile("\\d*");
        assertNull(RegExUtil.findLast(null, zeroWidth));
        assertEquals("", RegExUtil.findLast("", zeroWidth));
    }

    @Test
    @DisplayName("Test findLast with empty source returns null")
    public void testFindLastWithEmptySource() {
        Pattern pattern = Pattern.compile("\\d+");
        assertNull(RegExUtil.findLast("", pattern));

        Pattern anyPattern = Pattern.compile(".*");
        assertEquals("", RegExUtil.findLast("", anyPattern)); // Empty match
    }

    @Test
    @DisplayName("Test findLast with predefined patterns")
    public void testFindLastWithPredefinedPatterns() {
        // Test with INTEGER_FINDER
        assertEquals("456", RegExUtil.findLast("abc123def456", RegExUtil.INTEGER_FINDER));
        assertEquals("+789", RegExUtil.findLast("abc-456def+789", RegExUtil.INTEGER_FINDER));

        // Test with NUMBER_FINDER
        assertEquals("2.71", RegExUtil.findLast("Pi is 3.14 and e is 2.71", RegExUtil.NUMBER_FINDER));

        // Test with EMAIL_ADDRESS_RFC_5322_FINDER
        assertEquals("second@test.org", RegExUtil.findLast("Emails: first@example.com, second@test.org", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER));

        // Test with DATE_FINDER
        assertEquals("2024-01-15", RegExUtil.findLast("Born on 2023-12-25, died on 2024-01-15", RegExUtil.DATE_FINDER));

        // Test with WHITESPACE_FINDER - finds the last whitespace sequence
        String whitespaceResult = RegExUtil.findLast("Hello World  Test", RegExUtil.WHITESPACE_FINDER);
        assertNotNull(whitespaceResult); // Will find whitespace, but could be single or double space
    }

    @Test
    @DisplayName("Test findLast with special regex patterns")
    public void testFindLastWithSpecialPatterns() {
        // Test with word boundaries
        Pattern wordPattern = Pattern.compile("\\bcat\\b");
        assertEquals("cat", RegExUtil.findLast("The cat and another cat", wordPattern));

        // Test with lookahead
        Pattern lookaheadPattern = Pattern.compile("\\d+(?=\\$)");
        assertEquals("200", RegExUtil.findLast("Price: 100$ and 200$ total", lookaheadPattern));

        // Test with lookbehind
        Pattern lookbehindPattern = Pattern.compile("(?<=\\$)\\d+");
        assertEquals("150", RegExUtil.findLast("Total: $99 and final: $150", lookbehindPattern));

        // Test with groups
        Pattern groupPattern = Pattern.compile("(\\w+)@(\\w+\\.\\w+)");
        assertEquals("admin@test.org", RegExUtil.findLast("Emails: user@domain.com and admin@test.org", groupPattern));
    }

    @Test
    @DisplayName("Test findLast at end of string")
    public void testFindLastAtEnd() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("456", RegExUtil.findLast("123abc456", pattern));

        Pattern anchorPattern = Pattern.compile("\\d+$");
        assertEquals("456", RegExUtil.findLast("123abc456", anchorPattern));
        assertNull(RegExUtil.findLast("123abcdef", anchorPattern));
    }

    @Test
    @DisplayName("Test findLast with case sensitivity")
    public void testFindLastCaseSensitivity() {
        Pattern caseSensitive = Pattern.compile("Hello");
        assertEquals("Hello", RegExUtil.findLast("Hello World Hello Again", caseSensitive));
        assertNull(RegExUtil.findLast("hello world hello again", caseSensitive));

        Pattern caseInsensitive = Pattern.compile("(?i)Hello");
        assertEquals("HELLO", RegExUtil.findLast("hello World HELLO", caseInsensitive));
    }

    @Test
    @DisplayName("Test findLast(String, String) with common patterns")
    public void testFindLastStringRegexWithCommonPatterns() {
        // Email pattern
        assertEquals("admin@test.org", RegExUtil.findLast("Contact: user@example.com or admin@test.org", "\\w+@\\w+\\.\\w+"));

        // URL pattern
        assertEquals("https://test.org", RegExUtil.findLast("Visit https://www.example.com or https://test.org", "https?://[\\w.]+"));

        // Phone number pattern
        assertEquals("098-765-4321", RegExUtil.findLast("Call 123-456-7890 or 098-765-4321", "\\d{3}-\\d{3}-\\d{4}"));

        // Date pattern
        assertEquals("2024-01-15", RegExUtil.findLast("Born on 2023-12-25, died on 2024-01-15", "\\d{4}-\\d{2}-\\d{2}"));

        // Hex color
        assertEquals("#00AA00", RegExUtil.findLast("color: #FF5733; background: #00AA00", "#[0-9A-Fa-f]{6}"));

        // IP address
        assertEquals("10.0.0.1", RegExUtil.findLast("Server: 192.168.1.1 and 10.0.0.1", "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"));
    }

    @Test
    @DisplayName("Test findLast(String, String) with special regex features")
    public void testFindLastStringRegexWithSpecialFeatures() {
        // Case insensitive
        assertEquals("HELLO", RegExUtil.findLast("Say hello World HELLO", "(?i)hello"));

        // Word boundaries
        assertEquals("cat", RegExUtil.findLast("The cat and another cat", "\\bcat\\b"));

        // Lookahead
        assertEquals("200", RegExUtil.findLast("Price: 100$ and 200$ total", "\\d+(?=\\$)"));

        // Lookbehind
        assertEquals("150", RegExUtil.findLast("Total: $99 and final: $150", "(?<=\\$)\\d+"));

        // Groups
        assertEquals("admin@test.org", RegExUtil.findLast("Emails: user@domain.com and admin@test.org", "(\\w+)@(\\w+\\.\\w+)"));

        // Backreferences
        assertEquals("dog dog", RegExUtil.findLast("the the cat cat dog dog", "(\\w+)\\s+\\1"));
    }

    @Test
    @DisplayName("Test findLast(String, String) at end of string")
    public void testFindLastStringRegexAtEnd() {
        assertEquals("456", RegExUtil.findLast("123abc456", "\\d+"));
        assertEquals("456", RegExUtil.findLast("123abc456", "\\d+$"));
        assertNull(RegExUtil.findLast("123abcdef", "\\d+$"));
    }

    @Test
    @DisplayName("Test removeFirst(String, String) with valid regex")
    public void testRemoveFirstWithRegex() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals("HelloWorld456", RegExUtil.removeFirst("Hello123World456", "\\d+"));
        assertEquals("HelloWorld456", RegExUtil.removeFirst("Hello123World456", digits));
        assertEquals("", RegExUtil.removeFirst(null, "\\d+"));
        assertEquals("", RegExUtil.removeFirst(null, digits));
        assertEquals("", RegExUtil.removeFirst("", "\\d+"));
        assertEquals("abc", RegExUtil.removeFirst("abc", "\\d+"));
        assertEquals("Hello   World", RegExUtil.removeFirst("Hello   World   !", "\\s+!"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeFirst("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeFirst("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeFirst("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test removeLast(String, String) with valid regex")
    public void testRemoveLastWithRegex() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals("Hello123World", RegExUtil.removeLast("Hello123World456", "\\d+"));
        assertEquals("Hello123World", RegExUtil.removeLast("Hello123World456", digits));
        assertEquals("", RegExUtil.removeLast(null, "\\d+"));
        assertEquals("", RegExUtil.removeLast(null, digits));
        assertEquals("", RegExUtil.removeLast("", "\\d+"));
        assertEquals("abc", RegExUtil.removeLast("abc", "\\d+"));
        assertEquals("Hello   World", RegExUtil.removeLast("Hello   World   !", "\\s+!"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeLast("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeLast("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeLast("test", (Pattern) null));
    }

    @Test
    public void test_whitespace_pattern() {
        String text = "This is a string  with spaces,\ttabs,\nand newlines.";

        String ret = RegExUtil.removeAll(text, RegExUtil.WHITESPACE_FINDER);

        assertEquals("Thisisastringwithspaces,tabs,andnewlines.", ret);

        ret = RegExUtil.replaceAll(text, RegExUtil.WHITESPACE_FINDER, " ");

        assertEquals("This is a string with spaces, tabs, and newlines.", ret);

        assertFalse(RegExUtil.matches(text, RegExUtil.WHITESPACE_FINDER));
        assertTrue(RegExUtil.find(text, RegExUtil.WHITESPACE_FINDER));

        assertFalse(RegExUtil.matches(text, RegExUtil.WHITESPACE_MATCHER));
        assertFalse(RegExUtil.find(text, RegExUtil.WHITESPACE_MATCHER));

        assertTrue(CommonUtil.equals(new String[] { "a", "b", "c" }, "a b c".split(" ")));
        assertTrue(CommonUtil.equals(new String[] { "a", "b", "c" }, "a b c".split(" ", 0)));
        assertTrue(CommonUtil.equals(new String[] { "a", "b", "c" }, "a b c".split(" ", -1)));
        assertTrue(CommonUtil.equals(new String[] { "a b c" }, "a b c".split(" ", 1)));
        assertTrue(CommonUtil.equals(new String[] { "a", "b c" }, "a b c".split(" ", 2)));
        assertTrue(CommonUtil.equals(new String[] { "a", "b", "c" }, "a b c".split(RegExUtil.WHITESPACE_FINDER.pattern())));
        assertTrue(CommonUtil.equals(new String[] { "a b c" }, "a b c".split(RegExUtil.WHITESPACE_MATCHER.pattern())));
        assertTrue(CommonUtil.equals(new String[] { "" }, "".split(" ")));
        assertTrue(CommonUtil.equals(new String[] { "" }, "".split(RegExUtil.WHITESPACE_FINDER.pattern())));
        assertTrue(CommonUtil.equals(new String[] { "" }, "".split(RegExUtil.WHITESPACE_MATCHER.pattern())));

        assertTrue(CommonUtil.equals(new String[] { "" }, RegExUtil.split("", " ")));
        assertTrue(CommonUtil.equals(new String[] { "" }, RegExUtil.split("", RegExUtil.WHITESPACE_FINDER.pattern())));
        assertTrue(CommonUtil.equals(new String[] { "" }, RegExUtil.split("", RegExUtil.WHITESPACE_MATCHER.pattern())));
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, RegExUtil.split(null, " ")));
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, RegExUtil.split(null, RegExUtil.WHITESPACE_FINDER.pattern())));
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, RegExUtil.split(null, RegExUtil.WHITESPACE_MATCHER.pattern())));

        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Splitter.with(" ").splitToArray(null)));
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Splitter.with(RegExUtil.WHITESPACE_FINDER).splitToArray(null)));
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Splitter.with(RegExUtil.WHITESPACE_MATCHER).splitToArray(null)));

        assertTrue(CommonUtil.equals(new String[] { "" }, Splitter.with(" ").splitToArray("")));
        assertTrue(CommonUtil.equals(new String[] { "" }, Splitter.with(RegExUtil.WHITESPACE_FINDER).splitToArray("")));
        assertTrue(CommonUtil.equals(new String[] { "" }, Splitter.with(RegExUtil.WHITESPACE_MATCHER).splitToArray("")));
    }

    @Test
    @DisplayName("Test removeAll(String, String) with valid regex")
    public void testRemoveAllWithRegex() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals("HelloWorld", RegExUtil.removeAll("Hello123World456", "\\d+"));
        assertEquals("HelloWorld", RegExUtil.removeAll("Hello123World456", digits));
        assertEquals("", RegExUtil.removeAll("123456", "\\d+"));
        assertEquals("HelloWorld", RegExUtil.removeAll("Hello   World", "\\s+"));
        assertEquals("", RegExUtil.removeAll(null, "\\d+"));
        assertEquals("", RegExUtil.removeAll(null, digits));
        assertEquals("", RegExUtil.removeAll("", "\\d+"));
        assertEquals("abc", RegExUtil.removeAll("abc", "\\d+"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeAll("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeAll("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeAll("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test replaceFirst(String, String, String) with valid regex")
    public void testReplaceFirstWithRegex() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals("HelloXXXWorld456", RegExUtil.replaceFirst("Hello123World456", "\\d+", "XXX"));
        assertEquals("HelloXXXWorld456", RegExUtil.replaceFirst("Hello123World456", digits, "XXX"));
        assertEquals("", RegExUtil.replaceFirst(null, "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceFirst(null, digits, "XXX"));
        assertEquals("", RegExUtil.replaceFirst("", "\\d+", "XXX"));
        assertEquals("abc", RegExUtil.replaceFirst("abc", "\\d+", "XXX"));
        assertEquals("Hello_World", RegExUtil.replaceFirst("Hello   World", "\\s+", "_"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("test", (String) null, "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("test", "", "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("test", (Pattern) null, "X"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, String, Function) with function replacer")
    public void testReplaceFirstWithRegexAndFunction() {
        Pattern word = Pattern.compile("\\b\\w");
        assertEquals("Hello world", RegExUtil.replaceFirst("hello world", "\\b\\w", match -> match.toUpperCase()));
        assertEquals("Hello world", RegExUtil.replaceFirst("hello world", word, match -> match.toUpperCase()));
        assertEquals("", RegExUtil.replaceFirst(null, "\\d+", match -> "X"));
        assertEquals("", RegExUtil.replaceFirst("", "\\d+", match -> "X"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, String, IntBiFunction) with function replacer")
    public void testReplaceFirstWithRegexAndIntBiFunction() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals("abc[3-6]def456", RegExUtil.replaceFirst("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("abc[3-6]def456", RegExUtil.replaceFirst("abc123def456", digits, (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("", RegExUtil.replaceFirst(null, "\\d+", (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceFirst("", "\\d+", (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test replaceLast with no match")
    public void testReplaceLastWithNoMatch() {
        assertEquals("HelloWorld", RegExUtil.replaceLast("HelloWorld", "\\d+", "XXX"));
    }

    @Test
    @DisplayName("Test replaceLast(String, String, String) with valid regex")
    public void testReplaceLastWithRegex() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals("Hello123WorldXXX", RegExUtil.replaceLast("Hello123World456", "\\d+", "XXX"));
        assertEquals("Hello123WorldXXX", RegExUtil.replaceLast("Hello123World456", digits, "XXX"));
        assertEquals("", RegExUtil.replaceLast(null, "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceLast("", "\\d+", "XXX"));
        assertEquals("abc", RegExUtil.replaceLast("abc", "\\d+", "XXX"));
        assertEquals("Hello   World_!", RegExUtil.replaceLast("Hello   World   !", "\\s+", "_"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("test", (String) null, "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("test", "", "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("test", (Pattern) null, "X"));
    }

    @Test
    @DisplayName("Test replaceLast(String, String, Function) with function replacer")
    public void testReplaceLastWithRegexAndFunction() {
        Pattern hello = Pattern.compile("hello");
        assertEquals("hello world HELLO", RegExUtil.replaceLast("hello world hello", "hello", match -> match.toUpperCase()));
        assertEquals("hello world HELLO", RegExUtil.replaceLast("hello world hello", hello, match -> match.toUpperCase()));
        assertEquals("", RegExUtil.replaceLast(null, "\\d+", match -> "X"));
        assertEquals("", RegExUtil.replaceLast("", "\\d+", match -> "X"));
    }

    @Test
    @DisplayName("Test replaceLast(String, String, IntBiFunction) with function replacer")
    public void testReplaceLastWithRegexAndIntBiFunction() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals("abc123def[9-12]", RegExUtil.replaceLast("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("abc123def[9-12]", RegExUtil.replaceLast("abc123def456", digits, (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("", RegExUtil.replaceLast(null, "\\d+", (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceLast("", "\\d+", (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test replaceLast with single match")
    public void testReplaceLastWithSingleMatch() {
        assertEquals("HelloXXXWorld", RegExUtil.replaceLast("Hello123World", "\\d+", "XXX"));
    }

    @Test
    @DisplayName("Test greedy vs non-greedy matching")
    public void testGreedyMatching() {
        assertEquals("X", RegExUtil.replaceAll("<tag>content</tag>", "<.*>", "X"));
        assertEquals("Xcontent</tag>", RegExUtil.replaceFirst("<tag>content</tag>", "<.*?>", "X"));
    }

    @Test
    @DisplayName("Test replaceAll(String, String, String) with valid regex")
    public void testReplaceAllWithRegex() {
        Pattern spaces = Pattern.compile("\\s+");
        assertEquals("Hello World", RegExUtil.replaceAll("Hello   World", "\\s+", " "));
        assertEquals("Hello World", RegExUtil.replaceAll("Hello   World", spaces, " "));
        assertEquals("HelloXXXWorldXXX", RegExUtil.replaceAll("Hello123World456", "\\d+", "XXX"));
        assertEquals("X-X-X", RegExUtil.replaceAll("123-456-789", "\\d+", "X"));
        assertEquals("", RegExUtil.replaceAll(null, "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceAll("", "\\d+", "XXX"));
        assertEquals("abc", RegExUtil.replaceAll("abc", "\\d+", "XXX"));
        assertEquals("HelloWorld", RegExUtil.replaceAll("Hello123World", "\\d+", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("test", (String) null, "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("test", "", "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("test", (Pattern) null, "X"));
    }

    @Test
    @DisplayName("Test replaceAll(String, String, Function) with function replacer")
    public void testReplaceAllWithRegexAndFunction() {
        Pattern word = Pattern.compile("\\b\\w");
        assertEquals("Hello World", RegExUtil.replaceAll("hello world", "\\b\\w", match -> match.toUpperCase()));
        assertEquals("Hello World", RegExUtil.replaceAll("hello world", word, match -> match.toUpperCase()));
        assertEquals("2 4 6", RegExUtil.replaceAll("1 2 3", "\\d", match -> String.valueOf(Integer.parseInt(match) * 2)));
        assertEquals("", RegExUtil.replaceAll(null, "\\d+", match -> "X"));
        assertEquals("", RegExUtil.replaceAll("", "\\d+", match -> "X"));
    }

    @Test
    @DisplayName("Test replaceAll(String, String, IntBiFunction) with function replacer")
    public void testReplaceAllWithRegexAndIntBiFunction() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals("abc[3-6]def", RegExUtil.replaceAll("abc123def", "\\d+", (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("abc[3-6]def", RegExUtil.replaceAll("abc123def", digits, (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("a{1}b{3}c{5}", RegExUtil.replaceAll("a1b2c3", "\\d", (start, end) -> "{" + start + "}"));
        assertEquals("", RegExUtil.replaceAll(null, "\\d+", (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceAll("", "\\d+", (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test replaceAll with empty replacement")
    public void testReplaceAllWithEmptyReplacement() {
        assertEquals("HelloWorld", RegExUtil.replaceAll("Hello123World", "\\d+", ""));
    }

    @Test
    @DisplayName("Test multiple consecutive matches")
    public void testMultipleConsecutiveMatches() {
        assertEquals("XXX", RegExUtil.replaceAll("123456789", "\\d+", "XXX"));
        assertEquals(1, RegExUtil.countMatches("123456789", "\\d+"));
    }

    @Test
    @DisplayName("Test empty pattern matches")
    public void testEmptyPattercountMatchBetweenes() {
        String result = RegExUtil.replaceAll("abc", "(?=.)", "X");
        assertEquals("XaXbXc", result);
    }

    @Test
    @DisplayName("Test overlapping patterns")
    public void testOverlappingPatterns() {
        String text = "aaaa";
        assertEquals(4, RegExUtil.countMatches(text, "a"));
        assertEquals(2, RegExUtil.countMatches(text, "aa"));
    }

    @Test
    @DisplayName("Test countMatches(String, String) with valid regex")
    public void testCountMatchesWithRegex() {
        Pattern digits = Pattern.compile("\\d+");
        assertEquals(3, RegExUtil.countMatches("Hello World", "l"));
        assertEquals(2, RegExUtil.countMatches("abc123def456", "\\d+"));
        assertEquals(3, RegExUtil.countMatches("abc123def456ghi789", digits));
        assertEquals(0, RegExUtil.countMatches("abc", "\\d+"));
        assertEquals(0, RegExUtil.countMatches(null, "\\d+"));
        assertEquals(0, RegExUtil.countMatches(null, digits));
        assertEquals(0, RegExUtil.countMatches("", "\\d+"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.countMatches("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.countMatches("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.countMatches("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test matchResults(String, String) with valid regex")
    public void testMatchResultsWithRegex() {
        Stream<MatchResult> matches = RegExUtil.matchResults("abc123def456", "\\d+");
        assertNotNull(matches);
        assertEquals(2, matches.count());

        matches = RegExUtil.matchResults(null, "\\d+");
        assertNotNull(matches);
        assertEquals(0, matches.count());

        matches = RegExUtil.matchResults("", "\\d+");
        assertNotNull(matches);
        assertEquals(0, matches.count());

        Pattern digits = Pattern.compile("\\d+");
        assertEquals(2, RegExUtil.matchResults("abc123def456", digits).count());
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchResults("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchResults("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchResults("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test matchIndices with no matches")
    public void testMatchIndicesWithNoMatches() {
        IntStream indices = RegExUtil.matchIndices("abc", "\\d+");
        assertEquals(0, indices.count());
    }

    @Test
    @DisplayName("Test matchIndices(String, String) with valid regex")
    public void testMatchIndicesWithRegex() {
        IntStream indices = RegExUtil.matchIndices("Hello World", "l");
        assertNotNull(indices);
        assertArrayEquals(new int[] { 2, 3, 9 }, indices.toArray());

        indices = RegExUtil.matchIndices(null, "\\d+");
        assertNotNull(indices);
        assertEquals(0, indices.count());

        indices = RegExUtil.matchIndices("", "\\d+");
        assertNotNull(indices);
        assertEquals(0, indices.count());

        Pattern ell = Pattern.compile("l");
        assertArrayEquals(new int[] { 2, 3, 9 }, RegExUtil.matchIndices("Hello World", ell).toArray());
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchIndices("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchIndices("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchIndices("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test split with no delimiter found")
    public void testSplitWithNoDelimiter() {
        assertArrayEquals(new String[] { "HelloWorld" }, RegExUtil.split("HelloWorld", ","));
    }

    @Test
    @DisplayName("Test split with trailing delimiter")
    public void testSplitWithTrailingDelimiter() {
        String[] result = RegExUtil.split("one,two,three,", ",");
        assertTrue(result.length >= 3);
    }

    @Test
    @DisplayName("Test split(String, String) with valid regex")
    public void testSplitWithRegex() {
        Pattern comma = Pattern.compile(",");
        assertArrayEquals(new String[] { "one", "two", "three" }, RegExUtil.split("one,two,three", ","));
        assertArrayEquals(new String[] { "one", "two", "three" }, RegExUtil.split("one,two,three", comma));
        assertArrayEquals(new String[] { "Hello", "World" }, RegExUtil.split("Hello   World", "\\s+"));
        assertArrayEquals(new String[0], RegExUtil.split(null, ","));
        assertArrayEquals(new String[0], RegExUtil.split(null, comma));
        assertArrayEquals(new String[] { "" }, RegExUtil.split("", ","));
        assertArrayEquals(new String[] { "one", "two", "three,four" }, RegExUtil.split("one,two,three,four", comma, 3));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", (Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", (Pattern) null, 3));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", (String) null, 3));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", "", 3));
    }

    @Test
    @DisplayName("Test split(String, String, int) with limit")
    public void testSplitWithRegexAndLimit() {
        assertArrayEquals(new String[] { "one", "two", "three,four" }, RegExUtil.split("one,two,three,four", ",", 3));
        assertArrayEquals(new String[] { "a", "b" }, RegExUtil.split("a,b,,", ",", 0));
        assertArrayEquals(new String[] { "a", "b", "", "" }, RegExUtil.split("a,b,,", ",", -1));
        assertArrayEquals(new String[0], RegExUtil.split(null, ",", 3));
        assertArrayEquals(new String[] { "" }, RegExUtil.split("", ",", 3));
    }

    @Test
    public void testSplitWithLimit() {
        String[] parts = RegExUtil.split("one,two,three,four", ",", 3);
        Assertions.assertArrayEquals(new String[] { "one", "two", "three,four" }, parts);

        parts = RegExUtil.split("a:b:c:d", ":", -1);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "d" }, parts);

        parts = RegExUtil.split(null, ",", 2);
        Assertions.assertArrayEquals(new String[0], parts);
    }

    @Test
    @DisplayName("Test splitToLines(String) with various line separators")
    public void testSplitToLines() {
        assertArrayEquals(new String[] { "Line 1", "Line 2", "Line 3" }, RegExUtil.splitToLines("Line 1\nLine 2\r\nLine 3"));
        assertArrayEquals(new String[] { "Line 1", "Line 2" }, RegExUtil.splitToLines("Line 1\rLine 2"));
        assertArrayEquals(new String[0], RegExUtil.splitToLines(null));
        assertArrayEquals(new String[] { "" }, RegExUtil.splitToLines(""));
        assertArrayEquals(new String[] { "single line" }, RegExUtil.splitToLines("single line"));
    }

    @Test
    @DisplayName("Test splitToLines(String, int) with limit")
    public void testSplitToLinesWithLimit() {
        assertArrayEquals(new String[] { "Line 1", "Line 2", "Line 3\nLine 4" }, RegExUtil.splitToLines("Line 1\nLine 2\nLine 3\nLine 4", 3));
        assertArrayEquals(new String[] { "Line 1", "Line 2" }, RegExUtil.splitToLines("Line 1\nLine 2\n\n", 0));
        assertArrayEquals(new String[] { "Line 1", "Line 2", "", "" }, RegExUtil.splitToLines("Line 1\nLine 2\n\n", -1));
        assertArrayEquals(new String[0], RegExUtil.splitToLines(null, 3));
        assertArrayEquals(new String[] { "" }, RegExUtil.splitToLines("", 3));
    }

    @Test
    @DisplayName("Test special regex characters")
    public void testSpecialRegexCharacters() {
        assertTrue(RegExUtil.find("test.file", "\\."));
        assertTrue(RegExUtil.find("test(file)", "\\("));
        assertTrue(RegExUtil.find("test[file]", "\\["));
    }

    // -------- Bug-fix regression tests (replaceLast where rightmost match starts at 0 / extends to position 0) --------

    @Test
    @DisplayName("replaceLast: leftmost equivalent of rightmost match extends to index 0 (single match)")
    public void testReplaceLast_matchAtStart_singleMatch() {
        // The only match starts at index 0; before the fix, this returned the source unchanged.
        assertEquals("Xhello", RegExUtil.replaceLast("123hello", "\\d+", "X"));
        assertEquals("Xhello", RegExUtil.replaceLast("123hello", Pattern.compile("\\d+"), "X"));
        assertEquals("Yxy", RegExUtil.replaceLast("0xy", "\\d+", "Y"));
    }

    @Test
    @DisplayName("replaceLast: rightmost greedy match extends back to index 0")
    public void testReplaceLast_rightmostMatchExtendsToZero() {
        // \w+ matches the whole string; before the fix, the loop terminated without
        // applying replacement because matcher.start() never became < 0.
        assertEquals("X", RegExUtil.replaceLast("Hello", "\\w+", "X"));
        assertEquals("X", RegExUtil.replaceLast("Hello", Pattern.compile("\\w+"), "X"));
    }

    @Test
    @DisplayName("replaceLast with Function/IntBiFunction handles match starting at index 0")
    public void testReplaceLast_matchAtStart_withReplacers() {
        assertEquals("[123]hello", RegExUtil.replaceLast("123hello", "\\d+", m -> "[" + m + "]"));
        assertEquals("[0,3]hello", RegExUtil.replaceLast("123hello", "\\d+", (s, e) -> "[" + s + "," + e + "]"));
        assertEquals("[123]hello", RegExUtil.replaceLast("123hello", Pattern.compile("\\d+"), m -> "[" + m + "]"));
        assertEquals("[0,3]hello", RegExUtil.replaceLast("123hello", Pattern.compile("\\d+"), (s, e) -> "[" + s + "," + e + "]"));
    }

    @Test
    @DisplayName("replaceLast: existing behavior preserved when match doesn't extend to index 0")
    public void testReplaceLast_regressionForOtherCases() {
        assertEquals("Hello123WorldX", RegExUtil.replaceLast("Hello123World456", "\\d+", "X"));
        assertEquals("HelloWorld", RegExUtil.replaceLast("HelloWorld", "\\d+", "X"));
        assertEquals("HelloXWorld", RegExUtil.replaceLast("Hello123World", "\\d+", "X"));
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testFunctionReplacersAreLiteral() {
        // regression: function-replacer results went through Matcher template interpretation,
        // so '$'/'\' in the result crashed (e.g. "No group 5") or was misinterpreted
        assertEquals("PRICE $5", RegExUtil.replaceAll("price $5", Pattern.compile("price \\$\\d"), (String m) -> m.toUpperCase()));
        assertEquals("price $5", RegExUtil.replaceAll("price $5", Pattern.compile("\\$\\d"), (String m) -> m));
        assertEquals("a\\b", RegExUtil.replaceAll("a\\b", Pattern.compile("\\\\"), (String m) -> m));
        assertEquals("cost $9 now", RegExUtil.replaceFirst("cost $9 now", Pattern.compile("\\$\\d"), (String m) -> m));
        assertEquals("a$bc", RegExUtil.replaceAll("abc", Pattern.compile("b"), (start, end) -> "$b"));
    }

    @Test
    public void testReplaceLastMatchesForwardIterationSemantics() {
        // regression: reverse find(i) probing could replace a shorter sub-match that forward
        // iteration (findLast's semantics) would never report
        assertEquals("X", RegExUtil.replaceLast("ababab", Pattern.compile("(ab)+"), "X"));
        assertEquals("X", RegExUtil.replaceLast("aba", Pattern.compile("aba|a"), "X"));
        assertEquals("Hello123WorldX", RegExUtil.replaceLast("Hello123World456", "\\d+", "X"));
    }

    @Test
    public void testJavaIdentifierPatternsSupportUnicodeIdentifiers() {
        assertTrue(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("\u53d8\u91cf2").matches());
        assertTrue(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("\u03c0Value").matches());
        assertTrue(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("\uD835\uDC9Cvalue").matches());
        assertEquals("\u53d8\u91cf2", RegExUtil.findFirst("123\u53d8\u91cf2!", RegExUtil.JAVA_IDENTIFIER_FINDER));
        assertFalse(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("\uD83D\uDE00value").matches());
    }

    @Test
    public void testWholeInputPatternsDoNotAcceptTrailingLineTerminators() {
        assertFalse(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("identifier\n").find());
        assertFalse(RegExUtil.INTEGER_MATCHER.matcher("123\r\n").find());
        assertFalse(RegExUtil.DATE_MATCHER.matcher("2026-07-16\n").find());
        assertFalse(RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher("USER@EXAMPLE.COM\n").find());
        assertFalse(RegExUtil.HTTP_URL_MATCHER.matcher("https://example.com\n").find());

        // The helper that adds strict anchors must retain the finder's flags.
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher("USER@EXAMPLE.COM").matches());
    }

    @Test
    public void testDuplicatePatternsHandleWholeInputMultilineAndUnicodeText() {
        assertTrue(RegExUtil.DUPLICATES_MATCHER.matcher("alpha beta alpha").matches());
        assertTrue(RegExUtil.DUPLICATES_MATCHER.matcher("alpha beta alpha").find());
        assertTrue(RegExUtil.DUPLICATES_FINDER.matcher("\u732b\n\u72d7\n\u732b").find());
        assertTrue(RegExUtil.DUPLICATES_MATCHER.matcher("\u732b\n\u72d7\n\u732b").matches());
        assertFalse(RegExUtil.DUPLICATES_MATCHER.matcher("alpha beta gamma").matches());
        assertFalse(RegExUtil.DUPLICATES_MATCHER.matcher("Alpha beta alpha").matches());
    }

    @Test
    public void testStringRegexIsValidatedForNullOrEmptySources() {
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.findAll(null, "["));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.replaceFirst(null, "[", "x"));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.replaceFirst("", "[", match -> "x"));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.replaceFirst("", "[", (start, end) -> "x"));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.replaceLast(null, "[", "x"));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.replaceLast("", "[", match -> "x"));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.replaceLast("", "[", (start, end) -> "x"));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.countMatches(null, "["));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.matchResults("", "["));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.matchIndices(null, "["));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.split(null, "["));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.split("", "[", 2));
    }

    @Test
    public void testFunctionalReplacersAreNotEvaluatedForEmptySources() {
        final Pattern pattern = Pattern.compile("x");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("", "x", (Function<String, String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("", "x", (IntBiFunction<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> RegExUtil.replaceFirst("", pattern, (Function<String, String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("", pattern, (IntBiFunction<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("", "x", (Function<String, String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("", "x", (IntBiFunction<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> RegExUtil.replaceLast("", pattern, (Function<String, String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("", pattern, (IntBiFunction<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("", "x", (Function<String, String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("", "x", (IntBiFunction<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("", pattern, (Function<String, String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("", pattern, (IntBiFunction<String>) null));
    }

    @Test
    public void testNullFunctionalReplacementResultsRemoveMatchesConsistently() {
        assertEquals("ab2", RegExUtil.replaceFirst("a1b2", "\\d", match -> null));
        assertEquals("ab2", RegExUtil.replaceFirst("a1b2", "\\d", (start, end) -> null));
        assertEquals("a1b", RegExUtil.replaceLast("a1b2", "\\d", match -> null));
        assertEquals("a1b", RegExUtil.replaceLast("a1b2", "\\d", (start, end) -> null));
        assertEquals("ab", RegExUtil.replaceAll("a1b2", "\\d", match -> null));
        assertEquals("ab", RegExUtil.replaceAll("a1b2", "\\d", (start, end) -> null));
    }

    @Test
    @DisplayName("a null replacer is rejected before matching")
    public void testNullReplacerIsRejectedEagerly() {
        final Pattern digit = Pattern.compile("\\d");
        final Function<String, String> nullFunction = null;
        final IntBiFunction<String> nullIntBiFunction = null;

        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("a1", digit, nullFunction));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("a1", digit, nullIntBiFunction));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("a1", digit, nullFunction));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("a1", digit, nullIntBiFunction));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1", digit, nullFunction));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1", digit, nullIntBiFunction));
    }

    // -------- 2026-09-06 review fixes --------

    @Test
    @DisplayName("replaceFirst/replaceAll interpret the replacement as a template; replaceLast and the functional overloads do not")
    public void reviewFixes20260906_templateVsLiteralReplacementSplit() {
        final Pattern b = Pattern.compile("b");

        // template: $0 is the whole match, and a lone backslash escapes the next char away
        assertEquals("abbc", RegExUtil.replaceAll("abc", "b", "$0$0"));
        assertEquals("abbc", RegExUtil.replaceAll("abc", b, "$0$0"));
        assertEquals("abbc", RegExUtil.replaceFirst("abc", "b", "$0$0"));
        assertEquals("abbc", RegExUtil.replaceFirst("abc", b, "$0$0"));
        assertEquals("aC:xc", RegExUtil.replaceAll("abc", "b", "C:\\x"));
        assertEquals("aC:xc", RegExUtil.replaceFirst("abc", "b", "C:\\x"));
        assertEquals("a[1]b[2]", RegExUtil.replaceAll("a1b2", "(\\d)", "[$1]"));

        // template: a reference to a group the regex does not have is an IndexOutOfBoundsException, not a literal
        assertThrows(IndexOutOfBoundsException.class, () -> RegExUtil.replaceAll("abc", "b", "$1"));
        assertThrows(IndexOutOfBoundsException.class, () -> RegExUtil.replaceAll("abc", b, "$1"));
        assertThrows(IndexOutOfBoundsException.class, () -> RegExUtil.replaceFirst("abc", "b", "$1"));
        assertThrows(IndexOutOfBoundsException.class, () -> RegExUtil.replaceFirst("abc", b, "$1"));

        // literal: replaceLast splices the string in verbatim
        assertEquals("a$0$0c", RegExUtil.replaceLast("abc", "b", "$0$0"));
        assertEquals("a$1c", RegExUtil.replaceLast("abc", "b", "$1"));
        assertEquals("aC:\\xc", RegExUtil.replaceLast("abc", "b", "C:\\x"));

        // literal: every functional overload quotes the replacer's result (Matcher.quoteReplacement)
        assertEquals("a$0$0c", RegExUtil.replaceAll("abc", "b", match -> "$0$0"));
        assertEquals("a$0$0c", RegExUtil.replaceAll("abc", "b", (start, end) -> "$0$0"));
        assertEquals("a$0$0c", RegExUtil.replaceFirst("abc", "b", match -> "$0$0"));
        assertEquals("a$0$0c", RegExUtil.replaceFirst("abc", "b", (start, end) -> "$0$0"));
        assertEquals("a$0$0c", RegExUtil.replaceLast("abc", "b", match -> "$0$0"));
        assertEquals("a$0$0c", RegExUtil.replaceLast("abc", "b", (start, end) -> "$0$0"));
        assertEquals("aC:\\xc", RegExUtil.replaceAll("abc", "b", match -> "C:\\x"));
        assertEquals("aC:\\xc", RegExUtil.replaceAll("abc", b, (start, end) -> "C:\\x"));

        // control: a replacement with neither $ nor \ behaves the same in all three families
        assertEquals("aXc", RegExUtil.replaceAll("abc", "b", "X"));
        assertEquals("aXc", RegExUtil.replaceFirst("abc", "b", "X"));
        assertEquals("aXc", RegExUtil.replaceLast("abc", "b", "X"));
    }

    @Test
    @DisplayName("PHONE_NUMBER_WITH_CODE needs eleven [\\\\d\\\\s] characters, so a bare 10-digit number does not match")
    public void reviewFixes20260906_phoneNumberWithCodeNeedsElevenCharacters() {
        assertFalse(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("5551234567").matches());
        assertFalse(RegExUtil.PHONE_NUMBER_WITH_CODE_FINDER.matcher("5551234567").find());

        // one more character - a digit or a separator - is enough
        assertTrue(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("55512345678").matches());
        assertTrue(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("555 123 4567").matches());

        // control: the documented example matches must keep matching
        assertTrue(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("+1 234 567 8900").matches());
        assertTrue(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("+44 20 1234 5678").matches());
        assertFalse(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("(123) 456 7890").matches());
        assertFalse(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher("+1-234-567-8900").matches());
    }

    @Test
    @DisplayName("NUMBER_* accepts a trailing dot; POSITIVE_/NEGATIVE_NUMBER_* deliberately does not")
    public void reviewFixes20260906_trailingDotSplitsTheNumberFamilies() {
        assertTrue(RegExUtil.NUMBER_MATCHER.matcher("-100.").matches());
        assertTrue(RegExUtil.NUMBER_MATCHER.matcher("1.").matches());
        assertFalse(RegExUtil.NEGATIVE_NUMBER_MATCHER.matcher("-100.").matches());
        assertFalse(RegExUtil.POSITIVE_NUMBER_MATCHER.matcher("100.").matches());

        // the finders stop at the last digit
        assertEquals(CommonUtil.asList("-100"), RegExUtil.findAll("-100.", RegExUtil.NEGATIVE_NUMBER_FINDER));
        assertEquals(CommonUtil.asList("100"), RegExUtil.findAll("100.", RegExUtil.POSITIVE_NUMBER_FINDER));
        assertEquals(CommonUtil.asList("-100."), RegExUtil.findAll("-100.", RegExUtil.NUMBER_FINDER));

        // control: the documented example matches are unaffected
        assertTrue(RegExUtil.NEGATIVE_NUMBER_MATCHER.matcher("-.25").matches());
        assertTrue(RegExUtil.NEGATIVE_NUMBER_MATCHER.matcher("-3.14").matches());
        assertTrue(RegExUtil.POSITIVE_NUMBER_MATCHER.matcher(".25").matches());
        assertTrue(RegExUtil.POSITIVE_NUMBER_MATCHER.matcher("3.14").matches());
    }

    @Test
    @DisplayName("the no-limit split/splitToLines overloads discard trailing empty results")
    public void reviewFixes20260906_noLimitSplitDiscardsTrailingEmpties() {
        final Pattern comma = Pattern.compile(",");

        assertArrayEquals(new String[] { "a", "b" }, RegExUtil.split("a,b,,", ","));
        assertArrayEquals(new String[] { "a", "b" }, RegExUtil.split("a,b,,", comma));
        assertArrayEquals(new String[] { "a", "b", "", "" }, RegExUtil.split("a,b,,", ",", -1));
        assertArrayEquals(new String[] { "a", "b", "", "" }, RegExUtil.split("a,b,,", comma, -1));

        // a source made only of line terminators splits to a ZERO-length array, not to one empty line
        assertEquals(0, RegExUtil.splitToLines("\n").length);
        assertArrayEquals(new String[] { "a", "b" }, RegExUtil.splitToLines("a\nb\n"));
        assertArrayEquals(new String[] { "a", "b", "" }, RegExUtil.splitToLines("a\nb\n", -1));

        // control: the documented null/empty short-circuits are unchanged
        assertArrayEquals(new String[0], RegExUtil.split(null, ","));
        assertArrayEquals(new String[] { "" }, RegExUtil.split("", ","));
        assertArrayEquals(new String[0], RegExUtil.splitToLines(null));
        assertArrayEquals(new String[] { "" }, RegExUtil.splitToLines(""));
    }

    @Test
    @DisplayName("a null source never matches, in every method (F77)")
    public void reviewFixes20260908_nullSourceNeverMatchesInAnyMethod() {
        final Pattern aStar = Pattern.compile("a*");

        // find/matches/findFirst/findLast used to normalize null to "" and match it, so find(null, "a*")
        // answered true while findAll/countMatches/matchResults/matchIndices answered "no match".
        assertFalse(RegExUtil.find(null, "a*"));
        assertFalse(RegExUtil.find(null, aStar));
        assertFalse(RegExUtil.matches(null, "a*"));
        assertFalse(RegExUtil.matches(null, aStar));
        assertNull(RegExUtil.findFirst(null, "a*"));
        assertNull(RegExUtil.findFirst(null, aStar));
        assertNull(RegExUtil.findLast(null, "a*"));
        assertNull(RegExUtil.findLast(null, aStar));

        // ... and the rest of the class already answered that way; now the whole class agrees.
        assertEquals(CommonUtil.asList(), RegExUtil.findAll(null, aStar));
        assertEquals(0, RegExUtil.countMatches(null, aStar));
        assertEquals(0, RegExUtil.matchResults(null, aStar).count());
        assertEquals(0, RegExUtil.matchIndices(null, aStar).count());
        assertEquals("", RegExUtil.replaceAll(null, aStar, "X"));
        assertEquals(0, RegExUtil.split(null, aStar).length);

        // An EMPTY (non-null) source is still matched normally by every method.
        assertTrue(RegExUtil.find("", aStar));
        assertTrue(RegExUtil.matches("", aStar));
        assertEquals("", RegExUtil.findFirst("", aStar));
        assertEquals("", RegExUtil.findLast("", aStar));
        assertEquals(1, RegExUtil.countMatches("", aStar));

        // A String regex is still compiled (and so validated) before the null source short-circuits.
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.find(null, "["));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.matches(null, "["));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.findFirst(null, "["));
        assertThrows(PatternSyntaxException.class, () -> RegExUtil.findLast(null, "["));

        // ... and the argument checks still run first.
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.find(null, (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches(null, (Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst(null, (Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast(null, (Pattern) null));
    }

    /**
     * Counts every {@code charAt} the regex engine performs, which makes backtracking blow-up observable
     * without timing anything.
     */
    private static final class CountingCharSequence implements CharSequence {
        private final String text;
        private long reads;

        CountingCharSequence(final String text) {
            this.text = text;
        }

        @Override
        public char charAt(final int index) {
            reads++;
            return text.charAt(index);
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            return text.subSequence(start, end);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    @Test
    @DisplayName("POSITIVE_/NEGATIVE_NUMBER_MATCHER reject a long non-number in linear time, not quadratic")
    public void reviewFixes20260911_numberMatchersAreLinearNotQuadratic() {
        // \d*\.?\d+ put two overlapping \d quantifiers around an OPTIONAL \., so every one of the n+1
        // split points for \d* was retried against the whole remaining suffix: ~n*n/2 steps to reject.
        final int n = 2000;
        final long linearBudget = 20L * n;

        final CountingCharSequence positive = new CountingCharSequence("9".repeat(n) + "x");
        assertFalse(RegExUtil.POSITIVE_NUMBER_MATCHER.matcher(positive).matches());
        assertTrue(positive.reads <= linearBudget, "POSITIVE_NUMBER_MATCHER read " + positive.reads + " characters for a " + n + "-character input");

        // the documented rejection case ("100." must not match) is the same shape
        final CountingCharSequence trailingDot = new CountingCharSequence("9".repeat(n) + ".");
        assertFalse(RegExUtil.POSITIVE_NUMBER_MATCHER.matcher(trailingDot).matches());
        assertTrue(trailingDot.reads <= linearBudget, "POSITIVE_NUMBER_MATCHER read " + trailingDot.reads + " characters for a trailing-dot input");

        final CountingCharSequence negative = new CountingCharSequence("-" + "9".repeat(n) + "x");
        assertFalse(RegExUtil.NEGATIVE_NUMBER_MATCHER.matcher(negative).matches());
        assertTrue(negative.reads <= linearBudget, "NEGATIVE_NUMBER_MATCHER read " + negative.reads + " characters for a " + n + "-character input");

        // control: the linear siblings were always this cheap, and still are
        final CountingCharSequence number = new CountingCharSequence("9".repeat(n) + "x");
        assertFalse(RegExUtil.NUMBER_MATCHER.matcher(number).matches());
        assertTrue(number.reads <= linearBudget);
    }

    @Test
    @DisplayName("the two PHONE_NUMBER_* constants are still quadratic, exactly as their Performance warning says")
    public void reviewFixes20260911_phoneFindersAreQuadraticAsDocumented() {
        // Characterisation, not an aspiration: these two patterns are PRE-EXISTING and were deliberately not
        // rewritten, only documented. If a later change makes one of them linear, this test goes red - and the
        // <b>Performance:</b> paragraph on that constant must be removed in the same change.
        final int n = 400;
        final long linearBudget = 20L * n;

        // the lookahead re-runs from its own start at every candidate position
        final CountingCharSequence phone = new CountingCharSequence(" ".repeat(n) + "x");
        assertFalse(RegExUtil.PHONE_NUMBER_FINDER.matcher(phone).find());
        assertTrue(phone.reads > linearBudget, "PHONE_NUMBER_FINDER is no longer superlinear (" + phone.reads + " reads): drop its Performance warning");

        final CountingCharSequence withCode = new CountingCharSequence(" ".repeat(n) + "x");
        assertFalse(RegExUtil.PHONE_NUMBER_WITH_CODE_FINDER.matcher(withCode).find());
        assertTrue(withCode.reads > linearBudget,
                "PHONE_NUMBER_WITH_CODE_FINDER is no longer superlinear (" + withCode.reads + " reads): drop its Performance warning");

        // the second shape: [\d\s]+ and [\d\s]{10,} draw from the same class, so a failing whole-string match
        // has to try every split of the run between them
        final CountingCharSequence split = new CountingCharSequence("1".repeat(n) + "x");
        assertFalse(RegExUtil.PHONE_NUMBER_WITH_CODE_MATCHER.matcher(split).matches());
        assertTrue(split.reads > linearBudget, "PHONE_NUMBER_WITH_CODE_MATCHER no longer retries every split (" + split.reads + " reads)");

        // control: the anchored PHONE_NUMBER_MATCHER has one start position and IS linear, as documented
        final CountingCharSequence anchored = new CountingCharSequence("1".repeat(n) + "x");
        assertFalse(RegExUtil.PHONE_NUMBER_MATCHER.matcher(anchored).matches());
        assertTrue(anchored.reads <= linearBudget, "PHONE_NUMBER_MATCHER read " + anchored.reads + " characters");

        // both patterns still do their documented job on real input
        assertTrue(RegExUtil.PHONE_NUMBER_FINDER.matcher("123 456 7890").find());
        assertTrue(RegExUtil.PHONE_NUMBER_WITH_CODE_FINDER.matcher("+1 234 567 8900").find());
    }

    @Test
    @DisplayName("the rewritten POSITIVE_/NEGATIVE_NUMBER bodies still describe exactly the same language")
    public void reviewFixes20260911_numberPatternsKeepTheirLanguage() {
        // every documented example match
        for (final String s : new String[] { "42", "3.14", "0.99", ".25" }) {
            assertTrue(RegExUtil.POSITIVE_NUMBER_MATCHER.matcher(s).matches(), s);
            assertFalse(RegExUtil.NEGATIVE_NUMBER_MATCHER.matcher(s).matches(), s);
        }
        for (final String s : new String[] { "-7", "-3.14", "-0.99", "-.25" }) {
            assertTrue(RegExUtil.NEGATIVE_NUMBER_MATCHER.matcher(s).matches(), s);
            assertFalse(RegExUtil.POSITIVE_NUMBER_MATCHER.matcher(s).matches(), s);
        }

        // the documented non-matches, including the trailing-dot split
        for (final String s : new String[] { "", ".", "-", "-.", "abc", "100.", "-100.", "1.2.3", "1..2", "..1", "--1", "+1" }) {
            assertFalse(RegExUtil.POSITIVE_NUMBER_MATCHER.matcher(s).matches(), s);
            assertFalse(RegExUtil.NEGATIVE_NUMBER_MATCHER.matcher(s).matches(), s);
        }

        // find() mode keeps the same offsets and the same matched text
        assertEquals(CommonUtil.asList("100"), RegExUtil.findAll("100.", RegExUtil.POSITIVE_NUMBER_FINDER));
        assertEquals(CommonUtil.asList("-100"), RegExUtil.findAll("-100.", RegExUtil.NEGATIVE_NUMBER_FINDER));
        assertEquals(CommonUtil.asList("1", "2", "3"), RegExUtil.findAll("1.x2.y3", RegExUtil.POSITIVE_NUMBER_FINDER));
        assertEquals(CommonUtil.asList("12.5"), RegExUtil.findAll("a12.5b", RegExUtil.POSITIVE_NUMBER_FINDER));
        assertEquals(CommonUtil.asList("-5.5"), RegExUtil.findAll("Temperature: -5.5", RegExUtil.NEGATIVE_NUMBER_FINDER));
        assertEquals(".25", RegExUtil.findFirst("x=.25", RegExUtil.POSITIVE_NUMBER_FINDER));

        // neither body may introduce a capturing group, and neither may change meaning when spliced into a
        // larger expression - RegExUtil itself splices a FINDER body (see DUPLICATES_MATCHER).
        assertEquals(0, RegExUtil.POSITIVE_NUMBER_FINDER.matcher("").groupCount());
        assertEquals(0, RegExUtil.NEGATIVE_NUMBER_FINDER.matcher("").groupCount());
        assertTrue(Pattern.compile("x" + RegExUtil.POSITIVE_NUMBER_FINDER.pattern() + "y").matcher("x12.5y").matches());
        assertTrue(Pattern.compile("x" + RegExUtil.NEGATIVE_NUMBER_FINDER.pattern() + "y").matcher("x-12.5y").matches());
    }

    @Test
    @DisplayName("JAVA_IDENTIFIER_* is syntactic: a Java keyword MATCHES it")
    public void reviewFixes20260911_javaIdentifierAcceptsKeywords() {
        // "class" was listed under the javadoc's "Example non-matches" although the bullet itself said it matches.
        assertTrue(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("class").matches());
        assertTrue(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("int").matches());
        assertEquals("class", RegExUtil.findFirst("class", RegExUtil.JAVA_IDENTIFIER_FINDER));

        // the two genuine whole-token non-matches it is listed beside
        assertFalse(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("123invalid").matches());
        assertEquals("invalid", RegExUtil.findFirst("123invalid", RegExUtil.JAVA_IDENTIFIER_FINDER));
        assertFalse(RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher("my-variable").matches());
        assertEquals(CommonUtil.asList("my", "variable"), RegExUtil.findAll("my-variable", RegExUtil.JAVA_IDENTIFIER_FINDER));
    }

    @Test
    @DisplayName("WHITESPACE_* and ALPHANUMERIC_SPACE_* are ASCII-only, unlike the Unicode-aware LINE_SEPARATOR")
    public void reviewFixes20260911_whitespaceFinderIsAsciiOnly() {
        // \s without UNICODE_CHARACTER_CLASS is exactly [ \t\n\x0B\f\r]
        for (final String s : new String[] { " ", "\t", "\n", "\u000B", "\f", "\r", "  \t\r\n" }) {
            assertTrue(RegExUtil.WHITESPACE_MATCHER.matcher(s).matches(), "expected ASCII whitespace: " + s);
        }
        for (final String s : new String[] { "\u00A0", "\u2028", "\u2029", "\u0085", "\u2003", "\u3000" }) {
            assertFalse(RegExUtil.WHITESPACE_MATCHER.matcher(s).matches(), "expected NOT matched: " + s);
            assertFalse(RegExUtil.WHITESPACE_FINDER.matcher(s).find(), "expected NOT found: " + s);
            assertFalse(RegExUtil.ALPHANUMERIC_SPACE_MATCHER.matcher("a" + s + "b").matches(), "expected NOT matched: " + s);
        }

        // ... so a WHITESPACE_FINDER normalisation leaves them untouched
        assertEquals("a\u00A0b", RegExUtil.replaceAll("a\u00A0b", RegExUtil.WHITESPACE_FINDER, " "));
        assertEquals("a b", RegExUtil.replaceAll("a\t\tb", RegExUtil.WHITESPACE_FINDER, " "));

        // ... and the remedy the javadoc prescribes really is a remedy: UNICODE_CHARACTER_CLASS matches them.
        // U+200B/U+FEFF stay unmatched on purpose - neither is a Unicode space separator.
        final Pattern unicodeWhitespace = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

        for (final String s : new String[] { "\u00A0", "\u2028", "\u2029", "\u0085", "\u2003", "\u202F", "\u3000" }) {
            assertTrue(unicodeWhitespace.matcher(s).matches(), "expected UNICODE_CHARACTER_CLASS match: " + s);
        }

        assertFalse(unicodeWhitespace.matcher("\u200B").matches());
        assertFalse(unicodeWhitespace.matcher("\uFEFF").matches());

        // ... while LINE_SEPARATOR (\R) IS Unicode-aware, which is the contrast the javadoc now names
        assertTrue(RegExUtil.LINE_SEPARATOR.matcher("\u2028").find());
        assertTrue(RegExUtil.LINE_SEPARATOR.matcher("\u0085").find());
        assertArrayEquals(new String[] { "a", "b" }, RegExUtil.splitToLines("a\u2028b"));
    }

    @Test
    @DisplayName("a malformed $ in a String replacement raises IllegalArgumentException, not IndexOutOfBounds")
    public void reviewFixes20260911_malformedGroupReferenceRaisesIllegalArgument() {
        // a trailing $ / a $ not followed by a digit - IllegalArgumentException out of Matcher's replacement parser
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("ab", "a", "c$"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("ab", Pattern.compile("a"), "c$"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("ab", "a", "c$"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("ab", Pattern.compile("a"), "c$"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("ab", "a", "c$x"));

        // ... whereas a WELL-FORMED reference to a group the pattern does not have is IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> RegExUtil.replaceAll("ab", "a", "$1"));
        assertThrows(IndexOutOfBoundsException.class, () -> RegExUtil.replaceFirst("ab", "a", "$1"));

        // a ${name} reference is a $ NOT followed by a digit, and is perfectly legal when the group exists -
        // which is why the @throws wording says "neither a digit nor a well-formed {name}" rather than "not a digit"
        assertEquals("[a]b", RegExUtil.replaceAll("ab", "(?<g>a)", "[${g}]"));
        assertEquals("[a]b", RegExUtil.replaceAll("ab", Pattern.compile("(?<g>a)"), "[${g}]"));
        assertEquals("[a]b", RegExUtil.replaceFirst("ab", "(?<g>a)", "[${g}]"));
        assertEquals("[a]b", RegExUtil.replaceFirst("ab", Pattern.compile("(?<g>a)"), "[${g}]"));

        // ... but a ${name} for a group the pattern does not declare is IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("ab", "(?<g>a)", "${nope}"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("ab", "(a)", "${g}"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("ab", "(?<g>a)", "${}"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("ab", "(?<g>a)", "${g_1}"));

        // a trailing backslash comes out of the same replacement parser as the same IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("ab", "a", "c\\"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("ab", "a", "c\\"));
        assertEquals("c$b", RegExUtil.replaceAll("ab", "a", "c\\$"));

        // the replacement is parsed ONLY when a match is found, which is why the tag is qualified "if a match is
        // found": every malformed replacement above returns the source unchanged against a non-matching source
        assertEquals("zzz", RegExUtil.replaceAll("zzz", "(a)", "c$"));
        assertEquals("zzz", RegExUtil.replaceFirst("zzz", "(a)", "c$"));
        assertEquals("zzz", RegExUtil.replaceAll("zzz", "a", "c\\"));
        assertEquals("zzz", RegExUtil.replaceAll("zzz", "(?<g>a)", "${nope}"));
        assertEquals("zzz", RegExUtil.replaceFirst("zzz", "a", "$1"));
        assertEquals("", RegExUtil.replaceAll(null, "(a)", "c$"));

        // control: the functional overloads quote the replacer's output, so neither can be raised there
        assertEquals("c$b", RegExUtil.replaceAll("ab", "a", match -> "c$"));
        assertEquals("$1b", RegExUtil.replaceAll("ab", "(a)", match -> "$1"));
    }
}
