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
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

@Tag("new-test")
public class IntIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        IntIterator iter = IntIterator.empty();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testOf_EmptyArray() {
        IntIterator iter = IntIterator.of();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testOf_NullArray() {
        IntIterator iter = IntIterator.of((int[]) null);

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testOf_SingleElement() {
        int[] array = { 42 };
        IntIterator iter = IntIterator.of(array);

        assertTrue(iter.hasNext());
        assertEquals(42, iter.nextInt());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testOf_MultipleElements() {
        int[] array = { 1, 2, 3 };
        IntIterator iter = IntIterator.of(array);

        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextInt());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_SpecialValues() {
        int[] array = { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE };
        IntIterator iter = IntIterator.of(array);

        assertEquals(Integer.MIN_VALUE, iter.nextInt());
        assertEquals(-1, iter.nextInt());
        assertEquals(0, iter.nextInt());
        assertEquals(1, iter.nextInt());
        assertEquals(Integer.MAX_VALUE, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIterator iter = IntIterator.of(array, 1, 4);

        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertEquals(4, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_EmptyRange() {
        int[] array = { 1, 2, 3 };
        IntIterator iter = IntIterator.of(array, 1, 1);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_FullArray() {
        int[] array = { 1, 2, 3 };
        IntIterator iter = IntIterator.of(array, 0, array.length);

        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_InvalidIndices() {
        int[] array = { 1, 2, 3 };

        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, 2, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<IntIterator> supplier = () -> {
            counter.incrementAndGet();
            return IntIterator.of(1, 2, 3);
        };

        IntIterator iter = IntIterator.defer(supplier);

        assertEquals(0, counter.get());
        assertTrue(iter.hasNext());
        assertEquals(1, counter.get());
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> IntIterator.defer(null));
    }

    @Test
    public void testGenerate_Infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        IntSupplier supplier = counter::getAndIncrement;

        IntIterator iter = IntIterator.generate(supplier);

        assertTrue(iter.hasNext());
        assertEquals(0, iter.nextInt());
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextInt());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_WithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        IntSupplier supplier = counter::getAndIncrement;

        IntIterator iter = IntIterator.generate(hasNext, supplier);

        assertTrue(iter.hasNext());
        assertEquals(0, iter.nextInt());
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextInt());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testGenerate_NullArguments() {
        IntSupplier supplier = () -> 0;
        BooleanSupplier hasNext = () -> true;

        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate((IntSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate(null, supplier));
        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate(hasNext, null));
    }

    @Test
    public void testNext_Deprecated() {
        IntIterator iter = IntIterator.of(42);

        Integer value = iter.next();
        assertEquals(Integer.valueOf(42), value);
    }

    @Test
    public void testSkip() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        IntIterator skipped = iter.skip(2);

        assertTrue(skipped.hasNext());
        assertEquals(3, skipped.nextInt());
        assertEquals(4, skipped.nextInt());
        assertEquals(5, skipped.nextInt());
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_Zero() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntIterator skipped = iter.skip(0);

        assertSame(iter, skipped);
    }

    @Test
    public void testSkip_MoreThanAvailable() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntIterator skipped = iter.skip(5);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_Negative() {
        IntIterator iter = IntIterator.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testLimit() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        IntIterator limited = iter.limit(3);

        assertTrue(limited.hasNext());
        assertEquals(1, limited.nextInt());
        assertEquals(2, limited.nextInt());
        assertEquals(3, limited.nextInt());
        assertFalse(limited.hasNext());
        assertThrows(NoSuchElementException.class, () -> limited.nextInt());
    }

    @Test
    public void testLimit_Zero() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntIterator limited = iter.limit(0);

        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_MoreThanAvailable() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntIterator limited = iter.limit(5);

        assertEquals(1, limited.nextInt());
        assertEquals(2, limited.nextInt());
        assertEquals(3, limited.nextInt());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_Negative() {
        IntIterator iter = IntIterator.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testFilter() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        IntPredicate evenPredicate = x -> x % 2 == 0;
        IntIterator filtered = iter.filter(evenPredicate);

        assertTrue(filtered.hasNext());
        assertEquals(2, filtered.nextInt());
        assertTrue(filtered.hasNext());
        assertEquals(4, filtered.nextInt());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilter_NoneMatch() {
        IntIterator iter = IntIterator.of(1, 3, 5);
        IntPredicate evenPredicate = x -> x % 2 == 0;
        IntIterator filtered = iter.filter(evenPredicate);

        assertFalse(filtered.hasNext());
        assertThrows(NoSuchElementException.class, () -> filtered.nextInt());
    }

    @Test
    public void testFilter_AllMatch() {
        IntIterator iter = IntIterator.of(2, 4, 6);
        IntPredicate evenPredicate = x -> x % 2 == 0;
        IntIterator filtered = iter.filter(evenPredicate);

        assertEquals(2, filtered.nextInt());
        assertEquals(4, filtered.nextInt());
        assertEquals(6, filtered.nextInt());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilter_NullPredicate() {
        IntIterator iter = IntIterator.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFirst() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        OptionalInt first = iter.first();

        assertTrue(first.isPresent());
        assertEquals(1, first.getAsInt());
    }

    @Test
    public void testFirst_Empty() {
        IntIterator iter = IntIterator.empty();
        OptionalInt first = iter.first();

        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        OptionalInt last = iter.last();

        assertTrue(last.isPresent());
        assertEquals(3, last.getAsInt());
    }

    @Test
    public void testLast_Empty() {
        IntIterator iter = IntIterator.empty();
        OptionalInt last = iter.last();

        assertFalse(last.isPresent());
    }

    @Test
    public void testLast_SingleElement() {
        IntIterator iter = IntIterator.of(42);
        OptionalInt last = iter.last();

        assertTrue(last.isPresent());
        assertEquals(42, last.getAsInt());
    }

    @Test
    public void testToArray() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        int[] array = iter.toArray();

        assertArrayEquals(new int[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToArray_Empty() {
        IntIterator iter = IntIterator.empty();
        int[] array = iter.toArray();

        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_PartiallyConsumed() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        iter.nextInt();
        iter.nextInt();

        int[] array = iter.toArray();
        assertArrayEquals(new int[] { 3, 4, 5 }, array);
    }

    @Test
    public void testToList() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntList list = iter.toList();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testToList_Empty() {
        IntIterator iter = IntIterator.empty();
        IntList list = iter.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    public void testStream() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntStream stream = iter.stream();

        assertNotNull(stream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testIndexed() {
        IntIterator iter = IntIterator.of(10, 20, 30);
        ObjIterator<IndexedInt> indexed = iter.indexed();

        assertTrue(indexed.hasNext());
        IndexedInt first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(10, first.value());

        IndexedInt second = indexed.next();
        assertEquals(1, second.index());
        assertEquals(20, second.value());

        IndexedInt third = indexed.next();
        assertEquals(2, third.index());
        assertEquals(30, third.value());

        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_WithStartIndex() {
        IntIterator iter = IntIterator.of(10, 20, 30);
        ObjIterator<IndexedInt> indexed = iter.indexed(100);

        IndexedInt first = indexed.next();
        assertEquals(100, first.index());
        assertEquals(10, first.value());

        IndexedInt second = indexed.next();
        assertEquals(101, second.index());
        assertEquals(20, second.value());

        IndexedInt third = indexed.next();
        assertEquals(102, third.index());
        assertEquals(30, third.value());
    }

    @Test
    public void testIndexed_NegativeStartIndex() {
        IntIterator iter = IntIterator.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testForEachRemaining_Deprecated() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        AtomicInteger sum = new AtomicInteger(0);

        iter.forEachRemaining((Integer i) -> sum.addAndGet(i));

        assertEquals(6, sum.get());
    }

    @Test
    public void testForeachRemaining() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        AtomicInteger sum = new AtomicInteger(0);

        iter.foreachRemaining(sum::addAndGet);

        assertEquals(6, sum.get());
    }

    @Test
    public void testForeachRemaining_PartiallyConsumed() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        iter.nextInt();
        iter.nextInt();

        AtomicInteger sum = new AtomicInteger(0);
        iter.foreachRemaining(sum::addAndGet);

        assertEquals(12, sum.get());
    }

    @Test
    public void testForeachRemaining_Empty() {
        IntIterator iter = IntIterator.empty();
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachRemaining(i -> count.incrementAndGet());

        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed() {
        IntIterator iter = IntIterator.of(10, 20, 30);
        AtomicInteger indexSum = new AtomicInteger(0);
        AtomicInteger valueSum = new AtomicInteger(0);

        iter.foreachIndexed((index, value) -> {
            indexSum.addAndGet(index);
            valueSum.addAndGet(value);
        });

        assertEquals(3, indexSum.get());
        assertEquals(60, valueSum.get());
    }

    @Test
    public void testForeachIndexed_Empty() {
        IntIterator iter = IntIterator.empty();
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachIndexed((index, value) -> count.incrementAndGet());

        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed_NullAction() {
        IntIterator iter = IntIterator.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }
}
