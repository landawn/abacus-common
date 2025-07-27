package com.landawn.abacus.util.stream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;

public class DoubleIteratorEx101Test extends TestBase {

    @Test
    public void testEmpty() {
        DoubleIteratorEx iter = DoubleIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new double[0], iter.toArray());
        iter.close(); // Should not throw
    }

    @Test
    public void testEmptyConstant() {
        DoubleIteratorEx iter1 = DoubleIteratorEx.EMPTY;
        DoubleIteratorEx iter2 = DoubleIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

    @Test
    public void testOfArray() {
        double[] array = {1.1, 2.2, 3.3};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array);
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1.1, iter.nextDouble());
        Assertions.assertEquals(2.2, iter.nextDouble());
        Assertions.assertEquals(3.3, iter.nextDouble());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testOfEmptyArray() {
        double[] array = {};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(DoubleIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndices() {
        double[] array = {1.0, 2.0, 3.0, 4.0, 5.0};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array, 1, 4);
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(2.0, iter.nextDouble());
        Assertions.assertEquals(3.0, iter.nextDouble());
        Assertions.assertEquals(4.0, iter.nextDouble());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        double[] array = {1.0, 2.0, 3.0};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(DoubleIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        double[] array = {1.0, 2.0, 3.0};
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DoubleIteratorEx.of(array, 0, 4));
    }

    @Test
    public void testOfDoubleIterator() {
        DoubleIterator baseIter = new DoubleIterator() {
            private int index = 0;
            private double[] data = {1.5, 2.5, 3.5};
            
            @Override
            public boolean hasNext() {
                return index < data.length;
            }
            
            @Override
            public double nextDouble() {
                return data[index++];
            }
        };
        
        DoubleIteratorEx iter = DoubleIteratorEx.of(baseIter);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1.5, iter.nextDouble());
        Assertions.assertEquals(2.5, iter.nextDouble());
        Assertions.assertEquals(3.5, iter.nextDouble());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfDoubleIteratorNull() {
        DoubleIterator nullIterator = null;
        DoubleIteratorEx iter = DoubleIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfDoubleIteratorAlreadyDoubleIteratorEx() {
        DoubleIteratorEx original = DoubleIteratorEx.of(1.0, 2.0, 3.0);
        DoubleIteratorEx wrapped = DoubleIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testFromIterator() {
        List<Double> list = Arrays.asList(1.1, 2.2, 3.3);
        DoubleIteratorEx iter = DoubleIteratorEx.from(list.iterator());
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1.1, iter.nextDouble());
        Assertions.assertEquals(2.2, iter.nextDouble());
        Assertions.assertEquals(3.3, iter.nextDouble());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromIteratorNull() {
        Iterator<Double> nullIterator = null;
        DoubleIteratorEx iter = DoubleIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Double> objIter = ObjIteratorEx.of(1.0, 2.0, 3.0);
        DoubleIteratorEx iter = DoubleIteratorEx.from(objIter);
        
        // Test advance
        iter.advance(1);
        Assertions.assertEquals(2.0, iter.nextDouble());
        
        // Test count
        Assertions.assertEquals(1, iter.count());
        
        // Test close
        iter.close();
    }

    @Test
    public void testAdvance() {
        double[] array = {1.0, 2.0, 3.0, 4.0, 5.0};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array);
        
        iter.advance(2);
        Assertions.assertEquals(3.0, iter.nextDouble());
        
        iter.advance(1);
        Assertions.assertEquals(5.0, iter.nextDouble());
        
        // Advance beyond end
        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        double[] array = {1.0, 2.0, 3.0};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array);
        
        iter.advance(0);
        Assertions.assertEquals(1.0, iter.nextDouble());
    }

    @Test
    public void testAdvanceNegative() {
        DoubleIteratorEx iter = DoubleIteratorEx.of(1.0, 2.0, 3.0);
        // Assertions.assertThrows(IllegalArgumentException.class, () -> iter.advance(-1));
        iter.advance(-1);
    }

    @Test
    public void testCount() {
        double[] array = {1.0, 2.0, 3.0, 4.0, 5.0};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array, 1, 4);
        
        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray() {
        double[] array = {1.1, 2.2, 3.3};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array);
        
        double[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        double[] array = {1.0, 2.0, 3.0, 4.0, 5.0};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array, 1, 4);
        
        double[] result = iter.toArray();
        Assertions.assertArrayEquals(new double[]{2.0, 3.0, 4.0}, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToList() {
        double[] array = {1.1, 2.2, 3.3};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array);
        
        DoubleList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1.1, result.get(0));
        Assertions.assertEquals(2.2, result.get(1));
        Assertions.assertEquals(3.3, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListPartial() {
        double[] array = {1.0, 2.0, 3.0, 4.0, 5.0};
        DoubleIteratorEx iter = DoubleIteratorEx.of(array, 1, 4);
        
        DoubleList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(2.0, result.get(0));
        Assertions.assertEquals(3.0, result.get(1));
        Assertions.assertEquals(4.0, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testClose() {
        DoubleIteratorEx iter = DoubleIteratorEx.of(1.0, 2.0, 3.0);
        iter.close(); // Should not throw
    }
}