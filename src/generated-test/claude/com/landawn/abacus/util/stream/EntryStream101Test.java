package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u;


public class EntryStream101Test extends TestBase {

    private Map<String, Integer> testMap;
    private Map<Integer, String> numberMap;

    @BeforeEach
    public void setUp() {
        testMap = new LinkedHashMap<>();
        testMap.put("one", 1);
        testMap.put("two", 2);
        testMap.put("three", 3);
        testMap.put("four", 4);
        testMap.put("five", 5);

        numberMap = new LinkedHashMap<>();
        numberMap.put(1, "one");
        numberMap.put(2, "two");
        numberMap.put(3, "three");
    }

    @Test
    public void testEntries() {
        // Test entries() method returns the underlying stream
        Stream<Entry<String, Integer>> entriesStream = EntryStream.of(testMap).entries();
        assertEquals(5, entriesStream.count());

        // Verify we can operate on the returned stream
        List<String> keys = EntryStream.of(testMap).entries().map(Entry::getKey).toList();
        assertEquals(Arrays.asList("one", "two", "three", "four", "five"), keys);
    }

    @Test
    public void testSortedByIntLongDouble() {
        Map<String, Integer> unsortedMap = new LinkedHashMap<>();
        unsortedMap.put("c", 30);
        unsortedMap.put("a", 10);
        unsortedMap.put("b", 20);

        // Test sortedByInt
        List<Entry<String, Integer>> sortedByInt = EntryStream.of(unsortedMap).sortedByInt(Entry::getValue).toList();
        assertEquals(10, sortedByInt.get(0).getValue());
        assertEquals(20, sortedByInt.get(1).getValue());
        assertEquals(30, sortedByInt.get(2).getValue());

        // Test sortedByLong
        Map<String, Long> longMap = new LinkedHashMap<>();
        longMap.put("c", 30L);
        longMap.put("a", 10L);
        longMap.put("b", 20L);

        List<Entry<String, Long>> sortedByLong = EntryStream.of(longMap).sortedByLong(Entry::getValue).toList();
        assertEquals(10L, sortedByLong.get(0).getValue());
        assertEquals(20L, sortedByLong.get(1).getValue());
        assertEquals(30L, sortedByLong.get(2).getValue());

        // Test sortedByDouble
        Map<String, Double> doubleMap = new LinkedHashMap<>();
        doubleMap.put("c", 3.5);
        doubleMap.put("a", 1.5);
        doubleMap.put("b", 2.5);

        List<Entry<String, Double>> sortedByDouble = EntryStream.of(doubleMap).sortedByDouble(Entry::getValue).toList();
        assertEquals(1.5, sortedByDouble.get(0).getValue());
        assertEquals(2.5, sortedByDouble.get(1).getValue());
        assertEquals(3.5, sortedByDouble.get(2).getValue());
    }

    @Test
    public void testReverseSortedBy() {
        Map<String, Integer> unsortedMap = new LinkedHashMap<>();
        unsortedMap.put("a", 10);
        unsortedMap.put("c", 30);
        unsortedMap.put("b", 20);

        // Test reverseSortedBy with comparable
        List<Entry<String, Integer>> reverseSorted = EntryStream.of(unsortedMap).reverseSortedBy(Entry::getValue).toList();
        assertEquals(30, reverseSorted.get(0).getValue());
        assertEquals(20, reverseSorted.get(1).getValue());
        assertEquals(10, reverseSorted.get(2).getValue());
    }

