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
import org.junit.jupiter.api.Tag;

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

@Tag("new-test")
public class ParallelArrayIntStream200Test extends TestBase {
    private static final int testMaxThreadNum = 4;

    private static final int[] TEST_ARRAY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };

    protected IntStream createIntStream(int... elements) {
        return IntStream.of(elements).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
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
        IntToFloatFunction mapper = i -> (float) i / 2.0f;
        List<Float> result = stream.mapToFloat(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(0.5f));
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
    }

    @Test
    public void testMapToDouble() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        IntToDoubleFunction mapper = i -> (double) i / 2.0;
        List<Double> result = stream.mapToDouble(mapper).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(0.5));
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
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
    public void testFlatMapToLong() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<LongStream> mapper = i -> LongStream.of((long) i, (long) (i + 1));
        List<Long> result = stream.flatMapToLong(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
    }

    @Test
    public void testFlatMapToFloat() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<FloatStream> mapper = i -> FloatStream.of((float) i, (float) (i + 0.5));
        List<Float> result = stream.flatMapToFloat(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(1.5f));
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    public void testFlatMapToDouble() {
        IntStream stream = createIntStream(new int[] { 1, 2 });
        IntFunction<DoubleStream> mapper = i -> DoubleStream.of((double) i, (double) (i + 0.5));
        List<Double> result = stream.flatMapToDouble(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(1.5));
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(2.5));
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
    public void testOnEach() {
        IntStream stream = createIntStream(TEST_ARRAY);
        List<Integer> consumed = new ArrayList<>();
        IntConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.onEach(action).forEach(i -> {
        });
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
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
    public void testToMap() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        Map<String, String> result = stream.toMap(i -> String.valueOf(i), i -> "Value_" + i, (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("Value_1", result.get("1"));
        assertEquals("Value_2", result.get("2"));
        assertEquals("Value_3", result.get("3"));
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
    public void testCollect() {
        IntStream stream = createIntStream(new int[] { 1, 2, 3 });
        List<Integer> collectedList = stream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of(1, 2, 3), collectedList);
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
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
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
}
