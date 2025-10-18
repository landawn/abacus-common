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
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

@Tag("2025")
public class LongIterator2025Test extends TestBase {

    @Test
    public void testEmpty() {
        LongIterator iter = LongIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmptyNextThrowsException() {
        LongIterator iter = LongIterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testEmptyConstant() {
        assertSame(LongIterator.EMPTY, LongIterator.empty());
    }

    @Test
    public void testOf() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmpty() {
        LongIterator iter = LongIterator.of();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfNull() {
        LongIterator iter = LongIterator.of((long[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfSingleElement() {
        LongIterator iter = LongIterator.of(42L);
        assertTrue(iter.hasNext());
        assertEquals(42L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfRange() {
        long[] array = { 10L, 20L, 30L, 40L, 50L };
        LongIterator iter = LongIterator.of(array, 1, 4);
        assertEquals(20L, iter.nextLong());
        assertEquals(30L, iter.nextLong());
        assertEquals(40L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfRangeFullArray() {
        long[] array = { 1L, 2L, 3L };
        LongIterator iter = LongIterator.of(array, 0, 3);
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfRangeEmpty() {
        long[] array = { 1L, 2L, 3L };
        LongIterator iter = LongIterator.of(array, 1, 1);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfRangeInvalidIndices() {
        long[] array = { 1L, 2L, 3L };
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, 2, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger callCount = new AtomicInteger(0);
        LongIterator iter = LongIterator.defer(() -> {
            callCount.incrementAndGet();
            return LongIterator.of(1L, 2L, 3L);
        });
        assertEquals(0, callCount.get());
        assertTrue(iter.hasNext());
        assertEquals(1, callCount.get());
        assertEquals(1L, iter.nextLong());
        assertEquals(1, callCount.get());
    }

    @Test
    public void testDeferNull() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.defer(null));
    }

    @Test
    public void testDeferMultipleCalls() {
        AtomicInteger callCount = new AtomicInteger(0);
        LongIterator iter = LongIterator.defer(() -> {
            callCount.incrementAndGet();
            return LongIterator.of(100L);
        });
        iter.hasNext();
        iter.nextLong();
        iter.hasNext();
        assertEquals(1, callCount.get());
    }

    @Test
    public void testGenerate() {
        AtomicLong counter = new AtomicLong(0);
        LongIterator iter = LongIterator.generate(() -> counter.incrementAndGet());
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerateNull() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate(null));
    }

    @Test
    public void testGenerateWithCondition() {
        AtomicInteger count = new AtomicInteger(0);
        LongIterator iter = LongIterator.generate(() -> count.get() < 3, () -> count.getAndIncrement());
        assertEquals(0L, iter.nextLong());
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerateWithConditionThrowsWhenExhausted() {
        LongIterator iter = LongIterator.generate(() -> false, () -> 1L);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testGenerateWithConditionNullChecks() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate(null, () -> 1L));
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate(() -> true, null));
    }

    @Test
    public void testNext() {
        LongIterator iter = LongIterator.of(10L, 20L);
        Long value = iter.next();
        assertEquals(10L, value);
    }

    @Test
    public void testNextBoxing() {
        LongIterator iter = LongIterator.of(100L);
        Object obj = iter.next();
        assertTrue(obj instanceof Long);
        assertEquals(100L, obj);
    }

    @Test
    public void testNextLong() {
        LongIterator iter = LongIterator.of(5L, 10L, 15L);
        assertEquals(5L, iter.nextLong());
        assertEquals(10L, iter.nextLong());
        assertEquals(15L, iter.nextLong());
    }

    @Test
    public void testNextLongThrowsWhenEmpty() {
        LongIterator iter = LongIterator.of(1L);
        iter.nextLong();
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testSkip() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L).skip(2);
        assertEquals(3L, iter.nextLong());
        assertEquals(4L, iter.nextLong());
        assertEquals(5L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipZero() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L).skip(0);
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
    }

    @Test
    public void testSkipAll() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L).skip(10);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNegative() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L, 2L).skip(-1));
    }

    @Test
    public void testSkipLazyEvaluation() {
        AtomicInteger consumed = new AtomicInteger(0);
        LongIterator base = LongIterator.generate(() -> {
            consumed.incrementAndGet();
            return 1L;
        });
        LongIterator skipped = base.skip(3);
        assertEquals(0, consumed.get());
        skipped.hasNext();
        assertEquals(3, consumed.get());
    }

    @Test
    public void testLimit() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L).limit(3);
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimitZero() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L).limit(0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimitMoreThanAvailable() {
        LongIterator iter = LongIterator.of(1L, 2L).limit(10);
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimitNegative() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L, 2L).limit(-1));
    }

    @Test
    public void testFilter() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L, 6L).filter(x -> x % 2 == 0);
        assertEquals(2L, iter.nextLong());
        assertEquals(4L, iter.nextLong());
        assertEquals(6L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterNoneMatch() {
        LongIterator iter = LongIterator.of(1L, 3L, 5L).filter(x -> x % 2 == 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterAllMatch() {
        LongIterator iter = LongIterator.of(2L, 4L, 6L).filter(x -> x % 2 == 0);
        assertEquals(2L, iter.nextLong());
        assertEquals(4L, iter.nextLong());
        assertEquals(6L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterNull() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L, 2L).filter(null));
    }

    @Test
    public void testFirst() {
        OptionalLong first = LongIterator.of(10L, 20L, 30L).first();
        assertTrue(first.isPresent());
        assertEquals(10L, first.get());
    }

    @Test
    public void testFirstEmpty() {
        OptionalLong first = LongIterator.empty().first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testFirstConsumesOne() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        iter.first();
        assertEquals(2L, iter.nextLong());
    }

    @Test
    public void testLast() {
        OptionalLong last = LongIterator.of(10L, 20L, 30L).last();
        assertTrue(last.isPresent());
        assertEquals(30L, last.get());
    }

    @Test
    public void testLastEmpty() {
        OptionalLong last = LongIterator.empty().last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testLastSingleElement() {
        OptionalLong last = LongIterator.of(42L).last();
        assertTrue(last.isPresent());
        assertEquals(42L, last.get());
    }

    @Test
    public void testLastConsumesAll() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        iter.last();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray() {
        long[] array = LongIterator.of(1L, 2L, 3L, 4L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testToArrayEmpty() {
        long[] array = LongIterator.empty().toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToArrayConsumesIterator() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        iter.toArray();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartiallyConsumed() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        iter.nextLong();
        iter.nextLong();
        long[] array = iter.toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, array);
    }

    @Test
    public void testToList() {
        LongList list = LongIterator.of(1L, 2L, 3L).toList();
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testToListEmpty() {
        LongList list = LongIterator.empty().toList();
        assertEquals(0, list.size());
    }

    @Test
    public void testToListConsumesIterator() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        iter.toList();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        LongStream stream = LongIterator.of(1L, 2L, 3L, 4L, 5L).stream();
        assertNotNull(stream);
        assertEquals(15L, stream.sum());
    }

    @Test
    public void testStreamEmpty() {
        LongStream stream = LongIterator.empty().stream();
        assertNotNull(stream);
        assertEquals(0L, stream.count());
    }

    @Test
    public void testStreamOperations() {
        long max = LongIterator.of(5L, 2L, 8L, 1L, 9L).stream().max().getAsLong();
        assertEquals(9L, max);
    }

    @Test
    public void testIndexed() {
        ObjIterator<IndexedLong> iter = LongIterator.of(10L, 20L, 30L).indexed();
        assertTrue(iter.hasNext());

        IndexedLong indexed0 = iter.next();
        assertEquals(10L, indexed0.value());
        assertEquals(0L, indexed0.index());

        IndexedLong indexed1 = iter.next();
        assertEquals(20L, indexed1.value());
        assertEquals(1L, indexed1.index());

        IndexedLong indexed2 = iter.next();
        assertEquals(30L, indexed2.value());
        assertEquals(2L, indexed2.index());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testIndexedEmpty() {
        ObjIterator<IndexedLong> iter = LongIterator.empty().indexed();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIndexedWithStart() {
        ObjIterator<IndexedLong> iter = LongIterator.of(100L, 200L).indexed(5);

        IndexedLong indexed0 = iter.next();
        assertEquals(100L, indexed0.value());
        assertEquals(5L, indexed0.index());

        IndexedLong indexed1 = iter.next();
        assertEquals(200L, indexed1.value());
        assertEquals(6L, indexed1.index());
    }

    @Test
    public void testIndexedWithStartNegative() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).indexed(-1));
    }

    @Test
    public void testIndexedWithStartZero() {
        ObjIterator<IndexedLong> iter = LongIterator.of(10L).indexed(0);
        IndexedLong indexed = iter.next();
        assertEquals(0L, indexed.index());
    }

    @Test
    public void testForEachRemaining() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        AtomicLong sum = new AtomicLong(0);
        iter.forEachRemaining((Long value) -> sum.addAndGet(value));
        assertEquals(6L, sum.get());
    }

    @Test
    public void testForeachRemaining() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        AtomicLong sum = new AtomicLong(0);
        iter.foreachRemaining(sum::addAndGet);
        assertEquals(15L, sum.get());
    }

    @Test
    public void testForeachRemainingEmpty() {
        LongIterator iter = LongIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachRemaining(value -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForeachRemainingNull() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).foreachRemaining(null));
    }

