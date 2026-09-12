package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;

public class IOUtilReadTest extends IOUtilTestSupport {
    @Test
    public void testReadAllBytes_FromFile() {
        byte[] bytes = IOUtil.readAllBytes(tempFile);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testReadAllBytes_EmptyFile() {
        byte[] bytes = IOUtil.readAllBytes(emptyFile);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testReadAllBytes_LargeData() {
        byte[] bytes = IOUtil.readAllBytes(largeFile);
        assertNotNull(bytes);
        assertTrue(bytes.length > 10000);
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
    public void testReadAllBytes_EmptyInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(new byte[0])) {
            byte[] bytes = IOUtil.readAllBytes(is);
            assertNotNull(bytes);
            assertEquals(0, bytes.length);
        }
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
    public void testReadBytes_FromInputStreamWithOffset() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            byte[] bytes = IOUtil.readBytes(is, 5, 5);
            assertNotNull(bytes);
            assertEquals("56789", new String(bytes, UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            assertEquals("World", new String(IOUtil.readBytes(is, 6L, 5), UTF_8));
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
    public void testReadBytes_BreaksOnZeroProgressInputStream() throws IOException {
        final byte[] bytes = IOUtil.readBytes(new ZeroThenEofInputStream(), 0, 10);

        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testReadBytes_PartialLargeFile() throws IOException {
        byte[] bytes = IOUtil.readBytes(largeFile, 100, 50);
        assertNotNull(bytes);
        assertEquals(50, bytes.length);
    }

    @Test
    public void testReadBytes_FromInputStream_ZeroMaxLen() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            assertEquals(0, IOUtil.readBytes(is, 0L, 0).length);
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
    public void testReadAllChars_EmptyFile() {
        char[] chars = IOUtil.readAllChars(emptyFile);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testReadAllChars_LargeData() {
        char[] chars = IOUtil.readAllChars(largeFile);
        assertNotNull(chars);
        assertTrue(chars.length > 10000);
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
    public void testReadChars_FromReaderWithOffset() throws IOException {
        try (Reader reader = new StringReader("0123456789ABCDEF")) {
            char[] chars = IOUtil.readChars(reader, 5, 5);
            assertNotNull(chars);
            assertEquals("56789", new String(chars));
        }
        try (Reader reader = new StringReader("Hello World!")) {
            assertEquals("World", new String(IOUtil.readChars(reader, 6L, 5)));
        }
    }

    @Test
    public void testReadChars_BreaksOnZeroProgressReader() throws IOException {
        final char[] chars = IOUtil.readChars(new ZeroThenEofReader(), 0, 10);

        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testReadChars_FromReaderWithZeroLength() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            assertEquals(0, IOUtil.readChars(reader, 0, 0).length);
        }
        try (Reader reader = new StringReader("Hello World!")) {
            assertEquals(0, IOUtil.readChars(reader, 0L, 0).length);
        }
    }

    @Test
    public void testReadChars_PartialLargeFile() throws IOException {
        char[] chars = IOUtil.readChars(largeFile, 100, 50);
        assertNotNull(chars);
        assertEquals(50, chars.length);
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
    public void testReadAllToString_EmptyFile() {
        String content = IOUtil.readAllToString(emptyFile);
        assertNotNull(content);
        assertEquals("", content);
    }

    @Test
    public void testReadAllToString_LargeData() {
        String content = IOUtil.readAllToString(largeFile);
        assertNotNull(content);
        assertTrue(content.length() > 10000);
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
    public void testReadToString_PartialLargeFile() throws IOException {
        String content = IOUtil.readToString(largeFile, 100, 50);
        assertNotNull(content);
        assertEquals(50, content.length());
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
        Files.write(noNewlineFile.toPath(), "Line 1\nLine 2\nLine 3".getBytes(UTF_8));

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
    public void testReadAllBytes_ZipSkipsLeadingDirectoryEntries() throws Exception {
        final File zipFile = Files.createTempFile(tempFolder, "io-util-directory-first", ".zip").toFile();

        try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            out.putNextEntry(new java.util.zip.ZipEntry("directory/"));
            out.closeEntry();
            out.putNextEntry(new java.util.zip.ZipEntry("directory/entry.txt"));
            out.write("zip-content".getBytes(UTF_8));
            out.closeEntry();
        }

        assertEquals("zip-content", new String(IOUtil.readAllBytes(zipFile), UTF_8));
    }

    @Test
    public void testReadAllBytes_ZipWithOnlyDirectoriesFailsAndClosesArchive() throws Exception {
        final File zipFile = Files.createTempFile(tempFolder, "io-util-directory-only", ".zip").toFile();

        try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            out.putNextEntry(new java.util.zip.ZipEntry("directory/"));
            out.closeEntry();
        }

        final UncheckedIOException error = assertThrows(UncheckedIOException.class, () -> IOUtil.readAllBytes(zipFile));
        assertTrue(error.getCause().getMessage().contains("contains no file entries"));
        assertTrue(zipFile.delete(), "the ZipFile must be closed when opening fails");
    }

    @Test
    public void testReadLines_doesNotCloseCallerReader() throws Exception {
        File f = Files.createTempFile(tempFolder, "rl", ".txt").toFile();
        Files.write(f.toPath(), "x\ny\nz\n".getBytes(UTF_8));
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        // Use a BufferedReader so subsequent reads aren't lost — readLines() wraps a
        // non-BufferedReader in a pooled buffer that read-aheads then gets recycled,
        // which is incompatible with the "still advanceable" check below.
        try (Reader r = new java.io.BufferedReader(new java.io.InputStreamReader(new FileInputStream(f), UTF_8) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        })) {
            java.util.List<String> first = IOUtil.readLines(r, 0, 1);
            assertEquals(java.util.Collections.singletonList("x"), first);
            assertFalse(closed.get(), "readLines must not close caller's Reader");
            // Reader should still be advanceable.
            java.util.List<String> rest = IOUtil.readLines(r, 0, 10);
            assertEquals(java.util.Arrays.asList("y", "z"), rest);
        }
    }

    @Test
    public void testReadFileIntoByteBuffer_decompressesGzLikeSiblings() throws Exception {
        // regression: read(File, byte[], off, len) bypassed openFile(), returning raw gzip container
        // bytes while the char[] sibling and readBytes(File, ...) returned decompressed content
        final java.io.File gz = tempFolder.resolve("regression_data.gz").toFile();
        try (java.util.zip.GZIPOutputStream out = IOUtil.newGZIPOutputStream(IOUtil.newFileOutputStream(gz))) {
            out.write("hello world".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        final byte[] buf = new byte[11];
        final int n = IOUtil.read(gz, buf, 0, buf.length);

        assertEquals(11, n);
        assertEquals("hello world", new String(buf, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testReadFamilyReportsMissingFilesUnchecked() {
        final File missing = new File(tempFolder.toFile(), "no-such-checked-read.bin");

        // Every content-returning read wraps the failure, whole-file and sliced alike.
        assertThrows(UncheckedIOException.class, () -> IOUtil.readAllBytes(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readBytes(missing, 0, 10));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readAllChars(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readChars(missing, 0, 10));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readAllToString(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readToString(missing, 0, 10));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readAllLines(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readLines(missing, 0, 10));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readFirstLine(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readLastLine(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readLine(missing, 0));

        // ...as do the other question-answering methods.
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOf(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOfDirectory(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOfAsBigInteger(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOfDirectoryAsBigInteger(missing));
        // map(..) follows the same rule as the rest of the family now: a missing file is a FileNotFoundException
        // wrapped in UncheckedIOException, not a bad argument. All three overloads agree.
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(missing, java.nio.channels.FileChannel.MapMode.READ_ONLY));
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(missing, java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, 4));

        // The low-level buffer fill stays checked: it mirrors InputStream.read and returns a count.
        assertThrows(IOException.class, () -> IOUtil.read(missing, new byte[8]));
    }
}
