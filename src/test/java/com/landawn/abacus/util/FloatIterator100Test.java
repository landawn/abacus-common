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
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatSupplier;
import com.landawn.abacus.util.stream.FloatStream;

@Tag("new-test")
public class FloatIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        FloatIterator iter = FloatIterator.empty();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testOf_EmptyArray() {
        FloatIterator iter = FloatIterator.of();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testOf_NullArray() {
        FloatIterator iter = FloatIterator.of((float[]) null);

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testOf_SingleElement() {
        float[] array = { 3.14f };
        FloatIterator iter = FloatIterator.of(array);

        assertTrue(iter.hasNext());
        assertEquals(3.14f, iter.nextFloat());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testOf_MultipleElements() {
        float[] array = { 1.0f, 2.5f, 3.7f };
        FloatIterator iter = FloatIterator.of(array);

        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat());
        assertTrue(iter.hasNext());
        assertEquals(2.5f, iter.nextFloat());
        assertTrue(iter.hasNext());
        assertEquals(3.7f, iter.nextFloat());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_SpecialValues() {
        float[] array = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.MIN_VALUE, Float.MAX_VALUE };
        FloatIterator iter = FloatIterator.of(array);

        assertTrue(Float.isNaN(iter.nextFloat()));
        assertEquals(Float.POSITIVE_INFINITY, iter.nextFloat());
        assertEquals(Float.NEGATIVE_INFINITY, iter.nextFloat());
        assertEquals(Float.MIN_VALUE, iter.nextFloat());
        assertEquals(Float.MAX_VALUE, iter.nextFloat());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatIterator iter = FloatIterator.of(array, 1, 4);

        assertTrue(iter.hasNext());
        assertEquals(2.0f, iter.nextFloat());
        assertEquals(3.0f, iter.nextFloat());
        assertEquals(4.0f, iter.nextFloat());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_EmptyRange() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        FloatIterator iter = FloatIterator.of(array, 1, 1);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_FullArray() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        FloatIterator iter = FloatIterator.of(array, 0, array.length);

        assertEquals(1.0f, iter.nextFloat());
        assertEquals(2.0f, iter.nextFloat());
        assertEquals(3.0f, iter.nextFloat());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_InvalidIndices() {
        float[] array = { 1.0f, 2.0f, 3.0f };

        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of(array, 2, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<FloatIterator> supplier = () -> {
            counter.incrementAndGet();
            return FloatIterator.of(1.0f, 2.0f, 3.0f);
        };

        FloatIterator iter = FloatIterator.defer(supplier);

        assertEquals(0, counter.get());
        assertTrue(iter.hasNext());
        assertEquals(1, counter.get());
        assertEquals(1.0f, iter.nextFloat());
        assertEquals(2.0f, iter.nextFloat());
        assertEquals(3.0f, iter.nextFloat());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.defer(null));
    }

    @Test
    public void testGenerate_Infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatSupplier supplier = () -> counter.getAndIncrement() * 0.5f;

        FloatIterator iter = FloatIterator.generate(supplier);

        assertTrue(iter.hasNext());
        assertEquals(0.0f, iter.nextFloat());
        assertTrue(iter.hasNext());
        assertEquals(0.5f, iter.nextFloat());
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_WithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        FloatSupplier supplier = () -> counter.getAndIncrement() * 1.5f;

        FloatIterator iter = FloatIterator.generate(hasNext, supplier);

        assertTrue(iter.hasNext());
        assertEquals(0.0f, iter.nextFloat());
        assertTrue(iter.hasNext());
        assertEquals(1.5f, iter.nextFloat());
        assertTrue(iter.hasNext());
        assertEquals(3.0f, iter.nextFloat());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testGenerate_NullArguments() {
        FloatSupplier supplier = () -> 0.0f;
        BooleanSupplier hasNext = () -> true;

        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate((FloatSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate(null, supplier));
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate(hasNext, null));
    }

    @Test
    public void testNext_Deprecated() {
        FloatIterator iter = FloatIterator.of(42.5f);

        Float value = iter.next();
        assertEquals(Float.valueOf(42.5f), value);
    }

    @Test
    public void testSkip() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatIterator skipped = iter.skip(2);

        assertTrue(skipped.hasNext());
        assertEquals(3.0f, skipped.nextFloat());
        assertEquals(4.0f, skipped.nextFloat());
        assertEquals(5.0f, skipped.nextFloat());
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_Zero() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatIterator skipped = iter.skip(0);

        assertSame(iter, skipped);
    }

    @Test
    public void testSkip_MoreThanAvailable() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatIterator skipped = iter.skip(5);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_Negative() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);

        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testLimit() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatIterator limited = iter.limit(3);

