package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors;

public class SeqRateTest extends SeqTestSupport {

    @Test
    public void testRateLimited() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).rateLimited(1000).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).rateLimited(0.0));
    }

    @Test
    public void testDelay() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).delay(Duration.ofMillis(0)).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).delay((Duration) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).delay(Duration.ofMillis(-1)));
    }

    @Test
    public void testIntersperse() throws Exception {
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), Seq.of(1, 2, 3).intersperse(0).toList());
        assertTrue(Seq.<Integer, Exception> empty().intersperse(0).toList().isEmpty());
        assertEquals(Collections.singletonList(1), Seq.of(1).intersperse(0).toList());
    }

    @Test
    public void testStep() throws Exception {
        assertEquals(Arrays.asList(1, 3, 5), Seq.of(1, 2, 3, 4, 5).step(2).toList());
        assertEquals(Arrays.asList(1, 4), Seq.of(1, 2, 3, 4, 5).step(3).toList());
    }

    @Test
    public void testIndexed() throws Exception {
        List<Indexed<String>> indexed = Seq.of("a", "b").indexed().toList();
        assertEquals(Indexed.of("a", 0L), indexed.get(0));
        assertEquals(Indexed.of("b", 1L), indexed.get(1));
    }

    @Test
    public void testBuffered() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).buffered().toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).buffered(8).toList());
    }

    @Test
    public void testMinMax() throws Exception {
        assertEquals(Nullable.of(1), Seq.of(3, 1, 2).min(Comparator.naturalOrder()));
        assertEquals(Nullable.of(3), Seq.of(3, 1, 2).max(Comparator.naturalOrder()));
        assertEquals(Nullable.of(3), Seq.of(3, 1, 2).min(Comparator.reverseOrder()));
        assertEquals(Nullable.of(1), Seq.of(3, 1, 2).max(Comparator.reverseOrder()));
        assertTrue(Seq.<Integer, Exception> empty().min(Comparator.naturalOrder()).isEmpty());
        assertTrue(Seq.<Integer, Exception> empty().max(Comparator.naturalOrder()).isEmpty());
        assertEquals(Nullable.of("a"), Seq.of("bb", "a", "ccc").minBy(String::length));
        assertEquals(Nullable.of("ccc"), Seq.of("bb", "a", "ccc").maxBy(String::length));
        assertEquals(Nullable.of(1), Seq.of(3, 1, 2).sorted().min(Comparator.naturalOrder()));
        assertEquals(Nullable.of(3), Seq.of(3, 1, 2).sorted().max(Comparator.naturalOrder()));
    }

    @Test
    public void testMatchAndFind() throws Exception {
        assertTrue(Seq.of(1, 2, 3).anyMatch(x -> x == 2));
        assertFalse(Seq.of(1, 2, 3).anyMatch(x -> x == 9));
        assertTrue(Seq.of(2, 4, 6).allMatch(x -> x % 2 == 0));
        assertTrue(Seq.of(1, 3, 5).noneMatch(x -> x % 2 == 0));
        assertEquals(Nullable.of(2), Seq.of(1, 2, 3).findFirst(x -> x > 1));
        assertEquals(Nullable.of(2), Seq.of(1, 2, 3).findAny(x -> x > 1));
        assertEquals(Nullable.of(3), Seq.of(1, 2, 3).findLast(x -> x > 1));
        assertTrue(Seq.of(1, 2, 3).findFirst(x -> x > 9).isEmpty());
        assertEquals(Nullable.of(1), Seq.of(1, 2, 3).first());
        assertEquals(Nullable.of(1), Seq.of(1, 2, 3).findFirst());
        assertEquals(Nullable.of(1), Seq.of(1, 2, 3).findAny());
        assertEquals(Nullable.of(2), Seq.of(1, 2, 3).elementAt(1));
        assertTrue(Seq.of(1, 2, 3).elementAt(9).isEmpty());
        assertEquals(Nullable.of(1), Seq.of(1).onlyOne());
        assertTrue(Seq.<Integer, Exception> empty().onlyOne().isEmpty());
        assertThrows(TooManyElementsException.class, () -> Seq.of(1, 2).onlyOne());
    }

    @Test
    public void testKthLargestAndPercentiles() throws Exception {
        assertEquals(Nullable.of(4), Seq.of(1, 5, 3, 4, 2).kthLargest(2, Comparator.naturalOrder()));
        assertTrue(Seq.of(1, 2).kthLargest(5, Comparator.naturalOrder()).isEmpty());
        assertTrue(Seq.<Integer, Exception> empty().kthLargest(1, Comparator.naturalOrder()).isEmpty());
        assertTrue(Seq.of(1, 2, 3, 4, 5).percentiles().isPresent());
        assertTrue(Seq.<Integer, Exception> empty().percentiles().isEmpty());
    }

    @Test
    public void testCountSumAverageReduceCollect() throws Exception {
        assertEquals(3, Seq.of(1, 2, 3).count());
        assertEquals(0, Seq.empty().count());
        assertEquals(6L, Seq.of(1, 2, 3).sumInt(Integer::intValue));
        assertEquals(6L, Seq.of(1, 2, 3).sumLong(Integer::longValue));
        assertEquals(6.0, Seq.of(1, 2, 3).sumDouble(Integer::doubleValue));
        assertEquals(2.0, Seq.of(1, 2, 3).averageInt(Integer::intValue).orElseThrow());
        assertEquals(2.0, Seq.of(1, 2, 3).averageLong(Integer::longValue).orElseThrow());
        assertEquals(2.0, Seq.of(1, 2, 3).averageDouble(Integer::doubleValue).orElseThrow());
        assertTrue(Seq.<Integer, Exception> empty().averageInt(Integer::intValue).isEmpty());
        assertEquals(Nullable.of(6), Seq.of(1, 2, 3).reduce(Integer::sum));
        assertEquals(Integer.valueOf(6), Seq.of(1, 2, 3).reduce(0, Integer::sum));
        assertEquals(Integer.valueOf(42), Seq.<Integer, Exception> empty().reduce(42, Integer::sum));
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).collect(ArrayList::new, ArrayList::add));
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3).collect(Collectors.toSet()));
        assertEquals(3, Seq.of(1, 2, 3).collectThenApply(Collectors.toList(), List::size).intValue());
        AtomicInteger size = new AtomicInteger();
        Seq.of(1, 2, 3).collectThenAccept(Collectors.toList(), list -> size.set(list.size()));
        assertEquals(3, size.get());
    }

    @Test
    public void testJoinCastStreamSps() throws Exception {
        assertEquals("1, 2, 3", Seq.of(1, 2, 3).join(", "));
        assertEquals("[1, 2, 3]", Seq.of(1, 2, 3).join(", ", "[", "]"));
        Joiner joiner = Joiner.with(", ", "[", "]");
        Seq.of("a", "b", "c").joinTo(joiner);
        assertEquals("[a, b, c]", joiner.toString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));
        try {
            Seq.of(1, "hello", 3.0).println();
            assertEquals("[1, hello, 3.0]" + IOUtil.LINE_SEPARATOR, baos.toString());
        } finally {
            System.setOut(originalOut);
        }
        assertEquals(Arrays.asList(1, 2, 3), Seq.<Number, RuntimeException> of(Arrays.asList(1, 2, 3)).cast().toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).stream().toList());
        assertEquals(new HashSet<>(Arrays.asList(2, 4, 6)), new HashSet<>(Seq.of(1, 2, 3).sps(s -> s.map(x -> x * 2)).toList()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).sps(0, s -> s));
    }

    @Test
    public void testPartitionTo() throws Exception {
        Map<Boolean, List<Integer>> parts = Seq.of(1, 2, 3, 4).partitionTo(x -> x % 2 == 0);
        assertEquals(Arrays.asList(2, 4), parts.get(true));
        assertEquals(Arrays.asList(1, 3), parts.get(false));
    }

    @Test
    public void testApplyAndAcceptIfNotEmpty() throws Exception {
        assertEquals(Optional.of(3L), Seq.of(1, 2, 3).applyIfNotEmpty(Seq::count));
        assertTrue(Seq.<Integer, Exception> empty().applyIfNotEmpty(Seq::count).isEmpty());
        List<Integer> holder = new ArrayList<>();
        assertSame(OrElse.TRUE, Seq.of(1, 2, 3).acceptIfNotEmpty(s -> s.forEach(holder::add)));
        assertEquals(Arrays.asList(1, 2, 3), holder);
        assertSame(OrElse.FALSE, Seq.<Integer, Exception> empty().acceptIfNotEmpty(s -> holder.add(9)));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).applyIfNotEmpty(null));
    }

    @Test
    public void testClose() throws Exception {
        AtomicInteger closed = new AtomicInteger();
        Seq.of(1, 2, 3).onClose(closed::incrementAndGet).toList();
        assertEquals(1, closed.get());
        Seq<Integer, Exception> seq = Seq.of(1).onClose(closed::incrementAndGet);
        seq.close();
        seq.close();
        assertEquals(2, closed.get());
    }

    @Test
    public void testrunAsync() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);
        ContinuableFuture<Void> future = Seq.of(1, 2, 3).runAsync(seq -> seq.forEach(sum::addAndGet));
        future.get();
        assertEquals(6, sum.get());
    }

    @Test
    public void testrunAsyncWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
            AtomicInteger sum = new AtomicInteger(0);

            Seq<Integer, Exception> seq = Seq.of(data);

            ContinuableFuture<Void> future = seq.runAsync(s -> s.forEach(sum::addAndGet), executor);

            future.get();
            assertEquals(15, sum.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testrunAsyncClosesSeqOnCompletionAndFailure() throws Exception {
        AtomicInteger closeCount = new AtomicInteger();

        Seq.of(1).onClose(closeCount::incrementAndGet).runAsync(s -> {
        }).get();
        assertEquals(1, closeCount.get());

        ContinuableFuture<Void> failed = Seq.of(1).onClose(closeCount::incrementAndGet).runAsync(s -> {
            throw new IllegalStateException("boom");
        });

        assertThrows(Exception.class, failed::get);
        assertEquals(2, closeCount.get());

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Seq.of(1).onClose(closeCount::incrementAndGet).runAsync(s -> {
            }, executor).get();
            assertEquals(3, closeCount.get());

            failed = Seq.of(1).onClose(closeCount::incrementAndGet).runAsync(s -> {
                throw new IllegalStateException("boom");
            }, executor);

            assertThrows(Exception.class, failed::get);
            assertEquals(4, closeCount.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testrunAsyncNullAction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.runAsync(null));
    }

    @Test
    public void testcallAsync() throws Exception {
        ContinuableFuture<List<Integer>> future = Seq.of(1, 2, 3).callAsync(seq -> seq.toList());
        List<Integer> result = future.get();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testcallAsyncWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<String> data = Arrays.asList("hello", "world");
            Seq<String, Exception> seq = Seq.of(data);

            ContinuableFuture<String> future = seq.callAsync(s -> s.join(" "), executor);

            String result = future.get();
            assertEquals("hello world", result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testcallAsyncClosesSeqOnCompletionAndFailure() throws Exception {
        AtomicInteger closeCount = new AtomicInteger();

        assertEquals("done", Seq.of(1).onClose(closeCount::incrementAndGet).callAsync(s -> "done").get());
        assertEquals(1, closeCount.get());

        ContinuableFuture<String> failed = Seq.of(1).onClose(closeCount::incrementAndGet).callAsync(s -> {
            throw new IllegalStateException("boom");
        });

        assertThrows(Exception.class, failed::get);
        assertEquals(2, closeCount.get());

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            assertEquals("done", Seq.of(1).onClose(closeCount::incrementAndGet).callAsync(s -> "done", executor).get());
            assertEquals(3, closeCount.get());

            failed = Seq.of(1).onClose(closeCount::incrementAndGet).callAsync(s -> {
                throw new IllegalStateException("boom");
            }, executor);

            assertThrows(Exception.class, failed::get);
            assertEquals(4, closeCount.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testcallAsyncWithNullAction() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.callAsync(null));
    }

    @Test
    public void testAsyncTerminalWrappersCloseSeqWhenSchedulingIsRejected() {
        final RejectedExecutionException rejection = new RejectedExecutionException("expected");
        final AtomicInteger closeCount = new AtomicInteger();
        final Seq<Integer, RuntimeException> runSource = Seq.<Integer, RuntimeException> of(1).onClose(closeCount::incrementAndGet);

        assertSame(rejection, assertThrows(RejectedExecutionException.class, () -> runSource.runAsync(s -> s.count(), command -> {
            throw rejection;
        })));
        assertEquals(1, closeCount.get());
        assertThrows(IllegalStateException.class, runSource::count);

        final Seq<Integer, RuntimeException> callSource = Seq.<Integer, RuntimeException> of(1).onClose(closeCount::incrementAndGet);
        assertSame(rejection, assertThrows(RejectedExecutionException.class, () -> callSource.callAsync(s -> s.count(), command -> {
            throw rejection;
        })));
        assertEquals(2, closeCount.get());
        assertThrows(IllegalStateException.class, callSource::count);

        final IllegalStateException closeFailure = new IllegalStateException("close failed");
        final Seq<Integer, RuntimeException> closeFailingSource = Seq.<Integer, RuntimeException> of(1).onClose(() -> {
            throw closeFailure;
        });
        final RejectedExecutionException thrown = assertThrows(RejectedExecutionException.class, () -> closeFailingSource.runAsync(s -> s.count(), command -> {
            throw rejection;
        }));

        assertSame(rejection, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(closeFailure, thrown.getSuppressed()[0]);
        assertThrows(IllegalStateException.class, closeFailingSource::count);
    }

    @Test
    public void testAsyncTerminalWrappersPreserveTaskFailureWhenCloseFails() throws Exception {
        for (final boolean call : new boolean[] { false, true }) {
            for (final boolean customExecutor : new boolean[] { false, true }) {
                final IOException taskFailure = new IOException("task failed");
                final IllegalStateException closeFailure = new IllegalStateException("close failed");
                final AtomicInteger closeCount = new AtomicInteger();
                final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1).onClose(() -> {
                    closeCount.incrementAndGet();
                    throw closeFailure;
                });
                final ContinuableFuture<?> future;

                if (call) {
                    future = customExecutor ? source.callAsync(s -> {
                        throw taskFailure;
                    }, Runnable::run) : source.callAsync(s -> {
                        throw taskFailure;
                    });
                } else {
                    future = customExecutor ? source.runAsync(s -> {
                        throw taskFailure;
                    }, Runnable::run) : source.runAsync(s -> {
                        throw taskFailure;
                    });
                }

                final ExecutionException thrown = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));

                assertSame(taskFailure, thrown.getCause());
                assertArrayEquals(new Throwable[] { closeFailure }, taskFailure.getSuppressed());
                assertEquals(1, closeCount.get());
                assertThrows(IllegalStateException.class, source::count);
            }
        }
    }

    @Test
    public void testAsyncTerminalWrappersPreserveErrorsAndHandleCloseOnlyOrRepeatedFailure() throws Exception {
        for (final boolean call : new boolean[] { false, true }) {
            for (final boolean customExecutor : new boolean[] { false, true }) {
                for (int scenario = 0; scenario < 3; scenario++) {
                    final IllegalStateException closeFailure = new IllegalStateException("close failed");
                    final Throwable taskFailure = scenario == 0 ? new AssertionError("task failed") : scenario == 1 ? closeFailure : null;
                    final AtomicInteger closeCount = new AtomicInteger();
                    final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1).onClose(() -> {
                        closeCount.incrementAndGet();
                        throw closeFailure;
                    });
                    final Throwables.Function<Seq<Integer, RuntimeException>, String, Exception> action = s -> {
                        if (taskFailure instanceof Error error) {
                            throw error;
                        } else if (taskFailure instanceof RuntimeException exception) {
                            throw exception;
                        }
                        return "done";
                    };
                    final ContinuableFuture<?> future = call ? customExecutor ? source.callAsync(action, Runnable::run) : source.callAsync(action)
                            : customExecutor ? source.runAsync(s -> action.apply(s), Runnable::run) : source.runAsync(s -> action.apply(s));
                    final ExecutionException thrown = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
                    assertSame(taskFailure == null ? closeFailure : taskFailure, thrown.getCause());
                    assertArrayEquals(scenario == 0 ? new Throwable[] { closeFailure } : new Throwable[0], thrown.getCause().getSuppressed());
                    assertEquals(1, closeCount.get());
                    assertThrows(IllegalStateException.class, source::count);
                }
            }
        }
    }

    @Test
    public void testBufferedClosesSourceWhenClosedWithoutConsumption() throws Exception {
        // regression: buffered() attached the source's close handlers only to the lazily created inner
        // sequence, so closing the result before pulling any element leaked the source's resources
        final AtomicBoolean closed = new AtomicBoolean(false);
        final Seq<Integer, Exception> buf = Seq.<Integer, Exception> of(1, 2, 3).onClose(() -> closed.set(true)).buffered();
        buf.close();
        assertTrue(closed.get());

        // normal consumption still yields all elements and closes exactly once
        final AtomicInteger closeCount = new AtomicInteger();
        final List<Integer> result = Seq.<Integer, Exception> of(1, 2, 3).onClose(closeCount::incrementAndGet).buffered().toList();
        assertEquals(CommonUtil.asList(1, 2, 3), result);
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testBufferedRestoresCheckedException() {
        // regression: a checked exception from the source was rethrown by the buffered consumer as a
        // RuntimeException wrapper, breaking the declared "throws E" contract of the pipeline
        final Seq<Integer, IOException> seq = Seq.<Integer, IOException> of(1, 2, 3).map(i -> {
            if (i == 2) {
                throw new IOException("boom");
            }

            return i;
        });

        assertThrows(IOException.class, () -> seq.buffered().toList());
    }

    @Test
    public void testBufferedRelaysProducerError() {
        final Seq<Integer, RuntimeException> seq = Seq.<Integer, RuntimeException> of(1, 2, 3).map(i -> {
            if (i == 2) {
                throw new AssertionError("boom");
            }

            return i;
        });

        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> seq.buffered(1).toList());
        assertTrue(thrown.getCause() instanceof AssertionError);
        assertEquals("boom", thrown.getCause().getMessage());
    }

    @Test
    public void testAverageLongDoesNotOverflowIntegralSum() throws Exception {
        assertEquals(Long.MAX_VALUE, Seq.of(Long.MAX_VALUE, Long.MAX_VALUE).averageLong(Long::longValue).getAsDouble());
        assertEquals(Long.MIN_VALUE, Seq.of(Long.MIN_VALUE, Long.MIN_VALUE).averageLong(Long::longValue).getAsDouble());
        assertEquals(-0.5d, Seq.of(Long.MAX_VALUE, Long.MIN_VALUE).averageLong(Long::longValue).getAsDouble());
    }

    @Test
    public void testRateLimitedNaNClosesSequenceOnValidationFailure() {
        final AtomicBoolean closed = new AtomicBoolean(false);
        final Seq<Integer, RuntimeException> seq = Seq.<Integer, RuntimeException> of(1, 2, 3).onClose(() -> closed.set(true));

        assertThrows(IllegalArgumentException.class, () -> seq.rateLimited(Double.NaN));
        assertTrue(closed.get());
    }

    @Test
    public void testRepeatIteratorOptimizedCountAndExhaustion() throws Exception {
        final Throwables.Iterator<String, RuntimeException> iter = Seq.<String, RuntimeException> repeat("x", Long.MAX_VALUE).iteratorEx();

        iter.advance(Long.MAX_VALUE - 2);
        assertEquals("x", iter.next());
        assertEquals(1L, iter.count());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testDebounce_emptyEmitsNothing() throws Exception {
        assertTrue(Seq.<Integer, RuntimeException> empty().debounce(Duration.ofSeconds(1)).toList().isEmpty());
    }

    @Test
    public void testDebounce_singleElementSurvives() throws Exception {
        assertEquals(CommonUtil.asList(42), Seq.of(42).debounce(Duration.ofSeconds(1)).toList());
    }

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() throws Exception {
        assertEquals(CommonUtil.asList(5), Seq.of(1, 2, 3, 4, 5).debounce(Duration.ofSeconds(1)).toList());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() throws Exception {
        assertEquals(CommonUtil.asList(1, 2, 3), Seq.of(1, 2, 3).delay(Duration.ofMillis(60)).debounce(Duration.ofMillis(20)).toList());
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).debounce((Duration) null).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).debounce(Duration.ofMillis(0)).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).debounce(Duration.ofMillis(-100)).toList());
    }

    @Test
    public void testDefer_supplierInvokedExactlyOnceWhenTraversed() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        final List<Integer> result = Seq.<Integer, Exception> defer(() -> {
            calls.incrementAndGet();
            return Seq.of(1, 2, 3);
        }).toList();

        assertEquals(CommonUtil.asList(1, 2, 3), result);
        assertEquals(1, calls.get());
    }

    @Test
    public void testDefer_supplierNotInvokedWhenClosedWithoutTraversal() {
        final AtomicInteger calls = new AtomicInteger();

        final Seq<Integer, Exception> seq = Seq.defer(() -> {
            calls.incrementAndGet();
            return Seq.of(1, 2, 3);
        });
        seq.close();

        assertEquals(0, calls.get());
    }

    @Test
    public void testDefer_supplierThatRefusesASecondCallStillWorks() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        final List<Integer> result = Seq.<Integer, Exception> defer(() -> {
            if (calls.incrementAndGet() > 1) {
                throw new IllegalStateException("supplier must only be called once");
            }
            return Seq.of(1, 2, 3);
        }).toList();

        assertEquals(CommonUtil.asList(1, 2, 3), result);
    }

    @Test
    public void testDefer_closesTheSuppliedSequence() throws Exception {
        final MutableBoolean closed = MutableBoolean.of(false);

        Seq.<Integer, Exception> defer(() -> Seq.<Integer, Exception> of(1, 2).onClose(closed::setTrue)).toList();

        assertTrue(closed.value());
    }

    @Test
    public void testDefer_untraversedCloseDoesNotCreateTheSuppliedSequence() {
        final MutableBoolean closed = MutableBoolean.of(false);

        final Seq<Integer, Exception> seq = Seq.defer(() -> Seq.<Integer, Exception> of(1, 2).onClose(closed::setTrue));
        seq.close();

        assertFalse(closed.value(), "the supplied sequence was never created, so there is nothing to close");
    }

    @Test
    public void testDefer_nullResultIsTreatedAsEmpty() throws Exception {
        assertEquals(CommonUtil.emptyList(), Seq.<Integer, Exception> defer(() -> null).toList());
    }

    @Test
    public void testDefer_rejectsNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Seq.defer(null));
    }

    @Test
    public void testListFiles_rejectsNullParentPathWithIAE() {
        assertThrows(IllegalArgumentException.class, () -> Seq.listFiles(null));
        assertThrows(IllegalArgumentException.class, () -> Seq.listFiles(null, true));
        assertThrows(IllegalArgumentException.class, () -> Seq.listFiles(null, false));
    }

    @Test
    public void testListFiles_recursiveDoesNotFollowDirectorySymlinks() throws Exception {
        final Path root = tempDir.resolve("symlink-root");
        final Path child = root.resolve("child");
        Files.createDirectories(child);
        Files.write(child.resolve("leaf.txt"), CommonUtil.asList("x"));

        try {
            // A link that points back at its own ancestor: following it would loop forever.
            Files.createSymbolicLink(child.resolve("loop"), root);
        } catch (final UnsupportedOperationException | IOException | SecurityException e) {
            // Creating symlinks needs a privilege that may be unavailable (typically on Windows).
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "symbolic links are not available here: " + e);
        }

        final List<String> names = Seq.listFiles(root.toFile(), true).map(File::getName).toList();

        // The link itself is reported, but the traversal terminates instead of descending through it.
        assertTrue(names.contains("child"));
        assertTrue(names.contains("leaf.txt"));
        assertTrue(names.contains("loop"));
        assertEquals(3, names.size(), "unexpected entries: " + names);
    }

    @Test
    public void testListFiles_nonRecursiveAndRecursiveStillList() throws Exception {
        final Path root = tempDir.resolve("listfiles-plain");
        final Path sub = root.resolve("sub");
        Files.createDirectories(sub);
        Files.write(root.resolve("a.txt"), CommonUtil.asList("a"));
        Files.write(sub.resolve("b.txt"), CommonUtil.asList("b"));

        assertEquals(CommonUtil.asSet("a.txt", "sub"), CommonUtil.newHashSet(Seq.listFiles(root.toFile()).map(File::getName).toList()));
        assertEquals(CommonUtil.asSet("a.txt", "sub", "b.txt"), CommonUtil.newHashSet(Seq.listFiles(root.toFile(), true).map(File::getName).toList()));
        assertEquals(0, Seq.listFiles(root.resolve("missing").toFile(), true).count());
    }

    @Test
    public void testPrintln_closesTheSequence() throws Exception {
        final Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.println();

        assertThrows(IllegalStateException.class, seq::toList);
    }

    @Test
    public void testPrintln_runsTheCloseHandlers() throws Exception {
        final MutableBoolean closed = MutableBoolean.of(false);
        Seq.<Integer, Exception> of(1, 2).onClose(closed::setTrue).println();

        assertTrue(closed.value());
    }

    @Test
    public void testNullDownstreamCollectorRejectedEagerly() {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).groupBy(x -> x, (Collector<Integer, ?, Long>) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).groupBy(x -> x, (Collector<Integer, ?, Long>) null, Suppliers.ofMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).groupBy(x -> x, x -> x, (Collector<Integer, ?, Long>) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).groupBy(x -> x, x -> x, (Collector<Integer, ?, Long>) null, Suppliers.ofMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).partitionBy(x -> x > 1, (Collector<Integer, ?, Long>) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).groupTo(x -> x, (Collector<Integer, ?, Long>) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).groupTo(x -> x, (Collector<Integer, ?, Long>) null, Suppliers.ofMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).groupTo(x -> x, x -> x, (Collector<Integer, ?, Long>) null));
    }

    @Test
    public void testNullDownstreamCollectorClosesTheSequence() {
        final MutableBoolean closed = MutableBoolean.of(false);
        final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2).onClose(closed::setTrue);

        assertThrows(IllegalArgumentException.class, () -> seq.groupBy(x -> x, (Collector<Integer, ?, Long>) null));
        assertTrue(closed.value());
    }

    @Test
    public void testDownstreamCollectorOperationsStillWork() throws Exception {
        assertEquals(CommonUtil.asList("1=1", "2=1"), Seq.of(1, 2).groupBy(x -> x, Collectors.countingToInt()).map(String::valueOf).toList());
        assertEquals(CommonUtil.asList("false=[1]", "true=[2, 3]"), Seq.of(1, 2, 3).partitionBy(x -> x > 1).map(String::valueOf).toList());
        assertEquals(2, Seq.of(1, 2).groupTo(x -> x, Collectors.countingToInt()).size());
    }

    @Test
    public void testPrependAppendOptional_rejectNullWithIAE() {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).prepend((Optional<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).append((Optional<Integer>) null));
    }

    @Test
    public void testPrependAppendOptional_stillWork() throws Exception {
        assertEquals(CommonUtil.asList(0, 1, 2), Seq.of(1, 2).prepend(Optional.of(0)).toList());
        assertEquals(CommonUtil.asList(1, 2, 3), Seq.of(1, 2).append(Optional.of(3)).toList());
        assertEquals(CommonUtil.asList(1, 2), Seq.of(1, 2).prepend(Optional.<Integer> empty()).toList());
        assertEquals(CommonUtil.asList(1, 2), Seq.of(1, 2).append(Optional.<Integer> empty()).toList());
    }

    @Test
    public void testDifferenceWithMapper_appliesMapperToEveryElement() throws Exception {
        final AtomicInteger differenceCalls = new AtomicInteger();
        final List<Integer> difference = Seq.of(1, 2, 3, 4, 5).difference(x -> {
            differenceCalls.incrementAndGet();
            return x;
        }, CommonUtil.asList(1)).toList();

        final AtomicInteger intersectionCalls = new AtomicInteger();
        Seq.of(1, 2, 3, 4, 5).intersection(x -> {
            intersectionCalls.incrementAndGet();
            return x;
        }, CommonUtil.asList(1)).toList();

        assertEquals(CommonUtil.asList(2, 3, 4, 5), difference);
        assertEquals(5, differenceCalls.get());
        assertEquals(intersectionCalls.get(), differenceCalls.get());
    }

    @Test
    public void testDifferenceWithMapper_multisetSemanticsUnchanged() throws Exception {
        assertEquals(CommonUtil.asList("Alice30", "Bob35"),
                Seq.of("Alice25", "Alice30", "Bob35").difference(s -> s.substring(0, s.length() - 2), CommonUtil.asList("Alice", "Charlie")).toList());
        assertEquals(CommonUtil.asList(1, 2), Seq.of(1, 2).difference(x -> x, CommonUtil.<Integer> emptyList()).toList());
    }

    @Test
    public void testSorted_skipsRedundantResortForEquivalentComparators() throws Exception {
        // The redundant sort is still skipped, but the result is a derived sequence rather than the receiver:
        // every intermediate operation now returns a new instance linked back to its source.
        final Seq<Integer, Exception> naturalOrdered = Seq.of(3, 1, 2).sorted(Comparators.naturalOrder());
        assertNotSame(naturalOrdered, naturalOrdered.sorted());

        final Seq<Integer, Exception> defaultSorted = Seq.of(3, 1, 2).sorted();
        assertNotSame(defaultSorted, defaultSorted.sorted(Comparators.naturalOrder()));

        assertEquals(CommonUtil.asList(1, 2, 3), Seq.of(3, 1, 2).sorted().sorted().toList());
        assertEquals(CommonUtil.asList(3, 2, 1), Seq.of(3, 1, 2).sorted().reverseSorted().toList());
        // A genuinely different comparator must still re-sort.
        assertEquals(CommonUtil.asList("a", "b", "c"), Seq.of("c", "a", "b").sorted(Comparator.comparingInt(String::length)).sorted().toList());
        // The no-op keeps the `sorted` flag, so min(..)/max(..) still take their shortcut.
        assertEquals(Nullable.of(1), Seq.of(3, 1, 2).sorted().sorted().min(Comparators.<Integer> naturalOrder()));
        assertEquals(Nullable.of(3), Seq.of(3, 1, 2).sorted().sorted().max(Comparators.<Integer> naturalOrder()));
    }

    @Test
    public void testHasMatchCountBetween_argumentValidationOrder() {
        final IllegalArgumentException negativeAtMost = assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).hasMatchCountBetween(0, -1, x -> true));
        assertTrue(negativeAtMost.getMessage().contains("atMost"), negativeAtMost.getMessage());

        final IllegalArgumentException inverted = assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).hasMatchCountBetween(3, 1, x -> true));
        assertTrue(inverted.getMessage().contains("must be <="), inverted.getMessage());

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).hasMatchCountBetween(-1, 1, x -> true));
    }

    @Test
    public void testHasMatchCountBetween_stillMatches() throws Exception {
        assertTrue(Seq.of(1, 2, 3).hasMatchCountBetween(1, 2, x -> x > 1));
        assertTrue(Seq.of(1, 2, 3).hasMatchCountBetween(0, 0, x -> x > 9));
        assertFalse(Seq.of(1, 2, 3).hasMatchCountBetween(3, 3, x -> x > 1));
    }

    @Test
    public void testOnlyOne_abbreviatesHugeElementsInTheMessage() {
        final String big = Strings.repeat("x", 5000);

        final TooManyElementsException ex = assertThrows(TooManyElementsException.class, () -> Seq.of(big, big + "y").onlyOne());
        assertTrue(ex.getMessage().length() < 400, "message length was " + ex.getMessage().length());
    }

    @Test
    public void testOnlyOne_shortMessageIsUnchanged() throws Exception {
        final TooManyElementsException ex = assertThrows(TooManyElementsException.class, () -> Seq.of("a", "b").onlyOne());
        assertEquals("There are at least two elements: a, b", ex.getMessage());

        assertEquals(Nullable.of("a"), Seq.of("a").onlyOne());
        assertEquals(Nullable.empty(), Seq.<String, Exception> empty().onlyOne());
    }

    @Test
    public void testOnlyOne_handlesANullSecondElement() {
        final TooManyElementsException ex = assertThrows(TooManyElementsException.class, () -> Seq.of("a", (String) null).onlyOne());
        assertEquals("There are at least two elements: a, null", ex.getMessage());
    }

    @Test
    public void testMinMax_nullExtremeIsAPresentNull() throws Exception {
        // A null winner is a PRESENT Nullable holding null; empty is reserved for "no element" (Seq rule, 2026-09-20).
        assertTrue(Seq.of((Integer) null, 1, 2).min(Comparators.nullsFirst()).isNull());
        assertTrue(Seq.of((Integer) null, 1, 2).max(Comparators.nullsLast()).isNull());
        assertTrue(Seq.of((Integer) null, (Integer) null).minBy(x -> (Comparable) x).isNull());
        assertTrue(Seq.of((Integer) null, (Integer) null).maxBy(x -> (Comparable) x).isNull());
    }

    @Test
    public void testMinByMaxBy_nullsAreOrderedOutOfTheWay() throws Exception {
        assertEquals(Nullable.of(1), Seq.of((Integer) null, 1, 2).minBy(x -> (Comparable) x));
        assertEquals(Nullable.of(2), Seq.of((Integer) null, 1, 2).maxBy(x -> (Comparable) x));
    }

    @Test
    public void testTop_materializesTheWholeUpstreamOnFirstAccess() throws Exception {
        final AtomicInteger pulls = new AtomicInteger();
        Seq.<Integer, Exception> of(1, 2, 3, 4, 5).onEach(x -> pulls.incrementAndGet()).top(2).first();
        assertEquals(5, pulls.get());

        pulls.set(0);
        Seq.<Integer, Exception> of(1, 2, 3, 4, 5).onEach(x -> pulls.incrementAndGet()).top(2, Comparators.naturalOrder()).first();
        assertEquals(5, pulls.get());

        // Contrast: skipLast(..) is genuinely lazy and must not drain the upstream to produce its first element.
        pulls.set(0);
        Seq.<Integer, Exception> of(1, 2, 3, 4, 5).onEach(x -> pulls.incrementAndGet()).skipLast(2).first();
        assertTrue(pulls.get() < 5, "skipLast should stay lazy but pulled " + pulls.get());
    }

    @Test
    public void testTakeLast_closesTheUpstreamBeforeEmittingItsFirstElement() throws Exception {
        final MutableBoolean sourceClosed = MutableBoolean.of(false);
        final List<Boolean> closedWhileTraversing = new ArrayList<>();

        Seq.<Integer, Exception> of(1, 2, 3, 4, 5).onClose(sourceClosed::setTrue).takeLast(2).forEach(x -> closedWhileTraversing.add(sourceClosed.value()));

        assertEquals(CommonUtil.asList(true, true), closedWhileTraversing);
    }

    // top(n) for n > 0 drains and CLOSES the upstream inside its lazy init(), exactly as takeLast(..) does - its
    // javadoc used to claim "will not close the sequence", which holds only for n == 0.
    @Test
    public void testTop_closesTheUpstreamBeforeEmittingItsFirstElement() throws Exception {
        final MutableBoolean sourceClosed = MutableBoolean.of(false);
        final List<Boolean> closedWhileTraversing = new ArrayList<>();

        Seq.<Integer, Exception> of(1, 2, 3, 4, 5).onClose(sourceClosed::setTrue).top(2).forEach(x -> closedWhileTraversing.add(sourceClosed.value()));

        assertEquals(CommonUtil.asList(true, true), closedWhileTraversing);

        final MutableBoolean comparatorSourceClosed = MutableBoolean.of(false);
        final List<Boolean> comparatorClosedWhileTraversing = new ArrayList<>();

        Seq.<Integer, Exception> of(1, 2, 3, 4, 5)
                .onClose(comparatorSourceClosed::setTrue)
                .top(2, Comparators.naturalOrder())
                .forEach(x -> comparatorClosedWhileTraversing.add(comparatorSourceClosed.value()));

        assertEquals(CommonUtil.asList(true, true), comparatorClosedWhileTraversing);

        // n == 0 is the exception the javadoc calls out: nothing is drained, so nothing is closed early either.
        final MutableBoolean zeroSourceClosed = MutableBoolean.of(false);
        final Seq<Integer, Exception> zero = Seq.<Integer, Exception> of(1, 2, 3).onClose(zeroSourceClosed::setTrue).top(0);
        assertFalse(zeroSourceClosed.value());
        zero.close();
        assertTrue(zeroSourceClosed.value());
    }

    @Test
    public void testDistinct_staysLazyAndShortCircuits() throws Exception {
        // distinct() only retains the keys seen so far - it must not have to drain the source first.
        final AtomicInteger pulls = new AtomicInteger();
        assertEquals(Nullable.of(1), Seq.<Integer, Exception> of(1, 2, 3, 4, 5).onEach(x -> pulls.incrementAndGet()).distinct().first());
        assertEquals(1, pulls.get());

        pulls.set(0);
        assertEquals(Nullable.of(1), Seq.<Integer, Exception> of(1, 2, 3, 4, 5).onEach(x -> pulls.incrementAndGet()).distinctBy(x -> x).first());
        assertEquals(1, pulls.get());
    }

    @Test
    public void testMaterializingIntermediatesStillProduceTheRightElements() throws Exception {
        assertEquals(CommonUtil.asList(4, 5), Seq.of(1, 2, 3, 4, 5).takeLast(2).toList());
        assertEquals(CommonUtil.asList(4, 5), Seq.of(1, 2, 3, 4, 5).last(2).toList());
        assertEquals(CommonUtil.asList(1, 2, 3), Seq.of(1, 2, 3, 4, 5).skipLast(2).toList());
        assertEquals(CommonUtil.asSet(3, 4, 5), CommonUtil.newHashSet(Seq.of(5, 1, 4, 2, 3).top(3).toList()));
        assertEquals(CommonUtil.asSet(4, 5), CommonUtil.newHashSet(Seq.of(5, 1, 4, 2, 3).top(2, Comparators.naturalOrder()).toList()));
    }

    @Test
    public void testStepResumesUnfinishedGapAfterSourceFailure() throws IOException {
        for (boolean failHasNext : new boolean[] { false, true }) {
            for (boolean directNext : new boolean[] { false, true }) {
                Throwables.Iterator<Integer, IOException> source = new Throwables.Iterator<>() {
                    private int index;
                    private boolean failed;

                    private void failOnce() throws IOException {
                        if (index == 2 && !failed) {
                            failed = true;
                            throw new IOException("source failed partway through the step");
                        }
                    }

                    @Override
                    public boolean hasNext() throws IOException {
                        if (failHasNext) {
                            failOnce();
                        }
                        return index < 7;
                    }

                    @Override
                    public Integer next() throws IOException {
                        if (!failHasNext) {
                            failOnce();
                        }
                        if (index >= 7) {
                            throw new java.util.NoSuchElementException();
                        }
                        return index++;
                    }
                };
                try (Seq<Integer, IOException> stepped = Seq.of(source).step(3)) {
                    Throwables.Iterator<Integer, IOException> iterator = stepped.iteratorEx();
                    assertEquals(0, iterator.next());
                    if (directNext) {
                        assertThrows(IOException.class, iterator::next);
                    } else {
                        assertThrows(IOException.class, iterator::hasNext);
                    }
                    assertEquals(3, iterator.next());
                    assertTrue(iterator.hasNext());
                    assertEquals(6, iterator.next());
                    assertFalse(iterator.hasNext());
                    assertThrows(java.util.NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    // --- G13-004 (doc): takeLast(0)/last(0)/top(0)/top(0, cmp) short-circuit to limit(0), which never touches the
    // --- upstream - so the "all elements are consumed" / @TerminalOpTriggered wording does not hold for n == 0.
    @Test
    public void testTakeLastAndTopWithZero_doNotTraverseTheUpstream() throws Exception {
        final AtomicInteger pulls = new AtomicInteger();

        assertEquals(0, Seq.<Integer, Exception> of(1, 2, 3).onEach(x -> pulls.incrementAndGet()).takeLast(0).count());
        assertEquals(0, pulls.get());

        pulls.set(0);
        assertEquals(0, Seq.<Integer, Exception> of(1, 2, 3).onEach(x -> pulls.incrementAndGet()).last(0).count());
        assertEquals(0, pulls.get());

        pulls.set(0);
        assertEquals(0, Seq.<Integer, Exception> of(1, 2, 3).onEach(x -> pulls.incrementAndGet()).top(0).count());
        assertEquals(0, pulls.get());

        pulls.set(0);
        assertEquals(0, Seq.<Integer, Exception> of(1, 2, 3).onEach(x -> pulls.incrementAndGet()).top(0, Comparators.naturalOrder()).count());
        assertEquals(0, pulls.get());

        // Contrast: for n > 0 the whole upstream really is consumed.
        pulls.set(0);
        assertEquals(1, Seq.<Integer, Exception> of(1, 2, 3).onEach(x -> pulls.incrementAndGet()).takeLast(1).count());
        assertEquals(3, pulls.get());

        // The upstream is still released - by the terminal operation, or by closing the returned sequence.
        final AtomicInteger closed = new AtomicInteger();
        Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet).takeLast(0).count();
        assertEquals(1, closed.get());

        closed.set(0);
        final Seq<Integer, Exception> untraversed = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet).takeLast(0);
        assertEquals(0, closed.get());
        untraversed.close();
        assertEquals(1, closed.get());
    }
}
