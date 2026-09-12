package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.LongStream;

public class LongIteratorTest extends TestBase {

    @Test
    public void testShortCircuitStreamLeavesRemainingElements() {
        final LongIterator iter = LongIterator.of(1L, 2L, 3L);

        assertEquals(1L, iter.stream().limit(1).count());
        assertTrue(iter.hasNext());
        assertArrayEquals(new long[] { 2L, 3L }, iter.stream().toArray());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty() {
        LongIterator iter = LongIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertSame(LongIterator.EMPTY, LongIterator.empty());
        assertEquals(0, iter.toArray().length);
        assertTrue(iter.toList().isEmpty());
        assertFalse(iter.indexed().hasNext());
    }

    @Test
    public void testOf() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertFalse(LongIterator.of().hasNext());
        assertFalse(LongIterator.of((long[]) null).hasNext());
        assertEquals(42L, LongIterator.of(42L).nextLong());
        LongIterator special = LongIterator.of(Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE);
        assertEquals(Long.MIN_VALUE, special.nextLong());
        assertEquals(-1L, special.nextLong());
        assertEquals(0L, special.nextLong());
        assertEquals(1L, special.nextLong());
        assertEquals(Long.MAX_VALUE, special.nextLong());
    }

    @Test
    public void testOf_Range() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        assertArrayEquals(new long[] { 2L, 3L, 4L }, LongIterator.of(array, 1, 4).toArray());
        assertEquals(LongList.of(2L, 3L, 4L), LongIterator.of(array, 1, 4).toList());
        assertArrayEquals(array, LongIterator.of(array, 0, array.length).toArray());
        assertFalse(LongIterator.of(array, 1, 1).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of((long[]) null, 0, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger calls = new AtomicInteger();
        LongIterator iter = LongIterator.defer(() -> {
            calls.incrementAndGet();
            return LongIterator.of(1L, 2L, 3L);
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertEquals(1, calls.get());

        boolean[] calledOnNext = { false };
        LongIterator onNext = LongIterator.defer(() -> {
            calledOnNext[0] = true;
            return LongIterator.of(42L);
        });
        assertEquals(42L, onNext.nextLong());
        assertTrue(calledOnNext[0]);

        int[] failCount = { 0 };
        LongIterator failing = LongIterator.defer(() -> {
            failCount[0]++;
            return null;
        });
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertThrows(IllegalStateException.class, failing::hasNext);
        assertEquals(1, failCount[0]);
        assertThrows(IllegalArgumentException.class, () -> LongIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        LongIterator infinite = LongIterator.generate(() -> n.getAndIncrement());
        assertEquals(0L, infinite.nextLong());
        assertEquals(1L, infinite.nextLong());
        assertTrue(infinite.hasNext());

        AtomicInteger counter = new AtomicInteger();
        LongIterator finite = LongIterator.generate(() -> counter.get() < 3, () -> counter.getAndIncrement());
        assertArrayEquals(new long[] { 0L, 1L, 2L }, finite.toArray());
        assertThrows(NoSuchElementException.class, finite::nextLong);
        assertFalse(LongIterator.generate(() -> false, () -> 1L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate((LongSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate(null, () -> 0L));
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate((BooleanSupplier) () -> true, null));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNext() {
        LongIterator iter = LongIterator.of(10L, 20L, 30L);
        assertEquals(Long.valueOf(10L), iter.next());
        assertEquals(20L, iter.nextLong());
        assertEquals(30L, iter.nextLong());
        assertThrows(NoSuchElementException.class, iter::nextLong);
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new long[] { 3L, 4L, 5L }, LongIterator.of(1L, 2L, 3L, 4L, 5L).skip(2).toArray());
        assertFalse(LongIterator.of(1L, 2L, 3L).skip(5).hasNext());
        LongIterator original = LongIterator.of(1L, 2L, 3L);
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).skip(-1));
        assertThrows(NoSuchElementException.class, () -> LongIterator.of(1L, 2L).skip(2).nextLong());

        LongIterator source = new LongIterator() {
            private int next;
            private boolean failedOnce;

            @Override
            public boolean hasNext() {
                return next < 4;
            }

            @Override
            public long nextLong() {
                if (next == 1 && !failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("transient failure");
                }
                return next++;
            }
        };
        LongIterator skipped = source.skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals(2L, skipped.nextLong());
    }

    @Test
    public void testLimit() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, LongIterator.of(1L, 2L, 3L, 4L, 5L).limit(3).toArray());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, LongIterator.of(1L, 2L, 3L).limit(5).toArray());
        assertFalse(LongIterator.of(1L).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).limit(-1));

        int[] attempts = { 0 };
        LongIterator quota = LongIterator.generate(() -> {
            if (attempts[0]++ == 0) {
                throw new IllegalStateException("temporary failure");
            }
            return 7L;
        }).limit(1);
        assertThrows(IllegalStateException.class, quota::nextLong);
        assertTrue(quota.hasNext());
        assertEquals(7L, quota.nextLong());
        assertFalse(quota.hasNext());
        assertEquals(2, attempts[0]);
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new long[] { 2L, 4L, 6L }, LongIterator.of(1L, 2L, 3L, 4L, 5L, 6L).filter(x -> x % 2 == 0).toArray());
        LongIterator none = LongIterator.of(1L, 3L, 5L).filter(x -> x % 2 == 0);
        assertFalse(none.hasNext());
        assertThrows(NoSuchElementException.class, none::nextLong);
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).filter(null));
        assertEquals(18L, LongIterator.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L).skip(2).limit(6).filter(x -> x % 2 == 0).stream().sum());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, LongIterator.of(1L, 2L, 3L).toArray());
        LongIterator partial = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        partial.nextLong();
        partial.nextLong();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, partial.toArray());
        assertEquals(LongList.of(1L, 2L, 3L), LongIterator.of(1L, 2L, 3L).toList());
        assertTrue(LongIterator.empty().toList().isEmpty());
    }

    @Test
    public void testStream() {
        LongStream stream = LongIterator.of(1L, 2L, 3L).stream();
        assertNotNull(stream);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.toArray());
        assertEquals(0, LongIterator.empty().stream().toArray().length);
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedLong> indexed = LongIterator.of(10L, 20L, 30L).indexed();
        assertEquals(0, indexed.next().index());
        assertEquals(20L, indexed.next().value());
        assertEquals(2, indexed.next().index());
        assertFalse(indexed.hasNext());
        assertEquals(100, LongIterator.of(10L).indexed(100).next().index());
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).indexed(-1));

        LongIterator source = LongIterator.of(1L, 2L);
        ObjIterator<IndexedLong> overflowing = source.indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, overflowing.next().longIndex());
        assertThrows(ArithmeticException.class, overflowing::next);
        assertEquals(2L, source.nextLong());

        ObjIterator<IndexedLong> max = LongIterator.of(1L).indexed(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max.next().longIndex());
        assertFalse(max.hasNext());
        assertThrows(NoSuchElementException.class, max::next);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() {
        LongList boxed = new LongList();
        LongIterator.of(1L, 2L, 3L).forEachRemaining((Long i) -> boxed.add(i));
        assertEquals(LongList.of(1L, 2L, 3L), boxed);

        LongList values = new LongList();
        LongIterator.of(1L, 2L, 3L).foreachRemaining(values::add);
        assertEquals(LongList.of(1L, 2L, 3L), values);

        LongIterator partial = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        partial.nextLong();
        partial.nextLong();
        AtomicInteger sum = new AtomicInteger();
        partial.foreachRemaining(v -> sum.addAndGet((int) v));
        assertEquals(12, sum.get());
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).foreachRemaining(null));

        // forEachRemaining(Consumer) overrides Iterator.forEachRemaining, whose contract specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> LongIterator.of(1L).forEachRemaining((java.util.function.Consumer<Long>) null));
        assertThrows(NullPointerException.class, () -> LongIterator.empty().forEachRemaining((java.util.function.Consumer<Long>) null));
    }

    @Test
    public void testForeachIndexed() {
        LongList indices = new LongList();
        LongList values = new LongList();
        LongIterator.of(10L, 20L, 30L).foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });
        assertEquals(LongList.of(0L, 1L, 2L), indices);
        assertEquals(LongList.of(10L, 20L, 30L), values);

        LongIterator partial = LongIterator.of(10L, 20L, 30L);
        partial.nextLong();
        int[] firstIndex = { -1 };
        partial.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });
        assertEquals(0, firstIndex[0]);
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).foreachIndexed(null));
    }
}
