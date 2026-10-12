package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.N;

/** FloatStream lifecycle contracts. */
public class FloatStreamLifecycleTest extends FloatStreamTestSupport {

    @Test
    @DisplayName("close() should close stream")
    public void testClose() {
        assertDoesNotThrow(() -> {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            stream.close();
        });
    }

    @Test
    @DisplayName("closed stream should throw IllegalStateException")
    public void testClosedStream() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        stream.toArray();
        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    // onClose

    @Test
    @DisplayName("onClose() should register close handler")
    public void testOnClose() {
        List<String> closeActions = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f).onClose(() -> closeActions.add("closed"));
        stream.toArray();
        assertEquals(Arrays.asList("closed"), closeActions);
    }

    @Test
    public void testOnClose_Multiple() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatStream stream = createFloatStream(1.0f, 2.0f).onClose(counter::incrementAndGet).onClose(counter::incrementAndGet);
        stream.close();
        assertEquals(2, counter.get());
    }

    @Test
    public void testStreamCreatedAfterOnClose() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).toList());
    }

    @Test
    @DisplayName("parallel() with executor")
    public void testParallelWithExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatStream parallel = stream.parallel(executor);
        assertTrue(parallel.isParallel());
    }

    @Test
    public void testSpsWithExecutor() {
        java.util.concurrent.Executor executor = Executors.newFixedThreadPool(2);
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).sps(2, executor, s -> s.filter(f -> f > 2.0f)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testStreamIsClosedAfterTerminalOp() {
        FloatStream stream = FloatStream.of(1.0f, 2.0f);
        stream.count();
        assertThrows(IllegalStateException.class, stream::count);
    }

}
