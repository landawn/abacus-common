package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

public class TriIteratorTest extends TestBase {

    // =====================================================================
    // empty()
    // =====================================================================

    @Test
    public void testEmpty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmptyIteratorMethods() {
        TriIterator<String, Integer, Double> empty = TriIterator.empty();

        AtomicInteger count = new AtomicInteger(0);
        empty.forEachRemaining((a, b, c) -> count.incrementAndGet());
        assertEquals(0, count.get());

        ObjIterator<String> mapped = empty.map((a, b, c) -> a + b + c);
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testEmpty_foreachRemainingDoesNothing() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        iter.foreachRemaining((a, b, c) -> {
            Assertions.fail("Should not be called");
        });
    }

    @Test
    public void testToSet_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        Set<Triple<String, Integer, Boolean>> set = iter.toSet();

        assertTrue(set.isEmpty());
    }

    @Test
    public void testToCollection_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        ArrayList<Triple<String, Integer, Boolean>> collection = iter.toCollection(ArrayList::new);

        assertTrue(collection.isEmpty());
    }

    @Test
    public void testToImmutableList_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        ImmutableList<Triple<String, Integer, Boolean>> immutableList = iter.toImmutableList();

        assertTrue(immutableList.isEmpty());
    }

    @Test
    public void testToImmutableSet_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        ImmutableSet<Triple<String, Integer, Boolean>> immutableSet = iter.toImmutableSet();

        assertTrue(immutableSet.isEmpty());
    }

    @Test
    public void testCount_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        assertEquals(0, iter.count());
    }

    @Test
    public void testEmpty_next_throwsException() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testEmpty_nextAction_throwsException() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        assertThrows(NoSuchElementException.class, () -> {
            iter.next((a, b, c) -> {
            });
        });
    }

    @Test
    public void testRemove_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    // =====================================================================
    // generate(Consumer)
    // =====================================================================

    @Test
    public void testGenerateWithConsumer() {
        List<Triple<Integer, String, Double>> results = new ArrayList<>();
        Consumer<Triple<Integer, String, Double>> output = triple -> {
            triple.set(results.size(), "item" + results.size(), results.size() * 1.5);
        };

        TriIterator<Integer, String, Double> iter = TriIterator.generate(output);

        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            Triple<Integer, String, Double> triple = iter.next();
            assertEquals(i, triple.left());
            assertEquals("item" + i, triple.middle());
            assertEquals(i * 1.5, triple.right());
            results.add(triple);
        }
    }

    @Test
    public void testGenerate_consumer_limited() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, Integer, Integer> iter = TriIterator.<Integer, Integer, Integer> generate(triple -> {
            int n = counter.getAndIncrement();
            triple.setLeft(n);
            triple.setMiddle(n * 2);
            triple.setRight(n * 3);
        }).limit(5);

        int count = 0;
        while (iter.hasNext()) {
            Triple<Integer, Integer, Integer> t = iter.next();
            assertEquals(count, t.left().intValue());
            assertEquals(count * 2, t.middle().intValue());
            assertEquals(count * 3, t.right().intValue());
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void testGenerate_infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(t -> {
            int val = counter.getAndIncrement();
            t.set(val, val + 1, val + 2);
        });

        assertTrue(iter.hasNext());
        assertEquals(Triple.of(0, 1, 2), iter.next());
        assertEquals(Triple.of(1, 2, 3), iter.next());
    }

    @Test
    public void testGenerate_withCondition() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(() -> counter.get() < 3, t -> {
            int val = counter.getAndIncrement();
            t.set(val, val, val);
        });

        List<Triple<Integer, Integer, Integer>> result = iter.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(Triple.of(0, 0, 0), Triple.of(1, 1, 1), Triple.of(2, 2, 2)), result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerateMapFunction() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        Consumer<Triple<Integer, Integer, Integer>> output = triple -> {
            int val = counter.getAndIncrement();
            triple.set(val, val * 10, val * 100);
        };

        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(hasNext, output);

        ObjIterator<Integer> sumIter = iter.map((a, b, c) -> a + b + c);

        List<Integer> sums = new ArrayList<>();
        sumIter.forEachRemaining(sums::add);

        assertEquals(3, sums.size());
        assertEquals(Integer.valueOf(0), sums.get(0));
        assertEquals(Integer.valueOf(111), sums.get(1));
        assertEquals(Integer.valueOf(222), sums.get(2));
    }

    // =====================================================================
    // generate(int, int, IntObjConsumer)
    // =====================================================================

    @Test
    public void testGenerateWithIndexRange() {
        IntObjConsumer<Triple<String, Integer, Boolean>> output = (index, triple) -> {
            triple.set("idx" + index, index * 10, index % 2 == 0);
        };

        TriIterator<String, Integer, Boolean> iter = TriIterator.generate(2, 5, output);

        List<Triple<String, Integer, Boolean>> results = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> results.add(Triple.of(a, b, c)));

        assertEquals(3, results.size());
        assertEquals("idx2", results.get(0).left());
        assertEquals(20, results.get(0).middle());
        assertTrue(results.get(0).right());
        assertEquals("idx3", results.get(1).left());
        assertEquals(30, results.get(1).middle());
        assertFalse(results.get(1).right());
        assertEquals("idx4", results.get(2).left());
        assertEquals(40, results.get(2).middle());
        assertTrue(results.get(2).right());
    }

    @Test
    public void testGenerate_withIndex() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(1, 4, (i, t) -> t.set(i, i * 2, i * 3));
        assertEquals(Arrays.asList(Triple.of(1, 2, 3), Triple.of(2, 4, 6), Triple.of(3, 6, 9)), iter.toList());
    }

    @Test
    public void testCount_afterPartialConsumption() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(0, 5, (i, t) -> t.set(i, i, i));
        iter.next();
        iter.next();
        assertEquals(3, iter.count());
    }

    @Test
    public void testCount_exhaustsIterator() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(0, 3, (i, t) -> t.set(i, i, i));
        iter.count();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testChainedOperations() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(0, 10, (i, t) -> t.set(i, i, i));
        List<Triple<Integer, Integer, Integer>> result = iter.skip(2).limit(5).filter((a, b, c) -> a % 2 == 0).toList();
        assertEquals(Arrays.asList(Triple.of(2, 2, 2), Triple.of(4, 4, 4), Triple.of(6, 6, 6)), result);
    }

    @Test
    public void testGenerateEdgeCases() {
        BooleanSupplier alwaysFalse = () -> false;
        Consumer<Triple<String, Integer, Double>> output = triple -> {
            fail("Should never be called");
        };

        TriIterator<String, Integer, Double> iter = TriIterator.generate(alwaysFalse, output);
        assertFalse(iter.hasNext());

        IntObjConsumer<Triple<Integer, Integer, Integer>> indexOutput = (idx, triple) -> {
            triple.set(idx, idx * 2, idx * 3);
        };

        TriIterator<Integer, Integer, Integer> indexIter = TriIterator.generate(0, 1, indexOutput);
        assertTrue(indexIter.hasNext());
        Triple<Integer, Integer, Integer> single = indexIter.next();
        assertEquals(Integer.valueOf(0), single.left());
        assertEquals(Integer.valueOf(0), single.middle());
        assertEquals(Integer.valueOf(0), single.right());
        assertFalse(indexIter.hasNext());
    }

    @Test
    public void testGenerate_emptyRange() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(1, 1, (i, t) -> t.set(i, i, i));
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_consumer_null() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null));
    }

    // =====================================================================
    // generate(BooleanSupplier, Consumer)
    // =====================================================================

    @Test
    public void testGenerateWithBooleanSupplierAndConsumer() {
        MutableInt counter = MutableInt.of(0);
        BooleanSupplier hasNext = () -> counter.value() < 3;
        Consumer<Triple<Integer, String, Boolean>> output = triple -> {
            triple.set(counter.value(), "value" + counter.value(), counter.value() % 2 == 0);
            counter.increment();
        };

        TriIterator<Integer, String, Boolean> iter = TriIterator.generate(hasNext, output);

        List<Triple<Integer, String, Boolean>> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(3, results.size());
        assertEquals(0, results.get(0).left());
        assertEquals("value0", results.get(0).middle());
        assertTrue(results.get(0).right());
        assertEquals(1, results.get(1).left());
        assertEquals("value1", results.get(1).middle());
        assertFalse(results.get(1).right());

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testGenerate_booleanSupplierAndConsumer_nullArgs() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null, triple -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(() -> true, null));
    }

    @Test
    public void testGenerate_intRange_invalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> TriIterator.generate(5, 2, (idx, triple) -> {
        }));
    }

    @Test
    public void testGenerate_intRange_nullOutput() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(0, 5, null));
    }

    @Test
    public void testArgumentValidation() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null, (Consumer<Triple<String, Integer, Double>>) null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().skip(-1));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().limit(-1));

        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(2, 5, (IntObjConsumer<Triple<String, Integer, Double>>) null));
    }

    // =====================================================================
    // zip(A[], B[], C[])
    // =====================================================================

    @Test
    public void testZipArrays() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> results = iter.toList();
        assertEquals(3, results.size());
        assertEquals("a", results.get(0).left());
        assertEquals(1, results.get(0).middle());
        assertEquals(1.1, results.get(0).right());
        assertEquals("c", results.get(2).left());
        assertEquals(3, results.get(2).middle());
        assertEquals(3.3, results.get(2).right());
    }

    @Test
    public void testZipArrays_differentLengths() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        assertTrue(iter.hasNext());
        Triple<String, Integer, Boolean> t = iter.next();
        assertEquals("Alice", t.left());
        assertEquals(25, t.middle());
        assertTrue(t.right());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipArrays_shortestWins() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2 }, new String[] { "a", "b", "c" }, new Boolean[] { true });
        assertTrue(iter.hasNext());
        assertEquals(Triple.of(1, "a", true), iter.next());
        assertFalse(iter.hasNext());
    }

    // =====================================================================
    // zip(A[], B[], C[], A, B, C)
    // =====================================================================

    @Test
    public void testZipArraysWithDefaults() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active, "Unknown", 0, false);

        List<Triple<String, Integer, Boolean>> result = iter.toList();
        assertEquals(3, result.size());

        assertEquals("Alice", result.get(0).left());
        assertEquals("Bob", result.get(1).left());
        assertEquals("Unknown", result.get(2).left());

        assertEquals(35, result.get(2).middle().intValue());
        assertFalse(result.get(2).right());
    }

    @Test
    public void testZipArraysWithDefaults_differentLengths() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2, 3, 4 };
        Double[] arr3 = { 1.1 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3, "default", -1, 0.0);

        List<Triple<String, Integer, Double>> results = iter.toList();
        assertEquals(4, results.size());
        assertEquals("default", results.get(2).left());
        assertEquals(3, results.get(2).middle());
        assertEquals(0.0, results.get(2).right());
    }

    @Test
    public void testZipArraysWithDefaults_mixed() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1 }, new String[] { "a", "b" }, new Boolean[] {}, -1, "z", false);
        assertEquals(Triple.of(1, "a", false), iter.next());
        assertEquals(Triple.of(-1, "b", false), iter.next());
        assertFalse(iter.hasNext());
    }

    // =====================================================================
    // zip(Iterable, Iterable, Iterable)
    // =====================================================================

    @Test
    public void testZipIterables() {
        List<String> list1 = Arrays.asList("x", "y", "z");
        List<Integer> list2 = Arrays.asList(10, 20, 30);
        List<Boolean> list3 = Arrays.asList(true, false, true);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(list1, list2, list3);

        List<Triple<String, Integer, Boolean>> results = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> results.add(Triple.of(a, b, c)));

        assertEquals(3, results.size());
        assertEquals("y", results.get(1).left());
        assertEquals(20, results.get(1).middle());
        assertFalse(results.get(1).right());
    }

    @Test
    public void testZipIterables_shortestWins() {
        TriIterator<Integer, String, Double> iter = TriIterator.zip(Arrays.asList(1, 2), Arrays.asList("a"), Arrays.asList(1.1, 2.2));
        assertTrue(iter.hasNext());
        assertEquals(Triple.of(1, "a", 1.1), iter.next());
        assertFalse(iter.hasNext());
    }

    // =====================================================================
    // zip(Iterable, Iterable, Iterable, A, B, C)
    // =====================================================================

    @Test
    public void testZipIterablesWithDefaults() {
        List<String> names = CommonUtil.toList("Alice");
        List<Integer> ages = CommonUtil.toList(25, 30);
        List<Boolean> active = CommonUtil.toList();

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active, "Unknown", 0, false);

        List<Triple<String, Integer, Boolean>> result = iter.toList();
        assertEquals(2, result.size());

        assertEquals("Alice", result.get(0).left());
        assertEquals("Unknown", result.get(1).left());
    }

    @Test
    public void testZipIterablesWithDefaults_mixed() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(Arrays.asList(1), Arrays.asList("a", "b"), new ArrayList<>(), 99, "nn", true);
        assertEquals(Triple.of(1, "a", true), iter.next());
        assertEquals(Triple.of(99, "b", true), iter.next());
        assertFalse(iter.hasNext());
    }

    // =====================================================================
    // zip(Iterator, Iterator, Iterator)
    // =====================================================================

    @Test
    public void testZipIterators() {
        Iterator<String> iter1 = Arrays.asList("p", "q", "r").iterator();
        Iterator<Integer> iter2 = Arrays.asList(100, 200, 300).iterator();
        Iterator<Character> iter3 = Arrays.asList('A', 'B', 'C').iterator();

        TriIterator<String, Integer, Character> triIter = TriIterator.zip(iter1, iter2, iter3);

        assertTrue(triIter.hasNext());
        Triple<String, Integer, Character> first = triIter.next();
        assertEquals("p", first.left());
        assertEquals(100, first.middle());
        assertEquals('A', first.right());

        ObjIterator<String> mapped = triIter.map((a, b, c) -> a + b + c);
        assertEquals("q200B", mapped.next());
        assertEquals("r300C", mapped.next());
        assertFalse(mapped.hasNext());
    }

    // =====================================================================
    // toSet() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testToSet() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2 }, new String[] { "a", "b" }, new Boolean[] { true, false });
        Set<Triple<Integer, String, Boolean>> set = iter.toSet();

        assertEquals(2, set.size());
        assertTrue(set.contains(Triple.of(1, "a", true)));
        assertTrue(set.contains(Triple.of(2, "b", false)));
    }

    @Test
    public void testToSet_duplicates() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 1 }, new Integer[] { 2, 2 }, new Integer[] { 3, 3 });
        Set<Triple<Integer, Integer, Integer>> set = iter.toSet();

        assertEquals(1, set.size());
    }

    // =====================================================================
    // toCollection(Supplier) (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testToCollection() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new String[] { "a", "b", "c" },
                new Boolean[] { true, false, true });
        LinkedList<Triple<Integer, String, Boolean>> collection = iter.toCollection(LinkedList::new);

        assertEquals(3, collection.size());
        assertEquals(Triple.of(1, "a", true), collection.getFirst());
        assertEquals(Triple.of(3, "c", true), collection.getLast());
    }

    // =====================================================================
    // toImmutableList() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testToImmutableList() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2 }, new String[] { "a", "b" }, new Boolean[] { true, false });
        ImmutableList<Triple<Integer, String, Boolean>> immutableList = iter.toImmutableList();

        assertEquals(2, immutableList.size());
        assertEquals(Triple.of(1, "a", true), immutableList.get(0));
        assertEquals(Triple.of(2, "b", false), immutableList.get(1));
    }

    // =====================================================================
    // toImmutableSet() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testToImmutableSet() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2 }, new String[] { "a", "b" }, new Boolean[] { true, false });
        ImmutableSet<Triple<Integer, String, Boolean>> immutableSet = iter.toImmutableSet();

        assertEquals(2, immutableSet.size());
        assertTrue(immutableSet.contains(Triple.of(1, "a", true)));
        assertTrue(immutableSet.contains(Triple.of(2, "b", false)));
    }

    // =====================================================================
    // count() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testCount() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new String[] { "a", "b", "c" },
                new Boolean[] { true, false, true });
        assertEquals(3, iter.count());
    }

    @Test
    public void testCombinedOperations() {
        String[] arr1 = { "a", "b", "c", "d", "e", "f" };
        Integer[] arr2 = { 1, 2, 3, 4, 5, 6 };
        Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6 };

        TriPredicate<String, Integer, Double> predicate = (s, i, d) -> i % 2 == 0;

        List<Triple<String, Integer, Double>> result = TriIterator.zip(arr1, arr2, arr3).skip(1).limit(4).filter(predicate).toList();

        assertEquals(2, result.size());
        assertEquals("b", result.get(0).left());
        assertEquals(Integer.valueOf(2), result.get(0).middle());
        assertEquals("d", result.get(1).left());
        assertEquals(Integer.valueOf(4), result.get(1).middle());
    }

    @Test
    public void testZipArrays_nullArray() {
        String[] names = { "Alice" };
        Integer[] ages = { 25 };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, (Boolean[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipArrays_withNullElements() {
        String[] arr1 = { "a", null, "c" };
        Integer[] arr2 = { 1, 2, null };
        Double[] arr3 = { null, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> results = iter.toList();
        assertEquals(3, results.size());
        assertNull(results.get(0).right());
        assertNull(results.get(1).left());
        assertNull(results.get(2).middle());
    }

    @Test
    public void testZipArrays_nullAndEmpty() {
        assertFalse(TriIterator.zip(new Integer[] { 1 }, null, new Boolean[] { true }).hasNext());
        assertFalse(TriIterator.zip(new Integer[] {}, new String[] { "a" }, new Boolean[] { true }).hasNext());
        assertFalse(TriIterator.zip(new Integer[] {}, new String[] {}, new Boolean[] {}).hasNext());
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip((Integer[]) null, (String[]) null, (Boolean[]) null, 1, "a", true);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipIterables_nullIterable() {
        List<String> names = CommonUtil.toList("Alice");
        List<Integer> ages = CommonUtil.toList(25);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, (Iterable<Boolean>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipIterables_withNulls() {
        List<String> list1 = Arrays.asList("a", "b");
        List<Integer> list2 = null;
        List<Double> list3 = Arrays.asList(1.0, 2.0);

        TriIterator<String, Integer, Double> iter = TriIterator.zip(list1, list2, list3);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipIterables_emptyCollections() {
        List<String> empty1 = Collections.emptyList();
        List<Integer> empty2 = Collections.emptyList();
        List<Double> empty3 = Collections.emptyList();

        TriIterator<String, Integer, Double> iter = TriIterator.zip(empty1, empty2, empty3);
        assertFalse(iter.hasNext());

        List<String> list1 = Arrays.asList("a", "b");
        List<Integer> list2 = Collections.emptyList();
        List<Double> list3 = Arrays.asList(1.1, 2.2);

        iter = TriIterator.zip(list1, list2, list3);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipIterators_nullIterator() {
        List<String> names = CommonUtil.toList("Alice");
        List<Integer> ages = CommonUtil.toList(25);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names.iterator(), ages.iterator(), (Iterator<Boolean>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipIterators_withNullIterators() {
        Iterator<String> iter1 = null;
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Double> iter3 = Arrays.asList(1.1, 2.2).iterator();

        TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3);

        assertFalse(triIter.hasNext());
    }

    // =====================================================================
    // zip(Iterator, Iterator, Iterator, A, B, C)
    // =====================================================================

    @Test
    public void testZipIteratorsWithDefaults() {
        Iterator<String> iter1 = Arrays.asList("a").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();
        Iterator<Double> iter3 = Arrays.asList(1.1, 2.2).iterator();

        TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3, "missing", 0, 0.0);

        List<Triple<String, Integer, Double>> results = triIter.toList();
        assertEquals(3, results.size());
        assertEquals("missing", results.get(1).left());
        assertEquals(2, results.get(1).middle());
        assertEquals(2.2, results.get(1).right());
        assertEquals("missing", results.get(2).left());
        assertEquals(3, results.get(2).middle());
        assertEquals(0.0, results.get(2).right());
    }

    @Test
    public void testZipIteratorsWithDefaults_allEmpty() {
        Iterator<String> iter1 = Collections.<String> emptyList().iterator();
        Iterator<Integer> iter2 = Collections.<Integer> emptyList().iterator();
        Iterator<Double> iter3 = Collections.<Double> emptyList().iterator();

        TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3, "default", -1, -1.0);
        assertFalse(triIter.hasNext());
    }

    @Test
    public void testMultipleHasNextCalls() {
        String[] arr1 = { "a" };
        Integer[] arr2 = { 1 };
        Double[] arr3 = { 1.1 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());

        Triple<String, Integer, Double> element = iter.next();
        assertEquals("a", element.left());

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

    // =====================================================================
    // remove() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testRemove() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(new String[] { "a" }, new Integer[] { 1 }, new Boolean[] { true });
        assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    @Test
    public void testToImmutableList_isImmutable() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1 }, new String[] { "a" }, new Boolean[] { true });
        ImmutableList<Triple<Integer, String, Boolean>> immutableList = iter.toImmutableList();

        assertThrows(UnsupportedOperationException.class, () -> immutableList.add(Triple.of(2, "b", false)));
    }

    @Test
    public void testToImmutableSet_isImmutable() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1 }, new String[] { "a" }, new Boolean[] { true });
        ImmutableSet<Triple<Integer, String, Boolean>> immutableSet = iter.toImmutableSet();

        assertThrows(UnsupportedOperationException.class, () -> immutableSet.add(Triple.of(2, "b", false)));
    }

    @Test
    public void testIteratorExhaustion() {
        String[] arr1 = { "a" };
        Integer[] arr2 = { 1 };
        Double[] arr3 = { 1.1 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        iter.next();

        assertFalse(iter.hasNext());

        try {
            iter.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((a, b, c) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testUnzipIterable_complexObjects() {
        List<Map<String, Object>> source = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "Alice");
        map1.put("age", 25);
        map1.put("score", 95.5);
        source.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Bob");
        map2.put("age", 30);
        map2.put("score", 88.0);
        source.add(map2);

        BiConsumer<Map<String, Object>, Triple<String, Integer, Double>> unzipFunction = (map, triple) -> {
            triple.set((String) map.get("name"), (Integer) map.get("age"), (Double) map.get("score"));
        };

        TriIterator<String, Integer, Double> iter = TriIterator.unzip(source, unzipFunction);

        List<Triple<String, Integer, Double>> result = iter.toList();
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).left());
        assertEquals(Integer.valueOf(25), result.get(0).middle());
        assertEquals(95.5, result.get(0).right(), 0.001);
    }

    @Test
    public void testUnzip_withTripleSource() {
        List<Triple<Integer, String, Boolean>> source = Arrays.asList(Triple.of(1, "a", true), Triple.of(2, "b", false));
        TriIterator<Integer, String, Boolean> iter = TriIterator.unzip(source, (src, target) -> target.set(src.left(), src.middle(), src.right()));
        assertEquals(source, iter.toList());
    }

    // =====================================================================
    // unzip(Iterable, BiConsumer)
    // =====================================================================

    @Test
    public void testUnzipIterable() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        TriIterator<String, Integer, String> iter = TriIterator.unzip(pairs, (pair, triple) -> {
            triple.set(pair.left(), pair.right(), pair.left().toUpperCase());
        });

        List<Triple<String, Integer, String>> results = iter.toList();
        assertEquals(3, results.size());
        assertEquals("b", results.get(1).left());
        assertEquals(2, results.get(1).middle());
        assertEquals("B", results.get(1).right());
    }

    @Test
    public void testUnzipIterable_null() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.unzip((Iterable<String>) null, (item, triple) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzipIterable_empty() {
        TriIterator<Object, Object, Object> iter = TriIterator.unzip(Collections.emptyList(), (src, target) -> {
        });
        assertFalse(iter.hasNext());
    }

    // =====================================================================
    // unzip(Iterator, BiConsumer)
    // =====================================================================

    @Test
    public void testUnzipIterator() {
        Iterator<String> stringIter = Arrays.asList("hello", "world", "test").iterator();

        TriIterator<Character, Integer, String> iter = TriIterator.unzip(stringIter, (str, triple) -> {
            triple.set(str.charAt(0), str.length(), str.toUpperCase());
        });

        assertTrue(iter.hasNext());
        Triple<Character, Integer, String> first = iter.next();
        assertEquals('h', first.left());
        assertEquals(5, first.middle());
        assertEquals("HELLO", first.right());

        List<Triple<Character, Integer, String>> remaining = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> remaining.add(Triple.of(a, b, c)));
        assertEquals(2, remaining.size());
    }

    @Test
    public void testUnzipIterator_null() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.unzip((Iterator<String>) null, (item, triple) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void testNextActionThrowsException() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1 }, new Integer[] { 1 }, new Integer[] { 1 });
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    // =====================================================================
    // forEachRemaining(Consumer) (deprecated)
    // =====================================================================

    @Test
    public void testForEachRemainingConsumerDeprecated() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> collected = new ArrayList<>();
        Consumer<Triple<String, Integer, Double>> consumer = collected::add;

        iter.next();
        iter.forEachRemaining(consumer);

        assertEquals(2, collected.size());
        assertEquals("b", collected.get(0).left());
        assertEquals("c", collected.get(1).left());
    }

    // =====================================================================
    // forEachRemaining(TriConsumer)
    // =====================================================================

    @Test
    public void testForEachRemaining_triConsumer() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        List<String> results = new ArrayList<>();
        iter.forEachRemaining((name, age, isActive) -> results.add(name + ":" + age + ":" + isActive));

        assertEquals(3, results.size());
        assertEquals("Alice:25:true", results.get(0));
    }

    @Test
    public void testForEachRemaining() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.generate(0, 5, (i, triple) -> {
            triple.set(i, "for" + i, i % 2 == 1);
        });

        iter.next();
        iter.next();

        List<String> results = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> results.add(a + "-" + b + "-" + c));

        assertEquals(3, results.size());
        assertEquals("2-for2-false", results.get(0));
        assertEquals("3-for3-true", results.get(1));
        assertEquals("4-for4-false", results.get(2));
    }

    @Test
    public void testForEachRemaining_elements() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 });
        List<Integer> listA = new ArrayList<>();
        List<Integer> listB = new ArrayList<>();
        List<Integer> listC = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> {
            listA.add(a);
            listB.add(b);
            listC.add(c);
        });
        assertEquals(Arrays.asList(1, 2), listA);
        assertEquals(Arrays.asList(3, 4), listB);
        assertEquals(Arrays.asList(5, 6), listC);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForEachRemainingWithNullAction() {
        TriIterator.empty().forEachRemaining(s -> Assertions.fail("Should not be called"));
    }

    // =====================================================================
    // foreachRemaining(Throwables.TriConsumer)
    // =====================================================================

    @Test
    public void testForeachRemaining_throwable() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30 };
        Boolean[] active = { true, false };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        List<String> results = new ArrayList<>();
        iter.foreachRemaining((name, age, isActive) -> results.add(name + ":" + age));

        assertEquals(2, results.size());
    }

    @Test
    public void testForeachRemaining_throwsException() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1 }, new Integer[] { 1 }, new Integer[] { 1 });
        assertThrows(IOException.class, () -> iter.foreachRemaining((a, b, c) -> {
            throw new IOException("test");
        }));
    }

    @Test
    public void testForeachRemainingWithThrowables() throws IOException {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<String> results = new ArrayList<>();

        Throwables.TriConsumer<String, Integer, Double, IOException> action = (s, i, d) -> {
            if (i == 2) {
                throw new IOException("Test exception at 2");
            }
            results.add(s + i + ":" + d);
        };

        try {
            iter.foreachRemaining(action);
            fail("Should throw IOException");
        } catch (IOException e) {
            assertEquals("Test exception at 2", e.getMessage());
            assertEquals(1, results.size());
            assertEquals("a1:1.1", results.get(0));
        }
    }

    @Test
    public void testForeachRemaining() throws Exception {
        TriIterator<String, Integer, Double> iter = TriIterator.generate(0, 4, (i, triple) -> {
            triple.set("each" + i, i * 2, i * 0.25);
        });

        iter.next();

        List<Triple<String, Integer, Double>> results = new ArrayList<>();
        iter.foreachRemaining((a, b, c) -> results.add(Triple.of(a, b, c)));

        assertEquals(3, results.size());
        assertEquals("each1", results.get(0).left());
        assertEquals(2, results.get(0).middle());
        assertEquals(0.25, results.get(0).right());
    }

    // =====================================================================
    // skip(long)
    // =====================================================================

    @Test
    public void testSkip() {
        Integer[] nums = { 1, 2, 3, 4, 5 };
        String[] strs = { "a", "b", "c", "d", "e" };
        Boolean[] flags = { true, false, true, false, true };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(2);

        List<Triple<Integer, String, Boolean>> result = iter.toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).left().intValue());
    }

    @Test
    public void testSkip_moreThanAvailable() {
        Integer[] nums = { 1, 2 };
        String[] strs = { "a", "b" };
        Boolean[] flags = { true, false };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(10);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_detailed() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        TriIterator<Integer, Integer, Integer> skipped = iter.skip(2);
        assertTrue(skipped.hasNext());
        assertEquals(Triple.of(3, 6, 9), skipped.next());
        assertFalse(skipped.hasNext());

        iter = TriIterator.zip(new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 });
        skipped = iter.skip(5);
        assertFalse(skipped.hasNext());
    }

    // =====================================================================
    // Complex / Integration Tests
    // =====================================================================

    @Test
    public void testSkipLimitFilter() {
        Integer[] nums = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        String[] strs = new String[10];
        Boolean[] flags = new Boolean[10];
        for (int i = 0; i < 10; i++) {
            strs[i] = "str" + i;
            flags[i] = i % 2 == 0;
        }

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(2).limit(5).filter((num, str, flag) -> flag);

        List<Triple<Integer, String, Boolean>> result = iter.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSkipAndLimitCombinations() {
        String[] arr1 = { "a", "b", "c", "d", "e" };
        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).skip(0);
        assertEquals(5, iter.toList().size());

        iter = TriIterator.zip(arr1, arr2, arr3).limit(10);
        assertEquals(5, iter.toList().size());

        iter = TriIterator.zip(arr1, arr2, arr3).skip(10).limit(5);
        assertEquals(0, iter.toList().size());
    }

    @Test
    public void testSkip_zero() {
        Integer[] nums = { 1, 2 };
        String[] strs = { "a", "b" };
        Boolean[] flags = { true, false };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(0);

        List<Triple<Integer, String, Boolean>> result = iter.toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testSkip_negative() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().skip(-1));
    }

    // =====================================================================
    // limit(long)
    // =====================================================================

    @Test
    public void testLimit() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, Integer, Integer> iter = TriIterator.<Integer, Integer, Integer> generate(triple -> {
            int n = counter.getAndIncrement();
            triple.set(n, n * 2, n * 3);
        }).limit(5);

        List<Triple<Integer, Integer, Integer>> result = iter.toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testLimit_moreThanAvailable() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        TriIterator<Integer, Integer, Integer> limited = iter.limit(5);
        assertEquals(3, limited.toList().size());
    }

    @Test
    public void testLimit_zero() {
        Integer[] nums = { 1, 2, 3 };
        String[] strs = { "a", "b", "c" };
        Boolean[] flags = { true, false, true };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).limit(0);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_negative() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().limit(-1));
    }

    // =====================================================================
    // filter(TriPredicate)
    // =====================================================================

    @Test
    public void testFilter() {
        Integer[] nums = { 1, 2, 3, 4, 5 };
        String[] strs = { "a", "bb", "ccc", "dddd", "eeeee" };
        Boolean[] flags = { true, false, true, false, true };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).filter((num, str, flag) -> flag && num > 2);

        List<Triple<Integer, String, Boolean>> result = iter.toList();
        assertEquals(2, result.size());
        assertEquals(3, result.get(0).left().intValue());
        assertEquals(5, result.get(1).left().intValue());
    }

    @Test
    public void testFilter_allMatch() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        TriIterator<Integer, Integer, Integer> filtered = iter.filter((a, b, c) -> (a + b + c) > 0);
        assertEquals(3, filtered.toList().size());
    }

    @Test
    public void testFilter_complexPredicate() {
        String[] arr1 = { "apple", "banana", "cherry", "date", "elderberry" };
        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        Double[] arr3 = { 1.5, 2.5, 3.5, 4.5, 5.5 };

        TriPredicate<String, Integer, Double> complex = (s, i, d) -> (s.length() > 4 && i % 2 == 0) || d > 4.0;

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).filter(complex);

        List<Triple<String, Integer, Double>> result = iter.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFilterAllElementsFiltered() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriPredicate<String, Integer, Double> rejectAll = (s, i, d) -> false;

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).filter(rejectAll);
        assertFalse(iter.hasNext());
        assertEquals(0, iter.toList().size());
    }

    @Test
    public void testFilterMapCombination() {
        String[] arr1 = { "a", "bb", "ccc", "dddd", "eeeee" };
        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        Double[] arr3 = { 1.0, 2.0, 3.0, 4.0, 5.0 };

        ObjIterator<Integer> iter = TriIterator.zip(arr1, arr2, arr3).filter((s, i, d) -> i % 2 == 0).map((s, i, d) -> s.length());

        List<Integer> lengths = new ArrayList<>();
        iter.forEachRemaining(lengths::add);

        assertEquals(2, lengths.size());
        assertEquals(Integer.valueOf(2), lengths.get(0));
        assertEquals(Integer.valueOf(4), lengths.get(1));
    }

    @Test
    public void testFilter_noneMatch() {
        Integer[] nums = { 1, 2 };
        String[] strs = { "a", "b" };
        Boolean[] flags = { false, false };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).filter((num, str, flag) -> flag);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_nullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().filter(null));
    }

    // =====================================================================
    // map(TriFunction)
    // =====================================================================

    @Test
    public void testMap() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        String[] cities = { "NYC", "LA", "Chicago" };

        ObjIterator<String> iter = TriIterator.zip(names, ages, cities).map((name, age, city) -> name + " (" + age + ") from " + city);

        List<String> result = iter.toList();
        assertEquals(3, result.size());
        assertEquals("Alice (25) from NYC", result.get(0));
    }

    @Test
    public void testMap_elements() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 });
        ObjIterator<String> mapped = iter.map((a, b, c) -> a + ":" + b + ":" + c);
        assertEquals(Arrays.asList("1:3:5", "2:4:6"), mapped.toList());
    }

    @Test
    public void testMapAfterFilter() {
        Integer[] nums = { 1, 2, 3, 4 };
        String[] strs = { "a", "b", "c", "d" };
        Boolean[] flags = { true, false, true, false };

        ObjIterator<Integer> iter = TriIterator.zip(nums, strs, flags).filter((num, str, flag) -> flag).map((num, str, flag) -> num * 10);

        List<Integer> result = iter.toList();
        assertEquals(2, result.size());
        assertEquals(10, result.get(0).intValue());
        assertEquals(30, result.get(1).intValue());
    }

    @Test
    public void testMap_after_filter() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(0, 5, (i, t) -> t.set(i, i, i));
        ObjIterator<String> mapped = iter.filter((a, b, c) -> a > 2).map((a, b, c) -> "v" + a);
        assertEquals(Arrays.asList("v3", "v4"), mapped.toList());
    }

    @Test
    public void testMapChaining() {
        String[] arr1 = { "1", "2", "3" };
        Integer[] arr2 = { 10, 20, 30 };
        Double[] arr3 = { 100.0, 200.0, 300.0 };

        ObjIterator<Double> iter = TriIterator.zip(arr1, arr2, arr3).map((s, i, d) -> Integer.parseInt(s) + i + d).map(sum -> sum * 2);

        List<Double> result = new ArrayList<>();
        iter.forEachRemaining(result::add);

        assertEquals(3, result.size());
        assertEquals(222.0, result.get(0), 0.001);
        assertEquals(444.0, result.get(1), 0.001);
        assertEquals(666.0, result.get(2), 0.001);
    }

    @Test
    public void testMapWithNullMapper() {
        TriIterator.empty().stream(null).forEach(s -> Assertions.fail("Should not be called"));
    }

    // =====================================================================
    // stream(TriFunction)
    // =====================================================================

    @Test
    public void testStream() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30 };
        Boolean[] active = { true, false };

        Stream<String> stream = TriIterator.zip(names, ages, active).stream((name, age, isActive) -> name + ":" + age);

        List<String> result = stream.toList();
        assertEquals(2, result.size());
        assertEquals("Alice:25", result.get(0));
    }

    @Test
    public void testStream_count() {
        TriIterator<Integer, String, Double> iter = TriIterator.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(1.1, 2.2));
        long count = iter.stream((a, b, c) -> a).count();
        assertEquals(2, count);
    }

    @Test
    public void testStream_empty() {
        TriIterator<String, Integer, Double> empty = TriIterator.empty();

        Stream<String> stream = empty.stream((a, b, c) -> a + b + c);
        assertEquals(0, stream.count());
    }

    // =====================================================================
    // toArray()
    // =====================================================================

    @Test
    public void testToArray() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30 };
        Boolean[] active = { true, false };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Triple<String, Integer, Boolean>[] array = iter.toArray();
        assertEquals(2, array.length);
        assertEquals("Alice", array[0].left());
    }

    @Test
    public void testToArray_elements() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2 }, new String[] { "a", "b" }, new Boolean[] { true, false });
        Triple<Integer, String, Boolean>[] result = iter.toArray();
        assertEquals(2, result.length);
        assertEquals(Triple.of(1, "a", true), result[0]);
        assertEquals(Triple.of(2, "b", false), result[1]);
    }

    // =====================================================================
    // toArray(T[]) (deprecated)
    // =====================================================================

    @Test
    @SuppressWarnings("deprecation")
    public void testToArrayWithType() {
        String[] names = { "Alice" };
        Integer[] ages = { 25 };
        Boolean[] active = { true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Triple[] array = new Triple[0];
        Triple[] result = iter.toArray(array);
        assertEquals(1, result.length);
    }

    @Test
    public void testToArray_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        Triple<String, Integer, Boolean>[] array = iter.toArray();
        assertEquals(0, array.length);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToArrayWithType_deprecated() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 });
        Triple<Integer, Integer, Integer>[] result = iter.toArray(new Triple[0]);
        assertEquals(2, result.length);

        iter = TriIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 });
        Triple<Integer, Integer, Integer>[] preallocated = new Triple[3];
        preallocated[2] = Triple.of(9, 9, 9);
        iter.toArray(preallocated);
        assertEquals(Triple.of(1, 3, 5), preallocated[0]);
        assertEquals(Triple.of(2, 4, 6), preallocated[1]);
        assertNull(preallocated[2]);
    }

    @Test
    public void testToArrayWithProvidedArray() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        @SuppressWarnings("unchecked")
        Triple<String, Integer, Double>[] smallArray = new Triple[1];
        Triple<String, Integer, Double>[] result = iter.toArray(smallArray);

        assertNotSame(smallArray, result);
        assertEquals(3, result.length);

        iter = TriIterator.zip(arr1, arr2, arr3);
        @SuppressWarnings("unchecked")
        Triple<String, Integer, Double>[] exactArray = new Triple[3];
        result = iter.toArray(exactArray);

        assertSame(exactArray, result);
        assertEquals(3, result.length);

        iter = TriIterator.zip(arr1, arr2, arr3);
        @SuppressWarnings("unchecked")
        Triple<String, Integer, Double>[] largeArray = new Triple[5];
        result = iter.toArray(largeArray);

        assertSame(largeArray, result);
        assertNull(result[3]);
        assertNull(result[4]);
    }

    // =====================================================================
    // toList()
    // =====================================================================

    @Test
    public void testToList() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        List<Triple<String, Integer, Boolean>> list = iter.toList();
        assertEquals(3, list.size());
        assertEquals("Bob", list.get(1).left());
    }

    @Test
    public void testToList_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        List<Triple<String, Integer, Boolean>> list = iter.toList();
        assertTrue(list.isEmpty());
    }

    // =====================================================================
    // toMultiList(Supplier)
    // =====================================================================

    @Test
    public void testToMultiList() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Triple<List<String>, List<Integer>, List<Boolean>> result = iter.toMultiList(ArrayList::new);

        assertEquals(3, result.left().size());
        assertEquals(3, result.middle().size());
        assertEquals(3, result.right().size());

        assertEquals("Alice", result.left().get(0));
        assertEquals(30, result.middle().get(1).intValue());
        assertTrue(result.right().get(2));
    }

    @Test
    public void testToMultiList_customImpl() {
        TriIterator<Integer, String, Double> iter = TriIterator.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(1.1, 2.2));
        Triple<List<Integer>, List<String>, List<Double>> result = iter.toMultiList(LinkedList::new);

        assertTrue(result.left() instanceof LinkedList);
        assertTrue(result.middle() instanceof LinkedList);
        assertTrue(result.right() instanceof LinkedList);
        assertEquals(Arrays.asList(1, 2), result.left());
        assertEquals(Arrays.asList("a", "b"), result.middle());
        assertEquals(Arrays.asList(1.1, 2.2), result.right());
    }

    @Test
    public void testToMultiList_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        Triple<List<String>, List<Integer>, List<Boolean>> result = iter.toMultiList(ArrayList::new);

        assertTrue(result.left().isEmpty());
        assertTrue(result.middle().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    // =====================================================================
    // toMultiSet(Supplier)
    // =====================================================================

    @Test
    public void testToMultiSet() {
        String[] names = { "Alice", "Bob", "Alice" };
        Integer[] ages = { 25, 30, 25 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Triple<Set<String>, Set<Integer>, Set<Boolean>> result = iter.toMultiSet(HashSet::new);

        assertEquals(2, result.left().size());
        assertEquals(2, result.middle().size());
        assertEquals(2, result.right().size());

        assertTrue(result.left().contains("Alice"));
        assertTrue(result.left().contains("Bob"));
    }

    @Test
    public void testToMultiSet_duplicates() {
        TriIterator<Integer, String, String> iter = TriIterator.zip(Arrays.asList(1, 1), Arrays.asList("a", "b"), Arrays.asList("x", "x"));
        Triple<Set<Integer>, Set<String>, Set<String>> result = iter.toMultiSet(HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList(1)), result.left());
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), result.middle());
        assertEquals(new HashSet<>(Arrays.asList("x")), result.right());
    }

    @Test
    public void testToMultiSet_linkedHashSet() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new String[] { "a", "b", "c" },
                new Boolean[] { true, false, true });
        Triple<Set<Integer>, Set<String>, Set<Boolean>> result = iter.toMultiSet(LinkedHashSet::new);

        assertTrue(result.left() instanceof LinkedHashSet);
        assertTrue(result.middle() instanceof LinkedHashSet);
        assertTrue(result.right() instanceof LinkedHashSet);
    }

    @Test
    public void testToMultiSet_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        Triple<Set<String>, Set<Integer>, Set<Boolean>> result = iter.toMultiSet(HashSet::new);

        assertTrue(result.left().isEmpty());
        assertTrue(result.middle().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testComplexChaining() {
        TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 20, (i, triple) -> {
            triple.set(i, "val" + i, i * 0.5);
        });

        List<String> results = iter.skip(2).limit(10).filter((a, b, c) -> a % 3 == 0).map((a, b, c) -> b + "=" + c).toList();

        assertEquals(3, results.size());
        assertEquals("val3=1.5", results.get(0));
        assertEquals("val6=3.0", results.get(1));
        assertEquals("val9=4.5", results.get(2));
    }

    // TODO: Remaining TriIterator$5 gaps are anonymous zip-iterator callback wrappers that are not meaningfully isolatable beyond the public TriIterator API already exercised here.
}
