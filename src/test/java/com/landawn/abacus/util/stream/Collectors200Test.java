package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;

public class Collectors200Test extends TestBase {

    //    @Test
    //    public void test_constructorIsPrivate() {
    //        // This test is to ensure the constructor is not public, enforcing the utility class pattern.
    //        // It's a compile-time check rather than a runtime test.
    //        assertThrows(IllegalAccessException.class, () -> Collectors.class.getDeclaredConstructor().newInstance());
    //    }

    @Test
    public void test_toCollection() {
        List<Integer> list = Stream.of(1, 2, 3, 2).collect(Collectors.toCollection(LinkedList::new));
        assertEquals(LinkedList.class, list.getClass());
        assertEquals(Arrays.asList(1, 2, 3, 2), list);

        Set<Integer> set = Stream.of(1, 2, 3, 2).collect(Collectors.toCollection(HashSet::new));
        assertEquals(HashSet.class, set.getClass());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void test_toCollection_withSizeLimit() {
        List<Integer> list = Stream.of(1, 2, 3, 4, 5).collect(Collectors.toCollection(3, ArrayList::new));
        assertEquals(3, list.size());
        assertEquals(Arrays.asList(1, 2, 3), list);

        // Test with parallel stream
        List<Integer> parallelList = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel().collect(Collectors.toCollection(5, ArrayList::new));
        assertEquals(5, parallelList.size());
    }

    @Test
    public void test_toList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertTrue(list instanceof ArrayList);
    }

