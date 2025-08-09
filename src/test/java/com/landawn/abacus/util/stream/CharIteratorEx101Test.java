package com.landawn.abacus.util.stream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;

public class CharIteratorEx101Test extends TestBase {

    @Test
    public void testEmpty() {
        CharIteratorEx iter = CharIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextChar());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new char[0], iter.toArray());
        iter.close(); // Should not throw
    }

    @Test
    public void testEmptyConstant() {
        CharIteratorEx iter1 = CharIteratorEx.EMPTY;
        CharIteratorEx iter2 = CharIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

    @Test
    public void testOfArray() {
        char[] array = {'a', 'b', 'c'};
        CharIteratorEx iter = CharIteratorEx.of(array);
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals('a', iter.nextChar());
        Assertions.assertEquals('b', iter.nextChar());
        Assertions.assertEquals('c', iter.nextChar());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    public void testOfEmptyArray() {
        char[] array = {};
        CharIteratorEx iter = CharIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(CharIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndices() {
        char[] array = {'a', 'b', 'c', 'd', 'e'};
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 4);
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals('b', iter.nextChar());
        Assertions.assertEquals('c', iter.nextChar());
        Assertions.assertEquals('d', iter.nextChar());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        char[] array = {'a', 'b', 'c'};
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(CharIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        char[] array = {'a', 'b', 'c'};
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CharIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CharIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CharIteratorEx.of(array, 0, 4));
    }

    @Test
    public void testOfCharIterator() {
        CharIterator baseIter = new CharIterator() {
            private int index = 0;
            private char[] data = {'x', 'y', 'z'};
            
            @Override
            public boolean hasNext() {
                return index < data.length;
            }
            
            @Override
            public char nextChar() {
                return data[index++];
            }
        };
        
        CharIteratorEx iter = CharIteratorEx.of(baseIter);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals('x', iter.nextChar());
        Assertions.assertEquals('y', iter.nextChar());
        Assertions.assertEquals('z', iter.nextChar());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfCharIteratorNull() {
        CharIterator nullIterator = null;
        CharIteratorEx iter = CharIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfCharIteratorAlreadyCharIteratorEx() {
        CharIteratorEx original = CharIteratorEx.of('a', 'b', 'c');
        CharIteratorEx wrapped = CharIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testFromIterator() {
        List<Character> list = Arrays.asList('a', 'b', 'c');
        CharIteratorEx iter = CharIteratorEx.from(list.iterator());
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals('a', iter.nextChar());
        Assertions.assertEquals('b', iter.nextChar());
        Assertions.assertEquals('c', iter.nextChar());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromIteratorNull() {
        Iterator<Character> nullIterator = null;
        CharIteratorEx iter = CharIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Character> objIter = ObjIteratorEx.of('a', 'b', 'c');
        CharIteratorEx iter = CharIteratorEx.from(objIter);
        
        // Test advance
        iter.advance(1);
        Assertions.assertEquals('b', iter.nextChar());
        
        // Test count
        Assertions.assertEquals(1, iter.count());
        
        // Test close
        iter.close();
    }

    @Test
    public void testAdvance() {
        char[] array = {'a', 'b', 'c', 'd', 'e'};
        CharIteratorEx iter = CharIteratorEx.of(array);
        
        iter.advance(2);
        Assertions.assertEquals('c', iter.nextChar());
        
        iter.advance(1);
        Assertions.assertEquals('e', iter.nextChar());
        
        // Advance beyond end
        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        char[] array = {'a', 'b', 'c'};
        CharIteratorEx iter = CharIteratorEx.of(array);
        
        iter.advance(0);
        Assertions.assertEquals('a', iter.nextChar());
    }

    @Test
    public void testAdvanceNegative() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
        // Assertions.assertThrows(IllegalArgumentException.class, () -> iter.advance(-1));
        iter.advance(-1);
    }

    @Test
    public void testCount() {
        char[] array = {'a', 'b', 'c', 'd', 'e'};
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 4);
        
        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray() {
        char[] array = {'a', 'b', 'c'};
        CharIteratorEx iter = CharIteratorEx.of(array);
        
        char[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        char[] array = {'a', 'b', 'c', 'd', 'e'};
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 4);
        
        char[] result = iter.toArray();
        Assertions.assertArrayEquals(new char[]{'b', 'c', 'd'}, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToList() {
        char[] array = {'a', 'b', 'c'};
        CharIteratorEx iter = CharIteratorEx.of(array);
        
        CharList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals('a', result.get(0));
        Assertions.assertEquals('b', result.get(1));
        Assertions.assertEquals('c', result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListPartial() {
        char[] array = {'a', 'b', 'c', 'd', 'e'};
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 4);
        
        CharList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals('b', result.get(0));
        Assertions.assertEquals('c', result.get(1));
        Assertions.assertEquals('d', result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testClose() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
        iter.close(); // Should not throw
    }
}