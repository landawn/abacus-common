package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BrotliInputStreamTest extends TestBase {

    // The org.brotli:dec library (0.1.2) is decoder-only. We cannot generate
    // valid compressed data at test time, so the tests below focus on the
    // wrapper's contract: bounds checking, idempotent close, skip(0), skip(<0),
    // and error propagation on invalid Brotli streams.

    @Test
    public void testConstructorWithEmptyStreamSucceeds() throws IOException {
        // The org.brotli.dec constructor does not read from the source eagerly;
        // it only fails on first read. So construction with an empty stream is OK.
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
            Assertions.assertNotNull(bis);
        }
    }

    @Test
    public void testConstructorWithBufferSize() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]), 1024)) {
            Assertions.assertNotNull(bis);
        }
    }

    @Test
    public void testConstructorRejectsZeroOrNegativeBufferSize() {
        // Underlying lib enforces buffer size > 0 with IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new BrotliInputStream(new ByteArrayInputStream(new byte[0]), 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new BrotliInputStream(new ByteArrayInputStream(new byte[0]), -1));
    }

    @Test
    public void testReadInvalidThrowsIOException() {
        // Random data is not valid Brotli; first read() must throw IOException
        Assertions.assertThrows(IOException.class, () -> {
            try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 }))) {
                while (bis.read() != -1) {
                    // drain
                }
            }
        });
    }

    @Test
    public void testReadByteArrayWithOffsetOutOfBounds() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
            final byte[] buf = new byte[10];
            // Wrapper now enforces the InputStream contract: IndexOutOfBoundsException
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bis.read(buf, -1, 1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bis.read(buf, 0, -1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bis.read(buf, 5, 10));
        }
    }

    @Test
    public void testReadZeroLengthReturnsZero() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
            // Per InputStream contract, read with len==0 returns 0 without reading
            final byte[] buf = new byte[10];
            Assertions.assertEquals(0, bis.read(buf, 0, 0));
        }
    }

    @Test
    public void testSkipNegative() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> bis.skip(-1));
        }
    }

    @Test
    public void testMarkSupported() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
            // org.brotli.dec.BrotliInputStream does not override markSupported() so it returns false
            Assertions.assertFalse(bis.markSupported());
        }
    }

    @Test
    public void testMarkIsNoOpWhenNotSupported() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
            // Should be a no-op since markSupported() returns false (per InputStream contract)
            bis.mark(1024);
        }
    }

    @Test
    public void testResetThrowsWhenNotSupported() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
            Assertions.assertThrows(IOException.class, () -> bis.reset());
        }
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        final BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]));
        bis.close();
        bis.close(); // Should not throw
    }
}
