package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.stream.Stream;

public class MultisetTest extends MultisetTestSupport {
    @Test
    public void testMultisetIterator_remove() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        Iterator<String> it = multiset.iterator();

        assertTrue(it.hasNext());
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove, "Multiset.iterator().remove() should throw UnsupportedOperationException");
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
    public void testOfVarArgs() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testDeprecatedCount() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertEquals(2, multiset.count("a"));
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
    public void testCreateWithCollection() {
        Collection<String> initialElements = Arrays.asList("x", "y", "x");
        Multiset<String> multiset = Multiset.create(initialElements);
        assertEquals(3, multiset.size());
        assertEquals(2, multiset.getCount("x"));
        assertEquals(1, multiset.getCount("y"));
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
    public void testCreateFromCollection() {
        List<String> list = Arrays.asList("x", "y", "x", "z");
        Multiset<String> ms = Multiset.create(list);
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("x"));
        assertEquals(1, ms.getCount("y"));
        assertEquals(1, ms.getCount("z"));
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
            if (i > 100) {
                break;
            }
        }
        Iterator<String> iter = list.iterator();
        Multiset<String> ms = Multiset.create(iter);
        assertEquals(list.size(), ms.getCount("a"));
    }

    @Test
    public void testCreateWithNullCollection() {
        Multiset<String> multiset = Multiset.create((Collection<String>) null);
        assertTrue(multiset.isEmpty());
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
    public void testSumOfOccurrences() {
        multiset.add("a", 3);
        multiset.add("b", 5);
        multiset.add("c", 2);
        assertEquals(10, multiset.sumOfOccurrences());
    }

    @Test
    public void testSumOfOccurrences_Empty() {
        assertEquals(0, multiset.sumOfOccurrences());
    }

    @Test
    public void testSumOfOccurrences_Large() {
        multiset.add("a", Integer.MAX_VALUE / 2);
        multiset.add("b", Integer.MAX_VALUE / 2);
        long sum = multiset.sumOfOccurrences();
        assertEquals((long) (Integer.MAX_VALUE / 2) * 2, sum);
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
    public void testAverageOfOccurrences_Empty() {
        assertFalse(multiset.averageOfOccurrences().isPresent());
    }

    @Test
    public void testCount() {
        assertEquals(0, multiset.count("test"));

        multiset.add("test", 5);
        assertEquals(5, multiset.count("test"));
    }

    @Test
    @DisplayName("Test deprecated count() method")
    public void testCountDeprecated() {
        multiset.add("item", 7);
        assertEquals(7, multiset.count("item"));
    }

    @Test
    public void testCount_Deprecated() {
        multiset.add("apple", 5);
        assertEquals(5, multiset.count("apple"));
        assertEquals(0, multiset.count("nonexistent"));
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
    public void testOccurrencesOf() {
        multiset.add("apple", 3);
        assertEquals(3, multiset.getCount("apple"));
        assertEquals(0, multiset.getCount("banana"));
        assertEquals(0, multiset.getCount("nonexistent"));

        multiset.add(null, 3);
        assertEquals(3, multiset.getCount(null));
    }

    @Test
    public void testLargeCount() {
        int largeCount = 1000000;
        multiset.add("test", largeCount);
        assertEquals(largeCount, multiset.getCount("test"));
        assertEquals(largeCount, multiset.size());
    }

    @Test
    public void test_01() {
        Multiset<String> set = CommonUtil.toMultiset("a", "b", "c", "C");
        set.add("a");

        assertEquals(2, set.getCount("a"));
        set.remove("a");

        assertEquals(1, set.getCount("a"));
        set.add("b", 100);
        set.remove("b", 90);

        assertEquals(11, set.getCount("b"));
        set.remove("b", 11);
        assertEquals(0, set.getCount("b"));

        set.add("C");
        assertEquals(2, set.getCount("C"));
        assertEquals(1, set.getCount("a"));
        assertEquals(1, set.getCount("c"));
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
        int removed = testMultiset.removeAllOccurrencesOf((String) null);
        assertEquals(7, removed);
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
    public void testSetCount_Conditional_Success() {
        multiset.add("apple", 2);
        boolean updated = multiset.setCount("apple", 2, 5);
        assertTrue(updated);
        assertEquals(5, multiset.getCount("apple"));
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
    public void testSetCount_OverwriteExistingWithHigherValue() {
        multiset.add("a", 2);
        int old = multiset.setCount("a", 10);
        assertEquals(2, old);
        assertEquals(10, multiset.getCount("a"));
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
    public void test_02() {
        Multiset<String> set = CommonUtil.toMultiset("a", "b", "c");
        set.setCount("a", 0);
        set.setCount("a", 3);
        assertEquals(3, set.getCount("a"));

        assertThrows(IllegalArgumentException.class, () -> set.setCount("a", -1));

        assertEquals(3, set.maxOccurrences().get().left().intValue());

        assertThrows(IllegalArgumentException.class, () -> set.add("a", -1));
        assertThrows(IllegalArgumentException.class, () -> set.add("a", Integer.MAX_VALUE));

        assertEquals(3, set.getCount("a"));

        assertTrue(set.contains("a"));
        assertFalse(set.contains("e"));

        assertTrue(set.containsAll(CommonUtil.toList("a", "b")));
        assertFalse(set.contains(CommonUtil.toList("b", "e")));

        assertTrue(set.containsAll(CommonUtil.toList("a")));
        assertFalse(set.contains(CommonUtil.toList("e")));

        assertThrows(IllegalArgumentException.class, () -> set.remove("a", -1));

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
    public void testSetCount_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("apple", -1));
        assertThrows(IllegalArgumentException.class, () -> new Multiset<String>().setCount("a", -1));
    }

    @Test
    public void testSetCount_Conditional_Failure() {
        multiset.add("apple", 2);
        boolean updated = multiset.setCount("apple", 3, 5);
        assertFalse(updated);
        assertEquals(2, multiset.getCount("apple"));
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
    @DisplayName("Test integer overflow protection")
    public void testIntegerOverflowProtection() {
        intMultiset.add(1, Integer.MAX_VALUE);

        assertThrows(IllegalArgumentException.class, () -> intMultiset.add(1, 1));

        assertThrows(IllegalArgumentException.class, () -> intMultiset.addAndGetCount(1, 1));
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
    public void testUpdateAllOccurrences_DoubleValues() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.updateAllOccurrences((e, count) -> count * 2);
        assertEquals(4, multiset.getCount("a"));
        assertEquals(6, multiset.getCount("b"));
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
    public void testUpdateAllOccurrences_returnNull() {
        Multiset<String> multiset = Multiset.of("a", "b");
        multiset.updateAllOccurrences((el, count) -> el.equals("a") ? null : count);
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testUpdateAllOccurrences_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.updateAllOccurrences(null));
    }

    @Test
    public void testUpdateAllOccurrences_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.updateAllOccurrences(null));
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
    public void testMerge_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.merge("apple", 0, (oldCount, value) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testMerge_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.merge("a", 1, (oldVal, newVal) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testMerge_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.merge("apple", 5, null));
    }

    @Test
    public void testMerge_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.merge("a", 1, null));
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
    public void testRetainAll_NoChange() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertFalse(multiset.retainAll(Arrays.asList("a", "b", "c")));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
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
    public void testRetainAll_Empty() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertTrue(multiset.retainAll(new ArrayList<>()));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testRetainAll_Null() {
        multiset.add("a", 3);
        // Collection.retainAll requires NullPointerException for a null argument; treating it as an empty
        // collection (the old behaviour) silently discarded every element.
        assertThrows(NullPointerException.class, () -> multiset.retainAll(null));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRetainAll_nullCollection() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertThrows(NullPointerException.class, () -> multiset.retainAll(null));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testRetainAll_emptyMultiset() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.retainAll(Arrays.asList("a", "b")));
        assertTrue(multiset.isEmpty());
    }

    @Test
    @DisplayName("Test retainAll() with empty collection")
    public void testRetainAllEmpty() {
        multiset.add("a", 3);

        assertTrue(multiset.retainAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
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
    public void testContains() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertTrue(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
        assertFalse(multiset.contains(null));
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
    public void testContainsAll() {
        Multiset<String> multiset = Multiset.of("a", "b", "c", "a");
        assertTrue(multiset.containsAll(Arrays.asList("a", "b")));
        assertTrue(multiset.containsAll(Arrays.asList("c")));
        assertTrue(multiset.containsAll(Collections.emptyList()));
        assertFalse(multiset.containsAll(Arrays.asList("a", "d")));
        assertFalse(multiset.containsAll(Arrays.asList("d", "e")));
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
    public void testElementSet_Empty() {
        Set<String> elements = multiset.elementSet();
        assertTrue(elements.isEmpty());
    }

    @Test
    public void testElementSetIterator_remove_supported() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Set<String> elementSet = multiset.elementSet();
        Iterator<String> it = elementSet.iterator();

        assertTrue(it.hasNext());
        String firstElement = it.next();
        multiset.removeAllOccurrencesOf(firstElement);

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
    public void testConstructorWithIterable_NonCollection() {
        Iterable<String> iterable = () -> Arrays.asList("a", "b", "a", "c").iterator();

        Multiset<String> result = new Multiset<>(iterable);

        assertEquals(2, result.getCount("a"));
        assertEquals(1, result.getCount("b"));
        assertEquals(1, result.getCount("c"));
    }

    @Test
    public void testIterator_Empty() {
        Iterator<String> iter = multiset.iterator();
        assertFalse(iter.hasNext());
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
    public void testIterator_NoSuchElement() {
        Iterator<String> iter = multiset.iterator();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_empty() {
        Multiset<String> multiset = new Multiset<>();
        Iterator<String> it = multiset.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    @DisplayName("Test NoSuchElementException in iterator")
    public void testIteratorNoSuchElement() {
        ObjIterator<String> iter = multiset.iterator();
        assertThrows(NoSuchElementException.class, iter::next);
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
    public void testSize() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertEquals(5, multiset.size());
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
    public void testSize_Empty() {
        assertEquals(0, multiset.size());
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

        multiset.removeAllOccurrencesOf("c");
        assertEquals(11, multiset.size());
        assertEquals(2, multiset.countOfDistinctElements());
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
    public void testSize_saturatesAtIntegerMaxValue() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", Integer.MAX_VALUE);
        multiset.add("b", 1);

        assertEquals(Integer.MAX_VALUE, multiset.size());
        assertEquals((long) Integer.MAX_VALUE + 1, multiset.sumOfOccurrences());
    }

    @Test
    @DisplayName("Test size saturation with multiple maximum counts")
    public void testSizeSaturationWithMultipleMaximumCounts() {
        intMultiset.add(1, Integer.MAX_VALUE);
        intMultiset.add(2, Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, intMultiset.size());
        assertEquals(2L * Integer.MAX_VALUE, intMultiset.sumOfOccurrences());
    }

    @Test
    public void testCountOfDistinctElements() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 5);
        assertEquals(3, multiset.countOfDistinctElements());
    }

    @Test
    public void testCountOfDistinctElements_AfterRemoval() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertEquals(3, multiset.countOfDistinctElements());

        multiset.removeAllOccurrencesOf("b");
        assertEquals(2, multiset.countOfDistinctElements());
    }

    @Test
    public void testCountOfDistinctElements_Empty() {
        assertEquals(0, multiset.countOfDistinctElements());
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
    public void testIsEmpty_True() {
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testIsEmpty_False() {
        multiset.add("a");
        assertFalse(multiset.isEmpty());
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
    public void testIsEmpty() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.isEmpty());
        multiset.add("a");
        assertFalse(multiset.isEmpty());
        multiset.remove("a");
        assertTrue(multiset.isEmpty());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        Multiset<String> ms = new Multiset<>(100);
        assertNotNull(ms);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithMapType() {
        Multiset<String> ms = new Multiset<>(LinkedHashMap.class);
        assertTrue(ms.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> new Multiset<>((Class<? extends Map>) null));
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
    public void testForEach() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        List<String> collected = new ArrayList<>();
        multiset.forEach((java.util.function.Consumer<String>) collected::add);

        assertEquals(3, collected.size());
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
    public void testForEach_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> multiset.forEach((java.util.function.Consumer<String>) null));
    }

    @Test
    public void testForEach_ObjIntConsumer_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> multiset.forEach((com.landawn.abacus.util.function.ObjIntConsumer<String>) null));
    }

    @Test
    public void testForEach_Consumer_nullAction() {
        Multiset<String> multiset = new Multiset<>();
        // Iterable.forEach specifies NullPointerException.
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> multiset.forEach((Consumer<String>) null));
    }

    @Test
    public void testForEach_ObjIntConsumer_nullAction() {
        Multiset<String> multiset = new Multiset<>();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.forEach((ObjIntConsumer<String>) null));
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
    public void testElementsStream() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        List<String> streamed = multiset.elements().toList();

        assertEquals(4, streamed.size());
        assertEquals(2, Collections.frequency(streamed, "a"));
        assertEquals(1, Collections.frequency(streamed, "b"));
        assertEquals(1, Collections.frequency(streamed, "c"));
    }

    @Test
    public void testElementsStream_CollectToList() {
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
    public void testElementsStream_empty() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.elements().count());
    }

    @Test
    public void testEntries() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        List<Multiset.Entry<String>> entries = multiset.entries().toList();
        assertEquals(2, entries.size());
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
    public void testEntriesStream_CollectToList() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        List<Multiset.Entry<String>> entries = multiset.entries().toList();
        assertEquals(2, entries.size());
    }

    @Test
    public void testEntries_Empty() {
        Stream<Multiset.Entry<String>> stream = multiset.entries();
        assertEquals(0, stream.count());
    }

    @Test
    public void testEntriesStream_empty() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.entries().count());
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
        multiset.add("a", 3);
        multiset.add("b", 2);

        Multiset<String> other = new Multiset<>();
        other.add("a", 3);
        other.add("b", 2);

        assertEquals(multiset.hashCode(), other.hashCode());
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
    public void testEquals_SameInstance() {
        assertTrue(multiset.equals(multiset));
    }

    @Test
    public void testEquals_Null() {
        assertFalse(multiset.equals(null));
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
    public void testConstructorWithInitialCapacity_Negative() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<>(-1));
    }

    @Test
    public void testConstructorWithValueMapType_null() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<>((Class<? extends Map>) null));
    }

    @Test
    public void testConstructorWithMapSupplier_null() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<>((Supplier<Map<String, ?>>) null));
    }

    @Test
    public void testConstructorWithMapSupplierReturningNull() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<String>((Supplier<Map<String, ?>>) () -> null));
    }

    @Test
    public void testConstructorWithMapSupplierReturningNonEmptyMap() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(() -> Map.of("a", 1)));
    }

    @Test
    public void testMerge_rejectsNegativeValue() {
        final Multiset<String> multiset = Multiset.of("a");
        assertThrows(IllegalArgumentException.class, () -> multiset.merge("a", -1, (oldCount, value) -> oldCount + value));
        assertThrows(IllegalArgumentException.class, () -> multiset.merge("missing", -5, (oldCount, value) -> value));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    @DisplayName("addAll rejects a count overflow, and the elements accepted before it stay added")
    public void reviewFixes20260906_addAllRejectsCountOverflow() {
        final Multiset<String> one = new Multiset<>();
        one.add("a", Integer.MAX_VALUE);
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> one.addAll(Arrays.asList("a")));
        assertTrue(e1.getMessage().contains("out of the bound of int"), e1.getMessage());
        assertEquals(Integer.MAX_VALUE, one.getCount("a"));

        final Multiset<String> two = new Multiset<>();
        two.add("a", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> two.addAll(Arrays.asList("a"), 1));
        assertEquals(Integer.MAX_VALUE, two.getCount("a"));

        // add(e, MAX_VALUE) twice throws the same way
        final Multiset<String> twice = new Multiset<>();
        twice.add("a", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> twice.add("a", Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, twice.getCount("a"));

        // addAll is not atomic: "y" is already in when "z" overflows - the javadoc now says so
        final Multiset<String> partial = new Multiset<>();
        partial.add("z", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> partial.addAll(Arrays.asList("y", "z")));
        assertEquals(1, partial.getCount("y"));
        assertEquals(Integer.MAX_VALUE, partial.getCount("z"));

        // Control: an addAll that lands exactly on MAX_VALUE is fine
        final Multiset<String> edge = new Multiset<>();
        edge.add("a", Integer.MAX_VALUE - 1);
        assertTrue(edge.addAll(Arrays.asList("a")));
        assertEquals(Integer.MAX_VALUE, edge.getCount("a"));
    }

    @Test
    @DisplayName("an EMPTY comparator-less TreeMap backing still throws ClassCastException for a non-Comparable key")
    public void reviewFixes20260906_emptySortedBackingWithForeignKey() {
        final Multiset<Integer> emptyTree = new Multiset<>(TreeMap.class);

        // TreeMap.getEntry casts the key to Comparable BEFORE it looks at the root, so "empty never compares"
        // does not save a comparator-less tree.
        assertThrows(ClassCastException.class, () -> emptyTree.contains(new Object()));
        assertThrows(ClassCastException.class, () -> emptyTree.getCount(new Object()));
        assertThrows(ClassCastException.class, () -> emptyTree.remove(new Object(), 1));
        assertThrows(ClassCastException.class, () -> emptyTree.removeAllOccurrencesOf(new Object()));

        // A foreign key that IS Comparable passes the cast, so the empty tree reports "absent".
        assertFalse(emptyTree.contains("not an Integer"));

        // An explicitly comparator-based empty tree never casts at all.
        final Multiset<Integer> emptyComparatorTree = new Multiset<>(() -> new TreeMap<Integer, Object>(Comparator.<Integer> naturalOrder()));
        assertFalse(emptyComparatorTree.contains(new Object()));

        // Control: the non-empty cases and the HashMap backing are unchanged.
        final Multiset<Integer> nonEmptyTree = new Multiset<>(TreeMap.class);
        nonEmptyTree.add(1);
        assertThrows(ClassCastException.class, () -> nonEmptyTree.contains(new Object()));
        assertFalse(new Multiset<Integer>().contains(new Object()));
    }

    @Test
    public void testApplyIfNotEmptyReturnsAnEmptyOptionalForANullResult() {
        // an empty Optional does not imply an empty multiset: the function's own null result produces one too
        final Multiset<String> multiset = Multiset.of("x");
        assertFalse(multiset.isEmpty());
        assertFalse(multiset.applyIfNotEmpty(ms -> (String) null).isPresent());
        assertEquals(Optional.empty(), multiset.applyIfNotEmpty(ms -> (String) null));

        assertEquals(Optional.empty(), new Multiset<String>().applyIfNotEmpty(ms -> "value"));
        assertEquals(Optional.of(1), multiset.applyIfNotEmpty(Multiset::size));
    }
}
