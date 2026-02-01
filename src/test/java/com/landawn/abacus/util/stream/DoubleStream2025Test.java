package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

@Tag("2025")
public class DoubleStream2025Test extends TestBase {

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
        List<String> result = stream.flattmapToObj(x -> new String[] { "A" + x, "B" + x }).toList();
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
        assertEquals(N.asList("1.0-2.0", "3.0-3.0", "10.0-11.0"), result);
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
    public void testConcatIterators() {
        DoubleIterator iter1 = DoubleIterator.of(new double[] { 1.0, 2.0 });
        DoubleIterator iter2 = DoubleIterator.of(new double[] { 3.0, 4.0 });
        DoubleStream stream = DoubleStream.concat(iter1, iter2);
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
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
                .debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // All elements should pass
        assertEquals(5, result.length);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testDebounce_EmptyStream() {
        double[] result = DoubleStream.empty()
                .debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        double[] result = DoubleStream.of(42.5)
                .debounce(1, com.landawn.abacus.util.Duration.ofMillis(100))
                .toArray();

        assertEquals(1, result.length);
        assertEquals(42.5, result[0], 0.001);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
                .debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

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

        double[] result = DoubleStream.of(input)
                .debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10))
                .toArray();

        assertEquals(500, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        double[] result = DoubleStream.of(10.0, 20.0, 30.0, 40.0, 50.0)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertArrayEquals(new double[] { 10.0, 20.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
                .filter(n -> n % 2 == 0)  // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))  // 2, 4, 6
                .map(n -> n * 10)  // 20, 40, 60
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
}
