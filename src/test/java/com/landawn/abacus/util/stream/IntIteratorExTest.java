package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;

public class IntIteratorExTest extends TestBase {

    @Test
    public void testEmptyConstant() {
        IntIteratorEx iter1 = IntIteratorEx.EMPTY;
        IntIteratorEx iter2 = IntIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

    // ---- empty() ----

    @Test
    public void testEmpty() {
        IntIteratorEx iter = IntIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextInt());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new int[0], iter.toArray());
        iter.close();
    }

    // ---- of(int[] a, int fromIndex, int toIndex) ----

    @Test
    public void testOfArrayWithIndices() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIteratorEx iter = IntIteratorEx.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(2, iter.nextInt());
        Assertions.assertEquals(3, iter.nextInt());
        Assertions.assertEquals(4, iter.nextInt());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- of(IntIterator iter) ----

    @Test
    public void testOfIntIterator() {
        IntIterator baseIter = new IntIterator() {
            private int index = 0;
            private int[] data = { 10, 20, 30 };

            @Override
            public boolean hasNext() {
                return index < data.length;
            }

            @Override
            public int nextInt() {
                return data[index++];
            }
        };

        IntIteratorEx iter = IntIteratorEx.of(baseIter);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(10, iter.nextInt());
        Assertions.assertEquals(20, iter.nextInt());
        Assertions.assertEquals(30, iter.nextInt());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- next() (boxed) ----

    @Test
    public void testNext() {
        IntIteratorEx iter = IntIteratorEx.of(10, 20);
        Assertions.assertEquals(Integer.valueOf(10), iter.next());
        Assertions.assertEquals(Integer.valueOf(20), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- skip(long n) ----

    @Test
    public void testSkip() {
        IntIteratorEx iter = IntIteratorEx.of(1, 2, 3, 4, 5);
        IntIterator skipped = iter.skip(2);

        Assertions.assertTrue(skipped.hasNext());
        Assertions.assertEquals(3, skipped.nextInt());
        Assertions.assertEquals(4, skipped.nextInt());
        Assertions.assertEquals(5, skipped.nextInt());
        Assertions.assertFalse(skipped.hasNext());
    }

    // ---- limit(long count) ----

    @Test
    public void testLimit() {
        IntIteratorEx iter = IntIteratorEx.of(1, 2, 3, 4, 5);
        IntIterator limited = iter.limit(3);

        Assertions.assertEquals(1, limited.nextInt());
        Assertions.assertEquals(2, limited.nextInt());
        Assertions.assertEquals(3, limited.nextInt());
        Assertions.assertFalse(limited.hasNext());
    }

    // ---- filter(IntPredicate) ----

    @Test
    public void testFilter() {
        IntIteratorEx iter = IntIteratorEx.of(1, 2, 3, 4, 5);
        IntIterator filtered = iter.filter(i -> i % 2 == 0);

        Assertions.assertEquals(2, filtered.nextInt());
        Assertions.assertEquals(4, filtered.nextInt());
        Assertions.assertFalse(filtered.hasNext());
    }

    // ---- indexed() ----

    @Test
    public void testIndexed() {
        IntIteratorEx iter = IntIteratorEx.of(10, 20, 30);
        var indexed = iter.indexed();

        Assertions.assertTrue(indexed.hasNext());
        var first = indexed.next();
        Assertions.assertEquals(0, first.index());
        Assertions.assertEquals(10, first.value());

        var second = indexed.next();
        Assertions.assertEquals(1, second.index());
        Assertions.assertEquals(20, second.value());

        var third = indexed.next();
        Assertions.assertEquals(2, third.index());
        Assertions.assertEquals(30, third.value());

        Assertions.assertFalse(indexed.hasNext());
    }

    // ---- forEachRemaining(Consumer) ----

    @Test
    public void testForEachRemaining() {
        IntIteratorEx iter = IntIteratorEx.of(1, 2, 3);
        List<Integer> collected = new ArrayList<>();
        iter.forEachRemaining((java.util.function.Consumer<? super Integer>) collected::add);

        Assertions.assertEquals(3, collected.size());
        Assertions.assertEquals(1, collected.get(0));
        Assertions.assertEquals(2, collected.get(1));
        Assertions.assertEquals(3, collected.get(2));
    }

    // ---- toArray() ----

    @Test
    public void testToArray() {
        int[] array = { 1, 2, 3 };
        IntIteratorEx iter = IntIteratorEx.of(array);

        int[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIteratorEx iter = IntIteratorEx.of(array, 1, 4);

        int[] result = iter.toArray();
        Assertions.assertArrayEquals(new int[] { 2, 3, 4 }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- toList() ----

    @Test
    public void testToList() {
        int[] array = { 1, 2, 3 };
        IntIteratorEx iter = IntIteratorEx.of(array);

        IntList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1, result.get(0));
        Assertions.assertEquals(2, result.get(1));
        Assertions.assertEquals(3, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListPartial() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIteratorEx iter = IntIteratorEx.of(array, 1, 4);

        IntList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(2, result.get(0));
        Assertions.assertEquals(3, result.get(1));
        Assertions.assertEquals(4, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        int[] array = {};
        IntIteratorEx iter = IntIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(IntIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        int[] array = { 1, 2, 3 };
        IntIteratorEx iter = IntIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(IntIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfIntIteratorNull() {
        IntIterator nullIterator = null;
        IntIteratorEx iter = IntIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIntIteratorAlreadyIntIteratorEx() {
        IntIteratorEx original = IntIteratorEx.of(1, 2, 3);
        IntIteratorEx wrapped = IntIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    // ---- stream() ----

    @Test
    public void testStream() {
        IntIteratorEx iter = IntIteratorEx.of(1, 2, 3);
        IntStream stream = iter.stream();
        Assertions.assertNotNull(stream);
        Assertions.assertEquals(3, stream.count());
    }

    // ---- of(int... a) ----

    @Test
    public void testOfArray() {
        int[] array = { 1, 2, 3 };
        IntIteratorEx iter = IntIteratorEx.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1, iter.nextInt());
        Assertions.assertEquals(2, iter.nextInt());
        Assertions.assertEquals(3, iter.nextInt());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        int[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IntIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IntIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IntIteratorEx.of(array, 0, 4));
    }

    // ---- from(Iterator<Integer>) ----

    @Test
    public void testFromIterator() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        IntIteratorEx iter = IntIteratorEx.from(list.iterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1, iter.nextInt());
        Assertions.assertEquals(2, iter.nextInt());
        Assertions.assertEquals(3, iter.nextInt());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Integer> objIter = ObjIteratorEx.of(1, 2, 3);
        IntIteratorEx iter = IntIteratorEx.from(objIter);

        iter.advance(1);
        Assertions.assertEquals(2, iter.nextInt());

        Assertions.assertEquals(1, iter.count());

        iter.close();
    }

    @Test
    public void testFromIteratorNull() {
        Iterator<Integer> nullIterator = null;
        IntIteratorEx iter = IntIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- advance(long n) ----

    @Test
    public void testAdvance() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIteratorEx iter = IntIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals(3, iter.nextInt());

        iter.advance(1);
        Assertions.assertEquals(5, iter.nextInt());

        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        int[] array = { 1, 2, 3 };
        IntIteratorEx iter = IntIteratorEx.of(array);

        iter.advance(0);
        Assertions.assertEquals(1, iter.nextInt());
    }

    @Test
    public void testAdvanceNegative() {
        IntIteratorEx iter = IntIteratorEx.of(1, 2, 3);
        iter.advance(-1);
        assertNotNull(iter);
    }

    // ---- count() ----

    @Test
    public void testCount() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIteratorEx iter = IntIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- close() ----

    @Test
    public void testClose() {
        IntIteratorEx iter = IntIteratorEx.of(1, 2, 3);
        iter.close();
        assertNotNull(iter);
    }

}
