package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;

@Tag("new-test")
public class LongIteratorExTest extends TestBase {

    // ---- empty() ----

    @Test
    public void testEmpty() {
        LongIteratorEx iter = LongIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextLong());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new long[0], iter.toArray());
        iter.close();
    }

    @Test
    public void testEmptyConstant() {
        LongIteratorEx iter1 = LongIteratorEx.EMPTY;
        LongIteratorEx iter2 = LongIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

    // ---- of(long... a) ----

    @Test
    public void testOfArray() {
        long[] array = { 1L, 2L, 3L };
        LongIteratorEx iter = LongIteratorEx.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1L, iter.nextLong());
        Assertions.assertEquals(2L, iter.nextLong());
        Assertions.assertEquals(3L, iter.nextLong());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testOfEmptyArray() {
        long[] array = {};
        LongIteratorEx iter = LongIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(LongIteratorEx.EMPTY, iter);
    }

    // ---- of(long[] a, int fromIndex, int toIndex) ----

    @Test
    public void testOfArrayWithIndices() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(2L, iter.nextLong());
        Assertions.assertEquals(3L, iter.nextLong());
        Assertions.assertEquals(4L, iter.nextLong());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        long[] array = { 1L, 2L, 3L };
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(LongIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        long[] array = { 1L, 2L, 3L };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> LongIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> LongIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> LongIteratorEx.of(array, 0, 4));
    }

    // ---- of(LongIterator iter) ----

    @Test
    public void testOfLongIterator() {
        LongIterator baseIter = new LongIterator() {
            private int index = 0;
            private long[] data = { 10L, 20L, 30L };

            @Override
            public boolean hasNext() {
                return index < data.length;
            }

            @Override
            public long nextLong() {
                return data[index++];
            }
        };

        LongIteratorEx iter = LongIteratorEx.of(baseIter);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(10L, iter.nextLong());
        Assertions.assertEquals(20L, iter.nextLong());
        Assertions.assertEquals(30L, iter.nextLong());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfLongIteratorNull() {
        LongIterator nullIterator = null;
        LongIteratorEx iter = LongIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfLongIteratorAlreadyLongIteratorEx() {
        LongIteratorEx original = LongIteratorEx.of(1L, 2L, 3L);
        LongIteratorEx wrapped = LongIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    // ---- from(Iterator<Long>) ----

    @Test
    public void testFromIterator() {
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        LongIteratorEx iter = LongIteratorEx.from(list.iterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1L, iter.nextLong());
        Assertions.assertEquals(2L, iter.nextLong());
        Assertions.assertEquals(3L, iter.nextLong());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromIteratorNull() {
        Iterator<Long> nullIterator = null;
        LongIteratorEx iter = LongIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Long> objIter = ObjIteratorEx.of(1L, 2L, 3L);
        LongIteratorEx iter = LongIteratorEx.from(objIter);

        iter.advance(1);
        Assertions.assertEquals(2L, iter.nextLong());

        Assertions.assertEquals(1, iter.count());

        iter.close();
    }

    // ---- advance(long n) ----

    @Test
    public void testAdvance() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongIteratorEx iter = LongIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals(3L, iter.nextLong());

        iter.advance(1);
        Assertions.assertEquals(5L, iter.nextLong());

        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        long[] array = { 1L, 2L, 3L };
        LongIteratorEx iter = LongIteratorEx.of(array);

        iter.advance(0);
        Assertions.assertEquals(1L, iter.nextLong());
    }

    @Test
    public void testAdvanceNegative() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L);
        iter.advance(-1);
        assertNotNull(iter);
    }

    // ---- count() ----

    @Test
    public void testCount() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- next() (boxed) ----

    @Test
    public void testNext() {
        LongIteratorEx iter = LongIteratorEx.of(10L, 20L);
        Assertions.assertEquals(Long.valueOf(10L), iter.next());
        Assertions.assertEquals(Long.valueOf(20L), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- skip(long n) ----

    @Test
    public void testSkip() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L, 4L, 5L);
        LongIterator skipped = iter.skip(2);

        Assertions.assertTrue(skipped.hasNext());
        Assertions.assertEquals(3L, skipped.nextLong());
        Assertions.assertEquals(4L, skipped.nextLong());
        Assertions.assertEquals(5L, skipped.nextLong());
        Assertions.assertFalse(skipped.hasNext());
    }

    // ---- limit(long count) ----

    @Test
    public void testLimit() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L, 4L, 5L);
        LongIterator limited = iter.limit(3);

        Assertions.assertEquals(1L, limited.nextLong());
        Assertions.assertEquals(2L, limited.nextLong());
        Assertions.assertEquals(3L, limited.nextLong());
        Assertions.assertFalse(limited.hasNext());
    }

    // ---- filter(LongPredicate) ----

    @Test
    public void testFilter() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L, 4L, 5L);
        LongIterator filtered = iter.filter(l -> l % 2 == 0);

        Assertions.assertEquals(2L, filtered.nextLong());
        Assertions.assertEquals(4L, filtered.nextLong());
        Assertions.assertFalse(filtered.hasNext());
    }

    // ---- stream() ----

    @Test
    public void testStream() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L);
        LongStream stream = iter.stream();
        Assertions.assertNotNull(stream);
        Assertions.assertEquals(3, stream.count());
    }

    // ---- indexed() ----

    @Test
    public void testIndexed() {
        LongIteratorEx iter = LongIteratorEx.of(10L, 20L, 30L);
        var indexed = iter.indexed();

        Assertions.assertTrue(indexed.hasNext());
        var first = indexed.next();
        Assertions.assertEquals(0, first.index());
        Assertions.assertEquals(10L, first.value());

        var second = indexed.next();
        Assertions.assertEquals(1, second.index());
        Assertions.assertEquals(20L, second.value());

        var third = indexed.next();
        Assertions.assertEquals(2, third.index());
        Assertions.assertEquals(30L, third.value());

        Assertions.assertFalse(indexed.hasNext());
    }

    // ---- forEachRemaining(Consumer) ----

    @Test
    public void testForEachRemaining() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L);
        List<Long> collected = new ArrayList<>();
        iter.forEachRemaining((java.util.function.Consumer<? super Long>) collected::add);

        Assertions.assertEquals(3, collected.size());
        Assertions.assertEquals(1L, collected.get(0));
        Assertions.assertEquals(2L, collected.get(1));
        Assertions.assertEquals(3L, collected.get(2));
    }

    // ---- toArray() ----

    @Test
    public void testToArray() {
        long[] array = { 1L, 2L, 3L };
        LongIteratorEx iter = LongIteratorEx.of(array);

        long[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 4);

        long[] result = iter.toArray();
        Assertions.assertArrayEquals(new long[] { 2L, 3L, 4L }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- toList() ----

    @Test
    public void testToList() {
        long[] array = { 1L, 2L, 3L };
        LongIteratorEx iter = LongIteratorEx.of(array);

        LongList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1L, result.get(0));
        Assertions.assertEquals(2L, result.get(1));
        Assertions.assertEquals(3L, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListPartial() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 4);

        LongList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(2L, result.get(0));
        Assertions.assertEquals(3L, result.get(1));
        Assertions.assertEquals(4L, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- close() ----

    @Test
    public void testClose() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L);
        iter.close();
        assertNotNull(iter);
    }

}
