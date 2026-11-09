package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;

public class AbstractIntStreamTest extends TestBase {

    @Test
    public void testScanInitializesOnlyAfterSuccessfulSourceRead() {
        final IllegalStateException failure = new IllegalStateException("first read failed");
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger accumulatorCalls = new java.util.concurrent.atomic.AtomicInteger();

        try (IntStream stream = IntStream.of(new int[] { 1, 2, 3 }).map(value -> {
            if (attempts.getAndIncrement() == 0) {
                throw failure;
            }
            return value;
        }).scan((left, right) -> {
            accumulatorCalls.incrementAndGet();
            return left + right;
        })) {
            final com.landawn.abacus.util.IntIterator iter = stream.iterator();
            org.junit.jupiter.api.Assertions.assertSame(failure, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::nextInt));
            org.junit.jupiter.api.Assertions.assertEquals(2, iter.nextInt());
            org.junit.jupiter.api.Assertions.assertEquals(0, accumulatorCalls.get());
            org.junit.jupiter.api.Assertions.assertEquals(5, iter.nextInt());
            org.junit.jupiter.api.Assertions.assertEquals(1, accumulatorCalls.get());
            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
        }
    }

    private IntStream stream;

    @BeforeEach
    public void setUp() {
        stream = createIntStream(new int[] { 1, 2, 3, 4, 5 });
    }

    protected IntStream createIntStream(int... elements) {
        return IntStream.of(elements).map(e -> e + 0);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        IntStream result = stream.rateLimited(rateLimiter);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testRateLimitedWithNullArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.rateLimited(null);
        });
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(1);
        IntStream result = stream.delay(delay);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testDelayWithNullArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.delay((Duration) null);
        });
    }

    @Test
    public void testSkipUntil() {
        IntStream result = stream.skipUntil(x -> x > 3);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 4, 5 }, array);
    }

    @Test
    public void testSkipUntilNoMatch() {
        IntStream result = stream.skipUntil(x -> x > 10);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] {}, array);
    }

    @Test
    public void testDistinct() {
        IntStream distinctStream = createIntStream(new int[] { 1, 2, 2, 3, 3, 3, 4, 5, 5 });
        int[] result = distinctStream.distinct().toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlatmap() {
        IntStream result = stream.flatMapArray(x -> new int[] { x, x * 2 });
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 }, array);
    }

    @Test
    public void testFlattmap() {
        IntStream result = stream.flattMap(x -> java.util.stream.IntStream.of(x, x * 2));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 }, array);
    }

    @Test
    public void testFlattMapToObj() {
        Stream<String> result = stream.flatMapArrayToObj(x -> new String[] { String.valueOf(x), String.valueOf(x * 2) });
        String[] array = result.toArray(String[]::new);
        Assertions.assertArrayEquals(new String[] { "1", "2", "2", "4", "3", "6", "4", "8", "5", "10" }, array);
    }

    @Test
    public void testFlatmapToObj() {
        Stream<String> result = stream.flatmapToObj(x -> Arrays.asList(String.valueOf(x), String.valueOf(x * 2)));
        String[] array = result.toArray(String[]::new);
        Assertions.assertArrayEquals(new String[] { "1", "2", "2", "4", "3", "6", "4", "8", "5", "10" }, array);
    }

    @Test
    public void testMapMulti() {
        IntStream result = stream.mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 2);
        });
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 }, array);
    }

    @Test
    public void testMapPartial() {
        IntStream result = stream.mapPartial(x -> x % 2 == 0 ? OptionalInt.of(x * 2) : OptionalInt.empty());
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 4, 8 }, array);
    }

    @Test
    public void testMapPartialJdk() {
        IntStream result = stream.mapPartialJdk(x -> x % 2 == 0 ? java.util.OptionalInt.of(x * 2) : java.util.OptionalInt.empty());
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 4, 8 }, array);
    }

    @Test
    public void testRangeMap() {
        IntStream rangeStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        IntStream result = rangeStream.rangeMap((left, right) -> Math.abs(left - right) <= 1, Integer::sum);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 6, 11, 16 }, array);
    }

    @Test
    public void testRangeMapToObj() {
        IntStream rangeStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        Stream<String> result = rangeStream.rangeMapToObj((left, right) -> Math.abs(left - right) <= 1, (left, right) -> left + "-" + right);
        String[] array = result.toArray(String[]::new);
        Assertions.assertArrayEquals(new String[] { "1-2", "3-3", "5-6", "8-8" }, array);
    }

    @Test
    public void testCollapse() {
        IntStream collapseStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        Stream<IntList> result = collapseStream.collapse((a, b) -> Math.abs(a - b) <= 1);
        List<IntList> lists = result.toList();
        assertEquals(3, lists.size());
        Assertions.assertArrayEquals(new int[] { 1, 2, 3 }, lists.get(0).toArray());
        Assertions.assertArrayEquals(new int[] { 5, 6 }, lists.get(1).toArray());
        Assertions.assertArrayEquals(new int[] { 8 }, lists.get(2).toArray());
    }

    @Test
    public void testCollapseWithMergeFunction() {
        IntStream collapseStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        IntStream result = collapseStream.collapse((a, b) -> Math.abs(a - b) <= 1, Integer::sum);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 6, 11, 8 }, array);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        IntStream collapseStream = createIntStream(new int[] { 1, 2, 3, 5, 6, 8 });
        IntStream result = collapseStream.collapse((first, prev, next) -> next - first <= 2, Integer::sum);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 6, 11, 8 }, array);
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        IntStream result = stream.skip(2, skipped::add);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 4, 5 }, array);
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream result = stream.filter(x -> x % 2 == 0, dropped::add);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 2, 4 }, array);
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream result = stream.dropWhile(x -> x < 3, dropped::add);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 4, 5 }, array);
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testStep() {
        IntStream stepStream = createIntStream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        IntStream result = stepStream.step(3);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 4, 7, 10 }, array);
    }

    @Test
    public void testStepWithOne() {
        IntStream result = stream.step(1);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testScan() {
        IntStream result = stream.scan((a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 3, 6, 10, 15 }, array);
    }

    @Test
    public void testScanWithInit() {
        IntStream result = stream.scan(10, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, array);
    }

    @Test
    public void testScanWithInitIncluded() {
        IntStream result = stream.scan(10, true, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 10, 11, 13, 16, 20, 25 }, array);
    }

    @Test
    public void testScanWithInitNotIncluded() {
        IntStream result = stream.scan(10, false, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, array);
    }

    @Test
    public void testTop() {
        IntStream topStream = createIntStream(new int[] { 5, 2, 8, 1, 9, 3 });
        IntStream result = topStream.top(3);
        int[] array = result.toArray();
        Arrays.sort(array);
        Assertions.assertArrayEquals(new int[] { 5, 8, 9 }, array);
    }

    @Test
    public void testIntersection() {
        IntStream result = stream.intersection(Arrays.asList(2, 3, 4, 6, 7));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 2, 3, 4 }, array);
    }

    @Test
    public void testDifference() {
        IntStream result = stream.difference(Arrays.asList(2, 3, 4, 6, 7));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 5 }, array);
    }

    @Test
    public void testSymmetricDifference() {
        IntStream result = stream.symmetricDifference(Arrays.asList(2, 3, 4, 6, 7));
        int[] array = result.toArray();
        Arrays.sort(array);
        Assertions.assertArrayEquals(new int[] { 1, 5, 6, 7 }, array);
    }

    @Test
    public void testReversed() {
        IntStream result = stream.reversed();
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, array);
    }

    @Test
    public void testRotated() {
        IntStream result = stream.rotated(2);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, array);
    }

    @Test
    public void testRotatedNegative() {
        IntStream result = stream.rotated(-2);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 3, 4, 5, 1, 2 }, array);
    }

    @Test
    public void testShuffled() {
        Random rnd = new Random(42);
        IntStream result = stream.shuffled(rnd);
        int[] array = result.toArray();
        assertEquals(5, array.length);
        Arrays.sort(array);
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testSorted() {
        IntStream unsortedStream = createIntStream(new int[] { 5, 2, 8, 1, 3 });
        IntStream result = unsortedStream.sorted();
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 5, 8 }, array);
    }

    @Test
    public void testReverseSorted() {
        IntStream unsortedStream = createIntStream(new int[] { 5, 2, 8, 1, 3 });
        IntStream result = unsortedStream.reverseSorted();
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 8, 5, 3, 2, 1 }, array);
    }

    @Test
    public void testCycled() {
        IntStream cycledStream = createIntStream(new int[] { 1, 2, 3 });
        IntStream result = cycledStream.cycled().limit(10);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, array);
    }

    @Test
    public void testCycledWithRounds() {
        IntStream cycledStream = createIntStream(new int[] { 1, 2, 3 });
        IntStream result = cycledStream.cycled(2);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3 }, array);
    }

    @Test
    public void testCycled_long() {
        stream = IntStream.of(1, 2);
        List<Integer> result = stream.cycled(3).collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2), result);
    }

    @Test
    public void testCycledWithZeroRounds() {
        IntStream result = stream.cycled(0);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] {}, array);
    }

    @Test
    public void testIndexed() {
        Stream<IndexedInt> result = stream.indexed();
        List<IndexedInt> list = result.toList();
        assertEquals(5, list.size());
        assertEquals(1, list.get(0).value());
        assertEquals(0, list.get(0).index());
        assertEquals(5, list.get(4).value());
        assertEquals(4, list.get(4).index());
    }

    @Test
    public void testBoxed() {
        Stream<Integer> result = stream.boxed();
        Integer[] array = result.toArray(Integer[]::new);
        Assertions.assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testPrependAppend_Stream_Parallel() {
        int[] result = createIntStream(3, 4).parallel().prepend(IntStream.of(1, 2)).append(IntStream.of(5)).toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testPrepend() {
        IntStream result = stream.prepend(0, -1);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 0, -1, 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testPrependStream() {
        IntStream prependStream = createIntStream(new int[] { -2, -1, 0 });
        IntStream result = stream.prepend(prependStream);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { -2, -1, 0, 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testPrependOptionalInt() {
        IntStream result = stream.prepend(OptionalInt.of(0));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testPrependEmptyOptionalInt() {
        IntStream result = stream.prepend(OptionalInt.empty());
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testAppend() {
        IntStream result = stream.append(6, 7);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7 }, array);
    }

    @Test
    public void testAppendStream() {
        IntStream appendStream = createIntStream(new int[] { 6, 7, 8 });
        IntStream result = stream.append(appendStream);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, array);
    }

    @Test
    public void testAppendOptionalInt() {
        IntStream result = stream.append(OptionalInt.of(6));
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, array);
    }

    @Test
    public void testAppendEmptyOptionalInt() {
        IntStream result = stream.append(OptionalInt.empty());
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testAppendIfEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        IntStream result = emptyStream.appendIfEmpty(1, 2, 3);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3 }, array);
    }

    @Test
    public void testAppendIfEmptyNotEmpty() {
        IntStream result = stream.appendIfEmpty(10, 20);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testMergeWith() {
        IntStream otherStream = createIntStream(new int[] { 2, 4, 6 });
        IntStream result = stream.mergeWith(otherStream, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 2, 3, 4, 4, 5, 6 }, array);
    }

    @Test
    public void testZipWith() {
        IntStream otherStream = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        IntStream result = stream.zipWith(otherStream, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 11, 22, 33, 44, 55 }, array);
    }

    @Test
    public void testZipWithThreeStreams() {
        IntStream stream2 = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        IntStream stream3 = createIntStream(new int[] { 100, 200, 300, 400, 500 });
        IntStream result = stream.zipWith(stream2, stream3, (a, b, c) -> a + b + c);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 111, 222, 333, 444, 555 }, array);
    }

    @Test
    public void testZipWithDefault() {
        IntStream otherStream = createIntStream(new int[] { 10, 20 });
        IntStream result = stream.zipWith(otherStream, 0, 100, (a, b) -> a + b);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 11, 22, 103, 104, 105 }, array);
    }

    @Test
    public void testZipWithDefaultThreeStreams() {
        IntStream stream2 = createIntStream(new int[] { 10, 20 });
        IntStream stream3 = createIntStream(new int[] { 100 });
        IntStream result = stream.zipWith(stream2, stream3, 0, 50, 500, (a, b, c) -> a + b + c);
        int[] array = result.toArray();
        Assertions.assertArrayEquals(new int[] { 111, 522, 553, 554, 555 }, array);
    }

    @Test
    public void testToMap() throws Exception {
        Map<String, Integer> result = stream.toMap(x -> "key" + x, x -> x * 10);
        assertEquals(5, result.size());
        assertEquals(10, result.get("key1"));
        assertEquals(50, result.get("key5"));
    }

    @Test
    public void testToMapWithMapFactory() throws Exception {
        Map<String, Integer> result = stream.toMap(x -> "key" + x, x -> x * 10, Suppliers.ofLinkedHashMap());
        Assertions.assertTrue(result instanceof LinkedHashMap);
        assertEquals(5, result.size());
    }

    @Test
    public void testToMapWithMergeFunction() throws Exception {
        IntStream duplicateStream = createIntStream(new int[] { 1, 1, 2, 2, 3 });
        Map<String, Integer> result = duplicateStream.toMap(x -> "key" + x, x -> x, (a, b) -> a + b);
        assertEquals(3, result.size());
        assertEquals(2, result.get("key1"));
        assertEquals(4, result.get("key2"));
    }

    @Test
    public void testToMapWithMergeFunctionAndMapFactory() throws Exception {
        IntStream duplicateStream = createIntStream(new int[] { 1, 1, 2, 2, 3 });
        Map<String, Integer> result = duplicateStream.toMap(x -> "key" + x, x -> x, (a, b) -> a + b, TreeMap::new);
        Assertions.assertTrue(result instanceof TreeMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testGroupTo() throws Exception {
        Map<String, List<Integer>> result = stream.groupTo(x -> x % 2 == 0 ? "even" : "odd", Collectors.toList());
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2, 4), result.get("even"));
        assertEquals(Arrays.asList(1, 3, 5), result.get("odd"));
    }

    @Test
    public void testGroupToWithMapFactory() throws Exception {
        Map<String, Long> result = stream.groupTo(x -> x % 2 == 0 ? "even" : "odd", Collectors.counting(), TreeMap::new);
        Assertions.assertTrue(result instanceof TreeMap);
        assertEquals(2L, result.get("even"));
        assertEquals(3L, result.get("odd"));
    }

    @Test
    public void testForEachIndexed() throws Exception {
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:1", "1:2", "2:3", "3:4", "4:5"), result);
    }

    @Test
    public void testFirstEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        OptionalInt result = emptyStream.first();
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_MultipleElements() {
        stream = IntStream.of(1, 2, 3);
        OptionalInt result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void testFirst() {
        OptionalInt result = stream.first();
        Assertions.assertTrue(result.isPresent());
        assertEquals(1, result.orElseThrow());
    }

    @Test
    public void testLastEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        OptionalInt result = emptyStream.last();
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        OptionalInt result = stream.last();
        Assertions.assertTrue(result.isPresent());
        assertEquals(5, result.orElseThrow());
    }

    @Test
    public void testOnlyOneEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        OptionalInt result = emptyStream.onlyOne();
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        IntStream singleStream = createIntStream(new int[] { 42 });
        OptionalInt result = singleStream.onlyOne();
        Assertions.assertTrue(result.isPresent());
        assertEquals(42, result.orElseThrow());
    }

    @Test
    public void testOnlyOneTooMany() {
        Assertions.assertThrows(TooManyElementsException.class, () -> {
            stream.onlyOne();
        });
    }

    @Test
    public void testFindAny() throws Exception {
        OptionalInt result = stream.findAny(x -> x > 3);
        Assertions.assertTrue(result.isPresent());
        assertEquals(4, result.orElseThrow());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Integer>> result = stream.percentiles();
        Assertions.assertTrue(result.isPresent());
        Map<Percentage, Integer> percentiles = result.get();
        Assertions.assertTrue(percentiles.containsKey(Percentage._20));
        Assertions.assertTrue(percentiles.containsKey(Percentage._50));
        Assertions.assertTrue(percentiles.containsKey(Percentage._70));
    }

    @Test
    public void testPercentilesEmpty() {
        IntStream emptyStream = createIntStream(new int[] {});
        Optional<Map<Percentage, Integer>> result = emptyStream.percentiles();
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result = stream.summaryStatisticsAndPercentiles();

        IntSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum());

        Optional<Map<Percentage, Integer>> percentiles = result.right();
        Assertions.assertTrue(percentiles.isPresent());
    }

    @Test
    public void testJoin() {
        String result = stream.join(",", "[", "]");
        assertEquals("[1,2,3,4,5]", result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with("-");
        Joiner result = stream.joinTo(joiner);
        assertEquals("1-2-3-4-5", result.toString());
    }

    @Test
    public void testJoinToRejectsNullForEmptyStream() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createIntStream().joinTo(null));
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        List<Integer> result = stream.collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testIterator() {
        IntIterator iter = stream.iterator();
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextInt());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testReversedRotatedReverseSorted_CountExhaustsIterator() {
        // reversed(): count() must consume all remaining elements
        IntIteratorEx revIter = createIntStream(new int[] { 1, 2, 3, 4, 5 }).reversed().iteratorEx();
        assertEquals(5L, revIter.count());
        Assertions.assertFalse(revIter.hasNext());

        // rotated(): count() must consume all remaining elements
        IntIteratorEx rotIter = createIntStream(new int[] { 1, 2, 3, 4, 5 }).rotated(2).iteratorEx();
        assertEquals(5L, rotIter.count());
        Assertions.assertFalse(rotIter.hasNext());

        // reverseSorted(): count() must consume all remaining elements
        IntIteratorEx rsIter = createIntStream(new int[] { 3, 1, 4, 1, 5 }).reverseSorted().iteratorEx();
        assertEquals(5L, rsIter.count());
        Assertions.assertFalse(rsIter.hasNext());

        // After consuming a few elements first, count() returns the remaining count and exhausts the iterator
        IntIteratorEx revIter2 = createIntStream(new int[] { 1, 2, 3, 4, 5 }).reversed().iteratorEx();
        revIter2.nextInt();
        revIter2.nextInt();
        assertEquals(3L, revIter2.count());
        Assertions.assertFalse(revIter2.hasNext());
    }

    @Test
    public void testReversedRotatedReverseSorted_ToArrayExhaustsIterator() {
        IntIteratorEx reversed = createIntStream(new int[] { 1, 2, 3, 4, 5 }).reversed().iteratorEx();
        assertEquals(5, reversed.toArray().length);
        Assertions.assertFalse(reversed.hasNext());
        assertEquals(0, reversed.toArray().length);

        IntIteratorEx rotated = createIntStream(new int[] { 1, 2, 3, 4, 5 }).rotated(2).iteratorEx();
        rotated.nextInt();
        assertEquals(4, rotated.toArray().length);
        Assertions.assertFalse(rotated.hasNext());
        assertEquals(0, rotated.toArray().length);

        IntIteratorEx reverseSorted = createIntStream(new int[] { 3, 1, 4, 1, 5 }).reverseSorted().iteratorEx();
        reverseSorted.nextInt();
        reverseSorted.nextInt();
        assertEquals(3, reverseSorted.toArray().length);
        Assertions.assertFalse(reverseSorted.hasNext());
        assertEquals(0, reverseSorted.toArray().length);
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0, createIntStream().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 42 },
                createIntStream(42).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 5 },
                createIntStream(1, 2, 3, 4, 5).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 1, 2, 3 },
                createIntStream(1, 2, 3).delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray());
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createIntStream(1, 2, 3).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createIntStream(1, 2, 3).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createIntStream(1, 2, 3).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }
    // ------------------------------------------------------------------------------------------------------
    // Stream review 2026-09-09 (pass B) - prepend/append(OptionalInt) argument validation
    // ------------------------------------------------------------------------------------------------------

    /**
     * r9509 dropped {@code checkArgNotNull(op, cs.op)} from {@code prepend}/{@code append(OptionalXxx)} in all
     * seven primitive {@code Abstract*Stream}s while leaving it on the object {@code Stream}
     * ({@code AbstractStream.append(Optional)}). A null argument therefore threw a raw {@code NullPointerException}
     * and left the stream open, against the package contract at {@code Stream.java:327-336}.
     */
    @Test
    public void testPrependAppendOptional_nullIsRejectedAndTheStreamIsClosed() {
        final MutableBoolean closed = MutableBoolean.of(false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> IntStream.of(1, 2).onClose(closed::setTrue).prepend((OptionalInt) null));
        Assertions.assertTrue(closed.isTrue(), "prepend(null) must close the stream before throwing");

        closed.setFalse();
        Assertions.assertThrows(IllegalArgumentException.class, () -> IntStream.of(1, 2).onClose(closed::setTrue).append((OptionalInt) null));
        Assertions.assertTrue(closed.isTrue(), "append(null) must close the stream before throwing");

        // the ordinary paths are unaffected
        assertArrayEquals(new int[] { 9, 1, 2 }, IntStream.of(1, 2).prepend(OptionalInt.of(9)).toArray());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(1, 2).prepend(OptionalInt.empty()).toArray());
        assertArrayEquals(new int[] { 1, 2, 9 }, IntStream.of(1, 2).append(OptionalInt.of(9)).toArray());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(1, 2).append(OptionalInt.empty()).toArray());
    }

    // ---- perf review 2026-09-26 G085 begin ----
    // G085-01: rotated() nextX()/toArray() replaced the per-element modulo with a conditional subtraction and two bulk copies.
    @Test
    public void testRotated_nextAndToArrayMatchModuloOracle() {
        final int[] distances = { 0, 1, 2, 3, 5, 7, 8, 13, -1, -2, -3, -7, -8, -13, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE + 1 };

        for (int len = 0; len <= 8; len++) {
            final int from = 2;
            final int[] backing = new int[len + 5];

            for (int i = 0; i < backing.length; i++) {
                backing[i] = i * 10 - 7;
            }

            for (final int distance : distances) {
                final int[] expected = new int[len];

                for (int i = 0; i < len; i++) {
                    expected[i] = backing[from + (int) Math.floorMod((long) i - distance, (long) len)];
                }

                for (int source = 0; source < 2; source++) {
                    final String msg = "len=" + len + ", distance=" + distance + ", source=" + source;
                    final int[] copy = Arrays.copyOfRange(backing, from, from + len);

                    // full toArray()
                    assertArrayEquals(expected,
                            (source == 0 ? IntStream.of(backing, from, from + len) : IntStream.of(IntIterator.of(copy))).rotated(distance).toArray(), msg);

                    // next() for every element, then partial consumption followed by toArray() / advance() followed by toArray()
                    for (int k = 0; k <= len + 1; k++) {
                        final IntIteratorEx byNext = (source == 0 ? IntStream.of(backing, from, from + len) : IntStream.of(IntIterator.of(copy)))
                                .rotated(distance)
                                .iteratorEx();

                        for (int j = 0; j < Math.min(k, len); j++) {
                            assertEquals(expected[j], byNext.nextInt(), msg + ", k=" + k + ", j=" + j);
                        }

                        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(k, len), len), byNext.toArray(), msg + ", k=" + k);
                        org.junit.jupiter.api.Assertions.assertFalse(byNext.hasNext(), msg);

                        final IntIteratorEx byAdvance = (source == 0 ? IntStream.of(backing, from, from + len) : IntStream.of(IntIterator.of(copy)))
                                .rotated(distance)
                                .iteratorEx();
                        byAdvance.advance(k);
                        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(k, len), len), byAdvance.toArray(), msg + ", advance=" + k);
                    }
                }
            }
        }
    }
    // ---- perf review 2026-09-26 G085 end ----

    // ---- deep review 2026-11-02b G090 begin ----
    // G090-01 (perf pin): sorted()/shuffled(Random) results read through toArray() - empty, single and several elements;
    // array, iterator, mapped and parallel sources; and the reversed()/skip()/limit() stages that read the snapshot again.
    @Test
    public void testSortedShuffledToArray_sourcesAndSizes_g090b() {
        final int[][] inputs = { {}, { 7 }, { 3, -1, 2, -1, 9, 0 }, { 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, 12 } };

        for (final int[] a : inputs) {
            final int[] expected = a.clone();
            java.util.Arrays.sort(expected);
            final int[] descending = new int[expected.length];

            for (int i = 0; i < expected.length; i++) {
                descending[i] = expected[expected.length - 1 - i];
            }

            final int k = Math.min(2, a.length);
            final int[] original = a.clone();

            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, IntStream.of(a).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(original, a);
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, IntStream.of(com.landawn.abacus.util.IntIterator.of(a.clone())).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, IntStream.of(a.clone()).map(x -> x).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, IntStream.of(a.clone()).parallel(2).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(descending, IntStream.of(a.clone()).map(x -> x).sorted().reversed().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(java.util.Arrays.copyOfRange(expected, k, expected.length),
                    IntStream.of(a.clone()).sorted().skip(2).toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(java.util.Arrays.copyOf(expected, k), IntStream.of(a.clone()).sorted().limit(2).toArray());

            final int[] shuffledExpected = a.clone();
            com.landawn.abacus.util.N.shuffle(shuffledExpected, new Random(42));
            org.junit.jupiter.api.Assertions.assertArrayEquals(shuffledExpected, IntStream.of(a.clone()).shuffled(new Random(42)).toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(shuffledExpected,
                    IntStream.of(com.landawn.abacus.util.IntIterator.of(a.clone())).shuffled(new Random(42)).toArray());
        }
    }

    // G090-01 (perf pin): toArray() on a partially consumed sorted() iterator returns exactly the remaining elements and
    // exhausts the iterator; an exhausted or empty one returns an empty array.
    @Test
    public void testSortedIterator_toArrayAfterPartialConsumption_g090b() {
        final com.landawn.abacus.util.IntIterator iter = IntStream.of(5, 3, 9, 1, 7).map(x -> x).sorted().iterator();
        org.junit.jupiter.api.Assertions.assertEquals(1, iter.nextInt());
        org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
        org.junit.jupiter.api.Assertions.assertEquals(3, iter.nextInt());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 5, 7, 9 }, iter.toArray());
        org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
        org.junit.jupiter.api.Assertions.assertEquals(0, iter.toArray().length);
        org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextInt);

        final com.landawn.abacus.util.IntIterator emptyIter = IntStream.of(com.landawn.abacus.util.IntIterator.of(new int[0])).sorted().iterator();
        org.junit.jupiter.api.Assertions.assertEquals(0, emptyIter.toArray().length);
        org.junit.jupiter.api.Assertions.assertFalse(emptyIter.hasNext());
    }

    // G090-01 (perf pin): sorted() stays lazy, and a failure while loading its snapshot is rethrown as the same instance by
    // every later access (toArray() included) without reading the source again.
    @Test
    public void testSortedIterator_loadFailureRethrownByEveryAccess_g090b() {
        final int[] calls = { 0 };
        final com.landawn.abacus.util.IntIterator iter = IntStream.of(1, 2, 3).map(x -> {
            calls[0]++;

            if (x == 2) {
                throw new IllegalStateException("load failed");
            }

            return x;
        }).sorted().iterator();

        org.junit.jupiter.api.Assertions.assertEquals(0, calls[0]);

        final IllegalStateException first = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::toArray);
        org.junit.jupiter.api.Assertions.assertEquals("load failed", first.getMessage());
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::toArray));
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::hasNext));
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::nextInt));
        org.junit.jupiter.api.Assertions.assertEquals(2, calls[0]);
    }

    // G090-01 (perf pin): the snapshot is loaded once on the terminal toArray() and the source's close handler runs once.
    @Test
    public void testSortedToArray_loadsOnceAndClosesSourceOnce_g090b() {
        final int[] closed = { 0 };
        final int[] calls = { 0 };
        final IntStream sorted = IntStream.of(3, 1, 2).map(x -> {
            calls[0]++;
            return x;
        }).onClose(() -> closed[0]++).sorted();

        org.junit.jupiter.api.Assertions.assertEquals(0, calls[0]);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 1, 2, 3 }, sorted.toArray());
        org.junit.jupiter.api.Assertions.assertEquals(3, calls[0]);
        org.junit.jupiter.api.Assertions.assertEquals(1, closed[0]);
    }

    // ---- deep review 2026-11-02b G090 end ----
    // ---- deep review 2026-11-02b G123 begin ----
    private static IntList drainG123(final IntIterator iter) {
        final IntList result = new IntList();

        while (iter.hasNext()) {
            result.add(iter.nextInt());
        }

        return result;
    }

    private static String outcomeG123(final java.util.function.Supplier<IntList> action) {
        try {
            return "OK " + Arrays.toString(action.get().toArray());
        } catch (final RuntimeException e) {
            return "EX " + e.getClass().getName();
        }
    }

    private static IntIterator brokenSourceOpG123(final int op) {
        final IntIterator failingSource = new IntIterator() {
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (cnt >= 2) {
                    throw new IllegalStateException("source failure");
                }

                return true;
            }

            @Override
            public int nextInt() {
                return (int) cnt++;
            }
        };

        final IntStream stream = IntStream.of(failingSource);

        return (op == 0 ? stream.reversed() : op == 1 ? stream.reverseSorted() : stream.rotated(1)).iteratorEx();
    }

    // G123-01 (perf pin, port of G091-03): reversed()/rotated()/reverseSorted() iterators build toList() (used by toIntList())
    // from an exactly sized copy; pin it against the element-by-element walk for empty/single/partially consumed/sub-range/
    // iterator/parallel sources and rotate distances beyond the size.
    @Test
    public void testReversedRotatedReverseSorted_toIntList_g123() {
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).reversed().toIntList().toArray());
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).rotated(2).toIntList().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1 }, IntStream.of(1, 2, 3, 4, 5).rotated(-1).toIntList().toArray());
        assertArrayEquals(new int[] { 3, 2, 1 }, IntStream.of(3, 1, 2).reverseSorted().toIntList().toArray());
        assertTrue(IntStream.empty().reversed().toIntList().isEmpty());
        assertTrue(IntStream.empty().rotated(3).toIntList().isEmpty());
        assertTrue(IntStream.empty().reverseSorted().toIntList().isEmpty());
        final int[] source = { 3, 1, 2 };
        final IntList reversed = IntStream.of(source).reversed().toIntList();
        reversed.set(0, 100);
        reversed.add(7);
        final IntList rotated = IntStream.of(source).rotated(1).toIntList();
        rotated.set(0, 100);
        final IntList reverseSorted = IntStream.of(source).reverseSorted().toIntList();
        reverseSorted.set(0, 100);
        assertArrayEquals(new int[] { 3, 1, 2 }, source);
        assertArrayEquals(new int[] { 100, 1, 3, 7 }, reversed.toArray());

        final int[] distances = { Integer.MIN_VALUE, -7, -1, 0, 1, 2, 6, Integer.MAX_VALUE };
        final List<java.util.function.UnaryOperator<IntStream>> ops = new ArrayList<>();
        ops.add(IntStream::reversed);
        ops.add(IntStream::reverseSorted);

        for (final int distance : distances) {
            ops.add(s -> s.rotated(distance));
        }

        for (int len = 0; len <= 6; len++) {
            final int n = len;
            final int[] padded = new int[n + 2];

            for (int i = 0; i < padded.length; i++) {
                padded[i] = (i * 5) % 7 - 3;
            }

            final int[] a = Arrays.copyOfRange(padded, 1, 1 + n);
            final List<java.util.function.Supplier<IntStream>> sources = List.of(() -> IntStream.of(a.clone()), () -> IntStream.of(padded, 1, 1 + n),
                    () -> IntStream.of(IntList.of(a.clone()).iterator()), () -> IntStream.of(a.clone()).parallel(2));

            for (final java.util.function.Supplier<IntStream> src : sources) {
                for (final java.util.function.UnaryOperator<IntStream> op : ops) {
                    assertArrayEquals(drainG123(op.apply(src.get()).iteratorEx()).toArray(), op.apply(src.get()).toIntList().toArray());

                    for (int consumed = 0; consumed <= n + 1; consumed++) {
                        final IntIterator expectedIter = op.apply(src.get()).iteratorEx();
                        final IntIterator iter = op.apply(src.get()).iteratorEx();

                        for (int i = 0; i < consumed && iter.hasNext(); i++) {
                            assertEquals(expectedIter.nextInt(), iter.nextInt());
                        }

                        final IntList rest = iter.toList();
                        assertArrayEquals(drainG123(expectedIter).toArray(), rest.toArray());
                        Assertions.assertFalse(iter.hasNext());
                        assertTrue(iter.toList().isEmpty());
                        rest.add(7);
                        assertEquals(7, rest.get(rest.size() - 1));
                    }
                }
            }
        }
    }

    // G123-01 (perf pin): after a failed materialization, toList() must keep behaving like the element-by-element walk on every
    // retry (reversed() keeps the inherited walk while its backing array is still null).
    @Test
    public void testReversedRotatedReverseSorted_toListRetryAfterFailure_g123() {
        for (int op = 0; op < 3; op++) {
            final IntIterator viaToList = brokenSourceOpG123(op);
            final IntIterator viaWalk = brokenSourceOpG123(op);

            assertEquals("EX java.lang.IllegalStateException", outcomeG123(viaToList::toList));
            assertEquals("EX java.lang.IllegalStateException", outcomeG123(() -> drainG123(viaWalk)));

            for (int retry = 0; retry < 3; retry++) {
                assertEquals(outcomeG123(() -> drainG123(viaWalk)), outcomeG123(viaToList::toList), "op=" + op + ", retry=" + retry);
            }
        }
    }
    // ---- deep review 2026-11-02b G123 end ----

    private static int[] sortedCopy(final int[] a) {
        final int[] copy = a.clone();
        N.sort(copy);
        return copy;
    }

    @Test
    public void testRateLimitedParallel() {
        final int[] result = IntStream.of(1, 2, 3).parallel().rateLimited(RateLimiter.create(10000)).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, sortedCopy(result));
    }

    @Test
    public void testDelayParallel() {
        final int[] result = IntStream.of(1, 2).parallel().delay(java.time.Duration.ofMillis(1)).toArray();
        assertArrayEquals(new int[] { 1, 2 }, sortedCopy(result));
    }

    @Test
    public void testDebounceIteratorExhausted() {
        final IntIterator iter = IntStream.of(1, 2).debounce(com.landawn.abacus.util.Duration.ofMillis(1)).iterator();
        int last = -1;
        while (iter.hasNext()) {
            last = iter.nextInt();
        }
        assertEquals(2, last);
        assertThrows(NoSuchElementException.class, iter::nextInt);
    }

    @Test
    public void testMapMultiFailedMapperCanBeRetried() {
        final AtomicBoolean fail = new AtomicBoolean(true);
        final IntIterator iter = IntStream.of(1, 2).mapMulti((x, consumer) -> {
            consumer.accept(x * 10);
            if (x == 1 && fail.getAndSet(false)) {
                throw new IllegalStateException("boom");
            }
        }).iterator();

        assertThrows(IllegalStateException.class, iter::hasNext);
        assertTrue(iter.hasNext());
        assertEquals(20, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMapMultiLargeExpansionReleasesBuffer() {
        final long count = IntStream.of(1, 2).mapMulti((x, consumer) -> {
            final int n = x == 1 ? 10241 : 1;
            for (int i = 0; i < n; i++) {
                consumer.accept(i);
            }
        }).count();
        assertEquals(10242L, count);
    }

    @Test
    public void testCycledIteratorStreamHasNextAfterEnd() {
        final IntIterator iter = IntStream.of(IntIterator.of(1, 2)).cycled(2).iterator();
        int n = 0;
        while (iter.hasNext()) {
            iter.nextInt();
            n++;
        }
        assertEquals(4, n);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeWithParallel() {
        final int[] result = IntStream.of(1, 3)
                .parallel()
                .mergeWith(IntStream.of(2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, sortedCopy(result));
    }

    @Test
    public void testPercentilesParallelUnsorted() {
        final Optional<Map<Percentage, Integer>> p = IntStream.of(IntIterator.of(3, 1, 2)).parallel().percentiles();
        assertTrue(p.isPresent());
        assertEquals(3, p.get().get(Percentage._99_9999));

        final Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> sp = IntStream.of(IntIterator.of(3, 1, 2))
                .parallel()
                .summaryStatisticsAndPercentiles();
        assertEquals(6L, sp.left().getSum());
        assertTrue(sp.right().isPresent());
    }

    @Test
    public void testSummaryStatisticsAndPercentilesEmptyUnsorted() {
        final Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> sp = IntStream.of(IntIterator.of(new int[0])).summaryStatisticsAndPercentiles();
        assertEquals(0L, sp.left().getCount());
        assertFalse(sp.right().isPresent());
    }
}
