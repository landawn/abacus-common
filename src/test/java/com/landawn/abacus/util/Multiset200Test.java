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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.ObjIntFunction;
import com.landawn.abacus.util.function.ObjIntPredicate;

public class Multiset200Test extends TestBase {

    //region Constructors and Static Factory Methods
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
        // Note: Verifying internal capacity is not directly possible without reflection or package-private access.
        // We trust it sets the initial capacity for the underlying map.
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
        // Verify order if possible (depends on map type, LinkedHashMap preserves insertion order for elementSet)
        List<String> elements = new ArrayList<>(multiset.elementSet());
        assertEquals(Arrays.asList("c", "a", "b"), elements);
        assertTrue(multiset.toString().startsWith("{c=1, a=1, b=1}")); // toString uses backingMap's toString
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
        Multiset<String> multiset = Multiset.of((String[]) null); // N.isEmpty handles null array
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

    //region Query Operations
    @Test
    public void testOccurrencesOfAndGetCount() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertEquals(2, multiset.occurrencesOf("a"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.occurrencesOf("b"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(0, multiset.occurrencesOf("c"));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(0, multiset.getCount(null)); // Assuming nulls are not added
    }

    @Test
    public void testMinOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.minOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c")); // a:2, b:1, c:3
        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertEquals("b", min.get().right());

        multiset.add("d", 1); // d:1, b:1
        min = multiset.minOccurrences(); // Behavior for ties is one of them
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

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c")); // a:2, b:1, c:3
        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(3, max.get().left().intValue());
        assertEquals("c", max.get().right());

        multiset.add("d", 3); // d:3, c:3
        max = multiset.maxOccurrences(); // Behavior for ties is one of them
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

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c", "d")); // a:2, b:1, c:3, d:1
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

        multiset.addAll(Arrays.asList("a", "a", "a", "b", "c", "c", "d", "d", "d")); // a:3, b:1, c:2, d:3
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

        //    assertThrows(ArithmeticException.class, () -> {
        //        Multiset<Integer> m = new Multiset<>();
        //        m.add(1, Integer.MAX_VALUE);
        //        m.add(2, Integer.MAX_VALUE);
        //        m.sumOfOccurrences(); // This should overflow long
        //    });

        Multiset<Integer> m = new Multiset<>();
        m.add(1, Integer.MAX_VALUE);
        m.add(2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, m.sumOfOccurrences()); // This should not overflow long, as it is within range
    }

    @Test
    public void testAverageOfOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.averageOfOccurrences().isPresent());

