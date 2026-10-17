package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.LongIterator;

/** LongStream lifecycle contracts. */
public class LongStreamLifecycleTest extends LongStreamTestSupport {

    // close

    @Test
    public void testClose() {
        AtomicLong closed = new AtomicLong(0);
        LongStream stream = createLongStream(1L, 2L, 3L).onClose(() -> closed.incrementAndGet());

        assertEquals(0, closed.get());
        stream.close();
        assertEquals(1, closed.get());
    }

    @Test
    public void testCloseHandler() {
        AtomicBoolean closed = new AtomicBoolean(false);
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.onClose(() -> closed.set(true));

        stream.sum();
        stream.close();

        assertTrue(closed.get());
    }

    @Test
    public void testExplicitClose() {
        AtomicLong counter = new AtomicLong(0);
        LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet);
        stream.close();
        assertEquals(1, counter.get());
        stream.close();
        assertEquals(1, counter.get());
    }

    @Test
    public void testMultipleCloseHandlers() {
        AtomicInteger closeCount = new AtomicInteger(0);
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.onClose(closeCount::incrementAndGet);
        stream.onClose(closeCount::incrementAndGet);

        stream.sum();
        stream.close();

        assertEquals(2, closeCount.get());
    }

    // G100-06: the list is still read lazily (at terminal-operation time), and close handlers still run
    @Test
    public void testConcatListOfArrays_lazyReadAndCloseHandler() {
        final List<long[]> list = Arrays.asList(new long[] { 1 }, new long[] { 2 });
        final LongStream stream = LongStream.concat(list);
        list.set(1, new long[] { 8, 9 });
        final AtomicInteger closed = new AtomicInteger();
        assertArrayEquals(new long[] { 1, 8, 9 }, stream.onClose(closed::incrementAndGet).toArray());
        assertEquals(1, closed.get());

        final AtomicInteger closed2 = new AtomicInteger();
        assertEquals(3, LongStream.concat(list).onClose(closed2::incrementAndGet).count());
        assertEquals(1, closed2.get());

        assertEquals(18L, LongStream.concat(list).sum());
        assertArrayEquals(new long[] { 1, 8, 9 }, LongStream.concat(list).sorted().toArray());
    }

    @Test
    public void testDeferCloseInvokesSupplierAndCloseHandlers() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();
        final LongStream stream = LongStream.defer(() -> {
            calls.incrementAndGet();
            return LongStream.of(1L).onClose(closed::incrementAndGet);
        });

        stream.close();

        assertEquals(1, calls.get());
        assertEquals(1, closed.get());
    }

    @Test
    public void testIteratorDoesNotCloseStream() {
        AtomicLong counter = new AtomicLong(0);
        LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet);
        LongIterator iter = stream.iterator();
        while (iter.hasNext()) {
            iter.nextLong();
        }
        assertEquals(0, counter.get());
        stream.close();
        assertEquals(1, counter.get());
    }

    // onClose

    @Test
    public void testOnClose() {
        AtomicLong counter = new AtomicLong(0);
        try (LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet)) {
            stream.count();
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void testOnClose_HappyPath() {
        AtomicBoolean closed = new AtomicBoolean(false);
        LongStream s = LongStream.of(1, 2, 3).onClose(() -> closed.set(true));
        s.toArray();
        s.close();
        assertTrue(closed.get());
    }

    @Test
    public void testOnClose_multipleHandlers() {
        final int[] closeCount = { 0 };
        LongStream stream = LongStream.of(1L, 2L, 3L).onClose(() -> closeCount[0]++).onClose(() -> closeCount[0]++);
        stream.toArray();
        assertEquals(2, closeCount[0]);
    }

    @Test
    public void testOnClose_singleHandler() {
        final boolean[] closed = { false };
        LongStream stream = LongStream.of(1L, 2L, 3L).onClose(() -> closed[0] = true);
        stream.toArray();
        assertTrue(closed[0]);
    }

    // rangeClosed

    @Test
    public void testRangeClosed() {
        assertArrayEquals(new long[] { 0L, 1L, 2L, 3L, 4L, 5L }, LongStream.rangeClosed(0L, 5L).toArray());
        assertArrayEquals(new long[] { 5L }, LongStream.rangeClosed(5L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.rangeClosed(5L, 0L).toArray());
    }

    // Covers count() in rangeClosed(long, long) inner LongIteratorEx
    @Test
    public void testRangeClosed_Count() {
        assertEquals(6L, LongStream.rangeClosed(0L, 5L).count());
    }

    @Test
    public void testRangeClosed_SingleElement() {
        // rangeClosed(long, long) when startInclusive == endInclusive
        long[] result = LongStream.rangeClosed(7L, 7L).toArray();
        assertArrayEquals(new long[] { 7L }, result);
    }

    // Covers advance() in rangeClosed(long, long) inner LongIteratorEx
    @Test
    public void testRangeClosed_Skip() {
        long[] result = LongStream.rangeClosed(0L, 9L).skip(3).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L, 6L, 7L, 8L, 9L }, result);
    }

    @Test
    public void testRangeClosed_Skip_ExercisesAdvance() {
        // exercises advance() for rangeClosed inner iterator - skip beyond size
        long[] result = LongStream.rangeClosed(1L, 5L).skip(10).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeClosed_WithStep_Count() {
        // rangeClosed(long, long, long) - positive step
        long[] result = LongStream.rangeClosed(0L, 10L, 2L).toArray();
        assertArrayEquals(new long[] { 0L, 2L, 4L, 6L, 8L, 10L }, result);
    }

    @Test
    public void testRangeClosed_WithStep_EmptyWhenWrongDirection() {
        assertEquals(0L, LongStream.rangeClosed(0L, 10L, -1L).count());
        assertEquals(0L, LongStream.rangeClosed(10L, 0L, 1L).count());
    }

    @Test
    public void testRangeClosed_WithStep_NegativeStep() {
        long[] result = LongStream.rangeClosed(10L, 2L, -2L).toArray();
        assertArrayEquals(new long[] { 10L, 8L, 6L, 4L, 2L }, result);
    }

    @Test
    public void testRangeClosed_WithStep_SingleElement() {
        // endInclusive == startInclusive: single element regardless of by
        long[] result = LongStream.rangeClosed(5L, 5L, 3L).toArray();
        assertArrayEquals(new long[] { 5L }, result);
    }

    @Test
    public void testRangeClosed_WithStep_SingleValueAndWrongDirection() {
        assertArrayEquals(new long[] { 5L }, LongStream.rangeClosed(5L, 5L, 2L).toArray());
        assertEquals(0L, LongStream.rangeClosed(5L, 1L, 2L).count());
        assertEquals(0L, LongStream.range(5L, 1L, 2L).count());
    }

    // Covers advance() in rangeClosed(long, long, long) inner LongIteratorEx
    @Test
    public void testRangeClosed_WithStep_Skip() {
        long[] result = LongStream.rangeClosed(0L, 20L, 2L).skip(3).toArray();
        assertArrayEquals(new long[] { 6L, 8L, 10L, 12L, 14L, 16L, 18L, 20L }, result);
    }

    @Test
    public void testRangeClosed_WithStep_SkipAdvance() {
        // exercises advance() in rangeClosed(long, long, long) inner iterator
        long[] result = LongStream.rangeClosed(0L, 10L, 2L).skip(2).toArray();
        assertArrayEquals(new long[] { 4L, 6L, 8L, 10L }, result);
    }

    @Test
    public void testRangeClosed_WithStep_SkipAll() {
        long[] result = LongStream.rangeClosed(0L, 10L, 2L).skip(100).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeClosed_WithStep_ZeroThrows() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.rangeClosed(0L, 10L, 0L));
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        LongStream stream = LongStream.rangeClosed(5L, 1L, -1L);
        assertArrayEquals(new long[] { 5L, 4L, 3L, 2L, 1L }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithNegativeStep2() {
        long[] result = LongStream.rangeClosed(5, 1, -1).toArray();
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, result);
    }

    @Test
    public void testRangeClosedWithStep() {
        assertArrayEquals(new long[] { 0L, 2L, 4L, 6L }, LongStream.rangeClosed(0L, 6L, 2L).toArray());
        assertArrayEquals(new long[] { 5L, 3L, 1L }, LongStream.rangeClosed(5L, 1L, -2L).toArray());
    }

}
