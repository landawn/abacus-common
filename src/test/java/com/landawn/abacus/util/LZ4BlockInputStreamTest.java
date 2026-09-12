package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LZ4BlockInputStreamTest extends TestBase {

    @Test
    public void testNullInputRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new LZ4BlockInputStream(null));
    }

    private static byte[] compress(final byte[] raw) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(baos)) {
            out.write(raw);
        }
        return baos.toByteArray();
    }

    @Test
    public void testRoundTripReadAll() throws IOException {
        final byte[] raw = "Hello, LZ4 compression world!".getBytes();
        final byte[] compressed = compress(raw);

        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
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
        final byte[] raw = { 1, 2, 3, 4, 5 };
        final byte[] compressed = compress(raw);

        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            for (final byte b : raw) {
                assertEquals(b & 0xFF, in.read());
            }
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testReadByteArrayFull() throws IOException {
        final byte[] raw = new byte[256];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) i;
        }
        final byte[] compressed = compress(raw);

        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
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
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            final byte[] buf = new byte[4];
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(buf, -1, 1));
            // FINDING 32: a negative length used to reach the delegate and surface as IllegalArgumentException,
            // contradicting this method's own "@throws IndexOutOfBoundsException if off or len is negative".
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(buf, 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(buf, 2, 5));
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(buf, 5, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(buf, 1, Integer.MAX_VALUE));
            // the buffer is checked first, so a null buffer is an NPE whatever the range is
            assertThrows(NullPointerException.class, () -> in.read(null, 0, 1));
            assertThrows(NullPointerException.class, () -> in.read(null, 0, -1));
            // a valid zero-length request still returns 0 without reading the underlying stream
            assertEquals(0, in.read(buf, 0, 0));
            assertEquals(0, in.read(buf, 4, 0));
        }
    }

    @Test
    public void testSkipNegativeThrows() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            assertThrows(IllegalArgumentException.class, () -> in.skip(-1));
        }
    }

    @Test
    public void testSkipZeroAllowed() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            assertEquals(0L, in.skip(0));
        }
    }

    @Test
    public void testSkipPositive() throws IOException {
        final byte[] raw = new byte[100];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) i;
        }
        final byte[] compressed = compress(raw);
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            final long skipped = in.skip(10);
            assertTrue(skipped >= 0);
            // After skipping, the next read returns a value within the original data range
            final int next = in.read();
            assertTrue(next >= 0 && next <= 0xFF);
        }
    }

    @Test
    public void testAvailable() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            assertTrue(in.available() >= 0);
        }
    }

    @Test
    public void testMarkSupportedAndOps() throws IOException {
        final byte[] compressed = compress(new byte[] { 10, 20, 30 });
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            // Just verify these don't throw unexpected exceptions for a fresh stream
            assertNotNull(Boolean.valueOf(in.markSupported()));
            in.mark(64);
        }
    }

    @Test
    public void testEmptyData() throws IOException {
        final byte[] compressed = compress(new byte[0]);
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        assertDoesNotThrow(() -> {
            final byte[] compressed = compress(new byte[] { 1, 2, 3 });
            final LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed));
            in.close();
            in.close(); // Should not throw
        });
    }

    @Test
    public void testReadByteArrayNullThrows() throws IOException {
        final byte[] compressed = compress(new byte[] { 1, 2, 3 });
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            assertThrows(NullPointerException.class, () -> in.read((byte[]) null));
        }
    }

    @Test
    public void testZeroLengthReadReturnsZeroAtEndOfStream() throws IOException {
        final byte[] compressed = compress(new byte[] { 1 });

        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            assertEquals(1, in.read());
            assertEquals(-1, in.read());
            assertEquals(0, in.read(new byte[0]));
            assertEquals(0, in.read(new byte[3], 1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> in.read(new byte[3], 4, 0));
        }
    }

    @Test
    public void testAvailableIsNeverNegativeAtEndOfStream() throws IOException {
        final byte[] raw = new byte[36];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) i;
        }
        final byte[] compressed = compress(raw);

        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            assertEquals(0, in.available()); // nothing has been decompressed yet
            assertEquals(0, in.read());
            assertEquals(raw.length - 1, in.available()); // the rest of the current decompressed block
            assertEquals(raw.length - 1, in.read(new byte[raw.length]));
            assertEquals(-1, in.read());

            // The delegate reports -(size of the last block) here; InputStream.available() specifies 0 at EOF.
            assertEquals(0, in.available());
            assertEquals(0, new byte[in.available()].length);
            // BufferedInputStream.available() overflows to Integer.MAX_VALUE when the delegate is negative.
            assertEquals(0, new BufferedInputStream(in).available());
        }
    }

    @Test
    public void testAvailableIsNeverNegativeAfterSkippingToTheEndOrAfterClose() throws IOException {
        final byte[] compressed = compress(new byte[64]);
        // Not try-with-resources: the point of this test is the assertion AFTER close(), so the stream is
        // closed in a finally that still runs when one of the earlier assertions fails.
        final LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(compressed));

        try {
            long skipped = 0;
            while (skipped < 64) {
                final long n = in.skip(64 - skipped);
                if (n <= 0) {
                    break;
                }
                skipped += n;
            }
            assertEquals(64, skipped);
            assertEquals(0, in.skip(1)); // the end has already been reached

            // Reached without the caller ever observing a -1.
            assertEquals(0, in.available());
        } finally {
            in.close();
        }

        assertEquals(0, in.available()); // and it must still be 0 after close()
    }

    @Test
    public void testAvailableUsageExampleSizesTheBufferFromTheRestOfTheFirstBlock() throws IOException {
        // Doc pin for the example on available(): it has to read BEFORE consulting available(), because a
        // fresh stream has decompressed nothing and reports 0 - the previous example guarded on
        // available() > 0 first and therefore read nothing, ever.
        final byte[] raw = new byte[4096];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) i;
        }

        try (LZ4BlockInputStream lz4In = new LZ4BlockInputStream(new ByteArrayInputStream(compress(raw)))) {
            assertEquals(0, lz4In.available());

            final int first = lz4In.read();
            assertTrue(first >= 0);

            final byte[] buffer = new byte[lz4In.available()];
            assertEquals(raw.length - 1, buffer.length);
            assertEquals(buffer.length, lz4In.read(buffer));
        }
    }
}
