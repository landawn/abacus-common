package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

@Tag("2025")
public class DoubleIterator2025Test extends TestBase {

    @Test
    public void test_empty() {
        DoubleIterator iter = DoubleIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_empty_nextDouble_throwsException() {
        DoubleIterator iter = DoubleIterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void test_of_varargs() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        assertTrue(iter.hasNext());
        assertEquals(1.5, iter.nextDouble(), 0.0001);
        assertEquals(2.5, iter.nextDouble(), 0.0001);
        assertEquals(3.5, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_varargs_empty() {
        DoubleIterator iter = DoubleIterator.of();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_varargs_null() {
        DoubleIterator iter = DoubleIterator.of((double[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_varargs_singleElement() {
        DoubleIterator iter = DoubleIterator.of(42.5);
        assertTrue(iter.hasNext());
        assertEquals(42.5, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleIterator iter = DoubleIterator.of(arr, 1, 4);
        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertEquals(4.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_fullArray() {
        double[] arr = { 1.0, 2.0, 3.0 };
        DoubleIterator iter = DoubleIterator.of(arr, 0, 3);
        assertEquals(1.0, iter.nextDouble(), 0.0001);
        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_emptyRange() {
        double[] arr = { 1.0, 2.0, 3.0 };
        DoubleIterator iter = DoubleIterator.of(arr, 1, 1);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_nullArray() {
        DoubleIterator iter = DoubleIterator.of(null, 0, 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_invalidBounds() {
        double[] arr = { 1.0, 2.0, 3.0 };
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, 2, 1));
    }

    @Test
    public void test_defer() {
        AtomicBoolean initialized = new AtomicBoolean(false);
        DoubleIterator iter = DoubleIterator.defer(() -> {
            initialized.set(true);
            return DoubleIterator.of(1.5, 2.5, 3.5);
        });

        assertFalse(initialized.get());
        assertTrue(iter.hasNext());
        assertTrue(initialized.get());
        assertEquals(1.5, iter.nextDouble(), 0.0001);
    }

    @Test
    public void test_defer_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.defer(null));
    }

    @Test
    public void test_defer_supplierReturnsNull() {
        DoubleIterator iter = DoubleIterator.defer(() -> null);
        assertThrows(IllegalStateException.class, () -> iter.hasNext());
    }

    @Test
    public void test_defer_lazyInitialization() {
        AtomicInteger callCount = new AtomicInteger(0);
        DoubleIterator iter = DoubleIterator.defer(() -> {
            callCount.incrementAndGet();
            return DoubleIterator.of(1.5, 2.5);
        });

        iter.hasNext();
        iter.nextDouble();
        iter.hasNext();
        iter.nextDouble();

        assertEquals(1, callCount.get());
    }

    @Test
    public void test_generate_infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleIterator iter = DoubleIterator.generate(() -> counter.incrementAndGet() * 1.5);

        assertTrue(iter.hasNext());
        assertEquals(1.5, iter.nextDouble(), 0.0001);
        assertTrue(iter.hasNext());
        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertTrue(iter.hasNext());
    }

    @Test
    public void test_generate_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(null));
    }

    @Test
    public void test_generate_withCondition() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleIterator iter = DoubleIterator.generate(() -> counter.get() < 3, () -> counter.incrementAndGet() * 2.5);

        assertTrue(iter.hasNext());
        assertEquals(2.5, iter.nextDouble(), 0.0001);
        assertTrue(iter.hasNext());
        assertEquals(5.0, iter.nextDouble(), 0.0001);
        assertTrue(iter.hasNext());
        assertEquals(7.5, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_generate_withCondition_noElements() {
        DoubleIterator iter = DoubleIterator.generate(() -> false, () -> 1.5);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void test_generate_withCondition_nullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(null, () -> 1.5));
    }

    @Test
    public void test_generate_withCondition_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(() -> true, null));
    }

    @Test
    public void test_generate_withCondition_cachedState() {
        AtomicInteger hasNextCalls = new AtomicInteger(0);
        AtomicInteger count = new AtomicInteger(0);

        DoubleIterator iter = DoubleIterator.generate(() -> {
            hasNextCalls.incrementAndGet();
            return count.get() < 2;
        }, () -> {
            count.incrementAndGet();
            return count.get() * 1.5;
        });

        assertTrue(iter.hasNext());
        assertEquals(1, hasNextCalls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, hasNextCalls.get());

        iter.nextDouble();
        assertTrue(iter.hasNext());
        assertEquals(2, hasNextCalls.get());
    }

    @Test
    public void test_next_deprecated() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);
        assertEquals(1.5, iter.next(), 0.0001);
        assertEquals(2.5, iter.next(), 0.0001);
    }

    @Test
    public void test_nextDouble() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        assertEquals(1.5, iter.nextDouble(), 0.0001);
        assertEquals(2.5, iter.nextDouble(), 0.0001);
        assertEquals(3.5, iter.nextDouble(), 0.0001);
    }

    @Test
    public void test_nextDouble_noMoreElements() {
        DoubleIterator iter = DoubleIterator.of(1.5);
        iter.nextDouble();
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void test_skip() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).skip(2);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertEquals(4.0, iter.nextDouble(), 0.0001);
        assertEquals(5.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_skip_zero() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0).skip(0);
        assertEquals(1.0, iter.nextDouble(), 0.0001);
    }

    @Test
    public void test_skip_all() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0).skip(5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_skip_negative() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).skip(-1));
    }

    @Test
    public void test_limit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).limit(3);
        assertEquals(1.0, iter.nextDouble(), 0.0001);
        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_limit_zero() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0).limit(0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_limit_moreThanAvailable() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0).limit(10);
        assertEquals(1.0, iter.nextDouble(), 0.0001);
        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_limit_negative() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).limit(-1));
    }

    @Test
    public void test_limit_nextDouble_afterLimit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0).limit(2);
        iter.nextDouble();
        iter.nextDouble();
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void test_filter() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5, 4.5, 5.5).filter(d -> d > 3.0);
        assertEquals(3.5, iter.nextDouble(), 0.0001);
        assertEquals(4.5, iter.nextDouble(), 0.0001);
        assertEquals(5.5, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_filter_noMatch() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0).filter(d -> d > 10.0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_filter_allMatch() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0).filter(d -> d > 0.0);
        assertEquals(1.0, iter.nextDouble(), 0.0001);
        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
    }

    @Test
    public void test_filter_nullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).filter(null));
    }

    @Test
    public void test_filter_nextDouble_afterNoMatch() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0).filter(d -> d > 10.0);
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void test_first() {
        OptionalDouble first = DoubleIterator.of(1.5, 2.5, 3.5).first();
        assertTrue(first.isPresent());
        assertEquals(1.5, first.get(), 0.0001);
    }

    @Test
    public void test_first_empty() {
        OptionalDouble first = DoubleIterator.empty().first();
        assertFalse(first.isPresent());
    }

    @Test
    public void test_first_consumesOnlyOne() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        iter.first();
        assertTrue(iter.hasNext());
        assertEquals(2.5, iter.nextDouble(), 0.0001);
    }

    @Test
    public void test_last() {
        OptionalDouble last = DoubleIterator.of(1.5, 2.5, 3.5).last();
        assertTrue(last.isPresent());
        assertEquals(3.5, last.get(), 0.0001);
    }

    @Test
    public void test_last_empty() {
        OptionalDouble last = DoubleIterator.empty().last();
        assertFalse(last.isPresent());
    }

    @Test
    public void test_last_singleElement() {
        OptionalDouble last = DoubleIterator.of(42.5).last();
        assertTrue(last.isPresent());
        assertEquals(42.5, last.get(), 0.0001);
    }

    @Test
    public void test_last_consumesAll() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        iter.last();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_toArray() {
        double[] arr = DoubleIterator.of(1.5, 2.5, 3.5).toArray();
        assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, arr, 0.0001);
    }

    @Test
    public void test_toArray_empty() {
        double[] arr = DoubleIterator.empty().toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void test_toArray_consumesIterator() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);
        iter.toArray();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_toList() {
        DoubleList list = DoubleIterator.of(1.5, 2.5, 3.5).toList();
        assertEquals(3, list.size());
        assertEquals(1.5, list.get(0), 0.0001);
        assertEquals(2.5, list.get(1), 0.0001);
        assertEquals(3.5, list.get(2), 0.0001);
    }

    @Test
    public void test_toList_empty() {
        DoubleList list = DoubleIterator.empty().toList();
        assertEquals(0, list.size());
    }

    @Test
    public void test_toList_consumesIterator() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);
        iter.toList();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_stream() {
        DoubleStream stream = DoubleIterator.of(1.5, 2.5, 3.5).stream();
        assertNotNull(stream);
        assertEquals(7.5, stream.sum(), 0.0001);
    }

    @Test
    public void test_stream_empty() {
        DoubleStream stream = DoubleIterator.empty().stream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void test_indexed() {
        ObjIterator<IndexedDouble> indexed = DoubleIterator.of(10.5, 20.5, 30.5).indexed();
        assertTrue(indexed.hasNext());

        IndexedDouble first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(10.5, first.value(), 0.0001);

        IndexedDouble second = indexed.next();
        assertEquals(1, second.index());
        assertEquals(20.5, second.value(), 0.0001);

        IndexedDouble third = indexed.next();
        assertEquals(2, third.index());
        assertEquals(30.5, third.value(), 0.0001);

        assertFalse(indexed.hasNext());
    }

    @Test
    public void test_indexed_withStartIndex() {
        ObjIterator<IndexedDouble> indexed = DoubleIterator.of(10.5, 20.5).indexed(100);

        IndexedDouble first = indexed.next();
        assertEquals(100, first.index());
        assertEquals(10.5, first.value(), 0.0001);

        IndexedDouble second = indexed.next();
        assertEquals(101, second.index());
        assertEquals(20.5, second.value(), 0.0001);
    }

    @Test
    public void test_indexed_negativeStartIndex() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).indexed(-1));
    }

    @Test
    public void test_indexed_empty() {
        ObjIterator<IndexedDouble> indexed = DoubleIterator.empty().indexed();
        assertFalse(indexed.hasNext());
    }

    @Test
    public void test_forEachRemaining_deprecated() {
        DoubleList result = new DoubleList();
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        iter.forEachRemaining((Double d) -> result.add(d));

        assertEquals(3, result.size());
        assertEquals(1.5, result.get(0), 0.0001);
    }

    @Test
    public void test_foreachRemaining() {
        DoubleList result = new DoubleList();
        DoubleIterator.of(1.5, 2.5, 3.5).foreachRemaining(result::add);

        assertEquals(3, result.size());
        assertEquals(1.5, result.get(0), 0.0001);
        assertEquals(2.5, result.get(1), 0.0001);
        assertEquals(3.5, result.get(2), 0.0001);
    }

    @Test
    public void test_foreachRemaining_empty() {
        DoubleList result = new DoubleList();
        DoubleIterator.empty().foreachRemaining(result::add);
        assertEquals(0, result.size());
    }

    @Test
    public void test_foreachRemaining_partiallyConsumed() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        iter.nextDouble();

        DoubleList result = new DoubleList();
        iter.foreachRemaining(result::add);

        assertEquals(2, result.size());
        assertEquals(2.5, result.get(0), 0.0001);
        assertEquals(3.5, result.get(1), 0.0001);
    }

    @Test
    public void test_foreachIndexed() {
        IntList indices = new IntList();
        DoubleList values = new DoubleList();

        DoubleIterator.of(10.5, 20.5, 30.5).foreachIndexed((idx, val) -> {
            indices.add(idx);
            values.add(val);
        });

        assertEquals(3, indices.size());
        assertEquals(0, indices.get(0));
        assertEquals(1, indices.get(1));
        assertEquals(2, indices.get(2));

        assertEquals(3, values.size());
        assertEquals(10.5, values.get(0), 0.0001);
        assertEquals(20.5, values.get(1), 0.0001);
        assertEquals(30.5, values.get(2), 0.0001);
    }

    @Test
    public void test_foreachIndexed_empty() {
        AtomicInteger count = new AtomicInteger(0);
        DoubleIterator.empty().foreachIndexed((idx, val) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void test_foreachIndexed_overflowProtection() {
        assertDoesNotThrow(() -> {
            DoubleIterator.of(1.5).foreachIndexed((idx, val) -> {
            });
        });
    }

    @Test
    public void test_skipAndLimit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).skip(2).limit(3);

        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertEquals(4.0, iter.nextDouble(), 0.0001);
        assertEquals(5.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_filterAndLimit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).filter(d -> d > 2.0).limit(2);

        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertEquals(4.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_skipFilterLimit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).skip(1).filter(d -> d % 2 == 0).limit(2);

        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertEquals(4.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_multipleHasNextCalls() {
        DoubleIterator iter = DoubleIterator.of(1.5);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(1.5, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_largeArray() {
        double[] large = new double[1000];
        for (int i = 0; i < 1000; i++) {
            large[i] = i * 1.5;
        }

        DoubleIterator iter = DoubleIterator.of(large);
        int count = 0;
        while (iter.hasNext()) {
            iter.nextDouble();
            count++;
        }
        assertEquals(1000, count);
    }

    @Test
    public void test_specialValues() {
        DoubleIterator iter = DoubleIterator.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE, Double.MIN_VALUE, 0.0, -0.0);

        assertTrue(Double.isNaN(iter.nextDouble()));
        assertEquals(Double.POSITIVE_INFINITY, iter.nextDouble(), 0.0001);
        assertEquals(Double.NEGATIVE_INFINITY, iter.nextDouble(), 0.0001);
        assertEquals(Double.MAX_VALUE, iter.nextDouble(), 0.0001);
        assertEquals(Double.MIN_VALUE, iter.nextDouble(), 0.0001);
        assertEquals(0.0, iter.nextDouble(), 0.0001);
        assertEquals(-0.0, iter.nextDouble(), 0.0001);
    }

    @Test
    public void test_filter_withNaN() {
        DoubleIterator iter = DoubleIterator.of(1.0, Double.NaN, 2.0, Double.NaN, 3.0).filter(d -> !Double.isNaN(d));

        assertEquals(1.0, iter.nextDouble(), 0.0001);
        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_toArray_fromRange() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] result = DoubleIterator.of(source, 1, 4).toArray();
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void test_toList_fromRange() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleList result = DoubleIterator.of(source, 1, 4).toList();
        assertEquals(3, result.size());
        assertEquals(2.0, result.get(0), 0.0001);
        assertEquals(3.0, result.get(1), 0.0001);
        assertEquals(4.0, result.get(2), 0.0001);
    }
}
