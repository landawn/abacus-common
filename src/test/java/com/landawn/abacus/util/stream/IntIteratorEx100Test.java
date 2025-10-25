package com.landawn.abacus.util.stream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;

@Tag("new-test")
public class IntIteratorEx100Test extends TestBase {

    @Test
    public void testEmpty() {
        IntIteratorEx iter = IntIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextInt());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new int[0], iter.toArray());
        iter.close();
    }

    @Test
    public void testEmptyConstant() {
        IntIteratorEx iter1 = IntIteratorEx.EMPTY;
        IntIteratorEx iter2 = IntIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

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
    public void testOfEmptyArray() {
        int[] array = {};
        IntIteratorEx iter = IntIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(IntIteratorEx.EMPTY, iter);
    }

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

    @Test
    public void testOfArrayWithIndicesEmpty() {
        int[] array = { 1, 2, 3 };
        IntIteratorEx iter = IntIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(IntIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        int[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IntIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IntIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> IntIteratorEx.of(array, 0, 4));
    }

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
    public void testFromIteratorNull() {
        Iterator<Integer> nullIterator = null;
        IntIteratorEx iter = IntIteratorEx.from(nullIterator);
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
    }

    @Test
    public void testCount() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntIteratorEx iter = IntIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

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
    public void testClose() {
        IntIteratorEx iter = IntIteratorEx.of(1, 2, 3);
        iter.close();
    }
}
