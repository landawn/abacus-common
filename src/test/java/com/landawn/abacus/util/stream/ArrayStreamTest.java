package com.landawn.abacus.util.stream;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u;
import com.landawn.abacus.util.u.Optional;

public class ArrayStreamTest extends TestBase {

    private String[] stringArray;
    private Integer[] integerArray;
    private String[] emptyArray;

    @BeforeEach
    public void setUp() {
        stringArray = new String[] { "apple", "banana", "cherry", "date", "elderberry" };
        integerArray = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        emptyArray = new String[0];
    }

    @Test
    public void testFilter() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.filter(x -> x % 2 == 0).toList();
        assertEquals(Arrays.asList(2, 4, 6, 8, 10), result);

        Stream<String> stream2 = Stream.of(stringArray);
        List<String> result2 = stream2.filter(s -> s.startsWith("z")).toList();
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testEdgeCases() {
        String[] arrayWithNulls = { "a", null, "b", null, "c" };
        Stream<String> stream = Stream.of(arrayWithNulls);
        List<String> result = stream.filter(Objects::nonNull).toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        Stream<String> singleStream = Stream.of(new String[] { "single" });
        assertEquals("single", singleStream.first().get());

        Stream<Integer> rangeStream = Stream.of(integerArray, 0, 0);
        assertEquals(0, rangeStream.count());

        Stream<Integer> fullRangeStream = Stream.of(integerArray, 0, integerArray.length);
        assertEquals(10, fullRangeStream.count());
    }

    @Test
    public void testTakeWhile() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.takeWhile(x -> x <= 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.takeWhile(x -> x > 10).toList();
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testDropWhile() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.dropWhile(x -> x <= 5).toList();
        assertEquals(Arrays.asList(6, 7, 8, 9, 10), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.dropWhile(x -> x <= 20).toList();
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testStep() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.step(2).toList();
        assertEquals(Arrays.asList(1, 3, 5, 7, 9), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.step(1).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result2);

        Stream<Integer> stream3 = Stream.of(integerArray);
        List<Integer> result3 = stream3.step(20).toList();
        assertEquals(Arrays.asList(1), result3);
    }

    @Test
    public void testStepMultiple() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<Integer> result = Stream.of(arr).step(3).toList();
        assertEquals(Arrays.asList(1, 4, 7, 10), result);

        result = Stream.of(arr).step(5).toList();
        assertEquals(Arrays.asList(1, 6), result);
    }

