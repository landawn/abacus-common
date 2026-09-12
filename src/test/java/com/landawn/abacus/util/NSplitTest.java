package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.IntBiFunction;

public class NSplitTest extends NTestSupport {

    @Test
    public void testSplit_arrays() {
        List<boolean[]> boolChunks = N.split(new boolean[] { true, false, true, false, true, false, true }, 3);
        assertEquals(3, boolChunks.size());
        assertArrayEquals(new boolean[] { true, false, true }, boolChunks.get(0));
        assertArrayEquals(new boolean[] { false, true, false }, boolChunks.get(1));
        assertArrayEquals(new boolean[] { true }, boolChunks.get(2));
        assertTrue(N.split((boolean[]) null, 3).isEmpty());
        assertTrue(N.split(EMPTY_BOOLEAN_ARRAY_CONST, 3).isEmpty());
        assertTrue(N.split(new boolean[] { true, false, true }, 1, 1, 2).isEmpty());
        assertEquals(1, N.split(new boolean[] { true, false }, 5).size());
        assertThrows(IllegalArgumentException.class, () -> N.split(new boolean[] { true }, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.split(new boolean[] { true, false }, -1, 1, 2));

        List<boolean[]> ranged = N.split(new boolean[] { true, false, true, false, true, false, true, false }, 1, 7, 2);
        assertEquals(3, ranged.size());
        assertArrayEquals(new boolean[] { false, true }, ranged.get(0));

        List<char[]> chars = N.split(new char[] { 'a', 'b', 'c', 'd', 'e' }, 2);
        assertEquals(3, chars.size());
        assertArrayEquals(new char[] { 'a', 'b' }, chars.get(0));
        assertArrayEquals(new char[] { 'e' }, chars.get(2));
        assertTrue(N.split((char[]) null, 2).isEmpty());
        List<char[]> charRange = N.split(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, 1, 5, 2);
        assertArrayEquals(new char[] { 'b', 'c' }, charRange.get(0));
        assertThrows(IllegalArgumentException.class, () -> N.split(new char[] { 'a' }, 1, 1, 0));

        List<byte[]> bytes = N.split(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, bytes.get(0));
        assertArrayEquals(new byte[] { 7 }, bytes.get(2));
        assertTrue(N.split((byte[]) null, 2).isEmpty());

        List<short[]> shorts = N.split(new short[] { 10, 20, 30, 40, 50 }, 2);
        assertArrayEquals(new short[] { 10, 20 }, shorts.get(0));
        assertTrue(N.split((short[]) null, 2).isEmpty());

        List<int[]> ints = N.split(new int[] { 1, 2, 3, 4, 5, 6, 7 }, 3);
        assertArrayEquals(new int[] { 1, 2, 3 }, ints.get(0));
        assertArrayEquals(new int[] { 7 }, ints.get(2));
        assertTrue(N.split((int[]) null, 2).isEmpty());
        List<int[]> intRange = N.split(new int[] { 0, 1, 2, 3, 4, 5, 6 }, 1, 6, 2);
        assertEquals("[[1, 2], [3, 4], [5]]", CommonUtil.stringOf(intRange));

        List<long[]> longs = N.split(new long[] { 1L, 2L, 3L, 4L, 5L }, 2);
        assertArrayEquals(new long[] { 1L, 2L }, longs.get(0));
        assertTrue(N.split((long[]) null, 2).isEmpty());

        List<float[]> floats = N.split(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, floats.get(0));
        assertTrue(N.split((float[]) null, 2).isEmpty());

        List<double[]> doubles = N.split(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2);
        assertArrayEquals(new double[] { 1.0, 2.0 }, doubles.get(0));
        assertTrue(N.split((double[]) null, 2).isEmpty());

        List<String[]> objects = N.split(new String[] { "a", "b", "c", "d", "e" }, 2);
        assertArrayEquals(new String[] { "a", "b" }, objects.get(0));
        assertArrayEquals(new String[] { "e" }, objects.get(2));
        assertTrue(N.split((String[]) null, 2).isEmpty());
        List<Integer[]> objRange = N.split(new Integer[] { 1, 2, 3, 4, 5, 6 }, 1, 5, 2);
        assertArrayEquals(new Integer[] { 2, 3 }, objRange.get(0));
        assertThrows(IllegalArgumentException.class, () -> N.split(new String[] { "a" }, 1, 1, 0));
    }

    @Test
    public void testSplit_collectionIterableIterator() {
        List<List<String>> coll = N.split(Arrays.asList("a", "b", "c", "d", "e"), 2);
        assertEquals(Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"), Arrays.asList("e")), coll);
        assertTrue(N.split((Collection<String>) null, 2).isEmpty());
        assertTrue(N.split(Collections.<String> emptyList(), 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(Arrays.asList("a"), 0));

        List<List<Integer>> ranged = N.split(Arrays.asList(1, 2, 3, 4, 5, 6, 7), 1, 6, 2);
        assertEquals(Arrays.asList(Arrays.asList(2, 3), Arrays.asList(4, 5), Arrays.asList(6)), ranged);
        Collection<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f"));
        assertEquals(Arrays.asList(Arrays.asList("b", "c"), Arrays.asList("d", "e")), N.split(set, 1, 5, 2));
        assertTrue(N.split(Arrays.asList("a", "b"), 1, 1, 2).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> N.split(Arrays.asList("a", "b"), -1, 1, 2));

        assertEquals(coll, N.split((Iterable<String>) Arrays.asList("a", "b", "c", "d", "e"), 2));
        assertTrue(N.split((Iterable<String>) null, 2).isEmpty());
        assertTrue(N.split((Iterable<String>) Collections::emptyIterator, 2).isEmpty());

        ObjIterator<List<String>> iterChunks = N.split(Arrays.asList("a", "b", "c", "d", "e").iterator(), 2);
        assertEquals(coll, iteratorToList(iterChunks));
        assertTrue(iteratorToList(N.split((Iterator<String>) null, 2)).isEmpty());
        assertTrue(iteratorToList(N.split(Collections.<String> emptyIterator(), 2)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(Arrays.asList("a").iterator(), 0));

        ObjIterator<List<String>> exhausted = N.split(Arrays.asList("a").iterator(), 1);
        assertEquals(Collections.singletonList("a"), exhausted.next());
        assertFalse(exhausted.hasNext());
        assertThrows(NoSuchElementException.class, exhausted::next);

        ObjIterator<List<Integer>> huge = N.split(Arrays.asList(1, 2, 3).iterator(), Integer.MAX_VALUE);
        assertEquals(Arrays.asList(1, 2, 3), huge.next());
        assertFalse(huge.hasNext());
    }

    @Test
    public void testSplit_charSequence() {
        assertEquals(Arrays.asList("abc", "def", "g"), N.split("abcdefg", 3));
        assertEquals(Arrays.asList("a", "b", "c"), N.split("abc", 1));
        assertEquals(Collections.singletonList("hello"), N.split("hello", 10));
        assertTrue(N.split((String) null, 2).isEmpty());
        assertTrue(N.split("", 2).isEmpty());
        assertEquals(Arrays.asList("bc", "de", "fg"), N.split("abcdefgh", 1, 7, 2));
        assertTrue(N.split("abcdef", 1, 1, 2).isEmpty());
        assertTrue(N.split((CharSequence) null, 0, 0, 1).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split("abc", 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.split("abc", -1, 2, 1));
    }

    @Test
    public void testSplitByChunkCount() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5), Arrays.asList(6), Arrays.asList(7)),
                N.splitByChunkCount(numbers, 5));
        assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3), Arrays.asList(4, 5), Arrays.asList(6, 7)),
                N.splitByChunkCount(numbers, 5, true));
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)), N.splitByChunkCount(Arrays.asList(1, 2, 3, 4, 5, 6), 3));
        assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3)), N.splitByChunkCount(Arrays.asList(1, 2, 3), 5));
        assertTrue(N.splitByChunkCount((Collection<Integer>) null, 3).isEmpty());
        assertTrue(N.splitByChunkCount(Collections.<Integer> emptyList(), 3).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(numbers, 0));

        Collection<Integer> set = CommonUtil.toLinkedHashSet(1, 2, 3, 4, 5, 6, 7);
        assertEquals("[[1], [2], [3], [4, 5], [6, 7]]", CommonUtil.toString(N.splitByChunkCount(set, 5, true)));
        assertEquals("[[1, 2], [3, 4], [5], [6], [7]]", CommonUtil.toString(N.splitByChunkCount(set, 5, false)));

        int[] a = { 1, 2, 3, 4, 5, 6, 7 };
        IntBiFunction<int[]> copy = (from, to) -> CommonUtil.copyOfRange(a, from, to);
        List<int[]> byFunc = N.splitByChunkCount(7, 5, copy);
        assertListOfIntArraysEquals(Arrays.asList(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5 }, new int[] { 6 }, new int[] { 7 }), byFunc);
        assertListOfIntArraysEquals(Arrays.asList(new int[] { 1 }, new int[] { 2 }, new int[] { 3 }, new int[] { 4, 5 }, new int[] { 6, 7 }),
                N.splitByChunkCount(7, 5, true, copy));
        assertEquals(1, N.splitByChunkCount(3, 1, copy).size());
        assertEquals(0, N.splitByChunkCount(0, 5, copy).size());
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(-1, 5, copy));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(5, 0, copy));

        IntBiFunction<int[]> indexFunc = (from, to) -> {
            int[] result = new int[to - from];
            for (int i = 0; i < result.length; i++) {
                result[i] = from + i;
            }
            return result;
        };
        List<int[]> largerLast = N.splitByChunkCount(10, 3, indexFunc);
        assertEquals(4, largerLast.get(0).length);
        assertEquals(3, largerLast.get(2).length);
        List<int[]> smallerFirst = N.splitByChunkCount(10, 3, true, indexFunc);
        assertEquals(3, smallerFirst.get(0).length);
        assertEquals(4, smallerFirst.get(2).length);
    }
}
