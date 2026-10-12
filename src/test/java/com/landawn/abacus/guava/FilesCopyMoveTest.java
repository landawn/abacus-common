package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Files copymove contracts. */
@SuppressWarnings("resource")
public class FilesCopyMoveTest extends FilesTestSupport {

    // copy

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

    @Test
    public void testCopy() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(testFile, out);
        assertEquals("Line 1\nLine 2\nLine 3", out.toString(StandardCharsets.UTF_8));

        File dest = file("copy-dest.txt");
        Files.copy(testFile, dest);
        assertTrue(Files.equal(testFile, dest));
        Files.writeString(testFile, "overwritten-source");
        Files.copy(testFile, dest);
        assertEquals("overwritten-source", Files.readString(dest));

        File empty = file("copy-empty.txt");
        Files.write(new byte[0], empty);
        ByteArrayOutputStream emptyOut = new ByteArrayOutputStream();
        Files.copy(empty, emptyOut);
        assertEquals(0, emptyOut.size());

        assertThrows(IllegalArgumentException.class, () -> Files.copy(testFile, testFile));
        File missingParent = new File(tempDir.toFile(), "no-such-dir/dest.txt");
        assertThrows(FileNotFoundException.class, () -> Files.copy(testFile, missingParent));
    }

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

    @Test
    public void testMove() throws IOException {
        File source = file("move-src.txt");
        File dest = file("move-dest.txt");
        Files.writeString(source, "relocated");
        Files.move(source, dest);
        assertFalse(source.exists());
        assertEquals("relocated", Files.readString(dest));

        File renamed = file("moved-name.txt");
        Files.move(dest, renamed);
        assertEquals("relocated", Files.readString(renamed));
        assertThrows(IllegalArgumentException.class, () -> Files.move(renamed, renamed));
    }

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

}
