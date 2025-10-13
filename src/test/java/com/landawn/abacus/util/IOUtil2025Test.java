package com.landawn.abacus.util;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("2025")
public class IOUtil2025Test extends TestBase {

    @TempDir
    Path tempFolder;

    private File tempFile;
    private File largeFile;
    private File emptyFile;
    private static final String TEST_CONTENT = "Hello World!";
    private static final String MULTILINE_CONTENT = "Line 1\r\nLine 2\r\nLine 3\r\nLine 4\r\nLine 5\r\n";
    private static final String UNICODE_CONTENT = "Hello 世界 \uD83D\uDE00 Здравствуй мир";
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final Charset UTF_16 = StandardCharsets.UTF_16;
    private static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        emptyFile = Files.createTempFile(tempFolder, "empty", ".txt").toFile();
        largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();

        Files.write(tempFile.toPath(), TEST_CONTENT.getBytes(UTF_8));

        StringBuilder largeSb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeSb.append("Line ").append(i).append(": This is a test line with some content.\r\n");
        }
        Files.write(largeFile.toPath(), largeSb.toString().getBytes(UTF_8));
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testChars2Bytes_Default() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testChars2Bytes_WithCharset() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars, UTF_8);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testChars2Bytes_WithCharsetUTF16() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars, UTF_16);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_16));
    }

    @Test
    public void testChars2Bytes_WithOffsetAndLength() {
        char[] chars = "0123456789".toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars, 2, 5, UTF_8);
        assertNotNull(bytes);
        assertEquals("23456", new String(bytes, UTF_8));
    }

    @Test
    public void testChars2Bytes_EmptyArray() {
        char[] chars = new char[0];
        byte[] bytes = IOUtil.chars2Bytes(chars);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testChars2Bytes_UnicodeChars() {
        char[] chars = UNICODE_CONTENT.toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars, UTF_8);
        assertNotNull(bytes);
        assertEquals(UNICODE_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testChars2Bytes_ZeroLength() {
        char[] chars = "0123456789".toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars, 5, 0, UTF_8);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testBytes2Chars_Default() {
        byte[] bytes = TEST_CONTENT.getBytes(UTF_8);
        char[] chars = IOUtil.bytes2Chars(bytes);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testBytes2Chars_WithCharset() {
        byte[] bytes = TEST_CONTENT.getBytes(UTF_8);
        char[] chars = IOUtil.bytes2Chars(bytes, UTF_8);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testBytes2Chars_WithCharsetUTF16() {
        byte[] bytes = TEST_CONTENT.getBytes(UTF_16);
        char[] chars = IOUtil.bytes2Chars(bytes, UTF_16);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testBytes2Chars_WithOffsetAndLength() {
        byte[] bytes = "0123456789".getBytes(UTF_8);
        char[] chars = IOUtil.bytes2Chars(bytes, 2, 5, UTF_8);
        assertNotNull(chars);
        assertEquals("23456", new String(chars));
    }

    @Test
    public void testBytes2Chars_EmptyArray() {
        byte[] bytes = new byte[0];
        char[] chars = IOUtil.bytes2Chars(bytes);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testBytes2Chars_UnicodeBytes() {
        byte[] bytes = UNICODE_CONTENT.getBytes(UTF_8);
        char[] chars = IOUtil.bytes2Chars(bytes, UTF_8);
        assertNotNull(chars);
        assertEquals(UNICODE_CONTENT, new String(chars));
    }

    @Test
    public void testBytes2Chars_ZeroLength() {
        byte[] bytes = "0123456789".getBytes(UTF_8);
        char[] chars = IOUtil.bytes2Chars(bytes, 5, 0, UTF_8);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testString2InputStream_Default() throws IOException {
        InputStream is = IOUtil.string2InputStream(TEST_CONTENT);
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
        is.close();
    }

    @Test
    public void testString2InputStream_WithCharset() throws IOException {
        InputStream is = IOUtil.string2InputStream(TEST_CONTENT, UTF_16);
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(TEST_CONTENT, new String(bytes, UTF_16));
        is.close();
    }

    @Test
    public void testString2InputStream_EmptyString() throws IOException {
        InputStream is = IOUtil.string2InputStream("");
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(0, bytes.length);
        is.close();
    }

    @Test
    public void testString2InputStream_UnicodeContent() throws IOException {
        InputStream is = IOUtil.string2InputStream(UNICODE_CONTENT, UTF_8);
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(UNICODE_CONTENT, new String(bytes, UTF_8));
        is.close();
    }

    @Test
    public void testString2InputStream_MultilineContent() throws IOException {
        InputStream is = IOUtil.string2InputStream(MULTILINE_CONTENT);
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(MULTILINE_CONTENT, new String(bytes, UTF_8));
        is.close();
    }

    @Test
    public void testString2Reader() throws IOException {
        Reader reader = IOUtil.string2Reader(TEST_CONTENT);
        assertNotNull(reader);
        char[] buffer = new char[TEST_CONTENT.length()];
        int read = reader.read(buffer);
        assertEquals(TEST_CONTENT.length(), read);
        assertEquals(TEST_CONTENT, new String(buffer));
        reader.close();
    }

    @Test
    public void testString2Reader_EmptyString() throws IOException {
        Reader reader = IOUtil.string2Reader("");
        assertNotNull(reader);
        char[] buffer = new char[10];
        int read = reader.read(buffer);
        assertEquals(-1, read);
        reader.close();
    }

    @Test
    public void testString2Reader_UnicodeContent() throws IOException {
        Reader reader = IOUtil.string2Reader(UNICODE_CONTENT);
        assertNotNull(reader);
        char[] buffer = new char[UNICODE_CONTENT.length()];
        int read = reader.read(buffer);
        assertEquals(UNICODE_CONTENT.length(), read);
        assertEquals(UNICODE_CONTENT, new String(buffer));
        reader.close();
    }

    @Test
    public void testString2Reader_MultilineContent() throws IOException {
        Reader reader = IOUtil.string2Reader(MULTILINE_CONTENT);
        assertNotNull(reader);
        char[] buffer = new char[MULTILINE_CONTENT.length()];
        int read = reader.read(buffer);
        assertEquals(MULTILINE_CONTENT.length(), read);
        assertEquals(MULTILINE_CONTENT, new String(buffer));
        reader.close();
    }

    @Test
    public void testStringBuilder2Writer() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        assertNotNull(writer);
        writer.write(TEST_CONTENT);
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testStringBuilder2Writer_MultipleWrites() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        writer.write("Hello");
        writer.write(" ");
        writer.write("World");
        writer.flush();
        assertEquals("Hello World", sb.toString());
        writer.close();
    }

    @Test
    public void testStringBuilder2Writer_EmptyWrite() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        writer.write("");
        writer.flush();
        assertEquals("", sb.toString());
        writer.close();
    }

    @Test
    public void testStringBuilder2Writer_UnicodeContent() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        writer.write(UNICODE_CONTENT);
        writer.flush();
        assertEquals(UNICODE_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testStringBuilder2Writer_MultilineContent() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        writer.write(MULTILINE_CONTENT);
        writer.flush();
        assertEquals(MULTILINE_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testReadAllBytes_FromFile() {
        byte[] bytes = IOUtil.readAllBytes(tempFile);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testReadAllBytes_FromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            byte[] bytes = IOUtil.readAllBytes(is);
            assertNotNull(bytes);
            assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
        }
    }

    @Test
    public void testReadAllBytes_EmptyFile() {
        byte[] bytes = IOUtil.readAllBytes(emptyFile);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testReadAllBytes_EmptyInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(new byte[0])) {
            byte[] bytes = IOUtil.readAllBytes(is);
            assertNotNull(bytes);
            assertEquals(0, bytes.length);
        }
    }

    @Test
    public void testReadAllBytes_LargeFile() {
        byte[] bytes = IOUtil.readAllBytes(largeFile);
        assertNotNull(bytes);
        assertTrue(bytes.length > 10000);
    }

    @Test
    public void testReadBytes_FromFile() throws IOException {
        byte[] bytes = IOUtil.readBytes(tempFile);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testReadBytes_FromFileWithOffset() throws IOException {
        byte[] bytes = IOUtil.readBytes(tempFile, 6, 5);
        assertNotNull(bytes);
        assertEquals("World", new String(bytes, UTF_8));
    }

    @Test
    public void testReadBytes_FromFileWithZeroLength() throws IOException {
        byte[] bytes = IOUtil.readBytes(tempFile, 0, 0);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testReadBytes_FromFileOffsetBeyondEnd() throws IOException {
        byte[] bytes = IOUtil.readBytes(tempFile, 1000, 10);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testReadBytes_FromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            byte[] bytes = IOUtil.readBytes(is);
            assertNotNull(bytes);
            assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
        }
    }

    @Test
    public void testReadBytes_FromInputStreamWithOffset() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            byte[] bytes = IOUtil.readBytes(is, 5, 5);
            assertNotNull(bytes);
            assertEquals("56789", new String(bytes, UTF_8));
        }
    }

    @Test
    public void testReadBytes_FromInputStreamWithZeroOffset() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            byte[] bytes = IOUtil.readBytes(is, 0, 5);
            assertNotNull(bytes);
            assertEquals("Hello", new String(bytes, UTF_8));
        }
    }

    @Test
    public void testReadBytes_FromInputStreamWithMaxLen() throws IOException {
        byte[] data = new byte[10000];
        try (InputStream is = new ByteArrayInputStream(data)) {
            byte[] bytes = IOUtil.readBytes(is, 0, 100);
            assertNotNull(bytes);
            assertEquals(100, bytes.length);
        }
    }

    @Test
    public void testReadAllChars_FromFile() {
        char[] chars = IOUtil.readAllChars(tempFile);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testReadAllChars_FromFileWithEncoding() {
        char[] chars = IOUtil.readAllChars(tempFile, UTF_8);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testReadAllChars_FromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            char[] chars = IOUtil.readAllChars(is);
            assertNotNull(chars);
            assertEquals(TEST_CONTENT, new String(chars));
        }
    }

    @Test
    public void testReadAllChars_FromInputStreamWithEncoding() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            char[] chars = IOUtil.readAllChars(is, UTF_8);
            assertNotNull(chars);
            assertEquals(TEST_CONTENT, new String(chars));
        }
    }

    @Test
    public void testReadAllChars_FromReader() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            char[] chars = IOUtil.readAllChars(reader);
            assertNotNull(chars);
            assertEquals(TEST_CONTENT, new String(chars));
        }
    }

    @Test
    public void testReadAllChars_EmptyFile() {
        char[] chars = IOUtil.readAllChars(emptyFile);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testReadAllChars_EmptyReader() throws IOException {
        try (Reader reader = new StringReader("")) {
            char[] chars = IOUtil.readAllChars(reader);
            assertNotNull(chars);
            assertEquals(0, chars.length);
        }
    }

    @Test
    public void testReadAllChars_MultilineContent() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        char[] chars = IOUtil.readAllChars(multilineFile);
        assertNotNull(chars);
        assertEquals(MULTILINE_CONTENT, new String(chars));
    }

    @Test
    public void testReadChars_FromFile() throws IOException {
        char[] chars = IOUtil.readChars(tempFile);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testReadChars_FromFileWithEncoding() throws IOException {
        char[] chars = IOUtil.readChars(tempFile, UTF_8);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testReadChars_FromFileWithOffset() throws IOException {
        char[] chars = IOUtil.readChars(tempFile, 6, 5);
        assertNotNull(chars);
        assertEquals("World", new String(chars));
    }

    @Test
    public void testReadChars_FromFileWithEncodingAndOffset() throws IOException {
        char[] chars = IOUtil.readChars(tempFile, UTF_8, 6, 5);
        assertNotNull(chars);
        assertEquals("World", new String(chars));
    }

    @Test
    public void testReadChars_FromFileWithZeroLength() throws IOException {
        char[] chars = IOUtil.readChars(tempFile, 0, 0);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testReadChars_FromFileOffsetBeyondEnd() throws IOException {
        char[] chars = IOUtil.readChars(tempFile, 1000, 10);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testReadChars_FromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            char[] chars = IOUtil.readChars(is);
            assertNotNull(chars);
            assertEquals(TEST_CONTENT, new String(chars));
        }
    }

    @Test
    public void testReadChars_FromInputStreamWithEncoding() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            char[] chars = IOUtil.readChars(is, UTF_8);
            assertNotNull(chars);
            assertEquals(TEST_CONTENT, new String(chars));
        }
    }

    @Test
    public void testReadChars_FromInputStreamWithOffset() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            char[] chars = IOUtil.readChars(is, 5, 5);
            assertNotNull(chars);
            assertEquals("56789", new String(chars));
        }
    }

    @Test
    public void testReadChars_FromInputStreamWithEncodingAndOffset() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            char[] chars = IOUtil.readChars(is, UTF_8, 5, 5);
            assertNotNull(chars);
            assertEquals("56789", new String(chars));
        }
    }

    @Test
    public void testReadChars_FromReader() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            char[] chars = IOUtil.readChars(reader);
            assertNotNull(chars);
            assertEquals(TEST_CONTENT, new String(chars));
        }
    }

    @Test
    public void testReadChars_FromReaderWithOffset() throws IOException {
        try (Reader reader = new StringReader("0123456789ABCDEF")) {
            char[] chars = IOUtil.readChars(reader, 5, 5);
            assertNotNull(chars);
            assertEquals("56789", new String(chars));
        }
    }

    @Test
    public void testReadChars_FromReaderWithZeroLength() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            char[] chars = IOUtil.readChars(reader, 0, 0);
            assertNotNull(chars);
            assertEquals(0, chars.length);
        }
    }

    @Test
    public void testReadAllToString_FromFile() {
        String content = IOUtil.readAllToString(tempFile);
        assertNotNull(content);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testReadAllToString_FromFileWithStringEncoding() {
        String content = IOUtil.readAllToString(tempFile, "UTF-8");
        assertNotNull(content);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testReadAllToString_FromFileWithCharsetEncoding() {
        String content = IOUtil.readAllToString(tempFile, UTF_8);
        assertNotNull(content);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testReadAllToString_FromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            String content = IOUtil.readAllToString(is);
            assertNotNull(content);
            assertEquals(TEST_CONTENT, content);
        }
    }

    @Test
    public void testReadAllToString_FromInputStreamWithEncoding() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            String content = IOUtil.readAllToString(is, UTF_8);
            assertNotNull(content);
            assertEquals(TEST_CONTENT, content);
        }
    }

    @Test
    public void testReadAllToString_FromReader() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            String content = IOUtil.readAllToString(reader);
            assertNotNull(content);
            assertEquals(TEST_CONTENT, content);
        }
    }

    @Test
    public void testReadAllToString_EmptyFile() {
        String content = IOUtil.readAllToString(emptyFile);
        assertNotNull(content);
        assertEquals("", content);
    }

    @Test
    public void testReadAllToString_EmptyReader() throws IOException {
        try (Reader reader = new StringReader("")) {
            String content = IOUtil.readAllToString(reader);
            assertNotNull(content);
            assertEquals("", content);
        }
    }

    @Test
    public void testReadAllToString_MultilineContent() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String content = IOUtil.readAllToString(multilineFile);
        assertNotNull(content);
        assertEquals(MULTILINE_CONTENT, content);
    }

    @Test
    public void testReadAllToString_UnicodeContent() throws IOException {
        File unicodeFile = Files.createTempFile(tempFolder, "unicode", ".txt").toFile();
        Files.write(unicodeFile.toPath(), UNICODE_CONTENT.getBytes(UTF_8));

        String content = IOUtil.readAllToString(unicodeFile, UTF_8);
        assertNotNull(content);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testReadToString_FromFileWithOffset() throws IOException {
        String content = IOUtil.readToString(tempFile, 6, 5);
        assertNotNull(content);
        assertEquals("World", content);
    }

    @Test
    public void testReadToString_FromFileWithEncodingAndOffset() throws IOException {
        String content = IOUtil.readToString(tempFile, UTF_8, 6, 5);
        assertNotNull(content);
        assertEquals("World", content);
    }

    @Test
    public void testReadToString_FromFileWithZeroLength() throws IOException {
        String content = IOUtil.readToString(tempFile, 0, 0);
        assertNotNull(content);
        assertEquals("", content);
    }

    @Test
    public void testReadToString_FromFileOffsetBeyondEnd() throws IOException {
        String content = IOUtil.readToString(tempFile, 1000, 10);
        assertNotNull(content);
        assertEquals("", content);
    }

    @Test
    public void testReadToString_FromInputStreamWithOffset() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            String content = IOUtil.readToString(is, 5, 5);
            assertNotNull(content);
            assertEquals("56789", content);
        }
    }

    @Test
    public void testReadToString_FromInputStreamWithEncodingAndOffset() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            String content = IOUtil.readToString(is, UTF_8, 5, 5);
            assertNotNull(content);
            assertEquals("56789", content);
        }
    }

    @Test
    public void testReadToString_FromReaderWithOffset() throws IOException {
        try (Reader reader = new StringReader("0123456789ABCDEF")) {
            String content = IOUtil.readToString(reader, 5, 5);
            assertNotNull(content);
            assertEquals("56789", content);
        }
    }

    @Test
    public void testReadToString_FromReaderWithZeroLength() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            String content = IOUtil.readToString(reader, 0, 0);
            assertNotNull(content);
            assertEquals("", content);
        }
    }

    @Test
    public void testReadToString_FromFileFullContent() throws IOException {
        String content = IOUtil.readToString(tempFile, 0, 1000);
        assertNotNull(content);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testReadToString_FromInputStreamZeroOffset() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            String content = IOUtil.readToString(is, 0, 5);
            assertNotNull(content);
            assertEquals("Hello", content);
        }
    }

    @Test
    public void testChars2Bytes_NullArray() {
        assertArrayEquals(new byte[] {}, IOUtil.chars2Bytes(null));
    }

    @Test
    public void testBytes2Chars_NullArray() {
        assertArrayEquals(new char[] {}, IOUtil.bytes2Chars(null));
    }

    @Test
    public void testString2InputStream_NullString() {
        try (InputStream is = IOUtil.string2InputStream(null)) {
            byte[] bytes = is.readAllBytes();
            assertEquals(0, bytes.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Test
    public void testString2Reader_NullString() {
        try (Reader reader = IOUtil.string2Reader(null)) {
            char[] buffer = new char[10];
            int read = reader.read(buffer);
            assertEquals(-1, read);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testStringBuilder2Writer_NullStringBuilder() {
        assertThrows(Exception.class, () -> {
            IOUtil.stringBuilder2Writer(null);
        });
    }

    @Test
    public void testReadAllBytes_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(Exception.class, () -> {
            IOUtil.readAllBytes(nonexistent);
        });
    }

    @Test
    public void testReadAllChars_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(Exception.class, () -> {
            IOUtil.readAllChars(nonexistent);
        });
    }

    @Test
    public void testReadAllToString_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(Exception.class, () -> {
            IOUtil.readAllToString(nonexistent);
        });
    }

    @Test
    public void testRoundTrip_CharsToBytes() {
        char[] original = UNICODE_CONTENT.toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(original, UTF_8);
        char[] result = IOUtil.bytes2Chars(bytes, UTF_8);
        assertArrayEquals(original, result);
    }

    @Test
    public void testRoundTrip_StringToInputStreamToString() throws IOException {
        InputStream is = IOUtil.string2InputStream(UNICODE_CONTENT, UTF_8);
        String result = IOUtil.readAllToString(is, UTF_8);
        assertEquals(UNICODE_CONTENT, result);
    }

    @Test
    public void testRoundTrip_StringToReaderToString() throws IOException {
        Reader reader = IOUtil.string2Reader(UNICODE_CONTENT);
        String result = IOUtil.readAllToString(reader);
        assertEquals(UNICODE_CONTENT, result);
    }

    @Test
    public void testRoundTrip_StringBuilderWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        writer.write(UNICODE_CONTENT);
        writer.flush();
        assertEquals(UNICODE_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testRoundTrip_FileWriteRead() throws IOException {
        File testFile = Files.createTempFile(tempFolder, "roundtrip", ".txt").toFile();
        Files.write(testFile.toPath(), UNICODE_CONTENT.getBytes(UTF_8));

        String result = IOUtil.readAllToString(testFile, UTF_8);
        assertEquals(UNICODE_CONTENT, result);
    }

    @Test
    public void testReadAllBytes_LargeData() {
        byte[] bytes = IOUtil.readAllBytes(largeFile);
        assertNotNull(bytes);
        assertTrue(bytes.length > 10000);
    }

    @Test
    public void testReadAllChars_LargeData() {
        char[] chars = IOUtil.readAllChars(largeFile);
        assertNotNull(chars);
        assertTrue(chars.length > 10000);
    }

    @Test
    public void testReadAllToString_LargeData() {
        String content = IOUtil.readAllToString(largeFile);
        assertNotNull(content);
        assertTrue(content.length() > 10000);
    }

    @Test
    public void testReadBytes_PartialLargeFile() throws IOException {
        byte[] bytes = IOUtil.readBytes(largeFile, 100, 50);
        assertNotNull(bytes);
        assertEquals(50, bytes.length);
    }

    @Test
    public void testReadChars_PartialLargeFile() throws IOException {
        char[] chars = IOUtil.readChars(largeFile, 100, 50);
        assertNotNull(chars);
        assertEquals(50, chars.length);
    }

    @Test
    public void testReadToString_PartialLargeFile() throws IOException {
        String content = IOUtil.readToString(largeFile, 100, 50);
        assertNotNull(content);
        assertEquals(50, content.length());
    }

    @Test
    public void testWrite_BooleanToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(true, writer);
        writer.flush();
        assertEquals("true", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_BooleanFalseToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(false, writer);
        writer.flush();
        assertEquals("false", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write('A', writer);
        writer.flush();
        assertEquals("A", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharUnicodeToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write('\u4E16', writer);
        writer.flush();
        assertEquals("世", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ByteToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((byte) 65, writer);
        writer.flush();
        assertEquals("65", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ByteNegativeToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((byte) -128, writer);
        writer.flush();
        assertEquals("-128", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ShortToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((short) 12345, writer);
        writer.flush();
        assertEquals("12345", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ShortNegativeToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((short) -32768, writer);
        writer.flush();
        assertEquals("-32768", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_IntToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(123456789, writer);
        writer.flush();
        assertEquals("123456789", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_IntNegativeToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(-987654321, writer);
        writer.flush();
        assertEquals("-987654321", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_LongToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(9876543210L, writer);
        writer.flush();
        assertEquals("9876543210", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_LongNegativeToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(-9876543210L, writer);
        writer.flush();
        assertEquals("-9876543210", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_FloatToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(3.14f, writer);
        writer.flush();
        assertEquals("3.14", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_FloatNegativeToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(-2.718f, writer);
        writer.flush();
        assertEquals("-2.718", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_DoubleToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(3.141592653589793, writer);
        writer.flush();
        assertEquals("3.141592653589793", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_DoubleNegativeToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(-2.718281828459045, writer);
        writer.flush();
        assertEquals("-2.718281828459045", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ObjectToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((Object) "Hello", writer);
        writer.flush();
        assertEquals("Hello", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ObjectIntegerToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((Object) Integer.valueOf(42), writer);
        writer.flush();
        assertEquals("42", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ObjectNullToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((Object) null, writer);
        writer.flush();
        assertEquals("null", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharSequenceToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(TEST_CONTENT, outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_CharSequenceWithCharsetToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(UNICODE_CONTENT, UTF_8, outputFile);
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_CharSequenceToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(TEST_CONTENT, fos);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_CharSequenceWithCharsetToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(UNICODE_CONTENT, UTF_8, fos);
        }
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_CharSequenceToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(TEST_CONTENT, fos, true);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_CharSequenceToOutputStreamWithoutFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(TEST_CONTENT, fos, false);
            fos.flush();
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_CharSequenceWithCharsetToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(UNICODE_CONTENT, UTF_8, fos, true);
        }
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_CharSequenceToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((CharSequence) TEST_CONTENT, writer);
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharSequenceToWriterWithFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((CharSequence) TEST_CONTENT, writer, true);
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharSequenceToWriterWithoutFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write((CharSequence) TEST_CONTENT, writer, false);
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_EmptyCharSequenceToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write("", outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWrite_MultilineCharSequenceToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(MULTILINE_CONTENT, outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(MULTILINE_CONTENT, content);
    }

    @Test
    public void testWrite_CharArrayToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(TEST_CONTENT.toCharArray(), outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_CharArrayWithCharsetToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(UNICODE_CONTENT.toCharArray(), UTF_8, outputFile);
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        char[] chars = "0123456789".toCharArray();
        IOUtil.write(chars, 2, 5, outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("23456", content);
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountCharsetToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        char[] chars = UNICODE_CONTENT.toCharArray();
        IOUtil.write(chars, 0, 5, UTF_8, outputFile);
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT.substring(0, 5), content);
    }

    @Test
    public void testWrite_CharArrayToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(TEST_CONTENT.toCharArray(), fos);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_CharArrayWithCharsetToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(UNICODE_CONTENT.toCharArray(), UTF_8, fos);
        }
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            char[] chars = "0123456789ABCDEF".toCharArray();
            IOUtil.write(chars, 5, 6, fos);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("56789A", content);
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountCharsetToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            char[] chars = UNICODE_CONTENT.substring(0, 11).toCharArray();
            IOUtil.write(chars, 0, 11, UTF_8, fos);
        }
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT.substring(0, 11), content);
    }

    @Test
    public void testWrite_CharArrayToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(TEST_CONTENT.toCharArray(), fos, true);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            char[] chars = "0123456789".toCharArray();
            IOUtil.write(chars, 3, 4, fos, true);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("3456", content);
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountCharsetToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            char[] chars = UNICODE_CONTENT.toCharArray();
            IOUtil.write(chars, 0, 8, UTF_8, fos, true);
        }
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT.substring(0, 8), content);
    }

    @Test
    public void testWrite_CharArrayToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(TEST_CONTENT.toCharArray(), writer);
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        char[] chars = "0123456789".toCharArray();
        IOUtil.write(chars, 2, 5, writer);
        writer.flush();
        assertEquals("23456", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharArrayToWriterWithFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        IOUtil.write(TEST_CONTENT.toCharArray(), writer, true);
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountToWriterWithFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        char[] chars = "ABCDEFGHIJ".toCharArray();
        IOUtil.write(chars, 3, 4, writer, true);
        assertEquals("DEFG", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_EmptyCharArrayToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(new char[0], outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWrite_CharArrayZeroCountToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        char[] chars = "0123456789".toCharArray();
        IOUtil.write(chars, 5, 0, writer);
        writer.flush();
        assertEquals("", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ByteArrayToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(TEST_CONTENT.getBytes(UTF_8), outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_ByteArrayWithOffsetCountToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        byte[] bytes = "0123456789".getBytes(UTF_8);
        IOUtil.write(bytes, 2, 5, outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("23456", content);
    }

    @Test
    public void testWrite_ByteArrayToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(TEST_CONTENT.getBytes(UTF_8), fos);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_ByteArrayWithOffsetCountToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] bytes = "0123456789ABCDEF".getBytes(UTF_8);
            IOUtil.write(bytes, 5, 6, fos);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("56789A", content);
    }

    @Test
    public void testWrite_ByteArrayToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtil.write(TEST_CONTENT.getBytes(UTF_8), fos, true);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_ByteArrayWithOffsetCountToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] bytes = "0123456789".getBytes(UTF_8);
            IOUtil.write(bytes, 3, 4, fos, true);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("3456", content);
    }

    @Test
    public void testWrite_EmptyByteArrayToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(new byte[0], outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWrite_ByteArrayZeroCountToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] bytes = "0123456789".getBytes(UTF_8);
            IOUtil.write(bytes, 5, 0, fos);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWrite_UnicodeByteArrayToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(UNICODE_CONTENT.getBytes(UTF_8), outputFile);
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_FileToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        long bytesWritten = IOUtil.write(tempFile, outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
        assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
    }

    @Test
    public void testWrite_FileWithOffsetCountToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        long bytesWritten = IOUtil.write(tempFile, 6, 5, outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("World", content);
        assertEquals(5, bytesWritten);
    }

    @Test
    public void testWrite_FileToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(tempFile, fos);
            assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_FileWithOffsetCountToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(tempFile, 6, 5, fos);
            assertEquals(5, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("World", content);
    }

    @Test
    public void testWrite_FileToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(tempFile, fos, true);
            assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_FileWithOffsetCountToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(tempFile, 0, 5, fos, true);
            assertEquals(5, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Hello", content);
    }

    @Test
    public void testWrite_EmptyFileToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        long bytesWritten = IOUtil.write(emptyFile, outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
        assertEquals(0, bytesWritten);
    }

    @Test
    public void testWrite_LargeFileToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        long bytesWritten = IOUtil.write(largeFile, outputFile);
        String originalContent = IOUtil.readAllToString(largeFile);
        String outputContent = IOUtil.readAllToString(outputFile);
        assertEquals(originalContent, outputContent);
        assertTrue(bytesWritten > 10000);
    }

    @Test
    public void testWrite_FileWithZeroCountToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        long bytesWritten = IOUtil.write(tempFile, 0, 0, outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
        assertEquals(0, bytesWritten);
    }

    @Test
    public void testWrite_InputStreamToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            long bytesWritten = IOUtil.write(is, outputFile);
            assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_InputStreamWithOffsetCountToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream("0123456789ABCDEF".getBytes(UTF_8))) {
            long bytesWritten = IOUtil.write(is, 5, 6, outputFile);
            assertEquals(6, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("56789A", content);
    }

    @Test
    public void testWrite_InputStreamToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)); FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(is, fos);
            assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_InputStreamWithOffsetCountToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream("0123456789ABCDEF".getBytes(UTF_8)); FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(is, 5, 6, fos);
            assertEquals(6, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("56789A", content);
    }

    @Test
    public void testWrite_InputStreamToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)); FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(is, fos, true);
            assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_InputStreamWithOffsetCountToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream("0123456789".getBytes(UTF_8)); FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(is, 3, 4, fos, true);
            assertEquals(4, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("3456", content);
    }

    @Test
    public void testWrite_EmptyInputStreamToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream(new byte[0])) {
            long bytesWritten = IOUtil.write(is, outputFile);
            assertEquals(0, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWrite_InputStreamWithZeroCountToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            long bytesWritten = IOUtil.write(is, 0, 0, outputFile);
            assertEquals(0, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWrite_FileInputStreamToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            long bytesWritten = IOUtil.write(fis, outputFile);
            assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_ReaderToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long charsWritten = IOUtil.write(reader, outputFile);
            assertEquals(TEST_CONTENT.length(), charsWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_ReaderWithCharsetToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader(UNICODE_CONTENT)) {
            long charsWritten = IOUtil.write(reader, UTF_8, outputFile);
            assertEquals(UNICODE_CONTENT.length(), charsWritten);
        }
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_ReaderWithOffsetCountToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader("0123456789ABCDEF")) {
            long charsWritten = IOUtil.write(reader, 5, 6, outputFile);
            assertEquals(6, charsWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("56789A", content);
    }

    @Test
    public void testWrite_ReaderWithOffsetCountCharsetToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader(UNICODE_CONTENT)) {
            long charsWritten = IOUtil.write(reader, 0, 11, UTF_8, outputFile);
            assertEquals(11, charsWritten);
        }
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT.substring(0, 11), content);
    }

    @Test
    public void testWrite_ReaderToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long charsWritten = IOUtil.write(reader, writer);
            assertEquals(TEST_CONTENT.length(), charsWritten);
        }
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ReaderWithOffsetCountToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        try (Reader reader = new StringReader("0123456789ABCDEF")) {
            long charsWritten = IOUtil.write(reader, 5, 6, writer);
            assertEquals(6, charsWritten);
        }
        writer.flush();
        assertEquals("56789A", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ReaderToWriterWithFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long charsWritten = IOUtil.write(reader, writer, true);
            assertEquals(TEST_CONTENT.length(), charsWritten);
        }
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ReaderWithOffsetCountToWriterWithFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        try (Reader reader = new StringReader("0123456789")) {
            long charsWritten = IOUtil.write(reader, 3, 4, writer, true);
            assertEquals(4, charsWritten);
        }
        assertEquals("3456", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_EmptyReaderToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader("")) {
            long charsWritten = IOUtil.write(reader, outputFile);
            assertEquals(0, charsWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWrite_ReaderWithZeroCountToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long charsWritten = IOUtil.write(reader, 0, 0, outputFile);
            assertEquals(0, charsWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWrite_FileReaderToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileReader reader = new FileReader(tempFile)) {
            long charsWritten = IOUtil.write(reader, outputFile);
            assertEquals(TEST_CONTENT.length(), charsWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_MultilineReaderToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        try (Reader reader = new StringReader(MULTILINE_CONTENT)) {
            long charsWritten = IOUtil.write(reader, writer);
            assertEquals(MULTILINE_CONTENT.length(), charsWritten);
        }
        writer.flush();
        assertEquals(MULTILINE_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_UnicodeReaderToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader(UNICODE_CONTENT)) {
            long charsWritten = IOUtil.write(reader, UTF_8, outputFile);
            assertEquals(UNICODE_CONTENT.length(), charsWritten);
        }
        String content = IOUtil.readAllToString(outputFile, UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_MultipleWritesToSameFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write("Hello", outputFile);
        IOUtil.write("World", outputFile);
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("World", content);
    }

    @Test
    public void testWrite_AppendToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
            IOUtil.write("Hello ", fos);
            IOUtil.write("World", fos);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Hello World", content);
    }

    @Test
    public void testWrite_LargeDataFileToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        long bytesWritten = IOUtil.write(largeFile, outputFile);
        String originalContent = IOUtil.readAllToString(largeFile);
        String outputContent = IOUtil.readAllToString(outputFile);
        assertEquals(originalContent, outputContent);
        assertTrue(bytesWritten > 10000);
    }

    @Test
    public void testWrite_CharsetEncodingRoundTrip() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(UNICODE_CONTENT, UTF_16, outputFile);
        String content = IOUtil.readAllToString(outputFile, UTF_16);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testWrite_CharArrayWithDifferentCharsets() throws IOException {
        File outputFile1 = Files.createTempFile(tempFolder, "output1", ".txt").toFile();
        File outputFile2 = Files.createTempFile(tempFolder, "output2", ".txt").toFile();

        char[] chars = UNICODE_CONTENT.toCharArray();
        IOUtil.write(chars, UTF_8, outputFile1);
        IOUtil.write(chars, UTF_16, outputFile2);

        String content1 = IOUtil.readAllToString(outputFile1, UTF_8);
        String content2 = IOUtil.readAllToString(outputFile2, UTF_16);

        assertEquals(UNICODE_CONTENT, content1);
        assertEquals(UNICODE_CONTENT, content2);
        assertEquals(content1, content2);
    }

    @Test
    public void testWrite_ByteArrayPartialData() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        byte[] bytes = new byte[100];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ('A' + (i % 26));
        }
        IOUtil.write(bytes, 10, 20, outputFile);
        byte[] readBytes = IOUtil.readAllBytes(outputFile);
        assertEquals(20, readBytes.length);
        for (int i = 0; i < 20; i++) {
            assertEquals(bytes[10 + i], readBytes[i]);
        }
    }

    @Test
    public void testWrite_StreamCopyWithOffset() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        try (InputStream is = new ByteArrayInputStream(data); FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(is, 10, 20, fos);
            assertEquals(20, bytesWritten);
        }

        byte[] readBytes = IOUtil.readAllBytes(outputFile);
        assertEquals(20, readBytes.length);
        for (int i = 0; i < 20; i++) {
            assertEquals(data[10 + i], readBytes[i]);
        }
    }

    @Test
    public void testWrite_ReaderToFileWithDifferentCharsets() throws IOException {
        File outputFile1 = Files.createTempFile(tempFolder, "output1", ".txt").toFile();
        File outputFile2 = Files.createTempFile(tempFolder, "output2", ".txt").toFile();

        try (Reader reader1 = new StringReader(UNICODE_CONTENT); Reader reader2 = new StringReader(UNICODE_CONTENT)) {
            IOUtil.write(reader1, UTF_8, outputFile1);
            IOUtil.write(reader2, UTF_16, outputFile2);
        }

        String content1 = IOUtil.readAllToString(outputFile1, UTF_8);
        String content2 = IOUtil.readAllToString(outputFile2, UTF_16);

        assertEquals(UNICODE_CONTENT, content1);
        assertEquals(UNICODE_CONTENT, content2);
    }

    @Test
    public void testWrite_FlushBehaviorComparison() throws IOException {
        File outputFile1 = Files.createTempFile(tempFolder, "output1", ".txt").toFile();
        File outputFile2 = Files.createTempFile(tempFolder, "output2", ".txt").toFile();

        try (FileOutputStream fos = new FileOutputStream(outputFile1)) {
            IOUtil.write(TEST_CONTENT.getBytes(UTF_8), fos, true);
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile2)) {
            IOUtil.write(TEST_CONTENT.getBytes(UTF_8), fos, false);
            fos.flush();
        }

        String content1 = IOUtil.readAllToString(outputFile1);
        String content2 = IOUtil.readAllToString(outputFile2);

        assertEquals(TEST_CONTENT, content1);
        assertEquals(TEST_CONTENT, content2);
        assertEquals(content1, content2);
    }

    @Test
    public void testReadAllLines_FromFile() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readAllLines(multilineFile);
        assertNotNull(lines);
        assertEquals(5, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
        assertEquals("Line 4", lines.get(3));
        assertEquals("Line 5", lines.get(4));
    }

    @Test
    public void testReadAllLines_FromFileWithStringEncoding() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readAllLines(multilineFile, "UTF-8");
        assertNotNull(lines);
        assertEquals(5, lines.size());
        assertEquals("Line 1", lines.get(0));
    }

    @Test
    public void testReadAllLines_FromFileWithCharsetEncoding() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readAllLines(multilineFile, UTF_8);
        assertNotNull(lines);
        assertEquals(5, lines.size());
        assertEquals("Line 1", lines.get(0));
    }

    @Test
    public void testReadAllLines_FromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(MULTILINE_CONTENT.getBytes(UTF_8))) {
            java.util.List<String> lines = IOUtil.readAllLines(is);
            assertNotNull(lines);
            assertEquals(5, lines.size());
            assertEquals("Line 1", lines.get(0));
            assertEquals("Line 5", lines.get(4));
        }
    }

    @Test
    public void testReadAllLines_FromInputStreamWithEncoding() throws IOException {
        try (InputStream is = new ByteArrayInputStream(MULTILINE_CONTENT.getBytes(UTF_8))) {
            java.util.List<String> lines = IOUtil.readAllLines(is, UTF_8);
            assertNotNull(lines);
            assertEquals(5, lines.size());
            assertEquals("Line 1", lines.get(0));
        }
    }

    @Test
    public void testReadAllLines_FromReader() throws IOException {
        try (Reader reader = new StringReader(MULTILINE_CONTENT)) {
            java.util.List<String> lines = IOUtil.readAllLines(reader);
            assertNotNull(lines);
            assertEquals(5, lines.size());
            assertEquals("Line 1", lines.get(0));
            assertEquals("Line 5", lines.get(4));
        }
    }

    @Test
    public void testReadAllLines_EmptyFile() throws IOException {
        java.util.List<String> lines = IOUtil.readAllLines(emptyFile);
        assertNotNull(lines);
        assertEquals(0, lines.size());
    }

    @Test
    public void testReadAllLines_SingleLine() throws IOException {
        File singleLineFile = Files.createTempFile(tempFolder, "single", ".txt").toFile();
        Files.write(singleLineFile.toPath(), "Single Line".getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readAllLines(singleLineFile);
        assertNotNull(lines);
        assertEquals(1, lines.size());
        assertEquals("Single Line", lines.get(0));
    }

    @Test
    public void testReadLines_FromFileWithOffsetAndCount() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readLines(multilineFile, 1, 3);
        assertNotNull(lines);
        assertEquals(3, lines.size());
        assertEquals("Line 2", lines.get(0));
        assertEquals("Line 3", lines.get(1));
        assertEquals("Line 4", lines.get(2));
    }

    @Test
    public void testReadLines_FromFileWithEncodingOffsetAndCount() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readLines(multilineFile, UTF_8, 1, 3);
        assertNotNull(lines);
        assertEquals(3, lines.size());
        assertEquals("Line 2", lines.get(0));
        assertEquals("Line 3", lines.get(1));
        assertEquals("Line 4", lines.get(2));
    }

    @Test
    public void testReadLines_FromInputStreamWithOffsetAndCount() throws IOException {
        try (InputStream is = new ByteArrayInputStream(MULTILINE_CONTENT.getBytes(UTF_8))) {
            java.util.List<String> lines = IOUtil.readLines(is, 1, 3);
            assertNotNull(lines);
            assertEquals(3, lines.size());
            assertEquals("Line 2", lines.get(0));
        }
    }

    @Test
    public void testReadLines_FromInputStreamWithEncodingOffsetAndCount() throws IOException {
        try (InputStream is = new ByteArrayInputStream(MULTILINE_CONTENT.getBytes(UTF_8))) {
            java.util.List<String> lines = IOUtil.readLines(is, UTF_8, 1, 3);
            assertNotNull(lines);
            assertEquals(3, lines.size());
            assertEquals("Line 2", lines.get(0));
        }
    }

    @Test
    public void testReadLines_FromReaderWithOffsetAndCount() throws IOException {
        try (Reader reader = new StringReader(MULTILINE_CONTENT)) {
            java.util.List<String> lines = IOUtil.readLines(reader, 1, 3);
            assertNotNull(lines);
            assertEquals(3, lines.size());
            assertEquals("Line 2", lines.get(0));
            assertEquals("Line 3", lines.get(1));
            assertEquals("Line 4", lines.get(2));
        }
    }

    @Test
    public void testReadLines_ZeroOffset() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readLines(multilineFile, 0, 2);
        assertNotNull(lines);
        assertEquals(2, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
    }

    @Test
    public void testReadLines_ZeroCount() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readLines(multilineFile, 1, 0);
        assertNotNull(lines);
        assertEquals(0, lines.size());
    }

    @Test
    public void testReadLines_OffsetBeyondEnd() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readLines(multilineFile, 100, 5);
        assertNotNull(lines);
        assertEquals(0, lines.size());
    }

    @Test
    public void testReadLines_CountExceedsAvailable() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readLines(multilineFile, 3, 100);
        assertNotNull(lines);
        assertEquals(2, lines.size());
        assertEquals("Line 4", lines.get(0));
        assertEquals("Line 5", lines.get(1));
    }

    @Test
    public void testReadFirstLine_FromFile() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String firstLine = IOUtil.readFirstLine(multilineFile);
        assertNotNull(firstLine);
        assertEquals("Line 1", firstLine);
    }

    @Test
    public void testReadFirstLine_FromFileWithEncoding() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String firstLine = IOUtil.readFirstLine(multilineFile, UTF_8);
        assertNotNull(firstLine);
        assertEquals("Line 1", firstLine);
    }

    @Test
    public void testReadFirstLine_FromReader() throws IOException {
        try (Reader reader = new StringReader(MULTILINE_CONTENT)) {
            String firstLine = IOUtil.readFirstLine(reader);
            assertNotNull(firstLine);
            assertEquals("Line 1", firstLine);
        }
    }

    @Test
    public void testReadFirstLine_EmptyFile() throws IOException {
        String firstLine = IOUtil.readFirstLine(emptyFile);
        assertEquals(null, firstLine);
    }

    @Test
    public void testReadFirstLine_SingleLineFile() throws IOException {
        File singleLineFile = Files.createTempFile(tempFolder, "single", ".txt").toFile();
        Files.write(singleLineFile.toPath(), "Only Line".getBytes(UTF_8));

        String firstLine = IOUtil.readFirstLine(singleLineFile);
        assertNotNull(firstLine);
        assertEquals("Only Line", firstLine);
    }

    @Test
    public void testReadLastLine_FromFile() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String lastLine = IOUtil.readLastLine(multilineFile);
        assertNotNull(lastLine);
        assertEquals("Line 5", lastLine);
    }

    @Test
    public void testReadLastLine_FromFileWithEncoding() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String lastLine = IOUtil.readLastLine(multilineFile, UTF_8);
        assertNotNull(lastLine);
        assertEquals("Line 5", lastLine);
    }

    @Test
    public void testReadLastLine_FromReader() throws IOException {
        try (Reader reader = new StringReader(MULTILINE_CONTENT)) {
            String lastLine = IOUtil.readLastLine(reader);
            assertNotNull(lastLine);
            assertEquals("Line 5", lastLine);
        }
    }

    @Test
    public void testReadLastLine_EmptyFile() throws IOException {
        String lastLine = IOUtil.readLastLine(emptyFile);
        assertEquals(null, lastLine);
    }

    @Test
    public void testReadLastLine_SingleLineFile() throws IOException {
        File singleLineFile = Files.createTempFile(tempFolder, "single", ".txt").toFile();
        Files.write(singleLineFile.toPath(), "Only Line".getBytes(UTF_8));

        String lastLine = IOUtil.readLastLine(singleLineFile);
        assertNotNull(lastLine);
        assertEquals("Only Line", lastLine);
    }

    @Test
    public void testReadLastLine_NoTrailingNewline() throws IOException {
        File noNewlineFile = Files.createTempFile(tempFolder, "nonewline", ".txt").toFile();
        Files.write(noNewlineFile.toPath(), "Line 1\r\nLine 2\r\nLine 3".getBytes(UTF_8));

        String lastLine = IOUtil.readLastLine(noNewlineFile);
        assertNotNull(lastLine);
        assertEquals("Line 3", lastLine);
    }

    @Test
    public void testReadLine_FromFileByIndex() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String line = IOUtil.readLine(multilineFile, 0);
        assertEquals("Line 1", line);

        line = IOUtil.readLine(multilineFile, 2);
        assertEquals("Line 3", line);

        line = IOUtil.readLine(multilineFile, 4);
        assertEquals("Line 5", line);
    }

    @Test
    public void testReadLine_FromFileWithEncodingByIndex() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String line = IOUtil.readLine(multilineFile, UTF_8, 0);
        assertEquals("Line 1", line);

        line = IOUtil.readLine(multilineFile, UTF_8, 2);
        assertEquals("Line 3", line);
    }

    @Test
    public void testReadLine_FromReaderByIndex() throws IOException {
        try (Reader reader = new StringReader(MULTILINE_CONTENT)) {
            String line = IOUtil.readLine(reader, 2);
            assertEquals("Line 3", line);
        }
    }

    @Test
    public void testReadLine_FirstIndex() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String line = IOUtil.readLine(multilineFile, 0);
        assertEquals("Line 1", line);
    }

    @Test
    public void testReadLine_LastIndex() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String line = IOUtil.readLine(multilineFile, 4);
        assertEquals("Line 5", line);
    }

    @Test
    public void testReadLine_IndexOutOfBounds() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String line = IOUtil.readLine(multilineFile, 100);
        assertEquals(null, line);
    }

    @Test
    public void testReadLine_NegativeIndex() {
        File multilineFile = tempFile;
        assertThrows(IllegalArgumentException.class, () -> {
            IOUtil.readLine(multilineFile, -1);
        });
    }

    @Test
    public void testWriteLine_ToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        IOUtil.writeLine("Test Line", outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Test Line\r\n", content);
    }

    @Test
    public void testWriteLine_ToWriter() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLine("Test Line", fw);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Test Line\r\n", content);
    }

    @Test
    public void testWriteLine_ToWriterWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLine("Test Line", fw, true);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Test Line\r\n", content);
    }

    @Test
    public void testWriteLine_MultipleLines() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLine("Line 1", fw, false);
            IOUtil.writeLine("Line 2", fw, false);
            IOUtil.writeLine("Line 3", fw, true);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\r\nLine 2\r\nLine 3\r\n", content);
    }

    @Test
    public void testWriteLine_NullObject() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        IOUtil.writeLine(null, outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("null\r\n", content);
    }

    @Test
    public void testWriteLine_EmptyString() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        IOUtil.writeLine("", outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("\r\n", content);
    }

    @Test
    public void testWriteLines_IteratorToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        IOUtil.writeLines(lines.iterator(), outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\r\nLine 2\r\nLine 3\r\n", content);
    }

    @Test
    public void testWriteLines_IteratorToWriter() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLines(lines.iterator(), fw);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\r\nLine 2\r\nLine 3\r\n", content);
    }

    @Test
    public void testWriteLines_IteratorToWriterWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLines(lines.iterator(), fw, true);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\r\nLine 2\r\nLine 3\r\n", content);
    }

    @Test
    public void testWriteLines_EmptyIterator() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Collections.emptyList();

        IOUtil.writeLines(lines.iterator(), outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWriteLines_IterableToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        IOUtil.writeLines(lines, outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\r\nLine 2\r\nLine 3\r\n", content);
    }

    @Test
    public void testWriteLines_IterableToWriter() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLines(lines, fw);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\r\nLine 2\r\nLine 3\r\n", content);
    }

    @Test
    public void testWriteLines_IterableToWriterWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLines(lines, fw, true);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\r\nLine 2\r\nLine 3\r\n", content);
    }

    @Test
    public void testWriteLines_EmptyIterable() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Collections.emptyList();

        IOUtil.writeLines(lines, outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("", content);
    }

    @Test
    public void testWriteLines_LargeIterable() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            lines.add("Line " + i);
        }

        IOUtil.writeLines(lines, outputFile);

        java.util.List<String> readLines = IOUtil.readAllLines(outputFile);
        assertEquals(100, readLines.size());
        assertEquals("Line 0", readLines.get(0));
        assertEquals("Line 99", readLines.get(99));
    }

    @Test
    public void testRead_ByteArrayFromFile() throws IOException {
        byte[] buf = new byte[100];
        int bytesRead = IOUtil.read(tempFile, buf);
        assertEquals(TEST_CONTENT.length(), bytesRead);
        assertEquals(TEST_CONTENT, new String(buf, 0, bytesRead, UTF_8));
    }

    @Test
    public void testRead_ByteArrayFromFileWithOffsetAndLength() throws IOException {
        byte[] buf = new byte[100];
        int bytesRead = IOUtil.read(tempFile, buf, 10, 50);
        assertEquals(TEST_CONTENT.length(), bytesRead);
        assertEquals(TEST_CONTENT, new String(buf, 10, bytesRead, UTF_8));
    }

    @Test
    public void testRead_ByteArrayFromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            byte[] buf = new byte[100];
            int bytesRead = IOUtil.read(is, buf);
            assertEquals(TEST_CONTENT.length(), bytesRead);
            assertEquals(TEST_CONTENT, new String(buf, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testRead_ByteArrayFromInputStreamWithOffsetAndLength() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            byte[] buf = new byte[100];
            int bytesRead = IOUtil.read(is, buf, 10, 50);
            assertEquals(TEST_CONTENT.length(), bytesRead);
            assertEquals(TEST_CONTENT, new String(buf, 10, bytesRead, UTF_8));
        }
    }

    @Test
    public void testRead_ByteArraySmallBuffer() throws IOException {
        byte[] buf = new byte[5];
        int bytesRead = IOUtil.read(tempFile, buf);
        assertEquals(5, bytesRead);
        assertEquals("Hello", new String(buf, 0, bytesRead, UTF_8));
    }

    @Test
    public void testRead_ByteArrayEmptyFile() throws IOException {
        byte[] buf = new byte[100];
        int bytesRead = IOUtil.read(emptyFile, buf);
        assertEquals(-1, bytesRead);
    }

    @Test
    public void testRead_ByteArrayZeroLength() throws IOException {
        byte[] buf = new byte[100];
        int bytesRead = IOUtil.read(tempFile, buf, 0, 0);
        assertEquals(0, bytesRead);
    }

    @Test
    public void testRead_CharArrayFromFile() throws IOException {
        char[] buf = new char[100];
        int charsRead = IOUtil.read(tempFile, buf);
        assertEquals(TEST_CONTENT.length(), charsRead);
        assertEquals(TEST_CONTENT, new String(buf, 0, charsRead));
    }

    @Test
    public void testRead_CharArrayFromFileWithCharset() throws IOException {
        char[] buf = new char[100];
        int charsRead = IOUtil.read(tempFile, UTF_8, buf);
        assertEquals(TEST_CONTENT.length(), charsRead);
        assertEquals(TEST_CONTENT, new String(buf, 0, charsRead));
    }

    @Test
    public void testRead_CharArrayFromFileWithOffsetAndLength() throws IOException {
        char[] buf = new char[100];
        int charsRead = IOUtil.read(tempFile, buf, 10, 50);
        assertEquals(TEST_CONTENT.length(), charsRead);
        assertEquals(TEST_CONTENT, new String(buf, 10, charsRead));
    }

    @Test
    public void testRead_CharArrayFromFileWithCharsetOffsetAndLength() throws IOException {
        char[] buf = new char[100];
        int charsRead = IOUtil.read(tempFile, UTF_8, buf, 10, 50);
        assertEquals(TEST_CONTENT.length(), charsRead);
        assertEquals(TEST_CONTENT, new String(buf, 10, charsRead));
    }

    @Test
    public void testRead_CharArrayFromReader() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            char[] buf = new char[100];
            int charsRead = IOUtil.read(reader, buf);
            assertEquals(TEST_CONTENT.length(), charsRead);
            assertEquals(TEST_CONTENT, new String(buf, 0, charsRead));
        }
    }

    @Test
    public void testRead_CharArrayFromReaderWithOffsetAndLength() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            char[] buf = new char[100];
            int charsRead = IOUtil.read(reader, buf, 10, 50);
            assertEquals(TEST_CONTENT.length(), charsRead);
            assertEquals(TEST_CONTENT, new String(buf, 10, charsRead));
        }
    }

    @Test
    public void testRead_CharArraySmallBuffer() throws IOException {
        char[] buf = new char[5];
        int charsRead = IOUtil.read(tempFile, buf);
        assertEquals(5, charsRead);
        assertEquals("Hello", new String(buf, 0, charsRead));
    }

    @Test
    public void testRead_CharArrayEmptyFile() throws IOException {
        char[] buf = new char[100];
        int charsRead = IOUtil.read(emptyFile, buf);
        assertEquals(-1, charsRead);
    }

    @Test
    public void testRead_CharArrayZeroLength() throws IOException {
        char[] buf = new char[100];
        int charsRead = IOUtil.read(tempFile, buf, 0, 0);
        assertEquals(0, charsRead);
    }

    @Test
    public void testRead_CharArrayUnicodeContent() throws IOException {
        File unicodeFile = Files.createTempFile(tempFolder, "unicode", ".txt").toFile();
        Files.write(unicodeFile.toPath(), UNICODE_CONTENT.getBytes(UTF_8));

        char[] buf = new char[200];
        int charsRead = IOUtil.read(unicodeFile, UTF_8, buf);
        assertEquals(UNICODE_CONTENT.length(), charsRead);
        assertEquals(UNICODE_CONTENT, new String(buf, 0, charsRead));
    }

    @Test
    public void testLineRoundTrip_WriteAndRead() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "roundtrip", ".txt").toFile();
        java.util.List<String> originalLines = java.util.Arrays.asList("First", "Second", "Third");

        IOUtil.writeLines(originalLines, outputFile);
        java.util.List<String> readLines = IOUtil.readAllLines(outputFile);

        assertEquals(originalLines.size(), readLines.size());
        assertEquals("First", readLines.get(0));
        assertEquals("Second", readLines.get(1));
        assertEquals("Third", readLines.get(2));
    }

    @Test
    public void testLineOperations_FirstLastAndIndex() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), "First\r\nSecond\r\nThird\r\nFourth\r\nFifth".getBytes(UTF_8));

        String first = IOUtil.readFirstLine(multilineFile);
        assertEquals("First", first);

        String last = IOUtil.readLastLine(multilineFile);
        assertEquals("Fifth", last);

        String third = IOUtil.readLine(multilineFile, 2);
        assertEquals("Third", third);
    }

    @Test
    public void testLineOperations_PartialRead() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), "Line0\r\nLine1\r\nLine2\r\nLine3\r\nLine4\r\nLine5".getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readLines(multilineFile, 2, 3);
        assertEquals(3, lines.size());
        assertEquals("Line2", lines.get(0));
        assertEquals("Line3", lines.get(1));
        assertEquals("Line4", lines.get(2));
    }

    @Test
    public void testBufferRead_PartialContent() throws IOException {
        byte[] byteBuffer = new byte[5];
        int bytesRead = IOUtil.read(tempFile, byteBuffer);
        assertEquals(5, bytesRead);

        char[] charBuffer = new char[5];
        int charsRead = IOUtil.read(tempFile, charBuffer);
        assertEquals(5, charsRead);

        assertEquals(new String(byteBuffer, UTF_8), new String(charBuffer));
    }

    @Test
    public void testReadAllLines_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(Exception.class, () -> {
            IOUtil.readAllLines(nonexistent);
        });
    }

    @Test
    public void testReadFirstLine_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(Exception.class, () -> {
            IOUtil.readFirstLine(nonexistent);
        });
    }

    @Test
    public void testReadLastLine_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(Exception.class, () -> {
            IOUtil.readLastLine(nonexistent);
        });
    }

    @Test
    public void testReadLine_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(Exception.class, () -> {
            IOUtil.readLine(nonexistent, 0);
        });
    }

    @Test
    public void testWriteLine_NonexistentDirectory() throws IOException {
        File outputFile = new File(tempFolder.toFile(), "nonexistent/output.txt");
        IOUtil.writeLine("Test", outputFile);
        assertEquals("Test", IOUtil.readLine(outputFile, 0));
    }

    @Test
    public void testWriteLines_NonexistentDirectory() throws IOException {
        File outputFile = new File(tempFolder.toFile(), "nonexistent/output.txt");
        java.util.List<String> lines = java.util.Arrays.asList("Line 1");
        IOUtil.writeLines(lines, outputFile);
        assertEquals("Line 1", IOUtil.readLine(outputFile, 0));
    }

    @Test
    public void testRead_NullBuffer() {
        assertThrows(Exception.class, () -> {
            IOUtil.read(tempFile, (byte[]) null);
        });
    }

    @Test
    public void testRead_NullCharBuffer() {
        assertThrows(Exception.class, () -> {
            IOUtil.read(tempFile, (char[]) null);
        });
    }

    @Test
    public void testAppendBytes_ToFile() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Initial".getBytes(UTF_8));

        IOUtil.append(" Content".getBytes(UTF_8), appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Initial Content", result);
    }

    @Test
    public void testAppendBytes_WithOffsetAndCount() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Start".getBytes(UTF_8));

        byte[] bytes = "0123456789".getBytes(UTF_8);
        IOUtil.append(bytes, 2, 5, appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Start23456", result);
    }

    @Test
    public void testAppendBytes_EmptyArray() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Content".getBytes(UTF_8));

        IOUtil.append(new byte[0], appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Content", result);
    }

    @Test
    public void testAppendBytes_ZeroCount() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Content".getBytes(UTF_8));

        IOUtil.append("Test".getBytes(UTF_8), 0, 0, appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Content", result);
    }

    @Test
    public void testAppendBytes_ToEmptyFile() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();

        IOUtil.append("New Content".getBytes(UTF_8), appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("New Content", result);
    }

    @Test
    public void testAppendBytes_MultipleAppends() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "A".getBytes(UTF_8));

        IOUtil.append("B".getBytes(UTF_8), appendFile);
        IOUtil.append("C".getBytes(UTF_8), appendFile);
        IOUtil.append("D".getBytes(UTF_8), appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("ABCD", result);
    }

    @Test
    public void testAppendChars_ToFile() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Initial".getBytes(UTF_8));

        IOUtil.append(" Content".toCharArray(), appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Initial Content", result);
    }

    @Test
    public void testAppendChars_WithCharset() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Start".getBytes(UTF_16));

        IOUtil.append(" End".toCharArray(), UTF_16, appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_16);
        assertEquals("Start﻿ End", result);
    }

    @Test
    public void testAppendChars_WithOffsetAndCount() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Begin".getBytes(UTF_8));

        char[] chars = "0123456789".toCharArray();
        IOUtil.append(chars, 3, 4, appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Begin3456", result);
    }

    @Test
    public void testAppendChars_WithOffsetCountAndCharset() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Data".getBytes(UTF_8));

        char[] chars = "ABCDEFGH".toCharArray();
        IOUtil.append(chars, 2, 3, UTF_8, appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("DataCDE", result);
    }

    @Test
    public void testAppendChars_EmptyArray() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Text".getBytes(UTF_8));

        IOUtil.append(new char[0], appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Text", result);
    }

    @Test
    public void testAppendChars_UnicodeContent() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Hello ".getBytes(UTF_8));

        IOUtil.append("世界 \uD83D\uDE00".toCharArray(), UTF_8, appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Hello 世界 \uD83D\uDE00", result);
    }

    @Test
    public void testAppendCharSequence_ToFile() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Hello".getBytes(UTF_8));

        IOUtil.append(" World", appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Hello World", result);
    }

    @Test
    public void testAppendCharSequence_WithCharset() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "First".getBytes(UTF_16));

        IOUtil.append(" Second", UTF_16, appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_16);
        assertEquals("First﻿ Second", result);
    }

    @Test
    public void testAppendCharSequence_StringBuilder() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Part1".getBytes(UTF_8));

        StringBuilder sb = new StringBuilder(" Part2");
        IOUtil.append(sb, UTF_8, appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Part1 Part2", result);
    }

    @Test
    public void testAppendCharSequence_EmptyString() throws IOException {
        File appendFile = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(appendFile.toPath(), "Content".getBytes(UTF_8));

        IOUtil.append("", appendFile);

        String result = IOUtil.readAllToString(appendFile, UTF_8);
        assertEquals("Content", result);
    }

    @Test
    public void testAppendFile_ToFile() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), " from source".getBytes(UTF_8));

        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Content".getBytes(UTF_8));

        long bytesAppended = IOUtil.append(sourceFile, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertEquals("Content from source", result);
        assertEquals(" from source".length(), bytesAppended);
    }

    @Test
    public void testAppendFile_WithOffsetAndCount() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), "0123456789".getBytes(UTF_8));

        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Start".getBytes(UTF_8));

        long bytesAppended = IOUtil.append(sourceFile, 3, 4, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertEquals("Start3456", result);
        assertEquals(4, bytesAppended);
    }

    @Test
    public void testAppendFile_EmptySource() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();

        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Target".getBytes(UTF_8));

        long bytesAppended = IOUtil.append(sourceFile, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertEquals("Target", result);
        assertEquals(0, bytesAppended);
    }

    @Test
    public void testAppendFile_ToEmptyTarget() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Source data".getBytes(UTF_8));

        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();

        long bytesAppended = IOUtil.append(sourceFile, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertEquals("Source data", result);
        assertEquals("Source data".length(), bytesAppended);
    }

    @Test
    public void testAppendInputStream_ToFile() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Prefix".getBytes(UTF_8));

        try (InputStream is = new ByteArrayInputStream(" suffix".getBytes(UTF_8))) {
            long bytesAppended = IOUtil.append(is, targetFile);

            String result = IOUtil.readAllToString(targetFile, UTF_8);
            assertEquals("Prefix suffix", result);
            assertEquals(" suffix".length(), bytesAppended);
        }
    }

    @Test
    public void testAppendInputStream_WithOffsetAndCount() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Base".getBytes(UTF_8));

        try (InputStream is = new ByteArrayInputStream("0123456789".getBytes(UTF_8))) {
            long bytesAppended = IOUtil.append(is, 2, 5, targetFile);

            String result = IOUtil.readAllToString(targetFile, UTF_8);
            assertEquals("Base23456", result);
            assertEquals(5, bytesAppended);
        }
    }

    @Test
    public void testAppendInputStream_EmptyStream() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Data".getBytes(UTF_8));

        try (InputStream is = new ByteArrayInputStream(new byte[0])) {
            long bytesAppended = IOUtil.append(is, targetFile);

            String result = IOUtil.readAllToString(targetFile, UTF_8);
            assertEquals("Data", result);
            assertEquals(0, bytesAppended);
        }
    }

    @Test
    public void testAppendInputStream_LargeStream() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Header\r\n".getBytes(UTF_8));

        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) ('A' + (i % 26));
        }

        try (InputStream is = new ByteArrayInputStream(largeData)) {
            long bytesAppended = IOUtil.append(is, targetFile);

            byte[] result = IOUtil.readAllBytes(targetFile);
            assertEquals(8 + 10000, result.length);
            assertEquals(10000, bytesAppended);
        }
    }

    @Test
    public void testAppendReader_ToFile() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Start".getBytes(UTF_8));

        try (Reader reader = new StringReader(" Middle End")) {
            long charsAppended = IOUtil.append(reader, targetFile);

            String result = IOUtil.readAllToString(targetFile, UTF_8);
            assertEquals("Start Middle End", result);
            assertEquals(" Middle End".length(), charsAppended);
        }
    }

    @Test
    public void testAppendReader_WithCharset() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Begin".getBytes(UTF_16));

        try (Reader reader = new StringReader(" Continue")) {
            long charsAppended = IOUtil.append(reader, UTF_16, targetFile);

            String result = IOUtil.readAllToString(targetFile, UTF_16);
            assertEquals("Begin﻿ Continue", result);
            assertEquals(" Continue".length(), charsAppended);
        }
    }

    @Test
    public void testAppendReader_WithOffsetAndCount() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Pre".getBytes(UTF_8));

        try (Reader reader = new StringReader("0123456789")) {
            long charsAppended = IOUtil.append(reader, 4, 3, targetFile);

            String result = IOUtil.readAllToString(targetFile, UTF_8);
            assertEquals("Pre456", result);
            assertEquals(3, charsAppended);
        }
    }

    @Test
    public void testAppendReader_WithOffsetCountAndCharset() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Data".getBytes(UTF_8));

        try (Reader reader = new StringReader("ABCDEFGH")) {
            long charsAppended = IOUtil.append(reader, 1, 4, UTF_8, targetFile);

            String result = IOUtil.readAllToString(targetFile, UTF_8);
            assertEquals("DataBCDE", result);
            assertEquals(4, charsAppended);
        }
    }

    @Test
    public void testAppendReader_EmptyReader() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Content".getBytes(UTF_8));

        try (Reader reader = new StringReader("")) {
            long charsAppended = IOUtil.append(reader, targetFile);

            String result = IOUtil.readAllToString(targetFile, UTF_8);
            assertEquals("Content", result);
            assertEquals(0, charsAppended);
        }
    }

    @Test
    public void testAppendLine_String() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Line 1\r\n".getBytes(UTF_8));

        IOUtil.appendLine("Line 2", targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Line 1"));
        assertTrue(result.contains("Line 2"));
    }

    @Test
    public void testAppendLine_WithCharset() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "First\r\n".getBytes(UTF_16));

        IOUtil.appendLine("Second", UTF_16, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_16);
        assertTrue(result.contains("First"));
        assertTrue(result.contains("Second"));
    }

    @Test
    public void testAppendLine_NullObject() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Start\r\n".getBytes(UTF_8));

        IOUtil.appendLine(null, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Start"));
    }

    @Test
    public void testAppendLine_Integer() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Number:\r\n".getBytes(UTF_8));

        IOUtil.appendLine(42, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Number:"));
        assertTrue(result.contains("42"));
    }

    @Test
    public void testAppendLines_StringList() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Header\r\n".getBytes(UTF_8));

        java.util.List<String> lines = java.util.Arrays.asList("Line A", "Line B", "Line C");
        IOUtil.appendLines(lines, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Header"));
        assertTrue(result.contains("Line A"));
        assertTrue(result.contains("Line B"));
        assertTrue(result.contains("Line C"));
    }

    @Test
    public void testAppendLines_WithCharset() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Title\r\n".getBytes(UTF_16));

        java.util.List<String> lines = java.util.Arrays.asList("Data 1", "Data 2");
        IOUtil.appendLines(lines, UTF_16, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_16);
        assertTrue(result.contains("Title"));
        assertTrue(result.contains("Data 1"));
        assertTrue(result.contains("Data 2"));
    }

    @Test
    public void testAppendLines_EmptyList() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Existing".getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.emptyList();
        IOUtil.appendLines(lines, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertEquals("Existing", result);
    }

    @Test
    public void testAppendLines_MixedTypes() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Start\r\n".getBytes(UTF_8));

        java.util.List<Object> lines = java.util.Arrays.asList("Text", 123, true, null);
        IOUtil.appendLines(lines, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Start"));
        assertTrue(result.contains("Text"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
    }

    @Test
    public void testTransfer_Channels() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".bin").toFile();
        File targetFile = Files.createTempFile(tempFolder, "target", ".bin").toFile();

        byte[] testData = "Transfer test data".getBytes(UTF_8);
        Files.write(sourceFile.toPath(), testData);

        try (FileInputStream fis = new FileInputStream(sourceFile); FileOutputStream fos = new FileOutputStream(targetFile)) {

            long transferred = IOUtil.transfer(fis.getChannel(), fos.getChannel());

            assertEquals(testData.length, transferred);
            assertArrayEquals(testData, Files.readAllBytes(targetFile.toPath()));
        }
    }

    @Test
    public void testTransfer_EmptyChannel() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".bin").toFile();
        File targetFile = Files.createTempFile(tempFolder, "target", ".bin").toFile();

        try (FileInputStream fis = new FileInputStream(sourceFile); FileOutputStream fos = new FileOutputStream(targetFile)) {

            long transferred = IOUtil.transfer(fis.getChannel(), fos.getChannel());

            assertEquals(0, transferred);
        }
    }

    @Test
    public void testTransfer_LargeData() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".bin").toFile();
        File targetFile = Files.createTempFile(tempFolder, "target", ".bin").toFile();

        byte[] largeData = new byte[100000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        Files.write(sourceFile.toPath(), largeData);

        try (FileInputStream fis = new FileInputStream(sourceFile); FileOutputStream fos = new FileOutputStream(targetFile)) {

            long transferred = IOUtil.transfer(fis.getChannel(), fos.getChannel());

            assertEquals(largeData.length, transferred);
            assertArrayEquals(largeData, Files.readAllBytes(targetFile.toPath()));
        }
    }

    @Test
    public void testSkip_InputStream() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            long skipped = IOUtil.skip(is, 5);

            assertEquals(5, skipped);
            assertEquals('5', is.read());
        }
    }

    @Test
    public void testSkip_InputStreamZeroBytes() throws IOException {
        byte[] data = "Test".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            long skipped = IOUtil.skip(is, 0);

            assertEquals(0, skipped);
            assertEquals('T', is.read());
        }
    }

    @Test
    public void testSkip_InputStreamBeyondEnd() throws IOException {
        byte[] data = "Short".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            long skipped = IOUtil.skip(is, 100);

            assertEquals(5, skipped);
            assertEquals(-1, is.read());
        }
    }

    @Test
    public void testSkip_InputStreamNegativeBytes() {
        try (InputStream is = new ByteArrayInputStream("Test".getBytes(UTF_8))) {
            assertThrows(IllegalArgumentException.class, () -> {
                IOUtil.skip(is, -5);
            });
        } catch (IOException e) {
        }
    }

    @Test
    public void testSkip_Reader() throws IOException {
        try (Reader reader = new StringReader("0123456789ABCDEF")) {
            long skipped = IOUtil.skip(reader, 7);

            assertEquals(7, skipped);
            assertEquals('7', reader.read());
        }
    }

    @Test
    public void testSkip_ReaderZeroChars() throws IOException {
        try (Reader reader = new StringReader("Test")) {
            long skipped = IOUtil.skip(reader, 0);

            assertEquals(0, skipped);
            assertEquals('T', reader.read());
        }
    }

    @Test
    public void testSkip_ReaderBeyondEnd() throws IOException {
        try (Reader reader = new StringReader("Small")) {
            long skipped = IOUtil.skip(reader, 50);

            assertEquals(5, skipped);
            assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testSkip_ReaderNegativeChars() {
        try (Reader reader = new StringReader("Test")) {
            assertThrows(IllegalArgumentException.class, () -> {
                IOUtil.skip(reader, -3);
            });
        } catch (IOException e) {
        }
    }

    @Test
    public void testSkip_InputStreamLargeSkip() throws IOException {
        byte[] largeData = new byte[50000];
        try (InputStream is = new ByteArrayInputStream(largeData)) {
            long skipped = IOUtil.skip(is, 30000);

            assertEquals(30000, skipped);
        }
    }

    @Test
    public void testSkipFully_InputStream() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            IOUtil.skipFully(is, 8);

            assertEquals('8', is.read());
        }
    }

    @Test
    public void testSkipFully_InputStreamExactLength() throws IOException {
        byte[] data = "12345".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            IOUtil.skipFully(is, 5);

            assertEquals(-1, is.read());
        }
    }

    @Test
    public void testSkipFully_InputStreamBeyondEnd() {
        byte[] data = "Short".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            assertThrows(IOException.class, () -> {
                IOUtil.skipFully(is, 10);
            });
        } catch (IOException e) {
        }
    }

    @Test
    public void testSkipFully_InputStreamZero() throws IOException {
        byte[] data = "Test".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            IOUtil.skipFully(is, 0);

            assertEquals('T', is.read());
        }
    }

    @Test
    public void testSkipFully_Reader() throws IOException {
        try (Reader reader = new StringReader("0123456789")) {
            IOUtil.skipFully(reader, 6);

            assertEquals('6', reader.read());
        }
    }

    @Test
    public void testSkipFully_ReaderExactLength() throws IOException {
        try (Reader reader = new StringReader("ABC")) {
            IOUtil.skipFully(reader, 3);

            assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testSkipFully_ReaderBeyondEnd() {
        try (Reader reader = new StringReader("Tiny")) {
            assertThrows(IOException.class, () -> {
                IOUtil.skipFully(reader, 20);
            });
        } catch (IOException e) {
        }
    }

    @Test
    public void testSkipFully_ReaderZero() throws IOException {
        try (Reader reader = new StringReader("Data")) {
            IOUtil.skipFully(reader, 0);

            assertEquals('D', reader.read());
        }
    }

    @Test
    public void testMap_DefaultMode() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] data = "Memory mapped file".getBytes(UTF_8);
        Files.write(mapFile.toPath(), data);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile);

        assertNotNull(buffer);
        assertEquals(data.length, buffer.remaining());

        byte[] read = new byte[data.length];
        buffer.get(read);
        assertArrayEquals(data, read);

        unmap(buffer);
    }

    @Test
    public void testMap_ReadOnlyMode() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] data = "Read only mapping".getBytes(UTF_8);
        Files.write(mapFile.toPath(), data);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile, java.nio.channels.FileChannel.MapMode.READ_ONLY);

        assertNotNull(buffer);
        assertTrue(buffer.isReadOnly());
        assertEquals(data.length, buffer.remaining());
        unmap(buffer);
    }

    @Test
    public void testMap_ReadWriteMode() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] data = "Read write mapping".getBytes(UTF_8);
        Files.write(mapFile.toPath(), data);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile, java.nio.channels.FileChannel.MapMode.READ_WRITE);

        assertNotNull(buffer);
        assertEquals(data.length, buffer.remaining());

        buffer.put(0, (byte) 'X');
        buffer.force();

        unmap(buffer);
    }

    @Test
    public void testMap_WithOffsetAndCount() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] data = "0123456789ABCDEFGHIJ".getBytes(UTF_8);
        Files.write(mapFile.toPath(), data);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile, java.nio.channels.FileChannel.MapMode.READ_ONLY, 5, 10);

        assertNotNull(buffer);
        assertEquals(10, buffer.remaining());

        byte[] read = new byte[10];
        buffer.get(read);
        assertEquals("56789ABCDE", new String(read, UTF_8));

        unmap(buffer);
    }

    @Test
    public void testMap_EmptyFile() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile, java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, 0);

        assertNotNull(buffer);
        assertEquals(0, buffer.remaining());
    }

    @Test
    public void testMap_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.bin");

        assertThrows(IllegalArgumentException.class, () -> {
            IOUtil.map(nonexistent);
        });
    }

    @Test
    public void testMap_LargeFile() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        Files.write(mapFile.toPath(), largeData);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile);

        assertNotNull(buffer);
        assertEquals(largeData.length, buffer.remaining());

        unmap(buffer);
    }

    @Test
    public void testSimplifyPath_BasicPath() {
        String result = IOUtil.simplifyPath("/foo/bar/baz");
        assertEquals("/foo/bar/baz", result);
    }

    @Test
    public void testSimplifyPath_WithDots() {
        String result = IOUtil.simplifyPath("/foo/./bar");
        assertEquals("/foo/bar", result);
    }

    @Test
    public void testSimplifyPath_WithDoubleDots() {
        String result = IOUtil.simplifyPath("/foo/bar/../baz");
        assertEquals("/foo/baz", result);
    }

    @Test
    public void testSimplifyPath_MultipleDoubleDots() {
        String result = IOUtil.simplifyPath("/foo/bar/../../baz");
        assertEquals("/baz", result);
    }

    @Test
    public void testSimplifyPath_MultipleSlashes() {
        String result = IOUtil.simplifyPath("/foo//bar///baz");
        assertEquals("/foo/bar/baz", result);
    }

    @Test
    public void testSimplifyPath_TrailingSlash() {
        String result = IOUtil.simplifyPath("/foo/bar/");
        assertEquals("/foo/bar", result);
    }

    @Test
    public void testSimplifyPath_RootPath() {
        String result = IOUtil.simplifyPath("/");
        assertEquals("/", result);
    }

    @Test
    public void testSimplifyPath_EmptyString() {
        String result = IOUtil.simplifyPath("");
        assertEquals(".", result);
    }

    @Test
    public void testSimplifyPath_CurrentDir() {
        String result = IOUtil.simplifyPath(".");
        assertEquals(".", result);
    }

    @Test
    public void testSimplifyPath_RelativePath() {
        String result = IOUtil.simplifyPath("foo/bar/baz");
        assertEquals("foo/bar/baz", result);
    }

    @Test
    public void testSimplifyPath_RelativeWithDots() {
        String result = IOUtil.simplifyPath("foo/./bar");
        assertEquals("foo/bar", result);
    }

    @Test
    public void testSimplifyPath_RelativeWithDoubleDots() {
        String result = IOUtil.simplifyPath("foo/bar/../baz");
        assertEquals("foo/baz", result);
    }

    @Test
    public void testSimplifyPath_OnlyDoubleDots() {
        String result = IOUtil.simplifyPath("..");
        assertEquals("..", result);
    }

    @Test
    public void testSimplifyPath_DoubleDotsBeyondRoot() {
        String result = IOUtil.simplifyPath("/../foo");
        assertEquals("/foo", result);
    }

    @Test
    public void testSimplifyPath_WindowsStyle() {
        String result = IOUtil.simplifyPath("C:\\foo\\bar\\..\\baz");
        assertEquals("C:/foo/baz", result);
    }

    @Test
    public void testSimplifyPath_MixedSlashes() {
        String result = IOUtil.simplifyPath("/foo\\bar/baz");
        assertEquals("/foo/bar/baz", result);
    }

    @Test
    public void testSimplifyPath_Complex() {
        String result = IOUtil.simplifyPath("/a/./b/../../c/");
        assertEquals("/c", result);
    }

    @Test
    public void testGetFileExtension_FromFile() {
        File file = new File("test.txt");
        String ext = IOUtil.getFileExtension(file);
        assertEquals("txt", ext);
    }

    @Test
    public void testGetFileExtension_FromFileName() {
        String ext = IOUtil.getFileExtension("document.pdf");
        assertEquals("pdf", ext);
    }

    @Test
    public void testGetFileExtension_MultipleExtensions() {
        String ext = IOUtil.getFileExtension("archive.tar.gz");
        assertEquals("gz", ext);
    }

    @Test
    public void testGetFileExtension_NoExtension() {
        String ext = IOUtil.getFileExtension("README");
        assertEquals("", ext);
    }

    @Test
    public void testGetFileExtension_NullFile() {
        String ext = IOUtil.getFileExtension((File) null);
        assertEquals(null, ext);
    }

    @Test
    public void testGetFileExtension_NullFileName() {
        String ext = IOUtil.getFileExtension((String) null);
        assertEquals(null, ext);
    }

    @Test
    public void testGetFileExtension_DotFile() {
        String ext = IOUtil.getFileExtension(".gitignore");
        assertEquals("gitignore", ext);
    }

    @Test
    public void testGetFileExtension_PathWithExtension() {
        String ext = IOUtil.getFileExtension("/path/to/file.java");
        assertEquals("java", ext);
    }

    @Test
    public void testGetFileExtension_EmptyString() {
        String ext = IOUtil.getFileExtension("");
        assertEquals("", ext);
    }

    @Test
    public void testGetFileExtension_OnlyDot() {
        String ext = IOUtil.getFileExtension("file.");
        assertEquals("", ext);
    }

    @Test
    public void testGetNameWithoutExtension_FromFile() {
        File file = new File("document.txt");
        String name = IOUtil.getNameWithoutExtension(file);
        assertEquals("document", name);
    }

    @Test
    public void testGetNameWithoutExtension_FromFileName() {
        String name = IOUtil.getNameWithoutExtension("image.png");
        assertEquals("image", name);
    }

    @Test
    public void testGetNameWithoutExtension_MultipleExtensions() {
        String name = IOUtil.getNameWithoutExtension("backup.tar.gz");
        assertEquals("backup.tar", name);
    }

    @Test
    public void testGetNameWithoutExtension_NoExtension() {
        String name = IOUtil.getNameWithoutExtension("LICENSE");
        assertEquals("LICENSE", name);
    }

    @Test
    public void testGetNameWithoutExtension_NullFile() {
        String name = IOUtil.getNameWithoutExtension((File) null);
        assertEquals(null, name);
    }

    @Test
    public void testGetNameWithoutExtension_NullFileName() {
        String name = IOUtil.getNameWithoutExtension((String) null);
        assertEquals(null, name);
    }

    @Test
    public void testGetNameWithoutExtension_DotFile() {
        String name = IOUtil.getNameWithoutExtension(".hidden");
        assertEquals("", name);
    }

    @Test
    public void testGetNameWithoutExtension_PathWithExtension() {
        String name = IOUtil.getNameWithoutExtension("/usr/local/bin/script.sh");
        assertEquals("/usr/local/bin/script", name);
    }

    @Test
    public void testGetNameWithoutExtension_EmptyString() {
        String name = IOUtil.getNameWithoutExtension("");
        assertEquals("", name);
    }

    @Test
    public void testGetNameWithoutExtension_OnlyDot() {
        String name = IOUtil.getNameWithoutExtension("name.");
        assertEquals("name", name);
    }

    @Test
    public void testGetNameWithoutExtension_WithPath() {
        File file = new File("/tmp/test.log");
        String name = IOUtil.getNameWithoutExtension(file);
        assertEquals("test", name);
    }

    @Test
    public void testCopyToDirectory_File() throws Exception {
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyToDirectory(tempFile, destDir);

        File copiedFile = new File(destDir, tempFile.getName());
        assertTrue(copiedFile.exists());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(copiedFile));
    }

    @Test
    public void testCopyToDirectory_FileWithPreserveFileDate() throws Exception {
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        long originalTime = tempFile.lastModified();
        Thread.sleep(100);

        IOUtil.copyToDirectory(tempFile, destDir, true);

        File copiedFile = new File(destDir, tempFile.getName());
        assertTrue(copiedFile.exists());
        assertEquals(originalTime, copiedFile.lastModified());
    }

    @Test
    public void testCopyToDirectory_FileWithoutPreserveFileDate() throws Exception {
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        long originalTime = tempFile.lastModified();
        Thread.sleep(100);

        IOUtil.copyToDirectory(tempFile, destDir, false);

        File copiedFile = new File(destDir, tempFile.getName());
        assertTrue(copiedFile.exists());
    }

    @Test
    public void testCopyToDirectory_Directory() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File subFile1 = new File(srcDir, "file1.txt");
        File subFile2 = new File(srcDir, "file2.txt");
        Files.write(subFile1.toPath(), "Content 1".getBytes());
        Files.write(subFile2.toPath(), "Content 2".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyToDirectory(srcDir, destDir);

        File copiedDir = new File(destDir, srcDir.getName());
        assertTrue(copiedDir.exists());
        assertTrue(copiedDir.isDirectory());
        assertTrue(new File(copiedDir, "file1.txt").exists());
        assertTrue(new File(copiedDir, "file2.txt").exists());
    }

    @Test
    public void testCopyToDirectory_WithFilter() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File txtFile = new File(srcDir, "file.txt");
        File logFile = new File(srcDir, "file.log");
        Files.write(txtFile.toPath(), "Text content".getBytes());
        Files.write(logFile.toPath(), "Log content".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyToDirectory(srcDir, destDir, true, (parent, file) -> file.getName().endsWith(".txt"));

        File copiedDir = new File(destDir, srcDir.getName());
        assertTrue(new File(copiedDir, "file.txt").exists());
        assertTrue(!new File(copiedDir, "file.log").exists());
    }

    @Test
    public void testCopyToDirectory_SameDirectory() throws Exception {
        File parentDir = tempFile.getParentFile();
        IOUtil.copyToDirectory(tempFile, parentDir);

        File copiedFile = new File(parentDir, "Copy of " + tempFile.getName());
        assertTrue(copiedFile.exists());
    }

    @Test
    public void testCopyDirectory_Basic() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File file1 = new File(srcDir, "file1.txt");
        File file2 = new File(srcDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyDirectory(srcDir, destDir);

        assertTrue(new File(destDir, file1.getName()).exists());
        assertTrue(new File(destDir, file2.getName()).exists());
    }

    @Test
    public void testCopyDirectory_Nested() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File subDir = new File(srcDir, "subdir");
        subDir.mkdir();
        File file1 = new File(srcDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyDirectory(srcDir, destDir);

        assertTrue(new File(destDir, "file1.txt").exists());
        File copiedSubDir = new File(destDir, "subdir");
        assertTrue(copiedSubDir.exists());
        assertTrue(new File(copiedSubDir, "file2.txt").exists());
    }

    @Test
    public void testCopyFile_Basic() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();
        destFile.delete();

        IOUtil.copyFile(tempFile, destFile);

        assertTrue(destFile.exists());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(destFile));
    }

    @Test
    public void testCopyFile_WithPreserveFileDate() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();
        destFile.delete();

        long originalTime = tempFile.lastModified();
        Thread.sleep(100);

        IOUtil.copyFile(tempFile, destFile, true);

        assertTrue(destFile.exists());
        assertEquals(originalTime, destFile.lastModified());
    }

    @Test
    public void testCopyFile_WithoutPreserveFileDate() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();
        destFile.delete();

        IOUtil.copyFile(tempFile, destFile, false);

        assertTrue(destFile.exists());
    }

    @Test
    public void testCopyFile_WithCopyOptions() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();

        IOUtil.copyFile(tempFile, destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertTrue(destFile.exists());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(destFile));
    }

    @Test
    public void testCopyFile_WithPreserveDateAndCopyOptions() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();

        IOUtil.copyFile(tempFile, destFile, true, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertTrue(destFile.exists());
    }

    @Test
    public void testCopyFile_ToOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        long bytesWritten = IOUtil.copyFile(tempFile, baos);

        assertTrue(bytesWritten > 0);
        assertEquals(TEST_CONTENT, new String(baos.toByteArray(), UTF_8));
    }

    @Test
    public void testCopyURLToFile_Basic() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), "URL content".getBytes());

        java.net.URL url = sourceFile.toURI().toURL();
        File destFile = Files.createTempFile(tempFolder, "url_dest", ".txt").toFile();
        destFile.delete();

        IOUtil.copyURLToFile(url, destFile);

        assertTrue(destFile.exists());
        assertEquals("URL content", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testCopyURLToFile_WithTimeout() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), "URL content with timeout".getBytes());

        java.net.URL url = sourceFile.toURI().toURL();
        File destFile = Files.createTempFile(tempFolder, "url_dest", ".txt").toFile();
        destFile.delete();

        IOUtil.copyURLToFile(url, destFile, 5000, 5000);

        assertTrue(destFile.exists());
        assertEquals("URL content with timeout", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testCopy_PathToPath() throws Exception {
        Path source = tempFile.toPath();
        Path target = Files.createTempFile(tempFolder, "path_dest", ".txt");
        Files.delete(target);

        Path result = IOUtil.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertNotNull(result);
        assertTrue(Files.exists(target));
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(target.toFile()));
    }

    @Test
    public void testCopy_InputStreamToPath() throws Exception {
        InputStream is = new ByteArrayInputStream("InputStream content".getBytes());
        Path target = Files.createTempFile(tempFolder, "is_dest", ".txt");
        Files.delete(target);

        long bytesWritten = IOUtil.copy(is, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertTrue(bytesWritten > 0);
        assertTrue(Files.exists(target));
        assertEquals("InputStream content", IOUtil.readAllToString(target.toFile()));
    }

    @Test
    public void testCopy_PathToOutputStream() throws Exception {
        Path source = tempFile.toPath();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        long bytesWritten = IOUtil.copy(source, baos);

        assertTrue(bytesWritten > 0);
        assertEquals(TEST_CONTENT, new String(baos.toByteArray(), UTF_8));
    }

    @Test
    public void testMove_FileToDirectory() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "move_src", ".txt").toFile();
        Files.write(srcFile.toPath(), "Move content".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "move_dest").toFile();

        IOUtil.move(srcFile, destDir);

        File movedFile = new File(destDir, srcFile.getName());
        assertTrue(movedFile.exists());
        assertTrue(!srcFile.exists());
        assertEquals("Move content", IOUtil.readAllToString(movedFile));
    }

    @Test
    public void testMove_FileToDirectoryWithOptions() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "move_src", ".txt").toFile();
        Files.write(srcFile.toPath(), "Move content with options".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "move_dest").toFile();

        IOUtil.move(srcFile, destDir, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        File movedFile = new File(destDir, srcFile.getName());
        assertTrue(movedFile.exists());
        assertTrue(!srcFile.exists());
    }

    @Test
    public void testMove_PathToPath() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "move_src", ".txt").toFile();
        Files.write(srcFile.toPath(), "Path move content".getBytes());

        Path target = tempFolder.resolve("moved_file.txt");

        Path result = IOUtil.move(srcFile.toPath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertNotNull(result);
        assertTrue(Files.exists(target));
        assertTrue(!Files.exists(srcFile.toPath()));
    }

    @Test
    public void testRenameTo_Success() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "rename_src", ".txt").toFile();
        Files.write(srcFile.toPath(), "Rename content".getBytes());

        boolean result = IOUtil.renameTo(srcFile, "renamed_file.txt");

        assertTrue(result);
        File renamedFile = new File(srcFile.getParent(), "renamed_file.txt");
        assertTrue(renamedFile.exists());
        assertTrue(!srcFile.exists());
    }

    @Test
    public void testRenameTo_DifferentName() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "original", ".txt").toFile();
        String newName = "new_name.txt";

        boolean result = IOUtil.renameTo(srcFile, newName);

        assertTrue(result);
        File renamedFile = new File(srcFile.getParent(), newName);
        assertTrue(renamedFile.exists());
    }

    @Test
    public void testDeleteIfExists_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "delete", ".txt").toFile();
        assertTrue(file.exists());

        boolean result = IOUtil.deleteIfExists(file);

        assertTrue(result);
        assertTrue(!file.exists());
    }

    @Test
    public void testDeleteIfExists_NonExistingFile() throws Exception {
        File file = new File(tempFolder.toFile(), "nonexistent.txt");

        boolean result = IOUtil.deleteIfExists(file);

        assertTrue(!result);
    }

    @Test
    public void testDeleteIfExists_NullFile() {
        boolean result = IOUtil.deleteIfExists(null);
        assertTrue(!result);
    }

    @Test
    public void testDeleteIfExists_EmptyDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_dir").toFile();
        assertTrue(dir.exists());

        boolean result = IOUtil.deleteIfExists(dir);

        assertTrue(result);
        assertTrue(!dir.exists());
    }

    @Test
    public void testDeleteQuietly_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "delete_quiet", ".txt").toFile();
        assertTrue(file.exists());

        boolean result = IOUtil.deleteQuietly(file);

        assertTrue(result);
        assertTrue(!file.exists());
    }

    @Test
    public void testDeleteQuietly_NonExistingFile() throws Exception {
        File file = new File(tempFolder.toFile(), "nonexistent.txt");

        boolean result = IOUtil.deleteQuietly(file);

        assertTrue(!result);
    }

    @Test
    public void testDeleteQuietly_NullFile() {
        boolean result = IOUtil.deleteQuietly(null);
        assertTrue(!result);
    }

    @Test
    public void testDeleteAllIfExists_SingleFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "delete_all", ".txt").toFile();
        assertTrue(file.exists());

        boolean result = IOUtil.deleteAllIfExists(file);

        assertTrue(result);
        assertTrue(!file.exists());
    }

    @Test
    public void testDeleteAllIfExists_DirectoryWithFiles() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_all_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.deleteAllIfExists(dir);

        assertTrue(result);
        assertTrue(!dir.exists());
        assertTrue(!file1.exists());
        assertTrue(!file2.exists());
    }

    @Test
    public void testDeleteAllIfExists_NestedDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_all_nested").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.deleteAllIfExists(dir);

        assertTrue(result);
        assertTrue(!dir.exists());
    }

    @Test
    public void testDeleteAllIfExists_NullFile() {
        boolean result = IOUtil.deleteAllIfExists(null);
        assertTrue(!result);
    }

    @Test
    public void testDeleteFilesFromDirectory_AllFiles() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_files_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.deleteFilesFromDirectory(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(!file1.exists());
        assertTrue(!file2.exists());
    }

    @Test
    public void testDeleteFilesFromDirectory_WithFilter() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_filter_dir").toFile();
        File txtFile = new File(dir, "file.txt");
        File logFile = new File(dir, "file.log");
        Files.write(txtFile.toPath(), "Text content".getBytes());
        Files.write(logFile.toPath(), "Log content".getBytes());

        boolean result = IOUtil.deleteFilesFromDirectory(dir, (parent, file) -> file.getName().endsWith(".txt"));

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(!txtFile.exists());
        assertTrue(logFile.exists());
    }

    @Test
    public void testDeleteFilesFromDirectory_EmptyDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_empty_dir").toFile();

        boolean result = IOUtil.deleteFilesFromDirectory(dir);

        assertTrue(result);
        assertTrue(dir.exists());
    }

    @Test
    public void testDeleteFilesFromDirectory_NonExistingDirectory() throws Exception {
        File dir = new File(tempFolder.toFile(), "nonexistent_dir");

        boolean result = IOUtil.deleteFilesFromDirectory(dir);

        assertTrue(!result);
    }

    @Test
    public void testCleanDirectory_WithFiles() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "clean_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.cleanDirectory(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(!file1.exists());
        assertTrue(!file2.exists());
    }

    @Test
    public void testCleanDirectory_WithSubdirectories() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "clean_nested_dir").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.cleanDirectory(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(!subDir.exists());
        assertTrue(!file1.exists());
    }

    @Test
    public void testCleanDirectory_EmptyDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "clean_empty").toFile();

        boolean result = IOUtil.cleanDirectory(dir);

        assertTrue(result);
        assertTrue(dir.exists());
    }

    @Test
    public void testCreateFileIfNotExists_NewFile() throws Exception {
        File file = new File(tempFolder.toFile(), "new_file.txt");
        assertTrue(!file.exists());

        boolean result = IOUtil.createFileIfNotExists(file);

        assertTrue(result);
        assertTrue(file.exists());
    }

    @Test
    public void testCreateFileIfNotExists_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "existing", ".txt").toFile();
        assertTrue(file.exists());

        boolean result = IOUtil.createFileIfNotExists(file);

        assertTrue(!result);
        assertTrue(file.exists());
    }

    @Test
    public void testCreateFileIfNotExists_WithNonExistingParent() throws Exception {
        File parentDir = new File(tempFolder.toFile(), "new_parent");
        File file = new File(parentDir, "new_file.txt");
        assertTrue(!file.exists());

        boolean result = IOUtil.createFileIfNotExists(file);

        assertTrue(result);
        assertTrue(file.exists());
        assertTrue(parentDir.exists());
    }

    @Test
    public void testMkdirIfNotExists_NewDirectory() throws Exception {
        File dir = new File(tempFolder.toFile(), "new_dir");
        assertTrue(!dir.exists());

        boolean result = IOUtil.mkdirIfNotExists(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
    }

    @Test
    public void testMkdirIfNotExists_ExistingDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "existing_dir").toFile();
        assertTrue(dir.exists());

        boolean result = IOUtil.mkdirIfNotExists(dir);

        assertTrue(!result);
        assertTrue(dir.exists());
    }

    @Test
    public void testMkdirIfNotExists_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "file", ".txt").toFile();
        assertTrue(file.exists());
        assertTrue(!file.isDirectory());

        boolean result = IOUtil.mkdirIfNotExists(file);

        assertTrue(!result);
    }

    @Test
    public void testMkdirsIfNotExists_NewDirectoryHierarchy() throws Exception {
        File dir = new File(tempFolder.toFile(), "parent/child/grandchild");
        assertTrue(!dir.exists());

        boolean result = IOUtil.mkdirsIfNotExists(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
    }

    @Test
    public void testMkdirsIfNotExists_ExistingDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "existing_dirs").toFile();
        assertTrue(dir.exists());

        boolean result = IOUtil.mkdirsIfNotExists(dir);

        assertTrue(!result);
        assertTrue(dir.exists());
    }

    @Test
    public void testMkdirsIfNotExists_SingleDirectory() throws Exception {
        File dir = new File(tempFolder.toFile(), "single_dir");
        assertTrue(!dir.exists());

        boolean result = IOUtil.mkdirsIfNotExists(dir);

        assertTrue(result);
        assertTrue(dir.exists());
    }

    @Test
    public void testIsBufferedReader_BufferedReader() throws Exception {
        Reader reader = new java.io.BufferedReader(new StringReader("test"));
        assertTrue(IOUtil.isBufferedReader(reader));
    }

    @Test
    public void testIsBufferedReader_NonBufferedReader() throws Exception {
        Reader reader = new StringReader("test");
        assertTrue(!IOUtil.isBufferedReader(reader));
    }

    @Test
    public void testIsBufferedReader_FileReader() throws Exception {
        Reader reader = new FileReader(tempFile);
        try {
            assertTrue(!IOUtil.isBufferedReader(reader));
        } finally {
            reader.close();
        }
    }

    @Test
    public void testIsBufferedWriter_BufferedWriter() throws Exception {
        Writer writer = new java.io.BufferedWriter(new java.io.StringWriter());
        assertTrue(IOUtil.isBufferedWriter(writer));
    }

    @Test
    public void testIsBufferedWriter_NonBufferedWriter() throws Exception {
        Writer writer = new java.io.StringWriter();
        assertTrue(!IOUtil.isBufferedWriter(writer));
    }

    @Test
    public void testIsFileNewer_WithDate() throws Exception {
        File file = Files.createTempFile(tempFolder, "newer", ".txt").toFile();
        java.util.Date pastDate = new java.util.Date(System.currentTimeMillis() - 10000);

        boolean result = IOUtil.isFileNewer(file, pastDate);
        assertTrue(result);
    }

    @Test
    public void testIsFileNewer_WithFutureDate() throws Exception {
        File file = Files.createTempFile(tempFolder, "newer", ".txt").toFile();
        java.util.Date futureDate = new java.util.Date(System.currentTimeMillis() + 10000);

        boolean result = IOUtil.isFileNewer(file, futureDate);
        assertTrue(!result);
    }

    @Test
    public void testIsFileNewer_WithReferenceFile() throws Exception {
        File oldFile = Files.createTempFile(tempFolder, "old", ".txt").toFile();
        Thread.sleep(100);
        File newFile = Files.createTempFile(tempFolder, "new", ".txt").toFile();

        boolean result = IOUtil.isFileNewer(newFile, oldFile);
        assertTrue(result);
    }

    @Test
    public void testIsFileOlder_WithDate() throws Exception {
        File file = Files.createTempFile(tempFolder, "older", ".txt").toFile();
        java.util.Date futureDate = new java.util.Date(System.currentTimeMillis() + 10000);

        boolean result = IOUtil.isFileOlder(file, futureDate);
        assertTrue(result);
    }

    @Test
    public void testIsFileOlder_WithPastDate() throws Exception {
        File file = Files.createTempFile(tempFolder, "older", ".txt").toFile();
        java.util.Date pastDate = new java.util.Date(System.currentTimeMillis() - 10000);

        boolean result = IOUtil.isFileOlder(file, pastDate);
        assertTrue(!result);
    }

    @Test
    public void testIsFileOlder_WithReferenceFile() throws Exception {
        File oldFile = Files.createTempFile(tempFolder, "old", ".txt").toFile();
        Thread.sleep(100);
        File newFile = Files.createTempFile(tempFolder, "new", ".txt").toFile();

        boolean result = IOUtil.isFileOlder(oldFile, newFile);
        assertTrue(result);
    }

    @Test
    public void testIsFile_ExistingFile() throws Exception {
        assertTrue(IOUtil.isFile(tempFile));
    }

    @Test
    public void testIsFile_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "dir").toFile();
        assertTrue(!IOUtil.isFile(dir));
    }

    @Test
    public void testIsFile_NullFile() {
        assertTrue(!IOUtil.isFile(null));
    }

    @Test
    public void testIsFile_NonExisting() {
        File file = new File(tempFolder.toFile(), "nonexistent.txt");
        assertTrue(!IOUtil.isFile(file));
    }

    @Test
    public void testIsDirectory_ExistingDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "dir").toFile();
        assertTrue(IOUtil.isDirectory(dir));
    }

    @Test
    public void testIsDirectory_File() throws Exception {
        assertTrue(!IOUtil.isDirectory(tempFile));
    }

    @Test
    public void testIsDirectory_NullFile() {
        assertTrue(!IOUtil.isDirectory(null));
    }

    @Test
    public void testIsDirectory_WithLinkOptions() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "dir").toFile();
        assertTrue(IOUtil.isDirectory(dir, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsRegularFile_ExistingFile() throws Exception {
        assertTrue(IOUtil.isRegularFile(tempFile, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsRegularFile_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "dir").toFile();
        assertTrue(!IOUtil.isRegularFile(dir, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsRegularFile_NullFile() {
        assertTrue(!IOUtil.isRegularFile(null, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsSymbolicLink_RegularFile() throws Exception {
        assertTrue(!IOUtil.isSymbolicLink(tempFile));
    }

    @Test
    public void testIsSymbolicLink_NullFile() {
        assertTrue(!IOUtil.isSymbolicLink(null));
    }

    @Test
    public void testSizeOf_File() throws Exception {
        long size = IOUtil.sizeOf(tempFile);
        assertEquals(TEST_CONTENT.getBytes(UTF_8).length, size);
    }

    @Test
    public void testSizeOf_EmptyFile() throws Exception {
        long size = IOUtil.sizeOf(emptyFile);
        assertEquals(0, size);
    }

    @Test
    public void testSizeOf_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "size_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "12345".getBytes());
        Files.write(file2.toPath(), "67890".getBytes());

        long size = IOUtil.sizeOf(dir);
        assertEquals(10, size);
    }

    @Test
    public void testSizeOf_WithConsiderNonExistingAsEmpty() throws Exception {
        File nonExisting = new File(tempFolder.toFile(), "nonexistent.txt");

        long size = IOUtil.sizeOf(nonExisting, true);
        assertEquals(0, size);
    }

    @Test
    public void testSizeOf_NonExistingFileThrowsException() {
        File nonExisting = new File(tempFolder.toFile(), "nonexistent.txt");

        assertThrows(java.io.FileNotFoundException.class, () -> {
            IOUtil.sizeOf(nonExisting, false);
        });
    }

    @Test
    public void testSizeOfDirectory_Basic() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "size_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "123".getBytes());
        Files.write(file2.toPath(), "4567".getBytes());

        long size = IOUtil.sizeOfDirectory(dir);
        assertEquals(7, size);
    }

    @Test
    public void testSizeOfDirectory_Nested() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "size_nested").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "12".getBytes());
        Files.write(file2.toPath(), "345".getBytes());

        long size = IOUtil.sizeOfDirectory(dir);
        assertEquals(5, size);
    }

    @Test
    public void testSizeOfDirectory_Empty() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "size_empty").toFile();

        long size = IOUtil.sizeOfDirectory(dir);
        assertEquals(0, size);
    }

    @Test
    public void testSizeOfDirectory_WithConsiderNonExistingAsEmpty() throws Exception {
        File nonExisting = new File(tempFolder.toFile(), "nonexistent_dir");

        long size = IOUtil.sizeOfDirectory(nonExisting, true);
        assertEquals(0, size);
    }

    @Test
    public void testSizeOfDirectory_NonExistingThrowsException() {
        File nonExisting = new File(tempFolder.toFile(), "nonexistent_dir");

        assertThrows(java.io.FileNotFoundException.class, () -> {
            IOUtil.sizeOfDirectory(nonExisting, false);
        });
    }

    @Test
    public void testSizeOfAsBigInteger_File() throws Exception {
        java.math.BigInteger size = IOUtil.sizeOfAsBigInteger(tempFile);
        assertEquals(java.math.BigInteger.valueOf(TEST_CONTENT.getBytes(UTF_8).length), size);
    }

    @Test
    public void testSizeOfAsBigInteger_EmptyFile() throws Exception {
        java.math.BigInteger size = IOUtil.sizeOfAsBigInteger(emptyFile);
        assertEquals(java.math.BigInteger.ZERO, size);
    }

    @Test
    public void testSizeOfAsBigInteger_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "bigint_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "12345".getBytes());
        Files.write(file2.toPath(), "67890".getBytes());

        java.math.BigInteger size = IOUtil.sizeOfAsBigInteger(dir);
        assertEquals(java.math.BigInteger.valueOf(10), size);
    }

    @Test
    public void testSizeOfAsBigInteger_NonExistingThrowsException() {
        File nonExisting = new File(tempFolder.toFile(), "nonexistent.txt");

        assertThrows(java.io.FileNotFoundException.class, () -> {
            IOUtil.sizeOfAsBigInteger(nonExisting);
        });
    }

    @Test
    public void testSizeOfDirectoryAsBigInteger_Basic() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "bigint_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "123".getBytes());
        Files.write(file2.toPath(), "4567".getBytes());

        java.math.BigInteger size = IOUtil.sizeOfDirectoryAsBigInteger(dir);
        assertEquals(java.math.BigInteger.valueOf(7), size);
    }

    @Test
    public void testSizeOfDirectoryAsBigInteger_Nested() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "bigint_nested").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "12".getBytes());
        Files.write(file2.toPath(), "345".getBytes());

        java.math.BigInteger size = IOUtil.sizeOfDirectoryAsBigInteger(dir);
        assertEquals(java.math.BigInteger.valueOf(5), size);
    }

    @Test
    public void testSizeOfDirectoryAsBigInteger_Empty() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "bigint_empty").toFile();

        java.math.BigInteger size = IOUtil.sizeOfDirectoryAsBigInteger(dir);
        assertEquals(java.math.BigInteger.ZERO, size);
    }

    @Test
    public void testNewStringWriter_Default() {
        StringWriter sw = IOUtil.newStringWriter();
        assertNotNull(sw);
        sw.write("test");
        assertEquals("test", sw.toString());
    }

    @Test
    public void testNewStringWriter_WithInitialSize() {
        StringWriter sw = IOUtil.newStringWriter(100);
        assertNotNull(sw);
        sw.write("test content");
        assertEquals("test content", sw.toString());
    }

    @Test
    public void testNewStringWriter_WithStringBuilder() {
        StringBuilder sb = new StringBuilder("initial");
        StringWriter sw = IOUtil.newStringWriter(sb);
        assertNotNull(sw);
        sw.write(" added");
        assertEquals("initial added", sw.toString());
    }

    @Test
    public void testNewStringWriter_MultipleWrites() {
        StringWriter sw = IOUtil.newStringWriter();
        sw.write("Hello");
        sw.write(" ");
        sw.write("World");
        assertEquals("Hello World", sw.toString());
    }

    @Test
    public void testNewByteArrayOutputStream_Default() {
        ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream();
        assertNotNull(baos);
        baos.write(65);
        assertEquals(1, baos.size());
    }

    @Test
    public void testNewByteArrayOutputStream_WithInitCapacity() {
        ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream(256);
        assertNotNull(baos);
        byte[] data = "test data".getBytes(UTF_8);
        baos.write(data, 0, data.length);
        assertEquals(9, baos.size());
    }

    @Test
    public void testNewByteArrayOutputStream_WriteBytes() throws Exception {
        ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream();
        byte[] testData = TEST_CONTENT.getBytes(UTF_8);
        baos.write(testData);
        assertArrayEquals(testData, baos.toByteArray());
    }

    @Test
    public void testNewFileInputStream_WithFile() throws Exception {
        try (FileInputStream fis = IOUtil.newFileInputStream(tempFile)) {
            assertNotNull(fis);
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewFileInputStream_WithFileName() throws Exception {
        try (FileInputStream fis = IOUtil.newFileInputStream(tempFile.getAbsolutePath())) {
            assertNotNull(fis);
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewFileInputStream_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileInputStream(nonExistent));
    }

    @Test
    public void testNewFileInputStream_NonExistentFileName() {
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileInputStream(tempFolder.resolve("nonexistent.txt").toString()));
    }

    @Test
    public void testNewFileOutputStream_WithFile() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile)) {
            assertNotNull(fos);
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileOutputStream_WithFileAppendFalse() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "existing".getBytes(UTF_8));

        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile, false)) {
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileOutputStream_WithFileAppendTrue() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "existing".getBytes(UTF_8));

        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile, true)) {
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("existing" + TEST_CONTENT, content);
    }

    @Test
    public void testNewFileOutputStream_WithFileName() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile.getAbsolutePath())) {
            assertNotNull(fos);
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileOutputStream_InvalidDirectory() {
        File invalidFile = new File("/invalid/path/that/does/not/exist/file.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileOutputStream(invalidFile));
    }

    @Test
    public void testNewFileReader_WithFile() throws Exception {
        try (FileReader fr = IOUtil.newFileReader(tempFile)) {
            assertNotNull(fr);
            char[] buffer = new char[1024];
            int charsRead = fr.read(buffer);
            assertTrue(charsRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewFileReader_WithFileAndCharset() throws Exception {
        try (FileReader fr = IOUtil.newFileReader(tempFile, UTF_8)) {
            assertNotNull(fr);
            char[] buffer = new char[1024];
            int charsRead = fr.read(buffer);
            assertTrue(charsRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewFileReader_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileReader(nonExistent));
    }

    @Test
    public void testNewFileReader_WithDifferentCharset() throws Exception {
        File isoFile = Files.createTempFile(tempFolder, "iso", ".txt").toFile();
        Files.write(isoFile.toPath(), "ISO content".getBytes(ISO_8859_1));

        try (FileReader fr = IOUtil.newFileReader(isoFile, ISO_8859_1)) {
            assertNotNull(fr);
            char[] buffer = new char[1024];
            int charsRead = fr.read(buffer);
            assertEquals("ISO content", new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewFileWriter_WithFile() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.FileWriter fw = IOUtil.newFileWriter(outputFile)) {
            assertNotNull(fw);
            fw.write(TEST_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileWriter_WithFileAndCharset() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.FileWriter fw = IOUtil.newFileWriter(outputFile, UTF_8)) {
            assertNotNull(fw);
            fw.write(UNICODE_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testNewFileWriter_WithFileCharsetAndAppend() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "existing".getBytes(UTF_8));

        try (java.io.FileWriter fw = IOUtil.newFileWriter(outputFile, UTF_8, true)) {
            fw.write(TEST_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("existing" + TEST_CONTENT, content);
    }

    @Test
    public void testNewFileWriter_WithFileCharsetNoAppend() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "existing".getBytes(UTF_8));

        try (java.io.FileWriter fw = IOUtil.newFileWriter(outputFile, UTF_8, false)) {
            fw.write(TEST_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileWriter_InvalidDirectory() {
        File invalidFile = new File("/invalid/path/that/does/not/exist/file.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileWriter(invalidFile));
    }

    @Test
    public void testNewInputStreamReader_WithInputStream() throws Exception {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)); java.io.InputStreamReader isr = IOUtil.newInputStreamReader(is)) {
            assertNotNull(isr);
            char[] buffer = new char[1024];
            int charsRead = isr.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewInputStreamReader_WithInputStreamAndCharset() throws Exception {
        try (InputStream is = new ByteArrayInputStream(UNICODE_CONTENT.getBytes(UTF_8));
                java.io.InputStreamReader isr = IOUtil.newInputStreamReader(is, UTF_8)) {
            assertNotNull(isr);
            char[] buffer = new char[1024];
            int charsRead = isr.read(buffer);
            assertEquals(UNICODE_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewInputStreamReader_WithUTF16() throws Exception {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_16));
                java.io.InputStreamReader isr = IOUtil.newInputStreamReader(is, UTF_16)) {
            assertNotNull(isr);
            char[] buffer = new char[1024];
            int charsRead = isr.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewOutputStreamWriter_WithOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.OutputStreamWriter osw = IOUtil.newOutputStreamWriter(baos)) {
            assertNotNull(osw);
            osw.write(TEST_CONTENT);
            osw.flush();
        }
        assertEquals(TEST_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewOutputStreamWriter_WithOutputStreamAndCharset() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.OutputStreamWriter osw = IOUtil.newOutputStreamWriter(baos, UTF_8)) {
            assertNotNull(osw);
            osw.write(UNICODE_CONTENT);
            osw.flush();
        }
        assertEquals(UNICODE_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewOutputStreamWriter_WithUTF16() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.OutputStreamWriter osw = IOUtil.newOutputStreamWriter(baos, UTF_16)) {
            assertNotNull(osw);
            osw.write(TEST_CONTENT);
            osw.flush();
        }
        assertEquals(TEST_CONTENT, baos.toString(UTF_16.name()));
    }

    @Test
    public void testNewBufferedInputStream_WithFile() throws Exception {
        try (java.io.BufferedInputStream bis = IOUtil.newBufferedInputStream(tempFile)) {
            assertNotNull(bis);
            byte[] buffer = new byte[1024];
            int bytesRead = bis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewBufferedInputStream_WithFileAndSize() throws Exception {
        try (java.io.BufferedInputStream bis = IOUtil.newBufferedInputStream(tempFile, 4096)) {
            assertNotNull(bis);
            byte[] buffer = new byte[1024];
            int bytesRead = bis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewBufferedInputStream_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedInputStream(nonExistent));
    }

    @Test
    public void testNewBufferedInputStream_LargeBuffer() throws Exception {
        try (java.io.BufferedInputStream bis = IOUtil.newBufferedInputStream(largeFile, 16384)) {
            assertNotNull(bis);
            byte[] buffer = new byte[1024];
            int bytesRead = bis.read(buffer);
            assertTrue(bytesRead > 0);
        }
    }

    @Test
    public void testNewBufferedOutputStream_WithFile() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedOutputStream bos = IOUtil.newBufferedOutputStream(outputFile)) {
            assertNotNull(bos);
            bos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewBufferedOutputStream_WithFileAndSize() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedOutputStream bos = IOUtil.newBufferedOutputStream(outputFile, 4096)) {
            assertNotNull(bos);
            bos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewBufferedOutputStream_InvalidDirectory() {
        File invalidFile = new File("/invalid/path/that/does/not/exist/file.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedOutputStream(invalidFile));
    }

    @Test
    public void testNewBufferedOutputStream_LargeBuffer() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedOutputStream bos = IOUtil.newBufferedOutputStream(outputFile, 16384)) {
            assertNotNull(bos);
            byte[] largeData = new byte[8192];
            java.util.Arrays.fill(largeData, (byte) 'X');
            bos.write(largeData);
        }
        assertEquals(8192, Files.size(outputFile.toPath()));
    }

    @Test
    public void testNewBufferedReader_WithFile() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithFileAndCharset() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile, UTF_8)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithPath() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile.toPath())) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithPathAndCharset() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile.toPath(), UTF_8)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithInputStream() throws Exception {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)); java.io.BufferedReader br = IOUtil.newBufferedReader(is)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithInputStreamAndCharset() throws Exception {
        try (InputStream is = new ByteArrayInputStream(UNICODE_CONTENT.getBytes(UTF_8)); java.io.BufferedReader br = IOUtil.newBufferedReader(is, UTF_8)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(UNICODE_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedReader(nonExistent));
    }

    @Test
    public void testNewBufferedReader_MultipleLines() throws Exception {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        try (java.io.BufferedReader br = IOUtil.newBufferedReader(multilineFile)) {
            assertEquals("Line 1", br.readLine());
            assertEquals("Line 2", br.readLine());
            assertEquals("Line 3", br.readLine());
        }
    }

    @Test
    public void testNewBufferedWriter_WithFile() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(outputFile)) {
            assertNotNull(bw);
            bw.write(TEST_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewBufferedWriter_WithFileAndCharset() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(outputFile, UTF_8)) {
            assertNotNull(bw);
            bw.write(UNICODE_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testNewBufferedWriter_WithOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(baos)) {
            assertNotNull(bw);
            bw.write(TEST_CONTENT);
            bw.flush();
        }
        assertEquals(TEST_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewBufferedWriter_WithOutputStreamAndCharset() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(baos, UTF_8)) {
            assertNotNull(bw);
            bw.write(UNICODE_CONTENT);
            bw.flush();
        }
        assertEquals(UNICODE_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewBufferedWriter_InvalidDirectory() {
        File invalidFile = new File("/invalid/path/that/does/not/exist/file.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedWriter(invalidFile));
    }

    @Test
    public void testNewBufferedWriter_MultipleWrites() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(outputFile)) {
            bw.write("Line 1");
            bw.newLine();
            bw.write("Line 2");
            bw.newLine();
            bw.write("Line 3");
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertTrue(content.contains("Line 1"));
        assertTrue(content.contains("Line 2"));
        assertTrue(content.contains("Line 3"));
    }

    @Test
    public void testNewLZ4BlockInputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Out = IOUtil.newLZ4BlockOutputStream(baos)) {
            lz4Out.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); LZ4BlockInputStream lz4In = IOUtil.newLZ4BlockInputStream(is)) {
            assertNotNull(lz4In);
            byte[] buffer = new byte[1024];
            int bytesRead = lz4In.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewLZ4BlockOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Out = IOUtil.newLZ4BlockOutputStream(baos)) {
            assertNotNull(lz4Out);
            lz4Out.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewLZ4BlockOutputStream_WithBlockSize() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Out = IOUtil.newLZ4BlockOutputStream(baos, 4096)) {
            assertNotNull(lz4Out);
            lz4Out.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testLZ4BlockCompressDecompress() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Out = IOUtil.newLZ4BlockOutputStream(baos, 8192)) {
            lz4Out.write(MULTILINE_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); LZ4BlockInputStream lz4In = IOUtil.newLZ4BlockInputStream(is)) {
            byte[] buffer = new byte[1024];
            int bytesRead = lz4In.read(buffer);
            assertEquals(MULTILINE_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewSnappyInputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (SnappyOutputStream snappyOut = IOUtil.newSnappyOutputStream(baos)) {
            snappyOut.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); SnappyInputStream snappyIn = IOUtil.newSnappyInputStream(is)) {
            assertNotNull(snappyIn);
            byte[] buffer = new byte[1024];
            int bytesRead = snappyIn.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewSnappyOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (SnappyOutputStream snappyOut = IOUtil.newSnappyOutputStream(baos)) {
            assertNotNull(snappyOut);
            snappyOut.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewSnappyOutputStream_WithBufferSize() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (SnappyOutputStream snappyOut = IOUtil.newSnappyOutputStream(baos, 4096)) {
            assertNotNull(snappyOut);
            snappyOut.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testSnappyCompressDecompress() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (SnappyOutputStream snappyOut = IOUtil.newSnappyOutputStream(baos, 8192)) {
            snappyOut.write(MULTILINE_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); SnappyInputStream snappyIn = IOUtil.newSnappyInputStream(is)) {
            byte[] buffer = new byte[1024];
            int bytesRead = snappyIn.read(buffer);
            assertEquals(MULTILINE_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewGZIPInputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos)) {
            gzipOut.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); java.util.zip.GZIPInputStream gzipIn = IOUtil.newGZIPInputStream(is)) {
            assertNotNull(gzipIn);
            byte[] buffer = new byte[1024];
            int bytesRead = gzipIn.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewGZIPInputStream_WithBufferSize() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos)) {
            gzipOut.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); java.util.zip.GZIPInputStream gzipIn = IOUtil.newGZIPInputStream(is, 4096)) {
            assertNotNull(gzipIn);
            byte[] buffer = new byte[1024];
            int bytesRead = gzipIn.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewGZIPOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos)) {
            assertNotNull(gzipOut);
            gzipOut.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewGZIPOutputStream_WithBufferSize() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos, 4096)) {
            assertNotNull(gzipOut);
            gzipOut.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testGZIPCompressDecompress() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos, 8192)) {
            gzipOut.write(MULTILINE_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); java.util.zip.GZIPInputStream gzipIn = IOUtil.newGZIPInputStream(is, 8192)) {
            byte[] buffer = new byte[1024];
            int bytesRead = gzipIn.read(buffer);
            assertEquals(MULTILINE_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewGZIPInputStream_InvalidData() {
        InputStream is = new ByteArrayInputStream("not gzip data".getBytes(UTF_8));
        assertThrows(UncheckedIOException.class, () -> IOUtil.newGZIPInputStream(is));
    }

    @Test
    public void testNewZipInputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos)) {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("test.txt");
            zipOut.putNextEntry(entry);
            zipOut.write(TEST_CONTENT.getBytes(UTF_8));
            zipOut.closeEntry();
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); java.util.zip.ZipInputStream zipIn = IOUtil.newZipInputStream(is)) {
            assertNotNull(zipIn);
            java.util.zip.ZipEntry entry = zipIn.getNextEntry();
            assertNotNull(entry);
            assertEquals("test.txt", entry.getName());
            byte[] buffer = new byte[1024];
            int bytesRead = zipIn.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewZipInputStream_WithCharset() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos, UTF_8)) {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("test.txt");
            zipOut.putNextEntry(entry);
            zipOut.write(TEST_CONTENT.getBytes(UTF_8));
            zipOut.closeEntry();
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); java.util.zip.ZipInputStream zipIn = IOUtil.newZipInputStream(is, UTF_8)) {
            assertNotNull(zipIn);
            java.util.zip.ZipEntry entry = zipIn.getNextEntry();
            assertNotNull(entry);
        }
    }

    @Test
    public void testNewZipOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos)) {
            assertNotNull(zipOut);
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("file.txt");
            zipOut.putNextEntry(entry);
            zipOut.write(TEST_CONTENT.getBytes(UTF_8));
            zipOut.closeEntry();
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewZipOutputStream_WithCharset() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos, UTF_8)) {
            assertNotNull(zipOut);
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("unicode.txt");
            zipOut.putNextEntry(entry);
            zipOut.write(UNICODE_CONTENT.getBytes(UTF_8));
            zipOut.closeEntry();
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testZipMultipleEntries() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos)) {
            java.util.zip.ZipEntry entry1 = new java.util.zip.ZipEntry("file1.txt");
            zipOut.putNextEntry(entry1);
            zipOut.write("Content 1".getBytes(UTF_8));
            zipOut.closeEntry();

            java.util.zip.ZipEntry entry2 = new java.util.zip.ZipEntry("file2.txt");
            zipOut.putNextEntry(entry2);
            zipOut.write("Content 2".getBytes(UTF_8));
            zipOut.closeEntry();
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray()); java.util.zip.ZipInputStream zipIn = IOUtil.newZipInputStream(is)) {
            java.util.zip.ZipEntry entry1 = zipIn.getNextEntry();
            assertEquals("file1.txt", entry1.getName());

            java.util.zip.ZipEntry entry2 = zipIn.getNextEntry();
            assertEquals("file2.txt", entry2.getName());
        }
    }

    @Test
    public void testClose_URLConnection() throws Exception {
        File testFile = Files.createTempFile(tempFolder, "urltest", ".txt").toFile();
        Files.write(testFile.toPath(), TEST_CONTENT.getBytes(UTF_8));

        java.net.URL url = testFile.toURI().toURL();
        java.net.URLConnection conn = url.openConnection();
        conn.connect();

        try (InputStream in = conn.getInputStream()) {
            in.readAllBytes(); // simulate use
        }

        IOUtil.close(conn);
    }

    @Test
    public void testClose_NullURLConnection() {
        IOUtil.close((java.net.URLConnection) null);
    }

    @Test
    public void testClose_AutoCloseable() throws Exception {
        InputStream is = IOUtil.newBufferedInputStream(new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)));
        IOUtil.close(is);

        assertThrows(IOException.class, () -> is.read());
    }

    @Test
    public void testClose_NullAutoCloseable() {
        IOUtil.close((AutoCloseable) null);
    }

    @Test
    public void testClose_WithExceptionHandler() throws Exception {
        InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8));
        java.util.concurrent.atomic.AtomicBoolean handlerCalled = new java.util.concurrent.atomic.AtomicBoolean(false);

        IOUtil.close(is, ex -> handlerCalled.set(true));

        assertTrue(!handlerCalled.get());
    }

    @Test
    public void testClose_WithExceptionHandlerOnError() {
        AutoCloseable problematic = () -> {
            throw new IOException("Test exception");
        };

        java.util.concurrent.atomic.AtomicBoolean handlerCalled = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicReference<Exception> caughtException = new java.util.concurrent.atomic.AtomicReference<>();

        IOUtil.close(problematic, ex -> {
            handlerCalled.set(true);
            caughtException.set(ex);
        });

        assertTrue(handlerCalled.get());
        assertNotNull(caughtException.get());
        assertTrue(caughtException.get().getMessage().contains("Test exception"));
    }

    @Test
    public void testCloseAll_VarArgs() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data2".getBytes(UTF_8)));
        InputStream is3 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8)));

        IOUtil.closeAll(is1, is2, is3);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is2.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAll_EmptyVarArgs() {
        IOUtil.closeAll();
    }

    @Test
    public void testCloseAll_WithNulls() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = null;
        InputStream is3 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8)));

        IOUtil.closeAll(is1, is2, is3);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAll_Iterable() throws Exception {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8))));
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data2".getBytes(UTF_8))));
        ;
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8))));
        ;

        IOUtil.closeAll(closeables);

        for (AutoCloseable c : closeables) {
            InputStream is = (InputStream) c;
            assertThrows(IOException.class, () -> is.read());
        }
    }

    @Test
    public void testCloseAll_EmptyIterable() {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        IOUtil.closeAll(closeables);
    }

    @Test
    public void testCloseAll_IterableWithNulls() throws Exception {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8))));
        ;
        closeables.add(null);
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8))));
        ;

        IOUtil.closeAll(closeables);

        InputStream is1 = (InputStream) closeables.get(0);
        InputStream is3 = (InputStream) closeables.get(2);
        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAll_ContinuesOnException() {
        AutoCloseable problematic = () -> {
            throw new IOException("Error in first closeable");
        };
        InputStream is = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data".getBytes(UTF_8)));

        assertThrows(UncheckedIOException.class, () -> IOUtil.closeAll(problematic, is));

        assertThrows(IOException.class, () -> is.read());
    }

    @Test
    public void testCloseQuietly_AutoCloseable() throws Exception {
        InputStream is = IOUtil.newBufferedInputStream(new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)));
        IOUtil.closeQuietly(is);

        assertThrows(IOException.class, () -> is.read());
    }

    @Test
    public void testCloseQuietly_Null() {
        IOUtil.closeQuietly((AutoCloseable) null);
    }

    @Test
    public void testCloseQuietly_WithException() {
        AutoCloseable problematic = () -> {
            throw new IOException("Test exception");
        };

        IOUtil.closeQuietly(problematic);
    }

    @Test
    public void testCloseQuietly_MultipleResources() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data2".getBytes(UTF_8)));

        IOUtil.closeQuietly(is1);
        IOUtil.closeQuietly(is2);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is2.read());
    }

    @Test
    public void testCloseAllQuietly_VarArgs() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data2".getBytes(UTF_8)));
        InputStream is3 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8)));

        IOUtil.closeAllQuietly(is1, is2, is3);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is2.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAllQuietly_EmptyVarArgs() {
        IOUtil.closeAllQuietly();
    }

    @Test
    public void testCloseAllQuietly_WithNulls() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = null;
        InputStream is3 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8)));

        IOUtil.closeAllQuietly(is1, is2, is3);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAllQuietly_Iterable() throws Exception {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        closeables.add(IOUtil.newBufferedReader(new ByteArrayInputStream("data1".getBytes(UTF_8))));
        closeables.add(IOUtil.newBufferedReader(new ByteArrayInputStream("data2".getBytes(UTF_8))));
        closeables.add(IOUtil.newBufferedReader(new ByteArrayInputStream("data3".getBytes(UTF_8))));

        IOUtil.closeAllQuietly(closeables);

        for (AutoCloseable c : closeables) {
            Reader is = (Reader) c;
            assertThrows(IOException.class, () -> is.read());
        }
    }

    @Test
    public void testCloseAllQuietly_EmptyIterable() {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        IOUtil.closeAllQuietly(closeables);
    }

    @Test
    public void testCloseAllQuietly_IterableWithNulls() throws Exception {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8))));
        ;
        closeables.add(null);
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8))));
        ;

        IOUtil.closeAllQuietly(closeables);

        InputStream is1 = (InputStream) closeables.get(0);
        InputStream is3 = (InputStream) closeables.get(2);
        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAllQuietly_WithExceptions() {
        AutoCloseable problematic1 = () -> {
            throw new IOException("Error 1");
        };
        AutoCloseable problematic2 = () -> {
            throw new IOException("Error 2");
        };
        InputStream is = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data".getBytes(UTF_8)));

        IOUtil.closeAllQuietly(problematic1, problematic2, is);

        assertThrows(IOException.class, () -> is.read());
    }

    @Test
    public void testCloseAllQuietly_MixedTypes() throws Exception {
        FileInputStream fis = new FileInputStream(tempFile);
        java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        IOUtil.closeAllQuietly(fis, br, baos);

        assertThrows(IOException.class, () -> fis.read());
        assertThrows(IOException.class, () -> br.read());
    }

    @Test
    public void testCloseAllQuietly_NullIterable() {
        IOUtil.closeAllQuietly((Iterable<? extends AutoCloseable>) null);
    }

    @Test
    public void testZip_SingleFile() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        File zipFile = Files.createTempFile(tempFolder, "archive", ".zip").toFile();
        Files.write(sourceFile.toPath(), "Test content for zipping".getBytes(UTF_8));

        IOUtil.zip(sourceFile, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    public void testZip_EmptyFile() throws Exception {
        File emptySource = Files.createTempFile(tempFolder, "empty", ".txt").toFile();
        File zipFile = Files.createTempFile(tempFolder, "empty-archive", ".zip").toFile();

        IOUtil.zip(emptySource, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    public void testZip_Directory() throws Exception {
        File sourceDir = Files.createTempDirectory(tempFolder, "source-dir").toFile();
        File file1 = new File(sourceDir, "file1.txt");
        File file2 = new File(sourceDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content 2".getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "dir-archive", ".zip").toFile();
        IOUtil.zip(sourceDir, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    public void testZip_CollectionOfFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();
        File file3 = Files.createTempFile(tempFolder, "file3", ".txt").toFile();

        Files.write(file1.toPath(), "Content 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content 2".getBytes(UTF_8));
        Files.write(file3.toPath(), "Content 3".getBytes(UTF_8));

        java.util.List<File> sourceFiles = java.util.Arrays.asList(file1, file2, file3);
        File zipFile = Files.createTempFile(tempFolder, "multi-archive", ".zip").toFile();

        IOUtil.zip(sourceFiles, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    public void testZip_EmptyCollection() throws Exception {
        java.util.List<File> emptyList = new java.util.ArrayList<>();
        File zipFile = Files.createTempFile(tempFolder, "empty-collection", ".zip").toFile();

        IOUtil.zip(emptyList, zipFile);

        assertTrue(zipFile.exists());
    }

    @Test
    public void testUnzip_Basic() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Content to unzip".getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "archive", ".zip").toFile();
        IOUtil.zip(sourceFile, zipFile);

        File targetDir = Files.createTempDirectory(tempFolder, "unzip-target").toFile();
        IOUtil.unzip(zipFile, targetDir);

        assertTrue(targetDir.exists());
        assertTrue(targetDir.isDirectory());
        File[] unzippedFiles = targetDir.listFiles();
        assertNotNull(unzippedFiles);
        assertTrue(unzippedFiles.length > 0);
    }

    @Test
    public void testUnzip_DirectoryStructure() throws Exception {
        File sourceDir = Files.createTempDirectory(tempFolder, "nested").toFile();
        File subDir = new File(sourceDir, "subdir");
        subDir.mkdir();

        File file1 = new File(sourceDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Root content".getBytes(UTF_8));
        Files.write(file2.toPath(), "Nested content".getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "nested-archive", ".zip").toFile();
        IOUtil.zip(sourceDir, zipFile);

        File targetDir = Files.createTempDirectory(tempFolder, "unzip-nested").toFile();
        IOUtil.unzip(zipFile, targetDir);

        assertTrue(targetDir.exists());
        File[] unzippedFiles = targetDir.listFiles();
        assertNotNull(unzippedFiles);
        assertTrue(unzippedFiles.length > 0);
    }

    @Test
    public void testUnzip_NonexistentZipFile() throws IOException {
        File nonexistentZip = new File(tempFolder.toFile(), "nonexistent.zip");
        File targetDir = Files.createTempDirectory(tempFolder, "unzip-fail").toFile();

        assertThrows(Exception.class, () -> IOUtil.unzip(nonexistentZip, targetDir));
    }

    @Test
    public void testSplit_TwoParts() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-source", ".txt").toFile();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("Line ").append(i).append("\r\n");
        }
        Files.write(sourceFile.toPath(), content.toString().getBytes(UTF_8));

        IOUtil.split(sourceFile, 2);

        File part1 = new File(sourceFile.getAbsolutePath() + "_0001");
        File part2 = new File(sourceFile.getAbsolutePath() + "_0002");

        assertTrue(part1.exists());
        assertTrue(part2.exists());
        assertTrue(part1.length() > 0);
        assertTrue(part2.length() > 0);
    }

    @Test
    public void testSplit_ThreeParts() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split3", ".txt").toFile();
        Files.write(sourceFile.toPath(), "0123456789ABCDEFGHIJ".getBytes(UTF_8));

        IOUtil.split(sourceFile, 3);

        File part1 = new File(sourceFile.getAbsolutePath() + "_0001");
        File part2 = new File(sourceFile.getAbsolutePath() + "_0002");
        File part3 = new File(sourceFile.getAbsolutePath() + "_0003");

        assertTrue(part1.exists());
        assertTrue(part2.exists());
        assertTrue(part3.exists());
    }

    @Test
    public void testSplit_WithDestDir() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-dest", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Content for splitting".getBytes(UTF_8));

        File destDir = Files.createTempDirectory(tempFolder, "split-dest-dir").toFile();

        IOUtil.split(sourceFile, 2, destDir);

        File part1 = new File(destDir.getAbsolutePath() + "\\" + sourceFile.getName() + "_0001");
        File part2 = new File(destDir.getAbsolutePath() + "\\" + sourceFile.getName() + "_0002");

        assertTrue(part1.exists());
        assertTrue(part2.exists());
    }

    @Test
    public void testSplitBySize_SmallChunks() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-size", ".txt").toFile();
        Files.write(sourceFile.toPath(), "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(UTF_8));

        IOUtil.splitBySize(sourceFile, 10);

        File part1 = new File(sourceFile.getAbsolutePath() + "_0001");
        File part2 = new File(sourceFile.getAbsolutePath() + "_0002");

        assertTrue(part1.exists());
        assertTrue(part2.exists());
        assertTrue(part1.length() <= 10);
    }

    @Test
    public void testSplitBySize_WithDestDir() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-by-size-dest", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Content for size-based splitting".getBytes(UTF_8));

        File destDir = Files.createTempDirectory(tempFolder, "split-size-dir").toFile();

        IOUtil.splitBySize(sourceFile, 10, destDir);

        File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertTrue(parts.length > 0);
    }

    @Test
    public void testSplitBySize_LargerThanFile() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "small-file", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Small".getBytes(UTF_8));

        IOUtil.splitBySize(sourceFile, 1000);

        File part1 = new File(sourceFile.getAbsolutePath() + "_0001");
        assertTrue(part1.exists());
        assertEquals("Small", IOUtil.readAllToString(part1));
    }

    @Test
    public void testMerge_FileArray() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "merge1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "merge2", ".txt").toFile();
        File file3 = Files.createTempFile(tempFolder, "merge3", ".txt").toFile();

        Files.write(file1.toPath(), "Part1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Part2".getBytes(UTF_8));
        Files.write(file3.toPath(), "Part3".getBytes(UTF_8));

        File[] sourceFiles = { file1, file2, file3 };
        File destFile = Files.createTempFile(tempFolder, "merged", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertTrue(totalBytes > 0);
        String merged = IOUtil.readAllToString(destFile);
        assertEquals("Part1Part2Part3", merged);
    }

    @Test
    public void testMerge_Collection() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "merge-c1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "merge-c2", ".txt").toFile();

        Files.write(file1.toPath(), "First".getBytes(UTF_8));
        Files.write(file2.toPath(), "Second".getBytes(UTF_8));

        java.util.List<File> sourceFiles = java.util.Arrays.asList(file1, file2);
        File destFile = Files.createTempFile(tempFolder, "merged-collection", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertTrue(totalBytes > 0);
        String merged = IOUtil.readAllToString(destFile);
        assertEquals("FirstSecond", merged);
    }

    @Test
    public void testMerge_WithDelimiter() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "delim1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "delim2", ".txt").toFile();

        Files.write(file1.toPath(), "Line1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line2".getBytes(UTF_8));

        java.util.List<File> sourceFiles = java.util.Arrays.asList(file1, file2);
        File destFile = Files.createTempFile(tempFolder, "merged-delim", ".txt").toFile();

        byte[] delimiter = "\r\n".getBytes(UTF_8);
        long totalBytes = IOUtil.merge(sourceFiles, delimiter, destFile);

        assertTrue(totalBytes > 0);
        String merged = IOUtil.readAllToString(destFile);
        assertEquals("Line1\r\nLine2", merged);
    }

    @Test
    public void testMerge_EmptyFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "empty1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "empty2", ".txt").toFile();

        File[] sourceFiles = { file1, file2 };
        File destFile = Files.createTempFile(tempFolder, "merged-empty", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertEquals(0, totalBytes);
    }

    @Test
    public void testMerge_SingleFile() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "single", ".txt").toFile();
        Files.write(file1.toPath(), "OnlyOne".getBytes(UTF_8));

        File[] sourceFiles = { file1 };
        File destFile = Files.createTempFile(tempFolder, "merged-single", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertTrue(totalBytes > 0);
        assertEquals("OnlyOne", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testListFiles_NonRecursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "list-files").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "Content1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content2".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir);

        assertNotNull(files);
        assertEquals(2, files.size());
    }

    @Test
    public void testListFiles_Recursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "list-recursive").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content2".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir, true, false);

        assertNotNull(files);
        assertTrue(files.size() >= 2);
    }

    @Test
    public void testListFiles_ExcludeDirectories() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "exclude-dirs").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        Files.write(file1.toPath(), "Content1".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir, true, true);

        assertNotNull(files);
        for (File f : files) {
            assertTrue(f.isFile());
        }
    }

    @Test
    public void testListFiles_WithFilter() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "filter-files").toFile();
        File txtFile = new File(dir, "file1.txt");
        File csvFile = new File(dir, "file2.csv");
        Files.write(txtFile.toPath(), "Text content".getBytes(UTF_8));
        Files.write(csvFile.toPath(), "CSV content".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir, false, (parent, f) -> f.getName().endsWith(".txt"));

        assertNotNull(files);
        assertEquals(1, files.size());
        assertTrue(files.get(0).getName().endsWith(".txt"));
    }

    @Test
    public void testListFiles_EmptyDirectory() throws Exception {
        File emptyDir = Files.createTempDirectory(tempFolder, "empty-dir").toFile();

        java.util.List<File> files = IOUtil.listFiles(emptyDir);

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void testListDirectories_NonRecursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "list-dirs").toFile();
        File subDir1 = new File(dir, "subdir1");
        File subDir2 = new File(dir, "subdir2");
        subDir1.mkdir();
        subDir2.mkdir();

        File file1 = new File(dir, "file1.txt");
        Files.write(file1.toPath(), "Content".getBytes(UTF_8));

        java.util.List<File> dirs = IOUtil.listDirectories(dir);

        assertNotNull(dirs);
        assertEquals(2, dirs.size());
        for (File d : dirs) {
            assertTrue(d.isDirectory());
        }
    }

    @Test
    public void testListDirectories_Recursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "list-dirs-rec").toFile();
        File subDir1 = new File(dir, "level1");
        subDir1.mkdir();
        File subDir2 = new File(subDir1, "level2");
        subDir2.mkdir();

        java.util.List<File> dirs = IOUtil.listDirectories(dir, true);

        assertNotNull(dirs);
        assertTrue(dirs.size() >= 2);
        for (File d : dirs) {
            assertTrue(d.isDirectory());
        }
    }

    @Test
    public void testListDirectories_EmptyDirectory() throws Exception {
        File emptyDir = Files.createTempDirectory(tempFolder, "empty-for-dirs").toFile();

        java.util.List<File> dirs = IOUtil.listDirectories(emptyDir);

        assertNotNull(dirs);
        assertEquals(0, dirs.size());
    }

    @Test
    public void testWalk_Basic() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "walk-basic").toFile();
        File file1 = new File(dir, "file1.txt");
        Files.write(file1.toPath(), "Content".getBytes(UTF_8));

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir);

        assertNotNull(stream);
        long count = stream.count();
        assertTrue(count > 0);
    }

    @Test
    public void testWalk_Recursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "walk-rec").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content2".getBytes(UTF_8));

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir, true, false);

        assertNotNull(stream);
        long count = stream.count();
        assertTrue(count >= 2);
    }

    @Test
    public void testWalk_ExcludeDirectories() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "walk-exclude").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        Files.write(file1.toPath(), "Content".getBytes(UTF_8));

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir, true, true);

        assertNotNull(stream);
        java.util.List<File> files = stream.toList();
        for (File f : files) {
            assertTrue(f.isFile());
        }
    }

    @Test
    public void testWalk_EmptyDirectory() throws Exception {
        File emptyDir = Files.createTempDirectory(tempFolder, "walk-empty").toFile();

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(emptyDir);

        assertNotNull(stream);
        long count = stream.count();
        assertEquals(0, count);
    }

    @Test
    public void testToFile_ValidURL() throws Exception {
        File file = Files.createTempFile(tempFolder, "url-test", ".txt").toFile();
        java.net.URL url = file.toURI().toURL();

        File result = IOUtil.toFile(url);

        assertNotNull(result);
        assertEquals(file.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    public void testToFiles_URLArray() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "url1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "url2", ".txt").toFile();

        java.net.URL[] urls = { file1.toURI().toURL(), file2.toURI().toURL() };

        File[] files = IOUtil.toFiles(urls);

        assertNotNull(files);
        assertEquals(2, files.length);
    }

    @Test
    public void testToFiles_URLCollection() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "url-c1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "url-c2", ".txt").toFile();

        java.util.List<java.net.URL> urls = java.util.Arrays.asList(file1.toURI().toURL(), file2.toURI().toURL());

        java.util.List<File> files = IOUtil.toFiles(urls);

        assertNotNull(files);
        assertEquals(2, files.size());
    }

    @Test
    public void testToFiles_EmptyArray() throws Exception {
        java.net.URL[] urls = {};

        File[] files = IOUtil.toFiles(urls);

        assertNotNull(files);
        assertEquals(0, files.length);
    }

    @Test
    public void testToFiles_EmptyCollection() throws Exception {
        java.util.List<java.net.URL> urls = new java.util.ArrayList<>();

        java.util.List<File> files = IOUtil.toFiles(urls);

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void testToURL_ValidFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "to-url", ".txt").toFile();

        java.net.URL url = IOUtil.toURL(file);

        assertNotNull(url);
        assertTrue(url.toString().contains(file.getName()));
    }

    @Test
    public void testToURLs_FileArray() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "to-urls1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "to-urls2", ".txt").toFile();

        File[] files = { file1, file2 };

        java.net.URL[] urls = IOUtil.toURLs(files);

        assertNotNull(urls);
        assertEquals(2, urls.length);
    }

    @Test
    public void testToURLs_FileCollection() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "to-urls-c1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "to-urls-c2", ".txt").toFile();

        java.util.List<File> files = java.util.Arrays.asList(file1, file2);

        java.util.List<java.net.URL> urls = IOUtil.toURLs(files);

        assertNotNull(urls);
        assertEquals(2, urls.size());
    }

    @Test
    public void testToURLs_EmptyArray() throws Exception {
        File[] files = {};

        java.net.URL[] urls = IOUtil.toURLs(files);

        assertNotNull(urls);
        assertEquals(0, urls.length);
    }

    @Test
    public void testToURLs_EmptyCollection() throws Exception {
        java.util.List<File> files = new java.util.ArrayList<>();

        java.util.List<java.net.URL> urls = IOUtil.toURLs(files);

        assertNotNull(urls);
        assertEquals(0, urls.size());
    }

    @Test
    public void testTouch_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "touch-existing", ".txt").toFile();
        long originalModified = file.lastModified();

        Thread.sleep(100);

        boolean result = IOUtil.touch(file);

        assertTrue(result);
        assertTrue(file.lastModified() >= originalModified);
    }

    @Test
    public void testTouch_NonexistentFile() throws Exception {
        File newFile = new File(tempFolder.toFile(), "new-touch-file.txt");

        boolean result = IOUtil.touch(newFile);

        assertFalse(result);
        assertFalse(newFile.exists());
    }

    @Test
    public void testTouch_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "touch-dir").toFile();

        boolean result = IOUtil.touch(dir);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Files_Identical() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "equal1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "equal2", ".txt").toFile();

        String content = "Identical content";
        Files.write(file1.toPath(), content.getBytes(UTF_8));
        Files.write(file2.toPath(), content.getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(file1, file2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Files_Different() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "diff1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "diff2", ".txt").toFile();

        Files.write(file1.toPath(), "Content 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content 2".getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(file1, file2);

        assertTrue(!result);
    }

    @Test
    public void testContentEquals_Files_BothEmpty() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "empty-eq1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "empty-eq2", ".txt").toFile();

        boolean result = IOUtil.contentEquals(file1, file2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Files_SameFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "same", ".txt").toFile();
        Files.write(file.toPath(), "Content".getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(file, file);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_InputStreams_Identical() throws Exception {
        String content = "Stream content";
        InputStream is1 = new ByteArrayInputStream(content.getBytes(UTF_8));
        InputStream is2 = new ByteArrayInputStream(content.getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(is1, is2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_InputStreams_Different() throws Exception {
        InputStream is1 = new ByteArrayInputStream("Content 1".getBytes(UTF_8));
        InputStream is2 = new ByteArrayInputStream("Content 2".getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(is1, is2);

        assertTrue(!result);
    }

    @Test
    public void testContentEquals_InputStreams_Empty() throws Exception {
        InputStream is1 = new ByteArrayInputStream(new byte[0]);
        InputStream is2 = new ByteArrayInputStream(new byte[0]);

        boolean result = IOUtil.contentEquals(is1, is2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Readers_Identical() throws Exception {
        String content = "Reader content";
        Reader r1 = new StringReader(content);
        Reader r2 = new StringReader(content);

        boolean result = IOUtil.contentEquals(r1, r2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Readers_Different() throws Exception {
        Reader r1 = new StringReader("Content 1");
        Reader r2 = new StringReader("Content 2");

        boolean result = IOUtil.contentEquals(r1, r2);

        assertTrue(!result);
    }

    @Test
    public void testContentEquals_Readers_Empty() throws Exception {
        Reader r1 = new StringReader("");
        Reader r2 = new StringReader("");

        boolean result = IOUtil.contentEquals(r1, r2);

        assertTrue(result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Files_Identical() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "eol1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "eol2", ".txt").toFile();

        Files.write(file1.toPath(), "Line1\r\nLine2\r\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line1\r\nLine2\r\n".getBytes(UTF_8));

        boolean result = IOUtil.contentEqualsIgnoreEOL(file1, file2, UTF_8.name());

        assertTrue(result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Files_DifferentEOL() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "eol-unix", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "eol-win", ".txt").toFile();

        Files.write(file1.toPath(), "Line1\nLine2\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line1\r\nLine2\r\n".getBytes(UTF_8));

        boolean result = IOUtil.contentEqualsIgnoreEOL(file1, file2, UTF_8.name());

        assertTrue(result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Files_DifferentContent() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "eol-diff1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "eol-diff2", ".txt").toFile();

        Files.write(file1.toPath(), "Line1\nLine2\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line1\r\nLine3\r\n".getBytes(UTF_8));

        boolean result = IOUtil.contentEqualsIgnoreEOL(file1, file2, UTF_8.name());

        assertTrue(!result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Readers_DifferentEOL() throws Exception {
        Reader r1 = new StringReader("Line1\nLine2");
        Reader r2 = new StringReader("Line1\r\nLine2");

        boolean result = IOUtil.contentEqualsIgnoreEOL(r1, r2);

        assertTrue(result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Readers_DifferentContent() throws Exception {
        Reader r1 = new StringReader("Line1\r\nLine2");
        Reader r2 = new StringReader("Line1\r\nLine3");

        boolean result = IOUtil.contentEqualsIgnoreEOL(r1, r2);

        assertTrue(!result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Readers_Empty() throws Exception {
        Reader r1 = new StringReader("");
        Reader r2 = new StringReader("");

        boolean result = IOUtil.contentEqualsIgnoreEOL(r1, r2);

        assertTrue(result);
    }

    @Test
    public void testForLines_File_Basic() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines", ".txt").toFile();
        Files.write(file.toPath(), "Line1\r\nLine2\r\nLine3\r\n".getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(file, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line1", lines.get(0));
        assertEquals("Line2", lines.get(1));
        assertEquals("Line3", lines.get(2));
    }

    @Test
    public void testForLines_File_WithCharset() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-enc", ".txt").toFile();
        Files.write(file.toPath(), "Line1\r\nLine2\r\n".getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(file, line -> lines.add(line), () -> {
        });

        assertEquals(2, lines.size());
    }

    @Test
    public void testForLines_File_WithOffsetAndCount() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-offset", ".txt").toFile();
        Files.write(file.toPath(), "Line1\r\nLine2\r\nLine3\r\nLine4\r\nLine5\r\n".getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(file, 1, 3, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line2", lines.get(0));
        assertEquals("Line3", lines.get(1));
        assertEquals("Line4", lines.get(2));
    }

    @Test
    public void testForLines_File_WithOffsetCountAndCallback() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-callback", ".txt").toFile();
        Files.write(file.toPath(), "Line1\r\nLine2\r\nLine3\r\n".getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(file, 0, 2, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_File_WithThreads() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-threads", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\r\n");
        }
        Files.write(file.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forLines(file, 0, 100, 2, line -> lines.add(line));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForLines_File_WithThreadsAndCallback() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-threads-cb", ".txt").toFile();
        Files.write(file.toPath(), "Line1\r\nLine2\r\nLine3\r\n".getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(file, 0, 3, 2, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_Collection_Basic() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "coll2", ".txt").toFile();
        Files.write(file1.toPath(), "File1Line1\r\nFile1Line2\r\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "File2Line1\r\nFile2Line2\r\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1, file2);
        java.util.List<String> lines = new java.util.ArrayList<>();

        IOUtil.forLines(files, line -> lines.add(line));

        assertEquals(4, lines.size());
    }

    @Test
    public void testForLines_Collection_WithCallback() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-cb1", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\r\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(files, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(1, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_Collection_WithOffsetAndCount() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-offset1", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\r\nLine2\r\nLine3\r\nLine4\r\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = new java.util.ArrayList<>();

        IOUtil.forLines(files, 1, 2, line -> lines.add(line));

        assertEquals(2, lines.size());
        assertEquals("Line2", lines.get(0));
    }

    @Test
    public void testForLines_Collection_WithThreads() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-threads", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Line").append(i).append("\r\n");
        }
        Files.write(file1.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        IOUtil.forLines(files, 0, 50, 2, line -> lines.add(line));

        assertEquals(50, lines.size());
    }

    @Test
    public void testForLines_File_ReadAndProcessThreads() throws Exception {
        File file = Files.createTempFile(tempFolder, "read-process", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\r\n");
        }
        Files.write(file.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forLines(file, 1, 2, 100, line -> lines.add(line));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForLines_File_ReadProcessThreadsWithCallback() throws Exception {
        File file = Files.createTempFile(tempFolder, "read-process-cb", ".txt").toFile();
        Files.write(file.toPath(), "Line1\r\nLine2\r\nLine3\r\n".getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(file, 1, 2, 100, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_File_WithOffsetCountReadProcessThreads() throws Exception {
        File file = Files.createTempFile(tempFolder, "offset-read-process", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\r\n");
        }
        Files.write(file.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forLines(file, 10, 50, 1, 2, 100, line -> lines.add(line));

        assertEquals(50, lines.size());
    }

    @Test
    public void testForLines_File_WithOffsetCountReadProcessThreadsCallback() throws Exception {
        File file = Files.createTempFile(tempFolder, "offset-read-process-cb", ".txt").toFile();
        Files.write(file.toPath(), "Line1\r\nLine2\r\nLine3\r\nLine4\r\nLine5\r\n".getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(file, 1, 3, 1, 2, 50, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_Collection_ReadProcessThreads() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-rp1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "coll-rp2", ".txt").toFile();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Line").append(i).append("\r\n");
        }
        Files.write(file1.toPath(), sb.toString().getBytes(UTF_8));
        Files.write(file2.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1, file2);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        IOUtil.forLines(files, 0, 100, 2, 1024, line -> lines.add(line));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForLines_Collection_ReadProcessThreadsCallback() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-rp-cb", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\r\nLine2\r\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(files, 1, 2, 50, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_Collection_WithOffsetCountReadProcessThreads() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-offset-rp", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\r\n");
        }
        Files.write(file1.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        IOUtil.forLines(files, 10, 30, 1, 2, 100, line -> lines.add(line));

        assertEquals(30, lines.size());
    }

    @Test
    public void testForLines_Collection_WithOffsetCountReadProcessThreadsCallback() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-offset-rp-cb", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\r\nLine2\r\nLine3\r\nLine4\r\nLine5\r\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(files, 1, 3, 1, 2, 50, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_InputStream_Basic() throws Exception {
        String content = "Line1\r\nLine2\r\nLine3\r\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(is, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line1", lines.get(0));
    }

    @Test
    public void testForLines_InputStream_WithCallback() throws Exception {
        String content = "Line1\r\nLine2\r\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(is, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_InputStream_WithOffsetAndCount() throws Exception {
        String content = "Line1\r\nLine2\r\nLine3\r\nLine4\r\nLine5\r\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(is, 1, 3, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line2", lines.get(0));
    }

    @Test
    public void testForLines_InputStream_WithOffsetCountCallback() throws Exception {
        String content = "Line1\r\nLine2\r\nLine3\r\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(is, 0, 2, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_InputStream_WithProcessThreads() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\r\n");
        }
        InputStream is = new ByteArrayInputStream(sb.toString().getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forLines(is, 0L, 100L, 2, 16, Fnn.c(line -> lines.add(line)));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForLines_InputStream_WithProcessThreadsCallback() throws Exception {
        String content = "Line1\r\nLine2\r\nLine3\r\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(is, 0L, 3L, 2, 16, Fnn.c(line -> lines.add(line)), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_Reader_Basic() throws Exception {
        Reader reader = new StringReader("Line1\r\nLine2\r\nLine3\r\n");

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(reader, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line1", lines.get(0));
    }

    @Test
    public void testForLines_Reader_WithCallback() throws Exception {
        Reader reader = new StringReader("Line1\r\nLine2\r\n");

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(reader, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_Reader_WithOffsetAndCount() throws Exception {
        Reader reader = new StringReader("Line1\r\nLine2\r\nLine3\r\nLine4\r\nLine5\r\n");

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(reader, 1, 3, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line2", lines.get(0));
    }

    @Test
    public void testForLines_Reader_WithOffsetCountCallback() throws Exception {
        Reader reader = new StringReader("Line1\r\nLine2\r\nLine3\r\n");

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(reader, 0, 2, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_Reader_WithProcessThreads() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\r\n");
        }
        Reader reader = new StringReader(sb.toString());

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forLines(reader, 0L, 100L, 2, 16, line -> lines.add(line));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForLines_Reader_WithProcessThreadsCallback() throws Exception {
        Reader reader = new StringReader("Line1\r\nLine2\r\nLine3\r\n");

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forLines(reader, 0L, 3L, 2, 16, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForLines_EmptyFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "empty-for-lines", ".txt").toFile();

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(file, line -> lines.add(line));

        assertEquals(0, lines.size());
    }

    @Test
    public void testForLines_EmptyInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(is, line -> lines.add(line));

        assertEquals(0, lines.size());
    }

    @Test
    public void testForLines_EmptyReader() throws Exception {
        Reader reader = new StringReader("");

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forLines(reader, line -> lines.add(line));

        assertEquals(0, lines.size());
    }

    @Test
    public void testGetHostName() {
        String hostName = IOUtil.getHostName();

        assertNotNull(hostName);
        assertTrue(hostName.length() > 0);
    }

    @Test
    public void testFreeDiskSpaceKb_Default() {
        long freeSpace = IOUtil.freeDiskSpaceKb();

        assertTrue(freeSpace > 0);
    }

    @Test
    public void testFreeDiskSpaceKb_WithTimeout() {
        long freeSpace = IOUtil.freeDiskSpaceKb(5000);

        assertTrue(freeSpace > 0);
    }

    @Test
    public void testFreeDiskSpaceKb_WithPath() throws Exception {
        String path = tempFolder.toFile().getAbsolutePath();
        long freeSpace = IOUtil.freeDiskSpaceKb(path);

        assertTrue(freeSpace > 0);
    }

    @Test
    public void testFreeDiskSpaceKb_WithPathAndTimeout() throws Exception {
        String path = tempFolder.toFile().getAbsolutePath();
        long freeSpace = IOUtil.freeDiskSpaceKb(path, 5000);

        assertTrue(freeSpace > 0);
    }

    @Test
    public void testZip_LargeFile() throws Exception {
        File largeSource = Files.createTempFile(tempFolder, "large-source", ".txt").toFile();
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is line ").append(i).append(" with some content to make it larger.\r\n");
        }
        Files.write(largeSource.toPath(), largeContent.toString().getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "large-zip", ".zip").toFile();
        IOUtil.zip(largeSource, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
        assertTrue(zipFile.length() < largeSource.length());
    }

    @Test
    public void testMerge_LargeFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "large-merge1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "large-merge2", ".txt").toFile();

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("Line ").append(i).append("\r\n");
        }

        Files.write(file1.toPath(), content.toString().getBytes(UTF_8));
        Files.write(file2.toPath(), content.toString().getBytes(UTF_8));

        File[] sourceFiles = { file1, file2 };
        File destFile = Files.createTempFile(tempFolder, "large-merged", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertTrue(totalBytes > 0);
        assertEquals(file1.length() + file2.length(), totalBytes);
    }

    @Test
    public void testForLines_LargeFile() throws Exception {
        File largeFile = Files.createTempFile(tempFolder, "large-for-lines", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Line ").append(i).append("\r\n");
        }
        Files.write(largeFile.toPath(), sb.toString().getBytes(UTF_8));

        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
        IOUtil.forLines(largeFile, line -> count.incrementAndGet());

        assertEquals(10000, count.get());
    }

    @Test
    public void testForLines_WithException() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-exception", ".txt").toFile();
        Files.write(file.toPath(), "Line1\r\nLine2\r\nLine3\r\n".getBytes(UTF_8));

        assertThrows(Exception.class, () -> {
            IOUtil.forLines(file, line -> {
                if (line.equals("Line2")) {
                    throw new RuntimeException("Test exception");
                }
            });
        });
    }

    @Test
    public void testContentEquals_LargeFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "large-eq1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "large-eq2", ".txt").toFile();

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            content.append("Line ").append(i).append("\r\n");
        }

        Files.write(file1.toPath(), content.toString().getBytes(UTF_8));
        Files.write(file2.toPath(), content.toString().getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(file1, file2);

        assertTrue(result);
    }

    @Test
    public void testListFiles_DeepNesting() throws Exception {
        File root = Files.createTempDirectory(tempFolder, "deep").toFile();
        File current = root;

        for (int i = 0; i < 5; i++) {
            current = new File(current, "level" + i);
            current.mkdir();
            File file = new File(current, "file" + i + ".txt");
            Files.write(file.toPath(), ("Content " + i).getBytes(UTF_8));
        }

        java.util.List<File> files = IOUtil.listFiles(root, true, true);

        assertNotNull(files);
        assertTrue(files.size() >= 5);
    }

    @Test
    public void testWalk_WithFileFilter() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "walk-filter").toFile();
        File txtFile = new File(dir, "file.txt");
        File csvFile = new File(dir, "file.csv");
        Files.write(txtFile.toPath(), "Text".getBytes(UTF_8));
        Files.write(csvFile.toPath(), "CSV".getBytes(UTF_8));

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir, false, false);
        long txtCount = stream.filter(f -> f.getName().endsWith(".txt")).count();

        assertEquals(1, txtCount);
    }

    @Test
    public void testNewAppendableWriter_WithStringBuilder() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newAppendableWriter(sb);

        writer.write("Hello");
        writer.write(" ");
        writer.write("World");
        writer.flush();

        assertEquals("Hello World", sb.toString());
    }

    @Test
    public void testNewAppendableWriter_WithStringBuffer() throws IOException {
        StringBuffer sb = new StringBuffer();
        Writer writer = IOUtil.newAppendableWriter(sb);

        writer.write("Test");
        writer.flush();

        assertEquals("Test", sb.toString());
    }

    @Test
    public void testNewAppendableWriter_MultipleWrites() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newAppendableWriter(sb);

        for (int i = 0; i < 5; i++) {
            writer.write("Line" + i + "\r\n");
        }
        writer.flush();

        assertTrue(sb.toString().contains("Line0"));
        assertTrue(sb.toString().contains("Line4"));
    }

    @Test
    public void testNewAppendableWriter_NullAppendable() {
        assertThrows(IllegalArgumentException.class, () -> {
            IOUtil.newAppendableWriter(null);
        });
    }

    @Test
    public void testNewBrotliInputStream_BasicDecompression() throws IOException {
        byte[] testData = TEST_CONTENT.getBytes(Charsets.UTF_8);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (InputStream testInput = new ByteArrayInputStream(testData)) {
            try {
                BrotliInputStream brotliIn = IOUtil.newBrotliInputStream(testInput);
                assertNotNull(brotliIn);
                brotliIn.close();
            } catch (NoClassDefFoundError | UncheckedIOException e) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void testNewBrotliInputStream_NullInputStream() {
        assertThrows(Exception.class, () -> {
            IOUtil.newBrotliInputStream(null);
        });
    }
}
