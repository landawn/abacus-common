package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Tests for the 2026-09-24 review findings on {@link Files} (ledger C-361..C-369).
 * Every file-system test works under a JUnit {@code @TempDir}, which JUnit deletes afterwards.
 */
public class FilesReview20260924Test extends TestBase {

    @TempDir
    Path tempDir;

    private File file(final String name) {
        return tempDir.resolve(name).toFile();
    }

    private File writeBytes(final String name, final byte[] bytes) throws IOException {
        final File f = file(name);
        java.nio.file.Files.write(f.toPath(), bytes);
        return f;
    }

    private static void assertInvalidPath(final Executable call) {
        final FileNotFoundException e = assertThrows(FileNotFoundException.class, call);
        assertInstanceOf(InvalidPathException.class, e.getCause());
        assertTrue(e.getMessage().startsWith("Invalid file path: "), e.getMessage());
    }

    // ---------------------------------------------------------------- C-361

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
    public void testC361_nulNameMatchesGuavaBackedSiblings() {
        final File bad = new File(tempDir.toFile(), "a\u0000b.txt");

        // the Guava-backed siblings already reported the same name as FileNotFoundException
        assertThrows(FileNotFoundException.class, () -> Files.toByteArray(bad));
        assertThrows(FileNotFoundException.class, () -> Files.readLines(bad, StandardCharsets.UTF_8));
        assertThrows(FileNotFoundException.class, () -> Files.readAllBytes(bad));
        assertThrows(FileNotFoundException.class, () -> Files.readAllLines(bad));
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

    // ---------------------------------------------------------------- C-362

    @Test
    public void testC362_mapOversizeRejectedBeforeFileIsCreated() {
        for (final MapMode mode : Arrays.asList(MapMode.READ_WRITE, MapMode.PRIVATE, MapMode.READ_ONLY)) {
            for (final long size : new long[] { Integer.MAX_VALUE + 1L, Long.MAX_VALUE }) {
                final File missing = file("m_big_" + mode + "_" + size + ".dat");

                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Files.map(missing, mode, size));
                assertTrue(e.getMessage().contains(String.valueOf(size)), e.getMessage());
                assertFalse(missing.exists(), mode + " created " + missing);
            }
        }
    }

