package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class ArrayStream100Test extends TestBase {

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
    public void testConstructors() {
        Stream<String> stream1 = Stream.of(stringArray);
        assertEquals(5, stream1.count());

        Stream<String> stream2 = Stream.of(stringArray, 1, 4);
        assertEquals(3, stream2.count());

        Stream<String> stream3 = Stream.of(emptyArray);
        assertEquals(0, stream3.count());
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
    public void testMap() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.map(String::toUpperCase).toList();
        assertEquals(Arrays.asList("APPLE", "BANANA", "CHERRY", "DATE", "ELDERBERRY"), result);

        Stream<String> stream2 = Stream.of(stringArray);
        List<Integer> result2 = stream2.map(String::length).toList();
        assertEquals(Arrays.asList(5, 6, 6, 4, 10), result2);
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
    public void testSlidingMapTriple() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<String> result = stream.slidingMap(1, false, (a, b, c) -> a + "-" + b + "-" + c).toList();
        assertEquals(8, result.size());
        assertEquals("1-2-3", result.get(0));
        assertEquals("8-9-10", result.get(7));
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
    public void testMapToByte() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        byte[] result = stream.mapToByte(Integer::byteValue).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToShort() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        short[] result = stream.mapToShort(Integer::shortValue).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToInt() {
        Stream<String> stream = Stream.of(stringArray);
        int[] result = stream.mapToInt(String::length).toArray();
        assertArrayEquals(new int[] { 5, 6, 6, 4, 10 }, result);
    }

    @Test
    public void testMapToLong() {
        Stream<Integer> stream = Stream.of(integerArray);
        long[] result = stream.mapToLong(Integer::longValue).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, result);
    }

    @Test
    public void testMapToFloat() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 3 });
        float[] result = stream.mapToFloat(Integer::floatValue).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testMapToDouble() {
        Stream<Integer> stream = Stream.of(integerArray);
        double[] result = stream.mapToDouble(Integer::doubleValue).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 }, result, 0.001);
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
    public void testFlattMap() {
        Stream<String> stream = Stream.of(new String[] { "ab", "cd" });
        List<Character> result = stream.flattmap(s -> new Character[] { s.charAt(0), s.charAt(1) }).toList();
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
    public void testDistinct() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 2, 3, 3, 3, 4 });
        List<Integer> result = stream.distinct().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
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
    public void testSkip() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.skip(5).toList();
        assertEquals(Arrays.asList(6, 7, 8, 9, 10), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.skip(20).toList();
        assertTrue(result2.isEmpty());
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
    public void testOnEach() {
        Stream<Integer> stream = Stream.of(integerArray);
        AtomicInteger sum = new AtomicInteger(0);
        List<Integer> result = stream.onEach(sum::addAndGet).toList();
        assertEquals(10, result.size());
        assertEquals(55, sum.get());
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
    public void testToList() {
        Stream<String> stream = Stream.of(stringArray);
        List<String> result = stream.toList();
        assertEquals(Arrays.asList(stringArray), result);
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
    public void testToMultiset() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 2, 2, 3, 3, 3 });
        Multiset<Integer> result = stream.toMultiset();
        assertEquals(1, result.count(1));
        assertEquals(2, result.count(2));
        assertEquals(3, result.count(3));
    }

    @Test
    public void testToMap() {
        Stream<String> stream = Stream.of(stringArray);
        Map<String, Integer> result = stream.toMap(s -> s, String::length, Integer::sum, HashMap::new);
        assertEquals(5, result.size());
        assertEquals(Integer.valueOf(5), result.get("apple"));
    }

    @Test
    public void testToMultimap() {
        Stream<String> stream = Stream.of(stringArray);
        ListMultimap<Integer, String> result = stream.toMultimap(String::length, s -> s, Suppliers.ofListMultimap());
        assertTrue(result.containsKey(5));
        assertTrue(result.get(5).contains("apple"));
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
    public void testTakeLast() {
        Stream<Integer> stream = Stream.of(integerArray);
        List<Integer> result = stream.takeLast(3).toList();
        assertEquals(Arrays.asList(8, 9, 10), result);

        Stream<Integer> stream2 = Stream.of(integerArray);
        List<Integer> result2 = stream2.takeLast(20).toList();
        assertEquals(10, result2.size());
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
    public void testMaxAll() {
        Stream<Integer> stream = Stream.of(new Integer[] { 1, 3, 3, 2 });
        List<Integer> result = stream.maxAll(Integer::compareTo);
        assertEquals(Arrays.asList(3, 3), result);
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
    public void testCount() {
        Stream<String> stream = Stream.of(stringArray);
        long count = stream.count();
        assertEquals(5, count);

        Stream<String> stream2 = Stream.of(emptyArray);
        long count2 = stream2.count();
        assertEquals(0, count2);
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
        assertTrue(stream.nMatch(5, 5, x -> x <= 5));

        Stream<Integer> stream2 = Stream.of(integerArray);
        assertTrue(stream2.nMatch(1, 3, x -> x > 8));
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
    public void testToJdkStream() {
        Stream<String> stream = Stream.of(stringArray);
        java.util.stream.Stream<String> jdkStream = stream.toJdkStream();
        List<String> result = jdkStream.collect(Collectors.toList());
        assertEquals(Arrays.asList(stringArray), result);
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Stream<String> stream = Stream.of(stringArray);
        Stream<String> streamWithCloseHandler = stream.onClose(() -> closed.set(true));

        streamWithCloseHandler.toList();
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
}
