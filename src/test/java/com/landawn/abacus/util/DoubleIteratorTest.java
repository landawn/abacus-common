package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

public class DoubleIteratorTest extends TestBase {

    // =================================================
    // empty()
    // =================================================

    @Test
    public void test_empty() {
        DoubleIterator iter = DoubleIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty_SameSingleton() {
        assertSame(DoubleIterator.EMPTY, DoubleIterator.empty());
    }

    @Test
    public void test_empty_nextDouble_throwsException() {
        DoubleIterator iter = DoubleIterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testEmpty() {
        DoubleIterator iter = DoubleIterator.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testEmptyConstant() {
        DoubleIterator iter = DoubleIterator.EMPTY;
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    // =================================================
    // of(double...)
    // =================================================

    @Test
    public void test_of_varargs() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        assertTrue(iter.hasNext());
        assertEquals(1.5, iter.nextDouble(), 0.0001);
        assertEquals(2.5, iter.nextDouble(), 0.0001);
        assertEquals(3.5, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    // =================================================
    // of(double[], int, int)
    // =================================================

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
    public void testOf_WithRange_FullArray() {
        double[] array = { 1.0, 2.0, 3.0 };
        DoubleIterator iter = DoubleIterator.of(array, 0, array.length);

        assertEquals(1.0, iter.nextDouble());
        assertEquals(2.0, iter.nextDouble());
        assertEquals(3.0, iter.nextDouble());
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
    public void testCombinedOperations() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0 };
        DoubleIterator iter1 = DoubleIterator.of(arr).skip(2).limit(3);
        Assertions.assertEquals(3.0, iter1.nextDouble());
        Assertions.assertEquals(4.0, iter1.nextDouble());
        Assertions.assertEquals(5.0, iter1.nextDouble());
        Assertions.assertFalse(iter1.hasNext());

        DoubleIterator iter2 = DoubleIterator.of(arr).filter(d -> d % 2 == 0).skip(1).limit(1);
        Assertions.assertEquals(4.0, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());

        DoubleIterator iter3 = DoubleIterator.of(arr).limit(4).filter(d -> d > 2);
        Assertions.assertEquals(3.0, iter3.nextDouble());
        Assertions.assertEquals(4.0, iter3.nextDouble());
        Assertions.assertFalse(iter3.hasNext());
    }

    @Test
    public void testIteratorConsistency() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.hasNext());

        Assertions.assertEquals(1.5, iter.nextDouble());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(iter.hasNext());

        Assertions.assertEquals(2.5, iter.nextDouble());

        Assertions.assertFalse(iter.hasNext());
        Assertions.assertFalse(iter.hasNext());
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
    public void testOfVarargs() {
        DoubleIterator iter1 = DoubleIterator.of();
        Assertions.assertFalse(iter1.hasNext());

        DoubleIterator iter2 = DoubleIterator.of(1.5);
        Assertions.assertTrue(iter2.hasNext());
        Assertions.assertEquals(1.5, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());

        DoubleIterator iter3 = DoubleIterator.of(1.1, 2.2, 3.3);
        Assertions.assertTrue(iter3.hasNext());
        Assertions.assertEquals(1.1, iter3.nextDouble());
        Assertions.assertEquals(2.2, iter3.nextDouble());
        Assertions.assertEquals(3.3, iter3.nextDouble());
        Assertions.assertFalse(iter3.hasNext());

        DoubleIterator iter4 = DoubleIterator.of((double[]) null);
        Assertions.assertFalse(iter4.hasNext());
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
    public void testOf_WithRange_EmptyRange() {
        double[] array = { 1.0, 2.0, 3.0 };
        DoubleIterator iter = DoubleIterator.of(array, 1, 1);

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
    public void test_of_withRange_invalidBounds() {
        double[] arr = { 1.0, 2.0, 3.0 };
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, 2, 1));
    }

    @Test
    public void testOfArrayWithRange() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };

        DoubleIterator iter1 = DoubleIterator.of(arr, 0, arr.length);
        for (double element : arr) {
            Assertions.assertTrue(iter1.hasNext());
            Assertions.assertEquals(element, iter1.nextDouble());
        }
        Assertions.assertFalse(iter1.hasNext());

        DoubleIterator iter2 = DoubleIterator.of(arr, 1, 4);
        Assertions.assertEquals(2.0, iter2.nextDouble());
        Assertions.assertEquals(3.0, iter2.nextDouble());
        Assertions.assertEquals(4.0, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());

        DoubleIterator iter3 = DoubleIterator.of(arr, 2, 2);
        Assertions.assertFalse(iter3.hasNext());

        DoubleIterator iter4 = DoubleIterator.of(null, 0, 0);
        Assertions.assertFalse(iter4.hasNext());

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, 0, 6));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, 3, 2));
    }

    @Test
    public void testOf_WithRange_InvalidIndices() {
        double[] array = { 1.0, 2.0, 3.0 };

        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(array, 2, 1));
    }

    @Test
    public void testOf_WithRange_NullArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of((double[]) null, 0, 1));
    }

    // =================================================
    // defer()
    // =================================================

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
    public void testDefer_CalledOnNextDouble() {
        boolean[] supplierCalled = { false };
        Supplier<DoubleIterator> supplier = () -> {
            supplierCalled[0] = true;
            return DoubleIterator.of(42.0);
        };

        DoubleIterator iter = DoubleIterator.defer(supplier);
        assertFalse(supplierCalled[0]);

        assertEquals(42.0, iter.nextDouble());
        assertTrue(supplierCalled[0]);
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
    public void testDefer() {
        AtomicBoolean initialized = new AtomicBoolean(false);

        Supplier<DoubleIterator> supplier = () -> {
            initialized.set(true);
            return DoubleIterator.of(1.5, 2.5, 3.5);
        };

        DoubleIterator iter = DoubleIterator.defer(supplier);

        Assertions.assertFalse(initialized.get());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(initialized.get());

        Assertions.assertEquals(1.5, iter.nextDouble());
        Assertions.assertEquals(2.5, iter.nextDouble());
        Assertions.assertEquals(3.5, iter.nextDouble());
        Assertions.assertFalse(iter.hasNext());

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.defer(null));
    }

    // =================================================
    // generate(DoubleSupplier)
    // =================================================

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

    // =================================================
    // generate(BooleanSupplier, DoubleSupplier)
    // =================================================

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
    public void test_generate_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(null));
    }

    @Test
    public void testGenerateWithSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleSupplier supplier = () -> counter.incrementAndGet() * 1.5;

        DoubleIterator iter = DoubleIterator.generate(supplier);

        for (int i = 0; i < 100; i++) {
            Assertions.assertTrue(iter.hasNext());
        }

        Assertions.assertEquals(1.5, iter.nextDouble());
        Assertions.assertEquals(3.0, iter.nextDouble());
        Assertions.assertEquals(4.5, iter.nextDouble());

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate((DoubleSupplier) null));
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
    public void testGenerateWithHasNextAndSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        DoubleSupplier supplier = () -> {
            counter.incrementAndGet();
            return counter.get() * 2.5;
        };

        DoubleIterator iter = DoubleIterator.generate(hasNext, supplier);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(2.5, iter.nextDouble());
        Assertions.assertEquals(5.0, iter.nextDouble());
        Assertions.assertEquals(7.5, iter.nextDouble());
        Assertions.assertFalse(iter.hasNext());

        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextDouble());

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(null, supplier));
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(hasNext, null));
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
    public void testGenerate_WithCondition_NoElements() {
        DoubleIterator iter = DoubleIterator.generate(() -> false, () -> 1.0);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    // =================================================
    // next() [deprecated]
    // =================================================

    @Test
    public void test_next_deprecated() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);
        assertEquals(1.5, iter.next(), 0.0001);
        assertEquals(2.5, iter.next(), 0.0001);
    }

    @Test
    public void testNext_Deprecated() {
        DoubleIterator iter = DoubleIterator.of(42.5);

        Double value = iter.next();
        assertEquals(Double.valueOf(42.5), value);
    }

    @Test
    public void testNext() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);

        Double val1 = iter.next();
        Assertions.assertEquals(1.5, val1);

        Double val2 = iter.next();
        Assertions.assertEquals(2.5, val2);

        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    // =================================================
    // nextDouble()
    // =================================================

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

    // =================================================
    // skip(long)
    // =================================================

    @Test
    public void test_skip() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).skip(2);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertEquals(4.0, iter.nextDouble(), 0.0001);
        assertEquals(5.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_skip_all() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0).skip(5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_MoreThanAvailable() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator skipped = iter.skip(5);

        assertFalse(skipped.hasNext());
    }

    // =================================================
    // Integration / combined tests
    // =================================================

    @Test
    public void test_skipAndLimit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).skip(2).limit(3);

        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertEquals(4.0, iter.nextDouble(), 0.0001);
        assertEquals(5.0, iter.nextDouble(), 0.0001);
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
    public void test_skip_zero() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0).skip(0);
        assertEquals(1.0, iter.nextDouble(), 0.0001);
    }

    @Test
    public void testSkip_Zero() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator skipped = iter.skip(0);

        assertSame(iter, skipped);
    }

    @Test
    public void test_skip_negative() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).skip(-1));
    }

    @Test
    public void testSkip() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };

        DoubleIterator iter1 = DoubleIterator.of(arr).skip(0);
        Assertions.assertEquals(1.0, iter1.nextDouble());

        DoubleIterator iter2 = DoubleIterator.of(arr).skip(2);
        Assertions.assertEquals(3.0, iter2.nextDouble());
        Assertions.assertEquals(4.0, iter2.nextDouble());
        Assertions.assertEquals(5.0, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());

        DoubleIterator iter3 = DoubleIterator.of(arr).skip(5);
        Assertions.assertFalse(iter3.hasNext());

        DoubleIterator iter4 = DoubleIterator.of(arr).skip(10);
        Assertions.assertFalse(iter4.hasNext());

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(arr).skip(-1));

        DoubleIterator iter5 = DoubleIterator.of(arr).skip(5);
        Assertions.assertThrows(NoSuchElementException.class, () -> iter5.nextDouble());
    }

    @Test
    public void testSkip_Negative() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testSkip_NoSuchElementAfterExhaustion() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0).skip(2);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    // =================================================
    // limit(long)
    // =================================================

    @Test
    public void test_limit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).limit(3);
        assertEquals(1.0, iter.nextDouble(), 0.0001);
        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
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
    public void testLimit_MoreThanAvailable() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator limited = iter.limit(5);

        assertEquals(1.0, limited.nextDouble());
        assertEquals(2.0, limited.nextDouble());
        assertEquals(3.0, limited.nextDouble());
    }

    @Test
    public void test_limit_zero() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0).limit(0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_Zero() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator limited = iter.limit(0);

        assertFalse(limited.hasNext());
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
    public void testLimit() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };

        DoubleIterator iter1 = DoubleIterator.of(arr).limit(0);
        Assertions.assertFalse(iter1.hasNext());

        DoubleIterator iter2 = DoubleIterator.of(arr).limit(3);
        Assertions.assertEquals(1.0, iter2.nextDouble());
        Assertions.assertEquals(2.0, iter2.nextDouble());
        Assertions.assertEquals(3.0, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());

        DoubleIterator iter3 = DoubleIterator.of(arr).limit(5);
        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(iter3.hasNext());
            iter3.nextDouble();
        }
        Assertions.assertFalse(iter3.hasNext());

        DoubleIterator iter4 = DoubleIterator.of(arr).limit(10);
        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(iter4.hasNext());
            iter4.nextDouble();
        }
        Assertions.assertFalse(iter4.hasNext());

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(arr).limit(-1));

        DoubleIterator iter5 = DoubleIterator.of(arr).limit(2);
        iter5.nextDouble();
        iter5.nextDouble();
        Assertions.assertThrows(NoSuchElementException.class, () -> iter5.nextDouble());
    }

    @Test
    public void testLimit_Negative() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testLimit_NoSuchElementAfterExhaustion() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0).limit(1);
        assertEquals(1.0, iter.nextDouble());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    // =================================================
    // filter(DoublePredicate)
    // =================================================

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
    public void test_filter_withNaN() {
        DoubleIterator iter = DoubleIterator.of(1.0, Double.NaN, 2.0, Double.NaN, 3.0).filter(d -> !Double.isNaN(d));

        assertEquals(1.0, iter.nextDouble(), 0.0001);
        assertEquals(2.0, iter.nextDouble(), 0.0001);
        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
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
    public void test_filterAndLimit() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).filter(d -> d > 2.0).limit(2);

        assertEquals(3.0, iter.nextDouble(), 0.0001);
        assertEquals(4.0, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
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
    public void testFilter() {
        double[] arr = { 1.5, 2.5, 3.5, 4.5, 5.5 };

        DoublePredicate predicate1 = d -> d > 3.0;
        DoubleIterator iter1 = DoubleIterator.of(arr).filter(predicate1);
        Assertions.assertEquals(3.5, iter1.nextDouble());
        Assertions.assertEquals(4.5, iter1.nextDouble());
        Assertions.assertEquals(5.5, iter1.nextDouble());
        Assertions.assertFalse(iter1.hasNext());

        DoublePredicate predicate2 = d -> d > 10.0;
        DoubleIterator iter2 = DoubleIterator.of(arr).filter(predicate2);
        Assertions.assertFalse(iter2.hasNext());

        DoublePredicate predicate3 = d -> d > 0.0;
        DoubleIterator iter3 = DoubleIterator.of(arr).filter(predicate3);
        for (double v : arr) {
            Assertions.assertTrue(iter3.hasNext());
            Assertions.assertEquals(v, iter3.nextDouble());
        }

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(arr).filter(null));

        DoubleIterator iter4 = DoubleIterator.of(arr).filter(d -> d > 10.0);
        Assertions.assertThrows(NoSuchElementException.class, () -> iter4.nextDouble());
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
    public void testFilter_NullPredicate() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFilter_NoSuchElementWhenNoneMatch() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0).filter(x -> x > 10.0);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void test_toArray_fromRange() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] result = DoubleIterator.of(source, 1, 4).toArray();
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, result, 0.0001);
    }

    // =================================================
    // toArray()
    // =================================================

    @Test
    public void test_toArray() {
        double[] arr = DoubleIterator.of(1.5, 2.5, 3.5).toArray();
        assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, arr, 0.0001);
    }

    @Test
    public void test_toArray_consumesIterator() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);
        iter.toArray();
        assertFalse(iter.hasNext());
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
    public void test_toArray_empty() {
        double[] arr = DoubleIterator.empty().toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void testToArray() {
        DoubleIterator iter1 = DoubleIterator.of(1.1, 2.2, 3.3);
        double[] arr1 = iter1.toArray();
        Assertions.assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, arr1);

        DoubleIterator iter2 = DoubleIterator.empty();
        double[] arr2 = iter2.toArray();
        Assertions.assertEquals(0, arr2.length);

        DoubleIterator iter3 = DoubleIterator.of(1.0, 2.0, 3.0, 4.0);
        iter3.nextDouble();
        double[] arr3 = iter3.toArray();
        Assertions.assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, arr3);

        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleIterator iter4 = DoubleIterator.of(source, 1, 4);
        double[] arr4 = iter4.toArray();
        Assertions.assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, arr4);
    }

    @Test
    public void testToArray_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        double[] array = iter.toArray();

        assertEquals(0, array.length);
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

    // =================================================
    // toList()
    // =================================================

    @Test
    public void test_toList() {
        DoubleList list = DoubleIterator.of(1.5, 2.5, 3.5).toList();
        assertEquals(3, list.size());
        assertEquals(1.5, list.get(0), 0.0001);
        assertEquals(2.5, list.get(1), 0.0001);
        assertEquals(3.5, list.get(2), 0.0001);
    }

    @Test
    public void test_toList_consumesIterator() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);
        iter.toList();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_toList_empty() {
        DoubleList list = DoubleIterator.empty().toList();
        assertEquals(0, list.size());
    }

    @Test
    public void testToList() {
        DoubleIterator iter1 = DoubleIterator.of(1.1, 2.2, 3.3);
        DoubleList list1 = iter1.toList();
        Assertions.assertEquals(3, list1.size());
        Assertions.assertEquals(1.1, list1.get(0));
        Assertions.assertEquals(2.2, list1.get(1));
        Assertions.assertEquals(3.3, list1.get(2));

        DoubleIterator iter2 = DoubleIterator.empty();
        DoubleList list2 = iter2.toList();
        Assertions.assertEquals(0, list2.size());

        DoubleIterator iter3 = DoubleIterator.of(1.0, 2.0, 3.0, 4.0);
        iter3.nextDouble();
        DoubleList list3 = iter3.toList();
        Assertions.assertEquals(3, list3.size());
        Assertions.assertEquals(2.0, list3.get(0));
        Assertions.assertEquals(3.0, list3.get(1));
        Assertions.assertEquals(4.0, list3.get(2));
    }

    @Test
    public void testToList_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        DoubleList list = iter.toList();

        assertTrue(list.isEmpty());
    }

    // =================================================
    // stream()
    // =================================================

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
    public void testStream() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5, 4.5);
        DoubleStream stream = iter.stream();
        Assertions.assertNotNull(stream);

        double sum = stream.sum();
        Assertions.assertEquals(12.0, sum);
    }

    @Test
    public void testStream_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        DoubleStream stream = iter.stream();

        assertNotNull(stream);
        assertEquals(0, stream.toArray().length);
    }

    // =================================================
    // indexed()
    // =================================================

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
    public void testIndexed() {
        DoubleIterator iter1 = DoubleIterator.of(10.5, 20.5, 30.5);
        ObjIterator<IndexedDouble> indexed1 = iter1.indexed();

        IndexedDouble id1 = indexed1.next();
        Assertions.assertEquals(0, id1.index());
        Assertions.assertEquals(10.5, id1.value());

        IndexedDouble id2 = indexed1.next();
        Assertions.assertEquals(1, id2.index());
        Assertions.assertEquals(20.5, id2.value());

        IndexedDouble id3 = indexed1.next();
        Assertions.assertEquals(2, id3.index());
        Assertions.assertEquals(30.5, id3.value());

        Assertions.assertFalse(indexed1.hasNext());
    }

    // =================================================
    // indexed(long)
    // =================================================

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
    public void test_indexed_empty() {
        ObjIterator<IndexedDouble> indexed = DoubleIterator.empty().indexed();
        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_WithStartIndex_Zero() {
        DoubleIterator iter = DoubleIterator.of(10.5, 20.5);
        ObjIterator<IndexedDouble> indexed = iter.indexed(0);

        IndexedDouble first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(10.5, first.value());
    }

    @Test
    public void test_indexed_negativeStartIndex() {
        assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).indexed(-1));
    }

    @Test
    public void testIndexedWithStartIndex() {
        DoubleIterator iter = DoubleIterator.of(10.5, 20.5, 30.5);
        ObjIterator<IndexedDouble> indexed = iter.indexed(100);

        IndexedDouble id1 = indexed.next();
        Assertions.assertEquals(100, id1.index());
        Assertions.assertEquals(10.5, id1.value());

        IndexedDouble id2 = indexed.next();
        Assertions.assertEquals(101, id2.index());
        Assertions.assertEquals(20.5, id2.value());

        IndexedDouble id3 = indexed.next();
        Assertions.assertEquals(102, id3.index());
        Assertions.assertEquals(30.5, id3.value());

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).indexed(-1));

        ObjIterator<IndexedDouble> emptyIndexed = DoubleIterator.empty().indexed(5);
        Assertions.assertFalse(emptyIndexed.hasNext());
    }

    @Test
    public void testIndexed_NegativeStartIndex() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);

        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    // =================================================
    // forEachRemaining() [deprecated]
    // =================================================

    @Test
    public void test_forEachRemaining_deprecated() {
        DoubleList result = new DoubleList();
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        iter.forEachRemaining((Double d) -> result.add(d));

        assertEquals(3, result.size());
        assertEquals(1.5, result.get(0), 0.0001);
    }

    @Test
    public void testForEachRemaining_Deprecated() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        StringBuilder sb = new StringBuilder();

        iter.forEachRemaining((Double d) -> sb.append(d).append(","));

        assertEquals("1.0,2.0,3.0,", sb.toString());
    }

    // =================================================
    // foreachRemaining()
    // =================================================

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
    public void testForeachRemaining_PartiallyConsumed() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0);
        iter.nextDouble();
        iter.nextDouble();

        StringBuilder sb = new StringBuilder();
        iter.foreachRemaining(d -> sb.append(d).append(","));

        assertEquals("3.0,4.0,5.0,", sb.toString());
    }

    @Test
    public void testForEachRemaining_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((Double d) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void test_foreachRemaining_empty() {
        DoubleList result = new DoubleList();
        DoubleIterator.empty().foreachRemaining(result::add);
        assertEquals(0, result.size());
    }

    @Test
    public void testForeachRemaining_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachRemaining(d -> count.incrementAndGet());

        assertEquals(0, count.get());
    }

    @Test
    public void testForEachRemaining() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        AtomicInteger count = new AtomicInteger(0);
        double[] values = new double[3];

        iter.forEachRemaining((Double d) -> {
            values[count.getAndIncrement()] = d;
        });

        Assertions.assertEquals(3, count.get());
        Assertions.assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, values);

        Assertions.assertThrows(NullPointerException.class, () -> DoubleIterator.of(1.0).forEachRemaining((java.util.function.Consumer<? super Double>) null));
    }

    @Test
    public void testForeachRemaining() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        AtomicInteger count = new AtomicInteger(0);
        double[] values = new double[3];

        iter.foreachRemaining(d -> {
            values[count.getAndIncrement()] = d;
        });

        Assertions.assertEquals(3, count.get());
        Assertions.assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, values);

        DoubleIterator.empty().foreachRemaining(d -> {
            Assertions.fail("Should not be called");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).foreachRemaining((Throwables.DoubleConsumer<Exception>) null));
    }

    @Test
    public void testForeachRemaining_Null() {
        DoubleIterator iter = DoubleIterator.of(1.0);
        assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining((Throwables.DoubleConsumer<Exception>) null));
    }

    // =================================================
    // foreachIndexed()
    // =================================================

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
    public void testForeachIndexed_PartiallyConsumed() {
        DoubleIterator iter = DoubleIterator.of(10.5, 20.5, 30.5);
        iter.nextDouble();
        int[] firstIndex = { -1 };

        iter.foreachIndexed((index, value) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });

        assertEquals(0, firstIndex[0], "Index should start from 0 even if iterator partially consumed");
    }

    @Test
    public void test_foreachIndexed_empty() {
        AtomicInteger count = new AtomicInteger(0);
        DoubleIterator.empty().foreachIndexed((idx, val) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed_Empty() {
        DoubleIterator iter = DoubleIterator.empty();
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachIndexed((index, value) -> count.incrementAndGet());

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
    public void testForeachIndexed() {
        DoubleIterator iter = DoubleIterator.of(10.5, 20.5, 30.5);
        int[] indices = new int[3];
        double[] values = new double[3];
        AtomicInteger count = new AtomicInteger(0);

        iter.foreachIndexed((idx, val) -> {
            int i = count.getAndIncrement();
            indices[i] = idx;
            values[i] = val;
        });

        Assertions.assertArrayEquals(new int[] { 0, 1, 2 }, indices);
        Assertions.assertArrayEquals(new double[] { 10.5, 20.5, 30.5 }, values);

        DoubleIterator.empty().foreachIndexed((idx, val) -> {
            Assertions.fail("Should not be called");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).foreachIndexed(null));
    }

    @Test
    public void testForeachIndexed_NullAction() {
        DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
        DoubleIterator limited = iter.limit(0);

        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
        assertFalse(limited.hasNext());
        assertThrows(NoSuchElementException.class, () -> limited.nextDouble());
    }

}
