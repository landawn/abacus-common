package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
