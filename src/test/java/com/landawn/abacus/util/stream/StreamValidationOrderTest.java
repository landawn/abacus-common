package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.function.TriFunction;

class StreamValidationOrderTest extends TestBase {
    private static List<Supplier<Stream<Integer>>> streams() {
        return List.of(() -> Stream.of(1, 2, 3), () -> Stream.of(List.of(1, 2, 3).iterator()), () -> Stream.of(1, 2, 3).parallel(2),
                () -> Stream.of(List.of(1, 2, 3).iterator()).parallel(2), () -> Stream.of(1).parallel(2), () -> Stream.of(List.of(1).iterator()).parallel(1));
    }

    @Test
    void invalidIncrementPrecedesNullCallbackAndClosesTheStream() {
        for (Supplier<Stream<Integer>> factory : streams()) {
            AtomicInteger closed = new AtomicInteger();
            Stream<Integer> stream = factory.get().onClose(closed::incrementAndGet);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> stream.slidingMap(0, false, (BiFunction<Integer, Integer, Integer>) null));
            assertTrue(ex.getMessage().contains("increment"));
            assertEquals(1, closed.get());
            assertThrows(IllegalStateException.class, () -> stream.slidingMap(0, false, (BiFunction<Integer, Integer, Integer>) null));

            Stream<Integer> terminal = factory.get();
            IllegalArgumentException terminalFailure = assertThrows(IllegalArgumentException.class,
                    () -> terminal.forEachPair(0, (Throwables.BiConsumer<Integer, Integer, RuntimeException>) null));
            assertTrue(terminalFailure.getMessage().contains("increment"));
        }
    }

    @Test
    void delegatingOverloadsValidateEarlierArgumentsFirst() {
        IllegalArgumentException sliding = assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1, 2).slidingMap(0, (BiFunction<Integer, Integer, Integer>) null));
        assertTrue(sliding.getMessage().contains("increment"));
        IllegalArgumentException indices = assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(new int[0], -1, null));
        assertTrue(indices.getMessage().contains("fromIndex"));
        IllegalArgumentException increment = assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(new int[0], 0, 0, null));
        assertTrue(increment.getMessage().contains("increment"));
        IllegalArgumentException interval = assertThrows(IllegalArgumentException.class, () -> LongStream.interval(-1, 0, null));
        assertTrue(interval.getMessage().contains("delay"));
    }

    @Test
    void tripleOperationsValidateBeforeEmptyOrSequentialFallbackPaths() {
        for (Supplier<Stream<Integer>> factory : streams()) {
            AtomicInteger closed = new AtomicInteger();
            Stream<Integer> stream = factory.get().onClose(closed::incrementAndGet);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> stream.slidingMap(0, false, (TriFunction<Integer, Integer, Integer, Integer>) null));
            assertTrue(failure.getMessage().contains("increment"));
            assertEquals(1, closed.get());
            assertThrows(IllegalStateException.class, () -> stream.slidingMap(0, (TriFunction<Integer, Integer, Integer, Integer>) null));

            AtomicInteger terminalClosed = new AtomicInteger();
            Stream<Integer> terminal = factory.get().onClose(terminalClosed::incrementAndGet);
            failure = assertThrows(IllegalArgumentException.class,
                    () -> terminal.forEachTriple(0, (Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException>) null));
            assertTrue(failure.getMessage().contains("increment"));
            assertEquals(1, terminalClosed.get());
        }
        assertThrows(IllegalArgumentException.class,
                () -> Stream.<Integer> empty().slidingMap(1, true, (TriFunction<Integer, Integer, Integer, Integer>) null));
        assertThrows(IllegalArgumentException.class,
                () -> Stream.<Integer> empty().forEachPair(1, (Throwables.BiConsumer<Integer, Integer, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).slidingMap(0, (TriFunction<Integer, Integer, Integer, Integer>) null));
    }

    @Test
    void slidingCallbacksRemainLazyAndTerminalFailureClosesTheSource() {
        int factoryIndex = 0;
        for (Supplier<Stream<Integer>> factory : streams()) {
            AtomicInteger mapped = new AtomicInteger();
            AtomicInteger closed = new AtomicInteger();
            Stream<Integer> result = factory.get().onClose(closed::incrementAndGet).slidingMap(1, true, (a, b) -> {
                mapped.incrementAndGet();
                return a + b;
            });
            assertEquals(0, mapped.get());
            assertEquals(0, closed.get());
            List<Integer> values = result.toList();
            // Parallel sliding windows may arrive in either order.
            values.sort(Integer::compareTo);
            assertEquals(factoryIndex++ < 4 ? List.of(3, 5) : List.of(), values);
            assertEquals(values.size(), mapped.get());
            assertEquals(1, closed.get());

            RuntimeException callbackFailure = new RuntimeException("callback failure");
            AtomicInteger failureClosed = new AtomicInteger();
            Stream<Integer> terminal = factory.get().onClose(failureClosed::incrementAndGet);
            assertSame(callbackFailure, assertThrows(RuntimeException.class, () -> terminal.forEachTriple(1, (a, b, c) -> {
                throw callbackFailure;
            })));
            assertEquals(1, failureClosed.get());
        }
    }

    @Test
    void indicesValidateBeforeNoOpAndInvokeTheLookupOnlyWhenConsumed() {
        assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(null, 0, 1, null));
        AtomicInteger calls = new AtomicInteger();
        IntStream indices = IntStream.ofIndices("aba", 0, 1, (source, from) -> {
            calls.incrementAndGet();
            return source.indexOf('a', from);
        });
        assertEquals(0, calls.get());
        assertArrayEquals(new int[] { 0, 2 }, indices.toArray());
        assertEquals(2, calls.get());
        assertArrayEquals(new int[0], IntStream.ofIndices(null, 0, 1, (source, from) -> {
            throw new AssertionError("null source must not invoke lookup");
        }).toArray());
    }

    @Test
    void intervalAndMaxWaitValidateInSignatureOrderWithoutInvokingSuppliers() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> LongStream.interval(0, 0, null)).getMessage().contains("interval"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> LongStream.interval(0, 1, null)).getMessage().contains("unit"));
        assertThrows(IllegalArgumentException.class, () -> LongStream.interval(0, 999, TimeUnit.MICROSECONDS));
        try (LongStream interval = LongStream.interval(0, 1000, TimeUnit.MICROSECONDS)) {
            assertEquals(0, interval.limit(0).count());
        }

        AtomicInteger closed = new AtomicInteger();
        Stream<Integer> invalid = Stream.of(1).onClose(closed::incrementAndGet);
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> invalid.maxWait(Duration.ZERO, (Supplier<Integer>) null)).getMessage().contains("duration"));
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, () -> invalid.maxWait(null, (Supplier<Integer>) null));

        AtomicInteger supplied = new AtomicInteger();
        AtomicInteger lazyClosed = new AtomicInteger();
        Stream<Integer> lazy = Stream.of(1).onClose(lazyClosed::incrementAndGet).maxWait(Duration.ofMillis(1), () -> {
            supplied.incrementAndGet();
            return -1;
        });
        lazy.close();
        assertEquals(0, supplied.get());
        assertEquals(1, lazyClosed.get());
    }
}
