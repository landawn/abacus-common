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
import static org.junit.jupiter.api.Assertions.fail;

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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.ObjIntFunction;
import com.landawn.abacus.util.function.ObjIntPredicate;
import com.landawn.abacus.util.stream.Stream;

@Tag("old-test")
public class MultisetTest extends AbstractTest {

    private Multiset<String> multiset;
    private Multiset<Integer> intMultiset;

    @BeforeEach
    public void setUp() {
        multiset = new Multiset<>();
        intMultiset = new Multiset<>();
    }

    @Test
    public void test_01() {
        Multiset<String> set = CommonUtil.toMultiset("a", "b", "c", "C");
        N.println(set);
        set.add("a");

        assertEquals(2, set.getCount("a"));
        N.println(set);
        set.remove("a");

        assertEquals(1, set.getCount("a"));
        set.add("b", 100);
        set.remove("b", 90);

        assertEquals(11, set.getCount("b"));
        N.println(set);
        set.remove("b", 11);
        assertEquals(0, set.getCount("b"));

        set.add("C");
        N.println(set);
        N.println(set.toMapSortedByOccurrences());
        N.println(set.toMapSortedBy((a, b) -> a.getKey().compareTo(b.getKey())));
        N.println(set.maxOccurrences());
        N.println(set.minOccurrences());
        N.println(set.sumOfOccurrences());
        N.println(set.averageOfOccurrences());
    }

