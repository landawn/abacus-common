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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Tests for the 2026-09-25 verification findings on {@link Files} (U24-01, U24-02, U24-03; ledger C-365, C-361, C-362).
 * Every file-system test works under a JUnit {@code @TempDir}, which JUnit deletes afterwards.
 */
public class FilesReview20260925Test extends TestBase {

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

    /** Reads through java.io, which (unlike the {@code Path} parser) accepts an NTFS alternate-data-stream name. */
    private static byte[] readViaStream(final File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            return in.readAllBytes();
        }
    }

    /**
     * Creates the NTFS alternate data stream {@code <host>:<streamName>} holding {@code content} through java.io, or
     * aborts the test when the file system does not support streams (a FAT/exFAT temp directory, a non-NTFS mount).
     */
    private static File alternateDataStream(final File host, final String streamName, final byte[] content) {
        final File ads = new File(host.getPath() + ":" + streamName);

        try (FileOutputStream out = new FileOutputStream(ads)) {
            out.write(content);
        } catch (final IOException e) {
            Assumptions.abort("NTFS alternate data streams are not supported here: " + e);
        }

        Assumptions.assumeTrue(ads.isFile() && ads.exists(), "alternate data stream not visible through java.io");
        // precondition of the finding: java.io accepts the name, the Path parser does not
        assertThrows(InvalidPathException.class, ads::toPath);

        return ads;
    }

    // ---------------------------------------------------------------- U24-01 (C-365 same-file guard vs. names Path rejects)

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testU24_01_copyFromAlternateDataStreamFallsThroughToGuava() throws IOException {
        final byte[] hostContent = "host-file".getBytes(StandardCharsets.UTF_8);
        final byte[] streamContent = "stream".getBytes(StandardCharsets.UTF_8);
        final File host = writeBytes("t.txt", hostContent);
        final File ads = alternateDataStream(host, "s", streamContent);
        // an EXISTING destination: the guard is reached (from.isFile() && to.exists()) and from.toPath() is attempted
        final File other = writeBytes("other.txt", "old-old-old".getBytes(StandardCharsets.UTF_8));

        Files.copy(ads, other); // r9620 copied the stream; the C-365 guard let InvalidPathException escape here

        assertArrayEquals(streamContent, java.nio.file.Files.readAllBytes(other.toPath()));
        assertArrayEquals(hostContent, java.nio.file.Files.readAllBytes(host.toPath()), "the host file is untouched");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testU24_01_copyOntoExistingAlternateDataStreamFallsThroughToGuava() throws IOException {
        final byte[] hostContent = "host-file".getBytes(StandardCharsets.UTF_8);
        final byte[] newContent = "\u65E5\u672C".getBytes(StandardCharsets.UTF_8);
        final File host = writeBytes("t.txt", hostContent);
        final File ads = alternateDataStream(host, "s", "stream".getBytes(StandardCharsets.UTF_8));
        final File from = writeBytes("from.txt", newContent);

        Files.copy(from, ads); // destination exists: guard reached, to.toPath() rejected, Guava overwrites the stream

        assertArrayEquals(newContent, readViaStream(ads));
        assertArrayEquals(hostContent, java.nio.file.Files.readAllBytes(host.toPath()), "only the stream changed");

        // a stream that does not exist yet never reaches the guard (to.exists() is false) and is created as before
        final File newStream = new File(host.getPath() + ":n");
        assertFalse(newStream.exists());
        Files.copy(from, newStream);
        assertArrayEquals(newContent, readViaStream(newStream));

        // textual equality is still Guava's IllegalArgumentException, even for a stream name
        assertThrows(IllegalArgumentException.class, () -> Files.copy(ads, new File(ads.getPath())));
        assertArrayEquals(newContent, readViaStream(ads));
    }

    @Test
    public void testU24_01_sameFileGuardAndOtherCopyCasesUnchanged() throws IOException {
        // the guard itself survives the restructuring: another spelling of the same file is still rejected, nothing written
        final byte[] content = "precious".getBytes(StandardCharsets.UTF_8);
        final File same = writeBytes("same.txt", content);
        final File alias = new File(tempDir.toFile(), "." + File.separator + "same.txt");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Files.copy(same, alias));
        assertTrue(e.getMessage().contains("same file"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Files.copy(alias, same));
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(same.toPath()));

        // a NUL name never reaches the Path conversion (isFile()/exists() are false) and stays Guava's FileNotFoundException
        final File nul = new File(tempDir.toFile(), "a\u0000b.txt");
        assertThrows(FileNotFoundException.class, () -> Files.copy(nul, same));
        assertThrows(FileNotFoundException.class, () -> Files.copy(same, nul));
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(same.toPath()));

        // a normal copy onto an existing, different file still goes through the guard and copies
        final File other = writeBytes("other.txt", new byte[] { 1, 2, 3 });
        Files.copy(same, other);
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(other.toPath()));

        // a missing destination skips the guard and copies
        final File missing = file("missing.txt");
        Files.copy(same, missing);
        assertArrayEquals(content, java.nio.file.Files.readAllBytes(missing.toPath()));
    }

    // ---------------------------------------------------------------- U24-02 (C-361 @throws wording: doc pin)

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

    // ---------------------------------------------------------------- U24-03 (C-362 map: argument-check order)

    @Test
    public void testU24_03_mapArgumentChecksKeepGuavaOrder() {
        final File missing = file("m_order.dat");

        // a negative size is Guava's own first check (checkArgument before checkNotNull): reported before a null argument
        assertEquals("size (-1) may not be negative",
                assertThrows(IllegalArgumentException.class, () -> Files.map(null, MapMode.READ_WRITE, -1)).getMessage());
        assertEquals("size (-1) may not be negative", assertThrows(IllegalArgumentException.class, () -> Files.map(missing, null, -1)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Files.map(null, null, Long.MIN_VALUE));

        // an oversize was detected by FileChannel.map AFTER Guava's checkNotNull calls at r9620: the hoisted check must
        // not overtake the NullPointerException
        assertThrows(NullPointerException.class, () -> Files.map(null, MapMode.READ_WRITE, Integer.MAX_VALUE + 1L));
        assertThrows(NullPointerException.class, () -> Files.map(missing, null, Integer.MAX_VALUE + 1L));
        assertThrows(NullPointerException.class, () -> Files.map(null, MapMode.READ_ONLY, Long.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> Files.map(null, null, Long.MAX_VALUE));
        assertFalse(missing.exists());

        // a single bad argument: unchanged
        assertThrows(NullPointerException.class, () -> Files.map(null, MapMode.READ_WRITE, 10));
        assertThrows(NullPointerException.class, () -> Files.map(missing, null, 10));
        assertThrows(NullPointerException.class, () -> Files.map(missing, null, 0));
        assertThrows(IllegalArgumentException.class, () -> Files.map(missing, MapMode.READ_WRITE, Integer.MAX_VALUE + 1L));
        assertThrows(IllegalArgumentException.class, () -> Files.map(missing, MapMode.PRIVATE, -1));
        assertFalse(missing.exists(), "no size or null rejection may create the file");
        assertThrows(FileNotFoundException.class, () -> Files.map(missing, MapMode.READ_ONLY, 10));
        assertFalse(missing.exists());
    }
}
