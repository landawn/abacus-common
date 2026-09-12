package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SnappyOutputStreamTest extends TestBase {

    private static byte[] decompress(final byte[] compressed) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            final byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
        }
        return baos.toByteArray();
    }

    @Test
    public void testWriteSingleByteRoundTrip() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos)) {
            for (int i = 0; i < 50; i++) {
                out.write(i);
            }
        }
        final byte[] decompressed = decompress(baos.toByteArray());
        final byte[] expected = new byte[50];
        for (int i = 0; i < 50; i++) {
            expected[i] = (byte) i;
        }
        assertArrayEquals(expected, decompressed);
    }

    @Test
    public void testWriteByteArrayRoundTrip() throws IOException {
        final byte[] raw = "Snappy compresses this!".getBytes();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos)) {
            out.write(raw);
        }
        assertArrayEquals(raw, decompress(baos.toByteArray()));
    }

    @Test
    public void testWriteByteArrayWithOffsetRoundTrip() throws IOException {
        final byte[] raw = "ABCDEFGHIJKLMNOP".getBytes();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos)) {
            out.write(raw, 4, 8); // "EFGHIJKL"
        }
        assertArrayEquals("EFGHIJKL".getBytes(), decompress(baos.toByteArray()));
    }

    @Test
    public void testCustomBufferSize() throws IOException {
        final byte[] raw = new byte[1024];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) (i % 251);
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos, 32 * 1024)) {
            out.write(raw);
        }
        assertArrayEquals(raw, decompress(baos.toByteArray()));
    }

    @Test
    public void testCustomBufferSizeRejectsUnsafeRange() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertThrows(IllegalArgumentException.class, () -> new SnappyOutputStream(baos, 1023));
        assertThrows(IllegalArgumentException.class, () -> new SnappyOutputStream(baos, 536_870_913));
        assertThrows(IllegalArgumentException.class, () -> new SnappyOutputStream(null));
    }

    @Test
    public void testFlush() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos)) {
            out.write(new byte[] { 1, 2, 3 });
            out.flush();
            out.write(new byte[] { 4, 5, 6 });
        }
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, decompress(baos.toByteArray()));
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final SnappyOutputStream out = new SnappyOutputStream(baos);
        out.write(42);
        out.close();
        out.close(); // must not throw
        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void testWriteOutOfBoundsThrows() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos)) {
            final byte[] buf = new byte[4];
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(buf, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(buf, 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(buf, 2, 5));
        }
    }

    @Test
    public void testLargeData() throws IOException {
        final byte[] raw = new byte[100_000];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) (i % 17);
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos)) {
            out.write(raw);
        }
        assertArrayEquals(raw, decompress(baos.toByteArray()));
    }

    @Test
    public void testEmptyWrite() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos)) {
            // Write nothing
        }
        assertArrayEquals(new byte[0], decompress(baos.toByteArray()));
    }

    @Test
    public void testCloseClosesUnderlyingWhenFinalWriteFails() throws IOException {
        final IOException writeFailure = new IOException("write failed");
        final IOException closeFailure = new IOException("close failed");
        final boolean[] closed = { false };
        final java.io.OutputStream underlying = new java.io.OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                throw writeFailure;
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                throw writeFailure;
            }

            @Override
            public void close() throws IOException {
                closed[0] = true;
                throw closeFailure;
            }
        };
        final SnappyOutputStream out = new SnappyOutputStream(underlying);
        out.write(1);

        final IOException thrown = assertThrows(IOException.class, out::close);

        assertTrue(closed[0]);
        assertEquals("write failed", thrown.getMessage());
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("close failed", thrown.getSuppressed()[0].getMessage());
    }
}
