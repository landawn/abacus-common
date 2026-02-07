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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.ExtractStrategy;
import com.landawn.abacus.util.Strings.StrUtil;

@Tag("new-test")
public class Strings200Test extends TestBase {

    private <T> List<T> list(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    private <T> Collection<T> toCollection(T... elements) {
        return Arrays.asList(elements);
    }

    @Test
    public void testConstants() {
        assertEquals("null", Strings.NULL);
        assertEquals("", Strings.EMPTY);
        assertEquals(" ", Strings.SPACE);
        assertEquals("\n", Strings.LF);
        assertEquals("\r", Strings.CR);
        assertEquals("\r\n", Strings.CR_LF);
        assertEquals('\0', Strings.CHAR_ZERO);
        assertEquals(' ', Strings.CHAR_SPACE);
        assertEquals('\n', Strings.CHAR_LF);
        assertEquals('\r', Strings.CHAR_CR);
        assertEquals(", ", Strings.COMMA_SPACE);
        assertEquals(", ", Strings.ELEMENT_SEPARATOR);
    }

    @Test
    public void testGuid() {
        String guid = Strings.uuidWithoutHyphens();
        assertNotNull(guid);
        assertEquals(32, guid.length());
        assertFalse(guid.contains("-"));
    }

    @Test
    public void testUuid() {
        String uuid = Strings.uuid();
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.contains("-"));
    }

    @Test
    public void testValueOfCharArray() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testIsKeyword() {
        assertTrue(Strings.isKeyword("int"));
        assertTrue(Strings.isKeyword("for"));
        assertFalse(Strings.isKeyword("myVar"));
        assertFalse(Strings.isKeyword(null));
        assertFalse(Strings.isKeyword(""));
    }

    @Test
    public void testIsValidJavaIdentifier() {
        assertTrue(Strings.isValidJavaIdentifier("myVar"));
        assertTrue(Strings.isValidJavaIdentifier("_myVar"));
        assertTrue(Strings.isValidJavaIdentifier("$myVar"));
        assertTrue(Strings.isValidJavaIdentifier("myVar123"));
        assertFalse(Strings.isValidJavaIdentifier("123myVar"));
        assertFalse(Strings.isValidJavaIdentifier("my-Var"));
        assertTrue(Strings.isValidJavaIdentifier("class"));
        assertFalse(Strings.isValidJavaIdentifier(null));
        assertFalse(Strings.isValidJavaIdentifier(""));
    }

    @Test
    public void testIsValidEmailAddress() {
        assertTrue(Strings.isValidEmailAddress("test@example.com"));
        assertTrue(Strings.isValidEmailAddress("test.name@example.co.uk"));
        assertFalse(Strings.isValidEmailAddress("test@example"));
        assertFalse(Strings.isValidEmailAddress("test"));
        assertFalse(Strings.isValidEmailAddress(null));
        assertFalse(Strings.isValidEmailAddress(""));
        assertTrue(Strings.isValidEmailAddress("user.name+tag+sorting@example.com"));
        assertFalse(Strings.isValidEmailAddress("user name@example.com"));

    }

    @Test
    public void testIsValidUrl() {
        assertTrue(Strings.isValidUrl("http://example.com"));
        assertTrue(Strings.isValidUrl("https://example.com/path?query=val#fragment"));
        assertTrue(Strings.isValidUrl("ftp://user:pass@example.com:21/path"));
        assertFalse(Strings.isValidUrl("example.com"));
        assertFalse(Strings.isValidHttpUrl("htp://example.com"));   // Invalid scheme
        assertFalse(Strings.isValidUrl(null));
        assertFalse(Strings.isValidUrl(""));
    }

