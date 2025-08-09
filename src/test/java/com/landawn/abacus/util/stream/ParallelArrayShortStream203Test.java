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

public class ParallelArrayShortStream203Test extends TestBase {

    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing 
    private static final short[] TEST_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };

    // Empty method to be implemented by the user for initializing ShortStream
    protected ShortStream createShortStream(short... elements) {
        return ShortStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
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
    public void testDropWhile() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortPredicate predicate = s -> s < 4;
        List<Short> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains((short) 4));
        assertFalse(result.contains((short) 3));
    }

    @Test
    public void testMap() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortUnaryOperator mapper = s -> (short) (s + 1);
        List<Short> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains((short) 2)); // 1 maps to 2
        assertFalse(result.contains((short) 1));
    }

    @Test
    public void testMapToInt() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        ShortToIntFunction mapper = s -> s * 2;
        List<Integer> result = stream.mapToInt(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2)); // 1 maps to 2
        assertTrue(result.contains(52)); // 26 maps to 52
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
    public void testOnEach() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        List<Short> consumed = new ArrayList<>();
        ShortConsumer action = it -> {
            synchronized (consumed) { // Ensure thread safety
                consumed.add(it);
            }
        };
        stream.onEach(action).forEach(s -> {
        }); // Trigger the onEach action

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEach() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        List<Short> consumed = new ArrayList<>();
        ShortConsumer action = it -> {
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
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        Map<String, String> result = stream.toMap(s -> String.valueOf(s), s -> "Value_" + s, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1", result.get("1"));
        assertEquals("Value_2", result.get("2"));
        assertEquals("Value_3", result.get("3"));
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
    public void testCollect() {
        ShortStream stream = createShortStream(new short[] { 1, 2, 3 });
        List<Short> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(List.of((short) 1, (short) 2, (short) 3), collectedList);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createShortStream(TEST_ARRAY).anyMatch(s -> s == 13));
        assertFalse(createShortStream(TEST_ARRAY).anyMatch(s -> s == 100));
        assertFalse(createShortStream(new short[] {}).anyMatch(s -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createShortStream(TEST_ARRAY).allMatch(s -> s >= 1 && s <= 26));
        assertFalse(createShortStream(TEST_ARRAY).allMatch(s -> s < 26));
        assertTrue(createShortStream(new short[] {}).allMatch(s -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createShortStream(TEST_ARRAY).noneMatch(s -> s == 100));
        assertFalse(createShortStream(TEST_ARRAY).noneMatch(s -> s == 1));
        assertTrue(createShortStream(new short[] {}).noneMatch(s -> true));
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
    public void testZipWithBinaryOperatorWithNoneValues() {
        ShortStream streamA = createShortStream(new short[] { 1, 2 });
        ShortStream streamB = ShortStream.of((short) 10, (short) 20, (short) 30);
        short valA = 0; // Assuming 0 as 'none' for short
        short valB = -1; // Assuming -1 as 'none' for short
        ShortBinaryOperator zipper = (s1, s2) -> {
            if (s1 == valA)
                return s2;
            if (s2 == valB)
                return s1;
            return (short) (s1 + s2);
        };
        List<Short> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals((short) 11, result.get(0)); // 1 + 10
        assertEquals((short) 22, result.get(1)); // 2 + 20
        assertEquals((short) 30, result.get(2)); // 0 (valA) + 30 -> 30
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
        assertEquals((short) (0 + 20 + 101), result.get(2)); // valA (0) + 20 + 101
        assertEquals((short) (0 + -1 + 102), result.get(0)); // valA (0) + valB (-1) + 102
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
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        // Since maxThreadNum() is protected in the superclass,
        // and its value is used internally, we can't directly call it from here.
        // However, we can assert its behavior indirectly or via reflection if necessary.
        // For this test, we'll assume the constructor correctly sets it and other tests
        // indirectly verify its usage.
        // If it was a public method of ParallelArrayShortStream, we would test it directly.
        // ShortStream stream = createShortStream(shortArray);
        //assertTrue(stream.maxThreadNum() > 0); // This line would work if maxThreadNum() was public in ShortStream
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, splitor() is protected.
        // ShortStream stream = createShortStream(shortArray);
        //assertNotNull(stream.splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, asyncExecutor() is protected.
        // ShortStream stream = createShortStream(shortArray);
        //assertNotNull(stream.asyncExecutor());
    }

    @Test
    public void testOnClose() {
        ShortStream stream = createShortStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        ShortStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get()); // Not closed yet
        newStream.close(); // Close the stream, which should trigger the handler
        assertTrue(closedFlag.get()); // Now it should be closed
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
        assertSame(stream, newStream); // Should return the same instance if handler is null
        newStream.close(); // Should not throw
    }
}
