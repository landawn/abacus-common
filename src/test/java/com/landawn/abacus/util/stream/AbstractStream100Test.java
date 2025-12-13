package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("new-test")
public class AbstractStream100Test extends TestBase {

    @TempDir
    Path tempFolder;

    private <T> Stream<T> createStream(Iterable<? extends T> iter) {
        return Stream.of(iter);
    }

    @Test
    public void testSelect() {
        List<Object> mixed = Arrays.asList("string", 1, 2.0, "another", 3);
        Stream<Object> stream = createStream(mixed);

        List<String> strings = stream.select(String.class).toList();
        assertEquals(Arrays.asList("string", "another"), strings);
    }

    @Test
    public void testPairWith() {
        List<String> input = Arrays.asList("a", "bb", "ccc");
        Stream<String> stream = createStream(input);

        List<Pair<String, Integer>> pairs = stream.pairWith(String::length).toList();
        assertEquals(3, pairs.size());
        assertEquals("a", pairs.get(0).left());
        assertEquals(1, pairs.get(0).right().intValue());
        assertEquals("bb", pairs.get(1).left());
        assertEquals(2, pairs.get(1).right().intValue());
    }

    @Test
    public void testSkipUntil() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.skipUntil(x -> x > 2).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.filter(x -> x % 2 == 0, dropped::add).toList();
        assertEquals(Arrays.asList(2, 4), result);
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.dropWhile(x -> x < 3, dropped::add).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testStep() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        Stream<Integer> stream1 = createStream(input);
        assertEquals(input, stream1.step(1).toList());

        Stream<Integer> stream2 = createStream(input);
        assertEquals(Arrays.asList(1, 3, 5, 7, 9), stream2.step(2).toList());

