package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.ToIntFunction;


public class Stream200Test extends TestBase {

    // Helper to get a List from a Stream for assertions
    private <T> List<T> toList(Stream<T> stream) {
        if (stream == null) {
            return Collections.emptyList();
        }
        return stream.toList();
    }

    private <T> Set<T> toSet(Stream<T> stream) {
        if (stream == null) {
            return Collections.emptySet();
        }
        return stream.toSet();
    }

    @Test
    public void testFilter() {
        assertEquals(Arrays.asList(2, 4), Stream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0).toList());
        assertEquals(Collections.emptyList(), Stream.<Integer> empty().filter(x -> x % 2 == 0).toList());
        assertTrue(N.isEqualCollection(Arrays.asList(2, 4), Stream.of(1, 2, 3, 4, 5).parallel().filter(x -> x % 2 == 0).toList()));
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        List<Integer> droppedItems = new ArrayList<>();
        assertEquals(Arrays.asList(2, 4), Stream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, droppedItems::add).toList());
        assertEquals(Arrays.asList(1, 3, 5), droppedItems);

        droppedItems.clear();
        assertEquals(Collections.emptyList(), Stream.<Integer> empty().filter(x -> x % 2 == 0, droppedItems::add).toList());
        assertTrue(droppedItems.isEmpty());
    }

    @Test
    public void testTakeWhile() {
        assertEquals(Arrays.asList(1, 2), Stream.of(1, 2, 3, 1, 2).takeWhile(x -> x < 3).toList());
        assertEquals(Collections.emptyList(), Stream.of(3, 1, 2).takeWhile(x -> x < 3).toList());
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).takeWhile(x -> x < 5).toList());
        // Parallel behavior can be complex as per Javadoc, testing simple ordered case.
        List<Integer> resultParallel = Stream.of(1, 2, 3, 4, 5, 0).parallel(2).takeWhile(it -> it < 4).toList();
        assertTrue(resultParallel.containsAll(Arrays.asList(1, 2, 3)) && resultParallel.size() <= 4 && resultParallel.stream().allMatch(i -> i < 4));
    }

    @Test
    public void testDropWhile() {
        assertEquals(Arrays.asList(3, 1, 2), Stream.of(1, 2, 3, 1, 2).dropWhile(x -> x < 3).toList());
        assertEquals(Arrays.asList(3, 1, 2), Stream.of(3, 1, 2).dropWhile(x -> x < 3).toList());
        assertEquals(Collections.emptyList(), Stream.of(1, 2, 3).dropWhile(x -> x < 5).toList());
        // Parallel behavior can be complex.
        List<Integer> resultParallel = Stream.of(1, 2, 3, 4, 0, 5).parallel(2).dropWhile(it -> it < 3).toList();
        assertTrue(resultParallel.containsAll(Arrays.asList(3, 4, 0, 5)) || resultParallel.containsAll(Arrays.asList(4, 0, 5))); // Order and exact drop point may vary in parallel
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).dropWhile(x -> x < 3, dropped::add).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testSkipUntil() {
        assertEquals(Arrays.asList(3, 1, 2), Stream.of(1, 2, 3, 1, 2).skipUntil(x -> x >= 3).toList());
        assertEquals(Arrays.asList(3, 1, 2), Stream.of(3, 1, 2).skipUntil(x -> x >= 3).toList());
        assertEquals(Collections.emptyList(), Stream.of(1, 2, 3).skipUntil(x -> x > 5).toList());
    }

    @Test
    public void testOnEachAndPeek() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> onEached = new ArrayList<>();

        List<Integer> resultPeek = Stream.of(1, 2).peek(peeked::add).map(x -> x * 2).toList();
        assertEquals(Arrays.asList(1, 2), peeked);
        assertEquals(Arrays.asList(2, 4), resultPeek);

        List<Integer> resultOnEach = Stream.of(1, 2).onEach(onEached::add).map(x -> x * 2).toList();
        assertEquals(Arrays.asList(1, 2), onEached);
        assertEquals(Arrays.asList(2, 4), resultOnEach);
    }

    @Test
    public void testSelect() {
        Stream<Object> mixedStream = Stream.of(1, "hello", 2.0, "world", 3);
        List<String> strings = mixedStream.select(String.class).toList();
        assertEquals(Arrays.asList("hello", "world"), strings);

        Stream<Object> noMatchingTypeStream = Stream.of(1, 2, 3);
        List<String> noStrings = noMatchingTypeStream.select(String.class).toList();
        assertTrue(noStrings.isEmpty());
    }

    @Test
    public void testPairWith() {
        List<Pair<Integer, String>> pairs = Stream.of(1, 2, 3).pairWith(x -> "val" + x).toList();
        assertEquals(3, pairs.size());
        assertEquals(Pair.of(1, "val1"), pairs.get(0));
        assertEquals(Pair.of(2, "val2"), pairs.get(1));
        assertEquals(Pair.of(3, "val3"), pairs.get(2));
    }

    @Test
    public void testMap() {
        assertEquals(Arrays.asList("A", "B", "C"), Stream.of("a", "b", "c").map(String::toUpperCase).toList());
        assertEquals(Collections.emptyList(), Stream.<String> empty().map(String::toUpperCase).toList());
    }

    @Test
    public void testMapIfNotNull() {
        assertEquals(Arrays.asList("A", "C"), Stream.of("a", null, "c").mapIfNotNull(String::toUpperCase).toList());
        assertEquals(Collections.emptyList(), Stream.<String> of(null, null).mapIfNotNull(String::toUpperCase).toList());
    }

    @Test
    public void testSlidingMapBiFunction() {
        List<String> result = Stream.of(1, 2, 3, 4).slidingMap((a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        // Default increment is 1, and non-paired are included with null
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), result);

        result = Stream.of(1).slidingMap((a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-null"), result);

        result = Stream.<Integer> empty().slidingMap((a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingMapBiFunctionWithIncrement() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).slidingMap(2, (a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-2", "3-4", "5-null"), result);
    }

    @Test
    public void testSlidingMapBiFunctionWithIncrementAndIgnore() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-2", "3-4"), result); // 5 is ignored

        result = Stream.of(1, 2, 3, 4).slidingMap(2, false, (a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-2", "3-4"), result); // No unpaired at the end with even length
    }

    @Test
    public void testSlidingMapTriFunction() {
        List<String> result = Stream.of(1, 2, 3, 4, 5)
                .slidingMap((a, b, c) -> (a == null ? "N" : a) + "-" + (b == null ? "N" : b) + "-" + (c == null ? "N" : c))
                .toList();
        // Default increment is 1
        assertEquals(Arrays.asList("1-2-3", "2-3-4", "3-4-5"), result);
    }

    @Test
    public void testSlidingMapTriFunctionWithIncrementAndIgnore() {
        List<String> result = Stream.of(1, 2, 3, 4, 5, 6, 7)
                .slidingMap(2, true, (a, b, c) -> (a == null ? "N" : a) + "-" + (b == null ? "N" : b) + "-" + (c == null ? "N" : c))
                .toList();
        assertEquals(Arrays.asList("1-2-3", "3-4-5", "5-6-7"), result);

        result = Stream.of(1, 2, 3, 4, 5, 6)
                .slidingMap(2, false, (a, b, c) -> (a == null ? "N" : a) + "-" + (b == null ? "N" : b) + "-" + (c == null ? "N" : c))
                .toList();
        assertEquals(Arrays.asList("1-2-3", "3-4-5", "5-6-N"), result);
    }

    @Test
    public void testRangeMap() {
        List<String> result = Stream.of("a", "aa", "ab", "b", "c", "ca")
                .rangeMap((s1, s2) -> s2.startsWith(String.valueOf(s1.charAt(0))), (s1, s2) -> s1 + "->" + s2)
                .toList();
        assertEquals(Arrays.asList("a->ab", "b->b", "c->ca"), result);

        result = Stream.of(1, 2, 3, 5, 6, 8).rangeMap((prev, curr) -> curr - prev <= 1, (first, last) -> first + ".." + last).toList();
        assertEquals(Arrays.asList("1..2", "3..3", "5..6", "8..8"), result);

        result = Stream.<Integer> empty().rangeMap((prev, curr) -> curr - prev <= 1, (first, last) -> first + ".." + last).toList();
        assertTrue(result.isEmpty());

        result = Stream.of(1).rangeMap((prev, curr) -> curr - prev <= 1, (first, last) -> first + ".." + last).toList();
        assertEquals(Arrays.asList("1..1"), result);
    }

    @Test
    public void testMapFirst() {
        assertEquals(Arrays.asList(10, 2, 3), Stream.of(1, 2, 3).mapFirst(x -> x * 10).toList());
        assertEquals(Collections.emptyList(), Stream.<Integer> empty().mapFirst(x -> x * 10).toList());
        assertEquals(Arrays.asList(100), Stream.of(10).mapFirst(x -> x * 10).toList());
    }

    @Test
    public void testMapFirstOrElse() {
        assertEquals(Arrays.asList(10, -2, -3), Stream.of(1, 2, 3).mapFirstOrElse(x -> x * 10, y -> -y).toList());
        assertEquals(Collections.emptyList(), Stream.<Integer> empty().mapFirstOrElse(x -> x * 10, y -> -y).toList());
        assertEquals(Arrays.asList(100), Stream.of(10).mapFirstOrElse(x -> x * 10, y -> -y).toList());
    }

    @Test
    public void testMapLast() {
        assertEquals(Arrays.asList(1, 2, 30), Stream.of(1, 2, 3).mapLast(x -> x * 10).toList());
        assertEquals(Collections.emptyList(), Stream.<Integer> empty().mapLast(x -> x * 10).toList());
        assertEquals(Arrays.asList(100), Stream.of(10).mapLast(x -> x * 10).toList());
    }

    @Test
    public void testMapLastOrElse() {
        assertEquals(Arrays.asList(-1, -2, 30), Stream.of(1, 2, 3).mapLastOrElse(x -> x * 10, y -> -y).toList());
        assertEquals(Collections.emptyList(), Stream.<Integer> empty().mapLastOrElse(x -> x * 10, y -> -y).toList());
        assertEquals(Arrays.asList(100), Stream.of(10).mapLastOrElse(x -> x * 10, y -> -y).toList());
    }

    @Test
    public void testMapToChar() {
        assertArrayEquals(new char[] { 'a', 'b' }, Stream.of("a", "b").mapToChar(s -> s.charAt(0)).toArray());
    }

    @Test
    public void testMapToByte() {
        assertArrayEquals(new byte[] { 1, 2 }, Stream.of(1, 2).mapToByte(Integer::byteValue).toArray());
    }

    @Test
    public void testMapToShort() {
        assertArrayEquals(new short[] { 1, 2 }, Stream.of(1, 2).mapToShort(Integer::shortValue).toArray());
    }

    @Test
    public void testMapToInt() {
        assertArrayEquals(new int[] { 1, 2 }, Stream.of("1", "2").mapToInt(Integer::parseInt).toArray());
    }

    @Test
    public void testMapToLong() {
        assertArrayEquals(new long[] { 1L, 2L }, Stream.of("1", "2").mapToLong(Long::parseLong).toArray());
    }

    @Test
    public void testMapToFloat() {
        assertArrayEquals(new float[] { 1.0f, 2.0f }, Stream.of("1.0", "2.0").mapToFloat(Float::parseFloat).toArray(), 0.01f);
    }

    @Test
    public void testMapToDouble() {
        assertArrayEquals(new double[] { 1.0, 2.0 }, Stream.of("1.0", "2.0").mapToDouble(Double::parseDouble).toArray(), 0.01);
    }

    @Test
    public void testMapToEntryWithFunction() {
        List<Map.Entry<Integer, String>> result = Stream.of(1, 2).mapToEntry(x -> N.newEntry(x, "v" + x)).toList();
        assertEquals(2, result.size());
        assertEquals(N.newEntry(1, "v1"), result.get(0));
        assertEquals(N.newEntry(2, "v2"), result.get(1));
    }

    @Test
    public void testMapToEntryWithKeyAndValueMappers() {
        List<Map.Entry<Integer, String>> result = Stream.of(1, 2).mapToEntry(Fn.identity(), x -> "v" + x).toList();
        assertEquals(2, result.size());
        assertEquals(N.newEntry(1, "v1"), result.get(0));
        assertEquals(N.newEntry(2, "v2"), result.get(1));
    }

    @Test
    public void testFlatMap() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flatMap(Stream::of).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatmap() { // for Collection
        List<Integer> result = Stream.of(1, 2).flatmap(x -> Arrays.asList(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20), result);
    }

    @Test
    public void testFlattMapArray() {
        List<Integer> result = Stream.of("1,2", "3,4").flatmapToInt(s -> Stream.of(s.split(",")).mapToInt(Integer::parseInt).toArray()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlattmapJdkStream() {
        List<Integer> result = Stream.of(1, 2).flattMap(x -> java.util.stream.Stream.of(x, x + 1)).toList();
        assertEquals(Arrays.asList(1, 2, 2, 3), result);
    }

    @Test
    public void testFlatMapToChar() {
        char[] result = Stream.of("ab", "cd").flatMapToChar(s -> CharStream.of(s.toCharArray())).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatmapToCharFromArray() {
        char[] result = Stream.of("ab", "cd").flatmapToChar(s -> s.toCharArray()).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    // Similar flatMapToPrimitive tests for Byte, Short, Int, Long, Float, Double
    @Test
    public void testFlatMapToInt() {
        int[] result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flatMapToInt(list -> IntStream.of(list)).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatmapToIntFromArray() {
        int[] result = Stream.of(Pair.of(1, 2), Pair.of(3, 4)).flatmapToInt(p -> new int[] { p.left(), p.right() }).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToEntryFromStream() {
        List<Map.Entry<String, Integer>> result = Stream.of("a:1,b:2", "c:3")
                .flatMapToEntry(str -> Stream.of(str.split(",")).map(pairStr -> N.newEntry(pairStr.split(":")[0], Integer.parseInt(pairStr.split(":")[1]))))
                .toList();
        assertEquals(Arrays.asList(N.newEntry("a", 1), N.newEntry("b", 2), N.newEntry("c", 3)), result);

    }

    @Test
    public void testFlatmapToEntryFromMap() {
        Map<String, Integer> map1 = N.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = N.asMap("c", 3);
        List<Map.Entry<String, Integer>> result = Stream.of(map1, map2)
                .flatmapToEntry(Fn.identity())
                .sortedByKey(Comparator.naturalOrder()) // Sorting for predictable assertion
                .toList();
        assertEquals(Arrays.asList(N.newEntry("a", 1), N.newEntry("b", 2), N.newEntry("c", 3)), result);
    }

    @Test
    public void testFlattMapToEntryFromEntryStream() {
        List<Map.Entry<String, Integer>> result = Stream.of("a", "b")
                .flattMapToEntry(s -> EntryStream.of(s, s.hashCode(), s.toUpperCase(), s.toUpperCase().hashCode()))
                .toList();

        List<Map.Entry<String, Integer>> expected = new ArrayList<>();
        expected.add(N.newEntry("a", "a".hashCode()));
        expected.add(N.newEntry("A", "A".hashCode()));
        expected.add(N.newEntry("b", "b".hashCode()));
        expected.add(N.newEntry("B", "B".hashCode()));

        // Order might not be guaranteed, so check contents
        assertTrue(result.containsAll(expected) && expected.containsAll(result));
    }

    @Test
    public void testFlatmapIfNotNullSingleMapper() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4))
                .flatmapIfNotNull(c -> c) // c is already a Collection
                .toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatmapIfNotNullTwoMappers() {
        List<String> data = Arrays.asList("1,2", null, "3,4");
        List<Integer> result = Stream.of(data, null, Arrays.asList("5,6"))
                .flatmapIfNotNull(list -> list, // First mapper: pass through the non-null list
                        (String str) -> str == null ? null : Stream.of(str.split(",")).map(Integer::parseInt).toList() // Second mapper
                )
                .toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMapMulti() {
        List<String> result = Stream.of(1, 2, 3).mapMulti((Integer num, Consumer<String> downstream) -> {
            downstream.accept("N:" + num);
            if (num % 2 == 0) {
                downstream.accept("Even:" + num);
            }
        }).toList();
        assertEquals(Arrays.asList("N:1", "N:2", "Even:2", "N:3"), result);
    }

    @Test
    public void testMapMultiToInt() {
        int[] result = Stream.of("1", "2", "3").mapMultiToInt((String str, IntConsumer downstream) -> {
            downstream.accept(Integer.parseInt(str));
            if (Integer.parseInt(str) > 1) {
                downstream.accept(Integer.parseInt(str) * 10);
            }
        }).toArray();
        assertArrayEquals(new int[] { 1, 2, 20, 3, 30 }, result);
    }
    // Similar tests for mapMultiToLong, mapMultiToDouble

    @Test
    public void testMapPartial() {
        List<Integer> result = Stream.of("1", "two", "3", "four").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void testMapPartialToInt() {
        int[] result = Stream.of("1", "two", "3", "four").mapPartialToInt(s -> {
            try {
                return OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        }).toArray();
        assertArrayEquals(new int[] { 1, 3 }, result);
    }
    // Similar tests for mapPartialToLong, mapPartialToDouble, and Jdk versions

    @Test
    public void testGroupBySimple() {
        Stream<Map.Entry<Integer, List<String>>> resultStream = Stream.of("apple", "banana", "apricot", "blueberry", "cherry").groupBy(String::length);

        Map<Integer, List<String>> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Set.of(5, 6, 7, 9), resultMap.keySet()); // blueberry is 9
        assertTrue(resultMap.get(5).containsAll(Arrays.asList("apple")));
        assertTrue(resultMap.get(6).containsAll(Arrays.asList("banana", "cherry")));
        assertTrue(resultMap.get(7).containsAll(Arrays.asList("apricot")));
        assertTrue(resultMap.get(9).containsAll(Arrays.asList("blueberry")));
    }

    @Test
    public void testGroupByWithValueMapper() {
        Stream<Map.Entry<Integer, List<String>>> resultStream = Stream.of("apple", "banana", "apricot").groupBy(String::length, String::toUpperCase);

        Map<Integer, List<String>> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Set.of(5, 6, 7), resultMap.keySet());
        assertEquals(Arrays.asList("APPLE"), resultMap.get(5));
        assertEquals(Arrays.asList("BANANA"), resultMap.get(6));
        assertEquals(Arrays.asList("APRICOT"), resultMap.get(7));
    }

    @Test
    public void testGroupByWithCollector() {
        Stream<Map.Entry<Integer, Long>> resultStream = Stream.of("a", "bb", "ccc", "dd", "e").groupBy(String::length, Collectors.counting()); // Standard Java Collectors.counting() returns Long
        Map<Integer, Long> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2L, resultMap.get(1)); // a, e
        assertEquals(2L, resultMap.get(2)); // bb, dd
        assertEquals(1L, resultMap.get(3)); // ccc
    }

    @Test
    public void testGroupByWithKeyMapperValueMapperAndMergeFunction() {
        Map<Character, String> result = Stream.of("apple", "apricot", "banana", "blueberry", "avocado")
                .groupBy(s -> s.charAt(0), // keyMapper
                        s -> s.substring(0, Math.min(s.length(), 3)), // valueMapper (first 3 chars)
                        (v1, v2) -> v1 + ";" + v2) // mergeFunction
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals("app;apr;avo", result.get('a'));
        assertEquals("ban;blu", result.get('b'));
    }

    @Test
    public void testGroupByToEntrySimple() {
        EntryStream<Integer, List<String>> resultStream = Stream.of("apple", "banana", "apricot", "blueberry", "cherry").groupByToEntry(String::length);

        Map<Integer, List<String>> resultMap = resultStream.toMap();
        assertEquals(Set.of(5, 6, 7, 9), resultMap.keySet());
        assertTrue(resultMap.get(5).containsAll(Arrays.asList("apple")));
    }

    @Test
    public void testGroupByToEntryWithKeyMapperValueMapperCollectorAndMapFactory() {
        Map<Character, String> result = Stream.of("apple", "apricot", "banana", "blueberry", "avocado")
                .groupByToEntry(s -> s.charAt(0), // keyMapper
                        s -> String.valueOf(s.length()), // valueMapper (to Integer)
                        Collectors.joining(",", "[", "]"), // downstream Collector (String)
                        TreeMap::new // mapFactory
                )
                .toMap();

        // assertTrue(result instanceof TreeMap);
        assertEquals("[5,7,7]", result.get('a')); // lengths of "apple", "apricot", "avocado"
        assertEquals("[6,9]", result.get('b')); // lengths of "banana", "blueberry"
    }

    @Test
    public void testPartitionBySimple() {
        Stream<Map.Entry<Boolean, List<Integer>>> resultStream = Stream.of(1, 2, 3, 4, 5).partitionBy(x -> x % 2 == 0);
        Map<Boolean, List<Integer>> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList(2, 4), resultMap.get(true));
        assertEquals(Arrays.asList(1, 3, 5), resultMap.get(false));
    }

    @Test
    public void testPartitionByWithCollector() {
        Stream<Map.Entry<Boolean, Long>> resultStream = Stream.of(1, 2, 3, 4, 5, 6).partitionBy(x -> x % 2 == 0, Collectors.counting()); // Standard Java Collectors.counting() returns Long
        Map<Boolean, Long> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(3L, resultMap.get(true)); // 2, 4, 6
        assertEquals(3L, resultMap.get(false)); // 1, 3, 5
    }

    @Test
    public void testPartitionByToEntrySimple() {
        EntryStream<Boolean, List<Integer>> resultStream = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(x -> x % 2 == 0);
        Map<Boolean, List<Integer>> resultMap = resultStream.toMap();

        assertEquals(Arrays.asList(2, 4), resultMap.get(true));
        assertEquals(Arrays.asList(1, 3, 5), resultMap.get(false));
    }

    @Test
    public void testCountBy() {
        Map<Integer, Integer> counts = Stream.of("apple", "banana", "apricot", "apple").countBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(2, counts.get(5)); // apple, apple
        assertEquals(1, counts.get(6)); // banana
        assertEquals(1, counts.get(7)); // apricot
    }

    @Test
    public void testCountByToEntry() {
        Map<Integer, Integer> counts = Stream.of("apple", "banana", "apricot", "apple").countByToEntry(String::length).toMap();
        assertEquals(2, counts.get(5));
        assertEquals(1, counts.get(6));
        assertEquals(1, counts.get(7));
    }

    @Test
    public void testCollapseBiPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 2, 3, 3, 3, 4).collapse((prev, curr) -> prev.equals(curr)).toList();
        assertEquals(Arrays.asList(Collections.singletonList(1), Arrays.asList(2, 2), Arrays.asList(3, 3, 3), Collections.singletonList(4)), result);

        result = Stream.<Integer> empty().collapse((p, c) -> p.equals(c)).toList();
        assertTrue(result.isEmpty());

        result = Stream.of(1).collapse((p, c) -> p.equals(c)).toList();
        assertEquals(Arrays.asList(Collections.singletonList(1)), result);
    }

    @Test
    public void testCollapseBiPredicateAndMerger() {
        List<Integer> result = Stream.of(1, 1, 2, 3, 3, 1, 2) // Modified from original for clarity
                .collapse((p, c) -> p.equals(c), (r, c) -> r + c)
                .toList();
        assertEquals(Arrays.asList(2, 2, 6, 1, 2), result); // (1+1), 2, (3+3), 1, 2
    }

    @Test
    public void testCollapseWithCollector() {
        List<Long> result = Stream.of(1, 2, 2, 3, 3, 3, 4)
                .collapse((prev, curr) -> prev.equals(curr), Collectors.counting()) // Standard Java Collectors.counting() returns Long
                .toList();
        assertEquals(Arrays.asList(1L, 2L, 3L, 1L), result);
    }

    @Test
    public void testCollapseTriPredicate() {
        // Example: collapse if current number is within +/- 1 of the first number in the current series,
        // and also within +/-1 of the previous number.
        // Series: [10, 11, 12], [5, 4], [20]
        List<List<Integer>> result = Stream.of(10, 11, 12, 9, 5, 4, 6, 20)
                .collapse((first, prev, curr) -> Math.abs(curr - first) <= 2 && Math.abs(curr - prev) <= 1)
                .toList();

        // 10 -> start [10]
        // 11 -> abs(11-10)<=2 (T), abs(11-10)<=1 (T) -> [10, 11]
        // 12 -> abs(12-10)<=2 (T), abs(12-11)<=1 (T) -> [10, 11, 12]
        // 9  -> abs(9-10)<=2 (T),  abs(9-12)<=1 (F)  -> New group [9]
        // 5  -> abs(5-9)<=2 (F)                   -> New group [5]
        // 4  -> abs(4-5)<=2 (T),  abs(4-5)<=1 (T)  -> [5,4]
        // 6  -> abs(6-5)<=2 (T),  abs(6-4)<=1 (F)  -> New group [6]
        // 20 -> abs(20-6)<=2 (F)                  -> New group [20]

        assertEquals(Arrays.asList(Arrays.asList(10, 11, 12), Collections.singletonList(9), Arrays.asList(5, 4), Collections.singletonList(6),
                Collections.singletonList(20)), result);
    }

    @Test
    public void testScanSimple() {
        List<Integer> result = Stream.of(1, 2, 3, 4).scan((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(1, 3, 6, 10), result);
        assertTrue(Stream.<Integer> empty().scan((a, b) -> a + b).toList().isEmpty());
        assertEquals(Arrays.asList(1), Stream.of(1).scan((a, b) -> a + b).toList());
    }

    @Test
    public void testScanWithInitialValue() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result); // 10+1, 11+2, 13+3
        assertTrue(Stream.<Integer> empty().scan(10, (a, b) -> a + b).toList().isEmpty());
    }

    @Test
    public void testScanWithInitialValueAndInclusion() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10, 11, 13, 16), result);

        result = Stream.of(1, 2, 3).scan(10, false, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result);

        result = Stream.<Integer> empty().scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10), result); // Initial value included if stream empty and initIncluded=true

        result = Stream.<Integer> empty().scan(10, false, (a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitByChunkSize() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).split(2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5)), result);
        assertTrue(Stream.empty().split(2).toList().isEmpty());
        assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(1).split(2).toList());
    }

    @Test
    public void testSplitByChunkSizeAndSupplier() {
        List<Set<Integer>> result = Stream.of(1, 2, 3, 4, 5).split(2, IntFunctions.ofLinkedHashSet()).toList();
        assertEquals(Arrays.asList(N.asLinkedHashSet(1, 2), N.asLinkedHashSet(3, 4), N.asLinkedHashSet(5)), result);
    }

    @Test
    public void testSplitByChunkSizeAndCollector() {
        List<Long> result = Stream.of(1, 2, 3, 4, 5)
                .split(2, Collectors.summingInt(x -> x)) // Standard Java Collectors.summingInt
                .map(Integer::longValue)
                .toList();
        assertEquals(Arrays.asList(3L, 7L, 5L), result); // (1+2), (3+4), 5
    }

    @Test
    public void testSplitByPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 0, 3, 4, 0, 5)
                .split(x -> x == 0) // Splits *before* element satisfying predicate
                .toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Collections.singletonList(0), // Element matching predicate starts a new group
                Arrays.asList(3, 4), Collections.singletonList(0), Collections.singletonList(5)), result);

        result = Stream.of(0, 1, 2, 0, 3, 4, 0).split(x -> x == 0).toList();
        assertEquals(Arrays.asList(Collections.singletonList(0), Arrays.asList(1, 2), Collections.singletonList(0), Arrays.asList(3, 4),
                Collections.singletonList(0)), result);
    }

    @Test
    public void testSplitAtPosition() {
        List<Stream<Integer>> resultStreams = Stream.of(1, 2, 3, 4, 5).splitAt(2).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertEquals(Arrays.asList(3, 4, 5), resultStreams.get(1).toList());

        resultStreams = Stream.of(1, 2).splitAt(2).toList(); // Split at end
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertTrue(resultStreams.get(1).toList().isEmpty());

        resultStreams = Stream.of(1, 2).splitAt(0).toList(); // Split at start
        assertEquals(2, resultStreams.size());
        assertTrue(resultStreams.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(1).toList());
    }

    @Test
    public void testSplitAtPositionWithCollector() {
        List<Long> result = Stream.of(1, 2, 3, 4, 5)
                .splitAt(2, Collectors.summingInt(ToIntFunction.UNBOX)) // sums first part
                .map(Integer::longValue)
                .toList();
        assertEquals(N.asList(3L, 12L), result); // Sum of [1,2] is 3. Second part is discarded.
    }

    @Test
    public void testSplitAtPredicate() {
        List<Stream<Integer>> resultStreams = Stream.of(1, 2, 99, 4, 5).splitAt(x -> x == 99).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertEquals(Arrays.asList(99, 4, 5), resultStreams.get(1).toList()); // 99 is first of second stream

        resultStreams = Stream.of(99, 1, 2).splitAt(x -> x == 99).toList();
        assertEquals(2, resultStreams.size());
        assertTrue(resultStreams.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(99, 1, 2), resultStreams.get(1).toList());

        resultStreams = Stream.of(1, 2, 3).splitAt(x -> x == 99).toList(); // Predicate never met
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2, 3), resultStreams.get(0).toList());
        assertTrue(resultStreams.get(1).toList().isEmpty());
    }

    @Test
    public void testSlidingWindowSize() throws Exception {
        {
            assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(1).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2)), Stream.of(1, 2).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 3)), Stream.of(1, 2, 3).sliding(2).toList());

            assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(N.asList(1).iterator()).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2)), Stream.of(N.asList(1, 2).iterator()).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 3)), Stream.of(N.asList(1, 2, 3).iterator()).sliding(2).toList());
        }
        {
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 4).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(1, 2, 3, 4, 5).sliding(3, 5).toList());

            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)),
                    Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)), Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5)), Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(5)), Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 4).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 5).toList());

        }

        {
            assertEquals(0, Stream.of(1).sliding(2).skip(1).count());
            assertEquals(0, Stream.of(1, 2).sliding(2).skip(1).count());
            assertEquals(1, Stream.of(1, 2, 3).sliding(2).skip(1).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5)).sliding(3).skip(2).count()); // [1,2,3], [2,3,4], [3,4,5] - skip first two);

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 2).skip(1).count());
            assertEquals(0, Stream.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 2).skip(2).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 3).skip(1).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 4).skip(1).count());

            assertEquals(0, Stream.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Stream.of(N.asList(1).iterator()).sliding(2).skip(1).count());
            assertEquals(0, Stream.of(N.asList(1, 2).iterator()).sliding(2).skip(1).count());
            assertEquals(1, Stream.of(N.asList(1, 2, 3).iterator()).sliding(2).skip(1).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3).skip(2).count()); // [1,2,3], [2,3,4], [3,4,5] - skip first two);

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).skip(1).count());
            assertEquals(0, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).skip(2).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 3).skip(1).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 4).skip(1).count());

            assertEquals(0, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Stream.of(N.asList(1).iterator()).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(0, Stream.of(N.asList(1, 2).iterator()).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(1, Stream.of(N.asList(1, 2, 3).iterator()).sliding(2, Collectors.toList()).skip(1).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, Collectors.toList()).skip(2).count()); // [1,2,3], [2,3,4], [3,4,5] - skip first two);

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 2, Collectors.toList()).skip(1).count());
            assertEquals(0, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 2, Collectors.toList()).skip(2).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 3, Collectors.toList()).skip(1).count());

            assertEquals(1, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 4, Collectors.toList()).skip(1).count());

            assertEquals(0, Stream.of(N.asList(1, 2, 3, 4, 5).iterator()).sliding(3, 5, Collectors.toList()).skip(1).count());

        }

        {
            assertEquals(0, Seq.of(1).sliding(2).skip(1).count());
            assertEquals(0, Seq.of(1, 2).sliding(2).skip(1).count());
            assertEquals(1, Seq.of(1, 2, 3).sliding(2).skip(1).count());

            assertEquals(1, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3).skip(2).count()); // [1,2,3], [2,3,4], [3,4,5] - skip first two);

            assertEquals(1, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 2).skip(1).count());
            assertEquals(0, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 2).skip(2).count());

            assertEquals(1, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 3).skip(1).count());

            assertEquals(1, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 4).skip(1).count());

            assertEquals(0, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Seq.of(1).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(0, Seq.of(1, 2).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(1, Seq.of(1, 2, 3).sliding(2, Collectors.toList()).skip(1).count());

            assertEquals(1, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, Collectors.toList()).skip(2).count()); // [1,2,3], [2,3,4], [3,4,5] - skip first two);

            assertEquals(1, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 2, Collectors.toList()).skip(1).count());
            assertEquals(0, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 2, Collectors.toList()).skip(2).count());

            assertEquals(1, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 3, Collectors.toList()).skip(1).count());

            assertEquals(1, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 4, Collectors.toList()).skip(1).count());

            assertEquals(0, Seq.of(N.asList(1, 2, 3, 4, 5)).sliding(3, 5, Collectors.toList()).skip(1).count());

        }
    }

    @Test
    public void testSlidingWindowSizeAndIncrement() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5, 6).sliding(3, 2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5), Arrays.asList(5, 6)), result); // Next would be [5,6,?] - too short
    }

    @Test
    public void testIntersperse() {
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), Stream.of(1, 2, 3).intersperse(0).toList());
        assertEquals(Arrays.asList(1), Stream.of(1).intersperse(0).toList());
        assertTrue(Stream.empty().intersperse(0).toList().isEmpty());
    }

    @Test
    public void testDistinctWithMerge() {
        // Key collision, merge by summing
        List<Pair<String, Integer>> data = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("a", 3));
        List<Pair<String, Integer>> result = Stream.of(data)
                .distinctBy(Pair::left, (p1, p2) -> Pair.of(p1.left(), p1.right() + p2.right()))
                .sorted(Comparator.comparing(Pair::left)) // For stable assertion order
                .toList();

        assertEquals(Arrays.asList(Pair.of("a", 4), Pair.of("b", 2)), result);
    }

    @Test
    public void testDistinctByFunction() {
        List<String> result = Stream.of("apple", "apricot", "banana", "blueberry", "Apple")
                .distinctBy(s -> s.toLowerCase().charAt(0)) // Distinct by first char, case-insensitive
                .toList();
        // Expect one 'a' word (apple or apricot or Apple), one 'b' word. Order depends on original.
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.toLowerCase().startsWith("a")));
        assertTrue(result.stream().anyMatch(s -> s.toLowerCase().startsWith("b")));
    }

    @Test
    public void testSorted() {
        assertEquals(Arrays.asList(1, 2, 3, 4), Stream.of(4, 1, 3, 2).sorted().toList());
        assertEquals(Arrays.asList(4, 3, 2, 1), Stream.of(4, 1, 3, 2).sorted(Comparator.reverseOrder()).toList());
    }

    @Test
    public void testSortedBy() {
        List<String> words = Arrays.asList("banana", "apple", "cherry");
        assertEquals(Arrays.asList("apple", "banana", "cherry"), Stream.of(words).sortedBy(Fn.identity()).toList());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), Stream.of(words).sortedBy(String::length).toList()); // apple, cherry (5), banana (6)
    }
    // Similar for sortedByInt, Long, Double and reverse versions

    @Test
    public void testTopN() {
        assertEquals(Arrays.asList(4, 5), Stream.of(1, 5, 2, 4, 3).top(2).toList()); // Natural order, so largest
        assertEquals(Arrays.asList(4, 5), Stream.of(1, 5, 2, 4, 3).top(2, Comparator.naturalOrder()).toList());
        assertEquals(Arrays.asList(2, 1), Stream.of(1, 5, 2, 4, 3).top(2, Comparator.reverseOrder()).toList());
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).top(5).toList());
    }

    @Test
    public void testSkipRange() {
        assertEquals(Arrays.asList(1, 5), Stream.of(1, 2, 3, 4, 5).skipRange(1, 4).toList()); // Skips index 1,2,3 (elements 2,3,4)
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).skipRange(5, 5).toList()); // Empty range
        assertEquals(Collections.emptyList(), Stream.of(1, 2, 3, 4, 5).skipRange(0, 5).toList());
    }

    @Test
    public void testSkipNulls() {
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, null, 2, null, 3).skipNulls().toList());
    }

    @Test
    public void testSkipLast() {
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).skipLast(2).toList());
        assertTrue(Stream.of(1, 2).skipLast(3).toList().isEmpty());
        assertTrue(Stream.empty().skipLast(2).toList().isEmpty());
    }

    @Test
    public void testTakeLast() {
        assertEquals(Arrays.asList(4, 5), Stream.of(1, 2, 3, 4, 5).takeLast(2).toList());
        assertEquals(Arrays.asList(1, 2), Stream.of(1, 2).takeLast(3).toList());
        assertTrue(Stream.empty().takeLast(2).toList().isEmpty());
    }

    @Test
    public void testOnFirst() {
        AtomicReference<Integer> first = new AtomicReference<>();
        Stream.of(1, 2, 3).onFirst(first::set).toList();
        assertEquals(1, first.get());

        first.set(null);
        Stream.<Integer> empty().onFirst(first::set).toList();
        assertNull(first.get());
    }

    @Test
    public void testOnLast() {
        AtomicReference<Integer> last = new AtomicReference<>();
        Stream.of(1, 2, 3).onLast(last::set).toList();
        assertEquals(3, last.get());

        last.set(null);
        Stream.<Integer> empty().onLast(last::set).toList();
        assertNull(last.get());
    }

    @Test
    public void testPeekIf() {
        List<Integer> peeked = new ArrayList<>();
        Stream.of(1, 2, 3, 4).peekIf(x -> x % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(2, 4), peeked);
    }

    @Test
    public void testPeekIfWithBiPredicate() {
        List<Integer> peeked = new ArrayList<>();
        // Peek if element is even AND index (1-based) is > 1
        Stream.of(1, 2, 3, 4, 5, 6).peekIf((val, idx) -> val % 2 == 0 && idx > 1, peeked::add).toList();
        assertEquals(Arrays.asList(2, 4, 6), peeked);
    }

    @Test
    public void testForEachThrowing() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachIndexed() {
        Map<Integer, String> map = new HashMap<>();
        Stream.of("a", "b").forEachIndexed((idx, val) -> map.put(idx, val));
        assertEquals("a", map.get(0));
        assertEquals("b", map.get(1));
    }

    @Test
    public void testForEachUntilBiConsumer() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5).forEachUntil((val, flag) -> {
            collected.add(val);
            if (val == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachUntilMutableBoolean() {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        Stream.of(1, 2, 3, 4, 5).forEachUntil(flag, val -> {
            collected.add(val);
            if (val == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(Stream.of(1, 2, 3).anyMatch(x -> x == 2));
        assertFalse(Stream.of(1, 2, 3).anyMatch(x -> x == 4));
    }

    @Test
    public void testAllMatch() {
        assertTrue(Stream.of(2, 4, 6).allMatch(x -> x % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).allMatch(x -> x % 2 == 0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(Stream.of(1, 3, 5).noneMatch(x -> x % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).noneMatch(x -> x % 2 == 0));
    }

    @Test
    public void testNMatch() {
        // At least 2, at most 3 even numbers
        assertTrue(Stream.of(1, 2, 3, 4, 5, 6).nMatch(2, 3, x -> x % 2 == 0)); // 2,4,6 are even (3 matches)
        assertFalse(Stream.of(1, 2, 3, 4, 5).nMatch(3, 3, x -> x % 2 == 0)); // 2,4 are even (2 matches) -> should be false
        assertTrue(Stream.of(1, 2, 3, 4, 5).nMatch(2, 2, x -> x % 2 == 0));

        assertFalse(Stream.of(1, 2, 3).nMatch(2, 3, x -> x % 2 == 0)); // only 1 even
        assertFalse(Stream.of(2, 4, 6, 8).nMatch(2, 3, x -> x % 2 == 0)); // 4 even numbers, limit is 3 for atMost, count of 3 is <= atMost.
    }

    @Test
    public void testFindFirstPredicate() {
        assertEquals(Optional.of(2), Stream.of(1, 2, 3, 4).findFirst(x -> x % 2 == 0));
        assertEquals(Optional.empty(), Stream.of(1, 3, 5).findFirst(x -> x % 2 == 0));
    }

    @Test
    public void testFindAnyPredicate() {
        // For sequential streams, findAny typically returns the first.
        assertEquals(Optional.of(2), Stream.of(1, 2, 3, 4).findAny(x -> x % 2 == 0));
        // For parallel, it could be any element that matches.
        Optional<Integer> result = Stream.of(1, 2, 3, 4).parallel().findAny(x -> x % 2 == 0);
        assertTrue(result.isPresent() && result.get() % 2 == 0);
    }

    @Test
    public void testFindLastPredicate() {
        assertEquals(Optional.of(4), Stream.of(1, 2, 3, 4).findLast(x -> x % 2 == 0));
        assertEquals(Optional.empty(), Stream.of(1, 3, 5).findLast(x -> x % 2 == 0));
        assertEquals(Optional.of(2), Stream.of(2).findLast(x -> x % 2 == 0));
    }

    @Test
    public void testContainsAllArray() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(2, 4));
        assertFalse(Stream.of(1, 2, 3).containsAll(2, 5));
        assertTrue(Stream.of(1, 2, 3).containsAll()); // Empty array
    }

    @Test
    public void testContainsAllCollection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 4)));
        assertFalse(Stream.of(1, 2, 3).containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testContainsAnyArray() {
        assertTrue(Stream.of(1, 2, 3).containsAny(3, 5, 6));
        assertFalse(Stream.of(1, 2, 3).containsAny(4, 5, 6));
    }

    @Test
    public void testContainsNoneArray() {
        assertTrue(Stream.of(1, 2, 3).containsNone(4, 5, 6));
        assertFalse(Stream.of(1, 2, 3).containsNone(3, 5, 6));
    }

    @Test
    public void testToArrayGenerator() {
        String[] result = Stream.of("a", "b").toArray(String[]::new);
        assertArrayEquals(new String[] { "a", "b" }, result);
    }

    @Test
    public void testToImmutableMap() {
        ImmutableMap<Integer, String> map = Stream.of("a", "bb", "ccc").toImmutableMap(String::length, Fn.identity());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        assertEquals("ccc", map.get(3));

        assertThrows(IllegalStateException.class, () -> Stream.of("a", "b").toImmutableMap(s -> 1, Fn.identity()));
    }

    @Test
    public void testToImmutableMapWithMerge() {
        ImmutableMap<Integer, String> map = Stream.of("a", "b", "cc").toImmutableMap(String::length, Fn.identity(), (s1, s2) -> s1 + ";" + s2);
        assertEquals("a;b", map.get(1));
        assertEquals("cc", map.get(2));
    }

    @Test
    public void testToMap() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc").toMap(String::length, Fn.identity());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        assertEquals("ccc", map.get(3));
    }

    @Test
    public void testToMapWithMerge() {
        Map<Integer, String> map = Stream.of("a", "b", "cc", "d").toMap(String::length, Fn.identity(), (s1, s2) -> s1 + ";" + s2);

        // Order of merge for HashMap is not guaranteed, check content
        Map<Integer, String> expected = N.asMap(1, "a;b;d", 2, "cc"); // Or "b;a;d" etc.
        assertEquals(expected.get(2), map.get(2));
        assertEquals(3, map.get(1).chars().filter(ch -> ch == ';').count() + 1); // 3 items merged
        assertTrue(map.get(1).contains("a") && map.get(1).contains("b") && map.get(1).contains("d"));
    }

    @Test
    public void testToMapWithFactory() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc").toMap(String::length, Fn.identity(), Suppliers.ofLinkedHashMap());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        Iterator<Map.Entry<Integer, String>> iter = map.entrySet().iterator();
        assertEquals(1, iter.next().getKey());
        assertEquals(2, iter.next().getKey());
    }

    @Test
    public void testGroupToKeyMapper() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "ddd").groupTo(String::length);
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb"), map.get(2));
        assertEquals(Arrays.asList("ddd"), map.get(3));
    }

    @Test
    public void testGroupToKeyAndValueMapper() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c").groupTo(String::length, String::toUpperCase);
        assertEquals(Arrays.asList("A", "C"), map.get(1));
        assertEquals(Arrays.asList("BB"), map.get(2));
    }

    @Test
    public void testGroupToWithDownstreamCollectorAndMapFactory() {
        TreeMap<Integer, Long> map = Stream.of("a", "bb", "c", "ddd", "ee").groupTo(String::length, Collectors.counting(), TreeMap::new); // Standard Java Collectors.counting()
        assertTrue(map instanceof TreeMap);
        assertEquals(2L, map.get(1)); // "a", "c"
        assertEquals(2L, map.get(2)); // "bb", "ee"
        assertEquals(1L, map.get(3)); // "ddd"
    }

    @Test
    public void testFlatGroupToKeyExtractor() {
        // Each string maps to a collection of its characters (as keys)
        Map<Character, List<String>> map = Stream.of("ant", "bat", "cat").flatGroupTo(s -> Stream.of(s.toCharArray()).map(c -> (Character) c).toList());
        assertEquals(Arrays.asList("ant", "bat", "cat"), map.get('a'));
        assertEquals(Arrays.asList("ant", "bat", "cat"), map.get('t'));
        assertEquals(Arrays.asList("bat"), map.get('b'));
        assertEquals(Arrays.asList("cat"), map.get('c'));
    }

    @Test
    public void testFlatGroupToWithDownstreamCollector() {
        Map<Character, Long> map = Stream.of("apple", "apricot", "banana")
                .flatGroupTo(s -> Stream.of(s.toCharArray()).map(c -> (Character) c).toList(), // flatKeyExtractor
                        Collectors.counting());

        assertEquals(Long.valueOf(5), map.get('a')); // from apple, apricot, banana
        assertEquals(Long.valueOf(3), map.get('p')); // from apple (2)
        assertEquals(Long.valueOf(1), map.get('l'));
        assertEquals(Long.valueOf(1), map.get('e'));
        // ... and so on for other characters
    }

    @Test
    public void testToMultimapKeyMapper() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb", "c", "dd").toMultimap(String::length);
        assertEquals(Arrays.asList("a", "c"), multimap.get(1));
        assertEquals(Arrays.asList("bb", "dd"), multimap.get(2));
    }

    @Test
    public void testToMultimapKeyAndValueMapperAndFactory() {
        ListMultimap<Character, Integer> multimap = Stream.of("apple", "apricot").toMultimap(s -> s.charAt(0), String::length, Suppliers.ofListMultimap());

        assertEquals(Arrays.asList(5, 7), multimap.get('a')); // lengths of apple, apricot
    }

    @Test
    public void testToDatasetSimple() {
        // Requires T to be Map or Bean. Let's use Maps.
        Map<String, Object> row1 = N.asMap("id", 1, "name", "Alice");
        Map<String, Object> row2 = N.asMap("id", 2, "name", "Bob");
        Dataset dataset = Stream.of(row1, row2).toDataset();

        assertEquals(2, dataset.size());
        assertTrue(dataset.columnNameList().containsAll(Arrays.asList("id", "name")));
    }

    @Test
    public void testToDatasetWithColumnNames() {
        List<String> columnNames = Arrays.asList("val1", "val2");
        // Assuming T can be converted to a list matching columnNames order
        List<Object> row1Data = Arrays.asList(10, "X");
        List<Object> row2Data = Arrays.asList(20, "Y");

        Dataset dataset = Stream.of(row1Data, row2Data).toDataset(columnNames);
        assertEquals(2, dataset.size());
        assertEquals(columnNames, dataset.columnNameList());
    }

    @Test
    public void testFoldLeft() {
        Optional<String> result = Stream.of("a", "b", "c").foldLeft((s1, s2) -> s1 + s2);
        assertEquals(Optional.of("abc"), result);
        assertEquals(Optional.empty(), Stream.<String> empty().foldLeft((s1, s2) -> s1 + s2));
    }

    @Test
    public void testFoldLeftWithIdentity() {
        String result = Stream.of("a", "b", "c").foldLeft("x", (s1, s2) -> s1 + s2);
        assertEquals("xabc", result);
        assertEquals("x", Stream.<String> empty().foldLeft("x", (s1, s2) -> s1 + s2));
    }

    @Test
    public void testFoldRight() {
        // For (x,y) -> x - y. List [1,2,3]
        // Abacus foldRight should be (a op (b op c)) style where op is (prev_result, current_from_right)
        // which means, the elements are processed effectively right-to-left.
        // (1, (2, 3)) -> (1, (2-3)) -> (1, -1) -> 1 - (-1) = 2.
        Optional<Integer> resultInt = Stream.of(1, 2, 3).foldRight((acc, curr) -> acc - curr);
        assertEquals(Optional.of(0), resultInt);

        // (a, (b, c)) for (x,y) -> x + "/" + y
        // (a, (b + "/" + c)) -> a + "/" + (b + "/" + c) -> "a/b/c"
        // This is how StreamEx defines it. Abacus might collect, reverse, then foldLeft.
        // If reversed and foldLeft: (c,b,a) -> ((c/b)/a)
        // Let's check Abacus actual behavior if it buffers and reverses for foldRight.
        // If it does, for ("a","b","c") with (x,y) -> x + "-" + y:
        // reversed: ["c", "b", "a"]. foldLeft: "c", then "c-b", then "c-b-a".
        Optional<String> resultStr = Stream.of("a", "b", "c").foldRight((s1, s2) -> s1 + "-" + s2);
        assertEquals(Optional.of("c-b-a"), resultStr); // This indicates collect, reverse, foldLeft
    }

    @Test
    public void testFoldRightWithIdentity() {
        // Identity "x", elements ["a","b","c"], op (s1,s2) -> s1+s2
        // reversed: ["c","b","a"]. foldLeft with "x": x-> (x+c) -> (xc+b) -> (xcb+a) = "xcba"
        String result = Stream.of("a", "b", "c").foldRight("x", (s1, s2) -> s1 + s2);
        assertEquals("xcba", result);
        assertEquals("x", Stream.<String> empty().foldRight("x", (s1, s2) -> s1 + s2));
    }

    @Test
    public void testReduceBinaryOperator() {
        assertEquals(Optional.of(6), Stream.of(1, 2, 3).reduce((a, b) -> a + b));
        assertEquals(Optional.empty(), Stream.<Integer> empty().reduce((a, b) -> a + b));
    }

    @Test
    public void testReduceIdentityBinaryOperator() {
        assertEquals(10, (int) Stream.of(1, 2, 3).reduce(4, (a, b) -> a + b)); // 4+1+2+3
        assertEquals(4, (int) Stream.<Integer> empty().reduce(4, (a, b) -> a + b));
    }

    @Test
    public void testReduceIdentityAccumulatorCombiner() {
        int result = Stream.of("1", "2", "3").reduce(0, (sum, str) -> sum + Integer.parseInt(str), (s1, s2) -> s1 + s2);
        assertEquals(6, result);

        // Test with parallel stream
        result = Stream.of("1", "2", "3", "4", "5", "6").parallel().reduce(0, (sum, str) -> sum + Integer.parseInt(str), Integer::sum);
        assertEquals(21, result);
    }

    @Test
    public void testReduceUntilAccumulatorAndCondition() {
        // Reduce sum until sum > 5
        Optional<Integer> result = Stream.of(1, 2, 3, 4, 5).reduceUntil((a, b) -> a + b, sum -> sum > 5);
        assertEquals(Optional.of(6), result); // 1+2+3 = 6. 6 > 5 is true, loop breaks.

        Optional<Integer> result2 = Stream.of(1, 2, 3).reduceUntil((a, b) -> a + b, sum -> sum > 10);
        assertEquals(Optional.of(6), result2); // 1+2+3=6. 6>10 is false. Stream ends.
    }

    @Test
    public void testReduceUntilAccumulatorAndBiCondition() {
        // Reduce sum until current accumulated value (acc) + the next element (next) > 5
        Optional<Integer> result = Stream.of(1, 2, 3, 4, 5).reduceUntil((acc, next) -> acc + next, (acc, next) -> (acc + next) > 5);
        // 1. acc=1, next=2 -> 1+2=3. (3 > 5) is false. current_sum = 3
        // 2. acc=3, next=3 -> 3+3=6. (6 > 5) is true. loop breaks. result is 6.
        assertEquals(Optional.of(6), result);
    }

    @Test
    public void testCollectSupplierAccumulatorCombiner() {
        ArrayList<Integer> result = Stream.of(1, 2, 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollectSupplierAccumulator() {
        List<Integer> result = Stream.of(1, 2, 3).collect(ArrayList::new, ArrayList::add);
        assertEquals(Arrays.asList(1, 2, 3), result);

        Set<Integer> parallelResult = Stream.of(1, 2, 3, 1, 2).parallel().collect(HashSet::new, HashSet::add);
        assertEquals(Set.of(1, 2, 3), parallelResult);
    }

    @Test
    public void testCollectCollector() {
        List<Integer> result = Stream.of(1, 2, 3).collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollectThenApply() {
        String result = Stream.of(1, 2, 3)
                .collectThenApply(Collectors.toList(), list -> list.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
        assertEquals("1,2,3", result);
    }

    @Test
    public void testCollectThenAccept() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3).collectThenAccept(Collectors.toList(), collected::addAll);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testToListThenApply() {
        String result = Stream.of(1, 2, 3).toListThenApply(list -> String.join(",", N.map(list, String::valueOf)));
        assertEquals("1,2,3", result);
    }

    @Test
    public void testMinComparator() {
        assertEquals(Optional.of(1), Stream.of(5, 1, 3).min(Comparator.naturalOrder()));
        assertEquals(Optional.empty(), Stream.<Integer> empty().min(Comparator.naturalOrder()));
    }

    @Test
    public void testMinBy() {
        assertEquals(Optional.of("a"), Stream.of("bb", "a", "ccc").minBy(String::length));
    }

    @Test
    public void testMinAll() {
        assertEquals(Arrays.asList(1, 1), Stream.of(5, 1, 3, 1).minAll(Comparator.naturalOrder()));
        assertEquals(Arrays.asList(1, 1), Stream.of(5, 1, 3, 1).sorted().minAll(Comparator.naturalOrder())); // ensure sorted to get consistent multiple min
    }

    @Test
    public void testMaxComparator() {
        assertEquals(Optional.of(5), Stream.of(5, 1, 3).max(Comparator.naturalOrder()));
    }

    @Test
    public void testMaxBy() {
        assertEquals(Optional.of("ccc"), Stream.of("bb", "a", "ccc").maxBy(String::length));
    }

    @Test
    public void testMaxAll() {
        assertEquals(Arrays.asList(5, 5), Stream.of(5, 1, 3, 5).maxAll(Comparator.naturalOrder()));
        assertEquals(Arrays.asList(5, 5), Stream.of(5, 1, 3, 5).sorted(Comparator.reverseOrder()).maxAll(Comparator.naturalOrder()));
    }

    @Test
    public void testSumInt() {
        assertEquals(6, Stream.of("1", "2", "3").sumInt(Integer::parseInt));
    }

    @Test
    public void testAverageDouble() {
        assertEquals(OptionalDouble.of(2.0), Stream.of(1, 2, 3).averageDouble(Integer::doubleValue));
        assertEquals(OptionalDouble.empty(), Stream.<Integer> empty().averageDouble(Integer::doubleValue));
    }
    // ... similar for sumLong, sumDouble, averageInt, averageLong ...

    @Test
    public void testKthLargest() {
        assertEquals(Optional.of(4), Stream.of(1, 5, 2, 4, 3).kthLargest(2, Comparator.naturalOrder())); // 2nd largest is 4
        assertEquals(Optional.of(5), Stream.of(1, 5, 2, 4, 3).kthLargest(1, Comparator.naturalOrder())); // 1st largest is 5
        assertEquals(Optional.of(1), Stream.of(1, 5, 2, 4, 3).kthLargest(5, Comparator.naturalOrder())); // 5th largest is 1
        assertEquals(Optional.empty(), Stream.of(1, 2).kthLargest(3, Comparator.naturalOrder()));
    }

    @Test
    public void testHasDuplicates() {
        assertTrue(Stream.of(1, 2, 2, 3).hasDuplicates());
        assertFalse(Stream.of(1, 2, 3, 4).hasDuplicates());
        assertFalse(Stream.empty().hasDuplicates());
    }

    @Test
    public void testCombinationsNoLength() {
        List<List<Integer>> combs = Stream.of(1, 2).combinations().toList();
        // Expected: [], [1], [2], [1,2]
        assertEquals(4, combs.size());
        assertTrue(combs.contains(Collections.emptyList()));
        assertTrue(combs.contains(Arrays.asList(1)));
        assertTrue(combs.contains(Arrays.asList(2)));
        assertTrue(combs.contains(Arrays.asList(1, 2)));
    }

    @Test
    public void testCombinationsWithLength() {
        List<List<Integer>> combs = Stream.of(1, 2, 3).combinations(2).toList();
        // Expected: [1,2], [1,3], [2,3]
        assertEquals(3, combs.size());
        assertTrue(combs.contains(Arrays.asList(1, 2)));
        assertTrue(combs.contains(Arrays.asList(1, 3)));
        assertTrue(combs.contains(Arrays.asList(2, 3)));
    }

    @Test
    public void testCombinationsWithLengthAndRepeat() {
        List<List<Integer>> combsWithoutRepeat = Stream.of(1, 2).combinations(2, false).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2)), combsWithoutRepeat);

        List<List<Integer>> combsWithRepeat = Stream.of(1, 2).combinations(2, true).toList();
        assertEquals(4, combsWithRepeat.size());
        assertTrue(combsWithRepeat.contains(Arrays.asList(1, 1)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(1, 2)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(2, 1)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(2, 2)));
    }

    @Test
    public void testPermutations() {
        List<List<Integer>> perms = Stream.of(1, 2).permutations().toList();
        // Expected: [1,2], [2,1]
        assertEquals(2, perms.size());
        assertTrue(perms.contains(Arrays.asList(1, 2)));
        assertTrue(perms.contains(Arrays.asList(2, 1)));
    }

    @Test
    public void testOrderedPermutations() {
        List<List<Integer>> perms = Stream.of(1, 3, 2).orderedPermutations().toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(1, 3, 2), Arrays.asList(2, 1, 3), Arrays.asList(2, 3, 1), Arrays.asList(3, 1, 2),
                Arrays.asList(3, 2, 1)), perms);
    }

    @Test
    public void testCartesianProductVarargs() {
        List<List<Integer>> result = Stream.of(0, 1).cartesianProduct(Arrays.asList(2, 3)).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 2), Arrays.asList(0, 3), Arrays.asList(1, 2), Arrays.asList(1, 3)), result);
    }

    @Test
    public void testCartesianProductCollectionOfCollections() {
        List<List<String>> result = Stream.of("a", "b").cartesianProduct(Arrays.asList(Arrays.asList("1", "2"))).toList();
        assertEquals(Arrays.asList(Arrays.asList("a", "1"), Arrays.asList("a", "2"), Arrays.asList("b", "1"), Arrays.asList("b", "2")), result);

        List<List<String>> result2 = Stream.of("a", "b").cartesianProduct(Arrays.asList(Arrays.asList("1", "2"), Arrays.asList("true", "false"))).toList();
        assertEquals(Arrays.asList(Arrays.asList("a", "1", "true"), Arrays.asList("a", "1", "false"), Arrays.asList("a", "2", "true"),
                Arrays.asList("a", "2", "false"), Arrays.asList("b", "1", "true"), Arrays.asList("b", "1", "false"), Arrays.asList("b", "2", "true"),
                Arrays.asList("b", "2", "false")), result2);
    }

    @Test
    public void testRollup() {
        List<List<Integer>> result = Stream.of(1, 2, 3).rollup().toList();
        assertEquals(Arrays.asList(Collections.emptyList(), Arrays.asList(1), Arrays.asList(1, 2), Arrays.asList(1, 2, 3)), result);
    }

    @Test
    public void testIntersection() {
        List<String> result = Stream.of("a", "b", "a", "c", "d").intersection(Fn.identity(), Arrays.asList("a", "c", "a", "e")).toList();
        assertEquals(Arrays.asList("a", "a", "c"), result); // Order from original stream
    }

    @Test
    public void testDifference() {
        List<String> result = Stream.of("a", "b", "a", "c", "d")
                .difference(Fn.identity(), Arrays.asList("a", "c", "e")) // remove one 'a', one 'c'
                .toList();
        assertEquals(Arrays.asList("b", "a", "d"), result);
    }

    @Test
    public void testPrependVarargs() {
        assertEquals(Arrays.asList(0, -1, 1, 2, 3), Stream.of(1, 2, 3).prepend(0, -1).toList());
    }

    @Test
    public void testAppendVarargs() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).toList());
    }

    @Test
    public void testAppendIfEmpty() {
        assertEquals(Arrays.asList(1, 2), Stream.<Integer> empty().appendIfEmpty(1, 2).toList());
        assertEquals(Arrays.asList(10), Stream.of(10).appendIfEmpty(1, 2).toList());
    }

    @Test
    public void testDefaultIfEmpty() {
        assertEquals(Arrays.asList(-1), Stream.<Integer> empty().defaultIfEmpty(-1).toList());
        assertEquals(Arrays.asList(10), Stream.of(10).defaultIfEmpty(-1).toList());
    }

    @Test
    public void testBuffered() throws InterruptedException {
        List<Integer> source = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> bufferedStream = Stream.of(source).buffered(2); // Small buffer
        List<Integer> result = new ArrayList<>();

        // Simulate slow consumption
        bufferedStream.forEach(e -> {
            result.add(e);
            try {
                Thread.sleep(10); // Ensure producer might get ahead
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        assertEquals(source, result);
    }

    //    @Test
    //    public void testBufferedWithQueue() throws InterruptedException {
    //        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);
    //        List<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
    //        AtomicBoolean producerFinished = new AtomicBoolean(false);
    //
    //        Thread producerThread = new Thread(() -> {
    //            try {
    //                for (Integer item : source) {
    //                    queue.put(item); // This will block if queue is full
    //                }
    //            } catch (InterruptedException e) {
    //                Thread.currentThread().interrupt();
    //            } finally {
    //                producerFinished.set(true);
    //            }
    //        });
    //        producerThread.start();
    //
    //        // Give producer a head start to fill the initial queue capacity.
    //        // This is not strictly necessary for the buffered(queue) method itself,
    //        // but for testing that it draws from the provided queue.
    //        while (queue.isEmpty() && producerThread.isAlive()) {
    //            Thread.sleep(1);
    //        }
    //
    //        assertThrows(IllegalArgumentException.class, () -> Stream.of(queue.iterator()) // Create a base stream from the queue's current state
    //                .buffered(queue)); // Then apply buffering with the *same* queue
    //
    //        List<Integer> consumed = new ArrayList<>();
    //        // Consume slightly slower to see buffering in action
    //        Stream.of(queue.iterator()).buffered().forEach(e -> {
    //            consumed.add(e);
    //            try {
    //                Thread.sleep(5);
    //            } catch (InterruptedException ex) {
    //                /* ignore */ }
    //        });
    //
    //        producerThread.join(); // Ensure producer is done.
    //        assertTrue(producerFinished.get());
    //
    //        // Elements in 'consumed' should be from 'source' via the queue.
    //        // The exact number might depend on timing if the stream terminates early
    //        // or if the test logic for queue filling/draining isn't perfectly synced.
    //        // Here, we expect all items if the stream consumes until the queue is empty
    //        // and the producer has finished.
    //        assertEquals(source, consumed);
    //    }

    @Test
    public void testMergeWithCollection() {
        Stream<Integer> s1 = Stream.of(1, 3, 5, 7);
        Collection<Integer> c2 = Arrays.asList(2, 4, 6);
        // Merge favoring smaller numbers
        List<Integer> merged = s1.mergeWith(c2, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), merged);
    }

    @Test
    public void testMergeWithStream() {
        Stream<Integer> s1 = Stream.of(1, 3, 5, 7);
        Stream<Integer> s2 = Stream.of(2, 4, 6);
        List<Integer> merged = s1.mergeWith(s2, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), merged);
    }

    @Test
    public void testZipWithCollection() {
        Collection<String> coll = Arrays.asList("a", "b", "c");
        List<Pair<Integer, String>> result = Stream.of(1, 2, 3, 4).zipWith(coll, Pair::of).toList();
        assertEquals(Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c")), result); // Length of shorter
    }

    @Test
    public void testZipWithCollectionAndDefaults() {
        Collection<String> coll = Arrays.asList("a", "b");
        List<Pair<Integer, String>> result = Stream.of(1, 2, 3, 4).zipWith(coll, 0, "z", Pair::of).toList();
        assertEquals(Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "z"), Pair.of(4, "z")), result);
    }

    @Test
    public void testZipWithStream() {
        Stream<String> streamB = Stream.of("a", "b", "c");
        List<Pair<Integer, String>> result = Stream.of(1, 2, 3, 4).zipWith(streamB, Pair::of).toList();
        assertEquals(Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c")), result);
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = Stream.of(1, 2, 3).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3), jdkStream.collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testTransformB() {
        Stream<String> transformed = Stream.of(1, 2, 3).transformB(jdkStream -> jdkStream.map(i -> "val" + i));
        assertEquals(Arrays.asList("val1", "val2", "val3"), transformed.toList());
    }

    // Static factory methods tests
    @Test
    public void testStreamEmpty() {
        assertTrue(Stream.empty().toList().isEmpty());
    }

    @Test
    public void testStreamDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Stream<Integer>> supplier = () -> {
            counter.incrementAndGet();
            return Stream.of(1, 2);
        };
        Stream<Integer> deferredStream = Stream.defer(supplier);
        assertEquals(0, counter.get()); // Supplier not called yet
        assertEquals(Arrays.asList(1, 2), deferredStream.toList());
        assertEquals(1, counter.get());
    }

    @Test
    public void testStreamFromJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.of(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), Stream.from(jdkStream).toList());
    }

    @Test
    public void testStreamJust() {
        assertEquals(Collections.singletonList(1), Stream.just(1).toList());
    }

    @Test
    public void testStreamOfNullable() {
        assertEquals(Collections.singletonList(1), Stream.ofNullable(1).toList());
        assertTrue(Stream.ofNullable(null).toList().isEmpty());
    }

    @Test
    public void testStreamOfArray() {
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(new Integer[] { 1, 2, 3 }).toList());
        assertTrue(Stream.of(new Integer[] {}).toList().isEmpty());
    }

    @Test
    public void testStreamOfArrayRange() {
        assertEquals(Arrays.asList(2, 3), Stream.of(new Integer[] { 1, 2, 3, 4 }, 1, 3).toList());
    }

    @Test
    public void testStreamOfCollection() {
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(Arrays.asList(1, 2, 3)).toList());
    }

    @Test
    public void testStreamOfIteratorRange() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(2, 3, 4), Stream.of(list.iterator()).skip(1).limit(3).toList());
        assertEquals(Collections.emptyList(), Stream.of(list.iterator()).skip(1).limit(0).toList());
        // assertThrows(IllegalArgumentException.class, () -> Stream.of(list.iterator(), 4, 1).toList());
    }

    @Test
    public void testStreamOfMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        List<Map.Entry<String, Integer>> entries = Stream.of(map).toList();
        assertEquals(2, entries.size());
        assertTrue(entries.contains(N.newEntry("a", 1)));
        assertTrue(entries.contains(N.newEntry("b", 2)));
    }

    @Test
    public void testStreamOfPrimitiveArrays() {
        assertEquals(Arrays.asList(true, false), Stream.of(new boolean[] { true, false }).toList());
        assertEquals(Arrays.asList('a', 'b'), Stream.of(new char[] { 'a', 'b' }).toList());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), Stream.of(new byte[] { 1, 2 }).toList());
        assertEquals(Arrays.asList((short) 1, (short) 2), Stream.of(new short[] { 1, 2 }).toList());
        assertEquals(Arrays.asList(1, 2), Stream.of(new int[] { 1, 2 }).toList());
        assertEquals(Arrays.asList(1L, 2L), Stream.of(new long[] { 1L, 2L }).toList());
        assertEquals(Arrays.asList(1.0f, 2.0f), Stream.of(new float[] { 1.0f, 2.0f }).toList());
        assertEquals(Arrays.asList(1.0, 2.0), Stream.of(new double[] { 1.0, 2.0 }).toList());
    }

    @Test
    public void testStreamOfOptional() {
        assertEquals(Collections.singletonList(1), Stream.of(Optional.of(1)).toList());
        assertTrue(Stream.of(Optional.empty()).toList().isEmpty());
        assertTrue(Stream.of((Optional<Integer>) null).toList().isEmpty()); // ofNullable handles null Optional
    }

    @Test
    public void testStreamOfJdkOptional() {
        assertEquals(Collections.singletonList(1), Stream.of(java.util.Optional.of(1)).toList());
        assertTrue(Stream.of(java.util.Optional.empty()).toList().isEmpty());
        assertTrue(Stream.of((java.util.Optional<Integer>) null).toList().isEmpty());
    }

    @Test
    public void testStreamOfKeys() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2);
        assertEquals(Set.of("a", "b"), Stream.ofKeys(map).toSet());
    }

    @Test
    public void testStreamOfValues() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2);
        List<Integer> values = Stream.ofValues(map).sorted().toList(); // Sort for predictable test
        assertEquals(Arrays.asList(1, 2), values);
    }

    @Test
    public void testStreamRange() {
        assertEquals(Arrays.asList(0, 1, 2), Stream.range(0, 3).toList());
        assertEquals(Arrays.asList(0, 2, 4), Stream.range(0, 5, 2).toList());
    }

    @Test
    public void testStreamRangeClosed() {
        assertEquals(Arrays.asList(0, 1, 2, 3), Stream.rangeClosed(0, 3).toList());
        assertEquals(Arrays.asList(0, 2), Stream.rangeClosed(0, 3, 2).toList());
    }

    @Test
    public void testStreamSplitString() {
        assertEquals(Arrays.asList("a", "b", "c"), Stream.split("a,b,c", ',').toList());
        assertEquals(Arrays.asList("a", "b", "c"), Stream.split("a-b-c", "-").toList());
    }

    @Test
    public void testStreamSplitToLines() {
        assertEquals(Arrays.asList("line1", "line2"), Stream.splitToLines("line1\nline2").toList());
        assertEquals(Arrays.asList("line1", "line2"), Stream.splitToLines("line1\r\nline2", true, true).toList());
        assertEquals(Arrays.asList("line1", "", "line2"), Stream.splitToLines("line1\n\nline2", false, false).toList());
        assertEquals(Arrays.asList("line1", "line2"), Stream.splitToLines("line1\n\nline2", true, true).toList());
    }

    @Test
    public void testStreamFlattenCollectionOfCollections() {
        List<List<Integer>> listOfLists = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), Stream.flatten(listOfLists).toList());
    }

    @Test
    public void testStreamFlatten2DArray() {
        Integer[][] arrayOfArrays = { { 1, 2 }, { 3, 4 } };
        assertEquals(Arrays.asList(1, 2, 3, 4), Stream.flatten(arrayOfArrays).toList());
    }

    @Test
    public void testStreamFlatten2DArrayVertically() {
        Integer[][] arrayOfArrays = { { 1, 2, 3 }, { 4, 5 }, { 6, 7, 8, 9 } };
        // Expected: 1,4,6, 2,5,7, 3,null,8, null,null,9 (if padded with null)
        // Abacus flatten vertically does not pad with nulls, it iterates existing elements.
        List<Integer> expected = Arrays.asList(1, 4, 6, 2, 5, 7, 3, 8, 9);
        assertEquals(expected, Stream.flatten(arrayOfArrays, true).toList());

        Integer[][] arrayOfArrays2 = { { 1, 2 }, { 3, 4, 5 } };
        List<Integer> expected2 = Arrays.asList(1, 3, 2, 4, 5);
        assertEquals(expected2, Stream.flatten(arrayOfArrays2, true).toList());
    }

    @Test
    public void testStreamFlatten2DArrayWithValueForAlignment() {
        Integer[][] arrayOfArrays = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        Integer fill = 0;
        // Horizontal (normal flatten, alignment not really used this way)
        List<Integer> horizontal = Stream.flatten(arrayOfArrays, fill, false).toList();
        assertEquals(Arrays.asList(1, 2, 0, 3, 0, 0, 4, 5, 6), horizontal); // Max length is 3.

        // Vertical
        List<Integer> vertical = Stream.flatten(arrayOfArrays, fill, true).toList();
        assertEquals(Arrays.asList(1, 3, 4, 2, 0, 5, 0, 0, 6), vertical);
    }

    @Test
    public void testStreamRepeat() {
        assertEquals(Arrays.asList("a", "a", "a"), Stream.repeat("a", 3).toList());
    }

    @Test
    public void testStreamIterateBooleanSupplier() {
        AtomicInteger val = new AtomicInteger(0);
        List<Integer> result = Stream.iterate(() -> val.get() < 3, () -> val.getAndIncrement()).toList();
        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testStreamIterateSeedHasNextUnaryOperator() {
        List<Integer> result = Stream.iterate(0, () -> true, x -> x + 1).limit(3).toList();
        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testStreamIterateSeedPredicateUnaryOperator() {
        List<Integer> result = Stream.iterate(0, x -> x < 3, x -> x + 1).toList();
        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testStreamGenerate() {
        AtomicInteger val = new AtomicInteger(0);
        List<Integer> result = Stream.generate(val::getAndIncrement).limit(3).toList();
        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testStreamOfLinesFile(@TempDir Path tempDir) throws IOException {
        Path tempFile = tempDir.resolve("test.txt");
        Files.write(tempFile, Arrays.asList("line1", "line2"), StandardCharsets.UTF_8);
        assertEquals(Arrays.asList("line1", "line2"), Stream.ofLines(tempFile.toFile()).toList());
    }

    @Test
    public void testStreamOfLinesReader() {
        Reader reader = new StringReader("line1\nline2");
        assertEquals(Arrays.asList("line1", "line2"), Stream.ofLines(reader).toList());
    }

    @Test
    public void testStreamListFiles(@TempDir Path tempDir) throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Path subDir = tempDir.resolve("subdir");
        Files.createFile(file1);
        Files.createFile(file2);
        Files.createDirectory(subDir);
        Path file3 = subDir.resolve("file3.txt");
        Files.createFile(file3);

        Set<String> namesNonRecursive = Stream.listFiles(tempDir.toFile(), false).map(File::getName).toSet();
        assertEquals(Set.of("file1.txt", "file2.txt", "subdir"), namesNonRecursive);

        Set<String> namesRecursive = Stream.listFiles(tempDir.toFile(), true).map(File::getName).toSet();
        assertEquals(Set.of("file1.txt", "file2.txt", "subdir", "file3.txt"), namesRecursive);

        Set<String> filesOnlyRecursive = Stream.listFiles(tempDir.toFile(), true, true).map(File::getName).toSet();
        assertEquals(Set.of("file1.txt", "file2.txt", "file3.txt"), filesOnlyRecursive);
    }

    @Test
    public void testStreamConcatArrays() {
        Integer[] a1 = { 1, 2 };
        Integer[] a2 = { 3, 4 };
        assertEquals(Arrays.asList(1, 2, 3, 4), Stream.concat(a1, a2).toList());
    }

    @Test
    public void testStreamConcatStreams() {
        Stream<Integer> s1 = Stream.of(1, 2);
        Stream<Integer> s2 = Stream.of(3, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4), Stream.concat(s1, s2).toList());
    }

    @Test
    public void testStreamParallelConcatStreams() {
        Stream<Integer> s1 = Stream.of(1, 2);
        Stream<Integer> s2 = Stream.of(3, 4);
        Stream<Integer> s3 = Stream.of(5, 6);

        // Collect to a Set because order is not guaranteed with parallelConcat
        Set<Integer> result = Stream.parallelConcat(s1, s2, s3).toSet();
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testParallelConcatIteratorsWithParams() throws InterruptedException {
        Iterator<Integer> iter1 = Arrays.asList(1, 2, 3).iterator();
        Iterator<Integer> iter2 = Arrays.asList(4, 5, 6).iterator();
        Collection<Iterator<? extends Integer>> iterators = Arrays.asList(iter1, iter2);

        // Test with specific readThreadNum and bufferSize
        Stream<Integer> stream = Stream.parallelConcatIterators(iterators, 1, 10); // 1 read thread, buffer 10
        Set<Integer> result = stream.toSet();
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), result);
    }

    // Representative Zip Test
    @Test
    public void testZipArrays() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        List<Pair<String, Integer>> result = Stream.zip(a, b, Pair::of).toList();
        assertEquals(Arrays.asList(Pair.of("a", 1), Pair.of("b", 2)), result);
    }

    @Test
    public void testZipStreamsWithDefaults() {
        Stream<String> sA = Stream.of("x", "y");
        Stream<Integer> sB = Stream.of(1, 2, 3, 4);
        List<Pair<String, Integer>> result = Stream.zip(sA, sB, "defaultA", 0, Pair::of).toList();
        assertEquals(Arrays.asList(Pair.of("x", 1), Pair.of("y", 2), Pair.of("defaultA", 3), Pair.of("defaultA", 4)), result);
    }

    @Test
    public void testParallelZipIteratorsWithParams() {
        Iterator<Integer> iterA = Arrays.asList(1, 2, 3, 4).iterator();
        Iterator<String> iterB = Arrays.asList("a", "b", "c").iterator();
        // List<String> defaultValues = Arrays.asList("defaultB"); // Incorrect, should match T for iterators

        // This static method is parallelZipIterators(Collection, Function, int)
        // or parallelZipIterators(Collection, List, Function, int)

        List<Pair<Integer, String>> result = Stream.parallelZipIterators(Arrays.asList(iterA, iterB), // This means it will zip (1 with "a"), (2 with "b"), (3 with "c") element-wise from the *zipped stream*
                // The parallelZipIterators that takes a list of iterators and a single zipFunction is for zipping *corresponding elements* across multiple iterators
                //  into a List<T> which is then passed to the zipFunction.
                (List<Object> list) -> Pair.of((Integer) list.get(0), (String) list.get(1)), 2).toList();

        assertTrue(N.isEqualCollection(Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c")), result));

        Iterator<Integer> iterA2 = Arrays.asList(1, 2, 3, 4).iterator();
        Iterator<String> iterB2 = Arrays.asList("a", "b", "c").iterator();
        List<Pair<Integer, String>> resultWithDefaults = Stream.parallelZipIterators(Arrays.asList(iterA2, iterB2), Arrays.asList(0, "defaultVal"), // valuesForNone
                (List<Object> list) -> Pair.of((Integer) list.get(0), (String) list.get(1)), 2).toList();
        assertTrue(N.isEqualCollection(Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"), Pair.of(4, "defaultVal")), resultWithDefaults));
    }

    // Representative Merge Test
    @Test
    public void testMergeIterables() {
        List<Integer> list1 = Arrays.asList(1, 4, 6);
        List<Integer> list2 = Arrays.asList(2, 3, 5, 7);
        List<Integer> merged = Stream.merge(list1, list2, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), merged);
    }

    @Test
    public void testParallelZip() throws InterruptedException {
        List<Integer> listA = new ArrayList<>();
        List<Integer> listB = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            listA.add(i);
            listB.add(i * 10);
        }

        CountDownLatch latch = new CountDownLatch(listA.size());

        Stream<Pair<Integer, Integer>> zippedStream = Stream.parallelZip(listA, listB, (a, b) -> {
            latch.countDown();
            return Pair.of(a, b);
        }, 4);

        List<Pair<Integer, Integer>> result = zippedStream.toList();
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Zip function calls did not complete in time");

        assertEquals(listA.size(), result.size());
        for (int i = 0; i < listA.size(); i++) {
            assertTrue(result.contains(Pair.of(listA.get(i), listB.get(i))), "Missing pair for index " + i);
        }
    }

    @Test
    public void testParallelMerge() {
        Stream<Integer> s1 = Stream.of(1, 5, 9);
        Stream<Integer> s2 = Stream.of(2, 6, 10);
        Stream<Integer> s3 = Stream.of(3, 7, 11);
        Stream<Integer> s4 = Stream.of(4, 8, 12);

        List<Stream<Integer>> streams = Arrays.asList(s1, s2, s3, s4);

        List<Integer> result = Stream.parallelMerge(streams, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 2) // Using 2 threads for merging pairs
                .toList();

        List<Integer> expected = IntStream.rangeClosed(1, 12).boxed().toList();
        assertEquals(expected, result);
    }

    // Test for a static zip variant to ensure those are covered conceptually
    @Test
    public void testStaticZipIntArrays() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6, 7 };
        List<Pair<Integer, Integer>> result = Stream.zip(a, b, (x, y) -> Pair.of(x, y)).toList();
        assertEquals(Arrays.asList(Pair.of(1, 4), Pair.of(2, 5), Pair.of(3, 6)), result);
    }

    @Test
    public void testStaticZipCollectionsWithDefaults() {
        List<String> c1 = Arrays.asList("a", "b");
        List<Integer> c2 = Arrays.asList(1, 2, 3);
        List<String> result = Stream.zip(c1, c2, "default_a", 0, (s, i) -> s + ":" + i).toList();
        assertEquals(Arrays.asList("a:1", "b:2", "default_a:3"), result);
    }

    @Test
    public void testObserveBlockingQueueWithDuration() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        List<Integer> producedItems = Collections.synchronizedList(new ArrayList<>());
        Duration observationDuration = Duration.ofMillis(200);

        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    queue.put(i);
                    Thread.sleep(30);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();

        Stream.observe(queue, observationDuration).forEach(producedItems::add);

        producer.join();
        // Depending on exact timings, between 4 to 5 items might be consumed.
        // (5 items * 30ms = 150ms, which is < 200ms duration)
        assertEquals(5, producedItems.size());
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), producedItems);
    }

    @Test
    public void testObserveBlockingQueueWithSupplier() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        List<Integer> consumedItems = Collections.synchronizedList(new ArrayList<>());
        MutableBoolean hasMore = MutableBoolean.of(true);
        long maxWaitInterval = 50; // ms

        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    if (!hasMore.isTrue())
                        break;
                    queue.put(i);
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                hasMore.setFalse();
            }
        });

        producer.start();
        Stream.observe(queue, hasMore::isTrue, maxWaitInterval).forEach(consumedItems::add);

        producer.join();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), consumedItems);
        assertFalse(hasMore.isTrue());
    }

    @Test
    public void testForEachPairAndTriple() {
        List<String> pairs = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachPair((a, b) -> pairs.add(a + "-" + (b == null ? "N" : b)));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), pairs);

        List<String> triples = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5).forEachTriple((a, b, c) -> triples.add(a + "-" + (b == null ? "N" : b) + "-" + (c == null ? "N" : c)));
        assertEquals(Arrays.asList("1-2-3", "2-3-4", "3-4-5"), triples);
    }

    @Test
    public void testSplitByChunkCountStatic() {
        List<int[]> result = Stream.splitByChunkCount(7, 3, (from, to) -> Arrays.copyOfRange(new int[] { 0, 1, 2, 3, 4, 5, 6 }, from, to)).toList();
        // 7 items, 3 chunks. Sizes: 3, 2, 2 (bigger first)
        // Chunks: [0,1,2], [3,4], [5,6]
        assertArrayEquals(new int[] { 0, 1, 2 }, result.get(0));
        assertArrayEquals(new int[] { 3, 4 }, result.get(1));
        assertArrayEquals(new int[] { 5, 6 }, result.get(2));

        List<int[]> resultSmallerFirst = Stream.splitByChunkCount(7, 3, true, (from, to) -> Arrays.copyOfRange(new int[] { 0, 1, 2, 3, 4, 5, 6 }, from, to))
                .toList();
        // Sizes: 2, 2, 3
        // Chunks: [0,1], [2,3], [4,5,6]
        assertArrayEquals(new int[] { 0, 1 }, resultSmallerFirst.get(0));
        assertArrayEquals(new int[] { 2, 3 }, resultSmallerFirst.get(1));
        assertArrayEquals(new int[] { 4, 5, 6 }, resultSmallerFirst.get(2));
    }

    @Test
    public void testStreamOfLinesPath(@TempDir Path tempDir) throws IOException {
        Path tempFile = tempDir.resolve("testLines.txt");
        List<String> lines = Arrays.asList("Hello", "Stream", "World");
        Files.write(tempFile, lines, StandardCharsets.UTF_8);

        List<String> readLines = Stream.ofLines(tempFile).toList();
        assertEquals(lines, readLines);
    }

    @Test
    public void testStreamOfLinesReaderWithAutoClose() throws IOException {
        final AtomicBoolean closed = new AtomicBoolean(false);
        Reader reader = new StringReader("line1\nline2") {
            @Override
            public void close() {
                super.close();
                closed.set(true);
            }
        };
        Stream.ofLines(reader, true).toList(); // Consume the stream
        assertTrue(closed.get(), "Reader should be closed when stream is closed");

        closed.set(false);
        Reader readerNoClose = new StringReader("line1\nline2") {
            @Override
            public void close() {
                super.close();
                closed.set(true);
            }
        };
        Stream.ofLines(readerNoClose, false).toList();
        assertFalse(closed.get(), "Reader should not be closed when autoClose is false");
        readerNoClose.close(); // Manually close for test hygiene
    }

}
