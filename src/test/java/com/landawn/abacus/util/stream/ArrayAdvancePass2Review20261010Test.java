package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class ArrayAdvancePass2Review20261010Test extends TestBase {
    @Test
    void stepSkipPreservesArraySlicesNullsAndLargeDistances() {
        final Integer[] values = { 90, null, 2, 3, null, 5, 6, 7, 80 };

        for (int from = 0; from <= values.length; from++) {
            for (int to = from; to <= values.length; to++) {
                for (final long step : new long[] { 1, 2, 3, 5, Integer.MAX_VALUE, Long.MAX_VALUE }) {
                    final List<Integer> sampled = new ArrayList<>();
                    for (long index = from; index < to; index += Math.min(step, Integer.MAX_VALUE)) {
                        sampled.add(values[(int) index]);
                    }

                    for (final long skipped : new long[] { 0, 1, 2, 4, 9, Long.MAX_VALUE }) {
                        final List<Integer> expected = sampled.subList((int) Math.min(skipped, sampled.size()), sampled.size());
                        assertEquals(expected, Stream.of(values, from, to).step(step).skip(skipped).toList());
                        assertEquals(expected.size(), Stream.of(values, from, to).step(step).skip(skipped).count());
                    }
                }
            }
        }
    }

    @Test
    void stepSkipComposesWithLimitAndClosesTheSourceOnce() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> source = Stream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).onClose(closed::incrementAndGet);

        assertEquals(List.of(6, 8), source.step(2).skip(1).limit(4).skip(2).toList());
        source.close();
        assertEquals(1, closed.get());
        assertEquals(0, Stream.of(0, 1, 2, 3).step(2).limit(1).skip(Long.MAX_VALUE).count());
    }

    @Test
    void stepIteratorKeepsItsPositionAcrossLookaheadAndExhaustion() {
        try (Stream<Integer> stream = Stream.of(9, null, 2, 3, 4, 5, 8).skip(1).limit(5).step(2).skip(1)) {
            final ObjIteratorEx<Integer> iterator = stream.iteratorEx();
            assertTrue(iterator.hasNext());
            assertTrue(iterator.hasNext());
            assertEquals(3, iterator.next());
            assertEquals(1, iterator.count());
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @Test
    void topSkipPreservesSelectionOrderAndEveryComparatorCall() {
        final Integer[] values = { 90, 8, null, 5, 8, 2, 9, 4, 80 };
        final List<String> originalCalls = new ArrayList<>();
        final List<Integer> selected = Stream.of(values, 1, 8).top(4, recordingComparator(originalCalls)).toList();

        for (final long skipped : new long[] { 0, 1, 3, 4, Long.MAX_VALUE }) {
            final List<String> calls = new ArrayList<>();
            assertEquals(selected.subList((int) Math.min(skipped, selected.size()), selected.size()),
                    Stream.of(values, 1, 8).top(4, recordingComparator(calls)).skip(skipped).toList());
            assertEquals(originalCalls, calls);
        }
    }

    @Test
    void topSkipRetriesComparatorFailureWithoutLosingTheSkipQuota() {
        final Integer[] values = { 7, 2, 9, 1, 8, 3, 6, 4, 5 };
        final List<Integer> selected = Stream.of(values).top(5, Comparator.naturalOrder()).toList();
        final IllegalStateException failure = new IllegalStateException("selection");
        final AtomicBoolean failOnce = new AtomicBoolean(true);
        final AtomicInteger closed = new AtomicInteger();

        try (Stream<Integer> stream = Stream.of(values).onClose(closed::incrementAndGet).top(5, (a, b) -> {
            if (failOnce.getAndSet(false)) {
                throw failure;
            }
            return Integer.compare(a, b);
        }).skip(2).limit(2)) {
            final ObjIteratorEx<Integer> iterator = stream.iteratorEx();
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertEquals(0, closed.get());
            assertTrue(iterator.hasNext());
            assertEquals(selected.get(2), iterator.next());
            assertEquals(selected.get(3), iterator.next());
            assertFalse(iterator.hasNext());
        }
        assertEquals(1, closed.get());
    }

    @Test
    void topSkipPropagatesSelectionFailureAndSuppressesCloseFailure() {
        final IllegalStateException failure = new IllegalStateException("selection");
        final IllegalArgumentException closeFailure = new IllegalArgumentException("close");
        final Stream<Integer> stream = Stream.of(3, 1, 4, 2).onClose(() -> {
            throw closeFailure;
        }).top(2, (a, b) -> {
            throw failure;
        }).skip(Long.MAX_VALUE);

        assertSame(failure, assertThrows(IllegalStateException.class, stream::count));
        assertEquals(List.of(closeFailure), Arrays.asList(failure.getSuppressed()));
    }

    private static Comparator<Integer> recordingComparator(final List<String> calls) {
        final Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.naturalOrder());
        return (a, b) -> {
            calls.add(a + ":" + b);
            return comparator.compare(a, b);
        };
    }
}