        assertTrue(limited.hasNext());
        assertEquals(1.0f, limited.nextFloat());
        assertEquals(2.0f, limited.nextFloat());
        assertEquals(3.0f, limited.nextFloat());
        assertFalse(limited.hasNext());
        assertThrows(NoSuchElementException.class, () -> limited.nextFloat());
    }

    @Test
    public void testLimit_Zero() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatIterator limited = iter.limit(0);

        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_MoreThanAvailable() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatIterator limited = iter.limit(5);

        assertEquals(1.0f, limited.nextFloat());
        assertEquals(2.0f, limited.nextFloat());
        assertEquals(3.0f, limited.nextFloat());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_Negative() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);

        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testFilter() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatPredicate greaterThan2_5 = x -> x > 2.5f;
        FloatIterator filtered = iter.filter(greaterThan2_5);

        assertTrue(filtered.hasNext());
        assertEquals(3.0f, filtered.nextFloat());
        assertTrue(filtered.hasNext());
        assertEquals(4.0f, filtered.nextFloat());
        assertTrue(filtered.hasNext());
        assertEquals(5.0f, filtered.nextFloat());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilter_NoneMatch() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatPredicate greaterThan10 = x -> x > 10.0f;
        FloatIterator filtered = iter.filter(greaterThan10);

        assertFalse(filtered.hasNext());
        assertThrows(NoSuchElementException.class, () -> filtered.nextFloat());
    }

    @Test
    public void testFilter_AllMatch() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatPredicate alwaysTrue = x -> true;
        FloatIterator filtered = iter.filter(alwaysTrue);

        assertEquals(1.0f, filtered.nextFloat());
        assertEquals(2.0f, filtered.nextFloat());
        assertEquals(3.0f, filtered.nextFloat());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilter_NullPredicate() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);

        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFirst() {
        FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
        OptionalFloat first = iter.first();

        assertTrue(first.isPresent());
        assertEquals(1.5f, first.getAsFloat());
    }

    @Test
    public void testFirst_Empty() {
        FloatIterator iter = FloatIterator.empty();
        OptionalFloat first = iter.first();

        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
        OptionalFloat last = iter.last();

        assertTrue(last.isPresent());
        assertEquals(3.5f, last.getAsFloat());
    }

    @Test
    public void testLast_Empty() {
        FloatIterator iter = FloatIterator.empty();
        OptionalFloat last = iter.last();

        assertFalse(last.isPresent());
    }

    @Test
    public void testLast_SingleElement() {
        FloatIterator iter = FloatIterator.of(42.42f);
        OptionalFloat last = iter.last();

        assertTrue(last.isPresent());
        assertEquals(42.42f, last.getAsFloat());
    }

    @Test
    public void testToArray() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        float[] array = iter.toArray();

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, array);
    }

    @Test
    public void testToArray_Empty() {
        FloatIterator iter = FloatIterator.empty();
        float[] array = iter.toArray();

        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_PartiallyConsumed() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        iter.nextFloat();
        iter.nextFloat();

        float[] array = iter.toArray();
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, array);
    }

    @Test
    public void testToList() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatList list = iter.toList();

        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0));
        assertEquals(2.0f, list.get(1));
        assertEquals(3.0f, list.get(2));
    }

    @Test
    public void testToList_Empty() {
        FloatIterator iter = FloatIterator.empty();
        FloatList list = iter.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    public void testStream() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatStream stream = iter.stream();

        assertNotNull(stream);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testIndexed() {
        FloatIterator iter = FloatIterator.of(10.5f, 20.5f, 30.5f);
        ObjIterator<IndexedFloat> indexed = iter.indexed();

        assertTrue(indexed.hasNext());
        IndexedFloat first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(10.5f, first.value());

        IndexedFloat second = indexed.next();
        assertEquals(1, second.index());
        assertEquals(20.5f, second.value());

        IndexedFloat third = indexed.next();
        assertEquals(2, third.index());
        assertEquals(30.5f, third.value());

        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_WithStartIndex() {
        FloatIterator iter = FloatIterator.of(10.5f, 20.5f, 30.5f);
        ObjIterator<IndexedFloat> indexed = iter.indexed(100);

        IndexedFloat first = indexed.next();
        assertEquals(100, first.index());
        assertEquals(10.5f, first.value());

        IndexedFloat second = indexed.next();
        assertEquals(101, second.index());
        assertEquals(20.5f, second.value());

        IndexedFloat third = indexed.next();
        assertEquals(102, third.index());
        assertEquals(30.5f, third.value());
    }

    @Test
    public void testIndexed_NegativeStartIndex() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);

        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testForEachRemaining_Deprecated() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        StringBuilder sb = new StringBuilder();

        iter.forEachRemaining((Float f) -> sb.append(f).append(","));

        assertEquals("1.0,2.0,3.0,", sb.toString());
    }

    @Test
    public void testForeachRemaining() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        StringBuilder sb = new StringBuilder();

        iter.foreachRemaining(f -> sb.append(f).append(","));

        assertEquals("1.0,2.0,3.0,", sb.toString());
    }

    @Test
    public void testForeachRemaining_PartiallyConsumed() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        iter.nextFloat();
        iter.nextFloat();

        StringBuilder sb = new StringBuilder();
        iter.foreachRemaining(f -> sb.append(f).append(","));

        assertEquals("3.0,4.0,5.0,", sb.toString());
    }

    @Test
    public void testForeachRemaining_Empty() {
        FloatIterator iter = FloatIterator.empty();
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachRemaining(f -> count.incrementAndGet());

        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed() {
        FloatIterator iter = FloatIterator.of(10.0f, 20.0f, 30.0f);
        StringBuilder sb = new StringBuilder();

        iter.foreachIndexed((index, value) -> {
            sb.append(index).append(":").append(value).append(",");
        });

        assertEquals("0:10.0,1:20.0,2:30.0,", sb.toString());
    }

    @Test
    public void testForeachIndexed_Empty() {
        FloatIterator iter = FloatIterator.empty();
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachIndexed((index, value) -> count.incrementAndGet());

        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed_NullAction() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);

        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }
}
