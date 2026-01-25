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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Supplier;

@Tag("new-test")
public class EntryStream100Test extends TestBase {

    private Map<String, Integer> testMap;
    private Map<String, List<Integer>> multiValueMap;
    private Map<Integer, String> numberMap;

    @BeforeEach
    public void setUp() {
        testMap = new LinkedHashMap<>();
        testMap.put("one", 1);
        testMap.put("two", 2);
        testMap.put("three", 3);
        testMap.put("four", 4);
        testMap.put("five", 5);

        multiValueMap = new LinkedHashMap<>();
        multiValueMap.put("a", Arrays.asList(1, 2, 3));
        multiValueMap.put("b", Arrays.asList(4, 5));
        multiValueMap.put("c", Arrays.asList(6, 7, 8, 9));

        numberMap = new LinkedHashMap<>();
        numberMap.put(1, "one");
        numberMap.put(2, "two");
        numberMap.put(3, "three");
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
    public void testKeys() {
        List<String> keys = EntryStream.of(testMap).keys().toList();
        assertEquals(Arrays.asList("one", "two", "three", "four", "five"), keys);
    }

    @Test
    public void testValues() {
        List<Integer> values = EntryStream.of(testMap).values().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), values);
    }

    @Test
    public void testInversed() {
        Map<Integer, String> inversed = EntryStream.of(testMap).inversed().toMap();

        assertEquals("one", inversed.get(1));
        assertEquals("two", inversed.get(2));
        assertEquals("three", inversed.get(3));
    }

    @Test
    public void testFilter() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filter(e -> e.getValue() > 2).toList();
        assertEquals(3, filtered.size());

        List<Entry<String, Integer>> biFiltered = EntryStream.of(testMap).filter((k, v) -> k.length() > 3 && v > 2).toList();
        assertEquals(3, biFiltered.size());
    }

    @Test
    public void testFilterByKey() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filterByKey(k -> k.startsWith("t")).toList();
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByValue() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filterByValue(v -> v % 2 == 0).toList();
        assertEquals(2, filtered.size());
    }

    @Test
    public void testMap() {
        Map<String, String> mapped = EntryStream.of(testMap).map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toString())).toMap();
        assertEquals("1", mapped.get("one"));
        assertEquals("2", mapped.get("two"));

        Map<String, Double> mapped2 = EntryStream.of(testMap).map(e -> e.getKey().toUpperCase(), e -> e.getValue() * 2.0).toMap();
        assertEquals(2.0, mapped2.get("ONE"));
        assertEquals(4.0, mapped2.get("TWO"));
    }

    @Test
    public void testMapKey() {
        Map<String, Integer> mapped = EntryStream.of(testMap).mapKey(Fn.toUpperCase()).toMap();
        assertEquals(1, mapped.get("ONE"));
        assertEquals(2, mapped.get("TWO"));
    }

    @Test
    public void testMapValue() {
        Map<String, Integer> mapped = EntryStream.of(testMap).mapValue(v -> v * 10).toMap();
        assertEquals(10, mapped.get("one"));
        assertEquals(20, mapped.get("two"));
    }

    @Test
    public void testFlatMap() {
        List<Entry<String, Integer>> flattened = EntryStream.of(multiValueMap).flatmap((k, v) -> {
            Map<String, Integer> map = new HashMap<>();
            for (Integer i : v) {
                map.put(k + i, i);
            }
            return map;
        }).toList();

        assertEquals(9, flattened.size());
    }

    @Test
    public void testFlatMapKey() {
        Map<String, Integer> result = EntryStream.of(testMap).flatMapKey(k -> Stream.of(k, k.toUpperCase())).toMap((v1, v2) -> v1);

        assertEquals(10, result.size());
        assertEquals(1, result.get("one"));
        assertEquals(1, result.get("ONE"));
    }

    @Test
    public void testFlatMapValue() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 2, "b", 3).flatMapValue(v -> Stream.range(1, v + 1)).toList();

        assertEquals(5, result.size());
    }

    @Test
    public void testGroupBy() {
        Map<String, List<Integer>> grouped = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4, "a", 5).groupBy().toMap();

        assertEquals(Arrays.asList(1, 3, 5), grouped.get("a"));
        assertEquals(Arrays.asList(2, 4), grouped.get("b"));

        Map<String, Integer> summed = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4, "a", 5).groupBy(Integer::sum).toMap();

        assertEquals(9, summed.get("a"));
        assertEquals(6, summed.get("b"));
    }

    @Test
    public void testSortedByKey() {
        List<String> sortedKeys = EntryStream.of("z", 1, "a", 2, "m", 3).sortedByKey(Comparator.naturalOrder()).keys().toList();

        assertEquals(Arrays.asList("a", "m", "z"), sortedKeys);
    }

    @Test
    public void testSortedByValue() {
        List<Entry<String, Integer>> sorted = EntryStream.of("a", 3, "b", 1, "c", 2).sortedByValue(Comparator.naturalOrder()).toList();

        assertEquals(1, sorted.get(0).getValue());
        assertEquals(2, sorted.get(1).getValue());
        assertEquals(3, sorted.get(2).getValue());
    }

    @Test
    public void testDistinctByKey() {
        List<Entry<String, Integer>> distinct = EntryStream.of("a", 1, "b", 2, "a", 3, "c", 4).distinctByKey().toList();

        assertEquals(3, distinct.size());

        java.util.Optional<Entry<String, Integer>> aEntry = distinct.stream().filter(e -> "a".equals(e.getKey())).findFirst();
        assertTrue(aEntry.isPresent());
        assertEquals(1, aEntry.get().getValue());
    }

    @Test
    public void testDistinctByValue() {
        List<Entry<String, Integer>> distinct = EntryStream.of("a", 1, "b", 2, "c", 1, "d", 3).distinctByValue().toList();

        assertEquals(3, distinct.size());
    }

    @Test
    public void testLimit() {
        List<Entry<String, Integer>> limited = EntryStream.of(testMap).limit(3).toList();

        assertEquals(3, limited.size());
        assertEquals("one", limited.get(0).getKey());
        assertEquals("two", limited.get(1).getKey());
        assertEquals("three", limited.get(2).getKey());
    }

    @Test
    public void testSkip() {
        List<Entry<String, Integer>> skipped = EntryStream.of(testMap).skip(2).toList();

        assertEquals(3, skipped.size());
        assertEquals("three", skipped.get(0).getKey());
    }

    @Test
    public void testPeek() {
        List<String> peekedKeys = new ArrayList<>();

        long count = EntryStream.of(testMap).peek((k, v) -> peekedKeys.add(k)).count();

        assertEquals(5, count);
        assertEquals(5, peekedKeys.size());
        assertEquals(Arrays.asList("one", "two", "three", "four", "five"), peekedKeys);
    }

    @Test
    public void testForEach() {
        Map<String, Integer> collected = new HashMap<>();

        EntryStream.of(testMap).forEach((k, v) -> collected.put(k.toUpperCase(), v * 2));

        assertEquals(10, collected.get("FIVE"));
        assertEquals(8, collected.get("FOUR"));
    }

    @Test
    public void testToMap() {
        Map<String, Integer> map = EntryStream.of("a", 1, "b", 2, "c", 3).toMap();
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));

        Map<String, Integer> merged = EntryStream.of("a", 1, "b", 2, "a", 3).toMap(Integer::sum);

        assertEquals(4, merged.get("a"));
        assertEquals(2, merged.get("b"));
    }

    @Test
    public void testToMultimap() {
        ListMultimap<String, Integer> multimap = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).toMultimap();

        assertEquals(Arrays.asList(1, 3), multimap.get("a"));
        assertEquals(Arrays.asList(2, 4), multimap.get("b"));
    }

    @Test
    public void testJoin() {
        String joined = EntryStream.of("a", 1, "b", 2, "c", 3).join(", ");
        assertTrue(joined.contains("a=1"));
        assertTrue(joined.contains("b=2"));
        assertTrue(joined.contains("c=3"));

        String customJoined = EntryStream.of("x", 10, "y", 20).join("; ", "->", "[", "]");
        assertEquals("[x->10; y->20]", customJoined);
    }

    @Test
    public void testMinMax() {
        Optional<Entry<String, Integer>> minByKey = EntryStream.of(testMap).minByKey(Comparator.naturalOrder());
        assertTrue(minByKey.isPresent());
        assertEquals("five", minByKey.get().getKey());

        Optional<Entry<String, Integer>> maxByKey = EntryStream.of(testMap).maxByKey(Comparator.naturalOrder());
        assertTrue(maxByKey.isPresent());
        assertEquals("two", maxByKey.get().getKey());

        Optional<Entry<String, Integer>> minByValue = EntryStream.of(testMap).minByValue(Comparator.naturalOrder());
        assertTrue(minByValue.isPresent());
        assertEquals(1, minByValue.get().getValue());

        Optional<Entry<String, Integer>> maxByValue = EntryStream.of(testMap).maxByValue(Comparator.naturalOrder());
        assertTrue(maxByValue.isPresent());
        assertEquals(5, maxByValue.get().getValue());
    }

    @Test
    public void testAnyAllNoneMatch() {
        assertTrue(EntryStream.of(testMap).anyMatch(e -> e.getValue() > 4));
        assertTrue(EntryStream.of(testMap).anyMatch((k, v) -> k.equals("three") && v == 3));
        assertFalse(EntryStream.of(testMap).anyMatch(e -> e.getValue() > 10));

        assertTrue(EntryStream.of(testMap).allMatch(e -> e.getValue() > 0));
        assertTrue(EntryStream.of(testMap).allMatch((k, v) -> k.length() >= 3));
        assertFalse(EntryStream.of(testMap).allMatch(e -> e.getValue() > 3));

        assertTrue(EntryStream.of(testMap).noneMatch(e -> e.getValue() > 10));
        assertTrue(EntryStream.of(testMap).noneMatch((k, v) -> k.isEmpty()));
        assertFalse(EntryStream.of(testMap).noneMatch(e -> e.getValue() == 3));
    }

    @Test
    public void testFindFirstAnyLast() {
        Optional<Entry<String, Integer>> first = EntryStream.of(testMap).findFirst(e -> e.getValue() > 3);
        assertTrue(first.isPresent());
        assertEquals("four", first.get().getKey());

        Optional<Entry<String, Integer>> any = EntryStream.of(testMap).findAny((k, v) -> v % 2 == 0);
        assertTrue(any.isPresent());
        assertTrue(any.get().getValue() % 2 == 0);

        Optional<Entry<String, Integer>> last = EntryStream.of(testMap).findLast(e -> e.getValue() < 4);
        assertTrue(last.isPresent());
        assertEquals("three", last.get().getKey());
    }

    @Test
    public void testAppendPrepend() {
        List<Entry<String, Integer>> appended = EntryStream.of("a", 1).append(EntryStream.of("b", 2, "c", 3)).toList();
        assertEquals(3, appended.size());
        assertEquals("c", appended.get(2).getKey());

        List<Entry<String, Integer>> prepended = EntryStream.of("c", 3).prepend(EntryStream.of("a", 1, "b", 2)).toList();
        assertEquals(3, prepended.size());
        assertEquals("a", prepended.get(0).getKey());
    }

    @Test
    public void testConcat() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);
        map2.put("d", 4);

        List<Entry<String, Integer>> concatenated = EntryStream.concat(map1, map2).toList();
        assertEquals(4, concatenated.size());
    }

    @Test
    public void testZip() {
        String[] keys = { "a", "b", "c" };
        Integer[] values = { 1, 2, 3 };

        Map<String, Integer> zipped = EntryStream.zip(keys, values).toMap();
        assertEquals(3, zipped.size());
        assertEquals(1, zipped.get("a"));
        assertEquals(2, zipped.get("b"));
        assertEquals(3, zipped.get("c"));

        String[] moreKeys = { "x", "y", "z", "w" };
        Integer[] fewerValues = { 10, 20 };

        Map<String, Integer> partial = EntryStream.zip(moreKeys, fewerValues).toMap();
        assertEquals(2, partial.size());
    }

    @Test
    public void testCollect() {
        Map<Boolean, List<Entry<String, Integer>>> partitioned = EntryStream.of(testMap).collect(Collectors.partitioningBy(e -> e.getValue() % 2 == 0));

        assertEquals(2, partitioned.get(true).size());
        assertEquals(3, partitioned.get(false).size());

        StringBuilder sb = EntryStream.of("a", 1, "b", 2)
                .collect(StringBuilder::new, (acc, e) -> acc.append(e.getKey()).append(":").append(e.getValue()).append(" "), StringBuilder::append);

        assertEquals("a:1 b:2 ", sb.toString());
    }

    @Test
    public void testOnClose() {
        AtomicInteger closeCount = new AtomicInteger(0);

        try (EntryStream<String, Integer> stream = EntryStream.of(testMap)
                .onClose(() -> closeCount.incrementAndGet())
                .onClose(() -> closeCount.incrementAndGet())) {

            stream.count();
        }

        assertEquals(2, closeCount.get());
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

    @Test
    public void testTakeWhileDropWhile() {
        List<Entry<String, Integer>> taken = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 1, "e", 2).takeWhile(e -> e.getValue() <= 2).toList();

        assertEquals(2, taken.size());
        assertEquals("a", taken.get(0).getKey());
        assertEquals("b", taken.get(1).getKey());

        List<Entry<String, Integer>> dropped = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).dropWhile((k, v) -> v < 3).toList();

        assertEquals(2, dropped.size());
        assertEquals("c", dropped.get(0).getKey());
    }

    @Test
    public void testMapPartial() {
        List<Entry<String, Integer>> mapped = EntryStream.of(testMap)
                .mapPartial(e -> e.getValue() > 2 ? Optional.of(new AbstractMap.SimpleEntry<>(e.getKey().toUpperCase(), e.getValue() * 10)) : Optional.empty())
                .toList();

        assertEquals(3, mapped.size());
        assertTrue(mapped.stream().allMatch(e -> e.getValue() > 20));
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
    public void testsliding() {
        List<List<Entry<String, Integer>>> windows = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).sliding(2).toList();

        assertEquals(3, windows.size());
        assertEquals(2, windows.get(0).size());
        assertEquals("a", windows.get(0).get(0).getKey());
        assertEquals("b", windows.get(0).get(1).getKey());
    }

    @Test
    public void testSplit() {
        List<List<Entry<String, Integer>>> chunks = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).split(2).toList();

        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testIntersectionDifferenceSymmetricDifference() {
        List<Entry<String, Integer>> list1 = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("c", 3));

        List<Entry<String, Integer>> list2 = Arrays.asList(new AbstractMap.SimpleEntry<>("b", 2), new AbstractMap.SimpleEntry<>("c", 3),
                new AbstractMap.SimpleEntry<>("d", 4));

        List<Entry<String, Integer>> intersection = EntryStream.of(list1).intersection(list2).toList();
        assertEquals(2, intersection.size());

        List<Entry<String, Integer>> difference = EntryStream.of(list1).difference(list2).toList();
        assertEquals(1, difference.size());
        assertEquals("a", difference.get(0).getKey());

        List<Entry<String, Integer>> symDiff = EntryStream.of(list1).symmetricDifference(list2).toList();
        assertEquals(2, symDiff.size());
    }

    @Test
    public void testReduce() {
        Entry<String, Integer> identity = new AbstractMap.SimpleEntry<>("", 0);
        Entry<String, Integer> result = EntryStream.of(testMap)
                .reduce(identity, (acc, e) -> new AbstractMap.SimpleEntry<>(acc.getKey() + e.getKey(), acc.getValue() + e.getValue()));

        assertEquals("onetwothreefourfive", result.getKey());
        assertEquals(15, result.getValue());

        Optional<Entry<String, Integer>> reduced = EntryStream.of("a", 1, "b", 2, "c", 3)
                .reduce((e1, e2) -> new AbstractMap.SimpleEntry<>(e1.getKey() + e2.getKey(), e1.getValue() + e2.getValue()));

        assertTrue(reduced.isPresent());
        assertEquals("abc", reduced.get().getKey());
        assertEquals(6, reduced.get().getValue());
    }

    @Test
    public void testFirst() {
        Optional<Entry<String, Integer>> first = EntryStream.of(testMap).first();
        assertTrue(first.isPresent());
        assertEquals("one", first.get().getKey());
        assertEquals(1, first.get().getValue());

        Optional<Entry<String, Integer>> empty = EntryStream.<String, Integer> empty().first();
        assertFalse(empty.isPresent());
    }

    @Test
    public void testLast() {
        Optional<Entry<String, Integer>> last = EntryStream.of(testMap).last();
        assertTrue(last.isPresent());
        assertEquals("five", last.get().getKey());
        assertEquals(5, last.get().getValue());
    }

    @Test
    public void testOnlyOne() {
        Optional<Entry<String, Integer>> single = EntryStream.of("a", 1).onlyOne();
        assertTrue(single.isPresent());
        assertEquals("a", single.get().getKey());

        assertThrows(Exception.class, () -> {
            EntryStream.of(testMap).onlyOne();
        });

        Optional<Entry<String, Integer>> empty = EntryStream.<String, Integer> empty().onlyOne();
        assertFalse(empty.isPresent());
    }

    @Test
    public void testCount() {
        assertEquals(5, EntryStream.of(testMap).count());
        assertEquals(0, EntryStream.empty().count());
        assertEquals(3, EntryStream.of(testMap).limit(3).count());
    }

    @Test
    public void testToList() {
        List<Entry<String, Integer>> list = EntryStream.of(testMap).toList();
        assertEquals(5, list.size());
        assertEquals("one", list.get(0).getKey());
        assertEquals(1, list.get(0).getValue());
    }

    @Test
    public void testToSet() {
        Set<Entry<String, Integer>> set = EntryStream.of("a", 1, "b", 2, "a", 1).toSet();
        assertEquals(2, set.size());
    }

    @Test
    public void testIndexed() {
        List<Indexed<Entry<String, Integer>>> indexed = EntryStream.of("a", 1, "b", 2, "c", 3).indexed().toList();

        assertEquals(3, indexed.size());
        assertEquals(0, indexed.get(0).index());
        assertEquals("a", indexed.get(0).value().getKey());
        assertEquals(1, indexed.get(1).index());
        assertEquals("b", indexed.get(1).value().getKey());
    }

    @Test
    public void testReversed() {
        List<Entry<String, Integer>> reversed = EntryStream.of("a", 1, "b", 2, "c", 3).reversed().toList();

        assertEquals(3, reversed.size());
        assertEquals("c", reversed.get(0).getKey());
        assertEquals("b", reversed.get(1).getKey());
        assertEquals("a", reversed.get(2).getKey());
    }

    @Test
    public void testShuffled() {
        List<Entry<String, Integer>> shuffled = EntryStream.of(testMap).shuffled().toList();

        assertEquals(5, shuffled.size());
        Set<String> keys = shuffled.stream().map(Entry::getKey).collect(Collectors.toSet());
        assertTrue(keys.containsAll(testMap.keySet()));
    }

    @Test
    public void testRotated() {
        List<Entry<String, Integer>> rotated = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).rotated(2).toList();

        assertEquals(4, rotated.size());
        assertEquals("c", rotated.get(0).getKey());
        assertEquals("d", rotated.get(1).getKey());
        assertEquals("a", rotated.get(2).getKey());
        assertEquals("b", rotated.get(3).getKey());
    }

    @Test
    public void testStep() {
        List<Entry<String, Integer>> stepped = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).step(2).toList();

        assertEquals(3, stepped.size());
        assertEquals("a", stepped.get(0).getKey());
        assertEquals("c", stepped.get(1).getKey());
        assertEquals("e", stepped.get(2).getKey());
    }

    @Test
    public void testDistinct() {
        List<Entry<String, Integer>> distinct = EntryStream.of("a", 1, "b", 2, "a", 1, "c", 3).distinct().toList();

        assertEquals(3, distinct.size());
    }

    @Test
    public void testCycled() {
        List<Entry<String, Integer>> cycled = EntryStream.of("a", 1, "b", 2).cycled(2).toList();

        assertEquals(4, cycled.size());
        assertEquals("a", cycled.get(0).getKey());
        assertEquals("b", cycled.get(1).getKey());
        assertEquals("a", cycled.get(2).getKey());
        assertEquals("b", cycled.get(3).getKey());
    }

    @Test
    public void testToImmutableMap() {
        ImmutableMap<String, Integer> immutableMap = EntryStream.of("a", 1, "b", 2, "c", 3).toImmutableMap();

        assertEquals(3, immutableMap.size());
        assertEquals(1, immutableMap.get("a"));

        assertThrows(UnsupportedOperationException.class, () -> {
            immutableMap.put("d", 4);
        });
    }

    @Test
    public void testParallel() {
        long count = EntryStream.of(testMap).parallel().filter(e -> e.getValue() > 2).count();

        assertEquals(3, count);
    }

    @Test
    public void testTransformB() {
        List<String> transformed = EntryStream.of(testMap)
                .entries()
                .transformB(stream -> stream.filter(e -> e.getValue() > 2).map(e -> e.getKey().toUpperCase()))
                .toList();

        assertEquals(3, transformed.size());
        assertTrue(transformed.contains("THREE"));
        assertTrue(transformed.contains("FOUR"));
        assertTrue(transformed.contains("FIVE"));
    }

    @Test
    public void testNMatch() {
        assertTrue(EntryStream.of(testMap).nMatch(2, 4, e -> e.getValue() > 2));

        assertTrue(EntryStream.of(testMap).nMatch(1, 3, (k, v) -> k.length() == 3 && v > 0));
    }

    @Test
    public void testMapMulti() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 2, "b", 3).<String, Integer> mapMulti((e, consumer) -> {
            for (int i = 0; i < e.getValue(); i++) {
                consumer.accept(new AbstractMap.SimpleEntry<>(e.getKey() + i, i));
            }
        }).toList();

        assertEquals(5, result.size());
    }

    @Test
    public void testCollectWithSupplierAccumulator() {
        Map<String, Integer> collected = EntryStream.of(testMap)
                .filter(e -> e.getValue() > 2)
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()));

        assertEquals(3, collected.size());
        assertEquals(3, collected.get("three"));
        assertEquals(4, collected.get("four"));
        assertEquals(5, collected.get("five"));
    }

    @Test
    public void testGroupTo() {
        Map<String, List<Integer>> grouped = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).groupTo();

        assertEquals(Arrays.asList(1, 3), grouped.get("a"));
        assertEquals(Arrays.asList(2, 4), grouped.get("b"));
    }

    @Test
    public void testDefer() {
        AtomicInteger creationCount = new AtomicInteger(0);

        Supplier<EntryStream<String, Integer>> supplier = () -> {
            creationCount.incrementAndGet();
            return EntryStream.of("a", 1, "b", 2);
        };

        EntryStream<String, Integer> deferred = EntryStream.defer(supplier);
        assertEquals(0, creationCount.get());

        long count = deferred.count();
        assertEquals(2, count);
        assertEquals(1, creationCount.get());
    }

    @Test
    public void testElementAt() {
        assertEquals("one", EntryStream.of(testMap).elementAt(0).get().getKey());
        assertEquals("three", EntryStream.of(testMap).elementAt(2).get().getKey());
        assertFalse(EntryStream.of(testMap).elementAt(10).isPresent());
    }

    @Test
    public void testSortedBy() {
        List<Entry<String, Integer>> sorted = EntryStream.of(testMap).sortedBy(e -> e.getValue()).toList();

        assertEquals(1, sorted.get(0).getValue());
        assertEquals(2, sorted.get(1).getValue());
        assertEquals(3, sorted.get(2).getValue());
        assertEquals(4, sorted.get(3).getValue());
        assertEquals(5, sorted.get(4).getValue());
    }

    @Test
    public void testCollapseByKey() {
        List<List<Integer>> collapsed = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4, "c", 5).collapseByKey((k1, k2) -> k1.equals(k2)).toList();

        assertEquals(3, collapsed.size());
        assertEquals(Arrays.asList(1, 2), collapsed.get(0));
        assertEquals(Arrays.asList(3, 4), collapsed.get(1));
        assertEquals(Arrays.asList(5), collapsed.get(2));
    }

    @Test
    public void testCollapseByValue() {
        List<List<String>> collapsed = EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2, "e", 3).collapseByValue((v1, v2) -> v1.equals(v2)).toList();

        assertEquals(3, collapsed.size());
        assertEquals(Arrays.asList("a", "b"), collapsed.get(0));
        assertEquals(Arrays.asList("c", "d"), collapsed.get(1));
        assertEquals(Arrays.asList("e"), collapsed.get(2));
    }

    @Test
    public void testMerge() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("c", 3);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("d", 4);

        List<Entry<String, Integer>> merged = EntryStream
                .merge(map1, map2, (e1, e2) -> e1.getKey().compareTo(e2.getKey()) < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList();

        assertEquals(4, merged.size());
        assertEquals("a", merged.get(0).getKey());
        assertEquals("b", merged.get(1).getKey());
        assertEquals("c", merged.get(2).getKey());
        assertEquals("d", merged.get(3).getKey());
    }

    @Test
    public void testBiIterator() {
        BiIterator<String, Integer> biIter = EntryStream.of("a", 1, "b", 2, "c", 3).biIterator();

        assertTrue(biIter.hasNext());
        Pair<String, Integer> pair = biIter.next();
        assertEquals("a", pair.left());
        assertEquals(1, pair.right());

        int count = 1;
        while (biIter.hasNext()) {
            biIter.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testForEachIndexed() {
        List<String> indexedKeys = new ArrayList<>();

        EntryStream.of("a", 1, "b", 2, "c", 3).forEachIndexed((index, entry) -> {
            indexedKeys.add(index + ":" + entry.getKey());
        });

        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), indexedKeys);
    }

    @Test
    public void testAppendIfEmpty() {
        List<Entry<String, Integer>> nonEmpty = EntryStream.of("a", 1).appendIfEmpty(Fn.s(() -> EntryStream.of("b", 2))).toList();
        assertEquals(1, nonEmpty.size());
        assertEquals("a", nonEmpty.get(0).getKey());

        List<Entry<String, Integer>> empty = EntryStream.<String, Integer> empty().appendIfEmpty(Fn.s(() -> EntryStream.of("b", 2))).toList();
        assertEquals(1, empty.size());
        assertEquals("b", empty.get(0).getKey());
    }

    @Test
    public void testIfEmpty() {
        AtomicInteger counter = new AtomicInteger(0);

        EntryStream.of("a", 1).ifEmpty(() -> counter.incrementAndGet()).count();
        assertEquals(0, counter.get());

        EntryStream.<String, Integer> empty().ifEmpty(() -> counter.incrementAndGet()).count();
        assertEquals(1, counter.get());
    }

    @Test
    public void testToMapThenApplyAccept() {
        int sum = EntryStream.of("a", 1, "b", 2, "c", 3).toMapThenApply(map -> map.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, sum);

        AtomicInteger result = new AtomicInteger(0);
        EntryStream.of("a", 1, "b", 2, "c", 3).toMapThenAccept(map -> result.set(map.size()));
        assertEquals(3, result.get());
    }

    @Test
    public void testGroupToThenApplyAccept() {
        int totalGroups = EntryStream.of("a", 1, "b", 2, "a", 3).groupToThenApply(Map::size);
        assertEquals(2, totalGroups);

        AtomicInteger maxGroupSize = new AtomicInteger(0);
        EntryStream.of("a", 1, "b", 2, "a", 3, "a", 4).groupToThenAccept(map -> {
            map.values().forEach(list -> maxGroupSize.set(Math.max(maxGroupSize.get(), list.size())));
        });
        assertEquals(3, maxGroupSize.get());
    }

    @Test
    public void testCollectThenApplyAccept() {
        String result = EntryStream.of("a", 1, "b", 2)
                .collectThenApply(Collectors.toMap(Entry::getKey, Entry::getValue), map -> map.keySet().stream().collect(Collectors.joining(",")));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));

        AtomicReference<Map<String, Integer>> mapRef = new AtomicReference<>();
        EntryStream.of("a", 1, "b", 2).collectThenAccept(Collectors.toMap(Entry::getKey, Entry::getValue), mapRef::set);
        assertEquals(2, mapRef.get().size());
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> sum = EntryStream.of("a", 1, "b", 2, "c", 3).applyIfNotEmpty(stream -> stream.values().mapToInt(Integer::intValue).sum());
        assertTrue(sum.isPresent());
        assertEquals(6, sum.get());

        Optional<Integer> emptyResult = EntryStream.<String, Integer> empty().applyIfNotEmpty(stream -> stream.values().mapToInt(Integer::intValue).sum());
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);

        EntryStream.of("a", 1, "b", 2).acceptIfNotEmpty(stream -> counter.addAndGet((int) stream.count()));
        assertEquals(2, counter.get());

        counter.set(0);
        EntryStream.<String, Integer> empty().acceptIfNotEmpty(stream -> counter.incrementAndGet()).orElse(() -> counter.set(100));
        assertEquals(100, counter.get());
    }
}
