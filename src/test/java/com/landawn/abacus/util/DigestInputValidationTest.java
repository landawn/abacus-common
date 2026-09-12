package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Tag("unit")
public class DigestInputValidationTest {
    @TempDir
    Path directory;

    @Test
    void nullDigestDoesNotReadOrCloseStreamsIncludingEof() throws Exception {
        for (final byte[] data : new byte[][] { {}, { 1, 2, 3 } }) {
            final var input = new ByteArrayInputStream(data);
            assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(null, input));
            assertEquals(data.length, input.available());
        }
        final var untouched = new InputStream() {
            @Override
            public int read() {
                throw new AssertionError("read");
            }

            @Override
            public void close() {
                throw new AssertionError("close");
            }
        };
        assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(null, untouched));
        assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(null, (InputStream) null));
    }

    @Test
    void nullDigestDoesNotOpenFilesOrAdvanceRandomAccessPositions() throws Exception {
        final Path missing = directory.resolve("missing");
        assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(null, missing));
        assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(null, missing.toFile()));
        final Path file = directory.resolve("data");
        Files.write(file, new byte[] { 1, 2, 3 });
        try (final var input = new RandomAccessFile(file.toFile(), "r")) {
            for (final long position : new long[] { 0, 1, 3 }) {
                input.seek(position);
                assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(null, input));
                assertEquals(position, input.getFilePointer());
            }
        }
    }

    @Test
    void validInputsUpdateTheSameDigestWithoutClosingBorrowedResources() throws Exception {
        final byte[] bytes = "\uD83D\uDE00".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final byte[] expected = DigestUtil.sha256(bytes);
        final Path file = directory.resolve("valid");
        Files.write(file, bytes);
        final var digest = DigestUtil.getSha256Digest();
        assertSame(digest, DigestUtil.updateDigest(digest, file));
        assertArrayEquals(expected, digest.digest());
        assertSame(digest, DigestUtil.updateDigest(digest, file.toFile()));
        assertArrayEquals(expected, digest.digest());
        try (final var input = new RandomAccessFile(file.toFile(), "r")) {
            assertSame(digest, DigestUtil.updateDigest(digest, input));
            assertEquals(bytes.length, input.getFilePointer());
            assertArrayEquals(expected, digest.digest());
        }
        assertSame(digest, DigestUtil.updateDigest(digest, new ByteArrayInputStream(bytes)));
        assertArrayEquals(expected, digest.digest());
    }
}
