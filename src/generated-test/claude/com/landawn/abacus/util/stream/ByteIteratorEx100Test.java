package com.landawn.abacus.util.stream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;

public class ByteIteratorEx100Test extends TestBase {

    @Test
    public void testEmpty() {
        ByteIteratorEx iter = ByteIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextByte());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new byte[0], iter.toArray());
        iter.close(); // Should not throw
    }

    @Test
    public void testEmptyConstant() {
        ByteIteratorEx iter1 = ByteIteratorEx.EMPTY;
        ByteIteratorEx iter2 = ByteIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

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
    public void testOfEmptyArray() {
        byte[] array = {};
        ByteIteratorEx iter = ByteIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(ByteIteratorEx.EMPTY, iter);
    }

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

    @Test
    public void testOfArrayWithIndicesEmpty() {
        byte[] array = { 1, 2, 3 };
        ByteIteratorEx iter = ByteIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(ByteIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        byte[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ByteIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ByteIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ByteIteratorEx.of(array, 0, 4));
    }

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
    public void testFromIteratorNull() {
        Iterator<Byte> nullIterator = null;
        ByteIteratorEx iter = ByteIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Byte> objIter = ObjIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        ByteIteratorEx iter = ByteIteratorEx.from(objIter);

        // Test advance
        iter.advance(1);
        Assertions.assertEquals((byte) 2, iter.nextByte());

        // Test count
        Assertions.assertEquals(1, iter.count());

        // Test close
        iter.close();
    }

    @Test
    public void testAdvance() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteIteratorEx iter = ByteIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals((byte) 3, iter.nextByte());

        iter.advance(1);
        Assertions.assertEquals((byte) 5, iter.nextByte());

        // Advance beyond end
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
    }

    @Test
    public void testCount() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteIteratorEx iter = ByteIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

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
    public void testClose() {
        ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        iter.close(); // Should not throw
    }
}