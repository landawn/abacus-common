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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelArrayDoubleStream101Test extends TestBase {

    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing 
    private static final double[] TEST_ARRAY = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };

    private DoubleStream parallelStream;

    DoubleStream createDoubleStream(double... elements) {
        // Will be implemented by the user
        return DoubleStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createDoubleStream(TEST_ARRAY);
    }

    @Test
    @DisplayName("Test filter method")
    public void testFilter() {
        // Test filtering even numbers
        DoubleStream filtered = parallelStream.filter(d -> d % 2 == 0);
        double[] result = filtered.toArray();

        assertHaveSameElements(new double[] { 2.0, 4.0, 6.0, 8.0, 10.0 }, result);

        // Test filtering with no matches
        parallelStream = createDoubleStream(TEST_ARRAY);
        filtered = parallelStream.filter(d -> d > 20);
        assertEquals(0, filtered.count());

        // Test filtering all elements
        parallelStream = createDoubleStream(TEST_ARRAY);
        filtered = parallelStream.filter(d -> d > 0);
        assertEquals(10, filtered.count());
    }

    @Test
    @DisplayName("Test takeWhile method")
    public void testTakeWhile() {
        DoubleStream result = parallelStream.takeWhile(d -> d < 5);
        double[] array = result.toArray();

        assertHaveSameElements(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);

        // Test takeWhile with no elements matching
        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.takeWhile(d -> d < 0);
        assertEquals(0, result.count());

        // Test takeWhile with all elements matching
        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.takeWhile(d -> d <= 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test dropWhile method")
    public void testDropWhile() {
        DoubleStream result = parallelStream.dropWhile(d -> d < 5);
        double[] array = result.toArray();

        assertHaveSameElements(new double[] { 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 }, array);

        // Test dropWhile with all elements matching
        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.dropWhile(d -> d <= 10);
        assertEquals(0, result.count());

        // Test dropWhile with no elements matching
        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.dropWhile(d -> d > 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test map method")
    public void testMap() {
        DoubleStream mapped = parallelStream.map(d -> d * 2);
        double[] result = mapped.toArray();

        assertHaveSameElements(new double[] { 2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0 }, result);

        // Test map with identity function
        parallelStream = createDoubleStream(TEST_ARRAY);
        mapped = parallelStream.map(d -> d);
        assertHaveSameElements(TEST_ARRAY, mapped.toArray());
    }

    @Test
    @DisplayName("Test mapToInt method")
    public void testMapToInt() {
        IntStream intStream = parallelStream.mapToInt(d -> (int) (d * 10));
        int[] result = intStream.toArray();

        assertHaveSameElements(new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 }, result);

        // Test simple conversion
        parallelStream = createDoubleStream(TEST_ARRAY);
        intStream = parallelStream.mapToInt(d -> (int) d);
        assertHaveSameElements(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, intStream.toArray());
    }

    @Test
    @DisplayName("Test mapToLong method")
    public void testMapToLong() {
        LongStream longStream = parallelStream.mapToLong(d -> (long) (d * 100));
        long[] result = longStream.toArray();

        assertHaveSameElements(new long[] { 100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L }, result);

        // Test simple conversion
        parallelStream = createDoubleStream(TEST_ARRAY);
        longStream = parallelStream.mapToLong(d -> (long) d);
        assertHaveSameElements(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L }, longStream.toArray());
    }

    @Test
    @DisplayName("Test mapToFloat method")
    public void testMapToFloat() {
        FloatStream floatStream = parallelStream.mapToFloat(d -> (float) (d * 0.5));
        float[] result = floatStream.toArray();

        assertHaveSameElements(new float[] { 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f }, result);

        // Test simple conversion
        parallelStream = createDoubleStream(TEST_ARRAY);
        floatStream = parallelStream.mapToFloat(d -> (float) d);
        assertHaveSameElements(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f }, floatStream.toArray());
    }

    @Test
    @DisplayName("Test mapToObj method")
    public void testMapToObj() {
        List<String> result = parallelStream.mapToObj(d -> "Value: " + d).sorted().toList();

        assertEquals(10, result.size());
        assertEquals("Value: 1.0", result.get(0));
        assertEquals("Value: 10.0", result.get(1));
        assertEquals("Value: 9.0", result.get(9));

        // Test mapping to Double
        parallelStream = createDoubleStream(TEST_ARRAY);
        Stream<Double> doubleObjStream = parallelStream.mapToObj(d -> Double.valueOf(d));
        List<Double> doubleResult = doubleObjStream.toList();
        assertHaveSameElements(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0), doubleResult);
    }

    @Test
    @DisplayName("Test flatMap method")
    public void testFlatMap() {
        DoubleStream flattened = parallelStream.flatMap(d -> DoubleStream.of(d, d + 10));
        double[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        // Test flatMap with empty streams
        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        flattened = parallelStream.flatMap(d -> d % 2 == 0 ? DoubleStream.of(d) : DoubleStream.empty());
        assertArrayEquals(new double[] { 2.0 }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        DoubleStream flattened = parallelStream.flatmap(d -> new double[] { d, d * 2 });
        double[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        // Test with empty arrays
        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        flattened = parallelStream.flatmap(d -> d % 2 == 0 ? new double[] { d } : new double[0]);
        assertArrayEquals(new double[] { 2.0 }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatMapToInt method")
    public void testFlatMapToInt() {
        IntStream flattened = parallelStream.flatMapToInt(d -> IntStream.of((int) d, (int) (d * 10)));
        int[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1, result[0]);
        assertEquals(100, result[19]);
    }

    @Test
    @DisplayName("Test flatMapToLong method")
    public void testFlatMapToLong() {
        LongStream flattened = parallelStream.flatMapToLong(d -> LongStream.of((long) d, (long) (d * 100)));
        long[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1L, result[0]);
        assertEquals(1000L, result[19]);
    }

    @Test
    @DisplayName("Test flatMapToFloat method")
    public void testFlatMapToFloat() {
        FloatStream flattened = parallelStream.flatMapToFloat(d -> FloatStream.of((float) d, (float) (d * 0.1)));
        float[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(0.1f, result[0], 0.0001f);
        assertEquals(10.0f, result[19], 0.0001f);
    }

    @Test
    @DisplayName("Test flatMapToObj method")
    public void testFlatMapToObj() {
        Stream<String> flattened = parallelStream.flatMapToObj(d -> Stream.of("A" + d, "B" + d));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1.0"));
        assertTrue(result.contains("B10.0"));
    }

    @Test
    @DisplayName("Test flatmapToObj with collection method")
    public void testFlatmapToObjCollection() {
        Stream<Double> flattened = parallelStream.flatmapToObj(d -> Arrays.asList(Double.valueOf(d), Double.valueOf(d * 10)));
        List<Double> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertEquals(Double.valueOf(1.0), result.get(0));
        assertEquals(Double.valueOf(100.0), result.get(19));
    }

    @Test
    @DisplayName("Test onEach method")
    public void testOnEach() {
        List<Double> capturedValues = new ArrayList<>();
        DoubleConsumer action = it -> {
            synchronized (capturedValues) { // Ensure thread safety
                capturedValues.add(it);
            }
        };
        DoubleStream result = parallelStream.onEach(action);

        // onEach should not consume the stream
        double[] array = result.toArray();

        assertEquals(10, capturedValues.size());
        assertHaveSameElements(TEST_ARRAY, array);

        // Test that values are captured
        Collections.sort(capturedValues);
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], capturedValues.get(i).doubleValue());
        }
    }

    @Test
    @DisplayName("Test forEach method")
    public void testForEach() throws Exception {
        List<Double> result = Collections.synchronizedList(new ArrayList<>());

        parallelStream.forEach(result::add);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).doubleValue());
        }

        // Test with exception
        parallelStream = createDoubleStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.forEach((Throwables.DoubleConsumer<Exception>) d -> {
                if (d == 5.0)
                    throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    @DisplayName("Test toMap method")
    public void testToMap() throws Exception {
        Map<String, Double> result = parallelStream.toMap(d -> "Key" + d, d -> Double.valueOf(d), (v1, v2) -> v1, HashMap::new);

        assertEquals(10, result.size());
        assertEquals(Double.valueOf(1.0), result.get("Key1.0"));
        assertEquals(Double.valueOf(10.0), result.get("Key10.0"));

        // Test with duplicate keys and merge function
        parallelStream = createDoubleStream(new double[] { 1.0, 1.0, 2.0, 2.0, 3.0 });
        Map<String, Double> mergedResult = parallelStream.toMap(d -> "Key" + (int) (d % 2), d -> Double.valueOf(d), Double::sum, HashMap::new);

        assertEquals(2, mergedResult.size());
    }

    @Test
    @DisplayName("Test groupTo method")
    public void testGroupTo() throws Exception {
        Map<String, List<Double>> result = parallelStream.groupTo(d -> d % 2 == 0 ? "even" : "odd", Collectors.toList(), HashMap::new);

        assertEquals(2, result.size());
        assertEquals(5, result.get("even").size());
        assertEquals(5, result.get("odd").size());

        // Test with custom downstream collector
        parallelStream = createDoubleStream(TEST_ARRAY);
        Map<Boolean, Long> countResult = parallelStream.groupTo(d -> d > 5, Collectors.counting(), HashMap::new);

        assertEquals(2, countResult.size());
        assertEquals(5L, countResult.get(true));
        assertEquals(5L, countResult.get(false));
    }

    @Test
    @DisplayName("Test reduce with identity method")
    public void testReduceWithIdentity() {
        double result = parallelStream.reduce(0.0, (a, b) -> a + b);

        assertEquals(55.0, result);

        // Test with multiplication
        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0 });
        result = parallelStream.reduce(1.0, (a, b) -> a * b);
        assertEquals(24.0, result);

        // Test with empty stream
        parallelStream = createDoubleStream(new double[0]);
        result = parallelStream.reduce(10.0, (a, b) -> a + b);
        assertEquals(10.0, result);
    }

    @Test
    @DisplayName("Test reduce without identity method")
    public void testReduceWithoutIdentity() {
        OptionalDouble result = parallelStream.reduce((a, b) -> a + b);

        assertTrue(result.isPresent());
        assertEquals(55.0, result.getAsDouble());

        // Test with max operation
        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.reduce((a, b) -> a > b ? a : b);
        assertTrue(result.isPresent());
        assertEquals(10.0, result.getAsDouble());

        // Test with empty stream
        parallelStream = createDoubleStream(new double[0]);
        result = parallelStream.reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test collect method")
    public void testCollect() {
        List<Double> result = parallelStream.collect(ArrayList::new, (list, d) -> list.add(d), ArrayList::addAll);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).doubleValue());
        }

        // Test with custom collector
        parallelStream = createDoubleStream(TEST_ARRAY);
        String concatenated = parallelStream.collect(StringBuilder::new, (sb, d) -> sb.append(d).append(","), StringBuilder::append).toString();

        assertTrue(concatenated.contains("1.0,"));
        assertTrue(concatenated.contains("10.0,"));
    }

    @Test
    @DisplayName("Test anyMatch method")
    public void testAnyMatch() throws Exception {
        boolean result = parallelStream.anyMatch(d -> d > 5);
        assertTrue(result);

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.anyMatch(d -> d > 20);
        assertFalse(result);

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.anyMatch(d -> d == 7.0);
        assertTrue(result);

        // Test with empty stream
        parallelStream = createDoubleStream(new double[0]);
        result = parallelStream.anyMatch(d -> true);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test allMatch method")
    public void testAllMatch() throws Exception {
        boolean result = parallelStream.allMatch(d -> d > 0);
        assertTrue(result);

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.allMatch(d -> d > 5);
        assertFalse(result);

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.allMatch(d -> d <= 10);
        assertTrue(result);

        // Test with empty stream
        parallelStream = createDoubleStream(new double[0]);
        result = parallelStream.allMatch(d -> false);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test noneMatch method")
    public void testNoneMatch() throws Exception {
        boolean result = parallelStream.noneMatch(d -> d > 20);
        assertTrue(result);

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.noneMatch(d -> d > 5);
        assertFalse(result);

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.noneMatch(d -> d == 0);
        assertTrue(result);

        // Test with empty stream
        parallelStream = createDoubleStream(new double[0]);
        result = parallelStream.noneMatch(d -> true);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalDouble result = parallelStream.findFirst(d -> d > 5);

        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble());

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findFirst(d -> d > 20);
        assertFalse(result.isPresent());

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findFirst(d -> d == 1.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalDouble result = parallelStream.findAny(d -> d > 5);

        assertTrue(result.isPresent());
        assertTrue(result.getAsDouble() > 5);

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findAny(d -> d > 20);
        assertFalse(result.isPresent());

        parallelStream = createDoubleStream(new double[] { 5.0 });
        result = parallelStream.findAny(d -> d == 5.0);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalDouble result = parallelStream.findLast(d -> d < 5);

        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble());

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findLast(d -> d > 20);
        assertFalse(result.isPresent());

        parallelStream = createDoubleStream(TEST_ARRAY);
        result = parallelStream.findLast(d -> d == 10.0);
        assertTrue(result.isPresent());
        assertEquals(10.0, result.getAsDouble());
    }

    @Test
    @DisplayName("Test zipWith two streams with binary operator")
    public void testZipWithBinary() {
        DoubleStream stream2 = createDoubleStream(new double[] { 10.0, 20.0, 30.0, 40.0, 50.0 });
        DoubleStream zipped = parallelStream.zipWith(stream2, (a, b) -> a + b);
        double[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new double[] { 11.0, 22.0, 33.0, 44.0, 55.0 }, result);

        // Test with different length streams
        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        stream2 = createDoubleStream(new double[] { 10.0, 20.0, 30.0, 40.0, 50.0 });
        zipped = parallelStream.zipWith(stream2, (a, b) -> a * b);
        assertEquals(3, zipped.count());
    }

    @Test
    @DisplayName("Test zipWith three streams with ternary operator")
    public void testZipWithTernary() {
        DoubleStream stream2 = createDoubleStream(new double[] { 10.0, 20.0, 30.0, 40.0, 50.0 });
        DoubleStream stream3 = createDoubleStream(new double[] { 100.0, 100.0, 100.0, 100.0, 100.0 });
        DoubleStream zipped = parallelStream.zipWith(stream2, stream3, (a, b, c) -> (a + b) * c / 100);
        double[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new double[] { 11.0, 22.0, 33.0, 44.0, 55.0 }, result);
    }

    @Test
    @DisplayName("Test zipWith two streams with default values")
    public void testZipWithBinaryDefaults() {
        DoubleStream stream2 = createDoubleStream(new double[] { 10.0, 20.0, 30.0 });
        double[] result = parallelStream.zipWith(stream2, 0.0, 100.0, (a, b) -> a + b).sorted().toArray();

        assertEquals(10, result.length);
        // First 3 elements: 1+10, 2+20, 3+30
        assertEquals(11.0, result[0]);
        assertEquals(22.0, result[1]);
        assertEquals(33.0, result[2]);
        // the next elements: 4+100, 5+100, etc.
        assertEquals(104.0, result[3]);
    }

    @Test
    @DisplayName("Test zipWith three streams with default values")
    public void testZipWithTernaryDefaults() {
        DoubleStream stream2 = createDoubleStream(new double[] { 10.0, 20.0 });
        DoubleStream stream3 = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0 });
        double[] result = parallelStream.zipWith(stream2, stream3, 0.0, 0.0, 1.0, (a, b, c) -> a + b + c).sorted().toArray();

        assertEquals(10, result.length);
        // Check first two elements
        assertEquals(6.0, result[0]); // 1 + 10 + 1
        assertEquals(24.0, result[9]); // 2 + 20 + 2
    }

    @Test
    @DisplayName("Test isParallel method")
    public void testIsParallel() {
        assertTrue(parallelStream.isParallel());
    }

    @Test
    @DisplayName("Test sequential method")
    public void testSequential() {
        DoubleStream sequential = parallelStream.sequential();

        assertNotNull(sequential);
        assertFalse(sequential.isParallel());

        // Verify data is preserved
        double[] result = sequential.toArray();
        assertArrayEquals(TEST_ARRAY, result);
    }

    @Test
    @DisplayName("Test onClose method")
    public void testOnClose() {
        AtomicBoolean closeCalled = new AtomicBoolean(false);

        DoubleStream streamWithCloseHandler = parallelStream.onClose(() -> closeCalled.set(true));

        // onClose should return a new stream
        assertNotSame(parallelStream, streamWithCloseHandler);

        // Close handler should be called when stream is closed
        streamWithCloseHandler.close();
        assertTrue(closeCalled.get());

        // Test with null close handler
        parallelStream = createDoubleStream(TEST_ARRAY);
        DoubleStream sameStream = parallelStream.onClose(null);
        // Should return the same stream instance for null handler
        assertSame(parallelStream, sameStream);
    }

    @Test
    @DisplayName("Test complex parallel operations")
    public void testComplexParallelOperations() {
        // Test chaining multiple operations
        double[] result = parallelStream.filter(d -> d % 2 == 0).map(d -> d * 2).sorted().toArray();

        assertArrayEquals(new double[] { 4.0, 8.0, 12.0, 16.0, 20.0 }, result);

        // Test parallel reduction
        parallelStream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 });
        double sum = parallelStream.map(d -> d * 2).reduce(0.0, Double::sum);

        assertEquals(110.0, sum);
    }

    @Test
    @DisplayName("Test edge cases")
    public void testEdgeCases() {
        // Test with empty array
        parallelStream = createDoubleStream(new double[0]);
        assertEquals(0, parallelStream.count());

        // Test with single element
        parallelStream = createDoubleStream(new double[] { 42.5 });
        assertEquals(42.5, parallelStream.first().getAsDouble());

        // Test with negative values
        parallelStream = createDoubleStream(new double[] { -5.0, -3.0, -1.0, 0.0, 1.0, 3.0, 5.0 });
        long positiveCount = parallelStream.filter(d -> d > 0).count();
        assertEquals(3, positiveCount);

        // Test with NaN and infinity
        parallelStream = createDoubleStream(new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0 });
        long finiteCount = parallelStream.filter(Double::isFinite).count();
        assertEquals(1, finiteCount);
    }

    @Test
    @DisplayName("Test thread safety")
    public void testThreadSafety() throws Exception {
        // Test concurrent modification during parallel operation
        AtomicInteger counter = new AtomicInteger(0);

        parallelStream.forEach(d -> {
            counter.incrementAndGet();
            // Simulate some work
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
        // Test exception in filter
        assertThrows(RuntimeException.class, () -> {
            parallelStream.filter(d -> {
                if (d == 5.0)
                    throw new RuntimeException("Test exception in filter");
                return true;
            }).count();
        });

        // Test exception in map
        parallelStream = createDoubleStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.map(d -> {
                if (d == 7.0)
                    throw new RuntimeException("Test exception in map");
                return d;
            }).toArray();
        });
    }

    @Test
    @DisplayName("Test precision in calculations")
    public void testDoublePrecision() {
        // Test with values that might have precision issues
        parallelStream = createDoubleStream(new double[] { 0.1, 0.2, 0.3 });
        double sum = parallelStream.reduce(0.0, Double::sum);

        // Use delta for floating point comparison
        assertEquals(0.6, sum, 0.0000001);

        // Test with very small values
        parallelStream = createDoubleStream(new double[] { 1e-15, 1e-15, 1e-15 });
        double smallSum = parallelStream.reduce(0.0, Double::sum);
        assertEquals(3e-15, smallSum, 1e-20);
    }

    @Test
    @DisplayName("Test with large arrays")
    public void testLargeArrays() {
        // Create a large array
        double[] largeArray = new double[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i + 1.0;
        }

        parallelStream = createDoubleStream(largeArray);

        // Test sum of large array
        double sum = parallelStream.reduce(0.0, Double::sum);
        assertEquals(500500.0, sum);

        // Test filtering on large array
        parallelStream = createDoubleStream(largeArray);
        long evenCount = parallelStream.filter(d -> d % 2 == 0).count();
        assertEquals(500, evenCount);
    }

    @Test
    @DisplayName("Test method chaining")
    public void testMethodChaining() {
        // Complex chaining test
        List<String> result = parallelStream.filter(d -> d > 3)
                .map(d -> d * 2)
                .flatMapToObj(d -> Stream.of("Value: " + d, "Half: " + (d / 2)))
                .sorted()
                .toList();

        assertTrue(result.size() > 0);
        assertTrue(result.contains("Value: 8.0"));
        assertTrue(result.contains("Half: 4.0"));
    }

    @Test
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        // Test that stream is closed after terminal operation
        parallelStream.count();

        // Should throw exception when trying to use closed stream
        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(d -> d > 5).count();
        });
    }

    @Test
    @DisplayName("Test special double values")
    public void testSpecialDoubleValues() {
        // Test with special values
        parallelStream = createDoubleStream(new double[] { Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 0.0, -0.0 });

        // Count finite values
        long finiteCount = parallelStream.filter(Double::isFinite).count();
        assertEquals(5, finiteCount); // MIN_VALUE, MAX_VALUE, MIN_NORMAL, 0.0, -0.0

        // Test NaN handling
        parallelStream = createDoubleStream(new double[] { Double.NaN, 1.0, 2.0 });
        OptionalDouble max = parallelStream.reduce(Double::max);
        assertTrue(max.isPresent());
        assertTrue(Double.isNaN(max.getAsDouble()));
    }
}
