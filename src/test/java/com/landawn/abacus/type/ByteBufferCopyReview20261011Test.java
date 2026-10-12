package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class ByteBufferCopyReview20261011Test extends TestBase {

    @Test
    void copiesTheWrittenPrefixAndPreservesEveryBufferViewState() {
        for (final boolean direct : new boolean[] { false, true }) {
            for (final boolean sliced : new boolean[] { false, true }) {
                for (final boolean readOnly : new boolean[] { false, true }) {
                    ByteBuffer buffer = direct ? ByteBuffer.allocateDirect(7) : ByteBuffer.allocate(7);

                    if (sliced) {
                        buffer.position(1).limit(6);
                        buffer = buffer.slice();
                    }

                    buffer.put(new byte[] { 10, 20, 30, 40, 50 });

                    if (readOnly) {
                        buffer = buffer.asReadOnlyBuffer();
                    }

                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    buffer.position(1).mark().position(3).limit(4);

                    final byte[] copy = ByteBufferType.byteArrayOf(buffer);
                    assertArrayEquals(new byte[] { 10, 20, 30 }, copy);
                    assertEquals("ChQe", new ByteBufferType().stringOf(buffer));
                    assertEquals(3, buffer.position());
                    assertEquals(4, buffer.limit());
                    assertEquals(ByteOrder.LITTLE_ENDIAN, buffer.order());
                    buffer.reset();
                    assertEquals(1, buffer.position());

                    copy[0] = 99;
                    assertEquals((byte) 10, buffer.get(0));
                }
            }
        }
    }

    @Test
    void zeroPositionReturnsAnEmptyCopyEvenWhenBytesRemain() {
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 1, 2, 3 }).asReadOnlyBuffer();
        buffer.mark();

        assertArrayEquals(new byte[0], ByteBufferType.byteArrayOf(buffer));
        assertEquals(0, buffer.position());
        assertEquals(3, buffer.limit());
        buffer.reset();
        assertArrayEquals(new byte[0], ByteBufferType.byteArrayOf(ByteBuffer.allocateDirect(0)));
    }
}
