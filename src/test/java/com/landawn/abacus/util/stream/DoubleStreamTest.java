package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

@Tag("2025")
public class DoubleStreamTest extends TestBase {

    protected DoubleStream createDoubleStream(double... a) {
        return DoubleStream.of(a);
    }

    protected DoubleStream createDoubleStream(double[] a, int fromIndex, int toIndex) {
        return DoubleStream.of(a, fromIndex, toIndex);
    }

    protected DoubleStream createDoubleStream(Double[] a) {
        return DoubleStream.of(a);
    }

    protected DoubleStream createDoubleStream(Double[] a, int fromIndex, int toIndex) {
        return DoubleStream.of(a, fromIndex, toIndex);
    }

    protected DoubleStream createDoubleStream(Collection<Double> coll) {
        return DoubleStream.of(coll.toArray(new Double[coll.size()]));
    }

    protected DoubleStream createDoubleStream(DoubleIterator iter) {
        return iter == null ? DoubleStream.empty() : DoubleStream.of(iter.toArray());
    }

    protected DoubleStream createDoubleStream(DoubleBuffer buff) {
        return DoubleStream.of(buff);
    }

    protected DoubleStream createDoubleStream(OptionalDouble op) {
        return DoubleStream.of(op);
    }

    @Test
    public void testFilter() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.filter(x -> x > 2.5).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertEquals(0, emptyStream.filter(x -> true).count());
    }

    @Test
    public void testTakeWhile() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 2.0);
        double[] result = stream.takeWhile(x -> x < 4.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);

        DoubleStream stream2 = DoubleStream.of(1.0, 2.0, 3.0);
        assertEquals(3, stream2.takeWhile(x -> x < 10.0).count());
    }

    @Test
    public void testDropWhile() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.dropWhile(x -> x < 3.0).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.0001);

        DoubleStream stream2 = DoubleStream.of(5.0, 6.0);
        assertEquals(2, stream2.dropWhile(x -> x < 1.0).count());
    }

    @Test
    public void testMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.map(x -> x * 2.0).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testMapToInt() {
        DoubleStream stream = DoubleStream.of(1.5, 2.7, 3.9);
        int[] result = stream.mapToInt(x -> (int) x).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToLong() {
        DoubleStream stream = DoubleStream.of(1.5, 2.7, 3.9);
        long[] result = stream.mapToLong(x -> (long) x).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testMapToFloat() {
        DoubleStream stream = DoubleStream.of(1.5, 2.7, 3.9);
        float[] result = stream.mapToFloat(x -> (float) x).toArray();
        assertEquals(3, result.length);
        assertEquals(1.5f, result[0], 0.0001f);
    }

    @Test
    public void testMapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<String> result = stream.mapToObj(x -> "Value: " + x).toList();
        assertEquals(3, result.size());
        assertEquals("Value: 1.0", result.get(0));
    }

    @Test
    public void testFlatMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.flatMap(x -> DoubleStream.of(x, x * 2)).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testFlatmap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.flatmap(x -> new double[] { x, x * 2 }).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testFlattMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.flattMap(x -> java.util.stream.DoubleStream.of(x, x * 2)).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testFlatMapToInt() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        int[] result = stream.flatMapToInt(x -> IntStream.of((int) x, (int) x * 2)).toArray();
        assertArrayEquals(new int[] { 1, 2, 2, 4 }, result);
    }

    @Test
    public void testFlatMapToLong() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        long[] result = stream.flatMapToLong(x -> LongStream.of((long) x, (long) x * 2)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 2L, 4L }, result);
    }

    @Test
    public void testFlatMapToFloat() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        float[] result = stream.flatMapToFloat(x -> FloatStream.of((float) x, (float) x * 2)).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testFlatMapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        List<String> result = stream.flatMapToObj(x -> Stream.of("A" + x, "B" + x)).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testFlatmapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        List<String> result = stream.flatmapToObj(x -> Arrays.asList("A" + x, "B" + x)).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testFlattmapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        List<String> result = stream.flatMapArrayToObj(x -> new String[] { "A" + x, "B" + x }).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testMapMulti() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.mapMulti((x, consumer) -> {
            consumer.accept(x);
            consumer.accept(x * 2);
        }).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testMapPartial() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double[] result = stream.mapPartial(x -> x > 2.0 ? OptionalDouble.of(x * 2) : OptionalDouble.empty()).toArray();
        assertArrayEquals(new double[] { 6.0, 8.0 }, result, 0.0001);
    }

    @Test
    public void testMapPartialJdk() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double[] result = stream.mapPartialJdk(x -> x > 2.0 ? java.util.OptionalDouble.of(x * 2) : java.util.OptionalDouble.empty()).toArray();
        assertArrayEquals(new double[] { 6.0, 8.0 }, result, 0.0001);
    }

    @Test
    public void testRangeMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0, 20.0);
        double[] result = stream.rangeMap((first, next) -> next - first < 2.0, (first, last) -> first + last).toArray();
        assertEquals(4, result.length);
        assertArrayEquals(new double[] { 3.0, 6.0, 21.0, 40.0 }, result, 0.0001);
    }

    @Test
    public void testRangeMapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0);
        List<String> result = stream.rangeMapToObj((first, next) -> next - first < 2.0, (first, last) -> first + "-" + last).toList();
        assertEquals(3, result.size());
        assertEquals(N.toList("1.0-2.0", "3.0-3.0", "10.0-11.0"), result);
    }

    @Test
    public void testCollapse() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0);
        List<DoubleList> result = stream.collapse((prev, next) -> next - prev < 2.0).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testCollapseWithMerge() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0);
        double[] result = stream.collapse((prev, next) -> next - prev < 2.0, (a, b) -> a + b).toArray();
        assertEquals(2, result.length);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0);
        double[] result = stream.collapse((first, last, next) -> next - first < 5.0, (a, b) -> a + b).toArray();
        assertEquals(2, result.length);
    }

    @Test
    public void testScan() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double[] result = stream.scan((a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 6.0, 10.0 }, result, 0.0001);
    }

    @Test
    public void testScanWithInit() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.scan(10.0, (a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 11.0, 13.0, 16.0 }, result, 0.0001);
    }

    @Test
    public void testScanWithInitIncluded() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.scan(10.0, true, (a, b) -> a + b).toArray();
        assertEquals(4, result.length);
        assertEquals(10.0, result[0], 0.0001);
    }

    @Test
    public void testPrepend() {
        DoubleStream stream = DoubleStream.of(3.0, 4.0);
        double[] result = stream.prepend(1.0, 2.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testAppend() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.append(3.0, 4.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testAppendIfEmpty() {
        DoubleStream stream = DoubleStream.empty();
        double[] result = stream.appendIfEmpty(1.0, 2.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0 }, result, 0.0001);

        DoubleStream stream2 = DoubleStream.of(5.0);
        double[] result2 = stream2.appendIfEmpty(1.0, 2.0).toArray();
        assertArrayEquals(new double[] { 5.0 }, result2, 0.0001);
    }

    @Test
    public void testTop() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 4.0, 2.0);
        double[] result = stream.top(3).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testTopWithComparator() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 4.0, 2.0);
        double[] result = stream.top(3, (a, b) -> Double.compare(b, a)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testToDoubleList() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleList list = stream.toDoubleList();
        assertEquals(3, list.size());
        assertEquals(1.0, list.get(0), 0.0001);
    }

    @Test
    public void testToMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x * 2);
        assertEquals(3, map.size());
        assertEquals(2.0, map.get(1), 0.0001);
    }

    @Test
    public void testToMapWithFactory() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x * 2, Suppliers.ofMap());
        assertEquals(3, map.size());
    }

    @Test
    public void testToMapWithMerge() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 1.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x, (a, b) -> a + b);
        assertEquals(2, map.size());
        assertEquals(2.0, map.get(1), 0.0001);
    }

    @Test
    public void testToMapWithMergeAndFactory() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 1.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x, (a, b) -> a + b, HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testGroupTo() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        Map<Boolean, List<Double>> map = stream.groupTo(x -> x > 2.0, java.util.stream.Collectors.toList());
        assertEquals(2, map.size());
    }

    @Test
    public void testGroupToWithFactory() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        Map<Boolean, List<Double>> map = stream.groupTo(x -> x > 2.0, java.util.stream.Collectors.toList(), HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testReduce() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double result = stream.reduce(0.0, (a, b) -> a + b);
        assertEquals(10.0, result, 0.0001);
    }

    @Test
    public void testReduceOptional() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(10.0, result.get(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void testCollect() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleList result = stream.collect(DoubleList::new, DoubleList::add, DoubleList::addAll);
        assertEquals(3, result.size());
    }

    @Test
    public void testCollectTwoArgs() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleList result = stream.collect(DoubleList::new, DoubleList::add);
        assertEquals(3, result.size());
    }

    @Test
    public void testForeach() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<Double> list = new ArrayList<>();
        stream.foreach(list::add);
        assertEquals(3, list.size());
    }

    @Test
    public void testForEach() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<Double> list = new ArrayList<>();
        stream.forEach(list::add);
        assertEquals(3, list.size());
    }

    @Test
    public void testForEachIndexed() {
        DoubleStream stream = DoubleStream.of(10.0, 20.0, 30.0);
        List<String> list = new ArrayList<>();
        stream.forEachIndexed((idx, val) -> list.add(idx + ":" + val));
        assertEquals(3, list.size());
        assertEquals("0:10.0", list.get(0));
    }

    @Test
    public void testAnyMatch() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertTrue(stream.anyMatch(x -> x > 2.0));

        DoubleStream stream2 = DoubleStream.of(1.0, 2.0);
        assertFalse(stream2.anyMatch(x -> x > 5.0));
    }

    @Test
    public void testAllMatch() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertTrue(stream.allMatch(x -> x > 0.0));

        DoubleStream stream2 = DoubleStream.of(1.0, 2.0, 3.0);
        assertFalse(stream2.allMatch(x -> x > 2.0));
    }

    @Test
    public void testNoneMatch() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertTrue(stream.noneMatch(x -> x > 10.0));

        DoubleStream stream2 = DoubleStream.of(1.0, 2.0, 3.0);
        assertFalse(stream2.noneMatch(x -> x > 2.0));
    }

    @Test
    public void testFindFirst() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble result = stream.findFirst(x -> x > 1.5);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.get(), 0.0001);
    }

    @Test
    public void testFindAny() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble result = stream.findAny(x -> x > 1.5);
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindLast() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.findLast(x -> x < 3.5);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.get(), 0.0001);
    }

    @Test
    public void testMin() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 2.0);
        OptionalDouble result = stream.min();
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.min().isPresent());
    }

    @Test
    public void testMax() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 2.0);
        OptionalDouble result = stream.max();
        assertTrue(result.isPresent());
        assertEquals(5.0, result.get(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.max().isPresent());
    }

    @Test
    public void testKthLargest() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 4.0, 2.0);
        OptionalDouble result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.get(), 0.0001);
    }

    @Test
    public void testSum() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double result = stream.sum();
        assertEquals(10.0, result, 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertEquals(0.0, emptyStream.sum(), 0.0001);
    }

    @Test
    public void testAverage() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.average();
        assertTrue(result.isPresent());
        assertEquals(2.5, result.get(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.average().isPresent());
    }

    @Test
    public void testsummaryStatistics() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        DoubleSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(15.0, stats.getSum(), 0.0001);
        assertEquals(3.0, stats.getAverage(), 0.0001);
        assertEquals(1.0, stats.getMin(), 0.0001);
        assertEquals(5.0, stats.getMax(), 0.0001);
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result = stream.summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertNotNull(result.left());
        assertEquals(5, result.left().getCount());
    }

    @Test
    public void testMergeWith() {
        DoubleStream stream1 = DoubleStream.of(1.0, 3.0, 5.0);
        DoubleStream stream2 = DoubleStream.of(2.0, 4.0, 6.0);
        double[] result = stream1.mergeWith(stream2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testZipWith() {
        DoubleStream stream1 = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream stream2 = DoubleStream.of(4.0, 5.0, 6.0);
        double[] result = stream1.zipWith(stream2, (a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 5.0, 7.0, 9.0 }, result, 0.0001);
    }

    @Test
    public void testZipWithThreeStreams() {
        DoubleStream stream1 = DoubleStream.of(1.0, 2.0);
        DoubleStream stream2 = DoubleStream.of(3.0, 4.0);
        DoubleStream stream3 = DoubleStream.of(5.0, 6.0);
        double[] result = stream1.zipWith(stream2, stream3, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new double[] { 9.0, 12.0 }, result, 0.0001);
    }

    @Test
    public void testZipWithDefaults() {
        DoubleStream stream1 = DoubleStream.of(1.0, 2.0);
        DoubleStream stream2 = DoubleStream.of(3.0, 4.0, 5.0);
        double[] result = stream1.zipWith(stream2, 10.0, 20.0, (a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 4.0, 6.0, 15.0 }, result, 0.0001);
    }

    @Test
    public void testZipWithDefaultsThreeStreams() {
        DoubleStream stream1 = DoubleStream.of(1.0);
        DoubleStream stream2 = DoubleStream.of(2.0, 3.0);
        DoubleStream stream3 = DoubleStream.of(4.0, 5.0, 6.0);
        double[] result = stream1.zipWith(stream2, stream3, 100.0, 200.0, 300.0, (a, b, c) -> a + b + c).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testBoxed() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<Double> result = stream.boxed().toList();
        assertEquals(3, result.size());
        assertEquals(1.0, result.get(0), 0.0001);
    }

    @Test
    public void testToJdkStream() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        java.util.stream.DoubleStream jdkStream = stream.toJdkStream();
        assertEquals(3, jdkStream.count());
    }

    @Test
    public void testTransformB() {
        DoubleStream stream = DoubleStream.of(3.0, 1.0, 2.0);
        double[] result = stream.transformB(s -> s.sorted()).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testTransformBDeferred() {
        DoubleStream stream = DoubleStream.of(3.0, 1.0, 2.0);
        double[] result = stream.transformB(s -> s.sorted(), true).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testEmpty() {
        DoubleStream stream = DoubleStream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleStream stream = DoubleStream.defer(() -> {
            counter.incrementAndGet();
            return DoubleStream.of(1.0, 2.0, 3.0);
        });
        assertEquals(0, counter.get());
        double[] result = stream.toArray();
        assertEquals(1, counter.get());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testFromJdkStream() {
        java.util.stream.DoubleStream jdkStream = java.util.stream.DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream stream = DoubleStream.from(jdkStream);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);

        DoubleStream emptyStream = DoubleStream.from(null);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfNullable() {
        DoubleStream stream1 = DoubleStream.ofNullable(5.0);
        assertEquals(1, stream1.count());

        DoubleStream stream2 = DoubleStream.ofNullable(null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOf() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);

        DoubleStream emptyStream = DoubleStream.of();
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfWithRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleStream stream = DoubleStream.of(array, 1, 4);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfBoxedArray() {
        Double[] array = { 1.0, 2.0, 3.0 };
        DoubleStream stream = DoubleStream.of(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfBoxedArrayWithRange() {
        Double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleStream stream = DoubleStream.of(array, 1, 4);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfOptionalDouble() {
        OptionalDouble op = OptionalDouble.of(5.0);
        DoubleStream stream = DoubleStream.of(op);
        assertArrayEquals(new double[] { 5.0 }, stream.toArray(), 0.0001);

        OptionalDouble empty = OptionalDouble.empty();
        DoubleStream emptyStream = DoubleStream.of(empty);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfJavaOptionalDouble() {
        java.util.OptionalDouble op = java.util.OptionalDouble.of(5.0);
        DoubleStream stream = DoubleStream.of(op);
        assertArrayEquals(new double[] { 5.0 }, stream.toArray(), 0.0001);

        java.util.OptionalDouble empty = java.util.OptionalDouble.empty();
        DoubleStream emptyStream = DoubleStream.of(empty);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testFlatten2D() {
        double[][] array = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0 } };
        DoubleStream stream = DoubleStream.flatten(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testFlatten2DVertical() {
        double[][] array = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0 } };
        DoubleStream stream = DoubleStream.flatten(array, true);
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0, 2.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testFlatten2DWithAlignment() {
        double[][] array = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0, 6.0 } };
        DoubleStream stream = DoubleStream.flatten(array, 0.0, false);
        assertArrayEquals(new double[] { 1.0, 2.0, 0.0, 3.0, 0.0, 0.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testFlatten2DWithAlignmentVertical() {
        double[][] array = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0 } };
        DoubleStream stream = DoubleStream.flatten(array, 0.0, true);
        assertEquals(6, stream.count());
    }

    @Test
    public void testFlatten3D() {
        double[][][] array = { { { 1.0, 2.0 }, { 3.0 } }, { { 4.0 }, { 5.0, 6.0 } } };
        DoubleStream stream = DoubleStream.flatten(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testRepeat() {
        DoubleStream stream = DoubleStream.repeat(7.5, 3);
        assertArrayEquals(new double[] { 7.5, 7.5, 7.5 }, stream.toArray(), 0.0001);

        DoubleStream emptyStream = DoubleStream.repeat(7.5, 0);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testRandom() {
        DoubleStream stream = DoubleStream.random();
        double[] values = stream.limit(5).toArray();
        assertEquals(5, values.length);

        for (double value : values) {
            assertTrue(value >= 0.0 && value < 1.0);
        }
    }

    @Test
    public void testIterateWithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleStream stream = DoubleStream.iterate(() -> counter.get() < 3, () -> counter.getAndIncrement() * 2.0);
        assertArrayEquals(new double[] { 0.0, 2.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testIterateWithInitAndHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleStream stream = DoubleStream.iterate(10.0, () -> counter.getAndIncrement() < 3, x -> x + 2.0);
        double[] result = stream.toArray();
        assertEquals(3, result.length);
        assertEquals(10.0, result[0], 0.0001);
    }

    @Test
    public void testIterateWithPredicate() {
        DoubleStream stream = DoubleStream.iterate(1.0, x -> x < 10.0, x -> x + 3.0);
        assertArrayEquals(new double[] { 1.0, 4.0, 7.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testIterateInfinite() {
        DoubleStream stream = DoubleStream.iterate(1.0, x -> x * 2.0);
        double[] result = stream.limit(5).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 4.0, 8.0, 16.0 }, result, 0.0001);
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(1);
        DoubleStream stream = DoubleStream.generate(() -> counter.getAndIncrement() * 1.5);
        double[] result = stream.limit(4).toArray();
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0 }, result, 0.0001);
    }

    @Test
    public void testConcatArrays() {
        double[] a1 = { 1.0, 2.0 };
        double[] a2 = { 3.0, 4.0 };
        double[] a3 = { 5.0 };
        DoubleStream stream = DoubleStream.concat(a1, a2, a3);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatTwoArrays() {
        double[] a1 = { 1.0, 2.0 };
        double[] a2 = { 3.0, 4.0 };
        DoubleStream stream = DoubleStream.concat(a1, a2);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatIteratorList() {
        List<DoubleIterator> list = Arrays.asList(DoubleIterator.of(new double[] { 1.0, 2.0 }), DoubleIterator.of(new double[] { 3.0, 4.0 }));
        DoubleStream stream = DoubleStream.concatIterators(list);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatStreams() {
        DoubleStream s1 = DoubleStream.of(1.0, 2.0);
        DoubleStream s2 = DoubleStream.of(3.0, 4.0);
        DoubleStream s3 = DoubleStream.of(5.0);
        DoubleStream stream = DoubleStream.concat(s1, s2, s3);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatTwoStreams() {
        DoubleStream s1 = DoubleStream.of(1.0, 2.0);
        DoubleStream s2 = DoubleStream.of(3.0, 4.0);
        DoubleStream stream = DoubleStream.concat(s1, s2);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatListOfArrays() {
        List<double[]> list = Arrays.asList(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }, new double[] { 5.0 });
        DoubleStream stream = DoubleStream.concat(list);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatCollectionOfStreams() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(3.0, 4.0), DoubleStream.of(5.0));
        DoubleStream stream = DoubleStream.concat(streams);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipArrays() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 4.0, 5.0, 6.0 };
        DoubleStream stream = DoubleStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new double[] { 5.0, 7.0, 9.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeArrays() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0 };
        double[] c = { 5.0, 6.0 };
        DoubleStream stream = DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipArraysWithDefaults() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0, 5.0 };
        DoubleStream stream = DoubleStream.zip(a, b, 10.0, 20.0, (x, y) -> x + y);
        assertArrayEquals(new double[] { 4.0, 6.0, 15.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        double[] a = { 1.0 };
        double[] b = { 2.0, 3.0 };
        double[] c = { 4.0, 5.0, 6.0 };
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipIterators() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0, 3.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 4.0, 5.0, 6.0 });
        DoubleStream stream = DoubleStream.zip(a, b, (x, y) -> x * y);
        assertArrayEquals(new double[] { 4.0, 10.0, 18.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeIterators() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 3.0, 4.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 5.0, 6.0 });
        DoubleStream stream = DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipStreams() {
        DoubleStream a = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream b = DoubleStream.of(4.0, 5.0, 6.0);
        DoubleStream stream = DoubleStream.zip(a, b, (x, y) -> x - y);
        assertArrayEquals(new double[] { -3.0, -3.0, -3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeStreams() {
        DoubleStream a = DoubleStream.of(1.0, 2.0);
        DoubleStream b = DoubleStream.of(3.0, 4.0);
        DoubleStream c = DoubleStream.of(5.0, 6.0);
        DoubleStream stream = DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipStreamsWithDefaults() {
        DoubleStream a = DoubleStream.of(1.0, 2.0);
        DoubleStream b = DoubleStream.of(3.0, 4.0, 5.0);
        DoubleStream stream = DoubleStream.zip(a, b, 10.0, 20.0, (x, y) -> x + y);
        assertArrayEquals(new double[] { 4.0, 6.0, 15.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeStreamsWithDefaults() {
        DoubleStream a = DoubleStream.of(1.0);
        DoubleStream b = DoubleStream.of(2.0, 3.0);
        DoubleStream c = DoubleStream.of(4.0, 5.0, 6.0);
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        assertEquals(3, stream.count());
    }

    @Test
    public void testMergeArrays() {
        double[] a = { 1.0, 3.0, 5.0 };
        double[] b = { 2.0, 4.0, 6.0 };
        DoubleStream stream = DoubleStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeIterators() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 3.0, 5.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 2.0, 4.0, 6.0 });
        DoubleStream stream = DoubleStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeStreams() {
        DoubleStream a = DoubleStream.of(1.0, 3.0, 5.0);
        DoubleStream b = DoubleStream.of(2.0, 4.0, 6.0);
        DoubleStream stream = DoubleStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testLimit() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.limit(3).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testSkip() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.skip(2).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.0001);
    }

    @Test
    public void testDistinct() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 2.0, 3.0, 1.0);
        double[] result = stream.distinct().toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testSorted() {
        DoubleStream stream = DoubleStream.of(3.0, 1.0, 4.0, 2.0);
        double[] result = stream.sorted().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testReversed() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.reversed().toArray();
        assertArrayEquals(new double[] { 3.0, 2.0, 1.0 }, result, 0.0001);
    }

    @Test
    public void testPeek() {
        List<Double> peeked = new ArrayList<>();
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.peek(peeked::add).toArray();
        assertEquals(3, peeked.size());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testCount() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertEquals(3, stream.count());
    }

    @Test
    public void testToArray() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testOnClose() {
        AtomicInteger closeCounter = new AtomicInteger(0);
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0).onClose(() -> closeCounter.incrementAndGet());
        stream.count();
        stream.close();
        assertEquals(1, closeCounter.get());
    }

    @Test
    public void testSequential() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream sequential = stream.sequential();
        assertNotNull(sequential);
    }

    @Test
    public void testParallel() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream parallel = stream.parallel();
        assertNotNull(parallel);
    }

    @Test
    public void testIsParallel() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertFalse(stream.isParallel());
    }

    @Test
    public void testDeferWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.defer(null));
    }

    @Test
    public void testIterateWithNullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.iterate(null, () -> 1.0));
    }

    @Test
    public void testIterateWithNullNext() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.iterate(() -> true, null));
    }

    @Test
    public void testGenerateWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.generate(null));
    }

    @Test
    public void testRepeatWithNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.repeat(1.0, -1));
    }

    @Test
    public void testFilterWithNull() {
        assertThrows(NullPointerException.class, () -> DoubleStream.of(1.0, 2.0).filter(null).count());
    }

    @Test
    public void testMapWithNull() {
        assertThrows(NullPointerException.class, () -> DoubleStream.of(1.0, 2.0).map(null).count());
    }

    @Test
    public void testEmptyStreamOperations() {
        DoubleStream empty = DoubleStream.empty();
        assertEquals(0, empty.count());

        DoubleStream empty2 = DoubleStream.empty();
        assertFalse(empty2.findFirst(x -> true).isPresent());

        DoubleStream empty3 = DoubleStream.empty();
        assertTrue(empty3.allMatch(x -> false));

        DoubleStream empty4 = DoubleStream.empty();
        assertFalse(empty4.anyMatch(x -> true));

        DoubleStream empty5 = DoubleStream.empty();
        assertTrue(empty5.noneMatch(x -> true));
    }

    @Test
    public void testOfCollection() {
        Collection<Double> coll = Arrays.asList(1.0, 2.0, 3.0);
        DoubleStream stream = DoubleStream.of(coll);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfIterator() {
        DoubleIterator iter = DoubleIterator.of(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream stream = DoubleStream.of(iter);
        assertEquals(3, stream.count());
    }

    @Test
    public void testOfDoubleBuffer() {
        java.nio.DoubleBuffer buffer = java.nio.DoubleBuffer.wrap(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream stream = DoubleStream.of(buffer);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testShuffled() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.shuffled().toArray();
        assertEquals(5, result.length);
        assertTrue(Arrays.stream(result).anyMatch(x -> Math.abs(x - 1.0) < 0.0001));
    }

    @Test
    public void testRotated() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double[] result = stream.rotated(2).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 1.0, 2.0 }, result, 0.0001);
    }

    @Test
    public void testConcatIteratorVarargs() {
        DoubleIterator iter1 = DoubleIterator.of(new double[] { 1.0, 2.0 });
        DoubleIterator iter2 = DoubleIterator.of(new double[] { 3.0, 4.0 });
        DoubleStream stream = DoubleStream.concat(iter1, iter2);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeListOfIterators() {
        DoubleIterator iter1 = DoubleIterator.of(new double[] { 1.0, 3.0 });
        DoubleIterator iter2 = DoubleIterator.of(new double[] { 2.0, 4.0 });
        DoubleIterator iter3 = DoubleIterator.of(new double[] { 5.0, 6.0 });
        Collection<DoubleIterator> iters = Arrays.asList(iter1, iter2, iter3);
        DoubleStream stream = DoubleStream.concatIterators(iters);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeCollectionOfStreams() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 3.0), DoubleStream.of(2.0, 4.0));
        DoubleStream stream = DoubleStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipCollection() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(3.0, 4.0), DoubleStream.of(5.0, 6.0));
        DoubleStream stream = DoubleStream.zip(streams, array -> Arrays.stream(array).sum());
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipCollectionWithDefaults() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0), DoubleStream.of(2.0, 3.0));
        double[] defaults = new double[] { 100.0, 200.0 };
        DoubleStream stream = DoubleStream.zip(streams, defaults, array -> Arrays.stream(array).sum());
        assertEquals(2, stream.count());
    }

    @Test
    public void testSkipUntil() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.skipUntil(x -> x > 2.5).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.0001);
    }

    @Test
    public void testFirst() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble first = stream.first();
        assertTrue(first.isPresent());
        assertEquals(1.0, first.get(), 0.0001);
    }

    @Test
    public void testLast() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble last = stream.last();
        assertTrue(last.isPresent());
        assertEquals(3.0, last.get(), 0.0001);
    }

    @Test
    public void testToSet() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 2.0, 1.0);
        DoubleList list = stream.toDoubleList();
        assertNotNull(list);
        assertTrue(list.size() == 5);
    }

    // Additional coverage tests for 2025

    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(10.0, 20.0), DoubleStream.of(100.0, 200.0));
        DoubleStream result = DoubleStream.zip(streams, doubles -> {
            double sum = 0.0;
            for (Double d : doubles)
                sum += d;
            return sum;
        });
        assertArrayEquals(new double[] { 111.0, 222.0 }, result.toArray(), 0.0001);
    }

    @Test
    public void testZipWithCollectionDefaultValues() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(10.0));
        double[] defaults = new double[] { 0.0, 0.0 };
        DoubleStream result = DoubleStream.zip(streams, defaults, doubles -> {
            double sum = 0.0;
            for (Double d : doubles)
                sum += d;
            return sum;
        });
        assertEquals(2, result.count());
    }

    @Test
    public void testMergeWithCollection() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 5.0), DoubleStream.of(2.0, 6.0), DoubleStream.of(3.0, 7.0));
        DoubleStream result = DoubleStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    public void testMergeThreeArraysAdditional() {
        double[] a1 = { 1.0, 7.0 };
        double[] a2 = { 3.0, 8.0 };
        double[] a3 = { 5.0, 9.0 };
        DoubleStream stream = DoubleStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeThreeStreamsAdditional() {
        DoubleStream s1 = DoubleStream.of(1.0, 7.0);
        DoubleStream s2 = DoubleStream.of(3.0, 8.0);
        DoubleStream s3 = DoubleStream.of(5.0, 9.0);
        DoubleStream result = DoubleStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    public void testFlattenHorizontalWithPadding() {
        double[][] array = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0, 6.0 } };
        DoubleStream stream = DoubleStream.flatten(array, 0.0, false);
        double[] result = stream.toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 0.0, 3.0, 0.0, 0.0, 4.0, 5.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        // DoubleStream doesn't support rangeClosed with step parameter
        // Testing generate with limited range instead
        DoubleStream stream = DoubleStream.iterate(5.0, d -> d >= 1.0, d -> d - 1.0);
        double[] result = stream.toArray();
        assertArrayEquals(new double[] { 5.0, 4.0, 3.0, 2.0, 1.0 }, result, 0.0001);
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        // DoubleStream doesn't support range with step parameter
        // Testing iterate with false condition instead
        DoubleStream stream = DoubleStream.iterate(1.0, d -> d < 5.0 && d > 10.0, d -> d + 1.0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testParallelFilterMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0);
        double[] result = stream.parallel().filter(x -> x % 2 == 0).map(x -> x * 2).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testIterateWithFalseHasNext() {
        DoubleStream stream = DoubleStream.iterate(() -> false, () -> 1.0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testCollapseNonCollapsible() {
        DoubleStream stream = DoubleStream.of(1.0, 10.0, 100.0);
        double[] result = stream.collapse((prev, next) -> false, (a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 100.0 }, result, 0.0001);
    }

    @Test
    public void testScanEmptyStream() {
        DoubleStream stream = DoubleStream.empty();
        double[] result = stream.scan((a, b) -> a + b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testZipThreeArraysWithDefaultsAdditional() {
        double[] a = { 1.0 };
        double[] b = { 2.0, 3.0 };
        double[] c = { 4.0, 5.0, 6.0 };
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        double[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testZipThreeIteratorsWithDefaultsAdditional() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 2.0, 3.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 4.0, 5.0, 6.0 });
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeStreamsWithDefaultsAdditional() {
        DoubleStream a = DoubleStream.of(1.0);
        DoubleStream b = DoubleStream.of(2.0, 3.0);
        DoubleStream c = DoubleStream.of(4.0, 5.0, 6.0);
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        double[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // All elements should pass
        assertEquals(5, result.length);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testDebounce_EmptyStream() {
        double[] result = DoubleStream.empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        double[] result = DoubleStream.of(42.5).debounce(1, com.landawn.abacus.util.Duration.ofMillis(100)).toArray();

        assertEquals(1, result.length);
        assertEquals(42.5, result[0], 0.001);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(1, result.length);
        assertEquals(1.0, result[0], 0.001);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            DoubleStream.of(1.0, 2.0, 3.0).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            DoubleStream.of(1.0, 2.0, 3.0).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            DoubleStream.of(1.0, 2.0, 3.0).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            DoubleStream.of(1.0, 2.0, 3.0).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toArray();
        });
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        double[] input = new double[1000];
        for (int i = 0; i < 1000; i++) {
            input[i] = i;
        }

        double[] result = DoubleStream.of(input).debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10)).toArray();

        assertEquals(500, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        double[] result = DoubleStream.of(10.0, 20.0, 30.0, 40.0, 50.0).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertArrayEquals(new double[] { 10.0, 20.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
                .filter(n -> n % 2 == 0) // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // 2, 4, 6
                .map(n -> n * 10) // 20, 40, 60
                .toArray();

        assertEquals(3, result.length);
        assertArrayEquals(new double[] { 20.0, 40.0, 60.0 }, result, 0.001);
    }

    @Test
    public void testDebounce_WithSpecialValues() {
        double[] result = DoubleStream.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0, 2.0)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(3, result.length);
        assertTrue(Double.isNaN(result[0]));
        assertEquals(Double.POSITIVE_INFINITY, result[1], 0.001);
        assertEquals(Double.NEGATIVE_INFINITY, result[2], 0.001);
    }

    @Test
    public void testIterate() {
        assertArrayEquals(new double[] { 1.0, 2.0, 4.0, 8.0, 16.0 }, DoubleStream.iterate(1.0, d -> d * 2).limit(5).toArray());
    }

    @Test
    public void testConcat() {
        DoubleStream a = DoubleStream.of(1.0, 2.0);
        DoubleStream b = DoubleStream.of(3.0, 4.0);
        DoubleStream c = DoubleStream.of(5.0, 6.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, DoubleStream.concat(a, b, c).toArray());
    }

    @Test
    public void testReduceWithIdentity() {
        double result = DoubleStream.of(1.0, 2.0, 3.0).reduce(10.0, (a, b) -> a + b);
        assertEquals(16.0, result);
    }

    @Test
    public void testReduceOnEmptyStream() {
        OptionalDouble result = DoubleStream.empty().reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAverageOnEmptyStream() {
        OptionalDouble avg = DoubleStream.empty().average();
        assertFalse(avg.isPresent());
    }

    @Test
    public void testFindFirstOnEmptyStream() {
        OptionalDouble first = DoubleStream.empty().first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testIterator() {
        DoubleIterator it = DoubleStream.of(1.0, 2.0, 3.0).iterator();
        assertTrue(it.hasNext());
        assertEquals(1.0, it.nextDouble());
        assertEquals(2.0, it.nextDouble());
        assertEquals(3.0, it.nextDouble());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextDouble);
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2 }, DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2 }, DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0), DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2.0), DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(3, DoubleStream.of(1, 2, 2, 3, 3).distinct().count());
        assertEquals(2, DoubleStream.of(1, 2, 2, 3, 3).distinct().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.of(1, 2, 2, 3, 3).distinct().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(1, 2, 2, 3, 3).distinct().skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0), DoubleStream.of(1, 2, 2, 3, 3).distinct().toList());
        assertEquals(N.toList(2.0, 3.0), DoubleStream.of(1, 2, 2, 3, 3).distinct().skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().count());
        assertEquals(2, DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0), DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().toList());
        assertEquals(N.toList(2.0, 3.0), DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, DoubleStream.of(3, 1, 4, 2, 5).sorted().count());
        assertEquals(4, DoubleStream.of(3, 1, 4, 2, 5).sorted().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(3, 1, 4, 2, 5).sorted().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(3, 1, 4, 2, 5).sorted().skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(3, 1, 4, 2, 5).sorted().toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(3, 1, 4, 2, 5).sorted().skip(1).toList());
        assertEquals(5, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().count());
        assertEquals(4, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().count());
        assertEquals(4, DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).count());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 3, 2, 1 }, DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toArray(), 0.0);
        assertEquals(N.toList(5.0, 4.0, 3.0, 2.0, 1.0), DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().toList());
        assertEquals(N.toList(4.0, 3.0, 2.0, 1.0), DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toList());
        assertEquals(5, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().count());
        assertEquals(4, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 3, 2, 1 }, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toArray(), 0.0);
        assertEquals(N.toList(5.0, 4.0, 3.0, 2.0, 1.0), DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList(4.0, 3.0, 2.0, 1.0), DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new double[] { 2, 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(2.0, 4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toList());
        assertEquals(N.toList(4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new double[] { 2, 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(2.0, 4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toList());
        assertEquals(N.toList(4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToInt() {
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).toList());
        assertEquals(N.toList(2, 3, 4, 5), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).skip(1).toList());
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).toList());
        assertEquals(N.toList(2, 3, 4, 5), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToLong() {
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).skip(1).toArray());
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).toList());
        assertEquals(N.toList(2L, 3L, 4L, 5L), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).skip(1).toList());
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).skip(1).toArray());
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).toList());
        assertEquals(N.toList(2L, 3L, 4L, 5L), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToFloat() {
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).skip(1).count());
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2.2f, 3.3f, 4.4f, 5.5f }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).skip(1).toArray(), 0.01f);
        assertEquals(N.toList(1.1f, 2.2f, 3.3f, 4.4f, 5.5f), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).toList());
        assertEquals(N.toList(2.2f, 3.3f, 4.4f, 5.5f), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).skip(1).toList());
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).skip(1).count());
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f },
                DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2.2f, 3.3f, 4.4f, 5.5f },
                DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).skip(1).toArray(), 0.01f);
        assertEquals(N.toList(1.1f, 2.2f, 3.3f, 4.4f, 5.5f), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).toList());
        assertEquals(N.toList(2.2f, 3.3f, 4.4f, 5.5f), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).skip(1).count());
        assertArrayEquals(new String[] { "num1.0", "num2.0", "num3.0", "num4.0", "num5.0" }, DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).toArray());
        assertArrayEquals(new String[] { "num2.0", "num3.0", "num4.0", "num5.0" }, DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).skip(1).toArray());
        assertEquals(N.toList("num1.0", "num2.0", "num3.0", "num4.0", "num5.0"), DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).toList());
        assertEquals(N.toList("num2.0", "num3.0", "num4.0", "num5.0"), DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).skip(1).count());
        assertArrayEquals(new String[] { "num1.0", "num2.0", "num3.0", "num4.0", "num5.0" },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).toArray());
        assertArrayEquals(new String[] { "num2.0", "num3.0", "num4.0", "num5.0" },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).skip(1).toArray());
        assertEquals(N.toList("num1.0", "num2.0", "num3.0", "num4.0", "num5.0"), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).toList());
        assertEquals(N.toList("num2.0", "num3.0", "num4.0", "num5.0"), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).toList());
        assertEquals(N.toList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).toArray(),
                0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).toArray(),
                0.0);
        assertEquals(N.toList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).toList());
        assertEquals(N.toList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).toList());
        assertEquals(N.toList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).toArray(),
                0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).toArray(),
                0.0);
        assertEquals(N.toList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).toList());
        assertEquals(N.toList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattMap() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 },
                DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 },
                DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).toList());
        assertEquals(N.toList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2.0, 2.5, 3.0, 3.5 },
                DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 },
                DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5),
                DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).toList());
        assertEquals(N.toList(2.0, 2.5, 3.0, 3.5),
                DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapMulti() {
        assertEquals(6, DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).toList());
        assertEquals(N.toList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).toList());
        assertEquals(N.toList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).count());
        assertArrayEquals(new double[] { 8, 10 },
                DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toArray(), 0.0);
        assertArrayEquals(new double[] { 10 },
                DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).toArray(), 0.0);
        assertEquals(N.toList(8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toList());
        assertEquals(N.toList(10.0),
                DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).toList());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).count());
        assertArrayEquals(new double[] { 8, 10 },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toArray(), 0.0);
        assertArrayEquals(new double[] { 10 },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).toArray(), 0.0);
        assertEquals(N.toList(8.0, 10.0),
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toList());
        assertEquals(N.toList(10.0),
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartialJdk() {
        assertEquals(2,
                DoubleStream.of(1, 2, 3, 4, 5).mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty()).count());
        assertEquals(1,
                DoubleStream.of(1, 2, 3, 4, 5)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new double[] { 8, 10 },
                DoubleStream.of(1, 2, 3, 4, 5).mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty()).toArray(),
                0.0);
        assertArrayEquals(new double[] { 10 },
                DoubleStream.of(1, 2, 3, 4, 5)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .toArray(),
                0.0);
        assertEquals(N.toList(8.0, 10.0),
                DoubleStream.of(1, 2, 3, 4, 5).mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty()).toList());
        assertEquals(N.toList(10.0),
                DoubleStream.of(1, 2, 3, 4, 5)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .toList());
        assertEquals(2,
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .count());
        assertEquals(1,
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new double[] { 8, 10 },
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .toArray(),
                0.0);
        assertArrayEquals(new double[] { 10 },
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .toArray(),
                0.0);
        assertEquals(N.toList(8.0, 10.0),
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .toList());
        assertEquals(N.toList(10.0),
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 20 }, DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 20 }, DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 11.0, 20.0), DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11.0, 20.0), DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 20 }, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).toArray(),
                0.0);
        assertArrayEquals(new double[] { 11, 20 },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 11.0, 20.0), DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11.0, 20.0), DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMapToObj() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).count());
        assertArrayEquals(new String[] { "range[1.0-2.0]", "range[5.0-6.0]", "range[10.0-10.0]" },
                DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").toArray());
        assertArrayEquals(new String[] { "range[5.0-6.0]", "range[10.0-10.0]" },
                DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).toArray());
        assertEquals(N.toList("range[1.0-2.0]", "range[5.0-6.0]", "range[10.0-10.0]"),
                DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").toList());
        assertEquals(N.toList("range[5.0-6.0]", "range[10.0-10.0]"),
                DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").count());
        assertEquals(2,
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).count());
        assertArrayEquals(new String[] { "range[1.0-2.0]", "range[5.0-6.0]", "range[10.0-10.0]" },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").toArray());
        assertArrayEquals(new String[] { "range[5.0-6.0]", "range[10.0-10.0]" },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).toArray());
        assertEquals(N.toList("range[1.0-2.0]", "range[5.0-6.0]", "range[10.0-10.0]"),
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").toList());
        assertEquals(N.toList("range[5.0-6.0]", "range[10.0-10.0]"),
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapse() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).skip(1).count());
        Stream<DoubleList> result = DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1);
        List<DoubleList> list = result.toList();
        assertEquals(3, list.size());
        assertEquals(DoubleList.of(1, 2), list.get(0));
        assertEquals(DoubleList.of(5, 6), list.get(1));
        assertEquals(DoubleList.of(10), list.get(2));

        result = DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).skip(1);
        list = result.toList();
        assertEquals(2, list.size());
        assertEquals(DoubleList.of(5, 6), list.get(0));
        assertEquals(DoubleList.of(10), list.get(1));

        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 10 }, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 10 }, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 10 }, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toArray(),
                0.0);
        assertArrayEquals(new double[] { 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(N.toList(11.0, 10.0),
                DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 11.0, 10.0),
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(N.toList(11.0, 10.0),
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 1, 3, 6, 10, 15 }, DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 6, 10, 15 }, DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 3.0, 6.0, 10.0, 15.0), DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3.0, 6.0, 10.0, 15.0), DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 1, 3, 6, 10, 15 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 6, 10, 15 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 3.0, 6.0, 10.0, 15.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3.0, 6.0, 10.0, 15.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 10, 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(10.0, 11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 10, 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray(),
                0.0);
        assertEquals(N.toList(10.0, 11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToInt() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).count());
        assertArrayEquals(new int[] { 1, 2, 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toArray());
        assertArrayEquals(new int[] { 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).toArray());
        assertEquals(N.toList(1, 2, 2, 3, 3, 4), DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toList());
        assertEquals(N.toList(2, 3, 3, 4), DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).count());
        assertArrayEquals(new int[] { 1, 2, 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toArray());
        assertArrayEquals(new int[] { 2, 3, 3, 4 },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).toArray());
        assertEquals(N.toList(1, 2, 2, 3, 3, 4), DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toList());
        assertEquals(N.toList(2, 3, 3, 4), DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToLong() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).count());
        assertArrayEquals(new long[] { 1, 2, 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).toArray());
        assertArrayEquals(new long[] { 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).toArray());
        assertEquals(N.toList(1L, 2L, 2L, 3L, 3L, 4L), DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).toList());
        assertEquals(N.toList(2L, 3L, 3L, 4L), DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).count());
        assertArrayEquals(new long[] { 1, 2, 2, 3, 3, 4 },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).toArray());
        assertArrayEquals(new long[] { 2, 3, 3, 4 },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).toArray());
        assertEquals(N.toList(1L, 2L, 2L, 3L, 3L, 4L), DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).toList());
        assertEquals(N.toList(2L, 3L, 3L, 4L), DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToFloat() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).count());
        assertArrayEquals(new float[] { 1f, 1.5f, 2f, 2.5f, 3f, 3.5f },
                DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2f, 2.5f, 3f, 3.5f },
                DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).toArray(), 0.01f);
        assertEquals(N.toList(1f, 1.5f, 2f, 2.5f, 3f, 3.5f),
                DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).toList());
        assertEquals(N.toList(2f, 2.5f, 3f, 3.5f), DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).count());
        assertArrayEquals(new float[] { 1f, 1.5f, 2f, 2.5f, 3f, 3.5f },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2f, 2.5f, 3f, 3.5f },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).toArray(), 0.01f);
        assertEquals(N.toList(1f, 1.5f, 2f, 2.5f, 3f, 3.5f),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).toList());
        assertEquals(N.toList(2f, 2.5f, 3f, 3.5f),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).toArray());
        assertEquals(N.toList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).toList());
        assertEquals(N.toList("a2.0", "b2.0", "a3.0", "b3.0"), DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).toArray());
        assertEquals(N.toList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).toList());
        assertEquals(N.toList("a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObj() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).toArray());
        assertEquals(N.toList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toList());
        assertEquals(N.toList("a2.0", "b2.0", "a3.0", "b3.0"), DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).toArray());
        assertEquals(N.toList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toList());
        assertEquals(N.toList("a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObj() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).toArray());
        assertEquals(N.toList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).toList());
        assertEquals(N.toList("a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).toArray());
        assertEquals(N.toList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).toList());
        assertEquals(N.toList("a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 2, 3, 4 }, DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 4 }, DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(2.0, 3.0, 4.0), DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.toList(3.0, 4.0), DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 2, 3, 4 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 4 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(),
                0.0);
        assertEquals(N.toList(2.0, 3.0, 4.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.toList(3.0, 4.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 1, 5 }, DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 5 }, DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.toList(5.0), DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 1, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.toList(5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 1, 5, 6 }, DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 5, 6 }, DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 5.0, 6.0), DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.toList(5.0, 6.0), DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 1, 5, 6 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(),
                0.0);
        assertArrayEquals(new double[] { 5, 6 },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 5.0, 6.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.toList(5.0, 6.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(1, 2, 3, 4, 5).reversed().toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 3, 2, 1 }, DoubleStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray(), 0.0);
        assertEquals(N.toList(5.0, 4.0, 3.0, 2.0, 1.0), DoubleStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.toList(4.0, 3.0, 2.0, 1.0), DoubleStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 3, 2, 1 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray(), 0.0);
        assertEquals(N.toList(5.0, 4.0, 3.0, 2.0, 1.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.toList(4.0, 3.0, 2.0, 1.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).skip(1).toArray(),
                0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new double[] { 4, 5, 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).rotated(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 5, 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(4.0, 5.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.toList(5.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new double[] { 4, 5, 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 5, 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(4.0, 5.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList(5.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled().toArray().length);
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled().skip(1).toArray().length);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled().toList().size());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled().skip(1).toList().size());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toArray().length);
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toArray().length);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toList().size());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray().length);
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList().size());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toList().size());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toArray().length);
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toList().size());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(10, DoubleStream.of(1, 2, 3).cycled().limit(10).count());
        assertEquals(8, DoubleStream.of(1, 2, 3).cycled().limit(10).skip(2).count());
        assertArrayEquals(new double[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, DoubleStream.of(1, 2, 3).cycled().limit(10).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 1, 2, 3, 1, 2, 3, 1 }, DoubleStream.of(1, 2, 3).cycled().limit(10).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0), DoubleStream.of(1, 2, 3).cycled().limit(10).toList());
        assertEquals(N.toList(3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0), DoubleStream.of(1, 2, 3).cycled().limit(10).skip(2).toList());
        assertEquals(10, DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).count());
        assertEquals(8, DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).count());
        assertArrayEquals(new double[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 1, 2, 3, 1, 2, 3, 1 }, DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0), DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).toList());
        assertEquals(N.toList(3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0), DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(9, DoubleStream.of(1, 2, 3).cycled(3).count());
        assertEquals(7, DoubleStream.of(1, 2, 3).cycled(3).skip(2).count());
        assertArrayEquals(new double[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, DoubleStream.of(1, 2, 3).cycled(3).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 1, 2, 3, 1, 2, 3 }, DoubleStream.of(1, 2, 3).cycled(3).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3).cycled(3).toList());
        assertEquals(N.toList(3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3).cycled(3).skip(2).toList());
        assertEquals(9, DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).count());
        assertEquals(7, DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).skip(2).count());
        assertArrayEquals(new double[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 1, 2, 3, 1, 2, 3 }, DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).skip(2).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).toList());
        assertEquals(N.toList(3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).indexed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).indexed().skip(1).count());
        Stream<IndexedDouble> indexedStream = DoubleStream.of(1, 2, 3, 4, 5).indexed();
        List<IndexedDouble> list = indexedStream.toList();
        assertEquals(5, list.size());
        assertEquals(0, list.get(0).index());
        assertEquals(1.0, list.get(0).value(), 0.0);
        assertEquals(4, list.get(4).index());
        assertEquals(5.0, list.get(4).value(), 0.0);

        indexedStream = DoubleStream.of(1, 2, 3, 4, 5).indexed().skip(1);
        list = indexedStream.toList();
        assertEquals(4, list.size());
        assertEquals(1, list.get(0).index());
        assertEquals(2.0, list.get(0).value(), 0.0);

        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skip(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.toList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).toList());
        assertEquals(N.toList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).limit(3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(N.toList(2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList(2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).count());
        assertArrayEquals(new double[] { 1, 3, 5 }, DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 5 }, DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 3.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).toList());
        assertEquals(N.toList(3.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new double[] { 1, 3, 5 }, DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 5 }, DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 3.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toList());
        assertEquals(N.toList(3.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimited() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimitedWithLimiter() {
        RateLimiter limiter = RateLimiter.create(100);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(limiter).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelay() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependStream() {
        assertEquals(6, DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).count());
        assertEquals(5, DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).skip(1).toList());
        assertEquals(6, DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).count());
        assertEquals(5, DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependOptional() {
        assertEquals(4, DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).count());
        assertEquals(3, DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4 }, DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4 }, DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0), DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0), DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).skip(1).toList());
        assertEquals(4, DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).count());
        assertEquals(3, DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4 }, DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4 }, DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0), DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0), DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependArray() {
        assertEquals(6, DoubleStream.of(4, 5, 6).prepend(1, 2, 3).count());
        assertEquals(5, DoubleStream.of(4, 5, 6).prepend(1, 2, 3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).prepend(1, 2, 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).prepend(1, 2, 3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).prepend(1, 2, 3).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).prepend(1, 2, 3).skip(1).toList());
        assertEquals(6, DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).count());
        assertEquals(5, DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendStream() {
        assertEquals(6, DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).count());
        assertEquals(5, DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).skip(1).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).count());
        assertEquals(5, DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        assertEquals(5, DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(4,
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 },
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 },
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(),
                0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0),
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0),
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(5,
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(4,
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 },
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray(),
                0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 },
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray(),
                0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0),
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0),
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith() {
        assertEquals(3, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 5, 7, 9 }, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 7, 9 }, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(5.0, 7.0, 9.0), DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).toList());
        assertEquals(N.toList(7.0, 9.0), DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 5, 7, 9 }, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 7, 9 }, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).toArray(),
                0.0);
        assertEquals(N.toList(5.0, 7.0, 9.0), DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).toList());
        assertEquals(N.toList(7.0, 9.0), DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThree() {
        assertEquals(3, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new double[] { 12, 15, 18 },
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).toArray(), 0.0);
        assertArrayEquals(new double[] { 15, 18 },
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).toArray(), 0.0);
        assertEquals(N.toList(12.0, 15.0, 18.0),
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(15.0, 18.0),
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).count());
        assertEquals(2,
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new double[] { 12, 15, 18 },
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).toArray(), 0.0);
        assertArrayEquals(new double[] { 15, 18 },
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).toArray(),
                0.0);
        assertEquals(N.toList(12.0, 15.0, 18.0),
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(15.0, 18.0),
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        assertEquals(3, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 5, 7, 13 }, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 7, 13 }, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.toList(5.0, 7.0, 13.0), DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).toList());
        assertEquals(N.toList(7.0, 13.0), DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 5, 7, 13 }, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).toArray(),
                0.0);
        assertArrayEquals(new double[] { 7, 13 }, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).toArray(),
                0.0);
        assertEquals(N.toList(5.0, 7.0, 13.0), DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).toList());
        assertEquals(N.toList(7.0, 13.0), DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThreeDefaults() {
        assertEquals(3, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new double[] { 12, 27.0, 33.0 },
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).toArray(), 0.0);
        assertArrayEquals(new double[] { 27.0, 33.0 },
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).toArray(), 0.0);
        assertEquals(N.toList(12.0, 27.0, 33.0),
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(27.0, 33.0),
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).count());
        assertEquals(2,
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new double[] { 12.0, 27.0, 33.0 },
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).toArray(), 0.0);
        assertArrayEquals(new double[] { 27.0, 33.0 },
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).toArray(),
                0.0);
        assertEquals(N.toList(12.0, 27.0, 33.0),
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(27.0, 33.0),
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).boxed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).boxed().skip(1).count());
        assertArrayEquals(new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, DoubleStream.of(1, 2, 3, 4, 5).boxed().toArray());
        assertArrayEquals(new Double[] { 2.0, 3.0, 4.0, 5.0 }, DoubleStream.of(1, 2, 3, 4, 5).boxed().skip(1).toArray());
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).boxed().toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).boxed().skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).count());
        assertArrayEquals(new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toArray());
        assertArrayEquals(new Double[] { 2.0, 3.0, 4.0, 5.0 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toArray());
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterToJdkStream() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).toJdkStream().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).toJdkStream().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).toJdkStream().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).toJdkStream().skip(1).toArray(), 0.0);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream().skip(1).toArray(), 0.0);
    }

    @Test
    public void testStreamCreatedAfterTransformB() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).skip(1).count());
        assertArrayEquals(new double[] { 2, 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(2.0, 4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).toList());
        assertEquals(N.toList(4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).skip(1).count());
        assertArrayEquals(new double[] { 2, 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(2.0, 4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).toList());
        assertEquals(N.toList(4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterEmpty() {
        assertEquals(0, DoubleStream.empty().count());
        assertEquals(0, DoubleStream.empty().skip(1).count());
        assertArrayEquals(new double[] {}, DoubleStream.empty().toArray(), 0.0);
        assertArrayEquals(new double[] {}, DoubleStream.empty().skip(1).toArray(), 0.0);
        assertEquals(N.toList(), DoubleStream.empty().toList());
        assertEquals(N.toList(), DoubleStream.empty().skip(1).toList());
        assertEquals(0, DoubleStream.empty().map(e -> e).count());
        assertEquals(0, DoubleStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new double[] {}, DoubleStream.empty().map(e -> e).toArray(), 0.0);
        assertArrayEquals(new double[] {}, DoubleStream.empty().map(e -> e).skip(1).toArray(), 0.0);
        assertEquals(N.toList(), DoubleStream.empty().map(e -> e).toList());
        assertEquals(N.toList(), DoubleStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefer() {
        assertEquals(3, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).count());
        assertEquals(2, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0), DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).toList());
        assertEquals(N.toList(2.0, 3.0), DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).skip(1).toList());
        assertEquals(3, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).count());
        assertEquals(2, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0), DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).toList());
        assertEquals(N.toList(2.0, 3.0), DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFrom() {
        assertEquals(3, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).count());
        assertEquals(2, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0), DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).toList());
        assertEquals(N.toList(2.0, 3.0), DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).skip(1).toList());
        assertEquals(3, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).count());
        assertEquals(2, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).toArray(), 0.0);
        assertEquals(N.toList(1.0, 2.0, 3.0), DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).toList());
        assertEquals(N.toList(2.0, 3.0), DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).toList());
    }

    @Test
    public void test_takeWhile() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0);

        double[] result = stream.takeWhile(x -> x < 4.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void test_distinct() {
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            double[] result = stream.sorted().distinct().toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
        }
        {
            DoubleStream stream = new ArrayDoubleStream(new double[] { 1.0, 2.0, 2.0, 3.0, 4.0, 5.0, 5.0 }, true, null);

            double[] result = stream.distinct().toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
        }
        {
            DoubleStream stream = new ArrayDoubleStream(new double[] { 1.0, 2.0, 2.0, 3.0, 4.0, 5.0, 5.0 }, true, null);

            double[] result = stream.filter(e -> true).distinct().toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
        }
    }

    @Test
    public void test_toCollection() {
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            List<Double> result = stream.toList();
            assertHaveSameElements(N.toSet(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            Set<Double> result = stream.toSet();
            assertHaveSameElements(N.toSet(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            Set<Double> result = stream.toCollection(Suppliers.ofLinkedHashSet());
            assertHaveSameElements(N.toSet(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
    }

    @Test
    public void test_toMultiset() {
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            Multiset<Double> result = stream.toMultiset();
            assertEquals(Multiset.of(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            Multiset<Double> result = stream.toMultiset(Suppliers.ofMultiset());
            assertEquals(Multiset.of(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).map(e -> e);

            Multiset<Double> result = stream.toMultiset();
            assertEquals(Multiset.of(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).map(e -> e);

            Multiset<Double> result = stream.toMultiset(Suppliers.ofMultiset());
            assertEquals(Multiset.of(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
    }

    @Test
    public void testFlattmap() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<java.util.stream.DoubleStream> mapper = x -> java.util.stream.DoubleStream.of(x, x * 3);

        double[] result = stream.flattMap(mapper).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 2.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testFlattMapToObj() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<String[]> mapper = x -> new String[] { "X" + x, "Y" + x };

        List<String> result = stream.flatMapArrayToObj(mapper).toList();
        assertEquals(Arrays.asList("X1.0", "Y1.0", "X2.0", "Y2.0"), result);
    }

    @Test
    public void testScanWithInitial() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleBinaryOperator accumulator = (a, b) -> a * b;

        double[] result = stream.scan(10.0, accumulator).toArray();
        assertArrayEquals(new double[] { 10.0, 20.0, 60.0 }, result, 0.0001);
    }

    @Test
    public void testScanWithInitialIncluded() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleBinaryOperator accumulator = Double::sum;

        double[] result = stream.scan(100.0, true, accumulator).toArray();
        assertArrayEquals(new double[] { 100.0, 101.0, 103.0, 106.0 }, result, 0.0001);
    }

    @Test
    public void testPrependArray() {
        DoubleStream stream = createDoubleStream(3.0, 4.0, 5.0);
        double[] result = stream.prepend(1.0, 2.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
    }

    @Test
    public void testAppendArray() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        double[] result = stream.append(4.0, 5.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
    }

    @Test
    public void testReduceWithoutIdentity() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.reduce(Double::max);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        OptionalDouble emptyResult = emptyStream.reduce(Double::max);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testCollectWithoutCombiner() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleList result = stream.collect(DoubleList::new, DoubleList::add);

        assertEquals(3, result.size());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result.toArray(), 0.0001);
    }

    @Test
    public void testFindFirstWithPredicate() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.findFirst(x -> x > 2);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.0001);
    }

    @Test
    public void testFindAnyWithPredicate() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.findAny(x -> x > 2);
        assertTrue(result.isPresent());
        assertTrue(result.getAsDouble() > 2);
    }

    @Test
    public void testFindLastWithPredicate() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.findLast(x -> x < 3);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble(), 0.0001);
    }

    @Test
    public void testOfArray() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleStream stream = createDoubleStream(array);
        assertArrayEquals(array, stream.toArray(), 0.0001);

        DoubleStream emptyStream = createDoubleStream(new double[0]);
        assertEquals(0, emptyStream.count());

        DoubleStream rangeStream = createDoubleStream(array, 1, 4);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, rangeStream.toArray(), 0.0001);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_2() {
        double[][] array = { { 1.0d, 2.0d }, { 3.0d, 4.0d } };
        double[] result = DoubleStream.flatten(array, false).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d }, result);

        result = DoubleStream.flatten(array, true).toArray();
        assertArrayEquals(new double[] { 1.0d, 3.0d, 2.0d, 4.0d }, result);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_3() {
        double[][] array = { { 1.0d, 2.0d }, { 3.0d, 4.0d, 5.f } };
        double[] result = DoubleStream.flatten(array, 0.0d, false).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 0.0d, 3.0d, 4.0d, 5.0d }, result);

        result = DoubleStream.flatten(array, 0.0d, true).toArray();
        assertArrayEquals(new double[] { 1.0d, 3.0d, 2.0d, 4.0d, 0.0d, 5.0d }, result);
    }

    @Test
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        double[][] array = {};
        DoubleStream stream = DoubleStream.flatten(array);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeat_02() {
        DoubleStream stream = DoubleStream.repeat(1.0d, 1000);
        assertEquals(1000, stream.mapToFloat(e -> (float) e).count());
    }

    @Test
    public void test_iterate_02() {
        DoubleStream stream = DoubleStream.iterate(() -> true, () -> 1.0d);
        assertEquals(10, stream.limit(10).mapToFloat(e -> (float) e).count());

        stream = DoubleStream.iterate(1.0d, () -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());

        stream = DoubleStream.iterate(1.0d, it -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());
    }

    @Test
    public void test_concat_02() {
        double[] a = { 1.0d, 2.0d, 3.0d };
        double[] b = { 4.0d, 5.0d, 6.0d };
        double[] result = DoubleStream.concat(a, b).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream.concat(N.toList(a, b)).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream.concatIterators(N.toList(DoubleIterator.of(a), DoubleIterator.of(b))).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);
    }

    @Test
    public void test_zip_02() {
        double[] a = { 1.0d };
        double[] b = { 4.0d, 5.0d };
        double[] c = { 7.0d, 8.0d, 9.0d };
        double[] result = DoubleStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new double[] { 12.0d, 113.0d, 309.0d }, result);
    }

    @Test
    public void test_merge() {
        double[] a = { 1.0d, 2.0d, 3.0d };
        double[] b = { 4.0d, 5.0d, 6.0d };
        double[] c = { 7.0d, 8.0d, 9.0d };
        double[] result = DoubleStream.merge(N.toList(DoubleStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d }, result);

        result = DoubleStream.merge(N.toList(DoubleStream.of(a), DoubleStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream
                .merge(N.toList(DoubleStream.of(a), DoubleStream.of(b), DoubleStream.of(c)),
                        (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d, 7.0d, 8.0d, 9.0d }, result);

    }

    @Test
    public void testTransform() {
        List<Integer> callOrder = new ArrayList<>();
        DoubleStream stream = createDoubleStream(1, 2, 3).onClose(() -> callOrder.add(1)).onClose(() -> callOrder.add(2)).onClose(() -> callOrder.add(3));

        DoubleStream.defer(() -> stream).close();

        Assertions.assertEquals(Arrays.asList(1, 2, 3), callOrder);
    }

    // TODO: filter(DoublePredicate) is abstract - tested via concrete implementations above
    // TODO: takeWhile(DoublePredicate) is abstract - tested via concrete implementations above
    // TODO: dropWhile(DoublePredicate) is abstract - tested via concrete implementations above
    // TODO: map(DoubleUnaryOperator) is abstract - tested via concrete implementations above
    // TODO: mapToInt(DoubleToIntFunction) is abstract - tested via concrete implementations above
    // TODO: mapToLong(DoubleToLongFunction) is abstract - tested via concrete implementations above
    // TODO: mapToFloat(DoubleToFloatFunction) is abstract - tested via concrete implementations above
    // TODO: mapToObj(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: flatMap(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: flatmap(DoubleFunction<double[]>) is abstract - tested via concrete implementations above
    // TODO: flattMap(DoubleFunction<JDK DoubleStream>) is abstract - tested via concrete implementations above
    // TODO: flatMapToInt(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToLong(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToFloat(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToObj(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: flatmapToObj(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: mapMulti(DoubleMapMultiConsumer) is abstract - tested via concrete implementations above
    // TODO: mapPartial(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: mapPartialJdk(DoubleFunction) is abstract - tested via concrete implementations above
    // TODO: rangeMap(DoubleBiPredicate, DoubleBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: rangeMapToObj(DoubleBiPredicate, DoubleBiFunction) is abstract - tested via concrete implementations above
    // TODO: collapse(...) overloads are abstract - tested via concrete implementations above
    // TODO: scan(...) overloads are abstract - tested via concrete implementations above
    // TODO: prepend/append/appendIfEmpty are abstract - tested via concrete implementations above
    // TODO: top(...) overloads are abstract - tested via concrete implementations above
    // TODO: toDoubleList() is abstract - tested via concrete implementations above
    // TODO: toMap/groupTo/reduce/collect overloads are abstract - tested via concrete implementations above
    // TODO: forEach/forEachIndexed/anyMatch/allMatch/noneMatch are abstract - tested via concrete implementations above
    // TODO: findFirst/findAny/findLast with predicate are abstract - tested via concrete implementations above
    // TODO: min/max/kthLargest/sum/average/summaryStatistics are abstract - tested via concrete implementations above
    // TODO: mergeWith/zipWith overloads are abstract - tested via concrete implementations above
    // TODO: boxed/toJdkStream are abstract - tested via concrete implementations above

    @Test
    public void testFlatMapArrayToObj() {
        List<String> result = createDoubleStream(1.0, 2.0, 3.0).flatMapArrayToObj(d -> new String[] { "A" + d, "B" + d }).toList();
        assertEquals(6, result.size());
        assertEquals("A1.0", result.get(0));
        assertEquals("B1.0", result.get(1));
    }

    @Test
    public void testFlatMapArrayToObj_Empty() {
        List<String> result = DoubleStream.empty().flatMapArrayToObj(d -> new String[] { "A" + d }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindFirst_NoArg() {
        OptionalDouble result = createDoubleStream(10.0, 20.0, 30.0).findFirst();
        assertTrue(result.isPresent());
        assertEquals(10.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testFindFirst_NoArg_Empty() {
        OptionalDouble result = DoubleStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg() {
        OptionalDouble result = createDoubleStream(10.0, 20.0, 30.0).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg_Empty() {
        OptionalDouble result = DoubleStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testTransformB_TwoArg() {
        double[] result = createDoubleStream(3.0, 1.0, 2.0).transformB(s -> s.sorted(), false).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testTransformB_TwoArg_Deferred() {
        double[] result = createDoubleStream(3.0, 1.0, 2.0).transformB(s -> s.sorted(), true).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testFrom_JdkStream() {
        java.util.stream.DoubleStream jdkStream = java.util.stream.DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream result = DoubleStream.from(jdkStream);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result.toArray(), 0.001);
    }

    @Test
    public void testFrom_JdkStream_Empty() {
        java.util.stream.DoubleStream jdkStream = java.util.stream.DoubleStream.empty();
        DoubleStream result = DoubleStream.from(jdkStream);
        assertEquals(0, result.count());
    }

    @Test
    public void testOfJdkOptionalDouble() {
        DoubleStream result = DoubleStream.of(java.util.OptionalDouble.of(42.5));
        assertArrayEquals(new double[] { 42.5 }, result.toArray(), 0.001);

        result = DoubleStream.of(java.util.OptionalDouble.empty());
        assertEquals(0, result.count());
    }

    @Test
    public void testOfNullable_Null() {
        DoubleStream result = DoubleStream.ofNullable(null);
        assertEquals(0, result.count());
    }

    @Test
    public void testOfNullable_Value() {
        DoubleStream result = DoubleStream.ofNullable(42.5);
        assertArrayEquals(new double[] { 42.5 }, result.toArray(), 0.001);
    }

    @Test
    public void testConcatIteratorsCollection() {
        List<DoubleIterator> iterators = Arrays.asList(DoubleIterator.of(new double[] { 1.0, 2.0 }), DoubleIterator.of(new double[] { 3.0, 4.0 }));
        DoubleStream result = DoubleStream.concatIterators(iterators);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result.toArray(), 0.001);
    }

    @Test
    public void testMergeIteratorThreeWay() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 4.0, 7.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 2.0, 5.0, 8.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 3.0, 6.0, 9.0 });
        double[] result = DoubleStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 }, result, 0.001);
    }

    @Test
    public void testZipIteratorsThreeWay() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 10.0, 20.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 100.0, 200.0 });
        double[] result = DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new double[] { 111.0, 222.0 }, result, 0.001);
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0, 3.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 10.0, 20.0 });
        double[] result = DoubleStream.zip(a, b, 0.0, 0.0, Double::sum).toArray();
        assertArrayEquals(new double[] { 11.0, 22.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testZipIteratorsThreeWayWithDefaults() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 10.0, 20.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 100.0, 200.0, 300.0 });
        double[] result = DoubleStream.zip(a, b, c, 0.0, 0.0, 0.0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new double[] { 111.0, 220.0, 300.0 }, result, 0.001);
    }

    @Test
    public void testZipStreamsThreeWayWithDefaults() {
        double[] result = DoubleStream
                .zip(DoubleStream.of(1.0), DoubleStream.of(10.0, 20.0), DoubleStream.of(100.0, 200.0, 300.0), 0.0, 0.0, 0.0, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new double[] { 111.0, 220.0, 300.0 }, result, 0.001);
    }

    @Test
    public void testZipCollectionOfStreams() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(10.0, 20.0));
        double[] result = DoubleStream.zip(streams, values -> values[0] + values[1]).toArray();
        assertArrayEquals(new double[] { 11.0, 22.0 }, result, 0.001);
    }

    @Test
    public void testMergeStreamsThreeWay() {
        double[] result = DoubleStream
                .merge(DoubleStream.of(1.0, 4.0, 7.0), DoubleStream.of(2.0, 5.0, 8.0), DoubleStream.of(3.0, 6.0, 9.0),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 }, result, 0.001);
    }

    @Test
    public void testToMap_WithMerge() {
        Map<Boolean, Double> map = createDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).toMap(n -> n % 2 == 0, n -> n, Double::sum);
        assertEquals(12.0, map.get(true), 0.001);
        assertEquals(9.0, map.get(false), 0.001);
    }

    @Test
    public void testToMap_WithMapSupplier() {
        Map<String, Double> map = createDoubleStream(1.0, 2.0, 3.0).toMap(n -> "key" + n, n -> n * 10, Suppliers.ofMap());
        assertEquals(3, map.size());
        assertEquals(10.0, map.get("key1.0"), 0.001);
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        HashMap<Boolean, List<Double>> grouped = createDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(),
                HashMap::new);
        assertEquals(2, grouped.size());
        assertTrue(grouped.get(true).contains(2.0));
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Double> result = createDoubleStream(1.0, 2.0, 3.0).collect(ArrayList::new, (list, n) -> list.add(n));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testOfOptionalDouble_Empty() {
        DoubleStream result = DoubleStream.of(OptionalDouble.empty());
        assertEquals(0, result.count());
    }

    @Test
    public void testOfOptionalDouble_Present() {
        DoubleStream result = DoubleStream.of(OptionalDouble.of(42.5));
        assertArrayEquals(new double[] { 42.5 }, result.toArray(), 0.001);
    }

    // ===== New tests for untested methods =====

    @Test
    public void testConcatIterators_Collection() {
        List<DoubleIterator> iters = new ArrayList<>();
        iters.add(DoubleIterator.of(new double[] { 1.0, 2.0 }));
        iters.add(DoubleIterator.of(new double[] { 3.0, 4.0 }));
        double[] result = DoubleStream.concatIterators(iters).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testConcatIterators_EmptyCollection() {
        List<DoubleIterator> iters = new ArrayList<>();
        double[] result = DoubleStream.concatIterators(iters).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testLimit_WithOffset() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).limit(1, 3).toArray();
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testLimit_WithOffsetBeyondSize() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).limit(5, 3).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testFlatMapArrayToObj_HappyPath2() {
        List<String> result = DoubleStream.of(1.0, 2.0, 3.0).flatMapArrayToObj(d -> new String[] { String.valueOf(d), String.valueOf(d * 10) }).toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testFlatMapArrayToObj_Empty2() {
        List<String> result = DoubleStream.empty().flatMapArrayToObj(d -> new String[] { String.valueOf(d) }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapMulti_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        }).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testMapMulti_Empty2() {
        double[] result = DoubleStream.empty().mapMulti((value, consumer) -> consumer.accept(value)).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testMapPartial_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).mapPartial(d -> d % 2 == 0 ? OptionalDouble.of(d * 10) : OptionalDouble.empty()).toArray();
        assertArrayEquals(new double[] { 20.0, 40.0 }, result, 0.001);
    }

    @Test
    public void testMapPartialJdk_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
                .mapPartialJdk(d -> d % 2 == 0 ? java.util.OptionalDouble.of(d * 10) : java.util.OptionalDouble.empty())
                .toArray();
        assertArrayEquals(new double[] { 20.0, 40.0 }, result, 0.001);
    }

    @Test
    public void testRangeMap_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 1.0, 2.0, 2.0, 3.0).rangeMap((a, b) -> a == b, Double::sum).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testRangeMapToObj_HappyPath2() {
        List<String> result = DoubleStream.of(1.0, 1.0, 2.0, 3.0, 3.0).rangeMapToObj((a, b) -> a == b, (a, b) -> a + "-" + b).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testScan_BasicScan2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0).scan(Double::sum).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 6.0, 10.0 }, result, 0.001);
    }

    @Test
    public void testScan_WithInit2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).scan(10.0, Double::sum).toArray();
        assertArrayEquals(new double[] { 11.0, 13.0, 16.0 }, result, 0.001);
    }

    @Test
    public void testScan_WithInitIncluded2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).scan(10.0, true, Double::sum).toArray();
        assertArrayEquals(new double[] { 10.0, 11.0, 13.0, 16.0 }, result, 0.001);
    }

    @Test
    public void testCollapse_ListForm() {
        List<DoubleList> result = DoubleStream.of(1.0, 1.0, 2.0, 2.0, 3.0).collapse((a, b) -> a == b).toList();
        assertEquals(3, result.size());
        assertArrayEquals(new double[] { 1.0, 1.0 }, result.get(0).toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 2.0 }, result.get(1).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0 }, result.get(2).toArray(), 0.001);
    }

    @Test
    public void testCollapse_WithTriPredicate2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0, 12.0).collapse((first, previous, current) -> current - first <= 2, Double::sum).toArray();
        assertArrayEquals(new double[] { 6.0, 33.0 }, result, 0.001);
    }

    @Test
    public void testSkipUntil_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).skipUntil(x -> x >= 3.0).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testSkipUntil_NeverMatches() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).skipUntil(x -> x > 10.0).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testStep_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).step(2).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testStep_StepThree() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0).step(3).toArray();
        assertArrayEquals(new double[] { 1.0, 4.0, 7.0 }, result, 0.001);
    }

    @Test
    public void testCycled_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).cycled().limit(7).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0 }, result, 0.001);
    }

    @Test
    public void testCycled_WithRounds() {
        double[] result = DoubleStream.of(1.0, 2.0).cycled(3).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 1.0, 2.0, 1.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testReverseSorted_HappyPath() {
        double[] result = DoubleStream.of(3.0, 1.0, 4.0, 1.0, 5.0).reverseSorted().toArray();
        assertArrayEquals(new double[] { 5.0, 4.0, 3.0, 1.0, 1.0 }, result, 0.001);
    }

    @Test
    public void testReverseSorted_Empty() {
        double[] result = DoubleStream.empty().reverseSorted().toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testIntersection_HappyPath() {
        Set<Double> other = new java.util.HashSet<>(Arrays.asList(2.0, 4.0, 6.0));
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).intersection(other).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testDifference_HappyPath() {
        Set<Double> other = new java.util.HashSet<>(Arrays.asList(2.0, 4.0));
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).difference(other).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testSymmetricDifference_HappyPath() {
        List<Double> other = Arrays.asList(2.0, 4.0, 6.0);
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).symmetricDifference(other).sorted().toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testIndexed_HappyPath() {
        List<IndexedDouble> result = DoubleStream.of(10.0, 20.0, 30.0).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10.0, result.get(0).value(), 0.001);
        assertEquals(1, result.get(1).index());
        assertEquals(20.0, result.get(1).value(), 0.001);
    }

    @Test
    public void testFirst_HappyPath2() {
        OptionalDouble result = DoubleStream.of(5.0, 3.0, 1.0).first();
        assertTrue(result.isPresent());
        assertEquals(5.0, result.get(), 0.001);
    }

    @Test
    public void testFirst_Empty() {
        OptionalDouble result = DoubleStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_HappyPath() {
        OptionalDouble result = DoubleStream.of(5.0, 3.0, 1.0).last();
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get(), 0.001);
    }

    @Test
    public void testLast_Empty() {
        OptionalDouble result = DoubleStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testJoin_WithDelimiter() {
        String result = DoubleStream.of(1.0, 2.0, 3.0).join(", ");
        assertNotNull(result);
        assertTrue(result.contains("1.0"));
        assertTrue(result.contains("2.0"));
    }

    @Test
    public void testJoin_WithDelimiterAndPrefixSuffix() {
        String result = DoubleStream.of(1.0, 2.0, 3.0).join(", ", "[", "]");
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    @Test
    public void testToList_HappyPath() {
        List<Double> result = DoubleStream.of(1.0, 2.0, 3.0).toList();
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testToSet_HappyPath2() {
        Set<Double> result = DoubleStream.of(1.0, 2.0, 2.0, 3.0, 3.0).toSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
    }

    @Test
    public void testToMultiset_HappyPath() {
        Multiset<Double> result = DoubleStream.of(1.0, 2.0, 2.0, 3.0, 3.0, 3.0).toMultiset();
        assertEquals(1, result.get(1.0));
        assertEquals(2, result.get(2.0));
        assertEquals(3, result.get(3.0));
    }

    @Test
    public void testToImmutableList_HappyPath() {
        com.landawn.abacus.util.ImmutableList<Double> result = DoubleStream.of(1.0, 2.0, 3.0).toImmutableList();
        assertEquals(3, result.size());
        assertEquals(Double.valueOf(1.0), result.get(0));
    }

    @Test
    public void testToImmutableSet_HappyPath() {
        com.landawn.abacus.util.ImmutableSet<Double> result = DoubleStream.of(1.0, 2.0, 2.0, 3.0).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
    }

    @Test
    public void testCount_HappyPath2() {
        long result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).count();
        assertEquals(5, result);
    }

    @Test
    public void testCount_Empty() {
        long result = DoubleStream.empty().count();
        assertEquals(0, result);
    }

    @Test
    public void testSkip_WithAction() {
        List<Double> skipped = new ArrayList<>();
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).skip(2, value -> skipped.add(value)).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 2.0), skipped);
    }

    @Test
    public void testFilter_WithAction() {
        List<Double> rejected = new ArrayList<>();
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).filter(x -> x % 2 == 0, value -> rejected.add(value)).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 3.0, 5.0), rejected);
    }

    @Test
    public void testDropWhile_WithAction() {
        List<Double> dropped = new ArrayList<>();
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).dropWhile(x -> x <= 3.0, value -> dropped.add(value)).toArray();
        assertArrayEquals(new double[] { 4.0, 5.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), dropped);
    }

    @Test
    public void testForeach_HappyPath2() {
        List<Double> collected = new ArrayList<>();
        DoubleStream.of(1.0, 2.0, 3.0).foreach(collected::add);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), collected);
    }

    @Test
    public void testPsp_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).psp(s -> s.filter(x -> x % 2 != 0)).toArray();
        Arrays.sort(result);
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testSps_HappyPath() {
        double sum = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).sps(s -> s.map(x -> x * 2)).sum();
        assertEquals(30.0, sum, 0.001);
    }

    @Test
    public void testTransform_HappyPath2() {
        double sum = DoubleStream.of(1.0, 2.0, 3.0).transform(s -> s.map(x -> x * 2)).sum();
        assertEquals(12.0, sum, 0.001);
    }

    @Test
    public void testTransformB_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).transformB(s -> s.map(x -> x * 2)).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testZipWith_CollectionOfStreams() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0, 3.0), DoubleStream.of(10.0, 20.0, 30.0));
        double[] result = DoubleStream.zip(streams, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new double[] { 11.0, 22.0, 33.0 }, result, 0.001);
    }

    @Test
    public void testZipWith_CollectionOfStreamsWithDefaults() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(10.0, 20.0, 30.0));
        double[] result = DoubleStream.zip(streams, new double[] { 0.0, 0.0 }, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new double[] { 11.0, 22.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testMerge_CollectionOfStreams() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 3.0, 5.0), DoubleStream.of(2.0, 4.0, 6.0));
        double[] result = DoubleStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testMerge_ThreeArrays() {
        double[] result = DoubleStream
                .merge(new double[] { 1.0, 4.0 }, new double[] { 2.0, 5.0 }, new double[] { 3.0, 6.0 },
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testMerge_ThreeIterators() {
        double[] result = DoubleStream
                .merge(DoubleIterator.of(new double[] { 1.0, 4.0 }), DoubleIterator.of(new double[] { 2.0, 5.0 }), DoubleIterator.of(new double[] { 3.0, 6.0 }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testCollapse_WithMergeFunction2() {
        double[] result = DoubleStream.of(1.0, 1.0, 2.0, 2.0, 3.0).collapse((a, b) -> a == b, Double::sum).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testPercentiles_HappyPath() {
        Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertEquals(1.0, result.left().getMin(), 0.001);
        assertEquals(5.0, result.left().getMax(), 0.001);
    }

    @Test
    public void testOnClose_HappyPath() {
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);
        DoubleStream s = DoubleStream.of(1.0, 2.0, 3.0).onClose(() -> closed.set(true));
        s.toArray();
        s.close();
        assertTrue(closed.get());
    }

    @Test
    public void testAcceptIfNotEmpty_HappyPath() {
        AtomicInteger called = new AtomicInteger(0);
        DoubleStream.of(1.0, 2.0, 3.0).acceptIfNotEmpty(s -> called.set((int) s.sum()));
        assertEquals(6, called.get());
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        AtomicInteger called = new AtomicInteger(0);
        DoubleStream.empty().acceptIfNotEmpty(s -> called.set(1));
        assertEquals(0, called.get());
    }

    @Test
    public void testApplyIfNotEmpty_HappyPath() {
        Optional<Double> result = DoubleStream.of(1.0, 2.0, 3.0).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(6.0, result.get(), 0.001);
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        Optional<Double> result = DoubleStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).throwIfEmpty().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testThrowIfEmpty_EmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> DoubleStream.empty().throwIfEmpty().toArray());
    }

    @Test
    public void testElementAt_HappyPath() {
        OptionalDouble result = DoubleStream.of(10.0, 20.0, 30.0, 40.0).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(30.0, result.get(), 0.001);
    }

    @Test
    public void testElementAt_BeyondSize() {
        OptionalDouble result = DoubleStream.of(10.0, 20.0).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_HappyPath() {
        OptionalDouble result = DoubleStream.of(42.0).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42.0, result.get(), 0.001);
    }

    @Test
    public void testOnlyOne_EmptyStream() {
        OptionalDouble result = DoubleStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_TooManyElements() {
        assertThrows(Exception.class, () -> DoubleStream.of(1.0, 2.0).onlyOne());
    }

    @Test
    public void testFlatten_WithAlignment2() {
        double[][] a = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0 } };
        double[] result = DoubleStream.flatten(a, 0.0, true).toArray();
        assertArrayEquals(new double[] { 1.0, 4.0, 2.0, 5.0, 3.0, 0.0 }, result, 0.001);
    }

    @Test
    public void testCollect_TwoArg2() {
        List<Double> result = DoubleStream.of(1.0, 2.0, 3.0).collect(ArrayList::new, (list, val) -> list.add(val));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testToMap_WithMergeFunction2() {
        Map<Double, Double> result = DoubleStream.of(1.0, 2.0, 1.0, 3.0, 2.0).toMap(d -> d, d -> 1.0, Double::sum);
        assertEquals(3, result.size());
        assertEquals(2.0, result.get(1.0), 0.001);
        assertEquals(2.0, result.get(2.0), 0.001);
        assertEquals(1.0, result.get(3.0), 0.001);
    }

    @Test
    public void testFlattMap_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).flattMap(d -> java.util.stream.DoubleStream.of(d, d * 10)).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testFlatmap_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).flatmap(d -> new double[] { d, d * 10 }).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapToObj_HappyPath() {
        List<String> result = DoubleStream.of(1.0, 2.0).flatmapToObj(d -> Arrays.asList(String.valueOf(d), String.valueOf(d * 10))).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testPeek_HappyPath2() {
        List<Double> peeked = new ArrayList<>();
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).peek(peeked::add).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), peeked);
    }

    @Test
    public void testShuffled_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).shuffled().toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testShuffled_WithRandom() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testRotated_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).rotated(2).toArray();
        assertArrayEquals(new double[] { 4.0, 5.0, 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRotated_NegativeDistance() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).rotated(-2).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0, 1.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testReversed_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).reversed().toArray();
        assertArrayEquals(new double[] { 3.0, 2.0, 1.0 }, result, 0.001);
    }

    @Test
    public void testReversed_Empty() {
        double[] result = DoubleStream.empty().reversed().toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testIsParallel_HappyPath() {
        assertFalse(DoubleStream.of(1.0, 2.0, 3.0).isParallel());
        assertTrue(DoubleStream.of(1.0, 2.0, 3.0).parallel().isParallel());
    }

    @Test
    public void testSequential_AfterParallel() {
        double sum = DoubleStream.of(1.0, 2.0, 3.0).parallel().sequential().sum();
        assertEquals(6.0, sum, 0.001);
    }

    @Test
    public void testFlatten_3D() {
        double[][][] a = { { { 1.0, 2.0 }, { 3.0, 4.0 } }, { { 5.0, 6.0 } } };
        double[] result = DoubleStream.flatten(a).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testRepeat_HappyPath() {
        double[] result = DoubleStream.repeat(3.14, 4).toArray();
        assertEquals(4, result.length);
        for (double d : result) {
            assertEquals(3.14, d, 0.001);
        }
    }

    @Test
    public void testRepeat_ZeroTimes() {
        double[] result = DoubleStream.repeat(3.14, 0).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testFindLast_HappyPath() {
        OptionalDouble result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).findLast(x -> x < 4.0);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.get(), 0.001);
    }

    @Test
    public void testFindLast_Empty() {
        OptionalDouble result = DoubleStream.empty().findLast(x -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_HappyPath2() {
        OptionalDouble result = DoubleStream.of(3.0, 1.0, 4.0, 1.0, 5.0).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.get(), 0.001);
    }

    @Test
    public void testMapToFloat_HappyPath2() {
        float[] result = DoubleStream.of(1.0, 2.0, 3.0).mapToFloat(d -> (float) d).toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
    }

    @Test
    public void testFlatMapToFloat_HappyPath2() {
        float[] result = DoubleStream.of(1.0, 2.0).flatMapToFloat(d -> FloatStream.of((float) d, (float) (d * 10))).toArray();
        assertEquals(4, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(10.0f, result[1], 0.001f);
    }

    @Test
    public void testRangeMap_Empty() {
        double[] result = DoubleStream.empty().rangeMap((a, b) -> a == b, Double::sum).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testScan_EmptyStream2() {
        double[] result = DoubleStream.empty().scan(Double::sum).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testMapToChar() {
        char[] result = DoubleStream.of(65.0, 66.0, 67.0)
                .mapToObj(d -> (char) (int) d)
                .toList()
                .stream()
                .map(c -> c.toString())
                .reduce("", String::concat)
                .toCharArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testFlatMapToLong_HappyPath() {
        long[] result = DoubleStream.of(1.0, 2.0).flatMapToLong(d -> LongStream.of((long) d, (long) (d * 10))).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatMapToInt_HappyPath() {
        int[] result = DoubleStream.of(1.0, 2.0).flatMapToInt(d -> IntStream.of((int) d, (int) (d * 10))).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, result);
    }

}
