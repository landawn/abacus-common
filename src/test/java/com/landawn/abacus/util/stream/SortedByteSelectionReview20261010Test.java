package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;

/**
 * Sorted {@code IteratorByteStream.kthLargest} switches from a ring buffer to the 256-bin histogram above
 * {@code 256 * Long.BYTES} ranks; both paths must select the same element. The allocation bound of the histogram
 * path is checked separately by the slow-test class SortedByteSelectionMemoryReview20261010Test.
 */
class SortedByteSelectionReview20261010Test extends TestBase {
    @Test
    void sortedSelectionPreservesRanksAcrossHistogramBoundary() {
        for (final int k : new int[] { 1, 16, 255, 256, 257, 2048, 2049, 4095, 4096 }) {
            assertEquals((byte) (Byte.MAX_VALUE - (k - 1) / 16), ascendingBytes(4096).kthLargest(k).get(), "rank " + k);
        }

        assertTrue(ascendingBytes(4096).kthLargest(4097).isEmpty());
        assertTrue(ascendingBytes(4096).kthLargest(Integer.MAX_VALUE).isEmpty());
        assertTrue(ascendingBytes(0).kthLargest(2049).isEmpty());
    }

    /** Sorted bytes from {@code Byte.MIN_VALUE} upwards, each value repeated 16 times. */
    private static ByteStream ascendingBytes(final int count) {
        return new IteratorByteStream(new ByteIterator() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < count;
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return (byte) (Byte.MIN_VALUE + cursor++ / 16);
            }
        }, true, null);
    }
}
