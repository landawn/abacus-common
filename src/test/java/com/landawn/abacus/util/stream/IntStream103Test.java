package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;

public class IntStream103Test extends TestBase {

    // This method needs to be implemented by a concrete test class to provide a IntStream instance.
    // For example, in ArrayIntStreamTest, it would return new ArrayIntStream(a);
    // In IteratorIntStreamTest, it would return new IteratorIntStream(IntegerIterator.of(a));
    protected IntStream createIntStream(int... a) {
        return IntStream.of(a).map(e -> e + 0);
    }

    protected IntStream createIntStream(int[] a, int fromIndex, int toIndex) {
        return IntStream.of(a, fromIndex, toIndex).map(e -> e + 0);
    }

    protected IntStream createIntStream(Integer[] a) {
        return IntStream.of(a).map(e -> e + 0);
    }

    protected IntStream createIntStream(Integer[] a, int fromIndex, int toIndex) {
        return IntStream.of(a, fromIndex, toIndex).map(e -> e + 0);
    }

    protected IntStream createIntStream(Collection<Integer> coll) {
        return IntStream.of(coll.toArray(new Integer[coll.size()])).map(e -> e + 0);
    }

    protected IntStream createIntStream(IntIterator iter) {
        return iter == null ? IntStream.empty() : IntStream.of(iter.toArray()).map(e -> e + 0);
    }

