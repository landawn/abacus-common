package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u;

@Tag("2025")
public class Stream2025Test extends TestBase {

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path tempDir;

    @Test
    public void testEmpty() {
        Stream<Integer> stream = Stream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testJust() {
        Stream<String> stream = Stream.just("hello");
        assertEquals("hello", stream.first().get());
    }

    @Test
    public void testOfNullable() {
        Stream<String> stream1 = Stream.ofNullable("hello");
        assertEquals("hello", stream1.first().get());

        Stream<String> stream2 = Stream.ofNullable(null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfVarargs() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfArray() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = Stream.of(array);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testOfArrayRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = Stream.of(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), stream.toList());
    }

    @Test
    public void testOfCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Stream<String> stream = Stream.of(list);
        assertEquals(Arrays.asList("a", "b", "c"), stream.toList());
    }

    @Test
    public void testOfCollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Stream<String> stream = Stream.of(list, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), stream.toList());
    }

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<Map.Entry<String, Integer>> stream = Stream.of(map);
        assertEquals(2, stream.count());
    }

    @Test
    public void testOfKeys() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<String> stream = Stream.ofKeys(map);
        assertEquals(Arrays.asList("a", "b"), stream.toList());
    }

    @Test
    public void testOfValues() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<Integer> stream = Stream.ofValues(map);
        assertEquals(Arrays.asList(1, 2), stream.toList());
    }

    @Test
    public void testOfIterator() {
        Iterator<Integer> iterator = Arrays.asList(1, 2, 3).iterator();
        Stream<Integer> stream = Stream.of(iterator);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfIterable() {
        Iterable<Integer> iterable = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = Stream.of(iterable);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = Arrays.asList(1, 2, 3).stream();
        Stream<Integer> stream = Stream.from(jdkStream);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfOptional() {
        Stream<Integer> stream1 = Stream.of(u.Optional.of(42));
        assertEquals(Arrays.asList(42), stream1.toList());

        Stream<Integer> stream2 = Stream.of(u.Optional.<Integer> empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfJdkOptional() {
        Stream<Integer> stream1 = Stream.of(java.util.Optional.of(42));
        assertEquals(Arrays.asList(42), stream1.toList());

        Stream<Integer> stream2 = Stream.of(java.util.Optional.<Integer> empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testRange() {
        Stream<Integer> stream = Stream.range(1, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testRangeWithStep() {
        Stream<Integer> stream = Stream.range(0, 10, 2);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), stream.toList());
    }

    @Test
    public void testRangeClosed() {
        Stream<Integer> stream = Stream.rangeClosed(1, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testRangeClosedWithStep() {
        Stream<Integer> stream = Stream.rangeClosed(0, 10, 2);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8, 10), stream.toList());
    }

    @Test
    public void testRepeat() {
        Stream<String> stream = Stream.repeat("a", 3);
        assertEquals(Arrays.asList("a", "a", "a"), stream.toList());
    }

    @Test
    public void testRepeatZero() {
        Stream<String> stream = Stream.repeat("a", 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testIterate() {
        Stream<Integer> stream = Stream.iterate(1, n -> n < 5, n -> n + 1);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> stream = Stream.generate(() -> counter.incrementAndGet()).limit(3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testConcat() {
        Stream<Integer> stream = Stream.concat(Stream.of(1, 2), Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testConcatVarargs() {
        Stream<Integer> stream = Stream.concat(Stream.of(1), Stream.of(2), Stream.of(3));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testZip() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), stream.toList());
    }

    @Test
    public void testZipWithDifferentLengths() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b"), stream.toList());
    }

    @Test
    public void testZipWithDefaultValues() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), 0, "", (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "0c"), stream.toList());
    }

    @Test
    public void testMerge() {
        Stream<Integer> stream = Stream.merge(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6),
                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), stream.toList());
    }

    @Test
    public void testFilter() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).filter(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4), stream.toList());
    }

    @Test
    public void testFilterEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().filter(n -> n % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFilterNoneMatch() {
        Stream<Integer> stream = Stream.of(1, 3, 5).filter(n -> n % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFilterAllMatch() {
        Stream<Integer> stream = Stream.of(2, 4, 6).filter(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4, 6), stream.toList());
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).filter(n -> n % 2 == 0, dropped::add);
        assertEquals(Arrays.asList(2, 4), stream.toList());
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testTakeWhile() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).takeWhile(n -> n < 4);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testTakeWhileEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().takeWhile(n -> n < 4);
        assertEquals(0, stream.count());
    }

    @Test
    public void testTakeWhileFirstFalse() {
        Stream<Integer> stream = Stream.of(5, 6, 7).takeWhile(n -> n < 4);
        assertEquals(0, stream.count());
    }

    @Test
    public void testTakeWhileAllTrue() {
        Stream<Integer> stream = Stream.of(1, 2, 3).takeWhile(n -> n < 10);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testDropWhile() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).dropWhile(n -> n < 3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testDropWhileEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().dropWhile(n -> n < 3);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhileNoneDrop() {
        Stream<Integer> stream = Stream.of(5, 6, 7).dropWhile(n -> n < 3);
        assertEquals(Arrays.asList(5, 6, 7), stream.toList());
    }

    @Test
    public void testDropWhileAllDrop() {
        Stream<Integer> stream = Stream.of(1, 2, 3).dropWhile(n -> n < 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).dropWhile(n -> n < 3, dropped::add);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testSkipUntil() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skipUntil(n -> n >= 3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testMap() {
        Stream<String> stream = Stream.of(1, 2, 3).map(n -> "num" + n);
        assertEquals(Arrays.asList("num1", "num2", "num3"), stream.toList());
    }

    @Test
    public void testMapEmpty() {
        Stream<String> stream = Stream.<Integer> empty().map(n -> "num" + n);
        assertEquals(0, stream.count());
    }

    @Test
    public void testMapToInt() {
        IntStream intStream = Stream.of("a", "bb", "ccc").mapToInt(String::length);
        assertArrayEquals(new int[] { 1, 2, 3 }, intStream.toArray());
    }

    @Test
    public void testMapToLong() {
        LongStream longStream = Stream.of(1, 2, 3).mapToLong(n -> n * 10L);
        assertArrayEquals(new long[] { 10, 20, 30 }, longStream.toArray());
    }

    @Test
    public void testMapToDouble() {
        DoubleStream doubleStream = Stream.of(1, 2, 3).mapToDouble(n -> n * 1.5);
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testMapIfNotNull() {
        Stream<String> stream = Stream.of(1, null, 3).mapIfNotNull(n -> "num" + n);
        assertEquals(Arrays.asList("num1", "num3"), stream.toList());
    }

    @Test
    public void testMapFirst() {
        Stream<Integer> stream = Stream.of(1, 2, 3).mapFirst(n -> n * 10);
        assertEquals(Arrays.asList(10, 2, 3), stream.toList());
    }

    @Test
    public void testMapFirstEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().mapFirst(n -> n * 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testMapLast() {
        Stream<Integer> stream = Stream.of(1, 2, 3).mapLast(n -> n * 10);
        assertEquals(Arrays.asList(1, 2, 30), stream.toList());
    }

    @Test
    public void testMapLastEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().mapLast(n -> n * 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatMap(n -> Stream.of(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), stream.toList());
    }

    @Test
    public void testFlatMapEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().flatMap(n -> Stream.of(n, n * 10));
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatMapReturnsEmpty() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatMap(n -> Stream.<Integer> empty());
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatmapCollection() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatmap(n -> Arrays.asList(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), stream.toList());
    }

    @Test
    public void testFlattmapArray() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flattmap(n -> new Integer[] { n, n * 10 });
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), stream.toList());
    }

    @Test
    public void testFlatMapToInt() {
        IntStream intStream = Stream.of("a", "bb", "ccc").flatMapToInt(s -> IntStream.of(s.length(), s.length() * 2));
        assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6 }, intStream.toArray());
    }

    @Test
    public void testFlatMapToLong() {
        LongStream longStream = Stream.of(1, 2, 3).flatMapToLong(n -> LongStream.of(n, n * 10L));
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, longStream.toArray());
    }

    @Test
    public void testFlatMapToDouble() {
        DoubleStream doubleStream = Stream.of(1, 2, 3).flatMapToDouble(n -> DoubleStream.of(n, n * 1.5));
        assertArrayEquals(new double[] { 1, 1.5, 2, 3.0, 3, 4.5 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testDistinct() {
        Stream<Integer> stream = Stream.of(1, 2, 2, 3, 3, 3).distinct();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testDistinctEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().distinct();
        assertEquals(0, stream.count());
    }

    @Test
    public void testDistinctNoDuplicates() {
        Stream<Integer> stream = Stream.of(1, 2, 3).distinct();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testDistinctAllDuplicates() {
        Stream<Integer> stream = Stream.of(1, 1, 1).distinct();
        assertEquals(Arrays.asList(1), stream.toList());
    }

    @Test
    public void testDistinctBy() {
        Stream<String> stream = Stream.of("a", "bb", "c", "dd").distinctBy(String::length);
        assertEquals(Arrays.asList("a", "bb"), stream.toList());
    }

    @Test
    public void testDistinctByWithMerge() {
        Stream<String> stream = Stream.of("a", "bb", "c", "dd").distinctBy(String::length, (s1, s2) -> s1 + s2);
        assertEquals(Arrays.asList("ac", "bbdd"), stream.toList());
    }

    @Test
    public void testSorted() {
        Stream<Integer> stream = Stream.of(3, 1, 2).sorted();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSortedEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().sorted();
        assertEquals(0, stream.count());
    }

    @Test
    public void testSortedAlreadySorted() {
        Stream<Integer> stream = Stream.of(1, 2, 3).sorted();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSortedComparator() {
        Stream<Integer> stream = Stream.of(1, 2, 3).sorted(Comparator.reverseOrder());
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testSortedBy() {
        Stream<String> stream = Stream.of("aaa", "b", "cc").sortedBy(String::length);
        assertEquals(Arrays.asList("b", "cc", "aaa"), stream.toList());
    }

    @Test
    public void testSortedByInt() {
        Stream<String> stream = Stream.of("aaa", "b", "cc").sortedByInt(String::length);
        assertEquals(Arrays.asList("b", "cc", "aaa"), stream.toList());
    }

    @Test
    public void testReverseSorted() {
        Stream<Integer> stream = Stream.of(1, 2, 3).reverseSorted();
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testReverseSortedComparator() {
        Stream<String> stream = Stream.of("a", "bbb", "cc").reverseSorted(Comparator.comparing(String::length));
        assertEquals(Arrays.asList("bbb", "cc", "a"), stream.toList());
    }

    @Test
    public void testReversed() {
        Stream<Integer> stream = Stream.of(1, 2, 3).reversed();
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testReversedEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().reversed();
        assertEquals(0, stream.count());
    }

    @Test
    public void testReversedSingleElement() {
        Stream<Integer> stream = Stream.of(42).reversed();
        assertEquals(Arrays.asList(42), stream.toList());
    }

    @Test
    public void testRotatedPositive() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).rotated(2);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), stream.toList());
    }

    @Test
    public void testRotatedNegative() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).rotated(-2);
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), stream.toList());
    }

    @Test
    public void testRotatedZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).rotated(0);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testRotatedGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).rotated(5);
        assertHaveSameElements(Arrays.asList(3, 1, 2), stream.toList());
    }

    @Test
    public void testShuffled() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> shuffled = Stream.of(original).shuffled().toList();
        assertEquals(5, shuffled.size());
        assertTrue(shuffled.containsAll(original));
    }

    @Test
    public void testShuffledWithRandom() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        Random random = new Random(12345);
        List<Integer> shuffled1 = Stream.of(original).shuffled(new Random(12345)).toList();
        List<Integer> shuffled2 = Stream.of(original).shuffled(new Random(12345)).toList();
        assertEquals(shuffled1, shuffled2);
    }

    @Test
    public void testIndexed() {
        Stream<Indexed<String>> stream = Stream.of("a", "b", "c").indexed();
        List<Indexed<String>> list = stream.toList();
        assertEquals(0, list.get(0).index());
        assertEquals("a", list.get(0).value());
        assertEquals(1, list.get(1).index());
        assertEquals("b", list.get(1).value());
        assertEquals(2, list.get(2).index());
        assertEquals("c", list.get(2).value());
    }

    @Test
    public void testSkip() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skip(2);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testSkipZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).skip(0);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSkipGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).skip(10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testLimit() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).limit(3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testLimitZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).limit(0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testLimitGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).limit(10);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testStep() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5, 6).step(2);
        assertEquals(Arrays.asList(1, 3, 5), stream.toList());
    }

    @Test
    public void testStepOne() {
        Stream<Integer> stream = Stream.of(1, 2, 3).step(1);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSkipLast() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skipLast(2);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testTakeLast() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).takeLast(2);
        assertEquals(Arrays.asList(4, 5), stream.toList());
    }

    @Test
    public void testLastN() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).last(3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testPeek() {
        List<Integer> peeked = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).peek(peeked::add);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Arrays.asList(1, 2, 3), peeked);
    }

    @Test
    public void testOnEach() {
        List<Integer> processed = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).onEach(processed::add);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Arrays.asList(1, 2, 3), processed);
    }

    @Test
    public void testPeekFirst() {
        AtomicReference<Integer> first = new AtomicReference<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).peekFirst(first::set);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Integer.valueOf(1), first.get());
    }

    @Test
    public void testPeekLast() {
        AtomicReference<Integer> last = new AtomicReference<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).peekLast(last::set);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Integer.valueOf(3), last.get());
    }

    @Test
    public void testOnFirst() {
        AtomicReference<Integer> first = new AtomicReference<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).onFirst(first::set);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Integer.valueOf(1), first.get());
    }

    @Test
    public void testOnLast() {
        AtomicReference<Integer> last = new AtomicReference<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).onLast(last::set);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Integer.valueOf(3), last.get());
    }

    @Test
    public void testPrependStream() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(Stream.of(1, 2));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testPrependVarargs() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(1, 2);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testPrependCollection() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testPrependOptional() {
        Stream<Integer> stream = Stream.of(2, 3).prepend(u.Optional.of(1));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testAppendStream() {
        Stream<Integer> stream = Stream.of(1, 2).append(Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendVarargs() {
        Stream<Integer> stream = Stream.of(1, 2).append(3, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendCollection() {
        Stream<Integer> stream = Stream.of(1, 2).append(Arrays.asList(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendOptional() {
        Stream<Integer> stream = Stream.of(1, 2).append(u.Optional.of(3));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testAppendIfEmpty() {
        Stream<Integer> stream1 = Stream.<Integer> empty().appendIfEmpty(() -> Stream.of(1, 2));
        assertEquals(Arrays.asList(1, 2), stream1.toList());

        Stream<Integer> stream2 = Stream.of(1, 2).appendIfEmpty(() -> Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2), stream2.toList());
    }

    @Test
    public void testIntersection() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).intersection(Arrays.asList(2, 3, 5));
        assertEquals(Arrays.asList(2, 3), stream.toList());
    }

    @Test
    public void testIntersectionEmpty() {
        Stream<Integer> stream = Stream.of(1, 2, 3).intersection(Arrays.asList(4, 5, 6));
        assertEquals(0, stream.count());
    }

    @Test
    public void testDifference() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).difference(Arrays.asList(2, 3, 5));
        assertEquals(Arrays.asList(1, 4), stream.toList());
    }

    @Test
    public void testDifferenceNone() {
        Stream<Integer> stream = Stream.of(1, 2, 3).difference(Arrays.asList(4, 5, 6));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSymmetricDifference() {
        Stream<Integer> stream = Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(2, 3, 4));
        List<Integer> result = stream.toList();
        assertTrue(result.containsAll(Arrays.asList(1, 4)));
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupBy() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd", "eee").groupBy(String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb", "dd"), map.get(2));
        assertEquals(Arrays.asList("eee"), map.get(3));
    }

    @Test
    public void testGroupByEmpty() {
        Map<Integer, List<String>> map = Stream.<String> empty().groupBy(String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGroupByWithValueMapper() {
        Map<Integer, List<Integer>> map = Stream.of("a", "bb", "ccc").groupBy(String::length, String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList(1), map.get(1));
        assertEquals(Arrays.asList(2), map.get(2));
        assertEquals(Arrays.asList(3), map.get(3));
    }

    @Test
    public void testGroupByWithCollector() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd").groupBy(String::length, Collectors.counting()).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Long.valueOf(2), map.get(1));
        assertEquals(Long.valueOf(2), map.get(2));
    }

    @Test
    public void testGroupByToEntry() {
        List<Map.Entry<Integer, List<String>>> list = Stream.of("a", "bb", "c", "dd").groupByToEntry(String::length).toList();
        assertEquals(2, list.size());
    }

    @Test
    public void testPartitionBy() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4, 5).partitionBy(n -> n % 2 == 0).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionByEmpty() {
        Map<Boolean, List<Integer>> map = Stream.<Integer> empty().partitionBy(n -> n % 2 == 0).toMap(e -> e.getKey(), e -> e.getValue());
        assertTrue(map.get(true).isEmpty());
        assertTrue(map.get(false).isEmpty());
    }

    @Test
    public void testPartitionByWithCollector() {
        Map<Boolean, Long> map = Stream.of(1, 2, 3, 4, 5).partitionBy(n -> n % 2 == 0, Collectors.counting()).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Long.valueOf(2), map.get(true));
        assertEquals(Long.valueOf(3), map.get(false));
    }

    @Test
    public void testCountBy() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd", "eee").countBy(String::length).toMap(e -> e.getKey(), e -> Long.valueOf(e.getValue()));
        assertEquals(Long.valueOf(2), map.get(1));
        assertEquals(Long.valueOf(2), map.get(2));
        assertEquals(Long.valueOf(1), map.get(3));
    }

    @Test
    public void testCountByEmpty() {
        Map<Integer, Long> map = Stream.<String> empty().countBy(String::length).toMap(e -> e.getKey(), e -> Long.valueOf(e.getValue()));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testCollapseBiPredicate() {
        Stream<List<Integer>> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b));
        List<List<Integer>> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 1), result.get(0));
        assertEquals(Arrays.asList(2, 2, 2), result.get(1));
        assertEquals(Arrays.asList(3), result.get(2));
    }

    @Test
    public void testCollapseWithMergeFunction() {
        Stream<Integer> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b), Integer::sum);
        assertEquals(Arrays.asList(2, 6, 3), stream.toList());
    }

    @Test
    public void testCollapseWithCollector() {
        Stream<Integer> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b), Collectors.summingInt(Integer::intValue));
        assertEquals(Arrays.asList(2, 6, 3), stream.toList());
    }

    @Test
    public void testScan() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(Integer::sum);
        assertEquals(Arrays.asList(1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testScanWithInit() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(0, Integer::sum);
        assertEquals(Arrays.asList(1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testScanWithInitIncluded() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(0, true, Integer::sum);
        assertEquals(Arrays.asList(0, 1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testSplit() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).split(2);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    @Test
    public void testSplitChunkSizeOne() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3).split(1);
        List<List<Integer>> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
    }

    @Test
    public void testSplitWithPredicate() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5, 6).split(n -> n % 3 == 0);
        List<List<Integer>> result = stream.toList();
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testSplitAt() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).splitAt(2).map(s -> s.toList()).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));
    }

    @Test
    public void testSliding() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).sliding(3);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));
    }

    @Test
    public void testSlidingWithIncrement() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).sliding(2, 2);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    @Test
    public void testSlidingMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).slidingMap((a, b) -> a + b);
        assertEquals(Arrays.asList(3, 5, 7), stream.toList());
    }

    @Test
    public void testSlidingMapTriFunction() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).slidingMap((a, b, c) -> a + b + c);
        assertEquals(Arrays.asList(6, 9), stream.toList());
    }

    @Test
    public void testTop() {
        Stream<Integer> stream = Stream.of(5, 1, 3, 2, 4).top(3);
        assertEquals(Arrays.asList(3, 5, 4), stream.toList());
    }

    @Test
    public void testTopWithComparator() {
        Stream<Integer> stream = Stream.of(5, 1, 3, 2, 4).top(3, Comparator.naturalOrder());
        assertEquals(Arrays.asList(3, 5, 4), stream.toList());
    }

    @Test
    public void testTopGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).top(10);
        assertHaveSameElements(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testForEach() {
        List<Integer> result = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(result::add);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachEmpty() {
        List<Integer> result = new ArrayList<>();
        Stream.<Integer> empty().forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForeachWithCompletion() {
        List<Integer> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Stream.of(1, 2, 3).forEach(result::add, () -> completed.set(true));
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        Stream.of("a", "b", "c").forEachIndexed((i, s) -> result.add(i + ":" + s));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), result);
    }

    @Test
    public void testForEachPair() {
        List<String> result = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachPair((a, b) -> result.add(a + "-" + b));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), result);
    }

    @Test
    public void testForEachTriple() {
        List<String> result = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachTriple((a, b, c) -> result.add(a + "-" + b + "-" + c));
        assertEquals(Arrays.asList("1-2-3", "2-3-4"), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(Stream.of(1, 2, 3, 4).anyMatch(n -> n > 3));
        assertFalse(Stream.of(1, 2, 3).anyMatch(n -> n > 5));
        assertFalse(Stream.<Integer> empty().anyMatch(n -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(Stream.of(2, 4, 6).allMatch(n -> n % 2 == 0));
        assertFalse(Stream.of(2, 3, 4).allMatch(n -> n % 2 == 0));
        assertTrue(Stream.<Integer> empty().allMatch(n -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(Stream.of(1, 3, 5).noneMatch(n -> n % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).noneMatch(n -> n % 2 == 0));
        assertTrue(Stream.<Integer> empty().noneMatch(n -> true));
    }

    @Test
    public void testNMatch() {
        assertTrue(Stream.of(2, 4, 6, 8).nMatch(2, 3, n -> n > 5));
        assertFalse(Stream.of(2, 4, 6, 8).nMatch(5, 10, n -> n > 5));
    }

    @Test
    public void testFindFirst() {
        assertEquals(Integer.valueOf(1), Stream.of(1, 2, 3).findFirst().get());
        assertFalse(Stream.<Integer> empty().findFirst().isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 2, 3, 4).findFirst(n -> n > 2).get());
        assertFalse(Stream.of(1, 2, 3).findFirst(n -> n > 5).isPresent());
    }

    @Test
    public void testFindLast() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 2, 3).last().get());
        assertFalse(Stream.<Integer> empty().last().isPresent());
    }

    @Test
    public void testFindLastWithPredicate() {
        assertEquals(Integer.valueOf(4), Stream.of(1, 2, 3, 4).findLast(n -> n > 2).get());
        assertFalse(Stream.of(1, 2, 3).findLast(n -> n > 5).isPresent());
    }

    @Test
    public void testFindAny() {
        u.Optional<Integer> result = Stream.of(1, 2, 3).findAny();
        assertTrue(result.isPresent());
        assertTrue(Arrays.asList(1, 2, 3).contains(result.get()));
    }

    @Test
    public void testFirst() {
        assertEquals(Integer.valueOf(1), Stream.of(1, 2, 3).first().get());
        assertFalse(Stream.<Integer> empty().first().isPresent());
    }

    @Test
    public void testLast() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 2, 3).last().get());
        assertFalse(Stream.<Integer> empty().last().isPresent());
    }

    @Test
    public void testElementAt() {
        assertEquals(Integer.valueOf(2), Stream.of(1, 2, 3).elementAt(1).get());
        assertFalse(Stream.of(1, 2, 3).elementAt(5).isPresent());
        assertFalse(Stream.<Integer> empty().elementAt(0).isPresent());
    }

    @Test
    public void testOnlyOne() {
        assertEquals(Integer.valueOf(42), Stream.of(42).onlyOne().get());
        assertFalse(Stream.<Integer> empty().onlyOne().isPresent());
        assertThrows(TooManyElementsException.class, () -> Stream.of(1, 2).onlyOne());
    }

    @Test
    public void testCount() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).count());
        assertEquals(0, Stream.empty().count());
    }

    @Test
    public void testMin() {
        assertEquals(Integer.valueOf(1), Stream.of(3, 1, 2).min(Comparator.naturalOrder()).get());
        assertFalse(Stream.<Integer> empty().min(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testMinBy() {
        assertEquals("c", Stream.of("aaa", "bb", "c").minBy(String::length).get());
    }

    @Test
    public void testMax() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 3, 2).max(Comparator.naturalOrder()).get());
        assertFalse(Stream.<Integer> empty().max(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testMaxBy() {
        assertEquals("aaa", Stream.of("a", "bb", "aaa").maxBy(String::length).get());
    }

    @Test
    public void testKthLargest() {
        assertEquals(Integer.valueOf(4), Stream.of(1, 2, 3, 4, 5).kthLargest(2, Comparator.naturalOrder()).get());
    }

    @Test
    public void testSumInt() {
        assertEquals(15, Stream.of(1, 2, 3, 4, 5).sumInt(n -> n));
    }

    @Test
    public void testSumLong() {
        assertEquals(15L, Stream.of(1, 2, 3, 4, 5).sumLong(n -> n));
    }

    @Test
    public void testSumDouble() {
        assertEquals(15.0, Stream.of(1, 2, 3, 4, 5).sumDouble(n -> n), 0.001);
    }

    @Test
    public void testAverageInt() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageInt(n -> n));
    }

    @Test
    public void testAverageLong() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageLong(n -> n));
    }

    @Test
    public void testAverageDouble() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageDouble(n -> n));
    }

    @Test
    public void testReduce() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(Integer::sum).get());
        assertFalse(Stream.<Integer> empty().reduce(Integer::sum).isPresent());
    }

    @Test
    public void testReduceWithIdentity() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(0, Integer::sum));
        assertEquals(Integer.valueOf(0), Stream.<Integer> empty().reduce(0, Integer::sum));
    }

    @Test
    public void testReduceWithIdentityAndCombiner() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(0, Integer::sum, Integer::sum));
    }

    @Test
    public void testCollect() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToList() {
        List<Integer> list = Stream.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToSet() {
        Set<Integer> set = Stream.of(1, 2, 2, 3).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToCollection() {
        LinkedHashSet<Integer> set = Stream.of(1, 2, 3).toCollection(LinkedHashSet::new);
        assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToArray() {
        Integer[] array = Stream.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToMap() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc").toMap(String::length, s -> s);
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        assertEquals("ccc", map.get(3));
    }

    @Test
    public void testToMapWithMerge() {
        Map<Integer, String> map = Stream.of("a", "b", "cc", "dd").toMap(String::length, s -> s, (s1, s2) -> s1 + s2);
        assertEquals("ab", map.get(1));
        assertEquals("ccdd", map.get(2));
    }

    @Test
    public void testToMultiset() {
        Multiset<Integer> multiset = Stream.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, multiset.count(1));
        assertEquals(2, multiset.count(2));
        assertEquals(3, multiset.count(3));
    }

    @Test
    public void testJoin() {
        assertEquals("1,2,3", Stream.of(1, 2, 3).join(","));
    }

    @Test
    public void testJoinEmpty() {
        assertEquals("", Stream.empty().join(","));
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        assertEquals("[1,2,3]", Stream.of(1, 2, 3).join(",", "[", "]"));
    }

    @Test
    public void testInnerJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(2, result.size());
        assertEquals(Pair.of(2, 2), result.get(0));
        assertEquals(Pair.of(3, 3), result.get(1));
    }

    @Test
    public void testInnerJoinEmpty() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(4, 5, 6), n -> n, n -> n).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testInnerJoinWithFunction() {
        List<String> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(2, 3, 4), n -> n, n -> n, (a, b) -> a + "-" + b).toList();
        assertEquals(Arrays.asList("2-2", "3-3"), result);
    }

    @Test
    public void testLeftJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).leftJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(1, null), result.get(0));
        assertEquals(Pair.of(2, 2), result.get(1));
        assertEquals(Pair.of(3, 3), result.get(2));
    }

    @Test
    public void testRightJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).rightJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(2, 2), result.get(0));
        assertEquals(Pair.of(3, 3), result.get(1));
        assertEquals(Pair.of(null, 4), result.get(2));
    }

    @Test
    public void testFullJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).fullJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testCrossJoin() {
        List<Pair<Integer, String>> result = Stream.of(1, 2).crossJoin(Arrays.asList("a", "b")).toList();
        assertEquals(4, result.size());
        assertEquals(Pair.of(1, "a"), result.get(0));
        assertEquals(Pair.of(1, "b"), result.get(1));
        assertEquals(Pair.of(2, "a"), result.get(2));
        assertEquals(Pair.of(2, "b"), result.get(3));
    }

    @Test
    public void testCycled() {
        List<Integer> result = Stream.of(1, 2, 3).cycled().limit(7).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), result);
    }

    @Test
    public void testCycledWithRounds() {
        List<Integer> result = Stream.of(1, 2, 3).cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
    }

    @Test
    public void testCycledZeroRounds() {
        List<Integer> result = Stream.of(1, 2, 3).cycled(0).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testIntersperse() {
        List<Integer> result = Stream.of(1, 2, 3).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    @Test
    public void testIntersperseEmpty() {
        List<Integer> result = Stream.<Integer> empty().intersperse(0).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testIntersperseSingleElement() {
        List<Integer> result = Stream.of(1).intersperse(0).toList();
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testParallel() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel();
        assertTrue(stream.isParallel());
    }

    @Test
    public void testSequential() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel().sequential();
        assertFalse(stream.isParallel());
    }

    @Test
    public void testIsParallel() {
        assertFalse(Stream.of(1, 2, 3).isParallel());
        assertTrue(Stream.of(1, 2, 3).parallel().isParallel());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel(2);
        assertTrue(stream.isParallel());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(() -> closed.set(true));
        stream.count();
        stream.close();
        assertTrue(closed.get());
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(counter::incrementAndGet).onClose(counter::incrementAndGet);
        stream.count();
        stream.close();
        assertEquals(2, counter.get());
    }

    @Test
    public void testThrowIfEmpty() {
        assertThrows(NoSuchElementException.class, () -> Stream.empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyNonEmpty() {
        assertEquals(3, Stream.of(1, 2, 3).throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        assertThrows(IllegalStateException.class, () -> Stream.empty().throwIfEmpty(IllegalStateException::new).count());
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Stream.empty().ifEmpty(() -> executed.set(true)).count();
        assertTrue(executed.get());
    }

    @Test
    public void testIfEmptyNonEmpty() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Stream.of(1, 2, 3).ifEmpty(() -> executed.set(true)).count();
        assertFalse(executed.get());
    }

    @Test
    public void testContainsAll() {
        assertTrue(Stream.of(1, 2, 3, 4, 5).containsAll(Arrays.asList(2, 3, 4)));
        assertFalse(Stream.of(1, 2, 3).containsAll(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testContainsAny() {
        assertTrue(Stream.of(1, 2, 3).containsAny(Arrays.asList(3, 4, 5)));
        assertFalse(Stream.of(1, 2, 3).containsAny(Arrays.asList(4, 5, 6)));
    }

    @Test
    public void testContainsNone() {
        assertTrue(Stream.of(1, 2, 3).containsNone(Arrays.asList(4, 5, 6)));
        assertFalse(Stream.of(1, 2, 3).containsNone(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testHasDuplicates() {
        assertTrue(Stream.of(1, 2, 2, 3).hasDuplicates());
        assertFalse(Stream.of(1, 2, 3).hasDuplicates());
        assertFalse(Stream.<Integer> empty().hasDuplicates());
    }

    @Test
    public void testAsyncRun() throws Exception {
        AtomicInteger result = new AtomicInteger(0);
        com.landawn.abacus.util.ContinuableFuture<Void> future = Stream.of(1, 2, 3).asyncRun(s -> s.forEach(result::addAndGet));
        future.get();
        assertEquals(6, result.get());
    }

    @Test
    public void testAsyncCall() throws Exception {
        com.landawn.abacus.util.ContinuableFuture<Integer> future = Stream.of(1, 2, 3).asyncCall(s -> s.reduce(0, Integer::sum));
        assertEquals(Integer.valueOf(6), future.get());
    }

    @Test
    public void testTransform() {
        List<Integer> result = Stream.of(1, 2, 3).transform(s -> s.map(n -> n * 2)).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void testSelect() {
        List<Integer> result = Stream.of(1, "two", 3, "four", 5).select(Integer.class).toList();
        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    @Test
    public void testBuffered() {
        List<Integer> result = Stream.of(1, 2, 3).buffered().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testBufferedWithSize() {
        List<Integer> result = Stream.of(1, 2, 3).buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPairWith() {
        List<Pair<String, Integer>> result = Stream.of("a", "bb", "ccc").pairWith(String::length).toList();
        assertEquals(Pair.of("a", 1), result.get(0));
        assertEquals(Pair.of("bb", 2), result.get(1));
        assertEquals(Pair.of("ccc", 3), result.get(2));
    }

    @Test
    public void testCombinations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(Arrays.asList(1, 2)));
        assertTrue(result.contains(Arrays.asList(1, 3)));
        assertTrue(result.contains(Arrays.asList(2, 3)));
    }

    @Test
    public void testCombinationsAll() {
        List<List<Integer>> result = Stream.of(1, 2).combinations().toList();
        assertTrue(result.size() >= 3);
    }

    @Test
    public void testPermutations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).permutations().toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testOrderedPermutations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).orderedPermutations().toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testCartesianProduct() {
        List<List<Integer>> result = Stream.of(1, 2).cartesianProduct(Arrays.asList(3, 4)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(Arrays.asList(1, 3)));
        assertTrue(result.contains(Arrays.asList(1, 4)));
        assertTrue(result.contains(Arrays.asList(2, 3)));
        assertTrue(result.contains(Arrays.asList(2, 4)));
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = Stream.of(1, 2, 3).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3), jdkStream.collect(Collectors.toList()));
    }

    @Test
    public void testSps() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).sps(s -> s.filter(n -> n % 2 == 0)).toList();
        assertHaveSameElements(Arrays.asList(2, 4), result);
    }

    @Test
    public void testSkipNulls() {
        List<Integer> result = Stream.of(1, null, 2, null, 3).skipNulls().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDefaultIfEmpty() {
        List<Integer> result1 = Stream.<Integer> empty().defaultIfEmpty(() -> Stream.of(1, 2, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result1);

        List<Integer> result2 = Stream.of(1, 2).defaultIfEmpty(() -> Stream.of(3, 4)).toList();
        assertEquals(Arrays.asList(1, 2), result2);
    }

    @Test
    public void testEmptyStreamOperations() {
        Stream<Integer> empty = Stream.empty();

        assertEquals(0, empty.count());
        assertFalse(Stream.<Integer> empty().first().isPresent());
        assertFalse(Stream.<Integer> empty().last().isPresent());
        assertEquals(0, Stream.<Integer> empty().toList().size());
        assertEquals(0, Stream.<Integer> empty().toSet().size());
        assertEquals("", Stream.<Integer> empty().join(","));
    }

    @Test
    public void testSingleElementStream() {
        Stream<Integer> single = Stream.of(42);

        assertEquals(1, single.count());
        assertEquals(Integer.valueOf(42), Stream.of(42).first().get());
        assertEquals(Integer.valueOf(42), Stream.of(42).last().get());
        assertEquals(Integer.valueOf(42), Stream.of(42).onlyOne().get());
    }

    @Test
    public void testNullHandling() {
        List<Integer> withNulls = Arrays.asList(1, null, 2, null, 3);

        List<Integer> result = Stream.of(withNulls).filter(n -> n != null).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testComplexStreamPipeline() {
        List<String> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).filter(n -> n % 2 == 0).map(n -> n * 2).sorted().limit(3).map(String::valueOf).toList();

        assertEquals(Arrays.asList("4", "8", "12"), result);
    }

    @Test
    public void testMapToChar() {
        char[] result = Stream.of("a", "b", "c").mapToChar(s -> s.charAt(0)).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testMapToCharEmpty() {
        char[] result = Stream.<String> empty().mapToChar(s -> s.charAt(0)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToByte() {
        byte[] result = Stream.of(1, 2, 3).mapToByte(Integer::byteValue).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToByteEmpty() {
        byte[] result = Stream.<Integer> empty().mapToByte(Integer::byteValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToShort() {
        short[] result = Stream.of(1, 2, 3).mapToShort(Integer::shortValue).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToShortEmpty() {
        short[] result = Stream.<Integer> empty().mapToShort(Integer::shortValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToFloat() {
        float[] result = Stream.of(1, 2, 3).mapToFloat(Integer::floatValue).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testMapToFloatEmpty() {
        float[] result = Stream.<Integer> empty().mapToFloat(Integer::floatValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToChar() {
        char[] result = Stream.of("ab", "cd", "ef").flatmapToChar(s -> s.toCharArray()).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testFlatmapToCharEmpty() {
        char[] result = Stream.<String> empty().flatmapToChar(s -> s.toCharArray()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToByte() {
        byte[] result = Stream.of(new byte[] { 1, 2 }, new byte[] { 3, 4 }).flatmapToByte(b -> b).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatmapToByteEmpty() {
        byte[] result = Stream.<byte[]> empty().flatmapToByte(b -> b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToShort() {
        short[] result = Stream.of(new short[] { 1, 2 }, new short[] { 3, 4 }).flatmapToShort(s -> s).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatmapToShortEmpty() {
        short[] result = Stream.<short[]> empty().flatmapToShort(s -> s).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToFloat() {
        float[] result = Stream.of(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f }).flatmapToFloat(f -> f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testFlatmapToFloatEmpty() {
        float[] result = Stream.<float[]> empty().flatmapToFloat(f -> f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFoldLeft() {
        String result = Stream.of("a", "b", "c", "d").foldLeft((a, b) -> a + b).get();
        assertEquals("abcd", result);
    }

    @Test
    public void testFoldLeftWithIdentity() {
        String result = Stream.of("a", "b", "c").foldLeft("start:", (a, b) -> a + b);
        assertEquals("start:abc", result);
    }

    @Test
    public void testFoldLeftEmpty() {
        assertFalse(Stream.<String> empty().foldLeft((a, b) -> a + b).isPresent());
    }

    @Test
    public void testFoldRight() {
        String result = Stream.of("a", "b", "c", "d").foldRight((a, b) -> a + b).get();
        assertEquals("dcba", result);
    }

    @Test
    public void testFoldRightWithIdentity() {
        String result = Stream.of("a", "b", "c").foldRight(":end", (a, b) -> a + b);
        assertEquals(":endcba", result);
    }

    @Test
    public void testFoldRightEmpty() {
        assertFalse(Stream.<String> empty().foldRight((a, b) -> a + b).isPresent());
    }

    @Test
    public void testMapPartial() {
        List<Integer> result = Stream.of("1", "abc", "2", "def", "3").mapPartial(s -> {
            try {
                return u.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return u.Optional.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMapPartialEmpty() {
        List<Integer> result = Stream.<String> empty().mapPartial(s -> u.Optional.of(Integer.parseInt(s))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapPartialJdk() {
        List<Integer> result = Stream.of("1", "abc", "2", "def", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMapMulti() {
        List<Object> result = Stream.of(1, 2, 3).mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testMapMultiEmpty() {
        List<Object> result = Stream.<Integer> empty().mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapMultiToInt() {
        int[] result = Stream.of("a", "bb", "ccc").mapMultiToInt((s, consumer) -> {
            for (int i = 0; i < s.length(); i++) {
                consumer.accept(s.length());
            }
        }).toArray();
        assertArrayEquals(new int[] { 1, 2, 2, 3, 3, 3 }, result);
    }

    @Test
    public void testMapMultiToLong() {
        long[] result = Stream.of(1, 2, 3).mapMultiToLong((n, consumer) -> {
            consumer.accept(n.longValue());
            consumer.accept(n.longValue() * 100L);
        }).toArray();
        assertArrayEquals(new long[] { 1L, 100L, 2L, 200L, 3L, 300L }, result);
    }

    @Test
    public void testMapMultiToDouble() {
        double[] result = Stream.of(1, 2, 3).mapMultiToDouble((n, consumer) -> {
            consumer.accept(n.doubleValue());
            consumer.accept(n.doubleValue() * 0.5);
        }).toArray();
        assertArrayEquals(new double[] { 1.0, 0.5, 2.0, 1.0, 3.0, 1.5 }, result, 0.001);
    }

    @Test
    public void testMapToEntry() {
        List<Map.Entry<String, Integer>> result = Stream.of("a", "bb", "ccc").mapToEntry(s -> s, s -> s.length()).toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals(Integer.valueOf(1), result.get(0).getValue());
        assertEquals("bb", result.get(1).getKey());
        assertEquals(Integer.valueOf(2), result.get(1).getValue());
    }

    @Test
    public void testMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<String> empty().mapToEntry(s -> s, s -> s.length()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMapToEntry() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("c", 3);

        List<Map.Entry<String, Integer>> result = Stream.of(map1, map2).flatMapToEntry(m -> Stream.of(m.entrySet())).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<Map<String, Integer>> empty().flatMapToEntry(m -> Stream.of(m.entrySet())).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRangeMap() {
        List<Integer> result = Stream.of(1, 2, 2, 3, 3, 3, 4).rangeMap((a, b) -> a.equals(b), (a, b) -> a).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testRangeMapEmpty() {
        List<Integer> result = Stream.<Integer> empty().rangeMap((a, b) -> a.equals(b), (a, b) -> a).toList();
        assertTrue(result.isEmpty());
    }

    //    @Test
    //    public void testReduceUntilWithPredicate() {
    //        int result = Stream.of(1, 2, 3, 4, 5).reduceUntil((a, b) -> a + b, sum -> sum > 6).get();
    //        assertEquals(10, result);
    //    }
    //
    //    @Test
    //    public void testReduceUntilWithBiPredicate() {
    //        int result = Stream.of(1, 2, 3, 4, 5).reduceUntil((a, b) -> a + b, (a, b) -> a + b > 6).get();
    //        assertEquals(6, result);
    //    }
    //
    //    @Test
    //    public void testReduceUntilEmpty() {
    //        assertFalse(Stream.<Integer> empty().reduceUntil((a, b) -> a + b, sum -> sum > 6).isPresent());
    //    }

    @Test
    public void testGroupTo() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd", "eee").groupTo(String::length);
        assertEquals(2, map.get(1).size());
        assertEquals(2, map.get(2).size());
        assertEquals(1, map.get(3).size());
    }

    @Test
    public void testGroupToEmpty() {
        Map<Integer, List<String>> map = Stream.<String> empty().groupTo(String::length);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testFlatGroupTo() {
        Map<Integer, List<String>> map = Stream.of("ab", "cd", "efg").flatGroupTo(s -> Arrays.asList(s.length(), s.length()));
        assertEquals(4, map.get(2).size());
        assertEquals(2, map.get(3).size());
    }

    @Test
    public void testCountByToEntry() {
        List<Map.Entry<Integer, Integer>> list = Stream.of("a", "bb", "c", "dd", "eee").countByToEntry(String::length).toList();
        assertEquals(3, list.size());
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 1 && e.getValue() == 2));
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 2 && e.getValue() == 2));
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 3 && e.getValue() == 1));
    }

    @Test
    public void testCountByToEntryEmpty() {
        List<Map.Entry<Integer, Integer>> list = Stream.<String> empty().countByToEntry(String::length).toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testPartitionTo() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4, 5).partitionTo(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionToEmpty() {
        Map<Boolean, List<Integer>> map = Stream.<Integer> empty().partitionTo(n -> n % 2 == 0);
        assertTrue(map.get(true).isEmpty());
        assertTrue(map.get(false).isEmpty());
    }

    @Test
    public void testPartitionByToEntry() {
        List<Map.Entry<Boolean, List<Integer>>> list = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(n -> n % 2 == 0).toList();
        assertEquals(2, list.size());
    }

    @Test
    public void testToMultimap() {
        com.landawn.abacus.util.ListMultimap<Integer, String> multimap = Stream.of("a", "bb", "c", "dd", "eee").toMultimap(String::length);
        assertEquals(2, multimap.get(1).size());
        assertEquals(2, multimap.get(2).size());
        assertEquals(1, multimap.get(3).size());
    }

    @Test
    public void testToMultimapEmpty() {
        com.landawn.abacus.util.ListMultimap<Integer, String> multimap = Stream.<String> empty().toMultimap(String::length);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testToDataset() {
        Dataset dataset = Stream.of(Arrays.asList("a", 1), Arrays.asList("b", 2), Arrays.asList("c", 3)).toDataset(N.asList("Column1", "Column2"));
        assertEquals(3, dataset.size());
    }

    @Test
    public void testToDatasetWithColumnNames() {
        Dataset dataset = Stream.of(Arrays.asList("a", 1), Arrays.asList("b", 2)).toDataset(Arrays.asList("col1", "col2"));
        assertEquals(2, dataset.size());
        assertEquals(Arrays.asList("col1", "col2"), dataset.columnNameList());
    }

    @Test
    public void testToDatasetEmpty() {
        Dataset dataset = Stream.<List<Object>> empty().toDataset();
        assertEquals(0, dataset.size());
    }

    @Test
    public void testSortedByLong() {
        List<String> result = Stream.of("a", "bbb", "cc").sortedByLong(String::length).toList();
        assertEquals(Arrays.asList("a", "cc", "bbb"), result);
    }

    @Test
    public void testSortedByDouble() {
        List<Integer> result = Stream.of(3, 1, 2).sortedByDouble(n -> n.doubleValue()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testReverseSortedBy() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedBy(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    @Test
    public void testReverseSortedByInt() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedByInt(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    @Test
    public void testReverseSortedByLong() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedByLong(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    @Test
    public void testReverseSortedByDouble() {
        List<Integer> result = Stream.of(1, 3, 2).reverseSortedByDouble(n -> n.doubleValue()).toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testMinAll() {
        List<Integer> result = Stream.of(1, 1, 2, 3, 4).minAll(Integer::compareTo);
        assertEquals(Arrays.asList(1, 1), result);
    }

    @Test
    public void testMinAllEmpty() {
        List<Integer> result = Stream.<Integer> empty().minAll(Integer::compareTo);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMaxAll() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 4).maxAll(Integer::compareTo);
        assertEquals(Arrays.asList(4, 4), result);
    }

    @Test
    public void testMaxAllEmpty() {
        List<Integer> result = Stream.<Integer> empty().maxAll(Integer::compareTo);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testPercentiles() {
        u.Optional<Map<Percentage, Integer>> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles(Integer::compareTo);
        assertTrue(result.isPresent());
        assertFalse(result.get().isEmpty());
    }

    @Test
    public void testSkipRange() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toList();
        assertEquals(Arrays.asList(1, 4, 5), result);
    }

    @Test
    public void testSkipRangeEmpty() {
        List<Integer> result = Stream.<Integer> empty().skipRange(1, 3).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipRangeOutOfBounds() {
        List<Integer> result = Stream.of(1, 2, 3).skipRange(5, 10).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPeekIf() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).peekIf(n -> n % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertEquals(Arrays.asList(2, 4), peeked);
    }

    @Test
    public void testPeekIfEmpty() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.<Integer> empty().peekIf(n -> n % 2 == 0, peeked::add).toList();
        assertTrue(result.isEmpty());
        assertTrue(peeked.isEmpty());
    }

    @Test
    public void testCollectThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).collectThenAccept(java.util.stream.Collectors.toList(), holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testCollectThenApply() {
        int result = Stream.of(1, 2, 3).collectThenApply(java.util.stream.Collectors.toList(), List::size);
        assertEquals(3, result);
    }

    @Test
    public void testToListThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toListThenAccept(holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testToListThenApply() {
        int result = Stream.of(1, 2, 3).toListThenApply(List::size);
        assertEquals(3, result);
    }

    @Test
    public void testToSetThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 2, 3).toSetThenAccept(holder::addAll);
        assertEquals(3, holder.size());
    }

    @Test
    public void testToSetThenApply() {
        int result = Stream.of(1, 2, 2, 3).toSetThenApply(java.util.Set::size);
        assertEquals(3, result);
    }

    @Test
    public void testToCollectionThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toCollectionThenAccept(ArrayList::new, holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testToCollectionThenApply() {
        int result = Stream.of(1, 2, 3).toCollectionThenApply(ArrayList::new, List::size);
        assertEquals(3, result);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).acceptIfNotEmpty(s -> s.forEach(holder::add));
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testAcceptIfNotEmptyWithEmpty() {
        List<Integer> holder = new ArrayList<>();
        Stream.<Integer> empty().acceptIfNotEmpty(s -> s.forEach(holder::add));
        assertTrue(holder.isEmpty());
    }

    @Test
    public void testApplyIfNotEmpty() {
        long result = Stream.of(1, 2, 3).applyIfNotEmpty(s -> s.count()).get();
        assertEquals(3L, result);
    }

    @Test
    public void testApplyIfNotEmptyWithEmpty() {
        assertFalse(Stream.<Integer> empty().applyIfNotEmpty(s -> s.count()).isPresent());
    }

    @Test
    public void testMapFirstOrElse() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(n -> "first:" + n, n -> "else:" + n).toList();
        assertEquals(Arrays.asList("first:1", "else:2", "else:3", "else:4", "else:5"), result);
    }

    @Test
    public void testMapFirstOrElseEmpty() {
        List<String> result = Stream.<Integer> empty().mapFirstOrElse(n -> "first:" + n, n -> "else:" + n).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapLastOrElse() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).mapLastOrElse(n -> "last:" + n, n -> "else:" + n).toList();
        assertEquals(Arrays.asList("else:1", "else:2", "else:3", "else:4", "last:5"), result);
    }

    @Test
    public void testMapLastOrElseEmpty() {
        List<String> result = Stream.<Integer> empty().mapLastOrElse(n -> "last:" + n, n -> "else:" + n).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapIfNotNull() {
        List<Integer> result = Stream.of("1", null, "2", null, "3")
                .flatmapIfNotNull(s -> Arrays.asList(Integer.parseInt(s), Integer.parseInt(s) * 10))
                .toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlatmapIfNotNullAllNull() {
        List<String> result = Stream.of(null, null, null).flatmapIfNotNull(s -> Arrays.asList((String) s)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapIfNotNullEmpty() {
        List<Integer> result = Stream.<String> empty().flatmapIfNotNull(s -> Arrays.asList(Integer.parseInt(s))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattMapToEntry() {
        EntryStream<String, Integer> stream1 = EntryStream.of("a", 1);
        EntryStream<String, Integer> stream2 = EntryStream.of("b", 2);

        List<Map.Entry<String, Integer>> result = Stream.of(stream1, stream2).flattMapToEntry(s -> s).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlattMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<EntryStream<String, Integer>> empty().flattMapToEntry(s -> s).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFromJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.of(1, 2, 3, 4, 5);
        List<Integer> result = Stream.from(jdkStream).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testFromJdkStreamEmpty() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.empty();
        List<Integer> result = Stream.from(jdkStream).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDefer() {
        AtomicInteger callCount = new AtomicInteger(0);
        Stream<Integer> deferred = Stream.defer(() -> {
            callCount.incrementAndGet();
            return Stream.of(1, 2, 3);
        });

        assertEquals(0, callCount.get());
        List<Integer> result = deferred.toList();
        assertEquals(1, callCount.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDeferEmpty() {
        Stream<Integer> deferred = Stream.defer(() -> Stream.empty());
        assertTrue(deferred.toList().isEmpty());
    }

    @Test
    public void testFlattenCollection() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> list3 = Arrays.asList(5, 6);

        List<Integer> result = Stream.<Integer> flatten(Arrays.asList(list1, list2, list3)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testFlattenCollectionEmpty() {
        List<Object> result = Stream.flatten(Arrays.asList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenArray() {
        Integer[][] arr = { { 1, 2 }, { 3, 4 } };

        List<Integer> result = Stream.flatten(arr).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testRollup() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4).rollup().toList();
        assertEquals(5, result.size());
        assertEquals(Arrays.asList(Arrays.asList(), Arrays.asList(1), Arrays.asList(1, 2), Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3, 4)), result);
    }

    @Test
    public void testRollupEmpty() {
        List<List<Integer>> result = Stream.<Integer> empty().rollup().toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList(Arrays.asList()), result);
    }

    @Test
    public void testRollupSingleElement() {
        List<List<Integer>> result = Stream.of(1).rollup().toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(Arrays.asList(), Arrays.asList(1)), result);
    }

    @Test
    public void testSplitByChunkCount() {
        List<Integer> result = Stream.splitByChunkCount(8, 3, (from, to) -> from).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSplitByChunkCountEmpty() {
        List<Integer> result = Stream.splitByChunkCount(0, 3, (from, to) -> from).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterables() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> list3 = Arrays.asList(5, 6);

        List<Integer> result = Stream.concatIterables(Arrays.asList(list1, list2, list3)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testConcatIterablesEmpty() {
        List<Integer> result = Stream.concatIterables(Arrays.<List<Integer>> asList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(3, 4).iterator();

        List<Integer> result = Stream.concatIterators(Arrays.asList(iter1, iter2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testMergeIterables() {
        List<Integer> list1 = Arrays.asList(1, 3, 5);
        List<Integer> list2 = Arrays.asList(2, 4, 6);

        List<Integer> result = Stream.mergeIterables(Arrays.asList(list1, list2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeIterablesEmpty() {
        List<Integer> result = Stream.mergeIterables(Arrays.<List<Integer>> asList(), (a, b) -> MergeResult.TAKE_FIRST).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4).iterator();

        List<Integer> result = Stream.mergeIterators(Arrays.asList(iter1, iter2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testZipIterables() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.zipIterables(Arrays.asList(list1, list2), (List<Integer> args) -> args.get(0) + args.get(1)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIterablesEmpty() {
        List<Integer> result = Stream.zipIterables(Arrays.asList(), (List<Integer> args) -> 0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(10, 20).iterator();

        List<Integer> result = Stream.zipIterators(Arrays.asList(iter1, iter2), (List<Integer> args) -> args.get(0) + args.get(1)).toList();
        assertEquals(Arrays.asList(11, 22), result);
    }

    @Test
    public void testOfReversed() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<Integer> result = Stream.ofReversed(array).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testOfReversedEmpty() {
        Integer[] array = {};
        List<Integer> result = Stream.ofReversed(array).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOfReversedList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> result = Stream.ofReversed(list).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testForEachUntil() {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flagToBreak = MutableBoolean.of(false);
        Stream.of(1, 2, 3, 4, 5).forEachUntil(flagToBreak, n -> {
            collected.add(n);
            if (n >= 3) {
                flagToBreak.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachUntilEmpty() {
        List<Integer> collected = new ArrayList<>();
        com.landawn.abacus.util.MutableBoolean flagToBreak = com.landawn.abacus.util.MutableBoolean.of(true);
        Stream.<Integer> empty().forEachUntil(flagToBreak, n -> {
            collected.add(n);
        });
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEachUntilBiConsumer() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5).forEachUntil((n, breakCondition) -> {
            collected.add(n);
            if (n >= 3) {
                breakCondition.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testOnEachE() throws Exception {
        List<Integer> collected = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).onEachE(collected::add).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), collected);
    }

    @Test
    public void testOnEachEEmpty() throws Exception {
        List<Integer> collected = new ArrayList<>();
        List<Integer> result = Stream.<Integer> empty().onEachE(collected::add).toList();
        assertTrue(result.isEmpty());
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testToImmutableMap() {
        com.landawn.abacus.util.ImmutableMap<Integer, String> map = Stream.of("a", "bb", "ccc").toImmutableMap(String::length, s -> s);
        assertEquals(3, map.size());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        assertEquals("ccc", map.get(3));
    }

    @Test
    public void testToImmutableMapEmpty() {
        com.landawn.abacus.util.ImmutableMap<Integer, String> map = Stream.<String> empty().toImmutableMap(String::length, s -> s);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMergeWith() {
        List<Integer> result = Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWithEmpty() {
        List<Integer> result = Stream.<Integer> empty().mergeWith(Stream.of(1, 2, 3), (a, b) -> MergeResult.TAKE_FIRST).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testJoinByRange() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(2, 3, 4);

        List<Pair<Integer, List<Integer>>> result = Stream.of(list1).joinByRange(list2.iterator(), (a, b) -> Math.abs(a - b) <= 1).toList();
        assertFalse(result.isEmpty());
    }

    @Test
    public void testJoinByRangeEmpty() {
        List<Integer> list2 = Arrays.asList(2, 3, 4);

        List<Pair<Integer, List<Integer>>> result = Stream.<Integer> empty().joinByRange(list2.iterator(), (a, b) -> Math.abs(a - b) <= 1).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListFiles() throws IOException {
        File tempFile = tempDir.resolve("test.txt").toFile();
        IOUtil.write("test content", tempFile);

        List<File> result = Stream.listFiles(tempDir.toFile()).toList();
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(f -> f.getName().equals("test.txt")));
    }

    @Test
    public void testListFilesRecursively() throws IOException {
        File txtFile = tempDir.resolve("test.txt").toFile();
        File jsonFile = tempDir.resolve("test.json").toFile();
        IOUtil.write("txt", txtFile);
        IOUtil.write("json", jsonFile);

        List<File> result = Stream.listFiles(tempDir.toFile(), false).toList();
        assertFalse(result.isEmpty());
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testOfLines() throws IOException {
        File file = tempDir.resolve("lines.txt").toFile();
        IOUtil.write("line1\nline2\nline3", file);

        List<String> result = Stream.ofLines(file).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testOfLinesEmpty() throws IOException {
        File file = tempDir.resolve("empty.txt").toFile();
        IOUtil.write("", file);

        List<String> result = Stream.ofLines(file).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitToLines() {
        List<String> result = Stream.splitToLines("line1\nline2\nline3").toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testSplitToLinesEmpty() {
        List<String> result = Stream.splitToLines("").toList();
        assertEquals(Arrays.asList(""), result);
    }

    @Test
    public void testSplitToLinesSingleLine() {
        List<String> result = Stream.splitToLines("single line").toList();
        assertEquals(Arrays.asList("single line"), result);
    }

    @Test
    public void testMapPartialToInt() {
        int[] result = Stream.of("1", "abc", "2", "3").mapPartialToInt(s -> {
            try {
                return u.OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return u.OptionalInt.empty();
            }
        }).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapPartialToLong() {
        long[] result = Stream.of("1", "abc", "2", "3").mapPartialToLong(s -> {
            try {
                return u.OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return u.OptionalLong.empty();
            }
        }).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testMapPartialToDouble() {
        double[] result = Stream.of("1.5", "abc", "2.5", "3.5").mapPartialToDouble(s -> {
            try {
                return u.OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return u.OptionalDouble.empty();
            }
        }).toArray();
        assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, result, 0.001);
    }

    @Test
    public void testInterval() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> result = Stream.interval(5, () -> counter.incrementAndGet()).limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testIntervalWithDelay() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> result = Stream.interval(1, 5, () -> counter.incrementAndGet()).limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testFlatMapToByte() {
        byte[] result = Stream.of(new byte[] { 1, 2 }, new byte[] { 3, 4 }).flatMapToByte(b -> ByteStream.of(b)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToByteEmpty() {
        byte[] result = Stream.<byte[]> empty().flatMapToByte(b -> ByteStream.of(b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToChar() {
        char[] result = Stream.of("ab", "cd").flatMapToChar(s -> CharStream.of(s)).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatMapToCharEmpty() {
        char[] result = Stream.<String> empty().flatMapToChar(s -> CharStream.of(s)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToFloat() {
        float[] result = Stream.of(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f }).flatMapToFloat(f -> FloatStream.of(f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testFlatMapToFloatEmpty() {
        float[] result = Stream.<float[]> empty().flatMapToFloat(f -> FloatStream.of(f)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToShort() {
        short[] result = Stream.of(new short[] { 1, 2 }, new short[] { 3, 4 }).flatMapToShort(s -> ShortStream.of(s)).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToShortEmpty() {
        short[] result = Stream.<short[]> empty().flatMapToShort(s -> ShortStream.of(s)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToIntArray() {
        int[] result = Stream.of("12", "34")
                .flatmapToInt(s -> new int[] { Character.getNumericValue(s.charAt(0)), Character.getNumericValue(s.charAt(1)) })
                .toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatmapToIntArrayEmpty() {
        int[] result = Stream.<String> empty().flatmapToInt(s -> new int[] { 1, 2 }).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToLongArray() {
        long[] result = Stream.of(new long[] { 1L, 2L }, new long[] { 3L, 4L }).flatmapToLong(arr -> arr).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testFlatmapToLongArrayEmpty() {
        long[] result = Stream.<long[]> empty().flatmapToLong(arr -> arr).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToDoubleArray() {
        double[] result = Stream.of(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }).flatmapToDouble(arr -> arr).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapToDoubleArrayEmpty() {
        double[] result = Stream.<double[]> empty().flatmapToDouble(arr -> arr).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToEntryArray() {
        List<Map.Entry<String, Integer>> result = Stream.of("ab", "cd")
                .flatMapToEntry(s -> Stream.of(new AbstractMap.SimpleEntry<>(s, s.length()), new AbstractMap.SimpleEntry<>(s.toUpperCase(), s.length())))
                .toList();
        assertEquals(4, result.size());
        assertEquals("ab", result.get(0).getKey());
        assertEquals(Integer.valueOf(2), result.get(0).getValue());
    }

    @Test
    public void testFlatmapToEntryArrayEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<String> empty().flatMapToEntry(s -> Stream.of(new AbstractMap.SimpleEntry<>(s, s.length()))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattMap() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flattMap(list -> list.stream()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlattMapEmpty() {
        List<Integer> result = Stream.<List<Integer>> empty().flattMap(list -> list.stream()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupJoin() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 1, 2, 3, 3);

        List<Pair<Integer, List<Integer>>> result = Stream.of(list1).groupJoin(list2, n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getLeft());
        assertEquals(2, result.get(0).getRight().size());
    }

    @Test
    public void testGroupJoinEmpty() {
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        List<Pair<Integer, List<Integer>>> result = Stream.<Integer> empty().groupJoin(list2, n -> n, n -> n).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupJoinWithMapper() {
        List<String> list1 = Arrays.asList("a", "bb", "ccc");
        List<Integer> list2 = Arrays.asList(1, 1, 2, 3);

        List<Pair<String, List<Integer>>> result = Stream.of(list1).groupJoin(list2, String::length, n -> n).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testZipWith() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipWithEmpty() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.<Integer> empty().zipWith(list1, (a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipWithThreeWay() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);
        List<Integer> list2 = Arrays.asList(100, 200, 300);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, list2, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipWithDefaultValue() {
        List<Integer> list1 = Arrays.asList(10, 20);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, 0, 0, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 22, 3), result);
    }

    @Test
    public void testMapPartialToIntJdk() {
        int[] result = Stream.of("1", "abc", "2", "3").mapPartialToIntJdk(s -> {
            try {
                return java.util.OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.OptionalInt.empty();
            }
        }).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapPartialToLongJdk() {
        long[] result = Stream.of("1", "abc", "2", "3").mapPartialToLongJdk(s -> {
            try {
                return java.util.OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return java.util.OptionalLong.empty();
            }
        }).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testMapPartialToDoubleJdk() {
        double[] result = Stream.of("1.5", "abc", "2.5", "3.5").mapPartialToDoubleJdk(s -> {
            try {
                return java.util.OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return java.util.OptionalDouble.empty();
            }
        }).toArray();
        assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, result, 0.001);
    }

    @Test
    public void testPrintln() {
        Stream.of(1, 2, 3).println();

        Stream.range(0, 1000).println();

        Stream.range(0, 1001).println();
    }

    @Test
    public void testPrintlnEmpty() {
        Stream.empty().println();
        assertTrue(true);
    }

    @Test
    public void testToImmutableList() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).toImmutableList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        try {
            result.add(6);
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToImmutableListEmpty() {
        List<Integer> result = Stream.<Integer> empty().toImmutableList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToImmutableSet() {
        Set<Integer> result = Stream.of(1, 2, 3, 2, 1).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));

        try {
            result.add(4);
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToImmutableSetEmpty() {
        Set<Integer> result = Stream.<Integer> empty().toImmutableSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransformB() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).transformB(jdkStream -> jdkStream.filter(n -> n % 2 == 0)).toList();
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testTransformBWithMap() {
        List<Integer> result = Stream.of("a", "bb", "ccc").transformB(jdkStream -> jdkStream.map(String::length)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testTransformBEmpty() {
        List<Integer> result = Stream.<Integer> empty().transformB(jdkStream -> jdkStream.filter(n -> n > 0)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParallelConcat() {
        Stream<Integer> s1 = Stream.of(1, 2, 3);
        Stream<Integer> s2 = Stream.of(4, 5, 6);
        List<Integer> result = Stream.parallelConcat(Arrays.asList(s1, s2), 2).toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void testParallelConcatIterators() {
        Iterator<Integer> it1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> it2 = Arrays.asList(3, 4).iterator();
        List<Integer> result = Stream.parallelConcatIterators(Arrays.asList(it1, it2), 2).toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    @Test
    public void testPersist() throws IOException {
        File tempFile = new File(tempDir.toFile(), "persist_test.txt");
        List<String> data = Arrays.asList("line1", "line2", "line3");
        Stream.of(data).persist(tempFile);

        List<String> readBack = IOUtil.readAllLines(tempFile);
        assertEquals(data, readBack);
    }

    @Test
    public void testPersistToCSV() throws IOException {
        File tempFile = new File(tempDir.toFile(), "test.csv");
        List<List<String>> rows = Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("1", "2", "3"));
        Stream.of(rows).persistToCSV(N.asList("Column1", "Column2", "Column3"), tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testPersistToJSON() throws IOException {
        File tempFile = new File(tempDir.toFile(), "test.json");
        List<Map<String, Object>> data = Arrays.asList(new LinkedHashMap<String, Object>() {
            {
                put("name", "Alice");
                put("age", 30);
            }
        });
        Stream.of(data).persistToJSON(tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testSpsMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<String> mapped = stream.spsMap(n -> String.valueOf(n * 2));
        assertNotNull(mapped);
        assertHaveSameElements(Arrays.asList("2", "4", "6"), mapped.toList());
    }

    @Test
    public void testSpsFilter() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
        Stream<Integer> filtered = stream.spsFilter(n -> n % 2 == 0);
        assertNotNull(filtered);
        assertHaveSameElements(Arrays.asList(2, 4), filtered.toList());
    }

    @Test
    public void testSpsFlatMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<Integer> flatMapped = stream.spsFlatMap(n -> Stream.of(n, n * 10));
        assertNotNull(flatMapped);
        List<Integer> result = flatMapped.toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 10, 2, 20, 3, 30)));
    }

    @Test
    public void testSpsOnEach() {
        AtomicInteger count = new AtomicInteger(0);
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<Integer> observed = stream.spsOnEach(n -> count.incrementAndGet());

        assertHaveSameElements(Arrays.asList(1, 2, 3), observed.toList());
        assertEquals(3, count.get());
    }

    @Test
    public void testSpsMapE() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<String> mapped = stream.spsMapE(n -> {
            if (n == null)
                throw new IllegalArgumentException("null value");
            return String.valueOf(n * 2);
        });
        assertHaveSameElements(Arrays.asList("2", "4", "6"), mapped.toList());
    }

    @Test
    public void testSpsFilterE() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
        Stream<Integer> filtered = stream.spsFilterE(n -> {
            if (n == null)
                throw new IllegalArgumentException("null value");
            return n % 2 == 0;
        });
        assertHaveSameElements(Arrays.asList(2, 4), filtered.toList());
    }

    @Test
    public void testSpsFlatMapE() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<Integer> flatMapped = stream.spsFlatMapE(n -> {
            if (n == null)
                throw new IllegalArgumentException("null value");
            return Stream.of(n, n * 10);
        });
        List<Integer> result = flatMapped.toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testSpsOnEachE() {
        AtomicInteger count = new AtomicInteger(0);
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<Integer> observed = stream.spsOnEachE(n -> {
            if (n == null)
                throw new IllegalArgumentException("null value");
            count.incrementAndGet();
        });

        assertHaveSameElements(Arrays.asList(1, 2, 3), observed.toList());
        assertEquals(3, count.get());
    }

    @Test
    public void testParallelConcatEmpty() {
        Stream<Integer> s1 = Stream.empty();
        Stream<Integer> s2 = Stream.empty();
        List<Integer> result = Stream.parallelConcat(Arrays.asList(s1, s2), 2).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSpsFilterEmptyStream() {
        Stream<Integer> filtered = Stream.<Integer> empty().spsFilter(n -> n > 0);
        assertTrue(filtered.toList().isEmpty());
    }

    @Test
    public void testSpsMapEmptyStream() {
        Stream<String> mapped = Stream.<Integer> empty().spsMap(String::valueOf);
        assertTrue(mapped.toList().isEmpty());
    }
}
