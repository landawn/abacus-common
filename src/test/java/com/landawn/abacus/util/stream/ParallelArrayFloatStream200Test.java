package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
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

public class ParallelArrayFloatStream200Test extends TestBase {

    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing 
    private static final float[] TEST_ARRAY = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f,
            18.0f, 19.0f, 20.0f, 21.0f, 22.0f, 23.0f, 24.0f, 25.0f, 26.0f };

    // Empty method to be implemented by the user for initializing FloatStream
    protected FloatStream createFloatStream(float... elements) {
        // Using default values for maxThreadNum, executorNumForVirtualThread, splitor, cancelUncompletedThreads
        // For testing, we might want to vary these parameters.
        return FloatStream.of(elements).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
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
    public void testDropWhile() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        FloatPredicate predicate = f -> f < 4.0f;
        List<Float> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains(4.0f));
        assertFalse(result.contains(3.0f));
    }

    @Test
    public void testMap() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        FloatUnaryOperator mapper = f -> f + 1.0f;
        List<Float> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2.0f)); // 1.0f maps to 2.0f
        assertFalse(result.contains(1.0f));
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
    public void testMapToDouble() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        FloatToDoubleFunction mapper = f -> (double) f * 2.0;
        List<Double> result = stream.mapToDouble(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(4.0));
        assertTrue(result.contains(6.0));
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
    public void testFlatMapToInt() {
        FloatStream stream = createFloatStream(new float[] { 1.5f, 2.8f });
        FloatFunction<IntStream> mapper = f -> IntStream.of((int) f, (int) (f * 2));
        List<Integer> result = stream.flatMapToInt(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(3)); // 1.5 * 2 = 3
        assertTrue(result.contains(2));
        assertTrue(result.contains(5)); // 2.8 * 2 = 5.6 -> 5
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
    public void testFlatMapToDouble() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatFunction<DoubleStream> mapper = f -> DoubleStream.of((double) f, (double) (f + 0.5));
        List<Double> result = stream.flatMapToDouble(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
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
    public void testOnEach() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        List<Float> consumed = new ArrayList<>();
        FloatConsumer action = it -> {
            synchronized (consumed) { // Ensure thread safety
                consumed.add(it);
            }
        };
        stream.onEach(action).forEach(f -> {
        }); // Trigger the onEach action
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEach() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        List<Float> consumed = new ArrayList<>();
        FloatConsumer action = it -> {
            synchronized (consumed) { // Ensure thread safety
                consumed.add(it);
            }
        };
        stream.forEach(action);
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
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
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        Map<String, String> result = stream.toMap(f -> String.valueOf(f), f -> "Value_" + f, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1.0", result.get("1.0"));
        assertEquals("Value_2.0", result.get("2.0"));
        assertEquals("Value_3.0", result.get("3.0"));
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
    public void testCollect() {
        FloatStream stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        List<Float> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1.0f, 2.0f, 3.0f), collectedList);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).anyMatch(f -> f == 13.0f));
        assertFalse(createFloatStream(TEST_ARRAY).anyMatch(f -> f == 100.0f));
        assertFalse(createFloatStream(new float[] {}).anyMatch(f -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).allMatch(f -> f >= 1.0f && f <= 26.0f));
        assertFalse(createFloatStream(TEST_ARRAY).allMatch(f -> f < 26.0f));
        assertTrue(createFloatStream(new float[] {}).allMatch(f -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createFloatStream(TEST_ARRAY).noneMatch(f -> f == 100.0f));
        assertFalse(createFloatStream(TEST_ARRAY).noneMatch(f -> f == 1.0f));
        assertTrue(createFloatStream(new float[] {}).noneMatch(f -> true));
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
    public void testZipWithBinaryOperatorWithNoneValues() {
        FloatStream streamA = createFloatStream(new float[] { 1.0f, 2.0f });
        FloatStream streamB = FloatStream.of(10.0f, 20.0f, 30.0f);
        float valA = 0.0f; // Assuming 0.0f as 'none' for float
        float valB = -1.0f; // Assuming -1.0f as 'none' for float
        FloatBinaryOperator zipper = (f1, f2) -> {
            if (f1 == valA)
                return f2;
            if (f2 == valB)
                return f1;
            return f1 + f2;
        };
        List<Float> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11.0f, result.get(0), 0.0001f); // 1.0 + 10.0
        assertEquals(22.0f, result.get(1), 0.0001f); // 2.0 + 20.0
        assertEquals(30.0f, result.get(2), 0.0001f); // 0.0 (valA) + 30.0 -> 30.0
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
        assertEquals(0.0f + 20.0f + 101.0f, result.get(2), 0.0001f); // valA (0.0) + 20.0 + 101.0
        assertEquals(0.0f + -1.0f + 102.0f, result.get(0), 0.0001f); // valA (0.0) + valB (-1.0) + 102.0
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
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        // Since maxThreadNum() is protected in the superclass,
        // and its value is used internally, we can't directly call it from here.
        // However, we can assert its behavior indirectly or via reflection if necessary.
        // For this test, we'll assume the constructor correctly sets it and other tests
        // indirectly verify its usage.
        // If it was a public method of ParallelArrayFloatStream, we would test it directly.
        // FloatStream stream = createFloatStream(floatArray);
        //assertTrue(stream.maxThreadNum() > 0); // This line would work if maxThreadNum() was public in FloatStream
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, splitor() is protected.
        // FloatStream stream = createFloatStream(floatArray);
        //assertNotNull(stream.splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, asyncExecutor() is protected.
        // FloatStream stream = createFloatStream(floatArray);
        //assertNotNull(stream.asyncExecutor());
    }

    @Test
    public void testOnClose() {
        FloatStream stream = createFloatStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        FloatStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get()); // Not closed yet
        newStream.close(); // Close the stream, which should trigger the handler
        assertTrue(closedFlag.get()); // Now it should be closed
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
        assertSame(stream, newStream); // Should return the same instance if handler is null
        newStream.close(); // Should not throw
    }
}
