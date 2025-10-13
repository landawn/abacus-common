package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.If;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class IteratorStream100Test extends TestBase {

    private Stream<Integer> emptyStream;
    private Stream<Integer> singleElementStream;
    private Stream<Integer> multiElementStream;
    private Stream<String> stringStream;

    @BeforeEach
    public void setUp() {
        emptyStream = createStream(Collections.emptyList());
        singleElementStream = createStream(Arrays.asList(1));
        multiElementStream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        stringStream = createStream(Arrays.asList("a", "b", "c", "d", "e"));
    }

    private <T> Stream<T> createStream(Iterable<? extends T> iter) {
        return Stream.of(iter);
    }

    @Test
    public void testFilter() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> result = stream.filter(x -> x % 2 == 0).toList();
        assertEquals(Arrays.asList(2, 4), result);

        assertTrue(emptyStream.filter(x -> true).toList().isEmpty());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 3, 5));
        assertTrue(stream2.filter(x -> x % 2 == 0).toList().isEmpty());
    }

    @Test
    public void testTakeWhile() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> result = stream.takeWhile(x -> x < 4).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertTrue(stream2.takeWhile(x -> x < 0).toList().isEmpty());

        Stream<Integer> stream3 = createStream(Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), stream3.takeWhile(x -> x < 10).toList());
    }

    @Test
    public void testDropWhile() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> result = stream.dropWhile(x -> x < 3).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertTrue(stream2.dropWhile(x -> x < 10).toList().isEmpty());

        Stream<Integer> stream3 = createStream(Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), stream3.dropWhile(x -> x < 0).toList());
    }

    @Test
    public void testMap() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result = stream.map(x -> x * 2).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        List<String> stringResult = stream2.map(x -> "value" + x).toList();
        assertEquals(Arrays.asList("value1", "value2", "value3"), stringResult);

        assertTrue(emptyStream.map(x -> x * 2).toList().isEmpty());
    }

    @Test
    public void testSlidingMap() {
        {
            Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result = stream.slidingMap(1, false, (a, b) -> a + b).toList();
            assertEquals(Arrays.asList(3, 5, 7, 9), result);

            Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result2 = stream2.slidingMap(2, false, (a, b) -> a + (b == null ? 0 : b)).toList();
            assertEquals(Arrays.asList(3, 7, 5), result2);

            Stream<Integer> stream3 = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result3 = stream3.slidingMap(1, false, (a, b, c) -> a + b + c).toList();
            assertEquals(Arrays.asList(6, 9, 12), result3);

            Stream<Integer> stream4 = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result4 = stream4.slidingMap(2, false, (a, b, c) -> a + b + c).toList();
            assertEquals(Arrays.asList(6, 12), result4);

            Stream<Integer> stream5 = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result5 = stream5.slidingMap(3, false, (a, b, c) -> a + b + (c == null ? 0 : c)).toList();
            assertEquals(Arrays.asList(6, 9), result5);
        }

        {
            Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result = stream.slidingMap(1, true, (a, b) -> a + b).toList();
            assertEquals(Arrays.asList(3, 5, 7, 9), result);

            Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result2 = stream2.slidingMap(2, true, (a, b) -> a + (b == null ? 0 : b)).toList();
            assertEquals(Arrays.asList(3, 7), result2);

            Stream<Integer> stream3 = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result3 = stream3.slidingMap(1, true, (a, b, c) -> a + b + c).toList();
            assertEquals(Arrays.asList(6, 9, 12), result3);

            Stream<Integer> stream4 = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result4 = stream4.slidingMap(2, true, (a, b, c) -> a + b + c).toList();
            assertEquals(Arrays.asList(6, 12), result4);

            Stream<Integer> stream5 = createStream(Arrays.asList(1, 2, 3, 4, 5));
            List<Integer> result5 = stream5.slidingMap(3, true, (a, b, c) -> a + b + (c == null ? 0 : c)).toList();
            assertEquals(Arrays.asList(6), result5);
        }
    }

    @Test
    public void testMapFirst() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result = stream.mapFirst(x -> x * 10).toList();
        assertEquals(Arrays.asList(10, 2, 3), result);

        assertEquals(Arrays.asList(10), singleElementStream.mapFirst(x -> x * 10).toList());

        assertTrue(emptyStream.mapFirst(x -> x * 10).toList().isEmpty());
    }

    @Test
    public void testMapFirstOrElse() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<String> result = stream.mapFirstOrElse(x -> "first:" + x, x -> "other:" + x).toList();
        assertEquals(Arrays.asList("first:1", "other:2", "other:3"), result);
    }

    @Test
    public void testMapLast() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result = stream.mapLast(x -> x * 10).toList();
        assertEquals(Arrays.asList(1, 2, 30), result);

        assertEquals(Arrays.asList(10), singleElementStream.mapLast(x -> x * 10).toList());
    }

    @Test
    public void testMapLastOrElse() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<String> result = stream.mapLastOrElse(x -> "last:" + x, x -> "other:" + x).toList();
        assertEquals(Arrays.asList("other:1", "other:2", "last:3"), result);
    }

    @Test
    public void testMapToChar() {
        Stream<Integer> stream = createStream(Arrays.asList(65, 66, 67));
        char[] result = stream.mapToChar(x -> (char) x.intValue()).toArray();
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, result);
    }

    @Test
    public void testMapToByte() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        byte[] result = stream.mapToByte(x -> x.byteValue()).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToShort() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        short[] result = stream.mapToShort(x -> x.shortValue()).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToInt() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        int[] result = stream.mapToInt(x -> x * 2).toArray();
        assertArrayEquals(new int[] { 2, 4, 6 }, result);
    }

    @Test
    public void testMapToLong() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        long[] result = stream.mapToLong(x -> x.longValue() * 1000L).toArray();
        assertArrayEquals(new long[] { 1000L, 2000L, 3000L }, result);
    }

    @Test
    public void testMapToFloat() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        float[] result = stream.mapToFloat(x -> x.floatValue() / 2).toArray();
        assertArrayEquals(new float[] { 0.5f, 1.0f, 1.5f }, result, 0.001f);
    }

    @Test
    public void testMapToDouble() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        double[] result = stream.mapToDouble(x -> x.doubleValue() / 2).toArray();
        assertArrayEquals(new double[] { 0.5, 1.0, 1.5 }, result, 0.001);
    }

    @Test
    public void testFlatMap() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result = stream.flatMap(x -> Stream.of(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result2 = stream2.flatMap(x -> x % 2 == 0 ? Stream.of(x) : Stream.empty()).toList();
        assertEquals(Arrays.asList(2), result2);
    }

    @Test
    public void testFlatmap() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result = stream.flatmap(x -> Arrays.asList(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlattMap() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result = stream.flattmap(x -> new Integer[] { x, x * 10 }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlatMapToChar() {
        Stream<String> stream = createStream(Arrays.asList("ab", "cd"));
        char[] result = stream.flatMapToChar(s -> CharStream.of(s.toCharArray())).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatmapToChar() {
        Stream<String> stream = createStream(Arrays.asList("ab", "cd"));
        char[] result = stream.flatmapToChar(String::toCharArray).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatMapToByte() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        byte[] result = stream.flatMapToByte(x -> ByteStream.of((byte) x.intValue(), (byte) (x * 10))).toArray();
        assertArrayEquals(new byte[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatmapToByte() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        byte[] result = stream.flatmapToByte(x -> new byte[] { x.byteValue(), (byte) (x * 10) }).toArray();
        assertArrayEquals(new byte[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatMapToShort() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        short[] result = stream.flatMapToShort(x -> ShortStream.of(x.shortValue(), (short) (x * 10))).toArray();
        assertArrayEquals(new short[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatmapToShort() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        short[] result = stream.flatmapToShort(x -> new short[] { x.shortValue(), (short) (x * 10) }).toArray();
        assertArrayEquals(new short[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatMapToInt() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        int[] result = stream.flatMapToInt(x -> IntStream.of(x, x * 10)).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatmapToInt() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        int[] result = stream.flatmapToInt(x -> new int[] { x, x * 10 }).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatMapToLong() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        long[] result = stream.flatMapToLong(x -> LongStream.of(x, x * 10L)).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L }, result);
    }

    @Test
    public void testFlatmapToLong() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        long[] result = stream.flatmapToLong(x -> new long[] { x, x * 10L }).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L }, result);
    }

    @Test
    public void testFlatMapToFloat() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        float[] result = stream.flatMapToFloat(x -> FloatStream.of(x, x * 10f)).toArray();
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f }, result, 0.001f);
    }

    @Test
    public void testFlatmapToFloat() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        float[] result = stream.flatmapToFloat(x -> new float[] { x, x * 10f }).toArray();
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f }, result, 0.001f);
    }

    @Test
    public void testFlatMapToDouble() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        double[] result = stream.flatMapToDouble(x -> DoubleStream.of(x, x * 10.0)).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapToDouble() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        double[] result = stream.flatmapToDouble(x -> new double[] { x, x * 10.0 }).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0 }, result, 0.001);
    }

    @Test
    public void testSplitWithChunkSize() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<List<Integer>> result = stream.split(2).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4));
        List<List<Integer>> result2 = stream2.split(2).toList();
        assertEquals(2, result2.size());
        assertEquals(Arrays.asList(1, 2), result2.get(0));
        assertEquals(Arrays.asList(3, 4), result2.get(1));
    }

    @Test
    public void testSplitWithChunkSizeAndCollector() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> result = stream.split(2, Collectors.summingInt(Integer::intValue)).toList();
        assertEquals(Arrays.asList(3, 7, 5), result);
    }

    @Test
    public void testSplitByPredicate() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<List<Integer>> result = stream.split(x -> x % 2 == 0).toList();
        assertTrue(result.size() > 1);
    }

    @Test
    public void test_distinct_sort() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 2, 3, 3, 4, 5));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.sorted().distinct().toList());
    }

    @Test
    public void testSplitAt() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<Stream<Integer>> splits = stream.splitAt(3).toList();
        assertEquals(2, splits.size());
        assertEquals(Arrays.asList(1, 2, 3), splits.get(0).toList());
        assertEquals(Arrays.asList(4, 5), splits.get(1).toList());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        List<Stream<Integer>> splits2 = stream2.splitAt(0).toList();
        assertTrue(splits2.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2, 3), splits2.get(1).toList());
    }

    @Test
    public void testSplitAtWithCollector() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> result = stream.splitAt(3, Collectors.summingInt(Integer::intValue)).toList();
        assertEquals(Arrays.asList(6, 9), result);
    }

    @Test
    public void testSliding() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<List<Integer>> result = stream.sliding(3, 1).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4, 5, 6));
        List<List<Integer>> result2 = stream2.sliding(2, 3).toList();
        assertEquals(2, result2.size());
        assertEquals(Arrays.asList(1, 2), result2.get(0));
        assertEquals(Arrays.asList(4, 5), result2.get(1));
    }

    @Test
    public void testSlidingWithCollector() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> result = stream.sliding(3, 1, Collectors.summingInt(Integer::intValue)).toList();
        assertEquals(Arrays.asList(6, 9, 12), result);
    }

    @Test
    public void testDistinct() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 2, 3, 3, 3, 1));
        List<Integer> result = stream.distinct().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        assertTrue(emptyStream.distinct().toList().isEmpty());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), stream2.distinct().toList());
    }

    @Test
    public void testLimit() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(Arrays.asList(1, 2, 3), stream.limit(3).toList());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertTrue(stream2.limit(0).toList().isEmpty());

        Stream<Integer> stream3 = createStream(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2), stream3.limit(10).toList());
    }

    @Test
    public void testSkip() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(Arrays.asList(3, 4, 5), stream.skip(2).toList());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), stream2.skip(0).toList());

        Stream<Integer> stream3 = createStream(Arrays.asList(1, 2, 3));
        assertTrue(stream3.skip(10).toList().isEmpty());
    }

    @Test
    public void testTop() {
        {
            Stream<Integer> stream = createStream(Arrays.asList(5, 2, 8, 1, 9, 3));
            List<Integer> result = stream.top(3, Comparator.naturalOrder()).toList();
            assertEquals(3, result.size());
            assertTrue(result.containsAll(Arrays.asList(9, 8, 5)));

            Stream<Integer> stream2 = createStream(Arrays.asList(5, 2, 8, 1, 9, 3));
            List<Integer> result2 = stream2.top(3, Comparator.reverseOrder()).toList();
            assertEquals(3, result2.size());
            assertTrue(result2.containsAll(Arrays.asList(1, 2, 3)));
        }
        {
            Stream<Integer> stream = createStream(Arrays.asList(5, 2, 8, 1, 9, 3));
            Integer[] result = stream.top(3, Comparator.naturalOrder()).toArray(Integer[]::new);
            assertEquals(3, result.length);
            assertArrayEquals(new Integer[] { 5, 8, 9 }, result);

            Stream<Integer> stream2 = createStream(Arrays.asList(5, 2, 8, 1, 9, 3));
            Integer[] result2 = stream2.top(3, Comparator.reverseOrder()).toArray(Integer[]::new);
            assertEquals(3, result.length);
            assertArrayEquals(new Integer[] { 3, 1, 2 }, result2);
        }

    }

    @Test
    public void testOnEach() {
        List<Integer> sideEffect = new ArrayList<>();
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result = stream.onEach(sideEffect::add).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(Arrays.asList(1, 2, 3), sideEffect);
    }

    @Test
    public void testForEach() {
        List<Integer> result = new ArrayList<>();
        multiElementStream.forEach(result::add);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testForEachWithOnComplete() {
        List<Integer> result = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        stream.forEach(result::add, () -> completed.incrementAndGet());
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(1, completed.get());
    }

    @Test
    public void testForEachWithFlatMapper() {
        List<String> result = new ArrayList<>();
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        stream.forEach(x -> Arrays.asList("a" + x, "b" + x), (x, s) -> result.add(s));
        assertEquals(Arrays.asList("a1", "b1", "a2", "b2"), result);
    }

    @Test
    public void testForEachWithDoubleFlatMapper() {
        List<String> result = new ArrayList<>();
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        stream.forEach(x -> Arrays.asList("a" + x, "b" + x), s -> Arrays.asList(s + "1", s + "2"), (x, s, t) -> result.add(t));
        assertEquals(Arrays.asList("a11", "a12", "b11", "b12", "a21", "a22", "b21", "b22"), result);
    }

    @Test
    public void testForEachPair() {
        List<String> result = new ArrayList<>();
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4));
        stream.forEachPair(1, (a, b) -> result.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "2,3", "3,4"), result);

        List<String> result2 = new ArrayList<>();
        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4, 5));
        stream2.forEachPair(2, (a, b) -> result2.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "3,4", "5,null"), result2);
    }

    @Test
    public void testForEachTriple() {
        List<String> result = new ArrayList<>();
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        stream.forEachTriple(1, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5"), result);

        List<String> result2 = new ArrayList<>();
        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4, 5, 6));
        stream2.forEachTriple(2, (a, b, c) -> result2.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "3,4,5", "5,6,null"), result2);
    }

    @Test
    public void testToArray() {
        Object[] array = multiElementStream.toArray();
        assertArrayEquals(new Object[] { 1, 2, 3, 4, 5 }, array);

        assertEquals(0, emptyStream.toArray().length);
    }

    @Test
    public void testToList() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), multiElementStream.toList());
        assertTrue(emptyStream.toList().isEmpty());
    }

    @Test
    public void testToSet() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 2, 3, 3, 3));
        Set<Integer> result = stream.toSet();
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testToCollection() {
        LinkedList<Integer> result = multiElementStream.toCollection(LinkedList::new);
        assertTrue(result instanceof LinkedList);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testToMultiset() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 2, 3, 3, 3));
        Multiset<Integer> result = stream.toMultiset();
        assertEquals(1, result.occurrencesOf(1));
        assertEquals(2, result.occurrencesOf(2));
        assertEquals(3, result.occurrencesOf(3));
    }

    @Test
    public void testToMultisetWithSupplier() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 2, 3, 3, 3));
        Multiset<Integer> result = stream.toMultiset(Multiset::new);
        assertEquals(1, result.occurrencesOf(1));
        assertEquals(2, result.occurrencesOf(2));
        assertEquals(3, result.occurrencesOf(3));
    }

    @Test
    public void testToMap() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        Map<Integer, String> result = stream.toMap(x -> x, x -> "value" + x);
        assertEquals(3, result.size());
        assertEquals("value1", result.get(1));
        assertEquals("value2", result.get(2));
        assertEquals("value3", result.get(3));
    }

    @Test
    public void testToMapWithMergeFunction() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 1, 3, 2));
        Map<Integer, Integer> result = stream.toMap(x -> x % 2, x -> x, (a, b) -> a + b);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(5), result.get(1));
        assertEquals(Integer.valueOf(4), result.get(0));
    }

    @Test
    public void testToMultimap() {
        Stream<String> stream = createStream(Arrays.asList("a1", "a2", "b1", "b2"));
        Multimap<Character, String, List<String>> result = stream.toMultimap(s -> s.charAt(0), s -> s);
        assertEquals(Arrays.asList("a1", "a2"), result.get('a'));
        assertEquals(Arrays.asList("b1", "b2"), result.get('b'));
    }

    @Test
    public void testFoldLeft() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4));
        Optional<Integer> result = stream.foldLeft((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(10), result.get());

        assertFalse(emptyStream.foldLeft((a, b) -> a + b).isPresent());
    }

    @Test
    public void testFoldLeftWithIdentity() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4));
        Integer result = stream.foldLeft(0, (a, b) -> a + b);
        assertEquals(Integer.valueOf(10), result);

        assertEquals(Integer.valueOf(100), emptyStream.foldLeft(100, (a, b) -> a + b));
    }

    @Test
    public void testFoldRight() {
        Stream<String> stream = createStream(Arrays.asList("a", "b", "c"));
        Optional<String> result = stream.foldRight((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals("cba", result.get());
    }

    @Test
    public void testFoldRightWithIdentity() {
        Stream<String> stream = createStream(Arrays.asList("a", "b", "c"));
        String result = stream.foldRight("", (a, b) -> a + b);
        assertEquals("cba", result);
    }

    @Test
    public void testReduce() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4));
        Optional<Integer> result = stream.reduce((a, b) -> a * b);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(24), result.get());
    }

    @Test
    public void testReduceWithIdentityAndCombiner() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4));
        Integer result = stream.reduce(1, (a, b) -> a * b, (a, b) -> a * b);
        assertEquals(Integer.valueOf(24), result);
    }

    @Test
    public void testCollectWithSupplierAccumulatorCombiner() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4));
        List<Integer> result = stream.collect(ArrayList::new, List::add, List::addAll);
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testCollectWithCollector() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4));
        List<Integer> result = stream.collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3, 4), result);

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4));
        Integer sum = stream2.collect(Collectors.summingInt(Integer::intValue));
        assertEquals(Integer.valueOf(10), sum);
    }

    @Test
    public void testTakeLast() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(Arrays.asList(3, 4, 5), stream.takeLast(3).toList());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertTrue(stream2.takeLast(0).toList().isEmpty());

        Stream<Integer> stream3 = createStream(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2), stream3.takeLast(10).toList());
    }

    @Test
    public void testSkipLast() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(Arrays.asList(1, 2), stream.skipLast(3).toList());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), stream2.skipLast(0).toList());

        Stream<Integer> stream3 = createStream(Arrays.asList(1, 2));
        assertTrue(stream3.skipLast(10).toList().isEmpty());
    }

    @Test
    public void testMin() {
        Stream<Integer> stream = createStream(Arrays.asList(3, 1, 4, 1, 5));
        Optional<Integer> min = stream.min(Comparator.naturalOrder());
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(1), min.get());

        assertFalse(emptyStream.min(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testMax() {
        Stream<Integer> stream = createStream(Arrays.asList(3, 1, 4, 1, 5));
        Optional<Integer> max = stream.max(Comparator.naturalOrder());
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(5), max.get());

        assertFalse(emptyStream.max(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testKthLargest() {
        Stream<Integer> stream = createStream(Arrays.asList(3, 1, 4, 1, 5, 9, 2));
        Optional<Integer> kth = stream.kthLargest(3, Comparator.naturalOrder());
        assertTrue(kth.isPresent());
        assertEquals(Integer.valueOf(4), kth.get());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2));
        assertFalse(stream2.kthLargest(3, Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testCount() {
        assertEquals(5, multiElementStream.count());
        assertEquals(0, emptyStream.count());
        assertEquals(1, singleElementStream.count());
    }

    @Test
    public void testAnyMatch() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(stream.anyMatch(x -> x > 3));

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertFalse(stream2.anyMatch(x -> x > 10));

        assertFalse(emptyStream.anyMatch(x -> true));
    }

    @Test
    public void testAllMatch() {
        Stream<Integer> stream = createStream(Arrays.asList(2, 4, 6));
        assertTrue(stream.allMatch(x -> x % 2 == 0));

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertFalse(stream2.allMatch(x -> x % 2 == 0));

        assertTrue(emptyStream.allMatch(x -> false));
    }

    @Test
    public void testNoneMatch() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 3, 5));
        assertTrue(stream.noneMatch(x -> x % 2 == 0));

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertFalse(stream2.noneMatch(x -> x % 2 == 0));

        assertTrue(emptyStream.noneMatch(x -> true));
    }

    @Test
    public void testNMatch() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(stream.nMatch(2, 3, x -> x % 2 == 0));

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3, 4, 5));
        assertFalse(stream2.nMatch(3, 4, x -> x % 2 == 0));

        assertTrue(emptyStream.nMatch(0, 0, x -> true));
    }

    @Test
    public void testFindFirst() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        Optional<Integer> result = stream.findFirst(x -> x > 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertFalse(stream2.findFirst(x -> x > 10).isPresent());
    }

    @Test
    public void testFindLast() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        Optional<Integer> result = stream.findLast(x -> x < 4);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        assertFalse(stream2.findLast(x -> x > 10).isPresent());
    }

    @Test
    public void testAppendIfEmpty() {
        Stream<Integer> stream = createStream(Collections.emptyList());
        List<Integer> result = stream.appendIfEmpty(Arrays.asList(10, 20)).toList();
        assertEquals(Arrays.asList(10, 20), result);

        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result2 = stream2.appendIfEmpty(Arrays.asList(10, 20)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result2);
    }

    @Test
    public void testAppendIfEmptyWithSupplier() {
        Stream<Integer> stream = createStream(Collections.emptyList());
        List<Integer> result = stream.appendIfEmpty(() -> Stream.of(10, 20)).toList();
        assertEquals(Arrays.asList(10, 20), result);

        AtomicInteger calls = new AtomicInteger(0);
        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        List<Integer> result2 = stream2.appendIfEmpty(() -> {
            calls.incrementAndGet();
            return Stream.of(10, 20);
        }).toList();
        assertEquals(Arrays.asList(1, 2, 3), result2);
        assertEquals(0, calls.get());
    }

    @Test
    public void testIfEmpty() {
        AtomicInteger calls = new AtomicInteger(0);
        Stream<Integer> stream = createStream(Collections.emptyList());
        stream.ifEmpty(() -> calls.incrementAndGet()).toList();
        assertEquals(1, calls.get());

        calls.set(0);
        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        stream2.ifEmpty(() -> calls.incrementAndGet()).toList();
        assertEquals(0, calls.get());
    }

    @Test
    public void testIteratorEx() {
        Iterator<Integer> iter = multiElementStream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(Integer.valueOf(1), iter.next());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testParallel() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        Stream<Integer> parallelStream = stream.parallel();
        assertTrue(parallelStream.isParallel());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), parallelStream.toList());
    }

    @Test
    public void testApplyIfNotEmpty() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        Optional<Integer> result = stream.applyIfNotEmpty(s -> s.reduce(0, Integer::sum));
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(6), result.get());

        Optional<Integer> emptyResult = emptyStream.applyIfNotEmpty(s -> s.reduce(0, Integer::sum));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicInteger sum = new AtomicInteger(0);
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        If.OrElse result = stream.acceptIfNotEmpty(s -> s.forEach(sum::addAndGet));
        assertTrue(result == If.OrElse.TRUE);
        assertEquals(6, sum.get());

        sum.set(0);
        If.OrElse emptyResult = emptyStream.acceptIfNotEmpty(s -> s.forEach(sum::addAndGet));
        assertTrue(emptyResult == If.OrElse.FALSE);
        assertEquals(0, sum.get());
    }

    @Test
    public void testToJdkStream() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3, 4, 5));
        java.util.stream.Stream<Integer> jdkStream = stream.toJdkStream();
        List<Integer> result = jdkStream.collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testOnClose() {
        AtomicInteger closeCalls = new AtomicInteger(0);
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        stream.onClose(() -> closeCalls.incrementAndGet()).toList();
        assertEquals(1, closeCalls.get());

        closeCalls.set(0);
        Stream<Integer> stream2 = createStream(Arrays.asList(1, 2, 3));
        stream2.onClose(() -> closeCalls.incrementAndGet()).onClose(() -> closeCalls.incrementAndGet()).toList();
        assertEquals(2, closeCalls.get());
    }
}
