package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SnappyInputStreamTest extends TestBase {

    @Test
    public void testNullInputRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new SnappyInputStream(null));
    }

    private static byte[] compress(final byte[] raw) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(baos)) {
            out.write(raw);
        }
        return baos.toByteArray();
    }

    @Test
    public void testRoundTripReadAll() throws IOException {
        final byte[] raw = "Hello, Snappy compression!".getBytes();
        final byte[] compressed = compress(raw);

        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            final byte[] buf = new byte[raw.length + 16];
            int total = 0;
            int n;
            while ((n = in.read(buf, total, buf.length - total)) != -1) {
                total += n;
            }
            final byte[] result = new byte[total];
            System.arraycopy(buf, 0, result, 0, total);
            assertArrayEquals(raw, result);
        }
    }

    @Test
    public void testRoundTripReadSingleByte() throws IOException {
        final byte[] raw = { 10, 20, 30, 40, 50 };
        final byte[] compressed = compress(raw);

        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            for (final byte b : raw) {
                assertEquals(b & 0xFF, in.read());
            }
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testReadByteArrayFull() throws IOException {
        final byte[] raw = new byte[300];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) i;
        }
        final byte[] compressed = compress(raw);

        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            final byte[] dst = new byte[raw.length];
            int off = 0;
            int n;
            while (off < dst.length && (n = in.read(dst, off, dst.length - off)) != -1) {
                off += n;
            }
            assertEquals(raw.length, off);
            assertArrayEquals(raw, dst);
        }
    }

    @Test
    public void testReadOutOfBoundsThrows() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            final byte[] buf = new byte[4];
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(buf, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(buf, 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(buf, 2, 5));
        }
    }

    @Test
    public void testSkipNegativeThrows() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            assertThrows(IllegalArgumentException.class, () -> in.skip(-1));
        }
    }

    @Test
    public void testSkipZeroAllowed() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            assertEquals(0L, in.skip(0));
        }
    }

    @Test
    public void testAvailable() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            assertTrue(in.available() >= 0);
        }
    }

    @Test
    public void testMarkSupported() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            assertNotNull(Boolean.valueOf(in.markSupported()));
        }
    }

    @Test
    public void testEmptyData() throws IOException {
        // SnappyOutputStream still emits a header for zero-byte input, so the round-trip works
        final byte[] compressed = compress(new byte[0]);
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testTrulyEmptyStreamConstructorThrows() {
        // org.xerial.snappy.SnappyInputStream eagerly reads its header in the constructor.
        // A zero-byte underlying stream therefore throws IOException at construction time.
        assertThrows(IOException.class, () -> new SnappyInputStream(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        assertDoesNotThrow(() -> {
            final byte[] compressed = compress(new byte[] { 1, 2, 3 });
            final SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed));
            in.close();
            in.close(); // must not throw
        });
    }

    @Test
    public void testCloseDelegatesOnlyOnceAndReadAfterCloseThrowsIOException() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        final int[] closeCalls = { 0 };
        final ByteArrayInputStream source = new ByteArrayInputStream(compressed) {
            @Override
            public void close() {
                closeCalls[0]++;
            }
        };
        final SnappyInputStream in = new SnappyInputStream(source);

        in.close();
        in.close();

        assertEquals(1, closeCalls[0]);
        assertThrows(IOException.class, in::read);
        assertThrows(IOException.class, in::available);
    }

    @Test
    public void testNeitherMarkNorMarkSupportedChecksForAClosedStreamButResetDoes() throws IOException {
        // Doc pin: neither mark(int) nor markSupported() can report an IOException, so those two are the
        // methods that skip ensureOpen(). After close() mark is a silent no-op and markSupported() still
        // answers, while reset() and the read-side methods throw.
        final SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compress("hi".getBytes())));
        in.close();

        assertDoesNotThrow(() -> in.mark(1));
        assertDoesNotThrow(() -> assertFalse(in.markSupported()));

        assertThrows(IOException.class, in::reset);
        assertThrows(IOException.class, in::read);
        assertThrows(IOException.class, in::available);
        assertThrows(IOException.class, () -> in.skip(1));
    }

    @Test
    public void testLargeData() throws IOException {
        final byte[] raw = new byte[50_000];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) (i % 31);
        }
        final byte[] compressed = compress(raw);
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            assertArrayEquals(raw, baos.toByteArray());
        }
    }
}
