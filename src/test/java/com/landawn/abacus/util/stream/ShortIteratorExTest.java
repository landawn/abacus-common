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
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;

public class ShortIteratorExTest extends TestBase {

    @Test
    public void testEmptyConstant() {
        ShortIteratorEx iter1 = ShortIteratorEx.EMPTY;
        ShortIteratorEx iter2 = ShortIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

    // ---- empty() ----

    @Test
    public void testEmpty() {
        ShortIteratorEx iter = ShortIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new short[0], iter.toArray());
        iter.close();
    }

    // ---- of(short[] a, int fromIndex, int toIndex) ----

    @Test
    public void testOfArrayWithIndices() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIteratorEx iter = ShortIteratorEx.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- of(ShortIterator iter) ----

    @Test
    public void testOfShortIterator() {
        ShortIterator baseIter = new ShortIterator() {
            private int index = 0;
            private short[] data = { 10, 20, 30 };

            @Override
            public boolean hasNext() {
                return index < data.length;
            }

            @Override
            public short nextShort() {
                return data[index++];
            }
        };

        ShortIteratorEx iter = ShortIteratorEx.of(baseIter);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 10, iter.nextShort());
        Assertions.assertEquals((short) 20, iter.nextShort());
        Assertions.assertEquals((short) 30, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- next() (boxed) ----

    @Test
    public void testNext() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 10, (short) 20);
        Assertions.assertEquals(Short.valueOf((short) 10), iter.next());
        Assertions.assertEquals(Short.valueOf((short) 20), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- skip(long n) ----

    @Test
    public void testSkip() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortIterator skipped = iter.skip(2);

        Assertions.assertTrue(skipped.hasNext());
        Assertions.assertEquals((short) 3, skipped.nextShort());
        Assertions.assertEquals((short) 4, skipped.nextShort());
        Assertions.assertEquals((short) 5, skipped.nextShort());
        Assertions.assertFalse(skipped.hasNext());
    }

    // ---- limit(long count) ----

    @Test
    public void testLimit() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortIterator limited = iter.limit(3);

        Assertions.assertEquals((short) 1, limited.nextShort());
        Assertions.assertEquals((short) 2, limited.nextShort());
        Assertions.assertEquals((short) 3, limited.nextShort());
        Assertions.assertFalse(limited.hasNext());
    }

    // ---- filter(ShortPredicate) ----

    @Test
    public void testFilter() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortIterator filtered = iter.filter(s -> s % 2 == 0);

        Assertions.assertEquals((short) 2, filtered.nextShort());
        Assertions.assertEquals((short) 4, filtered.nextShort());
        Assertions.assertFalse(filtered.hasNext());
    }

    // ---- indexed() ----

    @Test
    public void testIndexed() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 10, (short) 20, (short) 30);
        var indexed = iter.indexed();

        Assertions.assertTrue(indexed.hasNext());
        var first = indexed.next();
        Assertions.assertEquals(0, first.index());
        Assertions.assertEquals((short) 10, first.value());

        var second = indexed.next();
        Assertions.assertEquals(1, second.index());
        Assertions.assertEquals((short) 20, second.value());

        var third = indexed.next();
        Assertions.assertEquals(2, third.index());
        Assertions.assertEquals((short) 30, third.value());

        Assertions.assertFalse(indexed.hasNext());
    }

    // ---- forEachRemaining(Consumer) ----

    @Test
    public void testForEachRemaining() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3);
        List<Short> collected = new ArrayList<>();
        iter.forEachRemaining((java.util.function.Consumer<? super Short>) collected::add);

        Assertions.assertEquals(3, collected.size());
        Assertions.assertEquals((short) 1, collected.get(0));
        Assertions.assertEquals((short) 2, collected.get(1));
        Assertions.assertEquals((short) 3, collected.get(2));
    }

    // ---- toArray() ----

    @Test
    public void testToArray() {
        short[] array = { 1, 2, 3 };
        ShortIteratorEx iter = ShortIteratorEx.of(array);

        short[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIteratorEx iter = ShortIteratorEx.of(array, 1, 4);

        short[] result = iter.toArray();
        Assertions.assertArrayEquals(new short[] { 2, 3, 4 }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- toList() ----

    @Test
    public void testToList() {
        short[] array = { 1, 2, 3 };
        ShortIteratorEx iter = ShortIteratorEx.of(array);

        ShortList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals((short) 1, result.get(0));
        Assertions.assertEquals((short) 2, result.get(1));
        Assertions.assertEquals((short) 3, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListPartial() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIteratorEx iter = ShortIteratorEx.of(array, 1, 4);

        ShortList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals((short) 2, result.get(0));
        Assertions.assertEquals((short) 3, result.get(1));
        Assertions.assertEquals((short) 4, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        short[] array = {};
        ShortIteratorEx iter = ShortIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(ShortIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        short[] array = { 1, 2, 3 };
        ShortIteratorEx iter = ShortIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(ShortIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfShortIteratorNull() {
        ShortIterator nullIterator = null;
        ShortIteratorEx iter = ShortIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfShortIteratorAlreadyShortIteratorEx() {
        ShortIteratorEx original = ShortIteratorEx.of((short) 1, (short) 2, (short) 3);
        ShortIteratorEx wrapped = ShortIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    // ---- stream() ----

    @Test
    public void testStream() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3);
        ShortStream stream = iter.stream();
        Assertions.assertNotNull(stream);
        Assertions.assertEquals(3, stream.count());
    }

    // ---- of(short... a) ----

    @Test
    public void testOfArray() {
        short[] array = { 1, 2, 3 };
        ShortIteratorEx iter = ShortIteratorEx.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        short[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIteratorEx.of(array, 0, 4));
    }

    // ---- from(Iterator<Short>) ----

    @Test
    public void testFromIterator() {
        List<Short> list = Arrays.asList((short) 1, (short) 2, (short) 3);
        ShortIteratorEx iter = ShortIteratorEx.from(list.iterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Short> objIter = ObjIteratorEx.of((short) 1, (short) 2, (short) 3);
        ShortIteratorEx iter = ShortIteratorEx.from(objIter);

        iter.advance(1);
        Assertions.assertEquals((short) 2, iter.nextShort());

        Assertions.assertEquals(1, iter.count());

        iter.close();
    }

    @Test
    public void testFromIteratorNull() {
        Iterator<Short> nullIterator = null;
        ShortIteratorEx iter = ShortIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- advance(long n) ----

    @Test
    public void testAdvance() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIteratorEx iter = ShortIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals((short) 3, iter.nextShort());

        iter.advance(1);
        Assertions.assertEquals((short) 5, iter.nextShort());

        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        short[] array = { 1, 2, 3 };
        ShortIteratorEx iter = ShortIteratorEx.of(array);

        iter.advance(0);
        Assertions.assertEquals((short) 1, iter.nextShort());
    }

    @Test
    public void testAdvanceNegative() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3);
        iter.advance(-1);
        assertNotNull(iter);
    }

    // ---- count() ----

    @Test
    public void testCount() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIteratorEx iter = ShortIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- close() ----

    @Test
    public void testClose() {
        ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3);
        iter.close();
        assertNotNull(iter);
    }

}