    @Test
    public void testForeachRemainingPartiallyConsumed() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        iter.nextLong();
        iter.nextLong();

        AtomicLong sum = new AtomicLong(0);
        iter.foreachRemaining(sum::addAndGet);
        assertEquals(12L, sum.get());
    }

    @Test
    public void testForeachIndexed() {
        LongIterator iter = LongIterator.of(10L, 20L, 30L);
        LongList indices = new LongList();
        LongList values = new LongList();

        iter.foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });

        assertEquals(3, indices.size());
        assertEquals(0, indices.get(0));
        assertEquals(1, indices.get(1));
        assertEquals(2, indices.get(2));

        assertEquals(3, values.size());
        assertEquals(10L, values.get(0));
        assertEquals(20L, values.get(1));
        assertEquals(30L, values.get(2));
    }

    @Test
    public void testForeachIndexedEmpty() {
        LongIterator iter = LongIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachIndexed((index, value) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexedNull() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.of(1L).foreachIndexed(null));
    }

    @Test
    public void testForeachIndexedPartiallyConsumed() {
        LongIterator iter = LongIterator.of(100L, 200L, 300L, 400L);
        iter.nextLong();

        IntList indices = new IntList();
        iter.foreachIndexed((index, value) -> indices.add(index));

        assertEquals(3, indices.size());
        assertEquals(0, indices.get(0));
        assertEquals(1, indices.get(1));
        assertEquals(2, indices.get(2));
    }

    @Test
    public void testChainedOperations() {
        long result = LongIterator.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L).filter(x -> x % 2 == 0).skip(1).limit(2).toArray()[0];
        assertEquals(4L, result);
    }

    @Test
    public void testComplexChain() {
        LongList list = LongIterator.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L).skip(2).filter(x -> x > 4).limit(3).toList();

        assertEquals(3, list.size());
        assertEquals(5L, list.get(0));
        assertEquals(6L, list.get(1));
        assertEquals(7L, list.get(2));
    }

    @Test
    public void testToArrayFromRange() {
        long[] array = { 10L, 20L, 30L, 40L, 50L };
        long[] result = LongIterator.of(array, 1, 4).toArray();
        assertArrayEquals(new long[] { 20L, 30L, 40L }, result);
    }

    @Test
    public void testMultipleHasNextCalls() {
        LongIterator iter = LongIterator.of(1L, 2L);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterWithMultipleConditions() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L).filter(x -> x > 3).filter(x -> x < 8).filter(x -> x % 2 == 0);

        assertEquals(4L, iter.nextLong());
        assertEquals(6L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimit() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L).skip(3).limit(4);

        long[] array = iter.toArray();
        assertArrayEquals(new long[] { 4L, 5L, 6L, 7L }, array);
    }
}
