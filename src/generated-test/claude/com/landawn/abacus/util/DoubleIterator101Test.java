package com.landawn.abacus.util;

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

public class DoubleIterator101Test extends TestBase {

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

    @Test
    public void testOfVarargs() {
        // Test with empty array
        DoubleIterator iter1 = DoubleIterator.of();
        Assertions.assertFalse(iter1.hasNext());

        // Test with single element
        DoubleIterator iter2 = DoubleIterator.of(1.5);
        Assertions.assertTrue(iter2.hasNext());
        Assertions.assertEquals(1.5, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());

        // Test with multiple elements
        DoubleIterator iter3 = DoubleIterator.of(1.1, 2.2, 3.3);
        Assertions.assertTrue(iter3.hasNext());
        Assertions.assertEquals(1.1, iter3.nextDouble());
        Assertions.assertEquals(2.2, iter3.nextDouble());
        Assertions.assertEquals(3.3, iter3.nextDouble());
        Assertions.assertFalse(iter3.hasNext());

        // Test with null array
        DoubleIterator iter4 = DoubleIterator.of((double[]) null);
        Assertions.assertFalse(iter4.hasNext());
    }

    @Test
    public void testOfArrayWithRange() {
        double[] arr = {1.0, 2.0, 3.0, 4.0, 5.0};

        // Test full range
        DoubleIterator iter1 = DoubleIterator.of(arr, 0, arr.length);
        for (int i = 0; i < arr.length; i++) {
            Assertions.assertTrue(iter1.hasNext());
            Assertions.assertEquals(arr[i], iter1.nextDouble());
        }
        Assertions.assertFalse(iter1.hasNext());

        // Test partial range
        DoubleIterator iter2 = DoubleIterator.of(arr, 1, 4);
        Assertions.assertEquals(2.0, iter2.nextDouble());
        Assertions.assertEquals(3.0, iter2.nextDouble());
        Assertions.assertEquals(4.0, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());

        // Test empty range
        DoubleIterator iter3 = DoubleIterator.of(arr, 2, 2);
        Assertions.assertFalse(iter3.hasNext());

        // Test null array
        DoubleIterator iter4 = DoubleIterator.of(null, 0, 0);
        Assertions.assertFalse(iter4.hasNext());

        // Test invalid range
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, 0, 6));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIterator.of(arr, 3, 2));
    }

    @Test
    public void testDefer() {
        AtomicBoolean initialized = new AtomicBoolean(false);
        
        Supplier<DoubleIterator> supplier = () -> {
            initialized.set(true);
            return DoubleIterator.of(1.5, 2.5, 3.5);
        };

        DoubleIterator iter = DoubleIterator.defer(supplier);
        
        // Supplier not called yet
        Assertions.assertFalse(initialized.get());

        // First access initializes
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(initialized.get());
        
        Assertions.assertEquals(1.5, iter.nextDouble());
        Assertions.assertEquals(2.5, iter.nextDouble());
        Assertions.assertEquals(3.5, iter.nextDouble());
        Assertions.assertFalse(iter.hasNext());

        // Test null supplier
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.defer(null));
    }

    @Test
    public void testGenerateWithSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleSupplier supplier = () -> counter.incrementAndGet() * 1.5;
        
        DoubleIterator iter = DoubleIterator.generate(supplier);
        
        // Infinite iterator
        for (int i = 0; i < 100; i++) {
            Assertions.assertTrue(iter.hasNext());
        }
        
        Assertions.assertEquals(1.5, iter.nextDouble());
        Assertions.assertEquals(3.0, iter.nextDouble());
        Assertions.assertEquals(4.5, iter.nextDouble());

        // Test null supplier
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate((DoubleSupplier) null));
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
        
        // Test NoSuchElementException
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextDouble());

        // Test null arguments
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(null, supplier));
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.generate(hasNext, null));
    }

    @Test
    public void testNext() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5);
        
        // Test deprecated next() method
        Double val1 = iter.next();
        Assertions.assertEquals(1.5, val1);
        
        Double val2 = iter.next();
        Assertions.assertEquals(2.5, val2);
        
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testSkip() {
        double[] arr = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        // Skip 0 elements
        DoubleIterator iter1 = DoubleIterator.of(arr).skip(0);
        Assertions.assertEquals(1.0, iter1.nextDouble());
        
        // Skip some elements
        DoubleIterator iter2 = DoubleIterator.of(arr).skip(2);
        Assertions.assertEquals(3.0, iter2.nextDouble());
        Assertions.assertEquals(4.0, iter2.nextDouble());
        Assertions.assertEquals(5.0, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());
        
        // Skip all elements
        DoubleIterator iter3 = DoubleIterator.of(arr).skip(5);
        Assertions.assertFalse(iter3.hasNext());
        
        // Skip more than available
        DoubleIterator iter4 = DoubleIterator.of(arr).skip(10);
        Assertions.assertFalse(iter4.hasNext());
        
        // Test negative skip
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(arr).skip(-1));
        
        // Test NoSuchElementException after skip
        DoubleIterator iter5 = DoubleIterator.of(arr).skip(5);
        Assertions.assertThrows(NoSuchElementException.class, () -> iter5.nextDouble());
    }

    @Test
    public void testLimit() {
        double[] arr = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        // Limit to 0
        DoubleIterator iter1 = DoubleIterator.of(arr).limit(0);
        Assertions.assertFalse(iter1.hasNext());
        
        // Limit to some elements
        DoubleIterator iter2 = DoubleIterator.of(arr).limit(3);
        Assertions.assertEquals(1.0, iter2.nextDouble());
        Assertions.assertEquals(2.0, iter2.nextDouble());
        Assertions.assertEquals(3.0, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());
        
        // Limit to exact number
        DoubleIterator iter3 = DoubleIterator.of(arr).limit(5);
        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(iter3.hasNext());
            iter3.nextDouble();
        }
        Assertions.assertFalse(iter3.hasNext());
        
        // Limit to more than available
        DoubleIterator iter4 = DoubleIterator.of(arr).limit(10);
        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(iter4.hasNext());
            iter4.nextDouble();
        }
        Assertions.assertFalse(iter4.hasNext());
        
        // Test negative limit
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(arr).limit(-1));
        
        // Test NoSuchElementException after limit
        DoubleIterator iter5 = DoubleIterator.of(arr).limit(2);
        iter5.nextDouble();
        iter5.nextDouble();
        Assertions.assertThrows(NoSuchElementException.class, () -> iter5.nextDouble());
    }

    @Test
    public void testFilter() {
        double[] arr = {1.5, 2.5, 3.5, 4.5, 5.5};
        
        // Filter greater than 3
        DoublePredicate predicate1 = d -> d > 3.0;
        DoubleIterator iter1 = DoubleIterator.of(arr).filter(predicate1);
        Assertions.assertEquals(3.5, iter1.nextDouble());
        Assertions.assertEquals(4.5, iter1.nextDouble());
        Assertions.assertEquals(5.5, iter1.nextDouble());
        Assertions.assertFalse(iter1.hasNext());
        
        // Filter none
        DoublePredicate predicate2 = d -> d > 10.0;
        DoubleIterator iter2 = DoubleIterator.of(arr).filter(predicate2);
        Assertions.assertFalse(iter2.hasNext());
        
        // Filter all
        DoublePredicate predicate3 = d -> d > 0.0;
        DoubleIterator iter3 = DoubleIterator.of(arr).filter(predicate3);
        for (double v : arr) {
            Assertions.assertTrue(iter3.hasNext());
            Assertions.assertEquals(v, iter3.nextDouble());
        }
        
        // Test null predicate
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(arr).filter(null));
        
        // Test NoSuchElementException
        DoubleIterator iter4 = DoubleIterator.of(arr).filter(d -> d > 10.0);
        Assertions.assertThrows(NoSuchElementException.class, () -> iter4.nextDouble());
    }

    @Test
    public void testFirst() {
        // Test with elements
        DoubleIterator iter1 = DoubleIterator.of(1.5, 2.5, 3.5);
        OptionalDouble first1 = iter1.first();
        Assertions.assertTrue(first1.isPresent());
        Assertions.assertEquals(1.5, first1.get());
        
        // Test empty iterator
        DoubleIterator iter2 = DoubleIterator.empty();
        OptionalDouble first2 = iter2.first();
        Assertions.assertFalse(first2.isPresent());
        
        // Test single element
        DoubleIterator iter3 = DoubleIterator.of(7.7);
        OptionalDouble first3 = iter3.first();
        Assertions.assertTrue(first3.isPresent());
        Assertions.assertEquals(7.7, first3.get());
    }

    @Test
    public void testLast() {
        // Test with elements
        DoubleIterator iter1 = DoubleIterator.of(1.5, 2.5, 3.5);
        OptionalDouble last1 = iter1.last();
        Assertions.assertTrue(last1.isPresent());
        Assertions.assertEquals(3.5, last1.get());
        
        // Test empty iterator
        DoubleIterator iter2 = DoubleIterator.empty();
        OptionalDouble last2 = iter2.last();
        Assertions.assertFalse(last2.isPresent());
        
        // Test single element
        DoubleIterator iter3 = DoubleIterator.of(9.9);
        OptionalDouble last3 = iter3.last();
        Assertions.assertTrue(last3.isPresent());
        Assertions.assertEquals(9.9, last3.get());
    }

    @Test
    public void testToArray() {
        // Test normal case
        DoubleIterator iter1 = DoubleIterator.of(1.1, 2.2, 3.3);
        double[] arr1 = iter1.toArray();
        Assertions.assertArrayEquals(new double[]{1.1, 2.2, 3.3}, arr1);
        
        // Test empty iterator
        DoubleIterator iter2 = DoubleIterator.empty();
        double[] arr2 = iter2.toArray();
        Assertions.assertEquals(0, arr2.length);
        
        // Test partial consumption
        DoubleIterator iter3 = DoubleIterator.of(1.0, 2.0, 3.0, 4.0);
        iter3.nextDouble(); // consume first element
        double[] arr3 = iter3.toArray();
        Assertions.assertArrayEquals(new double[]{2.0, 3.0, 4.0}, arr3);
        
        // Test with array range
        double[] source = {1.0, 2.0, 3.0, 4.0, 5.0};
        DoubleIterator iter4 = DoubleIterator.of(source, 1, 4);
        double[] arr4 = iter4.toArray();
        Assertions.assertArrayEquals(new double[]{2.0, 3.0, 4.0}, arr4);
    }

    @Test
    public void testToList() {
        // Test normal case
        DoubleIterator iter1 = DoubleIterator.of(1.1, 2.2, 3.3);
        DoubleList list1 = iter1.toList();
        Assertions.assertEquals(3, list1.size());
        Assertions.assertEquals(1.1, list1.get(0));
        Assertions.assertEquals(2.2, list1.get(1));
        Assertions.assertEquals(3.3, list1.get(2));
        
        // Test empty iterator
        DoubleIterator iter2 = DoubleIterator.empty();
        DoubleList list2 = iter2.toList();
        Assertions.assertEquals(0, list2.size());
        
        // Test partial consumption
        DoubleIterator iter3 = DoubleIterator.of(1.0, 2.0, 3.0, 4.0);
        iter3.nextDouble(); // consume first element
        DoubleList list3 = iter3.toList();
        Assertions.assertEquals(3, list3.size());
        Assertions.assertEquals(2.0, list3.get(0));
        Assertions.assertEquals(3.0, list3.get(1));
        Assertions.assertEquals(4.0, list3.get(2));
    }

    @Test
    public void testStream() {
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5, 4.5);
        DoubleStream stream = iter.stream();
        Assertions.assertNotNull(stream);
        
        // Verify stream can process elements
        double sum = stream.sum();
        Assertions.assertEquals(12.0, sum);
    }

    @Test
    public void testIndexed() {
        // Test default indexing from 0
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

    @Test
    public void testIndexedWithStartIndex() {
        // Test with custom start index
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
        
        // Test negative start index
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleIterator.of(1.0).indexed(-1));
        
        // Test empty iterator
        ObjIterator<IndexedDouble> emptyIndexed = DoubleIterator.empty().indexed(5);
        Assertions.assertFalse(emptyIndexed.hasNext());
    }

    @Test
    public void testForEachRemaining() {
        // Test deprecated method with Consumer<Double>
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        AtomicInteger count = new AtomicInteger(0);
        double[] values = new double[3];
        
        iter.forEachRemaining((Double d) -> {
            values[count.getAndIncrement()] = d;
        });
        
        Assertions.assertEquals(3, count.get());
        Assertions.assertArrayEquals(new double[]{1.5, 2.5, 3.5}, values);
        
        // Test null consumer
        Assertions.assertThrows(NullPointerException.class, () -> 
            DoubleIterator.of(1.0).forEachRemaining((java.util.function.Consumer<? super Double>) null)
        );
    }

    @Test
    public void testForeachRemaining() {
        // Test with DoubleConsumer
        DoubleIterator iter = DoubleIterator.of(1.5, 2.5, 3.5);
        AtomicInteger count = new AtomicInteger(0);
        double sum = 0;
        double[] values = new double[3];
        
        iter.foreachRemaining(d -> {
            values[count.getAndIncrement()] = d;
        });
        
        Assertions.assertEquals(3, count.get());
        Assertions.assertArrayEquals(new double[]{1.5, 2.5, 3.5}, values);
        
        // Test with empty iterator
        DoubleIterator.empty().foreachRemaining(d -> {
            Assertions.fail("Should not be called");
        });
        
        // Test null action
        Assertions.assertThrows(NullPointerException.class, () ->
            DoubleIterator.of(1.0).foreachRemaining((Throwables.DoubleConsumer<Exception>) null)
        );
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
        
        Assertions.assertArrayEquals(new int[]{0, 1, 2}, indices);
        Assertions.assertArrayEquals(new double[]{10.5, 20.5, 30.5}, values);
        
        // Test with empty iterator
        DoubleIterator.empty().foreachIndexed((idx, val) -> {
            Assertions.fail("Should not be called");
        });
        
        // Test null action
        Assertions.assertThrows(NullPointerException.class, () -> 
            DoubleIterator.of(1.0).foreachIndexed(null)
        );
    }

    @Test
    public void testCombinedOperations() {
        // Test skip + limit
        double[] arr = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0};
        DoubleIterator iter1 = DoubleIterator.of(arr).skip(2).limit(3);
        Assertions.assertEquals(3.0, iter1.nextDouble());
        Assertions.assertEquals(4.0, iter1.nextDouble());
        Assertions.assertEquals(5.0, iter1.nextDouble());
        Assertions.assertFalse(iter1.hasNext());
        
        // Test filter + skip + limit
        DoubleIterator iter2 = DoubleIterator.of(arr)
            .filter(d -> d % 2 == 0)  // 2, 4, 6
            .skip(1)                   // 4, 6
            .limit(1);                 // 4
        Assertions.assertEquals(4.0, iter2.nextDouble());
        Assertions.assertFalse(iter2.hasNext());
        
        // Test limit + filter
        DoubleIterator iter3 = DoubleIterator.of(arr)
            .limit(4)                  // 1, 2, 3, 4
            .filter(d -> d > 2);       // 3, 4
        Assertions.assertEquals(3.0, iter3.nextDouble());
        Assertions.assertEquals(4.0, iter3.nextDouble());
        Assertions.assertFalse(iter3.hasNext());
    }

    @Test
    public void testIteratorConsistency() {
        // Test that hasNext() can be called multiple times without side effects
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
}