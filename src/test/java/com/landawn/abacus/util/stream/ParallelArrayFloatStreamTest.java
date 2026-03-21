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
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatTernaryOperator;
import com.landawn.abacus.util.function.FloatToDoubleFunction;
import com.landawn.abacus.util.function.FloatToIntFunction;
import com.landawn.abacus.util.function.FloatToLongFunction;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelArrayFloatStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final float[] TEST_ARRAY = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f,
            18.0f, 19.0f, 20.0f, 21.0f, 22.0f, 23.0f, 24.0f, 25.0f, 26.0f };
    private static final float[] DEFAULT_ARRAY = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f };

    private FloatStream parallelStream;

    protected FloatStream createFloatStream(float... elements) {
        return FloatStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    protected FloatStream createIteratorSplitorFloatStream(float... elements) {
        return new ParallelArrayFloatStream(elements, 0, elements.length, false, testMaxThreadNum, Splitor.ITERATOR, null, false, new ArrayList<>());
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createFloatStream(DEFAULT_ARRAY);
    }

    // Helper: creates a ParallelArrayFloatStream with a single element so canBeSequential() returns true.
    protected FloatStream createSingleElementFloatStream(float value) {
        return new ParallelArrayFloatStream(new float[] { value }, 0, 1, false, testMaxThreadNum, Splitor.ARRAY, null, false, new ArrayList<>());
    }

    // Covers the iterator-based terminal-operation branch in ParallelArrayFloatStream.
    @Test
    public void testReduceAndFindMethods_IteratorSplitor() {
        assertEquals(15.0f, createIteratorSplitorFloatStream(4.0f, 2.0f, 1.0f, 3.0f, 5.0f).reduce(0.0f, Float::sum), 0.0001f);

        OptionalFloat reduced = createIteratorSplitorFloatStream(4.0f, 2.0f, 1.0f, 3.0f, 5.0f).reduce(Float::sum);
        assertTrue(reduced.isPresent());
        assertEquals(15.0f, reduced.get(), 0.0001f);

        OptionalFloat firstOdd = createIteratorSplitorFloatStream(4.0f, 2.0f, 1.0f, 3.0f, 5.0f).findFirst(f -> f == 1.0f || f == 3.0f || f == 5.0f);
        assertTrue(firstOdd.isPresent());
        assertEquals(1.0f, firstOdd.get(), 0.0001f);

        OptionalFloat anyOdd = createIteratorSplitorFloatStream(4.0f, 2.0f, 1.0f, 3.0f, 5.0f).findAny(f -> f == 1.0f || f == 3.0f || f == 5.0f);
        assertTrue(anyOdd.isPresent());
        assertTrue(anyOdd.get() == 1.0f || anyOdd.get() == 3.0f || anyOdd.get() == 5.0f);

        OptionalFloat lastOdd = createIteratorSplitorFloatStream(4.0f, 2.0f, 1.0f, 3.0f, 5.0f).findLast(f -> f == 1.0f || f == 3.0f || f == 5.0f);
        assertTrue(lastOdd.isPresent());
        assertEquals(5.0f, lastOdd.get(), 0.0001f);

        OptionalFloat notFound = createIteratorSplitorFloatStream(4.0f, 2.0f, 1.0f, 3.0f, 5.0f).findAny(f -> f > 10.0f);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFilter() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        FloatPredicate predicate = f -> f > 13.0f;
        List<Float> result = stream.filter(predicate).toList();
        assertEquals(13, result.size());
        assertTrue(result.contains(14.0f));
        assertFalse(result.contains(1.0f));
    }

    @Test
    @DisplayName("Test complex parallel operations")
    public void testComplexParallelOperations() {
        float[] result = parallelStream.filter(f -> f % 2 == 0).map(f -> f * 2).sorted().toArray();

        assertArrayEquals(new float[] { 4.0f, 8.0f, 12.0f, 16.0f, 20.0f }, result);

        parallelStream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f });
        double sum = parallelStream.mapToDouble(f -> f).reduce(0.0, Double::sum);

        assertEquals(55.0, sum);
    }

    @Test
    @DisplayName("Test edge cases")
    public void testEdgeCases() {
        parallelStream = createFloatStream(new float[0]);
        assertEquals(0, parallelStream.count());

        parallelStream = createFloatStream(new float[] { 42.5f });
        assertEquals(42.5f, parallelStream.first().get());

        parallelStream = createFloatStream(new float[] { -5.0f, -3.0f, -1.0f, 0.0f, 1.0f, 3.0f, 5.0f });
        long positiveCount = parallelStream.filter(f -> f > 0).count();
        assertEquals(3, positiveCount);

        parallelStream = createFloatStream(new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1.0f });
        long finiteCount = parallelStream.filter(Float::isFinite).count();
        assertEquals(1, finiteCount);
    }

    @Test
    public void testSpecialFloatValues() {
        parallelStream = createFloatStream(
                new float[] { Float.MIN_VALUE, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f });

        long finiteCount = parallelStream.filter(Float::isFinite).count();
        assertEquals(4, finiteCount);
    }

    // Sequential-fallback branch tests (maxThreadNum > 1 but array has <= 1 element)

    @Test
    public void testFilter_SequentialFallback() {
        List<Float> result = createSingleElementFloatStream(5.0f).filter(f -> f > 3.0f).toList();
        assertEquals(1, result.size());
        assertEquals(5.0f, result.get(0), 0.0001f);

        List<Float> empty = createSingleElementFloatStream(1.0f).filter(f -> f > 3.0f).toList();
        assertEquals(0, empty.size());
    }

    @Test
    @DisplayName("Test exception handling in parallel operations")
    public void testExceptionHandling() {
        assertThrows(RuntimeException.class, () -> {
            parallelStream.filter(f -> {
                if (f == 5.0f)
                    throw new RuntimeException("Test exception in filter");
                return true;
            }).count();
        });

        parallelStream = createFloatStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.map(f -> {
                if (f == 7.0f)
                    throw new RuntimeException("Test exception in map");
                return f;
            }).toArray();
        });
    }

    @Test
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(f -> f > 5).count();
        });
    }

    @Test
    public void testTakeWhile() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        FloatPredicate predicate = f -> f < 4.0f;
        List<Float> result = stream.takeWhile(predicate).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(3.0f));
        assertFalse(result.contains(4.0f));
    }

    @Test
    public void testTakeWhile_SequentialFallback() {
        List<Float> result = createSingleElementFloatStream(2.0f).takeWhile(f -> f < 5.0f).toList();
        assertEquals(1, result.size());
        assertEquals(2.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testDropWhile() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        FloatPredicate predicate = f -> f < 4.0f;
        List<Float> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains(4.0f));
        assertFalse(result.contains(3.0f));
    }

    @Test
    public void testDropWhile_SequentialFallback() {
        List<Float> result = createSingleElementFloatStream(2.0f).dropWhile(f -> f < 5.0f).toList();
        assertEquals(0, result.size());

        List<Float> kept = createSingleElementFloatStream(10.0f).dropWhile(f -> f < 5.0f).toList();
        assertEquals(1, kept.size());
        assertEquals(10.0f, kept.get(0), 0.0001f);
    }

    @Test
    public void testMap() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        FloatUnaryOperator mapper = f -> f + 1.0f;
        List<Float> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2.0f));
        assertFalse(result.contains(1.0f));
    }

    @Test
    public void testMap_SequentialFallback() {
        List<Float> result = createSingleElementFloatStream(3.0f).map(f -> f * 2.0f).toList();
        assertEquals(1, result.size());
        assertEquals(6.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testMapToInt() {
        FloatStream stream = createFloatStream(new float[] { 10.1f, 20.9f, 30.0f });
        FloatToIntFunction mapper = f -> (int) f;
        List<Integer> result = stream.mapToInt(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));
        assertTrue(result.contains(30));
    }

    @Test
    public void testMapToInt_SequentialFallback() {
        List<Integer> result = createSingleElementFloatStream(7.9f).mapToInt(f -> (int) f).toList();
        assertEquals(1, result.size());
        assertEquals(7, result.get(0));
    }

    @Test
    public void testMapToLong() {
        FloatStream stream = createFloatStream(new float[] { 10.1f, 20.9f, 30.0f });
        FloatToLongFunction mapper = f -> (long) f;
        List<Long> result = stream.mapToLong(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(10L));
        assertTrue(result.contains(20L));
        assertTrue(result.contains(30L));
    }

    @Test
    public void testMapToLong_empty() {
        parallelStream = createFloatStream(new float[] {});
        long[] result = parallelStream.mapToLong(f -> (long) f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToLong_SequentialFallback() {
        List<Long> result = createSingleElementFloatStream(4.0f).mapToLong(f -> (long) f).toList();
        assertEquals(1, result.size());
        assertEquals(4L, result.get(0));
    }

    @Test
    public void testMapToDouble() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatToDoubleFunction mapper = f -> f * 2.0;
        List<Double> result = stream.mapToDouble(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(4.0));
        assertTrue(result.contains(6.0));
    }

    @Test
    public void testMapToDouble_SequentialFallback() {
        List<Double> result = createSingleElementFloatStream(3.0f).mapToDouble(f -> (double) f * 2.0).toList();
        assertEquals(1, result.size());
        assertEquals(6.0, result.get(0), 0.0001);
    }

    @Test
    public void testMapToObj() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        FloatFunction<String> mapper = f -> "Float_" + f;
        List<String> result = stream.mapToObj(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains("Float_1.0"));
        assertTrue(result.contains("Float_26.0"));
    }

    @Test
    public void testMapToObj_SequentialFallback() {
        List<String> result = createSingleElementFloatStream(9.0f).mapToObj(f -> "v" + f).toList();
        assertEquals(1, result.size());
        assertEquals("v9.0", result.get(0));
    }

    @Test
    public void testFlatMapFloatStream() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatFunction<FloatStream> mapper = f -> FloatStream.of(f, f + 0.5f);
        List<Float> result = stream.flatMap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    public void testFlatMapFloatArray() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatFunction<float[]> mapper = f -> new float[] { f, f + 0.5f };
        List<Float> result = stream.flatmap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        FloatStream flattened = parallelStream.flatmap(f -> new float[] { f, f * 2 });
        float[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        flattened = parallelStream.flatmap(f -> f % 2 == 0 ? new float[] { f } : new float[0]);
        assertArrayEquals(new float[] { 2.0f }, flattened.toArray());
    }

    // Tests covering parallel code paths with large array

    @Test
    public void testFlatMap_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).flatMap(f -> FloatStream.of(f, f * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmap_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).flatmap(f -> new float[] { f, f * 10 }).count();
        assertEquals(52, count);
    }

    @Test
    @DisplayName("Test flatMap method")
    public void testFlatMap() {
        FloatStream flattened = parallelStream.flatMap(f -> FloatStream.of(f, f + 10));
        float[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        flattened = parallelStream.flatMap(f -> f % 2 == 0 ? FloatStream.of(f) : FloatStream.empty());
        assertArrayEquals(new float[] { 2.0f }, flattened.toArray());
    }

    @Test
    public void testFlatMap_SequentialFallback() {
        List<Float> result = createSingleElementFloatStream(2.0f).flatMap(f -> FloatStream.of(f, f + 1.0f)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(3.0f));
    }

    @Test
    public void testFlatmapArray_SequentialFallback() {
        List<Float> result = createSingleElementFloatStream(2.0f).flatmap(f -> new float[] { f, f * 2.0f }).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(4.0f));
    }

    @Test
    public void testFlatMapToInt() {
        FloatStream stream = createFloatStream(new float[] { 1.5f, 2.8f });
        FloatFunction<IntStream> mapper = f -> IntStream.of((int) f, (int) (f * 2));
        List<Integer> result = stream.flatMapToInt(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(3));
        assertTrue(result.contains(2));
        assertTrue(result.contains(5));
    }

    @Test
    public void testFlatMapToInt_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).flatMapToInt(f -> IntStream.of((int) f)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToInt_SequentialFallback() {
        List<Integer> result = createSingleElementFloatStream(3.0f).flatMapToInt(f -> IntStream.of((int) f, (int) f + 1)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(3));
        assertTrue(result.contains(4));
    }

    @Test
    public void testFlatMapToLong() {
        FloatStream stream = createFloatStream(new float[] { 1.5f, 2.8f });
        FloatFunction<LongStream> mapper = f -> LongStream.of((long) f, (long) (f * 2));
        List<Long> result = stream.flatMapToLong(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(3L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(5L));
    }

    @Test
    public void testFlatMapToLong_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).flatMapToLong(f -> LongStream.of((long) f)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToLong_SequentialFallback() {
        List<Long> result = createSingleElementFloatStream(5.0f).flatMapToLong(f -> LongStream.of((long) f, (long) f * 2)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(5L));
        assertTrue(result.contains(10L));
    }

    @Test
    public void testFlatMapToDouble() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatFunction<DoubleStream> mapper = f -> DoubleStream.of(f, f + 0.5);
        List<Double> result = stream.flatMapToDouble(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
    }

    @Test
    public void testFlatMapToDouble_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).flatMapToDouble(f -> DoubleStream.of((double) f)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToDouble_SequentialFallback() {
        List<Double> result = createSingleElementFloatStream(4.0f).flatMapToDouble(f -> DoubleStream.of(f, f * 1.5)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(4.0));
        assertTrue(result.contains(6.0));
    }

    @Test
    public void testFlatMapToObjStream() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatFunction<Stream<String>> mapper = f -> Stream.of("A" + f, "B" + f);
        List<String> result = stream.flatMapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B1.0"));
        assertTrue(result.contains("A2.0"));
        assertTrue(result.contains("B2.0"));
    }

    @Test
    public void testFlatMapToObjCollection() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatFunction<Collection<String>> mapper = f -> {
            List<String> list = new ArrayList<>();
            list.add("X" + f);
            list.add("Y" + f);
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
        Stream<String> flattened = parallelStream.flatMapToObj(f -> Stream.of("A" + f, "B" + f));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B10.0"));
    }

    @Test
    @DisplayName("Test flatmapToObj with collection method")
    public void testFlatmapToObjCollection() {
        Stream<Float> flattened = parallelStream.flatmapToObj(f -> Arrays.asList(Float.valueOf(f), Float.valueOf(f * 10)));
        List<Float> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertEquals(Float.valueOf(1.0f), result.get(0));
        assertEquals(Float.valueOf(100.0f), result.get(19));
    }

    @Test
    public void testFlatMapToObj_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).flatMapToObj(f -> Stream.of(f, f * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmapToObj_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).flatmapToObj(f -> Arrays.asList(f, f * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatMapToObj_SequentialFallback() {
        List<String> result = createSingleElementFloatStream(1.0f).flatMapToObj(f -> Stream.of("A" + f, "B" + f)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B1.0"));
    }

    @Test
    public void testFlatmapToObj_SequentialFallback() {
        List<String> result = createSingleElementFloatStream(2.0f).flatmapToObj(f -> java.util.Arrays.asList("X" + f, "Y" + f)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("X2.0"));
        assertTrue(result.contains("Y2.0"));
    }

    @Test
    public void testOnEach() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        List<Float> consumed = new ArrayList<>();
        FloatConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.peek(action).forEach(f -> {
        });
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testOnEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleElementFloatStream(7.0f).peek(f -> count.incrementAndGet()).forEach(f -> {
        });
        assertEquals(1, count.get());
    }

    @Test
    public void testForEach_IteratorSplitor() {
        AtomicInteger count = new AtomicInteger(0);
        createIteratorSplitorFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).forEach(f -> count.incrementAndGet());
        assertEquals(5, count.get());
    }

    @Test
    public void testForEach_ParallelPath() {
        AtomicInteger count = new AtomicInteger(0);
        createFloatStream(TEST_ARRAY).forEach(f -> count.incrementAndGet());
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testForEach() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        List<Float> consumed = new ArrayList<>();
        FloatConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.forEach(action);
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleElementFloatStream(3.0f).forEach(f -> count.incrementAndGet());
        assertEquals(1, count.get());
    }

    @Test
    public void testForEachWithException() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        AtomicInteger count = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            stream.forEach(f -> {
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

        parallelStream.forEach(f -> {
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
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        Map<String, String> result = stream.toMap(f -> String.valueOf(f), f -> "Value_" + f, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1.0", result.get("1.0"));
        assertEquals("Value_2.0", result.get("2.0"));
        assertEquals("Value_3.0", result.get("3.0"));
    }

    @Test
    public void testToMap_ParallelPath() {
        Map<Float, Float> result = createFloatStream(TEST_ARRAY).toMap(f -> f, f -> f * 2, (a, b) -> a, java.util.HashMap::new);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testToMap_SequentialFallback() {
        Map<String, Float> result = createSingleElementFloatStream(5.0f).toMap(f -> "k" + f, f -> f, (a, b) -> a, java.util.HashMap::new);
        assertEquals(1, result.size());
        assertEquals(5.0f, result.get("k5.0"), 0.0001f);
    }

    @Test
    public void testGroupTo() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 1.0f, 3.0f, 2.0f });
        Map<String, List<Float>> result = stream.groupTo(f -> String.valueOf(f), Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(List.of(1.0f, 1.0f), result.get("1.0"));
        assertEquals(List.of(2.0f, 2.0f), result.get("2.0"));
        assertEquals(List.of(3.0f), result.get("3.0"));
    }

    @Test
    public void testGroupTo_SequentialFallback() {
        Map<Boolean, List<Float>> result = createSingleElementFloatStream(4.0f).groupTo(f -> f > 3.0f, java.util.stream.Collectors.toList(),
                java.util.HashMap::new);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(true));
        assertEquals(1, result.get(true).size());
    }

    @Test
    @DisplayName("Test precision in calculations")
    public void testFloatPrecision() {
        parallelStream = createFloatStream(new float[] { 0.1f, 0.2f, 0.3f });
        float sum = parallelStream.reduce(0.0f, Float::sum);

        assertEquals(0.6f, sum, 0.0001f);

        parallelStream = createFloatStream(new float[] { 1e-7f, 1e-7f, 1e-7f });
        float smallSum = parallelStream.reduce(0.0f, Float::sum);
        assertEquals(3e-7f, smallSum, 1e-10f);
    }

    @Test
    @DisplayName("Test with large arrays")
    public void testLargeArrays() {
        float[] largeArray = new float[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i + 1.0f;
        }

        parallelStream = createFloatStream(largeArray);

        float sum = parallelStream.reduce(0.0f, Float::sum);
        assertEquals(500500.0f, sum);

        parallelStream = createFloatStream(largeArray);
        long evenCount = parallelStream.filter(f -> f % 2 == 0).count();
        assertEquals(500, evenCount);
    }

    @Test
    public void testReduce_WithIdentity_ParallelPath() {
        float result = createFloatStream(TEST_ARRAY).reduce(0.0f, Float::sum);
        float expected = 0;
        for (float v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, result, 0.01f);
    }

    @Test
    public void testReduce_NoIdentity_ParallelPath() {
        OptionalFloat result = createFloatStream(TEST_ARRAY).reduce(Float::sum);
        assertTrue(result.isPresent());
    }

    @Test
    public void testReduceWithIdentity() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatBinaryOperator op = (f1, f2) -> f1 + f2;
        float result = stream.reduce(0.0f, op);
        assertEquals(6.0f, result, 0.0001f);

        FloatStream emptyStream = createFloatStream(new float[] {});
        float emptyResult = emptyStream.reduce(100.0f, op);
        assertEquals(100.0f, emptyResult, 0.0001f);
    }

    @Test
    public void testReduceWithoutIdentity() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatBinaryOperator accumulator = (f1, f2) -> f1 + f2;
        OptionalFloat result = stream.reduce(accumulator);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get(), 0.0001f);

        FloatStream emptyStream = createFloatStream(new float[] {});
        OptionalFloat emptyResult = emptyStream.reduce(accumulator);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testOverflowScenarios() {
        parallelStream = createFloatStream(new float[] { Float.MAX_VALUE, Float.MAX_VALUE });
        float sum = parallelStream.reduce(0.0f, Float::sum);
        assertTrue(Float.isInfinite(sum));
    }

    @Test
    public void testReduce_WithIdentity_SequentialFallback() {
        float result = createSingleElementFloatStream(7.0f).reduce(0.0f, Float::sum);
        assertEquals(7.0f, result, 0.0001f);
    }

    @Test
    public void testReduce_NoIdentity_SequentialFallback() {
        OptionalFloat result = createSingleElementFloatStream(9.0f).reduce(Float::sum);
        assertTrue(result.isPresent());
        assertEquals(9.0f, result.get(), 0.0001f);
    }

    @Test
    public void testIteratorSplitorReduceAndFindOperations_SparseMatch() {
        assertEquals(72.0f, createIteratorSplitorFloatStream(21.0f, 2.0f, 4.0f, 7.0f, 6.0f, 11.0f, 8.0f, 13.0f).reduce(0.0f, Float::sum), 0.0001f);

        OptionalFloat reduced = createIteratorSplitorFloatStream(21.0f, 2.0f, 4.0f).reduce(Float::sum);
        assertTrue(reduced.isPresent());
        assertEquals(27.0f, reduced.get(), 0.0001f);

        OptionalFloat firstMatch = createIteratorSplitorFloatStream(21.0f, 2.0f, 4.0f, 7.0f, 6.0f, 11.0f, 8.0f, 13.0f).findFirst(f -> {
            if (f == 21.0f) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return f > 5.0f && ((int) f) % 2 == 1;
        });
        assertTrue(firstMatch.isPresent());
        assertEquals(21.0f, firstMatch.get(), 0.0001f);

        OptionalFloat anyMatch = createIteratorSplitorFloatStream(21.0f, 2.0f, 4.0f, 7.0f, 6.0f, 11.0f, 8.0f, 13.0f)
                .findAny(f -> f > 5.0f && ((int) f) % 2 == 1);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21.0f || anyMatch.get() == 7.0f || anyMatch.get() == 11.0f || anyMatch.get() == 13.0f);

        OptionalFloat lastMatch = createIteratorSplitorFloatStream(21.0f, 2.0f, 4.0f, 7.0f, 6.0f, 11.0f, 8.0f, 13.0f).findLast(f -> {
            if (f == 13.0f) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return f > 5.0f && ((int) f) % 2 == 1;
        });
        assertTrue(lastMatch.isPresent());
        assertEquals(13.0f, lastMatch.get(), 0.0001f);
    }

    @Test
    public void testCollect_ParallelPath() {
        List<Float> result = createFloatStream(TEST_ARRAY).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testCollect() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        List<Float> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1.0f, 2.0f, 3.0f), collectedList);
    }

    @Test
    public void testCollect_SequentialFallback() {
        List<Float> result = createSingleElementFloatStream(3.0f).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(1, result.size());
        assertEquals(3.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).anyMatch(f -> f == 13.0f));
        assertFalse(createFloatStream(TEST_ARRAY).anyMatch(f -> f == 100.0f));
        assertFalse(createFloatStream(new float[] {}).anyMatch(f -> true));
    }

    @Test
    public void testAnyMatch_SequentialFallback() {
        assertTrue(createSingleElementFloatStream(5.0f).anyMatch(f -> f == 5.0f));
        assertFalse(createSingleElementFloatStream(5.0f).anyMatch(f -> f == 6.0f));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).allMatch(f -> f >= 1.0f && f <= 26.0f));
        assertFalse(createFloatStream(TEST_ARRAY).allMatch(f -> f < 26.0f));
        assertTrue(createFloatStream(new float[] {}).allMatch(f -> false));
    }

    @Test
    public void testAllMatch_SequentialFallback() {
        assertTrue(createSingleElementFloatStream(5.0f).allMatch(f -> f > 0.0f));
        assertFalse(createSingleElementFloatStream(5.0f).allMatch(f -> f > 10.0f));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).noneMatch(f -> f == 100.0f));
        assertFalse(createFloatStream(TEST_ARRAY).noneMatch(f -> f == 1.0f));
        assertTrue(createFloatStream(new float[] {}).noneMatch(f -> true));
    }

    // Cover the ITERATOR splitor path for anyMatch / allMatch / noneMatch / forEach
    @Test
    public void testAnyMatchAllMatchNoneMatch_IteratorSplitor() {
        assertTrue(createIteratorSplitorFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).anyMatch(f -> f > 4.0f));
        assertFalse(createIteratorSplitorFloatStream(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 10.0f));

        assertTrue(createIteratorSplitorFloatStream(1.0f, 2.0f, 3.0f).allMatch(f -> f >= 1.0f && f <= 3.0f));
        assertFalse(createIteratorSplitorFloatStream(1.0f, 2.0f, 3.0f).allMatch(f -> f < 3.0f));

        assertTrue(createIteratorSplitorFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 10.0f));
        assertFalse(createIteratorSplitorFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 2.0f));
    }

    @Test
    public void testNoneMatch_SequentialFallback() {
        assertTrue(createSingleElementFloatStream(5.0f).noneMatch(f -> f > 10.0f));
        assertFalse(createSingleElementFloatStream(5.0f).noneMatch(f -> f > 0.0f));
    }

    @Test
    public void testFindFirst() {
        FloatStream stream = createFloatStream(new float[] { 4.0f, 2.0f, 1.0f, 3.0f, 1.0f });
        OptionalFloat result = stream.findFirst(f -> f == 1.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get(), 0.0001f);

        stream = createFloatStream(new float[] { 4.0f, 2.0f, 1.0f, 3.0f, 1.0f });
        OptionalFloat notFound = stream.findFirst(f -> f == 10.0f);
        assertFalse(notFound.isPresent());

        OptionalFloat empty = createFloatStream(new float[] {}).findFirst(f -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindFirst_SequentialFallback() {
        OptionalFloat found = createSingleElementFloatStream(5.0f).findFirst(f -> f > 0.0f);
        assertTrue(found.isPresent());
        assertEquals(5.0f, found.get(), 0.0001f);

        OptionalFloat notFound = createSingleElementFloatStream(5.0f).findFirst(f -> f > 10.0f);
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalFloat result = parallelStream.findFirst(f -> f > 5);

        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findFirst(f -> f > 30);
        assertFalse(result.isPresent());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findFirst(f -> f == 1.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get());
    }

    @Test
    public void testFindAny() {
        FloatStream stream = createFloatStream(new float[] { 4.0f, 2.0f, 1.0f, 3.0f, 1.0f });
        OptionalFloat result = stream.findAny(f -> f == 1.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get(), 0.0001f);

        stream = createFloatStream(new float[] { 4.0f, 2.0f, 1.0f, 3.0f, 1.0f });
        OptionalFloat notFound = stream.findAny(f -> f == 10.0f);
        assertFalse(notFound.isPresent());

        OptionalFloat empty = createFloatStream(new float[] {}).findAny(f -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindAny_SequentialFallback() {
        OptionalFloat found = createSingleElementFloatStream(3.0f).findAny(f -> f < 10.0f);
        assertTrue(found.isPresent());
        assertEquals(3.0f, found.get(), 0.0001f);
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalFloat result = parallelStream.findAny(f -> f > 5);

        assertTrue(result.isPresent());
        assertTrue(result.get() > 5);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findAny(f -> f > 30);
        assertFalse(result.isPresent());

        parallelStream = createFloatStream(new float[] { 5.0f });
        result = parallelStream.findAny(f -> f == 5.0f);
        assertTrue(result.isPresent());
        assertEquals(5.0f, result.get());
    }

    @Test
    public void testFindLast() {
        FloatStream stream = createFloatStream(new float[] { 4.0f, 2.0f, 1.0f, 3.0f, 1.0f });
        OptionalFloat result = stream.findLast(f -> f == 1.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get(), 0.0001f);

        stream = createFloatStream(new float[] { 4.0f, 2.0f, 1.0f, 3.0f, 1.0f });
        OptionalFloat notFound = stream.findLast(f -> f == 10.0f);
        assertFalse(notFound.isPresent());

        OptionalFloat empty = createFloatStream(new float[] {}).findLast(f -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindLast_SequentialFallback() {
        OptionalFloat found = createSingleElementFloatStream(8.0f).findLast(f -> f > 0.0f);
        assertTrue(found.isPresent());
        assertEquals(8.0f, found.get(), 0.0001f);
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalFloat result = parallelStream.findLast(f -> f < 5);

        assertTrue(result.isPresent());
        assertEquals(4.0f, result.get());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findLast(f -> f > 30);
        assertFalse(result.isPresent());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findLast(f -> f == 10.0f);
        assertTrue(result.isPresent());
        assertEquals(10.0f, result.get());
    }

    @Test
    public void testZipWithBinaryOperator() {
        FloatStream streamA = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f, 30.0f);
        FloatBinaryOperator zipper = (f1, f2) -> f1 + f2;
        List<Float> result = streamA.zipWith(streamB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11.0f, result.get(0), 0.0001f);
        assertEquals(22.0f, result.get(1), 0.0001f);
        assertEquals(33.0f, result.get(2), 0.0001f);
    }

    @Test
    public void testZipWithTernaryOperator() {
        FloatStream streamA = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f);
        FloatStream streamC = FloatStream.of(100.0f, 101.0f);
        FloatTernaryOperator zipper = (f1, f2, f3) -> f1 + f2 + f3;
        List<Float> result = streamA.zipWith(streamB, streamC, zipper).sorted().toList();
        assertEquals(2, result.size());
        assertEquals(1.0f + 10.0f + 100.0f, result.get(0), 0.0001f);
        assertEquals(2.0f + 20.0f + 101.0f, result.get(1), 0.0001f);
    }

    @Test
    @DisplayName("Test zipWith two streams with default values")
    public void testZipWithBinaryDefaults() {
        FloatStream stream2 = createFloatStream(new float[] { 10.0f, 20.0f, 30.0f });
        float[] result = parallelStream.zipWith(stream2, 0.0f, 100.0f, (a, b) -> a + b).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(11.0f, result[0]);
        assertEquals(22.0f, result[1]);
        assertEquals(33.0f, result[2]);
        assertEquals(104.0f, result[3]);
    }

    @Test
    @DisplayName("Test zipWith three streams with default values")
    public void testZipWithTernaryDefaults() {
        FloatStream stream2 = createFloatStream(new float[] { 10.0f, 20.0f });
        FloatStream stream3 = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f });
        float[] result = parallelStream.zipWith(stream2, stream3, 0.0f, 0.0f, 1.0f, (a, b, c) -> a + b + c).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(6.0f, result[0]);
        assertEquals(24.0f, result[9]);
    }

    @Test
    public void testZipWithBinary_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).zipWith(FloatStream.of(TEST_ARRAY), (a, b) -> a + b).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernary_ParallelPath() {
        long count = createFloatStream(TEST_ARRAY).zipWith(FloatStream.of(TEST_ARRAY), FloatStream.of(TEST_ARRAY), (a, b, c) -> a + b + c).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryDefaults_ParallelPath() {
        float[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createFloatStream(TEST_ARRAY).zipWith(FloatStream.of(shorter), 0.0f, -1.0f, (a, b) -> a + b).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernaryDefaults_ParallelPath() {
        float[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createFloatStream(TEST_ARRAY).zipWith(FloatStream.of(shorter), FloatStream.of(shorter), 0.0f, -1.0f, -2.0f, (a, b, c) -> a + b + c)
                .count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        FloatStream streamA = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f, 30.0f);
        float valA = 0.0f;
        float valB = -1.0f;
        FloatBinaryOperator zipper = (f1, f2) -> {
            if (f1 == valA)
                return f2;
            if (f2 == valB)
                return f1;
            return f1 + f2;
        };
        List<Float> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11.0f, result.get(0), 0.0001f);
        assertEquals(22.0f, result.get(1), 0.0001f);
        assertEquals(30.0f, result.get(2), 0.0001f);
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        FloatStream streamA = createFloatStream(new float[] { 1.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f);
        FloatStream streamC = FloatStream.of(100.0f, 101.0f, 102.0f);
        float valA = 0.0f;
        float valB = -1.0f;
        float valC = -2.0f;
        FloatTernaryOperator zipper = (f1, f2, f3) -> {
            return f1 + f2 + f3;
        };
        List<Float> result = streamA.zipWith(streamB, streamC, valA, valB, valC, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(1.0f + 10.0f + 100.0f, result.get(1), 0.0001f);
        assertEquals(0.0f + 20.0f + 101.0f, result.get(2), 0.0001f);
        assertEquals(0.0f + -1.0f + 102.0f, result.get(0), 0.0001f);
    }

    @Test
    @DisplayName("Test zipWith two streams with binary operator")
    public void testZipWithBinary() {
        FloatStream stream2 = createFloatStream(new float[] { 10.0f, 20.0f, 30.0f, 40.0f, 50.0f });
        FloatStream zipped = parallelStream.zipWith(stream2, (a, b) -> a + b);
        float[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new float[] { 11.0f, 22.0f, 33.0f, 44.0f, 55.0f }, result);

        parallelStream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        stream2 = createFloatStream(new float[] { 10.0f, 20.0f, 30.0f, 40.0f, 50.0f });
        zipped = parallelStream.zipWith(stream2, (a, b) -> a * b);
        assertEquals(3, zipped.count());
    }

    @Test
    @DisplayName("Test zipWith three streams with ternary operator")
    public void testZipWithTernary() {
        FloatStream stream2 = createFloatStream(new float[] { 10.0f, 20.0f, 30.0f, 40.0f, 50.0f });
        FloatStream stream3 = createFloatStream(new float[] { 100.0f, 100.0f, 100.0f, 100.0f, 100.0f });
        FloatStream zipped = parallelStream.zipWith(stream2, stream3, (a, b, c) -> (a + b) * c / 100);
        float[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new float[] { 11.0f, 22.0f, 33.0f, 44.0f, 55.0f }, result);
    }

    @Test
    public void testZipWith_SequentialFallback() {
        FloatStream streamA = createSingleElementFloatStream(3.0f);
        FloatStream streamB = FloatStream.of(7.0f);
        List<Float> result = streamA.zipWith(streamB, (a, b) -> a + b).toList();
        assertEquals(1, result.size());
        assertEquals(10.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testZipWithTernary_SequentialFallback() {
        FloatStream streamA = createSingleElementFloatStream(1.0f);
        FloatStream streamB = FloatStream.of(2.0f);
        FloatStream streamC = FloatStream.of(3.0f);
        List<Float> result = streamA.zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList();
        assertEquals(1, result.size());
        assertEquals(6.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testZipWithDefaults_SequentialFallback() {
        FloatStream streamA = createSingleElementFloatStream(5.0f);
        FloatStream streamB = FloatStream.of(10.0f, 20.0f);
        List<Float> result = streamA.zipWith(streamB, 0.0f, 0.0f, (a, b) -> a + b).sorted().toList();
        assertEquals(2, result.size());
        assertEquals(15.0f, result.get(0), 0.0001f);
        assertEquals(20.0f, result.get(1), 0.0001f);
    }

    @Test
    public void testZipWithTernaryDefaults_SequentialFallback() {
        FloatStream streamA = createSingleElementFloatStream(1.0f);
        FloatStream streamB = FloatStream.of(2.0f, 3.0f);
        FloatStream streamC = FloatStream.of(4.0f, 5.0f, 6.0f);
        List<Float> result = streamA.zipWith(streamB, streamC, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c).sorted().toList();
        // sorted ascending: [6.0, 7.0, 8.0]
        assertEquals(3, result.size());
        assertEquals(6.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testZipWithBinaryDefaults_SequentialFallback_UnevenLengths() {
        List<Float> result = FloatStream.of(1F, 2F, 3F)
                .parallel(PS.create(Splitor.ARRAY).maxThreadNum(1))
                .zipWith(FloatStream.of(10F), 0F, -1F, (a, b) -> a + b)
                .toList();

        assertEquals(3, result.size());
        assertEquals(11F, result.get(0), 0.0001F);
        assertEquals(1F, result.get(1), 0.0001F);
        assertEquals(2F, result.get(2), 0.0001F);
    }

    @Test
    public void testIsParallel() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
    }

    @Test
    public void testSequential() {
        FloatStream parallelStream = createFloatStream(TEST_ARRAY);
        FloatStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        List<Float> result = sequentialStream.toList();
        assertEquals(TEST_ARRAY.length, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i), 0.0001f);
        }
        parallelStream.close();
        sequentialStream.close();
    }

    @Test
    public void testSequential_ParallelPath() {
        FloatStream parallel = createFloatStream(TEST_ARRAY);
        assertTrue(parallel.isParallel());
        FloatStream seq = parallel.sequential();
        assertFalse(seq.isParallel());
        assertEquals(TEST_ARRAY.length, seq.count());
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelArrayFloatStream) createFloatStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ARRAY, ((ParallelArrayFloatStream) createFloatStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelArrayFloatStream) createFloatStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    @DisplayName("Test method chaining")
    public void testMethodChaining() {
        List<String> result = parallelStream.filter(f -> f > 3)
                .map(f -> f * 2)
                .flatMapToObj(f -> Stream.of("Value: " + f, "Half: " + (f / 2)))
                .sorted()
                .toList();

        assertTrue(result.size() > 0);
        assertTrue(result.contains("Value: 8.0"));
        assertTrue(result.contains("Half: 4.0"));
    }

    @Test
    public void testOnClose() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        FloatStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get());
        newStream.close();
        assertTrue(closedFlag.get());
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        AtomicInteger closedCount = new AtomicInteger(0);
        Runnable handler1 = () -> closedCount.incrementAndGet();
        Runnable handler2 = () -> closedCount.incrementAndGet();

        FloatStream newStream = stream.onClose(handler1).onClose(handler2);
        assertEquals(0, closedCount.get());
        newStream.close();
        assertEquals(2, closedCount.get());
    }

    @Test
    public void testOnCloseEmptyHandler() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        FloatStream newStream = stream.onClose(null);
        assertSame(stream, newStream);
        newStream.close();
    }

    @Test
    public void testBoundaryValues() {
        float[] boundaryArray = new float[] { Float.MIN_VALUE, -Float.MAX_VALUE, -1.0f, 0.0f, 1.0f, Float.MAX_VALUE };
        parallelStream = createFloatStream(boundaryArray);
        float[] result = parallelStream.sorted().toArray();
        assertEquals(6, result.length);
        assertEquals(-Float.MAX_VALUE, result[0], 0.0001f);
        assertEquals(Float.MAX_VALUE, result[5], 0.0001f);
    }
}
