package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.Optional;

/** EntryStream mapping contracts. */
public class EntryStreamMappingTest extends EntryStreamTestSupport {

    // flatMap

    @Test
    public void testFlatMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));

        List<Integer> result = EntryStream.of(map)
                .flatMap(entry -> EntryStream
                        .of(entry.getValue().stream().map(v -> new AbstractMap.SimpleEntry<>(entry.getKey(), v)).collect(Collectors.toList())))
                .values()
                .toList();

        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatMap_BiFunction() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 2)
                .flatMap((k, v) -> EntryStream.of(Stream.range(0, v).map(i -> new AbstractMap.SimpleEntry<>(k + i, i))))
                .toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlatMap_Function() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 2, "b", 1)
                .flatMap(e -> EntryStream.of(Stream.range(0, e.getValue()).map(i -> new AbstractMap.SimpleEntry<>(e.getKey() + i, i))))
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatMapWithBiFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);
        map.put("b", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flatMap((k, v) -> EntryStream.of(k + "1", v, k + "2", v * 2)).toMap();
        assertEquals(4, result.size());
        assertEquals(2, result.get("a1"));
        assertEquals(4, result.get("a2"));
        assertEquals(1, result.get("b1"));
        assertEquals(2, result.get("b2"));
    }

    @Test
    public void testFlatMapWithFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);
        map.put("b", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flatMap(e -> {
            Map<String, Integer> expanded = new HashMap<>();
            for (int i = 0; i < e.getValue(); i++) {
                expanded.put(e.getKey() + i, i);
            }
            return EntryStream.of(expanded);
        }).toMap();
        assertEquals(3, result.size());
        assertEquals(0, result.get("a0"));
        assertEquals(1, result.get("a1"));
        assertEquals(0, result.get("b0"));
    }

    // flatmap

    @Test
    public void testFlatmap() {
        final Map<Integer, List<String>> map = new HashMap<>();
        map.put(1, Arrays.asList("a", "b"));
        map.put(2, Arrays.asList("c", "d"));

        final Map<Integer, String> result = EntryStream.of(map).flatmap(e -> {
            final Map<Integer, String> newMap = new HashMap<>();
            for (final String s : e.getValue()) {
                newMap.put(e.getKey(), s);
            }
            return newMap;
        }).toMap((v1, v2) -> v2);

        assertEquals(2, result.size());
        assertEquals("b", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testFlatmap_BiFunction() {
        List<Entry<String, Integer>> result = EntryStream.of(multiValueMap).flatmap((k, v) -> {
            Map<String, Integer> map = new HashMap<>();
            for (Integer i : v) {
                map.put(k + i, i);
            }
            return map;
        }).toList();
        assertEquals(9, result.size());
    }

    @Test
    public void testFlatmap_Function() {
        List<Entry<String, Integer>> result = EntryStream.of(multiValueMap).flatmap(e -> {
            Map<String, Integer> map = new HashMap<>();
            for (Integer i : e.getValue()) {
                map.put(e.getKey() + i, i);
            }
            return map;
        }).toList();
        assertEquals(9, result.size());
    }

    @Test
    public void testFlatmapWithBiFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);
        map.put("b", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flatmap((k, v) -> {
            Map<String, Integer> m = new HashMap<>();
            m.put(k + "_x", v);
            m.put(k + "_y", v * 2);
            return m;
        }).toMap();
        assertEquals(4, result.size());
        assertEquals(2, result.get("a_x"));
        assertEquals(4, result.get("a_y"));
        assertEquals(1, result.get("b_x"));
        assertEquals(2, result.get("b_y"));
    }

    @Test
    public void testFlatmapWithFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("group1", 2);
        map.put("group2", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flatmap(e -> {
            Map<String, Integer> expanded = new HashMap<>();
            for (int i = 1; i <= e.getValue(); i++) {
                expanded.put(e.getKey() + "_item" + i, i);
            }
            return expanded;
        }).toMap();
        assertEquals(3, result.size());
        assertEquals(1, result.get("group1_item1"));
        assertEquals(2, result.get("group1_item2"));
        assertEquals(1, result.get("group2_item1"));
    }

    @Test
    public void testFlatMapEdgeCases() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 0).flatMapValue(v -> Stream.range(0, v)).toList();

        assertEquals(0, result.size());
    }

    // flatMapKey

    @Test
    public void testFlatMapKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a,b", 1);
        map.put("c,d", 2);

        final Map<String, Integer> result = EntryStream.of(map).flatMapKey(k -> Stream.of(k.split(","))).toMap();
        assertEquals(4, result.size());
        assertEquals(1, (int) result.get("a"));
        assertEquals(2, (int) result.get("d"));
    }

    @Test
    public void testFlatMapKey_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).flatMapKey((k, v) -> Stream.of(k, k.toUpperCase())).toMap((v1, v2) -> v1);
        assertEquals(10, result.size());
    }

    @Test
    public void testFlatMapKey_Function() {
        Map<String, Integer> result = EntryStream.of(testMap).flatMapKey(k -> Stream.of(k, k.toUpperCase())).toMap((v1, v2) -> v1);
        assertEquals(10, result.size());
    }

    @Test
    public void testFlatMapKeyNullMappedStreamIsEmpty() {
        assertTrue(EntryStream.of("a", 1).flatMapKey((Function<String, Stream<String>>) k -> null).toList().isEmpty());
        assertTrue(EntryStream.of("a", 1).flatMapKey((BiFunction<String, Integer, Stream<String>>) (k, v) -> null).toList().isEmpty());
    }

    @Test
    public void testFlatMapKeyWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Map<String, Integer> result = stream.flatMapKey((k, v) -> Stream.of(k + v, k + "-" + v)).toMap();
        assertEquals(4, result.size());
        assertEquals(1, result.get("a1"));
        assertEquals(1, result.get("a-1"));
        assertEquals(2, result.get("b2"));
        assertEquals(2, result.get("b-2"));
    }

    // flatmapKey

    @Test
    public void testFlatmapKey() {
        Map<List<String>, Integer> map = new HashMap<>();
        map.put(Arrays.asList("a", "b"), 1);
        map.put(Arrays.asList("c", "d"), 2);

        final Map<String, Integer> result = EntryStream.of(map).flatmapKey(k -> k).toMap(Fn.throwingMerger());

        assertEquals(4, result.size());
        assertEquals(1, result.get("a").intValue());
        assertEquals(1, result.get("b").intValue());
        assertEquals(2, result.get("c").intValue());
        assertEquals(2, result.get("d").intValue());

    }

    @Test
    public void testFlatmapKey_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).flatmapKey((k, v) -> Arrays.asList(k, k.toUpperCase())).toMap((v1, v2) -> v1);
        assertEquals(10, result.size());
    }

    @Test
    public void testFlatmapKey_Function() {
        Map<String, Integer> result = EntryStream.of(testMap).flatmapKey(k -> Arrays.asList(k, k.toUpperCase())).toMap((v1, v2) -> v1);
        assertEquals(10, result.size());
    }

    // G095-01: the parallel path (left on the Stream-based expansion) still yields every expanded entry
    @Test
    public void testFlatmapKeyValue_parallel() {
        final Map<Integer, Integer> source = new LinkedHashMap<>();

        for (int i = 0; i < 200; i++) {
            source.put(i, i * 2);
        }

        final List<Map.Entry<Integer, Integer>> expectedKeys = new ArrayList<>();
        final List<Map.Entry<Integer, Integer>> expectedValues = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            expectedKeys.add(g095Entry(i, i * 2));
            expectedKeys.add(g095Entry(i + 1000, i * 2));
            expectedValues.add(g095Entry(i, i * 2));
            expectedValues.add(g095Entry(i, -i * 2));
        }

        final Comparator<Map.Entry<Integer, Integer>> byKeyThenValue = Comparator.<Map.Entry<Integer, Integer>> comparingInt(Map.Entry::getKey)
                .thenComparingInt(Map.Entry::getValue);
        expectedKeys.sort(byKeyThenValue);
        expectedValues.sort(byKeyThenValue);

        final EntryStream<Integer, Integer> keyStream = EntryStream.of(source).parallel(4).flatmapKey((k, v) -> Arrays.asList(k, k + 1000));
        assertTrue(keyStream.isParallel());
        final List<Map.Entry<Integer, Integer>> keys = new ArrayList<>(keyStream.toList());
        keys.sort(byKeyThenValue);
        assertEquals(expectedKeys, keys);

        final List<Map.Entry<Integer, Integer>> values = new ArrayList<>(EntryStream.of(source).parallel(4).flatmapValue(v -> Arrays.asList(v, -v)).toList());
        values.sort(byKeyThenValue);
        assertEquals(expectedValues, values);
    }

    // G095-01: pins the order of keyMapper / collection iterator / Entry accessor calls and the laziness under a short-circuit
    @Test
    public void testFlatmapKeyValue_sequentialCallOrderAndLaziness() {
        final List<String> log = new ArrayList<>();
        final List<Map.Entry<String, Integer>> source = Arrays.asList(new G095RecordingEntry("a", 1, log), new G095RecordingEntry("b", 2, log),
                new G095RecordingEntry("c", 3, log));

        final List<Map.Entry<String, Integer>> keyResult = EntryStream.of(source).flatmapKey(k -> {
            log.add("map:" + k);
            return g095RecordingCollection(Arrays.asList(k + "1", k + "2", k + "3"), log);
        }).limit(4).toList();

        assertEquals(Arrays.asList(g095Entry("a1", 1), g095Entry("a2", 1), g095Entry("a3", 1), g095Entry("b1", 2)), keyResult);
        assertEquals(Arrays.asList("getKey:a", "map:a", "iterator", "next:a1", "getValue:1", "next:a2", "getValue:1", "next:a3", "getValue:1", "getKey:b",
                "map:b", "iterator", "next:b1", "getValue:2"), log);

        log.clear();

        final Map.Entry<String, Integer> first = EntryStream.of(source).flatmapValue((k, v) -> {
            log.add("map:" + k + "=" + v);
            return g095RecordingCollection(v == 1 ? Collections.<Integer> emptyList() : Arrays.asList(v * 10, v * 100), log);
        }).first().orElseThrow();

        assertEquals(g095Entry("b", 20), first);
        assertEquals(Arrays.asList("getKey:a", "getValue:1", "map:a=1", "getKey:b", "getValue:2", "map:b=2", "iterator", "next:20", "getKey:b"), log);
    }

    // G095-01: sequential flatmapKey/flatmapValue expand null, empty, singleton and duplicate collections as before
    @Test
    public void testFlatmapKeyValue_sequentialNullEmptyDuplicates() {
        final Map<String, Integer> source = new LinkedHashMap<>();
        source.put("n", 0);
        source.put("e", 1);
        source.put("s", 2);
        source.put("d", 3);

        final Function<String, Collection<String>> keyMapper = k -> k.equals("n") ? null
                : k.equals("e") ? Collections.emptyList() : k.equals("s") ? Collections.singletonList("S") : Arrays.asList("D", "D", null);
        final List<Map.Entry<String, Integer>> expectedKeys = Arrays.asList(g095Entry("S", 2), g095Entry("D", 3), g095Entry("D", 3), g095Entry(null, 3));

        assertEquals(expectedKeys, EntryStream.of(source).flatmapKey(keyMapper).toList());
        assertEquals(expectedKeys, EntryStream.of(source).flatmapKey((k, v) -> keyMapper.apply(k)).toList());

        final Function<Integer, Collection<Integer>> valueMapper = v -> v == 0 ? null
                : v == 1 ? new ArrayList<>() : v == 2 ? Collections.singleton(20) : new LinkedList<>(Arrays.asList(30, 30, null));
        final List<Map.Entry<String, Integer>> expectedValues = Arrays.asList(g095Entry("s", 20), g095Entry("d", 30), g095Entry("d", 30), g095Entry("d", null));

        assertEquals(expectedValues, EntryStream.of(source).flatmapValue(valueMapper).toList());
        assertEquals(expectedValues, EntryStream.of(source).flatmapValue((k, v) -> valueMapper.apply(v)).toList());

        assertEquals(0, EntryStream.<String, Integer> empty().flatmapKey(k -> Arrays.asList(k, k)).count());
        assertEquals(0, EntryStream.of("a", 1).flatmapValue(v -> null).count());
        assertEquals(Arrays.asList(g095Entry("x", 1), g095Entry("y", 1)),
                EntryStream.of("a", 1).flatmapKey(k -> new LinkedHashSet<>(Arrays.asList("x", "y"))).toList());
    }

    @Test
    public void testFlatmapKeyWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Map<String, Integer> result = stream.flatmapKey((k, v) -> Arrays.asList(k + v, k + "-" + v)).toMap();
        assertEquals(4, result.size());
        assertEquals(1, result.get("a1"));
        assertEquals(1, result.get("a-1"));
        assertEquals(2, result.get("b2"));
        assertEquals(2, result.get("b-2"));
    }

    @Test
    public void testFlatmapToEntry() {
        final List<String> list = N.toList("a", "b");
        final Map<String, Integer> map = Stream.of(list).flatmapToEntry(s -> N.asMap(s, s.hashCode())).toMap();

        assertNotEquals(0, map.size());
        assertEquals("a".hashCode(), map.get("a").intValue());
        assertEquals("b".hashCode(), map.get("b").intValue());

    }

    // flatMapValue

    @Test
    public void testFlatMapValue() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1,2");
        map.put("b", "3,4");

        final Map<String, Integer> result = EntryStream.of(map).flatMapValue(v -> Stream.of(v.split(",")).map(Integer::parseInt)).toMap((v1, v2) -> v2);

        assertEquals(2, result.size());
        assertEquals(2, (int) result.get("a"));
        assertEquals(4, (int) result.get("b"));
    }

    @Test
    public void testFlatMapValue_BiFunction() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 3).flatMapValue((k, v) -> Stream.range(1, v + 1)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatMapValue_Function() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 3).flatMapValue(v -> Stream.range(1, v + 1)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatMapValueNullMappedStreamIsEmpty() {
        assertTrue(EntryStream.of("a", 1).flatMapValue((Function<Integer, Stream<String>>) v -> null).toList().isEmpty());
        assertTrue(EntryStream.of("a", 1).flatMapValue((BiFunction<String, Integer, Stream<String>>) (k, v) -> null).toList().isEmpty());
    }

    @Test
    public void testFlatMapValueWithBiFunction() {
        Map<String, String> map = new HashMap<>();
        map.put("prefix", "a,b");
        map.put("suffix", "c,d");

        EntryStream<String, String> stream = EntryStream.of(map);
        ListMultimap<String, String> result = stream.flatMapValue((k, v) -> Stream.of(v.split(",")).map(s -> k + "-" + s)).toMultimap();
        assertEquals(2, result.get("prefix").size());
        assertTrue(result.get("prefix").contains("prefix-a"));
        assertTrue(result.get("prefix").contains("prefix-b"));
        assertEquals(2, result.get("suffix").size());
        assertTrue(result.get("suffix").contains("suffix-c"));
        assertTrue(result.get("suffix").contains("suffix-d"));
    }

    // flatmapValue

    @Test
    public void testFlatmapValue() {
        final Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));

        final List<Map.Entry<String, Integer>> res = EntryStream.of(map).flatmapValue(v -> v).toList();

        final List<Integer> values = Stream.of(res).map(Map.Entry::getValue).toList();
        final List<String> keys = Stream.of(res).map(Map.Entry::getKey).toList();
        assertEquals(4, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertTrue(values.contains(4));
        assertEquals(4, keys.size());
    }

    @Test
    public void testFlatmapValue_BiFunction() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 3).flatmapValue((k, v) -> Arrays.asList(v, v * 2)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlatmapValue_Function() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 3).flatmapValue(v -> Arrays.asList(v, v * 2, v * 3)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatmapValueWithBiFunction() {
        Map<String, String> map = new HashMap<>();
        map.put("prefix", "a,b");
        map.put("suffix", "c,d");

        EntryStream<String, String> stream = EntryStream.of(map);
        ListMultimap<String, String> result = stream
                .flatmapValue((k, v) -> Arrays.stream(v.split(",")).map(s -> k + "-" + s).collect(java.util.stream.Collectors.toList()))
                .toMultimap();
        assertEquals(2, result.get("prefix").size());
        assertTrue(result.get("prefix").contains("prefix-a"));
        assertTrue(result.get("prefix").contains("prefix-b"));
        assertEquals(2, result.get("suffix").size());
        assertTrue(result.get("suffix").contains("suffix-c"));
        assertTrue(result.get("suffix").contains("suffix-d"));
    }

    // map

    @Test
    public void testMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> result = EntryStream.of(map).map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey() + "!", entry.getValue() * 2)).toMap();
        assertEquals(4, (int) result.get("b!"));
        assertEquals(2, (int) result.get("a!"));

    }

    @Test
    public void testMap_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).map((k, v) -> new AbstractMap.SimpleEntry<>(k.toUpperCase(), v * 2)).toMap();
        assertEquals(2, result.get("ONE"));
    }

    @Test
    public void testMap_Function() {
        Map<String, String> result = EntryStream.of(testMap).map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toString())).toMap();
        assertEquals("1", result.get("one"));
    }

    @Test
    public void testMap_KeyAndValueFunction() {
        Map<String, String> result = EntryStream.of(testMap).map(Entry::getKey, e -> e.getValue().toString()).toMap();
        assertEquals("1", result.get("one"));
    }

    @Test
    public void testMapperFunctions() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        Map<String, Integer> identity = stream.map(e -> e).toMap();
        assertEquals(testMap, identity);

        EntryStream<String, Integer> stream2 = EntryStream.of("a", 1);
        Map<String, Integer> nullMapped = stream2.mapKey(k -> (String) null).toMap();
        assertEquals(1, nullMapped.get(null));
    }

    @Test
    public void testMapWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map((k, v) -> new AbstractMap.SimpleImmutableEntry<>(k.toUpperCase(), k + ":" + v)).toMap();
        assertEquals("one:1", mapped.get("ONE"));
        assertEquals("two:2", mapped.get("TWO"));
        assertEquals("three:3", mapped.get("THREE"));
    }

    @Test
    public void testMapWithBiFunctions() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<Integer, String> mapped = stream.map((k, v) -> v, (k, v) -> k + " has value " + v).toMap();
        assertEquals("one has value 1", mapped.get(1));
        assertEquals("two has value 2", mapped.get(2));
        assertEquals("three has value 3", mapped.get(3));
    }

    @Test
    public void testMapWithFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey().toUpperCase(), String.valueOf(e.getValue()))).toMap();
        assertEquals("1", mapped.get("ONE"));
        assertEquals("2", mapped.get("TWO"));
        assertEquals("3", mapped.get("THREE"));
    }

    @Test
    public void testMapWithKeyValueMappers() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map(e -> e.getKey().toUpperCase(), e -> "Value: " + e.getValue()).toMap();
        assertEquals("Value: 1", mapped.get("ONE"));
        assertEquals("Value: 2", mapped.get("TWO"));
        assertEquals("Value: 3", mapped.get("THREE"));
    }

    // mapKey

    @Test
    public void testMapKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> mappedKeyMap = EntryStream.of(map).mapKey(k -> k + "!").toMap();

        assertTrue(mappedKeyMap.containsKey("a!"));
        assertTrue(mappedKeyMap.containsKey("b!"));
    }

    @Test
    public void testMapKey_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapKey((k, v) -> k.toUpperCase() + v).toMap();
        assertEquals(1, result.get("ONE1"));
    }

    @Test
    public void testMapKeyWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> mapped = stream.mapKey((k, v) -> k + v).toMap();
        assertEquals(1, mapped.get("one1"));
        assertEquals(2, mapped.get("two2"));
        assertEquals(3, mapped.get("three3"));
    }

    // mapKeyPartial

    @Test
    public void testMapKeyPartial() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Map<String, Integer> result = EntryStream.of(map)
                .mapKeyPartial(k -> k.equals("a") ? com.landawn.abacus.util.u.Optional.of("aa") : com.landawn.abacus.util.u.Optional.empty())
                .toMap();
        assertEquals(1, result.size());
        assertEquals(1, result.get("aa").intValue());
    }

    @Test
    public void testMapKeyPartial_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapKeyPartial((k, v) -> v > 3 ? Optional.of(k.toUpperCase()) : Optional.<String> empty()).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testMapKeyPartial_BiFunction_Basic() {
        List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3)
                .mapKeyPartial((java.util.function.BiFunction<String, Integer, com.landawn.abacus.util.u.Optional<String>>) (k, v) -> {
                    if (v > 1) {
                        return com.landawn.abacus.util.u.Optional.of(k.toUpperCase());
                    }
                    return com.landawn.abacus.util.u.Optional.empty();
                })
                .toList();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> "B".equals(e.getKey())));
    }

    @Test
    public void testMapKeyPartial_Function() {
        Map<String, Integer> result = EntryStream.of(testMap)
                .mapKeyPartial(k -> k.startsWith("t") ? Optional.of(k.toUpperCase()) : Optional.<String> empty())
                .toMap();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("TWO"));
    }

    @Test
    public void testMapKeyPartial_Function_Basic() {
        List<Map.Entry<Integer, Integer>> result = EntryStream.of("1", 10, "2", 20, "notNum", 30)
                .mapKeyPartial((java.util.function.Function<String, com.landawn.abacus.util.u.Optional<Integer>>) k -> {
                    try {
                        return com.landawn.abacus.util.u.Optional.of(Integer.parseInt(k));
                    } catch (NumberFormatException e) {
                        return com.landawn.abacus.util.u.Optional.empty();
                    }
                })
                .toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testMapKeyPartialWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.mapKeyPartial((k, v) -> v > 1 ? Optional.of(k.toUpperCase()) : Optional.empty()).toMap();
        assertEquals(4, result.size());
        assertEquals(2, result.get("TWO"));
        assertEquals(3, result.get("THREE"));
    }

    @Test
    public void testMapKeyValuePartial() {
        Map<String, Integer> mappedKeys = EntryStream.of(testMap).mapKeyPartial(k -> k.length() > 3 ? Optional.of(k.toUpperCase()) : Optional.empty()).toMap();

        assertEquals(3, mappedKeys.size());
        assertTrue(mappedKeys.containsKey("THREE"));
        assertTrue(mappedKeys.containsKey("FOUR"));
        assertTrue(mappedKeys.containsKey("FIVE"));

        Map<String, Integer> mappedValues = EntryStream.of(testMap).mapValuePartial(v -> v % 2 == 0 ? Optional.of(v * 100) : Optional.empty()).toMap();

        assertEquals(2, mappedValues.size());
        assertEquals(200, mappedValues.get("two"));
        assertEquals(400, mappedValues.get("four"));
    }

    @Test
    public void testMapMulti() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        final long count = EntryStream.of(map).<String, Integer> mapMulti((e, c) -> {
            c.accept(e);
            c.accept(new AbstractMap.SimpleEntry<>(e.getKey() + "!", e.getValue() * 10));
        }).count();

        assertEquals(4, count);
    }

    @Test
    public void testMapMultiWithBiConsumer() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 2, "b", 3).<String, Integer> mapMulti((entry, consumer) -> {
            String key = entry.getKey();
            int value = entry.getValue();
            for (int i = 1; i <= value; i++) {
                consumer.accept(new AbstractMap.SimpleEntry<>(key + i, i));
            }
        }).toList();

        assertEquals(5, result.size());
        assertEquals("a1", result.get(0).getKey());
        assertEquals(1, result.get(0).getValue());
        assertEquals("b3", result.get(4).getKey());
        assertEquals(3, result.get(4).getValue());
    }

    // mapPartial

    @Test
    public void testMapPartial() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        final long count = EntryStream.of(map)
                .mapPartial(e -> e.getValue() > 1 ? com.landawn.abacus.util.u.Optional.of(e) : com.landawn.abacus.util.u.Optional.empty())
                .count();
        assertEquals(2, count);
    }

    @Test
    public void testMapPartialWithBiFunction() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Map<String, Integer> result = EntryStream.of(map).mapPartial((k, v) -> {
            if (v > 1) {
                return com.landawn.abacus.util.u.Optional.of(new AbstractMap.SimpleEntry<>(k, v * 2));
            }
            return com.landawn.abacus.util.u.Optional.empty();
        }).toMap();

        assertEquals(2, result.size());
        assertEquals(4, result.get("b").intValue());
        assertEquals(6, result.get("c").intValue());
    }

    @Test
    public void testMapPartialWithFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.mapPartial(
                e -> e.getValue() > 3 ? Optional.of(new AbstractMap.SimpleImmutableEntry<>(e.getKey().toUpperCase(), e.getValue() * 10)) : Optional.empty())
                .toMap();
        assertEquals(2, result.size());
        assertEquals(40, result.get("FOUR"));
        assertEquals(50, result.get("FIVE"));
    }

    // mapValue

    @Test
    public void testMapValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> mappedValueMap = EntryStream.of(map).mapValue(v -> v * 2).toMap();

        assertEquals(2, (int) mappedValueMap.get("a"));
        assertEquals(4, (int) mappedValueMap.get("b"));
    }

    @Test
    public void testMapValue_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapValue((k, v) -> v * 10).toMap();
        assertEquals(10, result.get("one"));
    }

    @Test
    public void testMapValue_Function() {
        Map<String, Integer> result = EntryStream.of(testMap).mapValue(v -> v * 10).toMap();
        assertEquals(10, result.get("one"));
    }

    @Test
    public void testMapValueWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.mapValue((k, v) -> k + " has value " + v).toMap();
        assertEquals("one has value 1", mapped.get("one"));
        assertEquals("two has value 2", mapped.get("two"));
        assertEquals("three has value 3", mapped.get("three"));
    }

    // mapValuePartial

    @Test
    public void testMapValuePartial() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Map<String, Integer> result = EntryStream.of(map)
                .mapValuePartial(v -> v > 1 ? com.landawn.abacus.util.u.Optional.of(v * 2) : com.landawn.abacus.util.u.Optional.empty())
                .toMap();
        assertEquals(2, result.size());
        assertEquals(4, result.get("b").intValue());
        assertEquals(6, result.get("c").intValue());

    }

    @Test
    public void testMapValuePartial_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapValuePartial((k, v) -> v > 3 ? Optional.of(v * 10) : Optional.<Integer> empty()).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testMapValuePartial_BiFunction_Basic() {
        List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3)
                .mapValuePartial((java.util.function.BiFunction<String, Integer, com.landawn.abacus.util.u.Optional<Integer>>) (k, v) -> {
                    if ("b".equals(k)) {
                        return com.landawn.abacus.util.u.Optional.of(v * 100);
                    }
                    return com.landawn.abacus.util.u.Optional.empty();
                })
                .toList();
        assertEquals(1, result.size());
        assertEquals(200, (int) result.get(0).getValue());
    }

    @Test
    public void testMapValuePartial_Function() {
        Map<String, Integer> result = EntryStream.of(testMap).mapValuePartial(v -> v > 3 ? Optional.of(v * 10) : Optional.<Integer> empty()).toMap();
        assertEquals(2, result.size());
        assertEquals(40, result.get("four"));
    }

    @Test
    public void testMapValuePartial_Function_Basic() {
        List<Map.Entry<String, Integer>> result = EntryStream.of("1", 1, "2", 2, "3", 3)
                .mapValuePartial((java.util.function.Function<Integer, com.landawn.abacus.util.u.Optional<Integer>>) v -> {
                    if (v % 2 == 0) {
                        return com.landawn.abacus.util.u.Optional.of(v * 10);
                    }
                    return com.landawn.abacus.util.u.Optional.empty();
                })
                .toList();
        assertEquals(1, result.size());
        assertEquals(20, (int) result.get(0).getValue());
    }

    @Test
    public void testMapValuePartialWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> result = stream.mapValuePartial((k, v) -> v > 3 ? Optional.of(k + ":" + v) : Optional.empty()).toMap();
        assertEquals(2, result.size());
        assertEquals("four:4", result.get("four"));
        assertEquals("five:5", result.get("five"));
    }

    // selectByKey

    @Test
    public void testSelectByKey() {
        Map<String, Object> mixed = new LinkedHashMap<>();
        mixed.put("a", 1);
        mixed.put("b", "text");
        mixed.put("c", 3);

        EntryStream<String, Integer> result = EntryStream.of(mixed)
                .filter(e -> e.getValue() instanceof Integer)
                .mapValue(v -> (Integer) v)
                .selectByKey(String.class);
        assertEquals(2, result.count());
    }

    @Test
    public void testSelectByKey_Basic() {
        Map<Object, Object> map = new HashMap<>();
        map.put("stringKey", "val1");
        map.put(42, "val2");
        map.put(3.14, "val3");

        List<Map.Entry<String, Object>> result = EntryStream.of(map).selectByKey(String.class).toList();
        assertEquals(1, result.size());
        assertEquals("stringKey", result.get(0).getKey());
    }

    @Test
    public void testSelectByKey_Parallel() {
        // selectByKey(Class) parallel branch
        Map<Object, Object> mixedMap = new LinkedHashMap<>();
        mixedMap.put("str", 1);
        mixedMap.put(42, 2);
        @SuppressWarnings("unchecked")
        List<Map.Entry<String, Object>> result = EntryStream.of(mixedMap).parallel().selectByKey(String.class).toList();
        assertEquals(1, result.size());
        assertEquals("str", result.get(0).getKey());
    }

    @Test
    public void testSelectByKeyValue() {
        EntryStream<Integer, String> selected = EntryStream.<Object, String> of(1, "one", "two", "2", 3, "three").selectByKey(Integer.class);

        Map<Integer, String> result = selected.toMap();
        assertEquals(2, result.size());
        assertEquals("one", result.get(1));
        assertEquals("three", result.get(3));

        EntryStream<String, Integer> selectedByValue = EntryStream.<String, Object> of("a", 1, "b", "two", "c", 3).selectByValue(Integer.class);

        assertEquals(2, selectedByValue.count());
    }

    // selectByValue

    @Test
    public void testSelectByValue() {
        Map<Object, Integer> mixed = new LinkedHashMap<>();
        mixed.put("a", 1);
        mixed.put(2, 2);
        mixed.put("c", 3);

        EntryStream<Object, Integer> result = EntryStream.of(mixed).selectByValue(Integer.class);
        assertEquals(3, result.count());
    }

    @Test
    public void testSelectByValue_Basic() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "strVal");
        map.put("b", 42);
        map.put("c", 3.14);

        List<Map.Entry<String, Integer>> result = EntryStream.of(map).selectByValue(Integer.class).toList();
        assertEquals(1, result.size());
        assertEquals(42, (int) result.get(0).getValue());
    }

    @Test
    public void testSelectByValue_Parallel() {
        // selectByValue(Class) parallel branch
        Map<String, Object> mixedValMap = new LinkedHashMap<>();
        mixedValMap.put("a", 1);
        mixedValMap.put("b", "text");
        @SuppressWarnings("unchecked")
        List<Map.Entry<String, Integer>> result = EntryStream.of(mixedValMap).parallel().selectByValue(Integer.class).toList();
        assertEquals(1, result.size());
        assertEquals(1, (int) result.get(0).getValue());
    }

    @Test
    public void testTransformViaJdkStream() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2);
        final List<String> result = EntryStream.of(map).entries().transformViaJdkStream(s -> s.map(Map.Entry::getKey)).toList();

        assertTrue(result.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testTransformViaStream_Deferred() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).<String, Integer> transformViaStream(s -> s.filter(e -> e.getValue() > 2), true).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testTransformViaStream_NoDeferred() {
        List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3)
                .<String, Integer> transformViaStream(s -> s.filter(e -> e.getValue() > 1))
                .toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testTransformViaStreamDeferred() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        EntryStream<String, String> transformed = stream
                .<String, String> transformViaStream(s -> s.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), String.valueOf(e.getValue()))), true);

        Map<String, String> result = transformed.toMap();
        assertEquals(testMap.size(), result.size());
    }

}
