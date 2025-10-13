package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.LongSupplier;

@Tag("new-test")
public class LongStream103Test extends TestBase {

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
    public void testFlatmapArray() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        long[] result = stream.flatmap(n -> new long[] { n, n * 10 }).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testFlattmap() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        long[] result = stream.flattMap(n -> java.util.stream.LongStream.of(n, n * 10)).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testFlatMapToInt() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        int[] result = stream.flatMapToInt(n -> IntStream.of((int) n, (int) n * 10)).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatMapToFloat() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        float[] result = stream.flatMapToFloat(n -> FloatStream.of(n, n * 10f)).toArray();
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f, 3f, 30f }, result, 0.001f);
    }

    @Test
    public void testFlatMapToDouble() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        double[] result = stream.flatMapToDouble(n -> DoubleStream.of(n, n * 10.0)).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapToObj() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        List<String> result = stream.flatmapToObj(n -> Arrays.asList(String.valueOf(n), String.valueOf(n * 10))).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20", "3", "30"), result);
    }

    @Test
    public void testFlattMapToObj() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        List<String> result = stream.flattmapToObj(n -> new String[] { String.valueOf(n), String.valueOf(n * 10) }).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20", "3", "30"), result);
    }

    @Test
    public void testMapMulti() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        long[] result = stream.mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        }).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testMapPartialJdk() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).mapPartialJdk(n -> n % 2 == 0 ? java.util.OptionalLong.of(n * 10) : java.util.OptionalLong.empty())
                .toArray();
        assertArrayEquals(new long[] { 20L, 40L }, result);
    }

    @Test
    public void testRangeMapToObj() {
        List<String> result = createLongStream(1L, 2L, 3L, 5L, 6L, 8L).rangeMapToObj((a, b) -> b - a == 1, (a, b) -> a == b ? String.valueOf(a) : a + "-" + b)
                .toList();
        assertEquals(Arrays.asList("1-2", "3", "5-6", "8"), result);
    }

    @Test
    public void testCollapseWithMergeFunction() {
        long[] result = createLongStream(1L, 2L, 2L, 3L, 3L, 3L, 4L).collapse((a, b) -> a == b, Long::sum).toArray();
        assertArrayEquals(new long[] { 1L, 4L, 9L, 4L }, result);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        long[] result = createLongStream(1L, 2L, 3L, 10L, 11L, 12L, 20L).collapse((first, prev, curr) -> curr - first < 5, Long::sum).toArray();
        assertArrayEquals(new long[] { 6L, 33L, 20L }, result);
    }

    @Test
    public void testScanWithInit() {
        long[] result = createLongStream(1L, 2L, 3L, 4L).scan(10L, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 13L, 16L, 20L }, result);
    }

    @Test
    public void testScanWithInitIncluded() {
        long[] result = createLongStream(1L, 2L, 3L, 4L).scan(10L, true, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 10L, 11L, 13L, 16L, 20L }, result);
    }

    @Test
    public void testTopWithComparator() {
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).top(3, Comparator.reverseOrder()).toArray();
        assertArrayEquals(new long[] { 3L, 1L, 2L }, result);
    }

    @Test
    public void testToLongList() {
        LongList result = createLongStream(1L, 2L, 3L).toLongList();
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<Long, Long> result = createLongStream(1L, 2L, 3L, 1L, 2L).toMap(n -> n, n -> n * 10, (v1, v2) -> v1 + v2);
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(20L), result.get(1L));
        assertEquals(Long.valueOf(40L), result.get(2L));
        assertEquals(Long.valueOf(30L), result.get(3L));
    }

    @Test
    public void testToMapWithSupplier() {
        TreeMap<Long, Long> result = createLongStream(3L, 1L, 2L).toMap(n -> n, n -> n * 10, TreeMap::new);
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), new ArrayList<>(result.keySet()));
    }

    @Test
    public void testGroupToWithSupplier() {
        TreeMap<Boolean, List<Long>> result = createLongStream(1L, 2L, 3L, 4L, 5L).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(), TreeMap::new);
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2L, 4L), result.get(true));
        assertEquals(Arrays.asList(1L, 3L, 5L), result.get(false));
    }

    @Test
    public void testForEachIndexedWithException() throws Exception {
        List<String> result = new ArrayList<>();
        createLongStream(10L, 20L, 30L).forEachIndexed((index, value) -> {
            result.add(index + ":" + value);
        });
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);
    }

    @Test
    public void testAnyMatchWithException() throws Exception {
        assertTrue(createLongStream(1L, 2L, 3L).anyMatch(n -> n == 2L));
        assertFalse(createLongStream(1L, 2L, 3L).anyMatch(n -> n == 5L));
    }

    @Test
    public void testAllMatchWithException() throws Exception {
        assertTrue(createLongStream(2L, 4L, 6L).allMatch(n -> n % 2 == 0));
        assertFalse(createLongStream(1L, 2L, 3L).allMatch(n -> n % 2 == 0));
    }

    @Test
    public void testNoneMatchWithException() throws Exception {
        assertTrue(createLongStream(1L, 3L, 5L).noneMatch(n -> n % 2 == 0));
        assertFalse(createLongStream(1L, 2L, 3L).noneMatch(n -> n % 2 == 0));
    }

    @Test
    public void testFindFirstWithPredicateException() throws Exception {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findFirst(n -> n > 3);
        assertTrue(result.isPresent());
        assertEquals(4L, result.getAsLong());
    }

    @Test
    public void testFindAnyWithPredicate() throws Exception {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findAny(n -> n > 3);
        assertTrue(result.isPresent());
        assertTrue(result.getAsLong() > 3);
    }

    @Test
    public void testFindLastWithPredicateException() throws Exception {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findLast(n -> n < 4);
        assertTrue(result.isPresent());
        assertEquals(3L, result.getAsLong());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result = createLongStream(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
                .summarizeAndPercentiles();

        LongSummaryStatistics stats = result.left();
        assertEquals(10, stats.getCount());
        assertEquals(1L, stats.getMin());
        assertEquals(10L, stats.getMax());
        assertEquals(55L, stats.getSum());

        assertTrue(result.right().isPresent());
        Map<Percentage, Long> percentiles = result.right().get();
        assertTrue(percentiles.containsKey(Percentage._50));
    }

    @Test
    public void testMergeWith() {
        LongStream a = createLongStream(1L, 3L, 5L);
        LongStream b = createLongStream(2L, 4L, 6L);

        long[] result = a.mergeWith(b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testZipWithStreams() {
        LongStream a = createLongStream(1L, 2L, 3L);
        LongStream b = createLongStream(4L, 5L, 6L);

        long[] result = a.zipWith(b, (x, y) -> x + y).toArray();
        assertArrayEquals(new long[] { 5L, 7L, 9L }, result);
    }

    @Test
    public void testZipWithThreeStreams() {
        LongStream a = createLongStream(1L, 2L, 3L);
        LongStream b = createLongStream(4L, 5L, 6L);
        LongStream c = createLongStream(7L, 8L, 9L);

        long[] result = a.zipWith(b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 12L, 15L, 18L }, result);
    }

    @Test
    public void testZipWithDefaultValues() {
        LongStream a = createLongStream(1L, 2L);
        LongStream b = createLongStream(3L, 4L, 5L, 6L);

        long[] result = a.zipWith(b, 10L, 20L, (x, y) -> x + y).toArray();
        assertArrayEquals(new long[] { 4L, 6L, 15L, 16L }, result);
    }

    @Test
    public void testZipWithThreeStreamsAndDefaults() {
        LongStream a = createLongStream(1L, 2L);
        LongStream b = createLongStream(3L, 4L, 5L);
        LongStream c = createLongStream(6L);

        long[] result = a.zipWith(b, c, 10L, 20L, 30L, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 10L, 36L, 45L }, result);
    }

    @Test
    public void testAsFloatStream() {
        float[] result = createLongStream(1L, 2L, 3L).asFloatStream().toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.LongStream jdkStream = createLongStream(1L, 2L, 3L).toJdkStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, jdkStream.toArray());
    }

    @Test
    public void testTransformB() {
        long[] result = createLongStream(1L, 2L, 3L).transformB(s -> s.map(n -> n * 2)).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testTransformBWithDeferred() {
        long[] result = createLongStream(1L, 2L, 3L).transformB(s -> s.map(n -> n * 2), true).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testOfLongBuffer() {
        LongBuffer buffer = LongBuffer.wrap(new long[] { 1L, 2L, 3L, 4L, 5L });
        buffer.position(1);
        buffer.limit(4);

        long[] result = createLongStream(buffer).toArray();
        assertArrayEquals(new long[] { 2L, 3L, 4L }, result);
    }

    @Test
    public void testOfJavaOptionalLong() {
        LongStream stream1 = LongStream.of(java.util.OptionalLong.of(5L));
        assertEquals(1, stream1.count());

        LongStream stream2 = LongStream.of(java.util.OptionalLong.empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testConcatArrayList() {
        List<long[]> arrays = Arrays.asList(new long[] { 1L, 2L }, new long[] { 3L, 4L }, new long[] { 5L, 6L });

        long[] result = LongStream.concat(arrays).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testConcatStreamCollection() {
        List<LongStream> streams = Arrays.asList(createLongStream(1L, 2L), createLongStream(3L, 4L), createLongStream(5L, 6L));

        long[] result = LongStream.concat(streams).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testConcatIterators() {
        LongIterator iter1 = LongIterator.of(new long[] { 1L, 2L });
        LongIterator iter2 = LongIterator.of(new long[] { 3L, 4L });
        LongIterator iter3 = LongIterator.of(new long[] { 5L, 6L });

        long[] result = LongStream.concat(iter1, iter2, iter3).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testConcatIteratorCollection() {
        List<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1L, 2L }), LongIterator.of(new long[] { 3L, 4L }),
                LongIterator.of(new long[] { 5L, 6L }));

        long[] result = LongStream.concatIterators(iterators).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testFlatten3D() {
        long[][][] arrays = { { { 1L, 2L }, { 3L, 4L } }, { { 5L, 6L }, { 7L, 8L } } };

        long[] result = LongStream.flatten(arrays).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L }, result);
    }

    @Test
    public void testFlattenWithAlignment() {
        long[][] arrays = { { 1L, 2L }, { 3L }, { 4L, 5L, 6L } };
        long[] result = LongStream.flatten(arrays, 0L, false).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 0L, 3L, 0L, 0L, 4L, 5L, 6L }, result);

        result = LongStream.flatten(arrays, 0L, true).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 4L, 2L, 0, 5L, 0L, 0L, 6L }, result);
    }

    @Test
    public void testFlattenVerticallyWithAlignment() {
        long[][] arrays = { { 1L, 2L }, { 3L }, { 4L, 5L, 6L } };
        long[] result = LongStream.flatten(arrays, 0L, true).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 4L, 2L, 0L, 5L, 0L, 0L, 6L }, result);
    }

    @Test
    public void testZipMultipleStreams() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 2L, 3L), createLongStream(4L, 5L, 6L), createLongStream(7L, 8L, 9L));

        LongStream result = LongStream.zip(streams, values -> Stream.of(values).reduce(0L, Long::sum));
        assertArrayEquals(new long[] { 12L, 15L, 18L }, result.toArray());
    }

    @Test
    public void testZipMultipleStreamsWithDefaults() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 2L), createLongStream(3L, 4L, 5L), createLongStream(6L));

        long[] defaults = { 10L, 20L, 30L };
        LongStream result = LongStream.zip(streams, defaults, values -> Stream.of(values).reduce(0L, Long::sum));
        assertArrayEquals(new long[] { 10L, 36L, 45L }, result.toArray());
    }

    @Test
    public void testMergeMultipleStreams() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 4L, 7L), createLongStream(2L, 5L, 8L), createLongStream(3L, 6L, 9L));

        LongStream result = LongStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, result.toArray());
    }

    @Test
    public void testIterateWithInitAndHasNext() {
        BooleanSupplier hasNext = new BooleanSupplier() {
            private int count = 0;

            @Override
            public boolean getAsBoolean() {
                return count++ < 3;
            }
        };

        long[] result = LongStream.iterate(10L, hasNext, n -> n + 5).toArray();
        assertArrayEquals(new long[] { 10L, 15L, 20L }, result);
    }

    @Test
    public void testMapToFloat() {
        float[] result = createLongStream(1L, 2L, 3L).mapToFloat(n -> n * 1.5f).toArray();
        assertArrayEquals(new float[] { 1.5f, 3.0f, 4.5f }, result, 0.001f);
    }

    @Test
    public void testSymmetricDifference() {
        long[] result = createLongStream(1L, 2L, 3L, 4L).symmetricDifference(Arrays.asList(3L, 4L, 5L, 6L)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 5L, 6L }, result);
    }

    @Test
    public void testFilterWithAction() {
        List<Long> filtered = new ArrayList<>();
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).filter(n -> n % 2 == 0, filtered::add).toArray();

        assertArrayEquals(new long[] { 2L, 4L }, result);
        assertEquals(Arrays.asList(1L, 3L, 5L), filtered);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Long> dropped = new ArrayList<>();
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).dropWhile(n -> n < 3, dropped::add).toArray();

        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
        assertEquals(Arrays.asList(1L, 2L), dropped);
    }

    @Test
    public void testSkipWithAction() {
        List<Long> skipped = new ArrayList<>();
        long[] result = createLongStream(1L, 2L, 3L, 4L, 5L).skip(2, skipped::add).toArray();

        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
        assertEquals(Arrays.asList(1L, 2L), skipped);
    }

    @Test
    public void testPeekIfEmpty() {
        List<Long> result = new ArrayList<>();

        createLongStream(1L, 2L, 3L).ifEmpty(() -> result.add(999L)).forEach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);

        result.clear();
        LongStream.empty().ifEmpty(() -> result.add(999L)).forEach(result::add);
        assertEquals(Arrays.asList(999L), result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = createLongStream(1L, 2L, 3L).joinTo(joiner);
        assertEquals("[1, 2, 3]", result.toString());
    }

    @Test
    public void testRangeLargeNumbers() {
        long start = Long.MAX_VALUE - 5;
        long end = Long.MAX_VALUE;
        long[] result = LongStream.range(start, end).toArray();
        assertEquals(5, result.length);
        assertEquals(start, result[0]);
        assertEquals(end - 1, result[4]);
    }

    @Test
    public void testIterateWithBooleanSupplierNoHasNext() {
        LongSupplier next = new LongSupplier() {
            private long value = 1L;

            @Override
            public long getAsLong() {
                long result = value;
                value *= 2;
                return result;
            }
        };

        long[] result = LongStream.iterate(() -> false, next).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testTopNegativeN() {
        assertThrows(IllegalArgumentException.class, () -> createLongStream(1L, 2L, 3L).top(-1));
    }

    @Test
    public void testPrependOptionalLong() {
        long[] result1 = createLongStream(2L, 3L).prepend(OptionalLong.of(1L)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result1);

        long[] result2 = createLongStream(2L, 3L).prepend(OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 2L, 3L }, result2);
    }

    @Test
    public void testAppendOptionalLong() {
        long[] result1 = createLongStream(1L, 2L).append(OptionalLong.of(3L)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result1);

        long[] result2 = createLongStream(1L, 2L).append(OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 1L, 2L }, result2);
    }

    @Test
    public void testPrependStream() {
        LongStream stream1 = createLongStream(3L, 4L, 5L);
        LongStream stream2 = createLongStream(1L, 2L);

        long[] result = stream1.prepend(stream2).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testAppendStream() {
        LongStream stream1 = createLongStream(1L, 2L, 3L);
        LongStream stream2 = createLongStream(4L, 5L);

        long[] result = stream1.append(stream2).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testCycledWithRounds() {
        long[] result = createLongStream(1L, 2L).cycled(3).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 1L, 2L, 1L, 2L }, result);
    }

    @Test
    public void testThrowIfEmpty() {
        assertEquals(3, createLongStream(1L, 2L, 3L).throwIfEmpty().count());

        try {
            LongStream.empty().throwIfEmpty().count();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testThrowIfEmptyWithCustomException() {
        try {
            LongStream.empty().throwIfEmpty(() -> new IllegalStateException("Custom message")).count();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Custom message", e.getMessage());
        }
    }

    @Test
    public void testCollectWithSupplierOnly() {
        LongList result = createLongStream(1L, 2L, 3L).collect(LongList::new, LongList::add);
        assertEquals(3, result.size());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.toArray());
    }

    @Test
    public void testIntervalWithDelay() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(50, 10).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        assertTrue(endTime - startTime >= 70);
    }

    @Test
    public void testIntervalWithTimeUnit() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(50, 10, TimeUnit.MILLISECONDS).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        assertTrue(endTime - startTime >= 70);
    }

    @Test
    public void testDefaultIfEmpty() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, createLongStream(1L, 2L, 3L).defaultIfEmpty(() -> createLongStream(10L, 20L)).toArray());
        assertArrayEquals(new long[] { 10L, 20L }, LongStream.empty().defaultIfEmpty(() -> createLongStream(10L, 20L)).toArray());
    }

    @Test
    public void testForeach() {
        List<Long> result = new ArrayList<>();
        createLongStream(1L, 2L, 3L).foreach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }
}
