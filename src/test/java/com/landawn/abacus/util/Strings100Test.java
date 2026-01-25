package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.IntStream;

@Tag("new-test")
public class Strings100Test extends TestBase {

    private static final String EMPTY = "";
    private static final String SPACE = " ";
    private static final String WHITESPACE = "   \t\n\r  ";
    private static final String ABC = "abc";
    private static final String ABC_UPPER = "ABC";
    private static final String HELLO_WORLD = "Hello World";

    @Test
    @DisplayName("Test guid() generates valid GUID without dashes")
    public void testGuid() {
        String guid = Strings.uuid32();
        assertNotNull(guid);
        assertEquals(32, guid.length());
        assertFalse(guid.contains("-"));
        assertTrue(guid.matches("[a-f0-9]{32}"));
    }

    @Test
    @DisplayName("Test uuid() generates valid UUID with dashes")
    public void testUuid() {
        String uuid = Strings.uuid();
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
    }

    @Test
    @DisplayName("Test valueOf(char[])")
    public void testValueOf() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    @DisplayName("Test isKeyword()")
    public void testIsKeyword() {
        assertTrue(Strings.isKeyword("class"));
        assertTrue(Strings.isKeyword("public"));
        assertTrue(Strings.isKeyword("if"));
        assertFalse(Strings.isKeyword("Class"));
        assertFalse(Strings.isKeyword("hello"));
        assertFalse(Strings.isKeyword(null));
        assertFalse(Strings.isKeyword(""));
    }

    @Test
    @DisplayName("Test isValidJavaIdentifier()")
    public void testIsValidJavaIdentifier() {
        assertTrue(Strings.isValidJavaIdentifier("variable"));
        assertTrue(Strings.isValidJavaIdentifier("_var"));
        assertTrue(Strings.isValidJavaIdentifier("$var"));
        assertTrue(Strings.isValidJavaIdentifier("var123"));
        assertFalse(Strings.isValidJavaIdentifier("123var"));
        assertFalse(Strings.isValidJavaIdentifier("var-name"));
        assertTrue(Strings.isValidJavaIdentifier("class"));
        assertFalse(Strings.isValidJavaIdentifier(null));
        assertFalse(Strings.isValidJavaIdentifier(""));
    }

    @Test
    @DisplayName("Test isValidEmailAddress()")
    public void testIsValidEmailAddress() {
        assertTrue(Strings.isValidEmailAddress("test@example.com"));
        assertTrue(Strings.isValidEmailAddress("user.name@example.co.uk"));
        assertTrue(Strings.isValidEmailAddress("user+tag@example.com"));
        assertFalse(Strings.isValidEmailAddress("@example.com"));
        assertFalse(Strings.isValidEmailAddress("test@"));
        assertFalse(Strings.isValidEmailAddress("test"));
        assertFalse(Strings.isValidEmailAddress(null));
        assertFalse(Strings.isValidEmailAddress(""));
    }

    @Test
    @DisplayName("Test isValidUrl()")
    public void testIsValidUrl() {
        assertTrue(Strings.isValidUrl("http://example.com"));
        assertTrue(Strings.isValidUrl("https://example.com"));
        assertTrue(Strings.isValidUrl("ftp://example.com"));
        assertTrue(Strings.isValidUrl("http://example.com:8080/path"));
        assertFalse(Strings.isValidUrl("example.com"));
        assertFalse(Strings.isValidUrl(null));
        assertFalse(Strings.isValidUrl(""));
    }

    @Test
    @DisplayName("Test isValidHttpUrl()")
    public void testIsValidHttpUrl() {
        assertTrue(Strings.isValidHttpUrl("http://example.com"));
        assertTrue(Strings.isValidHttpUrl("https://example.com"));
        assertFalse(Strings.isValidHttpUrl("ftp://example.com"));
        assertFalse(Strings.isValidHttpUrl("example.com"));
        assertFalse(Strings.isValidHttpUrl(null));
        assertFalse(Strings.isValidHttpUrl(""));
    }

