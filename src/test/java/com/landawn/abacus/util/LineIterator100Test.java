package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LineIterator100Test extends TestBase {

    @Test
    public void testConstructorWithReader() {
        StringReader reader = new StringReader("line1\nline2\nline3");
        LineIterator iterator = new LineIterator(reader);

        Assertions.assertTrue(iterator.hasNext());
        Assertions.assertEquals("line1", iterator.next());
    }

    @Test
    public void testConstructorWithNullReader() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new LineIterator(null);
        });
    }

    @Test
    public void testOfFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("line1\nline2\nline3");
        }

        try (LineIterator iterator = LineIterator.of(tempFile)) {
            Assertions.assertTrue(iterator.hasNext());
            Assertions.assertEquals("line1", iterator.next());
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testOfFileWithCharset() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        try (FileWriter writer = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
            writer.write("line1\nline2\nline3");
        }

        try (LineIterator iterator = LineIterator.of(tempFile, StandardCharsets.UTF_8)) {
            Assertions.assertTrue(iterator.hasNext());
            Assertions.assertEquals("line1", iterator.next());
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testOfInputStream() {
        String content = "line1\nline2\nline3";
        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(is)) {
            Assertions.assertTrue(iterator.hasNext());
            Assertions.assertEquals("line1", iterator.next());
        }
    }

    @Test
    public void testOfInputStreamWithCharset() {
        String content = "line1\nline2\nline3";
        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        try (LineIterator iterator = LineIterator.of(is, StandardCharsets.UTF_8)) {
            Assertions.assertTrue(iterator.hasNext());
            Assertions.assertEquals("line1", iterator.next());
        }
    }

    @Test
    public void testOfReader() {
        StringReader reader = new StringReader("line1\nline2\nline3");

        try (LineIterator iterator = LineIterator.of(reader)) {
            Assertions.assertTrue(iterator.hasNext());
            Assertions.assertEquals("line1", iterator.next());
        }
    }

    @Test
    public void testHasNext() {
        StringReader reader = new StringReader("line1\nline2");
        LineIterator iterator = new LineIterator(reader);

        Assertions.assertTrue(iterator.hasNext());
        iterator.next();
        Assertions.assertTrue(iterator.hasNext());
        iterator.next();
        Assertions.assertFalse(iterator.hasNext());

        iterator.close();
    }

    @Test
    public void testNext() {
        StringReader reader = new StringReader("line1\nline2\nline3");
        LineIterator iterator = new LineIterator(reader);

        Assertions.assertEquals("line1", iterator.next());
        Assertions.assertEquals("line2", iterator.next());
        Assertions.assertEquals("line3", iterator.next());

        iterator.close();
    }

    @Test
    public void testNextNoMoreLines() {
        StringReader reader = new StringReader("line1");
        LineIterator iterator = new LineIterator(reader);

        iterator.next();

        Assertions.assertThrows(NoSuchElementException.class, () -> {
            iterator.next();
        });

        iterator.close();
    }

    @Test
    public void testClose() {
        StringReader reader = new StringReader("line1\nline2");
        LineIterator iterator = new LineIterator(reader);

        iterator.next();
        iterator.close();

        // After close, hasNext should return false
        Assertions.assertFalse(iterator.hasNext());

        // Multiple closes should be safe
        iterator.close();
    }

    @Test
    public void testEmptyLines() {
        StringReader reader = new StringReader("\n\nline3\n\n");
        LineIterator iterator = new LineIterator(reader);

        Assertions.assertEquals("", iterator.next());
        Assertions.assertEquals("", iterator.next());
        Assertions.assertEquals("line3", iterator.next());
        Assertions.assertEquals("", iterator.next());
        Assertions.assertFalse(iterator.hasNext());

        iterator.close();
    }
}