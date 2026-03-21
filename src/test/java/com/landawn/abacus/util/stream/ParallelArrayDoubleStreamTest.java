package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleBinaryOperator;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoubleFunction;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.DoubleTernaryOperator;
import com.landawn.abacus.util.function.DoubleToFloatFunction;
import com.landawn.abacus.util.function.DoubleToIntFunction;
import com.landawn.abacus.util.function.DoubleToLongFunction;
import com.landawn.abacus.util.function.DoubleUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelArrayDoubleStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final double[] TEST_ARRAY = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0,
            21.0, 22.0, 23.0, 24.0, 25.0, 26.0 };
    private static final double[] DEFAULT_ARRAY = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };

    private DoubleStream parallelStream;

    protected DoubleStream createDoubleStream(double... elements) {
        return DoubleStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    protected DoubleStream createIteratorSplitorDoubleStream(double... elements) {
        return new ParallelArrayDoubleStream(elements, 0, elements.length, false, testMaxThreadNum, Splitor.ITERATOR, null, false, new ArrayList<>());
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createDoubleStream(DEFAULT_ARRAY);
    }

    // Helper: creates a ParallelArrayDoubleStream with a single element so canBeSequential() returns true.
    protected DoubleStream createSingleElementDoubleStream(double value) {
        return new ParallelArrayDoubleStream(new double[] { value }, 0, 1, false, testMaxThreadNum, Splitor.ARRAY, null, false, new ArrayList<>());
    }

    // Covers the iterator-based terminal-operation branch in ParallelArrayDoubleStream.
    @Test
    public void testReduceAndFindMethods_IteratorSplitor() {
        assertEquals(15.0, createIteratorSplitorDoubleStream(4.0, 2.0, 1.0, 3.0, 5.0).reduce(0.0, Double::sum), 0.0001);

        OptionalDouble reduced = createIteratorSplitorDoubleStream(4.0, 2.0, 1.0, 3.0, 5.0).reduce(Double::sum);
        assertTrue(reduced.isPresent());
        assertEquals(15.0, reduced.get(), 0.0001);

        OptionalDouble firstOdd = createIteratorSplitorDoubleStream(4.0, 2.0, 1.0, 3.0, 5.0).findFirst(d -> d == 1.0 || d == 3.0 || d == 5.0);
        assertTrue(firstOdd.isPresent());
        assertEquals(1.0, firstOdd.get(), 0.0001);

        OptionalDouble anyOdd = createIteratorSplitorDoubleStream(4.0, 2.0, 1.0, 3.0, 5.0).findAny(d -> d == 1.0 || d == 3.0 || d == 5.0);
        assertTrue(anyOdd.isPresent());
        assertTrue(anyOdd.get() == 1.0 || anyOdd.get() == 3.0 || anyOdd.get() == 5.0);

        OptionalDouble lastOdd = createIteratorSplitorDoubleStream(4.0, 2.0, 1.0, 3.0, 5.0).findLast(d -> d == 1.0 || d == 3.0 || d == 5.0);
        assertTrue(lastOdd.isPresent());
        assertEquals(5.0, lastOdd.get(), 0.0001);

        OptionalDouble notFound = createIteratorSplitorDoubleStream(4.0, 2.0, 1.0, 3.0, 5.0).findAny(d -> d > 10.0);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFilter() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        DoublePredicate predicate = d -> d > 13.0;
        List<Double> result = stream.filter(predicate).toList();
        assertEquals(13, result.size());
        assertTrue(result.contains(14.0));
        assertFalse(result.contains(1.0));
    }

    @Test
    @DisplayName("Test complex parallel operations")
    public void testComplexParallelOperations() {
        double[] result = parallelStream.filter(d -> d % 2 == 0).map(d -> d * 2).sorted().toArray();

        assertArrayEquals(new double[] { 4.0, 8.0, 12.0, 16.0, 20.0 }, result);

        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 });
        double sum = parallelStream.map(d -> d * 2).reduce(0.0, Double::sum);

        assertEquals(110.0, sum);
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
    @DisplayName("Test special double values")
    public void testSpecialDoubleValues() {
        parallelStream = createDoubleStream(new double[] { Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 0.0, -0.0 });

        long finiteCount = parallelStream.filter(Double::isFinite).count();
        assertEquals(5, finiteCount);

        parallelStream = createDoubleStream(new double[] { Double.NaN, 1.0, 2.0 });
        OptionalDouble max = parallelStream.reduce(Double::max);
        assertTrue(max.isPresent());
        assertTrue(Double.isNaN(max.getAsDouble()));
    }

    // Sequential-fallback branch tests (maxThreadNum > 1 but array has <= 1 element)

    @Test
    public void testFilter_SequentialFallback() {
        List<Double> result = createSingleElementDoubleStream(5.0).filter(d -> d > 3.0).toList();
        assertEquals(1, result.size());
        assertEquals(5.0, result.get(0), 0.0001);

        List<Double> empty = createSingleElementDoubleStream(1.0).filter(d -> d > 3.0).toList();
        assertEquals(0, empty.size());
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
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(d -> d > 5).count();
        });
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
    public void testTakeWhile_SequentialFallback() {
        List<Double> result = createSingleElementDoubleStream(2.0).takeWhile(d -> d < 5.0).toList();
        assertEquals(1, result.size());
        assertEquals(2.0, result.get(0), 0.0001);
    }

    @Test
    public void testDropWhile() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        DoublePredicate predicate = d -> d < 4.0;
        List<Double> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains(4.0));
        assertFalse(result.contains(3.0));
    }

    @Test
    public void testDropWhile_SequentialFallback() {
        List<Double> result = createSingleElementDoubleStream(2.0).dropWhile(d -> d < 5.0).toList();
        assertEquals(0, result.size());

        List<Double> kept = createSingleElementDoubleStream(10.0).dropWhile(d -> d < 5.0).toList();
        assertEquals(1, kept.size());
        assertEquals(10.0, kept.get(0), 0.0001);
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
    public void testMap_SequentialFallback() {
        List<Double> result = createSingleElementDoubleStream(3.0).map(d -> d * 2.0).toList();
        assertEquals(1, result.size());
        assertEquals(6.0, result.get(0), 0.0001);
    }

    @Test
    public void testMapToInt() {
        DoubleStream stream = createDoubleStream(new double[] { 10.1, 20.9, 30.0 });
        DoubleToIntFunction mapper = d -> (int) d;
        List<Integer> result = stream.mapToInt(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));
        assertTrue(result.contains(30));
    }

    @Test
    public void testMapToInt_SequentialFallback() {
        List<Integer> result = createSingleElementDoubleStream(7.9).mapToInt(d -> (int) d).toList();
        assertEquals(1, result.size());
        assertEquals(7, result.get(0));
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
    public void testMapToLong_empty() {
        parallelStream = createDoubleStream(new double[] {});
        long[] result = parallelStream.mapToLong(d -> (long) d).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToLong_SequentialFallback() {
        List<Long> result = createSingleElementDoubleStream(4.0).mapToLong(d -> (long) d).toList();
        assertEquals(1, result.size());
        assertEquals(4L, result.get(0));
    }

    @Test
    public void testMapToFloat() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleToFloatFunction mapper = d -> (float) d * 2.0f;
        List<Float> result = stream.mapToFloat(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(4.0f));
        assertTrue(result.contains(6.0f));
    }

    @Test
    public void testMapToFloat_empty() {
        parallelStream = createDoubleStream(new double[] {});
        float[] result = parallelStream.mapToFloat(d -> (float) d).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToFloat_SequentialFallback() {
        List<Float> result = createSingleElementDoubleStream(3.0).mapToFloat(d -> (float) (d * 2.0)).toList();
        assertEquals(1, result.size());
        assertEquals(6.0f, result.get(0), 0.0001f);
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
    public void testMapToObj_SequentialFallback() {
        List<String> result = createSingleElementDoubleStream(9.0).mapToObj(d -> "v" + d).toList();
        assertEquals(1, result.size());
        assertEquals("v9.0", result.get(0));
    }

    @Test
    public void testFlatMapDoubleStream() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleFunction<DoubleStream> mapper = d -> DoubleStream.of(d, d + 0.5);
        List<Double> result = stream.flatMap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
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
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        DoubleStream flattened = parallelStream.flatmap(d -> new double[] { d, d * 2 });
        double[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        flattened = parallelStream.flatmap(d -> d % 2 == 0 ? new double[] { d } : new double[0]);
        assertArrayEquals(new double[] { 2.0 }, flattened.toArray());
    }

    // Tests covering parallel code paths with large array

    @Test
    public void testFlatMap_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).flatMap(d -> DoubleStream.of(d, d * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmap_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).flatmap(d -> new double[] { d, d * 10 }).count();
        assertEquals(52, count);
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
    public void testFlatMap_SequentialFallback() {
        List<Double> result = createSingleElementDoubleStream(2.0).flatMap(d -> DoubleStream.of(d, d + 1.0)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(3.0));
    }

    @Test
    public void testFlatmapArray_SequentialFallback() {
        List<Double> result = createSingleElementDoubleStream(2.0).flatmap(d -> new double[] { d, d * 2.0 }).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(4.0));
    }

    @Test
    public void testFlatMapToInt() {
        DoubleStream stream = createDoubleStream(new double[] { 1.5, 2.8 });
        DoubleFunction<IntStream> mapper = d -> IntStream.of((int) d, (int) (d * 2));
        List<Integer> result = stream.flatMapToInt(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(3));
        assertTrue(result.contains(2));
        assertTrue(result.contains(5));
    }

    @Test
    public void testFlatMapToInt_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).flatMapToInt(d -> IntStream.of((int) d)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToInt_SequentialFallback() {
        List<Integer> result = createSingleElementDoubleStream(3.0).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(3));
        assertTrue(result.contains(4));
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
    public void testFlatMapToLong_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).flatMapToLong(d -> LongStream.of((long) d)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToLong_SequentialFallback() {
        List<Long> result = createSingleElementDoubleStream(5.0).flatMapToLong(d -> LongStream.of((long) d, (long) d * 2)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(5L));
        assertTrue(result.contains(10L));
    }

    @Test
    public void testFlatMapToFloat() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleFunction<FloatStream> mapper = d -> FloatStream.of((float) d, (float) (d + 0.5));
        List<Float> result = stream.flatMapToFloat(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    public void testFlatMapToFloat_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).flatMapToFloat(d -> FloatStream.of((float) d)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToFloat_SequentialFallback() {
        List<Float> result = createSingleElementDoubleStream(4.0).flatMapToFloat(d -> FloatStream.of((float) d, (float) (d * 1.5))).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(4.0f));
        assertTrue(result.contains(6.0f));
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
    public void testFlatMapToObjCollection() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleFunction<Collection<String>> mapper = d -> {
            List<String> list = new ArrayList<>();
            list.add("X" + d);
            list.add("Y" + d);
            return list;
        };
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("X1.0"));
        assertTrue(result.contains("Y1.0"));
        assertTrue(result.contains("X2.0"));
        assertTrue(result.contains("Y2.0"));
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
    @DisplayName("Test flatmapToObj with collection method")
    public void testFlatmapToObjCollection() {
        Stream<Double> flattened = parallelStream.flatmapToObj(d -> Arrays.asList(Double.valueOf(d), Double.valueOf(d * 10)));
        List<Double> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertEquals(Double.valueOf(1.0), result.get(0));
        assertEquals(Double.valueOf(100.0), result.get(19));
    }

    @Test
    public void testFlatMapToObj_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).flatMapToObj(d -> Stream.of(d, d * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmapToObj_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).flatmapToObj(d -> Arrays.asList(d, d * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatMapToObj_SequentialFallback() {
        List<String> result = createSingleElementDoubleStream(1.0).flatMapToObj(d -> Stream.of("A" + d, "B" + d)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B1.0"));
    }

    @Test
    public void testFlatmapToObj_SequentialFallback() {
        List<String> result = createSingleElementDoubleStream(2.0).flatmapToObj(d -> java.util.Arrays.asList("X" + d, "Y" + d)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("X2.0"));
        assertTrue(result.contains("Y2.0"));
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
    public void testOnEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleElementDoubleStream(7.0).peek(d -> count.incrementAndGet()).forEach(d -> {
        });
        assertEquals(1, count.get());
    }

    @Test
    public void testForEach_IteratorSplitor() {
        AtomicInteger count = new AtomicInteger(0);
        createIteratorSplitorDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0).forEach(d -> count.incrementAndGet());
        assertEquals(5, count.get());
    }

    @Test
    public void testForEach_ParallelPath() {
        AtomicInteger count = new AtomicInteger(0);
        createDoubleStream(TEST_ARRAY).forEach(d -> count.incrementAndGet());
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testForEach() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        List<Double> consumed = new ArrayList<>();
        DoubleConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.forEach(action);

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleElementDoubleStream(3.0).forEach(d -> count.incrementAndGet());
        assertEquals(1, count.get());
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
    @DisplayName("Test thread safety")
    public void testThreadSafety() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        parallelStream.forEach(d -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertEquals(10, counter.get());
    }

    @Test
    public void testToMap() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        Map<String, String> result = stream.toMap(d -> String.valueOf(d), d -> "Value_" + d, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1.0", result.get("1.0"));
        assertEquals("Value_2.0", result.get("2.0"));
        assertEquals("Value_3.0", result.get("3.0"));
    }

    @Test
    public void testToMap_ParallelPath() {
        Map<Double, Double> result = createDoubleStream(TEST_ARRAY).toMap(d -> d, d -> d * 2, (a, b) -> a, java.util.HashMap::new);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testToMap_SequentialFallback() {
        Map<String, Double> result = createSingleElementDoubleStream(5.0).toMap(d -> "k" + d, d -> d, (a, b) -> a, java.util.HashMap::new);
        assertEquals(1, result.size());
        assertEquals(5.0, result.get("k5.0"), 0.0001);
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
    public void testGroupTo_ParallelPath() {
        Map<Boolean, List<Double>> result = createDoubleStream(TEST_ARRAY).groupTo(d -> d < 14.0, Collectors.toList(), java.util.HashMap::new);
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupTo_SequentialFallback() {
        Map<Boolean, List<Double>> result = createSingleElementDoubleStream(4.0).groupTo(d -> d > 3.0, java.util.stream.Collectors.toList(),
                java.util.HashMap::new);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(true));
        assertEquals(1, result.get(true).size());
    }

    @Test
    @DisplayName("Test precision in calculations")
    public void testDoublePrecision() {
        parallelStream = createDoubleStream(new double[] { 0.1, 0.2, 0.3 });
        double sum = parallelStream.reduce(0.0, Double::sum);

        assertEquals(0.6, sum, 0.0000001);

        parallelStream = createDoubleStream(new double[] { 1e-15, 1e-15, 1e-15 });
        double smallSum = parallelStream.reduce(0.0, Double::sum);
        assertEquals(3e-15, smallSum, 1e-20);
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
    public void testReduce_WithIdentity_ParallelPath() {
        double result = createDoubleStream(TEST_ARRAY).reduce(0.0, Double::sum);
        double expected = 0;
        for (double v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, result, 0.001);
    }

    @Test
    public void testReduce_NoIdentity_ParallelPath() {
        OptionalDouble result = createDoubleStream(TEST_ARRAY).reduce(Double::sum);
        assertTrue(result.isPresent());
    }

    @Test
    public void testReduceWithIdentity() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleBinaryOperator op = (d1, d2) -> d1 + d2;
        double result = stream.reduce(0.0, op);
        assertEquals(6.0, result, 0.0001);

        DoubleStream emptyStream = createDoubleStream(new double[] {});
        double emptyResult = emptyStream.reduce(100.0, op);
        assertEquals(100.0, emptyResult, 0.0001);
    }

    @Test
    public void testReduceWithoutIdentity() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleBinaryOperator accumulator = (d1, d2) -> d1 + d2;
        OptionalDouble result = stream.reduce(accumulator);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.get(), 0.0001);

        DoubleStream emptyStream = createDoubleStream(new double[] {});
        OptionalDouble emptyResult = emptyStream.reduce(accumulator);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testOverflowScenarios() {
        parallelStream = createDoubleStream(new double[] { Double.MAX_VALUE, Double.MAX_VALUE });
        double sum = parallelStream.reduce(0.0, Double::sum);
        assertTrue(Double.isInfinite(sum));
    }

    @Test
    public void testReduce_WithIdentity_SequentialFallback() {
        double result = createSingleElementDoubleStream(7.0).reduce(0.0, Double::sum);
        assertEquals(7.0, result, 0.0001);
    }

    @Test
    public void testReduce_NoIdentity_SequentialFallback() {
        OptionalDouble result = createSingleElementDoubleStream(9.0).reduce(Double::sum);
        assertTrue(result.isPresent());
        assertEquals(9.0, result.get(), 0.0001);
    }

    @Test
    public void testIteratorSplitorReduceAndFindOperations_SparseMatch() {
        assertEquals(72.0, createIteratorSplitorDoubleStream(21.0, 2.0, 4.0, 7.0, 6.0, 11.0, 8.0, 13.0).reduce(0.0, Double::sum), 0.0001);

        OptionalDouble reduced = createIteratorSplitorDoubleStream(21.0, 2.0, 4.0).reduce(Double::sum);
        assertTrue(reduced.isPresent());
        assertEquals(27.0, reduced.get(), 0.0001);

        OptionalDouble firstMatch = createIteratorSplitorDoubleStream(21.0, 2.0, 4.0, 7.0, 6.0, 11.0, 8.0, 13.0).findFirst(d -> {
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

        OptionalDouble anyMatch = createIteratorSplitorDoubleStream(21.0, 2.0, 4.0, 7.0, 6.0, 11.0, 8.0, 13.0).findAny(d -> d > 5.0 && ((int) d) % 2 == 1);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21.0 || anyMatch.get() == 7.0 || anyMatch.get() == 11.0 || anyMatch.get() == 13.0);

        OptionalDouble lastMatch = createIteratorSplitorDoubleStream(21.0, 2.0, 4.0, 7.0, 6.0, 11.0, 8.0, 13.0).findLast(d -> {
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

    @Test
    public void testCollect_ParallelPath() {
        List<Double> result = createDoubleStream(TEST_ARRAY).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testCollect() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        List<Double> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1.0, 2.0, 3.0), collectedList);
    }

    @Test
    public void testCollect_IteratorSplitor() {
        List<Double> result = createIteratorSplitorDoubleStream(1.0, 2.0, 3.0).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testCollect_SequentialFallback() {
        List<Double> result = createSingleElementDoubleStream(3.0).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(1, result.size());
        assertEquals(3.0, result.get(0), 0.0001);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).anyMatch(d -> d == 13.0));
        assertFalse(createDoubleStream(TEST_ARRAY).anyMatch(d -> d == 100.0));
        assertFalse(createDoubleStream(new double[] {}).anyMatch(d -> true));
    }

    @Test
    public void testAnyMatch_SequentialFallback() {
        assertTrue(createSingleElementDoubleStream(5.0).anyMatch(d -> d == 5.0));
        assertFalse(createSingleElementDoubleStream(5.0).anyMatch(d -> d == 6.0));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).allMatch(d -> d >= 1.0 && d <= 26.0));
        assertFalse(createDoubleStream(TEST_ARRAY).allMatch(d -> d < 26.0));
        assertTrue(createDoubleStream(new double[] {}).allMatch(d -> false));
    }

    @Test
    public void testAllMatch_SequentialFallback() {
        assertTrue(createSingleElementDoubleStream(5.0).allMatch(d -> d > 0.0));
        assertFalse(createSingleElementDoubleStream(5.0).allMatch(d -> d > 10.0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).noneMatch(d -> d == 100.0));
        assertFalse(createDoubleStream(TEST_ARRAY).noneMatch(d -> d == 1.0));
        assertTrue(createDoubleStream(new double[] {}).noneMatch(d -> true));
    }

    // Cover the ITERATOR splitor path for anyMatch / allMatch / noneMatch / collect / forEach
    @Test
    public void testAnyMatchAllMatchNoneMatch_IteratorSplitor() {
        assertTrue(createIteratorSplitorDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0).anyMatch(d -> d > 4.0));
        assertFalse(createIteratorSplitorDoubleStream(1.0, 2.0, 3.0).anyMatch(d -> d > 10.0));

        assertTrue(createIteratorSplitorDoubleStream(1.0, 2.0, 3.0).allMatch(d -> d >= 1.0 && d <= 3.0));
        assertFalse(createIteratorSplitorDoubleStream(1.0, 2.0, 3.0).allMatch(d -> d < 3.0));

        assertTrue(createIteratorSplitorDoubleStream(1.0, 2.0, 3.0).noneMatch(d -> d > 10.0));
        assertFalse(createIteratorSplitorDoubleStream(1.0, 2.0, 3.0).noneMatch(d -> d > 2.0));
    }

    @Test
    public void testNoneMatch_SequentialFallback() {
        assertTrue(createSingleElementDoubleStream(5.0).noneMatch(d -> d > 10.0));
        assertFalse(createSingleElementDoubleStream(5.0).noneMatch(d -> d > 0.0));
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
    public void testFindFirst_SequentialFallback() {
        OptionalDouble found = createSingleElementDoubleStream(5.0).findFirst(d -> d > 0.0);
        assertTrue(found.isPresent());
        assertEquals(5.0, found.get(), 0.0001);

        OptionalDouble notFound = createSingleElementDoubleStream(5.0).findFirst(d -> d > 10.0);
        assertFalse(notFound.isPresent());
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
    public void testFindAny() {
        DoubleStream stream = createDoubleStream(new double[] { 4.0, 2.0, 1.0, 3.0, 1.0 });
        OptionalDouble result = stream.findAny(d -> d == 1.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get(), 0.0001);

        stream = createDoubleStream(new double[] { 4.0, 2.0, 1.0, 3.0, 1.0 });
        OptionalDouble notFound = stream.findAny(d -> d == 10.0);
        assertFalse(notFound.isPresent());

        OptionalDouble empty = createDoubleStream(new double[] {}).findAny(d -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindAny_SequentialFallback() {
        OptionalDouble found = createSingleElementDoubleStream(3.0).findAny(d -> d < 10.0);
        assertTrue(found.isPresent());
        assertEquals(3.0, found.get(), 0.0001);
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
    public void testFindLast_SequentialFallback() {
        OptionalDouble found = createSingleElementDoubleStream(8.0).findLast(d -> d > 0.0);
        assertTrue(found.isPresent());
        assertEquals(8.0, found.get(), 0.0001);
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
    public void testZipWithBinaryOperator() {
        DoubleStream streamA = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0, 30.0);
        DoubleBinaryOperator zipper = (d1, d2) -> d1 + d2;
        List<Double> result = streamA.zipWith(streamB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11.0, result.get(0), 0.0001);
        assertEquals(22.0, result.get(1), 0.0001);
        assertEquals(33.0, result.get(2), 0.0001);
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
    @DisplayName("Test zipWith two streams with default values")
    public void testZipWithBinaryDefaults() {
        DoubleStream stream2 = createDoubleStream(new double[] { 10.0, 20.0, 30.0 });
        double[] result = parallelStream.zipWith(stream2, 0.0, 100.0, (a, b) -> a + b).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(11.0, result[0]);
        assertEquals(22.0, result[1]);
        assertEquals(33.0, result[2]);
        assertEquals(104.0, result[3]);
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
    public void testZipWithBinary_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).zipWith(DoubleStream.of(TEST_ARRAY), (a, b) -> a + b).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernary_ParallelPath() {
        long count = createDoubleStream(TEST_ARRAY).zipWith(DoubleStream.of(TEST_ARRAY), DoubleStream.of(TEST_ARRAY), (a, b, c) -> a + b + c).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryDefaults_ParallelPath() {
        double[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createDoubleStream(TEST_ARRAY).zipWith(DoubleStream.of(shorter), 0.0, -1.0, (a, b) -> a + b).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernaryDefaults_ParallelPath() {
        double[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createDoubleStream(TEST_ARRAY).zipWith(DoubleStream.of(shorter), DoubleStream.of(shorter), 0.0, -1.0, -2.0, (a, b, c) -> a + b + c)
                .count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        DoubleStream streamA = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0, 30.0);
        double valA = 0.0;
        double valB = -1.0;
        DoubleBinaryOperator zipper = (d1, d2) -> {
            if (d1 == valA)
                return d2;
            if (d2 == valB)
                return d1;
            return d1 + d2;
        };
        List<Double> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11.0, result.get(0), 0.0001);
        assertEquals(22.0, result.get(1), 0.0001);
        assertEquals(30.0, result.get(2), 0.0001);
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
    @DisplayName("Test zipWith two streams with binary operator")
    public void testZipWithBinary() {
        DoubleStream stream2 = createDoubleStream(new double[] { 10.0, 20.0, 30.0, 40.0, 50.0 });
        DoubleStream zipped = parallelStream.zipWith(stream2, (a, b) -> a + b);
        double[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new double[] { 11.0, 22.0, 33.0, 44.0, 55.0 }, result);

        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        stream2 = createDoubleStream(new double[] { 10.0, 20.0, 30.0, 40.0, 50.0 });
        zipped = parallelStream.zipWith(stream2, (a, b) -> a * b);
        assertEquals(3, zipped.count());
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
    public void testZipWith_SequentialFallback() {
        DoubleStream streamA = createSingleElementDoubleStream(3.0);
        DoubleStream streamB = DoubleStream.of(7.0);
        List<Double> result = streamA.zipWith(streamB, (a, b) -> a + b).toList();
        assertEquals(1, result.size());
        assertEquals(10.0, result.get(0), 0.0001);
    }

    @Test
    public void testZipWithTernary_SequentialFallback() {
        DoubleStream streamA = createSingleElementDoubleStream(1.0);
        DoubleStream streamB = DoubleStream.of(2.0);
        DoubleStream streamC = DoubleStream.of(3.0);
        List<Double> result = streamA.zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList();
        assertEquals(1, result.size());
        assertEquals(6.0, result.get(0), 0.0001);
    }

    @Test
    public void testZipWithDefaults_SequentialFallback() {
        DoubleStream streamA = createSingleElementDoubleStream(5.0);
        DoubleStream streamB = DoubleStream.of(10.0, 20.0);
        List<Double> result = streamA.zipWith(streamB, 0.0, 0.0, (a, b) -> a + b).sorted().toList();
        assertEquals(2, result.size());
        assertEquals(15.0, result.get(0), 0.0001);
        assertEquals(20.0, result.get(1), 0.0001);
    }

    @Test
    public void testZipWithTernaryDefaults_SequentialFallback() {
        DoubleStream streamA = createSingleElementDoubleStream(1.0);
        DoubleStream streamB = DoubleStream.of(2.0, 3.0);
        DoubleStream streamC = DoubleStream.of(4.0, 5.0, 6.0);
        List<Double> result = streamA.zipWith(streamB, streamC, 0.0, 0.0, 0.0, (a, b, c) -> a + b + c).sorted().toList();
        // sorted ascending: [6.0, 7.0, 8.0]
        assertEquals(3, result.size());
        assertEquals(6.0, result.get(0), 0.0001);
    }

    @Test
    public void testZipWithBinaryDefaults_SequentialFallback_UnevenLengths() {
        List<Double> result = DoubleStream.of(1D, 2D, 3D)
                .parallel(PS.create(Splitor.ARRAY).maxThreadNum(1))
                .zipWith(DoubleStream.of(10D), 0D, -1D, Double::sum)
                .toList();

        assertEquals(3, result.size());
        assertEquals(11D, result.get(0), 0.0001D);
        assertEquals(1D, result.get(1), 0.0001D);
        assertEquals(2D, result.get(2), 0.0001D);
    }

    @Test
    public void testIsParallel() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
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
    public void testSequential_ParallelPath() {
        DoubleStream parallel = createDoubleStream(TEST_ARRAY);
        assertTrue(parallel.isParallel());
        DoubleStream seq = parallel.sequential();
        assertFalse(seq.isParallel());
        assertEquals(TEST_ARRAY.length, seq.count());
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelArrayDoubleStream) createDoubleStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ARRAY, ((ParallelArrayDoubleStream) createDoubleStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelArrayDoubleStream) createDoubleStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    @DisplayName("Test method chaining")
    public void testMethodChaining() {
        List<String> result = parallelStream.filter(d -> d > 3)
                .map(d -> d * 2)
                .flatMapToObj(d -> Stream.of("Value: " + d, "Half: " + (d / 2)))
                .sorted()
                .toList();

        assertTrue(result.size() > 0);
        assertTrue(result.contains("Value: 8.0"));
        assertTrue(result.contains("Half: 4.0"));
    }

    @Test
    public void testOnClose() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        DoubleStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get());
        newStream.close();
        assertTrue(closedFlag.get());
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
    public void testOnCloseEmptyHandler() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        DoubleStream newStream = stream.onClose(null);
        assertSame(stream, newStream);
        newStream.close();
    }

    @Test
    public void testBoundaryValues() {
        double[] boundaryArray = new double[] { Double.MIN_VALUE, -Double.MAX_VALUE, -1.0, 0.0, 1.0, Double.MAX_VALUE };
        parallelStream = createDoubleStream(boundaryArray);
        double[] result = parallelStream.sorted().toArray();
        assertEquals(6, result.length);
        assertEquals(-Double.MAX_VALUE, result[0], 0.0001);
        assertEquals(Double.MAX_VALUE, result[5], 0.0001);
    }
}
