package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;

@Tag("2025")
public class IntStream2025Test extends TestBase {

    private IntStream stream;

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
        if (stream != null) {
            stream.close();
        }
    }

    @Test
    public void testEmpty_ReturnsEmptyStream() {
        stream = IntStream.empty();
        assertFalse(stream.iterator().hasNext());
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void testOf_WithArray() {
        stream = IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testOf_WithEmptyArray() {
        stream = IntStream.of(new int[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOf_WithArrayRange() {
        stream = IntStream.of(new int[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOf_WithIntegerArray() {
        stream = IntStream.of(new Integer[] { 1, 2, 3 });
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testOf_WithIntegerArrayRange() {
        stream = IntStream.of(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOf_WithCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        stream = IntStream.of(list);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOf_WithIntIterator() {
        IntIterator iterator = IntIterator.of(1, 2, 3);
        stream = IntStream.of(iterator);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testOf_WithJdkStream() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3);
        stream = IntStream.of(jdkStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testOf_WithOptionalInt() {
        stream = IntStream.of(OptionalInt.of(42));
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testOf_WithEmptyOptionalInt() {
        stream = IntStream.of(OptionalInt.empty());
        assertEquals(0, stream.count());
    }

    @Test
    public void testOf_WithJdkOptionalInt() {
        stream = IntStream.of(java.util.OptionalInt.of(42));
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testOfNullable_WithNull() {
        stream = IntStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullable_WithValue() {
        stream = IntStream.ofNullable(42);
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testRange_BasicRange() {
        stream = IntStream.range(0, 5);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testRange_EmptyRange() {
        stream = IntStream.range(5, 5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRange_WithStep() {
        stream = IntStream.range(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, stream.toArray());
    }

    @Test
    public void testRange_WithNegativeStep() {
        stream = IntStream.range(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_BasicRange() {
        stream = IntStream.rangeClosed(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_SingleElement() {
        stream = IntStream.rangeClosed(5, 5);
        assertArrayEquals(new int[] { 5 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_WithStep() {
        stream = IntStream.rangeClosed(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8, 10 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_WithNegativeStep() {
        stream = IntStream.rangeClosed(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2, 0 }, stream.toArray());
    }

    @Test
    public void testRepeat_BasicRepeat() {
        stream = IntStream.repeat(5, 3);
        assertArrayEquals(new int[] { 5, 5, 5 }, stream.toArray());
    }

    @Test
    public void testRepeat_ZeroTimes() {
        stream = IntStream.repeat(5, 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testGenerate_LimitedElements() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.generate(() -> counter.incrementAndGet()).limit(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testIterate_WithInitAndUnaryOperator() {
        stream = IntStream.iterate(1, x -> x * 2).limit(5);
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16 }, stream.toArray());
    }

    @Test
    public void testIterate_WithPredicate() {
        stream = IntStream.iterate(1, x -> x < 100, x -> x * 2);
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16, 32, 64 }, stream.toArray());
    }

    @Test
    public void testIterate_WithBooleanSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.iterate(() -> counter.get() < 5, () -> counter.incrementAndGet());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testIterate_WithBooleanSupplierAndUnaryOperator() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.iterate(1, () -> counter.incrementAndGet() < 5, x -> x * 2);
        assertEquals(4, stream.count());
    }

    @Test
    public void testConcat_WithArrays() {
        stream = IntStream.concat(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5, 6 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testConcat_WithIterators() {
        IntIterator iter1 = IntIterator.of(1, 2);
        IntIterator iter2 = IntIterator.of(3, 4);
        stream = IntStream.concat(iter1, iter2);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcat_WithStreams() {
        IntStream s1 = IntStream.of(1, 2);
        IntStream s2 = IntStream.of(3, 4);
        stream = IntStream.concat(s1, s2);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcat_WithList() {
        List<int[]> list = Arrays.asList(new int[] { 1, 2 }, new int[] { 3, 4 });
        stream = IntStream.concat(list);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcat_WithStreamCollection() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(3, 4));
        stream = IntStream.concat(streams);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testZip_WithArrays() {
        stream = IntStream.zip(new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 }, (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZip_WithArraysThreeWay() {
        stream = IntStream.zip(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5, 6 },
                (a, b, c) -> a + b + c);
        assertArrayEquals(new int[] { 9, 12 }, stream.toArray());
    }

    @Test
    public void testZip_WithIterators() {
        IntIterator iter1 = IntIterator.of(1, 2, 3);
        IntIterator iter2 = IntIterator.of(4, 5, 6);
        stream = IntStream.zip(iter1, iter2, (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZip_WithStreams() {
        IntStream s1 = IntStream.of(1, 2, 3);
        IntStream s2 = IntStream.of(4, 5, 6);
        stream = IntStream.zip(s1, s2, (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZip_WithDefaultValues() {
        stream = IntStream.zip(new int[] { 1, 2 }, new int[] { 3, 4, 5 }, 0, 10, (a, b) -> a + b);
        assertArrayEquals(new int[] { 4, 6, 5 }, stream.toArray());
    }

    @Test
    public void testMerge_WithArrays() {
        stream = IntStream.merge(new int[] { 1, 3, 5 }, new int[] { 2, 4, 6 },
                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMerge_WithStreams() {
        IntStream s1 = IntStream.of(1, 3, 5);
        IntStream s2 = IntStream.of(2, 4, 6);
        stream = IntStream.merge(s1, s2,
                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArray() {
        int[][] array = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        stream = IntStream.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArrayVertically() {
        int[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        stream = IntStream.flatten(array, true);
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, stream.toArray());
    }

    @Test
    public void testFlatten_3DArray() {
        int[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        stream = IntStream.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, stream.toArray());
    }

    @Test
    public void testRandom_Basic() {
        stream = IntStream.random().limit(10);
        assertEquals(10, stream.count());
    }

    @Test
    public void testRandom_WithRange() {
        stream = IntStream.random(1, 10).limit(100);
        int[] result = stream.toArray();
        for (int val : result) {
            assertTrue(val >= 1 && val < 10);
        }
    }

    @Test
    public void testFilter_BasicFiltering() {
        stream = IntStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0);
        assertArrayEquals(new int[] { 2, 4 }, stream.toArray());
    }

    @Test
    public void testFilter_EmptyResult() {
        stream = IntStream.of(1, 3, 5).filter(x -> x % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFilter_AllMatch() {
        stream = IntStream.of(2, 4, 6).filter(x -> x % 2 == 0);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testTakeWhile_BasicTake() {
        stream = IntStream.of(1, 2, 3, 4, 5).takeWhile(x -> x < 4);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testTakeWhile_EmptyResult() {
        stream = IntStream.of(5, 6, 7).takeWhile(x -> x < 5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testTakeWhile_AllElements() {
        stream = IntStream.of(1, 2, 3).takeWhile(x -> x < 10);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testDropWhile_BasicDrop() {
        stream = IntStream.of(1, 2, 3, 4, 5).dropWhile(x -> x < 3);
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testDropWhile_EmptyResult() {
        stream = IntStream.of(1, 2, 3).dropWhile(x -> x < 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhile_AllElements() {
        stream = IntStream.of(5, 6, 7).dropWhile(x -> x < 5);
        assertArrayEquals(new int[] { 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testMap_BasicMapping() {
        stream = IntStream.of(1, 2, 3, 4).map(x -> x * 2);
        assertArrayEquals(new int[] { 2, 4, 6, 8 }, stream.toArray());
    }

    @Test
    public void testMap_EmptyStream() {
        stream = IntStream.empty().map(x -> x * 2);
        assertEquals(0, stream.count());
    }

    @Test
    public void testMapToChar_BasicMapping() {
        CharStream charStream = IntStream.of(65, 66, 67).mapToChar(x -> (char) x);
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, charStream.toArray());
    }

    @Test
    public void testMapToByte_BasicMapping() {
        ByteStream byteStream = IntStream.of(1, 2, 3).mapToByte(x -> (byte) x);
        assertArrayEquals(new byte[] { 1, 2, 3 }, byteStream.toArray());
    }

    @Test
    public void testMapToShort_BasicMapping() {
        ShortStream shortStream = IntStream.of(1, 2, 3).mapToShort(x -> (short) x);
        assertArrayEquals(new short[] { 1, 2, 3 }, shortStream.toArray());
    }

    @Test
    public void testMapToLong_BasicMapping() {
        LongStream longStream = IntStream.of(1, 2, 3).mapToLong(x -> (long) x);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());
    }

    @Test
    public void testMapToFloat_BasicMapping() {
        FloatStream floatStream = IntStream.of(1, 2, 3).mapToFloat(x -> (float) x);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, floatStream.toArray());
    }

    @Test
    public void testMapToDouble_BasicMapping() {
        DoubleStream doubleStream = IntStream.of(1, 2, 3).mapToDouble(x -> (double) x);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray());
    }

    @Test
    public void testMapToObj_BasicMapping() {
        Stream<String> objStream = IntStream.of(1, 2, 3).mapToObj(x -> "Value:" + x);
        assertArrayEquals(new String[] { "Value:1", "Value:2", "Value:3" }, objStream.toArray());
    }

    @Test
    public void testFlatMap_BasicFlatMap() {
        stream = IntStream.of(1, 2, 3).flatMap(x -> IntStream.of(x, x * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testFlatMap_EmptyStreams() {
        stream = IntStream.of(1, 2, 3).flatMap(x -> IntStream.empty());
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatmap_WithArrays() {
        stream = IntStream.of(1, 2, 3).flatmap(x -> new int[] { x, x * 10 });
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testFlatMapToChar_BasicFlatMap() {
        CharStream charStream = IntStream.of(65, 66).flatMapToChar(x -> CharStream.of((char) x, (char) (x + 1)));
        assertArrayEquals(new char[] { 'A', 'B', 'B', 'C' }, charStream.toArray());
    }

    @Test
    public void testFlatMapToByte_BasicFlatMap() {
        ByteStream byteStream = IntStream.of(1, 2).flatMapToByte(x -> ByteStream.of((byte) x, (byte) (x + 1)));
        assertArrayEquals(new byte[] { 1, 2, 2, 3 }, byteStream.toArray());
    }

    @Test
    public void testFlatMapToShort_BasicFlatMap() {
        ShortStream shortStream = IntStream.of(1, 2).flatMapToShort(x -> ShortStream.of((short) x, (short) (x + 1)));
        assertArrayEquals(new short[] { 1, 2, 2, 3 }, shortStream.toArray());
    }

    @Test
    public void testFlatMapToLong_BasicFlatMap() {
        LongStream longStream = IntStream.of(1, 2).flatMapToLong(x -> LongStream.of((long) x, (long) (x + 1)));
        assertArrayEquals(new long[] { 1L, 2L, 2L, 3L }, longStream.toArray());
    }

    @Test
    public void testFlatMapToFloat_BasicFlatMap() {
        FloatStream floatStream = IntStream.of(1, 2).flatMapToFloat(x -> FloatStream.of((float) x, (float) (x + 1)));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 3.0f }, floatStream.toArray());
    }

    @Test
    public void testFlatMapToDouble_BasicFlatMap() {
        DoubleStream doubleStream = IntStream.of(1, 2).flatMapToDouble(x -> DoubleStream.of((double) x, (double) (x + 1)));
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 3.0 }, doubleStream.toArray());
    }

    @Test
    public void testFlatMapToObj_BasicFlatMap() {
        Stream<String> objStream = IntStream.of(1, 2).flatMapToObj(x -> Stream.of("A" + x, "B" + x));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, objStream.toArray());
    }

    @Test
    public void testFlatmapToObj_WithCollection() {
        Stream<String> objStream = IntStream.of(1, 2).flatmapToObj(x -> Arrays.asList("A" + x, "B" + x));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, objStream.toArray());
    }

    @Test
    public void testDistinct_RemovesDuplicates() {
        stream = IntStream.of(1, 2, 2, 3, 3, 3, 4).distinct();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testDistinct_EmptyStream() {
        stream = IntStream.empty().distinct();
        assertEquals(0, stream.count());
    }

    @Test
    public void testDistinct_NoDuplicates() {
        stream = IntStream.of(1, 2, 3, 4).distinct();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testSorted_BasicSort() {
        stream = IntStream.of(3, 1, 4, 1, 5, 9, 2, 6).sorted();
        assertArrayEquals(new int[] { 1, 1, 2, 3, 4, 5, 6, 9 }, stream.toArray());
    }

    @Test
    public void testSorted_EmptyStream() {
        stream = IntStream.empty().sorted();
        assertEquals(0, stream.count());
    }

    @Test
    public void testSorted_AlreadySorted() {
        stream = IntStream.of(1, 2, 3, 4, 5).sorted();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testReversed_BasicReverse() {
        stream = IntStream.of(1, 2, 3, 4, 5).reversed();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testReversed_EmptyStream() {
        stream = IntStream.empty().reversed();
        assertEquals(0, stream.count());
    }

    @Test
    public void testReversed_SingleElement() {
        stream = IntStream.of(42).reversed();
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testPeek_ExecutesAction() {
        AtomicInteger sum = new AtomicInteger(0);
        stream = IntStream.of(1, 2, 3, 4, 5).peek(sum::addAndGet);
        stream.toArray();
        assertEquals(15, sum.get());
    }

    @Test
    public void testLimit_LimitElements() {
        stream = IntStream.of(1, 2, 3, 4, 5).limit(3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testLimit_ZeroElements() {
        stream = IntStream.of(1, 2, 3).limit(0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testLimit_MoreThanAvailable() {
        stream = IntStream.of(1, 2, 3).limit(10);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testSkip_SkipElements() {
        stream = IntStream.of(1, 2, 3, 4, 5).skip(2);
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testSkip_ZeroElements() {
        stream = IntStream.of(1, 2, 3).skip(0);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testSkip_MoreThanAvailable() {
        stream = IntStream.of(1, 2, 3).skip(10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testPrepend_BasicPrepend() {
        stream = IntStream.of(3, 4, 5).prepend(1, 2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testPrepend_EmptyArray() {
        stream = IntStream.of(1, 2, 3).prepend();
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testAppend_BasicAppend() {
        stream = IntStream.of(1, 2, 3).append(4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testAppend_EmptyArray() {
        stream = IntStream.of(1, 2, 3).append();
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testAppendIfEmpty_EmptyStream() {
        stream = IntStream.empty().appendIfEmpty(1, 2, 3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testAppendIfEmpty_NonEmptyStream() {
        stream = IntStream.of(5, 6).appendIfEmpty(1, 2, 3);
        assertArrayEquals(new int[] { 5, 6 }, stream.toArray());
    }

    @Test
    public void testTop_TopElements() {
        stream = IntStream.of(5, 3, 8, 1, 9, 2).top(3);
        assertArrayEquals(new int[] { 5, 8, 9 }, stream.toArray());
    }

    @Test
    public void testTop_WithComparator() {
        stream = IntStream.of(5, 3, 8, 1, 9, 2).top(3, Comparator.naturalOrder());
        assertArrayEquals(new int[] { 5, 8, 9 }, stream.toArray());
    }

    @Test
    public void testScan_BasicScan() {
        stream = IntStream.of(1, 2, 3, 4).scan((a, b) -> a + b);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, stream.toArray());
    }

    @Test
    public void testScan_WithInit() {
        stream = IntStream.of(1, 2, 3, 4).scan(10, (a, b) -> a + b);
        assertArrayEquals(new int[] { 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testScan_WithInitNotIncluded() {
        stream = IntStream.of(1, 2, 3, 4).scan(10, false, (a, b) -> a + b);
        assertArrayEquals(new int[] { 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testCollapse_WithMergeFunction() {
        stream = IntStream.of(1, 1, 2, 2, 3, 3).collapse((a, b) -> a == b, (a, b) -> a + b);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testForEach_ExecutesAction() {
        AtomicInteger sum = new AtomicInteger(0);
        IntStream.of(1, 2, 3, 4, 5).forEach(sum::addAndGet);
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEach_EmptyStream() {
        AtomicInteger counter = new AtomicInteger(0);
        IntStream.empty().forEach(x -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testForEachIndexed_ExecutesAction() {
        List<String> results = new ArrayList<>();
        IntStream.of(10, 20, 30).forEachIndexed((idx, val) -> results.add(idx + ":" + val));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), results);
    }

    @Test
    public void testToArray_BasicConversion() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testToArray_EmptyStream() {
        int[] result = IntStream.empty().toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testToIntList_BasicConversion() {
        IntList result = IntStream.of(1, 2, 3, 4, 5).toIntList();
        assertEquals(5, result.size());
        assertEquals(1, result.get(0));
        assertEquals(5, result.get(4));
    }

    @Test
    public void testCount_BasicCount() {
        long count = IntStream.of(1, 2, 3, 4, 5).count();
        assertEquals(5, count);
    }

    @Test
    public void testCount_EmptyStream() {
        long count = IntStream.empty().count();
        assertEquals(0, count);
    }

    @Test
    public void testAnyMatch_MatchFound() {
        boolean result = IntStream.of(1, 2, 3, 4, 5).anyMatch(x -> x > 3);
        assertTrue(result);
    }

    @Test
    public void testAnyMatch_NoMatch() {
        boolean result = IntStream.of(1, 2, 3).anyMatch(x -> x > 10);
        assertFalse(result);
    }

    @Test
    public void testAnyMatch_EmptyStream() {
        boolean result = IntStream.empty().anyMatch(x -> true);
        assertFalse(result);
    }

    @Test
    public void testAllMatch_AllMatching() {
        boolean result = IntStream.of(2, 4, 6, 8).allMatch(x -> x % 2 == 0);
        assertTrue(result);
    }

    @Test
    public void testAllMatch_NotAllMatching() {
        boolean result = IntStream.of(1, 2, 3, 4).allMatch(x -> x % 2 == 0);
        assertFalse(result);
    }

    @Test
    public void testAllMatch_EmptyStream() {
        boolean result = IntStream.empty().allMatch(x -> false);
        assertTrue(result);
    }

    @Test
    public void testNoneMatch_NoneMatching() {
        boolean result = IntStream.of(1, 3, 5, 7).noneMatch(x -> x % 2 == 0);
        assertTrue(result);
    }

    @Test
    public void testNoneMatch_SomeMatching() {
        boolean result = IntStream.of(1, 2, 3, 4).noneMatch(x -> x % 2 == 0);
        assertFalse(result);
    }

    @Test
    public void testNoneMatch_EmptyStream() {
        boolean result = IntStream.empty().noneMatch(x -> true);
        assertTrue(result);
    }

    @Test
    public void testFindFirst_ElementFound() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFindFirst_EmptyStream() {
        OptionalInt result = IntStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirst_WithPredicate() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findFirst(x -> x > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testFindAny_ElementFound() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_EmptyStream() {
        OptionalInt result = IntStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_ElementFound() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).last();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testFindLast_EmptyStream() {
        OptionalInt result = IntStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin_BasicMin() {
        OptionalInt result = IntStream.of(5, 3, 8, 1, 9, 2).min();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testMin_EmptyStream() {
        OptionalInt result = IntStream.empty().min();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin_SingleElement() {
        OptionalInt result = IntStream.of(42).min();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testMax_BasicMax() {
        OptionalInt result = IntStream.of(5, 3, 8, 1, 9, 2).max();
        assertTrue(result.isPresent());
        assertEquals(9, result.get());
    }

    @Test
    public void testMax_EmptyStream() {
        OptionalInt result = IntStream.empty().max();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax_SingleElement() {
        OptionalInt result = IntStream.of(42).max();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testKthLargest_BasicKth() {
        OptionalInt result = IntStream.of(5, 3, 8, 1, 9, 2).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(8, result.get());
    }

    @Test
    public void testKthLargest_EmptyStream() {
        OptionalInt result = IntStream.empty().kthLargest(1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testSum_BasicSum() {
        int result = IntStream.of(1, 2, 3, 4, 5).sum();
        assertEquals(15, result);
    }

    @Test
    public void testSum_EmptyStream() {
        int result = IntStream.empty().sum();
        assertEquals(0, result);
    }

    @Test
    public void testSum_NegativeNumbers() {
        int result = IntStream.of(-5, -3, -2).sum();
        assertEquals(-10, result);
    }

    @Test
    public void testAverage_BasicAverage() {
        OptionalDouble result = IntStream.of(2, 4, 6, 8).average();
        assertTrue(result.isPresent());
        assertEquals(5.0, result.get(), 0.001);
    }

    @Test
    public void testAverage_EmptyStream() {
        OptionalDouble result = IntStream.empty().average();
        assertFalse(result.isPresent());
    }

    @Test
    public void testAverage_SingleElement() {
        OptionalDouble result = IntStream.of(42).average();
        assertTrue(result.isPresent());
        assertEquals(42.0, result.get(), 0.001);
    }

    @Test
    public void testSummarize_BasicSummary() {
        IntSummaryStatistics stats = IntStream.of(1, 2, 3, 4, 5).summarize();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummarize_EmptyStream() {
        IntSummaryStatistics stats = IntStream.empty().summarize();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testReduce_WithIdentity() {
        int result = IntStream.of(1, 2, 3, 4, 5).reduce(0, (a, b) -> a + b);
        assertEquals(15, result);
    }

    @Test
    public void testReduce_WithIdentity_EmptyStream() {
        int result = IntStream.empty().reduce(10, (a, b) -> a + b);
        assertEquals(10, result);
    }

    @Test
    public void testReduce_WithoutIdentity() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(15, result.get());
    }

    @Test
    public void testReduce_WithoutIdentity_EmptyStream() {
        OptionalInt result = IntStream.empty().reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void testCollect_BasicCollect() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5)
                .collect(ArrayList::new, ArrayList::add);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testToMap_BasicMapping() {
        Map<Integer, String> result = IntStream.of(1, 2, 3)
                .toMap(x -> x, x -> "Value:" + x);
        assertEquals(3, result.size());
        assertEquals("Value:1", result.get(1));
        assertEquals("Value:2", result.get(2));
        assertEquals("Value:3", result.get(3));
    }

    @Test
    public void testToMap_WithMapSupplier() {
        Map<Integer, String> result = IntStream.of(1, 2, 3)
                .toMap(x -> x, x -> "Value:" + x, Suppliers.ofMap());
        assertEquals(3, result.size());
        assertTrue(result instanceof HashMap);
    }

    @Test
    public void testToMap_WithMergeFunction() {
        Map<Integer, Integer> result = IntStream.of(1, 2, 1, 3, 2)
                .toMap(x -> x, x -> 1, (a, b) -> a + b);
        assertEquals(3, result.size());
        assertEquals(2, result.get(1).intValue());
        assertEquals(2, result.get(2).intValue());
        assertEquals(1, result.get(3).intValue());
    }

    @Test
    public void testBoxed_BasicBoxing() {
        Stream<Integer> result = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testBoxed_EmptyStream() {
        Stream<Integer> result = IntStream.empty().boxed();
        assertEquals(0, result.count());
    }

    @Test
    public void testAsLongStream_BasicConversion() {
        LongStream result = IntStream.of(1, 2, 3).asLongStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.toArray());
    }

    @Test
    public void testAsFloatStream_BasicConversion() {
        FloatStream result = IntStream.of(1, 2, 3).asFloatStream();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray());
    }

    @Test
    public void testAsDoubleStream_BasicConversion() {
        DoubleStream result = IntStream.of(1, 2, 3).asDoubleStream();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result.toArray());
    }

    @Test
    public void testToJdkStream_BasicConversion() {
        java.util.stream.IntStream result = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testSequential_ReturnsSequentialStream() {
        stream = IntStream.of(1, 2, 3, 4, 5).sequential();
        assertNotNull(stream);
        assertFalse(stream.isParallel());
    }

    @Test
    public void testParallel_ReturnsParallelStream() {
        stream = IntStream.of(1, 2, 3, 4, 5).parallel();
        assertNotNull(stream);
        assertTrue(stream.isParallel());
    }

    @Test
    public void testIsParallel_DefaultIsFalse() {
        stream = IntStream.of(1, 2, 3, 4, 5);
        assertFalse(stream.isParallel());
    }

    @Test
    public void testOnClose_ExecutesHandler() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.of(1, 2, 3).onClose(() -> counter.incrementAndGet());
        stream.count();
        stream.close();
        assertEquals(1, counter.get());
    }

    @Test
    public void testOnClose_MultipleHandlers() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        stream = IntStream.of(1, 2, 3)
                .onClose(() -> counter1.incrementAndGet())
                .onClose(() -> counter2.incrementAndGet());
        stream.count();
        stream.close();
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    public void testIterator_BasicIteration() {
        IntIterator iterator = IntStream.of(1, 2, 3, 4, 5).iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.nextInt();
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void testIterator_EmptyStream() {
        IntIterator iterator = IntStream.empty().iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testOnlyOne_SingleElement() {
        OptionalInt result = IntStream.of(42).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testOnlyOne_EmptyStream() {
        OptionalInt result = IntStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMergeWith_BasicMerge() {
        stream = IntStream.of(1, 3, 5).mergeWith(IntStream.of(2, 4, 6),
                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testZipWith_BasicZip() {
        stream = IntStream.of(1, 2, 3).zipWith(IntStream.of(4, 5, 6), (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZipWith_ThreeWay() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4), IntStream.of(5, 6),
                (a, b, c) -> a + b + c);
        assertArrayEquals(new int[] { 9, 12 }, stream.toArray());
    }

    @Test
    public void testZipWith_WithDefaultValues() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4, 5), 0, 10, (a, b) -> a + b);
        assertArrayEquals(new int[] { 4, 6, 5 }, stream.toArray());
    }

    @Test
    public void testZipWith_ThreeWayWithDefaults() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4, 5), IntStream.of(6, 7),
                0, 10, 20, (a, b, c) -> a + b + c);
        assertArrayEquals(new int[] { 10, 13, 25 }, stream.toArray());
    }

    @Test
    public void testSplitByChunkCount_BasicSplit() {
        stream = IntStream.splitByChunkCount(10, 3, (from, to) -> from);
        assertEquals(3, stream.count());
    }

    @Test
    public void testOfIndices_BasicIndices() {
        stream = IntStream.ofIndices(5);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfIndices_WithStep() {
        stream = IntStream.ofIndices(10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, stream.toArray());
    }

    @Test
    public void testOfIndices_WithSource() {
        String source = "hello";
        stream = IntStream.ofIndices(source, (s, i) -> i < s.length() ? i : null);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testDefer_CreatesLazyStream() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.defer(() -> {
            counter.incrementAndGet();
            return IntStream.of(1, 2, 3);
        });
        assertEquals(0, counter.get());
        stream.count();
        assertEquals(1, counter.get());
    }

    @Test
    public void testFrom_JdkStream() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3);
        stream = IntStream.from(jdkStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }
}
