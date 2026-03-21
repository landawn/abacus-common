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
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortTernaryOperator;
import com.landawn.abacus.util.function.ShortToIntFunction;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelArrayShortStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final short[] TEST_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };
    private static final short[] DEFAULT_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private static final short[] SMALL_ARRAY = { 1, 2, 3, 4, 5 };

    private ShortStream parallelStream;
    private ShortStream emptyParallelStream;
    private short[] largeArray;
    private short[] smallArray;

    protected ShortStream createShortStream(short... elements) {
        return ShortStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    protected ShortStream createIteratorSplitorShortStream(short... elements) {
        return new ParallelArrayShortStream(elements, 0, elements.length, false, testMaxThreadNum, Splitor.ITERATOR, null, false, new ArrayList<>());
    }

    @BeforeEach
    public void setUp() {
        largeArray = DEFAULT_ARRAY.clone();
        smallArray = SMALL_ARRAY.clone();
        parallelStream = createShortStream(largeArray);
        emptyParallelStream = createShortStream();
    }

    // Covers the iterator-based terminal-operation branch in ParallelArrayShortStream.
    @Test
    public void testReduceAndFindMethods_IteratorSplitor() {
        assertEquals((short) 15, createIteratorSplitorShortStream((short) 4, (short) 2, (short) 1, (short) 3, (short) 5).reduce((short) 0,
                (left, right) -> (short) (left + right)));

        OptionalShort reduced = createIteratorSplitorShortStream((short) 4, (short) 2, (short) 1, (short) 3, (short) 5)
                .reduce((left, right) -> (short) (left + right));
        assertTrue(reduced.isPresent());
        assertEquals((short) 15, reduced.get());

        OptionalShort firstOdd = createIteratorSplitorShortStream((short) 4, (short) 2, (short) 1, (short) 3, (short) 5).findFirst(s -> (s & 1) == 1);
        assertTrue(firstOdd.isPresent());
        assertEquals((short) 1, firstOdd.get());

        OptionalShort anyOdd = createIteratorSplitorShortStream((short) 4, (short) 2, (short) 1, (short) 3, (short) 5).findAny(s -> (s & 1) == 1);
        assertTrue(anyOdd.isPresent());
        assertTrue(anyOdd.get() == (short) 1 || anyOdd.get() == (short) 3 || anyOdd.get() == (short) 5);

        OptionalShort lastOdd = createIteratorSplitorShortStream((short) 4, (short) 2, (short) 1, (short) 3, (short) 5).findLast(s -> (s & 1) == 1);
        assertTrue(lastOdd.isPresent());
        assertEquals((short) 5, lastOdd.get());

        OptionalShort notFound = createIteratorSplitorShortStream((short) 4, (short) 2, (short) 1, (short) 3, (short) 5).findAny(s -> s > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFilter() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortPredicate predicate = s -> s > 13;
        List<Short> result = stream.filter(predicate).toList();
        assertEquals(13, result.size());
        assertTrue(result.contains((short) 14));
        assertFalse(result.contains((short) 1));
    }

    @Test
    @DisplayName("Test complex parallel operations")
    public void testComplexParallelOperations() {
        short[] result = parallelStream.filter(s -> s % 2 == 0).map(s -> (short) (s * 2)).sorted().toArray();

        assertArrayEquals(new short[] { 4, 8, 12, 16, 20 }, result);

        parallelStream = createShortStream(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        int sum = parallelStream.mapToInt(s -> s).reduce(0, Integer::sum);

        assertEquals(55, sum);
    }

    @Test
    @DisplayName("Test with specific short patterns")
    public void testSpecificShortPatterns() {
        parallelStream = createShortStream(new short[] { 1, 2, 4, 8, 16, 32, 64, 128 });
        short[] powersOfTwo = parallelStream.filter(s -> (s & (s - 1)) == 0).toArray();
        assertEquals(8, powersOfTwo.length);

        parallelStream = createShortStream(new short[] { 3, 6, 9, 12, 15 });
        boolean allDivisibleBy3 = parallelStream.allMatch(s -> s % 3 == 0);
        assertTrue(allDivisibleBy3);
    }

    @Test
    @DisplayName("Test edge cases")
    public void testEdgeCases() {
        parallelStream = createShortStream(new short[0]);
        assertEquals(0, parallelStream.count());

        parallelStream = createShortStream(new short[] { 42 });
        assertEquals(42, parallelStream.first().get());

        parallelStream = createShortStream(new short[] { -5, -3, -1, 0, 1, 3, 5 });
        long positiveCount = parallelStream.filter(s -> s > 0).count();
        assertEquals(3, positiveCount);

        parallelStream = createShortStream(new short[] { Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE });
        assertEquals(5, parallelStream.count());
    }

    @Test
    public void testFilter_empty() {
        parallelStream = createShortStream(new short[] {});
        short[] result = parallelStream.filter(s -> s > 0).toArray();
        assertEquals(0, result.length);
    }

    // ---- Sequential-fallback path tests (single-element array => canBeSequential == true) ----

    @Test
    public void testFilter_SequentialFallback() {
        List<Short> result = createShortStream(new short[] { (short) 5 }).filter(s -> s == 5).toList();
        assertEquals(1, result.size());
        assertEquals((short) 5, result.get(0));

        List<Short> empty = createShortStream(new short[] { (short) 5 }).filter(s -> s > 10).toList();
        assertEquals(0, empty.size());
    }

    @Test
    @DisplayName("Test exception handling in parallel operations")
    public void testExceptionHandling() {
        assertThrows(RuntimeException.class, () -> {
            parallelStream.filter(s -> {
                if (s == 5)
                    throw new RuntimeException("Test exception in filter");
                return true;
            }).count();
        });

        parallelStream = createShortStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.map(s -> {
                if (s == 7)
                    throw new RuntimeException("Test exception in map");
                return s;
            }).toArray();
        });
    }

    @Test
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(s -> s > 5).count();
        });
    }

    @Test
    public void testTakeWhile() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortPredicate predicate = s -> s < 4;
        List<Short> result = stream.takeWhile(predicate).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains((short) 1));
        assertTrue(result.contains((short) 2));
        assertTrue(result.contains((short) 3));
        assertFalse(result.contains((short) 4));
    }

    @Test
    public void testTakeWhile_SequentialFallback() {
        List<Short> result = createShortStream(new short[] { (short) 1 }).takeWhile(s -> s < 10).toList();
        assertEquals(1, result.size());
        assertEquals((short) 1, result.get(0));

        List<Short> empty = createShortStream(new short[] { (short) 1 }).takeWhile(s -> s > 10).toList();
        assertEquals(0, empty.size());
    }

    @Test
    public void testDropWhile() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortPredicate predicate = s -> s < 4;
        List<Short> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains((short) 4));
        assertFalse(result.contains((short) 3));
    }

    @Test
    public void testDropWhile_SequentialFallback() {
        List<Short> result = createShortStream(new short[] { (short) 1 }).dropWhile(s -> s > 10).toList();
        assertEquals(1, result.size());
        assertEquals((short) 1, result.get(0));

        List<Short> empty = createShortStream(new short[] { (short) 1 }).dropWhile(s -> s < 10).toList();
        assertEquals(0, empty.size());
    }

    @Test
    public void testMap() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortUnaryOperator mapper = s -> (short) (s + 1);
        List<Short> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains((short) 2));
        assertFalse(result.contains((short) 1));
    }

    @Test
    public void testMap_SequentialFallback() {
        List<Short> result = createShortStream(new short[] { (short) 5 }).map(s -> (short) (s + 1)).toList();
        assertEquals(1, result.size());
        assertEquals((short) 6, result.get(0));
    }

    @Test
    public void testMapToInt() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortToIntFunction mapper = s -> s * 2;
        List<Integer> result = stream.mapToInt(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2));
        assertTrue(result.contains(52));
    }

    @Test
    @DisplayName("Test with large arrays")
    public void testLargeArrays() {
        short[] largeArray = new short[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (short) (i + 1);
        }

        parallelStream = createShortStream(largeArray);

        long sum = parallelStream.mapToInt(s -> s).sum();
        assertEquals(500500L, sum);

        parallelStream = createShortStream(largeArray);
        long evenCount = parallelStream.filter(s -> s % 2 == 0).count();
        assertEquals(500, evenCount);
    }

    @Test
    public void testMapToInt_SequentialFallback() {
        List<Integer> result = createShortStream(new short[] { (short) 5 }).mapToInt(s -> s * 2).toList();
        assertEquals(1, result.size());
        assertEquals(10, result.get(0));
    }

    @Test
    public void testMapToInt_empty() {
        parallelStream = createShortStream(new short[] {});
        int[] result = parallelStream.mapToInt(s -> s).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToObj() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortFunction<String> mapper = s -> "Short_" + s;
        List<String> result = stream.mapToObj(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains("Short_1"));
        assertTrue(result.contains("Short_26"));
    }

    @Test
    public void testMapToObj_SequentialFallback() {
        List<String> result = createShortStream(new short[] { (short) 5 }).mapToObj(s -> "v" + s).toList();
        assertEquals(1, result.size());
        assertEquals("v5", result.get(0));
    }

    @Test
    public void testMapToObj_empty() {
        parallelStream = createShortStream(new short[] {});
        List<String> result = parallelStream.mapToObj(s -> String.valueOf(s)).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testFlatMapShortStream() {
        ShortStream stream = createShortStream(new short[] { 1, 2 });
        ShortFunction<ShortStream> mapper = s -> ShortStream.of(s, (short) (s + 1));
        List<Short> result = stream.flatMap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains((short) 1));
        assertTrue(result.contains((short) 2));
        assertTrue(result.contains((short) 3));
    }

    @Test
    public void testFlatMapShortArray() {
        ShortStream stream = createShortStream(new short[] { 1, 2 });
        ShortFunction<short[]> mapper = s -> new short[] { s, (short) (s + 1) };
        List<Short> result = stream.flatmap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains((short) 1));
        assertTrue(result.contains((short) 2));
        assertTrue(result.contains((short) 3));
    }

    @Test
    public void testFlatMap() {
        long count = createShortStream((short) 1, (short) 2, (short) 3).parallel().flatMap(x -> createShortStream(x, x, x)).count();
        assertEquals(9, count);
    }

    @Test
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        ShortStream flattened = parallelStream.flatmap(s -> new short[] { s, (short) (s * 2) });
        short[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createShortStream(new short[] { 1, 2, 3 });
        flattened = parallelStream.flatmap(s -> s % 2 == 0 ? new short[] { s } : new short[0]);
        assertArrayEquals(new short[] { 2 }, flattened.toArray());
    }

    @Test
    public void testFlatMap_ParallelPath() {
        long count = createShortStream(TEST_ARRAY).flatMap(s -> ShortStream.of(s, (short) (s * 2))).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmap_ParallelPath() {
        long count = createShortStream(TEST_ARRAY).flatmap(s -> new short[] { s, (short) (s * 2) }).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatMap_SequentialFallback() {
        List<Short> result = createShortStream(new short[] { (short) 1 }).flatMap(s -> ShortStream.of(s, (short) (s + 10))).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains((short) 1));
        assertTrue(result.contains((short) 11));
    }

    @Test
    public void testFlatmap_SequentialFallback() {
        List<Short> result = createShortStream(new short[] { (short) 1 }).flatmap(s -> new short[] { s, (short) (s + 10) }).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlatMapToInt() {
        ShortStream stream = createShortStream(new short[] { 1, 2 });
        ShortFunction<IntStream> mapper = s -> IntStream.of(s, s * 2);
        List<Integer> result = stream.flatMapToInt(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(4));
    }

    @Test
    public void testFlatMapToInt_ParallelPath() {
        long count = createShortStream(TEST_ARRAY).flatMapToInt(s -> IntStream.of((int) s)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToInt_SequentialFallback() {
        List<Integer> result = createShortStream(new short[] { (short) 1 }).flatMapToInt(s -> IntStream.of((int) s)).toList();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void testFlatMapToObjStream() {
        ShortStream stream = createShortStream(new short[] { 1, 2 });
        ShortFunction<Stream<String>> mapper = s -> Stream.of(String.valueOf(s), String.valueOf(s + 1));
        List<String> result = stream.flatMapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    public void testFlatMapToObjCollection() {
        ShortStream stream = createShortStream(new short[] { 1, 2 });
        ShortFunction<Collection<String>> mapper = s -> {
            List<String> list = new ArrayList<>();
            list.add(String.valueOf(s));
            list.add(String.valueOf(s + 1));
            return list;
        };
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    @DisplayName("Test flatMapToObj method")
    public void testFlatMapToObj() {
        Stream<String> flattened = parallelStream.flatMapToObj(s -> Stream.of("A" + s, "B" + s));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B10"));
    }

    @Test
    @DisplayName("Test flatmapToObj with collection method")
    public void testFlatmapToObjCollection() {
        Stream<Short> flattened = parallelStream.flatmapToObj(s -> Arrays.asList(Short.valueOf(s), Short.valueOf((short) (s * 10))));
        List<Short> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertEquals(Short.valueOf((short) 1), result.get(0));
        assertEquals(Short.valueOf((short) 100), result.get(19));
    }

    @Test
    public void testFlatMapToObj_ParallelPath() {
        long count = createShortStream(TEST_ARRAY).flatMapToObj(s -> Stream.of(s, (short) (s * 2))).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmapToObj_ParallelPath() {
        long count = createShortStream(TEST_ARRAY).flatmapToObj(s -> Arrays.asList(s, (short) (s * 2))).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatMapToObj_SequentialFallback() {
        List<String> result = createShortStream(new short[] { (short) 1 }).flatMapToObj(s -> Stream.of("v" + s)).toList();
        assertEquals(1, result.size());
        assertEquals("v1", result.get(0));
    }

    @Test
    public void testFlatmapToObj_SequentialFallback() {
        List<String> result = createShortStream(new short[] { (short) 1 }).flatmapToObj(s -> Arrays.asList("a" + s, "b" + s)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testOnEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        List<Short> result = createShortStream(new short[] { (short) 5 }).peek(s -> count.incrementAndGet()).toList();
        assertEquals(1, result.size());
        assertEquals(1, count.get());
    }

    @Test
    public void testOnEach_ParallelPath() {
        AtomicInteger count = new AtomicInteger(0);
        long total = createShortStream(TEST_ARRAY).peek(s -> count.incrementAndGet()).count();
        assertEquals(TEST_ARRAY.length, total);
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testOnEach() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        List<Short> consumed = new ArrayList<>();
        ShortConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.peek(action).forEach(s -> {
        });

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEach_IteratorSplitor() {
        AtomicInteger sum = new AtomicInteger(0);
        createIteratorSplitorShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).forEach(s -> sum.addAndGet(s));
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEach_ParallelPath() {
        AtomicInteger count = new AtomicInteger(0);
        createShortStream(TEST_ARRAY).forEach(s -> count.incrementAndGet());
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testForEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createShortStream(new short[] { (short) 5 }).forEach(s -> count.incrementAndGet());
        assertEquals(1, count.get());
    }

    @Test
    public void testForEach() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        List<Short> consumed = new ArrayList<>();
        ShortConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.forEach(action);
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEachWithException() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        AtomicInteger count = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            stream.forEach(s -> {
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

        parallelStream.forEach(s -> {
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
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        Map<String, String> result = stream.toMap(s -> String.valueOf(s), s -> "Value_" + s, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1", result.get("1"));
        assertEquals("Value_2", result.get("2"));
        assertEquals("Value_3", result.get("3"));
    }

    @Test
    public void testToMap_ParallelPath() {
        Map<Short, Short> result = createShortStream(TEST_ARRAY).toMap(s -> s, s -> (short) (s * 2), (a, b) -> a, java.util.HashMap::new);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testToMap_SequentialFallback() {
        Map<String, Short> result = createShortStream(new short[] { (short) 5 }).toMap(s -> "k" + s, s -> s, (v1, v2) -> v1, java.util.HashMap::new);
        assertEquals(1, result.size());
        assertEquals((short) 5, (short) result.get("k5"));
    }

    @Test
    public void testGroupTo() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 1, 3, 2 });
        Map<String, List<Short>> result = stream.groupTo(s -> String.valueOf(s), Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(List.of((short) 1, (short) 1), result.get("1"));
        assertEquals(List.of((short) 2, (short) 2), result.get("2"));
        assertEquals(List.of((short) 3), result.get("3"));
    }

    @Test
    public void testGroupTo_ParallelPath() {
        Map<Boolean, List<Short>> result = createShortStream(TEST_ARRAY).groupTo(s -> s % 2 == 0, Collectors.toList(), java.util.HashMap::new);
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupTo_SequentialFallback() {
        Map<Boolean, List<Short>> result = createShortStream(new short[] { (short) 5 }).groupTo(s -> s % 2 != 0, Collectors.toList(), java.util.HashMap::new);
        assertEquals(1, result.size());
        assertEquals(1, result.get(true).size());
    }

    @Test
    public void testReduce_SequentialFallback() {
        short result = createShortStream(new short[] { (short) 5 }).reduce((short) 0, (s1, s2) -> (short) (s1 + s2));
        assertEquals((short) 5, result);

        OptionalShort opt = createShortStream(new short[] { (short) 7 }).reduce((s1, s2) -> (short) (s1 + s2));
        assertTrue(opt.isPresent());
        assertEquals((short) 7, opt.get());
    }

    @Test
    public void testReduceWithIdentity() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        ShortBinaryOperator op = (s1, s2) -> (short) (s1 + s2);
        short result = stream.reduce((short) 0, op);
        assertEquals((short) 6, result);

        ShortStream emptyStream = createShortStream(new short[] {});
        short emptyResult = emptyStream.reduce((short) 100, op);
        assertEquals((short) 100, emptyResult);
    }

    @Test
    public void testReduceWithoutIdentity() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        ShortBinaryOperator accumulator = (s1, s2) -> (short) (s1 + s2);
        OptionalShort result = stream.reduce(accumulator);
        assertTrue(result.isPresent());
        assertEquals((short) 6, result.get());

        ShortStream emptyStream = createShortStream(new short[] {});
        OptionalShort emptyResult = emptyStream.reduce(accumulator);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testReduce() {
        short sum = parallelStream.reduce((a, b) -> (short) (a + b)).orElse((short) -1);
        short expectedSum = (short) N.sum(largeArray);
        assertEquals(expectedSum, sum);
        assertFalse(emptyParallelStream.reduce((a, b) -> (short) (a + b)).isPresent());
    }

    @Test
    public void testReduce_WithIdentity_ParallelPath() {
        short result = createShortStream(TEST_ARRAY).reduce((short) 0, (s1, s2) -> (short) Math.max(s1, s2));
        assertEquals((short) 26, result);
    }

    @Test
    public void testReduce_NoIdentity_ParallelPath() {
        OptionalShort result = createShortStream(TEST_ARRAY).reduce((s1, s2) -> (short) Math.max(s1, s2));
        assertTrue(result.isPresent());
        assertEquals((short) 26, result.get());
    }

    @Test
    public void testIteratorSplitorReduceAndFindOperations_SparseMatch() {
        assertEquals((short) 72, createIteratorSplitorShortStream((short) 21, (short) 2, (short) 4, (short) 7, (short) 6, (short) 11, (short) 8, (short) 13)
                .reduce((short) 0, (left, right) -> (short) (left + right)));

        OptionalShort reduced = createIteratorSplitorShortStream((short) 21, (short) 2, (short) 4).reduce((left, right) -> (short) (left + right));
        assertTrue(reduced.isPresent());
        assertEquals((short) 27, reduced.get());

        OptionalShort firstMatch = createIteratorSplitorShortStream((short) 21, (short) 2, (short) 4, (short) 7, (short) 6, (short) 11, (short) 8, (short) 13)
                .findFirst(s -> {
                    if (s == 21) {
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    return s > 5 && (s & 1) == 1;
                });
        assertTrue(firstMatch.isPresent());
        assertEquals((short) 21, firstMatch.get());

        OptionalShort anyMatch = createIteratorSplitorShortStream((short) 21, (short) 2, (short) 4, (short) 7, (short) 6, (short) 11, (short) 8, (short) 13)
                .findAny(s -> s > 5 && (s & 1) == 1);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21 || anyMatch.get() == 7 || anyMatch.get() == 11 || anyMatch.get() == 13);

        OptionalShort lastMatch = createIteratorSplitorShortStream((short) 21, (short) 2, (short) 4, (short) 7, (short) 6, (short) 11, (short) 8, (short) 13)
                .findLast(s -> {
                    if (s == 13) {
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    return s > 5 && (s & 1) == 1;
                });
        assertTrue(lastMatch.isPresent());
        assertEquals((short) 13, lastMatch.get());
    }

    @Test
    public void testCollect() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        List<Short> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(List.of((short) 1, (short) 2, (short) 3), collectedList);
    }

    // Tests covering parallel code paths with large array

    @Test
    public void testCollect_ArraySplitor_ParallelPath() {
        // Uses ARRAY splitor (default), 26 elements, 4 threads - goes parallel
        List<Short> result = createShortStream(TEST_ARRAY).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testCollect_IteratorSplitor_ParallelPath() {
        // Uses ITERATOR splitor, 26 elements, 4 threads - goes through iterator path
        List<Short> result = createIteratorSplitorShortStream(TEST_ARRAY).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testCollect_empty() {
        parallelStream = createShortStream(new short[] {});
        List<Short> collectedList = parallelStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertTrue(collectedList.isEmpty());
    }

    @Test
    public void testCollect_IteratorSplitor() {
        List<Short> result = createIteratorSplitorShortStream((short) 1, (short) 2, (short) 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of((short) 1, (short) 2, (short) 3), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createShortStream(TEST_ARRAY).anyMatch(s -> s == 13));
        assertFalse(createShortStream(TEST_ARRAY).anyMatch(s -> s == 100));
        assertFalse(createShortStream(new short[] {}).anyMatch(s -> true));
    }

    @Test
    public void testAnyMatch_SequentialFallback() {
        assertTrue(createShortStream(new short[] { (short) 5 }).anyMatch(s -> s == 5));
        assertFalse(createShortStream(new short[] { (short) 5 }).anyMatch(s -> s > 10));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createShortStream(TEST_ARRAY).allMatch(s -> s >= 1 && s <= 26));
        assertFalse(createShortStream(TEST_ARRAY).allMatch(s -> s < 26));
        assertTrue(createShortStream(new short[] {}).allMatch(s -> false));
    }

    @Test
    public void testAllMatch_SequentialFallback() {
        assertTrue(createShortStream(new short[] { (short) 5 }).allMatch(s -> s > 0));
        assertFalse(createShortStream(new short[] { (short) 5 }).allMatch(s -> s > 10));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createShortStream(TEST_ARRAY).noneMatch(s -> s == 100));
        assertFalse(createShortStream(TEST_ARRAY).noneMatch(s -> s == 1));
        assertTrue(createShortStream(new short[] {}).noneMatch(s -> true));
    }

    // Cover the ITERATOR splitor path for anyMatch / allMatch / noneMatch / collect / forEach
    @Test
    public void testAnyMatchAllMatchNoneMatch_IteratorSplitor() {
        assertTrue(createIteratorSplitorShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).anyMatch(s -> s > 4));
        assertFalse(createIteratorSplitorShortStream((short) 1, (short) 2, (short) 3).anyMatch(s -> s > 10));

        assertTrue(createIteratorSplitorShortStream((short) 1, (short) 2, (short) 3).allMatch(s -> s >= 1 && s <= 3));
        assertFalse(createIteratorSplitorShortStream((short) 1, (short) 2, (short) 3).allMatch(s -> s < 3));

        assertTrue(createIteratorSplitorShortStream((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 10));
        assertFalse(createIteratorSplitorShortStream((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 2));
    }

    @Test
    public void testNoneMatch_SequentialFallback() {
        assertTrue(createShortStream(new short[] { (short) 5 }).noneMatch(s -> s > 10));
        assertFalse(createShortStream(new short[] { (short) 5 }).noneMatch(s -> s == 5));
    }

    @Test
    public void testFindFirst_SequentialFallback() {
        OptionalShort found = createShortStream(new short[] { (short) 5 }).findFirst(s -> s == 5);
        assertTrue(found.isPresent());
        assertEquals((short) 5, found.get());

        OptionalShort notFound = createShortStream(new short[] { (short) 5 }).findFirst(s -> s > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindFirst() {
        ShortStream stream = createShortStream(new short[] { 4, 2, 1, 3, 1 });
        OptionalShort result = stream.findFirst(s -> s == 1);
        assertTrue(result.isPresent());
        assertEquals((short) 1, result.get());

        stream = createShortStream(new short[] { 4, 2, 1, 3, 1 });
        OptionalShort notFound = stream.findFirst(s -> s == 10);
        assertFalse(notFound.isPresent());

        OptionalShort empty = createShortStream(new short[] {}).findFirst(s -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalShort result = parallelStream.findFirst(s -> s > 5);

        assertTrue(result.isPresent());
        assertEquals(6, result.get());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findFirst(s -> s > 30);
        assertFalse(result.isPresent());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findFirst(s -> s == 1);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFindAny_SequentialFallback() {
        OptionalShort found = createShortStream(new short[] { (short) 5 }).findAny(s -> s == 5);
        assertTrue(found.isPresent());
        assertEquals((short) 5, found.get());

        OptionalShort notFound = createShortStream(new short[] { (short) 5 }).findAny(s -> s > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindAny() {
        ShortStream stream = createShortStream(new short[] { 4, 2, 1, 3, 1 });
        OptionalShort result = stream.findAny(s -> s == 1);
        assertTrue(result.isPresent());
        assertEquals((short) 1, result.get());

        stream = createShortStream(new short[] { 4, 2, 1, 3, 1 });
        OptionalShort notFound = stream.findAny(s -> s == 10);
        assertFalse(notFound.isPresent());

        OptionalShort empty = createShortStream(new short[] {}).findAny(s -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalShort result = parallelStream.findAny(s -> s > 5);

        assertTrue(result.isPresent());
        assertTrue(result.get() > 5);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findAny(s -> s > 30);
        assertFalse(result.isPresent());

        parallelStream = createShortStream(new short[] { 5 });
        result = parallelStream.findAny(s -> s == 5);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testFindLast_SequentialFallback() {
        OptionalShort found = createShortStream(new short[] { (short) 5 }).findLast(s -> s == 5);
        assertTrue(found.isPresent());
        assertEquals((short) 5, found.get());

        OptionalShort notFound = createShortStream(new short[] { (short) 5 }).findLast(s -> s > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindLast() {
        ShortStream stream = createShortStream(new short[] { 4, 2, 1, 3, 1 });
        OptionalShort result = stream.findLast(s -> s == 1);
        assertTrue(result.isPresent());
        assertEquals((short) 1, result.get());

        stream = createShortStream(new short[] { 4, 2, 1, 3, 1 });
        OptionalShort notFound = stream.findLast(s -> s == 10);
        assertFalse(notFound.isPresent());

        OptionalShort empty = createShortStream(new short[] {}).findLast(s -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalShort result = parallelStream.findLast(s -> s < 5);

        assertTrue(result.isPresent());
        assertEquals(4, result.get());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findLast(s -> s > 30);
        assertFalse(result.isPresent());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findLast(s -> s == 10);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    public void testZipWithBinaryOperator() {
        ShortStream streamA = createShortStream(new short[] { 1, 2, 3 });
        ShortStream streamB = ShortStream.of((short) 10, (short) 20, (short) 30);
        ShortBinaryOperator zipper = (s1, s2) -> (short) (s1 + s2);
        List<Short> result = streamA.zipWith(streamB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals((short) 11, result.get(0));
        assertEquals((short) 22, result.get(1));
        assertEquals((short) 33, result.get(2));
    }

    @Test
    public void testZipWithTernaryOperator() {
        ShortStream streamA = createShortStream(new short[] { 1, 2 });
        ShortStream streamB = ShortStream.of((short) 10, (short) 20);
        ShortStream streamC = ShortStream.of((short) 100, (short) 101);
        ShortTernaryOperator zipper = (s1, s2, s3) -> (short) (s1 + s2 + s3);
        List<Short> result = streamA.zipWith(streamB, streamC, zipper).sorted().toList();
        assertEquals(2, result.size());
        assertEquals((short) (1 + 10 + 100), result.get(0));
        assertEquals((short) (2 + 20 + 101), result.get(1));
    }

    @Test
    public void testZipWithTwoStreams() {
        ShortStream s1 = createShortStream(largeArray).parallel();
        ShortStream s2 = createShortStream(largeArray).parallel();
        long count = s1.zipWith(s2, (a, b) -> (short) (a + b)).count();
        assertEquals(largeArray.length, count);

        long sum = createShortStream(largeArray).parallel().zipWith(createShortStream(largeArray), (a, b) -> (short) (a + b)).sum();
        assertEquals(N.sum(largeArray) * 2, sum);
    }

    @Test
    public void testZipWithThreeStreams() {
        ShortStream s1 = createShortStream(largeArray).parallel();
        ShortStream s2 = createShortStream(largeArray).parallel();
        ShortStream s3 = createShortStream(largeArray).parallel();
        long sum = s1.zipWith(s2, s3, (a, b, c) -> (short) (a + b + c)).sum();
        assertEquals(N.sum(largeArray) * 3, sum);
    }

    @Test
    @DisplayName("Test zipWith two streams with default values")
    public void testZipWithBinaryDefaults() {
        ShortStream stream2 = createShortStream(new short[] { 10, 20, 30 });
        short[] result = parallelStream.zipWith(stream2, (short) 0, (short) 100, (a, b) -> (short) (a + b)).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(11, result[0]);
        assertEquals(22, result[1]);
        assertEquals(33, result[2]);
        assertEquals(104, result[3]);
    }

    @Test
    @DisplayName("Test zipWith three streams with default values")
    public void testZipWithTernaryDefaults() {
        ShortStream stream2 = createShortStream(new short[] { 10, 20 });
        ShortStream stream3 = createShortStream(new short[] { 1, 2, 3, 4 });
        short[] result = parallelStream.zipWith(stream2, stream3, (short) 0, (short) 0, (short) 1, (a, b, c) -> (short) (a + b + c)).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(6, result[0]);
        assertEquals(6, result[1]);
        assertEquals(24, result[9]);
    }

    @Test
    public void testZipWithBinary_ParallelPath() {
        long count = createShortStream(TEST_ARRAY).zipWith(ShortStream.of(TEST_ARRAY), (a, b) -> (short) (a + b)).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernary_ParallelPath() {
        long count = createShortStream(TEST_ARRAY).zipWith(ShortStream.of(TEST_ARRAY), ShortStream.of(TEST_ARRAY), (a, b, c) -> (short) (a + b + c)).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryDefaults_ParallelPath() {
        short[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createShortStream(TEST_ARRAY).zipWith(ShortStream.of(shorter), (short) 0, (short) -1, (a, b) -> (short) (a + b)).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernaryDefaults_ParallelPath() {
        short[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createShortStream(TEST_ARRAY)
                .zipWith(ShortStream.of(shorter), ShortStream.of(shorter), (short) 0, (short) -1, (short) -2, (a, b, c) -> (short) (a + b + c))
                .count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        ShortStream streamA = createShortStream(new short[] { 1, 2 });
        ShortStream streamB = ShortStream.of((short) 10, (short) 20, (short) 30);
        short valA = 0;
        short valB = -1;
        ShortBinaryOperator zipper = (s1, s2) -> {
            if (s1 == valA)
                return s2;
            if (s2 == valB)
                return s1;
            return (short) (s1 + s2);
        };
        List<Short> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals((short) 11, result.get(0));
        assertEquals((short) 22, result.get(1));
        assertEquals((short) 30, result.get(2));
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        ShortStream streamA = createShortStream(new short[] { 1 });
        ShortStream streamB = ShortStream.of((short) 10, (short) 20);
        ShortStream streamC = ShortStream.of((short) 100, (short) 101, (short) 102);
        short valA = 0;
        short valB = -1;
        short valC = -2;
        ShortTernaryOperator zipper = (s1, s2, s3) -> {
            return (short) (s1 + s2 + s3);
        };
        List<Short> result = streamA.zipWith(streamB, streamC, valA, valB, valC, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals((short) (1 + 10 + 100), result.get(1));
        assertEquals((short) (0 + 20 + 101), result.get(2));
        assertEquals((short) (0 + -1 + 102), result.get(0));
    }

    @Test
    public void testZipWithDefaultValues() {
        short[] shortArr = { 10, 20 };
        ShortStream s1 = createShortStream(smallArray).parallel();
        ShortStream s2 = createShortStream(shortArr).parallel();

        ShortList result = s1.zipWith(s2, (short) 99, (short) 100, (a, b) -> (short) (a + b)).toShortList();

        ShortList expected = new ShortList();
        expected.add((short) (1 + 10));
        expected.add((short) (2 + 20));
        expected.add((short) (3 + 100));
        expected.add((short) (4 + 100));
        expected.add((short) (5 + 100));

        assertHaveSameElements(expected.boxed(), result.boxed());
    }

    @Test
    @DisplayName("Test zipWith two streams with binary operator")
    public void testZipWithBinary() {
        ShortStream stream2 = createShortStream(new short[] { 10, 20, 30, 40, 50 });
        ShortStream zipped = parallelStream.zipWith(stream2, (a, b) -> (short) (a + b));
        short[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new short[] { 11, 22, 33, 44, 55 }, result);

        parallelStream = createShortStream(new short[] { 1, 2, 3 });
        stream2 = createShortStream(new short[] { 10, 20, 30, 40, 50 });
        zipped = parallelStream.zipWith(stream2, (a, b) -> (short) (a * b));
        assertEquals(3, zipped.count());
    }

    @Test
    @DisplayName("Test zipWith three streams with ternary operator")
    public void testZipWithTernary() {
        ShortStream stream2 = createShortStream(new short[] { 10, 20, 30, 40, 50 });
        ShortStream stream3 = createShortStream(new short[] { 100, 100, 100, 100, 100 });
        ShortStream zipped = parallelStream.zipWith(stream2, stream3, (a, b, c) -> (short) ((a + b) * c / 100));
        short[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new short[] { 11, 22, 33, 44, 55 }, result);
    }

    @Test
    public void testZipWithDefaultValues_SequentialFallback() {
        List<Short> result = ShortStream.of((short) 1, (short) 2, (short) 3)
                .parallel(PS.create(Splitor.ARRAY).maxThreadNum(1))
                .zipWith(ShortStream.of((short) 10), (short) 0, (short) -1, (a, b) -> (short) (a + b))
                .toList();

        assertEquals(Arrays.asList((short) 11, (short) 1, (short) 2), result);
    }

    @Test
    public void testIsParallel() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
    }

    @Test
    public void testSequential() {
        ShortStream parallelStream = createShortStream(TEST_ARRAY);
        ShortStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        List<Short> result = sequentialStream.toList();
        assertEquals(TEST_ARRAY.length, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i));
        }
        parallelStream.close();
        sequentialStream.close();
    }

    @Test
    public void testSequential_ParallelPath() {
        ShortStream parallel = createShortStream(TEST_ARRAY);
        assertTrue(parallel.isParallel());
        ShortStream seq = parallel.sequential();
        assertFalse(seq.isParallel());
        assertEquals(TEST_ARRAY.length, seq.count());
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelArrayShortStream) createShortStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ARRAY, ((ParallelArrayShortStream) createShortStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelArrayShortStream) createShortStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    @DisplayName("Test method chaining")
    public void testMethodChaining() {
        List<String> result = parallelStream.filter(s -> s > 3)
                .map(s -> (short) (s * 2))
                .flatMapToObj(s -> Stream.of("Value: " + s, "Half: " + (s / 2)))
                .sorted()
                .toList();

        assertTrue(result.size() > 0);
        assertTrue(result.contains("Value: 8"));
        assertTrue(result.contains("Half: 4"));
    }

    @Test
    public void testOnClose() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        ShortStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get());
        newStream.close();
        assertTrue(closedFlag.get());
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        AtomicInteger closedCount = new AtomicInteger(0);
        Runnable handler1 = () -> closedCount.incrementAndGet();
        Runnable handler2 = () -> closedCount.incrementAndGet();

        ShortStream newStream = stream.onClose(handler1).onClose(handler2);
        assertEquals(0, closedCount.get());
        newStream.close();
        assertEquals(2, closedCount.get());
    }

    @Test
    public void testOnCloseEmptyHandler() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortStream newStream = stream.onClose(null);
        assertSame(stream, newStream);
        newStream.close();
    }

    @Test
    @DisplayName("Test overflow scenarios")
    public void testOverflowScenarios() {
        parallelStream = createShortStream(new short[] { Short.MAX_VALUE, 1, 1 });

        short[] result = parallelStream.map(s -> (short) (s * 2)).toArray();
        assertEquals(-2, result[0]);
        assertEquals(2, result[1]);
        assertEquals(2, result[2]);
    }

    @Test
    @DisplayName("Test boundary values")
    public void testBoundaryValues() {
        short[] boundaryArray = new short[] { Short.MIN_VALUE, (short) (Short.MIN_VALUE + 1), -1, 0, 1, (short) (Short.MAX_VALUE - 1), Short.MAX_VALUE };

        parallelStream = createShortStream(boundaryArray);

        short[] result = parallelStream.sorted().toArray();
        assertEquals(7, result.length);
        assertEquals(Short.MIN_VALUE, result[0]);
        assertEquals(Short.MAX_VALUE, result[6]);
    }
}
