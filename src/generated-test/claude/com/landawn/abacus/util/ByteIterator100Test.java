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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteSupplier;
import com.landawn.abacus.util.stream.ByteStream;

public class ByteIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        ByteIterator iter = ByteIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextByte());
    }

    @Test
    public void testOfArray() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        assertTrue(iter.hasNext());
        assertEquals((byte)1, iter.nextByte());
        assertEquals((byte)2, iter.nextByte());
        assertEquals((byte)3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfEmptyArray() {
        ByteIterator iter = ByteIterator.of(new byte[0]);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithRange() {
        byte[] arr = {1, 2, 3, 4, 5};
        ByteIterator iter = ByteIterator.of(arr, 1, 4);
        assertEquals((byte)2, iter.nextByte());
        assertEquals((byte)3, iter.nextByte());
        assertEquals((byte)4, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithInvalidRange() {
        byte[] arr = {1, 2, 3};
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(arr, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteIterator.of(arr, 2, 1));
    }

    @Test
    public void testDefer() {
        boolean[] supplierCalled = {false};
        Supplier<ByteIterator> supplier = () -> {
            supplierCalled[0] = true;
            return ByteIterator.of((byte)1, (byte)2, (byte)3);
        };
        
        ByteIterator iter = ByteIterator.defer(supplier);
        assertFalse(supplierCalled[0]);
        
        assertTrue(iter.hasNext());
        assertTrue(supplierCalled[0]);
        assertEquals((byte)1, iter.nextByte());
    }

    @Test
    public void testDeferWithNull() {
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.defer(null));
    }

    @Test
    public void testGenerate() {
        ByteSupplier supplier = () -> (byte)42;
        ByteIterator iter = ByteIterator.generate(supplier);
        
        assertTrue(iter.hasNext());
        assertEquals((byte)42, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals((byte)42, iter.nextByte());
        // Always returns true for hasNext
    }

    @Test
    public void testGenerateWithNull() {
        assertThrows(IllegalArgumentException.class, () -> ByteIterator.generate(null));
    }

    @Test
    public void testGenerateWithCondition() {
        int[] count = {0};
        BooleanSupplier hasNext = () -> count[0] < 3;
        ByteSupplier supplier = () -> (byte)(count[0]++);
        
        ByteIterator iter = ByteIterator.generate(hasNext, supplier);
        
        assertEquals((byte)0, iter.nextByte());
        assertEquals((byte)1, iter.nextByte());
        assertEquals((byte)2, iter.nextByte());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextByte());
    }

    @Test
    public void testGenerateWithConditionNullParams() {
        assertThrows(IllegalArgumentException.class, 
            () -> ByteIterator.generate(null, () -> (byte)1));
        assertThrows(IllegalArgumentException.class, 
            () -> ByteIterator.generate(() -> true, null));
    }

    @Test
    public void testNext() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2);
        assertEquals(Byte.valueOf((byte)1), iter.next());
        assertEquals(Byte.valueOf((byte)2), iter.next());
    }

    @Test
    public void testSkip() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
        ByteIterator skipped = iter.skip(2);
        
        assertEquals((byte)3, skipped.nextByte());
        assertEquals((byte)4, skipped.nextByte());
        assertEquals((byte)5, skipped.nextByte());
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipZero() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2);
        ByteIterator skipped = iter.skip(0);
        
        assertSame(iter, skipped);
        assertEquals((byte)1, skipped.nextByte());
    }

    @Test
    public void testSkipMoreThanAvailable() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2);
        ByteIterator skipped = iter.skip(10);
        
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipNegative() {
        ByteIterator iter = ByteIterator.of((byte)1);
        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void testLimit() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
        ByteIterator limited = iter.limit(3);
        
        assertEquals((byte)1, limited.nextByte());
        assertEquals((byte)2, limited.nextByte());
        assertEquals((byte)3, limited.nextByte());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitZero() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2);
        ByteIterator limited = iter.limit(0);
        
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitMoreThanAvailable() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2);
        ByteIterator limited = iter.limit(10);
        
        assertEquals((byte)1, limited.nextByte());
        assertEquals((byte)2, limited.nextByte());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitNegative() {
        ByteIterator iter = ByteIterator.of((byte)1);
        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void testFilter() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
        BytePredicate evenFilter = b -> b % 2 == 0;
        ByteIterator filtered = iter.filter(evenFilter);
        
        assertEquals((byte)2, filtered.nextByte());
        assertEquals((byte)4, filtered.nextByte());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterNone() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)3, (byte)5);
        BytePredicate evenFilter = b -> b % 2 == 0;
        ByteIterator filtered = iter.filter(evenFilter);
        
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterAll() {
        ByteIterator iter = ByteIterator.of((byte)2, (byte)4, (byte)6);
        BytePredicate evenFilter = b -> b % 2 == 0;
        ByteIterator filtered = iter.filter(evenFilter);
        
        assertEquals((byte)2, filtered.nextByte());
        assertEquals((byte)4, filtered.nextByte());
        assertEquals((byte)6, filtered.nextByte());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterNull() {
        ByteIterator iter = ByteIterator.of((byte)1);
        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFirst() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        OptionalByte first = iter.first();
        assertTrue(first.isPresent());
        assertEquals((byte)1, first.get());
        // Iterator should still have remaining elements
        assertTrue(iter.hasNext());
        assertEquals((byte)2, iter.nextByte());
    }

    @Test
    public void testFirstEmpty() {
        ByteIterator iter = ByteIterator.empty();
        OptionalByte first = iter.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        OptionalByte last = iter.last();
        assertTrue(last.isPresent());
        assertEquals((byte)3, last.get());
        // Iterator should be exhausted
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLastEmpty() {
        ByteIterator iter = ByteIterator.empty();
        OptionalByte last = iter.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testToArray() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        byte[] array = iter.toArray();
        assertArrayEquals(new byte[]{1, 2, 3}, array);
    }

    @Test
    public void testToArrayEmpty() {
        ByteIterator iter = ByteIterator.empty();
        byte[] array = iter.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToArrayPartiallyConsumed() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        iter.nextByte(); // consume 1
        byte[] array = iter.toArray();
        assertArrayEquals(new byte[]{2, 3}, array);
    }

    @Test
    public void testToList() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        ByteList list = iter.toList();
        assertEquals(3, list.size());
        assertEquals((byte)1, list.get(0));
        assertEquals((byte)2, list.get(1));
        assertEquals((byte)3, list.get(2));
    }

    @Test
    public void testToListEmpty() {
        ByteIterator iter = ByteIterator.empty();
        ByteList list = iter.toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testStream() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        ByteStream stream = iter.stream();
        assertNotNull(stream);
        
        byte[] result = stream.toArray();
        assertArrayEquals(new byte[]{1, 2, 3}, result);
    }

    @Test
    public void testIndexed() {
        ByteIterator iter = ByteIterator.of((byte)10, (byte)20, (byte)30);
        ObjIterator<IndexedByte> indexed = iter.indexed();
        
        IndexedByte ib1 = indexed.next();
        assertEquals(0, ib1.index());
        assertEquals((byte)10, ib1.value());
        
        IndexedByte ib2 = indexed.next();
        assertEquals(1, ib2.index());
        assertEquals((byte)20, ib2.value());
        
        IndexedByte ib3 = indexed.next();
        assertEquals(2, ib3.index());
        assertEquals((byte)30, ib3.value());
        
        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexedWithStartIndex() {
        ByteIterator iter = ByteIterator.of((byte)10, (byte)20, (byte)30);
        ObjIterator<IndexedByte> indexed = iter.indexed(100);
        
        IndexedByte ib1 = indexed.next();
        assertEquals(100, ib1.index());
        assertEquals((byte)10, ib1.value());
        
        IndexedByte ib2 = indexed.next();
        assertEquals(101, ib2.index());
        assertEquals((byte)20, ib2.value());
    }

    @Test
    public void testIndexedWithNegativeStartIndex() {
        ByteIterator iter = ByteIterator.of((byte)1);
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testForEachRemaining() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        int[] sum = {0};
        
        iter.forEachRemaining(b -> sum[0] += b);
        
        assertEquals(6, sum[0]);
    }

    @Test
    public void testForeachRemaining() {
        ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
        int[] sum = {0};
        
        iter.foreachRemaining(b -> sum[0] += b);
        
        assertEquals(6, sum[0]);
    }
}