package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalFloat;

@Tag("2025")
public class FloatIterator2025Test extends TestBase {

    @Test
    public void test_empty() {
        FloatIterator iter = FloatIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());

        FloatIterator iter2 = FloatIterator.empty();
        assertTrue(iter == iter2);
    }

    @Test
    public void test_of_array() {
        float[] array = { 1.0f, 2.5f, 3.7f };
        FloatIterator iter = FloatIterator.of(array);
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat(), 0.0f);
        assertEquals(2.5f, iter.nextFloat(), 0.0f);
        assertEquals(3.7f, iter.nextFloat(), 0.0f);
        assertFalse(iter.hasNext());

        FloatIterator nullIter = FloatIterator.of((float[]) null);
        assertFalse(nullIter.hasNext());

        FloatIterator emptyIter = FloatIterator.of(new float[0]);
        assertFalse(emptyIter.hasNext());
    }

    @Test
    public void test_of_array_with_range() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };

        FloatIterator iter = FloatIterator.of(array, 1, 4);
        assertTrue(iter.hasNext());
        assertEquals(2.0f, iter.nextFloat(), 0.0f);
        assertEquals(3.0f, iter.nextFloat(), 0.0f);
        assertEquals(4.0f, iter.nextFloat(), 0.0f);
        assertFalse(iter.hasNext());

        FloatIterator emptyIter = FloatIterator.of(array, 2, 2);
        assertFalse(emptyIter.hasNext());

        FloatIterator nullIter = FloatIterator.of(null, 0, 0);
        assertFalse(nullIter.hasNext());

        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, 3, 2));
    }

    @Test
    public void test_of_array_toArray() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatIterator iter = FloatIterator.of(array, 1, 4);

        float[] result = iter.toArray();
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, result, 0.0f);

        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_array_toList() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatIterator iter = FloatIterator.of(array, 1, 4);

        FloatList result = iter.toList();
        assertEquals(3, result.size());
        assertEquals(2.0f, result.get(0), 0.0f);
        assertEquals(3.0f, result.get(1), 0.0f);
        assertEquals(4.0f, result.get(2), 0.0f);

        assertFalse(iter.hasNext());
    }

    @Test
    public void test_defer() {
        AtomicInteger callCount = new AtomicInteger(0);

        FloatIterator iter = FloatIterator.defer(() -> {
            callCount.incrementAndGet();
            return FloatIterator.of(1.0f, 2.0f, 3.0f);
        });

        assertEquals(0, callCount.get());

        assertTrue(iter.hasNext());
        assertEquals(1, callCount.get());

        assertEquals(1.0f, iter.nextFloat(), 0.0f);
        assertEquals(1, callCount.get());

        assertEquals(2.0f, iter.nextFloat(), 0.0f);
        assertEquals(3.0f, iter.nextFloat(), 0.0f);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.defer(null));
    }

    @Test
    public void test_generate() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatIterator iter = FloatIterator.generate(() -> counter.getAndIncrement() * 1.5f);

        assertTrue(iter.hasNext());
        assertEquals(0.0f, iter.nextFloat(), 0.0f);
        assertTrue(iter.hasNext());
        assertEquals(1.5f, iter.nextFloat(), 0.0f);
        assertTrue(iter.hasNext());
        assertEquals(3.0f, iter.nextFloat(), 0.0f);

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate(null));
    }

    @Test
    public void test_generate_with_hasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatIterator iter = FloatIterator.generate(() -> counter.get() < 3, () -> counter.getAndIncrement() * 2.0f);

        assertTrue(iter.hasNext());
        assertEquals(0.0f, iter.nextFloat(), 0.0f);
        assertTrue(iter.hasNext());
        assertEquals(2.0f, iter.nextFloat(), 0.0f);
        assertTrue(iter.hasNext());
        assertEquals(4.0f, iter.nextFloat(), 0.0f);
        assertFalse(iter.hasNext());

        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate(null, () -> 1.0f));
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate(() -> true, null));
    }

    @Test
    public void test_next_deprecated() {
        FloatIterator iter = FloatIterator.of(1.5f, 2.5f);

        Float boxed = iter.next();
        assertEquals(1.5f, boxed, 0.0f);
    }

    @Test
    public void test_nextFloat() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);

        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat(), 0.0f);
        assertTrue(iter.hasNext());
        assertEquals(2.0f, iter.nextFloat(), 0.0f);
        assertTrue(iter.hasNext());
        assertEquals(3.0f, iter.nextFloat(), 0.0f);
        assertFalse(iter.hasNext());

        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void test_skip() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatIterator skipped = iter.skip(2);

        assertTrue(skipped.hasNext());
        assertEquals(3.0f, skipped.nextFloat(), 0.0f);
        assertEquals(4.0f, skipped.nextFloat(), 0.0f);
        assertEquals(5.0f, skipped.nextFloat(), 0.0f);
        assertFalse(skipped.hasNext());

        FloatIterator iter2 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator skip0 = iter2.skip(0);
        assertTrue(iter2 == skip0);

        FloatIterator iter3 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator skipped3 = iter3.skip(10);
        assertFalse(skipped3.hasNext());

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1.0f).skip(-1));
    }

    @Test
    public void test_limit() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatIterator limited = iter.limit(3);

        assertTrue(limited.hasNext());
        assertEquals(1.0f, limited.nextFloat(), 0.0f);
        assertEquals(2.0f, limited.nextFloat(), 0.0f);
        assertEquals(3.0f, limited.nextFloat(), 0.0f);
        assertFalse(limited.hasNext());

        FloatIterator iter2 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator limited0 = iter2.limit(0);
        assertFalse(limited0.hasNext());

        FloatIterator iter3 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator limited3 = iter3.limit(10);
        assertEquals(1.0f, limited3.nextFloat(), 0.0f);
        assertEquals(2.0f, limited3.nextFloat(), 0.0f);
        assertFalse(limited3.hasNext());

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1.0f).limit(-1));
    }

    @Test
    public void test_filter() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.5f, 3.0f, 4.5f, 5.0f);
        FloatIterator filtered = iter.filter(x -> x > 2.5f);

        assertTrue(filtered.hasNext());
        assertEquals(3.0f, filtered.nextFloat(), 0.0f);
        assertEquals(4.5f, filtered.nextFloat(), 0.0f);
        assertEquals(5.0f, filtered.nextFloat(), 0.0f);
        assertFalse(filtered.hasNext());

        FloatIterator iter2 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator filtered2 = iter2.filter(x -> x > 10.0f);
        assertFalse(filtered2.hasNext());

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1.0f).filter(null));
    }

    @Test
    public void test_first() {
        FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
        OptionalFloat first = iter.first();
        assertTrue(first.isPresent());
        assertEquals(1.5f, first.get(), 0.0f);

        assertTrue(iter.hasNext());
        assertEquals(2.5f, iter.nextFloat(), 0.0f);

        FloatIterator emptyIter = FloatIterator.empty();
        OptionalFloat emptyFirst = emptyIter.first();
        assertFalse(emptyFirst.isPresent());
    }

    @Test
    public void test_last() {
        FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
        OptionalFloat last = iter.last();
        assertTrue(last.isPresent());
        assertEquals(3.5f, last.get(), 0.0f);

        assertFalse(iter.hasNext());

        FloatIterator emptyIter = FloatIterator.empty();
        OptionalFloat emptyLast = emptyIter.last();
        assertFalse(emptyLast.isPresent());
    }

    @Test
    public void test_toArray() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        float[] array = iter.toArray();

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, array, 0.0f);
        assertFalse(iter.hasNext());

        FloatIterator emptyIter = FloatIterator.empty();
        float[] emptyArray = emptyIter.toArray();
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void test_toList() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatList list = iter.toList();

        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0f);
        assertEquals(2.0f, list.get(1), 0.0f);
        assertEquals(3.0f, list.get(2), 0.0f);
        assertFalse(iter.hasNext());

        FloatIterator emptyIter = FloatIterator.empty();
        FloatList emptyList = emptyIter.toList();
        assertEquals(0, emptyList.size());
    }

    @Test
    public void test_stream() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f);

        double sum = iter.stream().filter(x -> x > 2.0f).sum();

        assertEquals(7.0, sum, 0.001);
    }

    @Test
    public void test_indexed() {
        FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
        ObjIterator<IndexedFloat> indexed = iter.indexed();

        assertTrue(indexed.hasNext());
        IndexedFloat first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(1.5f, first.value(), 0.0f);

        IndexedFloat second = indexed.next();
        assertEquals(1, second.index());
        assertEquals(2.5f, second.value(), 0.0f);

        IndexedFloat third = indexed.next();
        assertEquals(2, third.index());
        assertEquals(3.5f, third.value(), 0.0f);

        assertFalse(indexed.hasNext());
    }

    @Test
    public void test_indexed_with_startIndex() {
        FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
        ObjIterator<IndexedFloat> indexed = iter.indexed(10);

        assertTrue(indexed.hasNext());
        IndexedFloat first = indexed.next();
        assertEquals(10, first.index());
        assertEquals(1.5f, first.value(), 0.0f);

        IndexedFloat second = indexed.next();
        assertEquals(11, second.index());
        assertEquals(2.5f, second.value(), 0.0f);

        IndexedFloat third = indexed.next();
        assertEquals(12, third.index());
        assertEquals(3.5f, third.value(), 0.0f);

        assertFalse(indexed.hasNext());

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1.0f).indexed(-1));
    }

    @Test
    public void test_forEachRemaining_deprecated() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatList result = new FloatList();

        iter.forEachRemaining((Float value) -> result.add(value));

        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), 0.0f);
        assertEquals(2.0f, result.get(1), 0.0f);
        assertEquals(3.0f, result.get(2), 0.0f);
    }

    @Test
    public void test_foreachRemaining() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatList result = new FloatList();

        iter.foreachRemaining(value -> result.add(value));

        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), 0.0f);
        assertEquals(2.0f, result.get(1), 0.0f);
        assertEquals(3.0f, result.get(2), 0.0f);

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1.0f).foreachRemaining(null));
    }

    @Test
    public void test_foreachIndexed() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatList values = new FloatList();
        IntList indices = new IntList();

        iter.foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });

        assertEquals(3, values.size());
        assertEquals(1.0f, values.get(0), 0.0f);
        assertEquals(2.0f, values.get(1), 0.0f);
        assertEquals(3.0f, values.get(2), 0.0f);

        assertEquals(3, indices.size());
        assertEquals(0, indices.get(0));
        assertEquals(1, indices.get(1));
        assertEquals(2, indices.get(2));

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.of(1.0f).foreachIndexed(null));
    }

    @Test
    public void test_skip_limit_combination() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
        FloatIterator result = iter.skip(2).limit(3);

        float[] array = result.toArray();
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, array, 0.0f);
    }

    @Test
    public void test_filter_limit_combination() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
        FloatIterator result = iter.filter(x -> x > 2.0f).limit(2);

        float[] array = result.toArray();
        assertArrayEquals(new float[] { 3.0f, 4.0f }, array, 0.0f);
    }

    @Test
    public void test_empty_operations() {
        FloatIterator iter = FloatIterator.empty();

        assertFalse(iter.skip(5).hasNext());
        assertFalse(iter.limit(5).hasNext());
        assertFalse(iter.filter(x -> true).hasNext());

        FloatIterator iter2 = FloatIterator.empty();
        assertEquals(0, iter2.toArray().length);

        FloatIterator iter3 = FloatIterator.empty();
        assertEquals(0, iter3.toList().size());
    }
}
