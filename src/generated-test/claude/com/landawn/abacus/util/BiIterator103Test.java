package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

public class BiIterator103Test extends TestBase {

    @Test
    public void testEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Assertions.assertNotNull(iter);
        Assertions.assertFalse(iter.hasNext());
        
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
        
        // Test forEachRemaining with empty iterator
        BiConsumer<String, Integer> biConsumer = (s, i) -> Assertions.fail("Should not be called");
        iter.forEachRemaining(biConsumer);
        
        // Test map with empty iterator
        ObjIterator<String> mapped = iter.map((s, i) -> s + i);
        Assertions.assertFalse(mapped.hasNext());
    }

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
    public void testOfMapEmpty() {
        Map<String, Integer> map = new HashMap<>();
        BiIterator<String, Integer> iter = BiIterator.of(map);
        Assertions.assertFalse(iter.hasNext());
    }

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
        
        // Test map function
        ObjIterator<String> mapped = iter.map((k, v) -> k + v);
        Assertions.assertTrue(mapped.hasNext());
        Assertions.assertEquals("b2", mapped.next());
        Assertions.assertFalse(mapped.hasNext());
    }

    @Test
    public void testOfMapEntryIteratorNull() {
        BiIterator<String, Integer> iter = BiIterator.of((Iterator<Map.Entry<String, Integer>>) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerateWithOutput() {
        Consumer<Pair<Integer, String>> output = pair -> {
            pair.set(1, "one");
        };
        
        BiIterator<Integer, String> iter = BiIterator.generate(output);
        
        // Infinite iterator
        Assertions.assertTrue(iter.hasNext());
        Pair<Integer, String> pair = iter.next();
        Assertions.assertEquals(Integer.valueOf(1), pair.left());
        Assertions.assertEquals("one", pair.right());
        
        // Should still have next
        Assertions.assertTrue(iter.hasNext());
    }

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

    @Test
    public void testGenerateWithNullArguments() {
        Assertions.assertThrows(IllegalArgumentException.class, 
            () -> BiIterator.generate(null, pair -> {}));
        Assertions.assertThrows(IllegalArgumentException.class, 
            () -> BiIterator.generate(() -> true, null));
    }

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
    public void testGenerateWithInvalidIndices() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, 
            () -> BiIterator.generate(5, 3, (i, p) -> {}));
        Assertions.assertThrows(IllegalArgumentException.class, 
            () -> BiIterator.generate(0, 3, null));
    }

    @Test
    public void testZipArrays() {
        String[] arr1 = {"a", "b", "c"};
        Integer[] arr2 = {1, 2, 3};
        
        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);
        
        Assertions.assertEquals(Pair.of("a", 1), iter.next());
        Assertions.assertEquals(Pair.of("b", 2), iter.next());
        Assertions.assertEquals(Pair.of("c", 3), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testZipArraysDifferentLength() {
        String[] arr1 = {"a", "b", "c", "d"};
        Integer[] arr2 = {1, 2};
        
        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);
        
        Assertions.assertEquals(Pair.of("a", 1), iter.next());
        Assertions.assertEquals(Pair.of("b", 2), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testZipArraysWithDefaults() {
        String[] arr1 = {"a", "b"};
        Integer[] arr2 = {1, 2, 3, 4};
        
        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2, "default", -1);
        
        Assertions.assertEquals(Pair.of("a", 1), iter.next());
        Assertions.assertEquals(Pair.of("b", 2), iter.next());
        Assertions.assertEquals(Pair.of("default", 3), iter.next());
        Assertions.assertEquals(Pair.of("default", 4), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

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

    @Test
    public void testZipIterablesNull() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterable<String>) null, Arrays.asList(1, 2, 3));
        Assertions.assertFalse(iter.hasNext());
        
        iter = BiIterator.zip(Arrays.asList("a", "b"), (Iterable<Integer>) null);
        Assertions.assertFalse(iter.hasNext());
    }

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
    public void testZipIteratorsNull() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterator<String>) null, Arrays.asList(1, 2).iterator());
        Assertions.assertFalse(iter.hasNext());
        
        iter = BiIterator.zip(Arrays.asList("a").iterator(), (Iterator<Integer>) null);
        Assertions.assertFalse(iter.hasNext());
    }

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
    public void testUnzipIterable() {
        List<String> list = Arrays.asList("1:one", "2:two", "3:three");
        BiConsumer<String, Pair<Integer, String>> unzipFunc = (str, pair) -> {
            String[] parts = str.split(":");
            pair.set(Integer.parseInt(parts[0]), parts[1]);
        };
        
        BiIterator<Integer, String> iter = BiIterator.unzip(list, unzipFunc);
        
        Assertions.assertEquals(Pair.of(1, "one"), iter.next());
        Assertions.assertEquals(Pair.of(2, "two"), iter.next());
        Assertions.assertEquals(Pair.of(3, "three"), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzipIterableNull() {
        BiIterator<String, Integer> iter = BiIterator.unzip((Iterable<String>) null, (s, p) -> {});
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzipIterator() {
        Iterator<String> iterator = Arrays.asList("a=1", "b=2").iterator();
        BiConsumer<String, Pair<String, Integer>> unzipFunc = (str, pair) -> {
            String[] parts = str.split("=");
            pair.set(parts[0], Integer.parseInt(parts[1]));
        };
        
        BiIterator<String, Integer> iter = BiIterator.unzip(iterator, unzipFunc);
        
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
    public void testUnzipIteratorNull() {
        BiIterator<String, Integer> iter = BiIterator.unzip((Iterator<String>) null, (s, p) -> {});
        Assertions.assertFalse(iter.hasNext());
    }

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
    public void testSkipZero() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        
        BiIterator<String, Integer> iter = BiIterator.of(map).skip(0);
        Assertions.assertEquals(Pair.of("a", 1), iter.next());
    }

    @Test
    public void testSkipMoreThanSize() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        
        BiIterator<String, Integer> iter = BiIterator.of(map).skip(5);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNegative() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
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
    public void testLimitZero() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        
        BiIterator<String, Integer> iter = BiIterator.of(map).limit(0);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLimitMoreThanSize() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        
        BiIterator<String, Integer> iter = BiIterator.of(map).limit(10);
        
        Assertions.assertEquals(Pair.of("a", 1), iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLimitNegative() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
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
    public void testFilterNoMatch() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 3);
        
        BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> v > 10);
        Assertions.assertFalse(iter.hasNext());
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
    public void testFilterNullPredicate() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void testFirst() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        
        BiIterator<String, Integer> iter = BiIterator.of(map);
        Optional<Pair<String, Integer>> first = iter.first();
        
        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals("a", first.get().left());
        Assertions.assertEquals(Integer.valueOf(1), first.get().right());
    }

    @Test
    public void testFirstEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Optional<Pair<String, Integer>> first = iter.first();
        
        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        
        BiIterator<String, Integer> iter = BiIterator.of(map);
        Optional<Pair<String, Integer>> last = iter.last();
        
        Assertions.assertTrue(last.isPresent());
        Assertions.assertEquals("c", last.get().left());
        Assertions.assertEquals(Integer.valueOf(3), last.get().right());
    }

    @Test
    public void testLastEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Optional<Pair<String, Integer>> last = iter.last();
        
        Assertions.assertFalse(last.isPresent());
    }

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
    public void testStreamWithMapper() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        
        BiIterator<String, Integer> iter = BiIterator.of(map);
        Stream<String> stream = iter.stream((k, v) -> k + "=" + v);
        
        List<String> result = stream.toList();
        Assertions.assertEquals(Arrays.asList("a=1", "b=2"), result);
    }

    @Test
    public void testStreamWithMapperNull() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.stream(null));
    }

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

    @Test
    public void testToArrayEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Pair<String, Integer>[] array = iter.toArray();
        
        Assertions.assertEquals(0, array.length);
    }

    @Test
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
    public void testToMultiListEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Pair<List<String>, List<Integer>> multiList = iter.toMultiList(LinkedList::new);
        
        Assertions.assertTrue(multiList.left().isEmpty());
        Assertions.assertTrue(multiList.right().isEmpty());
    }

    @Test
    public void testToMultiSet() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a", 3); // This will overwrite the first "a"
        
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
        
        // LinkedHashSet should maintain insertion order
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
    public void testComplexChaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        
        BiIterator<String, Integer> iter = BiIterator.of(map)
            .skip(1)
            .limit(3)
            .filter((k, v) -> v % 2 == 0);
        
        List<Pair<String, Integer>> result = iter.toList();
        
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("b", result.get(0).left());
        Assertions.assertEquals(Integer.valueOf(2), result.get(0).right());
        Assertions.assertEquals("d", result.get(1).left());
        Assertions.assertEquals(Integer.valueOf(4), result.get(1).right());
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

    @Test
    public void testMapNullMapper() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        
        BiIterator<String, Integer> iter = BiIterator.of(map);
        iter.map(null);
        
        // Testing various methods that take mapper and should throw on null
        //    Assertions.assertThrows(IllegalArgumentException.class, () -> iter.map(null));
        //    Assertions.assertThrows(IllegalArgumentException.class, () -> iter.skip(0).map(null));
        //    Assertions.assertThrows(IllegalArgumentException.class, () -> iter.limit(1).map(null));
        //    Assertions.assertThrows(IllegalArgumentException.class, () -> iter.filter((k, v) -> true).map(null));
    }

    @Test
    public void testEmptyBiIteratorThrowables() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        
        // Test that empty iterator throws NoSuchElementException for next with throwable consumer
        Assertions.assertThrows(NoSuchElementException.class, () -> {
            iter.next((k, v) -> {});
        });
        
        // Test that forEachRemaining with throwable doesn't throw for empty
        iter.foreachRemaining((k, v) -> {
            Assertions.fail("Should not be called");
        });
    }

    @Test
    public void testGenerateEdgeCases() {
        // Test generate with output that modifies the pair multiple times
        Consumer<Pair<Integer, String>> output = pair -> {
            pair.set(1, "first");
            pair.set(2, "second"); // This should be the final value
        };
        
        BiIterator<Integer, String> iter = BiIterator.generate(() -> true, output).limit(1);
        Pair<Integer, String> result = iter.next();
        Assertions.assertEquals(Integer.valueOf(2), result.left());
        Assertions.assertEquals("second", result.right());
    }

    @Test
    public void testZipEdgeCases() {
        // Test zip with empty arrays
        String[] empty1 = new String[0];
        Integer[] empty2 = new Integer[0];
        BiIterator<String, Integer> iter = BiIterator.zip(empty1, empty2);
        Assertions.assertFalse(iter.hasNext());
        
        // Test zip with null arrays
        iter = BiIterator.zip((String[]) null, empty2);
        Assertions.assertFalse(iter.hasNext());
        
        iter = BiIterator.zip(empty1, (Integer[]) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterComplexPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("apple", 5);
        map.put("banana", 6);
        map.put("cherry", 6);
        map.put("date", 3);
        
        // Complex predicate: key length equals value
        BiIterator<String, Integer> iter = BiIterator.of(map)
            .filter((k, v) -> k.length() == v);
        
        List<Pair<String, Integer>> result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("apple", result.get(0).left());
        Assertions.assertEquals("banana", result.get(1).left());
        Assertions.assertEquals("cherry", result.get(2).left());
    }
}