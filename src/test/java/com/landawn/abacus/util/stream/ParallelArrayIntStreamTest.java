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
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToCharFunction;
import com.landawn.abacus.util.function.IntToDoubleFunction;
import com.landawn.abacus.util.function.IntToFloatFunction;
import com.landawn.abacus.util.function.IntToLongFunction;
import com.landawn.abacus.util.function.IntToShortFunction;
import com.landawn.abacus.util.function.IntUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelArrayIntStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;

    private static final int[] TEST_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };
    private static final int[] DEFAULT_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    private IntStream parallelStream;

    protected IntStream createIntStream(int... elements) {
        return IntStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    protected IntStream createIteratorSplitorIntStream(int... elements) {
        return new ParallelArrayIntStream(elements, 0, elements.length, false, testMaxThreadNum, Splitor.ITERATOR, null, false, new ArrayList<>());
    }

    @BeforeEach
    public void setUp() {
        parallelStream = createIntStream(DEFAULT_ARRAY);
    }

    // Covers the iterator-based terminal-operation branch in ParallelArrayIntStream.
    @Test
    public void testReduceAndFindMethods_IteratorSplitor() {
        assertEquals(15, createIteratorSplitorIntStream(4, 2, 1, 3, 5).reduce(0, Integer::sum));

        OptionalInt reduced = createIteratorSplitorIntStream(4, 2, 1, 3, 5).reduce(Integer::sum);
        assertTrue(reduced.isPresent());
        assertEquals(15, reduced.get());

        OptionalInt firstOdd = createIteratorSplitorIntStream(4, 2, 1, 3, 5).findFirst(i -> (i & 1) == 1);
        assertTrue(firstOdd.isPresent());
        assertEquals(1, firstOdd.get());

        OptionalInt anyOdd = createIteratorSplitorIntStream(4, 2, 1, 3, 5).findAny(i -> (i & 1) == 1);
        assertTrue(anyOdd.isPresent());
        assertTrue(anyOdd.get() == 1 || anyOdd.get() == 3 || anyOdd.get() == 5);

        OptionalInt lastOdd = createIteratorSplitorIntStream(4, 2, 1, 3, 5).findLast(i -> (i & 1) == 1);
        assertTrue(lastOdd.isPresent());
        assertEquals(5, lastOdd.get());

        OptionalInt notFound = createIteratorSplitorIntStream(4, 2, 1, 3, 5).findAny(i -> i > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFilter() {
        IntStream stream = createIntStream(TEST_ARRAY);
        IntPredicate predicate = i -> i > 13;
        List<Integer> result = stream.filter(predicate).toList();
        assertEquals(13, result.size());
        assertTrue(result.contains(14));
        assertFalse(result.contains(1));
    }

    @Test
    @DisplayName("Test complex parallel operations")
    public void testComplexParallelOperations() {
        int[] result = parallelStream.filter(i -> i % 2 == 0).map(i -> i * 2).sorted().toArray();

        assertArrayEquals(new int[] { 4, 8, 12, 16, 20 }, result);

        parallelStream = createIntStream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        int sum = parallelStream.map(i -> i * 2).reduce(0, Integer::sum);

        assertEquals(110, sum);
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
    @DisplayName("Test parallel stream state after operations")
    public void testStreamStateAfterOperations() {
        parallelStream.count();

        assertThrows(IllegalStateException.class, () -> {
            parallelStream.filter(i -> i > 5).count();
        });
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
    public void testDropWhile() {
        IntStream stream = createIntStream(TEST_ARRAY);
        IntPredicate predicate = i -> i < 4;
        List<Integer> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains(4));
        assertFalse(result.contains(3));
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
    public void testMapToChar() {
        IntStream stream = createIntStream(new int[] { 97, 98, 99 });
        IntToCharFunction mapper = i -> (char) i;
        List<Character> result = stream.mapToChar(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('c'));
    }

    @Test
    public void testMapToChar_empty() {
        parallelStream = createIntStream(new int[] {});
        char[] result = parallelStream.mapToChar(i -> (char) i).toArray();
        assertEquals(0, result.length);
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
    public void testMapToByte_empty() {
        parallelStream = createIntStream(new int[] {});
        byte[] result = parallelStream.mapToByte(i -> (byte) i).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToShort() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntToShortFunction mapper = i -> (short) (i * 100);
        List<Short> result = stream.mapToShort(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains((short) 100));
        assertTrue(result.contains((short) 200));
        assertTrue(result.contains((short) 300));
    }

    @Test
    public void testMapToShort_empty() {
        parallelStream = createIntStream(new int[] {});
        short[] result = parallelStream.mapToShort(i -> (short) i).toArray();
        assertEquals(0, result.length);
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
    public void testMapToFloat() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntToFloatFunction mapper = i -> i / 2.0f;
        List<Float> result = stream.mapToFloat(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(0.5f));
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
    }

    @Test
    public void testMapToFloat_SequentialFallback_SingleElement() {
        List<Float> result = createIntStream(new int[] { 4 }).mapToFloat(i -> i * 0.5f).toList();
        assertEquals(1, result.size());
        assertEquals(2.0f, result.get(0), 0.001f);
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
    public void testMapToDouble_SequentialFallback_SingleElement() {
        List<Double> result = createIntStream(new int[] { 3 }).mapToDouble(i -> i * 1.5).toList();
        assertEquals(1, result.size());
        assertEquals(4.5, result.get(0), 0.001);
    }

    @Test
    public void testMapToObj() {
        IntStream stream = createIntStream(TEST_ARRAY);
        IntFunction<String> mapper = i -> "Int_" + i;
        List<String> result = stream.mapToObj(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains("Int_1"));
        assertTrue(result.contains("Int_26"));
    }

    @Test
    public void testMapToObj_SequentialFallback_SingleElement() {
        List<String> result = createIntStream(new int[] { 42 }).mapToObj(i -> "val_" + i).toList();
        assertEquals(1, result.size());
        assertEquals("val_42", result.get(0));
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
    public void testFlatMapIntArray() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<int[]> mapper = i -> new int[] { i, i + 10 };
        List<Integer> result = stream.flatmap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(11));
        assertTrue(result.contains(2));
        assertTrue(result.contains(12));
    }

    @Test
    @DisplayName("Test flatmap with array method")
    public void testFlatmapArray() {
        IntStream flattened = parallelStream.flatmap(i -> new int[] { i, i * 2 });
        int[] result = flattened.sorted().toArray();

        assertEquals(20, result.length);

        parallelStream = createIntStream(new int[] { 1, 2, 3 });
        flattened = parallelStream.flatmap(i -> i % 2 == 0 ? new int[] { i } : new int[0]);
        assertArrayEquals(new int[] { 2 }, flattened.toArray());
    }

    // Tests covering parallel code paths with large array (to bypass canBeSequential)

    @Test
    public void testFlatMap_ParallelPath() {
        // Use TEST_ARRAY (26 elements) to force the parallel path
        long count = createIntStream(TEST_ARRAY).flatMap(i -> IntStream.of(i, i * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmap_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatmap(i -> new int[] { i, i * 10 }).count();
        assertEquals(52, count);
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
    public void testFlatmap_SequentialFallback_SingleElement() {
        List<Integer> result = createIntStream(new int[] { 3 }).flatmap(i -> new int[] { i, i * 10 }).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(3));
        assertTrue(result.contains(30));
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
    public void testFlatMapToChar_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToChar(i -> CharStream.of((char) i)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToChar_SequentialFallback_SingleElement() {
        List<Character> result = createIntStream(new int[] { 65 }).flatMapToChar(i -> CharStream.of((char) i)).toList();
        assertEquals(1, result.size());
        assertEquals(Character.valueOf('A'), result.get(0));
    }

    @Test
    public void testFlatMapToByte() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<ByteStream> mapper = i -> ByteStream.of((byte) i, (byte) (i + 1));
        List<Byte> result = stream.flatMapToByte(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 2));
        assertTrue(result.contains((byte) 3));
    }

    @Test
    public void testFlatMapToByte_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToByte(i -> ByteStream.of((byte) i)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToByte_SequentialFallback_SingleElement() {
        List<Byte> result = createIntStream(new int[] { 5 }).flatMapToByte(i -> ByteStream.of((byte) i)).toList();
        assertEquals(1, result.size());
        assertEquals(Byte.valueOf((byte) 5), result.get(0));
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
    public void testFlatMapToShort_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToShort(i -> ShortStream.of((short) i)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToShort_SequentialFallback_SingleElement() {
        List<Short> result = createIntStream(new int[] { 10 }).flatMapToShort(i -> ShortStream.of((short) i)).toList();
        assertEquals(1, result.size());
        assertEquals(Short.valueOf((short) 10), result.get(0));
    }

    @Test
    public void testFlatMapToLong() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<LongStream> mapper = i -> LongStream.of(i, i + 1);
        List<Long> result = stream.flatMapToLong(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
    }

    @Test
    public void testFlatMapToLong_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToLong(i -> LongStream.of((long) i)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToLong_SequentialFallback_SingleElement() {
        List<Long> result = createIntStream(new int[] { 9 }).flatMapToLong(i -> LongStream.of((long) i)).toList();
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(9L), result.get(0));
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
    public void testFlatMapToFloat_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToFloat(i -> FloatStream.of((float) i)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToFloat_SequentialFallback_SingleElement() {
        List<Float> result = createIntStream(new int[] { 2 }).flatMapToFloat(i -> FloatStream.of((float) i)).toList();
        assertEquals(1, result.size());
        assertEquals(Float.valueOf(2.0f), result.get(0));
    }

    @Test
    public void testFlatMapToDouble() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<DoubleStream> mapper = i -> DoubleStream.of(i, i + 0.5);
        List<Double> result = stream.flatMapToDouble(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
    }

    @Test
    public void testFlatMapToDouble_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToDouble(i -> DoubleStream.of((double) i)).count();
        assertEquals(26, count);
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
    public void testFlatMapToObjCollection() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<Collection<String>> mapper = i -> {
            List<String> list = new ArrayList<>();
            list.add("X" + i);
            list.add("Y" + i);
            return list;
        };
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("X1"));
        assertTrue(result.contains("Y1"));
        assertTrue(result.contains("X2"));
        assertTrue(result.contains("Y2"));
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
    public void testFlatMapToObj_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToObj(i -> Stream.of(i, i * 10)).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmapToObj_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatmapToObj(i -> java.util.Arrays.asList(i, i * 10)).count();
        assertEquals(52, count);
    }

    // onEach parallel path with large array via peek alias
    @Test
    public void testOnEach_ParallelPath() {
        AtomicInteger count = new AtomicInteger(0);
        createIntStream(TEST_ARRAY).onEach(i -> count.incrementAndGet()).forEach(i -> {
        });
        assertEquals(TEST_ARRAY.length, count.get());
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
    public void testForEach_IteratorSplitor() {
        AtomicInteger sum = new AtomicInteger(0);
        createIteratorSplitorIntStream(1, 2, 3, 4, 5).forEach(i -> sum.addAndGet(i));
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEach() {
        IntStream stream = createIntStream(TEST_ARRAY);
        List<Integer> consumed = new ArrayList<>();
        IntConsumer action = it -> {
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
    @DisplayName("Test thread safety")
    public void testThreadSafety() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        parallelStream.forEach(i -> {
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
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        Map<String, String> result = stream.toMap(i -> String.valueOf(i), i -> "Value_" + i, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1", result.get("1"));
        assertEquals("Value_2", result.get("2"));
        assertEquals("Value_3", result.get("3"));
    }

    // toMap with large array forces parallel path
    @Test
    public void testToMap_ParallelPath() {
        Map<Integer, Integer> result = createIntStream(TEST_ARRAY).toMap(i -> i, i -> i * 2, (v1, v2) -> v1, ConcurrentHashMap::new);
        assertEquals(TEST_ARRAY.length, result.size());
        assertEquals(2, (int) result.get(1));
        assertEquals(TEST_ARRAY[TEST_ARRAY.length - 1] * 2, (int) result.get(TEST_ARRAY[TEST_ARRAY.length - 1]));
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

    // groupTo with large array forces parallel path
    @Test
    public void testGroupTo_ParallelPath() {
        Map<Boolean, List<Integer>> result = createIntStream(TEST_ARRAY).groupTo(i -> i % 2 == 0, java.util.stream.Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(2, result.size());
        assertEquals(13, result.get(true).size());
        assertEquals(13, result.get(false).size());
    }

    @Test
    @DisplayName("Test with large arrays")
    public void testLargeArrays() {
        int[] largeArray = new int[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i + 1;
        }

        parallelStream = createIntStream(largeArray);

        int sum = parallelStream.reduce(0, Integer::sum);
        assertEquals(500500, sum);

        parallelStream = createIntStream(largeArray);
        long evenCount = parallelStream.filter(i -> i % 2 == 0).count();
        assertEquals(500, evenCount);
    }

    @Test
    public void testReduce_WithIdentity_ParallelPath() {
        int result = createIntStream(TEST_ARRAY).reduce(0, Integer::sum);
        int expected = 0;
        for (int v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, result);
    }

    @Test
    public void testReduce_NoIdentity_ParallelPath() {
        com.landawn.abacus.util.u.OptionalInt result = createIntStream(TEST_ARRAY).reduce(Integer::sum);
        assertTrue(result.isPresent());
        int expected = 0;
        for (int v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, result.getAsInt());
    }

    @Test
    public void testReduceWithIdentity() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntBinaryOperator op = (i1, i2) -> i1 + i2;
        int result = stream.reduce(0, op);
        assertEquals(6, result);

        IntStream emptyStream = createIntStream(new int[] {});
        int emptyResult = emptyStream.reduce(100, op);
        assertEquals(100, emptyResult);
    }

    @Test
    public void testReduceWithoutIdentity() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntBinaryOperator accumulator = (i1, i2) -> i1 + i2;
        OptionalInt result = stream.reduce(accumulator);
        assertTrue(result.isPresent());
        assertEquals(6, result.get());

        IntStream emptyStream = createIntStream(new int[] {});
        OptionalInt emptyResult = emptyStream.reduce(accumulator);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    @DisplayName("Test overflow scenarios")
    public void testOverflowScenarios() {
        parallelStream = createIntStream(new int[] { Integer.MAX_VALUE - 1, 1, 1 });

        try {
            int sum = parallelStream.reduce(0, Integer::sum);
            assertTrue(sum < 0 || sum == Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testIteratorSplitorReduceAndFindOperations_SparseMatch() {
        assertEquals(72, createIteratorSplitorIntStream(21, 2, 4, 7, 6, 11, 8, 13).reduce(0, Integer::sum));

        OptionalInt reduced = createIteratorSplitorIntStream(21, 2, 4).reduce(Integer::sum);
        assertTrue(reduced.isPresent());
        assertEquals(27, reduced.get());

        OptionalInt firstMatch = createIteratorSplitorIntStream(21, 2, 4, 7, 6, 11, 8, 13).findFirst(i -> {
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

        OptionalInt anyMatch = createIteratorSplitorIntStream(21, 2, 4, 7, 6, 11, 8, 13).findAny(i -> i > 5 && (i & 1) == 1);
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 21 || anyMatch.get() == 7 || anyMatch.get() == 11 || anyMatch.get() == 13);

        OptionalInt lastMatch = createIteratorSplitorIntStream(21, 2, 4, 7, 6, 11, 8, 13).findLast(i -> {
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

    @Test
    public void testCollect() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        List<Integer> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1, 2, 3), collectedList);
    }

    @Test
    public void testCollect_IteratorSplitor() {
        List<Integer> result = createIteratorSplitorIntStream(1, 2, 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1, 2, 3), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createIntStream(TEST_ARRAY).anyMatch(i -> i == 13));
        assertFalse(createIntStream(TEST_ARRAY).anyMatch(i -> i == 100));
        assertFalse(createIntStream(new int[] {}).anyMatch(i -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createIntStream(TEST_ARRAY).allMatch(i -> i >= 1 && i <= 26));
        assertFalse(createIntStream(TEST_ARRAY).allMatch(i -> i < 26));
        assertTrue(createIntStream(new int[] {}).allMatch(i -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createIntStream(TEST_ARRAY).noneMatch(i -> i == 100));
        assertFalse(createIntStream(TEST_ARRAY).noneMatch(i -> i == 1));
        assertTrue(createIntStream(new int[] {}).noneMatch(i -> true));
    }

    // Cover the ITERATOR splitor path for anyMatch / allMatch / noneMatch / collect / forEach
    @Test
    public void testAnyMatchAllMatchNoneMatch_IteratorSplitor() {
        assertTrue(createIteratorSplitorIntStream(1, 2, 3, 4, 5).anyMatch(i -> i > 4));
        assertFalse(createIteratorSplitorIntStream(1, 2, 3, 4, 5).anyMatch(i -> i > 10));
        assertFalse(createIteratorSplitorIntStream(new int[0]).anyMatch(i -> true));

        assertTrue(createIteratorSplitorIntStream(1, 2, 3, 4, 5).allMatch(i -> i >= 1 && i <= 5));
        assertFalse(createIteratorSplitorIntStream(1, 2, 3, 4, 5).allMatch(i -> i < 5));
        assertTrue(createIteratorSplitorIntStream(new int[0]).allMatch(i -> false));

        assertTrue(createIteratorSplitorIntStream(1, 2, 3, 4, 5).noneMatch(i -> i > 10));
        assertFalse(createIteratorSplitorIntStream(1, 2, 3, 4, 5).noneMatch(i -> i > 4));
        assertTrue(createIteratorSplitorIntStream(new int[0]).noneMatch(i -> true));
    }

    // anyMatch/allMatch/noneMatch on ITERATOR splitor with large array
    @Test
    public void testMatchOperations_IteratorSplitor_LargeArray() {
        assertTrue(createIteratorSplitorIntStream(TEST_ARRAY).anyMatch(i -> i > 20));
        assertFalse(createIteratorSplitorIntStream(TEST_ARRAY).anyMatch(i -> i > 100));
        assertTrue(createIteratorSplitorIntStream(TEST_ARRAY).allMatch(i -> i >= 1));
        assertFalse(createIteratorSplitorIntStream(TEST_ARRAY).allMatch(i -> i < 20));
        assertTrue(createIteratorSplitorIntStream(TEST_ARRAY).noneMatch(i -> i > 100));
        assertFalse(createIteratorSplitorIntStream(TEST_ARRAY).noneMatch(i -> i == 1));
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
    public void testFindAny() {
        IntStream stream = createIntStream(new int[] { 4, 2, 1, 3, 1 });
        OptionalInt result = stream.findAny(i -> i == 1);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        stream = createIntStream(new int[] { 4, 2, 1, 3, 1 });
        OptionalInt notFound = stream.findAny(i -> i == 10);
        assertFalse(notFound.isPresent());

        OptionalInt empty = createIntStream(new int[] {}).findAny(i -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    @DisplayName("Test findAny with predicate method")
    public void testFindAnyWithPredicate() throws Exception {
        OptionalInt result = parallelStream.findAny(i -> i > 5);

        assertTrue(result.isPresent());
        assertTrue(result.getAsInt() > 5);

        parallelStream = createIntStream(TEST_ARRAY);
        result = parallelStream.findAny(i -> i > 30);
        assertFalse(result.isPresent());

        parallelStream = createIntStream(new int[] { 5 });
        result = parallelStream.findAny(i -> i == 5);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    // findFirst/findAny/findLast on ITERATOR splitor with large array
    @Test
    public void testFindOperations_IteratorSplitor_LargeArray() {
        com.landawn.abacus.util.u.OptionalInt first = createIteratorSplitorIntStream(TEST_ARRAY).findFirst(i -> i > 20);
        assertTrue(first.isPresent());
        assertEquals(21, first.get());

        com.landawn.abacus.util.u.OptionalInt any = createIteratorSplitorIntStream(TEST_ARRAY).findAny(i -> i > 20);
        assertTrue(any.isPresent());
        assertTrue(any.get() > 20);

        com.landawn.abacus.util.u.OptionalInt last = createIteratorSplitorIntStream(TEST_ARRAY).findLast(i -> i < 10);
        assertTrue(last.isPresent());
        assertEquals(9, last.get());
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
    public void testZipWithBinaryOperator() {
        IntStream streamA = createIntStream(new int[] { 1, 2, 3 });
        IntStream streamB = IntStream.of(10, 20, 30);
        IntBinaryOperator zipper = (i1, i2) -> i1 + i2;
        List<Integer> result = streamA.zipWith(streamB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11, result.get(0));
        assertEquals(22, result.get(1));
        assertEquals(33, result.get(2));
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
    @DisplayName("Test zipWith two streams with default values")
    public void testZipWithBinaryDefaults() {
        IntStream stream2 = createIntStream(new int[] { 10, 20, 30 });
        int[] result = parallelStream.zipWith(stream2, 0, 100, (a, b) -> a + b).sorted().toArray();

        assertEquals(10, result.length);
        assertEquals(11, result[0]);
        assertEquals(22, result[1]);
        assertEquals(33, result[2]);
        assertEquals(104, result[3]);
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
    public void testZipWithBinary_ParallelPath() {
        IntStream a = createIntStream(TEST_ARRAY);
        IntStream b = IntStream.of(TEST_ARRAY);
        List<Integer> result = a.zipWith(b, (x, y) -> x + y).sorted().toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertEquals(TEST_ARRAY[0] * 2, (int) result.get(0));
    }

    @Test
    public void testZipWithTernary_ParallelPath() {
        IntStream a = createIntStream(TEST_ARRAY);
        IntStream b = IntStream.of(TEST_ARRAY);
        IntStream c = IntStream.of(TEST_ARRAY);
        long count = a.zipWith(b, c, (x, y, z) -> x + y + z).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryDefaults_ParallelPath() {
        int[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        IntStream a = createIntStream(TEST_ARRAY);
        IntStream b = IntStream.of(shorter);
        List<Integer> result = a.zipWith(b, 0, -1, (x, y) -> x + y).toList();
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testZipWithTernaryDefaults_ParallelPath() {
        int[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        IntStream a = createIntStream(TEST_ARRAY);
        IntStream b = IntStream.of(shorter);
        IntStream c = IntStream.of(shorter);
        long count = a.zipWith(b, c, 0, -1, -2, (x, y, z) -> x + y + z).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        IntStream streamA = createIntStream(new int[] { 1, 2 });
        IntStream streamB = IntStream.of(10, 20, 30);
        int valA = 0;
        int valB = -1;
        IntBinaryOperator zipper = (i1, i2) -> {
            if (i1 == valA)
                return i2;
            if (i2 == valB)
                return i1;
            return i1 + i2;
        };
        List<Integer> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11, result.get(0));
        assertEquals(22, result.get(1));
        assertEquals(30, result.get(2));
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
    @DisplayName("Test zipWith two streams with binary operator")
    public void testZipWithBinary() {
        IntStream stream2 = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        IntStream zipped = parallelStream.zipWith(stream2, (a, b) -> a + b);
        int[] result = zipped.toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new int[] { 11, 22, 33, 44, 55 }, result);

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
    public void testZipWithBinaryDefaults_SequentialFallback() {
        List<Integer> result = IntStream.of(1, 2, 3).parallel(PS.create(Splitor.ARRAY).maxThreadNum(1)).zipWith(IntStream.of(10), 0, -1, Integer::sum).toList();

        assertEquals(Arrays.asList(11, 1, 2), result);
    }

    @Test
    public void testIsParallel() {
        IntStream stream = createIntStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
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
    public void testSequential_ParallelPath() {
        IntStream parallelS = createIntStream(TEST_ARRAY);
        assertTrue(parallelS.isParallel());
        IntStream seqS = parallelS.sequential();
        assertFalse(seqS.isParallel());
        assertEquals(TEST_ARRAY.length, seqS.count());
    }

    // filter/map/mapToXxx in sequential fallback path (2-element array, 4 threads: canBeSequential = false since size > 1)
    @Test
    public void testFilter_ParallelPath_SmallEnoughToGoDirect() {
        // 2 elements, 4 threads: canBeSequential is false (size 2 > 1)
        List<Integer> result = createIntStream(new int[] { 10, 20 }).filter(i -> i > 15).toList();
        assertEquals(1, result.size());
        assertTrue(result.contains(20));
    }

    // sequential() caching - call sequential() twice to ensure cached instance is returned
    @Test
    public void testSequential_Cached() {
        ParallelArrayIntStream s = (ParallelArrayIntStream) createIntStream(TEST_ARRAY);
        IntStream seq1 = s.sequential();
        IntStream seq2 = s.sequential();
        assertSame(seq1, seq2);
        seq1.close();
    }

    // Single-element parallel streams trigger canBeSequential=true, hitting the sequential fallback paths
    @Test
    public void testMapToLong_SequentialFallback_SingleElement() {
        List<Long> result = createIntStream(new int[] { 7 }).mapToLong(i -> (long) i * 100).toList();
        assertEquals(1, result.size());
        assertEquals(700L, result.get(0).longValue());
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelArrayIntStream) createIntStream(TEST_ARRAY)).maxThreadNum());
    }

    // forEach with ITERATOR splitor and large array
    @Test
    public void testForEach_IteratorSplitor_LargeArray() {
        AtomicInteger sum = new AtomicInteger(0);
        createIteratorSplitorIntStream(TEST_ARRAY).forEach(i -> sum.addAndGet(i));
        int expected = 0;
        for (int v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, sum.get());
    }

    // anyMatch with ARRAY splitor and large array
    @Test
    public void testAnyMatch_ArraySplitor_ParallelPath() {
        assertTrue(createIntStream(TEST_ARRAY).anyMatch(i -> i == 26));
        assertFalse(createIntStream(TEST_ARRAY).anyMatch(i -> i > 100));
    }

    // allMatch with ARRAY splitor and large array
    @Test
    public void testAllMatch_ArraySplitor_ParallelPath() {
        assertTrue(createIntStream(TEST_ARRAY).allMatch(i -> i >= 1 && i <= 26));
        assertFalse(createIntStream(TEST_ARRAY).allMatch(i -> i < 10));
    }

    // reduce with identity on ITERATOR splitor with large array
    @Test
    public void testReduceWithIdentity_IteratorSplitor_LargeArray() {
        int sum = createIteratorSplitorIntStream(TEST_ARRAY).reduce(0, Integer::sum);
        int expected = 0;
        for (int v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, sum);
    }

    // reduce without identity on ITERATOR splitor with large array
    @Test
    public void testReduceNoIdentity_IteratorSplitor_LargeArray() {
        com.landawn.abacus.util.u.OptionalInt result = createIteratorSplitorIntStream(TEST_ARRAY).reduce(Integer::sum);
        assertTrue(result.isPresent());
        int expected = 0;
        for (int v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, result.getAsInt());
    }

    // noneMatch with ARRAY splitor and large array
    @Test
    public void testNoneMatch_ArraySplitor_ParallelPath() {
        assertTrue(createIntStream(TEST_ARRAY).noneMatch(i -> i > 100));
        assertFalse(createIntStream(TEST_ARRAY).noneMatch(i -> i == 1));
    }

    // collect with ITERATOR splitor and large array
    @Test
    public void testCollect_IteratorSplitor_LargeArray() {
        List<Integer> result = createIteratorSplitorIntStream(TEST_ARRAY).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, result.size());
        assertHaveSameElements(N.toList(TEST_ARRAY), result);
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ARRAY, ((ParallelArrayIntStream) createIntStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelArrayIntStream) createIntStream(TEST_ARRAY)).asyncExecutor() != null);
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
    public void testOnClose() {
        IntStream stream = createIntStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        IntStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get());
        newStream.close();
        assertTrue(closedFlag.get());
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
    public void testOnCloseEmptyHandler() {
        IntStream stream = createIntStream(TEST_ARRAY);
        IntStream newStream = stream.onClose(null);
        assertSame(stream, newStream);
        newStream.close();
    }

    @Test
    public void testBoundaryValues() {
        int[] boundaryArray = new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE };
        parallelStream = createIntStream(boundaryArray);
        int[] result = parallelStream.sorted().toArray();
        assertEquals(7, result.length);
        assertEquals(Integer.MIN_VALUE, result[0]);
        assertEquals(Integer.MAX_VALUE, result[6]);
    }

}
