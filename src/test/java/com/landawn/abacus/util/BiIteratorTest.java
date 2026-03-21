package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

public class BiIteratorTest extends TestBase {

    @Test
    public void testEmpty_forEachRemaining_doesNothing() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((a, b) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    @DisplayName("Test empty iterator operations chaining")
    public void testEmptyChaining() {
        BiIterator<String, Integer> result = BiIterator.<String, Integer> empty().skip(10).limit(5).filter((a, b) -> true);

        assertFalse(result.hasNext());
        assertEquals(0, result.toList().size());
    }

    @Test
    public void testToSet_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Set<Pair<String, Integer>> set = iter.toSet();

        assertTrue(set.isEmpty());
    }

    @Test
    public void testToCollection_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        ArrayList<Pair<String, Integer>> collection = iter.toCollection(ArrayList::new);

        assertTrue(collection.isEmpty());
    }

    @Test
    public void testToImmutableList_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        ImmutableList<Pair<String, Integer>> immutableList = iter.toImmutableList();

        assertTrue(immutableList.isEmpty());
    }

    @Test
    public void testToImmutableSet_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        ImmutableSet<Pair<String, Integer>> immutableSet = iter.toImmutableSet();

        assertTrue(immutableSet.isEmpty());
    }

    @Test
    public void testCount_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        assertEquals(0, iter.count());
    }

    // =====================================================================
    // empty()
    // =====================================================================

    @Test
    public void testEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Assertions.assertNotNull(iter);
        Assertions.assertFalse(iter.hasNext());

        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());

        BiConsumer<String, Integer> biConsumer = (s, i) -> Assertions.fail("Should not be called");
        iter.forEachRemaining(biConsumer);

        ObjIterator<String> mapped = iter.map((s, i) -> s + i);
        Assertions.assertFalse(mapped.hasNext());
    }

    @Test
    public void testEmpty_next_throwsException() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    @DisplayName("Test empty iterator map() returns empty ObjIterator")
    public void testEmptyMap() {
        BiIterator<String, Integer> empty = BiIterator.empty();
        ObjIterator<String> mapped = empty.map((a, b) -> a + b);

        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, () -> mapped.next());
    }

    @Test
    @DisplayName("Test empty iterator foreachRemaining with exception")
    public void testEmptyForeachRemainingException() {
        BiIterator<String, Integer> empty = BiIterator.empty();

        assertDoesNotThrow(() -> {
            empty.foreachRemaining((Throwables.BiConsumer<String, Integer, Exception>) (a, b) -> {
                throw new Exception("Should not be called");
            });
        });
    }

    @Test
    public void testEmptyBiIteratorThrowables() {
        BiIterator<String, Integer> iter = BiIterator.empty();

        Assertions.assertThrows(NoSuchElementException.class, () -> {
            iter.next((k, v) -> {
            });
        });

        iter.foreachRemaining((k, v) -> {
            Assertions.fail("Should not be called");
        });
    }

    @Test
    public void testRemove_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    // =====================================================================
    // of(Map)
    // =====================================================================

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map);

        Assertions.assertTrue(iter.hasNext());
        Pair<String, Integer> first = iter.next();
        Assertions.assertEquals("one", first.left());
        Assertions.assertEquals(Integer.valueOf(1), first.right());

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        Assertions.assertEquals(Arrays.asList("two", "three"), keys);
        Assertions.assertEquals(Arrays.asList(2, 3), values);
    }

    @Test
    public void testOfMap_withElements() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);
        assertTrue(iter.hasNext());

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining((k, v) -> collected.add(Pair.of(k, v)));
        assertEquals(2, collected.size());
    }

    @Test
    @DisplayName("Test of(Map) with populated map")
    public void testOfMapWithElements() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map);

        Set<String> keys = new HashSet<>();
        Set<Integer> values = new HashSet<>();

        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        assertEquals(map.keySet(), keys);
        assertEquals(new HashSet<>(map.values()), values);
    }

    // =====================================================================
    // of(Iterator<Map.Entry>)
    // =====================================================================

    @Test
    public void testOfMapEntryIterator() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map.entrySet().iterator());

        Assertions.assertTrue(iter.hasNext());
        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("a", pair.left());
        Assertions.assertEquals(Integer.valueOf(1), pair.right());

        ObjIterator<String> mapped = iter.map((k, v) -> k + v);
        Assertions.assertTrue(mapped.hasNext());
        Assertions.assertEquals("b2", mapped.next());
        Assertions.assertFalse(mapped.hasNext());
    }

    @Test
    public void testDeprecatedForEachRemaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);

        List<Pair<String, Integer>> pairs = new ArrayList<>();
        Consumer<Pair<String, Integer>> consumer = pairs::add;

        iter.forEachRemaining(consumer);

        Assertions.assertEquals(2, pairs.size());
        Assertions.assertEquals("a", pairs.get(0).left());
        Assertions.assertEquals("b", pairs.get(1).left());
    }

    // =====================================================================
    // toSet() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testToSet() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        Set<Pair<String, Integer>> set = iter.toSet();

        assertEquals(2, set.size());
        assertTrue(set.contains(Pair.of("a", 1)));
        assertTrue(set.contains(Pair.of("b", 2)));
    }

    // =====================================================================
    // toCollection(Supplier) (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testToCollection() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        LinkedList<Pair<String, Integer>> collection = iter.toCollection(LinkedList::new);

        assertEquals(3, collection.size());
        assertEquals(Pair.of("a", 1), collection.getFirst());
        assertEquals(Pair.of("c", 3), collection.getLast());
    }

    // =====================================================================
    // toImmutableList() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testToImmutableList() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        ImmutableList<Pair<String, Integer>> immutableList = iter.toImmutableList();

        assertEquals(2, immutableList.size());
        assertEquals(Pair.of("a", 1), immutableList.get(0));
        assertEquals(Pair.of("b", 2), immutableList.get(1));
    }

    // =====================================================================
    // toImmutableSet() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testToImmutableSet() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        ImmutableSet<Pair<String, Integer>> immutableSet = iter.toImmutableSet();

        assertEquals(2, immutableSet.size());
        assertTrue(immutableSet.contains(Pair.of("a", 1)));
        assertTrue(immutableSet.contains(Pair.of("b", 2)));
    }

    // =====================================================================
    // count() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testCount() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        assertEquals(3, iter.count());
    }

    @Test
    @DisplayName("Test with large dataset")
    public void testLargeDataset() {
        final int size = 10000;
        Map<Integer, String> largeMap = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            largeMap.put(i, "value" + i);
        }

        long count = BiIterator.of(largeMap).filter((k, v) -> k % 100 == 0).map((k, v) -> k).count();

        assertEquals(100, count);
    }

    @Test
    public void testOfMap_null() {
        BiIterator<String, Integer> iter = BiIterator.of((Map<String, Integer>) null);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfMap_empty() {
        Map<String, Integer> map = new HashMap<>();
        BiIterator<String, Integer> iter = BiIterator.of(map);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfMapEntryIterator_null() {
        BiIterator<String, Integer> iter = BiIterator.of((Iterator<Map.Entry<String, Integer>>) null);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfMapEntryIterator_empty() {
        Iterator<Map.Entry<String, Integer>> emptyIter = new HashMap<String, Integer>().entrySet().iterator();
        BiIterator<String, Integer> iter = BiIterator.of(emptyIter);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOfMapEntryIterator_withElements() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map.entrySet().iterator());
        assertTrue(iter.hasNext());

        int count = 0;
        while (iter.hasNext()) {
            Pair<String, Integer> pair = iter.next();
            assertNotNull(pair.left());
            assertNotNull(pair.right());
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test multiple filters chained")
    public void testMultipleFilters() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            map.put("key" + i, i);
        }

        BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> v % 2 == 0).filter((k, v) -> v % 3 == 0);

        List<Pair<String, Integer>> results = iter.toList();
        assertTrue(results.stream().allMatch(p -> p.right() % 6 == 0));
    }

    // =====================================================================
    // remove() (inherited from ImmutableIterator)
    // =====================================================================

    @Test
    public void testRemove() {
        BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1));
        assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    @Test
    public void testToImmutableList_isImmutable() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        ImmutableList<Pair<String, Integer>> immutableList = iter.toImmutableList();

        assertThrows(UnsupportedOperationException.class, () -> immutableList.add(Pair.of("b", 2)));
    }

    @Test
    public void testToImmutableSet_isImmutable() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        ImmutableSet<Pair<String, Integer>> immutableSet = iter.toImmutableSet();

        assertThrows(UnsupportedOperationException.class, () -> immutableSet.add(Pair.of("b", 2)));
    }

    @Test
    @DisplayName("Test concurrent modification scenarios")
    public void testConcurrentModification() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map);

        iter.next();

        map.put("d", 4);

        try {
            iter.forEachRemaining((k, v) -> {
            });
        } catch (ConcurrentModificationException e) {
        }
        assertNotNull(iter);
    }

    // =====================================================================
    // generate(Consumer)
    // =====================================================================

    @Test
    public void testGenerateWithOutput() {
        Consumer<Pair<Integer, String>> output = pair -> {
            pair.set(1, "one");
        };

        BiIterator<Integer, String> iter = BiIterator.generate(output);

        Assertions.assertTrue(iter.hasNext());
        Pair<Integer, String> pair = iter.next();
        Assertions.assertEquals(Integer.valueOf(1), pair.left());
        Assertions.assertEquals("one", pair.right());

        Assertions.assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_consumer_infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        BiIterator<Integer, String> iter = BiIterator.generate(pair -> {
            int n = counter.incrementAndGet();
            pair.set(n, "value" + n);
        });

        assertTrue(iter.hasNext());
        Pair<Integer, String> first = iter.next();
        assertEquals(1, first.left());
        assertEquals("value1", first.right());

        BiIterator<Integer, String> limited = BiIterator.<Integer, String> generate(pair -> {
            int n = counter.incrementAndGet();
            pair.set(n, "value" + n);
        }).limit(3);

        int count = 0;
        while (limited.hasNext()) {
            limited.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Test generate with infinite generator")
    public void testGenerateInfinite() {
        MutableInt counter = MutableInt.of(0);

        BiIterator<Integer, String> iter = BiIterator.generate(pair -> {
            int val = counter.getAndIncrement();
            pair.set(val, "value" + val);
        });

        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            Pair<Integer, String> pair = iter.next();
            assertEquals(i, pair.left());
            assertEquals("value" + i, pair.right());
        }

        assertTrue(iter.hasNext());
    }

    // =====================================================================
    // generate(BooleanSupplier, Consumer)
    // =====================================================================

    @Test
    public void testGenerateWithHasNextAndOutput() {
        MutableInt counter = MutableInt.of(0);
        BooleanSupplier hasNext = () -> counter.value() < 3;
        Consumer<Pair<Integer, String>> output = pair -> {
            int val = counter.getAndIncrement();
            pair.set(val, String.valueOf(val));
        };

        BiIterator<Integer, String> iter = BiIterator.generate(hasNext, output);

        List<Pair<Integer, String>> pairs = new ArrayList<>();
        while (iter.hasNext()) {
            pairs.add(iter.next());
        }

        Assertions.assertEquals(3, pairs.size());
        Assertions.assertEquals(Integer.valueOf(0), pairs.get(0).left());
        Assertions.assertEquals("0", pairs.get(0).right());
        Assertions.assertEquals(Integer.valueOf(2), pairs.get(2).left());
        Assertions.assertEquals("2", pairs.get(2).right());
    }

    @Test
    public void testGenerateWithHasNextAndOutputForEachRemaining() {
        MutableInt counter = MutableInt.of(0);
        BooleanSupplier hasNext = () -> counter.value() < 3;
        Consumer<Pair<Integer, String>> output = pair -> {
            int val = counter.value();
            pair.set(val, String.valueOf(val));
            counter.increment();
        };

        BiIterator<Integer, String> iter = BiIterator.generate(hasNext, output);

        List<Integer> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        Assertions.assertEquals(Arrays.asList(0, 1, 2), keys);
        Assertions.assertEquals(Arrays.asList("0", "1", "2"), values);
    }

    @Test
    public void testGenerateWithHasNextAndOutputMap() {
        MutableInt counter = MutableInt.of(0);
        BooleanSupplier hasNext = () -> counter.value() < 2;
        Consumer<Pair<Integer, String>> output = pair -> {
            int val = counter.getAndIncrement();
            pair.set(val, String.valueOf(val));
        };

        BiIterator<Integer, String> iter = BiIterator.generate(hasNext, output);
        ObjIterator<String> mapped = iter.map((i, s) -> s + "-" + i);

        Assertions.assertTrue(mapped.hasNext());
        Assertions.assertEquals("0-0", mapped.next());
        Assertions.assertTrue(mapped.hasNext());
        Assertions.assertEquals("1-1", mapped.next());
        Assertions.assertFalse(mapped.hasNext());
    }

    // =====================================================================
    // generate(int, int, IntObjConsumer)
    // =====================================================================

    @Test
    public void testGenerateWithIndices() {
        IntObjConsumer<Pair<String, Integer>> output = (index, pair) -> {
            pair.set("key" + index, index * 10);
        };

        BiIterator<String, Integer> iter = BiIterator.generate(0, 3, output);

        List<Pair<String, Integer>> pairs = new ArrayList<>();
        while (iter.hasNext()) {
            pairs.add(iter.next());
        }

        Assertions.assertEquals(3, pairs.size());
        Assertions.assertEquals("key0", pairs.get(0).left());
        Assertions.assertEquals(Integer.valueOf(0), pairs.get(0).right());
        Assertions.assertEquals("key2", pairs.get(2).left());
        Assertions.assertEquals(Integer.valueOf(20), pairs.get(2).right());
    }

    @Test
    public void testGenerateWithIndicesForEachRemaining() {
        IntObjConsumer<Pair<String, Integer>> output = (index, pair) -> {
            pair.set("item" + index, index);
        };

        BiIterator<String, Integer> iter = BiIterator.generate(0, 3, output);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        Assertions.assertEquals(Arrays.asList("item0", "item1", "item2"), keys);
        Assertions.assertEquals(Arrays.asList(0, 1, 2), values);
    }

    @Test
    public void testGenerateWithIndicesMap() {
        IntObjConsumer<Pair<String, Integer>> output = (index, pair) -> {
            pair.set("key" + index, index);
        };

        BiIterator<String, Integer> iter = BiIterator.generate(0, 2, output);
        ObjIterator<String> mapped = iter.map((k, v) -> k + "=" + v);

        Assertions.assertEquals("key0=0", mapped.next());
        Assertions.assertEquals("key1=1", mapped.next());
        Assertions.assertFalse(mapped.hasNext());
    }

    @Test
    public void testCount_afterPartialConsumption() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        iter.next();
        iter.next();
        assertEquals(3, iter.count());
    }

    @Test
    public void testCount_exhaustsIterator() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 3, (i, pair) -> pair.set(i, i));
        iter.count();
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test generate with stateful output consumer")
    public void testGenerateStateful() {
        MutableInt counter = MutableInt.of(10);
        BiIterator<Integer, Integer> fib = BiIterator.generate(() -> counter.getAndDecrement() > 0, pair -> {
            if (pair.left() == null || pair.right() == null) {
                pair.set(0, 1);
            } else {
                int next = pair.left() + pair.right();
                pair.set(pair.right(), next);
            }
        });

        List<Pair<Integer, Integer>> fibPairs = fib.toList();
        assertEquals(10, fibPairs.size());
        assertEquals(Pair.of(0, 1), fibPairs.get(0));
        assertEquals(Pair.of(1, 1), fibPairs.get(1));
        assertEquals(Pair.of(1, 2), fibPairs.get(2));
        assertEquals(Pair.of(2, 3), fibPairs.get(3));
    }

    @Test
    public void testGenerateEdgeCases() {
        Consumer<Pair<Integer, String>> output = pair -> {
            pair.set(1, "first");
            pair.set(2, "second");
        };

        BiIterator<Integer, String> iter = BiIterator.generate(() -> true, output).limit(1);
        Pair<Integer, String> result = iter.next();
        Assertions.assertEquals(Integer.valueOf(2), result.left());
        Assertions.assertEquals("second", result.right());
    }

    @Test
    public void testGenerateWithIndices_emptyRange() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(5, 5, (i, pair) -> {
            pair.set(i, i);
        });
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test generate with index at boundaries")
    public void testGenerateIndexBoundaries() {
        BiIterator<Long, Long> iter = BiIterator.generate(Integer.MAX_VALUE - 5, Integer.MAX_VALUE - 1, (index, pair) -> pair.set((long) index, (long) index));

        List<Pair<Long, Long>> results = iter.toList();
        assertEquals(4, results.size());
        assertEquals(Integer.MAX_VALUE - 5, results.get(0).left());
    }

    @Test
    @DisplayName("Test generate with exception in output consumer")
    public void testGenerateWithException() {
        BiIterator<String, String> iter = BiIterator.generate(() -> true, pair -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(RuntimeException.class, () -> iter.next());
    }

    @Test
    public void testGenerateWithHasNextAndOutput_nullChecks() {
        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(null, pair -> {
        }));

        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(() -> true, null));
    }

    @Test
    @DisplayName("Test generate with finite generator")
    public void testGenerateFinite() {
        MutableInt counter = MutableInt.of(0);

        BiIterator<Integer, Integer> iter = BiIterator.generate(() -> counter.value() < 3, pair -> {
            int val = counter.getAndIncrement();
            pair.set(val, val * 2);
        });

        assertTrue(iter.hasNext());
        assertEquals(Pair.of(0, 0), iter.next());

        assertTrue(iter.hasNext());
        assertEquals(Pair.of(1, 2), iter.next());

        assertTrue(iter.hasNext());
        assertEquals(Pair.of(2, 4), iter.next());

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testGenerateWithIndices_invalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> BiIterator.generate(5, 2, (i, pair) -> {
        }));
    }

    @Test
    public void testGenerateWithIndices_nullOutput() {
        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(0, 5, null));
    }

    // =====================================================================
    // zip(A[], B[])
    // =====================================================================

    @Test
    public void testZipArrays() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        Assertions.assertEquals(Pair.of("a", 1), iter.next());
        Assertions.assertEquals(Pair.of("b", 2), iter.next());
        Assertions.assertEquals(Pair.of("c", 3), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testZipArrays_differentLengths() {
        String[] arr1 = { "a", "b", "c", "d" };
        Integer[] arr2 = { 1, 2 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        Assertions.assertEquals(Pair.of("a", 1), iter.next());
        Assertions.assertEquals(Pair.of("b", 2), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // =====================================================================
    // zip(A[], B[], A, B)
    // =====================================================================

    @Test
    public void testZipArraysWithDefaults() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2, 3, 4 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2, "default", -1);

        Assertions.assertEquals(Pair.of("a", 1), iter.next());
        Assertions.assertEquals(Pair.of("b", 2), iter.next());
        Assertions.assertEquals(Pair.of("default", 3), iter.next());
        Assertions.assertEquals(Pair.of("default", 4), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    // =====================================================================
    // zip(Iterable, Iterable)
    // =====================================================================

    @Test
    public void testZipIterables() {
        List<String> list1 = Arrays.asList("x", "y", "z");
        Set<Integer> set2 = new LinkedHashSet<>(Arrays.asList(10, 20, 30));

        BiIterator<String, Integer> iter = BiIterator.zip(list1, set2);

        List<Pair<String, Integer>> pairs = new ArrayList<>();
        while (iter.hasNext()) {
            pairs.add(iter.next());
        }

        Assertions.assertEquals(3, pairs.size());
        Assertions.assertEquals("x", pairs.get(0).left());
        Assertions.assertEquals(Integer.valueOf(10), pairs.get(0).right());
    }

    // =====================================================================
    // zip(Iterator, Iterator)
    // =====================================================================

    @Test
    public void testZipIterators() {
        Iterator<String> iter1 = Arrays.asList("one", "two", "three").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3, 4).iterator();

        BiIterator<String, Integer> biIter = BiIterator.zip(iter1, iter2);

        ObjIterator<String> mapped = biIter.map((s, i) -> s + ":" + i);
        Assertions.assertEquals("one:1", mapped.next());
        Assertions.assertEquals("two:2", mapped.next());
        Assertions.assertEquals("three:3", mapped.next());
        Assertions.assertFalse(mapped.hasNext());
    }

    @Test
    public void testZipIterators_differentLengths() {
        Iterator<String> names = List.of("Alice", "Bob", "Charlie").iterator();
        Iterator<Integer> ages = List.of(25, 30).iterator();

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages);

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testToSet_duplicates() {
        // When zipping with defaults, duplicates can occur
        BiIterator<String, Integer> iter = BiIterator.zip(new String[] { "a", "a" }, new Integer[] { 1, 1 });
        Set<Pair<String, Integer>> set = iter.toSet();

        assertEquals(1, set.size());
        assertTrue(set.contains(Pair.of("a", 1)));
    }

    @Test
    @DisplayName("Test operations after iterator exhaustion")
    public void testOperationsAfterExhaustion() {
        String[] arr1 = { "a" };
        Integer[] arr2 = { 1 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        iter.next();

        assertFalse(iter.hasNext());
        assertEquals(0, iter.toList().size());

        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((a, b) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testZipArrays_null() {
        BiIterator<String, Integer> iter = BiIterator.zip((String[]) null, (Integer[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test zip with empty arrays")
    public void testZipEmptyArrays() {
        String[] empty1 = new String[0];
        Integer[] empty2 = new Integer[0];

        BiIterator<String, Integer> iter = BiIterator.zip(empty1, empty2);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test zip with null elements in arrays")
    public void testZipWithNullElements() {
        String[] arr1 = { "a", null, "c" };
        Integer[] arr2 = { 1, 2, null };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        assertEquals(Pair.of("a", 1), iter.next());
        assertEquals(Pair.of(null, 2), iter.next());
        assertEquals(Pair.of("c", null), iter.next());
    }

    @Test
    public void testZipEdgeCases() {
        String[] empty1 = new String[0];
        Integer[] empty2 = new Integer[0];
        BiIterator<String, Integer> iter = BiIterator.zip(empty1, empty2);
        Assertions.assertFalse(iter.hasNext());

        iter = BiIterator.zip((String[]) null, empty2);
        Assertions.assertFalse(iter.hasNext());

        iter = BiIterator.zip(empty1, (Integer[]) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test zip with one empty array and defaults")
    public void testZipOneEmptyWithDefaults() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] empty = new Integer[0];

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, empty, "default", 99);

        List<Pair<String, Integer>> results = iter.toList();
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(p -> p.right().equals(99)));
    }

    @Test
    public void testZipIterables_null() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterable<String>) null, (Iterable<Integer>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipIterablesNull() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterable<String>) null, Arrays.asList(1, 2, 3));
        Assertions.assertFalse(iter.hasNext());

        iter = BiIterator.zip(Arrays.asList("a", "b"), (Iterable<Integer>) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test zip with null iterables")
    public void testZipNullIterables() {
        BiIterator<String, Integer> iter1 = BiIterator.zip(null, Arrays.asList(1, 2));
        assertFalse(iter1.hasNext());

        BiIterator<String, Integer> iter2 = BiIterator.zip(Arrays.asList("a", "b"), null);
        assertFalse(iter2.hasNext());

        BiIterator<String, Integer> iter3 = BiIterator.zip((List<String>) null, (List<Integer>) null);
        assertFalse(iter3.hasNext());
    }

    // =====================================================================
    // zip(Iterable, Iterable, A, B)
    // =====================================================================

    @Test
    public void testZipIterablesWithDefaults() {
        List<String> list1 = Arrays.asList("a");
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        BiIterator<String, Integer> iter = BiIterator.zip(list1, list2, "missing", 0);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        Assertions.assertEquals(Arrays.asList("a", "missing", "missing"), keys);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), values);
    }

    @Test
    public void testZipIteratorsNull() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterator<String>) null, Arrays.asList(1, 2).iterator());
        Assertions.assertFalse(iter.hasNext());

        iter = BiIterator.zip(Arrays.asList("a").iterator(), (Iterator<Integer>) null);
        Assertions.assertFalse(iter.hasNext());
    }

    // =====================================================================
    // zip(Iterator, Iterator, A, B)
    // =====================================================================

    @Test
    public void testZipIteratorsWithDefaults() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1).iterator();

        BiIterator<String, Integer> biIter = BiIterator.zip(iter1, iter2, null, 99);

        Assertions.assertEquals(Pair.of("a", 1), biIter.next());
        Assertions.assertEquals(Pair.of("b", 99), biIter.next());
        Assertions.assertEquals(Pair.of("c", 99), biIter.next());
        Assertions.assertFalse(biIter.hasNext());
    }

    @Test
    public void testZipIteratorsWithDefaults_nullIterators() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterator<String>) null, (Iterator<Integer>) null, "Default", 0);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test iterator exhaustion behavior")
    public void testIteratorExhaustion() {
        String[] arr1 = { "a" };
        Integer[] arr2 = { 1 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        assertTrue(iter.hasNext());
        assertEquals(Pair.of("a", 1), iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    @DisplayName("Test null arguments validation")
    public void testNullArgumentsValidation() {
        BiIterator<String, Integer> iter = BiIterator.zip(new String[] { "a" }, new Integer[] { 1 });

        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    // =====================================================================
    // unzip(Iterable, BiConsumer)
    // =====================================================================

    @Test
    public void testUnzipIterable() {
        List<String> list = Arrays.asList("1:one", "2:two", "3:three");
        BiConsumer<String, Pair<Integer, String>> unzipFunction = (str, pair) -> {
            String[] parts = str.split(":");
            pair.set(Integer.parseInt(parts[0]), parts[1]);
        };

        BiIterator<Integer, String> iter = BiIterator.unzip(list, unzipFunction);

        Assertions.assertEquals(Pair.of(1, "one"), iter.next());
        Assertions.assertEquals(Pair.of(2, "two"), iter.next());
        Assertions.assertEquals(Pair.of(3, "three"), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test unzip with complex objects")
    public void testUnzipComplexObjects() {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }

        List<Person> people = Arrays.asList(new Person("Alice", 25), new Person("Bob", 30), new Person("Charlie", 35));

        BiIterator<String, Integer> iter = BiIterator.unzip(people, (person, pair) -> pair.set(person.name, person.age));

        Map<String, Integer> nameAgeMap = new HashMap<>();
        iter.forEachRemaining(nameAgeMap::put);

        assertEquals(3, nameAgeMap.size());
        assertEquals(25, nameAgeMap.get("Alice"));
        assertEquals(30, nameAgeMap.get("Bob"));
        assertEquals(35, nameAgeMap.get("Charlie"));
    }

    // =====================================================================
    // unzip(Iterator, BiConsumer)
    // =====================================================================

    @Test
    public void testUnzipIterator() {
        Iterator<String> iterator = Arrays.asList("a=1", "b=2").iterator();
        BiConsumer<String, Pair<String, Integer>> unzipFunction = (str, pair) -> {
            String[] parts = str.split("=");
            pair.set(parts[0], Integer.parseInt(parts[1]));
        };

        BiIterator<String, Integer> iter = BiIterator.unzip(iterator, unzipFunction);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        Assertions.assertEquals(Arrays.asList("a", "b"), keys);
        Assertions.assertEquals(Arrays.asList(1, 2), values);
    }

    @Test
    public void testUnzipIterable_null() {
        BiIterator<String, Integer> iter = BiIterator.unzip((Iterable<String>) null, (s, pair) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzipIterator_null() {
        BiIterator<String, Integer> iter = BiIterator.unzip((Iterator<String>) null, (s, pair) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void testNextThrowable() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("error", 1);

        BiIterator<String, Integer> iter = BiIterator.of(map);

        try {
            iter.next((k, v) -> {
                if ("error".equals(k)) {
                    throw new Exception("Test exception in next");
                }
            });
            Assertions.fail("Expected exception");
        } catch (Exception e) {
            Assertions.assertEquals("Test exception in next", e.getMessage());
        }
    }

    // =====================================================================
    // forEachRemaining(Consumer) (deprecated, inherited)
    // =====================================================================

    @Test
    public void testForEachRemaining_consumer_deprecated() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining(pair -> collected.add(pair));

        assertEquals(2, collected.size());
    }

    // =====================================================================
    // forEachRemaining(BiConsumer)
    // =====================================================================

    @Test
    public void testForEachRemaining_biConsumer() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        assertEquals(3, keys.size());
        assertEquals(3, values.size());
    }

    @Test
    @DisplayName("Test forEachRemaining with BiConsumer")
    public void testForEachRemainingBiConsumer() {
        String[] arr1 = { "x", "y", "z" };
        Integer[] arr2 = { 10, 20, 30 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        iter.next();

        Map<String, Integer> remaining = new HashMap<>();
        iter.forEachRemaining((s, i) -> remaining.put(s, i));

        assertEquals(2, remaining.size());
        assertEquals(20, remaining.get("y"));
        assertEquals(30, remaining.get("z"));
    }

    @Test
    public void testForEachRemaining_biConsumer_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((k, v) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    // =====================================================================
    // foreachRemaining(Throwables.BiConsumer)
    // =====================================================================

    @Test
    public void testForeachRemaining_throwable() throws Exception {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        List<String> collected = new ArrayList<>();
        iter.foreachRemaining((k, v) -> {
            collected.add(k + "=" + v);
        });

        assertEquals(2, collected.size());
    }

    @Test
    public void testForEachRemainingThrowable() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);

        List<String> keys = new ArrayList<>();
        try {
            iter.foreachRemaining((k, v) -> {
                keys.add(k);
                if ("b".equals(k)) {
                    throw new Exception("Test exception");
                }
            });
            Assertions.fail("Expected exception");
        } catch (Exception e) {
            Assertions.assertEquals("Test exception", e.getMessage());
        }

        Assertions.assertEquals(Arrays.asList("a", "b"), keys);
    }

    @Test
    @DisplayName("Test foreachRemaining with Throwables.BiConsumer")
    public void testForeachRemainingThrowable2() throws Exception {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        List<String> collected = new ArrayList<>();
        iter.foreachRemaining((Throwables.BiConsumer<String, Integer, Exception>) (s, i) -> {
            collected.add(s + i);
        });

        assertEquals(Arrays.asList("a1", "b2", "c3"), collected);
    }

    // =====================================================================
    // skip(long)
    // =====================================================================

    @Test
    public void testSkip() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        BiIterator<String, Integer> iter = BiIterator.of(map).skip(2);

        Assertions.assertEquals(Pair.of("c", 3), iter.next());
        Assertions.assertEquals(Pair.of("d", 4), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_moreThanAvailable() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 3, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> skipped = iter.skip(10);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipForEachRemaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).skip(1);

        List<String> keys = new ArrayList<>();
        iter.forEachRemaining((k, v) -> keys.add(k));

        Assertions.assertEquals(Arrays.asList("b", "c"), keys);
    }

    @Test
    public void testSkipMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).skip(1);
        ObjIterator<String> mapped = iter.map((k, v) -> k + v);

        Assertions.assertEquals("b2", mapped.next());
        Assertions.assertEquals("c3", mapped.next());
        Assertions.assertFalse(mapped.hasNext());
    }

    @Test
    @DisplayName("Test skip and limit with exact boundaries")
    public void testSkipLimitExactBoundaries() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add("item" + i);
        }

        BiIterator<String, Integer> iter = BiIterator.unzip(list, (s, pair) -> pair.set(s, Integer.parseInt(s.substring(4))));

        List<Pair<String, Integer>> result = iter.skip(3).limit(7).toList();

        assertEquals(7, result.size());
        assertEquals("item3", result.get(0).left());
        assertEquals("item9", result.get(6).left());
    }

    @Test
    public void testSkip_zero() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> skipped = iter.skip(0);

        assertSame(iter, skipped);
    }

    @Test
    public void testSkip_negative() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    // =====================================================================
    // limit(long)
    // =====================================================================

    @Test
    public void testLimit() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        BiIterator<String, Integer> iter = BiIterator.of(map).limit(2);

        Assertions.assertEquals(Pair.of("a", 1), iter.next());
        Assertions.assertEquals(Pair.of("b", 2), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_moreThanAvailable() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 3, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> limited = iter.limit(10);

        int count = 0;
        while (limited.hasNext()) {
            limited.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testLimitForEachRemaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).limit(2);

        List<String> keys = new ArrayList<>();
        iter.forEachRemaining((k, v) -> keys.add(k));

        Assertions.assertEquals(Arrays.asList("a", "b"), keys);
    }

    @Test
    public void testLimitMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).limit(2);
        ObjIterator<Integer> mapped = iter.map((k, v) -> v * 10);

        Assertions.assertEquals(Integer.valueOf(10), mapped.next());
        Assertions.assertEquals(Integer.valueOf(20), mapped.next());
        Assertions.assertFalse(mapped.hasNext());
    }

    @Test
    @DisplayName("Test limit then skip")
    public void testLimitThenSkip() {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            map.put(i, "value" + i);
        }

        List<Pair<Integer, String>> result = BiIterator.of(map).limit(10).skip(5).toList();

        assertEquals(5, result.size());
        assertEquals(5, result.get(0).left());
        assertEquals(9, result.get(4).left());
    }

    @Test
    public void testLimit_zero() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> limited = iter.limit(0);

        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_negative() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    // =====================================================================
    // filter(BiPredicate)
    // =====================================================================

    @Test
    public void testFilter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> v % 2 == 0);

        Assertions.assertEquals(Pair.of("b", 2), iter.next());
        Assertions.assertEquals(Pair.of("d", 4), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_allMatch() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> filtered = iter.filter((a, b) -> a >= 0);

        int count = 0;
        while (filtered.hasNext()) {
            filtered.next();
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void testFilterForEachRemaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> v > 2);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        Assertions.assertEquals(Arrays.asList("c", "d"), keys);
        Assertions.assertEquals(Arrays.asList(3, 4), values);
    }

    @Test
    public void testFilterComplexPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("apple", 5);
        map.put("banana", 6);
        map.put("cherry", 6);
        map.put("date", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> k.length() == v);

        List<Pair<String, Integer>> result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("apple", result.get(0).left());
        Assertions.assertEquals("banana", result.get(1).left());
        Assertions.assertEquals("cherry", result.get(2).left());
    }

    @Test
    public void testFilter_noneMatch() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> filtered = iter.filter((a, b) -> a > 10);

        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> k.compareTo("b") >= 0);
        ObjIterator<String> mapped = iter.map((k, v) -> k.toUpperCase() + v);

        Assertions.assertEquals("B2", mapped.next());
        Assertions.assertEquals("C3", mapped.next());
        Assertions.assertFalse(mapped.hasNext());
    }

    @Test
    public void testFilter_null() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    // =====================================================================
    // map(BiFunction)
    // =====================================================================

    @Test
    public void testMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        ObjIterator<String> iter = BiIterator.of(map).map((k, v) -> k + v);
        List<String> list = new ArrayList<>();
        iter.forEachRemaining(list::add);

        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.contains("a1"));
        Assertions.assertTrue(list.contains("b2"));
    }

    @Test
    public void testMap_toInteger() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i * 2));
        ObjIterator<Integer> mapped = iter.map((a, b) -> a + b);

        List<Integer> collected = new ArrayList<>();
        while (mapped.hasNext()) {
            collected.add(mapped.next());
        }
        assertEquals(5, collected.size());
        assertEquals(CommonUtil.toList(0, 3, 6, 9, 12), collected);
    }

    @Test
    @DisplayName("Test map after filter")
    public void testMapAfterFilter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        List<Integer> doubled = BiIterator.of(map).filter((k, v) -> k.length() == 3).map((k, v) -> v * 2).toList();

        assertEquals(2, doubled.size());
        assertTrue(doubled.contains(2));
        assertTrue(doubled.contains(4));
    }

    @Test
    @DisplayName("Test map with null transformation result")
    public void testMapToNull() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        ObjIterator<String> iter = BiIterator.zip(arr1, arr2).map((s, i) -> i == 2 ? null : s + i);

        assertEquals("a1", iter.next());
        assertNull(iter.next());
        assertEquals("c3", iter.next());
    }

    @Test
    public void testMapNullMapper() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        iter.map(null);
        assertNotNull(iter);
    }

    @Test
    @DisplayName("Test map with exception in mapper")
    public void testMapWithException() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 0 };

        ObjIterator<Integer> iter = BiIterator.zip(arr1, arr2).map((s, i) -> 10 / i);

        assertEquals(10, iter.next());
        assertThrows(ArithmeticException.class, () -> iter.next());
    }

    @Test
    @DisplayName("Test stream operations after BiIterator")
    public void testStreamOperations() {
        String[] arr1 = { "apple", "banana", "cherry" };
        Integer[] arr2 = { 5, 6, 6 };

        long count = BiIterator.zip(arr1, arr2).stream().filter(e -> e.getValue() == 6).count();

        assertEquals(2, count);
    }

    // =====================================================================
    // stream(BiFunction)
    // =====================================================================

    @Test
    public void testStreamWithMapper() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        Stream<String> stream = iter.stream((k, v) -> k + "=" + v);

        List<String> result = stream.toList();
        Assertions.assertEquals(Arrays.asList("a=1", "b=2"), result);
    }

    // =====================================================================
    // stream()
    // =====================================================================

    @Test
    public void testStream() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        EntryStream<String, Integer> stream = iter.stream();

        Assertions.assertNotNull(stream);
        List<Map.Entry<String, Integer>> entries = stream.toList();
        Assertions.assertEquals(2, entries.size());
    }

    @Test
    public void testStream_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        com.landawn.abacus.util.stream.EntryStream<String, Integer> stream = iter.stream();

        long count = stream.count();
        assertEquals(0, count);
    }

    @Test
    public void testStreamWithMapperNull() {
        BiIterator.empty().stream(null).forEach(s -> Assertions.fail("Should not be called"));
    }

    @Test
    @DisplayName("Test stream with mapper complex operations")
    public void testStreamMapperComplex() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 85);
        scores.put("Bob", 92);
        scores.put("Charlie", 78);
        scores.put("David", 95);

        List<String> highScorers = BiIterator.of(scores).stream((name, score) -> score >= 90 ? name : null).filter(Objects::nonNull).sorted().toList();

        assertEquals(Arrays.asList("Bob", "David"), highScorers);
    }

    // =====================================================================
    // toArray()
    // =====================================================================

    @Test
    public void testToArray() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        Pair<String, Integer>[] array = iter.toArray();

        Assertions.assertEquals(2, array.length);
        Assertions.assertEquals("a", array[0].left());
        Assertions.assertEquals(Integer.valueOf(1), array[0].right());
        Assertions.assertEquals("b", array[1].left());
        Assertions.assertEquals(Integer.valueOf(2), array[1].right());
    }

    // =====================================================================
    // toArray(T[]) (deprecated)
    // =====================================================================

    @Test
    @SuppressWarnings("deprecation")
    public void testToArrayWithType() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        @SuppressWarnings("unchecked")
        Pair<String, Integer>[] array = iter.toArray(new Pair[0]);

        Assertions.assertEquals(1, array.length);
        Assertions.assertEquals("a", array[0].left());
    }

    @Test
    public void testToArray_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Pair<String, Integer>[] array = iter.toArray();
        assertEquals(0, array.length);
    }

    // =====================================================================
    // toList()
    // =====================================================================

    @Test
    public void testToList() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        List<Pair<String, Integer>> list = iter.toList();

        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("x", list.get(0).left());
        Assertions.assertEquals(Integer.valueOf(10), list.get(0).right());
        Assertions.assertEquals("z", list.get(2).left());
        Assertions.assertEquals(Integer.valueOf(30), list.get(2).right());
    }

    @Test
    public void testToList_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        List<Pair<String, Integer>> list = iter.toList();
        assertTrue(list.isEmpty());
    }

    // =====================================================================
    // toMultiList(Supplier)
    // =====================================================================

    @Test
    public void testToMultiList() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        Pair<List<String>, List<Integer>> multiList = iter.toMultiList(ArrayList::new);

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), multiList.left());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), multiList.right());
    }

    @Test
    @DisplayName("Test toMultiList with custom list implementations")
    public void testToMultiListCustomImpl() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        Pair<List<String>, List<Integer>> result = BiIterator.zip(arr1, arr2).toMultiList(LinkedList::new);

        assertTrue(result.left() instanceof LinkedList);
        assertTrue(result.right() instanceof LinkedList);
        assertEquals(3, result.left().size());
        assertEquals(3, result.right().size());
    }

    @Test
    public void testToMultiList_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Pair<List<String>, List<Integer>> multiList = iter.toMultiList(LinkedList::new);

        Assertions.assertTrue(multiList.left().isEmpty());
        Assertions.assertTrue(multiList.right().isEmpty());
    }

    // =====================================================================
    // toMultiSet(Supplier)
    // =====================================================================

    @Test
    public void testToMultiSet() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        Pair<Set<String>, Set<Integer>> multiSet = iter.toMultiSet(HashSet::new);

        Assertions.assertEquals(2, multiSet.left().size());
        Assertions.assertTrue(multiSet.left().contains("a"));
        Assertions.assertTrue(multiSet.left().contains("b"));
        Assertions.assertTrue(multiSet.right().contains(2));
        Assertions.assertTrue(multiSet.right().contains(3));
    }

    @Test
    public void testToMultiSetLinkedHashSet() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        Pair<Set<String>, Set<Integer>> multiSet = iter.toMultiSet(LinkedHashSet::new);

        Iterator<String> keyIter = multiSet.left().iterator();
        Assertions.assertEquals("x", keyIter.next());
        Assertions.assertEquals("y", keyIter.next());
        Assertions.assertEquals("z", keyIter.next());

        Iterator<Integer> valueIter = multiSet.right().iterator();
        Assertions.assertEquals(Integer.valueOf(10), valueIter.next());
        Assertions.assertEquals(Integer.valueOf(20), valueIter.next());
        Assertions.assertEquals(Integer.valueOf(30), valueIter.next());
    }

    @Test
    @DisplayName("Test toMultiSet with duplicates")
    public void testToMultiSetDuplicates() {
        String[] arr1 = { "a", "b", "a", "c", "b" };
        Integer[] arr2 = { 1, 2, 1, 3, 2 };

        Pair<Set<String>, Set<Integer>> result = BiIterator.zip(arr1, arr2).toMultiSet(TreeSet::new);

        assertTrue(result.left() instanceof TreeSet);
        assertTrue(result.right() instanceof TreeSet);
        assertEquals(3, result.left().size());
        assertEquals(3, result.right().size());

        assertEquals("a", result.left().iterator().next());
        assertEquals(1, result.right().iterator().next());
    }

    @Test
    public void testToMultiSet_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Pair<Set<String>, Set<Integer>> sets = iter.toMultiSet(HashSet::new);

        assertTrue(sets.left().isEmpty());
        assertTrue(sets.right().isEmpty());
    }

    // =====================================================================
    // Complex / Integration Tests
    // =====================================================================

    @Test
    public void testComplexChaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        BiIterator<String, Integer> iter = BiIterator.of(map).skip(1).limit(3).filter((k, v) -> v % 2 == 0);

        List<Pair<String, Integer>> result = iter.toList();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("b", result.get(0).left());
        Assertions.assertEquals(Integer.valueOf(2), result.get(0).right());
        Assertions.assertEquals("d", result.get(1).left());
        Assertions.assertEquals(Integer.valueOf(4), result.get(1).right());
    }

    @Test
    @DisplayName("Test deep chaining performance")
    public void testDeepChaining() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 1000, (i, pair) -> pair.set(i, i * 2));

        List<String> result = iter.skip(100)
                .limit(800)
                .filter((a, b) -> a % 2 == 0)
                .filter((a, b) -> b % 4 == 0)
                .skip(50)
                .limit(100)
                .map((a, b) -> a + ":" + b)
                .toList();

        assertTrue(result.size() <= 100);
        assertTrue(result.stream().allMatch(s -> {
            String[] parts = s.split(":");
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first % 2 == 0 && second % 4 == 0;
        }));
    }
}
