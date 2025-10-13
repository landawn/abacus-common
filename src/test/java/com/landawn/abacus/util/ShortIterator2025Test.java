package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.stream.ShortStream;

@Tag("2025")
public class ShortIterator2025Test extends TestBase {

    @Test
    @DisplayName("Test empty() returns iterator with no elements")
    public void testEmpty() {
        ShortIterator iter = ShortIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test empty() iterator throws on nextShort")
    public void testEmptyThrowsOnNext() {
        ShortIterator iter = ShortIterator.empty();
        assertThrows(NoSuchElementException.class, iter::nextShort);
    }

    @Test
    @DisplayName("Test empty() returns singleton instance")
    public void testEmptySingleton() {
        ShortIterator iter1 = ShortIterator.empty();
        ShortIterator iter2 = ShortIterator.empty();
        assertTrue(iter1 == iter2);
        assertTrue(iter1 == ShortIterator.EMPTY);
    }

    @Test
    @DisplayName("Test of() with varargs")
    public void testOfVarargs() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextShort());
        assertEquals(2, iter.nextShort());
        assertEquals(3, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() with empty array")
    public void testOfEmptyArray() {
        ShortIterator iter = ShortIterator.of(new short[0]);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() with null array")
    public void testOfNullArray() {
        ShortIterator iter = ShortIterator.of((short[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() with single element")
    public void testOfSingleElement() {
        ShortIterator iter = ShortIterator.of((short) 42);
        assertTrue(iter.hasNext());
        assertEquals(42, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() with negative values")
    public void testOfNegativeValues() {
        ShortIterator iter = ShortIterator.of((short) -1, (short) -2, (short) -3);
        assertEquals(-1, iter.nextShort());
        assertEquals(-2, iter.nextShort());
        assertEquals(-3, iter.nextShort());
    }

    @Test
    @DisplayName("Test of() with MAX and MIN values")
    public void testOfMinMaxValues() {
        ShortIterator iter = ShortIterator.of(Short.MIN_VALUE, Short.MAX_VALUE);
        assertEquals(Short.MIN_VALUE, iter.nextShort());
        assertEquals(Short.MAX_VALUE, iter.nextShort());
    }

    @Test
    @DisplayName("Test of() with range")
    public void testOfRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 1, 4);
        assertEquals(2, iter.nextShort());
        assertEquals(3, iter.nextShort());
        assertEquals(4, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() with full range")
    public void testOfFullRange() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array, 0, 3);
        assertEquals(1, iter.nextShort());
        assertEquals(2, iter.nextShort());
        assertEquals(3, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() with empty range")
    public void testOfEmptyRange() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array, 1, 1);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() with invalid range throws exception")
    public void testOfInvalidRange() {
        short[] array = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, 2, 1));
    }

    @Test
    @DisplayName("Test of() range with null array")
    public void testOfRangeNullArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(null, 0, 1));
    }

    @Test
    @DisplayName("Test defer() lazy initialization")
    public void testDefer() {
        AtomicInteger callCount = new AtomicInteger(0);
        ShortIterator iter = ShortIterator.defer(() -> {
            callCount.incrementAndGet();
            return ShortIterator.of((short) 1, (short) 2, (short) 3);
        });

        assertEquals(0, callCount.get());
        assertTrue(iter.hasNext());
        assertEquals(1, callCount.get());
        assertEquals(1, iter.nextShort());
        assertEquals(1, callCount.get());
    }

    @Test
    @DisplayName("Test defer() with null supplier throws exception")
    public void testDeferNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.defer(null));
    }

    @Test
    @DisplayName("Test defer() initializes only once")
    public void testDeferInitializesOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        ShortIterator iter = ShortIterator.defer(() -> {
            callCount.incrementAndGet();
            return ShortIterator.of((short) 1);
        });

        iter.hasNext();
        iter.hasNext();
        iter.nextShort();
        assertEquals(1, callCount.get());
    }

