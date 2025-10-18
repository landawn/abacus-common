package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class Multiset2025Test extends TestBase {

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
        Multiset<String> ms = new Multiset<>(100);
        assertTrue(ms.isEmpty());
        assertEquals(0, ms.size());
    }

    @Test
    public void testConstructorWithInitialCapacity_Negative() {
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
    }

    @Test
    public void testConstructorWithCollection_Empty() {
        Multiset<String> ms = new Multiset<>(new ArrayList<>());
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithCollection_Null() {
        Multiset<String> ms = new Multiset<>((Collection<String>) null);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithMapClass() {
        Multiset<String> ms = new Multiset<>(LinkedHashMap.class);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithMapSupplier() {
        Multiset<String> ms = new Multiset<>(() -> new TreeMap<>());
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testOf() {
        Multiset<String> ms = Multiset.of("a", "b", "a", "c");
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
        assertEquals(1, ms.getCount("c"));
    }

    @Test
    public void testOf_Empty() {
        Multiset<String> ms = Multiset.of();
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testOf_Null() {
        Multiset<String> ms = Multiset.of((String[]) null);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testCreate() {
        List<String> list = Arrays.asList("x", "y", "x", "z");
        Multiset<String> ms = Multiset.create(list);
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("x"));
    }

    @Test
    public void testCreateFromIterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "a").iterator();
        Multiset<String> ms = Multiset.create(iter);
        assertEquals(3, ms.size());
        assertEquals(2, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
    }

    @Test
    public void testCreateFromIterator_Null() {
        Multiset<String> ms = Multiset.create((Iterator<String>) null);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testCreateFromIterator_MaxValue() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            list.add("a");
            if (i > 100)
                break;
        }
        Iterator<String> iter = list.iterator();
        Multiset<String> ms = Multiset.create(iter);
        assertEquals(list.size(), ms.getCount("a"));
    }

    @Test
    public void testOccurrencesOf() {
        multiset.add("apple", 3);
        assertEquals(3, multiset.occurrencesOf("apple"));
        assertEquals(0, multiset.occurrencesOf("banana"));
    }

    @Test
    public void testMinOccurrences_Empty() {
        assertFalse(multiset.minOccurrences().isPresent());
    }

    @Test
    public void testMinOccurrences_SingleElement() {
        multiset.add("a", 5);
        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(5, min.get().left().intValue());
        assertEquals("a", min.get().right());
    }

    @Test
    public void testMinOccurrences_MultipleElements() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertEquals("b", min.get().right());
    }

    @Test
    public void testMaxOccurrences_Empty() {
        assertFalse(multiset.maxOccurrences().isPresent());
    }

    @Test
    public void testMaxOccurrences_SingleElement() {
        multiset.add("a", 5);
        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(5, max.get().left().intValue());
        assertEquals("a", max.get().right());
    }

    @Test
    public void testMaxOccurrences_MultipleElements() {
        multiset.add("a", 3);
        multiset.add("b", 5);
        multiset.add("c", 2);
        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(5, max.get().left().intValue());
        assertEquals("b", max.get().right());
    }

    @Test
    public void testAllMinOccurrences_Empty() {
        assertFalse(multiset.allMinOccurrences().isPresent());
    }

    @Test
    public void testAllMinOccurrences_SingleMin() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.add("c", 4);
        Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(2, allMin.get().left().intValue());
        assertEquals(1, allMin.get().right().size());
        assertTrue(allMin.get().right().contains("a"));
    }

    @Test
    public void testAllMinOccurrences_MultipleMin() {
        multiset.add("a", 1);
        multiset.add("b", 1);
        multiset.add("c", 3);
        Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(1, allMin.get().left().intValue());
        assertEquals(2, allMin.get().right().size());
        assertTrue(allMin.get().right().contains("a"));
        assertTrue(allMin.get().right().contains("b"));
    }

    @Test
    public void testAllMaxOccurrences_Empty() {
        assertFalse(multiset.allMaxOccurrences().isPresent());
    }

    @Test
    public void testAllMaxOccurrences_SingleMax() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        multiset.add("c", 2);
        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(5, allMax.get().left().intValue());
        assertEquals(1, allMax.get().right().size());
        assertTrue(allMax.get().right().contains("a"));
    }

    @Test
    public void testAllMaxOccurrences_MultipleMax() {
        multiset.add("a", 5);
        multiset.add("b", 5);
        multiset.add("c", 2);
        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(5, allMax.get().left().intValue());
        assertEquals(2, allMax.get().right().size());
        assertTrue(allMax.get().right().contains("a"));
        assertTrue(allMax.get().right().contains("b"));
    }

    @Test
    public void testSumOfOccurrences_Empty() {
        assertEquals(0, multiset.sumOfOccurrences());
    }

    @Test
    public void testSumOfOccurrences() {
        multiset.add("a", 3);
        multiset.add("b", 5);
        multiset.add("c", 2);
        assertEquals(10, multiset.sumOfOccurrences());
    }

    @Test
    public void testAverageOfOccurrences_Empty() {
        assertFalse(multiset.averageOfOccurrences().isPresent());
    }

    @Test
    public void testAverageOfOccurrences() {
        multiset.add("a", 2);
        multiset.add("b", 4);
        multiset.add("c", 6);
        OptionalDouble avg = multiset.averageOfOccurrences();
        assertTrue(avg.isPresent());
        assertEquals(4.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testGetCount_NonExistent() {
        assertEquals(0, multiset.getCount("nonexistent"));
    }

    @Test
    public void testGetCount_Existing() {
        multiset.add("apple", 5);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testGetCount_Null() {
        multiset.add(null, 3);
        assertEquals(3, multiset.getCount(null));
    }

    @Test
    public void testSetCount_NewElement() {
        int oldCount = multiset.setCount("apple", 5);
        assertEquals(0, oldCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_ExistingElement() {
        multiset.add("apple", 3);
        int oldCount = multiset.setCount("apple", 7);
        assertEquals(3, oldCount);
        assertEquals(7, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_ToZero() {
        multiset.add("apple", 3);
        int oldCount = multiset.setCount("apple", 0);
        assertEquals(3, oldCount);
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testSetCount_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("apple", -1));
    }

    @Test
    public void testSetCount_Conditional_Success() {
        multiset.add("apple", 2);
        boolean updated = multiset.setCount("apple", 2, 5);
        assertTrue(updated);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_Conditional_Failure() {
        multiset.add("apple", 2);
        boolean updated = multiset.setCount("apple", 3, 5);
        assertFalse(updated);
        assertEquals(2, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_Conditional_ToZero() {
        multiset.add("apple", 2);
        boolean updated = multiset.setCount("apple", 2, 0);
        assertTrue(updated);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testSetCount_Conditional_FromZero() {
        boolean updated = multiset.setCount("apple", 0, 5);
        assertTrue(updated);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_Conditional_NegativeOld() {
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("apple", -1, 5));
    }

    @Test
    public void testSetCount_Conditional_NegativeNew() {
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("apple", 2, -1));
    }

    @Test
    public void testAdd_SingleOccurrence() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_MultipleOccurrences() {
        int oldCount = multiset.add("apple", 3);
        assertEquals(0, oldCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_IncrementExisting() {
        multiset.add("apple", 2);
        int oldCount = multiset.add("apple", 3);
        assertEquals(2, oldCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_ZeroOccurrences() {
        int oldCount = multiset.add("apple", 0);
        assertEquals(0, oldCount);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_NegativeOccurrences() {
        assertThrows(IllegalArgumentException.class, () -> multiset.add("apple", -1));
    }

    @Test
    public void testAdd_Overflow() {
        multiset.add("apple", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("apple", 1));
    }

    @Test
    public void testAdd_Null() {
        assertTrue(multiset.add(null));
        assertEquals(1, multiset.getCount(null));
    }

    @Test
    public void testAddAndGetCount_NewElement() {
        int newCount = multiset.addAndGetCount("apple", 3);
        assertEquals(3, newCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testAddAndGetCount_ExistingElement() {
        multiset.add("apple", 2);
        int newCount = multiset.addAndGetCount("apple", 3);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testAddAndGetCount_Zero() {
        multiset.add("apple", 5);
        int newCount = multiset.addAndGetCount("apple", 0);
        assertEquals(5, newCount);
    }

    @Test
    public void testAddAndGetCount_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("apple", -1));
    }

    @Test
    public void testAddAndGetCount_Overflow() {
        multiset.add("apple", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("apple", 1));
    }

    @Test
    public void testAddAll() {
        List<String> list = Arrays.asList("a", "b", "a");
        assertTrue(multiset.addAll(list));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testAddAll_Empty() {
        assertFalse(multiset.addAll(new ArrayList<>()));
    }

    @Test
    public void testAddAll_Null() {
        assertFalse(multiset.addAll(null));
    }

    @Test
    public void testAddAll_WithOccurrences() {
        List<String> list = Arrays.asList("a", "b");
        assertTrue(multiset.addAll(list, 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));
    }

    @Test
    public void testAddAll_WithOccurrences_Zero() {
        List<String> list = Arrays.asList("a", "b");
        assertFalse(multiset.addAll(list, 0));
        assertEquals(0, multiset.getCount("a"));
    }

    @Test
    public void testAddAll_WithOccurrences_Negative() {
        List<String> list = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(list, -1));
    }

    @Test
    public void testRemove_SingleOccurrence() {
        multiset.add("apple", 3);
        assertTrue(multiset.remove("apple"));
        assertEquals(2, multiset.getCount("apple"));
    }

    @Test
    public void testRemove_LastOccurrence() {
        multiset.add("apple");
        assertTrue(multiset.remove("apple"));
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testRemove_NonExistent() {
        assertFalse(multiset.remove("nonexistent"));
    }

    @Test
    public void testRemove_MultipleOccurrences() {
        multiset.add("apple", 5);
        int oldCount = multiset.remove("apple", 2);
        assertEquals(5, oldCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testRemove_MoreThanExists() {
        multiset.add("apple", 3);
        int oldCount = multiset.remove("apple", 5);
        assertEquals(3, oldCount);
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testRemove_Zero() {
        multiset.add("apple", 3);
        int oldCount = multiset.remove("apple", 0);
        assertEquals(3, oldCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testRemove_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.remove("apple", -1));
    }

    @Test
    public void testRemoveAndGetCount() {
        multiset.add("apple", 5);
        int newCount = multiset.removeAndGetCount("apple", 2);
        assertEquals(3, newCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testRemoveAndGetCount_AllOccurrences() {
        multiset.add("apple", 3);
        int newCount = multiset.removeAndGetCount("apple", 5);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testRemoveAndGetCount_NonExistent() {
        int newCount = multiset.removeAndGetCount("apple", 2);
        assertEquals(0, newCount);
    }

    @Test
    public void testRemoveAndGetCount_Zero() {
        multiset.add("apple", 5);
        int newCount = multiset.removeAndGetCount("apple", 0);
        assertEquals(5, newCount);
    }

    @Test
    public void testRemoveAndGetCount_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAndGetCount("apple", -1));
    }

    @Test
    public void testRemoveAll() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertTrue(multiset.removeAll(Arrays.asList("a", "b")));
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertTrue(multiset.contains("c"));
    }

    @Test
    public void testRemoveAll_Empty() {
        multiset.add("a", 3);
        assertFalse(multiset.removeAll(new ArrayList<>()));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_Null() {
        multiset.add("a", 3);
        assertFalse(multiset.removeAll(null));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_WithOccurrences() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        assertTrue(multiset.removeAll(Arrays.asList("a", "b"), 2));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAll_WithOccurrences_Zero() {
        multiset.add("a", 3);
        assertFalse(multiset.removeAll(Arrays.asList("a"), 0));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_WithOccurrences_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAll(Arrays.asList("a"), -1));
    }

    @Test
    public void testRemoveAllOccurrences_Element() {
        multiset.add("apple", 5);
        int removed = multiset.removeAllOccurrences("apple");
        assertEquals(5, removed);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testRemoveAllOccurrences_NonExistent() {
        int removed = multiset.removeAllOccurrences("nonexistent");
        assertEquals(0, removed);
    }

    @Test
    public void testRemoveAllOccurrences_Collection() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertTrue(multiset.removeAllOccurrences(Arrays.asList("a", "c")));
        assertFalse(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate() {
        multiset.add("apple", 3);
        multiset.add("banana", 2);
        multiset.add("cherry", 1);
        assertTrue(multiset.removeAllOccurrencesIf(s -> s.startsWith("a")));
        assertFalse(multiset.contains("apple"));
        assertTrue(multiset.contains("banana"));
        assertTrue(multiset.contains("cherry"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate_NoMatch() {
        multiset.add("apple", 3);
        assertFalse(multiset.removeAllOccurrencesIf(s -> s.startsWith("z")));
        assertTrue(multiset.contains("apple"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((java.util.function.Predicate<String>) null));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate() {
        multiset.add("a", 1);
        multiset.add("b", 3);
        multiset.add("c", 5);
        assertTrue(multiset.removeAllOccurrencesIf((element, count) -> count >= 3));
        assertTrue(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_NoMatch() {
        multiset.add("a", 1);
        assertFalse(multiset.removeAllOccurrencesIf((element, count) -> count > 10));
        assertTrue(multiset.contains("a"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((com.landawn.abacus.util.function.ObjIntPredicate<String>) null));
    }

    @Test
    public void testUpdateAllOccurrences() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.updateAllOccurrences((element, count) -> count * 2);
        assertEquals(4, multiset.getCount("a"));
        assertEquals(6, multiset.getCount("b"));
    }

    @Test
    public void testUpdateAllOccurrences_ToZero() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        multiset.updateAllOccurrences((element, count) -> 0);
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
    }

    @Test
    public void testUpdateAllOccurrences_ToNull() {
        multiset.add("a", 5);
        multiset.updateAllOccurrences((element, count) -> null);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testUpdateAllOccurrences_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.updateAllOccurrences(null));
    }

    @Test
    public void testComputeIfAbsent_Absent() {
        int count = multiset.computeIfAbsent("apple", e -> 5);
        assertEquals(5, count);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_Present() {
        multiset.add("apple", 3);
        int count = multiset.computeIfAbsent("apple", e -> 10);
        assertEquals(3, count);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_ZeroValue() {
        int count = multiset.computeIfAbsent("apple", e -> 0);
        assertEquals(0, count);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_NegativeValue() {
        int count = multiset.computeIfAbsent("apple", e -> -1);
        assertEquals(-1, count);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfAbsent("apple", null));
    }

    @Test
    public void testComputeIfPresent_Present() {
        multiset.add("apple", 3);
        int newCount = multiset.computeIfPresent("apple", (e, count) -> count * 2);
        assertEquals(6, newCount);
        assertEquals(6, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfPresent_Absent() {
        int newCount = multiset.computeIfPresent("apple", (e, count) -> 10);
        assertEquals(0, newCount);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfPresent_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.computeIfPresent("apple", (e, count) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testComputeIfPresent_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfPresent("apple", null));
    }

    @Test
    public void testCompute_Absent() {
        int newCount = multiset.compute("apple", (e, count) -> count + 5);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testCompute_Present() {
        multiset.add("apple", 3);
        int newCount = multiset.compute("apple", (e, count) -> count + 2);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testCompute_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.compute("apple", (e, count) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testCompute_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.compute("apple", null));
    }

    @Test
    public void testMerge_Absent() {
        int newCount = multiset.merge("apple", 5, (oldCount, value) -> oldCount + value);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testMerge_Present() {
        multiset.add("apple", 3);
        int newCount = multiset.merge("apple", 2, (oldCount, value) -> oldCount + value);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testMerge_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.merge("apple", 0, (oldCount, value) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testMerge_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.merge("apple", 5, null));
    }

    @Test
    public void testRetainAll() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertTrue(multiset.retainAll(Arrays.asList("a", "c")));
        assertTrue(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertTrue(multiset.contains("c"));
    }

    @Test
    public void testRetainAll_Empty() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertTrue(multiset.retainAll(new ArrayList<>()));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testRetainAll_Null() {
        multiset.add("a", 3);
        assertTrue(multiset.retainAll(null));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testRetainAll_NoChange() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertFalse(multiset.retainAll(Arrays.asList("a", "b", "c")));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
    }

    @Test
    public void testContains_Present() {
        multiset.add("apple", 3);
        assertTrue(multiset.contains("apple"));
    }

    @Test
    public void testContains_Absent() {
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testContains_Null() {
        multiset.add(null, 1);
        assertTrue(multiset.contains(null));
    }

    @Test
    public void testContainsAll_True() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.add("c", 1);
        assertTrue(multiset.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testContainsAll_False() {
        multiset.add("a", 2);
        assertFalse(multiset.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testContainsAll_Empty() {
        assertTrue(multiset.containsAll(new ArrayList<>()));
    }

    @Test
    public void testContainsAll_Null() {
        assertTrue(multiset.containsAll(null));
    }

    @Test
    public void testElementSet() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        Set<String> elements = multiset.elementSet();
        assertEquals(3, elements.size());
        assertTrue(elements.contains("a"));
        assertTrue(elements.contains("b"));
        assertTrue(elements.contains("c"));
    }

    @Test
    public void testElementSet_Empty() {
        Set<String> elements = multiset.elementSet();
        assertTrue(elements.isEmpty());
    }

    @Test
    public void testEntrySet() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        Set<Multiset.Entry<String>> entries = multiset.entrySet();
        assertEquals(2, entries.size());

        boolean foundA = false;
        boolean foundB = false;
        for (Multiset.Entry<String> entry : entries) {
            if (entry.element().equals("a") && entry.count() == 3) {
                foundA = true;
            }
            if (entry.element().equals("b") && entry.count() == 2) {
                foundB = true;
            }
        }
        assertTrue(foundA);
        assertTrue(foundB);
    }

    @Test
    public void testEntrySet_Contains() {
        multiset.add("a", 3);
        Set<Multiset.Entry<String>> entries = multiset.entrySet();

        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("a", 3);
        assertTrue(entries.contains(entry));

        Multiset.Entry<String> wrongCount = new Multiset.ImmutableEntry<>("a", 2);
        assertFalse(entries.contains(wrongCount));
    }

    @Test
    public void testEntrySet_Size() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertEquals(2, multiset.entrySet().size());
    }

    @Test
    public void testIterator() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        List<String> collected = new ArrayList<>();
        for (String s : multiset) {
            collected.add(s);
        }

        assertEquals(3, collected.size());
        assertTrue(collected.contains("a"));
        assertTrue(collected.contains("b"));
    }

    @Test
    public void testIterator_Empty() {
        Iterator<String> iter = multiset.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_NoSuchElement() {
        Iterator<String> iter = multiset.iterator();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_MultipleOccurrences() {
        multiset.add("a", 3);
        Iterator<String> iter = multiset.iterator();

        int count = 0;
        while (iter.hasNext()) {
            assertEquals("a", iter.next());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testSize_Empty() {
        assertEquals(0, multiset.size());
    }

    @Test
    public void testSize() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertEquals(5, multiset.size());
    }

    @Test
    public void testCountOfDistinctElements_Empty() {
        assertEquals(0, multiset.countOfDistinctElements());
    }

    @Test
    public void testCountOfDistinctElements() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 5);
        assertEquals(3, multiset.countOfDistinctElements());
    }

    @Test
    public void testIsEmpty_True() {
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testIsEmpty_False() {
        multiset.add("a");
        assertFalse(multiset.isEmpty());
    }

    @Test
    public void testClear() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.clear();
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
    }

    @Test
    public void testClear_AlreadyEmpty() {
        multiset.clear();
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testToArray() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        Object[] array = multiset.toArray();
        assertEquals(3, array.length);
    }

    @Test
    public void testToArray_Empty() {
        Object[] array = multiset.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_Typed() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        String[] array = multiset.toArray(new String[0]);
        assertEquals(3, array.length);
    }

    @Test
    public void testToArray_Typed_ExactSize() {
        multiset.add("a", 2);
        String[] array = new String[2];
        String[] result = multiset.toArray(array);
        assertEquals(array, result);
        assertEquals(2, result.length);
    }

    @Test
    public void testToArray_Typed_Null() {
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
    public void testToMap_Empty() {
        Map<String, Integer> map = multiset.toMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMap_WithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        Map<String, Integer> map = multiset.toMap(HashMap::new);
        assertEquals(2, map.size());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testToMapSortedByOccurrences() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Map<String, Integer> map = multiset.toMapSortedByOccurrences();

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals("b", keys.get(0));
        assertEquals("c", keys.get(1));
        assertEquals("a", keys.get(2));
    }

    @Test
    public void testToMapSortedByOccurrences_WithComparator() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Map<String, Integer> map = multiset.toMapSortedByOccurrences((i1, i2) -> i2.compareTo(i1));

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals("a", keys.get(0));
        assertEquals("c", keys.get(1));
        assertEquals("b", keys.get(2));
    }

    @Test
    public void testToMapSortedByKey() {
        multiset.add("c", 1);
        multiset.add("a", 1);
        multiset.add("b", 1);
        Map<String, Integer> map = multiset.toMapSortedByKey(String::compareTo);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals("a", keys.get(0));
        assertEquals("b", keys.get(1));
        assertEquals("c", keys.get(2));
    }

    @Test
    public void testToImmutableMap() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        ImmutableMap<String, Integer> map = multiset.toImmutableMap();
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
    }

    @Test
    public void testToImmutableMap_WithSupplier() {
        multiset.add("a", 3);
        ImmutableMap<String, Integer> map = multiset.toImmutableMap(HashMap::new);
        assertEquals(1, map.size());
    }

    @Test
    public void testForEach() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        List<String> collected = new ArrayList<>();
        multiset.forEach((java.util.function.Consumer<String>) collected::add);

        assertEquals(3, collected.size());
    }

    @Test
    public void testForEach_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.forEach((java.util.function.Consumer<String>) null));
    }

    @Test
    public void testForEach_ObjIntConsumer() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Map<String, Integer> collected = new HashMap<>();
        multiset.forEach(collected::put);

        assertEquals(2, collected.size());
        assertEquals(Integer.valueOf(3), collected.get("a"));
        assertEquals(Integer.valueOf(2), collected.get("b"));
    }

    @Test
    public void testForEach_ObjIntConsumer_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.forEach((com.landawn.abacus.util.function.ObjIntConsumer<String>) null));
    }

    @Test
    public void testElements() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        List<String> list = multiset.elements().toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testElements_Empty() {
        Stream<String> stream = multiset.elements();
        assertEquals(0, stream.count());
    }

    @Test
    public void testEntries() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        List<Multiset.Entry<String>> entries = multiset.entries().toList();
        assertEquals(2, entries.size());
    }

    @Test
    public void testEntries_Empty() {
        Stream<Multiset.Entry<String>> stream = multiset.entries();
        assertEquals(0, stream.count());
    }

    @Test
    public void testApply() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        int result = multiset.apply(ms -> ms.size());
        assertEquals(5, result);
    }

    @Test
    public void testApply_WithException() {
        assertThrows(RuntimeException.class, () -> {
            multiset.apply(ms -> {
                throw new RuntimeException("test");
            });
        });
    }

    @Test
    public void testApplyIfNotEmpty_NotEmpty() {
        multiset.add("a", 3);
        Optional<Integer> result = multiset.applyIfNotEmpty(ms -> ms.size());
        assertTrue(result.isPresent());
        assertEquals(3, result.get().intValue());
    }

    @Test
    public void testApplyIfNotEmpty_Empty() {
        Optional<Integer> result = multiset.applyIfNotEmpty(ms -> ms.size());
        assertFalse(result.isPresent());
    }

    @Test
    public void testAccept() {
        multiset.add("a", 3);

        List<Integer> sizes = new ArrayList<>();
        multiset.accept(ms -> sizes.add(ms.size()));

        assertEquals(1, sizes.size());
        assertEquals(3, sizes.get(0).intValue());
    }

    @Test
    public void testAccept_WithException() {
        assertThrows(RuntimeException.class, () -> {
            multiset.accept(ms -> {
                throw new RuntimeException("test");
            });
        });
    }

    @Test
    public void testAcceptIfNotEmpty_NotEmpty() {
        multiset.add("a", 3);

        List<Integer> sizes = new ArrayList<>();
        multiset.acceptIfNotEmpty(ms -> sizes.add(ms.size()));

        assertEquals(1, sizes.size());
        assertEquals(3, sizes.get(0).intValue());
    }

    @Test
    public void testAcceptIfNotEmpty_Empty() {
        List<Integer> sizes = new ArrayList<>();
        List<String> orElseRun = new ArrayList<>();

        multiset.acceptIfNotEmpty(ms -> sizes.add(ms.size())).orElse(() -> orElseRun.add("executed"));

        assertEquals(0, sizes.size());
        assertEquals(1, orElseRun.size());
    }

    @Test
    public void testHashCode() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Multiset<String> other = new Multiset<>();
        other.add("a", 3);
        other.add("b", 2);

        assertEquals(multiset.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals_SameInstance() {
        assertTrue(multiset.equals(multiset));
    }

    @Test
    public void testEquals_Equal() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Multiset<String> other = new Multiset<>();
        other.add("a", 3);
        other.add("b", 2);

        assertTrue(multiset.equals(other));
    }

    @Test
    public void testEquals_DifferentCounts() {
        multiset.add("a", 3);

        Multiset<String> other = new Multiset<>();
        other.add("a", 2);

        assertFalse(multiset.equals(other));
    }

    @Test
    public void testEquals_DifferentElements() {
        multiset.add("a", 3);

        Multiset<String> other = new Multiset<>();
        other.add("b", 3);

        assertFalse(multiset.equals(other));
    }

    @Test
    public void testEquals_NotMultiset() {
        assertFalse(multiset.equals("not a multiset"));
    }

    @Test
    public void testEquals_Null() {
        assertFalse(multiset.equals(null));
    }

    @Test
    public void testToString() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        String str = multiset.toString();
        assertNotNull(str);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
    }

    @Test
    public void testToString_Empty() {
        String str = multiset.toString();
        assertNotNull(str);
    }

    @Test
    public void testEntry_Element() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals("test", entry.element());
    }

    @Test
    public void testEntry_Count() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals(5, entry.count());
    }

    @Test
    public void testEntry_Equals_True() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>("test", 5);
        assertTrue(entry1.equals(entry2));
    }

    @Test
    public void testEntry_Equals_DifferentCount() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>("test", 3);
        assertFalse(entry1.equals(entry2));
    }

    @Test
    public void testEntry_Equals_DifferentElement() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>("other", 5);
        assertFalse(entry1.equals(entry2));
    }

    @Test
    public void testEntry_Equals_NotEntry() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertFalse(entry.equals("not an entry"));
    }

    @Test
    public void testEntry_Equals_Null() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertFalse(entry.equals(null));
    }

    @Test
    public void testEntry_Equals_NullElement() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>(null, 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>(null, 5);
        assertTrue(entry1.equals(entry2));
    }

    @Test
    public void testEntry_HashCode() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        int expectedHashCode = "test".hashCode() ^ 5;
        assertEquals(expectedHashCode, entry.hashCode());
    }

    @Test
    public void testEntry_HashCode_NullElement() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>(null, 5);
        int expectedHashCode = 0 ^ 5;
        assertEquals(expectedHashCode, entry.hashCode());
    }

    @Test
    public void testEntry_ToString_CountOne() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 1);
        assertEquals("test", entry.toString());
    }

    @Test
    public void testEntry_ToString_CountMultiple() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals("test x 5", entry.toString());
    }

    @Test
    public void testMultipleOperations_Complex() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        multiset.add("c", 7);

        assertEquals(15, multiset.size());
        assertEquals(3, multiset.countOfDistinctElements());

        multiset.remove("b", 2);
        assertEquals(13, multiset.size());

        multiset.setCount("a", 10);
        assertEquals(18, multiset.size());

        multiset.removeAllOccurrences("c");
        assertEquals(11, multiset.size());
        assertEquals(2, multiset.countOfDistinctElements());
    }

    @Test
    public void testLinkedHashMapOrdering() {
        Multiset<String> ordered = new Multiset<>(LinkedHashMap.class);
        ordered.add("z", 1);
        ordered.add("a", 1);
        ordered.add("m", 1);

        List<String> keys = new ArrayList<>(ordered.elementSet());
        assertEquals("z", keys.get(0));
        assertEquals("a", keys.get(1));
        assertEquals("m", keys.get(2));
    }

    @Test
    public void testTreeMapOrdering() {
        Multiset<String> sorted = new Multiset<>(TreeMap.class);
        sorted.add("z", 1);
        sorted.add("a", 1);
        sorted.add("m", 1);

        List<String> keys = new ArrayList<>(sorted.elementSet());
        assertEquals("a", keys.get(0));
        assertEquals("m", keys.get(1));
        assertEquals("z", keys.get(2));
    }

    @Test
    public void testNullElements() {
        Multiset<String> testMultiset = new Multiset<>();
        testMultiset.add(null, 5);
        assertEquals(5, testMultiset.getCount(null));
        assertTrue(testMultiset.contains(null));

        testMultiset.remove(null, 2);
        assertEquals(3, testMultiset.getCount(null));
        assertTrue(testMultiset.contains(null));

        testMultiset.clear();
        assertEquals(0, testMultiset.getCount(null));
        assertFalse(testMultiset.contains(null));

        testMultiset.add(null, 7);
        int removed = testMultiset.removeAllOccurrences((String) null);
        assertEquals(7, removed);
    }

    @Test
    public void testEmptyAfterOperations() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        multiset.clear();
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());

        assertFalse(multiset.minOccurrences().isPresent());
        assertFalse(multiset.maxOccurrences().isPresent());
    }

    @Test
    public void testLargeCount() {
        int largeCount = 1000000;
        multiset.add("test", largeCount);
        assertEquals(largeCount, multiset.getCount("test"));
        assertEquals(largeCount, multiset.size());
    }

    @Test
    public void testCount_Deprecated() {
        multiset.add("apple", 5);
        assertEquals(5, multiset.count("apple"));
        assertEquals(0, multiset.count("nonexistent"));
    }

    @Test
    public void testAddAndGetCount_ZeroOnNewElement() {
        int newCount = multiset.addAndGetCount("apple", 0);
        assertEquals(0, newCount);
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }
}
