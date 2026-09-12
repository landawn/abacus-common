package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class MultimapPutTest extends MultimapTestSupport {
    @Test
    public void testPut_DuplicateValueToList() {
        assertTrue(listMultimap.put("key1", 10));
        assertTrue(listMultimap.put("key1", 10));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPut_DuplicateValueToSet() {
        assertTrue(setMultimap.put("key1", 10));
        assertFalse(setMultimap.put("key1", 10));
        assertEquals(1, setMultimap.get("key1").size());
    }

    @Test
    public void testPutMap_WithValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 10);
        map.put("key2", 20);

        assertTrue(listMultimap.putAll(map));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
        assertTrue(listMultimap.get("key1").contains(10));
        assertTrue(listMultimap.get("key2").contains(20));
    }

    @Test
    public void testPutIfAbsent_NewKey() {
        assertTrue(listMultimap.putIfValueAbsent("key1", 10));
        assertTrue(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testPutIfAbsent_ExistingKeyDifferentValue() {
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.putIfValueAbsent("key1", 20));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPutMany_WithValues() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        assertTrue(listMultimap.putValues("key1", values));
        assertEquals(3, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").containsAll(values));
    }

    @Test
    public void testPutMany_ToExistingKey() {
        listMultimap.put("key1", 5);
        List<Integer> values = Arrays.asList(10, 20);
        assertTrue(listMultimap.putValues("key1", values));
        assertEquals(3, listMultimap.get("key1").size());
    }

    @Test
    public void testPutManyIfKeyAbsent_NewKey() {
        List<Integer> values = Arrays.asList(10, 20);
        assertTrue(listMultimap.putValuesIfKeyAbsent("key1", values));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPutManyIfKeyAbsent_ExistingKey() {
        listMultimap.put("key1", 5);
        List<Integer> values = Arrays.asList(10, 20);
        assertFalse(listMultimap.putValuesIfKeyAbsent("key1", values));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testPutManyMap_WithValues() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(10, 20));
        map.put("key2", Arrays.asList(30, 40));

        assertTrue(listMultimap.putValues(map));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testPutManyMultimap_WithValues() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 10);
        other.put("key1", 20);
        other.put("key2", 30);

        assertTrue(listMultimap.putValues(other));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
    }

    @Test
    public void testPutIfAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.putIfValueAbsent("a", 1));
        assertTrue(mm.containsEntry("a", 1));

        assertTrue(mm.putIfValueAbsent("a", 2));
        assertTrue(mm.containsEntry("a", 2));

        mm.put("b", 10);
        assertFalse(mm.putIfValueAbsent("b", 10));

        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        assertTrue(smm.putIfValueAbsent("x", 100));
        assertTrue(smm.containsEntry("x", 100));
        assertFalse(smm.putIfValueAbsent("x", 100));
        assertTrue(smm.putIfValueAbsent("x", 200));
    }

    @Test
    public void testPutMany_multimap() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        ListMultimap<String, Integer> otherMm = CommonUtil.newListMultimap();
        otherMm.putValues("a", Arrays.asList(1, 2));
        otherMm.put("b", 3);

        assertTrue(mm.putValues(otherMm));
        assertEquals(2, mm.get("a").size());
        assertEquals(1, mm.get("b").size());

        mm.put("a", 0);
        otherMm.clear();
        otherMm.put("a", 4);
        assertTrue(mm.putValues(otherMm));
        assertEquals(4, mm.get("a").size());
        assertTrue(mm.get("a").containsAll(Arrays.asList(0, 1, 2, 4)));

        assertFalse(mm.putValues(CommonUtil.newListMultimap()));
    }

    @Test
    public void testPut() {
        assertTrue(listMultimap.put("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 10));
        assertEquals(1, listMultimap.totalValueCount());

        assertTrue(listMultimap.put("key1", 20));
        assertEquals(2, listMultimap.totalValueCount());

        assertTrue(setMultimap.put("key1", 10));
        assertFalse(setMultimap.put("key1", 10));
        assertEquals(1, setMultimap.totalValueCount());

        assertTrue(listMultimap.put("nullVal", null));
        assertTrue(listMultimap.get("nullVal").contains(null));
        assertTrue(listMultimap.put(null, 10));
        assertTrue(listMultimap.get(null).contains(10));
    }

    @Test
    public void testPut_SingleValue() {
        assertTrue(listMultimap.put("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testPut_MultipleValuesToSameKey() {
        assertTrue(listMultimap.put("key1", 10));
        assertTrue(listMultimap.put("key1", 20));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPutIfAbsent_ExistingSameValueInList() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.putIfValueAbsent("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testPutIfAbsent_ExistingSameValueInSet() {
        setMultimap.put("key1", 10);
        assertFalse(setMultimap.putIfValueAbsent("key1", 10));
        assertEquals(1, setMultimap.get("key1").size());
    }

    @Test
    public void testPutManyIfKeyAbsent_EmptyCollection() {
        assertFalse(listMultimap.putValuesIfKeyAbsent("key1", new ArrayList<>()));
    }

    @Test
    public void testPutManyMap_SkipsEmptyCollections() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(10, 20));
        map.put("key2", new ArrayList<>());

        assertTrue(listMultimap.putValues(map));
        assertNotNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
    }

    @Test
    public void testPutMap_NullValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", null);
        map.put("key2", 20);
        assertTrue(listMultimap.putAll(map));
        assertTrue(listMultimap.get("key1").contains(null));
        assertTrue(listMultimap.get("key2").contains(20));
    }

    @Test
    public void testPutManyMap_NullValues() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        List<Integer> values = new ArrayList<>();
        values.add(null);
        values.add(10);
        map.put("key1", values);
        assertTrue(listMultimap.putValues(map));
        assertTrue(listMultimap.get("key1").contains(null));
        assertTrue(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testPutIfAbsent_WithNullValue() {
        assertTrue(listMultimap.putIfValueAbsent("key1", null));
        assertFalse(listMultimap.putIfValueAbsent("key1", null));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testPutMany_SingleElementCollection() {
        assertTrue(listMultimap.putValues("key1", Arrays.asList(10)));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testPut_singleValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.put("a", 1));
        assertTrue(mm.containsEntry("a", 1));
        assertEquals(1, mm.get("a").size());

        assertTrue(mm.put("a", 2));
        assertEquals(2, mm.get("a").size());
        assertTrue(mm.get("a").containsAll(Arrays.asList(1, 2)));

        assertTrue(mm.put("a", 1));

        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        assertTrue(smm.put("b", 10));
        assertTrue(smm.containsEntry("b", 10));
        assertFalse(smm.put("b", 10));
    }

    @Test
    public void testPut_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Map<String, Integer> mapToPut = new HashMap<>();
        mapToPut.put("a", 1);
        mapToPut.put("b", 2);

        assertTrue(mm.putAll(mapToPut));
        assertTrue(mm.containsEntry("a", 1));
        assertTrue(mm.containsEntry("b", 2));
        assertEquals(1, mm.get("a").size());

        mm.put("a", 0);
        mapToPut.put("a", 3);
        assertTrue(mm.putAll(mapToPut));
        assertEquals(3, mm.get("a").size());
        assertTrue(mm.get("a").containsAll(Arrays.asList(0, 1, 3)));

        assertFalse(mm.putAll(Collections.emptyMap()));
    }

    @Test
    public void testPutMany_collection() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Collection<Integer> values = Arrays.asList(1, 2, 1);
        assertTrue(mm.putValues("a", values));
        assertEquals(3, mm.get("a").size());
        assertTrue(mm.get("a").containsAll(Arrays.asList(1, 2)));
        assertEquals(2, Collections.frequency(mm.get("a"), 1));

        mm.put("b", 10);
        Collection<Integer> moreValues = Arrays.asList(3, 4);
        assertTrue(mm.putValues("b", moreValues));
        assertEquals(3, mm.get("b").size());
        assertTrue(mm.get("b").containsAll(Arrays.asList(10, 3, 4)));

        assertFalse(mm.putValues("c", Collections.emptyList()));
    }

    @Test
    public void testPutManyIfKeyAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Collection<Integer> values = Arrays.asList(1, 2);
        assertTrue(mm.putValuesIfKeyAbsent("a", values));
        assertEquals(2, mm.get("a").size());

        assertFalse(mm.putValuesIfKeyAbsent("a", Arrays.asList(3, 4)));
        assertEquals(2, mm.get("a").size());

        assertFalse(mm.putValuesIfKeyAbsent("b", Collections.emptyList()));
    }

    @Test
    public void testPutMany_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Map<String, Collection<Integer>> mapToPut = new HashMap<>();
        mapToPut.put("a", Arrays.asList(1, 2));
        mapToPut.put("b", Arrays.asList(3));

        assertTrue(mm.putValues(mapToPut));
        assertEquals(2, mm.get("a").size());
        assertEquals(1, mm.get("b").size());

        mapToPut.put("a", Arrays.asList(4));
        assertTrue(mm.putValues(mapToPut));
        assertEquals(3, mm.get("a").size());
        assertTrue(mm.get("a").containsAll(Arrays.asList(1, 2, 4)));

        Map<String, Collection<Integer>> mapWithEmptyColl = new HashMap<>();
        mapWithEmptyColl.put("c", Collections.emptyList());
        assertFalse(mm.putValues(mapWithEmptyColl));
        assertFalse(mm.containsKey("c"));

        assertFalse(mm.putValues(Collections.emptyMap()));
    }

    @Test
    public void testPut_SetBehavior() {
        assertTrue(setMultimap.put("key1", 100));
        assertEquals(Collections.singleton(100), setMultimap.get("key1"));

        assertFalse(setMultimap.put("key1", 100));
        assertEquals(Collections.singleton(100), setMultimap.get("key1"));
        assertEquals(1, setMultimap.get("key1").size());

        assertTrue(setMultimap.put("key1", 200));
        assertEquals(CommonUtil.toSet(100, 200), setMultimap.get("key1"));
    }

    @Test
    public void testPutMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 10);
        map.put("key2", 20);

        assertTrue(listMultimap.putAll(map));
        assertTrue(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key2", 20));

        assertFalse(listMultimap.putAll(Collections.emptyMap()));
        assertFalse(listMultimap.putAll((Map<String, Integer>) null));
    }

    @Test
    public void testPutMany() {
        Collection<Integer> values = Arrays.asList(10, 20, 30);
        assertTrue(listMultimap.putValues("key1", values));
        assertEquals(3, listMultimap.totalValueCount());

        assertTrue(listMultimap.putValues("key1", Arrays.asList(40)));
        assertEquals(4, listMultimap.totalValueCount());

        assertFalse(listMultimap.putValues("key2", Collections.emptyList()));
        assertFalse(listMultimap.putValues("key2", null));
        assertTrue(listMultimap.putValues(null, Arrays.asList(10, 20, 30)));
        assertEquals(3, listMultimap.get(null).size());
    }

    @Test
    public void testPutManyMap() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(10, 20));
        map.put("key2", Arrays.asList(30));

        assertTrue(listMultimap.putValues(map));
        assertEquals(3, listMultimap.totalValueCount());

        assertFalse(listMultimap.putValues(Collections.emptyMap()));

        map.put("key3", Collections.emptyList());
        listMultimap.putValues(map);
    }

    @Test
    public void testPutManyMultimap() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 10);
        other.put("key1", 20);
        other.put("key2", 30);

        assertTrue(listMultimap.putValues(other));
        assertEquals(3, listMultimap.totalValueCount());

        ListMultimap<String, Integer> empty = CommonUtil.newListMultimap();
        assertFalse(listMultimap.putValues(empty));
        assertFalse(listMultimap.putValues((Multimap<String, Integer, ? extends Collection<Integer>>) null));
    }

    // ===== 27 new tests following testMethodName() convention =====

    @Test
    public void testPutAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        assertTrue(listMultimap.putAll(map));
        assertEquals(3, listMultimap.keyCount());
        assertTrue(listMultimap.containsEntry("a", 1));
        assertTrue(listMultimap.containsEntry("b", 2));
        assertTrue(listMultimap.containsEntry("c", 3));
    }

    @Test
    public void testPutIfValueAbsent() {
        assertTrue(listMultimap.putIfValueAbsent("key", 1));
        assertTrue(listMultimap.putIfValueAbsent("key", 2));
        assertFalse(listMultimap.putIfValueAbsent("key", 1));
        assertEquals(2, listMultimap.get("key").size());
    }

    @Test
    public void testPutIfKeyAbsent_NewKey() {
        assertTrue(listMultimap.putIfKeyAbsent("key1", 10));
        assertTrue(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testPutIfKeyAbsent_ExistingKey() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.putIfKeyAbsent("key1", 20));
        assertEquals(1, listMultimap.get("key1").size());
        assertFalse(listMultimap.get("key1").contains(20));
    }

    @Test
    public void testPutIfKeyAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.putIfKeyAbsent("a", 1));
        assertTrue(mm.containsEntry("a", 1));

        assertFalse(mm.putIfKeyAbsent("a", 2));
        assertEquals(1, mm.get("a").size());
        assertFalse(mm.containsEntry("a", 2));
    }

    @Test
    public void testPutIfKeyAbsent_WithNullValue() {
        assertTrue(listMultimap.putIfKeyAbsent("key1", null));
        assertFalse(listMultimap.putIfKeyAbsent("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(null));
    }

    @Test
    public void testPutValues_Multimap() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("x", 10);
        other.put("x", 20);
        other.put("y", 30);

        assertTrue(listMultimap.putValues(other));
        assertEquals(2, listMultimap.get("x").size());
        assertEquals(1, listMultimap.get("y").size());

        assertFalse(listMultimap.putValues(CommonUtil.newListMultimap()));
    }

    @Test
    public void testPutValues() {
        assertTrue(listMultimap.putValues("key", Arrays.asList(1, 2, 3)));
        assertEquals(3, listMultimap.get("key").size());
        assertFalse(listMultimap.putValues("key", (Collection<Integer>) null));
        assertFalse(listMultimap.putValues("key", new ArrayList<>()));
    }

    @Test
    public void testPutValues_Map() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3));
        map.put("c", new ArrayList<>());

        assertTrue(listMultimap.putValues(map));
        assertEquals(2, listMultimap.get("a").size());
        assertEquals(1, listMultimap.get("b").size());
        assertNull(listMultimap.get("c"));
    }

    @Test
    public void testPutValues_Map_NullMap() {
        Map<String, Collection<Integer>> nullMap = null;
        assertFalse(listMultimap.putValues(nullMap));
    }

    @Test
    public void testPutValues_Multimap_NullMultimap() {
        Multimap<String, Integer, List<Integer>> nullMm = null;
        assertFalse(listMultimap.putValues(nullMm));
    }

    @Test
    public void testPutValuesIfKeyAbsent() {
        assertTrue(listMultimap.putValuesIfKeyAbsent("key", Arrays.asList(1, 2)));
        assertFalse(listMultimap.putValuesIfKeyAbsent("key", Arrays.asList(3, 4)));
        assertEquals(2, listMultimap.get("key").size());
        assertFalse(listMultimap.putValuesIfKeyAbsent("key2", new ArrayList<>()));
    }

    @Test
    public void testPutValuesIfKeyAbsent_EmptyCollection() {
        assertFalse(listMultimap.putValuesIfKeyAbsent("key1", Collections.emptyList()));
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testPutValuesIfKeyAbsent_NullCollection() {
        assertFalse(listMultimap.putValuesIfKeyAbsent("key1", null));
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testPut_rejectedValueDoesNotLeaveEmptyMapping() {
        // Rejecting Set (null into TreeSet) must not leave key -> empty collection.
        final Multimap<String, Integer, java.util.NavigableSet<Integer>> mm = CommonUtil.newMultimap(HashMap::new, TreeSet::new);

        assertThrows(NullPointerException.class, () -> mm.put("k", null));
        assertFalse(mm.containsKey("k"));
        assertNull(mm.get("k"));
    }
}
