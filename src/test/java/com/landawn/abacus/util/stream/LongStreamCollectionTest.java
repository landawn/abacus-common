package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multiset;

/** LongStream collection contracts. */
public class LongStreamCollectionTest extends LongStreamTestSupport {

    // collect

    @Test
    public void testCollect() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        List<Long> result = stream.collect(ArrayList::new, List::add, List::addAll);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testCollect_TwoArg() {
        List<Long> result = LongStream.of(1, 2, 3).collect(ArrayList::new, (list, val) -> list.add(val));
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Long> result = createLongStream(1L, 2L, 3L).collect(ArrayList::new, (list, n) -> list.add(n));
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testCollectWithoutCombiner() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        List<Long> result = stream.collect(ArrayList::new, List::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testCollectWithSupplierOnly() {
        LongList result = createLongStream(1L, 2L, 3L).collect(LongList::new, LongList::add);
        assertEquals(3, result.size());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.toArray());
    }

    // groupTo

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Long>> map = LongStream.of(1L, 2L, 3L, 4L, 5L).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList());

        assertEquals(2, map.size());
        assertTrue(map.containsKey(true));
        assertTrue(map.containsKey(false));
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        TreeMap<Boolean, List<Long>> grouped = createLongStream(1L, 2L, 3L, 4L, 5L, 6L).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(),
                TreeMap::new);
        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(2L, 4L, 6L), grouped.get(true));
    }

    @Test
    public void testGroupToWithSupplier() {
        TreeMap<Boolean, List<Long>> result = createLongStream(1L, 2L, 3L, 4L, 5L).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(), TreeMap::new);
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2L, 4L), result.get(true));
        assertEquals(Arrays.asList(1L, 3L, 5L), result.get(false));
    }

    @Test
    public void testToArray() {
        long[] result = LongStream.of(1L, 2L, 3L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testToCollection_Supplier() {
        java.util.LinkedList<Long> result = LongStream.of(1L, 2L, 3L).toCollection(java.util.LinkedList::new);
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(1L), result.get(0));
        assertEquals(Long.valueOf(3L), result.get(2));
    }

    @Test
    public void testToImmutableList() {
        List<Long> result = LongStream.of(1L, 2L, 3L).toImmutableList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(4L));
    }

    @Test
    public void testToImmutableList_HappyPath() {
        com.landawn.abacus.util.ImmutableList<Long> result = LongStream.of(1, 2, 3).toImmutableList();
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(1), result.get(0));
    }

    @Test
    public void testToImmutableSet() {
        Set<Long> result = LongStream.of(1L, 2L, 3L).toImmutableSet();
        assertEquals(3, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add(4L));
    }

    @Test
    public void testToImmutableSet_HappyPath() {
        com.landawn.abacus.util.ImmutableSet<Long> result = LongStream.of(1, 2, 2, 3).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
    }

    @Test
    public void testToJdkStream() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        java.util.stream.LongStream jdkStream = stream.toJdkStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, jdkStream.toArray());
    }

    @Test
    public void testToList() {
        List<Long> result = LongStream.of(1L, 2L, 3L).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testToList_HappyPath() {
        List<Long> result = LongStream.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testToLongList() {
        LongList result = LongStream.of(1L, 2L, 3L).toLongList();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.trimToSize().internalArray());
    }

    // toMap

    @Test
    public void testToMap() {
        Map<Long, String> map = LongStream.of(1L, 2L, 3L).toMap(n -> n, n -> "Value" + n);

        assertEquals(3, map.size());
        assertEquals("Value1", map.get(1L));
        assertEquals("Value2", map.get(2L));
        assertEquals("Value3", map.get(3L));
    }

    @Test
    public void testToMap_WithMapSupplier() {
        TreeMap<String, Long> map = createLongStream(1L, 2L, 3L).toMap(n -> "key" + n, n -> n * 10, TreeMap::new);
        assertEquals(3, map.size());
        assertEquals("key1", map.firstKey());
    }

    @Test
    public void testToMap_WithMerge() {
        java.util.Map<Boolean, Long> map = createLongStream(1L, 2L, 3L, 4L, 5L, 6L).toMap(n -> n % 2 == 0, n -> n, Long::sum);
        assertEquals(Long.valueOf(12), map.get(true));
        assertEquals(Long.valueOf(9), map.get(false));
    }

    @Test
    public void testToMap_WithMergeFunction2() {
        Map<Long, Long> result = LongStream.of(1, 2, 1, 3, 2).toMap(i -> i, i -> 1L, Long::sum);
        assertEquals(3, result.size());
        assertEquals(2L, result.get(1L).longValue());
        assertEquals(2L, result.get(2L).longValue());
        assertEquals(1L, result.get(3L).longValue());
    }

    @Test
    public void testToMapWithMerge() {
        Map<Long, Long> map = LongStream.of(1, 2, 1, 3, 2).toMap(l -> l, l -> 1L, Long::sum);
        assertEquals(2, map.get(1L));
        assertEquals(2, map.get(2L));
        assertEquals(1, map.get(3L));
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<Long, Long> result = createLongStream(1L, 2L, 3L, 1L, 2L).toMap(n -> n, n -> n * 10, (v1, v2) -> v1 + v2);
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(20L), result.get(1L));
        assertEquals(Long.valueOf(40L), result.get(2L));
        assertEquals(Long.valueOf(30L), result.get(3L));
    }

    @Test
    public void testToMapWithSupplier() {
        TreeMap<Long, Long> result = createLongStream(3L, 1L, 2L).toMap(n -> n, n -> n * 10, TreeMap::new);
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), new ArrayList<>(result.keySet()));
    }

    // toMultiset

    @Test
    public void testToMultiset() {
        Multiset<Long> result = LongStream.of(1L, 2L, 2L, 3L, 3L, 3L).toMultiset();
        assertEquals(1, result.getCount(1L));
        assertEquals(2, result.getCount(2L));
        assertEquals(3, result.getCount(3L));
    }

    @Test
    public void testToMultiset_HappyPath() {
        Multiset<Long> result = LongStream.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, result.getCount(1L));
        assertEquals(2, result.getCount(2L));
        assertEquals(3, result.getCount(3L));
    }

    @Test
    public void testToMultiset_Supplier() {
        Multiset<Long> result = LongStream.of(1L, 2L, 2L, 3L, 3L, 3L).toMultiset(Multiset::new);
        assertEquals(1, result.getCount(1L));
        assertEquals(2, result.getCount(2L));
        assertEquals(3, result.getCount(3L));
    }

    @Test
    public void testToSet() {
        Set<Long> result = LongStream.of(1L, 2L, 2L, 3L).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1L, 2L, 3L)), result);
    }

    @Test
    public void testToSet_HappyPath() {
        Set<Long> result = LongStream.of(1, 2, 2, 3, 3).toSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
    }

}
