package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** ShortStream lifecycle contracts. */
public class ShortStreamLifecycleTest extends ShortStreamTestSupport {

    @Test
    public void testClose() {
        final AtomicInteger closeCount = new AtomicInteger(0);
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).onClose(closeCount::incrementAndGet);

        stream.close();
        assertEquals(1, closeCount.get());

        stream.close();
        assertEquals(1, closeCount.get());
    }

    // onClose

    @Test
    public void testOnClose() {
        final AtomicInteger closeCount = new AtomicInteger(0);
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).onClose(closeCount::incrementAndGet);

        assertEquals(0, closeCount.get());
        stream.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testOnClose_Multiple() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortStream stream = createShortStream((short) 1, (short) 2).onClose(counter::incrementAndGet).onClose(counter::incrementAndGet);
        stream.close();
        assertEquals(2, counter.get());
    }

    @Test
    public void testOnCloseMultiple() {
        final AtomicInteger closeCount = new AtomicInteger(0);
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).onClose(closeCount::incrementAndGet).onClose(closeCount::incrementAndGet);

        stream.close();
        assertEquals(2, closeCount.get());
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel(executor).map(n -> (short) (n * 2)).toArray();

            Arrays.sort(result);
            assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel(2, executor).map(n -> (short) (n * 2)).toArray();

            Arrays.sort(result);
            assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
        } finally {
            executor.shutdown();
        }
    }

    // rangeClosed

    @Test
    public void testRangeClosed() {
        ShortStream stream1 = ShortStream.rangeClosed((short) 1, (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = ShortStream.rangeClosed((short) 5, (short) 5);
        assertArrayEquals(new short[] { 5 }, stream2.toArray());
    }

    @Test
    public void testRangeClosed_Descending() {
        short[] result = ShortStream.rangeClosed((short) 5, (short) 1).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeClosed_Same() {
        short[] result = ShortStream.rangeClosed((short) 3, (short) 3).toArray();
        assertArrayEquals(new short[] { 3 }, result);
    }

    @Test
    public void testRangeClosedSingleElement() {
        assertArrayEquals(new short[] { 5 }, ShortStream.rangeClosed((short) 5, (short) 5).toArray());
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        ShortStream stream = ShortStream.rangeClosed((short) 5, (short) 1, (short) -1);
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        ShortStream stream1 = ShortStream.rangeClosed((short) 0, (short) 10, (short) 3);
        assertArrayEquals(new short[] { 0, 3, 6, 9 }, stream1.toArray());

        ShortStream stream2 = ShortStream.rangeClosed((short) 10, (short) 0, (short) -3);
        assertArrayEquals(new short[] { 10, 7, 4, 1 }, stream2.toArray());
    }

    @Test
    public void testSpsWithExecutorMethod() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(2, executor, s -> s.filter(v -> v > 2)).toArray();
            assertEquals(3, result.length);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSpsWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(2, executor, s -> s.map(n -> (short) (n * 2)))
                    .toArray();

            Arrays.sort(result);
            assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testStreamAutoClosedAfterTerminalOp() {
        final AtomicInteger closeCount = new AtomicInteger(0);
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).onClose(closeCount::incrementAndGet);

        stream.count();
        assertEquals(1, closeCount.get());
    }

}