    @Test
    public void test_toLinkedList() {
        LinkedList<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toLinkedList());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_toImmutableList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toImmutableList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void test_toUnmodifiableList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toUnmodifiableList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void test_toSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2, 1).collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void test_toLinkedHashSet() {
        Set<Integer> set = Stream.of(3, 1, 2, 1).collect(Collectors.toLinkedHashSet());
        assertEquals(LinkedHashSet.class, set.getClass());
        assertEquals(Arrays.asList(3, 1, 2), new ArrayList<>(set));
    }

    @Test
    public void test_toImmutableSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2).collect(Collectors.toImmutableSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void test_toUnmodifiableSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2).collect(Collectors.toUnmodifiableSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void test_toArray() {
        Object[] array = Stream.of(1, "b", 3.0).collect(Collectors.toArray());
        assertArrayEquals(new Object[] { 1, "b", 3.0 }, array);
    }

    @Test
    public void test_toArray_withGenerator() {
        Integer[] array = Stream.of(1, 2, 3).collect(Collectors.toArray(Integer[]::new));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
    }

    @Test
    public void test_toPrimitiveArrays() {
        assertArrayEquals(new boolean[] { true, false }, Stream.of(true, false).collect(Collectors.toBooleanArray()));
        assertArrayEquals(new char[] { 'a', 'b' }, Stream.of('a', 'b').collect(Collectors.toCharArray()));
        assertArrayEquals(new byte[] { 1, 2 }, Stream.of((byte) 1, (byte) 2).collect(Collectors.toByteArray()));
        assertArrayEquals(new short[] { 1, 2 }, Stream.of((short) 1, (short) 2).collect(Collectors.toShortArray()));
        assertArrayEquals(new int[] { 1, 2 }, Stream.of(1, 2).collect(Collectors.toIntArray()));
        assertArrayEquals(new long[] { 1L, 2L }, Stream.of(1L, 2L).collect(Collectors.toLongArray()));
        assertArrayEquals(new float[] { 1.0f, 2.0f }, Stream.of(1.0f, 2.0f).collect(Collectors.toFloatArray()));
        assertArrayEquals(new double[] { 1.0, 2.0 }, Stream.of(1.0, 2.0).collect(Collectors.toDoubleArray()));
    }

    @Test
    public void test_onlyOne() {
        Optional<Integer> result = Stream.of(1).collect(Collectors.onlyOne());
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        Optional<Integer> emptyResult = Stream.<Integer> empty().collect(Collectors.onlyOne());
        assertFalse(emptyResult.isPresent());

        assertThrows(TooManyElementsException.class, () -> Stream.of(1, 2).collect(Collectors.onlyOne()));
    }

    @Test
    public void test_onlyOne_withPredicate() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).collect(Collectors.onlyOne(i -> i == 3));
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        Optional<Integer> emptyResult = Stream.of(1, 2, 4).collect(Collectors.onlyOne(i -> i == 3));
        assertFalse(emptyResult.isPresent());

        assertThrows(TooManyElementsException.class, () -> Stream.of(1, 2, 3, 2, 4).collect(Collectors.onlyOne(i -> i == 2)));
    }

    @Test
    public void test_first() {
        assertEquals(Optional.of(1), Stream.of(1, 2, 3).collect(Collectors.first()));
        assertEquals(Optional.empty(), Stream.empty().collect(Collectors.first()));
        assertThrows(UnsupportedOperationException.class, () -> Stream.of(1, 2, 3).parallel().collect(Collectors.first()));
    }

    @Test
    public void test_last() {
        assertEquals(Optional.of(3), Stream.of(1, 2, 3).collect(Collectors.last()));
        assertEquals(Optional.empty(), Stream.empty().collect(Collectors.last()));
        assertThrows(UnsupportedOperationException.class, () -> Stream.of(1, 2, 3).parallel().collect(Collectors.last()));
    }

    @Test
    public void test_first_n() {
        assertEquals(Arrays.asList(1, 2), Stream.of(1, 2, 3).collect(Collectors.first(2)));
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).collect(Collectors.first(5)));
        assertTrue(Stream.of(1, 2, 3).collect(Collectors.first(0)).isEmpty());
    }

    @Test
    public void test_last_n() {
        assertEquals(Arrays.asList(2, 3), Stream.of(1, 2, 3).collect(Collectors.last(2)));
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).collect(Collectors.last(5)));
        assertTrue(Stream.of(1, 2, 3).collect(Collectors.last(0)).isEmpty());
    }

    @Test
    public void test_joining() {
        assertEquals("abc", Stream.of("a", "b", "c").collect(Collectors.joining()));
        assertEquals("a,b,c", Stream.of("a", "b", "c").collect(Collectors.joining(",")));
        assertEquals("[a,b,c]", Stream.of("a", "b", "c").collect(Collectors.joining(",", "[", "]")));
    }

    @Test
    public void test_summing() {
        assertEquals(6, Stream.of(1, 2, 3).collect(Collectors.summingInt(i -> i)));
        assertEquals(6L, Stream.of(1, 2, 3).collect(Collectors.summingLong(i -> (long) i)));
        assertEquals(6.0, Stream.of(1.0, 2.0, 3.0).collect(Collectors.summingDouble(d -> d)));
        assertEquals(new BigInteger("6"), Stream.of("1", "2", "3").collect(Collectors.summingBigInteger(BigInteger::new)));
        assertEquals(new BigDecimal("6.6"), Stream.of("1.1", "2.2", "3.3").collect(Collectors.summingBigDecimal(BigDecimal::new)));
    }

    @Test
    public void test_averaging() {
        assertEquals(2.0, Stream.of(1, 2, 3).collect(Collectors.averagingInt(i -> i)));
        assertEquals(2.0, Stream.of(1L, 2L, 3L).collect(Collectors.averagingLong(i -> i)));
        assertEquals(2.0, Stream.of(1.0, 2.0, 3.0).collect(Collectors.averagingDouble(i -> i)));
        assertEquals(new BigDecimal("2"), Stream.of("1", "2", "3").collect(Collectors.averagingBigInteger(BigInteger::new)));
        assertEquals(new BigDecimal("2.2"), Stream.of("1.1", "2.2", "3.3").collect(Collectors.averagingBigDecimal(BigDecimal::new)));

        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.averagingIntOrElseThrow(i -> i)));
    }

    @Test
    public void test_toMap() {
        Map<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toMap(s -> s, String::length));
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));

        assertThrows(IllegalStateException.class, () -> Stream.of("a", "b", "a").collect(Collectors.toMap(s -> s, String::length)));

        Map<String, Integer> mergedMap = Stream.of("a", "b", "a").collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
        assertEquals(2, mergedMap.get("a"));
        assertEquals(1, mergedMap.get("b"));
    }

    @Test
    public void test_toConcurrentMap() {
        ConcurrentMap<String, Integer> map = Stream.of("a", "bb", "ccc").parallel().collect(Collectors.toConcurrentMap(s -> s, String::length));
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));

        assertThrows(IllegalStateException.class, () -> Stream.of("a", "b", "a").parallel().collect(Collectors.toConcurrentMap(s -> s, String::length)));
    }

    @Test
    public void test_groupingBy() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd").collect(Collectors.groupingBy(String::length));
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb", "dd"), map.get(2));
    }

    @Test
    public void test_groupingBy_withDownstream() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd").collect(Collectors.groupingBy(String::length, Collectors.counting()));
        assertEquals(2L, map.get(1));
        assertEquals(2L, map.get(2));
    }

    @Test
    public void test_groupingByConcurrent() {
        ConcurrentMap<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd").parallel().collect(Collectors.groupingByConcurrent(String::length));
        assertTrue(map.get(1).containsAll(Arrays.asList("a", "c")));
        assertTrue(map.get(2).containsAll(Arrays.asList("bb", "dd")));
    }

    @Test
    public void test_partitioningBy() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(i -> i % 2 == 0));
        assertEquals(Arrays.asList(1, 3), map.get(false));
        assertEquals(Arrays.asList(2, 4), map.get(true));
    }

    @Test
    public void test_partitioningBy_withDownstream() {
        Map<Boolean, Long> map = Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(i -> i % 2 == 0, Collectors.counting()));
        assertEquals(2L, map.get(false));
        assertEquals(2L, map.get(true));
    }

    @Test
    public void test_minMax() {
        Optional<Pair<Integer, Integer>> result = Stream.of(3, 1, 5, 2, 4).collect(Collectors.minMax());
        assertTrue(result.isPresent());
        assertEquals(Pair.of(1, 5), result.get());

        Optional<Pair<String, String>> strResult = Stream.of("c", "a", "b").collect(Collectors.minMax(Comparator.naturalOrder()));
        assertTrue(strResult.isPresent());
        assertEquals(Pair.of("a", "c"), strResult.get());

        Optional<Pair<Integer, Integer>> emptyResult = Stream.<Integer> empty().collect(Collectors.minMax());
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void test_minMaxBy() {
        List<String> data = Arrays.asList("a", "bb", "ccc");
        Optional<Pair<String, String>> result = data.stream().collect(Collectors.minMaxBy(String::length));
        assertTrue(result.isPresent());
        assertEquals(Pair.of("a", "ccc"), result.get());
    }

    @Test
    public void test_minMaxOrElse() {
        Pair<Integer, Integer> result = Stream.<Integer> empty().collect(Collectors.minMaxOrElseGet(() -> Pair.of(-1, 1)));
        assertEquals(Pair.of(-1, 1), result);

        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.minMaxOrElseThrow()));
    }

    @Nested
    public class MoreCollectorsTest {

        @Test
        public void test_summingInt_multiple() {
            Tuple2<Integer, Integer> result2 = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingInt(String::length, s -> s.hashCode() % 100));
            assertEquals(1 + 2 + 3, result2._1);

            Tuple3<Integer, Integer, Integer> result3 = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingInt(String::length, s -> s.hashCode() % 100, s -> 1));
            assertEquals(6, result3._1);
            assertEquals(3, result3._3);
        }

        @Test
        public void test_averagingInt_multiple() {
            Tuple2<Double, Double> result2 = Stream.of(1, 2, 3, 4).collect(MoreCollectors.averagingInt(i -> i, i -> i * 2));
            assertEquals(2.5, result2._1);
            assertEquals(5.0, result2._2);
        }

        @Test
        public void test_combine_two() {
            Tuple2<Long, Integer> result = Stream.of(1, 2, 3, 4, 5)
                    .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i)));
            assertEquals(5L, result._1);
            assertEquals(15, result._2);
        }

        @Test
        public void test_combine_three() {
            Tuple3<Long, Integer, Optional<Integer>> result = Stream.of(1, 2, 3, 4, 5)
                    .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.maxBy(Fn.identity())));
            assertEquals(5L, result._1);
            assertEquals(15, result._2);
            assertEquals(Optional.of(5), result._3);
        }

        @Test
        public void test_toDataset() {
            List<Map<String, Object>> data = new ArrayList<>();
            data.add(N.asMap("id", 1, "name", "John"));
            data.add(N.asMap("id", 2, "name", "Jane"));

            Dataset dataset = data.stream().collect(MoreCollectors.toDataset());
            assertEquals(2, dataset.size());
            assertEquals(2, dataset.columnNameList().size());
            assertTrue(dataset.columnNameList().contains("id"));
            assertTrue(dataset.columnNameList().contains("name"));

            Dataset datasetWithNames = data.stream().collect(MoreCollectors.toDataset(Arrays.asList("ID", "NAME")));
            assertEquals(2, datasetWithNames.size());
            assertEquals(Arrays.asList("ID", "NAME"), datasetWithNames.columnNameList());
        }
    }
}
