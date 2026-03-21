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
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;

public class ByteIteratorExTest extends TestBase {

    @Test
    public void testEmptyConstant() {
        ByteIteratorEx iter1 = ByteIteratorEx.EMPTY;
        ByteIteratorEx iter2 = ByteIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

    // ---- empty() ----

    @Test
    public void testEmpty() {
        ByteIteratorEx iter = ByteIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextByte());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new byte[0], iter.toArray());
        iter.close();
    }

    // ---- of(byte[] a, int fromIndex, int toIndex) ----

    @Test
    public void testOfArrayWithIndices() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteIteratorEx iter = ByteIteratorEx.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((byte) 2, iter.nextByte());
        Assertions.assertEquals((byte) 3, iter.nextByte());
        Assertions.assertEquals((byte) 4, iter.nextByte());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- of(ByteIterator iter) ----

    @Test
    public void testOfByteIterator() {
        ByteIterator baseIter = new ByteIterator() {
            private int index = 0;
            private byte[] data = { 10, 20, 30 };

            @Override
            public boolean hasNext() {
                return index < data.length;
            }

            @Override
            public byte nextByte() {
                return data[index++];
            }
        };

        ByteIteratorEx iter = ByteIteratorEx.of(baseIter);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((byte) 10, iter.nextByte());
        Assertions.assertEquals((byte) 20, iter.nextByte());
        Assertions.assertEquals((byte) 30, iter.nextByte());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- next() (boxed) ----

    @Test
    public void testNext() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 10, (byte) 20);
        Assertions.assertEquals(Byte.valueOf((byte) 10), iter.next());
        Assertions.assertEquals(Byte.valueOf((byte) 20), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- skip(long n) ----

    @Test
    public void testSkip() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteIterator skipped = iter.skip(2);

        Assertions.assertTrue(skipped.hasNext());
        Assertions.assertEquals((byte) 3, skipped.nextByte());
        Assertions.assertEquals((byte) 4, skipped.nextByte());
        Assertions.assertEquals((byte) 5, skipped.nextByte());
        Assertions.assertFalse(skipped.hasNext());
    }

    // ---- limit(long count) ----

    @Test
    public void testLimit() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteIterator limited = iter.limit(3);

        Assertions.assertEquals((byte) 1, limited.nextByte());
        Assertions.assertEquals((byte) 2, limited.nextByte());
        Assertions.assertEquals((byte) 3, limited.nextByte());
        Assertions.assertFalse(limited.hasNext());
    }

    // ---- filter(BytePredicate) ----

    @Test
    public void testFilter() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteIterator filtered = iter.filter(b -> b % 2 == 0);

        Assertions.assertEquals((byte) 2, filtered.nextByte());
        Assertions.assertEquals((byte) 4, filtered.nextByte());
        Assertions.assertFalse(filtered.hasNext());
    }

    // ---- indexed() ----

    @Test
    public void testIndexed() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 10, (byte) 20, (byte) 30);
        var indexed = iter.indexed();

        Assertions.assertTrue(indexed.hasNext());
        var first = indexed.next();
        Assertions.assertEquals(0, first.index());
        Assertions.assertEquals((byte) 10, first.value());

        var second = indexed.next();
        Assertions.assertEquals(1, second.index());
        Assertions.assertEquals((byte) 20, second.value());

        var third = indexed.next();
        Assertions.assertEquals(2, third.index());
        Assertions.assertEquals((byte) 30, third.value());

        Assertions.assertFalse(indexed.hasNext());
    }

    // ---- forEachRemaining(Consumer) ----

    @Test
    public void testForEachRemaining() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> collected = new ArrayList<>();
        iter.forEachRemaining((java.util.function.Consumer<? super Byte>) collected::add);

        Assertions.assertEquals(3, collected.size());
        Assertions.assertEquals((byte) 1, collected.get(0));
        Assertions.assertEquals((byte) 2, collected.get(1));
        Assertions.assertEquals((byte) 3, collected.get(2));
    }

    // ---- toArray() ----

    @Test
    public void testToArray() {
        byte[] array = { 1, 2, 3 };
        ByteIteratorEx iter = ByteIteratorEx.of(array);

        byte[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteIteratorEx iter = ByteIteratorEx.of(array, 1, 4);

        byte[] result = iter.toArray();
        Assertions.assertArrayEquals(new byte[] { 2, 3, 4 }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- toList() ----

    @Test
    public void testToList() {
        byte[] array = { 1, 2, 3 };
        ByteIteratorEx iter = ByteIteratorEx.of(array);

        ByteList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals((byte) 1, result.get(0));
        Assertions.assertEquals((byte) 2, result.get(1));
        Assertions.assertEquals((byte) 3, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListPartial() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteIteratorEx iter = ByteIteratorEx.of(array, 1, 4);

        ByteList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals((byte) 2, result.get(0));
        Assertions.assertEquals((byte) 3, result.get(1));
        Assertions.assertEquals((byte) 4, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        byte[] array = {};
        ByteIteratorEx iter = ByteIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(ByteIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        byte[] array = { 1, 2, 3 };
        ByteIteratorEx iter = ByteIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(ByteIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfByteIteratorNull() {
        ByteIterator nullIterator = null;
        ByteIteratorEx iter = ByteIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfByteIteratorAlreadyByteIteratorEx() {
        ByteIteratorEx original = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        ByteIteratorEx wrapped = ByteIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    // ---- first() ----

    @Test
    public void testFirst() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 10, (byte) 20, (byte) 30);
        Assertions.assertTrue(iter.first().isPresent());
        Assertions.assertEquals((byte) 10, iter.first().orElse((byte) 0));

        ByteIteratorEx emptyIter = ByteIteratorEx.empty();
        Assertions.assertFalse(emptyIter.first().isPresent());
    }

    // ---- last() ----

    @Test
    public void testLast() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 10, (byte) 20, (byte) 30);
        Assertions.assertTrue(iter.last().isPresent());
        Assertions.assertEquals((byte) 30, iter.last().orElse((byte) 0));

        ByteIteratorEx emptyIter = ByteIteratorEx.empty();
        Assertions.assertFalse(emptyIter.last().isPresent());
    }

    // ---- stream() ----

    @Test
    public void testStream() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream = iter.stream();
        Assertions.assertNotNull(stream);
        Assertions.assertEquals(3, stream.count());
    }

    // ---- of(byte... a) ----

    @Test
    public void testOfArray() {
        byte[] array = { 1, 2, 3 };
        ByteIteratorEx iter = ByteIteratorEx.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((byte) 1, iter.nextByte());
        Assertions.assertEquals((byte) 2, iter.nextByte());
        Assertions.assertEquals((byte) 3, iter.nextByte());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextByte());
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        byte[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ByteIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ByteIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ByteIteratorEx.of(array, 0, 4));
    }

    // ---- from(Iterator<Byte>) ----

    @Test
    public void testFromIterator() {
        List<Byte> list = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        ByteIteratorEx iter = ByteIteratorEx.from(list.iterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((byte) 1, iter.nextByte());
        Assertions.assertEquals((byte) 2, iter.nextByte());
        Assertions.assertEquals((byte) 3, iter.nextByte());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Byte> objIter = ObjIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        ByteIteratorEx iter = ByteIteratorEx.from(objIter);

        iter.advance(1);
        Assertions.assertEquals((byte) 2, iter.nextByte());

        Assertions.assertEquals(1, iter.count());

        iter.close();
    }

    @Test
    public void testFromIteratorNull() {
        Iterator<Byte> nullIterator = null;
        ByteIteratorEx iter = ByteIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- advance(long n) ----

    @Test
    public void testAdvance() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteIteratorEx iter = ByteIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals((byte) 3, iter.nextByte());

        iter.advance(1);
        Assertions.assertEquals((byte) 5, iter.nextByte());

        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        byte[] array = { 1, 2, 3 };
        ByteIteratorEx iter = ByteIteratorEx.of(array);

        iter.advance(0);
        Assertions.assertEquals((byte) 1, iter.nextByte());
    }

    @Test
    public void testAdvanceNegative() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        iter.advance(-1);
        assertNotNull(iter);
    }

    // ---- count() ----

    @Test
    public void testCount() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteIteratorEx iter = ByteIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- close() ----

    @Test
    public void testClose() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        iter.close();
        assertNotNull(iter);
    }

}
