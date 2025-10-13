package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Strings102Test extends TestBase {

    @Test
    public void testGuid() {
        String guid = Strings.guid();
        assertNotNull(guid);
        assertEquals(32, guid.length());
        assertFalse(guid.contains("-"));
    }

    @Test
    public void testUuid() {
        String uuid = Strings.uuid();
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    public void testValueOf() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("hello", Strings.valueOf(new char[] { 'h', 'e', 'l', 'l', 'o' }));
    }

    @Test
    public void testIsKeyword() {
        assertFalse(Strings.isKeyword(null));
        assertFalse(Strings.isKeyword(""));
        assertTrue(Strings.isKeyword("class"));
        assertTrue(Strings.isKeyword("public"));
        assertFalse(Strings.isKeyword("hello"));
    }

    @Test
    public void testIsValidJavaIdentifier() {
        assertFalse(Strings.isValidJavaIdentifier(null));
        assertFalse(Strings.isValidJavaIdentifier(""));
        assertTrue(Strings.isValidJavaIdentifier("myVariable"));
        assertTrue(Strings.isValidJavaIdentifier("_underscore"));
        assertTrue(Strings.isValidJavaIdentifier("$dollar"));
        assertFalse(Strings.isValidJavaIdentifier("123invalid"));
        assertFalse(Strings.isValidJavaIdentifier("my-variable"));
    }

    @Test
    public void testIsValidEmailAddress() {
        assertFalse(Strings.isValidEmailAddress(null));
        assertFalse(Strings.isValidEmailAddress(""));
        assertTrue(Strings.isValidEmailAddress("test@example.com"));
        assertTrue(Strings.isValidEmailAddress("user.name@domain.co.uk"));
        assertFalse(Strings.isValidEmailAddress("invalid.email"));
        assertFalse(Strings.isValidEmailAddress("@domain.com"));
    }

    @Test
    public void testIsValidUrl() {
        assertFalse(Strings.isValidUrl(null));
        assertFalse(Strings.isValidUrl(""));
        assertTrue(Strings.isValidUrl("https://www.example.com"));
        assertTrue(Strings.isValidUrl("http://localhost:8080/path"));
        assertFalse(Strings.isValidUrl("not-a-url"));
    }

    @Test
    public void testIsValidHttpUrl() {
        assertFalse(Strings.isValidHttpUrl(null));
        assertFalse(Strings.isValidHttpUrl(""));
        assertTrue(Strings.isValidHttpUrl("https://www.example.com"));
        assertTrue(Strings.isValidHttpUrl("http://localhost:8080"));
        assertFalse(Strings.isValidHttpUrl("ftp://example.com"));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("test"));
    }

    @Test
    public void testIsBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertTrue(Strings.isBlank("\t\n"));
        assertFalse(Strings.isBlank("test"));
        assertFalse(Strings.isBlank(" test "));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(Strings.isNotEmpty(null));
        assertFalse(Strings.isNotEmpty(""));
        assertTrue(Strings.isNotEmpty(" "));
        assertTrue(Strings.isNotEmpty("test"));
    }

    @Test
    public void testIsNotBlank() {
        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank(""));
        assertFalse(Strings.isNotBlank(" "));
        assertTrue(Strings.isNotBlank("test"));
    }

    @Test
    public void testIsAllEmpty() {
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertFalse(Strings.isAllEmpty("", "test"));
        assertTrue(Strings.isAllEmpty());
        assertTrue(Strings.isAllEmpty("", null, ""));
        assertFalse(Strings.isAllEmpty("", null, "test"));
    }

    @Test
    public void testIsAllBlank() {
        assertTrue(Strings.isAllBlank("", ""));
        assertTrue(Strings.isAllBlank(null, " "));
        assertFalse(Strings.isAllBlank("", "test"));
        assertTrue(Strings.isAllBlank());
        assertTrue(Strings.isAllBlank("", " ", "\t"));
        assertFalse(Strings.isAllBlank("", " ", "test"));
    }

    @Test
    public void testIsAnyEmpty() {
        assertTrue(Strings.isAnyEmpty("", "test"));
        assertTrue(Strings.isAnyEmpty(null, "test"));
        assertFalse(Strings.isAnyEmpty("hello", "world"));
        assertFalse(Strings.isAnyEmpty());
        assertTrue(Strings.isAnyEmpty("hello", "", "world"));
    }

    @Test
    public void testIsAnyBlank() {
        assertTrue(Strings.isAnyBlank("", "test"));
        assertTrue(Strings.isAnyBlank(" ", "test"));
        assertFalse(Strings.isAnyBlank("hello", "world"));
        assertFalse(Strings.isAnyBlank());
        assertTrue(Strings.isAnyBlank("hello", " ", "world"));
    }

    @Test
    public void testIsWrappedWith() {
        assertTrue(Strings.isWrappedWith("(test)", "(", ")"));
        assertTrue(Strings.isWrappedWith("\"hello\"", "\""));
        assertFalse(Strings.isWrappedWith("test", "(", ")"));
        assertFalse(Strings.isWrappedWith(null, "(", ")"));
        assertFalse(Strings.isWrappedWith("(test", "(", ")"));
    }

    @Test
    public void testFirstNonEmpty() {
        assertEquals("test", Strings.firstNonEmpty("", "test", "another"));
        assertEquals("first", Strings.firstNonEmpty("first", "second"));
        assertEquals("", Strings.firstNonEmpty("", "", null));
        assertEquals("", Strings.firstNonEmpty());
    }

    @Test
    public void testFirstNonBlank() {
        assertEquals("test", Strings.firstNonBlank("", " ", "test"));
        assertEquals("first", Strings.firstNonBlank("first", "second"));
        assertEquals("", Strings.firstNonBlank("", " ", null));
        assertEquals("", Strings.firstNonBlank());
    }

    @Test
    public void testDefaultIfNull() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
        assertEquals("value", Strings.defaultIfNull("value", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull((String) null, (String) null));
    }

    @Test
    public void testDefaultIfEmpty() {
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("value", Strings.defaultIfEmpty("value", "default"));
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("", ""));
    }

    @Test
    public void testDefaultIfBlank() {
        assertEquals("default", Strings.defaultIfBlank(" ", "default"));
        assertEquals("value", Strings.defaultIfBlank("value", "default"));
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("", " "));
    }

    @Test
    public void testNullToEmpty() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("test", Strings.nullToEmpty("test"));
        assertEquals("", Strings.nullToEmpty(""));
    }

    @Test
    public void testEmptyToNull() {
        assertNull(Strings.emptyToNull(""));
        assertNull(Strings.emptyToNull((String) null));
        assertEquals("test", Strings.emptyToNull("test"));
    }

    @Test
    public void testBlankToEmpty() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty(" "));
        assertEquals("test", Strings.blankToEmpty("test"));
    }

    @Test
    public void testBlankToNull() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull(" "));
        assertEquals("test", Strings.blankToNull("test"));
    }

    @Test
    public void testAbbreviate() {
        assertEquals("abc...", Strings.abbreviate("abcdefg", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 7));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 8));
        assertNull(Strings.abbreviate(null, 4));
        assertEquals("", Strings.abbreviate("", 4));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", 3));
    }

    @Test
    public void testAbbreviateWithMarker() {
        assertEquals("abc***", Strings.abbreviate("abcdefg", "***", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", "***", 7));
        assertNull(Strings.abbreviate(null, "***", 4));
    }

    @Test
    public void testAbbreviateMiddle() {
        assertEquals("abcdef", Strings.abbreviateMiddle("abcdef", "...", 6));
        assertEquals("ab...f", Strings.abbreviateMiddle("abccdef", "...", 6));
        assertNull(Strings.abbreviateMiddle(null, "...", 6));
    }

    @Test
    public void testCenter() {
        assertEquals("  ab  ", Strings.center("ab", 6));
        assertEquals(" ab  ", Strings.center("ab", 5));
        assertEquals("abcd", Strings.center("abcd", 2));
        assertEquals("    ", Strings.center(null, 4));
    }

    @Test
    public void testCenterWithChar() {
        assertEquals("xxabxx", Strings.center("ab", 6, 'x'));
        assertEquals("xabxx", Strings.center("ab", 5, 'x'));
    }

    @Test
    public void testCenterWithString() {
        assertEquals("--ab--", Strings.center("ab", 6, "--"));
        assertEquals("-ab--", Strings.center("ab", 5, "-"));
    }

    @Test
    public void testPadStart() {
        assertEquals("   abc", Strings.padStart("abc", 6));
        assertEquals("000123", Strings.padStart("123", 6, '0'));
        assertEquals("--hello", Strings.padStart("hello", 7, "--"));
        assertEquals("abc", Strings.padStart("abc", 2));
    }

    @Test
    public void testPadEnd() {
        assertEquals("abc   ", Strings.padEnd("abc", 6));
        assertEquals("123000", Strings.padEnd("123", 6, '0'));
        assertEquals("hello--", Strings.padEnd("hello", 7, "--"));
        assertEquals("abc", Strings.padEnd("abc", 2));
    }

    @Test
    public void testRepeat() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertEquals("", Strings.repeat("hello", 0));
        assertEquals("hellohello", Strings.repeat("hello", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    public void testRepeatWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("hello,hello", Strings.repeat("hello", 2, ","));
        assertEquals("", Strings.repeat("hello", 0, ","));
    }

    @Test
    public void testRepeatWithPrefixSuffix() {
        assertEquals("[a,a,a]", Strings.repeat("a", 3, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("a", 0, ",", "[", "]"));
    }

    @Test
    public void testGetBytes() {
        assertNull(Strings.getBytes(null));
        assertArrayEquals("hello".getBytes(), Strings.getBytes("hello"));
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8("hello"));
    }

    @Test
    public void testToCharArray() {
        assertNull(Strings.toCharArray(null));
        assertArrayEquals(new char[0], Strings.toCharArray(""));
        assertArrayEquals("hello".toCharArray(), Strings.toCharArray("hello"));
    }

    @Test
    public void testToCodePoints() {
        assertNull(Strings.toCodePoints(null));
        assertArrayEquals(new int[0], Strings.toCodePoints(""));
        assertArrayEquals("hello".codePoints().toArray(), Strings.toCodePoints("hello"));
    }

    @Test
    public void testToLowerCase() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals("hello", Strings.toLowerCase("HELLO"));
        assertEquals("", Strings.toLowerCase(""));
        assertNull(Strings.toLowerCase(null));
    }

    @Test
    public void testToUpperCase() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals("HELLO", Strings.toUpperCase("hello"));
        assertEquals("", Strings.toUpperCase(""));
        assertNull(Strings.toUpperCase(null));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("firstName", Strings.toCamelCase("first_name"));
        assertEquals("myVariable", Strings.toCamelCase("my_variable"));
        assertEquals("", Strings.toCamelCase(""));
        assertNull(Strings.toCamelCase(null));
    }

    @Test
    public void testToPascalCase() {
        assertEquals("FirstName", Strings.toPascalCase("first_name"));
        assertEquals("MyVariable", Strings.toPascalCase("my_variable"));
        assertEquals("", Strings.toPascalCase(""));
        assertNull(Strings.toPascalCase(null));
    }

    @Test
    public void testToLowerCaseWithUnderscore() {
        assertEquals("my_variable", Strings.toLowerCaseWithUnderscore("MyVariable"));
        assertEquals("test_string", Strings.toLowerCaseWithUnderscore("TestString"));
    }

    @Test
    public void testToUpperCaseWithUnderscore() {
        assertEquals("MY_VARIABLE", Strings.toUpperCaseWithUnderscore("MyVariable"));
        assertEquals("TEST_STRING", Strings.toUpperCaseWithUnderscore("TestString"));
    }

    @Test
    public void testSwapCase() {
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('a', Strings.swapCase('A'));
        assertEquals("hELLO", Strings.swapCase("Hello"));
    }

    @Test
    public void testCapitalize() {
        assertEquals("Hello", Strings.capitalize("hello"));
        assertEquals("Hello", Strings.capitalize("Hello"));
        assertEquals("", Strings.capitalize(""));
        assertNull(Strings.capitalize(null));
    }

    @Test
    public void testUncapitalize() {
        assertEquals("hello", Strings.uncapitalize("Hello"));
        assertEquals("hello", Strings.uncapitalize("hello"));
        assertEquals("", Strings.uncapitalize(""));
        assertNull(Strings.uncapitalize(null));
    }

    @Test
    public void testIsLowerCase() {
        assertTrue(Strings.isLowerCase('a'));
        assertFalse(Strings.isLowerCase('A'));
        assertTrue(Strings.isAllLowerCase("hello"));
        assertFalse(Strings.isAllLowerCase("Hello"));
        assertTrue(Strings.isAllLowerCase(""));
    }

    @Test
    public void testIsUpperCase() {
        assertTrue(Strings.isUpperCase('A'));
        assertFalse(Strings.isUpperCase('a'));
        assertTrue(Strings.isAllUpperCase("HELLO"));
        assertFalse(Strings.isAllUpperCase("Hello"));
        assertTrue(Strings.isAllUpperCase(""));
    }

    @Test
    public void testIsMixedCase() {
        assertTrue(Strings.isMixedCase("Hello"));
        assertFalse(Strings.isMixedCase("HELLO"));
        assertFalse(Strings.isMixedCase("hello"));
        assertFalse(Strings.isMixedCase(""));
        assertFalse(Strings.isMixedCase("H"));
    }

    @Test
    public void testIsDigit() {
        assertTrue(Strings.isDigit('5'));
        assertFalse(Strings.isDigit('a'));
    }

    @Test
    public void testIsLetter() {
        assertTrue(Strings.isLetter('a'));
        assertFalse(Strings.isLetter('5'));
    }

    @Test
    public void testIsLetterOrDigit() {
        assertTrue(Strings.isLetterOrDigit('a'));
        assertTrue(Strings.isLetterOrDigit('5'));
        assertFalse(Strings.isLetterOrDigit(' '));
    }

    @Test
    public void testIsAscii() {
        assertTrue(Strings.isAscii('a'));
        assertTrue(Strings.isAscii('1'));
        assertTrue(Strings.isAscii(' '));
        assertFalse(Strings.isAscii('ñ'));
    }

    @Test
    public void testIsAsciiPrintable() {
        assertTrue(Strings.isAsciiPrintable('a'));
        assertTrue(Strings.isAsciiPrintable('1'));
        assertTrue(Strings.isAsciiPrintable(' '));
        assertFalse(Strings.isAsciiPrintable('\n'));

        assertTrue(Strings.isAsciiPrintable("hello"));
        assertFalse(Strings.isAsciiPrintable("hello\n"));
        assertFalse(Strings.isAsciiPrintable(null));
    }

    @Test
    public void testIsAsciiAlpha() {
        assertTrue(Strings.isAsciiAlpha('a'));
        assertTrue(Strings.isAsciiAlpha('Z'));
        assertFalse(Strings.isAsciiAlpha('1'));

        assertTrue(Strings.isAsciiAlpha("hello"));
        assertFalse(Strings.isAsciiAlpha("hello1"));
        assertFalse(Strings.isAsciiAlpha(""));
    }

    @Test
    public void testIsAsciiNumeric() {
        assertTrue(Strings.isAsciiNumeric('5'));
        assertFalse(Strings.isAsciiNumeric('a'));

        assertTrue(Strings.isAsciiNumeric("12345"));
        assertFalse(Strings.isAsciiNumeric("123a"));
        assertFalse(Strings.isAsciiNumeric(""));
    }

    @Test
    public void testIsAsciiAlphanumeric() {
        assertTrue(Strings.isAsciiAlphanumeric('a'));
        assertTrue(Strings.isAsciiAlphanumeric('5'));
        assertFalse(Strings.isAsciiAlphanumeric(' '));

        assertTrue(Strings.isAsciiAlphanumeric("hello123"));
        assertFalse(Strings.isAsciiAlphanumeric("hello 123"));
        assertFalse(Strings.isAsciiAlphanumeric(""));
    }

    @Test
    public void testIsAlpha() {
        assertTrue(Strings.isAlpha("hello"));
        assertFalse(Strings.isAlpha("hello123"));
        assertFalse(Strings.isAlpha(""));
        assertFalse(Strings.isAlpha(null));
    }

    @Test
    public void testIsAlphaSpace() {
        assertTrue(Strings.isAlphaSpace("hello world"));
        assertFalse(Strings.isAlphaSpace("hello123"));
        assertFalse(Strings.isAlphaSpace(null));
        assertTrue(Strings.isAlphaSpace(""));
    }

    @Test
    public void testIsAlphanumeric() {
        assertTrue(Strings.isAlphanumeric("hello123"));
        assertFalse(Strings.isAlphanumeric("hello 123"));
        assertFalse(Strings.isAlphanumeric(""));
        assertFalse(Strings.isAlphanumeric(null));
    }

    @Test
    public void testIsNumeric() {
        assertTrue(Strings.isNumeric("12345"));
        assertFalse(Strings.isNumeric("123.45"));
        assertFalse(Strings.isNumeric(""));
        assertFalse(Strings.isNumeric(null));
    }

    @Test
    public void testIsWhitespace() {
        assertTrue(Strings.isWhitespace("   "));
        assertTrue(Strings.isWhitespace(""));
        assertFalse(Strings.isWhitespace("hello"));
        assertFalse(Strings.isWhitespace(null));
    }

    @Test
    public void testIsAsciiDigitalNumber() {
        assertTrue(Strings.isAsciiDigitalNumber("123"));
        assertTrue(Strings.isAsciiDigitalNumber("123.45"));
        assertTrue(Strings.isAsciiDigitalNumber("-123"));
        assertTrue(Strings.isAsciiDigitalNumber("1.23e10"));
        assertFalse(Strings.isAsciiDigitalNumber("abc"));
        assertFalse(Strings.isAsciiDigitalNumber(""));
        assertFalse(Strings.isAsciiDigitalNumber(null));
    }

    @Test
    public void testIsAsciiDigitalInteger() {
        assertTrue(Strings.isAsciiDigitalInteger("123"));
        assertTrue(Strings.isAsciiDigitalInteger("-123"));
        assertFalse(Strings.isAsciiDigitalInteger("123.45"));
        assertFalse(Strings.isAsciiDigitalInteger("abc"));
        assertFalse(Strings.isAsciiDigitalInteger(""));
        assertFalse(Strings.isAsciiDigitalInteger(null));
    }

    @Test
    public void testIndexOf() {
        assertEquals(1, Strings.indexOf("hello", 'e'));
        assertEquals(-1, Strings.indexOf("hello", 'x'));
        assertEquals(-1, Strings.indexOf(null, 'e'));
        assertEquals(-1, Strings.indexOf("", 'e'));

        assertEquals(1, Strings.indexOf("hello", "ell"));
        assertEquals(-1, Strings.indexOf("hello", "xyz"));
        assertEquals(-1, Strings.indexOf(null, "ell"));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        assertEquals(3, Strings.indexOf("hello", 'l', 3));
        assertEquals(-1, Strings.indexOf("hello", 'e', 2));

        assertEquals(4, Strings.indexOf("hello hello", "o", 2));
        assertEquals(-1, Strings.indexOf("hello", "ell", 2));
    }

    @Test
    public void testIndexOfAny() {
        assertEquals(1, Strings.indexOfAny("hello", 'e', 'x'));
        assertEquals(-1, Strings.indexOfAny("hello", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAny((String) null, Array.of('e')));
        assertEquals(-1, Strings.indexOfAny("", Array.of('e')));

        assertEquals(1, Strings.indexOfAny("hello", "ell", "xyz"));
        assertEquals(-1, Strings.indexOfAny("hello", "xyz", "abc"));
    }

    @Test
    public void testIndexOfAnyBut() {
        assertEquals(2, Strings.indexOfAnyBut("hello", 'h', 'e'));
        assertEquals(-1, Strings.indexOfAnyBut("hehe", 'h', 'e'));
        assertEquals(-1, Strings.indexOfAnyBut(null, 'h'));
        assertEquals(0, Strings.indexOfAnyBut("hello"));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        assertEquals(1, Strings.indexOfIgnoreCase("hello", "ELL"));
        assertEquals(-1, Strings.indexOfIgnoreCase("hello", "XYZ"));
        assertEquals(-1, Strings.indexOfIgnoreCase(null, "ell"));
    }

    @Test
    public void testLastIndexOf() {
        assertEquals(3, Strings.lastIndexOf("hello", 'l'));
        assertEquals(-1, Strings.lastIndexOf("hello", 'x'));
        assertEquals(-1, Strings.lastIndexOf(null, 'l'));

        assertEquals(1, Strings.lastIndexOf("hello", "ell"));
        assertEquals(-1, Strings.lastIndexOf("hello", "xyz"));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        assertEquals(1, Strings.lastIndexOfIgnoreCase("hello", "ELL"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("hello", "XYZ"));
    }

    @Test
    public void testLastIndexOfAny() {
        assertEquals(4, Strings.lastIndexOfAny("hello", 'o', 'x'));
        assertEquals(-1, Strings.lastIndexOfAny("hello", 'x', 'y'));
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
    public void testContainsIgnoreCase() {
        assertTrue(Strings.containsIgnoreCase("hello", "ELL"));
        assertFalse(Strings.containsIgnoreCase("hello", "XYZ"));
        assertFalse(Strings.containsIgnoreCase(null, "ell"));
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
    public void testContainsAny() {
        assertTrue(Strings.containsAny("hello", 'h', 'x'));
        assertFalse(Strings.containsAny("hello", 'x', 'y'));
        assertFalse(Strings.containsAny("", 'x'));

        assertTrue(Strings.containsAny("hello", "world", "ell"));
        assertFalse(Strings.containsAny("hello", "xyz", "abc"));
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
    @DisplayName("containsOnly should return true for null string")
    public void testContainsOnly_NullString() {
        assertTrue(Strings.containsOnly(null, 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly(null, 'x'));
        assertTrue(Strings.containsOnly(null));
    }

    @Test
    @DisplayName("containsOnly should return true for empty string")
    public void testContainsOnly_EmptyString() {
        assertTrue(Strings.containsOnly("", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("", 'x'));
        assertTrue(Strings.containsOnly("", new char[0]));
    }

    @Test
    @DisplayName("containsOnly should return false when valuesToFind is null or empty and string is not empty")
    public void testContainsOnly_EmptyValuesToFind() {
        assertFalse(Strings.containsOnly("abc", new char[0]));
        assertFalse(Strings.containsOnly("a", new char[0]));
        assertFalse(Strings.containsOnly(" ", new char[0]));

        assertFalse(Strings.containsOnly("abc", (char[]) null));
    }

    @Test
    @DisplayName("containsOnly should return true when string contains only specified characters")
    public void testContainsOnly_StringContainsOnlySpecifiedChars() {
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertTrue(Strings.containsOnly("abcabc", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("cba", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("a", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("ab", 'a', 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should return false when string contains characters not in valuesToFind")
    public void testContainsOnly_StringContainsOtherChars() {
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("axb", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("xyz", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("ab c", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("123", 'a', 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should handle single character strings")
    public void testContainsOnly_SingleCharacter() {
        assertTrue(Strings.containsOnly("a", 'a'));
        assertTrue(Strings.containsOnly("a", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("x", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("a", 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should handle special characters")
    public void testContainsOnly_SpecialCharacters() {
        assertTrue(Strings.containsOnly(" ", ' '));
        assertTrue(Strings.containsOnly("\t\n", '\t', '\n'));
        assertTrue(Strings.containsOnly("!@#", '!', '@', '#'));
        assertTrue(Strings.containsOnly("123", '1', '2', '3'));

        assertFalse(Strings.containsOnly("Hello World", 'H', 'e', 'l', 'o'));
        assertTrue(Strings.containsOnly("Hello", 'H', 'e', 'l', 'o'));
    }

    @Test
    @DisplayName("containsOnly should handle unicode characters")
    public void testContainsOnly_UnicodeCharacters() {
        assertTrue(Strings.containsOnly("αβγ", 'α', 'β', 'γ'));
        assertFalse(Strings.containsOnly("αβγδ", 'α', 'β', 'γ'));
    }

    @Test
    @DisplayName("containsOnly should handle repeated characters in valuesToFind")
    public void testContainsOnly_RepeatedValuesToFind() {
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c', 'a', 'b'));
        assertTrue(Strings.containsOnly("aaa", 'a', 'a', 'a'));
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c', 'a', 'b'));
    }

    @Test
    @DisplayName("containsOnly should handle case sensitivity")
    public void testContainsOnly_CaseSensitive() {
        assertFalse(Strings.containsOnly("ABC", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abc", 'A', 'B', 'C'));
        assertTrue(Strings.containsOnly("ABC", 'A', 'B', 'C'));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should handle long strings")
    public void testContainsOnly_LongStrings() {
        String longString = "a".repeat(1000);
        assertTrue(Strings.containsOnly(longString, 'a'));
        assertFalse(Strings.containsOnly(longString + "b", 'a'));

        String mixedLongString = "abcabc".repeat(100);
        assertTrue(Strings.containsOnly(mixedLongString, 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly(mixedLongString + "d", 'a', 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should handle whitespace characters")
    public void testContainsOnly_Whitespace() {
        assertTrue(Strings.containsOnly("   ", ' '));
        assertTrue(Strings.containsOnly("\t\t\t", '\t'));
        assertTrue(Strings.containsOnly("\n\n", '\n'));
        assertTrue(Strings.containsOnly(" \t\n", ' ', '\t', '\n'));

        assertFalse(Strings.containsOnly("a b", 'a', 'b'));
        assertTrue(Strings.containsOnly("a b", 'a', 'b', ' '));
    }

    @Test
    @DisplayName("containsOnly should handle numeric strings")
    public void testContainsOnly_Numeric() {
        assertTrue(Strings.containsOnly("123", '1', '2', '3'));
        assertTrue(Strings.containsOnly("000", '0'));
        assertTrue(Strings.containsOnly("1234567890", '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));

        assertFalse(Strings.containsOnly("123a", '1', '2', '3'));
        assertFalse(Strings.containsOnly("12.3", '1', '2', '3'));
    }

    @Test
    @DisplayName("containsOnly should validate input characters for surrogate pairs")
    public void testContainsOnly_SurrogateValidation() {
        char highSurrogate = '\uD800';
        char lowSurrogate = '\uDC00';

        assertThrows(IllegalArgumentException.class, () -> Strings.containsOnly("test", highSurrogate));

        assertThrows(IllegalArgumentException.class, () -> Strings.containsOnly("test", lowSurrogate));

        assertThrows(IllegalArgumentException.class, () -> Strings.containsOnly("test", 'a', highSurrogate, 'b'));
    }

    @Test
    @DisplayName("containsOnly edge cases")
    public void testContainsOnly_EdgeCases() {
        assertTrue(Strings.containsOnly("a", 'a'));

        assertTrue(Strings.containsOnly("aaabbbccc", 'a', 'b', 'c'));

        assertTrue(Strings.containsOnly("ab", 'a', 'b', 'c', 'd', 'e'));

        assertTrue(Strings.containsOnly("aaaa", 'a'));

        assertTrue(Strings.containsOnly("abcd", 'a', 'b', 'c', 'd'));
    }

    @Nested
    @DisplayName("containsWhitespace tests")
    public class ContainsWhitespaceTests {

        @Test
        @DisplayName("should return false for null string")
        public void testContainsWhitespace_NullString() {
            assertFalse(Strings.containsWhitespace(null));
        }

        @Test
        @DisplayName("should return false for empty string")
        public void testContainsWhitespace_EmptyString() {
            assertFalse(Strings.containsWhitespace(""));
        }

        @Test
        @DisplayName("should return false for string without whitespace")
        public void testContainsWhitespace_NoWhitespace() {
            assertFalse(Strings.containsWhitespace("abc"));
            assertFalse(Strings.containsWhitespace("123"));
            assertFalse(Strings.containsWhitespace("!@#$%"));
            assertFalse(Strings.containsWhitespace("αβγ"));
        }

        @Test
        @DisplayName("should return true for string with space")
        public void testContainsWhitespace_WithSpace() {
            assertTrue(Strings.containsWhitespace(" "));
            assertTrue(Strings.containsWhitespace("a b"));
            assertTrue(Strings.containsWhitespace(" abc"));
            assertTrue(Strings.containsWhitespace("abc "));
            assertTrue(Strings.containsWhitespace("a b c"));
        }

        @Test
        @DisplayName("should return true for string with various whitespace characters")
        public void testContainsWhitespace_VariousWhitespace() {
            assertTrue(Strings.containsWhitespace("\t"));
            assertTrue(Strings.containsWhitespace("\n"));
            assertTrue(Strings.containsWhitespace("\r"));
            assertTrue(Strings.containsWhitespace("\f"));
            assertTrue(Strings.containsWhitespace("a\tb"));
            assertTrue(Strings.containsWhitespace("line1\nline2"));
            assertTrue(Strings.containsWhitespace("text\rtext"));
        }
    }

    @Nested
    @DisplayName("startsWith tests")
    public class StartsWithTests {

        @Test
        @DisplayName("should return false for null string")
        public void testStartsWith_NullString() {
            assertFalse(Strings.startsWith(null, "prefix"));
            assertFalse(Strings.startsWith(null, null));
        }

        @Test
        @DisplayName("should return false for null prefix")
        public void testStartsWith_NullPrefix() {
            assertFalse(Strings.startsWith("test", null));
        }

        @Test
        @DisplayName("should return false when prefix is longer than string")
        public void testStartsWith_PrefixLongerThanString() {
            assertFalse(Strings.startsWith("ab", "abc"));
            assertFalse(Strings.startsWith("", "a"));
        }

        @Test
        @DisplayName("should return true for valid prefix")
        public void testStartsWith_ValidPrefix() {
            assertTrue(Strings.startsWith("hello", "hel"));
            assertTrue(Strings.startsWith("test", "test"));
            assertTrue(Strings.startsWith("abc", ""));
            assertTrue(Strings.startsWith("", ""));
        }

        @Test
        @DisplayName("should be case sensitive")
        public void testStartsWith_CaseSensitive() {
            assertFalse(Strings.startsWith("Hello", "hello"));
            assertFalse(Strings.startsWith("TEST", "test"));
            assertTrue(Strings.startsWith("Hello", "Hello"));
        }
    }

    @Nested
    @DisplayName("startsWithIgnoreCase tests")
    public class StartsWithIgnoreCaseTests {

        @Test
        @DisplayName("should return false for null string or prefix")
        public void testStartsWithIgnoreCase_NullInputs() {
            assertFalse(Strings.startsWithIgnoreCase(null, "prefix"));
            assertFalse(Strings.startsWithIgnoreCase("test", null));
            assertFalse(Strings.startsWithIgnoreCase(null, null));
        }

        @Test
        @DisplayName("should ignore case when matching")
        public void testStartsWithIgnoreCase_IgnoreCase() {
            assertTrue(Strings.startsWithIgnoreCase("Hello", "hello"));
            assertTrue(Strings.startsWithIgnoreCase("TEST", "test"));
            assertTrue(Strings.startsWithIgnoreCase("MiXeD", "mixed"));
            assertTrue(Strings.startsWithIgnoreCase("ABC", "abc"));
        }

        @Test
        @DisplayName("should return true for valid prefix regardless of case")
        public void testStartsWithIgnoreCase_ValidPrefix() {
            assertTrue(Strings.startsWithIgnoreCase("hello world", "HELLO"));
            assertTrue(Strings.startsWithIgnoreCase("JavaScript", "java"));
            assertFalse(Strings.startsWithIgnoreCase("hello", "world"));
        }
    }

    @Nested
    @DisplayName("startsWithAny tests")
    public class StartsWithAnyTests {

        @Test
        @DisplayName("should return false for null string")
        public void testStartsWithAny_NullString() {
            assertFalse(Strings.startsWithAny(null, "a", "b"));
        }

        @Test
        @DisplayName("should return false for null or empty prefixes")
        public void testStartsWithAny_NullOrEmptyPrefixes() {
            assertFalse(Strings.startsWithAny("test", (String[]) null));
            assertFalse(Strings.startsWithAny("test"));
        }

        @Test
        @DisplayName("should return true if string starts with any prefix")
        public void testStartsWithAny_MatchingPrefix() {
            assertTrue(Strings.startsWithAny("hello", "hel", "test", "world"));
            assertTrue(Strings.startsWithAny("test", "no", "te", "maybe"));
            assertTrue(Strings.startsWithAny("abc", "xyz", "abc", "def"));
        }

        @Test
        @DisplayName("should return false if string starts with none of the prefixes")
        public void testStartsWithAny_NoMatchingPrefix() {
            assertFalse(Strings.startsWithAny("hello", "world", "test", "abc"));
            assertFalse(Strings.startsWithAny("xyz", "a", "b", "c"));
        }
    }

    @Nested
    @DisplayName("startsWithAnyIgnoreCase tests")
    public class StartsWithAnyIgnoreCaseTests {

        @Test
        @DisplayName("should return false for null string")
        public void testStartsWithAnyIgnoreCase_NullString() {
            assertFalse(Strings.startsWithAnyIgnoreCase(null, "a", "b"));
        }

        @Test
        @DisplayName("should ignore case when matching any prefix")
        public void testStartsWithAnyIgnoreCase_IgnoreCase() {
            assertTrue(Strings.startsWithAnyIgnoreCase("Hello", "HELLO", "test"));
            assertTrue(Strings.startsWithAnyIgnoreCase("TEST", "xyz", "test"));
            assertTrue(Strings.startsWithAnyIgnoreCase("MiXeD", "abc", "MIXED"));
        }

        @Test
        @DisplayName("should return false if no prefix matches")
        public void testStartsWithAnyIgnoreCase_NoMatch() {
            assertFalse(Strings.startsWithAnyIgnoreCase("hello", "WORLD", "TEST"));
            assertFalse(Strings.startsWithAnyIgnoreCase("abc", "XYZ", "DEF"));
        }
    }

    @Nested
    @DisplayName("endsWith tests")
    public class EndsWithTests {

        @Test
        @DisplayName("should return false for null string")
        public void testEndsWith_NullString() {
            assertFalse(Strings.endsWith(null, "suffix"));
            assertFalse(Strings.endsWith(null, null));
        }

        @Test
        @DisplayName("should return false for null suffix")
        public void testEndsWith_NullSuffix() {
            assertFalse(Strings.endsWith("test", null));
        }

        @Test
        @DisplayName("should return false when suffix is longer than string")
        public void testEndsWith_SuffixLongerThanString() {
            assertFalse(Strings.endsWith("ab", "abc"));
            assertFalse(Strings.endsWith("", "a"));
        }

        @Test
        @DisplayName("should return true for valid suffix")
        public void testEndsWith_ValidSuffix() {
            assertTrue(Strings.endsWith("hello", "llo"));
            assertTrue(Strings.endsWith("test", "test"));
            assertTrue(Strings.endsWith("abc", ""));
            assertTrue(Strings.endsWith("", ""));
        }

        @Test
        @DisplayName("should be case sensitive")
        public void testEndsWith_CaseSensitive() {
            assertFalse(Strings.endsWith("Hello", "LLO"));
            assertFalse(Strings.endsWith("TEST", "test"));
            assertTrue(Strings.endsWith("Hello", "llo"));
        }
    }

    @Nested
    @DisplayName("endsWithIgnoreCase tests")
    public class EndsWithIgnoreCaseTests {

        @Test
        @DisplayName("should return false for null inputs")
        public void testEndsWithIgnoreCase_NullInputs() {
            assertFalse(Strings.endsWithIgnoreCase(null, "suffix"));
            assertFalse(Strings.endsWithIgnoreCase("test", null));
            assertFalse(Strings.endsWithIgnoreCase(null, null));
        }

        @Test
        @DisplayName("should ignore case when matching")
        public void testEndsWithIgnoreCase_IgnoreCase() {
            assertTrue(Strings.endsWithIgnoreCase("Hello", "LLO"));
            assertTrue(Strings.endsWithIgnoreCase("TEST", "test"));
            assertTrue(Strings.endsWithIgnoreCase("MiXeD", "XED"));
            assertTrue(Strings.endsWithIgnoreCase("abc", "ABC"));
        }
    }

    @Nested
    @DisplayName("endsWithAny tests")
    public class EndsWithAnyTests {

        @Test
        @DisplayName("should return false for null string")
        public void testEndsWithAny_NullString() {
            assertFalse(Strings.endsWithAny(null, "a", "b"));
        }

        @Test
        @DisplayName("should return true if string ends with any suffix")
        public void testEndsWithAny_MatchingSuffix() {
            assertTrue(Strings.endsWithAny("hello", "llo", "test", "world"));
            assertTrue(Strings.endsWithAny("test", "no", "st", "maybe"));
            assertTrue(Strings.endsWithAny("abc", "xyz", "bc", "def"));
        }

        @Test
        @DisplayName("should return false if string ends with none of the suffixes")
        public void testEndsWithAny_NoMatchingSuffix() {
            assertFalse(Strings.endsWithAny("hello", "world", "test", "abc"));
            assertFalse(Strings.endsWithAny("xyz", "a", "b", "c"));
        }
    }

    @Nested
    @DisplayName("endsWithAnyIgnoreCase tests")
    public class EndsWithAnyIgnoreCaseTests {

        @Test
        @DisplayName("should return false for null string")
        public void testEndsWithAnyIgnoreCase_NullString() {
            assertFalse(Strings.endsWithAnyIgnoreCase(null, "a", "b"));
        }

        @Test
        @DisplayName("should ignore case when matching any suffix")
        public void testEndsWithAnyIgnoreCase_IgnoreCase() {
            assertTrue(Strings.endsWithAnyIgnoreCase("Hello", "LLO", "test"));
            assertTrue(Strings.endsWithAnyIgnoreCase("TEST", "xyz", "test"));
            assertTrue(Strings.endsWithAnyIgnoreCase("MiXeD", "abc", "XED"));
        }
    }

    @Nested
    @DisplayName("equals tests")
    public class EqualsTests {

        @Test
        @DisplayName("should return true for both null strings")
        public void testEquals_BothNull() {
            assertTrue(Strings.equals(null, null));
        }

        @Test
        @DisplayName("should return false when one string is null")
        public void testEquals_OneNull() {
            assertFalse(Strings.equals(null, "test"));
            assertFalse(Strings.equals("test", null));
        }

        @Test
        @DisplayName("should return true for equal strings")
        public void testEquals_EqualStrings() {
            assertTrue(Strings.equals("", ""));
            assertTrue(Strings.equals("test", "test"));
            assertTrue(Strings.equals("Hello World", "Hello World"));
        }

        @Test
        @DisplayName("should return false for different strings")
        public void testEquals_DifferentStrings() {
            assertFalse(Strings.equals("test", "TEST"));
            assertFalse(Strings.equals("hello", "world"));
            assertFalse(Strings.equals("abc", "def"));
        }

        @Test
        @DisplayName("should be case sensitive")
        public void testEquals_CaseSensitive() {
            assertFalse(Strings.equals("Hello", "hello"));
            assertFalse(Strings.equals("TEST", "test"));
        }
    }

    @Nested
    @DisplayName("equalsIgnoreCase tests")
    public class EqualsIgnoreCaseTests {

        @Test
        @DisplayName("should return true for both null strings")
        public void testEqualsIgnoreCase_BothNull() {
            assertTrue(Strings.equalsIgnoreCase(null, null));
        }

        @Test
        @DisplayName("should return false when one string is null")
        public void testEqualsIgnoreCase_OneNull() {
            assertFalse(Strings.equalsIgnoreCase(null, "test"));
            assertFalse(Strings.equalsIgnoreCase("test", null));
        }

        @Test
        @DisplayName("should ignore case when comparing")
        public void testEqualsIgnoreCase_IgnoreCase() {
            assertTrue(Strings.equalsIgnoreCase("test", "TEST"));
            assertTrue(Strings.equalsIgnoreCase("Hello", "hello"));
            assertTrue(Strings.equalsIgnoreCase("MiXeD", "mixed"));
        }

        @Test
        @DisplayName("should return false for different strings")
        public void testEqualsIgnoreCase_DifferentStrings() {
            assertFalse(Strings.equalsIgnoreCase("hello", "world"));
            assertFalse(Strings.equalsIgnoreCase("abc", "def"));
        }
    }

    @Nested
    @DisplayName("equalsAny tests")
    public class EqualsAnyTests {

        @Test
        @DisplayName("should return false for empty search strings")
        public void testEqualsAny_EmptySearchStrings() {
            assertFalse(Strings.equalsAny("test"));
            assertFalse(Strings.equalsAny("test", (String[]) null));
        }

        @Test
        @DisplayName("should return true if string equals any search string")
        public void testEqualsAny_MatchingString() {
            assertTrue(Strings.equalsAny("test", "hello", "test", "world"));
            assertTrue(Strings.equalsAny("abc", "abc"));
            assertTrue(Strings.equalsAny(null, "hello", null, "world"));
        }

        @Test
        @DisplayName("should return false if string equals none of the search strings")
        public void testEqualsAny_NoMatchingString() {
            assertFalse(Strings.equalsAny("test", "hello", "world", "abc"));
            assertFalse(Strings.equalsAny("xyz", "a", "b", "c"));
        }

        @Test
        @DisplayName("should be case sensitive")
        public void testEqualsAny_CaseSensitive() {
            assertFalse(Strings.equalsAny("test", "TEST", "Hello"));
            assertTrue(Strings.equalsAny("test", "TEST", "test"));
        }
    }

    @Nested
    @DisplayName("equalsAnyIgnoreCase tests")
    public class EqualsAnyIgnoreCaseTests {

        @Test
        @DisplayName("should return false for empty search strings")
        public void testEqualsAnyIgnoreCase_EmptySearchStrings() {
            assertFalse(Strings.equalsAnyIgnoreCase("test"));
            assertFalse(Strings.equalsAnyIgnoreCase("test", (String[]) null));
        }

        @Test
        @DisplayName("should ignore case when comparing")
        public void testEqualsAnyIgnoreCase_IgnoreCase() {
            assertTrue(Strings.equalsAnyIgnoreCase("test", "HELLO", "TEST", "world"));
            assertTrue(Strings.equalsAnyIgnoreCase("ABC", "xyz", "abc"));
            assertTrue(Strings.equalsAnyIgnoreCase("Hello", "HELLO", "world"));
        }

        @Test
        @DisplayName("should return false if string equals none of the search strings")
        public void testEqualsAnyIgnoreCase_NoMatch() {
            assertFalse(Strings.equalsAnyIgnoreCase("test", "HELLO", "WORLD", "ABC"));
            assertFalse(Strings.equalsAnyIgnoreCase("xyz", "A", "B", "C"));
        }

        @Test
        @DisplayName("should handle single and multiple search strings")
        public void testEqualsAnyIgnoreCase_SingleAndMultiple() {
            assertTrue(Strings.equalsAnyIgnoreCase("test", "TEST"));
            assertTrue(Strings.equalsAnyIgnoreCase("test", "hello", "TEST"));
        }
    }

    @Nested
    @DisplayName("compareIgnoreCase tests")
    public class CompareIgnoreCaseTests {

        @Test
        @DisplayName("should return 0 for both null strings")
        public void testCompareIgnoreCase_BothNull() {
            assertEquals(0, Strings.compareIgnoreCase(null, null));
        }

        @Test
        @DisplayName("should return negative when first string is null")
        public void testCompareIgnoreCase_FirstNull() {
            assertTrue(Strings.compareIgnoreCase(null, "test") < 0);
        }

        @Test
        @DisplayName("should return positive when second string is null")
        public void testCompareIgnoreCase_SecondNull() {
            assertTrue(Strings.compareIgnoreCase("test", null) > 0);
        }

        @Test
        @DisplayName("should return 0 for equal strings ignoring case")
        public void testCompareIgnoreCase_EqualStringsIgnoreCase() {
            assertEquals(0, Strings.compareIgnoreCase("test", "TEST"));
            assertEquals(0, Strings.compareIgnoreCase("Hello", "hello"));
            assertEquals(0, Strings.compareIgnoreCase("MiXeD", "mixed"));
        }

        @Test
        @DisplayName("should return negative for lexicographically smaller first string")
        public void testCompareIgnoreCase_FirstSmaller() {
            assertTrue(Strings.compareIgnoreCase("apple", "BANANA") < 0);
            assertTrue(Strings.compareIgnoreCase("a", "B") < 0);
        }

        @Test
        @DisplayName("should return positive for lexicographically larger first string")
        public void testCompareIgnoreCase_FirstLarger() {
            assertTrue(Strings.compareIgnoreCase("BANANA", "apple") > 0);
            assertTrue(Strings.compareIgnoreCase("B", "a") > 0);
        }

        @Test
        @DisplayName("should handle empty strings")
        public void testCompareIgnoreCase_EmptyStrings() {
            assertEquals(0, Strings.compareIgnoreCase("", ""));
            assertTrue(Strings.compareIgnoreCase("", "test") < 0);
            assertTrue(Strings.compareIgnoreCase("test", "") > 0);
        }

        @Test
        @DisplayName("should handle strings of different lengths")
        public void testCompareIgnoreCase_DifferentLengths() {
            assertTrue(Strings.compareIgnoreCase("abc", "ABCD") < 0);
            assertTrue(Strings.compareIgnoreCase("ABCD", "abc") > 0);
        }
    }

    private static final String SAMPLE_TEXT = "Hello World";
    private static final String EMPTY_STRING = "";
    private static final String WHITESPACE_STRING = "   ";
    private static final String MIXED_CASE = "AbCdEf";

    @Test
    public void testReverse() {
        assertNull(Strings.reverse(null));

        assertEquals("", Strings.reverse(""));

        assertEquals("a", Strings.reverse("a"));

        assertEquals("dlroW olleH", Strings.reverse("Hello World"));

        assertEquals("racecar", Strings.reverse("racecar"));

        assertEquals("!@#$%", Strings.reverse("%$#@!"));
    }

    @Test
    public void testReverseDelimitedChar() {
        assertNull(Strings.reverseDelimited(null, '.'));

        assertEquals("", Strings.reverseDelimited("", '.'));

        assertEquals("a", Strings.reverseDelimited("a", '.'));

        assertEquals("abc", Strings.reverseDelimited("abc", '.'));

        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", '.'));

        assertEquals("d.c.b.a", Strings.reverseDelimited("a.b.c.d", '.'));

        assertEquals("String.lang.java", Strings.reverseDelimited("java.lang.String", '.'));
    }

    @Test
    public void testReverseDelimitedString() {
        assertNull(Strings.reverseDelimited(null, "."));

        assertEquals("", Strings.reverseDelimited("", "."));

        assertEquals("a", Strings.reverseDelimited("a", "."));

        assertEquals("abc", Strings.reverseDelimited("abc", "."));

        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));

        assertEquals("c::b::a", Strings.reverseDelimited("a::b::c", "::"));

        assertEquals("world|hello", Strings.reverseDelimited("hello|world", "|"));
    }

    @Test
    public void testSort() {
        assertNull(Strings.sort(null));

        assertEquals("", Strings.sort(""));

        assertEquals("a", Strings.sort("a"));

        assertEquals("abc", Strings.sort("abc"));

        assertEquals("abc", Strings.sort("cba"));

        assertEquals("  AHWdellloor", Strings.sort("Hello World A"));

        assertEquals("aabbcc", Strings.sort("abcabc"));
    }

    @Test
    public void testRotate() {
        assertNull(Strings.rotate(null, 2));

        assertEquals("", Strings.rotate("", 2));

        assertEquals("a", Strings.rotate("a", 2));

        assertEquals("abcdefg", Strings.rotate("abcdefg", 0));

        assertEquals("fgabcde", Strings.rotate("abcdefg", 2));

        assertEquals("cdefgab", Strings.rotate("abcdefg", -2));

        assertEquals("abcdefg", Strings.rotate("abcdefg", 7));

        assertEquals("abcdefg", Strings.rotate("abcdefg", -7));

        assertEquals("fgabcde", Strings.rotate("abcdefg", 9));

        assertEquals("cdefgab", Strings.rotate("abcdefg", -9));
    }

    @Test
    public void testShuffle() {
        assertNull(Strings.shuffle(null));

        assertEquals("", Strings.shuffle(""));

        assertEquals("a", Strings.shuffle("a"));

        Random random = new Random(12345);
        String shuffled = Strings.shuffle("abcdef", random);
        assertNotNull(shuffled);
        assertEquals(6, shuffled.length());

        for (char c : "abcdef".toCharArray()) {
            assertTrue(shuffled.indexOf(c) >= 0);
        }

        String original = "abcdefghijklmnopqrstuvwxyz";
        String shuffled2 = Strings.shuffle(original);
        assertEquals(original.length(), shuffled2.length());
        assertNotEquals(original, shuffled2);
    }

    @Test
    public void testFormatToPercentageWithScale() {
        assertEquals("50%", Numbers.format(0.5, "0%"));

        assertEquals("33.3%", Numbers.format(1.0 / 3.0, "0.0%"));

        assertEquals("33.333%", Numbers.format(1.0 / 3.0, "0.000%"));

        assertEquals("12.3457%", Numbers.format(0.123457, "0.0000%"));

        assertEquals("66.67%", Numbers.format(2.0 / 3.0, "0.00%"));
    }

    @Test
    @DisplayName("Test parseBoolean method")
    public void testParseBoolean() {
        assertTrue(Strings.parseBoolean("true"));
        assertTrue(Strings.parseBoolean("TRUE"));
        assertTrue(Strings.parseBoolean("True"));
        assertTrue(Strings.parseBoolean("TrUe"));

        assertFalse(Strings.parseBoolean("false"));
        assertFalse(Strings.parseBoolean("FALSE"));
        assertFalse(Strings.parseBoolean("False"));
        assertFalse(Strings.parseBoolean("FaLsE"));
        assertFalse(Strings.parseBoolean(""));
        assertFalse(Strings.parseBoolean("invalid"));
        assertFalse(Strings.parseBoolean("0"));
        assertFalse(Strings.parseBoolean("1"));

        assertFalse(Strings.parseBoolean(null));
        assertFalse(Strings.parseBoolean(""));
    }

    @Test
    @DisplayName("Test parseChar method")
    public void testParseChar() {
        assertEquals('a', Strings.parseChar("a"));
        assertEquals('Z', Strings.parseChar("Z"));
        assertEquals('1', Strings.parseChar("1"));
        assertEquals(' ', Strings.parseChar(" "));
        assertEquals('!', Strings.parseChar("!"));

        assertEquals((char) 65, Strings.parseChar("65"));
        assertEquals((char) 97, Strings.parseChar("97"));
        assertEquals((char) 48, Strings.parseChar("48"));

        assertEquals('\0', Strings.parseChar(null));
        assertEquals('\0', Strings.parseChar(""));
    }

    @Test
    @DisplayName("Test base64Encode method")
    public void testBase64Encode() {
        assertEquals("SGVsbG8=", Strings.base64Encode("Hello".getBytes()));
        assertEquals("V29ybGQ=", Strings.base64Encode("World".getBytes()));
        assertEquals("", Strings.base64Encode(new byte[0]));
        assertEquals("", Strings.base64Encode(null));

        assertEquals("SGVsbG8gV29ybGQh", Strings.base64Encode("Hello World!".getBytes()));
        assertEquals("MTIzNDU2Nzg5MA==", Strings.base64Encode("1234567890".getBytes()));
    }

    @Test
    @DisplayName("Test base64EncodeString method")
    public void testBase64EncodeString() {
        assertEquals("SGVsbG8=", Strings.base64EncodeString("Hello"));
        assertEquals("V29ybGQ=", Strings.base64EncodeString("World"));
        assertEquals("", Strings.base64EncodeString(""));
        assertEquals("", Strings.base64EncodeString(null));

        assertEquals("SGVsbG8gV29ybGQh", Strings.base64EncodeString("Hello World!"));
        assertEquals("MTIzNDU2Nzg5MA==", Strings.base64EncodeString("1234567890"));
    }

    @Test
    @DisplayName("Test base64EncodeUtf8String method")
    public void testBase64EncodeUtf8String() {
        assertEquals("SGVsbG8=", Strings.base64EncodeUtf8String("Hello"));
        assertEquals("", Strings.base64EncodeUtf8String(""));
        assertEquals("", Strings.base64EncodeUtf8String(null));

        assertEquals("8J+YgA==", Strings.base64EncodeUtf8String("😀"));
    }

    @Test
    @DisplayName("Test base64EncodeString with charset")
    public void testBase64EncodeStringWithCharset() {
        assertEquals("SGVsbG8=", Strings.base64EncodeString("Hello", StandardCharsets.UTF_8));
        assertEquals("SGVsbG8=", Strings.base64EncodeString("Hello", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64EncodeString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64EncodeString(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test base64Decode method")
    public void testBase64Decode() {
        assertArrayEquals("Hello".getBytes(), Strings.base64Decode("SGVsbG8="));
        assertArrayEquals("World".getBytes(), Strings.base64Decode("V29ybGQ="));
        assertArrayEquals(new byte[0], Strings.base64Decode(""));
        assertArrayEquals(new byte[0], Strings.base64Decode(null));

        assertArrayEquals("Hello World!".getBytes(), Strings.base64Decode("SGVsbG8gV29ybGQh"));
        assertArrayEquals("1234567890".getBytes(), Strings.base64Decode("MTIzNDU2Nzg5MA=="));
    }

    @Test
    @DisplayName("Test base64DecodeToString method")
    public void testBase64DecodeToString() {
        assertEquals("Hello", Strings.base64DecodeToString("SGVsbG8="));
        assertEquals("World", Strings.base64DecodeToString("V29ybGQ="));
        assertEquals("", Strings.base64DecodeToString(""));
        assertEquals("", Strings.base64DecodeToString(null));

        assertEquals("Hello World!", Strings.base64DecodeToString("SGVsbG8gV29ybGQh"));
        assertEquals("1234567890", Strings.base64DecodeToString("MTIzNDU2Nzg5MA=="));
    }

    @Test
    @DisplayName("Test base64DecodeToUtf8String method")
    public void testBase64DecodeToUtf8String() {
        assertEquals("Hello", Strings.base64DecodeToUtf8String("SGVsbG8="));
        assertEquals("", Strings.base64DecodeToUtf8String(""));
        assertEquals("", Strings.base64DecodeToUtf8String(null));

        assertEquals("😀", Strings.base64DecodeToUtf8String("8J+YgA=="));
    }

    @Test
    @DisplayName("Test base64DecodeToString with charset")
    public void testBase64DecodeToStringWithCharset() {
        assertEquals("Hello", Strings.base64DecodeToString("SGVsbG8=", StandardCharsets.UTF_8));
        assertEquals("Hello", Strings.base64DecodeToString("SGVsbG8=", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64DecodeToString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64DecodeToString(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test base64UrlEncode method")
    public void testBase64UrlEncode() {
        N.println(Base64.getUrlEncoder().withoutPadding().encodeToString("Hello".getBytes()));
        assertEquals("SGVsbG8", Strings.base64UrlEncode("Hello".getBytes()));
        assertEquals("V29ybGQ", Strings.base64UrlEncode("World".getBytes()));
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8"));
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8="));
        assertEquals("", Strings.base64UrlEncode(new byte[0]));
        assertEquals("", Strings.base64UrlEncode(null));

        byte[] testData = { -1, -2, -3, -4, -5 };
        String encoded = Strings.base64UrlEncode(testData);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertFalse(encoded.contains("="));
    }

    @Test
    @DisplayName("Test base64UrlDecode method")
    public void testBase64UrlDecode() {
        assertArrayEquals("Hello".getBytes(), Strings.base64UrlDecode("SGVsbG8"));
        assertArrayEquals("World".getBytes(), Strings.base64UrlDecode("V29ybGQ"));
        assertArrayEquals(new byte[0], Strings.base64UrlDecode(""));
        assertArrayEquals(new byte[0], Strings.base64UrlDecode(null));
    }

    @Test
    @DisplayName("Test base64UrlDecodeToString method")
    public void testBase64UrlDecodeToString() {
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8"));
        assertEquals("World", Strings.base64UrlDecodeToString("V29ybGQ"));
        assertEquals("", Strings.base64UrlDecodeToString(""));
        assertEquals("", Strings.base64UrlDecodeToString(null));
    }

    @Test
    @DisplayName("Test base64UrlDecodeToUtf8String method")
    public void testBase64UrlDecodeToUtf8String() {
        assertEquals("Hello", Strings.base64UrlDecodeToUtf8String("SGVsbG8"));
        assertEquals("", Strings.base64UrlDecodeToUtf8String(""));
        assertEquals("", Strings.base64UrlDecodeToUtf8String(null));
    }

    @Test
    @DisplayName("Test base64UrlDecodeToString with charset")
    public void testBase64UrlDecodeToStringWithCharset() {
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8", StandardCharsets.UTF_8));
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64UrlDecodeToString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64UrlDecodeToString(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test isBase64 byte method")
    public void testIsBase64Byte() {
        assertTrue(Strings.isBase64((byte) 'A'));
        assertTrue(Strings.isBase64((byte) 'Z'));
        assertTrue(Strings.isBase64((byte) 'a'));
        assertTrue(Strings.isBase64((byte) 'z'));
        assertTrue(Strings.isBase64((byte) '0'));
        assertTrue(Strings.isBase64((byte) '9'));
        assertTrue(Strings.isBase64((byte) '+'));
        assertTrue(Strings.isBase64((byte) '/'));
        assertTrue(Strings.isBase64((byte) '='));

        assertFalse(Strings.isBase64((byte) '@'));
        assertFalse(Strings.isBase64((byte) '['));
        assertFalse(Strings.isBase64((byte) '`'));
        assertFalse(Strings.isBase64((byte) '{'));
        assertFalse(Strings.isBase64((byte) '!'));
    }

    @Test
    @DisplayName("Test isBase64 byte array method")
    public void testIsBase64ByteArray() {
        assertTrue(Strings.isBase64("SGVsbG8=".getBytes()));
        assertTrue(Strings.isBase64("V29ybGQ=".getBytes()));
        assertTrue(Strings.isBase64(new byte[0]));

        assertFalse(Strings.isBase64("Hello@World".getBytes()));

        assertTrue(Strings.isBase64("SGVs bG8=".getBytes()));
    }

    @Test
    @DisplayName("Test isBase64 string method")
    public void testIsBase64String() {
        assertTrue(Strings.isBase64("SGVsbG8="));
        assertTrue(Strings.isBase64("V29ybGQ="));
        assertTrue(Strings.isBase64(""));

        assertFalse(Strings.isBase64("Hello@World"));

        assertTrue(Strings.isBase64("SGVs bG8="));
    }

    @Test
    @DisplayName("Test findFirstEmailAddress method")
    public void testFindFirstEmailAddress() {
        assertEquals("test@example.com", Strings.findFirstEmailAddress("Contact me at test@example.com"));
        assertEquals("user@domain.org", Strings.findFirstEmailAddress("Email: user@domain.org for more info"));
        assertEquals("admin@test.co.uk", Strings.findFirstEmailAddress("Send to admin@test.co.uk"));

        assertEquals("first@example.com", Strings.findFirstEmailAddress("first@example.com and second@example.com"));

        assertNull(Strings.findFirstEmailAddress("No email here"));
        assertNull(Strings.findFirstEmailAddress(""));
        assertNull(Strings.findFirstEmailAddress(null));

        assertNull(Strings.findFirstEmailAddress("invalid@"));
        assertNull(Strings.findFirstEmailAddress("@invalid.com"));
    }

    @Test
    @DisplayName("Test findAllEmailAddresses method")
    public void testFindAllEmailAddresses() {
        List<String> emails = Strings.findAllEmailAddresses("Contact first@example.com or second@test.org");
        assertEquals(2, emails.size());
        assertTrue(emails.contains("first@example.com"));
        assertTrue(emails.contains("second@test.org"));

        emails = Strings.findAllEmailAddresses("Send to user@domain.com");
        assertEquals(1, emails.size());
        assertEquals("user@domain.com", emails.get(0));

        emails = Strings.findAllEmailAddresses("No emails here");
        assertTrue(emails.isEmpty());

        emails = Strings.findAllEmailAddresses(null);
        assertTrue(emails.isEmpty());

        emails = Strings.findAllEmailAddresses("");
        assertTrue(emails.isEmpty());
    }

    @Test
    @DisplayName("Test copyThenTrim method")
    public void testCopyThenTrim() {
        String[] input = { " hello ", " world ", "  test  " };
        String[] result = Strings.copyThenTrim(input);

        assertNotSame(input, result);
        assertEquals("hello", result[0]);
        assertEquals("world", result[1]);
        assertEquals("test", result[2]);

        String[] inputWithNull = { " hello ", null, "  test  " };
        String[] resultWithNull = Strings.copyThenTrim(inputWithNull);
        assertEquals("hello", resultWithNull[0]);
        assertEquals(null, resultWithNull[1]);
        assertEquals("test", resultWithNull[2]);

        assertNull(Strings.copyThenTrim(null));

        String[] emptyArray = new String[0];
        String[] emptyResult = Strings.copyThenTrim(emptyArray);
        assertNotSame(emptyArray, emptyResult);
        assertEquals(0, emptyResult.length);
    }

    @Test
    @DisplayName("Test copyThenStrip method")
    public void testCopyThenStrip() {
        String[] input = { " hello ", " world ", "  test  " };
        String[] result = Strings.copyThenStrip(input);

        assertNotSame(input, result);
        assertEquals("hello", result[0]);
        assertEquals("world", result[1]);
        assertEquals("test", result[2]);

        String[] inputWithNull = { " hello ", null, "  test  " };
        String[] resultWithNull = Strings.copyThenStrip(inputWithNull);
        assertEquals("hello", resultWithNull[0]);
        assertEquals(null, resultWithNull[1]);
        assertEquals("test", resultWithNull[2]);

        assertNull(Strings.copyThenStrip(null));

        String[] emptyArray = new String[0];
        String[] emptyResult = Strings.copyThenStrip(emptyArray);
        assertNotSame(emptyArray, emptyResult);
        assertEquals(0, emptyResult.length);
    }

    @Test
    @DisplayName("Test formatToPercentage methods")
    public void testFormatToPercentage() {
        assertEquals("50.0%", Numbers.format(0.5, "0.0%"));
        assertEquals("100.0%", Numbers.format(1.0, "0.0%"));
        assertEquals("0.0%", Numbers.format(0.0, "0.0%"));
        assertEquals("25.5%", Numbers.format(0.255, "0.0%"));

        assertEquals("50%", Numbers.format(0.5, "0%"));
        assertEquals("50.0%", Numbers.format(0.5, "0.0%"));
        assertEquals("51.6%", Numbers.format(0.5156, "0.0%"));
        assertEquals("50.00%", Numbers.format(0.5, "0.00%"));
        assertEquals("33.333%", Numbers.format(1.0 / 3.0, "0.000%"));

        assertEquals("-25.0%", Numbers.format(-0.25, "0.0%"));
        assertEquals("-25%", Numbers.format(-0.25, "0%"));
    }

    @Test
    @DisplayName("Test extractFirstInteger method")
    public void testExtractFirstInteger() {
        assertEquals("123", Strings.extractFirstInteger("abc123def"));
        assertEquals("456", Strings.extractFirstInteger("test456"));
        assertEquals("789", Strings.extractFirstInteger("789test"));
        assertEquals("0", Strings.extractFirstInteger("value0"));

        assertEquals("-123", Strings.extractFirstInteger("value-123test"));
        assertEquals("-456", Strings.extractFirstInteger("-456abc"));

        assertNull(Strings.extractFirstInteger("no numbers here"));
        assertNull(Strings.extractFirstInteger(""));
        assertNull(Strings.extractFirstInteger(null));

        assertEquals("123", Strings.extractFirstInteger("first123second456"));
    }

    @Test
    @DisplayName("Test extractFirstDouble method")
    public void testExtractFirstDouble() {
        assertEquals("123.45", Strings.extractFirstDouble("value123.45test"));
        assertEquals("456.0", Strings.extractFirstDouble("test456.0"));
        assertEquals("0.789", Strings.extractFirstDouble("0.789test"));

        assertEquals("123", Strings.extractFirstDouble("abc123def"));

        assertEquals("-123.45", Strings.extractFirstDouble("value-123.45test"));

        assertNull(Strings.extractFirstDouble("no numbers here"));
        assertNull(Strings.extractFirstDouble(""));
        assertNull(Strings.extractFirstDouble(null));

        assertEquals("123.45", Strings.extractFirstDouble("first123.45second678.90"));
    }

    @Test
    @DisplayName("Test extractFirstSciNumber method")
    public void testExtractFirstSciNumber() {
        assertEquals("1.23e10", Strings.extractFirstDouble("value1.23e10test", true));
        assertEquals("4.56E-5", Strings.extractFirstDouble("test4.56E-5", true));
        assertEquals("1e3", Strings.extractFirstDouble("1e3test", true));

        assertEquals("-1.23e10", Strings.extractFirstDouble("value-1.23e10test", true));

        assertNull(Strings.extractFirstDouble("no sci numbers here", true));
        assertNull(Strings.extractFirstDouble("", true));
        assertNull(Strings.extractFirstDouble(null, true));
        assertEquals("123.45", Strings.extractFirstDouble("123.45", true));

        assertEquals("1.23e10", Strings.extractFirstDouble("first1.23e10second4.56e-5", true));
    }

    @Test
    @DisplayName("Test replaceFirstInteger method")
    public void testReplaceFirstInteger() {
        assertEquals("abcXXXdef", Strings.replaceFirstInteger("abc123def", "XXX"));
        assertEquals("testXXX", Strings.replaceFirstInteger("test456", "XXX"));
        assertEquals("XXXtest", Strings.replaceFirstInteger("789test", "XXX"));

        assertEquals("valueXXXtest", Strings.replaceFirstInteger("value-123test", "XXX"));

        assertEquals("no numbers here", Strings.replaceFirstInteger("no numbers here", "XXX"));
        assertEquals("", Strings.replaceFirstInteger("", "XXX"));
        assertEquals("", Strings.replaceFirstInteger(null, "XXX"));

        assertEquals("firstXXXsecond456", Strings.replaceFirstInteger("first123second456", "XXX"));
    }

    @Test
    @DisplayName("Test replaceFirstDouble method")
    public void testReplaceFirstDouble() {
        assertEquals("valueXXXtest", Strings.replaceFirstDouble("value123.45test", "XXX"));
        assertEquals("testXXX", Strings.replaceFirstDouble("test456.0", "XXX"));
        assertEquals("XXXtest", Strings.replaceFirstDouble("0.789test", "XXX"));

        assertEquals("abcXXXdef", Strings.replaceFirstDouble("abc123def", "XXX"));

        assertEquals("valueXXXtest", Strings.replaceFirstDouble("value-123.45test", "XXX"));

        assertEquals("no numbers here", Strings.replaceFirstDouble("no numbers here", "XXX"));
        assertEquals("", Strings.replaceFirstDouble("", "XXX"));
        assertEquals("", Strings.replaceFirstDouble(null, "XXX"));

        assertEquals("firstXXXsecond678.90", Strings.replaceFirstDouble("first123.45second678.90", "XXX"));
    }

    @Test
    @DisplayName("Test replaceFirstSciNumber method")
    public void testReplaceFirstSciNumber() {
        assertEquals("valueXXXtest", Strings.replaceFirstDouble("value1.23e10test", "XXX", true));
        assertEquals("testXXX", Strings.replaceFirstDouble("test4.56E-5", "XXX", true));
        assertEquals("XXXtest", Strings.replaceFirstDouble("1e3test", "XXX", true));

        assertEquals("valueXXXtest", Strings.replaceFirstDouble("value-1.23e10test", "XXX", true));

        assertEquals("no sci numbers here", Strings.replaceFirstDouble("no sci numbers here", "XXX", true));
        assertEquals("", Strings.replaceFirstDouble("", "XXX", true));
        assertEquals("", Strings.replaceFirstDouble(null, "XXX", true));
        assertEquals("XXX", Strings.replaceFirstDouble("123.45", "XXX", true));

        assertEquals("firstXXXsecond4.56e-5", Strings.replaceFirstDouble("first1.23e10second4.56e-5", "XXX", true));
    }

    @Test
    @DisplayName("Test urlEncode methods")
    public void testUrlEncode() {

        String result = Strings.urlEncode("hello world");
        assertNotNull(result);

        String resultWithCharset = Strings.urlEncode("hello world", StandardCharsets.UTF_8);
        assertNotNull(resultWithCharset);

        String nullResult = Strings.urlEncode(null);
        assertNotNull(nullResult);
    }

    @Test
    @DisplayName("Test urlDecode methods")
    public void testUrlDecode() {

        Map<String, String> result = Strings.urlDecode("key=value&foo=bar");
        assertNotNull(result);

        Map<String, String> resultWithCharset = Strings.urlDecode("key=value", StandardCharsets.UTF_8);
        assertNotNull(resultWithCharset);

        Object objectResult = Strings.urlDecode("id=1", Account.class);
        assertNotNull(objectResult);

        Object objectResultWithCharset = Strings.urlDecode("id=1", StandardCharsets.UTF_8, Account.class);
        assertNotNull(objectResultWithCharset);
    }

    public static class Account {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
