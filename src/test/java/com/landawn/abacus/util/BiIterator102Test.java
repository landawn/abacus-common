package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.EntryStream;

@Tag("new-test")
public class BiIterator102Test extends TestBase {

    @Test
    public void testEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map);
        int count = 0;
        while (iter.hasNext()) {
            Pair<String, Integer> pair = iter.next();
            Assertions.assertNotNull(pair.left());
            Assertions.assertNotNull(pair.right());
            count++;
        }
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testOfEmptyMap() {
        BiIterator<String, Integer> iter = BiIterator.of(new HashMap<String, Integer>());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIterator() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiIterator<String, Integer> iter = BiIterator.of(map.entrySet().iterator());
        int count = 0;
        while (iter.hasNext()) {
            count++;
            iter.next();
        }
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testOfNullIterator() {
        BiIterator<String, Integer> iter = BiIterator.of((Iterator<Map.Entry<String, Integer>>) null);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate() {
        int[] counter = { 0 };
        BiIterator<Integer, String> iter = BiIterator.generate(pair -> {
            pair.set(counter[0], "value" + counter[0]);
            counter[0]++;
        });

        Pair<Integer, String> first = iter.next();
        Assertions.assertEquals(0, first.left());
        Assertions.assertEquals("value0", first.right());
    }

    @Test
    public void testGenerateWithHasNext() {
        int[] counter = { 0 };
        BiIterator<Integer, String> iter = BiIterator.generate(() -> counter[0] < 3, pair -> {
            pair.set(counter[0], "value" + counter[0]);
            counter[0]++;
        });

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        Assertions.assertEquals(3, count);
    }

    @Test
    public void testGenerateWithIndex() {
        BiIterator<Integer, String> iter = BiIterator.generate(0, 3, (index, pair) -> {
            pair.set(index, "value" + index);
        });

        List<Pair<Integer, String>> list = iter.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(0, list.get(0).left());
        Assertions.assertEquals("value0", list.get(0).right());
    }

    @Test
    public void testZipArrays() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);
        List<Pair<String, Integer>> list = iter.toList();

        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0).left());
        Assertions.assertEquals(1, list.get(0).right());
    }

    @Test
    public void testZipArraysWithDefaults() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2, 3 };

        BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2, "default", -1);
        List<Pair<String, Integer>> list = iter.toList();

        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("default", list.get(2).left());
        Assertions.assertEquals(3, list.get(2).right());
    }

    @Test
    public void testZipIterables() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        BiIterator<String, Integer> iter = BiIterator.zip(list1, list2);
        Assertions.assertEquals(3, iter.toList().size());
    }

    @Test
    public void testZipIterators() {
        List<String> list1 = Arrays.asList("a", "b");
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        BiIterator<String, Integer> iter = BiIterator.zip(list1.iterator(), list2.iterator());
        Assertions.assertEquals(2, iter.toList().size());
    }

    @Test
    public void testUnzip() {
        List<String> list = Arrays.asList("a:1", "b:2", "c:3");

        BiIterator<String, Integer> iter = BiIterator.unzip(list, (str, pair) -> {
            String[] parts = str.split(":");
            pair.set(parts[0], Integer.parseInt(parts[1]));
        });

        List<Pair<String, Integer>> result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0).left());
        Assertions.assertEquals(1, result.get(0).right());
    }

    @Test
    public void testSkip() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).skip(1);
        List<Pair<String, Integer>> list = iter.toList();
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testLimit() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).limit(2);
        List<Pair<String, Integer>> list = iter.toList();
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFilter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> v > 1);
        List<Pair<String, Integer>> list = iter.toList();
        Assertions.assertEquals(2, list.size());
    }

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
    public void testFirst() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Optional<Pair<String, Integer>> first = BiIterator.of(map).first();
        Assertions.assertTrue(first.isPresent());
    }

    @Test
    public void testFirstEmpty() {
        Optional<Pair<String, Integer>> first = BiIterator.<String, Integer> empty().first();
        Assertions.assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Optional<Pair<String, Integer>> last = BiIterator.of(map).last();
        Assertions.assertTrue(last.isPresent());
        Assertions.assertEquals("c", last.get().left());
        Assertions.assertEquals(3, last.get().right());
    }

    @Test
    public void testStream() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = BiIterator.of(map).stream();
        long count = stream.count();
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testStreamWithMapper() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        var stream = BiIterator.of(map).stream((k, v) -> k + v);
        List<String> list = stream.toList();

        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.contains("a1"));
        Assertions.assertTrue(list.contains("b2"));
    }

    @Test
    public void testToArray() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Pair<String, Integer>[] array = BiIterator.of(map).toArray();
        Assertions.assertEquals(2, array.length);
    }

    @Test
    public void testToList() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        List<Pair<String, Integer>> list = BiIterator.of(map).toList();
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testToMultiList() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Pair<List<String>, List<Integer>> result = BiIterator.of(map).toMultiList(ArrayList::new);
        Assertions.assertEquals(3, result.left().size());
        Assertions.assertEquals(3, result.right().size());
        Assertions.assertTrue(result.left().contains("a"));
        Assertions.assertTrue(result.right().contains(1));
    }

    @Test
    public void testToMultiSet() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Pair<Set<String>, Set<Integer>> result = BiIterator.of(map).toMultiSet(HashSet::new);
        Assertions.assertEquals(3, result.left().size());
        Assertions.assertEquals(3, result.right().size());
        Assertions.assertTrue(result.left().contains("a"));
        Assertions.assertTrue(result.right().contains(1));
    }

    @Test
    public void testForEachRemaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        BiIterator.of(map).forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        Assertions.assertEquals(2, keys.size());
        Assertions.assertEquals(2, values.size());
    }

    @Test
    public void testForeachRemaining() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        BiIterator.of(map).foreachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        Assertions.assertEquals(2, keys.size());
        Assertions.assertEquals(2, values.size());
    }
}
