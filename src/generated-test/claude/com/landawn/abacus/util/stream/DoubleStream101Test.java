package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoubleMapMultiConsumer;
import com.landawn.abacus.util.function.DoubleToFloatFunction;
import com.landawn.abacus.util.function.DoubleTriPredicate;

public class DoubleStream101Test extends TestBase {

    // This method needs to be implemented by a concrete test class to provide a DoubleStream instance.
    // For example, in ArrayDoubleStreamTest, it would return new ArrayDoubleStream(a);
    // In IteratorDoubleStreamTest, it would return new IteratorDoubleStream(DoubleIterator.of(a));
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
    public void testMap() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleUnaryOperator mapper = x -> x * 2;

        double[] result = stream.map(mapper).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testMapToInt() {
        DoubleStream stream = createDoubleStream(1.5, 2.7, 3.9);
        DoubleToIntFunction mapper = x -> (int) Math.round(x);

        int[] result = stream.mapToInt(mapper).toArray();
        assertArrayEquals(new int[] { 2, 3, 4 }, result);
    }

    @Test
    public void testMapToLong() {
        DoubleStream stream = createDoubleStream(1.5, 2.7, 3.9);
        DoubleToLongFunction mapper = x -> Math.round(x);

        long[] result = stream.mapToLong(mapper).toArray();
        assertArrayEquals(new long[] { 2L, 3L, 4L }, result);
    }

    @Test
    public void testMapToFloat() {
        DoubleStream stream = createDoubleStream(1.5, 2.7, 3.9);
        DoubleToFloatFunction mapper = x -> (float) x;

        float[] result = stream.mapToFloat(mapper).toArray();
        assertArrayEquals(new float[] { 1.5f, 2.7f, 3.9f }, result, 0.0001f);
    }

    @Test
    public void testMapToObj() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleFunction<String> mapper = x -> "Value: " + x;

