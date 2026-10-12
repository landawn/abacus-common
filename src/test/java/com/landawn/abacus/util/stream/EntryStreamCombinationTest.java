package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.u.Optional;

/** EntryStream combination contracts. */
public class EntryStreamCombinationTest extends EntryStreamTestSupport {

    // append

    @Test
    public void testAppend() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        EntryStream<String, Integer> s1 = EntryStream.of(map1);
        EntryStream<String, Integer> s2 = EntryStream.of(map2);

        assertEquals(2, s1.append(s2).count());
    }

    @Test
    public void testAppend_Map() {
        Map<String, Integer> append = N.asMap("six", 6);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).append(append).toList();
        assertEquals(6, result.size());
        assertEquals("six", result.get(5).getKey());
    }

    @Test
    public void testAppend_MultipleEntries() {
        Map<String, Integer> append1 = N.asMap("six", 6);
        Map<String, Integer> append2 = N.asMap("seven", 7);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).append(append1).append(append2).toList();
        assertEquals(7, result.size());
        assertEquals("seven", result.get(6).getKey());
    }

    @Test
    public void testAppend_Optional() {
        Optional<Entry<String, Integer>> op = Optional.of(new AbstractMap.SimpleEntry<>("six", 6));
        List<Entry<String, Integer>> result = EntryStream.of(testMap).append(op).toList();
        assertEquals(6, result.size());
        assertEquals("six", result.get(5).getKey());
    }

    @Test
    public void testAppend_Stream() {
        EntryStream<String, Integer> append = EntryStream.of("six", 6);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).append(append).toList();
        assertEquals(6, result.size());
        assertEquals("six", result.get(5).getKey());
    }

    @Test
    public void testAppendOptional() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Optional<Map.Entry<String, Integer>> optional = Optional.of(new AbstractMap.SimpleEntry<>("x", 10));

        List<Map.Entry<String, Integer>> result = stream.append(optional).toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
        assertEquals("x", result.get(2).getKey());
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
    public void testAppendStream() {
        EntryStream<String, Integer> main = EntryStream.of("a", 1, "b", 2);
        EntryStream<String, Integer> suffix = EntryStream.of("x", 10, "y", 20);

        List<Map.Entry<String, Integer>> result = main.append(suffix).toList();
        assertEquals(4, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
        assertEquals("x", result.get(2).getKey());
        assertEquals("y", result.get(3).getKey());
    }

    // appendIfEmpty

    @Test
    public void testAppendIfEmpty() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        final Map<String, Integer> emptyMap = Collections.emptyMap();

        assertEquals(1, EntryStream.of(map).appendIfEmpty(emptyMap).count());
        assertEquals(1, EntryStream.of(emptyMap).appendIfEmpty(map).count());

    }

    @Test
    public void testAppendIfEmpty_Map() {
        Map<String, Integer> append = N.asMap("default", 0);
        List<Entry<String, Integer>> result = EntryStream.<String, Integer> empty().appendIfEmpty(append).toList();
        assertEquals(1, result.size());
        assertEquals("default", result.get(0).getKey());
    }

    @Test
    public void testAppendIfEmpty_Supplier() {
        List<Entry<String, Integer>> result = EntryStream.<String, Integer> empty()
                .appendIfEmpty((Supplier<EntryStream<String, Integer>>) () -> EntryStream.of("default", 0))
                .toList();
        assertEquals(1, result.size());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        Supplier<EntryStream<String, Integer>> defaultSupplier = () -> EntryStream.of("default", 0);

        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        List<Map.Entry<String, Integer>> result1 = emptyStream.appendIfEmpty(defaultSupplier).toList();
        assertEquals(1, result1.size());
        assertEquals("default", result1.get(0).getKey());

        EntryStream<String, Integer> nonEmptyStream = EntryStream.of("a", 1);
        List<Map.Entry<String, Integer>> result2 = nonEmptyStream.appendIfEmpty(defaultSupplier).toList();
        assertEquals(1, result2.size());
        assertEquals("a", result2.get(0).getKey());
    }

    // concat

    @Test
    public void testConcat() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        assertEquals(2, EntryStream.concat(map1, map2).count());
    }

    @Test
    public void testConcat_MapCollection() {
        List<Map<String, Integer>> maps = Arrays.asList(N.asMap("a", 1), N.asMap("b", 2));
        Map<String, Integer> result = EntryStream.concat(maps).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testConcat_Maps() {
        Map<String, Integer> map1 = N.asMap("a", 1);
        Map<String, Integer> map2 = N.asMap("b", 2);
        Map<String, Integer> result = EntryStream.concat(map1, map2).toMap();
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    public void testConcatCollection() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        List<Map<String, Integer>> maps = Arrays.asList(map1, map2);
        EntryStream<String, Integer> concatenated = EntryStream.concat(maps);
        Map<String, Integer> result = concatenated.toMap();

        assertEquals(2, result.size());
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

        List<Map<String, Integer>> maps = Arrays.asList(map1, map2, map3);
        Map<String, Integer> concatenated = EntryStream.concat(maps).toMap();

        assertEquals(5, concatenated.size());
        assertEquals(1, concatenated.get("a"));
        assertEquals(5, concatenated.get("e"));
    }

    @Test
    public void testConcatWithEmptyMaps() {
        Map<String, Integer> empty1 = new HashMap<>();
        Map<String, Integer> empty2 = new HashMap<>();
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("a", 1);

        Map<String, Integer> result1 = EntryStream.concat(empty1, empty2, nonEmpty).toMap();
        assertEquals(1, result1.size());
        assertEquals(1, result1.get("a"));

        Collection<Map<String, Integer>> emptyCollection = Collections.emptyList();
        assertEquals(0, EntryStream.concat(emptyCollection).count());

        Collection<Map<String, Integer>> collectionWithNull = Arrays.asList(nonEmpty, null, empty1);
        assertEquals(1, EntryStream.concat(collectionWithNull).count());
    }

    // join

    @Test
    public void testJoin() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");

        final String result = EntryStream.of(map).join(", ", "=", "[", "]");
        assertEquals("[1=a, 2=b]", result);
    }

    @Test
    public void testJoin_Delimiter() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ");
        assertTrue(result.contains("a"));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testJoin_DelimiterFromBase() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ");
        assertNotNull(result);
    }

    @Test
    public void testJoin_DelimiterPrefixSuffix() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ", "[", "]");
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    @Test
    public void testJoin_WithKeyValueDelimiter() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ", "=");
        assertTrue(result.contains("a=1"));
    }

    @Test
    public void testJoin_WithKeyValueDelimiterAndPrefixSuffix() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ", "=", "{", "}");
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("a=1"));
    }

    @Test
    public void testJoinWithAllDelimiters() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        String result = stream.join(", ", ":", "{", "}");

        assertEquals("{a:1, b:2}", result);
    }

    @Test
    public void testJoinWithKeyValueDelimiter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        String result = stream.join(", ", ":");

        assertEquals("a:1, b:2", result);
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        String result = stream.join(", ", "{", "}");

        assertEquals("{a=1, b=2}", result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ");
        Joiner result = EntryStream.of("a", 1, "b", 2).joinTo(joiner);
        assertNotNull(result);
    }

    // merge

    @Test
    public void testMerge() {
        Map<String, Integer> mapA = new HashMap<>();
        mapA.put("a", 1);
        mapA.put("b", 2);
        Map<String, Integer> mapB = new HashMap<>();
        mapB.put("c", 3);
        mapB.put("d", 4);

        final Map<String, Integer> mergedMap = EntryStream.merge(mapA, mapB,
                (e1, e2) -> Comparator.<String> naturalOrder().compare(e1.getKey(), e2.getKey()) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toMap();

        assertEquals(4, mergedMap.size());
    }

    @Test
    public void testMerge_ThreeMaps_Basic() {
        TreeMap<String, Integer> mapA = new TreeMap<>();
        mapA.put("a", 1);
        mapA.put("d", 4);
        TreeMap<String, Integer> mapB = new TreeMap<>();
        mapB.put("b", 2);
        mapB.put("e", 5);
        TreeMap<String, Integer> mapC = new TreeMap<>();
        mapC.put("c", 3);
        mapC.put("f", 6);

        List<Map.Entry<String, Integer>> result = EntryStream
                .merge(mapA, mapB, mapC, (e1, e2) -> e1.getKey().compareTo(e2.getKey()) <= 0 ? com.landawn.abacus.util.MergeResult.TAKE_FIRST
                        : com.landawn.abacus.util.MergeResult.TAKE_SECOND)
                .toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testMerge_TwoMaps_EmptyFirst() {
        // merge(Map, Map, Map, BiFunction) - empty first map branches
        Map<String, Integer> empty = new LinkedHashMap<>();
        Map<String, Integer> mapA = new LinkedHashMap<>();
        mapA.put("a", 1);
        Map<String, Integer> mapB = new LinkedHashMap<>();
        mapB.put("b", 2);

        List<Map.Entry<String, Integer>> result = EntryStream.merge(empty, mapA, mapB, (e1, e2) -> MergeResult.TAKE_FIRST).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testMerge_TwoMaps_EmptySecond() {
        Map<String, Integer> mapA = new LinkedHashMap<>();
        mapA.put("a", 1);
        Map<String, Integer> empty = new LinkedHashMap<>();
        Map<String, Integer> mapB = new LinkedHashMap<>();
        mapB.put("b", 2);

        List<Map.Entry<String, Integer>> result = EntryStream.merge(mapA, empty, mapB, (e1, e2) -> MergeResult.TAKE_FIRST).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testMerge_TwoMaps_EmptyThird() {
        Map<String, Integer> mapA = new LinkedHashMap<>();
        mapA.put("a", 1);
        Map<String, Integer> mapB = new LinkedHashMap<>();
        mapB.put("b", 2);
        Map<String, Integer> empty = new LinkedHashMap<>();

        List<Map.Entry<String, Integer>> result = EntryStream.merge(mapA, mapB, empty, (e1, e2) -> MergeResult.TAKE_FIRST).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testMergeCollectionOfMaps() {
        Map<Integer, String> map1 = new TreeMap<>();
        map1.put(1, "a");

        Map<Integer, String> map2 = new TreeMap<>();
        map2.put(2, "b");

        List<Map<Integer, String>> maps = Arrays.asList(map1, map2);

        EntryStream<Integer, String> merged = EntryStream.merge(maps, (e1, e2) -> e1.getKey() < e2.getKey() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        List<Map.Entry<Integer, String>> result = merged.toList();
        assertEquals(2, result.size());
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

        List<Entry<String, Integer>> merged = EntryStream
                .merge(map1, map2, map3, (e1, e2) -> e1.getKey().compareTo(e2.getKey()) < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList();

        assertEquals(6, merged.size());
        assertEquals("a", merged.get(0).getKey());
        assertEquals("b", merged.get(1).getKey());
        assertEquals("c", merged.get(2).getKey());

        List<Map<String, Integer>> mapList = Arrays.asList(map1, map2, map3);
        List<Entry<String, Integer>> mergedFromCollection = EntryStream
                .merge(mapList, (e1, e2) -> e1.getValue() < e2.getValue() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList();

        assertEquals(6, mergedFromCollection.size());
    }

    @Test
    public void testMergeThreeMaps() {
        Map<Integer, String> map1 = new TreeMap<>();
        map1.put(1, "a");

        Map<Integer, String> map2 = new TreeMap<>();
        map2.put(2, "b");

        Map<Integer, String> map3 = new TreeMap<>();
        map3.put(3, "c");

        EntryStream<Integer, String> merged = EntryStream.merge(map1, map2, map3,
                (e1, e2) -> e1.getKey() < e2.getKey() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        List<Map.Entry<Integer, String>> result = merged.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testMergeTwoMaps() {
        Map<Integer, String> map1 = new TreeMap<>();
        map1.put(1, "a");
        map1.put(3, "c");

        Map<Integer, String> map2 = new TreeMap<>();
        map2.put(2, "b");
        map2.put(4, "d");

        EntryStream<Integer, String> merged = EntryStream.merge(map1, map2,
                (e1, e2) -> e1.getKey() < e2.getKey() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        List<Map.Entry<Integer, String>> result = merged.toList();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0).getKey());
        assertEquals(2, result.get(1).getKey());
        assertEquals(3, result.get(2).getKey());
        assertEquals(4, result.get(3).getKey());
    }

    @Test
    public void testMergeWithEmptyMaps() {
        Map<String, Integer> empty = new HashMap<>();
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("a", 1);
        nonEmpty.put("b", 2);

        BiFunction<Entry<String, Integer>, Entry<String, Integer>, MergeResult> selector = (e1, e2) -> MergeResult.TAKE_FIRST;

        List<Entry<String, Integer>> merged1 = EntryStream.merge(empty, nonEmpty, selector).toList();
        assertEquals(2, merged1.size());

        List<Entry<String, Integer>> merged2 = EntryStream.merge(nonEmpty, empty, selector).toList();
        assertEquals(2, merged2.size());

        List<Entry<String, Integer>> merged3 = EntryStream.merge(empty, empty, selector).toList();
        assertEquals(0, merged3.size());

        Map<String, Integer> another = new HashMap<>();
        another.put("c", 3);

        List<Entry<String, Integer>> merged4 = EntryStream.merge(empty, nonEmpty, another, selector).toList();
        assertEquals(3, merged4.size());
    }

    // prepend

    @Test
    public void testPrepend() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        EntryStream<String, Integer> s1 = EntryStream.of(map1);
        EntryStream<String, Integer> s2 = EntryStream.of(map2);

        List<String> keys = s1.prepend(s2).keys().toList();

        assertEquals(Arrays.asList("b", "a"), keys);
    }

    @Test
    public void testPrepend_EmptyStream() {
        EntryStream<String, Integer> prepend = EntryStream.empty();
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(prepend).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testPrepend_Map() {
        Map<String, Integer> prepend = N.asMap("zero", 0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(prepend).toList();
        assertEquals(6, result.size());
        assertEquals("zero", result.get(0).getKey());
    }

    @Test
    public void testPrepend_MultipleEntries() {
        Map<String, Integer> prepend1 = N.asMap("zero", 0);
        Map<String, Integer> prepend2 = N.asMap("negative", -1);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(prepend1).prepend(prepend2).toList();
        assertEquals(7, result.size());
        assertEquals("negative", result.get(0).getKey());
    }

    @Test
    public void testPrepend_Optional() {
        Optional<Entry<String, Integer>> op = Optional.of(new AbstractMap.SimpleEntry<>("zero", 0));
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(op).toList();
        assertEquals(6, result.size());
        assertEquals("zero", result.get(0).getKey());
    }

    @Test
    public void testPrepend_Stream() {
        EntryStream<String, Integer> prepend = EntryStream.of("zero", 0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(prepend).toList();
        assertEquals(6, result.size());
        assertEquals("zero", result.get(0).getKey());
    }

    @Test
    public void testPrependOptional() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Optional<Map.Entry<String, Integer>> optional = Optional.of(new AbstractMap.SimpleEntry<>("x", 10));

        List<Map.Entry<String, Integer>> result = stream.prepend(optional).toList();
        assertEquals(3, result.size());
        assertEquals("x", result.get(0).getKey());
        assertEquals("a", result.get(1).getKey());
        assertEquals("b", result.get(2).getKey());
    }

    @Test
    public void testPrependStream() {
        EntryStream<String, Integer> main = EntryStream.of("a", 1, "b", 2);
        EntryStream<String, Integer> prefix = EntryStream.of("x", 10, "y", 20);

        List<Map.Entry<String, Integer>> result = main.prepend(prefix).toList();
        assertEquals(4, result.size());
        assertEquals("x", result.get(0).getKey());
        assertEquals("y", result.get(1).getKey());
        assertEquals("a", result.get(2).getKey());
        assertEquals("b", result.get(3).getKey());
    }

    @Test
    public void testPrependIfEmpty() {
        final Map<String, Integer> map1 = N.asMap("a", 1);
        final Map<String, Integer> emptyMap = Collections.emptyMap();

        assertEquals(1, EntryStream.of(map1).appendIfEmpty(emptyMap).count());
        assertEquals(1, EntryStream.of(emptyMap).appendIfEmpty(map1).count());
        assertEquals(0, EntryStream.of(emptyMap).appendIfEmpty(emptyMap).count());
    }

    @Test
    public void testPrependIfEmptyWithSupplier() {
        Map<String, Integer> map = N.asMap("a", 1);
        assertEquals(1, EntryStream.<String, Integer> empty().appendIfEmpty(Fn.s(() -> EntryStream.of(map))).count());
        assertEquals(1, EntryStream.of(map).appendIfEmpty(Fn.s(() -> EntryStream.of(N.asMap("b", 2)))).count());
    }

    // zip

    @Test
    public void testZip() {
        final List<String> keys = Arrays.asList("a", "b");
        final List<Integer> values = Arrays.asList(1, 2);

        final Map<String, Integer> map = EntryStream.zip(keys, values).toMap();
        assertEquals(2, map.size());
        assertEquals(1, (int) map.get("a"));
        assertEquals(2, (int) map.get("b"));
    }

    @Test
    public void testZip_Arrays() {
        String[] keys = { "a", "b", "c" };
        Integer[] values = { 1, 2, 3 };
        Map<String, Integer> result = EntryStream.zip(keys, values).toMap();
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
    }

    @Test
    public void testZip_Arrays_EmptyFirst() {
        // zip(K[], V[]) with empty first array
        String[] emptyKeys = new String[0];
        Integer[] values = { 1, 2 };
        List<Map.Entry<String, Integer>> result = EntryStream.zip(emptyKeys, values).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testZip_ArraysWithDefaults() {
        String[] keys = { "a", "b", "c" };
        Integer[] values = { 1, 2 };
        Map<String, Integer> result = EntryStream.zip(keys, values, "default", 0).toMap();
        assertEquals(3, result.size());
        assertEquals(0, result.get("c"));
    }

    @Test
    public void testZip_Iterables() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = EntryStream.zip(keys, values).toMap();
        assertEquals(3, result.size());
    }

    @Test
    public void testZip_Iterables_EmptyFirst() {
        // zip(Iterable, Iterable) with empty iterable
        List<String> emptyKeys = new ArrayList<>();
        List<Integer> values = Arrays.asList(1, 2);
        List<Map.Entry<String, Integer>> result = EntryStream.zip(emptyKeys, values).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testZip_Iterables_WithDefaults_EmptyFirst() {
        // zip(Iterable, Iterable, K, V) with defaults — result is max length
        List<String> emptyKeys = new ArrayList<>();
        List<Integer> values = Arrays.asList(1, 2);
        List<Map.Entry<String, Integer>> result = EntryStream.zip(emptyKeys, values, "none", 0).toList();
        assertEquals(2, result.size());
        assertEquals("none", result.get(0).getKey());
        assertEquals(1, (int) result.get(0).getValue());
    }

    @Test
    public void testZip_IterablesWithDefaults() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> result = EntryStream.zip(keys, values, "default", 0).toMap();
        assertEquals(3, result.size());
        assertEquals(0, result.get("c"));
    }

    @Test
    public void testZip_IterablesWithDefaults_Basic() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2);

        Map<String, Integer> result = EntryStream.zip(keys, values, "default", 0).toMap();
        assertEquals(3, result.size());
        assertEquals(1, (int) result.get("a"));
        assertEquals(2, (int) result.get("b"));
        assertEquals(0, (int) result.get("c"));
    }

    @Test
    public void testZip_Iterator_EmptyFirst() {
        // zip(Iterator, Iterator) with empty iterator
        Iterator<String> emptyIter = new ArrayList<String>().iterator();
        Iterator<Integer> valueIter = Arrays.asList(1, 2).iterator();
        List<Map.Entry<String, Integer>> result = EntryStream.zip(emptyIter, valueIter).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testZip_Iterators() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> result = EntryStream.zip(keys.iterator(), values.iterator()).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testZip_IteratorsWithDefaults() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> result = EntryStream.zip(keys.iterator(), values.iterator(), "default", 0).toMap();
        assertEquals(3, result.size());
    }

    @Test
    public void testZipArrays() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35, 40 };

        EntryStream<String, Integer> zipped = EntryStream.zip(names, ages);
        Map<String, Integer> result = zipped.toMap();

        assertEquals(3, result.size());
        assertEquals(25, result.get("Alice"));
        assertEquals(30, result.get("Bob"));
        assertEquals(35, result.get("Charlie"));
    }

    @Test
    public void testZipArraysWithDefaults() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30, 35 };

        EntryStream<String, Integer> zipped = EntryStream.zip(names, ages, "Unknown", -1);
        List<Map.Entry<String, Integer>> result = zipped.toList();

        assertEquals(3, result.size());
        assertEquals("Unknown", result.get(2).getKey());
        assertEquals(35, result.get(2).getValue());
    }

    @Test
    public void testZipIterables() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        List<Integer> ages = Arrays.asList(25, 30);

        EntryStream<String, Integer> zipped = EntryStream.zip(names, ages);
        Map<String, Integer> result = zipped.toMap();

        assertEquals(2, result.size());
        assertEquals(25, result.get("Alice"));
        assertEquals(30, result.get("Bob"));
    }

    @Test
    public void testZipIterablesWithDefaults() {
        List<String> names = Arrays.asList("Alice", "Bob");
        List<Integer> ages = Arrays.asList(25, 30, 35);

        EntryStream<String, Integer> zipped = EntryStream.zip(names, ages, "Unknown", -1);
        List<Map.Entry<String, Integer>> result = zipped.toList();

        assertEquals(3, result.size());
        assertEquals("Unknown", result.get(2).getKey());
        assertEquals(35, result.get(2).getValue());
    }

    @Test
    public void testZipIterators() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        List<Integer> ages = Arrays.asList(25, 30);

        EntryStream<String, Integer> zipped = EntryStream.zip(names.iterator(), ages.iterator());
        Map<String, Integer> result = zipped.toMap();

        assertEquals(2, result.size());
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        List<String> names = Arrays.asList("Alice");
        List<Integer> ages = Arrays.asList(25, 30);

        EntryStream<String, Integer> zipped = EntryStream.zip(names.iterator(), ages.iterator(), "Unknown", -1);
        List<Map.Entry<String, Integer>> result = zipped.toList();

        assertEquals(2, result.size());
        assertEquals("Unknown", result.get(1).getKey());
        assertEquals(30, result.get(1).getValue());
    }

    @Test
    public void testZipOperationsEdgeCases() {
        String[] keys = { "a", "b", "c", "d" };
        Integer[] values = { 1, 2 };

        Map<String, Integer> result = EntryStream.zip(keys, values, "default", 0).toMap();

        assertEquals(4, result.size());
        assertEquals(0, result.get("c"));
        assertEquals(0, result.get("d"));
    }

    @Test
    public void testZipWithDefaultsFillsNullSide() {
        // regression: a null side returned an empty stream, silently dropping the other side's
        // elements, while the array sibling fills the null side with the default value
        assertEquals(Arrays.asList(N.newEntry("K", 1), N.newEntry("K", 2)), EntryStream.zip((Iterable<String>) null, Arrays.asList(1, 2), "K", 0).toList());
        assertEquals(Arrays.asList(N.newEntry("a", 0)), EntryStream.zip(Arrays.asList("a"), (Iterable<Integer>) null, "K", 0).toList());
        assertEquals(Arrays.asList(N.newEntry("K", 1)), EntryStream.zip((Iterator<String>) null, Arrays.asList(1).iterator(), "K", 0).toList());

        // both sides null is still empty
        assertTrue(EntryStream.zip((Iterable<String>) null, (Iterable<Integer>) null, "K", 0).toList().isEmpty());
    }

    @Test
    public void testZipWithDefaultValues() {
        String[] keys = { "a", "b" };
        Integer[] values = { 1, 2, 3, 4 };

        Map<String, Integer> zipped = EntryStream.zip(keys, values, "default", 99).toMap(Fn.replacingMerger());
        assertEquals(3, zipped.size());
        assertEquals(1, zipped.get("a"));
        assertEquals(2, zipped.get("b"));
        assertEquals(4, zipped.get("default"));

        List<String> keyList = Arrays.asList("x", "y", "z");
        List<Integer> valueList = Arrays.asList(10);

        List<Entry<String, Integer>> zippedList = EntryStream.zip(keyList, valueList, "none", 0).toList();
        assertEquals(3, zippedList.size());
        assertEquals(10, zippedList.get(0).getValue());
        assertEquals(0, zippedList.get(1).getValue());
        assertEquals(0, zippedList.get(2).getValue());
    }

    @Test
    public void testZipWithIterators() {
        Iterator<String> nullKeys = null;
        Iterator<Integer> nullValues = null;

        assertEquals(0, EntryStream.zip(nullKeys, Arrays.asList(1, 2, 3).iterator()).count());
        assertEquals(0, EntryStream.zip(Arrays.asList("a", "b").iterator(), nullValues).count());
        assertEquals(0, EntryStream.zip(nullKeys, nullValues).count());

        Iterator<String> keys = Arrays.asList("x", "y").iterator();
        Iterator<Integer> values = Arrays.asList(10, 20, 30, 40).iterator();

        List<Entry<String, Integer>> zipped = EntryStream.zip(keys, values, "default", 99).toList();
        assertEquals(4, zipped.size());
        assertEquals("x", zipped.get(0).getKey());
        assertEquals(10, zipped.get(0).getValue());
        assertEquals("y", zipped.get(1).getKey());
        assertEquals(20, zipped.get(1).getValue());
        assertEquals("default", zipped.get(2).getKey());
        assertEquals(30, zipped.get(2).getValue());
        assertEquals("default", zipped.get(3).getKey());
        assertEquals(40, zipped.get(3).getValue());
    }

    @Test
    public void testZipWithNullIterables() {
        Iterable<String> nullKeys = null;
        Iterable<Integer> nullValues = null;

        assertEquals(0, EntryStream.zip(nullKeys, Arrays.asList(1, 2, 3)).count());
        assertEquals(0, EntryStream.zip(Arrays.asList("a", "b"), nullValues).count());

        assertEquals(0, EntryStream.zip(nullKeys, nullValues, "default", 0).count());
    }

}