    @Test
    public void testIsValidHttpUrl() {
        assertTrue(Strings.isValidHttpUrl("http://example.com"));
        assertTrue(Strings.isValidHttpUrl("https://example.com/path"));
        assertFalse(Strings.isValidHttpUrl("ftp://example.com"));
        assertFalse(Strings.isValidHttpUrl("example.com"));
        assertFalse(Strings.isValidHttpUrl(null));
        assertFalse(Strings.isValidHttpUrl(""));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("abc"));
    }

    @Test
    public void testIsBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertTrue(Strings.isBlank("\t\n\r "));
        assertFalse(Strings.isBlank("abc"));
        assertFalse(Strings.isBlank(" abc "));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(Strings.isNotEmpty(null));
        assertFalse(Strings.isNotEmpty(""));
        assertTrue(Strings.isNotEmpty(" "));
        assertTrue(Strings.isNotEmpty("abc"));
    }

    @Test
    public void testIsNotBlank() {
        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank(""));
        assertFalse(Strings.isNotBlank(" "));
        assertTrue(Strings.isNotBlank("abc"));
        assertTrue(Strings.isNotBlank(" abc "));
    }

    @Test
    public void testIsAllEmptyVarArgs() {
        assertTrue(Strings.isAllEmpty((CharSequence) null));
        assertTrue(Strings.isAllEmpty(null, null));
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertFalse(Strings.isAllEmpty("a", ""));
        assertFalse(Strings.isAllEmpty("a", "b"));
        assertTrue(Strings.isAllEmpty());
    }

    @Test
    public void testIsAllEmptyIterable() {
        assertTrue(Strings.isAllEmpty(list(null, "", null)));
        assertFalse(Strings.isAllEmpty(list(null, "a", "")));
        assertTrue(Strings.isAllEmpty(new ArrayList<>()));
        assertTrue(Strings.isAllEmpty((Iterable<String>) null));
    }

    @Test
    public void testIsAllBlankVarArgs() {
        assertTrue(Strings.isAllBlank((CharSequence) null));
        assertTrue(Strings.isAllBlank(null, null, " ", "\t"));
        assertFalse(Strings.isAllBlank(null, "a", " "));
        assertTrue(Strings.isAllBlank());
    }

    @Test
    public void testIsAllBlankIterable() {
        assertTrue(Strings.isAllBlank(list(null, " ", "\n")));
        assertFalse(Strings.isAllBlank(list(null, "a", " ")));
        assertTrue(Strings.isAllBlank(new ArrayList<>()));
        assertTrue(Strings.isAllBlank((Iterable<String>) null));
    }

    @Test
    public void testIsAnyEmptyVarArgs() {
        assertTrue(Strings.isAnyEmpty(null, "a"));
        assertTrue(Strings.isAnyEmpty("", "a"));
        assertFalse(Strings.isAnyEmpty("a", "b"));
        assertFalse(Strings.isAnyEmpty());
        assertTrue(Strings.isAnyEmpty("a", "b", null, "d"));
    }

    @Test
    public void testIsAnyEmptyIterable() {
        assertTrue(Strings.isAnyEmpty(list("a", "", "b")));
        assertFalse(Strings.isAnyEmpty(list("a", "b")));
        assertFalse(Strings.isAnyEmpty(new ArrayList<>()));
        assertFalse(Strings.isAnyEmpty((Iterable<String>) null));
    }

    @Test
    public void testIsAnyBlankVarArgs() {
        assertTrue(Strings.isAnyBlank(null, "a"));
        assertTrue(Strings.isAnyBlank(" ", "a"));
        assertFalse(Strings.isAnyBlank("a", "b"));
        assertFalse(Strings.isAnyBlank());
        assertTrue(Strings.isAnyBlank("a", "b", "\t", "d"));

    }

    @Test
    public void testIsAnyBlankIterable() {
        assertTrue(Strings.isAnyBlank(list("a", " ", "b")));
        assertFalse(Strings.isAnyBlank(list("a", "b")));
        assertFalse(Strings.isAnyBlank(new ArrayList<>()));
        assertFalse(Strings.isAnyBlank((Iterable<String>) null));
    }

    @Test
    public void testIsWrappedWithSingleArg() {
        assertFalse(Strings.isWrappedWith("[abc]", "["));
        assertFalse(Strings.isWrappedWith("[abc]", "["));
        assertTrue(Strings.isWrappedWith("wrap_me_wrap", "wrap"));
        assertFalse(Strings.isWrappedWith("wrap_me_no", "wrap"));
        assertFalse(Strings.isWrappedWith("nowrap_me_wrap", "wrap"));
        assertFalse(Strings.isWrappedWith("short", "longer_wrap"));
        assertFalse(Strings.isWrappedWith(null, "wrap"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", ""));
    }

    @Test
    public void testIsWrappedWithTwoArgs() {
        assertTrue(Strings.isWrappedWith("[abc]", "[", "]"));
        assertFalse(Strings.isWrappedWith("(abc]", "[", "]"));
        assertFalse(Strings.isWrappedWith("[abc)", "[", "]"));
        assertFalse(Strings.isWrappedWith("short", "long_prefix", "]"));
        assertFalse(Strings.isWrappedWith(null, "[", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", "", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", "[", ""));
    }

    @Test
    public void testFirstNonEmptyVarArgs() {
        assertEquals("a", Strings.firstNonEmpty(null, "", "a", "b"));
        assertEquals("", Strings.firstNonEmpty(null, ""));
        assertEquals("", Strings.firstNonEmpty());
    }

    @Test
    public void testFirstNonEmptyIterable() {
        assertEquals("a", Strings.firstNonEmpty(list(null, "", "a", "b")));
        assertEquals("", Strings.firstNonEmpty(list(null, "")));
        assertEquals("", Strings.firstNonEmpty(new ArrayList<>()));
        assertEquals("", Strings.firstNonEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstNonBlankVarArgs() {
        assertEquals("a", Strings.firstNonBlank(null, " ", "\t", "a", "b"));
        assertEquals("", Strings.firstNonBlank(null, " ", "\t"));
        assertEquals("", Strings.firstNonBlank());
    }

    @Test
    public void testFirstNonBlankIterable() {
        assertEquals("a", Strings.firstNonBlank(list(null, " ", "\t", "a", "b")));
        assertEquals("", Strings.firstNonBlank(list(null, " ", "\t")));
        assertEquals("", Strings.firstNonBlank(new ArrayList<>()));
        assertEquals("", Strings.firstNonBlank((Iterable<String>) null));
    }

    @Test
    public void testDefaultIfNull() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
        assertEquals("abc", Strings.defaultIfNull("abc", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("abc", (String) null));
    }

    @Test
    public void testDefaultIfNullSupplier() {
        assertEquals("supplied", Strings.defaultIfNull((String) null, Fn.s(() -> "supplied")));
        assertEquals("abc", Strings.defaultIfNull("abc", Fn.s(() -> "supplied")));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull((CharSequence) null, (Supplier<? extends CharSequence>) () -> null));
    }

    @Test
    public void testDefaultIfEmpty() {
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("abc", Strings.defaultIfEmpty("abc", "default"));
        assertEquals(" ", Strings.defaultIfEmpty(" ", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("abc", ""));
    }

    @Test
    public void testDefaultIfEmptySupplier() {
        Supplier<String> supplier = () -> "supplied";
        assertEquals("supplied", Strings.defaultIfEmpty("", supplier));
        assertEquals("abc", Strings.defaultIfEmpty("abc", supplier));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("", Fn.s(() -> "")));
    }

    @Test
    public void testDefaultIfBlank() {
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
        assertEquals("default", Strings.defaultIfBlank("", "default"));
        assertEquals("default", Strings.defaultIfBlank(" ", "default"));
        assertEquals("abc", Strings.defaultIfBlank("abc", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("abc", " "));
    }

    @Test
    public void testDefaultIfBlankSupplier() {
        Supplier<String> supplier = () -> "supplied";
        assertEquals("supplied", Strings.defaultIfBlank(" ", supplier));
        assertEquals("abc", Strings.defaultIfBlank("abc", supplier));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank(" ", Fn.s(() -> " ")));
    }

    @Test
    public void testNullToEmptyString() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("abc", Strings.nullToEmpty("abc"));
    }

    @Test
    public void testNullToEmptyArray() {
        String[] arr = { null, "a", null, "" };
        Strings.nullToEmpty(arr);
        assertArrayEquals(new String[] { "", "a", "", "" }, arr);
        Strings.nullToEmpty((String[]) null);
    }

    @Test
    public void testEmptyToNullString() {
        assertNull(Strings.emptyToNull((String) null));
        assertNull(Strings.emptyToNull(""));
        assertEquals("abc", Strings.emptyToNull("abc"));
        assertEquals(" ", Strings.emptyToNull(" "));
    }

    @Test
    public void testEmptyToNullArray() {
        String[] arr = { null, "a", "", " " };
        Strings.emptyToNull(arr);
        assertArrayEquals(new String[] { null, "a", null, " " }, arr);
        Strings.emptyToNull((String[]) null);
    }

    @Test
    public void testBlankToEmptyString() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("   "));
        assertEquals("abc", Strings.blankToEmpty("abc"));
    }

    @Test
    public void testBlankToEmptyArray() {
        String[] arr = { null, "a", " ", "\t" };
        Strings.blankToEmpty(arr);
        assertArrayEquals(new String[] { "", "a", "", "" }, arr);
    }

    @Test
    public void testBlankToNullString() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("   "));
        assertEquals("abc", Strings.blankToNull("abc"));
    }

    @Test
    public void testBlankToNullArray() {
        String[] arr = { null, "a", " ", "\t" };
        Strings.blankToNull(arr);
        assertArrayEquals(new String[] { null, "a", null, null }, arr);
    }

    @Test
    public void testAbbreviateMaxWidth() {
        assertNull(Strings.abbreviate(null, 10));
        assertEquals("", Strings.abbreviate("", 10));
        assertEquals("abc...", Strings.abbreviate("abcdefg", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 7));
        assertEquals("a...", Strings.abbreviate("abcdefg", 4));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abc", 3));
    }

    @Test
    public void testAbbreviateMarkerMaxWidth() {
        assertNull(Strings.abbreviate(null, "...", 10));
        assertEquals("", Strings.abbreviate("", "...", 10));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", null, 10));
        assertEquals("abc..", Strings.abbreviate("abcdefg", "..", 5));
        assertEquals("a..", Strings.abbreviate("abcdefg", "..", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abc", "..", 2));
    }

    @Test
    public void testAbbreviateMiddle() {
        assertNull(Strings.abbreviateMiddle(null, ".", 4));
        assertEquals("abc", Strings.abbreviateMiddle("abc", null, 4));
        assertEquals("abc", Strings.abbreviateMiddle("abc", ".", 0));
        assertEquals("abc", Strings.abbreviateMiddle("abc", ".", 3));
        assertEquals("ab.f", Strings.abbreviateMiddle("abcdef", ".", 4));
        assertEquals("a..f", Strings.abbreviateMiddle("abcdef", "..", 4));
        assertEquals("abcdef", Strings.abbreviateMiddle("abcdef", "...", 2));
    }

    @Test
    public void testCenterSize() {
        assertEquals("    ", Strings.center(null, 4));
        assertEquals("    ", Strings.center("", 4));
        assertEquals(" ab ", Strings.center("ab", 4));
        assertEquals("abcd", Strings.center("abcd", 2));
        assertEquals(" a  ", Strings.center("a", 4));
    }

    @Test
    public void testCenterSizeChar() {
        assertEquals("yyyy", Strings.center(null, 4, 'y'));
        assertEquals("yayy", Strings.center("a", 4, 'y'));
    }

    @Test
    public void testCenterSizeStr() {
        assertEquals("yzayz", Strings.center("a", 5, "yz"));
        assertEquals("  abc  ", Strings.center("abc", 7, ""));
        assertEquals("ab", Strings.center("ab", 1, "yz"));
    }

    @Test
    public void testPadStart() {
        assertEquals("  abc", Strings.padStart("abc", 5));
        assertEquals("xxabc", Strings.padStart("abc", 5, 'x'));
        assertEquals("xyzabc", Strings.padStart("abc", 6, "xyz"));
        assertEquals("abc", Strings.padStart("abc", 3, 'x'));
        assertEquals("abc", Strings.padStart("abc", 2));
        assertEquals("xyabc", Strings.padStart("abc", 5, "xy"));
        assertEquals("xyxyabc", Strings.padStart("abc", 7, "xy"));
    }

    @Test
    public void testPadEnd() {
        assertEquals("abc  ", Strings.padEnd("abc", 5));
        assertEquals("abcxx", Strings.padEnd("abc", 5, 'x'));
        assertEquals("abcxyz", Strings.padEnd("abc", 6, "xyz"));
    }

    @Test
    public void testRepeatChar() {
        assertEquals("aaa", Strings.repeat('a', 3));
        assertEquals("", Strings.repeat('a', 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    public void testRepeatCharWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("a", Strings.repeat('a', 1, ','));
        assertEquals("", Strings.repeat('a', 0, ','));
    }

    @Test
    public void testRepeatString() {
        assertEquals("ababab", Strings.repeat("ab", 3));
        assertEquals("", Strings.repeat("ab", 0));
        assertEquals("", Strings.repeat(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("ab", -1));
    }

    @Test
    public void testRepeatStringWithDelimiter() {
        assertEquals("ab,ab,ab", Strings.repeat("ab", 3, ","));
        assertEquals("[ab,ab]", Strings.repeat("ab", 2, ",", "[", "]"));
        assertEquals("[ab]", Strings.repeat("ab", 1, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("ab", 0, ",", "[", "]"));
        assertEquals("[,]", Strings.repeat("", 2, ",", "[", "]"));
    }

    @Test
    public void testGetBytes() {
        assertNull(Strings.getBytes(null));
        assertArrayEquals("abc".getBytes(), Strings.getBytes("abc"));
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_16), Strings.getBytes("abc", StandardCharsets.UTF_16));
    }

    @Test
    public void testGetBytesUtf8() {
        assertNull(Strings.getBytesUtf8(null));
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8("abc"));
    }

    @Test
    public void testToCharArray() {
        assertNull(Strings.toCharArray(null));
        assertArrayEquals(new char[0], Strings.toCharArray(""));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Strings.toCharArray("abc"));
    }

    @Test
    public void testToCodePoints() {
        assertNull(Strings.toCodePoints(null));
        assertArrayEquals(new int[0], Strings.toCodePoints(""));
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, Strings.toCodePoints("abc"));
        String smile = "\uD83D\uDE0E";
        assertArrayEquals(new int[] { 0x1F60E }, Strings.toCodePoints(smile));
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
    public void testToSnakeCase() {
        assertEquals("first_name", Strings.toSnakeCase("FirstName"));
        assertEquals("first_name_id", Strings.toSnakeCase("FirstNameID"));
        assertEquals("first__name", Strings.toSnakeCase("First__Name"));
        assertEquals("first_name", Strings.toSnakeCase("firstName"));
        assertEquals("fn", Strings.toSnakeCase("FN"));
        assertEquals("url_value", Strings.toSnakeCase("URLValue"));
        assertNull(Strings.toSnakeCase(null));
        assertEquals("", Strings.toSnakeCase(""));
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
    public void testToScreamingSnakeCase() {
        assertEquals("FIRST_NAME", Strings.toScreamingSnakeCase("firstName"));
        assertEquals("FIRST_NAME_ID", Strings.toScreamingSnakeCase("firstNameId"));
        assertEquals("FIRST__NAME", Strings.toScreamingSnakeCase("first__Name"));
        assertEquals("URL_VALUE", Strings.toScreamingSnakeCase("urlValue"));
        assertNull(Strings.toScreamingSnakeCase(null));
        assertEquals("", Strings.toScreamingSnakeCase(""));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("firstName", Strings.toCamelCase("first_name"));
        assertEquals("firstName", Strings.toCamelCase("First_Name"));
        assertEquals("firstName", Strings.toCamelCase("FIRST_NAME"));
        assertEquals("firstName", Strings.toCamelCase("firstName"));
        assertEquals("firstName", Strings.toCamelCase("FirstName"));
        assertEquals("fN", Strings.toCamelCase("F_N"));
        assertNull(Strings.toCamelCase(null));
        assertEquals("urlValue", Strings.toCamelCase("URL_Value"));
    }

    @Test
    public void testToUpperCamelCase() {
        assertEquals("FirstName", Strings.toUpperCamelCase("first_name"));
        assertEquals("FirstName", Strings.toUpperCamelCase("First_Name"));
        assertEquals("FirstName", Strings.toUpperCamelCase("FIRST_NAME"));
        assertEquals("FirstName", Strings.toUpperCamelCase("firstName"));
        assertEquals("FirstName", Strings.toUpperCamelCase("FirstName"));
        assertEquals("FN", Strings.toUpperCamelCase("F_N"));
        assertNull(Strings.toUpperCamelCase(null));
    }

    @Test
    public void testSwapCaseChar() {
        assertEquals('a', Strings.swapCase('A'));
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('1', Strings.swapCase('1'));
    }

    @Test
    public void testSwapCaseString() {
        assertNull(Strings.swapCase(null));
        assertEquals("", Strings.swapCase(""));
        assertEquals("tHE dOG hAS a bONE", Strings.swapCase("The Dog Has A Bone"));
    }

    @Test
    public void testUncapitalize() {
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
        assertEquals("cat", Strings.uncapitalize("Cat"));
        assertEquals("cat", Strings.uncapitalize("cat"));
        assertEquals("cAT", Strings.uncapitalize("CAT"));
    }

    @Test
    public void testCapitalize() {
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
        assertEquals("Cat", Strings.capitalize("cat"));
        assertEquals("Cat", Strings.capitalize("Cat"));
    }

    @Test
    public void testCapitalizeFully() {
        assertEquals("First Name Id", Strings.capitalizeFully("first name id"));
        assertEquals("First_Name_Id", Strings.capitalizeFully("first_name_id", "_"));
        assertNull(Strings.capitalizeFully(null));
        assertEquals("", Strings.capitalizeFully(""));
    }

    @Test
    public void testCapitalizeFullyWithExclusions() {
        assertEquals("The First Name of the Person", Strings.capitalizeFully("the first name of the person", " ", "the", "of"));
    }

    @Test
    public void testConvertWords() {
        assertEquals("ONE TWO THREE", Strings.convertWords("one two three", String::toUpperCase));
        assertEquals("one_TWO_three", Strings.convertWords("one_two_three", "_", s -> s.equals("two") ? s.toUpperCase() : s));
    }

    @Test
    public void testQuoteEscaped() {
        assertEquals("abc", Strings.quoteEscaped("abc"));
        assertEquals("ab\\\"c", Strings.quoteEscaped("ab\"c"));
        assertEquals("ab\\'c", Strings.quoteEscaped("ab'c"));
        assertEquals("ab\\\"c", Strings.quoteEscaped("ab\\\"c"));
        assertNull(Strings.quoteEscaped(null));
    }

    @Test
    public void testQuoteEscapedWithChar() {
        assertEquals("ab\\\"c", Strings.quoteEscaped("ab\"c", '"'));
        assertEquals("ab'c", Strings.quoteEscaped("ab'c", '"'));   // ' not escaped
        assertNull(Strings.quoteEscaped(null, '"'));
    }

    @Test
    public void testUnicodeEscaped() {
        assertEquals("\\u0020", Strings.unicodeEscaped(' '));
        assertEquals("\\u0041", Strings.unicodeEscaped('A'));
        assertEquals("\\u0100", Strings.unicodeEscaped('\u0100'));
        assertEquals("\\u1000", Strings.unicodeEscaped('\u1000'));
    }

    @Test
    public void testNormalizeSpace() {
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
        assertEquals("a b c", Strings.normalizeSpace("  a  b   c  "));
        assertEquals("a b c", Strings.normalizeSpace("a\tb\nc"));
    }

    @Test
    public void testReplaceAll() {
        assertEquals("zbz", Strings.replaceAll("aba", "a", "z"));
        assertEquals("b", Strings.replaceAll("abaa", "a", ""));
        assertEquals("aba", Strings.replaceAll("aba", "x", "z"));
        assertNull(Strings.replaceAll(null, "a", "z"));
    }

    @Test
    public void testReplaceAllWithFromIndex() {
        assertEquals("azbzz", Strings.replaceAll("aabaa", 1, "a", "z"));
    }

    @Test
    public void testReplaceFirst() {
        assertEquals("zbaa", Strings.replaceFirst("abaa", "a", "z"));
    }

    @Test
    public void testReplaceLast() {
        assertEquals("abz", Strings.replaceLast("aba", "a", "z"));
        assertEquals("ab", Strings.replaceLast("aba", "a", ""));
        assertEquals("aba", Strings.replaceLast("aba", "x", "z"));
    }

    @Test
    public void testReplaceLastWithStartIndex() {
        assertEquals("azbaa", Strings.replaceLast("aabaa", 2, "a", "z"));
        assertEquals("azbaa", Strings.replaceLast("aabaa", 2, "a", "z"));
        assertEquals("azbaa", Strings.replaceLast("aabaa", 1, "a", "z"));

    }

    @Test
    public void testReplaceRange() {
        assertEquals("abzzzzef", Strings.replaceRange("abcdef", 2, 4, "zzzz"));
        assertEquals("zzzzcdef", Strings.replaceRange("abcdef", 0, 2, "zzzz"));
        assertEquals("abcdzzzz", Strings.replaceRange("abcdef", 4, 6, "zzzz"));
        assertEquals("zzzz", Strings.replaceRange("abcdef", 0, 6, "zzzz"));
        assertEquals("abef", Strings.replaceRange("abcdef", 2, 4, ""));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceRange("abc", 0, 4, "z"));
    }

    @Test
    public void testReplaceIgnoreCase() {
        assertEquals("zzzzA", Strings.replaceAllIgnoreCase("aBaBA", "ab", "zz"));
        assertEquals("zzaBA", Strings.replaceFirstIgnoreCase("aBaBA", "ab", "zz"));
        assertEquals("aBzzA", Strings.replaceIgnoreCase("aBaBA", 1, "ab", "zz", 1));
    }

    @Test
    public void testRemoveStart() {
        assertEquals("domain.com", Strings.removeStart("www.domain.com", "www."));
        assertEquals("domain.com", Strings.removeStart("domain.com", "www."));
    }

    @Test
    public void testRemoveEnd() {
        assertEquals("www.domain", Strings.removeEnd("www.domain.com", ".com"));
    }

    @Test
    public void testRemoveAllChar() {
        assertEquals("qeed", Strings.removeAll("queued", 'u'));
        assertEquals("queued", Strings.removeAll("queued", 'z'));
    }

    @Test
    public void testRemoveAllString() {
        assertEquals("qd", Strings.removeAll("queued", "ue"));
    }

    @Test
    public void testSplitChar() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a.b.c", '.'));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a..b.c", '.'));
    }

    @Test
    public void testSplitStringTrim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a . b . c ", '.', true));
    }

    @Test
    public void testSplitStringMax() {
        assertArrayEquals(new String[] { "a", "b.c" }, Strings.split("a.b.c", ".", 2));
    }

    @Test
    public void testSplitPreserveAllTokensChar() {
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a..b.c", '.'));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", '.'));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, '.'));
        assertArrayEquals(new String[] { "", "a", "b", "" }, Strings.splitPreserveAllTokens(".a.b.", '.'));
    }

    @Test
    public void testSplitPreserveAllTokensStringMaxTrim() {
        assertArrayEquals(new String[] { "a", "b.c" }, Strings.splitPreserveAllTokens("a ..b.c ", "..", 3, true));
    }

    @Test
    public void testSplitToLines() {
        assertArrayEquals(new String[] { "a", "b", "c", "" }, Strings.splitToLines("a\nb\rc\r\n"));
        assertArrayEquals(new String[] { "" }, Strings.splitToLines(""));
        assertArrayEquals(new String[0], Strings.splitToLines(null));
    }

    @Test
    public void testSplitToLinesWithOptions() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines("a\n b \n\n c \r\n", true, true));
        assertArrayEquals(new String[] { "a", "b", "", "c", "" }, Strings.splitToLines("a\nb\n\nc\n", false, false));
    }

    @Test
    public void testTrim() {
        assertEquals("abc", Strings.trim("  abc  "));
        assertNull(Strings.trim((String) null));
    }

    @Test
    public void testTrimArray() {
        String[] arr = { "  a  ", null, "b" };
        Strings.trim(arr);
        assertArrayEquals(new String[] { "a", null, "b" }, arr);
    }

    @Test
    public void testStrip() {
        assertEquals("abc", Strings.strip("  abc  \t"));
        assertNull(Strings.strip((String) null));
    }

    @Test
    public void testStripWithChars() {
        assertEquals("abc", Strings.strip("xyabcxyz", "xyz"));
    }

    @Test
    public void testStripAccents() {
        assertEquals("eclair", Strings.stripAccents("éclair"));
        assertEquals("control", Strings.stripAccents("control"));
        assertNull(Strings.stripAccents((String) null));
    }

    @Test
    public void testChomp() {
        assertEquals("abc", Strings.chomp("abc\n"));
        assertEquals("abc", Strings.chomp("abc\r\n"));
        assertEquals("abc\r\n", Strings.chomp("abc\r\n\r\n"));
        assertNull(Strings.chomp((String) null));
    }

    @Test
    public void testChop() {
        assertEquals("abc", Strings.chop("abc\n"));
        assertEquals("ab", Strings.chop("abc"));
        assertEquals("abc", Strings.chop("abc\r\n"));
        assertNull(Strings.chop((String) null));
    }

    @Test
    public void testTruncate() {
        assertEquals("abcd", Strings.truncate("abcdefg", 4));
        assertEquals("abcdefg", Strings.truncate("abcdefg", 8));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", -1));
    }

    @Test
    public void testTruncateWithOffset() {
        assertEquals("cde", Strings.truncate("abcdefg", 2, 3));
        assertEquals("", Strings.truncate("abc", 3, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", -1, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", 0, -1));
    }

    @Test
    public void testDeleteWhitespace() {
        assertEquals("abc", Strings.deleteWhitespace(" a b  c "));
        assertNull(Strings.deleteWhitespace((String) null));
    }

    @Test
    public void testAppendIfMissing() {
        assertEquals("abc.txt", Strings.appendIfMissing("abc", ".txt"));
        assertEquals("abc.txt", Strings.appendIfMissing("abc.txt", ".txt"));
    }

    @Test
    public void testPrependIfMissing() {
        assertEquals("http://abc", Strings.prependIfMissing("abc", "http://"));
        assertEquals("http://abc", Strings.prependIfMissing("http://abc", "http://"));
    }

    @Test
    public void testWrapIfMissing() {
        assertEquals("[abc]", Strings.wrapIfMissing("abc", "[", "]"));
        assertEquals("[abc]", Strings.wrapIfMissing("[abc]", "[", "]"));
        assertEquals("[abc]", Strings.wrapIfMissing("[abc", "[", "]"));
        assertEquals("[abc]", Strings.wrapIfMissing("abc]", "[", "]"));
        assertEquals("[]", Strings.wrapIfMissing("", "[", "]"));
    }

    @Test
    public void testUnwrap() {
        assertEquals("abc", Strings.unwrap("[abc]", "[", "]"));
        assertEquals("[abc", Strings.unwrap("[abc", "[", "]"));
        assertEquals("abc]", Strings.unwrap("abc]", "[", "]"));
        assertEquals("", Strings.unwrap("[]", "[", "]"));
    }

    @Test
    public void testIsAllLowerCase() {
        assertTrue(Strings.isAllLowerCase("abc"));
        assertFalse(Strings.isAllLowerCase("aBc"));
        assertTrue(Strings.isAllLowerCase(""));
    }

    @Test
    public void testIsAllUpperCase() {
        assertTrue(Strings.isAllUpperCase("ABC"));
        assertFalse(Strings.isAllUpperCase("aBC"));
    }

    @Test
    public void testIsMixedCase() {
        assertTrue(Strings.isMixedCase("aBc"));
        assertFalse(Strings.isMixedCase("abc"));
        assertFalse(Strings.isMixedCase("ABC"));
        assertFalse(Strings.isMixedCase("a"));
        assertFalse(Strings.isMixedCase(""));
    }

    @Test
    public void testIsAsciiPrintableCharSequence() {
        assertTrue(Strings.isAsciiPrintable("abc"));
        assertFalse(Strings.isAsciiPrintable("éclair"));
        assertFalse(Strings.isAsciiPrintable(null));
    }

    @Test
    public void testIsNumeric() {
        assertTrue(Strings.isNumeric("123"));
        assertFalse(Strings.isNumeric("12.3"));
        assertFalse(Strings.isNumeric("12a"));
        assertFalse(Strings.isNumeric(""));
        assertFalse(Strings.isNumeric(null));
    }

    @Test
    public void testisAsciiNumber() {
        assertTrue(Strings.isAsciiNumber("123"));
        assertTrue(Strings.isAsciiNumber("123.45"));
        assertTrue(Strings.isAsciiNumber("-123.45"));
        assertTrue(Strings.isAsciiNumber("+123.45e10"));
        assertTrue(Strings.isAsciiNumber("2E-10"));
        assertFalse(Strings.isAsciiNumber("123."));
        assertFalse(Strings.isAsciiNumber(".123"));
        assertFalse(Strings.isAsciiNumber("1.2.3"));
        assertFalse(Strings.isAsciiNumber("abc"));
        assertFalse(Strings.isAsciiNumber(""));
        assertFalse(Strings.isAsciiNumber(null));
    }

    @Test
    public void testisAsciiInteger() {
        assertTrue(Strings.isAsciiInteger("123"));
        assertTrue(Strings.isAsciiInteger("-123"));
        assertTrue(Strings.isAsciiInteger("+0"));
        assertFalse(Strings.isAsciiInteger("123.0"));
        assertFalse(Strings.isAsciiInteger("12e3"));
        assertFalse(Strings.isAsciiInteger("abc"));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        assertEquals(0, Strings.indexOfIgnoreCase("aBaC", "ab"));
        assertEquals(2, Strings.indexOfIgnoreCase("aBaC", "ac"));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "X"));
    }

    @Test
    public void testLastIndexOfAny() {
        assertEquals(5, Strings.lastIndexOfAny("abacaba", 'b', 'c'));
        assertEquals(3, Strings.lastIndexOfAny("abacaba", 'c', 'b'));

        assertEquals(5, Strings.lastIndexOfAny("abacaba", 'b', 'c'));
        assertEquals(3, Strings.lastIndexOfAny("abacaba", 'c', 'b'));

    }

    @Test
    public void testminIndexOfAll() {
        assertEquals(1, Strings.minIndexOfAll("abracadabra", "bra", "cad"));
        assertEquals(-1, Strings.minIndexOfAll("abc", "d", "e"));
    }

    @Test
    public void testmaxIndexOfAll() {
        assertEquals(4, Strings.maxIndexOfAll("abracadabra", "bra", "cad"));
    }

    @Test
    public void testSubstring() {
        assertEquals("cde", Strings.substring("abcde", 2));
        assertNull(Strings.substring("abc", 5));
        assertNull(Strings.substring(null, 1));
    }

    @Test
    public void testSubstringWithEnd() {
        assertEquals("cd", Strings.substring("abcde", 2, 4));
        assertEquals("cde", Strings.substring("abcde", 2, 10));
        assertNull(Strings.substring("abc", 2, 1));
    }

    @Test
    public void testStrUtilSubstringOptional() {
        assertTrue(StrUtil.substring("abc", 1).isPresent());
        assertEquals("bc", StrUtil.substring("abc", 1).get());
        assertFalse(StrUtil.substring("abc", 5).isPresent());
    }

    @Test
    public void testSubstringAfter() {
        assertEquals("cde", Strings.substringAfter("abcde", "ab"));
        assertNull(Strings.substringAfter("abc", "x"));
        assertEquals("abc", Strings.substringAfter("abc", ""));
    }

    @Test
    public void testSubstringBefore() {
        assertEquals("ab", Strings.substringBefore("abcde", "cd"));
        assertNull(Strings.substringBefore("abc", "x"));
        assertEquals("", Strings.substringBefore("abc", ""));
    }

    @Test
    public void testSubstringBetween() {
        assertEquals("b", Strings.substringBetween("axbyc", "ax", "yc"));
        assertEquals("abc", Strings.substringBetween("(abc)", "(", ")"));
        assertNull(Strings.substringBetween("abc", "[", "]"));
    }

    @Test
    public void testSubstringsBetweenDefaultStrategy() {
        assertEquals(list("a", "b"), Strings.substringsBetween("[a][b]", '[', ']'));
        assertEquals(list("a[b"), Strings.substringsBetween("[a[b]c]", '[', ']'));
    }

    private List<String> substringsBetween_Default_(String str, char open, char close) {
        return Strings.substringsBetween(str, open, close, ExtractStrategy.DEFAULT);
    }

    private List<String> substringsBetween_StackBased_(String str, char open, char close) {
        return Strings.substringsBetween(str, open, close, ExtractStrategy.STACK_BASED);
    }

    private List<String> substringsBetween_IgnoreNested_(String str, char open, char close) {
        return Strings.substringsBetween(str, open, close, ExtractStrategy.IGNORE_NESTED);
    }

    @Test
    public void testSubstringsBetweenStackBased() {
        assertEquals(list("c", "a2[c]", "a"), substringsBetween_StackBased_("3[a2[c]]2[a]", '[', ']'));
        assertEquals(list("a", "b[a]", "[b[a]]c"), substringsBetween_StackBased_("[[b[a]]c]", '[', ']'));
    }

    @Test
    public void testSubstringsBetweenIgnoreNested() {
        assertEquals(list("a2[c]", "a"), substringsBetween_IgnoreNested_("3[a2[c]]2[a]", '[', ']'));
        assertEquals(list("[b[a]]c"), substringsBetween_IgnoreNested_("[[b[a]]c]", '[', ']'));
    }

    @Test
    public void testJoinObjectArray() {
        assertEquals("a, b, c", Strings.join(new Object[] { "a", "b", "c" }));
        assertEquals("a|b|c", Strings.join(new Object[] { "a", "b", "c" }, "|"));
        assertEquals("[a|b|c]", Strings.join(new Object[] { "a", "b", "c" }, "|", "[", "]"));
    }

    @Test
    public void testJoinIterable() {
        assertEquals("1, 2, 3", Strings.join(list(1, 2, 3)));
        assertEquals("1-2-3", Strings.join(list(1, 2, 3), "-"));
    }

    @Test
    public void testJoinEntriesMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a=1, b=2", Strings.joinEntries(map));
        assertEquals("a:1;b:2", Strings.joinEntries(map, ";", ":"));
        assertEquals("{a:1;b:2}", Strings.joinEntries(map, ";", ":", "{", "}"));
    }

    @Test
    public void testConcatStrings() {
        assertEquals("abc", Strings.concat("a", "b", "c"));
        assertEquals("a", Strings.concat("a", null, ""));
        assertEquals("", Strings.concat(null, null));
    }

    @Test
    public void testLenientFormat() {
        assertEquals("Hello World", Strings.lenientFormat("Hello %s", "World"));
        assertEquals("Hello null", Strings.lenientFormat("Hello %s", (Object) null));
        assertEquals("Value: 10, Name: Test", Strings.lenientFormat("Value: %s, Name: %s", 10, "Test"));
        assertEquals("Too few arg1 placeholders: [arg2]", Strings.lenientFormat("Too few %s placeholders", "arg1", "arg2"));
        assertEquals("Too many arg1: [arg2]", Strings.lenientFormat("Too many %s", "arg1", "arg2"));
        assertEquals("No placeholders: [arg1]", Strings.lenientFormat("No placeholders", "arg1"));
    }

    @Test
    public void testReverse() {
        assertEquals("cba", Strings.reverse("abc"));
        assertNull(Strings.reverse(null));
    }

    @Test
    public void testReverseDelimited() {
        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", '.'));
        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));
    }

    @Test
    public void testSort() {
        assertEquals("abc", Strings.sort("cba"));
        assertNull(Strings.sort(null));
    }

    @Test
    public void testRotate() {
        assertEquals("fgabcde", Strings.rotate("abcdefg", 2));
        assertEquals("cdefgab", Strings.rotate("abcdefg", -2));
        assertEquals("abcdefg", Strings.rotate("abcdefg", 7));
        assertNull(Strings.rotate(null, 2));
    }

    @Test
    public void testShuffle() {
        String original = "abcdefg";
        String shuffled = Strings.shuffle(original);
        assertEquals(original.length(), shuffled.length());
        int[] originalCounts = original.chars().sorted().toArray();
        int[] shuffledCounts = shuffled.chars().sorted().toArray();
        assertArrayEquals(originalCounts, shuffledCounts);

    }

    @Test
    public void testParseBoolean() {
        assertTrue(Strings.parseBoolean("true"));
        assertFalse(Strings.parseBoolean("false"));
        assertTrue(Strings.parseBoolean("TRUE"));
        assertFalse(Strings.parseBoolean("text"));
        assertFalse(Strings.parseBoolean(null));
    }

    @Test
    public void testParseChar() {
        assertEquals('a', Strings.parseChar("a"));
        assertEquals((char) 65, Strings.parseChar("65"));
        assertEquals('\0', Strings.parseChar(""));
        assertEquals('\0', Strings.parseChar(null));
        assertThrows(NumberFormatException.class, () -> Strings.parseChar("abc"));
    }

    @Test
    public void testBase64() {
        String original = "hello world";
        String encoded = Strings.base64EncodeString(original);
        assertNotNull(encoded);
        assertNotEquals(original, encoded);
        assertEquals(original, Strings.base64DecodeToString(encoded));

        byte[] data = { 0, 1, 2, 3, 4, 5 };
        String encodedBytes = Strings.base64Encode(data);
        assertArrayEquals(data, Strings.base64Decode(encodedBytes));

        String originalUrl = "abc?=/&123";
        byte[] originalUrlBytes = Strings.getBytesUtf8(originalUrl);
        String urlEncoded = Strings.base64UrlEncode(originalUrlBytes);
        assertFalse(urlEncoded.contains("+"));
        assertFalse(urlEncoded.contains("/"));
        assertFalse(urlEncoded.contains("="));
        assertArrayEquals(originalUrlBytes, Strings.base64UrlDecode(urlEncoded));
        assertEquals(originalUrl, Strings.base64UrlDecodeToUtf8String(urlEncoded));
    }

    @Test
    public void testIsBase64() {
        assertTrue(Strings.isBase64((byte) 'A'));
        assertTrue(Strings.isBase64((byte) '+'));
        assertTrue(Strings.isBase64((byte) '/'));
        assertTrue(Strings.isBase64((byte) '='));
        assertFalse(Strings.isBase64((byte) '*'));

        assertTrue(Strings.isBase64("SGVsbG8gd29ybGQ="));
        assertTrue(Strings.isBase64("SGVsbG8gd29ybGQ"));
        assertFalse(Strings.isBase64("SGVsbG8gd29ybGQ*"));
        assertTrue(Strings.isBase64(""));
        assertTrue(Strings.isBase64(new byte[] {}));
    }

    @Test
    public void testUrlEncodeDecode() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "John Doe");
        params.put("city", "New York");
        String encoded = Strings.urlEncode(params);
        assertEquals("name=John+Doe&city=New+York", encoded);

        Map<String, String> decoded = Strings.urlDecode(encoded);
        assertEquals("John Doe", decoded.get("name"));
        assertEquals("New York", decoded.get("city"));
    }

    @Test
    public void testFindFirstEmailAddress() {
        assertEquals("test@example.com", Strings.findFirstEmailAddress("Contact test@example.com for info."));
        assertNull(Strings.findFirstEmailAddress("No email here."));
    }

    @Test
    public void testFindAllEmailAddresses() {
        assertEquals(list("one@example.com", "two@test.org"), Strings.findAllEmailAddresses("Emails: one@example.com, two@test.org."));
        assertTrue(Strings.findAllEmailAddresses("None").isEmpty());
    }

    @Test
    public void testCopyThenTrim() {
        String[] original = { " a ", "b ", " c" };
        String[] trimmed = Strings.copyThenTrim(original);
        assertNotSame(original, trimmed);
        assertArrayEquals(new String[] { "a", "b", "c" }, trimmed);
        assertArrayEquals(new String[] { " a ", "b ", " c" }, original);
        assertNull(Strings.copyThenTrim(null));
    }

    @Test
    public void testCopyThenStrip() {
        String[] original = { " a\t", "\nb ", " c" };
        String[] stripped = Strings.copyThenStrip(original);
        assertNotSame(original, stripped);
        assertArrayEquals(new String[] { "a", "b", "c" }, stripped);
    }

    @Test
    public void testExtractFirstInteger() {
        assertEquals("123", Strings.extractFirstInteger("abc 123 def 456"));
        assertEquals("-123", Strings.extractFirstInteger("abc -123 def"));
        assertNull(Strings.extractFirstInteger("abc def"));
    }

    @Test
    public void testExtractFirstDouble() {
        assertEquals("123.45", Strings.extractFirstDouble("abc 123.45 def"));
        assertEquals("-0.5", Strings.extractFirstDouble("val -0.5"));
        assertEquals("1e5", Strings.extractFirstDouble("num 1e5", true));
        assertNull(Strings.extractFirstDouble("abc", true));
    }

    @Test
    public void testReplaceFirstInteger() {
        assertEquals("abc XX def 456", Strings.replaceFirstInteger("abc 123 def 456", "XX"));
    }

    @Test
    public void testReplaceFirstDouble() {
        assertEquals("val REPL and more", Strings.replaceFirstDouble("val -0.5 and more", "REPL"));
        assertEquals("num REPL stuff", Strings.replaceFirstDouble("num 1.2e-3 stuff", "REPL", true));
    }

    @Test
    public void testStrUtilSubstring() {
        assertTrue(StrUtil.substring("abc", 1).isPresent());
        assertEquals("bc", StrUtil.substring("abc", 1).get());
        assertFalse(StrUtil.substring("abc", 4).isPresent());
        assertFalse(StrUtil.substring(null, 1).isPresent());

        assertTrue(StrUtil.substring("abc", 1, 2).isPresent());
        assertEquals("b", StrUtil.substring("abc", 1, 2).get());
        assertFalse(StrUtil.substring("abc", 2, 1).isPresent());
    }

    @Test
    public void testStrUtilSubstringOrElse() {
        assertEquals("bc", StrUtil.substringOrElse("abc", 1, "default"));
        assertEquals("default", StrUtil.substringOrElse("abc", 4, "default"));
        assertEquals("default", StrUtil.substringOrElse(null, 1, "default"));
    }

    @Test
    public void testStrUtilSubstringOrElseItself() {
        assertEquals("bc", StrUtil.substringOrElseItself("abc", 1));
        assertEquals("abc", StrUtil.substringOrElseItself("abc", 4));
        assertNull(StrUtil.substringOrElseItself(null, 1));
    }

    @Test
    public void testStrUtilSubstringAfter() {
        assertEquals("cde", StrUtil.substringAfter("abcde", "ab").get());
        assertFalse(StrUtil.substringAfter("abc", "x").isPresent());
    }

    @Test
    public void testStrUtilSubstringBefore() {
        assertEquals("ab", StrUtil.substringBefore("abcde", "cd").get());
        assertFalse(StrUtil.substringBefore("abc", "x").isPresent());
    }

    @Test
    public void testStrUtilSubstringBetween() {
        assertEquals("abc", StrUtil.substringBetween("(abc)", "(", ")").get());
        assertFalse(StrUtil.substringBetween("abc", "[", "]").isPresent());
    }

    @Test
    public void testStrUtilCreateInteger() {
        assertTrue(StrUtil.createInteger("123").isPresent());
        assertEquals(123, StrUtil.createInteger("123").getAsInt());
        assertFalse(StrUtil.createInteger("abc").isPresent());
        assertFalse(StrUtil.createInteger("").isPresent());
        assertFalse(StrUtil.createInteger(null).isPresent());
        assertFalse(StrUtil.createInteger("123.45").isPresent());
    }

    @Test
    public void testStrUtilCreateLong() {
        assertTrue(StrUtil.createLong("12345678900").isPresent());
        assertEquals(12345678900L, StrUtil.createLong("12345678900").getAsLong());
        assertFalse(StrUtil.createLong("abc").isPresent());
    }

    @Test
    public void testStrUtilCreateFloat() {
        assertTrue(StrUtil.createFloat("123.45").isPresent());
        assertEquals(123.45f, StrUtil.createFloat("123.45").get(), 0.001f);
        assertFalse(StrUtil.createFloat("abc").isPresent());
    }

    @Test
    public void testStrUtilCreateDouble() {
        assertTrue(StrUtil.createDouble("123.456789").isPresent());
        assertEquals(123.456789, StrUtil.createDouble("123.456789").getAsDouble(), 0.000001);
        assertFalse(StrUtil.createDouble("abc").isPresent());
    }

    @Test
    public void testStrUtilCreateBigInteger() {
        assertTrue(StrUtil.createBigInteger("12345678901234567890").isPresent());
        assertEquals(new BigInteger("12345678901234567890"), StrUtil.createBigInteger("12345678901234567890").get());
        assertFalse(StrUtil.createBigInteger("abc.def").isPresent());
    }

    @Test
    public void testStrUtilCreateBigDecimal() {
        assertTrue(StrUtil.createBigDecimal("123.4567890123456789").isPresent());
        assertEquals(new BigDecimal("123.4567890123456789"), StrUtil.createBigDecimal("123.4567890123456789").get());
        assertFalse(StrUtil.createBigDecimal("abc efg").isPresent());
    }

    @Test
    public void testStrUtilCreateNumber() {
        assertTrue(StrUtil.createNumber("123").isPresent());
        assertEquals(123, StrUtil.createNumber("123").get());
        assertTrue(StrUtil.createNumber("123.45").isPresent());
        assertEquals(123.45, StrUtil.createNumber("123.45").get());
        assertFalse(StrUtil.createNumber("12L3").isPresent());
    }
}
