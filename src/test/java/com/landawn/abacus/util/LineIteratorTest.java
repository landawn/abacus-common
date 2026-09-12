package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

public class LineIteratorTest extends TestBase {

    @TempDir
    File tempDir;

    @Test
    public void testConstructor() {
        Reader reader = new StringReader("line1\nline2\nline3");
        LineIterator iterator = new LineIterator(reader);
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals("line1", iterator.next());
        assertEquals("line2", iterator.next());
        assertEquals("line3", iterator.next());
        assertFalse(iterator.hasNext());
        iterator.close();

        BufferedReader br = new BufferedReader(new StringReader("line1\nline2"));
        LineIterator buffered = new LineIterator(br);
        assertEquals("line1", buffered.next());
        assertEquals("line2", buffered.next());
        assertFalse(buffered.hasNext());
        buffered.close();

        assertThrows(IllegalArgumentException.class, () -> new LineIterator(null));
    }

    @Test
    public void testOfFile() throws IOException {
        File testFile = new File(tempDir, "test.txt");
        Files.write(testFile.toPath(), "line1\nline2\nline3".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(testFile)) {
            assertEquals("line1", iterator.next());
            assertEquals("line2", iterator.next());
            assertEquals("line3", iterator.next());
            assertFalse(iterator.hasNext());
        }

        File empty = new File(tempDir, "empty.txt");
        Files.write(empty.toPath(), "".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(empty)) {
            assertFalse(iterator.hasNext());
        }

        File utf8 = new File(tempDir, "utf8.txt");
        Files.write(utf8.toPath(), "Hello\nWorld\nTest".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(utf8, StandardCharsets.UTF_8)) {
            assertEquals(List.of("Hello", "World", "Test"), collect(iterator));
        }

        File single = new File(tempDir, "single_no_newline.txt");
        Files.write(single.toPath(), "single line without newline".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(single)) {
            assertEquals("single line without newline", iterator.next());
            assertFalse(iterator.hasNext());
        }

        File endingNewline = new File(tempDir, "ending_newline.txt");
        Files.write(endingNewline.toPath(), "line1\nline2\n".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(endingNewline)) {
            assertEquals("line1", iterator.next());
            assertEquals("line2", iterator.next());
            assertFalse(iterator.hasNext());
        }

        assertThrows(UncheckedIOException.class, () -> LineIterator.of(new File(tempDir, "nonexistent.txt")));
        assertThrows(IllegalArgumentException.class, () -> LineIterator.of((File) null));
        assertThrows(IllegalArgumentException.class, () -> LineIterator.of((File) null, StandardCharsets.UTF_8));
    }

