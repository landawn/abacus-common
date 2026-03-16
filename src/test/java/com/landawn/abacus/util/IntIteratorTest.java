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

@Tag("2025")
public class IntIteratorTest extends TestBase {

    // =================================================
    // empty()
    // =================================================

    @Test
    public void test_empty() {
        IntIterator iter = IntIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);

        IntIterator iter2 = IntIterator.empty();
        assertFalse(iter2.hasNext());
    }

    @Test
    public void testEmpty() {
        IntIterator iter = IntIterator.empty();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testEmpty_SameSingleton() {
        assertSame(IntIterator.EMPTY, IntIterator.empty());
    }

    // =================================================
    // of(int...)
    // =================================================

    @Test
    public void test_of_varargs() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertEquals(4, iter.nextInt());
        assertEquals(5, iter.nextInt());
        assertFalse(iter.hasNext());

        IntIterator empty = IntIterator.of();
        assertFalse(empty.hasNext());

        IntIterator nullIter = IntIterator.of((int[]) null);
        assertFalse(nullIter.hasNext());
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

    // =================================================
    // of(int[], int, int)
    // =================================================

    @Test
    public void test_of_array_range() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIterator iter = IntIterator.of(array, 1, 4);
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertEquals(4, iter.nextInt());
        assertFalse(iter.hasNext());

        IntIterator full = IntIterator.of(array, 0, array.length);
        int count = 0;
        while (full.hasNext()) {
            full.nextInt();
            count++;
        }
        assertEquals(5, count);

        IntIterator emptyRange = IntIterator.of(array, 2, 2);
        assertFalse(emptyRange.hasNext());
    }

    @Test
    public void test_of_array_range_exceptions() {
        int[] array = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of(array, 2, 1));
    }

    @Test
    public void test_of_array_range_toArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIterator iter = IntIterator.of(array, 1, 4);
        int[] result = iter.toArray();
        assertArrayEquals(new int[] { 2, 3, 4 }, result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_array_range_toList() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIterator iter = IntIterator.of(array, 1, 4);
        IntList result = iter.toList();
        assertEquals(3, result.size());
        assertEquals(2, result.get(0));
        assertEquals(3, result.get(1));
        assertEquals(4, result.get(2));
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
    public void testOf_WithRange_NullArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> IntIterator.of((int[]) null, 0, 1));
    }

    // =================================================
    // defer()
    // =================================================

    @Test
    public void test_defer() {
        final boolean[] initialized = { false };
        IntIterator iter = IntIterator.defer(() -> {
            initialized[0] = true;
            return IntIterator.of(1, 2, 3);
        });

        assertFalse(initialized[0]);
        assertTrue(iter.hasNext());
        assertTrue(initialized[0]);

        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_defer_null_supplier() {
        assertThrows(IllegalArgumentException.class, () -> IntIterator.defer(null));
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
    public void testDefer_CalledOnNextInt() {
        boolean[] supplierCalled = { false };
        Supplier<IntIterator> supplier = () -> {
            supplierCalled[0] = true;
            return IntIterator.of(42);
        };

        IntIterator iter = IntIterator.defer(supplier);
        assertFalse(supplierCalled[0]);

        assertEquals(42, iter.nextInt());
        assertTrue(supplierCalled[0]);
    }

    @Test
    public void testDefer_SupplierCalledOnce() {
        int[] callCount = { 0 };
        Supplier<IntIterator> supplier = () -> {
            callCount[0]++;
            return IntIterator.of(1, 2);
        };

        IntIterator iter = IntIterator.defer(supplier);
        iter.hasNext();
        iter.nextInt();
        iter.hasNext();
        iter.nextInt();

        assertEquals(1, callCount[0], "Supplier should only be called once");
    }

    // =================================================
    // generate(IntSupplier)
    // =================================================

    @Test
    public void test_generate_infinite() {
        final int[] counter = { 0 };
        IntIterator iter = IntIterator.generate(() -> counter[0]++);

        assertTrue(iter.hasNext());
        assertEquals(0, iter.nextInt());
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertTrue(iter.hasNext());
    }

    @Test
    public void test_generate_null_supplier() {
        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate((java.util.function.IntSupplier) null));
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

    // =================================================
    // generate(BooleanSupplier, IntSupplier)
    // =================================================

    @Test
    public void test_generate_conditional() {
        final int[] counter = { 0 };
        IntIterator iter = IntIterator.generate(() -> counter[0] < 5, () -> counter[0]++);

        assertEquals(0, iter.nextInt());
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertEquals(4, iter.nextInt());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);
    }

    @Test
    public void test_generate_conditional_null_params() {
        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate(null, () -> 1));
        assertThrows(IllegalArgumentException.class, () -> IntIterator.generate(() -> true, null));
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
    public void testGenerate_WithCondition_NoElements() {
        IntIterator iter = IntIterator.generate(() -> false, () -> 1);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    // =================================================
    // next() [deprecated]
    // =================================================

    @Test
    @SuppressWarnings("deprecation")
    public void test_next_deprecated() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());
    }

    @Test
    public void testNext_Deprecated() {
        IntIterator iter = IntIterator.of(42);

        Integer value = iter.next();
        assertEquals(Integer.valueOf(42), value);
    }

    // =================================================
    // nextInt()
    // =================================================

    @Test
    public void test_nextInt() {
        IntIterator iter = IntIterator.of(10, 20, 30);
        assertEquals(10, iter.nextInt());
        assertEquals(20, iter.nextInt());
        assertEquals(30, iter.nextInt());
        assertThrows(NoSuchElementException.class, iter::nextInt);
    }

    // =================================================
    // skip(long)
    // =================================================

    @Test
    public void test_skip() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        IntIterator skipped = iter.skip(2);
        assertEquals(3, skipped.nextInt());
        assertEquals(4, skipped.nextInt());
        assertEquals(5, skipped.nextInt());
        assertFalse(skipped.hasNext());
    }

    @Test
    public void test_skip_zero() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntIterator skipped = iter.skip(0);
        assertSame(iter, skipped);
        assertEquals(1, skipped.nextInt());
    }

    @Test
    public void test_skip_more_than_available() {
        IntIterator iter = IntIterator.of(1, 2);
        IntIterator skipped = iter.skip(10);
        assertFalse(skipped.hasNext());
    }

    @Test
    public void test_skip_negative() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
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
    public void testSkip_NoSuchElementAfterExhaustion() {
        IntIterator iter = IntIterator.of(1, 2).skip(2);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    // =================================================
    // limit(long)
    // =================================================

    @Test
    public void test_limit() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        IntIterator limited = iter.limit(3);
        assertEquals(1, limited.nextInt());
        assertEquals(2, limited.nextInt());
        assertEquals(3, limited.nextInt());
        assertFalse(limited.hasNext());
    }

    @Test
    public void test_limit_zero() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntIterator limited = iter.limit(0);
        assertFalse(limited.hasNext());
    }

    @Test
    public void test_limit_more_than_available() {
        IntIterator iter = IntIterator.of(1, 2);
        IntIterator limited = iter.limit(10);
        assertEquals(1, limited.nextInt());
        assertEquals(2, limited.nextInt());
        assertFalse(limited.hasNext());
    }

    @Test
    public void test_limit_negative() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
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
    public void testLimit_MoreThanAvailable() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntIterator limited = iter.limit(5);

        assertEquals(1, limited.nextInt());
        assertEquals(2, limited.nextInt());
        assertEquals(3, limited.nextInt());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_NoSuchElementAfterExhaustion() {
        IntIterator iter = IntIterator.of(1, 2).limit(1);
        assertEquals(1, iter.nextInt());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    // =================================================
    // filter(IntPredicate)
    // =================================================

    @Test
    public void test_filter() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5, 6);
        IntIterator filtered = iter.filter(x -> x % 2 == 0);
        assertEquals(2, filtered.nextInt());
        assertEquals(4, filtered.nextInt());
        assertEquals(6, filtered.nextInt());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void test_filter_none_match() {
        IntIterator iter = IntIterator.of(1, 3, 5);
        IntIterator filtered = iter.filter(x -> x % 2 == 0);
        assertFalse(filtered.hasNext());
    }

    @Test
    public void test_filter_all_match() {
        IntIterator iter = IntIterator.of(2, 4, 6);
        IntIterator filtered = iter.filter(x -> x % 2 == 0);
        assertEquals(2, filtered.nextInt());
        assertEquals(4, filtered.nextInt());
        assertEquals(6, filtered.nextInt());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void test_filter_null_predicate() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
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
    public void testFilter_NoSuchElementWhenNoneMatch() {
        IntIterator iter = IntIterator.of(1, 3, 5).filter(x -> x % 2 == 0);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    // =================================================
    // toArray()
    // =================================================

    @Test
    public void test_toArray() {
        int[] array = IntIterator.of(1, 2, 3, 4, 5).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);

        int[] emptyArray = IntIterator.empty().toArray();
        assertEquals(0, emptyArray.length);
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

    // =================================================
    // toList()
    // =================================================

    @Test
    public void test_toList() {
        IntList list = IntIterator.of(1, 2, 3).toList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));

        IntList emptyList = IntIterator.empty().toList();
        assertEquals(0, emptyList.size());
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

    // =================================================
    // stream()
    // =================================================

    @Test
    public void test_stream() {
        int sum = IntIterator.of(1, 2, 3, 4, 5).stream().sum();
        assertEquals(15, sum);

        long count = IntIterator.of(1, 2, 3).stream().filter(x -> x > 1).count();
        assertEquals(2, count);
    }

    @Test
    public void testStream() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntStream stream = iter.stream();

        assertNotNull(stream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testStream_Empty() {
        IntIterator iter = IntIterator.empty();
        IntStream stream = iter.stream();

        assertNotNull(stream);
        assertEquals(0, stream.toArray().length);
    }

    // =================================================
    // indexed()
    // =================================================

    @Test
    public void test_indexed_default() {
        ObjIterator<IndexedInt> indexed = IntIterator.of(10, 20, 30).indexed();

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
    public void testIndexed_Empty() {
        ObjIterator<IndexedInt> indexed = IntIterator.empty().indexed();
        assertFalse(indexed.hasNext());
    }

    // =================================================
    // indexed(long)
    // =================================================

    @Test
    public void test_indexed_with_start() {
        ObjIterator<IndexedInt> indexed = IntIterator.of(10, 20, 30).indexed(100);

        IndexedInt first = indexed.next();
        assertEquals(100, first.index());
        assertEquals(10, first.value());

        IndexedInt second = indexed.next();
        assertEquals(101, second.index());
        assertEquals(20, second.value());

        IndexedInt third = indexed.next();
        assertEquals(102, third.index());
        assertEquals(30, third.value());

        assertFalse(indexed.hasNext());
    }

    @Test
    public void test_indexed_negative_start() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
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
    public void testIndexed_WithStartIndex_Zero() {
        IntIterator iter = IntIterator.of(10, 20);
        ObjIterator<IndexedInt> indexed = iter.indexed(0);

        IndexedInt first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(10, first.value());
    }

    // =================================================
    // forEachRemaining() [deprecated]
    // =================================================

    @Test
    @SuppressWarnings("deprecation")
    public void test_forEachRemaining_deprecated() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        IntList result = new IntList();
        iter.forEachRemaining((Integer i) -> result.add(i));
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
    }

    @Test
    public void testForEachRemaining_Deprecated() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        AtomicInteger sum = new AtomicInteger(0);

        iter.forEachRemaining((Integer i) -> sum.addAndGet(i));

        assertEquals(6, sum.get());
    }

    @Test
    public void testForEachRemaining_Empty() {
        IntIterator iter = IntIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((Integer i) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    // =================================================
    // foreachRemaining()
    // =================================================

    @Test
    public void test_foreachRemaining() {
        IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
        IntList result = new IntList();
        iter.foreachRemaining(result::add);

        assertEquals(5, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
        assertEquals(4, result.get(3));
        assertEquals(5, result.get(4));
    }

    @Test
    public void test_foreachRemaining_empty() {
        IntIterator iter = IntIterator.empty();
        final int[] count = { 0 };
        iter.foreachRemaining(x -> count[0]++);
        assertEquals(0, count[0]);
    }

    @Test
    public void test_foreachRemaining_null_action() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
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

    // =================================================
    // foreachIndexed()
    // =================================================

    @Test
    public void test_foreachIndexed() {
        IntIterator iter = IntIterator.of(10, 20, 30);
        IntList indices = new IntList();
        IntList values = new IntList();

        iter.foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });

        assertEquals(3, indices.size());
        assertEquals(0, indices.get(0));
        assertEquals(1, indices.get(1));
        assertEquals(2, indices.get(2));

        assertEquals(3, values.size());
        assertEquals(10, values.get(0));
        assertEquals(20, values.get(1));
        assertEquals(30, values.get(2));
    }

    @Test
    public void test_foreachIndexed_empty() {
        IntIterator iter = IntIterator.empty();
        final int[] count = { 0 };
        iter.foreachIndexed((i, v) -> count[0]++);
        assertEquals(0, count[0]);
    }

    @Test
    public void test_foreachIndexed_null_action() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
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
    public void testForeachIndexed_PartiallyConsumed() {
        IntIterator iter = IntIterator.of(10, 20, 30);
        iter.nextInt();
        int[] firstIndex = { -1 };

        iter.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });

        assertEquals(0, firstIndex[0], "Index should start from 0 even if iterator partially consumed");
    }

    // =================================================
    // Integration / combined tests
    // =================================================

    @Test
    public void test_combined_operations() {
        int sum = IntIterator.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).skip(2).limit(6).filter(x -> x % 2 == 0).stream().sum();
        assertEquals(18, sum);
    }

    @Test
    public void test_iterator_consumption() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        iter.toArray();
        assertFalse(iter.hasNext());

        IntIterator iter2 = IntIterator.of(1, 2, 3);
        iter2.toList();
        assertFalse(iter2.hasNext());

        IntIterator iter3 = IntIterator.of(1, 2, 3);
        iter3.foreachRemaining(x -> {
        });
        assertFalse(iter3.hasNext());
    }

    @Test
    public void testMultipleHasNextCalls() {
        IntIterator iter = IntIterator.of(1, 2);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextInt());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

}
