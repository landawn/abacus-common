package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.ObjIntFunction;
import com.landawn.abacus.util.function.ObjIntPredicate;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class Multiset123Test extends TestBase {

    private Multiset<String> multiset;

    @BeforeEach
    public void setUp() {
        multiset = new Multiset<>();
    }

    @Test
    public void testDefaultConstructor() {
        Multiset<String> ms = new Multiset<>();
        assertTrue(ms.isEmpty());
        assertEquals(0, ms.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        Multiset<String> ms = new Multiset<>(50);
        assertTrue(ms.isEmpty());
        assertEquals(0, ms.size());

        assertThrows(IllegalArgumentException.class, () -> new Multiset<>(-1));
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> ms = new Multiset<>(list);

        assertEquals(6, ms.size());
        assertEquals(3, ms.getCount("a"));
        assertEquals(2, ms.getCount("b"));
        assertEquals(1, ms.getCount("c"));

        Multiset<String> ms2 = new Multiset<>((Collection<String>) null);
        assertTrue(ms2.isEmpty());

        Set<String> set = new HashSet<>(Arrays.asList("x", "y", "z"));
        Multiset<String> ms3 = new Multiset<>(set);
        assertEquals(3, ms3.size());
        assertEquals(1, ms3.getCount("x"));
    }

    @Test
    public void testConstructorWithMapType() {
        Multiset<String> ms = new Multiset<>(LinkedHashMap.class);
        assertTrue(ms.isEmpty());

        assertThrows(NullPointerException.class, () -> new Multiset<>((Class<? extends Map>) null));
    }

    @Test
    public void testConstructorWithMapSupplier() {
        Multiset<String> ms = new Multiset<>(() -> new TreeMap<>());
        assertTrue(ms.isEmpty());

        assertThrows(NullPointerException.class, () -> new Multiset<>((Supplier<Map<String, ?>>) null));
    }

    @Test
    public void testOfVarargs() {
        Multiset<String> ms = Multiset.of("a", "b", "a", "c");
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
        assertEquals(1, ms.getCount("c"));

        Multiset<String> empty = Multiset.of();
        assertTrue(empty.isEmpty());

        String[] nullArray = null;
        Multiset<String> nullMs = Multiset.of(nullArray);
        assertTrue(nullMs.isEmpty());
    }

    @Test
    public void testCreateFromCollection() {
        List<String> list = Arrays.asList("x", "y", "x", "z");
        Multiset<String> ms = Multiset.create(list);
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("x"));
        assertEquals(1, ms.getCount("y"));
        assertEquals(1, ms.getCount("z"));
    }

    @Test
    public void testCreateFromIterator() {
        List<String> list = Arrays.asList("a", "b", "a");
        Iterator<String> iter = list.iterator();
        Multiset<String> ms = Multiset.create(iter);
        assertEquals(3, ms.size());
        assertEquals(2, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));

        Multiset<String> nullMs = Multiset.create((Iterator<String>) null);
        assertTrue(nullMs.isEmpty());

        Multiset<String> overflowMs = new Multiset<>();
        overflowMs.add("test", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> overflowMs.add("test"));
    }

    @Test
    public void testOccurrencesOf() {
        multiset.add("apple", 3);
        multiset.add("banana", 2);

        assertEquals(3, multiset.occurrencesOf("apple"));
        assertEquals(2, multiset.occurrencesOf("banana"));
        assertEquals(0, multiset.occurrencesOf("cherry"));
    }

    @Test
    public void testMinOccurrences() {
        assertTrue(multiset.minOccurrences().isEmpty());

        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);

        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertEquals("b", min.get().right());
    }

    @Test
    public void testMaxOccurrences() {
        assertTrue(multiset.maxOccurrences().isEmpty());

        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);

        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(3, max.get().left().intValue());
        assertEquals("a", max.get().right());
    }

    @Test
    public void testAllMinOccurrences() {
        assertTrue(multiset.allMinOccurrences().isEmpty());

        multiset.add("a", 2);
        multiset.add("b", 1);
        multiset.add("c", 1);
        multiset.add("d", 3);

        Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(1, allMin.get().left().intValue());
        List<String> minElements = allMin.get().right();
        assertEquals(2, minElements.size());
        assertTrue(minElements.contains("b"));
        assertTrue(minElements.contains("c"));
    }

    @Test
    public void testAllMaxOccurrences() {
        assertTrue(multiset.allMaxOccurrences().isEmpty());

        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 3);
        multiset.add("d", 2);

        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(3, allMax.get().left().intValue());
        List<String> maxElements = allMax.get().right();
        assertEquals(2, maxElements.size());
        assertTrue(maxElements.contains("a"));
        assertTrue(maxElements.contains("c"));
    }

    @Test
    public void testSumOfOccurrences() {
        assertEquals(0, multiset.sumOfOccurrences());

        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        assertEquals(6, multiset.sumOfOccurrences());
    }

    @Test
    public void testAverageOfOccurrences() {
        assertTrue(multiset.averageOfOccurrences().isEmpty());

        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        OptionalDouble avg = multiset.averageOfOccurrences();
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testCount() {
        assertEquals(0, multiset.count("test"));

        multiset.add("test", 5);
        assertEquals(5, multiset.count("test"));
    }

    @Test
    public void testGetCount() {
        assertEquals(0, multiset.getCount("test"));

        multiset.add("test", 5);
        assertEquals(5, multiset.getCount("test"));

        assertEquals(0, multiset.getCount(null));
        multiset.add(null, 2);
        assertEquals(2, multiset.getCount(null));
    }

    @Test
    public void testSetCount() {
        assertEquals(0, multiset.setCount("apple", 5));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(5, multiset.setCount("apple", 3));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.setCount("apple", 0));
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));

        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("test", -1));
    }

    @Test
    public void testSetCountConditional() {
        multiset.add("apple", 2);

        assertTrue(multiset.setCount("apple", 2, 5));
        assertEquals(5, multiset.getCount("apple"));

        assertFalse(multiset.setCount("apple", 2, 10));
        assertEquals(5, multiset.getCount("apple"));

        assertTrue(multiset.setCount("banana", 0, 3));
        assertEquals(3, multiset.getCount("banana"));

        assertTrue(multiset.setCount("apple", 5, 0));
        assertFalse(multiset.contains("apple"));

        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("test", -1, 5));
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("test", 1, -1));
    }

    @Test
    public void testAdd() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.add("apple"));
        assertEquals(2, multiset.getCount("apple"));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testAddWithOccurrences() {
        assertEquals(0, multiset.add("apple", 3));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.add("apple", 2));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(0, multiset.add("banana", 0));
        assertEquals(0, multiset.getCount("banana"));

        assertThrows(IllegalArgumentException.class, () -> multiset.add("test", -1));

        multiset.add("overflow", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("overflow", 1));
    }

    @Test
    public void testAddAndGetCount() {
        assertEquals(3, multiset.addAndGetCount("apple", 3));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(5, multiset.addAndGetCount("apple", 2));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(0, multiset.addAndGetCount("banana", 0));
        assertEquals(0, multiset.getCount("banana"));

        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("test", -1));
    }

    @Test
    public void testAddAll() {
        List<String> list = Arrays.asList("a", "b", "a");
        assertTrue(multiset.addAll(list));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));

        assertFalse(multiset.addAll(Collections.emptyList()));
        assertFalse(multiset.addAll(null));
    }

    @Test
    public void testAddAllWithOccurrences() {
        List<String> list = Arrays.asList("a", "b");
        assertTrue(multiset.addAll(list, 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));

        assertFalse(multiset.addAll(list, 0));
        assertFalse(multiset.addAll(Collections.emptyList(), 5));
        assertFalse(multiset.addAll(null, 5));

        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(list, -1));
    }

    @Test
    public void testRemove() {
        multiset.add("apple", 3);

        assertTrue(multiset.remove("apple"));
        assertEquals(2, multiset.getCount("apple"));

        assertTrue(multiset.remove("apple"));
        assertEquals(1, multiset.getCount("apple"));

        assertTrue(multiset.remove("apple"));
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));

        assertFalse(multiset.remove("banana"));
    }

    @Test
    public void testRemoveWithOccurrences() {
        multiset.add("apple", 5);

        assertEquals(5, multiset.remove("apple", 2));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.remove("apple", 10));
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));

        assertEquals(0, multiset.remove("banana", 5));

        assertThrows(IllegalArgumentException.class, () -> multiset.remove("test", -1));
    }

    @Test
    public void testRemoveAndGetCount() {
        multiset.add("apple", 5);

        assertEquals(3, multiset.removeAndGetCount("apple", 2));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(0, multiset.removeAndGetCount("apple", 10));
        assertEquals(0, multiset.getCount("apple"));

        assertEquals(0, multiset.removeAndGetCount("banana", 5));

        assertThrows(IllegalArgumentException.class, () -> multiset.removeAndGetCount("test", -1));
    }

    @Test
    public void testRemoveAll() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        List<String> toRemove = Arrays.asList("a", "b");
        assertTrue(multiset.removeAll(toRemove));
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertTrue(multiset.contains("c"));

        assertFalse(multiset.removeAll(Collections.emptyList()));
        assertFalse(multiset.removeAll(null));
    }

    @Test
    public void testRemoveAllWithOccurrences() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        multiset.add("c", 1);

        List<String> toRemove = Arrays.asList("a", "b");
        assertTrue(multiset.removeAll(toRemove, 2));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));

        assertFalse(multiset.removeAll(toRemove, 0));
        assertFalse(multiset.removeAll(Collections.emptyList(), 5));
        assertFalse(multiset.removeAll(null, 5));

        assertThrows(IllegalArgumentException.class, () -> multiset.removeAll(toRemove, -1));
    }

    @Test
    public void testRemoveAllOccurrences() {
        multiset.add("apple", 5);
        multiset.add("banana", 3);

        assertEquals(5, multiset.removeAllOccurrences("apple"));
        assertFalse(multiset.contains("apple"));
        assertEquals(3, multiset.getCount("banana"));

        assertEquals(0, multiset.removeAllOccurrences("cherry"));
    }

    @Test
    public void testRemoveAllOccurrencesCollection() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        List<String> toRemove = Arrays.asList("a", "b");
        assertTrue(multiset.removeAllOccurrences(toRemove));
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertTrue(multiset.contains("c"));
    }

    @Test
    public void testRemoveAllOccurrencesIf() {
        multiset.add("apple", 3);
        multiset.add("banana", 2);
        multiset.add("cherry", 1);

        Predicate<String> startsWithA = s -> s.startsWith("a");
        assertTrue(multiset.removeAllOccurrencesIf(startsWithA));
        assertFalse(multiset.contains("apple"));
        assertTrue(multiset.contains("banana"));
        assertTrue(multiset.contains("cherry"));

        assertFalse(multiset.removeAllOccurrencesIf(startsWithA));

        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((Predicate) null));
    }

    @Test
    public void testRemoveAllOccurrencesIfObjInt() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        ObjIntPredicate<String> countGreaterThan2 = (element, count) -> count > 2;
        assertTrue(multiset.removeAllOccurrencesIf(countGreaterThan2));
        assertFalse(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertTrue(multiset.contains("c"));

        assertFalse(multiset.removeAllOccurrencesIf(countGreaterThan2));

        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((ObjIntPredicate<String>) null));
    }

    @Test
    public void testUpdateAllOccurrences() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.add("c", 1);

        ObjIntFunction<String, Integer> doubleCount = (element, count) -> count * 2;
        multiset.updateAllOccurrences(doubleCount);
        assertEquals(4, multiset.getCount("a"));
        assertEquals(6, multiset.getCount("b"));
        assertEquals(2, multiset.getCount("c"));

        ObjIntFunction<String, Integer> removeIfGreaterThan4 = (element, count) -> count > 4 ? 0 : count;
        multiset.updateAllOccurrences(removeIfGreaterThan4);
        assertEquals(4, multiset.getCount("a"));
        assertFalse(multiset.contains("b"));
        assertEquals(2, multiset.getCount("c"));

        assertThrows(IllegalArgumentException.class, () -> multiset.updateAllOccurrences(null));
    }

    @Test
    public void testComputeIfAbsent() {
        ToIntFunction<String> lengthFunction = String::length;

        assertEquals(5, multiset.computeIfAbsent("apple", lengthFunction));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(5, multiset.computeIfAbsent("apple", s -> 10));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(0, multiset.computeIfAbsent("test", s -> 0));
        assertEquals(0, multiset.getCount("test"));

        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfAbsent("test", null));
    }

    @Test
    public void testComputeIfPresent() {
        multiset.add("apple", 2);

        ObjIntFunction<String, Integer> doubleFunction = (e, count) -> count * 2;
        assertEquals(4, multiset.computeIfPresent("apple", doubleFunction));
        assertEquals(4, multiset.getCount("apple"));

        assertEquals(0, multiset.computeIfPresent("banana", doubleFunction));
        assertEquals(0, multiset.getCount("banana"));

        ObjIntFunction<String, Integer> zeroFunction = (e, count) -> 0;
        assertEquals(0, multiset.computeIfPresent("apple", zeroFunction));
        assertFalse(multiset.contains("apple"));

        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfPresent("test", null));
    }

    @Test
    public void testCompute() {
        ObjIntFunction<String, Integer> addOneFunction = (e, count) -> count + 1;

        assertEquals(1, multiset.compute("apple", addOneFunction));
        assertEquals(1, multiset.getCount("apple"));

        assertEquals(2, multiset.compute("apple", addOneFunction));
        assertEquals(2, multiset.getCount("apple"));

        ObjIntFunction<String, Integer> zeroFunction = (e, count) -> 0;
        assertEquals(0, multiset.compute("apple", zeroFunction));
        assertFalse(multiset.contains("apple"));

        assertThrows(IllegalArgumentException.class, () -> multiset.compute("test", null));
    }

    @Test
    public void testMerge() {
        multiset.add("apple", 2);

        IntBiFunction<Integer> sumFunction = Integer::sum;
        assertEquals(5, multiset.merge("apple", 3, sumFunction));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(4, multiset.merge("banana", 4, sumFunction));
        assertEquals(4, multiset.getCount("banana"));

        IntBiFunction<Integer> zeroFunction = (oldCount, value) -> 0;
        assertEquals(0, multiset.merge("apple", 10, zeroFunction));
        assertFalse(multiset.contains("apple"));

        assertThrows(IllegalArgumentException.class, () -> multiset.merge("test", 5, null));
    }

    @Test
    public void testRetainAll() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.add("c", 1);

        List<String> toRetain = Arrays.asList("a", "c");
        assertTrue(multiset.retainAll(toRetain));
        assertTrue(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertTrue(multiset.contains("c"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("c"));

        assertTrue(multiset.retainAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());

        multiset.add("test", 1);
        assertTrue(multiset.retainAll(null));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testContains() {
        assertFalse(multiset.contains("apple"));

        multiset.add("apple", 3);
        assertTrue(multiset.contains("apple"));

        multiset.remove("apple", 3);
        assertFalse(multiset.contains("apple"));

        assertFalse(multiset.contains(null));
        multiset.add(null, 1);
        assertTrue(multiset.contains(null));
    }

    @Test
    public void testContainsAll() {
        assertTrue(multiset.containsAll(Collections.emptyList()));
        assertTrue(multiset.containsAll(null));

        multiset.add("a", 1);
        multiset.add("b", 1);

        assertTrue(multiset.containsAll(Arrays.asList("a", "b")));
        assertTrue(multiset.containsAll(Arrays.asList("a")));
        assertFalse(multiset.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testElementSet() {
        Set<String> elements = multiset.elementSet();
        assertTrue(elements.isEmpty());

        multiset.add("a", 3);
        multiset.add("b", 1);

        elements = multiset.elementSet();
        assertEquals(2, elements.size());
        assertTrue(elements.contains("a"));
        assertTrue(elements.contains("b"));
    }

    @Test
    public void testEntrySet() {
        Set<Multiset.Entry<String>> entries = multiset.entrySet();
        assertTrue(entries.isEmpty());

        multiset.add("a", 2);
        multiset.add("b", 3);

        entries = multiset.entrySet();
        assertEquals(2, entries.size());

        boolean foundA = false, foundB = false;
        for (Multiset.Entry<String> entry : entries) {
            if ("a".equals(entry.element()) && entry.count() == 2) {
                foundA = true;
            } else if ("b".equals(entry.element()) && entry.count() == 3) {
                foundB = true;
            }
        }
        assertTrue(foundA && foundB);

        Multiset.Entry<String> testEntry = new Multiset.ImmutableEntry<>("a", 2);
        assertTrue(entries.contains(testEntry));

        Multiset.Entry<String> wrongEntry = new Multiset.ImmutableEntry<>("a", 3);
        assertFalse(entries.contains(wrongEntry));

        assertFalse(entries.contains("not an entry"));
    }

    @Test
    public void testIterator() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        Iterator<String> iter = multiset.iterator();
        List<String> elements = new ArrayList<>();
        while (iter.hasNext()) {
            elements.add(iter.next());
        }

        assertEquals(3, elements.size());
        assertEquals(2, Collections.frequency(elements, "a"));
        assertEquals(1, Collections.frequency(elements, "b"));

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testSize() {
        assertEquals(0, multiset.size());

        multiset.add("a", 3);
        multiset.add("b", 2);
        assertEquals(5, multiset.size());

        multiset.remove("a", 1);
        assertEquals(4, multiset.size());
    }

    @Test
    public void testCountOfDistinctElements() {
        assertEquals(0, multiset.countOfDistinctElements());

        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertEquals(3, multiset.countOfDistinctElements());
        assertEquals(6, multiset.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(multiset.isEmpty());

        multiset.add("test", 1);
        assertFalse(multiset.isEmpty());

        multiset.clear();
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testClear() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertFalse(multiset.isEmpty());

        multiset.clear();
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
    }

    @Test
    public void testToArray() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        Object[] array = multiset.toArray();
        assertEquals(3, array.length);

        List<Object> arrayList = Arrays.asList(array);
        assertEquals(2, Collections.frequency(arrayList, "a"));
        assertEquals(1, Collections.frequency(arrayList, "b"));
    }

    @Test
    public void testToArrayTyped() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        String[] array = multiset.toArray(new String[0]);
        assertEquals(3, array.length);

        List<String> arrayList = Arrays.asList(array);
        assertEquals(2, Collections.frequency(arrayList, "a"));
        assertEquals(1, Collections.frequency(arrayList, "b"));

        String[] largeArray = multiset.toArray(new String[10]);
        assertEquals(10, largeArray.length);
        assertNull(largeArray[3]);

        assertThrows(IllegalArgumentException.class, () -> multiset.toArray((String[]) null));
    }

    @Test
    public void testToMap() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Map<String, Integer> map = multiset.toMap();
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testToMapWithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        TreeMap<String, Integer> map = multiset.toMap(size -> new TreeMap<>());
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testToMapSortedByOccurrences() {
        multiset.add("a", 1);
        multiset.add("b", 3);
        multiset.add("c", 2);

        Map<String, Integer> sorted = multiset.toMapSortedByOccurrences();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(sorted.entrySet());

        assertEquals(1, entries.get(0).getValue().intValue());
        assertEquals(2, entries.get(1).getValue().intValue());
        assertEquals(3, entries.get(2).getValue().intValue());
    }

    @Test
    public void testToMapSortedByOccurrencesWithComparator() {
        multiset.add("a", 1);
        multiset.add("b", 3);
        multiset.add("c", 2);

        Map<String, Integer> sorted = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(sorted.entrySet());

        assertEquals(3, entries.get(0).getValue().intValue());
        assertEquals(2, entries.get(1).getValue().intValue());
        assertEquals(1, entries.get(2).getValue().intValue());
    }

    @Test
    public void testToMapSortedByKey() {
        multiset.add("c", 1);
        multiset.add("a", 2);
        multiset.add("b", 3);

        Map<String, Integer> sorted = multiset.toMapSortedByKey(String.CASE_INSENSITIVE_ORDER);
        List<String> keys = new ArrayList<>(sorted.keySet());

        assertEquals("a", keys.get(0));
        assertEquals("b", keys.get(1));
        assertEquals("c", keys.get(2));
    }

    @Test
    public void testToImmutableMap() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        ImmutableMap<String, Integer> immutableMap = multiset.toImmutableMap();
        assertEquals(2, immutableMap.size());
        assertEquals(Integer.valueOf(3), immutableMap.get("a"));
        assertEquals(Integer.valueOf(2), immutableMap.get("b"));

        assertThrows(UnsupportedOperationException.class, () -> immutableMap.put("c", 1));
    }

    @Test
    public void testToImmutableMapWithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        ImmutableMap<String, Integer> immutableMap = multiset.toImmutableMap(HashMap::new);
        assertEquals(2, immutableMap.size());
        assertEquals(Integer.valueOf(3), immutableMap.get("a"));
        assertEquals(Integer.valueOf(2), immutableMap.get("b"));
    }

    @Test
    public void testForEach() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        List<String> visited = new ArrayList<>();
        Consumer<String> visitor = visited::add;

        multiset.forEach(visitor);
        assertEquals(3, visited.size());
        assertEquals(2, Collections.frequency(visited, "a"));
        assertEquals(1, Collections.frequency(visited, "b"));

        assertThrows(IllegalArgumentException.class, () -> multiset.forEach((Consumer<String>) null));
    }

    @Test
    public void testForEachObjInt() {
        multiset.add("a", 2);
        multiset.add("b", 3);

        Map<String, Integer> visited = new HashMap<>();
        ObjIntConsumer<String> visitor = visited::put;

        multiset.forEach(visitor);
        assertEquals(2, visited.size());
        assertEquals(Integer.valueOf(2), visited.get("a"));
        assertEquals(Integer.valueOf(3), visited.get("b"));

        assertThrows(IllegalArgumentException.class, () -> multiset.forEach((ObjIntConsumer<String>) null));
    }

    @Test
    public void testElements() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        Stream<String> stream = multiset.elements();
        List<String> elements = stream.toList();

        assertEquals(3, elements.size());
        assertEquals(2, Collections.frequency(elements, "a"));
        assertEquals(1, Collections.frequency(elements, "b"));
    }

    @Test
    public void testEntries() {
        multiset.add("a", 2);
        multiset.add("b", 3);

        Stream<Multiset.Entry<String>> stream = multiset.entries();
        List<Multiset.Entry<String>> entries = stream.toList();

        assertEquals(2, entries.size());

        boolean foundA = false, foundB = false;
        for (Multiset.Entry<String> entry : entries) {
            if ("a".equals(entry.element()) && entry.count() == 2) {
                foundA = true;
            } else if ("b".equals(entry.element()) && entry.count() == 3) {
                foundB = true;
            }
        }
        assertTrue(foundA && foundB);
    }

    @Test
    public void testApply() {
        multiset.add("a", 1);
        multiset.add("b", 2);

        Integer distinctCount = multiset.apply(ms -> ms.countOfDistinctElements());
        assertEquals(2, distinctCount.intValue());

        assertThrows(RuntimeException.class, () -> multiset.apply(ms -> {
            throw new RuntimeException("Test exception");
        }));
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> emptyResult = multiset.applyIfNotEmpty(ms -> ms.size());
        assertFalse(emptyResult.isPresent());

        multiset.add("test", 1);
        Optional<Integer> result = multiset.applyIfNotEmpty(ms -> ms.size());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().intValue());
    }

    @Test
    public void testAccept() {
        multiset.add("test", 1);

        final boolean[] visited = { false };
        multiset.accept(ms -> visited[0] = true);
        assertTrue(visited[0]);

        assertThrows(RuntimeException.class, () -> multiset.accept(ms -> {
            throw new RuntimeException("Test exception");
        }));
    }

    @Test
    public void testAcceptIfNotEmpty() {
        final boolean[] visited = { false };

        multiset.acceptIfNotEmpty(ms -> visited[0] = true).orElse(() -> visited[0] = false);
        assertFalse(visited[0]);

        multiset.add("test", 1);
        multiset.acceptIfNotEmpty(ms -> visited[0] = true).orElse(() -> visited[0] = false);
        assertTrue(visited[0]);
    }

    @Test
    public void testHashCode() {
        Multiset<String> ms1 = new Multiset<>();
        Multiset<String> ms2 = new Multiset<>();

        assertEquals(ms1.hashCode(), ms2.hashCode());

        ms1.add("a", 2);
        ms2.add("a", 2);
        assertEquals(ms1.hashCode(), ms2.hashCode());

        ms2.add("b", 1);
        assertNotEquals(ms1.hashCode(), ms2.hashCode());
    }

    @Test
    public void testEquals() {
        Multiset<String> ms1 = new Multiset<>();
        Multiset<String> ms2 = new Multiset<>();

        assertTrue(ms1.equals(ms2));
        assertTrue(ms1.equals(ms1));

        ms1.add("a", 2);
        ms1.add("b", 1);
        ms2.add("b", 1);
        ms2.add("a", 2);
        assertTrue(ms1.equals(ms2));

        ms2.add("c", 1);
        assertFalse(ms1.equals(ms2));

        assertFalse(ms1.equals(null));
        assertFalse(ms1.equals("not a multiset"));
    }

    @Test
    public void testToString() {
        String emptyString = multiset.toString();
        assertEquals("{}", emptyString);

        multiset.add("a", 2);
        multiset.add("b", 1);
        String result = multiset.toString();
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testImmutableEntry() {
        Multiset.ImmutableEntry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals("test", entry1.element());
        assertEquals(5, entry1.count());

        Multiset.ImmutableEntry<String> entry2 = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals(entry1, entry2);
        assertEquals(entry1.hashCode(), entry2.hashCode());

        Multiset.ImmutableEntry<String> entry3 = new Multiset.ImmutableEntry<>("test", 3);
        assertNotEquals(entry1, entry3);

        Multiset.ImmutableEntry<String> entry4 = new Multiset.ImmutableEntry<>("other", 5);
        assertNotEquals(entry1, entry4);

        assertNotEquals(entry1, "not an entry");

        assertEquals("test x 5", entry1.toString());

        Multiset.ImmutableEntry<String> entryMultiple = new Multiset.ImmutableEntry<>("test", 3);
        assertEquals("test x 3", entryMultiple.toString());

        Multiset.ImmutableEntry<String> nullEntry = new Multiset.ImmutableEntry<>(null, 2);
        assertEquals("null x 2", nullEntry.toString());
        assertEquals(2, nullEntry.hashCode());
    }

    @Test
    public void testEdgeCases() {
        multiset.add(null, 3);
        assertEquals(3, multiset.getCount(null));
        assertTrue(multiset.contains(null));

        assertEquals(0, multiset.add("zero", 0));
        assertEquals(0, multiset.getCount("zero"));
        assertFalse(multiset.contains("zero"));

        Multiset<Integer> largeMs = new Multiset<>();
        for (int i = 0; i < 1000; i++) {
            largeMs.add(i, i % 10 + 1);
        }
        assertEquals(1000, largeMs.countOfDistinctElements());

        Multiset<String> overflowMs = new Multiset<>();
        overflowMs.add("max", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> overflowMs.add("max", 1));
    }
}
