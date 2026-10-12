package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.base64Encode;
import static com.landawn.abacus.util.Strings.base64EncodeString;
import static com.landawn.abacus.util.Strings.isBase64;
import static com.landawn.abacus.util.Strings.isBase64Alphabet;
import static com.landawn.abacus.util.Strings.substring;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Strings encoding contracts, including regression cases. */
public class StringsEncodingTest extends StringsTestSupport {



    // D-2: isBase64Alphabet(byte) derived from the per-dialect predicate

    /** The private lookup table that used to back {@code isBase64Alphabet(byte)}, transcribed verbatim. */
    private static final byte[] LEGACY_DECODE_TABLE = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, //
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, //
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, 62, -1, 63, //
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, //
            -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, //
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, 63, //
            -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, //
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 };

    private static String g072ReportingUtf8Decode(final byte[] bytes) throws java.nio.charset.CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString();
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
        assertEquals(originalUrl, Strings.base64UrlDecodeToString(urlEncoded));
    }

    @Test
    public void testBase64Decode() {
        byte[] decoded = Strings.base64Decode("SGVsbG8gV29ybGQ=");
        assertArrayEquals("Hello World".getBytes(), decoded);
        assertArrayEquals("".getBytes(), Strings.base64Decode((String) null));
        assertArrayEquals("Hello World".getBytes(StandardCharsets.US_ASCII), Strings.base64Decode("SGVsbG8gV29ybGQ=".getBytes(StandardCharsets.US_ASCII)));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, Strings.base64Decode((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64Decode("SGV!".getBytes(StandardCharsets.US_ASCII)));
    }

    // base64DecodeToString

    @Test
    public void testBase64DecodeToString() {
        String decoded = Strings.base64DecodeToString("SGVsbG8gV29ybGQ=");
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64DecodeToString(null));
    }

    @Test
    public void testBase64DecodeToString_WithCharset() {
        String encoded = Strings.base64EncodeString("hello");
        String decoded = Strings.base64DecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello", decoded);
    }

    @Test
    public void testBase64DecodeToStringWithCharset() {
        assertEquals("Hello", Strings.base64DecodeToString("SGVsbG8=", StandardCharsets.UTF_8));
        assertEquals("Hello", Strings.base64DecodeToString("SGVsbG8=", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64DecodeToString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64DecodeToString(null, StandardCharsets.UTF_8));
        assertEquals("", Strings.base64DecodeToString("", null));
        assertEquals("", Strings.base64DecodeToString(null, null));
        assertThrows(NullPointerException.class, () -> Strings.base64DecodeToString("eA==", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64DecodeToString("!", null));
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

    @Test
    public void testBase64Encode() {
        byte[] data = "Hello World".getBytes();
        String encoded = Strings.base64Encode(data);
        assertNotNull(encoded);
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64Encode((byte[]) null));
    }

    // base64EncodeString

    @Test
    public void testBase64EncodeString() {
        String encoded = Strings.base64EncodeString("Hello World");
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64EncodeString(null));
    }

    @Test
    public void testBase64EncodeString_WithCharset() {
        String encoded = Strings.base64EncodeString("hello", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(encoded);
        assertEquals("hello", Strings.base64DecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testBase64EncodeStringWithCharset() {
        assertEquals("SGVsbG8=", Strings.base64EncodeString("Hello", StandardCharsets.UTF_8));
        assertEquals("SGVsbG8=", Strings.base64EncodeString("Hello", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64EncodeString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64EncodeString(null, StandardCharsets.UTF_8));
        assertEquals("", Strings.base64EncodeString("", null));
        assertEquals("", Strings.base64EncodeString(null, null));
        assertThrows(NullPointerException.class, () -> Strings.base64EncodeString("x", null));
    }

    @Test
    public void testBase64MimeDecode() {
        assertArrayEquals("Hello World".getBytes(StandardCharsets.UTF_8), Strings.base64MimeDecode("SGVsbG8gV29ybGQ="));
        assertArrayEquals("Hello".getBytes(StandardCharsets.US_ASCII), Strings.base64MimeDecode("SGVs bG8="));
        assertArrayEquals("Hello".getBytes(StandardCharsets.US_ASCII), Strings.base64MimeDecode("SGVs\r\nbG8="));
        assertArrayEquals("".getBytes(), Strings.base64MimeDecode(""));
        assertArrayEquals("".getBytes(), Strings.base64MimeDecode((String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64MimeDecode("===="));

        assertArrayEquals("Hello World".getBytes(StandardCharsets.US_ASCII), Strings.base64MimeDecode("SGVsbG8gV29ybGQ=".getBytes(StandardCharsets.US_ASCII)));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, Strings.base64MimeDecode((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64MimeDecode("====".getBytes(StandardCharsets.US_ASCII)));

        // The MIME decoder is more lenient than isBase64Mime: non-alphabet characters are skipped.
        assertArrayEquals("Hello".getBytes(StandardCharsets.US_ASCII), Strings.base64MimeDecode("SGV!sbG8="));
        assertEquals("Hello", Strings.base64MimeDecodeToString("SGV!sbG8="));
        assertEquals("", Strings.base64MimeDecodeToString(null, null));
        assertEquals("", Strings.base64MimeDecodeToString("", null));
        assertThrows(NullPointerException.class, () -> Strings.base64MimeDecodeToString("SGVsbG8=", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64MimeDecodeToString("====", null));

        // Round-trip, including folded output.
        final byte[] data = new byte[60];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        assertArrayEquals(data, Strings.base64MimeDecode(Strings.base64MimeEncode(data)));
    }

    @Test
    public void testBase64MimeEncode() {
        assertEquals("SGVsbG8gV29ybGQ=", Strings.base64MimeEncode("Hello World".getBytes(StandardCharsets.UTF_8)));
        assertEquals("", Strings.base64MimeEncode(new byte[0]));
        assertEquals("", Strings.base64MimeEncode(null));

        assertEquals("SGVsbG8gV29ybGQ=", Strings.base64MimeEncodeString("Hello World"));
        assertEquals("Hello World", Strings.base64MimeDecodeToString(Strings.base64MimeEncodeString("Hello World")));
        assertEquals("Test123", Strings.base64MimeDecodeToString(Strings.base64MimeEncodeString("Test123", StandardCharsets.UTF_16), StandardCharsets.UTF_16));
        assertEquals("", Strings.base64MimeEncodeString(null, null));
        assertEquals("", Strings.base64MimeEncodeString("", null));
        assertThrows(NullPointerException.class, () -> Strings.base64MimeEncodeString("x", null));

        // RFC 2045 folding: 60 bytes encode to 80 Base64 chars, split 76 + "\r\n" + 4
        final String folded = Strings.base64MimeEncode(new byte[60]);
        assertEquals(76 + 2 + 4, folded.length());
        assertEquals("\r\n", folded.substring(76, 78));
        assertTrue(Strings.isBase64Mime(folded));
    }

    @Test
    public void testBase64RoundTrip() {
        String original = "Hello, World! \u00E9\u00FC";
        String encodedStr = base64EncodeString(original);
        assertEquals(original, Strings.base64DecodeToString(encodedStr));
    }

    @Test
    public void testBase64UrlDecode() {
        String encoded = Strings.base64UrlEncode("Hello+World/Test".getBytes());
        byte[] decoded = Strings.base64UrlDecode(encoded);
        assertArrayEquals("Hello+World/Test".getBytes(), decoded);
        assertArrayEquals("".getBytes(), Strings.base64UrlDecode(""));
        assertArrayEquals("".getBytes(), Strings.base64UrlDecode((String) null));
        assertArrayEquals("Hello+World/Test".getBytes(), Strings.base64UrlDecode(encoded.getBytes(StandardCharsets.US_ASCII)));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, Strings.base64UrlDecode((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlDecode("+/8=".getBytes(StandardCharsets.US_ASCII)));
    }

    // base64UrlDecodeToString

    @Test
    public void testBase64UrlDecodeToString() {
        String encoded = Strings.base64UrlEncode("Hello World".getBytes());
        String decoded = Strings.base64UrlDecodeToString(encoded);
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64UrlDecodeToString(null));
    }

    @Test
    public void testBase64UrlDecodeToString_EdgeCases() {
        assertEquals("", Strings.base64UrlDecodeToString(null));
        String encoded = Strings.base64UrlEncode("Test String".getBytes(StandardCharsets.UTF_8));
        assertEquals("Test String", Strings.base64UrlDecodeToString(encoded));
    }

    @Test
    public void testBase64UrlDecodeToString_WithCharset() {
        byte[] data = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String encoded = Strings.base64UrlEncode(data);
        String decoded = Strings.base64UrlDecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello", decoded);
    }

    @Test
    public void testBase64UrlDecodeToStringWithCharset() {
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8", StandardCharsets.UTF_8));
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64UrlDecodeToString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64UrlDecodeToString(null, StandardCharsets.UTF_8));
        assertEquals("", Strings.base64UrlDecodeToString("", null));
        assertEquals("", Strings.base64UrlDecodeToString(null, null));
        assertThrows(NullPointerException.class, () -> Strings.base64UrlDecodeToString("eA", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64UrlDecodeToString("+", null));
    }

    @Test
    public void testBase64UrlEncode() {
        byte[] data = "Hello+World/Test".getBytes();
        String encoded = Strings.base64UrlEncode(data);
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertEquals("", Strings.base64UrlEncode((byte[]) null));
    }

    @Test
    public void testBase64UrlEncode_EdgeCases() {
        assertEquals("", Strings.base64UrlEncode(null));
        String encoded = Strings.base64UrlEncode("Hello World".getBytes(StandardCharsets.UTF_8));
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        byte[] decoded = Strings.base64UrlDecode(encoded);
        assertEquals("Hello World", new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    public void testBase64UrlEncodeString() {
        final String value = "Hello+/ 世界";

        assertEquals("", Strings.base64UrlEncodeString(null));
        assertEquals("", Strings.base64UrlEncodeString(""));
        assertEquals(value, Strings.base64UrlDecodeToString(Strings.base64UrlEncodeString(value)));
        assertEquals(value, Strings.base64UrlDecodeToString(Strings.base64UrlEncodeString(value, StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertFalse(Strings.base64UrlEncodeString(value).contains("="));
        assertThrows(NullPointerException.class, () -> Strings.base64UrlEncodeString("x", null));
    }

    @Test
    public void testC571_base64UrlEncodeStringUsesUtf8Unpadded() {
        final String input = "\u00E9?>" + SMILE;
        assertEquals(Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8)), Strings.base64UrlEncodeString(input));
        assertEquals("", Strings.base64UrlEncodeString(""));
        assertEquals("", Strings.base64UrlEncodeString(null));
    }

    // encodeUrlQuery

    @Test
    public void testEncodeUrlQuery_emptyNameWithNullValue() {
        final Map<String, Object> emptyNameNullValue = new LinkedHashMap<>();
        emptyNameNullValue.put("", null);
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(emptyNameNullValue));
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(emptyNameNullValue, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(new Object[] { "", null }));
        assertEquals("=v", Strings.encodeUrlQuery(new Object[] { "", "v" }));
    }

    @Test
    public void testEncodeUrlQuery_verbatimQueryString() {
        assertEquals("q=a b;c=d", Strings.encodeUrlQuery("q=a b;c=d"));
        assertEquals("q=a%20b&flag", Strings.encodeUrlQuery("q=a%20b&flag"));
        assertEquals("q=\u00e9", Strings.encodeUrlQuery("q=\u00e9", StandardCharsets.US_ASCII));
        assertEquals("a%26b+c", Strings.encodeUrlQuery("a&b c"));

        final Map<String, Object> nonAscii = new LinkedHashMap<>();
        nonAscii.put("q", "\u00e9");
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(nonAscii, StandardCharsets.US_ASCII));
    }

    @Test
    public void testC534_encodeUrlQueryUsesUtf8() {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("a", "\u4E2D");
        assertEquals("a=%E4%B8%AD", Strings.encodeUrlQuery(m));
        assertEquals("a=%E4%B8%AD", Strings.encodeUrlQuery(m, (Charset) null));
        assertEquals(Strings.encodeUrlQuery(m, Charsets.UTF_8), Strings.encodeUrlQuery(m));
        assertEquals("", Strings.encodeUrlQuery(null));
    }



    // C-435: finding is linear on long runs without '@'
    @Test
    public void testC435_emailFindingIsLinear() throws Exception {
        final String base64 = Strings.base64Encode(new byte[750_000]).replace("=", "");
        final long start = System.nanoTime();
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("a".repeat(1_000_000))));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("a.".repeat(500_000))));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses(base64)));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("\"" + "\\\"".repeat(500_000))));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("x@" + "a-".repeat(500_000))));
        // generous bound: quadratic behaviour took minutes on these inputs
        assertTrue(System.nanoTime() - start < 60_000_000_000L);
        assertEquals(100_000, Strings.findAllEmailAddresses("a@b.cc ".repeat(100_000)).size());
    }

    @Test
    public void testIsBase64() {
        assertTrue(Strings.isBase64("SGVsbG8="));
        assertTrue(Strings.isBase64("SGVsbG8gV29ybGQ="));
        assertTrue(Strings.isBase64("V29ybGQ="));
        assertTrue(Strings.isBase64("YWJjZGVm"));
        assertTrue(Strings.isBase64(""));
        assertTrue(isBase64(base64EncodeString("test")));
        assertTrue(Strings.isBase64("SGVsbG8=".getBytes()));
        assertTrue(Strings.isBase64("V29ybGQ=".getBytes()));
        assertTrue(Strings.isBase64(new byte[0]));
        assertTrue(isBase64("".getBytes()));

        assertFalse(Strings.isBase64("Hello@World"));
        assertFalse(Strings.isBase64("Hello World!"));
        assertFalse(Strings.isBase64("ABC!@#"));
        assertFalse(Strings.isBase64("SGVs bG8="));
        assertFalse(Strings.isBase64("not base64!!!"));
        assertFalse(Strings.isBase64((String) null));
        assertFalse(Strings.isBase64((byte[]) null));
        assertFalse(Strings.isBase64("Hello@World".getBytes()));
        assertFalse(Strings.isBase64("SGVs bG8=".getBytes()));
        assertTrue(Strings.isBase64Mime("SGVs bG8=".getBytes()));
        assertTrue(Strings.isBase64Mime("SGVs bG8="));
    }

    @Test
    public void testD2_structuralValidatorsRemainDialectSpecific() {
        // The single-byte check is deliberately the union of both alphabets; the sequence validators are not.
        assertTrue(Strings.isBase64("+/+/"));
        assertFalse(Strings.isBase64Url("+/+/"));
        assertTrue(Strings.isBase64Url("-_-_"));
        assertFalse(Strings.isBase64("-_-_"));
        assertFalse(Strings.isBase64("-_+/"));
        assertFalse(Strings.isBase64Url("-_+/"));
        // MIME tolerates whitespace, the others do not.
        assertTrue(Strings.isBase64Mime("A B C D"));
        assertFalse(Strings.isBase64("A B C D"));
    }

    @Test
    public void testIsBase64_CompleteStructure() {
        assertTrue(isBase64(""));
        assertTrue(isBase64("TQ=="));
        assertTrue(isBase64("TWE="));
        assertTrue(isBase64("TWFu"));
        assertTrue(isBase64("TQ"));
        assertTrue(isBase64("TWE"));
        assertTrue(isBase64("+/8="));

        assertFalse(isBase64("===="));
        assertFalse(isBase64("A"));
        assertFalse(isBase64("AA=A"));
        assertFalse(isBase64("TQ==="));
        assertFalse(isBase64("TQ==AA"));
        assertFalse(isBase64("SGVs bG8="));
        assertFalse(isBase64("-_8="));
        assertFalse(isBase64("AA+_"));

        assertTrue(isBase64("TQ==".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(isBase64("====".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(isBase64("TQ==\r\n".getBytes(StandardCharsets.US_ASCII)));
    }

    // isBase64Alphabet

    @SuppressWarnings("deprecation")
    @Test
    public void testIsBase64Alphabet() {
        assertTrue(Strings.isBase64Alphabet((byte) 'A'));
        assertTrue(Strings.isBase64Alphabet((byte) 'Z'));
        assertTrue(Strings.isBase64Alphabet((byte) 'a'));
        assertTrue(Strings.isBase64Alphabet((byte) 'z'));
        assertTrue(Strings.isBase64Alphabet((byte) '0'));
        assertTrue(Strings.isBase64Alphabet((byte) '9'));
        assertTrue(Strings.isBase64Alphabet((byte) '+'));
        assertTrue(Strings.isBase64Alphabet((byte) '/'));
        assertTrue(Strings.isBase64Alphabet((byte) '='));
        assertFalse(Strings.isBase64Alphabet((byte) '@'));
        assertFalse(Strings.isBase64Alphabet((byte) '['));
        assertFalse(Strings.isBase64Alphabet((byte) '`'));
        assertFalse(Strings.isBase64Alphabet((byte) '{'));
        assertFalse(Strings.isBase64Alphabet((byte) '!'));
        assertFalse(isBase64Alphabet((byte) '#'));
        assertFalse(isBase64Alphabet((byte) ' '));
    }

    @Test
    public void testD2_isBase64AlphabetAcceptsBothDialectsAndPadding() {
        for (final char ch : "ABYZabyz0189".toCharArray()) {
            assertTrue(Strings.isBase64Alphabet((byte) ch), "expected alphabet member: " + ch);
        }

        assertTrue(Strings.isBase64Alphabet((byte) '+'));
        assertTrue(Strings.isBase64Alphabet((byte) '/'));
        assertTrue(Strings.isBase64Alphabet((byte) '-'));
        assertTrue(Strings.isBase64Alphabet((byte) '_'));
        assertTrue(Strings.isBase64Alphabet((byte) '='));

        assertFalse(Strings.isBase64Alphabet((byte) '!'));
        assertFalse(Strings.isBase64Alphabet((byte) ' '));
        assertFalse(Strings.isBase64Alphabet((byte) '@'));
        assertFalse(Strings.isBase64Alphabet((byte) '.'));
        assertFalse(Strings.isBase64Alphabet((byte) ','));
        assertFalse(Strings.isBase64Alphabet((byte) '{'));
        assertFalse(Strings.isBase64Alphabet((byte) 0));
        // Any non-ASCII octet is negative as a byte and belongs to neither alphabet.
        assertFalse(Strings.isBase64Alphabet((byte) 0x80));
        assertFalse(Strings.isBase64Alphabet((byte) 0xFF));
    }

    @Test
    public void testD2_isBase64AlphabetUnchangedForEveryByteValue() {
        for (int v = -128; v <= 127; v++) {
            final byte octet = (byte) v;
            final boolean expected = octet == '=' || (octet >= 0 && octet < LEGACY_DECODE_TABLE.length && LEGACY_DECODE_TABLE[octet] != -1);
            assertEquals(expected, Strings.isBase64Alphabet(octet), "isBase64Alphabet((byte) " + v + ")");
        }
    }



    /**
     * C2-S1: {@code isBase64Mime} documents, in three places, that everything it accepts is decodable by
     * {@code base64MimeDecode}, and actively recommends validate-then-decode. It skipped MIME whitespace
     * wherever it appeared, including <i>between</i> the two padding characters of a final two-symbol
     * quantum - where the JDK MIME decoder requires the second {@code =} to follow the first immediately and
     * throws. A differential over 3,000 whitespace/padding/body combinations found 300 such inputs.
     */
    @Test
    public void test20260906c3_isBase64MimeRejectsWhitespaceSplittingThePadding() {
        for (final String broken : new String[] { "QQ= =", "QQ=\t=", "QQ=\r\n=", "QQ=  =", "QQ = =" }) {
            assertFalse(Strings.isBase64Mime(broken), () -> "should reject: " + broken);
            assertFalse(Strings.isBase64Mime(broken.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            assertThrows(IllegalArgumentException.class, () -> Strings.base64MimeDecode(broken));
        }

        // whitespace before and after the padding is still accepted - the decoder accepts both
        for (final String ok : new String[] { "QQ==", "SGVs bG8=", "QQ== ", "A B C D", "QUJD", "SGVsbG8=", "QQ\r\n==" }) {
            assertTrue(Strings.isBase64Mime(ok), () -> "should accept: " + ok);
            assertNotNull(Strings.base64MimeDecode(ok));
        }

        // the documented invariant itself, asserted directly
        for (final String s : new String[] { "QQ==", "QQ= =", "SGVs bG8=", "QQ== ", "QQ=\t=", "A B C D", "QUJ =", "QQ" }) {
            if (Strings.isBase64Mime(s)) {
                assertNotNull(Strings.base64MimeDecode(s), () -> "accepted but not decodable: " + s);
            }
        }
    }

    @Test
    public void testIsBase64Mime() {
        assertTrue(Strings.isBase64Mime(""));
        assertTrue(Strings.isBase64Mime(" \t\r\n"));
        assertTrue(Strings.isBase64Mime("SGVs bG8="));
        assertTrue(Strings.isBase64Mime("SGVs\r\nbG8="));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64Decode("SGVs bG8="));
        assertArrayEquals("Hello".getBytes(StandardCharsets.US_ASCII), Base64.getMimeDecoder().decode("SGVs bG8="));
        assertFalse(Strings.isBase64Mime("===="));
        assertFalse(Strings.isBase64Mime("A"));
        assertFalse(Strings.isBase64Mime("SGV!sbG8="));
        assertArrayEquals("Hello".getBytes(StandardCharsets.US_ASCII), Base64.getMimeDecoder().decode("SGV!sbG8="));
        assertFalse(Strings.isBase64Mime("SGVs-bG8="));
        assertFalse(Strings.isBase64Mime("TQ=\r\nQ"));
        assertFalse(Strings.isBase64Mime((String) null));

        assertTrue(Strings.isBase64Mime("SGVs\r\nbG8=".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Strings.isBase64Mime("SGV!sbG8=".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Strings.isBase64Mime((byte[]) null));
    }

    @Test
    public void testIsBase64Url() {
        assertTrue(Strings.isBase64Url(""));
        assertTrue(Strings.isBase64Url("-_8="));
        assertTrue(Strings.isBase64Url("-_8"));
        assertFalse(Strings.isBase64Url("+/8="));
        assertFalse(Strings.isBase64Url("AA-_+"));
        assertFalse(Strings.isBase64Url("===="));
        assertFalse(Strings.isBase64Url("A"));
        assertFalse(Strings.isBase64Url("AA=A"));
        assertFalse(Strings.isBase64Url("TQ==\n"));
        assertFalse(Strings.isBase64Url((String) null));

        assertTrue(Strings.isBase64Url("-_8=".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Strings.isBase64Url("+/8=".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Strings.isBase64Url((byte[]) null));
    }

    @Test
    public void testUrlEncode() {
        String encoded = Strings.encodeUrlQuery("Hello World");
        assertEquals("Hello+World", encoded);
        String encoded2 = Strings.encodeUrlQuery("a=b&c=d");
        assertEquals("a=b&c=d", encoded2);
        assertEquals("", Strings.encodeUrlQuery(null));
    }

    @Test
    public void testUrlEncode_WithCharset() {
        String encoded = Strings.encodeUrlQuery("hello world", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(encoded);
        assertTrue(encoded.contains("hello"));
        assertEquals("hello+world", Strings.encodeUrlQuery("hello world", null));
        assertEquals("", Strings.encodeUrlQuery(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(new Object[] { "name" }, null));

        final Map<Object, Object> nonStringKey = new LinkedHashMap<>();
        nonStringKey.put(1, "value");
        assertThrows(ClassCastException.class, () -> Strings.encodeUrlQuery(nonStringKey));
        assertThrows(ClassCastException.class, () -> Strings.encodeUrlQuery(new Object[] { 1, "value" }));

        final Map<Object, Object> nullKey = new LinkedHashMap<>();
        nullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(nullKey));
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(new Object[] { null, "value" }));

        final Map<String, Object> nullValue = new LinkedHashMap<>();
        nullValue.put("a", null);
        assertEquals("a", Strings.encodeUrlQuery(nullValue));
        assertNull(Strings.parseUrlQuery(Strings.encodeUrlQuery(nullValue)).get("a"));
        assertEquals("a", Strings.encodeUrlQuery(new Object[] { "a", null }));

        // A "null" String value is distinct from a null value and keeps its '='.
        final Map<String, Object> nullStringValue = new LinkedHashMap<>();
        nullStringValue.put("a", "null");
        assertEquals("a=null", Strings.encodeUrlQuery(nullStringValue));
        assertEquals("null", Strings.parseUrlQuery(Strings.encodeUrlQuery(nullStringValue)).get("a"));

        // An empty String value encodes as "a=" and round-trips to "".
        final Map<String, Object> emptyValue = new LinkedHashMap<>();
        emptyValue.put("a", "");
        assertEquals("a=", Strings.encodeUrlQuery(emptyValue));
        assertEquals("", Strings.parseUrlQuery(Strings.encodeUrlQuery(emptyValue)).get("a"));
    }

    @Test
    public void testUrlEncodeDecode() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "John Doe");
        params.put("city", "New York");
        String encoded = Strings.encodeUrlQuery(params);
        assertEquals("name=John+Doe&city=New+York", encoded);

        final Map<String, String> snakeKeys = new LinkedHashMap<>();
        snakeKeys.put("first_name", "Ada");
        assertEquals("first_name=Ada", Strings.encodeUrlQuery(snakeKeys));
        assertEquals("first_name=Ada", Strings.encodeUrlQuery(snakeKeys, java.nio.charset.StandardCharsets.UTF_8));

        Map<String, String> decoded = Strings.parseUrlQuery(encoded);
        assertEquals("John Doe", decoded.get("name"));
        assertEquals("New York", decoded.get("city"));
    }

    @Test
    public void testUrlEncodeDecode_EdgeCases() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("hello world", "foo=bar");
        String encoded = Strings.encodeUrlQuery(params);
        assertNotNull(encoded);
        assertTrue(encoded.contains("hello"));
        // Round-trip test
        Map<String, String> decoded = Strings.parseUrlQuery("a=1&b=2");
        assertEquals("1", decoded.get("a"));
        assertEquals("2", decoded.get("b"));
    }

}
