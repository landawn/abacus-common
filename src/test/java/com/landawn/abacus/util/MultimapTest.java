package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

public class MultimapTest extends MultimapTestSupport {
    @Test
    public void testRemoveValuesStagesSharedLiveCollections() {
        for (final boolean useSubList : new boolean[] { false, true }) {
            ListMultimap<String, Integer> target = N.newListMultimap();
            target.putValues("a", Arrays.asList(1, 3));
            target.putValues("b", Arrays.asList(1, 2, 3));
            Collection<Integer> live = useSubList ? target.get("a").subList(0, 1) : target.get("a");
            Map<String, Collection<Integer>> removals = new LinkedHashMap<>();
            removals.put("a", live);
            removals.put("b", live);
            assertTrue(target.removeValues(removals));
            assertEquals(useSubList ? List.of(3) : null, target.get("a"));
            assertEquals(useSubList ? List.of(2, 3) : List.of(2), target.get("b"));
        }

        ListMultimap<String, Integer> target = N.newListMultimap();
        target.put("a", 1);
        target.putValues("b", List.of(1, 2));
        Map<String, List<Integer>> removalMap = new LinkedHashMap<>();
        removalMap.put("a", target.get("a"));
        removalMap.put("b", target.get("a"));
        assertTrue(target.removeValues(ListMultimap.wrap(removalMap)));
        assertNull(target.get("a"));
        assertEquals(List.of(2), target.get("b"));
    }

    @Test
    public void testRemoveValuesForOneKeyStagesALiveValueCollection() {
        // removeValues(key, Collection) documents that the argument may be a live value collection or a sublist
        // backed by this Multimap. That is what the IdentityHashSet staging in the body is for: ArrayList.removeAll
        // compacts in place while a sublist view of that same list keeps reading it, so values the argument never
        // matched used to be dropped silently. Only removeValues(Map)/removeValues(Multimap) and removeValuesIf
        // pinned this shape before.
        ListMultimap<String, Integer> viaSubList = N.newListMultimap();
        viaSubList.putValues("a", Arrays.asList(1, 2, 3, 4));
        assertTrue(viaSubList.removeValues("a", viaSubList.get("a").subList(0, 2)));
        assertEquals(List.of(3, 4), viaSubList.get("a"));

        // the whole live collection: every stored value matches, so the key goes with it
        ListMultimap<String, Integer> viaWholeCollection = N.newListMultimap();
        viaWholeCollection.putValues("a", Arrays.asList(1, 2, 3, 4));
        viaWholeCollection.put("b", 1);
        assertTrue(viaWholeCollection.removeValues("a", viaWholeCollection.get("a")));
        assertNull(viaWholeCollection.get("a"));
        assertEquals(List.of(1), viaWholeCollection.get("b"));

        // a Set value collection too, where Collection.removeAll would have picked an equivalence by size
        SetMultimap<String, Integer> setValued = N.newSetMultimap();
        setValued.putValues("a", Arrays.asList(1, 2, 3));
        assertTrue(setValued.removeValues("a", setValued.get("a")));
        assertNull(setValued.get("a"));
    }

    @Test
    public void testRemoveValuesPreservesOriginalMembershipRules() {
        String selected = new String("same");
        String equalButDistinct = new String("same");
        ListMultimap<String, String> identityTarget = N.newListMultimap();
        identityTarget.putValues("a", List.of(selected, equalButDistinct));
        Set<String> identityRemoval = new IdentityHashSet<>();
        identityRemoval.add(selected);
        assertTrue(identityTarget.removeValues(Map.of("a", identityRemoval)));
        assertEquals(1, identityTarget.get("a").size());
        assertSame(equalButDistinct, identityTarget.get("a").get(0));

        ListMultimap<String, String> comparatorTarget = N.newListMultimap();
        comparatorTarget.putValues("a", List.of("UPPER", "keep"));
        Set<String> comparatorRemoval = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        comparatorRemoval.add("upper");
        assertTrue(comparatorTarget.removeValues(Map.of("a", comparatorRemoval)));
        assertEquals(List.of("keep"), comparatorTarget.get("a"));
    }

