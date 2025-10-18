package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.CharStream;

@Tag("2025")
public class CharIterator2025Test extends TestBase {

    @Test
    public void test_empty() {
        CharIterator iter = CharIterator.empty();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_empty_nextChar_throwsException() {
        CharIterator iter = CharIterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    public void test_empty_singleton() {
        CharIterator iter1 = CharIterator.empty();
        CharIterator iter2 = CharIterator.empty();
        assertSame(iter1, iter2);
    }

    @Test
    public void test_of_varargs() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        assertTrue(iter.hasNext());
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_varargs_empty() {
        CharIterator iter = CharIterator.of();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_varargs_null() {
        CharIterator iter = CharIterator.of((char[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_varargs_singleElement() {
        CharIterator iter = CharIterator.of('x');
        assertTrue(iter.hasNext());
        assertEquals('x', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange() {
        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        CharIterator iter = CharIterator.of(chars, 1, 4);
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertEquals('d', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_emptyRange() {
        char[] chars = { 'a', 'b', 'c' };
        CharIterator iter = CharIterator.of(chars, 1, 1);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_fullRange() {
        char[] chars = { 'a', 'b', 'c' };
        CharIterator iter = CharIterator.of(chars, 0, 3);
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_nullArray() {
        CharIterator iter = CharIterator.of(null, 0, 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_invalidIndices() {
        char[] chars = { 'a', 'b', 'c' };
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(chars, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(chars, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(chars, 2, 1));
    }

    @Test
    public void test_of_withRange_toArray() {
        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        CharIterator iter = CharIterator.of(chars, 1, 4);
        iter.nextChar();
        char[] result = iter.toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_withRange_toList() {
        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        CharIterator iter = CharIterator.of(chars, 1, 4);
        iter.nextChar();
        CharList result = iter.toList();
        assertEquals(CharList.of('c', 'd'), result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_defer() {
        AtomicInteger callCount = new AtomicInteger(0);
        CharIterator iter = CharIterator.defer(() -> {
            callCount.incrementAndGet();
            return CharIterator.of('a', 'b', 'c');
        });
        assertEquals(0, callCount.get());
        assertTrue(iter.hasNext());
        assertEquals(1, callCount.get());
        assertEquals('a', iter.nextChar());
        assertEquals(1, callCount.get());
    }

    @Test
    public void test_defer_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.defer(null));
    }

    @Test
    public void test_defer_initOnNextChar() {
        AtomicInteger callCount = new AtomicInteger(0);
        CharIterator iter = CharIterator.defer(() -> {
            callCount.incrementAndGet();
            return CharIterator.of('x');
        });
        assertEquals(0, callCount.get());
        assertEquals('x', iter.nextChar());
        assertEquals(1, callCount.get());
    }

    @Test
    public void test_generate_infinite() {
        CharIterator iter = CharIterator.generate(() -> 'X');
        assertTrue(iter.hasNext());
        assertEquals('X', iter.nextChar());
        assertTrue(iter.hasNext());
        assertEquals('X', iter.nextChar());
        assertTrue(iter.hasNext());
    }

    @Test
    public void test_generate_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate((com.landawn.abacus.util.function.CharSupplier) null));
    }

    @Test
    public void test_generate_withCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        CharIterator iter = CharIterator.generate(() -> (char) ('A' + counter.getAndIncrement()));
        assertEquals('A', iter.nextChar());
        assertEquals('B', iter.nextChar());
        assertEquals('C', iter.nextChar());
    }

    @Test
    public void test_generate_conditional() {
        AtomicInteger count = new AtomicInteger(0);
        CharIterator iter = CharIterator.generate(() -> count.get() < 3, () -> (char) ('A' + count.getAndIncrement()));
        assertTrue(iter.hasNext());
        assertEquals('A', iter.nextChar());
        assertTrue(iter.hasNext());
        assertEquals('B', iter.nextChar());
        assertTrue(iter.hasNext());
        assertEquals('C', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_generate_conditional_nullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate(null, () -> 'x'));
    }

    @Test
    public void test_generate_conditional_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate(() -> true, null));
    }

    @Test
    public void test_generate_conditional_nextCharWhenNoElements() {
        CharIterator iter = CharIterator.generate(() -> false, () -> 'x');
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    public void test_next_boxed() {
        CharIterator iter = CharIterator.of('a', 'b');
        Character ch = iter.next();
        assertEquals(Character.valueOf('a'), ch);
    }

    @Test
    public void test_nextChar() {
        CharIterator iter = CharIterator.of('x', 'y', 'z');
        assertEquals('x', iter.nextChar());
        assertEquals('y', iter.nextChar());
        assertEquals('z', iter.nextChar());
    }

    @Test
    public void test_nextChar_noSuchElement() {
        CharIterator iter = CharIterator.of('a');
        iter.nextChar();
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    public void test_first() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        OptionalChar first = iter.first();
        assertTrue(first.isPresent());
        assertEquals('a', first.get());
        assertTrue(iter.hasNext());
        assertEquals('b', iter.nextChar());
    }

    @Test
    public void test_first_empty() {
        CharIterator iter = CharIterator.empty();
        OptionalChar first = iter.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void test_first_singleElement() {
        CharIterator iter = CharIterator.of('x');
        OptionalChar first = iter.first();
        assertTrue(first.isPresent());
        assertEquals('x', first.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_last() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        OptionalChar last = iter.last();
        assertTrue(last.isPresent());
        assertEquals('c', last.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_last_empty() {
        CharIterator iter = CharIterator.empty();
        OptionalChar last = iter.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void test_last_singleElement() {
        CharIterator iter = CharIterator.of('x');
        OptionalChar last = iter.last();
        assertTrue(last.isPresent());
        assertEquals('x', last.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_toArray() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        char[] result = iter.toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_toArray_empty() {
        CharIterator iter = CharIterator.empty();
        char[] result = iter.toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void test_toArray_partiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd');
        iter.nextChar();
        iter.nextChar();
        char[] result = iter.toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
    }

    @Test
    public void test_toList() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        CharList result = iter.toList();
        assertEquals(CharList.of('a', 'b', 'c'), result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_toList_empty() {
        CharIterator iter = CharIterator.empty();
        CharList result = iter.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_toList_partiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd');
        iter.nextChar();
        CharList result = iter.toList();
        assertEquals(CharList.of('b', 'c', 'd'), result);
    }

    @Test
    public void test_stream() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        CharStream stream = iter.stream();
        assertNotNull(stream);
        char[] result = stream.toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void test_stream_empty() {
        CharIterator iter = CharIterator.empty();
        CharStream stream = iter.stream();
        char[] result = stream.toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void test_stream_operations() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        long count = iter.stream().count();
        assertEquals(3, count);
    }

    @Test
    public void test_indexed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        ObjIterator<IndexedChar> indexed = iter.indexed();
        assertTrue(indexed.hasNext());
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
    public void test_indexed_empty() {
        CharIterator iter = CharIterator.empty();
        ObjIterator<IndexedChar> indexed = iter.indexed();
        assertFalse(indexed.hasNext());
    }

    @Test
    public void test_indexed_withStartIndex() {
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
    public void test_indexed_withStartIndex_negative() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void test_indexed_withStartIndex_zero() {
        CharIterator iter = CharIterator.of('x');
        ObjIterator<IndexedChar> indexed = iter.indexed(0);
        IndexedChar ic = indexed.next();
        assertEquals(0, ic.index());
        assertEquals('x', ic.value());
    }

    @Test
    public void test_forEachRemaining_deprecated() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        iter.forEachRemaining((Character ch) -> sb.append(ch));
        assertEquals("abc", sb.toString());
    }

    @Test
    public void test_foreachRemaining() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        iter.foreachRemaining(ch -> sb.append(ch));
        assertEquals("abc", sb.toString());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_foreachRemaining_empty() {
        CharIterator iter = CharIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachRemaining(ch -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void test_foreachRemaining_nullAction() {
        CharIterator iter = CharIterator.of('a');
        assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining((com.landawn.abacus.util.Throwables.CharConsumer<RuntimeException>) null));
    }

    @Test
    public void test_foreachRemaining_partiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd');
        iter.nextChar();
        iter.nextChar();
        StringBuilder sb = new StringBuilder();
        iter.foreachRemaining(ch -> sb.append(ch));
        assertEquals("cd", sb.toString());
    }

    @Test
    public void test_foreachIndexed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        iter.foreachIndexed((idx, ch) -> sb.append(idx).append(':').append(ch).append(' '));
        assertEquals("0:a 1:b 2:c ", sb.toString());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_foreachIndexed_empty() {
        CharIterator iter = CharIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachIndexed((idx, ch) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void test_foreachIndexed_nullAction() {
        CharIterator iter = CharIterator.of('a');
        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

    @Test
    public void test_foreachIndexed_partiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd');
        iter.nextChar();
        StringBuilder sb = new StringBuilder();
        iter.foreachIndexed((idx, ch) -> sb.append(idx).append(':').append(ch).append(' '));
        assertEquals("0:b 1:c 2:d ", sb.toString());
    }

    @Test
    public void test_foreachIndexed_verifyIndices() {
        CharIterator iter = CharIterator.of('x', 'y', 'z');
        AtomicInteger expectedIndex = new AtomicInteger(0);
        iter.foreachIndexed((idx, ch) -> {
            assertEquals(expectedIndex.getAndIncrement(), idx);
        });
        assertEquals(3, expectedIndex.get());
    }

    @Test
    public void test_hasNext_multipleCalls() {
        CharIterator iter = CharIterator.of('a', 'b');
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals('a', iter.nextChar());
    }

    @Test
    public void test_specialCharacters() {
        CharIterator iter = CharIterator.of('\n', '\t', '\r', '\0');
        assertEquals('\n', iter.nextChar());
        assertEquals('\t', iter.nextChar());
        assertEquals('\r', iter.nextChar());
        assertEquals('\0', iter.nextChar());
    }

    @Test
    public void test_unicodeCharacters() {
        CharIterator iter = CharIterator.of('\u4E2D', '\u6587', '\u5B57');
        assertEquals('\u4E2D', iter.nextChar());
        assertEquals('\u6587', iter.nextChar());
        assertEquals('\u5B57', iter.nextChar());
    }

    @Test
    public void test_maxMinCharValues() {
        CharIterator iter = CharIterator.of(Character.MIN_VALUE, Character.MAX_VALUE);
        assertEquals(Character.MIN_VALUE, iter.nextChar());
        assertEquals(Character.MAX_VALUE, iter.nextChar());
    }

    @Test
    public void test_of_largeArray() {
        char[] chars = new char[1000];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) ('A' + (i % 26));
        }
        CharIterator iter = CharIterator.of(chars);
        int count = 0;
        while (iter.hasNext()) {
            iter.nextChar();
            count++;
        }
        assertEquals(1000, count);
    }

    @Test
    public void test_stream_map() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        char[] result = iter.stream().map(ch -> (char) (ch + 1)).toArray();
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
    }

    @Test
    public void test_stream_filter() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd', 'e');
        char[] result = iter.stream().filter(ch -> ch < 'd').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void test_combinedOperations() {
        char[] chars = { 'a', 'b', 'c', 'd', 'e', 'f' };
        CharIterator iter = CharIterator.of(chars, 1, 5);

        OptionalChar first = iter.first();
        assertEquals('b', first.get());

        iter.nextChar();

        char[] rest = iter.toArray();
        assertArrayEquals(new char[] { 'd', 'e' }, rest);
    }

    @Test
    public void test_defer_withEmptyIterator() {
        CharIterator iter = CharIterator.defer(() -> CharIterator.empty());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_generate_conditional_multipleHasNextCalls() {
        AtomicInteger count = new AtomicInteger(0);
        CharIterator iter = CharIterator.generate(() -> count.get() < 2, () -> (char) ('A' + count.getAndIncrement()));
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals('A', iter.nextChar());
        assertTrue(iter.hasNext());
        assertEquals('B', iter.nextChar());
        assertFalse(iter.hasNext());
    }
}
