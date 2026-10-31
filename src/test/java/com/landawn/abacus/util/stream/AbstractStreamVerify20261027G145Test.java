package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.Pair;

/**
 * Verification tests for the sequential-only {@code combinations(1)} and {@code crossJoin} overloads on parallel streams.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AbstractStreamVerify20261027G145Test extends TestBase {

    // ---- deep review 2026-10-27 verify G145 begin ----

    private static Integer[] zeroTo(final int n) {
        final Integer[] a = new Integer[n];

        for (int i = 0; i < n; i++) {
            a[i] = i;
        }

        return a;
    }

    private static void assertSameParallelSettings(final Stream<?> source, final Stream<?> result, final String what) {
        final StreamBase s = (StreamBase) source;
        final StreamBase r = (StreamBase) result;

        assertTrue(result.isParallel(), what);
        assertEquals(s.maxThreadNum(), r.maxThreadNum(), what);
        assertEquals(s.splitStrategy(), r.splitStrategy(), what);
        assertSame(s.asyncExecutor(), r.asyncExecutor(), what);
        assertEquals(s.cancelUncompletedThreads(), r.cancelUncompletedThreads(), what);
    }

    // combinations(1) and the three crossJoin overloads return a stream with the source's parallel settings (thread count,
    // split strategy, executor, cancel flag), the same as the other combinations lengths; array- and iterator-backed sources.
    @Test
    public void testCombinationsOfOneAndCrossJoinKeepEveryParallelSetting() {
        final ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            final AsyncExecutor asyncExecutor = ((StreamBase) Stream.of(1).parallel(3, executor)).asyncExecutor();
            final Integer[] a = zeroTo(40);
            final List<String> b = Arrays.asList("x", "y");
            final List<Function<Stream<Integer>, Stream<?>>> ops = Arrays.asList(s -> s.combinations(1), s -> s.combinations(2), s -> s.combinations(0),
                    s -> s.combinations(a.length), s -> s.crossJoin(b), s -> s.crossJoin(b, Pair::of), s -> s.crossJoin(Stream.of(b), Pair::of));

            for (final boolean arrayBacked : new boolean[] { true, false }) {
                for (int i = 0; i < ops.size(); i++) {
                    final String what = "op#" + i + (arrayBacked ? " array" : " iterator");
                    final Stream<Integer> sequential = arrayBacked ? Stream.of(a) : Stream.of(Arrays.asList(a).iterator());
                    final Stream<Integer> source = ((StreamBase) sequential).parallel(3, BaseStream.SplitStrategy.ITERATOR, asyncExecutor,
                            true) instanceof Stream p ? p : null;
                    final Stream<?> result = ops.get(i).apply(source);

                    assertSameParallelSettings(source, result, what);
                    result.close();
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    // On a parallel stream crossJoin's function is now applied in encounter order (it ran on the worker threads of a
    // parallel flatMap before), while a downstream stage still runs on the worker threads.
    @Test
    public void testCrossJoinOnParallelStreamAppliesFunctionInEncounterOrder() {
        final Integer[] a = zeroTo(1000);
        final List<String> b = Arrays.asList("x", "y");
        final List<Integer> expectedCalls = new ArrayList<>();

        for (final Integer e : a) {
            expectedCalls.add(e);
            expectedCalls.add(e);
        }

        for (int run = 0; run < 3; run++) {
            final List<Integer> calls = Collections.synchronizedList(new ArrayList<>());
            final List<Pair<Integer, String>> joined = Stream.of(a).parallel(4).crossJoin(b, (t, u) -> {
                calls.add(t);
                return Pair.of(t, u);
            }).toList();

            assertEquals(-1, firstDifference(expectedCalls, calls), "first out-of-order call");
            assertEquals(2000, joined.size());

            final List<Integer> fromStream = Collections.synchronizedList(new ArrayList<>());
            Stream.of(a).parallel(4).crossJoin(Stream.of(b), (t, u) -> {
                fromStream.add(t);
                return t;
            }).toList();
            assertEquals(-1, firstDifference(expectedCalls, fromStream), "first out-of-order call");
        }

        final List<String> threads = Collections.synchronizedList(new ArrayList<>());
        assertEquals(2000, Stream.of(a).parallel(4).crossJoin(b).map(p -> {
            threads.add(Thread.currentThread().getName());
            return p;
        }).count());
        assertTrue(threads.stream().anyMatch(name -> !name.equals(Thread.currentThread().getName())), "downstream map ran on worker threads");
    }

    // A failing function or a failing right-hand stream propagates its own exception, and the source and the right-hand
    // stream are each closed exactly once - also when the joined stream is closed without being traversed.
    @Test
    public void testCrossJoinOnParallelStreamClosesBothSidesOnceOnFailure() {
        final Integer[] a = zeroTo(200);

        for (final boolean arrayBacked : new boolean[] { true, false }) {
            // function failure
            final AtomicInteger leftClosed = new AtomicInteger();
            final AtomicInteger rightClosed = new AtomicInteger();
            final IllegalStateException failure = new IllegalStateException("function failure");
            final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> source(a, arrayBacked, leftClosed).crossJoin(Stream.of("x", "y").onClose(rightClosed::incrementAndGet), (t, u) -> {
                        if (t == 77) {
                            throw failure;
                        }

                        return t;
                    }).count());
            assertSame(failure, thrown);
            assertEquals(1, leftClosed.get());
            assertEquals(1, rightClosed.get());

            // right-hand stream failure: read once, no suppressed "already terminated" from a second reader
            final AtomicInteger leftClosed2 = new AtomicInteger();
            final AtomicInteger rightClosed2 = new AtomicInteger();
            final IllegalArgumentException bFailure = new IllegalArgumentException("right-hand failure");
            final AtomicInteger bReads = new AtomicInteger();
            final IllegalArgumentException thrown2 = assertThrows(IllegalArgumentException.class,
                    () -> source(a, arrayBacked, leftClosed2).crossJoin(Stream.of("x", "y").onClose(rightClosed2::incrementAndGet).map(u -> {
                        bReads.incrementAndGet();
                        throw bFailure;
                    }), Pair::of).count());
            assertSame(bFailure, thrown2);
            assertEquals(0, thrown2.getSuppressed().length);
            assertEquals(1, bReads.get());
            assertEquals(1, leftClosed2.get());
            assertEquals(1, rightClosed2.get());

            // closed without traversal
            final AtomicInteger leftClosed3 = new AtomicInteger();
            final AtomicInteger rightClosed3 = new AtomicInteger();
            final Stream<Pair<Integer, String>> joined = source(a, arrayBacked, leftClosed3).crossJoin(Stream.of("x").onClose(rightClosed3::incrementAndGet),
                    Pair::of);
            joined.close();
            joined.close();
            assertEquals(1, leftClosed3.get());
            assertEquals(1, rightClosed3.get());
        }
    }

    private static int firstDifference(final List<Integer> expected, final List<Integer> actual) {
        for (int i = 0, n = Math.max(expected.size(), actual.size()); i < n; i++) {
            if (i >= expected.size() || i >= actual.size() || !expected.get(i).equals(actual.get(i))) {
                return i;
            }
        }

        return -1;
    }

    private static Stream<Integer> source(final Integer[] a, final boolean arrayBacked, final AtomicInteger closed) {
        return (arrayBacked ? Stream.of(a) : Stream.of(Arrays.asList(a).iterator())).onClose(closed::incrementAndGet).parallel(4);
    }

    // ---- deep review 2026-10-27 verify G145 end ----
}
