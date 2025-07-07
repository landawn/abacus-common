package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

public class BiIterator100Test extends TestBase {

    @Test
    @DisplayName("Test empty() returns empty BiIterator")
    public void testEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());

        // Test forEachRemaining doesn't throw on empty iterator
        assertDoesNotThrow(() -> iter.forEachRemaining((a, b) -> {
        }));
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

    @Test
    @DisplayName("Test of(Map) with empty map")
    public void testOfMapEmpty() {
        Map<String, Integer> map = new HashMap<>();
        BiIterator<String, Integer> iter = BiIterator.of(map);

        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of(Map) with null map")
    public void testOfMapNull() {
        BiIterator<String, Integer> iter = BiIterator.of((Map<String, Integer>) null);

        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test of(Iterator<Map.Entry>) with entries")
    public void testOfIteratorWithEntries() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map.entrySet().iterator());

        assertTrue(iter.hasNext());

        int count = 0;
        while (iter.hasNext()) {
            Pair<String, Integer> pair = iter.next();
            assertNotNull(pair);
            assertTrue(map.containsKey(pair.left()));
            assertEquals(map.get(pair.left()), pair.right());
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test of(Iterator<Map.Entry>) with null iterator")
    public void testOfIteratorNull() {
        BiIterator<String, Integer> iter = BiIterator.of((Iterator<Map.Entry<String, Integer>>) null);

        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test generate with infinite generator")
    public void testGenerateInfinite() {
        MutableInt counter = MutableInt.of(0);

        BiIterator<Integer, String> iter = BiIterator.generate(pair -> {
            int val = counter.getAndIncrement();
            pair.set(val, "value" + val);
        });

        // Should be able to get multiple values
        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            Pair<Integer, String> pair = iter.next();
            assertEquals(i, pair.left());
            assertEquals("value" + i, pair.right());
        }

        // Should still have more
        assertTrue(iter.hasNext());
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
    @DisplayName("Test generate with index range")
    public void testGenerateWithIndexRange() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (index, pair) -> {
            pair.set(index, index * index);
        });

        List<Pair<Integer, Integer>> results = iter.toList();

        assertEquals(5, results.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, results.get(i).left());
            assertEquals(i * i, results.get(i).right());
        }
    }

    @Test
    @DisplayName("Test generate with invalid index range")
    public void testGenerateWithInvalidIndexRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> BiIterator.generate(5, 2, (index, pair) -> {
        }));
    }

    @Test
    @DisplayName("Test zip arrays with equal length")
    public void testZipArraysEqualLength() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        assertEquals(Pair.of("a", 1), iter.next());
        assertEquals(Pair.of("b", 2), iter.next());
        assertEquals(Pair.of("c", 3), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test zip arrays with different lengths")
    public void testZipArraysDifferentLength() {
        String[] arr1 = { "a", "b", "c", "d" };
        Integer[] arr2 = { 1, 2 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        assertEquals(Pair.of("a", 1), iter.next());
        assertEquals(Pair.of("b", 2), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test zip arrays with default values")
    public void testZipArraysWithDefaults() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2, 3, 4 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2, "default", 0);

        assertEquals(Pair.of("a", 1), iter.next());
        assertEquals(Pair.of("b", 2), iter.next());
        assertEquals(Pair.of("default", 3), iter.next());
        assertEquals(Pair.of("default", 4), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test zip iterables")
    public void testZipIterables() {
        List<String> list1 = Arrays.asList("x", "y", "z");
        Set<Integer> set2 = new LinkedHashSet<>(Arrays.asList(10, 20, 30));

        BiIterator<String, Integer> iter = BiIterator.zip(list1, set2);

        List<Pair<String, Integer>> results = iter.toList();
        assertEquals(3, results.size());
        assertEquals("x", results.get(0).left());
        assertEquals(10, results.get(0).right());
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

    @Test
    @DisplayName("Test unzip with function")
    public void testUnzip() {
        List<String> data = Arrays.asList("a:1", "b:2", "c:3");

        BiIterator<String, Integer> iter = BiIterator.unzip(data, (str, pair) -> {
            String[] parts = str.split(":");
            pair.set(parts[0], Integer.parseInt(parts[1]));
        });

        assertEquals(Pair.of("a", 1), iter.next());
        assertEquals(Pair.of("b", 2), iter.next());
        assertEquals(Pair.of("c", 3), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test skip elements")
    public void testSkip() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        BiIterator<String, Integer> iter = BiIterator.of(map).skip(2);

        List<Pair<String, Integer>> remaining = iter.toList();
        assertEquals(2, remaining.size());
    }

    @Test
    @DisplayName("Test skip with negative value")
    public void testSkipNegative() {
        assertThrows(IllegalArgumentException.class, () -> BiIterator.empty().skip(-1));
    }

    @Test
    @DisplayName("Test skip more than available")
    public void testSkipMoreThanAvailable() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2).skip(5);

        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test limit elements")
    public void testLimit() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put("key" + i, i);
        }

        BiIterator<String, Integer> iter = BiIterator.of(map).limit(3);

        List<Pair<String, Integer>> limited = iter.toList();
        assertEquals(3, limited.size());
    }

    @Test
    @DisplayName("Test limit with zero")
    public void testLimitZero() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2).limit(0);

        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test limit with negative value")
    public void testLimitNegative() {
        assertThrows(IllegalArgumentException.class, () -> BiIterator.empty().limit(-1));
    }

    @Test
    @DisplayName("Test filter with predicate")
    public void testFilter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> v % 2 == 0);

        List<Pair<String, Integer>> evens = iter.toList();
        assertEquals(2, evens.size());
        assertTrue(evens.stream().allMatch(p -> p.right() % 2 == 0));
    }

    @Test
    @DisplayName("Test map transformation")
    public void testMap() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        ObjIterator<String> iter = BiIterator.zip(arr1, arr2).map((s, i) -> s + i);

        assertEquals("a1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test first() with elements")
    public void testFirstWithElements() {
        String[] arr1 = { "first", "second" };
        Integer[] arr2 = { 1, 2 };

        u.Optional<Pair<String, Integer>> first = BiIterator.zip(arr1, arr2).first();

        assertTrue(first.isPresent());
        assertEquals("first", first.get().left());
        assertEquals(1, first.get().right());
    }

    @Test
    @DisplayName("Test first() with empty iterator")
    public void testFirstEmpty() {
        u.Optional<Pair<String, Integer>> first = BiIterator.<String, Integer> empty().first();

        assertFalse(first.isPresent());
    }

    @Test
    @DisplayName("Test last() with elements")
    public void testLastWithElements() {
        String[] arr1 = { "first", "second", "last" };
        Integer[] arr2 = { 1, 2, 3 };

        u.Optional<Pair<String, Integer>> last = BiIterator.zip(arr1, arr2).last();

        assertTrue(last.isPresent());
        assertEquals("last", last.get().left());
        assertEquals(3, last.get().right());
    }

    @Test
    @DisplayName("Test last() with empty iterator")
    public void testLastEmpty() {
        u.Optional<Pair<String, Integer>> last = BiIterator.<String, Integer> empty().last();

        assertFalse(last.isPresent());
    }

    @Test
    @DisplayName("Test stream() conversion")
    public void testStream() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        EntryStream<String, Integer> stream = BiIterator.zip(arr1, arr2).stream();

        Map<String, Integer> result = stream.toMap();
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
    }

    @Test
    @DisplayName("Test stream(mapper) conversion")
    public void testStreamWithMapper() {
        String[] arr1 = { "x", "y" };
        Integer[] arr2 = { 10, 20 };

        Stream<String> stream = BiIterator.zip(arr1, arr2).stream((s, i) -> s + "=" + i);

        List<String> result = stream.toList();
        assertEquals(Arrays.asList("x=10", "y=20"), result);
    }

    @Test
    @DisplayName("Test toArray()")
    public void testToArray() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2 };

        Pair<String, Integer>[] result = BiIterator.zip(arr1, arr2).toArray();

        assertEquals(2, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].right());
        assertEquals("b", result[1].left());
        assertEquals(2, result[1].right());
    }

    @Test
    @DisplayName("Test toList()")
    public void testToList() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 100);
        map.put("y", 200);

        List<Pair<String, Integer>> list = BiIterator.of(map).toList();

        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(p -> p.left().equals("x") && p.right().equals(100)));
        assertTrue(list.stream().anyMatch(p -> p.left().equals("y") && p.right().equals(200)));
    }

    @Test
    @DisplayName("Test toMultiList()")
    public void testToMultiList() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        Pair<List<String>, List<Integer>> result = BiIterator.zip(arr1, arr2).toMultiList(ArrayList::new);

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    @DisplayName("Test toMultiSet()")
    public void testToMultiSet() {
        String[] arr1 = { "a", "b", "c", "a" };
        Integer[] arr2 = { 1, 2, 3, 1 };

        Pair<Set<String>, Set<Integer>> result = BiIterator.zip(arr1, arr2).toMultiSet(HashSet::new);

        assertEquals(3, result.left().size());
        assertTrue(result.left().containsAll(Arrays.asList("a", "b", "c")));
        assertEquals(3, result.right().size());
        assertTrue(result.right().containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    @DisplayName("Test forEachRemaining with BiConsumer")
    public void testForEachRemainingBiConsumer() {
        String[] arr1 = { "x", "y", "z" };
        Integer[] arr2 = { 10, 20, 30 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        // Consume first element
        iter.next();

        // Collect remaining
        Map<String, Integer> remaining = new HashMap<>();
        iter.forEachRemaining((s, i) -> remaining.put(s, i));

        assertEquals(2, remaining.size());
        assertEquals(20, remaining.get("y"));
        assertEquals(30, remaining.get("z"));
    }

    @Test
    @DisplayName("Test complex chaining operations")
    public void testComplexChaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            map.put("item" + i, i);
        }

        List<String> result = BiIterator.of(map).skip(5).limit(10).filter((k, v) -> v % 2 == 0).map((k, v) -> k + ":" + (v * 2)).toList();

        assertEquals(5, result.size());
        assertTrue(result.contains("item6:12"));
        assertTrue(result.contains("item8:16"));
        assertTrue(result.contains("item10:20"));
        assertTrue(result.contains("item12:24"));
        assertTrue(result.contains("item14:28"));
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
    @DisplayName("Test foreachRemaining with Throwables.BiConsumer")
    public void testForeachRemainingThrowable() throws Exception {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

        List<String> collected = new ArrayList<>();
        iter.foreachRemaining((Throwables.BiConsumer<String, Integer, Exception>) (s, i) -> {
            collected.add(s + i);
        });

        assertEquals(Arrays.asList("a1", "b2", "c3"), collected);
    }
}