    @Test
    public void testGet_ReturnsBackedCollection() {
        listMultimap.put("key1", 10);
        List<Integer> values = listMultimap.get("key1");
        values.add(20);
        assertEquals(2, listMultimap.get("key1").size());
    }

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
    public void testTotalCountOfValues_LargeNumbers() {
        for (int i = 0; i < 10; i++) {
            listMultimap.putValues("key" + i, Arrays.asList(1, 2, 3, 4, 5));
        }
        assertEquals(50, listMultimap.totalValueCount());
    }

    @Test
    public void testFlatForEach() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        mm.forEachKeyValue((k, e) -> pairs.add(Pair.of(k, e)));

        assertEquals(3, pairs.size());
        assertTrue(pairs.contains(Pair.of("a", 1)));
        assertTrue(pairs.contains(Pair.of("a", 2)));
        assertTrue(pairs.contains(Pair.of("b", 3)));
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

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> mm.merge("b", elementsToMerge, null));
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

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> mm.merge("y", elementToMerge, null));
    }

    @Test
    public void testMergeNullFunction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multimap.merge("key", 1, null));
    }

    @Test
    public void testMergeCollection_NullFunction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> listMultimap.merge("key1", Arrays.asList(1), null));
    }

    @Test
    public void testMergeElement_NullFunction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
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
        Multimap<Integer, String, Set<String>> inverse = setMultimap.invert(N::newSetMultimap);
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
        listMultimap.forEachKeyValue((k, e) -> count.incrementAndGet());
        assertEquals(3, count.get());
    }

    @Test
    public void testFlatForEach_ModificationDuringIteration() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        List<Integer> collected = new ArrayList<>();
        listMultimap.forEachKeyValue((k, e) -> collected.add(e));
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
        listMultimap.forEachKeyValue((k, e) -> {
            keys.add(k);
            vals.add(e);
        });

        assertEquals(3, keys.size());
        assertEquals(3, vals.size());
        assertTrue(vals.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testFlatForEach_EmptyMultimap() {
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEachKeyValue((k, e) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testAllExceptionScenarios() {
        assertDoesNotThrow(() -> {
            //        try {
            //            multimap.forEachKeyValue((BiConsumer) null);
            //            fail("Should throw IllegalArgumentException");
            //        } catch (IllegalArgumentException e) {
            //        }

            //    try {
            //        multimap.flatForEachValue(null);
            //        fail("Should throw IllegalArgumentException");
            //    } catch (IllegalArgumentException e) {
            //    }
        });
    }

    @Test
    public void testForEach_NullAction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> listMultimap.forEachKeyValue((java.util.function.BiConsumer<? super String, ? super Integer>) null));
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

        assertEquals(CommonUtil.toList(1, 2, 3), list);
        assertEquals(CommonUtil.asSet("a", "b", "c"), map.keySet());
        assertEquals(9, map.totalValueCount());
        assertFalse(map.isEmpty());

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(5));

        assertFalse(map.containsKey("e"));
        assertFalse(map.containsValue(0));

        Multimap<String, Integer, Set<Integer>> map2 = CommonUtil.newSetMultimap();
        map2.put("a", 11);
        map2.put("a", 12);
        map2.put("a", 13);

        map2.put("b", 15);

        map2.put("d", 20);
        map.putValues(map2);
        assertTrue(map.get("a").containsAll(CommonUtil.toList(1, 2, 3, 11, 12, 13)));
        assertTrue(map.containsKey("d"));
        assertNotEquals(0, map.hashCode());
        assertFalse(map.equals(null));
        assertTrue(map.toString().contains("a"));

        map.removeAll("a");
        assertNull(map.get("a"));

        map.clear();

        assertEquals(0, map.totalValueCount());
    }

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
        listMultimap.entryStream().forEach(e -> collected.add(CommonUtil.newEntry(e.getKey(), e.getValue())));
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
        SetMultimap<String, Integer> emptySetMultimap = CommonUtil.newSetMultimap();
        assertTrue(listMultimap.equals(emptySetMultimap));
        assertTrue(emptySetMultimap.equals(listMultimap));
        assertEquals(listMultimap.hashCode(), emptySetMultimap.hashCode());
    }

    @Test
    public void testEquals_WithSetMultimap() {
        listMultimap.put("key1", 10);
        SetMultimap<String, Integer> setMm = CommonUtil.newSetMultimap();
        setMm.put("key1", 10);
        assertFalse(listMultimap.equals(setMm));
        assertFalse(setMm.equals(listMultimap));
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
        assertDoesNotThrow(() -> {
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

        });
    }

    @Test
    public void test_02() {
        Map<String, Integer> m = CommonUtil.asMap("abc", 123, "abc", 123, "abc", 456, "a", 1, "b", 2);
        Multimap<String, Integer, List<Integer>> multimap2 = ListMultimap.fromMap(m);
        assertEquals(CommonUtil.asSet("abc", "a", "b"), multimap2.keySet());
        assertEquals(CommonUtil.toList(456), multimap2.get("abc"));
        assertEquals(CommonUtil.toList(1), multimap2.get("a"));
        assertEquals(CommonUtil.toList(2), multimap2.get("b"));
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
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(sourceMap, null));
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

    /**
     * Regression test: merge(K, Collection, BiFunction) must defensively copy the
     * remapping function's result before clearing the existing value collection.
     * Previously merge did {@code oldValue.clear(); oldValue.addAll(newValue);} with no
     * defensive copy. When the remapping function returns a view backed by oldValue
     * (e.g. a subList), clear() empties that view too, so addAll adds nothing and
     * the data is silently lost. The sibling methods compute/computeIfPresent/replaceAll
     * already copy defensively.
     */
    @Test
    public void testMergeCollectionReturningViewBackedByOldValue() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.putValues("k", Arrays.asList(1, 2, 3, 4));

        // remapping function returns a view (subList) backed by oldValue
        List<Integer> result = mm.merge("k", Arrays.asList(99), (oldVals, newVals) -> oldVals.subList(0, 2));

        assertEquals(Arrays.asList(1, 2), result);
        assertEquals(Arrays.asList(1, 2), mm.get("k"));
    }

    /**
     * Regression test for the single-element merge(K, E, BiFunction) overload with the
     * same defensive-copy requirement as {@link #testMergeCollectionReturningViewBackedByOldValue()}.
     */
    @Test
    public void testMergeElementReturningViewBackedByOldValue() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.putValues("k", Arrays.asList(10, 20, 30));

        List<Integer> result = mm.merge("k", 99, (oldVals, newVal) -> oldVals.subList(1, 3));

        assertEquals(Arrays.asList(20, 30), result);
        assertEquals(Arrays.asList(20, 30), mm.get("k"));
    }

    @Test
    public void testBulkPutRejectedValuesDoNotLeaveEmptyMappings() {
        final Supplier<Set<Integer>> rejectingValueSupplier = () -> new HashSet<>() {
            @Override
            public boolean add(final Integer value) {
                return false;
            }
        };
        final Multimap<String, Integer, Set<Integer>> target = CommonUtil.newMultimap(HashMap::new, rejectingValueSupplier);

        assertFalse(target.putAll(Collections.singletonMap("map", 1)));
        assertFalse(target.putValuesIfKeyAbsent("single-key", Arrays.asList(1, 2)));

        final Map<String, Collection<Integer>> valuesByKey = new HashMap<>();
        valuesByKey.put("map-of-values", Arrays.asList(1, 2));
        assertFalse(target.putValues(valuesByKey));

        final ListMultimap<String, Integer> source = CommonUtil.newListMultimap();
        source.putValues("multimap", Arrays.asList(1, 2));
        assertFalse(target.putValues(source));

        assertTrue(target.isEmpty());
        assertFalse(target.containsKey("map"));
        assertFalse(target.containsKey("single-key"));
        assertFalse(target.containsKey("map-of-values"));
        assertFalse(target.containsKey("multimap"));
    }

    @Test
    public void testAllValuesSizeSaturatesAndSpliteratorKeepsLongEstimate() {
        final Map<String, Collection<Integer>> backing = new HashMap<>();
        backing.put("a", Collections.nCopies(Integer.MAX_VALUE, 1));
        backing.put("b", Collections.nCopies(Integer.MAX_VALUE, 2));
        final Multimap<String, Integer, Collection<Integer>> mm = new Multimap<>(backing, ArrayList::new);

        assertThrows(ArithmeticException.class, mm::totalValueCount);
        assertEquals(Integer.MAX_VALUE, mm.allValues().size());
        assertEquals(2L * Integer.MAX_VALUE, mm.valueSpliterator().estimateSize());
    }

    @Test
    public void testConditionalMutatorsRejectNullPredicatesForEmptyMultimap() {
        final Multimap<String, Integer, List<Integer>> empty = CommonUtil.newListMultimap();
        final Predicate<String> keyPredicate = null;
        final BiPredicate<String, List<Integer>> entryPredicate = null;

        assertThrows(IllegalArgumentException.class, () -> empty.removeEntriesIf(keyPredicate, 1));
        assertThrows(IllegalArgumentException.class, () -> empty.removeEntriesIf(entryPredicate, 1));
        assertThrows(IllegalArgumentException.class, () -> empty.removeValuesIf(keyPredicate, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> empty.removeValuesIf(entryPredicate, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> empty.removeKeysIf(keyPredicate));
        assertThrows(IllegalArgumentException.class, () -> empty.removeKeysIf(entryPredicate));
        assertThrows(IllegalArgumentException.class, () -> empty.replaceEntriesIf(keyPredicate, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> empty.replaceEntriesIf(entryPredicate, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> empty.replaceValuesIf(keyPredicate, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> empty.replaceValuesIf(entryPredicate, Collections.emptyList()));
    }

    @Test
    public void testHashCodeFollowsTheBackingMapRuleNotAnUnconditionalGuarantee() {
        // hashCode() delegates to the backing map, so the equal-implies-same-hash guarantee only holds between
        // multimaps whose backing maps share the same key equivalence - exactly what equals(Object) warns about.
        final String key = new String("k");
        final List<Integer> values = new ArrayList<>(Arrays.asList(1, 2));
        final Map<String, List<Integer>> hashBacked = new HashMap<>();
        hashBacked.put(key, values);
        final Map<String, List<Integer>> identityBacked = new java.util.IdentityHashMap<>();
        identityBacked.put(key, values);

        final ListMultimap<String, Integer> a = ListMultimap.wrap(hashBacked);
        final ListMultimap<String, Integer> b = ListMultimap.wrap(identityBacked);

        // mutually equal ...
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        // ... yet hashed by two different rules
        assertEquals(key.hashCode() ^ values.hashCode(), a.hashCode());
        assertEquals(System.identityHashCode(key) ^ System.identityHashCode(values), b.hashCode());

        // compatible backing maps keep the guarantee the javadoc does make
        final ListMultimap<String, Integer> c = CommonUtil.newListMultimap();
        c.putValues("k", Arrays.asList(1, 2));
        assertTrue(a.equals(c));
        assertEquals(a.hashCode(), c.hashCode());

        // "same key equivalence" is NOT enough: these two share one backing-map class and one comparator instance,
        // both implement Map#hashCode() exactly as specified, and they are mutually equal - yet they hash apart,
        // because AbstractMap.hashCode() is built from key.hashCode() while the comparator decides equality.
        final ListMultimap<String, Integer> upperKey = new ListMultimap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), ArrayList::new);
        upperKey.put("A", 1);
        final ListMultimap<String, Integer> lowerKey = new ListMultimap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), ArrayList::new);
        lowerKey.put("a", 1);

        assertTrue(upperKey.equals(lowerKey));
        assertTrue(lowerKey.equals(upperKey));
        assertEquals(97, upperKey.hashCode());
        assertEquals(65, lowerKey.hashCode());

        // the same hole on the value-collection side: these value sets honour Set#hashCode() exactly (88 and 120)
        // and still decide membership by a comparator rather than equals
        final SetMultimap<String, String> upperValue = new SetMultimap<>(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        upperValue.put("k", "X");
        final SetMultimap<String, String> lowerValue = new SetMultimap<>(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        lowerValue.put("k", "x");

        assertTrue(upperValue.equals(lowerValue));
        assertTrue(lowerValue.equals(upperValue));
        assertEquals(51, upperValue.hashCode());
        assertEquals(19, lowerValue.hashCode());
    }
}