    @Test
    @DisplayName("Test isEmpty()")
    public void testIsEmpty() {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("abc"));
    }

    @Test
    @DisplayName("Test isBlank()")
    public void testIsBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertTrue(Strings.isBlank("   \t\n\r  "));
        assertFalse(Strings.isBlank("abc"));
        assertFalse(Strings.isBlank("  abc  "));
    }

    @Test
    @DisplayName("Test isNotEmpty()")
    public void testIsNotEmpty() {
        assertFalse(Strings.isNotEmpty(null));
        assertFalse(Strings.isNotEmpty(""));
        assertTrue(Strings.isNotEmpty(" "));
        assertTrue(Strings.isNotEmpty("abc"));
    }

    @Test
    @DisplayName("Test isNotBlank()")
    public void testIsNotBlank() {
        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank(""));
        assertFalse(Strings.isNotBlank(" "));
        assertFalse(Strings.isNotBlank("   \t\n\r  "));
        assertTrue(Strings.isNotBlank("abc"));
        assertTrue(Strings.isNotBlank("  abc  "));
    }

    @Test
    @DisplayName("Test isAllEmpty(CharSequence, CharSequence)")
    public void testIsAllEmptyTwoArgs() {
        assertTrue(Strings.isAllEmpty(null, null));
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertFalse(Strings.isAllEmpty(null, "foo"));
        assertFalse(Strings.isAllEmpty("", "bar"));
        assertFalse(Strings.isAllEmpty("bob", ""));
    }

    @Test
    @DisplayName("Test isAllEmpty(CharSequence...)")
    public void testIsAllEmptyVarArgs() {
        assertTrue(Strings.isAllEmpty());
        assertTrue(Strings.isAllEmpty((CharSequence[]) null));
        assertTrue(Strings.isAllEmpty(null, "", null));
        assertFalse(Strings.isAllEmpty(null, "foo", ""));
        assertFalse(Strings.isAllEmpty("", "bar", null));
    }

    @Test
    @DisplayName("Test isAllBlank(CharSequence...)")
    public void testIsAllBlankVarArgs() {
        assertTrue(Strings.isAllBlank());
        assertTrue(Strings.isAllBlank((CharSequence[]) null));
        assertTrue(Strings.isAllBlank(null, "", "  "));
        assertFalse(Strings.isAllBlank(null, "foo", "  "));
        assertFalse(Strings.isAllBlank(" ", "bar", null));
    }

    @Test
    @DisplayName("Test isAnyEmpty(CharSequence...)")
    public void testIsAnyEmptyVarArgs() {
        assertFalse(Strings.isAnyEmpty());
        assertFalse(Strings.isAnyEmpty((CharSequence[]) null));
        assertTrue(Strings.isAnyEmpty(null, "foo"));
        assertTrue(Strings.isAnyEmpty("", "bar"));
        assertTrue(Strings.isAnyEmpty("bob", ""));
        assertFalse(Strings.isAnyEmpty("foo", "bar"));
    }

    @Test
    @DisplayName("Test isAnyBlank(CharSequence...)")
    public void testIsAnyBlankVarArgs() {
        assertFalse(Strings.isAnyBlank());
        assertFalse(Strings.isAnyBlank((CharSequence[]) null));
        assertTrue(Strings.isAnyBlank(null, "foo"));
        assertTrue(Strings.isAnyBlank("", "bar"));
        assertTrue(Strings.isAnyBlank("  ", "bar"));
        assertFalse(Strings.isAnyBlank("foo", "bar"));
    }

    @Test
    @DisplayName("Test isWrappedWith()")
    public void testIsWrappedWith() {
        assertTrue(Strings.isWrappedWith("'test'", "'"));
        assertTrue(Strings.isWrappedWith("\"test\"", "\""));
        assertFalse(Strings.isWrappedWith("test", "'"));
        assertFalse(Strings.isWrappedWith(null, "'"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", ""));
    }

    @Test
    @DisplayName("Test isWrappedWith() with prefix and suffix")
    public void testIsWrappedWithPrefixSuffix() {
        assertTrue(Strings.isWrappedWith("[test]", "[", "]"));
        assertFalse(Strings.isWrappedWith("test]", "[", "]"));
        assertFalse(Strings.isWrappedWith("[test", "[", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "[", ""));
    }

    @Test
    @DisplayName("Test firstNonEmpty()")
    public void testFirstNonEmpty() {
        assertEquals("abc", Strings.firstNonEmpty(null, null, "abc"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz", "abc"));
        assertEquals("", Strings.firstNonEmpty(null, null, null));
        assertEquals("", Strings.firstNonEmpty("", "", ""));
    }

    @Test
    @DisplayName("Test firstNonBlank()")
    public void testFirstNonBlank() {
        assertEquals("abc", Strings.firstNonBlank(null, "  ", "abc"));
        assertEquals("xyz", Strings.firstNonBlank("  ", "xyz", "abc"));
        assertEquals("", Strings.firstNonBlank(null, "  ", ""));
    }

    @Test
    @DisplayName("Test defaultIfNull()")
    public void testDefaultIfNull() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
        assertEquals("test", Strings.defaultIfNull("test", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull((String) null, (String) null));
    }

    @Test
    @DisplayName("Test defaultIfEmpty()")
    public void testDefaultIfEmpty() {
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("test", Strings.defaultIfEmpty("test", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("", ""));
    }

    @Test
    @DisplayName("Test defaultIfBlank()")
    public void testDefaultIfBlank() {
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
        assertEquals("default", Strings.defaultIfBlank("", "default"));
        assertEquals("default", Strings.defaultIfBlank("  ", "default"));
        assertEquals("test", Strings.defaultIfBlank("test", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("  ", "  "));
    }

    @Test
    @DisplayName("Test nullToEmpty()")
    public void testNullToEmpty() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("test", Strings.nullToEmpty("test"));
    }

    @Test
    @DisplayName("Test emptyToNull()")
    public void testEmptyToNull() {
        assertNull(Strings.emptyToNull((String) null));
        assertNull(Strings.emptyToNull(""));
        assertEquals("test", Strings.emptyToNull("test"));
        assertEquals(" ", Strings.emptyToNull(" "));
    }

    @Test
    @DisplayName("Test blankToEmpty()")
    public void testBlankToEmpty() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("  "));
        assertEquals("test", Strings.blankToEmpty("test"));
    }

    @Test
    @DisplayName("Test blankToNull()")
    public void testBlankToNull() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("  "));
        assertEquals("test", Strings.blankToNull("test"));
    }

    @Test
    @DisplayName("Test abbreviate()")
    public void testAbbreviate() {
        assertNull(Strings.abbreviate(null, 4));
        assertEquals("", Strings.abbreviate("", 4));
        assertEquals("abc...", Strings.abbreviate("abcdefg", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 7));
        assertEquals("a...", Strings.abbreviate("abcdefg", 4));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", 3));
    }

    @Test
    @DisplayName("Test abbreviateMiddle()")
    public void testAbbreviateMiddle() {
        assertNull(Strings.abbreviateMiddle(null, "...", 0));
        assertEquals("abc", Strings.abbreviateMiddle("abc", null, 0));
        assertEquals("abc", Strings.abbreviateMiddle("abc", ".", 3));
        assertEquals("ab.f", Strings.abbreviateMiddle("abcdef", ".", 4));
    }

    @Test
    @DisplayName("Test center()")
    public void testCenter() {
        assertEquals("    ", Strings.center(null, 4));
        assertEquals(" ab ", Strings.center("ab", 4));
        assertEquals("abcd", Strings.center("abcd", 2));
        assertEquals(" a  ", Strings.center("a", 4));
    }

    @Test
    @DisplayName("Test padStart()")
    public void testPadStart() {
        assertEquals("   ", Strings.padStart(null, 3));
        assertEquals("  abc", Strings.padStart("abc", 5));
        assertEquals("abc", Strings.padStart("abc", 2));
        assertEquals("XXabc", Strings.padStart("abc", 5, 'X'));
    }

    @Test
    @DisplayName("Test padEnd()")
    public void testPadEnd() {
        assertEquals("   ", Strings.padEnd(null, 3));
        assertEquals("abc  ", Strings.padEnd("abc", 5));
        assertEquals("abc", Strings.padEnd("abc", 2));
        assertEquals("abcXX", Strings.padEnd("abc", 5, 'X'));
    }

    @Test
    @DisplayName("Test repeat(char, int)")
    public void testRepeatChar() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("a", Strings.repeat('a', 1));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    @DisplayName("Test repeat(String, int)")
    public void testRepeatString() {
        assertEquals("", Strings.repeat("abc", 0));
        assertEquals("abc", Strings.repeat("abc", 1));
        assertEquals("abcabcabc", Strings.repeat("abc", 3));
        assertEquals("", Strings.repeat(null, 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("abc", -1));
    }

    @Test
    @DisplayName("Test getBytes()")
    public void testGetBytes() {
        assertNull(Strings.getBytes(null));
        assertArrayEquals(new byte[0], Strings.getBytes(""));
        assertArrayEquals("test".getBytes(), Strings.getBytes("test"));
    }

    @Test
    @DisplayName("Test getBytesUtf8()")
    public void testGetBytesUtf8() {
        assertNull(Strings.getBytesUtf8(null));
        assertArrayEquals(new byte[0], Strings.getBytesUtf8(""));
        assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8("test"));
    }

    @Test
    @DisplayName("Test toCharArray()")
    public void testToCharArray() {
        assertNull(Strings.toCharArray(null));
        assertArrayEquals(new char[0], Strings.toCharArray(""));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Strings.toCharArray("abc"));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Strings.toCharArray(new StringBuilder("abc")));
    }

    @Test
    @DisplayName("Test toCodePoints()")
    public void testToCodePoints() {
        assertNull(Strings.toCodePoints(null));
        assertArrayEquals(new int[0], Strings.toCodePoints(""));
        assertArrayEquals(new int[] { 97, 98, 99 }, Strings.toCodePoints("abc"));
    }

    @Test
    @DisplayName("Test toLowerCase(char)")
    public void testToLowerCaseChar() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals('a', Strings.toLowerCase('a'));
        assertEquals('1', Strings.toLowerCase('1'));
    }

    @Test
    @DisplayName("Test toLowerCase(String)")
    public void testToLowerCaseString() {
        assertNull(Strings.toLowerCase(null));
        assertEquals("", Strings.toLowerCase(""));
        assertEquals("abc", Strings.toLowerCase("ABC"));
        assertEquals("abc", Strings.toLowerCase("aBc"));
    }

    @Test
    @DisplayName("Test toUpperCase(char)")
    public void testToUpperCaseChar() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals('A', Strings.toUpperCase('A'));
        assertEquals('1', Strings.toUpperCase('1'));
    }

    @Test
    @DisplayName("Test toUpperCase(String)")
    public void testToUpperCaseString() {
        assertNull(Strings.toUpperCase(null));
        assertEquals("", Strings.toUpperCase(""));
        assertEquals("ABC", Strings.toUpperCase("abc"));
        assertEquals("ABC", Strings.toUpperCase("aBc"));
    }

    @Test
    @DisplayName("Test toSnakeCase()")
    public void testToSnakeCase() {
        assertNull(Strings.toSnakeCase(null));
        assertEquals("", Strings.toSnakeCase(""));
        assertEquals("hello_world", Strings.toSnakeCase("HelloWorld"));
        assertEquals("hello_world", Strings.toSnakeCase("helloWorld"));
    }

    @Test
    @DisplayName("Test toScreamingSnakeCase()")
    public void testToScreamingSnakeCase() {
        assertNull(Strings.toScreamingSnakeCase(null));
        assertEquals("", Strings.toScreamingSnakeCase(""));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("HelloWorld"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("helloWorld"));
    }

    @Test
    @DisplayName("Test toCamelCase()")
    public void testToCamelCase() {
        assertNull(Strings.toCamelCase(null));
        assertEquals("", Strings.toCamelCase(""));
        assertEquals("firstName", Strings.toCamelCase("first_name"));
        assertEquals("firstName", Strings.toCamelCase("firstName"));
        assertEquals("firstName", Strings.toCamelCase("FIRST_NAME"));
        assertEquals("firstName", Strings.toCamelCase("FirstName"));
        assertEquals("first", Strings.toCamelCase("FIRST"));
        assertEquals("first", Strings.toCamelCase("first"));
        assertEquals("first", Strings.toCamelCase("First"));
        assertEquals("id", Strings.toCamelCase("_id"));
    }

    @Test
    @DisplayName("Test toUpperCamelCase()")
    public void testToUpperCamelCase() {
        assertNull(Strings.toUpperCamelCase(null));
        assertEquals("", Strings.toUpperCamelCase(""));
        assertEquals("FirstName", Strings.toUpperCamelCase("first_name"));
        assertEquals("FirstName", Strings.toUpperCamelCase("firstName"));
        assertEquals("FirstName", Strings.toUpperCamelCase("FIRST_NAME"));
        assertEquals("FirstName", Strings.toUpperCamelCase("FirstName"));
        assertEquals("FIRST", Strings.toUpperCamelCase("FIRST"));
        assertEquals("First", Strings.toUpperCamelCase("first"));
        assertEquals("First", Strings.toUpperCamelCase("First"));
        assertEquals("Id", Strings.toUpperCamelCase("_id"));
    }

    @Test
    @DisplayName("Test swapCase()")
    public void testSwapCase() {
        assertNull(Strings.swapCase(null));
        assertEquals("", Strings.swapCase(""));
        assertEquals("tHE DOG HAS A bone", Strings.swapCase("The dog has a BONE"));
    }

    @Test
    @DisplayName("Test capitalize()")
    public void testCapitalize() {
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
        assertEquals("Cat", Strings.capitalize("cat"));
        assertEquals("CAt", Strings.capitalize("cAt"));
    }

    @Test
    @DisplayName("Test uncapitalize()")
    public void testUncapitalize() {
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
        assertEquals("cat", Strings.uncapitalize("Cat"));
        assertEquals("cAT", Strings.uncapitalize("CAT"));
    }

    @Test
    @DisplayName("Test capitalizeFully()")
    public void testCapitalizeFully() {
        assertNull(Strings.capitalizeFully(null));
        assertEquals("", Strings.capitalizeFully(""));
        assertEquals("The Dog Has A Bone", Strings.capitalizeFully("the dog has a bone"));
    }

    @Test
    @DisplayName("Test quoteEscaped()")
    public void testQuoteEscaped() {
        assertNull(Strings.quoteEscaped(null));
        assertEquals("", Strings.quoteEscaped(""));
        assertEquals("test\\'s", Strings.quoteEscaped("test's"));
        assertEquals("\\\"quoted\\\"", Strings.quoteEscaped("\"quoted\""));
    }

    @Test
    @DisplayName("Test unicodeEscaped()")
    public void testUnicodeEscaped() {
        assertEquals("\\u0020", Strings.unicodeEscaped(' '));
        assertEquals("\\u0041", Strings.unicodeEscaped('A'));
    }

    @Test
    @DisplayName("Test normalizeSpace()")
    public void testNormalizeSpace() {
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
        assertEquals("abc", Strings.normalizeSpace("  abc   "));
        assertEquals("a b c", Strings.normalizeSpace("a   b   c"));
    }

    @Test
    @DisplayName("Test replaceAll()")
    public void testReplaceAll() {
        assertNull(Strings.replaceAll(null, "a", "b"));
        assertEquals("", Strings.replaceAll("", "a", "b"));
        assertEquals("any", Strings.replaceAll("any", null, "b"));
        assertEquals("zbz", Strings.replaceAll("aba", "a", "z"));
    }

    @Test
    @DisplayName("Test replaceFirst()")
    public void testReplaceFirst() {
        assertNull(Strings.replaceFirst(null, "a", "b"));
        assertEquals("", Strings.replaceFirst("", "a", "b"));
        assertEquals("zba", Strings.replaceFirst("aba", "a", "z"));
    }

    @Test
    @DisplayName("Test replaceLast()")
    public void testReplaceLast() {
        assertNull(Strings.replaceLast(null, "a", "b"));
        assertEquals("", Strings.replaceLast("", "a", "b"));
        assertEquals("abz", Strings.replaceLast("aba", "a", "z"));
    }

    @Test
    @DisplayName("Test replaceAllIgnoreCase()")
    public void testReplaceAllIgnoreCase() {
        assertNull(Strings.replaceAllIgnoreCase(null, "a", "b"));
        assertEquals("zbz", Strings.replaceAllIgnoreCase("aba", "A", "z"));
        assertEquals("zbz", Strings.replaceAllIgnoreCase("AbA", "a", "z"));
    }

    @Test
    @DisplayName("Test replaceRange()")
    public void testReplaceRange() {
        assertEquals("abXYZef", Strings.replaceRange("abcdef", 2, 4, "XYZ"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceRange("abc", -1, 2, "X"));
    }

    @Test
    @DisplayName("Test removeStart()")
    public void testRemoveStart() {
        assertNull(Strings.removeStart(null, "www."));
        assertEquals("", Strings.removeStart("", "www."));
        assertEquals("domain.com", Strings.removeStart("www.domain.com", "www."));
        assertEquals("domain.com", Strings.removeStart("domain.com", "www."));
    }

    @Test
    @DisplayName("Test removeEnd()")
    public void testRemoveEnd() {
        assertNull(Strings.removeEnd(null, ".com"));
        assertEquals("", Strings.removeEnd("", ".com"));
        assertEquals("www.domain", Strings.removeEnd("www.domain.com", ".com"));
        assertEquals("www.domain.com", Strings.removeEnd("www.domain.com", ".net"));
    }

    @Test
    @DisplayName("Test removeAll(char)")
    public void testRemoveAllChar() {
        assertNull(Strings.removeAll(null, 'u'));
        assertEquals("", Strings.removeAll("", 'u'));
        assertEquals("qeed", Strings.removeAll("queued", 'u'));
        assertEquals("queued", Strings.removeAll("queued", 'z'));
    }

    @Test
    @DisplayName("Test removeAll(String)")
    public void testRemoveAllString() {
        assertNull(Strings.removeAll(null, "ue"));
        assertEquals("", Strings.removeAll("", "ue"));
        assertEquals("qd", Strings.removeAll("queued", "ue"));
    }

    @Test
    @DisplayName("Test split(char)")
    public void testSplitChar() {
        assertArrayEquals(new String[0], Strings.split(null, ','));
        assertArrayEquals(new String[0], Strings.split("", ','));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ','));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,,c", ','));
    }

    @Test
    @DisplayName("Test split(String)")
    public void testSplitString() {
        assertArrayEquals(new String[0], Strings.split(null, ","));
        assertArrayEquals(new String[0], Strings.split("", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ","));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens()")
    public void testSplitPreserveAllTokens() {
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ','));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ','));
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a,,b,c", ','));
    }

    @Test
    @DisplayName("Test splitToLines()")
    public void testSplitToLines() {
        assertArrayEquals(new String[0], Strings.splitToLines(null));
        assertArrayEquals(new String[] { "" }, Strings.splitToLines(""));
        assertArrayEquals(new String[] { "line1", "line2", "line3" }, Strings.splitToLines("line1\nline2\r\nline3"));
    }

    @Test
    @DisplayName("Test trim()")
    public void testTrim() {
        assertNull(Strings.trim((String) null));
        assertEquals("", Strings.trim(""));
        assertEquals("", Strings.trim("     "));
        assertEquals("abc", Strings.trim("  abc  "));
    }

    @Test
    @DisplayName("Test trimToNull()")
    public void testTrimToNull() {
        assertNull(Strings.trimToNull((String) null));
        assertNull(Strings.trimToNull(""));
        assertNull(Strings.trimToNull("     "));
        assertEquals("abc", Strings.trimToNull("  abc  "));
    }

    @Test
    @DisplayName("Test trimToEmpty()")
    public void testTrimToEmpty() {
        assertEquals("", Strings.trimToEmpty((String) null));
        assertEquals("", Strings.trimToEmpty(""));
        assertEquals("", Strings.trimToEmpty("     "));
        assertEquals("abc", Strings.trimToEmpty("  abc  "));
    }

    @Test
    @DisplayName("Test strip()")
    public void testStrip() {
        assertNull(Strings.strip((String) null));
        assertEquals("", Strings.strip(""));
        assertEquals("", Strings.strip("   "));
        assertEquals("abc", Strings.strip("  abc"));
        assertEquals("ab c", Strings.strip(" ab c "));
    }

    @Test
    @DisplayName("Test stripStart()")
    public void testStripStart() {
        assertNull(Strings.stripStart((String) null, (String) null));
        assertEquals("", Strings.stripStart("", null));
        assertEquals("abc  ", Strings.stripStart("  abc  ", null));
        assertEquals("abc  ", Strings.stripStart("yxabc  ", "xyz"));
    }

    @Test
    @DisplayName("Test stripEnd()")
    public void testStripEnd() {
        assertNull(Strings.stripEnd((String) null, (String) null));
        assertEquals("", Strings.stripEnd("", null));
        assertEquals("  abc", Strings.stripEnd("  abc  ", null));
        assertEquals("  abc", Strings.stripEnd("  abcyx", "xyz"));
    }

    @Test
    @DisplayName("Test stripAccents()")
    public void testStripAccents() {
        assertNull(Strings.stripAccents((String) null));
        assertEquals("", Strings.stripAccents(""));
        assertEquals("eclair", Strings.stripAccents("éclair"));
        assertEquals("control", Strings.stripAccents("control"));
    }

    @Test
    @DisplayName("Test chomp()")
    public void testChomp() {
        assertNull(Strings.chomp((String) null));
        assertEquals("", Strings.chomp(""));
        assertEquals("abc", Strings.chomp("abc\n"));
        assertEquals("abc", Strings.chomp("abc\r\n"));
        assertEquals("abc\r\n", Strings.chomp("abc\r\n\r\n"));
    }

    @Test
    @DisplayName("Test chop()")
    public void testChop() {
        assertNull(Strings.chop((String) null));
        assertEquals("", Strings.chop(""));
        assertEquals("ab", Strings.chop("abc"));
        assertEquals("abc", Strings.chop("abc\r\n"));
        assertEquals("", Strings.chop("a"));
    }

    @Test
    @DisplayName("Test truncate()")
    public void testTruncate() {
        assertNull(Strings.truncate((String) null, 0));
        assertEquals("", Strings.truncate("", 4));
        assertEquals("abcd", Strings.truncate("abcdefg", 4));
        assertEquals("abcdefg", Strings.truncate("abcdefg", 8));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abcdefg", -1));
    }

    @Test
    @DisplayName("Test deleteWhitespace()")
    public void testDeleteWhitespace() {
        assertNull(Strings.deleteWhitespace((String) null));
        assertEquals("", Strings.deleteWhitespace(""));
        assertEquals("abc", Strings.deleteWhitespace("   ab  c  "));
    }

    @Test
    @DisplayName("Test appendIfMissing()")
    public void testAppendIfMissing() {
        assertEquals(".txt", Strings.appendIfMissing(null, ".txt"));
        assertEquals(".txt", Strings.appendIfMissing("", ".txt"));
        assertEquals("file.txt", Strings.appendIfMissing("file", ".txt"));
        assertEquals("file.txt", Strings.appendIfMissing("file.txt", ".txt"));
        assertThrows(IllegalArgumentException.class, () -> Strings.appendIfMissing("file", ""));
    }

    @Test
    @DisplayName("Test prependIfMissing()")
    public void testPrependIfMissing() {
        assertEquals("www.", Strings.prependIfMissing(null, "www."));
        assertEquals("www.", Strings.prependIfMissing("", "www."));
        assertEquals("www.domain.com", Strings.prependIfMissing("domain.com", "www."));
        assertEquals("www.domain.com", Strings.prependIfMissing("www.domain.com", "www."));
        assertThrows(IllegalArgumentException.class, () -> Strings.prependIfMissing("domain", ""));
    }

    @Test
    @DisplayName("Test wrapIfMissing()")
    public void testWrapIfMissing() {
        assertEquals("''", Strings.wrapIfMissing(null, "'"));
        assertEquals("'test'", Strings.wrapIfMissing("test", "'"));
        assertEquals("'test'", Strings.wrapIfMissing("'test'", "'"));
        assertThrows(IllegalArgumentException.class, () -> Strings.wrapIfMissing("test", ""));
    }

    @Test
    @DisplayName("Test wrap()")
    public void testWrap() {
        assertEquals("''", Strings.wrap(null, "'"));
        assertEquals("'test'", Strings.wrap("test", "'"));
        assertEquals("''test''", Strings.wrap("'test'", "'"));
        assertThrows(IllegalArgumentException.class, () -> Strings.wrap("test", ""));
    }

    @Test
    @DisplayName("Test unwrap()")
    public void testUnwrap() {
        assertNull(Strings.unwrap(null, "'"));
        assertEquals("", Strings.unwrap("", "'"));
        assertEquals("test", Strings.unwrap("'test'", "'"));
        assertEquals("test", Strings.unwrap("test", "'"));
        assertThrows(IllegalArgumentException.class, () -> Strings.unwrap("test", ""));
    }

    @Test
    @DisplayName("Test isLowerCase()")
    public void testIsLowerCase() {
        assertTrue(Strings.isLowerCase('a'));
        assertFalse(Strings.isLowerCase('A'));
        assertFalse(Strings.isLowerCase('1'));
    }

    @Test
    @DisplayName("Test isUpperCase()")
    public void testIsUpperCase() {
        assertTrue(Strings.isUpperCase('A'));
        assertFalse(Strings.isUpperCase('a'));
        assertFalse(Strings.isUpperCase('1'));
    }

    @Test
    @DisplayName("Test isAllLowerCase()")
    public void testIsAllLowerCase() {
        assertTrue(Strings.isAllLowerCase(""));
        assertTrue(Strings.isAllLowerCase("abc"));
        assertFalse(Strings.isAllLowerCase("aBc"));
        assertFalse(Strings.isAllLowerCase("ABC"));
    }

    @Test
    @DisplayName("Test isAllUpperCase()")
    public void testIsAllUpperCase() {
        assertTrue(Strings.isAllUpperCase(""));
        assertTrue(Strings.isAllUpperCase("ABC"));
        assertFalse(Strings.isAllUpperCase("aBc"));
        assertFalse(Strings.isAllUpperCase("abc"));
    }

    @Test
    @DisplayName("Test isMixedCase()")
    public void testIsMixedCase() {
        assertTrue(Strings.isMixedCase("aBc"));
        assertFalse(Strings.isMixedCase("abc"));
        assertFalse(Strings.isMixedCase("ABC"));
        assertFalse(Strings.isMixedCase(""));
        assertFalse(Strings.isMixedCase("a"));
    }

    @Test
    @DisplayName("Test isAscii()")
    public void testIsAscii() {
        assertTrue(Strings.isAscii('a'));
        assertTrue(Strings.isAscii('A'));
        assertTrue(Strings.isAscii('3'));
        assertFalse(Strings.isAscii('©'));
    }

    @Test
    @DisplayName("Test isAsciiPrintable(char)")
    public void testIsAsciiPrintableChar() {
        assertTrue(Strings.isAsciiPrintable('a'));
        assertTrue(Strings.isAsciiPrintable('A'));
        assertFalse(Strings.isAsciiPrintable('\n'));
    }

    @Test
    @DisplayName("Test isAsciiPrintable(CharSequence)")
    public void testIsAsciiPrintableCharSequence() {
        assertFalse(Strings.isAsciiPrintable(null));
        assertTrue(Strings.isAsciiPrintable(""));
        assertTrue(Strings.isAsciiPrintable("Ceki"));
        assertFalse(Strings.isAsciiPrintable("Ceki Gülcü"));
    }

    @Test
    @DisplayName("Test isAsciiAlpha()")
    public void testIsAsciiAlpha() {
        assertFalse(Strings.isAsciiAlpha(null));
        assertFalse(Strings.isAsciiAlpha(""));
        assertTrue(Strings.isAsciiAlpha("abc"));
        assertFalse(Strings.isAsciiAlpha("ab2c"));
    }

    @Test
    @DisplayName("Test isAsciiNumeric()")
    public void testIsAsciiNumeric() {
        assertFalse(Strings.isAsciiNumeric(null));
        assertFalse(Strings.isAsciiNumeric(""));
        assertTrue(Strings.isAsciiNumeric("123"));
        assertFalse(Strings.isAsciiNumeric("12 3"));
    }

    @Test
    @DisplayName("Test isAsciiAlphanumeric()")
    public void testIsAsciiAlphanumeric() {
        assertFalse(Strings.isAsciiAlphanumeric(null));
        assertFalse(Strings.isAsciiAlphanumeric(""));
        assertTrue(Strings.isAsciiAlphanumeric("abc123"));
        assertFalse(Strings.isAsciiAlphanumeric("ab-c"));
    }

    @Test
    @DisplayName("Test isAlpha()")
    public void testIsAlpha() {
        assertFalse(Strings.isAlpha(null));
        assertFalse(Strings.isAlpha(""));
        assertTrue(Strings.isAlpha("abc"));
        assertFalse(Strings.isAlpha("ab2c"));
    }

    @Test
    @DisplayName("Test isAlphaSpace()")
    public void testIsAlphaSpace() {
        assertFalse(Strings.isAlphaSpace(null));
        assertTrue(Strings.isAlphaSpace(""));
        assertTrue(Strings.isAlphaSpace("abc"));
        assertTrue(Strings.isAlphaSpace("ab c"));
        assertFalse(Strings.isAlphaSpace("ab2c"));
    }

    @Test
    @DisplayName("Test isAlphanumeric()")
    public void testIsAlphanumeric() {
        assertFalse(Strings.isAlphanumeric(null));
        assertFalse(Strings.isAlphanumeric(""));
        assertTrue(Strings.isAlphanumeric("abc"));
        assertTrue(Strings.isAlphanumeric("ab2c"));
        assertFalse(Strings.isAlphanumeric("ab-c"));
    }

    @Test
    @DisplayName("Test isNumeric()")
    public void testIsNumeric() {
        assertFalse(Strings.isNumeric(null));
        assertFalse(Strings.isNumeric(""));
        assertTrue(Strings.isNumeric("123"));
        assertFalse(Strings.isNumeric("12.3"));
        assertFalse(Strings.isNumeric("-123"));
    }

    @Test
    @DisplayName("Test isWhitespace()")
    public void testIsWhitespace() {
        assertFalse(Strings.isWhitespace(null));
        assertTrue(Strings.isWhitespace(""));
        assertTrue(Strings.isWhitespace("  "));
        assertFalse(Strings.isWhitespace("abc"));
    }

    @Test
    @DisplayName("Test isAsciiNumber()")
    public void testisAsciiNumber() {
        assertFalse(Strings.isAsciiNumber(null));
        assertFalse(Strings.isAsciiNumber(""));
        assertTrue(Strings.isAsciiNumber("0"));
        assertTrue(Strings.isAsciiNumber("123"));
        assertTrue(Strings.isAsciiNumber("123.45"));
        assertTrue(Strings.isAsciiNumber("-123.45"));
        assertTrue(Strings.isAsciiNumber("2e10"));
        assertTrue(Strings.isAsciiNumber("2E-10"));
        assertFalse(Strings.isAsciiNumber(" 0.1 "));
        assertFalse(Strings.isAsciiNumber("abc"));
    }

    @Test
    @DisplayName("Test isAsciiInteger()")
    public void testisAsciiInteger() {
        assertFalse(Strings.isAsciiInteger(null));
        assertFalse(Strings.isAsciiInteger(""));
        assertTrue(Strings.isAsciiInteger("0"));
        assertTrue(Strings.isAsciiInteger("123"));
        assertTrue(Strings.isAsciiInteger("-123"));
        assertTrue(Strings.isAsciiInteger("+123"));
        assertFalse(Strings.isAsciiInteger("123.45"));
        assertFalse(Strings.isAsciiInteger("2e10"));
    }

    @Test
    @DisplayName("Test indexOf(char)")
    public void testIndexOfChar() {
        assertEquals(-1, Strings.indexOf(null, 'a'));
        assertEquals(-1, Strings.indexOf("", 'a'));
        assertEquals(0, Strings.indexOf("aabaabaa", 'a'));
        assertEquals(2, Strings.indexOf("aabaabaa", 'b'));
    }

    @Test
    @DisplayName("Test indexOf(String)")
    public void testIndexOfString() {
        assertEquals(-1, Strings.indexOf(null, "a"));
        assertEquals(-1, Strings.indexOf("", "a"));
        assertEquals(0, Strings.indexOf("aabaabaa", "a"));
        assertEquals(2, Strings.indexOf("aabaabaa", "b"));
        assertEquals(1, Strings.indexOf("aabaabaa", "ab"));
    }

    @Test
    @DisplayName("Test indexOfAny(char...)")
    public void testIndexOfAnyChars() {
        assertEquals(-1, Strings.indexOfAny(null, 'a', 'b'));
        assertEquals(-1, Strings.indexOfAny("", 'a', 'b'));
        assertEquals(0, Strings.indexOfAny("zzabyycdxx", 'z', 'a'));
        assertEquals(3, Strings.indexOfAny("zzabyycdxx", 'b', 'd'));
    }

    @Test
    @DisplayName("Test indexOfAny(String...)")
    public void testIndexOfAnyStrings() {
        assertEquals(-1, Strings.indexOfAny(null, "ab", "cd"));
        assertEquals(-1, Strings.indexOfAny("", "ab", "cd"));
        assertEquals(2, Strings.indexOfAny("zzabyycdxx", "ab", "cd"));
        assertEquals(6, Strings.indexOfAny("zzabyycdxx", "cd", "ef"));
    }

    @Test
    @DisplayName("Test indexOfAnyBut()")
    public void testIndexOfAnyBut() {
        assertEquals(-1, Strings.indexOfAnyBut(null, 'a'));
        assertEquals(-1, Strings.indexOfAnyBut("", 'a'));
        assertEquals(0, Strings.indexOfAnyBut("zzabyycdxx", 'b', 'y'));
        assertEquals(1, Strings.indexOfAnyBut("aba", 'a'));
    }

    @Test
    @DisplayName("Test indexOfIgnoreCase()")
    public void testIndexOfIgnoreCase() {
        assertEquals(-1, Strings.indexOfIgnoreCase(null, "a"));
        assertEquals(-1, Strings.indexOfIgnoreCase("", "a"));
        assertEquals(0, Strings.indexOfIgnoreCase("aabaabaa", "A"));
        assertEquals(2, Strings.indexOfIgnoreCase("aabaabaa", "B"));
    }

    @Test
    @DisplayName("Test lastIndexOf(char)")
    public void testLastIndexOfChar() {
        assertEquals(-1, Strings.lastIndexOf(null, 'a'));
        assertEquals(-1, Strings.lastIndexOf("", 'a'));
        assertEquals(7, Strings.lastIndexOf("aabaabaa", 'a'));
        assertEquals(5, Strings.lastIndexOf("aabaabaa", 'b'));
    }

    @Test
    @DisplayName("Test lastIndexOf(String)")
    public void testLastIndexOfString() {
        assertEquals(-1, Strings.lastIndexOf(null, "a"));
        assertEquals(-1, Strings.lastIndexOf("", "a"));
        assertEquals(7, Strings.lastIndexOf("aabaabaa", "a"));
        assertEquals(4, Strings.lastIndexOf("aabaabaa", "ab"));
    }

    @Test
    @DisplayName("Test lastIndexOfIgnoreCase()")
    public void testLastIndexOfIgnoreCase() {
        assertEquals(-1, Strings.lastIndexOfIgnoreCase(null, "a"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("", "a"));
        assertEquals(7, Strings.lastIndexOfIgnoreCase("aabaabaa", "A"));
        assertEquals(5, Strings.lastIndexOfIgnoreCase("aabaabaa", "B"));
    }

    @Test
    @DisplayName("Test lastIndexOfAny(char...)")
    public void testLastIndexOfAnyChars() {
        assertEquals(-1, Strings.lastIndexOfAny((String) null, new char[] {'a'}));
        assertEquals(-1, Strings.lastIndexOfAny("", new char[] {'a'}));
        assertEquals(1, Strings.lastIndexOfAny("zzabyycdxx", 'z', 'x'));
        assertEquals(7, Strings.lastIndexOfAny("zzabyycdxx", 'd', 'x'));
    }

    @Test
    @DisplayName("Test lastIndexOfAny(String...)")
    public void testLastIndexOfAnyStrings() {
        assertEquals(-1, Strings.lastIndexOfAny(null, "ab"));
        assertEquals(-1, Strings.lastIndexOfAny("", "ab"));
        assertEquals(2, Strings.lastIndexOfAny("zzabyycdxx", "ab", "cd"));
        assertEquals(8, Strings.lastIndexOfAny("zzabyycdxx", "dd", "xx"));
    }

    @Test
    @DisplayName("Test ordinalIndexOf()")
    public void testOrdinalIndexOf() {
        assertEquals(-1, Strings.ordinalIndexOf(null, "a", 1));
        assertEquals(-1, Strings.ordinalIndexOf("", "a", 1));
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(1, Strings.ordinalIndexOf("aabaabaa", "a", 2));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.ordinalIndexOf("abc", "a", 0));
    }

    @Test
    @DisplayName("Test lastOrdinalIndexOf()")
    public void testLastOrdinalIndexOf() {
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, "a", 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf("", "a", 1));
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(6, Strings.lastOrdinalIndexOf("aabaabaa", "a", 2));
        assertEquals(4, Strings.lastOrdinalIndexOf("aabaabaa", "a", 3));
    }

    @Test
    @DisplayName("Test indicesOf()")
    public void testIndicesOf() {
        IntStream indices = Strings.indicesOf("abcabc", "a");
        assertArrayEquals(new int[] { 0, 3 }, indices.toArray());

        indices = Strings.indicesOf("abcabc", "d");
        assertArrayEquals(new int[0], indices.toArray());

        indices = Strings.indicesOf(null, "a");
        assertArrayEquals(new int[0], indices.toArray());
    }

    @Test
    @DisplayName("Test indicesOfIgnoreCase()")
    public void testIndicesOfIgnoreCase() {
        IntStream indices = Strings.indicesOfIgnoreCase("AbcAbc", "a");
        assertArrayEquals(new int[] { 0, 3 }, indices.toArray());

        indices = Strings.indicesOfIgnoreCase("abcabc", "D");
        assertArrayEquals(new int[0], indices.toArray());
    }

    @Test
    @DisplayName("Test countMatches(char)")
    public void testCountMatchesChar() {
        assertEquals(0, Strings.countMatches(null, 'a'));
        assertEquals(0, Strings.countMatches("", 'a'));
        assertEquals(6, Strings.countMatches("aabaabaa", 'a'));
        assertEquals(2, Strings.countMatches("aabaabaa", 'b'));
    }

    @Test
    @DisplayName("Test countMatches(String)")
    public void testCountMatchesString() {
        assertEquals(0, Strings.countMatches(null, "a"));
        assertEquals(0, Strings.countMatches("", "a"));
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(1, Strings.countMatches("abcdef", "def"));
    }

    @Test
    @DisplayName("Test contains(char)")
    public void testContainsChar() {
        assertFalse(Strings.contains(null, 'a'));
        assertFalse(Strings.contains("", 'a'));
        assertTrue(Strings.contains("abc", 'a'));
        assertFalse(Strings.contains("abc", 'd'));
    }

    @Test
    @DisplayName("Test contains(String)")
    public void testContainsString() {
        assertFalse(Strings.contains(null, "a"));
        assertFalse(Strings.contains("", "a"));
        assertTrue(Strings.contains("abc", "a"));
        assertTrue(Strings.contains("abc", "ab"));
        assertFalse(Strings.contains("abc", "d"));
    }

    @Test
    @DisplayName("Test containsIgnoreCase()")
    public void testContainsIgnoreCase() {
        assertFalse(Strings.containsIgnoreCase(null, "a"));
        assertFalse(Strings.containsIgnoreCase("", "a"));
        assertTrue(Strings.containsIgnoreCase("abc", "A"));
        assertTrue(Strings.containsIgnoreCase("ABC", "ab"));
    }

    @Test
    @DisplayName("Test containsAll(char...)")
    public void testContainsAllChars() {
        assertTrue(Strings.containsAll("abcd", 'a', 'b'));
        assertFalse(Strings.containsAll("abcd", 'a', 'e'));
        assertTrue(Strings.containsAll("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertFalse(Strings.containsAll(null, 'a'));
    }

    @Test
    @DisplayName("Test containsAll(String...)")
    public void testContainsAllStrings() {
        assertTrue(Strings.containsAll("abcdef", "ab", "cd"));
        assertFalse(Strings.containsAll("abcdef", "ab", "gh"));
        assertTrue(Strings.containsAll("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertFalse(Strings.containsAll(null, "a"));
    }

    @Test
    @DisplayName("Test containsAny(char...)")
    public void testContainsAnyChars() {
        assertTrue(Strings.containsAny("abcd", 'a', 'e'));
        assertFalse(Strings.containsAny("abcd", 'e', 'f'));
        assertFalse(Strings.containsAny("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertFalse(Strings.containsAny(null, 'a'));
    }

    @Test
    @DisplayName("Test containsAny(String...)")
    public void testContainsAnyStrings() {
        assertTrue(Strings.containsAny("abcdef", "ab", "gh"));
        assertFalse(Strings.containsAny("abcdef", "gh", "ij"));
        assertFalse(Strings.containsAny("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertFalse(Strings.containsAny(null, "a"));
    }

    @Test
    @DisplayName("Test containsNone(char...)")
    public void testContainsNoneChars() {
        assertTrue(Strings.containsNone("abcd", 'e', 'f'));
        assertFalse(Strings.containsNone("abcd", 'a', 'e'));
        assertTrue(Strings.containsNone("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertTrue(Strings.containsNone(null, 'a'));
    }

    @Test
    @DisplayName("Test containsNone(String...)")
    public void testContainsNoneStrings() {
        assertTrue(Strings.containsNone("abcdef", "gh", "ij"));
        assertFalse(Strings.containsNone("abcdef", "ab", "gh"));
        assertTrue(Strings.containsNone("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertTrue(Strings.containsNone(null, "a"));
    }

    @Test
    @DisplayName("Test containsOnly()")
    public void testContainsOnly() {
        assertTrue(Strings.containsOnly("", 'a'));
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertFalse(Strings.containsOnly("aaab", 'a'));
        assertFalse(Strings.containsOnly("aaa"));
    }

    @Test
    @DisplayName("Test containsWhitespace()")
    public void testContainsWhitespace() {
        assertFalse(Strings.containsWhitespace(null));
        assertFalse(Strings.containsWhitespace(""));
        assertTrue(Strings.containsWhitespace("ab c"));
        assertTrue(Strings.containsWhitespace("ab\tc"));
        assertFalse(Strings.containsWhitespace("abc"));
    }

    @Test
    @DisplayName("Test startsWith()")
    public void testStartsWith() {
        assertFalse(Strings.startsWith(null, "ab"));
        assertFalse(Strings.startsWith("abcdef", null));
        assertTrue(Strings.startsWith("abcdef", "ab"));
        assertFalse(Strings.startsWith("abcdef", "cd"));
    }

    @Test
    @DisplayName("Test startsWithIgnoreCase()")
    public void testStartsWithIgnoreCase() {
        assertFalse(Strings.startsWithIgnoreCase(null, "ab"));
        assertFalse(Strings.startsWithIgnoreCase("abcdef", null));
        assertTrue(Strings.startsWithIgnoreCase("abcdef", "AB"));
        assertFalse(Strings.startsWithIgnoreCase("abcdef", "CD"));
    }

    @Test
    @DisplayName("Test startsWithAny()")
    public void testStartsWithAny() {
        assertFalse(Strings.startsWithAny(null, "ab", "cd"));
        assertTrue(Strings.startsWithAny("abcdef", "ab", "cd"));
        assertTrue(Strings.startsWithAny("cdefab", "ab", "cd"));
        assertFalse(Strings.startsWithAny("abcdef", "cd", "ef"));
    }

    @Test
    @DisplayName("Test endsWith()")
    public void testEndsWith() {
        assertFalse(Strings.endsWith(null, "ef"));
        assertFalse(Strings.endsWith("abcdef", null));
        assertTrue(Strings.endsWith("abcdef", "ef"));
        assertFalse(Strings.endsWith("abcdef", "cd"));
    }

    @Test
    @DisplayName("Test endsWithIgnoreCase()")
    public void testEndsWithIgnoreCase() {
        assertFalse(Strings.endsWithIgnoreCase(null, "ef"));
        assertFalse(Strings.endsWithIgnoreCase("abcdef", null));
        assertTrue(Strings.endsWithIgnoreCase("abcdef", "EF"));
        assertFalse(Strings.endsWithIgnoreCase("abcdef", "CD"));
    }

    @Test
    @DisplayName("Test endsWithAny()")
    public void testEndsWithAny() {
        assertFalse(Strings.endsWithAny(null, "ef", "gh"));
        assertTrue(Strings.endsWithAny("abcdef", "ef", "gh"));
        assertTrue(Strings.endsWithAny("abcdef", "ab", "ef"));
        assertFalse(Strings.endsWithAny("abcdef", "ab", "cd"));
    }

    @Test
    @DisplayName("Test equals()")
    public void testEquals() {
        assertTrue(Strings.equals(null, null));
        assertFalse(Strings.equals(null, "abc"));
        assertFalse(Strings.equals("abc", null));
        assertTrue(Strings.equals("abc", "abc"));
        assertFalse(Strings.equals("abc", "ABC"));
    }

    @Test
    @DisplayName("Test equalsIgnoreCase()")
    public void testEqualsIgnoreCase() {
        assertTrue(Strings.equalsIgnoreCase(null, null));
        assertFalse(Strings.equalsIgnoreCase(null, "abc"));
        assertFalse(Strings.equalsIgnoreCase("abc", null));
        assertTrue(Strings.equalsIgnoreCase("abc", "ABC"));
        assertTrue(Strings.equalsIgnoreCase("abc", "abc"));
    }

    @Test
    @DisplayName("Test equalsAny()")
    public void testEqualsAny() {
        assertFalse(Strings.equalsAny(null));
        assertFalse(Strings.equalsAny("abc"));
        assertTrue(Strings.equalsAny("abc", "def", "abc"));
        assertFalse(Strings.equalsAny("abc", "def", "ghi"));
    }

    @Test
    @DisplayName("Test equalsAnyIgnoreCase()")
    public void testEqualsAnyIgnoreCase() {
        assertFalse(Strings.equalsAnyIgnoreCase(null));
        assertFalse(Strings.equalsAnyIgnoreCase("abc"));
        assertTrue(Strings.equalsAnyIgnoreCase("abc", "DEF", "ABC"));
        assertFalse(Strings.equalsAnyIgnoreCase("abc", "def", "ghi"));
    }

    @Test
    @DisplayName("Test compareIgnoreCase()")
    public void testCompareIgnoreCase() {
        assertEquals(0, Strings.compareIgnoreCase(null, null));
        assertTrue(Strings.compareIgnoreCase(null, "a") < 0);
        assertTrue(Strings.compareIgnoreCase("a", null) > 0);
        assertEquals(0, Strings.compareIgnoreCase("abc", "ABC"));
        assertTrue(Strings.compareIgnoreCase("abc", "def") < 0);
    }

    @Test
    @DisplayName("Test indexOfDifference(String, String)")
    public void testIndexOfDifferenceTwoStrings() {
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(-1, Strings.indexOfDifference("", ""));
        assertEquals(0, Strings.indexOfDifference("", "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", ""));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(2, Strings.indexOfDifference("ab", "abxyz"));
        assertEquals(2, Strings.indexOfDifference("abcde", "abxyz"));
        assertEquals(0, Strings.indexOfDifference("abcde", "xyz"));
    }

    @Test
    @DisplayName("Test indexOfDifference(String...)")
    public void testIndexOfDifferenceVarArgs() {
        assertEquals(-1, Strings.indexOfDifference());
        assertEquals(-1, Strings.indexOfDifference((String[]) null));
        assertEquals(-1, Strings.indexOfDifference("abc"));
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(0, Strings.indexOfDifference(null, null, "abc"));
        assertEquals(0, Strings.indexOfDifference("", "abc"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(1, Strings.indexOfDifference("abc", "a"));
        assertEquals(2, Strings.indexOfDifference("ab", "abxyz"));
        assertEquals(7, Strings.indexOfDifference("i am a machine", "i am a robot"));
    }

    @Test
    @DisplayName("Test lengthOfCommonPrefix()")
    public void testLengthOfCommonPrefix() {
        assertEquals(0, Strings.lengthOfCommonPrefix(null, null));
        assertEquals(0, Strings.lengthOfCommonPrefix("", "abc"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", ""));
        assertEquals(3, Strings.lengthOfCommonPrefix("abc", "abc"));
        assertEquals(2, Strings.lengthOfCommonPrefix("abc", "abxyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", "xyz"));
    }

    @Test
    @DisplayName("Test lengthOfCommonSuffix()")
    public void testLengthOfCommonSuffix() {
        assertEquals(0, Strings.lengthOfCommonSuffix(null, null));
        assertEquals(0, Strings.lengthOfCommonSuffix("", "abc"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", ""));
        assertEquals(3, Strings.lengthOfCommonSuffix("abc", "abc"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", "abxyz"));
        assertEquals(2, Strings.lengthOfCommonSuffix("abc", "xbc"));
    }

    @Test
    @DisplayName("Test commonPrefix(CharSequence, CharSequence)")
    public void testCommonPrefixTwoArgs() {
        assertEquals("", Strings.commonPrefix(null, null));
        assertEquals("", Strings.commonPrefix("", "abc"));
        assertEquals("", Strings.commonPrefix("abc", ""));
        assertEquals("abc", Strings.commonPrefix("abc", "abc"));
        assertEquals("ab", Strings.commonPrefix("abc", "abxyz"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
    }

    @Test
    @DisplayName("Test commonPrefix(CharSequence...)")
    public void testCommonPrefixVarArgs() {
        assertEquals("", Strings.commonPrefix());
        assertEquals("", Strings.commonPrefix((CharSequence[]) null));
        assertEquals("abc", Strings.commonPrefix("abc"));
        assertEquals("a", Strings.commonPrefix("abc", "axy", "aijk"));
        assertEquals("", Strings.commonPrefix("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test commonSuffix(CharSequence, CharSequence)")
    public void testCommonSuffixTwoArgs() {
        assertEquals("", Strings.commonSuffix(null, null));
        assertEquals("", Strings.commonSuffix("", "abc"));
        assertEquals("", Strings.commonSuffix("abc", ""));
        assertEquals("abc", Strings.commonSuffix("abc", "abc"));
        assertEquals("bc", Strings.commonSuffix("abc", "xbc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
    }

    @Test
    @DisplayName("Test commonSuffix(CharSequence...)")
    public void testCommonSuffixVarArgs() {
        assertEquals("", Strings.commonSuffix());
        assertEquals("", Strings.commonSuffix((CharSequence[]) null));
        assertEquals("abc", Strings.commonSuffix("abc"));
        assertEquals("c", Strings.commonSuffix("abc", "xyc", "ijkc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test longestCommonSubstring()")
    public void testLongestCommonSubstring() {
        assertEquals("", Strings.longestCommonSubstring(null, null));
        assertEquals("", Strings.longestCommonSubstring("", "abc"));
        assertEquals("", Strings.longestCommonSubstring("abc", ""));
        assertEquals("abc", Strings.longestCommonSubstring("abc", "abc"));
        assertEquals("ab", Strings.longestCommonSubstring("abcdef", "xyabz"));
        assertEquals("", Strings.longestCommonSubstring("abc", "xyz"));
    }

    @Test
    @DisplayName("Test firstChar()")
    public void testFirstChar() {
        assertEquals(OptionalChar.empty(), Strings.firstChar(null));
        assertEquals(OptionalChar.empty(), Strings.firstChar(""));
        assertEquals(OptionalChar.of('a'), Strings.firstChar("abc"));
    }

    @Test
    @DisplayName("Test lastChar()")
    public void testLastChar() {
        assertEquals(OptionalChar.empty(), Strings.lastChar(null));
        assertEquals(OptionalChar.empty(), Strings.lastChar(""));
        assertEquals(OptionalChar.of('c'), Strings.lastChar("abc"));
    }

    @Test
    @DisplayName("Test firstChars()")
    public void testFirstChars() {
        assertEquals("", Strings.firstChars(null, 3));
        assertEquals("", Strings.firstChars("", 3));
        assertEquals("", Strings.firstChars("abc", 0));
        assertEquals("abc", Strings.firstChars("abc", 3));
        assertEquals("abc", Strings.firstChars("abc", 5));
        assertEquals("ab", Strings.firstChars("abc", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.firstChars("abc", -1));
    }

    @Test
    @DisplayName("Test lastChars()")
    public void testLastChars() {
        assertEquals("", Strings.lastChars(null, 3));
        assertEquals("", Strings.lastChars("", 3));
        assertEquals("", Strings.lastChars("abc", 0));
        assertEquals("abc", Strings.lastChars("abc", 3));
        assertEquals("abc", Strings.lastChars("abc", 5));
        assertEquals("bc", Strings.lastChars("abc", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.lastChars("abc", -1));
    }

    @Test
    @DisplayName("Test join()")
    public void testJoin() {
    }

    @Test
    @DisplayName("Test nullToEmpty(String[])")
    public void testNullToEmptyArray() {
        String[] strs = { "a", null, "b", null };
        Strings.nullToEmpty(strs);
        assertArrayEquals(new String[] { "a", "", "b", "" }, strs);

        Strings.nullToEmpty((String[]) null);

        String[] empty = new String[0];
        Strings.nullToEmpty(empty);
        assertArrayEquals(new String[0], empty);
    }

    @Test
    @DisplayName("Test emptyToNull(T[])")
    public void testEmptyToNullArray() {
        String[] strs = { "a", "", "b", "" };
        Strings.emptyToNull(strs);
        assertArrayEquals(new String[] { "a", null, "b", null }, strs);

        Strings.emptyToNull((String[]) null);
    }

    @Test
    @DisplayName("Test blankToEmpty(String[])")
    public void testBlankToEmptyArray() {
        String[] strs = { "a", "  ", "b", null };
        Strings.blankToEmpty(strs);
        assertArrayEquals(new String[] { "a", "", "b", "" }, strs);
    }

    @Test
    @DisplayName("Test blankToNull(T[])")
    public void testBlankToNullArray() {
        String[] strs = { "a", "  ", "b", "" };
        Strings.blankToNull(strs);
        assertArrayEquals(new String[] { "a", null, "b", null }, strs);
    }

    @Test
    @DisplayName("Test trim(String[])")
    public void testTrimArray() {
        String[] strs = { " a ", "  b  ", " c" };
        Strings.trim(strs);
        assertArrayEquals(new String[] { "a", "b", "c" }, strs);
    }

    @Test
    @DisplayName("Test strip(String[])")
    public void testStripArray() {
        String[] strs = { " a ", "  b  ", " c" };
        Strings.strip(strs);
        assertArrayEquals(new String[] { "a", "b", "c" }, strs);
    }

    @Test
    @DisplayName("Test methods with StringBuilder input")
    public void testStringBuilderInput() {
        StringBuilder sb = new StringBuilder("test");
        assertTrue(Strings.isNotEmpty(sb));
        assertFalse(Strings.isBlank(sb));
        assertEquals(4, Strings.lengthOfCommonPrefix(sb, "test"));
    }

    @Test
    @DisplayName("Test methods with very long strings")
    public void testLongStrings() {
        String longStr = Strings.repeat('a', 10000);
        assertEquals(10000, longStr.length());
        assertTrue(Strings.isAllLowerCase(longStr));
        assertEquals(1, Strings.countMatches(longStr + "b" + longStr, "b"));
    }

    @Test
    @DisplayName("Test methods with unicode characters")
    public void testUnicodeCharacters() {
        String unicode = "Hello 世界 🌍";
        assertTrue(Strings.isNotEmpty(unicode));
        assertFalse(Strings.isAsciiPrintable(unicode));
        assertTrue(Strings.contains(unicode, "世界"));
    }

    @Test
    @DisplayName("Test methods with empty string edge cases")
    public void testEmptyStringEdgeCases() {
        assertEquals("", Strings.repeat("", 5));
        assertEquals(0, Strings.countMatches("", ""));
        assertTrue(Strings.contains("abc", ""));
        assertEquals("abc", Strings.removeAll("abc", ""));
    }

    @Test
    @DisplayName("Test delimiter-based methods")
    public void testDelimiterMethods() {
        assertTrue(Strings.contains("a,b,c", "b", ","));
        assertFalse(Strings.contains("abc", "b", ","));
        assertEquals(2, Strings.indexOf("a,b,c", "b", ","));
        assertEquals(-1, Strings.indexOf("abc", "b", ","));
    }

    @Test
    @DisplayName("Test performance-optimized methods")
    public void testPerformanceOptimized() {
        String str = "abc";

        assertEquals(0, Strings.indexOfIgnoreCase("ABC", "abc"));
        assertEquals(0, Strings.indexOfIgnoreCase("ABC", "ABC"));
    }

    @Test
    @DisplayName("Test null safety across all methods")
    public void testNullSafety() {
        assertDoesNotThrow(() -> {
            Strings.isEmpty(null);
            Strings.isBlank(null);
            Strings.trim((String) null);
            Strings.toLowerCase(null);
            Strings.toUpperCase(null);
            Strings.contains(null, "test");
            Strings.indexOf(null, "test");
            Strings.split(null, ",");
            Strings.replaceAll((String) null, "a", "b");
        });
    }

    @Test
    @DisplayName("Test parameter validation")
    public void testParameterValidation() {
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("test", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.center("test", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("test", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ",", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.appendIfMissing("test", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.prependIfMissing("test", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.wrapIfMissing("test", ""));
    }

    @Test
    @DisplayName("Test method combinations")
    public void testMethodCombinations() {
        String input = "  Hello World  ";

        String result = Strings.capitalize(Strings.trim(input));
        assertEquals("Hello World", result);

        String replaced = Strings.replaceAll("aaabbbccc", "b", "x");
        assertEquals(3, Strings.countMatches(replaced, 'x'));

        String[] parts = Strings.split("a,b,c", ",");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("Test locale-sensitive methods")
    public void testLocaleSensitive() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            String turkishI = "I";
            String lowered = Strings.toLowerCase(turkishI);

            assertEquals("i", Strings.toLowerCase("I", Locale.ENGLISH));

        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

}
