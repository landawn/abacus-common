package com.landawn.abacus.util.stream;

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
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortTernaryOperator;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelIteratorShortStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final short[] TEST_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };
    private static final short[] DEFAULT_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private static final short[] SMALL_ARRAY = { 1, 2, 3, 4, 5 };

    private ShortStream parallelStream;
    private ShortStream emptyParallelStream;
    private short[] largeArray;
    private short[] smallArray;

    protected ShortStream createShortStream(short... elements) {
        return ShortStream.of(elements).map(e -> (short) (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        largeArray = DEFAULT_ARRAY.clone();
        smallArray = SMALL_ARRAY.clone();
        parallelStream = createShortStream(largeArray);
        emptyParallelStream = createShortStream();
    }

    // ---- Sequential-fallback path: 1-thread iterator stream => canBeSequential(maxThreadNum) == true ----

    private ShortStream createSingleThreadStream(short... elements) {
        return ShortStream.of(elements).map(e -> (short) (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1));
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
    public void testFilter() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        List<Short> result = stream.filter(s -> s > 20).toList();
        assertEquals(6, result.size());
        for (Short s : result) {
            assertTrue(s > 20);
        }

        ShortStream emptyStream = createShortStream(new short[] {});
        List<Short> emptyResult = emptyStream.filter(s -> true).toList();
        assertEquals(0, emptyResult.size());
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
    public void testFilter_SequentialFallback() {
        List<Short> result = createSingleThreadStream((short) 1, (short) 2, (short) 3, (short) 4).filter(s -> s % 2 == 0).toList();
        assertHaveSameElements(Arrays.asList((short) 2, (short) 4), result);
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
        List<Short> result = createSingleThreadStream((short) 1, (short) 2, (short) 3, (short) 4).takeWhile(s -> s < 3).toList();
        assertHaveSameElements(Arrays.asList((short) 1, (short) 2), result);
    }

    @Test
    public void testDropWhile() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3, 4, 5 });
        List<Short> result = stream.dropWhile(s -> s < 4).toList();
        assertHaveSameElements(Arrays.asList((short) 4, (short) 5), result);

        ShortStream stream2 = createShortStream(new short[] { 1, 2, 3 });
        List<Short> result2 = stream2.dropWhile(s -> true).toList();
        assertEquals(0, result2.size());
    }

    @Test
    public void testDropWhile_SequentialFallback() {
        List<Short> result = createSingleThreadStream((short) 1, (short) 2, (short) 3, (short) 4).dropWhile(s -> s < 3).toList();
        assertHaveSameElements(Arrays.asList((short) 3, (short) 4), result);
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
        List<Short> result = createSingleThreadStream((short) 1, (short) 2, (short) 3).map(s -> (short) (s + 10)).toList();
        assertHaveSameElements(Arrays.asList((short) 11, (short) 12, (short) 13), result);
    }

    @Test
    public void testMapToInt() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        List<Integer> result = stream.mapToInt(s -> s * 10).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
    }

    @Test
    public void testMapToInt_SequentialFallback() {
        List<Integer> result = createSingleThreadStream((short) 1, (short) 2, (short) 3).mapToInt(s -> s * 10).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
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
        List<String> result = createSingleThreadStream((short) 1, (short) 2).mapToObj(s -> "v" + s).toList();
        assertHaveSameElements(Arrays.asList("v1", "v2"), result);
    }

    @Test
    public void testFlatMap() {
        long count = createShortStream((short) 1, (short) 2, (short) 3).parallel().flatMap(x -> createShortStream(x, x, x)).count();
        assertEquals(9, count);
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
    public void testFlatMap_SequentialFallback() {
        List<Short> result = createSingleThreadStream((short) 1, (short) 2).flatMap(s -> ShortStream.of(s, (short) (s + 10))).toList();
        assertHaveSameElements(Arrays.asList((short) 1, (short) 11, (short) 2, (short) 12), result);
    }

    @Test
    public void testFlatmap_SequentialFallback() {
        List<Short> result = createSingleThreadStream((short) 1, (short) 2).flatmap(s -> new short[] { s, (short) (s + 10) }).toList();
        assertHaveSameElements(Arrays.asList((short) 1, (short) 11, (short) 2, (short) 12), result);
    }

    @Test
    public void testFlatMapToInt() {
        ShortStream stream = createShortStream(new short[] { 1, 2 });
        List<Integer> result = stream.flatMapToInt(s -> IntStream.of(s, s * 10)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(10));
        assertTrue(result.contains(2));
        assertTrue(result.contains(20));
    }

    @Test
    public void testFlatMapToInt_SequentialFallback() {
        List<Integer> result = createSingleThreadStream((short) 1, (short) 2).flatMapToInt(s -> IntStream.of(s, s * 10)).toList();
        assertHaveSameElements(Arrays.asList(1, 10, 2, 20), result);
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
    public void testFlatmapToObj() {
        ShortStream stream = createShortStream(new short[] { 1, 2 });
        List<String> result = stream.flatmapToObj(s -> Arrays.asList("X" + s, "Y" + s)).toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("X1", "Y1", "X2", "Y2")));
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
    public void testFlatMapToObj_largeArray() {
        List<String> result = createShortStream(TEST_ARRAY).flatMapToObj(s -> Stream.of(new String[] { "v" + s })).toList();
        assertEquals(26, result.size());
        assertTrue(result.contains("v1"));
    }

    @Test
    public void testFlatmapToObj_largeArray() {
        List<String> result = createShortStream(TEST_ARRAY).flatmapToObj(s -> Arrays.asList("a" + s, "b" + s)).toList();
        assertEquals(52, result.size());
    }

    @Test
    public void testFlatMapToObj_SequentialFallback() {
        List<String> result = createSingleThreadStream((short) 1, (short) 2).flatMapToObj(s -> Stream.of("A" + s, "B" + s)).toList();
        assertHaveSameElements(Arrays.asList("A1", "B1", "A2", "B2"), result);
    }

    @Test
    public void testFlatmapToObj_SequentialFallback() {
        List<String> result = createSingleThreadStream((short) 1, (short) 2).flatmapToObj(s -> Arrays.asList("X" + s, "Y" + s)).toList();
        assertHaveSameElements(Arrays.asList("X1", "Y1", "X2", "Y2"), result);
    }

    @Test
    public void testOnEach_ParallelPath_Iterator() {
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
    public void testOnEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleThreadStream((short) 1, (short) 2, (short) 3).peek(s -> count.incrementAndGet()).count();
        assertEquals(3, count.get());
    }

    @Test
    public void testForEach_ParallelPath_Iterator() {
        AtomicInteger count = new AtomicInteger(0);
        createShortStream(TEST_ARRAY).forEach(s -> count.incrementAndGet());
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testForEach() {
        List<Short> consumed = new ArrayList<>();
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        stream.forEach(s -> {
            synchronized (consumed) {
                consumed.add(s);
            }
        });
        assertEquals(3, consumed.size());
        assertHaveSameElements(Arrays.asList((short) 1, (short) 2, (short) 3), consumed);
    }

    @Test
    public void testForEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleThreadStream((short) 1, (short) 2, (short) 3).forEach(s -> count.incrementAndGet());
        assertEquals(3, count.get());
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
    public void testToMap() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3, 1 });
        Map<String, Integer> result = stream.toMap(s -> "Key_" + s, s -> (int) s, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(2, (int) result.get("Key_1"));
        assertEquals(2, (int) result.get("Key_2"));
        assertEquals(3, (int) result.get("Key_3"));
    }

    @Test
    public void testToMap_SequentialFallback() {
        Map<String, Integer> result = createSingleThreadStream((short) 1, (short) 2).toMap(s -> "k" + s, s -> (int) s, (v1, v2) -> v1, ConcurrentHashMap::new);
        assertEquals(2, result.size());
        assertEquals(1, (int) result.get("k1"));
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
    public void testGroupTo_SequentialFallback() {
        Map<Boolean, List<Short>> result = createSingleThreadStream((short) 1, (short) 2, (short) 3, (short) 4).groupTo(s -> s % 2 == 0, Collectors.toList(),
                ConcurrentHashMap::new);
        assertEquals(2, result.size());
        assertEquals(2, result.get(true).size());
        assertEquals(2, result.get(false).size());
    }

    @Test
    public void testReduce_parallelWithLargeArray() {
        short sum = createShortStream(TEST_ARRAY).reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals((short) 351, sum);

        OptionalShort opt = createShortStream(TEST_ARRAY).reduce((a, b) -> (short) (a + b));
        assertTrue(opt.isPresent());
        assertEquals((short) 351, opt.get());
    }

    @Test
    public void testConstructor_withDefaultValues() {
        ShortStream stream = ShortStream
                .of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7, (short) 8, (short) 9, (short) 10, (short) 11, (short) 12,
                        (short) 13, (short) 14, (short) 15, (short) 16, (short) 17, (short) 18, (short) 19, (short) 20)
                .parallel();
        short sum = stream.reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals((short) 210, sum);
    }

    @Test
    public void testReduceWithIdentity() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        ShortBinaryOperator op = (s1, s2) -> (short) (s1 + s2);
        short result = stream.reduce((short) 0, op);
        assertEquals((short) 6, result);

        ShortStream emptyStream = createShortStream(new short[] {});
        short emptyResult = emptyStream.reduce((short) 0, op);
        assertEquals((short) 0, emptyResult);
    }

    @Test
    public void testReduceWithoutIdentity() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3, 4, 5 });
        OptionalShort result = stream.reduce((s1, s2) -> (short) (s1 + s2));
        assertTrue(result.isPresent());
        assertEquals((short) 15, result.get());

        ShortStream emptyStream = createShortStream(new short[] {});
        OptionalShort emptyResult = emptyStream.reduce((s1, s2) -> (short) (s1 + s2));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testReduce_emptyStreamParallel() {
        short result = createShortStream(new short[0]).reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals((short) 0, result);

        OptionalShort opt = createShortStream(new short[0]).reduce((a, b) -> (short) (a + b));
        assertFalse(opt.isPresent());
    }

    @Test
    public void testReduceWithIdentity_SequentialFallback() {
        short result = createSingleThreadStream((short) 1, (short) 2, (short) 3).reduce((short) 0, (s1, s2) -> (short) (s1 + s2));
        assertEquals((short) 6, result);
    }

    @Test
    public void testReduceWithoutIdentity_SequentialFallback() {
        OptionalShort result = createSingleThreadStream((short) 1, (short) 2, (short) 3).reduce((s1, s2) -> (short) (s1 + s2));
        assertTrue(result.isPresent());
        assertEquals((short) 6, result.get());

        OptionalShort empty = createSingleThreadStream(new short[0]).reduce((s1, s2) -> (short) (s1 + s2));
        assertFalse(empty.isPresent());
    }

    // Covers delayed-match ordering for iterator-backed terminal operations.
    @Test
    public void testReduceAndFindOperations_DelayedMatchOrdering() {
        assertEquals((short) 72, createShortStream((short) 21, (short) 2, (short) 4, (short) 7, (short) 6, (short) 11, (short) 8, (short) 13).reduce((short) 0,
                (left, right) -> (short) (left + right)));

        OptionalShort reduced = createShortStream((short) 21, (short) 2, (short) 4).reduce((left, right) -> (short) (left + right));
        assertTrue(reduced.isPresent());
        assertEquals((short) 27, reduced.get());

        OptionalShort firstMatch = createShortStream((short) 21, (short) 2, (short) 4, (short) 7, (short) 6, (short) 11, (short) 8, (short) 13).findFirst(s -> {
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

        OptionalShort anyMatch = createShortStream((short) 21, (short) 2, (short) 4, (short) 7, (short) 6, (short) 11, (short) 8, (short) 13)
                .findAny(s -> s > 5 && (s & 1) == 1);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21 || anyMatch.get() == 7 || anyMatch.get() == 11 || anyMatch.get() == 13);

        OptionalShort lastMatch = createShortStream((short) 21, (short) 2, (short) 4, (short) 7, (short) 6, (short) 11, (short) 8, (short) 13).findLast(s -> {
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
        assertHaveSameElements(List.of((short) 1, (short) 2, (short) 3), collectedList);
    }

    @Test
    public void testCollect_SequentialFallback() {
        List<Short> result = createSingleThreadStream((short) 1, (short) 2, (short) 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(Arrays.asList((short) 1, (short) 2, (short) 3), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createShortStream(TEST_ARRAY).anyMatch(s -> s == 26));
        assertFalse(createShortStream(TEST_ARRAY).anyMatch(s -> s == 100));
        assertFalse(createShortStream(new short[] {}).anyMatch(s -> true));
    }

    @Test
    public void testAnyMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream((short) 1, (short) 2, (short) 3).anyMatch(s -> s == 3));
        assertFalse(createSingleThreadStream((short) 1, (short) 2, (short) 3).anyMatch(s -> s > 10));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createShortStream(TEST_ARRAY).allMatch(s -> s >= 1 && s <= 26));
        assertFalse(createShortStream(TEST_ARRAY).allMatch(s -> s < 26));
        assertTrue(createShortStream(new short[] {}).allMatch(s -> false));
    }

    @Test
    public void testAllMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream((short) 1, (short) 2, (short) 3).allMatch(s -> s > 0));
        assertFalse(createSingleThreadStream((short) 1, (short) 2, (short) 3).allMatch(s -> s > 1));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createShortStream(TEST_ARRAY).noneMatch(s -> s > 100));
        assertFalse(createShortStream(TEST_ARRAY).noneMatch(s -> s == 1));
        assertTrue(createShortStream(new short[] {}).noneMatch(s -> true));
    }

    @Test
    public void testNoneMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 10));
        assertFalse(createSingleThreadStream((short) 1, (short) 2, (short) 3).noneMatch(s -> s == 2));
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
    public void testFindFirst_SequentialFallback() {
        OptionalShort found = createSingleThreadStream((short) 1, (short) 2, (short) 3).findFirst(s -> s == 2);
        assertTrue(found.isPresent());
        assertEquals((short) 2, found.get());

        OptionalShort notFound = createSingleThreadStream((short) 1, (short) 2).findFirst(s -> s > 10);
        assertFalse(notFound.isPresent());
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
    public void testFindAny() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        OptionalShort result = stream.findAny(s -> s == 13);
        assertTrue(result.isPresent());
        assertEquals((short) 13, result.get());

        ShortStream stream2 = createShortStream(TEST_ARRAY);
        OptionalShort notFound = stream2.findAny(s -> s == 100);
        assertFalse(notFound.isPresent());

        OptionalShort empty = createShortStream(new short[] {}).findAny(s -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindAny_SequentialFallback() {
        OptionalShort found = createSingleThreadStream((short) 1, (short) 2, (short) 3).findAny(s -> s == 2);
        assertTrue(found.isPresent());

        OptionalShort notFound = createSingleThreadStream((short) 1, (short) 2).findAny(s -> s > 10);
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
    public void testFindLast_SequentialFallback() {
        OptionalShort found = createSingleThreadStream((short) 1, (short) 2, (short) 1, (short) 3).findLast(s -> s == 1);
        assertTrue(found.isPresent());
        assertEquals((short) 1, found.get());

        OptionalShort notFound = createSingleThreadStream((short) 1, (short) 2).findLast(s -> s > 10);
        assertFalse(notFound.isPresent());
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
    public void testZipWithTwoStreams() {
        ShortStream s1 = createShortStream(largeArray).parallel();
        ShortStream s2 = createShortStream(largeArray).parallel();
        long count = s1.zipWith(s2, (a, b) -> (short) (a + b)).count();
        assertEquals(largeArray.length, count);

        long sum = createShortStream(largeArray).parallel().zipWith(createShortStream(largeArray), (a, b) -> (short) (a + b)).sum();
        assertEquals(N.sum(largeArray) * 2, sum);
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
    public void testZipWithDefaultValues_SequentialFallback_UnevenLengths() {
        List<Short> result = ShortStream.of((short) 1, (short) 2, (short) 3)
                .parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1))
                .zipWith(ShortStream.of((short) 10), (short) 0, (short) -1, (a, b) -> (short) (a + b))
                .toList();

        assertEquals(Arrays.asList((short) 11, (short) 1, (short) 2), result);
    }

    @Test
    public void testIsParallel() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
        stream.close();
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
    public void testMaxThreadNum_SequentialFallback() {
        assertEquals(testMaxThreadNum, ((ParallelIteratorShortStream) createShortStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
    }

    @Test
    public void testSplitor_SequentialFallback() {
        assertEquals(Splitor.ITERATOR, ((ParallelIteratorShortStream) createShortStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
    }

    @Test
    public void testAsyncExecutor_SequentialFallback() {
        assertTrue(((ParallelIteratorShortStream) createShortStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
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
