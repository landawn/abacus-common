package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("2025")
public class IntIterator2025Test extends TestBase {

    @Test
    public void test_empty() {
        IntIterator iter = IntIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);

        IntIterator iter2 = IntIterator.empty();
        assertFalse(iter2.hasNext());
    }

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
    @SuppressWarnings("deprecation")
    public void test_next_deprecated() {
        IntIterator iter = IntIterator.of(1, 2, 3);
        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());
    }

    @Test
    public void test_nextInt() {
        IntIterator iter = IntIterator.of(10, 20, 30);
        assertEquals(10, iter.nextInt());
        assertEquals(20, iter.nextInt());
        assertEquals(30, iter.nextInt());
        assertThrows(NoSuchElementException.class, iter::nextInt);
    }

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
    public void test_first() {
        OptionalInt first = IntIterator.of(1, 2, 3).first();
        assertTrue(first.isPresent());
        assertEquals(1, first.get());

        OptionalInt empty = IntIterator.empty().first();
        assertFalse(empty.isPresent());
    }

    @Test
    public void test_last() {
        OptionalInt last = IntIterator.of(1, 2, 3).last();
        assertTrue(last.isPresent());
        assertEquals(3, last.get());

        OptionalInt empty = IntIterator.empty().last();
        assertFalse(empty.isPresent());
    }

    @Test
    public void test_toArray() {
        int[] array = IntIterator.of(1, 2, 3, 4, 5).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);

        int[] emptyArray = IntIterator.empty().toArray();
        assertEquals(0, emptyArray.length);
    }

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
    public void test_stream() {
        int sum = IntIterator.of(1, 2, 3, 4, 5).stream().sum();
        assertEquals(15, sum);

        long count = IntIterator.of(1, 2, 3).stream().filter(x -> x > 1).count();
        assertEquals(2, count);
    }

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
}