    @Test
    public void test_02() {
        Multiset<String> set = CommonUtil.toMultiset("a", "b", "c");
        N.println(set);

        set.setCount("a", 0);
        set.setCount("a", 3);
        assertEquals(3, set.getCount("a"));

        set.entrySet().forEach(Fn.println());

        set.entrySet().forEach(Fn.println());

        try {
            set.setCount("a", -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        assertEquals(3, set.maxOccurrences().get().left().intValue());

        try {
            set.add("a", -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {
            set.add("a", Integer.MAX_VALUE);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        assertEquals(3, set.getCount("a"));

        assertTrue(set.contains("a"));
        assertFalse(set.contains("e"));

        assertTrue(set.containsAll(CommonUtil.toList("a", "b")));
        assertFalse(set.contains(CommonUtil.toList("b", "e")));

        assertTrue(set.containsAll(CommonUtil.toList("a")));
        assertFalse(set.contains(CommonUtil.toList("e")));

        try {
            set.remove("a", -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        set.remove("a", 2);
        assertEquals(1, set.getCount("a"));

        set.remove("a", 2);
        assertEquals(0, set.getCount("a"));

        set.add("a", 3);
        assertEquals(3, set.getCount("a"));

        CommonUtil.toList("a").forEach(e -> set.remove(e));
        assertEquals(2, set.getCount("a"));
        assertEquals(1, set.getCount("b"));

        CommonUtil.toList("a", "b", "e").forEach(e -> set.remove(e, 2));
        assertEquals(0, set.getCount("a"));
        assertEquals(0, set.getCount("b"));

        set.add("a", 3);
        set.add("b", 3);

        set.retainAll(CommonUtil.toList("a", "b", "e"));

        assertEquals(3, set.getCount("a"));
        assertEquals(3, set.getCount("b"));

        Multiset<String> set2 = CommonUtil.toMultiset();
        set2.setCount("a", 3);
        set2.setCount("b", 3);

        assertTrue(CommonUtil.toSet(set).contains(set2));

        set.clear();
        assertTrue(set.isEmpty());
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
        assertEquals(3, multiset.getCount("apple"));
        assertEquals(0, multiset.getCount("banana"));
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
    public void testGet_Deprecated() {
        multiset.add("apple", 5);
        assertEquals(5, multiset.get("apple"));
        assertEquals(0, multiset.get("nonexistent"));
        // Verify get() returns the same result as getCount()
        assertEquals(multiset.getCount("apple"), multiset.get("apple"));
    }

    @Test
    public void testAddAndGetCount_ZeroOnNewElement() {
        int newCount = multiset.addAndGetCount("apple", 0);
        assertEquals(0, newCount);
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testConstructorWithEmptyCollection() {
        Collection<String> initialElements = Collections.emptyList();
        Multiset<String> multiset = new Multiset<>(initialElements);
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
    }

    @Test
    public void testConstructorWithNullCollection() {
        Multiset<String> multiset = new Multiset<>((Collection<String>) null);
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
    }

    @Test
    public void testConstructorWithSetCollection() {
        Set<String> initialElements = CommonUtil.toSet("a", "b", "c");
        Multiset<String> multiset = new Multiset<>(initialElements);
        assertEquals(3, multiset.size());
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testConstructorWithValueMapType() {
        Multiset<String> multiset = new Multiset<>(LinkedHashMap.class);
        multiset.add("c");
        multiset.add("a");
        multiset.add("b");
        List<String> elements = new ArrayList<>(multiset.elementSet());
        assertEquals(Arrays.asList("c", "a", "b"), elements);
        assertTrue(multiset.toString().startsWith("{c=1, a=1, b=1}"));
    }

    @Test
    public void testConstructorWithValueMapType_null() {
        assertThrows(NullPointerException.class, () -> new Multiset<>((Class<? extends Map>) null));
    }

    @Test
    public void testConstructorWithMapSupplier_null() {
        assertThrows(NullPointerException.class, () -> new Multiset<>((Supplier<Map<String, ?>>) null));
    }

    @Test
    public void testOfVarArgs() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testOfVarArgsEmpty() {
        Multiset<String> multiset = Multiset.of();
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testOfVarArgsNullArray() {
        Multiset<String> multiset = Multiset.of((String[]) null);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testCreateWithCollection() {
        Collection<String> initialElements = Arrays.asList("x", "y", "x");
        Multiset<String> multiset = Multiset.create(initialElements);
        assertEquals(3, multiset.size());
        assertEquals(2, multiset.getCount("x"));
        assertEquals(1, multiset.getCount("y"));
    }

    @Test
    public void testCreateWithNullCollection() {
        Multiset<String> multiset = Multiset.create((Collection<String>) null);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testCreateWithIterator() {
        Iterator<String> iterator = Arrays.asList("m", "n", "m", "o", "m").iterator();
        Multiset<String> multiset = Multiset.create(iterator);
        assertEquals(5, multiset.size());
        assertEquals(3, multiset.getCount("m"));
        assertEquals(1, multiset.getCount("n"));
        assertEquals(1, multiset.getCount("o"));
    }

    @Test
    public void testCreateWithEmptyIterator() {
        Iterator<String> iterator = Collections.emptyIterator();
        Multiset<String> multiset = Multiset.create(iterator);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testCreateWithNullIterator() {
        Multiset<String> multiset = Multiset.create((Iterator<String>) null);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testOccurrencesOfAndGetCount() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertEquals(2, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(0, multiset.getCount(null));
    }

    @Test
    public void testMinOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.minOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c"));
        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertEquals("b", min.get().right());

        multiset.add("d", 1);
        min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertTrue(CommonUtil.toSet("b", "d").contains(min.get().right()));

        multiset.clear();
        multiset.add("x", 5);
        min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(5, min.get().left().intValue());
        assertEquals("x", min.get().right());
    }

    @Test
    public void testMaxOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.maxOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c"));
        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(3, max.get().left().intValue());
        assertEquals("c", max.get().right());

        multiset.add("d", 3);
        max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(3, max.get().left().intValue());
        assertTrue(CommonUtil.toSet("c", "d").contains(max.get().right()));

        multiset.clear();
        multiset.add("y", 2);
        max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(2, max.get().left().intValue());
        assertEquals("y", max.get().right());
    }

    @Test
    public void testAllMinOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.allMinOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c", "d"));
        Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(1, allMin.get().left().intValue());
        assertEquals(CommonUtil.toSet("b", "d"), new HashSet<>(allMin.get().right()));

        multiset.clear();
        multiset.add("x", 5);
        multiset.add("y", 5);
        allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(5, allMin.get().left().intValue());
        assertEquals(CommonUtil.toSet("x", "y"), new HashSet<>(allMin.get().right()));
    }

    @Test
    public void testAllMaxOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.allMaxOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "a", "b", "c", "c", "d", "d", "d"));
        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(3, allMax.get().left().intValue());
        assertEquals(CommonUtil.toSet("a", "d"), new HashSet<>(allMax.get().right()));

        multiset.clear();
        multiset.add("x", 1);
        multiset.add("y", 1);
        allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(1, allMax.get().left().intValue());
        assertEquals(CommonUtil.toSet("x", "y"), new HashSet<>(allMax.get().right()));
    }

    @Test
    public void testDeprecatedCount() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertEquals(2, multiset.count("a"));
    }

    @Test
    public void testEntrySet_iterator_next_without_hasNext() {
        Multiset<String> multiset = Multiset.of("a");
        Iterator<Multiset.Entry<String>> it = multiset.entrySet().iterator();
        assertNotNull(it.next());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testSize_overflow() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", Integer.MAX_VALUE);
        multiset.add("b", 1);

        assertThrows(ArithmeticException.class, () -> multiset.size());
    }

    @Test
    public void testIsEmpty() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.isEmpty());
        multiset.add("a");
        assertFalse(multiset.isEmpty());
        multiset.remove("a");
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testContains() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertTrue(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
        assertFalse(multiset.contains(null));
    }

    @Test
    public void testContainsAll() {
        Multiset<String> multiset = Multiset.of("a", "b", "c", "a");
        assertTrue(multiset.containsAll(Arrays.asList("a", "b")));
        assertTrue(multiset.containsAll(Arrays.asList("c")));
        assertTrue(multiset.containsAll(Collections.emptyList()));
        assertFalse(multiset.containsAll(Arrays.asList("a", "d")));
        assertFalse(multiset.containsAll(Arrays.asList("d", "e")));
    }

    @Test
    public void testAddSingleElement() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.add("a"));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.add("a"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(2, multiset.size());

        assertTrue(multiset.add("b"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(3, multiset.size());
    }

    @Test
    public void testAddSingleElement_nullNotPermittedByDefaultMap() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.add(null));
        assertEquals(1, multiset.getCount(null));
        assertTrue(multiset.add(null));
        assertEquals(2, multiset.getCount(null));
    }

    @Test
    public void testAddSingleElement_maxOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.setCount("a", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a"));
    }

    @Test
    public void testAddElementWithOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.add("a", 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.size());

        assertEquals(3, multiset.add("a", 2));
        assertEquals(5, multiset.getCount("a"));
        assertEquals(5, multiset.size());

        assertEquals(0, multiset.add("b", 0));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(5, multiset.size());

        assertEquals(0, multiset.add("c", 1));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(6, multiset.size());
    }

    @Test
    public void testAddElementWithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a", -1));
    }

    @Test
    public void testAddElementWithOccurrences_overflow() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", Integer.MAX_VALUE - 1);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a", 2));

        assertEquals(Integer.MAX_VALUE - 1, multiset.getCount("a"));

        Multiset<String> multiset2 = new Multiset<>();

        multiset2.add("b", 10);
        multiset2.add("b", Integer.MAX_VALUE - 10);
        assertEquals(Integer.MAX_VALUE, multiset2.getCount("b"));

    }

    @Test
    public void testAddAndGetCount() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(3, multiset.addAndGetCount("a", 3));
        assertEquals(3, multiset.getCount("a"));

        assertEquals(5, multiset.addAndGetCount("a", 2));
        assertEquals(5, multiset.getCount("a"));

        assertEquals(0, multiset.addAndGetCount("b", 0));
        assertEquals(0, multiset.getCount("b"));

        assertEquals(5, multiset.getCount("a"));
    }

