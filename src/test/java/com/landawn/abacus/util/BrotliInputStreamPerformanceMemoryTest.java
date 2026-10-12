package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for BrotliInputStream. */
@Tag("slow-test")
public class BrotliInputStreamPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G005 end ----
    // ---- perf review 2026-10-08 G006 begin ----
    @Nested
    class G006 {
        // A real Brotli stream whose decompressed payload is "hello": hex 0b 02 80 68 65 6c 6c 6f 03.
        private final byte[] helloBr = { 0x0b, 0x02, (byte) 0x80, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x03 };

        // G006-01: array reads and skip never touch the single-byte read buffer, so they must not allocate it
        @Test
        void testBrotliInputStreamRead_arrayReadsAndSkipLeaveTheSingleByteBufferUnallocated() throws Exception {
            try (BrotliInputStream in = new BrotliInputStream(new java.io.ByteArrayInputStream(helloBr))) {
                assertEquals("unallocated", readBufferState(in));

                final byte[] out = new byte[8];
                assertEquals(2, in.read(out, 0, 2));
                assertEquals(1, in.skip(1));
                assertEquals(2, in.read(out));
                assertEquals("lo", new String(out, 0, 2, java.nio.charset.StandardCharsets.UTF_8));
                assertEquals(-1, in.read(out));
                assertEquals(0, in.skip(5));
                assertEquals("unallocated", readBufferState(in));

                // The first single-byte read creates it with the configured size.
                assertEquals(-1, in.read());
                assertEquals("byte[" + org.brotli.dec.BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE + "]", readBufferState(in));
            }

            try (BrotliInputStream in = new BrotliInputStream(new java.io.ByteArrayInputStream(helloBr), 3)) {
                assertEquals("unallocated", readBufferState(in));
                assertEquals('h', in.read());
                assertEquals("byte[3]", readBufferState(in));
            }
        }

        private String readBufferState(final BrotliInputStream in) throws ReflectiveOperationException {
            final java.lang.reflect.Field field = BrotliInputStream.class.getDeclaredField("buf");
            field.setAccessible(true);
            final byte[] buffer = (byte[]) field.get(in);

            return buffer == null ? "unallocated" : "byte[" + buffer.length + "]";
        }

        // G006-01: single-byte, array and skip reads interleave exactly as before, for every buffer size and refill pattern
        @Test
        void testBrotliInputStreamRead_interleavedReadsReturnThePayloadForEveryBufferSize() throws Exception {
            final Random random = new Random(20261008);
            final byte[] payload = new byte[70_000]; // two uncompressed meta-blocks (65,536 + 4,464 bytes)
            random.nextBytes(payload);
            final byte[] stream = uncompressedBrotli(payload);

            for (final int size : new int[] { 1, 2, 3, 7, 64, 4096, 65_536, 70_001, org.brotli.dec.BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE }) {
                // single-byte reads only
                try (BrotliInputStream in = new BrotliInputStream(new java.io.ByteArrayInputStream(stream), size)) {
                    final byte[] read = new byte[payload.length];
                    int n = 0;
                    int b;
                    while ((b = in.read()) != -1) {
                        read[n++] = (byte) b;
                    }
                    assertEquals(payload.length, n, "size=" + size);
                    assertArrayEquals(payload, read, "size=" + size);
                    assertEquals(-1, in.read());
                    assertEquals(-1, in.read(new byte[4]));
                }

                // a seeded mix of read(), read(byte[], int, int), read(byte[]) and skip(long)
                for (int round = 0; round < 3; round++) {
                    try (BrotliInputStream in = new BrotliInputStream(new java.io.ByteArrayInputStream(stream), size)) {
                        final java.io.ByteArrayOutputStream read = new java.io.ByteArrayOutputStream();
                        final java.io.ByteArrayOutputStream expected = new java.io.ByteArrayOutputStream();
                        int position = 0;

                        while (true) {
                            final int op = random.nextInt(4);

                            if (op == 0) {
                                final int b = in.read();
                                if (b == -1) {
                                    assertEquals(payload.length, position, "size=" + size);
                                    break;
                                }
                                read.write(b);
                                expected.write(payload[position++]);
                            } else if (op == 1 || op == 2) {
                                final byte[] chunk = new byte[1 + random.nextInt(300)];
                                final int n = op == 1 ? in.read(chunk, 0, chunk.length) : in.read(chunk);
                                if (n == -1) {
                                    assertEquals(payload.length, position, "size=" + size);
                                    break;
                                }
                                assertTrue(n > 0 && n <= chunk.length);
                                read.write(chunk, 0, n);
                                expected.write(payload, position, n);
                                position += n;
                            } else {
                                final long skipped = in.skip(random.nextInt(200));
                                assertTrue(skipped >= 0 && position + skipped <= payload.length);
                                position += (int) skipped;
                            }
                        }

                        assertArrayEquals(expected.toByteArray(), read.toByteArray(), "size=" + size);
                        assertEquals(-1, in.read());
                    }
                }
            }
        }

        // G006-01: failures of the first single-byte read are unchanged (closed stream, truncated and invalid input)
        @Test
        void testBrotliInputStreamRead_firstSingleByteReadFailuresAreUnchanged() throws Exception {
            final BrotliInputStream closed = new BrotliInputStream(new java.io.ByteArrayInputStream(helloBr));
            closed.close();
            assertThrows(IllegalStateException.class, () -> closed.read());
            assertThrows(IllegalStateException.class, () -> closed.read(new byte[4], 0, 4));
            assertThrows(IllegalStateException.class, () -> closed.skip(1));
            assertEquals(0, closed.read(new byte[0]));

            try (BrotliInputStream empty = new BrotliInputStream(new java.io.ByteArrayInputStream(new byte[0]))) {
                assertThrows(java.io.IOException.class, () -> empty.read());
            }

            try (BrotliInputStream invalid = new BrotliInputStream(new java.io.ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 }))) {
                assertThrows(java.io.IOException.class, () -> invalid.read());
            }

            assertThrows(IllegalArgumentException.class, () -> new BrotliInputStream(new java.io.ByteArrayInputStream(helloBr), 0));
            assertThrows(IllegalArgumentException.class, () -> new BrotliInputStream(null, 16));
        }

        /**
         * Builds a valid Brotli stream (WBITS 16) that stores {@code payload} in uncompressed meta-blocks of at most
         * 65,536 bytes each, followed by an empty last meta-block.
         */
        private byte[] uncompressedBrotli(final byte[] payload) {
            final BrotliBits bits = new BrotliBits();
            bits.write(0, 1); // WBITS = 16
            int offset = 0;

            while (offset < payload.length) {
                final int length = Math.min(65_536, payload.length - offset);
                bits.write(0, 1); // ISLAST = 0
                bits.write(0, 2); // MNIBBLES = 4
                bits.write(length - 1, 16); // MLEN - 1
                bits.write(1, 1); // ISUNCOMPRESSED = 1
                bits.alignToByte();
                bits.bytes.write(payload, offset, length);
                offset += length;
            }

            bits.write(1, 1); // ISLAST = 1
            bits.write(1, 1); // ISLASTEMPTY = 1
            bits.alignToByte();

            return bits.bytes.toByteArray();
        }

        /**
         * Least-significant-bit-first bit writer for the Brotli stream format.
         */
        private final class BrotliBits {
            private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            private int current;
            private int used;

            void write(final int value, final int count) {
                for (int i = 0; i < count; i++) {
                    current |= ((value >>> i) & 1) << used;

                    if (++used == 8) {
                        bytes.write(current);
                        current = 0;
                        used = 0;
                    }
                }
            }

            void alignToByte() {
                if (used > 0) {
                    bytes.write(current);
                    current = 0;
                    used = 0;
                }
            }
        }
    }
}
