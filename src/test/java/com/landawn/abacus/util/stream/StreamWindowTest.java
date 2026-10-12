package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongSuppliers;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NoCachingNoUpdating;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.QuadPredicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongTriFunction;
import com.landawn.abacus.util.stream.Stream.WindowHandler;

/** Stream window contracts. */
public class StreamWindowTest extends StreamTestSupport {

    // buffered

    @Test
    public void testBuffered() {
        List<Integer> result = Stream.of(1, 2, 3).buffered().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testBuffered_Iterator() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        // Stream.buffered(Iterator) is a static internal method used via merge
        // Test via buffered() instance method which internally calls it
        List<Integer> result = Stream.of(iter).buffered().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testBuffered_WithSize() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testBufferedWithSize() {
        List<Integer> result = Stream.of(1, 2, 3).buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // sliding

    @Test
    public void testsliding() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).sliding(3);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));
    }

    @Test
    public void test_sliding_3() {
        assertDoesNotThrow(() -> {
            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 2).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(11).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 3).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(11).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 4).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(11).println();
        });
    }

    @Test
    public void test_sliding_4() {
        assertDoesNotThrow(() -> {
            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();
        });
    }

    @Test
    public void testSlidingWindowSize() throws Exception {
        {
            assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(1).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2)), Stream.of(1, 2).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 3)), Stream.of(1, 2, 3).sliding(2).toList());

            assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(N.toList(1).iterator()).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2)), Stream.of(N.toList(1, 2).iterator()).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 3)), Stream.of(N.toList(1, 2, 3).iterator()).sliding(2).toList());
        }
        {
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 4).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(1, 2, 3, 4, 5).sliding(3, 5).toList());

            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)),
                    Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)), Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5)), Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(5)), Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 4).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 5).toList());

        }

        {
            assertEquals(0, Stream.of(1).sliding(2).skip(1).count());
            assertEquals(0, Stream.of(1, 2).sliding(2).skip(1).count());
            assertEquals(1, Stream.of(1, 2, 3).sliding(2).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 3).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 4).skip(1).count());

            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Stream.of(N.toList(1).iterator()).sliding(2).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2).iterator()).sliding(2).skip(1).count());
            assertEquals(1, Stream.of(N.toList(1, 2, 3).iterator()).sliding(2).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 3).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 4).skip(1).count());

            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Stream.of(N.toList(1).iterator()).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2).iterator()).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(1, Stream.of(N.toList(1, 2, 3).iterator()).sliding(2, Collectors.toList()).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, Collectors.toList()).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2, Collectors.toList()).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2, Collectors.toList()).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 3, Collectors.toList()).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 4, Collectors.toList()).skip(1).count());

            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 5, Collectors.toList()).skip(1).count());

        }

        {
            assertEquals(0, Seq.of(1).sliding(2).skip(1).count());
            assertEquals(0, Seq.of(1, 2).sliding(2).skip(1).count());
            assertEquals(1, Seq.of(1, 2, 3).sliding(2).skip(1).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3).skip(2).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2).skip(1).count());
            assertEquals(0, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2).skip(2).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 3).skip(1).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 4).skip(1).count());

            assertEquals(0, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Seq.of(1).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(0, Seq.of(1, 2).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(1, Seq.of(1, 2, 3).sliding(2, Collectors.toList()).skip(1).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, Collectors.toList()).skip(2).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2, Collectors.toList()).skip(1).count());
            assertEquals(0, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2, Collectors.toList()).skip(2).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 3, Collectors.toList()).skip(1).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 4, Collectors.toList()).skip(1).count());

            assertEquals(0, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 5, Collectors.toList()).skip(1).count());

        }
    }

    @Test
    public void testSlidingWindowSizeAndIncrement() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5, 6).sliding(3, 2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5), Arrays.asList(5, 6)), result);
    }

    @Test
    public void testSlidingWithIncrement() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).sliding(2, 2);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    // slidingMap

    @Test
    public void testSlidingMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).slidingMap((a, b) -> a + b);
        assertEquals(Arrays.asList(3, 5, 7), stream.toList());
    }

    @Test
    public void testSlidingMap_BiFunction() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7, 9), result);
    }

    @Test
    public void testSlidingMap_Empty() {
        List<Integer> result = Stream.<Integer> empty().slidingMap((a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingMap_TriFunction() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9, 12), result);
    }

    @Test
    public void testSlidingMap_WithIncrement() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).slidingMap(2, (a, b) -> a + (b == null ? 0 : b)).toList();
        assertEquals(Arrays.asList(3, 7, 5), result);
    }

    @Test
    public void testSlidingMapBiFunction() {
        List<String> result = Stream.of(1, 2, 3, 4).slidingMap((a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
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
        assertEquals(Arrays.asList("1-2", "3-4"), result);

        result = Stream.of(1, 2, 3, 4).slidingMap(2, false, (a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-2", "3-4"), result);
    }

    @Test
    public void testSlidingMapTriFunction() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).slidingMap((a, b, c) -> a + b + c);
        assertEquals(Arrays.asList(6, 9), stream.toList());
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

    // split

    @Test
    public void testSplit() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).split(2);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    @Test
    public void testSplit2() {
        Stream<List<Integer>> splitBySize = createStream(1, 2, 3, 4, 5, 6, 7).split(3);
        List<List<Integer>> chunks = splitBySize.toList();
        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2, 3), chunks.get(0));
        assertEquals(Arrays.asList(4, 5, 6), chunks.get(1));
        assertEquals(Arrays.asList(7), chunks.get(2));

        Stream<List<Integer>> splitByPredicate = Stream.range(0, 7).split(n -> n % 3 == 0);
        List<List<Integer>> predicateChunks = splitByPredicate.toList();
        assertEquals(Arrays.asList(Arrays.asList(0), Arrays.asList(1, 2), Arrays.asList(3), Arrays.asList(4, 5), Arrays.asList(6)), predicateChunks);

        Stream<Integer> splitWithCollector = createStream(1, 2, 3, 4, 5, 6).split(2, Collectors.summingInt(Integer::intValue));
        assertEquals(Arrays.asList(3, 7, 11), splitWithCollector.toList());
    }

    @Test
    public void testSplit_CharDelimiter() {
        String str = "apple,banana,cherry";

        List<String> parts = Stream.split(str, ',').toList();

        assertEquals(Arrays.asList("apple", "banana", "cherry"), parts);
    }

    @Test
    public void testSplit_EdgeCases() {
        assertEquals("", Stream.split("", ',').onlyOne().orElseThrow());

        assertEquals(Arrays.asList("hello"), Stream.split("hello", ',').toList());

        List<String> onlyDelimiters = Stream.split("::::", ":").toList();
        assertEquals(5, onlyDelimiters.size());
        assertTrue(onlyDelimiters.stream().allMatch(String::isEmpty));

        assertEquals(Arrays.asList("a", "b", ""), Stream.split("a,b,", ',').toList());
    }

    @Test
    public void testSplit_Pattern() {
        String str = "a1b2c3d";
        Pattern pattern = Pattern.compile("\\d");

        List<String> parts = Stream.split(str, pattern).toList();

        assertEquals(Arrays.asList("a", "b", "c", "d"), parts);
    }

    @Test
    public void testSplit_StringDelimiter() {
        String str = "one::two::three";

        List<String> parts = Stream.split(str, "::").toList();

        assertEquals(Arrays.asList("one", "two", "three"), parts);
    }

    @Test
    public void testSplitByCharDelimiter() {
        List<String> result = Stream.split("a,b,c", ',').toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitByChunkSize() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).split(2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5)), result);
        assertTrue(Stream.empty().split(2).toList().isEmpty());
        assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(1).split(2).toList());
    }

    @Test
    public void testSplitByChunkSizeAndCollector() {
        List<Long> result = Stream.of(1, 2, 3, 4, 5).split(2, Collectors.summingInt(x -> x)).map(Integer::longValue).toList();
        assertEquals(Arrays.asList(3L, 7L, 5L), result);
    }

    @Test
    public void testSplitByChunkSizeAndSupplier() {
        List<Set<Integer>> result = Stream.of(1, 2, 3, 4, 5).split(2, IntFunctions.ofLinkedHashSet()).toList();
        assertEquals(Arrays.asList(N.toLinkedHashSet(1, 2), N.toLinkedHashSet(3, 4), N.toLinkedHashSet(5)), result);
    }

    @Test
    public void testSplitByPattern() {
        List<String> result = Stream.split("a  b   c", Pattern.compile("\\s+")).toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitByPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 0, 3, 4, 0, 5).split(x -> x == 0).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Collections.singletonList(0), Arrays.asList(3, 4), Collections.singletonList(0),
                Collections.singletonList(5)), result);

        result = Stream.of(0, 1, 2, 0, 3, 4, 0).split(x -> x == 0).toList();
        assertEquals(Arrays.asList(Collections.singletonList(0), Arrays.asList(1, 2), Collections.singletonList(0), Arrays.asList(3, 4),
                Collections.singletonList(0)), result);
    }

    @Test
    public void testSplitByStringDelimiter() {
        List<String> result = Stream.split("a::b::c", "::").toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitChunkSizeOne() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3).split(1);
        List<List<Integer>> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
    }

    @Test
    public void testSplitWithCharDelimiter() {
        List<String> result = Stream.split("a,b,c", ',').toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Stream.split("hello world", ' ').toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        result = Stream.split("", ',').toList();
        assertEquals(Arrays.asList(""), result);

        result = Stream.split("abc", ',').toList();
        assertEquals(Arrays.asList("abc"), result);
    }

    @Test
    public void testSplitWithCharSequenceDelimiter() {
        List<String> result = Stream.split("a::b::c", "::").toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Stream.split("hello<br>world", "<br>").toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        result = Stream.split("test", "::").toList();
        assertEquals(Arrays.asList("test"), result);
    }

    @Test
    public void testSplitWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        List<String> result = Stream.split("hello   world  test", pattern).toList();
        assertEquals(Arrays.asList("hello", "world", "test"), result);

        pattern = Pattern.compile("[,;]");
        result = Stream.split("a,b;c,d", pattern).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testSplitWithPredicate() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5, 6).split(n -> n % 3 == 0);
        List<List<Integer>> result = stream.toList();
        assertTrue(result.size() >= 2);
    }

    // splitAt

    @Test
    public void test_splitAt() {
        for (int j = 0; j < 10; j++) {
            final int[] a = IntList.random(10_000).internalArray();
            int n = a.length / 2;

            for (int i = 0, len = a.length; i < len; i++) {
                if (a[i] == a[n]) {
                    n = i;
                    break;
                }
            }

            final int tmp = a[n];

            Object[] r1 = Stream.of(a).splitAt(n).toArray();
            Object[] r2 = Stream.of(a).parallel().splitAt(value -> value == tmp).toArray();

            assertTrue(N.deepEquals(((Stream) r1[0]).toArray(), ((Stream) r2[0]).toArray()));
            assertTrue(N.deepEquals(((Stream) r1[1]).toArray(), ((Stream) r2[1]).toArray()));

            r1 = IntStream.of(a).boxed().splitAt(n).toArray();
            r2 = IntStream.of(a).boxed().parallel().splitAt(value -> value.intValue() == tmp).toArray();

            assertTrue(N.equals(((Stream<Integer>) r1[0]).toArray(), ((Stream<Integer>) r2[0]).toArray()));

            final Integer[] a1 = ((Stream<Integer>) r1[1]).toArray(value -> new Integer[value]);

            final Integer[] a2 = ((Stream<Integer>) r2[1]).toArray(value -> new Integer[value]);

            N.println("=================");
            N.println(a);
            N.println(tmp);

            N.println("=================");
            N.println(a1);
            N.println(a2);

            assertTrue(N.equals(a1, a2));
        }
    }

    @Test
    public void testSplitAt() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).splitAt(2).map(s -> s.toList()).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));
    }

    @Test
    public void testSplitAtPosition() {
        List<Stream<Integer>> resultStreams = Stream.of(1, 2, 3, 4, 5).splitAt(2).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertEquals(Arrays.asList(3, 4, 5), resultStreams.get(1).toList());

        resultStreams = Stream.of(1, 2).splitAt(2).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertTrue(resultStreams.get(1).toList().isEmpty());

        resultStreams = Stream.of(1, 2).splitAt(0).toList();
        assertEquals(2, resultStreams.size());
        assertTrue(resultStreams.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(1).toList());
    }

    @Test
    public void testSplitAtPositionWithCollector() {
        List<Long> result = Stream.of(1, 2, 3, 4, 5).splitAt(2, Collectors.summingInt(ToIntFunction.UNBOX)).map(Integer::longValue).toList();
        assertEquals(N.toList(3L, 12L), result);
    }

    @Test
    public void testSplitAtPredicate() {
        List<Stream<Integer>> resultStreams = Stream.of(1, 2, 99, 4, 5).splitAt(x -> x == 99).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertEquals(Arrays.asList(99, 4, 5), resultStreams.get(1).toList());

        resultStreams = Stream.of(99, 1, 2).splitAt(x -> x == 99).toList();
        assertEquals(2, resultStreams.size());
        assertTrue(resultStreams.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(99, 1, 2), resultStreams.get(1).toList());

        resultStreams = Stream.of(1, 2, 3).splitAt(x -> x == 99).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2, 3), resultStreams.get(0).toList());
        assertTrue(resultStreams.get(1).toList().isEmpty());
    }

    // splitByChunkCount

    @Test
    public void testSplitByChunkCount() {
        List<Integer> result = Stream.splitByChunkCount(8, 3, (from, to) -> from).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSplitByChunkCount2() {
        IntBiFunction<List<Integer>> mapper = (from, to) -> {
            List<Integer> list = new ArrayList<>();
            for (int i = from; i < to; i++) {
                list.add(i);
            }
            return list;
        };

        Stream<List<Integer>> chunks = Stream.splitByChunkCount(10, 3, mapper);
        List<List<Integer>> result = chunks.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(0, 1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(4, 5, 6), result.get(1));
        assertEquals(Arrays.asList(7, 8, 9), result.get(2));

        Stream<List<Integer>> smallerFirstChunks = Stream.splitByChunkCount(10, 3, true, mapper);
        List<List<Integer>> smallerFirstResult = smallerFirstChunks.toList();
        assertEquals(3, smallerFirstResult.size());
        assertEquals(Arrays.asList(0, 1, 2), smallerFirstResult.get(0));
        assertEquals(Arrays.asList(3, 4, 5), smallerFirstResult.get(1));
        assertEquals(Arrays.asList(6, 7, 8, 9), smallerFirstResult.get(2));
    }

    @Test
    public void testSplitByChunkCount_NegativeTotalSize() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(-1, 5, (from, to) -> Collections.emptyList()));
    }

    @Test
    public void testSplitByChunkCount_sizeBiggerFirst_skip_uncovered() {
        // 7/3 bigger-first => [3,2,2]; skip 1 -> [2,2]
        assertEquals(N.asList(2, 2), Stream.splitByChunkCount(7, 3, false, (from, to) -> to - from).skip(1).toList());
    }

    @Test
    public void testSplitByChunkCount_SizeSmallerFirst() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7 };

        List<List<Integer>> chunks = Stream.splitByChunkCount(7, 5, true, (from, to) -> Arrays.asList(array).subList(from, to)).toList();

        assertEquals(5, chunks.size());
        assertEquals(Arrays.asList(1), chunks.get(0));
        assertEquals(Arrays.asList(2), chunks.get(1));
        assertEquals(Arrays.asList(3), chunks.get(2));
        assertEquals(Arrays.asList(4, 5), chunks.get(3));
        assertEquals(Arrays.asList(6, 7), chunks.get(4));
    }



    // splitByChunkCount sizeSmallerFirst=true advance() + count() (lines 17905-17941).
    @Test
    public void testSplitByChunkCount_sizeSmallerFirst_count_uncovered() {
        // 7 elements into 3 chunks, smaller first
        final long chunks = Stream.splitByChunkCount(7, 3, true, (from, to) -> to - from).count();
        assertEquals(3, chunks);
    }

    @Test
    public void testSplitByChunkCount_sizeSmallerFirst_skip_uncovered() {
        // chunk sizes smaller-first for 7/3 => [2,2,3]; skip 1 -> [2,3]
        assertEquals(N.asList(2, 3), Stream.splitByChunkCount(7, 3, true, (from, to) -> to - from).skip(1).toList());
    }

    @Test
    public void testSplitByChunkCount_ZeroMaxChunkCount() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(10, 0, (from, to) -> Collections.emptyList()));
    }

    @Test
    public void testSplitByChunkCountEmpty() {
        List<Integer> result = Stream.splitByChunkCount(0, 3, (from, to) -> from).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitByChunkCountNegativeTotalSize() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(-1, 3, (from, to) -> from + "-" + to));
    }

    @Test
    public void testSplitByChunkCountWithSizeSmallerFirst() {
        List<String> result = Stream.splitByChunkCount(7, 5, true, (from, to) -> from + "-" + to).toList();
        assertEquals(5, result.size());
        assertEquals("0-1", result.get(0));
        assertEquals("1-2", result.get(1));
        assertEquals("2-3", result.get(2));
        assertEquals("3-5", result.get(3));
        assertEquals("5-7", result.get(4));

        result = Stream.splitByChunkCount(7, 5, false, (from, to) -> from + "-" + to).toList();
        assertEquals(5, result.size());
        assertEquals("0-2", result.get(0));
        assertEquals("2-4", result.get(1));
        assertEquals("4-5", result.get(2));
        assertEquals("5-6", result.get(3));
        assertEquals("6-7", result.get(4));
    }

    @Test
    public void testSplitByChunkCountZeroMaxChunkCount() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(10, 0, (from, to) -> from + "-" + to));
    }

    // splitToLines

    @Test
    public void testSplitToLines() {
        List<String> result = Stream.splitToLines("line1\nline2\nline3").toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testSplitToLines2() {
        String str = "line1\nline2\rline3\r\nline4";

        List<String> lines = Stream.splitToLines(str).toList();

        assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), lines);
    }

    @Test
    public void testSplitToLines_WithOptions() {
        String str = "  line1  \n\n  line2  \n  \n  line3  ";

        List<String> lines1 = Stream.splitToLines(str, true, true).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), lines1);

        List<String> lines2 = Stream.splitToLines(str, true, false).toList();
        assertEquals(Arrays.asList("line1", "", "line2", "", "line3"), lines2);

        List<String> lines3 = Stream.splitToLines(str, false, true).toList();
        assertEquals(Arrays.asList("  line1  ", "  line2  ", "  ", "  line3  "), lines3);

        List<String> lines4 = Stream.splitToLines(str, false, false).toList();
        assertEquals(Arrays.asList("  line1  ", "", "  line2  ", "  ", "  line3  "), lines4);
    }

    @Test
    public void testSplitToLines_withTrimAndOmitEmpty() {
        List<String> result = Stream.splitToLines("  line1  \n\n  line2  ", true, true).toList();
        assertEquals(Arrays.asList("line1", "line2"), result);
    }

    @Test
    public void testSplitToLinesEmpty() {
        List<String> result = Stream.splitToLines("").toList();
        assertEquals(Arrays.asList(""), result);
    }

    @Test
    public void testSplitToLinesSingleLine() {
        List<String> result = Stream.splitToLines("single line").toList();
        assertEquals(Arrays.asList("single line"), result);
    }

    @Test
    public void testSplitToLinesWithOptions() {
        String multiLine = "  line1  \n\n  line2  \n\n";

        List<String> result = Stream.splitToLines(multiLine, false, false).toList();
        assertTrue(result.contains("  line1  "));
        assertTrue(result.contains("  line2  "));
        assertTrue(result.contains(""));

        result = Stream.splitToLines(multiLine, true, false).toList();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains(""));

        result = Stream.splitToLines(multiLine, false, true).toList();
        assertTrue(result.contains("  line1  "));
        assertTrue(result.contains("  line2  "));
        assertFalse(result.contains(""));

        result = Stream.splitToLines(multiLine, true, true).toList();
        assertEquals(Arrays.asList("line1", "line2"), result);
    }

    // window

    @Test
    public void test_sliding_window() throws Exception {

        long count = Stream.range(0, 30).rateLimited(10).window(Duration.ofMillis(1000), Suppliers.ofList()).peek(Fn.println()).count();
        assertEquals(3, count);

        count = Stream.range(0, 30).rateLimited(10).window(Duration.ofMillis(1000), 2, Suppliers.ofList()).peek(Fn.println()).count();
        assertEquals(15, count);

    }

    @Test
    public void testWindow() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Stream<Integer> stream = Stream.of(input);

        List<List<Integer>> windows = stream.window(Duration.ofMillis(100)).limit(3).toList();
        assertNotNull(windows);
        assertTrue(windows.size() > 0);
    }

    @Test
    public void test_window_01() {
        assertDoesNotThrow(() -> {
            final MutableBoolean stopSign = MutableBoolean.of(false);
            final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

            N.asyncExecute(() -> Stream.observe(queue, () -> stopSign.value(), 100)
                    .flatMapArray(s -> s.split(" "))
                    .window(Duration.ofMillis(100))
                    .forEach(ss -> Stream.of(ss).countBy(Fn.identity()).println()));

            for (int i = 0; i < 100; i++) {
                CharStream.of(Strings.uuid()).mapToObj(String::valueOf).forEach(ch -> queue.add(ch));
                N.sleep(33);
            }

            stopSign.setTrue();

            IntStream.range(1, 100).boxed().window(Duration.ofMillis(10)).flatmap(Fn.identity()).groupBy(i -> i % 3).forEach(Fn.println());
        });
    }

    @Test
    public void test_window_03() {
        assertDoesNotThrow(() -> {
            IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100)).forEach(N::println);

            IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), Collectors.summingInt(ToIntFunction.UNBOX)).forEach(Fn.println());

            N.println(Strings.repeat("=", 120));

            IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 20).forEach(N::println);

            IntStream.range(0, 30)
                    .boxed()
                    .peek(Fn.sleep(11))
                    .window(Duration.ofMillis(100), 20, Collectors.summingInt(ToIntFunction.UNBOX))
                    .forEach(Fn.println());

            N.println(Strings.repeat("=", 120));

            IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 120).forEach(N::println);

            IntStream.range(0, 30)
                    .boxed()
                    .peek(Fn.sleep(11))
                    .window(Duration.ofMillis(100), 120, Collectors.summingInt(ToIntFunction.UNBOX))
                    .forEach(Fn.println());

            N.println(Strings.repeat("=", 120));

            IntStream.range(0, 30)
                    .boxed()
                    .peek(Fn.sleep(11))
                    .map(e -> Timed.of(e, System.currentTimeMillis()))
                    .window(Duration.ofMillis(100))
                    .forEach(s -> Stream.of(s).map(Timed::value).println());

            IntStream.range(0, 30)
                    .boxed()
                    .peek(Fn.sleep(11))
                    .map(e -> Timed.of(e, System.currentTimeMillis()))
                    .window(Duration.ofMillis(100), System::currentTimeMillis, Collectors.summingInt(Timed::value))
                    .forEach(Fn.println());

            N.println(Strings.repeat("=", 120));

            IntStream.range(0, 30)
                    .boxed()
                    .peek(Fn.sleep(11))
                    .map(e -> Timed.of(e, System.currentTimeMillis()))
                    .window(Duration.ofMillis(100), 20)
                    .forEach(s -> Stream.of(s).map(Timed::value).println());

            IntStream.range(0, 30)
                    .boxed()
                    .peek(Fn.sleep(11))
                    .map(e -> Timed.of(e, System.currentTimeMillis()))
                    .window(Duration.ofMillis(100), 20, System::currentTimeMillis, Collectors.summingInt(Timed::value))
                    .forEach(Fn.println());

            N.println(Strings.repeat("=", 120));

            IntStream.range(0, 30)
                    .boxed()
                    .peek(Fn.sleep(11))
                    .map(e -> Timed.of(e, System.currentTimeMillis()))
                    .window(Duration.ofMillis(100), 120)
                    .forEach(s -> Stream.of(s).map(Timed::value).println());

            IntStream.range(0, 30)
                    .boxed()
                    .peek(Fn.sleep(11))
                    .map(e -> Timed.of(e, System.currentTimeMillis()))
                    .window(Duration.ofMillis(100), 120, System::currentTimeMillis, Collectors.summingInt(Timed::value))
                    .forEach(Fn.println());
        });
    }

    @Test
    public void test_window_04() {
        final Consumer<List<Integer>> println = N::println;

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 3).forEach(println);

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 20).forEach(println);

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 3, Collectors.summingInt(ToIntFunction.UNBOX)).forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 20, Collectors.summingInt(ToIntFunction.UNBOX)).forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        final Function<Integer, Timed<Integer>> mapper = t -> Timed.of(t, System.currentTimeMillis());

        final LongSupplier sysTimeGetter = System::currentTimeMillis;

        final Function<Timed<Integer>, Integer> unwrap = Timed::value;

        final ToIntFunction<Timed<Integer>> toInt = Timed::value;

        final Consumer<List<Timed<Integer>>> println2 = s -> Stream.of(s).map(unwrap).println();

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).map(mapper).window(Duration.ofMillis(100), 3).forEach(println2);

        IntStream.range(0, 30)
                .boxed()
                .peek(Fn.sleep(11))
                .map(mapper)
                .window(Duration.ofMillis(100), 5, sysTimeGetter, Collectors.summingInt(toInt))
                .forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).map(mapper).window(Duration.ofMillis(100), 20).forEach(println2);

        IntStream.range(0, 30)
                .boxed()
                .peek(Fn.sleep(11))
                .map(mapper)
                .window(Duration.ofMillis(100), 3, sysTimeGetter, Collectors.summingInt(toInt))
                .forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).map(mapper).window(Duration.ofMillis(100), 120).forEach(println2);

        IntStream.range(0, 30)
                .boxed()
                .peek(Fn.sleep(11))
                .map(mapper)
                .window(Duration.ofMillis(100), 3, sysTimeGetter, Collectors.summingInt(toInt))
                .forEach(Fn.println());
        assertNotNull(println2);
    }

    @Test
    public void test_window_05() {
        final Consumer<List<Integer>> println = N::println;

        IntStream.range(0, 12).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(9), 3).forEach(println);

        N.println(Strings.repeat('=', 80));
        N.sleep(200);

        IntStream.range(0, 12).boxed().peek(Fn.sleep(10)).window(Duration.ofMillis(100), 3).forEach(println);

        N.println(Strings.repeat('=', 80));
        N.sleep(200);

        IntStream.range(0, 12).boxed().peek(Fn.sleep(1)).window(Duration.ofMillis(100), 3).forEach(println);

        N.println(Strings.repeat('=', 80));
        N.sleep(200);
        assertNotNull(println);
    }

    @Test
    public void test_window_06() {
        final Consumer<List<Integer>> println = N::println;

        IntStream.range(0, 12).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(10)).forEach(println);

        N.println(Strings.repeat('=', 80));
        N.sleep(200);

        IntStream.range(0, 12).boxed().peek(Fn.sleep(10)).window(Duration.ofMillis(100)).forEach(println);

        N.println(Strings.repeat('=', 80));
        N.sleep(200);

        IntStream.range(0, 12).boxed().peek(Fn.sleep(1)).window(Duration.ofMillis(100)).forEach(println);

        N.println(Strings.repeat('=', 80));
        N.sleep(200);
        assertNotNull(println);
    }

    @Test
    public void test_window_07() {
        final Consumer<List<Integer>> println = N::println;

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100)).forEach(println);

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), Collectors.summingInt(ToIntFunction.UNBOX)).forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 20).forEach(println);

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 20, Collectors.summingInt(ToIntFunction.UNBOX)).forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 120).forEach(println);

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), 120, Collectors.summingInt(ToIntFunction.UNBOX)).forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        final Function<Integer, Timed<Integer>> mapper = t -> Timed.of(t, System.currentTimeMillis());

        final LongSupplier sysTimeGetter = System::currentTimeMillis;

        final Function<Timed<Integer>, Integer> unwrap = Timed::value;

        final ToIntFunction<Timed<Integer>> toInt = Timed::value;

        final Consumer<List<Timed<Integer>>> println2 = s -> Stream.of(s).map(unwrap).println();

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).map(mapper).window(Duration.ofMillis(100)).forEach(println2);

        IntStream.range(0, 30)
                .boxed()
                .peek(Fn.sleep(11))
                .map(mapper)
                .window(Duration.ofMillis(100), sysTimeGetter, Collectors.summingInt(toInt))
                .forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).map(mapper).window(Duration.ofMillis(100), 20).forEach(println2);

        IntStream.range(0, 30)
                .boxed()
                .peek(Fn.sleep(11))
                .map(mapper)
                .window(Duration.ofMillis(100), 20, sysTimeGetter, Collectors.summingInt(toInt))
                .forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).map(mapper).window(Duration.ofMillis(100), 120).forEach(println2);

        IntStream.range(0, 30)
                .boxed()
                .peek(Fn.sleep(11))
                .map(mapper)
                .window(Duration.ofMillis(100), 120, sysTimeGetter, Collectors.summingInt(toInt))
                .forEach(Fn.println());

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), ArrayList::new).forEach(Fn.println());
        assertNotNull(println2);
    }

    @Test
    public void test_window_08() {
        final long now = System.currentTimeMillis();
        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), () -> now + 3000, Suppliers.ofList()).forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30).boxed().peek(Fn.sleep(11)).window(Duration.ofMillis(100), () -> now - 3000, Suppliers.ofList()).forEach(Fn.println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30)
                .boxed()
                .map(e -> Timed.of(e, now - 3000 - e * 10))
                .window(Duration.ofMillis(100))
                .forEach(s -> Stream.of(s).map(Timed::value).println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30)
                .boxed()
                .map(e -> Timed.of(e, now + 3000 + e * 10))
                .window(Duration.ofMillis(100))
                .forEach(s -> Stream.of(s).map(Timed::value).println());

        N.println(Strings.repeat("=", 120));

        IntStream.range(0, 30)
                .boxed()
                .map(e -> Timed.of(e, now + 3000 + e * 10))
                .window(Duration.ofMillis(100), 20)
                .forEach(s -> Stream.of(s).map(Timed::value).println());

        N.println(Strings.repeat("=", 120));
        assertNotNull(now);
    }

    @Test
    public void test_window_late_03() {

        final long startTimeMillis = System.currentTimeMillis();

        final long windowInMillis = 1000;
        final long slideInMillis = 500;

        final int cacheSize = 10;
        final Tuple2<Long, Multiset<String>>[] cache = new Tuple2[cacheSize];
        final MutableLong prevWindowNum = MutableLong.of(-1);

        Supplier<Tuple2<Long, Multiset<String>>> supplier = () -> {
            long windowNum = (System.currentTimeMillis() - startTimeMillis) / slideInMillis;

            if (windowNum <= prevWindowNum.value()) {
                windowNum = prevWindowNum.value() + 1;
            }

            prevWindowNum.setValue(windowNum);

            final Tuple2<Long, Multiset<String>> tp = Tuple.of(windowNum, N.newMultiset());

            cache[(int) (tp._1 % cacheSize)] = tp;
            return tp;
        };

        BiConsumer<Tuple2<Long, Multiset<String>>, Timed<String>> accumulator = (tp, it) -> {
            long n = (it.timestamp() - startTimeMillis) / slideInMillis;

            List<String> words = Array.asList(Strings.split(it.value(), " "));
            Tuple2<Long, Multiset<String>> e = cache[(int) (n % cacheSize)];

            if (e != null && e._1 == n) {
                e._2.addAll(words);
            }

            e = n < 1 ? null : cache[(int) ((n - 1) % cacheSize)];

            if (e != null && e._1 == n - 1) {
                e._2.addAll(words);
            }
        };

        BinaryOperator<Tuple2<Long, Multiset<String>>> combiner = (a, b) -> {
            a._2.addAll(b._2);
            return a;
        };

        Collector<Timed<String>, ?, Tuple2<Long, Multiset<String>>> collector = Collectors.create(supplier, accumulator, combiner);

        List<Timed<String>> list = N.toList(Timed.of("cat dog", startTimeMillis + 200), Timed.of("dog dog", startTimeMillis + 300),
                Timed.of("owl cat", startTimeMillis + 700), Timed.of("owl", startTimeMillis + 1300));

        Stream.of(list)
                .peek(it -> N.sleep(it.timestamp() - System.currentTimeMillis()))
                .window(Duration.ofMillis(windowInMillis), Duration.ofMillis(slideInMillis), collector)
                .toList()
                .forEach(tp -> N.println("Window[" + tp._1 + "]: " + tp._2.toMapSortedByKey(Comparators.naturalOrder())));
        assertNotNull(collector);
    }

    @Test
    public void testWindow_WithDurationSizeHandlerAndCollector_Async() {
        Duration maxDuration = Duration.ofMillis(500);
        int maxWindowSize = 3;
        List<Long> result = Stream.of(1, 2, 3, 4, 5, 6).window(maxDuration, maxWindowSize, System::currentTimeMillis, null, Collectors.counting()).toList();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testWindowInternalMethodsWithAsync() {
        Duration duration = Duration.ofMillis(100);
        Supplier<List<Integer>> supplier = ArrayList::new;

        Stream<List<Integer>> resultAsync = stream.window(duration, duration, LongSuppliers.ofCurrentTimeMillis(), supplier, true);
        List<List<Integer>> windowsAsync = resultAsync.toList();
        Assertions.assertNotNull(windowsAsync);

        stream = Stream.of(testData);

        Stream<List<Integer>> resultSync = stream.window(duration, duration, LongSuppliers.ofCurrentTimeMillis(), supplier, false);
        List<List<Integer>> windowsSync = resultSync.toList();
        Assertions.assertNotNull(windowsSync);
    }

    @Test
    public void testWindowWithCollector() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = Stream.of(input);

        List<Integer> sums = stream.window(Duration.ofMillis(100), Collectors.summingInt(Integer::intValue)).limit(3).toList();

        assertNotNull(sums);
        assertTrue(sums.size() > 0);
    }

    @Test
    public void testWindowWithComplexPredicate() {
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> {
            if (current.value() != null && next.value() != null) {
                return Math.abs(next.value() - current.value()) > 2;
            }
            return false;
        };

        Stream<Integer> testStream = Stream.of(1, 2, 3, 7, 8, 9, 15, 16);
        Stream<List<Integer>> result = testStream.window(splitter);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        Assertions.assertTrue(windows.size() >= 3);
    }

    @Test
    public void testWindowWithCustomCollectorAggregation() {
        Duration duration = Duration.ofMillis(100);

        Stream<Integer> sumResult = stream.window(duration, Collectors.summingInt(Integer::intValue));
        List<Integer> sums = sumResult.toList();

        Assertions.assertNotNull(sums);
        int totalSum = sums.stream().mapToInt(Integer::intValue).sum();
        int expectedSum = testData.stream().mapToInt(Integer::intValue).sum();
        Assertions.assertEquals(expectedSum, totalSum);
    }

    @Test
    public void testWindowWithCustomSplitter() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        Stream<Integer> stream = Stream.of(input);

        List<List<Integer>> windows = stream.window((first, last, next, count) -> count >= 3 || next.value() - first.value() > 3).toList();

        assertNotNull(windows);
        assertTrue(windows.size() > 0);
    }

    @Test
    public void testWindowWithDuration() {
        Duration duration = Duration.ofMillis(50);

        Stream<List<Integer>> result = stream.window(duration);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        int totalElements = windows.stream().mapToInt(List::size).sum();
        Assertions.assertEquals(testData.size(), totalElements);
    }

    @Test
    public void testWindowWithDurationAndCollectionSupplier() {
        Duration duration = Duration.ofMillis(50);
        Supplier<List<Integer>> supplier = LinkedList::new;

        Stream<List<Integer>> result = stream.window(duration, supplier);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (List<Integer> window : windows) {
            Assertions.assertTrue(window instanceof LinkedList);
        }
    }

    @Test
    public void testWindowWithDurationAndCollector() {
        Duration duration = Duration.ofMillis(50);

        Stream<Long> result = stream.window(duration, Collectors.counting());
        List<Long> counts = result.toList();

        Assertions.assertNotNull(counts);
        long totalCount = counts.stream().mapToLong(Long::longValue).sum();
        Assertions.assertEquals(testData.size(), totalCount);
    }

    @Test
    public void testWindowWithDurationAndIncrement() {
        Duration duration = Duration.ofMillis(100);
        Duration increment = Duration.ofMillis(50);

        Stream<List<Integer>> result = stream.window(duration, increment);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
    }

    @Test
    public void testWindowWithDurationIncrementAndCollectionSupplier() {
        Duration duration = Duration.ofMillis(100);
        Duration increment = Duration.ofMillis(50);
        Supplier<HashSet<Integer>> supplier = HashSet::new;

        Stream<Set<Integer>> result = stream.window(duration, increment, supplier);
        List<Set<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (Set<Integer> window : windows) {
            Assertions.assertTrue(window instanceof HashSet);
        }
    }

    @Test
    public void testWindowWithDurationIncrementAndCollector() {
        Duration duration = Duration.ofMillis(100);
        Duration increment = Duration.ofMillis(50);

        Stream<String> result = stream.window(duration, increment, Collectors.mapping(Object::toString, Collectors.joining(",")));
        List<String> joinedWindows = result.toList();

        Assertions.assertNotNull(joinedWindows);
    }

    @Test
    public void testWindowWithDurationIncrementStartTimeAndCollectionSupplier() {
        Duration duration = Duration.ofMillis(100);
        Duration increment = Duration.ofMillis(50);
        LongSupplier startTimeSupplier = () -> System.currentTimeMillis() - 1000;
        Supplier<List<Integer>> supplier = ArrayList::new;

        Stream<List<Integer>> result = stream.window(duration, increment, startTimeSupplier, supplier);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
    }

    @Test
    public void testWindowWithDurationIncrementStartTimeAndCollector() {
        Duration duration = Duration.ofMillis(100);
        Duration increment = Duration.ofMillis(50);
        LongSupplier startTimeSupplier = System::currentTimeMillis;

        Stream<Long> result = stream.window(duration, increment, startTimeSupplier, Collectors.counting());
        List<Long> counts = result.toList();

        Assertions.assertNotNull(counts);
    }

    @Test
    public void testWindowWithDurationIncrementStartTimeHandlerAndCollector() {
        Duration duration = Duration.ofMillis(100);
        Duration increment = Duration.ofMillis(50);
        LongSupplier startTimeSupplier = System::currentTimeMillis;
        WindowHandler<Integer, Long> handler = null;

        Stream<Long> result = stream.window(duration, increment, startTimeSupplier, handler, Collectors.counting());
        List<Long> counts = result.toList();

        Assertions.assertNotNull(counts);
    }

    @Test
    public void testWindowWithDurationStartTimeAndCollectionSupplier() {
        Duration duration = Duration.ofMillis(50);
        LongSupplier startTimeSupplier = System::currentTimeMillis;
        Supplier<List<Integer>> supplier = ArrayList::new;

        Stream<List<Integer>> result = stream.window(duration, startTimeSupplier, supplier);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (List<Integer> window : windows) {
            Assertions.assertTrue(window instanceof ArrayList);
        }
    }

    @Test
    public void testWindowWithDurationStartTimeAndCollector() {
        Duration duration = Duration.ofMillis(50);
        LongSupplier startTimeSupplier = System::currentTimeMillis;

        Stream<Double> result = stream.window(duration, startTimeSupplier, Collectors.averagingIntOrElseThrow(Integer::intValue));
        List<Double> averages = result.toList();

        Assertions.assertNotNull(averages);
        Assertions.assertTrue(averages.size() > 0);
    }

    @Test
    public void testWindowWithDynamicMaxWait() {
        ToLongTriFunction<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> maxWait = (first, current, count) -> count * 20L;
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 5;

        Stream<List<Integer>> result = stream.window(maxWait, splitter);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (List<Integer> window : windows) {
            Assertions.assertTrue(window.size() <= 5);
        }
    }

    @Test
    public void testWindowWithEmptyStream() {
        Stream<Integer> emptyStream = Stream.<Integer> empty();
        Duration duration = Duration.ofMillis(100);

        Stream<List<Integer>> result = emptyStream.window(duration);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        Assertions.assertTrue(windows.isEmpty());
    }

    @Test
    public void testWindowWithIncrement() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Stream<Integer> stream = Stream.of(input);

        List<List<Integer>> windows = stream.window(Duration.ofMillis(100), Duration.ofMillis(50)).limit(3).toList();

        assertNotNull(windows);
        assertTrue(windows.size() > 0);
    }

    @Test
    public void testWindowWithLargeDataset() {
        List<Integer> largeData = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeData.add(i);
        }
        Stream<Integer> largeStream = Stream.of(largeData);
        Duration duration = Duration.ofMillis(10);
        int maxWindowSize = 100;

        Stream<List<Integer>> result = largeStream.window(duration, maxWindowSize);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        int totalElements = windows.stream().mapToInt(List::size).sum();
        Assertions.assertEquals(largeData.size(), totalElements);

        for (List<Integer> window : windows) {
            Assertions.assertTrue(window.size() <= maxWindowSize);
        }
    }

    @Test
    public void testWindowWithMaxDurationAndMaxWindowSize() {
        Duration maxDuration = Duration.ofMillis(100);
        int maxWindowSize = 3;

        Stream<List<Integer>> result = stream.window(maxDuration, maxWindowSize);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (List<Integer> window : windows) {
            Assertions.assertTrue(window.size() <= maxWindowSize);
        }
    }

    @Test
    public void testWindowWithMaxDurationMaxSizeAndCollectionSupplier() {
        Duration maxDuration = Duration.ofMillis(100);
        int maxWindowSize = 3;
        Supplier<List<Integer>> supplier = LinkedList::new;

        Stream<List<Integer>> result = stream.window(maxDuration, maxWindowSize, supplier);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (List<Integer> window : windows) {
            Assertions.assertTrue(window.size() <= maxWindowSize);
            Assertions.assertTrue(window instanceof LinkedList);
        }
    }

    @Test
    public void testWindowWithMaxDurationMaxSizeAndCollector() {
        Duration maxDuration = Duration.ofMillis(100);
        int maxWindowSize = 3;

        Stream<Long> result = stream.window(maxDuration, maxWindowSize, Collectors.counting());
        List<Long> counts = result.toList();

        Assertions.assertNotNull(counts);
        for (Long count : counts) {
            Assertions.assertTrue(count <= maxWindowSize);
        }
    }

    @Test
    public void testWindowWithMaxDurationMaxSizeInternalAsync() {
        Duration maxDuration = Duration.ofMillis(100);
        int maxWindowSize = 3;
        Supplier<List<Integer>> supplier = ArrayList::new;

        Stream<List<Integer>> resultAsync = stream.window(maxDuration, maxWindowSize, LongSuppliers.ofCurrentTimeMillis(), supplier, true);
        List<List<Integer>> windowsAsync = resultAsync.toList();
        Assertions.assertNotNull(windowsAsync);

        stream = Stream.of(testData);

        Stream<List<Integer>> resultSync = stream.window(maxDuration, maxWindowSize, LongSuppliers.ofCurrentTimeMillis(), supplier, false);
        List<List<Integer>> windowsSync = resultSync.toList();
        Assertions.assertNotNull(windowsSync);
    }

    @Test
    public void testWindowWithMaxDurationMaxSizeStartTimeAndCollectionSupplier() {
        Duration maxDuration = Duration.ofMillis(100);
        int maxWindowSize = 3;
        LongSupplier startTimeSupplier = System::currentTimeMillis;
        Supplier<List<Integer>> supplier = ArrayList::new;

        Stream<List<Integer>> result = stream.window(maxDuration, maxWindowSize, startTimeSupplier, supplier);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (List<Integer> window : windows) {
            Assertions.assertTrue(window.size() <= maxWindowSize);
        }
    }

    @Test
    public void testWindowWithMaxDurationMaxSizeStartTimeAndCollector() {
        Duration maxDuration = Duration.ofMillis(100);
        int maxWindowSize = 3;
        LongSupplier startTimeSupplier = System::currentTimeMillis;

        Stream<Double> result = stream.window(maxDuration, maxWindowSize, startTimeSupplier, Collectors.averagingIntOrElseThrow(Integer::intValue));
        List<Double> averages = result.toList();

        Assertions.assertNotNull(averages);
    }

    @Test
    public void testWindowWithMaxDurationMaxSizeStartTimeHandlerAndCollector() {
        Duration maxDuration = Duration.ofMillis(100);
        int maxWindowSize = 3;
        LongSupplier startTimeSupplier = System::currentTimeMillis;
        WindowHandler<Integer, Long> handler = null;

        Stream<Long> result = stream.window(maxDuration, maxWindowSize, startTimeSupplier, handler, Collectors.counting());
        List<Long> counts = result.toList();

        Assertions.assertNotNull(counts);
    }

    @Test
    public void testWindowWithMaxSize() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Stream<Integer> stream = Stream.of(input);

        List<List<Integer>> windows = stream.window(Duration.ofMillis(1000), 3).limit(4).toList();

        assertNotNull(windows);
        for (List<Integer> window : windows) {
            assertTrue(window.size() <= 3);
        }
    }

    @Test
    public void testWindowWithMaxWaitAndQuadPredicate() {
        ToLongTriFunction<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> maxWait = (first, current, count) -> 50L;
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;

        Stream<List<Integer>> result = stream.window(maxWait, splitter);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
    }

    @Test
    public void testWindowWithMaxWaitQuadPredicateAndCollectionSupplier() {
        ToLongTriFunction<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> maxWait = (first, current, count) -> 50L;
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;
        Supplier<HashSet<Integer>> supplier = HashSet::new;

        Stream<Set<Integer>> result = stream.window(maxWait, splitter, supplier);
        List<Set<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (Set<Integer> window : windows) {
            Assertions.assertTrue(window instanceof HashSet);
        }
    }

    @Test
    public void testWindowWithMaxWaitQuadPredicateAndCollector() {
        ToLongTriFunction<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> maxWait = (first, current, count) -> 50L;
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;

        Stream<Long> result = stream.window(maxWait, splitter, Collectors.counting());
        List<Long> counts = result.toList();

        Assertions.assertNotNull(counts);
    }

    @Test
    public void testWindowWithMaxWaitQuadPredicateStartTimeAndCollectionSupplier() {
        ToLongTriFunction<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> maxWait = (first, current, count) -> 50L;
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;
        LongSupplier startTimeSupplier = System::currentTimeMillis;
        Supplier<List<Integer>> supplier = ArrayList::new;

        Stream<List<Integer>> result = stream.window(maxWait, splitter, startTimeSupplier, supplier);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
    }

    @Test
    public void testWindowWithMaxWaitQuadPredicateStartTimeAndCollector() {
        ToLongTriFunction<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> maxWait = (first, current, count) -> 50L;
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;
        LongSupplier startTimeSupplier = System::currentTimeMillis;

        Stream<Double> result = stream.window(maxWait, splitter, startTimeSupplier, Collectors.averagingIntOrElseThrow(Integer::intValue));
        List<Double> averages = result.toList();

        Assertions.assertNotNull(averages);
    }

    @Test
    public void testWindowWithNegativeDuration() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(Duration.ofMillis(-1));
        });
    }

    @Test
    public void testWindowWithNegativeIncrement() {
        Duration duration = Duration.ofMillis(100);
        Duration increment = Duration.ofMillis(-1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(duration, increment);
        });
    }

    @Test
    public void testWindowWithNegativeMaxWindowSize() {
        Duration duration = Duration.ofMillis(100);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(duration, -1);
        });
    }

    @Test
    public void testWindowWithNonOverlappingWindows() {
        Duration duration = Duration.ofMillis(50);
        Duration increment = Duration.ofMillis(100);

        Stream<List<Integer>> result = stream.window(duration, increment);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
    }

    @Test
    public void testWindowWithNullCollector() {
        Duration duration = Duration.ofMillis(100);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(duration, (java.util.stream.Collector<Integer, ?, ?>) null);
        });
    }

    @Test
    public void testWindowWithNullDuration() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window((Duration) null);
        });
    }

    @Test
    public void testWindowWithNullIncrement() {
        Duration duration = Duration.ofMillis(100);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(duration, (Duration) null);
        });
    }

    @Test
    public void testWindowWithNullMaxWaitFunction() {
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(null, splitter).count();
        });
    }

    @Test
    public void testWindowWithNullQuadPredicate() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(
                    (QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer>) null)
                    .count();
        });
    }

    @Test
    public void testWindowWithNullStartTimeSupplier() {
        final Duration duration = Duration.ofMillis(100);
        final Duration increment = Duration.ofMillis(50);
        final QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (
                first, current, next, count) -> count >= 3;
        final ToLongTriFunction<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> maxWait = (first, current, count) -> 50L;

        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).window(duration, increment, (LongSupplier) null, Collectors.toList()).count());
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).window(splitter, (LongSupplier) null, Collectors.toList()).count());
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).window(maxWait, splitter, (LongSupplier) null, Collectors.toList()).count());
    }

    @Test
    public void testWindowWithOverlappingWindows() {
        Duration duration = Duration.ofMillis(100);
        Duration increment = Duration.ofMillis(30);

        Stream<List<Integer>> result = stream.window(duration, increment);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
    }

    @Test
    public void testWindowWithQuadPredicate() {
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;

        Stream<List<Integer>> result = stream.window(splitter);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (List<Integer> window : windows) {
            Assertions.assertTrue(window.size() <= 3);
        }
    }

    @Test
    public void testWindowWithQuadPredicateAndCollectionSupplier() {
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;
        Supplier<List<Integer>> supplier = LinkedList::new;

        Stream<List<Integer>> result = stream.window(splitter, supplier);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        for (List<Integer> window : windows) {
            Assertions.assertTrue(window instanceof LinkedList);
        }
    }

    @Test
    public void testWindowWithQuadPredicateAndCollector() {
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;

        Stream<Long> result = stream.window(splitter, Collectors.counting());
        List<Long> counts = result.toList();

        Assertions.assertNotNull(counts);
    }

    @Test
    public void testWindowWithQuadPredicateStartTimeAndCollectionSupplier() {
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;
        LongSupplier startTimeSupplier = System::currentTimeMillis;
        Supplier<List<Integer>> supplier = ArrayList::new;

        Stream<List<Integer>> result = stream.window(splitter, startTimeSupplier, supplier);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
    }

    @Test
    public void testWindowWithQuadPredicateStartTimeAndCollector() {
        QuadPredicate<NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, NoCachingNoUpdating.Timed<Integer>, Integer> splitter = (first,
                current, next, count) -> count >= 3;
        LongSupplier startTimeSupplier = System::currentTimeMillis;

        Stream<String> result = stream.window(splitter, startTimeSupplier, Collectors.mapping(Object::toString, Collectors.joining(",")));
        List<String> joinedWindows = result.toList();

        Assertions.assertNotNull(joinedWindows);
    }

    @Test
    public void testWindowWithSingleElement() {
        Stream<Integer> singleStream = Stream.of(42);
        Duration duration = Duration.ofMillis(100);

        Stream<List<Integer>> result = singleStream.window(duration);
        List<List<Integer>> windows = result.toList();

        Assertions.assertNotNull(windows);
        Assertions.assertEquals(1, windows.size());
        Assertions.assertEquals(1, windows.get(0).size());
        Assertions.assertEquals(42, windows.get(0).get(0));
    }

    @Test
    public void testWindowWithZeroDuration() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(Duration.ZERO);
        });
    }

    @Test
    public void testWindowWithZeroMaxWindowSize() {
        Duration duration = Duration.ofMillis(100);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.window(duration, 0);
        });
    }

    @Test
    public void testWindowAssignerCanBeImplementedOutsideUtilPackage() {
        final IdentityWindowAssigner assigner = new IdentityWindowAssigner();

        assertEquals(Arrays.asList(1, 2, 3), assigner.apply(ObjIterator.of(1, 2, 3)).toList());
    }

    // WindowHandler

    @Test
    public void testWindowHandlerBuilderAndDefaults() {
        Stream.WindowHandler<String, List<String>> handler = Stream.WindowHandler.<String, List<String>> builder()
                .cacheSizeForLateData(10)
                .delayForLateData(true)
                .timeExtractor(s -> Long.parseLong(s.split(":")[1]))
                .onLateData((el, resList) -> resList.add("LATE:" + el))
                .build();

        assertEquals(10, handler.cacheSizeForLateData());
        assertTrue(handler.delayForLateData());
        assertNotNull(handler.timeExtractor());
        assertEquals(100L, handler.timeExtractor().applyAsLong("event:100"));
        assertNotNull(handler.onLateDataAction());

        List<String> testList = new ArrayList<>();
        handler.onLateDataAction().accept(0, 0, 0, "lateEvent", testList);
        assertEquals(Arrays.asList("LATE:lateEvent"), testList);

        Stream.WindowHandler<String, Void> handler2 = Stream.WindowHandler.of(s -> Long.parseLong(s.split(":")[0]));
        assertEquals(Stream.WindowHandler.DEFAULT_CACHE_SIZE_LATE_DATA, handler2.cacheSizeForLateData());
        assertFalse(handler2.delayForLateData());
        assertNotNull(handler2.timeExtractor());
        assertNull(handler2.onLateDataAction());
    }

    @Test
    public void testWindowHandlerBuilderSnapshotsSettings() {
        final Stream.WindowHandler.WindowHandlerBuilder<String, List<String>> builder = Stream.WindowHandler.<String, List<String>> builder()
                .cacheSizeForLateData(10)
                .delayForLateData(true)
                .timeExtractor(String::length)
                .timeWrapper((value, timestamp) -> Timed.of("first:" + value, timestamp))
                .onLateData((element, result) -> result.add("first:" + element));
        final Stream.WindowHandler<String, List<String>> first = builder.build();

        final Stream.WindowHandler<String, List<String>> second = builder.cacheSizeForLateData(20)
                .delayForLateData(false)
                .timeExtractor(value -> 99L)
                .timeWrapper((value, timestamp) -> Timed.of("second:" + value, timestamp))
                .onLateData((element, result) -> result.add("second:" + element))
                .build();

        assertEquals(10, first.cacheSizeForLateData());
        assertTrue(first.delayForLateData());
        assertEquals(3L, first.timeExtractor().applyAsLong("abc"));
        assertEquals("first:abc", first.timeWrapper().apply("abc", 100L).value());
        final List<String> firstLateData = new ArrayList<>();
        first.onLateDataAction().accept(0L, 0L, 0L, "abc", firstLateData);
        assertEquals(List.of("first:abc"), firstLateData);

        assertEquals(20, second.cacheSizeForLateData());
        assertFalse(second.delayForLateData());
        assertEquals(99L, second.timeExtractor().applyAsLong("abc"));
        assertEquals("second:abc", second.timeWrapper().apply("abc", 100L).value());
        final List<String> secondLateData = new ArrayList<>();
        second.onLateDataAction().accept(0L, 0L, 0L, "abc", secondLateData);
        assertEquals(List.of("second:abc"), secondLateData);
    }

    @Test
    public void testWindowHandlerRejectsNonPositiveCacheSizeAtConfigurationTime() {
        assertThrows(IllegalArgumentException.class,
                () -> Stream.WindowHandler.<String, List<String>> of(0, false, String::length, (element, result) -> result.add(element)));
        assertThrows(IllegalArgumentException.class,
                () -> Stream.WindowHandler.<String, List<String>> of(-1, false, String::length, (element, result) -> result.add(element)));
        assertThrows(IllegalArgumentException.class, () -> Stream.WindowHandler.<String, List<String>> builder().cacheSizeForLateData(0));
        assertThrows(IllegalArgumentException.class, () -> Stream.WindowHandler.<String, List<String>> builder().cacheSizeForLateData(-1));
    }

}
