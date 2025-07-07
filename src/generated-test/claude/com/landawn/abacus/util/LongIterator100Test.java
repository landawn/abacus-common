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
import java.util.function.BooleanSupplier;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

public class LongIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        LongIterator iter = LongIterator.empty();
        
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testOf_EmptyArray() {
        LongIterator iter = LongIterator.of();
        
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testOf_NullArray() {
        LongIterator iter = LongIterator.of((long[]) null);
        
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testOf_SingleElement() {
        long[] array = {42L};
        LongIterator iter = LongIterator.of(array);
        
        assertTrue(iter.hasNext());
        assertEquals(42L, iter.nextLong());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testOf_MultipleElements() {
        long[] array = {1L, 2L, 3L};
        LongIterator iter = LongIterator.of(array);
        
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_SpecialValues() {
        long[] array = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};
        LongIterator iter = LongIterator.of(array);
        
        assertEquals(Long.MIN_VALUE, iter.nextLong());
        assertEquals(-1L, iter.nextLong());
        assertEquals(0L, iter.nextLong());
        assertEquals(1L, iter.nextLong());
        assertEquals(Long.MAX_VALUE, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange() {
        long[] array = {1L, 2L, 3L, 4L, 5L};
        LongIterator iter = LongIterator.of(array, 1, 4);
        
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertEquals(4L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_EmptyRange() {
        long[] array = {1L, 2L, 3L};
        LongIterator iter = LongIterator.of(array, 1, 1);
        
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_FullArray() {
        long[] array = {1L, 2L, 3L};
        LongIterator iter = LongIterator.of(array, 0, array.length);
        
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_InvalidIndices() {
        long[] array = {1L, 2L, 3L};
        
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> LongIterator.of(array, 2, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<LongIterator> supplier = () -> {
            counter.incrementAndGet();
            return LongIterator.of(1L, 2L, 3L);
        };
        
        LongIterator iter = LongIterator.defer(supplier);
        
        assertEquals(0, counter.get());
        assertTrue(iter.hasNext());
        assertEquals(1, counter.get());
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> LongIterator.defer(null));
    }

    @Test
    public void testGenerate_Infinite() {
        AtomicLong counter = new AtomicLong(0);
        LongSupplier supplier = () -> counter.getAndIncrement();
        
        LongIterator iter = LongIterator.generate(supplier);
        
        assertTrue(iter.hasNext());
        assertEquals(0L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_WithHasNext() {
        AtomicLong counter = new AtomicLong(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        LongSupplier supplier = () -> counter.getAndIncrement();
        
        LongIterator iter = LongIterator.generate(hasNext, supplier);
        
        assertTrue(iter.hasNext());
        assertEquals(0L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testGenerate_NullArguments() {
        LongSupplier supplier = () -> 0L;
        BooleanSupplier hasNext = () -> true;
        
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate((LongSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate(null, supplier));
        assertThrows(IllegalArgumentException.class, () -> LongIterator.generate(hasNext, null));
    }

    @Test
    public void testNext_Deprecated() {
        LongIterator iter = LongIterator.of(42L);
        
        Long value = iter.next();
        assertEquals(Long.valueOf(42L), value);
    }

    @Test
    public void testSkip() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        LongIterator skipped = iter.skip(2);
        
        assertTrue(skipped.hasNext());
        assertEquals(3L, skipped.nextLong());
        assertEquals(4L, skipped.nextLong());
        assertEquals(5L, skipped.nextLong());
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_Zero() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        LongIterator skipped = iter.skip(0);
        
        assertSame(iter, skipped);
    }

    @Test
    public void testSkip_MoreThanAvailable() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        LongIterator skipped = iter.skip(5);
        
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_Negative() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        
        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testLimit() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        LongIterator limited = iter.limit(3);
        
        assertTrue(limited.hasNext());
        assertEquals(1L, limited.nextLong());
        assertEquals(2L, limited.nextLong());
        assertEquals(3L, limited.nextLong());
        assertFalse(limited.hasNext());
        assertThrows(NoSuchElementException.class, () -> limited.nextLong());
    }

    @Test
    public void testLimit_Zero() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        LongIterator limited = iter.limit(0);
        
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_MoreThanAvailable() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        LongIterator limited = iter.limit(5);
        
        assertEquals(1L, limited.nextLong());
        assertEquals(2L, limited.nextLong());
        assertEquals(3L, limited.nextLong());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_Negative() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        
        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testFilter() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        LongPredicate evenPredicate = x -> x % 2 == 0;
        LongIterator filtered = iter.filter(evenPredicate);
        
        assertTrue(filtered.hasNext());
        assertEquals(2L, filtered.nextLong());
        assertTrue(filtered.hasNext());
        assertEquals(4L, filtered.nextLong());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilter_NoneMatch() {
        LongIterator iter = LongIterator.of(1L, 3L, 5L);
        LongPredicate evenPredicate = x -> x % 2 == 0;
        LongIterator filtered = iter.filter(evenPredicate);
        
        assertFalse(filtered.hasNext());
        assertThrows(NoSuchElementException.class, () -> filtered.nextLong());
    }

    @Test
    public void testFilter_AllMatch() {
        LongIterator iter = LongIterator.of(2L, 4L, 6L);
        LongPredicate evenPredicate = x -> x % 2 == 0;
        LongIterator filtered = iter.filter(evenPredicate);
        
        assertEquals(2L, filtered.nextLong());
        assertEquals(4L, filtered.nextLong());
        assertEquals(6L, filtered.nextLong());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilter_NullPredicate() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        
        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFirst() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        OptionalLong first = iter.first();
        
        assertTrue(first.isPresent());
        assertEquals(1L, first.getAsLong());
    }

    @Test
    public void testFirst_Empty() {
        LongIterator iter = LongIterator.empty();
        OptionalLong first = iter.first();
        
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        OptionalLong last = iter.last();
        
        assertTrue(last.isPresent());
        assertEquals(3L, last.getAsLong());
    }

    @Test
    public void testLast_Empty() {
        LongIterator iter = LongIterator.empty();
        OptionalLong last = iter.last();
        
        assertFalse(last.isPresent());
    }

    @Test
    public void testLast_SingleElement() {
        LongIterator iter = LongIterator.of(42L);
        OptionalLong last = iter.last();
        
        assertTrue(last.isPresent());
        assertEquals(42L, last.getAsLong());
    }

    @Test
    public void testToArray() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        long[] array = iter.toArray();
        
        assertArrayEquals(new long[]{1L, 2L, 3L}, array);
    }

    @Test
    public void testToArray_Empty() {
        LongIterator iter = LongIterator.empty();
        long[] array = iter.toArray();
        
        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_PartiallyConsumed() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        iter.nextLong();
        iter.nextLong();
        
        long[] array = iter.toArray();
        assertArrayEquals(new long[]{3L, 4L, 5L}, array);
    }

    @Test
    public void testToList() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        LongList list = iter.toList();
        
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testToList_Empty() {
        LongIterator iter = LongIterator.empty();
        LongList list = iter.toList();
        
        assertTrue(list.isEmpty());
    }

    @Test
    public void testStream() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        LongStream stream = iter.stream();
        
        assertNotNull(stream);
        assertArrayEquals(new long[]{1L, 2L, 3L}, stream.toArray());
    }

    @Test
    public void testIndexed() {
        LongIterator iter = LongIterator.of(10L, 20L, 30L);
        ObjIterator<IndexedLong> indexed = iter.indexed();
        
        assertTrue(indexed.hasNext());
        IndexedLong first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(10L, first.value());
        
        IndexedLong second = indexed.next();
        assertEquals(1, second.index());
        assertEquals(20L, second.value());
        
        IndexedLong third = indexed.next();
        assertEquals(2, third.index());
        assertEquals(30L, third.value());
        
        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_WithStartIndex() {
        LongIterator iter = LongIterator.of(10L, 20L, 30L);
        ObjIterator<IndexedLong> indexed = iter.indexed(100);
        
        IndexedLong first = indexed.next();
        assertEquals(100, first.index());
        assertEquals(10L, first.value());
        
        IndexedLong second = indexed.next();
        assertEquals(101, second.index());
        assertEquals(20L, second.value());
        
        IndexedLong third = indexed.next();
        assertEquals(102, third.index());
        assertEquals(30L, third.value());
    }

    @Test
    public void testIndexed_NegativeStartIndex() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testForEachRemaining_Deprecated() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        AtomicLong sum = new AtomicLong(0);
        
        iter.forEachRemaining((Long l) -> sum.addAndGet(l));
        
        assertEquals(6L, sum.get());
    }

    @Test
    public void testForeachRemaining() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        AtomicLong sum = new AtomicLong(0);
        
        iter.foreachRemaining(sum::addAndGet);
        
        assertEquals(6L, sum.get());
    }

    @Test
    public void testForeachRemaining_PartiallyConsumed() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
        iter.nextLong();
        iter.nextLong();
        
        AtomicLong sum = new AtomicLong(0);
        iter.foreachRemaining(sum::addAndGet);
        
        assertEquals(12L, sum.get()); // 3 + 4 + 5
    }

    @Test
    public void testForeachRemaining_Empty() {
        LongIterator iter = LongIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        
        iter.foreachRemaining(l -> count.incrementAndGet());
        
        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed() {
        LongIterator iter = LongIterator.of(10L, 20L, 30L);
        AtomicInteger indexSum = new AtomicInteger(0);
        AtomicLong valueSum = new AtomicLong(0);
        
        iter.foreachIndexed((index, value) -> {
            indexSum.addAndGet(index);
            valueSum.addAndGet(value);
        });
        
        assertEquals(3, indexSum.get()); // 0 + 1 + 2
        assertEquals(60L, valueSum.get()); // 10 + 20 + 30
    }

    @Test
    public void testForeachIndexed_Empty() {
        LongIterator iter = LongIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        
        iter.foreachIndexed((index, value) -> count.incrementAndGet());
        
        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed_NullAction() {
        LongIterator iter = LongIterator.of(1L, 2L, 3L);
        
        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }
}