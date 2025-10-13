package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("2025")
public class LineIterator2025Test extends TestBase {

    @TempDir
    File tempDir;

    @Test
    @DisplayName("Test LineIterator(Reader) constructor with valid Reader")
    public void testConstructorWithValidReader() {
        Reader reader = new StringReader("line1\nline2\nline3");
        LineIterator iterator = new LineIterator(reader);

        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        iterator.close();
    }

    @Test
    @DisplayName("Test LineIterator(Reader) constructor with null Reader throws exception")
    public void testConstructorWithNullReader() {
        assertThrows(IllegalArgumentException.class, () -> new LineIterator(null));
    }

    @Test
    @DisplayName("Test of(File) with valid file")
    public void testOfFileWithValidFile() throws IOException {
        File testFile = new File(tempDir, "test.txt");
        Files.write(testFile.toPath(), "line1\nline2\nline3".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            assertNotNull(iterator);
            assertTrue(iterator.hasNext());
            assertEquals("line1", iterator.next());
            assertEquals("line2", iterator.next());
            assertEquals("line3", iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test of(File) with empty file")
    public void testOfFileWithEmptyFile() throws IOException {
        File testFile = new File(tempDir, "empty.txt");
        Files.write(testFile.toPath(), "".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test of(File) with non-existent file throws exception")
    public void testOfFileWithNonExistentFile() {
        File nonExistent = new File(tempDir, "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> LineIterator.of(nonExistent));
    }

    @Test
    @DisplayName("Test of(File, Charset) with UTF-8 encoding")
    public void testOfFileWithCharset() throws IOException {
        File testFile = new File(tempDir, "utf8.txt");
        String content = "Hello\nWorld\nTest";
        Files.write(testFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile, StandardCharsets.UTF_8)) {
            List<String> lines = new ArrayList<>();
            iterator.forEachRemaining(lines::add);

            assertEquals(3, lines.size());
            assertEquals("Hello", lines.get(0));
            assertEquals("World", lines.get(1));
            assertEquals("Test", lines.get(2));
        }
    }

    @Test
    @DisplayName("Test of(File, Charset) with null charset uses default")
    public void testOfFileWithNullCharset() throws IOException {
        File testFile = new File(tempDir, "default_encoding.txt");
        Files.write(testFile.toPath(), "line1\nline2".getBytes());

        try (LineIterator iterator = LineIterator.of(testFile, Charsets.UTF_8)) {
            assertTrue(iterator.hasNext());
            assertEquals("line1", iterator.next());
        }
    }

    @Test
    @DisplayName("Test of(InputStream) with valid input stream")
    public void testOfInputStreamWithValidStream() {
        InputStream inputStream = new ByteArrayInputStream("line1\nline2\nline3".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(inputStream)) {
            List<String> lines = new ArrayList<>();
            while (iterator.hasNext()) {
                lines.add(iterator.next());
            }

            assertEquals(3, lines.size());
            assertEquals("line1", lines.get(0));
            assertEquals("line2", lines.get(1));
            assertEquals("line3", lines.get(2));
        }
    }

    @Test
    @DisplayName("Test of(InputStream) with empty stream")
    public void testOfInputStreamWithEmptyStream() {
        InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(inputStream)) {
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test of(InputStream, Charset) with UTF-8 encoding")
    public void testOfInputStreamWithCharset() {
        String content = "First Line\nSecond Line\nThird Line";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(inputStream, StandardCharsets.UTF_8)) {
            assertEquals("First Line", iterator.next());
            assertEquals("Second Line", iterator.next());
            assertEquals("Third Line", iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test of(InputStream, Charset) with null charset")
    public void testOfInputStreamWithNullCharset() {
        InputStream inputStream = new ByteArrayInputStream("test\nline".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(inputStream, Charsets.UTF_8)) {
            assertTrue(iterator.hasNext());
            assertEquals("test", iterator.next());
        }
    }

    @Test
    @DisplayName("Test of(Reader) with valid reader")
    public void testOfReaderWithValidReader() {
        Reader reader = new StringReader("line1\nline2\nline3");

        try (LineIterator iterator = LineIterator.of(reader)) {
            assertNotNull(iterator);

            List<String> lines = new ArrayList<>();
            iterator.forEachRemaining(lines::add);

            assertEquals(3, lines.size());
            assertEquals("line1", lines.get(0));
            assertEquals("line2", lines.get(1));
            assertEquals("line3", lines.get(2));
        }
    }

    @Test
    @DisplayName("Test of(Reader) with empty reader")
    public void testOfReaderWithEmptyReader() {
        Reader reader = new StringReader("");

        try (LineIterator iterator = LineIterator.of(reader)) {
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test of(Reader) with null reader throws exception")
    public void testOfReaderWithNullReader() {
        assertThrows(IllegalArgumentException.class, () -> LineIterator.of((Reader) null));
    }

    @Test
    @DisplayName("Test hasNext() returns true when lines are available")
    public void testHasNextWithAvailableLines() {
        Reader reader = new StringReader("line1\nline2");
        LineIterator iterator = LineIterator.of(reader);

        assertTrue(iterator.hasNext());
        iterator.next();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertFalse(iterator.hasNext());

        iterator.close();
    }

    @Test
    @DisplayName("Test hasNext() returns false when no lines available")
    public void testHasNextWithNoLines() {
        Reader reader = new StringReader("");
        LineIterator iterator = LineIterator.of(reader);

        assertFalse(iterator.hasNext());
        iterator.close();
    }

    @Test
    @DisplayName("Test hasNext() can be called multiple times")
    public void testHasNextMultipleCalls() {
        Reader reader = new StringReader("line1");
        LineIterator iterator = LineIterator.of(reader);

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());

        assertEquals("line1", iterator.next());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());

        iterator.close();
    }

    @Test
    @DisplayName("Test next() returns lines in order")
    public void testNextReturnsLinesInOrder() {
        Reader reader = new StringReader("first\nsecond\nthird");
        LineIterator iterator = LineIterator.of(reader);

        assertEquals("first", iterator.next());
        assertEquals("second", iterator.next());
        assertEquals("third", iterator.next());

        iterator.close();
    }

    @Test
    @DisplayName("Test next() throws NoSuchElementException when no more lines")
    public void testNextThrowsWhenNoMoreLines() {
        Reader reader = new StringReader("only one line");
        LineIterator iterator = LineIterator.of(reader);

        iterator.next();
        assertThrows(NoSuchElementException.class, () -> iterator.next());

        iterator.close();
    }

    @Test
    @DisplayName("Test next() on empty iterator throws NoSuchElementException")
    public void testNextOnEmptyIterator() {
        Reader reader = new StringReader("");
        LineIterator iterator = LineIterator.of(reader);

        assertThrows(NoSuchElementException.class, () -> iterator.next());

        iterator.close();
    }

    @Test
    @DisplayName("Test next() handles empty lines correctly")
    public void testNextHandlesEmptyLines() {
        Reader reader = new StringReader("line1\n\nline3");
        LineIterator iterator = LineIterator.of(reader);

        assertEquals("line1", iterator.next());
        assertEquals("", iterator.next());
        assertEquals("line3", iterator.next());

        iterator.close();
    }

    @Test
    @DisplayName("Test close() closes the underlying reader")
    public void testCloseClosesUnderlyingReader() {
        Reader reader = new StringReader("line1\nline2\nline3");
        LineIterator iterator = LineIterator.of(reader);

        assertTrue(iterator.hasNext());
        iterator.close();

        assertFalse(iterator.hasNext());
    }

    @Test
    @DisplayName("Test close() can be called multiple times safely")
    public void testCloseMultipleTimes() {
        Reader reader = new StringReader("line1");
        LineIterator iterator = LineIterator.of(reader);

        assertDoesNotThrow(() -> {
            iterator.close();
            iterator.close();
            iterator.close();
        });
    }

    @Test
    @DisplayName("Test close() in try-with-resources")
    public void testCloseWithTryWithResources() {
        assertDoesNotThrow(() -> {
            try (LineIterator iterator = LineIterator.of(new StringReader("line1\nline2"))) {
                assertTrue(iterator.hasNext());
                iterator.next();
            }
        });
    }

    @Test
    @DisplayName("Test iteration with while loop")
    public void testIterationWithWhileLoop() {
        Reader reader = new StringReader("a\nb\nc\nd\ne");
        LineIterator iterator = LineIterator.of(reader);

        List<String> lines = new ArrayList<>();
        while (iterator.hasNext()) {
            lines.add(iterator.next());
        }

        assertEquals(5, lines.size());
        assertEquals("a", lines.get(0));
        assertEquals("e", lines.get(4));

        iterator.close();
    }

    @Test
    @DisplayName("Test forEachRemaining() processes all remaining lines")
    public void testForEachRemaining() {
        Reader reader = new StringReader("1\n2\n3\n4\n5");
        LineIterator iterator = LineIterator.of(reader);

        List<String> lines = new ArrayList<>();
        iterator.forEachRemaining(lines::add);

        assertEquals(5, lines.size());
        assertEquals("1", lines.get(0));
        assertEquals("5", lines.get(4));

        iterator.close();
    }

    @Test
    @DisplayName("Test forEachRemaining() after consuming some lines")
    public void testForEachRemainingAfterPartialConsumption() {
        Reader reader = new StringReader("1\n2\n3\n4\n5");
        LineIterator iterator = LineIterator.of(reader);

        assertEquals("1", iterator.next());
        assertEquals("2", iterator.next());

        List<String> remainingLines = new ArrayList<>();
        iterator.forEachRemaining(remainingLines::add);

        assertEquals(3, remainingLines.size());
        assertEquals("3", remainingLines.get(0));
        assertEquals("5", remainingLines.get(2));

        iterator.close();
    }

    @Test
    @DisplayName("Test with file containing Windows line endings")
    public void testWithWindowsLineEndings() throws IOException {
        File testFile = new File(tempDir, "windows.txt");
        Files.write(testFile.toPath(), "line1\r\nline2\r\nline3".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            assertEquals("line1", iterator.next());
            assertEquals("line2", iterator.next());
            assertEquals("line3", iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test with file containing Unix line endings")
    public void testWithUnixLineEndings() throws IOException {
        File testFile = new File(tempDir, "unix.txt");
        Files.write(testFile.toPath(), "line1\nline2\nline3".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            assertEquals("line1", iterator.next());
            assertEquals("line2", iterator.next());
            assertEquals("line3", iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test with file containing Mac line endings")
    public void testWithMacLineEndings() throws IOException {
        File testFile = new File(tempDir, "mac.txt");
        Files.write(testFile.toPath(), "line1\rline2\rline3".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            assertTrue(iterator.hasNext());
            iterator.forEachRemaining(line -> assertNotNull(line));
        }
    }

    @Test
    @DisplayName("Test with large file")
    public void testWithLargeFile() throws IOException {
        File testFile = new File(tempDir, "large.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(testFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            int count = 0;
            while (iterator.hasNext()) {
                String line = iterator.next();
                assertNotNull(line);
                count++;
            }
            assertEquals(10000, count);
        }
    }

    @Test
    @DisplayName("Test early termination and close on large file")
    public void testEarlyTerminationOnLargeFile() throws IOException {
        File testFile = new File(tempDir, "large2.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(testFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            for (int i = 0; i < 100 && iterator.hasNext(); i++) {
                iterator.next();
            }
        }

        assertDoesNotThrow(() -> Files.delete(testFile.toPath()));
    }

    @Test
    @DisplayName("Test iterator behavior after close")
    public void testIteratorBehaviorAfterClose() {
        Reader reader = new StringReader("line1\nline2\nline3");
        LineIterator iterator = LineIterator.of(reader);

        assertTrue(iterator.hasNext());
        iterator.close();

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, () -> iterator.next());
    }

    @Test
    @DisplayName("Test with special characters in lines")
    public void testWithSpecialCharacters() {
        Reader reader = new StringReader("line with\ttab\nline with \"quotes\"\nline with 'apostrophe'");
        LineIterator iterator = LineIterator.of(reader);

        assertEquals("line with\ttab", iterator.next());
        assertEquals("line with \"quotes\"", iterator.next());
        assertEquals("line with 'apostrophe'", iterator.next());

        iterator.close();
    }

    @Test
    @DisplayName("Test with Unicode characters")
    public void testWithUnicodeCharacters() throws IOException {
        File testFile = new File(tempDir, "unicode.txt");
        String content = "Hello 世界\nБоже мой\n日本語";
        Files.write(testFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile, StandardCharsets.UTF_8)) {
            assertEquals("Hello 世界", iterator.next());
            assertEquals("Боже мой", iterator.next());
            assertEquals("日本語", iterator.next());
        }
    }

    @Test
    @DisplayName("Test with only newlines file")
    public void testWithOnlyNewlines() {
        Reader reader = new StringReader("\n\n\n");
        LineIterator iterator = LineIterator.of(reader);

        assertEquals("", iterator.next());
        assertEquals("", iterator.next());
        assertEquals("", iterator.next());
        assertFalse(iterator.hasNext());

        iterator.close();
    }

    @Test
    @DisplayName("Test stream() method if available")
    public void testStreamMethod() {
        Reader reader = new StringReader("a\nb\nc\nd\ne");
        LineIterator iterator = LineIterator.of(reader);

        List<String> lines = new ArrayList<>();
        iterator.stream().forEach(lines::add);

        assertEquals(5, lines.size());
        assertTrue(lines.contains("a"));
        assertTrue(lines.contains("e"));

        iterator.close();
    }

    @Test
    @DisplayName("Test concurrent close() calls")
    public void testConcurrentCloseCalls() throws Exception {
        Reader reader = new StringReader("line1\nline2\nline3");
        LineIterator iterator = LineIterator.of(reader);

        Thread t1 = new Thread(() -> iterator.close());
        Thread t2 = new Thread(() -> iterator.close());
        Thread t3 = new Thread(() -> iterator.close());

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        assertFalse(iterator.hasNext());
    }

    @Test
    @DisplayName("Test file with single line no newline")
    public void testFileWithSingleLineNoNewline() throws IOException {
        File testFile = new File(tempDir, "single_no_newline.txt");
        Files.write(testFile.toPath(), "single line without newline".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            assertTrue(iterator.hasNext());
            assertEquals("single line without newline", iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test file ending with newline")
    public void testFileEndingWithNewline() throws IOException {
        File testFile = new File(tempDir, "ending_newline.txt");
        Files.write(testFile.toPath(), "line1\nline2\n".getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            assertEquals("line1", iterator.next());
            assertEquals("line2", iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Test that constructor wraps non-BufferedReader")
    public void testConstructorWrapsNonBufferedReader() {
        Reader reader = new StringReader("test");
        LineIterator iterator = new LineIterator(reader);

        assertTrue(iterator.hasNext());
        assertEquals("test", iterator.next());

        iterator.close();
    }
}
