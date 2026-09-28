package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.base64Encode;
import static com.landawn.abacus.util.Strings.base64EncodeString;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.reverse;
import static com.landawn.abacus.util.Strings.rotate;
import static com.landawn.abacus.util.Strings.shuffle;
import static com.landawn.abacus.util.Strings.sort;
import static com.landawn.abacus.util.Strings.substring;
import static com.landawn.abacus.util.Strings.substringAfter;
import static com.landawn.abacus.util.Strings.substringBefore;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.DelimiterMatchMode;
import com.landawn.abacus.util.Strings.StrUtil;

public class StringsTest extends StringsTestSupport {
    @Test
    public void testUuid_Unique() {
        String uuid1 = Strings.uuid();
        String uuid2 = Strings.uuid();
        assertFalse(uuid1.equals(uuid2));
    }

    @Test
    public void testUuid() {
        String uuid = Strings.uuid();
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
    }

    @Test
    public void testGuid_Unique() {
        String guid1 = Strings.uuidWithoutHyphens();
        String guid2 = Strings.uuidWithoutHyphens();
        assertFalse(guid1.equals(guid2));
    }

    @Test
    public void testGuid() {
        String guid = Strings.uuidWithoutHyphens();
        assertNotNull(guid);
        assertEquals(32, guid.length());
        assertFalse(guid.contains("-"));
        assertTrue(guid.matches("[a-f0-9]{32}"));
    }

    // ==================== NEW TESTS FOR UNTESTED METHODS ====================

    @Test
    public void testUuidWithoutHyphens() {
        String result = Strings.uuidWithoutHyphens();
        assertNotNull(result);
        assertEquals(32, result.length());
        assertFalse(result.contains("-"));
        // Should generate unique values
        assertNotEquals(result, Strings.uuidWithoutHyphens());
    }

    @Test
    public void testUuidWithoutHyphens_matchesCanonicalHexLayout() {
        final String hex = Strings.uuidWithoutHyphens();
        final String canonical = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-"
                + hex.substring(20);
        final UUID parsed = UUID.fromString(canonical);
        assertEquals(hex, parsed.toString().replace("-", ""));
    }

    @Test
    public void testValueOf_CharArray() {
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
        assertEquals("Hello World", Strings.valueOf("Hello World".toCharArray()));
    }

    @Test
    public void testValueOf_Null() {
        assertNull(Strings.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyArray() {
        assertEquals("", Strings.valueOf(new char[0]));
    }

    @Test
    public void testValueOfCharArray() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testValueOf() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("hello", Strings.valueOf(new char[] { 'h', 'e', 'l', 'l', 'o' }));
    }

    @Test
    public void test_findEmail() {
        final String str = "*** test@gmail.orgg&&^ test2@gmail.cn ((& ";
        assertTrue(Strings.isValidEmailAddress("test@gmail.com"));
        assertEquals("test@gmail.orgg", Strings.findFirstEmailAddress(str));
        assertEquals(List.of("test@gmail.orgg", "test2@gmail.cn"), Strings.findAllEmailAddresses(str));
    }

    @Test
    public void testNullSafety() {
        assertDoesNotThrow(() -> {
            Strings.isEmpty(null);
            Strings.isBlank(null);
            Strings.trim(null);
            Strings.toLowerCase(null);
            Strings.toUpperCase(null);
            Strings.contains(null, "test");
            Strings.indexOf(null, "test");
            Strings.split(null, ",");
            Strings.replaceAll((String) null, "a", "b");
        });
    }

    @Test
    public void testStringBuilderInput() {
        StringBuilder sb = new StringBuilder("test");
        assertTrue(Strings.isNotEmpty(sb));
        assertFalse(Strings.isBlank(sb));
        assertEquals(4, Strings.lengthOfCommonPrefix(sb, "test"));
    }

    @Test
    public void testUnicodeCharacters() {
        String unicode = "Hello 世界";

        assertTrue(Strings.isNotEmpty(unicode));
        assertFalse(Strings.isAsciiPrintable(unicode));
        assertTrue(Strings.contains(unicode, "世界"));
    }

    @Test
    public void testDefaultIfNull_NotNull() {
        assertEquals("hello", Strings.defaultIfNull("hello", "default"));
        assertEquals("", Strings.defaultIfNull("", "default"));
        assertEquals("   ", Strings.defaultIfNull("   ", "default"));
    }

    @Test
    public void testDefaultIfNull_Null() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
    }

    @Test
    public void testDefaultIfNull_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfNull("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfNull(null, () -> "default"));
    }