    @Test
    public void testAddAndGetCount_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("a", -1));
    }

    @Test
    public void testAddAndGetCount_overflow() {
        Multiset<String> multiset = new Multiset<>();
        multiset.setCount("a", Integer.MAX_VALUE - 1);
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("a", 2));
        assertEquals(Integer.MAX_VALUE - 1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollection() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a");
        Collection<String> toAdd = Arrays.asList("a", "b", "c", "b");

        assertTrue(multiset.addAll(toAdd));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(1 + 4, multiset.size());
    }

    @Test
    public void testAddAllCollection_empty() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(Collections.emptyList()));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollection_null() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(null));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 1);
        Collection<String> toAdd = Arrays.asList("a", "b");

        assertTrue(multiset.addAll(toAdd, 2));
        assertEquals(1 + 2, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1 + 2 + 2, multiset.size());
    }

    @Test
    public void testAddAllCollectionWithOccurrences_zero() {
        Multiset<String> multiset = Multiset.of("a", "b");
        Collection<String> toAdd = Arrays.asList("a", "c");
        assertFalse(multiset.addAll(toAdd, 0));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(0, multiset.getCount("c"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        Collection<String> toAdd = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(toAdd, -1));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_emptyCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(Collections.emptyList(), 2));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_nullCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(null, 2));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testSetCount() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.setCount("a", 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.size());

        assertEquals(3, multiset.setCount("a", 5));
        assertEquals(5, multiset.getCount("a"));
        assertEquals(5, multiset.size());

        assertEquals(5, multiset.setCount("a", 0));
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty());

        multiset.add("b", 2);
        assertEquals(0, multiset.setCount("c", 1));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(2 + 1, multiset.size());
    }

    @Test
    public void testSetCount_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("a", -1));
    }

    @Test
    public void testSetCount_elementNotPresent_toZero() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.setCount("a", 0));
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testSetCountWithOldOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.setCount("a", 0, 3));
        assertEquals(3, multiset.getCount("a"));

        assertFalse(multiset.setCount("a", 2, 5));
        assertEquals(3, multiset.getCount("a"));

        assertTrue(multiset.setCount("a", 3, 1));
        assertEquals(1, multiset.getCount("a"));

        assertTrue(multiset.setCount("a", 1, 0));
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty());

        assertTrue(multiset.setCount("b", 0, 0));
        assertEquals(0, multiset.getCount("b"));
        assertTrue(multiset.isEmpty());

        multiset.add("c", 5);
        assertTrue(multiset.setCount("c", 5, 5));
        assertEquals(5, multiset.getCount("c"));
    }

    @Test
    public void testSetCountWithOldOccurrences_negativeOld() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("a", -1, 3));
    }

    @Test
    public void testSetCountWithOldOccurrences_negativeNew() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("a", 0, -1));
    }

    @Test
    public void testRemoveSingleElement() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        assertTrue(multiset.remove("a"));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(2, multiset.size());

        assertTrue(multiset.remove("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertFalse(multiset.remove("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.remove("b"));
        assertEquals(0, multiset.getCount("b"));
        assertTrue(multiset.isEmpty());

        assertFalse(multiset.remove("c"));
    }

    @Test
    public void testRemoveElementWithOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "a", "b", "b");
        assertEquals(4, multiset.remove("a", 2));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(4, multiset.size());

        assertEquals(2, multiset.remove("a", 3));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(2, multiset.size());

        assertEquals(0, multiset.remove("c", 1));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(2, multiset.size());

        assertEquals(2, multiset.remove("b", 0));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testRemoveElementWithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.remove("a", -1));
    }

    @Test
    public void testRemoveAndGetCount_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAndGetCount("a", -1));
    }

    @Test
    public void testDeprecatedRemoveAll_Collection() {
        Multiset<String> multiset = Multiset.of("a", "a", "b", "c", "c");
        Collection<String> toRemove = Arrays.asList("a", "c", "d");

        assertTrue(multiset.removeAll(toRemove));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("d"));
        assertEquals(1, multiset.size());

        assertFalse(multiset.removeAll(Arrays.asList("x", "y")));
        assertTrue(multiset.removeAll(Arrays.asList("a", "b", "c")));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testDeprecatedRemoveAll_Collection_empty() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertFalse(multiset.removeAll(Collections.emptyList()));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testDeprecatedRemoveAll_Collection_null() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertFalse(multiset.removeAll(null));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "b", "c");
        Collection<String> toRemove = Arrays.asList("a", "b", "d");

        assertTrue(multiset.removeAll(toRemove, 2));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("d"));
        assertEquals(3, multiset.size());

        assertTrue(multiset.removeAll(Arrays.asList("a", "b"), 2));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(1, multiset.size());
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences_zero() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        assertFalse(multiset.removeAll(Arrays.asList("a", "c"), 0));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        Collection<String> toRemove = Arrays.asList("a");
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAll(toRemove, -1));
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences_emptyCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.removeAll(Collections.emptyList(), 1));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences_nullCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.removeAll(null, 1));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAllOccurrences_Object() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b");
        assertEquals(3, multiset.removeAllOccurrences("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.size());

        assertEquals(0, multiset.removeAllOccurrences("c"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate_null() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((Predicate<String>) null));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate_noMatch() {
        Multiset<String> multiset = Multiset.of("apple", "banana");
        assertFalse(multiset.removeAllOccurrencesIf(s -> s.startsWith("z")));
        assertEquals(1, multiset.getCount("apple"));
        assertEquals(1, multiset.getCount("banana"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_null() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((ObjIntPredicate<String>) null));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_noMatch() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertFalse(multiset.removeAllOccurrencesIf((s, count) -> count > 1));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRetainAll_nullCollection() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertTrue(multiset.retainAll(null));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testRetainAll_emptyMultiset() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.retainAll(Arrays.asList("a", "b")));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testRetainAll_noChange() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertFalse(multiset.retainAll(Arrays.asList("a", "b", "c")));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testUpdateAllOccurrences_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.updateAllOccurrences(null));
    }

    @Test
    public void testUpdateAllOccurrences_returnNull() {
        Multiset<String> multiset = Multiset.of("a", "b");
        multiset.updateAllOccurrences((el, count) -> el.equals("a") ? null : count);
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testComputeIfAbsent() {
        Multiset<String> multiset = new Multiset<>();
        ToIntFunction<String> computer = s -> s.length();

        assertEquals(1, multiset.computeIfAbsent("a", computer));
        assertEquals(1, multiset.getCount("a"));

        assertEquals(1, multiset.computeIfAbsent("a", computer));
        assertEquals(1, multiset.getCount("a"));

        assertEquals(3, multiset.computeIfAbsent("xyz", computer));
        assertEquals(3, multiset.getCount("xyz"));

        assertEquals(0, multiset.computeIfAbsent("zero", s -> 0));
        assertEquals(0, multiset.getCount("zero"));

    }

    @Test
    public void testComputeIfAbsent_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfAbsent("a", null));
    }

    @Test
    public void testComputeIfPresent() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        ObjIntFunction<String, Integer> remapper = (s, count) -> s.equals("a") ? count + 1 : 0;

        assertEquals(3, multiset.computeIfPresent("a", remapper));
        assertEquals(3, multiset.getCount("a"));

        assertEquals(0, multiset.computeIfPresent("b", remapper));
        assertEquals(0, multiset.getCount("b"));

        assertEquals(0, multiset.computeIfPresent("c", remapper));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testComputeIfPresent_nullFunction() {
        Multiset<String> multiset = Multiset.of("a");
        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfPresent("a", null));
    }

    @Test
    public void testCompute() {
        Multiset<String> multiset = new Multiset<>();
        ObjIntFunction<String, Integer> computer = (s, oldCount) -> {
            if (s.equals("add"))
                return oldCount + 2;
            if (s.equals("set"))
                return 5;
            if (s.equals("remove"))
                return 0;
            if (s.equals("no_change_if_present"))
                return oldCount > 0 ? oldCount : 0;
            if (s.equals("add_if_absent"))
                return oldCount == 0 ? 1 : oldCount;
            return 0;
        };

        assertEquals(2, multiset.compute("add", computer));
        assertEquals(2, multiset.getCount("add"));

        assertEquals(4, multiset.compute("add", computer));
        assertEquals(4, multiset.getCount("add"));

        assertEquals(5, multiset.compute("set", computer));
        assertEquals(5, multiset.getCount("set"));

        assertEquals(5, multiset.compute("set", computer));
        assertEquals(5, multiset.getCount("set"));

        multiset.setCount("remove_target", 3);
        assertEquals(0, multiset.compute("remove_target", computer));
        assertEquals(0, multiset.getCount("remove_target"));

        assertEquals(0, multiset.compute("remove_absent", computer));
        assertEquals(0, multiset.getCount("remove_absent"));
    }

    @Test
    public void testCompute_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.compute("a", null));
    }

    @Test
    public void testMerge() {
        Multiset<String> multiset = new Multiset<>();
        IntBiFunction<Integer> merger = (oldCount, value) -> oldCount + value;

        assertEquals(3, multiset.merge("a", 3, merger));
        assertEquals(3, multiset.getCount("a"));

        assertEquals(5, multiset.merge("a", 2, merger));
        assertEquals(5, multiset.getCount("a"));

        assertEquals(1, multiset.merge("b", 1, merger));
        assertEquals(1, multiset.getCount("b"));

        IntBiFunction<Integer> subtractMerger = (oldCount, value) -> oldCount - value;
        multiset.setCount("c", 5);
        assertEquals(0, multiset.merge("c", 5, subtractMerger));
        assertEquals(0, multiset.getCount("c"));

        assertEquals(0, multiset.merge("d", 0, merger));
        assertEquals(0, multiset.getCount("d"));

        assertEquals(7, multiset.merge("e", 7, (ov, v) -> ov + v + 100));
        assertEquals(7, multiset.getCount("e"));

        assertEquals(7 + 8 + 100, multiset.merge("e", 8, (ov, v) -> ov + v + 100));
        assertEquals(115, multiset.getCount("e"));
    }

    @Test
    public void testMerge_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.merge("a", 1, null));
    }

    @Test
    public void testToArray_empty() {
        Multiset<String> multiset = new Multiset<>();
        Object[] array = multiset.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_Generic() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");

        String[] arraySmall = new String[2];
        String[] resultSmall = multiset.toArray(arraySmall);
        assertNotSame(arraySmall, resultSmall);
        assertEquals(4, resultSmall.length);
        List<String> listSmall = Arrays.asList(resultSmall);
        assertEquals(2, Collections.frequency(listSmall, "a"));
        assertEquals(1, Collections.frequency(listSmall, "b"));
        assertEquals(1, Collections.frequency(listSmall, "c"));

        String[] arrayExact = new String[4];
        String[] resultExact = multiset.toArray(arrayExact);
        assertSame(arrayExact, resultExact);
        List<String> listExact = Arrays.asList(resultExact);
        assertEquals(2, Collections.frequency(listExact, "a"));
        assertEquals(1, Collections.frequency(listExact, "b"));
        assertEquals(1, Collections.frequency(listExact, "c"));

        String[] arrayLarge = new String[6];
        String[] resultLarge = multiset.toArray(arrayLarge);
        assertSame(arrayLarge, resultLarge);
        List<String> listLarge = Arrays.asList(resultLarge);
        assertEquals(2, Collections.frequency(listLarge.subList(0, 4), "a"));
        assertEquals(1, Collections.frequency(listLarge.subList(0, 4), "b"));
        assertEquals(1, Collections.frequency(listLarge.subList(0, 4), "c"));
        if (arrayLarge.length > multiset.size()) {
            Arrays.fill(arrayLarge, null);
            multiset.toArray(arrayLarge);
            assertNull(arrayLarge[4]);
            assertNull(arrayLarge[5]);
        }
    }

    @Test
    public void testToArray_Generic_nullArray() {
        Multiset<String> multiset = Multiset.of("a");
        assertThrows(IllegalArgumentException.class, () -> multiset.toArray((String[]) null));
    }

    @Test
    public void testToArray_Generic_emptyMultiset() {
        Multiset<String> multiset = new Multiset<>();
        String[] a = new String[0];
        String[] result = multiset.toArray(a);
        assertSame(a, result);
        assertEquals(0, result.length);

        String[] b = new String[5];
        Arrays.fill(b, "test");
        String[] resultB = multiset.toArray(b);
        assertSame(b, resultB);
        assertEquals("test", b[0]);
    }

    @Test
    public void testToMap_empty() {
        Multiset<String> multiset = new Multiset<>();
        Map<String, Integer> map = multiset.toMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMapSortedByOccurrences_empty() {
        Multiset<String> multiset = new Multiset<>();
        Map<String, Integer> map = multiset.toMapSortedByOccurrences();
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testToMapSortedByOccurrences_Comparator() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("c", 3);
        multiset.add("a", 1);
        multiset.add("b", 2);

        Map<String, Integer> sortedMapDesc = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
        Iterator<Map.Entry<String, Integer>> itDesc = sortedMapDesc.entrySet().iterator();
        assertEquals("c", itDesc.next().getKey());
        assertEquals("b", itDesc.next().getKey());
        assertEquals("a", itDesc.next().getKey());
        assertFalse(itDesc.hasNext());
    }

    @Test
    public void testIterator_empty() {
        Multiset<String> multiset = new Multiset<>();
        Iterator<String> it = multiset.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testIterator_singleElementMultipleTimes() {
        Multiset<String> multiset = Multiset.of("a", "a", "a");
        List<String> elements = new ArrayList<>();
        multiset.iterator().forEachRemaining(elements::add);
        assertEquals(Arrays.asList("a", "a", "a"), elements);
    }

    @Test
    public void testIterator_multipleCallsToHasNext() {
        Multiset<String> multiset = Multiset.of("a", "b");
        Iterator<String> it = multiset.iterator();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }

    @Test
    public void testForEach_Consumer() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        List<String> seen = new ArrayList<>();
        Consumer<String> consumer = seen::add;

        multiset.forEach(consumer);

        assertEquals(4, seen.size());
        assertEquals(2, Collections.frequency(seen, "a"));
        assertEquals(1, Collections.frequency(seen, "b"));
        assertEquals(1, Collections.frequency(seen, "c"));
    }

    @Test
    public void testForEach_Consumer_nullAction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.forEach((Consumer<String>) null));
    }

    @Test
    public void testForEach_ObjIntConsumer_nullAction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.forEach((ObjIntConsumer<String>) null));
    }

    @Test
    public void testElementsStream() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        List<String> streamed = multiset.elements().toList();

        assertEquals(4, streamed.size());
        assertEquals(2, Collections.frequency(streamed, "a"));
        assertEquals(1, Collections.frequency(streamed, "b"));
        assertEquals(1, Collections.frequency(streamed, "c"));
    }

    @Test
    public void testElementsStream_empty() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.elements().count());
    }

    @Test
    public void testEntriesStream() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Map<String, Integer> streamedEntries = multiset.entries().toMap(Multiset.Entry::element, Multiset.Entry::count);

        assertEquals(3, streamedEntries.size());
        assertEquals(2, streamedEntries.get("a").intValue());
        assertEquals(1, streamedEntries.get("b").intValue());
        assertEquals(1, streamedEntries.get("c").intValue());
    }

    @Test
    public void testEntriesStream_empty() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.entries().count());
    }

    @Test
    public void testEquals() {
        Multiset<String> m1 = Multiset.of("a", "b", "a");
        Multiset<String> m2 = Multiset.of("b", "a", "a");
        Multiset<String> m3 = new Multiset<>();
        m3.add("a", 2);
        m3.add("b", 1);

        assertTrue(m1.equals(m1));
        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
        assertTrue(m1.equals(m3));
        assertTrue(m3.equals(m1));

        Multiset<String> m4 = Multiset.of("a", "b");
        assertFalse(m1.equals(m4));
        assertFalse(m4.equals(m1));

        Multiset<String> m5 = Multiset.of("a", "c", "a");
        assertFalse(m1.equals(m5));

        assertFalse(m1.equals(null));
        assertFalse(m1.equals(new Object()));

        Multiset<String> empty1 = new Multiset<>();
        Multiset<String> empty2 = new Multiset<>();
        assertTrue(empty1.equals(empty2));
        assertFalse(empty1.equals(m1));
    }

    @Test
    public void testMultisetIterator_remove() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        Iterator<String> it = multiset.iterator();

        assertTrue(it.hasNext());
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove, "Multiset.iterator().remove() should throw UnsupportedOperationException");
    }

    @Test
    public void testElementSetIterator_remove_supported() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Set<String> elementSet = multiset.elementSet();
        Iterator<String> it = elementSet.iterator();

        assertTrue(it.hasNext());
        String firstElement = it.next();
        multiset.removeAllOccurrences(firstElement);

        assertEquals(0, multiset.getCount(firstElement), "Element should be removed from multiset after elementSet().iterator().remove()");
        assertFalse(multiset.contains(firstElement), "Multiset should not contain element after elementSet().iterator().remove()");
        assertFalse(elementSet.contains(firstElement), "ElementSet should not contain element after its iterator.remove()");

        it = elementSet.iterator();

        assertTrue(it.hasNext());
        String secondElement = it.next();
        it.remove();
        assertEquals(0, multiset.getCount(secondElement));

        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(multiset.isEmpty(), "Multiset should be empty after removing all from elementSet iterator");
        assertTrue(elementSet.isEmpty(), "ElementSet should be empty");
    }

    @Test
    public void testEntrySetIterator_remove() {
        Multiset<String> multiset = Multiset.of("a", "b");
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();
        Iterator<Multiset.Entry<String>> it = entrySet.iterator();

        assertTrue(it.hasNext());
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove,
                "entrySet().iterator().remove() should throw UnsupportedOperationException if not implemented.");
    }

    @Test
    public void testEntrySet_removeObject() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();

        Multiset.Entry<String> entryOfA = null;
        for (Multiset.Entry<String> entry : entrySet) {
            if (entry.element().equals("a")) {
                entryOfA = entry;
                break;
            }
        }
        assertNotNull(entryOfA);
        assertEquals("a", entryOfA.element());
        assertEquals(2, entryOfA.count());

    }

    @Test
    public void testElementSet_removeObject_supported() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Set<String> elementSet = multiset.elementSet();

        assertTrue(elementSet.contains("a"));
        assertTrue(multiset.contains("a"));

        assertTrue(elementSet.remove("a"), "elementSet.remove('a') should return true");

        assertFalse(elementSet.contains("a"), "Element 'a' should be removed from elementSet");
        assertFalse(multiset.contains("a"), "Element 'a' should be removed from multiset");
        assertEquals(0, multiset.getCount("a"), "Count of 'a' should be 0 in multiset");

        assertEquals(2, multiset.size(), "Multiset size should be updated");
        assertEquals(2, elementSet.size(), "elementSet size should be updated");

        assertFalse(elementSet.remove("x"), "Removing non-existent element from elementSet should return false");
    }

    @Test
    public void testConstructorWithMapType() {
        Multiset<String> ms = new Multiset<>(LinkedHashMap.class);
        assertTrue(ms.isEmpty());

        assertThrows(NullPointerException.class, () -> new Multiset<>((Class<? extends Map>) null));
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
    public void testToMapWithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        TreeMap<String, Integer> map = multiset.toMap(size -> new TreeMap<>());
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
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
    public void testToImmutableMapWithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        ImmutableMap<String, Integer> immutableMap = multiset.toImmutableMap(HashMap::new);
        assertEquals(2, immutableMap.size());
        assertEquals(Integer.valueOf(3), immutableMap.get("a"));
        assertEquals(Integer.valueOf(2), immutableMap.get("b"));
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
    public void testApplyIfNotEmpty() {
        Optional<Integer> emptyResult = multiset.applyIfNotEmpty(ms -> ms.size());
        assertFalse(emptyResult.isPresent());

        multiset.add("test", 1);
        Optional<Integer> result = multiset.applyIfNotEmpty(ms -> ms.size());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().intValue());
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

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        Multiset<String> ms = new Multiset<>(100);
        assertNotNull(ms);
        assertTrue(ms.isEmpty());
    }

    @Test
    @DisplayName("Test deprecated count() method")
    public void testCountDeprecated() {
        multiset.add("item", 7);
        assertEquals(7, multiset.count("item"));
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
    @DisplayName("Test addAll() empty collection")
    public void testAddAllEmptyCollection() {
        assertFalse(multiset.addAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
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
    @DisplayName("Test removeAllOccurrences() single element")
    public void testRemoveAllOccurrencesSingle() {
        multiset.add("apple", 5);

        assertEquals(5, multiset.removeAllOccurrences("apple"));
        assertFalse(multiset.contains("apple"));

        assertEquals(0, multiset.removeAllOccurrences("banana"));
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
    @DisplayName("Test retainAll() with empty collection")
    public void testRetainAllEmpty() {
        multiset.add("a", 3);

        assertTrue(multiset.retainAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
    }

    @Test
    @DisplayName("Test entrySet() contains")
    public void testEntrySetContains() {
        multiset.add("a", 3);

        Set<Multiset.Entry<String>> entries = multiset.entrySet();

        Multiset.Entry<String> testEntry = new Multiset.Entry<>() {
            @Override
            public String element() {
                return "a";
            }

            @Override
            public int count() {
                return 3;
            }

            @Override
            public boolean equals(Object o) {
                if (o instanceof Multiset.Entry<?> e) {
                    return count() == e.count() && Objects.equals(element(), e.element());
                }
                return false;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(element()) ^ count();
            }

            @Override
            public String toString() {
                return element() + " x " + count();
            }
        };

        assertTrue(entries.contains(testEntry));
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
    @DisplayName("Test Entry interface")
    public void testEntryInterface() {
        multiset.add("test", 5);

        Multiset.Entry<String> entry = multiset.entrySet().iterator().next();

        assertEquals("test", entry.element());
        assertEquals(5, entry.count());

        assertEquals("test x 5", entry.toString());

        Multiset.Entry<String> sameEntry = new Multiset.Entry<>() {
            @Override
            public String element() {
                return "test";
            }

            @Override
            public int count() {
                return 5;
            }

            @Override
            public boolean equals(Object o) {
                if (o instanceof Multiset.Entry<?> e) {
                    return count() == e.count() && Objects.equals(element(), e.element());
                }
                return false;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(element()) ^ count();
            }

            @Override
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

    // Additional tests for missing coverage

    @Test
    public void testToMapSortedByKey_ReverseOrder() {
        multiset.add("c", 1);
        multiset.add("a", 3);
        multiset.add("b", 2);
        Map<String, Integer> map = multiset.toMapSortedByKey(Comparator.reverseOrder());
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals("c", keys.get(0));
        assertEquals("b", keys.get(1));
        assertEquals("a", keys.get(2));
    }

    @Test
    public void testToMapSortedByKey_Empty() {
        Map<String, Integer> map = multiset.toMapSortedByKey(String::compareTo);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMapSortedByOccurrences_AscendingOrder() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Map<String, Integer> map = multiset.toMapSortedByOccurrences(Comparator.naturalOrder());
        List<Integer> values = new ArrayList<>(map.values());
        assertEquals(Integer.valueOf(1), values.get(0));
        assertEquals(Integer.valueOf(2), values.get(1));
        assertEquals(Integer.valueOf(3), values.get(2));
    }

    @Test
    public void testToMapSortedByOccurrences_DescendingOrder() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Map<String, Integer> map = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
        List<Integer> values = new ArrayList<>(map.values());
        assertEquals(Integer.valueOf(3), values.get(0));
        assertEquals(Integer.valueOf(2), values.get(1));
        assertEquals(Integer.valueOf(1), values.get(2));
    }

    @Test
    public void testToImmutableMapIsUnmodifiable() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        ImmutableMap<String, Integer> map = multiset.toImmutableMap();
        assertThrows(UnsupportedOperationException.class, () -> map.put("c", 1));
    }

    @Test
    public void testToImmutableMap_Empty() {
        ImmutableMap<String, Integer> map = multiset.toImmutableMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testSumOfOccurrences_Large() {
        multiset.add("a", Integer.MAX_VALUE / 2);
        multiset.add("b", Integer.MAX_VALUE / 2);
        long sum = multiset.sumOfOccurrences();
        assertEquals((long) (Integer.MAX_VALUE / 2) * 2, sum);
    }

    @Test
    public void testRemoveAllOccurrences_EmptyCollection() {
        multiset.add("a", 3);
        assertFalse(multiset.removeAllOccurrences(Collections.emptyList()));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testComputeIfAbsent_ReturnZero() {
        int result = multiset.computeIfAbsent("a", e -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testComputeIfPresent_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.computeIfPresent("a", (e, count) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testCompute_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.compute("a", (e, count) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testMerge_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.merge("a", 1, (oldVal, newVal) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testElementsStream_CollectToList() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        List<String> list = multiset.elements().toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testEntriesStream_CollectToList() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        List<Multiset.Entry<String>> entries = multiset.entries().toList();
        assertEquals(2, entries.size());
    }

    @Test
    public void testCountOfDistinctElements_AfterRemoval() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertEquals(3, multiset.countOfDistinctElements());

        multiset.removeAllOccurrences("b");
        assertEquals(2, multiset.countOfDistinctElements());
    }

    @Test
    public void testSetCount_OverwriteExistingWithHigherValue() {
        multiset.add("a", 2);
        int old = multiset.setCount("a", 10);
        assertEquals(2, old);
        assertEquals(10, multiset.getCount("a"));
    }

    @Test
    public void testAddAndGetCount_MultipleAdds() {
        assertEquals(3, multiset.addAndGetCount("a", 3));
        assertEquals(5, multiset.addAndGetCount("a", 2));
        assertEquals(5, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAndGetCount_PartialRemoval() {
        multiset.add("a", 5);
        assertEquals(3, multiset.removeAndGetCount("a", 2));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testToMap_WithLinkedHashMapSupplier() {
        multiset.add("b", 2);
        multiset.add("a", 3);
        Map<String, Integer> map = multiset.toMap(LinkedHashMap::new);
        assertEquals(2, map.size());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testEntrySetIterator() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();
        Iterator<Multiset.Entry<String>> iter = entrySet.iterator();
        int count = 0;
        while (iter.hasNext()) {
            Multiset.Entry<String> entry = iter.next();
            assertNotNull(entry.element());
            assertTrue(entry.count() > 0);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testUpdateAllOccurrences_DoubleValues() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.updateAllOccurrences((e, count) -> count * 2);
        assertEquals(4, multiset.getCount("a"));
        assertEquals(6, multiset.getCount("b"));
    }

    @Test
    public void testConstructorWithIterable_NonCollection() {
        Iterable<String> iterable = () -> Arrays.asList("a", "b", "a", "c").iterator();

        Multiset<String> result = new Multiset<>(iterable);

        assertEquals(2, result.getCount("a"));
        assertEquals(1, result.getCount("b"));
        assertEquals(1, result.getCount("c"));
    }

    @Test
    public void testAllMinAndAllMaxOccurrences_WithOrderedTies() {
        Multiset<String> ordered = new Multiset<>(LinkedHashMap.class);
        ordered.add("alpha", 1);
        ordered.add("beta", 3);
        ordered.add("gamma", 3);
        ordered.add("delta", 1);

        Optional<Pair<Integer, List<String>>> min = ordered.allMinOccurrences();
        Optional<Pair<Integer, List<String>>> max = ordered.allMaxOccurrences();

        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertEquals(Arrays.asList("alpha", "delta"), min.get().right());
        assertTrue(max.isPresent());
        assertEquals(3, max.get().left().intValue());
        assertEquals(Arrays.asList("beta", "gamma"), max.get().right());
    }

    @Test
    public void testUpdateAllOccurrences_MixedRetainAndRemove() {
        multiset.add("a", 1);
        multiset.add("b", 2);
        multiset.add("c", 3);

        multiset.updateAllOccurrences((element, count) -> count == 1 ? 0 : count + 1);

        assertEquals(0, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));
        assertEquals(4, multiset.getCount("c"));
    }
}
