package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleBinaryOperator;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoubleFunction;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.DoubleTernaryOperator;
import com.landawn.abacus.util.function.DoubleToLongFunction;
import com.landawn.abacus.util.function.DoubleUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

@Tag("new-test")
public class ParallelIteratorDoubleStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final double[] TEST_ARRAY = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0,
            21.0, 22.0, 23.0, 24.0, 25.0, 26.0 };
    private static final double[] DEFAULT_ARRAY = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };

    private DoubleStream parallelStream;

    protected DoubleStream createDoubleStream(double... elements) {
        return DoubleStream.of(elements).map(e -> (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createDoubleStream(DEFAULT_ARRAY);
    }

    @Test
    public void testFilter() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        List<Double> result = stream.filter(d -> d > 20.0).toList();
        assertEquals(6, result.size());
        for (Double d : result) {
            assertTrue(d > 20.0);
        }

        DoubleStream emptyStream = createDoubleStream(new double[] {});
        List<Double> emptyResult = emptyStream.filter(d -> true).toList();
        assertEquals(0, emptyResult.size());
    }

    @Test
    public void testTakeWhile() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        DoublePredicate predicate = d -> d < 4.0;
        List<Double> result = stream.takeWhile(predicate).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(3.0));
        assertFalse(result.contains(4.0));
    }

    @Test
    public void testDropWhile() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        List<Double> result = stream.dropWhile(d -> d < 4.0).toList();
        assertHaveSameElements(Arrays.asList(4.0, 5.0), result);

        DoubleStream stream2 = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        List<Double> result2 = stream2.dropWhile(d -> true).toList();
        assertEquals(0, result2.size());
    }

    @Test
    public void testMap() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        DoubleUnaryOperator mapper = d -> d + 1.0;
        List<Double> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2.0));
        assertFalse(result.contains(1.0));
    }

    @Test
    public void testMapToInt() {
        DoubleStream stream = createDoubleStream(new double[] { 1.5, 2.5, 3.5 });
        List<Integer> result = stream.mapToInt(d -> (int) d).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testMapToLong() {
        DoubleStream stream = createDoubleStream(new double[] { 10.1, 20.9, 30.0 });
        DoubleToLongFunction mapper = d -> (long) d;
        List<Long> result = stream.mapToLong(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(10L));
        assertTrue(result.contains(20L));
        assertTrue(result.contains(30L));
    }

    @Test
    public void testMapToFloat() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        List<Float> result = stream.mapToFloat(d -> (float) d).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(3.0f));
    }

    @Test
    public void testMapToObj() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        DoubleFunction<String> mapper = d -> "Double_" + d;
        List<String> result = stream.mapToObj(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains("Double_1.0"));
        assertTrue(result.contains("Double_26.0"));
    }

    @Test
    @DisplayName("Test flatMap method")
    public void testFlatMap() {
        DoubleStream flattened = parallelStream.flatMap(d -> DoubleStream.of(d, d + 10));
        double[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        flattened = parallelStream.flatMap(d -> d % 2 == 0 ? DoubleStream.of(d) : DoubleStream.empty());
        assertArrayEquals(new double[] { 2.0 }, flattened.toArray());
    }

    @Test
    public void testFlatMapDoubleArray() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleFunction<double[]> mapper = d -> new double[] { d, d + 0.5 };
        List<Double> result = stream.flatmap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
    }

    @Test
    public void testFlatMapToInt() {
        DoubleStream stream = createDoubleStream(new double[] { 1.5, 2.5 });
        List<Integer> result = stream.flatMapToInt(d -> IntStream.of((int) d, (int) (d * 2))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(3));
        assertTrue(result.contains(2));
        assertTrue(result.contains(5));
    }

    @Test
    public void testFlatMapToLong() {
        DoubleStream stream = createDoubleStream(new double[] { 1.5, 2.8 });
        DoubleFunction<LongStream> mapper = d -> LongStream.of((long) d, (long) (d * 2));
        List<Long> result = stream.flatMapToLong(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(3L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(5L));
    }

    @Test
    public void testFlatMapToFloat() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0 });
        List<Float> result = stream.flatMapToFloat(d -> FloatStream.of((float) d, (float) (d + 0.5))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    public void testFlatMapToObjStream() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleFunction<Stream<String>> mapper = d -> Stream.of("A" + d, "B" + d);
        List<String> result = stream.flatMapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B1.0"));
        assertTrue(result.contains("A2.0"));
        assertTrue(result.contains("B2.0"));
    }

    @Test
    public void testFlatmapToObj() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0 });
        List<String> result = stream.flatmapToObj(d -> Arrays.asList("X" + d, "Y" + d)).toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("X1.0", "Y1.0", "X2.0", "Y2.0")));
    }

    @Test
    public void testOnEach() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        List<Double> consumed = new ArrayList<>();
        DoubleConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.peek(action).forEach(d -> {
        });
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEachWithException() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        AtomicInteger count = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            stream.forEach(d -> {
                if (count.incrementAndGet() > 5) {
                    throw new RuntimeException("Test Exception");
                }
            });
        });
    }

    @Test
    public void testToMap() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 1.0 });
        Map<String, Double> result = stream.toMap(d -> "Key_" + d, d -> d, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(2.0, result.get("Key_1.0"), 0.0001);
        assertEquals(2.0, result.get("Key_2.0"), 0.0001);
        assertEquals(3.0, result.get("Key_3.0"), 0.0001);
    }

    @Test
    public void testGroupTo() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 1.0, 3.0, 2.0 });
        Map<String, List<Double>> result = stream.groupTo(d -> String.valueOf(d), Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(List.of(1.0, 1.0), result.get("1.0"));
        assertEquals(List.of(2.0, 2.0), result.get("2.0"));
        assertEquals(List.of(3.0), result.get("3.0"));
    }

    @Test
    public void testReduceWithIdentity() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleBinaryOperator op = (d1, d2) -> d1 + d2;
        double result = stream.reduce(0.0, op);
        assertEquals(6.0, result, 0.0001);

        DoubleStream emptyStream = createDoubleStream(new double[] {});
        double emptyResult = emptyStream.reduce(0, op);
        assertEquals(0, emptyResult, 0.0001);
    }

    @Test
    public void testReduceWithoutIdentity() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        OptionalDouble result = stream.reduce((d1, d2) -> d1 + d2);
        assertTrue(result.isPresent());
        assertEquals(15.0, result.getAsDouble(), 0.0001);

        DoubleStream emptyStream = createDoubleStream(new double[] {});
        OptionalDouble emptyResult = emptyStream.reduce((d1, d2) -> d1 + d2);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testCollect() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        List<Double> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1.0, 2.0, 3.0), collectedList);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).anyMatch(d -> d == 26.0));
        assertFalse(createDoubleStream(TEST_ARRAY).anyMatch(d -> d == 100.0));
        assertFalse(createDoubleStream(new double[] {}).anyMatch(d -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).allMatch(d -> d >= 1.0 && d <= 26.0));
        assertFalse(createDoubleStream(TEST_ARRAY).allMatch(d -> d < 26.0));
        assertTrue(createDoubleStream(new double[] {}).allMatch(d -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).noneMatch(d -> d > 100.0));
        assertFalse(createDoubleStream(TEST_ARRAY).noneMatch(d -> d == 1.0));
        assertTrue(createDoubleStream(new double[] {}).noneMatch(d -> true));
    }

    @Test
    public void testFindFirst() {
        DoubleStream stream = createDoubleStream(new double[] { 4.0, 2.0, 1.0, 3.0, 1.0 });
        OptionalDouble result = stream.findFirst(d -> d == 1.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get(), 0.0001);

        stream = createDoubleStream(new double[] { 4.0, 2.0, 1.0, 3.0, 1.0 });
        OptionalDouble notFound = stream.findFirst(d -> d == 10.0);
        assertFalse(notFound.isPresent());

        OptionalDouble empty = createDoubleStream(new double[] {}).findFirst(d -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalDouble result = parallelStream.findAny(d -> d > 5);

        assertTrue(result.isPresent());
        assertTrue(result.getAsDouble() > 5);

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findAny(d -> d > 30);
        assertFalse(result.isPresent());

        parallelStream = createDoubleStream(new double[] { 5.0 });
        result = parallelStream.findAny(d -> d == 5.0);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble());
    }

    @Test
    public void testFindLast() {
        DoubleStream stream = createDoubleStream(new double[] { 4.0, 2.0, 1.0, 3.0, 1.0 });
        OptionalDouble result = stream.findLast(d -> d == 1.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get(), 0.0001);

        stream = createDoubleStream(new double[] { 4.0, 2.0, 1.0, 3.0, 1.0 });
        OptionalDouble notFound = stream.findLast(d -> d == 10.0);
        assertFalse(notFound.isPresent());

        OptionalDouble empty = createDoubleStream(new double[] {}).findLast(d -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testZipWithTwoStreams() {
        DoubleStream streamA = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0, 30.0);
        double[] result = streamA.zipWith(streamB, (a, b) -> a + b).toArray();
        assertHaveSameElements(new double[] { 11.0, 22.0, 33.0 }, result);
    }

    @Test
    public void testZipWithTwoStreamsWithDefaultValues() {
        DoubleStream streamA = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream streamB = DoubleStream.of(10.0);
        double[] result = streamA.zipWith(streamB, 0.0, -1.0, (a, b) -> a + b).toArray();
        assertHaveSameElements(new double[] { 11.0, 1.0, 2.0 }, result);
    }

    @Test
    public void testZipWithTernaryOperator() {
        DoubleStream streamA = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0);
        DoubleStream streamC = DoubleStream.of(100.0, 101.0);
        DoubleTernaryOperator zipper = (d1, d2, d3) -> d1 + d2 + d3;
        List<Double> result = streamA.zipWith(streamB, streamC, zipper).sorted().toList();
        assertEquals(2, result.size());
        assertEquals(1.0 + 10.0 + 100.0, result.get(0), 0.0001);
        assertEquals(2.0 + 20.0 + 101.0, result.get(1), 0.0001);
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        DoubleStream streamA = createDoubleStream(new double[] { 1.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0);
        DoubleStream streamC = DoubleStream.of(100.0, 101.0, 102.0);
        double valA = 0.0;
        double valB = -1.0;
        double valC = -2.0;
        DoubleTernaryOperator zipper = (d1, d2, d3) -> {
            return d1 + d2 + d3;
        };
        List<Double> result = streamA.zipWith(streamB, streamC, valA, valB, valC, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(1.0 + 10.0 + 100.0, result.get(1), 0.0001);
        assertEquals(0.0 + 20.0 + 101.0, result.get(2), 0.0001);
        assertEquals(0.0 + -1.0 + 102.0, result.get(0), 0.0001);
    }

    @Test
    public void testIsParallel() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
        stream.close();
    }

    @Test
    public void testSequential() {
        DoubleStream parallelStream = createDoubleStream(TEST_ARRAY);
        DoubleStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        List<Double> result = sequentialStream.toList();
        assertEquals(TEST_ARRAY.length, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i), 0.0001);
        }
        parallelStream.close();
        sequentialStream.close();
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelIteratorDoubleStream) createDoubleStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ITERATOR, ((ParallelIteratorDoubleStream) createDoubleStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelIteratorDoubleStream) createDoubleStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        AtomicInteger closedCount = new AtomicInteger(0);
        Runnable handler1 = () -> closedCount.incrementAndGet();
        Runnable handler2 = () -> closedCount.incrementAndGet();

        DoubleStream newStream = stream.onClose(handler1).onClose(handler2);
        assertEquals(0, closedCount.get());
        newStream.close();
        assertEquals(2, closedCount.get());
    }

    @Test
    @DisplayName("Test flatMapToObj method")
    public void testFlatMapToObj() {
        Stream<String> flattened = parallelStream.flatMapToObj(d -> Stream.of("A" + d, "B" + d));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B10.0"));
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalDouble result = parallelStream.findFirst(d -> d > 5);

        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble());

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findFirst(d -> d > 30);
        assertFalse(result.isPresent());

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findFirst(d -> d == 1.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalDouble result = parallelStream.findLast(d -> d < 5);

        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble());

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findLast(d -> d > 30);
        assertFalse(result.isPresent());

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findLast(d -> d == 10.0);
        assertTrue(result.isPresent());
        assertEquals(10.0, result.getAsDouble());
    }

    @Test
    @DisplayName("Test zipWith three streams with ternary operator")
    public void testZipWithTernary() {
        DoubleStream stream2 = createDoubleStream(new double[] { 10.0, 20.0, 30.0, 40.0, 50.0 });
        DoubleStream stream3 = createDoubleStream(new double[] { 100.0, 100.0, 100.0, 100.0, 100.0 });
        DoubleStream zipped = parallelStream.zipWith(stream2, stream3, (a, b, c) -> (a + b) * c / 100);
        double[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new double[] { 11.0, 22.0, 33.0, 44.0, 55.0 }, result);
    }

    @Test
    @DisplayName("Test zipWith three streams with default values")
    public void testZipWithTernaryDefaults() {
        DoubleStream stream2 = createDoubleStream(new double[] { 10.0, 20.0 });
        DoubleStream stream3 = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0 });
        double[] result = parallelStream.zipWith(stream2, stream3, 0.0, 0.0, 1.0, (a, b, c) -> a + b + c).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(6.0, result[0]);
        assertEquals(24.0, result[9]);
    }

    @Test
    @DisplayName("Test edge cases")
    public void testEdgeCases() {
        parallelStream = createDoubleStream(new double[0]);
        assertEquals(0, parallelStream.count());

        parallelStream = createDoubleStream(new double[] { 42.5 });
        assertEquals(42.5, parallelStream.first().getAsDouble());

        parallelStream = createDoubleStream(new double[] { -5.0, -3.0, -1.0, 0.0, 1.0, 3.0, 5.0 });
        long positiveCount = parallelStream.filter(d -> d > 0).count();
        assertEquals(3, positiveCount);

        parallelStream = createDoubleStream(new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0 });
        long finiteCount = parallelStream.filter(Double::isFinite).count();
        assertEquals(1, finiteCount);
    }

    @Test
    @DisplayName("Test exception handling in parallel operations")
    public void testExceptionHandling() {
        assertThrows(RuntimeException.class, () -> {
            parallelStream.filter(d -> {
                if (d == 5.0)
                    throw new RuntimeException("Test exception in filter");
                return true;
            }).count();
        });

        parallelStream = createDoubleStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.map(d -> {
                if (d == 7.0)
                    throw new RuntimeException("Test exception in map");
                return d;
            }).toArray();
        });
    }

    @Test
    @DisplayName("Test with large arrays")
    public void testLargeArrays() {
        double[] largeArray = new double[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i + 1.0;
        }

        parallelStream = createDoubleStream(largeArray);

        double sum = parallelStream.reduce(0.0, Double::sum);
        assertEquals(500500.0, sum);

        parallelStream = createDoubleStream(largeArray);
        long evenCount = parallelStream.filter(d -> d % 2 == 0).count();
        assertEquals(500, evenCount);
    }

    @Test
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(d -> d > 5).count();
        });
    }

    // Covers delayed-match ordering for iterator-backed terminal operations.
    @Test
    public void testReduceAndFindOperations_DelayedMatchOrdering() {
        assertEquals(72.0, createDoubleStream(21.0, 2.0, 4.0, 7.0, 6.0, 11.0, 8.0, 13.0).reduce(0.0, Double::sum));

        OptionalDouble reduced = createDoubleStream(21.0, 2.0, 4.0).reduce(Double::sum);
        assertTrue(reduced.isPresent());
        assertEquals(27.0, reduced.get(), 0.0001);

        OptionalDouble firstMatch = createDoubleStream(21.0, 2.0, 4.0, 7.0, 6.0, 11.0, 8.0, 13.0).findFirst(d -> {
            if (d == 21.0) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return d > 5.0 && ((int) d) % 2 == 1;
        });
        assertTrue(firstMatch.isPresent());
        assertEquals(21.0, firstMatch.get(), 0.0001);

        OptionalDouble anyMatch = createDoubleStream(21.0, 2.0, 4.0, 7.0, 6.0, 11.0, 8.0, 13.0).findAny(d -> d > 5.0 && ((int) d) % 2 == 1);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21.0 || anyMatch.get() == 7.0 || anyMatch.get() == 11.0 || anyMatch.get() == 13.0);

        OptionalDouble lastMatch = createDoubleStream(21.0, 2.0, 4.0, 7.0, 6.0, 11.0, 8.0, 13.0).findLast(d -> {
            if (d == 13.0) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return d > 5.0 && ((int) d) % 2 == 1;
        });
        assertTrue(lastMatch.isPresent());
        assertEquals(13.0, lastMatch.get(), 0.0001);
    }
}
