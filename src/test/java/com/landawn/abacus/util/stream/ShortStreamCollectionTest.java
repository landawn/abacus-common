package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Suppliers;

/** ShortStream collection contracts. */
public class ShortStreamCollectionTest extends ShortStreamTestSupport {

    // collect

    @Test
    public void testCollect() {
        ShortList result = ShortStream.of((short) 1, (short) 2, (short) 3).collect(ShortList::new, ShortList::add, ShortList::addAll);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testCollect_Empty() {
        List<Short> result = ShortStream.empty().collect(ArrayList::new, (list, s) -> list.add(s));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollectWithCombiner_Empty() {
        List<Short> result = ShortStream.empty().collect(ArrayList::new, (list, s) -> list.add(s), (l1, l2) -> l1.addAll(l2));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollectWithoutCombiner() {
        ShortList result = ShortStream.of((short) 1, (short) 2, (short) 3).collect(ShortList::new, ShortList::add);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testCollectWithSupplierAccumulatorCombiner() {
        List<Short> list = createShortStream((short) 1, (short) 2, (short) 3).collect(ArrayList::new, List::add, List::addAll);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testCollectWithSupplierAccumulatorMethod() {
        List<Short> result = createShortStream((short) 1, (short) 2, (short) 3).collect(ArrayList::new, (list, s) -> list.add(s));
        assertEquals(3, result.size());
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        ShortList list = createShortStream((short) 1, (short) 2, (short) 3).collect(ShortList::new, ShortList::add);
        assertArrayEquals(new short[] { 1, 2, 3 }, list.toArray());
    }

    // groupTo

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Short>> map = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).groupTo(n -> n % 2 == 0, Collectors.toList());
        assertEquals(Arrays.asList((short) 2, (short) 4), map.get(true));
        assertEquals(Arrays.asList((short) 1, (short) 3), map.get(false));
    }

    @Test
    public void testGroupTo_Empty() {
        Map<Boolean, List<Short>> result = ShortStream.empty().boxed().groupTo(s -> s > 2);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupToWithFactory() {
        Map<Boolean, List<Short>> map = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)
                .groupTo(n -> n % 2 == 0, Collectors.toList(), LinkedHashMap::new);
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testGroupToWithFactoryMethod() {
        Map<Boolean, List<Short>> result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).boxed()
                .groupTo(s -> s > 2, Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertTrue(result.containsKey(true));
        assertTrue(result.containsKey(false));
    }

    @Test
    public void testToArray() {
        short[] array = ShortStream.of((short) 1, (short) 2, (short) 3).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, array);
    }

    @Test
    @DisplayName("Test toCollection() method")
    public void testToCollection() {
        LinkedList<Short> linkedList = stream.toCollection(LinkedList::new);
        assertEquals(5, linkedList.size());
        assertEquals((short) 1, linkedList.getFirst());
        assertEquals((short) 5, linkedList.getLast());
    }

    @Test
    @DisplayName("Test toImmutableList() method")
    public void testToImmutableList() {
        var list = stream.toImmutableList();
        assertEquals(5, list.size());
        assertThrows(UnsupportedOperationException.class, () -> list.add((short) 6));
    }

    @Test
    @DisplayName("Test toImmutableSet() method")
    public void testToImmutableSet() {
        var set = stream.toImmutableSet();
        assertEquals(5, set.size());
        assertTrue(set.contains((short) 1));
        assertThrows(UnsupportedOperationException.class, () -> set.add((short) 6));
    }

    @Test
    public void testToList() {
        List<Short> list = ShortStream.of((short) 1, (short) 2, (short) 3).toList();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testToList_Empty() {
        List<Short> result = ShortStream.empty().toList();
        assertTrue(result.isEmpty());
    }

    // toMap

    @Test
    public void testToMap() {
        Map<Short, String> map = ShortStream.of((short) 1, (short) 2, (short) 3).toMap(n -> n, n -> "V" + n);
        assertEquals("V1", map.get((short) 1));
        assertEquals("V2", map.get((short) 2));
        assertEquals("V3", map.get((short) 3));
    }

    @Test
    public void testToMap_DuplicateKeys() {
        assertThrows(Exception.class, () -> createShortStream((short) 1, (short) 1).toMap(s -> s, s -> s));
    }

    @Test
    public void testToMap_Empty() {
        Map<Short, Short> result = ShortStream.empty().toMap(s -> s, s -> s);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToMapWithFactory() {
        Map<Short, String> map = ShortStream.of((short) 1, (short) 2, (short) 3).toMap(n -> n, n -> "V" + n, Suppliers.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals("V1", map.get((short) 1));
    }

    @Test
    public void testToMapWithFactoryMethod() {
        LinkedHashMap<Short, Short> result = createShortStream((short) 1, (short) 2, (short) 3).toMap(s -> s, s -> (short) (s * 10), (a, b) -> a,
                LinkedHashMap::new);
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testToMapWithMerge() {
        Map<Short, String> map = ShortStream.of((short) 1, (short) 2, (short) 1).toMap(n -> n, n -> "V" + n, (a, b) -> a + "," + b);
        assertEquals("V1,V1", map.get((short) 1));
        assertEquals("V2", map.get((short) 2));
    }

    @Test
    public void testToMapWithMergeAndFactory() {
        Map<Short, String> map = ShortStream.of((short) 1, (short) 2, (short) 1)
                .toMap(n -> n, n -> "V" + n, (a, b) -> a + "," + b, Suppliers.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals("V1,V1", map.get((short) 1));
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<String, Integer> map = createShortStream((short) 1, (short) 2, (short) 1).toMap(n -> "key" + n, n -> (int) n, (v1, v2) -> v1 + v2);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(2), map.get("key1"));
        assertEquals(Integer.valueOf(2), map.get("key2"));
    }

    @Test
    public void testToMapWithSupplier() {
        Map<String, Short> map = createShortStream((short) 1, (short) 2, (short) 3).toMap(n -> "key" + n, n -> n, Suppliers.ofMap());

        assertTrue(map instanceof HashMap);
        assertEquals(3, map.size());
    }

    // toMultiset

    @Test
    public void testToMultiset() {
        Multiset<Short> multiset = ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3).toMultiset();
        assertEquals(1, multiset.getCount((short) 1));
        assertEquals(2, multiset.getCount((short) 2));
        assertEquals(1, multiset.getCount((short) 3));
    }

    @Test
    public void testToMultisetDefault() {
        Multiset<Short> multiset = createShortStream((short) 1, (short) 2, (short) 1).toMultiset();
        assertEquals(2, multiset.getCount((short) 1));
        assertEquals(1, multiset.getCount((short) 2));
    }

    @Test
    public void testToMultisetWithSupplier() {
        var multiset = createShortStream((short) 1, (short) 2, (short) 2, (short) 3).toMultiset(com.landawn.abacus.util.Multiset::new);
        assertEquals(1, multiset.count((short) 1));
        assertEquals(2, multiset.count((short) 2));
        assertEquals(1, multiset.count((short) 3));
    }

    @Test
    public void testToMultisetWithSupplierMethod() {
        Multiset<Short> result = createShortStream((short) 1, (short) 2, (short) 2, (short) 3, (short) 3, (short) 3).toMultiset(Suppliers.ofMultiset());
        assertEquals(1, result.getCount((short) 1));
        assertEquals(2, result.getCount((short) 2));
        assertEquals(3, result.getCount((short) 3));
    }

    @Test
    public void testToSet() {
        var set = ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3).toSet();
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 1));
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
    }

    @Test
    public void testToSet_Empty() {
        java.util.Set<Short> result = ShortStream.empty().toSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToShortList() {
        ShortList list = ShortStream.of((short) 1, (short) 2, (short) 3).toShortList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testToShortList_Empty() {
        ShortList result = ShortStream.empty().toShortList();
        assertEquals(0, result.size());
    }

}
