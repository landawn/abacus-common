package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;

/** IntStream lifecycle contracts. */
public class IntStreamLifecycleTest extends IntStreamTestSupport {

    @Test
    public void testOperationAfterClose() {
        stream = createIntStream(1, 2, 3);
        stream.close();
        assertThrows(IllegalStateException.class, () -> stream.count());
    }



    // G097-05: the list is still read at terminal-operation time; close handlers run once
    @Test
    public void testConcatListOfArrays_lazyReadAndCloseHandler() {
        final List<int[]> list = Arrays.asList(new int[] { 1 }, new int[] { 2 });
        final IntStream stream = IntStream.concat(list);
        list.set(1, new int[] { 8, 9 });
        final AtomicInteger closed = new AtomicInteger();
        assertArrayEquals(new int[] { 1, 8, 9 }, stream.onClose(closed::incrementAndGet).toArray());
        assertEquals(1, closed.get());

        final AtomicInteger closed2 = new AtomicInteger();
        assertEquals(3, IntStream.concat(list).onClose(closed2::incrementAndGet).count());
        assertEquals(1, closed2.get());

        assertEquals(18, IntStream.concat(list).sum());
        assertArrayEquals(new int[] { 1, 8, 9 }, IntStream.concat(list).sorted().toArray());
    }

    @Test
    public void testMergeClosesOpenedSourceWhenSiblingIterateFails() {
        final AtomicInteger closed = new AtomicInteger();
        final IntStream a = IntStream.of(1, 2).onClose(closed::incrementAndGet);
        final IntStream b = IntStream.of(3);
        b.close();

        assertThrows(IllegalStateException.class, () -> IntStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertEquals(1, closed.get());
    }

    // onClose

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        stream = createIntStream(1, 2, 3).onClose(() -> closed.set(true));
        stream.count();
        assertTrue(closed.get());
    }

    @Test
    public void testOnClose_ExecutesHandler() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.of(1, 2, 3).onClose(() -> counter.incrementAndGet());
        stream.count();
        stream.close();
        assertEquals(1, counter.get());
    }

    @Test
    public void testOnClose_HappyPath() {
        AtomicBoolean closed = new AtomicBoolean(false);
        IntStream s = IntStream.of(1, 2, 3).onClose(() -> closed.set(true));
        s.toArray();
        s.close();
        assertTrue(closed.get());
    }

    @Test
    public void testOnClose_MultipleHandlers() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        stream = IntStream.of(1, 2, 3).onClose(() -> counter1.incrementAndGet()).onClose(() -> counter2.incrementAndGet());
        stream.count();
        stream.close();
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    public void testParallel_WithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            int sum = IntStream.of(1, 2, 3, 4, 5).parallel(executor).sum();
            assertEquals(15, sum);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            IntStream stream = IntStream.range(0, 100).parallel(executor);
            assertTrue(stream.isParallel());
            assertEquals(4950, stream.sum());
        } finally {
            executor.shutdown();
        }
    }

    // rangeClosed

    @Test
    public void testRangeClosed_BasicRange() {
        stream = IntStream.rangeClosed(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_EmptyWhenStartGreaterThanEnd() {
        assertEquals(0, IntStream.rangeClosed(5, 3).count());
    }



    // rangeClosed(int, int) with multiple elements uses IteratorIntStream path
    @Test
    public void testRangeClosed_MultipleElements() {
        int[] result = IntStream.rangeClosed(1, 10).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, result);
    }

    @Test
    public void testRangeClosed_NegativeRange() {
        int[] result = IntStream.rangeClosed(-3, 3).toArray();
        assertArrayEquals(new int[] { -3, -2, -1, 0, 1, 2, 3 }, result);
    }

    @Test
    public void rangeClosed_shouldCreateInclusiveRangeStream() {
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, IntStream.rangeClosed(0, 3).toArray());
    }

    @Test
    public void testRangeClosed_SingleElement() {
        stream = IntStream.rangeClosed(5, 5);
        assertArrayEquals(new int[] { 5 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_WithNegativeStep() {
        stream = IntStream.rangeClosed(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2, 0 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_WithStep() {
        stream = IntStream.rangeClosed(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8, 10 }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        stream = IntStream.rangeClosed(5, 1, -1);
        int[] result = stream.toArray();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, result);
    }

    @Test
    public void testRangeClosedWithStep() {
        stream = IntStream.rangeClosed(0, 10, 3);
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, stream.toArray());
    }

    @Test
    public void testStreamCreatedFromRangeClosed() {
        assertEquals(6, IntStream.rangeClosed(0, 5).count());
        assertEquals(5, IntStream.rangeClosed(0, 5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).skip(1).toList());
        assertEquals(6, IntStream.rangeClosed(0, 5).map(e -> e).count());
        assertEquals(5, IntStream.rangeClosed(0, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).map(e -> e).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRangeClosedWithStep() {
        assertEquals(4, IntStream.rangeClosed(0, 9, 3).count());
        assertEquals(3, IntStream.rangeClosed(0, 9, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.rangeClosed(0, 9, 3).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.rangeClosed(0, 9, 3).skip(1).toList());
        assertEquals(4, IntStream.rangeClosed(0, 9, 3).map(e -> e).count());
        assertEquals(3, IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.rangeClosed(0, 9, 3).map(e -> e).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testZipClosesOpenedSourceWhenSiblingIterateFails() {
        final AtomicInteger closed = new AtomicInteger();
        final IntStream a = IntStream.of(1, 2).onClose(closed::incrementAndGet);
        final IntStream b = IntStream.of(3);
        b.close();

        assertThrows(IllegalStateException.class, () -> IntStream.zip(a, b, (x, y) -> x + y).toArray());
        assertEquals(1, closed.get());
    }

    @Test
    public void testZipDoesNotCloseUnopenedSiblingWhenFirstIterateFails() {
        final AtomicInteger closed = new AtomicInteger();
        final IntStream a = IntStream.of(1, 2);
        a.close();
        final IntStream b = IntStream.of(3, 4).onClose(closed::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> IntStream.zip(a, b, (x, y) -> x + y).toArray());
        assertEquals(0, closed.get());
        assertArrayEquals(new int[] { 3, 4 }, b.toArray());
    }

}
