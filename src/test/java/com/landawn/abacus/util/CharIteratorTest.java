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
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.stream.CharStream;

public class CharIteratorTest extends TestBase {

    @Test
    public void testEmpty_singleton() {
        CharIterator iter1 = CharIterator.empty();
        CharIterator iter2 = CharIterator.empty();
        assertSame(iter1, iter2);
    }

    @Test
    public void testEmpty_sameAsField() {
        assertSame(CharIterator.EMPTY, CharIterator.empty());
    }

    // ==================== empty() ====================

    @Test
    public void testEmpty() {
        CharIterator iter = CharIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    // ==================== of(char...) ====================

    @Test
    public void testOf() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        assertTrue(iter.hasNext());
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_specialCharacters() {
        CharIterator iter = CharIterator.of('\n', '\t', '\r', '\0');
        assertEquals('\n', iter.nextChar());
        assertEquals('\t', iter.nextChar());
        assertEquals('\r', iter.nextChar());
        assertEquals('\0', iter.nextChar());
    }

    @Test
    public void testOf_unicodeCharacters() {
        CharIterator iter = CharIterator.of('\u4E2D', '\u6587', '\u5B57');
        assertEquals('\u4E2D', iter.nextChar());
        assertEquals('\u6587', iter.nextChar());
        assertEquals('\u5B57', iter.nextChar());
    }

    @Test
    public void testOf_largeArray() {
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

    // ==================== of(char[], int, int) ====================

    @Test
    public void testOfWithRange() {
        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        CharIterator iter = CharIterator.of(chars, 1, 4);
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertEquals('d', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRange_fullRange() {
        char[] chars = { 'a', 'b', 'c' };
        CharIterator iter = CharIterator.of(chars, 0, 3);
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRange_toArray() {
        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        CharIterator iter = CharIterator.of(chars, 1, 4);
        iter.nextChar();
        char[] result = iter.toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRange_toList() {
        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        CharIterator iter = CharIterator.of(chars, 1, 4);
        iter.nextChar();
        CharList result = iter.toList();
        assertEquals(CharList.of('c', 'd'), result);
        assertFalse(iter.hasNext());
    }

    // ==================== combined operations ====================

    @Test
    public void testCombinedOperations_skipAndLimit() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd', 'e').skip(1).limit(3);
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertEquals('d', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCombinedOperations_filterAndToArray() {
        char[] result = CharIterator.of('a', 'b', 'c', 'd', 'e').filter(ch -> ch != 'c').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'd', 'e' }, result);
    }

    @Test
    public void testCombinedOperations_filterAndToList() {
        CharList result = CharIterator.of('a', 'b', 'c', 'd').filter(ch -> ch >= 'c').toList();
        assertEquals(CharList.of('c', 'd'), result);
    }

    @Test
    public void testCombinedOperations_skipFilterLimit() {
        char[] result = CharIterator.of('a', 'b', 'c', 'd', 'e', 'f').skip(1).filter(ch -> ch != 'd').limit(2).toArray();
        assertArrayEquals(new char[] { 'b', 'c' }, result);
    }

    @Test
    public void testOf_emptyArray() {
        CharIterator iter = CharIterator.of(new char[0]);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_nullArray() {
        CharIterator iter = CharIterator.of((char[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_singleElement() {
        CharIterator iter = CharIterator.of('x');
        assertTrue(iter.hasNext());
        assertEquals('x', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_maxMinCharValues() {
        CharIterator iter = CharIterator.of(Character.MIN_VALUE, Character.MAX_VALUE);
        assertEquals(Character.MIN_VALUE, iter.nextChar());
        assertEquals(Character.MAX_VALUE, iter.nextChar());
    }

    @Test
    public void testOf_hasNextMultipleCalls() {
        CharIterator iter = CharIterator.of('a', 'b');
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals('a', iter.nextChar());
    }

    @Test
    public void testOfWithRange_emptyRange() {
        char[] chars = { 'a', 'b', 'c' };
        CharIterator iter = CharIterator.of(chars, 1, 1);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRange_nullArray() {
        CharIterator iter = CharIterator.of(null, 0, 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfWithRange_invalidIndices() {
        char[] chars = { 'a', 'b', 'c' };
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(chars, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(chars, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> CharIterator.of(chars, 2, 1));
    }

    @Test
    public void testOfWithRange_nextCharAfterExhausted() {
        char[] chars = { 'a', 'b' };
        CharIterator iter = CharIterator.of(chars, 0, 1);
        iter.nextChar();
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    // ==================== defer(Supplier) ====================

    @Test
    public void testDefer() {
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
    public void testDefer_initOnNextChar() {
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
    public void testDefer_withEmptyIterator() {
        CharIterator iter = CharIterator.defer(() -> CharIterator.empty());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.defer(null));
    }

    // ==================== generate(CharSupplier) ====================

    @Test
    public void testGenerate() {
        CharIterator iter = CharIterator.generate(() -> 'X');
        assertTrue(iter.hasNext());
        assertEquals('X', iter.nextChar());
        assertTrue(iter.hasNext());
        assertEquals('X', iter.nextChar());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_withCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        CharIterator iter = CharIterator.generate(() -> (char) ('A' + counter.getAndIncrement()));
        assertEquals('A', iter.nextChar());
        assertEquals('B', iter.nextChar());
        assertEquals('C', iter.nextChar());
    }

    // ==================== generate(BooleanSupplier, CharSupplier) ====================

    @Test
    public void testGenerate_conditional() {
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
    public void testGenerate_conditional_multipleHasNextCalls() {
        AtomicInteger count = new AtomicInteger(0);
        CharIterator iter = CharIterator.generate(() -> count.get() < 2, () -> (char) ('A' + count.getAndIncrement()));
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals('A', iter.nextChar());
        assertTrue(iter.hasNext());
        assertEquals('B', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate((CharSupplier) null));
    }

    @Test
    public void testGenerate_conditional_nullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate(null, () -> 'x'));
    }

    @Test
    public void testGenerate_conditional_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.generate(() -> true, null));
    }

    @Test
    public void testGenerate_conditional_nextCharWhenNoElements() {
        CharIterator iter = CharIterator.generate(() -> false, () -> 'x');
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    // ==================== next() (deprecated boxed) ====================

    @Test
    public void testNext() {
        CharIterator iter = CharIterator.of('a', 'b');
        Character ch = iter.next();
        assertEquals(Character.valueOf('a'), ch);
        assertEquals(Character.valueOf('b'), iter.next());
    }

    // ==================== nextChar() ====================

    @Test
    public void testNextChar() {
        CharIterator iter = CharIterator.of('x', 'y', 'z');
        assertEquals('x', iter.nextChar());
        assertEquals('y', iter.nextChar());
        assertEquals('z', iter.nextChar());
    }

    @Test
    public void testNextChar_noSuchElement() {
        CharIterator iter = CharIterator.of('a');
        iter.nextChar();
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    // ==================== skip(long) ====================

    @Test
    public void testSkip() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd', 'e').skip(2);
        assertEquals('c', iter.nextChar());
        assertEquals('d', iter.nextChar());
        assertEquals('e', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_moreThanAvailable() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').skip(10);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_all() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').skip(3);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_one() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').skip(1);
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_zero() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').skip(0);
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
    }

    @Test
    public void testSkip_emptyIterator() {
        CharIterator iter = CharIterator.empty().skip(5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_returnsSameIfZero() {
        CharIterator original = CharIterator.of('a', 'b');
        CharIterator skipped = original.skip(0);
        assertSame(original, skipped);
    }

    @Test
    public void testSkip_negative() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a', 'b', 'c').skip(-1));
    }

    @Test
    public void testSkip_nextCharAfterExhausted() {
        CharIterator iter = CharIterator.of('a', 'b').skip(2);
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    public void testSkip_lazyEvaluation() {
        AtomicInteger consumed = new AtomicInteger(0);
        CharIterator base = new CharIterator() {
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < 5;
            }

            @Override
            public char nextChar() {
                if (idx >= 5) {
                    throw new NoSuchElementException();
                }
                consumed.incrementAndGet();
                return (char) ('a' + idx++);
            }
        };

        CharIterator iter = base.skip(2);
        // skip is lazy, not consumed yet
        assertEquals(0, consumed.get());
        // hasNext triggers the skip
        assertTrue(iter.hasNext());
        assertEquals(2, consumed.get());
        assertEquals('c', iter.nextChar());
    }

    // ==================== limit(long) ====================

    @Test
    public void testLimit() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd', 'e').limit(3);
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_moreThanAvailable() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').limit(10);
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_one() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').limit(1);
        assertEquals('a', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_zero() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').limit(0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_emptyIterator() {
        CharIterator iter = CharIterator.empty().limit(5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_returnsEmptyIfZero() {
        CharIterator iter = CharIterator.of('a', 'b').limit(0);
        assertSame(CharIterator.EMPTY, iter);
    }

    @Test
    public void testLimit_negative() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a', 'b', 'c').limit(-1));
    }

    @Test
    public void testLimit_nextCharAfterExhausted() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').limit(1);
        iter.nextChar();
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    // ==================== filter(CharPredicate) ====================

    @Test
    public void testFilter() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd', 'e').filter(ch -> ch >= 'c');
        assertEquals('c', iter.nextChar());
        assertEquals('d', iter.nextChar());
        assertEquals('e', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_allMatch() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').filter(ch -> ch >= 'a');
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_everyOtherElement() {
        AtomicInteger idx = new AtomicInteger(0);
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd', 'e', 'f').filter(ch -> idx.getAndIncrement() % 2 == 0);
        assertEquals('a', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertEquals('e', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_vowels() {
        CharIterator iter = CharIterator.of('h', 'e', 'l', 'l', 'o').filter(ch -> "aeiou".indexOf(ch) >= 0);
        assertEquals('e', iter.nextChar());
        assertEquals('o', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_noneMatch() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').filter(ch -> ch > 'z');
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_emptyIterator() {
        CharIterator iter = CharIterator.empty().filter(ch -> true);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_multipleHasNextCalls() {
        CharIterator iter = CharIterator.of('a', 'b', 'c').filter(ch -> ch == 'b');
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals('b', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_nullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> CharIterator.of('a').filter(null));
    }

    @Test
    public void testFilter_nextCharWithoutHasNext() {
        CharIterator iter = CharIterator.of('x', 'y', 'z').filter(ch -> ch == 'y');
        assertEquals('y', iter.nextChar());
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    // ==================== toArray() ====================

    @Test
    public void testToArray() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        char[] result = iter.toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray_partiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd');
        iter.nextChar();
        iter.nextChar();
        char[] result = iter.toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
    }

    @Test
    public void testToArray_empty() {
        CharIterator iter = CharIterator.empty();
        char[] result = iter.toArray();
        assertEquals(0, result.length);
    }

    // ==================== toList() ====================

    @Test
    public void testToList() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        CharList result = iter.toList();
        assertEquals(CharList.of('a', 'b', 'c'), result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToList_partiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd');
        iter.nextChar();
        CharList result = iter.toList();
        assertEquals(CharList.of('b', 'c', 'd'), result);
    }

    @Test
    public void testToList_empty() {
        CharIterator iter = CharIterator.empty();
        CharList result = iter.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStream_count() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        long count = iter.stream().count();
        assertEquals(3, count);
    }

    @Test
    public void testStream_map() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        char[] result = iter.stream().map(ch -> (char) (ch + 1)).toArray();
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
    }

    @Test
    public void testStream_filter() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd', 'e');
        char[] result = iter.stream().filter(ch -> ch < 'd').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    // ==================== stream() ====================

    @Test
    public void testStream() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        CharStream stream = iter.stream();
        assertNotNull(stream);
        char[] result = stream.toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testStream_empty() {
        CharIterator iter = CharIterator.empty();
        CharStream stream = iter.stream();
        char[] result = stream.toArray();
        assertEquals(0, result.length);
    }

    // ==================== indexed() ====================

    @Test
    public void testIndexed() {
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

    // ==================== indexed(long) ====================

    @Test
    public void testIndexed_withStartIndex() {
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
    public void testIndexed_empty() {
        CharIterator iter = CharIterator.empty();
        ObjIterator<IndexedChar> indexed = iter.indexed();
        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_withStartIndex_zero() {
        CharIterator iter = CharIterator.of('x');
        ObjIterator<IndexedChar> indexed = iter.indexed(0);
        IndexedChar ic = indexed.next();
        assertEquals(0, ic.index());
        assertEquals('x', ic.value());
    }

    @Test
    public void testIndexed_withStartIndex_negative() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    // ==================== forEachRemaining(Consumer) (deprecated) ====================

    @Test
    public void testForEachRemaining_deprecated() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        iter.forEachRemaining((Character ch) -> sb.append(ch));
        assertEquals("abc", sb.toString());
    }

    // ==================== foreachRemaining(CharConsumer) ====================

    @Test
    public void testForeachRemaining() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        iter.foreachRemaining(ch -> sb.append(ch));
        assertEquals("abc", sb.toString());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachRemaining_partiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd');
        iter.nextChar();
        iter.nextChar();
        StringBuilder sb = new StringBuilder();
        iter.foreachRemaining(ch -> sb.append(ch));
        assertEquals("cd", sb.toString());
    }

    @Test
    public void testForeachRemaining_empty() {
        CharIterator iter = CharIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachRemaining(ch -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForeachRemaining_nullAction() {
        CharIterator iter = CharIterator.of('a');
        assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining((Throwables.CharConsumer<RuntimeException>) null));
    }

    @Test
    public void testForEachRemaining() {
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
                return (char) ('X' + count++);
            }
        };

        assertEquals('X', iter.nextChar());
        assertEquals('Y', iter.nextChar());
        assertEquals('Z', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    // ==================== foreachIndexed(IntCharConsumer) ====================

    @Test
    public void testForeachIndexed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        StringBuilder sb = new StringBuilder();
        iter.foreachIndexed((idx, ch) -> sb.append(idx).append(':').append(ch).append(' '));
        assertEquals("0:a 1:b 2:c ", sb.toString());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachIndexed_partiallyConsumed() {
        CharIterator iter = CharIterator.of('a', 'b', 'c', 'd');
        iter.nextChar();
        StringBuilder sb = new StringBuilder();
        iter.foreachIndexed((idx, ch) -> sb.append(idx).append(':').append(ch).append(' '));
        assertEquals("0:b 1:c 2:d ", sb.toString());
    }

    @Test
    public void testForeachIndexed_verifyIndices() {
        CharIterator iter = CharIterator.of('x', 'y', 'z');
        AtomicInteger expectedIndex = new AtomicInteger(0);
        iter.foreachIndexed((idx, ch) -> {
            assertEquals(expectedIndex.getAndIncrement(), idx);
        });
        assertEquals(3, expectedIndex.get());
    }

    @Test
    public void testForeachIndexed_empty() {
        CharIterator iter = CharIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachIndexed((idx, ch) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForeachIndexed_nullAction() {
        CharIterator iter = CharIterator.of('a');
        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

}
