package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;

public class IOUtilWriteTest extends IOUtilTestSupport {
    @Test
    public void testWriteLine_ToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        IOUtil.writeLine("Test Line", outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Test Line\n", content);
    }

    @Test
    public void testWriteLine_ToWriter() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLine("Test Line", fw);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Test Line\n", content);
    }

    @Test
    public void testWriteLine_ToWriterWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLine("Test Line", fw, true);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Test Line\n", content);
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
        assertEquals("Line 1\nLine 2\nLine 3\n", content);
    }

    @Test
    public void testWriteLine_NullObject() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        IOUtil.writeLine(null, outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("null\n", content);
    }

    @Test
    public void testWriteLine_EmptyString() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();

        IOUtil.writeLine("", outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("\n", content);
    }

    @Test
    public void testWriteLine_NonexistentDirectory() throws IOException {
        File outputFile = new File(tempFolder.toFile(), "nonexistent/output.txt");
        IOUtil.writeLine("Test", outputFile);
        assertEquals("Test", IOUtil.readLine(outputFile, 0));
    }

    @Test
    public void testWriteLines_IteratorToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        IOUtil.writeLines(lines.iterator(), outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\nLine 2\nLine 3\n", content);
    }

    @Test
    public void testWriteLines_IteratorToWriter() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLines(lines.iterator(), fw);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\nLine 2\nLine 3\n", content);
    }

    @Test
    public void testWriteLines_IteratorToWriterWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLines(lines.iterator(), fw, true);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\nLine 2\nLine 3\n", content);
    }

    @Test
    public void testWriteLines_EmptyIterator() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.writeLines(java.util.Collections.emptyList().iterator(), outputFile);
        assertEquals("", IOUtil.readAllToString(outputFile));

        java.io.StringWriter sw = new java.io.StringWriter();
        IOUtil.writeLines(java.util.Collections.emptyIterator(), sw);
        assertEquals("", sw.toString());

        java.io.StringWriter flushed = new java.io.StringWriter();
        IOUtil.writeLines(java.util.Collections.emptyIterator(), flushed, true);
        assertEquals("", flushed.toString());
    }

    @Test
    public void testWriteLines_IterableToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        IOUtil.writeLines(lines, outputFile);

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\nLine 2\nLine 3\n", content);
    }

    @Test
    public void testWriteLines_IterableToWriter() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLines(lines, fw);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\nLine 2\nLine 3\n", content);
    }

    @Test
    public void testWriteLines_IterableToWriterWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        java.util.List<String> lines = java.util.Arrays.asList("Line 1", "Line 2", "Line 3");

        try (java.io.FileWriter fw = new java.io.FileWriter(outputFile)) {
            IOUtil.writeLines(lines, fw, true);
        }

        String content = IOUtil.readAllToString(outputFile);
        assertEquals("Line 1\nLine 2\nLine 3\n", content);
    }

    @Test
    public void testWriteLines_EmptyIterable() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.writeLines(java.util.Collections.emptyList(), outputFile);
        assertEquals("", IOUtil.readAllToString(outputFile));

        java.io.StringWriter sw = new java.io.StringWriter();
        IOUtil.writeLines(java.util.Collections.emptyList(), sw);
        assertEquals("", sw.toString());

        java.io.StringWriter flushed = new java.io.StringWriter();
        IOUtil.writeLines(java.util.Collections.emptyList(), flushed, true);
        assertEquals("", flushed.toString());
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
    public void testWriteLines_NonexistentDirectory() throws IOException {
        File outputFile = new File(tempFolder.toFile(), "nonexistent/output.txt");
        java.util.List<String> lines = java.util.Arrays.asList("Line 1");
        IOUtil.writeLines(lines, outputFile);
        assertEquals("Line 1", IOUtil.readLine(outputFile, 0));
    }

    @Test
    public void testWriteLines_Iterator_NullElement_WritesNullString() throws IOException {
        // When the iterator contains a null element, the implementation writes
        // Strings.NULL_CHAR_ARRAY (the text "null") instead of calling N.toString(line).
        java.io.StringWriter sw = new java.io.StringWriter();
        java.util.Iterator<String> iter = java.util.Arrays.asList("hello", null, "world").iterator();
        IOUtil.writeLines(iter, sw);
        String result = sw.toString();
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("world"));
    }

    @Test
    public void testWriteLines_Iterable_NullElement_WritesNullString() throws IOException {
        java.io.StringWriter sw = new java.io.StringWriter();
        IOUtil.writeLines(java.util.Arrays.asList("first", null, "last"), sw);
        String result = sw.toString();
        assertTrue(result.contains("first"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("last"));
    }

    @Test
    public void testWrite_BooleanToWriter() throws IOException {
        assertEquals("true", writeToString(w -> IOUtil.write(true, w)));
        assertEquals("false", writeToString(w -> IOUtil.write(false, w)));
    }

    @Test
    public void testWrite_CharToWriter() throws IOException {
        assertEquals("A", writeToString(w -> IOUtil.write('A', w)));
        assertEquals("世", writeToString(w -> IOUtil.write('\u4E16', w)));
    }

    @Test
    public void testWrite_ByteToWriter() throws IOException {
        assertEquals("65", writeToString(w -> IOUtil.write((byte) 65, w)));
        assertEquals("-128", writeToString(w -> IOUtil.write((byte) -128, w)));
    }

    @Test
    public void testWrite_ShortToWriter() throws IOException {
        assertEquals("12345", writeToString(w -> IOUtil.write((short) 12345, w)));
        assertEquals("-32768", writeToString(w -> IOUtil.write((short) -32768, w)));
    }

    @Test
    public void testWrite_IntToWriter() throws IOException {
        assertEquals("123456789", writeToString(w -> IOUtil.write(123456789, w)));
        assertEquals("-987654321", writeToString(w -> IOUtil.write(-987654321, w)));
    }

    @Test
    public void testWrite_LongToWriter() throws IOException {
        assertEquals("9876543210", writeToString(w -> IOUtil.write(9876543210L, w)));
        assertEquals("-9876543210", writeToString(w -> IOUtil.write(-9876543210L, w)));
    }

    @Test
    public void testWrite_FloatToWriter() throws IOException {
        assertEquals("3.14", writeToString(w -> IOUtil.write(3.14f, w)));
        assertEquals("-2.718", writeToString(w -> IOUtil.write(-2.718f, w)));
    }

    @Test
    public void testWrite_DoubleToWriter() throws IOException {
        assertEquals("3.141592653589793", writeToString(w -> IOUtil.write(3.141592653589793, w)));
        assertEquals("-2.718281828459045", writeToString(w -> IOUtil.write(-2.718281828459045, w)));
    }

    @Test
    public void testWrite_ObjectToWriter() throws IOException {
        assertEquals("Hello", writeToString(w -> IOUtil.write((Object) "Hello", w)));
        assertEquals("42", writeToString(w -> IOUtil.write(Integer.valueOf(42), w)));
    }

    @Test
    public void testWrite_Object_Null() throws IOException {
        assertEquals("null", writeToString(w -> IOUtil.write((Object) null, w)));
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
        Writer writer = IOUtil.newStringWriter(sb);
        IOUtil.write(TEST_CONTENT, writer);
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharSequenceToWriterWithFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        IOUtil.write(TEST_CONTENT, writer, true);
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharSequenceToWriterWithoutFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        IOUtil.write(TEST_CONTENT, writer, false);
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
        Writer writer = IOUtil.newStringWriter(sb);
        IOUtil.write(TEST_CONTENT.toCharArray(), writer);
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        char[] chars = "0123456789".toCharArray();
        IOUtil.write(chars, 2, 5, writer);
        writer.flush();
        assertEquals("23456", sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharArrayToWriterWithFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        IOUtil.write(TEST_CONTENT.toCharArray(), writer, true);
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_CharArrayWithOffsetCountToWriterWithFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
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
        Writer writer = IOUtil.newStringWriter(sb);
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
    public void testWrite_FileToItself_isRejectedWithoutDataLoss() throws IOException {
        // Regression: a self-copy must not truncate/wipe the source. write(File, File) opens the output
        // with a truncating FileOutputStream, so a same-file copy previously zeroed the file and returned 0.
        // It is now rejected up front (IllegalArgumentException), consistent with copyFile(...).
        final long originalLen = tempFile.length();
        assertTrue(originalLen > 0);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(tempFile, tempFile));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(tempFile, 0, Long.MAX_VALUE, tempFile));
        assertEquals(originalLen, tempFile.length());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(tempFile));
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
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8));
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(is, fos);
            assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_InputStreamToOutputStreamBreaksOnZeroProgress() throws IOException {
        final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

        assertEquals(0, IOUtil.write(new ZeroThenEofInputStream(), output));
        assertEquals(0, output.size());
    }

    @Test
    public void testWrite_InputStreamWithOffsetCountToOutputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream("0123456789ABCDEF".getBytes(UTF_8));
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(is, 5, 6, fos);
            assertEquals(6, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals("56789A", content);
    }

    @Test
    public void testWrite_InputStreamToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8));
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            long bytesWritten = IOUtil.write(is, fos, true);
            assertEquals(TEST_CONTENT.getBytes(UTF_8).length, bytesWritten);
        }
        String content = IOUtil.readAllToString(outputFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWrite_InputStreamWithOffsetCountToOutputStreamWithFlush() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream("0123456789".getBytes(UTF_8));
             FileOutputStream fos = new FileOutputStream(outputFile)) {
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
        Writer writer = IOUtil.newStringWriter(sb);
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long charsWritten = IOUtil.write(reader, writer);
            assertEquals(TEST_CONTENT.length(), charsWritten);
        }
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testWrite_ReaderToWriterBreaksOnZeroProgress() throws IOException {
        final java.io.StringWriter writer = new java.io.StringWriter();

        assertEquals(0, IOUtil.write(new ZeroThenEofReader(), writer));
        assertEquals("", writer.toString());
    }

    @Test
    public void testWrite_ReaderWithOffsetCountToWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
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
        Writer writer = IOUtil.newStringWriter(sb);
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
        Writer writer = IOUtil.newStringWriter(sb);
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
        Writer writer = IOUtil.newStringWriter(sb);
        try (Reader reader = new StringReader(MULTILINE_CONTENT)) {
            long charsWritten = IOUtil.write(reader, writer);
            assertEquals(MULTILINE_CONTENT.length(), charsWritten);
        }
        writer.flush();
        assertEquals(MULTILINE_CONTENT, sb.toString());
        writer.close();
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

        try (InputStream is = new ByteArrayInputStream(data);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
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

        try (Reader reader1 = new StringReader(UNICODE_CONTENT);
             Reader reader2 = new StringReader(UNICODE_CONTENT)) {
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
    public void testWrite_InputStreamOffsetBeyondEnd() throws Exception {
        byte[] data = "Hello".getBytes(UTF_8);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data)) {
            long written = IOUtil.write(bais, 100L, 5L, baos, false);
            assertEquals(0L, written);
        }
    }

    @Test
    public void testWriteLines_doesNotCloseCallerWriter() throws Exception {
        File out = Files.createTempFile(tempFolder, "wl", ".txt").toFile();
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        try (Writer w = new java.io.OutputStreamWriter(new FileOutputStream(out), UTF_8) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        }) {
            IOUtil.writeLines(java.util.Arrays.asList("a", "b"), w);
            // Caller-supplied writer must still be usable after writeLines returns.
            assertFalse(closed.get(), "writeLines must not close caller's Writer");
            w.write("c\n");
        }
        // After try-with-resources, content should include all three lines.
        String content = new String(Files.readAllBytes(out.toPath()), UTF_8);
        assertTrue(content.contains("a"));
        assertTrue(content.contains("b"));
        assertTrue(content.contains("c"));
    }

    @Test
    public void testWrite_CharArrayNegativeOffsetOrCountThrows() throws Exception {
        // Regression: char[] write variants must validate offset/count like their byte[]/Writer siblings (#34-37).
        final char[] chars = { 'a', 'b', 'c' };
        final java.io.File f = new java.io.File("./tmp_abacus_write_neg_test.txt");
        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> IOUtil.write(chars, -1, 0, f));
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> IOUtil.write(chars, 0, -1, f));
            final java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> IOUtil.write(chars, -1, 0, os));
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> IOUtil.write(chars, 0, -1, os));
        } finally {
            f.delete();
        }
    }

    @Test
    public void testWriteFileToFile_negativeOffsetOrCount_doesNotTruncateOutput() throws Exception {
        // regression: write(File, long, long, File) opened (and truncated) the output file before
        // validating offset/count, so an invalid call destroyed the existing output content.
        // Siblings (write(File, long, long, OutputStream, boolean), write(InputStream, long, long, File))
        // validate before any side effect.
        final File src = tempFolder.resolve("regression_write_src.txt").toFile();
        IOUtil.write("source-data", src);

        final File out = tempFolder.resolve("regression_write_out.txt").toFile();
        IOUtil.write("KEEP", out);

        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(src, -1L, 5L, out));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(src, 0L, -1L, out));

        assertEquals("KEEP", IOUtil.readAllToString(out));

        // a valid call still overwrites the output file
        assertEquals(11L, IOUtil.write(src, 0L, Long.MAX_VALUE, out));
        assertEquals("source-data", IOUtil.readAllToString(out));
    }

    @Test
    public void testWriteBytesToFile_outOfBoundsRange_doesNotTruncateOutput() throws Exception {
        // regression: write(byte[], offset, count, File) opened (and truncated) the target before
        // validating offset+count against the array length, so a bad range destroyed existing content
        // and then failed with IndexOutOfBoundsException.
        final File out = tempFolder.resolve("regression_write_bytes_out.txt").toFile();
        IOUtil.write("KEEP", out);

        final byte[] bytes = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(bytes, 1, 5, out));

        assertEquals("KEEP", IOUtil.readAllToString(out));

        // a valid sub-range still overwrites the output file
        IOUtil.write(bytes, 1, 2, out);
        assertArrayEquals(new byte[] { 2, 3 }, IOUtil.readAllBytes(out));
    }

    @Test
    public void testWriteReaderOffsetPastEofReturnsZeroLikeInputStreamOverload() throws Exception {
        final java.io.StringWriter writer = new java.io.StringWriter();

        assertEquals(0L, IOUtil.write(new StringReader("abc"), 10L, 1L, writer, false));
        assertEquals("", writer.toString());
    }

    @Test
    public void testWriteFlushFlagIsHonoredWhenNoContentIsWritten() throws Exception {
        final java.util.concurrent.atomic.AtomicInteger byteFlushes = new java.util.concurrent.atomic.AtomicInteger();
        final java.io.OutputStream output = new java.io.ByteArrayOutputStream() {
            @Override
            public void flush() {
                byteFlushes.incrementAndGet();
            }
        };

        assertEquals(0L, IOUtil.write(new ByteArrayInputStream(new byte[0]), 0, 0, output, true));
        assertEquals(0L, IOUtil.write(new ByteArrayInputStream(new byte[] { 1 }), 2, 1, output, true));
        assertEquals(2, byteFlushes.get());

        final java.util.concurrent.atomic.AtomicInteger charFlushes = new java.util.concurrent.atomic.AtomicInteger();
        final Writer writer = new java.io.StringWriter() {
            @Override
            public void flush() {
                charFlushes.incrementAndGet();
            }
        };

        assertEquals(0L, IOUtil.write(new StringReader(""), 0, 0, writer, true));
        assertEquals(0L, IOUtil.write(new StringReader("a"), 2, 1, writer, true));
        assertEquals(2, charFlushes.get());
    }

    @Test
    public void testWrite_EmptyByteArrayTruncatesExistingFile() throws IOException {
        // A write to a File is a complete replacement, so empty input empties the target rather than
        // leaving the previous content in place. append(..) is the overload that never truncates.
        File outputFile = Files.createTempFile(tempFolder, "keep-content", ".txt").toFile();
        Files.write(outputFile.toPath(), "keep-me".getBytes(UTF_8));
        IOUtil.write(new byte[0], outputFile);
        assertEquals("", IOUtil.readAllToString(outputFile));

        Files.write(outputFile.toPath(), "keep-me".getBytes(UTF_8));
        IOUtil.append(new byte[0], outputFile);
        assertEquals("keep-me", IOUtil.readAllToString(outputFile));
    }

    @Test
    public void testWrite_MissingSourceThrowsIOExceptionWithoutTouchingDest() throws IOException {
        File missing = new File(tempFolder.toFile(), "no-such-write-source.bin");
        File dest = Files.createTempFile(tempFolder, "write-dest", ".bin").toFile();
        Files.write(dest.toPath(), "precious".getBytes(UTF_8));

        assertThrows(IOException.class, () -> IOUtil.write(missing, dest));
        assertEquals("precious", IOUtil.readAllToString(dest));
    }

    @Test
    public void testWrite_EmptyInputStillFlushesWhenRequested() throws IOException {
        // flush=true is a promise about the output stream, not about the input: it must be honored even when
        // there is nothing to write.
        final CountingOutputStream os = new CountingOutputStream();
        IOUtil.write(new byte[0], os, true);
        IOUtil.write(new byte[0], 0, 0, os, true);
        IOUtil.write(new char[0], os, true);
        IOUtil.write(new char[0], 0, 0, os, true);
        IOUtil.write(new char[0], 0, 0, UTF_8, os, true);
        assertEquals(5, os.flushCount.get());

        final CountingWriter writer = new CountingWriter();
        IOUtil.write(new char[0], writer, true);
        IOUtil.write(new char[0], 0, 0, writer, true);
        IOUtil.writeLines(Collections.emptyList(), writer, true);
        IOUtil.writeLines(Collections.emptyList().iterator(), writer, true);
        assertEquals(4, writer.flushCount.get());
    }

    @Test
    public void testWrite_EmptyInputDoesNotFlushWhenNotRequested() throws IOException {
        final CountingOutputStream os = new CountingOutputStream();
        IOUtil.write(new byte[0], os, false);
        IOUtil.write(new char[0], os, false);
        assertEquals(0, os.flushCount.get());
    }

    @Test
    public void testWrite_NullCharSequence() throws IOException {
        // File write replaces content, so null means "no content". Stream/Writer overloads write the text "null".
        final File output = Files.createTempFile(tempFolder, "null-cs", ".txt").toFile();
        Files.write(output.toPath(), "old".getBytes(UTF_8));
        IOUtil.write((CharSequence) null, output);
        assertEquals("", IOUtil.readAllToString(output));

        final java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        IOUtil.write((CharSequence) null, os);
        assertEquals("null", os.toString("UTF-8"));

        final StringBuilder sb = new StringBuilder();
        IOUtil.write((CharSequence) null, IOUtil.newStringWriter(sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void testWrite_EmptyCharSequence_TruncatesExistingFile() throws Exception {
        final File f = new File(tempFolder.toFile(), "trunc-cs.txt");
        Files.write(f.toPath(), "PRECIOUS".getBytes(UTF_8));
        IOUtil.write("", f);
        assertTrue(f.exists());
        assertEquals(0, f.length());
    }

    @Test
    public void testWrite_emptyAndNullArraysTruncateExistingFile() throws Exception {
        final File a = new File(tempFolder.toFile(), "trunc-bytes.txt");
        Files.write(a.toPath(), "PRECIOUS".getBytes(UTF_8));
        IOUtil.write(new byte[0], a);
        assertEquals(0, a.length());

        final File b = new File(tempFolder.toFile(), "trunc-null-bytes.txt");
        Files.write(b.toPath(), "PRECIOUS".getBytes(UTF_8));
        IOUtil.write((byte[]) null, b);
        assertEquals(0, b.length());

        final File c = new File(tempFolder.toFile(), "trunc-chars.txt");
        Files.write(c.toPath(), "PRECIOUS".getBytes(UTF_8));
        IOUtil.write(new char[0], c);
        assertEquals(0, c.length());

        final File d = new File(tempFolder.toFile(), "trunc-null-chars.txt");
        Files.write(d.toPath(), "PRECIOUS".getBytes(UTF_8));
        IOUtil.write((char[]) null, UTF_8, d);
        assertEquals(0, d.length());
    }

    @Test
    public void testWriteLines_emptyInputTruncatesExistingFile() throws Exception {
        final File a = new File(tempFolder.toFile(), "trunc-lines-iterable.txt");
        Files.write(a.toPath(), "PRECIOUS".getBytes(UTF_8));
        IOUtil.writeLines(Collections.<String> emptyList(), a);
        assertEquals(0, a.length());

        final File b = new File(tempFolder.toFile(), "trunc-lines-iterator.txt");
        Files.write(b.toPath(), "PRECIOUS".getBytes(UTF_8));
        IOUtil.writeLines(Collections.<String> emptyList().iterator(), b);
        assertEquals(0, b.length());

        final File c = new File(tempFolder.toFile(), "trunc-lines-null.txt");
        Files.write(c.toPath(), "PRECIOUS".getBytes(UTF_8));
        IOUtil.writeLines((Iterable<?>) null, c);
        assertEquals(0, c.length());
    }

    @Test
    public void testWrite_zeroCountTruncatesExistingFile() throws Exception {
        final File f = new File(tempFolder.toFile(), "trunc-zero-count.txt");
        Files.write(f.toPath(), "PRECIOUS".getBytes(UTF_8));

        IOUtil.write("abc".getBytes(UTF_8), 1, 0, f);

        assertEquals(0, f.length());
    }

    @Test
    public void testWrite_emptyInputStillCreatesMissingFile() throws Exception {
        final File f = new File(tempFolder.toFile(), "created-empty/nested.txt");
        assertFalse(f.exists());

        IOUtil.write("", f);

        assertTrue(f.exists());
        assertEquals(0, f.length());
    }

    @Test
    public void testWrite_badRangeStillDoesNotTruncateTarget() throws Exception {
        final File f = new File(tempFolder.toFile(), "bad-range.txt");
        final byte[] original = "PRECIOUS".getBytes(UTF_8);

        Files.write(f.toPath(), original);
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new byte[3], 2, 5, f));
        assertArrayEquals(original, Files.readAllBytes(f.toPath()));

        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new char[3], 5, 0, f));
        assertArrayEquals(original, Files.readAllBytes(f.toPath()));
    }

    @Test
    public void testWriteCharArraySliceCanSplitASurrogatePair() throws Exception {
        final char[] emoji = "ab\uD83D\uDE00".toCharArray();

        final File encoded = Files.createTempFile(tempFolder, "surrogate-write", ".bin").toFile();
        IOUtil.write(emoji, 2, 1, UTF_8, encoded);
        assertArrayEquals(new byte[] { (byte) '?' }, Files.readAllBytes(encoded.toPath()));

        final File appended = Files.createTempFile(tempFolder, "surrogate-append", ".bin").toFile();
        IOUtil.append(emoji, 2, 1, UTF_8, appended);
        assertArrayEquals(new byte[] { (byte) '?' }, Files.readAllBytes(appended.toPath()));

        // an OutputStream destination encodes here too, so it replaces the lone surrogate as well
        final java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        IOUtil.write(emoji, 2, 1, UTF_8, stream);
        assertArrayEquals(new byte[] { (byte) '?' }, stream.toByteArray());

        // an encoding Writer replaces it one layer down, exactly as a File destination does here
        final File viaFileWriter = Files.createTempFile(tempFolder, "surrogate-filewriter", ".bin").toFile();
        try (Writer w = IOUtil.newFileWriter(viaFileWriter, UTF_8)) {
            IOUtil.write(emoji, 2, 1, w);
        }
        assertArrayEquals(new byte[] { (byte) '?' }, Files.readAllBytes(viaFileWriter.toPath()));

        // a Writer that buffers characters keeps the lone surrogate
        final java.io.StringWriter buffered = new java.io.StringWriter();
        IOUtil.write(emoji, 2, 1, buffered);
        assertEquals(1, buffered.toString().length());
        assertTrue(Character.isHighSurrogate(buffered.toString().charAt(0)));
    }
}
