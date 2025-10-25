package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.base64Decode;
import static com.landawn.abacus.util.Strings.base64DecodeToString;
import static com.landawn.abacus.util.Strings.base64DecodeToUtf8String;
import static com.landawn.abacus.util.Strings.base64Encode;
import static com.landawn.abacus.util.Strings.base64EncodeString;
import static com.landawn.abacus.util.Strings.base64EncodeUtf8String;
import static com.landawn.abacus.util.Strings.base64UrlDecode;
import static com.landawn.abacus.util.Strings.base64UrlDecodeToString;
import static com.landawn.abacus.util.Strings.base64UrlDecodeToUtf8String;
import static com.landawn.abacus.util.Strings.base64UrlEncode;
import static com.landawn.abacus.util.Strings.concat;
import static com.landawn.abacus.util.Strings.copyThenStrip;
import static com.landawn.abacus.util.Strings.copyThenTrim;
import static com.landawn.abacus.util.Strings.deleteRange;
import static com.landawn.abacus.util.Strings.extractFirstDouble;
import static com.landawn.abacus.util.Strings.extractFirstInteger;
import static com.landawn.abacus.util.Strings.findAllEmailAddresses;
import static com.landawn.abacus.util.Strings.findFirstEmailAddress;
import static com.landawn.abacus.util.Strings.isBase64;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.joinEntries;
import static com.landawn.abacus.util.Strings.lenientFormat;
import static com.landawn.abacus.util.Strings.moveRange;
import static com.landawn.abacus.util.Strings.parseBoolean;
import static com.landawn.abacus.util.Strings.parseChar;
import static com.landawn.abacus.util.Strings.replaceFirstDouble;
import static com.landawn.abacus.util.Strings.replaceFirstInteger;
import static com.landawn.abacus.util.Strings.replaceRange;
import static com.landawn.abacus.util.Strings.reverse;
import static com.landawn.abacus.util.Strings.reverseDelimited;
import static com.landawn.abacus.util.Strings.rotate;
import static com.landawn.abacus.util.Strings.shuffle;
import static com.landawn.abacus.util.Strings.sort;
import static com.landawn.abacus.util.Strings.substring;
import static com.landawn.abacus.util.Strings.substringAfter;
import static com.landawn.abacus.util.Strings.substringAfterAny;
import static com.landawn.abacus.util.Strings.substringAfterIgnoreCase;
import static com.landawn.abacus.util.Strings.substringAfterLast;
import static com.landawn.abacus.util.Strings.substringAfterLastIgnoreCase;
import static com.landawn.abacus.util.Strings.substringBefore;
import static com.landawn.abacus.util.Strings.substringBeforeAny;
import static com.landawn.abacus.util.Strings.substringBeforeIgnoreCase;
import static com.landawn.abacus.util.Strings.substringBeforeLast;
import static com.landawn.abacus.util.Strings.substringBeforeLastIgnoreCase;
import static com.landawn.abacus.util.Strings.substringBetween;
import static com.landawn.abacus.util.Strings.substringBetweenIgnoreCaes;
import static com.landawn.abacus.util.Strings.substringIndicesBetween;
import static com.landawn.abacus.util.Strings.substringsBetween;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.StrUtil;

@Tag("new-test")
public class Strings101Test extends TestBase {

    @Test
    public void testConstants() {
        assertEquals("null", Strings.NULL);
        assertEquals("", Strings.EMPTY);
        assertEquals(" ", Strings.SPACE);
        assertEquals("\n", Strings.LF);
        assertEquals("\r", Strings.CR);
        assertEquals("\r\n", Strings.CR_LF);
        assertEquals(", ", Strings.COMMA_SPACE);
        assertEquals(", ", Strings.ELEMENT_SEPARATOR);
    }

    @Test
    public void testSubstring_WithInclusiveBeginIndex() {
        assertEquals("World", substring("Hello World", 6));
        assertEquals("Hello World", substring("Hello World", 0));
        assertNull(substring("Hello", -1));
        assertNull(substring("Hello", 6));
        assertNull(substring(null, 0));
        assertEquals("", substring("Hello", 5));
    }

    @Test
    public void testSubstring_WithInclusiveBeginAndExclusiveEndIndex() {
        assertEquals("Hello", substring("Hello World", 0, 5));
        assertEquals("World", substring("Hello World", 6, 11));
        assertEquals("", substring("Hello", 2, 2));
        assertNull(substring("Hello", -1, 5));
        assertNull(substring("Hello", 0, -1));
        assertNull(substring("Hello", 3, 2));
        assertNull(substring(null, 0, 5));
        assertEquals("ello", substring("Hello", 1, 10));
    }

    @Test
    public void testSubstring_WithFunctionOfExclusiveEndIndex() {
        assertEquals("Hel", substring("Hello", 0, i -> i + 3));
        assertEquals("", substring("Hello", 2, i -> i));
        assertNull(substring("Hello", 0, i -> -1));
        assertNull(substring(null, 0, i -> 5));
        assertNull(substring("Hello", -1, i -> 5));
    }

    @Test
    public void testSubstring_WithFunctionOfInclusiveBeginIndex() {
        assertEquals("lo", substring("Hello", i -> i - 2, 5));
        assertEquals("", substring("Hello", i -> i, 3));
        assertNull(substring("Hello", i -> -1, 5));
        assertNull(substring(null, i -> 0, 5));
        assertNull(substring("Hello", i -> 0, -1));
    }

    @Test
    public void testSubstringAfter_Char() {
        assertEquals("llo World", substringAfter("Hello World", 'e'));
        assertEquals("World", substringAfter("Hello World", ' '));
        assertEquals("", substringAfter("Hello", 'o'));
        assertNull(substringAfter("Hello", 'x'));
        assertNull(substringAfter(null, 'a'));
        assertNull(substringAfter("", 'a'));
    }

    @Test
    public void testSubstringAfter_String() {
        assertEquals("World", substringAfter("Hello World", "Hello "));
        assertEquals(" World", substringAfter("Hello World", "Hello"));
        assertEquals("", substringAfter("Hello", "Hello"));
        assertNull(substringAfter("Hello", "xyz"));
        assertNull(substringAfter(null, "test"));
        assertNull(substringAfter("test", null));
        assertEquals("test", substringAfter("test", ""));
    }

    @Test
    public void testSubstringAfter_StringWithEndIndex() {
        assertEquals("Wo", substringAfter("Hello World", "Hello ", 8));
        assertNull(substringAfter("Hello World", "World", 8));
        assertNull(substringAfter(null, "test", 5));
        assertNull(substringAfter("test", null, 5));
        assertNull(substringAfter("test", "test", -1));
        assertEquals("te", substringAfter("test", "", 2));
        assertNull(substringAfter("test", "es", 0));
    }

    @Test
    public void testSubstringAfterIgnoreCase() {
        assertEquals("World", substringAfterIgnoreCase("Hello World", "hello "));
        assertEquals(" World", substringAfterIgnoreCase("Hello World", "HELLO"));
        assertNull(substringAfterIgnoreCase("Hello", "xyz"));
        assertNull(substringAfterIgnoreCase(null, "test"));
        assertNull(substringAfterIgnoreCase("test", null));
        assertEquals("test", substringAfterIgnoreCase("test", ""));
    }

    @Test
    public void testSubstringAfterLast_Char() {
        assertEquals("txt", substringAfterLast("file.name.txt", '.'));
        assertEquals("", substringAfterLast("Hello World!", '!'));
        assertNull(substringAfterLast("Hello", 'x'));
        assertNull(substringAfterLast(null, 'a'));
        assertNull(substringAfterLast("", 'a'));
    }

    @Test
    public void testSubstringAfterLast_String() {
        assertEquals("txt", substringAfterLast("file.name.txt", "."));
        assertEquals("789", substringAfterLast("123.456.789", "."));
        assertNull(substringAfterLast("Hello", "xyz"));
        assertNull(substringAfterLast(null, "test"));
        assertNull(substringAfterLast("test", null));
        assertEquals("", substringAfterLast("test", ""));
    }

    @Test
    public void testSubstringAfterLast_StringWithEndIndex() {
        assertEquals("name", substringAfterLast("file.name.txt", ".", 9));
        assertNull(substringAfterLast("file.name.txt", ".", 3));
        assertNull(substringAfterLast(null, ".", 5));
        assertNull(substringAfterLast("test", null, 5));
        assertNull(substringAfterLast("test", ".", -1));
        assertEquals("", substringAfterLast("test", "", 3));
    }

    @Test
    public void testSubstringAfterLastIgnoreCase() {
        assertEquals("txt", substringAfterLastIgnoreCase("file.NAME.txt", ".name."));
        assertEquals("789", substringAfterLastIgnoreCase("123.ABC.789", ".abc."));
        assertNull(substringAfterLastIgnoreCase("Hello", "xyz"));
        assertNull(substringAfterLastIgnoreCase(null, "test"));
        assertNull(substringAfterLastIgnoreCase("test", null));
        assertEquals("", substringAfterLastIgnoreCase("test", ""));
    }

