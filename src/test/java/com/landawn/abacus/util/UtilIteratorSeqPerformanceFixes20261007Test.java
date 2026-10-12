package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class UtilIteratorSeqPerformanceFixes20261007Test extends TestBase {
    @Test
    void pairMergeReleasesEveryConsumedSlotIncludingNullValues() throws Exception {
        for (final MergeResult selection : MergeResult.values()) {
            final ObjIterator<Integer> iter = Iterators.merge(Arrays.asList(null, 1, 2).iterator(), Arrays.asList(3, null, 4).iterator(), (a, b) -> selection);
            final List<Integer> actual = new ArrayList<>();
            while (iter.hasNext()) {
                actual.add(iter.next());
                assertConsumedSlotsEmpty(iter);
            }
            assertEquals(selection == MergeResult.TAKE_FIRST ? Arrays.asList(null, 1, 2, 3, null, 4) : Arrays.asList(3, null, 4, null, 1, 2), actual);
        }
    }

    @Test
    void leftFoldRetainsItsSelectorSequenceAndReleasesConsumedSlots() throws Exception {
        final List<String> calls = new ArrayList<>();
        final ObjIterator<Integer> iter = Iterators.merge(List.of(List.of(1, 4).iterator(), List.of(2, 5).iterator(), List.of(3, 6).iterator()), (a, b) -> {
            calls.add(a + ":" + b);
            return a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        for (int expected = 1; expected <= 6; expected++) {
            assertEquals(expected, iter.next());
            assertConsumedSlotsEmpty(iter);
        }
        assertFalse(iter.hasNext());
        assertEquals(List.of("1:2", "1:3", "4:2", "2:3", "4:5", "4:3", "4:6", "5:6"), calls);
    }

    @Test
    void seqMergeReleasesEveryConsumedHeadIncludingTheThreeWayInnerMerge() throws Exception {
        for (final MergeResult selection : MergeResult.values()) {
            final Throwables.Iterator<Integer, RuntimeException> iter = Seq
                    .<Integer, RuntimeException> merge(Arrays.asList(null, 1, 2).iterator(), Arrays.asList(3, null, 4).iterator(), (a, b) -> selection)
                    .iteratorEx();
            final List<Integer> actual = new ArrayList<>();
            while (iter.hasNext()) {
                actual.add(iter.next());
                assertConsumedSlotsEmpty(iter);
            }
            assertEquals(selection == MergeResult.TAKE_FIRST ? Arrays.asList(null, 1, 2, 3, null, 4) : Arrays.asList(3, null, 4, null, 1, 2), actual);
        }

        // The three-way factory nests a two-way merge as the first source of another one; both levels release.
        final Throwables.Iterator<Integer, RuntimeException> outer = Seq
                .<Integer, RuntimeException> merge(List.of(1, 4).iterator(), List.of(2, 5).iterator(), List.of(3, 6).iterator(),
                        (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .iteratorEx();
        final Field innerField = outer.getClass().getDeclaredField("iterA");
        innerField.setAccessible(true);
        final Object inner = innerField.get(outer);
        for (int expected = 1; expected <= 6; expected++) {
            assertEquals(expected, outer.next());
            assertConsumedSlotsEmpty(outer);
            assertConsumedSlotsEmpty(inner);
        }
        assertFalse(outer.hasNext());
    }

    // A selector that fails on its failAt-th call (1-based) and is min-first otherwise.
    private static BiFunction<Integer, Integer, MergeResult> failingMinFirst(final int failAt, final AtomicInteger calls) {
        return (a, b) -> {
            if (calls.incrementAndGet() == failAt) {
                throw new IllegalStateException("selector " + failAt);
            }
            return a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        };
    }

    // Drains with catch-and-continue, recording each selector failure as "EX".
    private static <E extends Exception> List<Object> drain(final Throwables.Iterator<Integer, E> iter, final Object slots) throws Exception {
        final List<Object> out = new ArrayList<>();
        while (true) {
            try {
                if (!iter.hasNext()) {
                    return out;
                }
                out.add(iter.next());
                if (slots != null) {
                    assertConsumedSlotsEmpty(slots);
                }
            } catch (final IllegalStateException e) {
                out.add("EX");
            }
        }
    }

    @Test
    void mergesKeepBothHeadsWhenTheSelectorThrowsAndRetryTheSamePair() throws Exception {
        for (int failAt = 1; failAt <= 3; failAt++) {
            final List<Object> expected = new ArrayList<>(List.of(1, 2, 3, 4));
            expected.add(failAt - 1, "EX"); // the first three next() calls make one selector call each; 4 needs none

            final AtomicInteger calls = new AtomicInteger();
            final ObjIterator<Integer> pair = Iterators.merge(List.of(1, 3).iterator(), List.of(2, 4).iterator(), failingMinFirst(failAt, calls));
            assertEquals(expected, drain(Throwables.Iterator.of(pair), pair), "Iterators.merge failAt=" + failAt);

            calls.set(0);
            final Throwables.Iterator<Integer, RuntimeException> seq = Seq
                    .<Integer, RuntimeException> merge(List.of(1, 3).iterator(), List.of(2, 4).iterator(), failingMinFirst(failAt, calls)::apply)
                    .iteratorEx();
            assertEquals(expected, drain(seq, seq), "Seq.merge failAt=" + failAt);

            calls.set(0);
            final List<Object> streamed = drain(
                    Throwables.Iterator.of(Stream.merge(List.of(1, 3).iterator(), List.of(2, 4).iterator(), failingMinFirst(failAt, calls)).iterator()), null);
            assertEquals(expected, streamed, "Stream.merge, the reference behavior, failAt=" + failAt);
        }

        // Three or more sources: a failure at any fold node keeps the value the node below already produced for it.
        for (int failAt = 1; failAt <= 12; failAt++) {
            final AtomicInteger calls = new AtomicInteger();
            final ObjIterator<Integer> fold = Iterators.merge(List.of(List.of(1, 4).iterator(), List.of(2, 5).iterator(), List.of(3, 6).iterator()),
                    failingMinFirst(failAt, calls));
            final List<Object> folded = drain(Throwables.Iterator.of(fold), fold);
            assertEquals(List.of(1, 2, 3, 4, 5, 6), folded.stream().filter(v -> !"EX".equals(v)).toList(), "fold failAt=" + failAt);
            assertTrue(folded.size() <= 7, "at most one failure is injected");

            calls.set(0);
            final Throwables.Iterator<Integer, RuntimeException> nested = Seq
                    .<Integer, RuntimeException> merge(List.of(1, 4).iterator(), List.of(2, 5).iterator(), List.of(3, 6).iterator(),
                            failingMinFirst(failAt, calls)::apply)
                    .iteratorEx();
            final List<Object> seqFolded = drain(nested, nested);
            assertEquals(List.of(1, 2, 3, 4, 5, 6), seqFolded.stream().filter(v -> !"EX".equals(v)).toList(), "Seq 3-way failAt=" + failAt);
        }

        // Without failures the documented left-fold selector sequence is unchanged.
        final List<String> sequence = new ArrayList<>();
        final ObjIterator<Integer> fold = Iterators.merge(List.of(List.of(1, 4).iterator(), List.of(2, 5).iterator(), List.of(3, 6).iterator()), (a, b) -> {
            sequence.add(a + ":" + b);
            return a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        assertEquals(List.of(1, 2, 3, 4, 5, 6), drain(Throwables.Iterator.of(fold), fold));
        assertEquals(List.of("1:2", "1:3", "4:2", "2:3", "4:5", "4:3", "4:6", "5:6"), sequence);
    }

    @Test
    void mergesKeepAnAlreadyPulledHeadWhenTheOtherSourceThrows() throws Exception {
        // With no head pending, next() pulls 1 from the first source and then fails pulling from the second one. The
        // pulled 1 stays pending for the retry instead of being dropped with the failed call.
        final BiFunction<Integer, Integer, MergeResult> minFirst = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        final ObjIterator<Integer> pair = Iterators.merge(List.of(1, 3).iterator(), failingOnce(List.of(2, 4), 1, false), minFirst);
        assertEquals(List.of("EX", 1, 2, 3, 4), drain(Throwables.Iterator.of(pair), pair), "Iterators.merge");

        final Throwables.Iterator<Integer, RuntimeException> seq = Seq
                .<Integer, RuntimeException> merge(List.of(1, 3).iterator(), failingOnce(List.of(2, 4), 1, false), minFirst::apply)
                .iteratorEx();
        assertEquals(List.of("EX", 1, 2, 3, 4), drain(seq, seq), "Seq.merge");

        // A failing hasNext() pulls nothing in a pair merge: the retry starts the same comparison afresh.
        final ObjIterator<Integer> pairHasNext = Iterators.merge(List.of(1, 3).iterator(), failingOnce(List.of(2, 4), 1, true), minFirst);
        assertEquals(List.of("EX", 1, 2, 3, 4), drain(Throwables.Iterator.of(pairHasNext), pairHasNext), "Iterators.merge, hasNext()");

        // In a fold, the value handed up by the node below stays with the node whose own source then fails, whether the
        // source fails in next() or in hasNext() (the node below has already released that value either way).
        for (final boolean inHasNext : new boolean[] { false, true }) {
            for (int failAt = 1; failAt <= 4; failAt++) {
                final String label = (inHasNext ? "hasNext()" : "next()") + " failAt=" + failAt;
                final ObjIterator<Integer> fold = Iterators
                        .merge(List.of(List.of(1, 4).iterator(), List.of(2, 5).iterator(), failingOnce(List.of(3, 6), failAt, inHasNext)), minFirst);
                final List<Object> folded = drain(Throwables.Iterator.of(fold), fold);
                assertEquals(List.of(1, 2, 3, 4, 5, 6), folded.stream().filter(v -> !"EX".equals(v)).toList(), "fold " + label);
                assertTrue(folded.size() <= 7, "fold " + label);

                final Throwables.Iterator<Integer, RuntimeException> nested = Seq
                        .<Integer, RuntimeException> merge(List.of(1, 4).iterator(), List.of(2, 5).iterator(), failingOnce(List.of(3, 6), failAt, inHasNext),
                                minFirst::apply)
                        .iteratorEx();
                final List<Object> seqFolded = drain(nested, nested);
                assertEquals(List.of(1, 2, 3, 4, 5, 6), seqFolded.stream().filter(v -> !"EX".equals(v)).toList(), "Seq 3-way " + label);
                assertTrue(seqFolded.size() <= 7, "Seq 3-way " + label);

                if (failAt <= 2) {
                    // The first two calls of either method happen in every drain, so the failure was really injected.
                    assertEquals(7, folded.size(), "fold " + label);
                    assertEquals(7, seqFolded.size(), "Seq 3-way " + label);
                }
            }
        }
    }

    // Yields values in order, except that one call - the failAt-th call of next(), or of hasNext() when inHasNext is
    // set - throws IllegalStateException and consumes nothing.
    private static Iterator<Integer> failingOnce(final List<Integer> values, final int failAt, final boolean inHasNext) {
        return new Iterator<>() {
            private int cursor;
            private int calls;

            @Override
            public boolean hasNext() {
                if (inHasNext && ++calls == failAt) {
                    throw new IllegalStateException("source hasNext() " + failAt);
                }

                return cursor < values.size();
            }

            @Override
            public Integer next() {
                if (!inHasNext && ++calls == failAt) {
                    throw new IllegalStateException("source next() " + failAt);
                }

                return values.get(cursor++);
            }
        };
    }

    private static void assertConsumedSlotsEmpty(final Object iter) throws Exception {
        for (final String suffix : List.of("A", "B")) {
            final Field flag = iter.getClass().getDeclaredField("hasNext" + suffix);
            final Field slot = iter.getClass().getDeclaredField("next" + suffix);
            flag.setAccessible(true);
            slot.setAccessible(true);
            final Object flags = flag.get(iter);
            if (flags instanceof boolean[] present) {
                final Object[] values = (Object[]) slot.get(iter);
                for (int i = 0; i < present.length; i++) {
                    if (!present[i]) {
                        assertNull(values[i], "consumed " + suffix + " slot " + i);
                    }
                }
            } else if (!((Boolean) flags)) {
                assertNull(slot.get(iter), "consumed head " + suffix);
            }
        }
    }

    @Test
    void seqCloseHandlesLateOffersAndPreservesCallerQueueContents() throws Exception {
        for (final boolean timed : new boolean[] { false, true }) {
            for (final boolean ownsQueue : new boolean[] { true, false }) {
                final GatedQueue<Object> queue = new GatedQueue<>(timed);
                final Object payload = new Object();
                final MutableBoolean hasMore = MutableBoolean.of(true);
                final Throwables.Iterator<Object, RuntimeException> iter = seqBuffer(Throwables.Iterator.of(List.of(payload).iterator()), queue, hasMore,
                        ownsQueue);
                try {
                    assertTrue(queue.entered.await(5, TimeUnit.SECONDS));
                    iter.closeResource();
                } finally {
                    queue.release.countDown();
                }
                await(() -> !hasMore.value());
                assertEquals(ownsQueue ? 0 : 1, queue.size());
                if (!ownsQueue) {
                    assertSame(payload, queue.peek());
                }
                assertFalse(iter.hasNext());
                iter.closeResource();
            }
        }
    }

    @Test
    void seqWorkerFailureKeepsBufferedValuesAndCheckedExceptionIdentity() throws Exception {
        final IOException failure = new IOException("producer");
        final MutableBoolean hasMore = MutableBoolean.of(true);
        final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(8);
        final Throwables.Iterator<Integer, IOException> source = new Throwables.Iterator<>() {
            private int cursor;

            public boolean hasNext() throws IOException {
                if (cursor == 2) {
                    throw failure;
                }
                return true;
            }

            public Integer next() {
                return cursor++ == 0 ? null : 2;
            }
        };
        final Throwables.Iterator<Integer, IOException> iter = seqBuffer(source, queue, hasMore, true);
        try {
            await(() -> !hasMore.value());
            assertEquals(2, queue.size());
            assertNull(iter.next());
            assertEquals(2, iter.next());
            assertSame(failure, assertThrows(IOException.class, iter::hasNext));
        } finally {
            iter.closeResource();
        }
    }

    @Test
    void publicStreamAndSeqBuffersOwnTheirQueuesAndCloseSourcesOnce() throws Exception {
        final CountDownLatch streamDone = new CountDownLatch(1);
        final AtomicInteger streamCloses = new AtomicInteger();
        final Stream<Object> stream = Stream.of(finiteSource(streamDone)).onClose(streamCloses::incrementAndGet).buffered(8);
        stream.iterator().next();
        assertTrue(streamDone.await(5, TimeUnit.SECONDS));
        final BlockingQueue<?> streamQueue = findQueue(stream);
        assertEquals(3, streamQueue.size());
        stream.close();
        assertTrue(streamQueue.isEmpty());
        assertEquals(1, streamCloses.get());

        final CountDownLatch seqDone = new CountDownLatch(1);
        final AtomicInteger seqCloses = new AtomicInteger();
        final Seq<Object, RuntimeException> seq = Seq.<Object, RuntimeException> of(finiteSource(seqDone)).onClose(seqCloses::incrementAndGet).buffered(8);
        seq.iteratorEx().next();
        assertTrue(seqDone.await(5, TimeUnit.SECONDS));
        final BlockingQueue<?> seqQueue = findQueue(seq);
        assertEquals(3, seqQueue.size());
        seq.close();
        assertTrue(seqQueue.isEmpty());
        assertEquals(1, seqCloses.get());
    }

    @Test
    void callerSuppliedQueuesAreNotDrainedByPublicClose() throws Exception {
        final CountDownLatch streamDone = new CountDownLatch(1);
        final ArrayBlockingQueue<Object> streamQueue = new ArrayBlockingQueue<>(8);
        final Stream<Object> stream = Stream.of(finiteSource(streamDone)).buffered(streamQueue);
        stream.iterator().next();
        assertTrue(streamDone.await(5, TimeUnit.SECONDS));
        stream.close();
        assertEquals(3, streamQueue.size());

        final CountDownLatch seqDone = new CountDownLatch(1);
        final ArrayBlockingQueue<Object> seqQueue = new ArrayBlockingQueue<>(8);
        final Seq<Object, RuntimeException> seq = Seq.<Object, RuntimeException> of(finiteSource(seqDone)).buffered(seqQueue);
        seq.iteratorEx().next();
        assertTrue(seqDone.await(5, TimeUnit.SECONDS));
        seq.close();
        assertEquals(3, seqQueue.size());
    }

    private static Iterator<Object> finiteSource(final CountDownLatch done) {
        return new Iterator<>() {
            private int cursor;

            public boolean hasNext() {
                if (cursor == 4) {
                    done.countDown();
                }
                return cursor < 4;
            }

            public Object next() {
                cursor++;
                return new Object();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Exception> Throwables.Iterator<T, E> seqBuffer(final Throwables.Iterator<T, E> source, final BlockingQueue<T> queue,
            final MutableBoolean hasMore, final boolean ownsQueue) throws Exception {
        final Method method = Seq.class.getDeclaredMethod("buffered", Throwables.Iterator.class, BlockingQueue.class, MutableBoolean.class, boolean.class);
        method.setAccessible(true);
        return (Throwables.Iterator<T, E>) method.invoke(null, source, queue, hasMore, ownsQueue);
    }

    private static BlockingQueue<?> findQueue(final Object root) throws Exception {
        final ArrayDeque<Object> pending = new ArrayDeque<>();
        final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(root);
        while (!pending.isEmpty()) {
            final Object value = pending.remove();
            if (!seen.add(value)) {
                continue;
            }
            if (value instanceof BlockingQueue<?> queue) {
                return queue;
            }
            if (value instanceof Object[] values) {
                for (final Object child : values) {
                    if (child != null) {
                        pending.add(child);
                    }
                }
            } else {
                for (Class<?> type = value.getClass(); type != null && type.getName().startsWith("com.landawn.abacus."); type = type.getSuperclass()) {
                    for (final Field field : type.getDeclaredFields()) {
                        if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                            field.setAccessible(true);
                            final Object child = field.get(value);
                            if (child != null) {
                                pending.add(child);
                            }
                        }
                    }
                }
            }
        }
        throw new AssertionError("No reachable buffer queue");
    }

    private static void await(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        assertTrue(condition.getAsBoolean(), "producer did not finish");
    }

    private static final class GatedQueue<T> extends ArrayBlockingQueue<T> {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        private final boolean timed;

        GatedQueue(final boolean timed) {
            super(4);
            this.timed = timed;
        }

        @Override
        public boolean offer(final T value) {
            if (timed) {
                return false;
            }
            gate();
            return super.offer(value);
        }

        @Override
        public boolean offer(final T value, final long timeout, final TimeUnit unit) throws InterruptedException {
            gate();
            return super.offer(value, timeout, unit);
        }

        private void gate() {
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("offer was not released");
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }
}
