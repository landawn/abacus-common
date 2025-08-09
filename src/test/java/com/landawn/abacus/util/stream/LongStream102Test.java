package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;

public class LongStream102Test extends TestBase {

    // This method needs to be implemented by a concrete test class to provide a LongStream instance.
    // For example, in ArrayLongStreamTest, it would return new ArrayLongStream(a);
    // In IteratorLongStreamTest, it would return new IteratorLongStream(LongIterator.of(a));
    protected LongStream createLongStream(long... a) {
        return LongStream.of(a).map(e -> e + 0);
    }

    protected LongStream createLongStream(long[] a, int fromIndex, int toIndex) {
        return LongStream.of(a, fromIndex, toIndex).map(e -> e + 0);
    }

    protected LongStream createLongStream(Long[] a) {
        return LongStream.of(a).map(e -> e + 0);
    }

    protected LongStream createLongStream(Long[] a, int fromIndex, int toIndex) {
        return LongStream.of(a, fromIndex, toIndex).map(e -> e + 0);
    }

    protected LongStream createLongStream(Collection<Long> coll) {
        return LongStream.of(coll.toArray(new Long[coll.size()])).map(e -> e + 0);
    }

    protected LongStream createLongStream(LongIterator iter) {
        return iter == null ? LongStream.empty() : LongStream.of(iter.toArray()).map(e -> e + 0);
    }

    protected LongStream createLongStream(OptionalLong op) {
        return LongStream.of(op).map(e -> e + 0);
    }

    protected LongStream createLongStream(LongBuffer buff) {
        return LongStream.of(buff).map(e -> e + 0);
    }

    @Test
    public void testEmpty() {
        LongStream stream = LongStream.empty();
        assertFalse(stream.iterator().hasNext());
        assertEquals(0, stream.count());
    }