    @Test
    public void testSubstringAfterAny_Chars() {
        assertEquals("llo World", substringAfterAny("Hello World", 'x', 'e', 'z'));
        assertEquals(" World", substringAfterAny("Hello World", 'o', ' '));
        assertNull(substringAfterAny("Hello", 'x', 'y', 'z'));
        assertNull(substringAfterAny(null, 'a', 'b'));
        assertNull(substringAfterAny("test", CommonUtil.EMPTY_CHAR_ARRAY));
    }

    @Test
    public void testSubstringAfterAny_Strings() {
        assertEquals("World", substringAfterAny("Hello World", "xyz", "Hello ", "abc"));
        assertEquals(" World", substringAfterAny("Hello World", "abc", "Hello", "xyz"));
        assertNull(substringAfterAny("Hello", "xyz", "abc"));
        assertNull(substringAfterAny(null, "test", "abc"));
        assertNull(substringAfterAny("test", CommonUtil.EMPTY_STRING_ARRAY));
    }

    @Test
    public void testSubstringBefore_Char() {
        assertEquals("He", substringBefore("Hello World", 'l'));
        assertEquals("Hello", substringBefore("Hello World", ' '));
        assertNull(substringBefore("Hello", 'x'));
        assertNull(substringBefore(null, 'a'));
    }

    @Test
    public void testSubstringBefore_String() {
        assertEquals("Hello", substringBefore("Hello World", " World"));
        assertEquals("", substringBefore("Hello World", "Hello"));
        assertNull(substringBefore("Hello", "xyz"));
        assertNull(substringBefore(null, "test"));
        assertNull(substringBefore("test", null));
        assertEquals("", substringBefore("test", ""));
    }

    @Test
    public void testSubstringBefore_StringWithBeginIndex() {
        assertEquals("lo", substringBefore("Hello World", 3, " World"));
        assertNull(substringBefore("Hello World", 7, " "));
        assertNull(substringBefore(null, 0, "test"));
        assertNull(substringBefore("test", 0, null));
        assertNull(substringBefore("test", -1, "st"));
        assertNull(substringBefore("test", 5, "st"));
        assertEquals("", substringBefore("test", 0, ""));
    }

    @Test
    public void testSubstringBeforeIgnoreCase() {
        assertEquals("Hello", substringBeforeIgnoreCase("Hello WORLD", " world"));
        assertEquals("", substringBeforeIgnoreCase("Hello World", "HELLO"));
        assertNull(substringBeforeIgnoreCase("Hello", "xyz"));
        assertNull(substringBeforeIgnoreCase(null, "test"));
        assertNull(substringBeforeIgnoreCase("test", null));
        assertEquals("", substringBeforeIgnoreCase("test", ""));
    }

    @Test
    public void testSubstringBeforeLast_Char() {
        assertEquals("file.name", substringBeforeLast("file.name.txt", '.'));
        assertEquals("Hello World", substringBeforeLast("Hello World!", '!'));
        assertNull(substringBeforeLast("Hello", 'x'));
        assertNull(substringBeforeLast(null, 'a'));
        assertNull(substringBeforeLast("", 'a'));
    }

    @Test
    public void testSubstringBeforeLast_String() {
        assertEquals("file.name", substringBeforeLast("file.name.txt", "."));
        assertEquals("123.456", substringBeforeLast("123.456.789", "."));
        assertNull(substringBeforeLast("Hello", "xyz"));
        assertNull(substringBeforeLast(null, "test"));
        assertNull(substringBeforeLast("test", null));
        assertEquals("test", substringBeforeLast("test", ""));
    }

    @Test
    public void testSubstringBeforeLast_StringWithBeginIndex() {
        assertEquals("le.name", substringBeforeLast("file.name.txt", 2, "."));
        assertNull(substringBeforeLast("file.name.txt", 10, "."));
        assertNull(substringBeforeLast(null, 0, "."));
        assertNull(substringBeforeLast("test", 0, null));
        assertNull(substringBeforeLast("test", -1, "."));
        assertEquals("st", substringBeforeLast("test", 2, ""));
    }

    @Test
    public void testSubstringBeforeLastIgnoreCase() {
        assertEquals("file.NAME", substringBeforeLastIgnoreCase("file.NAME.TXT", ".txt"));
        assertEquals("123.ABC", substringBeforeLastIgnoreCase("123.ABC.789", ".789"));
        assertNull(substringBeforeLastIgnoreCase("Hello", "xyz"));
        assertNull(substringBeforeLastIgnoreCase(null, "test"));
        assertNull(substringBeforeLastIgnoreCase("test", null));
        assertEquals("test", substringBeforeLastIgnoreCase("test", ""));
    }

    @Test
    public void testSubstringBeforeAny_Chars() {
        assertEquals("He", substringBeforeAny("Hello World", 'x', 'l', 'z'));
        assertEquals("Hello ", substringBeforeAny("Hello World", 'W', ' '));
        assertNull(substringBeforeAny("Hello", 'x', 'y', 'z'));
        assertNull(substringBeforeAny(null, 'a', 'b'));
        assertNull(substringBeforeAny("test", CommonUtil.EMPTY_CHAR_ARRAY));
    }

    @Test
    public void testSubstringBeforeAny_Strings() {
        assertEquals("Hello", substringBeforeAny("Hello World", "xyz", " World", "abc"));
        assertEquals("", substringBeforeAny("Hello World", "abc", "Hello", "xyz"));
        assertNull(substringBeforeAny("Hello", "xyz", "abc"));
        assertNull(substringBeforeAny(null, "test", "abc"));
        assertNull(substringBeforeAny("test", CommonUtil.EMPTY_STRING_ARRAY));
    }

    @Test
    public void testSubstringBetween_Indexes() {
        assertEquals("ll", substringBetween("Hello", 1, 4));
        assertEquals(null, substringBetween("Hello", 2, 2));
        assertEquals(null, substringBetween("Hello", -2, 4));
        assertNull(substringBetween("Hello", 2, 1));
        assertNull(substringBetween("Hello", 5, 10));
        assertNull(substringBetween(null, 0, 5));
    }

    @Test
    public void testSubstringBetween_IndexAndCharDelimiter() {
        assertEquals("ll", substringBetween("Hello World", 1, 'o'));
        assertNull(substringBetween("Hello", 1, 'x'));
        assertEquals("Hell", substringBetween("Hello", -1, 'o'));
        assertNull(substringBetween("Hello", 5, 'o'));
        assertNull(substringBetween(null, 0, 'o'));
    }

    @Test
    public void testSubstringBetween_IndexAndStringDelimiter() {
        assertEquals("ello", substringBetween("Hello World", 0, " World"));
        assertNull(substringBetween("Hello", 0, "xyz"));
        assertEquals("Hel", substringBetween("Hello", -1, "lo"));
        assertNull(substringBetween("Hello", 5, "lo"));
        assertNull(substringBetween(null, 0, "lo"));
        assertNull(substringBetween("Hello", 0, (String) null));
    }

    @Test
    public void testSubstringBetween_CharDelimiterAndIndex() {
        assertEquals("llo", substringBetween("Hello World", 'e', 5));
        assertNull(substringBetween("Hello", 'x', 5));
        assertNull(substringBetween("Hello", 'H', 0));
        assertNull(substringBetween(null, 'e', 5));
    }

    @Test
    public void testSubstringBetween_StringDelimiterAndIndex() {
        assertEquals("llo", substringBetween("Hello World", "He", 5));
        assertNull(substringBetween("Hello", "xyz", 5));
        assertNull(substringBetween("Hello", "He", -1));
        assertNull(substringBetween(null, "He", 5));
        assertNull(substringBetween("Hello", (String) null, 5));
    }

    @Test
    public void testSubstringBetween_CharDelimiters() {
        assertEquals("b", substringBetween("abc", 'a', 'c'));
        assertEquals("a", substringBetween("aac", 'a', 'c'));
        assertNull(substringBetween("abc", 'x', 'c'));
        assertNull(substringBetween("abc", 'a', 'x'));
        assertNull(substringBetween(null, 'a', 'c'));
        assertNull(substringBetween("", 'a', 'c'));
    }

    @Test
    public void testSubstringBetween_StringDelimiters() {
        assertEquals("b", substringBetween("abc", "a", "c"));
        assertEquals("a", substringBetween("aac", "a", "c"));
        assertEquals("tagged", substringBetween("<tag>tagged</tag>", "<tag>", "</tag>"));
        assertEquals("abc", substringBetween("xabcx", "x"));
        assertNull(substringBetween("abc", "x", "c"));
        assertNull(substringBetween("abc", "a", "x"));
        assertNull(substringBetween(null, "a", "c"));
        assertNull(substringBetween("abc", (String) null, "c"));
        assertNull(substringBetween("abc", "a", (String) null));
    }

