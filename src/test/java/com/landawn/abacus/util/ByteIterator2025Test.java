package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteSupplier;
import com.landawn.abacus.util.stream.ByteStream;

@Tag("2025")
public class ByteIterator2025Test extends TestBase {

    @Test
    public void testEmpty() {
        ByteIterator iter = ByteIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextByte());
        assertSame(ByteIterator.EMPTY, ByteIterator.empty());
    }

    @Test
    public void testOfArrayBasic() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(iter.hasNext());
        assertEquals((byte) 1, iter.nextByte());
        assertEquals((byte) 2, iter.nextByte());
        assertEquals((byte) 3, iter.nextByte());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextByte());
    }

    @Test
    public void testOfEmptyArray() {
        ByteIterator iter = ByteIterator.of(new byte[0]);
        assertFalse(iter.hasNext());
        assertSame(ByteIterator.EMPTY, iter);
    }

    @Test
    public void testOfNullArray() {
        ByteIterator iter = ByteIterator.of((byte[]) null);
        assertFalse(iter.hasNext());
        assertSame(ByteIterator.EMPTY, iter);
    }

    @Test
    public void testOfSingleElement() {
        ByteIterator iter = ByteIterator.of((byte) 42);
        assertTrue(iter.hasNext());
        assertEquals((byte) 42, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRangeBasic() {
        byte[] arr = { 10, 20, 30, 40, 50 };
        ByteIterator iter = ByteIterator.of(arr, 1, 4);
        assertEquals((byte) 20, iter.nextByte());
        assertEquals((byte) 30, iter.nextByte());
        assertEquals((byte) 40, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRangeFullArray() {
        byte[] arr = { 1, 2, 3 };
        ByteIterator iter = ByteIterator.of(arr, 0, 3);
        assertEquals((byte) 1, iter.nextByte());
        assertEquals((byte) 2, iter.nextByte());
        assertEquals((byte) 3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRangeEmpty() {
        byte[] arr = { 1, 2, 3 };
        ByteIterator iter = ByteIterator.of(arr, 1, 1);
        assertFalse(iter.hasNext());
        assertSame(ByteIterator.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithRangeInvalidBounds() {
        byte[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(arr, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(arr, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(arr, 0, 10));
    }

    @Test
    public void testOfArrayWithRangeNullArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(null, 0, 1));
    }

    @Test
    public void testOfArrayToArrayOptimization() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        ByteIterator iter = ByteIterator.of(arr, 1, 4);
        byte[] result = iter.toArray();
        assertArrayEquals(new byte[] { 2, 3, 4 }, result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayToListOptimization() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        ByteIterator iter = ByteIterator.of(arr, 1, 4);
        ByteList result = iter.toList();
        assertEquals(3, result.size());
        assertEquals((byte) 2, result.get(0));
        assertEquals((byte) 3, result.get(1));
        assertEquals((byte) 4, result.get(2));
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDeferBasic() {
        boolean[] supplierCalled = { false };
        Supplier<ByteIterator> supplier = () -> {
            supplierCalled[0] = true;
            return ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        };

        ByteIterator iter = ByteIterator.defer(supplier);
        assertFalse(supplierCalled[0], "Supplier should not be called until first use");

        assertTrue(iter.hasNext());
        assertTrue(supplierCalled[0], "Supplier should be called on first hasNext");
        assertEquals((byte) 1, iter.nextByte());
        assertEquals((byte) 2, iter.nextByte());
    }

    @Test
    public void testDeferCalledOnNextByte() {
        boolean[] supplierCalled = { false };
        Supplier<ByteIterator> supplier = () -> {
            supplierCalled[0] = true;
            return ByteIterator.of((byte) 42);
        };

        ByteIterator iter = ByteIterator.defer(supplier);
        assertFalse(supplierCalled[0]);

        assertEquals((byte) 42, iter.nextByte());
        assertTrue(supplierCalled[0]);
    }

    @Test
    public void testDeferSupplierCalledOnce() {
        int[] callCount = { 0 };
        Supplier<ByteIterator> supplier = () -> {
            callCount[0]++;
            return ByteIterator.of((byte) 1, (byte) 2);
        };

        ByteIterator iter = ByteIterator.defer(supplier);
        iter.hasNext();
        iter.nextByte();
        iter.hasNext();
        iter.nextByte();

        assertEquals(1, callCount[0], "Supplier should only be called once");
    }

    @Test
    public void testDeferWithNull() {
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.defer(null));
    }

    @Test
    public void testGenerateInfinite() {
        ByteSupplier supplier = () -> (byte) 42;
        ByteIterator iter = ByteIterator.generate(supplier);

        assertTrue(iter.hasNext());
        assertEquals((byte) 42, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals((byte) 42, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals((byte) 42, iter.nextByte());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerateWithCounter() {
        int[] count = { 0 };
        ByteSupplier supplier = () -> (byte) (count[0]++);
        ByteIterator iter = ByteIterator.generate(supplier);

        assertEquals((byte) 0, iter.nextByte());
        assertEquals((byte) 1, iter.nextByte());
        assertEquals((byte) 2, iter.nextByte());
    }

    @Test
    public void testGenerateWithNull() {
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.generate(null));
    }

    @Test
    public void testGenerateWithConditionBasic() {
        int[] count = { 0 };
        BooleanSupplier hasNext = () -> count[0] < 3;
        ByteSupplier supplier = () -> (byte) (count[0]++);

        ByteIterator iter = ByteIterator.generate(hasNext, supplier);

        assertTrue(iter.hasNext());
        assertEquals((byte) 0, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals((byte) 1, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals((byte) 2, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerateWithConditionNoElements() {
        BooleanSupplier hasNext = () -> false;
        ByteSupplier supplier = () -> (byte) 1;

        ByteIterator iter = ByteIterator.generate(hasNext, supplier);

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextByte());
    }

    @Test
    public void testGenerateWithConditionNullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.generate(null, () -> (byte) 1));
    }

    @Test
    public void testGenerateWithConditionNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.generate(() -> true, null));
    }

    @Test
    public void testNext() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2);
        assertEquals(Byte.valueOf((byte) 1), iter.next());
        assertEquals(Byte.valueOf((byte) 2), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testNextByte() {
        ByteIterator iter = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals((byte) 10, iter.nextByte());
        assertEquals((byte) 20, iter.nextByte());
        assertEquals((byte) 30, iter.nextByte());
        assertThrows(NoSuchElementException.class, () -> iter.nextByte());
    }

    @Test
    public void testSkipBasic() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteIterator skipped = iter.skip(2);

        assertTrue(skipped.hasNext());
        assertEquals((byte) 3, skipped.nextByte());
        assertEquals((byte) 4, skipped.nextByte());
        assertEquals((byte) 5, skipped.nextByte());
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipZero() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator skipped = iter.skip(0);

        assertSame(iter, skipped);
        assertEquals((byte) 1, skipped.nextByte());
    }

    @Test
    public void testSkipAll() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator skipped = iter.skip(3);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipMoreThanAvailable() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator skipped = iter.skip(10);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipNegative() {
        ByteIterator iter = ByteIterator.of((byte) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testSkipLazy() {
        int[] count = { 0 };
        ByteSupplier supplier = () -> {
            count[0]++;
            return (byte) count[0];
        };
        ByteIterator iter = ByteIterator.generate(() -> count[0] < 10, supplier);
        ByteIterator skipped = iter.skip(5);

        assertEquals(0, count[0], "Skip should be lazy");
        skipped.hasNext();
        assertEquals(5, count[0], "Should skip 5 elements");
    }

    @Test
    public void testLimitBasic() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteIterator limited = iter.limit(3);

        assertEquals((byte) 1, limited.nextByte());
        assertEquals((byte) 2, limited.nextByte());
        assertEquals((byte) 3, limited.nextByte());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitZero() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator limited = iter.limit(0);

        assertFalse(limited.hasNext());
        assertSame(ByteIterator.EMPTY, limited);
    }

    @Test
    public void testLimitOne() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator limited = iter.limit(1);

        assertTrue(limited.hasNext());
        assertEquals((byte) 1, limited.nextByte());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitMoreThanAvailable() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator limited = iter.limit(10);

        assertEquals((byte) 1, limited.nextByte());
        assertEquals((byte) 2, limited.nextByte());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitNegative() {
        ByteIterator iter = ByteIterator.of((byte) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testLimitExact() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator limited = iter.limit(3);

        assertEquals((byte) 1, limited.nextByte());
        assertEquals((byte) 2, limited.nextByte());
        assertEquals((byte) 3, limited.nextByte());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testFilterBasic() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        BytePredicate evenFilter = b -> b % 2 == 0;
        ByteIterator filtered = iter.filter(evenFilter);

        assertEquals((byte) 2, filtered.nextByte());
        assertEquals((byte) 4, filtered.nextByte());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterNone() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 3, (byte) 5);
        BytePredicate evenFilter = b -> b % 2 == 0;
        ByteIterator filtered = iter.filter(evenFilter);

        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterAll() {
        ByteIterator iter = ByteIterator.of((byte) 2, (byte) 4, (byte) 6);
        BytePredicate evenFilter = b -> b % 2 == 0;
        ByteIterator filtered = iter.filter(evenFilter);

        assertEquals((byte) 2, filtered.nextByte());
        assertEquals((byte) 4, filtered.nextByte());
        assertEquals((byte) 6, filtered.nextByte());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterNull() {
        ByteIterator iter = ByteIterator.of((byte) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFilterLazy() {
        int[] count = { 0 };
        ByteSupplier supplier = () -> {
            count[0]++;
            return (byte) count[0];
        };
        ByteIterator iter = ByteIterator.generate(() -> count[0] < 10, supplier);
        ByteIterator filtered = iter.filter(b -> b > 5);

        assertEquals(0, count[0], "Filter should be lazy");
        filtered.hasNext();
        assertTrue(count[0] >= 6, "Should consume elements until predicate matches");
    }

    @Test
    public void testFirstWithElements() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        OptionalByte first = iter.first();
        assertTrue(first.isPresent());
        assertEquals((byte) 1, first.get());
        assertTrue(iter.hasNext());
        assertEquals((byte) 2, iter.nextByte());
    }

    @Test
    public void testFirstEmpty() {
        ByteIterator iter = ByteIterator.empty();
        OptionalByte first = iter.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testFirstSingleElement() {
        ByteIterator iter = ByteIterator.of((byte) 42);
        OptionalByte first = iter.first();
        assertTrue(first.isPresent());
        assertEquals((byte) 42, first.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLastWithElements() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        OptionalByte last = iter.last();
        assertTrue(last.isPresent());
        assertEquals((byte) 3, last.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLastEmpty() {
        ByteIterator iter = ByteIterator.empty();
        OptionalByte last = iter.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testLastSingleElement() {
        ByteIterator iter = ByteIterator.of((byte) 42);
        OptionalByte last = iter.last();
        assertTrue(last.isPresent());
        assertEquals((byte) 42, last.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayBasic() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        byte[] array = iter.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, array);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayEmpty() {
        ByteIterator iter = ByteIterator.empty();
        byte[] array = iter.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToArrayPartiallyConsumed() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        iter.nextByte();
        iter.nextByte();
        byte[] array = iter.toArray();
        assertArrayEquals(new byte[] { 3, 4 }, array);
    }

    @Test
    public void testToArraySingleElement() {
        ByteIterator iter = ByteIterator.of((byte) 42);
        byte[] array = iter.toArray();
        assertArrayEquals(new byte[] { 42 }, array);
    }

    @Test
    public void testToListBasic() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list = iter.toList();
        assertEquals(3, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 2, list.get(1));
        assertEquals((byte) 3, list.get(2));
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToListEmpty() {
        ByteIterator iter = ByteIterator.empty();
        ByteList list = iter.toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testToListPartiallyConsumed() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        iter.nextByte();
        ByteList list = iter.toList();
        assertEquals(2, list.size());
        assertEquals((byte) 2, list.get(0));
        assertEquals((byte) 3, list.get(1));
    }

    @Test
    public void testStreamBasic() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream = iter.stream();
        assertNotNull(stream);

        byte[] result = stream.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testStreamEmpty() {
        ByteIterator iter = ByteIterator.empty();
        ByteStream stream = iter.stream();
        assertNotNull(stream);

        byte[] result = stream.toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testStreamOperations() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream stream = iter.stream();

        long sum = stream.asIntStream().sum();
        assertEquals(15, sum);
    }

    @Test
    public void testIndexedBasic() {
        ByteIterator iter = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        ObjIterator<IndexedByte> indexed = iter.indexed();

        IndexedByte ib1 = indexed.next();
        assertEquals(0, ib1.index());
        assertEquals((byte) 10, ib1.value());

        IndexedByte ib2 = indexed.next();
        assertEquals(1, ib2.index());
        assertEquals((byte) 20, ib2.value());

        IndexedByte ib3 = indexed.next();
        assertEquals(2, ib3.index());
        assertEquals((byte) 30, ib3.value());

        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedEmpty() {
        ByteIterator iter = ByteIterator.empty();
        ObjIterator<IndexedByte> indexed = iter.indexed();

        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedSingleElement() {
        ByteIterator iter = ByteIterator.of((byte) 42);
        ObjIterator<IndexedByte> indexed = iter.indexed();

        IndexedByte ib = indexed.next();
        assertEquals(0, ib.index());
        assertEquals((byte) 42, ib.value());
        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedWithStartIndexBasic() {
        ByteIterator iter = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        ObjIterator<IndexedByte> indexed = iter.indexed(100);

        IndexedByte ib1 = indexed.next();
        assertEquals(100, ib1.index());
        assertEquals((byte) 10, ib1.value());

        IndexedByte ib2 = indexed.next();
        assertEquals(101, ib2.index());
        assertEquals((byte) 20, ib2.value());

        IndexedByte ib3 = indexed.next();
        assertEquals(102, ib3.index());
        assertEquals((byte) 30, ib3.value());
    }

    @Test
    public void testIndexedWithStartIndexZero() {
        ByteIterator iter = ByteIterator.of((byte) 10, (byte) 20);
        ObjIterator<IndexedByte> indexed = iter.indexed(0);

        IndexedByte ib1 = indexed.next();
        assertEquals(0, ib1.index());
        assertEquals((byte) 10, ib1.value());
    }

    @Test
    public void testIndexedWithStartIndexNegative() {
        ByteIterator iter = ByteIterator.of((byte) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testForEachRemainingDeprecated() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        int[] sum = { 0 };

        iter.forEachRemaining((Byte b) -> sum[0] += b);

        assertEquals(6, sum[0]);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForEachRemainingEmpty() {
        ByteIterator iter = ByteIterator.empty();
        int[] count = { 0 };

        iter.forEachRemaining((Byte b) -> count[0]++);

        assertEquals(0, count[0]);
    }

    @Test
    public void testForeachRemainingBasic() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        int[] sum = { 0 };

        iter.foreachRemaining(b -> sum[0] += b);

        assertEquals(6, sum[0]);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachRemainingEmpty() {
        ByteIterator iter = ByteIterator.empty();
        int[] count = { 0 };

        iter.foreachRemaining(b -> count[0]++);

        assertEquals(0, count[0]);
    }

    @Test
    public void testForeachRemainingPartiallyConsumed() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        iter.nextByte();
        int[] sum = { 0 };

        iter.foreachRemaining(b -> sum[0] += b);

        assertEquals(9, sum[0]);
    }

    @Test
    public void testForeachRemainingNull() {
        ByteIterator iter = ByteIterator.of((byte) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
    }

    @Test
    public void testForeachIndexedBasic() {
        ByteIterator iter = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        int[] indexSum = { 0 };
        int[] valueSum = { 0 };

        iter.foreachIndexed((index, b) -> {
            indexSum[0] += index;
            valueSum[0] += b;
        });

        assertEquals(3, indexSum[0]);
        assertEquals(60, valueSum[0]);
    }

    @Test
    public void testForeachIndexedEmpty() {
        ByteIterator iter = ByteIterator.empty();
        int[] count = { 0 };

        iter.foreachIndexed((index, b) -> count[0]++);

        assertEquals(0, count[0]);
    }

    @Test
    public void testForeachIndexedPartiallyConsumed() {
        ByteIterator iter = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        iter.nextByte();
        int[] firstIndex = { -1 };

        iter.foreachIndexed((index, b) -> {
            if (firstIndex[0] == -1) {
                firstIndex[0] = index;
            }
        });

        assertEquals(0, firstIndex[0], "Index should start from 0 even if iterator partially consumed");
    }

    @Test
    public void testForeachIndexedNull() {
        ByteIterator iter = ByteIterator.of((byte) 1);
        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

    @Test
    public void testChainedOperations() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8)
                .skip(2)
                .filter(b -> b % 2 == 0)
                .limit(2);

        assertEquals((byte) 4, iter.nextByte());
        assertEquals((byte) 6, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMultipleHasNextCalls() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals((byte) 1, iter.nextByte());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals((byte) 2, iter.nextByte());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterHasNextConsistency() {
        ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3).filter(b -> b == 2);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals((byte) 2, iter.nextByte());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }
}
