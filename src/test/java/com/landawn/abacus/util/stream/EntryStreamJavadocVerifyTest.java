package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.Optional;

public class EntryStreamJavadocVerifyTest extends TestBase {

    private Map<String, Integer> testMap;

    @BeforeEach
    public void setUp() {
        testMap = new LinkedHashMap<>();
        testMap.put("a", 1);
        testMap.put("b", 2);
        testMap.put("c", 3);
    }

    @Test
    public void verifyKeys() {
        List<String> keys = EntryStream.of(testMap).keys().toList();
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    @Test
    public void verifyValues() {
        List<Integer> values = EntryStream.of(testMap).values().toList();
        assertEquals(Arrays.asList(1, 2, 3), values);
    }

    @Test
    public void verifyInvert() {
        Map<Integer, String> inverted = EntryStream.of(testMap).inverted().toMap();
        assertEquals("a", inverted.get(1));
        assertEquals("b", inverted.get(2));
        assertEquals("c", inverted.get(3));
    }

    @Test
    public void verifyFilter() {
        Map<String, Integer> result = EntryStream.of(testMap).filter(e -> e.getValue() > 1).toMap();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(3), result.get("c"));
    }

    @Test
    public void verifyFilterBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("apple", 5);
        map.put("banana", 3);
        map.put("cherry", 7);
        Map<String, Integer> result = EntryStream.of(map).filter((k, v) -> k.length() > 5 && v > 4).toMap();
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(7), result.get("cherry"));
    }

    @Test
    public void verifyFilterByKey() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("apricot", 3);
        Map<String, Integer> result = EntryStream.of(map).filterByKey(k -> k.startsWith("a")).toMap();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("apple"));
        assertTrue(result.containsKey("apricot"));
    }

    @Test
    public void verifyFilterByValue() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 2);
        Map<String, Integer> result = EntryStream.of(map).filterByValue(v -> v == 2).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void verifyTakeWhile() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");
        Map<Integer, String> result = EntryStream.of(map).takeWhile(e -> e.getKey() < 3).toMap();
        assertEquals(2, result.size());
        assertEquals("a", result.get(1));
        assertEquals("b", result.get(2));
    }

    @Test
    public void verifyDropWhile() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");
        Map<Integer, String> result = EntryStream.of(map).dropWhile(e -> e.getKey() < 3).toMap();
        assertEquals(2, result.size());
        assertEquals("c", result.get(3));
        assertEquals("d", result.get(4));
    }

    @Test
    public void verifyMapKeyFunction() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("hello", 1);
        map.put("world", 2);
        List<String> result = EntryStream.of(map).mapKey(k -> k.toUpperCase()).keys().toList();
        assertTrue(result.contains("HELLO"));
        assertTrue(result.contains("WORLD"));
    }

    @Test
    public void verifyMapKeyBiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapKey((k, v) -> k + v).toMap();
        assertEquals(Integer.valueOf(1), result.get("a1"));
        assertEquals(Integer.valueOf(2), result.get("b2"));
        assertEquals(Integer.valueOf(3), result.get("c3"));
    }

    @Test
    public void verifyMapValue() {
        Map<String, Integer> result = EntryStream.of(testMap).<Integer> mapValue(v -> v * v).toMap();
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(4), result.get("b"));
        assertEquals(Integer.valueOf(9), result.get("c"));
    }

    @Test
    public void verifyMapValueBiFunction() {
        Map<String, String> result = EntryStream.of(testMap).<String> mapValue((k, v) -> k + " has value " + v).toMap();
        assertEquals("a has value 1", result.get("a"));
        assertEquals("b has value 2", result.get("b"));
    }

    @Test
    public void verifyMapPartial() {
        Map<String, Integer> result = EntryStream.of(testMap)
                .mapPartial(e -> e.getValue() > 1 ? Optional.of(new AbstractMap.SimpleImmutableEntry<>(e.getKey().toUpperCase(), e.getValue() * 10))
                        : Optional.empty())
                .toMap();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(20), result.get("B"));
        assertEquals(Integer.valueOf(30), result.get("C"));
    }

    @Test
    public void verifyMapKeyPartial() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("1", 10);
        map.put("two", 20);
        map.put("3", 30);
        Map<Integer, Integer> result = EntryStream.of(map).<Integer> mapKeyPartial(k -> {
            try {
                return Optional.of(Integer.parseInt(k));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toMap();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(10), result.get(1));
        assertEquals(Integer.valueOf(30), result.get(3));
    }

    @Test
    public void verifyFlatMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 2);
        map.put("b", 1);
        Map<String, Integer> result = EntryStream.of(map).flatmap(e -> {
            Map<String, Integer> expanded = new LinkedHashMap<>();
            for (int i = 1; i <= e.getValue(); i++) {
                expanded.put(e.getKey() + "_item" + i, i);
            }
            return expanded;
        }).toMap();
        assertEquals(3, result.size());
    }

    @Test
    public void verifyGroupBy() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).groupBy().toMap();
        assertEquals(Arrays.asList(1, 3), result.get("a"));
        assertEquals(Arrays.asList(2, 4), result.get("b"));
    }

    @Test
    public void verifyGroupByMergeFunction() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).groupBy(Integer::sum).toMap();
        assertEquals(Integer.valueOf(4), result.get("a"));
        assertEquals(Integer.valueOf(6), result.get("b"));
    }

    @Test
    public void verifyToMap() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3).toMap();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(3), result.get("c"));
    }

    @Test
    public void verifyToMapWithMerge() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).toMap(Integer::sum);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(4), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void verifyToMapDuplicateKeysThrows() {
        boolean caught = false;
        try {
            EntryStream.of("a", 1, "a", 2).toMap();
        } catch (IllegalStateException e) {
            caught = true;
        }
        assertTrue(caught, "toMap() should throw IllegalStateException on duplicate keys");
    }

    @Test
    public void verifyToMultimap() {
        ListMultimap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).toMultimap();
        assertEquals(2, result.get("a").size());
        assertEquals(1, result.get("b").size());
    }

    @Test
    public void verifyGroupTo() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).groupTo();
        assertEquals(Arrays.asList(1, 3), result.get("a"));
        assertEquals(Arrays.asList(2, 4), result.get("b"));
    }

    @Test
    public void verifyJoin() {
        String result = EntryStream.of("a", 1, "b", 2, "c", 3).join(", ");
        assertEquals("a=1, b=2, c=3", result);
    }

    @Test
    public void verifyJoinWithPrefixSuffix() {
        String result = EntryStream.of("a", 1, "b", 2, "c", 3).join(", ", "{", "}");
        assertEquals("{a=1, b=2, c=3}", result);
    }

    @Test
    public void verifyJoinWithKeyValueDelimiter() {
        String result = EntryStream.of("a", 1, "b", 2, "c", 3).join(", ", ":");
        assertEquals("a:1, b:2, c:3", result);
    }

    @Test
    public void verifyJoinFourParam() {
        String result = EntryStream.of("a", 1, "b", 2, "c", 3).join(", ", ":", "{", "}");
        assertEquals("{a:1, b:2, c:3}", result);
    }

    @Test
    public void verifyJoinXmlStyle() {
        String xml = EntryStream.of("id", "123", "name", "Product").join("' ", "='", "<item ", "' />");
        N.println(xml);
        assertEquals("<item id='123' name='Product' />", xml);
    }

    @Test
    public void verifyReduceWithIdentity() {
        Map.Entry<String, Integer> maxEntry = EntryStream.of("a", 1, "b", 3, "c", 2)
                .reduce(new AbstractMap.SimpleEntry<>("", Integer.MIN_VALUE), (e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2);
        assertEquals("b", maxEntry.getKey());
        assertEquals(Integer.valueOf(3), maxEntry.getValue());
    }

    @Test
    public void verifyFindFirst() {
        Optional<Map.Entry<String, Integer>> found = EntryStream.of("a", 1, "b", 2, "c", 3).findFirst();
        assertTrue(found.isPresent());
        assertEquals("a", found.get().getKey());
        assertEquals(Integer.valueOf(1), found.get().getValue());
    }

    @Test
    public void verifyFindFirstEmpty() {
        Optional<Map.Entry<String, Integer>> found = EntryStream.<String, Integer> empty().findFirst();
        assertFalse(found.isPresent());
    }

    @Test
    public void verifyCount() {
        long total = EntryStream.of(testMap).count();
        assertEquals(3, total);
    }

    @Test
    public void verifyEmptyStream() {
        Map<String, Integer> result = EntryStream.<String, Integer> empty().toMap();
        assertTrue(result.isEmpty());
        assertEquals(0, EntryStream.empty().count());
    }

    @Test
    public void verifyOfNullable() {
        Map<String, Integer> result = EntryStream.ofNullable(new AbstractMap.SimpleImmutableEntry<>("x", 10)).toMap();
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(10), result.get("x"));
        assertEquals(0, EntryStream.ofNullable(null).count());
    }

    @Test
    public void verifyOfKeyValuePairs() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2).toMap();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void verifyIntersection() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("c", 3);
        map2.put("d", 4);
        Map<String, Integer> result = EntryStream.of(map1).intersection(map2).toMap();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
    }

    @Test
    public void verifyDifference() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("d", 4);
        Map<String, Integer> result = EntryStream.of(map1).difference(map2).toMap();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("c"));
    }

    @Test
    public void verifySortedByValue() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 3);
        map.put("b", 1);
        map.put("c", 2);
        List<String> keys = EntryStream.of(map).sortedByValue(Comparator.naturalOrder()).keys().toList();
        assertEquals(Arrays.asList("b", "c", "a"), keys);
    }

    @Test
    public void verifyToImmutableMap() {
        ImmutableMap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3).toImmutableMap();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
    }

    @Test
    public void verifyZip() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35, 40 };
        Map<String, Integer> result = EntryStream.zip(names, ages).toMap();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(25), result.get("Alice"));
    }

    @Test
    public void verifySkip() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).skip(2).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void verifyLimit() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).limit(2).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void verifyOnlyOne() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        Optional<Map.Entry<String, Integer>> result = EntryStream.of(map).onlyOne();
        assertTrue(result.isPresent());
        assertEquals("a", result.get().getKey());
    }

    @Test
    public void verifyOnlyOneTooMany() {
        boolean caught = false;
        try {
            EntryStream.of("a", 1, "b", 2).onlyOne();
        } catch (com.landawn.abacus.exception.TooManyElementsException e) {
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public void verifySortedThrows() {
        boolean caught = false;
        try {
            EntryStream.of("a", 1, "b", 2).sorted();
        } catch (UnsupportedOperationException e) {
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public void verifyReverseSortedThrows() {
        boolean caught = false;
        try {
            EntryStream.of("a", 1, "b", 2).reverseSorted();
        } catch (UnsupportedOperationException e) {
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public void verifyMapBiFunctionBiFunction() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("hello", 5);
        map.put("world", 10);
        Map<Integer, String> result = EntryStream.of(map).map((k, v) -> k.length() + v, (k, v) -> k + " has value " + v).toMap();
        assertEquals("hello has value 5", result.get(10));
        assertEquals("world has value 10", result.get(15));
    }

    @Test
    public void verifyDistinctByKey() {
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleImmutableEntry<>("a", 1), new AbstractMap.SimpleImmutableEntry<>("b", 2),
                new AbstractMap.SimpleImmutableEntry<>("a", 3));
        List<Entry<String, Integer>> result = EntryStream.of(entries).distinctByKey().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void verifyDistinctByValue() {
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleImmutableEntry<>("a", 1), new AbstractMap.SimpleImmutableEntry<>("b", 2),
                new AbstractMap.SimpleImmutableEntry<>("c", 1));
        List<Entry<String, Integer>> result = EntryStream.of(entries).distinctByValue().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void verifyCollapseByKey() {
        List<List<Integer>> result = EntryStream.of("a1", 1, "a2", 2, "b1", 3, "b2", 4, "c1", 5)
                .collapseByKey((k1, k2) -> k1.charAt(0) == k2.charAt(0))
                .toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    @Test
    public void verifySplit() {
        List<List<Entry<String, Integer>>> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).split(2).toList();
        assertEquals(3, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());
        assertEquals(1, result.get(2).size());
    }

    @Test
    public void verifyRotated() {
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleImmutableEntry<>("a", 1), new AbstractMap.SimpleImmutableEntry<>("b", 2),
                new AbstractMap.SimpleImmutableEntry<>("c", 3), new AbstractMap.SimpleImmutableEntry<>("d", 4));
        List<Entry<String, Integer>> result = EntryStream.of(entries).rotated(2).toList();
        assertEquals(4, result.size());
        assertEquals("c", result.get(0).getKey());
        assertEquals("d", result.get(1).getKey());
    }

    @Test
    public void verifyMin() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 3);
        map.put("c", 2);
        Optional<Map.Entry<String, Integer>> min = EntryStream.of(map).min(Map.Entry.comparingByValue());
        assertTrue(min.isPresent());
        assertEquals("a", min.get().getKey());
        assertEquals(Integer.valueOf(1), min.get().getValue());
    }

    @Test
    public void verifyToMapThenApply() {
        int size = EntryStream.of("a", 1, "b", 2, "c", 3).toMapThenApply(Map::size);
        assertEquals(3, size);
    }

    @Test
    public void verifyConcat() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("c", 3);
        map2.put("d", 4);
        List<Entry<String, Integer>> result = EntryStream.concat(map1, map2).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void verifyMapPartialBiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap)
                .mapPartial((k, v) -> v > 1 ? Optional.of(new AbstractMap.SimpleImmutableEntry<>(k.toUpperCase(), v * 10)) : Optional.empty())
                .toMap();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(20), result.get("B"));
        assertEquals(Integer.valueOf(30), result.get("C"));
    }

    @Test
    public void verifyMapKeyPartialBiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).<String> mapKeyPartial((k, v) -> v > 1 ? Optional.of(k.toUpperCase()) : Optional.empty()).toMap();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(2), result.get("B"));
        assertEquals(Integer.valueOf(3), result.get("C"));
    }

    @Test
    public void verifySymmetricDifference() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("c", 3);
        map2.put("d", 4);
        Map<String, Integer> result = EntryStream.of(map1).symmetricDifference(map2).toMap();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("d"));
    }

    @Test
    public void verifyMapMulti() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 2);
        map.put("b", 3);
        long count = EntryStream.of(map).mapMulti((entry, consumer) -> {
            for (int i = 1; i <= entry.getValue(); i++) {
                consumer.accept(new AbstractMap.SimpleImmutableEntry<>(entry.getKey() + i, i));
            }
        }).count();
        assertEquals(5, count);
    }

    @Test
    public void verifyPrependOptional() {
        Optional<Map.Entry<String, Integer>> optional = Optional.of(N.newEntry("x", 10));
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).prepend(optional).toList();
        assertEquals(3, result.size());
        assertEquals("x", result.get(0).getKey());
    }

    @Test
    public void verifyAppendIfEmpty() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("default", 0);
        List<Entry<String, Integer>> result = EntryStream.<String, Integer> empty().appendIfEmpty(defaults).toList();
        assertEquals(1, result.size());
        assertEquals("default", result.get(0).getKey());
    }

    @Test
    public void verifyOfMapWithFilter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Alice", 95);
        map.put("Bob", 87);
        map.put("Charlie", 92);
        Map<String, Integer> result = EntryStream.of(map).filterByValue(score -> score > 90).<Integer> mapValue(score -> score + 5).toMap();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(100), result.get("Alice"));
        assertEquals(Integer.valueOf(97), result.get("Charlie"));
    }

    @Test
    public void verifyJoinTo() {
        Joiner joiner = Joiner.with(", ", "=");
        EntryStream.of("a", 1, "b", 2, "c", 3).joinTo(joiner);
        assertEquals("a=1, b=2, c=3", joiner.toString());
    }

    @Test
    public void verifyCycled() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        List<Entry<String, Integer>> result = EntryStream.of(map).cycled(3).toList();
        assertEquals(6, result.size());
    }
}