    @Test
    public void testSubstringBetween_StringDelimitersWithFromIndex() {
        assertEquals("", substringBetween("abcabcd", 2, "b", ""));
        assertEquals("", substringBetween("<tag1>text1</tag1><tag2>text2</tag2>", 10, ">", "<"));
        assertEquals("tag2", substringBetween("<tag1>text1</tag1><tag2>text2</tag2>", 10, "><", ">"));
        assertNull(substringBetween("abc", 5, "a", "c"));
        assertEquals("b", substringBetween("abc", -1, "a", "c"));
        assertNull(substringBetween(null, 0, "a", "c"));
    }

    @Test
    public void testSubstringBetweenIgnoreCase() {
        assertEquals("B", substringBetweenIgnoreCaes("aBc", "A", "C"));
        assertEquals("tagged", substringBetweenIgnoreCaes("<TAG>tagged</TAG>", "<tag>", "</tag>"));
        assertEquals("ABC", substringBetweenIgnoreCaes("xABCx", "X"));
        assertNull(substringBetweenIgnoreCaes("abc", "x", "c"));
        assertNull(substringBetweenIgnoreCaes(null, "a", "c"));
    }

    @Test
    public void testSubstringBetween_WithFunctions() {
        assertEquals("e", substringBetween("Hello", 0, i -> i + 2));
        assertNull(substringBetween("Hello", 0, i -> -1));
        assertEquals("Hello", substringBetween("Hello", -1, i -> 5));
        assertNull(substringBetween(null, 0, i -> 5));

        assertEquals("l", substringBetween("Hello", i -> i - 2, 4));
        assertEquals("Hell", substringBetween("Hello", i -> -1, 4));
        assertNull(substringBetween("Hello", i -> 5, 4));
        assertNull(substringBetween(null, i -> 0, 4));

        assertEquals("ello Wor", substringBetween("Hello World", "H", i -> i + 8));
        assertNull(substringBetween("Hello", "H", i -> -1));

        assertEquals("ello ", substringBetween("Hello World", i -> 0, "World"));
        assertEquals(null, substringBetween("Hello", i -> -1, "World"));
    }

    @Test
    public void testSubstringsBetween_Chars() {
        List<String> result = substringsBetween("3[a2[c]]2[a]", '[', ']');
        assertEquals(2, result.size());
        assertEquals("a2[c", result.get(0));
        assertEquals("a", result.get(1));

        assertTrue(substringsBetween("abc", '[', ']').isEmpty());
        assertTrue(substringsBetween(null, '[', ']').isEmpty());
        assertTrue(substringsBetween("", '[', ']').isEmpty());
    }

    @Test
    public void testSubstringsBetween_CharsWithRange() {
        assertEquals("a2[c", substringBetween("3[a2[c]]2[a]", '[', ']'));

        List<String> result = substringsBetween("3[a2[c]]2[a]", 0, 8, '[', ']');
        assertEquals(1, result.size());
        assertEquals("a2[c", result.get(0));

        assertTrue(substringsBetween("abc", 0, 3, '[', ']').isEmpty());
    }

    @Test
    public void testSubstringsBetween_Strings() {
        List<String> result = substringsBetween("<tag>text1</tag><tag>text2</tag>", "<tag>", "</tag>");
        assertEquals(2, result.size());
        assertEquals("text1", result.get(0));
        assertEquals("text2", result.get(1));

        assertTrue(substringsBetween("abc", "<", ">").isEmpty());
        assertTrue(substringsBetween(null, "<", ">").isEmpty());
        assertTrue(substringsBetween("test", null, ">").isEmpty());
        assertTrue(substringsBetween("test", "<", null).isEmpty());
    }

