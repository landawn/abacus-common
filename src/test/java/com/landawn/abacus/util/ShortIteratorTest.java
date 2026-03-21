package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortSupplier;
import com.landawn.abacus.util.stream.ShortStream;

public class ShortIteratorTest extends TestBase {

    // ===================== Additional tests for untested ShortIterator methods =====================

    @Test
    @DisplayName("Test EMPTY constant is same as empty()")
    public void testEmptyConstantIsSingleton() {
        assertTrue(ShortIterator.EMPTY == ShortIterator.empty());
    }

    @Test
    @DisplayName("Test empty() returns iterator with no elements")
    public void testEmpty() {
        ShortIterator iter = ShortIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
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
    @DisplayName("Test empty iterator toArray returns empty array")
    public void testEmpty_ToArray() {
        short[] result = ShortIterator.empty().toArray();
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("Test empty iterator toList returns empty list")
    public void testEmpty_ToList() {
        ShortList result = ShortIterator.empty().toList();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test empty iterator stream is empty")
    public void testEmpty_Stream() {
        ShortStream stream = ShortIterator.empty().stream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("Test empty() iterator throws on nextShort")
    public void testEmptyThrowsOnNext() {
        ShortIterator iter = ShortIterator.empty();
        assertThrows(NoSuchElementException.class, iter::nextShort);
    }

    @Test
    public void testEmptyConstant() {
        ShortIterator iter = ShortIterator.EMPTY;
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
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
    public void testOfArrayWithRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRangeFullArray() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array, 0, array.length);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testCombinedOperations() {
        short[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        ShortIterator iter = ShortIterator.of(array).skip(2).filter(x -> x % 2 == 0).limit(3);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertEquals((short) 6, iter.nextShort());
        Assertions.assertEquals((short) 8, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() range toArray after partial consumption")
    public void testOfRangeToArray_AfterPartialConsumption() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 1, 5);
        iter.nextShort(); // consume 2
        short[] result = iter.toArray();
        assertArrayEquals(new short[] { 3, 4, 5 }, result);
    }

    @Test
    @DisplayName("Test of() range toList after partial consumption")
    public void testOfRangeToList_AfterPartialConsumption() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 0, 3);
        iter.nextShort(); // consume 1
        ShortList result = iter.toList();
        assertEquals(2, result.size());
        assertEquals(2, result.get(0));
        assertEquals(3, result.get(1));
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
    @DisplayName("Test of() with empty range")
    public void testOfEmptyRange() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array, 1, 1);
        assertFalse(iter.hasNext());
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
    public void testOfArrayWithRangeEmptyRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 2, 2);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of() with large array")
    public void testOfLargeArray() {
        short[] input = new short[10000];
        for (int i = 0; i < input.length; i++) {
            input[i] = (short) (i % Short.MAX_VALUE);
        }
        ShortIterator iter = ShortIterator.of(input);
        int count = 0;
        while (iter.hasNext()) {
            iter.nextShort();
            count++;
        }
        assertEquals(10000, count);
    }

    @Test
    @DisplayName("Test of() with null array range 0,0 returns empty")
    public void testOfNullArrayRange() {
        ShortIterator iter = ShortIterator.of(null, 0, 0);
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
    public void testOfArray() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 3, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertEquals((short) 5, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testOfArrayWithRangeInvalidRange() {
        short[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, 0, 4));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ShortIterator.of(array, 2, 1));
    }

    @Test
    @DisplayName("Test of() range exhaustion throws NoSuchElementException")
    public void testOfRange_ExhaustionThrows() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array, 0, 2);
        iter.nextShort();
        iter.nextShort();
        assertThrows(NoSuchElementException.class, iter::nextShort);
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
    @DisplayName("Test defer() nextShort initializes lazily")
    public void testDefer_NextShortInitializes() {
        AtomicInteger callCount = new AtomicInteger(0);
        ShortIterator iter = ShortIterator.defer(() -> {
            callCount.incrementAndGet();
            return ShortIterator.of((short) 10, (short) 20);
        });

        assertEquals(0, callCount.get());
        assertEquals(10, iter.nextShort());
        assertEquals(1, callCount.get());
        assertEquals(20, iter.nextShort());
        assertEquals(1, callCount.get());
    }

    @Test
    @DisplayName("Test defer with generate creates lazy infinite iterator")
    public void testDeferWithGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortIterator iter = ShortIterator.defer(() -> ShortIterator.generate(() -> (short) counter.incrementAndGet()));
        assertEquals(0, counter.get());
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextShort());
        assertEquals(2, iter.nextShort());
    }

    @Test
    @DisplayName("Test defer with empty iterator")
    public void testDeferEmpty() {
        ShortIterator iter = ShortIterator.defer(ShortIterator::empty);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test defer() with null supplier throws exception")
    public void testDeferNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.defer(null));
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
    public void testGenerate() {
        int[] counter = { 0 };
        ShortSupplier supplier = () -> (short) counter[0]++;

        ShortIterator iter = ShortIterator.generate(supplier);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 0, iter.nextShort());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertTrue(iter.hasNext());
    }

    @Test
    @DisplayName("Test generate() infinite iterator always has next")
    public void testGenerate_AlwaysHasNext() {
        ShortIterator iter = ShortIterator.generate(() -> (short) 0);
        for (int i = 0; i < 100; i++) {
            assertTrue(iter.hasNext());
            assertEquals(0, iter.nextShort());
        }
    }

    @Test
    @DisplayName("Test generate with limit and toArray")
    public void testGenerateLimitToArray() {
        AtomicInteger counter = new AtomicInteger(0);
        short[] result = ShortIterator.generate(() -> (short) counter.incrementAndGet()).limit(5).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    @DisplayName("Test generate with limit and toList")
    public void testGenerateLimitToList() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortList result = ShortIterator.generate(() -> (short) counter.incrementAndGet()).limit(3).toList();
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(3, result.get(2));
    }

    @Test
    @DisplayName("Test generate() with single element supplier")
    public void testGenerateConditional_SingleElement() {
        int[] counter = { 0 };
        ShortIterator iter = ShortIterator.generate(() -> counter[0] < 1, () -> {
            counter[0]++;
            return (short) 99;
        });
        assertTrue(iter.hasNext());
        assertEquals(99, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test generate() with null supplier throws exception")
    public void testGenerateNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(null));
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
    public void testGenerateWithCondition() {
        int[] counter = { 0 };
        BooleanSupplier hasNext = () -> counter[0] < 3;
        ShortSupplier supplier = () -> (short) counter[0]++;

        ShortIterator iter = ShortIterator.generate(hasNext, supplier);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 0, iter.nextShort());
        Assertions.assertEquals((short) 1, iter.nextShort());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    public void testGenerateWithConditionNullArgs() {
        BooleanSupplier hasNext = () -> true;
        ShortSupplier supplier = () -> (short) 1;

        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(null, supplier));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(hasNext, null));
    }

    @Test
    @DisplayName("Test generate(BooleanSupplier, ShortSupplier) with null supplier throws")
    public void testGenerateConditional_NullArgs() {
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(null, () -> (short) 1));
        assertThrows(IllegalArgumentException.class, () -> ShortIterator.generate(() -> true, null));
    }

    @Test
    @DisplayName("Test generate conditional with immediate false")
    public void testGenerateConditional_ImmediateFalse() {
        ShortIterator iter = ShortIterator.generate(() -> false, () -> (short) 42);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);
    }

    @Test
    @DisplayName("Test next() returns boxed Short")
    public void testNext() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        Short value = iter.next();
        assertEquals(Short.valueOf((short) 1), value);
    }

    @Test
    @DisplayName("Test next() returns boxed Short correctly for boundary values")
    public void testNext_BoundaryValues() {
        ShortIterator iter = ShortIterator.of(Short.MIN_VALUE, Short.MAX_VALUE);
        @SuppressWarnings("deprecation")
        Short val1 = iter.next();
        assertEquals(Short.MIN_VALUE, val1.shortValue());
        @SuppressWarnings("deprecation")
        Short val2 = iter.next();
        assertEquals(Short.MAX_VALUE, val2.shortValue());
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
    @DisplayName("Test nextShort() advances iterator")
    public void testNextShortAdvances() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.nextShort();
        assertEquals(2, iter.nextShort());
    }

    @Test
    @DisplayName("Test nextShort() throws NoSuchElementException when exhausted")
    public void testNextShort_Exhausted() {
        ShortIterator iter = ShortIterator.of((short) 1);
        iter.nextShort();
        assertThrows(NoSuchElementException.class, iter::nextShort);
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
    @DisplayName("Test skip() more than available")
    public void testSkipMoreThanAvailable() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator skipped = iter.skip(10);
        assertFalse(skipped.hasNext());
    }

    @Test
    @DisplayName("Test skip() lazy evaluation")
    public void testSkipLazy() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortIterator skipped = iter.skip(2);
        assertEquals(3, skipped.nextShort());
    }

    @Test
    @DisplayName("Test skip all elements")
    public void testSkipAll() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator result = iter.skip(5);
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Test skip() with exact element count")
    public void testSkip_ExactCount() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator skipped = iter.skip(3);
        assertFalse(skipped.hasNext());
    }

    @Test
    @DisplayName("Test skip and toArray")
    public void testSkipAndToArray() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4);
        short[] result = iter.skip(2).toArray();
        assertArrayEquals(new short[] { 3, 4 }, result);
    }

    @Test
    @DisplayName("Test skip() with zero returns same iterator")
    public void testSkipZero() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator skipped = iter.skip(0);
        assertTrue(skipped == iter);
    }

    @Test
    @DisplayName("Test skip on empty iterator returns empty")
    public void testSkip_OnEmpty() {
        ShortIterator iter = ShortIterator.empty().skip(5);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test skip() with negative value throws exception")
    public void testSkipNegative() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    @DisplayName("Test skip() then nextShort throws when no elements left")
    public void testSkip_ThenNextShortThrows() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator skipped = iter.skip(5);
        assertThrows(NoSuchElementException.class, skipped::nextShort);
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
    @DisplayName("Test limit() more than available")
    public void testLimitMoreThanAvailable() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator limited = iter.limit(10);
        assertEquals(1, limited.nextShort());
        assertEquals(2, limited.nextShort());
        assertFalse(limited.hasNext());
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
    @DisplayName("Test limit(1) returns only first element")
    public void testLimit_One() {
        ShortIterator iter = ShortIterator.of((short) 10, (short) 20, (short) 30);
        ShortIterator limited = iter.limit(1);
        assertEquals(10, limited.nextShort());
        assertFalse(limited.hasNext());
    }

    @Test
    @DisplayName("Test limit and toArray")
    public void testLimitAndToArray() {
        ShortIterator iter = ShortIterator.of((short) 10, (short) 20, (short) 30);
        short[] result = iter.limit(2).toArray();
        assertArrayEquals(new short[] { 10, 20 }, result);
    }

    @Test
    @DisplayName("Test limit() with zero returns empty")
    public void testLimitZero() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2);
        ShortIterator limited = iter.limit(0);
        assertFalse(limited.hasNext());
    }

    @Test
    @DisplayName("Test limit on empty iterator returns empty")
    public void testLimit_OnEmpty() {
        ShortIterator iter = ShortIterator.empty().limit(5);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test limit() with negative value throws exception")
    public void testLimitNegative() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    @DisplayName("Test limit() nextShort throws NoSuchElementException when limit reached")
    public void testLimit_NextShortThrowsWhenLimitReached() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator limited = iter.limit(1);
        limited.nextShort();
        assertThrows(NoSuchElementException.class, limited::nextShort);
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
    @DisplayName("Test filter with complex predicate")
    public void testFilterComplexPredicate() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
        ShortIterator result = iter.filter(x -> x > 2 && x < 6 && x % 2 == 0);
        assertEquals(4, result.nextShort());
        assertFalse(result.hasNext());
    }

    @Test
    public void testFilterAllMatch() {
        short[] array = { 2, 4, 6 };
        ShortPredicate evenPredicate = x -> x % 2 == 0;
        ShortIterator iter = ShortIterator.of(array).filter(evenPredicate);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals((short) 2, iter.nextShort());
        Assertions.assertEquals((short) 4, iter.nextShort());
        Assertions.assertEquals((short) 6, iter.nextShort());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test filter and toList")
    public void testFilterAndToList() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortList result = iter.filter(x -> x > 2).toList();
        assertEquals(2, result.size());
        assertEquals(3, result.get(0));
        assertEquals(4, result.get(1));
    }

    @Test
    @DisplayName("Test filter() with single match at end")
    public void testFilter_SingleMatchAtEnd() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 3, (short) 4);
        ShortIterator filtered = iter.filter(x -> x % 2 == 0);
        assertTrue(filtered.hasNext());
        assertEquals(4, filtered.nextShort());
        assertFalse(filtered.hasNext());
    }

    @Test
    @DisplayName("Test filter() with single match at start")
    public void testFilter_SingleMatchAtStart() {
        ShortIterator iter = ShortIterator.of((short) 2, (short) 3, (short) 5);
        ShortIterator filtered = iter.filter(x -> x % 2 == 0);
        assertEquals(2, filtered.nextShort());
        assertFalse(filtered.hasNext());
    }

    @Test
    @DisplayName("Test filter on empty iterator returns empty")
    public void testFilter_OnEmpty() {
        ShortIterator iter = ShortIterator.empty().filter(x -> true);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test filter() with null predicate throws exception")
    public void testFilterNullPredicate() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFilterNoMatch() {
        short[] array = { 1, 3, 5 };
        ShortPredicate evenPredicate = x -> x % 2 == 0;
        ShortIterator iter = ShortIterator.of(array).filter(evenPredicate);

        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextShort());
    }

    @Test
    @DisplayName("Test filter() nextShort throws when no match left")
    public void testFilter_NextShortThrowsWhenNoMatchLeft() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 3, (short) 5);
        ShortIterator filtered = iter.filter(x -> x % 2 == 0);
        assertFalse(filtered.hasNext());
        assertThrows(NoSuchElementException.class, filtered::nextShort);
    }

    @Test
    @DisplayName("Test toArray() converts to array")
    public void testToArray() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        short[] array = iter.toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, array);
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
    public void testToArrayPartiallyConsumed() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        iter.nextShort();
        iter.nextShort();

        short[] result = iter.toArray();
        Assertions.assertArrayEquals(new short[] { 3, 4, 5 }, result);
    }

    @Test
    public void testToArrayImplementation() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 1, 4);

        short[] result = iter.toArray();
        Assertions.assertArrayEquals(new short[] { 2, 3, 4 }, result);
    }

    @Test
    @DisplayName("Test toArray() after partial consumption")
    public void testToArray_AfterPartialConsumption() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4);
        iter.nextShort();
        short[] result = iter.toArray();
        assertArrayEquals(new short[] { 2, 3, 4 }, result);
    }

    @Test
    @DisplayName("Test toArray() on empty iterator")
    public void testToArrayEmpty() {
        ShortIterator iter = ShortIterator.empty();
        short[] array = iter.toArray();
        assertArrayEquals(new short[0], array);
    }

    @Test
    @DisplayName("Test toArray() on single element")
    public void testToArray_SingleElement() {
        ShortIterator iter = ShortIterator.of((short) 42);
        short[] result = iter.toArray();
        assertArrayEquals(new short[] { 42 }, result);
        assertFalse(iter.hasNext());
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
    @DisplayName("Test toList() consumes iterator")
    public void testToListConsumes() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.toList();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToListImplementation() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array, 1, 4);

        ShortList list = iter.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals((short) 2, list.get(0));
        Assertions.assertEquals((short) 3, list.get(1));
        Assertions.assertEquals((short) 4, list.get(2));
    }

    @Test
    @DisplayName("Test toList() after partial consumption")
    public void testToList_AfterPartialConsumption() {
        ShortIterator iter = ShortIterator.of((short) 10, (short) 20, (short) 30);
        iter.nextShort();
        ShortList list = iter.toList();
        assertEquals(2, list.size());
        assertEquals(20, list.get(0));
        assertEquals(30, list.get(1));
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
    @DisplayName("Test toList() on single element")
    public void testToList_SingleElement() {
        ShortIterator iter = ShortIterator.of((short) 42);
        ShortList list = iter.toList();
        assertEquals(1, list.size());
        assertEquals(42, list.get(0));
    }

    @Test
    @DisplayName("Test stream() allows stream operations")
    public void testStreamOperations() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        long sum = iter.stream().filter(x -> x % 2 == 0).sum();
        assertEquals(6, sum);
    }

    @Test
    @DisplayName("Test stream with filter and map")
    public void testStreamComplexOperations() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        long result = iter.stream().filter(x -> x % 2 == 1).map(x -> (short) (x * 2)).sum();
        assertEquals(18, result);
    }

    @Test
    @DisplayName("Test stream() collects all elements")
    public void testStream_Collect() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        short[] result = iter.stream().toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    @DisplayName("Test stream() after partial consumption")
    public void testStream_AfterPartialConsumption() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.nextShort();
        long count = iter.stream().count();
        assertEquals(2, count);
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
    public void testIndexedWithStartIndex() {
        short[] array = { 10, 20, 30 };
        ShortIterator iter = ShortIterator.of(array);

        ObjIterator<IndexedShort> indexed = iter.indexed(100);

        Assertions.assertTrue(indexed.hasNext());
        IndexedShort first = indexed.next();
        Assertions.assertEquals((short) 10, first.value());
        Assertions.assertEquals(100L, first.index());

        IndexedShort second = indexed.next();
        Assertions.assertEquals((short) 20, second.value());
        Assertions.assertEquals(101L, second.index());

        IndexedShort third = indexed.next();
        Assertions.assertEquals((short) 30, third.value());
        Assertions.assertEquals(102L, third.index());

        Assertions.assertFalse(indexed.hasNext());
    }

    @Test
    @DisplayName("Test indexed() on empty iterator")
    public void testIndexedEmpty() {
        ShortIterator iter = ShortIterator.empty();
        ObjIterator<IndexedShort> indexed = iter.indexed();
        assertFalse(indexed.hasNext());
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
    @DisplayName("Test indexed() on single element")
    public void testIndexed_SingleElement() {
        ShortIterator iter = ShortIterator.of((short) 42);
        ObjIterator<IndexedShort> indexed = iter.indexed();
        assertTrue(indexed.hasNext());
        IndexedShort item = indexed.next();
        assertEquals(42, item.value());
        assertEquals(0, item.index());
        assertFalse(indexed.hasNext());
    }

    @Test
    @DisplayName("Test indexed(long) with large start index")
    public void testIndexed_LargeStartIndex() {
        ShortIterator iter = ShortIterator.of((short) 1);
        ObjIterator<IndexedShort> indexed = iter.indexed(Long.MAX_VALUE - 1);
        IndexedShort item = indexed.next();
        assertEquals(1, item.value());
        assertEquals(Long.MAX_VALUE - 1, item.longIndex());
    }

    @Test
    @DisplayName("Test indexed on empty iterator returns empty")
    public void testIndexed_OnEmpty() {
        ObjIterator<IndexedShort> indexed = ShortIterator.empty().indexed();
        assertFalse(indexed.hasNext());
    }

    @Test
    @DisplayName("Test indexed() with negative start throws exception")
    public void testIndexedNegativeStart() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testIndexedWithNegativeStartIndex() {
        short[] array = { 10, 20, 30 };
        ShortIterator iter = ShortIterator.of(array);

        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
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
    public void testForEachRemaining() {
        short[] array = { 1, 2, 3 };
        ShortIterator iter = ShortIterator.of(array);

        short[] sum = { 0 };
        iter.forEachRemaining((Short value) -> sum[0] += value);

        Assertions.assertEquals((short) 6, sum[0]);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachRemainingPartiallyConsumed() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortIterator iter = ShortIterator.of(array);

        iter.nextShort();
        iter.nextShort();

        short[] sum = { 0 };
        iter.foreachRemaining(value -> sum[0] += value);

        Assertions.assertEquals((short) 12, sum[0]);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test forEachRemaining(Consumer) processes boxed elements")
    public void testForEachRemaining_Boxed() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        List<Short> collected = new ArrayList<>();
        iter.forEachRemaining(collected::add);
        assertEquals(3, collected.size());
        assertEquals(Short.valueOf((short) 1), collected.get(0));
        assertEquals(Short.valueOf((short) 2), collected.get(1));
        assertEquals(Short.valueOf((short) 3), collected.get(2));
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
    @DisplayName("Test forEachRemaining(Consumer) on empty iterator")
    public void testForEachRemaining_Empty() {
        ShortIterator iter = ShortIterator.empty();
        List<Short> collected = new ArrayList<>();
        iter.forEachRemaining(collected::add);
        assertTrue(collected.isEmpty());
    }

    @Test
    @DisplayName("Test foreachRemaining with null action throws")
    public void testForeachRemaining_NullAction() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
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
    @DisplayName("Test foreachIndexed() consumes iterator")
    public void testForeachIndexedConsumes() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
        iter.foreachIndexed((idx, val) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test foreachIndexed after partial consumption")
    public void testForeachIndexed_AfterPartialConsumption() {
        ShortIterator iter = ShortIterator.of((short) 10, (short) 20, (short) 30);
        iter.nextShort();
        IntList indices = new IntList();
        ShortList values = new ShortList();
        iter.foreachIndexed((idx, val) -> {
            indices.add(idx);
            values.add(val);
        });
        // Index starts from 0 for the foreachIndexed call, not the original position
        assertEquals(0, indices.get(0));
        assertEquals(1, indices.get(1));
        assertEquals(20, values.get(0));
        assertEquals(30, values.get(1));
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
    @DisplayName("Test foreachIndexed with null action throws")
    public void testForeachIndexed_NullAction() {
        ShortIterator iter = ShortIterator.of((short) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
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
    @DisplayName("Test chaining skip, filter, and limit")
    public void testChainingSkipFilterLimit() {
        ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7, (short) 8, (short) 9, (short) 10);
        ShortIterator result = iter.skip(1).filter(x -> x % 2 == 0).limit(2);
        assertEquals(2, result.nextShort());
        assertEquals(4, result.nextShort());
        assertFalse(result.hasNext());
    }

}
