package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;

/** EntryStream reduction contracts. */
public class EntryStreamReductionTest extends EntryStreamTestSupport {

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicInteger sum = new AtomicInteger(0);
        EntryStream.of(testMap).acceptIfNotEmpty(s -> s.forEach(e -> sum.addAndGet(e.getValue())));
        assertEquals(15, sum.get());
    }

    // allMatch

    @Test
    public void testAllMatch() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(EntryStream.of(map).allMatch(entry -> entry.getValue() > 0));
        assertFalse(EntryStream.of(map).allMatch(entry -> entry.getValue() > 1));
    }

    @Test
    public void testAllMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).allMatch((k, v) -> v > 0));
        assertFalse(EntryStream.of(testMap).allMatch((k, v) -> v > 3));
    }

    @Test
    public void testAllMatch_Predicate() {
        assertTrue(EntryStream.of(testMap).allMatch(e -> e.getValue() > 0));
        assertFalse(EntryStream.of(testMap).allMatch(e -> e.getValue() > 3));
    }

    @Test
    public void testAllMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.allMatch((k, v) -> k.length() >= 3));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.allMatch((k, v) -> v > 2));
    }

    // anyMatch

    @Test
    public void testAnyMatch() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(EntryStream.of(map).anyMatch(entry -> entry.getValue() > 1));
        assertFalse(EntryStream.of(map).anyMatch(entry -> entry.getValue() > 2));
    }

    @Test
    public void testAnyMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).anyMatch((k, v) -> v > 3));
        assertFalse(EntryStream.of(testMap).anyMatch((k, v) -> v > 10));
    }

    @Test
    public void testAnyMatch_Predicate() {
        assertTrue(EntryStream.of(testMap).anyMatch(e -> e.getValue() > 3));
        assertFalse(EntryStream.of(testMap).anyMatch(e -> e.getValue() > 10));
    }

    @Test
    public void testAnyMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.anyMatch((k, v) -> k.equals("two") && v == 2));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.anyMatch((k, v) -> k.equals("x")));
    }

    @Test
    public void testApplyIfNotEmpty() {
        final Map<String, Integer> map = N.asMap("a", 1);
        assertTrue(EntryStream.of(map).applyIfNotEmpty(s -> true).get());
        assertFalse(EntryStream.<String, Integer> empty().applyIfNotEmpty(s -> true).isPresent());
    }

    @Test
    public void testCount() {
        assertEquals(5, EntryStream.of(testMap).count());
        assertEquals(0, EntryStream.empty().count());
    }

    @Test
    public void testCount_FromBase() {
        assertEquals(5, EntryStream.of(testMap).count());
    }

    @Test
    public void testElementAt() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals("three", result.get().getKey());
    }

    @Test
    public void testElementAt_FromBase() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).elementAt(2);
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertNotNull(EntryStream.of(map).first().get());
    }

    // findAny

    @Test
    public void testFindAny_BiPredicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findAny((k, v) -> v > 3);
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg() {
        Optional<Map.Entry<String, Integer>> result = EntryStream.of(testMap).findAny();
        assertTrue(result.isPresent());

        Optional<Map.Entry<String, Integer>> empty = EntryStream.of(emptyMap).findAny();
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindAny_parallel_empty() {
        Optional<Map.Entry<String, Integer>> result = EntryStream.<String, Integer> empty().parallel().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_parallel_nonEmpty() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Optional<Map.Entry<String, Integer>> result = EntryStream.of(map).parallel().findAny();

        assertTrue(result.isPresent());
        assertTrue(map.containsKey(result.get().getKey()));
        assertEquals(map.get(result.get().getKey()), result.get().getValue());
    }

    @Test
    public void testFindAny_Predicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findAny(e -> e.getValue() > 3);
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_sequential_empty() {
        Optional<Map.Entry<String, Integer>> result = EntryStream.<String, Integer> empty().findAny();
        assertFalse(result.isPresent());
    }

    // SUSPECT #2: findAny() must work in both sequential and parallel modes.
    @Test
    public void testFindAny_sequential_nonEmpty() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Optional<Map.Entry<String, Integer>> result = EntryStream.of(map).findAny();

        assertTrue(result.isPresent());
        assertEquals("a", result.get().getKey());
        assertEquals(Integer.valueOf(1), result.get().getValue());
    }

    @Test
    public void testFindAnyBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> found = stream.findAny((k, v) -> v == 2);

        assertTrue(found.isPresent());
        assertEquals(2, found.get().getValue());
    }

    // findFirst

    @Test
    public void testFindFirst_BiPredicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findFirst((k, v) -> v > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getValue());
    }

    @Test
    public void testFindFirst_NoArg() {
        Optional<Map.Entry<String, Integer>> result = EntryStream.of(testMap).findFirst();
        assertTrue(result.isPresent());
        assertEquals("one", result.get().getKey());

        Optional<Map.Entry<String, Integer>> empty = EntryStream.of(emptyMap).findFirst();
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindFirst_Predicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findFirst(e -> e.getValue() > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getValue());
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
    public void testFindFirstBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> found = stream.findFirst((k, v) -> v >= 2);

        assertTrue(found.isPresent());
        assertEquals("b", found.get().getKey());
    }

    // findLast

    @Test
    public void testFindLast() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 2);
        map.put("b", 4);
        map.put("c", 6);
        map.put("d", 8);

        assertEquals(8, EntryStream.of(map).findLast(e -> e.getValue() > 5).get().getValue().intValue());
        assertFalse(EntryStream.of(map).findLast(e -> e.getValue() > 10).isPresent());
    }

    @Test
    public void testFindLast_BiPredicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findLast((k, v) -> v < 5);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getValue());
    }

    @Test
    public void testFindLast_Predicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findLast(e -> e.getValue() < 5);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getValue());
    }

    @Test
    public void testFindLastBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> found = stream.findLast((k, v) -> v == 1);

        assertTrue(found.isPresent());
        assertEquals("c", found.get().getKey());
    }

    @Test
    public void testFindLastCanStopBeforeEarlierNullEntry() {
        final Entry<String, Integer> last = new AbstractMap.SimpleImmutableEntry<>("last", 2);
        final EntryStream<String, Integer> source = EntryStream.of(Stream.<Entry<String, Integer>> of(null, last));

        assertEquals(last, source.findLast((key, value) -> value % 2 == 0).get());
    }

    // first

    @Test
    public void testFindFirst() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(1, (int) EntryStream.of(map).first().get().getValue());
    }

    @Test
    public void testFirst() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).first();
        assertTrue(result.isPresent());
        assertEquals("one", result.get().getKey());
    }

    @Test
    public void testFirst_FromBase() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).first();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFirstAndLast() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        assertEquals(N.newEntry(1, "a"), EntryStream.of(map).first().get());
        assertEquals(N.newEntry(3, "c"), EntryStream.of(map).last().get());
    }

    // forEach

    @Test
    public void testForEach() {
        List<String> keys = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.forEach(e -> keys.add(e.getKey()));

        assertEquals(testMap.size(), keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
        assertTrue(keys.contains("three"));
    }

    @Test
    public void testForEach_BiConsumer() {
        AtomicInteger sum = new AtomicInteger(0);
        EntryStream.of(testMap).forEach((k, v) -> sum.addAndGet(v));
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEach_Consumer() {
        AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(testMap).forEach(e -> count.incrementAndGet());
        assertEquals(5, count.get());
    }

    @Test
    public void testForEachBiConsumer() {
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.forEach((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        assertEquals(testMap.size(), keys.size());
        assertEquals(testMap.size(), values.size());
    }

    @Test
    public void testForEachIndexed() {
        List<Integer> indices = new ArrayList<>();
        EntryStream.of(testMap).forEachIndexed((i, e) -> indices.add(i));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), indices);
    }

    @Test
    public void testLast() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).last();
        assertTrue(result.isPresent());
        assertEquals("five", result.get().getKey());
    }

    @Test
    public void testLast_FromBase() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).last();
        assertTrue(result.isPresent());
    }

    @Test
    public void testMax() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).max(Comparator.comparing(Entry::getValue));
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getValue());
    }

    @Test
    public void testMaxBy() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).maxBy(Entry::getValue);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getValue());
    }

    @Test
    public void testMaxByKey() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).maxByKey(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals("two", result.get().getKey());
    }

    @Test
    public void testMaxByValue() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).maxByValue(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getValue());
    }

    @Test
    public void testMin() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).min(Comparator.comparing(Entry::getValue));
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getValue());
    }

    @Test
    public void testMinBy() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).minBy(Entry::getValue);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getValue());
    }

    @Test
    public void testMinByAndMaxByRetainEntryWhenAllSortKeysAreNull() {
        final Entry<String, Integer> first = new AbstractMap.SimpleImmutableEntry<>("first", 1);

        assertEquals(first, EntryStream.of("first", 1, "second", 2).minBy(entry -> null).get());
        assertEquals(first, EntryStream.of("first", 1, "second", 2).maxBy(entry -> null).get());
    }

    @Test
    public void testMinByKey() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).minByKey(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals("five", result.get().getKey());
    }

    @Test
    public void testMinByValue() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).minByValue(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getValue());
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

    // noneMatch

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
    public void testNoneMatch() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(EntryStream.of(map).noneMatch(entry -> entry.getValue() > 2));
        assertFalse(EntryStream.of(map).noneMatch(entry -> entry.getValue() > 1));
    }

    @Test
    public void testNoneMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).noneMatch((k, v) -> v > 10));
        assertFalse(EntryStream.of(testMap).noneMatch((k, v) -> v > 3));
    }

    @Test
    public void testNoneMatch_Predicate() {
        assertTrue(EntryStream.of(testMap).noneMatch(e -> e.getValue() > 10));
        assertFalse(EntryStream.of(testMap).noneMatch(e -> e.getValue() > 3));
    }

    @Test
    public void testNoneMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.noneMatch((k, v) -> k.equals("missing")));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.noneMatch((k, v) -> k.equals("one")));
    }

    @Test
    public void testPercentileOperations() {
        Optional<Map<Percentage, Entry<String, Integer>>> result = EntryStream.of(testMap).percentiles(Comparator.comparing(Entry::getValue));

        assertTrue(result.isPresent());
        Map<Percentage, Entry<String, Integer>> percentiles = result.get();
        assertNotNull(percentiles);
    }

    @Test
    public void testPercentiles() {
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.of(testMap).percentiles());
    }

    @Test
    public void testPercentiles_WithComparator() {
        Optional<Map<Percentage, Entry<String, Integer>>> result = EntryStream.of(testMap).percentiles(Comparator.comparing(Entry::getValue));
        assertTrue(result.isPresent());
    }

    // reduce

    @Test
    public void testReduce() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        int sum = EntryStream.of(map).values().reduce(0, (a, b) -> a + b);

        assertEquals(6, sum);
    }

    @Test
    public void testReduce_WithIdentity() {
        Entry<String, Integer> result = EntryStream.of("a", 1, "b", 2)
                .reduce(new AbstractMap.SimpleEntry<>("sum", 0), (e1, e2) -> new AbstractMap.SimpleEntry<>("sum", e1.getValue() + e2.getValue()));
        assertEquals(3, result.getValue());
    }

    @Test
    public void testReduce_WithoutIdentity() {
        Optional<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2)
                .reduce((e1, e2) -> new AbstractMap.SimpleEntry<>("sum", e1.getValue() + e2.getValue()));
        assertTrue(result.isPresent());
        assertEquals(3, result.get().getValue());
    }

    @Test
    public void testReduceWithIdentity() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map.Entry<String, Integer> result = EntryStream.of(map)
                .reduce(new AbstractMap.SimpleEntry<>("sum", 0), (acc, e) -> new AbstractMap.SimpleEntry<>(acc.getKey(), acc.getValue() + e.getValue()));

        assertEquals("sum", result.getKey());
        assertEquals(6, (int) result.getValue());
    }

    @Test
    public void testReduceWithIdentityAndBinaryOperator() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);
        final Map.Entry<String, Integer> identity = new AbstractMap.SimpleEntry<>("total", 0);
        final Map.Entry<String, Integer> result = EntryStream.of(map)
                .reduce(identity, (acc, e) -> new AbstractMap.SimpleEntry<>(acc.getKey(), acc.getValue() + e.getValue()));

        assertEquals("total", result.getKey());
        assertEquals(6, result.getValue().intValue());
    }

    @Test
    public void testReduceWithoutIdentity() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> result = stream.reduce((e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2);

        assertTrue(result.isPresent());
        assertEquals(5, result.get().getValue());
    }

}
