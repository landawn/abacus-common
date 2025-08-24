package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collector;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Index;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.Splitter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.stream.IntStream.IntStreamEx;
import com.landawn.abacus.util.stream.Stream.StreamEx;

public class MoreStreamTest {
    static final int maxLen = 123;
    static final int repeatNum = 1;

    @Test
    public void test_parallelMerge() {

        final List<Integer> list = N.toList(Array.range(0, 1000_000));
        final List<List<Integer>> lists = N.split(list, 10000);
        N.shuffle(lists);

        final List<Integer> result = Stream.parallelMergeIterables(lists, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 20).toList();

        assertEquals(1000_000, result.size());
        assertEquals(0, result.get(0).intValue());
        assertEquals(1000_000 - 1, result.get(result.size() - 1).intValue());
        assertTrue(N.isSorted(result));
    }

    @Test
    public void test_parallelMerge_2() {

        final List<List<String>> lists = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            final List<String> list = new ArrayList<>();

            for (int j = 0; j < 10000; j++) {
                list.add(Strings.uuid());
            }

            N.sort(list);

            lists.add(list);
        }

        List<String> result = Stream.mergeIterables(lists, (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();

        assertTrue(N.isSorted(result));

        result = Stream.parallelMergeIterables(lists, (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 20).toList();

        assertTrue(N.isSorted(result));

        Profiler.run(1, 10, 3, "mergeIterables",
                () -> Stream.mergeIterables(lists, (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList()).printResult();

        Profiler.run(1, 10, 3, "parallelMergeIterables",
                () -> Stream.parallelMergeIterables(lists, (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 20).toList())
                .printResult();

    }

    @Test
    public void test_index() {
        {
            // Forwards
            final int[] source = { 1, 2, 3, 1, 5, 1 };
            IntStreamEx.ofIndices(source, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).println(); // [0, 3, 5]
            IntStreamEx.ofIndices(source, 1, (a, fromIndex) -> N.indexOf(a, 1, fromIndex)).println(); // [3, 5]

            // Backwards
            IntStreamEx.ofIndices(source, 5, -1, (a, fromIndex) -> N.lastIndexOf(a, 1, fromIndex)).println(); // [5, 3, 0]
            IntStreamEx.ofIndices(source, 4, -1, (a, fromIndex) -> N.lastIndexOf(a, 1, fromIndex)).println(); // [3, 0]
        }

        N.println("==================================");

        {
            // Forwards
            final int[] source = { 1, 2, 3, 1, 2, 1 };
            final int[] targetSubArray = { 1, 2 };

            N.println(N.lastIndexOfSubList(N.asList(1, 2, 3, 1, 2, 1), N.asList(1, 2)));
            N.println(Collections.lastIndexOfSubList(N.asList(1, 2, 3, 1, 2, 1), N.asList(1, 2)));
            N.println("aabba".lastIndexOf("ab", 5));
            IntStreamEx.ofIndices(source, (a, fromIndex) -> Index.ofSubArray(a, fromIndex, targetSubArray, 0, targetSubArray.length).orElse(-1)).println(); // [0, 3]

            // Backwards
            IntStreamEx.ofIndices(source, 5, -1, (a, fromIndex) -> Index.lastOfSubArray(a, fromIndex, targetSubArray, 0, targetSubArray.length).orElse(-1))
                    .println(); // [3, 0]
        }

        N.println("==================================");

        {
            IntStreamEx.ofIndices(5).println();
            IntStreamEx.ofIndices(5, 1).println();
            IntStreamEx.ofIndices(5, 2).println();
            IntStreamEx.ofIndices(5, -1).println();
            IntStreamEx.ofIndices(5, -2).println();
            IntStreamEx.ofIndices(0).println();
            IntStreamEx.ofIndices(0, 1).println();
            IntStreamEx.ofIndices(0, -1).println();
            IntStreamEx.ofIndices(0, -2).println();
        }
    }

    @Test
    public void test_lazy_2() {

        IntStreamEx.of(1, 2, 3).reverseSorted();
        Stream.of(1, 2, 3).flatMap(i -> Stream.of(i - 1, i, i + 1)).flatMap(i -> Stream.of(i - 1, i, i + 1)).filter(i -> {
            System.out.println(i);
            return true;
        }).first();

        N.println("====================================================");

        java.util.stream.Stream.of(1, 2, 3)
                .flatMap(i -> java.util.stream.Stream.of(i - 1, i, i + 1))
                .flatMap(i -> java.util.stream.Stream.of(i - 1, i, i + 1))
                .filter(i -> {
                    System.out.println(i);
                    return true;
                })
                .findFirst();
    }

    @Test
    public void test_sliding_5() {
        //        IntStream.range(0, 30).boxed().sliding(3, 5).map(s -> s.join(", ")).forEach(Fn.println());
        //        N.println(Strings.repeat('=', 80));
        //        IntStream.range(0, 30).boxed().sliding(3, 5).skip(3).map(s -> s.join(", ")).forEach(Fn.println());

        N.println(Strings.repeat('=', 80));
        N.println(com.google.common.base.Splitter.on(',').split(" foo,,,  bar ,"));
        assertEquals("[ foo, , ,   bar , ]", Splitter.with(",").splitToStream(" foo,,,  bar ,").join(", ", "[", "]"));

        N.println(com.google.common.base.Splitter.on(',').splitToList("").size());
        N.println(Splitter.with(",").split("").size());
        N.println(Splitter.with(",").split(null).size());

    }

    @Test
    public void test_merge_2() {
        IntStream.range(0, 100)
                .shuffled()
                .mergeWith(IntStream.range(100, 200).shuffled(), (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .sorted()
                .last()
                .ifPresent(N::println);

        //        assertEquals(
        //                1000, Stream
        //                        .merge(IntStream.rangeClosed(0, 1000).shuffled().boxed().split(10).toList(),
        //                                (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
        //                        .sorted()
        //                        .last()
        //                        .get()
        //                        .intValue());
        //
        //        assertEquals(
        //                1000, Stream
        //                        .parallelMerge(IntStream.rangeClosed(0, 1000).shuffled().boxed().split(10).toList(),
        //                                (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
        //                        .sorted()
        //                        .last()
        //                        .get()
        //                        .intValue());
    }

    @Test
    public void test_queue() {
        IntStream.range(0, 10000).boxed().buffered().limit(10).println();
    }

    @Test
    public void test_collect_2() {
        Stream.of(1, 2, 3).collect(Collectors.groupingByConcurrent(Fn.identity())).forEach(Fn.println("="));

        Stream.of(1, 2, 3).collect(java.util.stream.Collectors.groupingByConcurrent(Fn.identity())).forEach(Fn.println("="));
    }
    @Test
    public void test_flattmapToObj() {
        StreamEx.of(1, 2, 3).println();
        IntStream.of(1, 2, 3).mapToObj(i -> N.repeat("a", i)).println();
        IntStream.of(1, 2, 3).mapToObj(i -> Array.repeat("a", i)).println();
        IntStream.of(1, 2, 3).flatmapToObj(i -> N.repeat("a", i)).println();
        IntStream.of(1, 2, 3).flattmapToObj(i -> Array.repeat("a", i)).println();

        IntStream.range(0, 100).map(i -> i % 11).boxed().groupBy(Fn.identity(), i -> i + "a").println();
    }

    @Test
    public void test_scan() {
        Stream.of(1, 2, 3).prepend(0).scan((e, r) -> e + r).println();

        assertTrue(N.equals(Stream.of(1, 2, 3).scan((e, r) -> e + r).toList(), N.asList(1, 3, 6)));
        assertTrue(N.equals(Stream.of(1, 2, 3).prepend(0).scan((e, r) -> e + r).toList(), N.asList(0, 1, 3, 6)));
        assertTrue(N.equals(Stream.of(1, 2, 3).scan("a", true, (e, r) -> e + r).toList(), N.asList("a", "a1", "a12", "a123")));
        assertTrue(N.equals(IntStream.of(1, 2, 3).scan(1, true, (e, r) -> e + r).toList(), N.asList(1, 2, 4, 7)));

    }

    @Test
    public void test_collectors() {
        final Function<Integer, Integer> keyExtractor = i -> {
            N.sleep(100);
            return i;
        };

        long startTime = System.currentTimeMillis();
        //        IntList.range(0, 128).toList().stream().parallel().collect(java.util.stream.Collectors.toMap(keyExtractor, keyExtractor));
        //        System.out.println("=========: " + (System.currentTimeMillis() - startTime));
        //
        //        startTime = System.currentTimeMillis();
        //        IntList.range(0, 128).toList().stream().parallel().collect(java.util.stream.Collectors.toConcurrentMap(keyExtractor, keyExtractor));
        //        System.out.println("=========: " + (System.currentTimeMillis() - startTime));
        //
        //        startTime = System.currentTimeMillis();
        //        IntList.range(0, 128).stream().boxed().parallel().collect(Collectors.toMap(keyExtractor, keyExtractor));
        //        System.out.println("=========: " + (System.currentTimeMillis() - startTime));
        //
        //        startTime = System.currentTimeMillis();
        //        IntList.range(0, 128).stream().boxed().parallel(64).collect(Collectors.toMap(keyExtractor, keyExtractor));
        //        System.out.println("=========: " + (System.currentTimeMillis() - startTime));
        //
        //        startTime = System.currentTimeMillis();
        //        IntList.range(0, 128).stream().boxed().parallel().collect(java.util.stream.Collectors.toConcurrentMap(keyExtractor, keyExtractor));
        //        System.out.println("=========: " + (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();
        IntList.range(0, 256).stream().boxed().parallel(64).collect(java.util.stream.Collectors.toConcurrentMap(keyExtractor, keyExtractor));
        System.out.println("=========: " + (System.currentTimeMillis() - startTime));
    }

    @Test
    public void test_sort_2() {
        IntStream.range(0, 100)
                .boxed()
                .onClose(() -> N.println("close1"))
                .sorted()
                .peekFirst(e -> N.println("peek: " + e))
                .onClose(() -> N.println("close2"))
                .peekLast(e -> N.println("peek2: " + e))
                .count();

        N.println(Strings.repeat("=", 90));

        IntStream.range(0, 100)
                .boxed()
                .onClose(() -> N.println("close1"))
                .shuffled()
                .peekFirst(e -> N.println("peek: " + e))
                .onClose(() -> N.println("close2"))
                .peekLast(e -> N.println("peek2: " + e))
                .count();

        N.println(Strings.repeat("=", 90));

        IntStream.range(0, 100)
                .boxed()
                .onClose(() -> N.println("close1"))
                .rotated(3)
                .peekFirst(e -> N.println("peek: " + e))
                .onClose(() -> N.println("close2"))
                .peekLast(e -> N.println("peek2: " + e))
                .count();
    }

    @Test
    public void test_last_2() {
        IntStream.range(0, 10000)
                .boxed()
                .onClose(() -> N.println("close1"))
                .takeLast(10)
                .peekFirst(e -> N.println("peek: " + e))
                .onClose(() -> N.println("close2"))
                .peekLast(e -> N.println("peek2: " + e))
                .count();
    }

    @Test
    public void test_last() {
        IntStream.range(0, 10000).boxed().takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).boxed().sorted().takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).boxed().rotated(3).takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).boxed().shuffled().takeLast(10).forEach(Fn.println());

        IntStream.range(0, 10000).parallel().boxed().takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).parallel().boxed().sorted().takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).parallel().boxed().rotated(3).takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).parallel().boxed().shuffled().takeLast(10).forEach(Fn.println());

        IntStream.range(0, 10000).boxed().parallel().takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).boxed().parallel().sorted().takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).boxed().parallel().rotated(3).takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).boxed().parallel().shuffled().takeLast(10).forEach(Fn.println());

        IntStream.range(0, 10000).boxed().takeLast(10).parallel().forEach(Fn.println());
        IntStream.range(0, 10000).boxed().sorted().parallel().takeLast(10).forEach(Fn.println());
        IntStream.range(0, 10000).boxed().rotated(3).takeLast(10).parallel().forEach(Fn.println());
        IntStream.range(0, 10000).boxed().shuffled().takeLast(10).parallel().forEach(Fn.println());
    }

    @Test
    public void test_lines() {
        final File file = IOUtil.listFiles(new File("./lib/"), true, true).get(0);

        for (int i = 0; i < 1000000; i++) {
            Stream.ofLines(file).filter(s -> !s.startsWith("aa")).skip(10);
        }

        N.println(Stream.ofLines(file).filter(s -> !s.startsWith("aa")).skip(10).count());
    }

    @Test
    public void test_lines_1() throws IOException {
        final File file = IOUtil.listFiles(new File("./lib/"), true, true).get(0);

        for (int i = 0; i < 1000000; i++) {
            Seq.ofLines(file).filter(s -> !s.startsWith("aa")).skip(10);
        }

        N.println(Seq.ofLines(file).skip(10).count());
    }

    @Test
    public void test_lines_2() throws Exception {
        final Path path = IOUtil.listFiles(new File("./lib/"), true, true).get(0).toPath();

        for (int i = 0; i < 1000000; i++) {
            Files.lines(path).filter(s -> !s.startsWith("aa")).skip(10);
        }

        N.println(Files.lines(path).filter(s -> !s.startsWith("aa")).skip(10).count());
    }

    @Test
    public void test_rangeClosedBy() {
        N.println(IntStream.range(Integer.MAX_VALUE, 1, -1).limit(3).join(", "));
        N.println(IntStream.rangeClosed(Integer.MAX_VALUE, 1, -1).limit(3).join(", "));
    }

    @Test
    public void test_sliding() {

        N.println(Stream.of(1, 2, 3, 5).slidingMap((i, j) -> i <= j).allMatch(e -> e));
        N.println(Stream.of(1, 2, 5, 3).slidingMap((i, j) -> i <= j).allMatch(e -> e));

        N.println(Strings.repeat("=", 80));
        IntStream.range(1, 10).boxed().split(3, IntFunctions.ofQueue()).forEach(Fn.println());

        N.println(Strings.repeat("=", 80));
        IntStream.range(1, 10).boxed().split(i -> i % 3 == 0, Suppliers.ofList()).forEach(Fn.println());

        N.println(Strings.repeat("=", 80));
        IntStream.range(1, 10).boxed().sliding(3, IntFunctions.ofQueue()).forEach(Fn.println());

        N.println(Strings.repeat("=", 80));
        IntStream.range(1, 10).boxed().sliding(3, 3, IntFunctions.ofQueue()).forEach(Fn.println());

        N.println(Strings.repeat("=", 80));
        IntStream.range(1, 10).boxed().sliding(3, 4, IntFunctions.ofQueue()).forEach(Fn.println());
    }

    //    @Test
    //    public void test_sliding_2() {
    //        IntStream.range(0, 10).slidingToList(4).skip(0).println();
    //        IntStream.range(0, 10).slidingToList(4).skip(1).println();
    //        IntStream.range(0, 10).slidingToList(4).skip(2).println();
    //        IntStream.range(0, 10).slidingToList(4).skip(3).println();
    //        IntStream.range(0, 10).slidingToList(4).skip(7).println();
    //        IntStream.range(0, 10).slidingToList(4).skip(8).println();
    //        IntStream.range(0, 10).slidingToList(4).skip(9).println();
    //        IntStream.range(0, 10).slidingToList(4).skip(10).println();
    //        IntStream.range(0, 10).slidingToList(4).skip(11).println();
    //
    //        N.println(Strings.repeat("=", 80));
    //
    //        IntStream.range(0, 10).sliding(4, 2).skip(0).println();
    //        IntStream.range(0, 10).sliding(4, 2).skip(1).println();
    //        IntStream.range(0, 10).sliding(4, 2).skip(2).println();
    //        IntStream.range(0, 10).sliding(4, 2).skip(3).println();
    //        IntStream.range(0, 10).sliding(4, 2).skip(7).println();
    //        IntStream.range(0, 10).sliding(4, 2).skip(8).println();
    //        IntStream.range(0, 10).sliding(4, 2).skip(9).println();
    //        IntStream.range(0, 10).sliding(4, 2).skip(10).println();
    //        IntStream.range(0, 10).sliding(4, 2).skip(11).println();
    //
    //        N.println(Strings.repeat("=", 80));
    //
    //        IntStream.range(0, 10).sliding(4, 3).skip(0).println();
    //        IntStream.range(0, 10).sliding(4, 3).skip(1).println();
    //        IntStream.range(0, 10).sliding(4, 3).skip(2).println();
    //        IntStream.range(0, 10).sliding(4, 3).skip(3).println();
    //        IntStream.range(0, 10).sliding(4, 3).skip(7).println();
    //        IntStream.range(0, 10).sliding(4, 3).skip(8).println();
    //        IntStream.range(0, 10).sliding(4, 3).skip(9).println();
    //        IntStream.range(0, 10).sliding(4, 3).skip(10).println();
    //        IntStream.range(0, 10).sliding(4, 3).skip(11).println();
    //
    //        N.println(Strings.repeat("=", 80));
    //
    //        IntStream.range(0, 10).sliding(4, 4).skip(0).println();
    //        IntStream.range(0, 10).sliding(4, 4).skip(1).println();
    //        IntStream.range(0, 10).sliding(4, 4).skip(2).println();
    //        IntStream.range(0, 10).sliding(4, 4).skip(3).println();
    //        IntStream.range(0, 10).sliding(4, 4).skip(7).println();
    //        IntStream.range(0, 10).sliding(4, 4).skip(8).println();
    //        IntStream.range(0, 10).sliding(4, 4).skip(9).println();
    //        IntStream.range(0, 10).sliding(4, 4).skip(10).println();
    //        IntStream.range(0, 10).sliding(4, 4).skip(11).println();
    //    }

    @Test
    public void test_sliding_3() {
        //        IntStream.range(0, 10).boxed().slidingToList(4).skip(0).println();
        //        IntStream.range(0, 10).boxed().slidingToList(4).skip(1).println();
        //        IntStream.range(0, 10).boxed().slidingToList(4).skip(2).println();
        //        IntStream.range(0, 10).boxed().slidingToList(4).skip(7).println();
        //        IntStream.range(0, 10).boxed().slidingToList(4).skip(8).println();
        //        IntStream.range(0, 10).boxed().slidingToList(4).skip(9).println();
        //        IntStream.range(0, 10).boxed().slidingToList(4).skip(10).println();
        //        IntStream.range(0, 10).boxed().slidingToList(4).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().sliding(4, 2).skip(0).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).skip(1).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).skip(2).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).skip(7).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).skip(8).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).skip(9).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).skip(10).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().sliding(4, 3).skip(0).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).skip(1).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).skip(2).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).skip(7).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).skip(8).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).skip(9).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).skip(10).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().sliding(4, 4).skip(0).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).skip(1).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).skip(2).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).skip(7).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).skip(8).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).skip(9).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).skip(10).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).skip(11).println();
    }

    @Test
    public void test_sliding_4() {
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(0).println();
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(1).println();
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(2).println();
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(3).println();
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(7).println();
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(8).println();
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(9).println();
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(10).println();
        IntStream.range(0, 10).boxed().sliding(4, IntFunctions.ofSet()).mapFirst(Fn.<Set<Integer>> identity()).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().sliding(4, 2).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
        IntStream.range(0, 10).boxed().sliding(4, 2).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().sliding(4, 3).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
        IntStream.range(0, 10).boxed().sliding(4, 3).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().sliding(4, 4).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
        IntStream.range(0, 10).boxed().sliding(4, 4).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();
    }

    @Test
    public void test_peekFirstLast() {
        IntStream.range(1, 10).boxed().mapFirst(i -> i + 10).mapLast(i -> i + 100).peekFirst(Fn.println()).peekLast(Fn.println()).println();

        Stream.of("a", "b", null, "c", "a").distinct().println();

        // Stream.of("a", "b", null, "c", "a").distinctBy(Fn.identity(), e -> e.getValue() > 0).println();
    }

    @Test
    public void test_partitionBy() {
        Stream.of(N.EMPTY_BYTE_ARRAY).partitionBy(i -> i % 2 == 0).forEach(Fn.println());
    }

    @Test
    public void test_top() {

        IntStream.random(1, 100).limit(10).mapToObj(i -> i).peek(Fn.println()).top(5).println();
        IntStream.random(1, 100).limit(10).onEach(N::println).top(5).println();

    }

    @Test
    public void test_first_last() {
        if (null instanceof String) {
            N.println("ok");
        }

        Stream.of(1, 2, 3).filter(i -> i < 2).collect(Collectors.onlyOne()).ifPresent(Fn.println());
        Stream.of(1, 2, 3).collect(Collectors.onlyOne(i -> i < 2)).ifPresent(Fn.println());

        Stream.of(1, 2, 3).filter(i -> i < 1).collect(Collectors.onlyOne()).ifPresent(Fn.println());
        Stream.of(1, 2, 3).collect(Collectors.onlyOne(i -> i < 1)).ifPresent(Fn.println());

        try {
            Stream.of(1, 2, 3).filter(i -> i < 3).collect(Collectors.onlyOne()).ifPresent(Fn.println());
            fail("should throw TooManyElementsException");
        } catch (final TooManyElementsException e) {
        }

        try {
            Stream.of(1, 2, 3).collect(Collectors.onlyOne(i -> i < 3)).ifPresent(Fn.println());
            fail("should throw TooManyElementsException");
        } catch (final TooManyElementsException e) {
        }

        N.println(Strings.repeat("=", 80));

        Stream.of(1, 2, 3).collect(Collectors.first()).ifPresent(Fn.println());
        Stream.of(1, 2, 3).collect(Collectors.last()).ifPresent(Fn.println());

        N.println(Strings.repeat("=", 80));

        Stream.of(1, 2, 3).collect(Collectors.first(2)).forEach(Fn.println());
        Stream.of(1, 2, 3).collect(Collectors.last(2)).forEach(Fn.println());

        N.println(Strings.repeat("=", 80));

        Stream.of(1, 2, 3).collect(Collectors.first(100)).forEach(Fn.println());
        Stream.of(1, 2, 3).collect(Collectors.last(100)).forEach(Fn.println());

        final java.util.stream.Stream<String> input = java.util.stream.Stream.of("1", "1.0f", "2f");

        final float[] a = Stream.from(input).mapToFloat(Float::parseFloat).toArray();

        N.println(a);
    }

    @Test
    public void test_step() {
        IntStreamEx.range(0, 10).step(2).skip(2).forEach(N::println);
        IntStreamEx.of(Array.range(0, 10)).step(2).skip(2).forEach(N::println);

        IntStreamEx.range(0, 10).step(3).skip(2).forEach(N::println);
        IntStreamEx.of(Array.range(0, 10)).step(3).skip(2).forEach(N::println);

        final Collector<Integer, ?, List<Integer>> collector = Collectors.toCollection(Suppliers.<Integer> ofLinkedList());
        Stream.of(1, 2, 3).groupTo(Fn.identity(), collector);
    }

    @Test
    public void test_fn_f() {
        Stream.of(1, 2, 3).filter(Fn.p((final Integer e) -> e > 1).negate()).println();
    }

    @Test
    public void test_Multimap() {
        final ListMultimap<String, Integer> listMultimap = ListMultimap.of("a", 1, "b", 2, "a", 2, "a", 2); // -> {a=[1, 2, 2], b=[2]}
        listMultimap.removeAll(N.asMap("a", 2)); // -> {a=[1, 2], b=[2]}
        N.println(listMultimap);

        final SetMultimap<String, Integer> setMultimap = SetMultimap.of("a", 1, "b", 2, "a", 2); // -> {a=[1, 2, 2], b=[2]}
        setMultimap.removeAll(N.asMap("a", 2)); // -> {a=[1], b=[2]}
        N.println(setMultimap);
    }

    @Test
    public void test_BigInteger() {
        final BigInteger sum = Stream.of(BigInteger.valueOf(1), BigInteger.valueOf(2)).collect(Collectors.summingBigInteger(Fn.identity()));
        N.println(sum);

        final BigDecimal avg = Stream.of(BigInteger.valueOf(1), BigInteger.valueOf(2)).collect(Collectors.averagingBigIntegerOrElseThrow(Fn.identity()));
        N.println(avg);

    }

    @Test
    public void test_001() {
        IntIterator.of(Array.of(1, 2, 3)).foreachRemaining((final int i) -> N.println(i));
    }

    @Test
    public void test_combineCollectors() {

        final Tuple2<List<String>, Set<String>> res = Stream.of("a", "b", "c", "a", "d")
                .collect(MoreCollectors.combine(Collectors.toList(), Collectors.toSet(), Tuple::of));

        res.forEach(Fn.println());

        final Tuple3<List<String>, Set<String>, LongSummaryStatistics> res2 = Stream.of("a", "b", "c", "a", "d")
                .collect(MoreCollectors.combine(Collectors.toList(), Collectors.toSet(), Collectors.summarizingLong(String::hashCode), Tuple::of));

        res2.forEach(Fn.println());

        final Tuple4<List<String>, Set<String>, OptionalDouble, LongSummaryStatistics> res3 = Stream.of("a", "b", "c", "a", "d")
                .collect(MoreCollectors.combine(Collectors.toList(), Collectors.toSet(), Collectors.averagingDoubleOrEmpty(String::hashCode),
                        Collectors.summarizingLong(String::hashCode), Tuple::of));

        res3.forEach(Fn.println());

    }

    @Test
    public void test_rangeMap() {
        Stream.of(1).rangeMap((l, r) -> r - l < 2, (l, r) -> l + "->" + r).forEach(Fn.println());
    }

    @Test
    public void test_perf() {
        Profiler.run(1, 3, 3, "sequential", () -> IntStream.range(0, 1000_000).filter(i -> {
            if (i % 1000 == 0) {
                N.sleep(1);
            }
            return i > -1;
        }).sum()).printResult();

        Profiler.run(1, 3, 3, "parallel", () -> IntStream.range(0, 1000_000).parallel(8).filter(i -> {
            if (i % 1000 == 0) {
                N.sleep(1);
            }
            return i > -1;
        }).sum()).printResult();
    }

    @Test
    public void test_parallel() {
        final Function<Integer, Integer> mapper = i -> {
            N.sleep(10);
            return i * 2;
        };

        final Predicate<Integer> filter = i -> {
            N.sleep(10);
            return i % 2 == 0;
        };

        IntStream.range(0, 10_000)
                .boxed()
                .parallel(1024)
                .filter(filter)
                .map(mapper)
                .filter(filter)
                .map(mapper)
                .filter(filter)
                .map(mapper)
                .filter(filter)
                .map(mapper)
                .filter(filter)
                .map(mapper)
                .filter(filter)
                .map(mapper)
                .filter(filter)
                .filter(filter)
                .forEach(i -> N.println(Thread.currentThread().getName() + ": " + i));
    }

    @Test
    public void test_filter() {
        for (int r = 0; r < repeatNum; r++) {
            for (int l = 0; l < maxLen; l++) {
                final int[] a = IntStream.random().limit(l).toArray();
                final int[] a2 = IntStream.of(a).sorted().toArray();

                {
                    int[] b = IntStream.of(a).filter(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(a).parallel().filter(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    //    b = IntStream.of(a).parallel(Splitor.ARRAY).filter(i -> i < a2[a2.length / 2]).toArray();
                    //    assertEquals(a2.length / 2, b.length);
                    //    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a)).filter(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a)).parallel().filter(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }

                {
                    Integer[] b = Stream.of(a).filter(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(a).parallel().filter(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    //    b = Stream.of(a).parallel(Splitor.ARRAY).filter(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    //    assertEquals(a2.length / 2, b.length);
                    //    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator()).filter(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator()).parallel().filter(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }
            }
        }
    }

    @Test
    public void test_takeWhile() {
        for (int r = 0; r < repeatNum; r++) {
            for (int l = 0; l < maxLen; l++) {
                final int[] a = IntStream.random().limit(l).toArray();
                final int[] a2 = IntStream.of(a).sorted().toArray();

                {
                    int[] b = IntStream.of(a2).takeWhile(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(a2).parallel().takeWhile(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a2)).takeWhile(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a2)).parallel().takeWhile(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }

                {
                    Integer[] b = Stream.of(a2).takeWhile(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(a2).parallel().takeWhile(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a2).iterator()).takeWhile(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a2).iterator()).parallel().takeWhile(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }
            }
        }
    }

    @Test
    public void test_dropWhile() {
        for (int r = 0; r < repeatNum; r++) {
            for (int l = 0; l < maxLen; l++) {
                final int[] a = IntStream.random().limit(l).toArray();
                final int[] a2 = IntStream.of(a).sorted().toArray();

                {
                    int[] b = IntStream.of(a2).dropWhile(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length - a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i >= a2[a2.length / 2]));

                    b = IntStream.of(a2).parallel().dropWhile(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length - a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i >= a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a2)).dropWhile(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length - a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i >= a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a2)).parallel().dropWhile(i -> i < a2[a2.length / 2]).toArray();
                    assertEquals(a2.length - a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i >= a2[a2.length / 2]));
                }

                {
                    Integer[] b = Stream.of(a2).dropWhile(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length - a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i >= a2[a2.length / 2]));

                    b = Stream.of(a2).parallel().dropWhile(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length - a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i >= a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a2).iterator()).dropWhile(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length - a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i >= a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a2).iterator()).parallel().dropWhile(i -> i < a2[a2.length / 2]).toArray(Integer[]::new);
                    assertEquals(a2.length - a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i >= a2[a2.length / 2]));
                }
            }
        }
    }

    @Test
    public void test_map() {
        for (int r = 0; r < repeatNum; r++) {
            for (int l = 0; l < maxLen; l++) {
                final int[] a = IntStream.random().limit(l).toArray();
                final int[] a2 = IntStream.of(a).sorted().toArray();

                {
                    int[] b = IntStream.of(a).map(i -> i).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(a).parallel().map(i -> i).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a)).map(i -> i).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a)).parallel().map(i -> i).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }

                {
                    Integer[] b = Stream.of(a).map(i -> i).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(a).parallel().map(i -> i).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator()).map(i -> i).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator()).parallel().map(i -> i).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }

                {
                    Integer[] b = Stream.of(a).map(i -> i).filter(i -> i < a2[a2.length / 2]).mapToLong(i -> i).mapToObj(i -> (int) i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(a).parallel().map(i -> i).filter(i -> i < a2[a2.length / 2]).mapToLong(i -> i).mapToObj(i -> (int) i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator())
                            .map(i -> i)
                            .filter(i -> i < a2[a2.length / 2])
                            .mapToLong(i -> i)
                            .mapToObj(i -> (int) i)
                            .toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator())
                            .parallel()
                            .map(i -> i)
                            .filter(i -> i < a2[a2.length / 2])
                            .mapToLong(i -> i)
                            .mapToObj(i -> (int) i)
                            .toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }
            }
        }
    }

    @Test
    public void test_flatMap() {
        for (int r = 0; r < repeatNum; r++) {
            for (int l = 0; l < maxLen; l++) {
                final int[] a = IntStream.random().limit(l).toArray();
                final int[] a2 = IntStream.of(a).sorted().toArray();

                {
                    int[] b = IntStream.of(a).flatMap(IntStream::of).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(a).parallel().flatMap(IntStream::of).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a)).flatMap(IntStream::of).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a)).parallel().flatMap(IntStream::of).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray();
                    assertEquals(a2.length / 2, b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }

                {
                    Integer[] b = Stream.of(a).flatMap(Stream::of).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(a).parallel().flatMap(Stream::of).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator()).flatMap(Stream::of).filter(i -> i < a2[a2.length / 2]).map(i -> i).toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator())
                            .parallel()
                            .flatMap(Stream::of)
                            .filter(i -> i < a2[a2.length / 2])
                            .map(i -> i)
                            .toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }

                {
                    Integer[] b = Stream.of(a)
                            .flatMap(Stream::of)
                            .filter(i -> i < a2[a2.length / 2])
                            .flatMapToLong(LongStream::of)
                            .flatMapToObj(i -> Stream.of((int) i))
                            .toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(a)
                            .parallel()
                            .flatMap(Stream::of)
                            .filter(i -> i < a2[a2.length / 2])
                            .flatMapToLong(LongStream::of)
                            .flatMapToObj(i -> Stream.of((int) i))
                            .toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator())
                            .flatMap(Stream::of)
                            .filter(i -> i < a2[a2.length / 2])
                            .flatMapToLong(LongStream::of)
                            .flatMapToObj(i -> Stream.of((int) i))
                            .toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator())
                            .parallel()
                            .flatMap(Stream::of)
                            .filter(i -> i < a2[a2.length / 2])
                            .flatMapToLong(LongStream::of)
                            .flatMapToObj(i -> Stream.of((int) i))
                            .toArray(Integer[]::new);
                    assertEquals(a2.length / 2, b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }
            }
        }
    }

    @Test
    public void test_skip_limit() {
        for (int r = 0; r < repeatNum; r++) {
            for (int l = 0; l < maxLen; l++) {
                final int[] a = IntStream.random().limit(l).toArray();
                final int[] a2 = IntStream.of(a).sorted().toArray();
                {
                    int[] b = IntStream.of(a).filter(i -> i < a2[a2.length / 2]).skip(N.max(a2.length / 2 - 3, 3)).limit(3).toArray();
                    assertEquals(N.max(0, N.min(a2.length / 2 - 3, 3)), b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(a).parallel().filter(i -> i < a2[a2.length / 2]).skip(N.max(a2.length / 2 - 3, 3)).limit(3).toArray();
                    assertEquals(N.max(0, N.min(a2.length / 2 - 3, 3)), b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a)).filter(i -> i < a2[a2.length / 2]).skip(N.max(a2.length / 2 - 3, 3)).limit(3).toArray();
                    assertEquals(N.max(0, N.min(a2.length / 2 - 3, 3)), b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = IntStream.of(IntIterator.of(a)).parallel().filter(i -> i < a2[a2.length / 2]).skip(N.max(a2.length / 2 - 3, 3)).limit(3).toArray();
                    assertEquals(N.max(0, N.min(a2.length / 2 - 3, 3)), b.length);
                    IntStream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }

                {
                    Integer[] b = Stream.of(a).filter(i -> i < a2[a2.length / 2]).skip(N.max(a2.length / 2 - 3, 3)).limit(3).toArray(Integer[]::new);
                    assertEquals(N.max(0, N.min(a2.length / 2 - 3, 3)), b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(a).parallel().filter(i -> i < a2[a2.length / 2]).skip(N.max(a2.length / 2 - 3, 3)).limit(3).toArray(Integer[]::new);
                    assertEquals(N.max(0, N.min(a2.length / 2 - 3, 3)), b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator())
                            .filter(i -> i < a2[a2.length / 2])
                            .skip(N.max(a2.length / 2 - 3, 3))
                            .limit(3)
                            .toArray(Integer[]::new);
                    assertEquals(N.max(0, N.min(a2.length / 2 - 3, 3)), b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));

                    b = Stream.of(IntStream.of(a).iterator())
                            .parallel()
                            .filter(i -> i < a2[a2.length / 2])
                            .skip(N.max(a2.length / 2 - 3, 3))
                            .limit(3)
                            .toArray(Integer[]::new);
                    assertEquals(N.max(0, N.min(a2.length / 2 - 3, 3)), b.length);
                    Stream.of(b).forEach(i -> assertTrue(i < a2[a2.length / 2]));
                }
            }
        }
    }

    @Test
    public void test_append_prepend() {
        for (int r = 0; r < repeatNum; r++) {
            assertEquals("a, b, c, 1, 2, 3", CharStream.of('a', 'b', 'c').append(CharStream.of('1', '2', '3')).join(", "));
            assertEquals("1, 2, 3, a, b, c", CharStream.of('a', 'b', 'c').prepend(CharStream.of('1', '2', '3')).join(", "));

            assertEquals("a, b, c, 1, 2, 3", CharStream.of('a', 'b', 'c').parallel().append(CharStream.of('1', '2', '3')).sequential().join(", "));
            assertEquals("1, 2, 3, a, b, c", CharStream.of('a', 'b', 'c').parallel().prepend(CharStream.of('1', '2', '3')).sequential().join(", "));

            assertEquals("a, b, c, 1, 2, 3", Stream.of('a', 'b', 'c').append(Stream.of('1', '2', '3')).join(", "));
            assertEquals("1, 2, 3, a, b, c", Stream.of('a', 'b', 'c').prepend(Stream.of('1', '2', '3')).join(", "));

            assertEquals("a, b, c, 1, 2, 3", Stream.of('a', 'b', 'c').parallel().append(Stream.of('1', '2', '3')).sequential().join(", "));
            assertEquals("1, 2, 3, a, b, c", Stream.of('a', 'b', 'c').parallel().prepend(Stream.of('1', '2', '3')).sequential().join(", "));
        }
    }

    @Test
    public void test_reverse_rotate_shuffled() {

        for (int r = 0; r < repeatNum; r++) {
            assertEquals("5, 4, 3, 2, 1", IntStream.of(1, 2, 3, 4, 5).reversed().join(", "));
            assertEquals("5, 4, 3, 2, 1", IntStream.of(1, 2, 3, 4, 5).map(i -> i).parallel().reversed().sequential().join(", "));

            assertEquals("5, 4, 3, 2, 1", Stream.of(1, 2, 3, 4, 5).reversed().join(", "));
            assertEquals("5, 4, 3, 2, 1", Stream.of(1, 2, 3, 4, 5).map(i -> i).parallel().reversed().sequential().join(", "));

            assertEquals("5, 4, 3, 2, 1", IntStream.of(IntStream.of(1, 2, 3, 4, 5).iterator()).reversed().join(", "));
            assertEquals("5, 4, 3, 2, 1", IntStream.of(IntStream.of(1, 2, 3, 4, 5).iterator()).map(i -> i).parallel().reversed().sequential().join(", "));

            assertEquals("5, 4, 3, 2, 1", Stream.of(Stream.of(1, 2, 3, 4, 5).iterator()).reversed().join(", "));
            assertEquals("5, 4, 3, 2, 1", Stream.of(Stream.of(1, 2, 3, 4, 5).iterator()).map(i -> i).parallel().reversed().sequential().join(", "));

            assertEquals("4, 5, 1, 2, 3", IntStream.of(1, 2, 3, 4, 5).rotated(2).join(", "));
            assertEquals("4, 5, 1, 2, 3", IntStream.of(1, 2, 3, 4, 5).map(i -> i).parallel().rotated(2).sequential().join(", "));

            assertEquals("4, 5, 1, 2, 3", Stream.of(1, 2, 3, 4, 5).rotated(2).join(", "));
            assertEquals("4, 5, 1, 2, 3", Stream.of(1, 2, 3, 4, 5).map(i -> i).parallel().rotated(2).sequential().join(", "));

            assertFalse("1, 2, 3, 4, 5".equals(IntStream.of(1, 2, 3, 4, 5).shuffled().onEach(N::println).join(", ")));
            assertFalse("1, 2, 3, 4, 5".equals(IntStream.of(1, 2, 3, 4, 5).parallel().shuffled().onEach(N::println).sequential().join(", ")));

            assertFalse("1, 2, 3, 4, 5".equals(Stream.of(1, 2, 3, 4, 5).shuffled().onEach(N::println).join(", ")));
            assertFalse("1, 2, 3, 4, 5".equals(Stream.of(1, 2, 3, 4, 5).parallel().shuffled().onEach(N::println).sequential().join(", ")));
        }
    }

    @Test
    public void test_merge() {
        for (int r = 0; r < repeatNum; r++) {
            for (int l = 0; l < maxLen; l++) {
                final int[] a = Array.rangeClosed(0, maxLen);
                final IntList[] b = new IntList[5];

                for (int i = 0; i < b.length; i++) {
                    b[i] = new IntList();
                }

                for (int i = 0; i < a.length; i++) {
                    b[a[i] % 5].add(i);
                }

                int[] c = IntStream
                        .merge(b[0].trimToSize().array(), b[1].trimToSize().array(), (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray();

                for (int i = 1; i < c.length; i++) {
                    assertTrue(c[i] > c[i - 1]);
                }

                assertTrue(N.equals(a,
                        IntStream
                                .merge(Stream.of(b).map(i -> IntStream.of(i.trimToSize().array())).toList(),
                                        (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                                .toArray()));

                //    assertTrue(N.equals(a,
                //            IntStream
                //                    .parallelMerge(Stream.of(b).map(i -> IntStream.of(i.trimToSize().array())).toList(),
                //                            (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                //                    .toArray()));

                c = Stream.merge(b[0].trimToSize().toList(), b[1].trimToSize().toList(), (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .mapToInt(i -> i)
                        .toArray();

                for (int i = 1; i < c.length; i++) {
                    assertTrue(c[i] > c[i - 1]);
                }

                assertTrue(N.equals(
                        a, Stream
                                .mergeIterators(Stream.of(b).map(i -> i.toList().iterator()).toList(),
                                        (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                                .mapToInt(i -> i)
                                .toArray()));

                assertTrue(N.equals(
                        a, Stream
                                .parallelMergeIterators(Stream.of(b).map(i -> i.toList().iterator()).toList(),
                                        (i, j) -> i <= j ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                                .mapToInt(i -> i)
                                .toArray()));
            }
        }
    }

    @Test
    public void test_zip() {
        final int[] a = { 11, 12, 13, 14 };
        final int[] b = { 21, 22, 23 };
        final int[] c = { 31, 32, 33, 34, 35 };

        assertEquals("32, 34, 36", IntStream.zip(a, b, (i, j) -> i + j).join(", "));
        assertEquals("32, 34, 36, 34", IntStream.zip(a, b, 10, 20, (i, j) -> i + j).join(", "));

        assertEquals("32, 34, 36", IntStream.zip(a, b, (i, j) -> i + j).join(", "));
        assertEquals("32, 34, 36, 34", IntStream.zip(a, b, 10, 20, (i, j) -> i + j).join(", "));

        assertEquals("63, 66, 69", IntStream.zip(a, b, c, (i, j, k) -> i + j + k).join(", "));
        assertEquals("63, 66, 69, 68, 65", IntStream.zip(a, b, c, 10, 20, 30, (i, j, k) -> i + j + k).join(", "));

        assertEquals("63, 66, 69", IntStream.zip(Stream.of(a, b, c).map(IntStream::of).toList(), N::sum).join(", "));
        assertEquals("63, 66, 69, 68, 65", IntStream.zip(Stream.of(a, b, c).map(IntStream::of).toList(), Array.of(10, 20, 30), N::sum).join(", "));

        assertEquals("32, 34, 36", Stream.zip(IntList.of(a).toList(), IntList.of(b).toList(), (i, j) -> i + j).join(", "));
        assertEquals("32, 34, 36, 34", Stream.zip(IntList.of(a).toList(), IntList.of(b).toList(), 10, 20, (i, j) -> i + j).join(", "));

        assertEquals("63, 66, 69", Stream.zip(IntList.of(a).toList(), IntList.of(b).toList(), IntList.of(c).toList(), (i, j, k) -> i + j + k).join(", "));
        assertEquals("63, 66, 69, 68, 65",
                Stream.zip(IntList.of(a).toList(), IntList.of(b).toList(), IntList.of(c).toList(), 10, 20, 30, (i, j, k) -> i + j + k).join(", "));

        assertEquals("32, 34, 36", Stream.parallelZip(IntList.of(a).toList().iterator(), IntList.of(b).toList().iterator(), (i, j) -> i + j, 3).join(", "));
        assertEquals("32, 34, 36, 34", Stream.zip(IntList.of(a).toList().iterator(), IntList.of(b).toList().iterator(), 10, 20, (i, j) -> i + j).join(", "));

        assertEquals("63, 66, 69",
                Stream.zip(IntList.of(a).toList().iterator(), IntList.of(b).toList().iterator(), IntList.of(c).toList().iterator(), (i, j, k) -> i + j + k)
                        .join(", "));
        assertEquals(
                "63, 66, 69, 68, 65", Stream
                        .zip(IntList.of(a).toList().iterator(), IntList.of(b).toList().iterator(), IntList.of(c).toList().iterator(), 10, 20, 30,
                                (i, j, k) -> i + j + k)
                        .join(", "));

    }

    @Test
    public void test_groupBy() {
        assertEquals("a=[a], b=[b], c=[c]", Stream.of("a", "b", "c").groupBy(e -> e).join(", "));
        assertEquals("a=[a], b=[b], c=[c]", Joiner.defauLt().appendEntries(Stream.of("a", "b", "c").groupTo(e -> e)).toString());

        Stream.of("a", "b", "c").groupBy(e -> e, Collectors.toSet()).forEach(N::println);
        Stream.of("a", "b", "c", "b").groupBy(e -> e, Collectors.toSet()).forEach(N::println);
        Stream.of("a", "b", "c", "b").groupBy(e -> e, Collectors.toList()).forEach(N::println);

        Stream.of("a", "b", "c", "b").groupBy(e -> e, e -> e, (a, b) -> a + b).forEach(N::println);
        Stream.of("a", "b", "c", "b").groupBy(e -> e, e -> e, (a, b) -> a + b, HashMap::new).forEach(N::println);

        Stream.of("a", "b", "c", "b").groupBy(e -> e, e -> e).forEach(N::println);
    }

    @Test
    public void test_toMap() {

        {
            final Map<Integer, List<Integer>> map1 = IntStream.range(1, 10).boxed().groupTo(i -> i);
            N.println(map1);

            final Map<Integer, List<Integer>> map5 = IntStream.range(1, 10).boxed().groupTo(i -> i, Suppliers.ofLinkedHashMap());
            N.println(map5);

            final Map<Integer, Set<Integer>> map2 = IntStream.range(1, 10).groupTo(i -> i, Collectors.toSet());
            N.println(map2);

            final Map<Integer, Set<Integer>> map3 = IntStream.range(1, 10).groupTo(i -> i, Collectors.toSet(), Suppliers.ofLinkedHashMap());
            N.println(map3);

            final Map<Integer, Integer> map4 = IntStream.range(1, 10).toMap(i -> i, i -> i);
            N.println(map4);

            final Map<Integer, Integer> map6 = IntStream.range(1, 10).toMap(i -> i, i -> i, Suppliers.ofLinkedHashMap());
            N.println(map6);

            final Map<Integer, Integer> map7 = IntStream.range(1, 10).toMap(i -> i, i -> i, Fn.throwingMerger());
            N.println(map7);

            IntStream.range(1, 10).toMap(i -> i, i -> i, Fn.throwingMerger(), Suppliers.ofLinkedHashMap());
        }

        {
            final Map<Integer, List<Integer>> map1 = IntStream.range(1, 10).boxed().groupTo(i -> i);
            N.println(map1);

            final Map<Integer, List<Integer>> map5 = IntStream.range(1, 10).boxed().groupTo(i -> i, Suppliers.ofLinkedHashMap());
            N.println(map5);

            final Map<Integer, Set<Integer>> map2 = IntStream.range(1, 10).boxed().groupTo(i -> i, Collectors.toSet());
            N.println(map2);

            final Map<Integer, Set<Integer>> map3 = IntStream.range(1, 10).boxed().groupTo(i -> i, Collectors.toSet(), Suppliers.ofLinkedHashMap());
            N.println(map3);

            final Map<Integer, Integer> map4 = IntStream.range(1, 10).boxed().toMap(i -> i, i -> i);
            N.println(map4);

            final Map<Integer, Integer> map6 = IntStream.range(1, 10).boxed().toMap(i -> i, i -> i, Suppliers.ofLinkedHashMap());
            N.println(map6);

            final Map<Integer, Integer> map7 = IntStream.range(1, 10).boxed().toMap(i -> i, i -> i, Fn.throwingMerger());
            N.println(map7);

            final Map<Integer, Integer> map8 = IntStream.range(1, 10).boxed().toMap(i -> i, i -> i, Fn.throwingMerger(), Suppliers.ofLinkedHashMap());
            N.println(map8);

            final Map<Integer, List<Integer>> map9 = IntStream.range(1, 10).boxed().groupTo(i -> i, i -> i);
            N.println(map9);

            final Map<Integer, List<Integer>> map10 = IntStream.range(1, 10).boxed().groupTo(i -> i, i -> i, Suppliers.ofLinkedHashMap());
            N.println(map10);

            final Multimap<Integer, Integer, List<Integer>> map11 = IntStream.range(1, 10).boxed().toMultimap(i -> i);
            N.println(map11);

            final Multimap<Integer, Integer, List<Integer>> map12 = IntStream.range(1, 10).boxed().toMultimap(i -> i, Suppliers.ofListMultimap());
            N.println(map12);

            final Multimap<Integer, Integer, List<Integer>> map13 = IntStream.range(1, 10).boxed().toMultimap(i -> i, i -> i);
            N.println(map13);

            final Multimap<Integer, Integer, List<Integer>> map14 = IntStream.range(1, 10).boxed().toMultimap(i -> i, i -> i, Suppliers.ofListMultimap());
            N.println(map14);
        }

        {
            IntStream.range(1, 10).boxed().groupBy(i -> i).forEach(N::println);

            IntStream.range(1, 10).boxed().groupBy(i -> i, Suppliers.ofLinkedHashMap()).forEach(N::println);

            IntStream.range(1, 10).boxed().groupBy(i -> i, Collectors.toSet()).forEach(N::println);

            IntStream.range(1, 10).boxed().groupBy(i -> i, Collectors.toSet(), LinkedHashMap::new).forEach(N::println);

            IntStream.range(1, 10).boxed().groupBy(i -> i, i -> i).forEach(N::println);

            IntStream.range(1, 10).boxed().groupBy(i -> i, i -> i, Suppliers.ofLinkedHashMap()).forEach(N::println);

            IntStream.range(1, 10).boxed().groupBy(i -> i, i -> i, Fn.throwingMerger()).forEach(N::println);
            IntStream.range(1, 10).boxed().groupBy(i -> i, i -> i, Fn.throwingMerger(), LinkedHashMap::new).forEach(N::println);

            IntStream.range(1, 10).boxed().groupBy(i -> i, i -> i).forEach(N::println);

            IntStream.range(1, 10).boxed().groupBy(i -> i, i -> i, Suppliers.ofLinkedHashMap()).forEach(N::println);
        }

        N.println(Stream.of("a", "b", "c").groupTo(e -> e, Collectors.toSet()));
        N.println(Stream.of("a", "b", "c", "b").groupTo(e -> e, Collectors.toSet()));
        N.println(Stream.of("a", "b", "c", "b").groupTo(e -> e, Collectors.toList()));

        try {
            N.println(Stream.of("a", "b", "c", "b").toMap(e -> e, e -> e));
            fail("Should throw IllegalStateException");
        } catch (final IllegalStateException e) {
            // ignore
        }

        N.println(Stream.of("a", "b", "c", "b").toMap(e -> e, e -> e, (a, b) -> a + b));
        N.println(Stream.of("a", "b", "c", "b").toMap(e -> e, e -> e, (a, b) -> a + b, HashMap::new));

        N.println(Stream.of("a", "b", "c", "b").groupTo(e -> e, e -> e));
        N.println(Stream.of("a", "b", "c", "b").groupTo(e -> e, e -> e, HashMap::new));
    }

    @Test
    public void test_reduce() {
        IntStream.of().reduce((a, b) -> a + b).ifPresent(N::println);
        IntStream.of(1, 2, 3).reduce((a, b) -> a + b).ifPresent(N::println);

        CharStream.of('a', 'b', 'c').reduce((a, b) -> (char) (a + b - b)).ifPresent(N::println);
        N.println(CharStream.of('a', 'b', 'c').reduce('z', (a, b) -> (char) (a + b - b)));

        Stream.of('a', 'b', 'c').reduce((a, b) -> (char) (a + b - b)).ifPresent(N::println);
        N.println(Stream.of('a', 'b', 'c').reduce('z', (a, b) -> (char) (a + b - b)));
    }

    @Test
    public void test_collect() {
        List<Character> result = CharStream.of('a', 'b', 'c').collect(ArrayList::new, ArrayList::add);
        N.println(result);
        result = CharStream.of('a', 'b', 'c').collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        N.println(result);

        result = Stream.of('a', 'b', 'c').collect(ArrayList::new, ArrayList::add);
        N.println(result);
        result = Stream.of('a', 'b', 'c').collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        N.println(result);
        result = Stream.of('a', 'b', 'c').collect(Collectors.toList());
        N.println(result);
    }

    @Test
    public void test_summarize() {
        CharSummaryStatistics result = CharStream.of('a', 'b', 'c').summarize();
        N.println(result);

        result = Stream.of('a', 'b', 'c').mapToChar(i -> i).summarize();
        N.println(result);

        final IntSummaryStatistics result2 = Stream.of('a', 'b', 'c').mapToInt(i -> i).summarize();
        N.println(result2);
    }

    @Test
    public void test_zipWith() {
        Stream.of("a", "b", "c").zipWith(Stream.of("1", "2"), (a, b) -> a + b).println();
        Stream.of("a", "b", "c").zipWith(Stream.of("1", "2"), " ", "0", (a, b) -> a + b).println();
    }

    @Test
    public void test_println() {
        IntStream.of(1, 2, 3).println();
        IntStream.of(1, 2, 3).mapToObj(IntStream::of).println();

        Stream.of(1, 2, 3).println();
        Stream.of(1, 2, 3).map(IntStream::of).println();
    }

    @Test
    public void test_split() {
        IntList.range(1, 10).split(2).forEach(N::println);
    }

    @Test
    public void test_find() {
        final Random rand = new Random();

        for (int k = 0; k < 103; k++) {
            final int[] a = IntStream.random().limit(rand.nextInt(10001) + 1).toArray();
            final int mid = a[a.length / 2];

            IntStream.of(a).findFirst(i -> i >= mid).get();
            IntStream.of(a).findLast(i -> i <= mid).get();

            //            N.println(a);
            //            N.println(mid);
            //            N.println(first);
            //            N.println(last);

            //    assertEquals(first, IntStream.of(IntStream.of(a).iterator()).findFirst(i -> i >= mid).get());
            //    assertEquals(last, IntStream.of(IntStream.of(a).iterator()).findLast(i -> i <= mid).get());
            //
            //    assertEquals(first, IntStream.of(a).parallel().findFirst(i -> i >= mid).get());
            //    assertEquals(last, IntStream.of(a).parallel().findLast(i -> i <= mid).get());
            //
            //    assertEquals(first, IntStream.of(a).parallel(Splitor.ARRAY).findFirst(i -> i >= mid).get());
            //    assertEquals(last, IntStream.of(a).parallel(Splitor.ARRAY).findLast(i -> i <= mid).get());
            //
            //    assertEquals(first, IntStream.of(IntStream.of(a).iterator()).parallel().findFirst(i -> i >= mid).get());
            //    assertEquals(last, IntStream.of(IntStream.of(a).iterator()).parallel().findLast(i -> i <= mid).get());
            //
            //    assertEquals(first, Stream.of(Array.box(a)).parallel(Splitor.ARRAY).findFirst(i -> i >= mid).get().longValue());
            //    assertEquals(last, Stream.of(Array.box(a)).parallel(Splitor.ARRAY).findLast(i -> i <= mid).get().longValue());
            //
            //    assertEquals(first, Stream.of(Array.box(a)).parallel().findFirst(i -> i >= mid).get().longValue());
            //    assertEquals(last, Stream.of(Array.box(a)).parallel().findLast(i -> i <= mid).get().longValue());
            //
            //    assertEquals(first, Stream.of(Stream.of(Array.box(a)).iterator()).parallel().findFirst(i -> i >= mid).get().longValue());
            //    assertEquals(last, Stream.of(Stream.of(Array.box(a)).iterator()).parallel().findLast(i -> i <= mid).get().longValue());
        }
    }

    //    @Test
    //    public void test_interval() {
    //        LongStream.interval(System.currentTimeMillis(), 3).limit(3).println();
    //    }

    @Test
    public void test_parallel_concat() {
        final int[] a = IntStream.range(0, 19).toArray();

        final int[] b = Stream.parallelConcat(Stream.of(IntStream.range(0, 9).iterator()), Stream.of(IntStream.range(9, 19).iterator()))
                .mapToInt(ToIntFunction.UNBOX)
                .sorted()
                .toArray();

        assertTrue(N.equals(a, b));
    }

    @Test
    public void test_parallel_zip() {
        final int[] a = IntStream.repeat(3, 9).toArray();

        final int[] b = Stream.parallelZip(Stream.of(IntStream.repeat(1, 9).iterator()), Stream.of(IntStream.repeat(2, 10).iterator()), (i, j) -> i + j, 3)
                .mapToInt(ToIntFunction.UNBOX)
                .sorted()
                .toArray();

        assertTrue(N.equals(a, b));
    }

}
