package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class StreamReversedSubListTradeoffReview20261010Test extends TestBase {
    @Test
    void arraysSubListsKeepLiveValuesAcrossSkipAndCount() {
        for (final int size : new int[] { 8, 64 }) {
            for (final boolean nested : new boolean[] { false, true }) {
                final Integer[] values = ascendingValues(size + 4);
                final List<Integer> root = Arrays.asList(values);
                final List<Integer> view = nested ? root.subList(1, size + 3).subList(1, size + 1) : root.subList(2, size + 2);
                final AtomicInteger reads = new AtomicInteger();
                final List<Integer> counted = countPreviousCalls(view, reads);

                try (Stream<Integer> reversed = Stream.ofReversed(counted).skip(size - 2L)) {
                    values[3] = 99;
                    assertEquals(List.of(99, 2), reversed.toList());
                }
                assertTrue(reads.get() <= size);

                reads.set(0);
                assertEquals(1, Stream.ofReversed(counted).skip(size - 1L).count());
                assertTrue(reads.get() <= size);

                reads.set(0);
                assertEquals(size, Stream.ofReversed(counted).count());
                assertTrue(reads.get() <= size);
            }
        }
    }

    @Test
    void arraysRootsAndNCopiesSubListsKeepConstantTimeSkipAndCount() {
        for (final int size : new int[] { 8, 64 }) {
            final List<List<Integer>> cases = List.of(Arrays.asList(ascendingValues(size)), Collections.nCopies(size + 2, 7).subList(1, size + 1),
                    Collections.nCopies(size + 4, 7).subList(1, size + 3).subList(1, size + 1));

            for (final List<Integer> values : cases) {
                final AtomicInteger reads = new AtomicInteger();
                final List<Integer> counted = countPreviousCalls(values, reads);

                assertEquals(1, Stream.ofReversed(counted).skip(size - 1L).count());
                assertTrue(reads.get() <= 2);

                reads.set(0);
                assertEquals(size, Stream.ofReversed(counted).count());
                assertTrue(reads.get() <= 1);
            }
        }
    }

    @Test
    void aRangeOfTheSupportedRootAvoidsScanningTheExcludedSuffix() {
        for (final int size : new int[] { 8, 64 }) {
            final AtomicInteger reads = new AtomicInteger();
            final List<Integer> root = countPreviousCalls(Arrays.asList(ascendingValues(size)), reads);

            assertEquals(List.of(2, 1), Stream.ofReversed(root).skip(root.size() - 3L).limit(2).toList());
            assertTrue(reads.get() <= 3);
        }
    }

    @Test
    void nestedSubListsKeepTheCustomRootsOriginalSnapshotThroughSkipAndCount() {
        final List<Integer> values = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7));
        final AtomicInteger iteratorCalls = new AtomicInteger();
        final List<Integer> root = new SnapshotList(values, iteratorCalls);
        final List<Integer> view = root.subList(1, 7).subList(1, 5);

        try (Stream<Integer> reversed = Stream.ofReversed(view).skip(2)) {
            values.set(3, 99);
            final ObjIteratorEx<Integer> iterator = reversed.iteratorEx();
            assertEquals(3, iterator.next());
            assertEquals(1, iterator.count());
            assertEquals(1, iteratorCalls.get());
        }
    }

    private static Integer[] ascendingValues(final int size) {
        final Integer[] values = new Integer[size];
        for (int i = 0; i < size; i++) {
            values[i] = i;
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> countPreviousCalls(final List<T> values, final AtomicInteger reads) {
        // Count iterator operations rather than time; upper bounds also permit a future safe sub-list optimization.
        final List<T> counted = spy(values);
        doAnswer(invocation -> {
            final ListIterator<T> iterator = spy((ListIterator<T>) invocation.callRealMethod());
            doAnswer(read -> {
                reads.incrementAndGet();
                return read.callRealMethod();
            }).when(iterator).previous();
            return iterator;
        }).when(counted).listIterator(anyInt());
        return counted;
    }

    private static final class SnapshotList extends AbstractList<Integer> implements RandomAccess {
        private final List<Integer> values;
        private final AtomicInteger iteratorCalls;

        private SnapshotList(final List<Integer> values, final AtomicInteger iteratorCalls) {
            this.values = values;
            this.iteratorCalls = iteratorCalls;
        }

        @Override
        public Integer get(final int index) {
            return values.get(index);
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public ListIterator<Integer> listIterator(final int index) {
            iteratorCalls.incrementAndGet();
            return new ArrayList<>(values).listIterator(index);
        }
    }
}
