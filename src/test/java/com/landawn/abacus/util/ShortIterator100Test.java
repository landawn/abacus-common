package com.landawn.abacus.util;

import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortSupplier;
import com.landawn.abacus.util.stream.ShortStream;

@Tag("new-test")
public class ShortIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        ShortIterator iter = ShortIterator.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testEmptyConstant() {
        ShortIterator iter = ShortIterator.EMPTY;
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testOfArray() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertEquals((short) 5, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testOfNullArray() {
        ShortIterator iter = ShortIterator.of((short[]) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        short[] array = new short[0];
        ShortIterator iter = ShortIterator.of(array);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRangeFullArray() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array, 0, array.length);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRangeEmptyRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 2, 2);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRangeInvalidRange() {
        short[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, 0, 4));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, 2, 1));
    }

    @Test
    public void testDefer() {
        boolean[] supplierCalled = { false };
        Supplier<ShortIterator> supplier = () -> {
            supplierCalled[0] = true;
            return ShortIterator.of(new short[] { 1, 2, 3 });
        };

        ShortIterator iter = ShortIterator.defer(supplier);
        Assertions.assertFalse(supplierCalled[0]);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertTrue(supplierCalled[0]);
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDeferWithNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        int[] counter = { 0 };
        ShortSupplier supplier = () -> (short) counter[0]++;

        ShortIterator iter = ShortIterator.generate(supplier);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 0, iter.nextShort());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerateWithNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(null));
    }

    @Test
    public void testGenerateWithCondition() {
        int[] counter = { 0 };
        BooleanSupplier hasNext = () -> counter[0] < 3;
        ShortSupplier supplier = () -> (short) counter[0]++;

        ShortIterator iter = ShortIterator.generate(hasNext, supplier);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 0, iter.nextShort());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testGenerateWithConditionNullArgs() {
        BooleanSupplier hasNext = () -> true;
        ShortSupplier supplier = () -> (short) 1;

        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(null, supplier));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(hasNext, null));
    }

    @Test
    public void testNext() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array);

        Assertions.assertEquals(Short.valueOf((short) 1), iter.next());
        Assertions.assertEquals(Short.valueOf((short) 2), iter.next());
        Assertions.assertEquals(Short.valueOf((short) 3), iter.next());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testSkip() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array).skip(2);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertEquals((short) 5, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipZero() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array).skip(0);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipMoreThanAvailable() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array).skip(5);

        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testSkipNegative() {
        short[] array = { 1, 2, 3 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.of(array).skip(-1));
    }

    @Test
    public void testLimit() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array).limit(3);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testLimitZero() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array).limit(0);

        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLimitMoreThanAvailable() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array).limit(5);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLimitNegative() {
        short[] array = { 1, 2, 3 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.of(array).limit(-1));
    }

    @Test
    public void testFilter() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortPredicate evenPredicate = x -> x % 2 == 0;
        ShortIterator iter = ShortIterator.of(array).filter(evenPredicate);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testFilterNoMatch() {
        short[] array = { 1, 3, 5 };
        ShortPredicate evenPredicate = x -> x % 2 == 0;
        ShortIterator iter = ShortIterator.of(array).filter(evenPredicate);

        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testFilterAllMatch() {
        short[] array = { 2, 4, 6 };
        ShortPredicate evenPredicate = x -> x % 2 == 0;
        ShortIterator iter = ShortIterator.of(array).filter(evenPredicate);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertEquals((short) 6, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterNullPredicate() {
        short[] array = { 1, 2, 3 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.of(array).filter(null));
    }

    @Test
    public void testFirst() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array);

        OptionalShort first = iter.first();
        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals((short) 1, first.get());

        Assertions.assertEquals((short) 2, iter.nextShort());
    }

    @Test
    public void testFirstEmpty() {
        ShortIterator iter = ShortIterator.empty();
        OptionalShort first = iter.first();
        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array);

        OptionalShort last = iter.last();
        Assertions.assertTrue(last.isPresent());
        Assertions.assertEquals((short) 3, last.get());

        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLastEmpty() {
        ShortIterator iter = ShortIterator.empty();
        OptionalShort last = iter.last();
        Assertions.assertFalse(last.isPresent());
    }

    @Test
    public void testLastSingleElement() {
        short[] array = { 42 };
        ShortIterator iter = ShortIterator.of(array);

        OptionalShort last = iter.last();
        Assertions.assertTrue(last.isPresent());
        Assertions.assertEquals((short) 42, last.get());
    }

    @Test
    public void testToArray() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        short[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);

        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayEmpty() {
        ShortIterator iter = ShortIterator.empty();
        short[] result = iter.toArray();
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testToArrayPartiallyConsumed() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        iter.nextShort();
        iter.nextShort();

        short[] result = iter.toArray();
        Assertions.assertArrayEquals(new short[] { 3, 4, 5 }, result);
    }

    @Test
    public void testToList() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        ShortList list = iter.toList();
        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals((short) 1, list.get(0));
        Assertions.assertEquals((short) 2, list.get(1));
        Assertions.assertEquals((short) 3, list.get(2));
        Assertions.assertEquals((short) 4, list.get(3));
        Assertions.assertEquals((short) 5, list.get(4));

        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListEmpty() {
        ShortIterator iter = ShortIterator.empty();
        ShortList list = iter.toList();
        Assertions.assertEquals(0, list.size());
    }

    @Test
    public void testStream() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        ShortStream stream = iter.stream();
        Assertions.assertNotNull(stream);

        short[] streamArray = stream.toArray();
        Assertions.assertArrayEquals(array, streamArray);
    }

    @Test
    public void testIndexed() {
        short[] array = { 10, 20, 30 };
        ShortIterator iter = ShortIterator.of(array);

        ObjIterator<IndexedShort> indexed = iter.indexed();

        Assertions.assertTrue(indexed.hasNext());
        IndexedShort first = indexed.next();
        Assertions.assertEquals((short) 10, first.value());
        Assertions.assertEquals(0L, first.index());

        IndexedShort second = indexed.next();
        Assertions.assertEquals((short) 20, second.value());
        Assertions.assertEquals(1L, second.index());

        IndexedShort third = indexed.next();
        Assertions.assertEquals((short) 30, third.value());
        Assertions.assertEquals(2L, third.index());

        Assertions.assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedWithStartIndex() {
        short[] array = { 10, 20, 30 };
        ShortIterator iter = ShortIterator.of(array);

        ObjIterator<IndexedShort> indexed = iter.indexed(100);

        Assertions.assertTrue(indexed.hasNext());
        IndexedShort first = indexed.next();
        Assertions.assertEquals((short) 10, first.value());
        Assertions.assertEquals(100L, first.index());

        IndexedShort second = indexed.next();
        Assertions.assertEquals((short) 20, second.value());
        Assertions.assertEquals(101L, second.index());

        IndexedShort third = indexed.next();
        Assertions.assertEquals((short) 30, third.value());
        Assertions.assertEquals(102L, third.index());

        Assertions.assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedWithNegativeStartIndex() {
        short[] array = { 10, 20, 30 };
        ShortIterator iter = ShortIterator.of(array);

        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testForEachRemaining() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array);

        short[] sum = { 0 };
        iter.forEachRemaining((Short value) -> sum[0] += value);

        Assertions.assertEquals((short) 6, sum[0]);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachRemaining() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array);

        short[] sum = { 0 };
        iter.foreachRemaining(value -> sum[0] += value);

        Assertions.assertEquals((short) 6, sum[0]);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachRemainingPartiallyConsumed() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        iter.nextShort();
        iter.nextShort();

        short[] sum = { 0 };
        iter.foreachRemaining(value -> sum[0] += value);

        Assertions.assertEquals((short) 12, sum[0]);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachIndexed() {
        short[] array = { 10, 20, 30 };
        ShortIterator iter = ShortIterator.of(array);

        short[] values = new short[3];
        int[] indices = new int[3];
        int[] counter = { 0 };

        iter.foreachIndexed((index, value) -> {
            indices[counter[0]] = index;
            values[counter[0]] = value;
            counter[0]++;
        });

        Assertions.assertArrayEquals(new int[] { 0, 1, 2 }, indices);
        Assertions.assertArrayEquals(new short[] { 10, 20, 30 }, values);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testCombinedOperations() {
        short[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        ShortIterator iter = ShortIterator.of(array).skip(2).filter(x -> x % 2 == 0).limit(3);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertEquals((short) 6, iter.nextShort());
        Assertions.assertEquals((short) 8, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayImplementation() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 1, 4);

        short[] result = iter.toArray();
        Assertions.assertArrayEquals(new short[] { 2, 3, 4 }, result);
    }

    @Test
    public void testToListImplementation() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 1, 4);

        ShortList list = iter.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals((short) 2, list.get(0));
        Assertions.assertEquals((short) 3, list.get(1));
        Assertions.assertEquals((short) 4, list.get(2));
    }
}
