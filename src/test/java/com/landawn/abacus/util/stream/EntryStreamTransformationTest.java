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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.Optional;

/** EntryStream transformation contracts. */
public class EntryStreamTransformationTest extends EntryStreamTestSupport {

    // collapseByKey

    @Test
    public void testCollapseByKey() {
        List<List<Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4, "c", 5).collapseByKey((k1, k2) -> k1.equals(k2)).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
    }

    @Test
    public void testCollapseByKeyAndValueWithMapperAndCollector() {
        List<String> collapsed = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4, "c", 5)
                .collapseByKey((k1, k2) -> k1.equals(k2), Entry::getValue, Collectors.mapping(Object::toString, Collectors.joining(",")))
                .toList();

        assertEquals(3, collapsed.size());
        assertEquals("1,2", collapsed.get(0));
        assertEquals("3,4", collapsed.get(1));
        assertEquals("5", collapsed.get(2));

        List<String> collapsedByValue = EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2)
                .collapseByValue((v1, v2) -> v1.equals(v2), Entry::getKey, Collectors.joining("-"))
                .toList();

        assertEquals(2, collapsedByValue.size());
        assertEquals("a-b", collapsedByValue.get(0));
        assertEquals("c-d", collapsedByValue.get(1));
    }

    @Test
    public void testCollapseByKeyWithMapperAndCollector() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a1", 1);
        map.put("a2", 2);
        map.put("b1", 3);
        map.put("b2", 4);
        map.put("c1", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Integer> result = stream.collapseByKey((k1, k2) -> k1.charAt(0) == k2.charAt(0), Map.Entry::getValue, Collectors.summingInt(Integer::intValue))
                .toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0));
        assertEquals(7, result.get(1));
        assertEquals(5, result.get(2));
    }

    @Test
    public void testCollapseByValue() {
        List<List<String>> result = EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2).collapseByValue((v1, v2) -> v1.equals(v2)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testCollapseByValueWithMapperAndCollector() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 1);
        map.put("c", 2);
        map.put("d", 2);
        map.put("e", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<String> result = stream.collapseByValue((v1, v2) -> v1.equals(v2), Map.Entry::getKey, Collectors.joining("-")).toList();
        assertEquals(3, result.size());
        assertEquals("a-b", result.get(0));
        assertEquals("c-d", result.get(1));
        assertEquals("e", result.get(2));
    }

    // cycled

    @Test
    public void testCycled() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).cycled().limit(5).toList();
        assertEquals(5, result.size());
        assertEquals("a", result.get(2).getKey());
    }

    @Test
    public void testCycled_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).cycled().limit(5).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testCycled_WithRoundsFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).cycled(2).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testCycledOperations() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1).cycled(3).toList();

        assertEquals(3, result.size());
        assertEquals("a", result.get(2).getKey());
    }

    @Test
    public void testCycledWithRounds() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> cycled = stream.cycled(3).toList();
        assertEquals(6, cycled.size());
        assertEquals("a", cycled.get(0).getKey());
        assertEquals("b", cycled.get(1).getKey());
        assertEquals("a", cycled.get(2).getKey());
        assertEquals("b", cycled.get(3).getKey());
        assertEquals("a", cycled.get(4).getKey());
        assertEquals("b", cycled.get(5).getKey());
    }

    @Test
    public void testDifference_FromBase() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).difference(set).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testDifference_Map() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);

        List<Map.Entry<String, Integer>> result = EntryStream.of(testMap).difference(other).toList();
        assertEquals(3, result.size()); // three, four, five remain
    }

    // distinct

    @Test
    public void testDistinct() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 1);
        map.put("c", 2);

        long count = EntryStream.of(map).distinctByValue().count();
        assertEquals(2, count);

        assertEquals(3, EntryStream.of(map).distinct().count());
    }

    @Test
    public void testDistinct_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1, "b", 2).distinct().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctWithMergeFunction() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);
        map.put("a", 3);

        final EntryStream<String, Integer> stream = EntryStream.of(map);

        final Map<String, Integer> result = stream.distinct((e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2).toMap();

        assertEquals(2, result.get("b").intValue());
        assertEquals(3, result.get("a").intValue());
    }

    @Test
    public void testDistinct_WithMergeFunction() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3)
                .distinctBy(e -> e.getKey(), (e1, e2) -> new AbstractMap.SimpleEntry<>(e1.getKey(), e1.getValue() + e2.getValue()))
                .toList();
        assertEquals(2, result.size());
    }

    // distinctBy

    @Test
    public void testDistinctBy() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("aaa", 1);
        map.put("b", 2);
        map.put("cc", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> distinct = stream.distinctBy(e -> e.getKey().length()).toList();
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctBy_Function() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).distinctBy(e -> e.getValue() % 2).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctBy_WithMergeFunction() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).distinctBy(e -> e.getValue() % 2, (e1, e2) -> e1).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctByWithMergeFunction() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 3),
                new AbstractMap.SimpleEntry<>("c", 2));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        List<Map.Entry<String, Integer>> distinct = stream.distinctBy(Map.Entry::getKey, (e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2).toList();
        assertEquals(3, distinct.size());
    }

    // distinctByKey

    @Test
    public void testDistinctByKey() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3).distinctByKey().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctByKey_Basic() {
        List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4, "c", 5).distinctByKey().toList();
        assertEquals(3, result.size());
        // only first occurrence of each key is kept
        long aCount = result.stream().filter(e -> "a".equals(e.getKey())).count();
        assertEquals(1, aCount);
    }

    @Test
    public void testDistinctByKey_Parallel() {
        // distinctByKey() parallel branch
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        List<Map.Entry<String, Integer>> result = EntryStream.of(source.entrySet()).parallel().distinctByKey().toList();
        assertEquals(3, result.size());
    }

    // distinctByValue

    @Test
    public void testDistinctByValue() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 1, "c", 2).distinctByValue().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctByValue_Basic() {
        List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2, "e", 3).distinctByValue().toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctByValue_Parallel() {
        // distinctByValue() parallel branch
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        entries.add(new AbstractMap.SimpleEntry<>("a", 1));
        entries.add(new AbstractMap.SimpleEntry<>("b", 2));
        entries.add(new AbstractMap.SimpleEntry<>("c", 1)); // duplicate value
        List<Map.Entry<String, Integer>> result = EntryStream.of(entries).parallel().distinctByValue().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctVariants() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 1, "a", 2, "c", 1).distinctByKey().toList();

        assertEquals(3, result.size());
    }

    // dropWhile

    @Test
    public void testDropWhile() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        List<Integer> result = EntryStream.of(map).dropWhile(e -> e.getValue() < 3).values().toList();
        assertEquals(Arrays.asList(3, 4), result);
    }

    @Test
    public void testDropWhile_BiPredicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).dropWhile((k, v) -> v < 3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDropWhile_Predicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).dropWhile(e -> e.getValue() < 3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDropWhile_PredicateFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .dropWhile((java.util.function.Predicate<Entry<String, Integer>>) e -> e.getValue() < 3)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDropWhile_WithAction() {
        AtomicInteger dropped = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).dropWhile(e -> e.getValue() < 3, e -> dropped.incrementAndGet()).toList();
        assertEquals(3, result.size());
        assertEquals(2, dropped.get());
    }

    @Test
    public void testDropWhileWithAction() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        List<Map.Entry<String, Integer>> dropped = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.dropWhile(e -> e.getValue() < 3, dropped::add).toList();
        assertEquals(1, result.size());
        assertEquals(2, dropped.size());
    }

    @Test
    public void testDropWhileWithBiPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.dropWhile((k, v) -> v < 3).toList();
        assertEquals(1, result.size());
        assertEquals("c", result.get(0).getKey());
    }

    @Test
    public void testDropWhileWithBiPredicateAndConsumer() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        final AtomicInteger droppedSum = new AtomicInteger(0);

        final List<Integer> result = EntryStream.of(map).dropWhile(e -> e.getValue() < 3, e -> droppedSum.addAndGet(e.getValue())).values().toList();

        assertEquals(Arrays.asList(3, 4), result);
        assertEquals(3, droppedSum.get());
    }

    @Test
    public void testDropWhileWithPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);
        orderedMap.put("d", 1);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.dropWhile(e -> e.getValue() < 3).toList();
        assertEquals(2, result.size());
        assertEquals("c", result.get(0).getKey());
        assertEquals("d", result.get(1).getKey());
    }

    // filter

    @Test
    public void testFilter() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(1, EntryStream.of(map).filter(entry -> entry.getValue() > 2).count());
    }

    @Test
    public void testFilter_BiPredicate() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filter((k, v) -> k.length() > 3 && v > 2).toList();
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilter_Predicate() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filter(e -> e.getValue() > 2).toList();
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilter_PredicateFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .filter((java.util.function.Predicate<Entry<String, Integer>>) e -> e.getValue() > 2)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFilter_WithAction() {
        AtomicInteger dropped = new AtomicInteger(0);
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filter(e -> e.getValue() > 3, e -> dropped.incrementAndGet()).toList();
        assertEquals(2, filtered.size());
        assertEquals(3, dropped.get());
    }

    @Test
    public void testFilterWithAction() {
        List<Map.Entry<String, Integer>> dropped = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter(e -> e.getValue() > 3, dropped::add).toMap();
        assertEquals(2, filtered.size());
        assertEquals(3, dropped.size());
        assertEquals("one", dropped.get(0).getKey());
    }

    @Test
    public void testFilterWithBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter((k, v) -> k.startsWith("t") && v > 1).toMap();
        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("two"));
        assertTrue(filtered.containsKey("three"));
    }

    @Test
    public void testFilterWithPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter(e -> e.getValue() > 3).toMap();
        assertEquals(2, filtered.size());
        assertFalse(filtered.containsKey("one"));
        assertTrue(filtered.containsKey("four"));
        assertTrue(filtered.containsKey("five"));
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
    public void testIndexed() {
        List<Indexed<Entry<String, Integer>>> result = EntryStream.of(testMap).indexed().toList();
        assertEquals(5, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals("one", result.get(0).value().getKey());
    }

    @Test
    public void testIndexed_FromBase() {
        List<Indexed<Entry<String, Integer>>> result = EntryStream.of(testMap).indexed().toList();
        assertEquals(5, result.size());
        assertEquals(0, result.get(0).index());
    }

    // intersection

    @Test
    public void testIntersection() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));
        set.add(new AbstractMap.SimpleEntry<>("three", 3));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).intersection(set).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersection_FromBase() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).intersection(set).toList();
        assertEquals(1, result.size());
    }

    @Test
    public void testIntersection_Map() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 99); // different value
        other.put("six", 6);

        List<Map.Entry<String, Integer>> result = EntryStream.of(testMap).intersection(other).toList();
        // intersection by key+value match
        assertFalse(result.isEmpty());
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
    public void testLimit() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(2, EntryStream.of(map).limit(2).count());
    }

    @Test
    public void testLimit_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).limit(3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testOnEach() {
        List<String> processed = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.peek(e -> processed.add(e.getKey())).toMap();

        assertEquals(testMap.size(), result.size());
        assertEquals(testMap.size(), processed.size());
    }

    @Test
    public void testOnEach_BiConsumer_New() {
        List<String> keys = new ArrayList<>();
        List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2)
                .onEach((java.util.function.BiConsumer<String, Integer>) (k, v) -> keys.add(k))
                .toList();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertEquals(2, result.size());
    }

    @Test
    public void testOnEach_Consumer_New() {
        List<Map.Entry<String, Integer>> seen = new ArrayList<>();
        List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2)
                .onEach((java.util.function.Consumer<Map.Entry<String, Integer>>) seen::add)
                .toList();
        assertEquals(2, seen.size());
        assertEquals(2, result.size());
    }

    @Test
    public void testOnEach_BiConsumer() {
        AtomicInteger sum = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).peek((k, v) -> sum.addAndGet(v)).toList();
        assertEquals(5, result.size());
        assertEquals(15, sum.get());
    }

    @Test
    public void testOnEach_Consumer() {
        AtomicInteger count = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).peek(e -> count.incrementAndGet()).toList();
        assertEquals(5, result.size());
        assertEquals(5, count.get());
    }

    @Test
    public void testOnEach_ConsumerFromBase() {
        AtomicInteger count = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .peek((java.util.function.Consumer<Entry<String, Integer>>) e -> count.incrementAndGet())
                .toList();
        assertEquals(5, result.size());
        assertEquals(5, count.get());
    }

    @Test
    public void testOnEachBiConsumer() {
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.peek((k, v) -> {
            keys.add(k);
            values.add(v);
        }).toMap();

        assertEquals(testMap.size(), result.size());
        assertEquals(testMap.size(), keys.size());
        assertEquals(testMap.size(), values.size());
    }

    // onlyOne

    @Test
    public void testOnlyOne() {
        Optional<Entry<String, Integer>> result = EntryStream.of("a", 1).onlyOne();
        assertTrue(result.isPresent());
        assertEquals("a", result.get().getKey());

        assertThrows(Exception.class, () -> EntryStream.of(testMap).onlyOne());
    }

    @Test
    public void testOnlyOne_FromBase() {
        Optional<Entry<String, Integer>> result = EntryStream.of("a", 1).onlyOne();
        assertTrue(result.isPresent());
    }

    @Test
    public void testOnlyOneThrowsException() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testPeek() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(map).peek(entry -> count.incrementAndGet()).count();
        assertEquals(2, count.get());
    }

    @Test
    public void testReversed() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).reversed().toList();
        assertEquals("five", result.get(0).getKey());
    }

    @Test
    public void testReversed_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).reversed().toList();
        assertEquals(5, result.size());
        assertEquals("five", result.get(0).getKey());
    }

    // reverseSorted

    @Test
    public void testReverseSorted() {
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.of(1, "a", 2, "b", 3, "c").reverseSorted().toList());
    }

    @Test
    public void testReverseSorted_WithComparator() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).reverseSorted((e1, e2) -> e1.getValue().compareTo(e2.getValue())).toList();
        assertEquals(5, result.get(0).getValue());
    }

    @Test
    public void testReverseSortedWithComparator() {
        Map<String, Integer> map = new HashMap<>();
        map.put("aaa", 1);
        map.put("b", 2);
        map.put("cc", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.reverseSorted(Comparator.comparing(e -> e.getKey().length())).toList();
        assertEquals(3, sorted.size());
        assertEquals("aaa", sorted.get(0).getKey());
        assertEquals("cc", sorted.get(1).getKey());
        assertEquals("b", sorted.get(2).getKey());
    }

    @Test
    public void testReverseSortedBy() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).reverseSortedBy(Entry::getValue).toList();
        assertEquals(5, result.get(0).getValue());
    }

    @Test
    public void testRotated() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final List<Map.Entry<Integer, String>> result = EntryStream.of(map).rotated(1).toList();
        assertEquals(Arrays.asList(N.newEntry(3, "c"), N.newEntry(1, "a"), N.newEntry(2, "b")), result);
    }

    @Test
    public void testRotated_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3).rotated(1).toList();
        assertEquals("c", result.get(0).getKey());
    }

    // shuffled

    @Test
    public void testShuffled() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");
        map.put(5, "e");

        final List<Map.Entry<Integer, String>> original = EntryStream.of(map).toList();
        final List<Map.Entry<Integer, String>> shuffled = EntryStream.of(map).shuffled().toList();

        assertEquals(original.size(), shuffled.size());
        assertFalse(original.equals(shuffled));
        assertTrue(original.containsAll(shuffled));
    }

    @Test
    public void testShuffled_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).shuffled().toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testShuffled_WithRandomFromBase() {
        Random rnd = new Random(42);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).shuffled(rnd).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testShuffledWithRandom() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Random rnd = new Random(42);
        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> shuffled = stream.shuffled(rnd).toList();
        assertEquals(3, shuffled.size());
        assertTrue(shuffled.stream().anyMatch(e -> e.getKey().equals("a")));
        assertTrue(shuffled.stream().anyMatch(e -> e.getKey().equals("b")));
        assertTrue(shuffled.stream().anyMatch(e -> e.getKey().equals("c")));
    }

    // skip

    @Test
    public void testSkip() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skip(2).toList();
        assertEquals(3, result.size());
        assertEquals("three", result.get(0).getKey());
    }

    @Test
    public void testSkip_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skip(2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSkip_WithAction() {
        AtomicInteger skipped = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skip(2, e -> skipped.incrementAndGet()).toList();
        assertEquals(3, result.size());
        assertEquals(2, skipped.get());
    }

    @Test
    public void testSkipWithAction() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        List<Map.Entry<String, Integer>> skipped = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> result = stream.skip(2, skipped::add).toList();
        assertEquals(2, result.size());
        assertEquals(2, skipped.size());
        assertEquals("a", skipped.get(0).getKey());
        assertEquals("b", skipped.get(1).getKey());
    }

    @Test
    public void testSkipWithNegativeCountAndNullOnSkipReportsCountFirst() {
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> EntryStream.of("a", 1).skip(-1, null));

        assertTrue(ex.getMessage().contains("'n'"), ex.getMessage());
    }

    @Test
    public void testSkipWithNullOnSkipNamesOnSkipParameter() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        final AtomicInteger closed = new AtomicInteger();
        final EntryStream<String, Integer> stream = EntryStream.of(map).onClose(closed::incrementAndGet);

        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> stream.skip(1, null));

        assertTrue(ex.getMessage().contains("onSkip"), ex.getMessage());
        assertEquals(1, closed.get());
    }

    // skipUntil

    @Test
    public void testSkipUntil_BiPredicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skipUntil((k, v) -> v >= 3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSkipUntil_Predicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skipUntil(e -> e.getValue() >= 3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSkipUntil_PredicateFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .skipUntil((java.util.function.Predicate<Entry<String, Integer>>) e -> e.getValue() >= 3)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSkipUntilWithBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        assertEquals(2, EntryStream.of(map).skipUntil((k, v) -> v > 2).count());
    }

    @Test
    public void testSkipUntilWithPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.skipUntil(e -> e.getValue() >= 3).toList();
        assertEquals(1, result.size());
        assertEquals("c", result.get(0).getKey());
    }

    // sorted

    @Test
    public void testSorted() {
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.of(3, "c", 1, "a", 2, "b").sorted().toList());
    }

    @Test
    public void testSorted_WithComparator() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).toList();
        assertEquals(5, result.get(0).getValue());
    }

    @Test
    public void testSortedAndReverseSorted() {
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.of("b", 2, "a", 1, "c", 3).sorted().toList());
    }

    @Test
    public void testSortedWithComparator() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 3);
        map.put("b", 1);
        map.put("c", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sorted(Map.Entry.<String, Integer> comparingByValue()).toList();
        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0).getValue());
        assertEquals(2, sorted.get(1).getValue());
        assertEquals(3, sorted.get(2).getValue());
    }

    @Test
    public void testSortedBy() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sortedBy(Entry::getValue).toList();
        assertEquals(1, result.get(0).getValue());
    }

    @Test
    public void testSortedByDouble() {
        List<Entry<String, Double>> result = EntryStream.of("a", 3.0, "b", 1.0, "c", 2.0).sortedByDouble(Entry::getValue).toList();
        assertEquals(1.0, result.get(0).getValue());
    }

    @Test
    public void testSortedByInt() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sortedByInt(Entry::getValue).toList();
        assertEquals(1, result.get(0).getValue());
    }

    @Test
    public void testSortedByIntLongDouble() {
        Map<String, Integer> unsortedMap = new LinkedHashMap<>();
        unsortedMap.put("c", 30);
        unsortedMap.put("a", 10);
        unsortedMap.put("b", 20);

        List<Entry<String, Integer>> sortedByInt = EntryStream.of(unsortedMap).sortedByInt(Entry::getValue).toList();
        assertEquals(10, sortedByInt.get(0).getValue());
        assertEquals(20, sortedByInt.get(1).getValue());
        assertEquals(30, sortedByInt.get(2).getValue());

        Map<String, Long> longMap = new LinkedHashMap<>();
        longMap.put("c", 30L);
        longMap.put("a", 10L);
        longMap.put("b", 20L);

        List<Entry<String, Long>> sortedByLong = EntryStream.of(longMap).sortedByLong(Entry::getValue).toList();
        assertEquals(10L, sortedByLong.get(0).getValue());
        assertEquals(20L, sortedByLong.get(1).getValue());
        assertEquals(30L, sortedByLong.get(2).getValue());

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
    public void testSortedByKey() {
        List<Entry<String, Integer>> result = EntryStream.of("c", 1, "a", 2, "b", 3).sortedByKey(Comparator.naturalOrder()).toList();
        assertEquals("a", result.get(0).getKey());
    }

    @Test
    public void testSortedByLong() {
        List<Entry<String, Long>> result = EntryStream.of("a", 3L, "b", 1L, "c", 2L).sortedByLong(Entry::getValue).toList();
        assertEquals(1L, result.get(0).getValue());
    }

    @Test
    public void testSortedByValue() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sortedByValue(Comparator.reverseOrder()).toList();
        assertEquals(5, result.get(0).getValue());
    }

    @Test
    public void testStep() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> result = stream.step(2).toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("c", result.get(1).getKey());
        assertEquals("e", result.get(2).getKey());
    }

    @Test
    public void testStep_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).step(2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));
        set.add(new AbstractMap.SimpleEntry<>("six", 6));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).symmetricDifference(set).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testSymmetricDifference_Map() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("six", 6);

        List<Map.Entry<String, Integer>> result = EntryStream.of(testMap).symmetricDifference(other).toList();
        assertFalse(result.isEmpty());
    }

    // takeWhile

    @Test
    public void testTakeWhile() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        List<Integer> result = EntryStream.of(map).takeWhile(e -> e.getValue() < 3).values().toList();

        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testTakeWhile_BiPredicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).takeWhile((k, v) -> v < 3).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testTakeWhile_Predicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).takeWhile(e -> e.getValue() < 4).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testTakeWhile_PredicateFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .takeWhile((java.util.function.Predicate<Entry<String, Integer>>) e -> e.getValue() < 4)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testTakeWhileAndDropWhileWithBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        assertEquals(2, EntryStream.of(map).takeWhile((k, v) -> v < 3).count());
        assertEquals(2, EntryStream.of(map).dropWhile((k, v) -> v < 3).count());
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
    public void testTakeWhileWithBiPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.takeWhile((k, v) -> v < 3).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testTakeWhileWithPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);
        orderedMap.put("d", 1);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.takeWhile(e -> e.getValue() <= 2).toList();
        assertEquals(2, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
    }

}
