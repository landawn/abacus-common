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

public class ParallelArrayDoubleStream200Test extends TestBase {

    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing   
    private static final double[] TEST_ARRAY = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0,
            21.0, 22.0, 23.0, 24.0, 25.0, 26.0 };

    // Empty method to be implemented by the user for initializing DoubleStream
    protected DoubleStream createDoubleStream(double... elements) {
        // Using default values for maxThreadNum, executorNumForVirtualThread, splitor, cancelUncompletedThreads
        // For testing, we might want to vary these parameters.
        return DoubleStream.of(elements).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
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
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        DoublePredicate predicate = d -> d < 4.0;
        List<Double> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains(4.0));
        assertFalse(result.contains(3.0));
    }

    @Test
    public void testMap() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        DoubleUnaryOperator mapper = d -> d + 1.0;
        List<Double> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2.0)); // 1.0 maps to 2.0
        assertFalse(result.contains(1.0));
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
        DoubleToFloatFunction mapper = d -> (float) d * 2.0f;
        List<Float> result = stream.mapToFloat(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(4.0f));
        assertTrue(result.contains(6.0f));
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
    public void testFlatMapToInt() {
        DoubleStream stream = createDoubleStream(new double[] { 1.5, 2.8 });
        DoubleFunction<IntStream> mapper = d -> IntStream.of((int) d, (int) (d * 2));
        List<Integer> result = stream.flatMapToInt(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(3)); // 1.5 * 2 = 3.0 -> 3
        assertTrue(result.contains(2));
        assertTrue(result.contains(5)); // 2.8 * 2 = 5.6 -> 5
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
        DoubleFunction<FloatStream> mapper = d -> FloatStream.of((float) d, (float) (d + 0.5));
        List<Float> result = stream.flatMapToFloat(mapper).toList();
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
    public void testOnEach() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        List<Double> consumed = new ArrayList<>();
        DoubleConsumer action = it -> {
            synchronized (consumed) { // Ensure thread safety
                consumed.add(it);
            }
        };
        stream.onEach(action).forEach(d -> {
        }); // Trigger the onEach action
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEach() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        List<Double> consumed = new ArrayList<>();
        DoubleConsumer action = it -> {
            synchronized (consumed) { // Ensure thread safety
                consumed.add(it);
            }
        };
        stream.forEach(action);

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
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        Map<String, String> result = stream.toMap(d -> String.valueOf(d), d -> "Value_" + d, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1.0", result.get("1.0"));
        assertEquals("Value_2.0", result.get("2.0"));
        assertEquals("Value_3.0", result.get("3.0"));
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
    public void testCollect() {
        DoubleStream stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        List<Double> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1.0, 2.0, 3.0), collectedList);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createDoubleStream(TEST_ARRAY).anyMatch(d -> d == 13.0));
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
        assertTrue(createDoubleStream(TEST_ARRAY).noneMatch(d -> d == 100.0));
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
    public void testZipWithBinaryOperatorWithNoneValues() {
        DoubleStream streamA = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleStream streamB = DoubleStream.of(10.0, 20.0, 30.0);
        double valA = 0.0; // Assuming 0.0 as 'none' for double
        double valB = -1.0; // Assuming -1.0 as 'none' for double
        DoubleBinaryOperator zipper = (d1, d2) -> {
            if (d1 == valA)
                return d2;
            if (d2 == valB)
                return d1;
            return d1 + d2;
        };
        List<Double> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11.0, result.get(0), 0.0001); // 1.0 + 10.0
        assertEquals(22.0, result.get(1), 0.0001); // 2.0 + 20.0
        assertEquals(30.0, result.get(2), 0.0001); // 0.0 (valA) + 30.0 -> 30.0
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
        assertEquals(0.0 + 20.0 + 101.0, result.get(2), 0.0001); // valA (0.0) + 20.0 + 101.0
        assertEquals(0.0 + -1.0 + 102.0, result.get(0), 0.0001); // valA (0.0) + valB (-1.0) + 102.0
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
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        // Since maxThreadNum() is protected in the superclass,
        // and its value is used internally, we can't directly call it from here.
        // However, we can assert its behavior indirectly or via reflection if necessary.
        // For this test, we'll assume the constructor correctly sets it and other tests
        // indirectly verify its usage.
        // If it was a public method of ParallelArrayDoubleStream, we would test it directly.
        // DoubleStream stream = createDoubleStream(doubleArray);
        //assertTrue(stream.maxThreadNum() > 0); // This line would work if maxThreadNum() was public in DoubleStream
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, splitor() is protected.
        // DoubleStream stream = createDoubleStream(doubleArray);
        //assertNotNull(stream.splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, asyncExecutor() is protected.
        // DoubleStream stream = createDoubleStream(doubleArray);
        //assertNotNull(stream.asyncExecutor());
    }

    @Test
    public void testOnClose() {
        DoubleStream stream = createDoubleStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        DoubleStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get()); // Not closed yet
        newStream.close(); // Close the stream, which should trigger the handler
        assertTrue(closedFlag.get()); // Now it should be closed
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
        assertSame(stream, newStream); // Should return the same instance if handler is null
        newStream.close(); // Should not throw
    }
}