    @Test
    @DisplayName("Test generate() infinite iterator")
    public void testGenerateInfinite() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortIterator iter = ShortIterator.generate(() -> (short) counter.incrementAndGet());

        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextShort());
        assertTrue(iter.hasNext());
    }

    @Test
    @DisplayName("Test generate() with null supplier throws exception")
    public void testGenerateNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(null));
    }

    @Test
    @DisplayName("Test generate() with constant value")
    public void testGenerateConstant() {
        ShortIterator iter = ShortIterator.generate(() -> (short) 42).limit(3);
        assertEquals(42, iter.nextShort());
        assertEquals(42, iter.nextShort());
        assertEquals(42, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test generate() with hasNext condition")
    public void testGenerateConditional() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortIterator iter = ShortIterator.generate(() -> counter.get() < 3, () -> (short) counter.incrementAndGet());

        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextShort());
        assertEquals(2, iter.nextShort());
        assertEquals(3, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test generate() conditional throws on next when hasNext is false")
    public void testGenerateConditionalThrowsOnNext() {
        ShortIterator iter = ShortIterator.generate(() -> false, () -> (short) 1);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);
    }

    @Test
    @DisplayName("Test generate() with null hasNext throws exception")
    public void testGenerateNullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(null, () -> (short) 1));
    }

    @Test
    @DisplayName("Test generate() with null supplier throws exception")
    public void testGenerateConditionalNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(() -> true, null));
    }

    @Test
    @DisplayName("Test next() returns boxed Short")
    public void testNext() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        Short value = iter.next();
        assertEquals(Short.valueOf((short) 1), value);
    }

    @Test
    @DisplayName("Test next() throws when no elements")
    public void testNextThrows() {
        ShortIterator iter = ShortIterator.empty();
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    @DisplayName("Test nextShort() returns primitive")
    public void testNextShort() {
        ShortIterator iter = ShortIterator.of((short) 42);
        short value = iter.nextShort();
        assertEquals(42, value);
    }

    @Test
    @DisplayName("Test nextShort() throws when no elements")
    public void testNextShortThrows() {
        ShortIterator iter = ShortIterator.empty();
        assertThrows(NoSuchElementException.class, iter::nextShort);
    }

    @Test
    @DisplayName("Test nextShort() advances iterator")
    public void testNextShortAdvances() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.nextShort();
        assertEquals(2, iter.nextShort());
    }

    @Test
    @DisplayName("Test skip() skips n elements")
    public void testSkip() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortIterator skipped = iter.skip(2);
        assertEquals(3, skipped.nextShort());
        assertEquals(4, skipped.nextShort());
        assertEquals(5, skipped.nextShort());
        assertFalse(skipped.hasNext());
    }

    @Test
    @DisplayName("Test skip() with zero returns same iterator")
    public void testSkipZero() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator skipped = iter.skip(0);
        assertTrue(skipped == iter);
    }

    @Test
    @DisplayName("Test skip() more than available")
    public void testSkipMoreThanAvailable() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator skipped = iter.skip(10);
        assertFalse(skipped.hasNext());
    }

    @Test
    @DisplayName("Test skip() with negative value throws exception")
    public void testSkipNegative() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    @DisplayName("Test skip() lazy evaluation")
    public void testSkipLazy() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortIterator skipped = iter.skip(2);
        assertEquals(3, skipped.nextShort());
    }

    @Test
    @DisplayName("Test limit() limits elements")
    public void testLimit() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortIterator limited = iter.limit(3);
        assertEquals(1, limited.nextShort());
        assertEquals(2, limited.nextShort());
        assertEquals(3, limited.nextShort());
        assertFalse(limited.hasNext());
    }

    @Test
    @DisplayName("Test limit() with zero returns empty")
    public void testLimitZero() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator limited = iter.limit(0);
        assertFalse(limited.hasNext());
    }

    @Test
    @DisplayName("Test limit() more than available")
    public void testLimitMoreThanAvailable() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator limited = iter.limit(10);
        assertEquals(1, limited.nextShort());
        assertEquals(2, limited.nextShort());
        assertFalse(limited.hasNext());
    }

    @Test
    @DisplayName("Test limit() with negative value throws exception")
    public void testLimitNegative() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    @DisplayName("Test limit() with infinite iterator")
    public void testLimitInfinite() {
        ShortIterator iter = ShortIterator.generate(() -> (short) 1);
        ShortIterator limited = iter.limit(5);
        int count = 0;
        while (limited.hasNext()) {
            limited.nextShort();
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    @DisplayName("Test filter() filters elements")
    public void testFilter() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortIterator filtered = iter.filter(x -> x % 2 == 0);
        assertEquals(2, filtered.nextShort());
        assertEquals(4, filtered.nextShort());
        assertFalse(filtered.hasNext());
    }

    @Test
    @DisplayName("Test filter() with no matches")
    public void testFilterNoMatches() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 3, (short) 5);
        ShortIterator filtered = iter.filter(x -> x % 2 == 0);
        assertFalse(filtered.hasNext());
    }

    @Test
    @DisplayName("Test filter() with all matches")
    public void testFilterAllMatches() {
        ShortIterator iter = ShortIterator.of((short) 2, (short) 4, (short) 6);
        ShortIterator filtered = iter.filter(x -> x % 2 == 0);
        assertEquals(2, filtered.nextShort());
        assertEquals(4, filtered.nextShort());
        assertEquals(6, filtered.nextShort());
        assertFalse(filtered.hasNext());
    }

    @Test
    @DisplayName("Test filter() with null predicate throws exception")
    public void testFilterNullPredicate() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    @DisplayName("Test filter() lazy evaluation")
    public void testFilterLazy() {
        AtomicInteger count = new AtomicInteger(0);
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortIterator filtered = iter.filter(x -> {
            count.incrementAndGet();
            return x % 2 == 0;
        });
        assertEquals(0, count.get());
        filtered.hasNext();
        assertTrue(count.get() > 0);
    }

    @Test
    @DisplayName("Test first() returns first element")
    public void testFirst() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        OptionalShort first = iter.first();
        assertTrue(first.isPresent());
        assertEquals(1, first.getAsShort());
    }

    @Test
    @DisplayName("Test first() on empty iterator")
    public void testFirstEmpty() {
        ShortIterator iter = ShortIterator.empty();
        OptionalShort first = iter.first();
        assertFalse(first.isPresent());
    }

    @Test
    @DisplayName("Test first() consumes first element")
    public void testFirstConsumes() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.first();
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextShort());
    }

    @Test
    @DisplayName("Test last() returns last element")
    public void testLast() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        OptionalShort last = iter.last();
        assertTrue(last.isPresent());
        assertEquals(3, last.getAsShort());
    }

    @Test
    @DisplayName("Test last() on empty iterator")
    public void testLastEmpty() {
        ShortIterator iter = ShortIterator.empty();
        OptionalShort last = iter.last();
        assertFalse(last.isPresent());
    }

    @Test
    @DisplayName("Test last() consumes all elements")
    public void testLastConsumes() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.last();
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test last() on single element")
    public void testLastSingle() {
        ShortIterator iter = ShortIterator.of((short) 42);
        OptionalShort last = iter.last();
        assertTrue(last.isPresent());
        assertEquals(42, last.getAsShort());
    }

    @Test
    @DisplayName("Test toArray() converts to array")
    public void testToArray() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        short[] array = iter.toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, array);
    }

    @Test
    @DisplayName("Test toArray() on empty iterator")
    public void testToArrayEmpty() {
        ShortIterator iter = ShortIterator.empty();
        short[] array = iter.toArray();
        assertArrayEquals(new short[0], array);
    }

    @Test
    @DisplayName("Test toArray() consumes iterator")
    public void testToArrayConsumes() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.toArray();
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test toArray() with large array")
    public void testToArrayLarge() {
        short[] input = new short[1000];
        for (int i = 0; i < 1000; i++) {
            input[i] = (short) i;
        }
        ShortIterator iter = ShortIterator.of(input);
        short[] result = iter.toArray();
        assertArrayEquals(input, result);
    }

    @Test
    @DisplayName("Test toList() converts to ShortList")
    public void testToList() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortList list = iter.toList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    @DisplayName("Test toList() on empty iterator")
    public void testToListEmpty() {
        ShortIterator iter = ShortIterator.empty();
        ShortList list = iter.toList();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test toList() consumes iterator")
    public void testToListConsumes() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.toList();
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test stream() creates ShortStream")
    public void testStream() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortStream stream = iter.stream();
        assertNotNull(stream);
        long count = stream.count();
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Test stream() on empty iterator")
    public void testStreamEmpty() {
        ShortIterator iter = ShortIterator.empty();
        ShortStream stream = iter.stream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("Test stream() allows stream operations")
    public void testStreamOperations() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        long sum = iter.stream().filter(x -> x % 2 == 0).sum();
        assertEquals(6, sum);
    }

    @Test
    @DisplayName("Test indexed() pairs with indices from 0")
    public void testIndexed() {
        ShortIterator iter = ShortIterator.of((short) 10, (short) 20, (short) 30);
        ObjIterator<IndexedShort> indexed = iter.indexed();

        assertTrue(indexed.hasNext());
        IndexedShort idx0 = indexed.next();
        assertEquals(10, idx0.value());
        assertEquals(0, idx0.index());

        IndexedShort idx1 = indexed.next();
        assertEquals(20, idx1.value());
        assertEquals(1, idx1.index());

        IndexedShort idx2 = indexed.next();
        assertEquals(30, idx2.value());
        assertEquals(2, idx2.index());

        assertFalse(indexed.hasNext());
    }

    @Test
    @DisplayName("Test indexed() on empty iterator")
    public void testIndexedEmpty() {
        ShortIterator iter = ShortIterator.empty();
        ObjIterator<IndexedShort> indexed = iter.indexed();
        assertFalse(indexed.hasNext());
    }

    @Test
    @DisplayName("Test indexed() with custom start index")
    public void testIndexedCustomStart() {
        ShortIterator iter = ShortIterator.of((short) 10, (short) 20, (short) 30);
        ObjIterator<IndexedShort> indexed = iter.indexed(100);

        IndexedShort idx0 = indexed.next();
        assertEquals(10, idx0.value());
        assertEquals(100, idx0.index());

        IndexedShort idx1 = indexed.next();
        assertEquals(20, idx1.value());
        assertEquals(101, idx1.index());
    }

    @Test
    @DisplayName("Test indexed() with negative start throws exception")
    public void testIndexedNegativeStart() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    @DisplayName("Test indexed() with zero start index")
    public void testIndexedZeroStart() {
        ShortIterator iter = ShortIterator.of((short) 5);
        ObjIterator<IndexedShort> indexed = iter.indexed(0);
        IndexedShort idx = indexed.next();
        assertEquals(5, idx.value());
        assertEquals(0, idx.index());
    }

    @Test
    @DisplayName("Test foreachRemaining() processes all elements")
    public void testForeachRemaining() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortList collected = new ShortList();
        iter.foreachRemaining(collected::add);
        assertEquals(3, collected.size());
        assertEquals(1, collected.get(0));
        assertEquals(2, collected.get(1));
        assertEquals(3, collected.get(2));
    }

    @Test
    @DisplayName("Test foreachRemaining() on empty iterator")
    public void testForeachRemainingEmpty() {
        ShortIterator iter = ShortIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachRemaining(x -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    @DisplayName("Test foreachRemaining() processes only remaining")
    public void testForeachRemainingPartial() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4);
        iter.nextShort();
        ShortList collected = new ShortList();
        iter.foreachRemaining(collected::add);
        assertEquals(3, collected.size());
        assertEquals(2, collected.get(0));
    }

    @Test
    @DisplayName("Test foreachRemaining() consumes iterator")
    public void testForeachRemainingConsumes() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.foreachRemaining(x -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test foreachIndexed() processes with indices")
    public void testForeachIndexed() {
        ShortIterator iter = ShortIterator.of((short) 10, (short) 20, (short) 30);
        ShortList values = new ShortList();
        IntList indices = new IntList();

        iter.foreachIndexed((idx, val) -> {
            indices.add(idx);
            values.add(val);
        });

        assertEquals(3, values.size());
        assertEquals(10, values.get(0));
        assertEquals(20, values.get(1));
        assertEquals(30, values.get(2));

        assertEquals(0, indices.get(0));
        assertEquals(1, indices.get(1));
        assertEquals(2, indices.get(2));
    }

    @Test
    @DisplayName("Test foreachIndexed() on empty iterator")
    public void testForeachIndexedEmpty() {
        ShortIterator iter = ShortIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.foreachIndexed((idx, val) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    @DisplayName("Test foreachIndexed() starts from 0")
    public void testForeachIndexedStartsFromZero() {
        ShortIterator iter = ShortIterator.of((short) 5);
        AtomicInteger firstIndex = new AtomicInteger(-1);
        iter.foreachIndexed((idx, val) -> firstIndex.set(idx));
        assertEquals(0, firstIndex.get());
    }

    @Test
    @DisplayName("Test foreachIndexed() consumes iterator")
    public void testForeachIndexedConsumes() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.foreachIndexed((idx, val) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test chaining skip and limit")
    public void testChainingSkipLimit() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
        ShortIterator result = iter.skip(2).limit(3);
        assertEquals(3, result.nextShort());
        assertEquals(4, result.nextShort());
        assertEquals(5, result.nextShort());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Test chaining filter and limit")
    public void testChainingFilterLimit() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
        ShortIterator result = iter.filter(x -> x % 2 == 0).limit(2);
        assertEquals(2, result.nextShort());
        assertEquals(4, result.nextShort());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Test multiple hasNext calls")
    public void testMultipleHasNextCalls() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextShort());
    }

    @Test
    @DisplayName("Test of() range toArray optimization")
    public void testOfRangeToArray() {
        short[] original = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(original, 1, 4);
        short[] result = iter.toArray();
        assertArrayEquals(new short[] { 2, 3, 4 }, result);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() range toList optimization")
    public void testOfRangeToList() {
        short[] original = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(original, 1, 4);
        ShortList result = iter.toList();
        assertEquals(3, result.size());
        assertEquals(2, result.get(0));
        assertEquals(3, result.get(1));
        assertEquals(4, result.get(2));
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test filter with complex predicate")
    public void testFilterComplexPredicate() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
        ShortIterator result = iter.filter(x -> x > 2 && x < 6 && x % 2 == 0);
        assertEquals(4, result.nextShort());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Test zero values")
    public void testZeroValues() {
        ShortIterator iter = ShortIterator.of((short) 0, (short) 0, (short) 0);
        assertEquals(0, iter.nextShort());
        assertEquals(0, iter.nextShort());
        assertEquals(0, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test boundary values in filter")
    public void testBoundaryValuesInFilter() {
        ShortIterator iter = ShortIterator.of(Short.MIN_VALUE, (short) -1, (short) 0, (short) 1, Short.MAX_VALUE);
        ShortIterator positive = iter.filter(x -> x > 0);
        assertEquals(1, positive.nextShort());
        assertEquals(Short.MAX_VALUE, positive.nextShort());
        assertFalse(positive.hasNext());
    }

    @Test
    @DisplayName("Test skip all elements")
    public void testSkipAll() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator result = iter.skip(5);
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Test limit to exact size")
    public void testLimitExactSize() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator result = iter.limit(3);
        assertEquals(1, result.nextShort());
        assertEquals(2, result.nextShort());
        assertEquals(3, result.nextShort());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Test generate with limit and filter")
    public void testGenerateLimitFilter() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortIterator iter = ShortIterator.generate(() -> (short) counter.incrementAndGet());
        ShortIterator result = iter.limit(10).filter(x -> x % 2 == 0);

        assertEquals(2, result.nextShort());
        assertEquals(4, result.nextShort());
        assertEquals(6, result.nextShort());
        assertEquals(8, result.nextShort());
        assertEquals(10, result.nextShort());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Test defer with empty iterator")
    public void testDeferEmpty() {
        ShortIterator iter = ShortIterator.defer(ShortIterator::empty);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test stream with filter and map")
    public void testStreamComplexOperations() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        long result = iter.stream().filter(x -> x % 2 == 1).map(x -> (short) (x * 2)).sum();
        assertEquals(18, result);
    }
}
