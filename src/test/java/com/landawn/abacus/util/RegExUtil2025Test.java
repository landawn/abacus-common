package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class RegExUtil2025Test extends TestBase {

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
    @DisplayName("Test NUMBER_FINDER pattern")
    public void testNumberFinder() {
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("123").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("123.45").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("-123.45").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("+0.99").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher("100.").find());
        assertTrue(RegExUtil.NUMBER_FINDER.matcher(".25").find());
        assertFalse(RegExUtil.NUMBER_FINDER.matcher("abc").find());
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
    @DisplayName("Test SCIENTIFIC_NUMBER_FINDER pattern")
    public void testScientificNumberFinder() {
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("1.5e10").find());
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("-2E-5").find());
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("+6.022e23").find());
        assertTrue(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("42").find());
        assertFalse(RegExUtil.SCIENTIFIC_NUMBER_FINDER.matcher("abc").find());
    }

    @Test
    @DisplayName("Test PHONE_NUMBER_FINDER pattern")
    public void testPhoneNumberFinder() {
        assertTrue(RegExUtil.PHONE_NUMBER_FINDER.matcher("123 456 7890").find());
        assertTrue(RegExUtil.PHONE_NUMBER_FINDER.matcher("+1 234 567 8900").find());
        assertTrue(RegExUtil.PHONE_NUMBER_FINDER.matcher("555").find());
        assertFalse(RegExUtil.PHONE_NUMBER_FINDER.matcher("ab").find());
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
    @DisplayName("Test EMAIL_ADDRESS_RFC_5322_FINDER pattern")
    public void testEmailAddressFinder() {
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("test@example.com").find());
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("user.name@example.co.uk").find());
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("user+tag@example.com").find());
        assertFalse(RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("invalid@").find());
    }

    @Test
    @DisplayName("Test URL_FINDER pattern")
    public void testUrlFinder() {
        assertTrue(RegExUtil.URL_FINDER.matcher("https://www.example.com").find());
        assertTrue(RegExUtil.URL_FINDER.matcher("ftp://ftp.example.com/file").find());
        assertTrue(RegExUtil.URL_FINDER.matcher("http://localhost:8080/api").find());
        assertFalse(RegExUtil.URL_FINDER.matcher("not a url").find());
    }

    @Test
    @DisplayName("Test HTTP_URL_FINDER pattern")
    public void testHttpUrlFinder() {
        assertTrue(RegExUtil.HTTP_URL_FINDER.matcher("http://www.example.com").find());
        assertTrue(RegExUtil.HTTP_URL_FINDER.matcher("https://api.example.com:8443/v1/users?id=123").find());
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
        assertFalse(RegExUtil.NUMBER_MATCHER.matcher("abc123").matches());
    }

    @Test
    @DisplayName("Test EMAIL_ADDRESS_RFC_5322_MATCHER pattern")
    public void testEmailMatcher() {
        assertTrue(RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher("test@example.com").matches());
        assertFalse(RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher("not an email").matches());
    }

    @Test
    @DisplayName("Test find(String, String) with valid regex")
    public void testFindWithRegex() {
        assertTrue(RegExUtil.find("Hello123World", "\\d+"));
        assertTrue(RegExUtil.find("test@example.com", "\\w+@\\w+\\.\\w+"));
        assertFalse(RegExUtil.find("Hello World", "\\d+"));
        assertFalse(RegExUtil.find(null, "\\d+"));
        assertFalse(RegExUtil.find("", "\\d+"));
    }

    @Test
    @DisplayName("Test find(String, String) with null/empty regex throws exception")
    public void testFindWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", ""));
    }

    @Test
    @DisplayName("Test find(String, Pattern) with valid pattern")
    public void testFindWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        assertTrue(RegExUtil.find("Hello123World", pattern));
        assertFalse(RegExUtil.find("Hello World", pattern));
        assertFalse(RegExUtil.find(null, pattern));
        assertFalse(RegExUtil.find("", pattern));
    }

    @Test
    @DisplayName("Test find(String, Pattern) with null pattern throws exception")
    public void testFindWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.find("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test matches(String, String) with valid regex")
    public void testMatchesWithRegex() {
        assertTrue(RegExUtil.matches("123", "\\d+"));
        assertTrue(RegExUtil.matches("abc", "[a-z]+"));
        assertFalse(RegExUtil.matches("abc123", "\\d+"));
        assertFalse(RegExUtil.matches(null, "\\d+"));
        assertFalse(RegExUtil.matches("", "\\d+"));
    }

    @Test
    @DisplayName("Test matches(String, String) with null/empty regex throws exception")
    public void testMatchesWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches("test", ""));
    }

    @Test
    @DisplayName("Test matches(String, Pattern) with valid pattern")
    public void testMatchesWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        assertTrue(RegExUtil.matches("123", pattern));
        assertFalse(RegExUtil.matches("abc123", pattern));
        assertFalse(RegExUtil.matches(null, pattern));
        assertFalse(RegExUtil.matches("", pattern));
    }

    @Test
    @DisplayName("Test matches(String, Pattern) with null pattern throws exception")
    public void testMatchesWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matches("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test removeAll(String, String) with valid regex")
    public void testRemoveAllWithRegex() {
        assertEquals("HelloWorld", RegExUtil.removeAll("Hello123World456", "\\d+"));
        assertEquals("", RegExUtil.removeAll("123456", "\\d+"));
        assertEquals("HelloWorld", RegExUtil.removeAll("Hello   World", "\\s+"));
        assertEquals("", RegExUtil.removeAll(null, "\\d+"));
        assertEquals("", RegExUtil.removeAll("", "\\d+"));
        assertEquals("abc", RegExUtil.removeAll("abc", "\\d+"));
    }

    @Test
    @DisplayName("Test removeAll(String, String) with null/empty regex throws exception")
    public void testRemoveAllWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeAll("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeAll("test", ""));
    }

    @Test
    @DisplayName("Test removeAll(String, Pattern) with valid pattern")
    public void testRemoveAllWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("HelloWorld", RegExUtil.removeAll("Hello123World456", pattern));
        assertEquals("", RegExUtil.removeAll(null, pattern));
        assertEquals("", RegExUtil.removeAll("", pattern));
    }

    @Test
    @DisplayName("Test removeAll(String, Pattern) with null pattern throws exception")
    public void testRemoveAllWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeAll("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test removeFirst(String, String) with valid regex")
    public void testRemoveFirstWithRegex() {
        assertEquals("HelloWorld456", RegExUtil.removeFirst("Hello123World456", "\\d+"));
        assertEquals("", RegExUtil.removeFirst(null, "\\d+"));
        assertEquals("", RegExUtil.removeFirst("", "\\d+"));
        assertEquals("abc", RegExUtil.removeFirst("abc", "\\d+"));
    }

    @Test
    @DisplayName("Test removeFirst(String, String) with null/empty regex throws exception")
    public void testRemoveFirstWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeFirst("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeFirst("test", ""));
    }

    @Test
    @DisplayName("Test removeFirst(String, Pattern) with valid pattern")
    public void testRemoveFirstWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("HelloWorld456", RegExUtil.removeFirst("Hello123World456", pattern));
        assertEquals("", RegExUtil.removeFirst(null, pattern));
        assertEquals("", RegExUtil.removeFirst("", pattern));
    }

    @Test
    @DisplayName("Test removeFirst(String, Pattern) with null pattern throws exception")
    public void testRemoveFirstWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeFirst("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test removeLast(String, String) with valid regex")
    public void testRemoveLastWithRegex() {
        assertEquals("Hello123World", RegExUtil.removeLast("Hello123World456", "\\d+"));
        assertEquals("", RegExUtil.removeLast(null, "\\d+"));
        assertEquals("", RegExUtil.removeLast("", "\\d+"));
        assertEquals("abc", RegExUtil.removeLast("abc", "\\d+"));
    }

    @Test
    @DisplayName("Test removeLast(String, String) with null/empty regex throws exception")
    public void testRemoveLastWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeLast("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeLast("test", ""));
    }

    @Test
    @DisplayName("Test removeLast(String, Pattern) with valid pattern")
    public void testRemoveLastWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("Hello123World", RegExUtil.removeLast("Hello123World456", pattern));
        assertEquals("", RegExUtil.removeLast(null, pattern));
        assertEquals("", RegExUtil.removeLast("", pattern));
    }

    @Test
    @DisplayName("Test removeLast(String, Pattern) with null pattern throws exception")
    public void testRemoveLastWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.removeLast("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test replaceAll(String, String, String) with valid regex")
    public void testReplaceAllWithRegex() {
        assertEquals("Hello World", RegExUtil.replaceAll("Hello   World", "\\s+", " "));
        assertEquals("HelloXXXWorldXXX", RegExUtil.replaceAll("Hello123World456", "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceAll(null, "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceAll("", "\\d+", "XXX"));
        assertEquals("abc", RegExUtil.replaceAll("abc", "\\d+", "XXX"));
        assertEquals("HelloWorld", RegExUtil.replaceAll("Hello123World", "\\d+", (String) null));
    }

    @Test
    @DisplayName("Test replaceAll(String, String, String) with null/empty regex throws exception")
    public void testReplaceAllWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("test", (String) null, "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("test", "", "X"));
    }

    @Test
    @DisplayName("Test replaceAll(String, String, Function) with function replacer")
    public void testReplaceAllWithRegexAndFunction() {
        assertEquals("Hello World", RegExUtil.replaceAll("hello world", "\\b\\w", match -> match.toUpperCase()));
        assertEquals("", RegExUtil.replaceAll(null, "\\d+", match -> "X"));
        assertEquals("", RegExUtil.replaceAll("", "\\d+", match -> "X"));
    }

    @Test
    @DisplayName("Test replaceAll(String, String, IntBiFunction) with function replacer")
    public void testReplaceAllWithRegexAndIntBiFunction() {
        assertEquals("abc[3-6]def", RegExUtil.replaceAll("abc123def", "\\d+", (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("", RegExUtil.replaceAll(null, "\\d+", (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceAll("", "\\d+", (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test replaceAll(String, Pattern, String) with valid pattern")
    public void testReplaceAllWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        assertEquals("Hello World", RegExUtil.replaceAll("Hello   World", pattern, " "));
        assertEquals("", RegExUtil.replaceAll(null, pattern, " "));
        assertEquals("", RegExUtil.replaceAll("", pattern, " "));
    }

    @Test
    @DisplayName("Test replaceAll(String, Pattern, String) with null pattern throws exception")
    public void testReplaceAllWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("test", (Pattern) null, "X"));
    }

    @Test
    @DisplayName("Test replaceAll(String, Pattern, Function) with function replacer")
    public void testReplaceAllWithPatternAndFunction() {
        Pattern pattern = Pattern.compile("\\b\\w");
        assertEquals("Hello World", RegExUtil.replaceAll("hello world", pattern, match -> match.toUpperCase()));
        assertEquals("", RegExUtil.replaceAll(null, pattern, match -> "X"));
        assertEquals("", RegExUtil.replaceAll("", pattern, match -> "X"));
    }

    @Test
    @DisplayName("Test replaceAll(String, Pattern, IntBiFunction) with function replacer")
    public void testReplaceAllWithPatternAndIntBiFunction() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("abc[3-6]def", RegExUtil.replaceAll("abc123def", pattern, (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("", RegExUtil.replaceAll(null, pattern, (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceAll("", pattern, (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, String, String) with valid regex")
    public void testReplaceFirstWithRegex() {
        assertEquals("HelloXXXWorld456", RegExUtil.replaceFirst("Hello123World456", "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceFirst(null, "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceFirst("", "\\d+", "XXX"));
        assertEquals("abc", RegExUtil.replaceFirst("abc", "\\d+", "XXX"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, String, String) with null/empty regex throws exception")
    public void testReplaceFirstWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("test", (String) null, "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("test", "", "X"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, String, Function) with function replacer")
    public void testReplaceFirstWithRegexAndFunction() {
        assertEquals("Hello world", RegExUtil.replaceFirst("hello world", "\\b\\w", match -> match.toUpperCase()));
        assertEquals("", RegExUtil.replaceFirst(null, "\\d+", match -> "X"));
        assertEquals("", RegExUtil.replaceFirst("", "\\d+", match -> "X"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, String, IntBiFunction) with function replacer")
    public void testReplaceFirstWithRegexAndIntBiFunction() {
        assertEquals("abc[3-6]def456", RegExUtil.replaceFirst("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("", RegExUtil.replaceFirst(null, "\\d+", (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceFirst("", "\\d+", (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, Pattern, String) with valid pattern")
    public void testReplaceFirstWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("HelloXXXWorld456", RegExUtil.replaceFirst("Hello123World456", pattern, "XXX"));
        assertEquals("", RegExUtil.replaceFirst(null, pattern, "XXX"));
        assertEquals("", RegExUtil.replaceFirst("", pattern, "XXX"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, Pattern, String) with null pattern throws exception")
    public void testReplaceFirstWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceFirst("test", (Pattern) null, "X"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, Pattern, Function) with function replacer")
    public void testReplaceFirstWithPatternAndFunction() {
        Pattern pattern = Pattern.compile("\\b\\w");
        assertEquals("Hello world", RegExUtil.replaceFirst("hello world", pattern, match -> match.toUpperCase()));
        assertEquals("", RegExUtil.replaceFirst(null, pattern, match -> "X"));
        assertEquals("", RegExUtil.replaceFirst("", pattern, match -> "X"));
    }

    @Test
    @DisplayName("Test replaceFirst(String, Pattern, IntBiFunction) with function replacer")
    public void testReplaceFirstWithPatternAndIntBiFunction() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("abc[3-6]def456", RegExUtil.replaceFirst("abc123def456", pattern, (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("", RegExUtil.replaceFirst(null, pattern, (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceFirst("", pattern, (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test replaceLast(String, String, String) with valid regex")
    public void testReplaceLastWithRegex() {
        assertEquals("Hello123WorldXXX", RegExUtil.replaceLast("Hello123World456", "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceLast(null, "\\d+", "XXX"));
        assertEquals("", RegExUtil.replaceLast("", "\\d+", "XXX"));
        assertEquals("abc", RegExUtil.replaceLast("abc", "\\d+", "XXX"));
    }

    @Test
    @DisplayName("Test replaceLast(String, String, String) with null/empty regex throws exception")
    public void testReplaceLastWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("test", (String) null, "X"));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("test", "", "X"));
    }

    @Test
    @DisplayName("Test replaceLast(String, String, Function) with function replacer")
    public void testReplaceLastWithRegexAndFunction() {
        assertEquals("hello world HELLO", RegExUtil.replaceLast("hello world hello", "hello", match -> match.toUpperCase()));
        assertEquals("", RegExUtil.replaceLast(null, "\\d+", match -> "X"));
        assertEquals("", RegExUtil.replaceLast("", "\\d+", match -> "X"));
    }

    @Test
    @DisplayName("Test replaceLast(String, String, IntBiFunction) with function replacer")
    public void testReplaceLastWithRegexAndIntBiFunction() {
        assertEquals("abc123def[9-12]", RegExUtil.replaceLast("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("", RegExUtil.replaceLast(null, "\\d+", (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceLast("", "\\d+", (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test replaceLast(String, Pattern, String) with valid pattern")
    public void testReplaceLastWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("Hello123WorldXXX", RegExUtil.replaceLast("Hello123World456", pattern, "XXX"));
        assertEquals("", RegExUtil.replaceLast(null, pattern, "XXX"));
        assertEquals("", RegExUtil.replaceLast("", pattern, "XXX"));
    }

    @Test
    @DisplayName("Test replaceLast(String, Pattern, String) with null pattern throws exception")
    public void testReplaceLastWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceLast("test", (Pattern) null, "X"));
    }

    @Test
    @DisplayName("Test replaceLast(String, Pattern, Function) with function replacer")
    public void testReplaceLastWithPatternAndFunction() {
        Pattern pattern = Pattern.compile("hello");
        assertEquals("hello world HELLO", RegExUtil.replaceLast("hello world hello", pattern, match -> match.toUpperCase()));
        assertEquals("", RegExUtil.replaceLast(null, pattern, match -> "X"));
        assertEquals("", RegExUtil.replaceLast("", pattern, match -> "X"));
    }

    @Test
    @DisplayName("Test replaceLast(String, Pattern, IntBiFunction) with function replacer")
    public void testReplaceLastWithPatternAndIntBiFunction() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals("abc123def[9-12]", RegExUtil.replaceLast("abc123def456", pattern, (start, end) -> "[" + start + "-" + end + "]"));
        assertEquals("", RegExUtil.replaceLast(null, pattern, (start, end) -> "X"));
        assertEquals("", RegExUtil.replaceLast("", pattern, (start, end) -> "X"));
    }

    @Test
    @DisplayName("Test countMatches(String, String) with valid regex")
    public void testCountMatchesWithRegex() {
        assertEquals(3, RegExUtil.countMatches("Hello World", "l"));
        assertEquals(2, RegExUtil.countMatches("abc123def456", "\\d+"));
        assertEquals(0, RegExUtil.countMatches("abc", "\\d+"));
        assertEquals(0, RegExUtil.countMatches(null, "\\d+"));
        assertEquals(0, RegExUtil.countMatches("", "\\d+"));
    }

    @Test
    @DisplayName("Test countMatches(String, String) with null/empty regex throws exception")
    public void testCountMatchesWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.countMatches("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.countMatches("test", ""));
    }

    @Test
    @DisplayName("Test countMatches(String, Pattern) with valid pattern")
    public void testCountMatchesWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        assertEquals(3, RegExUtil.countMatches("abc123def456ghi789", pattern));
        assertEquals(0, RegExUtil.countMatches(null, pattern));
        assertEquals(0, RegExUtil.countMatches("", pattern));
    }

    @Test
    @DisplayName("Test countMatches(String, Pattern) with null pattern throws exception")
    public void testCountMatchesWithNullPattern() {
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
    }

    @Test
    @DisplayName("Test matchResults(String, String) with null/empty regex throws exception")
    public void testMatchResultsWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchResults("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchResults("test", ""));
    }

    @Test
    @DisplayName("Test matchResults(String, Pattern) with valid pattern")
    public void testMatchResultsWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        Stream<MatchResult> matches = RegExUtil.matchResults("abc123def456", pattern);
        assertNotNull(matches);
        assertEquals(2, matches.count());

        matches = RegExUtil.matchResults(null, pattern);
        assertNotNull(matches);
        assertEquals(0, matches.count());

        matches = RegExUtil.matchResults("", pattern);
        assertNotNull(matches);
        assertEquals(0, matches.count());
    }

    @Test
    @DisplayName("Test matchResults(String, Pattern) with null pattern throws exception")
    public void testMatchResultsWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchResults("test", (Pattern) null));
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
    }

    @Test
    @DisplayName("Test matchIndices(String, String) with null/empty regex throws exception")
    public void testMatchIndicesWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchIndices("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchIndices("test", ""));
    }

    @Test
    @DisplayName("Test matchIndices(String, Pattern) with valid pattern")
    public void testMatchIndicesWithPattern() {
        Pattern pattern = Pattern.compile("\\d+");
        IntStream indices = RegExUtil.matchIndices("abc123def456ghi", pattern);
        assertNotNull(indices);
        assertArrayEquals(new int[] { 3, 9 }, indices.toArray());

        indices = RegExUtil.matchIndices(null, pattern);
        assertNotNull(indices);
        assertEquals(0, indices.count());

        indices = RegExUtil.matchIndices("", pattern);
        assertNotNull(indices);
        assertEquals(0, indices.count());
    }

    @Test
    @DisplayName("Test matchIndices(String, Pattern) with null pattern throws exception")
    public void testMatchIndicesWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchIndices("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test split(String, String) with valid regex")
    public void testSplitWithRegex() {
        assertArrayEquals(new String[] { "one", "two", "three" }, RegExUtil.split("one,two,three", ","));
        assertArrayEquals(new String[] { "Hello", "World" }, RegExUtil.split("Hello   World", "\\s+"));
        assertArrayEquals(new String[0], RegExUtil.split(null, ","));
        assertArrayEquals(new String[] { "" }, RegExUtil.split("", ","));
    }

    @Test
    @DisplayName("Test split(String, String) with null/empty regex throws exception")
    public void testSplitWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", ""));
    }

    @Test
    @DisplayName("Test split(String, String, int) with limit")
    public void testSplitWithRegexAndLimit() {
        assertArrayEquals(new String[] { "one", "two", "three,four" }, RegExUtil.split("one,two,three,four", ",", 3));
        assertArrayEquals(new String[0], RegExUtil.split(null, ",", 3));
        assertArrayEquals(new String[] { "" }, RegExUtil.split("", ",", 3));
    }

    @Test
    @DisplayName("Test split(String, String, int) with null/empty regex throws exception")
    public void testSplitWithRegexAndLimitNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", (String) null, 3));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", "", 3));
    }

    @Test
    @DisplayName("Test split(String, Pattern) with valid pattern")
    public void testSplitWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        assertArrayEquals(new String[] { "Hello", "World", "Java" }, RegExUtil.split("Hello   World   Java", pattern));
        assertArrayEquals(new String[0], RegExUtil.split(null, pattern));
        assertArrayEquals(new String[] { "" }, RegExUtil.split("", pattern));
    }

    @Test
    @DisplayName("Test split(String, Pattern) with null pattern throws exception")
    public void testSplitWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", (Pattern) null));
    }

    @Test
    @DisplayName("Test split(String, Pattern, int) with limit")
    public void testSplitWithPatternAndLimit() {
        Pattern pattern = Pattern.compile(",");
        assertArrayEquals(new String[] { "a", "b", "c,d" }, RegExUtil.split("a,b,c,d", pattern, 3));
        assertArrayEquals(new String[0], RegExUtil.split(null, pattern, 3));
        assertArrayEquals(new String[] { "" }, RegExUtil.split("", pattern, 3));
    }

    @Test
    @DisplayName("Test split(String, Pattern, int) with null pattern throws exception")
    public void testSplitWithPatternAndLimitNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.split("test", (Pattern) null, 3));
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
        assertArrayEquals(new String[0], RegExUtil.splitToLines(null, 3));
        assertArrayEquals(new String[] { "" }, RegExUtil.splitToLines("", 3));
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
    @DisplayName("Test overlapping patterns")
    public void testOverlappingPatterns() {
        String text = "aaaa";
        assertEquals(4, RegExUtil.countMatches(text, "a"));
        assertEquals(2, RegExUtil.countMatches(text, "aa"));
    }

    @Test
    @DisplayName("Test special regex characters")
    public void testSpecialRegexCharacters() {
        assertTrue(RegExUtil.find("test.file", "\\."));
        assertTrue(RegExUtil.find("test(file)", "\\("));
        assertTrue(RegExUtil.find("test[file]", "\\["));
    }

    @Test
    @DisplayName("Test case sensitivity")
    public void testCaseSensitivity() {
        assertTrue(RegExUtil.find("Hello World", "Hello"));
        assertFalse(RegExUtil.find("Hello World", "hello"));
        assertTrue(RegExUtil.find("Hello World", "(?i)hello"));
    }

    @Test
    @DisplayName("Test word boundaries")
    public void testWordBoundaries() {
        assertTrue(RegExUtil.find("the cat", "\\bcat\\b"));
        assertFalse(RegExUtil.find("scatter", "\\bcat\\b"));
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
    @DisplayName("Test greedy vs non-greedy matching")
    public void testGreedyMatching() {
        assertEquals("X", RegExUtil.replaceAll("<tag>content</tag>", "<.*>", "X"));
        assertEquals("Xcontent</tag>", RegExUtil.replaceFirst("<tag>content</tag>", "<.*?>", "X"));
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
    @DisplayName("Test empty pattern matches")
    public void testEmptyPatternMatches() {
        String result = RegExUtil.replaceAll("abc", "(?=.)", "X");
        assertEquals("XaXbXc", result);
    }

    @Test
    @DisplayName("Test replaceLast with single match")
    public void testReplaceLastWithSingleMatch() {
        assertEquals("HelloXXXWorld", RegExUtil.replaceLast("Hello123World", "\\d+", "XXX"));
    }

    @Test
    @DisplayName("Test replaceLast with no match")
    public void testReplaceLastWithNoMatch() {
        assertEquals("HelloWorld", RegExUtil.replaceLast("HelloWorld", "\\d+", "XXX"));
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
    @DisplayName("Test matchIndices with no matches")
    public void testMatchIndicesWithNoMatches() {
        IntStream indices = RegExUtil.matchIndices("abc", "\\d+");
        assertEquals(0, indices.count());
    }

    @Test
    @DisplayName("Test pattern compilation caching")
    public void testPatternCompilationCaching() {
        Pattern pattern = Pattern.compile("\\d+");
        assertTrue(RegExUtil.find("123", pattern));
        assertTrue(RegExUtil.find("456", pattern));
        assertTrue(RegExUtil.find("789", pattern));
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

        // Verify null is converted to empty string internally
        assertEquals(RegExUtil.findFirst("", pattern), RegExUtil.findFirst(null, pattern));
    }

    @Test
    @DisplayName("Test findFirst with empty source returns null")
    public void testFindFirstWithEmptySource() {
        Pattern pattern = Pattern.compile("\\d+");
        assertNull(RegExUtil.findFirst("", pattern));

        Pattern anyPattern = Pattern.compile(".*");
        assertEquals("", RegExUtil.findFirst("", anyPattern));   // Empty match
    }

    @Test
    @DisplayName("Test findFirst with null pattern throws exception")
    public void testFindFirstWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst("test", (Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst(null, (Pattern) null));
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
        assertNotNull(phoneResult);   // Phone pattern finds something, exact match depends on pattern
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
        assertEquals("char", RegExUtil.findLast("This is a test with many four char items", wordPattern));   // "char" is the last 4-letter word

        // Test with overlapping possibilities
        Pattern overlapPattern = Pattern.compile("\\d{2}");
        assertEquals("90", RegExUtil.findLast("12345678901234567890", overlapPattern));   // Last two digits are "90"
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

        // Verify null is converted to empty string internally
        assertEquals(RegExUtil.findLast("", pattern), RegExUtil.findLast(null, pattern));
    }

    @Test
    @DisplayName("Test findLast with empty source returns null")
    public void testFindLastWithEmptySource() {
        Pattern pattern = Pattern.compile("\\d+");
        assertNull(RegExUtil.findLast("", pattern));

        Pattern anyPattern = Pattern.compile(".*");
        assertEquals("", RegExUtil.findLast("", anyPattern));   // Empty match
    }

    @Test
    @DisplayName("Test findLast with null pattern throws exception")
    public void testFindLastWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast("test", (Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast(null, (Pattern) null));
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
        assertNotNull(whitespaceResult);   // Will find whitespace, but could be single or double space
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
    @DisplayName("Test findFirst and findLast with multiline text")
    public void testFindFirstAndLastMultiline() {
        String multilineText = "Line 1: 100\nLine 2: 200\nLine 3: 300";

        Pattern numberPattern = Pattern.compile("\\d+");
        assertEquals("1", RegExUtil.findFirst(multilineText, numberPattern));   // Finds "1" in "Line 1"
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
        assertEquals("", RegExUtil.findLast("Hello World", fullMatchPattern));   // Last match might be empty at the end

        // Backreference pattern
        Pattern backrefPattern = Pattern.compile("(\\w+)\\s+\\1");
        assertEquals("the the", RegExUtil.findFirst("the the cat cat dog dog", backrefPattern));
        assertEquals("dog dog", RegExUtil.findLast("the the cat cat dog dog", backrefPattern));
    }

    // ===========================================
    // Tests for findFirst(String, String) and findLast(String, String) methods
    // ===========================================

    @Test
    @DisplayName("Test findFirst(String, String) with single match")
    public void testFindFirstStringRegexWithSingleMatch() {
        assertEquals("123", RegExUtil.findFirst("abc123def", "\\d+"));
        assertEquals("test", RegExUtil.findFirst("This is a test string", "test"));
        assertEquals("World", RegExUtil.findFirst("Hello World", "World"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with multiple matches returns first")
    public void testFindFirstStringRegexWithMultipleMatches() {
        assertEquals("123", RegExUtil.findFirst("abc123def456ghi789", "\\d+"));
        assertEquals("cat", RegExUtil.findFirst("The cat and the cat again", "cat"));
        assertEquals("10", RegExUtil.findFirst("Price: 10.99 and 20.50", "\\d+"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with no match returns null")
    public void testFindFirstStringRegexWithNoMatch() {
        assertNull(RegExUtil.findFirst("abc def ghi", "\\d+"));
        assertNull(RegExUtil.findFirst("Hello World", "xyz"));
        assertNull(RegExUtil.findFirst("no numbers", "[0-9]+"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with null source returns null")
    public void testFindFirstStringRegexWithNullSource() {
        assertNull(RegExUtil.findFirst(null, "\\d+"));
        assertNull(RegExUtil.findFirst(null, "test"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with empty source returns null")
    public void testFindFirstStringRegexWithEmptySource() {
        assertNull(RegExUtil.findFirst("", "\\d+"));
        assertNull(RegExUtil.findFirst("", "test"));
        assertEquals("", RegExUtil.findFirst("", ".*"));   // Pattern matches empty string
    }

    @Test
    @DisplayName("Test findFirst(String, String) with null regex throws exception")
    public void testFindFirstStringRegexWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst(null, (String) null));
    }

    @Test
    @DisplayName("Test findFirst(String, String) with empty regex throws exception")
    public void testFindFirstStringRegexWithEmptyRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findFirst("", ""));
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
    @DisplayName("Test findFirst(String, String) with greedy vs non-greedy")
    public void testFindFirstStringRegexGreedyVsNonGreedy() {
        // Greedy - matches from first < to last >
        assertEquals("<tag>content</tag>", RegExUtil.findFirst("<tag>content</tag> and <other>text</other>", "<([a-zA-Z0-9]+)>.*?</\\1>"));
        assertEquals("<other>text</other>", RegExUtil.findLast("<tag>content</tag> and <other>text</other>", "<([a-zA-Z0-9]+)>.*?</\\1>"));

        // Non-greedy - matches shortest possible
        assertEquals("<tag>", RegExUtil.findFirst("<tag>content</tag> and <other>text</other>", "<.*?>"));
    }

    @Test
    @DisplayName("Test findFirst(String, String) at beginning of string")
    public void testFindFirstStringRegexAtBeginning() {
        assertEquals("123", RegExUtil.findFirst("123abc456", "\\d+"));
        assertEquals("123", RegExUtil.findFirst("123abc456", "^\\d+"));
        assertNull(RegExUtil.findFirst("abc123def", "^\\d+"));
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
    @DisplayName("Test findLast(String, String) with single match")
    public void testFindLastStringRegexWithSingleMatch() {
        assertEquals("123", RegExUtil.findLast("abc123def", "\\d+"));
        assertEquals("test", RegExUtil.findLast("This is a test string", "test"));
        assertEquals("World", RegExUtil.findLast("Hello World", "World"));
    }

    @Test
    @DisplayName("Test findLast(String, String) with multiple matches returns last")
    public void testFindLastStringRegexWithMultipleMatches() {
        assertEquals("789", RegExUtil.findLast("abc123def456ghi789", "\\d+"));
        assertEquals("cat", RegExUtil.findLast("The cat and the cat again", "cat"));
        assertEquals("50", RegExUtil.findLast("Price: 10.99 and 20.50", "\\d+"));
    }

    @Test
    @DisplayName("Test findLast(String, String) with no match returns null")
    public void testFindLastStringRegexWithNoMatch() {
        assertNull(RegExUtil.findLast("abc def ghi", "\\d+"));
        assertNull(RegExUtil.findLast("Hello World", "xyz"));
        assertNull(RegExUtil.findLast("no numbers", "[0-9]+"));
    }

    @Test
    @DisplayName("Test findLast(String, String) with null source returns null")
    public void testFindLastStringRegexWithNullSource() {
        assertNull(RegExUtil.findLast(null, "\\d+"));
        assertNull(RegExUtil.findLast(null, "test"));
    }

    @Test
    @DisplayName("Test findLast(String, String) with empty source returns null")
    public void testFindLastStringRegexWithEmptySource() {
        assertNull(RegExUtil.findLast("", "\\d+"));
        assertNull(RegExUtil.findLast("", "test"));
        assertEquals("", RegExUtil.findLast("", ".*"));   // Pattern matches empty string
    }

    @Test
    @DisplayName("Test findLast(String, String) with null regex throws exception")
    public void testFindLastStringRegexWithNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast("test", (String) null));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast(null, (String) null));
    }

    @Test
    @DisplayName("Test findLast(String, String) with empty regex throws exception")
    public void testFindLastStringRegexWithEmptyRegex() {
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast("test", ""));
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.findLast("", ""));
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
    @DisplayName("Test findLast(String, String) with greedy vs non-greedy")
    public void testFindLastStringRegexGreedyVsNonGreedy() {
        // Greedy - matches from first < to last >
        assertEquals("<tag>content</tag> and <other>text</other>", RegExUtil.findLast("<tag>content</tag> and <other>text</other> extra", "<.*>"));

        // Non-greedy - matches individual tags
        assertEquals("</other>", RegExUtil.findLast("<tag>content</tag> and <other>text</other>", "<.*?>"));
    }

    @Test
    @DisplayName("Test findLast(String, String) at end of string")
    public void testFindLastStringRegexAtEnd() {
        assertEquals("456", RegExUtil.findLast("123abc456", "\\d+"));
        assertEquals("456", RegExUtil.findLast("123abc456", "\\d+$"));
        assertNull(RegExUtil.findLast("123abcdef", "\\d+$"));
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
}
