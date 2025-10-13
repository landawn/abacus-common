package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

@Tag("new-test")
public class Multiset100Test extends TestBase {

    private Multiset<String> multiset;
    private Multiset<Integer> intMultiset;

    @BeforeEach
    public void setUp() {
        multiset = new Multiset<>();
        intMultiset = new Multiset<>();
    }

    @Test
    @DisplayName("Test default constructor")
    public void testDefaultConstructor() {
        Multiset<String> ms = new Multiset<>();
        assertNotNull(ms);
        assertTrue(ms.isEmpty());
        assertEquals(0, ms.size());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        Multiset<String> ms = new Multiset<>(100);
        assertNotNull(ms);
        assertTrue(ms.isEmpty());
    }

    @Test
    @DisplayName("Test constructor with collection")
    public void testConstructorWithCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "a", "b", "a");
        Multiset<String> ms = new Multiset<>(list);
        assertEquals(6, ms.size());
        assertEquals(3, ms.getCount("a"));
        assertEquals(2, ms.getCount("b"));
        assertEquals(1, ms.getCount("c"));
    }

    @Test
    @DisplayName("Test constructor with null collection")
    public void testConstructorWithNullCollection() {
        Multiset<String> ms = new Multiset<>((Collection<String>) null);
        assertNotNull(ms);
        assertTrue(ms.isEmpty());
    }

    @Test
    @DisplayName("Test constructor with map type")
    public void testConstructorWithMapType() {
        Multiset<String> ms = new Multiset<>(LinkedHashMap.class);
        assertNotNull(ms);
        assertTrue(ms.isEmpty());
    }

    @Test
    @DisplayName("Test constructor with map supplier")
    public void testConstructorWithMapSupplier() {
        Multiset<String> ms = new Multiset<>(() -> new TreeMap<>());
        assertNotNull(ms);
        assertTrue(ms.isEmpty());
    }

    @Test
    @DisplayName("Test of() factory method")
    public void testOf() {
        Multiset<String> ms = Multiset.of("a", "b", "c", "a", "b", "a");
        assertEquals(6, ms.size());
        assertEquals(3, ms.getCount("a"));
        assertEquals(2, ms.getCount("b"));
        assertEquals(1, ms.getCount("c"));
    }

    @Test
    @DisplayName("Test of() with empty array")
    public void testOfEmpty() {
        Multiset<String> ms = Multiset.of();
        assertTrue(ms.isEmpty());
    }

    @Test
    @DisplayName("Test create() with collection")
    public void testCreateWithCollection() {
        List<String> list = Arrays.asList("x", "y", "z", "x");
        Multiset<String> ms = Multiset.create(list);
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("x"));
    }

    @Test
    @DisplayName("Test create() with iterator")
    public void testCreateWithIterator() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> ms = Multiset.create(list.iterator());
        assertEquals(6, ms.size());
        assertEquals(3, ms.getCount("a"));
    }

    @Test
    @DisplayName("Test create() with null iterator")
    public void testCreateWithNullIterator() {
        Multiset<String> ms = Multiset.create((Iterator<String>) null);
        assertTrue(ms.isEmpty());
    }

    @Test
    @DisplayName("Test occurrencesOf()")
    public void testOccurrencesOf() {
        multiset.add("apple", 3);
        multiset.add("banana", 2);
        assertEquals(3, multiset.occurrencesOf("apple"));
        assertEquals(2, multiset.occurrencesOf("banana"));
        assertEquals(0, multiset.occurrencesOf("orange"));
    }

    @Test
    @DisplayName("Test getCount()")
    public void testGetCount() {
        multiset.add("apple", 5);
        assertEquals(5, multiset.getCount("apple"));
        assertEquals(0, multiset.getCount("nonexistent"));
    }

    @Test
    @DisplayName("Test deprecated count() method")
    public void testCountDeprecated() {
        multiset.add("item", 7);
        assertEquals(7, multiset.count("item"));
    }

    @Test
    @DisplayName("Test minOccurrences()")
    public void testMinOccurrences() {
        assertTrue(multiset.minOccurrences().isEmpty());

        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 5);

        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left());
        assertEquals("b", min.get().right());
    }

    @Test
    @DisplayName("Test maxOccurrences()")
    public void testMaxOccurrences() {
        assertTrue(multiset.maxOccurrences().isEmpty());

        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 5);

        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(5, max.get().left());
        assertEquals("c", max.get().right());
    }

    @Test
    @DisplayName("Test allMinOccurrences()")
    public void testAllMinOccurrences() {
        assertTrue(multiset.allMinOccurrences().isEmpty());

        multiset.add("a", 2);
        multiset.add("b", 1);
        multiset.add("c", 1);
        multiset.add("d", 3);

        Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(1, allMin.get().left());
        assertTrue(allMin.get().right().containsAll(Arrays.asList("b", "c")));
        assertEquals(2, allMin.get().right().size());
    }

    @Test
    @DisplayName("Test allMaxOccurrences()")
    public void testAllMaxOccurrences() {
        assertTrue(multiset.allMaxOccurrences().isEmpty());

        multiset.add("a", 3);
        multiset.add("b", 3);
        multiset.add("c", 1);
        multiset.add("d", 2);

        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(3, allMax.get().left());
        assertTrue(allMax.get().right().containsAll(Arrays.asList("a", "b")));
        assertEquals(2, allMax.get().right().size());
    }

    @Test
    @DisplayName("Test sumOfOccurrences()")
    public void testSumOfOccurrences() {
        assertEquals(0, multiset.sumOfOccurrences());

        multiset.add("a", 10);
        multiset.add("b", 20);
        multiset.add("c", 30);

        assertEquals(60, multiset.sumOfOccurrences());
    }

    @Test
    @DisplayName("Test averageOfOccurrences()")
    public void testAverageOfOccurrences() {
        assertTrue(multiset.averageOfOccurrences().isEmpty());

        multiset.add("a", 10);
        multiset.add("b", 20);
        multiset.add("c", 30);

        OptionalDouble avg = multiset.averageOfOccurrences();
        assertTrue(avg.isPresent());
        assertEquals(20.0, avg.getAsDouble(), 0.001);
    }

    @Test
    @DisplayName("Test setCount()")
    public void testSetCount() {
        assertEquals(0, multiset.setCount("apple", 5));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(5, multiset.setCount("apple", 3));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.setCount("apple", 0));
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    @DisplayName("Test setCount() with negative count")
    public void testSetCountNegative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("apple", -1));
    }

    @Test
    @DisplayName("Test conditional setCount()")
    public void testConditionalSetCount() {
        multiset.add("apple", 3);

        assertTrue(multiset.setCount("apple", 3, 5));
        assertEquals(5, multiset.getCount("apple"));

        assertFalse(multiset.setCount("apple", 3, 7));
        assertEquals(5, multiset.getCount("apple"));

        assertTrue(multiset.setCount("apple", 5, 0));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    @DisplayName("Test add() single element")
    public void testAddSingle() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));

        assertTrue(multiset.add("apple"));
        assertEquals(2, multiset.getCount("apple"));
    }

    @Test
    @DisplayName("Test add() with occurrences")
    public void testAddWithOccurrences() {
        assertEquals(0, multiset.add("apple", 3));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.add("apple", 2));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(5, multiset.add("apple", 0));
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    @DisplayName("Test add() with negative occurrences")
    public void testAddNegativeOccurrences() {
        assertThrows(IllegalArgumentException.class, () -> multiset.add("apple", -1));
    }

    @Test
    @DisplayName("Test addAndGetCount()")
    public void testAddAndGetCount() {
        assertEquals(3, multiset.addAndGetCount("apple", 3));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(8, multiset.addAndGetCount("apple", 5));
        assertEquals(8, multiset.getCount("apple"));
    }

    @Test
    @DisplayName("Test addAll() collection")
    public void testAddAllCollection() {
        List<String> items = Arrays.asList("a", "b", "a", "c");
        assertTrue(multiset.addAll(items));

        assertEquals(4, multiset.size());
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    @DisplayName("Test addAll() empty collection")
    public void testAddAllEmptyCollection() {
        assertFalse(multiset.addAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
    }

    @Test
    @DisplayName("Test addAll() with occurrences")
    public void testAddAllWithOccurrences() {
        List<String> items = Arrays.asList("a", "b", "c");
        assertTrue(multiset.addAll(items, 3));

        assertEquals(9, multiset.size());
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));
        assertEquals(3, multiset.getCount("c"));
    }

    @Test
    @DisplayName("Test remove() single occurrence")
    public void testRemoveSingle() {
        multiset.add("apple", 3);

        assertTrue(multiset.remove("apple"));
        assertEquals(2, multiset.getCount("apple"));

        assertFalse(multiset.remove("banana"));
    }

    @Test
    @DisplayName("Test remove() with occurrences")
    public void testRemoveWithOccurrences() {
        multiset.add("apple", 5);

        assertEquals(5, multiset.remove("apple", 2));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.remove("apple", 5));
        assertEquals(0, multiset.getCount("apple"));

        assertEquals(0, multiset.remove("banana", 1));
    }

    @Test
    @DisplayName("Test removeAndGetCount()")
    public void testRemoveAndGetCount() {
        multiset.add("apple", 10);

        assertEquals(7, multiset.removeAndGetCount("apple", 3));
        assertEquals(7, multiset.getCount("apple"));

        assertEquals(0, multiset.removeAndGetCount("apple", 10));
        assertFalse(multiset.contains("apple"));

        assertEquals(0, multiset.removeAndGetCount("banana", 5));
    }

    @Test
    @DisplayName("Test removeAll() collection")
    public void testRemoveAllCollection() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        assertTrue(multiset.removeAll(Arrays.asList("a", "c")));
        assertEquals(2, multiset.size());
        assertEquals(2, multiset.getCount("b"));
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("c"));
    }

    @Test
    @DisplayName("Test removeAll() with occurrences")
    public void testRemoveAllWithOccurrences() {
        multiset.add("a", 5);
        multiset.add("b", 3);

        assertTrue(multiset.removeAll(Arrays.asList("a", "b"), 2));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    @DisplayName("Test removeAllOccurrences() single element")
    public void testRemoveAllOccurrencesSingle() {
        multiset.add("apple", 5);

        assertEquals(5, multiset.removeAllOccurrences("apple"));
        assertFalse(multiset.contains("apple"));

        assertEquals(0, multiset.removeAllOccurrences("banana"));
    }

    @Test
    @DisplayName("Test removeAllOccurrences() collection")
    public void testRemoveAllOccurrencesCollection() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        assertTrue(multiset.removeAllOccurrences(Arrays.asList("a", "c")));
        assertEquals(2, multiset.size());
        assertEquals(2, multiset.getCount("b"));
    }

    @Test
    @DisplayName("Test removeAllOccurrencesIf() with predicate")
    public void testRemoveAllOccurrencesIfPredicate() {
        multiset.add("apple", 3);
        multiset.add("apricot", 2);
        multiset.add("banana", 1);

        assertTrue(multiset.removeAllOccurrencesIf(s -> s.startsWith("ap")));
        assertEquals(1, multiset.size());
        assertEquals(1, multiset.getCount("banana"));
    }

    @Test
    @DisplayName("Test removeAllOccurrencesIf() with ObjIntPredicate")
    public void testRemoveAllOccurrencesIfObjIntPredicate() {
        multiset.add("a", 1);
        multiset.add("b", 2);
        multiset.add("c", 3);
        multiset.add("d", 4);

        assertTrue(multiset.removeAllOccurrencesIf((element, count) -> count >= 3));
        assertEquals(3, multiset.size());
        assertTrue(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
        assertFalse(multiset.contains("d"));
    }

    @Test
    @DisplayName("Test updateAllOccurrences()")
    public void testUpdateAllOccurrences() {
        multiset.add("a", 1);
        multiset.add("b", 2);
        multiset.add("c", 3);

        multiset.updateAllOccurrences((element, count) -> count * 2);

        assertEquals(2, multiset.getCount("a"));
        assertEquals(4, multiset.getCount("b"));
        assertEquals(6, multiset.getCount("c"));
    }

    @Test
    @DisplayName("Test updateAllOccurrences() removing elements")
    public void testUpdateAllOccurrencesRemove() {
        multiset.add("a", 1);
        multiset.add("b", 2);
        multiset.add("c", 3);

        multiset.updateAllOccurrences((element, count) -> count >= 2 ? count : 0);

        assertFalse(multiset.contains("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(3, multiset.getCount("c"));
    }

    @Test
    @DisplayName("Test computeIfAbsent()")
    public void testComputeIfAbsent() {
        assertEquals(5, multiset.computeIfAbsent("apple", e -> 5));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(5, multiset.computeIfAbsent("apple", e -> 10));
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    @DisplayName("Test computeIfPresent()")
    public void testComputeIfPresent() {
        assertEquals(0, multiset.computeIfPresent("apple", (e, count) -> count + 5));

        multiset.add("apple", 3);
        assertEquals(8, multiset.computeIfPresent("apple", (e, count) -> count + 5));
        assertEquals(8, multiset.getCount("apple"));

        assertEquals(0, multiset.computeIfPresent("apple", (e, count) -> 0));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    @DisplayName("Test compute()")
    public void testCompute() {
        assertEquals(5, multiset.compute("apple", (e, count) -> count + 5));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(10, multiset.compute("apple", (e, count) -> count * 2));
        assertEquals(10, multiset.getCount("apple"));

        assertEquals(0, multiset.compute("apple", (e, count) -> 0));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    @DisplayName("Test merge()")
    public void testMerge() {
        assertEquals(5, multiset.merge("apple", 5, (oldVal, newVal) -> oldVal + newVal));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(8, multiset.merge("apple", 3, (oldVal, newVal) -> oldVal + newVal));
        assertEquals(8, multiset.getCount("apple"));

        assertEquals(0, multiset.merge("apple", 1, (oldVal, newVal) -> 0));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    @DisplayName("Test contains()")
    public void testContains() {
        assertFalse(multiset.contains("apple"));

        multiset.add("apple");
        assertTrue(multiset.contains("apple"));

        multiset.removeAllOccurrences("apple");
        assertFalse(multiset.contains("apple"));
    }

    @Test
    @DisplayName("Test containsAll()")
    public void testContainsAll() {
        multiset.add("a");
        multiset.add("b");
        multiset.add("c");

        assertTrue(multiset.containsAll(Arrays.asList("a", "b")));
        assertTrue(multiset.containsAll(Arrays.asList("a", "b", "c")));
        assertFalse(multiset.containsAll(Arrays.asList("a", "b", "d")));
        assertTrue(multiset.containsAll(Collections.emptyList()));
    }

    @Test
    @DisplayName("Test retainAll()")
    public void testRetainAll() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        assertTrue(multiset.retainAll(Arrays.asList("a", "c")));
        assertEquals(4, multiset.size());
        assertEquals(3, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("c"));
        assertFalse(multiset.contains("b"));
    }

    @Test
    @DisplayName("Test retainAll() with empty collection")
    public void testRetainAllEmpty() {
        multiset.add("a", 3);

        assertTrue(multiset.retainAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
    }

    @Test
    @DisplayName("Test elementSet()")
    public void testElementSet() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        Set<String> elements = multiset.elementSet();
        assertEquals(3, elements.size());
        assertTrue(elements.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    @DisplayName("Test entrySet()")
    public void testEntrySet() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Set<Multiset.Entry<String>> entries = multiset.entrySet();
        assertEquals(2, entries.size());

        for (Multiset.Entry<String> entry : entries) {
            if (entry.element().equals("a")) {
                assertEquals(3, entry.count());
            } else if (entry.element().equals("b")) {
                assertEquals(2, entry.count());
            }
        }
    }

    @Test
    @DisplayName("Test entrySet() contains")
    public void testEntrySetContains() {
        multiset.add("a", 3);

        Set<Multiset.Entry<String>> entries = multiset.entrySet();

        Multiset.Entry<String> testEntry = new Multiset.Entry<String>() {
            public String element() {
                return "a";
            }

            public int count() {
                return 3;
            }

            public boolean equals(Object o) {
                if (o instanceof Multiset.Entry<?> e) {
                    return count() == e.count() && Objects.equals(element(), e.element());
                }
                return false;
            }

            public int hashCode() {
                return Objects.hashCode(element()) ^ count();
            }

            public String toString() {
                return element() + " x " + count();
            }
        };

        assertTrue(entries.contains(testEntry));
    }

    @Test
    @DisplayName("Test iterator()")
    public void testIterator() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        int count = 0;
        Map<String, Integer> counts = new HashMap<>();

        for (String element : multiset) {
            count++;
            counts.merge(element, 1, Integer::sum);
        }

        assertEquals(5, count);
        assertEquals(3, counts.get("a").intValue());
        assertEquals(2, counts.get("b").intValue());
    }

    @Test
    @DisplayName("Test size()")
    public void testSize() {
        assertEquals(0, multiset.size());

        multiset.add("a", 3);
        assertEquals(3, multiset.size());

        multiset.add("b", 2);
        assertEquals(5, multiset.size());

        multiset.remove("a", 1);
        assertEquals(4, multiset.size());
    }

    @Test
    @DisplayName("Test countOfDistinctElements()")
    public void testCountOfDistinctElements() {
        assertEquals(0, multiset.countOfDistinctElements());

        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        assertEquals(3, multiset.countOfDistinctElements());
    }

    @Test
    @DisplayName("Test isEmpty()")
    public void testIsEmpty() {
        assertTrue(multiset.isEmpty());

        multiset.add("a");
        assertFalse(multiset.isEmpty());

        multiset.clear();
        assertTrue(multiset.isEmpty());
    }

    @Test
    @DisplayName("Test clear()")
    public void testClear() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        assertFalse(multiset.isEmpty());

        multiset.clear();

        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
        assertEquals(0, multiset.countOfDistinctElements());
    }

    @Test
    @DisplayName("Test toArray()")
    public void testToArray() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Object[] array = multiset.toArray();
        assertEquals(5, array.length);

        Map<String, Integer> counts = new HashMap<>();
        for (Object o : array) {
            counts.merge((String) o, 1, Integer::sum);
        }

        assertEquals(3, counts.get("a").intValue());
        assertEquals(2, counts.get("b").intValue());
    }

    @Test
    @DisplayName("Test toArray(T[])")
    public void testToArrayTyped() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        String[] array = multiset.toArray(new String[0]);
        assertEquals(5, array.length);

        String[] providedArray = new String[10];
        String[] result = multiset.toArray(providedArray);
        assertSame(providedArray, result);
    }

    @Test
    @DisplayName("Test toMap()")
    public void testToMap() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Map<String, Integer> map = multiset.toMap();
        assertEquals(2, map.size());
        assertEquals(3, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
    }

    @Test
    @DisplayName("Test toMap(IntFunction)")
    public void testToMapWithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        LinkedHashMap<String, Integer> map = multiset.toMap(LinkedHashMap::new);
        assertEquals(2, map.size());
        assertEquals(3, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
    }

    @Test
    @DisplayName("Test toMapSortedByOccurrences()")
    public void testToMapSortedByOccurrences() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);

        Map<String, Integer> map = multiset.toMapSortedByOccurrences();
        List<Integer> values = new ArrayList<>(map.values());
        assertEquals(Arrays.asList(1, 2, 3), values);
    }

    @Test
    @DisplayName("Test toMapSortedByOccurrences(Comparator)")
    public void testToMapSortedByOccurrencesWithComparator() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);

        Map<String, Integer> map = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
        List<Integer> values = new ArrayList<>(map.values());
        assertEquals(Arrays.asList(3, 2, 1), values);
    }

    @Test
    @DisplayName("Test toMapSortedByKey()")
    public void testToMapSortedByKey() {
        multiset.add("c", 1);
        multiset.add("a", 3);
        multiset.add("b", 2);

        Map<String, Integer> map = multiset.toMapSortedByKey(Comparator.naturalOrder());
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    @Test
    @DisplayName("Test toImmutableMap()")
    public void testToImmutableMap() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        ImmutableMap<String, Integer> map = multiset.toImmutableMap();
        assertEquals(2, map.size());
        assertEquals(3, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
    }

    @Test
    @DisplayName("Test forEach(Consumer)")
    public void testForEachConsumer() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        List<String> elements = new ArrayList<>();
        multiset.forEach(e -> elements.add(e));

        assertEquals(5, elements.size());
        assertEquals(3, Collections.frequency(elements, "a"));
        assertEquals(2, Collections.frequency(elements, "b"));
    }

    @Test
    @DisplayName("Test forEach(ObjIntConsumer)")
    public void testForEachObjIntConsumer() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Map<String, Integer> counts = new HashMap<>();
        multiset.forEach((element, count) -> counts.put(element, count));

        assertEquals(2, counts.size());
        assertEquals(3, counts.get("a").intValue());
        assertEquals(2, counts.get("b").intValue());
    }

    @Test
    @DisplayName("Test elements()")
    public void testElements() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        List<String> elements = multiset.elements().toList();
        assertEquals(5, elements.size());
        assertEquals(3, Collections.frequency(elements, "a"));
        assertEquals(2, Collections.frequency(elements, "b"));
    }

    @Test
    @DisplayName("Test entries()")
    public void testEntries() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        List<Multiset.Entry<String>> entries = multiset.entries().toList();
        assertEquals(2, entries.size());

        Map<String, Integer> counts = entries.stream().collect(Collectors.toMap(Multiset.Entry::element, Multiset.Entry::count));

        assertEquals(3, counts.get("a").intValue());
        assertEquals(2, counts.get("b").intValue());
    }

    @Test
    @DisplayName("Test apply()")
    public void testApply() {
        multiset.add("a", 3);

        Integer result = multiset.apply(ms -> ms.size());
        assertEquals(3, result);
    }

    @Test
    @DisplayName("Test applyIfNotEmpty()")
    public void testApplyIfNotEmpty() {
        Optional<Integer> emptyResult = multiset.applyIfNotEmpty(ms -> ms.size());
        assertFalse(emptyResult.isPresent());

        multiset.add("a", 3);
        Optional<Integer> result = multiset.applyIfNotEmpty(ms -> ms.size());
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    @DisplayName("Test accept()")
    public void testAccept() {
        final boolean[] called = { false };
        multiset.accept(ms -> {
            called[0] = true;
            ms.add("test");
        });

        assertTrue(called[0]);
        assertTrue(multiset.contains("test"));
    }

    @Test
    @DisplayName("Test acceptIfNotEmpty()")
    public void testAcceptIfNotEmpty() {
        final boolean[] called = { false };

        multiset.acceptIfNotEmpty(ms -> called[0] = true).orElse(() -> called[0] = false);

        assertFalse(called[0]);

        multiset.add("a");
        multiset.acceptIfNotEmpty(ms -> called[0] = true);

        assertTrue(called[0]);
    }

    @Test
    @DisplayName("Test equals()")
    public void testEquals() {
        Multiset<String> ms1 = new Multiset<>();
        Multiset<String> ms2 = new Multiset<>();

        assertEquals(ms1, ms2);
        assertEquals(ms1, ms1);

        ms1.add("a", 3);
        ms1.add("b", 2);

        assertNotEquals(ms1, ms2);

        ms2.add("b", 2);
        ms2.add("a", 3);

        assertEquals(ms1, ms2);

        assertNotEquals(ms1, null);
        assertNotEquals(ms1, "not a multiset");
    }

    @Test
    @DisplayName("Test hashCode()")
    public void testHashCode() {
        Multiset<String> ms1 = new Multiset<>();
        Multiset<String> ms2 = new Multiset<>();

        assertEquals(ms1.hashCode(), ms2.hashCode());

        ms1.add("a", 3);
        ms2.add("a", 3);

        assertEquals(ms1.hashCode(), ms2.hashCode());
    }

    @Test
    @DisplayName("Test toString()")
    public void testToString() {
        assertTrue(multiset.toString().contains("{}"));

        multiset.add("a", 3);
        multiset.add("b", 2);

        String str = multiset.toString();
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
        assertTrue(str.contains("3"));
        assertTrue(str.contains("2"));
    }

    @Test
    @DisplayName("Test Entry interface")
    public void testEntryInterface() {
        multiset.add("test", 5);

        Multiset.Entry<String> entry = multiset.entrySet().iterator().next();

        assertEquals("test", entry.element());
        assertEquals(5, entry.count());

        assertEquals("test x 5", entry.toString());

        Multiset.Entry<String> sameEntry = new Multiset.Entry<String>() {
            public String element() {
                return "test";
            }

            public int count() {
                return 5;
            }

            public boolean equals(Object o) {
                if (o instanceof Multiset.Entry<?> e) {
                    return count() == e.count() && Objects.equals(element(), e.element());
                }
                return false;
            }

            public int hashCode() {
                return Objects.hashCode(element()) ^ count();
            }

            public String toString() {
                return element() + " x " + count();
            }
        };

        assertEquals(entry, sameEntry);
        assertEquals(entry.hashCode(), sameEntry.hashCode());
    }

    @Test
    @DisplayName("Test Entry with count 1")
    public void testEntryCountOne() {
        multiset.add("single");

        Multiset.Entry<String> entry = multiset.entrySet().iterator().next();
        assertEquals("single", entry.toString());
    }

    @Test
    @DisplayName("Test operations with null elements when supported")
    public void testNullElements() {
        multiset.add(null, 2);
        assertEquals(2, multiset.getCount(null));
        assertTrue(multiset.contains(null));

        multiset.remove(null);
        assertEquals(1, multiset.getCount(null));
    }

    @Test
    @DisplayName("Test integer overflow protection")
    public void testIntegerOverflowProtection() {
        intMultiset.add(1, Integer.MAX_VALUE);

        assertThrows(IllegalArgumentException.class, () -> intMultiset.add(1, 1));

        assertThrows(IllegalArgumentException.class, () -> intMultiset.addAndGetCount(1, 1));
    }

    @Test
    @DisplayName("Test long overflow in sumOfOccurrences")
    public void testLongOverflowInSum() {
        intMultiset.add(1, Integer.MAX_VALUE);
        intMultiset.add(2, Integer.MAX_VALUE);

        assertThrows(ArithmeticException.class, () -> intMultiset.size());
    }

    @Test
    @DisplayName("Test operations on empty multiset")
    public void testEmptyOperations() {
        assertEquals(0, multiset.size());
        assertEquals(0, multiset.countOfDistinctElements());
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.sumOfOccurrences());
        assertTrue(multiset.averageOfOccurrences().isEmpty());
        assertTrue(multiset.minOccurrences().isEmpty());
        assertTrue(multiset.maxOccurrences().isEmpty());
        assertFalse(multiset.iterator().hasNext());
        assertTrue(multiset.elementSet().isEmpty());
        assertTrue(multiset.entrySet().isEmpty());
    }

    @Test
    @DisplayName("Test NoSuchElementException in iterator")
    public void testIteratorNoSuchElement() {
        ObjIterator<String> iter = multiset.iterator();
        assertThrows(NoSuchElementException.class, iter::next);
    }
}
