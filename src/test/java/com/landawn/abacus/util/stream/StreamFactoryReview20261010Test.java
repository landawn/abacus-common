package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class StreamFactoryReview20261010Test extends TestBase {
    @Test
    void reversedCustomIteratorIsNeverRecreatedDuringSkip() {
        final AtomicInteger iteratorCalls = new AtomicInteger();
        final List<Integer> values = new ArrayList<>(List.of(0, 1, 2, 3, 4)) {
            @Override
            public ListIterator<Integer> listIterator(int index) {
                if (iteratorCalls.incrementAndGet() > 1) {
                    throw new IllegalStateException("A new iterator cannot replace the original");
                }
                return super.listIterator(index);
            }
        };

        assertEquals(List.of(2, 1, 0), Stream.ofReversed(values).skip(2).toList());
        assertEquals(1, iteratorCalls.get());
    }

    @Test
    void reversedCustomSnapshotIteratorKeepsItsCapturedValues() {
        final List<Integer> values = new ArrayList<>(List.of(0, 1, 2, 3, 4)) {
            @Override
            public ListIterator<Integer> listIterator(int index) {
                return new ArrayList<>(this).listIterator(index);
            }
        };

        try (Stream<Integer> reversed = Stream.ofReversed(values).skip(2)) {
            values.set(2, 99);
            assertEquals(List.of(2, 1, 0), reversed.toList());
        }
    }

    @Test
    void reversedSubListKeepsItsCustomRootIteratorSnapshot() {
        for (final boolean nested : new boolean[] { false, true }) {
            final List<Integer> values = new ArrayList<>(List.of(0, 1, 2, 3, 4));
            final List<Integer> root = new SnapshotList(values);
            final List<Integer> view = nested ? root.subList(0, 5).subList(1, 5) : root.subList(1, 5);
            try (Stream<Integer> reversed = Stream.ofReversed(view).skip(2)) {
                values.set(2, 99);
                assertEquals(List.of(2, 1), reversed.toList());
            }
        }
    }

    @Test
    void reversedSkipRetriesIteratorConstructionWithoutLosingTheProbedValue() {
        final FailingSizeList values = new FailingSizeList();
        try (Stream<Integer> reversed = Stream.ofReversed(values).skip(2)) {
            final ObjIteratorEx<Integer> iter = reversed.iteratorEx();
            assertSame(values.failure, assertThrows(IllegalStateException.class, iter::hasNext));
            assertEquals(2, iter.next());
            assertEquals(List.of(1, 0), iter.toList());
        }
    }

    @Test
    void reversedFailedAdvancePreservesDirectNextAndCount() {
        final FailingSizeList first = new FailingSizeList();
        try (Stream<Integer> reversed = Stream.ofReversed(first)) {
            final ObjIteratorEx<Integer> iter = reversed.iteratorEx();
            assertSame(first.failure, assertThrows(IllegalStateException.class, () -> iter.advance(2)));
            iter.advance(0);
            assertEquals(4, iter.next());
            assertEquals(3, iter.next());
            assertEquals(3, iter.count());
            assertFalse(iter.hasNext());
        }

        final FailingSizeList second = new FailingSizeList();
        try (Stream<Integer> reversed = Stream.ofReversed(second)) {
            final ObjIteratorEx<Integer> iter = reversed.iteratorEx();
            assertSame(second.failure, assertThrows(IllegalStateException.class, () -> iter.advance(2)));
            assertEquals(5, iter.count());
            assertFalse(iter.hasNext());
        }
    }

    @Test
    void reversedFailedAdvanceKeepsLiveValuesAndFailFastChecks() {
        final FailingSizeList live = new FailingSizeList();
        try (Stream<Integer> reversed = Stream.ofReversed(live)) {
            final ObjIteratorEx<Integer> iter = reversed.iteratorEx();
            assertSame(live.failure, assertThrows(IllegalStateException.class, () -> iter.advance(2)));
            live.set(4, 99);
            assertEquals(99, iter.next());
            assertEquals(List.of(3, 2, 1, 0), iter.toList());
        }

        final FailingSizeList changed = new FailingSizeList();
        try (Stream<Integer> reversed = Stream.ofReversed(changed)) {
            final ObjIteratorEx<Integer> iter = reversed.iteratorEx();
            assertSame(changed.failure, assertThrows(IllegalStateException.class, () -> iter.advance(2)));
            changed.add(5, 99);
            assertThrows(ConcurrentModificationException.class, iter::next);
        }
    }

    @Test
    void inheritedAbstractListIteratorKeepsConstantTimeSkipAndCount() {
        final AtomicInteger gets = new AtomicInteger();
        final List<Integer> values = new IndexedList(Integer.MAX_VALUE, gets);
        assertEquals(List.of(1, 0), Stream.ofReversed(values).skip(Integer.MAX_VALUE - 2L).toList());
        assertTrue(gets.get() <= 4);
        gets.set(0);
        assertEquals(Integer.MAX_VALUE, Stream.ofReversed(values).count());
        assertTrue(gets.get() <= 2);
    }

    @Test
    void arrayListSubListKeepsConstantTimeSkipAndCount() {
        final List<Integer> view = spy(new ArrayList<>(Collections.nCopies(10_002, 7)).subList(1, 10_001));
        final AtomicInteger reads = new AtomicInteger();
        doAnswer(invocation -> {
            final ListIterator<Integer> iterator = spy((ListIterator<Integer>) invocation.callRealMethod());
            doAnswer(read -> {
                reads.incrementAndGet();
                return read.callRealMethod();
            }).when(iterator).previous();
            return iterator;
        }).when(view).listIterator(anyInt());

        assertEquals(List.of(7, 7), Stream.ofReversed(view).skip(9_998).toList());
        assertEquals(3, reads.get());
        reads.set(0);
        assertEquals(10_000, Stream.ofReversed(view).count());
        assertEquals(1, reads.get());
    }

    @Test
    void genericChunkAdvanceCrossesBothSizeRegionsAndHandlesMaximumCounts() {
        final Integer[][] expected = { { 4, 408, 811, 1114, 1417 }, { 3, 306, 609, 913, 1317 } };

        for (int order = 0; order < 2; order++) {
            for (int skipped = 0; skipped <= 5; skipped++) {
                try (Stream<Integer> stream = Stream.splitByChunkCount(17, 5, order == 1, (from, to) -> from * 100 + to)) {
                    final ObjIteratorEx<Integer> iter = stream.iteratorEx();
                    iter.advance(skipped);
                    assertArrayEquals(Arrays.copyOfRange(expected[order], skipped, 5), iter.toArray(new Integer[0]));
                    assertFalse(iter.hasNext());
                }
            }

            final AtomicInteger mapped = new AtomicInteger();
            try (Stream<Integer> stream = Stream.splitByChunkCount(Integer.MAX_VALUE, Integer.MAX_VALUE, order == 1, (from, to) -> {
                mapped.incrementAndGet();
                assertEquals(from + 1, to);
                return from;
            })) {
                final ObjIteratorEx<Integer> iter = stream.iteratorEx();
                iter.advance(-1);
                assertEquals(0, iter.next());
                iter.advance(Integer.MAX_VALUE - 3L);
                assertEquals(Integer.MAX_VALUE - 2, iter.next());
                iter.advance(Long.MAX_VALUE);
                assertEquals(2, mapped.get());
                assertFalse(iter.hasNext());
                assertEquals(0, iter.count());
            }
        }
    }

    private static final class IndexedList extends AbstractList<Integer> implements RandomAccess {
        private final int size;
        private final AtomicInteger gets;

        private IndexedList(int size, AtomicInteger gets) {
            this.size = size;
            this.gets = gets;
        }

        @Override
        public Integer get(int index) {
            gets.incrementAndGet();
            return index;
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static final class SnapshotList extends AbstractList<Integer> implements RandomAccess {
        private final List<Integer> values;

        private SnapshotList(List<Integer> values) {
            this.values = values;
        }

        @Override
        public Integer get(int index) {
            return values.get(index);
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public ListIterator<Integer> listIterator(int index) {
            return new ArrayList<>(values).listIterator(index);
        }
    }

    private static final class FailingSizeList extends AbstractList<Integer> implements RandomAccess {
        private final IllegalStateException failure = new IllegalStateException("Transient size failure");
        private final List<Integer> values = new ArrayList<>(List.of(0, 1, 2, 3, 4));
        private boolean armFailure = true;
        private boolean failNextSize;

        @Override
        public Integer get(int index) {
            if (armFailure) {
                armFailure = false;
                failNextSize = true;
            }
            return values.get(index);
        }

        @Override
        public int size() {
            if (failNextSize) {
                failNextSize = false;
                throw failure;
            }
            return values.size();
        }

        @Override
        public Integer set(int index, Integer value) {
            return values.set(index, value);
        }

        @Override
        public void add(int index, Integer value) {
            values.add(index, value);
            modCount++;
        }
    }
}
