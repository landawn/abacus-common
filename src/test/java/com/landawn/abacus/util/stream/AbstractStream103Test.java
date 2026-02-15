package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.guava.Files;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("new-test")
public class AbstractStream103Test extends TestBase {

    private Stream<Integer> stream;
    private Stream<String> stringStream;
    private Stream<Object> objectStream;

    @BeforeEach
    public void setUp() {
    }

    protected <T> Stream<T> createStream(T... elements) {
        return Stream.of(elements).map(e -> e);
    }

    @Test
    public void testSelect() {
        objectStream = createStream("string", 123, 45.6, "another");
        Stream<String> selected = objectStream.select(String.class);
        List<String> result = selected.toList();
        assertEquals(Arrays.asList("string", "another"), result);
    }

    @Test
    public void testPairWith() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> paired = stream.pairWith(i -> "val" + i);
        List<Pair<Integer, String>> result = paired.toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(1, "val1"), result.get(0));
    }

    @Test
    public void testSkipUntil() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.skipUntil(i -> i > 2);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());
    }

    @Test
    public void testFilterWithActionOnDropped() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> filtered = stream.filter(i -> i % 2 == 0, dropped::add);
        List<Integer> result = filtered.toList();
        assertEquals(Arrays.asList(2, 4), result);
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testDropWhileWithActionOnDropped() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> result = stream.dropWhile(i -> i < 3, dropped::add);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testStep() {
        stream = createStream(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertEquals(Arrays.asList(1, 3, 5, 7, 9), stream.step(2).toList());
    }

    @Test
    public void testSlidingMap() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.slidingMap((a, b) -> a + b);
        assertEquals(Arrays.asList(3, 5, 7), result.toList());
    }

    @Test
    public void testSlidingMapWithIncrement() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.slidingMap(2, (a, b) -> a + (b == null ? 0 : b));
        assertEquals(Arrays.asList(3, 7, 5), result.toList());
    }

    @Test
    public void testSlidingMapTriFunction() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.slidingMap((a, b, c) -> a + b + c);
        assertEquals(Arrays.asList(6, 9, 12), result.toList());
    }

    @Test
    public void testMapIfNotNull() {
        objectStream = createStream("a", null, "b", null, "c");
        Stream<String> result = objectStream.mapIfNotNull(obj -> obj == null ? null : obj.toString().toUpperCase());
        assertEquals(Arrays.asList("A", "B", "C"), result.toList());
    }

    @Test
    public void testMapToEntry() {
        stream = createStream(1, 2, 3);
        EntryStream<Integer, String> result = stream.mapToEntry(i -> new AbstractMap.SimpleEntry<>(i, "val" + i));
        Map<Integer, String> map = result.toMap();
        assertEquals("val1", map.get(1));
        assertEquals("val2", map.get(2));
    }

    @Test
    public void testMapToEntryWithKeyValueMappers() {
        stream = createStream(1, 2, 3);
        EntryStream<Integer, String> result = stream.mapToEntry(i -> i * 10, i -> "val" + i);
        Map<Integer, String> map = result.toMap();
        assertEquals("val1", map.get(10));
        assertEquals("val2", map.get(20));
    }

    @Test
    public void testFlatmap() {
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.flatmap(i -> Arrays.asList(i, i * 10));
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.parallel().flatmap(i -> Arrays.asList(i, i * 10));
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.flatmap(i -> Arrays.asList(i, i * 10));
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.parallel().flatmap(i -> Arrays.asList(i, i * 10));
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
    }

    @Test
    public void testFlattMap() {
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.flattmap(i -> new Integer[] { i, i * 10 });
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.parallel().flattmap(i -> new Integer[] { i, i * 10 });
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.flattmap(i -> new Integer[] { i, i * 10 });
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.parallel().flattmap(i -> new Integer[] { i, i * 10 });
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
    }

    @Test
    public void testFlattmap() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.flattMap(i -> java.util.stream.Stream.of(i, i * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
    }

    @Test
    public void testFlatmapToChar() {
        stringStream = createStream("ab", "cd");
        CharStream result = stringStream.flatmapToChar(String::toCharArray);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result.toArray());
    }

    @Test
    public void testFlatmapToByte() {
        stringStream = createStream("ab", "cd");
        ByteStream result = stringStream.flatmapToByte(String::getBytes);
        byte[] expected = "abcd".getBytes();
        assertArrayEquals(expected, result.toArray());
    }

    @Test
    public void testFlatmapToShort() {
        stream = createStream(1, 2);
        ShortStream result = stream.flatmapToShort(i -> new short[] { i.byteValue(), (short) (i * 10) });
        assertArrayEquals(new short[] { 1, 10, 2, 20 }, result.toArray());
    }

    @Test
    public void testFlatmapToInt() {
        stream = createStream(1, 2);
        IntStream result = stream.flatmapToInt(i -> new int[] { i, i * 10 });
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, result.toArray());
    }

    @Test
    public void testFlatmapToLong() {
        stream = createStream(1, 2);
        LongStream result = stream.flatmapToLong(i -> new long[] { i, i * 10L });
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L }, result.toArray());
    }

    @Test
    public void testFlatmapToFloat() {
        stream = createStream(1, 2);
        FloatStream result = stream.flatmapToFloat(i -> new float[] { i, i * 10f });
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f }, result.toArray(), 0.01f);
    }

    @Test
    public void testFlatmapToDouble() {
        stream = createStream(1, 2);
        DoubleStream result = stream.flatmapToDouble(i -> new double[] { i, i * 10.0 });
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0 }, result.toArray(), 0.01);
    }

    @Test
    public void testFlatmapIfNotNull() {
        objectStream = createStream("a", null, "b");
        Stream<Character> result = objectStream.flatmapIfNotNull(obj -> obj == null ? null : Arrays.asList(((String) obj).charAt(0)));
        assertEquals(Arrays.asList('a', 'b'), result.toList());
    }

    @Test
    public void testFlatmapIfNotNullWithTwoMappers() {
        objectStream = createStream("ab", null, "cd");
        Stream<Character> result = objectStream.flatmapIfNotNull(obj -> obj == null ? null : Arrays.asList(obj.toString()),
                str -> str == null ? null : Arrays.asList(str.charAt(0), str.charAt(1)));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result.toList());
    }

    @Test
    public void testFlatMapToEntry() {
        stream = createStream(1, 2);
        EntryStream<Integer, String> result = stream
                .flatMapToEntry(i -> Stream.of(new AbstractMap.SimpleEntry<>(i, "a"), new AbstractMap.SimpleEntry<>(i, "b")));
        assertEquals(4, result.toList().size());
    }

    @Test
    public void testFlatmapToEntry() {
        stream = createStream(1, 2);
        EntryStream<String, Integer> result = stream.flatmapToEntry(i -> {
            Map<String, Integer> map = new HashMap<>();
            map.put("a" + i, i);
            map.put("b" + i, i * 10);
            return map;
        });
        Map<String, Integer> map = result.toMap();
        assertEquals(1, (int) map.get("a1"));
        assertEquals(10, (int) map.get("b1"));
    }

    @Test
    public void testFlattMapToEntry() {
        stream = createStream(1, 2);
        EntryStream<Integer, String> result = stream.flattMapToEntry(i -> EntryStream.of(i, "val" + i));
        Map<Integer, String> map = result.toMap();
        assertEquals("val1", map.get(1));
    }

    @Test
    public void testMapMulti() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        });
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
    }

    @Test
    public void testMapMultiToInt() {
        stream = createStream(1, 2, 3);
        IntStream result = stream.mapMultiToInt((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        });
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result.toArray());
    }

    @Test
    public void testMapMultiToLong() {
        stream = createStream(1, 2, 3);
        LongStream result = stream.mapMultiToLong((i, consumer) -> {
            consumer.accept(i.longValue());
            consumer.accept(i * 10L);
        });
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result.toArray());
    }

    @Test
    public void testMapMultiToDouble() {
        stream = createStream(1, 2, 3);
        DoubleStream result = stream.mapMultiToDouble((i, consumer) -> {
            consumer.accept(i.doubleValue());
            consumer.accept(i * 10.0);
        });
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result.toArray(), 0.01);
    }

    @Test
    public void testMapPartial() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.mapPartial(i -> i % 2 == 0 ? Optional.of(i * 10) : Optional.empty());
        assertEquals(Arrays.asList(20, 40), result.toList());
    }

    @Test
    public void testMapPartialToInt() {
        stream = createStream(1, 2, 3, 4);
        IntStream result = stream.mapPartialToInt(i -> i % 2 == 0 ? OptionalInt.of(i * 10) : OptionalInt.empty());
        assertArrayEquals(new int[] { 20, 40 }, result.toArray());
    }

    @Test
    public void testMapPartialToLong() {
        stream = createStream(1, 2, 3, 4);
        LongStream result = stream.mapPartialToLong(i -> i % 2 == 0 ? OptionalLong.of(i * 10L) : OptionalLong.empty());
        assertArrayEquals(new long[] { 20L, 40L }, result.toArray());
    }

    @Test
    public void testMapPartialToDouble() {
        stream = createStream(1, 2, 3, 4);
        DoubleStream result = stream.mapPartialToDouble(i -> i % 2 == 0 ? OptionalDouble.of(i * 10.0) : OptionalDouble.empty());
        assertArrayEquals(new double[] { 20.0, 40.0 }, result.toArray(), 0.01);
    }

    @Test
    public void testMapPartialJdk() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.mapPartialJdk(i -> i % 2 == 0 ? java.util.Optional.of(i * 10) : java.util.Optional.empty());
        assertEquals(Arrays.asList(20, 40), result.toList());
    }

    @Test
    public void testMapPartialToIntJdk() {
        stream = createStream(1, 2, 3, 4);
        IntStream result = stream.mapPartialToIntJdk(i -> i % 2 == 0 ? java.util.OptionalInt.of(i * 10) : java.util.OptionalInt.empty());
        assertArrayEquals(new int[] { 20, 40 }, result.toArray());
    }

    @Test
    public void testMapPartialToLongJdk() {
        stream = createStream(1, 2, 3, 4);
        LongStream result = stream.mapPartialToLongJdk(i -> i % 2 == 0 ? java.util.OptionalLong.of(i * 10L) : java.util.OptionalLong.empty());
        assertArrayEquals(new long[] { 20L, 40L }, result.toArray());
    }

    @Test
    public void testMapPartialToDoubleJdk() {
        stream = createStream(1, 2, 3, 4);
        DoubleStream result = stream.mapPartialToDoubleJdk(i -> i % 2 == 0 ? java.util.OptionalDouble.of(i * 10.0) : java.util.OptionalDouble.empty());
        assertArrayEquals(new double[] { 20.0, 40.0 }, result.toArray(), 0.01);
    }

    @Test
    public void testRangeMap() {
        stream = createStream(1, 2, 2, 3, 3, 3, 4);
        Stream<String> result = stream.rangeMap((left, right) -> left.equals(right), (left, right) -> left + "-" + right);
        assertEquals(Arrays.asList("1-1", "2-2", "3-3", "4-4"), result.toList());
    }

    @Test
    public void testCollapse() {
        stream = createStream(1, 2, 2, 3, 3, 3, 4);
        Stream<List<Integer>> result = stream.collapse((a, b) -> a.equals(b));
        List<List<Integer>> lists = result.toList();
        assertEquals(4, lists.size());
        assertEquals(Arrays.asList(1), lists.get(0));
        assertEquals(Arrays.asList(2, 2), lists.get(1));
    }

    @Test
    public void testCollapseWithSupplier() {
        stream = createStream(1, 2, 2, 3, 3, 3);
        Stream<Set<Integer>> result = stream.collapse((a, b) -> a.equals(b), Suppliers.ofSet());
        List<Set<Integer>> sets = result.toList();
        assertEquals(3, sets.size());
    }

    @Test
    public void testCollapseWithMergeFunction() {
        stream = createStream(1, 2, 2, 3, 3, 3);
        Stream<Integer> result = stream.collapse((a, b) -> a.equals(b), Integer::sum);
        assertEquals(Arrays.asList(1, 4, 9), result.toList());
    }

    @Test
    public void testCollapseWithInitAndOp() {
        stream = createStream(1, 2, 2, 3, 3, 3);
        Stream<Integer> result = stream.collapse((a, b) -> a.equals(b), 10, Integer::sum);
        assertEquals(Arrays.asList(11, 14, 19), result.toList());
    }

    @Test
    public void testCollapseWithCollector() {
        stream = createStream(1, 2, 2, 3, 3, 3);
        Stream<String> result = stream.collapse((a, b) -> a.equals(b), Collectors.mapping(Object::toString, Collectors.joining(",")));
        assertEquals(Arrays.asList("1", "2,2", "3,3,3"), result.toList());
    }

    @Test
    public void testCollapseTriPredicate() {
        stream = createStream(1, 2, 3, 5, 6, 10);
        Stream<List<Integer>> result = stream.collapse((first, prev, curr) -> curr - first < 3);
        List<List<Integer>> lists = result.toList();
        assertEquals(Arrays.asList(1, 2, 3), lists.get(0));
        assertEquals(Arrays.asList(5, 6), lists.get(1));
    }

    @Test
    public void testScan() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.scan(Integer::sum);
        assertEquals(Arrays.asList(1, 3, 6, 10), result.toList());
    }

    @Test
    public void testScanWithInit() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.scan(10, Integer::sum);
        assertEquals(Arrays.asList(11, 13, 16), result.toList());
    }

    @Test
    public void testScanWithInitIncluded() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.scan(10, true, Integer::sum);
        assertEquals(Arrays.asList(10, 11, 13, 16), result.toList());
    }

    @Test
    public void testSplit() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<List<Integer>> result = stream.split(3);
        List<List<Integer>> lists = result.toList();
        assertEquals(2, lists.size());
        assertEquals(Arrays.asList(1, 2, 3), lists.get(0));
        assertEquals(Arrays.asList(4, 5, 6), lists.get(1));
    }

    @Test
    public void testSplitByPredicate() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<List<Integer>> result = stream.split(i -> i % 3 == 0);
        List<List<Integer>> lists = result.toList();
        assertEquals(3, lists.size());
        assertEquals(Arrays.asList(1, 2), lists.get(0));
        assertEquals(Arrays.asList(3), lists.get(1));
        assertEquals(Arrays.asList(4, 5), lists.get(2));
    }

    @Test
    public void testSplitAt() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Stream<Integer>> result = stream.splitAt(i -> i == 3);
        List<List<Integer>> lists = result.map(Stream::toList).toList();
        assertEquals(2, lists.size());
        assertEquals(Arrays.asList(1, 2), lists.get(0));
        assertEquals(Arrays.asList(3, 4, 5), lists.get(1));
    }

    @Test
    public void testSplitAtWithCollector() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<String> result = stream.splitAt(i -> i == 3, Collectors.mapping(Object::toString, Collectors.joining(",")));
        assertEquals(Arrays.asList("1,2", "3,4,5"), result.toList());
    }

    @Test
    public void testsliding() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<List<Integer>> result = stream.sliding(3);
        List<List<Integer>> lists = result.toList();
        assertEquals(3, lists.size());
        assertEquals(Arrays.asList(1, 2, 3), lists.get(0));
        assertEquals(Arrays.asList(2, 3, 4), lists.get(1));
    }

    @Test
    public void testSlidingWithIncrement() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<List<Integer>> result = stream.sliding(3, 2);
        List<List<Integer>> lists = result.toList();
        assertEquals(3, lists.size());
        assertEquals(Arrays.asList(1, 2, 3), lists.get(0));
        assertEquals(Arrays.asList(3, 4, 5), lists.get(1));
        assertEquals(Arrays.asList(5, 6), lists.get(2));
    }

    @Test
    public void testSlidingWithCollectionSupplier() {
        stream = createStream(1, 2, 3, 4);
        Stream<Set<Integer>> result = stream.sliding(2, i -> new HashSet<>());
        assertEquals(3, result.toList().size());
    }

    @Test
    public void testSlidingWithCollector() {
        stream = createStream(1, 2, 3, 4);
        Stream<String> result = stream.sliding(2, Collectors.mapping(Object::toString, Collectors.joining(",")));
        assertEquals(Arrays.asList("1,2", "2,3", "3,4"), result.toList());
    }

    @Test
    public void testIntersperse() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.intersperse(0);
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result.toList());
    }

    @Test
    public void testOnFirst() {
        stream = createStream(1, 2, 3);
        List<Integer> firstElement = new ArrayList<>();
        stream.onFirst(firstElement::add).toList();
        assertEquals(Arrays.asList(1), firstElement);
    }

    @Test
    public void testOnLast() {
        stream = createStream(1, 2, 3);
        List<Integer> lastElement = new ArrayList<>();
        stream.onLast(lastElement::add).toList();
        assertEquals(Arrays.asList(3), lastElement);
    }

    @Test
    public void testForEach() {
        stream = createStream(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        stream.forEach(result::add);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachIndexed() {
        stringStream = createStream("a", "b", "c");
        Map<Integer, String> result = new HashMap<>();
        stringStream.forEachIndexed(result::put);
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testForEachUntil() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = new ArrayList<>();
        stream.forEachUntil((i, flag) -> {
            result.add(i);
            if (i == 3)
                flag.setValue(true);
        });
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachUntilWithFlag() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        stream.forEachUntil(flag, i -> {
            result.add(i);
            if (i == 3)
                flag.setValue(true);
        });
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachPair() {
        stream = createStream(1, 2, 3, 4);
        List<String> result = new ArrayList<>();
        stream.forEachPair((a, b) -> result.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "2,3", "3,4"), result);
    }

    @Test
    public void testForEachTriple() {
        stream = createStream(1, 2, 3, 4, 5);
        List<String> result = new ArrayList<>();
        stream.forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5"), result);
    }

    //    @Test
    //    public void testReduceUntil() {
    //        stream = createStream(1, 2, 3, 4, 5);
    //        Optional<Integer> result = stream.reduceUntil(Integer::sum, sum -> sum > 5);
    //        assertTrue(result.isPresent());
    //        assertEquals(6, result.get());
    //    }
    //
    //    @Test
    //    public void testReduceUntilBiPredicate() {
    //        stream = createStream(1, 2, 3, 4, 5);
    //        Optional<Integer> result = stream.reduceUntil(Integer::sum, (sum, prev) -> sum > 5);
    //        assertTrue(result.isPresent());
    //        assertEquals(6, result.get());
    //    }
    //
    //    @Test
    //    public void testReduceUntilWithIdentity() {
    //        stream = createStream(1, 2, 3, 4, 5);
    //        Integer result = stream.reduceUntil(0, Integer::sum, (a, b) -> a + b, sum -> sum > 5);
    //        assertEquals(6, result);
    //    }

    @Test
    public void testGroupBy() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Map.Entry<Integer, List<Integer>>> result = stream.groupBy(i -> i % 2);
        Map<Integer, List<Integer>> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList(2, 4, 6), map.get(0));
        assertEquals(Arrays.asList(1, 3, 5), map.get(1));
    }

    @Test
    public void testGroupByWithMapFactory() {
        stream = createStream(1, 2, 3, 4);
        Stream<Map.Entry<Integer, List<Integer>>> result = stream.groupBy(i -> i % 2, Suppliers.ofTreeMap());
        assertTrue(result.toList().size() > 0);
    }

    @Test
    public void testGroupByWithValueMapper() {
        stream = createStream(1, 2, 3, 4);
        Stream<Map.Entry<Integer, List<String>>> result = stream.groupBy(i -> i % 2, i -> "val" + i);
        Map<Integer, List<String>> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList("val2", "val4"), map.get(0));
    }

    @Test
    public void testGroupByWithCollector() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Map.Entry<Integer, Long>> result = stream.groupBy(i -> i % 2, Collectors.counting());
        Map<Integer, Long> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(3L, map.get(0));
        assertEquals(3L, map.get(1));
    }

    @Test
    public void testGroupByWithMergeFunction() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Map.Entry<Integer, Integer>> result = stream.groupBy(i -> i % 2, Fn.identity(), Integer::sum);
        Map<Integer, Integer> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(12, map.get(0));
        assertEquals(9, map.get(1));
    }

    @Test
    public void testPartitionBy() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Map.Entry<Boolean, List<Integer>>> result = stream.partitionBy(i -> i % 2 == 0);
        Map<Boolean, List<Integer>> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionByWithCollector() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Map.Entry<Boolean, Long>> result = stream.partitionBy(i -> i % 2 == 0, Collectors.counting());
        Map<Boolean, Long> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(2L, map.get(true));
        assertEquals(3L, map.get(false));
    }

    @Test
    public void testPartitionByToEntry() {
        stream = createStream(1, 2, 3, 4, 5);
        EntryStream<Boolean, List<Integer>> result = stream.partitionByToEntry(i -> i % 2 == 0);
        Map<Boolean, List<Integer>> map = result.toMap();
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testGroupByToEntry() {
        {
            stream = createStream(1, 2, 3, 4);
            EntryStream<Integer, List<Integer>> result = stream.groupByToEntry(i -> i % 2);
            Map<Integer, List<Integer>> map = result.toMap();
            assertEquals(Arrays.asList(2, 4), map.get(0));
            assertEquals(Arrays.asList(1, 3), map.get(1));
        }
        {
            stream = createStream(1, 2, 3, 4);
            EntryStream<Integer, List<Integer>> result = stream.groupByToEntry(i -> i % 2, Fn.identity(), Collectors.toList());
            Map<Integer, List<Integer>> map = result.toMap();
            assertEquals(Arrays.asList(2, 4), map.get(0));
            assertEquals(Arrays.asList(1, 3), map.get(1));
        }
    }

    @Test
    public void testToMap() {
        stream = createStream(1, 2, 3);
        Map<Integer, String> result = stream.toMap(Fn.identity(), i -> "val" + i);
        assertEquals("val1", result.get(1));
        assertEquals("val2", result.get(2));
        assertEquals("val3", result.get(3));
    }

    @Test
    public void testToMapWithMapFactory() {
        stream = createStream(1, 2, 3);
        TreeMap<Integer, String> result = stream.toMap(Fn.identity(), i -> "val" + i, TreeMap::new);
        assertEquals("val1", result.get(1));
    }

    @Test
    public void testToMapWithMergeFunction() {
        stream = createStream(1, 1, 2, 2, 3);
        Map<Integer, Integer> result = stream.toMap(Fn.identity(), Fn.identity(), Integer::sum);
        assertEquals(2, result.get(1));
        assertEquals(4, result.get(2));
        assertEquals(3, result.get(3));
    }

    @Test
    public void testGroupTo() {
        stream = createStream(1, 2, 3, 4);
        Map<Integer, List<Integer>> result = stream.groupTo(i -> i % 2);
        assertEquals(Arrays.asList(2, 4), result.get(0));
        assertEquals(Arrays.asList(1, 3), result.get(1));
    }

    @Test
    public void testGroupToWithMapFactory() {
        stream = createStream(1, 2, 3, 4);
        TreeMap<Integer, List<Integer>> result = stream.groupTo(i -> i % 2, Suppliers.ofTreeMap());
        assertEquals(Arrays.asList(2, 4), result.get(0));
    }

    @Test
    public void testGroupToWithValueMapper() {
        stream = createStream(1, 2, 3, 4);
        Map<Integer, List<String>> result = stream.groupTo(i -> i % 2, i -> "val" + i);
        assertEquals(Arrays.asList("val2", "val4"), result.get(0));
    }

    @Test
    public void testGroupToWithCollector() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Map<Integer, Long> result = stream.groupTo(i -> i % 2, Collectors.counting());
        assertEquals(3L, result.get(0));
        assertEquals(3L, result.get(1));
    }

    @Test
    public void testFlatGroupTo() {
        stream = createStream(1, 2, 3);
        Map<String, List<Integer>> result = stream.flatGroupTo(i -> Arrays.asList("a", "b"));
        assertEquals(Arrays.asList(1, 2, 3), result.get("a"));
        assertEquals(Arrays.asList(1, 2, 3), result.get("b"));
    }

    @Test
    public void testFlatGroupToWithValueMapper() {
        stream = createStream(1, 2);
        Map<String, List<String>> result = stream.flatGroupTo(i -> Arrays.asList("a", "b"), (key, val) -> key + val);
        assertEquals(Arrays.asList("a1", "a2"), result.get("a"));
        assertEquals(Arrays.asList("b1", "b2"), result.get("b"));
    }

    @Test
    public void testFlatGroupToWithCollector() {
        stream = createStream(1, 2, 3);
        Map<String, Long> result = stream.flatGroupTo(i -> Arrays.asList("a", "b"), Collectors.counting());
        assertEquals(3L, result.get("a"));
        assertEquals(3L, result.get("b"));
    }

    @Test
    public void testPartitionTo() {
        stream = createStream(1, 2, 3, 4, 5);
        Map<Boolean, List<Integer>> result = stream.partitionTo(i -> i % 2 == 0);
        assertEquals(Arrays.asList(2, 4), result.get(true));
        assertEquals(Arrays.asList(1, 3, 5), result.get(false));
    }

    @Test
    public void testPartitionToWithCollector() {
        stream = createStream(1, 2, 3, 4, 5);
        Map<Boolean, Long> result = stream.partitionTo(i -> i % 2 == 0, Collectors.counting());
        assertEquals(2L, result.get(true));
        assertEquals(3L, result.get(false));
    }

    @Test
    public void testToMultimap() {
        stream = createStream(1, 2, 3, 4);
        ListMultimap<Integer, Integer> result = stream.toMultimap(i -> i % 2);
        assertEquals(Arrays.asList(2, 4), result.get(0));
        assertEquals(Arrays.asList(1, 3), result.get(1));
    }

    @Test
    public void testToMultimapWithMapFactory() {
        stream = createStream(1, 2, 3, 4);
        ListMultimap<Integer, Integer> result = stream.toMultimap(i -> i % 2, Suppliers.ofListMultimap());
        assertEquals(Arrays.asList(2, 4), result.get(0));
    }

    @Test
    public void testToMultimapWithValueMapper() {
        stream = createStream(1, 2, 3, 4);
        ListMultimap<Integer, String> result = stream.toMultimap(i -> i % 2, i -> "val" + i);
        assertEquals(Arrays.asList("val2", "val4"), result.get(0));
    }

    @Test
    public void testSumInt() {
        stream = createStream(1, 2, 3, 4);
        long result = stream.sumInt(Integer::intValue);
        assertEquals(10L, result);
    }

    @Test
    public void testSumLong() {
        stream = createStream(1, 2, 3, 4);
        long result = stream.sumLong(Integer::longValue);
        assertEquals(10L, result);
    }

    @Test
    public void testSumDouble() {
        stream = createStream(1, 2, 3, 4);
        double result = stream.sumDouble(Integer::doubleValue);
        assertEquals(10.0, result, 0.01);
    }

    @Test
    public void testAverageInt() {
        stream = createStream(1, 2, 3, 4);
        OptionalDouble result = stream.averageInt(Integer::intValue);
        assertTrue(result.isPresent());
        assertEquals(2.5, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAverageLong() {
        stream = createStream(1, 2, 3, 4);
        OptionalDouble result = stream.averageLong(Integer::longValue);
        assertTrue(result.isPresent());
        assertEquals(2.5, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAverageDouble() {
        stream = createStream(1, 2, 3, 4);
        OptionalDouble result = stream.averageDouble(Integer::doubleValue);
        assertTrue(result.isPresent());
        assertEquals(2.5, result.getAsDouble(), 0.01);
    }

    @Test
    public void testMinAll() {
        stream = createStream(3, 1, 2, 1, 4);
        List<Integer> result = stream.minAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 1), result);
    }

    @Test
    public void testMaxAll() {
        stream = createStream(1, 4, 2, 4, 3);
        List<Integer> result = stream.maxAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(4, 4), result);
    }

    @Test
    public void testFindAny() {
        stream = createStream(1, 2, 3, 4, 5);
        Optional<Integer> result = stream.findAny(i -> i > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testContainsAll() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsAll(1, 3, 5));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsAll(1, 6));
    }

    @Test
    public void testContainsAllCollection() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsAll(Arrays.asList(1, 3, 5)));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsAll(Arrays.asList(1, 6)));
    }

    @Test
    public void testContainsAny() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsAny(3, 6, 9));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsAny(6, 7, 8));
    }

    @Test
    public void testContainsAnyCollection() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsAny(Arrays.asList(3, 6, 9)));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsAny(Arrays.asList(6, 7, 8)));
    }

    @Test
    public void testContainsNone() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsNone(6, 7, 8));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsNone(3, 6, 9));
    }

    @Test
    public void testContainsNoneCollection() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsNone(Arrays.asList(6, 7, 8)));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsNone(Arrays.asList(3, 6, 9)));
    }

    @Test
    public void testFirst() {
        stream = createStream(1, 2, 3);
        Optional<Integer> result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        stream = createStream();
        result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        stream = createStream(1, 2, 3);
        Optional<Integer> result = stream.last();
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        stream = createStream();
        result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt() {
        stream = createStream(1, 2, 3, 4, 5);
        Optional<Integer> result = stream.elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testOnlyOne() {
        stream = createStream(42);
        Optional<Integer> result = stream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());

        stream = createStream();
        result = stream.onlyOne();
        assertFalse(result.isPresent());

        stream = createStream(1, 2);
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(2);
        stream = createStream(1, 2, 3, 4);
        long start = System.currentTimeMillis();
        stream.rateLimited(rateLimiter).toList();
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 1000);
    }

    @Test
    public void testDelay() {
        stream = createStream(1, 2, 3);
        long start = System.currentTimeMillis();
        stream.delay(Duration.ofMillis(100)).toList();
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 300);
    }

    @Test
    public void testSkipNulls() {
        objectStream = createStream("a", null, "b", null, "c");
        Stream<Object> result = objectStream.skipNulls();
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());
    }

    @Test
    public void testSkipRange() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Integer> result = stream.skipRange(2, 4);
        assertEquals(Arrays.asList(1, 2, 5, 6), result.toList());
    }

    @Test
    public void testSkipWithAction() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> skipped = new ArrayList<>();
        Stream<Integer> result = stream.skip(2, skipped::add);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testIntersection() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.intersection(Arrays.asList(3, 4, 5, 6));
        assertEquals(Arrays.asList(3, 4, 5), result.toList());
    }

    @Test
    public void testIntersectionWithMapper() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.intersection(i -> i % 2, Arrays.asList(0, 1));
        assertEquals(Arrays.asList(1, 2), result.toList());
    }

    @Test
    public void testDifference() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.difference(Arrays.asList(3, 4));
        assertEquals(Arrays.asList(1, 2, 5), result.toList());
    }

    @Test
    public void testDifferenceWithMapper() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.difference(i -> i % 2, Arrays.asList(0));
        assertEquals(Arrays.asList(1, 3, 4, 5), result.toList());
    }

    @Test
    public void testSymmetricDifference() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.symmetricDifference(Arrays.asList(3, 4, 5, 6));
        List<Integer> list = result.toList();
        assertTrue(list.contains(1));
        assertTrue(list.contains(2));
        assertTrue(list.contains(5));
        assertTrue(list.contains(6));
    }

    @Test
    public void testReversed() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.reversed();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result.toList());
    }

    @Test
    public void testRotated() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.rotated(2);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), result.toList());
    }

    @Test
    public void testShuffled() {
        stream = createStream(1, 2, 3, 4, 5);
        Random random = new Random(42);
        List<Integer> result = stream.shuffled(random).toList();
        assertEquals(5, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5)));
    }

    @Test
    public void testSorted() {
        stream = createStream(3, 1, 4, 1, 5, 9);
        Stream<Integer> result = stream.sorted();
        assertEquals(Arrays.asList(1, 1, 3, 4, 5, 9), result.toList());
    }

    @Test
    public void testSortedWithComparator() {
        stream = createStream(3, 1, 4, 1, 5, 9);
        Stream<Integer> result = stream.sorted(Comparator.reverseOrder());
        assertEquals(Arrays.asList(9, 5, 4, 3, 1, 1), result.toList());
    }

    @Test
    public void testSortedBy() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.sortedBy(String::length);
        assertEquals(Arrays.asList("b", "cc", "aaa"), result.toList());
    }

    @Test
    public void testSortedByInt() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.sortedByInt(String::length);
        assertEquals(Arrays.asList("b", "cc", "aaa"), result.toList());
    }

    @Test
    public void testSortedByLong() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.sortedByLong(s -> (long) s.length());
        assertEquals(Arrays.asList("b", "cc", "aaa"), result.toList());
    }

    @Test
    public void testSortedByDouble() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.sortedByDouble(s -> (double) s.length());
        assertEquals(Arrays.asList("b", "cc", "aaa"), result.toList());
    }

    @Test
    public void testReverseSorted() {
        stream = createStream(3, 1, 4, 1, 5, 9);
        Stream<Integer> result = stream.reverseSorted();
        assertEquals(Arrays.asList(9, 5, 4, 3, 1, 1), result.toList());
    }

    @Test
    public void testReverseSortedWithComparator() {
        stream = createStream(3, 1, 4, 1, 5, 9);
        Stream<Integer> result = stream.reverseSorted(Comparator.naturalOrder());
        assertEquals(Arrays.asList(9, 5, 4, 3, 1, 1), result.toList());
    }

    @Test
    public void testReverseSortedBy() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.reverseSortedBy(String::length);
        assertEquals(Arrays.asList("aaa", "cc", "b"), result.toList());
    }

    @Test
    public void testReverseSortedByInt() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.reverseSortedByInt(String::length);
        assertEquals(Arrays.asList("aaa", "cc", "b"), result.toList());
    }

    @Test
    public void testReverseSortedByLong() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.reverseSortedByLong(s -> (long) s.length());
        assertEquals(Arrays.asList("aaa", "cc", "b"), result.toList());
    }

    @Test
    public void testReverseSortedByDouble() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.reverseSortedByDouble(s -> (double) s.length());
        assertEquals(Arrays.asList("aaa", "cc", "b"), result.toList());
    }

    @Test
    public void testDistinct() {
        stream = createStream(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> result = stream.distinct();
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());
    }

    @Test
    public void testDistinctBy() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Integer> result = stream.distinctBy(i -> i % 3);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testTop() {
        stream = createStream(5, 2, 8, 1, 9, 3);
        Stream<Integer> result = stream.top(3);
        assertEquals(Arrays.asList(5, 8, 9), result.toList());
    }

    @Test
    public void testPercentiles() {
        stream = createStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Optional<Map<Percentage, Integer>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Integer> percentiles = result.get();
        assertTrue(percentiles.containsKey(Percentage._20));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._70));
    }

    @Test
    public void testPercentilesWithComparator() {
        stream = createStream(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        Optional<Map<Percentage, Integer>> result = stream.percentiles(Comparator.naturalOrder());
        assertTrue(result.isPresent());
    }

    @Test
    public void testCombinations() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.combinations();
        List<List<Integer>> lists = result.toList();
        assertEquals(8, lists.size());
    }

    @Test
    public void testCombinationsWithLength() {
        stream = createStream(1, 2, 3, 4);
        Stream<List<Integer>> result = stream.combinations(2);
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
        assertTrue(lists.contains(Arrays.asList(1, 2)));
        assertTrue(lists.contains(Arrays.asList(3, 4)));
    }

    @Test
    public void testCombinationsWithRepeat() {
        stream = createStream(1, 2);
        Stream<List<Integer>> result = stream.combinations(2, true);
        List<List<Integer>> lists = result.toList();
        assertEquals(4, lists.size());
    }

    @Test
    public void testPermutations() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.permutations();
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
    }

    @Test
    public void testOrderedPermutations() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.orderedPermutations();
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
    }

    @Test
    public void testOrderedPermutationsWithComparator() {
        stream = createStream(3, 2, 1);
        Stream<List<Integer>> result = stream.orderedPermutations(Comparator.naturalOrder());
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
    }

    @Test
    public void testCartesianProduct() {
        stream = createStream(1, 2);
        Stream<List<Integer>> result = stream.cartesianProduct(Arrays.asList(Arrays.asList(3, 4), Arrays.asList(5, 6)));
        List<List<Integer>> lists = result.toList();
        assertEquals(8, lists.size());
    }

    @Test
    public void testToArray() {
        stream = createStream(1, 2, 3);
        Integer[] result = stream.toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    public void testToDataset() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("id", 1);
        map1.put("value", 100);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("id", 2);
        map2.put("value", 200);

        Stream<Map<String, Integer>> mapStream = createStream(map1, map2);
        Dataset result = mapStream.toDataset();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testToDatasetWithColumnNames() {
        stream = createStream(1, 2, 3);
        Dataset result = stream.toDataset(Arrays.asList("value"));
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testJoin() {
        stringStream = createStream("a", "b", "c");
        String result = stringStream.join(",", "[", "]");
        assertEquals("[a,b,c]", result);
    }

    @Test
    public void testJoinTo() {
        stringStream = createStream("a", "b", "c");
        Joiner joiner = Joiner.with(",");
        Joiner result = stringStream.joinTo(joiner);
        assertEquals("a,b,c", result.toString());
    }

    @Test
    public void testHasDuplicates() {
        stream = createStream(1, 2, 3, 2, 4);
        assertTrue(stream.hasDuplicates());

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.hasDuplicates());
    }

    @Test
    public void testCollectThenApply() {
        stream = createStream(1, 2, 3);
        String result = stream.collectThenApply(Collectors.toList(), list -> "Size: " + list.size());
        assertEquals("Size: 3", result);
    }

    @Test
    public void testCollectThenAccept() {
        stream = createStream(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        stream.collectThenAccept(Collectors.toList(), list -> result.addAll(list));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testToListThenApply() {
        stream = createStream(1, 2, 3);
        Integer result = stream.toListThenApply(list -> list.size());
        assertEquals(3, result);
    }

    @Test
    public void testToListThenAccept() {
        stream = createStream(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        stream.toListThenAccept(result::addAll);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testToSetThenApply() {
        stream = createStream(1, 2, 2, 3);
        Integer result = stream.toSetThenApply(Set::size);
        assertEquals(3, result);
    }

    @Test
    public void testToSetThenAccept() {
        stream = createStream(1, 2, 2, 3);
        Set<Integer> result = new HashSet<>();
        stream.toSetThenAccept(result::addAll);
        assertEquals(3, result.size());
    }

    @Test
    public void testToCollectionThenApply() {
        stream = createStream(1, 2, 3);
        Integer result = stream.toCollectionThenApply(ArrayList::new, Collection::size);
        assertEquals(3, result);
    }

    @Test
    public void testToCollectionThenAccept() {
        stream = createStream(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        stream.toCollectionThenAccept(LinkedList::new, result::addAll);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testIndexed() {
        stringStream = createStream("a", "b", "c");
        Stream<Indexed<String>> result = stringStream.indexed();
        List<Indexed<String>> list = result.toList();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0).value());
        assertEquals(0, list.get(0).index());
        assertEquals("b", list.get(1).value());
        assertEquals(1, list.get(1).index());
    }

    @Test
    public void testCycled() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.cycled();
        List<Integer> list = result.limit(10).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1, 2, 3, 1), list);
    }

    @Test
    public void testCycledWithRounds() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.cycled(2);
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result.toList());
    }

    @Test
    public void testRollup() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.rollup();
        List<List<Integer>> lists = result.toList();
        assertEquals(4, lists.size());
        assertEquals(Arrays.asList(), lists.get(0));
        assertEquals(Arrays.asList(1), lists.get(1));
        assertEquals(Arrays.asList(1, 2), lists.get(2));
        assertEquals(Arrays.asList(1, 2, 3), lists.get(3));
    }

    @Test
    public void testBuffered() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.buffered();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testBufferedWithSize() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.buffered(3);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testBufferedWithQueue() {
        stream = createStream(1, 2, 3, 4, 5);
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
        Stream<Integer> result = stream.buffered(queue);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testAppendStream() {
        stream = createStream(1, 2, 3);
        Stream<Integer> other = createStream(4, 5);
        Stream<Integer> result = stream.append(other);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testAppendCollection() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.append(Arrays.asList(4, 5));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testAppendOptional() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.append(Optional.of(4));
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        stream = createStream(1, 2, 3);
        result = stream.append(Optional.empty());
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testPrependStream() {
        stream = createStream(3, 4, 5);
        Stream<Integer> other = createStream(1, 2);
        Stream<Integer> result = stream.prepend(other);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testPrependCollection() {
        stream = createStream(3, 4, 5);
        Stream<Integer> result = stream.prepend(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testPrependOptional() {
        stream = createStream(2, 3, 4);
        Stream<Integer> result = stream.prepend(Optional.of(1));
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        stream = createStream(1, 2, 3);
        result = stream.prepend(Optional.empty());
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testMergeWith() {
        stream = createStream(1, 3, 5);
        Stream<Integer> result = stream.mergeWith(Arrays.asList(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());
    }

    @Test
    public void testMergeWithStream() {
        stream = createStream(1, 3, 5);
        Stream<Integer> other = createStream(2, 4, 6);
        Stream<Integer> result = stream.mergeWith(other, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());
    }

    @Test
    public void testZipWith() {
        stream = createStream(1, 2, 3);
        Stream<String> result = stream.zipWith(Arrays.asList("a", "b", "c"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), result.toList());
    }

    @Test
    public void testZipWithDefaultValues() {
        stream = createStream(1, 2, 3);
        Stream<String> result = stream.zipWith(Arrays.asList("a", "b"), 0, "z", (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3z"), result.toList());
    }

    @Test
    public void testZipWithThreeCollections() {
        stream = createStream(1, 2, 3);
        Stream<String> result = stream.zipWith(Arrays.asList("a", "b", "c"), Arrays.asList(10.0, 20.0, 30.0), (i, s, d) -> i + s + d);
        assertEquals(Arrays.asList("1a10.0", "2b20.0", "3c30.0"), result.toList());
    }

    @Test
    public void testZipWithStream() {
        stream = createStream(1, 2, 3);
        Stream<String> other = createStream("a", "b", "c");
        Stream<String> result = stream.zipWith(other, (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), result.toList());
    }

    @Test
    public void testSaveEachToFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        stringStream = createStream("line1", "line2", "line3");
        stringStream.saveEach(tempFile).toList();

        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
    }

    @Test
    public void testSaveEachToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stringStream = createStream("line1", "line2", "line3");
        stringStream.saveEach(Fn.identity(), baos).toList();

        String result = baos.toString();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
    }

    @Test
    public void testSaveEachToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        stringStream = createStream("line1", "line2", "line3");
        stringStream.saveEach(Fn.identity(), writer).toList();

        String result = writer.toString();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
    }

    @Test
    public void testSaveEachWithBiConsumer() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        stringStream = createStream("line1", "line2", "line3");
        stringStream.saveEach((s, writer) -> writer.write(s.toUpperCase()), tempFile).toList();

        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(Arrays.asList("LINE1", "LINE2", "LINE3"), lines);
    }

    @Test
    public void testSaveEachToPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        stream = createStream(1, 2, 3);
        stream.saveEach(stmt, (i, ps) -> ps.setInt(1, i)).toList();

        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testSaveEachToConnection() throws SQLException {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        stream = createStream(1, 2, 3);
        stream.saveEach(conn, "INSERT INTO test VALUES (?)", (i, ps) -> ps.setInt(1, i)).toList();

        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testSaveEachToDataSource() throws SQLException {
        javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        stream = createStream(1, 2, 3);
        stream.saveEach(ds, "INSERT INTO test VALUES (?)", (i, ps) -> ps.setInt(1, i)).toList();

        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testPersistToFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        stringStream = createStream("line1", "line2", "line3");
        long count = stringStream.persist(tempFile);

        assertEquals(3, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
    }

    @Test
    public void testPersistWithHeaderAndTail() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        stringStream = createStream("data1", "data2");
        long count = stringStream.persist("HEADER", "TAIL", tempFile);

        assertEquals(2, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(Arrays.asList("HEADER", "data1", "data2", "TAIL"), lines);
    }

    @Test
    public void testPersistToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stringStream = createStream("line1", "line2");
        long count = stringStream.persist(Fn.identity(), baos);

        assertEquals(2, count);
        String result = baos.toString();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
    }

    @Test
    public void testPersistToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        stringStream = createStream("line1", "line2");
        long count = stringStream.persist(Fn.identity(), writer);

        assertEquals(2, count);
        String result = writer.toString();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
    }

    @Test
    public void testPersistWithBiConsumer() throws IOException {
        StringWriter writer = new StringWriter();
        stringStream = createStream("line1", "line2");
        long count = stringStream.persist((s, w) -> w.write(s.toUpperCase()), writer);

        assertEquals(2, count);
        String result = writer.toString();
        assertTrue(result.contains("LINE1"));
        assertTrue(result.contains("LINE2"));
    }

    @Test
    public void testPersistToPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        stream = createStream(1, 2, 3);
        long count = stream.persist(stmt, 2, 0, (i, ps) -> ps.setInt(1, i));

        assertEquals(3, count);
        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).addBatch();
        verify(stmt, times(2)).executeBatch();
    }

    @Test
    public void testPersistToConnection() throws SQLException {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        stream = createStream(1, 2, 3);
        long count = stream.persist(conn, "INSERT INTO test VALUES (?)", 1, 0, (i, ps) -> ps.setInt(1, i));

        assertEquals(3, count);
        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testPersistToDataSource() throws SQLException {
        javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        stream = createStream(1, 2, 3);
        long count = stream.persist(ds, "INSERT INTO test VALUES (?)", 1, 0, (i, ps) -> ps.setInt(1, i));

        assertEquals(3, count);
        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testPersistToCSVFile() throws IOException {
        File tempFile = File.createTempFile("test", ".csv");
        tempFile.deleteOnExit();

        List<TestBean> beans = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> beanStream = createStream(beans.toArray(new TestBean[0]));
        long count = beanStream.persistToCsv(tempFile);

        assertEquals(2, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertTrue(lines.size() >= 3);
    }

    @Test
    public void testPersistToCSVWithHeaders() throws IOException {
        File tempFile = File.createTempFile("test", ".csv");
        tempFile.deleteOnExit();

        objectStream = createStream(new Integer[] { 1, 2 }, new Integer[] { 3, 4 });
        long count = objectStream.persistToCsv(Arrays.asList("col1", "col2"), tempFile);

        assertEquals(2, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertEquals("\"col1\",\"col2\"", lines.get(0));
    }

    @Test
    public void testPersistToCSVOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        List<TestBean> beans = Arrays.asList(new TestBean("John", 25));
        Stream<TestBean> beanStream = createStream(beans.toArray(new TestBean[0]));
        long count = beanStream.persistToCsv(baos);

        assertEquals(1, count);
        String result = baos.toString();
        assertTrue(result.contains("John"));
        assertTrue(result.contains("25"));
    }

    @Test
    public void testPersistToCSVWriter() throws IOException {
        StringWriter writer = new StringWriter();

        List<TestBean> beans = Arrays.asList(new TestBean("John", 25));
        Stream<TestBean> beanStream = createStream(beans.toArray(new TestBean[0]));
        long count = beanStream.persistToCsv(writer);

        assertEquals(1, count);
        String result = writer.toString();
        assertTrue(result.contains("John"));
        assertTrue(result.contains("25"));
    }

    @Test
    public void testPersistToJSONFile() throws IOException {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();

        stream = createStream(1, 2, 3);
        long count = stream.persistToJson(tempFile);

        assertEquals(3, count);
        String content = new String(Files.readAllBytes(tempFile));
        assertTrue(content.contains("["));
        assertTrue(content.contains("1"));
        assertTrue(content.contains("2"));
        assertTrue(content.contains("3"));
        assertTrue(content.contains("]"));
    }

    @Test
    public void testPersistToJSONOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stream = createStream(1, 2, 3);
        long count = stream.persistToJson(baos);

        assertEquals(3, count);
        String result = baos.toString();
        assertTrue(result.contains("["));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testPersistToJSONWriter() throws IOException {
        StringWriter writer = new StringWriter();
        stream = createStream(1, 2, 3);
        long count = stream.persistToJson(writer);

        assertEquals(3, count);
        String result = writer.toString();
        assertTrue(result.contains("["));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testCrossJoin() {
        stream = createStream(1, 2);
        Stream<Pair<Integer, String>> result = stream.crossJoin(Arrays.asList("a", "b"));
        List<Pair<Integer, String>> list = result.toList();
        assertEquals(4, list.size());
        assertTrue(list.contains(Pair.of(1, "a")));
        assertTrue(list.contains(Pair.of(1, "b")));
        assertTrue(list.contains(Pair.of(2, "a")));
        assertTrue(list.contains(Pair.of(2, "b")));
    }

    @Test
    public void testCrossJoinWithFunction() {
        stream = createStream(1, 2);
        Stream<String> result = stream.crossJoin(Arrays.asList("a", "b"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "1b", "2a", "2b"), result.toList());
    }

    @Test
    public void testCrossJoinWithStream() {
        stream = createStream(1, 2);
        Stream<String> other = createStream("a", "b");
        Stream<String> result = stream.crossJoin(other, (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "1b", "2a", "2b"), result.toList());
    }

    @Test
    public void testInnerJoin() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> result = stream.innerJoin(Arrays.asList("1a", "2b", "3c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        assertEquals(3, result.toList().size());
    }

    @Test
    public void testInnerJoinSameType() {
        stream = createStream(1, 2, 3, 4);
        Stream<Pair<Integer, Integer>> result = stream.innerJoin(Arrays.asList(2, 3, 4, 5), i -> i % 2);
        List<Pair<Integer, Integer>> list = result.toList();
        assertTrue(list.size() > 0);
    }

    @Test
    public void testInnerJoinWithFunction() {
        stream = createStream(1, 2, 3);
        Stream<String> result = stream.innerJoin(Arrays.asList("1a", "2b", "3c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)),
                (i, s) -> i + "-" + s);
        assertTrue(result.toList().contains("1-1a"));
    }

    @Test
    public void testInnerJoinWithPredicate() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> result = stream.innerJoin(Arrays.asList("a", "b", "c"), (i, s) -> true);
        assertEquals(9, result.toList().size());
    }

    @Test
    public void testFullJoin() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> result = stream.fullJoin(Arrays.asList("2b", "3c", "4d"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        List<Pair<Integer, String>> list = result.toList();
        assertEquals(4, list.size());
        assertTrue(list.contains(Pair.of(1, null)));
        assertTrue(list.contains(Pair.of(null, "4d")));
    }

    @Test
    public void testFullJoinSameType() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.fullJoin(Arrays.asList(2, 3, 4), Fn.identity());
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(4, list.size());
    }

    @Test
    public void testFullJoinWithPredicate() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.fullJoin(Arrays.asList(2, 3, 4), (a, b) -> a.equals(b));
        List<Pair<Integer, Integer>> list = result.toList();
        assertTrue(list.contains(Pair.of(1, null)));
        assertTrue(list.contains(Pair.of(null, 4)));
    }

    @Test
    public void testLeftJoin() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> result = stream.leftJoin(Arrays.asList("2b", "3c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        List<Pair<Integer, String>> list = result.toList();
        assertEquals(3, list.size());
        assertTrue(list.contains(Pair.of(1, null)));
    }

    @Test
    public void testLeftJoinSameType() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.leftJoin(Arrays.asList(2, 3, 4), Fn.identity());
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testLeftJoinWithPredicate() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.leftJoin(Arrays.asList(2, 3, 4), (a, b) -> a.equals(b));
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(3, list.size());
        assertTrue(list.contains(Pair.of(1, null)));
    }

    @Test
    public void testRightJoin() {
        stream = createStream(1, 2);
        Stream<Pair<Integer, String>> result = stream.rightJoin(Arrays.asList("1a", "2b", "3c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        List<Pair<Integer, String>> list = result.toList();
        assertEquals(3, list.size());
        assertTrue(list.contains(Pair.of(null, "3c")));
    }

    @Test
    public void testRightJoinSameType() {
        stream = createStream(1, 2);
        Stream<Pair<Integer, Integer>> result = stream.rightJoin(Arrays.asList(1, 2, 3), Fn.identity());
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testRightJoinWithPredicate() {
        stream = createStream(1, 2);
        Stream<Pair<Integer, Integer>> result = stream.rightJoin(Arrays.asList(1, 2, 3), (a, b) -> a.equals(b));
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(3, list.size());
        assertTrue(list.contains(Pair.of(null, 3)));
    }

    @Test
    public void testGroupJoin() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, List<String>>> result = stream.groupJoin(Arrays.asList("1a", "1b", "2c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        Map<Integer, List<String>> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2, map.get(1).size());
        assertEquals(1, map.get(2).size());
        assertEquals(0, map.get(3).size());
    }

    @Test
    public void testGroupJoinSameType() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, List<Integer>>> result = stream.groupJoin(Arrays.asList(1, 1, 2, 2, 2), Fn.identity());
        Map<Integer, List<Integer>> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2, map.get(1).size());
        assertEquals(3, map.get(2).size());
    }

    @Test
    public void testGroupJoinWithMergeFunction() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.groupJoin(Arrays.asList(1, 1, 2, 2, 2), Fn.identity(), Fn.identity(), Integer::sum);
        Map<Integer, Integer> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2, (int) map.get(1));
        assertEquals(6, (int) map.get(2));
    }

    @Test
    public void testGroupJoinWithCollector() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Long>> result = stream.groupJoin(Arrays.asList(1, 1, 2, 2, 2), Fn.identity(), Fn.identity(), Collectors.counting());
        Map<Integer, Long> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2L, (long) map.get(1));
        assertEquals(3L, (long) map.get(2));
    }

    @Test
    public void testJoinByRange() {
        stream = createStream(1, 5, 10);
        Iterator<Integer> iter = Arrays.asList(2, 3, 6, 7, 8, 11).iterator();
        Stream<Pair<Integer, List<Integer>>> result = stream.joinByRange(iter, (a, b) -> b > a && b < a + 5);
        List<Pair<Integer, List<Integer>>> list = result.toList();
        assertEquals(3, list.size());
        assertEquals(Arrays.asList(2, 3), list.get(0).getRight());
        assertEquals(Arrays.asList(6, 7, 8), list.get(1).getRight());
    }

    @Test
    public void testJoinByRangeWithCollector() {
        stream = createStream(1, 5, 10);
        Iterator<Integer> iter = Arrays.asList(2, 3, 6, 7, 8, 11).iterator();
        Stream<Pair<Integer, Long>> result = stream.joinByRange(iter, (a, b) -> b > a && b < a + 5, Collectors.counting());
        Map<Integer, Long> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2L, (long) map.get(1));
        assertEquals(3L, (long) map.get(5));
    }

    @Test
    public void testJoinByRangeWithMapperForUnjoined() {
        stream = createStream(1, 5);
        Iterator<Integer> iter = Arrays.asList(2, 6, 10, 11).iterator();
        Stream<String> result = stream.joinByRange(iter, (a, b) -> b > a && b < a + 4, Collectors.counting(), (key, count) -> key + ":" + count,
                unjoined -> Stream.of(unjoined).map(i -> "unjoined:" + i));
        List<String> list = result.toList();
        assertTrue(list.contains("1:1"));
        assertTrue(list.contains("5:1"));
        assertTrue(list.contains("unjoined:10"));
        assertTrue(list.contains("unjoined:11"));
    }

    public static class TestBean {
        private String name;
        private int age;

        public TestBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
