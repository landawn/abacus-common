package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

@Tag("new-test")
public class ParallelArrayLongStream101Test extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final long[] TEST_ARRAY = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L };

    private LongStream parallelStream;

    LongStream createLongStream(long... elements) {
        return LongStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createLongStream(TEST_ARRAY);
    }

    @Test
    @DisplayName("Test filter method")
    public void testFilter() {
        LongStream filtered = parallelStream.filter(l -> l % 2 == 0);
        long[] result = filtered.toArray();

        assertHaveSameElements(new long[] { 2L, 4L, 6L, 8L, 10L }, result);

        parallelStream = createLongStream(TEST_ARRAY);
        filtered = parallelStream.filter(l -> l > 20);
        assertEquals(0, filtered.count());

        parallelStream = createLongStream(TEST_ARRAY);
        filtered = parallelStream.filter(l -> l > 0);
        assertEquals(10, filtered.count());
    }

    @Test
    @DisplayName("Test takeWhile method")
    public void testTakeWhile() {
        LongStream result = parallelStream.takeWhile(l -> l < 5);
        long[] array = result.toArray();

        assertHaveSameElements(new long[] { 1L, 2L, 3L, 4L }, array);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.takeWhile(l -> l < 0);
        assertEquals(0, result.count());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.takeWhile(l -> l <= 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test dropWhile method")
    public void testDropWhile() {
        LongStream result = parallelStream.dropWhile(l -> l < 5);
        long[] array = result.toArray();

        assertHaveSameElements(new long[] { 5L, 6L, 7L, 8L, 9L, 10L }, array);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.dropWhile(l -> l <= 10);
        assertEquals(0, result.count());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.dropWhile(l -> l > 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test map method")
    public void testMap() {
        LongStream mapped = parallelStream.map(l -> l * 2);
        long[] result = mapped.toArray();

        assertHaveSameElements(new long[] { 2L, 4L, 6L, 8L, 10L, 12L, 14L, 16L, 18L, 20L }, result);

        parallelStream = createLongStream(TEST_ARRAY);
        mapped = parallelStream.map(l -> l);
        assertHaveSameElements(TEST_ARRAY, mapped.toArray());
    }

    @Test
    @DisplayName("Test mapToInt method")
    public void testMapToInt() {
        IntStream intStream = parallelStream.mapToInt(l -> (int) (l * 10));
        int[] result = intStream.toArray();

        assertHaveSameElements(new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 }, result);

        parallelStream = createLongStream(TEST_ARRAY);
        intStream = parallelStream.mapToInt(l -> (int) l);
        assertHaveSameElements(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, intStream.toArray());
    }

    @Test
    @DisplayName("Test mapToFloat method")
    public void testMapToFloat() {
        FloatStream floatStream = parallelStream.mapToFloat(l -> l * 0.5f);
        float[] result = floatStream.toArray();

        assertHaveSameElements(new float[] { 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f }, result);

        parallelStream = createLongStream(TEST_ARRAY);
        floatStream = parallelStream.mapToFloat(l -> (float) l);
        assertHaveSameElements(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f }, floatStream.toArray());
    }

    @Test
    @DisplayName("Test mapToDouble method")
    public void testMapToDouble() {
        DoubleStream doubleStream = parallelStream.mapToDouble(l -> l * 0.5);
        double[] result = doubleStream.toArray();

        assertHaveSameElements(new double[] { 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0 }, result);

        parallelStream = createLongStream(TEST_ARRAY);
        doubleStream = parallelStream.mapToDouble(l -> (double) l);
        assertHaveSameElements(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 }, doubleStream.toArray());
    }

    @Test
    @DisplayName("Test mapToObj method")
    public void testMapToObj() {
        List<String> result = parallelStream.mapToObj(l -> "Value: " + l).sorted().toList();

        assertEquals(10, result.size());
        assertEquals("Value: 1", result.get(0));
        assertEquals("Value: 10", result.get(1));
        assertEquals("Value: 9", result.get(9));

        parallelStream = createLongStream(TEST_ARRAY);
        Stream<Long> longObjStream = parallelStream.mapToObj(l -> Long.valueOf(l));
        List<Long> longResult = longObjStream.toList();
        assertHaveSameElements(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L), longResult);
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
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        LongStream flattened = parallelStream.flatmap(l -> new long[] { l, l * 2 });
        long[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createLongStream(new long[] { 1L, 2L, 3L });
        flattened = parallelStream.flatmap(l -> l % 2 == 0 ? new long[] { l } : new long[0]);
        assertArrayEquals(new long[] { 2L }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatMapToInt method")
    public void testFlatMapToInt() {
        IntStream flattened = parallelStream.flatMapToInt(l -> IntStream.of((int) l, (int) (l * 10)));
        int[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1, result[0]);
        assertEquals(100, result[19]);
    }

    @Test
    @DisplayName("Test flatMapToFloat method")
    public void testFlatMapToFloat() {
        FloatStream flattened = parallelStream.flatMapToFloat(l -> FloatStream.of((float) l, (float) (l * 0.1)));
        float[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(0.1f, result[0], 0.0001f);
        assertEquals(10.0f, result[19], 0.0001f);
    }

    @Test
    @DisplayName("Test flatMapToDouble method")
    public void testFlatMapToDouble() {
        DoubleStream flattened = parallelStream.flatMapToDouble(l -> DoubleStream.of((double) l, (double) (l * 0.1)));
        double[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(0.1, result[0], 0.0001);
        assertEquals(10.0, result[19], 0.0001);
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
    @DisplayName("Test onEach method")
    public void testOnEach() {
        List<Long> capturedValues = new ArrayList<>();
        LongConsumer action = it -> {
            synchronized (capturedValues) {
                capturedValues.add(it);
            }
        };
        LongStream result = parallelStream.onEach(action);

        long[] array = result.toArray();

        assertEquals(10, capturedValues.size());
        assertHaveSameElements(TEST_ARRAY, array);

        Collections.sort(capturedValues);
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], capturedValues.get(i).longValue());
        }
    }

    @Test
    @DisplayName("Test forEach method")
    public void testForEach() throws Exception {
        List<Long> result = Collections.synchronizedList(new ArrayList<>());

        parallelStream.forEach(result::add);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).longValue());
        }

        parallelStream = createLongStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.forEach((Throwables.LongConsumer<Exception>) l -> {
                if (l == 5L)
                    throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    @DisplayName("Test toMap method")
    public void testToMap() throws Exception {
        Map<String, Long> result = parallelStream.toMap(l -> "Key" + l, l -> Long.valueOf(l), (v1, v2) -> v1, HashMap::new);

        assertEquals(10, result.size());
        assertEquals(Long.valueOf(1L), result.get("Key1"));
        assertEquals(Long.valueOf(10L), result.get("Key10"));

        parallelStream = createLongStream(new long[] { 1L, 1L, 2L, 2L, 3L });
        Map<String, Long> mergedResult = parallelStream.toMap(l -> "Key" + (l % 2), l -> Long.valueOf(l), Long::sum, HashMap::new);

        assertEquals(2, mergedResult.size());
    }

    @Test
    @DisplayName("Test groupTo method")
    public void testGroupTo() throws Exception {
        Map<String, List<Long>> result = parallelStream.groupTo(l -> l % 2 == 0 ? "even" : "odd", Collectors.toList(), HashMap::new);

        assertEquals(2, result.size());
        assertEquals(5, result.get("even").size());
        assertEquals(5, result.get("odd").size());

        parallelStream = createLongStream(TEST_ARRAY);
        Map<Boolean, Long> countResult = parallelStream.groupTo(l -> l > 5, Collectors.counting(), HashMap::new);

        assertEquals(2, countResult.size());
        assertEquals(5L, countResult.get(true));
        assertEquals(5L, countResult.get(false));
    }

    @Test
    @DisplayName("Test reduce with identity method")
    public void testReduceWithIdentity() {
        long result = parallelStream.reduce(0L, (a, b) -> a + b);

        assertEquals(55L, result);

        parallelStream = createLongStream(new long[] { 1L, 2L, 3L, 4L });
        result = parallelStream.reduce(1L, (a, b) -> a * b);
        assertEquals(24L, result);

        parallelStream = createLongStream(new long[0]);
        result = parallelStream.reduce(10L, (a, b) -> a + b);
        assertEquals(10L, result);
    }

    @Test
    @DisplayName("Test reduce without identity method")
    public void testReduceWithoutIdentity() {
        OptionalLong result = parallelStream.reduce((a, b) -> a + b);

        assertTrue(result.isPresent());
        assertEquals(55L, result.getAsLong());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.reduce((a, b) -> a > b ? a : b);
        assertTrue(result.isPresent());
        assertEquals(10L, result.getAsLong());

        parallelStream = createLongStream(new long[0]);
        result = parallelStream.reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test collect method")
    public void testCollect() {
        List<Long> result = parallelStream.collect(ArrayList::new, (list, l) -> list.add(l), ArrayList::addAll);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).longValue());
        }

        parallelStream = createLongStream(TEST_ARRAY);
        String concatenated = parallelStream.collect(StringBuilder::new, (sb, l) -> sb.append(l).append(","), StringBuilder::append).toString();

        assertTrue(concatenated.contains("1,"));
        assertTrue(concatenated.contains("10,"));
    }

    @Test
    @DisplayName("Test anyMatch method")
    public void testAnyMatch() throws Exception {
        boolean result = parallelStream.anyMatch(l -> l > 5);
        assertTrue(result);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.anyMatch(l -> l > 20);
        assertFalse(result);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.anyMatch(l -> l == 7L);
        assertTrue(result);

        parallelStream = createLongStream(new long[0]);
        result = parallelStream.anyMatch(l -> true);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test allMatch method")
    public void testAllMatch() throws Exception {
        boolean result = parallelStream.allMatch(l -> l > 0);
        assertTrue(result);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.allMatch(l -> l > 5);
        assertFalse(result);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.allMatch(l -> l <= 10);
        assertTrue(result);

        parallelStream = createLongStream(new long[0]);
        result = parallelStream.allMatch(l -> false);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test noneMatch method")
    public void testNoneMatch() throws Exception {
        boolean result = parallelStream.noneMatch(l -> l > 20);
        assertTrue(result);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.noneMatch(l -> l > 5);
        assertFalse(result);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.noneMatch(l -> l == 0);
        assertTrue(result);

        parallelStream = createLongStream(new long[0]);
        result = parallelStream.noneMatch(l -> true);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalLong result = parallelStream.findFirst(l -> l > 5);

        assertTrue(result.isPresent());
        assertEquals(6L, result.getAsLong());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findFirst(l -> l > 20);
        assertFalse(result.isPresent());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findFirst(l -> l == 1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.getAsLong());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalLong result = parallelStream.findAny(l -> l > 5);

        assertTrue(result.isPresent());
        assertTrue(result.getAsLong() > 5);

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findAny(l -> l > 20);
        assertFalse(result.isPresent());

        parallelStream = createLongStream(new long[] { 5L });
        result = parallelStream.findAny(l -> l == 5L);
        assertTrue(result.isPresent());
        assertEquals(5L, result.getAsLong());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalLong result = parallelStream.findLast(l -> l < 5);

        assertTrue(result.isPresent());
        assertEquals(4L, result.getAsLong());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findLast(l -> l > 20);
        assertFalse(result.isPresent());

        parallelStream = createLongStream(TEST_ARRAY);
        result = parallelStream.findLast(l -> l == 10L);
        assertTrue(result.isPresent());
        assertEquals(10L, result.getAsLong());
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
    @DisplayName("Test isParallel method")
    public void testIsParallel() {
        assertTrue(parallelStream.isParallel());
    }

    @Test
    @DisplayName("Test sequential method")
    public void testSequential() {
        LongStream sequential = parallelStream.sequential();

        assertNotNull(sequential);
        assertFalse(sequential.isParallel());

        long[] result = sequential.toArray();
        assertArrayEquals(TEST_ARRAY, result);
    }

    @Test
    @DisplayName("Test onClose method")
    public void testOnClose() {
        AtomicBoolean closeCalled = new AtomicBoolean(false);

        LongStream streamWithCloseHandler = parallelStream.onClose(() -> closeCalled.set(true));

        assertSame(parallelStream, streamWithCloseHandler);

        streamWithCloseHandler.close();
        assertTrue(closeCalled.get());

        parallelStream = createLongStream(TEST_ARRAY);
        LongStream sameStream = parallelStream.onClose(null);
        assertSame(parallelStream, sameStream);
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
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(l -> l > 5).count();
        });
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
}
