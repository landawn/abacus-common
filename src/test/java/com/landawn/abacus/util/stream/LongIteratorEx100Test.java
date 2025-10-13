package com.landawn.abacus.util.stream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;

@Tag("new-test")
public class LongIteratorEx100Test extends TestBase {

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

    @Test
    public void testOfArray() {
        long[] array = {1L, 2L, 3L};
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

    @Test
    public void testOfArrayWithIndices() {
        long[] array = {1L, 2L, 3L, 4L, 5L};
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 4);
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(2L, iter.nextLong());
        Assertions.assertEquals(3L, iter.nextLong());
        Assertions.assertEquals(4L, iter.nextLong());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        long[] array = {1L, 2L, 3L};
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(LongIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        long[] array = {1L, 2L, 3L};
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> LongIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> LongIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> LongIteratorEx.of(array, 0, 4));
    }

    @Test
    public void testOfLongIterator() {
        LongIterator baseIter = new LongIterator() {
            private int index = 0;
            private long[] data = {10L, 20L, 30L};
            
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

    @Test
    public void testAdvance() {
        long[] array = {1L, 2L, 3L, 4L, 5L};
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
        long[] array = {1L, 2L, 3L};
        LongIteratorEx iter = LongIteratorEx.of(array);
        
        iter.advance(0);
        Assertions.assertEquals(1L, iter.nextLong());
    }

    @Test
    public void testAdvanceNegative() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L);
        iter.advance(-1);
    }

    @Test
    public void testCount() {
        long[] array = {1L, 2L, 3L, 4L, 5L};
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 4);
        
        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray() {
        long[] array = {1L, 2L, 3L};
        LongIteratorEx iter = LongIteratorEx.of(array);
        
        long[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        long[] array = {1L, 2L, 3L, 4L, 5L};
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 4);
        
        long[] result = iter.toArray();
        Assertions.assertArrayEquals(new long[]{2L, 3L, 4L}, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToList() {
        long[] array = {1L, 2L, 3L};
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
        long[] array = {1L, 2L, 3L, 4L, 5L};
        LongIteratorEx iter = LongIteratorEx.of(array, 1, 4);
        
        LongList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(2L, result.get(0));
        Assertions.assertEquals(3L, result.get(1));
        Assertions.assertEquals(4L, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testClose() {
        LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L);
        iter.close();
    }
}