        List<String> result = stream.mapToObj(mapper).toList();
        assertEquals(Arrays.asList("Value: 1.0", "Value: 2.0", "Value: 3.0"), result);
    }

    @Test
    public void testFlatMap() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleFunction<DoubleStream> mapper = x -> createDoubleStream(x, x * 2);

        double[] result = stream.flatMap(mapper).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0, 3.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testFlatmap() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<double[]> mapper = x -> new double[] { x, x + 1 };

        double[] result = stream.flatmap(mapper).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testFlattmap() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<java.util.stream.DoubleStream> mapper = x -> java.util.stream.DoubleStream.of(x, x * 3);

        double[] result = stream.flattMap(mapper).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 2.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testFlatMapToInt() {
        DoubleStream stream = createDoubleStream(1.5, 2.5);
        DoubleFunction<IntStream> mapper = x -> IntStream.of((int) x, (int) (x + 1));

        int[] result = stream.flatMapToInt(mapper).toArray();
        assertArrayEquals(new int[] { 1, 2, 2, 3 }, result);
    }

    @Test
    public void testFlatMapToLong() {
        DoubleStream stream = createDoubleStream(1.5, 2.5);
        DoubleFunction<LongStream> mapper = x -> LongStream.of(Math.round(x), Math.round(x * 2));

        long[] result = stream.flatMapToLong(mapper).toArray();
        assertArrayEquals(new long[] { 2L, 3L, 3L, 5L }, result);
    }

    @Test
    public void testFlatMapToFloat() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<FloatStream> mapper = x -> FloatStream.of((float) x, (float) (x / 2));

        float[] result = stream.flatMapToFloat(mapper).toArray();
        assertArrayEquals(new float[] { 1.0f, 0.5f, 2.0f, 1.0f }, result, 0.0001f);
    }

    @Test
    public void testFlatMapToObj() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<Stream<String>> mapper = x -> Stream.of("A" + x, "B" + x);

        List<String> result = stream.flatMapToObj(mapper).toList();
        assertEquals(Arrays.asList("A1.0", "B1.0", "A2.0", "B2.0"), result);
    }

    @Test
    public void testFlatmapToObj() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<Collection<Integer>> mapper = x -> Arrays.asList((int) x, (int) (x * 2));

        List<Integer> result = stream.flatmapToObj(mapper).toList();
        assertEquals(Arrays.asList(1, 2, 2, 4), result);
    }

    @Test
    public void testFlattMapToObj() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<String[]> mapper = x -> new String[] { "X" + x, "Y" + x };

        List<String> result = stream.flattmapToObj(mapper).toList();
        assertEquals(Arrays.asList("X1.0", "Y1.0", "X2.0", "Y2.0"), result);
    }

    @Test
    public void testMapMulti() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleMapMultiConsumer mapper = (value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 2);
        };

        double[] result = stream.mapMulti(mapper).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0, 3.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testMapPartial() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        DoubleFunction<OptionalDouble> mapper = x -> x % 2 == 0 ? OptionalDouble.of(x * 2) : OptionalDouble.empty();

        double[] result = stream.mapPartial(mapper).toArray();
        assertArrayEquals(new double[] { 4.0, 8.0 }, result, 0.0001);
    }

    @Test
    public void testMapPartialJdk() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        DoubleFunction<java.util.OptionalDouble> mapper = x -> x > 2 ? java.util.OptionalDouble.of(x * 3) : java.util.OptionalDouble.empty();

        double[] result = stream.mapPartialJdk(mapper).toArray();
        assertArrayEquals(new double[] { 9.0, 12.0 }, result, 0.0001);
    }

    @Test
    public void testRangeMap() {
        DoubleStream stream = createDoubleStream(1.0, 1.5, 2.0, 5.0, 5.5, 6.0);
        DoubleBiPredicate sameRange = (a, b) -> Math.abs(a - b) < 1;
        DoubleBinaryOperator mapper = (a, b) -> (a + b) / 2;

        double[] result = stream.rangeMap(sameRange, mapper).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testRangeMapToObj() {
        DoubleStream stream = createDoubleStream(1.0, 1.5, 3.0, 3.5);
        DoubleBiPredicate sameRange = (a, b) -> Math.abs(a - b) < 1;
        DoubleBiFunction<String> mapper = (a, b) -> "[" + a + "-" + b + "]";

        List<String> result = stream.rangeMapToObj(sameRange, mapper).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testCollapse() {
        DoubleStream stream = createDoubleStream(1.0, 1.1, 1.2, 3.0, 3.1, 5.0);
        DoubleBiPredicate collapsible = (a, b) -> Math.abs(a - b) < 0.5;

        List<DoubleList> result = stream.collapse(collapsible).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size()); // [1.0, 1.1, 1.2]
        assertEquals(2, result.get(1).size()); // [3.0, 3.1]
        assertEquals(1, result.get(2).size()); // [5.0]
    }

    @Test
    public void testCollapseWithMerge() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 10.0, 11.0);
        DoubleBiPredicate collapsible = (a, b) -> Math.abs(a - b) <= 1;
        DoubleBinaryOperator mergeFunction = Double::sum;

        double[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertArrayEquals(new double[] { 6.0, 21.0 }, result, 0.0001); // (1+2+3), (10+11)
    }

    @Test
    public void testCollapseWithTriPredicate() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 10.0, 20.0);
        DoubleTriPredicate collapsible = (first, prev, curr) -> curr - first < 5;
        DoubleBinaryOperator mergeFunction = Double::max;

        double[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertEquals(3, result.length);
        assertArrayEquals(new double[] { 3.0, 10.0, 20.0 }, result, 0.0001); // (1,2,3), (10), (20)
    }

    @Test
    public void testScan() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        DoubleBinaryOperator accumulator = Double::sum;

        double[] result = stream.scan(accumulator).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 6.0, 10.0 }, result, 0.0001);
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
    public void testAppendIfEmpty() {
        DoubleStream emptyStream = DoubleStream.empty();
        double[] result1 = emptyStream.appendIfEmpty(1.0, 2.0, 3.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result1, 0.0001);

        DoubleStream nonEmptyStream = createDoubleStream(5.0);
        double[] result2 = nonEmptyStream.appendIfEmpty(1.0, 2.0, 3.0).toArray();
        assertArrayEquals(new double[] { 5.0 }, result2, 0.0001);
    }

    @Test
    public void testTopWithComparator() {
        DoubleStream stream = createDoubleStream(5.0, 2.0, 8.0, 1.0, 9.0, 3.0);
        Comparator<Double> reverseComparator = (a, b) -> Double.compare(b, a);
        double[] result = stream.top(3, reverseComparator).toArray();
        assertArrayEquals(new double[] { 3.0, 1.0, 2.0 }, result, 0.0001);
    }

    @Test
    public void testToDoubleList() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleList list = stream.toDoubleList();
        assertEquals(3, list.size());
        assertEquals(1.0, list.get(0), 0.0001);
        assertEquals(2.0, list.get(1), 0.0001);
        assertEquals(3.0, list.get(2), 0.0001);
    }

    @Test
    public void testToMap() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        Map<String, Double> map = stream.toMap(x -> "key" + (int) x, x -> x * 2);

        assertEquals(3, map.size());
        assertEquals(2.0, map.get("key1"), 0.0001);
        assertEquals(4.0, map.get("key2"), 0.0001);
        assertEquals(6.0, map.get("key3"), 0.0001);
    }

    @Test
    public void testToMapWithMerge() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 1.0, 3.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x, Double::sum);

        assertEquals(3, map.size());
        assertEquals(2.0, map.get(1), 0.0001); // 1.0 + 1.0
        assertEquals(2.0, map.get(2), 0.0001);
        assertEquals(3.0, map.get(3), 0.0001);
    }

    @Test
    public void testGroupTo() {
        DoubleStream stream = createDoubleStream(1.1, 2.2, 1.3, 2.4, 1.5);
        Map<Integer, List<Double>> grouped = stream.groupTo(x -> (int) x, Collectors.toList());

        assertEquals(2, grouped.size());
        assertEquals(3, grouped.get(1).size()); // [1.1, 1.3, 1.5]
        assertEquals(2, grouped.get(2).size()); // [2.2, 2.4]
    }

    @Test
    public void testReduce() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        double result = stream.reduce(0.0, Double::sum);
        assertEquals(10.0, result, 0.0001);
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
    public void testCollect() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        ArrayList<Double> result = stream.collect(ArrayList::new, (list, value) -> list.add(value), (list1, list2) -> list1.addAll(list2));

        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testCollectWithoutCombiner() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleList result = stream.collect(DoubleList::new, DoubleList::add);

        assertEquals(3, result.size());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result.toArray(), 0.0001);
    }

    @Test
    public void testForeach() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        List<Double> list = new ArrayList<>();
        stream.foreach(list::add);

        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);
    }

    @Test
    public void testForEach() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        List<Double> list = new ArrayList<>();
        stream.forEach(list::add);

        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);
    }

    @Test
    public void testForEachIndexed() {
        DoubleStream stream = createDoubleStream(10.0, 20.0, 30.0);
        Map<Integer, Double> map = new HashMap<>();
        stream.forEachIndexed((index, value) -> map.put(index, value));

        assertEquals(3, map.size());
        assertEquals(10.0, map.get(0), 0.0001);
        assertEquals(20.0, map.get(1), 0.0001);
        assertEquals(30.0, map.get(2), 0.0001);
    }

    @Test
    public void testAnyMatch() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        assertTrue(stream.anyMatch(x -> x > 3));

        DoubleStream stream2 = createDoubleStream(1.0, 2.0, 3.0);
        assertFalse(stream2.anyMatch(x -> x > 5));
    }

    @Test
    public void testAllMatch() {
        DoubleStream stream = createDoubleStream(2.0, 4.0, 6.0);
        assertTrue(stream.allMatch(x -> x % 2 == 0));

        DoubleStream stream2 = createDoubleStream(2.0, 3.0, 6.0);
        assertFalse(stream2.allMatch(x -> x % 2 == 0));
    }

    @Test
    public void testNoneMatch() {
        DoubleStream stream = createDoubleStream(1.0, 3.0, 5.0);
        assertTrue(stream.noneMatch(x -> x % 2 == 0));

        DoubleStream stream2 = createDoubleStream(1.0, 2.0, 3.0);
        assertFalse(stream2.noneMatch(x -> x % 2 == 0));
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
    public void testMin() {
        DoubleStream stream = createDoubleStream(3.0, 1.0, 4.0, 2.0);
        OptionalDouble min = stream.min();
        assertTrue(min.isPresent());
        assertEquals(1.0, min.getAsDouble(), 0.0001);
    }

    @Test
    public void testMax() {
        DoubleStream stream = createDoubleStream(3.0, 1.0, 4.0, 2.0);
        OptionalDouble max = stream.max();
        assertTrue(max.isPresent());
        assertEquals(4.0, max.getAsDouble(), 0.0001);
    }

    @Test
    public void testKthLargest() {
        DoubleStream stream = createDoubleStream(3.0, 1.0, 4.0, 5.0, 2.0);
        OptionalDouble result = stream.kthLargest(2); // 2nd largest
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.0001);
    }

    @Test
    public void testSum() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        double sum = stream.sum();
        assertEquals(10.0, sum, 0.0001);
    }

    @Test
    public void testAverage() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble avg = stream.average();
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.getAsDouble(), 0.0001);
    }

    @Test
    public void testSummarize() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0);
        DoubleSummaryStatistics stats = stream.summarize();

        assertEquals(5, stats.getCount());
        assertEquals(1.0, stats.getMin(), 0.0001);
        assertEquals(5.0, stats.getMax(), 0.0001);
        assertEquals(15.0, stats.getSum(), 0.0001);
        assertEquals(3.0, stats.getAverage(), 0.0001);
    }

    @Test
    public void testSummarizeAndPercentiles() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0);
        Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result = stream.summarizeAndPercentiles();

        DoubleSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(15.0, stats.getSum(), 0.0001);

        assertTrue(result.right().isPresent());
        Map<Percentage, Double> percentiles = result.right().get();
        assertNotNull(percentiles);
    }

    @Test
    public void testMergeWith() {
        DoubleStream a = createDoubleStream(1.0, 3.0, 5.0);
        DoubleStream b = createDoubleStream(2.0, 4.0, 6.0);
        DoubleBiFunction<MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        double[] result = a.mergeWith(b, selector).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testZipWith() {
        DoubleStream a = createDoubleStream(1.0, 2.0, 3.0);
        DoubleStream b = createDoubleStream(4.0, 5.0, 6.0);

        double[] result = a.zipWith(b, Double::sum).toArray();
        assertArrayEquals(new double[] { 5.0, 7.0, 9.0 }, result, 0.0001);
    }

    @Test
    public void testZipWithThreeStreams() {
        DoubleStream a = createDoubleStream(1.0, 2.0);
        DoubleStream b = createDoubleStream(3.0, 4.0);
        DoubleStream c = createDoubleStream(5.0, 6.0);

        double[] result = a.zipWith(b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new double[] { 9.0, 12.0 }, result, 0.0001);
    }

    @Test
    public void testZipWithDefaults() {
        DoubleStream a = createDoubleStream(1.0, 2.0);
        DoubleStream b = createDoubleStream(3.0, 4.0, 5.0);

        double[] result = a.zipWith(b, 10.0, 20.0, Double::sum).toArray();
        assertArrayEquals(new double[] { 4.0, 6.0, 15.0 }, result, 0.0001);
    }

    @Test
    public void testBoxed() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        List<Double> result = stream.boxed().toList();
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testToJdkStream() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        java.util.stream.DoubleStream jdkStream = stream.toJdkStream();
        double[] result = jdkStream.toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testTransformB() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        Function<java.util.stream.DoubleStream, java.util.stream.DoubleStream> transfer = s -> s.map(x -> x * 2);

        double[] result = stream.transformB(transfer).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testTransformBDeferred() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        Function<java.util.stream.DoubleStream, java.util.stream.DoubleStream> transfer = s -> {
            counter.incrementAndGet();
            return s.map(x -> x * 2);
        };

        DoubleStream transformed = stream.transformB(transfer, true);
        assertEquals(0, counter.get()); // Not executed yet due to deferred

        double[] result = transformed.toArray();
        assertEquals(1, counter.get()); // Executed once
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testEmptyStreamOperations() {
        DoubleStream emptyStream = DoubleStream.empty();

        assertFalse(emptyStream.min().isPresent());

        emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.max().isPresent());

        emptyStream = DoubleStream.empty();
        assertEquals(0.0, emptyStream.sum(), 0.0001);

        emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.average().isPresent());
    }
}
