package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.N;

/** AbstractStream window contracts. */
public class AbstractStreamWindowTest extends AbstractStreamTestSupport {

    // buffered

    @Test
    public void testBuffered() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.buffered();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testBuffered_withSize() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = stream.buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testBufferedWithQueue() {
        stream = createStream(1, 2, 3, 4, 5);
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
        Stream<Integer> result = stream.buffered(queue);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testBufferedWithSize() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.buffered(3);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    // sliding

    @Test
    public void test_sliding() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).sliding(3).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void testsliding() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<List<Integer>> result = stream.sliding(3);
        List<List<Integer>> lists = result.toList();
        assertEquals(3, lists.size());
        assertEquals(Arrays.asList(1, 2, 3), lists.get(0));
        assertEquals(Arrays.asList(2, 3, 4), lists.get(1));
    }

    @Test
    public void test_sliding_withIncrement() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).sliding(3, 2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void testSliding_withWindowAndIncrement() {
        stream = createStream(1, 2, 3, 4, 5);
        List<List<Integer>> result = stream.sliding(3, 2).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));
    }

    @Test
    public void testSlidingWithCollectionSupplier() {
        stream = createStream(1, 2, 3, 4);
        Stream<Set<Integer>> result = stream.sliding(2, i -> new HashSet<>());
        assertEquals(3, result.toList().size());
    }

    @Test
    public void testSlidingWithCollector() {
        stream = createStream(1, 2, 3, 4);
        Stream<String> result = stream.sliding(2, Collectors.mapping(Object::toString, Collectors.joining(",")));
        assertEquals(Arrays.asList("1,2", "2,3", "3,4"), result.toList());
    }

    @Test
    public void testSlidingWithIncrement() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<List<Integer>> result = stream.sliding(3, 2);
        List<List<Integer>> lists = result.toList();
        assertEquals(3, lists.size());
        assertEquals(Arrays.asList(1, 2, 3), lists.get(0));
        assertEquals(Arrays.asList(3, 4, 5), lists.get(1));
        assertEquals(Arrays.asList(5, 6), lists.get(2));
    }

    // slidingMap

    @Test
    public void testSlidingMap() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.slidingMap((a, b) -> a + (b == null ? 0 : b));
        assertEquals(Arrays.asList(3, 5, 7), result.toList());
    }

    @Test
    public void test_slidingMap_biFunction() {
        List<Integer> result = Stream.of(1, 2, 3, 4).slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7), result);
    }

    @Test
    public void test_slidingMap_biFunctionWithIncrement() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).slidingMap(2, (a, b) -> a + (b == null ? 0 : b)).toList();
        assertEquals(Arrays.asList(3, 7, 5), result);
    }

    @Test
    public void test_slidingMap_triFunction() {
        List<String> result = Stream.of("a", "b", "c", "d").slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList("abc", "bcd"), result);
    }

    @Test
    public void test_slidingMap_triFunctionWithIncrement() {
        List<String> result = Stream.of("a", "b", "c", "d", "e", "d").slidingMap(2, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList("abc", "cde", "ednull"), result);
    }

    @Test
    public void testSlidingMapBiFunction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> sums = stream.slidingMap((a, b) -> a + (b == null ? 0 : b)).toList();
        assertEquals(Arrays.asList(3, 5, 7, 9), sums);
    }

    @Test
    public void testSlidingMapTriFunction() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.slidingMap((a, b, c) -> a + (b == null ? 0 : b) + (c == null ? 0 : c));
        assertEquals(Arrays.asList(6, 9, 12), result.toList());
    }

    @Test
    public void testSlidingMapWithIncrement() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.slidingMap(2, (a, b) -> a + (b == null ? 0 : b));
        assertEquals(Arrays.asList(3, 7, 5), result.toList());
    }

    // split

    @Test
    public void testSplit() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<List<Integer>> result = stream.split(3);
        List<List<Integer>> lists = result.toList();
        assertEquals(2, lists.size());
        assertEquals(Arrays.asList(1, 2, 3), lists.get(0));
        assertEquals(Arrays.asList(4, 5, 6), lists.get(1));
    }

    @Test
    public void test_split_byPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5, 6).split(i -> i % 3 == 0).toList();
        assertEquals(Arrays.asList(List.of(1, 2), List.of(3), List.of(4, 5), List.of(6)), result);
    }

    @Test
    public void test_split_bySize() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).split(2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), List.of(5)), result);
    }

    @Test
    public void testSplit_bySize() {
        stream = createStream(1, 2, 3, 4, 5);
        List<List<Integer>> result = stream.split(2).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    @Test
    public void testSplitByPredicate() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<List<Integer>> result = stream.split(i -> i % 3 == 0);
        List<List<Integer>> lists = result.toList();
        assertEquals(3, lists.size());
        assertEquals(Arrays.asList(1, 2), lists.get(0));
        assertEquals(Arrays.asList(3), lists.get(1));
        assertEquals(Arrays.asList(4, 5), lists.get(2));
    }

    // splitAt

    @Test
    public void testSplitAt() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Stream<Integer>> result = stream.splitAt(i -> i == 3);
        List<List<Integer>> lists = result.map(Stream::toList).toList();
        assertEquals(2, lists.size());
        assertEquals(Arrays.asList(1, 2), lists.get(0));
        assertEquals(Arrays.asList(3, 4, 5), lists.get(1));
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
    public void testSplitAt_byPosition() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Stream<Integer>> result = stream.splitAt(3).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0).toList());
        assertEquals(Arrays.asList(4, 5), result.get(1).toList());
    }

    @Test
    public void testSplitAt_byPosition_zero() {
        stream = createStream(1, 2, 3);
        List<Stream<Integer>> result = stream.splitAt(0).toList();
        assertEquals(2, result.size());
        assertEquals(Collections.emptyList(), result.get(0).toList());
        assertEquals(Arrays.asList(1, 2, 3), result.get(1).toList());
    }

    @Test
    public void test_splitAt_byPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).splitAt(i -> i == 3).map(Stream::toList).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void testSplitAt_predicateStillSplitsAtTheRightPoint() {
        final List<Stream<Integer>> halves = Stream.of(1, 2, 3, 4, 5, 6).splitAt(i -> i > 3).toList();

        assertEquals(N.asList(1, 2, 3), halves.get(0).toList());
        assertEquals(N.asList(4, 5, 6), halves.get(1).toList());
    }

    /**
     * {@code splitAt(Predicate)} built its tail with a hard-coded {@code new IteratorStream<>(...)}, so the tail
     * came back sequential and lost maxThreadNum, the split strategy and the executor - while the sibling
     * {@code splitAt(int)} preserved all of them.
     */
    @Test
    public void testSplitAt_predicateTailKeepsTheStreamsParallelSettings() {
        final List<Stream<Integer>> halves = Stream.of(1, 2, 3, 4, 5, 6).parallel(4).splitAt(i -> i > 3).toList();

        assertEquals(2, halves.size());
        assertTrue(halves.get(0).isParallel(), "head should be parallel");
        assertTrue(halves.get(1).isParallel(), "tail should be parallel"); // used to be false

        halves.get(0).close();
        halves.get(1).close();
    }

    @Test
    public void testSplitAtExhaustedOuterIteratorDoesNotConsumeSecondStream() {
        assertSplitAtExhaustedOuterIteratorDoesNotConsumeSecondStream(source -> source.splitAt(2));
        assertSplitAtExhaustedOuterIteratorDoesNotConsumeSecondStream(source -> source.splitAt(value -> value == 3));
    }

    @Test
    public void testSplitAtKeepsIteratorSourceOpenForEscapedSecondStream() {
        assertSplitAtKeepsIteratorSourceOpen(source -> source.splitAt(2));
        assertSplitAtKeepsIteratorSourceOpen(source -> source.splitAt(value -> value == 3));
    }

    @Test
    public void testSplitAtRejectsNullCollectorsBeforeTraversal() {
        final Collector<Integer, ?, List<Integer>> collector = null;
        try (final Stream<Integer> stream = Stream.of(1, 2)) {
            assertThrows(IllegalArgumentException.class, () -> stream.splitAt(1, collector));
        }
        final AtomicInteger predicateCalls = new AtomicInteger();
        try (final Stream<Integer> stream = Stream.of(1, 2)) {
            assertThrows(IllegalArgumentException.class, () -> stream.splitAt(value -> {
                predicateCalls.incrementAndGet();
                return value == 2;
            }, collector));
            assertEquals(0, predicateCalls.get());
        }
    }

    @Test
    public void testSplitAtWithCollector() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<String> result = stream.splitAt(i -> i == 3, Collectors.mapping(Object::toString, Collectors.joining(",")));
        assertEquals(Arrays.asList("1,2", "3,4,5"), result.toList());
    }

}
