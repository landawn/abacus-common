package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class IOUtilAppendTest extends IOUtilTestSupport {
    @Test
    public void testAppend_FileToItself_isRejectedWithoutGrowth() throws IOException {
        // Regression: append(File, File) opens the target in append mode while reading the same
        // file from position 0, so a self-append grows the file unboundedly (the reader keeps
        // finding the bytes the writer just appended). It must be rejected up front like
        // write(File, File) and copyFile(...).
        final long originalLen = tempFile.length();
        assertTrue(originalLen > 0);
        // bounded-count overload first: on a regression this fails fast without unbounded growth
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(tempFile, 0, 16, tempFile));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(tempFile, tempFile));
        assertEquals(originalLen, tempFile.length());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(tempFile));
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
        Files.write(targetFile.toPath(), "Header\n".getBytes(UTF_8));

        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) ('A' + (i % 26));
        }

        try (InputStream is = new ByteArrayInputStream(largeData)) {
            long bytesAppended = IOUtil.append(is, targetFile);

            byte[] result = IOUtil.readAllBytes(targetFile);
            assertEquals(7 + 10000, result.length);
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

    // ===== append(byte[], int, int, File) =====

    @Test
    public void testAppend_byteArrayOffsetCountFile() throws Exception {
        File target = Files.createTempFile(tempFolder, "append-bytes", ".bin").toFile();
        byte[] data = "Hello World!".getBytes(UTF_8);
        IOUtil.append(data, 6, 5, target);
        byte[] result = IOUtil.readAllBytes(target);
        assertEquals("World", new String(result, UTF_8));
    }

    @Test
    public void testAppend_byteArrayOffsetCountFile_appendsToExisting() throws Exception {
        File target = Files.createTempFile(tempFolder, "append-bytes-existing", ".bin").toFile();
        Files.write(target.toPath(), "Hello ".getBytes(UTF_8));
        byte[] data = "World!".getBytes(UTF_8);
        IOUtil.append(data, 0, data.length, target);
        byte[] result = IOUtil.readAllBytes(target);
        assertEquals("Hello World!", new String(result, UTF_8));
    }

    @Test
    public void testAppend_byteArrayOffsetCountFile_zeroCount() throws Exception {
        File target = Files.createTempFile(tempFolder, "append-bytes-zero", ".bin").toFile();
        byte[] data = "Hello".getBytes(UTF_8);
        IOUtil.append(data, 0, 0, target);
        assertEquals(0, target.length());
    }

    @Test
    public void testAppendLine_String() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Line 1\n".getBytes(UTF_8));

        IOUtil.appendLine("Line 2", targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Line 1"));
        assertTrue(result.contains("Line 2"));
    }

    @Test
    public void testAppendLine_WithCharset() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "First\n".getBytes(UTF_16));

        IOUtil.appendLine("Second", UTF_16, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_16);
        assertTrue(result.contains("First"));
        assertTrue(result.contains("Second"));
    }

    @Test
    public void testAppendLine_NullObject() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Start\n".getBytes(UTF_8));

        IOUtil.appendLine(null, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Start"));
    }

    @Test
    public void testAppendLine_Integer() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Number:\n".getBytes(UTF_8));

        IOUtil.appendLine(42, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Number:"));
        assertTrue(result.contains("42"));
    }

    @Test
    public void testAppendLines_StringList() throws IOException {
        File targetFile = Files.createTempFile(tempFolder, "target", ".txt").toFile();
        Files.write(targetFile.toPath(), "Header\n".getBytes(UTF_8));

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
        Files.write(targetFile.toPath(), "Title\n".getBytes(UTF_16));

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
        Files.write(targetFile.toPath(), "Start\n".getBytes(UTF_8));

        java.util.List<Object> lines = java.util.Arrays.asList("Text", 123, true, null);
        IOUtil.appendLines(lines, targetFile);

        String result = IOUtil.readAllToString(targetFile, UTF_8);
        assertTrue(result.contains("Start"));
        assertTrue(result.contains("Text"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
    }

    @Test
    public void testAppendCharSequence_nullCharsetUsesDefault() throws Exception {
        File f = Files.createTempFile(tempFolder, "appendcs", ".txt").toFile();
        // overwrite with a known prefix using default charset
        IOUtil.write("PRE-", f);
        // null charset must be tolerated and behave as DEFAULT_CHARSET (no NPE).
        IOUtil.append("hello", (Charset) null, f);
        String content = new String(Files.readAllBytes(f.toPath()), Charset.defaultCharset());
        assertEquals("PRE-hello", content);
    }

    @Test
    public void testAppendLine_nullCharsetUsesDefault() throws Exception {
        File f = Files.createTempFile(tempFolder, "appendln", ".txt").toFile();
        IOUtil.write("first\n", f);
        // null charset must not NPE; should append "second\n" using default charset.
        IOUtil.appendLine("second", (Charset) null, f);
        String content = new String(Files.readAllBytes(f.toPath()), Charset.defaultCharset());
        assertEquals("first\nsecond\n", content);
    }

    @Test
    public void testAppendEmptyBytesCreatesTargetFile() throws Exception {
        // regression: append(byte[], File) with empty input did not create the missing target file,
        // unlike the char[] sibling and the write family
        final java.io.File f1 = tempFolder.resolve("append_empty_bytes.txt").toFile();
        IOUtil.append(new byte[0], f1);
        assertTrue(f1.exists());

        final java.io.File f2 = tempFolder.resolve("append_zero_count_bytes.txt").toFile();
        IOUtil.append(new byte[] { 1, 2 }, 0, 0, f2);
        assertTrue(f2.exists());
    }

    @Test
    public void testAppend_emptyInputNeverTruncates() throws Exception {
        final File f = new File(tempFolder.toFile(), "append-empty.txt");
        Files.write(f.toPath(), "PRECIOUS".getBytes(UTF_8));

        IOUtil.append("", f);
        IOUtil.append((CharSequence) null, f);
        IOUtil.append(new byte[0], f);
        IOUtil.append(new char[0], f);

        assertEquals("PRECIOUS", new String(Files.readAllBytes(f.toPath()), UTF_8));
    }
}