    @Test
    public void testMapMulti() {
        IntStream stream = createIntStream(1, 2, 3).mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        });
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testMapPartial() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5).mapPartial(n -> n % 2 == 0 ? OptionalInt.of(n * 2) : OptionalInt.empty());
        assertArrayEquals(new int[] { 4, 8 }, stream.toArray());
    }

    @Test
    public void testMapPartialJdk() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5).mapPartialJdk(n -> n % 2 == 0 ? java.util.OptionalInt.of(n * 2) : java.util.OptionalInt.empty());
        assertArrayEquals(new int[] { 4, 8 }, stream.toArray());
    }

    @Test
    public void testRangeMap() {
        IntStream stream = createIntStream(1, 1, 2, 2, 2, 3).rangeMap((a, b) -> a == b, (first, last) -> first * 10 + last);
        assertArrayEquals(new int[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testRangeMapToObj() {
        Stream<String> stream = createIntStream(1, 1, 2, 2, 2, 3).rangeMapToObj((a, b) -> a == b, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "1-1", "2-2", "3-3" }, stream.toArray());
    }

    // Tests for Complex Transformations

    @Test
    public void testFlatMapToChar() {
        CharStream charStream = createIntStream(65, 66, 67).flatMapToChar(n -> CharStream.of((char) n, (char) (n + 32)));
        assertArrayEquals(new char[] { 'A', 'a', 'B', 'b', 'C', 'c' }, charStream.toArray());
    }

    @Test
    public void testFlatMapToByte() {
        ByteStream byteStream = createIntStream(1, 2, 3).flatMapToByte(n -> ByteStream.of((byte) n, (byte) (n * 10)));
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, byteStream.toArray());
    }

    @Test
    public void testFlatMapToShort() {
        ShortStream shortStream = createIntStream(1, 2, 3).flatMapToShort(n -> ShortStream.of((short) n, (short) (n * 100)));
        assertArrayEquals(new short[] { 1, 100, 2, 200, 3, 300 }, shortStream.toArray());
    }

    @Test
    public void testFlatMapToFloat() {
        FloatStream floatStream = createIntStream(1, 2, 3).flatMapToFloat(n -> FloatStream.of(n * 0.1f, n * 0.2f));
        assertArrayEquals(new float[] { 0.1f, 0.2f, 0.2f, 0.4f, 0.3f, 0.6f }, floatStream.toArray(), 0.001f);
    }

    // Tests for Grouping and Collecting

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Integer>> grouped = createIntStream(1, 2, 3, 4, 5, 6).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList());

        assertEquals(Arrays.asList(2, 4, 6), grouped.get(true));
        assertEquals(Arrays.asList(1, 3, 5), grouped.get(false));
    }

    @Test
    public void testToMap() {
        Map<String, Integer> map = createIntStream(1, 2, 3).toMap(n -> "key" + n, n -> n * 10);

        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(10), map.get("key1"));
        assertEquals(Integer.valueOf(20), map.get("key2"));
        assertEquals(Integer.valueOf(30), map.get("key3"));
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<Boolean, Integer> map = createIntStream(1, 2, 3, 4, 5, 6).toMap(n -> n % 2 == 0, n -> n, Integer::sum);

        assertEquals(Integer.valueOf(12), map.get(true)); // 2 + 4 + 6
        assertEquals(Integer.valueOf(9), map.get(false)); // 1 + 3 + 5
    }

    @Test
    public void testToMultiset() {
        Multiset<Integer> multiset = createIntStream(1, 2, 2, 3, 3, 3).toMultiset();

        assertEquals(1, multiset.occurrencesOf(1));
        assertEquals(2, multiset.occurrencesOf(2));
        assertEquals(3, multiset.occurrencesOf(3));
    }

    // Tests for Collapse Operations

    @Test
    public void testCollapseWithMergeFunction() {
        IntStream stream = createIntStream(1, 1, 2, 2, 2, 3, 3).collapse((a, b) -> a == b, Integer::sum);
        assertArrayEquals(new int[] { 2, 6, 6 }, stream.toArray());
    }

    @Test
    public void testCollapseWithTriPredicate() {
        IntStream stream = createIntStream(1, 2, 3, 5, 6, 8, 9).collapse((first, prev, curr) -> curr - first <= 2, Integer::max);
        assertArrayEquals(new int[] { 3, 6, 9 }, stream.toArray());
    }

    // Tests for Advanced Stream Operations

    @Test
    public void testScanWithInit() {
        IntStream stream = createIntStream(1, 2, 3, 4).scan(10, Integer::sum);
        assertArrayEquals(new int[] { 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testScanWithInitIncluded() {
        IntStream stream = createIntStream(1, 2, 3, 4).scan(10, true, Integer::sum);
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testTopWithComparator() {
        IntStream stream = createIntStream(5, 2, 8, 1, 9, 3, 7).top(3, (a, b) -> b - a);
        int[] bottom3 = stream.sorted().toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, bottom3);
    }

    // Tests for Rate Limiting and Delay

    @Test
    public void testRateLimited() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(5); // 5 permits per second
        long startTime = System.currentTimeMillis();

        createIntStream(1, 2, 3).rateLimited(rateLimiter).count();

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 400, "Rate limiting should introduce delay"); // At least 400ms for 3 items at 5/sec
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(100);
        long startTime = System.currentTimeMillis();

        createIntStream(1, 2, 3).delay(delay).count();

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 200, "Delay should be applied"); // At least 200ms for 2 delays
    }

    // Tests for Advanced Filtering

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).filter(n -> n % 2 == 0, dropped::add);

        assertArrayEquals(new int[] { 2, 4 }, stream.toArray());
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).skip(2, skipped::add);

        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).dropWhile(n -> n < 3, dropped::add);

        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    // Tests for Zipping Operations

    @Test
    public void testZipWithDefault() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20);
        IntStream zipped = s1.zipWith(s2, 0, 100, Integer::sum);

        assertArrayEquals(new int[] { 11, 22, 103 }, zipped.toArray());
    }

    @Test
    public void testZipThreeStreams() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20, 30);
        IntStream s3 = createIntStream(100, 200, 300);
        IntStream zipped = s1.zipWith(s2, s3, (a, b, c) -> a + b + c);

        assertArrayEquals(new int[] { 111, 222, 333 }, zipped.toArray());
    }

    // Tests for Iterator-based Operations

    @Test
    public void testFromIterator() {
        IntIterator iter = new IntIteratorEx() {
            private int n = 0;

            @Override
            public boolean hasNext() {
                return n < 5;
            }

            @Override
            public int nextInt() {
                return n++;
            }
        };

        IntStream stream = createIntStream(iter);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        IntStream stream = IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement());

        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    // Tests for Special Find Operations

    @Test
    public void testFindFirstWithPredicate() throws Exception {
        OptionalInt found = createIntStream(1, 2, 3, 4, 5).findFirst(n -> n > 3);
        assertTrue(found.isPresent());
        assertEquals(4, found.getAsInt());
    }

    @Test
    public void testFindLastWithPredicate() throws Exception {
        OptionalInt found = createIntStream(1, 2, 3, 4, 5).findLast(n -> n < 4);
        assertTrue(found.isPresent());
        assertEquals(3, found.getAsInt());
    }

    // Tests for Append/Prepend Operations

    @Test
    public void testAppendIfEmpty() {
        // Non-empty stream should not append
        IntStream stream = createIntStream(1, 2, 3).appendIfEmpty(4, 5, 6);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        // Empty stream should append
        stream = IntStream.empty().appendIfEmpty(4, 5, 6);
        assertArrayEquals(new int[] { 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testAppendPrependWithOptional() {
        IntStream stream = createIntStream(2, 3).prepend(OptionalInt.of(1));
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        stream = createIntStream(1, 2).append(OptionalInt.of(3));
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        // Empty optional should not affect stream
        stream = createIntStream(1, 2).append(OptionalInt.empty());
        assertArrayEquals(new int[] { 1, 2 }, stream.toArray());
    }

    // Tests for ForEach with Index

    @Test
    public void testForEachIndexed() throws Exception {
        Map<Integer, Integer> indexToValue = new HashMap<>();
        createIntStream(10, 20, 30).forEachIndexed((index, value) -> {
            indexToValue.put(index, value);
        });

        assertEquals(3, indexToValue.size());
        assertEquals(Integer.valueOf(10), indexToValue.get(0));
        assertEquals(Integer.valueOf(20), indexToValue.get(1));
        assertEquals(Integer.valueOf(30), indexToValue.get(2));
    }

    // Tests for Summary Statistics with Percentiles

    @Test
    public void testSummarizeAndPercentiles() {
        Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result = IntStream.range(1, 101).summarizeAndPercentiles();

        IntSummaryStatistics stats = result.left();
        assertEquals(100, stats.getCount());
        assertEquals(5050, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(100, stats.getMax());

        assertTrue(result.right().isPresent());
        Map<Percentage, Integer> percentiles = result.right().get();
        assertNotNull(percentiles);
    }

    // Tests for Flatten Operations

    @Test
    public void testFlatten() {
        int[][] arrays = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        IntStream stream = IntStream.flatten(arrays);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFlattenVertically() {
        int[][] arrays = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        IntStream stream = IntStream.flatten(arrays, true);
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, stream.toArray());
    }

    @Test
    public void testFlattenWithPadding() {
        int[][] arrays = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        IntStream stream = IntStream.flatten(arrays, 0, false);
        assertArrayEquals(new int[] { 1, 2, 0, 3, 4, 5, 6, 0, 0 }, stream.toArray());
    }

    // Tests for Code Points

    @Test
    public void testOfCodePoints() {
        IntStream stream = IntStream.ofCodePoints("Hello");
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, stream.toArray());
    }

    // Tests for Parallel Stream with Custom Settings

    @Test
    public void testParallelWithMaxThreadNum() {
        IntStream stream = IntStream.range(0, 1000).parallel(4);
        assertTrue(stream.isParallel());
        assertEquals(499500, stream.sum());
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            IntStream stream = IntStream.range(0, 100).parallel(executor);
            assertTrue(stream.isParallel());
            assertEquals(4950, stream.sum());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSps() {
        // Test switching to parallel for specific operation
        IntStream stream = IntStream.range(0, 100);
        IntStream result = stream.sps(s -> s.filter(n -> n % 2 == 0));
        assertFalse(result.isParallel());
        assertEquals(50, result.count());
    }

    // Tests for Transform Operations

    @Test
    public void testTransform() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5);
        int sum = stream.transform(s -> s.filter(n -> n % 2 == 0)).sum();
        assertEquals(6, sum); // 2 + 4
    }

    // Tests for Apply/Accept If Not Empty

    @Test
    public void testApplyIfNotEmpty() throws Exception {
        Optional<Integer> result = createIntStream(1, 2, 3, 4, 5).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(15), result.get());

        result = IntStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);

        createIntStream(1, 2, 3).acceptIfNotEmpty(s -> sum.set(s.sum()));
        assertEquals(6, sum.get());

        sum.set(0);
        IntStream.empty().acceptIfNotEmpty(s -> sum.set(s.sum())).orElse(() -> sum.set(-1));
        assertEquals(-1, sum.get());
    }

    // Tests for IfEmpty

    @Test
    public void testIfEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);

        createIntStream(1, 2, 3).ifEmpty(() -> called.set(true)).count();
        assertFalse(called.get());

        IntStream.empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());
    }

    // Edge Cases and Error Conditions

    @Test
    public void testEmptyStreamOperations() {
        IntStream empty = IntStream.empty();

        assertEquals(0, empty.sum());
        assertEquals(0, IntStream.empty().count());
        assertFalse(IntStream.empty().min().isPresent());
        assertFalse(IntStream.empty().max().isPresent());
        assertFalse(IntStream.empty().average().isPresent());
        assertFalse(IntStream.empty().first().isPresent());
        assertFalse(IntStream.empty().last().isPresent());
        assertTrue(IntStream.empty().allMatch(n -> false));
        assertFalse(IntStream.empty().anyMatch(n -> true));
        assertTrue(IntStream.empty().noneMatch(n -> true));
    }

    @Test
    public void testLargeStreamOperations() {
        // Test with large stream
        int size = 10_000;
        IntStream stream = IntStream.range(0, size);

        assertEquals(size, stream.count());

        stream = IntStream.range(0, size);
        long expectedSum = (long) size * (size - 1) / 2;
        assertEquals(expectedSum, stream.sum());
    }

    @Test
    public void testInfiniteStreamWithLimit() {
        IntStream stream = IntStream.iterate(1, n -> n + 1).limit(10);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, stream.toArray());
    }
}
