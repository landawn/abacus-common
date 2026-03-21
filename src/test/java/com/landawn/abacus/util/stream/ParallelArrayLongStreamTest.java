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

public class ParallelArrayLongStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final long[] TEST_ARRAY = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L,
            23L, 24L, 25L, 26L };
    private static final long[] DEFAULT_ARRAY = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L };

    private LongStream parallelStream;

    protected LongStream createLongStream(long... elements) {
        return LongStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    protected LongStream createIteratorSplitorLongStream(long... elements) {
        return new ParallelArrayLongStream(elements, 0, elements.length, false, testMaxThreadNum, Splitor.ITERATOR, null, false, new ArrayList<>());
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createLongStream(DEFAULT_ARRAY);
    }

    // Helper: creates a ParallelArrayLongStream with a single element so canBeSequential() returns true.
    protected LongStream createSingleElementLongStream(long value) {
        return new ParallelArrayLongStream(new long[] { value }, 0, 1, false, testMaxThreadNum, Splitor.ARRAY, null, false, new ArrayList<>());
    }

    // Covers the iterator-based terminal-operation branch in ParallelArrayLongStream.
    @Test
    public void testReduceAndFindMethods_IteratorSplitor() {
        assertEquals(15L, createIteratorSplitorLongStream(4L, 2L, 1L, 3L, 5L).reduce(0L, Long::sum));

        OptionalLong reduced = createIteratorSplitorLongStream(4L, 2L, 1L, 3L, 5L).reduce(Long::sum);
        assertTrue(reduced.isPresent());
        assertEquals(15L, reduced.get());

        OptionalLong firstOdd = createIteratorSplitorLongStream(4L, 2L, 1L, 3L, 5L).findFirst(l -> (l & 1L) == 1L);
        assertTrue(firstOdd.isPresent());
        assertEquals(1L, firstOdd.get());

        OptionalLong anyOdd = createIteratorSplitorLongStream(4L, 2L, 1L, 3L, 5L).findAny(l -> (l & 1L) == 1L);
        assertTrue(anyOdd.isPresent());
        assertTrue(anyOdd.get() == 1L || anyOdd.get() == 3L || anyOdd.get() == 5L);

        OptionalLong lastOdd = createIteratorSplitorLongStream(4L, 2L, 1L, 3L, 5L).findLast(l -> (l & 1L) == 1L);
        assertTrue(lastOdd.isPresent());
        assertEquals(5L, lastOdd.get());

        OptionalLong notFound = createIteratorSplitorLongStream(4L, 2L, 1L, 3L, 5L).findAny(l -> l > 10L);
        assertFalse(notFound.isPresent());
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
    @DisplayName("Test complex parallel operations")
    public void testComplexParallelOperations() {
        long[] result = parallelStream.filter(l -> l % 2 == 0).map(l -> l * 2).sorted().toArray();

        assertArrayEquals(new long[] { 4L, 8L, 12L, 16L, 20L }, result);

        parallelStream = createLongStream(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L });
        long sum = parallelStream.map(l -> l * 2).reduce(0L, Long::sum);

        assertEquals(110L, sum);
    }

    @Test
    @DisplayName("Test with specific long patterns")
    public void testSpecificLongPatterns() {
        parallelStream = createLongStream(new long[] { 1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L });
        long[] powersOfTwo = parallelStream.filter(l -> (l & (l - 1)) == 0).toArray();
        assertEquals(8, powersOfTwo.length);

        parallelStream = createLongStream(new long[] { 3L, 6L, 9L, 12L, 15L });
        boolean allDivisibleBy3 = parallelStream.allMatch(l -> l % 3 == 0);
        assertTrue(allDivisibleBy3);
    }

    @Test
    @DisplayName("Test edge cases")
    public void testEdgeCases() {
        parallelStream = createLongStream(new long[0]);
        assertEquals(0, parallelStream.count());

        parallelStream = createLongStream(new long[] { 42L });
        assertEquals(42L, parallelStream.first().getAsLong());

        parallelStream = createLongStream(new long[] { -5L, -3L, -1L, 0L, 1L, 3L, 5L });
        long positiveCount = parallelStream.filter(l -> l > 0).count();
        assertEquals(3, positiveCount);

        parallelStream = createLongStream(new long[] { Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE });
        assertEquals(5, parallelStream.count());
    }

    // Sequential-fallback branch tests (maxThreadNum > 1 but array has <= 1 element)

    @Test
    public void testFilter_SequentialFallback() {
        List<Long> result = createSingleElementLongStream(5L).filter(l -> l > 3L).toList();
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0));

        List<Long> empty = createSingleElementLongStream(1L).filter(l -> l > 3L).toList();
        assertEquals(0, empty.size());
    }

    @Test
    @DisplayName("Test exception handling in parallel operations")
    public void testExceptionHandling() {
        assertThrows(RuntimeException.class, () -> {
            parallelStream.filter(l -> {
                if (l == 5L)
                    throw new RuntimeException("Test exception in filter");
                return true;
            }).count();
        });

        parallelStream = createLongStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.map(l -> {
                if (l == 7L)
                    throw new RuntimeException("Test exception in map");
                return l;
            }).toArray();
        });
    }

    @Test
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(l -> l > 5).count();
        });
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
    public void testTakeWhile_SequentialFallback() {
        List<Long> result = createSingleElementLongStream(2L).takeWhile(l -> l < 5L).toList();
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0));
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
    public void testDropWhile_SequentialFallback() {
        List<Long> result = createSingleElementLongStream(2L).dropWhile(l -> l < 5L).toList();
        assertEquals(0, result.size());

        List<Long> kept = createSingleElementLongStream(10L).dropWhile(l -> l < 5L).toList();
        assertEquals(1, kept.size());
        assertEquals(10L, kept.get(0));
    }

    @Test
    public void testMap() {
        LongStream stream = createLongStream(TEST_ARRAY);
        LongUnaryOperator mapper = l -> l + 1L;
        List<Long> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2L));
        assertFalse(result.contains(1L));
    }

    @Test
    public void testMap_SequentialFallback() {
        List<Long> result = createSingleElementLongStream(3L).map(l -> l * 2L).toList();
        assertEquals(1, result.size());
        assertEquals(6L, result.get(0));
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
    public void testMapToInt_empty() {
        parallelStream = createLongStream(new long[] {});
        int[] result = parallelStream.mapToInt(l -> (int) l).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToInt_SequentialFallback() {
        List<Integer> result = createSingleElementLongStream(7L).mapToInt(l -> (int) l).toList();
        assertEquals(1, result.size());
        assertEquals(7, result.get(0));
    }

    @Test
    public void testMapToFloat() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        LongToFloatFunction mapper = l -> l * 0.5f;
        List<Float> result = stream.mapToFloat(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(0.5f));
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
    }

    @Test
    public void testMapToFloat_empty() {
        parallelStream = createLongStream(new long[] {});
        float[] result = parallelStream.mapToFloat(l -> (float) l).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToFloat_SequentialFallback() {
        List<Float> result = createSingleElementLongStream(4L).mapToFloat(l -> (float) l * 0.5f).toList();
        assertEquals(1, result.size());
        assertEquals(2.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testMapToDouble() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        LongToDoubleFunction mapper = l -> l * 0.5;
        List<Double> result = stream.mapToDouble(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(0.5));
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
    }

    @Test
    public void testMapToDouble_empty() {
        parallelStream = createLongStream(new long[] {});
        double[] result = parallelStream.mapToDouble(l -> (double) l).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToDouble_SequentialFallback() {
        List<Double> result = createSingleElementLongStream(3L).mapToDouble(l -> (double) l * 2.0).toList();
        assertEquals(1, result.size());
        assertEquals(6.0, result.get(0), 0.0001);
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
    public void testMapToObj_SequentialFallback() {
        List<String> result = createSingleElementLongStream(9L).mapToObj(l -> "v" + l).toList();
        assertEquals(1, result.size());
        assertEquals("v9", result.get(0));
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
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        LongStream flattened = parallelStream.flatmap(l -> new long[] { l, l * 2 });
        long[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createLongStream(new long[] { 1L, 2L, 3L });
        flattened = parallelStream.flatmap(l -> l % 2 == 0 ? new long[] { l } : new long[0]);
        assertArrayEquals(new long[] { 2L }, flattened.toArray());
    }

    // Tests covering parallel code paths with large array

    @Test
    public void testFlatMap_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).flatMap(l -> LongStream.of(l, l * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmap_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).flatmap(l -> new long[] { l, l * 10 }).count();
        assertEquals(52, count);
    }

    @Test
    @DisplayName("Test flatMap method")
    public void testFlatMap() {
        LongStream flattened = parallelStream.flatMap(l -> LongStream.of(l, l + 10));
        long[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createLongStream(new long[] { 1L, 2L, 3L });
        flattened = parallelStream.flatMap(l -> l % 2 == 0 ? LongStream.of(l) : LongStream.empty());
        assertArrayEquals(new long[] { 2L }, flattened.toArray());
    }

    @Test
    public void testFlatMap_SequentialFallback() {
        List<Long> result = createSingleElementLongStream(2L).flatMap(l -> LongStream.of(l, l + 1L)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
    }

    @Test
    public void testFlatmapArray_SequentialFallback() {
        List<Long> result = createSingleElementLongStream(2L).flatmap(l -> new long[] { l, l * 2L }).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(2L));
        assertTrue(result.contains(4L));
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
    public void testFlatMapToInt_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).flatMapToInt(l -> IntStream.of((int) l)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToInt_SequentialFallback() {
        List<Integer> result = createSingleElementLongStream(3L).flatMapToInt(l -> IntStream.of((int) l, (int) l + 1)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(3));
        assertTrue(result.contains(4));
    }

    @Test
    public void testFlatMapToFloat() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<FloatStream> mapper = l -> FloatStream.of(l, (float) (l + 0.5));
        List<Float> result = stream.flatMapToFloat(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    public void testFlatMapToFloat_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).flatMapToFloat(l -> FloatStream.of((float) l)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToFloat_SequentialFallback() {
        List<Float> result = createSingleElementLongStream(5L).flatMapToFloat(l -> FloatStream.of((float) l, (float) l * 2.0f)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(5.0f));
        assertTrue(result.contains(10.0f));
    }

    @Test
    public void testFlatMapToDouble() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        LongFunction<DoubleStream> mapper = l -> DoubleStream.of(l, l + 0.5);
        List<Double> result = stream.flatMapToDouble(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
    }

    @Test
    public void testFlatMapToDouble_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).flatMapToDouble(l -> DoubleStream.of((double) l)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToDouble_SequentialFallback() {
        List<Double> result = createSingleElementLongStream(4L).flatMapToDouble(l -> DoubleStream.of((double) l, (double) l * 1.5)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(4.0));
        assertTrue(result.contains(6.0));
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
    @DisplayName("Test flatMapToObj method")
    public void testFlatMapToObj() {
        Stream<String> flattened = parallelStream.flatMapToObj(l -> Stream.of("A" + l, "B" + l));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B10"));
    }

    @Test
    @DisplayName("Test flatmapToObj with collection method")
    public void testFlatmapToObjCollection() {
        Stream<Long> flattened = parallelStream.flatmapToObj(l -> Arrays.asList(Long.valueOf(l), Long.valueOf(l * 10)));
        List<Long> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertEquals(Long.valueOf(1L), result.get(0));
        assertEquals(Long.valueOf(100L), result.get(19));
    }

    @Test
    public void testFlatMapToObj_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).flatMapToObj(l -> Stream.of(l, l * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmapToObj_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).flatmapToObj(l -> Arrays.asList(l, l * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatMapToObj_SequentialFallback() {
        List<String> result = createSingleElementLongStream(1L).flatMapToObj(l -> Stream.of("A" + l, "B" + l)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B1"));
    }

    @Test
    public void testFlatmapToObj_SequentialFallback() {
        List<String> result = createSingleElementLongStream(2L).flatmapToObj(l -> java.util.Arrays.asList("X" + l, "Y" + l)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("X2"));
        assertTrue(result.contains("Y2"));
    }

    @Test
    public void testOnEach() {
        LongStream stream = createLongStream(TEST_ARRAY);
        List<Long> consumed = new ArrayList<>();
        LongConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.peek(action).forEach(l -> {
        });
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testOnEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleElementLongStream(7L).peek(l -> count.incrementAndGet()).forEach(l -> {
        });
        assertEquals(1, count.get());
    }

    @Test
    public void testForEach_IteratorSplitor() {
        java.util.concurrent.atomic.AtomicLong sum = new java.util.concurrent.atomic.AtomicLong(0);
        createIteratorSplitorLongStream(1L, 2L, 3L, 4L, 5L).forEach(l -> sum.addAndGet(l));
        assertEquals(15L, sum.get());
    }

    @Test
    public void testForEach_ParallelPath() {
        AtomicInteger count = new AtomicInteger(0);
        createLongStream(TEST_ARRAY).forEach(l -> count.incrementAndGet());
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testForEach() {
        LongStream stream = createLongStream(TEST_ARRAY);
        List<Long> consumed = new ArrayList<>();
        LongConsumer action = it -> {
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
        createSingleElementLongStream(3L).forEach(l -> count.incrementAndGet());
        assertEquals(1, count.get());
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
    @DisplayName("Test thread safety")
    public void testThreadSafety() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        parallelStream.forEach(l -> {
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
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        Map<String, String> result = stream.toMap(l -> String.valueOf(l), l -> "Value_" + l, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1", result.get("1"));
        assertEquals("Value_2", result.get("2"));
        assertEquals("Value_3", result.get("3"));
    }

    @Test
    public void testToMap_ParallelPath() {
        Map<Long, Long> result = createLongStream(TEST_ARRAY).toMap(l -> l, l -> l * 2, (a, b) -> a, java.util.HashMap::new);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testToMap_SequentialFallback() {
        Map<String, Long> result = createSingleElementLongStream(5L).toMap(l -> "k" + l, l -> l, (a, b) -> a, java.util.HashMap::new);
        assertEquals(1, result.size());
        assertEquals(5L, result.get("k5"));
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
    public void testGroupTo_ParallelPath() {
        Map<Boolean, List<Long>> result = createLongStream(TEST_ARRAY).groupTo(l -> l < 14L, Collectors.toList(), java.util.HashMap::new);
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupTo_SequentialFallback() {
        Map<Boolean, List<Long>> result = createSingleElementLongStream(4L).groupTo(l -> l > 3L, Collectors.toList(), java.util.HashMap::new);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(true));
        assertEquals(1, result.get(true).size());
    }

    @Test
    @DisplayName("Test with large arrays")
    public void testLargeArrays() {
        long[] largeArray = new long[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i + 1L;
        }

        parallelStream = createLongStream(largeArray);

        long sum = parallelStream.reduce(0L, Long::sum);
        assertEquals(500500L, sum);

        parallelStream = createLongStream(largeArray);
        long evenCount = parallelStream.filter(l -> l % 2 == 0).count();
        assertEquals(500, evenCount);
    }

    @Test
    public void testReduce_WithIdentity_ParallelPath() {
        long result = createLongStream(TEST_ARRAY).reduce(0L, Long::sum);
        long expected = 0;
        for (long v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, result);
    }

    @Test
    public void testReduce_NoIdentity_ParallelPath() {
        OptionalLong result = createLongStream(TEST_ARRAY).reduce(Long::sum);
        assertTrue(result.isPresent());
    }

    @Test
    public void testReduceWithIdentity() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        LongBinaryOperator op = (l1, l2) -> l1 + l2;
        long result = stream.reduce(0L, op);
        assertEquals(6L, result);

        LongStream emptyStream = createLongStream(new long[] {});
        long emptyResult = emptyStream.reduce(100L, op);
        assertEquals(100L, emptyResult);
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
    public void testReduce_WithIdentity_SequentialFallback() {
        long result = createSingleElementLongStream(7L).reduce(0L, Long::sum);
        assertEquals(7L, result);
    }

    @Test
    public void testReduce_NoIdentity_SequentialFallback() {
        OptionalLong result = createSingleElementLongStream(9L).reduce(Long::sum);
        assertTrue(result.isPresent());
        assertEquals(9L, result.get());
    }

    @Test
    @DisplayName("Test overflow scenarios")
    public void testOverflowScenarios() {
        parallelStream = createLongStream(new long[] { Long.MAX_VALUE - 1, 1L, 1L });

        try {
            long sum = parallelStream.reduce(0L, Long::sum);
            assertTrue(sum < 0 || sum == Long.MAX_VALUE);
        } catch (ArithmeticException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testIteratorSplitorReduceAndFindOperations_SparseMatch() {
        assertEquals(72L, createIteratorSplitorLongStream(21L, 2L, 4L, 7L, 6L, 11L, 8L, 13L).reduce(0L, Long::sum));

        OptionalLong reduced = createIteratorSplitorLongStream(21L, 2L, 4L).reduce(Long::sum);
        assertTrue(reduced.isPresent());
        assertEquals(27L, reduced.get());

        OptionalLong firstMatch = createIteratorSplitorLongStream(21L, 2L, 4L, 7L, 6L, 11L, 8L, 13L).findFirst(l -> {
            if (l == 21L) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return (l & 1L) == 1L && l > 5L;
        });
        assertTrue(firstMatch.isPresent());
        assertEquals(21L, firstMatch.get());

        OptionalLong anyMatch = createIteratorSplitorLongStream(21L, 2L, 4L, 7L, 6L, 11L, 8L, 13L).findAny(l -> (l & 1L) == 1L && l > 5L);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21L || anyMatch.get() == 7L || anyMatch.get() == 11L || anyMatch.get() == 13L);

        OptionalLong lastMatch = createIteratorSplitorLongStream(21L, 2L, 4L, 7L, 6L, 11L, 8L, 13L).findLast(l -> {
            if (l == 13L) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return (l & 1L) == 1L && l > 5L;
        });
        assertTrue(lastMatch.isPresent());
        assertEquals(13L, lastMatch.get());
    }

    @Test
    public void testCollect() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        List<Long> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(List.of(1L, 2L, 3L), collectedList);
    }

    @Test
    public void testCollect_ParallelPath() {
        List<Long> result = createLongStream(TEST_ARRAY).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testCollect_IteratorSplitor() {
        List<Long> result = createIteratorSplitorLongStream(1L, 2L, 3L).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1L, 2L, 3L), result);
    }

    @Test
    public void testCollect_SequentialFallback() {
        List<Long> result = createSingleElementLongStream(3L).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createLongStream(TEST_ARRAY).anyMatch(l -> l == 13L));
        assertFalse(createLongStream(TEST_ARRAY).anyMatch(l -> l == 100L));
        assertFalse(createLongStream(new long[] {}).anyMatch(l -> true));
    }

    @Test
    public void testAnyMatch_SequentialFallback() {
        assertTrue(createSingleElementLongStream(5L).anyMatch(l -> l == 5L));
        assertFalse(createSingleElementLongStream(5L).anyMatch(l -> l == 6L));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createLongStream(TEST_ARRAY).allMatch(l -> l >= 1L && l <= 26L));
        assertFalse(createLongStream(TEST_ARRAY).allMatch(l -> l < 26L));
        assertTrue(createLongStream(new long[] {}).allMatch(l -> false));
    }

    @Test
    public void testAllMatch_SequentialFallback() {
        assertTrue(createSingleElementLongStream(5L).allMatch(l -> l > 0L));
        assertFalse(createSingleElementLongStream(5L).allMatch(l -> l > 10L));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createLongStream(TEST_ARRAY).noneMatch(l -> l == 100L));
        assertFalse(createLongStream(TEST_ARRAY).noneMatch(l -> l == 1L));
        assertTrue(createLongStream(new long[] {}).noneMatch(l -> true));
    }

    // Cover the ITERATOR splitor path for anyMatch / allMatch / noneMatch / collect / forEach
    @Test
    public void testAnyMatchAllMatchNoneMatch_IteratorSplitor() {
        assertTrue(createIteratorSplitorLongStream(1L, 2L, 3L, 4L, 5L).anyMatch(l -> l > 4L));
        assertFalse(createIteratorSplitorLongStream(1L, 2L, 3L).anyMatch(l -> l > 10L));

        assertTrue(createIteratorSplitorLongStream(1L, 2L, 3L).allMatch(l -> l >= 1L && l <= 3L));
        assertFalse(createIteratorSplitorLongStream(1L, 2L, 3L).allMatch(l -> l < 3L));

        assertTrue(createIteratorSplitorLongStream(1L, 2L, 3L).noneMatch(l -> l > 10L));
        assertFalse(createIteratorSplitorLongStream(1L, 2L, 3L).noneMatch(l -> l > 2L));
    }

    @Test
    public void testNoneMatch_SequentialFallback() {
        assertTrue(createSingleElementLongStream(5L).noneMatch(l -> l > 10L));
        assertFalse(createSingleElementLongStream(5L).noneMatch(l -> l > 0L));
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
    public void testFindFirst_SequentialFallback() {
        OptionalLong found = createSingleElementLongStream(5L).findFirst(l -> l > 0L);
        assertTrue(found.isPresent());
        assertEquals(5L, found.get());

        OptionalLong notFound = createSingleElementLongStream(5L).findFirst(l -> l > 10L);
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalLong result = parallelStream.findFirst(l -> l > 5);

        assertTrue(result.isPresent());
        assertEquals(6L, result.getAsLong());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findFirst(l -> l > 30);
        assertFalse(result.isPresent());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findFirst(l -> l == 1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.getAsLong());
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
    public void testFindAny_SequentialFallback() {
        OptionalLong found = createSingleElementLongStream(3L).findAny(l -> l < 10L);
        assertTrue(found.isPresent());
        assertEquals(3L, found.get());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalLong result = parallelStream.findAny(l -> l > 5);

        assertTrue(result.isPresent());
        assertTrue(result.getAsLong() > 5);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findAny(l -> l > 30);
        assertFalse(result.isPresent());

        parallelStream = createLongStream(new long[] { 5L });
        result = parallelStream.findAny(l -> l == 5L);
        assertTrue(result.isPresent());
        assertEquals(5L, result.getAsLong());
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
    public void testFindLast_SequentialFallback() {
        OptionalLong found = createSingleElementLongStream(8L).findLast(l -> l > 0L);
        assertTrue(found.isPresent());
        assertEquals(8L, found.get());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalLong result = parallelStream.findLast(l -> l < 5);

        assertTrue(result.isPresent());
        assertEquals(4L, result.getAsLong());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findLast(l -> l > 30);
        assertFalse(result.isPresent());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findLast(l -> l == 10L);
        assertTrue(result.isPresent());
        assertEquals(10L, result.getAsLong());
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
    @DisplayName("Test zipWith two streams with default values")
    public void testZipWithBinaryDefaults() {
        LongStream stream2 = createLongStream(new long[] { 10L, 20L, 30L });
        long[] result = parallelStream.zipWith(stream2, 0L, 100L, (a, b) -> a + b).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(11L, result[0]);
        assertEquals(22L, result[1]);
        assertEquals(33L, result[2]);
        assertEquals(104L, result[3]);
    }

    @Test
    @DisplayName("Test zipWith three streams with default values")
    public void testZipWithTernaryDefaults() {
        LongStream stream2 = createLongStream(new long[] { 10L, 20L });
        LongStream stream3 = createLongStream(new long[] { 1L, 2L, 3L, 4L });
        long[] result = parallelStream.zipWith(stream2, stream3, 0L, 0L, 1L, (a, b, c) -> a + b + c).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(6L, result[0]);
        assertEquals(6L, result[1]);
        assertEquals(24L, result[9]);
    }

    @Test
    public void testZipWithBinary_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).zipWith(LongStream.of(TEST_ARRAY), (a, b) -> a + b).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernary_ParallelPath() {
        long count = createLongStream(TEST_ARRAY).zipWith(LongStream.of(TEST_ARRAY), LongStream.of(TEST_ARRAY), (a, b, c) -> a + b + c).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryDefaults_ParallelPath() {
        long[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createLongStream(TEST_ARRAY).zipWith(LongStream.of(shorter), 0L, -1L, (a, b) -> a + b).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernaryDefaults_ParallelPath() {
        long[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createLongStream(TEST_ARRAY).zipWith(LongStream.of(shorter), LongStream.of(shorter), 0L, -1L, -2L, (a, b, c) -> a + b + c).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        LongStream streamA = createLongStream(new long[] { 1L, 2L });
        LongStream streamB = LongStream.of(10L, 20L, 30L);
        long valA = 0L;
        long valB = -1L;
        LongBinaryOperator zipper = (l1, l2) -> {
            return l1 + l2;
        };
        List<Long> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11L, result.get(0));
        assertEquals(22L, result.get(1));
        assertEquals(30L, result.get(2));
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
        List<Long> result = streamA.zipWith(streamB, streamC, valA, valB, valC, zipper).toList();
        assertEquals(3, result.size());
        assertEquals(1L + 10L + 100L, result.get(0));
        assertEquals(0L + 20L + 101L, result.get(1));
        assertEquals(0L + -1L + 102L, result.get(2));
    }

    @Test
    @DisplayName("Test zipWith two streams with binary operator")
    public void testZipWithBinary() {
        LongStream stream2 = createLongStream(new long[] { 10L, 20L, 30L, 40L, 50L });
        LongStream zipped = parallelStream.zipWith(stream2, (a, b) -> a + b);
        long[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new long[] { 11L, 22L, 33L, 44L, 55L }, result);

        parallelStream = createLongStream(new long[] { 1L, 2L, 3L });
        stream2 = createLongStream(new long[] { 10L, 20L, 30L, 40L, 50L });
        zipped = parallelStream.zipWith(stream2, (a, b) -> a * b);
        assertEquals(3, zipped.count());
    }

    @Test
    @DisplayName("Test zipWith three streams with ternary operator")
    public void testZipWithTernary() {
        LongStream stream2 = createLongStream(new long[] { 10L, 20L, 30L, 40L, 50L });
        LongStream stream3 = createLongStream(new long[] { 100L, 100L, 100L, 100L, 100L });
        LongStream zipped = parallelStream.zipWith(stream2, stream3, (a, b, c) -> (a + b) * c / 100);
        long[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new long[] { 11L, 22L, 33L, 44L, 55L }, result);
    }

    @Test
    public void testZipWith_SequentialFallback() {
        LongStream streamA = createSingleElementLongStream(3L);
        LongStream streamB = LongStream.of(7L);
        List<Long> result = streamA.zipWith(streamB, (a, b) -> a + b).toList();
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0));
    }

    @Test
    public void testZipWithTernary_SequentialFallback() {
        LongStream streamA = createSingleElementLongStream(1L);
        LongStream streamB = LongStream.of(2L);
        LongStream streamC = LongStream.of(3L);
        List<Long> result = streamA.zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList();
        assertEquals(1, result.size());
        assertEquals(6L, result.get(0));
    }

    @Test
    public void testZipWithDefaults_SequentialFallback() {
        LongStream streamA = createSingleElementLongStream(5L);
        LongStream streamB = LongStream.of(10L, 20L);
        List<Long> result = streamA.zipWith(streamB, 0L, 0L, (a, b) -> a + b).sorted().toList();
        assertEquals(2, result.size());
        assertEquals(15L, result.get(0));
        assertEquals(20L, result.get(1));
    }

    @Test
    public void testZipWithTernaryDefaults_SequentialFallback() {
        LongStream streamA = createSingleElementLongStream(1L);
        LongStream streamB = LongStream.of(2L, 3L);
        LongStream streamC = LongStream.of(4L, 5L, 6L);
        List<Long> result = streamA.zipWith(streamB, streamC, 0L, 0L, 0L, (a, b, c) -> a + b + c).sorted().toList();
        // sorted ascending: [6, 7, 8]
        assertEquals(3, result.size());
        assertEquals(6L, result.get(0));
    }

    @Test
    public void testZipWithBinaryDefaults_SequentialFallback_UnevenLengths() {
        List<Long> result = LongStream.of(1L, 2L, 3L)
                .parallel(PS.create(Splitor.ARRAY).maxThreadNum(1))
                .zipWith(LongStream.of(10L), 0L, -1L, Long::sum)
                .toList();

        assertEquals(Arrays.asList(11L, 1L, 2L), result);
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
    public void testSequential_ParallelPath() {
        LongStream parallel = createLongStream(TEST_ARRAY);
        assertTrue(parallel.isParallel());
        LongStream seq = parallel.sequential();
        assertFalse(seq.isParallel());
        assertEquals(TEST_ARRAY.length, seq.count());
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelArrayLongStream) createLongStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ARRAY, ((ParallelArrayLongStream) createLongStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelArrayLongStream) createLongStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    @DisplayName("Test method chaining")
    public void testMethodChaining() {
        List<String> result = parallelStream.filter(l -> l > 3)
                .map(l -> l * 2)
                .flatMapToObj(l -> Stream.of("Value: " + l, "Half: " + (l / 2)))
                .sorted()
                .toList();

        assertTrue(result.size() > 0);
        assertTrue(result.contains("Value: 8"));
        assertTrue(result.contains("Half: 4"));
    }

    @Test
    public void testOnClose() {
        LongStream stream = createLongStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        LongStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get());
        newStream.close();
        assertTrue(closedFlag.get());
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
        assertSame(stream, newStream);
        newStream.close();
    }

    @Test
    public void testBoundaryValues() {
        long[] boundaryArray = new long[] { Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, Long.MAX_VALUE - 1, Long.MAX_VALUE };
        parallelStream = createLongStream(boundaryArray);
        long[] result = parallelStream.sorted().toArray();
        assertEquals(7, result.length);
        assertEquals(Long.MIN_VALUE, result[0]);
        assertEquals(Long.MAX_VALUE, result[6]);
    }
}
