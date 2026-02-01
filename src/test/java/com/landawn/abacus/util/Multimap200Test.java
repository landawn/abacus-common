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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Tag("new-test")
public class Multimap200Test extends TestBase {

    private ListMultimap<String, Integer> listMultimap;
    private SetMultimap<String, Integer> setMultimap;

    @BeforeEach
    public void setUp() {
        listMultimap = CommonUtil.newListMultimap();
        setMultimap = CommonUtil.newSetMultimap();
    }

    private Multimap<String, Integer, List<Integer>> getTestMultimap() {
        return CommonUtil.newListMultimap();
    }

    private Multimap<String, Integer, Set<Integer>> getSetTestMultimap() {
        return CommonUtil.newSetMultimap();
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
    public void testPutIfKeyAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.putIfKeyAbsent("a", 1));
        assertTrue(mm.containsEntry("a", 1));

        assertFalse(mm.putIfKeyAbsent("a", 2));
        assertEquals(1, mm.get("a").size());
        assertFalse(mm.containsEntry("a", 2));
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

    //    @Test
    //    public void testReplaceAllWithOneIf_keyPredicate() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("apple", Arrays.asList(1, 2, 1, 5));
    //        mm.putValues("apricot", Arrays.asList(1, 3, 5));
    //        mm.put("banana", 1);
    //
    //        assertTrue(mm.replaceAllValuesIf(key -> key.startsWith("ap"), 100));
    //        assertEquals(Arrays.asList(100), mm.get("apple"));
    //        assertEquals(Arrays.asList(100), mm.get("apricot"));
    //        assertEquals(Arrays.asList(1), mm.get("banana"));
    //    }
    //
    //    @Test
    //    public void testReplaceAllWithOneIf_biPredicate() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2, 1, 5));
    //        mm.putValues("b", Arrays.asList(1, 3, 5));
    //        mm.put("c", 1);
    //
    //        assertTrue(mm.replaceAllValuesIf((k, v) -> k.equals("c") || v.contains(2), 100));
    //        assertEquals(Arrays.asList(100), mm.get("a"));
    //        assertEquals(Arrays.asList(1, 3, 5), mm.get("b"));
    //        assertEquals(Arrays.asList(100), mm.get("c"));
    //    }

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
    public void testContains_keyValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        assertTrue(mm.containsEntry("a", 1));
        assertFalse(mm.containsEntry("a", 2));
        assertFalse(mm.containsEntry("b", 1));
    }

    @Test
    public void testContainsKey() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        assertTrue(mm.containsKey("a"));
        assertFalse(mm.containsKey("b"));
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

    //    @Test
    //    public void testContainsAll_keyCollection() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2, 3, 1));
    //        assertTrue(mm.containsAllValues("a", Arrays.asList(1, 2)));
    //        assertTrue(mm.containsAllValues("a", Arrays.asList(1, 1, 2)));
    //        assertFalse(mm.containsAllValues("a", Arrays.asList(1, 4)));
    //        assertTrue(mm.containsAllValues("a", Collections.emptyList()));
    //        assertFalse(mm.containsAllValues("b", Arrays.asList(1)));
    //
    //        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
    //        smm.putValues("x", new HashSet<>(Arrays.asList(10, 20, 30)));
    //        assertTrue(smm.containsAllValues("x", new HashSet<>(Arrays.asList(10, 20))));
    //        assertFalse(smm.containsAllValues("x", new HashSet<>(Arrays.asList(10, 40))));
    //    }
    //
    //    @Test
    //    public void testFilterByKey() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.put("apple", 1);
    //        mm.put("apricot", 2);
    //        mm.put("banana", 3);
    //        Predicate<String> startsWithAp = k -> k.startsWith("ap");
    //        Multimap<String, Integer, List<Integer>> filtered = mm.filterByKey(startsWithAp);
    //
    //        assertEquals(2, filtered.size());
    //        assertTrue(filtered.containsKey("apple"));
    //        assertTrue(filtered.containsKey("apricot"));
    //        assertFalse(filtered.containsKey("banana"));
    //        assertHaveSameElements(mm.get("apple"), filtered.get("apple"));
    //    }
    //
    //    @Test
    //    public void testFilterByValue() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2));
    //        mm.putValues("b", Arrays.asList(3, 4, 5));
    //        mm.putValues("c", Arrays.asList(1));
    //        Predicate<List<Integer>> hasEvenNumber = v -> v.stream().anyMatch(i -> i % 2 == 0);
    //        Multimap<String, Integer, List<Integer>> filtered = mm.filterByValues(hasEvenNumber);
    //
    //        assertEquals(2, filtered.size());
    //        assertTrue(filtered.containsKey("a"));
    //        assertTrue(filtered.containsKey("b"));
    //        assertFalse(filtered.containsKey("c"));
    //        assertHaveSameElements(mm.get("a"), filtered.get("a"));
    //    }

    //    @Test
    //    public void testFilter_biPredicate() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2));
    //        mm.putValues("b", Arrays.asList(10, 20));
    //        BiPredicate<String, List<Integer>> filter = (k, v) -> k.equals("a") || v.contains(20);
    //        Multimap<String, Integer, List<Integer>> filtered = mm.filter(filter);
    //
    //        assertEquals(2, filtered.size());
    //        assertTrue(filtered.containsKey("a"));
    //        assertTrue(filtered.containsKey("b"));
    //        assertHaveSameElements(mm.get("a"), filtered.get("a"));
    //    }
    //
    //    @Test
    //    public void testForEach_biConsumer() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2));
    //        mm.put("b", 3);
    //        Map<String, List<Integer>> result = new HashMap<>();
    //        mm.forEach(result::put);
    //
    //        assertEquals(2, result.size());
    //        assertEquals(Arrays.asList(1, 2), result.get("a"));
    //        assertEquals(Arrays.asList(3), result.get("b"));
    //        assertThrows(NullPointerException.class, () -> mm.forEach((Consumer) null));
    //    }

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

    //    @Test
    //    public void testForEachKey() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.put("a", 1);
    //        mm.put("b", 2);
    //        Set<String> keys = new HashSet<>();
    //        mm.forEachKey(keys::add);
    //        assertEquals(CommonUtil.asSet("a", "b"), keys);
    //        assertThrows(IllegalArgumentException.class, () -> mm.forEachKey(null));
    //    }
    //
    //    @Test
    //    public void testForEachValue() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2));
    //        mm.putValues("b", Arrays.asList(3));
    //        List<Collection<Integer>> valuesList = new ArrayList<>();
    //        mm.forEachValues(valuesList::add);
    //
    //        assertEquals(2, valuesList.size());
    //        assertTrue(valuesList.contains(Arrays.asList(1, 2)));
    //        assertTrue(valuesList.contains(Arrays.asList(3)));
    //        assertThrows(IllegalArgumentException.class, () -> mm.forEachValues(null));
    //    }

    //    @Test
    //    public void testFlatForEachValue() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2));
    //        mm.put("b", 3);
    //        List<Integer> elements = new ArrayList<>();
    //        mm.flatForEachValue(elements::add);
    //
    //        assertEquals(3, elements.size());
    //        assertTrue(elements.containsAll(Arrays.asList(1, 2, 3)));
    //        assertThrows(IllegalArgumentException.class, () -> mm.flatForEachValue(null));
    //    }

    @Test
    public void testKeySet() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("b", 2);
        assertEquals(CommonUtil.asSet("a", "b"), mm.keySet());
        mm.keySet().remove("a");
        assertFalse(mm.containsKey("a"));
    }

    //    @Test
    //    public void testValues() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        List<Integer> listA = Arrays.asList(1, 2);
    //        List<Integer> listB = Arrays.asList(3);
    //        mm.putValues("a", listA);
    //        mm.putValues("b", listB);
    //
    //        Collection<List<Integer>> valuesCol = mm.valueCollections();
    //        assertEquals(2, valuesCol.size());
    //        AtomicInteger foundA = new AtomicInteger();
    //        AtomicInteger foundB = new AtomicInteger();
    //        valuesCol.forEach(v -> {
    //            if (v.equals(listA))
    //                foundA.incrementAndGet();
    //            if (v.equals(listB))
    //                foundB.incrementAndGet();
    //        });
    //        assertEquals(1, foundA.get());
    //        assertEquals(1, foundB.get());
    //    }

    //    @Test
    //    public void testEntrySet() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2));
    //        Set<Map.Entry<String, List<Integer>>> entrySet = mm.entrySet();
    //        assertEquals(1, entrySet.size());
    //        Map.Entry<String, List<Integer>> entry = entrySet.iterator().next();
    //        assertEquals("a", entry.getKey());
    //        assertEquals(Arrays.asList(1, 2), entry.getValue());
    //    }

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
    public void testToMap_supplier() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        Map<String, List<Integer>> linkedMap = mm.toMap(LinkedHashMap::new);
        assertTrue(linkedMap instanceof LinkedHashMap);
        assertEquals(Arrays.asList(1, 2), linkedMap.get("a"));
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
    public void testStream() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        assertEquals(2, mm.stream().count());
    }

    //    @Test
    //    public void testEntryStream() {
    //        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
    //        mm.putValues("a", Arrays.asList(1, 2));
    //        mm.put("b", 3);
    //        assertEquals(2, mm.entryStream().count());
    //    }

    @Test
    public void testClear() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.clear();
        assertTrue(mm.isEmpty());
        assertEquals(0, mm.totalValueCount());
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
    public void testIsEmpty() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.isEmpty());
        mm.put("a", 1);
        assertFalse(mm.isEmpty());
        mm.removeAll("a");
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
    public void testAccept() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        AtomicBoolean accepted = new AtomicBoolean(false);
        mm.accept(m -> accepted.set(true));
        assertTrue(accepted.get());
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

    //    @Test
    //    public void testGetFirst_ListMultimapSpecific() {
    //        listMultimap.put("a", 10);
    //        listMultimap.put("a", 20);
    //        assertEquals(Integer.valueOf(10), listMultimap.getFirst("a"));
    //        assertNull(listMultimap.getFirst("nonExistentKey"));
    //    }
    //
    //    @Test
    //    public void testGetFirstOrDefault_ListMultimapSpecific() {
    //        listMultimap.put("a", 10);
    //        listMultimap.put("a", 20);
    //        assertEquals(Integer.valueOf(10), listMultimap.getFirstOrDefault("a", 99));
    //        assertEquals(Integer.valueOf(99), listMultimap.getFirstOrDefault("nonExistentKey", 99));
    //    }

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

    //    @Test
    //    public void testFilterByKey_ListMultimapSpecific() {
    //        listMultimap.put("apple", 1);
    //        listMultimap.put("banana", 2);
    //        ListMultimap<String, Integer> filtered = listMultimap.filterByKey(s -> s.startsWith("a"));
    //        assertTrue(filtered instanceof ListMultimap);
    //        assertEquals(1, filtered.size());
    //        assertTrue(filtered.containsKey("apple"));
    //    }
    //
    //    @Test
    //    public void testFilterByValue_ListMultimapSpecific() {
    //        listMultimap.putValues("a", Arrays.asList(1, 2));
    //        listMultimap.putValues("b", Arrays.asList(3));
    //        ListMultimap<String, Integer> filtered = listMultimap.filterByValues(list -> list.contains(2));
    //        assertTrue(filtered instanceof ListMultimap);
    //        assertEquals(1, filtered.size());
    //        assertTrue(filtered.containsKey("a"));
    //    }

    //    @Test
    //    public void testFilter_BiPredicate_ListMultimapSpecific() {
    //        listMultimap.putValues("a", Arrays.asList(1, 2));
    //        listMultimap.putValues("b", Arrays.asList(3));
    //        ListMultimap<String, Integer> filtered = listMultimap.filter((k, v) -> k.equals("a") && v.contains(1));
    //        assertTrue(filtered instanceof ListMultimap);
    //        assertEquals(1, filtered.size());
    //        assertTrue(filtered.containsKey("a"));
    //    }

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
    public void testCreateFromCollectionWithKeyExtractor2() {
        List<String> data = Arrays.asList("apple", "apricot", "banana", "apple");
        SetMultimap<Character, String> sm = SetMultimap.fromCollection(data, s -> s.charAt(0));
        assertEquals(3, sm.totalValueCount());
        assertEquals(CommonUtil.asSet("apple", "apricot"), sm.get('a'));
        assertEquals(CommonUtil.asSet("banana"), sm.get('b'));
    }

    @Test
    public void testCreateFromCollectionWithKeyAndValueExtractors2() {
        List<Pair<String, String>> data = Arrays.asList(Pair.of("fruit", "apple"), Pair.of("fruit", "banana"), Pair.of("vegetable", "carrot"),
                Pair.of("fruit", "apple"));
        SetMultimap<String, String> sm = SetMultimap.fromCollection(data, Pair::left, Pair::right);
        assertEquals(3, sm.totalValueCount());
        assertEquals(CommonUtil.asSet("apple", "banana"), sm.get("fruit"));
        assertEquals(CommonUtil.asSet("carrot"), sm.get("vegetable"));
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
    public void testWrapMap2() {
        Map<String, Set<Integer>> sourceMap = new HashMap<>();
        Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2));
        sourceMap.put("a", setA);

        SetMultimap<String, Integer> sm = SetMultimap.wrap(sourceMap);
        assertEquals(CommonUtil.asSet(1, 2), sm.get("a"));
        assertSame(setA, sm.get("a"), "Wrapped map should share the same value collection instance");

        sm.put("a", 3);
        assertEquals(CommonUtil.asSet(1, 2, 3), sourceMap.get("a"));

        sourceMap.get("a").add(4);
        assertEquals(CommonUtil.asSet(1, 2, 3, 4), sm.get("a"));

        assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(null));
        Map<String, Set<Integer>> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("x", null);
        assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(mapWithNullValue));
    }

    @Test
    public void testInverse_SetMultimapSpecific() {
        setMultimap.put("k1", 100);
        setMultimap.put("k1", 200);
        setMultimap.put("k2", 100);
        setMultimap.put("k1", 100);

        SetMultimap<Integer, String> inverted = setMultimap.invert();
        assertEquals(3, inverted.totalValueCount());
        assertEquals(CommonUtil.asSet("k1", "k2"), inverted.get(100));
        assertEquals(CommonUtil.asSet("k1"), inverted.get(200));
        assertTrue(inverted.get(100) instanceof Set);
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

    //    @Test
    //    public void testFilterByKey_SetMultimapSpecific() {
    //        setMultimap.put("apple", 1);
    //        setMultimap.put("banana", 2);
    //        SetMultimap<String, Integer> filtered = setMultimap.filterByKey(s -> s.startsWith("a"));
    //        assertTrue(filtered instanceof SetMultimap);
    //        assertEquals(1, filtered.size());
    //        assertTrue(filtered.containsKey("apple"));
    //    }
    //
    //    @Test
    //    public void testFilterByValue_SetMultimapSpecific() {
    //        setMultimap.putValues("a", Arrays.asList(1, 2));
    //        setMultimap.putValues("b", Arrays.asList(3));
    //        SetMultimap<String, Integer> filtered = setMultimap.filterByValues(set -> set.contains(2));
    //        assertTrue(filtered instanceof SetMultimap);
    //        assertEquals(1, filtered.size());
    //        assertTrue(filtered.containsKey("a"));
    //    }

    //    @Test
    //    public void testFilter_BiPredicate_SetMultimapSpecific() {
    //        setMultimap.putValues("a", Arrays.asList(1, 2));
    //        setMultimap.putValues("b", Arrays.asList(3));
    //        SetMultimap<String, Integer> filtered = setMultimap.filter((k, v) -> k.equals("a") && v.contains(1));
    //        assertTrue(filtered instanceof SetMultimap);
    //        assertEquals(1, filtered.size());
    //        assertTrue(filtered.containsKey("a"));
    //    }

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
    public void testToImmutableMap_WithSupplier_SetMultimap() {
        setMultimap.putValues("a", Arrays.asList(1, 2));
        IntFunction<Map<String, ImmutableSet<Integer>>> mapSupplier = LinkedHashMap::new;

        ImmutableMap<String, ImmutableSet<Integer>> immutable = setMultimap.toImmutableMap(mapSupplier);
        assertEquals(1, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableSet);
        assertEquals(ImmutableSet.of(1, 2), immutable.get("a"));
    }

    @Test
    public void testPut_SetBehavior() {
        assertTrue(setMultimap.put("key1", 100));
        assertEquals(Collections.singleton(100), setMultimap.get("key1"));

        assertFalse(setMultimap.put("key1", 100));
        assertEquals(Collections.singleton(100), setMultimap.get("key1"));
        assertEquals(1, setMultimap.get("key1").size());

        assertTrue(setMultimap.put("key1", 200));
        assertEquals(CommonUtil.asSet(100, 200), setMultimap.get("key1"));
    }
}
