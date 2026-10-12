package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

public class MultimapConversionTest extends MultimapTestSupport {

    private record Box(int v) {
    }

    // flatValues

    @Test
    public void testC351_flatValues_nullFactory_NPE_onEmptyAndNonEmpty() {
        final ListMultimap<String, Integer> empty = CommonUtil.newListMultimap();
        final NullPointerException e1 = assertThrows(NullPointerException.class, () -> empty.flatValues(n -> null));
        assertTrue(e1.getMessage().contains("returned null"), e1.getMessage());

        assertThrows(NullPointerException.class, () -> listOf("a", 1, "b", 2).flatValues(n -> null));
        assertThrows(IllegalArgumentException.class, () -> listOf("a", 1).flatValues(null));
        assertEquals(List.of(1, 2), listOf("a", 1, "b", 2).flatValues(ArrayList::new));
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
    public void testFlatValuesWithSupplier() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        TreeSet<Integer> result = listMultimap.flatValues(size -> new TreeSet<>());
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));
    }

    @Test
    public void testFlatValuesWithSupplier_EmptyMultimap() {
        Set<Integer> values = listMultimap.flatValues(HashSet::new);
        assertNotNull(values);
        assertTrue(values.isEmpty());
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
    public void testFlatValues_EmptyMultimap() {
        Collection<Integer> values = listMultimap.allValues();
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
    public void testInverse_EmptyMultimap() {
        ListMultimap<Integer, String> inverse = listMultimap.invert(N::newListMultimap);
        assertNotNull(inverse);
        assertTrue(inverse.isEmpty());
    }

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

    // invert

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
    public void testInvert_EmptyMultimap() {
        ListMultimap<Integer, String> inverted = listMultimap.invert(N::newListMultimap);
        assertTrue(inverted.isEmpty());
    }

    @Test
    public void testC358_invert_hintIsKeyCount_notTotalValueCount() {
        final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.putValues("k", Collections.nCopies(100_000, 7));
        final AtomicInteger hint = new AtomicInteger(-1);

        final ListMultimap<Integer, String> inv = mm.invert(n -> {
            hint.set(n);
            return CommonUtil.newListMultimap(n);
        });

        assertEquals(1, hint.get());
        assertEquals(1, inv.keyCount());
        assertEquals(100_000, inv.get(7).size());

        final ListMultimap<String, Integer> three = listOf("a", 1, "b", 1, "c", 2);
        three.invert(n -> {
            hint.set(n);
            return CommonUtil.newListMultimap(n);
        });
        assertEquals(3, hint.get());

        CommonUtil.<String, Integer> newListMultimap().invert(n -> {
            hint.set(n);
            return CommonUtil.newListMultimap(n);
        });
        assertEquals(0, hint.get());
    }

    @Test
    public void testInvert_SetMultimap() {
        setMultimap.put("a", 1);
        setMultimap.put("a", 2);
        setMultimap.put("b", 1);

        @SuppressWarnings("unchecked")
        Multimap<Integer, String, Set<String>> inverse = setMultimap.invert(N::newSetMultimap);
        assertEquals(2, inverse.get(1).size());
        assertTrue(inverse.get(1).contains("a"));
        assertTrue(inverse.get(1).contains("b"));
        assertEquals(1, inverse.get(2).size());
        assertTrue(inverse.get(2).contains("a"));
    }

    // U16-02: invert() backing map is a HashMap only for an exact HashMap backing, else a LinkedHashMap
    @Test
    public void testU1602_listMultimapInvertBackingMap() {
        final ListMultimap<String, Integer> concurrent = new ListMultimap<>(ConcurrentHashMap.class, ArrayList.class);
        concurrent.put("a", 1);
        concurrent.put("b", 1);
        assertEquals(LinkedHashMap.class, concurrent.invert().backingMap.getClass());
        assertEquals(ConcurrentHashMap.class, concurrent.copy().backingMap.getClass());
        assertEquals(Set.of("a", "b"), new HashSet<>(concurrent.invert().get(1)));

        final ListMultimap<String, Integer> hash = new ListMultimap<>(HashMap.class, ArrayList.class);
        hash.put("a", 1);
        assertEquals(HashMap.class, hash.invert().backingMap.getClass());

        final ListMultimap<String, Integer> sorted = new ListMultimap<>(TreeMap.class, ArrayList.class);
        sorted.put("b", 1);
        sorted.put("a", 2);
        final ListMultimap<Integer, String> inverted = sorted.invert();
        assertEquals(LinkedHashMap.class, inverted.backingMap.getClass());
        assertEquals(List.of(2, 1), new ArrayList<>(inverted.keySet()));

        final ListMultimap<String, Integer> table = new ListMultimap<>(Hashtable.class, ArrayList.class);
        table.put("a", 1);
        assertEquals(LinkedHashMap.class, table.invert().backingMap.getClass());
        assertEquals(Hashtable.class, table.copy().backingMap.getClass());
    }

    @Test
    public void testU1602_setMultimapInvertBackingMap() {
        final SetMultimap<String, Integer> concurrent = new SetMultimap<>(ConcurrentHashMap.class, HashSet.class);
        concurrent.put("a", 1);
        concurrent.put("b", 1);
        assertEquals(LinkedHashMap.class, concurrent.invert().backingMap.getClass());
        assertEquals(ConcurrentHashMap.class, concurrent.copy().backingMap.getClass());
        assertEquals(Set.of("a", "b"), concurrent.invert().get(1));

        final SetMultimap<String, Integer> hash = new SetMultimap<>(HashMap.class, HashSet.class);
        hash.put("a", 1);
        assertEquals(HashMap.class, hash.invert().backingMap.getClass());

        final SetMultimap<String, Integer> sorted = new SetMultimap<>(TreeMap.class, HashSet.class);
        sorted.put("b", 1);
        sorted.put("a", 2);
        final SetMultimap<Integer, String> inverted = sorted.invert();
        assertEquals(LinkedHashMap.class, inverted.backingMap.getClass());
        assertEquals(List.of(2, 1), new ArrayList<>(inverted.keySet()));
    }

    @Test
    public void testC351_invert_nullFactory_NPE_onEmptyAndNonEmpty() {
        final ListMultimap<String, Integer> empty = CommonUtil.newListMultimap();
        final NullPointerException e1 = assertThrows(NullPointerException.class, () -> empty.<List<String>, ListMultimap<Integer, String>> invert(n -> null));
        assertTrue(e1.getMessage().contains("multimapSupplier returned null"), e1.getMessage());

        final ListMultimap<String, Integer> one = listOf("a", 1);
        assertThrows(NullPointerException.class, () -> one.<List<String>, ListMultimap<Integer, String>> invert(n -> null));
        assertEquals(List.of("a"), one.<List<String>, ListMultimap<Integer, String>> invert(n -> CommonUtil.newListMultimap()).get(1));
    }

    // toImmutableMap

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

    // toMap

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
    public void testC352_toMap_caseInsensitiveOrderKept_unicode() {
        final TreeSet<String> ci = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ci.addAll(List.of("Ωmega", "B", "a"));
        final Map<String, Set<String>> backing = new LinkedHashMap<>();
        backing.put("k", ci);
        final SetMultimap<String, String> mm = SetMultimap.wrap(backing);

        final List<String> expected = List.of("a", "B", "Ωmega");
        assertEquals(expected, new ArrayList<>(mm.toMap().get("k")));
        assertEquals(expected, new ArrayList<>(mm.toMap(n -> new HashMap<>()).get("k")));
        assertTrue(mm.toMap().get("k").contains("A"), "case-insensitive membership survives the copy");
    }

    @Test
    public void testC352_toMap_emptyKeyAndEmptyMultimap() {
        final Map<String, Set<Integer>> backing = new LinkedHashMap<>();
        backing.put("k", new TreeSet<>(Set.of(1)));
        final SetMultimap<String, Integer> mm = SetMultimap.wrap(backing);
        backing.put("empty", new TreeSet<>(Comparator.reverseOrder()));

        final Map<String, Set<Integer>> map = mm.toMap();
        assertTrue(map.get("empty").isEmpty());
        assertEquals(2, map.size());

        assertTrue(CommonUtil.<String, Integer> newSetMultimap().toMap().isEmpty());
        assertTrue(CommonUtil.<String, Integer> newListMultimap().toMap(HashMap::new).isEmpty());
    }

    @Test
    public void testToMap_EmptyMultimap() {
        Map<String, List<Integer>> map = listMultimap.toMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testC352_toMap_keepsReverseTreeSetComparator() {
        final TreeSet<Integer> reversed = new TreeSet<>(Comparator.reverseOrder());
        reversed.addAll(List.of(1, 2, 3));
        final Map<String, Set<Integer>> backing = new HashMap<>();
        backing.put("k", reversed);
        final SetMultimap<String, Integer> mm = SetMultimap.wrap(backing);

        final Map<String, Set<Integer>> map = mm.toMap();
        assertEquals(List.of(3, 2, 1), new ArrayList<>(map.get("k")));
        assertSame(reversed.comparator(), ((SortedSet<Integer>) map.get("k")).comparator());
        assertNotSame(reversed, map.get("k"));

        final Map<String, Set<Integer>> map2 = mm.toMap(LinkedHashMap::new);
        assertEquals(List.of(3, 2, 1), new ArrayList<>(map2.get("k")));

        // independent copies
        map.get("k").clear();
        assertEquals(3, reversed.size());
    }

    @Test
    public void testC352_toMap_listMultimapUnchanged() {
        final ListMultimap<String, Integer> mm = listOf("a", 1, "a", 1, "b", 2);
        final Map<String, List<Integer>> map = mm.toMap();

        assertEquals(Map.of("a", List.of(1, 1), "b", List.of(2)), map);
        assertNotSame(mm.get("a"), map.get("a"));
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
    public void testC352_toMap_nonComparableElementsUnderComparator_noClassCastException() {
        final TreeSet<Box> boxes = new TreeSet<>(Comparator.comparingInt(Box::v));
        boxes.add(new Box(2));
        boxes.add(new Box(1));
        final Map<String, Set<Box>> backing = new HashMap<>();
        backing.put("k", boxes);
        final SetMultimap<String, Box> mm = SetMultimap.wrap(backing);

        assertEquals(List.of(new Box(1), new Box(2)), new ArrayList<>(mm.toMap().get("k")));
        assertEquals(List.of(new Box(1), new Box(2)), new ArrayList<>(mm.toMap(HashMap::new).get("k")));
        assertEquals(mm.copy().toMap(), mm.toMap());
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
    public void testToMap_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Map<String, List<Integer>> map = listMultimap.toMap();
        assertEquals(2, map.size());
        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testC351_toMapIntFunction_nullFactory_NPE_onEmptyAndNonEmpty() {
        final ListMultimap<String, Integer> empty = CommonUtil.newListMultimap();
        final NullPointerException e1 = assertThrows(NullPointerException.class, () -> empty.toMap(n -> null));
        assertTrue(e1.getMessage().contains("returned null"), e1.getMessage());

        final ListMultimap<String, Integer> one = listOf("a", 1);
        assertThrows(NullPointerException.class, () -> one.toMap(n -> null));
        assertEquals(List.of(1), one.get("a"));
    }

    // U22-06: toMap() / toMap(IntFunction) reject a lossy set copy (the SetMultimap copy policy)
    @Test
    public void testU2206_toMapRejectsALossySetCopy() {
        final Set<String> values = new HashSet<>() {
            private static final long serialVersionUID = 1L;
        };
        values.add("a");
        values.add("A");
        final Map<String, Set<String>> source = new HashMap<>();
        source.put("k", values);
        final SetMultimap<String, String> mm = SetMultimap.wrap(source, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, mm::toMap);
        assertEquals("Set copy loses distinct source elements", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> mm.toMap(HashMap::new));
        assertEquals("Set copy loses distinct source elements", e.getMessage());
        assertEquals(Set.of("a", "A"), mm.get("k"));

        // a faithful copy is fine
        final SetMultimap<String, String> ok = CommonUtil.newLinkedSetMultimap();
        ok.putValues("k", List.of("a", "A"));
        assertEquals(Set.of("a", "A"), ok.toMap().get("k"));
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
    public void testToMapWithSupplier_EmptyMultimap() {
        HashMap<String, List<Integer>> map = listMultimap.toMap(HashMap::new);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMapWithSupplier_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        HashMap<String, List<Integer>> map = listMultimap.toMap(HashMap::new);
        assertEquals(2, map.size());
    }

    // toMultiset

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
    public void testToMultisetPreservesMapSupplier() {
        // regression: toMultiset rebuilt the backing map from its Class, losing custom comparators
        // (ClassCastException for non-Comparable keys in comparator-backed TreeMaps)
        final ListMultimap<Object, Integer> mm = CommonUtil
                .newListMultimap(() -> new java.util.TreeMap<>(java.util.Comparator.comparingInt(System::identityHashCode)), java.util.ArrayList::new);
        final Object k = new Object();
        mm.put(k, 1);
        mm.put(k, 2);

        final Multiset<Object> ms = mm.toMultiset();

        assertEquals(2, ms.getCount(k));
    }

    // U22-07: toMultiset() with a BiMap map supplier
    @Test
    public void testU2207_toMultisetRejectsABiMapMapSupplier() {
        final Multimap<String, Integer, List<Integer>> mm = CommonUtil.newMultimap(BiMap::new, ArrayList::new);
        mm.put("a", 1);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, mm::toMultiset);
        assertTrue(e.getMessage().contains("cannot supply a BiMap"), e.getMessage());

        // control: an ordinary map supplier
        final ListMultimap<String, Integer> plain = CommonUtil.newListMultimap();
        plain.putValues("a", Arrays.asList(1, 2));
        plain.put("b", 3);
        final Multiset<String> counts = plain.toMultiset();
        assertEquals(2, counts.getCount("a"));
        assertEquals(1, counts.getCount("b"));
    }

    // toString

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
    public void testTotalCountOfValues_EmptyMultimap() {
        assertEquals(0, listMultimap.totalValueCount());
    }

    @Test
    public void testTotalCountOfValues_LargeNumbers() {
        for (int i = 0; i < 10; i++) {
            listMultimap.putValues("key" + i, Arrays.asList(1, 2, 3, 4, 5));
        }
        assertEquals(50, listMultimap.totalValueCount());
    }

    @Test
    public void testTotalCountOfValues_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(3, listMultimap.totalValueCount());
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
}
