package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class Lz4ZeroReadTest extends TestBase {
    @Test
    void zeroReadsValidateWithoutTouchingTheSource() throws IOException {
        try (final var input = new LZ4BlockInputStream(new InputStream() {
            @Override
            public int read() {
                throw new AssertionError("Source must not be read");
            }
        })) {
            assertEquals(0, input.read(new byte[0]));
            assertEquals(0, input.read(new byte[1], 1, 0));
            assertThrows(NullPointerException.class, () -> input.read(null, 0, 0));
            assertThrows(NullPointerException.class, () -> input.read((byte[]) null));
            assertThrows(IndexOutOfBoundsException.class, () -> input.read(new byte[1], -1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> input.read(new byte[1], 2, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> input.read(new byte[1], Integer.MAX_VALUE, 0));
        }
    }

    @Test
    void zeroReadsPreserveSubsequentDecompressionAndEof() throws IOException {
        final byte[] raw = "\uD83D\uDE00 and ASCII".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final var encoded = new ByteArrayOutputStream();
        try (final var output = new LZ4BlockOutputStream(encoded)) {
            output.write(raw);
        }
        try (final var input = new LZ4BlockInputStream(new ByteArrayInputStream(encoded.toByteArray()))) {
            assertEquals(0, input.read(new byte[0]));
            assertArrayEquals(raw, input.readAllBytes());
            assertEquals(-1, input.read());
            assertEquals(0, input.read(new byte[1], 1, 0));
        }
    }
}
