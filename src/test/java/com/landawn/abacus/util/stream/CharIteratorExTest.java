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
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;

public class CharIteratorExTest extends TestBase {

    @Test
    public void testEmptyConstant() {
        CharIteratorEx iter1 = CharIteratorEx.EMPTY;
        CharIteratorEx iter2 = CharIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

    // ---- empty() ----

    @Test
    public void testEmpty() {
        CharIteratorEx iter = CharIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextChar());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new char[0], iter.toArray());
        iter.close();
    }

    // ---- of(char[] a, int fromIndex, int toIndex) ----

    @Test
    public void testOfArrayWithIndices() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals('b', iter.nextChar());
        Assertions.assertEquals('c', iter.nextChar());
        Assertions.assertEquals('d', iter.nextChar());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- of(CharIterator iter) ----

    @Test
    public void testOfCharIterator() {
        CharIterator baseIter = new CharIterator() {
            private int index = 0;
            private char[] data = { 'x', 'y', 'z' };

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

    // ---- next() (boxed) ----

    @Test
    public void testNext() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b');
        Assertions.assertEquals(Character.valueOf('a'), iter.next());
        Assertions.assertEquals(Character.valueOf('b'), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- skip(long n) ----

    @Test
    public void testSkip() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c', 'd', 'e');
        CharIterator skipped = iter.skip(2);

        Assertions.assertTrue(skipped.hasNext());
        Assertions.assertEquals('c', skipped.nextChar());
        Assertions.assertEquals('d', skipped.nextChar());
        Assertions.assertEquals('e', skipped.nextChar());
        Assertions.assertFalse(skipped.hasNext());
    }

    // ---- limit(long count) ----

    @Test
    public void testLimit() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c', 'd', 'e');
        CharIterator limited = iter.limit(3);

        Assertions.assertEquals('a', limited.nextChar());
        Assertions.assertEquals('b', limited.nextChar());
        Assertions.assertEquals('c', limited.nextChar());
        Assertions.assertFalse(limited.hasNext());
    }

    // ---- filter(CharPredicate) ----

    @Test
    public void testFilter() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c', 'd', 'e');
        CharIterator filtered = iter.filter(c -> c == 'b' || c == 'd');

        Assertions.assertEquals('b', filtered.nextChar());
        Assertions.assertEquals('d', filtered.nextChar());
        Assertions.assertFalse(filtered.hasNext());
    }

    // ---- indexed() ----

    @Test
    public void testIndexed() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
        var indexed = iter.indexed();

        Assertions.assertTrue(indexed.hasNext());
        var first = indexed.next();
        Assertions.assertEquals(0, first.index());
        Assertions.assertEquals('a', first.value());

        var second = indexed.next();
        Assertions.assertEquals(1, second.index());
        Assertions.assertEquals('b', second.value());

        var third = indexed.next();
        Assertions.assertEquals(2, third.index());
        Assertions.assertEquals('c', third.value());

        Assertions.assertFalse(indexed.hasNext());
    }

    // ---- forEachRemaining(Consumer) ----

    @Test
    public void testForEachRemaining() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
        List<Character> collected = new ArrayList<>();
        iter.forEachRemaining((java.util.function.Consumer<? super Character>) collected::add);

        Assertions.assertEquals(3, collected.size());
        Assertions.assertEquals('a', collected.get(0));
        Assertions.assertEquals('b', collected.get(1));
        Assertions.assertEquals('c', collected.get(2));
    }

    // ---- toArray() ----

    @Test
    public void testToArray() {
        char[] array = { 'a', 'b', 'c' };
        CharIteratorEx iter = CharIteratorEx.of(array);

        char[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 4);

        char[] result = iter.toArray();
        Assertions.assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- toList() ----

    @Test
    public void testToList() {
        char[] array = { 'a', 'b', 'c' };
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
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 4);

        CharList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals('b', result.get(0));
        Assertions.assertEquals('c', result.get(1));
        Assertions.assertEquals('d', result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        char[] array = {};
        CharIteratorEx iter = CharIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(CharIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        char[] array = { 'a', 'b', 'c' };
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(CharIteratorEx.EMPTY, iter);
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

    // ---- stream() ----

    @Test
    public void testStream() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
        CharStream stream = iter.stream();
        Assertions.assertNotNull(stream);
        Assertions.assertEquals(3, stream.count());
    }

    // ---- of(char... a) ----

    @Test
    public void testOfArray() {
        char[] array = { 'a', 'b', 'c' };
        CharIteratorEx iter = CharIteratorEx.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals('a', iter.nextChar());
        Assertions.assertEquals('b', iter.nextChar());
        Assertions.assertEquals('c', iter.nextChar());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        char[] array = { 'a', 'b', 'c' };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CharIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CharIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CharIteratorEx.of(array, 0, 4));
    }

    // ---- from(Iterator<Character>) ----

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
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Character> objIter = ObjIteratorEx.of('a', 'b', 'c');
        CharIteratorEx iter = CharIteratorEx.from(objIter);

        iter.advance(1);
        Assertions.assertEquals('b', iter.nextChar());

        Assertions.assertEquals(1, iter.count());

        iter.close();
    }

    @Test
    public void testFromIteratorNull() {
        Iterator<Character> nullIterator = null;
        CharIteratorEx iter = CharIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- advance(long n) ----

    @Test
    public void testAdvance() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharIteratorEx iter = CharIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals('c', iter.nextChar());

        iter.advance(1);
        Assertions.assertEquals('e', iter.nextChar());

        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        char[] array = { 'a', 'b', 'c' };
        CharIteratorEx iter = CharIteratorEx.of(array);

        iter.advance(0);
        Assertions.assertEquals('a', iter.nextChar());
    }

    @Test
    public void testAdvanceNegative() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
        iter.advance(-1);
        assertNotNull(iter);
    }

    // ---- count() ----

    @Test
    public void testCount() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharIteratorEx iter = CharIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    // ---- close() ----

    @Test
    public void testClose() {
        CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
        iter.close();
        assertNotNull(iter);
    }

}
