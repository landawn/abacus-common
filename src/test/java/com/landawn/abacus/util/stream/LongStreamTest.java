package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.LongIterator;

/** LongStream general contracts. */
public class LongStreamTest extends LongStreamTestSupport {

    @Test
    public void testChainedOperations() {
        long result = LongStream.range(1L, 101L).filter(n -> n % 2 == 0).map(n -> n * 2).limit(10).sum();

        assertEquals(220L, result);
    }

    @Test
    public void testDefaultIfEmpty() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, createLongStream(1L, 2L, 3L).defaultIfEmpty(() -> createLongStream(10L, 20L)).toArray());
        assertArrayEquals(new long[] { 10L, 20L }, LongStream.empty().defaultIfEmpty(() -> createLongStream(10L, 20L)).toArray());
    }

    @Test
    public void testNullHandlingInFilter() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertEquals(3, stream.filter(n -> n != 0).count());
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);
        LongStream.empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());

        called.set(false);
        LongStream.of(1L).ifEmpty(() -> called.set(true)).count();
        assertFalse(called.get());
    }

    @Test
    public void testIsParallel() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertFalse(stream.isParallel());
    }

    @Test
    public void testIsParallel_HappyPath2() {
        assertFalse(LongStream.of(1, 2, 3).isParallel());
        assertTrue(LongStream.of(1, 2, 3).parallel().isParallel());
    }

    // iterator

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        LongIterator iter = LongStream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> 1L).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = LongStream.iterate(1L, () -> conditionCalls.getAndIncrement() > 0, value -> value + 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertEquals(1, conditionCalls.get());
    }

    @Test
    public void testIterator() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        LongIterator iter = stream.iterator();

        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testNextOnExhaustedIterator() {
        LongStream s = LongStream.of(1);
        LongIterator it = s.iterator();
        it.nextLong();
        assertThrows(NoSuchElementException.class, it::nextLong);
    }

    @Test
    public void testLargeStream() {
        LongStream stream = LongStream.range(0L, 10000L);
        assertEquals(10000, stream.count());
    }

    @Test
    public void testInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).limit(-1));
    }

    @Test
    public void testMultipleTerminalOpsThrowsException() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.sum();

        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            LongStream.of(1L, 2L, 3L).println();
        });
    }

    @Test
    public void testPsp() {
        long[] result = LongStream.range(1, 100).parallel().psp(s -> s.filter(i -> i > 10).limit(5)).map(i -> i * 10).sorted().toArray();

        assertArrayEquals(new long[] { 110, 120, 130, 140, 150 }, result);
    }

    @Test
    public void testPsp_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).psp(s -> s.filter(x -> x % 2 != 0)).toArray();
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1, 3, 5 }, result);
    }

    @Test
    public void testInvalidSkip() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).skip(-1));
    }

    @Test
    public void testInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).step(0));
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).step(-1));
    }

    @Test
    public void testLargeStreamSum() {
        long sum = LongStream.range(1L, 101L).sum();
        assertEquals(5050L, sum);
    }

    // throwIfEmpty

    @Test
    public void testThrowIfEmpty() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertNotNull(stream.throwIfEmpty());

        assertThrows(NoSuchElementException.class, () -> LongStream.empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmpty_EmptyThrows() {
        assertThrows(java.util.NoSuchElementException.class, () -> LongStream.empty().throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        long[] result = LongStream.of(1, 2, 3).throwIfEmpty().toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
    }

    @Test
    public void testThrowIfEmptyWithCustomException() {
        assertThrows(IllegalStateException.class, () -> LongStream.empty().throwIfEmpty(() -> new IllegalStateException("Stream is empty")).count());
    }

}
