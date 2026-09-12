package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BrotliInputStream(new ByteArrayInputStream(new byte[0]), 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BrotliInputStream(new ByteArrayInputStream(new byte[0]), -1));
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
        assertDoesNotThrow(() -> {
            try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
                // Should be a no-op since markSupported() returns false (per InputStream contract)
                bis.mark(1024);
            }
        });
    }

    @Test
    public void testResetThrowsWhenNotSupported() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]))) {
            Assertions.assertThrows(IOException.class, () -> bis.reset());
        }
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        assertDoesNotThrow(() -> {
            final BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(new byte[0]));
            bis.close();
            bis.close(); // Should not throw
        });
    }

    // A real Brotli stream whose decompressed payload is "hello" (5 bytes), embedded as a literal so that
    // no encoder dependency is needed: hex 0b 02 80 68 65 6c 6c 6f 03.
    private static final byte[] HELLO_BR = { 0x0b, 0x02, (byte) 0x80, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x03 };

    @Test
    public void testReadIntoArrayAfterSingleByteReadDoesNotReportEof() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR))) {
            Assertions.assertEquals('h', bis.read());

            final byte[] buf = new byte[10];
            Assertions.assertEquals(4, bis.read(buf, 0, 10));
            Assertions.assertEquals("ello", new String(buf, 0, 4, StandardCharsets.UTF_8));
            Assertions.assertEquals(-1, bis.read(buf, 0, 10));
        }
    }

    @Test
    public void testReadIntoArrayAfterSingleByteReadDoesNotReportEof_SingleArgOverload() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR))) {
            Assertions.assertEquals('h', bis.read());

            final byte[] buf = new byte[10];
            Assertions.assertEquals(4, bis.read(buf));
            Assertions.assertEquals("ello", new String(buf, 0, 4, StandardCharsets.UTF_8));
            Assertions.assertEquals(-1, bis.read(buf));
        }
    }

    @Test
    public void testReadLoopAfterSingleByteReadReturnsWholePayload() throws IOException {
        final byte[] out = new byte[16];
        int total = 0;

        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR))) {
            out[total++] = (byte) bis.read();

            final byte[] buf = new byte[8];
            int n;

            while ((n = bis.read(buf)) != -1) {
                System.arraycopy(buf, 0, out, total, n);
                total += n;
            }
        }

        Assertions.assertEquals(5, total);
        Assertions.assertEquals("hello", new String(out, 0, total, StandardCharsets.UTF_8));
    }

    @Test
    public void testSkipAfterSingleByteReadCountsBufferedBytes() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR))) {
            Assertions.assertEquals('h', bis.read());
            Assertions.assertEquals(4, bis.skip(10));
            Assertions.assertEquals(-1, bis.read());
        }
    }

    @Test
    public void testSkipPartOfBufferThenRead() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR))) {
            Assertions.assertEquals('h', bis.read());
            Assertions.assertEquals(2, bis.skip(2));
            Assertions.assertEquals('l', bis.read());
            Assertions.assertEquals('o', bis.read());
            Assertions.assertEquals(-1, bis.read());
        }
    }

    @Test
    public void testReadWholePayloadWithArrayReadsOnly() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR))) {
            final byte[] buf = new byte[16];
            Assertions.assertEquals(5, bis.read(buf, 0, 16));
            Assertions.assertEquals("hello", new String(buf, 0, 5, StandardCharsets.UTF_8));
            Assertions.assertEquals(-1, bis.read(buf, 0, 16));
        }
    }

    @Test
    public void testReadWholePayloadWithSingleByteReadsOnly() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR))) {
            Assertions.assertEquals('h', bis.read());
            Assertions.assertEquals('e', bis.read());
            Assertions.assertEquals('l', bis.read());
            Assertions.assertEquals('l', bis.read());
            Assertions.assertEquals('o', bis.read());
            Assertions.assertEquals(-1, bis.read());
            Assertions.assertEquals(-1, bis.read());
        }
    }

    @Test
    public void testReadWholePayloadWithSmallInternalBuffer() throws IOException {
        try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR), 2)) {
            Assertions.assertEquals('h', bis.read());

            final byte[] buf = new byte[10];
            Assertions.assertEquals(4, bis.read(buf, 0, 10));
            Assertions.assertEquals("ello", new String(buf, 0, 4, StandardCharsets.UTF_8));
            Assertions.assertEquals(-1, bis.read(buf, 0, 10));
        }
    }

    @Test
    public void testMarkSupportedIsFalseEvenWhenSourceSupportsMark() throws IOException {
        final InputStream source = new BufferedInputStream(new ByteArrayInputStream(HELLO_BR));
        Assertions.assertTrue(source.markSupported());

        try (BrotliInputStream bis = new BrotliInputStream(source)) {
            Assertions.assertFalse(bis.markSupported());
            bis.mark(1024); // always a no-op
            Assertions.assertThrows(IOException.class, () -> bis.reset());
        }
    }

    @Test
    public void testAvailableIsAlwaysZeroEvenWithDecodedBytesBuffered() throws IOException {
        final BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR));
        Assertions.assertEquals(0, bis.available());
        Assertions.assertEquals('h', bis.read());
        // Four decoded bytes are buffered and readable without blocking, yet available() still reports 0.
        Assertions.assertEquals(0, bis.available());
        bis.close();
        Assertions.assertEquals(0, bis.available());
    }

    @Test
    public void testReadAndSkipAfterCloseThrowIllegalStateException() throws IOException {
        final BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR));
        bis.close();

        // Unchecked, so a catch (IOException) around stream use does not catch it.
        Assertions.assertThrows(IllegalStateException.class, () -> bis.read());
        Assertions.assertThrows(IllegalStateException.class, () -> bis.read(new byte[4]));
        Assertions.assertThrows(IllegalStateException.class, () -> bis.read(new byte[4], 0, 4));
        Assertions.assertThrows(IllegalStateException.class, () -> bis.skip(1));

        // A zero-length read still returns 0.
        Assertions.assertEquals(0, bis.read(new byte[0]));
        Assertions.assertEquals(0, bis.read(new byte[4], 0, 0));
    }

    @Test
    public void testReadAfterCloseStillReturnsBufferedBytes() throws IOException {
        final BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR));
        Assertions.assertEquals('h', bis.read());
        bis.close();

        Assertions.assertEquals('e', bis.read());
        Assertions.assertEquals(1, bis.skip(1));
        Assertions.assertEquals(0, bis.read(new byte[0]));
        Assertions.assertEquals('l', bis.read());
        Assertions.assertEquals('o', bis.read());
        Assertions.assertThrows(IllegalStateException.class, () -> bis.read());
    }

    @Test
    public void testArrayReadAfterCloseIsServedFromTheBufferedRemainderOnlyWhileItFits() throws IOException {
        final BrotliInputStream fits = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR));
        Assertions.assertEquals('h', fits.read()); // leaves "ello" buffered
        fits.close();

        final byte[] exact = new byte[4];
        Assertions.assertEquals(4, fits.read(exact, 0, 4)); // no decoding needed, so no exception
        Assertions.assertEquals("ello", new String(exact, StandardCharsets.UTF_8));
        Assertions.assertThrows(IllegalStateException.class, () -> fits.read(new byte[4], 0, 4));

        final BrotliInputStream overruns = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR));
        Assertions.assertEquals('h', overruns.read());
        overruns.close();

        // Asking for one byte more than the remainder must still REPORT the remainder: the bytes are written into
        // the caller's array, so a throw here would leave the caller unable to learn how much of it is valid.
        // The short count is returned and the exception surfaces on the next call instead.
        final byte[] tooBig = new byte[5];
        Assertions.assertEquals(4, overruns.read(tooBig, 0, 5));
        Assertions.assertEquals("ello", new String(tooBig, 0, 4, StandardCharsets.UTF_8));
        Assertions.assertThrows(IllegalStateException.class, () -> overruns.read(new byte[5], 0, 5));
        Assertions.assertThrows(IllegalStateException.class, () -> overruns.read());

        // read(byte[]) delegates, so it must report the short count too.
        final BrotliInputStream viaSingleArg = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR));
        Assertions.assertEquals('h', viaSingleArg.read());
        viaSingleArg.close();
        Assertions.assertEquals(4, viaSingleArg.read(new byte[5]));
        Assertions.assertThrows(IllegalStateException.class, () -> viaSingleArg.read(new byte[5]));
    }

    @Test
    public void testSkipAfterCloseIsServedFromTheBufferedRemainderOnlyWhileItFits() throws IOException {
        final BrotliInputStream fits = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR));
        Assertions.assertEquals('h', fits.read()); // leaves "ello" buffered
        fits.close();

        Assertions.assertEquals(4, fits.skip(4)); // exactly the remainder: no decoding, no exception
        Assertions.assertThrows(IllegalStateException.class, () -> fits.skip(1));

        final BrotliInputStream overruns = new BrotliInputStream(new ByteArrayInputStream(HELLO_BR));
        Assertions.assertEquals('h', overruns.read());
        overruns.close();

        // Same invariant for skip: bufOff has already moved past the remainder, so those bytes are consumed.
        // Unlike a read there is no caller array to inspect, so not reporting them would discard them silently.
        Assertions.assertEquals(4, overruns.skip(5));
        Assertions.assertThrows(IllegalStateException.class, () -> overruns.skip(1));
    }
}
