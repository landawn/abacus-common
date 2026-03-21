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

    // Single-thread parallel (sequential fallback) tests to cover the canBeSequential branches.
    protected DoubleStream createSingleThreadDoubleStream(double... elements) {
        return DoubleStream.of(elements).map(e -> (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1));
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
    public void testFilter_singleThread() {
        List<Double> result = createSingleThreadDoubleStream(TEST_ARRAY).filter(d -> d > 20.0).toList();
        assertEquals(6, result.size());
        for (Double d : result) {
            assertTrue(d > 20.0);
        }
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
    public void testTakeWhile_singleThread() {
        List<Double> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }).takeWhile(d -> d < 4.0).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(3.0));
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
    public void testDropWhile_singleThread() {
        List<Double> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }).dropWhile(d -> d < 4.0).toList();
        assertHaveSameElements(Arrays.asList(4.0, 5.0), result);
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
    public void testMap_singleThread() {
        List<Double> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).map(d -> d * 2.0).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(4.0));
        assertTrue(result.contains(6.0));
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
    public void testMapToInt_singleThread() {
        List<Integer> result = createSingleThreadDoubleStream(new double[] { 1.5, 2.5, 3.5 }).mapToInt(d -> (int) d).toList();
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
    public void testMapToLong_singleThread() {
        List<Long> result = createSingleThreadDoubleStream(new double[] { 10.1, 20.9, 30.0 }).mapToLong(d -> (long) d).toList();
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
    public void testMapToFloat_singleThread() {
        List<Float> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).mapToFloat(d -> (float) d).toList();
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
    public void testMapToObj_singleThread() {
        List<String> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).mapToObj(d -> "val_" + d).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("val_1.0"));
        assertTrue(result.contains("val_2.0"));
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
    public void testFlatMap_largeArray() {
        List<Double> result = createDoubleStream(TEST_ARRAY).flatMap(d -> DoubleStream.of(d, d * 2)).sorted().boxed().toList();
        assertEquals(52, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(52.0));
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
    public void testFlatMap_singleThread() {
        List<Double> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).flatMap(d -> DoubleStream.of(d, d + 10.0)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(11.0));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(12.0));
    }

    @Test
    public void testFlatmap_singleThread() {
        List<Double> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).flatmap(d -> new double[] { d, d + 0.5 }).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
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
    public void testFlatMapToInt_singleThread() {
        List<Integer> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).flatMapToInt(d -> IntStream.of((int) d, (int) (d * 10))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(10));
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
    public void testFlatMapToLong_singleThread() {
        List<Long> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).flatMapToLong(d -> LongStream.of((long) d, (long) (d * 10))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(10L));
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
    public void testFlatMapToFloat_singleThread() {
        List<Float> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).flatMapToFloat(d -> FloatStream.of((float) d, (float) (d + 0.5)))
                .toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
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
    @DisplayName("Test flatMapToObj method")
    public void testFlatMapToObj() {
        Stream<String> flattened = parallelStream.flatMapToObj(d -> Stream.of("A" + d, "B" + d));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B10.0"));
    }

    @Test
    public void testFlatMapToObj_largeArray() {
        List<String> result = createDoubleStream(TEST_ARRAY).flatMapToObj(d -> Stream.of(new String[] { "v" + d })).toList();
        assertEquals(26, result.size());
    }

    @Test
    public void testFlatmapToObj_largeArray() {
        List<String> result = createDoubleStream(TEST_ARRAY).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toList();
        assertEquals(52, result.size());
    }

    @Test
    public void testFlatMapToObj_singleThread() {
        List<String> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).flatMapToObj(d -> Stream.of("A" + d, "B" + d)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B2.0"));
    }

    @Test
    public void testFlatmapToObj_singleThread() {
        List<String> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).flatmapToObj(d -> Arrays.asList("X" + d, "Y" + d)).toList();
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
    public void testOnEach_singleThread() {
        List<Double> consumed = new ArrayList<>();
        createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).onEach(d -> {
            synchronized (consumed) {
                consumed.add(d);
            }
        }).forEach(d -> {
        });
        assertEquals(3, consumed.size());
    }

    @Test
    public void testForEach_singleThread() {
        List<Double> collected = new ArrayList<>();
        createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).forEach(d -> collected.add(d));
        assertHaveSameElements(Arrays.asList(1.0, 2.0, 3.0), collected);
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
    public void testToMap_singleThread() {
        Map<String, Double> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).toMap(d -> "Key_" + d, d -> d, (v1, v2) -> v1 + v2,
                java.util.HashMap::new);
        assertEquals(3, result.size());
        assertEquals(1.0, result.get("Key_1.0"), 0.0001);
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
    public void testGroupTo_singleThread() {
        Map<String, List<Double>> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 1.0 }).groupTo(d -> String.valueOf(d),
                java.util.stream.Collectors.toList(), java.util.HashMap::new);
        assertEquals(2, result.size());
        assertEquals(2, result.get("1.0").size());
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
    public void testReduce_parallelWithLargeArray() {
        double sum = createDoubleStream(TEST_ARRAY).reduce(0.0, Double::sum);
        assertEquals(351.0, sum, 0.01);

        OptionalDouble opt = createDoubleStream(TEST_ARRAY).reduce(Double::sum);
        assertTrue(opt.isPresent());
        assertEquals(351.0, opt.get(), 0.01);
    }

    @Test
    public void testConstructor_withDefaultValues() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0)
                .parallel();
        double sum = stream.reduce(0.0, Double::sum);
        assertEquals(210.0, sum, 0.01);
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
    public void testReduce_emptyStreamParallel() {
        double result = createDoubleStream(new double[0]).reduce(0.0, Double::sum);
        assertEquals(0.0, result, 0.001);

        OptionalDouble opt = createDoubleStream(new double[0]).reduce(Double::sum);
        assertFalse(opt.isPresent());
    }

    @Test
    public void testReduceWithIdentity_singleThread() {
        double result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).reduce(0.0, (d1, d2) -> d1 + d2);
        assertEquals(6.0, result, 0.0001);
    }

    @Test
    public void testReduceWithoutIdentity_singleThread() {
        OptionalDouble result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).reduce((d1, d2) -> d1 + d2);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble(), 0.0001);
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

    @Test
    public void testCollect() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        List<Double> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1.0, 2.0, 3.0), collectedList);
    }

    @Test
    public void testCollect_singleThread() {
        List<Double> result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).anyMatch(d -> d == 26.0));
        assertFalse(createDoubleStream(TEST_ARRAY).anyMatch(d -> d == 100.0));
        assertFalse(createDoubleStream(new double[] {}).anyMatch(d -> true));
    }

    @Test
    public void testAnyMatch_singleThread() {
        assertTrue(createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).anyMatch(d -> d == 2.0));
        assertFalse(createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).anyMatch(d -> d == 99.0));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).allMatch(d -> d >= 1.0 && d <= 26.0));
        assertFalse(createDoubleStream(TEST_ARRAY).allMatch(d -> d < 26.0));
        assertTrue(createDoubleStream(new double[] {}).allMatch(d -> false));
    }

    @Test
    public void testAllMatch_singleThread() {
        assertTrue(createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).allMatch(d -> d > 0.0));
        assertFalse(createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).allMatch(d -> d > 1.0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).noneMatch(d -> d > 100.0));
        assertFalse(createDoubleStream(TEST_ARRAY).noneMatch(d -> d == 1.0));
        assertTrue(createDoubleStream(new double[] {}).noneMatch(d -> true));
    }

    @Test
    public void testNoneMatch_singleThread() {
        assertTrue(createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).noneMatch(d -> d > 100.0));
        assertFalse(createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).noneMatch(d -> d == 1.0));
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
    public void testFindFirst_singleThread() {
        OptionalDouble result = createSingleThreadDoubleStream(new double[] { 4.0, 2.0, 1.0, 3.0 }).findFirst(d -> d == 1.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.0001);

        OptionalDouble notFound = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).findFirst(d -> d == 99.0);
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
    public void testFindAny_singleThread() {
        OptionalDouble result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 }).findAny(d -> d == 2.0);
        assertTrue(result.isPresent());

        OptionalDouble notFound = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).findAny(d -> d == 99.0);
        assertFalse(notFound.isPresent());
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
    public void testFindLast_singleThread() {
        OptionalDouble result = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 1.0 }).findLast(d -> d == 1.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.0001);

        OptionalDouble notFound = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 }).findLast(d -> d == 99.0);
        assertFalse(notFound.isPresent());
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
    public void testZipWith_singleThread() {
        DoubleStream streamA = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0, 30.0);
        List<Double> result = streamA.zipWith(streamB, (a, b) -> a + b).toList();
        assertHaveSameElements(Arrays.asList(11.0, 22.0, 33.0), result);
    }

    @Test
    public void testZipWithTwoDefaultValues_singleThread() {
        DoubleStream streamA = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 });
        DoubleStream streamB = DoubleStream.of(10.0);
        List<Double> result = streamA.zipWith(streamB, 0.0, -1.0, (a, b) -> a + b).toList();
        assertHaveSameElements(Arrays.asList(11.0, 1.0), result);
    }

    @Test
    public void testZipWithThreeStreams_singleThread() {
        DoubleStream streamA = createSingleThreadDoubleStream(new double[] { 1.0, 2.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0);
        DoubleStream streamC = DoubleStream.of(100.0, 200.0);
        List<Double> result = streamA.zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList();
        assertHaveSameElements(Arrays.asList(111.0, 222.0), result);
    }

    @Test
    public void testZipWithThreeDefaultValues_singleThread() {
        DoubleStream streamA = createSingleThreadDoubleStream(new double[] { 1.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0);
        DoubleStream streamC = DoubleStream.of(100.0, 200.0, 300.0);
        List<Double> result = streamA.zipWith(streamB, streamC, 0.0, 0.0, 0.0, (a, b, c) -> a + b + c).sorted().toList();
        // sorted ascending: [111.0, 220.0, 300.0]
        assertEquals(3, result.size());
        assertEquals(111.0, result.get(0), 0.0001);
    }

    @Test
    public void testZipWithBinaryDefaults_singleThread_UnevenLengths() {
        List<Double> result = DoubleStream.of(1D, 2D, 3D)
                .map(e -> e + 0D)
                .parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1))
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
    public void testSequential_singleThread() {
        DoubleStream stream = createSingleThreadDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream seq = stream.sequential();
        assertFalse(seq.isParallel());
        List<Double> result = seq.toList();
        assertEquals(3, result.size());
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
}
