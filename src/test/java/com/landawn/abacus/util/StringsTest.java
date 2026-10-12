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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.u.Optional;

/** Strings general contracts, including regression cases. */
public class StringsTest extends StringsTestSupport {



    // B2 : decode-only charsets

    /** A charset with no encoder, or {@code null} if this JDK ships none. JDK 25 ships ISO-2022-CN and x-JISAutoDetect. */
    private static Charset decodeOnlyCharset() {
        for (final Charset cs : Charset.availableCharsets().values()) {
            if (!cs.canEncode()) {
                return cs;
            }
        }

        return null;
    }

    private static byte[] g072ReportingUtf8Encode(final String str) throws java.nio.charset.CharacterCodingException {
        final java.nio.ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .encode(java.nio.CharBuffer.wrap(str));
        final byte[] result = new byte[encoded.remaining()];
        encoded.get(result);
        return result;
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

    // center

    @Test
    public void testCenter() {
        assertEquals("  abc  ", Strings.center("abc", 7));
        assertEquals(" abc  ", Strings.center("abc", 6));
        assertEquals("abc", Strings.center("abc", 3));
        assertEquals("abc", Strings.center("abc", 2));
        assertEquals("   ", Strings.center(null, 3));
    }

    @Test
    public void testCenter_PaddingDistribution() {
        assertEquals(" ab ", Strings.center("ab", 4));
        // Odd extra: extra goes on the right.
        assertEquals(" a  ", Strings.center("a", 4));
        assertEquals("    ", Strings.center("", 4));
        assertEquals("    ", Strings.center((String) null, 4));
        assertEquals("abcd", Strings.center("abcd", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.center("a", -1));
    }

    @Test
    public void testCenter_WithChar() {
        assertEquals("**abc**", Strings.center("abc", 7, '*'));
        assertEquals("*abc**", Strings.center("abc", 6, '*'));
        assertEquals("abc", Strings.center("abc", 3, '*'));
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
    public void testC507_padAndCenterSurfaceOutOfMemoryErrorLikeRepeat() {
        assertThrows(OutOfMemoryError.class, () -> Strings.padStart("a", Integer.MAX_VALUE, "xy"));
        assertThrows(OutOfMemoryError.class, () -> Strings.padEnd("a", Integer.MAX_VALUE, "xy"));
        assertThrows(OutOfMemoryError.class, () -> Strings.center("a", Integer.MAX_VALUE, "xy"));
        assertThrows(OutOfMemoryError.class, () -> Strings.repeat("xy", Integer.MAX_VALUE));
    }

    // chomp

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
    public void testChompEach() {
        String[] array = { "a\n", "b\r\n", "c" };
        Strings.chompEach(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testChompEach_EdgeCases() {
        String[] array = { "abc\n", "def\r\n", "ghi" };
        Strings.chompEach(array);
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
        assertEquals("ghi", array[2]);
    }

    // chop

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
    public void testC556_chopOfASingleCodePointIsEmpty() {
        assertEquals("", Strings.chop(SMILE));
        assertEquals("", Strings.chop("a"));
        assertEquals("", Strings.chop("\r\n"));
        assertEquals("", Strings.chop(""));
        assertNull(Strings.chop(null));
        assertEquals(SMILE, Strings.chop(SMILE + GRIN));
        assertEquals("\r", Strings.chop("\r\r\n"));
    }

    @Test
    public void testChopEach() {
        String[] array = { "abc", "xy", "z" };
        Strings.chopEach(array);
        assertArrayEquals(new String[] { "ab", "x", "" }, array);
    }

    @Test
    public void testChopEach_EdgeCases() {
        String[] array = { "abc", "de", "f" };
        Strings.chopEach(array);
        assertEquals("ab", array[0]);
        assertEquals("d", array[1]);
        assertEquals("", array[2]);
    }

    // commonPrefix

    @Test
    public void testCommonPrefix() {
        assertEquals("abc", Strings.commonPrefix("abcdef", "abcxyz"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
        assertNull(Strings.commonPrefix(null, "abc"));
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
    public void testCommonPrefix_Multiple() {
        assertEquals("abc", Strings.commonPrefix("abcdef", "abcxyz", "abc123"));
        assertEquals("", Strings.commonPrefix("abc", "xyz", "123"));
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

    // commonSuffix

    @Test
    public void testCommonSuffix() {
        assertEquals("xyz", Strings.commonSuffix("abcxyz", "defxyz"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
        assertNull(Strings.commonSuffix(null, "abc"));
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
    public void testCommonSuffix_Multiple() {
        assertEquals("xyz", Strings.commonSuffix("abcxyz", "defxyz", "123xyz"));
        assertEquals("", Strings.commonSuffix("abc", "xyz", "123"));
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
    public void testCompare() {
        // normal
        assertEquals(0, Strings.compare("abc", "abc"));
        assertTrue(Strings.compare("abc", "def") < 0);
        assertTrue(Strings.compare("def", "abc") > 0);
        // case-sensitive: lowercase > uppercase
        assertTrue(Strings.compare("abc", "ABC") > 0);
        assertNotEquals(0, Strings.compare("abc", "ABC"));
        // null/empty edge
        assertEquals(-1, Strings.compare(null, "abc"));
        assertEquals(1, Strings.compare("abc", null));
        assertEquals(0, Strings.compare(null, null));
        assertTrue(Strings.compare("", "a") < 0);
        assertEquals(0, Strings.compare("", ""));
        // consistency with String.compareTo
        assertEquals("abc".compareTo("abd"), Strings.compare("abc", "abd"));
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
    public void testCsOrdinalConstantMatchesItsName() {
        assertNotNull(cs.ordinal);
        assertEquals("ordinal", cs.ordinal);
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
    public void testEncoders_DecodeOnlyCharsetThrows() {
        final Charset cs = decodeOnlyCharset();
        assumeTrue(cs != null, "this JDK has no decode-only charset");

        // Non-strict and strict paths must fail the same way: the JDK's own String.getBytes(Charset) throws UOE.
        assertThrows(UnsupportedOperationException.class, () -> "a".getBytes(cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.getBytes("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.getBytesStrict("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64EncodeString("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64EncodeStringStrict("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64UrlEncodeString("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64UrlEncodeStringStrict("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64MimeEncodeString("a", cs));

        // getBytes* consult the charset for an empty (non-null) string too, exactly like String.getBytes.
        assertThrows(UnsupportedOperationException.class, () -> "".getBytes(cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.getBytes("", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.getBytesStrict("", cs));
    }

    @Test
    public void testEncoders_PrimaryFirstShortCircuitsDecodeOnlyCharset() {
        final Charset cs = decodeOnlyCharset();
        assumeTrue(cs != null, "this JDK has no decode-only charset");

        assertNull(Strings.getBytes(null, cs));
        assertNull(Strings.getBytesStrict(null, cs));

        for (final String s : new String[] { null, "" }) {
            assertEquals("", Strings.base64EncodeString(s, cs));
            assertEquals("", Strings.base64EncodeStringStrict(s, cs));
            assertEquals("", Strings.base64UrlEncodeString(s, cs));
            assertEquals("", Strings.base64UrlEncodeStringStrict(s, cs));
            assertEquals("", Strings.base64MimeEncodeString(s, cs));
        }
    }

    @Test
    public void testEncoders_UnmappableCharacters() {
        // The strict contract for a charset that CAN encode is unchanged: unmappable -> IAE, lenient -> replaced.
        assertThrows(IllegalArgumentException.class, () -> Strings.getBytesStrict("é", StandardCharsets.US_ASCII));
        assertArrayEquals(new byte[] { '?' }, Strings.getBytes("é", StandardCharsets.US_ASCII));
        assertEquals("Pw==", Strings.base64EncodeString("é", StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64EncodeStringStrict("é", StandardCharsets.US_ASCII));
    }

    @Test
    public void testEquals() {
        assertTrue(Strings.equals("", ""));
        assertTrue(Strings.equals("test", "test"));
        assertTrue(Strings.equals("Hello World", "Hello World"));
        assertTrue(Strings.equals("abc", "abc"));
        assertTrue(Strings.equals(null, null));

        assertFalse(Strings.equals("test", "TEST"));
        assertFalse(Strings.equals("hello", "world"));
        assertFalse(Strings.equals("abc", "def"));
        assertFalse(Strings.equals("abc", "ABC"));
        assertFalse(Strings.equals("abc", "xyz"));
        assertFalse(Strings.equals("Hello", "hello"));
        assertFalse(Strings.equals("TEST", "test"));
        assertFalse(Strings.equals(null, "test"));
        assertFalse(Strings.equals("test", null));
        assertFalse(Strings.equals("abc", null));
        assertFalse(Strings.equals(null, "abc"));
    }

    @Test
    public void testEqualsAny() {
        assertTrue(Strings.equalsAny("abc", "abc", "xyz"));
        assertTrue(Strings.equalsAny("test", "hello", "test", "world"));
        assertTrue(Strings.equalsAny("abc", "abc"));
        assertTrue(Strings.equalsAny(null, "hello", null, "world"));
        assertTrue(Strings.equalsAny("test", "TEST", "test"));
        assertTrue(Strings.equalsAny("abc", "xyz", "abc", "def"));

        assertFalse(Strings.equalsAny("abc", "xyz", "123"));
        assertFalse(Strings.equalsAny(null, "abc", "xyz"));
        assertFalse(Strings.equalsAny("test"));
        assertFalse(Strings.equalsAny("test", (String[]) null));
        assertFalse(Strings.equalsAny("test", "hello", "world", "abc"));
        assertFalse(Strings.equalsAny("xyz", "a", "b", "c"));
        assertFalse(Strings.equalsAny("test", "TEST", "Hello"));
        assertFalse(Strings.equalsAny("abc", "xyz", "def"));
        assertFalse(Strings.equalsAny(null, "abc"));
        assertFalse(Strings.equalsAny("abc"));
    }



 // IDEOGRAPHIC SPACE (Java whitespace)

    /**
     * The three non-breaking Unicode space separators are exactly the code points where
     * {@code isSpaceChar} and {@code isWhitespace} disagree, so they are exactly the ones
     * {@code normalizeSpace} collapses and {@code strip} keeps.
     */
    @Test
    public void testExactlyThreeNonBreakingSpaceSeparatorsExist() {
        final StringBuilder divergent = new StringBuilder();
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            if (Character.isSpaceChar(cp) && !Character.isWhitespace(cp)) {
                divergent.append(String.format("U+%04X ", cp));
            }
        }
        assertEquals("U+00A0 U+2007 U+202F ", divergent.toString());
    }

    // firstNonEmpty

    @Test
    public void testFirstNonEmpty() {
        assertEquals("test", Strings.firstNonEmpty("", "test", "another"));
        assertEquals("first", Strings.firstNonEmpty("first", "second"));
        assertNull(Strings.firstNonEmpty("", "", null));
        assertNull(Strings.firstNonEmpty());
    }

    @Test
    public void testFirstNonEmpty_charSequence() {
        final StringBuilder empty = new StringBuilder();
        final StringBuilder hello = new StringBuilder("hello");
        final StringBuilder world = new StringBuilder("world");

        assertSame(hello, Strings.firstNonEmpty(hello, world));
        assertSame(world, Strings.firstNonEmpty(empty, world));
        assertSame(world, Strings.firstNonEmpty(null, world));
        assertNull(Strings.firstNonEmpty(empty, empty));
        assertNull(Strings.firstNonEmpty((StringBuilder) null, (StringBuilder) null));

        assertSame(hello, Strings.firstNonEmpty(hello, world, empty));
        assertSame(world, Strings.firstNonEmpty(empty, world, hello));
        assertSame(hello, Strings.firstNonEmpty(empty, null, hello));
        assertNull(Strings.firstNonEmpty(empty, null, empty));

        assertSame(world, Strings.firstNonEmpty(new StringBuilder[] { empty, null, world, hello }));
        assertNull(Strings.firstNonEmpty(new StringBuilder[] { empty, null }));
        assertNull(Strings.firstNonEmpty((StringBuilder[]) null));

        assertSame(world, Strings.firstNonEmpty(Arrays.asList(empty, null, world, hello)));
        assertNull(Strings.firstNonEmpty(Arrays.asList(empty, null)));
        assertNull(Strings.firstNonEmpty((Iterable<StringBuilder>) null));
        assertNull(Strings.firstNonEmpty(new ArrayList<StringBuilder>()));
    }

    @Test
    public void testFirstNonEmpty_Iterable() {
        assertNull(Strings.firstNonEmpty((Iterable<String>) null));
        assertNull(Strings.firstNonEmpty(new ArrayList<>()));
        assertEquals("hello", Strings.firstNonEmpty(Arrays.asList("", null, "hello")));
        assertNull(Strings.firstNonEmpty(Arrays.asList("", null, "")));
    }

    @Test
    public void testFirstNonEmpty_ThreeArgs() {
        assertEquals("hello", Strings.firstNonEmpty("hello", "world", "!"));
        assertEquals("world", Strings.firstNonEmpty("", "world", "!"));
        assertEquals("!", Strings.firstNonEmpty("", "", "!"));
        assertEquals("!", Strings.firstNonEmpty(null, null, "!"));
        assertNull(Strings.firstNonEmpty("", "", ""));
        assertNull(Strings.firstNonEmpty(null, null, null));
    }

    @Test
    public void testFirstNonEmpty_TwoArgs() {
        assertEquals("hello", Strings.firstNonEmpty("hello", "world"));
        assertEquals("world", Strings.firstNonEmpty("", "world"));
        assertEquals("world", Strings.firstNonEmpty(null, "world"));
        assertNull(Strings.firstNonEmpty("", ""));
        assertNull(Strings.firstNonEmpty(null, null));
    }

    @Test
    public void testFirstNonEmpty_VarArgs() {
        assertNull(Strings.firstNonEmpty());
        assertNull(Strings.firstNonEmpty((String[]) null));
        assertNull(Strings.firstNonEmpty(null, null, null));
        assertEquals(" ", Strings.firstNonEmpty(null, "", " "));
        assertEquals("abc", Strings.firstNonEmpty("abc"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz"));
        assertEquals("xyz", Strings.firstNonEmpty("", "xyz"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz", "abc"));
    }

    @Test
    public void testFirstNonEmpty_VarargsAllEmpty() {
        assertNull(Strings.firstNonEmpty(null, null, null, null));
        assertNull(Strings.firstNonEmpty("", null, "", null));
        assertEquals("x", Strings.firstNonEmpty(null, "", "x", "y"));
    }

    @Test
    public void testFirstNonEmptyBlank_agreeWithN() {
        final List<List<CharSequence>> inputs = new ArrayList<>();
        inputs.add(new ArrayList<>());
        inputs.add(Arrays.asList((CharSequence) null));
        inputs.add(Arrays.asList("", new StringBuilder()));
        inputs.add(Arrays.asList("", new StringBuilder("  "), "x"));
        inputs.add(Arrays.asList(new StringBuffer("  "), (CharSequence) null, new StringBuilder("y")));

        for (final List<CharSequence> input : inputs) {
            final CharSequence[] array = input.toArray(new CharSequence[0]);

            assertEquals(CommonUtil.firstNonEmpty(input).orElseNull(), Strings.firstNonEmpty(input), input.toString());
            assertEquals(CommonUtil.firstNonBlank(input).orElseNull(), Strings.firstNonBlank(input), input.toString());
            assertEquals(CommonUtil.firstNonEmpty(array).orElseNull(), Strings.firstNonEmpty(array), input.toString());
            assertEquals(CommonUtil.firstNonBlank(array).orElseNull(), Strings.firstNonBlank(array), input.toString());
        }
    }

    @Test
    public void testFirstNonEmptyBlank_heterogeneousCharSequences() {
        final CharSequence buffer = new StringBuffer("  ");
        final CharSequence builder = new StringBuilder("built");
        final List<CharSequence> mixed = Arrays.asList("", null, buffer, builder, "tail");

        assertSame(buffer, Strings.firstNonEmpty(mixed));
        assertSame(builder, Strings.firstNonBlank(mixed));

        assertSame(buffer, Strings.firstNonEmpty(mixed.toArray(new CharSequence[0])));
        assertSame(builder, Strings.firstNonBlank(mixed.toArray(new CharSequence[0])));

        // Mixing the argument types in a fixed-arity call is fine; the result is their common supertype.
        final CharSequence firstOfTwo = Strings.firstNonEmpty(new StringBuilder(), "second");
        assertEquals("second", firstOfTwo);

        // Iterable<? extends T> - a List of a subtype binds without an explicit witness.
        final List<StringBuilder> builders = Arrays.asList(new StringBuilder(), new StringBuilder("x"));
        final StringBuilder found = Strings.firstNonEmpty(builders);
        assertEquals("x", found.toString());
    }

    @Test
    public void testFirstNonEmptyBlank_stringInferenceUnchanged() {
        // Each of these compiles only while T infers to String rather than to CharSequence.
        final String twoArgs = Strings.firstNonEmpty("", "b");
        final String threeArgs = Strings.firstNonEmpty("", "", "c");
        final String varArgs = Strings.firstNonEmpty("", "", "", "d");
        final String iterable = Strings.firstNonEmpty(Arrays.asList("", "e"));
        assertEquals("b", twoArgs);
        assertEquals("c", threeArgs);
        assertEquals("d", varArgs);
        assertEquals("e", iterable);

        final String blankTwo = Strings.firstNonBlank(" ", "b");
        final String blankThree = Strings.firstNonBlank(" ", " ", "c");
        final String blankVar = Strings.firstNonBlank(" ", " ", " ", "d");
        final String blankIterable = Strings.firstNonBlank(Arrays.asList(" ", "e"));
        assertEquals("b", blankTwo);
        assertEquals("c", blankThree);
        assertEquals("d", blankVar);
        assertEquals("e", blankIterable);

        // String literals still reach the fixed-arity overloads; the varargs form still accepts zero arguments.
        assertNull(Strings.firstNonEmpty());
        assertNull(Strings.firstNonBlank());
        assertSame("only", Strings.firstNonEmpty("only"));
    }

    @Test
    public void testFirstNonEmptyIterable() {
        assertEquals("a", Strings.firstNonEmpty(list(null, "", "a", "b")));
        assertNull(Strings.firstNonEmpty(list(null, "")));
        assertNull(Strings.firstNonEmpty(new ArrayList<>()));
        assertNull(Strings.firstNonEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstNonEmptyVarArgs() {
        assertEquals("a", Strings.firstNonEmpty(null, "", "a", "b"));
        assertNull(Strings.firstNonEmpty(null, ""));
        assertNull(Strings.firstNonEmpty());
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
    public void testGetBytesUtf8() {
        final String source = "café世界";

        assertArrayEquals(source.getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8(source));
        assertArrayEquals(new byte[0], Strings.getBytesUtf8(""));
        assertNull(Strings.getBytesUtf8(null));
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



    // G072-01: UTF-8 strict encoding equals the reporting CharsetEncoder for well-formed input (incl. surrogate pairs)
    @Test
    public void testGetBytesUtf8Strict_matchesReportingEncoder() throws Exception {
        final String[] inputs = { "a", "?", "???", "\u0000", "abc 123 ~\u007F", "caf\u00E9", "\u00FF\u0100", "\u4E2D\u6587\u5B57", "\uD83D\uDE00",
                "x\uD83D\uDE00y\uD801\uDC00", "\uFFFD", "\uFFFF\uFFFE", "\u07FF\u0800", "The quick brown fox".repeat(500), "\u4E2D".repeat(3000) + "a",
                "a?\uD83D\uDE00", "?".repeat(50) + "\u00E9" };

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

    // lengthOfCommonPrefix

    @Test
    public void testLengthOfCommonPrefix() {
        assertEquals(3, Strings.lengthOfCommonPrefix("abcdef", "abcxyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix(null, "abc"));
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
    public void testLengthOfCommonPrefix_EdgeCases() {
        assertEquals(0, Strings.lengthOfCommonPrefix(null, null));
        assertEquals(0, Strings.lengthOfCommonPrefix(null, "abc"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", null));
        assertEquals(3, Strings.lengthOfCommonPrefix("abc", "abcdef"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", "xyz"));
        assertEquals(2, Strings.lengthOfCommonPrefix("abc", "abx"));
    }

    // lengthOfCommonSuffix

    @Test
    public void testLengthOfCommonSuffix() {
        assertEquals(3, Strings.lengthOfCommonSuffix("abcxyz", "defxyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix(null, "abc"));
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
    public void testMoveRange() {
        assertEquals("cdeabfg", Strings.moveRange("abcdefg", 2, 5, 0));
        assertEquals("abfgcde", Strings.moveRange("abcdefg", 2, 5, 4));
        assertEquals("abcdefg", Strings.moveRange("abcdefg", 2, 5, 2));
        assertEquals("abcdefg", Strings.moveRange("abcdefg", 3, 3, 0));
        assertNull(Strings.moveRange(null, 0, 0, 0));
        assertEquals("", Strings.moveRange("", 0, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.moveRange(null, 0, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.moveRange(null, 0, 1, 2));
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
    public void testPrependIfMissing() {
        assertEquals("http://abc", Strings.prependIfMissing("abc", "http://"));
        assertEquals("http://abc", Strings.prependIfMissing("http://abc", "http://"));
        assertNull(Strings.prependIfMissing(null, "http://"));
    }

    @Test
    public void testQuoteEscaped() {
        assertEquals("\\\"hello\\\"", Strings.escapeQuotes("\"hello\""));
        assertEquals("abc", Strings.escapeQuotes("abc"));
        assertNull(Strings.escapeQuotes(null));
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
    public void testQuoteEscaped_WithCustomChar() {
        String result = Strings.escapeQuotes("hello 'world'", '\'');
        assertNotNull(result);
    }

    @Test
    public void testQuoteEscaped_WithQuoteChar() {
        assertEquals("\\'hello\\'", Strings.escapeQuotes("'hello'", '\''));
        assertEquals("abc", Strings.escapeQuotes("abc", '\''));
        assertNull(Strings.escapeQuotes(null, '\''));
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
    public void testQuoteEscapedWithChar() {
        assertEquals("ab\\\"c", Strings.escapeQuotes("ab\"c", '"'));
        assertEquals("ab'c", Strings.escapeQuotes("ab'c", '"')); // ' not escaped
        assertNull(Strings.escapeQuotes(null, '"'));
    }

    @Test
    public void test_RegExUtil() {
        {
            String ret = "abc".replace("a", "");
            assertEquals("bc", ret);

            ret = RegExUtil.replaceFirst("any", ".", (String) null);
            assertEquals("ny", ret);

            ret = RegExUtil.replaceAll("abc", ".", (String) null);
            assertEquals("", ret);
        }

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
    public void testStrUtil_OperatorMirrorsClamp() {
        assertEquals(Optional.of("lo"), StrUtil.substring("hello", end -> end - 2, 99));
        assertEquals("lo", StrUtil.substringOrElse("hello", end -> end - 2, 99, "default"));
        assertEquals("lo", StrUtil.substringOrElseItself("hello", end -> end - 2, 99));
        assertEquals("default", StrUtil.substringOrElse("hello", end -> end + 1, 99, "default"));
        assertSame("hello", StrUtil.substringOrElseItself("hello", end -> end + 1, 99));
        assertEquals(Optional.empty(), StrUtil.substring("hello", end -> end + 1, 99));
    }

    @Test
    public void testStrUtilOrElse_CharDelimiterOverloads() {
        assertEquals("world", StrUtil.substringAfterOrElse("hello.world", '.', "default"));
        assertEquals("default", StrUtil.substringAfterOrElse("hello", '.', "default"));
        assertEquals("", StrUtil.substringAfterOrElse("hello.", '.', "default"));
        assertEquals("default", StrUtil.substringAfterOrElse(null, '.', "default"));

        assertEquals("java", StrUtil.substringAfterLastOrElse("hello.world.java", '.', "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse("hello", '.', "default"));
        assertEquals("", StrUtil.substringAfterLastOrElse("hello.world.", '.', "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse(null, '.', "default"));

        assertEquals("hello", StrUtil.substringBeforeOrElse("hello.world", '.', "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse("hello", '.', "default"));
        assertEquals("", StrUtil.substringBeforeOrElse(".world", '.', "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse(null, '.', "default"));

        assertEquals("hello.world", StrUtil.substringBeforeLastOrElse("hello.world.java", '.', "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse("hello", '.', "default"));
        assertEquals("", StrUtil.substringBeforeLastOrElse(".world", '.', "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse(null, '.', "default"));

        // the char overload must agree with the String overload for a single-character delimiter
        assertEquals(StrUtil.substringAfterOrElse("a.b.c", ".", "x"), StrUtil.substringAfterOrElse("a.b.c", '.', "x"));
        assertEquals(StrUtil.substringBeforeLastOrElse("a.b.c", ".", "x"), StrUtil.substringBeforeLastOrElse("a.b.c", '.', "x"));
    }

    @Test
    public void suffixArrayBound_subtractsOffsetToAvoidIntOverflow() {
        final int index = 1 << 30;
        final int offset = 1 << 30;
        final int len = (1 << 30) + 1;
        assertTrue(index + offset < len);
        assertFalse(index < len - offset);
    }

    @Test
    public void test_surrogate() {
        assertTrue(Character.isLowSurrogate(Character.MIN_LOW_SURROGATE));
        assertTrue(Character.isLowSurrogate(Character.MAX_LOW_SURROGATE));
        assertTrue(Character.isHighSurrogate(Character.MIN_HIGH_SURROGATE));
        assertTrue(Character.isHighSurrogate(Character.MAX_HIGH_SURROGATE));
    }

    @Test
    public void testC573_tokenContainingTheDelimiterNeverMatches() {
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfToken("a,b", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("x,A,B,y", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("A,B", "a,b", ","));
        assertEquals(2, StrUtil.indexOfToken("x,a,b,y", "a", ","));
    }

    // truncate



    // C-021: documented surrogate / overlap rules that had no test
    @Test
    public void testC021_documentedSurrogateAndOverlapRules() {
        assertEquals("", Strings.truncate(EMOJI + "x", 1));
        assertEquals("x", Strings.truncate(EMOJI + "x", 1, 2));
        assertEquals("", Strings.truncate(EMOJI + "x", 0, 1));
        assertEquals(EMOJI, Strings.truncate(EMOJI + EMOJI, 1, 2));
        assertEquals("a", Strings.chop("a" + EMOJI));
        assertEquals("", Strings.chop(EMOJI));

        assertEquals("aaaaa", Strings.wrapIfMissing("a", "aa", "aa"));
        assertEquals("aaaa", Strings.wrapIfMissing("aa", "aa", "aa"));
        assertEquals("aaaaa", Strings.wrapIfMissing("aaa", "aa", "aa"));
        assertEquals("aaaa", Strings.wrapIfMissing("aaaa", "aa", "aa"));
        assertEquals("a", Strings.unwrap("aaaaa", "aa", "aa"));
        assertEquals("aa", Strings.unwrap("aa", "aa", "aa"));
        assertEquals("aaa", Strings.unwrap("aaa", "aa", "aa"));
        assertEquals("", Strings.unwrap("aaaa", "aa", "aa"));

        assertEquals(Strings.indexOf("abcabc", 'b', 0), Strings.indexOf("abcabc", 'b', -5));
        assertEquals(1, Strings.indexOf("abcabc", 'b', Integer.MIN_VALUE));
        assertEquals(-1, Strings.indexOf("abcabc", 'b', 6));
    }

    @Test
    public void testTruncate() {
        assertEquals("abc", Strings.truncate("abcdef", 3));
        assertEquals("abc", Strings.truncate("abc", 5));
        assertNull(Strings.truncate(null, 3));
    }

    @Test
    public void testTruncate_PreservesSurrogatePairs() {
        final String emoji = "\uD83D\uDE00";

        assertEquals("", Strings.truncate(emoji + "a", 1));
        assertEquals(emoji, Strings.truncate(emoji + "a", 2));
        assertEquals("ab", Strings.truncate(emoji + "abc", 1, 2));
    }

    @Test
    public void testTruncate_WithOffset() {
        assertEquals("cde", Strings.truncate("abcdef", 2, 3));
        assertEquals("abc", Strings.truncate("abc", 0, 5));
    }

    @Test
    public void testC516_truncateOffsetAndStripCharsContracts() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", -1, 2)).getMessage().contains("offset"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.truncateEach(new String[] { "abc" }, -1, 2)).getMessage().contains("offset"));
        assertEquals("abc", Strings.stripStart("abc", ""));
        assertEquals("abc", Strings.stripEnd("abc", ""));
        assertEquals(" abc ", Strings.stripStart(" abc ", ""));
        assertEquals("abc ", Strings.stripStart(" abc ", null));
        assertEquals(" abc", Strings.stripEnd(" abc ", null));
    }

    @Test
    public void testTruncateWithOffset() {
        assertEquals("cde", Strings.truncate("abcdefg", 2, 3));
        assertEquals("", Strings.truncate("abc", 3, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", -1, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", 0, -1));
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

    // unwrap

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
    public void testValidSurrogatePairAt() {
        // Emoji uses surrogate pair in Java
        String emoji = "\uD83D\uDE00"; // 😀
        assertTrue(Strings.validSurrogatePairAt(emoji, 0));
        assertFalse(Strings.validSurrogatePairAt(emoji, 1));
        assertFalse(Strings.validSurrogatePairAt("hello", 0));
        assertFalse(Strings.validSurrogatePairAt("hello", -1));
    }

    @Test
    public void testValidSurrogatePairAtNullStringReturnsFalse() {
        assertFalse(Strings.validSurrogatePairAt(null, 0));
    }

    // wrap

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

    // wrapIfMissing



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
    public void testC515_flaggedCommonsDivergences() {
        assertEquals("xx", Strings.wrap("", "x"));
        assertEquals("xx", Strings.wrapIfMissing("", "x"));
        assertEquals("[]", Strings.wrapIfMissing("", "[", "]"));
        assertEquals("", StringUtils.wrap("", "x"));
        assertEquals("", StringUtils.wrapIfMissing("", "x"));

        assertEquals("aaaa", Strings.wrapIfMissing("aa", "aa"));
        assertEquals("\"\"", Strings.wrapIfMissing("\"", "\""));
        assertEquals("aaaa", Strings.wrapIfMissing("aa", "aa", "aa"));
        assertEquals("aa", StringUtils.wrapIfMissing("aa", "aa"));
        assertEquals("\"", StringUtils.wrapIfMissing("\"", "\""));

        assertEquals("a", Strings.chop("a" + EMOJI));
        assertEquals("a\uD83D", StringUtils.chop("a" + EMOJI));

        assertTrue(Strings.isMixedCase("\u01C5a"));
        assertFalse(StringUtils.isMixedCase("\u01C5a"));

        assertEquals("\u0132", Strings.stripAccents("\u0132"));
        assertEquals("IJ", StringUtils.stripAccents("\u0132"));
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

}
