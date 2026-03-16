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

@Tag("2025")
public class FloatIteratorTest extends TestBase {

    // =================================================
    // empty()
    // =================================================

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
    public void testEmpty() {
        FloatIterator iter = FloatIterator.empty();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testEmpty_SameSingleton() {
        assertSame(FloatIterator.EMPTY, FloatIterator.empty());
    }

    // =================================================
    // of(float...)
    // =================================================

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

    // =================================================
    // of(float[], int, int)
    // =================================================

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
    public void testOf_WithRange_NullArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> FloatIterator.of((float[]) null, 0, 1));
    }

    // =================================================
    // defer()
    // =================================================

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
    public void testDefer_CalledOnNextFloat() {
        boolean[] supplierCalled = { false };
        Supplier<FloatIterator> supplier = () -> {
            supplierCalled[0] = true;
            return FloatIterator.of(42.0f);
        };

        FloatIterator iter = FloatIterator.defer(supplier);
        assertFalse(supplierCalled[0]);

        assertEquals(42.0f, iter.nextFloat());
        assertTrue(supplierCalled[0]);
    }

    @Test
    public void testDefer_SupplierCalledOnce() {
        int[] callCount = { 0 };
        Supplier<FloatIterator> supplier = () -> {
            callCount[0]++;
            return FloatIterator.of(1.0f, 2.0f);
        };

        FloatIterator iter = FloatIterator.defer(supplier);
        iter.hasNext();
        iter.nextFloat();
        iter.hasNext();
        iter.nextFloat();

        assertEquals(1, callCount[0], "Supplier should only be called once");
    }

    // =================================================
    // generate(FloatSupplier)
    // =================================================

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
    public void testGenerate_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> FloatIterator.generate((FloatSupplier) null));
    }

    // =================================================
    // generate(BooleanSupplier, FloatSupplier)
    // =================================================

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
    public void testGenerate_WithCondition_NoElements() {
        FloatIterator iter = FloatIterator.generate(() -> false, () -> 1.0f);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    // =================================================
    // next() [deprecated]
    // =================================================

    @Test
    public void test_next_deprecated() {
        FloatIterator iter = FloatIterator.of(1.5f, 2.5f);

        Float boxed = iter.next();
        assertEquals(1.5f, boxed, 0.0f);
    }

    @Test
    public void testNext_Deprecated() {
        FloatIterator iter = FloatIterator.of(42.5f);

        Float value = iter.next();
        assertEquals(Float.valueOf(42.5f), value);
    }

    // =================================================
    // nextFloat()
    // =================================================

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

    // =================================================
    // skip(long)
    // =================================================

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
    public void testSkip_NoSuchElementAfterExhaustion() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f).skip(2);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    // =================================================
    // limit(long)
    // =================================================

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
    public void testLimit_NoSuchElementAfterExhaustion() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f).limit(1);
        assertEquals(1.0f, iter.nextFloat());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    // =================================================
    // filter(FloatPredicate)
    // =================================================

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
    public void testFilter_NoSuchElementWhenNoneMatch() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f).filter(x -> x > 10.0f);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    // =================================================
    // toArray()
    // =================================================

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

    // =================================================
    // toList()
    // =================================================

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

    // =================================================
    // stream()
    // =================================================

    @Test
    public void test_stream() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f);

        double sum = iter.stream().filter(x -> x > 2.0f).sum();

        assertEquals(7.0, sum, 0.001);
    }

    @Test
    public void testStream() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatStream stream = iter.stream();

        assertNotNull(stream);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testStream_Empty() {
        FloatIterator iter = FloatIterator.empty();
        FloatStream stream = iter.stream();

        assertNotNull(stream);
        assertEquals(0, stream.toArray().length);
    }

    // =================================================
    // indexed()
    // =================================================

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
    public void testIndexed_Empty() {
        ObjIterator<IndexedFloat> indexed = FloatIterator.empty().indexed();
        assertFalse(indexed.hasNext());
    }

    // =================================================
    // indexed(long)
    // =================================================

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
    public void testIndexed_WithStartIndex_Zero() {
        FloatIterator iter = FloatIterator.of(10.5f, 20.5f);
        ObjIterator<IndexedFloat> indexed = iter.indexed(0);

        IndexedFloat first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(10.5f, first.value());
    }

    // =================================================
    // forEachRemaining() [deprecated]
    // =================================================

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
    public void testForEachRemaining_Deprecated() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
        StringBuilder sb = new StringBuilder();

        iter.forEachRemaining((Float f) -> sb.append(f).append(","));

        assertEquals("1.0,2.0,3.0,", sb.toString());
    }

    @Test
    public void testForEachRemaining_Empty() {
        FloatIterator iter = FloatIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((Float f) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    // =================================================
    // foreachRemaining()
    // =================================================

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
    public void testForeachRemaining_Null() {
        FloatIterator iter = FloatIterator.of(1.0f);
        assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
    }

    // =================================================
    // foreachIndexed()
    // =================================================

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

    @Test
    public void testForeachIndexed_PartiallyConsumed() {
        FloatIterator iter = FloatIterator.of(10.0f, 20.0f, 30.0f);
        iter.nextFloat();
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

    @Test
    public void testMultipleHasNextCalls() {
        FloatIterator iter = FloatIterator.of(1.0f, 2.0f);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(2.0f, iter.nextFloat());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

}
