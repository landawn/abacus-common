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
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

@Tag("new-test")
public class DoubleIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        DoubleIterator iter = DoubleIterator.empty();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testOf_EmptyArray() {
        DoubleIterator iter = DoubleIterator.of();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testOf_NullArray() {
        DoubleIterator iter = DoubleIterator.of((double[]) null);

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testOf_SingleElement() {
        double[] array = { 3.14159 };
        DoubleIterator iter = DoubleIterator.of(array);

        assertTrue(iter.hasNext());
        assertEquals(3.14159, iter.nextDouble());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testOf_MultipleElements() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleIterator iter = DoubleIterator.of(array);

        assertTrue(iter.hasNext());
        assertEquals(1.1, iter.nextDouble());
        assertTrue(iter.hasNext());
        assertEquals(2.2, iter.nextDouble());
        assertTrue(iter.hasNext());
        assertEquals(3.3, iter.nextDouble());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_SpecialValues() {
        double[] array = { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, Double.MAX_VALUE };
        DoubleIterator iter = DoubleIterator.of(array);

        assertTrue(Double.isNaN(iter.nextDouble()));
        assertEquals(Double.POSITIVE_INFINITY, iter.nextDouble());
        assertEquals(Double.NEGATIVE_INFINITY, iter.nextDouble());
        assertEquals(Double.MIN_VALUE, iter.nextDouble());
        assertEquals(Double.MAX_VALUE, iter.nextDouble());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleIterator iter = DoubleIterator.of(array, 1, 4);

        assertTrue(iter.hasNext());
        assertEquals(2.0, iter.nextDouble());
        assertEquals(3.0, iter.nextDouble());
        assertEquals(4.0, iter.nextDouble());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_EmptyRange() {
        double[] array = { 1.0, 2.0, 3.0 };
        DoubleIterator iter = DoubleIterator.of(array, 1, 1);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_FullArray() {
        double[] array = { 1.0, 2.0, 3.0 };
        DoubleIterator iter = DoubleIterator.of(array, 0, array.length);

        assertEquals(1.0, iter.nextDouble());
        assertEquals(2.0, iter.nextDouble());
        assertEquals(3.0, iter.nextDouble());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_WithRange_InvalidIndices() {
        double[] array = { 1.0, 2.0, 3.0 };

        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, 2, 1));
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<DoubleIterator> supplier = () -> {
            counter.incrementAndGet();
            return DoubleIterator.of(1.5, 2.5, 3.5);
        };

        DoubleIterator iter = DoubleIterator.defer(supplier);

        assertEquals(0, counter.get());
        assertTrue(iter.hasNext());
        assertEquals(1, counter.get());
        assertEquals(1.5, iter.nextDouble());
        assertEquals(2.5, iter.nextDouble());
        assertEquals(3.5, iter.nextDouble());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.defer(null));
    }

    @Test
    public void testGenerate_Infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleSupplier supplier = () -> counter.getAndIncrement() * 0.5;

        DoubleIterator iter = DoubleIterator.generate(supplier);

        assertTrue(iter.hasNext());
        assertEquals(0.0, iter.nextDouble());
        assertTrue(iter.hasNext());
        assertEquals(0.5, iter.nextDouble());
        assertTrue(iter.hasNext());
        assertEquals(1.0, iter.nextDouble());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_WithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        DoubleSupplier supplier = () -> counter.getAndIncrement() * 1.5;

        DoubleIterator iter = DoubleIterator.generate(hasNext, supplier);

        assertTrue(iter.hasNext());
        assertEquals(0.0, iter.nextDouble());
        assertTrue(iter.hasNext());
        assertEquals(1.5, iter.nextDouble());
        assertTrue(iter.hasNext());
        assertEquals(3.0, iter.nextDouble());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testGenerate_NullArguments() {
        DoubleSupplier supplier = () -> 0.0;
        BooleanSupplier hasNext = () -> true;

        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate((DoubleSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(null, supplier));
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(hasNext, null));
    }

    @Test
    public void testNext_Deprecated() {
        DoubleIterator iter = DoubleIterator.of(42.5);

        Double value = iter.next();
        assertEquals(Double.valueOf(42.5), value);
    }

    @Test
    public void testSkip() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0);
        DoubleIterator skipped = iter.skip(2);

        assertTrue(skipped.hasNext());
        assertEquals(3.0, skipped.nextDouble());
        assertEquals(4.0, skipped.nextDouble());
        assertEquals(5.0, skipped.nextDouble());
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_Zero() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator skipped = iter.skip(0);

        assertSame(iter, skipped);
    }

