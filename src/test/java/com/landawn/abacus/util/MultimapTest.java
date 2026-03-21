package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

public class MultimapTest extends AbstractTest {

    private ListMultimap<String, Integer> listMultimap;
    private Multimap<String, Integer, List<Integer>> multimap;
    @SuppressWarnings("rawtypes")
    private SetMultimap setMultimap;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    public void setUp() {
        listMultimap = CommonUtil.newListMultimap();
        multimap = CommonUtil.newListMultimap();
        setMultimap = CommonUtil.newSetMultimap();
    }

    private Multimap<String, Integer, List<Integer>> getTestMultimap() {
        return CommonUtil.newListMultimap();
    }

    private Multimap<String, Integer, Set<Integer>> getSetTestMultimap() {
        return CommonUtil.newSetMultimap();
    }

    @Test
    public void testGet_ReturnsBackedCollection() {
        listMultimap.put("key1", 10);
        List<Integer> values = listMultimap.get("key1");
        values.add(20);
        assertEquals(2, listMultimap.get("key1").size());
    }

    //
    //
    //
    //
    //
    //
    @Test
    public void testGet_WithNonExistentKey() {
        assertNull(listMultimap.get("nonexistent"));
    }

    @Test
    public void testGet_WithExistingKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);

        List<Integer> values = listMultimap.get("key1");
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));
    }

    @Test
    public void testGet_NullKey() {
        listMultimap.put(null, 10);
        List<Integer> values = listMultimap.get(null);
        assertNotNull(values);
        assertEquals(1, values.size());
    }

    @Test
    public void testGet() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertNull(mm.get("a"));
        mm.put("a", 1);
        List<Integer> valuesA = mm.get("a");
        assertNotNull(valuesA);
        assertEquals(1, valuesA.size());
        assertEquals(Integer.valueOf(1), valuesA.get(0));

        mm.put("a", 2);
        assertEquals(2, valuesA.size());
        assertTrue(valuesA.containsAll(Arrays.asList(1, 2)));
    }

    @Test
    public void testGetOrDefault_WithExistingKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        List<Integer> defaultValue = Arrays.asList(-1, -2);
        List<Integer> result = listMultimap.getOrDefault("key1", defaultValue);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
    }

    @Test
    public void testGetOrDefault() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        List<Integer> defaultList = new ArrayList<>(Arrays.asList(99));
        assertEquals(defaultList, mm.getOrDefault("a", defaultList));
        assertFalse(mm.containsKey("a"));

        mm.put("a", 1);
        List<Integer> valuesA = mm.get("a");
        assertEquals(valuesA, mm.getOrDefault("a", defaultList));
        assertNotEquals(defaultList, mm.getOrDefault("a", defaultList));
    }

    @Test
    public void testGetOrDefault_WithNonExistentKey() {
        List<Integer> defaultValue = Arrays.asList(-1, -2);
        List<Integer> result = listMultimap.getOrDefault("nonexistent", defaultValue);
        assertEquals(defaultValue, result);
    }

    @Test
    public void testGetOrDefault_ReturnsDefaultForNullKeyNotPresent() {
        List<Integer> defaultList = Arrays.asList(99);
        assertEquals(defaultList, listMultimap.getOrDefault("nonexistent", defaultList));
    }

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
    public void testPut_NullValue() {
        assertTrue(listMultimap.put("key1", null));
        assertTrue(listMultimap.get("key1").contains(null));
    }

    @Test
    public void testPutMap_EmptyMap() {
        assertFalse(listMultimap.putAll(new HashMap<>()));
    }

    @Test
    public void testPutMap_NullMap() {
        assertFalse(listMultimap.putAll((Map<String, Integer>) null));
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
    public void testPutMany_EmptyCollection() {
        assertFalse(listMultimap.putValues("key1", new ArrayList<>()));
    }

    @Test
    public void testPutMany_NullCollection() {
        assertFalse(listMultimap.putValues("key1", null));
    }

    @Test
    public void testPutManyIfKeyAbsent_EmptyCollection() {
        assertFalse(listMultimap.putValuesIfKeyAbsent("key1", new ArrayList<>()));
    }

    @Test
    public void testPutManyMap_EmptyMap() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        assertFalse(listMultimap.putValues(map));
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
    public void testPutManyMultimap_EmptyMultimap() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        assertFalse(listMultimap.putValues(other));
    }

    @Test
    public void testPut_NullKey() {
        assertTrue(listMultimap.put(null, 10));
        assertEquals(1, listMultimap.get(null).size());
        assertTrue(listMultimap.get(null).contains(10));
    }

    @Test
    public void testPutMany_NullKey() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        assertTrue(listMultimap.putValues(null, values));
        assertEquals(3, listMultimap.get(null).size());
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

    @Test
    public void testPutMapEmpty() {
        assertFalse(multimap.putAll(new HashMap<>()));
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
    public void testPutAll_NullMap() {
        assertFalse(listMultimap.putAll((Map<String, Integer>) null));
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
    public void testRemoveManyMap_WithValues() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(40, 50, 60));

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(10, 20));
        toRemove.put("key2", Arrays.asList(50));

        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManyMultimap_WithValues() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(40, 50));

        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.put("key1", 10);
        toRemove.put("key1", 20);
        toRemove.put("key2", 40);

        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveOneIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 30));

        assertTrue(listMultimap.removeEntriesIf((k, v) -> v.size() > 2, 10));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManyIfKeyPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 40));

        assertTrue(listMultimap.removeValuesIf(key -> key.equals("key1"), Arrays.asList(10, 20)));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(3, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManyIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30, 40));
        listMultimap.putValues("key2", Arrays.asList(10, 20));

        assertTrue(listMultimap.removeValuesIf((k, v) -> v.size() > 2, Arrays.asList(10, 20)));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testReplaceOneIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 30));

        assertTrue(listMultimap.replaceEntriesIf((k, v) -> v.size() > 2, 10, 99));
        assertEquals(Integer.valueOf(10), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
    }

    @Test
    public void testReplaceManyWithOneIfPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 40));

        assertTrue(listMultimap.replaceValuesIf(key -> key.equals("key1"), CommonUtil.toList(99)));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(99));
        assertFalse(listMultimap.get("key1").contains(30));
        assertEquals(3, listMultimap.get("key2").size());
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30, 40));
        listMultimap.putValues("key2", Arrays.asList(10, 20));

        assertTrue(listMultimap.replaceValuesIf((k, v) -> v.size() > 2, CommonUtil.toList(99)));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(99));
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testTotalCountOfValues_LargeNumbers() {
        for (int i = 0; i < 10; i++) {
            listMultimap.putValues("key" + i, Arrays.asList(1, 2, 3, 4, 5));
        }
        assertEquals(50, listMultimap.totalValueCount());
    }

    @Test
    public void testRemoveMany_PartialRemoval() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30, 40));
        assertTrue(listMultimap.removeValues("key1", Arrays.asList(15, 20, 35, 40)));
        assertEquals(2, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(10));
        assertTrue(listMultimap.get("key1").contains(30));
    }

    @Test
    public void testRemoveManyMap_PartialMatch() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(40));
        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(10, 20));
        toRemove.put("key3", Arrays.asList(50));
        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testRemoveMany_multimap() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 3, 1));
        mm.putValues("b", Arrays.asList(4, 5));

        ListMultimap<String, Integer> otherMm = CommonUtil.newListMultimap();
        otherMm.putValues("a", Arrays.asList(1, 3));
        otherMm.putValues("b", Arrays.asList(4, 5, 6));
        otherMm.put("c", 7);

        assertTrue(mm.removeValues(otherMm));
        assertEquals(1, mm.get("a").size());
        assertTrue(mm.get("a").contains(2));
        assertFalse(mm.containsKey("b"));

        assertFalse(mm.removeValues(CommonUtil.newListMultimap()));
    }

    @Test
    public void testRemoveOneIf_value_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("apple", Arrays.asList(1, 2, 1));
        mm.putValues("apricot", Arrays.asList(1, 3));
        mm.putValues("banana", Arrays.asList(1, 4));

        assertTrue(mm.removeEntriesIf(key -> key.startsWith("ap"), 1));
        assertEquals(Arrays.asList(2, 1), mm.get("apple"));
        assertEquals(Arrays.asList(3), mm.get("apricot"));
        assertFalse(mm.get("apricot").contains(1));
        assertEquals(Arrays.asList(1, 4), mm.get("banana"));

        assertTrue(mm.removeEntriesIf(key -> key.equals("apple"), 1));
        assertEquals(Arrays.asList(2), mm.get("apple"));

        assertFalse(mm.removeEntriesIf(key -> true, 99));
    }

    @Test
    public void testRemoveOneIf_value_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1));
        mm.putValues("b", Arrays.asList(1, 3));
        mm.putValues("c", Arrays.asList(2, 4));

        assertTrue(mm.removeEntriesIf((key, values) -> key.equals("a") || values.contains(3), 1));
        assertEquals(Arrays.asList(2, 1), mm.get("a"));
        assertEquals(Arrays.asList(3), mm.get("b"));
        assertEquals(Arrays.asList(2, 4), mm.get("c"));

        assertFalse(mm.removeEntriesIf((k, v) -> k.equals("non_existent"), 1));
    }

    @Test
    public void testRemoveManyIf_values_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1, 5));
        mm.putValues("b", Arrays.asList(1, 3, 5));
        mm.putValues("c", Arrays.asList(1, 4, 5));
        Collection<Integer> valuesToRemove = Arrays.asList(1, 5);

        assertTrue(mm.removeValuesIf((key, values) -> key.equals("a") || values.contains(3), valuesToRemove));
        assertEquals(Arrays.asList(2), mm.get("a"));
        assertEquals(Arrays.asList(3), mm.get("b"));
        assertEquals(Arrays.asList(1, 4, 5), mm.get("c"));
    }

    @Test
    public void testReplaceOne() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1, 3));

        assertTrue(mm.replaceEntry("a", 1, 10));
        assertEquals(Arrays.asList(10, 2, 1, 3), mm.get("a"));

        assertTrue(mm.replaceEntry("a", 1, 11));
        assertEquals(Arrays.asList(10, 2, 11, 3), mm.get("a"));

        assertFalse(mm.replaceEntry("a", 99, 100));
        assertFalse(mm.replaceEntry("b", 1, 10));

        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        smm.putValues("x", new HashSet<>(Arrays.asList(10, 20, 30)));
        assertTrue(smm.replaceEntry("x", 20, 200));
        assertTrue(smm.get("x").containsAll(Arrays.asList(10, 30, 200)));
        assertFalse(smm.get("x").contains(20));

    }

    @Test
    public void testReplaceOneIf_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("apple", Arrays.asList(1, 2, 1));
        mm.putValues("apricot", Arrays.asList(1, 3));
        mm.put("banana", 1);

        assertTrue(mm.replaceEntriesIf(key -> key.startsWith("ap"), 1, 10));
        assertEquals(Arrays.asList(10, 2, 1), mm.get("apple"));
        assertEquals(Arrays.asList(10, 3), mm.get("apricot"));
        assertEquals(Arrays.asList(1), mm.get("banana"));

        assertFalse(mm.replaceEntriesIf(key -> true, 99, 100));
    }

    @Test
    public void testReplaceOneIf_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1));
        mm.putValues("b", Arrays.asList(1, 3));
        mm.put("c", 1);

        assertTrue(mm.replaceEntriesIf((k, v) -> k.equals("c") || v.contains(2), 1, 10));
        assertEquals(Arrays.asList(10, 2, 1), mm.get("a"));
        assertEquals(Arrays.asList(1, 3), mm.get("b"));
        assertEquals(Arrays.asList(10), mm.get("c"));
    }

    //
    //
    //
    @Test
    public void testFlatForEach() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        mm.forEach((k, e) -> pairs.add(Pair.of(k, e)));

        assertEquals(3, pairs.size());
        assertTrue(pairs.contains(Pair.of("a", 1)));
        assertTrue(pairs.contains(Pair.of("a", 2)));
        assertTrue(pairs.contains(Pair.of("b", 3)));
    }

    @Test
    public void testToImmutableMap_WithSupplier_ListMultimap() {
        listMultimap.putValues("a", Arrays.asList(1, 2));
        IntFunction<Map<String, ImmutableList<Integer>>> mapSupplier = LinkedHashMap::new;

        ImmutableMap<String, ImmutableList<Integer>> immutable = listMultimap.toImmutableMap(mapSupplier);
        assertEquals(1, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableList);
        assertEquals(ImmutableList.of(1, 2), immutable.get("a"));
    }

    @Test
    public void testToImmutableMap_WithSupplier_SetMultimap() {
        setMultimap.putValues("a", Arrays.asList(1, 2));
        IntFunction<Map<String, ImmutableSet<Integer>>> mapSupplier = LinkedHashMap::new;

        ImmutableMap<String, ImmutableSet<Integer>> immutable = setMultimap.toImmutableMap(mapSupplier);
        assertEquals(1, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableSet);
        assertEquals(ImmutableSet.of(1, 2), immutable.get("a"));
    }

    @Test
    public void testReplaceManyWithOneIfPredicate() {
        multimap.putValues("key1", Arrays.asList(1, 2, 3, 4));
        multimap.putValues("key2", Arrays.asList(2, 3, 4, 5));

        assertTrue(multimap.replaceValuesIf(k -> k.equals("key1"), CommonUtil.toList(99)));
        assertEquals(1, multimap.get("key1").size());
        assertTrue(multimap.get("key1").contains(99));
        assertFalse(multimap.get("key1").contains(2));
        assertFalse(multimap.get("key1").contains(3));
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
    public void testRemoveOne_keyValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1, 3));
        assertTrue(mm.removeEntry("a", 1));
        assertEquals(3, mm.get("a").size());
        assertTrue(mm.get("a").contains(1));

        assertTrue(mm.removeEntry("a", 1));
        assertEquals(2, mm.get("a").size());
        assertFalse(mm.get("a").contains(1));

        assertFalse(mm.removeEntry("a", 99));
        assertFalse(mm.removeEntry("b", 1));

        mm.put("c", 10);
        assertTrue(mm.removeEntry("c", 10));
        assertNull(mm.get("c"));
        assertFalse(mm.containsKey("c"));
    }

    @Test
    public void testRemoveOne_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1));
        mm.put("b", 3);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        toRemove.put("b", 3);
        toRemove.put("c", 99);

        assertTrue(mm.removeEntries(toRemove));
        assertEquals(2, mm.get("a").size());
        assertTrue(mm.get("a").contains(2) && mm.get("a").contains(1));
        assertFalse(mm.containsKey("b"));

        assertFalse(mm.removeEntries(Collections.emptyMap()));

        mm.clear();
        mm.put("x", 10);
        Map<String, Integer> toRemoveNonExistentVal = CommonUtil.asMap("x", 99);
        assertFalse(mm.removeEntries(toRemoveNonExistentVal));
        assertTrue(mm.containsEntry("x", 10));
    }

    @Test
    public void testRemoveMany_keyCollection() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 3, 1, 4));

        assertTrue(mm.removeValues("a", Arrays.asList(1, 3, 99)));
        assertEquals(2, mm.get("a").size());
        assertFalse(mm.get("a").contains(1));
        assertFalse(mm.get("a").contains(3));

        mm.put("b", 10);
        mm.put("b", 20);
        assertTrue(mm.removeValues("b", Arrays.asList(20)));
        assertTrue(mm.containsKey("b"));

        assertFalse(mm.removeValues("a", Collections.emptyList()));
        assertFalse(mm.removeValues("non_existent_key", Arrays.asList(1)));

        mm.clear();
        mm.put("x", 1);
        mm.put("x", 2);
        assertFalse(mm.removeValues("x", Arrays.asList(3, 4)));
        assertEquals(2, mm.get("x").size());
    }

    @Test
    public void testRemoveMany_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 3, 1));
        mm.putValues("b", Arrays.asList(4, 5));

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("a", Arrays.asList(1, 3));
        toRemove.put("b", Arrays.asList(4, 5, 6));
        toRemove.put("c", Arrays.asList(7));

        assertTrue(mm.removeValues(toRemove));
        assertEquals(1, mm.get("a").size());
        assertTrue(mm.get("a").contains(2));
        assertFalse(mm.containsKey("b"));

        assertFalse(mm.removeValues(Collections.emptyMap()));
    }

    @Test
    public void testRemoveManyIf_values_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("apple", Arrays.asList(1, 2, 1, 5));
        mm.putValues("apricot", Arrays.asList(1, 3, 5));
        mm.putValues("banana", Arrays.asList(1, 4, 5));
        Collection<Integer> valuesToRemove = Arrays.asList(1, 5, 99);

        assertTrue(mm.removeValuesIf(key -> key.startsWith("ap"), valuesToRemove));
        assertEquals(Arrays.asList(2), mm.get("apple"));
        assertEquals(Arrays.asList(3), mm.get("apricot"));
        assertEquals(Arrays.asList(1, 4, 5), mm.get("banana"));

        assertFalse(mm.removeValuesIf(key -> true, Collections.emptyList()));
        assertFalse(mm.removeValuesIf(key -> true, Arrays.asList(100)));
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

    //
    //    @Test
    //    public void testFilter_BiPredicate_ListMultimapSpecific() {
    //        listMultimap.putValues("a", Arrays.asList(1, 2));
    //        listMultimap.putValues("b", Arrays.asList(3));
    //        ListMultimap<String, Integer> filtered = listMultimap.filter((k, v) -> k.equals("a") && v.contains(1));
    //        assertTrue(filtered instanceof ListMultimap);
    //        assertEquals(1, filtered.size());
    //        assertTrue(filtered.containsKey("a"));
    //    }

    //
    //

    @Test
    public void testToImmutableMap_ListMultimap() {
        listMultimap.putValues("a", Arrays.asList(1, 2));
        listMultimap.put("b", 3);

        ImmutableMap<String, ImmutableList<Integer>> immutable = listMultimap.toImmutableMap();
        assertEquals(2, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableList);
        assertEquals(ImmutableList.of(1, 2), immutable.get("a"));
        assertEquals(ImmutableList.of(3), immutable.get("b"));

        assertThrows(UnsupportedOperationException.class, () -> immutable.get("a").add(4));
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("c", ImmutableList.of(5)));
    }

    //
    @Test
    public void testToImmutableMap_SetMultimap() {
        setMultimap.putValues("a", Arrays.asList(1, 2, 1));
        setMultimap.put("b", 3);

        ImmutableMap<String, ImmutableSet<Integer>> immutable = setMultimap.toImmutableMap();
        assertEquals(2, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableSet);
        assertEquals(ImmutableSet.of(1, 2), immutable.get("a"));
        assertEquals(ImmutableSet.of(3), immutable.get("b"));

        assertThrows(UnsupportedOperationException.class, () -> immutable.get("a").add(4));
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("c", ImmutableSet.of(5)));
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
    public void testRemoveOne_ExistingValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.removeEntry("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
        assertFalse(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testRemoveOne_OnlyFirstOccurrence() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.removeEntry("key1", 10));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPerformanceCharacteristics() {
        int operations = 10000;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < operations; i++) {
            multimap.put("key" + (i % 100), i);
        }

        for (int i = 0; i < operations; i++) {
            multimap.get("key" + (i % 100));
        }

        for (int i = 0; i < operations / 2; i++) {
            multimap.removeEntry("key" + (i % 100), i);
        }

        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime < 5000, "Operations took too long: " + (endTime - startTime) + "ms");
    }

    @Test
    public void testRemoveOne_NonExistentKey() {
        assertFalse(listMultimap.removeEntry("key1", 10));
    }

    @Test
    public void testRemoveOne_NonExistentValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeEntry("key1", 20));
    }

    @Test
    public void testRemoveOne_LastValueRemovesKey() {
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.removeEntry("key1", 10));
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testRemoveOne_NullKey() {
        listMultimap.put(null, 10);
        assertTrue(listMultimap.removeEntry(null, 10));
        assertNull(listMultimap.get(null));
    }

    @Test
    public void testRemoveOne() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertTrue(listMultimap.removeEntry("key1", 10));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));

        assertFalse(listMultimap.removeEntry("key1", 99));
        assertFalse(listMultimap.removeEntry("nonexistent", 10));

        assertTrue(listMultimap.removeEntry("key2", 30));
        assertFalse(listMultimap.containsKey("key2"));
    }

    @Test
    public void testEmptyCollectionRemoval() {
        multimap.put("key", 1);
        multimap.removeEntry("key", 1);

        assertFalse(multimap.containsKey("key"));
        assertNull(multimap.get("key"));
    }

    @Test
    public void testMemoryEfficiency() {
        for (int i = 0; i < 100; i++) {
            multimap.put("key" + i, i);
        }

        for (int i = 0; i < 100; i++) {
            multimap.removeEntry("key" + i, i);
        }

        assertEquals(0, multimap.totalValueCount());
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testRemoveEntry() {
        listMultimap.put("key", 1);
        listMultimap.put("key", 2);
        listMultimap.put("key", 1);

        assertTrue(listMultimap.removeEntry("key", 1));
        assertEquals(2, listMultimap.get("key").size());
        assertEquals(Integer.valueOf(2), listMultimap.get("key").get(0));

        assertFalse(listMultimap.removeEntry("nonexistent", 1));
    }

    @Test
    public void testRemoveEntry_LastValueRemovesKey() {
        listMultimap.put("key", 1);
        assertTrue(listMultimap.removeEntry("key", 1));
        assertNull(listMultimap.get("key"));
        assertFalse(listMultimap.containsKey("key"));
    }

    @Test
    public void testRemoveOneMap_PartialMatch() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key3", 40);
        assertTrue(listMultimap.removeEntries(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testRemoveOneMap_EmptyMap() {
        assertFalse(listMultimap.removeEntries(new HashMap<>()));
    }

    @Test
    public void testRemoveOneMap_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeEntries(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertNull(listMultimap.get("key2"));
    }

    @Test
    public void testRemoveOneMap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeEntries(toRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeEntries(Collections.emptyMap()));
        assertFalse(listMultimap.removeEntries((Map<String, Integer>) null));
    }

    @Test
    public void testRemoveEntries() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 3);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        toRemove.put("b", 3);

        assertTrue(listMultimap.removeEntries(toRemove));
        assertEquals(1, listMultimap.get("a").size());
        assertNull(listMultimap.get("b"));

        assertFalse(listMultimap.removeEntries(new HashMap<>()));
    }

    @Test
    public void testRemoveEntries_NullMap() {
        Map<String, Integer> nullMap = null;
        assertFalse(listMultimap.removeEntries(nullMap));
    }

    @Test
    public void testRemoveAllIfPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeKeysIf(key -> key.equals("key2")));
    }

    @Test
    public void testRemoveAllIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeKeysIf((k, v) -> v.size() > 5));
    }

    @Test
    public void testRemoveAllIf_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("apple", Arrays.asList(1, 2));
        mm.putValues("apricot", Arrays.asList(3, 4));
        mm.put("banana", 5);

        assertTrue(mm.removeKeysIf(key -> key.startsWith("ap")));
        assertFalse(mm.containsKey("apple"));
        assertFalse(mm.containsKey("apricot"));
        assertTrue(mm.containsKey("banana"));

        assertFalse(mm.removeKeysIf(key -> key.startsWith("xyz")));
    }

    @Test
    public void testRemoveAllIf_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.putValues("b", Arrays.asList(3, 4, 5));
        mm.put("c", 5);

        assertTrue(mm.removeKeysIf((key, values) -> key.equals("a") || values.stream().mapToInt(i -> i).sum() > 10));
        assertFalse(mm.containsKey("a"));
        assertFalse(mm.containsKey("b"));
        assertTrue(mm.containsKey("c"));

        assertFalse(mm.removeKeysIf((k, v) -> k.equals("non_existent")));
    }

    @Test
    public void testRemoveAllIf() {
        listMultimap.put("remove_key1", 10);
        listMultimap.put("remove_key2", 20);
        listMultimap.put("keep_key", 30);

        Predicate<String> keyFilter = key -> key.startsWith("remove_");
        assertTrue(listMultimap.removeKeysIf(keyFilter));

        assertFalse(listMultimap.containsKey("remove_key1"));
        assertFalse(listMultimap.containsKey("remove_key2"));
        assertTrue(listMultimap.containsKey("keep_key"));

        assertFalse(listMultimap.removeKeysIf(key -> false));
    }

    @Test
    public void testRemoveAllIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;
        assertTrue(listMultimap.removeKeysIf(filter));

        assertFalse(listMultimap.containsKey("key1"));
        assertTrue(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeKeysIf((key, values) -> false));
    }

    @Test
    public void testRemoveAll_NonExistentKey() {
        assertNull(listMultimap.removeAll("key1"));
    }

    @Test
    public void testRemoveAll_ExistingKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);

        List<Integer> removed = listMultimap.removeAll("key1");
        assertNotNull(removed);
        assertEquals(3, removed.size());
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testRemoveAllIfPredicate_Matching() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("other", 30);

        assertTrue(listMultimap.removeKeysIf(key -> key.startsWith("key")));
        assertNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("other"));
    }

    @Test
    public void testRemoveAllIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key3", Arrays.asList(10));

        assertTrue(listMultimap.removeKeysIf((k, v) -> v.size() > 2));
        assertNotNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("key3"));
    }

    @Test
    public void testRemoveAll_NullKey() {
        listMultimap.put(null, 10);
        listMultimap.put(null, 20);
        List<Integer> removed = listMultimap.removeAll(null);
        assertNotNull(removed);
        assertEquals(2, removed.size());
        assertNull(listMultimap.get(null));
    }

    @Test
    public void testRemoveAll_key() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);

        Collection<Integer> removedA = mm.removeAll("a");
        assertNotNull(removedA);
        assertEquals(2, removedA.size());
        assertTrue(removedA.containsAll(Arrays.asList(1, 2)));
        assertFalse(mm.containsKey("a"));
        assertTrue(mm.containsKey("b"));

        assertNull(mm.removeAll("c"));
    }

    @Test
    public void testRemoveAll() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        List<Integer> removed = listMultimap.removeAll("key1");
        assertNotNull(removed);
        assertEquals(2, removed.size());
        assertTrue(removed.contains(10));
        assertTrue(removed.contains(20));
        assertFalse(listMultimap.containsKey("key1"));

        assertNull(listMultimap.removeAll("nonexistent"));
    }

    @Test
    public void testRemoveMany_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);
        listMultimap.put("key1", 40);

        assertTrue(listMultimap.removeValues("key1", Arrays.asList(10, 30)));
        assertEquals(2, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(20));
        assertTrue(listMultimap.get("key1").contains(40));
    }

    @Test
    public void testRemoveValues() {
        listMultimap.putValues("key", Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(listMultimap.removeValues("key", Arrays.asList(2, 4)));
        assertEquals(3, listMultimap.get("key").size());
        assertFalse(listMultimap.get("key").contains(2));
        assertFalse(listMultimap.get("key").contains(4));
    }

    @Test
    public void testRemoveValues_Multimap() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4, 5));

        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.putValues("a", Arrays.asList(2, 3));
        toRemove.put("b", 5);

        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("a").size());
        assertEquals(1, listMultimap.get("b").size());
    }

    @Test
    public void testRemoveMany_EmptyCollection() {
        assertFalse(listMultimap.removeValues("key1", new ArrayList<>()));
    }

    @Test
    public void testRemoveMany_NonExistentKey() {
        assertFalse(listMultimap.removeValues("key1", Arrays.asList(10, 20)));
    }

    @Test
    public void testRemoveMany_AllValuesRemovesKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.removeValues("key1", Arrays.asList(10, 20)));
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testRemoveManyMap_EmptyMap() {
        assertFalse(listMultimap.removeValues(new HashMap<>()));
    }

    @Test
    public void testRemoveManyMultimap_EmptyMultimap() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        assertFalse(listMultimap.removeValues(other));
    }

    @Test
    public void testRemoveMany_NullValues() {
        listMultimap.put("key1", null);
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        List<Integer> toRemove = new ArrayList<>();
        toRemove.add(null);
        toRemove.add(10);
        assertTrue(listMultimap.removeValues("key1", toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(20));
    }

    @Test
    public void testRemoveMany() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);

        Collection<Integer> toRemove = Arrays.asList(10, 20);
        assertTrue(listMultimap.removeValues("key1", toRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertFalse(listMultimap.containsEntry("key1", 20));
        assertTrue(listMultimap.containsEntry("key1", 30));

        assertFalse(listMultimap.removeValues("key1", Collections.emptyList()));
        assertFalse(listMultimap.removeValues("nonexistent", Arrays.asList(1, 2)));
        assertFalse(listMultimap.removeValues("key1", null));
    }

    @Test
    public void testRemoveManyMap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        listMultimap.put("key2", 40);

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(10));
        toRemove.put("key2", Arrays.asList(30, 40));

        assertTrue(listMultimap.removeValues(toRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeValues(Collections.emptyMap()));
    }

    @Test
    public void testRemoveManyMultimap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeValues(toRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        ListMultimap<String, Integer> empty = CommonUtil.newListMultimap();
        assertFalse(listMultimap.removeValues(empty));
    }

    @Test
    public void testRemoveValues_AllValuesRemovesKey() {
        listMultimap.putValues("key", Arrays.asList(1, 2));
        assertTrue(listMultimap.removeValues("key", Arrays.asList(1, 2)));
        assertNull(listMultimap.get("key"));
    }

    @Test
    public void testRemoveValues_Map() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4, 5));

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("a", Arrays.asList(1, 3));
        toRemove.put("b", Arrays.asList(4, 5));

        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("a").size());
        assertTrue(listMultimap.get("a").contains(2));
        assertNull(listMultimap.get("b"));
    }

    @Test
    public void testRemoveValues_Map_NullMap() {
        Map<String, Collection<Integer>> nullMap = null;
        assertFalse(listMultimap.removeValues(nullMap));
    }

    @Test
    public void testRemoveValues_Multimap_NullMultimap() {
        Multimap<String, Integer, List<Integer>> nullMm = null;
        assertFalse(listMultimap.removeValues(nullMm));
    }

    @Test
    public void testRemoveOneIfKeyPredicate_NoMatchingKeys() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeEntriesIf(key -> key.equals("key2"), 10));
    }

    @Test
    public void testRemoveOneIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeEntriesIf((k, v) -> v.size() > 5, 10));
    }

    @Test
    public void testRemoveOneIf() {
        listMultimap.put("prefix_key1", 10);
        listMultimap.put("prefix_key2", 10);
        listMultimap.put("other_key", 10);

        Predicate<String> keyFilter = key -> key.startsWith("prefix_");
        assertTrue(listMultimap.removeEntriesIf(keyFilter, 10));

        int countAfter = 0;
        if (listMultimap.containsEntry("prefix_key1", 10))
            countAfter++;
        if (listMultimap.containsEntry("prefix_key2", 10))
            countAfter++;
        assertTrue(countAfter < 2);
        assertTrue(listMultimap.containsEntry("other_key", 10));

        assertFalse(listMultimap.removeEntriesIf(keyFilter, 99));
    }

    @Test
    public void testRemoveEntriesIf_NoMatch() {
        listMultimap.put("a", 10);
        assertFalse(listMultimap.removeEntriesIf(key -> key.equals("z"), 10));
        assertEquals(1, listMultimap.get("a").size());
    }

    @Test
    public void testRemoveEntriesIf_BiPredicate() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(1));

        assertTrue(listMultimap.removeEntriesIf((k, v) -> v.size() > 2, 1));
        assertEquals(2, listMultimap.get("a").size());
        assertFalse(listMultimap.get("a").contains(1));
        assertEquals(1, listMultimap.get("b").size());
    }

    @Test
    public void testRemoveOneIfKeyPredicate_MatchingKeys() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 10);
        listMultimap.put("key3", 20);

        assertTrue(listMultimap.removeEntriesIf(key -> key.startsWith("key"), 10));
        assertNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("key3"));
    }

    @Test
    public void testRemoveEntriesIf() {
        listMultimap.put("a", 10);
        listMultimap.put("b", 10);
        listMultimap.put("c", 20);

        assertTrue(listMultimap.removeEntriesIf(key -> key.compareTo("c") < 0, 10));
        assertNull(listMultimap.get("a"));
        assertNull(listMultimap.get("b"));
        assertNotNull(listMultimap.get("c"));
    }

    @Test
    public void testRemoveEntriesIf_KeyPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeEntriesIf(k -> true, 1));
    }

    @Test
    public void testRemoveEntriesIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeEntriesIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true, 1));
    }

    @Test
    public void testRemoveValuesIf() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(1, 2, 3));

        assertTrue(listMultimap.removeValuesIf(key -> key.equals("a"), Arrays.asList(1, 2)));
        assertEquals(1, listMultimap.get("a").size());
        assertEquals(3, listMultimap.get("b").size());
    }

    @Test
    public void testRemoveValuesIf_BiPredicate() {
        listMultimap.putValues("small", Arrays.asList(1, 2));
        listMultimap.putValues("large", Arrays.asList(1, 2, 3, 4));

        assertTrue(listMultimap.removeValuesIf((k, v) -> v.size() > 3, Arrays.asList(1, 2)));
        assertEquals(2, listMultimap.get("small").size());
        assertEquals(2, listMultimap.get("large").size());
    }

    @Test
    public void testRemoveManyIfKeyPredicate_EmptyCollection() {
        assertFalse(listMultimap.removeValuesIf(key -> true, new ArrayList<>()));
    }

    @Test
    public void testRemoveManyIfBiPredicate_EmptyCollection() {
        assertFalse(listMultimap.removeValuesIf((k, v) -> true, new ArrayList<>()));
    }

    @Test
    public void testRemoveManyIf() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);
        listMultimap.put("key3", 30);

        Collection<Integer> valuesToRemove = Arrays.asList(10, 20);
        Predicate<String> keyFilter = key -> key.startsWith("key");

        assertTrue(listMultimap.removeValuesIf(keyFilter, valuesToRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertFalse(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsEntry("key2", 10));
        assertTrue(listMultimap.containsEntry("key3", 30));

        assertFalse(listMultimap.removeValuesIf(keyFilter, Collections.emptyList()));
        assertFalse(listMultimap.removeValuesIf(key -> false, valuesToRemove));
    }

    @Test
    public void testRemoveValuesIf_EmptyValues() {
        listMultimap.put("a", 1);
        assertFalse(listMultimap.removeValuesIf(key -> true, new ArrayList<>()));
    }

    @Test
    public void testRemoveValuesIf_KeyPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeValuesIf(k -> true, Arrays.asList(1)));
    }

    @Test
    public void testRemoveValuesIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeValuesIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true, Arrays.asList(1)));
    }

    @Test
    public void testRemoveKeysIf() {
        listMultimap.put("abc", 1);
        listMultimap.put("def", 2);
        listMultimap.put("abx", 3);

        assertTrue(listMultimap.removeKeysIf(key -> key.startsWith("ab")));
        assertNull(listMultimap.get("abc"));
        assertNull(listMultimap.get("abx"));
        assertNotNull(listMultimap.get("def"));
    }

    @Test
    public void testRemoveKeysIf_BiPredicate() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4));

        assertTrue(listMultimap.removeKeysIf((k, v) -> v.size() < 2));
        assertNotNull(listMultimap.get("a"));
        assertNull(listMultimap.get("b"));
    }

    @Test
    public void testRemoveKeysIf_EmptyMultimap() {
        assertFalse(listMultimap.removeKeysIf(k -> true));
    }

    @Test
    public void testRemoveKeysIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeKeysIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true));
    }

    @Test
    public void testRemoveKeysIf_AllKeysRemoved() {
        listMultimap.put("a", 1);
        listMultimap.put("b", 2);
        assertTrue(listMultimap.removeKeysIf(k -> true));
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testRemoveKeysIf_BiPredicate_AllKeysRemoved() {
        listMultimap.put("a", 1);
        listMultimap.put("b", 2);
        assertTrue(listMultimap.removeKeysIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true));
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testReplaceOne_InList() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 10);

        assertTrue(listMultimap.replaceEntry("key1", 10, 99));
        List<Integer> values = listMultimap.get("key1");
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(99), values.get(0));
        assertEquals(Integer.valueOf(10), values.get(2));
    }

    @Test
    public void testReplaceOne_InSet() {
        setMultimap.put("key1", 10);
        setMultimap.put("key1", 20);

        assertTrue(setMultimap.replaceEntry("key1", 10, 99));
        @SuppressWarnings("unchecked")
        Set<Integer> values = (Set<Integer>) setMultimap.get("key1");
        assertEquals(2, values.size());
        assertTrue(values.contains(99));
        assertFalse(values.contains(10));
    }

    @Test
    public void testReplaceOne_NonExistentKey() {
        assertFalse(listMultimap.replaceEntry("key1", 10, 20));
    }

    @Test
    public void testReplaceOne_NonExistentOldValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceEntry("key1", 20, 30));
    }

    @Test
    public void testReplaceOne_WithNullOldValue() {
        listMultimap.put("key1", null);
        listMultimap.put("key1", 20);

        assertTrue(listMultimap.replaceEntry("key1", null, 99));
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
    }

    @Test
    public void testReplaceOne_NullKey() {
        listMultimap.put(null, 10);
        assertTrue(listMultimap.replaceEntry(null, 10, 20));
        assertEquals(Integer.valueOf(20), listMultimap.get(null).get(0));
    }

    @Test
    public void testReplaceEdgeCases() {
        assertFalse(multimap.replaceEntry("key", 1, 2));
        assertFalse(multimap.replaceValues("key", CommonUtil.toList(1)));

        multimap.put("key", 1);
        assertTrue(multimap.replaceEntry("key", 1, 1));

        multimap.clear();
        multimap.putValues("key", Arrays.asList(1, 2, 1, 3, 1));
        assertTrue(multimap.replaceEntry("key", 1, 99));
        List<Integer> values = multimap.get("key");
        assertEquals(Integer.valueOf(99), values.get(0));
        assertEquals(Integer.valueOf(1), values.get(2));
    }

    @Test
    public void testReplaceEntry() {
        listMultimap.putValues("key", Arrays.asList(1, 2, 3));

        assertTrue(listMultimap.replaceEntry("key", 2, 99));
        assertEquals(Arrays.asList(1, 99, 3), listMultimap.get("key"));

        assertFalse(listMultimap.replaceEntry("key", 100, 200));
        assertFalse(listMultimap.replaceEntry("nonexistent", 1, 2));
    }

    @Test
    public void testReplaceWithInvalidValue() {
        Multimap<String, Integer, Set<Integer>> customMap = CommonUtil.newMultimap(HashMap::new, () -> new HashSet<Integer>() {
            @Override
            public boolean add(Integer e) {
                if (e != null && e < 0) {
                    return false;
                }
                return super.add(e);
            }
        });

        customMap.put("key", 1);
        assertThrows(IllegalStateException.class, () -> customMap.replaceEntry("key", 1, -1));
    }

    @Test
    public void testReplaceOneIfPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceEntriesIf(key -> key.equals("key2"), 10, 99));
    }

    @Test
    public void testReplaceOneIfPredicate_Matching() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 10);
        listMultimap.put("other", 10);

        assertTrue(listMultimap.replaceEntriesIf(key -> key.startsWith("key"), 10, 99));
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
        assertEquals(Integer.valueOf(10), listMultimap.get("other").get(0));
    }

    @Test
    public void testReplaceOneIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceEntriesIf((k, v) -> v.size() > 5, 10, 99));
    }

    @Test
    public void testReplaceOneIf() {
        listMultimap.put("replace_key", 10);
        listMultimap.put("keep_key", 10);

        Predicate<String> keyFilter = key -> key.startsWith("replace_");
        assertTrue(listMultimap.replaceEntriesIf(keyFilter, 10, 99));

        assertTrue(listMultimap.containsEntry("replace_key", 99));
        assertFalse(listMultimap.containsEntry("replace_key", 10));
        assertTrue(listMultimap.containsEntry("keep_key", 10));

        assertFalse(listMultimap.replaceEntriesIf(keyFilter, 999, 100));
    }

    @Test
    public void testReplaceEntriesIf() {
        listMultimap.put("a", 1);
        listMultimap.put("b", 1);
        listMultimap.put("c", 1);

        assertTrue(listMultimap.replaceEntriesIf(key -> key.compareTo("b") <= 0, 1, 99));
        assertEquals(Integer.valueOf(99), listMultimap.get("a").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("b").get(0));
        assertEquals(Integer.valueOf(1), listMultimap.get("c").get(0));
    }

    @Test
    public void testReplaceEntriesIf_BiPredicate() {
        listMultimap.putValues("a", Arrays.asList(1, 2));
        listMultimap.putValues("b", Arrays.asList(1, 2, 3, 4));

        assertTrue(listMultimap.replaceEntriesIf((k, v) -> v.size() > 3, 1, 99));
        assertEquals(Integer.valueOf(1), listMultimap.get("a").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("b").get(0));
    }

    @Test
    public void testReplaceEntriesIf_KeyPredicate_EmptyMultimap() {
        assertFalse(listMultimap.replaceEntriesIf(k -> true, 1, 2));
    }

    @Test
    public void testReplaceEntriesIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.replaceEntriesIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true, 1, 2));
    }

    @Test
    public void testReplaceValues() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        assertTrue(multimap.replaceValues("key", CommonUtil.toList(99)));
        assertEquals(1, multimap.get("key").size());
    }

    @Test
    public void testReplaceValuesWithSameCollectionInstance() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        List<Integer> sameCollection = multimap.get("key");
        assertTrue(multimap.replaceValues("key", sameCollection));
        assertEquals(Arrays.asList(1, 2, 3), multimap.get("key"));
    }

    // --- Additional missing coverage tests ---

    @Test
    public void testReplaceValues_NonExistentKey() {
        assertFalse(listMultimap.replaceValues("nokey", Arrays.asList(1, 2)));
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testReplaceValues_EmptyNewValuesRemovesKey() {
        listMultimap.putValues("key1", Arrays.asList(1, 2, 3));
        assertTrue(listMultimap.replaceValues("key1", Collections.emptyList()));
        assertNull(listMultimap.get("key1"));
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testReplaceValues_NullNewValuesRemovesKey() {
        listMultimap.putValues("key1", Arrays.asList(1, 2, 3));
        assertTrue(listMultimap.replaceValues("key1", null));
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testReplaceManyWithOneIf() {
        listMultimap.put("replace_key", 10);
        listMultimap.put("replace_key", 20);
        listMultimap.put("keep_key", 10);

        Collection<Integer> oldValues = Arrays.asList(10, 20);
        Predicate<String> keyFilter = key -> key.startsWith("replace_");

        assertTrue(listMultimap.replaceValuesIf(keyFilter, CommonUtil.toList(99)));

        Collection<Integer> values = listMultimap.get("replace_key");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.containsEntry("keep_key", 10));

        assertTrue(listMultimap.replaceValuesIf(keyFilter, CommonUtil.toList(99)));
    }

    @Test
    public void testReplaceValuesIf() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4, 5));

        assertTrue(listMultimap.replaceValuesIf(key -> key.equals("a"), Arrays.asList(99, 100)));
        assertEquals(Arrays.asList(99, 100), listMultimap.get("a"));
        assertEquals(Arrays.asList(4, 5), listMultimap.get("b"));
    }

    @Test
    public void testReplaceValuesIf_BiPredicate() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4));

        assertTrue(listMultimap.replaceValuesIf((k, v) -> v.size() > 2, Arrays.asList(99)));
        assertEquals(Arrays.asList(99), listMultimap.get("a"));
        assertEquals(Arrays.asList(4), listMultimap.get("b"));
    }

    @Test
    public void testReplaceManyWithOneIfPredicate_EmptyOldValues() {
        assertFalse(listMultimap.replaceValuesIf(key -> true, CommonUtil.toList(99)));
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate_EmptyOldValues() {
        assertFalse(listMultimap.replaceValuesIf((k, v) -> true, CommonUtil.toList(99)));
    }

    @Test
    public void testReplaceValuesIf_EmptyNewValues() {
        listMultimap.putValues("a", Arrays.asList(1, 2));
        listMultimap.putValues("b", Arrays.asList(3));

        assertTrue(listMultimap.replaceValuesIf(key -> key.equals("a"), new ArrayList<>()));
        assertNull(listMultimap.get("a"));
        assertNotNull(listMultimap.get("b"));
    }

    @Test
    public void testReplaceValuesIf_BiPredicate_EmptyNewValues() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4));

        assertTrue(listMultimap.replaceValuesIf((k, v) -> v.size() > 2, new ArrayList<>()));
        assertNull(listMultimap.get("a"));
        assertNotNull(listMultimap.get("b"));
    }

    @Test
    public void testReplaceValuesIf_KeyPredicate_EmptyMultimap() {
        assertFalse(listMultimap.replaceValuesIf(k -> true, Arrays.asList(99)));
    }

    @Test
    public void testReplaceValuesIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.replaceValuesIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true, Arrays.asList(99)));
    }

    @Test
    public void testReplaceAllWithOne_ExistingKey() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30, 40));

        assertTrue(listMultimap.replaceValues("key1", CommonUtil.toList(99)));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
    }

    @Test
    public void testReplaceAll_WithFunction() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        listMultimap.replaceAll((k, v) -> {
            List<Integer> newList = new ArrayList<>();
            newList.add(99);
            return newList;
        });

        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
    }

    @Test
    public void testReplaceAllWithOne() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertTrue(listMultimap.replaceValues("key1", CommonUtil.toList(99)));
        List<Integer> values = listMultimap.get("key1");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.containsEntry("key2", 30));
    }

    //    @Test
    //    public void testReplaceAllWithOneIfBiPredicate() {
    //        listMultimap.put("key1", 10);
    //        listMultimap.put("key1", 20);
    //        listMultimap.put("key2", 30);
    //
    //        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;
    //        assertTrue(listMultimap.replaceAllValuesIf(filter, 99));
    //
    //        Collection<Integer> values1 = listMultimap.get("key1");
    //        assertEquals(1, values1.size());
    //        assertTrue(values1.contains(99));
    //        assertTrue(listMultimap.containsEntry("key2", 30));
    //
    //        assertFalse(listMultimap.replaceAllValuesIf((key, values) -> false, 100));
    //    }

    //

    @Test
    public void testReplaceAll() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        BiFunction<String, List<Integer>, List<Integer>> function = (key, values) -> {
            if (key.equals("key1")) {
                List<Integer> newValues = new ArrayList<>();
                newValues.add(99);
                return newValues;
            }
            return values;
        };

        listMultimap.replaceAll(function);

        List<Integer> values1 = listMultimap.get("key1");
        assertEquals(1, values1.size());
        assertTrue(values1.contains(99));
        assertTrue(listMultimap.containsEntry("key2", 30));
    }

    @Test
    public void testReplaceAllWithAliasedCollection() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        multimap.replaceAll((k, v) -> v.subList(1, v.size()));
        assertEquals(Arrays.asList(2, 3), multimap.get("key"));
    }

    @Test
    public void testReplaceAllWithOne_NonExistentKey() {
        assertFalse(listMultimap.replaceValues("key1", CommonUtil.toList(99)));
    }

    //
    //
    //    @Test
    //    public void testReplaceAllWithOneIfBiPredicate_NoMatching() {
    //        listMultimap.put("key1", 10);
    //        assertFalse(listMultimap.replaceAllValuesIf((k, v) -> v.size() > 5, 99));
    //    }

    //
    //
    //
    //
    @Test
    public void testReplaceAll_EmptyMultimap() {
        listMultimap.replaceAll((k, v) -> {
            List<Integer> newList = new ArrayList<>();
            newList.add(99);
            return newList;
        });
        assertTrue(listMultimap.isEmpty());
    }

    //
    //
    @Test
    public void testReplaceAll_WithNullResults() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.replaceAll((k, v) -> null);
        assertTrue(listMultimap.isEmpty());
    }

    //
    @Test
    public void testReplaceAll_biFunction() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.putValues("b", Arrays.asList(3, 4, 5));

        mm.replaceAll((key, values) -> {
            if (key.equals("a"))
                return new ArrayList<>(Arrays.asList(values.get(0) * 10, values.get(1) * 10));
            if (key.equals("b"))
                return null;
            return values;
        });

        assertEquals(Arrays.asList(10, 20), mm.get("a"));
        assertFalse(mm.containsKey("b"));

        mm.putValues("c", Arrays.asList(1, 2));
        mm.replaceAll((k, v) -> k.equals("c") ? new ArrayList<>() : v);
        assertFalse(mm.containsKey("c"));
    }

    @Test
    public void testComputeIfAbsent_KeyPresent() {
        listMultimap.put("key1", 10);
        List<Integer> original = listMultimap.get("key1");

        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });

        assertEquals(original, result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(10), result.get(0));
    }

    @Test
    public void testComputeIfAbsent_KeyAbsent() {
        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testComputeIfAbsent_FunctionReturnsNull() {
        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> null);
        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testComputeIfAbsent_NullKey() {
        List<Integer> result = listMultimap.computeIfAbsent(null, k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testComputeIfAbsentWithEmptyReturn() {
        List<Integer> result = multimap.computeIfAbsent("key", k -> new ArrayList<>());
        assertNull(result);
        assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testComputeIfAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Function<String, List<Integer>> mappingFunc = key -> new ArrayList<>(Arrays.asList(key.length()));

        List<Integer> valA = mm.computeIfAbsent("aaa", mappingFunc);
        assertEquals(Arrays.asList(3), valA);
        assertEquals(Arrays.asList(3), mm.get("aaa"));

        List<Integer> valAExisting = mm.computeIfAbsent("aaa", mappingFunc);
        assertEquals(Arrays.asList(3), valAExisting);
        assertSame(valA, valAExisting);

        mm.computeIfAbsent("b", k -> null);
        assertFalse(mm.containsKey("b"));
        mm.computeIfAbsent("c", k -> new ArrayList<>());
        assertFalse(mm.containsKey("c"));
        assertNull(mm.computeIfAbsent("c", k -> new ArrayList<>()));

        assertThrows(IllegalArgumentException.class, () -> mm.computeIfAbsent("d", null));
    }

    @Test
    public void testComputeIfAbsentNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.computeIfAbsent("key", null));
    }

    @Test
    public void testComputeIfAbsent_NullFunction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.computeIfAbsent("key1", null));
    }

    @Test
    public void testComputeIfPresentWithAliasedCollection() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        List<Integer> result = multimap.computeIfPresent("key", (k, v) -> v.subList(1, v.size()));
        assertEquals(Arrays.asList(2, 3), result);
        assertEquals(Arrays.asList(2, 3), multimap.get("key"));
    }

    @Test
    public void testComputeIfPresent_KeyAbsent() {
        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testComputeIfPresent_KeyPresent() {
        listMultimap.put("key1", 10);

        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(99);
            return list;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(99));
    }

    @Test
    public void testComputeIfPresent_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> null);

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testComputeIfPresent_NullKey() {
        listMultimap.put(null, 10);
        List<Integer> result = listMultimap.computeIfPresent(null, (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(20);
            return list;
        });
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testComputeIfPresentRemovesKey() {
        multimap.put("key", 1);

        List<Integer> result = multimap.computeIfPresent("key", (k, v) -> null);
        assertNull(result);
        assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testComputeIfPresent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        BiFunction<String, List<Integer>, List<Integer>> remappingFunc = (key, oldValues) -> {
            List<Integer> newValues = new ArrayList<>(oldValues);
            newValues.add(key.length());
            return newValues;
        };

        assertNull(mm.computeIfPresent("absent", remappingFunc));

        mm.put("aaa", 1);
        List<Integer> valA = mm.computeIfPresent("aaa", remappingFunc);
        assertEquals(Arrays.asList(1, 3), valA);
        assertEquals(Arrays.asList(1, 3), mm.get("aaa"));

        mm.put("b", 10);
        assertNull(mm.computeIfPresent("b", (k, v) -> null));
        assertFalse(mm.containsKey("b"));

        mm.put("c", 20);
        assertNull(mm.computeIfPresent("c", (k, v) -> new ArrayList<>()));
        assertFalse(mm.containsKey("c"));

        assertThrows(IllegalArgumentException.class, () -> mm.computeIfPresent("d", null));
    }

    @Test
    public void testComputeIfPresentNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.computeIfPresent("key", null));
    }

    @Test
    public void testComputeIfPresent_NullFunction() {
        listMultimap.put("key1", 1);
        assertThrows(IllegalArgumentException.class, () -> listMultimap.computeIfPresent("key1", null));
    }

    @Test
    public void testComputeWithAliasedCollection() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        List<Integer> result = multimap.compute("key", (k, v) -> v.subList(0, 2));
        assertEquals(Arrays.asList(1, 2), result);
        assertEquals(Arrays.asList(1, 2), multimap.get("key"));
    }

    @Test
    public void testCompute_KeyAbsent() {
        List<Integer> result = listMultimap.compute("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testCompute_KeyPresent() {
        listMultimap.put("key1", 10);

        List<Integer> result = listMultimap.compute("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(99);
            return list;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testCompute_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> result = listMultimap.compute("key1", (k, v) -> null);

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testCompute_NullKey() {
        List<Integer> result = listMultimap.compute(null, (k, v) -> {
            List<Integer> list = v == null ? new ArrayList<>() : new ArrayList<>(v);
            list.add(99);
            return list;
        });
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testComputeWithComplexScenario() {
        multimap.compute("key1", (k, v) -> Arrays.asList(1, 2));
        assertEquals(2, multimap.get("key1").size());

        multimap.compute("key1", (k, v) -> {
            List<Integer> newList = new ArrayList<>(v);
            newList.add(3);
            return newList;
        });
        assertEquals(3, multimap.get("key1").size());

        multimap.compute("key1", (k, v) -> null);
        assertFalse(multimap.containsKey("key1"));
    }

    @Test
    public void testCompute_SameReferenceReturned() {
        listMultimap.putValues("key1", Arrays.asList(1, 2, 3));
        List<Integer> original = listMultimap.get("key1");
        List<Integer> result = listMultimap.compute("key1", (k, v) -> v);
        assertSame(original, result);
        assertEquals(3, listMultimap.get("key1").size());
    }

    @Test
    public void testCompute_EmptyResultRemovesKey() {
        listMultimap.putValues("key1", Arrays.asList(1, 2, 3));
        List<Integer> result = listMultimap.compute("key1", (k, v) -> new ArrayList<>());
        assertNull(result);
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testCompute() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        BiFunction<String, List<Integer>, List<Integer>> remappingFunc = (key, oldValues) -> {
            if (oldValues == null)
                return new ArrayList<>(Arrays.asList(key.length()));
            List<Integer> newValues = new ArrayList<>(oldValues);
            newValues.add(key.length() * 2);
            return newValues;
        };

        List<Integer> valAbsent = mm.compute("new", remappingFunc);
        assertEquals(Arrays.asList(3), valAbsent);
        assertEquals(Arrays.asList(3), mm.get("new"));

        List<Integer> valPresent = mm.compute("new", remappingFunc);
        assertEquals(Arrays.asList(3, 6), valPresent);
        assertEquals(Arrays.asList(3, 6), mm.get("new"));
        assertSame(mm.get("new"), valPresent);

        assertEquals(Arrays.asList(1), mm.compute("presentToNull", (k, v) -> v == null ? Arrays.asList(1) : null));
        assertEquals(Arrays.asList(1), mm.get("presentToNull"));
        assertNull(mm.compute("presentToNull", (k, v) -> null));
        assertFalse(mm.containsKey("presentToNull"));

        assertThrows(IllegalArgumentException.class, () -> mm.compute("d", null));
    }

    @Test
    public void testComputeNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.compute("key", null));
    }

    @Test
    public void testCompute_NullFunction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.compute("key1", null));
    }

    @Test
    public void testMergeCollection() {
        listMultimap.put("key1", 10);

        List<Integer> elements = Arrays.asList(20, 30);
        BiFunction<List<Integer>, List<Integer>, List<Integer>> remappingFunction = (oldValues, newElements) -> {
            List<Integer> merged = new ArrayList<>(oldValues);
            merged.addAll(newElements);
            return merged;
        };

        List<Integer> result = listMultimap.merge("key1", elements, remappingFunction);
        assertEquals(3, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));
        assertTrue(result.contains(30));

        List<Integer> result2 = listMultimap.merge("newKey", Arrays.asList(40), remappingFunction);
        assertEquals(1, result2.size());
        assertTrue(result2.contains(40));
    }

    @Test
    public void testMergeElement() {
        listMultimap.put("key1", 10);

        BiFunction<List<Integer>, Integer, List<Integer>> remappingFunction = (values, element) -> {
            List<Integer> merged = new ArrayList<>(values);
            merged.add(element);
            return merged;
        };

        List<Integer> result = listMultimap.merge("key1", 20, remappingFunction);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));

        List<Integer> result2 = listMultimap.merge("newKey", 30, remappingFunction);
        assertEquals(1, result2.size());
        assertTrue(result2.contains(30));
    }

    @Test
    public void testMergeCollection_KeyAbsent() {
        List<Integer> elements = Arrays.asList(10, 20);
        List<Integer> result = listMultimap.merge("key1", elements, (v, c) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.addAll(c);
            return merged;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testMergeCollection_KeyPresent() {
        listMultimap.put("key1", 5);
        List<Integer> elements = Arrays.asList(10, 20);

        List<Integer> result = listMultimap.merge("key1", elements, (v, c) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.addAll(c);
            return merged;
        });

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(5));
    }

    @Test
    public void testMergeElement_KeyAbsent() {
        List<Integer> result = listMultimap.merge("key1", 99, (v, e) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.add(e);
            return merged;
        });

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testMergeElement_KeyPresent() {
        listMultimap.put("key1", 10);

        List<Integer> result = listMultimap.merge("key1", 99, (v, e) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.add(e);
            return merged;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(99));
    }

    @Test
    public void testMergeCollection_NullKey() {
        List<Integer> elements = Arrays.asList(10, 20);
        List<Integer> result = listMultimap.merge(null, elements, (v, c) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.addAll(c);
            return merged;
        });
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testMergeElement_NullKey() {
        List<Integer> result = listMultimap.merge(null, 99, (v, e) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.add(e);
            return merged;
        });
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testMergeCollection_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> elements = Arrays.asList(20);
        List<Integer> result = listMultimap.merge("key1", elements, (v, c) -> null);
        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testMergeElement_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> result = listMultimap.merge("key1", 20, (v, e) -> null);
        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testConcatTwoMaps() {
        Map<String, Integer> mapA = Collections.singletonMap("a", 1);
        Map<String, Integer> mapB = Collections.singletonMap("b", 2);
        ListMultimap<String, Integer> lm = ListMultimap.merge(mapA, mapB);
        assertEquals(2, lm.totalValueCount());
        assertEquals(Arrays.asList(1), lm.get("a"));
        assertEquals(Arrays.asList(2), lm.get("b"));

        Map<String, Integer> mapC = Collections.singletonMap("a", 3);
        ListMultimap<String, Integer> lmDup = ListMultimap.merge(mapA, mapC);
        assertEquals(2, lmDup.totalValueCount());
        assertEquals(Arrays.asList(1, 3), lmDup.get("a"));

        assertEquals(Arrays.asList(1), ListMultimap.merge(mapA, null).get("a"));
        assertEquals(Arrays.asList(2), ListMultimap.merge(null, mapB).get("b"));
        assertTrue(ListMultimap.merge(null, null).isEmpty());
    }

    @Test
    public void testConcatThreeMaps() {
        Map<String, Integer> mapA = Collections.singletonMap("a", 1);
        Map<String, Integer> mapB = Collections.singletonMap("b", 2);
        Map<String, Integer> mapC = Collections.singletonMap("c", 3);
        ListMultimap<String, Integer> lm = ListMultimap.merge(mapA, mapB, mapC);
        assertEquals(3, lm.totalValueCount());
        assertEquals(Arrays.asList(3), lm.get("c"));
    }

    @Test
    public void testConcatCollectionOfMaps() {
        Collection<Map<String, Integer>> maps = Arrays.asList(Collections.singletonMap("a", 1), Collections.singletonMap("b", 2),
                Collections.singletonMap("a", 3));
        ListMultimap<String, Integer> lm = ListMultimap.merge(maps);
        assertEquals(3, lm.totalValueCount());
        assertEquals(Arrays.asList(1, 3), lm.get("a"));
        assertEquals(Arrays.asList(2), lm.get("b"));

        assertTrue(ListMultimap.merge(Collections.emptyList()).isEmpty());
    }

    @Test
    public void testConcatTwoMaps2() {
        Map<String, Integer> mapA = Collections.singletonMap("a", 1);
        Map<String, Integer> mapB = Collections.singletonMap("b", 2);
        SetMultimap<String, Integer> sm = SetMultimap.merge(mapA, mapB);
        assertEquals(2, sm.totalValueCount());
        assertEquals(Collections.singleton(1), sm.get("a"));
        assertEquals(Collections.singleton(2), sm.get("b"));

        Map<String, Integer> mapC = Collections.singletonMap("a", 1);
        SetMultimap<String, Integer> smDup = SetMultimap.merge(mapA, mapC);
        assertEquals(1, smDup.totalValueCount());
        assertEquals(Collections.singleton(1), smDup.get("a"));
    }

    @Test
    public void testMergeWithNullOldValue() {
        List<Integer> result = multimap.merge("newKey", Arrays.asList(1, 2), (old, val) -> val);
        assertEquals(2, result.size());
        assertEquals(result, multimap.get("newKey"));
    }

    @Test
    public void testMergeRemovesKey() {
        multimap.put("key", 1);

        List<Integer> result = multimap.merge("key", 2, (old, val) -> null);
        assertNull(result);
        assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testMerge_collection() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        List<Integer> elementsToMerge = Arrays.asList(10, 20);
        BiFunction<List<Integer>, List<Integer>, List<Integer>> remappingFunc = (oldVal, newElements) -> {
            List<Integer> merged = new ArrayList<>(oldVal);
            merged.addAll(newElements);
            return merged;
        };

        List<Integer> mergedValAbsent = mm.merge("a", elementsToMerge, remappingFunc);
        assertEquals(elementsToMerge, mergedValAbsent);
        assertEquals(elementsToMerge, mm.get("a"));

        List<Integer> moreElements = Arrays.asList(30);
        List<Integer> mergedValPresent = mm.merge("a", moreElements, remappingFunc);
        assertEquals(Arrays.asList(10, 20, 30), mergedValPresent);
        assertEquals(Arrays.asList(10, 20, 30), mm.get("a"));

        assertNull(mm.merge("a", Arrays.asList(40), (ov, nv) -> null));
        assertFalse(mm.containsKey("a"));

        assertThrows(IllegalArgumentException.class, () -> mm.merge("b", elementsToMerge, null));
        assertThrows(IllegalArgumentException.class, () -> mm.merge("b", null, remappingFunc));
    }

    @Test
    public void testMerge_element() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Integer elementToMerge = 100;
        BiFunction<List<Integer>, Integer, List<Integer>> remappingFunc = (oldVal, newElement) -> {
            List<Integer> merged = new ArrayList<>(oldVal);
            merged.add(newElement);
            return merged;
        };

        List<Integer> mergedValAbsent = mm.merge("x", elementToMerge, remappingFunc);
        assertEquals(Arrays.asList(100), mergedValAbsent);
        assertEquals(Arrays.asList(100), mm.get("x"));

        Integer moreElement = 200;
        List<Integer> mergedValPresent = mm.merge("x", moreElement, remappingFunc);
        assertEquals(Arrays.asList(100, 200), mergedValPresent);

        assertThrows(IllegalArgumentException.class, () -> mm.merge("y", elementToMerge, null));
    }

    @Test
    public void testMergeNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.merge("key", 1, null));
    }

    @Test
    public void testMergeCollection_NullFunction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.merge("key1", Arrays.asList(1), null));
    }

    @Test
    public void testMergeElement_NullFunction() {
        assertThrows(IllegalArgumentException.class,
                () -> listMultimap.merge("key1", 1, (BiFunction<? super List<Integer>, ? super Integer, ? extends List<Integer>>) null));
    }

    @Test
    public void testInverse_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        ListMultimap<Integer, String> inverse = listMultimap.invert(N::newListMultimap);
        assertEquals(2, inverse.keySet().size());
        assertEquals(2, inverse.get(10).size());
        assertTrue(inverse.get(10).contains("key1"));
        assertTrue(inverse.get(10).contains("key2"));
        assertEquals(1, inverse.get(20).size());
        assertTrue(inverse.get(20).contains("key1"));
    }

    //
    @Test
    public void testInverse_ListMultimapSpecific() {
        listMultimap.put("k1", 100);
        listMultimap.put("k1", 200);
        listMultimap.put("k2", 100);

        ListMultimap<Integer, String> inverted = listMultimap.invert();
        assertEquals(3, inverted.totalValueCount());
        assertEquals(Arrays.asList("k1", "k2"), inverted.get(100));
        assertEquals(Arrays.asList("k1"), inverted.get(200));
        assertTrue(inverted.get(100) instanceof List);
    }

    @Test
    public void testInverse_SetMultimapSpecific() {
        setMultimap.put("k1", 100);
        setMultimap.put("k1", 200);
        setMultimap.put("k2", 100);
        setMultimap.put("k1", 100);

        SetMultimap<Integer, String> inverted = setMultimap.invert();
        assertEquals(3, inverted.totalValueCount());
        assertEquals(CommonUtil.toSet("k1", "k2"), inverted.get(100));
        assertEquals(CommonUtil.toSet("k1"), inverted.get(200));
        assertTrue(inverted.get(100) instanceof Set);
    }

    @Test
    public void testInvert() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 1);

        ListMultimap<Integer, String> inverted = listMultimap.invert(N::newListMultimap);
        assertEquals(2, inverted.keyCount());
        assertEquals(2, inverted.get(1).size());
        assertTrue(inverted.get(1).contains("a"));
        assertTrue(inverted.get(1).contains("b"));
        assertEquals(1, inverted.get(2).size());
        assertTrue(inverted.get(2).contains("a"));
    }

    @Test
    public void testInvert_SetMultimap() {
        setMultimap.put("a", 1);
        setMultimap.put("a", 2);
        setMultimap.put("b", 1);

        @SuppressWarnings("unchecked")
        Multimap<Integer, String, Set<String>> inverse = (Multimap<Integer, String, Set<String>>) setMultimap.invert(N::newSetMultimap);
        assertEquals(2, inverse.get(1).size());
        assertTrue(inverse.get(1).contains("a"));
        assertTrue(inverse.get(1).contains("b"));
        assertEquals(1, inverse.get(2).size());
        assertTrue(inverse.get(2).contains("a"));
    }

    @Test
    public void testInverse_EmptyMultimap() {
        ListMultimap<Integer, String> inverse = listMultimap.invert(N::newListMultimap);
        assertNotNull(inverse);
        assertTrue(inverse.isEmpty());
    }

    @Test
    public void testInverse_MultipleValuesPerKey() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 1);
        listMultimap.put("b", 3);
        ListMultimap<Integer, String> inverse = listMultimap.invert(N::newListMultimap);

        assertEquals(4, inverse.totalValueCount());
        assertEquals(2, inverse.get(1).size());
        assertTrue(inverse.get(1).contains("a"));
        assertTrue(inverse.get(1).contains("b"));
    }

    @Test
    public void testInverse() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("a", 2);
        mm.put("b", 1);

        IntFunction<ListMultimap<Integer, String>> supplier = size -> CommonUtil.newListMultimap();
        Multimap<Integer, String, List<String>> inverted = mm.invert(supplier);

        assertTrue(inverted.containsEntry(1, "a"));
        assertTrue(inverted.containsEntry(1, "b"));
        assertTrue(inverted.containsEntry(2, "a"));
        assertEquals(2, inverted.get(1).size());
        assertEquals(1, inverted.get(2).size());

        Multimap<String, Integer, List<Integer>> emptyMm = getTestMultimap();
        Multimap<Integer, String, List<String>> invertedEmpty = emptyMm.invert(supplier);
        assertTrue(invertedEmpty.isEmpty());
    }

    @Test
    public void testInvert_EmptyMultimap() {
        ListMultimap<Integer, String> inverted = listMultimap.invert(N::newListMultimap);
        assertTrue(inverted.isEmpty());
    }

    @Test
    public void testCopy_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();
        assertEquals(2, copy.keySet().size());
        assertEquals(2, copy.get("key1").size());
        assertEquals(1, copy.get("key2").size());

        copy.put("key1", 99);
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(3, copy.get("key1").size());
    }

    @Test
    public void testCopy_SetMultimap() {
        setMultimap.put("a", 1);
        setMultimap.put("a", 2);
        setMultimap.put("b", 3);

        Multimap copy = setMultimap.copy();
        assertEquals(setMultimap.totalValueCount(), copy.totalValueCount());
        assertEquals(setMultimap.keyCount(), copy.keyCount());
        // Modifications to copy should not affect original
        copy.put("c", 99);
        assertFalse(setMultimap.containsKey("c"));
    }

    @Test
    public void testCopy_EmptyMultimap() {
        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();
        assertNotNull(copy);
        assertTrue(copy.isEmpty());
    }

    @Test
    public void testCopy_Independence() {
        listMultimap.put("key1", 10);
        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();

        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(1, copy.get("key1").size());
        assertNull(copy.get("key2"));
    }

    @Test
    public void testCopy() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.putValues("b", Arrays.asList(2, 3));

        Multimap<String, Integer, List<Integer>> copy = mm.copy();
        assertNotSame(mm, copy);
        assertEquals(mm, copy);

        assertNotSame(mm.get("a"), copy.get("a"));
        assertEquals(mm.get("a"), copy.get("a"));

        copy.put("a", 100);
        assertTrue(mm.get("a").contains(1));
        assertFalse(mm.get("a").contains(100));
    }

    @Test
    public void testCopy_ListMultimapSpecific() {
        listMultimap.put("a", 1);
        ListMultimap<String, Integer> copy = listMultimap.copy();
        assertNotSame(listMultimap, copy);
        assertEquals(listMultimap, copy);
        assertTrue(copy instanceof ListMultimap);
        assertEquals(Arrays.asList(1), copy.get("a"));

        assertNotSame(listMultimap.get("a"), copy.get("a"));
        assertEquals(listMultimap.get("a"), copy.get("a"));

        copy.get("a").add(2);
        assertEquals(Arrays.asList(1), listMultimap.get("a"));
    }

    @Test
    public void testCopy_SetMultimapSpecific() {
        setMultimap.put("a", 1);
        setMultimap.put("a", 1);
        SetMultimap<String, Integer> copy = setMultimap.copy();
        assertNotSame(setMultimap, copy);
        assertEquals(setMultimap, copy);
        assertTrue(copy instanceof SetMultimap);
        assertEquals(Collections.singleton(1), copy.get("a"));

        assertNotSame(setMultimap.get("a"), copy.get("a"));
        assertEquals(setMultimap.get("a"), copy.get("a"));

        copy.get("a").add(2);
        assertEquals(Collections.singleton(1), setMultimap.get("a"));
    }

    @Test
    public void testContains_ExistingKeyValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
    }

    @Test
    public void testContains_keyValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        assertTrue(mm.containsEntry("a", 1));
        assertFalse(mm.containsEntry("a", 2));
        assertFalse(mm.containsEntry("b", 1));
    }

    @Test
    public void testContainsEntry() {
        listMultimap.put("key", 1);
        listMultimap.put("key", 2);

        assertTrue(listMultimap.containsEntry("key", 1));
        assertTrue(listMultimap.containsEntry("key", 2));
        assertFalse(listMultimap.containsEntry("key", 3));
        assertFalse(listMultimap.containsEntry("other", 1));
    }

    @Test
    public void testContainsEntry_AfterReplaceEntry() {
        listMultimap.put("key1", 10);
        listMultimap.replaceEntry("key1", 10, 20);
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
    }

    @Test
    public void testContains_NonExistentKey() {
        assertFalse(listMultimap.containsEntry("key1", 10));
    }

    @Test
    public void testContains_NonExistentValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.containsEntry("key1", 20));
    }

    @Test
    public void testContains_NullValue() {
        listMultimap.put("key1", null);
        assertTrue(listMultimap.containsEntry("key1", null));
        assertFalse(listMultimap.containsEntry("key2", null));
    }

    @Test
    public void testContains() {
        listMultimap.put("key1", 10);

        assertTrue(listMultimap.containsEntry("key1", 10));
        assertFalse(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsEntry("key2", 10));
        assertFalse(listMultimap.containsEntry("key1", null));
        assertFalse(listMultimap.containsEntry(null, 10));
    }

    @Test
    public void testNullValues() {
        multimap.put("key", null);
        assertTrue(multimap.containsEntry("key", null));
        assertEquals(1, multimap.get("key").size());
    }

    @Test
    public void testBoundaryConditions() {
        multimap.put("", 0);
        assertTrue(multimap.containsEntry("", 0));

        multimap.put(null, 1);
        assertTrue(multimap.containsEntry(null, 1));

        assertTrue(multimap.removeEntry(null, 1));
        assertFalse(multimap.containsKey(null));
    }

    @Test
    public void testContainsEntry_NullValue() {
        listMultimap.put("key", null);
        assertTrue(listMultimap.containsEntry("key", null));
    }

    @Test
    public void testContainsKey_Existing() {
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsKey_AfterRemoval() {
        listMultimap.put("key1", 10);
        listMultimap.removeAll("key1");
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsKey() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        assertTrue(mm.containsKey("a"));
        assertFalse(mm.containsKey("b"));
    }

    @Test
    public void testContainsKey_NonExistent() {
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsKey_NullKey() {
        assertFalse(listMultimap.containsKey(null));
        listMultimap.put(null, 10);
        assertTrue(listMultimap.containsKey(null));
    }

    @Test
    public void testNullHandling() {
        assertFalse(listMultimap.containsKey(null));
        assertFalse(listMultimap.containsValue(null));
        assertNull(listMultimap.get(null));
        assertFalse(listMultimap.containsEntry(null, 10));
        assertFalse(listMultimap.containsEntry("key", null));

        assertFalse(listMultimap.putValues("key", null));
        assertFalse(listMultimap.removeValues("key", null));
    }

    @Test
    public void testContainsValue_Existing() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        assertTrue(listMultimap.containsValue(10));
        assertTrue(listMultimap.containsValue(20));
        assertFalse(listMultimap.containsValue(30));
    }

    @Test
    public void testContainsValue_element() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("b", 2);
        mm.put("c", 1);
        assertTrue(mm.containsValue(1));
        assertTrue(mm.containsValue(2));
        assertFalse(mm.containsValue(3));
    }

    @Test
    public void testContainsValue_NonExistent() {
        assertFalse(listMultimap.containsValue(10));
    }

    @Test
    public void testContainsValue_Null() {
        assertFalse(listMultimap.containsValue(null));
        listMultimap.put("key1", null);
        assertTrue(listMultimap.containsValue(null));
    }

    @Test
    public void testContainsValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        assertTrue(listMultimap.containsValue(10));
        assertTrue(listMultimap.containsValue(20));
        assertFalse(listMultimap.containsValue(30));
        assertFalse(listMultimap.containsValue(null));
    }

    @Test
    public void testFlatForEach_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEach((k, e) -> count.incrementAndGet());
        assertEquals(3, count.get());
    }

    @Test
    public void testFlatForEach_ModificationDuringIteration() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        List<Integer> collected = new ArrayList<>();
        listMultimap.forEach((k, e) -> collected.add(e));
        assertEquals(3, collected.size());
        assertTrue(collected.contains(10));
        assertTrue(collected.contains(20));
        assertTrue(collected.contains(30));
    }

    @Test
    public void testForEach() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 3);

        List<String> keys = new ArrayList<>();
        List<Integer> vals = new ArrayList<>();
        listMultimap.forEach((k, e) -> {
            keys.add(k);
            vals.add(e);
        });

        assertEquals(3, keys.size());
        assertEquals(3, vals.size());
        assertTrue(vals.containsAll(Arrays.asList(1, 2, 3)));
    }

    //
    //
    //
    //
    //
    //
    @Test
    public void testFlatForEach_EmptyMultimap() {
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEach((k, e) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testAllExceptionScenarios() {
        //        try {
        //            multimap.forEach((BiConsumer) null);
        //            fail("Should throw IllegalArgumentException");
        //        } catch (IllegalArgumentException e) {
        //        }

        //        try {
        //            multimap.forEachKey(null);
        //            fail("Should throw IllegalArgumentException");
        //        } catch (IllegalArgumentException e) {
        //        }
        //
        //        try {
        //            multimap.forEachValues(null);
        //            fail("Should throw IllegalArgumentException");
        //        } catch (IllegalArgumentException e) {
        //        }

        //    try {
        //        multimap.flatForEachValue(null);
        //        fail("Should throw IllegalArgumentException");
        //    } catch (IllegalArgumentException e) {
        //    }
    }

    @Test
    public void testForEach_NullAction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.forEach((java.util.function.BiConsumer<? super String, ? super Integer>) null));
    }

    @Test
    public void testKeySet_WithKeys() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key1", 30);

        Set<String> keys = listMultimap.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    //
    @Test
    public void testKeySet() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("b", 2);
        assertEquals(CommonUtil.toSet("a", "b"), mm.keySet());
        mm.keySet().remove("a");
        assertFalse(mm.containsKey("a"));
    }

    @Test
    public void testInternalMapSupplier() {
        Multimap<String, Integer, List<Integer>> treeMultimap = CommonUtil.newMultimap(TreeMap::new, ArrayList::new);

        treeMultimap.put("c", 3);
        treeMultimap.put("a", 1);
        treeMultimap.put("b", 2);

        Iterator<String> keyIter = treeMultimap.keySet().iterator();
        assertEquals("a", keyIter.next());
        assertEquals("b", keyIter.next());
        assertEquals("c", keyIter.next());
    }

    @Test
    public void test_01() {
        Multimap<String, Integer, List<Integer>> map = CommonUtil.newListMultimap();
        map.put("a", 1);
        map.put("a", 2);
        map.put("a", 3);

        map.put("b", 4);
        map.put("b", 5);
        map.put("b", 6);

        map.put("c", 7);
        map.put("c", 8);
        map.put("c", 9);

        List<Integer> list = map.get("a");

        N.println(list);

        assertEquals(CommonUtil.toList(1, 2, 3), list);

        N.println(map.keySet());

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(5));

        assertFalse(map.containsKey("e"));
        assertFalse(map.containsValue(0));

        N.println(map.totalValueCount());
        N.println(map.isEmpty());

        Multimap<String, Integer, Set<Integer>> map2 = CommonUtil.newSetMultimap();
        map2.put("a", 11);
        map2.put("a", 12);
        map2.put("a", 13);

        map2.put("b", 15);

        map2.put("d", 20);
        map.putValues(map2);

        N.println(map);

        N.println(map.hashCode());
        N.println(map.equals(null));
        N.println(map.toString());

        map.removeAll("a");
        assertNull(map.get("a"));

        map.clear();

        assertEquals(0, map.totalValueCount());
    }

    //
    //
    //
    //
    //
    //
    //
    //
    @Test
    public void testKeySet_EmptyMultimap() {
        Set<String> keys = listMultimap.keySet();
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testKeySet_BackingBehavior() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        Set<String> keys = listMultimap.keySet();
        assertEquals(2, keys.size());
        listMultimap.removeAll("key1");
        assertNotNull(keys);
    }

    @Test
    public void testConstructorWithMapAndCollectionTypes() {
        Multimap<String, Integer, Set<Integer>> mm = CommonUtil.newMultimap(TreeMap::new, TreeSet::new);
        assertNotNull(mm);
        mm.put("a", 1);
        mm.put("b", 2);
        assertEquals("a", mm.keySet().iterator().next());
    }

    @Test
    public void testValueCollections_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<List<Integer>> collections = listMultimap.valueCollections();
        assertEquals(2, collections.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValueCollections_SetMultimap() {
        setMultimap.put("key1", 10);
        setMultimap.put("key1", 20);
        setMultimap.put("key2", 30);

        Collection<Set<Integer>> collections = setMultimap.valueCollections();
        assertEquals(2, collections.size());
    }

    //
    //
    //
    @Test
    public void testValueCollections_EmptyMultimap() {
        Collection<List<Integer>> collections = listMultimap.valueCollections();
        assertNotNull(collections);
        assertTrue(collections.isEmpty());
    }

    @Test
    public void testValues_ReflectsLiveChanges() {
        listMultimap.put("key1", 1);
        Collection<Integer> values = listMultimap.allValues();
        assertEquals(1, values.size());

        listMultimap.put("key1", 2);
        listMultimap.put("key2", 3);
        assertEquals(3, values.size());
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));

        listMultimap.removeEntry("key1", 1);
        assertEquals(2, values.size());

        listMultimap.removeAll("key2");
        assertEquals(1, values.size());
        assertTrue(values.contains(2));
    }

    @Test
    public void testValues_IncludesDuplicatesForListMultimap() {
        listMultimap.put("key1", 1);
        listMultimap.put("key1", 1);
        listMultimap.put("key1", 2);

        Collection<Integer> values = listMultimap.allValues();
        assertEquals(3, values.size());
        assertEquals(2, Collections.frequency(values, 1));
        assertEquals(1, Collections.frequency(values, 2));
    }

    @Test
    public void testValues_IncludesDuplicatesAcrossKeysForSetMultimap() {
        setMultimap.put("key1", 1);
        setMultimap.put("key2", 1);
        setMultimap.put("key2", 2);

        Collection<Integer> values = setMultimap.allValues();
        assertEquals(3, values.size());
        assertEquals(2, Collections.frequency(values, 1));
    }

    @Test
    public void testAllValues() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 3);

        Collection<Integer> values = listMultimap.allValues();
        assertEquals(3, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
    }

    @Test
    public void testValues_ReturnsSameInstance() {
        Collection<Integer> values1 = listMultimap.allValues();
        Collection<Integer> values2 = listMultimap.allValues();
        assertSame(values1, values2);
    }

    @Test
    public void testValues_SupportsNullValues() {
        listMultimap.put("key1", null);
        Collection<Integer> values = listMultimap.allValues();
        assertTrue(values.contains(null));
        assertEquals(1, values.size());
    }

    @Test
    public void testAllValues_EmptyMultimap() {
        Collection<Integer> values = listMultimap.allValues();
        assertTrue(values.isEmpty());
    }

    @Test
    public void testValues_IsUnmodifiable() {
        listMultimap.put("key1", 1);
        Collection<Integer> values = listMultimap.allValues();

        assertThrows(UnsupportedOperationException.class, () -> values.add(2));
        assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
        assertThrows(UnsupportedOperationException.class, values::clear);
    }

    @Test
    public void testAllValues_Unmodifiable() {
        listMultimap.put("key", 1);
        Collection<Integer> values = listMultimap.allValues();
        assertThrows(UnsupportedOperationException.class, () -> values.add(2));
    }

    @Test
    public void testFlatValues_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<Integer> values = listMultimap.allValues();
        assertEquals(3, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));
        assertTrue(values.contains(30));
    }

    @Test
    public void testFlatValuesWithSupplier_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Set<Integer> values = listMultimap.flatValues(HashSet::new);
        assertEquals(3, values.size());
        assertTrue(values.contains(10));
    }

    @Test
    public void testFlatValues() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        Collection<Integer> flat = mm.allValues();
        assertEquals(3, flat.size());
        assertTrue(flat.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testFlatValues_supplier() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        HashSet<Integer> flatSet = mm.flatValues(HashSet::new);
        assertEquals(3, flatSet.size());
        assertTrue(flatSet.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testFlatValuesWithSupplier() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        TreeSet<Integer> result = listMultimap.flatValues(size -> new TreeSet<>());
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));
    }

    @Test
    public void testFlatValues_TreeSetSupplier() {
        listMultimap.put("b", 3);
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);

        TreeSet<Integer> sorted = listMultimap.flatValues(size -> new TreeSet<>());
        assertEquals(3, sorted.size());
        assertEquals(Integer.valueOf(1), sorted.first());
        assertEquals(Integer.valueOf(3), sorted.last());
    }

    @Test
    public void testFlatValues_EmptyMultimap() {
        Collection<Integer> values = listMultimap.allValues();
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void testFlatValuesWithSupplier_EmptyMultimap() {
        Set<Integer> values = listMultimap.flatValues(HashSet::new);
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void testFlatValues_unmodifiableList() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        Collection<Integer> values = listMultimap.allValues();
        assertThrows(UnsupportedOperationException.class, () -> values.add(30));
    }

    @Test
    public void testEntryStream_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        EntryStream<String, Integer> stream = listMultimap.entryStream();
        assertEquals(3, stream.count());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEntryStream_SetMultimap() {
        setMultimap.put("key1", 10);
        setMultimap.put("key1", 20);
        setMultimap.put("key2", 30);

        EntryStream<String, Integer> stream = setMultimap.entryStream();
        assertEquals(3, stream.count());
    }

    @Test
    public void testEntryStream_CollectsCorrectKeyValuePairs() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        List<Map.Entry<String, Integer>> collected = new ArrayList<>();
        listMultimap.entryStream().forEach(e -> collected.add(N.newEntry(e.getKey(), e.getValue())));
        assertEquals(3, collected.size());
        long key1Count = collected.stream().filter(e -> "key1".equals(e.getKey())).count();
        long key2Count = collected.stream().filter(e -> "key2".equals(e.getKey())).count();
        assertEquals(2, key1Count);
        assertEquals(1, key2Count);
    }

    @Test
    public void testEntryStream_EmptyMultimap() {
        EntryStream<String, Integer> stream = listMultimap.entryStream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testStream_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        long count = listMultimap.stream().count();
        assertEquals(2, count);
    }

    @Test
    public void testStream_Operations() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key3", 30);
        long count = listMultimap.stream().filter(e -> e.getKey().startsWith("key")).count();
        assertEquals(3, count);
    }

    @Test
    public void testStream() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        assertEquals(2, mm.stream().count());
    }

    @Test
    public void testStream_EmptyMultimap() {
        Stream<Map.Entry<String, List<Integer>>> stream = listMultimap.stream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testStreamOperations() {
        multimap.putValues("key1", Arrays.asList(1, 2, 3));
        multimap.putValues("key2", Arrays.asList(4, 5));
        multimap.putValues("key3", Arrays.asList(6));

        int sum = multimap.stream().mapToInt(e -> e.getValue().size()).sum();
        assertEquals(6, sum);

        //        Map<String, Integer> maxValues = multimap.entryStream().entries().toMap(Map.Entry::getKey, e -> e.getValue().stream().max(Integer::compare).orElse(0));
        //
        //        assertEquals(Integer.valueOf(3), maxValues.get("key1"));
        //        assertEquals(Integer.valueOf(5), maxValues.get("key2"));
        //        assertEquals(Integer.valueOf(6), maxValues.get("key3"));
    }

    @Test
    public void testIterator_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testIterator_Remove() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        if (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        assertEquals(1, listMultimap.totalValueCount());
    }

    @Test
    public void testIterator() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        Iterator<Map.Entry<String, List<Integer>>> it = mm.iterator();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testIterator_EmptyMultimap() {
        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ForEachRemainingOnEmpty() {
        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining(e -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testToMultiset_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);
        listMultimap.put("key2", 40);

        Multiset<String> multiset = listMultimap.toMultiset();
        assertEquals(3, multiset.count("key1"));
        assertEquals(1, multiset.count("key2"));
    }

    @Test
    public void testToMultiset() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1));
        mm.put("b", 3);
        Multiset<String> ms = mm.toMultiset();
        assertEquals(3, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
        assertEquals(2, ms.countOfDistinctElements());
    }

    @Test
    public void testToMultiset_AfterRemoval() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        listMultimap.removeAll("key1");

        Multiset<String> multiset = listMultimap.toMultiset();
        assertEquals(0, multiset.count("key1"));
        assertEquals(1, multiset.count("key2"));
    }

    @Test
    public void testToMultiset_EmptyMultimap() {
        Multiset<String> multiset = listMultimap.toMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testToMap_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Map<String, List<Integer>> map = listMultimap.toMap();
        assertEquals(2, map.size());
        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testToMapWithSupplier_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        HashMap<String, List<Integer>> map = listMultimap.toMap(HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testToMap_supplier() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        Map<String, List<Integer>> linkedMap = mm.toMap(LinkedHashMap::new);
        assertTrue(linkedMap instanceof LinkedHashMap);
        assertEquals(Arrays.asList(1, 2), linkedMap.get("a"));
    }

    @Test
    public void testToMapWithSupplier() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        TreeMap<String, List<Integer>> map = listMultimap.toMap(size -> new TreeMap<>());
        assertEquals(2, map.size());
        assertTrue(map instanceof TreeMap);
        assertTrue(map.get("key1").contains(10));
        assertTrue(map.get("key2").contains(20));
    }

    @Test
    public void testToMap_EmptyMultimap() {
        Map<String, List<Integer>> map = listMultimap.toMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMapWithSupplier_EmptyMultimap() {
        HashMap<String, List<Integer>> map = listMultimap.toMap(HashMap::new);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMap_Mutability() {
        listMultimap.put("key1", 10);
        Map<String, List<Integer>> map = listMultimap.toMap();
        map.put("key2", Arrays.asList(20));
        assertNull(listMultimap.get("key2"));
        assertNotNull(map.get("key2"));
    }

    @Test
    public void testToMap() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        Map<String, List<Integer>> map = mm.toMap();
        assertEquals(2, map.size());
        assertEquals(Arrays.asList(1, 2), map.get("a"));
        assertNotSame(mm.get("a"), map.get("a"));
    }

    @Test
    public void testClear_EmptyMultimap() {
        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testClear_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testClear() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.clear();
        assertTrue(mm.isEmpty());
        assertEquals(0, mm.totalValueCount());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSize_Deprecated_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        // size() returns number of distinct keys, not total values
        assertEquals(2, listMultimap.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSize_Deprecated_EqualsKeyCount() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key3", 30);

        assertEquals(listMultimap.keyCount(), listMultimap.size());
    }

    @Test
    public void testSize_AfterOperations() {
        assertEquals(0, listMultimap.totalValueCount());
        listMultimap.put("key1", 10);
        assertEquals(1, listMultimap.totalValueCount());
        listMultimap.put("key1", 20);
        assertEquals(2, listMultimap.totalValueCount());
        listMultimap.put("key2", 30);
        assertEquals(3, listMultimap.totalValueCount());
        listMultimap.removeAll("key1");
        assertEquals(1, listMultimap.totalValueCount());
    }

    @Test
    public void testSize() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertEquals(0, mm.totalValueCount());
        mm.put("a", 1);
        assertEquals(1, mm.totalValueCount());
        mm.put("b", 2);
        assertEquals(2, mm.totalValueCount());
        mm.put("a", 3);
        assertEquals(3, mm.totalValueCount());
    }

    @Test
    public void testReplaceOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;
        assertTrue(listMultimap.replaceEntriesIf(filter, 10, 99));

        assertTrue(listMultimap.containsEntry("key1", 99));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key2", 10));

        assertFalse(listMultimap.replaceEntriesIf(filter, 999, 100));
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<Integer> oldValues = Arrays.asList(10, 20);
        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;

        assertTrue(listMultimap.replaceValuesIf(filter, CommonUtil.toList(99)));

        List<Integer> values = listMultimap.get("key1");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.containsEntry("key2", 30));

        assertFalse(listMultimap.replaceValuesIf(filter, CommonUtil.toList(99)));
    }

    @Test
    public void testListBehavior() {
        multimap.put("key", 1);
        multimap.put("key", 1);
        multimap.put("key", 1);

        assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testSetBehavior() {
        setMultimap.put("key", "value");
        setMultimap.put("key", "value");
        setMultimap.put("key", "value");

        assertEquals(1, setMultimap.get("key").size());
    }

    @Test
    public void testTypeSafety() {
        Multimap<Integer, String, Set<String>> typedMultimap = CommonUtil.newMultimap(HashMap::new, HashSet::new);

        typedMultimap.put(1, "one");
        typedMultimap.put(2, "two");

        Set<String> values = typedMultimap.get(1);
        assertTrue(values instanceof Set);

        Multimap<String, Object, List<Object>> objectMultimap = CommonUtil.newListMultimap();
        objectMultimap.put("mixed", "string");
        objectMultimap.put("mixed", 123);
        objectMultimap.put("mixed", true);

        List<Object> mixedValues = objectMultimap.get("mixed");
        assertEquals(3, mixedValues.size());
        assertEquals("string", mixedValues.get(0));
        assertEquals(123, mixedValues.get(1));
        assertEquals(true, mixedValues.get(2));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSize_Deprecated_EmptyMultimap() {
        assertEquals(0, listMultimap.size());
    }

    @Test
    public void testRemoveManyIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<Integer> valuesToRemove = Arrays.asList(10, 30);
        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() >= 1;

        assertTrue(listMultimap.removeValuesIf(filter, valuesToRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsEntry("key2", 30));

        assertFalse(listMultimap.removeValuesIf(filter, Collections.emptyList()));
    }

    @Test
    public void testConstructorWithSuppliers() {
        Multimap<String, Integer, List<Integer>> mm = CommonUtil.newMultimap(() -> new HashMap<>(), () -> new ArrayList<>());
        assertNotNull(mm);
        mm.put("test", 1);
        assertEquals(1, mm.get("test").size());
    }

    @Test
    public void testKeyCount_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(2, listMultimap.keyCount());
    }

    @Test
    public void testKeyCount_AfterRemoval() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        assertEquals(2, listMultimap.keyCount());

        listMultimap.removeAll("key1");
        assertEquals(1, listMultimap.keyCount());
    }

    @Test
    public void testKeyCount_AfterClear() {
        listMultimap.put("a", 1);
        listMultimap.put("b", 2);
        listMultimap.clear();
        assertEquals(0, listMultimap.keyCount());
    }

    @Test
    public void testKeyCount_EmptyMultimap() {
        assertEquals(0, listMultimap.keyCount());
    }

    @Test
    public void testTotalCountOfValues_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(3, listMultimap.totalValueCount());
    }

    @Test
    public void testTotalCountOfValues() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertEquals(0, mm.totalValueCount());
        mm.put("a", 1);
        assertEquals(1, mm.totalValueCount());
        mm.putValues("b", Arrays.asList(2, 3));
        assertEquals(3, mm.totalValueCount());
        mm.put("a", 4);
        assertEquals(4, mm.totalValueCount());
    }

    @Test
    public void testOfFactories() {
        ListMultimap<String, Integer> lm1 = ListMultimap.of("a", 1);
        assertEquals(1, lm1.totalValueCount());
        assertEquals(Arrays.asList(1), lm1.get("a"));

        ListMultimap<String, Integer> lm2 = ListMultimap.of("a", 1, "b", 2);
        assertEquals(2, lm2.totalValueCount());
        assertEquals(Arrays.asList(1), lm2.get("a"));
        assertEquals(Arrays.asList(2), lm2.get("b"));

        ListMultimap<String, Integer> lm3 = ListMultimap.of("a", 1, "a", 2);
        assertEquals(2, lm3.totalValueCount());
        assertEquals(Arrays.asList(1, 2), lm3.get("a"));

        ListMultimap<String, Integer> lm7 = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, lm7.totalValueCount());
        assertEquals(Arrays.asList(7), lm7.get("g"));
    }

    @Test
    public void testCreateFromCollectionWithKeyExtractor2() {
        List<String> data = Arrays.asList("apple", "apricot", "banana", "apple");
        SetMultimap<Character, String> sm = SetMultimap.fromCollection(data, s -> s.charAt(0));
        assertEquals(3, sm.totalValueCount());
        assertEquals(CommonUtil.toSet("apple", "apricot"), sm.get('a'));
        assertEquals(CommonUtil.toSet("banana"), sm.get('b'));
    }

    @Test
    public void testCreateFromCollectionWithKeyAndValueExtractors2() {
        List<Pair<String, String>> data = Arrays.asList(Pair.of("fruit", "apple"), Pair.of("fruit", "banana"), Pair.of("vegetable", "carrot"),
                Pair.of("fruit", "apple"));
        SetMultimap<String, String> sm = SetMultimap.fromCollection(data, Pair::left, Pair::right);
        assertEquals(3, sm.totalValueCount());
        assertEquals(CommonUtil.toSet("apple", "banana"), sm.get("fruit"));
        assertEquals(CommonUtil.toSet("carrot"), sm.get("vegetable"));
    }

    @Test
    public void testLargeCollections() {
        for (int i = 0; i < 1000; i++) {
            listMultimap.put("key" + (i % 10), i);
        }

        assertEquals(1000, listMultimap.totalValueCount());

        //        Multimap<String, Integer, ? extends Collection<Integer>> filtered = listMultimap.filter((k, v) -> k.equals("key0"));
        //        assertEquals(1, filtered.size());
        //        assertEquals(100, filtered.totalCountOfValues());
    }

    @Test
    public void testLargeDataset() {
        int numKeys = 1000;
        int valuesPerKey = 100;

        for (int i = 0; i < numKeys; i++) {
            for (int j = 0; j < valuesPerKey; j++) {
                multimap.put("key" + i, i * valuesPerKey + j);
            }
        }

        assertEquals(numKeys * valuesPerKey, multimap.totalValueCount());

    }

    @Test
    public void testTotalValueCount_AfterRemoveEntry() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 3);
        assertEquals(3, listMultimap.totalValueCount());
        listMultimap.removeEntry("a", 1);
        assertEquals(2, listMultimap.totalValueCount());
    }

    @Test
    public void testTotalCountOfValues_EmptyMultimap() {
        assertEquals(0, listMultimap.totalValueCount());
    }

    @Test
    public void testCreateFromMap() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);
        ListMultimap<String, Integer> lm = ListMultimap.fromMap(sourceMap);
        assertEquals(2, lm.totalValueCount());
        assertEquals(Arrays.asList(1), lm.get("a"));
        assertEquals(Arrays.asList(2), lm.get("b"));

        ListMultimap<String, Integer> lmEmpty = ListMultimap.fromMap(Collections.emptyMap());
        assertTrue(lmEmpty.isEmpty());
    }

    @Test
    public void testCreateFromCollectionWithKeyAndValueExtractors() {
        List<String> data = Arrays.asList("apple:fruit", "banana:fruit", "carrot:vegetable");
        ListMultimap<String, String> lm = ListMultimap.fromCollection(data, s -> s.split(":")[1], s -> s.split(":")[0]);
        assertEquals(3, lm.totalValueCount());
        assertEquals(Arrays.asList("apple", "banana"), lm.get("fruit"));
        assertEquals(Arrays.asList("carrot"), lm.get("vegetable"));

        ListMultimap<String, String> lmEmpty = ListMultimap.fromCollection(Collections.<String> emptyList(), s -> s, s -> s);
        assertTrue(lmEmpty.isEmpty());
    }

    @Test
    public void testOfFactories2() {
        SetMultimap<String, Integer> sm1 = SetMultimap.of("a", 1);
        assertEquals(1, sm1.totalValueCount());
        assertEquals(Collections.singleton(1), sm1.get("a"));

        SetMultimap<String, Integer> sm2 = SetMultimap.of("a", 1, "b", 2);
        assertEquals(2, sm2.totalValueCount());
        assertEquals(Collections.singleton(1), sm1.get("a"));
        assertEquals(Collections.singleton(2), sm2.get("b"));

        SetMultimap<String, Integer> sm3 = SetMultimap.of("a", 1, "a", 1);
        assertEquals(1, sm3.totalValueCount());
        assertEquals(Collections.singleton(1), sm3.get("a"));

        SetMultimap<String, Integer> sm7 = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, sm7.totalValueCount());
        assertEquals(Collections.singleton(7), sm7.get("g"));
    }

    @Test
    public void testCreateFromMap2() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);
        sourceMap.put("c", 1);
        SetMultimap<String, Integer> sm = SetMultimap.fromMap(sourceMap);
        assertEquals(3, sm.totalValueCount());
        assertEquals(Collections.singleton(1), sm.get("a"));
        assertEquals(Collections.singleton(2), sm.get("b"));
        assertEquals(Collections.singleton(1), sm.get("c"));

        Map<String, Integer> mapWithDupKeyForCreate = new HashMap<>();
        mapWithDupKeyForCreate.put("x", 10);
    }

    @Test
    public void testComprehensiveScenario() {
        Multimap<String, Object, List<Object>> complexMap = CommonUtil.newListMultimap();

        complexMap.put("numbers", 1);
        complexMap.put("numbers", 2.5);
        complexMap.put("numbers", 3L);

        complexMap.put("strings", "hello");
        complexMap.put("strings", "world");

        complexMap.put("mixed", 42);
        complexMap.put("mixed", "forty-two");
        complexMap.put("mixed", true);

        assertEquals(8, complexMap.totalValueCount());

        Multimap<String, Object, List<Object>> copy = complexMap.copy();
        copy.removeAll("strings");
        assertTrue(complexMap.containsKey("strings"));
        assertFalse(copy.containsKey("strings"));

        complexMap.compute("computed", (k, v) -> {
            if (v == null) {
                return Arrays.asList("computed", "value");
            }
            return v;
        });

        assertTrue(complexMap.containsKey("computed"));
        assertEquals(2, complexMap.get("computed").size());
    }

    @Test
    public void testCreateFromCollectionWithKeyExtractor() {
        List<String> data = Arrays.asList("apple", "apricot", "banana");
        ListMultimap<Character, String> lm = ListMultimap.fromCollection(data, s -> s.charAt(0));
        assertEquals(3, lm.totalValueCount());
        assertEquals(Arrays.asList("apple", "apricot"), lm.get('a'));
        assertEquals(Arrays.asList("banana"), lm.get('b'));

        ListMultimap<Character, String> lmEmpty = ListMultimap.fromCollection(Collections.emptyList(), s -> s.charAt(0));
        assertTrue(lmEmpty.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ListMultimap.fromCollection(data, null));
    }

    @Test
    public void testIsEmpty_EmptyMultimap() {
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testIsEmpty_WithValues() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.isEmpty());
    }

    @Test
    public void testIsEmpty_AfterClear() {
        listMultimap.put("key1", 10);
        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.isEmpty());
        mm.put("a", 1);
        assertFalse(mm.isEmpty());
        mm.removeAll("a");
        assertTrue(mm.isEmpty());
    }

    @Test
    public void testDefaultConstructor() {
        Multimap<String, Integer, List<Integer>> mm = CommonUtil.newListMultimap();
        assertNotNull(mm);
        assertTrue(mm.isEmpty());
        assertEquals(0, mm.totalValueCount());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        Multimap<String, Integer, List<Integer>> mm = CommonUtil.newListMultimap();
        assertNotNull(mm);
        assertTrue(mm.isEmpty());
    }

    @Test
    public void testApply() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        Integer result = mm.apply(m -> m.totalValueCount() + m.totalValueCount());
        assertEquals(Integer.valueOf(1 + 1), result);
    }

    @Test
    public void testApply_ReturnsResult() throws Exception {
        listMultimap.put("key1", 10);

        Integer result = listMultimap.apply(m -> m.totalValueCount());
        assertEquals(1, result);
    }

    @Test
    public void testApply_NullFunction() {
        assertThrows(Exception.class, () -> listMultimap.apply(null));
    }

    @Test
    public void testApplyIfNotEmpty() {
        Multimap<String, Integer, List<Integer>> mmEmpty = getTestMultimap();
        Multimap<String, Integer, List<Integer>> mmNonEmpty = getTestMultimap();
        mmNonEmpty.put("a", 1);

        Optional<Integer> emptyResult = mmEmpty.applyIfNotEmpty(m -> m.totalValueCount());
        assertFalse(emptyResult.isPresent());

        Optional<Integer> nonEmptyResult = mmNonEmpty.applyIfNotEmpty(m -> m.totalValueCount());
        assertTrue(nonEmptyResult.isPresent());
        assertEquals(Integer.valueOf(1), nonEmptyResult.get());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyMultimap() throws Exception {
        Optional<Integer> result = listMultimap.applyIfNotEmpty(m -> m.totalValueCount());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_WithValues() throws Exception {
        listMultimap.put("key1", 10);

        Optional<Integer> result = listMultimap.applyIfNotEmpty(m -> m.totalValueCount());
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testAccept() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        AtomicBoolean accepted = new AtomicBoolean(false);
        mm.accept(m -> accepted.set(true));
        assertTrue(accepted.get());
    }

    @Test
    public void testAccept_ExecutesAction() throws Exception {
        listMultimap.put("key1", 10);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.accept(m -> count.set(m.totalValueCount()));
        assertEquals(1, count.get());
    }

    @Test
    public void testAccept_NullAction() {
        assertThrows(Exception.class, () -> listMultimap.accept(null));
    }

    @Test
    public void testAcceptIfNotEmpty() {
        Multimap<String, Integer, List<Integer>> mmEmpty = getTestMultimap();
        Multimap<String, Integer, List<Integer>> mmNonEmpty = getTestMultimap();
        mmNonEmpty.put("a", 1);
        AtomicInteger emptyCounter = new AtomicInteger(0);
        AtomicInteger nonEmptyCounter = new AtomicInteger(0);

        mmEmpty.acceptIfNotEmpty(m -> emptyCounter.incrementAndGet());
        assertEquals(0, emptyCounter.get());

        mmNonEmpty.acceptIfNotEmpty(m -> nonEmptyCounter.incrementAndGet());
        assertEquals(1, nonEmptyCounter.get());
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyMultimap() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.incrementAndGet());

        assertEquals(0, count.get());
        assertNotNull(orElse);
    }

    @Test
    public void testAcceptIfNotEmpty_WithValues() throws Exception {
        listMultimap.put("key1", 10);

        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.incrementAndGet());

        assertEquals(1, count.get());
        assertNotNull(orElse);
    }

    @Test
    public void testAcceptIfNotEmpty_WithOrElse() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.set(1));
        orElse.orElse(() -> count.set(2));
        assertEquals(2, count.get());
    }

    @Test
    public void testAcceptIfNotEmpty_NoOrElse() throws Exception {
        listMultimap.put("key1", 10);
        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.set(1));
        orElse.orElse(() -> count.set(2));
        assertEquals(1, count.get());
    }

    @Test
    public void testHashCode_EqualMultimaps() {
        listMultimap.put("key1", 10);

        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 10);

        assertEquals(listMultimap.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCode_ConsistentWithEquals() {
        listMultimap.put("key1", 10);
        ListMultimap<String, Integer> other1 = CommonUtil.newListMultimap();
        other1.put("key1", 10);
        ListMultimap<String, Integer> other2 = CommonUtil.newListMultimap();
        other2.put("key1", 20);

        assertEquals(listMultimap.hashCode(), other1.hashCode());
        assertTrue(listMultimap.equals(other1));
        assertFalse(listMultimap.equals(other2));
    }

    @Test
    public void testHashCode() {
        ListMultimap<String, Integer> mm1 = CommonUtil.newListMultimap();
        ListMultimap<String, Integer> mm2 = CommonUtil.newListMultimap();

        assertEquals(mm1.hashCode(), mm2.hashCode());

        mm1.put("key1", 10);
        mm2.put("key1", 10);
        assertEquals(mm1.hashCode(), mm2.hashCode());

        mm2.put("key2", 20);
        assertNotEquals(mm1.hashCode(), mm2.hashCode());
    }

    @Test
    public void testHashCode_EmptyMultimaps() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        assertEquals(listMultimap.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCodeAndEquals() {
        Multimap<String, Integer, List<Integer>> mm1 = CommonUtil.newListMultimap();
        mm1.putValues("a", Arrays.asList(1, 2));
        mm1.put("b", 3);

        Multimap<String, Integer, List<Integer>> mm2 = CommonUtil.newListMultimap();
        mm2.put("b", 3);
        mm2.putValues("a", Arrays.asList(1, 2));

        assertEquals(mm1.hashCode(), mm2.hashCode());
        assertTrue(mm1.equals(mm2));
        assertTrue(mm2.equals(mm1));
        assertTrue(mm1.equals(mm1));

        Multimap<String, Integer, List<Integer>> mm3 = CommonUtil.newListMultimap();
        mm3.putValues("a", Arrays.asList(1, 2));
        assertNotEquals(mm1.hashCode(), mm3.hashCode());
        assertFalse(mm1.equals(mm3));

        Multimap<String, Integer, List<Integer>> mm4 = CommonUtil.newListMultimap();
        mm4.putValues("a", Arrays.asList(1, 2, 3));
        mm4.put("b", 3);
        assertNotEquals(mm1.hashCode(), mm4.hashCode());
        assertFalse(mm1.equals(mm4));

        assertFalse(mm1.equals(null));
        assertFalse(mm1.equals(new Object()));

        Multimap<String, Integer, Set<Integer>> smm1 = CommonUtil.newSetMultimap();
        smm1.putValues("a", new HashSet<>(Arrays.asList(1, 2)));
        smm1.put("b", 3);

        Multimap<String, Integer, Set<Integer>> smm2 = CommonUtil.newSetMultimap();
        smm2.put("b", 3);
        smm2.putValues("a", new HashSet<>(Arrays.asList(2, 1)));

        assertEquals(smm1.hashCode(), smm2.hashCode());
        assertTrue(smm1.equals(smm2));

        assertFalse(mm1.equals(smm1));
    }

    @Test
    public void testEquals_DifferentType() {
        assertFalse(listMultimap.equals("not a multimap"));
    }

    @Test
    public void testEquals_EqualMultimaps() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 10);
        other.put("key2", 20);

        assertTrue(listMultimap.equals(other));
    }

    @Test
    public void testEquals_DifferentMultimaps() {
        listMultimap.put("key1", 10);

        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 20);

        assertFalse(listMultimap.equals(other));
    }

    @Test
    public void testRemoveOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> key.equals("key1") && values.size() > 1;

        assertTrue(listMultimap.removeEntriesIf(filter, 10));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertTrue(listMultimap.containsEntry("key2", 10));

        assertFalse(listMultimap.removeEntriesIf(filter, 99));
    }

    @Test
    public void testEquals_SameInstance() {
        assertTrue(listMultimap.equals(listMultimap));
    }

    @Test
    public void testEquals_Null() {
        assertFalse(listMultimap.equals(null));
    }

    @Test
    public void testEquals_EmptyMultimaps() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        assertTrue(listMultimap.equals(other));
    }

    @Test
    public void testEquals_WithSetMultimap() {
        listMultimap.put("key1", 10);
        SetMultimap<String, Integer> setMm = CommonUtil.newSetMultimap();
        setMm.put("key1", 10);
        assertNotNull(listMultimap.equals(setMm));
    }

    @Test
    public void testEquals() {
        ListMultimap<String, Integer> mm1 = CommonUtil.newListMultimap();
        ListMultimap<String, Integer> mm2 = CommonUtil.newListMultimap();

        assertTrue(mm1.equals(mm2));
        assertTrue(mm1.equals(mm1));

        mm1.put("key1", 10);
        mm1.put("key2", 20);
        mm2.put("key2", 20);
        mm2.put("key1", 10);
        assertTrue(mm1.equals(mm2));

        mm2.put("key3", 30);
        assertFalse(mm1.equals(mm2));

        assertFalse(mm1.equals(null));
        assertFalse(mm1.equals("not a multimap"));

        SetMultimap<String, Integer> setMm = CommonUtil.newSetMultimap();
        setMm.put("key1", 10);
        setMm.put("key2", 20);
        assertFalse(mm1.equals(setMm));
    }

    @Test
    public void testCustomEqualsHashCode() {
        class CustomKey {
            private final String value;

            CustomKey(String value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof CustomKey) {
                    return value.equalsIgnoreCase(((CustomKey) obj).value);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return value.toLowerCase().hashCode();
            }
        }

        Multimap<CustomKey, Integer, List<Integer>> customMultimap = CommonUtil.newListMultimap();
        CustomKey key1 = new CustomKey("TEST");
        CustomKey key2 = new CustomKey("test");

        customMultimap.put(key1, 1);
        customMultimap.put(key2, 2);

        assertEquals(2, customMultimap.totalValueCount());
        assertEquals(2, customMultimap.get(key1).size());
    }

    @Test
    public void testToString() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        String str = mm.toString();
        assertTrue(str.startsWith("{") && str.endsWith("}"));
        assertTrue(str.contains("a=[1, 2]") || str.contains("a=[2, 1]"));
        assertTrue(str.contains("b=[3]"));
        assertEquals(("{" + "a=[1, 2]" + ", " + "b=[3]" + "}").length(), str.length());

        Multimap<String, Integer, List<Integer>> linkedMm = CommonUtil.newListMultimap(LinkedHashMap.class, ArrayList.class);
        linkedMm.put("z", 10);
        linkedMm.put("y", 20);
        assertEquals("{z=[10], y=[20]}", linkedMm.toString());
    }

    @Test
    public void testToStringFormat() {
        multimap.putValues("key1", Arrays.asList(1, 2));
        multimap.put("key2", 3);

        String str = multimap.toString();
        assertTrue(str.startsWith("{"));
        assertTrue(str.endsWith("}"));
        assertTrue(str.contains("key1"));
        assertTrue(str.contains("key2"));
        assertTrue(str.contains("[1, 2]") || str.contains("[2, 1]"));
        assertTrue(str.contains("[3]"));
    }

    @Test
    public void testToString_EmptyMultimap() {
        String str = listMultimap.toString();
        assertNotNull(str);
        assertEquals("{}", str);
    }

    @Test
    public void testToString_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        String str = listMultimap.toString();
        assertNotNull(str);
        assertTrue(str.contains("key1"));
        assertTrue(str.contains("key2"));
    }

    @Test
    public void testIntegrationScenario3() {
        Multimap<String, Long, List<Long>> eventTimestamps = CommonUtil.newListMultimap();

        long baseTime = System.currentTimeMillis();
        eventTimestamps.putValues("login", Arrays.asList(baseTime, baseTime + 1000, baseTime + 2000));
        eventTimestamps.putValues("logout", Arrays.asList(baseTime + 3000, baseTime + 4000));
        eventTimestamps.putValues("error", Arrays.asList(baseTime + 1500));

        List<Long> loginTimes = eventTimestamps.get("login");
        List<Long> logoutTimes = eventTimestamps.get("logout");

        eventTimestamps.merge("session_duration", loginTimes, (old, count) -> {
            List<Long> durations = new ArrayList<>();
            for (int i = 0; i < Math.min(loginTimes.size(), logoutTimes.size()); i++) {
                durations.add(logoutTimes.get(i) - loginTimes.get(i));
            }
            return durations;
        });

        assertTrue(eventTimestamps.containsKey("session_duration"));
        assertEquals(3, eventTimestamps.get("session_duration").size());

        eventTimestamps.merge("session_duration", loginTimes, (old, count) -> {
            List<Long> durations = new ArrayList<>();
            for (int i = 0; i < Math.min(loginTimes.size(), logoutTimes.size()); i++) {
                durations.add(logoutTimes.get(i) - loginTimes.get(i));
            }
            return durations;
        });

        assertTrue(eventTimestamps.containsKey("session_duration"));
        assertEquals(2, eventTimestamps.get("session_duration").size());
    }

    @Test
    public void testIntegrationScenario1() {
        List<String> words = Arrays.asList("apple", "apricot", "banana", "berry", "cherry", "apple");

        Multimap<Character, String, List<String>> grouped = CommonUtil.newListMultimap();
        for (String word : words) {
            grouped.put(word.charAt(0), word);
        }

        grouped.replaceAll((k, v) -> {
            List<String> upper = new ArrayList<>();
            for (String s : v) {
                upper.add(s.toUpperCase());
            }
            return upper;
        });

        //        Multimap<Character, String, List<String>> filtered = grouped.filter((k, v) -> v.size() > 1);
        //
        //        assertEquals(2, filtered.size());
        //        assertTrue(filtered.containsKey('a'));
        //        assertTrue(filtered.containsKey('b'));
        //        assertTrue(filtered.get('a').contains("APPLE"));
        //        assertTrue(filtered.get('a').contains("APRICOT"));
    }

    @Test
    public void test_02() {
        Map<String, Integer> m = CommonUtil.asMap("abc", 123, "abc", 123, "abc", 456, "a", 1, "b", 2);
        Multimap<String, Integer, List<Integer>> multimap2 = ListMultimap.fromMap(m);
        N.println(multimap2);
        assertNotNull(multimap2);
    }

    @Test
    public void testWrapMap() {
        Map<String, List<Integer>> sourceMap = new HashMap<>();
        List<Integer> listA = new ArrayList<>(Arrays.asList(1, 2));
        sourceMap.put("a", listA);

        ListMultimap<String, Integer> lm = ListMultimap.wrap(sourceMap);
        assertEquals(Arrays.asList(1, 2), lm.get("a"));
        assertSame(listA, lm.get("a"), "Wrapped map should share the same value collection instance");

        lm.put("a", 3);
        assertEquals(Arrays.asList(1, 2, 3), sourceMap.get("a"));

        sourceMap.get("a").add(4);
        assertEquals(Arrays.asList(1, 2, 3, 4), lm.get("a"));

        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(null));
        Map<String, List<Integer>> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("x", null);
        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(mapWithNullValue));
    }

    @Test
    public void testWrapMapWithValueSupplier() {
        Map<String, List<Integer>> sourceMap = new LinkedHashMap<>();
        Supplier<List<Integer>> valueSupplier = LinkedList::new;

        ListMultimap<String, Integer> lm = ListMultimap.wrap(sourceMap, valueSupplier);
        lm.put("a", 1);
        assertTrue(sourceMap.get("a") instanceof LinkedList);
        assertEquals(Arrays.asList(1), sourceMap.get("a"));

        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(null, valueSupplier));
        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(sourceMap, null));
    }

    @Test
    public void testWrapMap2() {
        Map<String, Set<Integer>> sourceMap = new HashMap<>();
        Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2));
        sourceMap.put("a", setA);

        SetMultimap<String, Integer> sm = SetMultimap.wrap(sourceMap);
        assertEquals(CommonUtil.toSet(1, 2), sm.get("a"));
        assertSame(setA, sm.get("a"), "Wrapped map should share the same value collection instance");

        sm.put("a", 3);
        assertEquals(CommonUtil.toSet(1, 2, 3), sourceMap.get("a"));

        sourceMap.get("a").add(4);
        assertEquals(CommonUtil.toSet(1, 2, 3, 4), sm.get("a"));

        assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(null));
        Map<String, Set<Integer>> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("x", null);
        assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(mapWithNullValue));
    }

    @Test
    public void testCustomCollectionBehavior() {
        Multimap<String, Integer, List<Integer>> customMultimap = CommonUtil.newMultimap(HashMap::new, () -> new ArrayList<Integer>() {
            @Override
            public boolean add(Integer e) {
                if (e != null && e > 100) {
                    return super.add(e * 2);
                }
                return super.add(e);
            }
        });

        customMultimap.put("key", 50);
        customMultimap.put("key", 150);

        List<Integer> values = customMultimap.get("key");
        assertEquals(Integer.valueOf(50), values.get(0));
        assertEquals(Integer.valueOf(300), values.get(1));
    }

    @Test
    public void testConcurrentModificationDuringIteration() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        assertThrows(ConcurrentModificationException.class, () -> {
            for (Map.Entry<String, List<Integer>> entry : multimap) {
                multimap.put("key3", 3);
            }
        });
    }

}
