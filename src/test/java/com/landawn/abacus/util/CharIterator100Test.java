package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.stream.CharStream;

public class CharIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        CharIterator iter = CharIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    public void testOfArray() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        assertTrue(iter.hasNext());
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        CharIterator iter = CharIterator.of(new char[0]);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRange() {
        char[] arr = {'a', 'b', 'c', 'd', 'e'};
        CharIterator iter = CharIterator.of(arr, 1, 4);
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertEquals('d', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithInvalidRange() {
        char[] arr = {'a', 'b', 'c'};
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(arr, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(arr, 2, 1));
    }

    @Test
    public void testDefer() {
        boolean[] supplierCalled = {false};
        Supplier<CharIterator> supplier = () -> {
            supplierCalled[0] = true;
            return CharIterator.of('a', 'b', 'c');
        };
        
        CharIterator iter = CharIterator.defer(supplier);
        assertFalse(supplierCalled[0]);
        
        assertTrue(iter.hasNext());
        assertTrue(supplierCalled[0]);
        assertEquals('a', iter.nextChar());
    }

    @Test
    public void testDeferWithNull() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        CharSupplier supplier = () -> 'X';
        CharIterator iter = CharIterator.generate(supplier);
        
        assertTrue(iter.hasNext());
        assertEquals('X', iter.nextChar());
        assertTrue(iter.hasNext());
        assertEquals('X', iter.nextChar());
        // Always returns true for hasNext
    }

    @Test
    public void testGenerateWithNull() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate(null));
    }

    @Test
    public void testGenerateWithCondition() {
        int[] count = {0};
        BooleanSupplier hasNext = () -> count[0] < 3;
        CharSupplier supplier = () -> (char)('A' + count[0]++);
        
        CharIterator iter = CharIterator.generate(hasNext, supplier);
        
        assertEquals('A', iter.nextChar());
        assertEquals('B', iter.nextChar());
        assertEquals('C', iter.nextChar());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    public void testGenerateWithConditionNullParams() {
        assertThrows(IllegalArgumentException.class, 
            () -> CharIterator.generate(null, () -> 'a'));
        assertThrows(IllegalArgumentException.class, 
            () -> CharIterator.generate(() -> true, null));
    }

    @Test
    public void testNext() {
        CharIterator iter = CharIterator.of('a', 'b');
        assertEquals(Character.valueOf('a'), iter.next());
        assertEquals(Character.valueOf('b'), iter.next());
    }

    @Test
    public void testFirst() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        OptionalChar first = iter.first();
        assertTrue(first.isPresent());
        assertEquals('a', first.get());
        // Iterator should still have remaining elements
        assertTrue(iter.hasNext());
        assertEquals('b', iter.nextChar());
    }

    @Test
    public void testFirstEmpty() {
        CharIterator iter = CharIterator.empty();
        OptionalChar first = iter.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        OptionalChar last = iter.last();
        assertTrue(last.isPresent());
        assertEquals('c', last.get());
        // Iterator should be exhausted
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLastEmpty() {
        CharIterator iter = CharIterator.empty();
        OptionalChar last = iter.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testToArray() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        char[] array = iter.toArray();
        assertArrayEquals(new char[]{'a', 'b', 'c'}, array);
    }

    @Test
    public void testToArrayEmpty() {
        CharIterator iter = CharIterator.empty();
        char[] array = iter.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToArrayPartiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        iter.nextChar(); // consume 'a'
        char[] array = iter.toArray();
        assertArrayEquals(new char[]{'b', 'c'}, array);
    }

    @Test
    public void testToList() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        CharList list = iter.toList();
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    public void testToListEmpty() {
        CharIterator iter = CharIterator.empty();
        CharList list = iter.toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testStream() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        CharStream stream = iter.stream();
        assertNotNull(stream);
        
        char[] result = stream.toArray();
        assertArrayEquals(new char[]{'a', 'b', 'c'}, result);
    }

    @Test
    public void testIndexed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        ObjIterator<IndexedChar> indexed = iter.indexed();
        
        IndexedChar ic1 = indexed.next();
        assertEquals(0, ic1.index());
        assertEquals('a', ic1.value());
        
        IndexedChar ic2 = indexed.next();
        assertEquals(1, ic2.index());
        assertEquals('b', ic2.value());
        
        IndexedChar ic3 = indexed.next();
        assertEquals(2, ic3.index());
        assertEquals('c', ic3.value());
        
        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedWithStartIndex() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        ObjIterator<IndexedChar> indexed = iter.indexed(10);
        
        IndexedChar ic1 = indexed.next();
        assertEquals(10, ic1.index());
        assertEquals('a', ic1.value());
        
        IndexedChar ic2 = indexed.next();
        assertEquals(11, ic2.index());
        assertEquals('b', ic2.value());
    }

    @Test
    public void testIndexedWithNegativeStartIndex() {
        CharIterator iter = CharIterator.of('a');
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testForEachRemaining() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        
        iter.forEachRemaining(ch -> sb.append(ch));
        
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testForeachRemaining() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        
        iter.foreachRemaining(ch -> sb.append(ch));
        
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testForeachRemainingPartiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        iter.nextChar(); // consume 'a'
        
        StringBuilder sb = new StringBuilder();
        iter.foreachRemaining(ch -> sb.append(ch));
        
        assertEquals("bc", sb.toString());
    }

    @Test
    public void testForeachIndexed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        
        iter.foreachIndexed((index, ch) -> sb.append(index).append(':').append(ch).append(' '));
        
        assertEquals("0:a 1:b 2:c ", sb.toString());
    }

    @Test
    public void testForeachIndexedEmpty() {
        CharIterator iter = CharIterator.empty();
        int[] callCount = {0};
        
        iter.foreachIndexed((index, ch) -> callCount[0]++);
        
        assertEquals(0, callCount[0]);
    }

    @Test
    public void testForeachIndexedNull() {
        CharIterator iter = CharIterator.of('a');
        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

    // Test custom iterator implementation
    @Test
    public void testCustomIterator() {
        CharIterator iter = new CharIterator() {
            private int count = 0;
            
            @Override
            public boolean hasNext() {
                return count < 3;
            }
            
            @Override
            public char nextChar() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return (char)('X' + count++);
            }
        };
        
        assertEquals('X', iter.nextChar());
        assertEquals('Y', iter.nextChar());
        assertEquals('Z', iter.nextChar());
        assertFalse(iter.hasNext());
    }
}