    @Test
    public void testOfInputStream() {
        InputStream inputStream = new ByteArrayInputStream("line1\nline2\nline3".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(inputStream)) {
            assertEquals(List.of("line1", "line2", "line3"), collect(iterator));
        }

        try (LineIterator iterator = LineIterator.of(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)))) {
            assertFalse(iterator.hasNext());
        }

        try (LineIterator iterator = LineIterator.of(new ByteArrayInputStream("First Line\nSecond Line\nThird Line".getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8)) {
            assertEquals("First Line", iterator.next());
            assertEquals("Second Line", iterator.next());
            assertEquals("Third Line", iterator.next());
            assertFalse(iterator.hasNext());
        }

        assertThrows(IllegalArgumentException.class, () -> LineIterator.of((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> LineIterator.of((InputStream) null, StandardCharsets.UTF_8));
    }

    @Test
    public void testOfReader() {
        try (LineIterator iterator = LineIterator.of(new StringReader("line1\nline2\nline3"))) {
            assertEquals(List.of("line1", "line2", "line3"), collect(iterator));
        }
        try (LineIterator iterator = LineIterator.of(new StringReader(""))) {
            assertFalse(iterator.hasNext());
        }
        assertThrows(IllegalArgumentException.class, () -> LineIterator.of((Reader) null));
    }

    @Test
    public void testHasNextAndNext() {
        LineIterator iterator = LineIterator.of(new StringReader("first\nsecond\nthird"));
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals("first", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("second", iterator.next());
        assertEquals("third", iterator.next());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
        iterator.close();

        LineIterator empty = LineIterator.of(new StringReader(""));
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::next);
        empty.close();

        LineIterator emptyLines = new LineIterator(new StringReader("line1\n\nline3"));
        assertEquals("line1", emptyLines.next());
        assertEquals("", emptyLines.next());
        assertEquals("line3", emptyLines.next());
        emptyLines.close();
    }

    @Test
    public void testForeachRemaining() {
        LineIterator iterator = LineIterator.of(new StringReader("1\n2\n3\n4\n5"));
        List<String> lines = new ArrayList<>();
        iterator.forEachRemaining(lines::add);
        assertEquals(List.of("1", "2", "3", "4", "5"), lines);
        iterator.close();

        LineIterator partial = LineIterator.of(new StringReader("1\n2\n3\n4\n5"));
        assertEquals("1", partial.next());
        assertEquals("2", partial.next());
        List<String> remaining = new ArrayList<>();
        partial.forEachRemaining(remaining::add);
        assertEquals(List.of("3", "4", "5"), remaining);
        partial.close();
    }

    @Test
    public void testStream() {
        LineIterator iterator = LineIterator.of(new StringReader("a\nb\nc\nd\ne"));
        List<String> lines = new ArrayList<>();
        iterator.stream().forEach(lines::add);
        assertEquals(List.of("a", "b", "c", "d", "e"), lines);
        iterator.close();
    }

    @Test
    public void testClose() throws Exception {
        LineIterator iterator = LineIterator.of(new StringReader("line1\nline2\nline3"));
        assertTrue(iterator.hasNext());
        iterator.close();
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);

        LineIterator multiple = LineIterator.of(new StringReader("line1"));
        assertDoesNotThrow(() -> {
            multiple.close();
            multiple.close();
            multiple.close();
        });

        assertDoesNotThrow(() -> {
            try (LineIterator it = LineIterator.of(new StringReader("line1\nline2"))) {
                assertTrue(it.hasNext());
                it.next();
            }
        });

        LineIterator concurrent = LineIterator.of(new StringReader("line1\nline2\nline3"));
        Thread t1 = new Thread(concurrent::close);
        Thread t2 = new Thread(concurrent::close);
        Thread t3 = new Thread(concurrent::close);
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();
        assertFalse(concurrent.hasNext());
    }

    @Test
    public void testLineEndingsAndSpecialContent() throws IOException {
        LineIterator special = LineIterator.of(new StringReader("line with\ttab\nline with \"quotes\"\nline with 'apostrophe'"));
        assertEquals("line with\ttab", special.next());
        assertEquals("line with \"quotes\"", special.next());
        assertEquals("line with 'apostrophe'", special.next());
        special.close();

        LineIterator onlyNewlines = LineIterator.of(new StringReader("\n\n\n"));
        assertEquals("", onlyNewlines.next());
        assertEquals("", onlyNewlines.next());
        assertEquals("", onlyNewlines.next());
        assertFalse(onlyNewlines.hasNext());
        onlyNewlines.close();

        File windows = new File(tempDir, "windows.txt");
        Files.write(windows.toPath(), "line1\r\nline2\r\nline3".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(windows)) {
            assertEquals("line1", iterator.next());
            assertEquals("line2", iterator.next());
            assertEquals("line3", iterator.next());
            assertFalse(iterator.hasNext());
        }

        File unix = new File(tempDir, "unix.txt");
        Files.write(unix.toPath(), "line1\nline2\nline3".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(unix)) {
            assertEquals("line1", iterator.next());
            assertEquals("line2", iterator.next());
            assertEquals("line3", iterator.next());
        }

        File mac = new File(tempDir, "mac.txt");
        Files.write(mac.toPath(), "line1\rline2\rline3".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(mac)) {
            assertTrue(iterator.hasNext());
            iterator.forEachRemaining(line -> assertNotNull(line));
        }

        File unicode = new File(tempDir, "unicode.txt");
        Files.write(unicode.toPath(), "Hello 世界\nБоже мой\n日本語".getBytes(StandardCharsets.UTF_8));
        try (LineIterator iterator = LineIterator.of(unicode, StandardCharsets.UTF_8)) {
            assertEquals("Hello 世界", iterator.next());
            assertEquals("Боже мой", iterator.next());
            assertEquals("日本語", iterator.next());
        }
    }

    @Test
    public void testLargeFile() throws IOException {
        File testFile = new File(tempDir, "large.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(testFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(testFile)) {
            int count = 0;
            while (iterator.hasNext()) {
                assertNotNull(iterator.next());
                count++;
            }
            assertEquals(10000, count);
        }

        try (LineIterator iterator = LineIterator.of(testFile)) {
            for (int i = 0; i < 100 && iterator.hasNext(); i++) {
                iterator.next();
            }
        }
        assertDoesNotThrow(() -> Files.delete(testFile.toPath()));
    }

    private static List<String> collect(LineIterator iterator) {
        List<String> lines = new ArrayList<>();
        while (iterator.hasNext()) {
            lines.add(iterator.next());
        }
        return lines;
    }
}