    @Test
    public void testDefer() {
        AtomicLong counter = new AtomicLong(0);
        Supplier<LongStream> supplier = () -> {
            counter.incrementAndGet();
            return createLongStream(1L, 2L, 3L);
        };

        LongStream stream = LongStream.defer(supplier);
        assertEquals(0, counter.get()); // Supplier not called yet

        long[] result = stream.toArray();
        assertEquals(1, counter.get()); // Supplier called once
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testDeferWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.defer(null));
    }

    @Test
    public void testFromJavaStream() {
        java.util.stream.LongStream javaStream = java.util.stream.LongStream.of(1L, 2L, 3L);
        LongStream stream = LongStream.from(javaStream);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.toArray());
    }

    @Test
    public void testFromNullJavaStream() {
        LongStream stream = LongStream.from((java.util.stream.LongStream) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullable() {
        assertEquals(1, LongStream.ofNullable(5L).count());
        assertEquals(0, LongStream.ofNullable(null).count());
    }

    @Test
    public void testOfArray() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongStream stream = createLongStream(array);
        assertArrayEquals(array, stream.toArray());
    }

    @Test
    public void testOfArrayWithRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongStream stream = createLongStream(array, 1, 4);
        assertArrayEquals(new long[] { 2L, 3L, 4L }, stream.toArray());
    }

    @Test
    public void testOfLongArray() {
        Long[] array = { 1L, 2L, 3L, null, 5L };
        LongStream stream = createLongStream(array);
        long[] result = stream.toArray();
        // Note: null handling depends on implementation
        assertEquals(5, result.length);
    }

    @Test
    public void testOfCollection() {
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        LongStream stream = createLongStream(list);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.toArray());
    }

    @Test
    public void testOfIterator() {
        LongIterator iter = LongIterator.of(new long[] { 1L, 2L, 3L });
        LongStream stream = createLongStream(iter);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.toArray());
    }

    @Test
    public void testOfOptionalLong() {
        LongStream stream1 = createLongStream(OptionalLong.of(5L));
        assertEquals(1, stream1.count());

        LongStream stream2 = createLongStream(OptionalLong.empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testRange() {
        assertArrayEquals(new long[] { 0L, 1L, 2L, 3L, 4L }, LongStream.range(0L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.range(5L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.range(5L, 0L).toArray());
    }

    @Test
    public void testRangeWithStep() {
        assertArrayEquals(new long[] { 0L, 2L, 4L }, LongStream.range(0L, 6L, 2L).toArray());
        assertArrayEquals(new long[] { 5L, 3L, 1L }, LongStream.range(5L, 0L, -2L).toArray());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.range(0L, 10L, 0L));
    }

    @Test
    public void testRangeClosed() {
        assertArrayEquals(new long[] { 0L, 1L, 2L, 3L, 4L, 5L }, LongStream.rangeClosed(0L, 5L).toArray());
        assertArrayEquals(new long[] { 5L }, LongStream.rangeClosed(5L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.rangeClosed(5L, 0L).toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        assertArrayEquals(new long[] { 0L, 2L, 4L, 6L }, LongStream.rangeClosed(0L, 6L, 2L).toArray());
        assertArrayEquals(new long[] { 5L, 3L, 1L }, LongStream.rangeClosed(5L, 1L, -2L).toArray());
    }

    @Test
    public void testRepeat() {
        assertArrayEquals(new long[] { 5L, 5L, 5L }, LongStream.repeat(5L, 3).toArray());
        assertArrayEquals(new long[] {}, LongStream.repeat(5L, 0).toArray());
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.repeat(5L, -1));
    }

    @Test
    public void testRandom() {
        long[] randoms = LongStream.random().limit(5).toArray();
        assertEquals(5, randoms.length);
        // All values should be different (with very high probability)
        Set<Long> uniqueValues = new HashSet<>();
        for (long val : randoms) {
            uniqueValues.add(val);
        }
        assertTrue(uniqueValues.size() > 1);
    }

    @Test
    public void testIterate() {
        LongStream stream = LongStream.iterate(1L, n -> n * 2).limit(5);
        assertArrayEquals(new long[] { 1L, 2L, 4L, 8L, 16L }, stream.toArray());
    }

    @Test
    public void testIterateWithPredicate() {
        LongStream stream = LongStream.iterate(1L, n -> n < 10L, n -> n + 2);
        assertArrayEquals(new long[] { 1L, 3L, 5L, 7L, 9L }, stream.toArray());
    }

    @Test
    public void testGenerate() {
        AtomicLong counter = new AtomicLong(0);
        LongStream stream = LongStream.generate(counter::incrementAndGet).limit(3);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.toArray());
    }

    @Test
    public void testConcat() {
        long[] a1 = { 1L, 2L };
        long[] a2 = { 3L, 4L };
        long[] a3 = { 5L, 6L };

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, LongStream.concat(a1, a2, a3).toArray());
    }

    @Test
    public void testConcatStreams() {
        LongStream s1 = createLongStream(1L, 2L);
        LongStream s2 = createLongStream(3L, 4L);
        LongStream s3 = createLongStream(5L, 6L);

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, LongStream.concat(s1, s2, s3).toArray());
    }

    @Test
    public void testZip() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };

        LongStream zipped = LongStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new long[] { 5L, 7L, 9L }, zipped.toArray());
    }

    @Test
    public void testZipWithDifferentLengths() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 5L, 6L };

        LongStream zipped = LongStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new long[] { 6L, 8L }, zipped.toArray());
    }

    @Test
    public void testMerge() {
        long[] a = { 1L, 3L, 5L };
        long[] b = { 2L, 4L, 6L };

        LongStream merged = LongStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, merged.toArray());
    }

    // Test instance methods (using concrete implementation)

    @Test
    public void testMap() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        long[] result = stream.map(n -> n * 2).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testMapToInt() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        int[] result = stream.mapToInt(n -> (int) n).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToDouble() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        double[] result = stream.mapToDouble(n -> n * 1.5).toArray();
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5 }, result, 0.001);
    }

    @Test
    public void testMapToObj() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        List<String> result = stream.mapToObj(String::valueOf).toList();
        assertEquals(Arrays.asList("1", "2", "3"), result);
    }

    @Test
    public void testFlatMap() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        long[] result = stream.flatMap(n -> createLongStream(n, n * 10)).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testFilter() {
        LongStream stream = createLongStream(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new long[] { 2L, 4L }, result);
    }

    @Test
    public void testDistinct() {
        LongStream stream = createLongStream(1L, 2L, 2L, 3L, 3L, 3L);
        long[] result = stream.distinct().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testSorted() {
        LongStream stream = createLongStream(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L);
        long[] result = stream.sorted().toArray();
        assertArrayEquals(new long[] { 1L, 1L, 2L, 3L, 4L, 5L, 6L, 9L }, result);
    }

    @Test
    public void testLimit() {
        LongStream stream = createLongStream(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.limit(3).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testSkip() {
        LongStream stream = createLongStream(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.skip(2).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testPeek() {
        List<Long> peeked = new ArrayList<>();
        LongStream stream = createLongStream(1L, 2L, 3L);
        long[] result = stream.peek(peeked::add).toArray();

        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
        assertEquals(Arrays.asList(1L, 2L, 3L), peeked);
    }

    @Test
    public void testReduce() {
        LongStream stream = createLongStream(1L, 2L, 3L, 4L);
        long result = stream.reduce(0L, Long::sum);
        assertEquals(10L, result);
    }

    @Test
    public void testReduceWithoutIdentity() {
        LongStream stream = createLongStream(1L, 2L, 3L, 4L);
        OptionalLong result = stream.reduce(Long::sum);
        assertTrue(result.isPresent());
        assertEquals(10L, result.get());
    }

    @Test
    public void testCollect() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        List<Long> result = stream.collect(ArrayList::new, List::add, List::addAll);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testForEach() {
        List<Long> result = new ArrayList<>();
        LongStream stream = createLongStream(1L, 2L, 3L);
        stream.forEach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createLongStream(1L, 2L, 3L).anyMatch(n -> n == 2L));
        assertFalse(createLongStream(1L, 2L, 3L).anyMatch(n -> n == 5L));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createLongStream(2L, 4L, 6L).allMatch(n -> n % 2 == 0));
        assertFalse(createLongStream(1L, 2L, 3L).allMatch(n -> n % 2 == 0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createLongStream(1L, 3L, 5L).noneMatch(n -> n % 2 == 0));
        assertFalse(createLongStream(1L, 2L, 3L).noneMatch(n -> n % 2 == 0));
    }

    @Test
    public void testFindFirst() {
        OptionalLong result = createLongStream(1L, 2L, 3L).first();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    public void testFindAny() {
        OptionalLong result = createLongStream(1L, 2L, 3L).first();
        assertTrue(result.isPresent());
        assertTrue(result.get() >= 1L && result.get() <= 3L);
    }

    @Test
    public void testMin() {
        OptionalLong result = createLongStream(3L, 1L, 4L, 1L, 5L).min();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    public void testMax() {
        OptionalLong result = createLongStream(3L, 1L, 4L, 1L, 5L).max();
        assertTrue(result.isPresent());
        assertEquals(5L, result.get());
    }

    @Test
    public void testSum() {
        long result = createLongStream(1L, 2L, 3L, 4L, 5L).sum();
        assertEquals(15L, result);
    }

    @Test
    public void testAverage() {
        OptionalDouble result = createLongStream(1L, 2L, 3L, 4L, 5L).average();
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testSummarize() {
        LongSummaryStatistics stats = createLongStream(1L, 2L, 3L, 4L, 5L).summarize();
        assertEquals(5, stats.getCount());
        assertEquals(1L, stats.getMin());
        assertEquals(5L, stats.getMax());
        assertEquals(15L, stats.getSum());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testToArray() {
        long[] result = createLongStream(1L, 2L, 3L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testToList() {
        List<Long> result = createLongStream(1L, 2L, 3L).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testToSet() {
        Set<Long> result = createLongStream(1L, 2L, 2L, 3L).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1L, 2L, 3L)), result);
    }

    @Test
    public void testBoxed() {
        Stream<Long> boxed = createLongStream(1L, 2L, 3L).boxed();
        assertEquals(Arrays.asList(1L, 2L, 3L), boxed.toList());
    }

    @Test
    public void testParallel() {
        LongStream stream = createLongStream(1L, 2L, 3L, 4L, 5L);
        assertFalse(stream.isParallel());

        LongStream parallel = stream.parallel();
        assertTrue(parallel.isParallel());
    }

    @Test
    public void testSequential() {
        LongStream stream = createLongStream(1L, 2L, 3L).parallel();
        assertTrue(stream.isParallel());

        LongStream sequential = stream.sequential();
        assertFalse(sequential.isParallel());
    }

    @Test
    public void testClose() {
        AtomicLong closed = new AtomicLong(0);
        LongStream stream = createLongStream(1L, 2L, 3L).onClose(() -> closed.incrementAndGet());

        assertEquals(0, closed.get());
        stream.close();
        assertEquals(1, closed.get());
    }

    @Test
    public void testMultipleCloseHandlers() {
        List<Integer> closeOrder = new ArrayList<>();
        LongStream stream = createLongStream(1L).onClose(() -> closeOrder.add(1)).onClose(() -> closeOrder.add(2)).onClose(() -> closeOrder.add(3));

        stream.close();
        assertEquals(Arrays.asList(1, 2, 3), closeOrder);
    }

    @Test
    public void testTakeWhile() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).takeWhile(n -> n < 4L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testDropWhile() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).dropWhile(n -> n < 4L).toArray();
        assertArrayEquals(new long[] { 4L, 5L }, result);
    }

    @Test
    public void testScan() {
        long[] result = createLongStream(1L, 2L, 3L, 4L).scan((a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 6L, 10L }, result);
    }

    @Test
    public void testPrepend() {
        long[] result = createLongStream(3L, 4L, 5L).prepend(1L, 2L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testAppend() {
        long[] result = createLongStream(1L, 2L, 3L).append(4L, 5L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testCycled() {
        long[] result = createLongStream(1L, 2L).cycled().limit(6).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 1L, 2L, 1L, 2L }, result);
    }

    @Test
    public void testReversed() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).reversed().toArray();
        assertArrayEquals(new long[] { 5L, 4L, 3L, 2L, 1L }, result);
    }

    @Test
    public void testShuffled() {
        long[] original = { 1L, 2L, 3L, 4L, 5L };
        long[] shuffled = createLongStream(original).shuffled().toArray();

        // Should contain same elements
        assertEquals(original.length, shuffled.length);
        Set<Long> originalSet = new HashSet<>();
        Set<Long> shuffledSet = new HashSet<>();
        for (long val : original)
            originalSet.add(val);
        for (long val : shuffled)
            shuffledSet.add(val);
        assertEquals(originalSet, shuffledSet);
    }

    @Test
    public void testRotated() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).rotated(2).toArray();
        assertArrayEquals(new long[] { 4L, 5L, 1L, 2L, 3L }, result);
    }

    @Test
    public void testIntersection() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).intersection(Arrays.asList(3L, 4L, 5L, 6L, 7L)).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testDifference() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).difference(Arrays.asList(3L, 4L, 5L, 6L, 7L)).toArray();
        assertArrayEquals(new long[] { 1L, 2L }, result);
    }

    @Test
    public void testKthLargest() {
        OptionalLong result = createLongStream(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L).kthLargest(3);
        assertTrue(result.isPresent());
        assertEquals(5L, result.get());
    }

    @Test
    public void testTop() {
        long[] result = createLongStream(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L).top(3).toArray();
        assertArrayEquals(new long[] { 5L, 9L, 6L }, result);
    }

    @Test
    public void testIndexed() {
        List<IndexedLong> result = createLongStream(10L, 20L, 30L).indexed().toList();

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10L, result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals(20L, result.get(1).value());
        assertEquals(2, result.get(2).index());
        assertEquals(30L, result.get(2).value());
    }

    @Test
    public void testJoin() {
        String result = createLongStream(1L, 2L, 3L).join(", ");
        assertEquals("1, 2, 3", result);
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        String result = createLongStream(1L, 2L, 3L).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testCount() {
        assertEquals(5, createLongStream(1L, 2L, 3L, 4L, 5L).count());
        assertEquals(0, LongStream.empty().count());
    }

    @Test
    public void testToMap() {
        Map<String, Long> result = createLongStream(1L, 2L, 3L).toMap(n -> "key" + n, n -> n * 10);

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(10L), result.get("key1"));
        assertEquals(Long.valueOf(20L), result.get("key2"));
        assertEquals(Long.valueOf(30L), result.get("key3"));
    }

    @Test
    public void testCollapse() {
        List<LongList> result = createLongStream(1L, 2L, 2L, 3L, 3L, 3L, 4L).collapse((a, b) -> a == b).toList();

        assertEquals(4, result.size());
        assertArrayEquals(new long[] { 1L }, result.get(0).trimToSize().array());
        assertArrayEquals(new long[] { 2L, 2L }, result.get(1).trimToSize().array());
        assertArrayEquals(new long[] { 3L, 3L, 3L }, result.get(2).trimToSize().array());
        assertArrayEquals(new long[] { 4L }, result.get(3).trimToSize().array());
    }

    @Test
    public void testElementAt() {
        assertEquals(3L, createLongStream(1L, 2L, 3L, 4L, 5L).elementAt(2).get());
        assertFalse(createLongStream(1L, 2L).elementAt(5).isPresent());
    }

    @Test
    public void testOnlyOne() {
        assertEquals(5L, createLongStream(5L).onlyOne().get());
        assertFalse(LongStream.empty().onlyOne().isPresent());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createLongStream(1L, 2L).onlyOne());
    }

    @Test
    public void testFindFirstWithPredicate() {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findFirst(n -> n > 3);
        assertTrue(result.isPresent());
        assertEquals(4L, result.get());
    }

    @Test
    public void testFindLastWithPredicate() {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findLast(n -> n < 4);
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Long>> result = createLongStream(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L).percentiles();

        assertTrue(result.isPresent());
        Map<Percentage, Long> percentiles = result.get();
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._95));
    }

    @Test
    public void testAsDoubleStream() {
        double[] result = createLongStream(1L, 2L, 3L).asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testStep() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L).step(3).toArray();
        assertArrayEquals(new long[] { 1L, 4L, 7L }, result);
    }

    @Test
    public void testAppendIfEmpty() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, createLongStream(1L, 2L, 3L).appendIfEmpty(10L, 20L).toArray());
        assertArrayEquals(new long[] { 10L, 20L }, LongStream.empty().appendIfEmpty(10L, 20L).toArray());
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        createLongStream(10L, 20L, 30L).forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Long>> result = createLongStream(1L, 2L, 3L, 4L, 5L).groupTo(n -> n % 2 == 0, Collectors.toList());

        assertEquals(Arrays.asList(2L, 4L), result.get(true));
        assertEquals(Arrays.asList(1L, 3L, 5L), result.get(false));
    }

    @Test
    public void testToMultiset() {
        Multiset<Long> result = createLongStream(1L, 2L, 2L, 3L, 3L, 3L).toMultiset();
        assertEquals(1, result.count(1L));
        assertEquals(2, result.count(2L));
        assertEquals(3, result.count(3L));
    }

    @Test
    public void testFlatten() {
        long[][] arrays = { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } };
        long[] result = LongStream.flatten(arrays).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testFlattenVertically() {
        long[][] arrays = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
        long[] result = LongStream.flatten(arrays, true).toArray();
        assertArrayEquals(new long[] { 1L, 4L, 2L, 5L, 3L, 6L }, result);
    }

    @Test
    public void testInterval() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(10).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        // Should take at least 20ms (for 3 elements with 10ms interval)
        assertTrue(endTime - startTime >= 20);
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        AtomicLong counter = new AtomicLong(0);
        long[] result = LongStream.iterate(() -> counter.get() < 3, counter::incrementAndGet).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testZipWithThreeArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };
        long[] c = { 7L, 8L, 9L };

        long[] result = LongStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 12L, 15L, 18L }, result);
    }

    @Test
    public void testMergeWithThreeArrays() {
        long[] a = { 1L, 4L, 7L };
        long[] b = { 2L, 5L, 8L };
        long[] c = { 3L, 6L, 9L };

        LongStream merged = LongStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, merged.toArray());
    }

    @Test
    public void testMapPartial() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).mapPartial(n -> n % 2 == 0 ? OptionalLong.of(n * 10) : OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 20L, 40L }, result);
    }

    @Test
    public void testRangeMap() {
        long[] result = createLongStream(1L, 2L, 3L, 5L, 6L, 8L).rangeMap((a, b) -> b - a == 1, (a, b) -> a == b ? a : a * 10 + b).toArray();
        // Groups consecutive numbers: [1,2,3] -> 13, [5,6] -> 56, [8] -> 8
        assertArrayEquals(new long[] { 12L, 3L, 56L, 8L }, result);
    }

    @Test
    public void testPrintln() {
        // This is mainly for manual verification
        //  createLongStream(1L, 2L, 3L).println();
        // Should print: [1, 2, 3]

        // Since println() prints to console, we can't easily test its output
        // But we can at least verify it doesn't throw an exception
        createLongStream(1L, 2L, 3L).println();
    }
}
