package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;

public class ArrayLongStreamTest extends TestBase {

    private LongStream createStream(long... elements) {
        return LongStream.of(elements);
    }

    @Test
    public void testToJdkStreamCloseRunsSourceHandlersOnce() {
        final java.util.concurrent.atomic.AtomicInteger closeCount = new java.util.concurrent.atomic.AtomicInteger();
        final LongStream source = createStream(1L, 2L, 3L).onClose(closeCount::incrementAndGet);

        source.toJdkStream().close();
        source.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testFilter() {
        long[] result = createStream(1L, 2L, 3L, 4L, 5L).filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new long[] { 2L, 4L }, result);
    }

    @Test
    public void testStep() {
        long[] result = createStream(1L, 2L, 3L, 4L, 5L, 6L).step(2).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 5L }, result);
    }

    @Test
    public void testStepIteratorCountConsumes() {
        LongIterator iter = createStream(1L, 2L, 3L, 4L, 5L, 6L).step(2).iterator();
        assertEquals(3, iter.count());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStep_One() {
        long[] result = createStream(1L, 2L, 3L).step(1).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testStep_Three() {
        long[] result = createStream(1L, 2L, 3L, 4L, 5L, 6L, 7L).step(3).toArray();
        assertArrayEquals(new long[] { 1L, 4L, 7L }, result);
    }

    @Test
    public void testMap() {
        long[] result = createStream(1L, 2L, 3L).map(n -> n * 2).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testFlatmap() {
        long[] result = createStream(1L, 2L, 3L).flatMapArray(n -> new long[] { n, n * 10 }).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testFlatmap_EmptyResult() {
        long[] result = createStream(1L, 2L, 3L).flatMapArray(n -> new long[0]).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testDistinct() {
        long[] result = createStream(1L, 2L, 2L, 3L, 3L, 3L).distinct().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testLimit() {
        long[] result = createStream(1L, 2L, 3L, 4L, 5L).limit(3).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testLimit_ExceedsSize() {
        long[] result = createStream(1L, 2L, 3L).limit(100).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testLimit_Zero() {
        long[] result = createStream(1L, 2L, 3L).limit(0).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testSkip() {
        long[] result = createStream(1L, 2L, 3L, 4L, 5L).skip(2).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testSkip_All() {
        long[] result = createStream(1L, 2L, 3L).skip(10).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testSkip_Zero() {
        long[] result = createStream(1L, 2L, 3L).skip(0).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testSkip_Negative() {
        assertThrows(IllegalArgumentException.class, () -> createStream(1L, 2L, 3L).skip(-1).toArray());
    }

    @Test
    public void testTop() {
        long[] result = createStream(5L, 2L, 8L, 1L, 9L, 3L).top(3).toArray();
        assertEquals(3, result.length);
        // Verify top 3 contains the largest values
        long[] sorted = result.clone();
        Arrays.sort(sorted);
        assertEquals(5L, sorted[0]);
        assertEquals(8L, sorted[1]);
        assertEquals(9L, sorted[2]);
    }

    @Test
    public void testTopWithComparator() {
        long[] result = createStream(5L, 2L, 8L, 1L, 9L, 3L).top(3, Comparator.naturalOrder()).toArray();
        assertEquals(3, result.length);
        long[] sorted = result.clone();
        Arrays.sort(sorted);
        assertEquals(5L, sorted[0]);
        assertEquals(8L, sorted[1]);
        assertEquals(9L, sorted[2]);
    }

    @Test
    public void testToArray() {
        long[] result = createStream(3L, 1L, 2L).toArray();
        assertArrayEquals(new long[] { 3L, 1L, 2L }, result);
    }

    @Test
    public void testSorted() {
        long[] result = createStream(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L).sorted().toArray();
        assertArrayEquals(new long[] { 1L, 1L, 2L, 3L, 4L, 5L, 6L, 9L }, result);
    }

    @Test
    public void testToArray_Empty() {
        long[] result = createStream().toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testMin() {
        OptionalLong min = createStream(5L, 2L, 8L, 1L, 9L).min();
        assertTrue(min.isPresent());
        assertEquals(1L, min.getAsLong());
    }

    @Test
    public void testMin_SingleElement() {
        OptionalLong min = createStream(42L).min();
        assertTrue(min.isPresent());
        assertEquals(42L, min.getAsLong());
    }

    @Test
    public void testMin_Empty() {
        OptionalLong min = createStream().min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMax() {
        OptionalLong max = createStream(5L, 2L, 8L, 1L, 9L).max();
        assertTrue(max.isPresent());
        assertEquals(9L, max.getAsLong());
    }

    @Test
    public void testMax_SingleElement() {
        OptionalLong max = createStream(42L).max();
        assertTrue(max.isPresent());
        assertEquals(42L, max.getAsLong());
    }

    @Test
    public void testMax_Empty() {
        OptionalLong max = createStream().max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testKthLargest() {
        com.landawn.abacus.util.u.OptionalLong result = createStream(5L, 2L, 8L, 1L, 9L, 3L).kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals(9L, result.get());
    }

    @Test
    public void testKthLargest_Second() {
        com.landawn.abacus.util.u.OptionalLong result = createStream(5L, 2L, 8L, 1L, 9L, 3L).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(8L, result.get());
    }

    @Test
    public void testKthLargest_Empty() {
        com.landawn.abacus.util.u.OptionalLong result = createStream().kthLargest(1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testSum() {
        assertEquals(15L, createStream(1L, 2L, 3L, 4L, 5L).sum());
    }

    @Test
    public void testAverageDoesNotOverflowItsRunningSum() {
        OptionalDouble average = createStream(Long.MAX_VALUE, Long.MAX_VALUE).average();

        assertTrue(average.isPresent());
        assertEquals((double) Long.MAX_VALUE, average.getAsDouble());
    }

    @Test
    public void testCount() {
        assertEquals(5L, createStream(1L, 2L, 3L, 4L, 5L).count());
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.LongStream jdkStream = createStream(1L, 2L, 3L, 4L, 5L).toJdkStream();
        assertNotNull(jdkStream);
        assertEquals(15L, jdkStream.sum());
    }

    @Test
    public void testToJdkStream_Empty() {
        java.util.stream.LongStream jdkStream = createStream().toJdkStream();
        assertNotNull(jdkStream);
        assertEquals(0L, jdkStream.count());
    }

    @Test
    public void testCycled() {
        List<Long> result = createStream(1L, 2L, 3L).cycled(2).boxed().toList();
        assertEquals(6, result.size());
        assertEquals(Arrays.asList(1L, 2L, 3L, 1L, 2L, 3L), result);
    }

    @Test
    public void testCycled_One() {
        List<Long> result = createStream(1L, 2L, 3L).cycled(1).boxed().toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testCycled_Zero() {
        List<Long> result = createStream(1L, 2L, 3L).cycled(0).boxed().toList();
        assertEquals(0, result.size());
    }
}
