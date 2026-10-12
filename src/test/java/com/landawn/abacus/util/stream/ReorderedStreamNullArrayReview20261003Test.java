package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ObjIterator;

@Tag("unit")
class ReorderedStreamNullArrayReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullDestinationDoesNotReadOrCloseUpstreamBeforeRejection(boolean reverse) {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        Stream<Integer> source = Stream.of(Arrays.asList(1, 2, 3).iterator()).onEach(v -> reads.incrementAndGet()).onClose(closes::incrementAndGet);
        try (Stream<Integer> stream = reverse ? source.reversed() : source.rotated(1)) {
            ObjIterator<Integer> iterator = stream.iterator();
            assertThrows(NullPointerException.class, () -> iterator.toArray((Integer[]) null));
            assertEquals(0, reads.get());
            assertEquals(0, closes.get());
            assertArrayEquals(reverse ? new Integer[] { 3, 2, 1 } : new Integer[] { 3, 1, 2 }, iterator.toArray(new Integer[0]));
            assertEquals(3, reads.get());
            assertFalse(iterator.hasNext());
        }
        assertEquals(1, closes.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullDestinationAfterInitializationDoesNotAdvanceTheCursor(boolean reverse) {
        try (Stream<Integer> stream = reverse ? Stream.of(1, 2, 3).reversed() : Stream.of(1, 2, 3).rotated(1)) {
            ObjIterator<Integer> iterator = stream.iterator();
            assertEquals(3, iterator.next());
            assertThrows(NullPointerException.class, () -> iterator.toArray((Integer[]) null));
            assertArrayEquals(reverse ? new Integer[] { 2, 1 } : new Integer[] { 1, 2 }, iterator.toArray(new Integer[0]));
            assertFalse(iterator.hasNext());
            assertThrows(NullPointerException.class, () -> iterator.toArray((Integer[]) null));
        }
    }
}
