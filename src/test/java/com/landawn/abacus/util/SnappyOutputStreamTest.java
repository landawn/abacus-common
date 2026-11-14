package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        assertSame(writeFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(closeFailure, thrown.getSuppressed()[0]);
    }

    @Test
    public void testClosePreservesFailuresAndClosesExactlyOnce() throws IOException {
        // Cover both constructors and checked/unchecked final-write failures, including self-suppression.
        for (final boolean customBuffer : new boolean[] { false, true }) {
            for (final Throwable writeFailure : new Throwable[] { new IOException("write"), new IllegalStateException("write"), new AssertionError("write") }) {
                for (final int cleanup : new int[] { 0, 1, 2 }) {
                    final Throwable closeFailure = cleanup == 0 ? null : cleanup == 1 ? new IOException("close") : writeFailure;
                    final FailingOutputStream target = new FailingOutputStream(writeFailure, null, closeFailure);
                    final SnappyOutputStream out = customBuffer ? new SnappyOutputStream(target, 1024) : new SnappyOutputStream(target);
                    out.write(1);
                    final int suppressedBefore = writeFailure.getSuppressed().length;

                    assertSame(writeFailure, assertThrows(writeFailure.getClass(), out::close));
                    assertEquals(suppressedBefore + (cleanup == 1 ? 1 : 0), writeFailure.getSuppressed().length);
                    if (cleanup == 1) {
                        assertSame(closeFailure, writeFailure.getSuppressed()[suppressedBefore]);
                    }
                    assertEquals(1, target.closeCount);
                    out.close();
                    assertEquals(1, target.closeCount, "a failed close is not retried");
                    assertThrows(IOException.class, () -> out.write(2));
                    assertThrows(IOException.class, () -> out.write(new byte[] { 2 }));
                    assertThrows(IOException.class, () -> out.write(new byte[] { 2 }, 0, 1));
                    assertThrows(IOException.class, out::flush);
                }
            }
        }
    }

    @Test
    public void testClosePreservesFlushFailureAndSuppressesCloseFailure() throws IOException {
        final IOException flushFailure = new IOException("flush");
        final IllegalStateException closeFailure = new IllegalStateException("close");
        final FailingOutputStream target = new FailingOutputStream(null, flushFailure, closeFailure);
        final SnappyOutputStream out = new SnappyOutputStream(target);
        out.write(1);
        assertSame(flushFailure, assertThrows(IOException.class, out::close));
        assertArrayEquals(new Throwable[] { closeFailure }, flushFailure.getSuppressed());
        assertEquals(1, target.closeCount);
    }

    @Test
    public void testCloseFailureAloneIsNotRetriedOrSuppressed() throws IOException {
        for (final Throwable failure : new Throwable[] { new IOException("close"), new IllegalStateException("close"), new AssertionError("close") }) {
            final FailingOutputStream target = new FailingOutputStream(null, null, failure);
            final SnappyOutputStream out = new SnappyOutputStream(target);
            out.write(1);
            assertSame(failure, assertThrows(failure.getClass(), out::close));
            assertEquals(0, failure.getSuppressed().length);
            out.close();
            assertEquals(1, target.closeCount);
        }
    }

    private static final class FailingOutputStream extends java.io.OutputStream {
        private final Throwable writeFailure;
        private final Throwable flushFailure;
        private final Throwable closeFailure;
        private int closeCount;

        private FailingOutputStream(final Throwable writeFailure, final Throwable flushFailure, final Throwable closeFailure) {
            this.writeFailure = writeFailure;
            this.flushFailure = flushFailure;
            this.closeFailure = closeFailure;
        }

        @Override
        public void write(final int b) throws IOException {
            fail(writeFailure);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            fail(writeFailure);
        }

        @Override
        public void flush() throws IOException {
            fail(flushFailure);
        }

        @Override
        public void close() throws IOException {
            closeCount++;
            fail(closeFailure);
        }

        private static void fail(final Throwable failure) throws IOException {
            if (failure instanceof IOException e) {
                throw e;
            } else if (failure instanceof RuntimeException e) {
                throw e;
            } else if (failure instanceof Error e) {
                throw e;
            }
        }
    }
}
