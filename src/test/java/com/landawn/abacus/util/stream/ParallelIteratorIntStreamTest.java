package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToDoubleFunction;
import com.landawn.abacus.util.function.IntToLongFunction;
import com.landawn.abacus.util.function.IntUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

@Tag("new-test")
public class ParallelIteratorIntStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;

    private static final int[] TEST_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };
    private static final int[] DEFAULT_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    private IntStream parallelStream;

    protected IntStream createIntStream(int... elements) {
        return IntStream.of(elements).map(e -> (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createIntStream(DEFAULT_ARRAY);
    }

    @Test
    public void testTakeWhile() {
        IntStream stream = createIntStream(TEST_ARRAY);
        IntPredicate predicate = i -> i < 4;
        List<Integer> result = stream.takeWhile(predicate).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
        assertFalse(result.contains(4));
    }

    @Test
    public void testMap() {
        IntStream stream = createIntStream(TEST_ARRAY);
        IntUnaryOperator mapper = i -> i + 1;
        List<Integer> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(2));
        assertFalse(result.contains(1));
    }

    @Test
    public void testMapToByte() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntToByteFunction mapper = i -> (byte) (i * 10);
        List<Byte> result = stream.mapToByte(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains((byte) 10));
        assertTrue(result.contains((byte) 20));
        assertTrue(result.contains((byte) 30));
    }

    @Test
    public void testMapToLong() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntToLongFunction mapper = i -> (long) i * 1000;
        List<Long> result = stream.mapToLong(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1000L));
        assertTrue(result.contains(2000L));
        assertTrue(result.contains(3000L));
    }

    @Test
    public void testMapToDouble() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntToDoubleFunction mapper = i -> i / 2.0;
        List<Double> result = stream.mapToDouble(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(0.5));
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
    }

    @Test
    public void testFlatMapIntStream() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<IntStream> mapper = i -> IntStream.of(i, i + 10);
        List<Integer> result = stream.flatMap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(11));
        assertTrue(result.contains(2));
        assertTrue(result.contains(12));
    }

    @Test
    public void testFlatMapToChar() {
        IntStream stream = createIntStream(new int[] { 97, 98 });
        IntFunction<CharStream> mapper = i -> CharStream.of((char) i, (char) (i + 1));
        List<Character> result = stream.flatMapToChar(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('c'));
    }

    @Test
    public void testFlatMapToShort() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<ShortStream> mapper = i -> ShortStream.of((short) i, (short) (i + 1));
        List<Short> result = stream.flatMapToShort(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains((short) 1));
        assertTrue(result.contains((short) 2));
        assertTrue(result.contains((short) 3));
    }

    @Test
    public void testFlatMapToFloat() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<FloatStream> mapper = i -> FloatStream.of(i, (float) (i + 0.5));
        List<Float> result = stream.flatMapToFloat(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    public void testFlatMapToObjStream() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<Stream<String>> mapper = i -> Stream.of("A" + i, "B" + i);
        List<String> result = stream.flatMapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B1"));
        assertTrue(result.contains("A2"));
        assertTrue(result.contains("B2"));
    }

    @Test
    public void testOnEach() {
        IntStream stream = createIntStream(TEST_ARRAY);
        List<Integer> consumed = new ArrayList<>();
        IntConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.peek(action).forEach(i -> {
        });
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEachWithException() {
        IntStream stream = createIntStream(TEST_ARRAY);
        AtomicInteger count = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            stream.forEach(i -> {
                if (count.incrementAndGet() > 5) {
                    throw new RuntimeException("Test Exception");
                }
            });
        });
    }

    @Test
    public void testGroupTo() {
        IntStream stream = createIntStream(new int[] { 1, 2, 1, 3, 2 });
        Map<String, List<Integer>> result = stream.groupTo(i -> String.valueOf(i), Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(List.of(1, 1), result.get("1"));
        assertEquals(List.of(2, 2), result.get("2"));
        assertEquals(List.of(3), result.get("3"));
    }

    @Test
    public void testReduceWithIdentity() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntBinaryOperator op = (i1, i2) -> i1 + i2;
        int result = stream.reduce(0, op);
        assertEquals(6, result);

        IntStream emptyStream = createIntStream(new int[] {});
        int emptyResult = emptyStream.reduce(0, op);
        assertEquals(0, emptyResult);
    }

    @Test
    public void testCollect() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        List<Integer> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1, 2, 3), collectedList);
    }

    @Test
    public void testAllMatch() {
        assertTrue(createIntStream(TEST_ARRAY).allMatch(i -> i >= 1 && i <= 26));
        assertFalse(createIntStream(TEST_ARRAY).allMatch(i -> i < 26));
        assertTrue(createIntStream(new int[] {}).allMatch(i -> false));
    }

    @Test
    public void testFindFirst() {
        IntStream stream = createIntStream(new int[] { 4, 2, 1, 3, 1 });
        OptionalInt result = stream.findFirst(i -> i == 1);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        stream = createIntStream(new int[] { 4, 2, 1, 3, 1 });
        OptionalInt notFound = stream.findFirst(i -> i == 10);
        assertFalse(notFound.isPresent());

        OptionalInt empty = createIntStream(new int[] {}).findFirst(i -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindLast() {
        IntStream stream = createIntStream(new int[] { 4, 2, 1, 3, 1 });
        OptionalInt result = stream.findLast(i -> i == 1);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        stream = createIntStream(new int[] { 4, 2, 1, 3, 1 });
        OptionalInt notFound = stream.findLast(i -> i == 10);
        assertFalse(notFound.isPresent());

        OptionalInt empty = createIntStream(new int[] {}).findLast(i -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testZipWithTernaryOperator() {
        IntStream streamA = createIntStream(new int[] { 1, 2 });
        IntStream streamB = IntStream.of(10, 20);
        IntStream streamC = IntStream.of(100, 101);
        IntTernaryOperator zipper = (i1, i2, i3) -> i1 + i2 + i3;
        List<Integer> result = streamA.zipWith(streamB, streamC, zipper).sorted().toList();
        assertEquals(2, result.size());
        assertEquals(1 + 10 + 100, result.get(0));
        assertEquals(2 + 20 + 101, result.get(1));
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        IntStream streamA = createIntStream(new int[] { 1 });
        IntStream streamB = IntStream.of(10, 20);
        IntStream streamC = IntStream.of(100, 101, 102);
        int valA = 0;
        int valB = -1;
        int valC = -2;
        IntTernaryOperator zipper = (i1, i2, i3) -> {
            return i1 + i2 + i3;
        };
        List<Integer> result = streamA.zipWith(streamB, streamC, valA, valB, valC, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(1 + 10 + 100, result.get(1));
        assertEquals(0 + 20 + 101, result.get(2));
        assertEquals(0 + -1 + 102, result.get(0));
    }

    @Test
    public void testSequential() {
        IntStream parallelStream = createIntStream(TEST_ARRAY);
        IntStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        List<Integer> result = sequentialStream.toList();
        assertEquals(TEST_ARRAY.length, result.size());
        for (int i = 0; i < TEST_ARRAY.length; i++) {
            assertEquals(TEST_ARRAY[i], result.get(i));
        }
        parallelStream.close();
        sequentialStream.close();
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelIteratorIntStream) createIntStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ITERATOR, ((ParallelIteratorIntStream) createIntStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelIteratorIntStream) createIntStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        IntStream stream = createIntStream(TEST_ARRAY);
        AtomicInteger closedCount = new AtomicInteger(0);
        Runnable handler1 = () -> closedCount.incrementAndGet();
        Runnable handler2 = () -> closedCount.incrementAndGet();

        IntStream newStream = stream.onClose(handler1).onClose(handler2);
        assertEquals(0, closedCount.get());
        newStream.close();
        assertEquals(2, closedCount.get());
    }

    @Test
    @DisplayName("Test flatMap method")
    public void testFlatMap() {
        IntStream flattened = parallelStream.flatMap(i -> IntStream.of(i, i + 10));
        int[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createIntStream(new int[] { 1, 2, 3 });
        flattened = parallelStream.flatMap(i -> i % 2 == 0 ? IntStream.of(i) : IntStream.empty());
        assertArrayEquals(new int[] { 2 }, flattened.toArray());
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
    @DisplayName("Test findFirst with predicate method")
    public void testFindFirstWithPredicate() throws Exception {
        OptionalInt result = parallelStream.findFirst(i -> i > 5);

        assertTrue(result.isPresent());
        assertEquals(6, result.getAsInt());

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findFirst(i -> i > 30);
        assertFalse(result.isPresent());

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findFirst(i -> i == 1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    @DisplayName("Test findLast with predicate method")
    public void testFindLastWithPredicate() throws Exception {
        OptionalInt result = parallelStream.findLast(i -> i < 5);

        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findLast(i -> i > 30);
        assertFalse(result.isPresent());

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findLast(i -> i == 10);
        assertTrue(result.isPresent());
        assertEquals(10, result.getAsInt());
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
    @DisplayName("Test zipWith three streams with default values")
    public void testZipWithTernaryDefaults() {
        IntStream stream2 = createIntStream(new int[] { 10, 20 });
        IntStream stream3 = createIntStream(new int[] { 1, 2, 3, 4 });
        int[] result = parallelStream.zipWith(stream2, stream3, 0, 0, 1, (a, b, c) -> a + b + c).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(6, result[0]);
        assertEquals(6, result[1]);
        assertEquals(24, result[9]);
    }

    @Test
    @DisplayName("Test edge cases")
    public void testEdgeCases() {
        parallelStream = createIntStream(new int[0]);
        assertEquals(0, parallelStream.count());

        parallelStream = createIntStream(new int[] { 42 });
        assertEquals(42, parallelStream.first().getAsInt());

        parallelStream = createIntStream(new int[] { -5, -3, -1, 0, 1, 3, 5 });
        long positiveCount = parallelStream.filter(i -> i > 0).count();
        assertEquals(3, positiveCount);

        parallelStream = createIntStream(new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE });
        assertEquals(5, parallelStream.count());
    }

    @Test
    @DisplayName("Test exception handling in parallel operations")
    public void testExceptionHandling() {
        assertThrows(RuntimeException.class, () -> {
            parallelStream.filter(i -> {
                if (i == 5)
                    throw new RuntimeException("Test exception in filter");
                return true;
            }).count();
        });

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
    @DisplayName("Test method chaining")
    public void testMethodChaining() {
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
    @DisplayName("Test with specific int patterns")
    public void testSpecificIntPatterns() {
        parallelStream = createIntStream(new int[] { 1, 2, 4, 8, 16, 32, 64, 128 });
        int[] powersOfTwo = parallelStream.filter(i -> (i & (i - 1)) == 0).toArray();
        assertEquals(8, powersOfTwo.length);

        parallelStream = createIntStream(new int[] { 3, 6, 9, 12, 15 });
        boolean allDivisibleBy3 = parallelStream.allMatch(i -> i % 3 == 0);
        assertTrue(allDivisibleBy3);
    }

    // Covers uncovered iterator-stream terminal branches without falling back to the array-backed implementation.
    @Test
    public void testReduceWithoutIdentityAndFindAny_IteratorBranch() {
        assertEquals(15, createIntStream(4, 2, 1, 3, 5).reduce(0, Integer::sum));

        OptionalInt reduced = createIntStream(4, 2, 1, 3, 5).reduce(Integer::sum);
        assertTrue(reduced.isPresent());
        assertEquals(15, reduced.get());

        OptionalInt anyOdd = createIntStream(4, 2, 1, 3, 5).findAny(i -> (i & 1) == 1);
        assertTrue(anyOdd.isPresent());
        assertTrue(anyOdd.get() == 1 || anyOdd.get() == 3 || anyOdd.get() == 5);

        OptionalInt notFound = createIntStream(4, 2, 1, 3, 5).findAny(i -> i > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testReduceAndFindOperations_DelayedMatchOrdering() {
        assertEquals(72, createIntStream(21, 2, 4, 7, 6, 11, 8, 13).reduce(0, Integer::sum));

        OptionalInt reduced = createIntStream(21, 2, 4).reduce(Integer::sum);
        assertTrue(reduced.isPresent());
        assertEquals(27, reduced.get());

        OptionalInt firstMatch = createIntStream(21, 2, 4, 7, 6, 11, 8, 13).findFirst(i -> {
            if (i == 21) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return i > 5 && (i & 1) == 1;
        });
        assertTrue(firstMatch.isPresent());
        assertEquals(21, firstMatch.get());

        OptionalInt anyMatch = createIntStream(21, 2, 4, 7, 6, 11, 8, 13).findAny(i -> i > 5 && (i & 1) == 1);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21 || anyMatch.get() == 7 || anyMatch.get() == 11 || anyMatch.get() == 13);

        OptionalInt lastMatch = createIntStream(21, 2, 4, 7, 6, 11, 8, 13).findLast(i -> {
            if (i == 13) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return i > 5 && (i & 1) == 1;
        });
        assertTrue(lastMatch.isPresent());
        assertEquals(13, lastMatch.get());
    }
}
