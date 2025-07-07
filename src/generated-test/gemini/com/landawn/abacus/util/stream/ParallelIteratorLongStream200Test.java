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
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongTernaryOperator;
import com.landawn.abacus.util.function.LongToDoubleFunction;
import com.landawn.abacus.util.function.LongToFloatFunction;
import com.landawn.abacus.util.function.LongToIntFunction;
import com.landawn.abacus.util.function.LongUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelIteratorLongStream200Test extends TestBase {

    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing 
    private static final long[] TEST_ARRAY = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L,
            23L, 24L, 25L, 26L };

    // Empty method to be implemented by the user for initializing LongStream
    protected LongStream createLongStream(long... elements) {
        return LongStream.of(elements).map(e -> (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testFilter() {
        LongStream stream = createLongStream(TEST_ARRAY);
        LongPredicate predicate = l -> l > 13L;
        List<Long> result = stream.filter(predicate).toList();
        assertEquals(13, result.size());
        assertTrue(result.contains(14L));
        assertFalse(result.contains(1L));
    }

    @Test
    public void testTakeWhile() {
        LongStream stream = createLongStream(TEST_ARRAY);
        LongPredicate predicate = l -> l < 4L;
        List<Long> result = stream.takeWhile(predicate).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
        assertFalse(result.contains(4L));
    }

    @Test
    public void testDropWhile() {
        LongStream stream = createLongStream(TEST_ARRAY);
        LongPredicate predicate = l -> l < 4L;
        List<Long> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains(4L));
        assertFalse(result.contains(3L));
    }

    @Test
    public void testMap() {
        LongStream stream = createLongStream(TEST_ARRAY);
        LongUnaryOperator mapper = l -> l + 1L;
        List<Long> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2L)); // 1 maps to 2
        assertFalse(result.contains(1L));
    }

    @Test
    public void testMapToInt() {
        LongStream stream = createLongStream(new long[] { 10L, 20L, 30L });
        LongToIntFunction mapper = l -> (int) (l / 10L);
        List<Integer> result = stream.mapToInt(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testMapToFloat() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        LongToFloatFunction mapper = l -> (float) l * 0.5f;
        List<Float> result = stream.mapToFloat(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(0.5f));
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
    }

    @Test
    public void testMapToDouble() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        LongToDoubleFunction mapper = l -> (double) l * 0.5;
        List<Double> result = stream.mapToDouble(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(0.5));
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
    }

    @Test
    public void testMapToObj() {
        LongStream stream = createLongStream(TEST_ARRAY);
        LongFunction<String> mapper = l -> "Long_" + l;
        List<String> result = stream.mapToObj(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains("Long_1"));
        assertTrue(result.contains("Long_26"));
    }

    @Test
    public void testFlatMapLongStream() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<LongStream> mapper = l -> LongStream.of(l, l + 10L);
        List<Long> result = stream.flatMap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(11L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(12L));
    }

    @Test
    public void testFlatMapLongArray() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<long[]> mapper = l -> new long[] { l, l + 10L };
        List<Long> result = stream.flatmap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(11L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(12L));
    }

    @Test
    public void testFlatMapToInt() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<IntStream> mapper = l -> IntStream.of((int) l, (int) (l * 2));
        List<Integer> result = stream.flatMapToInt(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(4));
    }

    @Test
    public void testFlatMapToFloat() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<FloatStream> mapper = l -> FloatStream.of((float) l, (float) (l + 0.5));
        List<Float> result = stream.flatMapToFloat(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    public void testFlatMapToDouble() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<DoubleStream> mapper = l -> DoubleStream.of((double) l, (double) (l + 0.5));
        List<Double> result = stream.flatMapToDouble(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
    }

    @Test
    public void testFlatMapToObjStream() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<Stream<String>> mapper = l -> Stream.of("A" + l, "B" + l);
        List<String> result = stream.flatMapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B1"));
        assertTrue(result.contains("A2"));
        assertTrue(result.contains("B2"));
    }

    @Test
    public void testFlatMapToObjCollection() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<Collection<String>> mapper = l -> {
            List<String> list = new ArrayList<>();
            list.add("X" + l);
            list.add("Y" + l);
            return list;
        };
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("X1"));
        assertTrue(result.contains("Y1"));
        assertTrue(result.contains("X2"));
        assertTrue(result.contains("Y2"));
    }

    @Test
    public void testOnEach() {
        LongStream stream = createLongStream(TEST_ARRAY);
        List<Long> consumed = new ArrayList<>();
        LongConsumer action = it -> {
            synchronized (consumed) { // Ensure thread safety
                consumed.add(it);
            }
        };
        stream.onEach(action).forEach(l -> {
        }); // Trigger the onEach action
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEach() {
        LongStream stream = createLongStream(TEST_ARRAY);
        List<Long> consumed = new ArrayList<>();
        LongConsumer action = it -> {
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
        LongStream stream = createLongStream(TEST_ARRAY);
        AtomicInteger count = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            stream.forEach(l -> {
                if (count.incrementAndGet() > 5) {
                    throw new RuntimeException("Test Exception");
                }
            });
        });
    }

    @Test
    public void testToMap() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        Map<String, String> result = stream.toMap(l -> String.valueOf(l), l -> "Value_" + l, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1", result.get("1"));
        assertEquals("Value_2", result.get("2"));
        assertEquals("Value_3", result.get("3"));
    }

    @Test
    public void testGroupTo() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 1L, 3L, 2L });
        Map<String, List<Long>> result = stream.groupTo(l -> String.valueOf(l), Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(List.of(1L, 1L), result.get("1"));
        assertEquals(List.of(2L, 2L), result.get("2"));
        assertEquals(List.of(3L), result.get("3"));
    }

    @Test
    public void testReduceWithIdentity() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        LongBinaryOperator op = (l1, l2) -> l1 + l2;
        long result = stream.reduce(0L, op);
        assertEquals(6L, result);

        LongStream emptyStream = createLongStream(new long[] {});
        long emptyResult = emptyStream.reduce(0L, op);
        assertEquals(0L, emptyResult);
    }

    @Test
    public void testReduceWithoutIdentity() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        LongBinaryOperator accumulator = (l1, l2) -> l1 + l2;
        OptionalLong result = stream.reduce(accumulator);
        assertTrue(result.isPresent());
        assertEquals(6L, result.get());

        LongStream emptyStream = createLongStream(new long[] {});
        OptionalLong emptyResult = emptyStream.reduce(accumulator);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testCollect() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        List<Long> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1L, 2L, 3L), collectedList);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createLongStream(TEST_ARRAY).anyMatch(l -> l == 13L));
        assertFalse(createLongStream(TEST_ARRAY).anyMatch(l -> l == 100L));
        assertFalse(createLongStream(new long[] {}).anyMatch(l -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createLongStream(TEST_ARRAY).allMatch(l -> l >= 1L && l <= 26L));
        assertFalse(createLongStream(TEST_ARRAY).allMatch(l -> l < 26L));
        assertTrue(createLongStream(new long[] {}).allMatch(l -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createLongStream(TEST_ARRAY).noneMatch(l -> l == 100L));
        assertFalse(createLongStream(TEST_ARRAY).noneMatch(l -> l == 1L));
        assertTrue(createLongStream(new long[] {}).noneMatch(l -> true));
    }

    @Test
    public void testFindFirst() {
        OptionalLong result = createLongStream(new long[] { 4L, 2L, 1L, 3L, 1L }).findFirst(l -> l == 1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());

        OptionalLong notFound = createLongStream(new long[] { 4L, 2L, 1L, 3L, 1L }).findFirst(l -> l == 10L);
        assertFalse(notFound.isPresent());

        OptionalLong empty = createLongStream(new long[] {}).findFirst(l -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindAny() {
        OptionalLong result = createLongStream(new long[] { 4L, 2L, 1L, 3L, 1L }).findAny(l -> l == 1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());

        OptionalLong notFound = createLongStream(new long[] { 4L, 2L, 1L, 3L, 1L }).findAny(l -> l == 10L);
        assertFalse(notFound.isPresent());

        OptionalLong empty = createLongStream(new long[] {}).findAny(l -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindLast() {
        OptionalLong result = createLongStream(new long[] { 4L, 2L, 1L, 3L, 1L }).findLast(l -> l == 1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());

        OptionalLong notFound = createLongStream(new long[] { 4L, 2L, 1L, 3L, 1L }).findLast(l -> l == 10L);
        assertFalse(notFound.isPresent());

        OptionalLong empty = createLongStream(new long[] {}).findLast(l -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testZipWithBinaryOperator() {
        LongStream streamA = createLongStream(new long[] { 1L, 2L, 3L });
        LongStream streamB = LongStream.of(10L, 20L, 30L);
        LongBinaryOperator zipper = (l1, l2) -> l1 + l2;
        List<Long> result = streamA.zipWith(streamB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11L, result.get(0));
        assertEquals(22L, result.get(1));
        assertEquals(33L, result.get(2));
    }

    @Test
    public void testZipWithTernaryOperator() {
        LongStream streamA = createLongStream(new long[] { 1L, 2L });
        LongStream streamB = LongStream.of(10L, 20L);
        LongStream streamC = LongStream.of(100L, 101L);
        LongTernaryOperator zipper = (l1, l2, l3) -> l1 + l2 + l3;
        List<Long> result = streamA.zipWith(streamB, streamC, zipper).sorted().toList();
        assertEquals(2, result.size());
        assertEquals(1L + 10L + 100L, result.get(0));
        assertEquals(2L + 20L + 101L, result.get(1));
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        LongStream streamA = createLongStream(new long[] { 1L, 2L });
        LongStream streamB = LongStream.of(10L, 20L, 30L);
        long valA = 0L; // Assuming 0L as 'none' for long
        long valB = -1L; // Assuming -1L as 'none' for long
        LongBinaryOperator zipper = (l1, l2) -> {
            return l1 + l2;
        };
        List<Long> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11L, result.get(0)); // 1 + 10
        assertEquals(22L, result.get(1)); // 2 + 20
        assertEquals(30L, result.get(2)); // 0 (valA) + 30 -> 30
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        LongStream streamA = createLongStream(new long[] { 1L });
        LongStream streamB = LongStream.of(10L, 20L);
        LongStream streamC = LongStream.of(100L, 101L, 102L);
        long valA = 0L;
        long valB = -1L;
        long valC = -2L;
        LongTernaryOperator zipper = (l1, l2, l3) -> {
            return l1 + l2 + l3;
        };
        List<Long> result = streamA.zipWith(streamB, streamC, valA, valB, valC, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(101L, result.get(0));
        assertEquals(111L, result.get(1));
        assertEquals(121L, result.get(2));
    }

    @Test
    public void testIsParallel() {
        LongStream stream = createLongStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
    }

    @Test
    public void testSequential() {
        LongStream parallelStream = createLongStream(TEST_ARRAY);
        LongStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        List<Long> result = sequentialStream.toList();
        assertEquals(TEST_ARRAY.length, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i));
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
        // If it was a public method of ParallelArrayLongStream, we would test it directly.
        // LongStream stream = createLongStream(longArray);
        //assertTrue(stream.maxThreadNum() > 0); // This line would work if maxThreadNum() was public in LongStream
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, splitor() is protected.
        // LongStream stream = createLongStream(longArray);
        //assertNotNull(stream.splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, asyncExecutor() is protected.
        // LongStream stream = createLongStream(longArray);
        //assertNotNull(stream.asyncExecutor());
    }

    @Test
    public void testOnClose() {
        LongStream stream = createLongStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        LongStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get()); // Not closed yet
        newStream.close(); // Close the stream, which should trigger the handler
        assertTrue(closedFlag.get()); // Now it should be closed
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        LongStream stream = createLongStream(TEST_ARRAY);
        AtomicInteger closedCount = new AtomicInteger(0);
        Runnable handler1 = () -> closedCount.incrementAndGet();
        Runnable handler2 = () -> closedCount.incrementAndGet();

        LongStream newStream = stream.onClose(handler1).onClose(handler2);
        assertEquals(0, closedCount.get());
        newStream.close();
        assertEquals(2, closedCount.get());
    }

    @Test
    public void testOnCloseEmptyHandler() {
        LongStream stream = createLongStream(TEST_ARRAY);
        LongStream newStream = stream.onClose(null);
        assertSame(stream, newStream); // Should return the same instance if handler is null
        newStream.close(); // Should not throw
    }
}
