package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.landawn.abacus.util.ImmutableList;

/** Files readwrite contracts. */
@SuppressWarnings("resource")
public class FilesReadWriteTest extends FilesTestSupport {

    @Test
    public void testAsByteSink() throws IOException {
        File file = file("bytes.bin");
        Path path = path("bytes-path.bin");
        byte[] data = { 1, 2, 3, 4 };

        Files.asByteSink(file).write(data);
        Files.asByteSink(path).write(data);
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(file.toPath()));
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(path));

        Files.asByteSink(file, FileWriteMode.APPEND).write(new byte[] { 5 });
        Files.asByteSink(path, StandardOpenOption.APPEND, StandardOpenOption.WRITE).write(new byte[] { 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, java.nio.file.Files.readAllBytes(file.toPath()));
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, java.nio.file.Files.readAllBytes(path));

        Files.asByteSink(file).write(new byte[] { 9 });
        assertArrayEquals(new byte[] { 9 }, java.nio.file.Files.readAllBytes(file.toPath()));
    }

    @Test
    public void testAsByteSource() throws IOException {
        ByteSource fromFile = Files.asByteSource(testFile);
        ByteSource fromPath = Files.asByteSource(testPath, StandardOpenOption.READ);
        ByteSource fromPathNoOptions = Files.asByteSource(testPath);

        byte[] expected = java.nio.file.Files.readAllBytes(testPath);
        assertArrayEquals(expected, fromFile.read());
        assertArrayEquals(expected, fromPath.read());
        assertArrayEquals(expected, fromPathNoOptions.read());
        assertEquals(expected.length, fromFile.size());

        File empty = file("empty.bin");
        Files.write(new byte[0], empty);
        assertEquals(0, Files.asByteSource(empty).size());
        assertArrayEquals(new byte[0], Files.asByteSource(empty).read());
    }

    @Test
    public void testAsCharSink() throws IOException {
        File file = file("chars.txt");
        Path path = path("chars-path.txt");

        Files.asCharSink(file, StandardCharsets.UTF_8).write("Hello");
        Files.asCharSink(path, StandardCharsets.UTF_8).write("Hello");
        assertEquals("Hello", Files.readString(file));
        assertEquals("Hello", new String(java.nio.file.Files.readAllBytes(path), StandardCharsets.UTF_8));

        Files.asCharSink(file, StandardCharsets.UTF_8, FileWriteMode.APPEND).write(" World");
        Files.asCharSink(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.WRITE).write(" World");
        assertEquals("Hello World", Files.readString(file));
        assertEquals("Hello World", new String(java.nio.file.Files.readAllBytes(path), StandardCharsets.UTF_8));

        Files.asCharSink(file, StandardCharsets.UTF_8).writeLines(Arrays.asList("A", "B"));
        String lines = Files.readString(file);
        assertTrue(lines.contains("A"));
        assertTrue(lines.contains("B"));
    }

    @Test
    public void testAsCharSource() throws IOException {
        CharSource fromFile = Files.asCharSource(testFile, StandardCharsets.UTF_8);
        CharSource fromPath = Files.asCharSource(testPath, StandardCharsets.UTF_8, StandardOpenOption.READ);
        CharSource fromPathNoOptions = Files.asCharSource(testPath, StandardCharsets.UTF_8);

        assertEquals("Line 1\nLine 2\nLine 3", fromFile.read());
        assertEquals(fromFile.read(), fromPath.read());
        assertEquals(fromFile.read(), fromPathNoOptions.read());
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), fromFile.readLines());

        File latin = writeRawBytes("chars-latin.txt", "áéíóú".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("áéíóú", Files.asCharSource(latin, StandardCharsets.ISO_8859_1).read());

        File empty = file("empty-chars.txt");
        Files.write(new byte[0], empty);
        assertEquals("", Files.asCharSource(empty, StandardCharsets.UTF_8).read());
    }

    @Test
    public void testC369_asCharSourceReadLinesIsGuavaImmutableListAndLenient() throws IOException {
        final File latin1 = writeBytes("latin1b.txt", new byte[] { 'c', 'a', 'f', (byte) 0xE9, '\n' });

        final List<String> lines = Files.asCharSource(latin1, StandardCharsets.UTF_8).readLines();
        assertInstanceOf(com.google.common.collect.ImmutableList.class, lines);
        assertEquals(Arrays.asList("caf\uFFFD"), lines);
        assertThrows(MalformedInputException.class, () -> Files.readAllLines(latin1));
    }

    @Test
    public void testC366_noOptionPathSourceLinesAreStrictButReadIsLenient() throws IOException {
        final Path latin1 = writeBytes("latin1.txt", new byte[] { 'c', 'a', 'f', (byte) 0xE9, '\n', 'x', '\n' }).toPath();

        // no options: lines()/forEachLine() go through java.nio.file.Files.lines and are strict
        assertThrows(UncheckedIOException.class, () -> {
            try (java.util.stream.Stream<String> s = Files.asCharSource(latin1, StandardCharsets.UTF_8).lines()) {
                s.count();
            }
        });
        assertThrows(MalformedInputException.class, () -> Files.asCharSource(latin1, StandardCharsets.UTF_8).forEachLine(l -> {
        }));
        assertThrows(UncheckedIOException.class, () -> {
            try (java.util.stream.Stream<String> s = Files.asByteSource(latin1).asCharSource(StandardCharsets.UTF_8).lines()) {
                s.count();
            }
        });

        // no options: read()/readLines() replace
        assertEquals(Arrays.asList("caf\uFFFD", "x"), Files.asCharSource(latin1, StandardCharsets.UTF_8).readLines());

        // explicit READ: lines()/forEachLine() replace too
        final List<String> viaForEach = new ArrayList<>();
        Files.asCharSource(latin1, StandardCharsets.UTF_8, StandardOpenOption.READ).forEachLine(viaForEach::add);
        assertEquals(Arrays.asList("caf\uFFFD", "x"), viaForEach);
        try (java.util.stream.Stream<String> s = Files.asCharSource(latin1, StandardCharsets.UTF_8, StandardOpenOption.READ).lines()) {
            assertEquals(Arrays.asList("caf\uFFFD", "x"), s.collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testC363_failedFallbackOnWindowsDeletesExistingDestinationAndKeepsSource() throws IOException {
        final File from = writeBytes("mv_locked_from.txt", "NEW".getBytes(StandardCharsets.UTF_8));
        final File to = writeBytes("mv_locked_to.txt", "OLD-precious".getBytes(StandardCharsets.UTF_8));

        // an open stream prevents deleting 'from' on Windows, so the copy+delete fallback fails after the copy
        try (FileInputStream lock = new FileInputStream(from)) {
            assertThrows(IOException.class, () -> Files.move(from, to));
        }

        assertTrue(from.exists());
        assertEquals("NEW", new String(java.nio.file.Files.readAllBytes(from.toPath()), StandardCharsets.UTF_8));
        assertFalse(to.exists(), "the documented failure path deletes the pre-existing destination");
    }

    @Test
    public void testNewReader() throws IOException {
        try (BufferedReader reader = Files.newReader(testFile, StandardCharsets.UTF_8)) {
            assertEquals("Line 1", reader.readLine());
            assertEquals("Line 2", reader.readLine());
            assertEquals("Line 3", reader.readLine());
            assertEquals(null, reader.readLine());
        }

        File empty = file("empty.txt");
        Files.write(new byte[0], empty);
        try (BufferedReader reader = Files.newReader(empty, StandardCharsets.UTF_8)) {
            assertEquals(null, reader.readLine());
        }

        File latin = writeRawBytes("latin.txt", "café".getBytes(StandardCharsets.ISO_8859_1));
        try (BufferedReader reader = Files.newReader(latin, StandardCharsets.ISO_8859_1)) {
            assertEquals("café", reader.readLine());
        }

        assertThrows(FileNotFoundException.class, () -> Files.newReader(file("missing.txt"), StandardCharsets.UTF_8));
        File dir = path("reader-dir").toFile();
        assertTrue(dir.mkdir());
        assertThrows(FileNotFoundException.class, () -> Files.newReader(dir, StandardCharsets.UTF_8));
    }

    @Test
    public void testNewWriter() throws IOException {
        File file = file("writer.txt");
        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Hello");
        }
        assertEquals("Hello", Files.readString(file));

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Replaced");
        }
        assertEquals("Replaced", Files.readString(file));

        File latin = file("writer-latin.txt");
        try (BufferedWriter writer = Files.newWriter(latin, StandardCharsets.ISO_8859_1)) {
            writer.write("café");
        }
        assertEquals("café", Files.readString(latin, StandardCharsets.ISO_8859_1));

        File dir = path("writer-dir").toFile();
        assertTrue(dir.mkdir());
        assertThrows(FileNotFoundException.class, () -> Files.newWriter(dir, StandardCharsets.UTF_8));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testU24_02_jdkBackedReadersRejectStreamNameThatGuavaBackedSiblingsRead() throws IOException {
        final File host = writeBytes("t.txt", "host".getBytes(StandardCharsets.UTF_8));
        final byte[] streamContent = "stream".getBytes(StandardCharsets.UTF_8);
        final File ads = alternateDataStream(host, "s", streamContent);

        // the five JDK-backed readers: the mapped FileNotFoundException with the InvalidPathException as cause (C-361)
        final Executable[] jdkBacked = { () -> Files.readAllBytes(ads), () -> Files.readString(ads), () -> Files.readString(ads, StandardCharsets.UTF_8),
                () -> Files.readAllLines(ads), () -> Files.readAllLines(ads, StandardCharsets.UTF_8) };

        for (final Executable call : jdkBacked) {
            final FileNotFoundException e = assertThrows(FileNotFoundException.class, call);
            assertInstanceOf(InvalidPathException.class, e.getCause());
            assertTrue(e.getMessage().startsWith("Invalid file path: "), e.getMessage());
        }

        // the Guava-backed siblings read the stream: the "readable through them" half of the reworded sentence
        assertArrayEquals(streamContent, Files.toByteArray(ads));
        assertEquals(Arrays.asList("stream"), Files.readLines(ads, StandardCharsets.UTF_8));
        assertArrayEquals(streamContent, Files.asByteSource(ads).read());
    }

    @Test
    public void testRead_MalformedUtf8() throws IOException {
        File bad = writeRawBytes("bad.txt", new byte[] { 'a', (byte) 0xFF, 'b', '\n', 'c' });
        assertEquals(Arrays.asList("a�b", "c"), Files.readLines(bad, StandardCharsets.UTF_8));
        assertThrows(MalformedInputException.class, () -> Files.readAllLines(bad));
        assertThrows(MalformedInputException.class, () -> Files.readAllLines(bad, StandardCharsets.UTF_8));
        assertThrows(MalformedInputException.class, () -> Files.readString(bad));
        assertThrows(MalformedInputException.class, () -> Files.readString(bad, StandardCharsets.UTF_8));

        File latin = writeRawBytes("latin1.txt", "Línea 1\nLínea 2".getBytes(StandardCharsets.ISO_8859_1));
        assertThrows(MalformedInputException.class, () -> Files.readString(latin));
        assertEquals("Línea 1\nLínea 2", Files.readString(latin, StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testReadAllBytes() throws IOException {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        File file = file("all-bytes.bin");
        Files.write(data, file);
        assertArrayEquals(data, Files.readAllBytes(file));

        File empty = file("all-bytes-empty.bin");
        Files.write(new byte[0], empty);
        assertArrayEquals(new byte[0], Files.readAllBytes(empty));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testC361_windowsIllegalCharactersReportedAsFileNotFound() {
        for (final String name : Arrays.asList("a?b.txt", "a*b.txt", "a<b.txt", "a>b.txt", "a|b.txt", "a\"b.txt", "\u65E5?\u672C.txt")) {
            final File bad = new File(tempDir.toFile(), name);

            assertInvalidPath(() -> Files.readAllBytes(bad));
            assertInvalidPath(() -> Files.readString(bad));
            assertInvalidPath(() -> Files.readAllLines(bad, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testReadAllLines() throws IOException {
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), Files.readAllLines(testFile));
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), Files.readAllLines(testFile, StandardCharsets.UTF_8));

        File empty = file("all-lines-empty.txt");
        Files.write(new byte[0], empty);
        assertTrue(Files.readAllLines(empty).isEmpty());

        File blank = writeRawBytes("blank-lines.txt", "A\n\nC".getBytes(StandardCharsets.UTF_8));
        assertEquals(Arrays.asList("A", "", "C"), Files.readAllLines(blank));
    }

    @Test
    public void testReadAllLines_Utf8Bom_IsKeptByBothPaths() throws IOException {
        byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] body = "x\ny".getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(body, 0, all, bom.length, body.length);
        File f = writeRawBytes("bom.txt", all);
        assertEquals(Arrays.asList("\uFEFFx", "y"), Files.readAllLines(f));
        assertEquals(Arrays.asList("\uFEFFx", "y"), Files.readLines(f, StandardCharsets.UTF_8));
        assertEquals("\uFEFFx\ny", Files.readString(f));
    }

    @Test
    public void testReadLines() throws IOException {
        List<String> lines = Files.readLines(testFile, StandardCharsets.UTF_8);
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), lines);
        lines.add("Line 4");
        assertEquals(4, lines.size());

        File empty = file("read-empty.txt");
        Files.write(new byte[0], empty);
        assertTrue(Files.readLines(empty, StandardCharsets.UTF_8).isEmpty());

        File latin = writeRawBytes("read-latin.txt", "Línea 1\nLínea 2".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(Arrays.asList("Línea 1", "Línea 2"), Files.readLines(latin, StandardCharsets.ISO_8859_1));
    }

    // readString

    @Test
    public void testC361_nulNameReportedAsFileNotFoundByAllJdkBackedReaders() {
        final File bad = new File(tempDir.toFile(), "a\u0000b.txt");

        assertInvalidPath(() -> Files.readAllBytes(bad));
        assertInvalidPath(() -> Files.readString(bad));
        assertInvalidPath(() -> Files.readString(bad, StandardCharsets.UTF_8));
        assertInvalidPath(() -> Files.readAllLines(bad));
        assertInvalidPath(() -> Files.readAllLines(bad, StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testReadString() throws IOException {
        assertEquals("Line 1\nLine 2\nLine 3", Files.readString(testFile));
        assertEquals("Line 1\nLine 2\nLine 3", Files.readString(testFile, StandardCharsets.UTF_8));

        File empty = file("read-string-empty.txt");
        Files.write(new byte[0], empty);
        assertEquals("", Files.readString(empty));

        File latin = writeRawBytes("read-string-latin.txt", "Línea".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("Línea", Files.readString(latin, StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testC367_unmappableByteThrowsUnmappableCharacterException() throws IOException {
        Assumptions.assumeTrue(Charset.isSupported("windows-1252"));
        final Charset cp1252 = Charset.forName("windows-1252");
        final File f = writeBytes("cp1252.txt", new byte[] { 'a', (byte) 0x81, '\n' });

        assertThrows(UnmappableCharacterException.class, () -> Files.readString(f, cp1252));
        assertThrows(UnmappableCharacterException.class, () -> Files.readAllLines(f, cp1252));
        // the Guava-backed sibling replaces it
        assertEquals(Arrays.asList("a\uFFFD"), Files.readLines(f, cp1252));
    }

    @Test
    public void testC361_validAndMissingNamesUnchanged() throws IOException {
        final File ok = writeBytes("\u65E5\u672C\u8A9E.txt", "x\ny".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("x\ny".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(ok));
        assertEquals("x\ny", Files.readString(ok));
        assertEquals(Arrays.asList("x", "y"), Files.readAllLines(ok));

        final File empty = writeBytes("empty.txt", new byte[0]);
        assertEquals("", Files.readString(empty, StandardCharsets.UTF_8));
        assertEquals(new ArrayList<>(), Files.readAllLines(empty, StandardCharsets.UTF_8));

        // a missing but valid name still surfaces the JDK's NoSuchFileException
        final File missing = file("missing.txt");
        assertThrows(NoSuchFileException.class, () -> Files.readAllBytes(missing));
        assertThrows(NoSuchFileException.class, () -> Files.readString(missing));
        assertThrows(NoSuchFileException.class, () -> Files.readAllLines(missing));

        assertThrows(NullPointerException.class, () -> Files.readAllBytes(null));
        assertThrows(NullPointerException.class, () -> Files.readString(null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> Files.readAllLines(null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> Files.readString(ok, null));
    }

    @Test
    public void testC361_nulNameMatchesGuavaBackedSiblings() {
        final File bad = new File(tempDir.toFile(), "a\u0000b.txt");

        // the Guava-backed siblings already reported the same name as FileNotFoundException
        assertThrows(FileNotFoundException.class, () -> Files.toByteArray(bad));
        assertThrows(FileNotFoundException.class, () -> Files.readLines(bad, StandardCharsets.UTF_8));
        assertThrows(FileNotFoundException.class, () -> Files.readAllBytes(bad));
        assertThrows(FileNotFoundException.class, () -> Files.readAllLines(bad));
    }

    @Test
    public void testToByteArray() throws IOException {
        byte[] data = "Byte array content".getBytes(StandardCharsets.UTF_8);
        File file = file("bytes.txt");
        Files.write(data, file);
        assertArrayEquals(data, Files.toByteArray(file));

        File empty = file("empty-bytes.txt");
        Files.write(new byte[0], empty);
        assertArrayEquals(new byte[0], Files.toByteArray(empty));
        assertThrows(FileNotFoundException.class, () -> Files.toByteArray(file("missing.bin")));
    }

    @Test
    public void testWrite() throws IOException {
        File file = file("write.bin");
        byte[] data = { 1, 2, 3, 100, -50 };
        Files.write(data, file);
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(file.toPath()));

        Files.write(new byte[] { 4, 5, 6 }, file);
        assertArrayEquals(new byte[] { 4, 5, 6 }, java.nio.file.Files.readAllBytes(file.toPath()));

        Files.write(new byte[0], file);
        assertEquals(0, file.length());

        File nested = new File(tempDir.toFile(), "missing-parent/file.bin");
        assertThrows(FileNotFoundException.class, () -> Files.write("x".getBytes(), nested));
    }

    @Test
    public void testWriteString() throws IOException {
        File file = file("write-string.txt");
        Files.writeString(file, "smile 😀 and 日本 and é");
        assertEquals("smile 😀 and 日本 and é", Files.readString(file));

        Files.writeString(file, "日本", StandardCharsets.ISO_8859_1);
        assertArrayEquals(new byte[] { '?', '?' }, Files.readAllBytes(file));

        assertThrows(NullPointerException.class, () -> Files.writeString(file, null));
        assertThrows(NullPointerException.class, () -> Files.writeString(file, "x", null));
        assertThrows(NullPointerException.class, () -> Files.writeString(null, "x"));
    }

}
