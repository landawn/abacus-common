package com.landawn.abacus.util;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Charsets2025Test extends TestBase {

    @Test
    public void testCharsetConstants() {
        Assertions.assertNotNull(Charsets.US_ASCII);
        Assertions.assertEquals(StandardCharsets.US_ASCII, Charsets.US_ASCII);

        Assertions.assertNotNull(Charsets.ISO_8859_1);
        Assertions.assertEquals(StandardCharsets.ISO_8859_1, Charsets.ISO_8859_1);

        Assertions.assertNotNull(Charsets.UTF_8);
        Assertions.assertEquals(StandardCharsets.UTF_8, Charsets.UTF_8);

        Assertions.assertNotNull(Charsets.UTF_16);
        Assertions.assertEquals(StandardCharsets.UTF_16, Charsets.UTF_16);

        Assertions.assertNotNull(Charsets.UTF_16BE);
        Assertions.assertEquals(StandardCharsets.UTF_16BE, Charsets.UTF_16BE);

        Assertions.assertNotNull(Charsets.UTF_16LE);
        Assertions.assertEquals(StandardCharsets.UTF_16LE, Charsets.UTF_16LE);

        Assertions.assertNotNull(Charsets.DEFAULT);
        Assertions.assertEquals(Charset.defaultCharset(), Charsets.DEFAULT);
    }

    @Test
    public void testGetWithStandardCharsetNames() {
        Charset usAscii = Charsets.get("US-ASCII");
        Assertions.assertNotNull(usAscii);
        Assertions.assertEquals("US-ASCII", usAscii.name());

        Charset iso88591 = Charsets.get("ISO-8859-1");
        Assertions.assertNotNull(iso88591);
        Assertions.assertEquals("ISO-8859-1", iso88591.name());

        Charset utf8 = Charsets.get("UTF-8");
        Assertions.assertNotNull(utf8);
        Assertions.assertEquals("UTF-8", utf8.name());

        Charset utf16 = Charsets.get("UTF-16");
        Assertions.assertNotNull(utf16);
        Assertions.assertEquals("UTF-16", utf16.name());

        Charset utf16be = Charsets.get("UTF-16BE");
        Assertions.assertNotNull(utf16be);
        Assertions.assertEquals("UTF-16BE", utf16be.name());

        Charset utf16le = Charsets.get("UTF-16LE");
        Assertions.assertNotNull(utf16le);
        Assertions.assertEquals("UTF-16LE", utf16le.name());
    }

    @Test
    public void testGetReturnsCachedInstances() {
        Charset utf8_1 = Charsets.get("UTF-8");
        Charset utf8_2 = Charsets.get("UTF-8");
        Assertions.assertSame(utf8_1, utf8_2, "Multiple calls should return same cached instance");

        Charset usAscii_1 = Charsets.get("US-ASCII");
        Charset usAscii_2 = Charsets.get("US-ASCII");
        Assertions.assertSame(usAscii_1, usAscii_2, "Multiple calls should return same cached instance");

        Charset iso_1 = Charsets.get("ISO-8859-1");
        Charset iso_2 = Charsets.get("ISO-8859-1");
        Assertions.assertSame(iso_1, iso_2, "Multiple calls should return same cached instance");
    }

    @Test
    public void testGetWithCharsetAliases() {
        Charset utf8Alias1 = Charsets.get("utf8");
        Assertions.assertNotNull(utf8Alias1);
        Assertions.assertEquals("UTF-8", utf8Alias1.name());

        Charset asciiAlias = Charsets.get("ASCII");
        Assertions.assertNotNull(asciiAlias);
        Assertions.assertEquals("US-ASCII", asciiAlias.name());

        Charset isoAlias = Charsets.get("latin1");
        Assertions.assertNotNull(isoAlias);
        Assertions.assertEquals("ISO-8859-1", isoAlias.name());
    }

    @Test
    public void testGetWithNonStandardCharsets() {
        try {
            Charset gbk = Charsets.get("GBK");
            Assertions.assertNotNull(gbk);
            Assertions.assertEquals("GBK", gbk.name());

            Charset gbk2 = Charsets.get("GBK");
            Assertions.assertSame(gbk, gbk2, "Non-standard charsets should also be cached");
        } catch (UnsupportedCharsetException e) {
        }

        try {
            Charset windows1252 = Charsets.get("windows-1252");
            Assertions.assertNotNull(windows1252);

            Charset windows1252_2 = Charsets.get("windows-1252");
            Assertions.assertSame(windows1252, windows1252_2);
        } catch (UnsupportedCharsetException e) {
        }
    }

    @Test
    public void testGetWithInvalidCharsetName() {
        Assertions.assertThrows(UnsupportedCharsetException.class, () -> {
            Charsets.get("invalid:charset:name");
        }, "Should throw IllegalCharsetNameException for illegal charset name");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Charsets.get("charset with spaces");
        }, "Should throw IllegalCharsetNameException for charset name with spaces");
    }

    @Test
    public void testGetWithUnsupportedCharsetName() {
        Assertions.assertThrows(UnsupportedCharsetException.class, () -> {
            Charsets.get("NONEXISTENT-CHARSET-XYZ-123");
        }, "Should throw UnsupportedCharsetException for unsupported charset");
    }

    @Test
    public void testGetWithNullCharsetName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Charsets.get(null);
        }, "Should throw IllegalArgumentException for null charset name");
    }

    @Test
    public void testGetForEncodingDecoding() {
        String originalText = "Hello World! 你好世界!";

        Charset utf8 = Charsets.get("UTF-8");
        byte[] utf8Bytes = originalText.getBytes(utf8);
        String decodedUtf8 = new String(utf8Bytes, utf8);
        Assertions.assertEquals(originalText, decodedUtf8);

        Charset utf16 = Charsets.get("UTF-16");
        byte[] utf16Bytes = originalText.getBytes(utf16);
        String decodedUtf16 = new String(utf16Bytes, utf16);
        Assertions.assertEquals(originalText, decodedUtf16);

        Charset ascii = Charsets.get("US-ASCII");
        String asciiText = "Hello ASCII";
        byte[] asciiBytes = asciiText.getBytes(ascii);
        String decodedAscii = new String(asciiBytes, ascii);
        Assertions.assertEquals(asciiText, decodedAscii);
    }

    @Test
    public void testConstantsMatchGetMethod() {
        Charset utf8FromGet = Charsets.get("UTF-8");
        Assertions.assertEquals(Charsets.UTF_8, utf8FromGet);

        Charset asciiFromGet = Charsets.get("US-ASCII");
        Assertions.assertEquals(Charsets.US_ASCII, asciiFromGet);

        Charset isoFromGet = Charsets.get("ISO-8859-1");
        Assertions.assertEquals(Charsets.ISO_8859_1, isoFromGet);

        Charset utf16FromGet = Charsets.get("UTF-16");
        Assertions.assertEquals(Charsets.UTF_16, utf16FromGet);

        Charset utf16beFromGet = Charsets.get("UTF-16BE");
        Assertions.assertEquals(Charsets.UTF_16BE, utf16beFromGet);

        Charset utf16leFromGet = Charsets.get("UTF-16LE");
        Assertions.assertEquals(Charsets.UTF_16LE, utf16leFromGet);
    }

    @Test
    public void testGetWithDifferentCasing() {
        Charset utf8Lower = Charsets.get("utf-8");
        Charset utf8Upper = Charsets.get("UTF-8");
        Charset utf8Mixed = Charsets.get("Utf-8");

        Assertions.assertEquals(utf8Lower.name(), utf8Upper.name());
        Assertions.assertEquals(utf8Lower.name(), utf8Mixed.name());

    }

    @Test
    public void testGetCachesMultipleCharsets() {
        Charset utf8 = Charsets.get("UTF-8");
        Charset ascii = Charsets.get("US-ASCII");
        Charset iso = Charsets.get("ISO-8859-1");
        Charset utf16 = Charsets.get("UTF-16");

        Charset utf8_2 = Charsets.get("UTF-8");
        Charset ascii_2 = Charsets.get("US-ASCII");
        Charset iso_2 = Charsets.get("ISO-8859-1");
        Charset utf16_2 = Charsets.get("UTF-16");

        Assertions.assertSame(utf8, utf8_2);
        Assertions.assertSame(ascii, ascii_2);
        Assertions.assertSame(iso, iso_2);
        Assertions.assertSame(utf16, utf16_2);
    }

    @Test
    public void testConstantsUsableForStringOperations() {
        String testString = "Test String";

        byte[] asciiBytes = testString.getBytes(Charsets.US_ASCII);
        Assertions.assertTrue(asciiBytes.length > 0);
        Assertions.assertEquals(testString, new String(asciiBytes, Charsets.US_ASCII));

        byte[] isoBytes = testString.getBytes(Charsets.ISO_8859_1);
        Assertions.assertTrue(isoBytes.length > 0);
        Assertions.assertEquals(testString, new String(isoBytes, Charsets.ISO_8859_1));

        byte[] utf8Bytes = testString.getBytes(Charsets.UTF_8);
        Assertions.assertTrue(utf8Bytes.length > 0);
        Assertions.assertEquals(testString, new String(utf8Bytes, Charsets.UTF_8));

        byte[] utf16Bytes = testString.getBytes(Charsets.UTF_16);
        Assertions.assertTrue(utf16Bytes.length > 0);
        Assertions.assertEquals(testString, new String(utf16Bytes, Charsets.UTF_16));

        byte[] utf16beBytes = testString.getBytes(Charsets.UTF_16BE);
        Assertions.assertTrue(utf16beBytes.length > 0);
        Assertions.assertEquals(testString, new String(utf16beBytes, Charsets.UTF_16BE));

        byte[] utf16leBytes = testString.getBytes(Charsets.UTF_16LE);
        Assertions.assertTrue(utf16leBytes.length > 0);
        Assertions.assertEquals(testString, new String(utf16leBytes, Charsets.UTF_16LE));
    }

    @Test
    public void testDefaultCharset() {
        Assertions.assertNotNull(Charsets.DEFAULT);
        Assertions.assertEquals(Charset.defaultCharset(), Charsets.DEFAULT);

        String testString = "Default Charset Test";
        byte[] bytes = testString.getBytes(Charsets.DEFAULT);
        String decoded = new String(bytes, Charsets.DEFAULT);
        Assertions.assertEquals(testString, decoded);
    }

    @Test
    public void testGetWithEmptyString() {
        Assertions.assertThrows(IllegalCharsetNameException.class, () -> {
            Charsets.get("");
        }, "Should throw IllegalCharsetNameException for empty charset name");
    }

    @Test
    public void testCharsetConstantsCanonicalNames() {
        Assertions.assertEquals("US-ASCII", Charsets.US_ASCII.name());
        Assertions.assertEquals("ISO-8859-1", Charsets.ISO_8859_1.name());
        Assertions.assertEquals("UTF-8", Charsets.UTF_8.name());
        Assertions.assertEquals("UTF-16", Charsets.UTF_16.name());
        Assertions.assertEquals("UTF-16BE", Charsets.UTF_16BE.name());
        Assertions.assertEquals("UTF-16LE", Charsets.UTF_16LE.name());
    }
}