    @Test
    public void testC362_mapNegativeRejectedBeforeFileIsCreated() {
        for (final long size : new long[] { -1, Long.MIN_VALUE }) {
            final File missing = file("m_neg.dat");

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Files.map(missing, MapMode.READ_WRITE, size));
            assertEquals("size (" + size + ") may not be negative", e.getMessage());
            assertFalse(missing.exists());
        }
    }

    @Test
    public void testC362_mapBoundarySizesStillDelegate() throws IOException {
        // size 0 on a missing file in READ_WRITE mode still creates an empty file
        final File zero = file("m_zero.dat");
        final MappedByteBuffer buffer = Files.map(zero, MapMode.READ_WRITE, 0);
        assertEquals(0, buffer.capacity());
        assertTrue(zero.exists());

        // READ_ONLY on a missing file with a valid size still reports the missing file
        assertThrows(FileNotFoundException.class, () -> Files.map(file("m_ro_missing.dat"), MapMode.READ_ONLY, 10));

        // null file / mode with a valid size still NPE
        assertThrows(NullPointerException.class, () -> Files.map(null, MapMode.READ_WRITE, 10));
        assertThrows(NullPointerException.class, () -> Files.map(zero, null, 10));
    }

    // ---------------------------------------------------------------- C-365

    @Test
    public void testC365_copyOntoDotSpellingOfItselfRejectedAndContentKept() throws IOException {
        final byte[] content = "precious".getBytes(StandardCharsets.UTF_8);
        final File same = writeBytes("same.txt", content);
        final File alias = new File(tempDir.toFile(), "." + File.separator + "same.txt");
        assertFalse(same.equals(alias));

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Files.copy(same, alias));
        assertTrue(e.getMessage().contains("same file"), e.getMessage());
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(same.toPath()));
    }

    @Test
    public void testC365_copyOntoParentDotDotSpellingRejected() throws IOException {
        final byte[] content = "\u65E5\u672C".getBytes(StandardCharsets.UTF_8);
        final File same = writeBytes("\u65E5\u672C.txt", content);
        assertTrue(new File(tempDir.toFile(), "sub").mkdir());
        final File alias = new File(tempDir.toFile(), "sub" + File.separator + ".." + File.separator + "\u65E5\u672C.txt");

        assertThrows(IllegalArgumentException.class, () -> Files.copy(same, alias));
        assertThrows(IllegalArgumentException.class, () -> Files.copy(alias, same));
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(same.toPath()));
    }

    @Test
    public void testC365_copyOntoHardLinkRejected() throws IOException {
        final byte[] content = "linked".getBytes(StandardCharsets.UTF_8);
        final File same = writeBytes("orig.txt", content);
        final File link = file("link.txt");

        try {
            java.nio.file.Files.createLink(link.toPath(), same.toPath());
        } catch (final UnsupportedOperationException | IOException e) {
            Assumptions.abort("hard links not supported here: " + e);
        }

        assertThrows(IllegalArgumentException.class, () -> Files.copy(same, link));
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(same.toPath()));
    }

    @Test
    public void testC365_copyUnaffectedCases() throws IOException {
        final byte[] content = "abc".getBytes(StandardCharsets.UTF_8);
        final File from = writeBytes("from.txt", content);

        // missing destination
        final File to = file("to.txt");
        Files.copy(from, to);
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(to.toPath()));

        // existing, different destination is overwritten
        final File other = writeBytes("other.txt", "old-old-old".getBytes(StandardCharsets.UTF_8));
        Files.copy(from, other);
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(other.toPath()));

        // empty source
        final File empty = writeBytes("empty.txt", new byte[0]);
        Files.copy(empty, other);
        assertEquals(0, other.length());

        // textual equality keeps Guava's IAE; a missing source keeps FileNotFoundException
        assertThrows(IllegalArgumentException.class, () -> Files.copy(from, new File(from.getPath())));
        assertThrows(FileNotFoundException.class, () -> Files.copy(file("nope.txt"), file("nope2.txt")));

        assertThrows(NullPointerException.class, () -> Files.copy((File) null, to));
        assertThrows(NullPointerException.class, () -> Files.copy(from, (File) null));
    }

    // ---------------------------------------------------------------- C-363 (doc pins)

    @Test
    public void testC363_moveReplacesExistingDestination() throws IOException {
        final File from = writeBytes("mv_from_\u65E5.txt", "NEW".getBytes(StandardCharsets.UTF_8));
        final File to = writeBytes("mv_to.txt", "OLD-precious".getBytes(StandardCharsets.UTF_8));

        Files.move(from, to);

        assertFalse(from.exists());
        assertEquals("NEW", new String(java.nio.file.Files.readAllBytes(to.toPath()), StandardCharsets.UTF_8));

        // moving to a missing destination, and an empty file
        final File empty = writeBytes("mv_empty.txt", new byte[0]);
        final File fresh = file("mv_fresh.txt");
        Files.move(empty, fresh);
        assertFalse(empty.exists());
        assertEquals(0, fresh.length());

        assertThrows(IllegalArgumentException.class, () -> Files.move(fresh, new File(fresh.getPath())));
        assertThrows(NullPointerException.class, () -> Files.move(null, fresh));
        assertThrows(NullPointerException.class, () -> Files.move(fresh, null));
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

    // ---------------------------------------------------------------- C-364 (doc pins)

    @Test
    public void testC364_simplifyPathTreatsDriveAndUncAsOrdinarySegments() {
        assertEquals("b", Files.simplifyPath("C:/a/../../b"));
        assertEquals("b", Files.simplifyPath("C:/../b"));
        assertEquals("/host/x", Files.simplifyPath("//host/share/../x"));
        assertEquals("a\\..\\b", Files.simplifyPath("a\\..\\b"));
        assertEquals("/foo/baz", Files.simplifyPath("/foo//bar/../baz/./"));
        assertEquals(".", Files.simplifyPath(""));
        assertThrows(NullPointerException.class, () -> Files.simplifyPath(null));
    }

    @Test
    public void testC364_nameAndExtensionDropThePathAndTrailingSeparator() {
        assertEquals("document", Files.getNameWithoutExtension("/home/user/document.pdf"));
        assertEquals("b", Files.getNameWithoutExtension("a/b.c/"));
        assertEquals("c", Files.getFileExtension("a/b.c/"));
        assertThrows(NullPointerException.class, () -> Files.getNameWithoutExtension((String) null));
        assertThrows(NullPointerException.class, () -> Files.getFileExtension((String) null));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testC364_windowsBackslashIsSeparatorForNameAndExtension() {
        assertEquals("", Files.getFileExtension("a.b\\c"));
        assertEquals("c", Files.getNameWithoutExtension("a.b\\c"));
    }

    // ---------------------------------------------------------------- C-366 (doc pins)

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

    // ---------------------------------------------------------------- C-367 (doc pins)

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

    // ---------------------------------------------------------------- C-368 (doc pins)

    @Test
    public void testC368_pathWithoutFileNameGivesEmptyString() {
        final Path root = tempDir.getRoot();
        assertEquals("", Files.getNameWithoutExtension(root));
        assertEquals("", Files.getFileExtension(root));
        assertEquals("", Files.getNameWithoutExtension(Paths.get("")));
        assertEquals("", Files.getFileExtension(Paths.get("")));
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("dir", "document.pdf")));
        assertEquals("pdf", Files.getFileExtension(Paths.get("dir", "document.pdf")));
    }

    // ---------------------------------------------------------------- C-369 (doc pins)

    @Test
    public void testC369_asCharSourceReadLinesIsGuavaImmutableListAndLenient() throws IOException {
        final File latin1 = writeBytes("latin1b.txt", new byte[] { 'c', 'a', 'f', (byte) 0xE9, '\n' });

        final List<String> lines = Files.asCharSource(latin1, StandardCharsets.UTF_8).readLines();
        assertInstanceOf(com.google.common.collect.ImmutableList.class, lines);
        assertEquals(Arrays.asList("caf\uFFFD"), lines);
        assertThrows(MalformedInputException.class, () -> Files.readAllLines(latin1));
    }
}