    @Test
    public void testStep_Advance_Zero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).step(2);
        ObjIteratorEx<Integer> iter = (ObjIteratorEx) stream.iterator();
        iter.advance(0);
        assertEquals(1, iter.next());
    }

    @Test
    public void testStep_ToArray_Sufficient_Length() {
        Stream<Integer> stream = Stream.of(1, 2, 3).step(2);
        ObjIterator<Integer> iter = stream.iterator();
        Integer[] arr = new Integer[2];
        Integer[] result = iter.toArray(arr);
        assertSame(arr, result);
        assertEquals(1, result[0]);
        assertEquals(3, result[1]);
    }

    @Test
    public void testStep_singleElement() {
        Stream<Integer> stream = Stream.of(new Integer[] { 42 });
        // empty stream path: fromIndex == toIndex is false for single element
        List<Integer> result = stream.step(3).toList();
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(42), result.get(0));
    }

    @Test
    public void testStep_emptyStream() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        List<Integer> result = stream.step(2).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testStep_stepOne() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        // step == 1 returns same stream
        List<Integer> result = stream.step(1).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testExceptionCases() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream<Integer> stream = Stream.of(integerArray);
            stream.step(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Stream<Integer> stream = Stream.of(integerArray);
            stream.limit(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Stream<Integer> stream = Stream.of(integerArray);
            stream.skip(-1);
        });
    }

    @Test
    public void testStep_Next_After_Last() {
        Stream<Integer> stream = Stream.of(1, 2, 3).step(2);
        ObjIterator<Integer> iter = stream.iterator();
        assertEquals(1, iter.next());
        assertEquals(3, iter.next());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testMap() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.map(String::toUpperCase).toList();
        assertEquals(Arrays.asList("APPLE", "BANANA", "CHERRY", "DATE", "ELDERBERRY"), result);

        Stream<String> stream2 = Stream.of(stringArray);
        List<Integer> result2 = stream2.map(String::length).toList();
        assertEquals(Arrays.asList(5, 6, 6, 4, 10), result2);
    }

    @Test
    public void testSlidingMapTriple() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<String> result = stream.slidingMap(1, false, (a, b, c) -> a + "-" + b + "-" + c).toList();
        assertEquals(8, result.size());
        assertEquals("1-2-3", result.get(0));
        assertEquals("8-9-10", result.get(7));
    }

    @Test
    public void testSlidingMap() {
        {
            Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result = stream.slidingMap(1, false, (a, b) -> a + b).toList();
            assertEquals(Arrays.asList(3, 5, 7, 9), result);

            Stream<Integer> stream2 = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result2 = stream2.slidingMap(2, false, (a, b) -> a + (b == null ? 0 : b)).toList();
            assertEquals(Arrays.asList(3, 7, 5), result2);

            Stream<Integer> stream3 = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result3 = stream3.slidingMap(1, false, (a, b, c) -> a + b + c).toList();
            assertEquals(Arrays.asList(6, 9, 12), result3);

            Stream<Integer> stream4 = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result4 = stream4.slidingMap(2, false, (a, b, c) -> a + b + c).toList();
            assertEquals(Arrays.asList(6, 12), result4);

            Stream<Integer> stream5 = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result5 = stream5.slidingMap(3, false, (a, b, c) -> a + b + (c == null ? 0 : c)).toList();
            assertEquals(Arrays.asList(6, 9), result5);
        }

        {
            Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result = stream.slidingMap(1, true, (a, b) -> a + b).toList();
            assertEquals(Arrays.asList(3, 5, 7, 9), result);

            Stream<Integer> stream2 = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result2 = stream2.slidingMap(2, true, (a, b) -> a + (b == null ? 0 : b)).toList();
            assertEquals(Arrays.asList(3, 7), result2);

            Stream<Integer> stream3 = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result3 = stream3.slidingMap(1, true, (a, b, c) -> a + b + c).toList();
            assertEquals(Arrays.asList(6, 9, 12), result3);

            Stream<Integer> stream4 = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result4 = stream4.slidingMap(2, true, (a, b, c) -> a + b + c).toList();
            assertEquals(Arrays.asList(6, 12), result4);

            Stream<Integer> stream5 = Stream.of(1, 2, 3, 4, 5);
            List<Integer> result5 = stream5.slidingMap(3, true, (a, b, c) -> a + b + (c == null ? 0 : c)).toList();
            assertEquals(Arrays.asList(6), result5);
        }
    }

    @Test
    public void testSlidingMapEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        List<Integer> result = stream.slidingMap(1, false, (a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapFirst() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.mapFirst(String::toUpperCase).toList();
        assertEquals("APPLE", result.get(0));
        assertEquals("banana", result.get(1));

        Stream<String> stream2 = Stream.of(new String[] { "single" });
        List<String> result2 = stream2.mapFirst(String::toUpperCase).toList();
        assertEquals(Arrays.asList("SINGLE"), result2);

        Stream<String> stream3 = Stream.of(emptyArray);
        List<String> result3 = stream3.mapFirst(String::toUpperCase).toList();
        assertTrue(result3.isEmpty());
    }

    @Test
    public void testMapFirstFromArray() {
        Integer[] arr = { 1, 2, 3 };
        List<Integer> result = Stream.of(arr).mapFirst(x -> x * 100).toList();
        assertEquals(Arrays.asList(100, 2, 3), result);

        result = Stream.of(new Integer[0]).mapFirst(x -> x * 100).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapFirstOrElse() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.mapFirstOrElse(String::toUpperCase, String::toLowerCase).toList();
        assertEquals("APPLE", result.get(0));
        assertEquals("banana", result.get(1));

        Stream<String> stream2 = Stream.of(emptyArray);
        List<String> result2 = stream2.mapFirstOrElse(String::toUpperCase, String::toLowerCase).toList();
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testMapFirstOrElse_empty() {
        Stream<String> stream = Stream.of(new String[0]);
        List<String> result = stream.mapFirstOrElse(String::toUpperCase, String::toLowerCase).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testMapFirstOrElse_singleElement() {
        Stream<String> stream = Stream.of(new String[] { "hello" });
        List<String> result = stream.mapFirstOrElse(String::toUpperCase, String::toLowerCase).toList();
        assertEquals(1, result.size());
        assertEquals("HELLO", result.get(0));
    }

    @Test
    public void testMapFirstOrElse_multipleElements() {
        Stream<String> stream = Stream.of(new String[] { "Hello", "World", "Test" });
        List<String> result = stream.mapFirstOrElse(String::toUpperCase, String::toLowerCase).toList();
        assertEquals(3, result.size());
        assertEquals("HELLO", result.get(0));
        assertEquals("world", result.get(1));
        assertEquals("test", result.get(2));
    }

    @Test
    public void testMapLastFromArray() {
        Integer[] arr = { 1, 2, 3 };
        List<Integer> result = Stream.of(arr).mapLast(x -> x * 100).toList();
        assertEquals(Arrays.asList(1, 2, 300), result);
    }

    @Test
    public void testMapLast() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.mapLast(String::toUpperCase).toList();
        assertEquals("apple", result.get(0));
        assertEquals("ELDERBERRY", result.get(4));

        Stream<String> stream2 = Stream.of(new String[] { "single" });
        List<String> result2 = stream2.mapLast(String::toUpperCase).toList();
        assertEquals(Arrays.asList("SINGLE"), result2);
    }

    @Test
    public void testMapLastOrElse() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.mapLastOrElse(String::toUpperCase, String::toLowerCase).toList();
        assertEquals("apple", result.get(0));
        assertEquals("ELDERBERRY", result.get(4));
    }

    @Test
    public void testMapToChar() {
        Stream<String> stream = Stream.of(new String[] { "a", "b", "c" });
        char[] result = stream.mapToChar(s -> s.charAt(0)).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testMapToCharEmpty() {
        Stream<String> stream = Stream.of(emptyArray);
        char[] result = stream.mapToChar(s -> s.charAt(0)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToByte() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        byte[] result = stream.mapToByte(Integer::byteValue).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToByteEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        byte[] result = stream.mapToByte(Integer::byteValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToShort() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        short[] result = stream.mapToShort(Integer::shortValue).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToShortEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        short[] result = stream.mapToShort(Integer::shortValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToInt() {
        Stream<String> stream = Stream.of(stringArray);
        int[] result = stream.mapToInt(String::length).toArray();
        assertArrayEquals(new int[] { 5, 6, 6, 4, 10 }, result);
    }

    @Test
    public void testMapToIntEmpty() {
        Stream<String> stream = Stream.of(emptyArray);
        int[] result = stream.mapToInt(String::length).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToLong() {
        Stream<Integer> stream = Stream.of(integerArray);
        long[] result = stream.mapToLong(Integer::longValue).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, result);
    }

    @Test
    public void testMapToLongEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        long[] result = stream.mapToLong(Integer::longValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToFloat() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        float[] result = stream.mapToFloat(Integer::floatValue).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testMapToFloatEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        float[] result = stream.mapToFloat(Integer::floatValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToDouble() {
        Stream<Integer> stream = Stream.of(integerArray);
        double[] result = stream.mapToDouble(Integer::doubleValue).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 }, result, 0.001);
    }

    @Test
    public void testMapToDoubleEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        double[] result = stream.mapToDouble(Integer::doubleValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMap() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        List<Character> result = stream.flatMap(s -> Stream.of(s.charAt(0), s.charAt(1))).toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);
    }

    @Test
    public void testFlatmap() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        List<Character> result = stream.flatmap(s -> Arrays.asList(s.charAt(0), s.charAt(1))).toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);
    }

    @Test
    public void testFlatMapEmpty() {
        Stream<String> stream = Stream.of(emptyArray);
        List<Character> result = stream.flatMap(s -> Stream.of(s.charAt(0))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapEmpty() {
        Stream<String> stream = Stream.of(emptyArray);
        List<Character> result = stream.flatmap(s -> Arrays.asList(s.charAt(0))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattMap() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        List<Character> result = stream.flatMapArray(s -> new Character[] { s.charAt(0), s.charAt(1) }).toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);
    }

    @Test
    public void testFlatMapArray() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        List<Character> result = stream.flatMapArray(s -> new Character[] { s.charAt(0), s.charAt(1) }).toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);
    }

    @Test
    public void testFlatMapToChar() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        char[] result = stream.flatMapToChar(s -> CharStream.of(s.toCharArray())).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatmapToChar() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        char[] result = stream.flatmapToChar(String::toCharArray).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatMapToCharWithContent() {
        String[] arr = { "hi", "yo" };
        char[] result = Stream.of(arr).flatMapToChar(s -> CharStream.of(s.toCharArray())).toArray();
        assertEquals(4, result.length);
        assertEquals('h', result[0]);
        assertEquals('i', result[1]);
        assertEquals('y', result[2]);
        assertEquals('o', result[3]);
    }

    @Test
    public void testFlatmapToCharWithContent() {
        String[] arr = { "hi", "yo" };
        char[] result = Stream.of(arr).flatmapToChar(String::toCharArray).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testFlatMapToCharEmpty() {
        Stream<String> stream = Stream.of(emptyArray);
        char[] result = stream.flatMapToChar(s -> CharStream.of(s.toCharArray())).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToByte() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        byte[] result = stream.flatMapToByte(i -> ByteStream.of(new byte[] { i.byteValue(), (byte) (i * 10) })).toArray();
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatmapToByte() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        byte[] result = stream.flatmapToByte(i -> new byte[] { i.byteValue(), (byte) (i * 10) }).toArray();
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatMapToShort() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2 });
        short[] result = stream.flatMapToShort(i -> ShortStream.of(new short[] { i.shortValue(), (short) (i * 10) })).toArray();
        assertArrayEquals(new short[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatmapToShort() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2 });
        short[] result = stream.flatmapToShort(i -> new short[] { i.shortValue(), (short) (i * 10) }).toArray();
        assertArrayEquals(new short[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatMapToInt() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        int[] result = stream.flatMapToInt(i -> IntStream.of(new int[] { i, i * 10 })).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatmapToInt() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        int[] result = stream.flatmapToInt(i -> new int[] { i, i * 10 }).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatMapToLong() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2 });
        long[] result = stream.flatMapToLong(i -> LongStream.of(new long[] { i.longValue(), i * 10L })).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatmapToLong() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2 });
        long[] result = stream.flatmapToLong(i -> new long[] { i.longValue(), i * 10L }).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatMapToFloat() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2 });
        float[] result = stream.flatMapToFloat(i -> FloatStream.of(new float[] { i.floatValue(), i * 10.0f })).toArray();
        assertArrayEquals(new float[] { 1.0f, 10.0f, 2.0f, 20.0f }, result, 0.001f);
    }

    @Test
    public void testFlatmapToFloat() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2 });
        float[] result = stream.flatmapToFloat(i -> new float[] { i.floatValue(), i * 10.0f }).toArray();
        assertArrayEquals(new float[] { 1.0f, 10.0f, 2.0f, 20.0f }, result, 0.001f);
    }

    @Test
    public void testFlatMapToDouble() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2 });
        double[] result = stream.flatMapToDouble(i -> DoubleStream.of(new double[] { i.doubleValue(), i * 10.0 })).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapToDouble() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2 });
        double[] result = stream.flatmapToDouble(i -> new double[] { i.doubleValue(), i * 10.0 }).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0 }, result, 0.001);
    }

    @Test
    public void testSplit() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<List<Integer>> result = stream.split(3).toList();
        assertEquals(4, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(4, 5, 6), result.get(1));
        assertEquals(Arrays.asList(7, 8, 9), result.get(2));
        assertEquals(Arrays.asList(10), result.get(3));
    }

    @Test
    public void testSplitWithCollectionSupplier() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Set<Integer>> result = stream.split(3, IntFunctions.ofSet()).toList();
        assertEquals(4, result.size());
        assertTrue(result.get(0).contains(1));
        assertTrue(result.get(0).contains(2));
        assertTrue(result.get(0).contains(3));
    }

    @Test
    public void testSplitWithCollector() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<String> result = stream.split(3, Collectors.mapping(Fn.toStr(), Collectors.joining(","))).toList();
        assertEquals(4, result.size());
        assertEquals("1,2,3", result.get(0));
        assertEquals("10", result.get(3));
    }

    @Test
    public void testSplitByPredicate() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<List<Integer>> result = stream.split(x -> x % 2 == 0).toList();
        assertTrue(result.size() > 1);
    }

    @Test
    public void testSplitByPredicateWithCollectionSupplier() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Set<Integer>> result = stream.split(x -> x % 2 == 0, Suppliers.ofSet()).toList();
        assertTrue(result.size() > 1);
        assertTrue(result.get(0) instanceof Set);
    }

    @Test
    public void testSplitByPredicateWithCollector() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<String> result = stream.split(x -> x % 2 == 0, Collectors.mapping(Fn.toStr(), Collectors.joining(","))).toList();
        assertTrue(result.size() > 1);
    }

    @Test
    public void testSplit_Advance() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).split(2, IntFunctions.ofList());
        ObjIteratorEx<List<Integer>> iter = (ObjIteratorEx) stream.iterator();
        iter.advance(1);
        assertEquals(Arrays.asList(3, 4), iter.next());
        iter.advance(0);
        assertEquals(Arrays.asList(5), iter.next());
    }

    @Test
    public void testSplit_Count() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).split(2, IntFunctions.ofList());
        ObjIterator<List<Integer>> iter = stream.iterator();
        assertEquals(3, iter.count());
        iter.next();
        assertEquals(2, iter.count());
    }

    @Test
    public void testSplit_Next_After_Last() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).split(2, IntFunctions.ofList());
        ObjIterator<List<Integer>> iter = stream.iterator();
        assertEquals(Arrays.asList(1, 2), iter.next());
        assertEquals(Arrays.asList(3, 4), iter.next());
        assertEquals(Arrays.asList(5), iter.next());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testSplitAt() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<List<Integer>> result = stream.splitAt(5).map(s -> s.toList()).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.get(0));
        assertEquals(Arrays.asList(6, 7, 8, 9, 10), result.get(1));
    }

    @Test
    public void testSplitAtWithCollector() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<String> result = stream.splitAt(5, Collectors.mapping(String::valueOf, Collectors.joining(","))).toList();
        assertEquals(2, result.size());
        assertEquals("1,2,3,4,5", result.get(0));
        assertEquals("6,7,8,9,10", result.get(1));
    }

    @Test
    public void testSplitAtEdgeCases() {
        // splitAt at end
        List<Stream<Integer>> splits = Stream.of(integerArray).splitAt(10).toList();
        assertEquals(2, splits.size());
        assertEquals(10, splits.get(0).toList().size());
        assertTrue(splits.get(1).toList().isEmpty());
    }

    @Test
    public void testsliding() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<List<Integer>> result = stream.sliding(3, 2, IntFunctions.ofList()).toList();
        assertTrue(result.size() > 0);
        assertEquals(3, result.get(0).size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
    }

    @Test
    public void testSlidingWithCollector() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<String> result = stream.sliding(3, 2, Collectors.mapping(Fn.toStr(), Collectors.joining(","))).toList();
        assertTrue(result.size() > 0);
        assertEquals("1,2,3", result.get(0));
    }

    @Test
    public void testSlidingNoCollector() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<List<Integer>> result = stream.sliding(3, 1).toList();
        assertEquals(8, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(8, 9, 10), result.get(7));
    }

    @Test
    public void testSlidingFromArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        List<List<Integer>> result = Stream.of(arr).sliding(2, 1).toList();
        assertEquals(4, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(4, 5), result.get(3));
    }

    @Test
    public void testCheckSlidingWindowSize_validArgs() {
        // Valid window; should not throw
        List<List<Integer>> result = Stream.of(new Integer[] { 1, 2, 3, 4, 5 }).sliding(2, 1).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testCheckSlidingWindowSize_invalidWindowThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(new Integer[] { 1, 2, 3 }).sliding(0, 1).toList();
        });
    }

    @Test
    public void testDistinct() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 2, 3, 3, 3, 4 });
        List<Integer> result = stream.distinct().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testDistinctByKey() {
        String[] arr = { "apple", "avocado", "banana", "blueberry", "cherry" };
        List<String> result = Stream.of(arr).distinctBy(s -> s.charAt(0)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        List<Integer> result = stream.distinct().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLimit() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.limit(5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.limit(20).toList();
        assertEquals(10, result2.size());
    }

    @Test
    public void testLimit_sameSize() {
        Stream<Integer> stream = Stream.of(integerArray);
        // limit equal to size -> returns same stream
        List<Integer> result = stream.limit(10).toList();
        assertEquals(10, result.size());
    }

    @Test
    public void testLimit_zero() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.limit(0).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testSkip_fewerThanN() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        List<Integer> result = stream.skip(10).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testSkip_exactSize() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        List<Integer> result = stream.skip(3).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testSkip() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.skip(5).toList();
        assertEquals(Arrays.asList(6, 7, 8, 9, 10), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.skip(20).toList();
        assertTrue(result2.isEmpty());
    }

    // Additional tests for uncovered branches

    @Test
    public void testSkip_zeroElements() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.skip(0).toList();
        assertEquals(10, result.size());
    }

    @Test
    public void testSkip_emptyStream() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        List<Integer> result = stream.skip(5).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testTop() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.top(3).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(8) || result.contains(9) || result.contains(10));

        Stream<String> stream2 = Stream.of(stringArray);
        List<String> result2 = stream2.top(2, Comparator.comparing(String::length)).toList();
        assertEquals(2, result2.size());
        assertTrue(result2.contains("elderberry"));
    }

    @Test
    public void testTopFromArray() {
        Integer[] arr = { 5, 2, 8, 1, 9, 3 };
        Integer[] result = Stream.of(arr).top(3, Comparator.naturalOrder()).toArray(Integer[]::new);
        assertEquals(3, result.length);
    }

    @Test
    public void testTop_sizedGreaterThanStream() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        List<Integer> result = stream.top(10, Integer::compareTo).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testTop_zero() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.top(0).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testTop_withComparator_sorted() {
        // When stream is already sorted with same comparator, use fast path
        Integer[] sorted = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = Stream.of(sorted).sorted(Integer::compareTo);
        List<Integer> result = stream.top(2, Integer::compareTo).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testOnEach() {
        Stream<Integer> stream = Stream.of(integerArray);
        AtomicInteger sum = new AtomicInteger(0);
        List<Integer> result = stream.peek(sum::addAndGet).toList();
        assertEquals(10, result.size());
        assertEquals(55, sum.get());
    }

    @Test
    public void testOnEachFromArray() {
        List<Integer> sideEffect = new ArrayList<>();
        List<Integer> result = Stream.of(integerArray).onEach(sideEffect::add).toList();
        assertEquals(10, result.size());
        assertEquals(10, sideEffect.size());
    }

    @Test
    public void testForEach() {
        Stream<Integer> stream = Stream.of(integerArray);
        AtomicInteger sum = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);

        stream.forEach(sum::addAndGet, () -> completed.set(true));

        assertEquals(55, sum.get());
        assertTrue(completed.get());
    }

    @Test
    public void testForEachWithFlatMapper() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        List<String> result = new ArrayList<>();

        stream.forEach(s -> Arrays.asList(s.charAt(0), s.charAt(1)), (str, ch) -> result.add(str + ":" + ch));

        assertEquals(4, result.size());
        assertEquals("ab:a", result.get(0));
        assertEquals("ab:b", result.get(1));
        assertEquals("cd:c", result.get(2));
        assertEquals("cd:d", result.get(3));
    }

    @Test
    public void testForEachWithOnComplete() {
        List<String> result = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        Stream.of(stringArray).forEach(result::add, () -> completed.incrementAndGet());
        assertEquals(5, result.size());
        assertEquals(1, completed.get());
    }

    @Test
    public void testForEachWithDoubleFlatMapper() {
        List<String> result = new ArrayList<>();
        Stream.of(new Integer[] { 1, 2 }).forEach(x -> Arrays.asList("a" + x, "b" + x), s -> Arrays.asList(s + "1", s + "2"), (x, s, t) -> result.add(t));
        assertEquals(Arrays.asList("a11", "a12", "b11", "b12", "a21", "a22", "b21", "b22"), result);
    }

    @Test
    public void testForEach3() {
        Stream<String> stream = Stream.of(new String[] { "ab" });
        List<String> result = new ArrayList<>();

        stream.forEach(s -> Arrays.asList(s.charAt(0), s.charAt(1)), (ch) -> Arrays.asList(String.valueOf(ch).toUpperCase()),
                (str, ch, uc) -> result.add(str + ":" + ch + ":" + uc));

        assertEquals(2, result.size());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicInteger sum = new AtomicInteger(0);
        Stream.of(integerArray).acceptIfNotEmpty(s -> s.forEach(sum::addAndGet));
        assertEquals(55, sum.get());

        sum.set(0);
        Stream.of(new Integer[0]).acceptIfNotEmpty(s -> s.forEach(sum::addAndGet));
        assertEquals(0, sum.get());
    }

    @Test
    public void testForEach_withFlatMapper_nullIterable() {
        // Test null iterable returned from flatMapper
        Stream<String> stream = Stream.of(new String[] { "a", "b" });
        List<String> result = new ArrayList<>();
        stream.forEach(s -> null, (str, ch) -> result.add(str));
        assertEquals(0, result.size());
    }

    @Test
    public void testForEach_tripleNestedFlatMapper() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        List<String> result = new ArrayList<>();
        stream.forEach(s -> java.util.Arrays.asList(s.substring(0, 1), s.substring(1)), t -> java.util.Arrays.asList(t + "1", t + "2"),
                (orig, mid, end) -> result.add(orig + ":" + mid + ":" + end));
        assertEquals(8, result.size());
    }

    @Test
    public void testForEachPair() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<String> result = new ArrayList<>();

        stream.forEachPair(2, (a, b) -> result.add(a + "-" + b));

        assertEquals(5, result.size());
        assertEquals("1-2", result.get(0));
        assertEquals("3-4", result.get(1));
        assertEquals("9-10", result.get(4));
    }

    @Test
    public void testForEachPair_increment1() {
        // increment=1, windowSize=2: cursor stops when windowSize >= toIndex-cursor
        // [1,2,3,4,5] with increment=1: cursors 0,1,2,3 (4 iterations)
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3, 4, 5 });
        List<String> result = new ArrayList<>();
        stream.forEachPair(1, (a, b) -> result.add(a + "-" + b));
        assertEquals(4, result.size());
        assertEquals("1-2", result.get(0));
        assertEquals("4-5", result.get(3));
    }

    @Test
    public void testForEachPair_singleElement() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1 });
        List<String> result = new ArrayList<>();
        stream.forEachPair(1, (a, b) -> result.add(a + "-" + (b == null ? "null" : b)));
        assertEquals(1, result.size());
        assertEquals("1-null", result.get(0));
    }

    @Test
    public void testForEachTriple() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<String> result = new ArrayList<>();

        stream.forEachTriple(3, (a, b, c) -> result.add(a + "-" + b + "-" + c));

        assertTrue(result.size() > 0);
        assertEquals("1-2-3", result.get(0));
    }

    @Test
    public void testToArrayWithGenerator() {
        Stream<String> stream = Stream.of(stringArray);
        String[] result = stream.toArray(String[]::new);
        assertArrayEquals(stringArray, result);
    }

    @Test
    public void testToArray_booleanTrue() {
        // toArray(boolean closeStream) with closeStream=true
        Stream<Integer> stream = Stream.of(integerArray);
        // This is a protected method; test via public toArray(IntFunction)
        Object[] arr = stream.toArray(Object[]::new);
        assertEquals(10, arr.length);
    }

    @Test
    public void testToArray_preallocated_exact() {
        Stream<String> stream = Stream.of(stringArray);
        String[] arr = stream.toArray(String[]::new);
        assertArrayEquals(stringArray, arr);
    }

    @Test
    public void testToList() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.toList();
        assertEquals(Arrays.asList(stringArray), result);
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Stream<String> stream = Stream.of(stringArray);
        Stream<String> streamWithCloseHandler = stream.onClose(() -> closed.set(true));

        streamWithCloseHandler.toList();
        assertNotNull(streamWithCloseHandler);
    }

    @Test
    public void testSorted() {
        Integer[] unsorted = { 5, 3, 1, 4, 2 };
        List<Integer> result = Stream.of(unsorted).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        result = Stream.of(unsorted).sorted(Comparator.reverseOrder()).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);

        List<Integer> emptyResult = Stream.of(new Integer[0]).sorted().toList();
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void testToSet() {
        Stream<String> stream = Stream.of(stringArray);
        Set<String> result = stream.toSet();
        assertEquals(new HashSet<>(Arrays.asList(stringArray)), result);
    }

    @Test
    public void testToCollection() {
        Stream<String> stream = Stream.of(stringArray);
        LinkedList<String> result = stream.toCollection(LinkedList::new);
        assertEquals(new LinkedList<>(Arrays.asList(stringArray)), result);
    }

    @Test
    public void testToCollection_allElements() {
        // When toIndex - fromIndex == elements.length, uses Arrays.asList branch
        Integer[] all = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = Stream.of(all);
        List<Integer> result = stream.toCollection(ArrayList::new);
        assertEquals(5, result.size());
    }

    @Test
    public void testToCollection_largeSubsetAsList() {
        // When toIndex - fromIndex > 9 && result instanceof List, uses copyOfRange branch
        Integer[] arr = new Integer[15];
        for (int i = 0; i < 15; i++)
            arr[i] = i;
        Stream<Integer> stream = Stream.of(arr).skip(2); // skip 2 so it's a subrange
        List<Integer> result = stream.toCollection(ArrayList::new);
        assertEquals(13, result.size());
    }

    @Test
    public void testToCollection_smallSubset() {
        // When toIndex - fromIndex <= 9, uses element-by-element branch
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3, 4, 5 }).skip(1);
        java.util.LinkedList<Integer> result = stream.toCollection(java.util.LinkedList::new);
        assertEquals(4, result.size());
    }

    @Test
    public void testToMultiset() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 2, 3, 3, 3 });
        Multiset<Integer> result = stream.toMultiset();
        assertEquals(1, result.count(1));
        assertEquals(2, result.count(2));
        assertEquals(3, result.count(3));
    }

    @Test
    public void testToMultisetWithSupplier() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 2, 3, 3, 3 });
        Multiset<Integer> result = stream.toMultiset(Multiset::new);
        assertEquals(1, result.count(1));
        assertEquals(2, result.count(2));
        assertEquals(3, result.count(3));
    }

    @Test
    public void testToMultisetFromArray() {
        Integer[] arr = { 1, 2, 2, 3, 3, 3 };
        Multiset<Integer> result = Stream.of(arr).toMultiset(Multiset::new);
        assertEquals(1, result.getCount(1));
        assertEquals(2, result.getCount(2));
        assertEquals(3, result.getCount(3));
    }

    @Test
    public void testToMap() {
        Stream<String> stream = Stream.of(stringArray);
        Map<String, Integer> result = stream.toMap(s -> s, String::length, Integer::sum, HashMap::new);
        assertEquals(5, result.size());
        assertEquals(Integer.valueOf(5), result.get("apple"));
    }

    @Test
    public void testToMapWithSupplier() {
        Stream<String> stream = Stream.of(stringArray);
        Map<String, Integer> result = stream.toMap(s -> s, s -> s.length(), (a, b) -> a, java.util.LinkedHashMap::new);
        assertEquals(5, result.size());
        assertTrue(result instanceof java.util.LinkedHashMap);
    }

    @Test
    public void testToMultimap() {
        Stream<String> stream = Stream.of(stringArray);
        ListMultimap<Integer, String> result = stream.toMultimap(String::length, s -> s, Suppliers.ofListMultimap());
        assertTrue(result.containsKey(5));
        assertTrue(result.get(5).contains("apple"));
    }

    @Test
    public void testToMultimapWithSupplier() {
        String[] arr = { "a1", "a2", "b1" };
        ListMultimap<Character, String> result = Stream.of(arr).toMultimap(s -> s.charAt(0), s -> s);
        assertEquals(2, result.get('a').size());
        assertEquals(1, result.get('b').size());
    }

    @Test
    public void testFirst() {
        Stream<String> stream = Stream.of(stringArray);
        Optional<String> result = stream.first();
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());

        Stream<String> stream2 = Stream.of(emptyArray);
        Optional<String> result2 = stream2.first();
        assertFalse(result2.isPresent());
    }

    @Test
    public void testLast() {
        Stream<String> stream = Stream.of(stringArray);
        Optional<String> result = stream.last();
        assertTrue(result.isPresent());
        assertEquals("elderberry", result.get());

        Stream<String> stream2 = Stream.of(emptyArray);
        Optional<String> result2 = stream2.last();
        assertFalse(result2.isPresent());
    }

    @Test
    public void testElementAt() {
        Stream<String> stream = Stream.of(stringArray);
        Optional<String> result = stream.elementAt(2);
        assertTrue(result.isPresent());
        assertEquals("cherry", result.get());

        Stream<String> stream2 = Stream.of(stringArray);
        Optional<String> result2 = stream2.elementAt(10);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testOnlyOne() {
        Stream<String> stream = Stream.of(new String[] { "single" });
        Optional<String> result = stream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals("single", result.get());

        Stream<String> stream2 = Stream.of(emptyArray);
        Optional<String> result2 = stream2.onlyOne();
        assertFalse(result2.isPresent());

        Stream<String> stream3 = Stream.of(stringArray);
        assertThrows(TooManyElementsException.class, () -> stream3.onlyOne());
    }

    @Test
    public void testFoldLeft() {
        Stream<Integer> stream = Stream.of(integerArray);
        Optional<Integer> result = stream.foldLeft(Integer::sum);
        assertTrue(result.isPresent());
        assertEquals(55, result.get().intValue());

        Stream<Integer> stream2 = Stream.of(integerArray);
        Integer result2 = stream2.foldLeft(0, Integer::sum);
        assertEquals(55, result2.intValue());
    }

    @Test
    public void testFoldLeftEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        u.Optional<Integer> result = stream.foldLeft(Integer::sum);
        assertFalse(result.isPresent());

        Stream<Integer> stream2 = Stream.of(new Integer[0]);
        Integer result2 = stream2.foldLeft(0, Integer::sum);
        assertEquals(Integer.valueOf(0), result2);
    }

    @Test
    public void testFoldRight() {
        Stream<Integer> stream = Stream.of(integerArray);
        Optional<Integer> result = stream.foldRight(Integer::sum);
        assertTrue(result.isPresent());
        assertEquals(55, result.get().intValue());

        Stream<Integer> stream2 = Stream.of(integerArray);
        Integer result2 = stream2.foldRight(0, Integer::sum);
        assertEquals(55, result2.intValue());
    }

    @Test
    public void testFoldRightEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        u.Optional<Integer> result = stream.foldRight(Integer::sum);
        assertFalse(result.isPresent());

        Stream<Integer> stream2 = Stream.of(new Integer[0]);
        Integer result2 = stream2.foldRight(0, Integer::sum);
        assertEquals(Integer.valueOf(0), result2);
    }

    @Test
    public void testReduce() {
        Stream<Integer> stream = Stream.of(integerArray);
        Optional<Integer> result = stream.reduce(Integer::sum);
        assertTrue(result.isPresent());
        assertEquals(55, result.get().intValue());

        Stream<Integer> stream2 = Stream.of(integerArray);
        Integer result2 = stream2.reduce(0, Integer::sum, Integer::sum);
        assertEquals(55, result2.intValue());
    }

    @Test
    public void testReduceEmpty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        u.Optional<Integer> result = stream.reduce(Integer::sum);
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduceWithIdentity() {
        Integer sum = Stream.of(integerArray).reduce(0, Integer::sum);
        assertEquals(Integer.valueOf(55), sum);

        Integer emptyResult = Stream.of(new Integer[0]).reduce(100, Integer::sum);
        assertEquals(Integer.valueOf(100), emptyResult);
    }

    @Test
    public void testCollect() {
        Stream<String> stream = Stream.of(stringArray);
        String result = stream.collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2))
                .stream()
                .map(Fn.toStr())
                .collect(Collectors.joining(","));
        assertTrue(result.contains("apple"));

        Stream<String> stream2 = Stream.of(stringArray);
        String result2 = stream2.collect(Collectors.joining(","));
        assertTrue(result2.contains("apple"));
    }

    @Test
    public void testCollectWithCollector() {
        String result = Stream.of(stringArray).collect(Collectors.joining(", "));
        assertEquals("apple, banana, cherry, date, elderberry", result);
    }

    @Test
    public void testTakeLast() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.takeLast(3).toList();
        assertEquals(Arrays.asList(8, 9, 10), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.takeLast(20).toList();
        assertEquals(10, result2.size());
    }

    @Test
    public void testTakeLastFromArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertEquals(Arrays.asList(3, 4, 5), Stream.of(arr).takeLast(3).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Stream.of(arr).takeLast(10).toList());
        assertTrue(Stream.of(arr).takeLast(0).toList().isEmpty());
    }

    @Test
    public void testSkipLast() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.skipLast(3).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.skipLast(20).toList();
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testSkipLastFromArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertEquals(Arrays.asList(1, 2), Stream.of(arr).skipLast(3).toList());
        assertTrue(Stream.of(arr).skipLast(10).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Stream.of(arr).skipLast(0).toList());
    }

    @Test
    public void testMin() {
        Stream<Integer> stream = Stream.of(integerArray);
        Optional<Integer> result = stream.min(Integer::compareTo);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());

        Stream<Integer> stream2 = Stream.of(new Integer[0]);
        Optional<Integer> result2 = stream2.min(Integer::compareTo);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testMin_empty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        Optional<Integer> result = stream.min(Integer::compareTo);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax() {
        Stream<Integer> stream = Stream.of(integerArray);
        Optional<Integer> result = stream.max(Integer::compareTo);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(10), result.get());

        Stream<Integer> stream2 = Stream.of(new Integer[0]);
        Optional<Integer> result2 = stream2.max(Integer::compareTo);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testMax_empty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        Optional<Integer> result = stream.max(Integer::compareTo);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax_withNullComparator() {
        Stream<String> stream = Stream.of(new String[] { "apple", "banana", "cherry" });
        // with non-null comparator
        Optional<String> result = stream.max(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals("cherry", result.get());
    }

    @Test
    public void testMaxAll() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 3, 3, 2 });
        List<Integer> result = stream.maxAll(Integer::compareTo);
        assertEquals(Arrays.asList(3, 3), result);
    }

    @Test
    public void testMaxAll_empty() {
        Stream<Integer> stream = Stream.of(new Integer[0]);
        List<Integer> result = stream.maxAll(Integer::compareTo);
        assertEquals(0, result.size());
    }

    @Test
    public void testMaxAll_allEqual() {
        Stream<Integer> stream = Stream.of(new Integer[] { 5, 5, 5 });
        List<Integer> result = stream.maxAll(Integer::compareTo);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
    }

    @Test
    public void testMaxAll_someEqual() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3, 3 });
        List<Integer> result = stream.maxAll(Integer::compareTo);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get(0));
    }

    @Test
    public void testKthLargest() {
        Stream<Integer> stream = Stream.of(integerArray);
        Optional<Integer> result = stream.kthLargest(3, Integer::compareTo);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(8), result.get());

        Stream<Integer> stream2 = Stream.of(integerArray);
        Optional<Integer> result2 = stream2.kthLargest(20, Integer::compareTo);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testKthLargest_notFound() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        Optional<Integer> result = stream.kthLargest(5, Integer::compareTo);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_found() {
        Stream<Integer> stream = Stream.of(new Integer[] { 5, 1, 3, 2, 4 });
        Optional<Integer> result = stream.kthLargest(2, Integer::compareTo);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get());
    }

    @Test
    public void testCountFromArray() {
        assertEquals(10, Stream.of(integerArray).count());
        assertEquals(0, Stream.of(new Integer[0]).count());
    }

    @Test
    public void testConstructors() {
        Stream<String> stream1 = Stream.of(stringArray);
        assertEquals(5, stream1.count());

        Stream<String> stream2 = Stream.of(stringArray, 1, 4);
        assertEquals(3, stream2.count());

        Stream<String> stream3 = Stream.of(emptyArray);
        assertEquals(0, stream3.count());
    }

    @Test
    public void testCount() {
        Stream<String> stream = Stream.of(stringArray);
        long count = stream.count();
        assertEquals(5, count);

        Stream<String> stream2 = Stream.of(emptyArray);
        long count2 = stream2.count();
        assertEquals(0, count2);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Stream<String> stream = Stream.of(stringArray);
        u.Optional<Long> result = stream.applyIfNotEmpty(s -> s.count());
        assertTrue(result.isPresent());
        assertEquals(Long.valueOf(5), result.get());

        Stream<String> stream2 = Stream.of(emptyArray);
        u.Optional<Long> result2 = stream2.applyIfNotEmpty(s -> s.count());
        assertFalse(result2.isPresent());
    }

    @Test
    public void testAnyMatch() {
        Stream<Integer> stream = Stream.of(integerArray);
        assertTrue(stream.anyMatch(x -> x > 5));

        Stream<Integer> stream2 = Stream.of(integerArray);
        assertFalse(stream2.anyMatch(x -> x > 20));
    }

    @Test
    public void testAllMatch() {
        Stream<Integer> stream = Stream.of(integerArray);
        assertTrue(stream.allMatch(x -> x > 0));

        Stream<Integer> stream2 = Stream.of(integerArray);
        assertFalse(stream2.allMatch(x -> x > 5));
    }

    @Test
    public void testNoneMatch() {
        Stream<Integer> stream = Stream.of(integerArray);
        assertTrue(stream.noneMatch(x -> x > 20));

        Stream<Integer> stream2 = Stream.of(integerArray);
        assertFalse(stream2.noneMatch(x -> x > 5));
    }

    @Test
    public void testNMatch() {
        Stream<Integer> stream = Stream.of(integerArray);
        assertTrue(stream.hasMatchCountBetween(5, 5, x -> x <= 5));

        Stream<Integer> stream2 = Stream.of(integerArray);
        assertTrue(stream2.hasMatchCountBetween(1, 3, x -> x > 8));
    }

    @Test
    public void testHasMatchCountBetween_withinRange() {
        Stream<Integer> stream = Stream.of(integerArray);
        assertTrue(stream.hasMatchCountBetween(3, 7, x -> x <= 5));
    }

    @Test
    public void testHasMatchCountBetween_exceedsAtMost() {
        Stream<Integer> stream = Stream.of(integerArray);
        // 10 elements all > 0, so count would be 10 > atMost=5
        assertFalse(stream.hasMatchCountBetween(1, 5, x -> x > 0));
    }

    @Test
    public void testHasMatchCountBetween_belowAtLeast() {
        Stream<Integer> stream = Stream.of(integerArray);
        assertFalse(stream.hasMatchCountBetween(5, 10, x -> x > 8));
    }

    @Test
    public void testFindFirst() {
        Stream<Integer> stream = Stream.of(integerArray);
        Optional<Integer> result = stream.findFirst(x -> x > 5);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(6), result.get());

        Stream<Integer> stream2 = Stream.of(integerArray);
        Optional<Integer> result2 = stream2.findFirst(x -> x > 20);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testFindLast() {
        Stream<Integer> stream = Stream.of(integerArray);
        Optional<Integer> result = stream.findLast(x -> x < 8);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(7), result.get());

        Stream<Integer> stream2 = Stream.of(integerArray);
        Optional<Integer> result2 = stream2.findLast(x -> x > 20);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testCycled() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        List<Integer> result = stream.cycled().limit(10).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1, 2, 3, 1), result);
    }

    @Test
    public void testCycledWithRounds() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        List<Integer> result = stream.cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
    }

    @Test
    public void testCycled_oneRound() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        List<Integer> result = stream.cycled(1).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testCycled_zeroRounds() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        List<Integer> result = stream.cycled(0).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testCycled_multipleRounds() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        List<Integer> result = stream.cycled(3).toList();
        assertEquals(9, result.size());
    }

    @Test
    public void testBuffered() {
        Stream<Integer> stream = Stream.of(integerArray);
        Stream<Integer> buffered = stream.buffered(5);
        assertEquals(10, buffered.count());
    }

    @Test
    public void testAppendIfEmpty() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.appendIfEmpty(Arrays.asList("x", "y")).toList();
        assertEquals(5, result.size());
        assertEquals("apple", result.get(0));

        Stream<String> stream2 = Stream.of(emptyArray);
        List<String> result2 = stream2.appendIfEmpty(Arrays.asList("x", "y")).toList();
        assertEquals(2, result2.size());
        assertEquals("x", result2.get(0));
    }

    @Test
    public void testAppendIfEmptyWithSupplier() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.appendIfEmpty(() -> Stream.of("x", "y")).toList();
        assertEquals(5, result.size());

        Stream<String> stream2 = Stream.of(emptyArray);
        List<String> result2 = stream2.appendIfEmpty(() -> Stream.of("x", "y")).toList();
        assertEquals(2, result2.size());
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertEquals(5, result.size());
        assertFalse(actionExecuted.get());

        Stream<String> stream2 = Stream.of(emptyArray);
        List<String> result2 = stream2.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertEquals(0, result2.size());
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testIteratorEx() {
        java.util.Iterator<Integer> iter = Stream.of(integerArray).iterator();
        assertTrue(iter.hasNext());
        assertEquals(Integer.valueOf(1), iter.next());
    }

    @Test
    public void testParallel() {
        Stream<Integer> stream = Stream.of(integerArray);
        Stream<Integer> parallelStream = stream.parallel();
        assertTrue(parallelStream.isParallel());
        Set<Integer> result = parallelStream.toSet();
        assertEquals(10, result.size());
    }

    @Test
    public void testToJdkStream() {
        Stream<String> stream = Stream.of(stringArray);
        java.util.stream.Stream<String> jdkStream = stream.toJdkStream();
        List<String> result = jdkStream.collect(Collectors.toList());
        assertEquals(Arrays.asList(stringArray), result);
    }

    @Test
    public void testToJdkStreamFromArray() {
        java.util.stream.Stream<Integer> jdkStream = Stream.of(integerArray).toJdkStream();
        List<Integer> result = jdkStream.collect(Collectors.toList());
        assertEquals(10, result.size());
    }

    @Test
    public void testToJdkStream_notParallel() {
        Stream<Integer> stream = Stream.of(integerArray);
        java.util.stream.Stream<Integer> jdk = stream.toJdkStream();
        assertNotNull(jdk);
        assertFalse(jdk.isParallel());
        assertEquals(10, jdk.count());
    }

    @Test
    public void testToJdkStream_parallel() {
        Stream<Integer> stream = Stream.of(integerArray).parallel();
        java.util.stream.Stream<Integer> jdk = stream.toJdkStream();
        assertNotNull(jdk);
        assertTrue(jdk.isParallel());
        assertEquals(10, jdk.count());
    }

    @Test
    public void testGroupTo() {
        Map<Integer, List<Integer>> result = Stream.of(integerArray).groupTo(x -> x % 2);
        assertEquals(2, result.size());
        assertEquals(5, result.get(0).size());
        assertEquals(5, result.get(1).size());
    }
}
