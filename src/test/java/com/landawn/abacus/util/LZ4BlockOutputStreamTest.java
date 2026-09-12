package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LZ4BlockOutputStreamTest extends TestBase {

    @Test
    public void testNullOutputRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new LZ4BlockOutputStream(null));
    }

    private static byte[] decompress(final byte[] compressed) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
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
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos)) {
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
        final byte[] raw = "Compress me with LZ4!".getBytes();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos)) {
            out.write(raw);
        }
        assertArrayEquals(raw, decompress(baos.toByteArray()));
    }

    @Test
    public void testWriteByteArrayWithOffsetRoundTrip() throws IOException {
        final byte[] raw = "ABCDEFGHIJKLMNOP".getBytes();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos)) {
            out.write(raw, 4, 8); // "EFGHIJKL"
        }
        assertArrayEquals("EFGHIJKL".getBytes(), decompress(baos.toByteArray()));
    }

    @Test
    public void testCustomBlockSize() throws IOException {
        final byte[] raw = new byte[1024];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) (i % 251);
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Use a valid 64KB custom block size.
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos, 64 * 1024)) {
            out.write(raw);
        }
        assertArrayEquals(raw, decompress(baos.toByteArray()));
    }

    @Test
    public void testFinishThenCloseUnderlyingNotPropagated() throws IOException {
        final byte[] raw = { 1, 2, 3, 4, 5 };
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos);
        out.write(raw);
        out.finish();
        out.close();
        assertArrayEquals(raw, decompress(baos.toByteArray()));
    }

    @Test
    public void testFlush() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos)) {
            out.write(new byte[] { 7, 7, 7 });
            out.flush();
            // After flush, more data can still be written
            out.write(new byte[] { 8, 8, 8 });
        }
        assertArrayEquals(new byte[] { 7, 7, 7, 8, 8, 8 }, decompress(baos.toByteArray()));
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos);
        out.write(42);
        out.close();
        out.close(); // must not throw
        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void testWriteAfterCloseThrows() throws IOException {
        // The underlying jpountz LZ4BlockOutputStream throws IllegalStateException
        // (not IOException) when writing after close/finish. This deviates from the
        // OutputStream convention but is the upstream library's behavior.
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos);
        out.close();
        assertThrows(IllegalStateException.class, () -> out.write(1));
    }

    @Test
    public void testWriteOutOfBoundsThrows() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos)) {
            final byte[] buf = new byte[4];
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(buf, -1, 1));
            // Updated deliberately: a negative length used to reach net.jpountz's SafeUtils.checkLength and
            // surface as IllegalArgumentException. write(byte[], int, int) now validates the range itself, as
            // OutputStream specifies and as BrotliInputStream/SnappyInputStream/SnappyOutputStream/
            // LZ4BlockInputStream already did, so it is an IndexOutOfBoundsException.
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(buf, 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(buf, 2, 5));
        }
    }

    @Test
    public void reviewFixes20260908_writeReportsABadRangeTheSameWayAsEveryOtherWrapperInThisPackage() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos)) {
            final byte[] buf = new byte[4];

            // A negative length is an IndexOutOfBoundsException, not the delegate's IllegalArgumentException.
            final IndexOutOfBoundsException e = assertThrows(IndexOutOfBoundsException.class, () -> out.write(buf, 0, -1));
            assertTrue(e.getMessage().contains("len: -1"), e.getMessage());

            // ... and off is validated even for a zero-length write, exactly like LZ4BlockInputStream.read.
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(buf, 5, 0));

            // A legal zero-length write is still a no-op, and normal writes still round-trip.
            out.write(buf, 4, 0);
            out.write("ok".getBytes(), 0, 2);
        }
        assertArrayEquals("ok".getBytes(), decompress(baos.toByteArray()));
    }

    @Test
    public void testLargeData() throws IOException {
        final byte[] raw = new byte[100_000];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) (i % 17);
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos)) {
            out.write(raw);
        }
        assertArrayEquals(raw, decompress(baos.toByteArray()));
    }

    @Test
    public void testCloseClosesUnderlyingWhenFinishingFails() throws IOException {
        final IOException writeFailure = new IOException("write failed");
        final IOException closeFailure = new IOException("close failed");
        final boolean[] closed = { false };
        final java.io.OutputStream underlying = new java.io.OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                throw writeFailure;
            }

            @Override
            public void close() throws IOException {
                closed[0] = true;
                throw closeFailure;
            }
        };
        final LZ4BlockOutputStream out = new LZ4BlockOutputStream(underlying);
        out.write(1); // buffered; the underlying failure occurs while close() finishes the block

        final IOException thrown = assertThrows(IOException.class, out::close);

        assertTrue(closed[0]);
        assertEquals("write failed", thrown.getMessage());
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("close failed", thrown.getSuppressed()[0].getMessage());
    }

    @Test
    public void testCloseHasNoEffectOnlyAfterASuccessfulClose() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos);
        out.write(new byte[] { 1, 2, 3 });
        out.close();
        final int sizeAfterFirstClose = baos.size();

        out.close(); // an already successfully closed stream: no effect
        assertEquals(sizeAfterFirstClose, baos.size());
        assertThrows(IllegalStateException.class, () -> out.write(7));

        final java.io.OutputStream failing = new java.io.OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                throw new IOException("boom");
            }
        };
        final LZ4BlockOutputStream broken = new LZ4BlockOutputStream(failing);

        // The delegate sets finished = true only AFTER the end marker has been written, so a close that failed
        // leaves the stream unfinished: the retry repeats the finish, and write is still accepted. This sink's
        // close() is OutputStream's inherited no-op, which is the only shape that fails again IDENTICALLY.
        assertEquals("boom", assertThrows(IOException.class, broken::close).getMessage());
        assertEquals("boom", assertThrows(IOException.class, broken::close).getMessage());
        assertDoesNotThrow(() -> broken.write(7));

        // A realistic sink rejects writes once closed, and close() closed it after the FIRST failure, so the
        // retry reports that sink's closed-stream error instead of the original one.
        final java.io.OutputStream latching = new java.io.OutputStream() {
            private boolean closed;

            @Override
            public void write(final int b) throws IOException {
                throw new IOException(closed ? "Stream Closed" : "boom");
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                throw new IOException(closed ? "Stream Closed" : "boom");
            }

            @Override
            public void close() {
                closed = true;
            }
        };
        final LZ4BlockOutputStream realistic = new LZ4BlockOutputStream(latching);

        assertEquals("boom", assertThrows(IOException.class, realistic::close).getMessage());
        assertEquals("Stream Closed", assertThrows(IOException.class, realistic::close).getMessage());
        assertDoesNotThrow(() -> realistic.write(7));
    }
}
