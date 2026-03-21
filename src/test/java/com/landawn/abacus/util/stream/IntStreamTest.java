package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;

public class IntStreamTest extends TestBase {

    private IntStream stream;

    protected IntStream createIntStream(int... a) {
        return IntStream.of(a);
    }

    protected IntStream createIntStream(int[] a, int fromIndex, int toIndex) {
        return IntStream.of(a, fromIndex, toIndex);
    }

    protected IntStream createIntStream(Integer[] a) {
        return IntStream.of(a);
    }

    protected IntStream createIntStream(Integer[] a, int fromIndex, int toIndex) {
        return IntStream.of(a, fromIndex, toIndex);
    }

    protected IntStream createIntStream(Collection<Integer> coll) {
        return IntStream.of(coll.toArray(new Integer[coll.size()]));
    }

    protected IntStream createIntStream(IntIterator iter) {
        return iter == null ? IntStream.empty() : IntStream.of(iter.toArray());
    }

    protected IntStream createIntStream(IntBuffer buff) {
        return IntStream.of(buff);
    }

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
    public void testFilter_BasicFiltering() {
        stream = IntStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0);
        assertArrayEquals(new int[] { 2, 4 }, stream.toArray());
    }

    @Test
    public void testFilter_AllMatch() {
        stream = IntStream.of(2, 4, 6).filter(x -> x % 2 == 0);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void filter_shouldFilterElements() {
        assertArrayEquals(new int[] { 2, 4 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i % 2 == 0).toArray());
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).filter(n -> n % 2 == 0, dropped::add);

        assertArrayEquals(new int[] { 2, 4 }, stream.toArray());
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testTransform() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5);
        int sum = stream.transform(s -> s.filter(n -> n % 2 == 0)).sum();
        assertEquals(6, sum);
    }

    @Test
    public void testFilter() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).filter(n -> n % 2 == 0);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testFilter_WithAction() {
        List<Integer> rejected = new ArrayList<>();
        int[] result = IntStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, value -> rejected.add(value)).toArray();
        assertArrayEquals(new int[] { 2, 4 }, result);
        assertEquals(Arrays.asList(1, 3, 5), rejected);
    }

    @Test
    public void testFilter_EmptyResult() {
        stream = IntStream.of(1, 3, 5).filter(x -> x % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testTakeWhile_BasicTake() {
        stream = IntStream.of(1, 2, 3, 4, 5).takeWhile(x -> x < 4);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testTakeWhile_AllElements() {
        stream = IntStream.of(1, 2, 3).takeWhile(x -> x < 10);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testTakeWhile() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).takeWhile(n -> n < 4);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testTakeWhile_EmptyResult() {
        stream = IntStream.of(5, 6, 7).takeWhile(x -> x < 5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhile_BasicDrop() {
        stream = IntStream.of(1, 2, 3, 4, 5).dropWhile(x -> x < 3);
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testDropWhile_AllElements() {
        stream = IntStream.of(5, 6, 7).dropWhile(x -> x < 5);
        assertArrayEquals(new int[] { 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).dropWhile(n -> n < 3, dropped::add);

        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testDropWhile() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).dropWhile(n -> n < 4);
        assertArrayEquals(new int[] { 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testDropWhile_WithAction() {
        List<Integer> dropped = new ArrayList<>();
        int[] result = IntStream.of(1, 2, 3, 4, 5).dropWhile(x -> x <= 3, value -> dropped.add(value)).toArray();
        assertArrayEquals(new int[] { 4, 5 }, result);
        assertEquals(Arrays.asList(1, 2, 3), dropped);
    }

    @Test
    public void testDropWhile_EmptyResult() {
        stream = IntStream.of(1, 2, 3).dropWhile(x -> x < 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testMap_BasicMapping() {
        stream = IntStream.of(1, 2, 3, 4).map(x -> x * 2);
        assertArrayEquals(new int[] { 2, 4, 6, 8 }, stream.toArray());
    }

    @Test
    public void map_shouldTransformElements() {
        assertArrayEquals(new int[] { 2, 4, 6 }, IntStream.of(1, 2, 3).map(i -> i * 2).toArray());
    }

    @Test
    public void testMap() {
        stream = createIntStream(1, 2, 3).map(n -> n * 2);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
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
    public void testMapToLong() {
        LongStream longStream = createIntStream(1, 2, 3).mapToLong(n -> n * 1000000000L);
        assertArrayEquals(new long[] { 1000000000L, 2000000000L, 3000000000L }, longStream.toArray());
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
    public void testMapToDouble() {
        DoubleStream doubleStream = createIntStream(1, 2, 3).mapToDouble(n -> n * 0.5);
        assertArrayEquals(new double[] { 0.5, 1.0, 1.5 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testMapToObj_BasicMapping() {
        Stream<String> objStream = IntStream.of(1, 2, 3).mapToObj(x -> "Value:" + x);
        assertArrayEquals(new String[] { "Value:1", "Value:2", "Value:3" }, objStream.toArray());
    }

    @Test
    public void testMapToObj() {
        Stream<String> objStream = createIntStream(1, 2, 3).mapToObj(String::valueOf);
        assertArrayEquals(new String[] { "1", "2", "3" }, objStream.toArray());
    }

    @Test
    public void testFlatMap_BasicFlatMap() {
        stream = IntStream.of(1, 2, 3).flatMap(x -> IntStream.of(x, x * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testFlatmap_WithArrays() {
        stream = IntStream.of(1, 2, 3).flatmap(x -> new int[] { x, x * 10 });
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testFlatMap() {
        stream = createIntStream(1, 2, 3).flatMap(n -> createIntStream(n, n * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testFlatmap_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10 }).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatMap_EmptyStreams() {
        stream = IntStream.of(1, 2, 3).flatMap(x -> IntStream.empty());
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlattMap_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10)).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlattMap_Empty() {
        IntStream result = IntStream.empty().flattMap(n -> java.util.stream.IntStream.of(n, n * 10));
        assertEquals(0, result.count());
    }

    @Test
    public void testFlatMapToChar_BasicFlatMap() {
        CharStream charStream = IntStream.of(65, 66).flatMapToChar(x -> CharStream.of((char) x, (char) (x + 1)));
        assertArrayEquals(new char[] { 'A', 'B', 'B', 'C' }, charStream.toArray());
    }

    @Test
    public void testFlatMapToChar() {
        CharStream charStream = createIntStream(65, 66, 67).flatMapToChar(n -> CharStream.of((char) n, (char) (n + 32)));
        assertArrayEquals(new char[] { 'A', 'a', 'B', 'b', 'C', 'c' }, charStream.toArray());
    }

    @Test
    public void testFlatMapToByte_BasicFlatMap() {
        ByteStream byteStream = IntStream.of(1, 2).flatMapToByte(x -> ByteStream.of((byte) x, (byte) (x + 1)));
        assertArrayEquals(new byte[] { 1, 2, 2, 3 }, byteStream.toArray());
    }

    @Test
    public void testFlatMapToByte() {
        ByteStream byteStream = createIntStream(1, 2, 3).flatMapToByte(n -> ByteStream.of((byte) n, (byte) (n * 10)));
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, byteStream.toArray());
    }

    @Test
    public void testFlatMapToShort_BasicFlatMap() {
        ShortStream shortStream = IntStream.of(1, 2).flatMapToShort(x -> ShortStream.of((short) x, (short) (x + 1)));
        assertArrayEquals(new short[] { 1, 2, 2, 3 }, shortStream.toArray());
    }

    @Test
    public void testFlatMapToShort() {
        ShortStream shortStream = createIntStream(1, 2, 3).flatMapToShort(n -> ShortStream.of((short) n, (short) (n * 100)));
        assertArrayEquals(new short[] { 1, 100, 2, 200, 3, 300 }, shortStream.toArray());
    }

    @Test
    public void testFlatMapToLong_BasicFlatMap() {
        LongStream longStream = IntStream.of(1, 2).flatMapToLong(x -> LongStream.of(x, x + 1));
        assertArrayEquals(new long[] { 1L, 2L, 2L, 3L }, longStream.toArray());
    }

    @Test
    public void testFlatMapToFloat_BasicFlatMap() {
        FloatStream floatStream = IntStream.of(1, 2).flatMapToFloat(x -> FloatStream.of(x, x + 1));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 3.0f }, floatStream.toArray());
    }

    @Test
    public void testFlatMapToFloat() {
        FloatStream floatStream = createIntStream(1, 2, 3).flatMapToFloat(n -> FloatStream.of(n * 0.1f, n * 0.2f));
        assertArrayEquals(new float[] { 0.1f, 0.2f, 0.2f, 0.4f, 0.3f, 0.6f }, floatStream.toArray(), 0.001f);
    }

    @Test
    public void testFlatMapToDouble_BasicFlatMap() {
        DoubleStream doubleStream = IntStream.of(1, 2).flatMapToDouble(x -> DoubleStream.of(x, x + 1));
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
    public void testFlatmapToObj_HappyPath() {
        List<String> result = IntStream.of(1, 2).flatmapToObj(i -> Arrays.asList(String.valueOf(i), String.valueOf(i * 10))).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20"), result);
    }

    @Test
    public void testFlatMapArrayToObj() {
        Stream<String> result = createIntStream(1, 2, 3).flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, result.toArray(String[]::new));
    }

    @Test
    public void testFlatMapArrayToObj_HappyPath() {
        List<String> result = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { String.valueOf(i), String.valueOf(i * 10) }).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20", "3", "30"), result);
    }

    @Test
    public void testFlatMapArrayToObj_Empty() {
        Stream<String> result = IntStream.empty().flatMapArrayToObj(n -> new String[] { "A" + n });
        assertEquals(0, result.count());
    }

    @Test
    public void testMapMulti() {
        IntStream stream = createIntStream(1, 2, 3).mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        });
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testMapMulti_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        }).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testMapMulti_Empty() {
        int[] result = IntStream.empty().mapMulti((value, consumer) -> consumer.accept(value)).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testMapPartial() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5).mapPartial(n -> n % 2 == 0 ? OptionalInt.of(n * 2) : OptionalInt.empty());
        assertArrayEquals(new int[] { 4, 8 }, stream.toArray());
    }

    @Test
    public void testMapPartial_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.of(i * 10) : OptionalInt.empty()).toArray();
        assertArrayEquals(new int[] { 20, 40 }, result);
    }

    @Test
    public void testMapPartialJdk() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5).mapPartialJdk(n -> n % 2 == 0 ? java.util.OptionalInt.of(n * 2) : java.util.OptionalInt.empty());
        assertArrayEquals(new int[] { 4, 8 }, stream.toArray());
    }

    @Test
    public void testMapPartialJdk_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.of(i * 10) : java.util.OptionalInt.empty()).toArray();
        assertArrayEquals(new int[] { 20, 40 }, result);
    }

    @Test
    public void testRangeMap() {
        IntStream stream = createIntStream(1, 1, 2, 2, 2, 3).rangeMap((a, b) -> a == b, (first, last) -> first * 10 + last);
        assertArrayEquals(new int[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testRangeMap_HappyPath() {
        int[] result = IntStream.of(1, 1, 2, 2, 3).rangeMap((a, b) -> a == b, Integer::sum).toArray();
        assertArrayEquals(new int[] { 2, 4, 6 }, result);
    }

    @Test
    public void testRangeMap_AllSame() {
        int[] result = IntStream.of(5, 5, 5).rangeMap((a, b) -> a == b, Integer::sum).toArray();
        assertArrayEquals(new int[] { 10 }, result);
    }

    @Test
    public void testRangeMapToObj() {
        Stream<String> stream = createIntStream(1, 1, 2, 2, 2, 3).rangeMapToObj((a, b) -> a == b, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "1-1", "2-2", "3-3" }, stream.toArray());
    }

    @Test
    public void testRangeMapToObj_HappyPath() {
        List<String> result = IntStream.of(1, 1, 2, 3, 3).rangeMapToObj((a, b) -> a == b, (a, b) -> a + "-" + b).toList();
        assertEquals(Arrays.asList("1-1", "2-2", "3-3"), result);
    }

    @Test
    public void testCollapse_WithMergeFunction() {
        stream = IntStream.of(1, 1, 2, 2, 3, 3).collapse((a, b) -> a == b, (a, b) -> a + b);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testCollapseNonCollapsible() {
        stream = IntStream.of(1, 10, 100);
        int[] result = stream.collapse((prev, next) -> false, (a, b) -> a + b).toArray();
        assertArrayEquals(new int[] { 1, 10, 100 }, result);
    }

    @Test
    public void testCollapseWithMergeFunction() {
        IntStream stream = createIntStream(1, 1, 2, 2, 2, 3, 3).collapse((a, b) -> a == b, Integer::sum);
        assertArrayEquals(new int[] { 2, 6, 6 }, stream.toArray());
    }

    @Test
    public void testCollapse() {
        Stream<IntList> collapsed = createIntStream(1, 1, 2, 2, 2, 3, 3).collapse((a, b) -> a == b);
        List<IntList> result = collapsed.toList();

        assertEquals(3, result.size());
        assertArrayEquals(new int[] { 1, 1 }, result.get(0).toArray());
        assertArrayEquals(new int[] { 2, 2, 2 }, result.get(1).toArray());
        assertArrayEquals(new int[] { 3, 3 }, result.get(2).toArray());
    }

    @Test
    public void testCollapse_ListForm() {
        List<IntList> result = IntStream.of(1, 1, 2, 2, 3).collapse((a, b) -> a == b).toList();
        assertEquals(3, result.size());
        assertArrayEquals(new int[] { 1, 1 }, result.get(0).toArray());
        assertArrayEquals(new int[] { 2, 2 }, result.get(1).toArray());
        assertArrayEquals(new int[] { 3 }, result.get(2).toArray());
    }

    @Test
    public void testCollapse_WithTriPredicate() {
        int[] result = IntStream.of(1, 2, 3, 10, 11, 12).collapse((first, previous, current) -> current - first <= 2, Integer::sum).toArray();
        assertArrayEquals(new int[] { 6, 33 }, result);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        IntStream stream = createIntStream(1, 2, 3, 5, 6, 8, 9).collapse((first, prev, curr) -> curr - first <= 2, Integer::max);
        assertArrayEquals(new int[] { 3, 6, 9 }, stream.toArray());
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
    public void testScanWithInit() {
        IntStream stream = createIntStream(1, 2, 3, 4).scan(10, Integer::sum);
        assertArrayEquals(new int[] { 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testScanWithInitIncluded() {
        IntStream stream = createIntStream(1, 2, 3, 4).scan(10, true, Integer::sum);
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testScan() {
        stream = createIntStream(1, 2, 3, 4).scan(Integer::sum);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, stream.toArray());

        stream = createIntStream(1, 2, 3, 4).scan(0, Integer::sum);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, stream.toArray());
    }

    @Test
    public void testScan_WithInitIncluded() {
        int[] result = IntStream.of(1, 2, 3).scan(10, true, Integer::sum).toArray();
        assertArrayEquals(new int[] { 10, 11, 13, 16 }, result);
    }

    @Test
    public void testScanEmptyStream() {
        stream = IntStream.empty();
        int[] result = stream.scan((a, b) -> a + b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testPrepend_BasicPrepend() {
        stream = IntStream.of(3, 4, 5).prepend(1, 2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void prepend_shouldPrependElements() {
        assertArrayEquals(new int[] { 3, 4, 1, 2 }, IntStream.of(1, 2).prepend(3, 4).toArray());
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
    public void append_shouldAppendElements() {
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.of(1, 2).append(3, 4).toArray());
    }

    @Test
    public void testAppendPrepend() {
        stream = createIntStream(1, 2, 3).append(4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());

        stream = createIntStream(3, 4, 5).prepend(1, 2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testAppend_EmptyArray() {
        stream = IntStream.of(1, 2, 3).append();
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testAppendPrependWithOptional() {
        IntStream stream = createIntStream(2, 3).prepend(OptionalInt.of(1));
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        stream = createIntStream(1, 2).append(OptionalInt.of(3));
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        stream = createIntStream(1, 2).append(OptionalInt.empty());
        assertArrayEquals(new int[] { 1, 2 }, stream.toArray());
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
    public void testAppendIfEmpty() {
        IntStream stream = createIntStream(1, 2, 3).appendIfEmpty(4, 5, 6);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        stream = IntStream.empty().appendIfEmpty(4, 5, 6);
        assertArrayEquals(new int[] { 4, 5, 6 }, stream.toArray());
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
    public void testTopWithComparator() {
        IntStream stream = createIntStream(5, 2, 8, 1, 9, 3, 7).top(3, (a, b) -> b - a);
        int[] bottom3 = stream.sorted().toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, bottom3);
    }

    @Test
    public void testTop() {
        stream = createIntStream(5, 2, 8, 1, 9, 3, 7).top(3);
        int[] top3 = stream.sorted().toArray();
        assertArrayEquals(new int[] { 7, 8, 9 }, top3);
    }

    @Test
    public void testToIntList_BasicConversion() {
        IntList result = IntStream.of(1, 2, 3, 4, 5).toIntList();
        assertEquals(5, result.size());
        assertEquals(1, result.get(0));
        assertEquals(5, result.get(4));
    }

    @Test
    public void toIntList_shouldReturnIntList() {
        IntList expected = IntList.of(1, 2, 3);
        assertEquals(expected, IntStream.of(1, 2, 3).toIntList());
    }

    @Test
    public void testToIntList() {
        IntList list = createIntStream(1, 2, 3).toIntList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testToMap_BasicMapping() {
        Map<Integer, String> result = IntStream.of(1, 2, 3).toMap(x -> x, x -> "Value:" + x);
        assertEquals(3, result.size());
        assertEquals("Value:1", result.get(1));
        assertEquals("Value:2", result.get(2));
        assertEquals("Value:3", result.get(3));
    }

    @Test
    public void testToMap_WithMapSupplier() {
        Map<Integer, String> result = IntStream.of(1, 2, 3).toMap(x -> x, x -> "Value:" + x, Suppliers.ofMap());
        assertEquals(3, result.size());
        assertTrue(result instanceof HashMap);
    }

    @Test
    public void testToMap_WithMergeFunction() {
        Map<Integer, Integer> result = IntStream.of(1, 2, 1, 3, 2).toMap(x -> x, x -> 1, (a, b) -> a + b);
        assertEquals(3, result.size());
        assertEquals(2, result.get(1).intValue());
        assertEquals(2, result.get(2).intValue());
        assertEquals(1, result.get(3).intValue());
    }

    @Test
    public void testToMap() {
        Map<String, Integer> map = createIntStream(1, 2, 3).toMap(n -> "key" + n, n -> n * 10);

        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(10), map.get("key1"));
        assertEquals(Integer.valueOf(20), map.get("key2"));
        assertEquals(Integer.valueOf(30), map.get("key3"));
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<Boolean, Integer> map = createIntStream(1, 2, 3, 4, 5, 6).toMap(n -> n % 2 == 0, n -> n, Integer::sum);

        assertEquals(Integer.valueOf(12), map.get(true));
        assertEquals(Integer.valueOf(9), map.get(false));
    }

    @Test
    public void testToMap_WithMergeFunction2() {
        Map<Integer, Integer> result = IntStream.of(1, 2, 1, 3, 2).toMap(i -> i, i -> 1, Integer::sum);
        assertEquals(3, result.size());
        assertEquals(2, result.get(1).intValue());
        assertEquals(2, result.get(2).intValue());
        assertEquals(1, result.get(3).intValue());
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Integer>> grouped = createIntStream(1, 2, 3, 4, 5, 6).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList());

        assertEquals(Arrays.asList(2, 4, 6), grouped.get(true));
        assertEquals(Arrays.asList(1, 3, 5), grouped.get(false));
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        java.util.TreeMap<Boolean, List<Integer>> grouped = createIntStream(1, 2, 3, 4, 5, 6).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(),
                java.util.TreeMap::new);
        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(2, 4, 6), grouped.get(true));
    }

    @Test
    public void testReduce_WithIdentity() {
        int result = IntStream.of(1, 2, 3, 4, 5).reduce(0, (a, b) -> a + b);
        assertEquals(15, result);
    }

    @Test
    public void testReduce_WithoutIdentity() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(15, result.get());
    }

    @Test
    public void reduce_withIdentityAndAccumulator_shouldPerformReduction() {
        int result = IntStream.of(1, 2, 3).reduce(0, (a, b) -> a + b);
        assertEquals(6, result);
    }

    @Test
    public void testReduce() {
        int sum = createIntStream(1, 2, 3, 4, 5).reduce(0, Integer::sum);
        assertEquals(15, sum);

        OptionalInt product = createIntStream(2, 3, 4).reduce((a, b) -> a * b);
        assertTrue(product.isPresent());
        assertEquals(24, product.getAsInt());
    }

    @Test
    public void testReduce_WithIdentity_EmptyStream() {
        int result = IntStream.empty().reduce(10, (a, b) -> a + b);
        assertEquals(10, result);
    }

    @Test
    public void testReduce_WithoutIdentity_EmptyStream() {
        OptionalInt result = IntStream.empty().reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void reduce_withAccumulator_shouldPerformReduction() {
        int result = IntStream.of(1, 2, 3).reduce((a, b) -> a + b).orElseThrow();
        assertEquals(6, result);
    }

    @Test
    public void testCollect_BasicCollect() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Integer> result = IntStream.of(1, 2, 3, 4, 5).collect(ArrayList::new, ArrayList::add);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testCollect() {
        IntList list = createIntStream(1, 2, 3).collect(IntList::new, IntList::add);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
    }

    @Test
    public void testCollect_WithCombiner() {
        List<Integer> result = createIntStream(1, 2, 3).collect(ArrayList::new, (list, n) -> list.add(n), (l1, l2) -> l1.addAll(l2));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollect_TwoArg() {
        List<Integer> result = IntStream.of(1, 2, 3).collect(ArrayList::new, (list, val) -> list.add(val));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEach_ExecutesAction() {
        AtomicInteger sum = new AtomicInteger(0);
        IntStream.of(1, 2, 3, 4, 5).forEach(sum::addAndGet);
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEach() {
        List<Integer> list = new ArrayList<>();
        createIntStream(1, 2, 3).forEach(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testForeach() {
        List<Integer> collected = new ArrayList<>();
        createIntStream(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForeach_HappyPath() {
        List<Integer> collected = new ArrayList<>();
        IntStream.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEach_EmptyStream() {
        AtomicInteger counter = new AtomicInteger(0);
        IntStream.empty().forEach(x -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testForeach_Empty() {
        List<Integer> collected = new ArrayList<>();
        IntStream.empty().foreach(collected::add);
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEachIndexed_ExecutesAction() {
        List<String> results = new ArrayList<>();
        IntStream.of(10, 20, 30).forEachIndexed((idx, val) -> results.add(idx + ":" + val));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), results);
    }

    @Test
    public void testForEachIndexed() throws Exception {
        Map<Integer, Integer> indexToValue = new HashMap<>();
        createIntStream(10, 20, 30).forEachIndexed((index, value) -> {
            indexToValue.put(index, value);
        });

        assertEquals(3, indexToValue.size());
        assertEquals(Integer.valueOf(10), indexToValue.get(0));
        assertEquals(Integer.valueOf(20), indexToValue.get(1));
        assertEquals(Integer.valueOf(30), indexToValue.get(2));
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
    public void anyMatch_shouldReturnTrueIfAnyElementMatches() {
        assertTrue(IntStream.of(1, 2, 3).anyMatch(i -> i % 2 == 0));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(IntStream.of(1, 2, 3, 4, 5).anyMatch(n -> n > 4));
        assertFalse(IntStream.of(1, 2, 3).anyMatch(n -> n > 5));
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
    public void allMatch_shouldReturnTrueIfAllElementsMatch() {
        assertTrue(IntStream.of(2, 4, 6).allMatch(i -> i % 2 == 0));
    }

    @Test
    public void allMatch_shouldReturnFalseIfAnyElementDoesNotMatch() {
        assertFalse(IntStream.of(1, 2, 3).allMatch(i -> i % 2 == 0));
    }

    @Test
    public void testAllMatch() {
        assertTrue(IntStream.of(2, 4, 6, 8).allMatch(n -> n % 2 == 0));
        assertFalse(IntStream.of(2, 4, 5, 8).allMatch(n -> n % 2 == 0));
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
    public void noneMatch_shouldReturnTrueIfNoElementsMatch() {
        assertTrue(IntStream.of(1, 3, 5).noneMatch(i -> i % 2 == 0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(IntStream.of(1, 3, 5, 7).noneMatch(n -> n % 2 == 0));
        assertFalse(IntStream.of(1, 3, 4, 7).noneMatch(n -> n % 2 == 0));
    }

    @Test
    public void testFindFirst_ElementFound() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFindFirst_WithPredicate() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findFirst(x -> x > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testFindFirst_NoArg() {
        OptionalInt result = createIntStream(10, 20, 30).findFirst();
        assertTrue(result.isPresent());
        assertEquals(10, result.getAsInt());
    }

    @Test
    public void testFindFirst_EmptyStream() {
        OptionalInt result = IntStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void findFirst_onEmptyStream_shouldReturnEmpty() {
        assertTrue(IntStream.empty().first().isEmpty());
    }

    @Test
    public void testFindFirst() {
        OptionalInt first = createIntStream(1, 2, 3).first();
        assertTrue(first.isPresent());
        assertEquals(1, first.getAsInt());

        assertFalse(IntStream.empty().first().isPresent());
    }

    @Test
    public void testFindFirst_NoArg_Empty() {
        OptionalInt result = IntStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void findFirst_onNonEmptyStream_shouldReturnFirstElement() {
        assertEquals(1, IntStream.of(1, 2, 3).first().orElseThrow());
    }

    @Test
    public void testFindFirstWithPredicate() throws Exception {
        OptionalInt found = createIntStream(1, 2, 3, 4, 5).findFirst(n -> n > 3);
        assertTrue(found.isPresent());
        assertEquals(4, found.getAsInt());
    }

    @Test
    public void testFindAny_ElementFound() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny() {
        OptionalInt any = createIntStream(1, 2, 3).first();
        assertTrue(any.isPresent());
        assertTrue(Arrays.asList(1, 2, 3).contains(any.getAsInt()));
    }

    @Test
    public void testFindAny_NoArg() {
        OptionalInt result = createIntStream(10, 20, 30).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_EmptyStream() {
        OptionalInt result = IntStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg_Empty() {
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
    public void testFindLast() {
        OptionalInt last = createIntStream(1, 2, 3).last();
        assertTrue(last.isPresent());
        assertEquals(3, last.getAsInt());
    }

    @Test
    public void testFindLast_EmptyStream() {
        OptionalInt result = IntStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLastWithPredicate() throws Exception {
        OptionalInt found = createIntStream(1, 2, 3, 4, 5).findLast(n -> n < 4);
        assertTrue(found.isPresent());
        assertEquals(3, found.getAsInt());
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
    public void testMin() {
        OptionalInt min = createIntStream(3, 1, 4, 1, 5).min();
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsInt());

        assertFalse(IntStream.empty().min().isPresent());
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
    public void testMax() {
        OptionalInt max = createIntStream(3, 1, 4, 1, 5).max();
        assertTrue(max.isPresent());
        assertEquals(5, max.getAsInt());

        assertFalse(IntStream.empty().max().isPresent());
    }

    @Test
    public void testKthLargest_BasicKth() {
        OptionalInt result = IntStream.of(5, 3, 8, 1, 9, 2).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(8, result.get());
    }

    @Test
    public void testKthLargest() {
        OptionalInt kth = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).kthLargest(3);
        assertTrue(kth.isPresent());
        assertEquals(5, kth.getAsInt());
    }

    @Test
    public void testKthLargest_SortedBranch() {
        OptionalInt result = createIntStream(1, 2, 3, 4).sorted().kthLargest(2);

        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
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
    public void sum_shouldReturnSumOfElements() {
        assertEquals(6, IntStream.of(1, 2, 3).sum());
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
    public void sum_onEmptyStream_shouldReturnZero() {
        assertEquals(0, IntStream.empty().sum());
    }

    @Test
    public void testSum() {
        assertEquals(15, createIntStream(1, 2, 3, 4, 5).sum());
        assertEquals(0, IntStream.empty().sum());
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
    public void average_onEmptyStream_shouldReturnEmpty() {
        assertTrue(IntStream.empty().average().isEmpty());
    }

    @Test
    public void testAverage() {
        OptionalDouble avg = createIntStream(1, 2, 3, 4, 5).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);

        assertFalse(IntStream.empty().average().isPresent());
    }

    @Test
    public void average_shouldReturnAverageOfElements() {
        assertEquals(2.0, IntStream.of(1, 2, 3).average().orElseThrow(), 0.001);
    }

    @Test
    public void testsummaryStatistics() {
        IntSummaryStatistics stats = createIntStream(1, 2, 3, 4, 5).summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testMergeWith_BasicMerge() {
        stream = IntStream.of(1, 3, 5).mergeWith(IntStream.of(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeWithCollection() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 5), IntStream.of(2, 6), IntStream.of(3, 7));
        stream = IntStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeWith() {
        IntStream s1 = createIntStream(1, 3, 5);
        IntStream s2 = createIntStream(2, 4, 6);
        stream = s1.mergeWith(s2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testZipWith_BasicZip() {
        stream = IntStream.of(1, 2, 3).zipWith(IntStream.of(4, 5, 6), (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZipWith_ThreeWay() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4), IntStream.of(5, 6), (a, b, c) -> a + b + c);
        assertArrayEquals(new int[] { 9, 12 }, stream.toArray());
    }

    @Test
    public void testZipWith_WithDefaultValues() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4, 5), 0, 10, (a, b) -> a + b);
        assertArrayEquals(new int[] { 4, 6, 5 }, stream.toArray());
    }

    @Test
    public void testZipWith_ThreeWayWithDefaults() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4, 5), IntStream.of(6, 7), 0, 10, 20, (a, b, c) -> a + b + c);
        assertArrayEquals(new int[] { 10, 13, 25 }, stream.toArray());
    }

    // Additional coverage tests for 2025

    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10, 20), IntStream.of(100, 200));
        stream = IntStream.zip(streams, ints -> {
            int sum = 0;
            for (Integer i : ints)
                sum += i;
            return sum;
        });
        assertArrayEquals(new int[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipWithCollectionDefaultValues() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10));
        int[] defaults = new int[] { 0, 0 };
        stream = IntStream.zip(streams, defaults, ints -> {
            int sum = 0;
            for (Integer i : ints)
                sum += i;
            return sum;
        });
        assertEquals(2, stream.count());
    }

    @Test
    public void testZipWithDefault() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20);
        IntStream zipped = s1.zipWith(s2, 0, 100, Integer::sum);

        assertArrayEquals(new int[] { 11, 22, 103 }, zipped.toArray());
    }

    @Test
    public void testZipWith_CollectionOfStreams() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30));
        int[] result = IntStream.zip(streams, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new int[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZipWith_CollectionOfStreamsWithDefaults() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10, 20, 30));
        int[] result = IntStream.zip(streams, new int[] { 0, 0 }, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new int[] { 11, 22, 30 }, result);
    }

    @Test
    public void testAsLongStream_BasicConversion() {
        LongStream result = IntStream.of(1, 2, 3).asLongStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.toArray());
    }

    @Test
    public void testAsLongStream() {
        LongStream longStream = createIntStream(1, 2, 3).asLongStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());
    }

    @Test
    public void testAsXXXStream() {
        {
            LongStream longStream = createIntStream(1, 2, 3).asLongStream();
            assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());

            FloatStream floatStream = createIntStream(1, 2, 3).asFloatStream().skip(1);
            assertArrayEquals(new float[] { 2.0f, 3.0f }, floatStream.toArray());

            DoubleStream doubleStream = createIntStream(1, 2, 3).asDoubleStream();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray(), 0.001);
        }
        {
            LongStream longStream = createIntStream(1, 2, 3).map(e -> e).asLongStream();
            assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());

            FloatStream floatStream = createIntStream(1, 2, 3).map(e -> e).asFloatStream().skip(1);
            assertArrayEquals(new float[] { 2.0f, 3.0f }, floatStream.toArray());

            DoubleStream doubleStream = createIntStream(1, 2, 3).map(e -> e).asDoubleStream();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray(), 0.001);
        }
    }

    @Test
    public void testAsLongStream_HappyPath() {
        long[] result = IntStream.of(1, 2, 3).asLongStream().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testAsFloatStream_BasicConversion() {
        FloatStream result = IntStream.of(1, 2, 3).asFloatStream();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray());
    }

    @Test
    public void testAsFloatStream() {
        float[] result = IntStream.of(1, 2, 3).asFloatStream().toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(2.0f, result[1], 0.001f);
    }

    @Test
    public void testAsDoubleStream_BasicConversion() {
        DoubleStream result = IntStream.of(1, 2, 3).asDoubleStream();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result.toArray());
    }

    @Test
    public void testAsDoubleStream() {
        DoubleStream doubleStream = createIntStream(1, 2, 3).asDoubleStream();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testAsDoubleStream_HappyPath2() {
        double[] result = IntStream.of(1, 2, 3).asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testBoxed_BasicBoxing() {
        Stream<Integer> result = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testBoxed() {
        Stream<Integer> boxed = createIntStream(1, 2, 3).boxed();
        assertArrayEquals(new Integer[] { 1, 2, 3 }, boxed.toArray());
    }

    @Test
    public void testBoxed_EmptyStream() {
        Stream<Integer> result = IntStream.empty().boxed();
        assertEquals(0, result.count());
    }

    @Test
    public void testToJdkStream_BasicConversion() {
        java.util.stream.IntStream result = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.IntStream javaStream = createIntStream(1, 2, 3).toJdkStream();
        assertArrayEquals(new int[] { 1, 2, 3 }, javaStream.toArray());
    }

    // TODO: filter(IntPredicate) is abstract - tested via concrete implementations above
    // TODO: takeWhile(IntPredicate) is abstract - tested via concrete implementations above
    // TODO: dropWhile(IntPredicate) is abstract - tested via concrete implementations above
    // TODO: map(IntUnaryOperator) is abstract - tested via concrete implementations above
    // TODO: mapToChar(IntToCharFunction) is abstract - tested via concrete implementations above
    // TODO: mapToByte(IntToByteFunction) is abstract - tested via concrete implementations above
    // TODO: mapToShort(IntToShortFunction) is abstract - tested via concrete implementations above
    // TODO: mapToLong(IntToLongFunction) is abstract - tested via concrete implementations above
    // TODO: mapToFloat(IntToFloatFunction) is abstract - tested via concrete implementations above
    // TODO: mapToDouble(IntToDoubleFunction) is abstract - tested via concrete implementations above
    // TODO: mapToObj(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatMap(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatmap(IntFunction<int[]>) is abstract - tested via concrete implementations above
    // TODO: flatMapToByte(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToChar(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToShort(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToLong(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToFloat(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToDouble(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToObj(IntFunction) is abstract - tested via concrete implementations above
    // TODO: flatmapToObj(IntFunction) is abstract - tested via concrete implementations above
    // TODO: mapMulti(IntMapMultiConsumer) is abstract - tested via concrete implementations above
    // TODO: mapPartial(IntFunction) is abstract - tested via concrete implementations above
    // TODO: mapPartialJdk(IntFunction) is abstract - tested via concrete implementations above
    // TODO: rangeMap(IntBiPredicate, IntBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: rangeMapToObj(IntBiPredicate, IntBiFunction) is abstract - tested via concrete implementations above
    // TODO: collapse(IntBiPredicate) is abstract - tested via concrete implementations above
    // TODO: collapse(IntBiPredicate, IntBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: collapse(IntTriPredicate, IntBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: scan(IntBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: scan(int, IntBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: scan(int, boolean, IntBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: prepend(int...) is abstract - tested via concrete implementations above
    // TODO: append(int...) is abstract - tested via concrete implementations above
    // TODO: appendIfEmpty(int...) is abstract - tested via concrete implementations above
    // TODO: top(int) is abstract - tested via concrete implementations above
    // TODO: top(int, Comparator) is abstract - tested via concrete implementations above
    // TODO: toIntList() is abstract - tested via concrete implementations above
    // TODO: toMap(...) overloads are abstract - tested via concrete implementations above
    // TODO: groupTo(...) overloads are abstract - tested via concrete implementations above
    // TODO: reduce(int, IntBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: reduce(IntBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: collect(...) overloads are abstract - tested via concrete implementations above
    // TODO: forEach(Throwables.IntConsumer) is abstract - tested via concrete implementations above
    // TODO: forEachIndexed(Throwables.IntIntConsumer) is abstract - tested via concrete implementations above
    // TODO: anyMatch(Throwables.IntPredicate) is abstract - tested via concrete implementations above
    // TODO: allMatch(Throwables.IntPredicate) is abstract - tested via concrete implementations above
    // TODO: noneMatch(Throwables.IntPredicate) is abstract - tested via concrete implementations above
    // TODO: findFirst(Throwables.IntPredicate) is abstract - tested via concrete implementations above
    // TODO: findAny(Throwables.IntPredicate) is abstract - tested via concrete implementations above
    // TODO: findLast(Throwables.IntPredicate) is abstract - tested via concrete implementations above
    // TODO: min() is abstract - tested via concrete implementations above
    // TODO: max() is abstract - tested via concrete implementations above
    // TODO: kthLargest(int) is abstract - tested via concrete implementations above
    // TODO: sum() is abstract - tested via concrete implementations above
    // TODO: average() is abstract - tested via concrete implementations above
    // TODO: summaryStatistics() is abstract - tested via concrete implementations above
    // TODO: summaryStatisticsAndPercentiles() is abstract - tested via concrete implementations above
    // TODO: mergeWith(IntStream, IntBiFunction) is abstract - tested via concrete implementations above
    // TODO: zipWith(...) overloads are abstract - tested via concrete implementations above
    // TODO: asLongStream() is abstract - tested via concrete implementations above
    // TODO: asFloatStream() is abstract - tested via concrete implementations above
    // TODO: asDoubleStream() is abstract - tested via concrete implementations above
    // TODO: boxed() is abstract - tested via concrete implementations above
    // TODO: toJdkStream() is abstract - tested via concrete implementations above

    @Test
    public void testFlattMap() {
        // flattMap uses JDK IntStream
        IntStream result = createIntStream(1, 2, 3).flattMap(n -> java.util.stream.IntStream.of(n, n * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result.toArray());
    }

    @Test
    public void testTransformB() {
        int[] result = createIntStream(3, 1, 2).transformB(s -> s.sorted()).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTransformB_Deferred() {
        int[] result = createIntStream(3, 1, 2).transformB(s -> s.sorted(), true).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTransformB_NotDeferred() {
        int[] result = createIntStream(3, 1, 2).transformB(s -> s.map(x -> x * 2), false).toArray();
        assertArrayEquals(new int[] { 6, 2, 4 }, result);
    }

    @Test
    public void testTransformB_HappyPath2() {
        int[] result = IntStream.of(1, 2, 3).transformB(s -> s.map(x -> x * 2)).toArray();
        assertArrayEquals(new int[] { 2, 4, 6 }, result);
    }

    // transformB(Function, boolean) - with deferred=true path
    @Test
    public void testTransformB_WithDeferred_True() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.filter(x -> x % 2 == 0), true).toArray();
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

    @Test
    public void testTransformB_WithDeferred_False() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(x -> x * 3), false).toArray();
        assertArrayEquals(new int[] { 3, 6, 9, 12, 15 }, result);
    }

    @Test
    public void testEmpty_ReturnsEmptyStream() {
        stream = IntStream.empty();
        assertFalse(stream.iterator().hasNext());
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void testDistinct_EmptyStream() {
        stream = IntStream.empty().distinct();
        assertEquals(0, stream.count());
    }

    @Test
    public void testSorted_EmptyStream() {
        stream = IntStream.empty().sorted();
        assertEquals(0, stream.count());
    }

    @Test
    public void testReversed_EmptyStream() {
        stream = IntStream.empty().reversed();
        assertEquals(0, stream.count());
    }

    @Test
    public void testToArray_EmptyStream() {
        int[] result = IntStream.empty().toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testCount_EmptyStream() {
        long count = IntStream.empty().count();
        assertEquals(0, count);
    }

    @Test
    public void testSummarize_EmptyStream() {
        IntSummaryStatistics stats = IntStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testIterator_EmptyStream() {
        IntIterator iterator = IntStream.empty().iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testOnlyOne_EmptyStream() {
        OptionalInt result = IntStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testDebounce_EmptyStream() {
        stream = IntStream.empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1));
        int[] result = stream.toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_ParallelStreamEmpty() {
        int[] result = IntStream.empty().parallel().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void empty_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void toArray_onEmptyStream_shouldReturnEmptyArray() {
        assertEquals(0, IntStream.empty().toArray().length);
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, IntStream.empty().appendIfEmpty(1, 2, 3).count());
        assertEquals(2, IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.empty().appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), IntStream.empty().appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.toList(2, 3), IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).toList());

        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromEmpty() {
        assertEquals(0, IntStream.empty().count());
        assertEquals(0, IntStream.empty().skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.empty().toArray());
        assertArrayEquals(new int[] {}, IntStream.empty().skip(1).toArray());
        assertEquals(N.toList(), IntStream.empty().toList());
        assertEquals(N.toList(), IntStream.empty().skip(1).toList());
        assertEquals(0, IntStream.empty().map(e -> e).count());
        assertEquals(0, IntStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.empty().map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.empty().map(e -> e).skip(1).toArray());
        assertEquals(N.toList(), IntStream.empty().map(e -> e).toList());
        assertEquals(N.toList(), IntStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);

        createIntStream(1, 2, 3).ifEmpty(() -> called.set(true)).count();
        assertFalse(called.get());

        IntStream.empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());
    }

    @Test
    public void testEmptyStreamOperations() {
        IntStream empty = IntStream.empty();

        assertEquals(0, empty.sum());
        assertEquals(0, IntStream.empty().count());
        assertFalse(IntStream.empty().min().isPresent());
        assertFalse(IntStream.empty().max().isPresent());
        assertFalse(IntStream.empty().average().isPresent());
        assertFalse(IntStream.empty().first().isPresent());
        assertFalse(IntStream.empty().last().isPresent());
        assertTrue(IntStream.empty().allMatch(n -> false));
        assertFalse(IntStream.empty().anyMatch(n -> true));
        assertTrue(IntStream.empty().noneMatch(n -> true));
    }

    @Test
    public void testCount() {
        assertEquals(5, createIntStream(1, 2, 3, 4, 5).count());
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void testOnlyOne() {
        OptionalInt only = createIntStream(42).onlyOne();
        assertTrue(only.isPresent());
        assertEquals(42, only.getAsInt());

        assertFalse(IntStream.empty().onlyOne().isPresent());
    }

    @Test
    public void testReverseSorted_Empty() {
        int[] result = IntStream.empty().reverseSorted().toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testFirst_Empty() {
        OptionalInt result = IntStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_Empty() {
        OptionalInt result = IntStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testCount_Empty() {
        long result = IntStream.empty().count();
        assertEquals(0, result);
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        AtomicBoolean called = new AtomicBoolean(false);
        IntStream.empty().acceptIfNotEmpty(s -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        Optional<Integer> result = IntStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_EmptyStream2() {
        OptionalInt result = IntStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testReversed_Empty() {
        int[] result = IntStream.empty().reversed().toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testApplyIfNotEmpty() throws Exception {
        Optional<Integer> result = createIntStream(1, 2, 3, 4, 5).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(15), result.get());

        result = IntStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);

        createIntStream(1, 2, 3).acceptIfNotEmpty(s -> sum.set(s.sum()));
        assertEquals(6, sum.get());

        sum.set(0);
        IntStream.empty().acceptIfNotEmpty(s -> sum.set(s.sum())).orElse(() -> sum.set(-1));
        assertEquals(-1, sum.get());
    }

    @Test
    public void testThrowIfEmptyOnEmptyStream() {
        assertThrows(NoSuchElementException.class, () -> IntStream.empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmpty_EmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> IntStream.empty().throwIfEmpty().toArray());
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
    public void testStreamCreatedFromDefer() {
        assertEquals(5, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).count());
        assertEquals(4, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).toList());
        assertEquals(5, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).count());
        assertEquals(4, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).toList());
    }

    // defer(Supplier) - lazy stream creation
    @Test
    public void testDefer_LazyEvaluation() {
        AtomicInteger callCount = new AtomicInteger(0);
        // Supplier should not be called until terminal operation
        IntStream deferred = IntStream.defer(() -> {
            callCount.incrementAndGet();
            return IntStream.of(1, 2, 3);
        });
        assertEquals(0, callCount.get());
        int[] result = deferred.toArray();
        assertEquals(1, callCount.get());
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testDefer_EmptySupplier() {
        IntStream deferred = IntStream.defer(IntStream::empty);
        assertEquals(0, deferred.count());
    }

    @Test
    public void testFrom_JdkStream() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3);
        stream = IntStream.from(jdkStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testFromIterator() {
        IntIterator iter = new IntIteratorEx() {
            private int n = 0;

            @Override
            public boolean hasNext() {
                return n < 5;
            }

            @Override
            public int nextInt() {
                return n++;
            }
        };

        IntStream stream = createIntStream(iter);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testFromJavaStream() {
        java.util.stream.IntStream javaStream = java.util.stream.IntStream.of(1, 2, 3);
        stream = IntStream.from(javaStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testFrom() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3);
        IntStream result = IntStream.from(jdkStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testFrom_Empty() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.empty();
        IntStream result = IntStream.from(jdkStream);
        assertEquals(0, result.count());
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
    public void testStreamCreatedFromOfNullable() {
        assertEquals(1, IntStream.ofNullable(5).count());
        assertEquals(0, IntStream.ofNullable(5).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.ofNullable(5).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(5).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.ofNullable(5).toList());
        assertEquals(N.toList(), IntStream.ofNullable(5).skip(1).toList());
        assertEquals(1, IntStream.ofNullable(5).map(e -> e).count());
        assertEquals(0, IntStream.ofNullable(5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.ofNullable(5).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.ofNullable(5).map(e -> e).toList());
        assertEquals(N.toList(), IntStream.ofNullable(5).map(e -> e).skip(1).toList());

        assertEquals(0, IntStream.ofNullable(null).count());
        assertEquals(0, IntStream.ofNullable(null).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(null).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(null).skip(1).toArray());
        assertEquals(N.toList(), IntStream.ofNullable(null).toList());
        assertEquals(N.toList(), IntStream.ofNullable(null).skip(1).toList());
    }

    @Test
    public void testOfNullable() {
        stream = IntStream.ofNullable(null);
        assertEquals(0, stream.count());

        stream = IntStream.ofNullable(42);
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    // ofNullable(Integer) - both null and non-null paths
    @Test
    public void testOfNullable_NonNull() {
        int[] result = IntStream.ofNullable(Integer.valueOf(77)).toArray();
        assertArrayEquals(new int[] { 77 }, result);
    }

    @Test
    public void testOfNullable_NullInteger() {
        assertEquals(0, IntStream.ofNullable((Integer) null).count());
    }

    @Test
    public void testOf_WithArray() {
        stream = IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
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

    //    @Test
    //    public void testOf_WithJdkStream() {
    //        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3);
    //        stream = IntStream.of(jdkStream);
    //        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    //    }

    //

    @Test
    public void testOf_WithOptionalInt() {
        stream = IntStream.of(OptionalInt.of(42));
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testOf_WithJdkOptionalInt() {
        stream = IntStream.of(java.util.OptionalInt.of(42));
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testDistinct_RemovesDuplicates() {
        stream = IntStream.of(1, 2, 2, 3, 3, 3, 4).distinct();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
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
    public void testSkip_MoreThanAvailable() {
        stream = IntStream.of(1, 2, 3).skip(10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testToArray_BasicConversion() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testCount_BasicCount() {
        long count = IntStream.of(1, 2, 3, 4, 5).count();
        assertEquals(5, count);
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
    public void testParallelFilterMap() {
        stream = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8).parallel().filter(x -> x % 2 == 0).map(x -> x * 2);
        int[] result = stream.toArray();
        assertEquals(4, result.length);
    }

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        stream = IntStream.of(1, 2, 3, 4, 5).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1));
        int[] result = stream.toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        stream = IntStream.of(1, 2, 3, 4, 5).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1));
        int[] result = stream.toArray();

        // All elements should pass
        assertEquals(5, result.length);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        stream = IntStream.of(10, 20, 30, 40, 50).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1));
        int[] result = stream.toArray();

        assertArrayEquals(new int[] { 10, 20, 30 }, result);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        stream = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .filter(n -> n % 2 == 0) // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // 2, 4, 6
                .map(n -> n * 10); // 20, 40, 60
        int[] result = stream.toArray();

        assertEquals(3, result.length);
        assertArrayEquals(new int[] { 20, 40, 60 }, result);
    }

    @Test
    public void testDebounce_WithLongDuration() {
        // Test with a very long duration to ensure all elements within limit pass
        stream = IntStream.of(1, 2, 3, 4, 5).debounce(3, com.landawn.abacus.util.Duration.ofHours(1));
        int[] result = stream.toArray();

        assertEquals(3, result.length);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void of_array_shouldCreateStream() {
        int[] source = { 1, 2, 3 };
        assertArrayEquals(source, IntStream.of(source).toArray());
    }

    @Test
    public void distinct_shouldReturnStreamWithUniqueElements() {
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 2, 3, 1).distinct().toArray());
    }

    @Test
    public void sorted_shouldReturnSortedStream() {
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 5, 2).sorted().toArray());
    }

    @Test
    public void reversed_shouldReturnReversedStream() {
        assertArrayEquals(new int[] { 3, 2, 1 }, IntStream.of(1, 2, 3).reversed().toArray());
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        AtomicInteger droppedCount = new AtomicInteger();
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).count());
        droppedCount.set(0);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).count());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toArray());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toArray());
        droppedCount.set(0);
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toList());
        droppedCount.set(0);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).count());
        droppedCount.set(0);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).count());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toArray());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 4, 5 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toArray());
        droppedCount.set(0);
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).toList());
        assertEquals(N.toList(2, 3), IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).toList());
        assertEquals(N.toList(2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).toList());
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).toList());
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        AtomicInteger droppedCount = new AtomicInteger();
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).count());
        droppedCount.set(0);
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).count());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toArray());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toArray());
        droppedCount.set(0);
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toList());
        droppedCount.set(0);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).count());
        droppedCount.set(0);
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).count());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toArray());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 5 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toArray());
        droppedCount.set(0);
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).toList());
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).toList());
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(5, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().count());
        assertEquals(4, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().count());
        assertEquals(4, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).intersection(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).intersection(c).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).intersection(c).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).difference(c).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(1, 2, 3, 4, 5).difference(c).toArray());
        assertArrayEquals(new int[] { 2 }, IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).toArray());
        assertEquals(N.toList(1, 2), IntStream.of(1, 2, 3, 4, 5).difference(c).toList());
        assertEquals(N.toList(2), IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).toArray());
        assertArrayEquals(new int[] { 2 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).toArray());
        assertEquals(N.toList(1, 2), IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).toList());
        assertEquals(N.toList(2), IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 6 }, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).toArray());
        assertArrayEquals(new int[] { 2, 6 }, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).toArray());
        assertEquals(N.toList(1, 2, 6), IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).toList());
        assertEquals(N.toList(2, 6), IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 6 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).toArray());
        assertArrayEquals(new int[] { 2, 6 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).toArray());
        assertEquals(N.toList(1, 2, 6), IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).toList());
        assertEquals(N.toList(2, 6), IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).reversed().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.toList(4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.toList(4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).rotated(2).toArray());
        assertArrayEquals(new int[] { 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray());
        assertEquals(N.toList(4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.toList(5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new int[] { 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.toList(4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList(5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).sorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).sorted().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).sorted().toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).toList());
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).reverseSorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).reverseSorted().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).reverseSorted().toList());
        assertEquals(N.toList(4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toList());
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList(4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(15, IntStream.of(1, 2, 3, 4, 5).cycled().limit(15).count());
        assertEquals(14, IntStream.of(1, 2, 3, 4, 5).cycled().limit(15).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).toList());
        assertEquals(N.toList(2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).skip(1).toList());
        assertEquals(15, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).count());
        assertEquals(14, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).toList());
        assertEquals(N.toList(2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(10, IntStream.of(1, 2, 3, 4, 5).cycled(2).count());
        assertEquals(9, IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).cycled(2).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).cycled(2).toList());
        assertEquals(N.toList(2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toList());
        assertEquals(10, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).count());
        assertEquals(9, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).toList());
        assertEquals(N.toList(2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        Stream<IndexedInt> indexedStream = IntStream.of(1, 2, 3, 4, 5).indexed();
        assertEquals(5, indexedStream.count());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).indexed();
        assertEquals(4, indexedStream.skip(1).count());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).indexed();
        IndexedInt[] indexedArray = indexedStream.toArray(IndexedInt[]::new);
        assertEquals(5, indexedArray.length);
        assertEquals(0, indexedArray[0].index());
        assertEquals(1, indexedArray[0].value());
        assertEquals(4, indexedArray[4].index());
        assertEquals(5, indexedArray[4].value());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).indexed();
        List<IndexedInt> indexedList = indexedStream.toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals(1, indexedList.get(0).value());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).indexed();
        assertEquals(5, indexedStream.count());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).indexed();
        indexedList = indexedStream.toList();
        assertEquals(5, indexedList.size());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToChar() {
        CharStream charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(5, charStream.count());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(4, charStream.skip(1).count());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertArrayEquals(new char[] { 'A', 'B', 'C', 'D', 'E' }, charStream.toArray());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertArrayEquals(new char[] { 'B', 'C', 'D', 'E' }, charStream.skip(1).toArray());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(N.toList('A', 'B', 'C', 'D', 'E'), charStream.toList());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(N.toList('B', 'C', 'D', 'E'), charStream.skip(1).toList());

        charStream = IntStream.of(65, 66, 67, 68, 69).map(e -> e).mapToChar(i -> (char) i);
        assertEquals(5, charStream.count());

        charStream = IntStream.of(65, 66, 67, 68, 69).map(e -> e).mapToChar(i -> (char) i);
        assertEquals(N.toList('A', 'B', 'C', 'D', 'E'), charStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToByte() {
        ByteStream byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(5, byteStream.count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(4, byteStream.skip(1).count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, byteStream.toArray());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, byteStream.skip(1).toArray());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.toList());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.skip(1).toList());

        byteStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToByte(i -> (byte) i);
        assertEquals(5, byteStream.count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToByte(i -> (byte) i);
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToShort() {
        ShortStream shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(5, shortStream.count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(4, shortStream.skip(1).count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, shortStream.toArray());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, shortStream.skip(1).toArray());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), shortStream.toList());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5), shortStream.skip(1).toList());

        shortStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToShort(i -> (short) i);
        assertEquals(5, shortStream.count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToShort(i -> (short) i);
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), shortStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToLong() {
        LongStream longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(4, longStream.skip(1).count());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, longStream.toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, longStream.skip(1).toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), longStream.toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(N.toList(2L, 3L, 4L, 5L), longStream.skip(1).toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToLong(i -> (long) i);
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToLong(i -> (long) i);
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), longStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToFloat() {
        FloatStream floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(4, floatStream.skip(1).count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, floatStream.toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertArrayEquals(new float[] { 2f, 3f, 4f, 5f }, floatStream.skip(1).toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(N.toList(2f, 3f, 4f, 5f), floatStream.skip(1).toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(i -> (float) i);
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(i -> (float) i);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), floatStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToDouble() {
        DoubleStream doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(4, doubleStream.skip(1).count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, doubleStream.toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, doubleStream.skip(1).toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), doubleStream.skip(1).toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(i -> (double) i);
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(i -> (double) i);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(5, stringStream.count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(4, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertArrayEquals(new String[] { "Item1", "Item2", "Item3", "Item4", "Item5" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertArrayEquals(new String[] { "Item2", "Item3", "Item4", "Item5" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(N.toList("Item1", "Item2", "Item3", "Item4", "Item5"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(N.toList("Item2", "Item3", "Item4", "Item5"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(i -> "Item" + i);
        assertEquals(5, stringStream.count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(i -> "Item" + i);
        assertEquals(N.toList("Item1", "Item2", "Item3", "Item4", "Item5"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(9, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 }, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(9, IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).count());
        assertEquals(8, IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 }, IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattMap() {
        assertEquals(9, IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToChar() {
        CharStream charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(6, charStream.count());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(5, charStream.skip(1).count());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertArrayEquals(new char[] { 'A', 'a', 'B', 'b', 'C', 'c' }, charStream.toArray());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertArrayEquals(new char[] { 'a', 'B', 'b', 'C', 'c' }, charStream.skip(1).toArray());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.toList('A', 'a', 'B', 'b', 'C', 'c'), charStream.toList());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.toList('a', 'B', 'b', 'C', 'c'), charStream.skip(1).toList());

        charStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(6, charStream.count());

        charStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.toList('A', 'a', 'B', 'b', 'C', 'c'), charStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToByte() {
        ByteStream byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(6, byteStream.count());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(5, byteStream.skip(1).count());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, byteStream.toArray());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertArrayEquals(new byte[] { 10, 2, 20, 3, 30 }, byteStream.skip(1).toArray());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.toList((byte) 1, (byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.toList());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.toList((byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.skip(1).toList());

        byteStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(6, byteStream.count());

        byteStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.toList((byte) 1, (byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToShort() {
        ShortStream shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(6, shortStream.count());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(5, shortStream.skip(1).count());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertArrayEquals(new short[] { 1, 10, 2, 20, 3, 30 }, shortStream.toArray());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertArrayEquals(new short[] { 10, 2, 20, 3, 30 }, shortStream.skip(1).toArray());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.toList((short) 1, (short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.toList());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.toList((short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.skip(1).toList());

        shortStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(6, shortStream.count());

        shortStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.toList((short) 1, (short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToLong() {
        LongStream longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(6, longStream.count());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(5, longStream.skip(1).count());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, longStream.toArray());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L }, longStream.skip(1).toArray());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(N.toList(1L, 10L, 2L, 20L, 3L, 30L), longStream.toList());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(N.toList(10L, 2L, 20L, 3L, 30L), longStream.skip(1).toList());

        longStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(6, longStream.count());

        longStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(N.toList(1L, 10L, 2L, 20L, 3L, 30L), longStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToFloat() {
        FloatStream floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(6, floatStream.count());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(5, floatStream.skip(1).count());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f, 3f, 30f }, floatStream.toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertArrayEquals(new float[] { 10f, 2f, 20f, 3f, 30f }, floatStream.skip(1).toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f), floatStream.skip(1).toList());

        floatStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(6, floatStream.count());

        floatStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f), floatStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToDouble() {
        DoubleStream doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(6, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(5, doubleStream.skip(1).count());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, doubleStream.toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0 }, doubleStream.skip(1).toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(N.toList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(N.toList(10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.skip(1).toList());

        doubleStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(6, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(N.toList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapMulti() {
        assertEquals(6, IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).count());
        assertEquals(5, IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30 }, IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30), IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30), IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).toList());
        assertEquals(6, IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).count());
        assertEquals(5, IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30 }, IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30), IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30), IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).count());
        assertEquals(2, IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).count());
        assertArrayEquals(new int[] { 3, 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toArray());
        assertArrayEquals(new int[] { 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toArray());
        assertEquals(N.toList(3, 9, 16), IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toList());
        assertEquals(N.toList(9, 16), IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).count());
        assertEquals(2, IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).count());
        assertArrayEquals(new int[] { 3, 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toArray());
        assertArrayEquals(new int[] { 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toArray());
        assertEquals(N.toList(3, 9, 16),
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toList());
        assertEquals(N.toList(9, 16),
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(3, stringStream.count());

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(2, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "1-2", "4-5", "8-8" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "4-5", "8-8" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(N.toList("1-2", "4-5", "8-8"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(N.toList("4-5", "8-8"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(3, stringStream.count());

        stringStream = IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(N.toList("1-2", "4-5", "8-8"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterCollapse() {
        Stream<IntList> listStream = IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1);
        assertEquals(3, listStream.count());

        listStream = IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1);
        assertEquals(2, listStream.skip(1).count());

        listStream = IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1);
        List<IntList> result = listStream.toList();
        assertEquals(3, result.size());
        assertEquals(IntList.of(1, 2, 3), result.get(0));
        assertEquals(IntList.of(5, 6), result.get(1));
        assertEquals(IntList.of(8), result.get(2));

        listStream = IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1);
        assertEquals(3, listStream.count());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(6, 11, 8),
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 8),
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(1, 3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(1, 3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(10, 11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(10, 11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).count());
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).toArray());
        assertArrayEquals(new int[] { -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).toArray());
        assertEquals(N.toList(-2, -1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).toList());
        assertEquals(N.toList(-1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).toList());
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).count());
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).toArray());
        assertArrayEquals(new int[] { -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).toArray());
        assertEquals(N.toList(-2, -1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).toList());
        assertEquals(N.toList(-1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toList());
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).top(3).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).top(3).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(3, 1, 4, 2, 5).top(3).toList());
        assertEquals(N.toList(4, 5), IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).toList());
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).toList());
        assertEquals(N.toList(4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).count());
        assertArrayEquals(new int[] { 3, 1, 2 }, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).toArray());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).toArray());
        assertEquals(N.toList(3, 1, 2), IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).toList());
        assertEquals(N.toList(1, 2), IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).toList());
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).count());
        assertArrayEquals(new int[] { 3, 1, 2 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).toArray());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).toArray());
        assertEquals(N.toList(3, 1, 2), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).toList());
        assertEquals(N.toList(1, 2), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        IntStream streamB = IntStream.of(2, 4, 6);
        assertEquals(6, IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(5, IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());

        streamB = IntStream.of(2, 4, 6);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 },
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());

        streamB = IntStream.of(2, 4, 6);
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 },
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(N.toList(2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(6, IntStream.of(1, 3, 5).map(e -> e).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).map(e -> e).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith() {
        IntStream streamB = IntStream.of(10, 20, 30);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).skip(1).count());

        streamB = IntStream.of(10, 20, 30);
        assertArrayEquals(new int[] { 11, 22, 33 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).toArray());

        streamB = IntStream.of(10, 20, 30);
        assertArrayEquals(new int[] { 22, 33 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).skip(1).toArray());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(N.toList(11, 22, 33), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).toList());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(N.toList(22, 33), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).skip(1).toList());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(N.toList(11, 22, 33), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, (a, b) -> a + b).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith3Streams() {
        IntStream streamB = IntStream.of(10, 20, 30);
        IntStream streamC = IntStream.of(100, 200, 300);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).skip(1).count());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertArrayEquals(new int[] { 111, 222, 333 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).toArray());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertArrayEquals(new int[] { 222, 333 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).skip(1).toArray());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(N.toList(111, 222, 333), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(N.toList(222, 333), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).skip(1).toList());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(N.toList(111, 222, 333), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        IntStream streamB = IntStream.of(10, 20);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).skip(1).count());

        streamB = IntStream.of(10, 20);
        assertArrayEquals(new int[] { 11, 22, 103, 104, 105 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).toArray());

        streamB = IntStream.of(10, 20);
        assertArrayEquals(new int[] { 22, 103, 104, 105 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).skip(1).toArray());

        streamB = IntStream.of(10, 20);
        assertEquals(N.toList(11, 22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).toList());

        streamB = IntStream.of(10, 20);
        assertEquals(N.toList(22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).skip(1).toList());

        streamB = IntStream.of(10, 20);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, 0, 100, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20);
        assertEquals(N.toList(11, 22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, 0, 100, (a, b) -> a + b).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith3StreamsDefaults() {
        IntStream streamB = IntStream.of(10, 20);
        IntStream streamC = IntStream.of(100);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).skip(1).count());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertArrayEquals(new int[] { 111, 522, 553, 554, 555 },
                IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).toArray());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertArrayEquals(new int[] { 522, 553, 554, 555 },
                IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).skip(1).toArray());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(N.toList(111, 522, 553, 554, 555), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).toList());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(N.toList(522, 553, 554, 555), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).skip(1).toList());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(N.toList(111, 522, 553, 554, 555),
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).toList());
    }

    @Test
    public void testStreamCreatedAfterAsLongStream() {
        LongStream longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(4, longStream.skip(1).count());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, longStream.toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, longStream.skip(1).toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), longStream.toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(N.toList(2L, 3L, 4L, 5L), longStream.skip(1).toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asLongStream();
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asLongStream();
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), longStream.toList());
    }

    @Test
    public void testStreamCreatedAfterAsFloatStream() {
        FloatStream floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(4, floatStream.skip(1).count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, floatStream.toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertArrayEquals(new float[] { 2f, 3f, 4f, 5f }, floatStream.skip(1).toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(N.toList(2f, 3f, 4f, 5f), floatStream.skip(1).toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asFloatStream();
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asFloatStream();
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), floatStream.toList());
    }

    @Test
    public void testStreamCreatedAfterAsDoubleStream() {
        DoubleStream doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(4, doubleStream.skip(1).count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, doubleStream.toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, doubleStream.skip(1).toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), doubleStream.skip(1).toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream();
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream();
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        Stream<Integer> boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertEquals(5, boxedStream.count());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertEquals(4, boxedStream.skip(1).count());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, boxedStream.toArray(Integer[]::new));

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, boxedStream.skip(1).toArray(Integer[]::new));

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), boxedStream.toList());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertEquals(Arrays.asList(2, 3, 4, 5), boxedStream.skip(1).toList());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).boxed();
        assertEquals(5, boxedStream.count());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).boxed();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), boxedStream.toList());
    }

    @Test
    public void testStreamCreatedAfterToJdkStream() {
        java.util.stream.IntStream jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertEquals(5, jdkStream.count());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertEquals(4, jdkStream.skip(1).count());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, jdkStream.toArray());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, jdkStream.skip(1).toArray());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), jdkStream.boxed().collect(java.util.stream.Collectors.toList()));

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertEquals(Arrays.asList(2, 3, 4, 5), jdkStream.skip(1).boxed().collect(java.util.stream.Collectors.toList()));

        jdkStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream();
        assertEquals(5, jdkStream.count());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), jdkStream.boxed().collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testStreamCreatedAfterTransformB() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTransformBDeferred() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromJdkStream() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.from(jdkStream).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.from(jdkStream).skip(1).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.from(jdkStream).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.from(jdkStream).skip(1).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.from(jdkStream).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.from(jdkStream).skip(1).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.from(jdkStream).map(e -> e).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.from(jdkStream).map(e -> e).skip(1).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.from(jdkStream).map(e -> e).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.from(jdkStream).map(e -> e).skip(1).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.from(jdkStream).map(e -> e).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.from(jdkStream).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfArray() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfArrayWithRange() {
        int[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        assertEquals(5, IntStream.of(array, 2, 7).count());
        assertEquals(4, IntStream.of(array, 2, 7).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).toList());
        assertEquals(N.toList(4, 5, 6, 7), IntStream.of(array, 2, 7).skip(1).toList());
        assertEquals(5, IntStream.of(array, 2, 7).map(e -> e).count());
        assertEquals(4, IntStream.of(array, 2, 7).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).toList());
        assertEquals(N.toList(4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntegerArray() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        assertEquals(5, IntStream.of(array).count());
        assertEquals(4, IntStream.of(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(array).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(array).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(array).skip(1).toList());
        assertEquals(5, IntStream.of(array).map(e -> e).count());
        assertEquals(4, IntStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(array).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntegerArrayWithRange() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        assertEquals(5, IntStream.of(array, 2, 7).count());
        assertEquals(4, IntStream.of(array, 2, 7).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).toList());
        assertEquals(N.toList(4, 5, 6, 7), IntStream.of(array, 2, 7).skip(1).toList());
        assertEquals(5, IntStream.of(array, 2, 7).map(e -> e).count());
        assertEquals(4, IntStream.of(array, 2, 7).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).toList());
        assertEquals(N.toList(4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfCollection() {
        Collection<Integer> collection = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.of(collection).count());
        assertEquals(4, IntStream.of(collection).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(collection).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(collection).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(collection).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(collection).skip(1).toList());
        assertEquals(5, IntStream.of(collection).map(e -> e).count());
        assertEquals(4, IntStream.of(collection).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(collection).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(collection).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(collection).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(collection).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntIterator() {
        IntIterator iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.of(iterator).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.of(iterator).skip(1).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(iterator).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(iterator).skip(1).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(iterator).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(iterator).skip(1).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.of(iterator).map(e -> e).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.of(iterator).map(e -> e).skip(1).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(iterator).map(e -> e).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(iterator).map(e -> e).skip(1).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(iterator).map(e -> e).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(iterator).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntBuffer() {
        IntBuffer buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(5, IntStream.of(buffer).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(4, IntStream.of(buffer).skip(1).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(buffer).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(buffer).skip(1).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(buffer).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(buffer).skip(1).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(5, IntStream.of(buffer).map(e -> e).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(4, IntStream.of(buffer).map(e -> e).skip(1).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(buffer).map(e -> e).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(buffer).map(e -> e).skip(1).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(buffer).map(e -> e).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(buffer).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromConcatStreams() {
        IntStream stream1 = IntStream.of(1, 2);
        IntStream stream2 = IntStream.of(3, 4);
        IntStream stream3 = IntStream.of(5);
        assertEquals(5, IntStream.concat(stream1, stream2, stream3).count());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(4, IntStream.concat(stream1, stream2, stream3).skip(1).count());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(stream1, stream2, stream3).toArray());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(stream1, stream2, stream3).skip(1).toArray());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).toList());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).skip(1).toList());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(5, IntStream.concat(stream1, stream2, stream3).map(e -> e).count());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(4, IntStream.concat(stream1, stream2, stream3).map(e -> e).skip(1).count());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(stream1, stream2, stream3).map(e -> e).toArray());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(stream1, stream2, stream3).map(e -> e).skip(1).toArray());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).map(e -> e).toList());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOf() {
        stream = createIntStream(5);
        assertArrayEquals(new int[] { 5 }, stream.toArray());

        stream = createIntStream(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());

        stream = createIntStream(new int[] {});
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfWithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        stream = createIntStream(arr, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testElementAt() {
        OptionalInt element = createIntStream(10, 20, 30, 40, 50).elementAt(2);
        assertTrue(element.isPresent());
        assertEquals(30, element.getAsInt());

        assertFalse(IntStream.of(1, 2, 3).elementAt(10).isPresent());
    }

    @Test
    public void testOfIntBuffer() {
        IntBuffer buffer = IntBuffer.wrap(new int[] { 1, 2, 3 });
        IntStream result = IntStream.of(buffer);
        assertArrayEquals(new int[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testLimit_WithOffset() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).limit(1, 3).toArray();
        assertArrayEquals(new int[] { 2, 3, 4 }, result);
    }

    @Test
    public void testLimit_WithOffsetBeyondSize() {
        int[] result = IntStream.of(1, 2, 3).limit(5, 3).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testSkipUntil() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).skipUntil(x -> x >= 3).toArray();
        assertArrayEquals(new int[] { 3, 4, 5 }, result);
    }

    @Test
    public void testSkipUntil_NeverMatches() {
        int[] result = IntStream.of(1, 2, 3).skipUntil(x -> x > 10).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testStep_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5, 6).step(2).toArray();
        assertArrayEquals(new int[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStep_StepThree() {
        int[] result = IntStream.of(1, 2, 3, 4, 5, 6, 7).step(3).toArray();
        assertArrayEquals(new int[] { 1, 4, 7 }, result);
    }

    @Test
    public void testCycled_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).cycled().limit(7).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3, 1 }, result);
    }

    @Test
    public void testCycled_WithRounds() {
        int[] result = IntStream.of(1, 2).cycled(3).toArray();
        assertArrayEquals(new int[] { 1, 2, 1, 2, 1, 2 }, result);
    }

    @Test
    public void testIntersection_HappyPath() {
        Set<Integer> other = new HashSet<>(Arrays.asList(2, 4, 6));
        int[] result = IntStream.of(1, 2, 3, 4, 5).intersection(other).toArray();
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

    @Test
    public void testDifference_HappyPath() {
        Set<Integer> other = new HashSet<>(Arrays.asList(2, 4));
        int[] result = IntStream.of(1, 2, 3, 4, 5).difference(other).toArray();
        assertArrayEquals(new int[] { 1, 3, 5 }, result);
    }

    @Test
    public void testSymmetricDifference_HappyPath() {
        List<Integer> other = Arrays.asList(2, 4, 6);
        int[] result = IntStream.of(1, 2, 3, 4, 5).symmetricDifference(other).sorted().toArray();
        assertArrayEquals(new int[] { 1, 3, 5, 6 }, result);
    }

    @Test
    public void testIndexed_HappyPath() {
        List<IndexedInt> result = IntStream.of(10, 20, 30).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10, result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals(20, result.get(1).value());
        assertEquals(2, result.get(2).index());
        assertEquals(30, result.get(2).value());
    }

    @Test
    public void testFirst_HappyPath() {
        OptionalInt result = IntStream.of(5, 3, 1).first();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testLast_HappyPath() {
        OptionalInt result = IntStream.of(5, 3, 1).last();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testJoin_WithDelimiter() {
        String result = IntStream.of(1, 2, 3).join(", ");
        assertEquals("1, 2, 3", result);
    }

    @Test
    public void testJoin_WithDelimiterAndPrefixSuffix() {
        String result = IntStream.of(1, 2, 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testToList_HappyPath() {
        List<Integer> result = IntStream.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testToSet_HappyPath() {
        Set<Integer> result = IntStream.of(1, 2, 2, 3, 3).toSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testToMultiset_HappyPath() {
        Multiset<Integer> result = IntStream.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, result.get(1));
        assertEquals(2, result.get(2));
        assertEquals(3, result.get(3));
    }

    @Test
    public void testToImmutableList_HappyPath() {
        com.landawn.abacus.util.ImmutableList<Integer> result = IntStream.of(1, 2, 3).toImmutableList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
    }

    @Test
    public void testToImmutableSet_HappyPath() {
        com.landawn.abacus.util.ImmutableSet<Integer> result = IntStream.of(1, 2, 2, 3).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
    }

    @Test
    public void testCount_HappyPath() {
        long result = IntStream.of(1, 2, 3, 4, 5).count();
        assertEquals(5, result);
    }

    @Test
    public void testToArray_HappyPath() {
        int[] result = IntStream.of(3, 1, 2).toArray();
        assertArrayEquals(new int[] { 3, 1, 2 }, result);
    }

    @Test
    public void testSkip_WithAction() {
        List<Integer> skipped = new ArrayList<>();
        int[] result = IntStream.of(1, 2, 3, 4, 5).skip(2, value -> skipped.add(value)).toArray();
        assertArrayEquals(new int[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testPsp_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).psp(s -> s.filter(x -> x % 2 != 0)).toArray();
        Arrays.sort(result);
        assertArrayEquals(new int[] { 1, 3, 5 }, result);
    }

    @Test
    public void testSps_HappyPath() {
        int sum = IntStream.of(1, 2, 3, 4, 5).sps(s -> s.map(x -> x * 2)).sum();
        assertEquals(30, sum);
    }

    @Test
    public void testTransform_HappyPath() {
        int sum = IntStream.of(1, 2, 3).transform(s -> s.map(x -> x * 2)).sum();
        assertEquals(12, sum);
    }

    @Test
    public void testOnClose_HappyPath() {
        AtomicBoolean closed = new AtomicBoolean(false);
        IntStream s = IntStream.of(1, 2, 3).onClose(() -> closed.set(true));
        s.toArray();
        s.close();
        assertTrue(closed.get());
    }

    @Test
    public void testElementAt_HappyPath() {
        OptionalInt result = IntStream.of(10, 20, 30, 40).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
    }

    @Test
    public void testElementAt_BeyondSize() {
        OptionalInt result = IntStream.of(10, 20).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_HappyPath() {
        OptionalInt result = IntStream.of(42).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testPeek_HappyPath() {
        List<Integer> peeked = new ArrayList<>();
        int[] result = IntStream.of(1, 2, 3).peek(peeked::add).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
        assertEquals(Arrays.asList(1, 2, 3), peeked);
    }

    @Test
    public void testShuffled_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testShuffled_WithRandom() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testRotated_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).rotated(2).toArray();
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, result);
    }

    @Test
    public void testReversed_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).reversed().toArray();
        assertArrayEquals(new int[] { 3, 2, 1 }, result);
    }

    @Test
    public void testParallel_WithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            int sum = IntStream.of(1, 2, 3, 4, 5).parallel(executor).sum();
            assertEquals(15, sum);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSequential_AfterParallel() {
        int sum = IntStream.of(1, 2, 3).parallel().sequential().sum();
        assertEquals(6, sum);
    }

    @Test
    public void testIsParallel_HappyPath() {
        assertFalse(IntStream.of(1, 2, 3).isParallel());
        assertTrue(IntStream.of(1, 2, 3).parallel().isParallel());
    }

    @Test
    public void testToCollection_Supplier() {
        java.util.LinkedList<Integer> result = IntStream.of(1, 2, 3).toCollection(java.util.LinkedList::new);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(3), result.get(2));
    }

    @Test
    public void testToMultiset_Supplier() {
        Multiset<Integer> result = IntStream.of(1, 2, 2, 3, 3, 3).toMultiset(Multiset::new);
        assertEquals(1, result.get(1));
        assertEquals(2, result.get(2));
        assertEquals(3, result.get(3));
    }

    // of(int[], int, int) - array range factory
    @Test
    public void testOf_ArrayWithFromTo_HappyPath() {
        int[] src = { 10, 20, 30, 40, 50 };
        int[] result = IntStream.of(src, 1, 4).toArray();
        assertArrayEquals(new int[] { 20, 30, 40 }, result);
    }

    @Test
    public void testOf_ArrayWithFromTo_FullRange() {
        int[] src = { 1, 2, 3 };
        int[] result = IntStream.of(src, 0, 3).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    // of(u.OptionalInt) - Landawn's OptionalInt
    @Test
    public void testOf_LandawnOptionalInt_Present() {
        int[] result = IntStream.of(OptionalInt.of(99)).toArray();
        assertArrayEquals(new int[] { 99 }, result);
    }

    // of(java.util.OptionalInt) - JDK OptionalInt
    @Test
    public void testOf_JdkOptionalInt_Present() {
        int[] result = IntStream.of(java.util.OptionalInt.of(55)).toArray();
        assertArrayEquals(new int[] { 55 }, result);
    }

    @Test
    public void testOf_WithEmptyArray() {
        stream = IntStream.of(new int[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOf_WithEmptyOptionalInt() {
        stream = IntStream.of(OptionalInt.empty());
        assertEquals(0, stream.count());
    }

    @Test
    public void testReversed_SingleElement() {
        stream = IntStream.of(42).reversed();
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testLimit_ZeroElements() {
        stream = IntStream.of(1, 2, 3).limit(0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testSkip_ZeroElements() {
        stream = IntStream.of(1, 2, 3).skip(0);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testSummarize_BasicSummary() {
        IntSummaryStatistics stats = IntStream.of(1, 2, 3, 4, 5).summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
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
    public void testOnClose_MultipleHandlers() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        stream = IntStream.of(1, 2, 3).onClose(() -> counter1.incrementAndGet()).onClose(() -> counter2.incrementAndGet());
        stream.count();
        stream.close();
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    public void testOnlyOne_SingleElement() {
        OptionalInt result = IntStream.of(42).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testDebounce_SingleElement() {
        stream = IntStream.of(42).debounce(1, com.landawn.abacus.util.Duration.ofMillis(100));
        int[] result = stream.toArray();

        assertEquals(1, result.length);
        assertEquals(42, result[0]);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        stream = IntStream.of(1, 2, 3, 4, 5).debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1));
        int[] result = stream.toArray();

        assertEquals(1, result.length);
        assertEquals(1, result[0]);
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        int[] input = new int[1000];
        for (int i = 0; i < 1000; i++) {
            input[i] = i;
        }

        stream = IntStream.of(input).debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10));
        int[] result = stream.toArray();

        assertEquals(500, result.length);
    }

    @Test
    public void testDebounce_ParallelStream() {
        // Test debounce on a parallel stream
        int[] input = new int[100];
        for (int i = 0; i < 100; i++) {
            input[i] = i;
        }

        int[] result = IntStream.of(input).parallel().debounce(10, com.landawn.abacus.util.Duration.ofSeconds(10)).toArray();

        // Should limit to maxWindowSize elements
        assertEquals(10, result.length);
    }

    @Test
    public void of_emptyArray_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.of(new int[0]).count());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        int[] shuffledArray = IntStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, shuffledArray.length);
        assertHaveSameElements(N.toList(shuffledArray), N.toList(1, 2, 3, 4, 5));

        List<Integer> shuffledList = IntStream.of(1, 2, 3, 4, 5).shuffled().toList();
        assertEquals(5, shuffledList.size());
        assertHaveSameElements(shuffledList, N.toList(1, 2, 3, 4, 5));

        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        int[] shuffledArray = IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).toArray();
        assertEquals(5, shuffledArray.length);
        assertHaveSameElements(N.toList(shuffledArray), N.toList(1, 2, 3, 4, 5));

        rnd = new Random(42);
        List<Integer> shuffledList = IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        assertHaveSameElements(shuffledList, N.toList(1, 2, 3, 4, 5));

        rnd = new Random(42);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toArray());
        assertEquals(N.toList(10, 30, 50), IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toList());
        assertEquals(N.toList(30, 50), IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toArray());
        assertEquals(N.toList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toList());
        assertEquals(N.toList(30, 50),
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartialJdk() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).count());
        assertEquals(2,
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).skip(1).count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).toList());
        assertEquals(N.toList(30, 50),
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).skip(1).toList());
        assertEquals(3,
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .count());
        assertEquals(2,
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .toList());
        assertEquals(N.toList(30, 50),
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedFromOfOptionalInt() {
        OptionalInt op = OptionalInt.of(5);
        assertEquals(1, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.of(op).toList());
        assertEquals(N.toList(), IntStream.of(op).skip(1).toList());
        assertEquals(1, IntStream.of(op).map(e -> e).count());
        assertEquals(0, IntStream.of(op).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.of(op).map(e -> e).toList());
        assertEquals(N.toList(), IntStream.of(op).map(e -> e).skip(1).toList());

        op = OptionalInt.empty();
        assertEquals(0, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.toList(), IntStream.of(op).toList());
        assertEquals(N.toList(), IntStream.of(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfJavaOptionalInt() {
        java.util.OptionalInt op = java.util.OptionalInt.of(5);
        assertEquals(1, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.of(op).toList());
        assertEquals(N.toList(), IntStream.of(op).skip(1).toList());
        assertEquals(1, IntStream.of(op).map(e -> e).count());
        assertEquals(0, IntStream.of(op).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.of(op).map(e -> e).toList());
        assertEquals(N.toList(), IntStream.of(op).map(e -> e).skip(1).toList());

        op = java.util.OptionalInt.empty();
        assertEquals(0, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.toList(), IntStream.of(op).toList());
        assertEquals(N.toList(), IntStream.of(op).skip(1).toList());
    }

    @Test
    public void testOfIntBuffer_Empty() {
        IntBuffer buffer = IntBuffer.wrap(new int[] {});
        IntStream result = IntStream.of(buffer);
        assertEquals(0, result.count());
    }

    @Test
    public void testPercentiles_HappyPath() {
        Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result = IntStream.of(1, 2, 3, 4, 5).summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertEquals(1, result.left().getMin());
        assertEquals(5, result.left().getMax());
    }

    @Test
    public void testAcceptIfNotEmpty_HappyPath() {
        AtomicInteger sum = new AtomicInteger(0);
        IntStream.of(1, 2, 3).acceptIfNotEmpty(s -> sum.set(s.sum()));
        assertEquals(6, sum.get());
    }

    @Test
    public void testApplyIfNotEmpty_HappyPath() {
        Optional<Integer> result = IntStream.of(1, 2, 3).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(6, result.get().intValue());
    }

    @Test
    public void testRotated_NegativeDistance() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).rotated(-2).toArray();
        assertArrayEquals(new int[] { 3, 4, 5, 1, 2 }, result);
    }

    @Test
    public void testOf_ArrayWithFromTo_EmptyRange() {
        int[] src = { 1, 2, 3 };
        assertEquals(0, IntStream.of(src, 2, 2).count());
    }

    @Test
    public void testOf_LandawnOptionalInt_Empty() {
        assertEquals(0, IntStream.of(OptionalInt.empty()).count());
    }

    @Test
    public void testOf_LandawnOptionalInt_Null() {
        assertEquals(0, IntStream.of((OptionalInt) null).count());
    }

    @Test
    public void testOf_JdkOptionalInt_Empty() {
        assertEquals(0, IntStream.of(java.util.OptionalInt.empty()).count());
    }

    @Test
    public void testOf_JdkOptionalInt_Null() {
        assertEquals(0, IntStream.of((java.util.OptionalInt) null).count());
    }

    @Test
    public void testDebounce_WindowResetAfterDuration() throws InterruptedException {
        // Use a short duration to test window reset
        AtomicInteger count = new AtomicInteger(0);
        IntList elements = new IntList();

        // Generate elements with delays to span multiple windows
        IntStream.of(1, 2, 3, 4, 5, 6).peek(e -> {
            if (count.incrementAndGet() == 3) {
                // Sleep after 3rd element to allow window to reset
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }).debounce(2, com.landawn.abacus.util.Duration.ofMillis(100)).forEach(elements::add);

        // First window: 1, 2 pass
        // After sleep, window resets
        assertTrue(elements.size() >= 2);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IntStream.of(1, 2, 3).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IntStream.of(1, 2, 3).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IntStream.of(1, 2, 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toArray();
        });

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IntStream.of(1, 2, 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toArray();
        });
    }

    @Test
    public void of_singleElement_shouldCreateStream() {
        assertEquals(1, IntStream.of(1).count());
        assertEquals(1, IntStream.of(1).first().orElseThrow());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        int[] result = IntStream.of(1, 2, 3).throwIfEmpty().toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testOnlyOne_TooManyElements() {
        assertThrows(TooManyElementsException.class, () -> IntStream.of(1, 2).onlyOne());
    }

    @Test
    public void testStreamCreatedFromOfCodePoints() {
        String str = "Hello";
        assertEquals(5, IntStream.ofCodePoints(str).count());
        assertEquals(4, IntStream.ofCodePoints(str).skip(1).count());
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, IntStream.ofCodePoints(str).toArray());
        assertArrayEquals(new int[] { 101, 108, 108, 111 }, IntStream.ofCodePoints(str).skip(1).toArray());
        assertEquals(N.toList(72, 101, 108, 108, 111), IntStream.ofCodePoints(str).toList());
        assertEquals(N.toList(101, 108, 108, 111), IntStream.ofCodePoints(str).skip(1).toList());
        assertEquals(5, IntStream.ofCodePoints(str).map(e -> e).count());
        assertEquals(4, IntStream.ofCodePoints(str).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, IntStream.ofCodePoints(str).map(e -> e).toArray());
        assertArrayEquals(new int[] { 101, 108, 108, 111 }, IntStream.ofCodePoints(str).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(72, 101, 108, 108, 111), IntStream.ofCodePoints(str).map(e -> e).toList());
        assertEquals(N.toList(101, 108, 108, 111), IntStream.ofCodePoints(str).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfCodePoints() {
        IntStream stream = IntStream.ofCodePoints("Hello");
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, stream.toArray());
    }

    @Test
    public void testOfCodePoints_HappyPath() {
        int[] result = IntStream.ofCodePoints("ABC").toArray();
        assertArrayEquals(new int[] { 65, 66, 67 }, result);
    }

    @Test
    public void testOfCodePoints_EmptyString() {
        int[] result = IntStream.ofCodePoints("").toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testOfCodePoints_Null() {
        int[] result = IntStream.ofCodePoints(null).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testSplitByChunkCount_BasicSplit() {
        stream = IntStream.splitByChunkCount(10, 3, (from, to) -> from);
        assertEquals(3, stream.count());
    }

    @Test
    public void testStreamCreatedFromSplitByChunkCount() {
        assertEquals(3, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).count());
        assertEquals(2, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).count());
        assertArrayEquals(new int[] { 4, 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).toArray());
        assertArrayEquals(new int[] { 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).toArray());
        assertEquals(N.toList(4, 3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).toList());
        assertEquals(N.toList(3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).toList());
        assertEquals(3, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).count());
        assertEquals(2, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 4, 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(4, 3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).toList());
        assertEquals(N.toList(3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromSplitByChunkCountWithSizeSmallerFirst() {
        assertEquals(5, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).count());
        assertEquals(4, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).count());
        assertArrayEquals(new int[] { 1, 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).toArray());
        assertArrayEquals(new int[] { 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).toArray());
        assertEquals(N.toList(1, 1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).toList());
        assertEquals(N.toList(1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).toList());
        assertEquals(5, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).count());
        assertEquals(4, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).toList());
        assertEquals(N.toList(1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).toList());
    }

    @Test
    public void testSplitByChunkCount() {
        List<int[]> chunks = Stream.splitByChunkCount(10, 3, (from, to) -> {
            int[] chunk = new int[to - from];
            for (int i = from; i < to; i++) {
                chunk[i - from] = i;
            }
            return chunk;
        }).toList();

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, chunks.get(0));
        assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1));
        assertArrayEquals(new int[] { 7, 8, 9 }, chunks.get(2));
    }

    @Test
    public void testSplitByChunkCount_HappyPath2() {
        int[] result = IntStream.splitByChunkCount(7, 3, (from, to) -> to - from).toArray();
        assertEquals(3, result.length);
        assertEquals(7, IntStream.of(result).sum());
    }

    @Test
    public void testSplitByChunkCount_SizeSmallerFirst() {
        int[] result = IntStream.splitByChunkCount(7, 3, true, (from, to) -> to - from).toArray();
        assertEquals(3, result.length);
        assertEquals(7, IntStream.of(result).sum());
        // Smaller first: first chunk should be smaller or equal
        assertTrue(result[0] <= result[result.length - 1]);
    }

    @Test
    public void test_splitByChunkCount() {
        {
            final int[] a = Array.rangeClosed(1, 7);
            int[] result = IntStream.splitByChunkCount(7, 5, false, (fromIndex, toIndex) -> fromIndex + toIndex).toArray();
            N.println(result);
            assertArrayEquals(new int[] { 2, 6, 9, 11, 13 }, result);
        }
        {
            final int[] a = Array.rangeClosed(1, 7);
            int[] result = IntStream.splitByChunkCount(7, 5, true, (fromIndex, toIndex) -> fromIndex + toIndex).toArray();
            N.println(result);
            assertArrayEquals(new int[] { 1, 3, 5, 8, 12 }, result);
        }
    }

    @Test
    public void testSplitByChunkCount_ZeroTotal() {
        int[] result = IntStream.splitByChunkCount(0, 3, (from, to) -> to - from).toArray();
        assertArrayEquals(new int[0], result);
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
    public void testFlattenHorizontalWithPadding() {
        int[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        stream = IntStream.flatten(array, 0, false);
        int[] result = stream.toArray();
        assertArrayEquals(new int[] { 1, 2, 0, 3, 0, 0, 4, 5, 6 }, result);
    }

    @Test
    public void testStreamCreatedFromFlatten() {
        int[][] array = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, IntStream.flatten(array).count());
        assertEquals(4, IntStream.flatten(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.flatten(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.flatten(array).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.flatten(array).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.flatten(array).skip(1).toList());
        assertEquals(5, IntStream.flatten(array).map(e -> e).count());
        assertEquals(4, IntStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlattenVertically() {
        int[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        assertEquals(9, IntStream.flatten(array, true).count());
        assertEquals(8, IntStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).toArray());
        assertArrayEquals(new int[] { 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.toList(1, 4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).toList());
        assertEquals(N.toList(4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).skip(1).toList());
        assertEquals(9, IntStream.flatten(array, true).map(e -> e).count());
        assertEquals(8, IntStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.toList(4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlattenWithAlignment() {
        int[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertEquals(9, IntStream.flatten(array, 0, true).count());
        assertEquals(8, IntStream.flatten(array, 0, true).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).toArray());
        assertArrayEquals(new int[] { 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).skip(1).toArray());
        assertEquals(N.toList(1, 3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).toList());
        assertEquals(N.toList(3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).skip(1).toList());
        assertEquals(9, IntStream.flatten(array, 0, true).map(e -> e).count());
        assertEquals(8, IntStream.flatten(array, 0, true).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).map(e -> e).toList());
        assertEquals(N.toList(3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlatten3D() {
        int[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        assertEquals(8, IntStream.flatten(array).count());
        assertEquals(7, IntStream.flatten(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).skip(1).toList());
        assertEquals(8, IntStream.flatten(array).map(e -> e).count());
        assertEquals(7, IntStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFlatten() {
        int[][] arrays = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        IntStream stream = IntStream.flatten(arrays);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFlattenVertically() {
        int[][] arrays = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        IntStream stream = IntStream.flatten(arrays, true);
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, stream.toArray());
    }

    @Test
    public void testFlattenWithPadding() {
        int[][] arrays = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        IntStream stream = IntStream.flatten(arrays, 0, false);
        assertArrayEquals(new int[] { 1, 2, 0, 3, 4, 5, 6, 0, 0 }, stream.toArray());
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_2() {
        int[][] array = { { 1, 2 }, { 3, 4 } };
        int[] result = IntStream.flatten(array, false).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);

        result = IntStream.flatten(array, true).toArray();
        assertArrayEquals(new int[] { 1, 3, 2, 4 }, result);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_3() {
        int[][] array = { { 1, 2 }, { 3, 4, 5 } };
        int[] result = IntStream.flatten(array, 0, false).toArray();
        assertArrayEquals(new int[] { 1, 2, 0, 3, 4, 5 }, result);

        result = IntStream.flatten(array, 0, true).toArray();
        assertArrayEquals(new int[] { 1, 3, 2, 4, 0, 5 }, result);
    }

    @Test
    public void testFlatten_WithAlignment() {
        int[][] a = { { 1, 2, 3 }, { 4, 5 } };
        int[] result = IntStream.flatten(a, 0, true).toArray();
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 0 }, result);
    }

    // flatten(int[][], boolean) - with vertically=true
    @Test
    public void testFlatten_2D_Vertically() {
        int[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[] result = IntStream.flatten(arr, true).toArray();
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, result);
    }

    @Test
    public void testFlatten_2D_Horizontally() {
        int[][] arr = { { 1, 2 }, { 3, 4 } };
        int[] result = IntStream.flatten(arr, false).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    // flatten(int[][], int, boolean) - with padding value
    @Test
    public void testFlatten_2D_WithPadding_Vertically() {
        int[][] arr = { { 1, 2, 3 }, { 4, 5 } };
        int[] result = IntStream.flatten(arr, 0, true).toArray();
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 0 }, result);
    }

    @Test
    public void testFlatten_2D_WithPadding_Horizontally() {
        int[][] arr = { { 1, 2 }, { 3, 4, 5 } };
        int[] result = IntStream.flatten(arr, -1, false).toArray();
        assertArrayEquals(new int[] { 1, 2, -1, 3, 4, 5 }, result);
    }

    @Test
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        int[][] array = {};
        IntStream stream = IntStream.flatten(array);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatten_2D_EmptyArray_Vertically() {
        assertEquals(0, IntStream.flatten(new int[0][], true).count());
    }

    @Test
    public void testFlatten_2D_WithPadding_EmptyArray() {
        assertEquals(0, IntStream.flatten(new int[0][], 0, true).count());
    }

    @Test
    public void testRange_BasicRange() {
        stream = IntStream.range(0, 5);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testRange_WithStep() {
        stream = IntStream.range(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, stream.toArray());
    }

    @Test
    public void testDebounce_WithRange() {
        stream = IntStream.range(0, 100).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1));
        int[] result = stream.toArray();

        assertEquals(10, result.length);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, result[i]);
        }
    }

    @Test
    public void range_shouldCreateStreamOfRange() {
        assertArrayEquals(new int[] { 0, 1, 2 }, IntStream.range(0, 3).toArray());
    }

    @Test
    public void testStreamCreatedFromRange() {
        assertEquals(5, IntStream.range(0, 5).count());
        assertEquals(4, IntStream.range(0, 5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.range(0, 5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.range(0, 5).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.range(0, 5).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.range(0, 5).skip(1).toList());
        assertEquals(5, IntStream.range(0, 5).map(e -> e).count());
        assertEquals(4, IntStream.range(0, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.range(0, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.range(0, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.range(0, 5).map(e -> e).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.range(0, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRangeWithStep() {
        assertEquals(4, IntStream.range(0, 10, 3).count());
        assertEquals(3, IntStream.range(0, 10, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.range(0, 10, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.range(0, 10, 3).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.range(0, 10, 3).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.range(0, 10, 3).skip(1).toList());
        assertEquals(4, IntStream.range(0, 10, 3).map(e -> e).count());
        assertEquals(3, IntStream.range(0, 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.range(0, 10, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.range(0, 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.range(0, 10, 3).map(e -> e).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.range(0, 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            IntStream stream = IntStream.range(0, 100).parallel(executor);
            assertTrue(stream.isParallel());
            assertEquals(4950, stream.sum());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSps() {
        IntStream stream = IntStream.range(0, 100);
        IntStream result = stream.sps(s -> s.filter(n -> n % 2 == 0));
        assertFalse(result.isParallel());
        assertEquals(50, result.count());
    }

    @Test
    public void testLargeStreamOperations() {
        int size = 10_000;
        IntStream stream = IntStream.range(0, size);

        assertEquals(size, stream.count());

        stream = IntStream.range(0, size);
        long expectedSum = (long) size * (size - 1) / 2;
        assertEquals(expectedSum, stream.sum());
    }

    @Test
    public void testRange() {
        stream = IntStream.range(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());

        stream = IntStream.range(5, 5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRangeWithStep() {
        stream = IntStream.range(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, stream.toArray());

        stream = IntStream.range(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, stream.toArray());
    }

    @Test
    public void testParallel() {
        stream = IntStream.range(0, 1000).parallel();
        assertTrue(stream.isParallel());

        int sum = stream.sum();
        assertEquals(499500, sum);
    }

    @Test
    public void testSequential() {
        stream = IntStream.range(0, 100).parallel().sequential();
        assertFalse(stream.isParallel());
    }

    // range(int, int) with advance() exercised via skip/limit
    @Test
    public void testRange_WithSkip() {
        int[] result = IntStream.range(0, 10).skip(3).toArray();
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7, 8, 9 }, result);
    }

    @Test
    public void testRange_EmptyRange() {
        stream = IntStream.range(5, 5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRange_WithNegativeStep() {
        stream = IntStream.range(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, stream.toArray());
    }

    @Test
    public void range_emptyRange_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.range(3, 3).count());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result = IntStream.range(1, 101).summaryStatisticsAndPercentiles();

        IntSummaryStatistics stats = result.left();
        assertEquals(100, stats.getCount());
        assertEquals(5050, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(100, stats.getMax());

        assertTrue(result.right().isPresent());
        Map<Percentage, Integer> percentiles = result.right().get();
        assertNotNull(percentiles);
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        IntStream stream = IntStream.range(0, 1000).parallel(4);
        assertTrue(stream.isParallel());
        assertEquals(499500, stream.sum());
    }

    @Test
    public void testRange_NegativeToPositive() {
        int[] result = IntStream.range(-2, 3).toArray();
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2 }, result);
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        stream = IntStream.range(1, 5, -1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> IntStream.range(0, 10, 0));
    }

    @Test
    public void testRangeClosed_BasicRange() {
        stream = IntStream.rangeClosed(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_WithStep() {
        stream = IntStream.rangeClosed(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8, 10 }, stream.toArray());
    }

    @Test
    public void rangeClosed_shouldCreateInclusiveRangeStream() {
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, IntStream.rangeClosed(0, 3).toArray());
    }

    @Test
    public void testStreamCreatedFromRangeClosed() {
        assertEquals(6, IntStream.rangeClosed(0, 5).count());
        assertEquals(5, IntStream.rangeClosed(0, 5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).skip(1).toList());
        assertEquals(6, IntStream.rangeClosed(0, 5).map(e -> e).count());
        assertEquals(5, IntStream.rangeClosed(0, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).map(e -> e).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRangeClosedWithStep() {
        assertEquals(4, IntStream.rangeClosed(0, 9, 3).count());
        assertEquals(3, IntStream.rangeClosed(0, 9, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.rangeClosed(0, 9, 3).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.rangeClosed(0, 9, 3).skip(1).toList());
        assertEquals(4, IntStream.rangeClosed(0, 9, 3).map(e -> e).count());
        assertEquals(3, IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.rangeClosed(0, 9, 3).map(e -> e).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRangeClosedWithStep() {
        stream = IntStream.rangeClosed(0, 10, 3);
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_SingleElement() {
        stream = IntStream.rangeClosed(5, 5);
        assertArrayEquals(new int[] { 5 }, stream.toArray());
    }

    @Test
    public void testRangeClosed_WithNegativeStep() {
        stream = IntStream.rangeClosed(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2, 0 }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        stream = IntStream.rangeClosed(5, 1, -1);
        int[] result = stream.toArray();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, result);
    }

    // rangeClosed(int, int) with multiple elements uses IteratorIntStream path
    @Test
    public void testRangeClosed_MultipleElements() {
        int[] result = IntStream.rangeClosed(1, 10).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, result);
    }

    @Test
    public void testRangeClosed_NegativeRange() {
        int[] result = IntStream.rangeClosed(-3, 3).toArray();
        assertArrayEquals(new int[] { -3, -2, -1, 0, 1, 2, 3 }, result);
    }

    @Test
    public void testRangeClosed_EmptyWhenStartGreaterThanEnd() {
        assertEquals(0, IntStream.rangeClosed(5, 3).count());
    }

    @Test
    public void testRepeat_BasicRepeat() {
        stream = IntStream.repeat(5, 3);
        assertArrayEquals(new int[] { 5, 5, 5 }, stream.toArray());
    }

    @Test
    public void testStreamCreatedFromRepeat() {
        assertEquals(5, IntStream.repeat(7, 5).count());
        assertEquals(4, IntStream.repeat(7, 5).skip(1).count());
        assertArrayEquals(new int[] { 7, 7, 7, 7, 7 }, IntStream.repeat(7, 5).toArray());
        assertArrayEquals(new int[] { 7, 7, 7, 7 }, IntStream.repeat(7, 5).skip(1).toArray());
        assertEquals(N.toList(7, 7, 7, 7, 7), IntStream.repeat(7, 5).toList());
        assertEquals(N.toList(7, 7, 7, 7), IntStream.repeat(7, 5).skip(1).toList());
        assertEquals(5, IntStream.repeat(7, 5).map(e -> e).count());
        assertEquals(4, IntStream.repeat(7, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 7, 7, 7, 7, 7 }, IntStream.repeat(7, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 7, 7, 7, 7 }, IntStream.repeat(7, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(7, 7, 7, 7, 7), IntStream.repeat(7, 5).map(e -> e).toList());
        assertEquals(N.toList(7, 7, 7, 7), IntStream.repeat(7, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRepeat() {
        stream = IntStream.repeat(7, 3);
        assertArrayEquals(new int[] { 7, 7, 7 }, stream.toArray());

        stream = IntStream.repeat(7, 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeat_02() {
        IntStream stream = IntStream.repeat(1, 1000);
        assertEquals(1000, stream.mapToFloat(e -> (float) e).count());
    }

    // Tests targeting uncovered code paths identified by coverage report

    // repeat(int, long) with large n uses IteratorIntStream path (n >= 10)
    @Test
    public void testRepeat_LargeCount() {
        int[] result = IntStream.repeat(7, 15).toArray();
        assertEquals(15, result.length);
        for (int v : result) {
            assertEquals(7, v);
        }
    }

    @Test
    public void testRepeat_ExactlyTen() {
        int[] result = IntStream.repeat(3, 10).toArray();
        assertEquals(10, result.length);
        for (int v : result) {
            assertEquals(3, v);
        }
    }

    @Test
    public void testRepeat_ZeroTimes() {
        stream = IntStream.repeat(5, 0);
        assertEquals(0, stream.count());
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
    public void testStreamCreatedFromRandom() {
        assertEquals(10, IntStream.random().limit(10).count());
        assertEquals(9, IntStream.random().limit(10).skip(1).count());
        int[] randomArray = IntStream.random().limit(10).toArray();
        assertEquals(10, randomArray.length);
        randomArray = IntStream.random().limit(10).skip(1).toArray();
        assertEquals(9, randomArray.length);
        List<Integer> randomList = IntStream.random().limit(10).toList();
        assertEquals(10, randomList.size());
        randomList = IntStream.random().limit(10).skip(1).toList();
        assertEquals(9, randomList.size());
        assertEquals(10, IntStream.random().map(e -> e).limit(10).count());
        assertEquals(9, IntStream.random().map(e -> e).limit(10).skip(1).count());
        randomArray = IntStream.random().map(e -> e).limit(10).toArray();
        assertEquals(10, randomArray.length);
        randomArray = IntStream.random().map(e -> e).limit(10).skip(1).toArray();
        assertEquals(9, randomArray.length);
        randomList = IntStream.random().map(e -> e).limit(10).toList();
        assertEquals(10, randomList.size());
        randomList = IntStream.random().map(e -> e).limit(10).skip(1).toList();
        assertEquals(9, randomList.size());
    }

    @Test
    public void testStreamCreatedFromRandomWithRange() {
        assertEquals(10, IntStream.random(0, 100).limit(10).count());
        assertEquals(9, IntStream.random(0, 100).limit(10).skip(1).count());
        int[] randomArray = IntStream.random(0, 100).limit(10).toArray();
        assertEquals(10, randomArray.length);
        for (int i : randomArray) {
            assertTrue(i >= 0 && i < 100);
        }
        randomArray = IntStream.random(0, 100).limit(10).skip(1).toArray();
        assertEquals(9, randomArray.length);
        for (int i : randomArray) {
            assertTrue(i >= 0 && i < 100);
        }
        List<Integer> randomList = IntStream.random(0, 100).limit(10).toList();
        assertEquals(10, randomList.size());
        for (int i : randomList) {
            assertTrue(i >= 0 && i < 100);
        }
        randomList = IntStream.random(0, 100).limit(10).skip(1).toList();
        assertEquals(9, randomList.size());
        for (int i : randomList) {
            assertTrue(i >= 0 && i < 100);
        }
        assertEquals(10, IntStream.random(0, 100).map(e -> e).limit(10).count());
        assertEquals(9, IntStream.random(0, 100).map(e -> e).limit(10).skip(1).count());
        randomArray = IntStream.random(0, 100).map(e -> e).limit(10).toArray();
        assertEquals(10, randomArray.length);
        for (int i : randomArray) {
            assertTrue(i >= 0 && i < 100);
        }
        randomArray = IntStream.random(0, 100).map(e -> e).limit(10).skip(1).toArray();
        assertEquals(9, randomArray.length);
        for (int i : randomArray) {
            assertTrue(i >= 0 && i < 100);
        }
        randomList = IntStream.random(0, 100).map(e -> e).limit(10).toList();
        assertEquals(10, randomList.size());
        for (int i : randomList) {
            assertTrue(i >= 0 && i < 100);
        }
        randomList = IntStream.random(0, 100).map(e -> e).limit(10).skip(1).toList();
        assertEquals(9, randomList.size());
        for (int i : randomList) {
            assertTrue(i >= 0 && i < 100);
        }
    }

    @Test
    public void testRandom_WithRange_AllInRange() {
        int[] values = IntStream.random(10, 20).limit(100).toArray();
        assertEquals(100, values.length);
        for (int v : values) {
            assertTrue(v >= 10 && v < 20, "Value " + v + " should be in range [10, 20)");
        }
    }

    @Test
    public void testRandom_InRange() {
        int[] result = IntStream.random(5, 100).limit(10).toArray();
        assertEquals(10, result.length);
        for (int v : result) {
            assertTrue(v >= 5 && v < 100);
        }
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
    public void testStreamCreatedFromOfIndices() {
        assertEquals(5, IntStream.ofIndices(5).count());
        assertEquals(4, IntStream.ofIndices(5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.ofIndices(5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.ofIndices(5).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.ofIndices(5).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.ofIndices(5).skip(1).toList());
        assertEquals(5, IntStream.ofIndices(5).map(e -> e).count());
        assertEquals(4, IntStream.ofIndices(5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.ofIndices(5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.ofIndices(5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.ofIndices(5).map(e -> e).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.ofIndices(5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIndicesWithStep() {
        assertEquals(4, IntStream.ofIndices(10, 3).count());
        assertEquals(3, IntStream.ofIndices(10, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.ofIndices(10, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.ofIndices(10, 3).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.ofIndices(10, 3).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.ofIndices(10, 3).skip(1).toList());
        assertEquals(4, IntStream.ofIndices(10, 3).map(e -> e).count());
        assertEquals(3, IntStream.ofIndices(10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.ofIndices(10, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.ofIndices(10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.ofIndices(10, 3).map(e -> e).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.ofIndices(10, 3).map(e -> e).skip(1).toList());
    }

    // ofIndices(int, int) - range-based with step > 1
    @Test
    public void testOfIndices_RangeWithStep_StepTwo() {
        int[] result = IntStream.ofIndices(6, 2).toArray();
        assertArrayEquals(new int[] { 0, 2, 4 }, result);
    }

    @Test
    public void testOfIndices_RangeWithStep_StepOne() {
        int[] result = IntStream.ofIndices(4, 1).toArray();
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, result);
    }

    @Test
    public void testOfIndices_WithSource() {
        String source = "hello";
        stream = IntStream.ofIndices(source, (s, i) -> i < s.length() ? i : null);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfIndices_WithNegativeStep() {
        int[] result = IntStream.ofIndices(5, -1).toArray();
        assertArrayEquals(new int[] { 4, 3, 2, 1, 0 }, result);
    }

    // ===== New tests for untested methods =====

    @Test
    public void testOfIndices_WithSourceAndIncrement() {
        String source = "aXaXaX";
        int[] indices = IntStream.ofIndices(source, 0, 2, (s, fromIndex) -> {
            int idx = s.indexOf('a', fromIndex);
            return idx >= 0 ? idx : null;
        }).toArray();
        assertArrayEquals(new int[] { 0, 2, 4 }, indices);
    }

    @Test
    public void testOfIndices_RangeWithStep_NegativeStep() {
        int[] result = IntStream.ofIndices(4, -1).toArray();
        assertArrayEquals(new int[] { 3, 2, 1, 0 }, result);
    }

    // ofIndices(Object, int, int, ObjIntFunction) - source with fromIndex and increment
    @Test
    public void testOfIndices_SourceWithFromIndexAndIncrement() {
        int[] source = { 1, 2, 3, 1, 5, 1 };
        int[] result = IntStream.ofIndices(source, 1, 1, (arr, fromIdx) -> {
            for (int i = fromIdx; i < arr.length; i++) {
                if (arr[i] == 1)
                    return i;
            }
            return -1;
        }).toArray();
        assertArrayEquals(new int[] { 3, 5 }, result);
    }

    @Test
    public void testOfIndices_SourceWithFromIndex_NullSource() {
        int[] result = IntStream.ofIndices((int[]) null, 0, 1, (arr, fromIdx) -> fromIdx < 3 ? fromIdx : -1).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testOfIndices_RangeWithStep_Zero() {
        assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(5, 0));
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
    public void testIterateWithFalseHasNext() {
        stream = IntStream.iterate(() -> false, () -> 1);
        assertEquals(0, stream.count());
    }

    @Test
    public void iterate_shouldCreateInfiniteStream() {
        assertArrayEquals(new int[] { 0, 2, 4 }, IntStream.iterate(0, i -> i + 2).limit(3).toArray());
    }

    @Test
    public void testStreamCreatedFromIterate() {
        AtomicInteger counter = new AtomicInteger(0);
        assertEquals(5, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).count());

        counter.set(0);
        assertEquals(4, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).skip(1).toList());

        counter.set(0);
        assertEquals(5, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).count());

        counter.set(0);
        assertEquals(4, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromIterateWithInit() {
        AtomicInteger counter = new AtomicInteger(0);
        assertEquals(6, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).count());

        counter.set(0);
        assertEquals(5, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).skip(1).toList());

        counter.set(0);
        assertEquals(6, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).count());

        counter.set(0);
        assertEquals(5, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromIterateWithPredicate() {
        assertEquals(5, IntStream.iterate(0, i -> i < 5, i -> i + 1).count());
        assertEquals(4, IntStream.iterate(0, i -> i < 5, i -> i + 1).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).skip(1).toList());
        assertEquals(5, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).count());
        assertEquals(4, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromIterateInfinite() {
        assertEquals(10, IntStream.iterate(0, i -> i + 1).limit(10).count());
        assertEquals(9, IntStream.iterate(0, i -> i + 1).limit(10).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).limit(10).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).limit(10).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).limit(10).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).limit(10).skip(1).toList());
        assertEquals(10, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).count());
        assertEquals(9, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).toList());
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        IntStream stream = IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement());

        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testInfiniteStreamWithLimit() {
        IntStream stream = IntStream.iterate(1, n -> n + 1).limit(10);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, stream.toArray());
    }

    @Test
    public void testIterate() {
        stream = IntStream.iterate(1, n -> n * 2).limit(5);
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16 }, stream.toArray());

        stream = IntStream.iterate(1, n -> n < 10, n -> n + 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void test_iterate_02() {
        IntStream stream = IntStream.iterate(() -> true, () -> 1);
        assertEquals(10, stream.limit(10).mapToFloat(e -> (float) e).count());

        stream = IntStream.iterate(1, () -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToLong(e -> e).sum());

        stream = IntStream.iterate(1, it -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToLong(e -> e).sum());
    }

    // iterate(int, IntUnaryOperator) - infinite stream
    @Test
    public void testIterate_InfiniteWithLimit() {
        int[] result = IntStream.iterate(0, x -> x + 3).limit(4).toArray();
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, result);
    }

    // iterate(int, IntPredicate, IntUnaryOperator) - finite stream with predicate
    @Test
    public void testIterate_WithIntPredicate_HappyPath() {
        int[] result = IntStream.iterate(1, x -> x <= 16, x -> x * 2).toArray();
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16 }, result);
    }

    @Test
    public void testIterate_WithIntPredicate_FalseFromStart() {
        int[] result = IntStream.iterate(100, x -> x < 10, x -> x + 1).toArray();
        assertArrayEquals(new int[0], result);
    }

    // iterate(int, BooleanSupplier, IntUnaryOperator) - finite stream with BooleanSupplier
    @Test
    public void testIterate_WithBooleanSupplierAndInit() {
        AtomicInteger calls = new AtomicInteger(0);
        int[] result = IntStream.iterate(1, () -> calls.incrementAndGet() <= 3, x -> x + 10).toArray();
        assertArrayEquals(new int[] { 1, 11, 21 }, result);
    }

    // iterate(BooleanSupplier, IntSupplier) - BooleanSupplier-gated stream
    @Test
    public void testIterate_BooleanSupplierAndIntSupplier() {
        AtomicInteger counter = new AtomicInteger(10);
        int[] result = IntStream.iterate(() -> counter.get() > 7, () -> counter.getAndDecrement()).toArray();
        assertArrayEquals(new int[] { 10, 9, 8 }, result);
    }

    @Test
    public void testIterate_BooleanSupplierFalseImmediately() {
        int[] result = IntStream.iterate(() -> false, () -> 42).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testIterate_InfiniteNegative() {
        int[] result = IntStream.iterate(10, x -> x - 2).limit(5).toArray();
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, result);
    }

    @Test
    public void testGenerate_LimitedElements() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.generate(() -> counter.incrementAndGet()).limit(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void generate_shouldCreateInfiniteStream() {
        assertArrayEquals(new int[] { 0, 0, 0 }, IntStream.generate(() -> 0).limit(3).toArray());
    }

    @Test
    public void testStreamCreatedFromGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        assertEquals(5, IntStream.generate(() -> counter.getAndIncrement()).limit(5).count());

        counter.set(0);
        assertEquals(4, IntStream.generate(() -> counter.getAndIncrement()).limit(5).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).limit(5).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).limit(5).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).limit(5).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).limit(5).skip(1).toList());

        counter.set(0);
        assertEquals(5, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).count());

        counter.set(0);
        assertEquals(4, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.generate(counter::getAndIncrement).limit(5);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
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
    public void concat_shouldConcatenateStreams() {
        IntStream a = IntStream.of(1, 2);
        IntStream b = IntStream.of(3, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.concat(a, b).toArray());
    }

    @Test
    public void testStreamCreatedFromConcatArrays() {
        int[][] arrays = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, IntStream.concat(arrays).count());
        assertEquals(4, IntStream.concat(arrays).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(arrays).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(arrays).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(arrays).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(arrays).skip(1).toList());
        assertEquals(5, IntStream.concat(arrays).map(e -> e).count());
        assertEquals(4, IntStream.concat(arrays).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(arrays).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(arrays).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(arrays).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(arrays).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromConcatIterators() {
        IntIterator iter1 = IntIterator.of(1, 2);
        IntIterator iter2 = IntIterator.of(3, 4);
        IntIterator iter3 = IntIterator.of(5);
        assertEquals(5, IntStream.concat(iter1, iter2, iter3).count());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(4, IntStream.concat(iter1, iter2, iter3).skip(1).count());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(iter1, iter2, iter3).toArray());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(iter1, iter2, iter3).skip(1).toArray());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).toList());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).skip(1).toList());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(5, IntStream.concat(iter1, iter2, iter3).map(e -> e).count());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(4, IntStream.concat(iter1, iter2, iter3).map(e -> e).skip(1).count());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(iter1, iter2, iter3).map(e -> e).toArray());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(iter1, iter2, iter3).map(e -> e).skip(1).toArray());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).map(e -> e).toList());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testConcat() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(4, 5, 6);
        stream = IntStream.concat(s1, s2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void test_concat_02() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] result = IntStream.concat(a, b).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

        result = IntStream.concat(N.toList(a, b)).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

        result = IntStream.concatIterators(N.toList(IntIterator.of(a), IntIterator.of(b))).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testConcatIteratorsCollection() {
        List<IntIterator> iterators = Arrays.asList(IntIterator.of(new int[] { 1, 2 }), IntIterator.of(new int[] { 3, 4 }));
        IntStream result = IntStream.concatIterators(iterators);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result.toArray());
    }

    @Test
    public void testConcatIterators_Collection() {
        List<IntIterator> iters = new ArrayList<>();
        iters.add(IntIterator.of(new int[] { 1, 2 }));
        iters.add(IntIterator.of(new int[] { 3, 4 }));
        int[] result = IntStream.concatIterators(iters).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    // concatIterators(Collection) - collection of IntIterators
    @Test
    public void testConcatIterators_ThreeIterators() {
        java.util.List<IntIterator> iters = java.util.Arrays.asList(IntIterator.of(1, 2), IntIterator.of(3), IntIterator.of(4, 5, 6));
        int[] result = IntStream.concatIterators(iters).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testConcatIterators_EmptyCollection() {
        List<IntIterator> iters = new ArrayList<>();
        int[] result = IntStream.concatIterators(iters).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testConcatIterators_SingleIterator() {
        java.util.List<IntIterator> iters = java.util.Arrays.asList(IntIterator.of(10, 20, 30));
        int[] result = IntStream.concatIterators(iters).toArray();
        assertArrayEquals(new int[] { 10, 20, 30 }, result);
    }

    @Test
    public void testZip_WithArrays() {
        stream = IntStream.zip(new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 }, (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZip_WithArraysThreeWay() {
        stream = IntStream.zip(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5, 6 }, (a, b, c) -> a + b + c);
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
    public void testZipThreeArraysWithDefaultsAdditional() {
        int[] a = { 1 };
        int[] b = { 2, 3 };
        int[] c = { 4, 5, 6 };
        stream = IntStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z);
        int[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testZipThreeIteratorsWithDefaultsAdditional() {
        IntIterator a = IntIterator.of(new int[] { 1 });
        IntIterator b = IntIterator.of(new int[] { 2, 3 });
        IntIterator c = IntIterator.of(new int[] { 4, 5, 6 });
        stream = IntStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeStreamsWithDefaultsAdditional() {
        IntStream a = IntStream.of(1);
        IntStream b = IntStream.of(2, 3);
        IntStream c = IntStream.of(4, 5, 6);
        stream = IntStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z);
        int[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void zip_shouldZipStreams() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);
        IntStream zipped = IntStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new int[] { 5, 7, 9 }, zipped.toArray());
    }

    @Test
    public void testStreamCreatedFromZipArrays() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 10, 20, 30 };
        assertEquals(3, IntStream.zip(array1, array2, (a, b) -> a + b).count());
        assertEquals(2, IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(11, 22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).toList());
        assertEquals(N.toList(22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 11, 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new int[] { 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(11, 22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(N.toList(22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromZip3Arrays() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 10, 20, 30 };
        int[] array3 = { 100, 200, 300 };
        assertEquals(3, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).count());
        assertEquals(2, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new int[] { 111, 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new int[] { 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(N.toList(111, 222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).count());
        assertEquals(2, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 111, 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).toArray());
        assertArrayEquals(new int[] { 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(111, 222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).toList());
        assertEquals(N.toList(222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).toList());
    }

    @Test
    public void testZipThreeStreams() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20, 30);
        IntStream s3 = createIntStream(100, 200, 300);
        IntStream zipped = s1.zipWith(s2, s3, (a, b, c) -> a + b + c);

        assertArrayEquals(new int[] { 111, 222, 333 }, zipped.toArray());
    }

    @Test
    public void testZip() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20, 30);
        stream = IntStream.zip(s1, s2, Integer::sum);
        assertArrayEquals(new int[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void test_zip_02() {
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] result = IntStream.zip(a, b, (x, y) -> x + y).toArray();
            assertArrayEquals(new int[] { 5, 7, 9 }, result);
        }

        {
            int[] a = { 1, 2 };
            int[] b = { 4, 5, 6 };
            int[] result = IntStream.zip(a, b, 100, 200, (x, y) -> x + y).toArray();
            assertArrayEquals(new int[] { 5, 7, 106 }, result);
        }
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
            assertArrayEquals(new int[] { 12, 15, 18 }, result);
        }
        {
            int[] a = { 1 };
            int[] b = { 4, 5 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
            assertArrayEquals(new int[] { 12, 113, 309 }, result);
        }
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.zip(N.toList(IntStream.of(a), IntStream.of(b), IntStream.of(c)), it -> it[0] + it[1] + it[2]).toArray();
            assertArrayEquals(new int[] { 12, 15, 18 }, result);
        }
    }

    @Test
    public void testZipIteratorsThreeWay() {
        IntIterator a = IntIterator.of(new int[] { 1, 2 });
        IntIterator b = IntIterator.of(new int[] { 10, 20 });
        IntIterator c = IntIterator.of(new int[] { 100, 200 });
        int[] result = IntStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 222 }, result);
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        IntIterator a = IntIterator.of(new int[] { 1, 2, 3 });
        IntIterator b = IntIterator.of(new int[] { 10, 20 });
        int[] result = IntStream.zip(a, b, 0, 0, Integer::sum).toArray();
        assertArrayEquals(new int[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipIteratorsThreeWayWithDefaults() {
        IntIterator a = IntIterator.of(new int[] { 1 });
        IntIterator b = IntIterator.of(new int[] { 10, 20 });
        IntIterator c = IntIterator.of(new int[] { 100, 200, 300 });
        int[] result = IntStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 220, 300 }, result);
    }

    @Test
    public void testZipStreamsWithDefaults() {
        int[] result = IntStream.zip(IntStream.of(1, 2, 3), IntStream.of(10, 20), 0, 0, Integer::sum).toArray();
        assertArrayEquals(new int[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipStreamsThreeWayWithDefaults() {
        int[] result = IntStream.zip(IntStream.of(1), IntStream.of(10, 20), IntStream.of(100, 200, 300), 0, 0, 0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 220, 300 }, result);
    }

    // zip(IntStream, IntStream, IntBinaryOperator) - stream-based binary zip
    @Test
    public void testZip_TwoStreams_BinaryOp() {
        int[] result = IntStream.zip(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30), (a, b) -> a + b).toArray();
        assertArrayEquals(new int[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZip_TwoStreams_ShorterFirst() {
        int[] result = IntStream.zip(IntStream.of(1, 2), IntStream.of(10, 20, 30), (a, b) -> a * b).toArray();
        assertArrayEquals(new int[] { 10, 40 }, result);
    }

    // zip(IntStream, IntStream, IntStream, IntTernaryOperator) - three-stream zip
    @Test
    public void testZip_ThreeStreams_TernaryOp() {
        int[] result = IntStream.zip(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30), IntStream.of(100, 200, 300), (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new int[] { 111, 222, 333 }, result);
    }

    // zip(IntStream, IntStream, int, int, IntBinaryOperator) - binary zip with defaults
    @Test
    public void testZip_TwoStreams_WithDefaults_LongerSecond() {
        int[] result = IntStream.zip(IntStream.of(1, 2), IntStream.of(10, 20, 30), 0, 0, (a, b) -> a + b).toArray();
        assertArrayEquals(new int[] { 11, 22, 30 }, result);
    }

    @Test
    public void testZip_TwoStreams_WithDefaults_LongerFirst() {
        int[] result = IntStream.zip(IntStream.of(1, 2, 3), IntStream.of(10), 0, 99, (a, b) -> a + b).toArray();
        assertArrayEquals(new int[] { 11, 101, 102 }, result);
    }

    // zip(IntStream, IntStream, IntStream, int, int, int, IntTernaryOperator) - three-stream with defaults
    @Test
    public void testZip_ThreeStreams_WithDefaults() {
        int[] result = IntStream.zip(IntStream.of(1), IntStream.of(10, 20), IntStream.of(100, 200, 300), 0, 0, 0, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new int[] { 111, 220, 300 }, result);
    }

    // zip(IntIterator, IntIterator, int, int, IntBinaryOperator) - iterator binary with defaults
    @Test
    public void testZip_TwoIterators_WithDefaults() {
        IntIterator a = IntIterator.of(1, 2, 3);
        IntIterator b = IntIterator.of(10, 20);
        int[] result = IntStream.zip(a, b, 0, 99, (x, y) -> x + y).toArray();
        assertArrayEquals(new int[] { 11, 22, 102 }, result);
    }

    // zip(IntIterator, IntIterator, IntIterator, int, int, int, IntTernaryOperator) - iterator ternary with defaults
    @Test
    public void testZip_ThreeIterators_WithDefaults() {
        IntIterator a = IntIterator.of(1);
        IntIterator b = IntIterator.of(10, 20);
        IntIterator c = IntIterator.of(100, 200, 300);
        int[] result = IntStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 220, 300 }, result);
    }

    // zip(int[], int[], int[], int, int, int, IntTernaryOperator) - array ternary with defaults
    @Test
    public void testZip_ThreeArrays_WithDefaults() {
        int[] a = { 1, 2 };
        int[] b = { 10, 20, 30 };
        int[] c = { 100, 200, 300, 400 };
        int[] result = IntStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 222, 330, 400 }, result);
    }

    @Test
    public void testZip_ThreeStreams_AllEmpty() {
        int[] result = IntStream.zip(IntStream.empty(), IntStream.empty(), IntStream.empty(), (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testZip_ThreeStreams_WithDefaults_AllSameLength() {
        int[] result = IntStream.zip(IntStream.of(1, 2), IntStream.of(3, 4), IntStream.of(5, 6), -1, -1, -1, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new int[] { 9, 12 }, result);
    }

    @Test
    public void testZip_ThreeArrays_WithDefaults_AllSameLength() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] c = { 7, 8, 9 };
        int[] result = IntStream.zip(a, b, c, -1, -1, -1, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 12, 15, 18 }, result);
    }

    @Test
    public void testMerge_WithArrays() {
        stream = IntStream.merge(new int[] { 1, 3, 5 }, new int[] { 2, 4, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMerge_WithStreams() {
        IntStream s1 = IntStream.of(1, 3, 5);
        IntStream s2 = IntStream.of(2, 4, 6);
        stream = IntStream.merge(s1, s2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeThreeArraysAdditional() {
        int[] a1 = { 1, 7 };
        int[] a2 = { 3, 8 };
        int[] a3 = { 5, 9 };
        stream = IntStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeThreeStreamsAdditional() {
        IntStream s1 = IntStream.of(1, 7);
        IntStream s2 = IntStream.of(3, 8);
        IntStream s3 = IntStream.of(5, 9);
        stream = IntStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testStreamCreatedFromMergeArrays() {
        int[] array1 = { 1, 3, 5 };
        int[] array2 = { 2, 4, 6 };
        assertEquals(6, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(5, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(6, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(5, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromMerge3Arrays() {
        int[] array1 = { 1, 4, 7 };
        int[] array2 = { 2, 5, 8 };
        int[] array3 = { 3, 6, 9 };
        assertEquals(9, IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(8, IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(9, IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(8,
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void test_merge() {
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] result = IntStream.merge(a, b, (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
        }

        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.merge(a, b, c, (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
        }
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] result = IntStream.merge(N.toList(IntStream.of(a), IntStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
        }

        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream
                    .merge(N.toList(IntStream.of(a), IntStream.of(b), IntStream.of(c)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
        }
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.merge(N.toList(IntStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
            assertArrayEquals(new int[] { 1, 2, 3 }, result);

            result = IntStream.merge(N.toList(IntStream.of(a), IntStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

            result = IntStream
                    .merge(N.toList(IntStream.of(a), IntStream.of(b), IntStream.of(c)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
        }

    }

    @Test
    public void testMergeIteratorThreeWay() {
        IntIterator a = IntIterator.of(new int[] { 1, 4, 7 });
        IntIterator b = IntIterator.of(new int[] { 2, 5, 8 });
        IntIterator c = IntIterator.of(new int[] { 3, 6, 9 });
        int[] result = IntStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
    }

    @Test
    public void testMerge_CollectionOfStreams() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 3, 5), IntStream.of(2, 4, 6));
        int[] result = IntStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_ThreeArrays() {
        int[] result = IntStream
                .merge(new int[] { 1, 4 }, new int[] { 2, 5 }, new int[] { 3, 6 }, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_ThreeIterators() {
        int[] result = IntStream
                .merge(IntIterator.of(new int[] { 1, 4 }), IntIterator.of(new int[] { 2, 5 }), IntIterator.of(new int[] { 3, 6 }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testToMultiset() {
        Multiset<Integer> multiset = createIntStream(1, 2, 2, 3, 3, 3).toMultiset();

        assertEquals(1, multiset.getCount(1));
        assertEquals(2, multiset.getCount(2));
        assertEquals(3, multiset.getCount(3));
    }

    @Test
    public void testRateLimited() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(5);
        long startTime = System.currentTimeMillis();

        createIntStream(1, 2, 3).rateLimited(rateLimiter).count();

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 400, "Rate limiting should introduce delay");
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(100);
        long startTime = System.currentTimeMillis();

        createIntStream(1, 2, 3).delay(delay).count();

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 200, "Delay should be applied");
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).skip(2, skipped::add);

        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testDistinct() {
        stream = createIntStream(1, 2, 2, 3, 3, 3, 4).distinct();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testSorted() {
        stream = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).sorted();
        assertArrayEquals(new int[] { 1, 1, 2, 3, 4, 5, 6, 9 }, stream.toArray());
    }

    @Test
    public void testReverseSorted() {
        stream = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).reverseSorted();
        assertArrayEquals(new int[] { 9, 6, 5, 4, 3, 2, 1, 1 }, stream.toArray());
    }

    @Test
    public void testLimit() {
        stream = createIntStream(1, 2, 3, 4, 5).limit(3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testSkip() {
        stream = createIntStream(1, 2, 3, 4, 5).skip(2);
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testStep() {
        stream = createIntStream(1, 2, 3, 4, 5, 6, 7, 8).step(2);
        assertArrayEquals(new int[] { 1, 3, 5, 7 }, stream.toArray());
    }

    @Test
    public void testPeek() {
        List<Integer> sideEffect = new ArrayList<>();
        stream = createIntStream(1, 2, 3).peek(sideEffect::add);
        stream.toArray();
        assertEquals(Arrays.asList(1, 2, 3), sideEffect);
    }

    @Test
    public void testReversed() {
        stream = createIntStream(1, 2, 3, 4, 5).reversed();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testRotated() {
        stream = createIntStream(1, 2, 3, 4, 5).rotated(2);
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testShuffled() {
        int[] original = { 1, 2, 3, 4, 5 };
        stream = createIntStream(original).shuffled();
        int[] shuffled = stream.toArray();

        Arrays.sort(shuffled);
        assertArrayEquals(original, shuffled);
    }

    @Test
    public void testCycled() {
        stream = createIntStream(1, 2, 3).cycled(2);
        assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testIndexed() {
        Stream<IndexedInt> indexed = createIntStream(10, 20, 30).indexed();
        List<IndexedInt> list = indexed.toList();

        assertEquals(3, list.size());
        assertEquals(10, list.get(0).value());
        assertEquals(0, list.get(0).index());
        assertEquals(20, list.get(1).value());
        assertEquals(1, list.get(1).index());
    }

    @Test
    public void testIntersection() {
        stream = createIntStream(1, 2, 3, 4, 5).intersection(Arrays.asList(3, 4, 5, 6, 7));
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testDifference() {
        stream = createIntStream(1, 2, 3, 4, 5).difference(Arrays.asList(3, 4));
        assertArrayEquals(new int[] { 1, 2, 5 }, stream.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        stream = createIntStream(1, 2, 3).symmetricDifference(Arrays.asList(2, 3, 4));
        int[] result = stream.sorted().toArray();
        assertArrayEquals(new int[] { 1, 4 }, result);
    }

    @Test
    public void testToArray() {
        int[] array = createIntStream(1, 2, 3, 4, 5).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testToList() {
        List<Integer> list = createIntStream(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToSet() {
        Set<Integer> set = createIntStream(1, 2, 2, 3, 3).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createIntStream(1, 2).onlyOne());
    }

    @Test
    public void testJoin() {
        String joined = createIntStream(1, 2, 3).join(", ");
        assertEquals("1, 2, 3", joined);

        joined = createIntStream(1, 2, 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", joined);
    }

    @Test
    public void testNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> createIntStream(1, 2, 3).limit(-1));
    }

    @Test
    public void testNegativeSkip() {
        assertThrows(IllegalArgumentException.class, () -> createIntStream(1, 2, 3).skip(-1));
    }

    @Test
    public void testOperationAfterClose() {
        stream = createIntStream(1, 2, 3);
        stream.close();
        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        stream = createIntStream(1, 2, 3).onClose(() -> closed.set(true));
        stream.count();
        assertTrue(closed.get());
    }

    @Test
    public void testThrowIfEmpty() {
        stream = createIntStream(1, 2, 3).throwIfEmpty();
        assertEquals(3, stream.count());
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            createIntStream(1, 2, 3).println();
        });
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Integer>> percentiles = createIntStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles();
        assertTrue(percentiles.isPresent());

        Map<Percentage, Integer> map = percentiles.get();
        assertNotNull(map);
        assertTrue(map.containsKey(Percentage._50));
    }

    @Test
    public void test_toMultiset() {
        {
            Multiset<Integer> multiset = createIntStream(1, 2, 2, 3, 3).toMultiset();
            assertEquals(Multiset.of(1, 2, 2, 3, 3), multiset);
        }

        {
            Multiset<Integer> multiset = createIntStream(1, 2, 2, 3, 3).map(e -> e).toMultiset();
            assertEquals(Multiset.of(1, 2, 2, 3, 3), multiset);
        }
    }

}
