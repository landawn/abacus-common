package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
public class SnappyOutputStreamUtilReview20261011Test extends TestBase {
    @Test
    public void nullBufferIsRejectedBeforeItsRangeAndStreamRemainsUsable() throws IOException {
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (SnappyOutputStream output = new SnappyOutputStream(compressed)) {
            assertThrows(NullPointerException.class, () -> output.write(null, -1, 0));
            assertThrows(NullPointerException.class, () -> output.write(null, 0, -1));
            assertThrows(NullPointerException.class, () -> output.write(null, -1, -1));
            assertThrows(NullPointerException.class, () -> output.write(null, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> output.write(new byte[1], -1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> output.write(new byte[1], 0, -1));
            output.write(new byte[] { 1, 2, 3 });
        }
        try (SnappyInputStream input = new SnappyInputStream(new ByteArrayInputStream(compressed.toByteArray()))) {
            assertArrayEquals(new byte[] { 1, 2, 3 }, input.readAllBytes());
        }
    }
}
