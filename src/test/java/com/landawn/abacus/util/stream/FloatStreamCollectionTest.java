package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Suppliers;

/** FloatStream collection contracts. */
public class FloatStreamCollectionTest extends FloatStreamTestSupport {

    // collect

    @Test
    @DisplayName("collect() with supplier, accumulator, combiner")
    public void testCollect() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        StringBuilder result = stream.collect(StringBuilder::new, (sb, f) -> sb.append(f).append(","), StringBuilder::append);
        assertEquals("1.0,2.0,3.0,", result.toString());
    }

    @Test
    public void testCollect_WithCombiner() {
        List<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).collect(ArrayList::new, (list, f) -> list.add(f), (l1, l2) -> l1.addAll(l2));
        assertEquals(3, result.size());
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).collect(ArrayList::new, (list, f) -> list.add(f));
        assertEquals(3, result.size());
    }

    @Test
    public void testCollectWithCombiner() {
        ArrayList<Float> result = FloatStream.of(1.1f, 2.2f, 3.3f).parallel().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        result.sort(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1.1f, 2.2f, 3.3f), result);
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        List<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).collect(ArrayList::new, (list, f) -> list.add(f));
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), DELTA);
    }

    // groupTo

    @Test
    @DisplayName("groupTo() should group elements")
    public void testGroupTo() {
        FloatStream stream = createFloatStream(1.1f, 1.9f, 2.1f, 2.9f);
        Map<Integer, List<Float>> result = stream.groupTo(f -> (int) f, Collectors.toList());
        assertEquals(2, result.size());
        assertEquals(2, result.get(1).size());
    }

    @Test
    public void testGroupTo_Empty() {
        Map<String, List<Float>> result = FloatStream.empty().boxed().groupTo(f -> f > 2.0f ? "big" : "small");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupTo_Test() {
        Map<Boolean, List<Float>> grouped = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList());
        assertEquals(2, grouped.size());
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        LinkedHashMap<Boolean, List<Float>> grouped = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(),
                LinkedHashMap::new);
        assertEquals(2, grouped.size());
    }

    @Test
    public void testGroupToWithFactory() {
        Map<String, FloatList> map = FloatStream.of(1f, 2f, 3f, 4f).groupTo(f -> f % 2 == 0 ? "even" : "odd", Collectors.toFloatList(), HashMap::new);

        assertTrue(map instanceof HashMap);
        assertArrayEquals(new float[] { 2f, 4f }, map.get("even").toArray(), DELTA);
        assertArrayEquals(new float[] { 1f, 3f }, map.get("odd").toArray(), DELTA);
    }

    @Test
    @DisplayName("toArray() should return array")
    public void testToArray() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testToCollection() {
        java.util.LinkedList<Float> list = createFloatStream(1.0f, 2.0f, 3.0f).boxed().toCollection(java.util.LinkedList::new);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), DELTA);
        assertEquals(3.0f, list.get(2), DELTA);
    }

    @Test
    public void testToCollection_Supplier() {
        java.util.LinkedList<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).toCollection(java.util.LinkedList::new);
        assertEquals(3, result.size());
        assertEquals(Float.valueOf(1.0f), result.get(0));
        assertEquals(Float.valueOf(3.0f), result.get(2));
    }

    @Test
    @DisplayName("toFloatList() should return FloatList")
    public void testToFloatList() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatList result = stream.toFloatList();
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), 0.001);
    }

    @Test
    public void testToFloatList_Empty() {
        FloatList result = FloatStream.empty().toFloatList();
        assertEquals(0, result.size());
    }

    @Test
    public void testToImmutableList() {
        com.landawn.abacus.util.ImmutableList<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).toImmutableList();
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), DELTA);
        assertThrows(UnsupportedOperationException.class, () -> result.add(4.0f));
    }

    @Test
    public void testToImmutableSet() {
        com.landawn.abacus.util.ImmutableSet<Float> result = createFloatStream(1.0f, 2.0f, 3.0f, 2.0f).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0f));
        assertThrows(UnsupportedOperationException.class, () -> result.add(4.0f));
    }

    @Test
    @DisplayName("toList() should return List")
    public void testToList() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        List<Float> result = stream.toList();
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), result);
    }

    @Test
    public void testToList_Empty() {
        List<Float> result = FloatStream.empty().toList();
        assertTrue(result.isEmpty());
    }

    // toMap

    @Test
    @DisplayName("toMap() should create map")
    public void testToMap() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        Map<String, Float> result = stream.toMap(f -> "key" + (int) f, f -> f * 2);
        assertEquals(3, result.size());
        assertEquals(2.0f, result.get("key1"), 0.001);
    }

    @Test
    public void testToMap_DuplicateKeys() {
        assertThrows(Exception.class, () -> createFloatStream(1.0f, 1.0f).toMap(f -> f, f -> f));
    }

    @Test
    public void testToMap_Test() {
        Map<String, Float> map = createFloatStream(1.0f, 2.0f, 3.0f).toMap(n -> "key" + n, n -> n * 10);
        assertEquals(3, map.size());
    }

    @Test
    public void testToMap_WithMapSupplier_Test() {
        Map<String, Float> map = createFloatStream(1.0f, 2.0f, 3.0f).toMap(n -> "key" + n, n -> n * 10, Suppliers.ofLinkedHashMap());
        assertEquals(3, map.size());
    }

    @Test
    public void testToMap_WithMerge_Test() {
        Map<Boolean, Float> map = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).toMap(n -> n % 2 == 0, n -> n, Float::sum);
        assertEquals(2, map.size());
    }

    @Test
    public void testToMap_WithMergeAndSupplier() {
        LinkedHashMap<Boolean, Float> map = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).toMap(n -> n % 2 == 0, n -> n, Float::sum, LinkedHashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    @DisplayName("toMap() with map factory")
    public void testToMapWithFactory() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 2.0f);
        Map<String, Float> result = stream.toMap(f -> String.valueOf((int) f), f -> f, Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testToMapWithFactoryAndMerge() {
        Map<Integer, Float> map = FloatStream.of(1.1f, 2.9f, 1.8f, 3.5f).toMap(f -> (int) Math.floor(f), f -> f, (v1, v2) -> v1 + v2, HashMap::new);

        assertEquals(1.1f + 1.8f, map.get(1), DELTA);
        assertEquals(2.9f, map.get(2), DELTA);
        assertEquals(3.5f, map.get(3), DELTA);
    }

    @Test
    @DisplayName("toMap() with merge function")
    public void testToMapWithMerge() {
        FloatStream stream = createFloatStream(1.1f, 1.9f, 2.1f);
        Map<Integer, Float> result = stream.toMap(f -> (int) f, f -> f, Float::sum);
        assertEquals(2, result.size());
        assertEquals(3.0f, result.get(1), 0.001);
    }

    @Test
    public void testToMapWithMergeAndFactory() {
        LinkedHashMap<Integer, Float> result = createFloatStream(1.0f, 2.0f, 3.0f, 1.5f).toMap(f -> (int) f, f -> f, (a, b) -> a + b, LinkedHashMap::new);
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(2.5f, result.get(1), DELTA);
    }

    @Test
    @DisplayName("toMultiset() should return multiset")
    public void testToMultiset() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 1.0f, 3.0f, 2.0f, 1.0f);
        Multiset<Float> result = stream.toMultiset();
        assertEquals(3, result.count(1.0f));
        assertEquals(2, result.count(2.0f));
        assertEquals(1, result.count(3.0f));
    }

    @Test
    public void testToMultisetWithSupplier() {
        Multiset<Float> result = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 3.0f).toMultiset(Suppliers.ofMultiset());
        assertEquals(1, result.getCount(1.0f));
        assertEquals(2, result.getCount(2.0f));
        assertEquals(3, result.getCount(3.0f));
    }

    @Test
    @DisplayName("toSet() should return Set")
    public void testToSet() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f);
        Set<Float> result = stream.toSet();
        assertEquals(new HashSet<>(Arrays.asList(1.0f, 2.0f, 3.0f)), result);
    }

    @Test
    public void testToSet_Empty() {
        Set<Float> result = FloatStream.empty().toSet();
        assertTrue(result.isEmpty());
    }

}
