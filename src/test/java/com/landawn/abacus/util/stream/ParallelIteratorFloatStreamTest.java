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
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatTernaryOperator;
import com.landawn.abacus.util.function.FloatToLongFunction;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelIteratorFloatStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final float[] TEST_ARRAY = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f,
            18.0f, 19.0f, 20.0f, 21.0f, 22.0f, 23.0f, 24.0f, 25.0f, 26.0f };
    private static final float[] DEFAULT_ARRAY = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f };

    private FloatStream parallelStream;

    protected FloatStream createFloatStream(float... elements) {
        return FloatStream.of(elements).map(e -> (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createFloatStream(DEFAULT_ARRAY);
    }

    // Tests using thread count = 1 to exercise the canBeSequential sequential fallback branches
    protected FloatStream createSingleThreadFloatStream(float... elements) {
        return FloatStream.of(elements).map(e -> (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1));
    }

    @Test
    public void testFilter() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        List<Float> result = stream.filter(f -> f > 20.0f).toList();
        assertEquals(6, result.size());
        for (Float f : result) {
            assertTrue(f > 20.0f);
        }

        FloatStream emptyStream = createFloatStream(new float[] {});
        List<Float> emptyResult = emptyStream.filter(f -> true).toList();
        assertEquals(0, emptyResult.size());
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
    public void testFilter_singleThread() {
        List<Float> result = createSingleThreadFloatStream(TEST_ARRAY).filter(f -> f > 20.0f).toList();
        assertEquals(6, result.size());
        for (Float f : result) {
            assertTrue(f > 20.0f);
        }
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
    public void testTakeWhile_singleThread() {
        List<Float> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }).takeWhile(f -> f < 4.0f).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(3.0f));
    }

    @Test
    public void testDropWhile() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });
        List<Float> result = stream.dropWhile(f -> f < 4.0f).toList();
        assertHaveSameElements(Arrays.asList(4.0f, 5.0f), result);

        FloatStream stream2 = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        List<Float> result2 = stream2.dropWhile(f -> true).toList();
        assertEquals(0, result2.size());
    }

    @Test
    public void testDropWhile_singleThread() {
        List<Float> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }).dropWhile(f -> f < 4.0f).toList();
        assertHaveSameElements(Arrays.asList(4.0f, 5.0f), result);
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
    public void testMap_singleThread() {
        List<Float> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).map(f -> f * 2.0f).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(4.0f));
        assertTrue(result.contains(6.0f));
    }

    @Test
    public void testMapToInt() {
        FloatStream stream = createFloatStream(new float[] { 1.5f, 2.5f, 3.5f });
        List<Integer> result = stream.mapToInt(f -> (int) f).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testMapToInt_singleThread() {
        List<Integer> result = createSingleThreadFloatStream(new float[] { 1.5f, 2.5f, 3.5f }).mapToInt(f -> (int) f).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
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
    public void testMapToLong_singleThread() {
        List<Long> result = createSingleThreadFloatStream(new float[] { 10.1f, 20.9f, 30.0f }).mapToLong(f -> (long) f).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(10L));
        assertTrue(result.contains(20L));
        assertTrue(result.contains(30L));
    }

    @Test
    public void testMapToDouble() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        List<Double> result = stream.mapToDouble(f -> (double) f).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(3.0));
    }

    @Test
    public void testMapToDouble_singleThread() {
        List<Double> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).mapToDouble(f -> (double) f).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(3.0));
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
    public void testMapToObj_singleThread() {
        List<String> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).mapToObj(f -> "val_" + f).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("val_1.0"));
        assertTrue(result.contains("val_2.0"));
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
    public void testFlatMap_singleThread() {
        List<Float> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).flatMap(f -> FloatStream.of(f, f + 10.0f)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(11.0f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(12.0f));
    }

    @Test
    public void testFlatmap_singleThread() {
        List<Float> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).flatmap(f -> new float[] { f, f + 0.5f }).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
    }

    @Test
    public void testFlatMapToInt() {
        FloatStream stream = createFloatStream(new float[] { 1.5f, 2.5f });
        List<Integer> result = stream.flatMapToInt(f -> IntStream.of((int) f, (int) (f * 2))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(3));
        assertTrue(result.contains(2));
        assertTrue(result.contains(5));
    }

    @Test
    public void testFlatMapToInt_singleThread() {
        List<Integer> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(10));
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
    public void testFlatMapToLong_singleThread() {
        List<Long> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(10L));
    }

    @Test
    public void testFlatMapToDouble() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f });
        List<Double> result = stream.flatMapToDouble(f -> DoubleStream.of((double) f, (double) (f + 0.5f))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
    }

    @Test
    public void testFlatMapToDouble_singleThread() {
        List<Double> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).flatMapToDouble(f -> DoubleStream.of((double) f, (double) f + 0.5))
                .toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
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
    public void testFlatmapToObj() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f });
        List<String> result = stream.flatmapToObj(f -> Arrays.asList("X" + f, "Y" + f)).toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("X1.0", "Y1.0", "X2.0", "Y2.0")));
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
    public void testFlatMapToObj_largeArray() {
        List<String> result = createFloatStream(TEST_ARRAY).flatMapToObj(f -> Stream.of(new String[] { "v" + f })).toList();
        assertEquals(26, result.size());
    }

    @Test
    public void testFlatmapToObj_largeArray() {
        List<String> result = createFloatStream(TEST_ARRAY).flatmapToObj(f -> Arrays.asList("a" + f, "b" + f)).toList();
        assertEquals(52, result.size());
    }

    @Test
    public void testFlatMapToObj_singleThread() {
        List<String> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).flatMapToObj(f -> Stream.of("A" + f, "B" + f)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B2.0"));
    }

    @Test
    public void testFlatmapToObj_singleThread() {
        List<String> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).flatmapToObj(f -> Arrays.asList("X" + f, "Y" + f)).toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("X1.0", "Y1.0", "X2.0", "Y2.0")));
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
    public void testOnEach_singleThread() {
        List<Float> consumed = new ArrayList<>();
        createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).onEach(f -> {
            synchronized (consumed) {
                consumed.add(f);
            }
        }).forEach(f -> {
        });
        assertEquals(3, consumed.size());
    }

    @Test
    public void testForEach_singleThread() {
        List<Float> collected = new ArrayList<>();
        createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).forEach(f -> collected.add(f));
        assertHaveSameElements(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
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
    public void testToMap() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 1.0f });
        Map<String, Float> result = stream.toMap(f -> "Key_" + f, f -> f, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(2.0f, result.get("Key_1.0"), 0.0001f);
        assertEquals(2.0f, result.get("Key_2.0"), 0.0001f);
        assertEquals(3.0f, result.get("Key_3.0"), 0.0001f);
    }

    @Test
    public void testToMap_singleThread() {
        Map<String, Float> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).toMap(f -> "Key_" + f, f -> f, (v1, v2) -> v1 + v2,
                java.util.HashMap::new);
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get("Key_1.0"), 0.0001f);
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
    public void testGroupTo_singleThread() {
        Map<String, List<Float>> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 1.0f }).groupTo(f -> String.valueOf(f),
                java.util.stream.Collectors.toList(), java.util.HashMap::new);
        assertEquals(2, result.size());
        assertEquals(2, result.get("1.0").size());
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
    public void testReduce_parallelWithLargeArray() {
        float sum = createFloatStream(TEST_ARRAY).reduce(0.0f, Float::sum);
        assertEquals(351.0f, sum, 0.01f); // sum of 1..26 = 351

        OptionalFloat opt = createFloatStream(TEST_ARRAY).reduce(Float::sum);
        assertTrue(opt.isPresent());
        assertEquals(351.0f, opt.get(), 0.01f);
    }

    @Test
    public void testConstructor_withDefaultValues() {
        FloatStream stream = FloatStream
                .of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f, 18.0f, 19.0f, 20.0f)
                .parallel();
        float sum = stream.reduce(0.0f, Float::sum);
        assertEquals(210.0f, sum, 0.01f);
    }

    @Test
    public void testReduceWithIdentity() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatBinaryOperator op = (f1, f2) -> f1 + f2;
        float result = stream.reduce(0.0f, op);
        assertEquals(6.0f, result, 0.0001f);

        FloatStream emptyStream = createFloatStream(new float[] {});
        float emptyResult = emptyStream.reduce(0f, op);
        assertEquals(0f, emptyResult, 0.0001f);
    }

    @Test
    public void testReduceWithoutIdentity() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });
        OptionalFloat result = stream.reduce((f1, f2) -> f1 + f2);
        assertTrue(result.isPresent());
        assertEquals(15.0f, result.get(), 0.0001f);

        FloatStream emptyStream = createFloatStream(new float[] {});
        OptionalFloat emptyResult = emptyStream.reduce((f1, f2) -> f1 + f2);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testReduce_emptyStreamParallel() {
        float result = createFloatStream(new float[0]).reduce(0.0f, Float::sum);
        assertEquals(0.0f, result, 0.001f);

        OptionalFloat opt = createFloatStream(new float[0]).reduce(Float::sum);
        assertFalse(opt.isPresent());
    }

    @Test
    public void testReduceWithIdentity_singleThread() {
        float result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).reduce(0.0f, (f1, f2) -> f1 + f2);
        assertEquals(6.0f, result, 0.0001f);
    }

    @Test
    public void testReduceWithoutIdentity_singleThread() {
        OptionalFloat result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).reduce((f1, f2) -> f1 + f2);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get(), 0.0001f);
    }

    // Covers delayed-match ordering for iterator-backed terminal operations.
    @Test
    public void testReduceAndFindOperations_DelayedMatchOrdering() {
        assertEquals(72.0f, createFloatStream(21.0f, 2.0f, 4.0f, 7.0f, 6.0f, 11.0f, 8.0f, 13.0f).reduce(0.0f, Float::sum), 0.0001f);

        OptionalFloat reduced = createFloatStream(21.0f, 2.0f, 4.0f).reduce(Float::sum);
        assertTrue(reduced.isPresent());
        assertEquals(27.0f, reduced.get(), 0.0001f);

        OptionalFloat firstMatch = createFloatStream(21.0f, 2.0f, 4.0f, 7.0f, 6.0f, 11.0f, 8.0f, 13.0f).findFirst(f -> {
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

        OptionalFloat anyMatch = createFloatStream(21.0f, 2.0f, 4.0f, 7.0f, 6.0f, 11.0f, 8.0f, 13.0f).findAny(f -> f > 5.0f && ((int) f) % 2 == 1);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21.0f || anyMatch.get() == 7.0f || anyMatch.get() == 11.0f || anyMatch.get() == 13.0f);

        OptionalFloat lastMatch = createFloatStream(21.0f, 2.0f, 4.0f, 7.0f, 6.0f, 11.0f, 8.0f, 13.0f).findLast(f -> {
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
    public void testCollect() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        List<Float> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1.0f, 2.0f, 3.0f), collectedList);
    }

    @Test
    public void testCollect_singleThread() {
        List<Float> result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(Arrays.asList(1.0f, 2.0f, 3.0f), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).anyMatch(f -> f == 26.0f));
        assertFalse(createFloatStream(TEST_ARRAY).anyMatch(f -> f == 100.0f));
        assertFalse(createFloatStream(new float[] {}).anyMatch(f -> true));
    }

    @Test
    public void testAnyMatch_singleThread() {
        assertTrue(createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).anyMatch(f -> f == 2.0f));
        assertFalse(createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).anyMatch(f -> f == 99.0f));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).allMatch(f -> f >= 1.0f && f <= 26.0f));
        assertFalse(createFloatStream(TEST_ARRAY).allMatch(f -> f < 26.0f));
        assertTrue(createFloatStream(new float[] {}).allMatch(f -> false));
    }

    @Test
    public void testAllMatch_singleThread() {
        assertTrue(createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).allMatch(f -> f > 0.0f));
        assertFalse(createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).allMatch(f -> f > 1.0f));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).noneMatch(f -> f > 100.0f));
        assertFalse(createFloatStream(TEST_ARRAY).noneMatch(f -> f == 1.0f));
        assertTrue(createFloatStream(new float[] {}).noneMatch(f -> true));
    }

    @Test
    public void testNoneMatch_singleThread() {
        assertTrue(createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).noneMatch(f -> f > 100.0f));
        assertFalse(createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).noneMatch(f -> f == 1.0f));
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
    public void testFindFirst_singleThread() {
        OptionalFloat result = createSingleThreadFloatStream(new float[] { 4.0f, 2.0f, 1.0f, 3.0f }).findFirst(f -> f == 1.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get(), 0.0001f);

        OptionalFloat notFound = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).findFirst(f -> f == 99.0f);
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
        FloatStream stream = createFloatStream(TEST_ARRAY);
        OptionalFloat result = stream.findAny(f -> f == 13.0f);
        assertTrue(result.isPresent());
        assertEquals(13.0f, result.get(), 0.0001f);

        FloatStream stream2 = createFloatStream(TEST_ARRAY);
        OptionalFloat notFound = stream2.findAny(f -> f == 100.0f);
        assertFalse(notFound.isPresent());

        OptionalFloat empty = createFloatStream(new float[] {}).findAny(f -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindAny_singleThread() {
        OptionalFloat result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).findAny(f -> f == 2.0f);
        assertTrue(result.isPresent());

        OptionalFloat notFound = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).findAny(f -> f == 99.0f);
        assertFalse(notFound.isPresent());
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
    public void testFindLast_singleThread() {
        OptionalFloat result = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 1.0f }).findLast(f -> f == 1.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get(), 0.0001f);

        OptionalFloat notFound = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f }).findLast(f -> f == 99.0f);
        assertFalse(notFound.isPresent());
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
    public void testZipWithTwoStreams() {
        FloatStream streamA = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f, 30.0f);
        float[] result = streamA.zipWith(streamB, (a, b) -> a + b).toArray();
        assertHaveSameElements(new float[] { 11.0f, 22.0f, 33.0f }, result);
    }

    @Test
    public void testZipWithTwoStreamsWithDefaultValues() {
        FloatStream streamA = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatStream streamB = FloatStream.of(10.0f);
        float[] result = streamA.zipWith(streamB, 0.0f, -1.0f, (a, b) -> a + b).toArray();
        assertHaveSameElements(new float[] { 11.0f, 1.0f, 2.0f }, result);
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
    public void testZipWith_singleThread() {
        FloatStream streamA = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f, 30.0f);
        List<Float> result = streamA.zipWith(streamB, (a, b) -> a + b).toList();
        assertHaveSameElements(Arrays.asList(11.0f, 22.0f, 33.0f), result);
    }

    @Test
    public void testZipWithTwoDefaultValues_singleThread() {
        FloatStream streamA = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f });
        FloatStream streamB = FloatStream.of(10.0f);
        List<Float> result = streamA.zipWith(streamB, 0.0f, -1.0f, (a, b) -> a + b).toList();
        assertHaveSameElements(Arrays.asList(11.0f, 1.0f), result);
    }

    @Test
    public void testZipWithThreeStreams_singleThread() {
        FloatStream streamA = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f);
        FloatStream streamC = FloatStream.of(100.0f, 200.0f);
        List<Float> result = streamA.zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList();
        assertHaveSameElements(Arrays.asList(111.0f, 222.0f), result);
    }

    @Test
    public void testZipWithThreeDefaultValues_singleThread() {
        FloatStream streamA = createSingleThreadFloatStream(new float[] { 1.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f);
        FloatStream streamC = FloatStream.of(100.0f, 200.0f, 300.0f);
        List<Float> result = streamA.zipWith(streamB, streamC, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c).sorted().toList();
        // sorted ascending: [111.0, 220.0, 300.0]
        assertEquals(3, result.size());
        assertEquals(111.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testIsParallel() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
        stream.close();
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
    public void testSequential_singleThread() {
        FloatStream stream = createSingleThreadFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatStream seq = stream.sequential();
        assertFalse(seq.isParallel());
        List<Float> result = seq.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelIteratorFloatStream) createFloatStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ITERATOR, ((ParallelIteratorFloatStream) createFloatStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelIteratorFloatStream) createFloatStream(TEST_ARRAY)).asyncExecutor() != null);
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
}
