package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class MultimapCreationTest extends MultimapTestSupport {

    @Test
    public void testC356_baseAndSetMultimap_nullValueSupplier_NPE() {
        final Multimap<String, Integer, List<Integer>> base = CommonUtil.newMultimap(HashMap::new, () -> null);
        assertThrows(NullPointerException.class, () -> base.put("a", 1));
        assertTrue(base.isEmpty());
        assertTrue(base.copy().isEmpty(), "nothing to copy: the supplier is never called");

        final Map<String, Set<String>> backing = new HashMap<>();
        backing.put("x", new HashSet<>(Set.of("ü")));
        final SetMultimap<String, String> set = SetMultimap.wrap(backing, () -> null);
        assertThrows(NullPointerException.class, () -> set.put("y", "ü"));
        // a known HashSet policy copies without the supplier
        assertEquals(Set.of("ü"), set.copy().get("x"));
        assertEquals(Set.of("ü"), set.toMap().get("x"));
    }

    @Test
    public void testClassTypedConstructorBothTypesNullReportsMapTypeFirst() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.newListMultimap((Class<? extends Map>) null, (Class<? extends List>) null));
        assertEquals("'mapType' cannot be null", ex.getMessage());
    }

    @Test
    public void testClassTypedConstructorNullMapTypeMessageNamesMapType() {
        final IllegalArgumentException listEx = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.newListMultimap((Class<? extends Map>) null, ArrayList.class));
        assertEquals("'mapType' cannot be null", listEx.getMessage());

        final IllegalArgumentException setEx = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.newSetMultimap((Class<? extends Map>) null, HashSet.class));
        assertEquals("'mapType' cannot be null", setEx.getMessage());
    }

    @Test
    public void testClassTypedConstructorNullValueTypeMessageNamesValueType() {
        final IllegalArgumentException listEx = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.newListMultimap(HashMap.class, (Class<? extends List>) null));
        assertEquals("'valueType' cannot be null", listEx.getMessage());

        final IllegalArgumentException setEx = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.newSetMultimap(HashMap.class, (Class<? extends Set>) null));
        assertEquals("'valueType' cannot be null", setEx.getMessage());
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
    public void testConcatThreeMaps() {
        Map<String, Integer> mapA = Collections.singletonMap("a", 1);
        Map<String, Integer> mapB = Collections.singletonMap("b", 2);
        Map<String, Integer> mapC = Collections.singletonMap("c", 3);
        ListMultimap<String, Integer> lm = ListMultimap.merge(mapA, mapB, mapC);
        assertEquals(3, lm.totalValueCount());
        assertEquals(Arrays.asList(3), lm.get("c"));
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
    public void testConstructorWithInitialCapacity() {
        Multimap<String, Integer, List<Integer>> mm = CommonUtil.newListMultimap();
        assertNotNull(mm);
        assertTrue(mm.isEmpty());
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
    public void testConstructorWithSuppliers() {
        Multimap<String, Integer, List<Integer>> mm = CommonUtil.newMultimap(() -> new HashMap<>(), () -> new ArrayList<>());
        assertNotNull(mm);
        mm.put("test", 1);
        assertEquals(1, mm.get("test").size());
    }

    @Test
    public void testDefaultConstructor() {
        Multimap<String, Integer, List<Integer>> mm = CommonUtil.newListMultimap();
        assertNotNull(mm);
        assertTrue(mm.isEmpty());
        assertEquals(0, mm.totalValueCount());
    }

    // fromCollection

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
    public void testCreateFromCollectionWithKeyAndValueExtractors2() {
        List<Pair<String, String>> data = Arrays.asList(Pair.of("fruit", "apple"), Pair.of("fruit", "banana"), Pair.of("vegetable", "carrot"),
                Pair.of("fruit", "apple"));
        SetMultimap<String, String> sm = SetMultimap.fromCollection(data, Pair::left, Pair::right);
        assertEquals(3, sm.totalValueCount());
        assertEquals(CommonUtil.toSet("apple", "banana"), sm.get("fruit"));
        assertEquals(CommonUtil.toSet("carrot"), sm.get("vegetable"));
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
    public void testCreateFromCollectionWithKeyExtractor2() {
        List<String> data = Arrays.asList("apple", "apricot", "banana", "apple");
        SetMultimap<Character, String> sm = SetMultimap.fromCollection(data, s -> s.charAt(0));
        assertEquals(3, sm.totalValueCount());
        assertEquals(CommonUtil.toSet("apple", "apricot"), sm.get('a'));
        assertEquals(CommonUtil.toSet("banana"), sm.get('b'));
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

    // wrap

    @Test
    public void testC356_listWrap_nullValueSupplier_NPE_everyCreationSite() {
        final Map<String, List<Integer>> backing = new HashMap<>();
        backing.put("x", new ArrayList<>(List.of(1)));
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing, () -> null);

        final NullPointerException ex = assertThrows(NullPointerException.class, () -> mm.put("y", 2));
        assertEquals("valueSupplier returned null", ex.getMessage());
        assertThrows(NullPointerException.class, () -> mm.putValues("y", List.of(2)));
        assertThrows(NullPointerException.class, () -> mm.putIfKeyAbsent("y", 2));
        assertThrows(NullPointerException.class, () -> mm.putIfValueAbsent("y", 2));
        assertThrows(NullPointerException.class, () -> mm.putValuesIfKeyAbsent("y", List.of(2)));
        assertThrows(NullPointerException.class, () -> mm.putAll(Map.of("y", 2)));
        assertThrows(NullPointerException.class, () -> mm.putValues(Map.of("y", List.of(2))));
        assertThrows(NullPointerException.class, () -> mm.putValues(listOf("y", 2)));
        assertThrows(NullPointerException.class, () -> mm.compute("y", (k, v) -> new ArrayList<>(List.of(2))));
        assertThrows(NullPointerException.class, () -> mm.merge("y", 2, (v, e) -> v));
        assertThrows(NullPointerException.class, mm::copy);
        assertThrows(NullPointerException.class, mm::toMap);
        assertThrows(NullPointerException.class, () -> mm.toMap(HashMap::new));
        assertFalse(mm.containsKey("y"));

        // existing key: no supplier call, still works; empty input: nothing is created
        assertTrue(mm.put("x", 3));
        assertFalse(mm.putValues("y", List.of()));
        assertEquals(List.of(1, 3), mm.get("x"));
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
    public void testWrapMapWithValueSupplier() {
        Map<String, List<Integer>> sourceMap = new LinkedHashMap<>();
        Supplier<List<Integer>> valueSupplier = LinkedList::new;

        ListMultimap<String, Integer> lm = ListMultimap.wrap(sourceMap, valueSupplier);
        lm.put("a", 1);
        assertTrue(sourceMap.get("a") instanceof LinkedList);
        assertEquals(Arrays.asList(1), sourceMap.get("a"));

        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(null, valueSupplier));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(sourceMap, null));
    }
}