    @Test
    public void testDefaultIfNull_NullDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull((String) null, (String) null));
    }

    @Test
    public void testDefaultIfNull_SupplierNullDefault() {
        assertThrows(NullPointerException.class, () -> Strings.<String> defaultIfNull(null, () -> null));
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
        assertThrows(NullPointerException.class, () -> Strings.defaultIfNull((CharSequence) null, (Supplier<? extends CharSequence>) () -> null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("abc", (Supplier<String>) null));
    }

    @Test
    public void testDefaultIfEmpty_NotEmpty() {
        assertEquals("hello", Strings.defaultIfEmpty("hello", "default"));
        assertEquals("   ", Strings.defaultIfEmpty("   ", "default"));
    }

    @Test
    public void testDefaultIfEmpty_Empty() {
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
    }

    @Test
    public void testDefaultIfEmpty_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfEmpty("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfEmpty("", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfEmpty(null, () -> "default"));
    }

    @Test
    public void testDefaultIfEmpty_EmptyDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty((String) null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty((String) null, (String) null));
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
    public void testDefaultIfBlank_NotBlank() {
        assertEquals("hello", Strings.defaultIfBlank("hello", "default"));
        assertEquals("  abc  ", Strings.defaultIfBlank("  abc  ", "default"));
    }

    @Test
    public void testDefaultIfBlank_Blank() {
        assertEquals("default", Strings.defaultIfBlank("   ", "default"));
        assertEquals("default", Strings.defaultIfBlank("", "default"));
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
    }

    @Test
    public void testDefaultIfBlank_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfBlank("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfBlank("   ", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfBlank(null, () -> "default"));
    }

    @Test
    public void testDefaultIfBlank_BlankDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, "  "));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, (String) null));
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
    public void testNullToEmpty() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("abc", Strings.nullToEmpty("abc"));
        assertEquals("   ", Strings.nullToEmpty("   "));
    }

    @Test
    public void testNullToEmptyString() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("abc", Strings.nullToEmpty("abc"));
    }

    @Test
    public void testNullElementsToEmpty_Array() {
        String[] array = { null, "abc", null, "xyz" };
        Strings.nullElementsToEmpty(array);
        assertArrayEquals(new String[] { "", "abc", "", "xyz" }, array);

        String[] emptyArray = {};
        Strings.nullElementsToEmpty(emptyArray);
        assertArrayEquals(new String[] {}, emptyArray);
    }

    @Test
    public void testNullElementsToEmptyArray() {
        String[] arr = { null, "a", null, "" };
        Strings.nullElementsToEmpty(arr);
        assertArrayEquals(new String[] { "", "a", "", "" }, arr);
        Strings.nullElementsToEmpty((String[]) null);
    }

    @Test
    public void testEmptyToNull() {
        assertNull(Strings.emptyToNull(""));
        assertNull(Strings.emptyToNull((String) null));
        assertEquals("abc", Strings.emptyToNull("abc"));
        assertEquals("   ", Strings.emptyToNull("   "));
    }

    @Test
    public void testEmptyToNullString() {
        assertNull(Strings.emptyToNull((String) null));
        assertNull(Strings.emptyToNull(""));
        assertEquals("abc", Strings.emptyToNull("abc"));
        assertEquals(" ", Strings.emptyToNull(" "));
    }

    @Test
    public void testEmptyElementsToNull_Array() {
        String[] array = { "", "abc", "", "xyz" };
        Strings.emptyElementsToNull(array);
        assertArrayEquals(new String[] { null, "abc", null, "xyz" }, array);
    }

    @Test
    public void testEmptyElementsToNullArray() {
        String[] arr = { null, "a", "", " " };
        Strings.emptyElementsToNull(arr);
        assertArrayEquals(new String[] { null, "a", null, " " }, arr);
        Strings.emptyElementsToNull((String[]) null);
    }

    @Test
    public void testBlankToEmpty() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("   "));
        assertEquals("abc", Strings.blankToEmpty("abc"));
        assertEquals("  abc  ", Strings.blankToEmpty("  abc  "));
    }

    @Test
    public void testBlankToEmptyString() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("   "));
        assertEquals("abc", Strings.blankToEmpty("abc"));
    }

    @Test
    public void testBlankElementsToEmpty_Array() {
        String[] array = { null, "abc", "  ", "xyz" };
        Strings.blankElementsToEmpty(array);
        assertArrayEquals(new String[] { "", "abc", "", "xyz" }, array);
    }

    @Test
    public void testBlankElementsToEmptyArray() {
        String[] arr = { null, "a", " ", "\t" };
        Strings.blankElementsToEmpty(arr);
        assertArrayEquals(new String[] { "", "a", "", "" }, arr);
    }

    @Test
    public void testBlankToNull() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("   "));
        assertEquals("abc", Strings.blankToNull("abc"));
        assertEquals("  abc  ", Strings.blankToNull("  abc  "));
    }

    @Test
    public void testBlankToNullString() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("   "));
        assertEquals("abc", Strings.blankToNull("abc"));
    }

    @Test
    public void testBlankElementsToNull_Array() {
        String[] array = { "  ", "abc", "", "xyz" };
        Strings.blankElementsToNull(array);
        assertArrayEquals(new String[] { null, "abc", null, "xyz" }, array);
    }

    @Test
    public void testBlankElementsToNullArray() {
        String[] arr = { null, "a", " ", "\t" };
        Strings.blankElementsToNull(arr);
        assertArrayEquals(new String[] { null, "a", null, null }, arr);
    }

    @Test
    public void testBlankElementsToEmpty_NullOrEmptyInput() {
        assertDoesNotThrow(() -> Strings.blankElementsToEmpty((String[]) null));
        assertDoesNotThrow(() -> Strings.blankElementsToEmpty(new String[0]));
    }

    @Test
    public void testBlankElementsToNull_NullOrEmptyInput() {
        assertDoesNotThrow(() -> Strings.blankElementsToNull((String[]) null));
        assertDoesNotThrow(() -> Strings.blankElementsToNull(new String[0]));
    }

    @Test
    public void testAbbreviate() {
        assertEquals("abc", Strings.abbreviate("abc", 5));
        assertEquals("ab...", Strings.abbreviate("abcdefg", 5));
        assertNull(Strings.abbreviate(null, 5));
        assertEquals("", Strings.abbreviate("", 5));
    }

    @Test
    public void testAbbreviate_WithMarker() {
        assertEquals("abc", Strings.abbreviate("abc", "...", 5));
        assertEquals("ab...", Strings.abbreviate("abcdefg", "...", 5));
        assertEquals("abcd*", Strings.abbreviate("abcdefg", "*", 5));
        assertNull(Strings.abbreviate(null, "...", 5));
    }

    @Test
    public void testAbbreviateWithMarker() {
        assertEquals("abc***", Strings.abbreviate("abcdefg", "***", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", "***", 7));
        assertNull(Strings.abbreviate(null, "***", 4));
    }

    @Test
    public void testAbbreviate_OffsetBeyondLengthAndEmptyMarker() {
        assertEquals("abcd", Strings.abbreviate("abcdef", "", 100, 4));
        assertEquals("a", Strings.abbreviate("abcdef", "", 100, 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdef", "", 100, 0));
        assertEquals("...ghij", Strings.abbreviate("abcdefghij", "...", 100, 7));
        assertEquals("abcdefg...", Strings.abbreviate("abcdefghijklmno", "...", -1, 10));
        assertEquals("abcdefg...", Strings.abbreviate("abcdefghijklmno", "...", Integer.MIN_VALUE, 10));
    }

    @Test
    public void testAbbreviate_OffsetOverloadsArePublic() throws NoSuchMethodException {
        assertTrue(Modifier.isPublic(Strings.class.getDeclaredMethod("abbreviate", String.class, int.class, int.class).getModifiers()));
        assertTrue(Modifier.isPublic(Strings.class.getDeclaredMethod("abbreviate", String.class, String.class, int.class, int.class).getModifiers()));
    }

    @Test
    public void testAbbreviateMaxLength() {
        assertNull(Strings.abbreviate(null, 10));
        assertEquals("", Strings.abbreviate("", 10));
        // null/empty input must not throw on too-small maxLength (null-in/null-out, empty-in/empty-out)
        assertNull(Strings.abbreviate(null, 3));
        assertEquals("", Strings.abbreviate("", 3));
        assertEquals("abc...", Strings.abbreviate("abcdefg", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 7));
        assertEquals("a...", Strings.abbreviate("abcdefg", 4));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abc", 3));
    }

    @Test
    public void testAbbreviateMarkerMaxLength() {
        assertNull(Strings.abbreviate(null, "...", 10));
        assertEquals("", Strings.abbreviate("", "...", 10));
        assertNull(Strings.abbreviate(null, "...", 3));
        assertEquals("", Strings.abbreviate("", "...", 3));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", null, 10));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", null, 0));
        assertEquals("a", Strings.abbreviate("abcdefg", "", 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "", 0));
        assertEquals("abc..", Strings.abbreviate("abcdefg", "..", 5));
        assertEquals("a..", Strings.abbreviate("abcdefg", "..", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abc", "..", 2));
    }

    @Test
    public void testAbbreviate_OffsetMinLengthException() {
        // marker="..." (len=3), minAbbrevLengthOffset=7, offset=6 > 4
        // maxLength=6 < 7 => IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("Hello World!!", "...", 6, 6));
    }

    @Test
    public void testAbbreviate_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals("...", Strings.abbreviate(emoji + "abc", 4));
        assertEquals("", Strings.abbreviate(emoji + "abc", "", 1));
        assertEquals("...efgh", Strings.abbreviate("abcd" + emoji + "efgh", "...", 5, 7));
    }

    @Test
    public void testAbbreviateMiddle() {
        assertEquals("abc...xyz", Strings.abbreviateMiddle("abcdefghijklmnopqrstuvwxyz", "...", 9));
        assertEquals("abc", Strings.abbreviateMiddle("abc", "...", 10));
        assertNull(Strings.abbreviateMiddle(null, "...", 5));
    }

    @Test
    public void testAbbreviateMiddle_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals("a.cd", Strings.abbreviateMiddle("a" + emoji + "bcd", ".", 4));
        assertEquals("ab.", Strings.abbreviateMiddle("abcd" + emoji, ".", 3));
    }

    @Test
    public void testCenter_WithChar() {
        assertEquals("**abc**", Strings.center("abc", 7, '*'));
        assertEquals("*abc**", Strings.center("abc", 6, '*'));
        assertEquals("abc", Strings.center("abc", 3, '*'));
    }

    @Test
    public void testCenterSizeStr() {
        assertEquals("yzayz", Strings.center("a", 5, "yz"));
        assertEquals("  abc  ", Strings.center("abc", 7, ""));
        assertEquals("ab", Strings.center("ab", 1, "yz"));
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
    public void testCenter() {
        assertEquals("  abc  ", Strings.center("abc", 7));
        assertEquals(" abc  ", Strings.center("abc", 6));
        assertEquals("abc", Strings.center("abc", 3));
        assertEquals("abc", Strings.center("abc", 2));
        assertEquals("   ", Strings.center(null, 3));
    }

    @Test
    public void testCenter_WithString() {
        assertEquals("--abc--", Strings.center("abc", 7, "--"));
        assertEquals("-abc--", Strings.center("abc", 6, "-"));
        assertEquals("abc", Strings.center("abc", 3, "-"));
        assertEquals("-----", Strings.center(null, 5, "-"));
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
    public void testPadStart_WithChar() {
        assertEquals("00abc", Strings.padStart("abc", 5, '0'));
        assertEquals("abc", Strings.padStart("abc", 3, '0'));
    }

    @Test
    public void testPadStart_WithString() {
        assertEquals("--abc", Strings.padStart("abc", 5, "--"));
        assertEquals("abc", Strings.padStart("abc", 3, "-"));
    }

    @Test
    public void testPadStart_WithStringPad() {
        assertEquals("--abc", Strings.padStart("abc", 5, "--"));
        assertEquals("abc", Strings.padStart("abc", 3, "--"));
        assertEquals("--abc", Strings.padStart("abc", 5, "--"));
    }

    @Test
    public void testPadStart() {
        assertEquals("  abc", Strings.padStart("abc", 5));
        assertEquals("abc", Strings.padStart("abc", 3));
        assertEquals("abc", Strings.padStart("abc", 2));
        assertEquals("     ", Strings.padStart(null, 5));
    }

    @Test
    public void testPadStart_WithString_FullCoverage() {
        // basic padding
        assertEquals("00abc", Strings.padStart("abc", 5, "0"));
        assertEquals("xyzxabc", Strings.padStart("abc", 7, "xyz"));

        // no padding needed
        assertEquals("abcde", Strings.padStart("abcde", 3, "0"));
        assertEquals("abc", Strings.padStart("abc", 3, "0"));

        // null input is treated as empty, then padded
        assertEquals("00", Strings.padStart(null, 2, "0"));

        // padding where padStr is longer than remaining
        assertEquals("ababc", Strings.padStart("c", 5, "ab"));
    }

    @Test
    public void testPadEnd_WithChar() {
        assertEquals("abc00", Strings.padEnd("abc", 5, '0'));
        assertEquals("abc", Strings.padEnd("abc", 3, '0'));
    }

    @Test
    public void testPadEnd_WithString() {
        assertEquals("abc--", Strings.padEnd("abc", 5, "--"));
        assertEquals("abc", Strings.padEnd("abc", 3, "-"));
    }

    @Test
    public void testPadEnd_WithStringPad() {
        assertEquals("abc--", Strings.padEnd("abc", 5, "--"));
        assertEquals("abc", Strings.padEnd("abc", 3, "--"));
    }

    @Test
    public void testPadEnd() {
        assertEquals("abc  ", Strings.padEnd("abc", 5));
        assertEquals("abc", Strings.padEnd("abc", 3));
        assertEquals("abc", Strings.padEnd("abc", 2));
        assertEquals("     ", Strings.padEnd(null, 5));
    }

    @Test
    public void testPadEnd_NullStr() {
        assertEquals("*****", Strings.padEnd(null, 5, "*"));
        assertEquals("**", Strings.padEnd(null, 2, "*"));
        assertEquals("", Strings.padEnd(null, 0, "*"));
    }

    @Test
    public void testPadEnd_NullOrEmptyPadStr() {
        assertEquals("abc  ", Strings.padEnd("abc", 5, null));
        assertEquals("abc  ", Strings.padEnd("abc", 5, ""));
    }

    @Test
    public void testStringPadding_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals(emoji + "x", Strings.padStart("x", 2, emoji));
        assertEquals("x" + emoji, Strings.padEnd("x", 2, emoji));
        assertEquals(emoji + "x", Strings.center("x", 3, emoji));
    }

    @Test
    public void testStringPadding_BulkPathMatchesReference() {
        final String[] values = { null, "", "x", "xy", "\uD83D\uDE00x" };
        final String[] padStrings = { null, "", "0", "ab", "abc", "\uD83D\uDE00", "x\uD83D\uDE00", "x\uD83D" };
        final int[] minimumLengths = { 0, 1, 2, 3, 4, 5, 16, 31, 32, 33, 256, 257 };

        for (final String value : values) {
            for (final String padString : padStrings) {
                for (final int minimumLength : minimumLengths) {
                    assertEquals(padStartReference(value, minimumLength, padString), Strings.padStart(value, minimumLength, padString));
                    assertEquals(padEndReference(value, minimumLength, padString), Strings.padEnd(value, minimumLength, padString));
                }
            }
        }

        assertEquals(padStartReference("", 256, "ab"), Strings.padStart(null, 256, "ab"));
        assertEquals(padEndReference("", 256, "ab"), Strings.padEnd(null, 256, "ab"));
    }

    @Test
    public void test_repeat() {
        String sql = Strings.repeat("?", 0, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN ()", sql);

        sql = Strings.repeat("?", 1, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?)", sql);

        sql = Strings.repeat("?", 2, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?)", sql);

        sql = Strings.repeat("?", 3, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?)", sql);

        sql = Strings.repeat("?", 4, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?, ?)", sql);

        sql = Strings.repeat("?", 5, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?, ?, ?)", sql);

        for (int n = 0; n < 10; n++) {
            assertEquals(new String(Array.repeat('a', n)), Strings.repeat('a', n));
            assertEquals(Strings.join(Array.repeat('a', n), ","), Strings.repeat('a', n, ','));
            assertEquals("ab".repeat(n), Strings.repeat("ab", n));
            assertEquals(Strings.join(Array.repeat("ab", n), ", "), Strings.repeat("ab", n, ", "));
        }
    }

    @Test
    public void testRepeat_CharWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("a", Strings.repeat('a', 1, ','));
        assertEquals("", Strings.repeat('a', 0, ','));
    }

    @Test
    public void testRepeat_StringWithDelimiter() {
        assertEquals("abc,abc,abc", Strings.repeat("abc", 3, ","));
        assertEquals("abc", Strings.repeat("abc", 1, ","));
        assertEquals("", Strings.repeat("abc", 0, ","));
    }

    @Test
    public void testRepeat_WithPrefixSuffix() {
        assertEquals("[abc,abc,abc]", Strings.repeat("abc", 3, ",", "[", "]"));
        assertEquals("[abc]", Strings.repeat("abc", 1, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("abc", 0, ",", "[", "]"));
        assertEquals("[,,]", Strings.repeat("", 3, ",", "[", "]"));
        assertEquals("xxx", Strings.repeat("x", 3, null, null, null));
    }

    @Test
    public void testRepeat_WithDelimiterBulkPathMatchesReference() {
        final String[] values = { null, "", "x", "ab", "\uD83D\uDE00" };
        final String[] delimiters = { null, "", ",", "::", "\uD83D\uDE00" };
        final String[][] affixes = { { null, null }, { "", "" }, { "[", "]" }, { "prefix", "suffix" } };
        final int[] repeatCounts = { 0, 1, 2, 3, 7, 8, 16, 257 };

        for (final String value : values) {
            for (final String delimiter : delimiters) {
                for (final String[] affix : affixes) {
                    for (final int repeatCount : repeatCounts) {
                        assertEquals(repeatReference(value, repeatCount, delimiter, affix[0], affix[1]),
                                Strings.repeat(value, repeatCount, delimiter, affix[0], affix[1]));
                    }
                }
            }
        }
    }

    @Test
    public void testRepeatWithDelimiterRejectsImpossibleLengthBeforeAppending() {
        final OutOfMemoryError charError = assertThrows(OutOfMemoryError.class, () -> Strings.repeat('a', Integer.MAX_VALUE, ','));
        assertTrue(charError.getMessage().contains("Required string length"));

        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("", Integer.MAX_VALUE, ","));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("a", Integer.MAX_VALUE, ",", "[", "]"));
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
    public void testRepeatWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("hello,hello", Strings.repeat("hello", 2, ","));
        assertEquals("", Strings.repeat("hello", 0, ","));
        assertEquals(",,", Strings.repeat(null, 3, ","));
        assertEquals(",,", Strings.repeat(null, 3, ','));
    }

    @Test
    public void testRepeatWithPrefixSuffix() {
        assertEquals("[a,a,a]", Strings.repeat("a", 3, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("a", 0, ",", "[", "]"));
    }

    @Test
    public void testJoinerAndStringsJoinAgree() {
        final String[] values = { "a", "b", "c" };
        assertEquals(Joiner.with(", ").appendAll(values).toString(), Strings.join(values, ", "));
    }

    @Test
    public void test_repeat_2() {
        for (int n = 0; n < 10; n++) {
            final String repeated = Strings.repeat("ab", n, ", ");
            assertEquals(repeated + ")", Strings.repeat("ab", n, ", ", null, ")"));
            assertEquals("(" + repeated, Strings.repeat("ab", n, ", ", "(", null));
            assertEquals("(" + repeated + ")", Strings.repeat("ab", n, ", ", "(", ")"));
        }
    }

    @Test
    public void testEdgeCases() {
        assertFalse(Character.isLetter('\u24D0'));
        assertFalse(Character.isLetter('\u24B6'));
        assertTrue(Strings.isAllLowerCase("a\u24D0"));
        assertFalse(Strings.isAllUpperCase("a\u24D0"));
        assertTrue(Strings.isAllUpperCase("A\u24B6"));
        assertFalse(Strings.isAllLowerCase("A\u24B6"));

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
    public void testLongStrings() {
        String longStr = Strings.repeat('a', 10000);
        assertEquals(10000, longStr.length());
        assertTrue(Strings.isAllLowerCase(longStr));
        assertEquals(1, Strings.countMatches(longStr + "b" + longStr, "b"));
    }

    @Test
    public void testEmptyStringEdgeCases() {
        assertEquals("", Strings.repeat("", 5));
        assertEquals(0, Strings.countMatches("", ""));
        assertTrue(Strings.contains("abc", ""));
        assertEquals("abc", Strings.removeAll("abc", ""));
    }

    @Test
    public void testRepeat_Char() {
        assertEquals("aaa", Strings.repeat('a', 3));
        assertEquals("", Strings.repeat('a', 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    public void testRepeat_String() {
        assertEquals("abcabcabc", Strings.repeat("abc", 3));
        assertEquals("", Strings.repeat("abc", 0));
        assertEquals("", Strings.repeat(null, 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("abc", -1));
    }

    @Test
    public void testRepeatString() {
        assertEquals("ababab", Strings.repeat("ab", 3));
        assertEquals("", Strings.repeat("ab", 0));
        assertEquals("", Strings.repeat(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("ab", -1));
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
    public void testRepeatChar() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("a", Strings.repeat('a', 1));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
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
    public void testGetBytes_WithCharset() {
        final String source = "café世界";

        assertArrayEquals(source.getBytes(StandardCharsets.UTF_8), Strings.getBytes(source, StandardCharsets.UTF_8));
        assertNull(Strings.getBytes(null, StandardCharsets.UTF_8));
        assertNull(Strings.getBytes(null, null));
        assertThrows(NullPointerException.class, () -> Strings.getBytes("x", null));
        // empty source is non-null, so charset is still required (primary-first only for null source)
        assertThrows(NullPointerException.class, () -> Strings.getBytes("", null));
    }

    @Test
    public void testStrictCharsetConversions() {
        assertArrayEquals("café".getBytes(StandardCharsets.UTF_8), Strings.getBytesStrict("café", StandardCharsets.UTF_8));
        assertArrayEquals("café".getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8Strict("café"));
        assertNull(Strings.getBytesStrict(null, null));
        assertThrows(NullPointerException.class, () -> Strings.getBytesStrict("", null));

        // The JDK-compatible methods replace malformed/unmappable input; the strict counterparts report it.
        assertArrayEquals(new byte[] { '?' }, Strings.getBytes("é", StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> Strings.getBytesStrict("é", StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> Strings.getBytesUtf8Strict("\uD800"));

        assertEquals("Pw==", Strings.base64EncodeString("é", StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64EncodeStringStrict("é", StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64EncodeStringStrict("\uD800"));
        assertEquals("\uFFFD", Strings.base64DecodeToString("/w==", StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64DecodeToStringStrict("/w==", StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64DecodeToStringStrict("/w=="));

        assertEquals("Pw", Strings.base64UrlEncodeString("é", StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlEncodeStringStrict("é", StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlEncodeStringStrict("\uD800"));
        assertEquals("\uFFFD", Strings.base64UrlDecodeToString("_w", StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlDecodeToStringStrict("_w", StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlDecodeToStringStrict("_w"));

        assertEquals("", Strings.base64EncodeStringStrict(null, null));
        assertEquals("", Strings.base64DecodeToStringStrict("", null));
        assertEquals("", Strings.base64UrlEncodeStringStrict(null, null));
        assertEquals("", Strings.base64UrlDecodeToStringStrict("", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64DecodeToStringStrict("!!!!", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlDecodeToStringStrict("!!!!", null));

        // Positive paths cover all strict overloads and distinguish the basic and URL-safe alphabets.
        assertEquals("Pz8/", Strings.base64EncodeStringStrict("???"));
        assertEquals("???", Strings.base64DecodeToStringStrict("Pz8/"));
        assertEquals("/v8AVABlAHMAdAAxADIAMw==", Strings.base64EncodeStringStrict("Test123", StandardCharsets.UTF_16));
        assertEquals("Test123", Strings.base64DecodeToStringStrict("/v8AVABlAHMAdAAxADIAMw==", StandardCharsets.UTF_16));
        assertEquals("Pz8_", Strings.base64UrlEncodeStringStrict("???"));
        assertEquals("???", Strings.base64UrlDecodeToStringStrict("Pz8_"));
        assertEquals("_v8AVABlAHMAdAAxADIAMw", Strings.base64UrlEncodeStringStrict("Test123", StandardCharsets.UTF_16));
        assertEquals("Test123", Strings.base64UrlDecodeToStringStrict("_v8AVABlAHMAdAAxADIAMw", StandardCharsets.UTF_16));

        // Structurally valid, non-empty input reaches the documented charset validation.
        assertThrows(NullPointerException.class, () -> Strings.base64EncodeStringStrict("a", null));
        assertThrows(NullPointerException.class, () -> Strings.base64DecodeToStringStrict("YQ==", null));
        assertThrows(NullPointerException.class, () -> Strings.base64UrlEncodeStringStrict("a", null));
        assertThrows(NullPointerException.class, () -> Strings.base64UrlDecodeToStringStrict("YQ", null));
    }

    @Test
    public void testCollaboratorNullValidationOrder() {
        // Primary-first: null/empty primary bypasses locale
        assertNull(Strings.toLowerCase(null, null));
        assertEquals("", Strings.toLowerCase("", null));
        assertThrows(NullPointerException.class, () -> Strings.toLowerCase("A", null));
        assertNull(Strings.toUpperCase(null, null));
        assertEquals("", Strings.toUpperCase("", null));
        assertThrows(NullPointerException.class, () -> Strings.toUpperCase("a", null));

        // The random generator is validated before no-op returns.
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle("a", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle("ab", null));

        // Primary-first Base64 charset collaborators
        assertEquals("", Strings.base64EncodeString(null, null));
        assertEquals("", Strings.base64EncodeString("", null));
        assertThrows(NullPointerException.class, () -> Strings.base64EncodeString("a", null));
        assertEquals("", Strings.base64DecodeToString(null, null));
        assertEquals("", Strings.base64DecodeToString("", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64DecodeToString("!!!!", null));
        assertThrows(NullPointerException.class, () -> Strings.base64DecodeToString("YQ==", null));
        assertEquals("", Strings.base64UrlDecodeToString(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlDecodeToString("!!!!", null));

        // Optional charset (null → UTF-8) for URL query helpers
        assertEquals("", Strings.encodeUrlQuery(null, null));
        assertNotNull(Strings.parseUrlQuery("a=b", (java.nio.charset.Charset) null));
        assertTrue(Strings.parseUrlQuery("", (java.nio.charset.Charset) null).isEmpty());

        // Collaborator-first: target type always required
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("a=b", (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("", (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("", null, null));

        // Collaborator-first: mapper always required
        assertThrows(IllegalArgumentException.class, () -> Strings.mapWords(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("a b", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.mapWords(null, "", String::toUpperCase));
        assertNull(Strings.mapWords(null, " ", String::toUpperCase));
        assertEquals("", Strings.mapWords("", " ", String::toUpperCase));

        final java.util.function.Function<String, String> nullMapper = null;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("a", nullMapper)).getMessage().contains("mapper"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("a", " ", nullMapper)).getMessage().contains("mapper"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("a", " ", Collections.emptyList(), nullMapper)).getMessage()
                .contains("mapper"));

        // Collaborator-first: suppliers always required
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("x", (java.util.function.Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("x", (java.util.function.Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("x", (java.util.function.Supplier<String>) null));

        // Collaborator-first: IntUnaryOperator always required
        assertThrows(IllegalArgumentException.class, () -> Strings.substring(null, 0, (java.util.function.IntUnaryOperator) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween(null, 0, (java.util.function.IntUnaryOperator) null));
    }

    @Test
    public void testGetBytesUtf8() {
        final String source = "café世界";

        assertArrayEquals(source.getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8(source));
        assertArrayEquals(new byte[0], Strings.getBytesUtf8(""));
        assertNull(Strings.getBytesUtf8(null));
    }

    @Test
    public void testLocaleSensitive() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            // No-arg toLowerCase uses Locale.ROOT, not the JVM default locale.
            assertEquals("i", Strings.toLowerCase("I"));
            assertEquals("i", Strings.toLowerCase("I", Locale.ENGLISH));
            assertEquals("ı", Strings.toLowerCase("I", new Locale("tr", "TR")));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testCaseConverters_splitOnCaseBoundaries() {
        // Case-boundary splitting is shared with toSnakeCase; invertibility is not.
        assertEquals("xmlParser", Strings.toCamelCase("XMLParser"));
        assertEquals("XmlParser", Strings.toUpperCamelCase("XMLParser"));
        assertEquals("helloWorldApi", Strings.toCamelCase("helloWorldAPI"));
        assertEquals("fooBarBaz", Strings.toCamelCase("foo_barBaz"));
        // An all-caps single word: the first letter is cased per camel/upper-camel, the rest lowercased.
        assertEquals("user", Strings.toCamelCase("USER"));
        assertEquals("User", Strings.toUpperCamelCase("USER"));
    }

    @Test
    public void testCaseConverters_TitlecaseBoundary() {
        // U+01C5 is a Unicode titlecase letter; its uppercase/lowercase forms are U+01C4/U+01C6.
        final String input = "a\u01C5b";

        // The camel-case forms capitalize the second word with the titlecase mapping (U+01C5), as capitalize()
        // does, not the uppercase mapping (U+01C4). Changed in the 2026-09-02 review; see StringsReviewFixes20260902Test.
        assertEquals("a\u01C5b", Strings.toCamelCase(input));
        assertEquals("A\u01C5b", Strings.toUpperCamelCase(input));
        assertEquals("a_\u01C6b", Strings.toSnakeCase(input));
        assertEquals("A_\u01C4B", Strings.toScreamingSnakeCase(input));
        assertEquals("a-\u01C6b", Strings.toKebabCase(input));
    }

    @Test
    public void testCaseConverters_fullUnicodeMappings() {
        assertEquals("STRASSE", Strings.toScreamingSnakeCase("stra\u00DFe"));
        assertEquals("i\u0307", Strings.toSnakeCase("\u0130"));
        assertEquals("\u03BF\u03C2", Strings.toSnakeCase("\u039F\u03A3"));
        assertEquals("\u03BF\u03C2Value", Strings.toCamelCase("\u039F\u03A3_VALUE"));
        // A capitalized word's first letter uses the Unicode 17 titlecase mapping ("Ss"), as capitalize() does,
        // not String.toUpperCase ("SS"). Changed in the 2026-09-02 review; see StringsReviewFixes20260902Test.
        assertEquals("SsetaName", Strings.toUpperCamelCase("\u00DFeta_name"));
    }

    @Test
    public void test_format_propName() {
        String str = Strings.toSnakeCase("ME_#A3C_AAA_1A2");
        assertEquals("me_#a3c_aaa_1a2", str);

        str = Strings.toScreamingSnakeCase("me_#a3c_aaa_1a2");
        assertEquals("ME_#A3C_AAA_1A2", str);

        str = Strings.toCamelCase("me_#a3c_aaa_1a2");
        assertEquals("me#A3cAaa1A2", str);

        str = Strings.toCamelCase("ME_#A3C_AAA_1A2");
        assertEquals("me#A3cAaa1A2", str);

        assertEquals("xml_parser", Strings.toSnakeCase("XmlParser"));
        assertEquals("xml_parser", Strings.toSnakeCase("xmlPARSER"));
        assertEquals("io_error", Strings.toSnakeCase("IOError"));
        assertEquals("io_error", Strings.toSnakeCase("ioERROR"));
        assertEquals("hello_world_api", Strings.toSnakeCase("helloWorldAPI"));

        assertEquals("XML_PARSER", Strings.toScreamingSnakeCase("XmlParser"));
        assertEquals("XML_PARSER", Strings.toScreamingSnakeCase("xmlPARSER"));
        assertEquals("IO_ERROR", Strings.toScreamingSnakeCase("IOError"));
        assertEquals("IO_ERROR", Strings.toScreamingSnakeCase("ioERROR"));
        assertEquals("HELLO_WORLD_API", Strings.toScreamingSnakeCase("helloWorldAPI"));
    }

    @Test
    public void testScreamingSnakeCase() {
        assertNull(Strings.toScreamingSnakeCase(null));
        assertEquals("", Strings.toScreamingSnakeCase(""));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("helloWorld"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("HelloWorld"));
    }

    @Test
    public void testSwapCaseChar() {
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('a', Strings.swapCase('A'));
        assertEquals('1', Strings.swapCase('1'));
        assertEquals(' ', Strings.swapCase(' '));
    }

    @Test
    public void testSwapCaseString() {
        assertNull(Strings.swapCase(null));
        assertEquals("", Strings.swapCase(""));
        assertEquals("ABC", Strings.swapCase("abc"));
        assertEquals("abc", Strings.swapCase("ABC"));
        assertEquals("AbC", Strings.swapCase("aBc"));
        assertEquals("tHE dOG hAS a bONE", Strings.swapCase("The Dog Has A Bone"));
        assertEquals("hELLO", Strings.swapCase("Hello"));
        assertEquals("tHE DOG HAS A bone", Strings.swapCase("The dog has a BONE"));
        assertEquals("abc123XYZ", Strings.swapCase("ABC123xyz"));
        assertEquals("hELLO wORLD", Strings.swapCase("Hello World"));
        assertEquals("123", Strings.swapCase("123"));

        assertEquals("\u03BF\u03C2", Strings.swapCase("\u039F\u03A3"));
        assertEquals("A\u03C2", Strings.swapCase("a\u03A3"));
        assertEquals("\u03BF\u03C3\u03B1", Strings.swapCase("\u039F\u03A3\u0391"));
        assertEquals("a\u03C3B", Strings.swapCase("A\u03A3b"));
        assertEquals("a'\u03C2", Strings.swapCase("A'\u03A3"));
        assertEquals("a\u03C3'B", Strings.swapCase("A\u03A3'b"));
        assertEquals("a\u03C2\u0301 ", Strings.swapCase("A\u03A3\u0301 "));
        assertEquals("a\u03C3\u0301B", Strings.swapCase("A\u03A3\u0301b"));
        assertEquals("\u03C3", Strings.swapCase("\u03A3"));
    }

    @Test
    public void testStringCaseTransformsUseFullUnicodeMappings() {
        assertEquals('ß', Strings.swapCase('ß')); // char overload is necessarily a simple one-to-one mapping
        assertEquals("SS", Strings.swapCase("ß"));
        assertEquals("i\u0307", Strings.swapCase("\u0130"));

        assertEquals("Sseta", Strings.capitalize("ßeta"));
        assertEquals("\u01C5en", Strings.capitalize("\u01C6en"));
        assertEquals("\u02BCN", Strings.capitalize("\u0149"));
        assertEquals("Ffile", Strings.capitalize("\uFB03le"));

        final String vithkuqiSmallA = new String(Character.toChars(0x10597));
        final String vithkuqiCapitalA = new String(Character.toChars(0x10570));
        assertEquals(vithkuqiCapitalA + "bc", Strings.capitalize(vithkuqiSmallA + "bc"));

        assertEquals("i\u0307stanbul", Strings.uncapitalize("\u0130stanbul"));
        assertEquals("\u01C5en Sseta", Strings.capitalizeWords("\u01C6en ßeta"));
        assertEquals("\u01C5en::Sseta", Strings.capitalizeWords("\u01C6en::ßeta", "::"));
        assertEquals("\u01C5en Sseta", Strings.capitalizeWordsFully("\u01C4EN ßETA"));
    }

    @Test
    public void testUncapitalize() {
        assertEquals("abc", Strings.uncapitalize("Abc"));
        assertEquals("abc", Strings.uncapitalize("abc"));
        assertEquals("aBC", Strings.uncapitalize("ABC"));
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
    }

    @Test
    public void testUncapitalize_EdgeCases() {
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
        assertEquals("a", Strings.uncapitalize("A"));
        assertEquals("hello", Strings.uncapitalize("Hello"));
        assertEquals("hello", Strings.uncapitalize("hello"));
        assertEquals("123", Strings.uncapitalize("123"));
    }

    @Test
    public void testCapitalize() {
        assertEquals("Abc", Strings.capitalize("abc"));
        assertEquals("Abc", Strings.capitalize("Abc"));
        assertEquals("ABC", Strings.capitalize("ABC"));
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
    }

    @Test
    public void testCapitalize_EdgeCases() {
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
        assertEquals("A", Strings.capitalize("a"));
        assertEquals("Hello", Strings.capitalize("hello"));
        assertEquals("Hello", Strings.capitalize("Hello"));
        assertEquals("123", Strings.capitalize("123"));
        assertEquals(" abc", Strings.capitalize(" abc"));
    }

    @Test
    public void testCapitalizeWords() {
        assertEquals("APPLE ORange         Water", Strings.capitalizeWords("APPLE ORange         Water"));
        assertEquals("APPLE", Strings.capitalizeWords("aPPLE"));
        assertEquals("HELLO WORLD", Strings.capitalizeWords("hELLO wORLD"));
        assertEquals("Hello  World", Strings.capitalizeWords("hello  world"));
        assertEquals("Hello\tWorld", Strings.capitalizeWords("hello\tworld"));
        assertEquals("Hello\nWorld", Strings.capitalizeWords("hello\nworld"));
        assertNull(Strings.capitalizeWords(null));
        assertEquals("", Strings.capitalizeWords(""));
    }

    @Test
    public void testCapitalizeWordsWithSeparator() {
        assertEquals("Abc-Def", Strings.capitalizeWords("abc-def", "-"));
        assertEquals("ABC_DEF", Strings.capitalizeWords("ABC_DEF", "_"));
        assertNull(Strings.capitalizeWords(null, "-"));
        assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords("abc", ""));
    }

    @Test
    public void testCapitalizeWordsWithExcludedWords() {
        assertEquals("The Quick Brown Fox", Strings.capitalizeWords("the quick brown fox", " ", Set.of("the")));
        assertEquals("Hello and Goodbye", Strings.capitalizeWords("hello and goodbye", " ", Set.of("and")));
        assertEquals("APPLE ORange    of     WaTer Of", Strings.capitalizeWords("APPLE oRange    of     WaTer Of", " ", Set.of("of")));
        assertEquals("The First Name of the Person", Strings.capitalizeWords("the first name of the person", " ", Set.of("the", "of")));
        assertEquals("Hello World", Strings.capitalizeWords("hello world", " ", null));
        assertEquals("Hello World", Strings.capitalizeWords("hello world", " ", Set.of()));

        List<String> excludedWords = Arrays.asList("the", "and", "or");
        assertEquals("The Quick Brown Fox", Strings.capitalizeWords("the quick brown fox", " ", excludedWords));
        assertNull(Strings.capitalizeWords(null, " ", excludedWords));
    }

    @Test
    public void testCapitalizeWordsFully() {
        assertEquals("Hello World", Strings.capitalizeWordsFully("hELLO wORLD"));
        assertEquals("Hello World", Strings.capitalizeWordsFully("HELLO WORLD"));
        assertEquals("Hello  World", Strings.capitalizeWordsFully("hELLO  wORLD"));
        assertEquals("Hello\tWorld", Strings.capitalizeWordsFully("hELLO\twORLD"));
        assertEquals("Hello-World", Strings.capitalizeWordsFully("hELLO-wORLD", "-"));
        assertEquals("Hello_World", Strings.capitalizeWordsFully("HELLO_WORLD", "_"));
        assertNull(Strings.capitalizeWordsFully(null));
        assertEquals("", Strings.capitalizeWordsFully(""));
        assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWordsFully("abc", ""));
    }

    @Test
    public void testCapitalizeWordsFullyWithExcludedWords() {
        assertEquals("The Lord of the Rings", Strings.capitalizeWordsFully("tHE lORD oF tHE rINGS", " ", Set.of("OF", "THE")));
        assertEquals("Of Mice and Men", Strings.capitalizeWordsFully("OF MICE AND MEN", " ", Set.of("of", "and")));
        assertEquals("Hello World", Strings.capitalizeWordsFully("hELLO wORLD", " ", null));

        List<String> excludedWords = Arrays.asList("the", "of", "and");
        assertEquals("The Lord of the Rings", Strings.capitalizeWordsFully("THE LORD OF THE RINGS", " ", excludedWords));
        assertNull(Strings.capitalizeWordsFully(null, " ", excludedWords));
    }

    /**
     * The {@code String...} overloads of capitalizeWords/capitalizeWordsFully were removed because they were
     * ambiguous with the {@code Collection} overloads for an untyped {@code null} argument. This pins the
     * replacement call shapes (including a bare {@code null}, which must now resolve without a cast).
     */
    @Test
    public void testCapitalizeWords_excludedWordsIsCollectionOnly() {
        assertEquals("Hello World", Strings.capitalizeWords("hello world", " ", null));
        assertEquals("Hello World", Strings.capitalizeWordsFully("hELLO wORLD", " ", null));
        assertEquals("The End the", Strings.capitalizeWords("the end the", " ", Set.of("the")));
        assertEquals("End the End", Strings.capitalizeWordsFully("END THE END", " ", Set.of("the")));

        // The Collection overload accepts any Collection, not just Set.
        assertEquals("The End the", Strings.capitalizeWords("the end the", " ", List.of("the")));
        assertEquals("The End the", Strings.capitalizeWords("the end the", " ", new HashSet<>(List.of("the"))));

        assertEquals(1, N.filter(Strings.class.getMethods(), m -> "capitalizeWords".equals(m.getName()) && m.getParameterCount() == 3).size());
        assertEquals(1, N.filter(Strings.class.getMethods(), m -> "capitalizeWordsFully".equals(m.getName()) && m.getParameterCount() == 3).size());
    }

    /**
     * The word-splitting parameter of capitalizeWords/capitalizeWordsFully is named {@code delimiter} (was
     * {@code separator}) so its validation message matches split/mapWords/reverseDelimited.
     */
    @Test
    public void testCapitalizeWords_delimiterValidationMessage() {
        String expected = assertThrows(IllegalArgumentException.class, () -> Strings.split("x", "")).getMessage();

        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords("x", "")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWordsFully("x", "")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWords("x", "", Set.of("a"))).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.capitalizeWordsFully("x", "", Set.of("a"))).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Strings.mapWords("x", "", Fn.identity())).getMessage());
    }

    /**
     * reverseDelimited(String, String) validates its required delimiter before short-circuiting on a
     * null/empty/single-character input, like split/splitPreserveAllTokens/mapWords/capitalizeWords.
     */
    @Test
    public void testReverseDelimited_delimiterValidatedBeforeInputShortCircuit() {
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited(null, (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("ab", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited(null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a b", ""));

        // A valid delimiter still short-circuits on null/empty/single-character input.
        assertNull(Strings.reverseDelimited(null, " "));
        assertEquals("", Strings.reverseDelimited("", " "));
        assertEquals("a", Strings.reverseDelimited("a", " "));
        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));
        assertEquals("C::B::A", Strings.reverseDelimited("A::B::C", "::"));

        // The char overload has no required-argument check and is unaffected.
        assertNull(Strings.reverseDelimited(null, '.'));
        assertEquals("a", Strings.reverseDelimited("a", '.'));
    }

    /**
     * abbreviate's length errors report the offending maxLength (not just the minimum), the marker's length
     * rather than its contents (so a huge marker cannot produce a huge message), and — for the offset form —
     * the offset the CALLER passed, not the internally clamped/shifted one.
     */
    @Test
    public void testAbbreviate_lengthErrorMessagesIncludeMaxLength() {
        assertEquals("maxLength (3) must be at least 4 for an abbreviationMarker of length 3",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", 3)).getMessage());
        assertEquals("maxLength (2) must be at least 3 for an abbreviationMarker of length 2",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "..", 2)).getMessage());
        assertEquals("maxLength (0) must be at least 1 when abbreviationMarker is empty",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "", 0)).getMessage());
        assertEquals("maxLength (6) must be at least 7 for an abbreviationMarker of length 3 at offset 5",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefghij", "...", 5, 6)).getMessage());

        // offset is clamped to str.length() then shifted left internally; the message must still echo 100.
        assertEquals("maxLength (6) must be at least 7 for an abbreviationMarker of length 3 at offset 100",
                assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefghij", "...", 100, 6)).getMessage());

        // The message must not embed the marker itself, so its size stays bounded.
        String hugeMarker = Strings.repeat('x', 10_000);
        String message = assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefghij", hugeMarker, 0, 2)).getMessage();
        assertTrue(message.length() < 100, message);
        assertFalse(message.contains(hugeMarker));
    }

    /**
     * Characterization tests for Unicode case mapping and exclusion matching.
     */
    @Test
    public void testCaseMappingAndExclusionMatching() {
        // toUpperCamelCase, capitalize and capitalizeWords all capitalize with the Unicode 17 TITLECASE mapping
        // (U+01C5). Before the 2026-09-02 review toUpperCamelCase used the UPPERCASE mapping (U+01C4) and this
        // test pinned that difference; see StringsReviewFixes20260902Test.
        assertEquals("ǅen", Strings.toUpperCamelCase("ǆen"));
        assertEquals("ǅen", Strings.capitalize("ǆen"));
        assertEquals("ǅen", Strings.capitalizeWords("ǆen"));
        assertEquals("ǅen", Strings.capitalizeWordsFully("ǄEN"));

        // excludedWords matching is case-sensitive regardless of a caller-supplied Set's comparator.
        Set<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("the");

        assertEquals("A THE B", Strings.mapWords("a The b", " ", caseInsensitive, String::toUpperCase));
        assertEquals("A THE B", Strings.mapWords("a The b", " ", new HashSet<>(List.of("the")), String::toUpperCase));
        assertEquals("A THE B", Strings.capitalizeWords("a tHE b", " ", caseInsensitive));
    }

    @Test
    public void testConvertWords() {
        assertEquals("HELLO WORLD", Strings.mapWords("hello world", String::toUpperCase));
        assertEquals("abc def", Strings.mapWords("ABC DEF", String::toLowerCase));
        assertEquals("hello!\tworld!", Strings.mapWords("hello\tworld", s -> s + "!"));
        assertNull(Strings.mapWords(null, String::toUpperCase));
    }

    @Test
    public void testConvertWords_WithDelimiter() {
        assertEquals("HELLO-WORLD", Strings.mapWords("hello-world", "-", String::toUpperCase));
        assertNull(Strings.mapWords(null, "-", String::toUpperCase));
    }

    @Test
    public void testConvertWords_WithExcludedWords() {
        List<String> excluded = Arrays.asList("the", "and");
        assertEquals("HELLO the WORLD", Strings.mapWords("hello the world", " ", excluded, String::toUpperCase));
    }

    @Test
    public void testConvertWordsEdgeCases() {
        Assertions.assertEquals("HELLO", Strings.mapWords("hello", " ", null, String::toUpperCase));

        Function<String, String> reverser = s -> new StringBuilder(s).reverse().toString();
        Assertions.assertEquals("olleh dlrow", Strings.mapWords("hello world", " ", null, reverser));

        Set<String> allExcluded = new HashSet<>(Arrays.asList("hello", "world"));
        Assertions.assertEquals("hello world", Strings.mapWords("hello world", " ", allExcluded, String::toUpperCase));
    }

    @Test
    public void testConvertWords_WithDelimiterAndExcludedWords() {
        String result = Strings.mapWords("hello world", " ", Arrays.asList("world"), s -> s.toUpperCase());
        assertNotNull(result);
        assertTrue(result.contains("HELLO"));
        assertTrue(result.contains("world"));
    }

    @Test
    public void testConvertWords_EdgeCases() {
        assertNull(Strings.mapWords(null, String::toUpperCase));
        assertEquals("", Strings.mapWords("", String::toUpperCase));
        assertEquals("HELLO WORLD", Strings.mapWords("hello world", String::toUpperCase));
        assertEquals("HELLO-WORLD", Strings.mapWords("hello-world", "-", String::toUpperCase));
    }

    @Test
    public void testConvertWords_WithExcludedWords_FullCoverage() {
        // with excluded words
        List<String> excluded = Arrays.asList("of", "the");
        assertEquals("TOWER_of_LONDON_the_CITY", Strings.mapWords("tower_of_london_the_city", "_", excluded, String::toUpperCase));

        // null/empty string
        assertNull(Strings.mapWords(null, "_", excluded, String::toUpperCase));
        assertEquals("", Strings.mapWords("", "_", excluded, String::toUpperCase));

        // no excluded words
        assertEquals("HELLO_WORLD", Strings.mapWords("hello_world", "_", null, String::toUpperCase));

        // empty excluded words
        assertEquals("HELLO_WORLD", Strings.mapWords("hello_world", "_", Collections.emptyList(), String::toUpperCase));
    }

    @Test
    public void testQuoteEscaped() {
        assertEquals("\\\"hello\\\"", Strings.escapeQuotes("\"hello\""));
        assertEquals("abc", Strings.escapeQuotes("abc"));
        assertNull(Strings.escapeQuotes(null));
    }

    @Test
    public void testQuoteEscaped_WithQuoteChar() {
        assertEquals("\\'hello\\'", Strings.escapeQuotes("'hello'", '\''));
        assertEquals("abc", Strings.escapeQuotes("abc", '\''));
        assertNull(Strings.escapeQuotes(null, '\''));
    }

    @Test
    public void testQuoteEscapedWithChar() {
        assertEquals("ab\\\"c", Strings.escapeQuotes("ab\"c", '"'));
        assertEquals("ab'c", Strings.escapeQuotes("ab'c", '"')); // ' not escaped
        assertNull(Strings.escapeQuotes(null, '"'));
    }

    @Test
    public void testQuoteEscaped_WithCustomChar() {
        String result = Strings.escapeQuotes("hello 'world'", '\'');
        assertNotNull(result);
    }

    @Test
    public void testQuoteEscaped_FullCoverage() {
        // null/empty
        assertNull(Strings.escapeQuotes(null));
        assertEquals("", Strings.escapeQuotes(""));

        // no quotes
        assertEquals("Hello World", Strings.escapeQuotes("Hello World"));

        // single quote
        assertEquals("It\\'s a test", Strings.escapeQuotes("It's a test"));

        // double quote
        assertEquals("She said \\\"Hi\\\"", Strings.escapeQuotes("She said \"Hi\""));

        // already escaped (backslash before quote)
        assertEquals("Already \\'escaped\\'", Strings.escapeQuotes("Already \\'escaped\\'"));

        for (char quote : new char[] { '\'', '"' }) {
            for (int slashCount = 0; slashCount <= 4; slashCount++) {
                String input = "\\".repeat(slashCount) + quote;
                String expected = "\\".repeat(slashCount % 2 == 0 ? slashCount + 1 : slashCount) + quote;
                assertEquals(expected, Strings.escapeQuotes(input));
                assertEquals(expected, Strings.escapeQuotes(input, quote));
            }
        }

        // backslash at end of string (no next char to skip)
        assertEquals("test\\\\", Strings.escapeQuotes("test\\\\"));
    }

    @Test
    public void testQuoteEscaped_WithQuoteChar_FullCoverage() {
        // null/empty
        assertNull(Strings.escapeQuotes(null, '"'));
        assertEquals("", Strings.escapeQuotes("", '"'));

        // escape double quote
        assertEquals("She said \\\"Hi\\\"", Strings.escapeQuotes("She said \"Hi\"", '"'));

        // escape single quote
        assertEquals("It\\'s ok", Strings.escapeQuotes("It's ok", '\''));

        // already escaped - backslash then quote
        assertEquals("Already \\\"escaped\\\"", Strings.escapeQuotes("Already \\\"escaped\\\"", '"'));

        // no matching quote
        assertEquals("No quotes here", Strings.escapeQuotes("No quotes here", '"'));

        // Backslash is a supported target: odd runs are completed and even runs are already escaped.
        assertEquals("a\\\\b\\\\", Strings.escapeQuotes("a\\b\\", '\\'));
        assertEquals("a\\\\b", Strings.escapeQuotes("a\\\\b", '\\'));
    }

    @Test
    public void testUnicodeEscaped() {
        assertEquals("\\u0041", Strings.toUnicodeEscape('A'));
        assertEquals("\\u0061", Strings.toUnicodeEscape('a'));
        assertEquals("\\u0031", Strings.toUnicodeEscape('1'));
    }

    @Test
    public void testUnicodeEscaped_EdgeCases() {
        assertEquals("\\u0041", Strings.toUnicodeEscape('A'));
        assertEquals("\\u0000", Strings.toUnicodeEscape('\0'));
        assertEquals("\\u000a", Strings.toUnicodeEscape('\n'));
        assertEquals("\\u0009", Strings.toUnicodeEscape('\t'));
        // Multi-byte chars
        String escaped = Strings.toUnicodeEscape('\u00E9'); // e with acute
        assertTrue(escaped.startsWith("\\u"));
    }

    @Test
    public void testNormalizeSpace() {
        assertEquals("a b c", Strings.normalizeSpace("a  b   c"));
        assertEquals("abc", Strings.normalizeSpace("abc"));
        assertEquals("a b", Strings.normalizeSpace("  a  b  "));
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
    }

    @Test
    public void testNormalizeSpace_EdgeCases() {
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
        assertEquals("", Strings.normalizeSpace("   "));
        assertEquals("abc", Strings.normalizeSpace("abc"));
        assertEquals("abc", Strings.normalizeSpace("  abc  "));
        assertEquals("abc def", Strings.normalizeSpace("  abc    def  "));
        assertEquals("abc def", Strings.normalizeSpace("abc\n\tdef"));
        assertEquals("a b c", Strings.normalizeSpace("  a \t b \n c  "));
    }

    @Test
    public void testNormalizeSpace_UnicodeWhitespace() {
        final String nbsp = "\u00A0";
        assertEquals("a b", Strings.normalizeSpace(nbsp + nbsp + "a" + nbsp + nbsp + "b" + nbsp));
        assertEquals("", Strings.normalizeSpace(nbsp + nbsp));
        assertEquals("foo bar", Strings.normalizeSpace(nbsp + "foo" + nbsp + "bar" + nbsp));
        // Em space (U+2003) is both whitespace and a space separator
        assertEquals("a b", Strings.normalizeSpace("a\u2003\u2003b"));
        // Ordinary single spaces still collapse runs and trim ends
        assertEquals("x y", Strings.normalizeSpace("  x   y  "));
    }

    @Test
    public void testNullHandlingConsistency() {

        Assertions.assertArrayEquals(new String[0], Strings.split(null, ","));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", 5));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", true));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", 5, true));

        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ','));
        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ","));

        Strings.trimToNullEach(null);
        Strings.trimToEmptyEach(null);
        Strings.stripToNullEach(null);
        Strings.stripToEmptyEach(null);
        Strings.removeWhitespaceEach(null);

    }

    @Test
    public void testTrimEach() {
        String[] array = { "  a  ", " b ", "c" };
        Strings.trimEach(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testTrim() {
        assertEquals("abc", Strings.trim("  abc  "));
        assertEquals("abc", Strings.trim("abc"));
        assertEquals("", Strings.trim("   "));
        assertNull(Strings.trim(null));
    }

    @Test
    public void testTrimEach_WithNullElement() {
        String[] arr = { "  a  ", null, "b" };
        Strings.trimEach(arr);
        assertArrayEquals(new String[] { "a", null, "b" }, arr);
    }

    @Test
    public void testTrim_EdgeCases() {
        assertNull(Strings.trim(null));
        assertEquals("", Strings.trim(""));
        assertEquals("", Strings.trim("   "));
        assertEquals("abc", Strings.trim("  abc  "));
        assertEquals("abc", Strings.trim("abc"));
    }

    @Test
    public void testTrimEach_EdgeCases() {
        String[] array = { "  abc  ", " xyz ", null };
        Strings.trimEach(array);
        assertEquals("abc", array[0]);
        assertEquals("xyz", array[1]);
        assertNull(array[2]);
    }

    @Test
    public void testTrimToNull() {
        assertEquals("abc", Strings.trimToNull("  abc  "));
        assertNull(Strings.trimToNull("   "));
        assertNull(Strings.trimToNull(null));
    }

    @Test
    public void testTrimToNullEach() {
        String[] array = { "  a  ", "   ", "c" };
        Strings.trimToNullEach(array);
        assertArrayEquals(new String[] { "a", null, "c" }, array);
    }

    @Test
    public void testEachMethodsWithSingleElement() {
        String[] single = { "  test  " };
        Strings.trimToNullEach(single);
        Assertions.assertEquals("test", single[0]);

        String[] single2 = { "  " };
        Strings.trimToNullEach(single2);
        Assertions.assertNull(single2[0]);

        String[] single3 = { "xyztest" };
        Strings.stripStartEach(single3, "xyz");
        Assertions.assertEquals("test", single3[0]);
    }

    @Test
    public void testTrimToNull_EdgeCases() {
        assertNull(Strings.trimToNull(null));
        assertNull(Strings.trimToNull(""));
        assertNull(Strings.trimToNull("   "));
        assertEquals("abc", Strings.trimToNull("  abc  "));
    }

    @Test
    public void testTrimToNullEach_EdgeCases() {
        String[] array = { "  abc  ", "   ", null };
        Strings.trimToNullEach(array);
        assertEquals("abc", array[0]);
        assertNull(array[1]);
        assertNull(array[2]);
    }

    @Test
    public void testTrimToEmpty() {
        assertEquals("abc", Strings.trimToEmpty("  abc  "));
        assertEquals("", Strings.trimToEmpty("   "));
        assertEquals("", Strings.trimToEmpty(null));
    }

    @Test
    public void testTrimToEmptyEach() {
        String[] array = { "  a  ", "   ", null };
        Strings.trimToEmptyEach(array);
        assertArrayEquals(new String[] { "a", "", "" }, array);
    }

    @Test
    public void testTrimToEmpty_EdgeCases() {
        assertEquals("", Strings.trimToEmpty(null));
        assertEquals("", Strings.trimToEmpty(""));
        assertEquals("", Strings.trimToEmpty("   "));
        assertEquals("abc", Strings.trimToEmpty("  abc  "));
    }

    @Test
    public void testTrimToEmptyEach_EdgeCases() {
        String[] array = { "  abc  ", null, "   " };
        Strings.trimToEmptyEach(array);
        assertEquals("abc", array[0]);
        assertEquals("", array[1]);
        assertEquals("", array[2]);
    }

    @Test
    public void testChompEach() {
        String[] array = { "a\n", "b\r\n", "c" };
        Strings.chompEach(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testChomp() {
        assertEquals("abc", Strings.chomp("abc\n"));
        assertEquals("abc", Strings.chomp("abc\r\n"));
        assertEquals("abc", Strings.chomp("abc\r"));
        assertEquals("abc", Strings.chomp("abc"));
        assertNull(Strings.chomp(null));
    }

    @Test
    public void testChomp_EdgeCases() {
        assertNull(Strings.chomp(null));
        assertEquals("", Strings.chomp(""));
        assertEquals("abc", Strings.chomp("abc\r"));
        assertEquals("abc", Strings.chomp("abc\n"));
        assertEquals("abc", Strings.chomp("abc\r\n"));
        assertEquals("abc\r\n", Strings.chomp("abc\r\n\r\n"));
        assertEquals("abc", Strings.chomp("abc"));
    }

    @Test
    public void testChompEach_EdgeCases() {
        String[] array = { "abc\n", "def\r\n", "ghi" };
        Strings.chompEach(array);
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
        assertEquals("ghi", array[2]);
    }

    @Test
    public void testChomp_FullCoverage() {
        // null
        assertNull(Strings.chomp(null));

        // empty
        assertEquals("", Strings.chomp(""));

        // single char \r
        assertEquals("", Strings.chomp("\r"));

        // single char \n
        assertEquals("", Strings.chomp("\n"));

        // single char non-newline
        assertEquals("a", Strings.chomp("a"));

        // ends with \r\n
        assertEquals("abc", Strings.chomp("abc\r\n"));

        // ends with \n
        assertEquals("abc", Strings.chomp("abc\n"));

        // ends with \r
        assertEquals("abc", Strings.chomp("abc\r"));

        // ends with non-newline (lastIdx++)
        assertEquals("abc", Strings.chomp("abc"));

        // \r\n\r\n - only removes last pair
        assertEquals("abc\r\n", Strings.chomp("abc\r\n\r\n"));

        // \n\r - removes only \r (last char)
        assertEquals("abc\n", Strings.chomp("abc\n\r"));
    }

    @Test
    public void testChopEach() {
        String[] array = { "abc", "xy", "z" };
        Strings.chopEach(array);
        assertArrayEquals(new String[] { "ab", "x", "" }, array);
    }

    @Test
    public void testChop() {
        assertEquals("ab", Strings.chop("abc"));
        assertEquals("abc", Strings.chop("abc\r\n"));
        assertEquals("", Strings.chop("a"));
        assertEquals("", Strings.chop(""));
        assertNull(Strings.chop(null));
    }

    @Test
    public void testChop_EdgeCases() {
        assertNull(Strings.chop(null));
        assertEquals("", Strings.chop(""));
        assertEquals("ab", Strings.chop("abc"));
        assertEquals("abc", Strings.chop("abc\r\n"));
        assertEquals("abc\n", Strings.chop("abc\n\r"));
        assertEquals("", Strings.chop("a"));
    }

    @Test
    public void testChop_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals("", Strings.chop(emoji));
        assertEquals("a", Strings.chop("a" + emoji));
    }

    @Test
    public void testChopEach_EdgeCases() {
        String[] array = { "abc", "de", "f" };
        Strings.chopEach(array);
        assertEquals("ab", array[0]);
        assertEquals("d", array[1]);
        assertEquals("", array[2]);
    }

    @Test
    public void testTruncate_WithOffset() {
        assertEquals("cde", Strings.truncate("abcdef", 2, 3));
        assertEquals("abc", Strings.truncate("abc", 0, 5));
    }

    @Test
    public void testTruncateEach() {
        String[] array = { "abcdef", "xyz" };
        Strings.truncateEach(array, 3);
        assertArrayEquals(new String[] { "abc", "xyz" }, array);
    }

    @Test
    public void testTruncateEach_WithOffset() {
        String[] array = { "abcdef", "xyz" };
        Strings.truncateEach(array, 1, 3);
        assertArrayEquals(new String[] { "bcd", "yz" }, array);
    }

    @Test
    public void testTruncate() {
        assertEquals("abc", Strings.truncate("abcdef", 3));
        assertEquals("abc", Strings.truncate("abc", 5));
        assertNull(Strings.truncate(null, 3));
    }

    @Test
    public void testTruncateWithOffset() {
        assertEquals("cde", Strings.truncate("abcdefg", 2, 3));
        assertEquals("", Strings.truncate("abc", 3, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", -1, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", 0, -1));
    }

    @Test
    public void testTruncate_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals("", Strings.truncate(emoji + "a", 1));
        assertEquals(emoji, Strings.truncate(emoji + "a", 2));
        assertEquals("ab", Strings.truncate(emoji + "abc", 1, 2));
    }

    @Test
    public void testDeleteWhitespace() {
        assertEquals("abc", Strings.removeWhitespace("a b c"));
        assertEquals("abc", Strings.removeWhitespace("  a  b  c  "));
        assertEquals("abc", Strings.removeWhitespace("abc"));
        assertNull(Strings.removeWhitespace(null));
    }

    @Test
    public void testAppendIfMissing() {
        assertEquals("abc.txt", Strings.appendIfMissing("abc", ".txt"));
        assertEquals("abc.txt", Strings.appendIfMissing("abc.txt", ".txt"));
        assertNull(Strings.appendIfMissing(null, ".txt"));
    }

    @Test
    public void testAppendIfMissing_EmptySuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.appendIfMissing("abc", ""));
    }

    @Test
    public void testAppendIfMissingIgnoreCase() {
        assertEquals("abc.txt", Strings.appendIfMissingIgnoreCase("abc", ".txt"));
        assertEquals("abc.TXT", Strings.appendIfMissingIgnoreCase("abc.TXT", ".txt"));
        assertNull(Strings.appendIfMissingIgnoreCase(null, ".txt"));
    }

    @Test
    public void testAppendPrependCaseSensitivity() {
        Assertions.assertEquals("File.TXT", Strings.appendIfMissingIgnoreCase("File", ".TXT"));
        Assertions.assertEquals("File.txt", Strings.appendIfMissingIgnoreCase("File.txt", ".TXT"));
        Assertions.assertEquals("File.TxT", Strings.appendIfMissingIgnoreCase("File.TxT", ".txt"));

        Assertions.assertEquals("HTTP://site.com", Strings.prependIfMissingIgnoreCase("site.com", "HTTP://"));
        Assertions.assertEquals("HtTp://site.com", Strings.prependIfMissingIgnoreCase("HtTp://site.com", "http://"));
    }

    @Test
    public void testPrependIfMissing() {
        assertEquals("http://abc", Strings.prependIfMissing("abc", "http://"));
        assertEquals("http://abc", Strings.prependIfMissing("http://abc", "http://"));
        assertNull(Strings.prependIfMissing(null, "http://"));
    }

    @Test
    public void testPrependIfMissingIgnoreCase() {
        assertEquals("http://abc", Strings.prependIfMissingIgnoreCase("abc", "http://"));
        assertEquals("HTTP://abc", Strings.prependIfMissingIgnoreCase("HTTP://abc", "http://"));
        assertNull(Strings.prependIfMissingIgnoreCase(null, "http://"));
    }

    @Test
    public void testWrapIfMissing() {
        assertEquals("'abc'", Strings.wrapIfMissing("abc", "'"));
        assertEquals("'abc'", Strings.wrapIfMissing("'abc'", "'"));
        assertNull(Strings.wrapIfMissing(null, "'"));
    }

    @Test
    public void testWrapIfMissing_DifferentPrefixSuffix() {
        assertEquals("<abc>", Strings.wrapIfMissing("abc", "<", ">"));
        assertEquals("<abc>", Strings.wrapIfMissing("<abc>", "<", ">"));
        assertNull(Strings.wrapIfMissing(null, "<", ">"));
    }

    @Test
    public void testWrapIfMissing_EdgeCases() {
        assertEquals("\"abc\"", Strings.wrapIfMissing("abc", "\""));
        assertEquals("\"abc\"", Strings.wrapIfMissing("\"abc\"", "\""));
        assertEquals("[abc]", Strings.wrapIfMissing("abc", "[", "]"));
        assertEquals("[abc]", Strings.wrapIfMissing("[abc]", "[", "]"));
        assertNull(Strings.wrapIfMissing(null, "\""));
        assertNull(Strings.wrapIfMissing(null, "[", "]"));
    }

    @Test
    public void testWrapIfMissing_WithPrefixSuffix() {
        // only starts with prefix, missing suffix -> add suffix
        assertEquals("[hello]", Strings.wrapIfMissing("[hello", "[", "]"));

        // already has both prefix and suffix -> return as-is
        assertEquals("[hello]", Strings.wrapIfMissing("[hello]", "[", "]"));

        // only ends with suffix, missing prefix -> add prefix
        assertEquals("[hello]", Strings.wrapIfMissing("hello]", "[", "]"));

        // neither prefix nor suffix -> add both
        assertEquals("[hello]", Strings.wrapIfMissing("hello", "[", "]"));

        // null/empty str
        assertNull(Strings.wrapIfMissing(null, "[", "]"));
        assertEquals("[]", Strings.wrapIfMissing("", "[", "]"));
    }

    @Test
    public void testWrap() {
        assertEquals("'abc'", Strings.wrap("abc", "'"));
        assertEquals("''abc''", Strings.wrap("'abc'", "'"));
        assertNull(Strings.wrap(null, "'"));
    }

    @Test
    public void testWrap_DifferentPrefixSuffix() {
        assertEquals("<abc>", Strings.wrap("abc", "<", ">"));
        assertEquals("<<abc>>", Strings.wrap("<abc>", "<", ">"));
        assertNull(Strings.wrap(null, "<", ">"));
    }

    @Test
    public void testWrap_EdgeCases() {
        assertEquals("\"abc\"", Strings.wrap("abc", "\""));
        assertEquals("[abc]", Strings.wrap("abc", "[", "]"));
        assertNull(Strings.wrap(null, "\""));
        assertNull(Strings.wrap(null, "[", "]"));
    }

    @Test
    public void test_unwrap() {
        // prefix and suffix overlap, so the string is not unwrapped
        assertEquals("ababa", Strings.unwrap("ababa", "aba"));
    }

    @Test
    public void testUnwrap() {
        assertEquals("abc", Strings.unwrap("'abc'", "'"));
        assertEquals("abc", Strings.unwrap("abc", "'"));
        assertNull(Strings.unwrap(null, "'"));
    }

    @Test
    public void testUnwrap_DifferentPrefixSuffix() {
        assertEquals("abc", Strings.unwrap("<abc>", "<", ">"));
        assertEquals("abc", Strings.unwrap("abc", "<", ">"));
        assertNull(Strings.unwrap(null, "<", ">"));
    }

    @Test
    public void testUnwrap_EdgeCases() {
        assertEquals("abc", Strings.unwrap("\"abc\"", "\""));
        assertEquals("abc", Strings.unwrap("[abc]", "[", "]"));
        assertEquals("abc", Strings.unwrap("abc", "\""));
        assertNull(Strings.unwrap(null, "\""));
    }

    @Test
    public void testAnyCandidateFamilies_CandidatePriority() {
        assertEquals(3, Strings.indexOfAny("a:b.c", '.', ':'));
        assertEquals(3, Strings.indexOfAny("a:b.c", ".", ":"));
        assertEquals(1, Strings.lastIndexOfAny("a.b:c", '.', ':'));
        assertEquals(1, Strings.lastIndexOfAny("a.b:c", ".", ":"));

        assertEquals(1, Strings.minIndexOfAll("a:b.c", '.', ':'));
        assertEquals(3, Strings.maxIndexOfAll("a:b.c", '.', ':'));
        assertEquals(1, Strings.minLastIndexOfAll("a.b:c", '.', ':'));
        assertEquals(3, Strings.maxLastIndexOfAll("a.b:c", '.', ':'));
        assertEquals(2, Strings.minIndexOfAll("hello", 'o', 'l'));
        assertEquals(4, Strings.indexOfAny("hello", 'o', 'l'));
        assertEquals(2, Strings.indexOfAny("hello", 'l', 'o'));
        assertEquals(4, Strings.maxIndexOfAll("hello", 'o', 'l'));
        assertEquals(3, Strings.minLastIndexOfAll("hello", 'o', 'l'));
        assertEquals(4, Strings.maxLastIndexOfAll("hello", 'o', 'l'));
        assertEquals(2, Strings.minIndexOfAll("hello", 1, 'o', 'l'));
        assertEquals(3, Strings.minLastIndexOfAll("hello", 3, 'o', 'l'));
        assertEquals(-1, Strings.minIndexOfAll("hello", 'x', 'y'));
        assertEquals(-1, Strings.maxLastIndexOfAll((String) null, 'a', 'b'));

        assertEquals("c", Strings.substringAfterAny("a:b.c", '.', ':'));
        assertEquals("c", Strings.substringAfterAny("a:b.c", ".", ":"));
        assertEquals("a:b", Strings.substringBeforeAny("a:b.c", '.', ':'));
        assertEquals("a:b", Strings.substringBeforeAny("a:b.c", ".", ":"));
    }

    @Test
    public void testEmptySearchStringClampsExplicitIndexToLength() {
        assertEquals(3, Strings.indexOf("abc", "", 3));
        assertEquals(3, Strings.indexOf("abc", "", 4));
        assertEquals(0, Strings.indexOf("", "", 1));

        assertEquals(3, Strings.indexOfIgnoreCase("abc", "", 4));
        assertEquals(0, Strings.indexOfIgnoreCase("", "", 1));

        assertEquals(3, Strings.lastIndexOf("abc", "", 3));
        assertEquals(3, Strings.lastIndexOf("abc", "", 4));
        assertEquals(0, Strings.lastIndexOf("", "", 0));

        assertEquals(3, Strings.lastIndexOfIgnoreCase("abc", "", 4));
        assertEquals(0, Strings.lastIndexOfIgnoreCase("", "", 0));

        assertEquals(3, Strings.minLastIndexOfAll("abc", 4, ""));
        assertEquals(3, Strings.maxLastIndexOfAll("abc", 4, ""));
        assertEquals(3, Strings.maxLastIndexOfAll("abc", ""));
    }

    @Test
    public void testAdaptiveCharacterSearches_MatchReferenceContracts() {
        final String emoji = "\uD83D\uDE00";
        final String str = "a".repeat(700) + "q" + "b".repeat(700) + "x" + emoji + "z" + "a".repeat(700);
        final char[] values = { 'x', 'q', '\uFFFF', '\uD800', 'z', '\0', 'm', 'n', 'x', '\uDC00', 'a', 'b' };
        final int[] forwardStarts = { -10, 0, 699, 700, 701, 1400, str.length() - 1, str.length(), str.length() + 1 };
        final int[] backwardStarts = { -1, 0, 700, 1400, str.length() - 2, str.length() - 1, str.length(), str.length() + 10 };

        for (final int fromIndex : forwardStarts) {
            assertEquals(indexOfAnyReference(str, fromIndex, values), Strings.indexOfAny(str, fromIndex, values));
            assertEquals(minIndexOfAllReference(str, fromIndex, values), Strings.minIndexOfAll(str, fromIndex, values));
        }

        for (final int startIndexFromBack : backwardStarts) {
            assertEquals(lastIndexOfAnyReference(str, startIndexFromBack, values), Strings.lastIndexOfAny(str, startIndexFromBack, values));
            assertEquals(maxLastIndexOfAllReference(str, startIndexFromBack, values), Strings.maxLastIndexOfAll(str, startIndexFromBack, values));
        }

        final char[] mostlyAbsentValues = new char[256];

        for (int i = 0; i < mostlyAbsentValues.length - 1; i++) {
            mostlyAbsentValues[i] = (char) (0x0100 + i);
        }

        mostlyAbsentValues[mostlyAbsentValues.length - 1] = 'z';
        assertEquals(str.indexOf('z'), Strings.indexOfAny(str, mostlyAbsentValues));
        assertEquals(str.indexOf('z'), Strings.minIndexOfAll(str, mostlyAbsentValues));
        assertEquals(str.lastIndexOf('z'), Strings.lastIndexOfAny(str, mostlyAbsentValues));
        assertEquals(str.lastIndexOf('z'), Strings.maxLastIndexOfAll(str, mostlyAbsentValues));

        final Random random = new Random(0x5EAC4L);

        for (int testCase = 0; testCase < 100; testCase++) {
            final char[] haystackChars = new char[512 + random.nextInt(256)];
            final char[] candidateChars = new char[8 + random.nextInt(56)];

            for (int i = 0; i < haystackChars.length; i++) {
                haystackChars[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
            }

            for (int i = 0; i < candidateChars.length; i++) {
                candidateChars[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
            }

            candidateChars[random.nextInt(candidateChars.length)] = haystackChars[random.nextInt(haystackChars.length)];

            final String haystack = new String(haystackChars);
            final int fromIndex = random.nextInt(33) - 16;
            final int startIndexFromBack = haystack.length() - 1 - random.nextInt(16);

            assertEquals(indexOfAnyReference(haystack, fromIndex, candidateChars), Strings.indexOfAny(haystack, fromIndex, candidateChars));
            assertEquals(minIndexOfAllReference(haystack, fromIndex, candidateChars), Strings.minIndexOfAll(haystack, fromIndex, candidateChars));
            assertEquals(lastIndexOfAnyReference(haystack, startIndexFromBack, candidateChars),
                    Strings.lastIndexOfAny(haystack, startIndexFromBack, candidateChars));
            assertEquals(maxLastIndexOfAllReference(haystack, startIndexFromBack, candidateChars),
                    Strings.maxLastIndexOfAll(haystack, startIndexFromBack, candidateChars));
        }
    }

    @Test
    public void testPerformanceOptimized() {
        String str = "abc";

        assertEquals(0, Strings.indexOfIgnoreCase("ABC", "abc"));
        assertEquals(0, Strings.indexOfIgnoreCase("ABC", "ABC"));
    }

    @Test
    public void testOrdinalIndexOf() {
        assertEquals(0, Strings.ordinalIndexOf("", "", 1));
        assertEquals(-1, Strings.ordinalIndexOf("", "", 2));
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertEquals(5, Strings.ordinalIndexOf("aabaabaa", "b", 2));
        assertEquals(4, Strings.ordinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(0, Strings.ordinalIndexOf("abcabc", "abc", 1));
        assertEquals(3, Strings.ordinalIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.ordinalIndexOf("abcabc", "abc", 3));
        assertEquals(-1, Strings.ordinalIndexOf(null, "abc", 1));
        assertEquals(0, Strings.ordinalIndexOf("abc", "", 1));
        assertEquals(1, Strings.ordinalIndexOf("abc", "", 2));
        assertEquals(2, Strings.ordinalIndexOf("abc", "", 3));
        // Empty needle matches at every UTF-16 code-unit boundary, including length.
        assertEquals(3, Strings.ordinalIndexOf("abc", "", 4));
        assertEquals(-1, Strings.ordinalIndexOf("abc", "", 5));
        assertEquals(3, Strings.lastOrdinalIndexOf("abc", "", 1));
        assertEquals(2, Strings.lastOrdinalIndexOf("abc", "", 2));
        assertEquals(0, Strings.lastOrdinalIndexOf("abc", "", 4));
        assertEquals(-1, Strings.lastOrdinalIndexOf("abc", "", 5));
        assertEquals(0, Strings.countMatches("abc", ""));
        assertEquals(-1, Strings.ordinalIndexOf("", null, 1));
        assertEquals(-1, Strings.ordinalIndexOf(null, "", 1));
        assertEquals(-1, Strings.ordinalIndexOf(null, null, 1));
    }

    @Test
    public void testEmptyNeedleUsesUtf16CodeUnitBoundaries() {
        final String emoji = new String(Character.toChars(0x1F600));

        assertEquals(0, Strings.ordinalIndexOf(emoji, "", 1));
        assertEquals(1, Strings.ordinalIndexOf(emoji, "", 2));
        assertEquals(2, Strings.ordinalIndexOf(emoji, "", 3));
        assertEquals(-1, Strings.ordinalIndexOf(emoji, "", 4));

        assertEquals(2, Strings.lastOrdinalIndexOf(emoji, "", 1));
        assertEquals(1, Strings.lastOrdinalIndexOf(emoji, "", 2));
        assertEquals(0, Strings.lastOrdinalIndexOf(emoji, "", 3));
        assertEquals(-1, Strings.lastOrdinalIndexOf(emoji, "", 4));

        assertArrayEquals(new int[] { 0, 1 }, Strings.indicesOf(emoji, "").toArray());
        assertArrayEquals(new int[] { 0, 1 }, Strings.indicesOfIgnoreCase(emoji, "").toArray());
    }

    @Test
    public void testOrdinalIndexOf_EdgeCases() {
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(1, Strings.ordinalIndexOf("aabaabaa", "a", 2));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertEquals(-1, Strings.ordinalIndexOf("aabaabaa", "a", 10));
        assertEquals(-1, Strings.ordinalIndexOf(null, "a", 1));
        assertEquals(-1, Strings.ordinalIndexOf("abc", null, 1));
    }

    @Test
    public void testminIndexOfAll() {
        assertEquals(0, Strings.minIndexOfAll("abcdefg", "abc", "efg"));
        assertEquals(3, Strings.minIndexOfAll("abcdefg", "def", "efg"));
        assertEquals(-1, Strings.minIndexOfAll("abc", "xyz", "123"));
    }

    @Test
    public void testminIndexOfAll_WithFromIndex() {
        assertEquals(3, Strings.minIndexOfAll("abcdefg", 2, "def", "efg"));
        assertEquals(-1, Strings.minIndexOfAll("abc", 5, "abc"));
    }

    @Test
    public void testMinIndexOfAll() {
        assertEquals(0, Strings.minIndexOfAll("hello world", "hello", "world"));
        assertEquals(-1, Strings.minIndexOfAll("hello world", "xyz", "abc"));
        assertEquals(-1, Strings.minIndexOfAll(null, "a"));
    }

    @Test
    public void testMinIndexOfAll_WithFromIndex() {
        int result = Strings.minIndexOfAll("hello world", 1, "ello", "world");
        assertEquals(1, result);
    }

    @Test
    public void testmaxIndexOfAll() {
        assertEquals(4, Strings.maxIndexOfAll("abcdefg", "abc", "efg"));
        assertEquals(3, Strings.maxIndexOfAll("abcdefg", "def", "abc"));
        assertEquals(-1, Strings.maxIndexOfAll("abc", "xyz", "123"));
    }

    @Test
    public void testmaxIndexOfAll_WithFromIndex() {
        assertEquals(4, Strings.maxIndexOfAll("abcdefg", 2, "def", "efg"));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 5, "abc"));
    }

    @Test
    public void testMaxIndexOfAll() {
        assertEquals(6, Strings.maxIndexOfAll("hello world", "hello", "world"));
        assertEquals(-1, Strings.maxIndexOfAll("hello world", "xyz", "abc"));
    }

    @Test
    public void testMaxIndexOfAll_WithFromIndex() {
        int result = Strings.maxIndexOfAll("hello world", 1, "ello", "world");
        assertEquals(6, result);
    }

    @Test
    public void testminLastIndexOfAll() {
        assertEquals(3, Strings.minLastIndexOfAll("abcdefabc", "abc", "def"));
        assertEquals(6, Strings.minLastIndexOfAll("abcabcabc", "abc"));
    }

    @Test
    public void testminLastIndexOfAllWithIndex() {
        Assertions.assertEquals(6, Strings.minLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(4, Strings.minLastIndexOfAll("Hello World", 5, "o", "World"));
        Assertions.assertEquals(-1, Strings.minLastIndexOfAll("Hello World", 3, "o", "World"));

        Assertions.assertEquals(-1, Strings.minLastIndexOfAll("test", -1, "test"));

        Assertions.assertEquals(0, Strings.minLastIndexOfAll("test", 100, "test"));
    }

    @Test
    public void testMinLastIndexOfAll() {
        int result = Strings.minLastIndexOfAll("hello hello world", "hello", "world");
        assertTrue(result >= 0);
        assertEquals(-1, Strings.minLastIndexOfAll(null, "a"));
    }

    @Test
    public void testmaxLastIndexOfAll() {
        assertEquals(6, Strings.maxLastIndexOfAll("abcabcabc", "abc"));
        assertEquals(6, Strings.maxLastIndexOfAll("abcdefabc", "abc", "def"));
    }

    @Test
    public void testmaxLastIndexOfAllWithIndex() {
        Assertions.assertEquals(7, Strings.maxLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(6, Strings.maxLastIndexOfAll("Hello World", 6, "o", "World"));
        Assertions.assertEquals(0, Strings.maxLastIndexOfAll("Hello World", 3, "o", "H"));

        Assertions.assertEquals(-1, Strings.maxLastIndexOfAll("test", -1, "test"));

        Assertions.assertEquals(0, Strings.maxLastIndexOfAll("test", 100, "test"));
    }

    @Test
    public void testMaxLastIndexOfAll() {
        int result = Strings.maxLastIndexOfAll("hello hello world", "hello", "world");
        assertTrue(result >= 0);
        assertEquals(-1, Strings.maxLastIndexOfAll(null, "a"));
    }

    @Test
    public void testIndicesOf() {
        int[] indices = Strings.indicesOf("abcabc", "abc").toArray();
        assertArrayEquals(new int[] { 0, 3 }, indices);
        int[] indices2 = Strings.indicesOf("abc", "xyz").toArray();
        assertArrayEquals(new int[0], indices2);
    }

    @Test
    public void test_indicesOf() {
        {
            String ret = Strings.indicesOf("abca", "a").join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOf("abca", "a", 0).join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOf("abca", "a", 1).join(", ");
            assertEquals("3", ret);
        }
        {
            String ret = Strings.indicesOf("abcA", "a").join(", ");
            assertEquals("0", ret);

            ret = Strings.indicesOf("abcA", "a", 0).join(", ");
            assertEquals("0", ret);

            ret = Strings.indicesOf("abcA", "a", 1).join(", ");
            assertEquals("", ret);
        }

        {
            String ret = Strings.indicesOfIgnoreCase("abca", "a").join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abca", "a", 0).join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abca", "a", 1).join(", ");
            assertEquals("3", ret);
        }
        {
            String ret = Strings.indicesOfIgnoreCase("abcA", "a").join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abcA", "a", 0).join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abcA", "a", 1).join(", ");
            assertEquals("3", ret);
        }

        {
            String ret = Strings.indicesOf("abca", "").join(", ");
            assertEquals("0, 1, 2, 3", ret);

            ret = Strings.indicesOf("abca", null).join(", ");
            assertEquals("", ret);

            ret = Strings.indicesOf(null, "").join(", ");
            assertEquals("", ret);

            ret = Strings.indicesOf("", null).join(", ");
            assertEquals("", ret);
        }

        {
            String ret = RegExUtil.matchIndices("sent email to xyz@gmail.com from 123@outlook.cn", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(", "));
            assertEquals("14, 33", ret);

            ret = RegExUtil.matchResults("sent email to xyz@gmail.com from 123@outlook.cn", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER)
                    .map(MatchResult::group)
                    .collect(Collectors.joining(", "));
            assertEquals("xyz@gmail.com, 123@outlook.cn", ret);
        }

    }

    @Test
    public void testIndicesOf_WithFromIndex() {
        int[] indices = Strings.indicesOf("abcabc", "abc", 1).toArray();
        assertNotNull(indices);
        assertEquals(1, indices.length);
        assertEquals(3, indices[0]);
    }

    @Test
    public void testIndicesOfIgnoreCase() {
        {
            int[] indices = Strings.indicesOfIgnoreCase("AbCaBc", "abc").toArray();
            assertArrayEquals(new int[] { 0, 3 }, indices);
            int[] indices2 = Strings.indicesOfIgnoreCase("abc", "xyz").toArray();
            assertArrayEquals(new int[0], indices2);

            int[] indices3 = Strings.indicesOfIgnoreCase("abc", "").toArray();
            assertArrayEquals(new int[] { 0, 1, 2 }, indices3);

            int[] indices4 = Strings.indicesOfIgnoreCase("abc", null).toArray();
            assertEquals(0, indices4.length);
        }

        {
            int[] indices = Strings.indicesOfIgnoreCase("AbCaBc", "abc", 1).toArray();
            assertArrayEquals(new int[] { 3 }, indices);
            int[] indices2 = Strings.indicesOfIgnoreCase("abc", "xyz", 1).toArray();
            assertArrayEquals(new int[0], indices2);

            int[] indices3 = Strings.indicesOfIgnoreCase("abc", "", 1).toArray();
            assertArrayEquals(new int[] { 1, 2 }, indices3);

            int[] indices4 = Strings.indicesOfIgnoreCase("abc", null, 1).toArray();
            assertEquals(0, indices4.length);
        }
    }

    @Test
    public void testIndicesOfIgnoreCase_WithFromIndex() {
        int[] indices = Strings.indicesOfIgnoreCase("aAbAa", "a", 2).toArray();
        assertNotNull(indices);
        assertTrue(indices.length >= 1);
    }

    @Test
    public void testCountMatches_Char() {
        assertEquals(2, Strings.countMatches("abcabc", 'a'));
        assertEquals(2, Strings.countMatches("abcabc", 'c'));
        assertEquals(0, Strings.countMatches("abc", 'x'));
        assertEquals(0, Strings.countMatches(null, 'a'));
    }

    @Test
    public void testCountMatches_String() {
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(2, Strings.countMatches("aaaa", "aa"));
        assertEquals(0, Strings.countMatches("abc", "xyz"));
        assertEquals(0, Strings.countMatches(null, "abc"));
    }

    @Test
    public void testCountMatchesChar() {
        assertEquals(0, Strings.countMatches(null, 'a'));
        assertEquals(0, Strings.countMatches("", 'a'));
        assertEquals(6, Strings.countMatches("aabaabaa", 'a'));
        assertEquals(2, Strings.countMatches("aabaabaa", 'b'));
    }

    @Test
    public void testCountMatchesString() {
        assertEquals(0, Strings.countMatches(null, "a"));
        assertEquals(0, Strings.countMatches("", "a"));
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(1, Strings.countMatches("abcdef", "def"));
    }

    @Test
    public void testCountMatches_CharEdgeCases() {
        assertEquals(0, Strings.countMatches(null, 'a'));
        assertEquals(0, Strings.countMatches("", 'a'));
        assertEquals(2, Strings.countMatches("abcabc", 'a'));
        assertEquals(0, Strings.countMatches("abc", 'x'));
    }

    @Test
    public void testCountMatches_StringEdgeCases() {
        assertEquals(0, Strings.countMatches(null, "a"));
        assertEquals(0, Strings.countMatches("", "a"));
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(0, Strings.countMatches("abc", "xyz"));
        assertEquals(0, Strings.countMatches("abc", null));
        assertEquals(0, Strings.countMatches("abc", ""));
    }

    @Test
    public void testCountMatches_CodePointVersusChar() {
        assertEquals(3, Strings.countMatches("hello world", (int) 'l'));
        assertEquals(Strings.countMatches("hello world", 'l'), Strings.countMatches("hello world", (int) 'l'));

        final String twoFaces = "a😀😀";
        N.println(twoFaces);
        assertEquals(2, Strings.countMatches(twoFaces, 0x1F600));
        assertEquals(2, Strings.countMatches(twoFaces, '\uD83D'));
        assertEquals(2, Strings.countMatches(twoFaces, '\uDE00'));
        assertEquals(0, Strings.countMatches(twoFaces, 0x1F44D));

        assertEquals(0, Strings.countMatches(null, 0x1F600));
        assertEquals(0, Strings.countMatches("", 0x1F600));
        assertEquals(0, Strings.countMatches("abc", -1));
        assertEquals(0, Strings.countMatches("abc", Character.MAX_CODE_POINT + 1));
    }

    @Test
    public void testDelimiterMethods() {
        assertTrue(StrUtil.containsToken("a,b,c", "b", ","));
        assertFalse(StrUtil.containsToken("abc", "b", ","));
        assertFalse(StrUtil.containsToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(2, StrUtil.indexOfToken("a,b,c", "b", ","));
        assertEquals(-1, StrUtil.indexOfToken("abc", "b", ","));
    }

    @Test
    public void testStrUtilEmptyNeedleWithOversizedSearchIndex() {
        // C-538: an empty or null delimiter is rejected; the plain search is Strings.indexOf("ab", "", 10) == 2.
        assertEquals(2, Strings.indexOf("ab", "", 10));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfToken("ab", "", "", 10));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfTokenIgnoreCase("ab", "", null, 10));

        // Delimited search uses the same clamp, then still requires the position to be an empty field.
        assertEquals(3, StrUtil.indexOfToken("ab,", "", ",", 10));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("ab,", "", ",", 10));
        assertEquals(0, StrUtil.indexOfToken("", "", ",", 10));
        assertEquals(-1, StrUtil.indexOfToken("ab", "", ",", 10));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("ab", "", ",", 10));

        // Backward searches already clamp oversized indices and follow the same empty-field rule.
        assertEquals(3, StrUtil.lastIndexOfToken("ab,", "", ",", 10));
        assertEquals(3, StrUtil.lastIndexOfTokenIgnoreCase("ab,", "", ",", 10));
        assertEquals(-1, StrUtil.lastIndexOfToken("ab", "", ",", 10));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("ab", "", ",", 10));
    }

    @Test
    public void test_whilespace() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("abc\n"));
        assertTrue(Strings.containsWhitespace("abc\t"));
    }

    @Test
    public void testLengthOfCommonPrefix() {
        assertEquals(3, Strings.lengthOfCommonPrefix("abcdef", "abcxyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix(null, "abc"));
    }

    @Test
    public void testLengthOfCommonPrefix_EdgeCases() {
        assertEquals(0, Strings.lengthOfCommonPrefix(null, null));
        assertEquals(0, Strings.lengthOfCommonPrefix(null, "abc"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", null));
        assertEquals(3, Strings.lengthOfCommonPrefix("abc", "abcdef"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", "xyz"));
        assertEquals(2, Strings.lengthOfCommonPrefix("abc", "abx"));
    }

    @Test
    public void testLengthOfCommonSuffix() {
        assertEquals(3, Strings.lengthOfCommonSuffix("abcxyz", "defxyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix(null, "abc"));
    }

    @Test
    public void testLengthOfCommonSuffix_EdgeCases() {
        assertEquals(0, Strings.lengthOfCommonSuffix(null, null));
        assertEquals(0, Strings.lengthOfCommonSuffix(null, "abc"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", null));
        assertEquals(3, Strings.lengthOfCommonSuffix("abc", "xyzabc"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", "xyz"));
        assertEquals(2, Strings.lengthOfCommonSuffix("abc", "xbc"));
    }

    @Test
    public void testLengthOfCommonSuffix_FullCoverage() {
        assertEquals(4, Strings.lengthOfCommonSuffix("testing", "eating"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix("", "hello"));
        assertEquals(0, Strings.lengthOfCommonSuffix(null, "hello"));
        assertEquals(2, Strings.lengthOfCommonSuffix("hello", "lo"));
    }

    @Test
    public void testCommonPrefix() {
        assertEquals("abc", Strings.commonPrefix("abcdef", "abcxyz"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
        assertNull(Strings.commonPrefix(null, "abc"));
    }

    @Test
    public void testCommonPrefix_Multiple() {
        assertEquals("abc", Strings.commonPrefix("abcdef", "abcxyz", "abc123"));
        assertEquals("", Strings.commonPrefix("abc", "xyz", "123"));
    }

    @Test
    public void testCommonPrefixTwoArgs() {
        assertNull(Strings.commonPrefix(null, null));
        assertEquals("", Strings.commonPrefix("", "abc"));
        assertEquals("", Strings.commonPrefix("abc", ""));
        assertEquals("abc", Strings.commonPrefix("abc", "abc"));
        assertEquals("ab", Strings.commonPrefix("abc", "abxyz"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
    }

    @Test
    public void testCommonPrefixVarArgs() {
        assertNull(Strings.commonPrefix());
        assertNull(Strings.commonPrefix((CharSequence[]) null));
        assertEquals("abc", Strings.commonPrefix("abc"));
        assertEquals("a", Strings.commonPrefix("abc", "axy", "aijk"));
        assertEquals("", Strings.commonPrefix("abc", "xyz", "123"));
    }

    @Test
    public void testCommonPrefix_EdgeCases() {
        assertNull(Strings.commonPrefix(null, null));
        assertNull(Strings.commonPrefix(null, "abc"));
        assertNull(Strings.commonPrefix("abc", null));
        assertEquals("abc", Strings.commonPrefix("abc", "abcdef"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
        assertEquals("ab", Strings.commonPrefix("abc", "abx"));
    }

    @Test
    public void testCommonPrefix_VarArgs_EdgeCases() {
        assertEquals("ab", Strings.commonPrefix("abc", "abd", "abe"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
        assertNull(Strings.commonPrefix((CharSequence[]) null));
    }

    @Test
    public void testCommonPrefix_VarArgs_FullCoverage() {
        // empty array
        assertNull(Strings.commonPrefix(new CharSequence[0]));

        // single element
        assertEquals("prefix", Strings.commonPrefix(new CharSequence[] { "prefix" }));
        assertEquals("", Strings.commonPrefix(new CharSequence[] { "" }));
        assertNull(Strings.commonPrefix(new CharSequence[] { null }));

        // any empty
        assertEquals("", Strings.commonPrefix(new CharSequence[] { "hello", "", "help" }));

        // three elements with common prefix
        assertEquals("fl", Strings.commonPrefix(new CharSequence[] { "flower", "flow", "flight" }));

        // no common prefix among 3
        assertEquals("", Strings.commonPrefix(new CharSequence[] { "dog", "racecar", "car" }));

        // two elements
        assertEquals("ab", Strings.commonPrefix(new CharSequence[] { "abc", "ab" }));
    }

    @Test
    public void testCommonSuffix() {
        assertEquals("xyz", Strings.commonSuffix("abcxyz", "defxyz"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
        assertNull(Strings.commonSuffix(null, "abc"));
    }

    @Test
    public void testCommonSuffix_Multiple() {
        assertEquals("xyz", Strings.commonSuffix("abcxyz", "defxyz", "123xyz"));
        assertEquals("", Strings.commonSuffix("abc", "xyz", "123"));
    }

    @Test
    public void testCommonSuffixTwoArgs() {
        assertNull(Strings.commonSuffix(null, null));
        assertEquals("", Strings.commonSuffix("", "abc"));
        assertEquals("", Strings.commonSuffix("abc", ""));
        assertEquals("abc", Strings.commonSuffix("abc", "abc"));
        assertEquals("bc", Strings.commonSuffix("abc", "xbc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
    }

    @Test
    public void testCommonSuffixVarArgs() {
        assertNull(Strings.commonSuffix());
        assertNull(Strings.commonSuffix((CharSequence[]) null));
        assertEquals("abc", Strings.commonSuffix("abc"));
        assertEquals("c", Strings.commonSuffix("abc", "xyc", "ijkc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz", "123"));
    }

    @Test
    public void testCommonSuffix_EdgeCases() {
        assertNull(Strings.commonSuffix(null, null));
        assertNull(Strings.commonSuffix(null, "abc"));
        assertNull(Strings.commonSuffix("abc", null));
        assertEquals("abc", Strings.commonSuffix("abc", "xyzabc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
        assertEquals("bc", Strings.commonSuffix("abc", "xbc"));
    }

    @Test
    public void testCommonSuffix_VarArgs_EdgeCases() {
        assertEquals("bc", Strings.commonSuffix("abc", "xbc", "ybc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
        assertNull(Strings.commonSuffix((CharSequence[]) null));
    }

    @Test
    public void testCommonSuffix_VarArgs_FullCoverage() {
        // empty array
        assertNull(Strings.commonSuffix(new CharSequence[0]));

        // single element
        assertEquals("suffix", Strings.commonSuffix(new CharSequence[] { "suffix" }));
        assertEquals("", Strings.commonSuffix(new CharSequence[] { "" }));
        assertNull(Strings.commonSuffix(new CharSequence[] { null }));

        // any empty
        assertEquals("", Strings.commonSuffix(new CharSequence[] { "testing", "", "eating" }));

        // three elements with common suffix
        assertEquals("ting", Strings.commonSuffix(new CharSequence[] { "testing", "eating", "meeting" }));

        // no common suffix among 3
        assertEquals("", Strings.commonSuffix(new CharSequence[] { "abc", "xyz", "123" }));

        // two elements
        assertEquals("bc", Strings.commonSuffix(new CharSequence[] { "abc", "bc" }));
    }

    @Test
    public void testValidSurrogatePairAt() {
        // Emoji uses surrogate pair in Java
        String emoji = "\uD83D\uDE00"; // 😀
        assertTrue(Strings.validSurrogatePairAt(emoji, 0));
        assertFalse(Strings.validSurrogatePairAt(emoji, 1));
        assertFalse(Strings.validSurrogatePairAt("hello", 0));
        assertFalse(Strings.validSurrogatePairAt("hello", -1));
    }

    @Test
    public void test_lcs() {
        final String a = "0878e121-b14c-4a77-aef1-ec46f191d593";
        final String b = "b2b61159-491f-401b-bc17-08dd0a21e241";
        final String c = "zxabcdezy";
        final String d = "yzabcdezx";

        assertEquals("abcdez", Strings.longestCommonSubstring(c, d));
        assertEquals("08", Strings.longestCommonSubstring(a, b));
        assertEquals("11-14c7-a1e41", ListUtils.longestCommonSubsequence(a, b));
    }

    @Test
    public void test_lcs_02() {
        assertEquals("abcdez", Strings.longestCommonSubstring("zxabcdezy", "yzabcdezx"));
        assertEquals("", Strings.longestCommonSubstring("", ""));
        assertEquals("a", Strings.longestCommonSubstring("abc", "zxa"));
        assertEquals("Geeks", Strings.longestCommonSubstring("GeeksforGeeks", "GeeksQuiz"));
        assertEquals("xddd", Strings.longestCommonSubstring("xddddd", "ddxxddda"));
        assertEquals("ddddd", Strings.longestCommonSubstring("xddddd", "ddxxdddaddddd"));
        assertEquals("", Strings.longestCommonSubstring("ddddddddd", "eeeeeeeeee"));
        assertEquals("", Strings.longestCommonSubstring("", ""));
    }

    @Test
    public void testLongestCommonSubstring() {
        assertEquals("abc", Strings.longestCommonSubstring("xabcy", "zabc123"));
        assertEquals("", Strings.longestCommonSubstring("abc", "xyz"));
        assertNull(Strings.longestCommonSubstring(null, "abc"));

        // The shorter first input is used as the DP row, while ties still prefer the match ending earliest in it.
        assertEquals("abab", Strings.longestCommonSubstring("ababa", "xxababyybabazz"));
        assertEquals("abc", Strings.longestCommonSubstring("abc", "x".repeat(100) + "abc"));
    }

    @Test
    public void testLongestCommonSubstring_LargeSuffixArrayPathMatchesDynamicProgramming() {
        final Random random = new Random(20260820L);

        for (int iteration = 0; iteration < 4; iteration++) {
            final StringBuilder a = new StringBuilder(1000);
            final StringBuilder b = new StringBuilder(1000);

            for (int i = 0; i < 1000; i++) {
                a.append((char) ('a' + random.nextInt(7)));
                b.append((char) ('a' + random.nextInt(7)));
            }

            assertEquals(longestCommonSubstringReference(a.toString(), b.toString()), Strings.longestCommonSubstring(a, b));
        }

        final String repeated = "ab".repeat(256);
        assertEquals(repeated, Strings.longestCommonSubstring(repeated, repeated));

        final String emojiA = new String(Character.toChars(0x1F600));
        final String emojiB = new String(Character.toChars(0x1F601));
        final String a = ("a" + emojiA + "b" + emojiB).repeat(250);
        final String b = ("x" + emojiA + "b" + emojiB).repeat(250);
        assertEquals(longestCommonSubstringReference(a, b), Strings.longestCommonSubstring(a, b));
    }

    @Test
    public void testLongestCommonSubstring_RejectsImpracticalDynamicProgrammingFallback() throws ReflectiveOperationException {
        final Method workGuard = Strings.class.getDeclaredMethod("checkLongestCommonSubstringFallbackWork", int.class, int.class);
        workGuard.setAccessible(true);
        final int largeLength = CommonUtil.MAX_ARRAY_SIZE / 2 + 1;

        final java.lang.reflect.InvocationTargetException exception = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> workGuard.invoke(null, largeLength, largeLength));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertDoesNotThrow(() -> workGuard.invoke(null, 0, CommonUtil.MAX_ARRAY_SIZE));
    }

    @Test
    public void testPadEndByDisplayWidth() {
        assertEquals("ab   ", Strings.padEndToDisplayWidth("ab", 5));
        assertEquals("abcde", Strings.padEndToDisplayWidth("abcde", 3));
        assertEquals("中文 ", Strings.padEndToDisplayWidth("中文", 5));
        assertEquals("   ", Strings.padEndToDisplayWidth(null, 3));
        // R-16: a negative minDisplayWidth is now rejected (was silently accepted before)
        assertThrows(IllegalArgumentException.class, () -> Strings.padEndToDisplayWidth("ab", -1));

        // Cluster-aware: ZWJ emoji and keycaps are one display unit (width 2), not a sum of code points.
        final String womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        final String keycapOne = "1\uFE0F\u20E3";
        assertEquals(2, Strings.displayWidth(womanTechnologist));
        assertEquals(womanTechnologist, Strings.padEndToDisplayWidth(womanTechnologist, 2));
        assertEquals(womanTechnologist + "  ", Strings.padEndToDisplayWidth(womanTechnologist, 4));
        assertEquals(keycapOne, Strings.padEndToDisplayWidth(keycapOne, 2));
    }

    @Test
    public void testStrUtilSubstringOptional() {
        assertTrue(StrUtil.substring("abc", 1).isPresent());
        assertEquals("bc", StrUtil.substring("abc", 1).get());
        assertFalse(StrUtil.substring("abc", 5).isPresent());
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

    @Test
    public void testStrUtilSubstringAfter() {
        assertEquals("cde", StrUtil.substringAfter("abcde", "ab").get());
        assertFalse(StrUtil.substringAfter("abc", "x").isPresent());
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
        assertEquals("test", StrUtil.substringAfterAny("test", null, "").orElseThrow());
    }

    @Test
    public void testStrUtilSubstringBefore() {
        assertEquals("ab", StrUtil.substringBefore("abcde", "cd").get());
        assertFalse(StrUtil.substringBefore("abc", "x").isPresent());
    }

    @Test
    public void testStrUtil_SubstringBefore() {
        assertEquals("He", StrUtil.substringBefore("Hello World", 'l').orElse(null));
        assertFalse(StrUtil.substringBefore("Hello", 'x').isPresent());

        assertEquals("Hello", StrUtil.substringBefore("Hello World", " World").orElse(null));
        assertFalse(StrUtil.substringBefore("Hello", "xyz").isPresent());

        assertEquals("lo", StrUtil.substringBefore("Hello World", 3, " World").orElse(null));
        assertFalse(StrUtil.substringBefore("Hello World", 7, " ").isPresent());
        assertEquals("", StrUtil.substringBefore("test@end", 4, "@").orElse(null));
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
        assertEquals("", StrUtil.substringBeforeAny("test", null, "").orElseThrow());
    }

    @Test
    public void testStrUtilSubstringBetween() {
        assertEquals("abc", StrUtil.substringBetween("(abc)", "(", ")").get());
        assertFalse(StrUtil.substringBetween("abc", "[", "]").isPresent());
        assertEquals("ello", StrUtil.substringBetween("hello", 0, 10).orElseThrow());
        assertFalse(StrUtil.substringBetween("hello", 5, 10).isPresent());
        // The computed-begin variant anchors on the FIRST occurrence of the delimiter (delegates to Strings).
        assertEquals("a", StrUtil.substringBetween("a=b=c", i -> -1, "=").get());
    }

    @Test
    public void testStrUtilSubstringBetweenRejectsNullFunctions() {
        assertThrows(IllegalArgumentException.class, () -> StrUtil.substringBetween("hello", 0, (IntUnaryOperator) null));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.substringBetween("hello", (IntUnaryOperator) null, 5));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.substringBetween("hello", "h", (IntUnaryOperator) null));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.substringBetween("hello", (IntUnaryOperator) null, "o"));
    }

    @Test
    public void test_between() {
        {
            String ret = Strings.substringBetween(null, null);
            assertEquals(null, ret);

            ret = Strings.substringBetween("", null);
            assertEquals(null, ret);

            ret = Strings.substringBetween(null, "");
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "c");
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "a");
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "b");
            assertEquals("", ret);

            ret = Strings.substringBetween("aab", "a", "b");
            assertEquals("a", ret);

            ret = Strings.substringBetween("aab", "a", "a");
            assertEquals("", ret);
        }
    }

    @Test
    public void test_substringbetween_01() {
        assertEquals("aa", CommonUtil.firstNonBlank("aa", "bb").get());

        assertEquals("ab", StringUtils.substringBetween("abc", "", "c"));
        assertEquals("", StringUtils.substringBetween("abc", "a", ""));
        assertEquals("", StringUtils.substringBetween("abc", "", ""));
        assertEquals("b", StringUtils.substringBetween("abc", "a", "c"));

        assertEquals("ab", Strings.substringBetween("abc", "", "c"));
        assertEquals("", Strings.substringBetween("abc", "a", ""));
        assertEquals("", Strings.substringBetween("abc", "", ""));
        assertEquals("b", Strings.substringBetween("abc", "a", "c"));
        assertEquals("b", Strings.substringBetween("abc", 0, 'c'));
        assertEquals("", Strings.substringBetween("abc", 0, "bc"));
        assertEquals("b", Strings.substringBetween("abc", 0, "c"));
        assertEquals("", Strings.substringBetween("abc", "b", 2));
        assertNull(Strings.substringBetween("abc", "b", 1));
        assertEquals("bc", Strings.substringBetween("abc::tail", i -> {
            assertEquals(3, i);
            return i - 3;
        }, "::"));
    }

    @Test
    public void testSubstringsBetween() {
        List<String> result = Strings.substringsBetween("a:b:c:d", ':', ':');
        assertEquals(Arrays.asList("b"), result);
    }

    @Test
    public void testSubstringsBetweenDefaultStrategy() {
        assertEquals(list("a", "b"), Strings.substringsBetween("[a][b]", '[', ']'));
        assertEquals(list("a[b"), Strings.substringsBetween("[a[b]c]", '[', ']'));
    }

    @Test
    public void testSubstringsBetweenStackBased() {
        assertEquals(list("c", "a2[c]", "a"), substringsBetween_StackBased_("3[a2[c]]2[a]", '[', ']'));
        assertEquals(list("a", "b[a]", "[b[a]]c"), substringsBetween_StackBased_("[[b[a]]c]", '[', ']'));
    }

    @Test
    public void test_substringsBetween() {

        {
            assertEquals("[\"a2[c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.SEQUENTIAL)));
            assertEquals("[\"c\", \"a2[c]\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.ALL_LEVELS)));
            assertEquals("[\"a2[c]\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY)));
        }

        {
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.SEQUENTIAL)));
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.ALL_LEVELS)));
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY)));
        }

        {
            assertEquals("[\"[b[a\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.SEQUENTIAL)));
            assertEquals("[\"a\", \"b[a]\", \"[b[a]]c\"]",
                    CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.ALL_LEVELS)));
            assertEquals("[\"[b[a]]c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY)));
        }

        {
            assertEquals("[\"[b[a\", \"c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.SEQUENTIAL)));
            assertEquals("[\"a\", \"c\", \"b[a][c]d\"]",
                    CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.ALL_LEVELS)));
            assertEquals("[\"b[a][c]d\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY)));
        }

    }

    @Test
    public void suffixArrayBound_subtractsOffsetToAvoidIntOverflow() {
        final int index = 1 << 30;
        final int offset = 1 << 30;
        final int len = (1 << 30) + 1;
        assertTrue(index + offset < len);
        assertFalse(index < len - offset);
    }

    /**
     * Contract pin for the {@code @return} text of {@code appendIfMissing}, {@code appendIfMissingIgnoreCase},
     * {@code prependIfMissing}, {@code prependIfMissingIgnoreCase} and {@code wrapIfMissing(String,String,String)}:
     * a {@code null} input yields {@code null}, an affix already present yields the original string, and an empty
     * input yields the affix.
     */
    @Test
    public void testAppendPrependWrapIfMissing_NullInputAndAffixAlreadyPresent() {
        assertNull(Strings.appendIfMissing(null, ".txt"));
        assertEquals("file.txt", Strings.appendIfMissing("file", ".txt"));
        assertEquals("file.txt", Strings.appendIfMissing("file.txt", ".txt"));
        assertEquals(".txt", Strings.appendIfMissing("", ".txt"));

        assertNull(Strings.appendIfMissingIgnoreCase(null, ".TXT"));
        assertEquals("file.TXT", Strings.appendIfMissingIgnoreCase("file.TXT", ".txt"));
        assertEquals(".TXT", Strings.appendIfMissingIgnoreCase("", ".TXT"));

        assertNull(Strings.prependIfMissing(null, "http://"));
        assertEquals("http://ex.com", Strings.prependIfMissing("ex.com", "http://"));
        assertEquals("http://", Strings.prependIfMissing("", "http://"));

        assertNull(Strings.prependIfMissingIgnoreCase(null, "HTTP://"));
        assertEquals("HTTP://ex", Strings.prependIfMissingIgnoreCase("HTTP://ex", "http://"));
        assertEquals("HTTP://", Strings.prependIfMissingIgnoreCase("", "HTTP://"));

        assertNull(Strings.wrapIfMissing(null, "[", "]"));
        assertEquals("[ab]", Strings.wrapIfMissing("ab", "[", "]"));
        assertEquals("[ab]", Strings.wrapIfMissing("[ab]", "[", "]"));
        assertEquals("[]", Strings.wrapIfMissing("", "[", "]"));
    }


    @Test
    public void testLengthOfCommonPrefix_DoesNotSplitSurrogatePair() {
        final String grinning = new String(Character.toChars(0x1F600));
        final String beaming = new String(Character.toChars(0x1F601));
        assertEquals(grinning.charAt(0), beaming.charAt(0));
        assertEquals(0, Strings.lengthOfCommonPrefix(grinning, beaming));
        assertEquals("", Strings.commonPrefix(grinning, beaming));
        assertEquals(1, Strings.lengthOfCommonPrefix("a" + grinning, "a" + beaming));
        assertEquals(3, Strings.lengthOfCommonPrefix("a" + grinning, "a" + grinning + "b"));
    }

    @Test
    public void testLengthOfCommonSuffix_DoesNotSplitSurrogatePair() {
        final String grinning = new String(Character.toChars(0x1F600));
        final String other = new String(Character.toChars(0x1FA00));
        assertEquals(grinning.charAt(1), other.charAt(1));
        assertEquals(0, Strings.lengthOfCommonSuffix(grinning, other));
        assertEquals("", Strings.commonSuffix(grinning, other));
        assertEquals(1, Strings.lengthOfCommonSuffix(grinning + "z", other + "z"));
        assertEquals(3, Strings.lengthOfCommonSuffix(grinning + "z", "b" + grinning + "z"));
    }

    // ---- perf review 2026-09-26 G072 begin ----

    private static byte[] g072ReportingUtf8Encode(final String str) throws java.nio.charset.CharacterCodingException {
        final java.nio.ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .encode(java.nio.CharBuffer.wrap(str));
        final byte[] result = new byte[encoded.remaining()];
        encoded.get(result);
        return result;
    }

    private static String g072ReportingUtf8Decode(final byte[] bytes) throws java.nio.charset.CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString();
    }

    // G072-01: UTF-8 strict encoding equals the reporting CharsetEncoder for well-formed input (incl. surrogate pairs)
    @Test
    public void testGetBytesUtf8Strict_matchesReportingEncoder() throws Exception {
        final String[] inputs = { "a", "?", "???", "\u0000", "abc 123 ~\u007F", "caf\u00E9", "\u00FF\u0100", "\u4E2D\u6587\u5B57",
                "\uD83D\uDE00", "x\uD83D\uDE00y\uD801\uDC00", "\uFFFD", "\uFFFF\uFFFE", "\u07FF\u0800", "The quick brown fox".repeat(500),
                "\u4E2D".repeat(3000) + "a", "a?\uD83D\uDE00", "?".repeat(50) + "\u00E9" };

        for (final String input : inputs) {
            final byte[] expected = g072ReportingUtf8Encode(input);
            assertArrayEquals(expected, Strings.getBytesUtf8Strict(input), input);
            assertArrayEquals(expected, Strings.getBytesStrict(input, StandardCharsets.UTF_8), input);
            assertArrayEquals(expected, Strings.getBytesStrict(input, Charsets.UTF_8), input);
            assertArrayEquals(expected, Strings.getBytesStrict(input, java.nio.charset.Charset.forName("UTF-8")), input);
            assertEquals(java.util.Base64.getEncoder().encodeToString(expected), Strings.base64EncodeStringStrict(input), input);
            assertEquals(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(expected),
                    Strings.base64UrlEncodeStringStrict(input, StandardCharsets.UTF_8), input);
        }

        final byte[] empty = Strings.getBytesUtf8Strict("");
        assertEquals(0, empty.length);
        final byte[] first = Strings.getBytesUtf8Strict("abc");
        final byte[] second = Strings.getBytesUtf8Strict("abc");
        assertTrue(first != second);
        first[0] = 'z';
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8Strict("abc"));
        assertNull(Strings.getBytesUtf8Strict(null));
    }

    // G072-01: malformed UTF-16 still fails with the same exception, message and cause type
    @Test
    public void testGetBytesUtf8Strict_loneSurrogates() {
        final String[] inputs = { "\ud800", "\udc00", "a\ud83d", "\ude00\ud83d", "abc\udfffdef", "\uD83D\uDE00\ud83d", "?\ud800", "\udc00?" };

        for (final String input : inputs) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Strings.getBytesUtf8Strict(input));
            assertEquals("Input contains malformed UTF-16 or a character not representable in UTF-8", e.getMessage());
            assertTrue(e.getCause() instanceof java.nio.charset.MalformedInputException, input);
            assertThrows(IllegalArgumentException.class, () -> Strings.base64EncodeStringStrict(input));
            assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlEncodeStringStrict(input));
        }

        assertThrows(NullPointerException.class, () -> Strings.getBytesStrict("a", null));
    }

    // G072-02: UTF-8 strict decoding equals the reporting CharsetDecoder for well-formed input (incl. a genuine U+FFFD)
    @Test
    public void testBase64DecodeToStringStrict_utf8MatchesReportingDecoder() throws Exception {
        final String[] inputs = { "a", "?", "abc 123", "\u0000", "caf\u00E9", "\u4E2D\u6587", "\uD83D\uDE00", "x\uFFFDy", "\uFFFD", "\uFFFF",
                "The quick brown fox".repeat(500) };

        for (final String input : inputs) {
            final byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            final String expected = g072ReportingUtf8Decode(bytes);
            assertEquals(input, expected);
            assertEquals(expected, Strings.base64DecodeToStringStrict(java.util.Base64.getEncoder().encodeToString(bytes)), input);
            assertEquals(expected, Strings.base64DecodeToStringStrict(java.util.Base64.getEncoder().encodeToString(bytes), StandardCharsets.UTF_8));
            assertEquals(expected, Strings.base64UrlDecodeToStringStrict(java.util.Base64.getUrlEncoder().encodeToString(bytes), StandardCharsets.UTF_8));
        }
    }

    // G072-02: malformed UTF-8 still fails with the same exception, message and cause type
    @Test
    public void testBase64DecodeToStringStrict_utf8Malformed() {
        final byte[][] inputs = { { (byte) 0xFF }, { 'a', (byte) 0xC3 }, { (byte) 0xED, (byte) 0xA0, (byte) 0x80 }, { (byte) 0xC0, (byte) 0x80 },
                { (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80 }, { (byte) 0xE2, (byte) 0x82 }, { 'a', 'b', (byte) 0x80, 'c' },
                { (byte) 0xEF, (byte) 0xBF, (byte) 0xBD, (byte) 0xFE } };

        for (final byte[] input : inputs) {
            final String base64 = java.util.Base64.getEncoder().encodeToString(input);
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Strings.base64DecodeToStringStrict(base64));
            assertEquals("Input bytes are not valid UTF-8", e.getMessage());
            assertTrue(e.getCause() instanceof java.nio.charset.CharacterCodingException);
            assertThrows(IllegalArgumentException.class,
                    () -> Strings.base64UrlDecodeToStringStrict(java.util.Base64.getUrlEncoder().encodeToString(input), StandardCharsets.UTF_8));
        }

        assertThrows(NullPointerException.class, () -> Strings.base64DecodeToStringStrict("YQ==", null));
    }

    // G072-03: toCharArray(StringBuilder) bulk copy returns the same fresh array as the charAt loop
    @Test
    public void testToCharArray_stringBuilderAndOtherCharSequences() {
        final String[] inputs = { "a", "ab", "caf\u00E9", "\uD83D\uDE00x\ud800", "The quick brown fox".repeat(100) };

        for (final String input : inputs) {
            final StringBuilder sb = new StringBuilder(input.length() + 16).append(input);
            final char[] fromBuilder = Strings.toCharArray(sb);
            assertArrayEquals(input.toCharArray(), fromBuilder);
            assertTrue(fromBuilder != Strings.toCharArray(sb));
            fromBuilder[0] = '#';
            assertEquals(input, sb.toString());
            assertArrayEquals(input.toCharArray(), Strings.toCharArray(new StringBuffer(input)));
            assertArrayEquals(input.toCharArray(), Strings.toCharArray(java.nio.CharBuffer.wrap(input)));
        }

        assertTrue(N.EMPTY_CHAR_ARRAY == Strings.toCharArray(new StringBuilder()));
        assertNull(Strings.toCharArray(null));
    }

    // G072-04: swapCase non-ASCII path; the per-code-point substring is only built for cased code points
    @Test
    public void testSwapCase_unicodeUncasedAndCasedCodePoints() {
        assertEquals("\u4E2D\u6587 1-2 \uFF01", Strings.swapCase("\u4E2D\u6587 1-2 \uFF01"));
        assertEquals("\u4E2DAbC\u00C9", Strings.swapCase("\u4E2DaBc\u00E9"));
        assertEquals("SS\u00E0", Strings.swapCase("\u00DF\u00C0"));
        assertEquals("i\u0307x", Strings.swapCase("\u0130X"));
        assertEquals("\u01C6", Strings.swapCase("\u01C5"));
        assertEquals("\uD801\uDC28\uD801\uDC00", Strings.swapCase("\uD801\uDC00\uD801\uDC28"));
        assertEquals("\uD83D\uDE00A\ud800b\udc00", Strings.swapCase("\uD83D\uDE00a\ud800B\udc00"));
        assertEquals("o\u03C3\u03C2 \u03C3", Strings.swapCase("O\u03A3\u03A3 \u03A3"));
        assertEquals("\u00E9\u4E2D", Strings.swapCase("\u00C9\u4E2D"));

        final int[] pool = { 'a', 'Z', '5', ' ', 0x00e9, 0x00c9, 0x00df, 0x0130, 0x01c5, 0x4e2d, 0x3000, 0x10400, 0x10428, 0x1f600, 0xd800, 0xdc00,
                0xfb00, 0x0149, 0x0390 };
        final Random random = new Random(20260926L);

        for (int round = 0; round < 2000; round++) {
            final StringBuilder input = new StringBuilder();
            final StringBuilder expected = new StringBuilder();

            for (int i = random.nextInt(12); i >= 0; i--) {
                final int codePoint = pool[random.nextInt(pool.length)];
                final String single = new String(Character.toChars(codePoint));
                input.append(single);

                if (Character.isUpperCase(codePoint) || Character.isTitleCase(codePoint)) {
                    expected.append(single.toLowerCase(Locale.ROOT));
                } else if (Character.isLowerCase(codePoint)) {
                    expected.append(single.toUpperCase(Locale.ROOT));
                } else {
                    expected.append(single);
                }
            }

            final String text = input.toString();

            if (text.indexOf("\uD800\uDC00") >= 0) {
                continue;
            }

            assertEquals(expected.toString(), Strings.swapCase(text), text);
        }
    }

    // ---- perf review 2026-09-26 G072 end ----
    // ---- perf review 2026-09-26 G073 begin ----
    private static int countMatchesCharReference(final String str, final char ch) {
        int count = 0;

        for (int i = 0; str != null && i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }

        return count;
    }

    // G073-01: countMatches(String, char) switches from indexOf hopping to a plain scan on dense input; pin both regimes.
    @Test
    public void testCountMatchesChar_sparseDenseAndMixedRegions() {
        assertEquals(0, Strings.countMatches(null, ','));
        assertEquals(0, Strings.countMatches("", ','));
        assertEquals(1, Strings.countMatches(",", ','));
        assertEquals(0, Strings.countMatches("abc", '\u4E2D'));
        assertEquals(1, Strings.countMatches("ab\u4E2D", '\u4E2D'));
        assertEquals(40, Strings.countMatches(",".repeat(40), ','));
        assertEquals(7, Strings.countMatches(",".repeat(7), ','));
        assertEquals(8, Strings.countMatches(",".repeat(8), ','));
        assertEquals(9, Strings.countMatches(",".repeat(9), ','));
        assertEquals(40, Strings.countMatches(",".repeat(20) + "x".repeat(1000) + ",".repeat(20), ','));
        assertEquals(20, Strings.countMatches("x".repeat(1000) + ",".repeat(20), ','));
        assertEquals(2, Strings.countMatches("\uD83D\uDE00\uD83D\uDE00", '\uD83D'));

        final Random random = new Random(0x6073L);
        final double[] densities = { 0.0, 0.01, 0.1, 0.24, 0.26, 0.5, 0.9, 1.0 };

        for (int testCase = 0; testCase < 2000; testCase++) {
            final int length = random.nextInt(testCase < 1000 ? 70 : 600);
            final StringBuilder sb = new StringBuilder(length);
            double density = densities[random.nextInt(densities.length)];

            for (int i = 0; i < length; i++) {
                if (random.nextInt(50) == 0) {
                    density = densities[random.nextInt(densities.length)];
                }

                sb.append(random.nextDouble() < density ? ',' : (random.nextInt(20) == 0 ? '\u4E2D' : (char) ('a' + random.nextInt(3))));
            }

            final String str = sb.toString();

            for (final char ch : new char[] { ',', 'a', '\u4E2D', 'z' }) {
                assertEquals(countMatchesCharReference(str, ch), Strings.countMatches(str, ch), str);
            }
        }
    }

    private static int minIndexOfAllStringReference(final String str, final int fromIndex, final String... valuesToFind) {
        if (str == null || fromIndex > str.length() || valuesToFind == null) {
            return -1;
        }

        final int from = Math.max(0, fromIndex);
        int result = -1;

        for (final String value : valuesToFind) {
            if (value == null) {
                continue;
            }

            final int found = value.isEmpty() ? from : str.indexOf(value, from);

            if (found >= 0 && (result < 0 || found < result)) {
                result = found;
            }
        }

        return result;
    }

    private static int minIndexOfAllCharReference(final String str, final int fromIndex, final char... valuesToFind) {
        if (str == null || fromIndex > str.length() || valuesToFind == null) {
            return -1;
        }

        final int from = Math.max(0, fromIndex);
        int result = -1;

        for (final char value : valuesToFind) {
            final int found = str.indexOf(value, from);

            if (found >= 0 && (result < 0 || found < result)) {
                result = found;
            }
        }

        return result;
    }

    // G073-02: minIndexOfAll bounds each later candidate's search by the current hit; pin matches that straddle the
    // bound, empty/null/overlong candidates and every fromIndex against an unbounded reference.
    @Test
    public void testMinIndexOfAll_boundedSearchMatchesUnboundedReference() {
        assertEquals(1, Strings.minIndexOfAll("abcd", "c", "bcd"));
        assertEquals(1, Strings.minIndexOfAll("abcd", "c", "bcde", "bc"));
        assertEquals(2, Strings.minIndexOfAll("abcd", "c", "bcde", "cd"));
        assertEquals(0, Strings.minIndexOfAll("abcd", "d", "b", ""));
        assertEquals(2, Strings.minIndexOfAll("abcd", 2, "d", "zz", "", "c"));
        assertEquals(1, Strings.minIndexOfAll("aaab", 1, "b", "aab"));
        assertEquals(3, Strings.minIndexOfAll("aaab", 2, "b", "aab"));
        assertEquals(1, Strings.minIndexOfAll("aaab", 1, "b", "aa"));
        assertEquals(1, Strings.minIndexOfAll("abcd", 'c', 'b', 'z'));
        assertEquals(2, Strings.minIndexOfAll("abcd", 'c', 'd', 'z'));
        assertEquals(1, Strings.minIndexOfAll("x\u4E2D\u4E2D", 0, 'q', '\u4E2D', 'z'));

        final Random random = new Random(0x6073_02L);
        final String alphabet = "abcab\u4E2D";

        for (int testCase = 0; testCase < 3000; testCase++) {
            // Lengths past 256 reach the ranged search, which is used only when the skipped tail is long.
            final int length = testCase < 2000 ? random.nextInt(12) : 250 + random.nextInt(700);
            final StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }

            final String str = sb.toString();
            final String[] values = new String[1 + random.nextInt(5)];

            for (int i = 0; i < values.length; i++) {
                final int kind = random.nextInt(10);

                if (kind == 0) {
                    values[i] = null;
                } else if (kind == 1) {
                    values[i] = "";
                } else {
                    final int valueLength = 1 + random.nextInt(kind == 2 ? 15 : 4);
                    final StringBuilder value = new StringBuilder(valueLength);

                    for (int j = 0; j < valueLength; j++) {
                        value.append((alphabet + "z").charAt(random.nextInt(alphabet.length() + 1)));
                    }

                    values[i] = value.toString();
                }
            }

            final char[] chars = new char[1 + random.nextInt(7)];

            for (int i = 0; i < chars.length; i++) {
                chars[i] = (alphabet + "z").charAt(random.nextInt(alphabet.length() + 1));
            }

            for (int fromIndex = -2; fromIndex <= length + 1; fromIndex += length < 20 || fromIndex < 20 ? 1 : 1 + random.nextInt(40)) {
                assertEquals(minIndexOfAllStringReference(str, fromIndex, values), Strings.minIndexOfAll(str, fromIndex, values), str);
                assertEquals(minIndexOfAllCharReference(str, fromIndex, chars), Strings.minIndexOfAll(str, fromIndex, chars), str);
            }

            for (final int fromIndex : new int[] { length - 1, length, length + 1 }) {
                assertEquals(minIndexOfAllStringReference(str, fromIndex, values), Strings.minIndexOfAll(str, fromIndex, values), str);
                assertEquals(minIndexOfAllCharReference(str, fromIndex, chars), Strings.minIndexOfAll(str, fromIndex, chars), str);
            }

            assertEquals(minIndexOfAllStringReference(str, 0, values), Strings.minIndexOfAll(str, values), str);
            assertEquals(minIndexOfAllCharReference(str, 0, chars), Strings.minIndexOfAll(str, chars), str);
        }

        final String longStr = "abcd" + "e".repeat(400) + "bcd";
        assertEquals(1, Strings.minIndexOfAll(longStr, "c", "bcd"));
        assertEquals(2, Strings.minIndexOfAll(longStr, "c", "cde", "zz"));
        assertEquals(1, Strings.minIndexOfAll(longStr, "c", "bcde", "abcdX"));
        assertEquals(2, Strings.minIndexOfAll(longStr, 2, "d", "cd", "abcd"));
        assertEquals(2, Strings.minIndexOfAll(longStr, 2, "d", ""));
        assertEquals(0, Strings.minIndexOfAll(longStr, "e", "abcd"));
        assertEquals(3, Strings.minIndexOfAll(longStr, "e", "de", "abcde".repeat(2)));
        assertEquals(4, Strings.minIndexOfAll(longStr, "e", "e".repeat(400), "e".repeat(401)));
        assertEquals(404, Strings.minIndexOfAll(longStr, 5, "bcd", "e".repeat(401), "cd"));
        assertEquals(3, Strings.minIndexOfAll(longStr, 'd', 'z', 'x'));
        assertEquals(1, Strings.minIndexOfAll(longStr, 'd', 'z', 'b'));
    }
    // ---- perf review 2026-09-26 G073 end ----
    // ---- perf review 2026-09-26 G075 begin ----
    // G075-01: the East Asian display-width shortcut must agree with the Unicode 17 property tables it bypasses.
    @Test
    public void testCodePointDisplayWidth_commonWideRangesMatchPropertyTables() {
        final int[][] ranges = { { 0x3041, 0x3096 }, { 0x309B, 0x30FF }, { 0x3400, 0x9FFF }, { 0xAC00, 0xD7A3 }, { 0xFF01, 0xFF5E } };
        final Strings.DisplayWidthPolicy narrow = new Strings.DisplayWidthPolicy(1, 1);

        for (final int[] range : ranges) {
            for (int codePoint = range[0]; codePoint <= range[1]; codePoint++) {
                final String hex = Integer.toHexString(codePoint);
                assertTrue(Unicode17Data.isEastAsianWideOrFullwidth(codePoint), hex);
                assertFalse(Unicode17Data.isEmojiPresentation(codePoint), hex);
                assertFalse(Unicode17Data.isZeroWidthCategory(codePoint), hex);

                if (codePoint < 0xAC00 || codePoint > 0xD7A3) {
                    assertEquals(Unicode17Data.GCB_OTHER, Unicode17Data.graphemeBreakProperty(codePoint), hex);
                    assertEquals(Unicode17Data.INCB_NONE, Unicode17Data.indicConjunctBreakProperty(codePoint), hex);
                    assertFalse(Unicode17Data.isExtendedPictographic(codePoint), hex);
                }

                assertEquals(2, Strings.codePointDisplayWidth(codePoint), hex);
                assertEquals(2, Strings.codePointDisplayWidth(codePoint, Strings.DisplayWidthPolicy.CJK), hex);
                assertEquals(2, Strings.codePointDisplayWidth(codePoint, narrow), hex);
                assertEquals(2, Strings.displayWidth(new String(Character.toChars(codePoint))), hex);
            }
        }
    }

    // G075-01: code points at the edges of the shortcut ranges and mixed with marks, joiners and surrogates.
    @Test
    public void testDisplayWidth_commonWideRangesBoundariesAndClusters() {
        final int[][] codePoints = { { 0x3040 }, { 0x3097 }, { 0x3098 }, { 0x3099 }, { 0x309A }, { 0x3100 }, { 0x33FF }, { 0xA000 }, { 0xFF00 }, { 0xFF5F },
                { 0x3399, 0x9FFF, 0xA48C, 0xA48D }, { 0x33FF, 0x3400 }, { 0x9FFF, 0xA000 }, { 0xABFF, 0xAC00, 0xD7A3, 0xD7A4 }, { 0x3099, 0x4E00 },
                { 0x4E00, 0x3099 }, { 0x4E00, 0x0301 }, { 0x304B, 0x3099 }, { 0x61, 0x4E00, 0x62 }, { 0x30A2, 0xFF9E }, { 0x4E00, 0xFE0F },
                { 0x3042, 0x200D, 0x3042 }, { 0xAC00, 0x11A8 }, { 0x0915, 0x094D, 0x4E00 }, { 0x3400, 0x9FFF, 0x3041, 0x3096, 0x309B, 0x30FF, 0xFF01, 0xFF5E },
                { 0x0E01, 0x4E00 }, { 0x4E00, 0x0E33 }, { 0x30AB, 0x309A }, { 0x1F600, 0x4E00 }, { 0x4E00, 0x200D, 0x1F600 }, { 0x0600, 0x4E00 },
                { 0x4E00, 0xE0001 }, { 0x3040, 0x3097, 0x3098, 0x3099, 0x309A, 0x3100, 0x33FF, 0xA000, 0xFF00, 0xFF5F } };
        final int[] expected = { 1, 1, 1, 0, 0, 1, 2, 2, 1, 2, 7, 4, 4, 6, 2, 2, 2, 2, 4, 3, 2, 4, 2, 3, 16, 3, 2, 2, 4, 4, 2, 2, 11 };
        final Strings.DisplayWidthPolicy narrow = new Strings.DisplayWidthPolicy(1, 1);

        assertEquals(codePoints.length, expected.length);

        for (int i = 0; i < codePoints.length; i++) {
            final StringBuilder sb = new StringBuilder();

            for (final int codePoint : codePoints[i]) {
                sb.appendCodePoint(codePoint);
            }

            final String value = sb.toString();
            final String message = Arrays.toString(codePoints[i]);

            assertEquals(expected[i], Strings.displayWidth(value), message);
            assertEquals(expected[i], Strings.displayWidth(value, Strings.DisplayWidthPolicy.CJK), message);
            // An emoji takes the policy's emoji width; everything else here is policy-independent.
            assertEquals(expected[i] - (value.codePoints().anyMatch(codePoint -> codePoint >= 0x1F000 && codePoint <= 0x1FAFF) ? 1 : 0), Strings.displayWidth(value, narrow), message);

            final String padding = expected[i] < 8 ? Strings.repeat(' ', 8 - expected[i]) : "";
            assertEquals(value + padding, Strings.padEndToDisplayWidth(value, 8), message);
            assertEquals(padding + value, Strings.padStartToDisplayWidth(value, 8), message);
        }
    }
    // ---- perf review 2026-09-26 G075 end ----
}
