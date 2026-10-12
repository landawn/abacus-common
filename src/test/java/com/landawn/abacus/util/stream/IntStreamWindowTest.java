package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.N;

/** IntStream window contracts. */
public class IntStreamWindowTest extends IntStreamTestSupport {

    @Test
    public void testSplitByChunkCount_BasicSplit() {
        stream = IntStream.splitByChunkCount(10, 3, (from, to) -> from);
        assertEquals(3, stream.count());
    }

    @Test
    public void testSplitByChunkCount() {
        List<int[]> chunks = Stream.splitByChunkCount(10, 3, (from, to) -> {
            int[] chunk = new int[to - from];
            for (int i = from; i < to; i++) {
                chunk[i - from] = i;
            }
            return chunk;
        }).toList();

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, chunks.get(0));
        assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1));
        assertArrayEquals(new int[] { 7, 8, 9 }, chunks.get(2));
    }

    @Test
    public void testSplitByChunkCount_HappyPath2() {
        int[] result = IntStream.splitByChunkCount(7, 3, (from, to) -> to - from).toArray();
        assertEquals(3, result.length);
        assertEquals(7, IntStream.of(result).sum());
    }

    @Test
    public void testSplitByChunkCount_SizeSmallerFirst() {
        int[] result = IntStream.splitByChunkCount(7, 3, true, (from, to) -> to - from).toArray();
        assertEquals(3, result.length);
        assertEquals(7, IntStream.of(result).sum());
        // Smaller first: first chunk should be smaller or equal
        assertTrue(result[0] <= result[result.length - 1]);
    }

    @Test
    public void test_splitByChunkCount() {
        {
            final int[] a = Array.rangeClosed(1, 7);
            int[] result = IntStream.splitByChunkCount(7, 5, false, (fromIndex, toIndex) -> fromIndex + toIndex).toArray();
            N.println(result);
            assertArrayEquals(new int[] { 2, 6, 9, 11, 13 }, result);
        }
        {
            final int[] a = Array.rangeClosed(1, 7);
            int[] result = IntStream.splitByChunkCount(7, 5, true, (fromIndex, toIndex) -> fromIndex + toIndex).toArray();
            N.println(result);
            assertArrayEquals(new int[] { 1, 3, 5, 8, 12 }, result);
        }
    }

    @Test
    public void testSplitByChunkCount_ZeroTotal() {
        int[] result = IntStream.splitByChunkCount(0, 3, (from, to) -> to - from).toArray();
        assertArrayEquals(new int[0], result);
    }
}
