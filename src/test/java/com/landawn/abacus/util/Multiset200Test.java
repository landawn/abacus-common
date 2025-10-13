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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.ObjIntFunction;
import com.landawn.abacus.util.function.ObjIntPredicate;

@Tag("new-test")
public class Multiset200Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        Multiset<String> multiset = new Multiset<>(10);
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
    }

    @Test
    public void testConstructorWithCollection() {
        Collection<String> initialElements = Arrays.asList("a", "b", "a", "c");
        Multiset<String> multiset = new Multiset<>(initialElements);
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("d"));
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
        Set<String> initialElements = N.asSet("a", "b", "c");
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
    public void testConstructorWithMapSupplier() {
        Supplier<Map<String, ?>> mapSupplier = LinkedHashMap::new;
        Multiset<String> multiset = new Multiset<>(mapSupplier);
        multiset.add("z");
        multiset.add("y");
        multiset.add("x");
        List<String> elements = new ArrayList<>(multiset.elementSet());
        assertEquals(Arrays.asList("z", "y", "x"), elements);
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
        assertEquals(2, multiset.occurrencesOf("a"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.occurrencesOf("b"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(0, multiset.occurrencesOf("c"));
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
        assertTrue(N.asSet("b", "d").contains(min.get().right()));

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
        assertTrue(N.asSet("c", "d").contains(max.get().right()));

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
        assertEquals(N.asSet("b", "d"), new HashSet<>(allMin.get().right()));

        multiset.clear();
        multiset.add("x", 5);
        multiset.add("y", 5);
        allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(5, allMin.get().left().intValue());
        assertEquals(N.asSet("x", "y"), new HashSet<>(allMin.get().right()));
    }

    @Test
    public void testAllMaxOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.allMaxOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "a", "b", "c", "c", "d", "d", "d"));
        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(3, allMax.get().left().intValue());
        assertEquals(N.asSet("a", "d"), new HashSet<>(allMax.get().right()));

        multiset.clear();
        multiset.add("x", 1);
        multiset.add("y", 1);
        allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(1, allMax.get().left().intValue());
        assertEquals(N.asSet("x", "y"), new HashSet<>(allMax.get().right()));
    }

    @Test
    public void testSumOfOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0L, multiset.sumOfOccurrences());

        multiset.add("a", 3);
        multiset.add("b", 2);
        assertEquals(5L, multiset.sumOfOccurrences());

        multiset.add("c", Integer.MAX_VALUE);
        long expectedSum = 5L + Integer.MAX_VALUE;
        assertEquals(expectedSum, multiset.sumOfOccurrences());

        Multiset<Integer> m = new Multiset<>();
        m.add(1, Integer.MAX_VALUE);
        m.add(2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, m.sumOfOccurrences());
    }

    @Test
    public void testAverageOfOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.averageOfOccurrences().isPresent());

        multiset.add("a", 2);
        multiset.add("b", 4);
        OptionalDouble avg = multiset.averageOfOccurrences();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);

        multiset.add("c", 6);
        avg = multiset.averageOfOccurrences();
        assertTrue(avg.isPresent());
        assertEquals(4.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testDeprecatedCount() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertEquals(2, multiset.count("a"));
    }

    @Test
    public void testElementSet() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c", "b", "a");
        Set<String> elementSet = multiset.elementSet();
        assertEquals(N.asSet("a", "b", "c"), elementSet);
        assertEquals(3, elementSet.size());

        multiset.add("d");
        assertEquals(N.asSet("a", "b", "c", "d"), elementSet);
        assertEquals(4, elementSet.size());

        multiset.remove("a", 3);
        assertEquals(N.asSet("b", "c", "d"), elementSet);

        elementSet.remove("b");
        assertFalse(multiset.contains("b"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(N.asSet("c", "d"), elementSet);
    }

    @Test
    public void testEntrySet() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();
        assertEquals(3, entrySet.size());

        Map<String, Integer> expectedEntries = N.newHashMap();
        expectedEntries.put("a", 2);
        expectedEntries.put("b", 1);
        expectedEntries.put("c", 1);

        for (Multiset.Entry<String> entry : entrySet) {
            assertTrue(expectedEntries.containsKey(entry.element()));
            assertEquals(expectedEntries.get(entry.element()), entry.count());
        }

        int count = 0;
        for (Iterator<Multiset.Entry<String>> it = entrySet.iterator(); it.hasNext();) {
            Multiset.Entry<String> entry = it.next();
            assertTrue(expectedEntries.containsKey(entry.element()));
            count++;
        }
        assertEquals(3, count);

        multiset.add("d");
        assertEquals(4, entrySet.size());
        multiset.removeAllOccurrences("a");
        assertEquals(3, entrySet.size());
    }

    @Test
    public void testEntrySet_iterator_next_without_hasNext() {
        Multiset<String> multiset = Multiset.of("a");
        Iterator<Multiset.Entry<String>> it = multiset.entrySet().iterator();
        assertNotNull(it.next());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testSize() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.size());
        multiset.add("a");
        assertEquals(1, multiset.size());
        multiset.add("a");
        assertEquals(2, multiset.size());
        multiset.add("b", 3);
        assertEquals(5, multiset.size());
    }

    @Test
    public void testSize_overflow() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", Integer.MAX_VALUE);
        multiset.add("b", 1);
        assertThrows(ArithmeticException.class, multiset::size);
    }

    @Test
    public void testCountOfDistinctElements() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.countOfDistinctElements());
        multiset.add("a");
        assertEquals(1, multiset.countOfDistinctElements());
        multiset.add("a");
        assertEquals(1, multiset.countOfDistinctElements());
        multiset.add("b");
        assertEquals(2, multiset.countOfDistinctElements());
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
    public void testRemoveAndGetCount() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "a");
        assertEquals(2, multiset.removeAndGetCount("a", 2));
        assertEquals(2, multiset.getCount("a"));

        assertEquals(0, multiset.removeAndGetCount("a", 3));
        assertEquals(0, multiset.getCount("a"));

        assertEquals(0, multiset.removeAndGetCount("b", 1));
        assertEquals(0, multiset.getCount("b"));
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
    public void testRemoveAllOccurrences_Collection() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "c");
        Collection<String> toRemove = Arrays.asList("a", "b", "d", "a");

        assertTrue(multiset.removeAllOccurrences(toRemove));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("d"));
        assertEquals(1, multiset.size());
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate() {
        Multiset<String> multiset = Multiset.of("apple", "apricot", "banana", "blueberry", "avocado");
        multiset.add("apple", 2);

        Predicate<String> startsWithA = s -> s.startsWith("a");
        assertTrue(multiset.removeAllOccurrencesIf(startsWithA));
        assertEquals(0, multiset.getCount("apple"));
        assertEquals(0, multiset.getCount("apricot"));
        assertEquals(0, multiset.getCount("avocado"));
        assertEquals(1, multiset.getCount("banana"));
        assertEquals(1, multiset.getCount("blueberry"));
        assertEquals(2, multiset.size());

        assertFalse(multiset.removeAllOccurrencesIf(startsWithA));
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
    public void testRemoveAllOccurrencesIf_ObjIntPredicate() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "c");

        ObjIntPredicate<String> countAtLeastTwo = (s, count) -> count >= 2;
        assertTrue(multiset.removeAllOccurrencesIf(countAtLeastTwo));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(1, multiset.size());

        assertFalse(multiset.removeAllOccurrencesIf(countAtLeastTwo));
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
    public void testRetainAll() {
        Multiset<String> multiset = Multiset.of("a", "a", "b", "c", "c", "c");
        Collection<String> toRetain = Arrays.asList("a", "c", "d");

        assertTrue(multiset.retainAll(toRetain));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(3, multiset.getCount("c"));
        assertEquals(5, multiset.size());

        assertFalse(multiset.retainAll(Arrays.asList("a", "c", "e")));

        assertTrue(multiset.retainAll(Collections.singletonList("c")));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("c"));
        assertEquals(3, multiset.size());

        assertTrue(multiset.retainAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
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
    public void testClear() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertFalse(multiset.isEmpty());
        multiset.clear();
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
        assertEquals(0, multiset.getCount("a"));
    }

    @Test
    public void testUpdateAllOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "b", "c", "c", "c");

        ObjIntFunction<String, Integer> updater = (elem, count) -> {
            if (elem.equals("a"))
                return count * 2;
            if (elem.equals("b"))
                return 0;
            if (elem.equals("c"))
                return count / 2;
            return count;
        };

        multiset.updateAllOccurrences(updater);
        assertEquals(4, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(4 + 1, multiset.size());
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
    public void testToArray() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Object[] array = multiset.toArray();
        assertEquals(4, array.length);
        List<Object> list = Arrays.asList(array);
        assertEquals(2, Collections.frequency(list, "a"));
        assertEquals(1, Collections.frequency(list, "b"));
        assertEquals(1, Collections.frequency(list, "c"));
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
    public void testToMap() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Map<String, Integer> map = multiset.toMap();
        assertEquals(3, map.size());
        assertEquals(2, map.get("a").intValue());
        assertEquals(1, map.get("b").intValue());
        assertEquals(1, map.get("c").intValue());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testToMap_empty() {
        Multiset<String> multiset = new Multiset<>();
        Map<String, Integer> map = multiset.toMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMap_WithSupplier() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        IntFunction<Map<String, Integer>> supplier = LinkedHashMap::new;

        Map<String, Integer> map = multiset.toMap(supplier);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(3, map.size());
        assertEquals(2, map.get("a").intValue());

        Multiset<String> orderedMultiset = new Multiset<>(LinkedHashMap.class);
        orderedMultiset.add("z", 1);
        orderedMultiset.add("y", 2);
        orderedMultiset.add("x", 3);

        Map<String, Integer> linkedMap = orderedMultiset.toMap(LinkedHashMap::new);
        Iterator<Map.Entry<String, Integer>> it = linkedMap.entrySet().iterator();
        assertEquals("z", it.next().getKey());
        assertEquals("y", it.next().getKey());
        assertEquals("x", it.next().getKey());
    }

    @Test
    public void testToMapSortedByOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("c", 3);
        multiset.add("a", 1);
        multiset.add("b", 2);
        multiset.add("d", 1);

        Map<String, Integer> sortedMap = multiset.toMapSortedByOccurrences();
        assertTrue(sortedMap instanceof LinkedHashMap);

        Iterator<Map.Entry<String, Integer>> it = sortedMap.entrySet().iterator();
        Map.Entry<String, Integer> entry1 = it.next();
        assertEquals(1, entry1.getValue().intValue());
        assertTrue(entry1.getKey().equals("a") || entry1.getKey().equals("d"));

        Map.Entry<String, Integer> entry2 = it.next();
        assertEquals(1, entry2.getValue().intValue());
        assertTrue(entry2.getKey().equals("a") || entry2.getKey().equals("d"));
        assertNotEquals(entry1.getKey(), entry2.getKey());

        assertEquals("b", it.next().getKey());
        assertEquals("c", it.next().getKey());
        assertFalse(it.hasNext());
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
    public void testToMapSortedByKey() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("zebra", 3);
        multiset.add("apple", 1);
        multiset.add("banana", 2);

        Map<String, Integer> sortedMap = multiset.toMapSortedByKey(Comparator.naturalOrder());
        Iterator<Map.Entry<String, Integer>> it = sortedMap.entrySet().iterator();
        assertEquals("apple", it.next().getKey());
        assertEquals("banana", it.next().getKey());
        assertEquals("zebra", it.next().getKey());
        assertFalse(it.hasNext());

        Map<String, Integer> sortedMapDesc = multiset.toMapSortedByKey(Comparator.reverseOrder());
        Iterator<Map.Entry<String, Integer>> itDesc = sortedMapDesc.entrySet().iterator();
        assertEquals("zebra", itDesc.next().getKey());
        assertEquals("banana", itDesc.next().getKey());
        assertEquals("apple", itDesc.next().getKey());
        assertFalse(itDesc.hasNext());
    }

    @Test
    public void testToImmutableMap() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        com.landawn.abacus.util.ImmutableMap<String, Integer> immutableMap = multiset.toImmutableMap();
        assertEquals(2, immutableMap.size());
        assertEquals(2, immutableMap.get("a").intValue());
        assertEquals(1, immutableMap.get("b").intValue());
        assertThrows(UnsupportedOperationException.class, () -> immutableMap.put("c", 1));
    }

    @Test
    public void testToImmutableMap_WithSupplier() {
        Multiset<String> multiset = Multiset.of("z", "y", "z");
        IntFunction<Map<String, Integer>> supplier = (size) -> new TreeMap<>();

        com.landawn.abacus.util.ImmutableMap<String, Integer> immutableMap = multiset.toImmutableMap(supplier);
        assertEquals(2, immutableMap.size());
        assertEquals(1, immutableMap.get("y").intValue());
        assertEquals(2, immutableMap.get("z").intValue());

        assertThrows(UnsupportedOperationException.class, () -> immutableMap.put("c", 1));
    }

    @Test
    public void testIterator() {
        Multiset<String> multiset = new Multiset<>(LinkedHashMap.class);
        multiset.add("a", 2);
        multiset.add("b", 1);
        multiset.add("c", 3);

        List<String> iteratedElements = new ArrayList<>();
        Iterator<String> it = multiset.iterator();
        while (it.hasNext()) {
            iteratedElements.add(it.next());
        }

        assertEquals(2 + 1 + 3, iteratedElements.size());

        assertEquals(Arrays.asList("a", "a", "b", "c", "c", "c"), iteratedElements);

        Multiset<String> hashMultiset = Multiset.of("x", "y", "x", "z", "x");
        List<String> hashIterated = new ArrayList<>();
        hashMultiset.iterator().forEachRemaining(hashIterated::add);
        assertEquals(5, hashIterated.size());
        assertEquals(3, Collections.frequency(hashIterated, "x"));
        assertEquals(1, Collections.frequency(hashIterated, "y"));
        assertEquals(1, Collections.frequency(hashIterated, "z"));
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
    public void testForEach_ObjIntConsumer() {
        Multiset<String> multiset = new Multiset<>(LinkedHashMap.class);
        multiset.add("a", 2);
        multiset.add("b", 1);
        multiset.add("c", 3);

        Map<String, Integer> seen = new LinkedHashMap<>();
        ObjIntConsumer<String> action = seen::put;

        multiset.forEach(action);

        assertEquals(3, seen.size());
        assertEquals(2, seen.get("a").intValue());
        assertEquals(1, seen.get("b").intValue());
        assertEquals(3, seen.get("c").intValue());

        Iterator<Map.Entry<String, Integer>> it = seen.entrySet().iterator();
        assertEquals("a", it.next().getKey());
        assertEquals("b", it.next().getKey());
        assertEquals("c", it.next().getKey());
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
    public void testApply() {
        Multiset<String> multiset = Multiset.of("a", "b");
        String result = multiset.apply(ms -> "Size is " + ms.size());
        assertEquals("Size is 2", result);
    }

    @Test
    public void testApplyIfNotEmpty_NotEmpty() {
        Multiset<String> multiset = Multiset.of("a");
        Optional<String> result = multiset.applyIfNotEmpty(ms -> "First element: " + ms.iterator().next());
        assertTrue(result.isPresent());
        assertEquals("First element: a", result.get());
    }

    @Test
    public void testApplyIfNotEmpty_Empty() {
        Multiset<String> multiset = new Multiset<>();
        Optional<Integer> result = multiset.applyIfNotEmpty(Multiset::size);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAccept() {
        Multiset<String> multiset = Multiset.of("hello");
        AtomicReference<String> ref = new AtomicReference<>();
        multiset.accept(ms -> ref.set(ms.iterator().next().toUpperCase()));
        assertEquals("HELLO", ref.get());
    }

    @Test
    public void testAcceptIfNotEmpty_NotEmpty() {
        Multiset<String> multiset = Multiset.of("data");
        AtomicInteger counter = new AtomicInteger(0);
        multiset.acceptIfNotEmpty(ms -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testAcceptIfNotEmpty_Empty() {
        Multiset<String> multiset = new Multiset<>();
        AtomicInteger counter = new AtomicInteger(0);
        multiset.acceptIfNotEmpty(ms -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testHashCode() {
        Multiset<String> m1 = Multiset.of("a", "b", "a");
        Multiset<String> m2 = Multiset.of("b", "a", "a");
        Multiset<String> m3 = new Multiset<>();
        m3.add("a", 2);
        m3.add("b", 1);

        assertEquals(m1.hashCode(), m2.hashCode());
        assertEquals(m1.hashCode(), m3.hashCode());

        Multiset<String> m4 = Multiset.of("a", "b");
        assertNotEquals(m1.hashCode(), m4.hashCode());

        Multiset<String> empty1 = new Multiset<>();
        Multiset<String> empty2 = new Multiset<>();
        assertEquals(empty1.hashCode(), empty2.hashCode());
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
    public void testToString() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals("{}", multiset.toString());

        multiset.add("a", 2);
        multiset.add("b", 1);
        String str = multiset.toString();
        assertTrue(str.startsWith("{") && str.endsWith("}"));
        assertTrue(str.contains("a=2"));
        assertTrue(str.contains("b=1"));
        assertEquals(str.length(), "{a=2, b=1}".length());

        Multiset<String> linkedMultiset = new Multiset<>(LinkedHashMap.class);
        linkedMultiset.add("c", 3);
        linkedMultiset.add("a", 1);
        assertEquals("{c=3, a=1}", linkedMultiset.toString());
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
}
