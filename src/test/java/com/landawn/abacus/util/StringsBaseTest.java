package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.base64EncodeString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class StringsBaseTest extends StringsTestSupport {
    @Test
    public void testBase64RoundTrip() {
        String original = "Hello, World! \u00E9\u00FC";
        String encodedStr = base64EncodeString(original);
        assertEquals(original, Strings.base64DecodeToString(encodedStr));
    }

    @Test
    public void testBase64Encode() {
        byte[] data = "Hello World".getBytes();
        String encoded = Strings.base64Encode(data);
        assertNotNull(encoded);
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64Encode((byte[]) null));
    }

    @Test
    public void testBase64EncodeString() {
        String encoded = Strings.base64EncodeString("Hello World");
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64EncodeString(null));
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
    public void testBase64EncodeString_WithCharset() {
        String encoded = Strings.base64EncodeString("hello", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(encoded);
        assertEquals("hello", Strings.base64DecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8));
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

    @Test
    public void testBase64DecodeToString_WithCharset() {
        String encoded = Strings.base64EncodeString("hello");
        String decoded = Strings.base64DecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello", decoded);
    }

    @Test
    public void testBase64DecodeToString() {
        String decoded = Strings.base64DecodeToString("SGVsbG8gV29ybGQ=");
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64DecodeToString(null));
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

    @Test
    public void testBase64UrlDecodeToString_WithCharset() {
        byte[] data = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String encoded = Strings.base64UrlEncode(data);
        String decoded = Strings.base64UrlDecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello", decoded);
    }

    @Test
    public void testBase64UrlDecodeToString() {
        String encoded = Strings.base64UrlEncode("Hello World".getBytes());
        String decoded = Strings.base64UrlDecodeToString(encoded);
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64UrlDecodeToString(null));
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
    public void testBase64UrlDecodeToString_EdgeCases() {
        assertEquals("", Strings.base64UrlDecodeToString(null));
        String encoded = Strings.base64UrlEncode("Test String".getBytes(StandardCharsets.UTF_8));
        assertEquals("Test String", Strings.base64UrlDecodeToString(encoded));
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
}
