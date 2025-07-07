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
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelIteratorIntStream100Test extends TestBase {

    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing 
    private static final int[] TEST_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    private IntStream parallelStream;

    IntStream createIntStream(int... elements) {
        // Will be implemented by the user
        return IntStream.of(elements).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createIntStream(TEST_ARRAY);
    }

    @Test
    @DisplayName("Test filter method")
    public void testFilter() {
        // Test filtering even numbers
        IntStream filtered = parallelStream.filter(i -> i % 2 == 0);
        int[] result = filtered.toArray();

        assertHaveSameElements(new int[] { 2, 4, 6, 8, 10 }, result);

        // Test filtering with no matches
        parallelStream = createIntStream(TEST_ARRAY);
        filtered = parallelStream.filter(i -> i > 20);
        assertEquals(0, filtered.count());

        // Test filtering all elements
        parallelStream = createIntStream(TEST_ARRAY);
        filtered = parallelStream.filter(i -> i > 0);
        assertEquals(10, filtered.count());
    }

    @Test
    @DisplayName("Test takeWhile method")
    public void testTakeWhile() {
        IntStream result = parallelStream.takeWhile(i -> i < 5);
        int[] array = result.toArray();

        assertHaveSameElements(new int[] { 1, 2, 3, 4 }, array);

        // Test takeWhile with no elements matching
        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.takeWhile(i -> i < 0);
        assertEquals(0, result.count());

        // Test takeWhile with all elements matching
        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.takeWhile(i -> i <= 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test dropWhile method")
    public void testDropWhile() {
        IntStream result = parallelStream.dropWhile(i -> i < 5);
        int[] array = result.toArray();

        assertHaveSameElements(new int[] { 5, 6, 7, 8, 9, 10 }, array);

        // Test dropWhile with all elements matching
        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.dropWhile(i -> i <= 10);
        assertEquals(0, result.count());

        // Test dropWhile with no elements matching
        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.dropWhile(i -> i > 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test map method")
    public void testMap() {
        IntStream mapped = parallelStream.map(i -> i * 2);
        int[] result = mapped.toArray();

        assertHaveSameElements(new int[] { 2, 4, 6, 8, 10, 12, 14, 16, 18, 20 }, result);

        // Test map with identity function
        parallelStream = createIntStream(TEST_ARRAY);
        mapped = parallelStream.map(i -> i);
        assertHaveSameElements(TEST_ARRAY, mapped.toArray());
    }

    @Test
    @DisplayName("Test mapToChar method")
    public void testMapToChar() {
        CharStream charStream = parallelStream.mapToChar(i -> (char) ('A' + i - 1));
        char[] result = charStream.toArray();

        assertHaveSameElements(new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J' }, result);

        // Test simple conversion
        parallelStream = createIntStream(new int[] { 65, 66, 67 });
        charStream = parallelStream.mapToChar(i -> (char) i);
        assertHaveSameElements(new char[] { 'A', 'B', 'C' }, charStream.toArray());
    }

    @Test
    @DisplayName("Test mapToByte method")
    public void testMapToByte() {
        ByteStream byteStream = parallelStream.mapToByte(i -> (byte) (i * 10));
        byte[] result = byteStream.toArray();

        assertHaveSameElements(new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 }, result);

        // Test simple conversion
        parallelStream = createIntStream(TEST_ARRAY);
        byteStream = parallelStream.mapToByte(i -> (byte) i);
        assertHaveSameElements(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, byteStream.toArray());
    }

    @Test
    @DisplayName("Test mapToShort method")
    public void testMapToShort() {
        ShortStream shortStream = parallelStream.mapToShort(i -> (short) (i * 100));
        short[] result = shortStream.toArray();

        assertHaveSameElements(new short[] { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 }, result);

        // Test simple conversion
        parallelStream = createIntStream(TEST_ARRAY);
        shortStream = parallelStream.mapToShort(i -> (short) i);
        assertHaveSameElements(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, shortStream.toArray());
    }

    @Test
    @DisplayName("Test mapToLong method")
    public void testMapToLong() {
        LongStream longStream = parallelStream.mapToLong(i -> (long) i * 1000000);
        long[] result = longStream.toArray();

        assertHaveSameElements(new long[] { 1000000L, 2000000L, 3000000L, 4000000L, 5000000L, 6000000L, 7000000L, 8000000L, 9000000L, 10000000L }, result);

        // Test simple conversion
        parallelStream = createIntStream(TEST_ARRAY);
        longStream = parallelStream.mapToLong(i -> (long) i);
        assertHaveSameElements(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L }, longStream.toArray());
    }

    @Test
    @DisplayName("Test mapToFloat method")
    public void testMapToFloat() {
        FloatStream floatStream = parallelStream.mapToFloat(i -> i * 0.5f);
        float[] result = floatStream.toArray();

        assertHaveSameElements(new float[] { 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f }, result);

        // Test simple conversion
        parallelStream = createIntStream(TEST_ARRAY);
        floatStream = parallelStream.mapToFloat(i -> (float) i);
        assertHaveSameElements(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f }, floatStream.toArray());
    }

    @Test
    @DisplayName("Test mapToDouble method")
    public void testMapToDouble() {
        DoubleStream doubleStream = parallelStream.mapToDouble(i -> i * 0.5);
        double[] result = doubleStream.toArray();

        assertHaveSameElements(new double[] { 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0 }, result);

        // Test simple conversion
        parallelStream = createIntStream(TEST_ARRAY);
        doubleStream = parallelStream.mapToDouble(i -> (double) i);
        assertHaveSameElements(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 }, doubleStream.toArray());
    }

    @Test
    @DisplayName("Test mapToObj method")
    public void testMapToObj() {
        List<String> result = parallelStream.mapToObj(i -> "Value: " + i).sorted().toList();

        assertEquals(10, result.size());
        assertEquals("Value: 1", result.get(0));
        assertEquals("Value: 10", result.get(1));
        assertEquals("Value: 9", result.get(9));

        // Test mapping to Integer
        parallelStream = createIntStream(TEST_ARRAY);
        Stream<Integer> intObjStream = parallelStream.mapToObj(i -> Integer.valueOf(i));
        List<Integer> intResult = intObjStream.toList();
        assertHaveSameElements(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), intResult);
    }

    @Test
    @DisplayName("Test flatMap method")
    public void testFlatMap() {
        IntStream flattened = parallelStream.flatMap(i -> IntStream.of(i, i + 10));
        int[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        // Test flatMap with empty streams
        parallelStream = createIntStream(new int[] { 1, 2, 3 });
        flattened = parallelStream.flatMap(i -> i % 2 == 0 ? IntStream.of(i) : IntStream.empty());
        assertArrayEquals(new int[] { 2 }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        IntStream flattened = parallelStream.flatmap(i -> new int[] { i, i * 2 });
        int[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        // Test with empty arrays
        parallelStream = createIntStream(new int[] { 1, 2, 3 });
        flattened = parallelStream.flatmap(i -> i % 2 == 0 ? new int[] { i } : new int[0]);
        assertArrayEquals(new int[] { 2 }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatMapToChar method")
    public void testFlatMapToChar() {
        CharStream flattened = parallelStream.flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        char[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals('A', result[0]);
        assertEquals('j', result[19]);
    }

    @Test
    @DisplayName("Test flatMapToByte method")
    public void testFlatMapToByte() {
        ByteStream flattened = parallelStream.flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        byte[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1, result[0]);
        assertEquals(100, result[19]);
    }

    @Test
    @DisplayName("Test flatMapToShort method")
    public void testFlatMapToShort() {
        ShortStream flattened = parallelStream.flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 100)));
        short[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1, result[0]);
        assertEquals(1000, result[19]);
    }

    @Test
    @DisplayName("Test flatMapToLong method")
    public void testFlatMapToLong() {
        LongStream flattened = parallelStream.flatMapToLong(i -> LongStream.of((long) i, (long) (i * 100)));
        long[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1L, result[0]);
        assertEquals(1000L, result[19]);
    }

    @Test
    @DisplayName("Test flatMapToFloat method")
    public void testFlatMapToFloat() {
        FloatStream flattened = parallelStream.flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 0.1)));
        float[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(0.1f, result[0], 0.0001f);
        assertEquals(10.0f, result[19], 0.0001f);
    }

    @Test
    @DisplayName("Test flatMapToDouble method")
    public void testFlatMapToDouble() {
        DoubleStream flattened = parallelStream.flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 0.1)));
        double[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(0.1, result[0], 0.0001);
        assertEquals(10.0, result[19], 0.0001);
    }

    @Test
    @DisplayName("Test flatMapToObj method")
    public void testFlatMapToObj() {
        Stream<String> flattened = parallelStream.flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B10"));
    }

    @Test
    @DisplayName("Test flatmapToObj with collection method")
    public void testFlatmapToObjCollection() {
        Stream<Integer> flattened = parallelStream.flatmapToObj(i -> Arrays.asList(Integer.valueOf(i), Integer.valueOf(i * 10)));
        List<Integer> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(100), result.get(19));
    }

    @Test
    @DisplayName("Test onEach method")
    public void testOnEach() {
        List<Integer> capturedValues = new ArrayList<>();
        IntConsumer action = it -> {
            synchronized (capturedValues) { // Ensure thread safety
                capturedValues.add(it);
            }
        };
        IntStream result = parallelStream.onEach(action);

        // onEach should not consume the stream
        int[] array = result.toArray();

        assertEquals(10, capturedValues.size());
        assertHaveSameElements(TEST_ARRAY, array);

        // Test that values are captured
        Collections.sort(capturedValues);
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], capturedValues.get(i).intValue());
        }
    }

    @Test
    @DisplayName("Test forEach method")
    public void testForEach() throws Exception {
        List<Integer> result = Collections.synchronizedList(new ArrayList<>());

        parallelStream.forEach(result::add);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).intValue());
        }

        // Test with exception
        parallelStream = createIntStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.forEach((Throwables.IntConsumer<Exception>) i -> {
                if (i == 5)
                    throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    @DisplayName("Test toMap method")
    public void testToMap() throws Exception {
        Map<String, Integer> result = parallelStream.toMap(i -> "Key" + i, i -> Integer.valueOf(i), (v1, v2) -> v1, HashMap::new);

        assertEquals(10, result.size());
        assertEquals(Integer.valueOf(1), result.get("Key1"));
        assertEquals(Integer.valueOf(10), result.get("Key10"));

        // Test with duplicate keys and merge function
        parallelStream = createIntStream(new int[] { 1, 1, 2, 2, 3 });
        Map<String, Integer> mergedResult = parallelStream.toMap(i -> "Key" + (i % 2), i -> Integer.valueOf(i), Integer::sum, HashMap::new);

        assertEquals(2, mergedResult.size());
    }

    @Test
    @DisplayName("Test groupTo method")
    public void testGroupTo() throws Exception {
        Map<String, List<Integer>> result = parallelStream.groupTo(i -> i % 2 == 0 ? "even" : "odd", Collectors.toList(), HashMap::new);

        assertEquals(2, result.size());
        assertEquals(5, result.get("even").size());
        assertEquals(5, result.get("odd").size());

        // Test with custom downstream collector
        parallelStream = createIntStream(TEST_ARRAY);
        Map<Boolean, Long> countResult = parallelStream.groupTo(i -> i > 5, Collectors.counting(), HashMap::new);

        assertEquals(2, countResult.size());
        assertEquals(5L, countResult.get(true));
        assertEquals(5L, countResult.get(false));
    }

    @Test
    @DisplayName("Test reduce with identity method")
    public void testReduceWithIdentity() {
        int result = parallelStream.reduce(0, (a, b) -> a + b);

        assertEquals(55, result);

        // Test with multiplication
        parallelStream = createIntStream(new int[] { 1, 2, 3, 4 });
        result = parallelStream.reduce(1, (a, b) -> a * b);
        assertEquals(24, result);

        // Test with empty stream
        parallelStream = createIntStream(new int[0]);
        result = parallelStream.reduce(0, (a, b) -> a + b);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Test reduce without identity method")
    public void testReduceWithoutIdentity() {
        OptionalInt result = parallelStream.reduce((a, b) -> a + b);

        assertTrue(result.isPresent());
        assertEquals(55, result.getAsInt());

        // Test with max operation
        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.reduce((a, b) -> a > b ? a : b);
        assertTrue(result.isPresent());
        assertEquals(10, result.getAsInt());

        // Test with empty stream
        parallelStream = createIntStream(new int[0]);
        result = parallelStream.reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test collect method")
    public void testCollect() {
        List<Integer> result = parallelStream.collect(ArrayList::new, (list, i) -> list.add(i), ArrayList::addAll);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).intValue());
        }

        // Test with custom collector
        parallelStream = createIntStream(TEST_ARRAY);
        String concatenated = parallelStream.collect(StringBuilder::new, (sb, i) -> sb.append(i).append(","), StringBuilder::append).toString();

        assertTrue(concatenated.contains("1,"));
        assertTrue(concatenated.contains("10,"));
    }

    @Test
    @DisplayName("Test anyMatch method")
    public void testAnyMatch() throws Exception {
        boolean result = parallelStream.anyMatch(i -> i > 5);
        assertTrue(result);

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.anyMatch(i -> i > 20);
        assertFalse(result);

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.anyMatch(i -> i == 7);
        assertTrue(result);

        // Test with empty stream
        parallelStream = createIntStream(new int[0]);
        result = parallelStream.anyMatch(i -> true);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test allMatch method")
    public void testAllMatch() throws Exception {
        boolean result = parallelStream.allMatch(i -> i > 0);
        assertTrue(result);

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.allMatch(i -> i > 5);
        assertFalse(result);

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.allMatch(i -> i <= 10);
        assertTrue(result);

        // Test with empty stream
        parallelStream = createIntStream(new int[0]);
        result = parallelStream.allMatch(i -> false);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test noneMatch method")
    public void testNoneMatch() throws Exception {
        boolean result = parallelStream.noneMatch(i -> i > 20);
        assertTrue(result);

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.noneMatch(i -> i > 5);
        assertFalse(result);

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.noneMatch(i -> i == 0);
        assertTrue(result);

        // Test with empty stream
        parallelStream = createIntStream(new int[0]);
        result = parallelStream.noneMatch(i -> true);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalInt result = parallelStream.findFirst(i -> i > 5);

        assertTrue(result.isPresent());
        assertEquals(6, result.getAsInt());

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findFirst(i -> i > 20);
        assertFalse(result.isPresent());

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findFirst(i -> i == 1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalInt result = parallelStream.findAny(i -> i > 5);

        assertTrue(result.isPresent());
        assertTrue(result.getAsInt() > 5);

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findAny(i -> i > 20);
        assertFalse(result.isPresent());

        parallelStream = createIntStream(new int[] { 5 });
        result = parallelStream.findAny(i -> i == 5);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalInt result = parallelStream.findLast(i -> i < 5);

        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findLast(i -> i > 20);
        assertFalse(result.isPresent());

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findLast(i -> i == 10);
        assertTrue(result.isPresent());
        assertEquals(10, result.getAsInt());
    }

    @Test
    @DisplayName("Test zipWith two streams with binary operator")
    public void testZipWithBinary() {
        IntStream stream2 = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        IntStream zipped = parallelStream.zipWith(stream2, (a, b) -> a + b);
        int[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new int[] { 11, 22, 33, 44, 55 }, result);

        // Test with different length streams
        parallelStream = createIntStream(new int[] { 1, 2, 3 });
        stream2 = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        zipped = parallelStream.zipWith(stream2, (a, b) -> a * b);
        assertEquals(3, zipped.count());
    }

    @Test
    @DisplayName("Test zipWith three streams with ternary operator")
    public void testZipWithTernary() {
        IntStream stream2 = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        IntStream stream3 = createIntStream(new int[] { 100, 100, 100, 100, 100 });
        int[] result = parallelStream.zipWith(stream2, stream3, (a, b, c) -> (a + b) * c / 100).toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new int[] { 11, 22, 33, 44, 55 }, result);
    }

    @Test
    @DisplayName("Test zipWith two streams with default values")
    public void testZipWithBinaryDefaults() {
        IntStream stream2 = createIntStream(new int[] { 10, 20, 30 });
        int[] result = parallelStream.zipWith(stream2, 0, 100, (a, b) -> a + b).sorted().toArray();

        assertEquals(10, result.length);
        // First 3 elements: 1+10, 2+20, 3+30
        assertEquals(11, result[0]);
        assertEquals(22, result[1]);
        assertEquals(33, result[2]);
        // the next elements: 4+100, 5+100, etc.
        assertEquals(104, result[3]);
    }

    @Test
    @DisplayName("Test zipWith three streams with default values")
    public void testZipWithTernaryDefaults() {
        IntStream stream2 = createIntStream(new int[] { 10, 20 });
        IntStream stream3 = createIntStream(new int[] { 1, 2, 3, 4 });
        int[] result = parallelStream.zipWith(stream2, stream3, 0, 0, 1, (a, b, c) -> a + b + c).sorted().toArray();

        assertEquals(10, result.length);
        // Check first two elements
        assertEquals(6, result[0]);
        assertEquals(6, result[1]);
        assertEquals(24, result[9]);
    }

    @Test
    @DisplayName("Test isParallel method")
    public void testIsParallel() {
        assertTrue(parallelStream.isParallel());
    }

    @Test
    @DisplayName("Test sequential method")
    public void testSequential() {
        IntStream sequential = parallelStream.sequential();

        assertNotNull(sequential);
        assertFalse(sequential.isParallel());

        // Verify data is preserved
        int[] result = sequential.toArray();
        assertArrayEquals(TEST_ARRAY, result);
    }

    @Test
    @DisplayName("Test onClose method")
    public void testOnClose() {
        AtomicBoolean closeCalled = new AtomicBoolean(false);

        IntStream streamWithCloseHandler = parallelStream.onClose(() -> closeCalled.set(true));

        // onClose should return a new stream
        assertNotSame(parallelStream, streamWithCloseHandler);

        // Close handler should be called when stream is closed
        streamWithCloseHandler.close();
        assertTrue(closeCalled.get());

        // Test with null close handler
        parallelStream = createIntStream(TEST_ARRAY);
        IntStream sameStream = parallelStream.onClose(null);
        // Should return the same stream instance for null handler
        assertSame(parallelStream, sameStream);
    }

    @Test
    @DisplayName("Test complex parallel operations")
    public void testComplexParallelOperations() {
        // Test chaining multiple operations
        int[] result = parallelStream.filter(i -> i % 2 == 0).map(i -> i * 2).sorted().toArray();

        assertArrayEquals(new int[] { 4, 8, 12, 16, 20 }, result);

        // Test parallel reduction
        parallelStream = createIntStream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        int sum = parallelStream.map(i -> i * 2).reduce(0, Integer::sum);

        assertEquals(110, sum);
    }

    @Test
    @DisplayName("Test edge cases")
    public void testEdgeCases() {
        // Test with empty array
        parallelStream = createIntStream(new int[0]);
        assertEquals(0, parallelStream.count());

        // Test with single element
        parallelStream = createIntStream(new int[] { 42 });
        assertEquals(42, parallelStream.first().getAsInt());

        // Test with negative values
        parallelStream = createIntStream(new int[] { -5, -3, -1, 0, 1, 3, 5 });
        long positiveCount = parallelStream.filter(i -> i > 0).count();
        assertEquals(3, positiveCount);

        // Test with max and min values
        parallelStream = createIntStream(new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE });
        assertEquals(5, parallelStream.count());
    }

    @Test
    @DisplayName("Test thread safety")
    public void testThreadSafety() throws Exception {
        // Test concurrent modification during parallel operation
        AtomicInteger counter = new AtomicInteger(0);

        parallelStream.forEach(i -> {
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
            parallelStream.filter(i -> {
                if (i == 5)
                    throw new RuntimeException("Test exception in filter");
                return true;
            }).count();
        });

        // Test exception in map
        parallelStream = createIntStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.map(i -> {
                if (i == 7)
                    throw new RuntimeException("Test exception in map");
                return i;
            }).toArray();
        });
    }

    @Test
    @DisplayName("Test with large arrays")
    public void testLargeArrays() {
        // Create a large array
        int[] largeArray = new int[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i + 1;
        }

        parallelStream = createIntStream(largeArray);

        // Test sum of large array
        int sum = parallelStream.reduce(0, Integer::sum);
        assertEquals(500500, sum);

        // Test filtering on large array
        parallelStream = createIntStream(largeArray);
        long evenCount = parallelStream.filter(i -> i % 2 == 0).count();
        assertEquals(500, evenCount);
    }

    @Test
    @DisplayName("Test method chaining")
    public void testMethodChaining() {
        // Complex chaining test
        List<String> result = parallelStream.filter(i -> i > 3)
                .map(i -> i * 2)
                .flatMapToObj(i -> Stream.of("Value: " + i, "Half: " + (i / 2)))
                .sorted()
                .toList();

        assertTrue(result.size() > 0);
        assertTrue(result.contains("Value: 8"));
        assertTrue(result.contains("Half: 4"));
    }

    @Test
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        // Test that stream is closed after terminal operation
        parallelStream.count();

        // Should throw exception when trying to use closed stream
        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(i -> i > 5).count();
        });
    }

    @Test
    @DisplayName("Test with specific int patterns")
    public void testSpecificIntPatterns() {
        // Test with powers of 2
        parallelStream = createIntStream(new int[] { 1, 2, 4, 8, 16, 32, 64, 128 });
        int[] powersOfTwo = parallelStream.filter(i -> (i & (i - 1)) == 0).toArray();
        assertEquals(8, powersOfTwo.length);

        // Test with arithmetic progression
        parallelStream = createIntStream(new int[] { 3, 6, 9, 12, 15 });
        boolean allDivisibleBy3 = parallelStream.allMatch(i -> i % 3 == 0);
        assertTrue(allDivisibleBy3);
    }

    @Test
    @DisplayName("Test overflow scenarios")
    public void testOverflowScenarios() {
        // Test potential overflow with large values
        parallelStream = createIntStream(new int[] { Integer.MAX_VALUE - 1, 1, 1 });

        // This should handle overflow gracefully or throw appropriate exception
        try {
            int sum = parallelStream.reduce(0, Integer::sum);
            // If no overflow exception, check if result is reasonable
            assertTrue(sum < 0 || sum == Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            // Expected if overflow checking is enabled
            assertTrue(true);
        }
    }
}