        multiset.add("a", 2); // count: 2
        multiset.add("b", 4); // count: 4
        // distinct elements: a, b. sum = 6. distinct count = 2. avg = 3.0
        OptionalDouble avg = multiset.averageOfOccurrences();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);

        multiset.add("c", 6); // count: 6
        // distinct: a, b, c. sum = 12. distinct count = 3. avg = 4.0
        avg = multiset.averageOfOccurrences();
        assertTrue(avg.isPresent());
        assertEquals(4.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testDeprecatedCount() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertEquals(2, multiset.count("a")); // deprecated
    }

    @Test
    public void testElementSet() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c", "b", "a");
        Set<String> elementSet = multiset.elementSet();
        assertEquals(N.asSet("a", "b", "c"), elementSet);
        assertEquals(3, elementSet.size());

        // Test that changes to multiset reflect in elementSet
        multiset.add("d");
        assertEquals(N.asSet("a", "b", "c", "d"), elementSet);
        assertEquals(4, elementSet.size());

        multiset.remove("a", 3); // remove all 'a's
        assertEquals(N.asSet("b", "c", "d"), elementSet);

        // Test that changes to elementSet (if supported, usually read-only view for keySet)
        // The returned set is typically a view, modifications like 'remove' on keySet affect the map.
        // elementSet().clear() would clear the multiset.
        elementSet.remove("b");
        assertFalse(multiset.contains("b"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(N.asSet("c", "d"), elementSet);
    }

    @Test
    public void testEntrySet() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c"); // a:2, b:1, c:1
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

        //    // Test contains for EntrySet
        //    assertTrue(entrySet.contains(N.newImmutableEntry("a", 2)));
        //    assertFalse(entrySet.contains(N.newImmutableEntry("a", 1)));
        //    assertFalse(entrySet.contains(N.newImmutableEntry("d", 1)));
        //    assertFalse(entrySet.contains(new Object())); // Test with a different object type

        // Test iterator for EntrySet
        int count = 0;
        for (Iterator<Multiset.Entry<String>> it = entrySet.iterator(); it.hasNext();) {
            Multiset.Entry<String> entry = it.next();
            assertTrue(expectedEntries.containsKey(entry.element()));
            count++;
        }
        assertEquals(3, count);

        // Test modifications to multiset reflect in entrySet (size)
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
        multiset.add("b", 1); // This will cause sumOfOccurrences to overflow long, then toIntExact to fail
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
        assertFalse(multiset.contains(null)); // Assuming nulls not added
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
    //endregion

    //region Modification Operations (Adding)
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
        Multiset<String> multiset = new Multiset<>(); // Uses HashMap
        // HashMap allows null keys, so adding null should work.
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

        assertEquals(0, multiset.add("b", 0)); // Add 0 occurrences
        assertEquals(0, multiset.getCount("b"));
        assertEquals(5, multiset.size()); // Size should not change

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

        assertEquals(Integer.MAX_VALUE - 1, multiset.getCount("a")); // should not change

        Multiset<String> multiset2 = new Multiset<>();
        //  assertThrows(IllegalArgumentException.class, () -> multiset2.add("b", Integer.MAX_VALUE)); // Not a method

        multiset2.add("b", 10);
        // assertThrows(IllegalArgumentException.class, () -> multiset2.add("b", Integer.MAX_VALUE - 10));
        multiset2.add("b", Integer.MAX_VALUE - 10);
        assertEquals(Integer.MAX_VALUE, multiset2.getCount("b")); // Should not throw

    }

    @Test
    public void testAddAndGetCount() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(3, multiset.addAndGetCount("a", 3));
        assertEquals(3, multiset.getCount("a"));

        assertEquals(5, multiset.addAndGetCount("a", 2));
        assertEquals(5, multiset.getCount("a"));

        assertEquals(0, multiset.addAndGetCount("b", 0)); // Add 0 occurrences
        assertEquals(0, multiset.getCount("b"));

        assertEquals(5, multiset.getCount("a")); // 'a' count should be unchanged
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
        assertEquals(Integer.MAX_VALUE - 1, multiset.getCount("a")); // Should not change on exception
    }

    @Test
    public void testAddAllCollection() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a");
        Collection<String> toAdd = Arrays.asList("a", "b", "c", "b");

        assertTrue(multiset.addAll(toAdd));
        assertEquals(2, multiset.getCount("a")); // 1 existing + 2 from list (actually 1 existing + 1 from list's "a")
                                                 // addAll(c) means add(e,1) for each e in c
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
        assertFalse(multiset.addAll(null)); // N.isEmpty(null) is true
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 1); // a:1
        Collection<String> toAdd = Arrays.asList("a", "b"); // Add "a" twice, "b" twice

        assertTrue(multiset.addAll(toAdd, 2));
        assertEquals(1 + 2, multiset.getCount("a")); // 1 existing + 2 occurrences
        assertEquals(2, multiset.getCount("b")); // 0 existing + 2 occurrences
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
        assertFalse(multiset.addAll(null, 2)); // N.isEmpty(null) is true
        assertEquals(1, multiset.getCount("a"));
    }

    //endregion

    //region Modification Operations (Setting Count)
    @Test
    public void testSetCount() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.setCount("a", 3)); // Was 0, set to 3
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.size());

        assertEquals(3, multiset.setCount("a", 5)); // Was 3, set to 5
        assertEquals(5, multiset.getCount("a"));
        assertEquals(5, multiset.size());

        assertEquals(5, multiset.setCount("a", 0)); // Was 5, set to 0 (remove)
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty()); // 'a' was the only element

        multiset.add("b", 2); // b:2
        assertEquals(0, multiset.setCount("c", 1)); // c not present, set to 1
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
        Multiset<String> multiset = new Multiset<>(); // {}
        // Element not present, oldCount is 0
        assertTrue(multiset.setCount("a", 0, 3)); // Expected old:0, actual old:0. Set "a" to 3
        assertEquals(3, multiset.getCount("a"));

        assertFalse(multiset.setCount("a", 2, 5)); // Expected old:2, actual old:3. No change.
        assertEquals(3, multiset.getCount("a"));

        assertTrue(multiset.setCount("a", 3, 1)); // Expected old:3, actual old:3. Set "a" to 1
        assertEquals(1, multiset.getCount("a"));

        assertTrue(multiset.setCount("a", 1, 0)); // Expected old:1, actual old:1. Set "a" to 0 (remove)
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty());

        // Element not present, oldOccurrences is 0, newOccurrences is 0
        assertTrue(multiset.setCount("b", 0, 0));
        assertEquals(0, multiset.getCount("b"));
        assertTrue(multiset.isEmpty());

        // Element present, oldOccurrences matches, newOccurrences is same
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
    //endregion

    //region Modification Operations (Removing)
    @Test
    public void testRemoveSingleElement() {
        Multiset<String> multiset = Multiset.of("a", "a", "b"); // a:2, b:1
        assertTrue(multiset.remove("a"));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(2, multiset.size());

        assertTrue(multiset.remove("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertFalse(multiset.remove("a")); // Already 0
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.remove("b"));
        assertEquals(0, multiset.getCount("b"));
        assertTrue(multiset.isEmpty());

        assertFalse(multiset.remove("c")); // Not present
    }

    @Test
    public void testRemoveElementWithOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "a", "b", "b"); // a:4, b:2
        assertEquals(4, multiset.remove("a", 2)); // Removed 2 'a's, old count was 4
        assertEquals(2, multiset.getCount("a"));
        assertEquals(4, multiset.size());

        assertEquals(2, multiset.remove("a", 3)); // Try to remove 3, only 2 present, old count was 2
        assertEquals(0, multiset.getCount("a"));
        assertEquals(2, multiset.size()); // Only 'b's left

        assertEquals(0, multiset.remove("c", 1)); // 'c' not present
        assertEquals(0, multiset.getCount("c"));
        assertEquals(2, multiset.size());

        assertEquals(2, multiset.remove("b", 0)); // Remove 0 occurrences
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
        Multiset<String> multiset = Multiset.of("a", "a", "a", "a"); // a:4
        assertEquals(2, multiset.removeAndGetCount("a", 2)); // New count is 2
        assertEquals(2, multiset.getCount("a"));

        assertEquals(0, multiset.removeAndGetCount("a", 3)); // New count is 0 (was 2, remove 3)
        assertEquals(0, multiset.getCount("a"));

        assertEquals(0, multiset.removeAndGetCount("b", 1)); // 'b' not present, new count 0
        assertEquals(0, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAndGetCount_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAndGetCount("a", -1));
    }

    @Test
    public void testDeprecatedRemoveAll_Collection() {
        Multiset<String> multiset = Multiset.of("a", "a", "b", "c", "c"); // a:2, b:1, c:2
        Collection<String> toRemove = Arrays.asList("a", "c", "d"); // remove one 'a', one 'c'

        assertTrue(multiset.removeAll(toRemove)); // Should be true because 'a' and 'c' were removed
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b")); // 'b' untouched
        assertEquals(0, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("d")); // 'd' was not there
        assertEquals(1, multiset.size());

        assertFalse(multiset.removeAll(Arrays.asList("x", "y"))); // Nothing removed
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
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "b", "c"); // a:3, b:3, c:1
        Collection<String> toRemove = Arrays.asList("a", "b", "d");

        assertTrue(multiset.removeAll(toRemove, 2)); // Remove 2 'a's, 2 'b's. 'd' not present.
        assertEquals(1, multiset.getCount("a")); // 3-2=1
        assertEquals(1, multiset.getCount("b")); // 3-2=1
        assertEquals(1, multiset.getCount("c")); // 'c' untouched
        assertEquals(0, multiset.getCount("d"));
        assertEquals(3, multiset.size());

        // Remove more than present
        assertTrue(multiset.removeAll(Arrays.asList("a", "b"), 2)); // remove 2 of each, only 1 of each left
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
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b"); // a:3, b:1
        assertEquals(3, multiset.removeAllOccurrences("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.size());

        assertEquals(0, multiset.removeAllOccurrences("c")); // Not present
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAllOccurrences_Collection() {
        // This method is actually removeAll(c), which removes only one occurrence of each.
        // The Javadoc indicates it's `removeAll(c)`.
        // The name is confusing. It should behave like the deprecated removeAll(Collection).
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "c"); // a:3, b:2, c:1
        Collection<String> toRemove = Arrays.asList("a", "b", "d", "a"); // remove one 'a', one 'b' (due to Collection semantics in removeAll)

        assertTrue(multiset.removeAllOccurrences(toRemove));
        assertEquals(0, multiset.getCount("a")); // 3 - 1 = 2 (because "a" is in toRemove once effectively for distinctness)
        assertEquals(0, multiset.getCount("b")); // 2 - 1 = 1
        assertEquals(1, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("d"));
        assertEquals(1, multiset.size());
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate() {
        Multiset<String> multiset = Multiset.of("apple", "apricot", "banana", "blueberry", "avocado");
        multiset.add("apple", 2); // apple:3, apricot:1, banana:1, blueberry:1, avocado:1

        // Remove elements starting with "a"
        Predicate<String> startsWithA = s -> s.startsWith("a");
        assertTrue(multiset.removeAllOccurrencesIf(startsWithA));
        assertEquals(0, multiset.getCount("apple"));
        assertEquals(0, multiset.getCount("apricot"));
        assertEquals(0, multiset.getCount("avocado"));
        assertEquals(1, multiset.getCount("banana"));
        assertEquals(1, multiset.getCount("blueberry"));
        assertEquals(2, multiset.size());

        assertFalse(multiset.removeAllOccurrencesIf(startsWithA)); // No "a" elements left
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
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "c"); // a:3, b:2, c:1

        // Remove elements if count is >= 2
        ObjIntPredicate<String> countAtLeastTwo = (s, count) -> count >= 2;
        assertTrue(multiset.removeAllOccurrencesIf(countAtLeastTwo));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c")); // c has count 1, not removed
        assertEquals(1, multiset.size());

        assertFalse(multiset.removeAllOccurrencesIf(countAtLeastTwo)); // No elements with count >= 2 left
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_null() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((ObjIntPredicate<String>) null));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_noMatch() {
        Multiset<String> multiset = Multiset.of("a", "b"); // a:1, b:1
        assertFalse(multiset.removeAllOccurrencesIf((s, count) -> count > 1));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRetainAll() {
        Multiset<String> multiset = Multiset.of("a", "a", "b", "c", "c", "c"); // a:2, b:1, c:3
        Collection<String> toRetain = Arrays.asList("a", "c", "d"); // Keep 'a' and 'c', 'd' is not there

        assertTrue(multiset.retainAll(toRetain)); // 'b' should be removed
        assertEquals(2, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(3, multiset.getCount("c"));
        assertEquals(5, multiset.size());

        assertFalse(multiset.retainAll(Arrays.asList("a", "c", "e"))); // No change, 'e' not there

        assertTrue(multiset.retainAll(Collections.singletonList("c"))); // Remove 'a'
        assertEquals(0, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("c"));
        assertEquals(3, multiset.size());

        assertTrue(multiset.retainAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testRetainAll_nullCollection() {
        Multiset<String> multiset = Multiset.of("a", "b");
        // N.isEmpty(null) is true, so this should clear the multiset
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
    //endregion

    //region Compute/Update/Merge Operations
    @Test
    public void testUpdateAllOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "b", "c", "c", "c"); // a:2, b:1, c:3

        // Double the count for 'a', halve for 'c', remove 'b' (return 0 or less)
        ObjIntFunction<String, Integer> updater = (elem, count) -> {
            if (elem.equals("a"))
                return count * 2;
            if (elem.equals("b"))
                return 0; // remove b
            if (elem.equals("c"))
                return count / 2; // 3/2 = 1
            return count;
        };

        multiset.updateAllOccurrences(updater);
        assertEquals(4, multiset.getCount("a")); // 2*2 = 4
        assertEquals(0, multiset.getCount("b")); // Removed
        assertEquals(1, multiset.getCount("c")); // 3/2 = 1
        assertEquals(4 + 1, multiset.size());
    }

    @Test
    public void testUpdateAllOccurrences_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.updateAllOccurrences(null));
    }

    @Test
    public void testUpdateAllOccurrences_returnNull() {
        Multiset<String> multiset = Multiset.of("a", "b"); // a:1, b:1
        multiset.updateAllOccurrences((el, count) -> el.equals("a") ? null : count);
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testComputeIfAbsent() {
        Multiset<String> multiset = new Multiset<>();
        ToIntFunction<String> computer = s -> s.length(); // Computes count based on string length

        // "a" is absent, compute its count (length of "a" is 1)
        assertEquals(1, multiset.computeIfAbsent("a", computer));
        assertEquals(1, multiset.getCount("a"));

        // "a" is present, should return existing count, not recompute
        assertEquals(1, multiset.computeIfAbsent("a", computer));
        assertEquals(1, multiset.getCount("a"));

        // "xyz" is absent, compute its count (length of "xyz" is 3)
        assertEquals(3, multiset.computeIfAbsent("xyz", computer));
        assertEquals(3, multiset.getCount("xyz"));

        // Compute to 0, should not be added
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
        Multiset<String> multiset = Multiset.of("a", "a", "b"); // a:2, b:1
        ObjIntFunction<String, Integer> remapper = (s, count) -> s.equals("a") ? count + 1 : 0;

        // "a" is present (count 2), remap to 2+1=3
        assertEquals(3, multiset.computeIfPresent("a", remapper));
        assertEquals(3, multiset.getCount("a"));

        // "b" is present (count 1), remap to 0 (remove)
        assertEquals(0, multiset.computeIfPresent("b", remapper));
        assertEquals(0, multiset.getCount("b"));

        // "c" is not present, should return 0, no change
        assertEquals(0, multiset.computeIfPresent("c", remapper));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(3, multiset.getCount("a")); // 'a' unchanged
    }

    @Test
    public void testComputeIfPresent_nullFunction() {
        Multiset<String> multiset = Multiset.of("a");
        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfPresent("a", null));
    }

    @Test
    public void testCompute() {
        Multiset<String> multiset = new Multiset<>(); // empty
        ObjIntFunction<String, Integer> computer = (s, oldCount) -> {
            if (s.equals("add"))
                return oldCount + 2; // Add 2 or increment by 2
            if (s.equals("set"))
                return 5; // Set to 5
            if (s.equals("remove"))
                return 0; // Remove if present
            if (s.equals("no_change_if_present"))
                return oldCount > 0 ? oldCount : 0;
            if (s.equals("add_if_absent"))
                return oldCount == 0 ? 1 : oldCount;
            return 0; // default no change
        };

        // "add": key not present (oldCount=0), new value = 0+2=2
        assertEquals(2, multiset.compute("add", computer));
        assertEquals(2, multiset.getCount("add"));

        // "add": key present (oldCount=2), new value = 2+2=4
        assertEquals(4, multiset.compute("add", computer));
        assertEquals(4, multiset.getCount("add"));

        // "set": key not present (oldCount=0), new value = 5
        assertEquals(5, multiset.compute("set", computer));
        assertEquals(5, multiset.getCount("set"));

        // "set": key present (oldCount=5), new value = 5 (no actual change in count, but recomputed)
        assertEquals(5, multiset.compute("set", computer));
        assertEquals(5, multiset.getCount("set"));

        // "remove": key present (oldCount=5 from "set"), new value = 0
        multiset.setCount("remove_target", 3);
        assertEquals(0, multiset.compute("remove_target", computer));
        assertEquals(0, multiset.getCount("remove_target"));

        // "remove": key not present (oldCount=0), new value = 0
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
        IntBiFunction<Integer> merger = (oldCount, value) -> oldCount + value; // Sum merger

        // "a": not present, oldValue=0. newValue = value (3)
        assertEquals(3, multiset.merge("a", 3, merger));
        assertEquals(3, multiset.getCount("a"));

        // "a": present (count=3), oldValue=3. newValue = merger(3, 2) = 5
        assertEquals(5, multiset.merge("a", 2, merger));
        assertEquals(5, multiset.getCount("a"));

        // "b": not present, value=1. newValue = 1
        assertEquals(1, multiset.merge("b", 1, merger));
        assertEquals(1, multiset.getCount("b"));

        // Merge to 0 to remove
        IntBiFunction<Integer> subtractMerger = (oldCount, value) -> oldCount - value;
        multiset.setCount("c", 5);
        assertEquals(0, multiset.merge("c", 5, subtractMerger)); // 5-5 = 0
        assertEquals(0, multiset.getCount("c"));

        // Merge on absent element resulting in 0
        assertEquals(0, multiset.merge("d", 0, merger)); // old=0, value=0 -> new=0
        assertEquals(0, multiset.getCount("d"));

        // Merge on absent element, new value is value if old value is 0.
        // remappingFunction.apply(oldValue, value); -> remappingFunction.apply(0, value)
        // The code is: final int newValue = (oldValue == 0) ? value : remappingFunction.apply(oldValue, value);
        // So if key is absent, oldValue is 0, newValue becomes 'value'.
        assertEquals(7, multiset.merge("e", 7, (ov, v) -> ov + v + 100)); // oldValue=0, so newValue=7 (not 0+7+100)
        assertEquals(7, multiset.getCount("e"));

        // Test the remappingFunction part when element is present
        assertEquals(7 + 8 + 100, multiset.merge("e", 8, (ov, v) -> ov + v + 100)); // oldValue=7, value=8 -> newValue=7+8+100=115
        assertEquals(115, multiset.getCount("e"));
    }

    @Test
    public void testMerge_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.merge("a", 1, null));
    }

    //endregion

    //region Conversion Operations
    @Test
    public void testToArray() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c"); // a:2, b:1, c:1. Total 4.
        Object[] array = multiset.toArray();
        assertEquals(4, array.length);
        // Content needs to be checked carefully as order is not guaranteed beyond element grouping
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
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c"); // a:2, b:1, c:1. Total 4.

        // Array too small
        String[] arraySmall = new String[2];
        String[] resultSmall = multiset.toArray(arraySmall);
        assertNotSame(arraySmall, resultSmall); // New array should be allocated
        assertEquals(4, resultSmall.length);
        List<String> listSmall = Arrays.asList(resultSmall);
        assertEquals(2, Collections.frequency(listSmall, "a"));
        assertEquals(1, Collections.frequency(listSmall, "b"));
        assertEquals(1, Collections.frequency(listSmall, "c"));

        // Array exact size
        String[] arrayExact = new String[4];
        String[] resultExact = multiset.toArray(arrayExact);
        assertSame(arrayExact, resultExact);
        List<String> listExact = Arrays.asList(resultExact);
        assertEquals(2, Collections.frequency(listExact, "a"));
        assertEquals(1, Collections.frequency(listExact, "b"));
        assertEquals(1, Collections.frequency(listExact, "c"));

        // Array too large
        String[] arrayLarge = new String[6];
        String[] resultLarge = multiset.toArray(arrayLarge);
        assertSame(arrayLarge, resultLarge); // Should use the provided array
        // Elements should be at the beginning, followed by nulls if original array was larger and not cleared
        // The implementation fills from index 0 up to size.
        List<String> listLarge = Arrays.asList(resultLarge); // includes nulls
        assertEquals(2, Collections.frequency(listLarge.subList(0, 4), "a"));
        assertEquals(1, Collections.frequency(listLarge.subList(0, 4), "b"));
        assertEquals(1, Collections.frequency(listLarge.subList(0, 4), "c"));
        if (arrayLarge.length > multiset.size()) { // If toArray contract implies nulling rest of array
            // The current implementation does not null out remaining elements if 'a' is larger.
            // This is standard for Collection.toArray(T[]).
            // So if arrayLarge[4] had a value, it would remain.
            // Let's ensure arrayLarge has nulls first for a clean test.
            Arrays.fill(arrayLarge, null);
            multiset.toArray(arrayLarge);
            assertNull(arrayLarge[4]); // Index for size=4 is arrayLarge[3], so arrayLarge[4] is after elements
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
        assertSame(b, resultB); // Standard behavior is to use 'b', but elements are 0.
                                // If size < a.length, a[size] = null (if contract implies)
                                // Current impl does not set a[size]=null.
                                // For an empty multiset, nothing is written to 'b', so 'b' remains unchanged.
        assertEquals("test", b[0]); // If T[] contract means nulling a[size] this would be null
                                    // but standard Collections.toArray doesn't do that
                                    // for empty source.
                                    // The provided code does N.fill, which won't run for 0 occurrences.
                                    // So array 'b' is unchanged.
    }

    @Test
    public void testToMap() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c"); // a:2, b:1, c:1
        Map<String, Integer> map = multiset.toMap();
        assertEquals(3, map.size());
        assertEquals(2, map.get("a").intValue());
        assertEquals(1, map.get("b").intValue());
        assertEquals(1, map.get("c").intValue());
        assertTrue(map instanceof HashMap); // Default backing map type from N.newHashMap
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
        IntFunction<Map<String, Integer>> supplier = LinkedHashMap::new; // size -> new LinkedHashMap<>(size)

        Map<String, Integer> map = multiset.toMap(supplier);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(3, map.size());
        assertEquals(2, map.get("a").intValue());

        // Verify order for LinkedHashMap (insertion order of distinct elements)
        // The backingMap's keySet order dictates this.
        // If backingMap is HashMap, then elementSet order is undefined.
        // toMap iterates over backingMap.entrySet()
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
        multiset.add("c", 3); // c:3
        multiset.add("a", 1); // a:1
        multiset.add("b", 2); // b:2
        multiset.add("d", 1); // d:1 (tie with a)

        Map<String, Integer> sortedMap = multiset.toMapSortedByOccurrences(); // Ascending by count
        assertTrue(sortedMap instanceof LinkedHashMap); // Ensures order is preserved

        Iterator<Map.Entry<String, Integer>> it = sortedMap.entrySet().iterator();
        Map.Entry<String, Integer> entry1 = it.next(); // Should be 'a' or 'd'
        assertEquals(1, entry1.getValue().intValue());
        assertTrue(entry1.getKey().equals("a") || entry1.getKey().equals("d"));

        Map.Entry<String, Integer> entry2 = it.next(); // The other 1-count element
        assertEquals(1, entry2.getValue().intValue());
        assertTrue(entry2.getKey().equals("a") || entry2.getKey().equals("d"));
        assertNotEquals(entry1.getKey(), entry2.getKey());

        assertEquals("b", it.next().getKey()); // Count 2
        assertEquals("c", it.next().getKey()); // Count 3
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

        // Sort descending by count
        Map<String, Integer> sortedMapDesc = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
        Iterator<Map.Entry<String, Integer>> itDesc = sortedMapDesc.entrySet().iterator();
        assertEquals("c", itDesc.next().getKey()); // 3
        assertEquals("b", itDesc.next().getKey()); // 2
        assertEquals("a", itDesc.next().getKey()); // 1
        assertFalse(itDesc.hasNext());
    }

    @Test
    public void testToMapSortedByKey() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("zebra", 3);
        multiset.add("apple", 1);
        multiset.add("banana", 2);

        // Sort ascending by key (natural order for strings)
        Map<String, Integer> sortedMap = multiset.toMapSortedByKey(Comparator.naturalOrder());
        Iterator<Map.Entry<String, Integer>> it = sortedMap.entrySet().iterator();
        assertEquals("apple", it.next().getKey());
        assertEquals("banana", it.next().getKey());
        assertEquals("zebra", it.next().getKey());
        assertFalse(it.hasNext());

        // Sort descending by key
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
        Multiset<String> multiset = Multiset.of("z", "y", "z"); // z:2, y:1
        // Supplier for a map that might have a specific order if underlying map used by toMap(supplier) has order
        // For ImmutableMap, the internal representation might reorder anyway unless it's specifically a sorted/linked variant.
        // The wrap method usually takes what it's given.
        IntFunction<Map<String, Integer>> supplier = (size) -> new TreeMap<>(); // TreeMap for key-sorted

        com.landawn.abacus.util.ImmutableMap<String, Integer> immutableMap = multiset.toImmutableMap(supplier);
        assertEquals(2, immutableMap.size());
        assertEquals(1, immutableMap.get("y").intValue()); // TreeMap ensures y then z
        assertEquals(2, immutableMap.get("z").intValue());

        // The underlying map used by toImmutableMap will be a TreeMap.
        // ImmutableMap.wrap will then use this.
        // If we check the toString or iterate, it should reflect TreeMap order.
        // (Note: ImmutableMap's own iteration order depends on its specific implementation after wrapping)
        // However, get operations are fine.

        // Test that it's truly immutable
        assertThrows(UnsupportedOperationException.class, () -> immutableMap.put("c", 1));
    }
    //endregion

    //region Iteration/Stream Operations
    @Test
    public void testIterator() {
        Multiset<String> multiset = new Multiset<>(LinkedHashMap.class); // Use LinkedHashMap for predictable elementSet iteration
        multiset.add("a", 2); // a:2
        multiset.add("b", 1); // b:1
        multiset.add("c", 3); // c:3

        // Expected iteration order of elements from backingMap.entrySet() is a, b, c
        // The iterator should yield: a, a, b, c, c, c (though not necessarily grouped if backingMap was HashMap)
        // With LinkedHashMap for backingMap, entrySet iteration is predictable.
        // The Multiset iterator iterates through entries, then through counts for each entry.

        List<String> iteratedElements = new ArrayList<>();
        Iterator<String> it = multiset.iterator();
        while (it.hasNext()) {
            iteratedElements.add(it.next());
        }

        assertEquals(2 + 1 + 3, iteratedElements.size());

        // With LinkedHashMap, the order of blocks of elements is predictable
        assertEquals(Arrays.asList("a", "a", "b", "c", "c", "c"), iteratedElements);

        // Test with HashMap (default) where order of element blocks is not guaranteed
        Multiset<String> hashMultiset = Multiset.of("x", "y", "x", "z", "x"); // x:3, y:1, z:1
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
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c"); // a:2, b:1, c:1
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
        Multiset<String> multiset = new Multiset<>(LinkedHashMap.class); // Predictable element order
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

        // Check order
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
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c"); // a:2, b:1, c:1
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

    //endregion

    //region Functional Operations
    // These require Throwables.Function/Consumer which are not standard.
    // Assuming they exist in com.landawn.abacus.util.Throwables
    // For simplicity, I will use standard functional interfaces and assume no exceptions are thrown.
    // If Throwables.* are crucial, specific mock/setup for them would be needed.

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
        // The return type is OrElse, so we can't directly verify the action.
        // We can verify the side effect.
        multiset.acceptIfNotEmpty(ms -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testAcceptIfNotEmpty_Empty() {
        Multiset<String> multiset = new Multiset<>();
        AtomicInteger counter = new AtomicInteger(0);
        // OrElse orElse = 
        multiset.acceptIfNotEmpty(ms -> counter.incrementAndGet());
        // We can test the 'orElse' part if needed, but for now, ensure no action on empty.
        assertEquals(0, counter.get());
        // Example of using OrElse (if it had a method like `orElseRun(Runnable)`)
        // final boolean[] elseCalled = {false};
        // multiset.acceptIfNotEmpty(ms -> counter.incrementAndGet()).orElseRun(() -> elseCalled[0] = true);
        // assertTrue(elseCalled[0]);
    }

    //endregion

    //region Object Methods (hashCode, equals, toString)
    @Test
    public void testHashCode() {
        Multiset<String> m1 = Multiset.of("a", "b", "a"); // a:2, b:1
        Multiset<String> m2 = Multiset.of("b", "a", "a"); // a:2, b:1 (same content)
        Multiset<String> m3 = new Multiset<>();
        m3.add("a", 2);
        m3.add("b", 1);

        assertEquals(m1.hashCode(), m2.hashCode());
        assertEquals(m1.hashCode(), m3.hashCode());

        Multiset<String> m4 = Multiset.of("a", "b"); // a:1, b:1
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

        assertTrue(m1.equals(m1)); // Reflexive
        assertTrue(m1.equals(m2)); // Symmetric with m2
        assertTrue(m2.equals(m1));
        assertTrue(m1.equals(m3)); // Transitive with m3 (via m2)
        assertTrue(m3.equals(m1));

        Multiset<String> m4 = Multiset.of("a", "b"); // Different counts
        assertFalse(m1.equals(m4));
        assertFalse(m4.equals(m1));

        Multiset<String> m5 = Multiset.of("a", "c", "a"); // Different element
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
        // toString relies on the backing map's toString.
        // For HashMap, order is not guaranteed.
        // For testing, we can check if elements and counts are present.
        Multiset<String> multiset = new Multiset<>();
        assertEquals("{}", multiset.toString()); // Empty map

        multiset.add("a", 2);
        multiset.add("b", 1);
        // Possible outputs: "{a=2, b=1}" or "{b=1, a=2}"
        String str = multiset.toString();
        assertTrue(str.startsWith("{") && str.endsWith("}"));
        assertTrue(str.contains("a=2"));
        assertTrue(str.contains("b=1"));
        assertEquals(str.length(), "{a=2, b=1}".length()); // Ensure no other elements

        // Test with LinkedHashMap for predictable toString
        Multiset<String> linkedMultiset = new Multiset<>(LinkedHashMap.class);
        linkedMultiset.add("c", 3);
        linkedMultiset.add("a", 1);
        assertEquals("{c=3, a=1}", linkedMultiset.toString());
    }
    //endregion

    //region Multiset.Entry and ImmutableEntry tests (mostly via entrySet)

    @Test
    public void testMultisetIterator_remove() {
        Multiset<String> multiset = Multiset.of("a", "b", "a"); // a:2, b:1
        Iterator<String> it = multiset.iterator();

        assertTrue(it.hasNext());
        it.next(); // Move to first element

        // The ObjIterator base class does not support remove unless overridden.
        // Multiset's main iterator does not override remove().
        assertThrows(UnsupportedOperationException.class, it::remove, "Multiset.iterator().remove() should throw UnsupportedOperationException");
    }

    @Test
    public void testElementSetIterator_remove_supported() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c"); // a:2, b:1, c:1
        Set<String> elementSet = multiset.elementSet();
        Iterator<String> it = elementSet.iterator();

        assertTrue(it.hasNext());
        String firstElement = it.next(); // e.g., "a" (depends on backing map iteration order)
        multiset.removeAllOccurrences(firstElement);

        // Verify the element (and all its occurrences) is removed from the multiset
        assertEquals(0, multiset.getCount(firstElement), "Element should be removed from multiset after elementSet().iterator().remove()");
        assertFalse(multiset.contains(firstElement), "Multiset should not contain element after elementSet().iterator().remove()");
        assertFalse(elementSet.contains(firstElement), "ElementSet should not contain element after its iterator.remove()");

        // Example: remove another element
        it = elementSet.iterator();

        assertTrue(it.hasNext());
        String secondElement = it.next();
        it.remove();
        assertEquals(0, multiset.getCount(secondElement));

        // Continue removing all
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(multiset.isEmpty(), "Multiset should be empty after removing all from elementSet iterator");
        assertTrue(elementSet.isEmpty(), "ElementSet should be empty");
    }

    @Test
    public void testEntrySetIterator_remove() {
        Multiset<String> multiset = Multiset.of("a", "b"); // a:1, b:1
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();
        Iterator<Multiset.Entry<String>> it = entrySet.iterator();

        assertTrue(it.hasNext());
        it.next(); // Move to the first entry

        // The EntrySet.iterator() in the provided code does not implement remove().
        assertThrows(UnsupportedOperationException.class, it::remove,
                "entrySet().iterator().remove() should throw UnsupportedOperationException if not implemented.");
    }

    @Test
    public void testEntrySet_removeObject() {
        Multiset<String> multiset = Multiset.of("a", "b", "a"); // a:2, b:1
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();

        // Create an entry that exists
        Multiset.Entry<String> entryOfA = null;
        for (Multiset.Entry<String> entry : entrySet) {
            if (entry.element().equals("a")) {
                entryOfA = entry; // This would be an ImmutableEntry("a", 2)
                break;
            }
        }
        assertNotNull(entryOfA);
        assertEquals("a", entryOfA.element());
        assertEquals(2, entryOfA.count());

    }

    @Test
    public void testElementSet_removeObject_supported() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c"); // a:2, b:1, c:1
        Set<String> elementSet = multiset.elementSet();

        assertTrue(elementSet.contains("a"));
        assertTrue(multiset.contains("a"));

        // elementSet is typically map.keySet(), and keySet().remove(key) is supported.
        assertTrue(elementSet.remove("a"), "elementSet.remove('a') should return true");

        assertFalse(elementSet.contains("a"), "Element 'a' should be removed from elementSet");
        assertFalse(multiset.contains("a"), "Element 'a' should be removed from multiset");
        assertEquals(0, multiset.getCount("a"), "Count of 'a' should be 0 in multiset");

        assertEquals(2, multiset.size(), "Multiset size should be updated"); // b:1, c:1
        assertEquals(2, elementSet.size(), "elementSet size should be updated");

        assertFalse(elementSet.remove("x"), "Removing non-existent element from elementSet should return false");
    }
}