    @Test
    public void testSubstringsBetween_StringsWithRange() {
        List<String> result = substringsBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>");
        assertEquals(1, result.size());
        assertEquals("text1", result.get(0));

        assertTrue(substringsBetween("abc", 0, 3, "<", ">").isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_Chars() {
        List<int[]> indices = substringIndicesBetween("3[a2[c]]2[a]", '[', ']');
        assertEquals(2, indices.size());
        assertArrayEquals(new int[] { 2, 6 }, indices.get(0));
        assertArrayEquals(new int[] { 10, 11 }, indices.get(1));

        assertTrue(substringIndicesBetween("abc", '[', ']').isEmpty());
        assertTrue(substringIndicesBetween(null, '[', ']').isEmpty());
        assertTrue(substringIndicesBetween("", '[', ']').isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_CharsWithRange() {
        List<int[]> indices = substringIndicesBetween("3[a2[c]]2[a]", 0, 8, '[', ']');
        assertEquals(1, indices.size());
        assertArrayEquals(new int[] { 2, 6 }, indices.get(0));
    }

    @Test
    public void testSubstringIndicesBetween_Strings() {
        List<int[]> indices = substringIndicesBetween("<tag>text</tag>", "<tag>", "</tag>");
        assertEquals(1, indices.size());
        assertArrayEquals(new int[] { 5, 9 }, indices.get(0));

        assertTrue(substringIndicesBetween("abc", "<", ">").isEmpty());
        assertTrue(substringIndicesBetween(null, "<", ">").isEmpty());
        assertTrue(substringIndicesBetween("test", "", ">").isEmpty());
        assertTrue(substringIndicesBetween("test", "<", "").isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_StringsWithRange() {
        assertEquals("text1", substringsBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>").get(0));
        List<int[]> indices = substringIndicesBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>");
        assertEquals(1, indices.size());
        assertArrayEquals(new int[] { 5, 10 }, indices.get(0));

    }

    @Test
    public void testReplaceRange() {
        assertEquals("Heo World", replaceRange("Hello World", 2, 4, ""));
        assertEquals("He123llo World", replaceRange("Hello World", 2, 2, "123"));
        assertEquals("He123 World", replaceRange("Hello World", 2, 5, "123"));
        assertEquals("123", replaceRange("", 0, 0, "123"));
        assertEquals("", replaceRange("", 0, 0, null));

        try {
            replaceRange("Hello", -1, 3, "x");
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            replaceRange("Hello", 2, 1, "x");
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testMoveRange() {
        assertEquals("lo WoHelrld", moveRange("Hello World", 0, 3, 5));
        assertEquals("Hlo Worldel", moveRange("Hello World", 1, 3, 9));
        assertEquals("lo HelWorld", moveRange("Hello World", 3, 6, 0));
        assertEquals("", moveRange("", 0, 0, 0));
        assertEquals("Hello", moveRange("Hello", 0, 3, 0));
        assertEquals("Hello", moveRange("Hello", 2, 2, 3));

        try {
            moveRange("Hello", -1, 3, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            moveRange("Hello", 0, 3, 6);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testDeleteRange() {
        assertEquals("Ho World", deleteRange("Hello World", 1, 4));
        assertEquals("World", deleteRange("Hello World", 0, 6));
        assertEquals("Hello", deleteRange("Hello World", 5, 11));
        assertEquals("", deleteRange("Hello", 0, 5));
        assertEquals("Hello", deleteRange("Hello", 2, 2));

        assertEquals("", deleteRange("", 0, 0));

        try {
            deleteRange("Hello", 5, 10);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            deleteRange("Hello", -1, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            deleteRange("Hello", 3, 1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testJoin_BooleanArray() {
        assertEquals("true, false, true", join(new boolean[] { true, false, true }));
        assertEquals("true:false:true", join(new boolean[] { true, false, true }, ":"));
        assertEquals("true|false|true", join(new boolean[] { true, false, true }, "|"));
        assertEquals("", join(new boolean[] {}));
        assertEquals("", join((boolean[]) null));
        assertEquals("true", join(new boolean[] { true }));
    }

    @Test
    public void testJoin_BooleanArrayWithRange() {
        assertEquals("false,true", join(new boolean[] { true, false, true }, 1, 3, ","));
        assertEquals("false|true", join(new boolean[] { true, false, true }, 1, 3, "|"));
        assertEquals("", join(new boolean[] { true, false, true }, 1, 1, ","));

        try {
            join(new boolean[] { true, false }, 0, 3, ",");
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testJoin_BooleanArrayWithPrefixSuffix() {
        assertEquals("[true, false]", join(new boolean[] { true, false }, 0, 2, ", ", "[", "]"));
        assertEquals("true", join(new boolean[] { true }, 0, 1, ", ", "", ""));
        assertEquals("[]", join(new boolean[] {}, 0, 0, ", ", "[", "]"));
        assertEquals("prefix", join(new boolean[] {}, 0, 0, ", ", "prefix", null));
        assertEquals("suffix", join(new boolean[] {}, 0, 0, ", ", null, "suffix"));
    }

    @Test
    public void testJoin_CharArray() {
        assertEquals("a, b, c", join(new char[] { 'a', 'b', 'c' }));
        assertEquals("a:b:c", join(new char[] { 'a', 'b', 'c' }, ":"));
        assertEquals("abc", join(new char[] { 'a', 'b', 'c' }, ""));
        assertEquals("", join(new char[] {}));
        assertEquals("", join((char[]) null));
        assertEquals("x", join(new char[] { 'x' }));
    }

    @Test
    public void testJoin_CharArrayWithRange() {
        assertEquals("b,c", join(new char[] { 'a', 'b', 'c' }, 1, 3, ","));
        assertEquals("b|c", join(new char[] { 'a', 'b', 'c' }, 1, 3, "|"));
        assertEquals("", join(new char[] { 'a', 'b', 'c' }, 1, 1, ","));
    }

    @Test
    public void testJoin_CharArrayWithPrefixSuffix() {
        assertEquals("[a, b]", join(new char[] { 'a', 'b' }, 0, 2, ", ", "[", "]"));
        assertEquals("<a-b-c>", join(new char[] { 'a', 'b', 'c' }, 0, 3, "-", "<", ">"));
    }

    @Test
    public void testJoin_ByteArray() {
        assertEquals("1, 2, 3", join(new byte[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new byte[] { 1, 2, 3 }, ":"));
        assertEquals("123", join(new byte[] { 1, 2, 3 }, ""));
        assertEquals("", join(new byte[] {}));
        assertEquals("", join((byte[]) null));
    }

    @Test
    public void testJoin_ByteArrayWithRange() {
        assertEquals("2,3", join(new byte[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("2|3", join(new byte[] { 1, 2, 3 }, 1, 3, "|"));
        assertEquals("", join(new byte[] { 1, 2, 3 }, 1, 1, ","));
    }

    @Test
    public void testJoin_ByteArrayWithPrefixSuffix() {
        assertEquals("[1, 2]", join(new byte[] { 1, 2 }, 0, 2, ", ", "[", "]"));
        assertEquals("{1-2-3}", join(new byte[] { 1, 2, 3 }, 0, 3, "-", "{", "}"));
    }

    @Test
    public void testJoin_ShortArray() {
        assertEquals("1, 2, 3", join(new short[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new short[] { 1, 2, 3 }, ":"));
        assertEquals("123", join(new short[] { 1, 2, 3 }, ""));
        assertEquals("", join(new short[] {}));
        assertEquals("", join((short[]) null));
    }

    @Test
    public void testJoin_ShortArrayWithRange() {
        assertEquals("2,3", join(new short[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("2|3", join(new short[] { 1, 2, 3 }, 1, 3, "|"));
    }

    @Test
    public void testJoin_ShortArrayWithPrefixSuffix() {
        assertEquals("[1, 2]", join(new short[] { 1, 2 }, 0, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_IntArray() {
        assertEquals("1, 2, 3", join(new int[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new int[] { 1, 2, 3 }, ":"));
        assertEquals("123", join(new int[] { 1, 2, 3 }, ""));
        assertEquals("", join(new int[] {}));
        assertEquals("", join((int[]) null));
    }

    @Test
    public void testJoin_IntArrayWithRange() {
        assertEquals("2,3", join(new int[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("2|3", join(new int[] { 1, 2, 3 }, 1, 3, "|"));
    }

    @Test
    public void testJoin_IntArrayWithPrefixSuffix() {
        assertEquals("[1, 2]", join(new int[] { 1, 2 }, 0, 2, ", ", "[", "]"));
        assertEquals("START:100:200:END", join(new int[] { 100, 200 }, 0, 2, ":", "START:", ":END"));
    }

    @Test
    public void testJoin_LongArray() {
        assertEquals("1, 2, 3", join(new long[] { 1L, 2L, 3L }));
        assertEquals("1:2:3", join(new long[] { 1L, 2L, 3L }, ":"));
        assertEquals("123", join(new long[] { 1L, 2L, 3L }, ""));
        assertEquals("", join(new long[] {}));
        assertEquals("", join((long[]) null));
    }

    @Test
    public void testJoin_LongArrayWithRange() {
        assertEquals("2,3", join(new long[] { 1L, 2L, 3L }, 1, 3, ","));
        assertEquals("2|3", join(new long[] { 1L, 2L, 3L }, 1, 3, "|"));
    }

    @Test
    public void testJoin_LongArrayWithPrefixSuffix() {
        assertEquals("[1, 2]", join(new long[] { 1L, 2L }, 0, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_FloatArray() {
        assertEquals("1.0, 2.0, 3.0", join(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals("1.0:2.0:3.0", join(new float[] { 1.0f, 2.0f, 3.0f }, ":"));
        assertEquals("1.02.03.0", join(new float[] { 1.0f, 2.0f, 3.0f }, ""));
        assertEquals("", join(new float[] {}));
        assertEquals("", join((float[]) null));
    }

    @Test
    public void testJoin_FloatArrayWithRange() {
        assertEquals("2.0,3.0", join(new float[] { 1.0f, 2.0f, 3.0f }, 1, 3, ","));
        assertEquals("2.0|3.0", join(new float[] { 1.0f, 2.0f, 3.0f }, 1, 3, "|"));
    }

    @Test
    public void testJoin_FloatArrayWithPrefixSuffix() {
        assertEquals("[1.0, 2.0]", join(new float[] { 1.0f, 2.0f }, 0, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_DoubleArray() {
        assertEquals("1.0, 2.0, 3.0", join(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals("1.0:2.0:3.0", join(new double[] { 1.0, 2.0, 3.0 }, ":"));
        assertEquals("1.02.03.0", join(new double[] { 1.0, 2.0, 3.0 }, ""));
        assertEquals("", join(new double[] {}));
        assertEquals("", join((double[]) null));
    }

    @Test
    public void testJoin_DoubleArrayWithRange() {
        assertEquals("2.0,3.0", join(new double[] { 1.0, 2.0, 3.0 }, 1, 3, ","));
        assertEquals("2.0|3.0", join(new double[] { 1.0, 2.0, 3.0 }, 1, 3, "|"));
    }

    @Test
    public void testJoin_DoubleArrayWithPrefixSuffix() {
        assertEquals("[1.0, 2.0]", join(new double[] { 1.0, 2.0 }, 0, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_ObjectArray() {
        assertEquals("a, b, c", join(new Object[] { "a", "b", "c" }));
        assertEquals("a:b:c", join(new Object[] { "a", "b", "c" }, ":"));
        assertEquals("abc", join(new Object[] { "a", "b", "c" }, ""));
        assertEquals("1, null, 3", join(new Object[] { 1, null, 3 }));
        assertEquals("", join(new Object[] {}));
        assertEquals("", join((Object[]) null));
    }

    @Test
    public void testJoin_ObjectArrayWithRange() {
        assertEquals("b,c", join(new Object[] { "a", "b", "c" }, 1, 3, ","));
        assertEquals("b|c", join(new Object[] { "a", "b", "c" }, 1, 3, "|"));
        assertEquals("", join(new Object[] { "a", "b", "c" }, 1, 1, ","));
    }

    @Test
    public void testJoin_ObjectArrayWithPrefixSuffix() {
        assertEquals("[a, b]", join(new Object[] { "a", "b" }, 0, 2, ", ", "[", "]"));
        assertEquals("{1-null-3}", join(new Object[] { 1, null, 3 }, 0, 3, "-", "{", "}"));
    }

    @Test
    public void testJoin_ObjectArrayWithTrim() {
        assertEquals("a, b, c", join(new Object[] { " a ", " b ", " c " }, ", ", "", "", true));
        assertEquals("b:c", join(new Object[] { " a ", " b ", " c " }, 1, 3, ":", true));
        assertEquals("b|c", join(new Object[] { " a ", " b ", " c " }, 1, 3, "|", true));
        assertEquals("[a, b]", join(new Object[] { " a ", " b " }, 0, 2, ", ", "[", "]", true));
    }

    @Test
    public void testJoin_Iterable() {
        assertEquals("a, b, c", join(Arrays.asList("a", "b", "c")));
        assertEquals("a:b:c", join(Arrays.asList("a", "b", "c"), ":"));
        assertEquals("abc", join(Arrays.asList("a", "b", "c"), ""));
        assertEquals("", join(Collections.emptyList()));
        assertEquals("", join((Iterable<?>) null));
    }

    @Test
    public void testJoin_IterableWithPrefixSuffix() {
        assertEquals("[a, b]", join(Arrays.asList("a", "b"), ", ", "[", "]"));
        assertEquals("{1-2-3}", join(Arrays.asList(1, 2, 3), "-", "{", "}"));
    }

    @Test
    public void testJoin_IterableWithTrim() {
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c "), ", ", "", "", true));
        assertEquals("[a, b]", join(Arrays.asList(" a ", " b "), ", ", "[", "]", true));
    }

    @Test
    public void testJoin_CollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b,c", join(list, 1, 3, ","));
        assertEquals("b|c", join(list, 1, 3, "|"));
        assertEquals("b:c", join(list, 1, 3, ":"));
        assertEquals("", join(list, 2, 2, ","));

        try {
            join(list, 0, 5, ",");
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testJoin_CollectionWithPrefixSuffix() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("[a, b, c]", join(list, 0, 3, ", ", "[", "]", false));
        assertEquals("START:b:c:END", join(list, 1, 3, ":", "START:", ":END", false));
    }

    @Test
    public void testJoin_CollectionWithTrim() {
        List<String> list = Arrays.asList(" a ", " b ", " c ");
        assertEquals("a, b, c", join(list, 0, 3, ", ", true));
        assertEquals("a:b:c", join(list, 0, 3, ":", true));
        assertEquals("[a, b]", join(list, 0, 2, ", ", "[", "]", true));
    }

    @Test
    public void testJoin_Iterator() {
        assertEquals("a, b, c", join(Arrays.asList("a", "b", "c").iterator()));
        assertEquals("a:b:c", join(Arrays.asList("a", "b", "c").iterator(), ":"));
        assertEquals("abc", join(Arrays.asList("a", "b", "c").iterator(), ""));
        assertEquals("", join(Collections.emptyList().iterator()));
        assertEquals("", join((Iterator<?>) null));
    }

    @Test
    public void testJoin_IteratorWithPrefixSuffix() {
        assertEquals("[a, b]", join(Arrays.asList("a", "b").iterator(), ", ", "[", "]"));
        assertEquals("{1-2-3}", join(Arrays.asList(1, 2, 3).iterator(), "-", "{", "}"));
    }

    @Test
    public void testJoin_IteratorWithTrim() {
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c ").iterator(), ", ", "", "", true));
        assertEquals("[a, b]", join(Arrays.asList(" a ", " b ").iterator(), ", ", "[", "]", true));
    }

    @Test
    public void testJoinEntries_Map() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a=1, b=2", joinEntries(map));
        assertEquals("a=1;b=2", joinEntries(map, ";"));
        assertEquals("a=1|b=2", joinEntries(map, "|"));
        assertEquals("", joinEntries(Collections.emptyMap()));
        assertEquals("", joinEntries((Map<?, ?>) null));
    }

    @Test
    public void testJoinEntries_MapWithDelimiters() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a:1;b:2", joinEntries(map, ";", ":"));
        assertEquals("a:1|b:2", joinEntries(map, "|", ":"));
        assertEquals("a->1, b->2", joinEntries(map, ", ", "->"));
    }

    @Test
    public void testJoinEntries_MapWithPrefixSuffix() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("{a=1, b=2}", joinEntries(map, ", ", "=", "{", "}"));
        assertEquals("[a:1|b:2]", joinEntries(map, "|", ":", "[", "]"));
    }

    @Test
    public void testJoinEntries_MapWithTrim() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(" a ", " 1 ");
        map.put(" b ", " 2 ");
        assertEquals("a=1, b=2", joinEntries(map, ", ", "=", "", "", true));
        assertEquals("{a:1|b:2}", joinEntries(map, "|", ":", "{", "}", true));
    }

    @Test
    public void testJoinEntries_MapWithExtractors() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("key1", 100);
        map.put("key2", 200);

        Function<Map.Entry<String, Integer>, String> keyExtractor = e -> e.getKey().toUpperCase();
        Function<Map.Entry<String, Integer>, String> valueExtractor = e -> String.valueOf(e.getValue() * 2);

        assertEquals("KEY1=200, KEY2=400", joinEntries(map.entrySet(), ", ", "=", "", "", false, keyExtractor, valueExtractor));
    }

    @Test
    public void testJoinEntries_MapWithRange() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("b=2", joinEntries(map, 1, 2, ","));
        assertEquals("b:2", joinEntries(map, 1, 2, ",", ":"));
        assertEquals("b=2, c=3", joinEntries(map, 1, 3, ", "));
        assertEquals("[b:2|c:3]", joinEntries(map, 1, 3, "|", ":", "[", "]", false));
        assertEquals("", joinEntries(map, 1, 1, ","));

        try {
            joinEntries(map, 0, 4, ",");
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testConcat_TwoStrings() {
        assertEquals("HelloWorld", concat("Hello", "World"));
        assertEquals("Hello", concat("Hello", ""));
        assertEquals("World", concat("", "World"));
        assertEquals("", concat("", ""));
        assertEquals("Hello", concat("Hello", null));
        assertEquals("World", concat(null, "World"));
        assertEquals("", concat(null, null));
    }

    @Test
    public void testConcat_ThreeStrings() {
        assertEquals("HelloWorldTest", concat("Hello", "World", "Test"));
        assertEquals("HelloWorld", concat("Hello", "World", ""));
        assertEquals("HelloTest", concat("Hello", "", "Test"));
        assertEquals("Hello", concat("Hello", null, null));
        assertEquals("", concat(null, null, null));
    }

    @Test
    public void testConcat_MultipleStrings() {
        assertEquals("abcde", concat("a", "b", "c", "d", "e"));
        assertEquals("abcdefghi", concat("a", "b", "c", "d", "e", "f", "g", "h", "i"));
        assertEquals("abc", concat("a", null, "b", null, "c"));
        assertEquals("", concat(new String[] {}));
        assertEquals("", concat((String[]) null));
    }

    @Test
    public void testConcat_Objects() {
        assertEquals("12", concat(1, 2));
        assertEquals("123", concat(1, 2, 3));
        assertEquals("1null3", concat(1, null, 3));
        assertEquals("true42", concat(true, 42));
        assertEquals("", concat(null, null));
    }

    @Test
    public void testLenientFormat() {
        assertEquals("Hello World", lenientFormat("Hello %s", "World"));
        assertEquals("Hello World, Test", lenientFormat("Hello %s, %s", "World", "Test"));
        assertEquals("Hello World: [Extra, Args]", lenientFormat("Hello %s", "World", "Extra", "Args"));
        assertEquals("No placeholders: [arg1, arg2]", lenientFormat("No placeholders", "arg1", "arg2"));
        assertEquals("null", lenientFormat(null));
        assertEquals("Hello (Object[])null", lenientFormat("Hello %s", (Object[]) null));
    }

    @Test
    public void testReverse() {
        assertEquals("dlroW olleH", reverse("Hello World"));
        assertEquals("a", reverse("a"));
        assertEquals("", reverse(""));
        assertNull(reverse(null));
        assertEquals("12345", reverse("54321"));
    }

    @Test
    public void testReverseDelimited_Char() {
        assertEquals("c.b.a", reverseDelimited("a.b.c", '.'));
        assertEquals("a.b.c", reverseDelimited("a.b.c", 'x'));
        assertEquals("", reverseDelimited("", '.'));
        assertNull(reverseDelimited(null, '.'));
        assertEquals("a", reverseDelimited("a", '.'));
    }

    @Test
    public void testReverseDelimited_String() {
        assertEquals("c.b.a", reverseDelimited("a.b.c", "."));
        assertEquals("789->456->123", reverseDelimited("123->456->789", "->"));
        assertEquals("a.b.c", reverseDelimited("a.b.c", "xyz"));
        assertEquals("", reverseDelimited("", "."));
        assertNull(reverseDelimited(null, "."));
    }

    @Test
    public void testSort() {
        assertEquals("abcde", sort("edcba"));
        assertEquals("  Hello", sort("Hello  "));
        assertEquals("123abc", sort("abc123"));
        assertEquals("", sort(""));
        assertNull(sort(null));
        assertEquals("a", sort("a"));
    }

    @Test
    public void testRotate() {
        assertEquals("fgabcde", rotate("abcdefg", 2));
        assertEquals("cdefgab", rotate("abcdefg", -2));
        assertEquals("abcdefg", rotate("abcdefg", 0));
        assertEquals("abcdefg", rotate("abcdefg", 7));
        assertEquals("abcdefg", rotate("abcdefg", -7));
        assertEquals("fgabcde", rotate("abcdefg", 9));
        assertEquals("cdefgab", rotate("abcdefg", -9));
        assertEquals("", rotate("", 5));
        assertNull(rotate(null, 5));
        assertEquals("a", rotate("a", 5));
    }

    @Test
    public void testShuffle() {
        String original = "abcdefghijk";
        String shuffled = shuffle(original);
        assertEquals(original.length(), shuffled.length());

        for (char c : original.toCharArray()) {
            assertTrue(shuffled.indexOf(c) >= 0);
        }

        assertEquals("", shuffle(""));
        assertNull(shuffle(null));
        assertEquals("a", shuffle("a"));
    }

    @Test
    public void testShuffle_WithRandom() {
        Random rnd = new Random(12345);
        String original = "abcdefghijk";
        String shuffled = shuffle(original, rnd);
        assertEquals(original.length(), shuffled.length());

        for (char c : original.toCharArray()) {
            assertTrue(shuffled.indexOf(c) >= 0);
        }
    }

    @Test
    public void testParseBoolean() {
        assertTrue(parseBoolean("true"));
        assertTrue(parseBoolean("TRUE"));
        assertTrue(parseBoolean("True"));
        assertFalse(parseBoolean("false"));
        assertFalse(parseBoolean("FALSE"));
        assertFalse(parseBoolean("xyz"));
        assertFalse(parseBoolean(""));
        assertFalse(parseBoolean(null));
    }

    @Test
    public void testParseChar() {
        assertEquals('a', parseChar("a"));
        assertEquals('Z', parseChar("Z"));
        assertEquals('5', parseChar("5"));
        assertEquals((char) 65, parseChar("65"));
        assertEquals((char) 0, parseChar(""));
        assertEquals((char) 0, parseChar(null));
    }

    @Test
    public void testBase64Encode() {
        assertEquals("SGVsbG8gV29ybGQ=", base64Encode("Hello World".getBytes()));
        assertEquals("", base64Encode(new byte[] {}));
        assertEquals("", base64Encode(null));
    }

    @Test
    public void testBase64EncodeString() {
        assertEquals("SGVsbG8gV29ybGQ=", base64EncodeString("Hello World"));
        assertEquals("", base64EncodeString(""));
        assertEquals("", base64EncodeString(null));
    }

    @Test
    public void testBase64EncodeUtf8String() {
        assertEquals("SGVsbG8gV29ybGQ=", base64EncodeUtf8String("Hello World"));
        assertEquals("", base64EncodeUtf8String(""));
        assertEquals("", base64EncodeUtf8String(null));
    }

    @Test
    public void testBase64EncodeString_WithCharset() {
        assertEquals("SGVsbG8gV29ybGQ=", base64EncodeString("Hello World", StandardCharsets.UTF_8));
        assertEquals("", base64EncodeString("", StandardCharsets.UTF_8));
        assertEquals("", base64EncodeString(null, StandardCharsets.UTF_8));
    }

    @Test
    public void testBase64Decode() {
        assertArrayEquals("Hello World".getBytes(), base64Decode("SGVsbG8gV29ybGQ="));
        assertArrayEquals(new byte[] {}, base64Decode(""));
        assertArrayEquals(new byte[] {}, base64Decode(null));
    }

    @Test
    public void testBase64DecodeToString() {
        assertEquals("Hello World", base64DecodeToString("SGVsbG8gV29ybGQ="));
        assertEquals("", base64DecodeToString(""));
        assertEquals("", base64DecodeToString(null));
    }

    @Test
    public void testBase64DecodeToUtf8String() {
        assertEquals("Hello World", base64DecodeToUtf8String("SGVsbG8gV29ybGQ="));
        assertEquals("", base64DecodeToUtf8String(""));
        assertEquals("", base64DecodeToUtf8String(null));
    }

    @Test
    public void testBase64DecodeToString_WithCharset() {
        assertEquals("Hello World", base64DecodeToString("SGVsbG8gV29ybGQ=", StandardCharsets.UTF_8));
        assertEquals("", base64DecodeToString("", StandardCharsets.UTF_8));
        assertEquals("", base64DecodeToString(null, StandardCharsets.UTF_8));
    }

    @Test
    public void testBase64UrlEncode() {
        byte[] data = "Hello World!".getBytes();
        String encoded = base64UrlEncode(data);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertFalse(encoded.contains("="));
        assertEquals("", base64UrlEncode(new byte[] {}));
        assertEquals("", base64UrlEncode(null));
    }

    @Test
    public void testBase64UrlDecode() {
        String encoded = base64UrlEncode("Hello World!".getBytes());
        assertArrayEquals("Hello World!".getBytes(), base64UrlDecode(encoded));
        assertArrayEquals(new byte[] {}, base64UrlDecode(""));
        assertArrayEquals(new byte[] {}, base64UrlDecode(null));
    }

    @Test
    public void testBase64UrlDecodeToString() {
        String encoded = base64UrlEncode("Hello World!".getBytes());
        assertEquals("Hello World!", base64UrlDecodeToString(encoded));
        assertEquals("", base64UrlDecodeToString(""));
        assertEquals("", base64UrlDecodeToString(null));
    }

    @Test
    public void testBase64UrlDecodeToUtf8String() {
        String encoded = base64UrlEncode("Hello World!".getBytes(StandardCharsets.UTF_8));
        assertEquals("Hello World!", base64UrlDecodeToUtf8String(encoded));
        assertEquals("", base64UrlDecodeToUtf8String(""));
        assertEquals("", base64UrlDecodeToUtf8String(null));
    }

    @Test
    public void testBase64UrlDecodeToString_WithCharset() {
        String encoded = base64UrlEncode("Hello World!".getBytes(StandardCharsets.UTF_8));
        assertEquals("Hello World!", base64UrlDecodeToString(encoded, StandardCharsets.UTF_8));
        assertEquals("", base64UrlDecodeToString("", StandardCharsets.UTF_8));
        assertEquals("", base64UrlDecodeToString(null, StandardCharsets.UTF_8));
    }

    @Test
    public void testIsBase64_Byte() {
        assertTrue(isBase64((byte) 'A'));
        assertTrue(isBase64((byte) 'a'));
        assertTrue(isBase64((byte) '0'));
        assertTrue(isBase64((byte) '+'));
        assertTrue(isBase64((byte) '/'));
        assertTrue(isBase64((byte) '='));
        assertFalse(isBase64((byte) '#'));
        assertFalse(isBase64((byte) ' '));
    }

    @Test
    public void testIsBase64_ByteArray() {
        assertTrue(isBase64("SGVsbG8gV29ybGQ=".getBytes()));
        assertTrue(isBase64("".getBytes()));
        assertFalse(isBase64("Hello#World".getBytes()));
        assertTrue(isBase64("SGVsbG8 V29ybGQ=".getBytes()));
    }

    @Test
    public void testIsBase64_String() {
        assertTrue(isBase64("SGVsbG8gV29ybGQ="));
        assertTrue(isBase64(""));
        assertFalse(isBase64("Hello#World"));
        assertTrue(isBase64("SGVsbG8 V29ybGQ="));
    }

    @Test
    public void testFindFirstEmailAddress() {
        assertEquals("test@example.com", findFirstEmailAddress("Contact us at test@example.com"));
        assertEquals("user@domain.co.uk", findFirstEmailAddress("Email: user@domain.co.uk for info"));
        assertNull(findFirstEmailAddress("No email here"));
        assertNull(findFirstEmailAddress(""));
        assertNull(findFirstEmailAddress(null));
    }

    @Test
    public void testFindAllEmailAddresses() {
        List<String> emails = findAllEmailAddresses("Contact: test@example.com or admin@site.org");
        assertEquals(2, emails.size());
        assertEquals("test@example.com", emails.get(0));
        assertEquals("admin@site.org", emails.get(1));

        assertTrue(findAllEmailAddresses("No emails here").isEmpty());
        assertTrue(findAllEmailAddresses("").isEmpty());
        assertTrue(findAllEmailAddresses(null).isEmpty());
    }

    @Test
    public void testCopyThenTrim() {
        String[] input = { " a ", " b ", " c " };
        String[] result = copyThenTrim(input);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
        assertNotSame(input, result);

        assertArrayEquals(new String[] {}, copyThenTrim(new String[] {}));
        assertNull(copyThenTrim(null));
    }

    @Test
    public void testCopyThenStrip() {
        String[] input = { " a ", " b ", " c " };
        String[] result = copyThenStrip(input);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
        assertNotSame(input, result);

        assertArrayEquals(new String[] {}, copyThenStrip(new String[] {}));
        assertNull(copyThenStrip(null));
    }

    @Test
    public void testExtractFirstInteger() {
        assertEquals("123", extractFirstInteger("abc123def"));
        assertEquals("-456", extractFirstInteger("test-456test"));
        assertEquals("789", extractFirstInteger("789"));
        assertNull(extractFirstInteger("no numbers"));
        assertNull(extractFirstInteger(""));
        assertNull(extractFirstInteger(null));
    }

    @Test
    public void testExtractFirstDouble() {
        assertEquals("123.45", extractFirstDouble("abc123.45def"));
        assertEquals("-456.78", extractFirstDouble("test-456.78test"));
        assertEquals("789", extractFirstDouble("789"));
        assertEquals("3.14", extractFirstDouble("pi=3.14"));
        assertNull(extractFirstDouble("no numbers"));
        assertNull(extractFirstDouble(""));
        assertNull(extractFirstDouble(null));
    }

    @Test
    public void testExtractFirstDouble_WithScientific() {
        assertEquals("1.23e4", extractFirstDouble("value=1.23e4", true));
        assertEquals("1.23E-4", extractFirstDouble("small=1.23E-4", true));
        assertEquals("123.45", extractFirstDouble("abc123.45def", true));
        assertNull(extractFirstDouble("no numbers", true));
        assertNull(extractFirstDouble("", true));
        assertNull(extractFirstDouble(null, true));
    }

    @Test
    public void testReplaceFirstInteger() {
        assertEquals("abcXXXdef", replaceFirstInteger("abc123def", "XXX"));
        assertEquals("test[NUM]test", replaceFirstInteger("test-456test", "[NUM]"));
        assertEquals("[NUM]", replaceFirstInteger("789", "[NUM]"));
        assertEquals("no numbers", replaceFirstInteger("no numbers", "XXX"));
        assertEquals("", replaceFirstInteger("", "XXX"));
        assertEquals("", replaceFirstInteger(null, "XXX"));
    }

    @Test
    public void testReplaceFirstDouble() {
        assertEquals("abcXXXdef", replaceFirstDouble("abc123.45def", "XXX"));
        assertEquals("test[NUM]test", replaceFirstDouble("test-456.78test", "[NUM]"));
        assertEquals("[NUM]", replaceFirstDouble("789", "[NUM]"));
        assertEquals("pi=[NUM]", replaceFirstDouble("pi=3.14", "[NUM]"));
        assertEquals("no numbers", replaceFirstDouble("no numbers", "XXX"));
        assertEquals("", replaceFirstDouble("", "XXX"));
        assertEquals("", replaceFirstDouble(null, "XXX"));
    }

    @Test
    public void testReplaceFirstDouble_WithScientific() {
        assertEquals("value=[NUM]", replaceFirstDouble("value=1.23e4", "[NUM]", true));
        assertEquals("small=[NUM]", replaceFirstDouble("small=1.23E-4", "[NUM]", true));
        assertEquals("abc[NUM]def", replaceFirstDouble("abc123.45def", "[NUM]", true));
        assertEquals("no numbers", replaceFirstDouble("no numbers", "XXX", true));
        assertEquals("", replaceFirstDouble("", "XXX", true));
        assertEquals("", replaceFirstDouble(null, "XXX", true));
    }

    @Test
    public void testStrUtil_Substring() {
        assertEquals("World", StrUtil.substring("Hello World", 6).orElse(null));
        assertFalse(StrUtil.substring("Hello", -1).isPresent());
        assertFalse(StrUtil.substring(null, 0).isPresent());

        assertEquals("Hello", StrUtil.substring("Hello World", 0, 5).orElse(null));
        assertFalse(StrUtil.substring("Hello", -1, 5).isPresent());

        assertEquals("Hel", StrUtil.substring("Hello", 0, i -> i + 3).orElse(null));
        assertFalse(StrUtil.substring("Hello", 0, i -> -1).isPresent());

        assertEquals("lo", StrUtil.substring("Hello", i -> i - 2, 5).orElse(null));
        assertFalse(StrUtil.substring("Hello", i -> -1, 5).isPresent());
    }

    @Test
    public void testStrUtil_SubstringOrElse() {
        assertEquals("World", StrUtil.substringOrElse("Hello World", 6, "default"));
        assertEquals("default", StrUtil.substringOrElse("Hello", -1, "default"));
        assertEquals("default", StrUtil.substringOrElse(null, 0, "default"));

        assertEquals("Hello", StrUtil.substringOrElse("Hello World", 0, 5, "default"));
        assertEquals("default", StrUtil.substringOrElse("Hello", -1, 5, "default"));

        assertEquals("Hel", StrUtil.substringOrElse("Hello", 0, i -> i + 3, "default"));
        assertEquals("default", StrUtil.substringOrElse("Hello", 0, i -> -1, "default"));

        assertEquals("lo", StrUtil.substringOrElse("Hello", i -> i - 2, 5, "default"));
        assertEquals("default", StrUtil.substringOrElse("Hello", i -> -1, 5, "default"));
    }

    @Test
    public void testStrUtil_SubstringOrElseItself() {
        assertEquals("World", StrUtil.substringOrElseItself("Hello World", 6));
        assertEquals("Hello", StrUtil.substringOrElseItself("Hello", -1));
        assertEquals(null, StrUtil.substringOrElseItself(null, 0));

        assertEquals("Hello", StrUtil.substringOrElseItself("Hello World", 0, 5));
        assertEquals("Hello", StrUtil.substringOrElseItself("Hello", -1, 5));

        assertEquals("Hel", StrUtil.substringOrElseItself("Hello", 0, i -> i + 3));
        assertEquals("Hello", StrUtil.substringOrElseItself("Hello", 0, i -> -1));

        assertEquals("lo", StrUtil.substringOrElseItself("Hello", i -> i - 2, 5));
        assertEquals("Hello", StrUtil.substringOrElseItself("Hello", i -> -1, 5));
    }

    @Test
    public void testStrUtil_SubstringAfter() {
        assertEquals("llo World", StrUtil.substringAfter("Hello World", 'e').orElse(null));
        assertFalse(StrUtil.substringAfter("Hello", 'x').isPresent());
        assertFalse(StrUtil.substringAfter(null, 'a').isPresent());

        assertEquals("World", StrUtil.substringAfter("Hello World", "Hello ").orElse(null));
        assertFalse(StrUtil.substringAfter("Hello", "xyz").isPresent());

        assertEquals("Wo", StrUtil.substringAfter("Hello World", "Hello ", 8).orElse(null));
        assertFalse(StrUtil.substringAfter("Hello World", "World", 8).isPresent());
    }

    @Test
    public void testStrUtil_SubstringAfterLast() {
        assertEquals("txt", StrUtil.substringAfterLast("file.name.txt", '.').orElse(null));
        assertFalse(StrUtil.substringAfterLast("Hello", 'x').isPresent());

        assertEquals("txt", StrUtil.substringAfterLast("file.name.txt", ".").orElse(null));
        assertFalse(StrUtil.substringAfterLast("Hello", "xyz").isPresent());

        assertEquals("name", StrUtil.substringAfterLast("file.name.txt", ".", 9).orElse(null));
        assertFalse(StrUtil.substringAfterLast("file.name.txt", ".", 3).isPresent());
    }

    @Test
    public void testStrUtil_SubstringAfterAny() {
        assertEquals("llo World", StrUtil.substringAfterAny("Hello World", 'x', 'e', 'z').orElse(null));
        assertFalse(StrUtil.substringAfterAny("Hello", 'x', 'y', 'z').isPresent());

        assertEquals("World", StrUtil.substringAfterAny("Hello World", "xyz", "Hello ", "abc").orElse(null));
        assertFalse(StrUtil.substringAfterAny("Hello", "xyz", "abc").isPresent());
    }

    @Test
    public void testStrUtil_SubstringBefore() {
        assertEquals("He", StrUtil.substringBefore("Hello World", 'l').orElse(null));
        assertFalse(StrUtil.substringBefore("Hello", 'x').isPresent());

        assertEquals("Hello", StrUtil.substringBefore("Hello World", " World").orElse(null));
        assertFalse(StrUtil.substringBefore("Hello", "xyz").isPresent());

        assertEquals("lo", StrUtil.substringBefore("Hello World", 3, " World").orElse(null));
        assertFalse(StrUtil.substringBefore("Hello World", 7, " ").isPresent());
    }

    @Test
    public void testStrUtil_SubstringBeforeLast() {
        assertEquals("file.name", StrUtil.substringBeforeLast("file.name.txt", '.').orElse(null));
        assertFalse(StrUtil.substringBeforeLast("Hello", 'x').isPresent());

        assertEquals("file.name", StrUtil.substringBeforeLast("file.name.txt", ".").orElse(null));
        assertFalse(StrUtil.substringBeforeLast("Hello", "xyz").isPresent());

        assertEquals("le.name", StrUtil.substringBeforeLast("file.name.txt", 2, ".").orElse(null));
        assertFalse(StrUtil.substringBeforeLast("file.name.txt", 10, ".").isPresent());
    }

    @Test
    public void testStrUtil_SubstringBeforeAny() {
        assertEquals("He", StrUtil.substringBeforeAny("Hello World", 'x', 'l', 'z').orElse(null));
        assertFalse(StrUtil.substringBeforeAny("Hello", 'x', 'y', 'z').isPresent());

        assertEquals("Hello", StrUtil.substringBeforeAny("Hello World", "xyz", " World", "abc").orElse(null));
        assertFalse(StrUtil.substringBeforeAny("Hello", "xyz", "abc").isPresent());
    }

    @Test
    public void testStrUtil_SubstringOrElseMethods() {
        assertEquals("World", StrUtil.substringAfterOrElse("Hello World", "Hello ", "default"));
        assertEquals("default", StrUtil.substringAfterOrElse("Hello", "xyz", "default"));

        assertEquals("txt", StrUtil.substringAfterLastOrElse("file.name.txt", ".", "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse("Hello", "xyz", "default"));

        assertEquals("Hello", StrUtil.substringBeforeOrElse("Hello World", " World", "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse("Hello", "xyz", "default"));

        assertEquals("file.name", StrUtil.substringBeforeLastOrElse("file.name.txt", ".", "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse("Hello", "xyz", "default"));
    }

    @Test
    public void testStrUtil_SubstringOrElseItselfMethods() {
        assertEquals("llo World", StrUtil.substringAfterOrElseItself("Hello World", 'e'));
        assertEquals("Hello", StrUtil.substringAfterOrElseItself("Hello", 'x'));

        assertEquals("World", StrUtil.substringAfterOrElseItself("Hello World", "Hello "));
        assertEquals("Hello", StrUtil.substringAfterOrElseItself("Hello", "xyz"));

        assertEquals("Wo", StrUtil.substringAfterOrElseItself("Hello World", "Hello ", 8));
        assertEquals("Hello World", StrUtil.substringAfterOrElseItself("Hello World", "World", 8));

        assertEquals("txt", StrUtil.substringAfterLastOrElseItself("file.name.txt", '.'));
        assertEquals("Hello", StrUtil.substringAfterLastOrElseItself("Hello", 'x'));

        assertEquals("txt", StrUtil.substringAfterLastOrElseItself("file.name.txt", "."));
        assertEquals("Hello", StrUtil.substringAfterLastOrElseItself("Hello", "xyz"));

        assertEquals("name", StrUtil.substringAfterLastOrElseItself("file.name.txt", ".", 9));
        assertEquals("file.name.txt", StrUtil.substringAfterLastOrElseItself("file.name.txt", ".", 3));

        assertEquals("He", StrUtil.substringBeforeOrElseItself("Hello World", 'l'));
        assertEquals("Hello", StrUtil.substringBeforeOrElseItself("Hello", 'x'));

        assertEquals("Hello", StrUtil.substringBeforeOrElseItself("Hello World", " World"));
        assertEquals("Hello", StrUtil.substringBeforeOrElseItself("Hello", "xyz"));

        assertEquals("lo", StrUtil.substringBeforeOrElseItself("Hello World", 3, " World"));
        assertEquals("Hello World", StrUtil.substringBeforeOrElseItself("Hello World", 7, " "));

        assertEquals("file.name", StrUtil.substringBeforeLastOrElseItself("file.name.txt", '.'));
        assertEquals("Hello", StrUtil.substringBeforeLastOrElseItself("Hello", 'x'));

        assertEquals("file.name", StrUtil.substringBeforeLastOrElseItself("file.name.txt", "."));
        assertEquals("Hello", StrUtil.substringBeforeLastOrElseItself("Hello", "xyz"));

        assertEquals("le.name", StrUtil.substringBeforeLastOrElseItself("file.name.txt", 2, "."));
        assertEquals("file.name.txt", StrUtil.substringBeforeLastOrElseItself("file.name.txt", 10, "."));
    }

    @Test
    public void testStrUtil_SubstringBetween() {
        assertEquals("ll", StrUtil.substringBetween("Hello", 1, 4).orElse(null));
        assertTrue(StrUtil.substringBetween("Hello", -1, 4).isPresent());
        assertEquals("Hell", StrUtil.substringBetween("Hello", -1, 4).orElse(null));

        assertEquals("ll", StrUtil.substringBetween("Hello World", 1, 'o').orElse(null));
        assertFalse(StrUtil.substringBetween("Hello", 1, 'x').isPresent());

        assertEquals("ello", StrUtil.substringBetween("Hello World", 0, " World").orElse(null));
        assertFalse(StrUtil.substringBetween("Hello", 0, "xyz").isPresent());

        assertEquals("llo", StrUtil.substringBetween("Hello World", 'e', 5).orElse(null));
        assertFalse(StrUtil.substringBetween("Hello", 'x', 5).isPresent());

        assertEquals("llo", StrUtil.substringBetween("Hello World", "He", 5).orElse(null));
        assertFalse(StrUtil.substringBetween("Hello", "xyz", 5).isPresent());

        assertEquals("b", StrUtil.substringBetween("abc", 'a', 'c').orElse(null));
        assertFalse(StrUtil.substringBetween("abc", 'x', 'c').isPresent());

        assertEquals("abc", StrUtil.substringBetween("xabcx", "x").orElse(null));
        assertEquals("b", StrUtil.substringBetween("abc", "a", "c").orElse(null));
        assertFalse(StrUtil.substringBetween("abc", "x", "c").isPresent());

        assertEquals("", StrUtil.substringBetween("abcabc", 2, "b", "").orElse(null));
        assertFalse(StrUtil.substringBetween("abc", 5, "a", "c").isPresent());

        assertEquals("e", StrUtil.substringBetween("Hello", 0, i -> i + 2).orElse(null));
        assertFalse(StrUtil.substringBetween("Hello", 0, i -> -1).isPresent());

        assertEquals("l", StrUtil.substringBetween("Hello", i -> i - 2, 4).orElse(null));
        assertEquals("Hell", StrUtil.substringBetween("Hello", i -> -1, 4).orElse(null));

        assertEquals("ello Wor", StrUtil.substringBetween("Hello World", "H", i -> i + 8).orElse(null));
        assertFalse(StrUtil.substringBetween("Hello", "H", i -> -1).isPresent());

        assertEquals("Hello ", StrUtil.substringBetween("Hello World", i -> -1, "World").orElse(null));
        assertFalse(StrUtil.substringBetween("Hello", i -> -1, "World").isPresent());
    }

    @Test
    public void testStrUtil_CreateNumber() {
        assertEquals(123, StrUtil.createInteger("123").orElse(-1));
        assertFalse(StrUtil.createInteger("abc").isPresent());
        assertFalse(StrUtil.createInteger("").isPresent());
        assertFalse(StrUtil.createInteger(null).isPresent());

        assertEquals(123L, StrUtil.createLong("123").orElse(-1L));
        assertFalse(StrUtil.createLong("abc").isPresent());
        assertFalse(StrUtil.createLong("").isPresent());
        assertFalse(StrUtil.createLong(null).isPresent());

        assertEquals(123.45f, StrUtil.createFloat("123.45").orElse(-1f), 0.001f);
        assertFalse(StrUtil.createFloat("abc").isPresent());
        assertFalse(StrUtil.createFloat("").isPresent());
        assertFalse(StrUtil.createFloat(null).isPresent());

        assertEquals(123.45, StrUtil.createDouble("123.45").orElse(-1.0), 0.001);
        assertFalse(StrUtil.createDouble("abc").isPresent());
        assertFalse(StrUtil.createDouble("").isPresent());
        assertFalse(StrUtil.createDouble(null).isPresent());

        assertEquals(new BigInteger("123456789012345678901234567890"), StrUtil.createBigInteger("123456789012345678901234567890").orElse(null));
        assertFalse(StrUtil.createBigInteger("abc").isPresent());
        assertFalse(StrUtil.createBigInteger("").isPresent());
        assertFalse(StrUtil.createBigInteger(null).isPresent());

        assertEquals(new BigDecimal("123.45678901234567890"), StrUtil.createBigDecimal("123.45678901234567890").orElse(null));
        assertFalse(StrUtil.createBigDecimal("abc").isPresent());
        assertFalse(StrUtil.createBigDecimal("").isPresent());
        assertFalse(StrUtil.createBigDecimal(null).isPresent());

        assertTrue(StrUtil.createNumber("123").isPresent());
        assertTrue(StrUtil.createNumber("123.45").isPresent());
        assertTrue(StrUtil.createNumber("123L").isPresent());
        assertFalse(StrUtil.createNumber("abc").isPresent());
        assertFalse(StrUtil.createNumber("").isPresent());
        assertFalse(StrUtil.createNumber(null).isPresent());
    }

    @Test
    public void testEdgeCases() {
        String longString = "a".repeat(10000);
        assertEquals(10000, reverse(longString).length());
        assertEquals(10000, sort(longString).length());

        String unicode = "Hello 世界 🌍";
        assertNotNull(reverse(unicode));
        assertNotNull(sort(unicode));

        String special = "!@#$%^&*()_+-=[] {}|;:'\",.<>?/\\`~";
        assertNotNull(reverse(special));
        assertNotNull(sort(special));
    }

    @Test
    public void testNullHandling() {
        assertNull(substring(null, 0));
        assertNull(substringAfter(null, 'a'));
        assertNull(substringBefore(null, 'a'));
        assertNull(reverse(null));
        assertNull(sort(null));
        assertNull(rotate(null, 5));
        assertNull(shuffle(null));
        assertEquals("", join((Object[]) null));
        assertEquals("", base64Encode(null));
        assertEquals("", base64EncodeString(null));
    }

    @Test
    public void testEmptyStringHandling() {
        assertEquals("", substring("", 0));
        assertNull(substringAfter("", 'a'));
        assertEquals("", reverse(""));
        assertEquals("", sort(""));
        assertEquals("", rotate("", 5));
        assertEquals("", shuffle(""));
        assertEquals("", join(new Object[] {}));
        assertEquals("", base64Encode(new byte[] {}));
        assertEquals("", base64EncodeString(""));
    }
}