        Stream<Integer> stream3 = createStream(input);
        assertEquals(Arrays.asList(1, 4, 7), stream3.step(3).toList());
    }

    @Test
    public void testStepWithInvalidArgument() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> stream.step(0));
    }

    @Test
    public void testSlidingMapBiFunction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> sums = stream.slidingMap(Integer::sum).toList();
        assertEquals(Arrays.asList(3, 5, 7, 9), sums);
    }

    @Test
    public void testSlidingMapWithIncrement() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> sums = stream.slidingMap(2, (a, b) -> a + (b == null ? 0 : b)).toList();
        assertEquals(Arrays.asList(3, 7, 5), sums);
    }

    @Test
    public void testSlidingMapTriFunction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> sums = stream.slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9, 12), sums);
    }

    @Test
    public void testMapIfNotNull() {
        List<String> input = Arrays.asList("a", null, "b", null, "c");
        Stream<String> stream = createStream(input);

        List<String> result = stream.mapIfNotNull(String::toUpperCase).toList();
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testMapToEntry() {
        List<String> input = Arrays.asList("a", "bb", "ccc");
        Stream<String> stream = createStream(input);

        List<Map.Entry<String, Integer>> entries = stream.mapToEntry(s -> new AbstractMap.SimpleEntry<>(s, s.length())).entries().toList();

        assertEquals(3, entries.size());
        assertEquals("a", entries.get(0).getKey());
        assertEquals(1, entries.get(0).getValue().intValue());
    }

    @Test
    public void testMapToEntryWithKeyValueMappers() {
        List<String> input = Arrays.asList("a", "bb", "ccc");
        Stream<String> stream = createStream(input);

        List<Map.Entry<String, Integer>> entries = stream.mapToEntry(String::toUpperCase, String::length).entries().toList();

        assertEquals(3, entries.size());
        assertEquals("A", entries.get(0).getKey());
        assertEquals(1, entries.get(0).getValue().intValue());
    }

    @Test
    public void testFlatmap() {
        List<String> input = Arrays.asList("a,b", "c,d,e");
        Stream<String> stream = createStream(input);

        List<String> result = stream.flatmap(s -> Arrays.asList(s.split(","))).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    public void testFlatmapToInt() {
        List<String> input = Arrays.asList("1,2", "3,4,5");
        Stream<String> stream = createStream(input);

        int[] result = stream.flatmapToInt(s -> Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray()).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapMulti() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.<Integer> mapMulti((n, consumer) -> {
            for (int i = 0; i < n; i++) {
                consumer.accept(n);
            }
        }).toList();

        assertEquals(Arrays.asList(1, 2, 2, 3, 3, 3), result);
    }

    @Test
    public void testMapPartial() {
        List<String> input = Arrays.asList("1", "abc", "2", "3");
        Stream<String> stream = createStream(input);

        List<Integer> result = stream.mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.<Integer> empty();
            }
        }).toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testRangeMap() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<String> result = stream.rangeMap(Objects::equals, (left, right) -> left + "-" + right).toList();

        assertEquals(Arrays.asList("1-1", "2-2", "3-3", "4-4"), result);
    }

    @Test
    public void testCollapse() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<List<Integer>> result = stream.collapse(Objects::equals).toList();
        assertEquals(4, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
        assertEquals(Arrays.asList(2, 2), result.get(1));
        assertEquals(Arrays.asList(3, 3, 3), result.get(2));
        assertEquals(Arrays.asList(4), result.get(3));
    }

    @Test
    public void testCollapse_2() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.collapse((a, b, c) -> b == c, (b, c) -> b + c).toList();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0));
        assertEquals(4, result.get(1));
        assertEquals(9, result.get(2));
        assertEquals(4, result.get(3));
    }

    @Test
    public void testCollapse_3() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.collapse((a, b, c) -> b == c, 0, (b, c) -> b + c).toList();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0));
        assertEquals(4, result.get(1));
        assertEquals(9, result.get(2));
        assertEquals(4, result.get(3));
    }

    @Test
    public void testCollapse_4() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.collapse((a, b, c) -> b == c, Collectors.summingInt(e -> e)).toList();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0));
        assertEquals(4, result.get(1));
        assertEquals(9, result.get(2));
        assertEquals(4, result.get(3));
    }

    @Test
    public void testScan() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.scan(Integer::sum).toList();
        assertEquals(Arrays.asList(1, 3, 6, 10), result);
    }

    @Test
    public void testScanWithInit() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.scan(10, Integer::sum).toList();
        assertEquals(Arrays.asList(11, 13, 16, 20), result);
    }

    @Test
    public void testSplit() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Stream<Integer> stream = createStream(input);

        List<List<Integer>> result = stream.split(3).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(4, 5, 6), result.get(1));
        assertEquals(Arrays.asList(7, 8, 9), result.get(2));
    }

    @Test
    public void testSplitByPredicate() {
        List<Integer> input = Arrays.asList(1, 2, 0, 3, 4, 0, 5);
        Stream<Integer> stream = createStream(input);

        List<List<Integer>> result = stream.split(x -> x == 0).toList();
        assertEquals(5, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(0), result.get(1));
        assertEquals(Arrays.asList(3, 4), result.get(2));
        assertEquals(Arrays.asList(0), result.get(3));
        assertEquals(Arrays.asList(5), result.get(4));
    }

    @Test
    public void testSplitAt() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Stream<Integer>> splits = stream.splitAt(x -> x == 3).toList();
        assertEquals(2, splits.size());
        assertEquals(Arrays.asList(1, 2), splits.get(0).toList());
        assertEquals(Arrays.asList(3, 4, 5), splits.get(1).toList());
    }

    @Test
    public void testSplitAt_advice() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        {
            assertEquals(input, createStream(input).splitAt(4).skip(0).flatMap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), createStream(input).splitAt(4).skip(1).flatMap(s -> s).toList());
            assertTrue(createStream(input).splitAt(4).skip(2).flatMap(s -> s).toList().isEmpty());

            assertEquals(input, createStream(input).splitAt(x -> x == 5).skip(0).flatMap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), createStream(input).splitAt(x -> x == 5).skip(1).flatMap(s -> s).toList());
            assertTrue(createStream(input).splitAt(x -> x == 5).skip(2).flatMap(s -> s).toList().isEmpty());
        }

        {
            assertEquals(input, createStream(input).splitAt(4, Collectors.toList()).skip(0).flatmap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), createStream(input).splitAt(4, Collectors.toList()).skip(1).flatmap(s -> s).toList());
            assertTrue(createStream(input).splitAt(4, Collectors.toList()).skip(2).flatmap(s -> s).toList().isEmpty());

            assertEquals(input, createStream(input).splitAt(x -> x == 5, Collectors.toList()).skip(0).flatmap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), createStream(input).splitAt(x -> x == 5, Collectors.toList()).skip(1).flatmap(s -> s).toList());
            assertTrue(createStream(input).splitAt(x -> x == 5, Collectors.toList()).skip(2).flatmap(s -> s).toList().isEmpty());
        }
    }

    @Test
    public void testSplitAt_advice_array() {
        Integer[] inputArray = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        List<Integer> input = Arrays.asList(inputArray);

        {
            assertEquals(input, Stream.of(inputArray).splitAt(4).skip(0).flatMap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), Stream.of(inputArray).splitAt(4).skip(1).flatMap(s -> s).toList());
            assertTrue(Stream.of(inputArray).splitAt(4).skip(2).flatMap(s -> s).toList().isEmpty());

            assertEquals(input, Stream.of(inputArray).splitAt(x -> x == 5).skip(0).flatMap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), Stream.of(inputArray).splitAt(x -> x == 5).skip(1).flatMap(s -> s).toList());
            assertTrue(Stream.of(inputArray).splitAt(x -> x == 5).skip(2).flatMap(s -> s).toList().isEmpty());
        }

        {
            assertEquals(input, Stream.of(inputArray).splitAt(4, Collectors.toList()).skip(0).flatmap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), Stream.of(inputArray).splitAt(4, Collectors.toList()).skip(1).flatmap(s -> s).toList());
            assertTrue(Stream.of(inputArray).splitAt(4, Collectors.toList()).skip(2).flatmap(s -> s).toList().isEmpty());

            assertEquals(input, Stream.of(inputArray).splitAt(x -> x == 5, Collectors.toList()).skip(0).flatmap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), Stream.of(inputArray).splitAt(x -> x == 5, Collectors.toList()).skip(1).flatmap(s -> s).toList());
            assertTrue(Stream.of(inputArray).splitAt(x -> x == 5, Collectors.toList()).skip(2).flatmap(s -> s).toList().isEmpty());
        }
    }

    @Test
    public void testSliding() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<List<Integer>> result = stream.sliding(3).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));
    }

    @Test
    public void testSlidingWithIncrement() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6);
        Stream<Integer> stream = createStream(input);

        List<List<Integer>> result = stream.sliding(3, 2).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));
        assertEquals(Arrays.asList(5, 6), result.get(2));
    }

    @Test
    public void testIntersperse() {
        List<String> input = Arrays.asList("a", "b", "c");
        Stream<String> stream = createStream(input);

        List<String> result = stream.intersperse(",").toList();
        assertEquals(Arrays.asList("a", ",", "b", ",", "c"), result);
    }

    @Test
    public void testOnFirst() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<Integer> captured = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.onFirst(captured::add).toList();
        assertEquals(input, result);
        assertEquals(Arrays.asList(1), captured);
    }

    @Test
    public void testOnLast() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<Integer> captured = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.onLast(captured::add).toList();
        assertEquals(input, result);
        assertEquals(Arrays.asList(3), captured);
    }

    @Test
    public void testForEachIndexed() {
        List<String> input = Arrays.asList("a", "b", "c");
        Map<Integer, String> indexMap = new HashMap<>();
        Stream<String> stream = createStream(input);

        stream.forEachIndexed(indexMap::put);

        assertEquals("a", indexMap.get(0));
        assertEquals("b", indexMap.get(1));
        assertEquals("c", indexMap.get(2));
    }

    @Test
    public void testForEachUntil() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> collected = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        stream.forEachUntil((value, flag) -> {
            collected.add(value);
            if (value == 3) {
                flag.setValue(true);
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachPair() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4);
        List<String> pairs = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        stream.forEachPair((a, b) -> pairs.add(a + "-" + b));

        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), pairs);
    }

    @Test
    public void testForEachTriple() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<String> triples = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        stream.forEachTriple((a, b, c) -> triples.add(a + "-" + b + "-" + c));

        assertEquals(Arrays.asList("1-2-3", "2-3-4", "3-4-5"), triples);
    }

    //    @Test
    //    public void testReduceUntil() {
    //        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
    //        Stream<Integer> stream = createStream(input);
    //
    //        Optional<Integer> result = stream.reduceUntil(Integer::sum, sum -> sum >= 10);
    //        assertTrue(result.isPresent());
    //        assertEquals(10, result.get().intValue());
    //    }
    //
    //    @Test
    //    public void testReduceUntil_2() {
    //        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
    //        Stream<Integer> stream = createStream(input);
    //
    //        Integer result = stream.reduceUntil(0, (a, b) -> a + b, (a, b) -> a + b, sum -> sum >= 10);
    //        assertEquals(11, result);
    //    }
    //
    //    @Test
    //    public void testReduceUntil_3() {
    //        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
    //        Stream<Integer> stream = createStream(input);
    //
    //        Integer result = stream.reduceUntil(0, (a, b) -> a + b, (a, b) -> a + b, (sum, next) -> sum + next > 10);
    //        assertEquals(10, result);
    //    }

    @Test
    public void testGroupBy() {
        List<String> input = Arrays.asList("a", "bb", "ccc", "dd", "e");
        Stream<String> stream = createStream(input);

        Map<Integer, List<String>> groups = stream.groupBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(3, groups.size());
        assertEquals(Arrays.asList("a", "e"), groups.get(1));
        assertEquals(Arrays.asList("bb", "dd"), groups.get(2));
        assertEquals(Arrays.asList("ccc"), groups.get(3));
    }

    @Test
    public void testPartitionBy() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        Map<Boolean, List<Integer>> partitions = stream.partitionBy(x -> x % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList(2, 4), partitions.get(true));
        assertEquals(Arrays.asList(1, 3, 5), partitions.get(false));
    }

    @Test
    public void testToMap() {
        List<String> input = Arrays.asList("a", "bb", "ccc");
        Stream<String> stream = createStream(input);

        Map<String, Integer> map = stream.toMap(Function.identity(), String::length);

        assertEquals(3, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("bb").intValue());
        assertEquals(3, map.get("ccc").intValue());
    }

    @Test
    public void testToMapWithMergeFunction() {
        List<String> input = Arrays.asList("a", "b", "a", "c", "b");
        Stream<String> stream = createStream(input);

        Map<String, Integer> map = stream.toMap(Function.identity(), s -> 1, Integer::sum);

        assertEquals(2, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
        assertEquals(1, map.get("c").intValue());
    }

    @Test
    public void testGroupTo() {
        List<String> input = Arrays.asList("a", "bb", "ccc", "dd");
        Stream<String> stream = createStream(input);

        Map<Integer, List<String>> groups = stream.groupTo(String::length);

        assertEquals(Arrays.asList("a"), groups.get(1));
        assertEquals(Arrays.asList("bb", "dd"), groups.get(2));
        assertEquals(Arrays.asList("ccc"), groups.get(3));
    }

    @Test
    public void testFlatGroupTo() {
        List<String> input = Arrays.asList("a,b", "c,d,e");
        Stream<String> stream = createStream(input);

        Map<String, List<String>> groups = stream.flatGroupTo(s -> Arrays.asList(s.split(",")), (letter, original) -> original);

        assertEquals(Arrays.asList("a,b"), groups.get("a"));
        assertEquals(Arrays.asList("a,b"), groups.get("b"));
        assertEquals(Arrays.asList("c,d,e"), groups.get("c"));
    }

    @Test
    public void testPartitionTo() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        Map<Boolean, List<Integer>> partitions = stream.partitionTo(x -> x % 2 == 0);

        assertEquals(Arrays.asList(2, 4), partitions.get(true));
        assertEquals(Arrays.asList(1, 3, 5), partitions.get(false));
    }

    @Test
    public void testToMultimap() {
        List<String> input = Arrays.asList("a", "b", "a", "c", "b");
        Stream<String> stream = createStream(input);

        ListMultimap<String, String> multimap = stream.toMultimap(Function.identity());

        assertEquals(Arrays.asList("a", "a"), multimap.get("a"));
        assertEquals(Arrays.asList("b", "b"), multimap.get("b"));
        assertEquals(Arrays.asList("c"), multimap.get("c"));
    }

    @Test
    public void testSumInt() {
        List<String> input = Arrays.asList("a", "bb", "ccc");
        Stream<String> stream = createStream(input);

        long sum = stream.sumInt(String::length);
        assertEquals(6L, sum);
    }

    @Test
    public void testAverageInt() {
        List<String> input = Arrays.asList("a", "bb", "ccc");
        Stream<String> stream = createStream(input);

        OptionalDouble avg = stream.averageInt(String::length);
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testMinAll() {
        List<Integer> input = Arrays.asList(3, 1, 4, 1, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> minAll = stream.minAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 1), minAll);
    }

    @Test
    public void testMaxAll() {
        List<Integer> input = Arrays.asList(3, 1, 5, 1, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> maxAll = stream.maxAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(5, 5), maxAll);
    }

    @Test
    public void testFindAny() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        Optional<Integer> found = stream.findAny(x -> x > 3);
        assertTrue(found.isPresent());
        assertEquals(4, found.get().intValue());
    }

    @Test
    public void testContainsAll() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream1 = createStream(input);
        assertTrue(stream1.containsAll(1, 3, 5));

        Stream<Integer> stream2 = createStream(input);
        assertFalse(stream2.containsAll(1, 3, 6));

        Stream<Integer> stream3 = createStream(input);
        assertTrue(stream3.containsAll(Arrays.asList(2, 4)));
    }

    @Test
    public void testContainsAny() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream1 = createStream(input);
        assertTrue(stream1.containsAny(6, 7, 3));

        Stream<Integer> stream2 = createStream(input);
        assertFalse(stream2.containsAny(6, 7, 8));

        Stream<Integer> stream3 = createStream(input);
        assertTrue(stream3.containsAny(Arrays.asList(6, 2)));
    }

    @Test
    public void testContainsNone() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream1 = createStream(input);
        assertTrue(stream1.containsNone(6, 7, 8));

        Stream<Integer> stream2 = createStream(input);
        assertFalse(stream2.containsNone(6, 7, 3));
    }

    @Test
    public void testFirst() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        Optional<Integer> first = stream.first();
        assertTrue(first.isPresent());
        assertEquals(1, first.get().intValue());

        Stream<Integer> emptyStream = createStream(Collections.emptyList());
        assertFalse(emptyStream.first().isPresent());
    }

    @Test
    public void testLast() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        Optional<Integer> last = stream.last();
        assertTrue(last.isPresent());
        assertEquals(3, last.get().intValue());

        Stream<Integer> emptyStream = createStream(Collections.emptyList());
        assertFalse(emptyStream.last().isPresent());
    }

    @Test
    public void testElementAt() {
        List<String> input = Arrays.asList("a", "b", "c", "d");

        Stream<String> stream1 = createStream(input);
        assertEquals("a", stream1.elementAt(0).get());

        Stream<String> stream2 = createStream(input);
        assertEquals("c", stream2.elementAt(2).get());

        Stream<String> stream3 = createStream(input);
        assertFalse(stream3.elementAt(10).isPresent());
    }

    @Test
    public void testOnlyOne() {
        Stream<Integer> stream1 = createStream(Arrays.asList(42));
        Optional<Integer> only1 = stream1.onlyOne();
        assertTrue(only1.isPresent());
        assertEquals(42, only1.get().intValue());

        Stream<Integer> stream2 = createStream(Collections.emptyList());
        assertFalse(stream2.onlyOne().isPresent());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testSkipNulls() {
        List<String> input = Arrays.asList("a", null, "b", null, "c");
        Stream<String> stream = createStream(input);

        List<String> result = stream.skipNulls().toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSkipRange() {
        List<Integer> input = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.skipRange(2, 7).toList();
        assertEquals(Arrays.asList(0, 1, 7, 8, 9), result);
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> skipped = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.skip(2, skipped::add).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testIntersection() {
        List<Integer> input = Arrays.asList(1, 2, 3, 2, 4, 3);
        List<Integer> other = Arrays.asList(2, 3, 3, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.intersection(other).toList();
        assertEquals(Arrays.asList(2, 3, 3), result);
    }

    @Test
    public void testDifference() {
        List<Integer> input = Arrays.asList(1, 2, 3, 2, 4, 3);
        List<Integer> other = Arrays.asList(2, 3, 3, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.difference(other).toList();
        assertEquals(Arrays.asList(1, 2, 4), result);
    }

    @Test
    public void testSymmetricDifference() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4);
        List<Integer> other = Arrays.asList(3, 4, 5, 6);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.symmetricDifference(other).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 5, 6), result);
    }

    @Test
    public void testReversed() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.reversed().toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testRotated() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        Stream<Integer> stream1 = createStream(input);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), stream1.rotated(2).toList());

        Stream<Integer> stream2 = createStream(input);
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), stream2.rotated(-2).toList());
    }

    @Test
    public void testRotated_2() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        Stream<Integer> stream1 = createStream(input);
        assertEquals(Arrays.asList(1, 2, 3), stream1.rotated(2).skip(2).toList());

        Stream<Integer> stream2 = createStream(input);
        assertArrayEquals(Arrays.asList(3, 4, 5, 1, 2).toArray(Integer[]::new), stream2.rotated(-2).toArray(Integer[]::new));
    }

    @Test
    public void testReverse_2() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        Stream<Integer> stream1 = createStream(input);
        assertEquals(Arrays.asList(3, 2, 1), stream1.reversed().skip(2).toList());

        Stream<Integer> stream2 = createStream(input);
        assertArrayEquals(Arrays.asList(3, 2, 1).toArray(Integer[]::new), stream2.reversed().skip(2).toArray(Integer[]::new));
    }

    @Test
    public void testShuffled() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.shuffled(new Random(42)).toList();
        assertEquals(5, result.size());
        assertTrue(result.containsAll(input));
    }

    @Test
    public void testSorted() {
        List<Integer> input = Arrays.asList(3, 1, 4, 1, 5, 9, 2);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.sorted().toList();
        assertEquals(Arrays.asList(1, 1, 2, 3, 4, 5, 9), result);
    }

    @Test
    public void testSortedBy() {
        List<String> input = Arrays.asList("ccc", "a", "bb", "dddd");
        Stream<String> stream = createStream(input);

        List<String> result = stream.sortedBy(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc", "dddd"), result);
    }

    @Test
    public void testReverseSorted() {
        List<Integer> input = Arrays.asList(3, 1, 4, 1, 5, 9, 2);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.reverseSorted().toList();
        assertEquals(Arrays.asList(9, 5, 4, 3, 2, 1, 1), result);
    }

    @Test
    public void testDistinct() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4, 4, 4, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.distinct().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testDistinctBy() {
        List<String> input = Arrays.asList("a", "bb", "ccc", "dd", "e");
        Stream<String> stream = createStream(input);

        List<String> result = stream.distinctBy(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testTop() {
        List<Integer> input = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.top(3).toList();
        assertEquals(Arrays.asList(5, 9, 6), result);
    }

    @Test
    public void testPercentiles() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Stream<Integer> stream = createStream(input);

        Optional<Map<Percentage, Integer>> percentiles = stream.percentiles();
        assertTrue(percentiles.isPresent());

        Map<Percentage, Integer> map = percentiles.get();
        assertNotNull(map.get(Percentage._50));
    }

    @Test
    public void testCombinations() {
        List<String> input = Arrays.asList("A", "B", "C");
        Stream<String> stream = createStream(input);

        List<List<String>> all = stream.combinations().toList();
        assertEquals(8, all.size());
        assertTrue(all.contains(Collections.emptyList()));
        assertTrue(all.contains(Arrays.asList("A", "B", "C")));
    }

    @Test
    public void testCombinationsWithLength() {
        List<String> input = Arrays.asList("A", "B", "C");
        Stream<String> stream = createStream(input);

        List<List<String>> combinations = stream.combinations(2).toList();
        assertEquals(3, combinations.size());
        assertTrue(combinations.contains(Arrays.asList("A", "B")));
        assertTrue(combinations.contains(Arrays.asList("A", "C")));
        assertTrue(combinations.contains(Arrays.asList("B", "C")));
    }

    @Test
    public void testPermutations() {
        List<String> input = Arrays.asList("A", "B", "C");
        Stream<String> stream = createStream(input);

        List<List<String>> permutations = stream.permutations().toList();
        assertEquals(6, permutations.size());
        assertTrue(permutations.contains(Arrays.asList("A", "B", "C")));
        assertTrue(permutations.contains(Arrays.asList("C", "B", "A")));
    }

    @Test
    public void testOrderedPermutations() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        List<List<Integer>> permutations = stream.orderedPermutations().toList();
        assertTrue(permutations.size() > 0);
        assertEquals(Arrays.asList(1, 2, 3), permutations.get(0));
    }

    @Test
    public void testCartesianProduct() {
        List<String> input = Arrays.asList("A", "B");
        List<List<Integer>> other = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3));
        Stream<String> stream = createStream(input);

        List<List<Object>> product = stream.cartesianProduct((Collection) other).toList();
        assertEquals(4, product.size());
    }

    @Test
    public void testToArray() {
        List<String> input = Arrays.asList("a", "b", "c");
        Stream<String> stream = createStream(input);

        String[] array = stream.toArray(String[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testToDataset() {
        List<Map<String, Object>> input = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "John");
        row1.put("age", 25);
        input.add(row1);

        Stream<Map<String, Object>> stream = createStream(input);
        Dataset dataset = stream.toDataset();
        assertNotNull(dataset);
        assertEquals(1, dataset.size());
    }

    @Test
    public void testJoin() {
        List<String> input = Arrays.asList("a", "b", "c");
        Stream<String> stream = createStream(input);

        String result = stream.join(",", "[", "]");
        assertEquals("[a,b,c]", result);
    }

    @Test
    public void testHasDuplicates() {
        Stream<Integer> stream1 = createStream(Arrays.asList(1, 2, 3, 2));
        assertTrue(stream1.hasDuplicates());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4));
        assertFalse(stream2.hasDuplicates());
    }

    @Test
    public void testCollectThenApply() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        Integer result = stream.collectThenApply(Collectors.summingInt(Integer::intValue), sum -> sum * 2);
        assertEquals(12, result.intValue());
    }

    @Test
    public void testIndexed() {
        List<String> input = Arrays.asList("a", "b", "c");
        Stream<String> stream = createStream(input);

        List<Indexed<String>> indexed = stream.indexed().toList();
        assertEquals(3, indexed.size());
        assertEquals("a", indexed.get(0).value());
        assertEquals(0, indexed.get(0).index());
        assertEquals("b", indexed.get(1).value());
        assertEquals(1, indexed.get(1).index());
    }

    @Test
    public void testCycled() {
        List<Integer> input = Arrays.asList(1, 2);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.cycled().limit(6).toList();
        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2), result);
    }

    @Test
    public void testCycledWithRounds() {
        List<Integer> input = Arrays.asList(1, 2);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.cycled(3).toList();
        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2), result);
    }

    @Test
    public void testRollup() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        List<List<Integer>> result = stream.rollup().toList();
        assertEquals(4, result.size());
        assertEquals(Collections.emptyList(), result.get(0));
        assertEquals(Arrays.asList(1), result.get(1));
        assertEquals(Arrays.asList(1, 2), result.get(2));
        assertEquals(Arrays.asList(1, 2, 3), result.get(3));
    }

    @Test
    public void testBuffered() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.buffered(2).toList();
        assertEquals(input, result);
    }

    @Test
    public void testAppend() {
        List<Integer> input1 = Arrays.asList(1, 2, 3);
        List<Integer> input2 = Arrays.asList(4, 5);
        Stream<Integer> stream1 = createStream(input1);
        Stream<Integer> stream2 = createStream(input2);

        List<Integer> result = stream1.append(stream2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPrepend() {
        List<Integer> input1 = Arrays.asList(3, 4, 5);
        List<Integer> input2 = Arrays.asList(1, 2);
        Stream<Integer> stream1 = createStream(input1);
        Stream<Integer> stream2 = createStream(input2);

        List<Integer> result = stream1.prepend(stream2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testMergeWith() {
        List<Integer> input1 = Arrays.asList(1, 3, 5);
        List<Integer> input2 = Arrays.asList(2, 4, 6);
        Stream<Integer> stream = createStream(input1);

        List<Integer> result = stream.mergeWith(input2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipWith() {
        List<String> input1 = Arrays.asList("a", "b", "c");
        List<Integer> input2 = Arrays.asList(1, 2, 3);
        Stream<String> stream = createStream(input1);

        List<String> result = stream.zipWith(input2, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testSaveEach() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        stream.saveEach(file).toList();

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(input, lines);
    }

    @Test
    public void testSaveEach_2() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        try (FileWriter writer = new FileWriter(file)) {
            stream.saveEach((e, w) -> IOUtil.write(e, w), writer).toList();
        }

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(input, lines);
    }

    @Test
    public void testPersist() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        long count = stream.persist(file);
        assertEquals(3, count);

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(input, lines);
    }

    @Test
    public void testPersistToCSV() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<TestBean> input = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persistToCSV(file);
        assertEquals(2, count);

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("name"));
        assertTrue(lines.get(0).contains("age"));
    }

    @Test
    public void testPersistToJSON() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<TestBean> input = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persistToJSON(file);
        assertEquals(2, count);

        String json = new String(IOUtil.readAllBytes(file));
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("John"));
        assertTrue(json.contains("Jane"));
    }

    @Test
    public void testCrossJoin() {
        List<String> input1 = Arrays.asList("A", "B");
        List<Integer> input2 = Arrays.asList(1, 2);
        Stream<String> stream = createStream(input1);

        List<Pair<String, Integer>> result = stream.crossJoin(input2).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(Pair.of("A", 1)));
        assertTrue(result.contains(Pair.of("A", 2)));
        assertTrue(result.contains(Pair.of("B", 1)));
        assertTrue(result.contains(Pair.of("B", 2)));
    }

    @Test
    public void testInnerJoin() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"), new TestPerson(3, "Bob"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(2, "Order2"), new TestOrder(1, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.innerJoin(orders, TestPerson::getId, TestOrder::getPersonId).toList();

        assertEquals(3, result.size());
    }

    @Test
    public void testInnerJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"), new TestPerson(3, "Bob"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(2, "Order2"), new TestOrder(1, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.innerJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(3, result.size());
    }

    @Test
    public void testLeftJoin() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"), new TestPerson(3, "Bob"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.leftJoin(orders, TestPerson::getId, TestOrder::getPersonId).toList();

        assertEquals(4, result.size());
    }

    @Test
    public void testLeftJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.leftJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(Pair.of(new TestPerson(1, "John"), null), result.get(0));
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(1));
    }

    @Test
    public void testRightJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.rightJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(0));
        assertEquals(Pair.of(null, new TestOrder(3, "Order3")), result.get(1));
    }

    @Test
    public void testFullJoin() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(3, "Order2"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.fullJoin(orders, TestPerson::getId, TestOrder::getPersonId).toList();

        assertEquals(3, result.size());
    }

    @Test
    public void testfullJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.fullJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(3, result.size());
        assertEquals(Pair.of(new TestPerson(1, "John"), null), result.get(0));
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(1));
        assertEquals(Pair.of(null, new TestOrder(3, "Order3")), result.get(2));
    }

    @Test
    public void testGroupJoin() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, List<TestOrder>>> result = stream.groupJoin(orders, TestPerson::getId, TestOrder::getPersonId).toList();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).right().size());
        assertEquals(1, result.get(1).right().size());
    }

    @Test
    public void testGroupJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, List<TestOrder>>> result = stream.groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).right().size());
        assertEquals(1, result.get(1).right().size());
    }

    @Test
    public void testGroupJoin_3() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, (a, b) -> a, Pair::of)
                .toList();

        assertEquals(2, result.size());
        assertEquals("John", result.get(0).left().getName());
        assertEquals("Order1", result.get(0).right().getOrderName());
        assertEquals("Jane", result.get(1).left().getName());
        assertEquals("Order3", result.get(1).right().getOrderName());

    }

    @Test
    public void testGroupJoin_4() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, List<TestOrder>>> result = stream
                .groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Collectors.toList(), Pair::of)
                .toList();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).right().size());
        assertEquals(1, result.get(1).right().size());
    }

    @Test
    public void testRateLimited() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(2);
        List<Integer> input = Arrays.asList(1, 2, 3, 4);
        Stream<Integer> stream = createStream(input);

        long startTime = System.currentTimeMillis();
        List<Integer> result = stream.rateLimited(rateLimiter).toList();
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(input, result);
        assertTrue(duration >= 1000);
    }

    @Test
    public void testJoinByRange() {
        List<Integer> left = Arrays.asList(1, 5, 10, 15);
        List<Integer> right = Arrays.asList(2, 3, 6, 7, 11, 12);
        Stream<Integer> stream = createStream(left);

        List<Pair<Integer, List<Integer>>> result = stream.joinByRange(right.iterator(), (l, r) -> r > l && r < l + 5).toList();

        assertEquals(4, result.size());
        assertEquals(Arrays.asList(2, 3), result.get(0).right());
        assertEquals(Arrays.asList(6, 7), result.get(1).right());
        assertEquals(Arrays.asList(11, 12), result.get(2).right());
        assertEquals(Collections.emptyList(), result.get(3).right());
    }

    @Test
    public void testJoinByRange_2() {
        List<Integer> left = Arrays.asList(1, 5, 10, 15);
        List<Integer> right = Arrays.asList(2, 3, 6, 7, 11, 12);
        Stream<Integer> stream = createStream(left);

        List<Pair<Integer, List<Integer>>> result = stream.joinByRange(right.iterator(), (l, r) -> r > l && r < l + 5).toList();

        assertEquals(4, result.size());
        assertEquals(Arrays.asList(2, 3), result.get(0).right());
        assertEquals(Arrays.asList(6, 7), result.get(1).right());
        assertEquals(Arrays.asList(11, 12), result.get(2).right());
        assertEquals(Collections.emptyList(), result.get(3).right());
    }

    @Test
    public void testDelay() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        long startTime = System.currentTimeMillis();
        List<Integer> result = stream.delay(Duration.ofMillis(50)).toList();
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(input, result);
        assertTrue(duration >= 150);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestBean {
        private String name;
        private int age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPerson {
        private int id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestOrder {
        private int personId;
        private String orderName;
    }
}
