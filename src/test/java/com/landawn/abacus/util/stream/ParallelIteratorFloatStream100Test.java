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
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

@Tag("new-test")
public class ParallelIteratorFloatStream100Test extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final float[] TEST_ARRAY = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f };

    private FloatStream parallelStream;

    FloatStream createFloatStream(float... elements) {
        return FloatStream.of(elements).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createFloatStream(TEST_ARRAY);
    }

    @Test
    @DisplayName("Test filter method")
    public void testFilter() {
        FloatStream filtered = parallelStream.filter(f -> f % 2 == 0);
        float[] result = filtered.toArray();

        assertHaveSameElements(new float[] { 2.0f, 4.0f, 6.0f, 8.0f, 10.0f }, result);

        parallelStream = createFloatStream(TEST_ARRAY);
        filtered = parallelStream.filter(f -> f > 20);
        assertEquals(0, filtered.count());

        parallelStream = createFloatStream(TEST_ARRAY);
        filtered = parallelStream.filter(f -> f > 0);
        assertEquals(10, filtered.count());
    }

    @Test
    @DisplayName("Test takeWhile method")
    public void testTakeWhile() {
        FloatStream result = parallelStream.takeWhile(f -> f < 5);
        float[] array = result.toArray();

        assertHaveSameElements(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.takeWhile(f -> f < 0);
        assertEquals(0, result.count());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.takeWhile(f -> f <= 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test dropWhile method")
    public void testDropWhile() {
        FloatStream result = parallelStream.dropWhile(f -> f < 5);
        float[] array = result.toArray();

        assertHaveSameElements(new float[] { 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f }, array);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.dropWhile(f -> f <= 10);
        assertEquals(0, result.count());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.dropWhile(f -> f > 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test map method")
    public void testMap() {
        FloatStream mapped = parallelStream.map(f -> f * 2);
        float[] result = mapped.toArray();

        assertHaveSameElements(new float[] { 2.0f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f, 18.0f, 20.0f }, result);

        parallelStream = createFloatStream(TEST_ARRAY);
        mapped = parallelStream.map(f -> f);
        assertHaveSameElements(TEST_ARRAY, mapped.toArray());
    }

    @Test
    @DisplayName("Test mapToInt method")
    public void testMapToInt() {
        IntStream intStream = parallelStream.mapToInt(f -> (int) (f * 10));
        int[] result = intStream.toArray();

        assertHaveSameElements(new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 }, result);

        parallelStream = createFloatStream(TEST_ARRAY);
        intStream = parallelStream.mapToInt(f -> (int) f);
        assertHaveSameElements(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, intStream.toArray());
    }

    @Test
    @DisplayName("Test mapToLong method")
    public void testMapToLong() {
        LongStream longStream = parallelStream.mapToLong(f -> (long) (f * 100));
        long[] result = longStream.toArray();

        assertHaveSameElements(new long[] { 100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L }, result);

        parallelStream = createFloatStream(TEST_ARRAY);
        longStream = parallelStream.mapToLong(f -> (long) f);
        assertHaveSameElements(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L }, longStream.toArray());
    }

    @Test
    @DisplayName("Test mapToDouble method")
    public void testMapToDouble() {
        DoubleStream doubleStream = parallelStream.mapToDouble(f -> f * 0.5);
        double[] result = doubleStream.toArray();

        assertHaveSameElements(new double[] { 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0 }, result);

        parallelStream = createFloatStream(TEST_ARRAY);
        doubleStream = parallelStream.mapToDouble(f -> (double) f);
        assertHaveSameElements(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 }, doubleStream.toArray());
    }

    @Test
    @DisplayName("Test mapToObj method")
    public void testMapToObj() {
        List<String> result = parallelStream.mapToObj(f -> "Value: " + f).sorted().toList();

        assertEquals(10, result.size());
        assertEquals("Value: 1.0", result.get(0));
        assertEquals("Value: 10.0", result.get(1));
        assertEquals("Value: 9.0", result.get(9));

        parallelStream = createFloatStream(TEST_ARRAY);
        Stream<Float> floatObjStream = parallelStream.mapToObj(f -> Float.valueOf(f));
        List<Float> floatResult = floatObjStream.toList();
        assertHaveSameElements(Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f), floatResult);
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
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        FloatStream flattened = parallelStream.flatmap(f -> new float[] { f, f * 2 });
        float[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f });
        flattened = parallelStream.flatmap(f -> f % 2 == 0 ? new float[] { f } : new float[0]);
        assertArrayEquals(new float[] { 2.0f }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatMapToInt method")
    public void testFlatMapToInt() {
        IntStream flattened = parallelStream.flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10)));
        int[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1, result[0]);
        assertEquals(100, result[19]);
    }

    @Test
    @DisplayName("Test flatMapToLong method")
    public void testFlatMapToLong() {
        LongStream flattened = parallelStream.flatMapToLong(f -> LongStream.of((long) f, (long) (f * 100)));
        long[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1L, result[0]);
        assertEquals(1000L, result[19]);
    }

    @Test
    @DisplayName("Test flatMapToDouble method")
    public void testFlatMapToDouble() {
        DoubleStream flattened = parallelStream.flatMapToDouble(f -> DoubleStream.of(f, f * 0.1));
        double[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(0.1, result[0], 0.0001);
        assertEquals(10.0, result[19], 0.0001);
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
    @DisplayName("Test onEach method")
    public void testOnEach() {
        List<Float> capturedValues = new ArrayList<>();
        FloatConsumer action = it -> {
            synchronized (capturedValues) {
                capturedValues.add(it);
            }
        };
        FloatStream result = parallelStream.onEach(action);

        float[] array = result.toArray();

        assertEquals(10, capturedValues.size());
        assertHaveSameElements(TEST_ARRAY, array);

        Collections.sort(capturedValues);
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], capturedValues.get(i).floatValue());
        }
    }

    @Test
    @DisplayName("Test forEach method")
    public void testForEach() throws Exception {
        List<Float> result = Collections.synchronizedList(new ArrayList<>());

        parallelStream.forEach(result::add);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).floatValue());
        }

        parallelStream = createFloatStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.forEach((Throwables.FloatConsumer<Exception>) f -> {
                if (f == 5.0f)
                    throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    @DisplayName("Test toMap method")
    public void testToMap() throws Exception {
        Map<String, Float> result = parallelStream.toMap(f -> "Key" + f, f -> Float.valueOf(f), (v1, v2) -> v1, HashMap::new);

        assertEquals(10, result.size());
        assertEquals(Float.valueOf(1.0f), result.get("Key1.0"));
        assertEquals(Float.valueOf(10.0f), result.get("Key10.0"));

        parallelStream = createFloatStream(new float[] { 1.0f, 1.0f, 2.0f, 2.0f, 3.0f });
        Map<String, Float> mergedResult = parallelStream.toMap(f -> "Key" + (int) (f % 2), f -> Float.valueOf(f), Float::sum, HashMap::new);

        assertEquals(2, mergedResult.size());
    }

    @Test
    @DisplayName("Test groupTo method")
    public void testGroupTo() throws Exception {
        Map<String, List<Float>> result = parallelStream.groupTo(f -> f % 2 == 0 ? "even" : "odd", Collectors.toList(), HashMap::new);

        assertEquals(2, result.size());
        assertEquals(5, result.get("even").size());
        assertEquals(5, result.get("odd").size());

        parallelStream = createFloatStream(TEST_ARRAY);
        Map<Boolean, Long> countResult = parallelStream.groupTo(f -> f > 5, Collectors.counting(), HashMap::new);

        assertEquals(2, countResult.size());
        assertEquals(5L, countResult.get(true));
        assertEquals(5L, countResult.get(false));
    }

    @Test
    @DisplayName("Test reduce with identity method")
    public void testReduceWithIdentity() {
        float result = parallelStream.reduce(0.0f, (a, b) -> a + b);

        assertEquals(55.0f, result);

        parallelStream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f });
        result = parallelStream.reduce(1.0f, (a, b) -> a * b);
        assertEquals(24.0f, result);

        parallelStream = createFloatStream(new float[0]);
        result = parallelStream.reduce(0.0f, (a, b) -> a + b);
        assertEquals(0.0f, result);
    }

    @Test
    @DisplayName("Test reduce without identity method")
    public void testReduceWithoutIdentity() {
        OptionalFloat result = parallelStream.reduce((a, b) -> a + b);

        assertTrue(result.isPresent());
        assertEquals(55.0f, result.get());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.reduce((a, b) -> a > b ? a : b);
        assertTrue(result.isPresent());
        assertEquals(10.0f, result.get());

        parallelStream = createFloatStream(new float[0]);
        result = parallelStream.reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test collect method")
    public void testCollect() {
        List<Float> result = parallelStream.collect(ArrayList::new, (list, f) -> list.add(f), ArrayList::addAll);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).floatValue());
        }

        parallelStream = createFloatStream(TEST_ARRAY);
        String concatenated = parallelStream.collect(StringBuilder::new, (sb, f) -> sb.append(f).append(","), StringBuilder::append).toString();

        assertTrue(concatenated.contains("1.0,"));
        assertTrue(concatenated.contains("10.0,"));
    }

    @Test
    @DisplayName("Test anyMatch method")
    public void testAnyMatch() throws Exception {
        boolean result = parallelStream.anyMatch(f -> f > 5);
        assertTrue(result);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.anyMatch(f -> f > 20);
        assertFalse(result);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.anyMatch(f -> f == 7.0f);
        assertTrue(result);

        parallelStream = createFloatStream(new float[0]);
        result = parallelStream.anyMatch(f -> true);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test allMatch method")
    public void testAllMatch() throws Exception {
        boolean result = parallelStream.allMatch(f -> f > 0);
        assertTrue(result);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.allMatch(f -> f > 5);
        assertFalse(result);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.allMatch(f -> f <= 10);
        assertTrue(result);

        parallelStream = createFloatStream(new float[0]);
        result = parallelStream.allMatch(f -> false);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test noneMatch method")
    public void testNoneMatch() throws Exception {
        boolean result = parallelStream.noneMatch(f -> f > 20);
        assertTrue(result);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.noneMatch(f -> f > 5);
        assertFalse(result);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.noneMatch(f -> f == 0);
        assertTrue(result);

        parallelStream = createFloatStream(new float[0]);
        result = parallelStream.noneMatch(f -> true);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalFloat result = parallelStream.findFirst(f -> f > 5);

        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findFirst(f -> f > 20);
        assertFalse(result.isPresent());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findFirst(f -> f == 1.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalFloat result = parallelStream.findAny(f -> f > 5);

        assertTrue(result.isPresent());
        assertTrue(result.get() > 5);

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findAny(f -> f > 20);
        assertFalse(result.isPresent());

        parallelStream = createFloatStream(new float[] { 5.0f });
        result = parallelStream.findAny(f -> f == 5.0f);
        assertTrue(result.isPresent());
        assertEquals(5.0f, result.get());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalFloat result = parallelStream.findLast(f -> f < 5);

        assertTrue(result.isPresent());
        assertEquals(4.0f, result.get());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findLast(f -> f > 20);
        assertFalse(result.isPresent());

        parallelStream = createFloatStream(TEST_ARRAY);
        result = parallelStream.findLast(f -> f == 10.0f);
        assertTrue(result.isPresent());
        assertEquals(10.0f, result.get());
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
    @DisplayName("Test isParallel method")
    public void testIsParallel() {
        assertTrue(parallelStream.isParallel());
    }

    @Test
    @DisplayName("Test sequential method")
    public void testSequential() {
        FloatStream sequential = parallelStream.sequential();

        assertNotNull(sequential);
        assertFalse(sequential.isParallel());

        float[] result = sequential.toArray();
        assertArrayEquals(TEST_ARRAY, result);
    }

    @Test
    @DisplayName("Test onClose method")
    public void testOnClose() {
        AtomicBoolean closeCalled = new AtomicBoolean(false);

        FloatStream streamWithCloseHandler = parallelStream.onClose(() -> closeCalled.set(true));

        assertSame(parallelStream, streamWithCloseHandler);

        streamWithCloseHandler.close();
        assertTrue(closeCalled.get());

        parallelStream = createFloatStream(TEST_ARRAY);
        FloatStream sameStream = parallelStream.onClose(null);
        assertSame(parallelStream, sameStream);
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
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(f -> f > 5).count();
        });
    }
}
