package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Suppliers;

/** IntStream collection contracts. */
public class IntStreamCollectionTest extends IntStreamTestSupport {

    // collect

    @Test
    public void testCollect() {
        IntList list = createIntStream(1, 2, 3).collect(IntList::new, IntList::add);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
    }

    @Test
    public void testCollect_BasicCollect() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testCollect_TwoArg() {
        List<Integer> result = IntStream.of(1, 2, 3).collect(ArrayList::new, (list, val) -> list.add(val));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollect_WithCombiner() {
        List<Integer> result = createIntStream(1, 2, 3).collect(ArrayList::new, (list, n) -> list.add(n), (l1, l2) -> l1.addAll(l2));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5).collect(ArrayList::new, ArrayList::add);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Integer>> grouped = createIntStream(1, 2, 3, 4, 5, 6).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList());

        assertEquals(Arrays.asList(2, 4, 6), grouped.get(true));
        assertEquals(Arrays.asList(1, 3, 5), grouped.get(false));
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        java.util.TreeMap<Boolean, List<Integer>> grouped = createIntStream(1, 2, 3, 4, 5, 6).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(),
                java.util.TreeMap::new);
        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(2, 4, 6), grouped.get(true));
    }

    // toArray

    @Test
    public void testToArray() {
        int[] array = createIntStream(1, 2, 3, 4, 5).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testToArray_BasicConversion() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testToArray_EmptyStream() {
        int[] result = IntStream.empty().toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testToArray_HappyPath() {
        int[] result = IntStream.of(3, 1, 2).toArray();
        assertArrayEquals(new int[] { 3, 1, 2 }, result);
    }

    @Test
    public void toArray_onEmptyStream_shouldReturnEmptyArray() {
        assertEquals(0, IntStream.empty().toArray().length);
    }

    @Test
    public void testToCollection_Supplier() {
        java.util.LinkedList<Integer> result = IntStream.of(1, 2, 3).toCollection(java.util.LinkedList::new);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(3), result.get(2));
    }

    @Test
    public void testToImmutableList_HappyPath() {
        com.landawn.abacus.util.ImmutableList<Integer> result = IntStream.of(1, 2, 3).toImmutableList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
    }

    @Test
    public void testToImmutableSet_HappyPath() {
        com.landawn.abacus.util.ImmutableSet<Integer> result = IntStream.of(1, 2, 2, 3).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
    }

    // toIntList

    @Test
    public void testToIntList() {
        IntList list = createIntStream(1, 2, 3).toIntList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testToIntList_BasicConversion() {
        IntList result = IntStream.of(1, 2, 3, 4, 5).toIntList();
        assertEquals(5, result.size());
        assertEquals(1, result.get(0));
        assertEquals(5, result.get(4));
    }

    @Test
    public void toIntList_shouldReturnIntList() {
        IntList expected = IntList.of(1, 2, 3);
        assertEquals(expected, IntStream.of(1, 2, 3).toIntList());
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.IntStream javaStream = createIntStream(1, 2, 3).toJdkStream();
        assertArrayEquals(new int[] { 1, 2, 3 }, javaStream.toArray());
    }

    @Test
    public void testToJdkStream_BasicConversion() {
        java.util.stream.IntStream result = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testToList() {
        List<Integer> list = createIntStream(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToList_HappyPath() {
        List<Integer> result = IntStream.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // toMap

    @Test
    public void testToMap() {
        Map<String, Integer> map = createIntStream(1, 2, 3).toMap(n -> "key" + n, n -> n * 10);

        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(10), map.get("key1"));
        assertEquals(Integer.valueOf(20), map.get("key2"));
        assertEquals(Integer.valueOf(30), map.get("key3"));
    }

    @Test
    public void testToMap_BasicMapping() {
        Map<Integer, String> result = IntStream.of(1, 2, 3).toMap(x -> x, x -> "Value:" + x);
        assertEquals(3, result.size());
        assertEquals("Value:1", result.get(1));
        assertEquals("Value:2", result.get(2));
        assertEquals("Value:3", result.get(3));
    }

    @Test
    public void testToMap_WithMapSupplier() {
        Map<Integer, String> result = IntStream.of(1, 2, 3).toMap(x -> x, x -> "Value:" + x, Suppliers.ofMap());
        assertEquals(3, result.size());
        assertTrue(result instanceof HashMap);
    }

    @Test
    public void testToMap_WithMergeFunction() {
        Map<Integer, Integer> result = IntStream.of(1, 2, 1, 3, 2).toMap(x -> x, x -> 1, (a, b) -> a + b);
        assertEquals(3, result.size());
        assertEquals(2, result.get(1).intValue());
        assertEquals(2, result.get(2).intValue());
        assertEquals(1, result.get(3).intValue());
    }

    @Test
    public void testToMap_WithMergeFunction2() {
        Map<Integer, Integer> result = IntStream.of(1, 2, 1, 3, 2).toMap(i -> i, i -> 1, Integer::sum);
        assertEquals(3, result.size());
        assertEquals(2, result.get(1).intValue());
        assertEquals(2, result.get(2).intValue());
        assertEquals(1, result.get(3).intValue());
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<Boolean, Integer> map = createIntStream(1, 2, 3, 4, 5, 6).toMap(n -> n % 2 == 0, n -> n, Integer::sum);

        assertEquals(Integer.valueOf(12), map.get(true));
        assertEquals(Integer.valueOf(9), map.get(false));
    }

    // toMultiset

    @Test
    public void testToMultiset() {
        Multiset<Integer> multiset = createIntStream(1, 2, 2, 3, 3, 3).toMultiset();

        assertEquals(1, multiset.getCount(1));
        assertEquals(2, multiset.getCount(2));
        assertEquals(3, multiset.getCount(3));
    }

    @Test
    public void test_toMultiset() {
        {
            Multiset<Integer> multiset = createIntStream(1, 2, 2, 3, 3).toMultiset();
            assertEquals(Multiset.of(1, 2, 2, 3, 3), multiset);
        }

        {
            Multiset<Integer> multiset = createIntStream(1, 2, 2, 3, 3).map(e -> e).toMultiset();
            assertEquals(Multiset.of(1, 2, 2, 3, 3), multiset);
        }
    }

    @Test
    public void testToMultiset_HappyPath() {
        Multiset<Integer> result = IntStream.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, result.getCount(1));
        assertEquals(2, result.getCount(2));
        assertEquals(3, result.getCount(3));
    }

    @Test
    public void testToMultiset_Supplier() {
        Multiset<Integer> result = IntStream.of(1, 2, 2, 3, 3, 3).toMultiset(Multiset::new);
        assertEquals(1, result.getCount(1));
        assertEquals(2, result.getCount(2));
        assertEquals(3, result.getCount(3));
    }

    @Test
    public void testToSet() {
        Set<Integer> set = createIntStream(1, 2, 2, 3, 3).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToSet_HappyPath() {
        Set<Integer> result = IntStream.of(1, 2, 2, 3, 3).toSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

}
