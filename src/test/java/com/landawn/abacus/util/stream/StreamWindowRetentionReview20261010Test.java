package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
// Window results and close behaviour are covered in the regular suite by StreamWindowCloseReview20261010Test.
@Tag("slow-test")
class StreamWindowRetentionReview20261010Test extends TestBase {
    @ParameterizedTest
    @ValueSource(strings = { "sliding", "slidingDelayed", "count", "countDelayed" })
    void closeDetachesLateResultsOverlapAndPendingState(final String kind) throws Exception {
        try (Stream<List<Event>> windows = windows(Stream.of(events(12)), kind, Collectors.toList(), false)) {
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            iterator.next();
            final List<Object> retained = retainedState(iterator);
            assertFalse(retained.isEmpty());
            windows.close();
            assertDetached(iterator, retained);
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "splitter", "wait" })
    void closeDetachesSplitterTimedState(final String kind) throws Exception {
        try (Stream<List<Event>> windows = windows(Stream.of(events(6)), kind, Collectors.toList(), true)) {
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            iterator.next();
            final List<Object> retained = retainedState(iterator);
            assertFalse(retained.isEmpty());
            windows.close();
            assertDetached(iterator, retained);
        }
    }

    @Test
    void delayedResultsDetachEvenWhenCancellationThrows() throws Exception {
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
        final List<Object> retained = retainedState(delayed);
        assertSame(failure, assertThrows(IllegalStateException.class, delayed::closeResource));
        assertDetached(delayed, retained);
        delayed.closeResource();
        assertEquals(1, closes.get());
        assertFalse(delayed.hasNext());
    }

    @ParameterizedTest
    @ValueSource(strings = { "sliding", "slidingDelayed" })
    void closeDoesNotMutateAnActiveWindowRead(final String kind) throws Exception {
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
                final List<Object> retained = retainedState(iterator);
                assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
                    windows.close();
                });
                assertDetached(iterator, retained);
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
    @ValueSource(strings = { "sliding", "count", "wait" })
    void bufferedReaderPublishedAfterCloseIsImmediatelyCancelled(final String kind) throws Exception {
        final CountDownLatch entering = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        final ObjIteratorEx<Event> values = events(Integer.MAX_VALUE);
        final IteratorStream<Event> source = new IteratorStream<>(values) {
            @Override
            ObjIteratorEx<Event> iteratorEx() {
                final ObjIteratorEx<Event> result = super.iteratorEx();
                entering.countDown();
                await(resume);
                return result;
            }
        };
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        ObjIteratorEx<?> raw = null;
        try (Stream<List<Event>> windows = windows(source, kind, Collectors.toList(), true)) {
            final ObjIteratorEx<List<Event>> iterator = windows.iteratorEx();
            raw = rawWindow(iterator);
            final Future<Boolean> read = worker.submit(iterator::hasNext);
            try {
                assertTrue(entering.await(5, TimeUnit.SECONDS));
                assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
                    windows.close();
                });
                resume.countDown();
                assertFalse(read.get(5, TimeUnit.SECONDS));
                final ObjIteratorEx<?> buffered = (ObjIteratorEx<?>) field(raw, "iter");
                assertNotNull(buffered);
                assertFalse(buffered.hasNext());
            } finally {
                resume.countDown();
            }
        } finally {
            if (raw != null) {
                // The baseline may create its buffer after close; clean it up even when an assertion fails.
                raw.closeResource();
            }
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
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

    private static Object field(final Object owner, final String name) throws Exception {
        final Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static List<Object> retainedState(final ObjIteratorEx<?> root) throws Exception {
        final List<Object> result = new ArrayList<>();
        for (final Object value : iteratorGraph(root)) {
            if (value instanceof ObjIteratorEx<?> iterator) {
                for (final Field field : iterator.getClass().getDeclaredFields()) {
                    if (Set.of("pending", "queue", "queueIter", "windowResultCacheForLateData", "timedNext", "timedFirst", "timedLast", "next")
                            .contains(field.getName())) {
                        field.setAccessible(true);
                        final Object retained = field.get(iterator);
                        if (retained != null) {
                            result.add(retained);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static ObjIteratorEx<?> rawWindow(final ObjIteratorEx<?> root) throws Exception {
        for (final Object value : iteratorGraph(root)) {
            if (value instanceof ObjIteratorEx<?> iterator) {
                for (final Field field : iterator.getClass().getDeclaredFields()) {
                    if (field.getName().equals("iter")) {
                        return iterator;
                    }
                }
            }
        }
        throw new AssertionError("Window iterator not found");
    }

    private static Set<Object> iteratorGraph(final ObjIteratorEx<?> root) throws Exception {
        // Traverse only iterator ownership edges, avoiding GC timing and unrelated source or callback captures.
        final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        final Deque<Object> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            final Object current = pending.removeFirst();
            if (!visited.add(current) || !(current instanceof ObjIteratorEx<?>)) {
                continue;
            }
            for (final Field field : current.getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    field.setAccessible(true);
                    final Object value = field.get(current);
                    if (value != null) {
                        pending.addLast(value);
                    }
                }
            }
        }
        return visited;
    }

    private static void assertDetached(final ObjIteratorEx<?> root, final List<Object> priorState) throws Exception {
        final Set<Object> remaining = iteratorGraph(root);
        for (final Object state : priorState) {
            assertFalse(remaining.contains(state), "Closed iterator retains window state: " + state.getClass());
        }
    }

    private record Event(long time) {
    }
}
