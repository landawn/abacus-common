package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** DoubleStream lifecycle contracts. */
public class DoubleStreamLifecycleTest extends DoubleStreamTestSupport {

    @Test
    public void testToJdkStreamCloseRunsArraySourceHandlersOnce() {
        final AtomicInteger closeCount = new AtomicInteger();
        final DoubleStream source = createDoubleStream(1d, 2d, 3d).onClose(closeCount::incrementAndGet);

        source.toJdkStream().close();
        source.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testOnClose() {
        AtomicInteger closeCounter = new AtomicInteger(0);
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0).onClose(() -> closeCounter.incrementAndGet());
        stream.count();
        stream.close();
        assertEquals(1, closeCounter.get());
    }

    @Test
    public void testOnClose_HappyPath() {
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);
        DoubleStream s = DoubleStream.of(1.0, 2.0, 3.0).onClose(() -> closed.set(true));
        s.toArray();
        s.close();
        assertTrue(closed.get());
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        // DoubleStream doesn't support rangeClosed with step parameter
        // Testing generate with limited range instead
        DoubleStream stream = DoubleStream.iterate(5.0, d -> d >= 1.0, d -> d - 1.0);
        double[] result = stream.toArray();
        assertArrayEquals(new double[] { 5.0, 4.0, 3.0, 2.0, 1.0 }, result, 0.0001);
    }

    // G094-01: the list is still read lazily (at terminal-operation time), and the stream is exhausted afterwards
    @Test
    public void testConcatListOfArrays_lazyReadAndCloseHandler() {
        final List<double[]> list = Arrays.asList(new double[] { 1 }, new double[] { 2 });
        final DoubleStream stream = DoubleStream.concat(list);
        list.set(1, new double[] { 8, 9 });
        final AtomicInteger closed = new AtomicInteger();
        assertArrayEquals(new double[] { 1, 8, 9 }, stream.onClose(closed::incrementAndGet).toArray());
        assertEquals(1, closed.get());

        final AtomicInteger closed2 = new AtomicInteger();
        assertEquals(3, DoubleStream.concat(list).onClose(closed2::incrementAndGet).count());
        assertEquals(1, closed2.get());

        assertEquals(8.0 + 9.0 + 1.0, DoubleStream.concat(list).sum());
        assertArrayEquals(new double[] { 1, 8, 9 }, DoubleStream.concat(list).sorted().toArray());
    }
}