    @Test
    public void testDistinctWithMergeFunction() {
        // Create entries with duplicate keys
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3), new AbstractMap.SimpleEntry<>("b", 4), new AbstractMap.SimpleEntry<>("c", 5));

        // Test distinct with merge function that sums values
        Map<String, Integer> result = EntryStream.of(entries)
                .distinct((e1, e2) -> e1.getKey().equals(e2.getKey()) ? new AbstractMap.SimpleEntry<>(e1.getKey(), e1.getValue() + e2.getValue()) : e1)
                .toMap(Fn.ignoringMerger());

        assertEquals(3, result.size());
        // Values should be merged for duplicate keys
        assertTrue(result.get("a") == 1 || result.get("a") == 4); // Depends on implementation
        assertTrue(result.get("b") == 2 || result.get("b") == 6);
        assertEquals(5, result.get("c"));
    }

    @Test
    public void testDistinctByWithMergeFunction() {
        // Test distinctBy with key mapper and merge function
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("aa", 1), new AbstractMap.SimpleEntry<>("bb", 2),
                new AbstractMap.SimpleEntry<>("ab", 3), new AbstractMap.SimpleEntry<>("ba", 4));

        // Group by first character and sum values
        List<Entry<String, Integer>> result = EntryStream.of(entries)
                .distinctBy(e -> e.getKey().charAt(0), (e1, e2) -> new AbstractMap.SimpleEntry<>(e1.getKey(), e1.getValue() + e2.getValue()))
                .toList();

        assertEquals(2, result.size()); // Only 'a' and 'b' as first chars
    }

    @Test
    public void testPercentiles() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 1; i <= 100; i++) {
            map.put("key" + i, i);
        }

        // Test percentiles with comparator
        com.landawn.abacus.util.u.Optional<Map<Percentage, Entry<String, Integer>>> percentiles = EntryStream.of(map)
                .percentiles(Comparator.comparing(Entry::getValue));

        assertTrue(percentiles.isPresent());
        Map<Percentage, Entry<String, Integer>> percMap = percentiles.get();

        // Check some percentile values
        assertTrue(percMap.containsKey(Percentage._0_0001));
        assertTrue(percMap.containsKey(Percentage._20));
        assertTrue(percMap.containsKey(Percentage._50));
        assertTrue(percMap.containsKey(Percentage._70));
        assertTrue(percMap.containsKey(Percentage._99_9999));

        // Verify ordering
        assertTrue(percMap.get(Percentage._0_0001).getValue() <= percMap.get(Percentage._50).getValue());
        assertTrue(percMap.get(Percentage._50).getValue() <= percMap.get(Percentage._99_9999).getValue());
    }

    @Test
    public void testThrowIfEmpty() {
        // Test with non-empty stream - should not throw
        EntryStream<String, Integer> result = EntryStream.of(testMap).throwIfEmpty();
        assertEquals(5, result.count());

        // Test with empty stream - should throw NoSuchElementException
        assertThrows(NoSuchElementException.class, () -> {
            EntryStream.<String, Integer> empty().throwIfEmpty().count();
        });

        // Test with custom exception
        assertThrows(IllegalStateException.class, () -> {
            EntryStream.<String, Integer> empty().throwIfEmpty(() -> new IllegalStateException("Stream is empty")).count();
        });
    }

    @Test
    public void testPrintln() {
        // Just verify it doesn't throw exception
        // Note: This will print to console during test
        EntryStream.of("a", 1, "b", 2).println();

        // Empty stream should also work
        EntryStream.empty().println();
    }

    @Test
    public void testToMultiset() {
        // Create entries with duplicates
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2), new AbstractMap.SimpleEntry<>("c", 3));

        Multiset<Entry<String, Integer>> multiset = EntryStream.of(entries).toMultiset();

        assertEquals(5, multiset.size()); // Total elements
        assertEquals(3, multiset.elementSet().size()); // Unique elements
        assertEquals(2, multiset.count(new AbstractMap.SimpleEntry<>("a", 1)));
        assertEquals(2, multiset.count(new AbstractMap.SimpleEntry<>("b", 2)));
        assertEquals(1, multiset.count(new AbstractMap.SimpleEntry<>("c", 3)));
    }

    @Test
    public void testToCollection() {
        // Test with LinkedHashSet to preserve order
        LinkedHashSet<Entry<String, Integer>> set = EntryStream.of(testMap).toCollection(LinkedHashSet::new);

        assertEquals(5, set.size());
        Iterator<Entry<String, Integer>> iter = set.iterator();
        assertEquals("one", iter.next().getKey());
        assertEquals("two", iter.next().getKey());
    }

    @Test
    public void testToArray() {
        Object[] array = EntryStream.of("a", 1, "b", 2, "c", 3).toArray();

        assertEquals(3, array.length);
        assertTrue(array[0] instanceof Entry);
        assertEquals("a", ((Entry<?, ?>) array[0]).getKey());
        assertEquals(1, ((Entry<?, ?>) array[0]).getValue());
    }

    @Test
    public void testToImmutableList() {
        ImmutableList<Entry<String, Integer>> immutableList = EntryStream.of("a", 1, "b", 2).toImmutableList();

        assertEquals(2, immutableList.size());

        // Verify it's immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            immutableList.add(new AbstractMap.SimpleEntry<>("c", 3));
        });
    }

    @Test
    public void testToImmutableSet() {
        ImmutableSet<Entry<String, Integer>> immutableSet = EntryStream.of("a", 1, "b", 2, "a", 1).toImmutableSet();

        assertEquals(2, immutableSet.size()); // Duplicates removed

        // Verify it's immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            immutableSet.add(new AbstractMap.SimpleEntry<>("c", 3));
        });
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", ":", "[", "]");

        EntryStream.of("a", 1, "b", 2, "c", 3).joinTo(joiner);

        String result = joiner.toString();
        assertEquals("[a:1, b:2, c:3]", result);
    }

    @Test
    public void testSps() {
        // Test sequential-parallel-sequential operations
        long count = EntryStream.of(testMap).sps(stream -> stream.filter(e -> e.getValue() > 2)).count();

        assertEquals(3, count);

        // Test with specific thread number
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .sps(2, stream -> stream.map(e -> N.newEntry(e.getKey().toUpperCase(), e.getValue() * 2)))
                .sortedBy(Fn.value())
                .toList();

        assertEquals(5, result.size());
        assertEquals("ONE", result.get(0).getKey());
        assertEquals(2, result.get(0).getValue());
    }

    @Test
    public void testPsp() {
        // Test parallel-sequential-parallel operations
        long count = EntryStream.of(testMap).parallel(2).psp(stream -> stream.filter(e -> e.getValue() <= 3)).count();

        assertEquals(3, count);
    }

    @Test
    public void testIsParallelAndSequential() {
        // Test sequential stream
        EntryStream<String, Integer> sequential = EntryStream.of(testMap);
        assertFalse(sequential.isParallel());

        // Test parallel stream
        EntryStream<String, Integer> parallel = EntryStream.of(testMap).parallel();
        assertTrue(parallel.isParallel());

        // Test converting back to sequential
        EntryStream<String, Integer> backToSeq = parallel.sequential();
        assertFalse(backToSeq.isParallel());
    }

    @Test
    public void testOfWithArraysAndMappers() {
        // Test of with array and key mapper
        String[] names = { "Alice", "Bob", "Charlie" };
        Map<Integer, String> lengthToName = EntryStream.of(names, String::length).toMap((v1, v2) -> v1 + "," + v2); // Merge function for duplicate keys

        assertEquals("Alice", lengthToName.get(5));
        assertEquals("Bob", lengthToName.get(3));
        assertEquals("Charlie", lengthToName.get(7));

        // Test of with array and both mappers
        Integer[] numbers = { 1, 2, 3, 4, 5 };
        Map<String, Integer> evenOdd = EntryStream.of(numbers, n -> n % 2 == 0 ? "even" : "odd", n -> n * 10).toMap(Integer::sum); // Sum values for same keys

        assertEquals(60, evenOdd.get("even")); // 20 + 40
        assertEquals(90, evenOdd.get("odd")); // 10 + 30 + 50
    }

    @Test
    public void testOfWithIterableAndMappers() {
        // Test of with Iterable and key mapper
        List<String> cities = Arrays.asList("New York", "London", "Paris", "Tokyo");
        Map<Character, String> firstLetterToCity = EntryStream.of(cities, s -> s.charAt(0)).toMap((v1, v2) -> v1 + " & " + v2);

        assertEquals("New York", firstLetterToCity.get('N'));
        assertEquals("London", firstLetterToCity.get('L'));

        // Test of with Iterable and both mappers
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
        Map<Boolean, Integer> evenOddSum = EntryStream.of(nums, n -> n % 2 == 0, n -> n).toMap(Integer::sum);

        assertEquals(6, evenOddSum.get(true)); // 2 + 4
        assertEquals(9, evenOddSum.get(false)); // 1 + 3 + 5
    }

    @Test
    public void testOfWithIteratorAndMappers() {
        // Test of with Iterator and key mapper
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        Map<Integer, String> lengthToWord = EntryStream.of(words.iterator(), String::length).toMap((v1, v2) -> v1 + "," + v2);

        assertEquals("apple", lengthToWord.get(5));
        assertEquals("banana,cherry", lengthToWord.get(6));

        // Test of with Iterator and both mappers
        Iterator<Integer> iter = Arrays.asList(10, 20, 30, 40).iterator();
        Map<String, String> rangeToValue = EntryStream.of(iter, n -> n <= 20 ? "low" : "high", n -> "val" + n).toMap((v1, v2) -> v1 + "," + v2);

        assertEquals("val10,val20", rangeToValue.get("low"));
        assertEquals("val30,val40", rangeToValue.get("high"));
    }

    @Test
    public void testZipWithDefaultValues() {
        // Test zip arrays with default values
        String[] keys = { "a", "b" };
        Integer[] values = { 1, 2, 3, 4 };

        Map<String, Integer> zipped = EntryStream.zip(keys, values, "default", 99).toMap(Fn.replacingMerger());
        assertEquals(3, zipped.size());
        assertEquals(1, zipped.get("a"));
        assertEquals(2, zipped.get("b"));
        assertEquals(4, zipped.get("default")); // Multiple defaults will overwrite

        // Test with iterables
        List<String> keyList = Arrays.asList("x", "y", "z");
        List<Integer> valueList = Arrays.asList(10);

        List<Entry<String, Integer>> zippedList = EntryStream.zip(keyList, valueList, "none", 0).toList();
        assertEquals(3, zippedList.size());
        assertEquals(10, zippedList.get(0).getValue());
        assertEquals(0, zippedList.get(1).getValue());
        assertEquals(0, zippedList.get(2).getValue());
    }

    @Test
    public void testConcatWithCollection() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);
        map2.put("d", 4);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("e", 5);

        // Test concat with collection
        List<Map<String, Integer>> maps = Arrays.asList(map1, map2, map3);
        Map<String, Integer> concatenated = EntryStream.concat(maps).toMap();

        assertEquals(5, concatenated.size());
        assertEquals(1, concatenated.get("a"));
        assertEquals(5, concatenated.get("e"));
    }

    @Test
    public void testMergeMultipleMaps() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("d", 4);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("e", 5);

        Map<String, Integer> map3 = new LinkedHashMap<>();
        map3.put("c", 3);
        map3.put("f", 6);

        // Test merge with 3 maps
        List<Entry<String, Integer>> merged = EntryStream
                .merge(map1, map2, map3, (e1, e2) -> e1.getKey().compareTo(e2.getKey()) < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList();

        assertEquals(6, merged.size());
        assertEquals("a", merged.get(0).getKey());
        assertEquals("b", merged.get(1).getKey());
        assertEquals("c", merged.get(2).getKey());

        // Test merge with collection of maps
        List<Map<String, Integer>> mapList = Arrays.asList(map1, map2, map3);
        List<Entry<String, Integer>> mergedFromCollection = EntryStream
                .merge(mapList, (e1, e2) -> e1.getValue() < e2.getValue() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList();

        assertEquals(6, mergedFromCollection.size());
    }

    @Test
    public void testOfMultimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);
        multimap.put("b", 4);
        multimap.put("c", 5);

        Map<String, List<Integer>> result = EntryStream.of(multimap).toMap();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get("a"));
        assertEquals(Arrays.asList(3, 4), result.get("b"));
        assertEquals(Arrays.asList(5), result.get("c"));
    }

    @Test
    public void testSkipWithAction() {
        List<Entry<String, Integer>> skipped = new ArrayList<>();

        List<Entry<String, Integer>> remaining = EntryStream.of(testMap).skip(2, skipped::add).toList();

        assertEquals(3, remaining.size());
        assertEquals(2, skipped.size());
        assertEquals("one", skipped.get(0).getKey());
        assertEquals("two", skipped.get(1).getKey());
        assertEquals("three", remaining.get(0).getKey());
    }

    @Test
    public void testDropWhileWithAction() {
        List<Entry<String, Integer>> dropped = new ArrayList<>();

        List<Entry<String, Integer>> remaining = EntryStream.of(testMap).dropWhile(e -> e.getValue() < 3, dropped::add).toList();

        assertEquals(3, remaining.size());
        assertEquals(2, dropped.size());
        assertEquals("one", dropped.get(0).getKey());
        assertEquals("two", dropped.get(1).getKey());
    }

    @Test
    public void testFilterWithAction() {
        List<Entry<String, Integer>> filtered = new ArrayList<>();

        List<Entry<String, Integer>> passed = EntryStream.of(testMap).filter(e -> e.getValue() > 2, filtered::add).toList();

        assertEquals(3, passed.size());
        assertEquals(2, filtered.size());
        assertEquals("one", filtered.get(0).getKey());
        assertEquals("two", filtered.get(1).getKey());
    }

    @Test
    public void testMapMultiWithBiConsumer() {
        // Test mapMulti that produces multiple entries for each input
        List<Entry<String, Integer>> result = EntryStream.of("a", 2, "b", 3).<String, Integer> mapMulti((entry, consumer) -> {
            String key = entry.getKey();
            int value = entry.getValue();
            for (int i = 1; i <= value; i++) {
                consumer.accept(new AbstractMap.SimpleEntry<>(key + i, i));
            }
        }).toList();

        assertEquals(5, result.size()); // 2 entries for "a", 3 for "b"
        assertEquals("a1", result.get(0).getKey());
        assertEquals(1, result.get(0).getValue());
        assertEquals("b3", result.get(4).getKey());
        assertEquals(3, result.get(4).getValue());
    }

    @Test
    public void testGroupByWithDownstream() {
        // Create data with duplicate keys
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3), new AbstractMap.SimpleEntry<>("b", 4), new AbstractMap.SimpleEntry<>("a", 5));

        // Test groupBy with downstream collector that sums values
        Map<String, Integer> summed = EntryStream.of(entries).groupBy(Collectors.summingInt(Entry::getValue)).toMap();

        assertEquals(9, summed.get("a")); // 1 + 3 + 5
        assertEquals(6, summed.get("b")); // 2 + 4

        // Test with custom downstream collector
        Map<String, String> concatenated = EntryStream.of(entries)
                .groupBy(Collectors.mapping(Entry::getValue, Collectors.mapping(Object::toString, Collectors.joining(","))))
                .toMap();

        assertEquals("1,3,5", concatenated.get("a"));
        assertEquals("2,4", concatenated.get("b"));
    }

    @Test
    public void testGroupByWithKeyMapperAndDownstream() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("apple", 5);
        data.put("apricot", 7);
        data.put("banana", 6);
        data.put("berry", 5);

        // Group by first letter and count
        Map<Character, Long> counts = EntryStream.of(data).groupBy(e -> e.getKey().charAt(0), Collectors.counting()).toMap();

        assertEquals(2L, counts.get('a')); // apple, apricot
        assertEquals(2L, counts.get('b')); // banana, berry
    }

    @Test
    public void testInversedToDisposableEntry() {
        // This is a deprecated method but still test it
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // Sequential only operation
        Map<Integer, String> inversed = EntryStream.of(map).inversedToDisposableEntry().toMap();

        assertEquals("a", inversed.get(1));
        assertEquals("b", inversed.get(2));
        assertEquals("c", inversed.get(3));
    }

    @Test
    public void testMapKeyWithBiFunction() {
        // Test mapKey with BiFunction that uses both key and value
        Map<String, Integer> result = EntryStream.of(testMap).mapKey((k, v) -> k + v).toMap();

        assertEquals(1, result.get("one1"));
        assertEquals(2, result.get("two2"));
        assertEquals(3, result.get("three3"));
    }

    @Test
    public void testMapValueWithBiFunction() {
        // Test mapValue with BiFunction that uses both key and value
        Map<String, String> result = EntryStream.of(testMap).mapValue((k, v) -> k + "=" + v).toMap();

        assertEquals("one=1", result.get("one"));
        assertEquals("two=2", result.get("two"));
        assertEquals("three=3", result.get("three"));
    }

    @Test
    public void testMapKeyPartialWithBiFunction() {
        // Test mapKeyPartial with BiFunction
        Map<String, Integer> result = EntryStream.of(testMap)
                .mapKeyPartial((k, v) -> v > 2 ? u.Optional.of(k.toUpperCase()) : u.Optional.<String> empty())
                .toMap();

        assertEquals(3, result.size());
        assertEquals(3, result.get("THREE"));
        assertEquals(4, result.get("FOUR"));
        assertEquals(5, result.get("FIVE"));
    }

    @Test
    public void testMapValuePartialWithBiFunction() {
        // Test mapValuePartial with BiFunction
        Map<String, Integer> result = EntryStream.of(testMap)
                .mapValuePartial((k, v) -> k.length() > 3 ? u.Optional.of(v * 100) : u.Optional.<Integer> empty())
                .toMap();

        assertEquals(3, result.size());
        assertEquals(300, result.get("three"));
        assertEquals(400, result.get("four"));
        assertEquals(500, result.get("five"));
    }

    @Test
    public void testFlatMapKeyWithBiFunction() {
        // Test flatMapKey with BiFunction
        Map<String, Integer> result = EntryStream.of("a", 2, "b", 3).flatMapKey((k, v) -> Stream.of(k, k + v)).toMap((v1, v2) -> v1); // Keep first value for duplicates

        assertEquals(4, result.size());
        assertEquals(2, result.get("a"));
        assertEquals(2, result.get("a2"));
        assertEquals(3, result.get("b"));
        assertEquals(3, result.get("b3"));
    }

    @Test
    public void testFlatMapValueWithBiFunction() {
        // Test flatMapValue with BiFunction
        List<Entry<String, Integer>> result = EntryStream.of("a", 2, "b", 3).flatMapValue((k, v) -> Stream.range(1, v + 1)).toList();

        assertEquals(5, result.size()); // 2 for "a", 3 for "b"
        assertEquals("a", result.get(0).getKey());
        assertEquals(1, result.get(0).getValue());
        assertEquals("b", result.get(2).getKey());
        assertEquals(1, result.get(2).getValue());
    }

    @Test
    public void testFlatmapKeyWithBiFunction() {
        // Test flatmapKey with BiFunction that returns Collection
        Map<String, Integer> result = EntryStream.of("x", 2, "y", 3).flatmapKey((k, v) -> Arrays.asList(k, k.repeat(v))).toMap((v1, v2) -> v1); // Keep first value for duplicates

        assertEquals(4, result.size());
        assertEquals(2, result.get("x"));
        assertEquals(2, result.get("xx"));
        assertEquals(3, result.get("y"));
        assertEquals(3, result.get("yyy"));
    }

    @Test
    public void testFlatmapValueWithBiFunction() {
        // Test flatmapValue with BiFunction that returns Collection
        List<Entry<String, String>> result = EntryStream.of("a", 2, "b", 1).flatmapValue((k, v) -> {
            List<String> values = new ArrayList<>();
            for (int i = 1; i <= v; i++) {
                values.add(k + i);
            }
            return values;
        }).toList();

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("a1", result.get(0).getValue());
        assertEquals("a", result.get(1).getKey());
        assertEquals("a2", result.get(1).getValue());
        assertEquals("b", result.get(2).getKey());
        assertEquals("b1", result.get(2).getValue());
    }

    @Test
    public void testSlidingWithIncrement() {
        List<List<Entry<String, Integer>>> windows = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).sliding(3, 2).toList();

        assertEquals(2, windows.size());
        assertEquals(3, windows.get(0).size());
        assertEquals("a", windows.get(0).get(0).getKey());
        assertEquals("b", windows.get(0).get(1).getKey());
        assertEquals("c", windows.get(0).get(2).getKey());

        assertEquals(3, windows.get(1).size());
        assertEquals("c", windows.get(1).get(0).getKey());
        assertEquals("d", windows.get(1).get(1).getKey());
        assertEquals("e", windows.get(1).get(2).getKey());
    }

    @Test
    public void testSlidingAndSplitWithCollectionSupplier() {
        // Test sliding with collection supplier
        List<LinkedList<Entry<String, Integer>>> windows = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).sliding(2, Factory.ofLinkedList()).toList();

        assertEquals(3, windows.size());
        assertTrue(windows.get(0) instanceof LinkedList);
        assertEquals(2, windows.get(0).size());

        // Test split with collection supplier
        List<ArrayList<Entry<String, Integer>>> chunks = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).split(2, ArrayList::new).toList();

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0) instanceof ArrayList);
        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testCollapseByKeyAndValueWithMapperAndCollector() {
        // Test collapseByKey with mapper and collector
        List<String> collapsed = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4, "c", 5)
                .collapseByKey((k1, k2) -> k1.equals(k2), Entry::getValue, Collectors.mapping(Object::toString, Collectors.joining(",")))
                .toList();

        assertEquals(3, collapsed.size());
        assertEquals("1,2", collapsed.get(0));
        assertEquals("3,4", collapsed.get(1));
        assertEquals("5", collapsed.get(2));

        // Test collapseByValue with mapper and collector
        List<String> collapsedByValue = EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2)
                .collapseByValue((v1, v2) -> v1.equals(v2), Entry::getKey, Collectors.joining("-"))
                .toList();

        assertEquals(2, collapsedByValue.size());
        assertEquals("a-b", collapsedByValue.get(0));
        assertEquals("c-d", collapsedByValue.get(1));
    }

    @Test
    public void testDelayAndRateLimited() {
        // Test delay - just verify it doesn't throw
        long startTime = System.currentTimeMillis();
        List<Entry<String, Integer>> delayed = EntryStream.of("a", 1, "b", 2).delay(Duration.ofMillis(10)).toList();
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(2, delayed.size());
        assertTrue(duration >= 10); // Should have some delay

        // Test rateLimited
        RateLimiter rateLimiter = RateLimiter.create(100); // 100 permits per second
        List<Entry<String, Integer>> limited = EntryStream.of(testMap).rateLimited(rateLimiter).toList();

        assertEquals(5, limited.size());
    }

    @Test
    public void testSortedAndReverseSorted() {
        // Test deprecated sorted() method
        List<Entry<String, Integer>> sorted = EntryStream.of("b", 2, "a", 1, "c", 3).sorted().toList();

        // Should be sorted by key then value
        assertEquals("a", sorted.get(0).getKey());
        assertEquals("b", sorted.get(1).getKey());
        assertEquals("c", sorted.get(2).getKey());

        // Test deprecated reverseSorted() method
        List<Entry<String, Integer>> reverseSorted = EntryStream.of("b", 2, "a", 1, "c", 3).reverseSorted().toList();

        assertEquals("c", reverseSorted.get(0).getKey());
        assertEquals("b", reverseSorted.get(1).getKey());
        assertEquals("a", reverseSorted.get(2).getKey());
    }

    @Test
    public void testReverseSortedWithComparator() {
        // Test reverseSorted with custom comparator
        List<Entry<String, Integer>> reverseSorted = EntryStream.of(testMap).reverseSorted(Map.Entry.comparingByValue()).toList();

        assertEquals(5, reverseSorted.get(0).getValue()); // Highest value first
        assertEquals(4, reverseSorted.get(1).getValue());
        assertEquals(3, reverseSorted.get(2).getValue());
        assertEquals(2, reverseSorted.get(3).getValue());
        assertEquals(1, reverseSorted.get(4).getValue());
    }

    @Test
    public void testMapPartialWithBiFunction() {
        // Test mapPartial with BiFunction
        List<Entry<String, String>> mapped = EntryStream.of(testMap)
                .mapPartial((k, v) -> v % 2 == 0 ? u.Optional.of(N.newEntry(k.toUpperCase(), v + "!")) : u.Optional.empty())
                .toList();

        assertEquals(2, mapped.size());
        assertEquals("TWO", mapped.get(0).getKey());
        assertEquals("2!", mapped.get(0).getValue());
        assertEquals("FOUR", mapped.get(1).getKey());
        assertEquals("4!", mapped.get(1).getValue());
    }

    @Test
    public void testToMapWithMapFactory() {
        // Test toMap with map factory
        TreeMap<String, Integer> treeMap = EntryStream.of("b", 2, "a", 1, "c", 3).toMap(TreeMap::new);

        assertTrue(treeMap instanceof TreeMap);
        assertEquals(3, treeMap.size());
        // TreeMap should be sorted by key
        assertEquals("a", treeMap.firstKey());
        assertEquals("c", treeMap.lastKey());

        // Test toMap with merge function and map factory
        LinkedHashMap<String, Integer> linkedMap = EntryStream.of("a", 1, "b", 2, "a", 3).toMap(Integer::sum, LinkedHashMap::new);

        assertTrue(linkedMap instanceof LinkedHashMap);
        assertEquals(4, linkedMap.get("a"));
        assertEquals(2, linkedMap.get("b"));
    }

    @Test
    public void testToMultimapWithMapFactory() {
        // Test toMultimap with custom factory
        Multimap<String, Integer, Set<Integer>> setMultimap = EntryStream.of("a", 1, "b", 2, "a", 1, "a", 3, "b", 2).toMultimap(Suppliers.ofSetMultimap());

        assertEquals(2, setMultimap.get("a").size()); // Set removes duplicates
        assertEquals(1, setMultimap.get("b").size());
    }

    @Test
    public void testGroupToWithMapFactory() {
        // Test groupTo with map factory
        TreeMap<String, List<Integer>> grouped = EntryStream.of("b", 2, "a", 1, "b", 4, "a", 3).groupTo(TreeMap::new);

        assertTrue(grouped instanceof TreeMap);
        assertEquals(Arrays.asList(1, 3), grouped.get("a"));
        assertEquals(Arrays.asList(2, 4), grouped.get("b"));
        assertEquals("a", grouped.firstKey()); // TreeMap is sorted
    }

    @Test
    public void testToImmutableMapWithMergeFunction() {
        // Test toImmutableMap with merge function
        ImmutableMap<String, Integer> immutable = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).toImmutableMap(Integer::sum);

        assertEquals(4, immutable.get("a")); // 1 + 3
        assertEquals(6, immutable.get("b")); // 2 + 4

        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            immutable.put("c", 5);
        });
    }
}
