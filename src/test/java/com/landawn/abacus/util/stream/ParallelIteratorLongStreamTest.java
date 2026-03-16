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
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongTernaryOperator;
import com.landawn.abacus.util.function.LongToFloatFunction;
import com.landawn.abacus.util.function.LongUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

@Tag("new-test")
public class ParallelIteratorLongStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final long[] TEST_ARRAY = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L,
            23L, 24L, 25L, 26L };
    private static final long[] DEFAULT_ARRAY = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L };

    private LongStream parallelStream;

    protected LongStream createLongStream(long... elements) {
        return LongStream.of(elements).map(e -> (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createLongStream(DEFAULT_ARRAY);
    }

    @Test
    public void testFilter() {
        LongStream stream = createLongStream(TEST_ARRAY);
        List<Long> result = stream.filter(l -> l > 20L).toList();
        assertEquals(6, result.size());
        for (Long l : result) {
            assertTrue(l > 20L);
        }

        LongStream emptyStream = createLongStream(new long[] {});
        List<Long> emptyResult = emptyStream.filter(l -> true).toList();
        assertEquals(0, emptyResult.size());
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
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L, 4L, 5L });
        List<Long> result = stream.dropWhile(l -> l < 4L).toList();
        assertHaveSameElements(Arrays.asList(4L, 5L), result);

        LongStream stream2 = createLongStream(new long[] { 1L, 2L, 3L });
        List<Long> result2 = stream2.dropWhile(l -> true).toList();
        assertEquals(0, result2.size());
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
    public void testMapToInt() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        List<Integer> result = stream.mapToInt(l -> (int) l * 10).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
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
    public void testMapToDouble() {
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L });
        List<Double> result = stream.mapToDouble(l -> (double) l).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(3.0));
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
        List<Integer> result = stream.flatMapToInt(l -> IntStream.of((int) l, (int) (l * 10))).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(10));
        assertTrue(result.contains(2));
        assertTrue(result.contains(20));
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
    public void testFlatMapToDouble() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        List<Double> result = stream.flatMapToDouble(l -> DoubleStream.of((double) l, (double) (l + 0.5))).toList();
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
    public void testFlatmapToObj() {
        LongStream stream = createLongStream(new long[] { 1L, 2L });
        List<String> result = stream.flatmapToObj(l -> Arrays.asList("X" + l, "Y" + l)).toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("X1", "Y1", "X2", "Y2")));
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
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L, 1L });
        Map<String, Long> result = stream.toMap(l -> "Key_" + l, l -> l, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(2L, (long) result.get("Key_1"));
        assertEquals(2L, (long) result.get("Key_2"));
        assertEquals(3L, (long) result.get("Key_3"));
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
        LongStream stream = createLongStream(new long[] { 1L, 2L, 3L, 4L, 5L });
        OptionalLong result = stream.reduce((l1, l2) -> l1 + l2);
        assertTrue(result.isPresent());
        assertEquals(15L, result.getAsLong());

        LongStream emptyStream = createLongStream(new long[] {});
        OptionalLong emptyResult = emptyStream.reduce((l1, l2) -> l1 + l2);
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
        assertTrue(createLongStream(TEST_ARRAY).anyMatch(l -> l == 26L));
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
        assertTrue(createLongStream(TEST_ARRAY).noneMatch(l -> l > 100L));
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
        LongStream stream = createLongStream(TEST_ARRAY);
        OptionalLong result = stream.findAny(l -> l == 13L);
        assertTrue(result.isPresent());
        assertEquals(13L, result.getAsLong());

        LongStream stream2 = createLongStream(TEST_ARRAY);
        OptionalLong notFound = stream2.findAny(l -> l == 100L);
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
    public void testZipWithTwoStreams() {
        LongStream streamA = createLongStream(new long[] { 1L, 2L, 3L });
        LongStream streamB = LongStream.of(10L, 20L, 30L);
        long[] result = streamA.zipWith(streamB, (a, b) -> a + b).toArray();
        assertHaveSameElements(new long[] { 11L, 22L, 33L }, result);
    }

    @Test
    public void testZipWithTwoStreamsWithDefaultValues() {
        LongStream streamA = createLongStream(new long[] { 1L, 2L, 3L });
        LongStream streamB = LongStream.of(10L);
        long[] result = streamA.zipWith(streamB, 0L, -1L, (a, b) -> a + b).toArray();
        assertHaveSameElements(new long[] { 11L, 1L, 2L }, result);
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
        stream.close();
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
        assertEquals(testMaxThreadNum, ((ParallelIteratorLongStream) createLongStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ITERATOR, ((ParallelIteratorLongStream) createLongStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelIteratorLongStream) createLongStream(TEST_ARRAY)).asyncExecutor() != null);
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
    @DisplayName("Test flatMapToObj method")
    public void testFlatMapToObj() {
        Stream<String> flattened = parallelStream.flatMapToObj(l -> Stream.of("A" + l, "B" + l));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B10"));
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
    @DisplayName("Test with specific long patterns")
    public void testSpecificLongPatterns() {
        parallelStream = createLongStream(new long[] { 1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L });
        long[] powersOfTwo = parallelStream.filter(l -> (l & (l - 1)) == 0).toArray();
        assertEquals(8, powersOfTwo.length);

        parallelStream = createLongStream(new long[] { 3L, 6L, 9L, 12L, 15L });
        boolean allDivisibleBy3 = parallelStream.allMatch(l -> l % 3 == 0);
        assertTrue(allDivisibleBy3);
    }

    // Covers delayed-match ordering for iterator-backed terminal operations.
    @Test
    public void testReduceAndFindOperations_DelayedMatchOrdering() {
        assertEquals(72L, createLongStream(21L, 2L, 4L, 7L, 6L, 11L, 8L, 13L).reduce(0L, Long::sum));

        OptionalLong reduced = createLongStream(21L, 2L, 4L).reduce(Long::sum);
        assertTrue(reduced.isPresent());
        assertEquals(27L, reduced.get());

        OptionalLong firstMatch = createLongStream(21L, 2L, 4L, 7L, 6L, 11L, 8L, 13L).findFirst(l -> {
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

        OptionalLong anyMatch = createLongStream(21L, 2L, 4L, 7L, 6L, 11L, 8L, 13L).findAny(l -> (l & 1L) == 1L && l > 5L);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21L || anyMatch.get() == 7L || anyMatch.get() == 11L || anyMatch.get() == 13L);

        OptionalLong lastMatch = createLongStream(21L, 2L, 4L, 7L, 6L, 11L, 8L, 13L).findLast(l -> {
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
}
