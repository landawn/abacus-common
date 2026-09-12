package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.u.OptionalInt;

public class ArrayIntStreamTest extends TestBase {

    private IntStream createStream(int... elements) {
        return IntStream.of(elements);
    }

    @Test
    public void testToJdkStreamCloseRunsSourceHandlersOnce() {
        final java.util.concurrent.atomic.AtomicInteger closeCount = new java.util.concurrent.atomic.AtomicInteger();
        final IntStream source = createStream(1, 2, 3).onClose(closeCount::incrementAndGet);

        source.toJdkStream().close();
        source.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testFilter() {
        int[] result = createStream(1, 2, 3, 4, 5).filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

    @Test
    public void testStep() {
        int[] result = createStream(1, 2, 3, 4, 5, 6).step(2).toArray();
        assertArrayEquals(new int[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStepIteratorCountConsumes() {
        IntIterator iter = createStream(1, 2, 3, 4, 5, 6).step(2).iterator();
        assertEquals(3, iter.count());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStep_One() {
        int[] result = createStream(1, 2, 3).step(1).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testStep_Three() {
        int[] result = createStream(1, 2, 3, 4, 5, 6, 7).step(3).toArray();
        assertArrayEquals(new int[] { 1, 4, 7 }, result);
    }

    @Test
    public void testMap() {
        int[] result = createStream(1, 2, 3).map(n -> n * 2).toArray();
        assertArrayEquals(new int[] { 2, 4, 6 }, result);
    }

    @Test
    public void testFlatmap() {
        int[] result = createStream(1, 2, 3).flatMapArray(n -> new int[] { n, n * 10 }).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatmap_EmptyResult() {
        int[] result = createStream(1, 2, 3).flatMapArray(n -> new int[0]).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testDistinct() {
        int[] result = createStream(1, 2, 2, 3, 3, 3).distinct().toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testLimit() {
        int[] result = createStream(1, 2, 3, 4, 5).limit(3).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testLimit_ExceedsSize() {
        int[] result = createStream(1, 2, 3).limit(100).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testLimit_Zero() {
        int[] result = createStream(1, 2, 3).limit(0).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testLimit_Negative() {
        assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).limit(-1).toArray());
    }

    @Test
    public void testSkip() {
        int[] result = createStream(1, 2, 3, 4, 5).skip(2).toArray();
        assertArrayEquals(new int[] { 3, 4, 5 }, result);
    }

    @Test
    public void testSkip_All() {
        int[] result = createStream(1, 2, 3).skip(10).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testSkip_Zero() {
        int[] result = createStream(1, 2, 3).skip(0).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testSkip_Negative() {
        assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).skip(-1).toArray());
    }

    @Test
    public void testTop() {
        int[] result = createStream(5, 2, 8, 1, 9, 3).top(3).toArray();
        assertEquals(3, result.length);
        int[] sorted = result.clone();
        Arrays.sort(sorted);
        assertEquals(5, sorted[0]);
        assertEquals(8, sorted[1]);
        assertEquals(9, sorted[2]);
    }

    @Test
    public void testTopWithComparator() {
        int[] result = createStream(5, 2, 8, 1, 9, 3).top(3, Comparator.naturalOrder()).toArray();
        assertEquals(3, result.length);
        int[] sorted = result.clone();
        Arrays.sort(sorted);
        assertEquals(5, sorted[0]);
        assertEquals(8, sorted[1]);
        assertEquals(9, sorted[2]);
    }

    @Test
    public void testToArray() {
        int[] result = createStream(3, 1, 2).toArray();
        assertArrayEquals(new int[] { 3, 1, 2 }, result);
    }

    @Test
    public void testSorted() {
        int[] result = createStream(3, 1, 4, 1, 5, 9, 2, 6).sorted().toArray();
        assertArrayEquals(new int[] { 1, 1, 2, 3, 4, 5, 6, 9 }, result);
    }

    @Test
    public void testToArray_Empty() {
        int[] result = createStream().toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testMin() {
        OptionalInt min = createStream(5, 2, 8, 1, 9).min();
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsInt());
    }

    @Test
    public void testMin_SingleElement() {
        OptionalInt min = createStream(42).min();
        assertTrue(min.isPresent());
        assertEquals(42, min.getAsInt());
    }

    @Test
    public void testMin_Empty() {
        OptionalInt min = createStream().min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMax() {
        OptionalInt max = createStream(5, 2, 8, 1, 9).max();
        assertTrue(max.isPresent());
        assertEquals(9, max.getAsInt());
    }

    @Test
    public void testMax_SingleElement() {
        OptionalInt max = createStream(42).max();
        assertTrue(max.isPresent());
        assertEquals(42, max.getAsInt());
    }

    @Test
    public void testMax_Empty() {
        OptionalInt max = createStream().max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testKthLargest() {
        com.landawn.abacus.util.u.OptionalInt result = createStream(5, 2, 8, 1, 9, 3).kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals(9, result.get());
    }

    @Test
    public void testKthLargest_Second() {
        com.landawn.abacus.util.u.OptionalInt result = createStream(5, 2, 8, 1, 9, 3).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(8, result.get());
    }

    @Test
    public void testKthLargest_Empty() {
        com.landawn.abacus.util.u.OptionalInt result = createStream().kthLargest(1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testSum() {
        assertEquals(15, createStream(1, 2, 3, 4, 5).sum());
    }

    @Test
    public void testCount() {
        assertEquals(5L, createStream(1, 2, 3, 4, 5).count());
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.IntStream jdkStream = createStream(1, 2, 3, 4, 5).toJdkStream();
        assertNotNull(jdkStream);
        assertEquals(15, jdkStream.sum());
    }

    @Test
    public void testToJdkStream_Empty() {
        java.util.stream.IntStream jdkStream = createStream().toJdkStream();
        assertNotNull(jdkStream);
        assertEquals(0L, jdkStream.count());
    }

    @Test
    public void testCycled() {
        List<Integer> result = createStream(1, 2, 3).cycled(2).boxed().toList();
        assertEquals(6, result.size());
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
    }

    @Test
    public void testCycled_One() {
        List<Integer> result = createStream(1, 2, 3).cycled(1).boxed().toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCycled_Zero() {
        List<Integer> result = createStream(1, 2, 3).cycled(0).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testPrimitiveMapToObjIteratorTypedArrayNullTerminatesOversizedArray() {
        assertOversizedArrayNullTerminated(ByteStream.of((byte) 1, (byte) 2).mapToObj(e -> "v"));
        assertOversizedArrayNullTerminated(CharStream.of('a', 'b').mapToObj(e -> "v"));
        assertOversizedArrayNullTerminated(ShortStream.of((short) 1, (short) 2).mapToObj(e -> "v"));
        assertOversizedArrayNullTerminated(IntStream.of(1, 2).mapToObj(e -> "v"));
        assertOversizedArrayNullTerminated(LongStream.of(1L, 2L).mapToObj(e -> "v"));
        assertOversizedArrayNullTerminated(FloatStream.of(1.0f, 2.0f).mapToObj(e -> "v"));
        assertOversizedArrayNullTerminated(DoubleStream.of(1.0d, 2.0d).mapToObj(e -> "v"));
    }

    private String[] staleArray() {
        return new String[] { "old0", "old1", "stale" };
    }

    private void assertOversizedArrayNullTerminated(final Stream<String> stream) {
        try (stream) {
            final String[] actual = stream.iterator().toArray(staleArray());

            assertArrayEquals(new String[] { "v", "v", null }, actual);
            assertNull(actual[2]);
        }
    }
}