    @Test
    public void testSkip_MoreThanAvailable() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator skipped = iter.skip(5);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_Negative() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testLimit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0);
        DoubleIterator limited = iter.limit(3);

        assertTrue(limited.hasNext());
        assertEquals(1.0, limited.nextDouble());
        assertEquals(2.0, limited.nextDouble());
        assertEquals(3.0, limited.nextDouble());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_Negative() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testFilter() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0);
        DoublePredicate greaterThan2_5 = x -> x > 2.5;
        DoubleIterator filtered = iter.filter(greaterThan2_5);

        assertTrue(filtered.hasNext());
        assertEquals(3.0, filtered.nextDouble());
        assertTrue(filtered.hasNext());
        assertEquals(4.0, filtered.nextDouble());
        assertTrue(filtered.hasNext());
        assertEquals(5.0, filtered.nextDouble());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilter_NoneMatch() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoublePredicate greaterThan10 = x -> x > 10.0;
        DoubleIterator filtered = iter.filter(greaterThan10);

        assertFalse(filtered.hasNext());
        assertThrows(NoSuchElementException.class, () -> filtered.nextDouble());
    }

    @Test
    public void testFilter_AllMatch() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoublePredicate alwaysTrue = x -> true;
        DoubleIterator filtered = iter.filter(alwaysTrue);

        assertEquals(1.0, filtered.nextDouble());
        assertEquals(2.0, filtered.nextDouble());
        assertEquals(3.0, filtered.nextDouble());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilter_NullPredicate() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFirst() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        OptionalDouble first = iter.first();

        assertTrue(first.isPresent());
        assertEquals(1.5, first.getAsDouble());
    }

    @Test
    public void testFirst_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        OptionalDouble first = iter.first();

        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        OptionalDouble last = iter.last();

        assertTrue(last.isPresent());
        assertEquals(3.5, last.getAsDouble());
    }

    @Test
    public void testLast_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        OptionalDouble last = iter.last();

        assertFalse(last.isPresent());
    }

    @Test
    public void testLast_SingleElement() {
        DoubleIterator iter = DoubleIterator.of(42.42);
        OptionalDouble last = iter.last();

        assertTrue(last.isPresent());
        assertEquals(42.42, last.getAsDouble());
    }

    @Test
    public void testToArray() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        double[] array = iter.toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, array);
    }

    @Test
    public void testToArray_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        double[] array = iter.toArray();

        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_PartiallyConsumed() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0);
        iter.nextDouble();
        iter.nextDouble();

        double[] array = iter.toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, array);
    }

    @Test
    public void testToList() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleList list = iter.toList();

        assertEquals(3, list.size());
        assertEquals(1.0, list.get(0));
        assertEquals(2.0, list.get(1));
        assertEquals(3.0, list.get(2));
    }

    @Test
    public void testToList_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        DoubleList list = iter.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    public void testStream() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleStream stream = iter.stream();

        assertNotNull(stream);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray());
    }

    @Test
    public void testIndexed() {
        DoubleIterator iter = DoubleIterator.of(10.5, 20.5, 30.5);
        ObjIterator<IndexedDouble> indexed = iter.indexed();

        assertTrue(indexed.hasNext());
        IndexedDouble first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(10.5, first.value());

        IndexedDouble second = indexed.next();
        assertEquals(1, second.index());
        assertEquals(20.5, second.value());

        IndexedDouble third = indexed.next();
        assertEquals(2, third.index());
        assertEquals(30.5, third.value());

        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_WithStartIndex() {
        DoubleIterator iter = DoubleIterator.of(10.5, 20.5, 30.5);
        ObjIterator<IndexedDouble> indexed = iter.indexed(100);

        IndexedDouble first = indexed.next();
        assertEquals(100, first.index());
        assertEquals(10.5, first.value());

        IndexedDouble second = indexed.next();
        assertEquals(101, second.index());
        assertEquals(20.5, second.value());

        IndexedDouble third = indexed.next();
        assertEquals(102, third.index());
        assertEquals(30.5, third.value());
    }

    @Test
    public void testIndexed_NegativeStartIndex() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testForEachRemaining_Deprecated() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        StringBuilder sb = new StringBuilder();

        iter.forEachRemaining((Double d) -> sb.append(d).append(","));

        assertEquals("1.0,2.0,3.0,", sb.toString());
    }

    @Test
    public void testForeachRemaining() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        StringBuilder sb = new StringBuilder();

        iter.foreachRemaining(d -> sb.append(d).append(","));

        assertEquals("1.0,2.0,3.0,", sb.toString());
    }

    @Test
    public void testForeachRemaining_PartiallyConsumed() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0);
        iter.nextDouble();
        iter.nextDouble();

        StringBuilder sb = new StringBuilder();
        iter.foreachRemaining(d -> sb.append(d).append(","));

        assertEquals("3.0,4.0,5.0,", sb.toString());
    }

    @Test
    public void testForeachRemaining_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachRemaining(d -> count.incrementAndGet());

        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed() {
        DoubleIterator iter = DoubleIterator.of(10.0, 20.0, 30.0);
        StringBuilder sb = new StringBuilder();

        iter.foreachIndexed((index, value) -> {
            sb.append(index).append(":").append(value).append(",");
        });

        assertEquals("0:10.0,1:20.0,2:30.0,", sb.toString());
    }

    @Test
    public void testForeachIndexed_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachIndexed((index, value) -> count.incrementAndGet());

        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed_NullAction() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator limited = iter.limit(0);

        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
        assertFalse(limited.hasNext());
        assertThrows(NoSuchElementException.class, () -> limited.nextDouble());
    }

    @Test
    public void testLimit_Zero() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator limited = iter.limit(0);

        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_MoreThanAvailable() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator limited = iter.limit(5);

        assertEquals(1.0, limited.nextDouble());
        assertEquals(2.0, limited.nextDouble());
        assertEquals(3.0, limited.nextDouble());
    }
}
