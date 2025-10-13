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
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

@Tag("new-test")
public class ParallelArrayShortStream100Test extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final short[] TEST_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private ShortStream parallelStream;

    ShortStream createShortStream(short... elements) {
        return ShortStream.of(elements).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createShortStream(TEST_ARRAY);
    }

    @Test
    @DisplayName("Test filter method")
    public void testFilter() {
        ShortStream filtered = parallelStream.filter(s -> s % 2 == 0);
        short[] result = filtered.toArray();

        assertHaveSameElements(new short[] { 2, 4, 6, 8, 10 }, result);

        parallelStream = createShortStream(TEST_ARRAY);
        filtered = parallelStream.filter(s -> s > 20);
        assertEquals(0, filtered.count());

        parallelStream = createShortStream(TEST_ARRAY);
        filtered = parallelStream.filter(s -> s > 0);
        assertEquals(10, filtered.count());
    }

    @Test
    @DisplayName("Test takeWhile method")
    public void testTakeWhile() {
        ShortStream result = parallelStream.takeWhile(s -> s < 5);
        short[] array = result.toArray();

        assertHaveSameElements(new short[] { 1, 2, 3, 4 }, array);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.takeWhile(s -> s < 0);
        assertEquals(0, result.count());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.takeWhile(s -> s <= 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test dropWhile method")
    public void testDropWhile() {
        ShortStream result = parallelStream.dropWhile(s -> s < 5);
        short[] array = result.toArray();

        assertHaveSameElements(new short[] { 5, 6, 7, 8, 9, 10 }, array);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.dropWhile(s -> s <= 10);
        assertEquals(0, result.count());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.dropWhile(s -> s > 10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("Test map method")
    public void testMap() {
        ShortStream mapped = parallelStream.map(s -> (short) (s * 2));
        short[] result = mapped.toArray();

        assertHaveSameElements(new short[] { 2, 4, 6, 8, 10, 12, 14, 16, 18, 20 }, result);

        parallelStream = createShortStream(TEST_ARRAY);
        mapped = parallelStream.map(s -> s);
        assertHaveSameElements(TEST_ARRAY, mapped.toArray());
    }

    @Test
    @DisplayName("Test mapToInt method")
    public void testMapToInt() {
        IntStream intStream = parallelStream.mapToInt(s -> s * 10);
        int[] result = intStream.toArray();

        assertHaveSameElements(new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 }, result);

        parallelStream = createShortStream(TEST_ARRAY);
        intStream = parallelStream.mapToInt(s -> (int) s);
        assertHaveSameElements(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, intStream.toArray());
    }

    @Test
    @DisplayName("Test mapToObj method")
    public void testMapToObj() {
        List<String> result = parallelStream.mapToObj(s -> "Value: " + s).sorted().toList();

        assertEquals(10, result.size());
        assertEquals("Value: 1", result.get(0));
        assertEquals("Value: 10", result.get(1));
        assertEquals("Value: 9", result.get(9));

        parallelStream = createShortStream(TEST_ARRAY);
        Stream<Short> shortObjStream = parallelStream.mapToObj(s -> Short.valueOf(s));
        List<Short> shortResult = shortObjStream.toList();
        assertHaveSameElements(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7, (short) 8, (short) 9, (short) 10),
                shortResult);
    }

    @Test
    @DisplayName("Test flatMap method")
    public void testFlatMap() {
        ShortStream flattened = parallelStream.flatMap(s -> ShortStream.of(s, (short) (s + 10)));
        short[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createShortStream(new short[] { 1, 2, 3 });
        flattened = parallelStream.flatMap(s -> s % 2 == 0 ? ShortStream.of(s) : ShortStream.empty());
        assertArrayEquals(new short[] { 2 }, flattened.toArray());
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
    @DisplayName("Test flatMapToInt method")
    public void testFlatMapToInt() {
        IntStream flattened = parallelStream.flatMapToInt(s -> IntStream.of(s, s * 10));
        int[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);
        assertEquals(1, result[0]);
        assertEquals(100, result[19]);
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
    @DisplayName("Test onEach method")
    public void testOnEach() {
        List<Short> capturedValues = new ArrayList<>();
        ShortConsumer action = it -> {
            synchronized (capturedValues) {
                capturedValues.add(it);
            }
        };
        ShortStream result = parallelStream.onEach(action);

        short[] array = result.toArray();

        assertEquals(10, capturedValues.size());
        assertHaveSameElements(TEST_ARRAY, array);

        Collections.sort(capturedValues);
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], capturedValues.get(i).shortValue());
        }
    }

    @Test
    @DisplayName("Test forEach method")
    public void testForEach() throws Exception {
        List<Short> result = Collections.synchronizedList(new ArrayList<>());

        parallelStream.forEach(result::add);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).shortValue());
        }

        parallelStream = createShortStream(TEST_ARRAY);
        assertThrows(RuntimeException.class, () -> {
            parallelStream.forEach((Throwables.ShortConsumer<Exception>) s -> {
                if (s == 5)
                    throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    @DisplayName("Test toMap method")
    public void testToMap() throws Exception {
        Map<String, Short> result = parallelStream.toMap(s -> "Key" + s, s -> Short.valueOf(s), (v1, v2) -> v1, HashMap::new);

        assertEquals(10, result.size());
        assertEquals(Short.valueOf((short) 1), result.get("Key1"));
        assertEquals(Short.valueOf((short) 10), result.get("Key10"));

        parallelStream = createShortStream(new short[] { 1, 1, 2, 2, 3 });
        Map<String, Short> mergedResult = parallelStream.toMap(s -> "Key" + (s % 2), s -> Short.valueOf(s), (v1, v2) -> (short) (v1 + v2), HashMap::new);

        assertEquals(2, mergedResult.size());
    }

    @Test
    @DisplayName("Test groupTo method")
    public void testGroupTo() throws Exception {
        Map<String, List<Short>> result = parallelStream.groupTo(s -> s % 2 == 0 ? "even" : "odd", Collectors.toList(), HashMap::new);

        assertEquals(2, result.size());
        assertEquals(5, result.get("even").size());
        assertEquals(5, result.get("odd").size());

        parallelStream = createShortStream(TEST_ARRAY);
        Map<Boolean, Long> countResult = parallelStream.groupTo(s -> s > 5, Collectors.counting(), HashMap::new);

        assertEquals(2, countResult.size());
        assertEquals(5L, countResult.get(true));
        assertEquals(5L, countResult.get(false));
    }

    @Test
    @DisplayName("Test reduce with identity method")
    public void testReduceWithIdentity() {
        short result = parallelStream.reduce((short) 0, (a, b) -> (short) (a + b));

        assertEquals(55, result);

        parallelStream = createShortStream(new short[] { 1, 2, 3, 4 });
        result = parallelStream.reduce((short) 1, (a, b) -> (short) (a * b));
        assertEquals(24, result);

        parallelStream = createShortStream(new short[0]);
        result = parallelStream.reduce((short) 10, (a, b) -> (short) (a + b));
        assertEquals(10, result);
    }

    @Test
    @DisplayName("Test reduce without identity method")
    public void testReduceWithoutIdentity() {
        OptionalShort result = parallelStream.reduce((a, b) -> (short) (a + b));

        assertTrue(result.isPresent());
        assertEquals(55, result.get());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.reduce((a, b) -> a > b ? a : b);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());

        parallelStream = createShortStream(new short[0]);
        result = parallelStream.reduce((a, b) -> (short) (a + b));
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test collect method")
    public void testCollect() {
        List<Short> result = parallelStream.collect(ArrayList::new, (list, s) -> list.add(s), ArrayList::addAll);

        Collections.sort(result);
        assertEquals(10, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i).shortValue());
        }

        parallelStream = createShortStream(TEST_ARRAY);
        String concatenated = parallelStream.collect(StringBuilder::new, (sb, s) -> sb.append(s).append(","), StringBuilder::append).toString();

        assertTrue(concatenated.contains("1,"));
        assertTrue(concatenated.contains("10,"));
    }

    @Test
    @DisplayName("Test anyMatch method")
    public void testAnyMatch() throws Exception {
        boolean result = parallelStream.anyMatch(s -> s > 5);
        assertTrue(result);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.anyMatch(s -> s > 20);
        assertFalse(result);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.anyMatch(s -> s == 7);
        assertTrue(result);

        parallelStream = createShortStream(new short[0]);
        result = parallelStream.anyMatch(s -> true);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test allMatch method")
    public void testAllMatch() throws Exception {
        boolean result = parallelStream.allMatch(s -> s > 0);
        assertTrue(result);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.allMatch(s -> s > 5);
        assertFalse(result);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.allMatch(s -> s <= 10);
        assertTrue(result);

        parallelStream = createShortStream(new short[0]);
        result = parallelStream.allMatch(s -> false);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test noneMatch method")
    public void testNoneMatch() throws Exception {
        boolean result = parallelStream.noneMatch(s -> s > 20);
        assertTrue(result);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.noneMatch(s -> s > 5);
        assertFalse(result);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.noneMatch(s -> s == 0);
        assertTrue(result);

        parallelStream = createShortStream(new short[0]);
        result = parallelStream.noneMatch(s -> true);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalShort result = parallelStream.findFirst(s -> s > 5);

        assertTrue(result.isPresent());
        assertEquals(6, result.get());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findFirst(s -> s > 20);
        assertFalse(result.isPresent());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findFirst(s -> s == 1);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalShort result = parallelStream.findAny(s -> s > 5);

        assertTrue(result.isPresent());
        assertTrue(result.get() > 5);

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findAny(s -> s > 20);
        assertFalse(result.isPresent());

        parallelStream = createShortStream(new short[] { 5 });
        result = parallelStream.findAny(s -> s == 5);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalShort result = parallelStream.findLast(s -> s < 5);

        assertTrue(result.isPresent());
        assertEquals(4, result.get());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findLast(s -> s > 20);
        assertFalse(result.isPresent());

        parallelStream = createShortStream(TEST_ARRAY);
        result = parallelStream.findLast(s -> s == 10);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
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
    @DisplayName("Test isParallel method")
    public void testIsParallel() {
        assertTrue(parallelStream.isParallel());
    }

    @Test
    @DisplayName("Test sequential method")
    public void testSequential() {
        ShortStream sequential = parallelStream.sequential();

        assertNotNull(sequential);
        assertFalse(sequential.isParallel());

        short[] result = sequential.toArray();
        assertArrayEquals(TEST_ARRAY, result);
    }

    @Test
    @DisplayName("Test onClose method")
    public void testOnClose() {
        AtomicBoolean closeCalled = new AtomicBoolean(false);

        ShortStream streamWithCloseHandler = parallelStream.onClose(() -> closeCalled.set(true));

        assertSame(parallelStream, streamWithCloseHandler);

        streamWithCloseHandler.close();
        assertTrue(closeCalled.get());

        parallelStream = createShortStream(TEST_ARRAY);
        ShortStream sameStream = parallelStream.onClose(null);
        assertSame(parallelStream, sameStream);
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
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(s -> s > 5).count();
        });
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
    @DisplayName("Test overflow scenarios")
    public void testOverflowScenarios() {
        parallelStream = createShortStream(new short[] { Short.MAX_VALUE, 1, 1 });

        short[] result = parallelStream.map(s -> (short) (s * 2)).toArray();
        assertHaveSameElements(new short[] { -2, 2, 2 }, result);
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
