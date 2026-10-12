package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;

/**
 * Results and close behaviour of the time-, count- and splitter-based {@code window} stages, including closes that
 * race a read or come from a user callback. Whether a closed window releases its cached state is checked separately
 * by the slow-test class StreamWindowRetentionReview20261010Test.
 */
class StreamWindowCloseReview20261010Test extends TestBase {
    @ParameterizedTest
    @ValueSource(strings = { "sliding", "slidingDelayed", "count", "countDelayed", "splitter", "wait" })
    void closeEndsIteration(final String kind) {
        final boolean async = kind.equals("splitter") || kind.equals("wait");
        try (Stream<List<Event>> windows = windows(Stream.of(events(12)), kind, Collectors.toList(), async)) {
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            iterator.next();
            windows.close();
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @Test
    void delayedCloseFailureIsReportedOnceAndEndsIteration() {
        final IllegalStateException failure = new IllegalStateException("cancel");
        final AtomicInteger closes = new AtomicInteger();
        final ObjIteratorEx<Object> source = new ObjIteratorEx<>() {
            private int remaining = 4;

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            @Override
            public Object next() {
                remaining--;
                return new Object();
            }

            @Override
            public void closeResource() {
                closes.incrementAndGet();
                throw failure;
            }
        };
        final ObjIteratorEx<Object> delayed = Stream.releaseOnEviction(source, 2);
        assertTrue(delayed.hasNext());
        assertSame(failure, assertThrows(IllegalStateException.class, delayed::closeResource));
        delayed.closeResource();
        assertEquals(1, closes.get());
        assertFalse(delayed.hasNext());
    }

    @ParameterizedTest
    @ValueSource(strings = { "sliding", "slidingDelayed" })
    void closeDuringAnActiveReadKeepsItsResult(final String kind) throws Exception {
        final CountDownLatch finishing = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        final Collector<Event, List<Event>, List<Event>> collector = collecting(list -> {
            finishing.countDown();
            await(resume);
            return list;
        });
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        try (Stream<List<Event>> windows = windows(Stream.of(events(12)), kind, collector, false)) {
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            final Future<List<Event>> read = worker.submit(iterator::next);
            try {
                assertTrue(finishing.await(5, TimeUnit.SECONDS));
                assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
                    windows.close();
                });
                resume.countDown();
                assertEquals(List.of(new Event(1), new Event(6)), read.get(5, TimeUnit.SECONDS));
                assertFalse(iterator.hasNext());
            } finally {
                resume.countDown();
            }
        } finally {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "sliding", "count", "splitter", "wait" })
    void closingBeforeFirstReadDoesNotInitialize(final String kind) {
        final AtomicInteger suppliers = new AtomicInteger();
        final Collector<Event, List<Event>, List<Event>> collector = collectorWithSupplierAccessor(() -> {
            suppliers.incrementAndGet();
            return ArrayList::new;
        });
        try (Stream<List<Event>> windows = windows(Stream.of(events(12)), kind, collector, true)) {
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            windows.close();
            assertFalse(iterator.hasNext());
            assertEquals(0, suppliers.get());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "sliding", "count", "splitter", "wait" })
    void closingDuringCollectorInitializationPreventsBufferCreation(final String kind) throws Exception {
        final CountDownLatch entering = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        final Collector<Event, List<Event>, List<Event>> collector = collectorWithSupplierAccessor(() -> {
            entering.countDown();
            await(resume);
            return ArrayList::new;
        });
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        try (Stream<List<Event>> windows = windows(Stream.of(events(12)), kind, collector, true)) {
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            final Future<Boolean> read = worker.submit(iterator::hasNext);
            try {
                assertTrue(entering.await(5, TimeUnit.SECONDS));
                assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
                    windows.close();
                });
                resume.countDown();
                assertFalse(read.get(5, TimeUnit.SECONDS));
                assertFalse(iterator.hasNext());
            } finally {
                resume.countDown();
            }
        } finally {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void callbackCanCloseItsOwnWindowWithoutCorruptingTheResult() {
        final AtomicReference<Stream<List<Event>>> reference = new AtomicReference<>();
        final Collector<Event, List<Event>, List<Event>> collector = collecting(list -> {
            reference.get().close();
            return list;
        });
        try (Stream<List<Event>> windows = windows(Stream.of(events(6)), "slidingDelayed", collector, false)) {
            reference.set(windows);
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            assertEquals(List.of(new Event(1), new Event(6)), iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    void closingDuringASplitterCallbackStopsAnActiveRead() throws Exception {
        final CountDownLatch splitting = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        try (Stream<List<Event>> windows = Stream.of(events(1000)).window((first, previous, next, count) -> {
            splitting.countDown();
            await(resume);
            return true;
        }, () -> 0L, Collectors.toList())) {
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            final Future<List<Event>> read = worker.submit(iterator::next);
            try {
                assertTrue(splitting.await(5, TimeUnit.SECONDS));
                assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
                    windows.close();
                });
                resume.countDown();
                assertEquals(List.of(new Event(1), new Event(6)), read.get(5, TimeUnit.SECONDS));
                assertFalse(iterator.hasNext());
            } finally {
                resume.countDown();
            }
        } finally {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void splitterCanCloseItsOwnWindowWithoutReadingMoreInput() {
        final AtomicReference<Stream<List<Event>>> reference = new AtomicReference<>();
        try (Stream<List<Event>> windows = Stream.of(events(1000)).window((first, previous, next, count) -> {
            reference.get().close();
            return true;
        }, () -> 0L, Collectors.toList())) {
            reference.set(windows);
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            assertEquals(List.of(new Event(1), new Event(6)), iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    void delayedWindowsPreserveLateDataOrderAndNullResults() {
        final List<Long> input = List.of(1L, 11L, 21L, 15L, 31L);
        for (final boolean count : new boolean[] { false, true }) {
            final Stream.WindowHandler<Long, List<Long>> handler = Stream.WindowHandler.<Long, List<Long>> builder()
                    .timeExtractor(Long::longValue)
                    .cacheSizeForLateData(2)
                    .delayForLateData(true)
                    .onLateData((event, list) -> list.add(event))
                    .build();
            try (Stream<List<Long>> windows = count ? Stream.of(input).window(Duration.ofMillis(10), 100, () -> 0L, handler, Collectors.toList())
                    : Stream.of(input).window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, handler, Collectors.toList())) {
                assertEquals(List.of(List.of(1L), List.of(11L, 15L), List.of(21L), List.of(31L)), windows.toList());
            }
        }
        final Stream.WindowHandler<Long, Object> handler = Stream.WindowHandler.<Long, Object> builder()
                .timeExtractor(Long::longValue)
                .cacheSizeForLateData(2)
                .delayForLateData(true)
                .onLateData((event, result) -> {
                    // This collector intentionally produces null results.
                })
                .build();
        assertEquals(Arrays.asList(null, null),
                Stream.of(1L, 11L)
                        .window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, handler,
                                Collector.of(ArrayList::new, List::add, (a, b) -> a, list -> null))
                        .toList());
    }

    private static Stream<List<Event>> windows(final Stream<Event> source, final String kind, final Collector<Event, ?, List<Event>> collector,
            final boolean async) {
        if (kind.equals("splitter")) {
            return source.window((first, previous, next, count) -> count < 2, () -> 0L, collector);
        } else if (kind.equals("wait")) {
            return source.window((first, previous, count) -> 0L, (first, previous, next, count) -> count < 2, () -> 0L, collector);
        }
        final Stream.WindowHandler<Event, List<Event>> handler = Stream.WindowHandler.<Event, List<Event>> builder()
                .timeExtractor(Event::time)
                .cacheSizeForLateData(2)
                .delayForLateData(kind.endsWith("Delayed"))
                .onLateData((event, list) -> list.add(event))
                .build();
        return kind.startsWith("sliding") ? source.window(Duration.ofMillis(10), Duration.ofMillis(5), () -> 0L, handler, collector, async)
                : source.window(Duration.ofMillis(10), 1, () -> 0L, handler, collector, async);
    }

    private static Collector<Event, List<Event>, List<Event>> collecting(final Function<List<Event>, List<Event>> finisher) {
        return Collector.of(ArrayList::new, List::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, finisher);
    }

    private static Collector<Event, List<Event>, List<Event>> collectorWithSupplierAccessor(final Supplier<Supplier<List<Event>>> accessor) {
        return new Collector<>() {
            @Override
            public Supplier<List<Event>> supplier() {
                return accessor.get();
            }

            @Override
            public BiConsumer<List<Event>, Event> accumulator() {
                return List::add;
            }

            @Override
            public BinaryOperator<List<Event>> combiner() {
                return (left, right) -> {
                    left.addAll(right);
                    return left;
                };
            }

            @Override
            public Function<List<Event>, List<Event>> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        };
    }

    private static ObjIteratorEx<Event> events(final int limit) {
        return new ObjIteratorEx<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < limit;
            }

            @Override
            public Event next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return new Event(1 + 5L * cursor++);
            }
        };
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private record Event(long time) {
    }
}
