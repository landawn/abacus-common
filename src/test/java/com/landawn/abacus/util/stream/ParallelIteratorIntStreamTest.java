package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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
    @DisplayName("Test with specific int patterns")
    public void testSpecificIntPatterns() {
        parallelStream = createIntStream(new int[] { 1, 2, 4, 8, 16, 32, 64, 128 });
        int[] powersOfTwo = parallelStream.filter(i -> (i & (i - 1)) == 0).toArray();
        assertEquals(8, powersOfTwo.length);

        parallelStream = createIntStream(new int[] { 3, 6, 9, 12, 15 });
        boolean allDivisibleBy3 = parallelStream.allMatch(i -> i % 3 == 0);
        assertTrue(allDivisibleBy3);
    }

    // filter parallel path with large array
    @Test
    public void testFilter_ParallelPath() {
        List<Integer> result = createIntStream(TEST_ARRAY).filter(i -> i % 2 == 0).toList();
        assertEquals(13, result.size());
        assertTrue(result.contains(2));
        assertFalse(result.contains(1));
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
    public void testTakeWhile_SequentialFallback_maxThreadNum1() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1)).takeWhile(i -> i < 4).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDropWhile() {
        int[] result = createIntStream(1, 2, 3, 4, 5).dropWhile(n -> n < 3).sorted().toArray();
        assertTrue(result.length >= 2);
        // After dropping while < 3, should include 3, 4, 5
        boolean hasThree = false;
        for (int v : result) {
            if (v == 3) {
                hasThree = true;
                break;
            }
        }
        assertTrue(hasThree);
    }

    @Test
    public void testDropWhile_SequentialFallback_maxThreadNum1() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1)).dropWhile(i -> i < 3).toList();
        assertEquals(3, result.size());
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

    // mapToObj parallel path with large array
    @Test
    public void testMapToObj_ParallelPath() {
        List<String> result = createIntStream(TEST_ARRAY).mapToObj(i -> "v" + i).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains("v1"));
        assertTrue(result.contains("v26"));
    }

    // mapToChar parallel path with large array
    @Test
    public void testMapToChar_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).mapToChar(i -> (char) ('a' + i % 26)).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // mapToByte parallel path with large array
    @Test
    public void testMapToByte_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).mapToByte(i -> (byte) i).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // mapToShort parallel path with large array
    @Test
    public void testMapToShort_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).mapToShort(i -> (short) i).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // mapToLong parallel path with large array
    @Test
    public void testMapToLong_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).mapToLong(i -> (long) i).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // mapToFloat parallel path with large array
    @Test
    public void testMapToFloat_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).mapToFloat(i -> (float) i).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // mapToDouble parallel path with large array
    @Test
    public void testMapToDouble_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).mapToDouble(i -> (double) i).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // map parallel path with large array
    @Test
    public void testMap_ParallelPath() {
        int sum = createIntStream(TEST_ARRAY).map(i -> i * 2).reduce(0, Integer::sum);
        int expected = 0;
        for (int v : TEST_ARRAY)
            expected += v * 2;
        assertEquals(expected, sum);
    }

    @Test
    public void testMapToChar() {
        List<Character> result = createIntStream(65, 66, 67).mapToChar(i -> (char) i).boxed().sorted().toList();
        assertEquals(3, result.size());
        assertEquals('A', (char) result.get(0));
        assertEquals('C', (char) result.get(2));
    }

    @Test
    public void testMapToChar_SequentialFallback_maxThreadNum1() {
        List<Character> result = IntStream.of(65, 66, 67)
                .map(e -> e + 0)
                .parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1))
                .mapToChar(i -> (char) i)
                .toList();
        assertEquals(3, result.size());
        assertTrue(result.contains('A'));
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
    public void testMapToByte_SequentialFallback_maxThreadNum1() {
        List<Byte> result = IntStream.of(1, 2, 3).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1)).mapToByte(i -> (byte) i).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testMapToShort() {
        List<Short> result = createIntStream(1, 2, 3).mapToShort(i -> (short) i).boxed().sorted().toList();
        assertEquals(3, result.size());
        assertEquals((short) 1, (short) result.get(0));
    }

    @Test
    public void testMapToShort_SequentialFallback_maxThreadNum1() {
        List<Short> result = IntStream.of(100, 200, 300)
                .map(e -> e + 0)
                .parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1))
                .mapToShort(i -> (short) i)
                .toList();
        assertEquals(3, result.size());
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
    public void testMapToLong_SequentialFallback_maxThreadNum1() {
        List<Long> result = IntStream.of(1, 2, 3)
                .map(e -> e + 0)
                .parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1))
                .mapToLong(i -> (long) i * 1000)
                .toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(1000L));
    }

    @Test
    public void testMapToFloat() {
        List<Float> result = createIntStream(1, 2, 3).mapToFloat(i -> i * 1.5f).boxed().sorted().toList();
        assertEquals(3, result.size());
        assertEquals(1.5f, result.get(0), 0.001f);
    }

    @Test
    public void testMapToFloat_SequentialFallback_maxThreadNum1() {
        List<Float> result = IntStream.of(2, 4).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1)).mapToFloat(i -> i * 0.5f).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(1.0f));
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
    public void testMapToDouble_SequentialFallback_maxThreadNum1() {
        List<Double> result = IntStream.of(3, 6).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1)).mapToDouble(i -> i * 1.5).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testMapToObj_SequentialFallback_maxThreadNum1() {
        List<String> result = IntStream.of(10, 20).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1)).mapToObj(i -> "n" + i).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("n10"));
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

    // flatMapToByte is uncovered
    @Test
    public void testFlatMapToByte() {
        List<Byte> result = createIntStream(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 2))).toList();
        assertEquals(6, result.size());
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 6));
    }

    // flatMapToLong is uncovered
    @Test
    public void testFlatMapToLong() {
        List<Long> result = createIntStream(1, 2, 3).flatMapToLong(i -> LongStream.of(i, (long) i * 10)).toList();
        assertEquals(6, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(30L));
    }

    // flatMapToDouble is uncovered
    @Test
    public void testFlatMapToDouble() {
        List<Double> result = createIntStream(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i + 0.5)).toList();
        assertEquals(6, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(3.5));
    }

    // flatmap(IntFunction<int[]>) maps each element to a primitive array
    @Test
    public void testFlatmapPrimitiveArray() {
        int[] result = createIntStream(1, 2, 3).flatmap(i -> new int[] { i, i * 10 }).sorted().toArray();
        assertEquals(6, result.length);
        assertEquals(1, result[0]);
        assertEquals(30, result[5]);
    }

    // flatMapToShort parallel path with large array
    @Test
    public void testFlatMapToShort_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToShort(i -> ShortStream.of((short) i)).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // flatMapToFloat parallel path with large array
    @Test
    public void testFlatMapToFloat_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToFloat(i -> FloatStream.of((float) i)).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // flatMapToChar parallel path with large array
    @Test
    public void testFlatMapToChar_ParallelPath() {
        long count = createIntStream(TEST_ARRAY).flatMapToChar(i -> CharStream.of((char) i)).count();
        assertEquals(TEST_ARRAY.length, count);
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
    @DisplayName("Test flatMapToObj method")
    public void testFlatMapToObj() {
        Stream<String> flattened = parallelStream.flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        List<String> result = flattened.sorted().toList();

        assertEquals(20, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B10"));
    }

    @Test
    public void testFlatmapToObj() {
        List<String> result = createIntStream(1, 2, 3).flatmapToObj(i -> java.util.Arrays.asList("val" + i, "extra" + i)).toList();
        assertEquals(6, result.size());
        assertTrue(result.contains("val1"));
    }

    @Test
    public void testFlatMapToObj_largeArray() {
        // flatMapToObj parallel path with large array
        List<String> result = createIntStream(TEST_ARRAY).flatMapToObj(i -> Stream.of(new String[] { "v" + i })).toList();
        assertEquals(26, result.size());
        assertTrue(result.contains("v1"));
        assertTrue(result.contains("v26"));
    }

    @Test
    public void testFlatmapToObj_largeArray() {
        // flatmapToObj parallel path with large array
        List<String> result = createIntStream(TEST_ARRAY).flatmapToObj(i -> Arrays.asList("a" + i, "b" + i)).toList();
        assertEquals(52, result.size());
        assertTrue(result.contains("a1"));
        assertTrue(result.contains("b26"));
    }

    // onEach parallel path with large array
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

    // forEach parallel path with large array
    @Test
    public void testForEach_ParallelPath() {
        AtomicInteger sum = new AtomicInteger(0);
        createIntStream(TEST_ARRAY).forEach(i -> sum.addAndGet(i));
        int expected = 0;
        for (int v : TEST_ARRAY)
            expected += v;
        assertEquals(expected, sum.get());
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

    // toMap parallel path with large array
    @Test
    public void testToMap_ParallelPath() {
        Map<Integer, Integer> result = createIntStream(TEST_ARRAY).toMap(i -> i, i -> i * 2, (v1, v2) -> v1, java.util.HashMap::new);
        assertEquals(TEST_ARRAY.length, result.size());
        assertEquals(2, (int) result.get(1));
    }

    @Test
    public void testToMap_WithMergeAndSupplier() {
        Map<Integer, Integer> result = createIntStream(1, 2, 3, 1).toMap(n -> n, n -> n * 10, Integer::sum, java.util.HashMap::new);
        assertFalse(result.isEmpty());
        assertEquals(20, (int) result.get(1)); // 1*10 + 1*10 = 20
        assertEquals(20, (int) result.get(2));
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

    // groupTo parallel path with large array
    @Test
    public void testGroupTo_ParallelPath() {
        Map<Boolean, List<Integer>> result = createIntStream(TEST_ARRAY).groupTo(i -> i % 2 == 0, java.util.stream.Collectors.toList(), java.util.HashMap::new);
        assertEquals(2, result.size());
        assertEquals(13, result.get(true).size());
        assertEquals(13, result.get(false).size());
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
    public void testReduce_parallelWithLargeArray() {
        // Use TEST_ARRAY (26 elements) to ensure parallel path is exercised
        int sum = createIntStream(TEST_ARRAY).reduce(0, Integer::sum);
        assertEquals(351, sum); // sum of 1..26 = 351

        // reduce without identity - parallel branch
        OptionalInt opt = createIntStream(TEST_ARRAY).reduce(Integer::sum);
        assertTrue(opt.isPresent());
        assertEquals(351, opt.get());
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
    public void testReduce_emptyStreamParallel() {
        // Empty stream in parallel path: result is identity accumulated across threads
        // The implementation may return identity * maxThreadNum for the identity-based reduce
        int result = createIntStream(new int[0]).reduce(0, Integer::sum);
        assertEquals(0, result);

        OptionalInt opt = createIntStream(new int[0]).reduce(Integer::sum);
        assertFalse(opt.isPresent());
    }

    @Test
    public void testConstructor_withDefaultValues() {
        // Using parallel() with no PS settings triggers constructor with null/0 values
        IntStream stream = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20).parallel();
        int sum = stream.reduce(0, Integer::sum);
        assertEquals(210, sum);
    }

    @Test
    public void testReduceWithIdentity_SequentialFallback() {
        int result = IntStream.of(1, 2, 3).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1)).reduce(10, Integer::sum);

        assertEquals(16, result);
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

    @Test
    public void testCollect() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        List<Integer> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1, 2, 3), collectedList);
    }

    // collect parallel path with large array
    @Test
    public void testCollect_ParallelPath() {
        List<Integer> result = createIntStream(TEST_ARRAY).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, result.size());
        assertHaveSameElements(N.toList(TEST_ARRAY), result);
    }

    @Test
    public void testAllMatch() {
        assertTrue(createIntStream(TEST_ARRAY).allMatch(i -> i >= 1 && i <= 26));
        assertFalse(createIntStream(TEST_ARRAY).allMatch(i -> i < 26));
        assertTrue(createIntStream(new int[] {}).allMatch(i -> false));
    }

    // anyMatch/noneMatch are uncovered in parallel iterator path
    @Test
    public void testAnyMatch() {
        assertTrue(createIntStream(1, 2, 3, 4, 5).anyMatch(i -> i > 4));
        assertFalse(createIntStream(1, 2, 3, 4, 5).anyMatch(i -> i > 10));
        assertFalse(createIntStream(new int[0]).anyMatch(i -> true));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createIntStream(1, 2, 3, 4, 5).noneMatch(i -> i > 10));
        assertFalse(createIntStream(1, 2, 3, 4, 5).noneMatch(i -> i > 4));
        assertTrue(createIntStream(new int[0]).noneMatch(i -> false));
    }

    // anyMatch/allMatch/noneMatch parallel path with large array
    @Test
    public void testMatchOperations_ParallelPath() {
        assertTrue(createIntStream(TEST_ARRAY).anyMatch(i -> i > 25));
        assertFalse(createIntStream(TEST_ARRAY).anyMatch(i -> i > 100));
        assertTrue(createIntStream(TEST_ARRAY).allMatch(i -> i >= 1 && i <= 26));
        assertFalse(createIntStream(TEST_ARRAY).allMatch(i -> i < 26));
        assertTrue(createIntStream(TEST_ARRAY).noneMatch(i -> i > 100));
        assertFalse(createIntStream(TEST_ARRAY).noneMatch(i -> i == 1));
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

    // findFirst/findAny/findLast parallel path with large array
    @Test
    public void testFindOperations_ParallelPath() {
        OptionalInt first = createIntStream(TEST_ARRAY).findFirst(i -> i > 20);
        assertTrue(first.isPresent());
        assertEquals(21, first.getAsInt());

        OptionalInt any = createIntStream(TEST_ARRAY).findAny(i -> i > 20);
        assertTrue(any.isPresent());
        assertTrue(any.getAsInt() > 20);

        OptionalInt last = createIntStream(TEST_ARRAY).findLast(i -> i < 5);
        assertTrue(last.isPresent());
        assertEquals(4, last.getAsInt());
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

    // zipWith(IntStream, IntBinaryOperator) is uncovered
    @Test
    public void testZipWith_TwoStreams() {
        List<Integer> result = createIntStream(1, 2, 3).zipWith(IntStream.of(10, 20, 30), Integer::sum).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(11, result.get(0));
        assertEquals(22, result.get(1));
        assertEquals(33, result.get(2));
    }

    // zipWith(IntStream, int, int, IntBinaryOperator) is uncovered
    @Test
    public void testZipWith_TwoStreams_DefaultValues() {
        List<Integer> result = createIntStream(1, 2, 3).zipWith(IntStream.of(10, 20), 0, 0, Integer::sum).sorted().toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0)); // 3+0
        assertEquals(11, result.get(1));
        assertEquals(22, result.get(2));
    }

    // zipWith two streams parallel path with large array
    @Test
    public void testZipWithBinary_ParallelPath() {
        IntStream a = createIntStream(TEST_ARRAY);
        IntStream b = IntStream.of(TEST_ARRAY);
        List<Integer> result = a.zipWith(b, Integer::sum).sorted().toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertEquals(TEST_ARRAY[0] * 2, (int) result.get(0));
    }

    // zipWith three streams parallel path with large array
    @Test
    public void testZipWithTernary_ParallelPath() {
        IntStream a = createIntStream(TEST_ARRAY);
        IntStream b = IntStream.of(TEST_ARRAY);
        IntStream c = IntStream.of(TEST_ARRAY);
        long count = a.zipWith(b, c, (x, y, z) -> x + y + z).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    // zipWith two streams with defaults parallel path
    @Test
    public void testZipWithBinaryDefaults_ParallelPath() {
        int[] shorter = Arrays.copyOf(TEST_ARRAY, 10);
        IntStream a = createIntStream(TEST_ARRAY);
        IntStream b = IntStream.of(shorter);
        List<Integer> result = a.zipWith(b, 0, -1, Integer::sum).toList();
        assertEquals(TEST_ARRAY.length, result.size());
    }

    // zipWith three streams with defaults parallel path
    @Test
    public void testZipWithTernaryDefaults_ParallelPath() {
        int[] shorter = Arrays.copyOf(TEST_ARRAY, 10);
        IntStream a = createIntStream(TEST_ARRAY);
        IntStream b = IntStream.of(shorter);
        IntStream c = IntStream.of(shorter);
        long count = a.zipWith(b, c, 0, -1, -2, (x, y, z) -> x + y + z).count();
        assertEquals(TEST_ARRAY.length, count);
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
    @DisplayName("Test zipWith three streams with ternary operator")
    public void testZipWithTernary() {
        IntStream stream2 = createIntStream(new int[] { 10, 20, 30, 40, 50 });
        IntStream stream3 = createIntStream(new int[] { 100, 100, 100, 100, 100 });
        int[] result = parallelStream.zipWith(stream2, stream3, (a, b, c) -> (a + b) * c / 100).toArray();

        assertEquals(5, result.length);
        assertHaveSameElements(new int[] { 11, 22, 33, 44, 55 }, result);
    }

    @Test
    public void testZipWithTernaryDefaults_SequentialFallback() {
        List<Integer> result = IntStream.of(1, 2)
                .map(e -> e + 0)
                .parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1))
                .zipWith(IntStream.of(10), IntStream.of(100, 200, 300), 0, -1, -2, (x, y, z) -> x + y + z)
                .toList();

        assertEquals(Arrays.asList(111, 201, 299), result);
    }

    // isParallel should return true for a parallel iterator stream
    @Test
    public void testIsParallel() {
        assertTrue(createIntStream(TEST_ARRAY).isParallel());
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

    // sequential() caching - call sequential() twice to ensure cached instance is returned
    @Test
    public void testSequential_Cached() {
        ParallelIteratorIntStream s = (ParallelIteratorIntStream) createIntStream(TEST_ARRAY);
        IntStream seq1 = s.sequential();
        IntStream seq2 = s.sequential();
        assertSame(seq1, seq2);
        seq1.close();
    }

    // maxThreadNum=1 triggers canBeSequential=true, using sequential fallback for all operations
    @Test
    public void testFilter_SequentialFallback_maxThreadNum1() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5).map(e -> e + 0).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1)).filter(i -> i > 3).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(4));
        assertTrue(result.contains(5));
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

    // onClose with single handler
    @Test
    public void testOnClose_SingleHandler() {
        IntStream stream = createIntStream(TEST_ARRAY);
        AtomicBoolean closed = new AtomicBoolean(false);
        IntStream withClose = stream.onClose(() -> closed.set(true));
        assertFalse(closed.get());
        withClose.close();
        assertTrue(closed.get());
    }

    // onClose with null handler returns same stream
    @Test
    public void testOnClose_NullHandler() {
        IntStream stream = createIntStream(TEST_ARRAY);
        IntStream same = stream.onClose(null);
        assertSame(stream, same);
        same.close();
    }

}
