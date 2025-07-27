package com.landawn.abacus.util.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class StreamBase100Test extends TestBase {

    @Test
    public void testShuffled() {
        // Test with default random
        Stream<Integer> stream = createStream(1, 2, 3, 4, 5);
        List<Integer> shuffled = stream.shuffled().toList();

        Assertions.assertEquals(5, shuffled.size());
        Assertions.assertTrue(shuffled.containsAll(Arrays.asList(1, 2, 3, 4, 5)));
    }

    @Test
    public void testElementAt() {
        // Test valid positions
        Stream<String> stream1 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("a", stream1.elementAt(0).get());

        Stream<String> stream2 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("c", stream2.elementAt(2).get());

        Stream<String> stream3 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("e", stream3.elementAt(4).get());

        // Test negative position
        Stream<String> stream4 = createStream("a", "b", "c");
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream4.elementAt(-1));

        // Test position out of bounds
        Stream<String> stream5 = createStream("a", "b", "c");
        Assertions.assertThrows(NoSuchElementException.class, () -> stream5.elementAt(5).get());

        // Test empty stream
        Stream<String> stream6 = createStream();
        Assertions.assertThrows(NoSuchElementException.class, () -> stream6.elementAt(0).get());
    }

    @Test
    public void testToImmutableList() {
        Stream<Integer> stream = createStream(1, 2, 3, 4, 5);
        ImmutableList<Integer> immutableList = stream.toImmutableList();

        Assertions.assertEquals(5, immutableList.size());
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(immutableList));

        // Test empty stream
        Stream<Integer> emptyStream = createStream();
        ImmutableList<Integer> emptyList = emptyStream.toImmutableList();
        Assertions.assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testToImmutableSet() {
        Stream<Integer> stream = createStream(1, 2, 3, 3, 4, 5);
        ImmutableSet<Integer> immutableSet = stream.toImmutableSet();

        Assertions.assertEquals(5, immutableSet.size());
        Assertions.assertTrue(immutableSet.containsAll(Arrays.asList(1, 2, 3, 4, 5)));

        // Test empty stream
        Stream<Integer> emptyStream = createStream();
        ImmutableSet<Integer> emptySet = emptyStream.toImmutableSet();
        Assertions.assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testToArray() {
        Stream<String> stream = createStream("a", "b", "c");
        Object[] array = stream.toArray();

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, array);

        // Test empty stream
        Stream<String> emptyStream = createStream();
        Object[] emptyArray = emptyStream.toArray();
        Assertions.assertEquals(0, emptyArray.length);
    }

    @Test
    public void testThrowIfEmpty() {
        // Test non-empty stream
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream1.throwIfEmpty());

        // Test empty stream with default exception
        Stream<Integer> stream2 = createStream();
        Assertions.assertThrows(NoSuchElementException.class, () -> stream2.throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        // Test non-empty stream
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream1.throwIfEmpty(() -> new IllegalStateException("Empty!")));

        // Test empty stream with custom exception
        Stream<Integer> stream2 = createStream();
        Assertions.assertThrows(IllegalStateException.class, () -> stream2.throwIfEmpty(() -> new IllegalStateException("Custom empty message")).count());

        // Test null supplier
        Stream<Integer> stream3 = createStream();
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream3.throwIfEmpty(null));
    }

    @Test
    public void testPrintln() {
        // Just test that it doesn't throw exception
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream.println());

        // Test empty stream
        Stream<Integer> emptyStream = createStream();
        Assertions.assertDoesNotThrow(() -> emptyStream.println());
    }

    @Test
    public void testIsParallel() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertFalse(stream.isParallel());
    }

    @Test
    public void testSequential() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> sequential = stream.sequential();

        Assertions.assertSame(stream, sequential);
        Assertions.assertFalse(sequential.isParallel());
    }

    @Test
    public void testParallel() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> parallel = stream.parallel();

        Assertions.assertTrue(parallel.isParallel());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        Stream<Integer> stream = createStream(1, 2, 3);

        // Test with valid thread number
        Stream<Integer> parallel1 = stream.parallel(4);
        Assertions.assertTrue(parallel1.isParallel());

        // Test with zero thread number (should use default)
        Stream<Integer> stream2 = createStream(1, 2, 3);
        Stream<Integer> parallel2 = stream2.parallel(0);
        Assertions.assertTrue(parallel2.isParallel());

        // Test with negative thread number
        Stream<Integer> stream3 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream3.parallel(-1));
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> parallel = stream.parallel(executor);

            Assertions.assertTrue(parallel.isParallel());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> parallel = stream.parallel(4, executor);

            Assertions.assertTrue(parallel.isParallel());

            // Test with negative thread number
            Stream<Integer> stream2 = createStream(1, 2, 3);
            Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.parallel(-1, executor));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithParallelSettings() {
        ParallelSettings ps = PS.create(Splitor.ARRAY);

        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> parallel = stream.parallel(ps);

        Assertions.assertTrue(parallel.isParallel());

        // Test with null settings
        Stream<Integer> stream2 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.parallel((ParallelSettings) null));

        // Test with custom settings
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            ParallelSettings customPs = new ParallelSettings().maxThreadNum(4).executor(executor).splitor(Splitor.ARRAY);
            Stream<Integer> stream3 = createStream(1, 2, 3);
            Stream<Integer> parallel3 = stream3.parallel(customPs);
            Assertions.assertTrue(parallel3.isParallel());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSps() {
        // Test sequential to parallel to sequential
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.sps(s -> s.map(x -> x * 2));

        Assertions.assertFalse(result.isParallel());
        assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());

        // Test on closed stream
        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.sps(s -> s));
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.sps(4, s -> s.map(x -> x * 2));

        Assertions.assertFalse(result.isParallel());
        assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());
    }

    @Test
    public void testSpsWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.sps(4, executor, s -> s.map(x -> x * 2));

            Assertions.assertFalse(result.isParallel());
            assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testPsp() {
        // Test parallel to sequential to parallel
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.psp(s -> s.map(x -> x * 2));

        Assertions.assertTrue(result.isParallel());
        List<Integer> resultList = result.toList();
        Collections.sort(resultList);
        Assertions.assertEquals(Arrays.asList(2, 4, 6), resultList);

        // Test on closed stream
        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.psp(s -> s));
    }

    @Test
    public void testTransform() {
        Stream<Integer> stream = createStream(1, 2, 3);
        List<Integer> result = stream.transform(s -> s.map(x -> x * 2)).toList();

        Assertions.assertEquals(Arrays.asList(2, 4, 6), result);

        // Test with null transfer function
        Stream<Integer> stream2 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.transform(null));

        // Test on closed stream
        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.transform(s -> s));
    }

    @Test
    public void testClose() {
        // Test simple close
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream.close());

        // Test double close (should be idempotent)
        Assertions.assertDoesNotThrow(() -> stream.close());

        // Test operations after close
        Assertions.assertThrows(IllegalStateException.class, () -> stream.map(x -> x * 2));
    }

    @Test
    public void testCloseWithHandlers() {
        // Test with close handlers
        List<String> closeOrder = new ArrayList<>();

        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> closeOrder.add("handler1")).onClose(() -> closeOrder.add("handler2"));

        stream.close();

        // Verify handlers were called
        Assertions.assertEquals(2, closeOrder.size());
        Assertions.assertTrue(closeOrder.contains("handler1"));
        Assertions.assertTrue(closeOrder.contains("handler2"));
    }

    @Test
    public void testCloseWithException() {
        // Test close handler that throws exception
        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> {
            throw new RuntimeException("Close error");
        });

        Assertions.assertThrows(RuntimeException.class, () -> stream.close());
    }

    @Test
    public void testMultipleCloseHandlersWithExceptions() {
        // Test multiple close handlers with exceptions
        List<String> closeOrder = new ArrayList<>();

        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> {
            closeOrder.add("handler1");
            throw new RuntimeException("Error 1");
        }).onClose(() -> {
            closeOrder.add("handler2");
            throw new RuntimeException("Error 2");
        }).onClose(() -> closeOrder.add("handler3"));

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> stream.close());

        // Verify all handlers were called despite exceptions
        Assertions.assertEquals(3, closeOrder.size());

        // Verify suppressed exceptions
        Assertions.assertTrue(thrown.getSuppressed().length > 0);
    }

    @Test
    public void testAssertNotClosed() {
        Stream<Integer> stream = createStream(1, 2, 3);

        // Should work fine on open stream
        Assertions.assertDoesNotThrow(() -> stream.map(x -> x * 2).toList());

        // Close the stream
        stream.close();

        // Should throw on closed stream
        Assertions.assertThrows(IllegalStateException.class, () -> stream.map(x -> x * 2));
    }

    @Test
    public void testOnClose() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        createStream(1, 2, 3).onClose(() -> handlerCalled.set(true)).close();
        Assertions.assertTrue(handlerCalled.get());
    }

    @Test
    public void testMultipleOnClose() {
        {
            List<Integer> callOrder = new ArrayList<>();

            Stream<Integer> stream = createStream(1, 2, 3);
            stream = stream.onClose(() -> callOrder.add(1)).onClose(() -> callOrder.add(2)).onClose(() -> callOrder.add(3));

            stream.close();

            // Handlers are invoked in the order they were added (first-added, first-invoked).
            Assertions.assertEquals(Arrays.asList(1, 2, 3), callOrder);
        }
        {
            List<Integer> callOrder = new ArrayList<>();
            java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3);
            javaStream.onClose(() -> callOrder.add(1)).onClose(() -> callOrder.add(2)).onClose(() -> callOrder.add(3));
            javaStream.close();
            // Handlers are invoked in the order they were added (first-added, first-invoked).
            Assertions.assertEquals(Arrays.asList(1, 2, 3), callOrder);
        }
    }

    // Test helper method
    @SafeVarargs
    private final <T> Stream<T> createStream(T... elements) {
        return Stream.of(elements);
    }

    private <T> Stream<T> createStream(Collection<T> elements) {
        return Stream.of(elements);
    }
}