package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.IntBiFunction;

@Tag("new-test")
public class N100Test extends TestBase {

    @Test
    public void testOccurrencesOfBooleanArray() {
        boolean[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, true));
        assertEquals(0, N.occurrencesOf(null, true));

        boolean[] array = { true, false, true, true, false };
        assertEquals(3, N.occurrencesOf(array, true));
        assertEquals(2, N.occurrencesOf(array, false));
    }

    @Test
    public void testOccurrencesOfCharArray() {
        char[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 'a'));
        assertEquals(0, N.occurrencesOf((char[]) null, 'a'));

        char[] array = { 'a', 'b', 'c', 'a', 'a' };
        assertEquals(3, N.occurrencesOf(array, 'a'));
        assertEquals(1, N.occurrencesOf(array, 'b'));
        assertEquals(0, N.occurrencesOf(array, 'd'));
    }

    @Test
    public void testOccurrencesOfByteArray() {
        byte[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, (byte) 1));
        assertEquals(0, N.occurrencesOf((byte[]) null, (byte) 1));

        byte[] array = { 1, 2, 3, 1, 1 };
        assertEquals(3, N.occurrencesOf(array, (byte) 1));
        assertEquals(1, N.occurrencesOf(array, (byte) 2));
        assertEquals(0, N.occurrencesOf(array, (byte) 4));
    }

    @Test
    public void testOccurrencesOfShortArray() {
        short[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, (short) 1));
        assertEquals(0, N.occurrencesOf((short[]) null, (short) 1));

        short[] array = { 1, 2, 3, 1, 1 };
        assertEquals(3, N.occurrencesOf(array, (short) 1));
        assertEquals(1, N.occurrencesOf(array, (short) 2));
        assertEquals(0, N.occurrencesOf(array, (short) 4));
    }

    @Test
    public void testOccurrencesOfIntArray() {
        int[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 1));
        assertEquals(0, N.occurrencesOf((int[]) null, 1));

        int[] array = { 1, 2, 3, 1, 1 };
        assertEquals(3, N.occurrencesOf(array, 1));
        assertEquals(1, N.occurrencesOf(array, 2));
        assertEquals(0, N.occurrencesOf(array, 4));
    }

    @Test
    public void testOccurrencesOfLongArray() {
        long[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 1L));
        assertEquals(0, N.occurrencesOf((long[]) null, 1L));

        long[] array = { 1L, 2L, 3L, 1L, 1L };
        assertEquals(3, N.occurrencesOf(array, 1L));
        assertEquals(1, N.occurrencesOf(array, 2L));
        assertEquals(0, N.occurrencesOf(array, 4L));
    }

    @Test
    public void testOccurrencesOfFloatArray() {
        float[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 1.0f));
        assertEquals(0, N.occurrencesOf((float[]) null, 1.0f));

        float[] array = { 1.0f, 2.0f, 3.0f, 1.0f, 1.0f };
        assertEquals(3, N.occurrencesOf(array, 1.0f));
        assertEquals(1, N.occurrencesOf(array, 2.0f));
        assertEquals(0, N.occurrencesOf(array, 4.0f));
    }

    @Test
    public void testOccurrencesOfDoubleArray() {
        double[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 1.0));
        assertEquals(0, N.occurrencesOf(null, 1.0));

        double[] array = { 1.0, 2.0, 3.0, 1.0, 1.0 };
        assertEquals(3, N.occurrencesOf(array, 1.0));
        assertEquals(1, N.occurrencesOf(array, 2.0));
        assertEquals(0, N.occurrencesOf(array, 4.0));
    }

    @Test
    public void testOccurrencesOfObjectArray() {
        String[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, "a"));
        assertEquals(0, N.occurrencesOf((String[]) null, "a"));

        String[] array = { "a", "b", "c", "a", "a" };
        assertEquals(3, N.occurrencesOf(array, "a"));
        assertEquals(1, N.occurrencesOf(array, "b"));
        assertEquals(0, N.occurrencesOf(array, "d"));

        String[] arrayWithNull = { "a", null, "a", null };
        assertEquals(2, N.occurrencesOf(arrayWithNull, null));
        assertEquals(2, N.occurrencesOf(arrayWithNull, "a"));
    }

    @Test
    public void testOccurrencesOfIterable() {
        List<String> empty = new ArrayList<>();
        assertEquals(0, N.occurrencesOf(empty, "a"));
        assertEquals(0, N.occurrencesOf((List<String>) null, "a"));

        List<String> list = Arrays.asList("a", "b", "c", "a", "a");
        assertEquals(3, N.occurrencesOf(list, "a"));
        assertEquals(1, N.occurrencesOf(list, "b"));
        assertEquals(0, N.occurrencesOf(list, "d"));

        List<String> listWithNull = Arrays.asList("a", null, "a", null);
        assertEquals(2, N.occurrencesOf(listWithNull, null));
        assertEquals(2, N.occurrencesOf(listWithNull, "a"));
    }

    @Test
    public void testOccurrencesOfIterator() {
        Iterator<String> nullIter = null;
        assertEquals(0, N.occurrencesOf(nullIter, "a"));

        List<String> list = Arrays.asList("a", "b", "c", "a", "a");
        assertEquals(3, N.occurrencesOf(list.iterator(), "a"));

        List<String> list2 = Arrays.asList("a", "b", "c", "a", "a");
        assertEquals(1, N.occurrencesOf(list2.iterator(), "b"));
    }

    @Test
    public void testOccurrencesOfString() {
        assertEquals(0, N.occurrencesOf((String) null, 'a'));
        assertEquals(0, N.occurrencesOf("", 'a'));
        assertEquals(3, N.occurrencesOf("banana", 'a'));
        assertEquals(2, N.occurrencesOf("banana", 'n'));
        assertEquals(0, N.occurrencesOf("banana", 'x'));

        assertEquals(0, N.occurrencesOf((String) null, "a"));
        assertEquals(0, N.occurrencesOf("", "a"));
        assertEquals(2, N.occurrencesOf("aba aba", "aba"));
        assertEquals(1, N.occurrencesOf("banana", "ana"));
    }

    @Test
    public void testOccurrencesMapArray() {
        String[] empty = {};
        Map<String, Integer> emptyMap = N.occurrencesMap(empty);
        assertTrue(emptyMap.isEmpty());

        String[] nullArray = null;
        Map<String, Integer> nullMap = N.occurrencesMap(nullArray);
        assertTrue(nullMap.isEmpty());

        String[] array = { "a", "b", "c", "a", "a", "b" };
        Map<String, Integer> map = N.occurrencesMap(array);
        assertEquals(3, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
        assertEquals(1, map.get("c").intValue());
    }

    @Test
    public void testOccurrencesMapIterable() {
        List<String> empty = new ArrayList<>();
        Map<String, Integer> emptyMap = N.occurrencesMap(empty);
        assertTrue(emptyMap.isEmpty());

        List<String> nullList = null;
        Map<String, Integer> nullMap = N.occurrencesMap(nullList);
        assertTrue(nullMap.isEmpty());

        List<String> list = Arrays.asList("a", "b", "c", "a", "a", "b");
        Map<String, Integer> map = N.occurrencesMap(list);
        assertEquals(3, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
        assertEquals(1, map.get("c").intValue());
    }

    @Test
    public void testOccurrencesMapIterator() {
        Iterator<String> nullIter = null;
        Map<String, Integer> nullMap = N.occurrencesMap(nullIter);
        assertTrue(nullMap.isEmpty());

        List<String> list = Arrays.asList("a", "b", "c", "a", "a", "b");
        Map<String, Integer> map = N.occurrencesMap(list.iterator());
        assertEquals(3, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
        assertEquals(1, map.get("c").intValue());
    }

    @Test
    public void testContainsBooleanArray() {
        boolean[] empty = {};
        assertFalse(N.contains(empty, true));
        assertFalse(N.contains(null, true));

        boolean[] array = { true, false, true };
        assertTrue(N.contains(array, true));
        assertTrue(N.contains(array, false));
    }

    @Test
    public void testContainsCharArray() {
        char[] empty = {};
        assertFalse(N.contains(empty, 'a'));
        assertFalse(N.contains((char[]) null, 'a'));

        char[] array = { 'a', 'b', 'c' };
        assertTrue(N.contains(array, 'a'));
        assertTrue(N.contains(array, 'b'));
        assertFalse(N.contains(array, 'd'));
    }

    @Test
    public void testContainsByteArray() {
        byte[] empty = {};
        assertFalse(N.contains(empty, (byte) 1));
        assertFalse(N.contains((byte[]) null, (byte) 1));

        byte[] array = { 1, 2, 3 };
        assertTrue(N.contains(array, (byte) 1));
        assertTrue(N.contains(array, (byte) 2));
        assertFalse(N.contains(array, (byte) 4));
    }

    @Test
    public void testContainsShortArray() {
        short[] empty = {};
        assertFalse(N.contains(empty, (short) 1));
        assertFalse(N.contains((short[]) null, (short) 1));

        short[] array = { 1, 2, 3 };
        assertTrue(N.contains(array, (short) 1));
        assertTrue(N.contains(array, (short) 2));
        assertFalse(N.contains(array, (short) 4));
    }

    @Test
    public void testContainsIntArray() {
        int[] empty = {};
        assertFalse(N.contains(empty, 1));
        assertFalse(N.contains((int[]) null, 1));

        int[] array = { 1, 2, 3 };
        assertTrue(N.contains(array, 1));
        assertTrue(N.contains(array, 2));
        assertFalse(N.contains(array, 4));
    }

    @Test
    public void testContainsLongArray() {
        long[] empty = {};
        assertFalse(N.contains(empty, 1L));
        assertFalse(N.contains((long[]) null, 1L));

        long[] array = { 1L, 2L, 3L };
        assertTrue(N.contains(array, 1L));
        assertTrue(N.contains(array, 2L));
        assertFalse(N.contains(array, 4L));
    }

    @Test
    public void testContainsFloatArray() {
        float[] empty = {};
        assertFalse(N.contains(empty, 1.0f));
        assertFalse(N.contains((float[]) null, 1.0f));

        float[] array = { 1.0f, 2.0f, 3.0f };
        assertTrue(N.contains(array, 1.0f));
        assertTrue(N.contains(array, 2.0f));
        assertFalse(N.contains(array, 4.0f));
    }

    @Test
    public void testContainsDoubleArray() {
        double[] empty = {};
        assertFalse(N.contains(empty, 1.0));
        assertFalse(N.contains((double[]) null, 1.0));

        double[] array = { 1.0, 2.0, 3.0 };
        assertTrue(N.contains(array, 1.0));
        assertTrue(N.contains(array, 2.0));
        assertFalse(N.contains(array, 4.0));
    }

    @Test
    public void testContainsObjectArray() {
        String[] empty = {};
        assertFalse(N.contains(empty, "a"));
        assertFalse(N.contains((String[]) null, "a"));

        String[] array = { "a", "b", "c" };
        assertTrue(N.contains(array, "a"));
        assertTrue(N.contains(array, "b"));
        assertFalse(N.contains(array, "d"));

        String[] arrayWithNull = { "a", null, "c" };
        assertTrue(N.contains(arrayWithNull, null));
        assertTrue(N.contains(arrayWithNull, "a"));
    }

    @Test
    public void testContainsCollection() {
        List<String> empty = new ArrayList<>();
        assertFalse(N.contains(empty, "a"));
        assertFalse(N.contains((Collection<?>) null, "a"));

        List<String> list = Arrays.asList("a", "b", "c");
        assertTrue(N.contains(list, "a"));
        assertTrue(N.contains(list, "b"));
        assertFalse(N.contains(list, "d"));
    }

    @Test
    public void testContainsIterable() {
        List<String> empty = new ArrayList<>();
        assertFalse(N.contains((Iterable<?>) empty, "a"));
        assertFalse(N.contains((Iterable<?>) null, "a"));

        List<String> list = Arrays.asList("a", "b", "c");
        assertTrue(N.contains((Iterable<?>) list, "a"));
        assertTrue(N.contains((Iterable<?>) list, "b"));
        assertFalse(N.contains((Iterable<?>) list, "d"));
    }

    @Test
    public void testContainsIterator() {
        assertFalse(N.contains((Iterator<?>) null, "a"));

        List<String> list = Arrays.asList("a", "b", "c");
        assertTrue(N.contains(list.iterator(), "a"));

        List<String> list2 = Arrays.asList("a", "b", "c");
        assertFalse(N.contains(list2.iterator(), "d"));
    }

    @Test
    public void testContainsAllCollectionCollection() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        List<String> valuesToFind = Arrays.asList("a", "c");
        assertTrue(N.containsAll(collection, valuesToFind));

        List<String> notAllPresent = Arrays.asList("a", "e");
        assertFalse(N.containsAll(collection, notAllPresent));

        assertTrue(N.containsAll(collection, new ArrayList<>()));
        assertFalse(N.containsAll(new ArrayList<>(), valuesToFind));
        assertFalse(N.containsAll((List<String>) null, valuesToFind));
        assertTrue(N.containsAll(collection, (Collection<?>) null));
    }

    @Test
    public void testContainsAllCollectionArray() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.containsAll(collection, "a", "c"));
        assertFalse(N.containsAll(collection, "a", "e"));
        assertTrue(N.containsAll(collection, new String[] {}));
        assertFalse(N.containsAll(new ArrayList<>(), "a"));
    }

    @Test
    public void testContainsAllIterableCollection() {
        List<String> iterable = Arrays.asList("a", "b", "c", "d");
        List<String> valuesToFind = Arrays.asList("a", "c");
        assertTrue(N.containsAll((Iterable<?>) iterable, valuesToFind));

        List<String> notAllPresent = Arrays.asList("a", "e");
        assertFalse(N.containsAll((Iterable<?>) iterable, notAllPresent));
    }

    @Test
    public void testContainsAllIteratorCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> valuesToFind = Arrays.asList("a", "c");
        assertTrue(N.containsAll(list.iterator(), valuesToFind));

        List<String> list2 = Arrays.asList("a", "b", "c", "d");
        List<String> notAllPresent = Arrays.asList("a", "e");
        assertFalse(N.containsAll(list2.iterator(), notAllPresent));
    }

    @Test
    public void testContainsAnyCollectionCollection() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        List<String> valuesToFind = Arrays.asList("a", "e");
        assertTrue(N.containsAny(collection, valuesToFind));

        List<String> nonePresent = Arrays.asList("x", "y");
        assertFalse(N.containsAny(collection, nonePresent));

        assertFalse(N.containsAny(collection, new ArrayList<>()));
        assertFalse(N.containsAny(new ArrayList<>(), valuesToFind));
    }

    @Test
    public void testContainsAnyCollectionArray() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.containsAny(collection, "a", "e"));
        assertFalse(N.containsAny(collection, "x", "y"));
        assertFalse(N.containsAny(collection, new String[] {}));
    }

    @Test
    public void testContainsAnyIterableSet() {
        List<String> iterable = Arrays.asList("a", "b", "c", "d");
        Set<String> valuesToFind = new HashSet<>(Arrays.asList("a", "e"));
        assertTrue(N.containsAny((Iterable<?>) iterable, valuesToFind));

        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertFalse(N.containsAny((Iterable<?>) iterable, nonePresent));
    }

    @Test
    public void testContainsAnyIteratorSet() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Set<String> valuesToFind = new HashSet<>(Arrays.asList("a", "e"));
        assertTrue(N.containsAny(list.iterator(), valuesToFind));

        List<String> list2 = Arrays.asList("a", "b", "c", "d");
        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertFalse(N.containsAny(list2.iterator(), nonePresent));
    }

    @Test
    public void testContainsNoneCollectionCollection() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        List<String> nonePresent = Arrays.asList("x", "y");
        assertTrue(N.containsNone(collection, nonePresent));

        List<String> somePresent = Arrays.asList("a", "x");
        assertFalse(N.containsNone(collection, somePresent));

        assertTrue(N.containsNone(collection, new ArrayList<>()));
        assertTrue(N.containsNone(new ArrayList<>(), nonePresent));
    }

    @Test
    public void testContainsNoneCollectionArray() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.containsNone(collection, "x", "y"));
        assertFalse(N.containsNone(collection, "a", "x"));
        assertTrue(N.containsNone(collection, new String[] {}));
    }

    @Test
    public void testContainsNoneIterableSet() {
        List<String> iterable = Arrays.asList("a", "b", "c", "d");
        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertTrue(N.containsNone((Iterable<?>) iterable, nonePresent));

        Set<String> somePresent = new HashSet<>(Arrays.asList("a", "x"));
        assertFalse(N.containsNone((Iterable<?>) iterable, somePresent));
    }

    @Test
    public void testContainsNoneIteratorSet() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertTrue(N.containsNone(list.iterator(), nonePresent));

        List<String> list2 = Arrays.asList("a", "b", "c", "d");
        Set<String> somePresent = new HashSet<>(Arrays.asList("a", "x"));
        assertFalse(N.containsNone(list2.iterator(), somePresent));
    }

    @Test
    public void testSliceArray() {
        String[] array = { "a", "b", "c", "d", "e" };
        ImmutableList<String> slice = N.slice(array, 1, 4);
        assertEquals(3, slice.size());
        assertEquals("b", slice.get(0));
        assertEquals("c", slice.get(1));
        assertEquals("d", slice.get(2));

        ImmutableList<String> empty = N.slice(new String[] {}, 0, 0);
        assertTrue(empty.isEmpty());

        ImmutableList<String> nullSlice = N.slice((String[]) null, 0, 0);
        assertTrue(nullSlice.isEmpty());
    }

    @Test
    public void testSliceArrayInvalidIndices() {
        String[] array = { "a", "b", "c" };
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(array, 2, 5));
    }

    @Test
    public void testSliceList() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        ImmutableList<String> slice = N.slice(list, 1, 4);
        assertEquals(3, slice.size());
        assertEquals("b", slice.get(0));
        assertEquals("c", slice.get(1));
        assertEquals("d", slice.get(2));

        ImmutableList<String> fullSlice = N.slice(list, 0, list.size());
        assertEquals(list.size(), fullSlice.size());
    }

    @Test
    public void testSliceCollection() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        ImmutableCollection<String> slice = N.slice(set, 1, 4);
        assertEquals(3, slice.size());

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        ImmutableCollection<String> listSlice = N.slice((Collection<String>) list, 1, 4);
        assertEquals(3, listSlice.size());
    }

    @Test
    public void testSliceIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        ObjIterator<String> slice = N.slice(list.iterator(), 1, 4);

        List<String> result = new ArrayList<>();
        while (slice.hasNext()) {
            result.add(slice.next());
        }
        assertEquals(3, result.size());
        assertEquals("b", result.get(0));
        assertEquals("c", result.get(1));
        assertEquals("d", result.get(2));

        ObjIterator<String> empty = N.slice((Iterator<String>) null, 0, 0);
        assertFalse(empty.hasNext());
    }

    @Test
    public void testSliceIteratorInvalidFromIndex() {
        assertThrows(IllegalArgumentException.class, () -> N.slice(Arrays.asList("a", "b").iterator(), -1, 2));
    }

    @Test
    public void testSplitBooleanArray() {
        boolean[] array = { true, false, true, false, true };
        List<boolean[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);

        List<boolean[]> empty = N.split(new boolean[] {}, 2);
        assertTrue(empty.isEmpty());

        List<boolean[]> nullSplit = N.split((boolean[]) null, 2);
        assertTrue(nullSplit.isEmpty());
    }

    @Test
    public void testSplitBooleanArrayInvalidChunkSize() {
        assertThrows(IllegalArgumentException.class, () -> N.split(new boolean[] { true, false }, 0));
    }

    @Test
    public void testSplitBooleanArrayWithRange() {
        boolean[] array = { true, false, true, false, true, false };
        List<boolean[]> chunks = N.split(array, 1, 5, 2);
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
    }

    @Test
    public void testSplitCharArray() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        List<char[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);

        assertArrayEquals(new char[] { 'a', 'b' }, chunks.get(0));
        assertArrayEquals(new char[] { 'c', 'd' }, chunks.get(1));
        assertArrayEquals(new char[] { 'e' }, chunks.get(2));
    }

    @Test
    public void testSplitByteArray() {
        byte[] array = { 1, 2, 3, 4, 5 };
        List<byte[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);
    }

    @Test
    public void testSplitShortArray() {
        short[] array = { 1, 2, 3, 4, 5 };
        List<short[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);
    }

    @Test
    public void testSplitIntArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        List<int[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);

        assertArrayEquals(new int[] { 1, 2 }, chunks.get(0));
        assertArrayEquals(new int[] { 3, 4 }, chunks.get(1));
        assertArrayEquals(new int[] { 5 }, chunks.get(2));
    }

    @Test
    public void testSplitLongArray() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        List<long[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);
    }

    @Test
    public void testSplitFloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<float[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);
    }

    @Test
    public void testSplitDoubleArray() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<double[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);
    }

    @Test
    public void testSplitObjectArray() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String[]> chunks = N.split(array, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).length);
        assertEquals(2, chunks.get(1).length);
        assertEquals(1, chunks.get(2).length);

        assertArrayEquals(new String[] { "a", "b" }, chunks.get(0));
        assertArrayEquals(new String[] { "c", "d" }, chunks.get(1));
        assertArrayEquals(new String[] { "e" }, chunks.get(2));
    }

    @Test
    public void testSplitCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> chunks = N.split(list, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());

        assertEquals(Arrays.asList("a", "b"), chunks.get(0));
        assertEquals(Arrays.asList("c", "d"), chunks.get(1));
        assertEquals(Arrays.asList("e"), chunks.get(2));
    }

    @Test
    public void testSplitCollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<List<String>> chunks = N.split(list, 1, 5, 2);
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());

        assertEquals(Arrays.asList("b", "c"), chunks.get(0));
        assertEquals(Arrays.asList("d", "e"), chunks.get(1));
    }

    @Test
    public void testSplitIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> chunks = N.split((Iterable<String>) list, 2);
        assertEquals(3, chunks.size());

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<List<String>> setChunks = N.split((Iterable<String>) set, 2);
        assertEquals(3, setChunks.size());
    }

    @Test
    public void testSplitIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        ObjIterator<List<String>> chunks = N.split(list.iterator(), 2);

        List<List<String>> result = new ArrayList<>();
        while (chunks.hasNext()) {
            result.add(chunks.next());
        }

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());
        assertEquals(1, result.get(2).size());

        ObjIterator<List<String>> empty = N.split((Iterator<String>) null, 2);
        assertFalse(empty.hasNext());
    }

    @Test
    public void testSplitCharSequence() {
        String str = "abcde";
        List<String> chunks = N.split(str, 2);
        assertEquals(3, chunks.size());
        assertEquals("ab", chunks.get(0));
        assertEquals("cd", chunks.get(1));
        assertEquals("e", chunks.get(2));

        List<String> empty = N.split("", 2);
        assertTrue(empty.isEmpty());

        List<String> nullSplit = N.split((CharSequence) null, 2);
        assertTrue(nullSplit.isEmpty());
    }

    @Test
    public void testSplitCharSequenceWithRange() {
        String str = "abcdef";
        List<String> chunks = N.split(str, 1, 5, 2);
        assertEquals(2, chunks.size());
        assertEquals("bc", chunks.get(0));
        assertEquals("de", chunks.get(1));
    }

    @Test
    public void testSplitByChunkCount() {
        IntBiFunction<int[]> func = (fromIndex, toIndex) -> {
            int[] result = new int[toIndex - fromIndex];
            for (int i = 0; i < result.length; i++) {
                result[i] = fromIndex + i;
            }
            return result;
        };

        List<int[]> chunks = N.splitByChunkCount(10, 3, func);
        assertEquals(3, chunks.size());
        assertEquals(4, chunks.get(0).length);
        assertEquals(3, chunks.get(1).length);
        assertEquals(3, chunks.get(2).length);
    }

    @Test
    public void testSplitByChunkCountSizeSmallerFirst() {
        IntBiFunction<int[]> func = (fromIndex, toIndex) -> {
            int[] result = new int[toIndex - fromIndex];
            for (int i = 0; i < result.length; i++) {
                result[i] = fromIndex + i;
            }
            return result;
        };

        List<int[]> chunks = N.splitByChunkCount(10, 3, true, func);
        assertEquals(3, chunks.size());
        assertEquals(3, chunks.get(0).length);
        assertEquals(3, chunks.get(1).length);
        assertEquals(4, chunks.get(2).length);
    }

    @Test
    public void testSplitByChunkCountCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<List<Integer>> chunks = N.splitByChunkCount(list, 3);
        assertEquals(3, chunks.size());
        assertEquals(4, chunks.get(0).size());
        assertEquals(3, chunks.get(1).size());
        assertEquals(3, chunks.get(2).size());
    }

    @Test
    public void testSplitByChunkCountCollectionSizeSmallerFirst() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = N.splitByChunkCount(list, 5, true);
        assertEquals(5, chunks.size());
        assertEquals(1, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
        assertEquals(2, chunks.get(3).size());
        assertEquals(2, chunks.get(4).size());
    }

    @Test
    public void testConcatBooleanArrays() {
        boolean[] a = { true, false };
        boolean[] b = { false, true };
        boolean[] result = N.concat(a, b);
        assertArrayEquals(new boolean[] { true, false, false, true }, result);

        boolean[] empty = {};
        assertArrayEquals(a.clone(), N.concat(a, empty));
        assertArrayEquals(b.clone(), N.concat(empty, b));
        assertArrayEquals(empty, N.concat(empty, empty));
        assertArrayEquals(b.clone(), N.concat(null, b));
        assertArrayEquals(a.clone(), N.concat(a, null));
    }

    @Test
    public void testConcatMultipleBooleanArrays() {
        boolean[] a = { true };
        boolean[] b = { false };
        boolean[] c = { true, false };
        boolean[] result = N.concat(a, b, c);
        assertArrayEquals(new boolean[] { true, false, true, false }, result);

        assertArrayEquals(new boolean[] {}, N.concat(new boolean[0]));
        assertArrayEquals(a.clone(), N.concat(a));
        assertArrayEquals(new boolean[] {}, N.concat((boolean[][]) null));
    }

    @Test
    public void testConcatCharArrays() {
        char[] a = { 'a', 'b' };
        char[] b = { 'c', 'd' };
        char[] result = N.concat(a, b);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testConcatMultipleCharArrays() {
        char[] a = { 'a' };
        char[] b = { 'b' };
        char[] c = { 'c', 'd' };
        char[] result = N.concat(a, b, c);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testConcatByteArrays() {
        byte[] a = { 1, 2 };
        byte[] b = { 3, 4 };
        byte[] result = N.concat(a, b);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatMultipleByteArrays() {
        byte[] a = { 1 };
        byte[] b = { 2 };
        byte[] c = { 3, 4 };
        byte[] result = N.concat(a, b, c);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatShortArrays() {
        short[] a = { 1, 2 };
        short[] b = { 3, 4 };
        short[] result = N.concat(a, b);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatMultipleShortArrays() {
        short[] a = { 1 };
        short[] b = { 2 };
        short[] c = { 3, 4 };
        short[] result = N.concat(a, b, c);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatIntArrays() {
        int[] a = { 1, 2 };
        int[] b = { 3, 4 };
        int[] result = N.concat(a, b);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatMultipleIntArrays() {
        int[] a = { 1 };
        int[] b = { 2 };
        int[] c = { 3, 4 };
        int[] result = N.concat(a, b, c);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatLongArrays() {
        long[] a = { 1L, 2L };
        long[] b = { 3L, 4L };
        long[] result = N.concat(a, b);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatMultipleLongArrays() {
        long[] a = { 1L };
        long[] b = { 2L };
        long[] c = { 3L, 4L };
        long[] result = N.concat(a, b, c);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatFloatArrays() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 3.0f, 4.0f };
        float[] result = N.concat(a, b);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testConcatMultipleFloatArrays() {
        float[] a = { 1.0f };
        float[] b = { 2.0f };
        float[] c = { 3.0f, 4.0f };
        float[] result = N.concat(a, b, c);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testConcatDoubleArrays() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0 };
        double[] result = N.concat(a, b);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testConcatMultipleDoubleArrays() {
        double[] a = { 1.0 };
        double[] b = { 2.0 };
        double[] c = { 3.0, 4.0 };
        double[] result = N.concat(a, b, c);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testConcatObjectArrays() {
        String[] a = { "a", "b" };
        String[] b = { "c", "d" };
        String[] result = N.concat(a, b);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result);

        String[] empty = {};
        assertArrayEquals(a.clone(), N.concat(a, empty));
        assertArrayEquals(b.clone(), N.concat(empty, b));
        assertArrayEquals(empty, N.concat(empty, empty));
    }

    @Test
    public void testConcatMultipleObjectArrays() {
        String[] a = { "a" };
        String[] b = { "b" };
        String[] c = { "c", "d" };
        String[] result = N.concat(a, b, c);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result);

        assertNull(N.concat((String[][]) null));
        assertArrayEquals(new String[] {}, N.concat(new String[0][]));
        assertArrayEquals(a.clone(), N.concat(a));
    }

    @Test
    public void testConcatIterables() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("c", "d");
        List<String> result = N.concat(a, b);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        List<String> empty = new ArrayList<>();
        assertEquals(a, N.concat(a, empty));
        assertEquals(b, N.concat(empty, b));
        assertEquals(empty, N.concat(empty, empty));
    }

    @Test
    public void testConcatMultipleIterables() {
        List<String> a = Arrays.asList("a");
        List<String> b = Arrays.asList("b");
        List<String> c = Arrays.asList("c", "d");
        List<String> result = N.concat(a, b, c);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        assertEquals(new ArrayList<>(), N.concat(new Iterable[0]));
    }

    @Test
    public void testConcatCollectionOfIterables() {
        List<String> a = Arrays.asList("a");
        List<String> b = Arrays.asList("b");
        List<String> c = Arrays.asList("c", "d");
        List<Iterable<String>> iterables = Arrays.asList(a, b, c);
        List<String> result = N.concat(iterables);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testConcatCollectionOfIterablesWithSupplier() {
        List<String> a = Arrays.asList("a");
        List<String> b = Arrays.asList("b");
        List<String> c = Arrays.asList("c", "d");
        List<Iterable<String>> iterables = Arrays.asList(a, b, c);
        Set<String> result = N.concat(iterables, IntFunctions.ofSet());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testConcatIterators() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("c", "d");
        ObjIterator<String> result = N.concat(a.iterator(), b.iterator());

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), collected);
    }

    @Test
    public void testConcatMultipleIterators() {
        List<String> a = Arrays.asList("a");
        List<String> b = Arrays.asList("b");
        List<String> c = Arrays.asList("c", "d");
        ObjIterator<String> result = N.concat(a.iterator(), b.iterator(), c.iterator());

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), collected);
    }

    @Test
    public void testFlattenBoolean2DArray() {
        boolean[][] array = { { true, false }, { false }, { true, true } };
        boolean[] result = N.flatten(array);
        assertArrayEquals(new boolean[] { true, false, false, true, true }, result);

        assertArrayEquals(new boolean[] {}, N.flatten(new boolean[0][]));
        assertArrayEquals(new boolean[] {}, N.flatten((boolean[][]) null));

        boolean[][] withNull = { { true }, null, { false } };
        assertArrayEquals(new boolean[] { true, false }, N.flatten(withNull));
    }

    @Test
    public void testFlattenChar2DArray() {
        char[][] array = { { 'a', 'b' }, { 'c' }, { 'd', 'e' } };
        char[] result = N.flatten(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testFlattenByte2DArray() {
        byte[][] array = { { 1, 2 }, { 3 }, { 4, 5 } };
        byte[] result = N.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlattenShort2DArray() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5 } };
        short[] result = N.flatten(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlattenInt2DArray() {
        int[][] array = { { 1, 2 }, { 3 }, { 4, 5 } };
        int[] result = N.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlattenLong2DArray() {
        long[][] array = { { 1L, 2L }, { 3L }, { 4L, 5L } };
        long[] result = N.flatten(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testFlattenFloat2DArray() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f }, { 4.0f, 5.0f } };
        float[] result = N.flatten(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result, 0.001f);
    }

    @Test
    public void testFlattenDouble2DArray() {
        double[][] array = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0 } };
        double[] result = N.flatten(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testFlattenObject2DArray() {
        String[][] array = { { "a", "b" }, { "c" }, { "d", "e" } };
        String[] result = N.flatten(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, result);

        assertNull(N.flatten((String[][]) null));

        String[][] withNull = { { "a" }, null, { "b" } };
        assertArrayEquals(new String[] { "a", "b" }, N.flatten(withNull));
    }

    @Test
    public void testFlattenObject2DArrayWithClass() {
        String[][] array = { { "a", "b" }, { "c" }, { "d", "e" } };
        String[] result = N.flatten(array, String.class);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, result);

        assertArrayEquals(new String[] {}, N.flatten((String[][]) null, String.class));
    }

    @Test
    public void testFlattenIterableOfIterables() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e"));
        List<String> result = N.flatten(lists);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);

        List<List<String>> empty = new ArrayList<>();
        assertEquals(new ArrayList<>(), N.flatten(empty));

        assertEquals(new ArrayList<>(), N.flatten((Iterable<Iterable<String>>) null));
    }

    @Test
    public void testFlattenIterableOfIterablesWithSupplier() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e"));
        Set<String> result = N.flatten(lists, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d", "e")), result);
    }

    @Test
    public void testFlattenIteratorOfIterators() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator(), Arrays.asList("d", "e").iterator());
        ObjIterator<String> result = N.flatten(iterators.iterator());

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), collected);

        ObjIterator<String> empty = N.flatten((Iterator<Iterator<String>>) null);
        assertFalse(empty.hasNext());
    }

    @Test
    public void testFlattenEachElement() {
        List<Object> mixed = Arrays.asList("a", Arrays.asList("b", "c"), "d", Arrays.asList("e", Arrays.asList("f", "g")));
        List<?> result = N.flattenEachElement(mixed);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f", "g"), result);

        assertEquals(new ArrayList<>(), N.flattenEachElement(null));
        assertEquals(new ArrayList<>(), N.flattenEachElement(new ArrayList<>()));
    }

    @Test
    public void testFlattenEachElementWithSupplier() {
        List<Object> mixed = Arrays.asList("a", Arrays.asList("b", "c"), "d");
        Set<String> result = N.flattenEachElement(mixed, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testIntersectionBooleanArrays() {
        boolean[] a = { true, false, true, true };
        boolean[] b = { false, true, true };
        boolean[] result = N.intersection(a, b);
        assertArrayEquals(new boolean[] { true, false, true }, result);

        assertArrayEquals(new boolean[] {}, N.intersection(new boolean[] {}, b));
        assertArrayEquals(new boolean[] {}, N.intersection(a, new boolean[] {}));
        assertArrayEquals(new boolean[] {}, N.intersection((boolean[]) null, b));
    }

    @Test
    public void testIntersectionCharArrays() {
        char[] a = { 'a', 'b', 'c', 'a' };
        char[] b = { 'b', 'd', 'a' };
        char[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 'a'));
        assertTrue(N.contains(result, 'b'));
    }

    @Test
    public void testIntersectionByteArrays() {
        byte[] a = { 1, 2, 3, 1 };
        byte[] b = { 2, 4, 1 };
        byte[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, (byte) 1));
        assertTrue(N.contains(result, (byte) 2));
    }

    @Test
    public void testIntersectionShortArrays() {
        short[] a = { 1, 2, 3, 1 };
        short[] b = { 2, 4, 1 };
        short[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, (short) 1));
        assertTrue(N.contains(result, (short) 2));
    }

    @Test
    public void testIntersectionIntArrays() {
        int[] a = { 0, 1, 2, 2, 3 };
        int[] b = { 2, 5, 1 };
        int[] result = N.intersection(a, b);
        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testIntersectionLongArrays() {
        long[] a = { 1L, 2L, 3L, 1L };
        long[] b = { 2L, 4L, 1L };
        long[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1L));
        assertTrue(N.contains(result, 2L));
    }

    @Test
    public void testIntersectionFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f, 1.0f };
        float[] b = { 2.0f, 4.0f, 1.0f };
        float[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1.0f));
        assertTrue(N.contains(result, 2.0f));
    }

    @Test
    public void testIntersectionDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0, 1.0 };
        double[] b = { 2.0, 4.0, 1.0 };
        double[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1.0));
        assertTrue(N.contains(result, 2.0));
    }

    @Test
    public void testIntersectionObjectArrays() {
        String[] a = { "a", "b", "c", "a" };
        String[] b = { "b", "d", "a" };
        List<String> result = N.intersection(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));

        assertEquals(new ArrayList<>(), N.intersection(new String[] {}, b));
        assertEquals(new ArrayList<>(), N.intersection(a, new String[] {}));
    }

    @Test
    public void testIntersectionCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "a");
        List<String> b = Arrays.asList("b", "d", "a");
        List<String> result = N.intersection(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));

        assertEquals(new ArrayList<>(), N.intersection(new ArrayList<>(), b));
        assertEquals(new ArrayList<>(), N.intersection(a, new ArrayList<>()));
    }

    @Test
    public void testIntersectionMultipleCollections() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("b", "c", "d");
        List<String> c = Arrays.asList("c", "d", "e");
        List<Collection<String>> collections = Arrays.asList(a, b, c);
        List<String> result = N.intersection(collections);
        assertEquals(1, result.size());
        assertTrue(result.contains("c"));

        assertEquals(new ArrayList<>(), N.intersection(new ArrayList<Collection<String>>()));

        List<Collection<String>> withEmpty = Arrays.asList(a, new ArrayList<>(), c);
        assertEquals(new ArrayList<>(), N.intersection(withEmpty));
    }

    @Test
    public void testDifferenceBooleanArrays() {
        boolean[] a = { true, false, true, true };
        boolean[] b = { false, true };
        boolean[] result = N.difference(a, b);
        assertArrayEquals(new boolean[] { true, true }, result);

        assertArrayEquals(new boolean[] {}, N.difference(new boolean[] {}, b));
        assertArrayEquals(a.clone(), N.difference(a, new boolean[] {}));
        assertArrayEquals(new boolean[] {}, N.difference((boolean[]) null, b));
        assertArrayEquals(a.clone(), N.difference(a, null));
    }

    @Test
    public void testDifferenceCharArrays() {
        char[] a = { 'a', 'b', 'c', 'a' };
        char[] b = { 'b', 'd' };
        char[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.occurrencesOf(result, 'a'));
        assertEquals(1, N.occurrencesOf(result, 'c'));
    }

    @Test
    public void testDifferenceByteArrays() {
        byte[] a = { 1, 2, 3, 1 };
        byte[] b = { 2, 4 };
        byte[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.occurrencesOf(result, (byte) 1));
        assertEquals(1, N.occurrencesOf(result, (byte) 3));
    }

    @Test
    public void testDifferenceShortArrays() {
        short[] a = { 1, 2, 3, 1 };
        short[] b = { 2, 4 };
        short[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.occurrencesOf(result, (short) 1));
        assertEquals(1, N.occurrencesOf(result, (short) 3));
    }

    @Test
    public void testDifferenceIntArrays() {
        int[] a = { 0, 1, 2, 2, 3 };
        int[] b = { 2, 5, 1 };
        int[] result = N.difference(a, b);
        assertArrayEquals(new int[] { 0, 2, 3 }, result);
    }

    @Test
    public void testDifferenceLongArrays() {
        long[] a = { 1L, 2L, 3L, 1L };
        long[] b = { 2L, 4L };
        long[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.occurrencesOf(result, 1L));
        assertEquals(1, N.occurrencesOf(result, 3L));
    }

    @Test
    public void testDifferenceFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f, 1.0f };
        float[] b = { 2.0f, 4.0f };
        float[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.occurrencesOf(result, 1.0f));
        assertEquals(1, N.occurrencesOf(result, 3.0f));
    }

    @Test
    public void testDifferenceDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0, 1.0 };
        double[] b = { 2.0, 4.0 };
        double[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.occurrencesOf(result, 1.0));
        assertEquals(1, N.occurrencesOf(result, 3.0));
    }

    @Test
    public void testDifferenceObjectArrays() {
        String[] a = { "a", "b", "c", "a" };
        String[] b = { "b", "d" };
        List<String> result = N.difference(a, b);
        assertEquals(3, result.size());
        assertEquals(2, N.occurrencesOf(result, "a"));
        assertEquals(1, N.occurrencesOf(result, "c"));

        assertEquals(new ArrayList<>(), N.difference(new String[] {}, b));
        assertEquals(Arrays.asList(a), N.difference(a, new String[] {}));
    }

    @Test
    public void testDifferenceCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "a");
        List<String> b = Arrays.asList("b", "d");
        List<String> result = N.difference(a, b);
        assertEquals(3, result.size());
        assertEquals(2, N.occurrencesOf(result, "a"));
        assertEquals(1, N.occurrencesOf(result, "c"));

        assertEquals(new ArrayList<>(), N.difference(new ArrayList<>(), b));
        assertEquals(new ArrayList<>(a), N.difference(a, new ArrayList<>()));
    }

    @Test
    public void testSymmetricDifferenceBooleanArrays() {
        boolean[] a = { true, false, true };
        boolean[] b = { false, false, true };
        boolean[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertEquals(1, N.occurrencesOf(result, true));
        assertEquals(1, N.occurrencesOf(result, false));

        assertArrayEquals(b.clone(), N.symmetricDifference(new boolean[] {}, b));
        assertArrayEquals(a.clone(), N.symmetricDifference(a, new boolean[] {}));
        assertArrayEquals(b.clone(), N.symmetricDifference(null, b));
        assertArrayEquals(a.clone(), N.symmetricDifference(a, null));
    }

    @Test
    public void testSymmetricDifferenceCharArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'b', 'c', 'd' };
        char[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 'a'));
        assertTrue(N.contains(result, 'd'));
    }

    @Test
    public void testSymmetricDifferenceByteArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 2, 3, 4 };
        byte[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, (byte) 1));
        assertTrue(N.contains(result, (byte) 4));
    }

    @Test
    public void testSymmetricDifferenceShortArrays() {
        short[] a = { 1, 2, 3 };
        short[] b = { 2, 3, 4 };
        short[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, (short) 1));
        assertTrue(N.contains(result, (short) 4));
    }

    @Test
    public void testSymmetricDifferenceIntArrays() {
        int[] a = { 0, 1, 2, 2, 3 };
        int[] b = { 2, 5, 1 };
        int[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new int[] { 0, 2, 3, 5 }, result);
    }

    @Test
    public void testSymmetricDifferenceLongArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 2L, 3L, 4L };
        long[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1L));
        assertTrue(N.contains(result, 4L));
    }

    @Test
    public void testSymmetricDifferenceFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 2.0f, 3.0f, 4.0f };
        float[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1.0f));
        assertTrue(N.contains(result, 4.0f));
    }

    @Test
    public void testSymmetricDifferenceDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 2.0, 3.0, 4.0 };
        double[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1.0));
        assertTrue(N.contains(result, 4.0));
    }

    @Test
    public void testSymmetricDifferenceObjectArrays() {
        String[] a = { "a", "b", "c" };
        String[] b = { "b", "c", "d" };
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("d"));

        assertEquals(Arrays.asList(b), N.symmetricDifference(new String[] {}, b));
        assertEquals(Arrays.asList(a), N.symmetricDifference(a, new String[] {}));
    }

    @Test
    public void testSymmetricDifferenceCollections() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("b", "c", "d");
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("d"));

        assertEquals(new ArrayList<>(b), N.symmetricDifference(new ArrayList<>(), b));
        assertEquals(new ArrayList<>(a), N.symmetricDifference(a, new ArrayList<>()));
    }

    @Test
    public void testCommonSet() {
        List<String> a = Arrays.asList("a", "b", "c", "a");
        List<String> b = Arrays.asList("b", "c", "d", "c");
        Set<String> result = N.commonSet(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));

        assertEquals(new HashSet<>(), N.commonSet(new ArrayList<>(), b));
        assertEquals(new HashSet<>(), N.commonSet(a, new ArrayList<>()));
        assertEquals(new HashSet<>(), N.commonSet(null, b));
        assertEquals(new HashSet<>(), N.commonSet(a, null));
    }

    @Test
    public void testCommonSetMultipleCollections() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("b", "c", "d");
        List<String> c = Arrays.asList("c", "d", "e");
        List<Collection<String>> collections = Arrays.asList(a, b, c);
        Set<String> result = N.commonSet(collections);
        assertEquals(1, result.size());
        assertTrue(result.contains("c"));

        assertEquals(new HashSet<>(), N.commonSet(new ArrayList<Collection<String>>()));

        List<Collection<String>> singleCollection = Arrays.asList(a);
        assertEquals(new HashSet<>(a), N.commonSet(singleCollection));

        List<Collection<String>> withEmpty = Arrays.asList(a, new ArrayList<>(), c);
        assertEquals(new HashSet<>(), N.commonSet(withEmpty));
    }

    @Test
    public void testExclude() {
        List<String> collection = Arrays.asList("a", "b", "c", "a");
        List<String> result = N.exclude(collection, "a");
        assertEquals(2, result.size());
        assertEquals("b", result.get(0));
        assertEquals("c", result.get(1));

        assertEquals(new ArrayList<>(), N.exclude(new ArrayList<>(), "a"));
        assertEquals(new ArrayList<>(), N.exclude(null, "a"));

        List<String> withNull = Arrays.asList("a", null, "b", null);
        List<String> excludeNull = N.exclude(withNull, null);
        assertEquals(2, excludeNull.size());
        assertEquals("a", excludeNull.get(0));
        assertEquals("b", excludeNull.get(1));
    }

    @Test
    public void testExcludeToSet() {
        List<String> collection = Arrays.asList("a", "b", "c", "a");
        Set<String> result = N.excludeToSet(collection, "a");
        assertEquals(2, result.size());
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
        assertFalse(result.contains("a"));

        assertEquals(new HashSet<>(), N.excludeToSet(new ArrayList<>(), "a"));
        assertEquals(new HashSet<>(), N.excludeToSet(null, "a"));
    }

    @Test
    public void testExcludeAll() {
        List<String> collection = Arrays.asList("a", "b", "c", "d", "a");
        List<String> toExclude = Arrays.asList("a", "c");
        List<String> result = N.excludeAll(collection, toExclude);
        assertEquals(2, result.size());
        assertEquals("b", result.get(0));
        assertEquals("d", result.get(1));

        assertEquals(new ArrayList<>(), N.excludeAll(new ArrayList<>(), toExclude));
        assertEquals(new ArrayList<>(collection), N.excludeAll(collection, new ArrayList<>()));

        List<String> singleExclude = Arrays.asList("a");
        List<String> singleResult = N.excludeAll(collection, singleExclude);
        assertEquals(3, singleResult.size());
    }

    @Test
    public void testExcludeAllToSet() {
        List<String> collection = Arrays.asList("a", "b", "c", "d", "a");
        List<String> toExclude = Arrays.asList("a", "c");
        Set<String> result = N.excludeAllToSet(collection, toExclude);
        assertEquals(2, result.size());
        assertTrue(result.contains("b"));
        assertTrue(result.contains("d"));
        assertFalse(result.contains("a"));
        assertFalse(result.contains("c"));

        assertEquals(new HashSet<>(), N.excludeAllToSet(new ArrayList<>(), toExclude));
        assertEquals(new HashSet<>(collection), N.excludeAllToSet(collection, new ArrayList<>()));

        List<String> singleExclude = Arrays.asList("a");
        Set<String> singleResult = N.excludeAllToSet(collection, singleExclude);
        assertEquals(3, singleResult.size());
    }

    @Test
    public void testIsSubCollection() {
        List<String> subColl = Arrays.asList("a", "b", "a");
        List<String> coll = Arrays.asList("a", "b", "c", "a", "b");
        assertTrue(N.isSubCollection(subColl, coll));

        List<String> notSub = Arrays.asList("a", "b", "d");
        assertFalse(N.isSubCollection(notSub, coll));

        assertTrue(N.isSubCollection(new ArrayList<>(), coll));
        assertFalse(N.isSubCollection(coll, new ArrayList<>()));

        List<String> tooManyOccurrences = Arrays.asList("a", "a", "a");
        assertFalse(N.isSubCollection(tooManyOccurrences, coll));

        assertFalse(N.isSubCollection(coll, subColl));
    }

    @Test
    public void testIsSubCollectionNullSubColl() {
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(null, Arrays.asList("a")));
    }

    @Test
    public void testIsSubCollectionNullColl() {
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(Arrays.asList("a"), null));
    }

    @Test
    public void testIsProperSubCollection() {
        List<String> subColl = Arrays.asList("a", "b");
        List<String> coll = Arrays.asList("a", "b", "c");
        assertTrue(N.isProperSubCollection(subColl, coll));

        List<String> sameColl = Arrays.asList("a", "b", "c");
        assertFalse(N.isProperSubCollection(sameColl, coll));

        assertFalse(N.isProperSubCollection(coll, subColl));

        assertTrue(N.isProperSubCollection(new ArrayList<>(), coll));
        assertFalse(N.isProperSubCollection(new ArrayList<>(), new ArrayList<>()));
    }

    @Test
    public void testIsProperSubCollectionNullSubColl() {
        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(null, Arrays.asList("a")));
    }

    @Test
    public void testIsProperSubCollectionNullColl() {
        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(Arrays.asList("a"), null));
    }

    @Test
    public void testIsEqualCollection() {
        List<String> a = Arrays.asList("a", "b", "c", "a");
        List<String> b = Arrays.asList("c", "a", "b", "a");
        assertTrue(N.isEqualCollection(a, b));

        List<String> c = Arrays.asList("a", "b", "c");
        assertFalse(N.isEqualCollection(a, c));

        List<String> d = Arrays.asList("a", "b", "c", "d");
        assertFalse(N.isEqualCollection(a, d));

        assertTrue(N.isEqualCollection(null, null));
        assertFalse(N.isEqualCollection(a, null));
        assertFalse(N.isEqualCollection(null, a));

        assertTrue(N.isEqualCollection(new ArrayList<>(), new ArrayList<>()));
        assertFalse(N.isEqualCollection(a, new ArrayList<>()));

        List<Integer> nums1 = Arrays.asList(1, 2, 2, 3);
        List<Integer> nums2 = Arrays.asList(2, 3, 1, 2);
        assertTrue(N.isEqualCollection(nums1, nums2));
    }
}
