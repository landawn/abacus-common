package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;

/** EntryStream creation contracts. */
public class EntryStreamCreationTest extends EntryStreamTestSupport {

    @Test
    public void testDefer() {
        AtomicBoolean called = new AtomicBoolean(false);
        EntryStream<String, Integer> stream = EntryStream.defer(() -> {
            called.set(true);
            return EntryStream.of(testMap);
        });
        assertFalse(called.get());
        assertEquals(5, stream.count());
        assertTrue(called.get());
    }

    @Test
    public void testEmpty() {
        EntryStream<String, Integer> stream = EntryStream.empty();
        assertEquals(0, stream.count());
    }

    @Test
    public void testEmptyStreamOperations() {

        assertEquals(0, EntryStream.empty().count());
        assertFalse(EntryStream.empty().first().isPresent());
        assertFalse(EntryStream.empty().last().isPresent());
        assertFalse(EntryStream.<String, Integer> empty().min(Map.Entry.comparingByKey()).isPresent());
        assertFalse(EntryStream.<String, Integer> empty().max(Map.Entry.comparingByKey()).isPresent());
        assertTrue(EntryStream.empty().toList().isEmpty());
        assertTrue(EntryStream.empty().toMap().isEmpty());
    }

    @Test
    public void testFlattMapWithBiFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream
                .flattMap((k, v) -> Stream.of(new AbstractMap.SimpleImmutableEntry<>(k + "1", v), new AbstractMap.SimpleImmutableEntry<>(k + "2", v * 2)))
                .toMap();
        assertEquals(2, result.size());
        assertEquals(2, result.get("a1"));
        assertEquals(4, result.get("a2"));
    }

    @Test
    public void testFlattMapWithFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flattMap(e -> Stream.of(new AbstractMap.SimpleImmutableEntry<>(e.getKey() + "1", e.getValue()),
                new AbstractMap.SimpleImmutableEntry<>(e.getKey() + "2", e.getValue() * 2))).toMap();
        assertEquals(2, result.size());
        assertEquals(2, result.get("a1"));
        assertEquals(4, result.get("a2"));
    }

    @Test
    public void testFromStream() {
        java.util.stream.Stream<Map.Entry<String, Integer>> jdkStream = java.util.stream.Stream.of(new AbstractMap.SimpleEntry<>("a", 1));

        assertEquals(1, Stream.from(jdkStream).mapToEntry(Fn.identity()).count());
    }

    @Test
    public void testFromIterable() {
        List<Map.Entry<String, Integer>> list = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        assertEquals(2, EntryStream.of(list).count());
    }

    // iterator

    @Test
    public void testFromIterator() {
        List<Map.Entry<String, Integer>> list = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        assertEquals(2, EntryStream.of(list.iterator()).count());
    }

    @Test
    public void testOf_Iterator() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap.entrySet().iterator());
        assertEquals(5, stream.count());
    }

    @Test
    public void testOf_IteratorWithKeyMapper() {
        List<String> list = Arrays.asList("apple", "banana");
        Map<Integer, String> result = EntryStream.of(list.iterator(), String::length).toMap((v1, v2) -> v1);
        assertTrue(result.containsKey(5));
    }

    @Test
    public void testOf_IteratorWithKeyValueMapper() {
        List<String> list = Arrays.asList("apple", "banana");
        Map<Integer, String> result = EntryStream.of(list.iterator(), String::length, String::toUpperCase).toMap((v1, v2) -> v1);
        assertEquals("APPLE", result.get(5));
    }

    @Test
    public void testOfIterator() {
        Iterator<Map.Entry<String, Integer>> iterator = testMap.entrySet().iterator();
        EntryStream<String, Integer> stream = EntryStream.of(iterator);
        Map<String, Integer> result = stream.toMap();

        assertEquals(testMap.size(), result.size());
    }

    @Test
    public void testOfIteratorWithKeyMapper() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        EntryStream<Integer, String> stream = EntryStream.of(names.iterator(), String::length);
        Map<Integer, String> result = stream.toMap();

        assertEquals(3, result.size());
    }

    @Test
    public void testOfIteratorWithKeyValueMappers() {
        List<String> words = Arrays.asList("hello", "world");
        EntryStream<String, Integer> stream = EntryStream.of(words.iterator(), String::toUpperCase, String::length);
        Map<String, Integer> result = stream.toMap();

        assertEquals(2, result.size());
    }

    @Test
    public void testOfWithIteratorAndMappers() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        Map<Integer, String> lengthToWord = EntryStream.of(words.iterator(), String::length).toMap((v1, v2) -> v1 + "," + v2);

        assertEquals("apple", lengthToWord.get(5));
        assertEquals("banana,cherry", lengthToWord.get(6));

        Iterator<Integer> iter = Arrays.asList(10, 20, 30, 40).iterator();
        Map<String, String> rangeToValue = EntryStream.of(iter, n -> n <= 20 ? "low" : "high", n -> "val" + n).toMap((v1, v2) -> v1 + "," + v2);

        assertEquals("val10,val20", rangeToValue.get("low"));
        assertEquals("val30,val40", rangeToValue.get("high"));
    }

    @Test
    public void testOfWithIteratorOfEntries() {
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("c", 3));

        Map<String, Integer> result = EntryStream.of(entries.iterator()).toMap();
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
    }

    @Test
    public void testOf() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertEquals(5, stream.count());

        EntryStream<String, Integer> stream2 = EntryStream.of("a", 1, "b", 2, "c", 3);
        assertEquals(3, stream2.count());

        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        assertEquals(0, emptyStream.count());

        Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 100);
        assertEquals(1, EntryStream.ofNullable(entry).count());
        assertEquals(0, EntryStream.ofNullable(null).count());
    }

    @Test
    public void testOf_ArrayWithKeyMapper() {
        String[] array = { "apple", "banana", "cherry" };
        Map<Integer, String> result = EntryStream.of(array, String::length).toMap((v1, v2) -> v1);
        assertTrue(result.containsKey(5));
        assertTrue(result.containsKey(6));
    }

    @Test
    public void testOf_ArrayWithKeyValueMapper() {
        String[] array = { "apple", "banana", "cherry" };
        Map<Integer, String> result = EntryStream.of(array, String::length, String::toUpperCase).toMap((v1, v2) -> v1);
        assertEquals("APPLE", result.get(5));
    }

    @Test
    public void testOf_FiveEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).toMap();
        assertEquals(5, result.size());
        assertEquals(5, result.get("e"));
    }

    @Test
    public void testOf_FourEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).toMap();
        assertEquals(4, result.size());
        assertEquals(4, result.get("d"));
    }

    @Test
    public void testOf_Iterable() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap.entrySet());
        assertEquals(5, stream.count());
    }

    @Test
    public void testOf_IterableWithKeyMapper() {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        Map<Integer, String> result = EntryStream.of(list, String::length).toMap((v1, v2) -> v1);
        assertTrue(result.containsKey(5));
    }

    @Test
    public void testOf_IterableWithKeyValueMapper() {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        Map<Integer, String> result = EntryStream.of(list, String::length, String::toUpperCase).toMap((v1, v2) -> v1);
        assertEquals("APPLE", result.get(5));
    }

    @Test
    public void testOf_Map() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertEquals(5, stream.count());
    }

    @Test
    public void testOf_Multimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);
        EntryStream<String, List<Integer>> stream = EntryStream.of(multimap);
        assertEquals(2, stream.count());
    }

    @Test
    public void testOf_OneEntry() {
        Map<String, Integer> result = EntryStream.of("a", 1).toMap();
        assertEquals(1, result.size());
        assertEquals(1, result.get("a"));
    }

    @Test
    public void testOf_SevenEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7).toMap();
        assertEquals(7, result.size());
        assertEquals(7, result.get("g"));
    }

    @Test
    public void testOf_SixEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6).toMap();
        assertEquals(6, result.size());
        assertEquals(6, result.get("f"));
    }

    @Test
    public void testOf_ThreeEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3).toMap();
        assertEquals(3, result.size());
        assertEquals(3, result.get("c"));
    }

    @Test
    public void testOf_TwoEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2).toMap();
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    public void testOfArrayWithKeyMapper() {
        String[] names = { "Alice", "Bob", "Charlie" };
        EntryStream<Integer, String> stream = EntryStream.of(names, String::length);
        Map<Integer, String> result = stream.toMap();

        assertEquals(3, result.size());
    }

    @Test
    public void testOfArrayWithKeyValueMappers() {
        String[] words = { "hello", "world", "java" };
        EntryStream<String, Integer> stream = EntryStream.of(words, String::toUpperCase, String::length);
        Map<String, Integer> result = stream.toMap();

        assertEquals(3, result.size());
        assertEquals(5, result.get("HELLO"));
        assertEquals(5, result.get("WORLD"));
        assertEquals(4, result.get("JAVA"));
    }

    @Test
    public void testOfCollectionWithKeyMapper() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        EntryStream<Integer, String> stream = EntryStream.of(names, String::length);
        Map<Integer, String> result = stream.toMap();

        assertEquals(3, result.size());
    }

    @Test
    public void testOfCollectionWithKeyValueMappers() {
        List<String> words = Arrays.asList("hello", "world", "java");
        EntryStream<String, Integer> stream = EntryStream.of(words, String::toUpperCase, String::length);
        Map<String, Integer> result = stream.toMap();

        assertEquals(3, result.size());
    }

    @Test
    public void testOfEntries() {
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("b", 2);
        assertEquals(2, EntryStream.of(N.toList(entry1, entry2)).count());
    }

    @Test
    public void testOfFiveEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        Map<String, Integer> map = stream.toMap();

        assertEquals(5, map.size());
    }

    @Test
    public void testOfFourEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4);
        Map<String, Integer> map = stream.toMap();

        assertEquals(4, map.size());
    }

    @Test
    public void testOfIterable() {
        Set<Map.Entry<String, Integer>> entrySet = testMap.entrySet();
        EntryStream<String, Integer> stream = EntryStream.of(entrySet);
        Map<String, Integer> result = stream.toMap();

        assertEquals(testMap, result);
    }

    @Test
    public void testOfKeysAndValues() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);

        Map<String, Integer> map = EntryStream.of(keys, k -> k, v -> values.get(keys.indexOf(v))).toMap();
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());

        Map<Integer, String> valKeyMap = EntryStream.of(values, v -> v, k -> keys.get(values.indexOf(k))).toMap();
        assertEquals("a", valKeyMap.get(1));
        assertEquals("b", valKeyMap.get(2));
    }

    @Test
    public void testOfMap() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.toMap();

        assertEquals(testMap, result);
    }

    @Test
    public void testOfMultimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);

        EntryStream<String, Collection<Integer>> stream = EntryStream.of(multimap);
        Map<String, Collection<Integer>> result = stream.toMap();

        assertEquals(2, result.size());
        assertEquals(2, result.get("a").size());
        assertEquals(1, result.get("b").size());
    }

    @Test
    public void testOfNullable() {
        Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 100);
        assertEquals(1, EntryStream.ofNullable(entry).count());
        assertEquals(0, EntryStream.ofNullable(null).count());
    }

    @Test
    public void testOfSevenEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        Map<String, Integer> map = stream.toMap();

        assertEquals(7, map.size());
    }

    @Test
    public void testOfSingleEntry() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1);
        Map<String, Integer> map = stream.toMap();

        assertEquals(1, map.size());
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testOfSixEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        Map<String, Integer> map = stream.toMap();

        assertEquals(6, map.size());
    }

    @Test
    public void testOfThreeEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map = stream.toMap();

        assertEquals(3, map.size());
    }

    @Test
    public void testOfTwoEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Map<String, Integer> map = stream.toMap();

        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testOfWithArraysAndMappers() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Map<Integer, String> lengthToName = EntryStream.of(names, String::length).toMap((v1, v2) -> v1 + "," + v2);

        assertEquals("Alice", lengthToName.get(5));
        assertEquals("Bob", lengthToName.get(3));
        assertEquals("Charlie", lengthToName.get(7));

        Integer[] numbers = { 1, 2, 3, 4, 5 };
        Map<String, Integer> evenOdd = EntryStream.of(numbers, n -> n % 2 == 0 ? "even" : "odd", n -> n * 10).toMap(Integer::sum);

        assertEquals(60, evenOdd.get("even"));
        assertEquals(90, evenOdd.get("odd"));
    }

    @Test
    public void testOfWithIterableAndMappers() {
        List<String> cities = Arrays.asList("New York", "London", "Paris", "Tokyo");
        Map<Character, String> firstLetterToCity = EntryStream.of(cities, s -> s.charAt(0)).toMap((v1, v2) -> v1 + " & " + v2);

        assertEquals("New York", firstLetterToCity.get('N'));
        assertEquals("London", firstLetterToCity.get('L'));

        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
        Map<Boolean, Integer> evenOddSum = EntryStream.of(nums, n -> n % 2 == 0, n -> n).toMap(Integer::sum);

        assertEquals(6, evenOddSum.get(true));
        assertEquals(9, evenOddSum.get(false));
    }

    @Test
    public void testOfWithIterableOfEntries() {
        Set<Entry<String, Integer>> entrySet = new LinkedHashSet<>();
        entrySet.add(new AbstractMap.SimpleEntry<>("x", 10));
        entrySet.add(new AbstractMap.SimpleEntry<>("y", 20));
        entrySet.add(new AbstractMap.SimpleEntry<>("z", 30));

        Map<String, Integer> result = EntryStream.of(entrySet).toMap();
        assertEquals(3, result.size());
        assertEquals(10, result.get("x"));
        assertEquals(20, result.get("y"));
        assertEquals(30, result.get("z"));
    }

    @Test
    public void testOfWithSingleEntry() {
        Map<String, Integer> map7 = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7).toMap();

        assertEquals(7, map7.size());
        assertEquals(1, map7.get("a"));
        assertEquals(7, map7.get("g"));

        Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 100);
        Map<String, Integer> singleMap = EntryStream.of(Collections.singletonList(entry)).toMap();
        assertEquals(1, singleMap.size());
        assertEquals(100, singleMap.get("key"));
    }

}
